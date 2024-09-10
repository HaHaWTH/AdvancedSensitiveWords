package io.wdsj.asw.bukkit.manage.permission.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

// Taken from: xGinko/AnarchyExploitFixes
public final class CachingPermTool implements Listener {

    private static final Map<UUID, Cache<String, Boolean>> permissionCacheMap = new ConcurrentHashMap<>();

    CachingPermTool(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static CachingPermTool enable(JavaPlugin plugin) {
        return new CachingPermTool(plugin);
    }

    public void disable() {
        HandlerList.unregisterAll(this);
        for (Map.Entry<UUID, Cache<String, Boolean>> entry : permissionCacheMap.entrySet())
            entry.getValue().cleanUp();
        permissionCacheMap.clear();
    }

    public static boolean hasPermission(String permission, HumanEntity human) {
        Cache<String, Boolean> permCache = permissionCacheMap.computeIfAbsent(human.getUniqueId(),
                k -> CacheBuilder.newBuilder().expireAfterWrite(8, TimeUnit.SECONDS).build());
        Boolean hasPermission = permCache.getIfPresent(permission);
        if (hasPermission == null) {
            hasPermission = human.hasPermission(permission);
            permCache.put(permission, hasPermission);
        }
        return hasPermission;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onQuit(PlayerQuitEvent event) {
        permissionCacheMap.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onKick(PlayerKickEvent event) {
        permissionCacheMap.remove(event.getPlayer().getUniqueId());
    }
}
