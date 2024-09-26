package io.wdsj.asw.bukkit.util;


import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;

import java.util.concurrent.Callable;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.getScheduler;

public class SchedulingUtils {
    private SchedulingUtils() {
    }
    private static final boolean isFolia = UniversalScheduler.isFolia;

    public static void runSyncIfFolia(Runnable runnable) {
        if (isFolia) {
            getScheduler().runTask(runnable);
        } else {
            runnable.run();
        }
    }

    public static void runSyncAtEntityIfFolia(Entity entity, Runnable runnable) {
        if (isFolia) {
            getScheduler().runTask(entity, runnable);
        } else {
            runnable.run();
        }
    }

    public static void runSyncAtLocationIfFolia(Location location, Runnable runnable) {
        if (isFolia) {
            getScheduler().runTask(location, runnable);
        } else {
            runnable.run();
        }
    }

    public static void runSyncIfEventAsync(Runnable runnable, Event event) {
        if (event.isAsynchronous()) {
            getScheduler().runTask(runnable);
        } else {
            runnable.run();
        }
    }

    public static void runSyncIfNotOnMainThread(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            getScheduler().runTask(runnable);
        }
    }

    public static void cancelTaskSafely(MyScheduledTask task) {
        if (task == null) return;
        task.cancel();
    }

    public static <T> T callSyncMethod(Callable<T> callable) {
        try {
            return getScheduler().callSyncMethod(callable).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
