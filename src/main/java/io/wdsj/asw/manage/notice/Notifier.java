package io.wdsj.asw.manage.notice;

import io.wdsj.asw.event.EventType;
import io.wdsj.asw.setting.PluginMessages;
import io.wdsj.asw.setting.PluginSettings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collection;

import static io.wdsj.asw.AdvancedSensitiveWords.messagesManager;
import static io.wdsj.asw.AdvancedSensitiveWords.settingsManager;

public class Notifier {
    public static void notice(Player violatedPlayer, EventType eventType, String originalMessage) {
        if (!settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) return;
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        for (Player player : players) {
            if (player.hasPermission("advancedsensitivewords.notice")) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.ADMIN_REMINDER).replace("%player%", violatedPlayer.getName()).replace("%type%", eventType.toString()).replace("%message%", originalMessage)));
            }
        }
    }
}
