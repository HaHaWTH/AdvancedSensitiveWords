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

    /**
     * Adds a player to the alts list.
     * @param ip The player's IP address.
     * @param player The player
     */
    public static void addToAlts(String ip, Player player) {
        PLAYER_ALTS.put(ip, player.getUniqueId());
    }

    /**
     * Removes a player from the alts list.
     * @param ip The player's IP address.
     * @param player The player
     */
    public static void removeFromAlts(String ip, Player player) {
        PLAYER_ALTS.remove(ip, player.getUniqueId());
    }

    /**
     * Checks if a player has an alt.
     * @param player The player
     * @return True if the player has an alt, false otherwise.
     */
    public static boolean hasAlt(Player player) {
        String ip = Utils.getPlayerIp(player);
        return PLAYER_ALTS.get(ip).size() > 1;
    }

    /**
     * Gets a list of alts for a player(everyone except the player itself).
     * @param player The player
     * @return A list of alts for the player.
     */
    public static Collection<UUID> getAlts(Player player) {
        String ip = Utils.getPlayerIp(player);
        if (!PLAYER_ALTS.containsKey(ip)) {
            return Collections.emptyList();
        }
        return PLAYER_ALTS.get(ip).stream()
                .filter(uuid -> !uuid.equals(player.getUniqueId()))
                .collect(Collectors.toList());
    }

    /**
     * Checks if a player is in the alts list.
     * @param ip The player's IP address.
     * @param player The player
     * @return true if the player is in the alts list, false otherwise.
     */
    public static boolean contains(String ip, Player player) {
        return PLAYER_ALTS.get(ip).contains(player.getUniqueId());
    }

    /**
     * Clears the alts list.
     */
    public static void clear() {
        PLAYER_ALTS.clear();
    }
}
