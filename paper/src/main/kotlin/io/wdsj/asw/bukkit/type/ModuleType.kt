package io.wdsj.asw.bukkit.type

/**
 * Types for different detection modules.
 */
enum class ModuleType(val isViolationTracked: Boolean) {
    CHAT(true),
    AI(true),
    SIGN(true),
    ANVIL(true),
    BOOK(true),
    NAME(false),
    ITEM(true);

    companion object {
        @JvmStatic
        fun violationModules(): List<ModuleType> = entries.filter(ModuleType::isViolationTracked)

        @JvmStatic
        fun parseViolationModule(value: String): ModuleType? =
            entries.firstOrNull {
                it.isViolationTracked && it.name.equals(value, ignoreCase = true)
            }
    }
}
