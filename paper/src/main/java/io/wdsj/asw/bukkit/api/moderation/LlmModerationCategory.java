package io.wdsj.asw.bukkit.api.moderation;

import java.util.Arrays;
import java.util.Locale;

/**
 * Categories emitted by the LLM moderation classifier.
 *
 * <p>The lower-case {@linkplain #wireName() wire name} is used in configuration and provider JSON. Only
 * categories for which {@link #isAutomaticallyEnforceable()} returns {@code true} may be enabled in ASW's
 * {@code ai.category-policy} enforcement configuration.</p>
 */
public enum LlmModerationCategory {
    CLEAN,
    PROFANITY,
    HARASSMENT,
    HATE,
    SEXUAL,
    SEXUAL_MINORS,
    SELF_HARM,
    VIOLENCE_THREAT,
    ILLEGAL,
    PRIVACY_DOXXING,
    SPAM_SCAM,
    PROMPT_INJECTION;

    /**
     * @return lower-case provider/configuration identifier for this category
     */
    public String wireName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * @return kebab-case configuration identifier for this category
     */
    public String configurationKey() {
        return wireName().replace('_', '-');
    }

    /**
     * @return whether ASW permits automatic enforcement for this category
     */
    public boolean isAutomaticallyEnforceable() {
        return switch (this) {
            case CLEAN -> false;
            default -> true;
        };
    }

    /**
     * Resolves a lower-case provider/configuration category identifier.
     *
     * @param value exact lower-case identifier
     * @return matching category
     * @throws IllegalArgumentException when the identifier is unknown
     */
    public static LlmModerationCategory fromWireName(String value) {
        return Arrays.stream(values())
                .filter(category -> category.wireName().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown LLM moderation category: " + value));
    }
}
