package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.manage.punish.PlayerAltController
import io.wdsj.asw.bukkit.setting.PluginSettings
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import java.util.concurrent.ConcurrentHashMap

class FakeMessageExecutor : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        if (shouldFakeMessage(player)) {
            selfDecrement(player) // Decrease even the fake message is disabled
            if (settingsManager.getProperty(PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL)) {
                val players: MutableCollection<Player> = event.recipients
                players.clear()
                if (settingsManager.getProperty(PluginSettings.ENABLE_ALTS_CHECK) && PlayerAltController.hasAlt(player)) {
                    val alts = PlayerAltController.getAlts(player)
                    for (alt in alts) {
                        val altPlayer = Bukkit.getPlayer(alt)
                        altPlayer?.let { players.add(it) }
                    }
                }
                players.add(player)
            }
        }
    }

    private fun shouldFakeMessage(player: Player): Boolean {
        return FAKE_MESSAGE_NUM.getOrPut(player) { 0 } > 0
    }

    companion object {
        @JvmStatic
        private val FAKE_MESSAGE_NUM = ConcurrentHashMap<Player, Int>()
        @JvmStatic
        fun selfDecrement(player: Player) {
            val currentNum = FAKE_MESSAGE_NUM.getOrPut(player) { 0 }
            if (currentNum > 0) {
                FAKE_MESSAGE_NUM[player] = currentNum - 1
            }
        }

        @JvmStatic
        fun selfIncrement(player: Player) {
            FAKE_MESSAGE_NUM[player] = FAKE_MESSAGE_NUM.getOrPut(player) { 0 } + 1        }
    }
}
