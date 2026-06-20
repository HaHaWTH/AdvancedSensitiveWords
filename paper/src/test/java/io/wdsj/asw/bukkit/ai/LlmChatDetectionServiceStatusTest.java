package io.wdsj.asw.bukkit.ai;

import io.wdsj.asw.bukkit.setting.PaperConfigurationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LlmChatDetectionServiceStatusTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void reportsDisabledAiWithoutCreatingExecutorThreads() {
        PaperConfigurationService configuration = new PaperConfigurationService(
                LoggerFactory.getLogger(LlmChatDetectionServiceStatusTest.class),
                temporaryDirectory
        );
        configuration.load();

        try (LlmChatDetectionService service = new LlmChatDetectionService(configuration)) {
            LlmChatDetectionService.LlmRuntimeStatus status = service.runtimeStatus();

            assertFalse(status.enabled());
            assertEquals(0L, status.submittedRequests());
            assertEquals(0L, status.droppedRequests());
            assertEquals(0L, status.failedRequests());
            assertEquals(0L, status.invalidResponses());
            assertEquals(0L, status.enforcedResponses());
            assertEquals(0, status.activeRequests());
            assertEquals(0, status.queuedRequests());
            assertEquals(0, status.poolSize());
            assertEquals("deepseek-v4-flash", status.modelName());
            LlmCategoryPolicy harassment = status.categoryPolicy().get(
                    io.wdsj.asw.bukkit.api.moderation.LlmModerationCategory.HARASSMENT);
            LlmCategoryPolicy clean = status.categoryPolicy().get(
                    io.wdsj.asw.bukkit.api.moderation.LlmModerationCategory.CLEAN);
            assertEquals(0.75D, harassment.notifyConfidence());
            assertEquals(0.90D, harassment.punishConfidence());
            assertEquals(-1.0D, clean.notifyConfidence());
            assertEquals(-1.0D, clean.punishConfidence());
        }
    }
}
