package io.wdsj.asw.bukkit.proxy.velocity.sync;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.manage.notice.Notifier;
import io.wdsj.asw.bukkit.manage.punish.ViolationCounter;
import io.wdsj.asw.bukkit.setting.PaperConfigurationService;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.type.ModuleType;
import io.wdsj.asw.bukkit.util.message.MessageUtils;
import io.wdsj.asw.common.environment.PluginBuildInfo;
import io.wdsj.asw.common.sync.VelocitySyncProtocol;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;

import java.net.URI;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class VelocitySyncClient implements Listener, AutoCloseable {
    private final AdvancedSensitiveWords plugin;
    private final PaperConfigurationService configuration;
    private final Logger logger;
    private final ScheduledExecutorService executor;
    private volatile BackendClient client;
    private volatile boolean closed;
    private volatile boolean authenticated;

    public VelocitySyncClient(AdvancedSensitiveWords plugin) {
        this.plugin = plugin;
        this.configuration = plugin.getConfigurationService();
        this.logger = AdvancedSensitiveWords.LOGGER;
        this.executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "ASW Velocity Sync");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        if (!enabled()) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        connectLater(0L);
        executor.scheduleAtFixedRate(this::sendPing, 10L, 10L, TimeUnit.SECONDS);
    }

    public boolean enabled() {
        return configuration.get(PluginSettings.VELOCITY_SYNC_ENABLED);
    }

    public boolean authenticated() {
        return authenticated && currentClientOpen();
    }

    public void sendIncrement(Player player, ModuleType moduleType, long delta) {
        if (!authenticated() || !moduleType.isViolationTracked()) {
            return;
        }
        JsonObject message = base(VelocitySyncProtocol.TYPE_VL_INCREMENT);
        message.addProperty("requestId", UUID.randomUUID().toString());
        message.addProperty("playerUuid", player.getUniqueId().toString());
        message.addProperty("playerName", player.getName());
        message.addProperty("module", moduleType.name());
        message.addProperty("delta", delta);
        send(message);
    }

    public boolean requestReset(Player player, ModuleType moduleType) {
        if (!authenticated()) {
            return false;
        }
        JsonObject message = base(VelocitySyncProtocol.TYPE_VL_RESET_REQUEST);
        message.addProperty("requestId", UUID.randomUUID().toString());
        message.addProperty("playerUuid", player.getUniqueId().toString());
        message.addProperty("playerName", player.getName());
        if (moduleType != null) {
            message.addProperty("module", moduleType.name());
        }
        return send(message);
    }

    public void query(Player player) {
        if (!authenticated()) {
            return;
        }
        JsonObject message = base(VelocitySyncProtocol.TYPE_VL_QUERY);
        message.addProperty("playerUuid", player.getUniqueId().toString());
        message.addProperty("playerName", player.getName());
        send(message);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        query(event.getPlayer());
    }

    @Override
    public void close() {
        closed = true;
        authenticated = false;
        HandlerList.unregisterAll(this);
        BackendClient current = client;
        if (current != null) {
            current.close();
        }
        executor.shutdownNow();
    }

    private void connectLater(long delaySeconds) {
        if (closed || !enabled()) {
            return;
        }
        executor.schedule(this::connect, delaySeconds, TimeUnit.SECONDS);
    }

    private void connect() {
        if (closed || !enabled() || currentClientOpen()) {
            return;
        }
        try {
            URI uri = URI.create(configuration.get(PluginSettings.VELOCITY_SYNC_URI));
            BackendClient next = new BackendClient(uri);
            client = next;
            authenticated = false;
            next.connect();
        } catch (Exception exception) {
            logger.warn("Failed to connect to Velocity sync WebSocket.", exception);
            connectLater(configuration.get(PluginSettings.VELOCITY_SYNC_RECONNECT_SECONDS));
        }
    }

    private boolean currentClientOpen() {
        BackendClient current = client;
        return current != null && current.isOpen();
    }

    private boolean send(JsonObject message) {
        BackendClient current = client;
        if (current == null || !current.isOpen()) {
            return false;
        }
        current.send(message.toString());
        return true;
    }

    private void sendPing() {
        if (authenticated()) {
            send(base(VelocitySyncProtocol.TYPE_PING));
        }
    }

    private JsonObject base(String type) {
        JsonObject object = new JsonObject();
        object.addProperty("version", VelocitySyncProtocol.VERSION);
        object.addProperty("type", type);
        object.addProperty("serverId", configuration.get(PluginSettings.VELOCITY_SYNC_SERVER_ID));
        object.addProperty("timestamp", Instant.now().toEpochMilli());
        return object;
    }

    private void sendHello() {
        String serverId = configuration.get(PluginSettings.VELOCITY_SYNC_SERVER_ID);
        String secret = configuration.get(PluginSettings.VELOCITY_SYNC_SECRET);
        long timestamp = Instant.now().toEpochMilli();
        String nonce = VelocitySyncProtocol.nonce();

        JsonObject hello = new JsonObject();
        hello.addProperty("version", VelocitySyncProtocol.VERSION);
        hello.addProperty("type", VelocitySyncProtocol.TYPE_HELLO);
        hello.addProperty("pluginVersion", PluginBuildInfo.VERSION);
        hello.addProperty("serverId", serverId);
        hello.addProperty("timestamp", timestamp);
        hello.addProperty("nonce", nonce);
        hello.addProperty("signature", VelocitySyncProtocol.signature(secret, serverId, nonce, timestamp));
        send(hello);
    }

    private void handleMessage(String raw) {
        JsonObject message;
        try {
            message = JsonParser.parseString(raw).getAsJsonObject();
        } catch (Exception exception) {
            logger.warn("Ignoring invalid Velocity sync message.");
            return;
        }

        String type = string(message, "type");
        if (VelocitySyncProtocol.TYPE_HELLO_OK.equals(type)) {
            authenticated = true;
            logger.info("Velocity sync WebSocket authenticated.");
            Bukkit.getOnlinePlayers().forEach(this::query);
            return;
        }
        if (!authenticated) {
            return;
        }
        switch (type) {
            case VelocitySyncProtocol.TYPE_VL_SYNC -> handleSync(message);
            case VelocitySyncProtocol.TYPE_VL_RESET -> handleReset(message);
            case VelocitySyncProtocol.TYPE_VL_RESET_ALL -> handleResetAll();
            case VelocitySyncProtocol.TYPE_PING -> send(base(VelocitySyncProtocol.TYPE_PONG));
            default -> {
            }
        }
    }

    private void handleSync(JsonObject message) {
        UUID playerId = uuid(message, "playerUuid");
        JsonObject counts = object(message, "counts");
        if (playerId == null || counts == null) {
            return;
        }
        Map<ModuleType, Long> snapshot = new EnumMap<>(ModuleType.class);
        for (Map.Entry<String, JsonElement> entry : counts.entrySet()) {
            try {
                ModuleType moduleType = ModuleType.valueOf(entry.getKey());
                snapshot.put(moduleType, entry.getValue().getAsLong());
            } catch (Exception ignored) {
            }
        }
        ViolationCounter.INSTANCE.setViolationSnapshot(playerId, snapshot);
    }

    private void handleReset(JsonObject message) {
        UUID playerId = uuid(message, "playerUuid");
        if (playerId == null) {
            return;
        }
        String module = string(message, "module");
        if (module == null || module.isBlank()) {
            ViolationCounter.INSTANCE.resetViolationCount(playerId);
            return;
        }
        try {
            ViolationCounter.INSTANCE.resetViolationCount(playerId, ModuleType.valueOf(module));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void handleResetAll() {
        ViolationCounter.INSTANCE.resetAllViolations();
        Notifier.normalNotice(MessageUtils.retrieveMessage(PluginMessages.MESSAGE_ON_VIOLATION_RESET));
    }

    private static JsonObject object(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static UUID uuid(JsonObject object, String key) {
        try {
            String value = string(object, key);
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    private final class BackendClient extends WebSocketClient {
        private BackendClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshake) {
            logger.info("Connected to Velocity sync WebSocket.");
            sendHello();
        }

        @Override
        public void onMessage(String message) {
            handleMessage(message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            authenticated = false;
            if (!closed) {
                logger.warn("Velocity sync WebSocket closed: {} {}", code, reason);
                connectLater(configuration.get(PluginSettings.VELOCITY_SYNC_RECONNECT_SECONDS));
            }
        }

        @Override
        public void onError(Exception exception) {
            if (!closed) {
                logger.warn("Velocity sync WebSocket error.", exception);
            }
        }
    }
}
