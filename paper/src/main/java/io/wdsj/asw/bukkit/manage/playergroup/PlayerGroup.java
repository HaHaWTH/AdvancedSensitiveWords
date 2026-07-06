package io.wdsj.asw.bukkit.manage.playergroup;

import java.util.Locale;

public enum PlayerGroup {
    NEWBIE,
    PLAYER;

    public static PlayerGroup parse(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
