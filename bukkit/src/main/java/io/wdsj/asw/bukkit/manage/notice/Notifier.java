package io.wdsj.asw.bukkit.manage.notice;

import io.wdsj.asw.bukkit.manage.permission.PermissionsEnum;
import io.wdsj.asw.bukkit.manage.permission.cache.CachingPermTool;
import io.wdsj.asw.bukkit.manage.punish.ViolationCounter;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.type.ModuleType;
import io.wdsj.asw.bukkit.util.message.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.getScheduler;

public class Notifier {
    /**
     * Notice the operators
     * @param violatedPlayer the player who violated the rules
     * @param moduleType the detection module type
     * @param originalMessage original message sent by the player
     * @param censoredList censored list
     */
    public static void notice(Player violatedPlayer, ModuleType moduleType, String originalMessage, List<String> censoredList) {
        getScheduler().runTask(() -> {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            String message = MessageUtils.retrieveMessage(PluginMessages.ADMIN_REMINDER).replace("%player%", violatedPlayer.getName()).replace("%type%", moduleType.toString()).replace("%message%", originalMessage).replace("%censored_list%", censoredList.toString()).replace("%violation%", String.valueOf(ViolationCounter.getViolationCount(violatedPlayer)));
            for (Player player : players) {
                if (CachingPermTool.hasPermission(PermissionsEnum.NOTICE, player)) {
                    MessageUtils.sendMessage(player, message);
                }
            }
        });
    }

    public static void normalNotice(String message) {
        getScheduler().runTask(() -> {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            for (Player player : players) {
                if (CachingPermTool.hasPermission(PermissionsEnum.NOTICE, player)) {
                    MessageUtils.sendMessage(player, message);
                }
            }
        });
    }

    /**
     * Notice Operator method used by the proxy receivers
     * @param violatedPlayer the player who violated the rules, with server name
     * @param eventType the type
     * @param originalMessage the original message sent by the player
     * @param censoredList censored list
     */
    public static void noticeFromProxy(String violatedPlayer, String serverName, String eventType, String violationCount, String originalMessage, String censoredList) {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        String message = MessageUtils.retrieveMessage(PluginMessages.ADMIN_REMINDER_PROXY).replace("%player%", violatedPlayer).replace("%type%", eventType).replace("%message%", originalMessage).replace("%censored_list%", censoredList).replace("%server_name%", serverName).replace("%violation%", violationCount);
        for (Player player : players) {
            if (CachingPermTool.hasPermission(PermissionsEnum.NOTICE, player)) {
                player.sendMessage(message);
            }
        }
    }
}
