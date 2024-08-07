package io.wdsj.asw.bukkit.listener

import com.github.houbb.heaven.util.lang.StringUtil
import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.manage.notice.Notifier
import io.wdsj.asw.bukkit.manage.permission.Permissions
import io.wdsj.asw.bukkit.manage.punish.Punishment
import io.wdsj.asw.bukkit.proxy.bungee.BungeeSender
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.type.ModuleType
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.context.SignContext
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent

class SignListener : Listener {
    private var outMessage: String? = ""
    private var outList: List<String> = ArrayList()
    private var outProcessedMessage = ""


    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    fun onSign(event: SignChangeEvent) {
        if (!AdvancedSensitiveWords.isInitialized) return
        if (!AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_SIGN_EDIT_CHECK)) return
        val player = event.player
        if (player.hasPermission(Permissions.BYPASS) || event.lines.isEmpty()) return
        var shouldSendMessage = false
        val startTime = System.currentTimeMillis()
        val indexList: MutableList<Int> = ArrayList()
        val originalMultiMessages = StringBuilder()
        for (line in 0 until event.lines.size) {
            var originalMessage = event.getLine(line)
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.PRE_PROCESS) && originalMessage != null) originalMessage =
                originalMessage.replace(
                    Utils.getPreProcessRegex().toRegex(), ""
                )
            assert(originalMessage != null)
            val censoredWordList = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalMessage)
            if (censoredWordList.isNotEmpty()) {
                val processedMessage = AdvancedSensitiveWords.sensitiveWordBs.replace(originalMessage)
                outMessage = originalMessage
                outProcessedMessage = processedMessage
                outList = censoredWordList
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.SIGN_METHOD)
                        .equals("cancel", ignoreCase = true)
                ) {
                    event.isCancelled = true
                }
                event.setLine(line, processedMessage)
                shouldSendMessage = true
            } else if (StringUtil.isNotEmptyTrim(originalMessage)) {
                indexList.add(line)
                originalMultiMessages.append(originalMessage)
            }
        }
        if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.SIGN_MULTI_LINE_CHECK) && indexList.isNotEmpty()) {
            val originalMessagesString = originalMultiMessages.toString()
            val censoredWordList = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalMessagesString)
            if (censoredWordList.isNotEmpty()) {
                val processedMessagesString = AdvancedSensitiveWords.sensitiveWordBs.replace(originalMessagesString)
                outMessage = originalMessagesString
                outProcessedMessage = processedMessagesString
                outList = censoredWordList
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.SIGN_METHOD)
                        .equals("cancel", ignoreCase = true)
                ) {
                    shouldSendMessage = true
                    event.isCancelled = true
                } else {
                    shouldSendMessage = true
                    for (i in indexList) {
                        event.setLine(i, processedMessagesString)
                    }
                }
            }
        }

        // Sign context check
        if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.SIGN_CONTEXT_CHECK) && !shouldSendMessage) {
            val originalAllMessage = event.lines.joinToString("")
            SignContext.addMessage(player, originalAllMessage)
            val originalContext = SignContext.getHistory(player).joinToString("")
            val censoredContextList = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalContext)
            if (censoredContextList.isNotEmpty()) {
                SignContext.pollPlayerContext(player)
                val processedContext = AdvancedSensitiveWords.sensitiveWordBs.replace(originalContext)
                shouldSendMessage = true
                event.isCancelled = true
                outMessage = originalContext
                outProcessedMessage = processedContext
                outList = censoredContextList
            }
        }

        if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.SIGN_SEND_MESSAGE) && shouldSendMessage) {
            player.sendMessage(
                ChatColor.translateAlternateColorCodes(
                    '&',
                    AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_SIGN)
                )
            )
        }

        if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.LOG_VIOLATION) && shouldSendMessage) {
            val location = event.block.location
            val locationLog = "World: ${location.world?.name ?: "Unknown"}, X: ${location.x}, Y: ${location.y}, Z: ${location.z}"
            Utils.logViolation(player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Sign)(" + locationLog + ")", outMessage + outList)
        }

        if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_VELOCITY) && shouldSendMessage) {
            VelocitySender.send(player, ModuleType.SIGN, outMessage, outList)
        }

        if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
            BungeeSender.send(player, ModuleType.SIGN, outMessage, outList)
        }

        if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
            AdvancedSensitiveWords.databaseManager.checkAndUpdatePlayer(player.name)
        }

        if (shouldSendMessage) {
            Utils.messagesFilteredNum.getAndIncrement()
            val endTime = System.currentTimeMillis()
            TimingUtils.addProcessStatistic(endTime, startTime)
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(
                player,
                ModuleType.SIGN,
                outMessage,
                outList
            )
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.SIGN_PUNISH)) Punishment.punish(player)
        }
    }
}
