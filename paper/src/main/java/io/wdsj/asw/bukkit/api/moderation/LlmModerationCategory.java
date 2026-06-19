package io.wdsj.asw.bukkit.api.moderation;

import java.util.Arrays;
import java.util.Locale;

/** Categories emitted by the LLM moderation classifier. */
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

    public String wireName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean isAutomaticallyEnforceable() {
        return switch (this) {
            case HARASSMENT, HATE, SEXUAL, SEXUAL_MINORS, SELF_HARM, VIOLENCE_THREAT,
                    ILLEGAL, PRIVACY_DOXXING, SPAM_SCAM -> true;
            case CLEAN, PROFANITY, PROMPT_INJECTION -> false;
        };
    }

    public static LlmModerationCategory fromWireName(String value) {
        return Arrays.stream(values())
                .filter(category -> category.wireName().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown LLM moderation category: " + value));
    }
}
