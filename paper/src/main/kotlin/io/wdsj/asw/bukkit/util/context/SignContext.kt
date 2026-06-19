package io.wdsj.asw.bukkit.util.context

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.setting.PluginSettings
import org.bukkit.entity.Player
import org.bukkit.block.sign.Side
import java.util.UUID

object SignContext {
    private val signEditHistory = ContextHistory<SignContextEntry>()

    fun addMessage(player: Player, entry: SignContextEntry) {
        if (entry.content.isBlank()) return

        signEditHistory.removeMatching(player.uniqueId) { it.target == entry.target }
        signEditHistory.add(player.uniqueId, contextCapacity(), entry)
    }

    fun getHistory(player: Player): List<SignContextEntry> {
        return signEditHistory.snapshot(
            player.uniqueId,
            contextCapacity(),
            settingsManager.getProperty(PluginSettings.SIGN_CONTEXT_TIME_LIMIT) * 1_000L,
        ) { it.time }
    }

    fun clearPlayerContext(player: Player) {
        signEditHistory.clear(player.uniqueId)
    }

    @JvmStatic
    fun forceClearContext() {
        signEditHistory.clearAll()
    }

    private fun contextCapacity(): Int = settingsManager.getProperty(PluginSettings.SIGN_CONTEXT_MAX_SIZE)
}

data class SignContextEntry(
    val content: String,
    val target: SignContextTarget,
    val lineLengths: List<Int>,
    val time: Long = System.currentTimeMillis(),
)

data class SignContextTarget(
    val worldId: UUID,
    val x: Int,
    val y: Int,
    val z: Int,
    val side: Side,
)
