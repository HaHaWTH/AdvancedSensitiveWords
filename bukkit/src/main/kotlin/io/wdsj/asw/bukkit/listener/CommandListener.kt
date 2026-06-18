package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.messagesManager
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.sensitiveWordBs
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.manage.notice.Notifier
import io.wdsj.asw.bukkit.manage.punish.Punishment
import io.wdsj.asw.bukkit.manage.punish.ViolationCounter
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.type.ModuleType
import io.wdsj.asw.bukkit.util.LoggingUtils
import io.wdsj.asw.bukkit.util.PlayerProcessingGuard
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.message.MessageUtils
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent

class CommandListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        if (!settingsManager.getProperty(PluginSettings.ENABLE_CHAT_CHECK)) return

        val player = event.player
        val originalCommand = preprocess(event.message)
        if (PlayerProcessingGuard.shouldSkip(player, originalCommand)) return

        val startTime = System.currentTimeMillis()
        val censoredWords = sensitiveWordBs.findAll(originalCommand)
        if (censoredWords.isEmpty()) return

        Utils.messagesFilteredNum.getAndIncrement()
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

        if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
            LoggingUtils.logViolation(
                player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Chat)",
                originalCommand + censoredWords,
            )
        }

        ViolationCounter.INSTANCE.incrementViolationCount(player)
        if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
            VelocitySender.sendNotifyMessage(player, ModuleType.CHAT, originalCommand, censoredWords)
        }

        TimingUtils.addProcessStatistic(System.currentTimeMillis(), startTime)
        if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) {
            Notifier.notice(player, ModuleType.CHAT, originalCommand, censoredWords)
        }
        if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) {
            Punishment.punish(player)
        }
    }

    private fun isCancelMode(): Boolean {
        return settingsManager.getProperty(PluginSettings.CHAT_METHOD).equals("cancel", ignoreCase = true)
    }
}
