package io.wdsj.asw.bukkit.service.hook;

import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.integration.voicechat.VoiceChatExtension;
import io.wdsj.asw.bukkit.integration.voicechat.WhisperVoiceTranscribeTool;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.task.voicechat.VoiceChatTranscribeTask;
import io.wdsj.asw.bukkit.util.SchedulingUtils;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class VoiceChatHookService {
    private final AdvancedSensitiveWords plugin;
    private VoiceChatExtension voiceChatExtension;
    private MyScheduledTask transcribeTask;
    private WhisperVoiceTranscribeTool transcribeTool;
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
                if (settingsManager.getProperty(PluginSettings.VOICE_REALTIME_TRANSCRIBING)) {
                    transcribeTool = new WhisperVoiceTranscribeTool();
                    long interval = settingsManager.getProperty(PluginSettings.VOICE_CHECK_INTERVAL);
                    transcribeTask = new VoiceChatTranscribeTask(voiceChatExtension, transcribeTool).runTaskTimerAsynchronously(plugin, interval * 20L, interval * 20L);
                }
            } catch (Exception e) {
                LOGGER.warning("Failed to register voicechat listener." +
                        " This should not happen, please report to the author: " + e.getMessage());
            }
        } else {
            LOGGER.warning("Failed to hook into voicechat.");
        }
    }

    public void unregister() {
        if (voiceChatExtension != null) {
            plugin.getServer().getServicesManager().unregister(voiceChatExtension);
        }
        SchedulingUtils.cancelTaskSafely(transcribeTask);
        if (transcribeTool != null) {
            transcribeTool.shutdown();
        }
    }
}
