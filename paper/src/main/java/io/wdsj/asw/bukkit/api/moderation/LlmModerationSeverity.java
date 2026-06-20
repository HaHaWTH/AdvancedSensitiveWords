package io.wdsj.asw.bukkit.api.moderation;

import java.util.Arrays;
import java.util.Locale;

/**
 * Severity emitted by the LLM moderation classifier.
 *
 * <p>{@link #NONE} is valid only for a {@link LlmModerationCategory#CLEAN clean} result. All other
 * categories require a non-none severity.</p>
 */
public enum LlmModerationSeverity {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    /** @return lower-case provider/configuration identifier for this severity */
    public String wireName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Resolves a lower-case provider severity identifier.
     *
     * @param value exact lower-case identifier
     * @return matching severity
     * @throws IllegalArgumentException when the identifier is unknown
     */
    public static LlmModerationSeverity fromWireName(String value) {
        return Arrays.stream(values())
                .filter(severity -> severity.wireName().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown LLM moderation severity: " + value));
    }
}
