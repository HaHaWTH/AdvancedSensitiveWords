package io.wdsj.asw.bukkit.setting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandWhitelistInversionTest {
    @Test
    void preservesTheExistingWhitelistInversionSemantics() {
        assertTrue(PaperConfigurationService.shouldInspectCommand(true, true));
        assertFalse(PaperConfigurationService.shouldInspectCommand(false, true));
        assertFalse(PaperConfigurationService.shouldInspectCommand(true, false));
        assertTrue(PaperConfigurationService.shouldInspectCommand(false, false));
    }
}
