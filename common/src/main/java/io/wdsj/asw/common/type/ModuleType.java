package io.wdsj.asw.common.type;

import java.util.Arrays;
import java.util.List;

/**
 * Types for different detection modules.
 */
public enum ModuleType {
    CHAT(true),
    AI(true),
    SIGN(true),
    ANVIL(true),
    BOOK(true),
    NAME(false),
    ITEM(true),
    BROADCAST(false);

    private final boolean violationTracked;

    ModuleType(boolean violationTracked) {
        this.violationTracked = violationTracked;
    }

    public boolean isViolationTracked() {
        return violationTracked;
    }

    public static List<ModuleType> violationModules() {
        return Arrays.stream(values())
                .filter(ModuleType::isViolationTracked)
                .toList();
    }

    public static ModuleType parseViolationModule(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(ModuleType::isViolationTracked)
                .filter(moduleType -> moduleType.name().equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }
}
