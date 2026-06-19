package io.wdsj.asw.bukkit;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.github.houbb.sensitive.word.api.IWordAllow;
import com.github.houbb.sensitive.word.api.IWordDeny;
import com.github.houbb.sensitive.word.api.IWordResultCondition;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.allow.WordAllows;
import com.github.houbb.sensitive.word.support.check.WordChecks;
import com.github.houbb.sensitive.word.support.deny.WordDenys;
import com.github.houbb.sensitive.word.support.resultcondition.WordResultConditions;
import com.github.houbb.sensitive.word.support.tag.WordTags;
import io.wdsj.asw.bukkit.command.ConstructCommandExecutor;
import io.wdsj.asw.bukkit.command.ConstructTabCompleter;
import io.wdsj.asw.bukkit.core.condition.WordResultConditionNumMatch;
import io.wdsj.asw.bukkit.integration.placeholder.ASWExpansion;
import io.wdsj.asw.bukkit.manage.punish.PlayerAltController;
import io.wdsj.asw.bukkit.manage.punish.PlayerShadowController;
import io.wdsj.asw.bukkit.manage.punish.ViolationCounter;
import io.wdsj.asw.bukkit.method.*;
import io.wdsj.asw.bukkit.permission.cache.CachingPermTool;
import io.wdsj.asw.bukkit.proxy.velocity.VelocityChannel;
import io.wdsj.asw.bukkit.proxy.velocity.VelocityReceiver;
import io.wdsj.asw.bukkit.service.ListenerService;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.task.punish.ViolationResetTask;
import io.wdsj.asw.bukkit.util.SchedulingUtils;
import io.wdsj.asw.bukkit.util.TimingUtils;
import io.wdsj.asw.bukkit.util.cache.BookCache;
import io.wdsj.asw.bukkit.util.context.ChatContext;
import io.wdsj.asw.bukkit.util.context.SignContext;
import io.wdsj.asw.common.template.PluginVersionTemplate;
import io.wdsj.asw.common.update.Updater;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.io.File;
import java.util.Objects;

import static io.wdsj.asw.bukkit.util.LoggingUtils.purgeLog;
import static io.wdsj.asw.bukkit.util.TimingUtils.resetStatistics;
import static io.wdsj.asw.bukkit.util.Utils.*;


public final class AdvancedSensitiveWords extends JavaPlugin {
    public static volatile boolean isInitialized = false;
    public static SensitiveWordBs sensitiveWordBs;
    private final File configFile = new File(getDataFolder(), "config.yml");
    public static boolean isAuthMeAvailable;
    public static SettingsManager settingsManager;
    public static SettingsManager messagesManager;
    public static final String PLUGIN_VERSION = PluginVersionTemplate.VERSION;
    private static AdvancedSensitiveWords instance;
    private static TaskScheduler scheduler;
    public static Logger LOGGER;
    private ListenerService listenerService;
    private CachingPermTool permCache;
    public static TaskScheduler getScheduler() {
        return scheduler;
    }

    public static AdvancedSensitiveWords getInstance() {
        return instance;
    }
    private MyScheduledTask violationResetTask;

    @Override
    public void onLoad() {
        LOGGER = getSLF4JLogger();
        instance = this;
        settingsManager = SettingsManagerBuilder
                .withYamlFile(configFile)
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
    }

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        LOGGER.info("Initializing DFA system...");
        resetStatistics();
        scheduler = UniversalScheduler.getScheduler(this);
        permCache = CachingPermTool.enable(this);
        BookCache.initialize();
        doInitTasks();
        if (settingsManager.getProperty(PluginSettings.PURGE_LOG_FILE)) purgeLog();
        listenerService = new ListenerService(this);
        listenerService.registerListeners();
        registerCommands();
        setupMetrics();
        registerVelocityChannel();
        registerPlaceholderExpansion();
        scheduleViolationResetTask();
        long endTime = System.currentTimeMillis();
        LOGGER.info("AdvancedSensitiveWords is enabled!(took {}ms)", endTime - startTime);
        if (Updater.isDevChannel()) {
            LOGGER.info("You are running a development version of AdvancedSensitiveWords! Branch: " + PluginVersionTemplate.COMMIT_BRANCH);
        }
        checkForUpdatesAsync();
    }


    public void doInitTasks() {
        isAuthMeAvailable = Bukkit.getPluginManager().getPlugin("AuthMe") != null;
        IWordAllow wA = WordAllows.chains(WordAllows.defaults(), new WordAllow(), new ExternalWordAllow(this));
        isInitialized = false;
        sensitiveWordBs = null;
        IWordResultCondition condition = createWordResultCondition();
        getScheduler().runTaskAsynchronously(() -> {
            IWordDeny wordDeny = createWordDeny();
            sensitiveWordBs = SensitiveWordBs.newInstance()
                    .ignoreCase(settingsManager.getProperty(PluginSettings.IGNORE_CASE))
                    .ignoreWidth(settingsManager.getProperty(PluginSettings.IGNORE_WIDTH))
                    .ignoreNumStyle(settingsManager.getProperty(PluginSettings.IGNORE_NUM_STYLE))
                    .ignoreChineseStyle(settingsManager.getProperty(PluginSettings.IGNORE_CHINESE_STYLE))
                    .ignoreEnglishStyle(settingsManager.getProperty(PluginSettings.IGNORE_ENGLISH_STYLE))
                    .ignoreRepeat(settingsManager.getProperty(PluginSettings.IGNORE_REPEAT))
                    .enableNumCheck(settingsManager.getProperty(PluginSettings.ENABLE_NUM_CHECK))
                    .enableEmailCheck(settingsManager.getProperty(PluginSettings.ENABLE_EMAIL_CHECK))
                    .enableUrlCheck(settingsManager.getProperty(PluginSettings.ENABLE_URL_CHECK))
                    .enableWordCheck(settingsManager.getProperty(PluginSettings.ENABLE_WORD_CHECK))
                    .wordResultCondition(condition)
                    .wordCheckUrl(settingsManager.getProperty(PluginSettings.URL_CHECK_NO_PREFIX) ? WordChecks.urlNoPrefix() : WordChecks.url())
                    .wordDeny(wordDeny)
                    .wordAllow(wA)
                    .numCheckLen(settingsManager.getProperty(PluginSettings.NUM_CHECK_LEN))
                    .wordReplace(new WordReplace())
                    .wordTag(WordTags.none())
                    .charIgnore(new CharIgnore())
                    .enableIpv4Check(settingsManager.getProperty(PluginSettings.ENABLE_IP_CHECK))
                    .wordFailFast(settingsManager.getProperty(PluginSettings.FAIL_FAST))
                    .init();
            isInitialized = true;
        });
    }

    @Override
    public void onDisable() {
        listenerService.unregisterListeners();
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        TimingUtils.resetStatistics();
        ChatContext.forceClearContext();
        SignContext.forceClearContext();
        PlayerShadowController.clear();
        PlayerAltController.clear();
        BookCache.invalidateAll();
        ViolationCounter.INSTANCE.resetAllViolations();
        SchedulingUtils.cancelTaskSafely(violationResetTask);
        if (permCache != null) permCache.disable();
        if (isInitialized) sensitiveWordBs.destroy();
        Objects.requireNonNull(getCommand("advancedsensitivewords")).setExecutor(null);
        Objects.requireNonNull(getCommand("asw")).setExecutor(null);
        Objects.requireNonNull(getCommand("advancedsensitivewords")).setTabCompleter(null);
        Objects.requireNonNull(getCommand("asw")).setTabCompleter(null);
        LOGGER.info("AdvancedSensitiveWords is disabled.");
    }

    private void registerCommands() {
        ConstructCommandExecutor executor = new ConstructCommandExecutor();
        ConstructTabCompleter tabCompleter = new ConstructTabCompleter();
        Objects.requireNonNull(getCommand("advancedsensitivewords")).setExecutor(executor);
        Objects.requireNonNull(getCommand("asw")).setExecutor(executor);
        Objects.requireNonNull(getCommand("advancedsensitivewords")).setTabCompleter(tabCompleter);
        Objects.requireNonNull(getCommand("asw")).setTabCompleter(tabCompleter);
    }

    private void setupMetrics() {
        int pluginId = 20661;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new SimplePie("default_list", () -> String.valueOf(settingsManager.getProperty(PluginSettings.ENABLE_DEFAULT_WORDS))));
        metrics.addCustomChart(new SimplePie("java_vendor", TimingUtils::getJvmVendor));
        metrics.addCustomChart(new SingleLineChart("total_filtered_messages", () -> (int) messagesFilteredNum.get()));
    }

    private void registerVelocityChannel() {
        getServer().getMessenger().registerOutgoingPluginChannel(this, VelocityChannel.CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, VelocityChannel.CHANNEL, new VelocityReceiver());
    }

    private void registerPlaceholderExpansion() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") &&
                settingsManager.getProperty(PluginSettings.ENABLE_PLACEHOLDER)) {
            new ASWExpansion().register();
            LOGGER.info("Placeholders registered.");
        }
    }

    private void scheduleViolationResetTask() {
        long resetIntervalTicks = settingsManager.getProperty(PluginSettings.VIOLATION_RESET_TIME) * 20L * 60L;
        violationResetTask = new ViolationResetTask().runTaskTimerAsynchronously(this, resetIntervalTicks, resetIntervalTicks);
    }

    private void checkForUpdatesAsync() {
        if (!settingsManager.getProperty(PluginSettings.CHECK_FOR_UPDATE)) {
            return;
        }
        getScheduler().runTaskAsynchronously(() -> {
            LOGGER.info("Checking for update...");
            if (Updater.isUpdateAvailable()) {
                logAvailableUpdate();
            } else if (!Updater.isErred()) {
                LOGGER.info("You are running the latest version.");
            } else {
                LOGGER.info("Unable to fetch version info.");
            }
        });
    }

    private void logAvailableUpdate() {
        if (Updater.isDevChannel()) {
            LOGGER.warn("There is a new development version available: {}, you're on: {}", Updater.getLatestVersion(), Updater.getCurrentVersion());
            return;
        }
        LOGGER.warn("There is a new version available: {}, you're on: {}", Updater.getLatestVersion(), Updater.getCurrentVersion());
    }

    private IWordResultCondition createWordResultCondition() {
        return switch (settingsManager.getProperty(PluginSettings.FULL_MATCH_MODE)) {
            case 0 -> WordResultConditions.alwaysTrue();
            case 1 -> WordResultConditions.englishWordMatch();
            case 2 -> WordResultConditions.englishWordNumMatch();
            case 3 -> new WordResultConditionNumMatch();
            default -> {
                LOGGER.warn("Invalid full match mode, will turn off full match.");
                yield WordResultConditions.alwaysTrue();
            }
        };
    }

    private IWordDeny createWordDeny() {
        boolean enableDefaultWords = settingsManager.getProperty(PluginSettings.ENABLE_DEFAULT_WORDS);
        boolean enableOnlineWords = settingsManager.getProperty(PluginSettings.ENABLE_ONLINE_WORDS);
        if (enableDefaultWords && enableOnlineWords) {
            return WordDenys.chains(WordDenys.defaults(), new WordDeny(), new OnlineWordDeny(this), new ExternalWordDeny(this));
        }
        if (enableDefaultWords) {
            return WordDenys.chains(new WordDeny(), WordDenys.defaults(), new ExternalWordDeny(this));
        }
        if (enableOnlineWords) {
            return WordDenys.chains(new OnlineWordDeny(this), new WordDeny(), new ExternalWordDeny(this));
        }
        return WordDenys.chains(new WordDeny(), new ExternalWordDeny(this));
    }

}
