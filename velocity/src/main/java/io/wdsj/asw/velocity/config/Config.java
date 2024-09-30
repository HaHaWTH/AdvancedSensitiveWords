package io.wdsj.asw.velocity.config;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.github.thatsmusic99.configurationmaster.api.ConfigSection;
import io.wdsj.asw.common.template.PluginVersionTemplate;
import io.wdsj.asw.velocity.AdvancedSensitiveWords;

import java.io.File;
import java.util.List;
import java.util.Map;

public class Config {

    private final ConfigFile config;
    private final AdvancedSensitiveWords plugin;
    public final boolean check_for_update;

    public Config(AdvancedSensitiveWords plugin, File dataFolder) throws Exception {
        this.plugin = plugin;
        // Load config.yml with ConfigMaster
        this.config = ConfigFile.loadConfig(new File(dataFolder, "config.yml"));
        config.set("plugin-version", PluginVersionTemplate.VERSION);

        // Pre-structure to force order
        structureConfig();

        this.check_for_update = getBoolean("plugin.check-update", true, """
                If set to true, will check for update on plugin startup.""");
    }

    public void saveConfig() {
        try {
            config.save();
        } catch (Exception e) {
            this.plugin.getLogger().error("Failed to save config file", e);
        }
    }

    private void structureConfig() {
        createTitledSection("Plugin general setting", "plugin");
    }

    public void createTitledSection(String title, String path) {
        config.addSection(title);
        config.addDefault(path, null);
    }

    public boolean getBoolean(String path, boolean def, String comment) {
        config.addDefault(path, def, comment);
        return config.getBoolean(path, def);
    }

    public boolean getBoolean(String path, boolean def) {
        config.addDefault(path, def);
        return config.getBoolean(path, def);
    }

    public String getString(String path, String def, String comment) {
        config.addDefault(path, def, comment);
        return config.getString(path, def);
    }

    public String getString(String path, String def) {
        config.addDefault(path, def);
        return config.getString(path, def);
    }

    public double getDouble(String path, double def, String comment) {
        config.addDefault(path, def, comment);
        return config.getDouble(path, def);
    }

    public double getDouble(String path, double def) {
        config.addDefault(path, def);
        return config.getDouble(path, def);
    }

    public int getInt(String path, int def, String comment) {
        config.addDefault(path, def, comment);
        return config.getInteger(path, def);
    }

    public int getInt(String path, int def) {
        config.addDefault(path, def);
        return config.getInteger(path, def);
    }

    public long getLong(String path, long def, String comment) {
        config.addDefault(path, def, comment);
        return config.getLong(path, def);
    }

    public long getLong(String path, long def) {
        config.addDefault(path, def);
        return config.getLong(path, def);
    }

    public List<String> getList(String path, List<String> def, String comment) {
        config.addDefault(path, def, comment);
        return config.getStringList(path);
    }

    public List<String> getList(String path, List<String> def) {
        config.addDefault(path, def);
        return config.getStringList(path);
    }

    public ConfigSection getConfigSection(String path, Map<String, Object> defaultKeyValue) {
        config.addDefault(path, null);
        config.makeSectionLenient(path);
        defaultKeyValue.forEach((string, object) -> config.addExample(path+"."+string, object));
        return config.getConfigSection(path);
    }

    public ConfigSection getConfigSection(String path, Map<String, Object> defaultKeyValue, String comment) {
        config.addDefault(path, null, comment);
        config.makeSectionLenient(path);
        defaultKeyValue.forEach((string, object) -> config.addExample(path+"."+string, object));
        return config.getConfigSection(path);
    }

    public void addComment(String path, String comment) {
        config.addComment(path, comment);
    }
}