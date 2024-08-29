package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.manage.notice.Notifier
import io.wdsj.asw.bukkit.manage.permission.Permissions
import io.wdsj.asw.bukkit.manage.punish.Punishment
import io.wdsj.asw.bukkit.proxy.bungee.BungeeSender
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.type.ModuleType
import io.wdsj.asw.bukkit.util.LoggingUtils
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType

class AnvilListener : Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onAnvil(event: InventoryClickEvent) {
        if (!AdvancedSensitiveWords.isInitialized) return
        if (!AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_ANVIL_EDIT_CHECK)) return
        if (event.inventory.type == InventoryType.ANVIL) {
            val player = event.whoClicked as Player
            if (player.hasPermission(Permissions.BYPASS)) {
                return
            }
            if (event.rawSlot == 2) {
                val outputItem = event.currentItem
                val isCancelMode = AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ANVIL_METHOD).equals("cancel", ignoreCase = true)
                if (outputItem != null && outputItem.hasItemMeta()) {
                    val itemMeta = outputItem.itemMeta
                    if (itemMeta != null && itemMeta.hasDisplayName()) {
                        var originalItemName = itemMeta.displayName
                        if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.PRE_PROCESS)) originalItemName =
                            originalItemName.replace(
                                Utils.getPreProcessRegex().toRegex(), ""
                            )
                        val censoredWords = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalItemName)
                        if (censoredWords.isNotEmpty()) {
                            val startTime = System.currentTimeMillis()
                            Utils.messagesFilteredNum.getAndIncrement()
                            val processedItemName = AdvancedSensitiveWords.sensitiveWordBs.replace(originalItemName)
                            if (isCancelMode) {
                                event.isCancelled = true
                            } else {
                                itemMeta.setDisplayName(processedItemName)
                                outputItem.setItemMeta(itemMeta)
                            }

                            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ANVIL_SEND_MESSAGE)) {
                                player.sendMessage(
                                    ChatColor.translateAlternateColorCodes(
                                        '&',
                                        AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_ANVIL_RENAME)
                                    )
                                )
                            }

                            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                                LoggingUtils.logViolation(
                                    player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Anvil)",
                                    originalItemName + censoredWords
                                )
                            }

                            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                                VelocitySender.sendNotifyMessage(player, ModuleType.ANVIL, originalItemName, censoredWords)
                            }

                            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                                BungeeSender.sendNotifyMessage(player, ModuleType.ANVIL, originalItemName, censoredWords)
                            }

                            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                                AdvancedSensitiveWords.databaseManager.checkAndUpdatePlayer(player.name)
                            }

                            val endTime = System.currentTimeMillis()
                            TimingUtils.addProcessStatistic(endTime, startTime)
                            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(
                                player,
                                ModuleType.ANVIL,
                                originalItemName,
                                censoredWords
                            )
                            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ANVIL_PUNISH)) Punishment.punish(
                                player
                            )
                        }
                    }
                }
            }
        }
    }
}
