package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.listener.command.CommandArgumentRuleSet
import io.wdsj.asw.bukkit.permission.PermissionsEnum
import io.wdsj.asw.bukkit.permission.option.PlayerOptionResolver
import io.wdsj.asw.bukkit.permission.option.PlayerOptionView
import io.wdsj.asw.bukkit.permission.option.PlayerOptions
import io.wdsj.asw.bukkit.setting.PaperConfigurationService
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.common.type.ModuleType
import io.wdsj.asw.bukkit.util.PlayerProcessingGuard
import io.wdsj.asw.bukkit.util.SensitiveFilterEvents
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
        val globalEnabled = configuration.get(PluginSettings.ENABLE_CHAT_CHECK)
        if (!globalEnabled) return

        val originalCommand = preprocess(event.message)
        val player = event.player
        if (processingGuard.shouldSkip(player, PermissionsEnum.BYPASS_COMMAND)) return
        val options = PlayerOptionResolver.resolve(configuration, player)

        val selection = configuration.commandArgumentRules().select(originalCommand)
        if (!configuration.shouldInspectCommand(selection) || selection.segments().isEmpty()) return

        val startTime = System.currentTimeMillis()
        val censoredWords = selection.segments().flatMap { segment -> AdvancedSensitiveWords.findAllSensitive(segment.content()) }
        SensitiveFilterEvents.post(event.isAsynchronous, ModuleType.CHAT, player, selection.scannedContent(), censoredWords)
        if (censoredWords.isEmpty()) return

        applyCommandAction(event, selection, options)
        recordViolation(event, player, options, selection.scannedContent(), censoredWords, startTime)
    }

    private fun preprocess(message: String): String {
        if (!configuration.get(PluginSettings.PRE_PROCESS)) return message
        return message.replace(Utils.preProcessRegex.toRegex(), "")
    }

    private fun applyCommandAction(
        event: PlayerCommandPreprocessEvent,
        selection: CommandArgumentRuleSet.CommandSelection,
        options: PlayerOptionView,
    ) {
        if (isCancelMode(options)) {
            event.isCancelled = true
            return
        }

        val processedCommand = selection.replaceSelected(AdvancedSensitiveWords::replaceSensitive)
        event.message = if (Utils.isCommand(processedCommand)) processedCommand else "/$processedCommand"
    }

    private fun recordViolation(
        event: PlayerCommandPreprocessEvent,
        player: Player,
        options: PlayerOptionView,
        originalCommand: String,
        censoredWords: List<String>,
        startTime: Long,
    ) {
        if (options.bool(PlayerOptions.CHAT_SEND_MESSAGE, PluginSettings.CHAT_SEND_MESSAGE)) {
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

    private fun isCancelMode(options: PlayerOptionView): Boolean {
        return options.method(PlayerOptions.CHAT_METHOD, PluginSettings.CHAT_METHOD).isCancel
    }
}
