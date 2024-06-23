package io.wdsj.asw.bukkit.ai;

import java.util.concurrent.CompletableFuture;

/**
 * Abstract class for AI processors (Maybe more in the future?)
 */
public abstract class AIProcessorAbstract {
    public abstract void initService(String modelAddress, String modelName, int timeOut, boolean debug);
    public abstract void shutdown();
    public abstract CompletableFuture<String> process(String inputMessage);
}
