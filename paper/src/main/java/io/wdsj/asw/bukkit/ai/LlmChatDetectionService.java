package io.wdsj.asw.bukkit.ai;

import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.api.event.AsyncModerationResponseEvent;
import io.wdsj.asw.bukkit.api.moderation.LlmChatModerationResult;
import io.wdsj.asw.bukkit.api.moderation.LlmModerationCategory;
import io.wdsj.asw.bukkit.setting.PaperConfigurationService;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.type.ModuleType;
import io.wdsj.asw.bukkit.util.SchedulingUtils;
import io.wdsj.asw.bukkit.util.ViolationReporter;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.nio.charset.StandardCharsets;

/** Coordinates asynchronous, single-message LLM moderation requests. */
public final class LlmChatDetectionService implements Listener, AutoCloseable {
    private final PaperConfigurationService configuration;
    private final ViolationReporter violationReporter;
    private final Function<LlmSettings, LlmChatClient> clientFactory;
    private final AtomicLong generation = new AtomicLong();
    private final ConcurrentMap<UUID, Long> inFlightGenerations = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, CooldownReservation> cooldowns = new ConcurrentHashMap<>();
    private final LongAdder submittedRequests = new LongAdder();
    private final LongAdder droppedRequests = new LongAdder();
    private final LongAdder failedRequests = new LongAdder();
    private final LongAdder invalidResponses = new LongAdder();
    private final LongAdder enforcedResponses = new LongAdder();

    private volatile RuntimeState runtime;
    private volatile boolean closed;

    public LlmChatDetectionService(PaperConfigurationService configuration) {
        this(configuration, new ViolationReporter(configuration), OpenAiLlmChatClient::new);
    }

    LlmChatDetectionService(
            PaperConfigurationService configuration,
            ViolationReporter violationReporter,
            Function<LlmSettings, LlmChatClient> clientFactory
    ) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.violationReporter = Objects.requireNonNull(violationReporter, "violationReporter");
        this.clientFactory = Objects.requireNonNull(clientFactory, "clientFactory");
        reload();
    }

    public synchronized void reload() {
        if (closed) {
            return;
        }

        long nextGeneration = generation.incrementAndGet();
        RuntimeState previous = runtime;
        runtime = createRuntime(nextGeneration);
        inFlightGenerations.clear();
        cooldowns.clear();
        if (previous != null) {
            previous.executor().shutdownNow();
        }
    }

    public void submit(UUID playerId, String playerName, String message) {
        RuntimeState state = runtime;
        if (state == null || closed || !AdvancedSensitiveWords.isInitialized || AdvancedSensitiveWords.sensitiveWordBs == null) {
            return;
        }

        LlmSettings settings = state.settings();
        int rawCodePoints = message.codePointCount(0, message.length());
        if (rawCodePoints > settings.maximumMessageCodePoints()) {
            return;
        }
        int visibleCodePoints = ChatEntropy.visibleCodePointCount(message);
        if (visibleCodePoints < settings.minimumMessageCodePoints()) {
            return;
        }

        double entropy = ChatEntropy.shannonEntropyBits(message);
        if (entropy < settings.minimumEntropyBits()) {
            return;
        }

        Candidate candidate = reserveCandidate(state, playerId, playerName, message, entropy);
        if (candidate == null) {
            droppedRequests.increment();
            return;
        }

        try {
            state.executor().execute(() -> process(state, candidate));
            submittedRequests.increment();
        } catch (RejectedExecutionException exception) {
            releaseRejectedCandidate(candidate);
            droppedRequests.increment();
        }
    }

    public long submittedRequests() {
        return submittedRequests.sum();
    }

    public long droppedRequests() {
        return droppedRequests.sum();
    }

    public long failedRequests() {
        return failedRequests.sum();
    }

    public long invalidResponses() {
        return invalidResponses.sum();
    }

    public long enforcedResponses() {
        return enforcedResponses.sum();
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        generation.incrementAndGet();
        RuntimeState previous = runtime;
        runtime = null;
        inFlightGenerations.clear();
        cooldowns.clear();
        if (previous != null) {
            previous.executor().shutdownNow();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        clearPlayer(event.getPlayer().getUniqueId());
    }

    private RuntimeState createRuntime(long nextGeneration) {
        if (!configuration.get(PluginSettings.AI_ENABLED)) {
            return null;
        }

        LlmSettings settings = LlmSettings.from(configuration);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                settings.maxConcurrentRequests(),
                settings.maxConcurrentRequests(),
                30L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(settings.queueCapacity()),
                Thread.ofVirtual().name("ASW LangChain Moderation", 0L).factory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        executor.allowCoreThreadTimeOut(true);
        return new RuntimeState(nextGeneration, settings, clientFactory.apply(settings), executor);
    }

    private Candidate reserveCandidate(
            RuntimeState state,
            UUID playerId,
            String playerName,
            String message,
            double entropy
    ) {
        if (!isCurrent(state) || inFlightGenerations.putIfAbsent(playerId, state.generation()) != null) {
            return null;
        }

        long now = System.currentTimeMillis();
        long deadline = now + TimeUnit.SECONDS.toMillis(state.settings().perPlayerCooldownSeconds());
        AtomicBoolean granted = new AtomicBoolean();
        cooldowns.compute(playerId, (ignored, existing) -> {
            if (existing != null && existing.deadlineMillis() > now) {
                return existing;
            }
            granted.set(true);
            return new CooldownReservation(state.generation(), deadline);
        });
        if (!granted.get()) {
            inFlightGenerations.remove(playerId, state.generation());
            return null;
        }

        CooldownReservation reservation = new CooldownReservation(state.generation(), deadline);
        if (!isCurrent(state)) {
            inFlightGenerations.remove(playerId, state.generation());
            cooldowns.remove(playerId, reservation);
            return null;
        }
        return new Candidate(UUID.randomUUID(), playerId, playerName, message, entropy, reservation);
    }

    private void releaseRejectedCandidate(Candidate candidate) {
        inFlightGenerations.remove(candidate.playerId(), candidate.reservation().generation());
        cooldowns.remove(candidate.playerId(), candidate.reservation());
    }

    private void process(RuntimeState state, Candidate candidate) {
        try {
            String userMessage = LlmModerationPrompt.createUserMessage(candidate.message(), state.settings().serverContext());
            String rawResponse = state.client().classify(LlmModerationPrompt.SYSTEM_PROMPT, userMessage);
            if (!isCurrent(state)) {
                return;
            }
            handleResponse(state, candidate, rawResponse == null ? "" : rawResponse);
        } catch (RuntimeException exception) {
            if (isCurrent(state)) {
                failedRequests.increment();
                AdvancedSensitiveWords.LOGGER.debug("LLM moderation request failed for player {}.", candidate.playerId());
            }
        } finally {
            inFlightGenerations.remove(candidate.playerId(), state.generation());
        }
    }

    private void handleResponse(RuntimeState state, Candidate candidate, String rawResponse) {
        if (state.settings().logResponses()) {
            AdvancedSensitiveWords.LOGGER.info(
                    "LLM moderation response [requestId={}, player={}]: {}",
                    candidate.requestId(),
                    candidate.playerName(),
                    rawResponse
            );
        }
        Optional<LlmChatModerationResult> parsed = LlmModerationResponseParser.parse(rawResponse);
        if (parsed.isEmpty()) {
            invalidResponses.increment();
        }

        RawResponse eventResponse = limitRawResponse(rawResponse);
        AsyncModerationResponseEvent event = new AsyncModerationResponseEvent(
                candidate.requestId(),
                ModuleType.CHAT,
                candidate.playerId(),
                candidate.playerName(),
                candidate.message(),
                candidate.entropy(),
                eventResponse.content(),
                eventResponse.truncated(),
                parsed.orElse(null)
        );

        if (!isCurrent(state) || !event.callEvent()) {
            return;
        }
        LlmChatModerationResult result = event.getResult();
        if (result == null || !shouldEnforce(state.settings(), result)) {
            return;
        }

        SchedulingUtils.runForOnlinePlayer(candidate.playerId(), player -> {
            if (!isCurrent(state)) {
                return;
            }
            violationReporter.reportLlm(player, candidate.message(), result);
            enforcedResponses.increment();
        });
    }

    private boolean shouldEnforce(LlmSettings settings, LlmChatModerationResult result) {
        return result.confidence() >= settings.minimumConfidence()
                && settings.enforcedCategories().contains(result.category());
    }

    private boolean isCurrent(RuntimeState state) {
        return !closed && runtime == state && generation.get() == state.generation();
    }

    private void clearPlayer(UUID playerId) {
        inFlightGenerations.remove(playerId);
        cooldowns.remove(playerId);
    }

    private static RawResponse limitRawResponse(String response) {
        if (response.getBytes(StandardCharsets.UTF_8).length <= LlmModerationResponseParser.MAX_RAW_RESPONSE_BYTES) {
            return new RawResponse(response, false);
        }

        StringBuilder limited = new StringBuilder();
        int bytes = 0;
        for (int offset = 0; offset < response.length();) {
            int codePoint = response.codePointAt(offset);
            String codePointText = new String(Character.toChars(codePoint));
            int codePointBytes = codePointText.getBytes(StandardCharsets.UTF_8).length;
            if (bytes + codePointBytes > LlmModerationResponseParser.MAX_RAW_RESPONSE_BYTES) {
                break;
            }
            limited.appendCodePoint(codePoint);
            bytes += codePointBytes;
            offset += Character.charCount(codePoint);
        }
        return new RawResponse(limited.toString(), true);
    }

    record LlmSettings(
            String baseUrl,
            String apiKey,
            String modelName,
            int requestTimeoutSeconds,
            int maxOutputTokens,
            double temperature,
            boolean logResponses,
            int maxConcurrentRequests,
            int queueCapacity,
            int perPlayerCooldownSeconds,
            int minimumMessageCodePoints,
            int maximumMessageCodePoints,
            double minimumEntropyBits,
            double minimumConfidence,
            Set<LlmModerationCategory> enforcedCategories,
            String serverContext
    ) {
        static LlmSettings from(PaperConfigurationService configuration) {
            Set<LlmModerationCategory> categories = new LinkedHashSet<>();
            List<String> configuredCategories = configuration.get(PluginSettings.AI_ENFORCED_CATEGORIES);
            for (String category : configuredCategories) {
                categories.add(LlmModerationCategory.fromWireName(category));
            }
            return new LlmSettings(
                    configuration.get(PluginSettings.AI_BASE_URL).trim(),
                    resolveApiKey(configuration),
                    configuration.get(PluginSettings.AI_MODEL_NAME).trim(),
                    configuration.get(PluginSettings.AI_REQUEST_TIMEOUT_SECONDS),
                    configuration.get(PluginSettings.AI_MAX_OUTPUT_TOKENS),
                    configuration.get(PluginSettings.AI_TEMPERATURE),
                    configuration.get(PluginSettings.AI_LOG_RESPONSES),
                    configuration.get(PluginSettings.AI_MAX_CONCURRENT_REQUESTS),
                    configuration.get(PluginSettings.AI_QUEUE_CAPACITY),
                    configuration.get(PluginSettings.AI_PLAYER_COOLDOWN_SECONDS),
                    configuration.get(PluginSettings.AI_MINIMUM_MESSAGE_CODE_POINTS),
                    configuration.get(PluginSettings.AI_MAXIMUM_MESSAGE_CODE_POINTS),
                    configuration.get(PluginSettings.AI_MINIMUM_ENTROPY_BITS),
                    configuration.get(PluginSettings.AI_MINIMUM_CONFIDENCE),
                    Set.copyOf(categories),
                    Objects.requireNonNullElse(configuration.get(PluginSettings.AI_SERVER_CONTEXT), "")
            );
        }

        private static String resolveApiKey(PaperConfigurationService configuration) {
            String environmentName = Objects.requireNonNullElse(configuration.get(PluginSettings.AI_API_KEY_ENVIRONMENT), "").trim();
            if (!environmentName.isEmpty()) {
                String environmentValue = System.getenv(environmentName);
                if (environmentValue != null && !environmentValue.isBlank()) {
                    return environmentValue;
                }
            }
            return Objects.requireNonNullElse(configuration.get(PluginSettings.AI_API_KEY), "").trim();
        }
    }

    private record RuntimeState(long generation, LlmSettings settings, LlmChatClient client, ThreadPoolExecutor executor) {
    }

    private record Candidate(
            UUID requestId,
            UUID playerId,
            String playerName,
            String message,
            double entropy,
            CooldownReservation reservation
    ) {
    }

    private record CooldownReservation(long generation, long deadlineMillis) {
    }

    private record RawResponse(String content, boolean truncated) {
    }
}
