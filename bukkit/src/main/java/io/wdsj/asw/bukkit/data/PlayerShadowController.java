package io.wdsj.asw.bukkit.data;

import org.bukkit.entity.Player;

import java.util.HashMap;

public class PlayerShadowController {
    private static final HashMap<Player, StartAndDuration> SHADOWED_PLAYERS = new HashMap<>();

    /**
     * Add player to shadowed players
     * @param player to shadow
     * @param start Start time, in milliseconds
     * @param duration Duration, in seconds
     */
    public static void shadowPlayer(Player player, long start, long duration) {
        SHADOWED_PLAYERS.put(player, new StartAndDuration(start, duration));
    }

    public static void unshadowPlayer(Player player) {
        SHADOWED_PLAYERS.remove(player);
    }

    public static boolean isShadowed(Player player) {
        if (!SHADOWED_PLAYERS.containsKey(player)) return false;
        StartAndDuration startAndDuration = SHADOWED_PLAYERS.get(player);
        long currentTime = System.currentTimeMillis();
        if (currentTime - startAndDuration.getStart() > startAndDuration.getDuration() * 1000) {
            unshadowPlayer(player);
            return false;
        } else {
            return true;
        }
    }

    private static class StartAndDuration {
        private final long start;
        private final long duration;

        public StartAndDuration(long start, long duration) {
            this.start = start;
            this.duration = duration;
        }

        public long getStart() {
            return start;
        }

        public long getDuration() {
            return duration;
        }
    }
}
