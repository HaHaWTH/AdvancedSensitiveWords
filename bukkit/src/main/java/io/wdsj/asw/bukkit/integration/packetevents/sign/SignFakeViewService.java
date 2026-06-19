package io.wdsj.asw.bukkit.integration.packetevents.sign;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.nbt.*;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityType;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class SignFakeViewService {
    private static final int SCHEMA_VERSION = 2;
    private static final String LINE_SEPARATOR = "|";
    private static final Component[] BLANK_LINES = {Component.empty(), Component.empty(), Component.empty(), Component.empty()};
    private static final GsonComponentSerializer COMPONENT_SERIALIZER = GsonComponentSerializer.gson();

    private static final NamespacedKey VERSION_KEY = key("sign_fake_version");
    private static final NamespacedKey ORIGINAL_LINES_KEY = key("sign_fake_original_lines");
    private static final NamespacedKey BLANK_LINES_KEY = key("sign_fake_blank_lines");
    private static final NamespacedKey PLACER_KEY = key("sign_fake_placer");
    private static final NamespacedKey SIDE_KEY = key("sign_fake_side");
    private static final NamespacedKey CREATED_AT_KEY = key("sign_fake_created_at");
    private static final NamespacedKey VIOLATION_CONTENT_KEY = key("sign_fake_violation_content");
    private static final NamespacedKey CENSORED_WORDS_KEY = key("sign_fake_censored_words");
    private static final Queue<RefreshRequest> PENDING_REFRESHES = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean DRAIN_SCHEDULED = new AtomicBoolean();
    private static final ThreadFactory REFRESH_THREAD_FACTORY = new ThreadFactoryBuilder()
            .setNameFormat("ASW Fake Sign Refresher")
            .setDaemon(true)
            .setPriority(Thread.NORM_PRIORITY - 1)
            .build();

    private static volatile boolean operational;
    private static ExecutorService refreshExecutor;

    private SignFakeViewService() {
    }

    public static boolean isOperational() {
        return operational && isConfiguredForCancelFakeView();
    }

    static void setOperational(boolean enabled) {
        operational = enabled;
        if (enabled) {
            ensureRefreshExecutor();
            return;
        }

        PENDING_REFRESHES.clear();
        DRAIN_SCHEDULED.set(false);
        if (refreshExecutor != null) {
            refreshExecutor.shutdownNow();
            refreshExecutor = null;
        }
    }

    public static boolean isConfiguredForCancelFakeView() {
        return AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_SIGN_EDIT_CHECK)
                && AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.SIGN_FAKE_ON_CANCEL);
    }

    public static void recordCancelledEdit(
            SignChangeEvent event,
            Player player,
            List<Component> attemptedLines,
            String violationContent,
            List<String> censoredWords
    ) {
        if (!isOperational()) {
            return;
        }

        Location location = event.getBlock().getLocation();
        Side side = event.getSide();
        Component[] originalLines = toComponentLines(attemptedLines);
        long createdAt = System.currentTimeMillis();
        AdvancedSensitiveWords.getScheduler().runTaskLater(location, () -> writeRecord(
                location,
                side,
                player.getUniqueId(),
                originalLines,
                violationContent,
                censoredWords,
                createdAt
        ), 1L);
    }

    static void enqueueRefresh(Player viewer, World world, Collection<BlockKey> keys) {
        if (!isOperational() || keys.isEmpty()) {
            return;
        }

        UUID viewerId = viewer.getUniqueId();
        UUID worldId = world.getUID();
        for (BlockKey key : keys) {
            if (!key.worldId().equals(worldId)) {
                continue;
            }
            PENDING_REFRESHES.add(new RefreshRequest(viewerId, world, key));
        }
        scheduleDrain();
    }

    private static void writeRecord(
            Location location,
            Side side,
            UUID placer,
            Component[] originalLines,
            String violationContent,
            List<String> censoredWords,
            long createdAt
    ) {
        BlockState state = location.getBlock().getState();
        if (!(state instanceof Sign sign)) {
            return;
        }

        PersistentDataContainer pdc = sign.getPersistentDataContainer();
        pdc.set(VERSION_KEY, PersistentDataType.INTEGER, SCHEMA_VERSION);
        pdc.set(ORIGINAL_LINES_KEY, PersistentDataType.STRING, encodeComponents(originalLines));
        pdc.set(BLANK_LINES_KEY, PersistentDataType.STRING, encodeComponents(BLANK_LINES));
        pdc.set(PLACER_KEY, PersistentDataType.STRING, placer.toString());
        pdc.set(SIDE_KEY, PersistentDataType.STRING, side.name());
        pdc.set(CREATED_AT_KEY, PersistentDataType.LONG, createdAt);
        pdc.set(VIOLATION_CONTENT_KEY, PersistentDataType.STRING, encode(violationContent));
        pdc.set(CENSORED_WORDS_KEY, PersistentDataType.STRING, encode(String.join("\n", censoredWords)));

        if (!sign.update(false, false)) {
            return;
        }

        SignFakeRecord record = readRecord(sign);
        if (record != null) {
            AdvancedSensitiveWords.getScheduler().runTaskLater(location, () -> sendOriginalToPlacer(location, record), 2L);
        }
    }

    private static SignFakeRecord readRecordAt(Location location) {
        BlockState state = location.getBlock().getState();
        if (!(state instanceof Sign sign)) {
            return null;
        }
        return readRecord(sign);
    }

    private static SignFakeRecord readRecord(Sign sign) {
        PersistentDataContainer pdc = sign.getPersistentDataContainer();
        Integer version = pdc.get(VERSION_KEY, PersistentDataType.INTEGER);
        String original = pdc.get(ORIGINAL_LINES_KEY, PersistentDataType.STRING);
        String blank = pdc.get(BLANK_LINES_KEY, PersistentDataType.STRING);
        String placer = pdc.get(PLACER_KEY, PersistentDataType.STRING);
        String side = pdc.get(SIDE_KEY, PersistentDataType.STRING);
        Long createdAt = pdc.get(CREATED_AT_KEY, PersistentDataType.LONG);
        if (version != null && version != SCHEMA_VERSION) {
            pdc.remove(VERSION_KEY);
            pdc.remove(ORIGINAL_LINES_KEY);
            pdc.remove(BLANK_LINES_KEY);
            pdc.remove(PLACER_KEY);
            pdc.remove(SIDE_KEY);
            pdc.remove(CREATED_AT_KEY);
            return null;
        }
        if (version == null || original == null || blank == null || placer == null || side == null || createdAt == null) {
            return null;
        }

        try {
            UUID placerId = UUID.fromString(placer);
            Side signSide = Side.valueOf(side);
            return new SignFakeRecord(
                    placerId,
                    signSide,
                    decodeComponents(original),
                    decodeComponents(blank),
                    createdAt
            );
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void sendOriginalToPlacer(Location location, SignFakeRecord record) {
        if (!isOperational() || location.getWorld() == null) {
            return;
        }

        Player viewer = Bukkit.getPlayer(record.placer());
        if (viewer == null || !viewer.getWorld().equals(location.getWorld())) {
            return;
        }
        sendRecordView(viewer, location, record);
    }

    private static void sendRecordView(Player viewer, Location location, SignFakeRecord record) {
        if (!record.canSeeOriginal(viewer.getUniqueId())) return;
        BlockState state = location.getBlock().getState();
        if (!(state instanceof Sign sign)) {
            return;
        }

        Component[] lines = record.visibleLines(viewer.getUniqueId());
        Vector3i position = new Vector3i(location.getBlockX(), location.getBlockY(), location.getBlockZ());

        WrapperPlayServerBlockEntityData packet = new WrapperPlayServerBlockEntityData(
                position,
                blockEntityType(sign),
                new NBTCompound()
        );

        packet.setNBT(buildSignNbt(sign, location, record.side(), lines, packet));

        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(viewer, packet);
    }

    private static NBTCompound buildSignNbt(
            Sign sign,
            Location location,
            Side overriddenSide,
            Component[] overriddenLines,
            WrapperPlayServerBlockEntityData wrapper
    ) {
        NBTCompound nbt = new NBTCompound();

        nbt.setTag("id", new NBTString(blockEntityId(sign)));
        nbt.setTag("x", new NBTInt(location.getBlockX()));
        nbt.setTag("y", new NBTInt(location.getBlockY()));
        nbt.setTag("z", new NBTInt(location.getBlockZ()));

        nbt.setTag("front_text", buildTextSide(
                sign.getSide(Side.FRONT),
                overriddenSide == Side.FRONT ? overriddenLines : readSideLines(sign, Side.FRONT),
                wrapper
        ));

        nbt.setTag("back_text", buildTextSide(
                sign.getSide(Side.BACK),
                overriddenSide == Side.BACK ? overriddenLines : readSideLines(sign, Side.BACK),
                wrapper
        ));

        return nbt;
    }

    private static NBTCompound buildTextSide(
            SignSide side,
            Component[] lines,
            WrapperPlayServerBlockEntityData wrapper
    ) {
        NBTCompound text = new NBTCompound();

        List<Component> messages = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            messages.add(i < lines.length && lines[i] != null ? lines[i] : Component.empty());
        }

        AdventureSerializer serializer = AdventureSerializer.serializer(wrapper);

        text.setCompactList("messages", messages, serializer, wrapper);

        DyeColor color = side.getColor();
        text.setTag("color", new NBTString(color == null ? "black" : color.name().toLowerCase(Locale.ROOT)));
        text.setTag("has_glowing_text", new NBTByte(side.isGlowingText()));

        return text;
    }

    private static Component[] readSideLines(Sign sign, Side side) {
        Component[] lines = BLANK_LINES.clone();
        SignSide signSide = sign.getSide(side);
        for (int i = 0; i < 4; i++) {
            lines[i] = signSide.line(i);
        }
        return lines;
    }

    private static BlockEntityType blockEntityType(Sign sign) {
        return sign.getType().name().contains("HANGING_SIGN")
                ? BlockEntityTypes.HANGING_SIGN
                : BlockEntityTypes.SIGN;
    }

    private static String blockEntityId(Sign sign) {
        return sign.getType().name().contains("HANGING_SIGN")
                ? "minecraft:hanging_sign"
                : "minecraft:sign";
    }

    private static synchronized void ensureRefreshExecutor() {
        if (refreshExecutor == null || refreshExecutor.isShutdown()) {
            refreshExecutor = Executors.newSingleThreadExecutor(REFRESH_THREAD_FACTORY);
        }
    }

    private static void scheduleDrain() {
        if (!DRAIN_SCHEDULED.compareAndSet(false, true)) {
            return;
        }

        ensureRefreshExecutor();
        try {
            refreshExecutor.execute(SignFakeViewService::drainRefreshes);
        } catch (RejectedExecutionException ignored) {
            DRAIN_SCHEDULED.set(false);
        }
    }

    private static void drainRefreshes() {
        try {
            Map<RefreshGroup, List<RefreshRequest>> grouped = drainPendingRefreshes();
            for (Map.Entry<RefreshGroup, List<RefreshRequest>> entry : grouped.entrySet()) {
                scheduleRegionScan(entry.getKey(), entry.getValue());
            }
        } finally {
            DRAIN_SCHEDULED.set(false);
            if (!PENDING_REFRESHES.isEmpty()) {
                scheduleDrain();
            }
        }
    }

    private static Map<RefreshGroup, List<RefreshRequest>> drainPendingRefreshes() {
        ObjectOpenHashSet<RefreshRequest> uniqueRequests = new ObjectOpenHashSet<>();
        RefreshRequest request;
        while ((request = PENDING_REFRESHES.poll()) != null) {
            uniqueRequests.add(request);
        }

        return uniqueRequests.stream().collect(Collectors.groupingBy(refreshRequest -> new RefreshGroup(
                refreshRequest.viewerId(),
                refreshRequest.blockKey().worldId(),
                refreshRequest.blockKey().chunkX(),
                refreshRequest.blockKey().chunkZ()
        ), Object2ObjectOpenHashMap::new, Collectors.toList()));
    }

    private static void scheduleRegionScan(RefreshGroup group, List<RefreshRequest> requests) {
        if (requests.isEmpty()) {
            return;
        }

        RefreshRequest firstRequest = requests.getFirst();
        World world = firstRequest.world();
        BlockKey anchor = firstRequest.blockKey();
        Location anchorLocation = new Location(world, anchor.x(), anchor.y(), anchor.z());
        // we are processing single chunk, use first loc is fine
        AdvancedSensitiveWords.getScheduler().runTask(anchorLocation, () -> scanAndSend(group, world, requests));
    }

    private static void scanAndSend(RefreshGroup group, World world, List<RefreshRequest> requests) {
        if (!isOperational()) {
            return;
        }

        Player viewer = Bukkit.getPlayer(group.viewerId());
        if (viewer == null || !viewer.getWorld().equals(world)) {
            return;
        }

        for (RefreshRequest request : requests) {
            BlockKey key = request.blockKey();
            Location location = new Location(world, key.x(), key.y(), key.z());
            SignFakeRecord record = readRecordAt(location);
            if (record != null) {
                sendRecordView(viewer, location, record);
            }
        }
    }

    private static Component[] toComponentLines(List<Component> components) {
        Component[] lines = BLANK_LINES.clone();
        int count = Math.min(components.size(), lines.length);
        for (int i = 0; i < count; i++) {
            lines[i] = components.get(i);
        }
        return lines;
    }

    private static String encodeComponents(Component[] lines) {
        String[] encoded = new String[4];
        for (int i = 0; i < encoded.length; i++) {
            Component line = i < lines.length ? lines[i] : Component.empty();
            encoded[i] = encode(COMPONENT_SERIALIZER.serialize(line));
        }
        return String.join(LINE_SEPARATOR, encoded);
    }

    private static Component[] decodeComponents(String encodedLines) {
        String[] split = encodedLines.split("\\" + LINE_SEPARATOR, -1);
        Component[] lines = BLANK_LINES.clone();
        int count = Math.min(split.length, lines.length);
        for (int i = 0; i < count; i++) {
            lines[i] = COMPONENT_SERIALIZER.deserialize(decode(split[i]));
        }
        return lines;
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static NamespacedKey key(String value) {
        return new NamespacedKey(AdvancedSensitiveWords.getInstance(), value);
    }

    private record RefreshRequest(UUID viewerId, World world, BlockKey blockKey) {
    }

    private record RefreshGroup(UUID viewerId, UUID worldId, int chunkX, int chunkZ) {
    }
}
