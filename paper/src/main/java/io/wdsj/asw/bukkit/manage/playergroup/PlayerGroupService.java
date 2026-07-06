package io.wdsj.asw.bukkit.manage.playergroup;

import com.github.Anon8281.universalScheduler.UniversalRunnable;
import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.check.WordChecks;
import com.github.houbb.sensitive.word.support.resultcondition.WordResultConditions;
import com.github.houbb.sensitive.word.support.tag.WordTags;
import com.zaxxer.hikari.HikariDataSource;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.core.persistence.*;
import io.wdsj.asw.bukkit.method.CharIgnore;
import io.wdsj.asw.bukkit.method.WordReplace;
import io.wdsj.asw.bukkit.setting.PaperConfigurationService;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.setting.SettingsConfiguration;
import io.wdsj.asw.bukkit.util.SchedulingUtils;
import io.wdsj.asw.bukkit.util.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public final class PlayerGroupService implements AutoCloseable {
    private static final String[] DISTANCE_STATISTICS = {
            "WALK_ONE_CM", "SPRINT_ONE_CM", "SWIM_ONE_CM", "FALL_ONE_CM", "CLIMB_ONE_CM",
            "FLY_ONE_CM", "BOAT_ONE_CM", "MINECART_ONE_CM", "PIG_ONE_CM", "HORSE_ONE_CM",
            "AVIATE_ONE_CM", "STRIDER_ONE_CM"
    };

    private final AdvancedSensitiveWords plugin;
    private final PaperConfigurationService configuration;
    private final Logger logger;
    private final HikariDataSource dataSource;
    private final WriteBackCache<UUID, PlayerGroupState> groupStates;
    private final PlayerGroupActivityRepository activityRepository;
    private final WriteBackCache<UUID, PlayerGroupActivityState> localActivityStates;
    private final ExecutorService statisticExecutor;
    private final ConcurrentMap<UUID, PlayerActivitySnapshot> activitySnapshots = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, TokenBucket> tokenBuckets = new ConcurrentHashMap<>();
    private final Object newbieLinkWordBsLock = new Object();
    private final AtomicLong newbieLinkWordBsReloadVersion = new AtomicLong();
    private volatile SensitiveWordBs newbieLinkWordBs;
    private volatile CompletableFuture<Void> newbieLinkWordBsReload;
    private volatile boolean closed;
    private MyScheduledTask refreshTask;

    public PlayerGroupService(AdvancedSensitiveWords plugin) throws Exception {
        this.plugin = plugin;
        this.configuration = plugin.getConfigurationService();
        this.logger = AdvancedSensitiveWords.LOGGER;
        this.statisticExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ASW Statistic Refresher");
            thread.setPriority(Thread.NORM_PRIORITY - 2);
            thread.setDaemon(true);
            return thread;
        });
        SettingsConfiguration.PlayerGroups groups = configuration.get(PluginSettings.PLAYER_GROUPS);
        StorageConfig storageConfig = toStorageConfig(groups.storage, configuration.dataDirectory());
        this.dataSource = DataSourceFactory.create(storageConfig);
        PlayerGroupStateRepository repository = new PlayerGroupStateRepository(dataSource, storageConfig.type());
        repository.initialize();
        String serverId = groups.serverId.trim();
        this.activityRepository = new PlayerGroupActivityRepository(dataSource, storageConfig.type(), serverId);
        activityRepository.initialize();
        this.groupStates = new WriteBackCache<>(
                "PlayerGroups",
                repository,
                logger,
                Caffeine.newBuilder().expireAfterWrite(30L, TimeUnit.MINUTES).maximumSize(10_000L),
                Duration.ofSeconds(30L),
                Duration.ofSeconds(10L),
                FlushPolicy.PERIODIC
        );
        this.localActivityStates = new WriteBackCache<>(
                "PlayerGroupActivity",
                activityRepository,
                logger,
                Caffeine.newBuilder().expireAfterWrite(30L, TimeUnit.MINUTES).maximumSize(10_000L),
                Duration.ofSeconds(30L),
                Duration.ofSeconds(10L),
                FlushPolicy.PERIODIC
        );
        reloadConfiguration();
    }

    public void start() {
        long intervalTicks = Math.max(1L, configuration.get(PluginSettings.PLAYER_GROUPS).refreshIntervalSeconds) * 20L;
        refreshTask = new UniversalRunnable() {
            @Override
            public void run() {
                refreshOnlinePlayers();
            }
        }.runTaskTimerAsynchronously(plugin, intervalTicks, intervalTicks);
    }

    public void reloadConfiguration() {
        if (closed) {
            return;
        }
        long version = newbieLinkWordBsReloadVersion.incrementAndGet();
        CompletableFuture<Void> previousReload = newbieLinkWordBsReload;
        if (previousReload != null) {
            previousReload.cancel(false);
        }
        newbieLinkWordBsReload = CompletableFuture.supplyAsync(this::createLinkWordBs, statisticExecutor)
                .thenAccept(created -> {
                    if (closed || newbieLinkWordBsReloadVersion.get() != version) {
                        destroyWordBs(created);
                        return;
                    }
                    synchronized (newbieLinkWordBsLock) {
                        SensitiveWordBs previous = newbieLinkWordBs;
                        newbieLinkWordBs = created;
                        destroyWordBs(previous);
                    }
                })
                .exceptionally(exception -> {
                    if (!closed) {
                        logger.error("Failed to initialize newbie link detector.", exception);
                    }
                    return null;
                });
    }

    private static void destroyWordBs(SensitiveWordBs wordBs) {
        if (wordBs != null) {
            wordBs.destroy();
        }
    }

    private SensitiveWordBs currentNewbieLinkWordBs() {
        synchronized (newbieLinkWordBsLock) {
            return newbieLinkWordBs;
        }
    }

    private void clearNewbieLinkWordBs() {
        CompletableFuture<Void> reload = newbieLinkWordBsReload;
        if (reload != null) {
            reload.cancel(false);
            newbieLinkWordBsReload = null;
        }
        synchronized (newbieLinkWordBsLock) {
            SensitiveWordBs wordBs = newbieLinkWordBs;
            newbieLinkWordBs = null;
            destroyWordBs(wordBs);
        }
    }

    private boolean newbieLinkWordBsReady() {
        return currentNewbieLinkWordBs() != null;
    }

    private boolean matchNewbieLink(String content) {
        synchronized (newbieLinkWordBsLock) {
            SensitiveWordBs wordBs = newbieLinkWordBs;
            return wordBs != null && !wordBs.findAll(content).isEmpty();
        }
    }

    private boolean shouldRunNewbieLinkCheck(SettingsConfiguration.LinkCheck linkCheck) {
        if (!linkCheck.enabled) {
            return false;
        }
        if (!newbieLinkWordBsReady()) {
            CompletableFuture<Void> reload = newbieLinkWordBsReload;
            if (reload == null || reload.isDone()) {
                reloadConfiguration();
            }
            return false;
        }
        return true;
    }

    public void preload(UUID playerId, String playerName) {
        if (!isEnabled()) {
            return;
        }
        try {
            logger.info("Loading group info for player {}", playerName);
            groupStates.getAsync(playerId).join();
        } catch (Exception exception) {
            logger.warn("Unable to preload player group information for {}.", playerId, exception);
        }
    }

    public void handleJoin(Player player) {
        if (!isEnabled()) {
            return;
        }
        refreshAndSynchronize(player).thenRun(() ->
                SchedulingUtils.runForOnlinePlayer(player.getUniqueId(), current -> {
                    if (currentGroup(current) == PlayerGroup.NEWBIE
                            && configuration.get(PluginSettings.PLAYER_GROUPS).newbie.sendJoinMessage) {
                        MessageUtils.sendMessage(current, PluginMessages.PLAYER_GROUP_NEWBIE_JOIN);
                    }
                })
        ).exceptionally(exception -> {
            logger.error("Failed to load player group override for {}.", player.getUniqueId(), exception);
            return null;
        });
    }

    public void handleQuit(Player player) {
        UUID playerId = player.getUniqueId();
        if (isEnabled()) {
            persistLocalActivityOnQuit(player).whenComplete((ignored, exception) -> {
                if (exception != null) {
                    logger.error("Failed to persist final player activity snapshot for {}.", playerId, exception);
                    return;
                }
                localActivityStates.invalidate(playerId);
            });
        } else {
            localActivityStates.invalidate(playerId);
        }
        activitySnapshots.remove(playerId);
        tokenBuckets.remove(playerId);
        groupStates.invalidate(playerId);
    }

    public boolean isModuleEnabled(Player player, GroupModule module, boolean globalEnabled) {
        if (!globalEnabled) {
            return false;
        }
        if (!isEnabled()) {
            return true;
        }
        SettingsConfiguration.GroupPolicy policy = policyFor(currentGroup(player));
        GroupModuleMode mode = modeFor(policy.modules, module);
        return mode != GroupModuleMode.DISABLED;
    }

    public boolean isNewbie(Player player) {
        return isEnabled() && currentGroup(player) == PlayerGroup.NEWBIE;
    }

    public boolean consumeNewbieToken(Player player) {
        if (!isNewbie(player)) {
            return true;
        }
        SettingsConfiguration.RateLimit rateLimit = configuration.get(PluginSettings.PLAYER_GROUPS).newbie.rateLimit;
        if (!rateLimit.enabled) {
            return true;
        }
        TokenBucket bucket = tokenBuckets.compute(player.getUniqueId(), (ignored, existing) -> {
            if (existing == null || existing.capacity != rateLimit.capacity
                    || existing.refillIntervalMillis != rateLimit.refillIntervalSeconds * 1000L) {
                return new TokenBucket(rateLimit.capacity, rateLimit.refillIntervalSeconds * 1000L);
            }
            return existing;
        });
        return bucket.tryConsume(System.currentTimeMillis());
    }

    public boolean containsNewbieLink(Player player, String content) {
        if (!isNewbie(player)) {
            return false;
        }
        SettingsConfiguration.LinkCheck linkCheck = configuration.get(PluginSettings.PLAYER_GROUPS).newbie.linkCheck;
        if (content == null || !shouldRunNewbieLinkCheck(linkCheck)) {
            return false;
        }
        return matchNewbieLink(content);
    }

    public CompletableFuture<PlayerGroupStatus> statusAsync(Player player) {
        return refreshAndSynchronize(player)
                .thenApply(ignored -> status(player));
    }

    public PlayerGroupStatus status(Player player) {
        PlayerActivitySnapshot cachedSnapshot = activitySnapshots.get(player.getUniqueId());
        PlayerActivitySnapshot snapshot = cachedSnapshot == null ? PlayerActivitySnapshot.empty() : cachedSnapshot;
        Optional<PlayerGroupState> state = groupStates.getIfCached(player.getUniqueId());
        PlayerGroup group = state.map(PlayerGroupState::group)
                .orElseGet(() -> cachedSnapshot == null ? unloadedFallbackGroup() : automaticGroup(snapshot));
        PlayerGroupSource source = state.filter(PlayerGroupState::manualOverride).isPresent()
                ? PlayerGroupSource.MANUAL
                : PlayerGroupSource.AUTOMATIC;
        return new PlayerGroupStatus(group, source, snapshot, configuration.get(PluginSettings.PLAYER_GROUPS).playerThreshold);
    }

    public CompletableFuture<Void> setManualGroup(Player target, PlayerGroup group, CommandSender actor) {
        PlayerGroupState state = new PlayerGroupState(
                target.getUniqueId(),
                normalizeName(target.getName()),
                group,
                true,
                System.currentTimeMillis(),
                actor instanceof Player player ? player.getUniqueId() : null,
                actor.getName()
        );
        return groupStates.putAndFlushAsync(target.getUniqueId(), state)
                .thenCompose(ignored -> refreshSnapshotAsync(target))
                .thenApply(ignored -> null);
    }

    public CompletableFuture<Void> clearManualGroup(Player target) {
        return groupStates.deleteAndFlushAsync(target.getUniqueId())
                .thenCompose(ignored -> refreshSnapshotAsync(target))
                .thenCompose(snapshot -> groupStates.getAsync(target.getUniqueId())
                        .thenCompose(state -> synchronizeAutomaticState(target, snapshot, state)))
                .thenApply(ignored -> null);
    }

    private boolean isEnabled() {
        return configuration.get(PluginSettings.PLAYER_GROUPS_ENABLED);
    }

    private PlayerGroup currentGroup(Player player) {
        return status(player).group();
    }

    private PlayerGroup automaticGroup(PlayerActivitySnapshot snapshot) {
        return snapshot.score() >= configuration.get(PluginSettings.PLAYER_GROUPS).playerThreshold
                ? PlayerGroup.PLAYER
                : PlayerGroup.NEWBIE;
    }

    private PlayerGroup unloadedFallbackGroup() {
        return PlayerGroup.NEWBIE;
    }

    private SettingsConfiguration.GroupPolicy policyFor(PlayerGroup group) {
        SettingsConfiguration.PlayerGroups groups = configuration.get(PluginSettings.PLAYER_GROUPS);
        return group == PlayerGroup.PLAYER ? groups.player : groups.newbie;
    }

    private void refreshOnlinePlayers() {
        if (!isEnabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshAndSynchronize(player);
        }
    }

    private CompletableFuture<Void> refreshAndSynchronize(Player player) {
        CompletableFuture<Optional<PlayerGroupState>> stateFuture = groupStates.getAsync(player.getUniqueId());
        CompletableFuture<PlayerActivitySnapshot> snapshotFuture = refreshSnapshotAsync(player);
        return stateFuture.thenCombine(snapshotFuture, GroupRefresh::new)
                .thenCompose(refresh -> synchronizeAutomaticState(player, refresh.snapshot(), refresh.state()));
    }

    private CompletableFuture<PlayerActivitySnapshot> refreshSnapshotAsync(Player player) {
        if (!player.isOnline()) {
            return CompletableFuture.completedFuture(PlayerActivitySnapshot.empty());
        }
        UUID playerId = player.getUniqueId();
        SettingsConfiguration.ActivityWeights weights = configuration.get(PluginSettings.PLAYER_GROUPS).weights;
        return CompletableFuture.supplyAsync(() -> calculateSnapshot(player, weights), statisticExecutor).thenCompose(localSnapshot -> persistAndAggregateActivity(player, localSnapshot, weights))
                .exceptionally(exception -> {
                    logger.error("Failed to refresh player activity snapshot for {}.", playerId, exception);
                    return activitySnapshots.getOrDefault(playerId, PlayerActivitySnapshot.empty());
                });
    }

    private CompletableFuture<Void> persistLocalActivityOnQuit(Player player) {
        UUID playerId = player.getUniqueId();
        SettingsConfiguration.ActivityWeights weights = configuration.get(PluginSettings.PLAYER_GROUPS).weights;
        return CompletableFuture.supplyAsync(() -> calculateSnapshot(player, weights), statisticExecutor)
                .thenCompose(snapshot -> localActivityStates.putAndFlushAsync(playerId, new PlayerGroupActivityState(
                        playerId,
                        normalizeName(player.getName()),
                        snapshot,
                        System.currentTimeMillis()
                )));
    }

    private CompletableFuture<PlayerActivitySnapshot> persistAndAggregateActivity(
            Player player,
            PlayerActivitySnapshot localSnapshot,
            SettingsConfiguration.ActivityWeights weights
    ) {
        UUID playerId = player.getUniqueId();
        PlayerGroupActivityState localState = new PlayerGroupActivityState(
                playerId,
                normalizeName(player.getName()),
                localSnapshot,
                System.currentTimeMillis()
        );
        return localActivityStates.putAndFlushAsync(playerId, localState)
                .thenCompose(ignored -> localActivityStates.queryAsync(() -> activityRepository.loadRemoteTotal(playerId)))
                .thenApply(remoteSnapshot -> {
                    PlayerActivitySnapshot totalRaw = localSnapshot.merge(remoteSnapshot);
                    PlayerActivitySnapshot total = PlayerActivitySnapshot.withScore(totalRaw, calculateScore(totalRaw, weights));
                    activitySnapshots.put(playerId, total);
                    return total;
                });
    }

    private CompletableFuture<Void> synchronizeAutomaticState(
            Player player,
            PlayerActivitySnapshot snapshot,
            Optional<PlayerGroupState> currentState
    ) {
        PlayerGroup automaticGroup = automaticGroup(snapshot);
        if (currentState.isPresent() && currentState.get().manualOverride()) {
            return CompletableFuture.completedFuture(null);
        }
        if (currentState.isPresent() && currentState.get().group() == PlayerGroup.PLAYER) {
            return CompletableFuture.completedFuture(null);
        }
        if (automaticGroup == PlayerGroup.PLAYER) {
            PlayerGroupState state = new PlayerGroupState(
                    player.getUniqueId(),
                    normalizeName(player.getName()),
                    PlayerGroup.PLAYER,
                    false,
                    System.currentTimeMillis(),
                    null,
                    "AUTO"
            );
            return groupStates.putAndFlushAsync(player.getUniqueId(), state);
        }
        if (currentState.isPresent()) {
            return groupStates.deleteAndFlushAsync(player.getUniqueId());
        }
        return CompletableFuture.completedFuture(null);
    }

    private PlayerActivitySnapshot calculateSnapshot(Player player, SettingsConfiguration.ActivityWeights weights) {
        double playHours = statistic(player, "PLAY_ONE_MINUTE") / 72000.0D;
        long minedBlocks = materialStatistic(player, "MINE_BLOCK", true);
        double movedBlocks = distanceCentimeters(player) / 100.0D;
        long mobKills = statistic(player, "MOB_KILLS");
        long usedItems = materialStatistic(player, "USE_ITEM", false);
        long brokenItems = materialStatistic(player, "BREAK_ITEM", false);
        long craftedItems = materialStatistic(player, "CRAFT_ITEM", false);
        long damageDealt = statistic(player, "DAMAGE_DEALT");
        long damageTaken = statistic(player, "DAMAGE_TAKEN");
        long deaths = statistic(player, "DEATHS");
        long enchantedItems = statistic(player, "ITEM_ENCHANTED");
        long fishCaught = statistic(player, "FISH_CAUGHT");
        long villagerTrades = statistic(player, "TRADED_WITH_VILLAGER");
        PlayerActivitySnapshot snapshot = new PlayerActivitySnapshot(0.0D, playHours, minedBlocks, movedBlocks, mobKills, usedItems,
                brokenItems, craftedItems, damageDealt, damageTaken, deaths, enchantedItems, fishCaught, villagerTrades);
        return PlayerActivitySnapshot.withScore(snapshot, calculateScore(snapshot, weights));
    }

    private static double calculateScore(PlayerActivitySnapshot snapshot, SettingsConfiguration.ActivityWeights weights) {
        return snapshot.playTimeHours() * weights.playTimeHours
                + snapshot.minedBlocks() * weights.minedBlocks
                + snapshot.movedBlocks() * weights.movedBlocks
                + snapshot.mobKills() * weights.mobKills
                + snapshot.usedItems() * weights.usedItems
                + snapshot.brokenItems() * weights.brokenItems
                + snapshot.craftedItems() * weights.craftedItems
                + snapshot.damageDealt() * weights.damageDealt
                + snapshot.damageTaken() * weights.damageTaken
                + snapshot.deaths() * weights.deaths
                + snapshot.enchantedItems() * weights.enchantedItems
                + snapshot.fishCaught() * weights.fishCaught
                + snapshot.villagerTrades() * weights.villagerTrades;
    }

    private long distanceCentimeters(Player player) {
        long total = 0L;
        for (String statisticName : DISTANCE_STATISTICS) {
            total += statistic(player, statisticName);
        }
        return total;
    }

    private static long statistic(Player player, String statisticName) {
        try {
            return player.getStatistic(Statistic.valueOf(statisticName));
        } catch (IllegalArgumentException exception) {
            return 0L;
        }
    }

    private static long materialStatistic(Player player, String statisticName, boolean blocksOnly) {
        Statistic statistic;
        try {
            statistic = Statistic.valueOf(statisticName);
        } catch (IllegalArgumentException exception) {
            return 0L;
        }
        long total = 0L;
        for (Material material : Material.values()) {
            if (blocksOnly && (!material.isBlock() || material.isAir())) {
                continue;
            }
            if (!blocksOnly && !material.isItem()) {
                continue;
            }
            try {
                total += player.getStatistic(statistic, material);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return total;
    }

    private static GroupModuleMode modeFor(SettingsConfiguration.GroupModules modules, GroupModule module) {
        return switch (module) {
            case CHAT -> modules.chat;
            case COMMAND -> modules.command;
            case BOOK -> modules.book;
            case SIGN -> modules.sign;
            case ANVIL -> modules.anvil;
            case ITEM -> modules.item;
        };
    }

    private static StorageConfig toStorageConfig(SettingsConfiguration.GroupStorage storage, Path dataDirectory) {
        return new StorageConfig(
                storage.type,
                storage.type == StorageType.SQLITE
                        ? dataDirectory.resolve(storage.sqliteFile)
                        : null,
                storage.mysql.host,
                storage.mysql.port,
                storage.mysql.database,
                storage.mysql.username,
                storage.mysql.password,
                storage.poolName,
                storage.maximumPoolSize,
                storage.minimumIdle,
                Duration.ofMillis(storage.connectionTimeoutMillis),
                storage.sqliteWal
        );
    }

    private static String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private SensitiveWordBs createLinkWordBs() {
        SettingsConfiguration.PlayerGroups groups = configuration.get(PluginSettings.PLAYER_GROUPS);
        if (!groups.newbie.linkCheck.enabled) {
            return null;
        }
        return SensitiveWordBs.newInstance()
                .ignoreCase(configuration.get(PluginSettings.IGNORE_CASE))
                .ignoreWidth(configuration.get(PluginSettings.IGNORE_WIDTH))
                .ignoreNumStyle(configuration.get(PluginSettings.IGNORE_NUM_STYLE))
                .ignoreChineseStyle(configuration.get(PluginSettings.IGNORE_CHINESE_STYLE))
                .ignoreEnglishStyle(configuration.get(PluginSettings.IGNORE_ENGLISH_STYLE))
                .ignoreRepeat(configuration.get(PluginSettings.IGNORE_REPEAT))
                .enableNumCheck(false)
                .enableEmailCheck(false)
                .enableUrlCheck(true)
                .enableWordCheck(false)
                .wordResultCondition(WordResultConditions.alwaysTrue())
                .wordCheckUrl(configuration.get(PluginSettings.URL_CHECK_NO_PREFIX) ? WordChecks.urlNoPrefix() : WordChecks.url())
                .wordTag(WordTags.none())
                .wordReplace(new WordReplace())
                .charIgnore(new CharIgnore(configuration.get(PluginSettings.IGNORE_CHAR), CharIgnore.NETWORK_SYNTAX_CHARS))
                .enableIpv4Check(true)
                .wordFailFast(true)
                .init();
    }

    @Override
    public void close() {
        closed = true;
        SchedulingUtils.cancelTaskSafely(refreshTask);
        statisticExecutor.shutdownNow();
        localActivityStates.close();
        groupStates.close();
        dataSource.close();
        clearNewbieLinkWordBs();
        activitySnapshots.clear();
        tokenBuckets.clear();
    }

    private record GroupRefresh(Optional<PlayerGroupState> state, PlayerActivitySnapshot snapshot) {
    }

    private static final class TokenBucket {
        private final int capacity;
        private final long refillIntervalMillis;
        private double tokens;
        private long lastRefillMillis;

        private TokenBucket(int capacity, long refillIntervalMillis) {
            this.capacity = capacity;
            this.refillIntervalMillis = refillIntervalMillis;
            this.tokens = capacity;
            this.lastRefillMillis = System.currentTimeMillis();
        }

        private synchronized boolean tryConsume(long nowMillis) {
            refill(nowMillis);
            if (tokens < 1.0D) {
                return false;
            }
            tokens -= 1.0D;
            return true;
        }

        private void refill(long nowMillis) {
            long elapsed = Math.max(0L, nowMillis - lastRefillMillis);
            if (elapsed == 0L) {
                return;
            }
            double refill = (double) elapsed / refillIntervalMillis * capacity;
            tokens = Math.min(capacity, tokens + refill);
            lastRefillMillis = nowMillis;
        }
    }
}
