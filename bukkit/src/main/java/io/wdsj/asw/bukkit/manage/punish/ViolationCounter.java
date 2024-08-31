package io.wdsj.asw.bukkit.manage.punish;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ViolationCounter {
    private static final Map<UUID, Long> violationCountMap = new ConcurrentHashMap<>();

    public static long getViolationCount(Player player) {
        return violationCountMap.getOrDefault(player.getUniqueId(), 0L);
    }

    public static void incrementViolationCount(Player player, long count) {
        violationCountMap.merge(player.getUniqueId(), count, Long::sum);
    }

    public static void incrementViolationCount(Player player) {
        incrementViolationCount(player, 1);
    }

    public static void resetViolationCount(Player player) {
        violationCountMap.remove(player.getUniqueId());
    }

    public static boolean hasViolation(Player player) {
        return violationCountMap.containsKey(player.getUniqueId());
    }

    public static void resetAllViolations() {
        violationCountMap.clear();
    }
}