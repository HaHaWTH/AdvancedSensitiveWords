package io.wdsj.asw.bukkit.integration.voicechat;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;

public class VoiceChatHookService {
    private final AdvancedSensitiveWords plugin;
    private VoiceChatExtension voiceChatExtension;
    public VoiceChatHookService(AdvancedSensitiveWords plugin) {
        this.plugin = plugin;
    }

    public void register() {
        BukkitVoicechatService service = plugin.getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            try {
                voiceChatExtension = new VoiceChatExtension();
                service.registerPlugin(voiceChatExtension);
                LOGGER.info("Successfully hooked into voicechat.");
            } catch (Exception e) {
                LOGGER.severe("Failed to register voicechat listener." +
                        " This should not happen, please report to the author" + e.getMessage());
            }
        } else {
            LOGGER.warning("Failed to hook into voicechat.");
        }
    }

    public void unregister() {
        if (voiceChatExtension != null) {
            plugin.getServer().getServicesManager().unregister(voiceChatExtension);
        }
    }
}
