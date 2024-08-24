package io.wdsj.asw.bungee;

import io.wdsj.asw.bungee.listener.PluginMessageListener;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;

import java.util.logging.Logger;

public final class AdvancedSensitiveWords extends Plugin {
    public static final String BUNGEE_CHANNEL = "BungeeCord";
    public static final String SUB_CHANNEL = "asw";
    public static final String PLUGIN_VERSION = "1.0";
    public static Logger LOGGER;

    private static AdvancedSensitiveWords instance;
    public static AdvancedSensitiveWords getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        LOGGER = getLogger();
        instance = this;
        getProxy().getPluginManager().registerListener(this, new PluginMessageListener());
        Metrics metrics = new Metrics(this, 21636);
    }

    @Override
    public void onDisable() {
        getProxy().getPluginManager().unregisterListener(new PluginMessageListener());
    }
}
