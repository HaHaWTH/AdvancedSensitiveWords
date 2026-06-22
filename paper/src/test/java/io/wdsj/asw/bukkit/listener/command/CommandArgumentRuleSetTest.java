package io.wdsj.asw.bukkit.listener.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandArgumentRuleSetTest {
    @Test
    void tellRuleSkipsTheTargetAndKeepsTheMessage() {
        CommandArgumentRuleSet rules = CommandArgumentRuleSet.compile(List.of(
                "[default:include] /tell [ignore:1]"
        ));

        CommandArgumentRuleSet.CommandSelection selection = rules.select("/tell Player blocked message");
        assertTrue(selection.listed());
        assertEquals(List.of("blocked message"), selection.segments().stream()
                .map(CommandArgumentRuleSet.SelectedSegment::content)
                .toList());
        assertEquals("/tell Player FILTERED", selection.replaceSelected(ignored -> "FILTERED"));
    }

    @Test
    void broadcastRuleSkipsTheServerAndFinalCount() {
        CommandArgumentRuleSet rules = CommandArgumentRuleSet.compile(List.of(
                "[default:include] /bc [ignore:1,-1]"
        ));

        CommandArgumentRuleSet.CommandSelection selection = rules.select("/bc hub blocked message 3");
        assertEquals("blocked message", selection.scannedContent());
        assertEquals("/bc hub FILTERED 3", selection.replaceSelected(ignored -> "FILTERED"));
    }

    @Test
    void ignoredArgumentsSplitDetectionSegments() {
        CommandArgumentRuleSet rules = CommandArgumentRuleSet.compile(List.of(
                "[default:include] /example [ignore:2]"
        ));

        CommandArgumentRuleSet.CommandSelection selection = rules.select("/example left skipped right");
        assertEquals(List.of("left", "right"), selection.segments().stream()
                .map(CommandArgumentRuleSet.SelectedSegment::content)
                .toList());
        assertEquals("left\nright", selection.scannedContent());
        assertEquals("/example X skipped X", selection.replaceSelected(ignored -> "X"));
    }

    @Test
    void quotedArgumentsKeepTheirDelimitersWhenReplaced() {
        CommandArgumentRuleSet rules = CommandArgumentRuleSet.compile(List.of(
                "[default:include] /tell [ignore:1]"
        ));

        CommandArgumentRuleSet.CommandSelection selection = rules.select("/tell Player  \"blocked message\"");
        assertEquals("blocked message", selection.scannedContent());
        assertEquals("/tell Player  \"FILTERED\"", selection.replaceSelected(ignored -> "FILTERED"));
    }

    @Test
    void replaceSelectedDoesNotBreakQuotesWhenQuotesAreInTheMiddleOfTheMessage() {
        CommandArgumentRuleSet rules = CommandArgumentRuleSet.compile(List.of(
                "[default:include] /tell [ignore:1]"
        ));

        CommandArgumentRuleSet.CommandSelection selection = rules.select("/tell Player \"blocked message\" world");
        CommandArgumentRuleSet.CommandSelection selection2 = rules.select("/tell Player world of \"blocked message\"");
        assertEquals("/tell Player [\"blocked message\" world]", selection.replaceSelected(content -> "[" + content + "]"));
        assertEquals("/tell Player [world of \"blocked message\"]", selection2.replaceSelected(content -> "[" + content + "]"));
    }

    @Test
    void quotesAreInTheMiddleOfTheMessage() {
        CommandArgumentRuleSet rules = CommandArgumentRuleSet.compile(List.of(
                "[default:include] /tell [ignore:1]"
        ));

        CommandArgumentRuleSet.CommandSelection selection = rules.select("/tell Player \"blocked message\" world");
        CommandArgumentRuleSet.CommandSelection selection2 = rules.select("/tell Player world of \"blocked message\"");
        assertEquals("\"blocked message\" world", selection.scannedContent());
        assertEquals("world of \"blocked message\"", selection2.scannedContent());
    }

    @Test
    void quotesTokenBehaveSameAsBrigadier() {
        CommandArgumentRuleSet rules = CommandArgumentRuleSet.compile(List.of(
                "[default:include] /tell [ignore:1,3]"
        ));

        CommandArgumentRuleSet.CommandSelection selection = rules.select("/tell Player \"科比布莱恩特说 我要打\\\"篮球\"");
        assertEquals("/tell Player \"[科比布莱恩特说 我要打\\\"篮球]\"", selection.replaceSelected(content -> "[" + content + "]"));
    }

    @Test
    void longestCommandPathWinsAndIndexesAfterThePath() {
        CommandArgumentRuleSet rules = CommandArgumentRuleSet.compile(List.of(
                "[default:ignore] /mail [include:1]",
                "[default:ignore] /mail send [include:2..]"
        ));

        CommandArgumentRuleSet.CommandSelection selection = rules.select("/mail send Player blocked message");
        assertEquals("blocked message", selection.scannedContent());
    }

    @Test
    void plainRulesRemainCompatibleAndInvalidRulesAreRejected() {
        CommandArgumentRuleSet rules = CommandArgumentRuleSet.compile(List.of("/tell"));
        assertEquals("Player message", rules.select("/tell Player message").scannedContent());
        assertFalse(rules.select("/say message").listed());

        assertThrows(IllegalArgumentException.class,
                () -> CommandArgumentRuleSet.compile(List.of("tell [ignore:1]")));
        assertThrows(IllegalArgumentException.class,
                () -> CommandArgumentRuleSet.compile(List.of("[default:maybe] /tell")));
        assertThrows(IllegalArgumentException.class,
                () -> CommandArgumentRuleSet.compile(List.of("/tell [ignore:0]")));
        assertThrows(IllegalArgumentException.class,
                () -> CommandArgumentRuleSet.compile(List.of("/tell", "/TELL [ignore:1]")));
    }
}
