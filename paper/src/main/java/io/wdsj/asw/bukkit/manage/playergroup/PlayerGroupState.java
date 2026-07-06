package io.wdsj.asw.bukkit.manage.playergroup;

import java.util.Objects;
import java.util.UUID;

public record PlayerGroupState(
        UUID playerUuid,
        String playerNameLower,
        PlayerGroup group,
        boolean manualOverride,
        long updatedAtMillis,
        UUID updatedByUuid,
        String updatedByName
) {
    public PlayerGroupState {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(playerNameLower, "playerNameLower");
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(updatedByName, "updatedByName");
    }
}
