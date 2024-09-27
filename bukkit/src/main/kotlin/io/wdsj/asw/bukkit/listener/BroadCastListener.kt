package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.util.LoggingUtils
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.server.BroadcastMessageEvent

class BroadCastListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onBroadCast(event: BroadcastMessageEvent) {
        if (!AdvancedSensitiveWords.isInitialized || !settingsManager.getProperty(PluginSettings.CHAT_BROADCAST_CHECK)) return
        var originalMessage = event.message
        if (settingsManager.getProperty(PluginSettings.PRE_PROCESS)) originalMessage =
            originalMessage.replace(
                Utils.preProcessRegex.toRegex(), ""
            )
        val startTime = System.currentTimeMillis()
        val censoredWordList = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalMessage)
        if (censoredWordList.isNotEmpty()) {
            Utils.messagesFilteredNum.getAndIncrement()
            val processedMessage = AdvancedSensitiveWords.sensitiveWordBs.replace(originalMessage)
            if (settingsManager.getProperty(PluginSettings.CHAT_METHOD)
                    .equals("cancel", ignoreCase = true)
            ) {
                event.isCancelled = true
            } else {
                event.message = processedMessage
            }
            if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                LoggingUtils.logViolation("Broadcast(IP: None)(BroadCast)", originalMessage + censoredWordList)
            }
            val endTime = System.currentTimeMillis()
            TimingUtils.addProcessStatistic(endTime, startTime)
        }
    }
}
