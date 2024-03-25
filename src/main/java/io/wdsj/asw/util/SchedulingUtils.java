package io.wdsj.asw.util;


import io.wdsj.asw.AdvancedSensitiveWords;

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
}
