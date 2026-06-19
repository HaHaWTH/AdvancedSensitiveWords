package io.wdsj.asw.bukkit.setting;

import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.Property;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public class PluginMessages implements SettingsHolder {
    public static final Property<String> MESSAGE_ON_CHAT = newProperty("Chat.messageOnChat", "<red>Your message contains blocked words.");
    public static final Property<String> MESSAGE_ON_SIGN = newProperty("Sign.messageOnSign", "<red>Your sign text contains blocked words.");
    public static final Property<String> MESSAGE_ON_ANVIL_RENAME = newProperty("Anvil.messageOnAnvilRename", "<red>That item name contains blocked words.");
    public static final Property<String> MESSAGE_ON_BOOK = newProperty("Book.messageOnBook", "<red>Your book contains blocked words.");
    public static final Property<String> MESSAGE_ON_NAME = newProperty("Name.messageOnName", "<red>Your username contains blocked words. Please change it or contact an administrator.");
    public static final Property<String> MESSAGE_ON_ITEM = newProperty("Item.messageOnItem", "<red>This item contains blocked words.");
    public static final Property<String> MESSAGE_ON_COMMAND_RELOAD = newProperty("Plugin.messageOnCommandReload", "<green>AdvancedSensitiveWords has been reloaded.");
    public static final Property<String> MESSAGE_ON_VIOLATION_RESET = newProperty("Plugin.messageOnViolationReset", "<green><bold>All player violation counts have been reset.");
    public static final Property<String> MESSAGE_ON_COMMAND_STATUS = newProperty("Plugin.messageOnCommandStatus", """
            <aqua>AdvancedSensitiveWords<reset>---<aqua> Plugin Status(%version%)(MC %mc_version%)
               <gray>System: <aqua>%platform% %bit% (Java %java_version% -- %java_vendor%)
               <gray>Initialized: %init%
               <gray>Detection mode: %mode%
               <gray>Filtered messages: %num%
               <gray>Average processing time over the last 20 checks: %ms%""");
    public static final Property<String> MESSAGE_ON_COMMAND_TEST = newProperty("Plugin.commandTest.testResultTrue", """
            <gray>Original message: <red>%original_msg%
            <gray>Filtered message: <green>%processed_msg%
            <gray>Matched words: <aqua>%censored_list%""");
    public static final Property<String> MESSAGE_ON_COMMAND_TEST_PASS = newProperty("Plugin.commandTest.testResultPass", "<green>No blocked words were found.");
    public static final Property<String> MESSAGE_ON_COMMAND_TEST_NOT_INIT = newProperty("Plugin.commandTest.testNotInit", "<red>The plugin has not finished initializing.");
    public static final Property<String> MESSAGE_ON_COMMAND_PUNISH_PARSE_ERROR = newProperty("Plugin.commandPunish.parseError", "<red>Could not parse the punishment method. Please check the syntax.");
    public static final Property<String> MESSAGE_ON_COMMAND_PUNISH_SUCCESS = newProperty("Plugin.commandPunish.success", "<green>Punished %player%.");
    public static final Property<String> MESSAGE_ON_COMMAND_ADD_SUCCESS = newProperty("Plugin.commandAdd.success", "<green>Added to the word filter.");
    public static final Property<String> MESSAGE_ON_COMMAND_REMOVE_SUCCESS = newProperty("Plugin.commandRemove.success", "<green>Removed from the word filter.");
    public static final Property<String> MESSAGE_ON_COMMAND_RUNTIME_ONLY = newProperty(
            "Plugin.commandWord.runtimeOnly",
            "<yellow>Command changes are temporary and will be discarded when the filter reloads or the server restarts."
    );
    public static final Property<String> NO_PERMISSION = newProperty("Plugin.noPermission", "<red>You do not have permission to use that command.");
    public static final Property<String> UNKNOWN_COMMAND = newProperty("Plugin.unknownCommand", "<red>Unknown command. Use <gray>/asw help<red>.");
    public static final Property<String> NOT_ENOUGH_ARGS = newProperty("Plugin.argsNotEnough", "<red>Missing arguments. Use <gray>/asw help<red>.");
    public static final Property<String> PLAYER_NOT_FOUND = newProperty("Plugin.noSuchPlayer", "<red>That player could not be found.");
    public static final Property<String> ADMIN_REMINDER = newProperty("Plugin.noticeOperator", "<white>[<aqua>ASW<gray>Notify<white>] <red>%player% <gray>triggered %type% filtering. Message: <white>%message% <gray>Matches: <aqua>%censored_list%");
    public static final Property<String> ADMIN_REMINDER_PROXY = newProperty("Plugin.noticeOperatorProxy", "<white>[<aqua>ASW<gray>Notify<white>] <red>%player% <gray>on <aqua>%server_name% <gray>triggered %type% filtering. Message: <white>%message% <gray>Matches: <aqua>%censored_list%");
    public static final Property<String> UPDATE_AVAILABLE = newProperty("Plugin.updateAvailable", "<white>[<aqua>ASW<gray>Notify<white>] <gray>A new version is available. Latest: <aqua>%latest_version%<gray>, current: <aqua>%current_version%");
    public static final Property<String> MESSAGE_ON_PLAYER_INFO = newProperty("Plugin.messageOnCommandInfo", """
            <aqua>AdvancedSensitiveWords<reset>---<aqua> Player Info
               <gray>Name: <aqua>%player%
               <gray>Violations: <aqua>%violation%""");
    public static final Property<String> MESSAGE_ON_COMMAND_RESET = newProperty("Plugin.messageOnCommandReset", "<green>Reset violation count for %player%.");

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("", "AdvancedSensitiveWords message configuration. Messages use MiniMessage formatting.");
        conf.setComment("Plugin", "Plugin messages");
        conf.setComment("Plugin.commandTest", "Test command messages");
        conf.setComment("Plugin.commandPunish", "Punishment command messages");
        conf.setComment("Plugin.commandWord", "Temporary word command messages");
        conf.setComment("Plugin.commandAdd", "Add command messages");
        conf.setComment("Plugin.commandRemove", "Remove command messages");
        conf.setComment("Chat", "Chat detection messages");
        conf.setComment("Book", "Book detection messages");
        conf.setComment("Sign", "Sign detection messages");
        conf.setComment("Anvil", "Anvil rename detection messages");
        conf.setComment("Name", "Player name detection messages");
        conf.setComment("Item", "Player item detection messages");
    }

    private PluginMessages() {
    }
}
