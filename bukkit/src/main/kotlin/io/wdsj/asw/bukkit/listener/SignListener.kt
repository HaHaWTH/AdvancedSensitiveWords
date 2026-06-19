package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.sensitiveWordBs
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.type.ModuleType
import io.wdsj.asw.bukkit.integration.sign.SignFakeViewService
import io.wdsj.asw.bukkit.util.PlayerProcessingGuard
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.ViolationReporter
import io.wdsj.asw.bukkit.util.context.SignContext
import io.wdsj.asw.bukkit.util.message.MessageUtils
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent

// TODO: Paper event handler
class SignListener : Listener {
    private val processingGuard = PlayerProcessingGuard()
    private val violationReporter = ViolationReporter()

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onSign(event: SignChangeEvent) {
        if (!settingsManager.getProperty(PluginSettings.ENABLE_SIGN_EDIT_CHECK)) return
        if (event.lines().isEmpty()) return

        val player = event.player
        if (processingGuard.shouldSkipBasic(player)) return

        val startTime = System.currentTimeMillis()
        val attemptedLines = event.lines().toList()
        val lineScan = censorSingleLines(event)
        val violation = lineScan.violation
            ?: censorMultiLine(event, lineScan)
            ?: censorContext(event, player)
            ?: return

        if (isCancelMode() && settingsManager.getProperty(PluginSettings.SIGN_FAKE_ON_CANCEL)) {
            SignFakeViewService.recordCancelledEdit(
                event,
                player,
                attemptedLines,
                violation.content,
                violation.censoredWords,
            )
        }

        if (settingsManager.getProperty(PluginSettings.SIGN_SEND_MESSAGE)) {
            MessageUtils.sendMessage(player, PluginMessages.MESSAGE_ON_SIGN)
        }

        val location = event.block.location
        val locationLog = "World: ${location.world?.name ?: "Unknown"}, X: ${location.x}, Y: ${location.y}, Z: ${location.z}"
        violationReporter.reportWithCustomLogPrefix(
            player = player,
            moduleType = ModuleType.SIGN,
            content = violation.content,
            censoredWords = violation.censoredWords,
            logPrefix = "${player.name}(IP: ${Utils.getPlayerIp(player)})(Sign)($locationLog)",
            startTime = startTime,
            punish = settingsManager.getProperty(PluginSettings.SIGN_PUNISH),
        )
    }

    private fun censorSingleLines(event: SignChangeEvent): SignLineScan {
        var violation: SignViolation? = null
        val cleanLineIndexes = mutableListOf<Int>()
        val cleanLineContent = StringBuilder()

        for (lineIndex in event.lines().indices) {
            val originalComponent = event.line(lineIndex) ?: continue
            val originalMessage = preprocess(MessageUtils.plainText(originalComponent))
            val censoredWords = sensitiveWordBs.findAll(originalMessage)

            if (censoredWords.isEmpty()) {
                if (originalMessage.trim().isNotEmpty()) {
                    cleanLineIndexes.add(lineIndex)
                    cleanLineContent.append(originalMessage)
                }
                continue
            }

            violation = SignViolation(originalMessage, censoredWords)
            if (isCancelMode()) {
                event.isCancelled = true
                continue
            }
            val processedMessage = sensitiveWordBs.replace(originalMessage)
            event.line(lineIndex, MessageUtils.replaceLiteral(originalComponent, originalMessage, processedMessage))
        }

        return SignLineScan(violation, cleanLineIndexes, cleanLineContent.toString())
    }

    private fun censorMultiLine(event: SignChangeEvent, lineScan: SignLineScan): SignViolation? {
        if (!settingsManager.getProperty(PluginSettings.SIGN_MULTI_LINE_CHECK)) return null
        if (lineScan.cleanLineIndexes.isEmpty()) return null

        val censoredWords = sensitiveWordBs.findAll(lineScan.cleanLineContent)
        if (censoredWords.isEmpty()) return null

        if (isCancelMode()) {
            event.isCancelled = true
        } else {
            val processedMessage = sensitiveWordBs.replace(lineScan.cleanLineContent)
            for (lineIndex in lineScan.cleanLineIndexes) {
                event.line(lineIndex, MessageUtils.plainTextComponent(processedMessage))
            }
        }

        return SignViolation(lineScan.cleanLineContent, censoredWords)
    }

    private fun censorContext(event: SignChangeEvent, player: Player): SignViolation? {
        if (!settingsManager.getProperty(PluginSettings.SIGN_CONTEXT_CHECK)) return null

        val originalAllMessage = event.lines().joinToString("") { MessageUtils.plainText(it) }
        SignContext.addMessage(player, originalAllMessage)
        val originalContext = SignContext.getHistory(player).joinToString("")
        val censoredWords = sensitiveWordBs.findAll(originalContext)
        if (censoredWords.isEmpty()) return null

        SignContext.pollPlayerContext(player)
        event.isCancelled = true
        return SignViolation(originalContext, censoredWords)
    }

    private fun preprocess(text: String): String {
        if (!settingsManager.getProperty(PluginSettings.PRE_PROCESS)) return text
        return text.replace(Utils.preProcessRegex.toRegex(), "")
    }

    private fun isCancelMode(): Boolean {
        return settingsManager.getProperty(PluginSettings.SIGN_METHOD).equals("cancel", ignoreCase = true)
    }

    private data class SignLineScan(
        val violation: SignViolation?,
        val cleanLineIndexes: List<Int>,
        val cleanLineContent: String,
    )

    private data class SignViolation(
        val content: String,
        val censoredWords: List<String>,
    )
}
