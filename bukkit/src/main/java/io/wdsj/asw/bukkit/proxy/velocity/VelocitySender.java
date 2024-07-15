package io.wdsj.asw.bukkit.proxy.velocity;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.type.ModuleType;
import org.bukkit.entity.Player;

import java.util.List;

public class VelocitySender {
    public static void send(Player violatedPlayer, ModuleType moduleType, String originalMessage, List<String> censoredList) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(violatedPlayer.getName());
        out.writeUTF(moduleType.toString());
        out.writeUTF(originalMessage);
        out.writeUTF(censoredList.toString());
        byte[] data = out.toByteArray();
        violatedPlayer.sendPluginMessage(AdvancedSensitiveWords.getInstance(), VelocityChannel.CHANNEL, data);
    }
}
