package io.wdsj.asw.bukkit.util.context

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.setting
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.common.datatype.TimedString
import org.bukkit.entity.Player

object ChatContext {
    private val chatHistory = ContextHistory<TimedString>()

    /**
     * Add player message to history
     */
    fun addMessage(player: Player, message: String) {
        message.trim().takeIf { it.isNotEmpty() }?.let {
            chatHistory.add(player.uniqueId, contextCapacity(), TimedString.of(it))
        }
    }

    fun getHistory(player: Player): List<String> {
        return chatHistory.snapshot(
            player.uniqueId,
            contextCapacity(),
            setting(PluginSettings.CHAT_CONTEXT_TIME_LIMIT) * 1_000L,
        ) { it.time }.map { it.string }
    }

    fun clearPlayerContext(player: Player) {
        chatHistory.clear(player.uniqueId)
    }

    fun pollPlayerContext(player: Player) {
        chatHistory.removeLast(player.uniqueId)
    }

    @JvmStatic
    fun forceClearContext() {
        chatHistory.clearAll()
    }

    private fun contextCapacity(): Int = setting(PluginSettings.CHAT_CONTEXT_MAX_SIZE)
}
