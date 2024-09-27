package io.wdsj.asw.bukkit.util.message

import ch.jalu.configme.properties.Property
import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender

object MessageUtils {
    private const val AMPERSAND_CHAR: Char = '&'

    @JvmStatic
    fun retrieveMessage(property: Property<String>): String {
        return ChatColor.translateAlternateColorCodes(
            AMPERSAND_CHAR,
            AdvancedSensitiveWords.messagesManager.getProperty(property)
        )
    }

    @JvmStatic
    fun sendMessage(sender: CommandSender, property: Property<String>) {
        val msg = retrieveMessage(property)
        if (msg.isNotEmpty()) {
            sender.sendMessage(msg)
        }
    }

    @JvmStatic
    fun sendMessage(sender: CommandSender, message: String) {
        if (message.isNotEmpty()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes(AMPERSAND_CHAR, message))
        }
    }
}
