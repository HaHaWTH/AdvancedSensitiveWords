package io.wdsj.asw.bukkit.listener

import io.papermc.paper.event.player.PlayerServerFullCheckEvent
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.sensitiveWordBs
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.manage.notice.Notifier
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.type.ModuleType
import io.wdsj.asw.bukkit.util.LoggingUtils
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.message.MessageUtils
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.geysermc.floodgate.api.FloodgateApi

class PlayerLoginListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onServerFullCheck(event: PlayerServerFullCheckEvent) {
        if (!settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_NAME_CHECK)) return
        if (shouldIgnoreProfile(event)) return

        val playerName = event.playerProfile.name ?: return
        val startTime = System.currentTimeMillis()
        val censoredWords = sensitiveWordBs.findAll(playerName)
        if (censoredWords.isEmpty()) return

        denyLogin(event)
        reportViolation(playerName, censoredWords, startTime)
    }

    private fun shouldIgnoreProfile(event: PlayerServerFullCheckEvent): Boolean {
        if (!settingsManager.getProperty(PluginSettings.NAME_IGNORE_BEDROCK)) return false
        if (Bukkit.getPluginManager().getPlugin("floodgate") == null) return false

        val profileId = event.playerProfile.id ?: return false
        return FloodgateApi.getInstance().isFloodgatePlayer(profileId)
    }

    private fun denyLogin(event: PlayerServerFullCheckEvent) {
        val kickMessage = if (settingsManager.getProperty(PluginSettings.NAME_SEND_MESSAGE)) {
            MessageUtils.retrieveComponent(PluginMessages.MESSAGE_ON_NAME)
        } else {
            event.kickMessage()
        }
        event.deny(kickMessage)
    }

    private fun reportViolation(playerName: String, censoredWords: List<String>, startTime: Long) {
        Utils.messagesFilteredNum.getAndIncrement()

        if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
            LoggingUtils.logViolation("$playerName(IP: Unknown)(Name)", playerName + censoredWords)
        }

        TimingUtils.addProcessStatistic(System.currentTimeMillis(), startTime)

        if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) {
            val message = MessageUtils.retrieveMessage(PluginMessages.ADMIN_REMINDER)
                .replace("%player%", playerName)
                .replace("%type%", ModuleType.NAME.toString())
                .replace("%message%", playerName)
                .replace("%censored_list%", censoredWords.toString())
                .replace("%violation%", "N/A")
            Notifier.normalNotice(message)
        }
    }
}
