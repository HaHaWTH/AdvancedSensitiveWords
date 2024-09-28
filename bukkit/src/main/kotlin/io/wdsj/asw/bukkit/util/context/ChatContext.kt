package io.wdsj.asw.bukkit.util.context

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.common.datatype.TimedString
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

object ChatContext {
    private val chatHistory = ConcurrentHashMap<UUID, Deque<TimedString>>()

    /**
     * Add player message to history
     */
    fun addMessage(player: Player, message: String) {
        val uuid = player.uniqueId
        val history = chatHistory.getOrPut(uuid) { ConcurrentLinkedDeque() }
        while (history.size >= settingsManager.getProperty(PluginSettings.CHAT_CONTEXT_MAX_SIZE)) {
            history.pollFirst()
        }
        message.trim().takeIf { it.isNotEmpty() } ?.let {
            history.offerLast(TimedString.of(it))
        }
    }

    fun getHistory(player: Player): Deque<String> {
        val uuid = player.uniqueId
        val tsHistory = chatHistory.getOrPut(uuid) { ConcurrentLinkedDeque() }
        if (tsHistory.isEmpty()) return ConcurrentLinkedDeque()
        tsHistory.removeIf {
            (System.currentTimeMillis() - it.time) / 1000 > settingsManager.getProperty(
                PluginSettings.CHAT_CONTEXT_TIME_LIMIT
            )
        }
        return tsHistory.mapTo(ConcurrentLinkedDeque()) { it.string }
    }

    fun clearPlayerContext(player: Player) {
        val uuid = player.uniqueId
        chatHistory.remove(uuid)
    }

    fun pollPlayerContext(player: Player) {
        val uuid = player.uniqueId
        val history = chatHistory[uuid]
        history?.pollLast()
    }

    @JvmStatic
    fun forceClearContext() {
        chatHistory.clear()
    }
}
