package io.wdsj.asw.bungee;

import io.wdsj.asw.bungee.listener.PluginMessageListener;
import net.md_5.bungee.api.plugin.Plugin;

public final class AdvancedSensitiveWords extends Plugin {
    public static final String BUNGEE_CHANNEL = "BungeeCord";
    public static final String SUB_CHANNEL = "asw";

    private static AdvancedSensitiveWords instance;
    public static AdvancedSensitiveWords getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        getProxy().getPluginManager().registerListener(this, new PluginMessageListener());
    }

    @Override
    public void onDisable() {
        getProxy().getPluginManager().unregisterListener(new PluginMessageListener());
    }
}
