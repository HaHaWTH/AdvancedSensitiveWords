package io.wdsj.asw.bukkit.listener

import com.github.houbb.heaven.util.lang.StringUtil
import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.event.ASWFilterEvent
import io.wdsj.asw.bukkit.event.EventType
import io.wdsj.asw.bukkit.manage.notice.Notifier
import io.wdsj.asw.bukkit.manage.permission.Permissions
import io.wdsj.asw.bukkit.manage.punish.Punishment
import io.wdsj.asw.bukkit.proxy.bungee.BungeeSender
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import org.bukkit.Bukkit
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
        if (player.hasPermission(Permissions.BYPASS)) return
        var shouldSendMessage = false
        val startTime = System.currentTimeMillis()
        val indexList: MutableList<Int> = ArrayList()
        val originalMultiMessages = StringBuilder()
        for (line in 0..3) {
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
                    shouldSendMessage = true
                    event.isCancelled = true
                    break
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

        if (shouldSendMessage && AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_API)) {
            Bukkit.getPluginManager()
                .callEvent(ASWFilterEvent(player, outMessage, outProcessedMessage, outList, EventType.SIGN, false))
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
            Utils.logViolation(player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Sign)", outMessage + outList)
        }

        if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_VELOCITY) && shouldSendMessage) {
            VelocitySender.send(player, EventType.SIGN, outMessage, outList)
        }

        if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
            BungeeSender.send(player, EventType.SIGN, outMessage, outList)
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
                EventType.SIGN,
                outMessage,
                outList
            )
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.SIGN_PUNISH)) Punishment.punish(player)
        }
    }
}
