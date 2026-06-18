package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.getScheduler
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.sensitiveWordBs
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.type.ModuleType
import io.wdsj.asw.bukkit.util.PlayerProcessingGuard
import io.wdsj.asw.bukkit.util.PlayerUtils
import io.wdsj.asw.bukkit.util.ViolationReporter
import io.wdsj.asw.bukkit.util.message.MessageUtils
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.geysermc.floodgate.api.FloodgateApi

class PlayerLoginListener : Listener {
    private val processingGuard = PlayerProcessingGuard()
    private val violationReporter = ViolationReporter()

    @EventHandler(priority = EventPriority.LOWEST)
    fun onLogin(event: PlayerLoginEvent) {
        if (!settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_NAME_CHECK)) return

        val player = event.player
        if (processingGuard.shouldSkipBasic(player)) return
        if (shouldIgnorePlayer(player)) return

        val playerName = player.name
        val startTime = System.currentTimeMillis()
        val censoredWords = sensitiveWordBs.findAll(playerName)
        if (censoredWords.isEmpty()) return

        applyNameAction(event, player, playerName)

        violationReporter.reportWithCustomLogPrefix(
            player = player,
            moduleType = ModuleType.NAME,
            content = playerName,
            censoredWords = censoredWords,
            logPrefix = "${player.name}(IP: ${event.address.hostAddress})(Name)",
            startTime = startTime,
            punish = settingsManager.getProperty(PluginSettings.NAME_PUNISH),
        )
    }

    private fun shouldIgnorePlayer(player: Player): Boolean {
        if (PlayerUtils.isNpc(player) && settingsManager.getProperty(PluginSettings.NAME_IGNORE_NPC)) return true
        if (!settingsManager.getProperty(PluginSettings.NAME_IGNORE_BEDROCK)) return false
        if (Bukkit.getPluginManager().getPlugin("floodgate") == null) return false

        return FloodgateApi.getInstance().isFloodgatePlayer(player.uniqueId)
    }

    private fun applyNameAction(event: PlayerLoginEvent, player: Player, playerName: String) {
        if (isReplaceMode()) {
            val processedPlayerName = sensitiveWordBs.replace(playerName)
            player.setDisplayName(processedPlayerName)
            player.setPlayerListName(processedPlayerName)
            scheduleNameWarning(player)
            return
        }

        event.disallow(
            PlayerLoginEvent.Result.KICK_OTHER,
            MessageUtils.retrieveMessage(PluginMessages.MESSAGE_ON_NAME),
        )
    }

    private fun scheduleNameWarning(player: Player) {
        if (!settingsManager.getProperty(PluginSettings.NAME_SEND_MESSAGE)) return

        getScheduler().runTaskLater({
            MessageUtils.sendMessage(player, PluginMessages.MESSAGE_ON_NAME)
        }, 20L * 3L)
    }

    private fun isReplaceMode(): Boolean {
        return settingsManager.getProperty(PluginSettings.NAME_METHOD).equals("replace", ignoreCase = true)
    }
}
