package io.wdsj.asw.bungee.listener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.wdsj.asw.bungee.AdvancedSensitiveWords;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import static io.wdsj.asw.bungee.AdvancedSensitiveWords.BUNGEE_CHANNEL;
import static io.wdsj.asw.bungee.AdvancedSensitiveWords.SUB_CHANNEL;

public class PluginMessageListener implements Listener {

    @EventHandler
    public void onPluginMessage(final PluginMessageEvent event) {
        if (!event.getTag().equals(BUNGEE_CHANNEL)) return;
        if (!(event.getSender() instanceof Server)) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        if (in.readUTF().equals(SUB_CHANNEL)) {
            try {
                String playerName = in.readUTF();
                String eventType = in.readUTF();
                String originalMessage = in.readUTF();
                String serverName = ((Server) event.getSender()).getInfo().getName();
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF(SUB_CHANNEL);
                out.writeUTF(playerName);
                out.writeUTF(eventType);
                out.writeUTF(originalMessage);
                out.writeUTF(serverName);
                AdvancedSensitiveWords.getInstance().getProxy().getServers().forEach((name, server) -> {
                    if (!server.equals(((Server) event.getSender()).getInfo()) && !server.getPlayers().isEmpty()) {
                        server.sendData(BUNGEE_CHANNEL, out.toByteArray());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
