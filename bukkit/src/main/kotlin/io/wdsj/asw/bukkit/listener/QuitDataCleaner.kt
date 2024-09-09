package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.util.context.ChatContext
import io.wdsj.asw.bukkit.util.context.SignContext
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent

class QuitDataCleaner : Listener {
    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        if (!settingsManager.getProperty(PluginSettings.CLEAN_PLAYER_DATA_CACHE)) return
        val player = event.player
        doCleanTask(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onKick(event: PlayerKickEvent) {
        if (!settingsManager.getProperty(PluginSettings.CLEAN_PLAYER_DATA_CACHE)) return
        val player = event.player
        doCleanTask(player)
    }

    private fun doCleanTask(player: Player) {
        ChatContext.clearPlayerContext(player)
        SignContext.clearPlayerContext(player)
    }
}
