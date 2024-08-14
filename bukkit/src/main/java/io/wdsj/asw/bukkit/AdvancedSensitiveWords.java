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
import io.wdsj.asw.bukkit.ai.OllamaProcessor;
import io.wdsj.asw.bukkit.ai.OpenAIProcessor;
import io.wdsj.asw.bukkit.command.ConstructCommandExecutor;
import io.wdsj.asw.bukkit.command.ConstructTabCompleter;
import io.wdsj.asw.bukkit.datasource.DatabaseManager;
import io.wdsj.asw.bukkit.integration.placeholder.ASWExpansion;
import io.wdsj.asw.bukkit.integration.voicechat.VoiceChatHookService;
import io.wdsj.asw.bukkit.listener.*;
import io.wdsj.asw.bukkit.listener.packet.ASWBookPacketListener;
import io.wdsj.asw.bukkit.listener.packet.ASWChatPacketListener;
import io.wdsj.asw.bukkit.manage.punish.PlayerAltController;
import io.wdsj.asw.bukkit.manage.punish.PlayerShadowController;
import io.wdsj.asw.bukkit.method.*;
import io.wdsj.asw.bukkit.proxy.bungee.BungeeCordChannel;
import io.wdsj.asw.bukkit.proxy.bungee.BungeeReceiver;
import io.wdsj.asw.bukkit.proxy.velocity.VelocityChannel;
import io.wdsj.asw.bukkit.proxy.velocity.VelocityReceiver;
import io.wdsj.asw.bukkit.service.BukkitLibraryService;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.update.Updater;
import io.wdsj.asw.bukkit.util.TimingUtils;
import io.wdsj.asw.bukkit.util.cache.BookCache;
import io.wdsj.asw.bukkit.util.context.ChatContext;
import io.wdsj.asw.bukkit.util.context.SignContext;
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
    public static final String PLUGIN_VERSION = "X";
    private static AdvancedSensitiveWords instance;
    private static boolean USE_PE = false;
    private static TaskScheduler scheduler;
    private static boolean isEventMode = false;
    public static Logger LOGGER;
    private BukkitLibraryService libraryService;
    private static final OllamaProcessor OLLAMA_PROCESSOR = new OllamaProcessor();
    private static final OpenAIProcessor OPENAI_PROCESSOR = new OpenAIProcessor();
    private VoiceChatHookService voiceChatHookService;
    public static TaskScheduler getScheduler() {
        return scheduler;
    }

    public static AdvancedSensitiveWords getInstance() {
        return instance;
    }
    public static OllamaProcessor getOllamaProcessor() {
        return OLLAMA_PROCESSOR;
    }
    public static OpenAIProcessor getOpenAIProcessor() {
        return OPENAI_PROCESSOR;
    }
    public static boolean isEventMode() {
        return isEventMode;
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
        libraryService = new BukkitLibraryService(this);
        isEventMode = settingsManager.getProperty(PluginSettings.DETECTION_MODE).equalsIgnoreCase("event");
        if (canUsePE() &&
                !isEventMode) {
            USE_PE = true;
        }
    }

    @Override
    public void onEnable() {
        LOGGER.info("Loading libraries...");
        long startTime = System.currentTimeMillis();
        libraryService.load();
        LOGGER.info("Initializing DFA system...");
        if (settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) databaseManager.setupDataSource();
        cleanStatisticCache();
        scheduler = UniversalScheduler.getScheduler(this);
        doInitTasks();
        if (settingsManager.getProperty(PluginSettings.PURGE_LOG_FILE)) purgeLog();
        if (!isEventMode) {
            if (USE_PE) {
                try {
                    if (settingsManager.getProperty(PluginSettings.ENABLE_CHAT_CHECK)) {
                        PacketEvents.getAPI().getEventManager().registerListener(ASWChatPacketListener.class.getConstructor().newInstance());
                    }
                    if (settingsManager.getProperty(PluginSettings.ENABLE_BOOK_EDIT_CHECK)) {
                        PacketEvents.getAPI().getEventManager().registerListener(ASWBookPacketListener.class.getConstructor().newInstance());
                    }
                } catch (Exception e) {
                    LOGGER.severe("Failed to register packetevents listener." +
                            " This should not happen, please report to the author");
                    LOGGER.severe(e.getMessage());
                }
                PacketEvents.getAPI().init();
            } else {
                LOGGER.warning("Cannot use packetevents, using event mode instead.");
                registerEventBasedListener();
            }
        } else {
            registerEventBasedListener();
        }
        Objects.requireNonNull(getCommand("advancedsensitivewords")).setExecutor(new ConstructCommandExecutor());
        Objects.requireNonNull(getCommand("asw")).setExecutor(new ConstructCommandExecutor());
        Objects.requireNonNull(getCommand("advancedsensitivewords")).setTabCompleter(new ConstructTabCompleter());
        Objects.requireNonNull(getCommand("asw")).setTabCompleter(new ConstructTabCompleter());
        int pluginId = 20661;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new SimplePie("default_list", () -> String.valueOf(settingsManager.getProperty(PluginSettings.ENABLE_DEFAULT_WORDS))));
        metrics.addCustomChart(new SimplePie("java_vendor", TimingUtils::getJvmVendor));
        getServer().getPluginManager().registerEvents(new ShadowListener(), this);
        getServer().getPluginManager().registerEvents(new AltsListener(), this);
        if (settingsManager.getProperty(PluginSettings.ENABLE_OLLAMA_AI_MODEL_CHECK)) {
            OLLAMA_PROCESSOR.initService(settingsManager.getProperty(PluginSettings.OLLAMA_AI_API_ADDRESS), settingsManager.getProperty(PluginSettings.OLLAMA_AI_MODEL_NAME), settingsManager.getProperty(PluginSettings.AI_MODEL_TIMEOUT), settingsManager.getProperty(PluginSettings.OLLAMA_AI_DEBUG_LOG));
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_OPENAI_AI_MODEL_CHECK)) {
            OPENAI_PROCESSOR.initService(settingsManager.getProperty(PluginSettings.OPENAI_API_KEY), settingsManager.getProperty(PluginSettings.OPENAI_DEBUG_LOG));
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_SIGN_EDIT_CHECK)) {
            getServer().getPluginManager().registerEvents(new SignListener(), this);
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_ANVIL_EDIT_CHECK)) {
            getServer().getPluginManager().registerEvents(new AnvilListener(), this);
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_NAME_CHECK)) {
            getServer().getPluginManager().registerEvents(new PlayerLoginListener(), this);
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_ITEM_CHECK)) {
            getServer().getPluginManager().registerEvents(new PlayerItemListener(), this);
        }
        if (settingsManager.getProperty(PluginSettings.CHAT_BROADCAST_CHECK)) {
            if (isClassLoaded("org.bukkit.event.server.BroadcastMessageEvent")) {
                getServer().getPluginManager().registerEvents(new BroadCastListener(), this);
            } else {
                LOGGER.info("BroadcastMessage is not available, please disable chat broadcast check in config.yml");
            }
        }
        if (settingsManager.getProperty(PluginSettings.CLEAN_PLAYER_DATA_CACHE)) {
            getServer().getPluginManager().registerEvents(new QuitDataCleaner(), this);
        }
        if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, VelocityChannel.CHANNEL);
            getServer().getMessenger().registerIncomingPluginChannel(this, VelocityChannel.CHANNEL, new VelocityReceiver());
        }
        if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, BungeeCordChannel.BUNGEE_CHANNEL);
            getServer().getMessenger().registerIncomingPluginChannel(this, BungeeCordChannel.BUNGEE_CHANNEL, new BungeeReceiver());
        }
        if (settingsManager.getProperty(PluginSettings.CHECK_FOR_UPDATE)) {
            getServer().getPluginManager().registerEvents(new JoinUpdateNotifier(), this);
        }
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") &&
            settingsManager.getProperty(PluginSettings.ENABLE_PLACEHOLDER)) {
            new ASWExpansion().register();
            LOGGER.info("Placeholders registered.");
        }
        if (Bukkit.getPluginManager().isPluginEnabled("voicechat") &&
            settingsManager.getProperty(PluginSettings.HOOK_SIMPLE_VOICE_CHAT)) {
            voiceChatHookService = new VoiceChatHookService(this);
            voiceChatHookService.register();
        }
        long endTime = System.currentTimeMillis();
        LOGGER.info("AdvancedSensitiveWords is enabled!(took " + (endTime - startTime) + "ms)");
        if (settingsManager.getProperty(PluginSettings.CHECK_FOR_UPDATE)) {
            getScheduler().runTaskAsynchronously(() -> {
                Updater updater = new Updater(getDescription().getVersion());
                if (updater.isUpdateAvailable()) {
                    LOGGER.warning("There is a new version available: " + Updater.getLatestVersion() +
                            ", you're on: " + Updater.getCurrentVersion());
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
            sensitiveWordBs = SensitiveWordBs.newInstance().ignoreCase(settingsManager.getProperty(PluginSettings.IGNORE_CASE)).ignoreWidth(settingsManager.getProperty(PluginSettings.IGNORE_WIDTH)).ignoreNumStyle(settingsManager.getProperty(PluginSettings.IGNORE_NUM_STYLE)).ignoreChineseStyle(settingsManager.getProperty(PluginSettings.IGNORE_CHINESE_STYLE)).ignoreEnglishStyle(settingsManager.getProperty(PluginSettings.IGNORE_ENGLISH_STYLE)).ignoreRepeat(settingsManager.getProperty(PluginSettings.IGNORE_REPEAT)).enableNumCheck(settingsManager.getProperty(PluginSettings.ENABLE_NUM_CHECK)).enableEmailCheck(settingsManager.getProperty(PluginSettings.ENABLE_EMAIL_CHECK)).enableUrlCheck(settingsManager.getProperty(PluginSettings.ENABLE_URL_CHECK)).enableWordCheck(settingsManager.getProperty(PluginSettings.ENABLE_WORD_CHECK)).wordResultCondition(settingsManager.getProperty(PluginSettings.FORCE_ENGLISH_FULL_MATCH) ? WordResultConditions.englishWordMatch() : WordResultConditions.alwaysTrue()).wordDeny(wD.get()).wordAllow(wA).numCheckLen(settingsManager.getProperty(PluginSettings.NUM_CHECK_LEN)).wordReplace(new WordReplace()).wordTag(WordTags.none()).charIgnore(new CharIgnore()).enableIpv4Check(settingsManager.getProperty(PluginSettings.ENABLE_IP_CHECK)).init();
            isInitialized = true;
        });
    }

    @Override
    public void onDisable() {
        if (!isEventMode) {
            if (USE_PE) {
                PacketEvents.getAPI().terminate();
            }
        }
        if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD) ||
                settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
            getServer().getMessenger().unregisterOutgoingPluginChannel(this);
            getServer().getMessenger().unregisterIncomingPluginChannel(this);
        }
        HandlerList.unregisterAll(this);
        TimingUtils.cleanStatisticCache();
        ChatContext.forceClearContext();
        SignContext.forceClearContext();
        PlayerShadowController.clear();
        PlayerAltController.clear();
        OLLAMA_PROCESSOR.shutdown();
        OPENAI_PROCESSOR.shutdown();
        if (voiceChatHookService != null) {
            voiceChatHookService.unregister();
        }
        if (settingsManager.getProperty(PluginSettings.BOOK_CACHE)) {
            BookCache.invalidateAll();
        }
        if (isInitialized) sensitiveWordBs.destroy();
        Objects.requireNonNull(getCommand("advancedsensitivewords")).setExecutor(null);
        Objects.requireNonNull(getCommand("asw")).setExecutor(null);
        Objects.requireNonNull(getCommand("advancedsensitivewords")).setTabCompleter(null);
        Objects.requireNonNull(getCommand("asw")).setTabCompleter(null);
        databaseManager.closeDataSource();
        LOGGER.info("AdvancedSensitiveWords is disabled!");
    }

    private void registerEventBasedListener() {
        if (settingsManager.getProperty(PluginSettings.ENABLE_CHAT_CHECK)) {
            getServer().getPluginManager().registerEvents(new ChatListener(), this);
            getServer().getPluginManager().registerEvents(new CommandListener(), this);
            getServer().getPluginManager().registerEvents(new FakeMessageExecutor(), this);
        }
        if (settingsManager.getProperty(PluginSettings.ENABLE_BOOK_EDIT_CHECK)) {
            getServer().getPluginManager().registerEvents(new BookListener(), this);
        }
    }
}
