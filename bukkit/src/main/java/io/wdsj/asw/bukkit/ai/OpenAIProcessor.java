package io.wdsj.asw.bukkit.ai;

import dev.ai4j.openai4j.OpenAiClient;
import dev.ai4j.openai4j.moderation.ModerationRequest;
import dev.ai4j.openai4j.moderation.ModerationResponse;
import io.wdsj.asw.bukkit.setting.PluginSettings;

import java.net.Proxy;
import java.util.concurrent.CompletableFuture;

import static dev.ai4j.openai4j.moderation.ModerationModel.TEXT_MODERATION_LATEST;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

/**
 * OpenAI Moderation Processor.
 */
public class OpenAIProcessor implements AIProcessor {
    public static boolean isOpenAiInit = false;
    private static OpenAiClient client;
    public OpenAIProcessor() {
    }

    /**
     * Initialize the OpenAI moderation service.
     * @param apikey the openai key
     * @param debug whether to enable debug logging
     */
    public void initService(String apikey, boolean debug) {
        @SuppressWarnings("rawtypes")
        OpenAiClient.Builder builder = OpenAiClient.builder()
                        .openAiApiKey(apikey);
        if (settingsManager.getProperty(PluginSettings.OPENAI_ENABLE_HTTP_PROXY)) {
            builder.proxy(Proxy.Type.HTTP, settingsManager.getProperty(PluginSettings.OPENAI_HTTP_PROXY_ADDRESS), settingsManager.getProperty(PluginSettings.OPENAI_HTTP_PROXY_PORT));
        }
        if (debug) {
            builder.logResponses(true)
                    .logRequests(true);
        }
        client = builder.build();
        isOpenAiInit = true;
    }

    @Override
    public void shutdown() {
        if (isOpenAiInit && client != null) {
            client.shutdown();
        }
        if (!THREAD_POOL.isShutdown()) {
            THREAD_POOL.shutdownNow();
        }
        isOpenAiInit = false;
    }

    /**
     * Process the input message using OpenAI moderation.
     * @param inputMessage the input message
     * @return A future contains the moderation response
     */
    public static CompletableFuture<ModerationResponse> process(String inputMessage) {
        if (!isOpenAiInit) {
            throw new IllegalStateException("OpenAI Moderation Processor is not initialized");
        }
        ModerationRequest request = ModerationRequest.builder()
                .input(inputMessage)
                .model(TEXT_MODERATION_LATEST)
                .build();
        return CompletableFuture.supplyAsync(() -> {
            try {
                return client.moderation(request)
                        .execute();
            } catch (Exception e) {
                LOGGER.warning("OpenAI Moderation error: " + e.getMessage());
                return null;
            }
        }, THREAD_POOL);
    }
}
