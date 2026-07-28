package io.wdsj.asw.bukkit;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.github.houbb.sensitive.word.api.IWordAllow;
import com.github.houbb.sensitive.word.api.IWordDeny;
import com.github.houbb.sensitive.word.api.IWordResult;
import com.github.houbb.sensitive.word.api.IWordResultCondition;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.allow.WordAllows;
import com.github.houbb.sensitive.word.support.check.WordChecks;
import com.github.houbb.sensitive.word.support.deny.WordDenys;
import com.github.houbb.sensitive.word.support.result.WordResultHandlers;
import com.github.houbb.sensitive.word.support.resultcondition.WordResultConditions;
import com.github.houbb.sensitive.word.support.tag.WordTags;
import io.wdsj.asw.bukkit.command.AswCommandRegistrar;
import io.wdsj.asw.bukkit.ai.LlmChatDetectionService;
import io.wdsj.asw.bukkit.core.condition.WordResultConditionNumMatch;
import io.wdsj.asw.bukkit.integration.placeholder.ASWExpansion;
import io.wdsj.asw.bukkit.service.chat.antispam.ChatAntiSpamService;
import io.wdsj.asw.bukkit.manage.punish.PlayerAltController;
import io.wdsj.asw.bukkit.manage.punish.PlayerShadowController;
import io.wdsj.asw.bukkit.manage.punish.ViolationCounter;
import io.wdsj.asw.bukkit.method.*;
import io.wdsj.asw.bukkit.permission.cache.CachingPermTool;
import io.wdsj.asw.bukkit.proxy.velocity.VelocityChannel;
import io.wdsj.asw.bukkit.proxy.velocity.VelocityReceiver;
import io.wdsj.asw.bukkit.proxy.velocity.sync.VelocitySyncClient;
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static io.wdsj.asw.bukkit.util.LoggingUtils.purgeLog;
import static io.wdsj.asw.bukkit.util.TimingUtils.resetStatistics;
import static io.wdsj.asw.bukkit.util.Utils.*;


public final class AdvancedSensitiveWords extends JavaPlugin {
    public static volatile boolean isInitialized = false;
    public static SensitiveWordBs sensitiveWordBs;
    public static SensitiveWordBs networkSensitiveWordBs;
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
    private VelocitySyncClient velocitySyncClient;
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

    public VelocitySyncClient getVelocitySyncClient() {
        return velocitySyncClient;
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
        startVelocitySyncClient();
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
        networkSensitiveWordBs = null;
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
                    .enableEmailCheck(false)
                    .enableUrlCheck(false)
                    .enableWordCheck(configurationService.get(PluginSettings.ENABLE_WORD_CHECK))
                    .wordResultCondition(condition)
                    .wordDeny(wordDeny)
                    .wordAllow(wA)
                    .numCheckLen(configurationService.get(PluginSettings.NUM_CHECK_LEN))
                    .wordReplace(new WordReplace())
                    .wordTag(WordTags.none())
                    .charIgnore(new CharIgnore(configurationService.get(PluginSettings.IGNORE_CHAR)))
                    .enableIpv4Check(false)
                    .wordFailFast(configurationService.get(PluginSettings.FAIL_FAST))
                    .init();
            networkSensitiveWordBs = createNetworkSensitiveWordBs();
            isInitialized = true;
        });
    }

    @Override
    public void onDisable() {
        stopVelocitySyncClient();
        listenerService.unregisterListeners();
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        TimingUtils.resetStatistics();
        ChatContext.forceClearContext();
        SignContext.forceClearContext();
        ChatAntiSpamService.clearAll();
        PlayerShadowController.clear();
        PlayerAltController.clear();
        BookCache.invalidateAll();
        ViolationCounter.INSTANCE.resetAllViolations();
        SchedulingUtils.cancelTaskSafely(violationResetTask);
        if (permCache != null) permCache.disable();
        if (isInitialized) {
            sensitiveWordBs.destroy();
            if (networkSensitiveWordBs != null) {
                networkSensitiveWordBs.destroy();
            }
        }
        commandRegistrar = null;
        LOGGER.info("AdvancedSensitiveWords is disabled.");
    }

    public void reloadPluginConfiguration() {
        configurationService.reload();
        if (listenerService != null) {
            listenerService.reloadConfiguration();
        }
        restartVelocitySyncClient();
    }

    private void setupMetrics() {
        int pluginId = 20661;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new SimplePie("default_list", () -> String.valueOf(configurationService.get(PluginSettings.ENABLE_DEFAULT_WORDS))));
        metrics.addCustomChart(new SimplePie("java_vendor", TimingUtils::getJvmVendor));
        metrics.addCustomChart(new SimplePie("ai_enable_rate", () -> String.valueOf(configurationService.get(PluginSettings.AI_ENABLED))));
        if (configurationService.get(PluginSettings.AI_ENABLED)) {
            metrics.addCustomChart(new SimplePie("ai_model_used", () -> configurationService.get(PluginSettings.AI_MODEL_NAME)));
        }
        metrics.addCustomChart(new SingleLineChart("total_filtered_messages", () -> (int) messagesFilteredNum.get()));
    }

    private void registerVelocityChannel() {
        getServer().getMessenger().registerOutgoingPluginChannel(this, VelocityChannel.CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, VelocityChannel.CHANNEL, new VelocityReceiver());
    }

    private void startVelocitySyncClient() {
        stopVelocitySyncClient();
        if (!configurationService.get(PluginSettings.VELOCITY_SYNC_ENABLED)) {
            return;
        }
        velocitySyncClient = new VelocitySyncClient(this);
        velocitySyncClient.start();
    }

    private void stopVelocitySyncClient() {
        if (velocitySyncClient != null) {
            velocitySyncClient.close();
            velocitySyncClient = null;
        }
    }

    private void restartVelocitySyncClient() {
        startVelocitySyncClient();
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
        violationResetTask = new ViolationResetTask(configurationService).runTaskTimerAsynchronously(this, resetIntervalTicks, resetIntervalTicks);
    }

    private void checkForUpdatesAsync() {
        if (!configurationService.get(PluginSettings.CHECK_FOR_UPDATE)) {
            return;
        }
        getScheduler().runTaskAsynchronously(() -> {
            LOGGER.info("Checking for updates...");
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

    private SensitiveWordBs createNetworkSensitiveWordBs() {
        boolean enableEmail = configurationService.get(PluginSettings.ENABLE_EMAIL_CHECK);
        boolean enableUrl = configurationService.get(PluginSettings.ENABLE_URL_CHECK);
        boolean enableIp = configurationService.get(PluginSettings.ENABLE_IP_CHECK);
        if (!enableEmail && !enableUrl && !enableIp) {
            return null;
        }
        return SensitiveWordBs.newInstance()
                .ignoreCase(configurationService.get(PluginSettings.IGNORE_CASE))
                .ignoreWidth(configurationService.get(PluginSettings.IGNORE_WIDTH))
                .ignoreNumStyle(configurationService.get(PluginSettings.IGNORE_NUM_STYLE))
                .ignoreChineseStyle(configurationService.get(PluginSettings.IGNORE_CHINESE_STYLE))
                .ignoreEnglishStyle(configurationService.get(PluginSettings.IGNORE_ENGLISH_STYLE))
                .ignoreRepeat(configurationService.get(PluginSettings.IGNORE_REPEAT))
                .enableNumCheck(false)
                .enableEmailCheck(enableEmail)
                .enableUrlCheck(enableUrl)
                .enableWordCheck(false)
                .wordResultCondition(WordResultConditions.alwaysTrue())
                .wordCheckUrl(configurationService.get(PluginSettings.URL_CHECK_NO_PREFIX) ? WordChecks.urlNoPrefix() : WordChecks.url())
                .wordReplace(new WordReplace())
                .wordTag(WordTags.none())
                .charIgnore(new CharIgnore(configurationService.get(PluginSettings.IGNORE_CHAR), CharIgnore.NETWORK_SYNTAX_CHARS))
                .enableIpv4Check(enableIp)
                .wordFailFast(configurationService.get(PluginSettings.FAIL_FAST))
                .init();
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

    public static List<String> findAllSensitive(String text) {
        LinkedHashSet<String> results = new LinkedHashSet<>();
        SensitiveWordBs wordBs = sensitiveWordBs;
        if (wordBs != null) {
            results.addAll(wordBs.findAll(text));
        }
        SensitiveWordBs networkWordBs = networkSensitiveWordBs;
        if (networkWordBs != null) {
            results.addAll(networkWordBs.findAll(text));
        }
        return List.copyOf(results);
    }

    public static List<IWordResult> findAllSensitiveRaw(String text) {
        List<IWordResult> results = new ArrayList<>();
        SensitiveWordBs wordBs = sensitiveWordBs;
        if (wordBs != null) {
            results.addAll(wordBs.findAll(text, WordResultHandlers.raw()));
        }
        SensitiveWordBs networkWordBs = networkSensitiveWordBs;
        if (networkWordBs != null) {
            results.addAll(networkWordBs.findAll(text, WordResultHandlers.raw()));
        }
        return results;
    }

    public static String replaceSensitive(String text) {
        SensitiveWordBs wordBs = sensitiveWordBs;
        String result = wordBs == null ? text : wordBs.replace(text);
        SensitiveWordBs networkWordBs = networkSensitiveWordBs;
        return networkWordBs == null ? result : networkWordBs.replace(result);
    }

}
