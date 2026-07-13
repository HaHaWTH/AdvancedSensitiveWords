package io.wdsj.asw.velocity.sync;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.wdsj.asw.common.sync.VelocitySyncProtocol;
import io.wdsj.asw.common.type.ModuleType;
import io.wdsj.asw.velocity.AdvancedSensitiveWords;
import io.wdsj.asw.velocity.config.Config;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class VelocitySyncWebSocketServer implements AutoCloseable {
    private static final long AUTH_WINDOW_MILLIS = 60_000L;

    private final AdvancedSensitiveWords plugin;
    private final Logger logger;
    private final Config config;
    private final SyncServer server;
    private final ScheduledExecutorService scheduler;
    private final Map<WebSocket, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, Long> nonceHistory = new ConcurrentHashMap<>();
    private final Map<UUID, Map<ModuleType, AtomicLong>> counts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> shadowExpiresAt = new ConcurrentHashMap<>();

    public VelocitySyncWebSocketServer(AdvancedSensitiveWords plugin, Logger logger, Config config) {
        this.plugin = plugin;
        this.logger = logger;
        this.config = config;
        this.server = new SyncServer(new InetSocketAddress(config.velocity_sync_host, config.velocity_sync_port));
        this.scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "ASW Velocity Sync");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        server.start();
        scheduler.scheduleAtFixedRate(this::closeStaleSessions, 10L, 10L, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(
                this::resetAll,
                config.velocity_sync_reset_interval_minutes,
                config.velocity_sync_reset_interval_minutes,
                TimeUnit.MINUTES
        );
        logger.info("Velocity sync WebSocket listening on {}:{}.", config.velocity_sync_host, config.velocity_sync_port);
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
        try {
            server.stop(1000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        sessions.clear();
        nonceHistory.clear();
        counts.clear();
        shadowExpiresAt.clear();
    }

    private void handle(WebSocket socket, String raw) {
        JsonObject message;
        try {
            message = JsonParser.parseString(raw).getAsJsonObject();
        } catch (Exception exception) {
            socket.close(1003, "Invalid JSON");
            return;
        }
        Session session = sessions.computeIfAbsent(socket, ignored -> new Session());
        String type = string(message, "type");
        if (VelocitySyncProtocol.TYPE_HELLO.equals(type)) {
            handleHello(socket, session, message);
            return;
        }
        if (!session.authenticated) {
            socket.close(1008, "Authentication required");
            return;
        }
        session.lastSeen = Instant.now().toEpochMilli();
        switch (type) {
            case null -> socket.close(1008, "Invalid message");
            case VelocitySyncProtocol.TYPE_VL_INCREMENT -> handleIncrement(socket, message);
            case VelocitySyncProtocol.TYPE_VL_QUERY -> sendSync(socket, uuid(message, "playerUuid"));
            case VelocitySyncProtocol.TYPE_VL_RESET_REQUEST -> handleResetRequest(message);
            case VelocitySyncProtocol.TYPE_SHADOW_SET -> handleShadowSet(message);
            case VelocitySyncProtocol.TYPE_SHADOW_CLEAR -> handleShadowClear(message);
            case VelocitySyncProtocol.TYPE_SHADOW_QUERY -> sendShadowSync(socket, uuid(message, "playerUuid"));
            case VelocitySyncProtocol.TYPE_PING -> socket.send(base(VelocitySyncProtocol.TYPE_PONG).toString());
            default -> {
            }
        }
    }

    private void handleHello(WebSocket socket, Session session, JsonObject message) {
        String serverId = string(message, "serverId");
        String nonce = string(message, "nonce");
        String signature = string(message, "signature");
        long timestamp = longValue(message, "timestamp", 0L);
        long now = Instant.now().toEpochMilli();
        if (serverId == null || nonce == null || Math.abs(now - timestamp) > AUTH_WINDOW_MILLIS) {
            socket.close(1008, "Invalid hello");
            return;
        }
        if (!config.velocity_sync_allowed_server_ids.isEmpty()
                && config.velocity_sync_allowed_server_ids.stream().noneMatch(serverId::equals)) {
            socket.close(1008, "Server id not allowed");
            return;
        }
        Long previousNonce = nonceHistory.putIfAbsent(serverId + ":" + nonce, now);
        if (previousNonce != null) {
            socket.close(1008, "Repeated nonce");
            return;
        }
        String expected = VelocitySyncProtocol.signature(config.velocity_sync_secret, serverId, nonce, timestamp);
        if (!VelocitySyncProtocol.signatureMatches(expected, signature)) {
            socket.close(1008, "Bad signature");
            return;
        }
        session.authenticated = true;
        session.serverId = serverId;
        session.lastSeen = now;
        socket.send(base(VelocitySyncProtocol.TYPE_HELLO_OK).toString());
        logger.info("Velocity sync backend '{}' authenticated.", serverId);
    }

    private void handleIncrement(WebSocket socket, JsonObject message) {
        UUID playerId = uuid(message, "playerUuid");
        ModuleType module = parseModule(string(message, "module"));
        long delta = longValue(message, "delta", 0L);
        if (playerId == null || module == null || delta <= 0L) {
            return;
        }
        counts.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(module, ignored -> new AtomicLong())
                .addAndGet(delta);
        broadcastSync(playerId);
    }

    private void handleResetRequest(JsonObject message) {
        UUID playerId = uuid(message, "playerUuid");
        if (playerId == null) {
            return;
        }
        ModuleType module = parseModule(string(message, "module"));
        if (module == null) {
            counts.remove(playerId);
        } else {
            Map<ModuleType, AtomicLong> playerCounts = counts.get(playerId);
            if (playerCounts != null) {
                playerCounts.remove(module);
                if (playerCounts.isEmpty()) {
                    counts.remove(playerId);
                }
            }
        }
        broadcastReset(playerId, module);
    }

    private void resetAll() {
        counts.clear();
        broadcast(base(VelocitySyncProtocol.TYPE_VL_RESET_ALL));
    }

    private void handleShadowSet(JsonObject message) {
        UUID playerId = uuid(message, "playerUuid");
        long expiresAtMillis = longValue(message, "expiresAtMillis", 0L);
        if (playerId == null) {
            return;
        }
        if (expiresAtMillis <= Instant.now().toEpochMilli()) {
            shadowExpiresAt.remove(playerId);
            broadcastShadowSync(playerId, 0L);
            return;
        }
        shadowExpiresAt.put(playerId, expiresAtMillis);
        broadcastShadowSync(playerId, expiresAtMillis);
    }

    private void handleShadowClear(JsonObject message) {
        UUID playerId = uuid(message, "playerUuid");
        if (playerId == null) {
            return;
        }
        shadowExpiresAt.remove(playerId);
        broadcastShadowSync(playerId, 0L);
    }

    private void sendShadowSync(WebSocket socket, UUID playerId) {
        if (playerId == null) {
            return;
        }
        long expiresAtMillis = activeShadowExpiresAt(playerId);
        socket.send(shadowSyncMessage(playerId, expiresAtMillis).toString());
    }

    private void broadcastShadowSync(UUID playerId, long expiresAtMillis) {
        broadcast(shadowSyncMessage(playerId, expiresAtMillis));
    }

    private JsonObject shadowSyncMessage(UUID playerId, long expiresAtMillis) {
        JsonObject message = base(VelocitySyncProtocol.TYPE_SHADOW_SYNC);
        message.addProperty("playerUuid", playerId.toString());
        message.addProperty("expiresAtMillis", expiresAtMillis);
        return message;
    }

    private long activeShadowExpiresAt(UUID playerId) {
        Long expiresAtMillis = shadowExpiresAt.get(playerId);
        if (expiresAtMillis == null) {
            return 0L;
        }
        if (expiresAtMillis <= Instant.now().toEpochMilli()) {
            shadowExpiresAt.remove(playerId, expiresAtMillis);
            return 0L;
        }
        return expiresAtMillis;
    }

    private void broadcastSync(UUID playerId) {
        JsonObject message = syncMessage(playerId);
        if (message != null) {
            broadcast(message);
        }
    }

    private void sendSync(WebSocket socket, UUID playerId) {
        JsonObject message = syncMessage(playerId);
        if (message != null) {
            socket.send(message.toString());
        }
    }

    private JsonObject syncMessage(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        JsonObject message = base(VelocitySyncProtocol.TYPE_VL_SYNC);
        message.addProperty("playerUuid", playerId.toString());
        JsonObject values = new JsonObject();
        Map<ModuleType, AtomicLong> playerCounts = counts.getOrDefault(playerId, Map.of());
        for (ModuleType module : ModuleType.violationModules()) {
            values.addProperty(module.name(), playerCounts.getOrDefault(module, new AtomicLong()).get());
        }
        message.add("counts", values);
        return message;
    }

    private void broadcastReset(UUID playerId, ModuleType module) {
        JsonObject message = base(VelocitySyncProtocol.TYPE_VL_RESET);
        message.addProperty("playerUuid", playerId.toString());
        if (module != null) {
            message.addProperty("module", module.name());
        }
        broadcast(message);
    }

    private void broadcast(JsonObject message) {
        String raw = message.toString();
        for (Map.Entry<WebSocket, Session> entry : sessions.entrySet()) {
            if (entry.getValue().authenticated && entry.getKey().isOpen()) {
                entry.getKey().send(raw);
            }
        }
    }

    private JsonObject base(String type) {
        JsonObject object = new JsonObject();
        object.addProperty("version", VelocitySyncProtocol.VERSION);
        object.addProperty("type", type);
        object.addProperty("timestamp", Instant.now().toEpochMilli());
        return object;
    }

    private void closeStaleSessions() {
        long now = Instant.now().toEpochMilli();
        long timeoutMillis = config.velocity_sync_heartbeat_timeout_seconds * 1000L;
        nonceHistory.entrySet().removeIf(entry -> now - entry.getValue() > AUTH_WINDOW_MILLIS);
        sessions.forEach((socket, session) -> {
            if (session.authenticated && now - session.lastSeen > timeoutMillis) {
                socket.close(1001, "Heartbeat timeout");
            }
        });
        shadowExpiresAt.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private static ModuleType parseModule(String module) {
        return ModuleType.parseViolationModule(module);
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

    private static long longValue(JsonObject object, String key, long fallback) {
        try {
            JsonElement element = object.get(key);
            return element == null || element.isJsonNull() ? fallback : element.getAsLong();
        } catch (Exception exception) {
            return fallback;
        }
    }

    private final class SyncServer extends WebSocketServer {
        private SyncServer(InetSocketAddress address) {
            super(address);
        }

        @Override
        public void onOpen(WebSocket socket, ClientHandshake handshake) {
            if (!VelocitySyncProtocol.PATH.equals(handshake.getResourceDescriptor())) {
                socket.close(1008, "Invalid path");
            }
        }

        @Override
        public void onClose(WebSocket socket, int code, String reason, boolean remote) {
            Session session = sessions.remove(socket);
            if (session != null && session.authenticated) {
                logger.info("Velocity sync backend '{}' disconnected.", session.serverId);
            }
        }

        @Override
        public void onMessage(WebSocket socket, String message) {
            handle(socket, message);
        }

        @Override
        public void onError(WebSocket socket, Exception exception) {
            logger.warn("Velocity sync WebSocket error.", exception);
        }

        @Override
        public void onStart() {
            setConnectionLostTimeout(config.velocity_sync_heartbeat_timeout_seconds);
        }
    }

    private static final class Session {
        private boolean authenticated;
        private String serverId = "";
        private long lastSeen = Instant.now().toEpochMilli();
    }
}
