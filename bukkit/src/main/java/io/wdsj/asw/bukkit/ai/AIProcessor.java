package io.wdsj.asw.bukkit.ai;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.wdsj.asw.bukkit.util.VTUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Interface for AI processors (Maybe more in the future?)
 */
public interface AIProcessor {
    ExecutorService THREAD_POOL = VTUtils.getVTExecutorServiceOrProvided(Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("ASW-AIProcessor-%d").setDaemon(true).build()));
    default void shutdown() {
        THREAD_POOL.shutdownNow();
    }

    CompletableFuture<?> process(String input);
}
