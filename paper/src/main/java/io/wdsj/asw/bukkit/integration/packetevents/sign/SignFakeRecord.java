package io.wdsj.asw.bukkit.integration.packetevents.sign;

import net.kyori.adventure.text.Component;
import org.bukkit.block.sign.Side;

import java.util.UUID;

final class SignFakeRecord {
    private final UUID placer;
    private final Side side;
    private final Component[] originalLines;
    private final Component[] blankLines;
    private final long createdAtMillis;

    SignFakeRecord(
            UUID placer,
            Side side,
            Component[] originalLines,
            Component[] blankLines,
            long createdAtMillis
    ) {
        this.placer = placer;
        this.side = side;
        this.originalLines = originalLines.clone();
        this.blankLines = blankLines.clone();
        this.createdAtMillis = createdAtMillis;
    }

    UUID placer() {
        return placer;
    }

    Side side() {
        return side;
    }

    Component[] visibleLines(UUID viewer) {
        return canSeeOriginal(viewer) ? originalLines.clone() : blankLines.clone();
    }

    long createdAtMillis() {
        return createdAtMillis;
    }

    /**
     * Whether the viewer can see the original lines
     * @param viewer the viewer
     * @return true if the viewer can see the original lines, false otherwise
     */
    boolean canSeeOriginal(UUID viewer) {
        return placer.equals(viewer);
    }
}
