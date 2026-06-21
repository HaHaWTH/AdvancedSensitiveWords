package io.wdsj.asw.bukkit.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wdsj.asw.bukkit.api.moderation.LlmChatModerationResult;
import io.wdsj.asw.bukkit.api.moderation.LlmModerationCategory;
import io.wdsj.asw.bukkit.api.moderation.LlmModerationSeverity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmHistoryLoggerTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @TempDir
    Path temporaryDirectory;

    @Test
    void writesRequestResponseAndFailureAsJsonLines() throws Exception {
        UUID requestId = UUID.randomUUID();
        try (LlmHistoryLogger logger = new LlmHistoryLogger(
                temporaryDirectory,
                Clock.fixed(Instant.parse("2026-06-20T12:00:00Z"), ZoneOffset.UTC)
        )) {
            logger.logRequest(requestId, UUID.randomUUID(), "Tester", "deepseek-v4-flash",
                    LlmApiMode.ANTHROPIC_MESSAGES, 2.5D,
                    "{\"message\":\"你好\"}");
            logger.logResponse(requestId, "{\"category\":\"harassment\"}", result(), result(),
                    false, new LlmCategoryPolicy(0.75D, 0.90D, List.of()), true, true, false);
            logger.logFailure(requestId, IllegalStateException.class);
        }

        Path history = temporaryDirectory.resolve("llm-history/llm-history.log");
        List<String> lines = Files.readAllLines(history, StandardCharsets.UTF_8);
        assertEquals(3, lines.size());

        JsonNode request = OBJECT_MAPPER.readTree(lines.getFirst());
        assertEquals("request", request.get("type").asText());
        assertEquals("{\"message\":\"你好\"}", request.get("user_payload").asText());
        assertEquals("ANTHROPIC_MESSAGES", request.get("api_mode").asText());

        JsonNode response = OBJECT_MAPPER.readTree(lines.get(1));
        assertEquals("response", response.get("type").asText());
        assertEquals("harassment", response.get("effective_result").get("category").asText());
        assertEquals(0.75D, response.get("notification_threshold").asDouble());
        assertEquals(0.90D, response.get("punishment_threshold").asDouble());
        assertTrue(response.get("eligible_for_notification").asBoolean());
        assertTrue(response.get("eligible_for_enforcement").asBoolean());

        JsonNode failure = OBJECT_MAPPER.readTree(lines.get(2));
        assertEquals("failure", failure.get("type").asText());
        assertEquals(IllegalStateException.class.getName(), failure.get("error_type").asText());
        assertFalse(failure.has("error_message"));
    }

    @Test
    void rotatesPriorDayHistoryIntoIncrementedGzipArchive() throws Exception {
        Path directory = temporaryDirectory.resolve("llm-history");
        Files.createDirectories(directory);
        Path active = directory.resolve("llm-history.log");
        Files.writeString(active, "old-entry\n", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(active, FileTime.from(Instant.parse("2026-06-19T12:00:00Z")));
        Files.writeString(directory.resolve("2026-06-19-1.log.gz"), "reserved", StandardCharsets.UTF_8);

        try (LlmHistoryLogger logger = new LlmHistoryLogger(
                temporaryDirectory,
                Clock.fixed(Instant.parse("2026-06-20T12:00:00Z"), ZoneOffset.UTC)
        )) {
            logger.logFailure(UUID.randomUUID(), IllegalArgumentException.class);
        }

        Path archive = directory.resolve("2026-06-19-2.log.gz");
        assertTrue(Files.exists(archive));
        try (InputStream input = new GZIPInputStream(Files.newInputStream(archive))) {
            assertEquals("old-entry\n", new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertTrue(Files.readString(active, StandardCharsets.UTF_8).contains("\"type\":\"failure\""));
    }

    private static LlmChatModerationResult result() {
        return new LlmChatModerationResult(
                LlmModerationCategory.HARASSMENT,
                List.of(),
                0.95D,
                LlmModerationSeverity.HIGH,
                List.of("targeted abuse"),
                "Harassing content."
        );
    }
}
