package io.wdsj.asw.bukkit.proxy.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.Collection;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.messagesManager;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class BungeeReceiver implements PluginMessageListener {
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (channel.equals(BungeeCordChannel.BUNGEE_CHANNEL)) {
            ByteArrayDataInput input = ByteStreams.newDataInput(message);
            String subChannel = input.readUTF();
            if (subChannel.equals(BungeeCordChannel.SUB_CHANNEL)) {
                String playerName = input.readUTF();
                String eventType = input.readUTF();
                String originalMessage = input.readUTF();
                String serverName = input.readUTF();
                if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) {
                    Collection<? extends Player> players = Bukkit.getOnlinePlayers();
                    String msg = ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.ADMIN_REMINDER).replace("%player%", playerName + "(" + serverName + ")").replace("%type%", eventType).replace("%message%", originalMessage));
                    for (Player iPlayer : players) {
                        if (iPlayer.hasPermission("advancedsensitivewords.notice")) {
                            iPlayer.sendMessage(msg);
                        }
                    }
                }
            }
        }
    }
}
