package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.manage.punish.PlayerAltController
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.util.PlayerUtils
import io.wdsj.asw.bukkit.util.Utils
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class AltsListener : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!settingsManager.getProperty(PluginSettings.ENABLE_ALTS_CHECK)) {
            return
        }
        val player = event.player
        if (PlayerUtils.isNpc(player)) {
            return
        }
        val ip = Utils.getPlayerIp(player)
        if (!PlayerAltController.contains(ip, player)) {
            PlayerAltController.addToAlts(ip, player)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!settingsManager.getProperty(PluginSettings.ENABLE_ALTS_CHECK)) {
            return
        }
        val player = event.player
        if (PlayerUtils.isNpc(player)) {
            return
        }
        val ip = Utils.getPlayerIp(player)
        PlayerAltController.removeFromAlts(ip, player)
    }
}
