package io.wdsj.asw;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.github.houbb.sensitive.word.api.IWordAllow;
import com.github.houbb.sensitive.word.api.IWordDeny;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.allow.WordAllows;
import com.github.houbb.sensitive.word.support.deny.WordDenys;
import com.github.houbb.sensitive.word.support.resultcondition.WordResultConditions;
import com.github.houbb.sensitive.word.support.tag.WordTags;
import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.wdsj.asw.command.ConstructCommandExecutor;
import io.wdsj.asw.command.ConstructTabCompleter;
import io.wdsj.asw.listener.*;
import io.wdsj.asw.listener.packet.ASWPacketListener;
import io.wdsj.asw.listener.packet.ProtocolLibListener;
import io.wdsj.asw.method.*;
import io.wdsj.asw.setting.PluginMessages;
import io.wdsj.asw.setting.PluginSettings;
import io.wdsj.asw.util.TimingUtils;
import io.wdsj.asw.util.cache.BookCache;
import io.wdsj.asw.util.context.ChatContext;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.net.ProxySelector;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static io.wdsj.asw.util.TimingUtils.cleanStatisticCache;
import static io.wdsj.asw.util.Utils.*;


public final class AdvancedSensitiveWords extends JavaPlugin {
    public static boolean isInitialized = false;
    public static SensitiveWordBs sensitiveWordBs;
    private final File CONFIG_FILE = new File(getDataFolder(), "config.yml");
    public static boolean isAuthMeAvailable;
    public static boolean isCslAvailable;
    public static SettingsManager settingsManager;
    public static SettingsManager messagesManager;
    private static AdvancedSensitiveWords instance;
    private static TaskScheduler scheduler;

    public static TaskScheduler getScheduler() {
        return scheduler;
    }

    public static AdvancedSensitiveWords getInstance() {
        return instance;
    }
    @Override
    public void onLoad() {
        settingsManager = SettingsManagerBuilder
                .withYamlFile(CONFIG_FILE)
                .configurationData(PluginSettings.class)
                .useDefaultMigrationService()
                .create();
        File msgFile = new File(getDataFolder(), "messages_" + settingsManager.getProperty(PluginSettings.PLUGIN_LANGUAGE) +
                ".yml");
        if (!msgFile.exists()) {
            saveResource("messages_" + settingsManager.getProperty(PluginSettings.PLUGIN_LANGUAGE) + ".yml", false);
        }
        messagesManager = SettingsManagerBuilder
                .withYamlFile(msgFile)
                .configurationData(PluginMessages.class)
                .useDefaultMigrationService()
                .create();
        if (!checkProtocolLib()) return;
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().reEncodeByDefault(true).checkForUpdates(false).bStats(false);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        getLogger().info("Initializing DFA dict...");
        long startTime = System.currentTimeMillis();
        instance = this;
        cleanStatisticCache();
        scheduler = UniversalScheduler.getScheduler(this);
        doInitTasks();
        if (settingsManager.getProperty(PluginSettings.PURGE_LOG_FILE)) purgeLog();
        if (checkProtocolLib()) {
            PacketEvents.getAPI().getEventManager().registerListener(new ASWPacketListener());
            PacketEvents.getAPI().init();
        } else {
            getLogger().info("ProtocolLib v4 or older detected, enabling compatibility mode.");
            ProtocolLibListener.addAlternateListener();
        }
        Objects.requireNonNull(getCommand("advancedsensitivewords")).setExecutor(new ConstructCommandExecutor());
        Objects.requireNonNull(getCommand("asw")).setExecutor(new ConstructCommandExecutor());
        Objects.requireNonNull(getCommand("advancedsensitivewords")).setTabCompleter(new ConstructTabCompleter());
        Objects.requireNonNull(getCommand("asw")).setTabCompleter(new ConstructTabCompleter());
        int pluginId = 20661;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new SimplePie("default_list", () -> String.valueOf(settingsManager.getProperty(PluginSettings.ENABLE_DEFAULT_WORDS))));
        metrics.addCustomChart(new SimplePie("mode", () -> checkProtocolLib() ? "Fast" : "Compatibility"));
        metrics.addCustomChart(new SimplePie("java_vendor", TimingUtils::getJvmVendor));
        if (settingsManager.getProperty(PluginSettings.ENABLE_SIGN_EDIT_CHECK)) {
            getServer().getPluginManager().registerEvents(new SignListener(), this);
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_ANVIL_EDIT_CHECK)) {
            getServer().getPluginManager().registerEvents(new AnvilListener(), this);
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_BOOK_EDIT_CHECK)) {
            getServer().getPluginManager().registerEvents(new BookListener(), this);
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_NAME_CHECK)) {
            getServer().getPluginManager().registerEvents(new PlayerLoginListener(), this);
        }
        if (settingsManager.getProperty(PluginSettings.CHAT_BROADCAST_CHECK)) {
            if (isClassLoaded("org.bukkit.event.server.BroadcastMessageEvent")) {
                getServer().getPluginManager().registerEvents(new BroadCastListener(), this);
            } else {
                getLogger().info("BroadcastMessage is not available, please disable chat broadcast check in config.yml");
            }
        }
        long endTime = System.currentTimeMillis();
        getLogger().info("AdvancedSensitiveWords is enabled!(took " + (endTime - startTime) + "ms)");
        // bro, don't bytecode this, you can just disable it in the config TAT
        if (Math.random() < 0.1 && !settingsManager.getProperty(PluginSettings.DISABLE_DONATION)) {
            getLogger().info("This plugin takes over 600 hours to develop and optimize, if you think it's nice, consider" +
                    " support: https://afdian.net/a/114514woxiuyuan/");
        }
    }


    public void doInitTasks() {
        isAuthMeAvailable = Bukkit.getPluginManager().getPlugin("AuthMe") != null;
        isCslAvailable = Bukkit.getPluginManager().getPlugin("CatSeedLogin") != null;
        IWordAllow wA = WordAllows.chains(WordAllows.defaults(), new WordAllow());
        AtomicReference<IWordDeny> wD = new AtomicReference<>();
        isInitialized = false;
        sensitiveWordBs = null;
        ProxySelector.setDefault(null);
        getScheduler().runTaskAsynchronously(() -> {
            if (settingsManager.getProperty(PluginSettings.ENABLE_DEFAULT_WORDS) && settingsManager.getProperty(PluginSettings.ENABLE_ONLINE_WORDS)) {
                wD.set(WordDenys.chains(WordDenys.defaults(), new WordDeny(), new OnlineWordDeny()));
            } else if (settingsManager.getProperty(PluginSettings.ENABLE_DEFAULT_WORDS)) {
                wD.set(WordDenys.chains(new WordDeny(), WordDenys.defaults()));
            } else if (settingsManager.getProperty(PluginSettings.ENABLE_ONLINE_WORDS)) {
                wD.set(WordDenys.chains(new OnlineWordDeny(), new WordDeny()));
            } else {
                wD.set(new WordDeny());
            }
            // Full async reload
            sensitiveWordBs = SensitiveWordBs.newInstance().ignoreCase(settingsManager.getProperty(PluginSettings.IGNORE_CASE)).ignoreWidth(settingsManager.getProperty(PluginSettings.IGNORE_WIDTH)).ignoreNumStyle(settingsManager.getProperty(PluginSettings.IGNORE_NUM_STYLE)).ignoreChineseStyle(settingsManager.getProperty(PluginSettings.IGNORE_CHINESE_STYLE)).ignoreEnglishStyle(settingsManager.getProperty(PluginSettings.IGNORE_ENGLISH_STYLE)).ignoreRepeat(settingsManager.getProperty(PluginSettings.IGNORE_REPEAT)).enableNumCheck(settingsManager.getProperty(PluginSettings.ENABLE_NUM_CHECK)).enableEmailCheck(settingsManager.getProperty(PluginSettings.ENABLE_EMAIL_CHECK)).enableUrlCheck(settingsManager.getProperty(PluginSettings.ENABLE_URL_CHECK)).enableWordCheck(settingsManager.getProperty(PluginSettings.ENABLE_WORD_CHECK)).wordResultCondition(settingsManager.getProperty(PluginSettings.FORCE_ENGLISH_FULL_MATCH) ? WordResultConditions.englishWordMatch() : WordResultConditions.alwaysTrue()).wordDeny(wD.get()).wordAllow(wA).numCheckLen(settingsManager.getProperty(PluginSettings.NUM_CHECK_LEN)).wordReplace(new WordReplace()).wordTag(WordTags.none()).charIgnore(new CharIgnore()).init();
            isInitialized = true;
        });
    }
    public static String getIgnoreFormatCodeRegex() {
        return "[§" + settingsManager.getProperty(PluginSettings.ALT_COLOR_CODE) + "][0-9A-Fa-fK-Ok-oRr]";
    }

    @Override
    public void onDisable() {
        if (checkProtocolLib()) {
            PacketEvents.getAPI().terminate();
        } else {
            com.comphenix.protocol.ProtocolLibrary.getProtocolManager().removePacketListeners(this);
        }
        TimingUtils.cleanStatisticCache();
        ChatContext.forceClearContext();
        BookCache.forceClearCache();
        HandlerList.unregisterAll(this);
        Objects.requireNonNull(getCommand("advancedsensitivewords")).setExecutor(null);
        Objects.requireNonNull(getCommand("asw")).setExecutor(null);
        Objects.requireNonNull(getCommand("advancedsensitivewords")).setTabCompleter(null);
        Objects.requireNonNull(getCommand("asw")).setTabCompleter(null);
        getLogger().info("AdvancedSensitiveWords is disabled!");
    }
}
