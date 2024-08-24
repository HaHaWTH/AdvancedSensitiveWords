package io.wdsj.asw.bukkit.util;


import com.github.Anon8281.universalScheduler.UniversalScheduler;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public class SchedulingUtils {
    private SchedulingUtils() {
    }
    private static final boolean isFolia = UniversalScheduler.isFolia;

    public static void runSyncIfFolia(Runnable runnable) {
        if (isFolia) {
            AdvancedSensitiveWords.getScheduler().runTask(runnable);
        } else {
            runnable.run();
        }
    }

    public static void runSyncAtEntityIfFolia(Entity entity, Runnable runnable) {
        if (isFolia) {
            AdvancedSensitiveWords.getScheduler().runTask(entity, runnable);
        } else {
            runnable.run();
        }
    }

    public static void runSyncAtLocationIfFolia(Location location, Runnable runnable) {
        if (isFolia) {
            AdvancedSensitiveWords.getScheduler().runTask(location, runnable);
        } else {
            runnable.run();
        }
    }

}
