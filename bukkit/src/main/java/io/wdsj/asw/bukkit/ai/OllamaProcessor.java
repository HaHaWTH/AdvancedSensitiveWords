package io.wdsj.asw.bukkit.ai;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.amithkoujalgi.ollama4j.core.OllamaAPI;
import io.github.amithkoujalgi.ollama4j.core.models.OllamaResult;
import io.github.amithkoujalgi.ollama4j.core.utils.OptionsBuilder;
import io.github.amithkoujalgi.ollama4j.core.utils.PromptBuilder;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.util.VTUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class OllamaProcessor extends AIProcessorAbstract {
    public boolean isOllamaInit = false;
    private PromptBuilder promptBuilder;
    private final ExecutorService THREAD_POOL;
    private OllamaAPI api;
    private String modelName;
    public OllamaProcessor() {
        THREAD_POOL = VTUtils.getVTExecutorServiceOrProvided(Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("ASW-OllamaProcessor-%d").build()));
    }

    @Override
    public void initService(String modelAddress, String modelName, int timeOut, boolean debug) {
        this.modelName = modelName;
        api = new OllamaAPI(modelAddress);
        api.setRequestTimeoutSeconds(timeOut);
        try {
            if (!api.ping()) {
                LOGGER.warning("Ollama ping failed, please check the api address");
                isOllamaInit = false;
                return;
            }
        } catch (Exception e) {
            LOGGER.warning("Ollama ping failed, please check the api address");
            isOllamaInit = false;
            return;
        }
        LOGGER.info("Successfully connect to ollama server");
        if (debug) {
            LOGGER.info("Ollama debug logging enabled");
        }
        api.setVerbose(debug);
        isOllamaInit = true;
    }

    @Override
    public void shutdown() {
        THREAD_POOL.shutdownNow();
        api = null;
        isOllamaInit = false;
    }

    @Override
    public CompletableFuture<String> process(String inputMessage) {
        if (!isOllamaInit) {
            throw new IllegalStateException("OllamaProcessor is not initialized");
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                promptBuilder = new PromptBuilder().addLine(settingsManager.getProperty(PluginSettings.AI_MODEL_PROMPT)).addSeparator()
                        .add(inputMessage);
                OllamaResult response = api.generate(modelName, promptBuilder.build(),
                        new OptionsBuilder().build());
                return response.getResponse();
            } catch (Exception e) {
                LOGGER.severe("Error occurred while communicating with Ollama server: " + e.getMessage());
                return null;
            }
        }, THREAD_POOL);
    }
}
