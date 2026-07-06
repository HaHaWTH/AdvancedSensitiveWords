package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.manage.playergroup.PlayerGroupService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent

class PlayerGroupListener(private val playerGroupService: PlayerGroupService) : Listener {
    @EventHandler
    fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
        playerGroupService.preload(event.uniqueId, event.name)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        playerGroupService.handleJoin(event.player)
    }
}
