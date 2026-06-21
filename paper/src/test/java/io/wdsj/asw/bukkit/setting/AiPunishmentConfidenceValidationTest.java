package io.wdsj.asw.bukkit.setting;

import org.junit.jupiter.api.Test;
import io.wdsj.asw.bukkit.ai.LlmApiMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiPunishmentConfidenceValidationTest {
    @Test
    void validatesTheCompletePerCategoryThresholdMap() {
        assertDoesNotThrow(() -> PaperConfigurationService.validateSettings(new SettingsConfiguration()));

        SettingsConfiguration missingCategory = new SettingsConfiguration();
        missingCategory.ai.categoryPolicy.remove("hate");
        assertThrows(IllegalArgumentException.class,
                () -> PaperConfigurationService.validateSettings(missingCategory));

        SettingsConfiguration cleanEnabled = new SettingsConfiguration();
        cleanEnabled.ai.categoryPolicy.get("clean").notifyConfidence = 0.9D;
        assertThrows(IllegalArgumentException.class,
                () -> PaperConfigurationService.validateSettings(cleanEnabled));

        SettingsConfiguration cleanActions = new SettingsConfiguration();
        cleanActions.ai.categoryPolicy.get("clean").punishment = List.of("COMMAND|say unexpected");
        assertThrows(IllegalArgumentException.class,
                () -> PaperConfigurationService.validateSettings(cleanActions));

        SettingsConfiguration missingPunishment = new SettingsConfiguration();
        missingPunishment.ai.categoryPolicy.get("hate").punishment = null;
        assertThrows(IllegalArgumentException.class,
                () -> PaperConfigurationService.validateSettings(missingPunishment));

        SettingsConfiguration invalidThreshold = new SettingsConfiguration();
        invalidThreshold.ai.categoryPolicy.get("profanity").punishConfidence = -0.5D;
        assertThrows(IllegalArgumentException.class,
                () -> PaperConfigurationService.validateSettings(invalidThreshold));

        SettingsConfiguration missingApiMode = new SettingsConfiguration();
        missingApiMode.ai.apiMode = null;
        assertThrows(IllegalArgumentException.class,
                () -> PaperConfigurationService.validateSettings(missingApiMode));

        SettingsConfiguration missingAnthropicVersion = new SettingsConfiguration();
        missingAnthropicVersion.ai.enabled = true;
        missingAnthropicVersion.ai.apiMode = LlmApiMode.ANTHROPIC_MESSAGES;
        missingAnthropicVersion.ai.anthropicVersion = " ";
        assertThrows(IllegalArgumentException.class,
                () -> PaperConfigurationService.validateSettings(missingAnthropicVersion));
    }
}
