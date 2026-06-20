package io.wdsj.asw.bukkit.util

import org.bukkit.entity.Player

object PlayerUtils {
    private val isLeavesServer =
        Utils.isAnyClassLoaded("top.leavesmc.leaves.LeavesConfig", "org.leavesmc.leaves.LeavesConfig")

    fun isNpc(player: Player): Boolean {
        return if (isLeavesServer) {
            player.address == null || player.hasMetadata("NPC")
        } else {
            player.hasMetadata("NPC")
        }
    }
}
