package io.wdsj.asw.bukkit.proxy.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.manage.notice.Notifier;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.common.constant.ChannelDataConstant;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class BungeeReceiver implements PluginMessageListener {
    @Override
    public void onPluginMessageReceived(String channel, @NotNull Player player, @NotNull byte[] message) {
        if (channel.equals(BungeeCordChannel.BUNGEE_CHANNEL)) {
            ByteArrayDataInput input = ByteStreams.newDataInput(message);
            String subChannel = input.readUTF();
            if (subChannel.equals(BungeeCordChannel.SUB_CHANNEL)) {
                if (!input.readUTF().equals(AdvancedSensitiveWords.PLUGIN_VERSION)) {
                    LOGGER.warning("Plugin version mismatch! Things may not work properly.");
                }
                switch (input.readUTF().toLowerCase(Locale.ROOT)) { // Use switch for future updates
                    case ChannelDataConstant.NOTICE:
                        String playerName = input.readUTF();
                        String moduleType = input.readUTF();
                        String originalMessage = input.readUTF();
                        String censoredWordList = input.readUTF();
                        String serverName = input.readUTF();
                        if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) {
                            Notifier.noticeFromProxy(playerName, serverName, moduleType, originalMessage, censoredWordList);
                        }
                        break;
                }
            }
        }
    }
}
