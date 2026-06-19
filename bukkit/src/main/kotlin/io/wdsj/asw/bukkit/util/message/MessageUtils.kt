package io.wdsj.asw.bukkit.util.message

import ch.jalu.configme.properties.Property
import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.command.CommandSender

object MessageUtils {
    private val miniMessage: MiniMessage = MiniMessage.miniMessage()
    private val plainTextSerializer: PlainTextComponentSerializer = PlainTextComponentSerializer.plainText()
    private val legacySectionSerializer: LegacyComponentSerializer = LegacyComponentSerializer.legacySection()

    @JvmStatic
    fun retrieveMessage(property: Property<String>): String {
        return AdvancedSensitiveWords.messagesManager.getProperty(property)
    }

    @JvmStatic
    fun retrieveComponent(property: Property<String>): Component {
        return miniMessage.deserialize(retrieveMessage(property))
    }

    @JvmStatic
    fun deserialize(message: String): Component {
        return miniMessage.deserialize(message)
    }

    @JvmStatic
    fun plainText(component: Component): String {
        return plainTextSerializer.serialize(component)
    }

    @JvmStatic
    fun plainTextFromLegacy(message: String): String {
        return plainText(legacySectionSerializer.deserialize(message))
    }

    @JvmStatic
    fun plainTextComponent(message: String): Component {
        return Component.text(message)
    }

    @JvmStatic
    fun replaceLiteral(component: Component, originalPlainText: String, replacement: String): Component {
        val replacementConfig = TextReplacementConfig.builder()
            .matchLiteral(originalPlainText)
            .replacement(replacement)
            .build()
        return component.replaceText(replacementConfig)
    }

    @JvmStatic
    fun sendMessage(sender: CommandSender, property: Property<String>) {
        sendMessage(sender, retrieveMessage(property))
    }

    @JvmStatic
    fun sendMessage(sender: CommandSender, message: String) {
        if (message.isNotEmpty()) {
            sender.sendMessage(deserialize(message))
        }
    }
}
