package io.wdsj.asw.bukkit.listener

import io.papermc.paper.event.player.AsyncChatEvent
import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.annotation.PaperEventHandler
import io.wdsj.asw.bukkit.manage.punish.PlayerAltController
import io.wdsj.asw.bukkit.manage.punish.PlayerShadowController
import io.wdsj.asw.bukkit.setting.PluginSettings
import net.kyori.adventure.audience.Audience
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

@PaperEventHandler
class ShadowListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        if (PlayerShadowController.isShadowed(player)) {
            val viewers: MutableSet<Audience> = event.viewers()
            viewers.clear()
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_ALTS_CHECK) && PlayerAltController.hasAlt(player)) {
                val alts = PlayerAltController.getAlts(player)
                for (alt in alts) {
                    val altPlayer = Bukkit.getPlayer(alt)
                    altPlayer?.let { viewers.add(it) }
                }
            }
            viewers.add(player)
        }
    }
}
