package io.wdsj.asw.listener;

import io.wdsj.asw.event.ASWFilterEvent;
import io.wdsj.asw.event.EventType;
import io.wdsj.asw.setting.PluginMessages;
import io.wdsj.asw.setting.PluginSettings;
import io.wdsj.asw.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

import static io.wdsj.asw.AdvancedSensitiveWords.*;
import static io.wdsj.asw.AdvancedSensitiveWords.messagesManager;
import static io.wdsj.asw.util.TimingUtils.addProcessStatistic;
import static io.wdsj.asw.util.Utils.*;

/**
 * Another alternative choice if all packet-based listeners are not working.
 * Context check is not available when using this listener.
 * @author HaHaWTH
 * @version ImagineBreaker
 */
public class EventChatListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!isInitialized || isPacketBased()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("advancedsensitivewords.bypass")) return;
        if (isAuthMeAvailable && settingsManager.getProperty(PluginSettings.ENABLE_AUTHME_COMPATIBILITY)) {
            if (!fr.xephi.authme.api.v3.AuthMeApi.getInstance().isAuthenticated(player)) return;
        }
        if (isCslAvailable && settingsManager.getProperty(PluginSettings.ENABLE_CSL_COMPATIBILITY)) {
            if (!cc.baka9.catseedlogin.bukkit.CatSeedLoginAPI.isLogin(player.getName()) || !cc.baka9.catseedlogin.bukkit.CatSeedLoginAPI.isRegister(player.getName())) return;
        }
        String originalMessage = event.getMessage();
        long startTime = System.currentTimeMillis();
        // Chat check
        List<String> censoredWordList = sensitiveWordBs.findAll(originalMessage);
        if (!censoredWordList.isEmpty()) {
            messagesFilteredNum.getAndIncrement();
            String processedMessage = sensitiveWordBs.replace(originalMessage);
            if (settingsManager.getProperty(PluginSettings.CHAT_METHOD).equalsIgnoreCase("cancel")) {
                event.setCancelled(true);
                if (settingsManager.getProperty(PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL) && isNotCommand(originalMessage)) {
                    String fakeMessage = Bukkit.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI") ? me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, messagesManager.getProperty(PluginMessages.CHAT_FAKE_MESSAGE)).replace("%integrated_player%", player.getName()).replace("%integrated_message%", originalMessage) : messagesManager.getProperty(PluginMessages.CHAT_FAKE_MESSAGE).replace("%integrated_player%", player.getName()).replace("%integrated_message%", originalMessage);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', fakeMessage));
                } else {
                    event.setMessage(processedMessage);
                }
                if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", player.getName()).replace("%integrated_message%", originalMessage)));
                }
                if (settingsManager.getProperty(PluginSettings.ENABLE_API)) {
                    getScheduler().runTask(() -> Bukkit.getPluginManager().callEvent(new ASWFilterEvent(player, originalMessage, processedMessage, censoredWordList, EventType.CHAT, false)));
                }
                if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                    Utils.logViolation(player.getName() + "(IP: " + getPlayerIp(player) + ")(Chat)", originalMessage + censoredWordList);
                }
                long endTime = System.currentTimeMillis();
                addProcessStatistic(endTime, startTime);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!isInitialized || isPacketBased()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("advancedsensitivewords.bypass")) return;
        if (isAuthMeAvailable && settingsManager.getProperty(PluginSettings.ENABLE_AUTHME_COMPATIBILITY)) {
            if (!fr.xephi.authme.api.v3.AuthMeApi.getInstance().isAuthenticated(player)) return;
        }
        if (isCslAvailable && settingsManager.getProperty(PluginSettings.ENABLE_CSL_COMPATIBILITY)) {
            if (!cc.baka9.catseedlogin.bukkit.CatSeedLoginAPI.isLogin(player.getName()) || !cc.baka9.catseedlogin.bukkit.CatSeedLoginAPI.isRegister(player.getName())) return;
        }
        String originalCommand = event.getMessage();
        if (isCommandAndWhiteListed(originalCommand)) return;
        long startTime = System.currentTimeMillis();
        // Command check
        List<String> censoredWordList = sensitiveWordBs.findAll(originalCommand);
        if (!censoredWordList.isEmpty()) {
            messagesFilteredNum.getAndIncrement();
            String processedCommand = sensitiveWordBs.replace(originalCommand);
            if (isNotCommand(processedCommand)) processedCommand = "/" + processedCommand;
            if (settingsManager.getProperty(PluginSettings.CHAT_METHOD).equalsIgnoreCase("cancel")) {
                event.setCancelled(true);
            } else {
                event.setMessage(processedCommand);
            }
            if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", player.getName()).replace("%integrated_message%", originalCommand)));
            }
            if (settingsManager.getProperty(PluginSettings.ENABLE_API)) {
                Bukkit.getPluginManager().callEvent(new ASWFilterEvent(player, originalCommand, processedCommand, censoredWordList, EventType.CHAT, false));
            }
            if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                Utils.logViolation(player.getName() + "(IP: " + getPlayerIp(player) + ")(Chat)", "/" + originalCommand + censoredWordList);
            }
            long endTime = System.currentTimeMillis();
            addProcessStatistic(endTime, startTime);
        }
    }
}
