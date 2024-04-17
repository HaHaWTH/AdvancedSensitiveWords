package io.wdsj.asw.bukkit.listener;

import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.event.ASWFilterEvent;
import io.wdsj.asw.bukkit.event.EventType;
import io.wdsj.asw.bukkit.manage.notice.Notifier;
import io.wdsj.asw.bukkit.manage.punish.Punishment;
import io.wdsj.asw.bukkit.proxy.bungee.BungeeSender;
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.*;
import static io.wdsj.asw.bukkit.util.TimingUtils.addProcessStatistic;
import static io.wdsj.asw.bukkit.util.Utils.getIgnoreFormatCodeRegex;
import static io.wdsj.asw.bukkit.util.Utils.messagesFilteredNum;

public class AnvilListener implements Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onAnvil(InventoryClickEvent event) {
        if (!isInitialized) return;
        if (!settingsManager.getProperty(PluginSettings.ENABLE_ANVIL_EDIT_CHECK)) return;
        if (event.getInventory().getType() == InventoryType.ANVIL) {
            Player player = (Player) event.getWhoClicked();
            if (player.hasPermission("advancedsensitivewords.bypass")) {
                return;
            }
            if (event.getRawSlot() == 2) {
                ItemStack outputItem = event.getCurrentItem();
                if (outputItem != null && outputItem.hasItemMeta()) {
                    ItemMeta itemMeta = outputItem.getItemMeta();
                    if (itemMeta != null && itemMeta.hasDisplayName()) {
                        String originalItemName = itemMeta.getDisplayName();
                        if (settingsManager.getProperty(PluginSettings.IGNORE_FORMAT_CODE)) originalItemName = originalItemName.replaceAll(getIgnoreFormatCodeRegex(), "");
                        List<String> censoredWords = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalItemName);
                        if (!censoredWords.isEmpty()) {
                            long startTime = System.currentTimeMillis();
                            messagesFilteredNum.getAndIncrement();
                            String processedItemName = AdvancedSensitiveWords.sensitiveWordBs.replace(originalItemName);
                            if (settingsManager.getProperty(PluginSettings.ANVIL_METHOD).equalsIgnoreCase("cancel")) {
                                event.setCancelled(true);
                            } else {
                                itemMeta.setDisplayName(processedItemName);
                                outputItem.setItemMeta(itemMeta);
                            }

                            if (settingsManager.getProperty(PluginSettings.ANVIL_SEND_MESSAGE)) {
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_ANVIL_RENAME)));
                            }

                            if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                                Utils.logViolation(player.getName() + "(IP: " + Utils.getPlayerIp(player) + ")(Anvil)", originalItemName + censoredWords);
                            }

                            if (settingsManager.getProperty(PluginSettings.ENABLE_API)) {
                                Bukkit.getPluginManager().callEvent(new ASWFilterEvent(player, originalItemName, processedItemName, censoredWords, EventType.ANVIL, false));
                            }

                            if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                                VelocitySender.send(player, EventType.ANVIL, originalItemName);
                            }

                            if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                                BungeeSender.send(player, EventType.ANVIL, originalItemName);
                            }

                            long endTime = System.currentTimeMillis();
                            addProcessStatistic(endTime, startTime);
                            if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, EventType.ANVIL, originalItemName);
                            if (settingsManager.getProperty(PluginSettings.ANVIL_PUNISH)) Punishment.punish(player);
                        }
                    }
                }
            }
        }
    }
}
