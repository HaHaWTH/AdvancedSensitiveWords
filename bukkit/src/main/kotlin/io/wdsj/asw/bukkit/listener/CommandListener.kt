package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.messagesManager
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.sensitiveWordBs
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.type.ModuleType
import io.wdsj.asw.bukkit.util.PlayerProcessingGuard
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.ViolationReporter
import io.wdsj.asw.bukkit.util.message.MessageUtils
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent

class CommandListener : Listener {
    private val processingGuard = PlayerProcessingGuard()
    private val violationReporter = ViolationReporter()

    @EventHandler(priority = EventPriority.LOWEST)
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        if (!settingsManager.getProperty(PluginSettings.ENABLE_CHAT_CHECK)) return

        val player = event.player
        val originalCommand = preprocess(event.message)
        if (processingGuard.shouldSkip(player, originalCommand)) return

        val startTime = System.currentTimeMillis()
        val censoredWords = sensitiveWordBs.findAll(originalCommand)
        if (censoredWords.isEmpty()) return

        applyCommandAction(event, originalCommand)
        recordViolation(player, originalCommand, censoredWords, startTime)
    }

    private fun preprocess(message: String): String {
        if (!settingsManager.getProperty(PluginSettings.PRE_PROCESS)) return message
        return message.replace(Utils.preProcessRegex.toRegex(), "")
    }

    private fun applyCommandAction(event: PlayerCommandPreprocessEvent, originalCommand: String) {
        if (isCancelMode()) {
            event.isCancelled = true
            return
        }

        val processedCommand = sensitiveWordBs.replace(originalCommand)
        event.message = if (Utils.isCommand(processedCommand)) processedCommand else "/$processedCommand"
    }

    private fun recordViolation(
        player: Player,
        originalCommand: String,
        censoredWords: List<String>,
        startTime: Long,
    ) {
        if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
            MessageUtils.sendMessage(
                player,
                messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT)
                    .replace("%integrated_player%", player.name)
                    .replace("%integrated_message%", originalCommand),
            )
        }

        violationReporter.report(
            player = player,
            moduleType = ModuleType.CHAT,
            content = originalCommand,
            censoredWords = censoredWords,
            logSource = "Chat",
            startTime = startTime,
            punish = settingsManager.getProperty(PluginSettings.CHAT_PUNISH),
        )
    }

    private fun isCancelMode(): Boolean {
        return settingsManager.getProperty(PluginSettings.CHAT_METHOD).equals("cancel", ignoreCase = true)
    }
}
