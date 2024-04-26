package io.wdsj.asw.bukkit.manage.placeholder;

import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
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
        return AdvancedSensitiveWords.getInstance().getDescription().getVersion();
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
        if (params.equalsIgnoreCase("total")) {
            return String.valueOf(messagesFilteredNum);
        }
        return null;
    }
}
