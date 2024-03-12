package io.wdsj.asw.manage.notice;

import io.wdsj.asw.event.EventType;
import io.wdsj.asw.setting.PluginMessages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;

import static io.wdsj.asw.AdvancedSensitiveWords.messagesManager;

public class Notifier {
    public static void notice(Player violatedPlayer, EventType eventType, String originalMessage) {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        String message = ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.ADMIN_REMINDER).replace("%player%", violatedPlayer.getName()).replace("%type%", eventType.toString()).replace("%message%", originalMessage));
        for (Player player : players) {
            if (player.hasPermission("advancedsensitivewords.notice")) {
                player.sendMessage(message);
            }
        }
    }
}
