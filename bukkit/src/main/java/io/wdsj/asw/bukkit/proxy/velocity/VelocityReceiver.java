package io.wdsj.asw.bukkit.proxy.velocity;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import io.wdsj.asw.bukkit.manage.notice.Notifier;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class VelocityReceiver implements PluginMessageListener {
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (channel.equals(VelocityChannel.CHANNEL)) {
            ByteArrayDataInput input = ByteStreams.newDataInput(message);
            String playerName = input.readUTF();
            String eventType = input.readUTF();
            String originalMsg = input.readUTF();
            String censoredWordList = input.readUTF();
            String serverName = input.readUTF();
            if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) {
                Notifier.notice(playerName + "(" + serverName + ")", eventType, originalMsg, censoredWordList);
            }
        }
    }
}
