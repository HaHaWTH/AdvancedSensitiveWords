package io.wdsj.asw.bukkit.util

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/**
 * Utility class for virtual threads which are introduced in Java 21
 */
object VirtualThreadUtils {
    private var virtualThreadFactory: ThreadFactory?

    private var virtualThreadPerTaskExecutor: ExecutorService?

    init {
        try {
            val ofVirtual = Thread::class.java.getMethod("ofVirtual")
            val threadBuilder = Class.forName("java.lang.Thread\$Builder")
            val factory = threadBuilder.getMethod("factory")
            ofVirtual.isAccessible = true
            factory.isAccessible = true
            virtualThreadFactory = factory.invoke(ofVirtual.invoke(null)) as ThreadFactory
        } catch (e: Exception) {
            virtualThreadFactory = null
        }
        try {
            val method = Executors::class.java.getMethod("newVirtualThreadPerTaskExecutor")
            method.isAccessible = true
            virtualThreadPerTaskExecutor = method.invoke(null) as ExecutorService
        } catch (e: Exception) {
            virtualThreadPerTaskExecutor = null
        }
    }

    @JvmStatic
    fun newVirtualThreadFactory(): ThreadFactory? {
        return virtualThreadFactory
    }

    @JvmStatic
    fun newVirtualThreadPerTaskExecutor(): ExecutorService? {
        return virtualThreadPerTaskExecutor
    }

    @JvmStatic
    fun newVirtualThreadFactoryOrProvided(threadFactory: ThreadFactory): ThreadFactory {
        return virtualThreadFactory ?: threadFactory
    }

    @JvmStatic
    fun newVirtualThreadFactoryOrDefault(): ThreadFactory {
        return virtualThreadFactory ?: Executors.defaultThreadFactory()
    }

    @JvmStatic
    fun newVirtualThreadPerTaskExecutorOrProvided(executorService: ExecutorService): ExecutorService {
        return virtualThreadPerTaskExecutor ?: executorService
    }
}
