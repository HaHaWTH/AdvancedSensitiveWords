package io.wdsj.asw.bukkit.listener

import io.papermc.paper.event.player.AsyncChatEvent
import io.wdsj.asw.bukkit.integration.trchat.TrChatCompat
import io.wdsj.asw.bukkit.manage.punish.PlayerAltController
import io.wdsj.asw.bukkit.manage.punish.PlayerShadowController
import io.wdsj.asw.bukkit.setting.PaperConfigurationService
import io.wdsj.asw.bukkit.setting.PluginSettings
import net.kyori.adventure.audience.Audience
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class ShadowListener(private val configuration: PaperConfigurationService) : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        if (!PlayerShadowController.isShadowed(player)) return
        if (TrChatCompat.isEnabled()) return

        val viewers: MutableSet<Audience> = event.viewers()
        viewers.clear()
        if (configuration.get(PluginSettings.ENABLE_ALTS_CHECK) && PlayerAltController.hasAlt(player)) {
            for (alt in PlayerAltController.getAlts(player)) {
                val altPlayer = Bukkit.getPlayer(alt)
                altPlayer?.let { viewers.add(it) }
            }
        }
        viewers.add(player)
    }
}
