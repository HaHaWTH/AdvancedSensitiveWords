package io.wdsj.asw.bukkit.api.moderation;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Immutable, locally validated LLM classification response for one chat message.
 *
 * <p>Validates the category, severity, confidence, secondary categories, signal limits, and explanation
 * length before constructing this record. {@code signals} and {@code explanation} remain untrusted provider
 * text: they are useful for third-party review but must never be treated as commands or trusted instructions.</p>
 *
 * @param category primary moderation category
 * @param secondaryCategories distinct non-primary categories
 * @param confidence provider confidence from {@code 0.0} through {@code 1.0}
 * @param severity moderation severity consistent with {@code category}
 * @param signals up to five short, generic classification signals
 * @param explanation concise provider explanation with a maximum of 180 Unicode code points
 */
public record LlmChatModerationResult(
        LlmModerationCategory category,
        List<LlmModerationCategory> secondaryCategories,
        double confidence,
        LlmModerationSeverity severity,
        List<String> signals,
        String explanation
) {
    public static final int MAX_SIGNALS = 5;
    public static final int MAX_SIGNAL_CODE_POINTS = 96;
    public static final int MAX_EXPLANATION_CODE_POINTS = 180;

    public LlmChatModerationResult {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(secondaryCategories, "secondaryCategories");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(signals, "signals");
        Objects.requireNonNull(explanation, "explanation");

        secondaryCategories = List.copyOf(secondaryCategories);
        signals = List.copyOf(signals);
        validate(category, secondaryCategories, confidence, severity, signals, explanation);
    }

    private static void validate(
            LlmModerationCategory category,
            List<LlmModerationCategory> secondaryCategories,
            double confidence,
            LlmModerationSeverity severity,
            List<String> signals,
            String explanation
    ) {
        if (!Double.isFinite(confidence) || confidence < 0.0D || confidence > 1.0D) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
        if (secondaryCategories.stream().anyMatch(Objects::isNull) || secondaryCategories.contains(category)
                || new HashSet<>(secondaryCategories).size() != secondaryCategories.size()) {
            throw new IllegalArgumentException("secondary categories must be unique and exclude the primary category");
        }
        if (category == LlmModerationCategory.CLEAN) {
            if (severity != LlmModerationSeverity.NONE || !secondaryCategories.isEmpty()) {
                throw new IllegalArgumentException("clean results require severity none and no secondary categories");
            }
        } else if (severity == LlmModerationSeverity.NONE) {
            throw new IllegalArgumentException("non-clean results require a non-none severity");
        }
        if (signals.size() > MAX_SIGNALS) {
            throw new IllegalArgumentException("too many signals");
        }
        for (String signal : signals) {
            if (signal == null || signal.isBlank() || signal.codePointCount(0, signal.length()) > MAX_SIGNAL_CODE_POINTS) {
                throw new IllegalArgumentException("invalid signal");
            }
        }
        if (explanation.isBlank() || explanation.codePointCount(0, explanation.length()) > MAX_EXPLANATION_CODE_POINTS) {
            throw new IllegalArgumentException("invalid explanation");
        }
    }
}
