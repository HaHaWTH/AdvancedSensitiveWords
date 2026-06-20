package io.wdsj.asw.bukkit.setting;

import org.junit.jupiter.api.Test;

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

        SettingsConfiguration invalidThreshold = new SettingsConfiguration();
        invalidThreshold.ai.categoryPolicy.get("profanity").punishConfidence = -0.5D;
        assertThrows(IllegalArgumentException.class,
                () -> PaperConfigurationService.validateSettings(invalidThreshold));
    }
}
