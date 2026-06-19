package io.wdsj.asw.bukkit.integration.trchat;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.bukkit.entity.Player;

import java.util.UUID;

final class TrChatPendingFakeMessages {
    private static final Object2IntMap<UUID> PENDING_MESSAGES = Object2IntMaps.synchronize(new Object2IntOpenHashMap<>());

    private TrChatPendingFakeMessages() {
    }

    static void increment(Player player) {
        UUID uuid = player.getUniqueId();
        synchronized (PENDING_MESSAGES) {
            PENDING_MESSAGES.put(uuid, PENDING_MESSAGES.getInt(uuid) + 1);
        }
    }

    static boolean hasPending(Player player) {
        return PENDING_MESSAGES.getInt(player.getUniqueId()) > 0;
    }

    static boolean consume(Player player) {
        UUID uuid = player.getUniqueId();
        synchronized (PENDING_MESSAGES) {
            int count = PENDING_MESSAGES.getInt(uuid);
            if (count <= 0) {
                return false;
            }
            if (count == 1) {
                PENDING_MESSAGES.removeInt(uuid);
            } else {
                PENDING_MESSAGES.put(uuid, count - 1);
            }
            return true;
        }
    }
}
