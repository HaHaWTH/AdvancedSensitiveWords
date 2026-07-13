package io.wdsj.asw.bukkit.manage.punish;

import org.bukkit.entity.Player;

import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.proxy.velocity.sync.VelocitySyncClient;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player shadow controller
 */
public final class PlayerShadowController {
    private static final Map<UUID, ShadowBan> SHADOWED_PLAYERS = new ConcurrentHashMap<>();

    private PlayerShadowController() {
    }

    /**
     * Add player to shadowed players
     * @param player to shadow
     * @param duration shadow duration
     */
    public static void shadowPlayer(Player player, Duration duration) {
        Objects.requireNonNull(player, "player");
        shadowPlayer(player.getUniqueId(), duration);
    }

    /**
     * Add player to shadowed players
     * @param uuid player uuid
     * @param duration shadow duration
     */
    public static void shadowPlayer(UUID uuid, Duration duration) {
        shadowPlayer(uuid, duration, true);
    }

    public static void shadowPlayer(UUID uuid, Duration duration, boolean notifyProxy) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(duration, "duration");
        if (duration.isZero() || duration.isNegative()) {
            unshadowPlayer(uuid, notifyProxy);
            return;
        }

        long expiresAtMillis;
        try {
            expiresAtMillis = Math.addExact(System.currentTimeMillis(), duration.toMillis());
        } catch (ArithmeticException ignored) {
            expiresAtMillis = Long.MAX_VALUE;
        }
        shadowPlayerUntil(uuid, expiresAtMillis, notifyProxy);
    }

    public static void shadowPlayerUntil(UUID uuid, long expiresAtMillis, boolean notifyProxy) {
        Objects.requireNonNull(uuid, "uuid");
        if (expiresAtMillis <= System.currentTimeMillis()) {
            unshadowPlayer(uuid, notifyProxy);
            return;
        }
        SHADOWED_PLAYERS.put(uuid, new ShadowBan(expiresAtMillis));
        if (notifyProxy) {
            VelocitySyncClient client = AdvancedSensitiveWords.getInstance().getVelocitySyncClient();
            if (client != null) {
                client.sendShadowSet(uuid, expiresAtMillis);
            }
        }
    }

    /**
     * Remove player from shadowed players
     * @param player to unshadow
     */
    public static void unshadowPlayer(Player player) {
        Objects.requireNonNull(player, "player");
        unshadowPlayer(player.getUniqueId());
    }

    /**
     * Remove player from shadowed players
     * @param uuid player uuid
     */
    public static void unshadowPlayer(UUID uuid) {
        unshadowPlayer(uuid, true);
    }

    public static void unshadowPlayer(UUID uuid, boolean notifyProxy) {
        Objects.requireNonNull(uuid, "uuid");
        SHADOWED_PLAYERS.remove(uuid);
        if (notifyProxy) {
            VelocitySyncClient client = AdvancedSensitiveWords.getInstance().getVelocitySyncClient();
            if (client != null) {
                client.sendShadowClear(uuid);
            }
        }
    }

    /**
     * Check if player is shadowed
     * @param player to check
     * @return true if player is shadowed, false otherwise
     */
    public static boolean isShadowed(Player player) {
        Objects.requireNonNull(player, "player");
        return isShadowed(player.getUniqueId());
    }

    /**
     * Check if player is shadowed
     * @param uuid player uuid
     * @return true if player is shadowed, false otherwise
     */
    public static boolean isShadowed(UUID uuid) {
        return getActiveShadowBan(uuid).isPresent();
    }

    /**
     * Get remaining shadow duration for a player.
     * @param player player to check
     * @return remaining duration, or empty if player is not shadowed
     */
    public static Optional<Duration> getRemainingDuration(Player player) {
        Objects.requireNonNull(player, "player");
        return getRemainingDuration(player.getUniqueId());
    }

    /**
     * Get remaining shadow duration for a player.
     * @param uuid player uuid
     * @return remaining duration, or empty if player is not shadowed
     */
    public static Optional<Duration> getRemainingDuration(UUID uuid) {
        long currentTimeMillis = System.currentTimeMillis();
        return getActiveShadowBan(uuid, currentTimeMillis)
                .map(shadowBan -> Duration.ofMillis(shadowBan.expiresAtMillis() - currentTimeMillis));
    }

    /**
     * Clear all shadowed players
     */
    public static void clear() {
        SHADOWED_PLAYERS.clear();
    }

    private record ShadowBan(long expiresAtMillis) {
    }

    private static Optional<ShadowBan> getActiveShadowBan(UUID uuid) {
        return getActiveShadowBan(uuid, System.currentTimeMillis());
    }

    private static Optional<ShadowBan> getActiveShadowBan(UUID uuid, long currentTimeMillis) {
        Objects.requireNonNull(uuid, "uuid");
        ShadowBan shadowBan = SHADOWED_PLAYERS.get(uuid);
        if (shadowBan == null) {
            return Optional.empty();
        }
        if (currentTimeMillis >= shadowBan.expiresAtMillis()) {
            SHADOWED_PLAYERS.remove(uuid, shadowBan);
            return Optional.empty();
        }
        return Optional.of(shadowBan);
    }
}
