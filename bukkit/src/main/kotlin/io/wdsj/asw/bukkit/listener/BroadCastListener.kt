package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.isInitialized
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.sensitiveWordBs
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
        if (!isInitialized) return
        if (!settingsManager.getProperty(PluginSettings.CHAT_BROADCAST_CHECK)) return

        val originalMessage = preprocess(event.message)
        val startTime = System.currentTimeMillis()
        val censoredWords = sensitiveWordBs.findAll(originalMessage)
        if (censoredWords.isEmpty()) return

        Utils.messagesFilteredNum.getAndIncrement()
        if (isCancelMode()) {
            event.isCancelled = true
        } else {
            event.message = sensitiveWordBs.replace(originalMessage)
        }

        if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
            LoggingUtils.logViolation("Broadcast(IP: None)(BroadCast)", originalMessage + censoredWords)
        }
        TimingUtils.addProcessStatistic(System.currentTimeMillis(), startTime)
    }

    private fun preprocess(message: String): String {
        if (!settingsManager.getProperty(PluginSettings.PRE_PROCESS)) return message
        return message.replace(Utils.preProcessRegex.toRegex(), "")
    }

    private fun isCancelMode(): Boolean {
        return settingsManager.getProperty(PluginSettings.CHAT_METHOD).equals("cancel", ignoreCase = true)
    }
}
