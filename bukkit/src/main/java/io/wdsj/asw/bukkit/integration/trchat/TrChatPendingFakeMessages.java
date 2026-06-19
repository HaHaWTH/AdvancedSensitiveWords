package io.wdsj.asw.bukkit.integration.trchat;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.bukkit.entity.Player;

import java.util.UUID;

final class TrChatPendingFakeMessages {
    private static final Object LOCK = new Object();
    private static final Object2IntOpenHashMap<UUID> PENDING_MESSAGES = new Object2IntOpenHashMap<>();

    private TrChatPendingFakeMessages() {
    }

    static void increment(Player player) {
        synchronized (LOCK) {
            PENDING_MESSAGES.addTo(player.getUniqueId(), 1);
        }
    }

    static boolean hasPending(Player player) {
        synchronized (LOCK) {
            return PENDING_MESSAGES.getInt(player.getUniqueId()) > 0;
        }
    }

    static boolean consume(Player player) {
        UUID uuid = player.getUniqueId();
        synchronized (LOCK) {
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
