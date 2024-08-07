package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.type.ModuleType
import io.wdsj.asw.bukkit.manage.notice.Notifier
import io.wdsj.asw.bukkit.manage.permission.Permissions
import io.wdsj.asw.bukkit.manage.punish.Punishment
import io.wdsj.asw.bukkit.proxy.bungee.BungeeSender
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerItemHeldEvent

class PlayerItemListener : Listener {
    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerHeldItem(event: PlayerItemHeldEvent) {
        if (!AdvancedSensitiveWords.isInitialized || !AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_ITEM_CHECK)) return
        val player = event.player
        if (player.hasPermission(Permissions.BYPASS)) return
        val item = player.inventory.getItem(event.newSlot)
        if (item != null && item.hasItemMeta()) {
            val meta = item.itemMeta
            if (meta != null && meta.hasDisplayName()) {
                var originalName = meta.displayName
                val startTime = System.currentTimeMillis()
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.PRE_PROCESS)) originalName =
                    originalName.replace(
                        Utils.getPreProcessRegex().toRegex(), ""
                    )
                val censoredWordList = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalName)
                if (censoredWordList.isNotEmpty()) {
                    Utils.messagesFilteredNum.getAndIncrement()
                    val processedName = AdvancedSensitiveWords.sensitiveWordBs.replace(originalName)
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ITEM_METHOD)
                            .equals("cancel", ignoreCase = true)
                    ) {
                        event.isCancelled = true
                    } else {
                        meta.setDisplayName(processedName)
                        item.setItemMeta(meta)
                    }
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ITEM_SEND_MESSAGE)) {
                        player.sendMessage(
                            ChatColor.translateAlternateColorCodes(
                                '&',
                                AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_ITEM)
                            )
                        )
                    }
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                        Utils.logViolation(
                            player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Item)",
                            originalName + censoredWordList
                        )
                    }
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                        BungeeSender.send(player, ModuleType.ITEM, originalName, censoredWordList)
                    }
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                        VelocitySender.send(player, ModuleType.ITEM, originalName, censoredWordList)
                    }
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                        AdvancedSensitiveWords.databaseManager.checkAndUpdatePlayer(player.name)
                    }
                    val endTime = System.currentTimeMillis()
                    TimingUtils.addProcessStatistic(endTime, startTime)
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(
                        player,
                        ModuleType.ITEM,
                        originalName,
                        censoredWordList
                    )
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ITEM_PUNISH)) Punishment.punish(
                        player
                    )
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onDrop(event: PlayerDropItemEvent) {
        if (!AdvancedSensitiveWords.isInitialized || !AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_ITEM_CHECK)) return
        val player = event.player
        if (player.hasPermission(Permissions.BYPASS)) return
        val item = event.itemDrop.itemStack
        if (item.hasItemMeta()) {
            val meta = item.itemMeta
            if (meta != null && meta.hasDisplayName()) {
                var originalName = meta.displayName
                val startTime = System.currentTimeMillis()
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.PRE_PROCESS)) originalName =
                    originalName.replace(
                        Utils.getPreProcessRegex().toRegex(), ""
                    )
                val censoredWordList = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalName)
                if (censoredWordList.isNotEmpty()) {
                    Utils.messagesFilteredNum.getAndIncrement()
                    val processedName = AdvancedSensitiveWords.sensitiveWordBs.replace(originalName)
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ITEM_METHOD)
                            .equals("cancel", ignoreCase = true)
                    ) {
                        event.isCancelled = true
                    } else {
                        meta.setDisplayName(processedName)
                        item.setItemMeta(meta)
                    }
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ITEM_SEND_MESSAGE)) {
                        player.sendMessage(
                            ChatColor.translateAlternateColorCodes(
                                '&',
                                AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_ITEM)
                            )
                        )
                    }
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                        Utils.logViolation(
                            player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Item)",
                            originalName + censoredWordList
                        )
                    }
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                        VelocitySender.send(player, ModuleType.ITEM, originalName, censoredWordList)
                    }
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                        BungeeSender.send(player, ModuleType.ITEM, originalName, censoredWordList)
                    }
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                        AdvancedSensitiveWords.databaseManager.checkAndUpdatePlayer(player.name)
                    }
                    val endTime = System.currentTimeMillis()
                    TimingUtils.addProcessStatistic(endTime, startTime)
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(
                        player,
                        ModuleType.ITEM,
                        originalName,
                        censoredWordList
                    )
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ITEM_PUNISH)) Punishment.punish(
                        player
                    )
                }
            }
        }
    }
}
