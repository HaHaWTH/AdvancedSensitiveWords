package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.playergroup.PlayerGroupService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PlayerGroupListener(private val playerGroupService: PlayerGroupService) : Listener {
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        playerGroupService.handleJoin(event.player)
    }
}
