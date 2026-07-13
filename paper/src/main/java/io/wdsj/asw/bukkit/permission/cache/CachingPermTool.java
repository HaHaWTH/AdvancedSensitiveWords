package io.wdsj.asw.bukkit.permission.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.wdsj.asw.bukkit.permission.PermissionsEnum;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

// Taken from: xGinko/AnarchyExploitFixes
public final class CachingPermTool implements Listener {

    private static final Map<UUID, Cache<PermissionsEnum, Boolean>> permissionCacheMap = new ConcurrentHashMap<>();
    private static final Map<UUID, Cache<String, Boolean>> stringPermissionCacheMap = new ConcurrentHashMap<>();
    private static final Map<UUID, Cache<String, List<String>>> effectivePermissionCacheMap = new ConcurrentHashMap<>();

    CachingPermTool(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static CachingPermTool enable(JavaPlugin plugin) {
        return new CachingPermTool(plugin);
    }

    public void disable() {
        HandlerList.unregisterAll(this);
        for (Map.Entry<UUID, Cache<PermissionsEnum, Boolean>> entry : permissionCacheMap.entrySet()) {
            entry.getValue().cleanUp();
        }
        permissionCacheMap.clear();
        stringPermissionCacheMap.clear();
        effectivePermissionCacheMap.clear();
    }

    public static boolean hasPermission(PermissionsEnum permission, @NotNull HumanEntity human) {
        Cache<PermissionsEnum, Boolean> permCache = permissionCacheMap.computeIfAbsent(human.getUniqueId(),
                k -> CacheBuilder.newBuilder().expireAfterWrite(8, TimeUnit.SECONDS).build());
        Boolean hasPermission = permCache.getIfPresent(permission);
        if (hasPermission == null) {
            hasPermission = human.hasPermission(permission.getPermission());
            permCache.put(permission, hasPermission);
        }
        return hasPermission;
    }

    public static boolean hasPermission(@NotNull String permission, @NotNull HumanEntity human) {
        Cache<String, Boolean> permCache = stringPermissionCacheMap.computeIfAbsent(human.getUniqueId(),
                k -> CacheBuilder.newBuilder().expireAfterWrite(8, TimeUnit.SECONDS).build());
        String normalizedPermission = permission.toLowerCase(Locale.ROOT);
        Boolean hasPermission = permCache.getIfPresent(normalizedPermission);
        if (hasPermission == null) {
            hasPermission = human.hasPermission(permission);
            permCache.put(normalizedPermission, hasPermission);
        }
        return hasPermission;
    }

    public static List<String> effectivePermissions(@NotNull HumanEntity human) {
        Cache<String, List<String>> permCache = effectivePermissionCacheMap.computeIfAbsent(human.getUniqueId(),
                k -> CacheBuilder.newBuilder().expireAfterWrite(8, TimeUnit.SECONDS).build());
        List<String> permissions = permCache.getIfPresent("effective");
        if (permissions == null) {
            List<String> collected = new ArrayList<>();
            for (PermissionAttachmentInfo permission : human.getEffectivePermissions()) {
                if (permission.getValue()) {
                    collected.add(permission.getPermission().toLowerCase(Locale.ROOT));
                }
            }
            permissions = List.copyOf(collected);
            permCache.put("effective", permissions);
        }
        return permissions;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onQuit(PlayerQuitEvent event) {
        permissionCacheMap.remove(event.getPlayer().getUniqueId());
        stringPermissionCacheMap.remove(event.getPlayer().getUniqueId());
        effectivePermissionCacheMap.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onKick(PlayerKickEvent event) {
        permissionCacheMap.remove(event.getPlayer().getUniqueId());
        stringPermissionCacheMap.remove(event.getPlayer().getUniqueId());
        effectivePermissionCacheMap.remove(event.getPlayer().getUniqueId());
    }
}
