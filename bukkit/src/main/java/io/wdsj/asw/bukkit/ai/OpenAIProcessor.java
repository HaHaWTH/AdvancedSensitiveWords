package io.wdsj.asw.bukkit.ai;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.ai4j.openai4j.OpenAiClient;
import dev.ai4j.openai4j.moderation.*;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.util.VTUtils;

import java.net.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static dev.ai4j.openai4j.moderation.ModerationModel.TEXT_MODERATION_LATEST;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class OpenAIProcessor extends AIProcessorAbstract {
    public boolean isOpenAiInit = false;
    private OpenAiClient client;
    private final ExecutorService THREAD_POOL;
    public OpenAIProcessor() {
        THREAD_POOL = VTUtils.getVTExecutorServiceOrProvided(Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("ASW-OpenAIProcessor-%d").build()));
    }

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
        THREAD_POOL.shutdown();
        isOpenAiInit = false;
    }

    public void process(String inputMessage, Consumer<ModerationResponse> consumerOnResponse, Consumer<Throwable> consumerOnError) {
        if (!isOpenAiInit) {
            throw new IllegalStateException("OpenAI Moderation Processor is not initialized");
        }
        ModerationRequest request = ModerationRequest.builder()
                .input(inputMessage)
                .model(TEXT_MODERATION_LATEST)
                .build();
        CompletableFuture.supplyAsync(() -> client.moderation(request)
                .onResponse(consumerOnResponse)
                .onError(consumerOnError)
                .execute(), THREAD_POOL);
    }
}
