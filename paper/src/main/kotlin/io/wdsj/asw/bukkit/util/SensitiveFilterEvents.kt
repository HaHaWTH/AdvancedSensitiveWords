package io.wdsj.asw.bukkit.util

import io.wdsj.asw.bukkit.api.event.SensitiveFilterPostProcessEvent
import io.wdsj.asw.bukkit.type.ModuleType
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

object SensitiveFilterEvents {
    fun post(
        asynchronous: Boolean,
        moduleType: ModuleType,
        player: Player?,
        originalContent: String,
        censoredWords: List<String>,
    ) {
        post(asynchronous, moduleType, player?.uniqueId, player?.name, originalContent, censoredWords)
    }

    fun post(
        asynchronous: Boolean,
        moduleType: ModuleType,
        playerId: UUID?,
        playerName: String?,
        originalContent: String,
        censoredWords: List<String>,
    ) {
        Bukkit.getPluginManager().callEvent(
            SensitiveFilterPostProcessEvent(
                asynchronous,
                moduleType,
                playerId,
                playerName,
                originalContent,
                censoredWords,
            ),
        )
    }
}
