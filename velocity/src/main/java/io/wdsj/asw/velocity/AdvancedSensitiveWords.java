package io.wdsj.asw.velocity;

import com.google.common.io.ByteArrayDataInput;
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
import com.velocitypowered.api.proxy.server.ServerInfo;
import io.wdsj.asw.common.constant.networking.ChannelDataConstant;
import io.wdsj.asw.common.datatype.io.LimitedByteArrayDataOutput;
import io.wdsj.asw.common.template.PluginVersionTemplate;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.util.Locale;

@Plugin(
        id = "advancedsensitivewords",
        name = "AdvancedSensitiveWords",
        version = PluginVersionTemplate.VERSION + "-" + PluginVersionTemplate.VERSION_CHANNEL,
        authors = {"HaHaWTH"}
)
public class AdvancedSensitiveWords {

    private final Logger logger;
    private final ProxyServer server;
    private final Metrics.Factory metricsFactory;
    private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("asw", "main");
    private static final ChannelIdentifier LEGACY_CHANNEL
            = new LegacyChannelIdentifier("asw:main");
    @Inject
    public AdvancedSensitiveWords(Logger logger, ProxyServer server, Metrics.Factory metricsFactory) {
        this.logger = logger;
        this.server = server;
        this.metricsFactory = metricsFactory;
    }
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getChannelRegistrar().register(CHANNEL, LEGACY_CHANNEL);
        Metrics metrics = metricsFactory.make(this, 21637);
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getIdentifier().equals(CHANNEL) || event.getIdentifier().equals(LEGACY_CHANNEL)) {
            if (!(event.getSource() instanceof ServerConnection)) return;
            ServerInfo serverInfo = ((ServerConnection) event.getSource()).getServerInfo();
            byte[] message = event.getData();
            ByteArrayDataInput input = ByteStreams.newDataInput(message);
            if (!input.readUTF().equals(PluginVersionTemplate.VERSION)) {
                logger.warn("Plugin version mismatch! Things may not work properly.");
            }
            switch (input.readUTF().toLowerCase(Locale.ROOT)) {
                case ChannelDataConstant.NOTICE:
                    server.getAllServers().forEach(server -> {
                        if (!server.getServerInfo().equals(serverInfo) && !server.getPlayersConnected().isEmpty()) {
                            LimitedByteArrayDataOutput out = LimitedByteArrayDataOutput.newDataOutput(32767);
                            try {
                                out.write(message);
                                out.writeUTF(serverInfo.getName());
                            } catch (Exception e) {
                                logger.error("Failed to write notice message: " + e.getMessage());
                            }
                            server.sendPluginMessage(CHANNEL, out.toByteArray());
                            logger.debug("Send notice message to " + server.getServerInfo().getName());
                        }
                    });
                    break;
                case ChannelDataConstant.COMMAND_PROXY:
                    String command = input.readUTF();
                    server.getCommandManager().executeAsync(server.getConsoleCommandSource(), command);
                    break;
            }
            event.setResult(PluginMessageEvent.ForwardResult.handled());
        }
    }
}
