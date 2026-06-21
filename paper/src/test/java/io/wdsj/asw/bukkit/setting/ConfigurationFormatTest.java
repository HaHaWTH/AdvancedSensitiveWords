package io.wdsj.asw.bukkit.setting;

import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurationStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationFormatTest {
    private static final YamlConfigurationProperties PROPERTIES = YamlConfigurationProperties.newBuilder()
            .setNameFormatter(NameFormatters.LOWER_KEBAB_CASE)
            .build();

    @TempDir
    Path temporaryDirectory;

    @Test
    void resetsLegacyCamelCaseSettingsToKebabCaseDefaults() throws IOException {
        Path file = temporaryDirectory.resolve("config.yml");
        Files.writeString(file, "Plugin:\n  enableDefaultWords: false\n  punishment:\n    - legacy-action\n");

        SettingsConfiguration settings = new YamlConfigurationStore<>(SettingsConfiguration.class, PROPERTIES).update(file);
        String output = Files.readString(file);

        assertTrue(settings.plugin.enableDefaultWords);
        assertTrue(output.contains("plugin:"));
        assertTrue(output.contains("# General plugin settings."));
        assertTrue(output.contains("enable-default-words: true"));
        assertTrue(output.contains("# Whether to load the bundled default word dictionaries."));
        assertTrue(output.contains("method: REPLACE"));
        assertTrue(output.contains("ai:"));
        assertTrue(output.contains("base-url: https://api.deepseek.com"));
        assertTrue(output.contains("api-mode: CHAT_COMPLETIONS"));
        assertTrue(output.contains("anthropic-version: 2023-06-01"));
        assertTrue(output.contains("anthropic-thinking-enabled: false"));
        assertTrue(output.contains("server-context-can-override: false"));
        assertTrue(output.contains("model-name: deepseek-v4-flash"));
        assertTrue(output.contains("log-responses: false"));
        assertTrue(output.contains("category-policy:"));
        assertTrue(output.contains("sexual-minors:"));
        assertTrue(output.contains("notify-confidence: 0.75"));
        assertTrue(output.contains("punish-confidence: 0.9"));
        assertTrue(output.contains("manual-punishment: []"));
        assertTrue(output.contains("VL conditions in this manual punishment list use the player's total violations across all punishment modules."));
        assertTrue(output.contains("COMMAND|command; COMMAND_PROXY|command; DAMAGE|amount; HOSTILE|radius."));
        assertTrue(output.contains("Use %player% or %PLAYER% in commands. Append VL=3, VL>3, or VL<3 to condition an action."));
        assertTrue(output.contains("punishment: []"));
        assertFalse(output.contains("Plugin:"));
        assertFalse(output.contains("enableDefaultWords"));
        assertFalse(output.contains("legacy-action"));
        assertFalse(output.contains("minimum-confidence:"));
        assertFalse(output.contains("enforced-categories:"));
        assertFalse(output.contains("punishment-confidence:"));
    }

    @Test
    void writesChineseMessagesAsUtf8KebabCaseYaml() throws IOException {
        Path file = temporaryDirectory.resolve("messages_zhcn.yml");
        ChineseMessagesConfiguration messages =
                new YamlConfigurationStore<>(ChineseMessagesConfiguration.class, PROPERTIES).update(file);
        String output = Files.readString(file);

        assertTrue(messages.chat.messageOnChat.contains("请勿"));
        assertTrue(output.contains("message-on-chat:"));
        assertTrue(output.contains("# Message sent when chat or command content is blocked."));
        assertTrue(output.contains("请勿在聊天中发送敏感词汇"));
        assertTrue(output.contains("message-on-ai-status:"));
        assertTrue(output.contains("<gradient:#22d3ee:#4ade80>"));
        assertTrue(output.contains("%chat_violation%"));
        assertTrue(output.contains("%ai_violation%"));
        assertTrue(output.contains("%api_mode%"));
        assertTrue(output.contains("%module%"));
        assertFalse(output.contains("messageOnChat"));
    }

    @Test
    void rejectsAnUnknownLlmApiMode() throws IOException {
        Path file = temporaryDirectory.resolve("invalid-api-mode.yml");
        Files.writeString(file, "ai:\n  api-mode: INVALID\n");

        assertThrows(RuntimeException.class,
                () -> new YamlConfigurationStore<>(SettingsConfiguration.class, PROPERTIES).update(file));
    }
}
