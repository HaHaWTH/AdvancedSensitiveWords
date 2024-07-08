package io.wdsj.asw.bukkit.manage.notice;

import io.wdsj.asw.bukkit.type.ModuleType;
import io.wdsj.asw.bukkit.manage.permission.Permissions;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.getScheduler;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.messagesManager;

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
            String message = ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.ADMIN_REMINDER).replace("%player%", violatedPlayer.getName()).replace("%type%", moduleType.toString()).replace("%message%", originalMessage).replace("%censored_list%", censoredList.toString()));
            for (Player player : players) {
                if (player.hasPermission(Permissions.NOTICE)) {
                    player.sendMessage(message);
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
    public static void noticeFromProxy(String violatedPlayer, String serverName, String eventType, String originalMessage, String censoredList) {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        String message = ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.ADMIN_REMINDER_PROXY).replace("%player%", violatedPlayer).replace("%type%", eventType).replace("%message%", originalMessage).replace("%censored_list%", censoredList).replace("%server_name%", serverName));
        for (Player player : players) {
            if (player.hasPermission(Permissions.NOTICE)) {
                player.sendMessage(message);
            }
        }
    }
}
