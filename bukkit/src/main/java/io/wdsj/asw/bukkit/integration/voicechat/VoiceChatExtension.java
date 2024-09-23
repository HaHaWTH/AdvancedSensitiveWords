package io.wdsj.asw.bukkit.integration.voicechat;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import io.wdsj.asw.bukkit.manage.punish.PlayerShadowController;
import org.bukkit.entity.Player;

public class VoiceChatExtension implements VoicechatPlugin {


    public VoiceChatExtension() {
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
    }

    /**
     * This method is called whenever a player sends audio to the server via the voice chat.
     *
     * @param event the microphone packet event
     */
    private void onMicrophone(MicrophonePacketEvent event) { // TODO: incomplete version, plans to add real-time voice transcribe
        if (event.getSenderConnection() == null) {
            return;
        }
        if (!(event.getSenderConnection().getPlayer().getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getSenderConnection().getPlayer().getPlayer();
        if (PlayerShadowController.isShadowed(player)) {
            event.cancel();
        }
    }
}
