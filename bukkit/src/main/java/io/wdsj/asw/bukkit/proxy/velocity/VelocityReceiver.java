package io.wdsj.asw.bukkit.proxy.velocity;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import io.wdsj.asw.bukkit.manage.permission.Permissions;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.Collection;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.messagesManager;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class VelocityReceiver implements PluginMessageListener {
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (channel.equals(VelocityChannel.CHANNEL)) {
            ByteArrayDataInput input = ByteStreams.newDataInput(message);
            String playerName = input.readUTF();
            String eventType = input.readUTF();
            String originalMsg = input.readUTF();
            String serverName = input.readUTF();
            if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) {
                Collection<? extends Player> players = Bukkit.getOnlinePlayers();
                String msg = ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.ADMIN_REMINDER).replace("%player%", playerName + "(" + serverName + ")").replace("%type%", eventType).replace("%message%", originalMsg));
                for (Player iPlayer : players) {
                    if (iPlayer.hasPermission(Permissions.NOTICE)) {
                        iPlayer.sendMessage(msg);
                    }
                }
            }
        }
    }
}
