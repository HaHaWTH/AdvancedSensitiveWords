package io.wdsj.asw.bukkit.manage.punish;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum PunishmentType {
    COMMAND,

    COMMAND_PROXY,

    HOSTILE,

    DAMAGE,

    EFFECT,
    
    SHADOW;

    @Nullable
    public static PunishmentType getType(String type) {
        try {
            return valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
