package io.wdsj.asw.bukkit.util

import java.lang.reflect.Method
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/**
 * Utility class for virtual threads which are introduced in Java 21
 */
object VirtualThreadUtils {
    private var methodVirtualThreadFactory: Method?
    private var methodThreadOfVirtual: Method?

    private var methodVirtualThreadPerTaskExecutor: Method?

    init {
        try {
            methodThreadOfVirtual = Thread::class.java.getMethod("ofVirtual")
            val threadBuilder = Class.forName("java.lang.Thread\$Builder")
            methodVirtualThreadFactory= threadBuilder.getMethod("factory")
            methodThreadOfVirtual?.isAccessible = true
            methodVirtualThreadFactory?.isAccessible = true
        } catch (e: Exception) {
            methodThreadOfVirtual = null
            methodVirtualThreadFactory = null
        }
        try {
            methodVirtualThreadPerTaskExecutor = Executors::class.java.getMethod("newVirtualThreadPerTaskExecutor")
            methodVirtualThreadPerTaskExecutor?.isAccessible = true
        } catch (e: Exception) {
            methodVirtualThreadPerTaskExecutor = null
        }
    }

    @JvmStatic
    fun newVirtualThreadFactory(): ThreadFactory? {
        return invokeOfVirtualFactory()
    }

    @JvmStatic
    fun newVirtualThreadPerTaskExecutor(): ExecutorService? {
        return invokeNewVirtualThreadPerTaskExecutor()
    }

    @JvmStatic
    fun newVirtualThreadFactoryOrProvided(threadFactory: ThreadFactory): ThreadFactory {
        return invokeOfVirtualFactory() ?: threadFactory
    }

    @JvmStatic
    fun newVirtualThreadFactoryOrDefault(): ThreadFactory {
        return invokeOfVirtualFactory() ?: Executors.defaultThreadFactory()
    }

    @JvmStatic
    fun newVirtualThreadPerTaskExecutorOrProvided(executorService: ExecutorService): ExecutorService {
        return invokeNewVirtualThreadPerTaskExecutor() ?: executorService
    }

    private fun invokeNewVirtualThreadPerTaskExecutor(): ExecutorService? {
        return try {
            methodVirtualThreadPerTaskExecutor?.invoke(null) as ExecutorService
        } catch (e: Exception) {
            null
        }
    }

    private fun invokeOfVirtualFactory(): ThreadFactory? {
        return try {
            methodVirtualThreadFactory?.invoke(methodThreadOfVirtual?.invoke(null)) as ThreadFactory
        } catch (e: Exception) {
            null
        }
    }
}
