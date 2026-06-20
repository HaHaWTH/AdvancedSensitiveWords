package io.wdsj.asw.bukkit.proxy.velocity;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.manage.notice.Notifier;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.common.constant.networking.ChannelDataConstant;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.setting;

public class VelocityReceiver implements PluginMessageListener {
    private boolean warned = false;

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!setting(PluginSettings.HOOK_VELOCITY)) return;
        if (channel.equals(VelocityChannel.CHANNEL)) {
            ByteArrayDataInput input = ByteStreams.newDataInput(message);
            if (!input.readUTF().equals(AdvancedSensitiveWords.PLUGIN_VERSION) && !warned) {
                LOGGER.warn("Plugin version mismatch! Things may not work properly.");
                warned = true;
            }
            switch (input.readUTF().toLowerCase(Locale.ROOT)) {
                case ChannelDataConstant.NOTICE:
                    String playerName = input.readUTF();
                    String moduleType = input.readUTF();
                    String violationCount = input.readUTF();
                    String originalMsg = input.readUTF();
                    String censoredWordList = input.readUTF();
                    String serverName = input.readUTF();
                    if (setting(PluginSettings.NOTICE_OPERATOR)) {
                        Notifier.noticeFromProxy(playerName, serverName, moduleType, violationCount, originalMsg, censoredWordList);
                    }
                    break;
                case ChannelDataConstant.AI_OBSERVATION:
                    String observedPlayer = input.readUTF();
                    String category = input.readUTF();
                    String confidence = input.readUTF();
                    String observedMessage = input.readUTF();
                    String observationServer = input.readUTF();
                    if (setting(PluginSettings.NOTICE_OPERATOR)) {
                        Notifier.noticeAiObservationFromProxy(
                                observedPlayer,
                                observationServer,
                                observedMessage,
                                category,
                                confidence
                        );
                    }
                    break;
            }
        }
    }
}
