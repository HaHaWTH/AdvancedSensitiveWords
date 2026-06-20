package io.wdsj.asw.bukkit.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.wdsj.asw.bukkit.api.moderation.LlmChatModerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

/** Writes LLM request and response audit records as UTF-8 JSON Lines. */
final class LlmHistoryLogger implements AutoCloseable {
    private static final String ACTIVE_FILE_NAME = "llm-history.log";
    private static final DateTimeFormatter ARCHIVE_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Logger logger = LoggerFactory.getLogger(LlmHistoryLogger.class);
    private final Path directory;
    private final Path activeFile;
    private final Clock clock;
    private final ExecutorService writer;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean failureLogged = new AtomicBoolean();

    private LocalDate activeDate;

    LlmHistoryLogger(Path dataDirectory) {
        this(dataDirectory, Clock.systemDefaultZone());
    }

    LlmHistoryLogger(Path dataDirectory, Clock clock) {
        this.directory = dataDirectory.resolve("llm-history");
        this.activeFile = directory.resolve(ACTIVE_FILE_NAME);
        this.clock = clock;
        this.writer = Executors.newSingleThreadExecutor(
                Thread.ofVirtual().name("ASW LLM History Logger", 0L).factory()
        );
    }

    void logRequest(
            UUID requestId,
            UUID playerId,
            String playerName,
            String modelName,
            double entropy,
            String userPayload
    ) {
        Map<String, Object> entry = baseEntry("request", requestId);
        entry.put("player_uuid", playerId.toString());
        entry.put("player_name", playerName);
        entry.put("model", modelName);
        entry.put("entropy", entropy);
        entry.put("user_payload", userPayload);
        submit(entry);
    }

    void logResponse(
            UUID requestId,
            String rawResponse,
            LlmChatModerationResult localResult,
            LlmChatModerationResult effectiveResult,
            boolean eventCancelled,
            LlmCategoryPolicy categoryPolicy,
            boolean eligibleForNotification,
            boolean eligibleForEnforcement,
            boolean stale
    ) {
        Map<String, Object> entry = baseEntry("response", requestId);
        entry.put("raw_response", rawResponse);
        entry.put("local_result", resultMap(localResult));
        entry.put("effective_result", resultMap(effectiveResult));
        entry.put("event_cancelled", eventCancelled);
        entry.put("notification_threshold", categoryPolicy == null ? null : categoryPolicy.notifyConfidence());
        entry.put("punishment_threshold", categoryPolicy == null ? null : categoryPolicy.punishConfidence());
        entry.put("eligible_for_notification", eligibleForNotification);
        entry.put("eligible_for_enforcement", eligibleForEnforcement);
        entry.put("stale", stale);
        submit(entry);
    }

    void logFailure(UUID requestId, Class<? extends Throwable> errorType) {
        Map<String, Object> entry = baseEntry("failure", requestId);
        entry.put("error_type", errorType.getName());
        submit(entry);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        writer.shutdown();
        try {
            if (!writer.awaitTermination(5L, TimeUnit.SECONDS)) {
                writer.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            writer.shutdownNow();
        }
    }

    private Map<String, Object> baseEntry(String type, UUID requestId) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("timestamp", Instant.now(clock).toString());
        entry.put("type", type);
        entry.put("request_id", requestId.toString());
        return entry;
    }

    private void submit(Map<String, Object> entry) {
        if (closed.get()) {
            return;
        }
        try {
            writer.execute(() -> write(entry));
        } catch (RejectedExecutionException ignored) {
            // Shutdown races do not affect moderation.
        }
    }

    private void write(Map<String, Object> entry) {
        try {
            Files.createDirectories(directory);
            rotateIfNeeded(LocalDate.now(clock));
            String serialized = OBJECT_MAPPER.writeValueAsString(entry);
            try (BufferedWriter output = Files.newBufferedWriter(
                    activeFile,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            )) {
                output.write(serialized);
                output.newLine();
            }
        } catch (IOException | RuntimeException exception) {
            if (failureLogged.compareAndSet(false, true)) {
                logger.warn("Failed to write LLM history. Further write failures will be suppressed.", exception);
            }
        }
    }

    private void rotateIfNeeded(LocalDate today) throws IOException {
        if (activeDate == null && Files.exists(activeFile)) {
            activeDate = Files.getLastModifiedTime(activeFile).toInstant()
                    .atZone(clock.getZone()).toLocalDate();
        }
        if (activeDate != null && !activeDate.equals(today) && Files.exists(activeFile)) {
            Path archive = nextArchivePath(activeDate);
            try (InputStream input = Files.newInputStream(activeFile);
                 OutputStream output = new GZIPOutputStream(Files.newOutputStream(archive))) {
                input.transferTo(output);
            }
            Files.delete(activeFile);
        }
        activeDate = today;
    }

    private Path nextArchivePath(LocalDate date) {
        String prefix = ARCHIVE_DATE.format(date);
        for (int sequence = 1; ; sequence++) {
            Path candidate = directory.resolve(prefix + "-" + sequence + ".log.gz");
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
    }

    private static Map<String, Object> resultMap(LlmChatModerationResult result) {
        if (result == null) {
            return null;
        }
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("category", result.category().wireName());
        value.put("secondary_categories", result.secondaryCategories().stream()
                .map(category -> category.wireName())
                .toList());
        value.put("confidence", result.confidence());
        value.put("severity", result.severity().wireName());
        value.put("signals", List.copyOf(result.signals()));
        value.put("explanation", result.explanation());
        return value;
    }
}
