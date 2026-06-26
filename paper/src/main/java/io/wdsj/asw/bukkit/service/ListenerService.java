package io.wdsj.asw.bukkit.service;

import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.ai.LlmChatDetectionService;
import io.wdsj.asw.bukkit.integration.packetevents.sign.SignFakeViewCompat;
import io.wdsj.asw.bukkit.integration.trchat.TrChatCompat;
import io.wdsj.asw.bukkit.listener.*;
import io.wdsj.asw.bukkit.listener.compatibility.CompatibilityChecker;
import io.wdsj.asw.bukkit.listener.paper.PaperChatListener;
import io.wdsj.asw.bukkit.listener.paper.PaperFakeMessageExecutor;
import io.wdsj.asw.bukkit.setting.PaperConfigurationService;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class ListenerService {
    private final AdvancedSensitiveWords plugin;
    private final PaperConfigurationService configuration;
    private final LlmChatDetectionService llmChatDetectionService;

    public ListenerService(AdvancedSensitiveWords plugin) {
        this.plugin = plugin;
        this.configuration = plugin.getConfigurationService();
        this.llmChatDetectionService = new LlmChatDetectionService(configuration);
    }

    public void registerListeners() {
        if (!configuration.get(PluginSettings.DISABLE_COMPATIBILITY_CHECKER)) {
            registerEventListener(new CompatibilityChecker());
        }
        TrChatCompat.tryRegister(plugin);
        registerChatBookEventListeners();
        registerEventListener(new ShadowListener(configuration));
        registerEventListener(new AltsListener(configuration));
        registerEventListener(new PaperFakeMessageExecutor(configuration));
        if (configuration.get(PluginSettings.ENABLE_SIGN_EDIT_CHECK)) {
            registerEventListener(new SignListener(configuration));
        }
        SignFakeViewCompat.tryRegister(plugin);
        if (configuration.get(PluginSettings.ENABLE_ANVIL_EDIT_CHECK)) {
            registerEventListener(new AnvilListener(configuration));
        }
        if (configuration.get(PluginSettings.ENABLE_PLAYER_NAME_CHECK)) {
            registerEventListener(new PlayerLoginListener(configuration));
        }
        if (configuration.get(PluginSettings.ENABLE_PLAYER_ITEM_CHECK)) {
            registerEventListener(new PlayerItemListener(configuration));
        }
        if (configuration.get(PluginSettings.CHAT_BROADCAST_CHECK)) {
            registerEventListener(new BroadcastListener(configuration));
        }
        if (configuration.get(PluginSettings.CLEAN_PLAYER_DATA_CACHE)) {
            registerEventListener(new QuitDataCleaner(configuration));
        }
        if (configuration.get(PluginSettings.CHECK_FOR_UPDATE)) {
            registerEventListener(new JoinUpdateNotifier(configuration));
        }
    }

    public void unregisterListeners() {
        llmChatDetectionService.close();
        SignFakeViewCompat.unregister();
        HandlerList.unregisterAll(plugin);
    }

    public void reloadConfiguration() {
        llmChatDetectionService.reload();
    }

    public LlmChatDetectionService getLlmChatDetectionService() {
        return llmChatDetectionService;
    }
    
    
    private void registerEventListener(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    private void registerChatBookEventListeners() {
        if (configuration.get(PluginSettings.ENABLE_CHAT_CHECK)) {
            registerEventListener(llmChatDetectionService);
            registerEventListener(new PaperChatListener(configuration, llmChatDetectionService));
            registerEventListener(new CommandListener(configuration));
        }
        if (configuration.get(PluginSettings.ENABLE_BOOK_EDIT_CHECK)) {
            registerEventListener(new BookListener(configuration));
        }
    }

}
