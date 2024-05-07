package io.wdsj.asw.bukkit;

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
import io.wdsj.asw.bukkit.command.ConstructCommandExecutor;
import io.wdsj.asw.bukkit.command.ConstructTabCompleter;
import io.wdsj.asw.bukkit.datasource.DatabaseManager;
import io.wdsj.asw.bukkit.listener.*;
import io.wdsj.asw.bukkit.listener.packet.ASWPacketListener;
import io.wdsj.asw.bukkit.listener.packet.ProtocolLibListener;
import io.wdsj.asw.bukkit.manage.placeholder.ASWExpansion;
import io.wdsj.asw.bukkit.manage.punish.PlayerShadowController;
import io.wdsj.asw.bukkit.method.*;
import io.wdsj.asw.bukkit.proxy.bungee.BungeeCordChannel;
import io.wdsj.asw.bukkit.proxy.bungee.BungeeReceiver;
import io.wdsj.asw.bukkit.proxy.velocity.VelocityChannel;
import io.wdsj.asw.bukkit.proxy.velocity.VelocityReceiver;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.update.Updater;
import io.wdsj.asw.bukkit.util.TimingUtils;
import io.wdsj.asw.bukkit.util.cache.BookCache;
import io.wdsj.asw.bukkit.util.context.ChatContext;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static io.wdsj.asw.bukkit.util.TimingUtils.cleanStatisticCache;
import static io.wdsj.asw.bukkit.util.Utils.*;


public final class AdvancedSensitiveWords extends JavaPlugin {
    public static boolean isInitialized = false;
    public static SensitiveWordBs sensitiveWordBs;
    private final File CONFIG_FILE = new File(getDataFolder(), "config.yml");
    public static boolean isAuthMeAvailable;
    public static boolean isCslAvailable;
    public static SettingsManager settingsManager;
    public static SettingsManager messagesManager;
    public static DatabaseManager databaseManager;
    private static AdvancedSensitiveWords instance;
    private static boolean USE_PE = false;
    private static TaskScheduler scheduler;
    public static Logger LOGGER;
    public static TaskScheduler getScheduler() {
        return scheduler;
    }

    public static AdvancedSensitiveWords getInstance() {
        return instance;
    }
    @Override
    public void onLoad() {
        LOGGER = getLogger();
        instance = this;
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
        databaseManager = new DatabaseManager();
        if (!canUsePE() ||
                settingsManager.getProperty(PluginSettings.DETECTION_MODE).equalsIgnoreCase("event")) return;
        USE_PE = true;
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().reEncodeByDefault(true).checkForUpdates(false).bStats(false);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        LOGGER.info("Initializing DFA dict...");
        long startTime = System.currentTimeMillis();
        if (settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) databaseManager.setupDataSource();
        cleanStatisticCache();
        scheduler = UniversalScheduler.getScheduler(this);
        doInitTasks();
        if (settingsManager.getProperty(PluginSettings.PURGE_LOG_FILE)) purgeLog();
        if (!settingsManager.getProperty(PluginSettings.DETECTION_MODE).equalsIgnoreCase("event")) {
            if (USE_PE) {
                PacketEvents.getAPI().getEventManager().registerListener(new ASWPacketListener());
                PacketEvents.getAPI().init();
            } else {
                LOGGER.info("ProtocolLib v4 or older detected, enabling compatibility mode.");
                ProtocolLibListener.addAlternateListener();
            }
        } else {
            getServer().getPluginManager().registerEvents(new ChatListener(), this);
            getServer().getPluginManager().registerEvents(new CommandListener(), this);
        }
        Objects.requireNonNull(getCommand("advancedsensitivewords")).setExecutor(new ConstructCommandExecutor());
        Objects.requireNonNull(getCommand("asw")).setExecutor(new ConstructCommandExecutor());
        Objects.requireNonNull(getCommand("advancedsensitivewords")).setTabCompleter(new ConstructTabCompleter());
        Objects.requireNonNull(getCommand("asw")).setTabCompleter(new ConstructTabCompleter());
        int pluginId = 20661;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new SimplePie("default_list", () -> String.valueOf(settingsManager.getProperty(PluginSettings.ENABLE_DEFAULT_WORDS))));
        metrics.addCustomChart(new SimplePie("mode", () -> settingsManager.getProperty(PluginSettings.DETECTION_MODE).equalsIgnoreCase("event") ? "Event" : canUsePE() ? "Fast" : "Compatibility"));
        metrics.addCustomChart(new SimplePie("java_vendor", TimingUtils::getJvmVendor));
        getServer().getPluginManager().registerEvents(new ShadowListener(), this);
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
                LOGGER.info("BroadcastMessage is not available, please disable chat broadcast check in config.yml");
            }
        }
        if (settingsManager.getProperty(PluginSettings.FLUSH_PLAYER_DATA_CACHE)) {
            getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
        }
        if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, VelocityChannel.CHANNEL);
            getServer().getMessenger().registerIncomingPluginChannel(this, VelocityChannel.CHANNEL, new VelocityReceiver());
        }
        if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, BungeeCordChannel.BUNGEE_CHANNEL);
            getServer().getMessenger().registerIncomingPluginChannel(this, BungeeCordChannel.BUNGEE_CHANNEL, new BungeeReceiver());
        }
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") &&
            settingsManager.getProperty(PluginSettings.ENABLE_PLACEHOLDER)) {
            new ASWExpansion().register();
            LOGGER.info("Placeholders registered.");
        }
        long endTime = System.currentTimeMillis();
        LOGGER.info("AdvancedSensitiveWords is enabled!(took " + (endTime - startTime) + "ms)");
        if (settingsManager.getProperty(PluginSettings.CHECK_FOR_UPDATE)) {
            getScheduler().runTaskAsynchronously(() -> {
                Updater updater = new Updater(getDescription().getVersion());
                if (updater.isUpdateAvailable()) {
                    LOGGER.warning("There is a new version available: " + updater.getLatestVersion() +
                            ", you're on: " + updater.getCurrentVersion());
                }
            });
        }
    }


    public void doInitTasks() {
        isAuthMeAvailable = Bukkit.getPluginManager().getPlugin("AuthMe") != null;
        isCslAvailable = Bukkit.getPluginManager().getPlugin("CatSeedLogin") != null;
        IWordAllow wA = WordAllows.chains(WordAllows.defaults(), new WordAllow(), new ExternalWordAllow());
        AtomicReference<IWordDeny> wD = new AtomicReference<>();
        isInitialized = false;
        sensitiveWordBs = null;
        if (settingsManager.getProperty(PluginSettings.ENABLE_DATABASE) &&
                (databaseManager.getDataSource() == null || databaseManager.getDataSource().isClosed())) {
            databaseManager.setupDataSource();
        }
        getScheduler().runTaskAsynchronously(() -> {
            if (settingsManager.getProperty(PluginSettings.ENABLE_DEFAULT_WORDS) && settingsManager.getProperty(PluginSettings.ENABLE_ONLINE_WORDS)) {
                wD.set(WordDenys.chains(WordDenys.defaults(), new WordDeny(), new OnlineWordDeny(), new ExternalWordDeny()));
            } else if (settingsManager.getProperty(PluginSettings.ENABLE_DEFAULT_WORDS)) {
                wD.set(WordDenys.chains(new WordDeny(), WordDenys.defaults(), new ExternalWordDeny()));
            } else if (settingsManager.getProperty(PluginSettings.ENABLE_ONLINE_WORDS)) {
                wD.set(WordDenys.chains(new OnlineWordDeny(), new WordDeny(), new ExternalWordDeny()));
            } else {
                wD.set(WordDenys.chains(new WordDeny(), new ExternalWordDeny()));
            }
            sensitiveWordBs = SensitiveWordBs.newInstance().ignoreCase(settingsManager.getProperty(PluginSettings.IGNORE_CASE)).ignoreWidth(settingsManager.getProperty(PluginSettings.IGNORE_WIDTH)).ignoreNumStyle(settingsManager.getProperty(PluginSettings.IGNORE_NUM_STYLE)).ignoreChineseStyle(settingsManager.getProperty(PluginSettings.IGNORE_CHINESE_STYLE)).ignoreEnglishStyle(settingsManager.getProperty(PluginSettings.IGNORE_ENGLISH_STYLE)).ignoreRepeat(settingsManager.getProperty(PluginSettings.IGNORE_REPEAT)).enableNumCheck(settingsManager.getProperty(PluginSettings.ENABLE_NUM_CHECK)).enableEmailCheck(settingsManager.getProperty(PluginSettings.ENABLE_EMAIL_CHECK)).enableUrlCheck(settingsManager.getProperty(PluginSettings.ENABLE_URL_CHECK)).enableWordCheck(settingsManager.getProperty(PluginSettings.ENABLE_WORD_CHECK)).wordResultCondition(settingsManager.getProperty(PluginSettings.FORCE_ENGLISH_FULL_MATCH) ? WordResultConditions.englishWordMatch() : WordResultConditions.alwaysTrue()).wordDeny(wD.get()).wordAllow(wA).numCheckLen(settingsManager.getProperty(PluginSettings.NUM_CHECK_LEN)).wordReplace(new WordReplace()).wordTag(WordTags.none()).charIgnore(new CharIgnore()).init();
            isInitialized = true;
        });
    }

    @Override
    public void onDisable() {
        if (!settingsManager.getProperty(PluginSettings.DETECTION_MODE).equalsIgnoreCase("event")) {
            if (USE_PE) {
                PacketEvents.getAPI().terminate();
            } else {
                com.comphenix.protocol.ProtocolLibrary.getProtocolManager().removePacketListeners(this);
            }
        }
        if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD) ||
                settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
            getServer().getMessenger().unregisterOutgoingPluginChannel(this);
            getServer().getMessenger().unregisterIncomingPluginChannel(this);
        }
        TimingUtils.cleanStatisticCache();
        ChatContext.forceClearContext();
        PlayerShadowController.clear();
        if (settingsManager.getProperty(PluginSettings.BOOK_CACHE)) {
            BookCache.invalidateAll();
        }
        if (isInitialized) sensitiveWordBs.destroy();
        HandlerList.unregisterAll(this);
        Objects.requireNonNull(getCommand("advancedsensitivewords")).setExecutor(null);
        Objects.requireNonNull(getCommand("asw")).setExecutor(null);
        Objects.requireNonNull(getCommand("advancedsensitivewords")).setTabCompleter(null);
        Objects.requireNonNull(getCommand("asw")).setTabCompleter(null);
        databaseManager.closeDataSource();
        LOGGER.info("AdvancedSensitiveWords is disabled!");
    }
}
