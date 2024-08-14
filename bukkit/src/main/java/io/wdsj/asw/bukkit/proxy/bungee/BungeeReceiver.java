package io.wdsj.asw.bukkit.proxy.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import io.wdsj.asw.bukkit.manage.notice.Notifier;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class BungeeReceiver implements PluginMessageListener {
    @Override
    public void onPluginMessageReceived(String channel, @NotNull Player player, @NotNull byte[] message) {
        if (channel.equals(BungeeCordChannel.BUNGEE_CHANNEL)) {
            ByteArrayDataInput input = ByteStreams.newDataInput(message);
            String subChannel = input.readUTF();
            if (subChannel.equals(BungeeCordChannel.SUB_CHANNEL)) {
                if (input.readUTF().equalsIgnoreCase(BungeeCordChannel.DataType.NOTICE)) {
                    String playerName = input.readUTF();
                    String moduleType = input.readUTF();
                    String originalMessage = input.readUTF();
                    String censoredWordList = input.readUTF();
                    String serverName = input.readUTF();
                    if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) {
                        Notifier.noticeFromProxy(playerName, serverName, moduleType, originalMessage, censoredWordList);
                    }
                }
            }
        }
    }
}
