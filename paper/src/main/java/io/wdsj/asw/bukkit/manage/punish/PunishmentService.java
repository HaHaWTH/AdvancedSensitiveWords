package io.wdsj.asw.bukkit.manage.punish;

import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender;
import io.wdsj.asw.bukkit.setting.PaperConfigurationService;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.type.ModuleType;
import io.wdsj.asw.bukkit.util.SchedulingUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;

/** Executes configured punishment actions against online players. */
public final class PunishmentService {
    private static final Map<String, String> LEGACY_EFFECT_ALIASES = Map.ofEntries(
            Map.entry("slow", "slowness"),
            Map.entry("fast_digging", "haste"),
            Map.entry("slow_digging", "mining_fatigue"),
            Map.entry("increase_damage", "strength"),
            Map.entry("heal", "instant_health"),
            Map.entry("harm", "instant_damage"),
            Map.entry("jump", "jump_boost"),
            Map.entry("confusion", "nausea"),
            Map.entry("damage_resistance", "resistance")
    );

    private final PaperConfigurationService configuration;

    public PunishmentService(PaperConfigurationService configuration) {
        this.configuration = configuration;
    }

    /** Executes actions using the violation count of the supplied detection module. */
    public void executeForModule(Player player, ModuleType moduleType, List<String> actions) {
        executeActions(player, actions, ViolationCounter.INSTANCE.getViolationCount(player, moduleType));
    }

    /** Executes manual default actions using the player's total violations across all modules. */
    public void executeManual(Player player, List<String> actions) {
        executeActions(player, actions, ViolationCounter.INSTANCE.getTotalViolationCount(player));
    }

    /** Executes one administrator-supplied action using the player's total violations across all modules. */
    public void executeManualMethod(Player player, String method) {
        processSinglePunish(player, method, ViolationCounter.INSTANCE.getTotalViolationCount(player));
    }

    private void executeActions(Player player, List<String> actions, long violationCount) {
        for (String action : actions) {
            try {
                processSinglePunish(player, action, violationCount);
            } catch (IllegalArgumentException exception) {
                LOGGER.warn("Invalid punishment method: {}", action);
            }
        }
    }

    private void processSinglePunish(Player player, String method, long violationCount) {
        String[] parts = method.split("\\|");
        PunishmentType punishmentType = PunishmentType.getType(parts[0]);
        if (punishmentType == null) {
            throw new IllegalArgumentException("Invalid punishment method " + parts[0].toUpperCase(Locale.ROOT));
        }
        if (hasViolationCondition(parts) && !checkViolationCondition(parts[parts.length - 1].substring(2), violationCount)) {
            return;
        }

        switch (punishmentType) {
            case DAMAGE -> damage(player, parts);
            case HOSTILE -> makeHostileTowardsPlayer(player, parseDouble(parts, 1, 10.0D));
            case COMMAND -> executeConsoleCommand(player, parts);
            case COMMAND_PROXY -> executeProxyCommand(player, parts);
            case EFFECT -> applyPotionEffect(player, parts);
            case SHADOW -> shadow(player, parts);
        }
    }

    private static boolean hasViolationCondition(String[] parts) {
        return parts.length > 2
                && parts[parts.length - 1].length() > 2
                && parts[parts.length - 1].regionMatches(true, 0, "VL", 0, 2);
    }

    private static void damage(Player player, String[] parts) {
        double damage = parseDouble(parts, 1, 1.0D);
        SchedulingUtils.runSyncAtEntityIfFolia(player, () -> player.damage(damage));
    }

    private static double parseDouble(String[] parts, int index, double fallback) {
        if (parts.length <= index) {
            return fallback;
        }
        try {
            return Double.parseDouble(parts[index]);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static void executeConsoleCommand(Player player, String[] parts) {
        requireArgument(parts, 1);
        String command = replacePlayerPlaceholder(parts[1], player);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    private void executeProxyCommand(Player player, String[] parts) {
        requireArgument(parts, 1);
        if (configuration.get(PluginSettings.HOOK_VELOCITY)) {
            VelocitySender.executeVelocityCommand(player, replacePlayerPlaceholder(parts[1], player));
        }
    }

    private static void applyPotionEffect(Player player, String[] parts) {
        requireArgument(parts, 1);
        PotionEffectType potionEffect = resolvePotionEffectType(parts[1]);
        if (potionEffect == null) {
            throw new IllegalArgumentException("Unknown potion effect");
        }
        int duration = parseInteger(parts, 2, 10) * 20;
        int amplifier = parseInteger(parts, 3, 0);
        SchedulingUtils.runSyncAtEntityIfFolia(player, () -> player.addPotionEffect(new PotionEffect(potionEffect, duration, amplifier)));
    }

    private static void shadow(Player player, String[] parts) {
        long durationSeconds = parseLong(parts, 1, 30L);
        PlayerShadowController.shadowPlayer(player, Duration.ofSeconds(durationSeconds));
    }

    private static int parseInteger(String[] parts, int index, int fallback) {
        if (parts.length <= index || hasViolationCondition(parts) && index == parts.length - 1) {
            return fallback;
        }
        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static long parseLong(String[] parts, int index, long fallback) {
        if (parts.length <= index || hasViolationCondition(parts) && index == parts.length - 1) {
            return fallback;
        }
        try {
            return Long.parseLong(parts[index]);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static void requireArgument(String[] parts, int index) {
        if (parts.length <= index || parts[index].isBlank()) {
            throw new IllegalArgumentException("Not enough args");
        }
    }

    private static String replacePlayerPlaceholder(String value, Player player) {
        return value.replace("%player%", player.getName()).replace("%PLAYER%", player.getName());
    }

    private static PotionEffectType resolvePotionEffectType(String effect) {
        String normalizedEffect = effect.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (normalizedEffect.isEmpty()) {
            return null;
        }
        NamespacedKey key = normalizedEffect.contains(":")
                ? NamespacedKey.fromString(normalizedEffect)
                : NamespacedKey.minecraft(LEGACY_EFFECT_ALIASES.getOrDefault(normalizedEffect, normalizedEffect));
        return key == null ? null : Registry.EFFECT.get(key);
    }

    private static void makeHostileTowardsPlayer(Player target, double radius) {
        SchedulingUtils.runSyncAtEntityIfFolia(target, () -> {
            List<Entity> entities = target.getNearbyEntities(radius, radius, radius);
            for (Entity entity : entities) {
                SchedulingUtils.runSyncAtEntityIfFolia(entity, () -> {
                    if (entity instanceof Mob mob && !entity.hasMetadata("NPC")) {
                        mob.setTarget(target);
                    }
                });
            }
        });
    }

    private static boolean checkViolationCondition(String condition, long violationCount) {
        try {
            if (condition.startsWith("=")) {
                return violationCount == Long.parseLong(condition.substring(1));
            }
            if (condition.startsWith(">")) {
                return violationCount > Long.parseLong(condition.substring(1));
            }
            if (condition.startsWith("<")) {
                return violationCount < Long.parseLong(condition.substring(1));
            }
            return true;
        } catch (NumberFormatException exception) {
            LOGGER.warn("Invalid violation condition: {}", condition);
            return false;
        }
    }
}
