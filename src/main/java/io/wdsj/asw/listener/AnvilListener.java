package io.wdsj.asw.listener;

import io.wdsj.asw.AdvancedSensitiveWords;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

import static io.wdsj.asw.AdvancedSensitiveWords.*;
import static io.wdsj.asw.util.TimingUtils.addProcessStatistic;
import static io.wdsj.asw.util.Utils.messagesFilteredNum;

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
                        if (settingsManager.getProperty(PluginSettings.IGNORE_FORMAT_CODE)) originalItemName = originalItemName.replaceAll(IGNORE_FORMAT_CODE_REGEX, "");
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
                            long endTime = System.currentTimeMillis();
                            addProcessStatistic(endTime, startTime);
                        }
                    }
                }
            }
        }
    }
}
