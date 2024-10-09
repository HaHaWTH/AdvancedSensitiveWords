package io.wdsj.asw.bukkit.integration.voicechat;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.audio.AudioConverter;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerConnectedEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import io.wdsj.asw.bukkit.manage.punish.PlayerShadowController;
import io.wdsj.asw.bukkit.permission.PermissionsEnum;
import io.wdsj.asw.bukkit.permission.cache.CachingPermTool;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class VoiceChatExtension implements VoicechatPlugin {
    public final Map<UUID, float[]> connectedPlayers;


    public VoiceChatExtension() {
        connectedPlayers = new ConcurrentHashMap<>();
    }

    /**
     * @return the unique ID for this voice chat plugin
     */
    @Override
    public String getPluginId() {
        return "asw_voicechat";
    }

    /**
     * Called when the voice chat initializes the plugin.
     *
     * @param api the voice chat API
     */
    @Override
    public void initialize(VoicechatApi api) {

    }

    /**
     * Called once by the voice chat to register all events.
     *
     * @param registration the event registration
     */
    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
        registration.registerEvent(PlayerConnectedEvent.class, this::onConnect);
        registration.registerEvent(PlayerDisconnectedEvent.class, this::onDisconnect);
    }

    /**
     * This method is called whenever a player sends audio to the server via the voice chat.
     *
     * @param event the microphone packet event
     */
    private void onMicrophone(MicrophonePacketEvent event) {
        if (event.getSenderConnection() == null) {
            return;
        }
        if (!(event.getSenderConnection().getPlayer().getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getSenderConnection().getPlayer().getPlayer();
        if (PlayerShadowController.isShadowed(player) && settingsManager.getProperty(PluginSettings.VOICE_SYNC_SHADOW)) {
            event.cancel();
            return;
        }

        if (!settingsManager.getProperty(PluginSettings.VOICE_REALTIME_TRANSCRIBING)) return;
        if (event.getPacket() == null || CachingPermTool.hasPermission(PermissionsEnum.BYPASS, player)) return;
        OpusDecoder decoder = event.getVoicechat().createDecoder();
        AudioConverter converter = event.getVoicechat().getAudioConverter();
        float[] newData = converter.shortsToFloats(decoder.decode(event.getPacket().getOpusEncodedData()));
        if (connectedPlayers.get(player.getUniqueId()) != null) {
            float[] oldData = connectedPlayers.get(player.getUniqueId());
            float[] result = new float[oldData.length + newData.length];
            System.arraycopy(oldData, 0, result, 0, oldData.length);
            System.arraycopy(newData, 0, result, oldData.length, newData.length);
            connectedPlayers.put(player.getUniqueId(), result);
        } else {
            connectedPlayers.put(player.getUniqueId(), newData);
        }
        decoder.close();
    }

    private void onConnect(PlayerConnectedEvent event) {
        if (!settingsManager.getProperty(PluginSettings.VOICE_REALTIME_TRANSCRIBING)) return;
        if (event.getConnection() == null) {
            return;
        }
        if (!(event.getConnection().getPlayer().getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getConnection().getPlayer().getPlayer();
        connectedPlayers.put(player.getUniqueId(), new float[]{});
    }

    private void onDisconnect(PlayerDisconnectedEvent event) {
        if (!settingsManager.getProperty(PluginSettings.VOICE_REALTIME_TRANSCRIBING)) {
            if (!connectedPlayers.isEmpty()) connectedPlayers.clear();
            return;
        }
        if (event.getPlayerUuid() == null) {
            return;
        }
        connectedPlayers.remove(event.getPlayerUuid());
    }
}
