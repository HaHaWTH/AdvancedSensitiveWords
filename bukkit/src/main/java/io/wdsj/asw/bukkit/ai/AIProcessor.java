package io.wdsj.asw.bukkit.ai;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for AI processors (Maybe more in the future?)
 */
public interface AIProcessor {
    void shutdown();
    CompletableFuture<?> process(String input);
}
