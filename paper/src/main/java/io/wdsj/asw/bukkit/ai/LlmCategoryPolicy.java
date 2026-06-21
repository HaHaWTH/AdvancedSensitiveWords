package io.wdsj.asw.bukkit.ai;

import io.wdsj.asw.bukkit.api.moderation.LlmChatModerationResult;

import java.util.List;

/** Immutable notification and enforcement thresholds for one LLM moderation category. */
public record LlmCategoryPolicy(
        double notifyConfidence,
        double punishConfidence,
        List<String> punishmentActions
) {
    public LlmCategoryPolicy {
        punishmentActions = List.copyOf(punishmentActions);
    }

    public boolean shouldNotify(LlmChatModerationResult result) {
        return notifyConfidence >= 0.0D && result.confidence() >= notifyConfidence;
    }

    public boolean shouldPunish(LlmChatModerationResult result) {
        return punishConfidence >= 0.0D && result.confidence() >= punishConfidence;
    }
}
