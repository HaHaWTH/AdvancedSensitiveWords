package io.wdsj.asw.bukkit.proxy.velocity;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.event.EventType;
import org.bukkit.entity.Player;

public class VelocitySender {
    public static void send(Player violatedPlayer, EventType eventType, String originalMessage) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(violatedPlayer.getName());
        out.writeUTF(eventType.toString());
        out.writeUTF(originalMessage);
        byte[] data = out.toByteArray();
        violatedPlayer.sendPluginMessage(AdvancedSensitiveWords.getInstance(), VelocityChannel.CHANNEL, data);
    }
}
