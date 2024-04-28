package io.wdsj.asw.bukkit.manage.notice;

import io.wdsj.asw.bukkit.event.EventType;
import io.wdsj.asw.bukkit.manage.permission.Permissions;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.messagesManager;

public class Notifier {
    public static void notice(Player violatedPlayer, EventType eventType, String originalMessage) {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        String message = ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.ADMIN_REMINDER).replace("%player%", violatedPlayer.getName()).replace("%type%", eventType.toString()).replace("%message%", originalMessage));
        for (Player player : players) {
            if (player.hasPermission(Permissions.NOTICE)) {
                player.sendMessage(message);
            }
        }
    }
}
