package io.wdsj.asw.bukkit.integration.packetevents.sign;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

record BlockKey(UUID worldId, int x, int y, int z) {
    static BlockKey of(Location location) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Location has no world");
        }
        return new BlockKey(world.getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    int chunkX() {
        return x >> 4;
    }

    int chunkZ() {
        return z >> 4;
    }
}
