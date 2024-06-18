package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.manage.punish.PlayerShadowController
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class ShadowListener : Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        if (PlayerShadowController.isShadowed(player)) {
            val recipients: MutableCollection<Player> = event.recipients
            recipients.clear()
            recipients.add(player)
        }
    }
}
