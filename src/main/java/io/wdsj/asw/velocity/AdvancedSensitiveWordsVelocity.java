package io.wdsj.asw.velocity;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import io.wdsj.asw.velocity.template.PomData;
import org.slf4j.Logger;

import java.util.Optional;

@Plugin(
        id = "advancedsensitivewords",
        name = "AdvancedSensitiveWordsVelocity",
        version = PomData.VERSION,
        authors = {"HaHaWTH"}
)
public class AdvancedSensitiveWordsVelocity {

    @Inject
    private Logger logger;
    @Inject
    private ProxyServer server;
    private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("asw", "main");
    private static final ChannelIdentifier LEGACY_CHANNEL
            = new LegacyChannelIdentifier("asw:main");
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getChannelRegistrar().register(CHANNEL, LEGACY_CHANNEL);
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getIdentifier().equals(CHANNEL)) {
            if (!(event.getSource() instanceof ServerConnection)) return;
            Optional<ServerConnection> conn = ((ServerConnection) event.getSource()).getPlayer().getCurrentServer();
            byte[] message = event.getData();
            server.getAllServers().forEach(server -> {
                conn.ifPresent(source -> {
                    if (!server.getServerInfo().equals(source.getServerInfo()) && server.getPlayersConnected().size() > 0) {
                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.write(message);
                        out.writeUTF(source.getServerInfo().getName());
                        server.sendPluginMessage(CHANNEL, out.toByteArray());
                        logger.debug("Send message to " + server.getServerInfo().getName());
                    }
                });
            });
            return;
        }
        if (event.getIdentifier().equals(LEGACY_CHANNEL)) {
            if (!(event.getSource() instanceof ServerConnection)) return;
            Optional<ServerConnection> conn = ((ServerConnection) event.getSource()).getPlayer().getCurrentServer();
            byte[] message = event.getData();
            server.getAllServers().forEach(server -> {
                conn.ifPresent(source -> {
                    if (!server.getServerInfo().equals(source.getServerInfo()) && server.getPlayersConnected().size() > 0) {
                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.write(message);
                        out.writeUTF(source.getServerInfo().getName());
                        server.sendPluginMessage(LEGACY_CHANNEL, out.toByteArray());
                        logger.debug("Send message to " + server.getServerInfo().getName());
                    }
                });
            });
        }
    }
}
