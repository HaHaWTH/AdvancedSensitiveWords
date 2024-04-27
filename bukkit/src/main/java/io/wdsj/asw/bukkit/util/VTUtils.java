package io.wdsj.asw.bukkit.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Utility class for virtual threads which are available in Java 21+
 */
public class VTUtils {
    private VTUtils() {}

    private static ThreadFactory VTThreadFactory;

    private static ExecutorService VTExecutorService;

    static {
        try {
            Method ofVirtual = Thread.class.getMethod("ofVirtual");
            Class<?> ThreadBuilder = Class.forName("java.lang.Thread$Builder");
            Method factory = ThreadBuilder.getMethod("factory");
            VTThreadFactory = (ThreadFactory) factory.invoke(ofVirtual.invoke(null));
        } catch (Exception e) {
            VTThreadFactory = null;
        }
        try {
            Method method = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            method.setAccessible(true);
            VTExecutorService = (ExecutorService) method.invoke(null);
        } catch (Exception e) {
            VTExecutorService = null;
        }
    }

    @Nullable
    public static ThreadFactory getVTThreadFactory() {
        return VTThreadFactory;
    }

    @Nullable
    public static ExecutorService getVTExecutorService() {
        return VTExecutorService;
    }

    @NotNull
    public static ThreadFactory getVTThreadFactoryOrDefault() {
        return VTThreadFactory != null ? VTThreadFactory : Executors.defaultThreadFactory();
    }

    @NotNull
    public static ExecutorService getVTExecutorServiceOrDefault() {
        return VTExecutorService != null ? VTExecutorService : Executors.newCachedThreadPool(Executors.defaultThreadFactory());
    }

    @NotNull
    public static ThreadFactory getVTThreadFactoryOrProvided(ThreadFactory threadFactory) {
        return VTThreadFactory != null ? VTThreadFactory : threadFactory;
    }

    @NotNull
    public static ExecutorService getVTExecutorServiceOrProvided(ExecutorService executorService) {
        return VTExecutorService != null ? VTExecutorService : executorService;
    }

}
