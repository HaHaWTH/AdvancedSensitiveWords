package io.wdsj.asw.bukkit.api.event;

import io.wdsj.asw.bukkit.api.moderation.LlmChatModerationResult;
import io.wdsj.asw.bukkit.api.moderation.LlmModerationCategory;
import io.wdsj.asw.bukkit.api.moderation.LlmModerationSeverity;
import io.wdsj.asw.bukkit.type.ModuleType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncModerationResponseEventTest {
    @Test
    void exposesAnAsyncMutableResultAndCancellationState() {
        LlmChatModerationResult original = new LlmChatModerationResult(
                LlmModerationCategory.HARASSMENT,
                List.of(),
                0.95D,
                LlmModerationSeverity.HIGH,
                List.of("targeted abuse"),
                "Direct targeted abuse."
        );
        AsyncModerationResponseEvent event = new AsyncModerationResponseEvent(
                UUID.randomUUID(),
                ModuleType.CHAT,
                UUID.randomUUID(),
                "Player",
                "message",
                2.5D,
                "{}",
                false,
                original
        );

        event.setCancelled(true);

        assertTrue(event.isAsynchronous());
        assertTrue(event.isCancelled());
        assertFalse(event.isRawResponseTruncated());
        assertEquals(original, event.getResult());
    }
}
