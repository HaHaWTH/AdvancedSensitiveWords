package io.wdsj.asw.manage.punish;

import io.wdsj.asw.setting.PluginSettings;
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
        for (String punish : punishList) {
            String upperCasePunish = punish.toUpperCase();
            if (upperCasePunish.startsWith("DAMAGE")) {
                String[] damage = punish.split("\\|");
                try {
                    double damageAmount = (damage.length == 2) ? Double.parseDouble(damage[1]) : 1.0D;
                    player.damage(damageAmount);
                } catch (NumberFormatException e) {
                    player.damage(1.0D);
                }
            } else if (upperCasePunish.startsWith("HOSTILE")) {
                String[] hostile = upperCasePunish.split("\\|");
                try {
                    double radius = (hostile.length == 2) ? Double.parseDouble(hostile[1]) : 10D;
                    makeHostileTowardsPlayer(player, radius);
                } catch (NumberFormatException e) {
                    makeHostileTowardsPlayer(player, 10D);
                }
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