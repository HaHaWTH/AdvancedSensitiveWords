package io.wdsj.asw.bukkit.listener.paper

import io.papermc.paper.event.player.AsyncChatEvent
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.sensitiveWordBs
import io.wdsj.asw.bukkit.ai.LlmChatDetectionService
import io.wdsj.asw.bukkit.integration.trchat.TrChatCompat
import io.wdsj.asw.bukkit.listener.abstraction.AbstractFakeMessageExecutor
import io.wdsj.asw.bukkit.setting.PaperConfigurationService
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.type.ModuleType
import io.wdsj.asw.bukkit.util.PlayerProcessingGuard
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.ViolationReporter
import io.wdsj.asw.bukkit.util.context.ChatContext
import io.wdsj.asw.bukkit.util.message.MessageUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

@Suppress("UNUSED")
class PaperChatListener(
    private val configuration: PaperConfigurationService,
    private val llmChatDetectionService: LlmChatDetectionService,
) : Listener {
    private val processingGuard = PlayerProcessingGuard(configuration)
    private val violationReporter = ViolationReporter(configuration)

    @EventHandler(priority = EventPriority.LOWEST)
    fun onChat(event: AsyncChatEvent) {
        if (!configuration.get(PluginSettings.ENABLE_CHAT_CHECK)) return

        val player = event.player
        if (processingGuard.shouldSkip(player)) return

        val startTime = System.currentTimeMillis()
        val originalMessage = preprocess(event.message())
        val originalPlainText = PlainTextComponentSerializer.plainText().serialize(originalMessage)
        val censoredWords = sensitiveWordBs.findAll(originalPlainText)

        if (censoredWords.isNotEmpty()) {
            handleDirectMessage(event, player, originalMessage, originalPlainText, censoredWords, startTime)
            return
        }

        if (!handleContextMessage(event, player, originalPlainText, startTime)) {
            llmChatDetectionService.submit(player.uniqueId, player.name, originalPlainText)
        }
    }

    private fun preprocess(message: Component): Component {
        if (!configuration.get(PluginSettings.PRE_PROCESS)) return message

        val replacementConfig = TextReplacementConfig.builder()
            .match(Utils.preProcessRegex.toPattern())
            .replacement("")
            .build()
        return message.replaceText(replacementConfig)
    }

    private fun handleDirectMessage(
        event: AsyncChatEvent,
        player: Player,
        originalMessage: Component,
        originalPlainText: String,
        censoredWords: List<String>,
        startTime: Long,
    ) {
        applyDirectMessageAction(event, originalMessage, originalPlainText)
        recordViolation(event, player, originalPlainText, originalPlainText, censoredWords, false, startTime)
    }

    private fun applyDirectMessageAction(
        event: AsyncChatEvent,
        originalMessage: Component,
        originalPlainText: String,
    ) {
        if (isCancelMode()) {
            if (configuration.get(PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL)) {
                markFakeMessage(event.player)
            } else {
                event.isCancelled = true
            }
            return
        }

        val processedMessage = sensitiveWordBs.replace(originalPlainText)
        val replacementConfig = TextReplacementConfig.builder()
            .matchLiteral(originalPlainText)
            .replacement(processedMessage)
            .build()
        event.message(originalMessage.replaceText(replacementConfig))
    }

    private fun handleContextMessage(
        event: AsyncChatEvent,
        player: Player,
        originalPlainText: String,
        startTime: Long,
    ): Boolean {
        if (!configuration.get(PluginSettings.CHAT_CONTEXT_CHECK)) return false

        ChatContext.addMessage(player, originalPlainText)
        val originalContext = ChatContext.getHistory(player).joinToString("")
        val censoredWords = sensitiveWordBs.findAll(originalContext)
        if (censoredWords.isEmpty()) return false

        ChatContext.pollPlayerContext(player)
        if (configuration.get(PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL)) {
            markFakeMessage(player)
        } else {
            event.isCancelled = true
        }

        recordViolation(event, player, originalPlainText, originalContext, censoredWords, true, startTime)
        return true
    }

    private fun recordViolation(
        event: AsyncChatEvent,
        player: Player,
        playerMessage: String,
        violationContent: String,
        censoredWords: List<String>,
        contextCheck: Boolean,
        startTime: Long,
    ) {
        if (configuration.get(PluginSettings.CHAT_SEND_MESSAGE)) {
            MessageUtils.sendMessage(
                player,
                configuration.message(PluginMessages.MESSAGE_ON_CHAT)
                    .replace("%integrated_player%", player.name)
                    .replace("%integrated_message%", playerMessage),
            )
        }

        val source = if (contextCheck) "Chat)(Context" else "Chat"
        violationReporter.report(
            player = player,
            moduleType = ModuleType.CHAT,
            content = violationContent,
            censoredWords = censoredWords,
            logSource = source,
            startTime = startTime,
            punishmentActions = configuration.get(PluginSettings.CHAT_PUNISHMENT),
            event = event,
        )
    }

    private fun isCancelMode(): Boolean {
        return configuration.get(PluginSettings.CHAT_METHOD).isCancel
    }

    private fun markFakeMessage(player: Player) {
        if (!TrChatCompat.tryMarkFakeMessage(player)) {
            AbstractFakeMessageExecutor.selfIncrement(player)
        }
    }
}
