package io.wdsj.asw.manage.punish;

import io.wdsj.asw.setting.PluginSettings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.List;

import static io.wdsj.asw.AdvancedSensitiveWords.settingsManager;

public class Punishment {
    /**
     * 对玩家执行配置中定义的惩罚。
     * @param player 要惩罚的玩家
     */
    public static void punish(Player player) {
        List<String> punishList = settingsManager.getProperty(PluginSettings.PUNISHMENT);
        if (punishList.isEmpty()) return;
        for (String punish : punishList) {
            String upperCasePunish = punish.toUpperCase();
            String[] splitPunish = upperCasePunish.split("\\|");
            PunishmentType punishMethod = PunishmentType.valueOf(splitPunish[0]);
            switch (punishMethod) {
                case DAMAGE:
                    try {
                        double damageAmount = (splitPunish.length == 2) ? Double.parseDouble(splitPunish[1]) : 1.0D;
                        player.damage(damageAmount);
                    } catch (NumberFormatException e) {
                        player.damage(1.0D);
                    }
                    break;
                case HOSTILE:
                    try {
                        double radius = (splitPunish.length == 2) ? Double.parseDouble(splitPunish[1]) : 10D;
                        makeHostileTowardsPlayer(player, radius);
                    } catch (NumberFormatException e) {
                        makeHostileTowardsPlayer(player, 10D);
                    }
                    break;
                case COMMAND:
                    if (splitPunish.length != 2) throw new IllegalArgumentException("Not enough args");
                    String command = splitPunish[1].replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
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
        List<Entity> entities = target.getNearbyEntities(radius, radius, radius);
        for (Entity entity : entities) {
            if (entity instanceof Mob) {
                Mob mob = (Mob) entity;
                mob.setTarget(target);
            }
        }
    }
}