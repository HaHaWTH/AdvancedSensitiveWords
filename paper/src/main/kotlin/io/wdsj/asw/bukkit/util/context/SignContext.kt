package io.wdsj.asw.bukkit.util.context

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.setting
import io.wdsj.asw.bukkit.setting.PluginSettings
import org.bukkit.entity.Player
import org.bukkit.block.sign.Side
import java.util.UUID

object SignContext {
    private val signEditHistory = ContextHistory<SignContextEntry>()

    fun addMessage(player: Player, entry: SignContextEntry) {
        addMessage(player, entry, contextCapacity())
    }

    fun addMessage(player: Player, entry: SignContextEntry, capacity: Int) {
        if (entry.content.isBlank()) return

        signEditHistory.removeMatching(player.uniqueId) { it.target == entry.target }
        signEditHistory.add(player.uniqueId, capacity.coerceAtLeast(1), entry)
    }

    fun getHistory(player: Player): List<SignContextEntry> {
        return getHistory(player, contextCapacity(), setting(PluginSettings.SIGN_CONTEXT_TIME_LIMIT))
    }

    fun getHistory(player: Player, capacity: Int, timeLimitSeconds: Int): List<SignContextEntry> {
        return signEditHistory.snapshot(
            player.uniqueId,
            capacity.coerceAtLeast(1),
            timeLimitSeconds.coerceAtLeast(1) * 1_000L,
        ) { it.time }
    }

    fun clearPlayerContext(player: Player) {
        signEditHistory.clear(player.uniqueId)
    }

    @JvmStatic
    fun forceClearContext() {
        signEditHistory.clearAll()
    }

    private fun contextCapacity(): Int = setting(PluginSettings.SIGN_CONTEXT_MAX_SIZE)
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
