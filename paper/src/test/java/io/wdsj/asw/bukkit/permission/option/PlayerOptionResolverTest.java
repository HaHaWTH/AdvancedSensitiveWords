package io.wdsj.asw.bukkit.permission.option;

import io.wdsj.asw.bukkit.type.ProcessMethod;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerOptionResolverTest {
    @Test
    void higherPriorityNumberOverridesInheritedParentValue() {
        List<String> permissions = List.of(
                "advancedsensitivewords.option.chat.anti-spam.history-size.value.8",
                "advancedsensitivewords.option.chat.anti-spam.history-size.value.4.priority.100"
        );

        assertEquals(4, PlayerOptionResolver.resolveInteger(
                PlayerOptions.CHAT_ANTI_SPAM_HISTORY_SIZE,
                6,
                permissions
        ));
    }

    @Test
    void samePriorityNumberUsesMaximumValue() {
        List<String> permissions = List.of(
                "advancedsensitivewords.option.chat.anti-spam.history-size.value.4.priority.20",
                "advancedsensitivewords.option.chat.anti-spam.history-size.value.8.priority.20"
        );

        assertEquals(8, PlayerOptionResolver.resolveInteger(
                PlayerOptions.CHAT_ANTI_SPAM_HISTORY_SIZE,
                6,
                permissions
        ));
    }

    @Test
    void higherPriorityDefaultFallsBackToGlobalNumber() {
        List<String> permissions = List.of(
                "advancedsensitivewords.option.chat.anti-spam.history-size.value.8",
                "advancedsensitivewords.option.chat.anti-spam.history-size.default.priority.100"
        );

        assertEquals(6, PlayerOptionResolver.resolveInteger(
                PlayerOptions.CHAT_ANTI_SPAM_HISTORY_SIZE,
                6,
                permissions
        ));
    }

    @Test
    void booleanDisableWinsAtSamePriority() {
        List<String> permissions = List.of(
                "advancedsensitivewords.option.chat.anti-spam.enabled.enable.priority.10",
                "advancedsensitivewords.option.chat.anti-spam.enabled.disable.priority.10"
        );

        assertFalse(PlayerOptionResolver.resolveBoolean(
                PlayerOptions.CHAT_ANTI_SPAM_ENABLED,
                true,
                permissions
        ));
    }

    @Test
    void higherPriorityEnableBeatsLowerPriorityDisable() {
        List<String> permissions = List.of(
                "advancedsensitivewords.option.chat.anti-spam.enabled.disable",
                "advancedsensitivewords.option.chat.anti-spam.enabled.enable.priority.100"
        );

        assertTrue(PlayerOptionResolver.resolveBoolean(
                PlayerOptions.CHAT_ANTI_SPAM_ENABLED,
                false,
                permissions
        ));
    }

    @Test
    void methodCancelWinsAtSamePriority() {
        List<String> permissions = List.of(
                "advancedsensitivewords.option.chat.method.replace.priority.5",
                "advancedsensitivewords.option.chat.method.cancel.priority.5"
        );

        assertEquals(ProcessMethod.CANCEL, PlayerOptionResolver.resolveMethod(
                PlayerOptions.CHAT_METHOD,
                ProcessMethod.REPLACE,
                permissions
        ));
    }
}
