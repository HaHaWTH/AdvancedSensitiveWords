package io.wdsj.asw.bukkit.ai;

import io.wdsj.asw.bukkit.api.moderation.LlmChatModerationResult;
import io.wdsj.asw.bukkit.api.moderation.LlmModerationCategory;
import io.wdsj.asw.bukkit.api.moderation.LlmModerationSeverity;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmPunishmentThresholdTest {
    @Test
    void appliesThresholdsPerCategoryAndTreatsNegativeOneAsDisabled() {
        Map<LlmModerationCategory, LlmCategoryPolicy> policies = new EnumMap<>(LlmModerationCategory.class);
        for (LlmModerationCategory category : LlmModerationCategory.values()) {
            policies.put(category, new LlmCategoryPolicy(-1.0D, -1.0D, List.of()));
        }
        policies.put(LlmModerationCategory.PROFANITY, new LlmCategoryPolicy(0.60D, 0.75D, List.of("DAMAGE|1")));
        policies.put(LlmModerationCategory.PROMPT_INJECTION, new LlmCategoryPolicy(-1.0D, 0.50D, List.of("COMMAND|kick %player%")));
        policies.put(LlmModerationCategory.HATE, new LlmCategoryPolicy(0.90D, 0.80D, List.of("HOSTILE|5")));

        assertFalse(LlmChatDetectionService.isEligibleForPunishment(
                policies, result(LlmModerationCategory.CLEAN, 1.0D, LlmModerationSeverity.NONE)));
        assertFalse(LlmChatDetectionService.isEligibleForPunishment(
                policies, result(LlmModerationCategory.PROFANITY, 0.74D, LlmModerationSeverity.LOW)));
        assertTrue(LlmChatDetectionService.isEligibleForPunishment(
                policies, result(LlmModerationCategory.PROFANITY, 0.75D, LlmModerationSeverity.LOW)));
        assertTrue(LlmChatDetectionService.isEligibleForPunishment(
                policies, result(LlmModerationCategory.PROMPT_INJECTION, 0.50D, LlmModerationSeverity.MEDIUM)));
        assertFalse(LlmChatDetectionService.isEligibleForPunishment(
                policies, result(LlmModerationCategory.SEXUAL, 1.0D, LlmModerationSeverity.HIGH)));

        assertTrue(LlmChatDetectionService.isEligibleForNotification(
                policies, result(LlmModerationCategory.PROFANITY, 0.60D, LlmModerationSeverity.LOW)));
        assertFalse(LlmChatDetectionService.isEligibleForNotification(
                policies, result(LlmModerationCategory.PROFANITY, 0.59D, LlmModerationSeverity.LOW)));
        assertFalse(LlmChatDetectionService.isEligibleForNotification(
                policies, result(LlmModerationCategory.PROMPT_INJECTION, 1.0D, LlmModerationSeverity.MEDIUM)));
        assertTrue(LlmChatDetectionService.isEligibleForPunishment(
                policies, result(LlmModerationCategory.HATE, 0.80D, LlmModerationSeverity.HIGH)));
        assertFalse(LlmChatDetectionService.isEligibleForNotification(
                policies, result(LlmModerationCategory.HATE, 0.80D, LlmModerationSeverity.HIGH)));
        assertEquals(List.of("DAMAGE|1"), policies.get(LlmModerationCategory.PROFANITY).punishmentActions());
        assertEquals(List.of("HOSTILE|5"), policies.get(LlmModerationCategory.HATE).punishmentActions());
    }

    private static LlmChatModerationResult result(
            LlmModerationCategory category,
            double confidence,
            LlmModerationSeverity severity
    ) {
        return new LlmChatModerationResult(category, List.of(), confidence, severity,
                List.of("test signal"), "test explanation");
    }
}
