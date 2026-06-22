package io.wdsj.asw.bukkit.setting;

import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurationStore;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.ai.LlmApiMode;
import io.wdsj.asw.bukkit.api.moderation.LlmModerationCategory;
import io.wdsj.asw.bukkit.listener.command.CommandArgumentRuleSet;
import org.slf4j.Logger;

import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Owns the Paper configuration snapshots and their ConfigLib stores.
 */
public final class PaperConfigurationService {
    private static final YamlConfigurationProperties PROPERTIES = YamlConfigurationProperties.newBuilder()
            .setNameFormatter(NameFormatters.LOWER_KEBAB_CASE)
            .header("AdvancedSensitiveWords configuration. (Version " + AdvancedSensitiveWords.PLUGIN_VERSION + ")")
            .build();

    private final Logger logger;
    private final Path dataDirectory;
    private final YamlConfigurationStore<SettingsConfiguration> settingsStore =
            new YamlConfigurationStore<>(SettingsConfiguration.class, PROPERTIES);

    private volatile SettingsConfiguration settings;
    private volatile MessagesConfiguration messages;
    private volatile CommandArgumentRuleSet commandArgumentRules = CommandArgumentRuleSet.compile(List.of());

    public PaperConfigurationService(Logger logger, Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    public void load() {
        try {
            SettingsConfiguration loadedSettings = settingsStore.update(dataDirectory.resolve("config.yml"));
            validateSettings(loadedSettings);
            CommandArgumentRuleSet loadedCommandArgumentRules = CommandArgumentRuleSet.compile(loadedSettings.chat.commandWhiteList);
            MessagesConfiguration loadedMessages = loadMessages(loadedSettings.plugin.language);
            settings = loadedSettings;
            messages = loadedMessages;
            commandArgumentRules = loadedCommandArgumentRules;
        } catch (RuntimeException exception) {
            logger.error("Failed to load AdvancedSensitiveWords configuration from {}.", dataDirectory, exception);
            throw new IllegalStateException("Unable to load AdvancedSensitiveWords configuration", exception);
        }
    }

    public void reload() {
        load();
    }

    public Path dataDirectory() {
        return dataDirectory;
    }

    public <T> T get(SettingKey<T> key) {
        SettingsConfiguration snapshot = settings;
        if (snapshot == null) {
            throw new IllegalStateException("Configuration has not been loaded yet");
        }
        return key.get(snapshot);
    }

    public CommandArgumentRuleSet commandArgumentRules() {
        return commandArgumentRules;
    }

    public boolean shouldInspectCommand(CommandArgumentRuleSet.CommandSelection selection) {
        return shouldInspectCommand(selection.listed(), get(PluginSettings.CHAT_INVERT_WHITELIST));
    }

    static boolean shouldInspectCommand(boolean commandListed, boolean invertedWhitelist) {
        return commandListed == invertedWhitelist;
    }

    public String message(PluginMessages key) {
        MessagesConfiguration snapshot = messages;
        if (snapshot == null) {
            throw new IllegalStateException("Messages have not been loaded yet");
        }
        return switch (key) {
            case MESSAGE_ON_CHAT -> snapshot.chat.messageOnChat;
            case MESSAGE_ON_SIGN -> snapshot.sign.messageOnSign;
            case MESSAGE_ON_ANVIL_RENAME -> snapshot.anvil.messageOnAnvilRename;
            case MESSAGE_ON_BOOK -> snapshot.book.messageOnBook;
            case MESSAGE_ON_NAME -> snapshot.name.messageOnName;
            case MESSAGE_ON_ITEM -> snapshot.item.messageOnItem;
            case MESSAGE_ON_COMMAND_RELOAD -> snapshot.plugin.messageOnCommandReload;
            case MESSAGE_ON_VIOLATION_RESET -> snapshot.plugin.messageOnViolationReset;
            case MESSAGE_ON_COMMAND_STATUS -> snapshot.plugin.messageOnCommandStatus;
            case MESSAGE_ON_AI_STATUS -> snapshot.plugin.messageOnAiStatus;
            case MESSAGE_ON_COMMAND_TEST -> snapshot.plugin.commandTest.testResultTrue;
            case MESSAGE_ON_COMMAND_TEST_PASS -> snapshot.plugin.commandTest.testResultPass;
            case MESSAGE_ON_COMMAND_TEST_NOT_INIT -> snapshot.plugin.commandTest.testNotInit;
            case MESSAGE_ON_COMMAND_PUNISH_PARSE_ERROR -> snapshot.plugin.commandPunish.parseError;
            case MESSAGE_ON_COMMAND_PUNISH_SUCCESS -> snapshot.plugin.commandPunish.success;
            case MESSAGE_ON_COMMAND_ADD_SUCCESS -> snapshot.plugin.commandAdd.success;
            case MESSAGE_ON_COMMAND_REMOVE_SUCCESS -> snapshot.plugin.commandRemove.success;
            case MESSAGE_ON_COMMAND_RUNTIME_ONLY -> snapshot.plugin.commandWord.runtimeOnly;
            case NO_PERMISSION -> snapshot.plugin.noPermission;
            case UNKNOWN_COMMAND -> snapshot.plugin.unknownCommand;
            case NOT_ENOUGH_ARGS -> snapshot.plugin.argsNotEnough;
            case PLAYER_NOT_FOUND -> snapshot.plugin.noSuchPlayer;
            case ADMIN_REMINDER -> snapshot.plugin.noticeOperator;
            case ADMIN_REMINDER_PROXY -> snapshot.plugin.noticeOperatorProxy;
            case AI_OBSERVATION -> snapshot.plugin.aiObservation;
            case AI_OBSERVATION_PROXY -> snapshot.plugin.aiObservationProxy;
            case UPDATE_AVAILABLE -> snapshot.plugin.updateAvailable;
            case MESSAGE_ON_PLAYER_INFO -> snapshot.plugin.messageOnCommandInfo;
            case MESSAGE_ON_COMMAND_RESET -> snapshot.plugin.messageOnCommandReset;
            case INVALID_VIOLATION_MODULE -> snapshot.plugin.invalidViolationModule;
        };
    }

    private MessagesConfiguration loadMessages(String configuredLanguage) {
        String language = normalizeLanguage(configuredLanguage);
        Path messageFile = dataDirectory.resolve("messages_" + language + ".yml");
        if ("zhcn".equals(language)) {
            YamlConfigurationStore<ChineseMessagesConfiguration> store =
                    new YamlConfigurationStore<>(ChineseMessagesConfiguration.class, PROPERTIES);
            return store.update(messageFile);
        }
        YamlConfigurationStore<EnglishMessagesConfiguration> store =
                new YamlConfigurationStore<>(EnglishMessagesConfiguration.class, PROPERTIES);
        return store.update(messageFile);
    }

    static void validateSettings(SettingsConfiguration settings) {
        SettingsConfiguration.Ai ai = settings.ai;
        if (ai == null) {
            throw new IllegalArgumentException("ai settings cannot be null");
        }
        if (ai.apiMode == null) {
            throw new IllegalArgumentException("ai.api-mode cannot be null");
        }
        if (ai.requestTimeoutSeconds < 1) {
            throw new IllegalArgumentException("ai.request-timeout-seconds must be at least 1");
        }
        if (ai.maxOutputTokens < 1) {
            throw new IllegalArgumentException("ai.max-output-tokens must be at least 1");
        }
        if (!Double.isFinite(ai.temperature) || ai.temperature < 0.0D) {
            throw new IllegalArgumentException("ai.temperature must be a finite non-negative number");
        }
        if (ai.maxConcurrentRequests < 1 || ai.queueCapacity < 1) {
            throw new IllegalArgumentException("ai.max-concurrent-requests and ai.queue-capacity must be at least 1");
        }
        if (ai.perPlayerCooldownSeconds < 0) {
            throw new IllegalArgumentException("ai.per-player-cooldown-seconds cannot be negative");
        }
        if (ai.minimumMessageCodePoints < 1 || ai.maximumMessageCodePoints < ai.minimumMessageCodePoints) {
            throw new IllegalArgumentException("ai message code point bounds are invalid");
        }
        if (!Double.isFinite(ai.minimumEntropyBits) || ai.minimumEntropyBits < 0.0D) {
            throw new IllegalArgumentException("ai.minimum-entropy-bits must be a finite non-negative number");
        }
        validateCategoryPolicy(ai.categoryPolicy);
        CommandArgumentRuleSet.compile(settings.chat.commandWhiteList);

        if (!ai.enabled) {
            return;
        }

        validateHttpUrl(ai.baseUrl, "ai.base-url");
        if (ai.apiMode == LlmApiMode.ANTHROPIC_MESSAGES) {
            requireText(ai.anthropicVersion, "ai.anthropic-version");
        }
        requireText(ai.modelName, "ai.model-name");
        if (resolveApiKey(ai).isBlank()) {
            throw new IllegalArgumentException("AI is enabled but no API key is configured");
        }
    }

    private static void validateCategoryPolicy(Map<String, SettingsConfiguration.Ai.CategoryPolicy> policies) {
        if (policies == null) {
            throw new IllegalArgumentException("ai.category-policy cannot be null");
        }

        Set<String> expectedKeys = Arrays.stream(LlmModerationCategory.values())
                .map(LlmModerationCategory::configurationKey)
                .collect(Collectors.toUnmodifiableSet());
        if (!policies.keySet().equals(expectedKeys)) {
            throw new IllegalArgumentException("ai.category-policy must configure every LLM category exactly once");
        }

        for (LlmModerationCategory category : LlmModerationCategory.values()) {
            SettingsConfiguration.Ai.CategoryPolicy policy = policies.get(category.configurationKey());
            if (policy == null) {
                throw new IllegalArgumentException("Missing ai.category-policy value for " + category.configurationKey());
            }
            validateCategoryThreshold(policy.notifyConfidence, category, "notify-confidence");
            validateCategoryThreshold(policy.punishConfidence, category, "punish-confidence");
            if (policy.punishment == null) {
                throw new IllegalArgumentException("Missing ai.category-policy."
                        + category.configurationKey() + ".punishment");
            }
            if (category == LlmModerationCategory.CLEAN
                    && (policy.notifyConfidence != -1.0D || policy.punishConfidence != -1.0D || !policy.punishment.isEmpty())) {
                throw new IllegalArgumentException("ai.category-policy.clean must keep both thresholds at -1.0 and no punishment actions");
            }
        }
    }

    private static void validateCategoryThreshold(double threshold, LlmModerationCategory category, String thresholdName) {
        if (!Double.isFinite(threshold) || (threshold != -1.0D && (threshold < 0.0D || threshold > 1.0D))) {
            throw new IllegalArgumentException("Invalid ai.category-policy." + category.configurationKey() + "." + thresholdName);
        }
    }

    private static void validateHttpUrl(String value, String settingName) {
        requireText(value, settingName);
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(settingName + " must be a valid HTTP(S) URL", exception);
        }
        if ((!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) {
            throw new IllegalArgumentException(settingName + " must use HTTP or HTTPS");
        }
    }

    private static String resolveApiKey(SettingsConfiguration.Ai ai) {
        String environmentName = ai.apiKeyEnvironment == null ? "" : ai.apiKeyEnvironment.trim();
        if (!environmentName.isEmpty()) {
            String environmentValue = System.getenv(environmentName);
            if (environmentValue != null && !environmentValue.isBlank()) {
                return environmentValue;
            }
        }
        return ai.apiKey == null ? "" : ai.apiKey.trim();
    }

    private static String requireText(String value, String settingName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(settingName + " cannot be blank");
        }
        return value.trim();
    }

    private String normalizeLanguage(String configuredLanguage) {
        String language = configuredLanguage == null ? "" : configuredLanguage.trim().toLowerCase(Locale.ROOT);
        if ("en".equals(language) || "zhcn".equals(language)) {
            return language;
        }
        logger.warn("Unsupported plugin language '{}'; falling back to English.", configuredLanguage);
        return "en";
    }
}
