package io.wdsj.asw.bukkit.api.moderation;

import java.util.Arrays;
import java.util.Locale;

/** Severity emitted by the LLM moderation classifier. */
public enum LlmModerationSeverity {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public String wireName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static LlmModerationSeverity fromWireName(String value) {
        return Arrays.stream(values())
                .filter(severity -> severity.wireName().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown LLM moderation severity: " + value));
    }
}
