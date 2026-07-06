package io.wdsj.asw.bukkit.manage.playergroup;

import java.util.Objects;
import java.util.UUID;

public record PlayerGroupActivityState(
        UUID playerUuid,
        String playerNameLower,
        PlayerActivitySnapshot snapshot,
        long updatedAtMillis
) {
    public PlayerGroupActivityState {
        Objects.requireNonNull(playerUuid, "playerUuid");
        Objects.requireNonNull(playerNameLower, "playerNameLower");
        Objects.requireNonNull(snapshot, "snapshot");
    }
}
