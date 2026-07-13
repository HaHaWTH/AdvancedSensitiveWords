package io.wdsj.asw.velocity.config;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.github.thatsmusic99.configurationmaster.api.ConfigSection;
import io.wdsj.asw.common.environment.PluginBuildInfo;
import io.wdsj.asw.velocity.AdvancedSensitiveWords;

import java.io.File;
import java.util.List;
import java.util.Map;

public class Config {

    private final ConfigFile config;
    private final AdvancedSensitiveWords plugin;
    public final boolean check_for_update;
    public final boolean velocity_sync_enabled;
    public final String velocity_sync_host;
    public final int velocity_sync_port;
    public final String velocity_sync_secret;
    public final List<String> velocity_sync_allowed_server_ids;
    public final int velocity_sync_heartbeat_timeout_seconds;
    public final long velocity_sync_reset_interval_minutes;

    public Config(AdvancedSensitiveWords plugin, File dataFolder) throws Exception {
        this.plugin = plugin;
        this.config = ConfigFile.loadConfig(new File(dataFolder, "config.yml"));
        config.set("plugin-version", PluginBuildInfo.VERSION);
        structureConfig();
        this.check_for_update = getBoolean("plugin.check-update", true, """
                If set to true, will check for update on plugin startup.""");
        this.velocity_sync_enabled = getBoolean("velocity-sync.enabled", false, """
                Whether to start the Velocity WebSocket server for cross-server VL synchronization.""");
        this.velocity_sync_host = getString("velocity-sync.websocket.host", "127.0.0.1", """
                WebSocket host to bind. Keep 127.0.0.1 unless backend servers connect from other machines.""");
        this.velocity_sync_port = getInt("velocity-sync.websocket.port", 28645, """
                WebSocket port for Paper backend connections.""");
        this.velocity_sync_secret = getString("velocity-sync.websocket.secret", "change-me", """
                Shared HMAC secret. Change this before enabling the sync server.""");
        this.velocity_sync_allowed_server_ids = getList("velocity-sync.websocket.allowed-server-ids", List.of(), """
                Allowed Paper backend server ids. Leave empty to allow any authenticated server id.""");
        this.velocity_sync_heartbeat_timeout_seconds = getInt("velocity-sync.websocket.heartbeat-timeout-seconds", 30, """
                Seconds without heartbeat before a backend connection is closed.""");
        this.velocity_sync_reset_interval_minutes = getLong("velocity-sync.violation-reset-time", 20L, """
                Proxy-owned VL reset interval in minutes while Velocity sync is enabled.""");
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
        defaultKeyValue.forEach((string, object) -> config.addExample(path + "." + string, object));
        return config.getConfigSection(path);
    }

    public ConfigSection getConfigSection(String path, Map<String, Object> defaultKeyValue, String comment) {
        config.addDefault(path, null, comment);
        config.makeSectionLenient(path);
        defaultKeyValue.forEach((string, object) -> config.addExample(path + "." + string, object));
        return config.getConfigSection(path);
    }

    public void addComment(String path, String comment) {
        config.addComment(path, comment);
    }
}
