package io.wdsj.asw.bukkit.util.message;

import ch.jalu.configme.properties.Property;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.messagesManager;

public class MessageUtils {
    private MessageUtils() {
    }

    public static final char AMPERSAND_CHAR = '&';

    public static String retrieveMessage(Property<String> property) {
        return ChatColor.translateAlternateColorCodes(AMPERSAND_CHAR, messagesManager.getProperty(property));
    }

    public static void sendMessage(CommandSender sender, Property<String> property) {
        String msg = retrieveMessage(property);
        if (!msg.isEmpty()) {
            sender.sendMessage(msg);
        }
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (!message.isEmpty()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes(AMPERSAND_CHAR, message));
        }
    }
}
