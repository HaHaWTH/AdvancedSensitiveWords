package io.wdsj.asw.bukkit.manage.punish;

import io.wdsj.asw.bukkit.type.ModuleType;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public enum ViolationCounter {
    INSTANCE;
    private final Map<UUID, Map<ModuleType, Long>> violationCountMap = new ConcurrentHashMap<>();

    public long getViolationCount(Player player, ModuleType moduleType) {
        return getViolationCount(player.getUniqueId(), moduleType);
    }

    public long getViolationCount(UUID playerId, ModuleType moduleType) {
        if (!moduleType.isViolationTracked()) {
            return 0L;
        }
        return violationCountMap.getOrDefault(playerId, Map.of()).getOrDefault(moduleType, 0L);
    }

    public long getTotalViolationCount(Player player) {
        return getTotalViolationCount(player.getUniqueId());
    }

    public long getTotalViolationCount(UUID playerId) {
        return violationCountMap.getOrDefault(playerId, Map.of()).values().stream()
                .mapToLong(Long::longValue)
                .sum();
    }

    public void incrementViolationCount(Player player, ModuleType moduleType) {
        incrementViolationCount(player.getUniqueId(), moduleType, 1L);
    }

    public void incrementViolationCount(UUID playerId, ModuleType moduleType, long count) {
        requireTrackedModule(moduleType);
        violationCountMap.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .merge(moduleType, count, Long::sum);
    }

    public void resetViolationCount(Player player) {
        resetViolationCount(player.getUniqueId());
    }

    public void resetViolationCount(Player player, ModuleType moduleType) {
        resetViolationCount(player.getUniqueId(), moduleType);
    }

    public void resetViolationCount(UUID playerId) {
        violationCountMap.remove(playerId);
    }

    public void resetViolationCount(UUID playerId, ModuleType moduleType) {
        requireTrackedModule(moduleType);
        violationCountMap.computeIfPresent(playerId, (ignored, counts) -> {
            counts.remove(moduleType);
            return counts.isEmpty() ? null : counts;
        });
    }

    public boolean hasViolation(Player player, ModuleType moduleType) {
        return getViolationCount(player, moduleType) > 0L;
    }

    public void resetAllViolations() {
        violationCountMap.clear();
    }

    private static void requireTrackedModule(ModuleType moduleType) {
        if (!moduleType.isViolationTracked()) {
            throw new IllegalArgumentException(moduleType + " does not have a violation counter");
        }
    }
}
