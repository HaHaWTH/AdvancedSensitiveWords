package io.wdsj.asw.bukkit.util;

import io.wdsj.asw.bukkit.impl.list.AdvancedList;

import java.util.List;

import static io.wdsj.asw.bukkit.util.Utils.messagesFilteredNum;

public class TimingUtils {
    private static final List<Long> processStatistic = new AdvancedList<>();
    private static final String vendor = System.getProperties().getProperty("java.vendor");
    private static final String javaVersion = System.getProperties().getProperty("java.version");

    public static void addProcessStatistic(long endTime, long startTime) {
        long processTime = endTime - startTime;
        while (processStatistic.size() >= 20) {
            processStatistic.remove(0);
        }
        processStatistic.add(processTime);
    }

    public static String getJvmVersion() {
        return javaVersion;
    }

    public static String getJvmVendor() {
        return vendor;
    }

    public static long getProcessAverage() {
        long sum = 0L;
        for (long l : processStatistic) {
            sum += l;
        }
        return !processStatistic.isEmpty() ? sum / processStatistic.size() : 0L;
    }

    public static void cleanStatisticCache() {
        processStatistic.clear();
        messagesFilteredNum.set(0L);
    }
    private TimingUtils() {
    }

}
