package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.*
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.util.LoggingUtils
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.ItemSpawnEvent

class ItemSpawnListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onItemSpawn(event: ItemSpawnEvent) {
        if (!isInitialized || !settingsManager.getProperty(PluginSettings.ITEM_MONITOR_SPAWN) || !settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_ITEM_CHECK)) return
        val itemEntity = event.entity
        itemEntity.customName?.let {
            var originalName = it
            if (settingsManager.getProperty(PluginSettings.PRE_PROCESS)) originalName =
                originalName.replace(
                    Utils.getPreProcessRegex().toRegex(), ""
                )
            val startTime = System.currentTimeMillis()
            val censoredWordList = sensitiveWordBs.findAll(originalName)
            if (censoredWordList.isNotEmpty()) {
                Utils.messagesFilteredNum.getAndIncrement()
                if (settingsManager.getProperty(PluginSettings.ITEM_METHOD).equals("cancel", ignoreCase = true)) {
                    itemEntity.customName = null
                } else {
                    val processedName = sensitiveWordBs.replace(originalName)
                    itemEntity.customName = processedName
                }
                val locationLog = itemEntity.location.toLogString()
                if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                    LoggingUtils.logViolation("ItemSpawn(IP: None)(ItemSpawn)($locationLog)", originalName + censoredWordList)
                }
                val endTime = System.currentTimeMillis()
                TimingUtils.addProcessStatistic(endTime, startTime)
            }
        }
    }
    private fun Location.toLogString(): String {
        return "World: ${this.world?.name ?: "Unknown"}, X: ${this.x}, Y: ${this.y}, Z: ${this.z}"
    }
}