package io.wdsj.asw.bukkit.util.message

import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.setting.PluginMessages
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.command.CommandSender

object MessageUtils {
    private const val DYNAMIC_TAG_PREFIX = "asw_dynamic_"
    private val placeholderName = Regex("[a-z0-9_-]+")
    private val miniMessage: MiniMessage = MiniMessage.miniMessage()
    private val plainTextSerializer: PlainTextComponentSerializer = PlainTextComponentSerializer.plainText()
    private val legacySectionSerializer: LegacyComponentSerializer = LegacyComponentSerializer.legacySection()

    @JvmStatic
    fun retrieveMessage(property: PluginMessages): String {
        return AdvancedSensitiveWords.message(property)
    }

    @JvmStatic
    fun retrieveComponent(property: PluginMessages): Component {
        return miniMessage.deserialize(retrieveMessage(property))
    }

    @JvmStatic
    fun deserialize(message: String): Component {
        return miniMessage.deserialize(message)
    }

    @JvmStatic
    fun deserializeTemplate(template: String, placeholders: Map<String, String>): Component {
        if (placeholders.isEmpty()) return deserialize(template)

        var resolvedTemplate = template
        val resolvers = TagResolver.builder()
        placeholders.forEach { (name, value) ->
            require(placeholderName.matches(name)) { "Invalid message placeholder name: $name" }
            val tagName = DYNAMIC_TAG_PREFIX + name
            resolvedTemplate = resolvedTemplate.replace("%$name%", "<$tagName>")
            resolvers.resolver(Placeholder.unparsed(tagName, value))
        }
        return miniMessage.deserialize(resolvedTemplate, resolvers.build())
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
    fun sendMessage(sender: CommandSender, property: PluginMessages) {
        sendMessage(sender, retrieveMessage(property))
    }

    @JvmStatic
    fun sendMessage(sender: CommandSender, component: Component) {
        if (plainTextSerializer.serialize(component).isNotEmpty()) {
            sender.sendMessage(component)
        }
    }

    @JvmStatic
    fun sendMessage(sender: CommandSender, message: String) {
        if (message.isNotEmpty()) {
            sender.sendMessage(deserialize(message))
        }
    }

    @JvmStatic
    fun sendTemplate(sender: CommandSender, template: String, placeholders: Map<String, String>) {
        sendMessage(sender, deserializeTemplate(template, placeholders))
    }
}
