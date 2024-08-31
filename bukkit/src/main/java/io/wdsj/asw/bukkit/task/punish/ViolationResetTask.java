package io.wdsj.asw.bukkit.task.punish;

import com.github.Anon8281.universalScheduler.UniversalRunnable;
import io.wdsj.asw.bukkit.manage.notice.Notifier;
import io.wdsj.asw.bukkit.manage.punish.ViolationCounter;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import org.bukkit.ChatColor;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.messagesManager;

public class ViolationResetTask extends UniversalRunnable {
    @Override
    public void run() {
        ViolationCounter.resetAllViolations();
        String message = ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_VIOLATION_RESET));
        Notifier.normalNotice(message);
    }
}
