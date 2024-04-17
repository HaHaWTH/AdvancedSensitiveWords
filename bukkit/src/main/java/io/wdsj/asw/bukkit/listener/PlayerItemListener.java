package io.wdsj.asw.bukkit.listener;

import io.wdsj.asw.bukkit.event.ASWFilterEvent;
import io.wdsj.asw.bukkit.event.EventType;
import io.wdsj.asw.bukkit.manage.notice.Notifier;
import io.wdsj.asw.bukkit.manage.punish.Punishment;
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
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.*;
import static io.wdsj.asw.bukkit.util.Utils.messagesFilteredNum;

public class PlayerItemListener implements Listener {

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
                long startTime = System.currentTimeMillis();
                if (settingsManager.getProperty(PluginSettings.IGNORE_FORMAT_CODE)) originalName = originalName.replaceAll(Utils.getIgnoreFormatCodeRegex(), "");
                List<String> censoredWordList = sensitiveWordBs.findAll(originalName);
                if (!censoredWordList.isEmpty()) {
                    messagesFilteredNum.getAndIncrement();
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
                    if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, EventType.ITEM, originalName);
                    if (settingsManager.getProperty(PluginSettings.ITEM_PUNISH)) Punishment.punish(player);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDrop(PlayerDropItemEvent event) {
        if (!isInitialized || !settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_ITEM_CHECK)) return;
        Player player = event.getPlayer();
        if (player.hasPermission("advancedsensitivewords.bypass")) return;
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String originalName = meta.getDisplayName();
                long startTime = System.currentTimeMillis();
                if (settingsManager.getProperty(PluginSettings.IGNORE_FORMAT_CODE)) originalName = originalName.replaceAll(Utils.getIgnoreFormatCodeRegex(), "");
                List<String> censoredWordList = sensitiveWordBs.findAll(originalName);
                if (!censoredWordList.isEmpty()) {
                    messagesFilteredNum.getAndIncrement();
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
                    if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                        VelocitySender.send(player, EventType.ITEM, originalName);
                    }
                    long endTime = System.currentTimeMillis();
                    TimingUtils.addProcessStatistic(endTime, startTime);
                    if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, EventType.ITEM, originalName);
                    if (settingsManager.getProperty(PluginSettings.ITEM_PUNISH)) Punishment.punish(player);
                }
            }
        }
    }
}
