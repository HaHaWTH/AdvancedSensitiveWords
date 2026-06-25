package io.wdsj.asw.bukkit;

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
import io.wdsj.asw.bukkit.command.AswCommandRegistrar;
import io.wdsj.asw.bukkit.ai.LlmChatDetectionService;
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
import io.wdsj.asw.bukkit.setting.PaperConfigurationService;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.setting.SettingKey;
import io.wdsj.asw.bukkit.task.punish.ViolationResetTask;
import io.wdsj.asw.bukkit.util.SchedulingUtils;
import io.wdsj.asw.bukkit.util.TimingUtils;
import io.wdsj.asw.bukkit.util.cache.BookCache;
import io.wdsj.asw.bukkit.util.context.ChatContext;
import io.wdsj.asw.bukkit.util.context.SignContext;
import io.wdsj.asw.common.environment.PluginBuildInfo;
import io.wdsj.asw.common.update.Updater;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import static io.wdsj.asw.bukkit.util.LoggingUtils.purgeLog;
import static io.wdsj.asw.bukkit.util.TimingUtils.resetStatistics;
import static io.wdsj.asw.bukkit.util.Utils.*;


public final class AdvancedSensitiveWords extends JavaPlugin {
    public static volatile boolean isInitialized = false;
    public static SensitiveWordBs sensitiveWordBs;
    public static boolean isAuthMeAvailable;
    public static final String PLUGIN_VERSION = PluginBuildInfo.VERSION;
    private static AdvancedSensitiveWords instance;
    private static TaskScheduler scheduler;
    public static Logger LOGGER;
    private ListenerService listenerService;
    private CachingPermTool permCache;
    private PaperConfigurationService configurationService;
    private volatile Updater.UpdateResult updateResult = Updater.UpdateResult.noUpdate();
    private AswCommandRegistrar commandRegistrar;
    public static TaskScheduler getScheduler() {
        return scheduler;
    }

    public static AdvancedSensitiveWords getInstance() {
        return instance;
    }

    public Updater.UpdateResult getUpdateResult() {
        return updateResult;
    }

    public PaperConfigurationService getConfigurationService() {
        return configurationService;
    }

    public LlmChatDetectionService getLlmChatDetectionService() {
        if (listenerService == null) {
            throw new IllegalStateException("Listeners have not been initialized yet");
        }
        return listenerService.getLlmChatDetectionService();
    }

    public static <T> T setting(SettingKey<T> key) {
        return instance.configurationService.get(key);
    }

    public static String message(PluginMessages key) {
        return instance.configurationService.message(key);
    }
    private MyScheduledTask violationResetTask;

    @Override
    public void onLoad() {
        LOGGER = getSLF4JLogger();
        instance = this;
        configurationService = new PaperConfigurationService(LOGGER, getDataFolder().toPath());
        configurationService.load();
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
        if (configurationService.get(PluginSettings.PURGE_LOG_FILE)) purgeLog();
        listenerService = new ListenerService(this);
        listenerService.registerListeners();
        commandRegistrar = new AswCommandRegistrar(this);
        commandRegistrar.register();
        setupMetrics();
        registerVelocityChannel();
        registerPlaceholderExpansion();
        scheduleViolationResetTask();
        long endTime = System.currentTimeMillis();
        LOGGER.info("AdvancedSensitiveWords is enabled!(took {}ms)", endTime - startTime);
        if (Updater.isDevChannel()) {
            LOGGER.info("You are running a development version of AdvancedSensitiveWords! Branch: " + PluginBuildInfo.COMMIT_BRANCH);
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
                    .ignoreCase(configurationService.get(PluginSettings.IGNORE_CASE))
                    .ignoreWidth(configurationService.get(PluginSettings.IGNORE_WIDTH))
                    .ignoreNumStyle(configurationService.get(PluginSettings.IGNORE_NUM_STYLE))
                    .ignoreChineseStyle(configurationService.get(PluginSettings.IGNORE_CHINESE_STYLE))
                    .ignoreEnglishStyle(configurationService.get(PluginSettings.IGNORE_ENGLISH_STYLE))
                    .ignoreRepeat(configurationService.get(PluginSettings.IGNORE_REPEAT))
                    .enableNumCheck(configurationService.get(PluginSettings.ENABLE_NUM_CHECK))
                    .enableEmailCheck(configurationService.get(PluginSettings.ENABLE_EMAIL_CHECK))
                    .enableUrlCheck(configurationService.get(PluginSettings.ENABLE_URL_CHECK))
                    .enableWordCheck(configurationService.get(PluginSettings.ENABLE_WORD_CHECK))
                    .wordResultCondition(condition)
                    .wordCheckUrl(configurationService.get(PluginSettings.URL_CHECK_NO_PREFIX) ? WordChecks.urlNoPrefix() : WordChecks.url())
                    .wordDeny(wordDeny)
                    .wordAllow(wA)
                    .numCheckLen(configurationService.get(PluginSettings.NUM_CHECK_LEN))
                    .wordReplace(new WordReplace())
                    .wordTag(WordTags.none())
                    .charIgnore(new CharIgnore())
                    .enableIpv4Check(configurationService.get(PluginSettings.ENABLE_IP_CHECK))
                    .wordFailFast(configurationService.get(PluginSettings.FAIL_FAST))
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
        commandRegistrar = null;
        LOGGER.info("AdvancedSensitiveWords is disabled.");
    }

    public void reloadPluginConfiguration() {
        configurationService.reload();
        if (listenerService != null) {
            listenerService.reloadConfiguration();
        }
    }

    private void setupMetrics() {
        int pluginId = 20661;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new SimplePie("default_list", () -> String.valueOf(configurationService.get(PluginSettings.ENABLE_DEFAULT_WORDS))));
        metrics.addCustomChart(new SimplePie("java_vendor", TimingUtils::getJvmVendor));
        metrics.addCustomChart(new SingleLineChart("total_filtered_messages", () -> (int) messagesFilteredNum.get()));
    }

    private void registerVelocityChannel() {
        getServer().getMessenger().registerOutgoingPluginChannel(this, VelocityChannel.CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, VelocityChannel.CHANNEL, new VelocityReceiver());
    }

    private void registerPlaceholderExpansion() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") &&
                configurationService.get(PluginSettings.ENABLE_PLACEHOLDER)) {
            new ASWExpansion().register();
            LOGGER.info("Placeholders registered.");
        }
    }

    private void scheduleViolationResetTask() {
        long resetIntervalTicks = configurationService.get(PluginSettings.VIOLATION_RESET_TIME) * 20L * 60L;
        violationResetTask = new ViolationResetTask().runTaskTimerAsynchronously(this, resetIntervalTicks, resetIntervalTicks);
    }

    private void checkForUpdatesAsync() {
        if (!configurationService.get(PluginSettings.CHECK_FOR_UPDATE)) {
            return;
        }
        getScheduler().runTaskAsynchronously(() -> {
            LOGGER.info("Checking for update...");
            Updater.UpdateResult result = Updater.checkNow();
            updateResult = result;
            if (result.isUpdateAvailable()) {
                logAvailableUpdate(result);
            } else if (!result.isError()) {
                LOGGER.info("You are running the latest version.");
            } else {
                LOGGER.info("Unable to fetch version info.");
            }
        });
    }

    private void logAvailableUpdate(Updater.UpdateResult result) {
        if (Updater.isDevChannel()) {
            if (result.isReleaseUpdateAvailable()) {
                if (result.isError()) {
                    LOGGER.warn(
                            "A newer stable release is available: {} (current {}). Unable to compare the development branch.",
                            result.getLatestReleaseVersion(),
                            PLUGIN_VERSION
                    );
                    return;
                }
                LOGGER.warn(
                        "A newer stable release is available: {} (current {}). Latest development commit: {} ({} commit(s) behind).",
                        result.getLatestReleaseVersion(),
                        PLUGIN_VERSION,
                        result.getLatestVersion(),
                        result.getCommitsBehind()
                );
                return;
            }
            LOGGER.warn(
                "This development build is {} commit(s) behind {} (current {}).",
                    result.getCommitsBehind(),
                    result.getLatestVersion(),
                    PluginBuildInfo.COMMIT_HASH_SHORT
            );
            return;
        }
        LOGGER.warn("There is a new version available: {}, you're on: {}", result.getLatestVersion(), PLUGIN_VERSION);
    }

    private IWordResultCondition createWordResultCondition() {
        return switch (configurationService.get(PluginSettings.FULL_MATCH_MODE)) {
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
        boolean enableDefaultWords = configurationService.get(PluginSettings.ENABLE_DEFAULT_WORDS);
        boolean enableOnlineWords = configurationService.get(PluginSettings.ENABLE_ONLINE_WORDS);
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
