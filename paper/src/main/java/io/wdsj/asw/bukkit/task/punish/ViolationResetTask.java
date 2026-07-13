package io.wdsj.asw.bukkit.task.punish;

import com.github.Anon8281.universalScheduler.UniversalRunnable;
import io.wdsj.asw.bukkit.manage.notice.Notifier;
import io.wdsj.asw.bukkit.manage.punish.ViolationCounter;
import io.wdsj.asw.bukkit.setting.PaperConfigurationService;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.util.message.MessageUtils;

/**
 * Asynchronous task to reset the violation count of players.
 */
public class ViolationResetTask extends UniversalRunnable {
    private final PaperConfigurationService configuration;

    public ViolationResetTask(PaperConfigurationService configuration) {
        this.configuration = configuration;
    }

    @Override
    public void run() {
        if (configuration.get(PluginSettings.VELOCITY_SYNC_ENABLED)) {
            return;
        }
        ViolationCounter.INSTANCE.resetAllViolations();
        String message = MessageUtils.retrieveMessage(PluginMessages.MESSAGE_ON_VIOLATION_RESET);
        Notifier.normalNotice(message);
    }
}
