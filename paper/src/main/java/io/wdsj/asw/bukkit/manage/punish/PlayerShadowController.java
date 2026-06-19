package io.wdsj.asw.bukkit.manage.punish;

import org.bukkit.entity.Player;

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
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(duration, "duration");
        if (duration.isZero() || duration.isNegative()) {
            unshadowPlayer(uuid);
            return;
        }

        long expiresAtMillis;
        try {
            expiresAtMillis = Math.addExact(System.currentTimeMillis(), duration.toMillis());
        } catch (ArithmeticException ignored) {
            expiresAtMillis = Long.MAX_VALUE;
        }
        SHADOWED_PLAYERS.put(uuid, new ShadowBan(expiresAtMillis));
    }

    /**
     * Remove player from shadowed players
     * @param player to unshadow
     */
    public static void unshadowPlayer(Player player) {
        Objects.requireNonNull(player, "player");
        SHADOWED_PLAYERS.remove(player.getUniqueId());
    }

    /**
     * Remove player from shadowed players
     * @param uuid player uuid
     */
    public static void unshadowPlayer(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        SHADOWED_PLAYERS.remove(uuid);
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
