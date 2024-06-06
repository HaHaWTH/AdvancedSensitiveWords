package io.wdsj.asw.bukkit.proxy.bungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.event.EventType;
import org.bukkit.entity.Player;

import java.util.List;

public class BungeeSender {
    public static void send(Player violatedPlayer, EventType eventType, String originalMessage, List<String> censoredList) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(BungeeCordChannel.SUB_CHANNEL);
        out.writeUTF(violatedPlayer.getName());
        out.writeUTF(eventType.toString());
        out.writeUTF(originalMessage);
        out.writeUTF(censoredList.toString());
        byte[] data = out.toByteArray();
        violatedPlayer.sendPluginMessage(AdvancedSensitiveWords.getInstance(), BungeeCordChannel.BUNGEE_CHANNEL, data);
    }
}
