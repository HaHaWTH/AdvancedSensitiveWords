package io.wdsj.asw.bukkit.proxy.bungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.type.ModuleType;
import org.bukkit.entity.Player;

import java.util.List;

public class BungeeSender {
    public static void send(Player violatedPlayer, ModuleType moduleType, String originalMessage, List<String> censoredList) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(BungeeCordChannel.SUB_CHANNEL);
        out.writeUTF(BungeeCordChannel.DataType.NOTICE);
        out.writeUTF(violatedPlayer.getName());
        out.writeUTF(moduleType.toString());
        out.writeUTF(originalMessage);
        out.writeUTF(censoredList.toString());
        byte[] data = out.toByteArray();
        violatedPlayer.sendPluginMessage(AdvancedSensitiveWords.getInstance(), BungeeCordChannel.BUNGEE_CHANNEL, data);
    }

    public static void executeBungeeCommand(Player violatedPlayer, String command) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(BungeeCordChannel.SUB_CHANNEL);
        out.writeUTF(BungeeCordChannel.DataType.COMMAND_PROXY);
        out.writeUTF(command);
        byte[] data = out.toByteArray();
        violatedPlayer.sendPluginMessage(AdvancedSensitiveWords.getInstance(), BungeeCordChannel.BUNGEE_CHANNEL, data);
    }
}
