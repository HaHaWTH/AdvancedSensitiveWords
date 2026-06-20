package io.wdsj.asw.bukkit.api.moderation;

import java.util.Arrays;
import java.util.Locale;

/**
 * Categories emitted by the LLM moderation classifier.
 *
 * <p>The lower-case {@linkplain #wireName() wire name} is used in configuration and provider JSON. Only
 * categories for which {@link #isAutomaticallyEnforceable()} returns {@code true} may appear in ASW's
 * automatic AI enforcement configuration.</p>
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

    /** @return lower-case provider/configuration identifier for this category */
    public String wireName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * @return whether ASW permits this category in {@code ai.enforced-categories}; profanity and prompt
     * injection remain event-only classifications
     */
    public boolean isAutomaticallyEnforceable() {
        return switch (this) {
            case HARASSMENT, HATE, SEXUAL, SEXUAL_MINORS, SELF_HARM, VIOLENCE_THREAT,
                    ILLEGAL, PRIVACY_DOXXING, SPAM_SCAM -> true;
            case CLEAN, PROFANITY, PROMPT_INJECTION -> false;
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
