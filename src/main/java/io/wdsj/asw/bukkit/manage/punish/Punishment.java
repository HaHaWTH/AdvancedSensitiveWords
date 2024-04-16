package io.wdsj.asw.bukkit.manage.punish;

import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.util.SchedulingUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Locale;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

@SuppressWarnings("deprecation")
public class Punishment {
    /**
     * 对玩家执行配置中定义的惩罚。
     * @param player 要惩罚的玩家
     */
    public static void punish(Player player) {
        List<String> punishList = settingsManager.getProperty(PluginSettings.PUNISHMENT);
        if (punishList.isEmpty()) return;
        for (String punish : punishList) {
            String[] normalPunish = punish.split("\\|");
            PunishmentType punishMethod = PunishmentType.valueOf(normalPunish[0].toUpperCase(Locale.ROOT));
            switch (punishMethod) {
                case DAMAGE:
                    try {
                        double damageAmount = (normalPunish.length == 2) ? Double.parseDouble(normalPunish[1]) : 1.0D;
                        SchedulingUtils.runSyncIfFolia(player, () -> player.damage(damageAmount));
                    } catch (NumberFormatException e) {
                        SchedulingUtils.runSyncIfFolia(player, () -> player.damage(1.0D));
                    }
                    break;
                case HOSTILE:
                    try {
                        double radius = (normalPunish.length == 2) ? Double.parseDouble(normalPunish[1]) : 10D;
                        makeHostileTowardsPlayer(player, radius);
                    } catch (NumberFormatException e) {
                        makeHostileTowardsPlayer(player, 10D);
                    }
                    break;
                case COMMAND:
                    if (normalPunish.length < 2) throw new IllegalArgumentException("Not enough args");
                    String command = normalPunish[1].replace("%player%", player.getName()).replace("%PLAYER%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    break;
                case EFFECT:
                    if (normalPunish.length < 2) throw new IllegalArgumentException("Not enough args");
                    String effect = normalPunish[1];
                    PotionEffectType potionEffect = PotionEffectType.getByName(effect.toUpperCase(Locale.ROOT));
                    if (potionEffect == null) throw new IllegalArgumentException("Unknown potion effect");
                    switch (normalPunish.length) {
                        case 2:
                            SchedulingUtils.runSyncIfFolia(player,() -> player.addPotionEffect(new PotionEffect(potionEffect, 10, 0)));
                            break;
                        case 3:
                            int duration_3 = Integer.parseInt(normalPunish[2]);
                            SchedulingUtils.runSyncIfFolia(player,() -> player.addPotionEffect(new PotionEffect(potionEffect, duration_3 * 20, 0)));
                            break;
                        case 4:
                        default:
                            int duration_4 = Integer.parseInt(normalPunish[2]);
                            int amplifier = Integer.parseInt(normalPunish[3]);
                            SchedulingUtils.runSyncIfFolia(player,() -> player.addPotionEffect(new PotionEffect(potionEffect, duration_4 * 20, amplifier)));
                            break;
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown punishment type");
            }
        }
    }

    /**
     * 使指定半径内的敌对生物对玩家表现出敌对行为。
     * @param target 目标玩家
     * @param radius 敌对生物的搜索半径
     */
    private static void makeHostileTowardsPlayer(Player target, double radius) {
        SchedulingUtils.runSyncIfFolia(target, () -> {
            List<Entity> entities = target.getNearbyEntities(radius, radius, radius);
            for (Entity entity : entities) {
                if (entity instanceof Mob && !entity.hasMetadata("NPC")) {
                    Mob mob = (Mob) entity;
                    mob.setTarget(target);
                }
            }
        });
    }
}