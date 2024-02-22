package io.wdsj.asw.util;

import org.bukkit.entity.Player;

public class PlayerUtils {
    private static boolean isLeavesServer;
    static {
        try {
            Class.forName("top.leavesmc.leaves.LeavesConfig");
            isLeavesServer = true;
        } catch (ClassNotFoundException e) {
            isLeavesServer = false;
        }
    }

    public static boolean isNpc(Player player) {
        if (isLeavesServer) {
            return player.getAddress() == null || player.hasMetadata("NPC");
        } else {
            return player.hasMetadata("NPC");
        }
    }

    private PlayerUtils() {}
}
