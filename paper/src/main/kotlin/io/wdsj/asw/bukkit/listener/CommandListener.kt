package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.sensitiveWordBs
import io.wdsj.asw.bukkit.listener.command.CommandArgumentRuleSet
import io.wdsj.asw.bukkit.setting.PaperConfigurationService
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

class CommandListener(private val configuration: PaperConfigurationService) : Listener {
    private val processingGuard = PlayerProcessingGuard(configuration)
    private val violationReporter = ViolationReporter(configuration)

    @EventHandler(priority = EventPriority.LOWEST)
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        if (!configuration.get(PluginSettings.ENABLE_CHAT_CHECK)) return

        val originalCommand = preprocess(event.message)
        val player = event.player
        if (processingGuard.shouldSkip(player)) return

        val selection = configuration.commandArgumentRules().select(originalCommand)
        if (!configuration.shouldInspectCommand(selection) || selection.segments().isEmpty()) return

        val startTime = System.currentTimeMillis()
        val censoredWords = selection.segments().flatMap { segment -> sensitiveWordBs.findAll(segment.content()) }
        if (censoredWords.isEmpty()) return

        applyCommandAction(event, selection)
        recordViolation(event, player, selection.scannedContent(), censoredWords, startTime)
    }

    private fun preprocess(message: String): String {
        if (!configuration.get(PluginSettings.PRE_PROCESS)) return message
        return message.replace(Utils.preProcessRegex.toRegex(), "")
    }

    private fun applyCommandAction(
        event: PlayerCommandPreprocessEvent,
        selection: CommandArgumentRuleSet.CommandSelection,
    ) {
        if (isCancelMode()) {
            event.isCancelled = true
            return
        }

        val processedCommand = selection.replaceSelected(sensitiveWordBs::replace)
        event.message = if (Utils.isCommand(processedCommand)) processedCommand else "/$processedCommand"
    }

    private fun recordViolation(
        event: PlayerCommandPreprocessEvent,
        player: Player,
        originalCommand: String,
        censoredWords: List<String>,
        startTime: Long,
    ) {
        if (configuration.get(PluginSettings.CHAT_SEND_MESSAGE)) {
            MessageUtils.sendMessage(
                player,
                configuration.message(PluginMessages.MESSAGE_ON_CHAT)
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
            punishmentActions = configuration.get(PluginSettings.CHAT_PUNISHMENT),
            event = event,
        )
    }

    private fun isCancelMode(): Boolean {
        return configuration.get(PluginSettings.CHAT_METHOD).isCancel
    }
}
