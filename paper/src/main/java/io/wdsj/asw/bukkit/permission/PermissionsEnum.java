package io.wdsj.asw.bukkit.permission;

/**
 * Permission enums
 */
public enum PermissionsEnum {
    BYPASS("bypass"),
    BYPASS_ALL("bypass.*"),
    BYPASS_CHAT("bypass.chat"),
    BYPASS_COMMAND("bypass.command"),
    BYPASS_BOOK("bypass.book"),
    BYPASS_SIGN("bypass.sign"),
    BYPASS_ANVIL("bypass.anvil"),
    BYPASS_ITEM("bypass.item"),
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
