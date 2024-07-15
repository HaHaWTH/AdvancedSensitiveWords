package io.wdsj.asw.bukkit.integration.placeholder;

import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.manage.punish.PlayerShadowController;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.databaseManager;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;
import static io.wdsj.asw.bukkit.util.Utils.messagesFilteredNum;

public class ASWExpansion extends PlaceholderExpansion {
    private long databaseCachedTotal = 0L;
    private long databaseTotalLastRequestTime = System.currentTimeMillis();
    private final AdvancedSensitiveWords plugin = AdvancedSensitiveWords.getInstance();
    @Override
    public @NotNull String getIdentifier() {
        return "asw";
    }

    @Override
    public @NotNull String getAuthor() {
        return "HaHaWTH";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("version")) {
            return getVersion();
        }
        if (params.equalsIgnoreCase("current_total")) {
            return String.valueOf(messagesFilteredNum);
        }
        if (params.equalsIgnoreCase("database_total")) {
            if (settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                if ((System.currentTimeMillis() - databaseTotalLastRequestTime) / 1000 > settingsManager.getProperty(PluginSettings.DATABASE_CACHE_TIME)) {
                    databaseTotalLastRequestTime = System.currentTimeMillis();
                    long total = databaseManager.getTotalViolations();
                    databaseCachedTotal = total;
                    return String.valueOf(total);
                } else {
                    return String.valueOf(databaseCachedTotal);
                }
            } else {
                return "disabled";
            }
        }
        if (params.equalsIgnoreCase("player_database_total")) {
            if (player != null) {
                if (settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                    String playerName = player.getName();
                    if (playerName == null) return "";
                    return String.valueOf(databaseManager.getPlayerViolations(playerName));
                } else {
                    return "disabled";
                }
            }
        }
        if (params.equalsIgnoreCase("is_shadow")) {
            if (player != null) {
                Player onlinePlayer = player.getPlayer();
                if (onlinePlayer != null) {
                    return String.valueOf(PlayerShadowController.isShadowed(onlinePlayer));
                }
            }
        }
        return null;
    }
}
