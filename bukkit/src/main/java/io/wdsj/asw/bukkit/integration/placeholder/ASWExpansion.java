package io.wdsj.asw.bukkit.integration.placeholder;

import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.manage.punish.PlayerShadowController;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static io.wdsj.asw.bukkit.util.Utils.messagesFilteredNum;

public class ASWExpansion extends PlaceholderExpansion {
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
        return AdvancedSensitiveWords.PLUGIN_VERSION;
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
