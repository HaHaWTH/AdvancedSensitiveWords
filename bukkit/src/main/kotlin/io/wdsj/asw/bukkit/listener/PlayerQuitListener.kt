package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.util.context.ChatContext
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class PlayerQuitListener : Listener {
    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        if (!AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.FLUSH_PLAYER_DATA_CACHE)) return
        val player = event.player
        doFlushTask(player)
    }

    private fun doFlushTask(player: Player) {
        ChatContext.removePlayerContext(player)
    }
}
