package io.wdsj.asw.listener;

import io.wdsj.asw.event.ASWFilterEvent;
import io.wdsj.asw.event.EventType;
import io.wdsj.asw.setting.PluginMessages;
import io.wdsj.asw.setting.PluginSettings;
import io.wdsj.asw.util.TimingUtils;
import io.wdsj.asw.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

import static io.wdsj.asw.AdvancedSensitiveWords.*;
import static io.wdsj.asw.util.Utils.getIgnoreFormatCodeRegex;

public class PlayerHeldItemListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerHeldItem(PlayerItemHeldEvent event) {
        if (!isInitialized || !settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_ITEM_CHECK)) return;
        Player player = event.getPlayer();
        if (player.hasPermission("advancedsensitivewords.bypass")) return;
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String originalName = meta.getDisplayName();
                if (settingsManager.getProperty(PluginSettings.IGNORE_FORMAT_CODE)) originalName = originalName.replaceAll(getIgnoreFormatCodeRegex(), "");
                long startTime = System.currentTimeMillis();
                List<String> censoredWordList = sensitiveWordBs.findAll(originalName);
                if (!censoredWordList.isEmpty()) {
                    String processedName = sensitiveWordBs.replace(originalName);
                    if (settingsManager.getProperty(PluginSettings.ITEM_METHOD).equalsIgnoreCase("cancel")) {
                        event.setCancelled(true);
                    } else {
                        meta.setDisplayName(processedName);
                        item.setItemMeta(meta);
                    }
                    if (settingsManager.getProperty(PluginSettings.ITEM_SEND_MESSAGE)) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_ITEM)));
                    }
                    if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                        Utils.logViolation(player.getName() + "(IP: " + Utils.getPlayerIp(player) + ")(Item)", originalName + censoredWordList);
                    }
                    if (settingsManager.getProperty(PluginSettings.ENABLE_API)) {
                        Bukkit.getPluginManager().callEvent(new ASWFilterEvent(player, originalName, processedName, censoredWordList, EventType.ITEM, false));
                    }
                    long endTime = System.currentTimeMillis();
                    TimingUtils.addProcessStatistic(endTime, startTime);
                }
            }
        }
    }
}
