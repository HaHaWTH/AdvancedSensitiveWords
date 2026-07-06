package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.service.chat.antispam.ChatAntiSpamService
import io.wdsj.asw.bukkit.manage.punish.PlayerShadowController
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
        val player = event.player
        doCleanTask(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onKick(event: PlayerKickEvent) {
        val player = event.player
        doCleanTask(player)
    }

    private fun doCleanTask(player: Player) {
        ChatContext.clearPlayerContext(player)
        SignContext.clearPlayerContext(player)
        ChatAntiSpamService.clear(player.uniqueId)
        AdvancedSensitiveWords.getInstance().playerGroupService?.handleQuit(player.uniqueId)
        PlayerShadowController.unshadowPlayer(player)
    }
}
