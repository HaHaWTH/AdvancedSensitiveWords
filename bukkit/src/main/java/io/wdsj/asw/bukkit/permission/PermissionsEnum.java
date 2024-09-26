package io.wdsj.asw.bukkit.permission;

/**
 * Permission enums
 */
public enum PermissionsEnum {
    BYPASS("advancedsensitivewords.bypass"),
    RELOAD("advancedsensitivewords.reload"),
    ADD("advancedsensitivewords.add"),
    REMOVE("advancedsensitivewords.remove"),
    STATUS("advancedsensitivewords.status"),
    TEST("advancedsensitivewords.test"),
    HELP("advancedsensitivewords.help"),
    NOTICE("advancedsensitivewords.notice"),
    INFO("advancedsensitivewords.info"),
    RESET("advancedsensitivewords.reset"),
    UPDATE("advancedsensitivewords.update"),
    PUNISH("advancedsensitivewords.punish");

    private final String permission;

    PermissionsEnum(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }
}
