package io.wdsj.asw.bukkit.listener.paper

import io.papermc.paper.event.player.AsyncChatEvent
import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.ai.LlmChatDetectionService
import io.wdsj.asw.bukkit.integration.trchat.TrChatCompat
import io.wdsj.asw.bukkit.listener.abstraction.AbstractFakeMessageExecutor
import io.wdsj.asw.bukkit.permission.PermissionsEnum
import io.wdsj.asw.bukkit.permission.option.PlayerOptionResolver
import io.wdsj.asw.bukkit.permission.option.PlayerOptionView
import io.wdsj.asw.bukkit.permission.option.PlayerOptions
import io.wdsj.asw.bukkit.service.chat.antispam.ChatAntiSpamService
import io.wdsj.asw.bukkit.setting.PaperConfigurationService
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.common.type.ModuleType
import io.wdsj.asw.bukkit.util.PlayerProcessingGuard
import io.wdsj.asw.bukkit.util.SensitiveFilterEvents
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
    private val antiSpamService = ChatAntiSpamService(configuration)

    @EventHandler(priority = EventPriority.LOWEST)
    fun onChat(event: AsyncChatEvent) {
        val globalEnabled = configuration.get(PluginSettings.ENABLE_CHAT_CHECK)
        if (!globalEnabled) return

        val player = event.player
        if (processingGuard.shouldSkip(player, PermissionsEnum.BYPASS_CHAT)) return
        val options = PlayerOptionResolver.resolve(configuration, player)

        val startTime = System.currentTimeMillis()
        val originalMessage = preprocess(event.message())
        val originalPlainText = PlainTextComponentSerializer.plainText().serialize(originalMessage)

        if (antiSpamService.check(player.uniqueId, originalPlainText, options) == ChatAntiSpamService.Result.SPAM) {
            event.isCancelled = true
            if (options.bool(PlayerOptions.CHAT_ANTI_SPAM_SEND_MESSAGE, PluginSettings.CHAT_ANTI_SPAM_SEND_MESSAGE)) {
                MessageUtils.sendMessage(player, configuration.message(PluginMessages.MESSAGE_ON_CHAT_ANTI_SPAM))
            }
            return
        }

        val censoredWords = AdvancedSensitiveWords.findAllSensitive(originalPlainText)
        SensitiveFilterEvents.post(
            event.isAsynchronous,
            ModuleType.CHAT,
            player,
            originalPlainText,
            censoredWords,
        )

        if (censoredWords.isNotEmpty()) {
            handleDirectMessage(event, player, options, originalMessage, originalPlainText = originalPlainText, censoredWords, startTime)
            return
        }

        if (!handleContextMessage(event, player, options, originalPlainText, startTime)) {
            llmChatDetectionService.submit(player, originalPlainText, options)
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
        options: PlayerOptionView,
        originalMessage: Component,
        originalPlainText: String,
        censoredWords: List<String>,
        startTime: Long,
    ) {
        applyDirectMessageAction(event, options, originalMessage, originalPlainText)
        recordViolation(event, player, options, originalPlainText, originalPlainText, censoredWords, false, startTime)
    }

    private fun applyDirectMessageAction(
        event: AsyncChatEvent,
        options: PlayerOptionView,
        originalMessage: Component,
        originalPlainText: String,
    ) {
        if (isCancelMode(options)) {
            if (options.bool(PlayerOptions.CHAT_FAKE_MESSAGE_ON_CANCEL, PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL)) {
                markFakeMessage(event.player)
            } else {
                event.isCancelled = true
            }
            return
        }

        val processedMessage = AdvancedSensitiveWords.replaceSensitive(originalPlainText)
        val replacementConfig = TextReplacementConfig.builder()
            .matchLiteral(originalPlainText)
            .replacement(processedMessage)
            .build()
        event.message(originalMessage.replaceText(replacementConfig))
    }

    private fun handleContextMessage(
        event: AsyncChatEvent,
        player: Player,
        options: PlayerOptionView,
        originalPlainText: String,
        startTime: Long,
    ): Boolean {
        if (!options.bool(PlayerOptions.CHAT_CONTEXT_CHECK, PluginSettings.CHAT_CONTEXT_CHECK)) return false

        ChatContext.addMessage(player, originalPlainText)
        val originalContext = ChatContext.getHistory(player).joinToString("")
        val censoredWords = AdvancedSensitiveWords.findAllSensitive(originalContext)
        SensitiveFilterEvents.post(event.isAsynchronous, ModuleType.CHAT, player, originalContext, censoredWords)
        if (censoredWords.isEmpty()) return false

        ChatContext.pollPlayerContext(player)
        if (options.bool(PlayerOptions.CHAT_FAKE_MESSAGE_ON_CANCEL, PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL)) {
            markFakeMessage(player)
        } else {
            event.isCancelled = true
        }

        recordViolation(event, player, options, originalPlainText, originalContext, censoredWords, true, startTime)
        return true
    }

    private fun recordViolation(
        event: AsyncChatEvent,
        player: Player,
        options: PlayerOptionView,
        playerMessage: String,
        violationContent: String,
        censoredWords: List<String>,
        contextCheck: Boolean,
        startTime: Long,
    ) {
        if (options.bool(PlayerOptions.CHAT_SEND_MESSAGE, PluginSettings.CHAT_SEND_MESSAGE)) {
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

    private fun isCancelMode(options: PlayerOptionView): Boolean {
        return options.method(PlayerOptions.CHAT_METHOD, PluginSettings.CHAT_METHOD).isCancel
    }

    private fun markFakeMessage(player: Player) {
        if (!TrChatCompat.tryMarkFakeMessage(player)) {
            AbstractFakeMessageExecutor.selfIncrement(player)
        }
    }
}
