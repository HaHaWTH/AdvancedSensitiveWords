package io.wdsj.asw.bungee.listener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import io.wdsj.asw.bungee.AdvancedSensitiveWords;
import io.wdsj.asw.common.constant.networking.ChannelDataConstant;
import io.wdsj.asw.common.datatype.io.LimitedByteArrayDataOutput;
import io.wdsj.asw.common.template.PluginVersionTemplate;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Locale;

import static io.wdsj.asw.bungee.AdvancedSensitiveWords.*;

public class PluginMessageListener implements Listener {

    @EventHandler
    public void onPluginMessage(final PluginMessageEvent event) {
        if (!event.getTag().equals(BUNGEE_CHANNEL)) return;
        if (!(event.getSender() instanceof Server)) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        if (in.readUTF().equals(SUB_CHANNEL)) {
            if (!in.readUTF().equals(PluginVersionTemplate.VERSION)) {
                LOGGER.warning("Plugin version mismatch! Things may not work properly.");
            }
            switch (in.readUTF().toLowerCase(Locale.ROOT)) {
                case ChannelDataConstant.NOTICE:
                    try {
                        String serverName = ((Server) event.getSender()).getInfo().getName();
                        LimitedByteArrayDataOutput out = LimitedByteArrayDataOutput.newDataOutput(32767);
                        out.write(event.getData());
                        out.writeUTF(serverName);
                        AdvancedSensitiveWords.getInstance().getProxy().getServers().forEach((name, server) -> {
                            if (!server.equals(((Server) event.getSender()).getInfo()) && !server.getPlayers().isEmpty()) {
                                server.sendData(BUNGEE_CHANNEL, out.toByteArray());
                            }
                        });
                        event.setCancelled(true);
                    } catch (Exception e) {
                        LOGGER.severe("An error occurred while sending plugin message " + e.getMessage());
                    }
                    break;
                case ChannelDataConstant.COMMAND_PROXY:
                    String command = in.readUTF();
                    AdvancedSensitiveWords.getInstance().getProxy().getPluginManager().dispatchCommand(AdvancedSensitiveWords.getInstance().getProxy().getConsole(), command);
                    event.setCancelled(true);
                    break;
            }
        }
    }
}
