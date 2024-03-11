package io.wdsj.asw.listener;

import io.wdsj.asw.AdvancedSensitiveWords;
import io.wdsj.asw.event.ASWFilterEvent;
import io.wdsj.asw.event.EventType;
import io.wdsj.asw.manage.notice.Notifier;
import io.wdsj.asw.manage.punish.Punishment;
import io.wdsj.asw.setting.PluginMessages;
import io.wdsj.asw.setting.PluginSettings;
import io.wdsj.asw.util.PlayerUtils;
import io.wdsj.asw.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.List;

import static io.wdsj.asw.AdvancedSensitiveWords.*;
import static io.wdsj.asw.util.TimingUtils.addProcessStatistic;
import static io.wdsj.asw.util.Utils.messagesFilteredNum;

public class PlayerLoginListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent event) {
        if (!isInitialized || !settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_NAME_CHECK)) return;
        Player player = event.getPlayer();
        if (player.hasPermission("advancedsensitivewords.bypass")) return;
        if (PlayerUtils.isNpc(player) && settingsManager.getProperty(PluginSettings.NAME_IGNORE_NPC)) return;
        if (Bukkit.getPluginManager().getPlugin("floodgate") != null && settingsManager.getProperty(PluginSettings.NAME_IGNORE_BEDROCK)) {
            if (org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) return;
        }
        String playerName = player.getName();
        long startTime = System.currentTimeMillis();
        List<String> censoredWordList = sensitiveWordBs.findAll(playerName);
        if (!censoredWordList.isEmpty()) {
            String processedPlayerName = sensitiveWordBs.replace(playerName);
            String playerIp = event.getAddress().getHostAddress();
            messagesFilteredNum.getAndIncrement();
            if (settingsManager.getProperty(PluginSettings.NAME_METHOD).equalsIgnoreCase("replace")) {
                player.setDisplayName(processedPlayerName);
                player.setPlayerListName(processedPlayerName);
                if (settingsManager.getProperty(PluginSettings.NAME_SEND_MESSAGE)) {
                    AdvancedSensitiveWords.getScheduler().runTaskLater(() -> player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_NAME))), 60L);
                }
            } else {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_NAME)));
            }
            if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                Utils.logViolation(player.getName() + "(IP: " + playerIp + ")(Name)", playerName + censoredWordList);
            }
            if (settingsManager.getProperty(PluginSettings.ENABLE_API)) {
                Bukkit.getPluginManager().callEvent(new ASWFilterEvent(player, playerName, processedPlayerName, censoredWordList, EventType.NAME, false));
            }
            long endTime = System.currentTimeMillis();
            addProcessStatistic(endTime, startTime);
            Notifier.notice(player, EventType.NAME, playerName);
            Punishment.punish(player);
        }
    }
}
