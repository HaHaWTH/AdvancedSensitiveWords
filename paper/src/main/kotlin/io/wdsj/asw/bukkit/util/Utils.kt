package io.wdsj.asw.bukkit.util

import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.setting.PluginSettings
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.atomic.AtomicLong

object Utils {
    @JvmField
    val messagesFilteredNum: AtomicLong = AtomicLong(0)

    @JvmStatic
    fun getPlayerIp(player: Player): String {
        val address = player.address
        if (address != null && address.address != null) return address.address.hostAddress
        throw IllegalStateException("Player address is null")
    }

    @JvmStatic
    fun isClassLoaded(className: String): Boolean {
        try {
            Class.forName(className)
            return true
        } catch (ignored: ClassNotFoundException) {
            return false
        }
    }

    fun isAnyClassLoaded(vararg classNames: String): Boolean {
        for (className in classNames) {
            if (isClassLoaded(className)) return true
        }
        return false
    }

    @JvmStatic
    fun isClassExists(className: String): Boolean {
        return try {
            val url = className.replace(".", "/") + ".class"
            return Thread.currentThread().contextClassLoader.getResource(url) != null
        } catch (ignored: Throwable) {
            false
        }
    }

    fun isCommand(command: String): Boolean {
        return command.startsWith("/")
    }

    val preProcessRegex: String
        get() = AdvancedSensitiveWords.setting(PluginSettings.PRE_PROCESS_REGEX)

    @JvmStatic
    val minecraftVersion: String
        get() = Bukkit.getMinecraftVersion()
}
