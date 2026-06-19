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
        Files.writeString(file, "Plugin:\n  enableDefaultWords: false\n");

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
        assertTrue(output.contains("model-name: deepseek-v4-flash"));
        assertTrue(output.contains("log-responses: false"));
        assertFalse(output.contains("Plugin:"));
        assertFalse(output.contains("enableDefaultWords"));
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
        assertFalse(output.contains("messageOnChat"));
    }
}
