package io.wdsj.asw.bukkit.service;

import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.integration.sign.SignFakeViewCompat;
import io.wdsj.asw.bukkit.integration.trchat.TrChatCompat;
import io.wdsj.asw.bukkit.listener.*;
import io.wdsj.asw.bukkit.listener.paper.PaperChatListener;
import io.wdsj.asw.bukkit.listener.paper.PaperFakeMessageExecutor;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.*;
import static io.wdsj.asw.bukkit.util.Utils.isClassLoaded;

public class ListenerService {
    private final AdvancedSensitiveWords plugin;

    public ListenerService(AdvancedSensitiveWords plugin) {
        this.plugin = plugin;
    }

    public void registerListeners() {
        TrChatCompat.tryRegister(plugin);
        registerChatBookEventListeners();
        registerEventListener(ShadowListener.class);
        registerEventListener(AltsListener.class);
        registerEventListener(PaperFakeMessageExecutor.class);
        if (settingsManager.getProperty(PluginSettings.ENABLE_SIGN_EDIT_CHECK)) {
            registerEventListener(SignListener.class);
        }
        SignFakeViewCompat.tryRegister(plugin);
        if (settingsManager.getProperty(PluginSettings.ENABLE_ANVIL_EDIT_CHECK)) {
            registerEventListener(AnvilListener.class);
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_NAME_CHECK)) {
            registerEventListener(PlayerLoginListener.class);
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_ITEM_CHECK)) {
            registerEventListener(PlayerItemListener.class);
        }
        if (settingsManager.getProperty(PluginSettings.CHAT_BROADCAST_CHECK)) {
            if (isClassLoaded("org.bukkit.event.server.BroadcastMessageEvent")) {
                registerEventListener(BroadCastListener.class);
            } else {
                LOGGER.info("BroadcastMessage is not available, please disable chat broadcast check in config.yml");
            }
        }
        if (settingsManager.getProperty(PluginSettings.CLEAN_PLAYER_DATA_CACHE)) {
            registerEventListener(QuitDataCleaner.class);
        }
        if (settingsManager.getProperty(PluginSettings.CHECK_FOR_UPDATE)) {
            registerEventListener(JoinUpdateNotifier.class);
        }
    }

    public void unregisterListeners() {
        SignFakeViewCompat.unregister();
        HandlerList.unregisterAll(plugin);
    }
    
    
    private void registerEventListener(Class<? extends Listener> listenerClass) {
        Bukkit.getPluginManager().registerEvents(newListenerWithNoArgConstructor(listenerClass), plugin);
    }

    private Listener newListenerWithNoArgConstructor(Class<? extends Listener> listenerClass) {
        try {
            return listenerClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to register listener " + listenerClass.getSimpleName());
        }
    }

    private void registerChatBookEventListeners() {
        if (settingsManager.getProperty(PluginSettings.ENABLE_CHAT_CHECK)) {
            registerEventListener(PaperChatListener.class);
            registerEventListener(CommandListener.class);
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_BOOK_EDIT_CHECK)) {
            registerEventListener(BookListener.class);
        }
    }

}
