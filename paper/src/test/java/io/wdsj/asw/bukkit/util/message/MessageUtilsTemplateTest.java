package io.wdsj.asw.bukkit.util.message;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MessageUtilsTemplateTest {
    @Test
    void untrustedPlaceholderValueRemainsLiteralText() {
        String payload = "<click:run_command:'/op kobe'><hover:show_text:'click'>review</hover></click>";

        Component result = MessageUtils.deserializeTemplate(
                "<gradient:#22d3ee:#4ade80>Message</gradient> <white>%message%",
                Map.of("message", payload)
        );

        assertEquals("Message " + payload, MessageUtils.plainText(result));
        assertFalse(hasClickEvent(result));
        assertFalse(hasHoverEvent(result));
    }

    @Test
    void trustedTemplateFormattingStillWorks() {
        Component result = MessageUtils.deserializeTemplate(
                "<red>%player%</red>: %message%",
                Map.of("player", "Tester", "message", "hello")
        );

        assertEquals("Tester: hello", MessageUtils.plainText(result));
    }

    private static boolean hasClickEvent(Component component) {
        return component.clickEvent() != null || component.children().stream()
                .anyMatch(MessageUtilsTemplateTest::hasClickEvent);
    }

    private static boolean hasHoverEvent(Component component) {
        return component.hoverEvent() != null || component.children().stream()
                .anyMatch(MessageUtilsTemplateTest::hasHoverEvent);
    }
}
