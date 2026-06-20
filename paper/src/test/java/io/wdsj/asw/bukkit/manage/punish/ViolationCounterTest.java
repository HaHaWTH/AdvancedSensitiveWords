package io.wdsj.asw.bukkit.manage.punish;

import io.wdsj.asw.bukkit.type.ModuleType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ViolationCounterTest {
    @AfterEach
    void resetCounters() {
        ViolationCounter.INSTANCE.resetAllViolations();
    }

    @Test
    void keepsViolationCountsSeparatedByModuleAndProvidesTotals() {
        UUID playerId = UUID.randomUUID();
        ViolationCounter.INSTANCE.incrementViolationCount(playerId, ModuleType.CHAT, 2L);
        ViolationCounter.INSTANCE.incrementViolationCount(playerId, ModuleType.BOOK, 1L);
        ViolationCounter.INSTANCE.incrementViolationCount(playerId, ModuleType.AI, 3L);

        assertEquals(2L, ViolationCounter.INSTANCE.getViolationCount(playerId, ModuleType.CHAT));
        assertEquals(1L, ViolationCounter.INSTANCE.getViolationCount(playerId, ModuleType.BOOK));
        assertEquals(3L, ViolationCounter.INSTANCE.getViolationCount(playerId, ModuleType.AI));
        assertEquals(0L, ViolationCounter.INSTANCE.getViolationCount(playerId, ModuleType.SIGN));
        assertEquals(6L, ViolationCounter.INSTANCE.getTotalViolationCount(playerId));
    }

    @Test
    void resetsOnlyTheRequestedModuleOrAllModules() {
        UUID playerId = UUID.randomUUID();
        ViolationCounter.INSTANCE.incrementViolationCount(playerId, ModuleType.CHAT, 2L);
        ViolationCounter.INSTANCE.incrementViolationCount(playerId, ModuleType.AI, 1L);

        ViolationCounter.INSTANCE.resetViolationCount(playerId, ModuleType.CHAT);

        assertEquals(0L, ViolationCounter.INSTANCE.getViolationCount(playerId, ModuleType.CHAT));
        assertEquals(1L, ViolationCounter.INSTANCE.getViolationCount(playerId, ModuleType.AI));
        assertEquals(1L, ViolationCounter.INSTANCE.getTotalViolationCount(playerId));

        ViolationCounter.INSTANCE.resetViolationCount(playerId);

        assertEquals(0L, ViolationCounter.INSTANCE.getTotalViolationCount(playerId));
    }

    @Test
    void excludesNonViolationModulesFromCounterOperationsAndSuggestions() {
        assertTrue(ModuleType.violationModules().contains(ModuleType.CHAT));
        assertTrue(ModuleType.violationModules().contains(ModuleType.AI));
        assertFalse(ModuleType.violationModules().contains(ModuleType.NAME));
        assertEquals(ModuleType.SIGN, ModuleType.parseViolationModule("sign"));
        assertNull(ModuleType.parseViolationModule("name"));
        assertThrows(IllegalArgumentException.class,
                () -> ViolationCounter.INSTANCE.incrementViolationCount(UUID.randomUUID(), ModuleType.NAME, 1L));
    }
}
