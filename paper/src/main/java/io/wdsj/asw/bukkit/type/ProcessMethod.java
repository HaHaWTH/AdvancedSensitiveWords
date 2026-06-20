package io.wdsj.asw.bukkit.type;

/**
 * Defines how a filter reacts after finding blocked content.
 */
public enum ProcessMethod {
    REPLACE,
    CANCEL;

    public boolean isReplace() {
        return this == REPLACE;
    }

    public boolean isCancel() {
        return this == CANCEL;
    }
}
