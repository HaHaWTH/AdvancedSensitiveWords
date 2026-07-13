package io.wdsj.asw.bukkit.task.punish;

import com.github.Anon8281.universalScheduler.UniversalRunnable;
import io.wdsj.asw.bukkit.manage.notice.Notifier;
import io.wdsj.asw.bukkit.manage.punish.ViolationCounter;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.util.message.MessageUtils;

/**
 * Asynchronous task to reset the violation count of players.
 */
public class ViolationResetTask extends UniversalRunnable {
    @Override
    public void run() {
        ViolationCounter.INSTANCE.resetAllViolations();
        String message = MessageUtils.retrieveMessage(PluginMessages.MESSAGE_ON_VIOLATION_RESET);
        Notifier.normalNotice(message);
    }
}
