package io.wdsj.asw.bukkit.task.punish;

import com.github.Anon8281.universalScheduler.UniversalRunnable;
import io.wdsj.asw.bukkit.manage.notice.Notifier;
import io.wdsj.asw.bukkit.manage.punish.ViolationCounter;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.messagesManager;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class ViolationResetTask extends UniversalRunnable {
    @Override
    public void run() {
        if (settingsManager.getProperty(PluginSettings.ONLY_RESET_ONLINE_PLAYERS)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ViolationCounter.resetViolationCount(player);
            }
        } else {
            ViolationCounter.resetAllViolations();
        }
        String message = ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_VIOLATION_RESET));
        Notifier.normalNotice(message);
    }
}
