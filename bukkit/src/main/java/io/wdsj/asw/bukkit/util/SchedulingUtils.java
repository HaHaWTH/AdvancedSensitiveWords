package io.wdsj.asw.bukkit.util;


import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public class SchedulingUtils {
    private SchedulingUtils() {
    }
    private static final boolean isFolia = Utils.isClassLoaded("io.papermc.paper.threadedregions.RegionizedServer");

    public static void runSyncIfFolia(Runnable runnable) {
        if (isFolia) {
            AdvancedSensitiveWords.getScheduler().runTask(runnable);
        } else {
            runnable.run();
        }
    }

    public static void runSyncIfFolia(Entity entity, Runnable runnable) {
        if (isFolia) {
            AdvancedSensitiveWords.getScheduler().runTask(entity, runnable);
        } else {
            runnable.run();
        }
    }

    public static void runSyncIfFolia(Location location, Runnable runnable) {
        if (isFolia) {
            AdvancedSensitiveWords.getScheduler().runTask(location, runnable);
        } else {
            runnable.run();
        }
    }

}
