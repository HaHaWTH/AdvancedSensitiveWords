package io.wdsj.asw.bukkit.manage.punish;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ViolationCounter {
    private static final Map<UUID, Long> violationCountMap = new ConcurrentHashMap<>();

    /**
     * 获取指定玩家的违规次数。
     *
     * @param player 玩家对象
     * @return 违规次数，如果玩家没有记录，则返回 0
     */
    public static long getViolationCount(Player player) {
        return violationCountMap.getOrDefault(player.getUniqueId(), 0L);
    }

    /**
     * 增加指定玩家的违规次数。
     *
     * @param player 玩家对象
     * @param count  要增加的违规次数
     */
    public static void incrementViolationCount(Player player, long count) {
        violationCountMap.merge(player.getUniqueId(), count, Long::sum);
    }

    /**
     * 将指定玩家的违规次数增加 1。
     *
     * @param player 玩家对象
     */
    public static void incrementViolationCount(Player player) {
        incrementViolationCount(player, 1);
    }

    /**
     * 重置指定玩家的违规次数。
     *
     * @param player 玩家对象
     */
    public static void resetViolationCount(Player player) {
        violationCountMap.remove(player.getUniqueId());
    }

    /**
     * 检查玩家是否存在违规记录。
     *
     * @param player 玩家对象
     * @return 如果玩家有违规记录，则返回 true，否则返回 false
     */
    public static boolean hasViolation(Player player) {
        return violationCountMap.containsKey(player.getUniqueId());
    }

    public static void resetAllViolations() {
        violationCountMap.clear();
    }
}