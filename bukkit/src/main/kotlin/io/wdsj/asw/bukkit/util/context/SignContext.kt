package io.wdsj.asw.bukkit.util.context

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.common.datatype.TimedString
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

object SignContext {
    private val signEditHistory = ConcurrentHashMap<UUID, Deque<TimedString>>()

    /**
     * Add player message to history
     */
    fun addMessage(player: Player, message: String) {
        val uuid = player.uniqueId
        val history = signEditHistory.getOrPut(uuid) { ConcurrentLinkedDeque() }
        while (history.size >= settingsManager.getProperty(PluginSettings.SIGN_CONTEXT_MAX_SIZE)) {
            history.pollFirst()
        }
        if (message.trim().isEmpty()) return
        history.offerLast(TimedString.of(message.trim()))
    }

    fun getHistory(player: Player): Deque<String> {
        val uuid = player.uniqueId
        val tsHistory = signEditHistory.getOrPut(uuid) { ConcurrentLinkedDeque() }
        if (tsHistory.isEmpty()) return ConcurrentLinkedDeque()
        tsHistory.removeIf {
            (System.currentTimeMillis() - it.time) / 1000 > settingsManager.getProperty(
                PluginSettings.SIGN_CONTEXT_TIME_LIMIT
            )
        }
        return tsHistory.mapTo(ConcurrentLinkedDeque()) { it.string }
    }

    fun clearPlayerContext(player: Player) {
        val uuid = player.uniqueId
        signEditHistory.remove(uuid)
    }

    @JvmStatic
    fun pollPlayerContext(player: Player) {
        val uuid = player.uniqueId
        val history = signEditHistory[uuid]
        history?.pollLast()
    }

    @JvmStatic
    fun forceClearContext() {
        signEditHistory.clear()
    }
}
