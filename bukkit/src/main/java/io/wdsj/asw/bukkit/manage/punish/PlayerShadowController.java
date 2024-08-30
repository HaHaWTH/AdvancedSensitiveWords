package io.wdsj.asw.bukkit.manage.punish;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player shadow controller
 */
public class PlayerShadowController {
    private static final Map<UUID, StartAndDuration> SHADOWED_PLAYERS = new ConcurrentHashMap<>();

    /**
     * Add player to shadowed players
     * @param player to shadow
     * @param start Start time, in milliseconds
     * @param duration Duration, in seconds
     */
    public static void shadowPlayer(Player player, long start, long duration) {
        SHADOWED_PLAYERS.put(player.getUniqueId(), new StartAndDuration(start, duration));
    }

    /**
     * Remove player from shadowed players
     * @param player to unshadow
     */
    public static void unshadowPlayer(Player player) {
        SHADOWED_PLAYERS.remove(player.getUniqueId());
    }

    private static void unshadowPlayer(UUID uuid) {
        SHADOWED_PLAYERS.remove(uuid);
    }

    /**
     * Check if player is shadowed
     * @param player to check
     * @return true if player is shadowed, false otherwise
     */
    public static boolean isShadowed(Player player) {
        UUID uuid = player.getUniqueId();
        if (!SHADOWED_PLAYERS.containsKey(uuid)) return false;
        StartAndDuration startAndDuration = SHADOWED_PLAYERS.get(uuid);
        long currentTime = System.currentTimeMillis();
        if (currentTime - startAndDuration.getStart() > startAndDuration.getDuration() * 1000) {
            unshadowPlayer(uuid);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Clear all shadowed players
     */
    public static void clear() {
        SHADOWED_PLAYERS.clear();
    }

    /**
     * Class to store start time and duration
     */
    private static class StartAndDuration {
        private final long start;
        private final long duration;

        /**
         * Constructor
         * @param start Start time, in milliseconds
         * @param duration Duration, in seconds
         */
        public StartAndDuration(long start, long duration) {
            this.start = start;
            this.duration = duration;
        }

        /**
         * Get start time
         * @return Start time, in milliseconds
         */
        public long getStart() {
            return start;
        }

        /**
         * Get duration
         * @return Duration, in seconds
         */
        public long getDuration() {
            return duration;
        }
    }
}
