package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.isInitialized
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.sensitiveWordBs
import io.wdsj.asw.bukkit.setting.PaperConfigurationService
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.util.LoggingUtils
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.message.MessageUtils
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.server.BroadcastMessageEvent

class BroadcastListener(private val configuration: PaperConfigurationService) : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onBroadcast(event: BroadcastMessageEvent) {
        if (!isInitialized) return
        if (!configuration.get(PluginSettings.CHAT_BROADCAST_CHECK)) return

        val originalComponent = event.message()
        val originalMessage = preprocess(MessageUtils.plainText(originalComponent))
        val startTime = System.currentTimeMillis()
        val censoredWords = sensitiveWordBs.findAll(originalMessage)
        if (censoredWords.isEmpty()) return

        Utils.messagesFilteredNum.getAndIncrement()
        if (isCancelMode()) {
            event.isCancelled = true
        } else {
            event.message(
                MessageUtils.replaceLiteral(
                    originalComponent,
                    originalMessage,
                    sensitiveWordBs.replace(originalMessage),
                ),
            )
        }

        if (configuration.get(PluginSettings.LOG_VIOLATION)) {
            LoggingUtils.logViolation("Broadcast(IP: None)(BroadCast)", originalMessage + censoredWords)
        }
        TimingUtils.addProcessStatistic(System.currentTimeMillis(), startTime)
    }

    private fun preprocess(message: String): String {
        if (!configuration.get(PluginSettings.PRE_PROCESS)) return message
        return message.replace(Utils.preProcessRegex.toRegex(), "")
    }

    private fun isCancelMode(): Boolean {
        return configuration.get(PluginSettings.CHAT_METHOD).isCancel
    }
}
