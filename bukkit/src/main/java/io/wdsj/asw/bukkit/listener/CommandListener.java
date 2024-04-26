package io.wdsj.asw.bukkit.listener;

import io.wdsj.asw.bukkit.event.ASWFilterEvent;
import io.wdsj.asw.bukkit.event.EventType;
import io.wdsj.asw.bukkit.manage.notice.Notifier;
import io.wdsj.asw.bukkit.manage.punish.Punishment;
import io.wdsj.asw.bukkit.proxy.bungee.BungeeSender;
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.util.TimingUtils;
import io.wdsj.asw.bukkit.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.*;
import static io.wdsj.asw.bukkit.util.Utils.*;

@SuppressWarnings("unused")
public class CommandListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String originalCommand = settingsManager.getProperty(PluginSettings.IGNORE_FORMAT_CODE) ? event.getMessage().replaceAll(Utils.getIgnoreFormatCodeRegex(), "") : event.getMessage();
        if (shouldNotProcess(player, originalCommand)) return;
        List<String> censoredWordList = sensitiveWordBs.findAll(originalCommand);
        long startTime = System.currentTimeMillis();
        if (!censoredWordList.isEmpty()) {
            messagesFilteredNum.getAndIncrement();
            String processedCommand = sensitiveWordBs.replace(originalCommand);
            if (settingsManager.getProperty(PluginSettings.CHAT_METHOD).equalsIgnoreCase("cancel")) {
                event.setCancelled(true);
            } else {
                if (Utils.isCommand(processedCommand)) {
                    event.setMessage(processedCommand);
                } else {
                    event.setMessage("/" + processedCommand);
                }
            }
            if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", player.getName()).replace("%integrated_message%", originalCommand)));
            }
            if (settingsManager.getProperty(PluginSettings.ENABLE_API)) {
                getScheduler().runTask(() -> Bukkit.getPluginManager().callEvent(new ASWFilterEvent(player, originalCommand, processedCommand, censoredWordList, EventType.CHAT, false)));
            }
            if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                Utils.logViolation(player.getName() + "(IP: " + getPlayerIp(player) + ")(Chat)", originalCommand + censoredWordList);
            }
            if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                VelocitySender.send(player, EventType.CHAT, originalCommand);
            }
            if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                BungeeSender.send(player, EventType.CHAT, originalCommand);
            }
            if (settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                databaseManager.checkAndUpdatePlayer(player.getName());
            }
            long endTime = System.currentTimeMillis();
            TimingUtils.addProcessStatistic(endTime, startTime);
            if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, EventType.CHAT, originalCommand);
            if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) Punishment.punish(player);

        }

    }

    private boolean shouldNotProcess(Player player, String message) {
        if (isInitialized && !player.hasPermission("advancedsensitivewords.bypass") && !isCommandAndWhiteListed(message)) {
            if (isAuthMeAvailable && settingsManager.getProperty(PluginSettings.ENABLE_AUTHME_COMPATIBILITY)) {
                if (!fr.xephi.authme.api.v3.AuthMeApi.getInstance().isAuthenticated(player)) return true;
            }
            if (isCslAvailable && settingsManager.getProperty(PluginSettings.ENABLE_CSL_COMPATIBILITY)) {
                return !cc.baka9.catseedlogin.bukkit.CatSeedLoginAPI.isLogin(player.getName()) || !cc.baka9.catseedlogin.bukkit.CatSeedLoginAPI.isRegister(player.getName());
            }
            return false;
        }
        return true;
    }

}
