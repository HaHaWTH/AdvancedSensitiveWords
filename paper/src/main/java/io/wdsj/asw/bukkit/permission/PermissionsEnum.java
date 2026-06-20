package io.wdsj.asw.bukkit.permission;

/**
 * Permission enums
 */
public enum PermissionsEnum {
    BYPASS("bypass"),
    NOTICE("notice"),
    UPDATE("update");

    private final String permission;

    PermissionsEnum(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return PREFIX + permission;
    }

    private static final String PREFIX = "advancedsensitivewords.";
}
