package io.wdsj.asw.bukkit.util

import io.wdsj.asw.bukkit.util.list.EvictingRingList
import java.util.*


object TimingUtils {
    private val processStatistic: MutableList<Long> = Collections.synchronizedList(EvictingRingList(16))
    @JvmStatic
    val jvmVendor: String = System.getProperties().getProperty("java.vendor") ?: "Unknown"
    @JvmStatic
    val jvmVersion: String = System.getProperties().getProperty("java.version") ?: "Unknown"

    @JvmStatic
    fun addProcessStatistic(endTime: Long, startTime: Long) {
        val processTime = endTime - startTime
        processStatistic.add(processTime)
    }

    @JvmStatic
    val processAverage: Long
        get() {
            var sum = 0L
            for (l in processStatistic) {
                sum += l
            }
            return if (processStatistic.isNotEmpty()) sum / processStatistic.size else 0L
        }

    @JvmStatic
    fun resetStatistics() {
        processStatistic.clear()
        Utils.messagesFilteredNum.set(0L)
    }
}
