package io.wdsj.asw.bukkit.ai;

import io.github.amithkoujalgi.ollama4j.core.OllamaAPI;
import io.github.amithkoujalgi.ollama4j.core.models.OllamaResult;
import io.github.amithkoujalgi.ollama4j.core.utils.OptionsBuilder;
import io.github.amithkoujalgi.ollama4j.core.utils.PromptBuilder;
import io.wdsj.asw.bukkit.setting.PluginSettings;

import java.util.concurrent.CompletableFuture;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class OllamaProcessor implements AIProcessor {
    public static boolean isOllamaInit = false;
    private static PromptBuilder promptBuilder;
    private static OllamaAPI api;
    private static String modelName;
    public OllamaProcessor() {
    }

    public void initService(String modelAddress, String name, int timeOut, boolean debug) {
        modelName = name;
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
        if (!THREAD_POOL.isShutdown()) {
            THREAD_POOL.shutdownNow();
        }
        api = null;
        isOllamaInit = false;
    }

    public static CompletableFuture<String> process(String inputMessage) {
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
                LOGGER.warning("Error occurred while communicating with Ollama server: " + e.getMessage());
                return null;
            }
        }, THREAD_POOL);
    }
}
