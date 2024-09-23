package io.wdsj.asw.bukkit.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Utility class for virtual threads which are introduced in Java 21
 */
public class VirtualThreadUtils {
    private VirtualThreadUtils() {}

    private static ThreadFactory virtualThreadFactory;

    private static ExecutorService virtualThreadPerTaskExecutor;

    static {
        try {
            Method ofVirtual = Thread.class.getMethod("ofVirtual");
            Class<?> ThreadBuilder = Class.forName("java.lang.Thread$Builder");
            Method factory = ThreadBuilder.getMethod("factory");
            ofVirtual.setAccessible(true);
            factory.setAccessible(true);
            virtualThreadFactory = (ThreadFactory) factory.invoke(ofVirtual.invoke(null));
        } catch (Exception e) {
            virtualThreadFactory = null;
        }
        try {
            Method method = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            method.setAccessible(true);
            virtualThreadPerTaskExecutor = (ExecutorService) method.invoke(null);
        } catch (Exception e) {
            virtualThreadPerTaskExecutor = null;
        }
    }

    @Nullable
    public static ThreadFactory newVirtualThreadFactory() {
        return virtualThreadFactory;
    }

    @Nullable
    public static ExecutorService newVirtualThreadPerTaskExecutor() {
        return virtualThreadPerTaskExecutor;
    }

    @NotNull
    public static ThreadFactory newVirtualThreadFactoryOrProvided(ThreadFactory threadFactory) {
        return Utils.checkNotNullWithFallback(virtualThreadFactory, threadFactory);
    }

    @NotNull
    public static ThreadFactory newVirtualThreadFactoryOrDefault() {
        return Utils.checkNotNullWithFallback(virtualThreadFactory, Executors.defaultThreadFactory());
    }

    @NotNull
    public static ExecutorService newVirtualThreadPerTaskExecutorOrProvided(ExecutorService executorService) {
        return Utils.checkNotNullWithFallback(virtualThreadPerTaskExecutor, executorService);
    }
}
