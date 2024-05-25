package io.wdsj.asw.bukkit.util;

import org.bukkit.entity.Player;

import static io.wdsj.asw.bukkit.util.Utils.isAnyClassLoaded;

public class PlayerUtils {
    private static final boolean isLeavesServer = isAnyClassLoaded("top.leavesmc.leaves.LeavesConfig", "org.leavesmc.leaves.LeavesConfig");

    public static boolean isNpc(Player player) {
        if (isLeavesServer) {
            return player.getAddress() == null || player.hasMetadata("NPC");
        } else {
            return player.hasMetadata("NPC");
        }
    }

    private PlayerUtils() {}
}
