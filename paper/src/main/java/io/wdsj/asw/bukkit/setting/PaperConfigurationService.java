package io.wdsj.asw.bukkit.setting;

import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurationStore;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Owns the Paper configuration snapshots and their ConfigLib stores.
 */
public final class PaperConfigurationService {
    private static final YamlConfigurationProperties PROPERTIES = YamlConfigurationProperties.newBuilder()
            .setNameFormatter(NameFormatters.LOWER_KEBAB_CASE)
            .header("AdvancedSensitiveWords configuration. All keys use kebab-case.")
            .build();

    private final Logger logger;
    private final Path dataDirectory;
    private final YamlConfigurationStore<SettingsConfiguration> settingsStore =
            new YamlConfigurationStore<>(SettingsConfiguration.class, PROPERTIES);

    private volatile SettingsConfiguration settings;
    private volatile MessagesConfiguration messages;

    public PaperConfigurationService(Logger logger, Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    public void load() {
        try {
            SettingsConfiguration loadedSettings = settingsStore.update(dataDirectory.resolve("config.yml"));
            MessagesConfiguration loadedMessages = loadMessages(loadedSettings.plugin.language);
            settings = loadedSettings;
            messages = loadedMessages;
        } catch (RuntimeException exception) {
            logger.error("Failed to load AdvancedSensitiveWords configuration from {}.", dataDirectory, exception);
            throw new IllegalStateException("Unable to load AdvancedSensitiveWords configuration", exception);
        }
    }

    public void reload() {
        load();
    }

    public <T> T get(SettingKey<T> key) {
        SettingsConfiguration snapshot = settings;
        if (snapshot == null) {
            throw new IllegalStateException("Configuration has not been loaded yet");
        }
        return key.get(snapshot);
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
            case UPDATE_AVAILABLE -> snapshot.plugin.updateAvailable;
            case MESSAGE_ON_PLAYER_INFO -> snapshot.plugin.messageOnCommandInfo;
            case MESSAGE_ON_COMMAND_RESET -> snapshot.plugin.messageOnCommandReset;
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

    private String normalizeLanguage(String configuredLanguage) {
        String language = configuredLanguage == null ? "" : configuredLanguage.trim().toLowerCase(Locale.ROOT);
        if ("en".equals(language) || "zhcn".equals(language)) {
            return language;
        }
        logger.warn("Unsupported plugin language '{}'; falling back to English.", configuredLanguage);
        return "en";
    }
}
