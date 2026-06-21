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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import io.wdsj.asw.bukkit.setting.SettingsConfiguration;

/** Coordinates asynchronous, single-message LLM moderation requests. */
public final class LlmChatDetectionService implements Listener, AutoCloseable {
    private final PaperConfigurationService configuration;
    private final ViolationReporter violationReporter;
    private final Function<LlmSettings, LlmChatClient> clientFactory;
    private final LlmHistoryLogger historyLogger;
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
        this(configuration, new ViolationReporter(configuration), CompatibleLlmChatClient::new,
                new LlmHistoryLogger(configuration.dataDirectory()));
    }

    LlmChatDetectionService(
            PaperConfigurationService configuration,
            ViolationReporter violationReporter,
            Function<LlmSettings, LlmChatClient> clientFactory,
            LlmHistoryLogger historyLogger
    ) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.violationReporter = Objects.requireNonNull(violationReporter, "violationReporter");
        this.clientFactory = Objects.requireNonNull(clientFactory, "clientFactory");
        this.historyLogger = Objects.requireNonNull(historyLogger, "historyLogger");
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
        int visibleCodePoints = io.wdsj.asw.common.utils.ChatEntropy.visibleCodePointCount(message);
        if (visibleCodePoints < settings.minimumMessageCodePoints()) {
            return;
        }

        double entropy = io.wdsj.asw.common.utils.ChatEntropy.shannonEntropyBits(message);
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

    /** Returns a snapshot view of the configured AI moderation runtime and its lifetime counters. */
    public LlmRuntimeStatus runtimeStatus() {
        RuntimeState state = runtime;
        ThreadPoolExecutor executor = state == null ? null : state.executor();
        return new LlmRuntimeStatus(
                configuration.get(PluginSettings.AI_ENABLED),
                submittedRequests.sum(),
                droppedRequests.sum(),
                failedRequests.sum(),
                invalidResponses.sum(),
                enforcedResponses.sum(),
                executor == null ? 0 : executor.getActiveCount(),
                executor == null ? 0 : executor.getQueue().size(),
                executor == null ? 0 : executor.getPoolSize(),
                configuration.get(PluginSettings.AI_MODEL_NAME),
                configuration.get(PluginSettings.AI_API_MODE),
                Map.copyOf(toCategoryPolicies(configuration.get(PluginSettings.AI_CATEGORY_POLICY)))
        );
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
        historyLogger.close();
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
            historyLogger.logRequest(
                    candidate.requestId(),
                    candidate.playerId(),
                    candidate.playerName(),
                    state.settings().modelName(),
                    state.settings().apiMode(),
                    candidate.entropy(),
                    userMessage
            );
            String systemPrompt = LlmModerationPrompt.createSystemPrompt(
                    state.settings().serverContext(),
                    state.settings().serverContextCanOverride()
            );
            String rawResponse = state.client().classify(systemPrompt, userMessage);
            handleResponse(state, candidate, rawResponse == null ? "" : rawResponse, !isCurrent(state));
        } catch (RuntimeException exception) {
            historyLogger.logFailure(candidate.requestId(), exception.getClass());
            if (isCurrent(state)) {
                failedRequests.increment();
                AdvancedSensitiveWords.LOGGER.debug("LLM moderation request failed for player {}.", candidate.playerId());
            }
        } finally {
            inFlightGenerations.remove(candidate.playerId(), state.generation());
        }
    }

    private void handleResponse(RuntimeState state, Candidate candidate, String rawResponse, boolean stale) {
        if (state.settings().logResponses()) {
            AdvancedSensitiveWords.LOGGER.info(
                    "LLM moderation response [requestId={}, player={}]: {}",
                    candidate.requestId(),
                    candidate.playerName(),
                    rawResponse
            );
        }
        Optional<LlmChatModerationResult> parsed = LlmModerationResponseParser.parse(rawResponse);
        if (parsed.isEmpty() && !stale) {
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

        if (stale || !isCurrent(state)) {
            historyLogger.logResponse(candidate.requestId(), rawResponse, parsed.orElse(null), null,
                    false, null, false, false, true);
            return;
        }

        event.callEvent();
        LlmChatModerationResult result = event.getResult();
        LlmCategoryPolicy policy = result == null ? null : state.settings().categoryPolicy().get(result.category());
        boolean eligibleForNotification = !event.isCancelled() && policy != null && policy.shouldNotify(result);
        boolean eligibleForEnforcement = !event.isCancelled() && policy != null && policy.shouldPunish(result);
        historyLogger.logResponse(candidate.requestId(), rawResponse, parsed.orElse(null), result,
                event.isCancelled(), policy, eligibleForNotification, eligibleForEnforcement, false);
        if (!eligibleForNotification && !eligibleForEnforcement) {
            return;
        }

        SchedulingUtils.runForOnlinePlayer(candidate.playerId(), player -> {
            if (!isCurrent(state)) {
                return;
            }
            if (eligibleForEnforcement) {
                violationReporter.reportLlm(player, candidate.message(), result, eligibleForNotification);
                enforcedResponses.increment();
                return;
            }
            violationReporter.reportLlmObservation(player, candidate.message(), result);
        });
    }

    static boolean isEligibleForNotification(
            Map<LlmModerationCategory, LlmCategoryPolicy> categoryPolicy,
            LlmChatModerationResult result
    ) {
        LlmCategoryPolicy policy = categoryPolicy.get(result.category());
        return policy != null && policy.shouldNotify(result);
    }

    static boolean isEligibleForPunishment(
            Map<LlmModerationCategory, LlmCategoryPolicy> categoryPolicy,
            LlmChatModerationResult result
    ) {
        LlmCategoryPolicy policy = categoryPolicy.get(result.category());
        return policy != null && policy.shouldPunish(result);
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

    private static Map<LlmModerationCategory, LlmCategoryPolicy> toCategoryPolicies(
            Map<String, SettingsConfiguration.Ai.CategoryPolicy> configuredPolicies
    ) {
        Map<LlmModerationCategory, LlmCategoryPolicy> policies = new java.util.EnumMap<>(LlmModerationCategory.class);
        for (LlmModerationCategory category : LlmModerationCategory.values()) {
            SettingsConfiguration.Ai.CategoryPolicy policy = configuredPolicies.get(category.configurationKey());
            policies.put(category, new LlmCategoryPolicy(policy.notifyConfidence, policy.punishConfidence));
        }
        return policies;
    }

    record LlmSettings(
            String baseUrl,
            LlmApiMode apiMode,
            String anthropicVersion,
            boolean anthropicThinkingEnabled,
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
            Map<LlmModerationCategory, LlmCategoryPolicy> categoryPolicy,
            String serverContext,
            boolean serverContextCanOverride
    ) {
        static LlmSettings from(PaperConfigurationService configuration) {
            return new LlmSettings(
                    configuration.get(PluginSettings.AI_BASE_URL).trim(),
                    configuration.get(PluginSettings.AI_API_MODE),
                    configuration.get(PluginSettings.AI_ANTHROPIC_VERSION).trim(),
                    configuration.get(PluginSettings.AI_ANTHROPIC_THINKING_ENABLED),
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
                    Map.copyOf(toCategoryPolicies(configuration.get(PluginSettings.AI_CATEGORY_POLICY))),
                    Objects.requireNonNullElse(configuration.get(PluginSettings.AI_SERVER_CONTEXT), ""),
                    configuration.get(PluginSettings.AI_SERVER_CONTEXT_CAN_OVERRIDE)
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

    /** Immutable runtime data consumed by administrative status commands. */
    public record LlmRuntimeStatus(
            boolean enabled,
            long submittedRequests,
            long droppedRequests,
            long failedRequests,
            long invalidResponses,
            long enforcedResponses,
            int activeRequests,
            int queuedRequests,
            int poolSize,
            String modelName,
            LlmApiMode apiMode,
            Map<LlmModerationCategory, LlmCategoryPolicy> categoryPolicy
    ) {
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
