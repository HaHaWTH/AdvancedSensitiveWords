package io.wdsj.asw.bukkit.manage.punish;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.wdsj.asw.bukkit.util.Utils;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerAltController {
    private static final Multimap<String, UUID> PLAYER_ALTS = HashMultimap.create();

    public static void addToAlts(String ip, Player player) {
        PLAYER_ALTS.put(ip, player.getUniqueId());
    }

    public static void removeFromAlts(String ip, Player player) {
        PLAYER_ALTS.remove(ip, player.getUniqueId());
    }

    public static boolean hasAlt(Player player) {
        String ip = Utils.getPlayerIp(player);
        return PLAYER_ALTS.get(ip).size() > 1;
    }

    public static Collection<UUID> getAlts(Player player) {
        String ip = Utils.getPlayerIp(player);
        if (!PLAYER_ALTS.containsKey(ip)) {
            return Collections.emptyList();
        }
        return PLAYER_ALTS.get(ip).stream()
                .filter(uuid -> !uuid.equals(player.getUniqueId()))
                .collect(Collectors.toList());
    }

    public static boolean contains(String ip, Player player) {
        return PLAYER_ALTS.get(ip).contains(player.getUniqueId());
    }

    public static void clear() {
        PLAYER_ALTS.clear();
    }
}
