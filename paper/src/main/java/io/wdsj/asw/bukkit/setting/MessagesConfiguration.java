package io.wdsj.asw.bukkit.setting;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;

@Configuration
public abstract class MessagesConfiguration {
    @Comment({"Chat filtering messages."})
    public Chat chat = new Chat();
    @Comment({"Sign filtering messages."})
    public Sign sign = new Sign();
    @Comment({"Anvil filtering messages."})
    public Anvil anvil = new Anvil();
    @Comment({"Book filtering messages."})
    public Book book = new Book();
    @Comment({"Player-name filtering messages."})
    public Name name = new Name();
    @Comment({"Item filtering messages."})
    public Item item = new Item();
    @Comment({"General plugin messages."})
    public Plugin plugin = new Plugin();

    @Configuration
    public static final class Chat {
        @Comment("Message sent when chat or command content is blocked.")
        public String messageOnChat = "<red>Your message contains blocked words.";
    }

    @Configuration
    public static final class Sign {
        @Comment("Message sent when sign content is blocked.")
        public String messageOnSign = "<red>Your sign text contains blocked words.";
    }

    @Configuration
    public static final class Anvil {
        @Comment("Message sent when an anvil rename is blocked.")
        public String messageOnAnvilRename = "<red>That item name contains blocked words.";
    }

    @Configuration
    public static final class Book {
        @Comment("Message sent when book content is blocked.")
        public String messageOnBook = "<red>Your book contains blocked words.";
    }

    @Configuration
    public static final class Name {
        @Comment("Message shown when a player name is blocked.")
        public String messageOnName = "<red>Your username contains blocked words. Please change it or contact an administrator.";
    }

    @Configuration
    public static final class Item {
        @Comment("Message sent when an item contains blocked content.")
        public String messageOnItem = "<red>This item contains blocked words.";
    }

    @Configuration
    public static final class Plugin {
        @Comment("Message sent after a successful reload.")
        public String messageOnCommandReload = "<green>AdvancedSensitiveWords has been reloaded.";
        @Comment("Message sent after all violation counters are reset.")
        public String messageOnViolationReset = "<green><bold>All player violation counts have been reset.";
        @Comment("Status command output.")
        public String messageOnCommandStatus = """
                <aqua>AdvancedSensitiveWords<reset>---<aqua> Plugin Status(%version%)(MC %mc_version%)
                   <gray>System: <aqua>%platform% %bit% (Java %java_version% -- %java_vendor%)
                   <gray>Initialized: %init%
                   <gray>Detection mode: %mode%
                   <gray>Filtered messages: %num%
                   <gray>Average processing time over the last 20 checks: %ms%""";
        @Comment("Messages used by the test command.")
        public CommandTest commandTest = new CommandTest();
        @Comment("Messages used by the punishment command.")
        public CommandPunish commandPunish = new CommandPunish();
        @Comment("Messages used after adding words at runtime.")
        public CommandAdd commandAdd = new CommandAdd();
        @Comment("Messages used after removing words at runtime.")
        public CommandRemove commandRemove = new CommandRemove();
        @Comment("Messages about temporary word-list changes.")
        public CommandWord commandWord = new CommandWord();
        @Comment("Message sent when the sender lacks permission.")
        public String noPermission = "<red>You do not have permission to use that command.";
        @Comment("Message sent for an unknown command.")
        public String unknownCommand = "<red>Unknown command. Use <gray>/asw help<red>.";
        @Comment("Message sent when command arguments are missing.")
        public String argsNotEnough = "<red>Missing arguments. Use <gray>/asw help<red>.";
        @Comment("Message sent when an online player cannot be found.")
        public String noSuchPlayer = "<red>That player could not be found.";
        @Comment("Staff notification for a local violation.")
        public String noticeOperator = "<white>[<aqua>ASW<gray>Notify<white>] <red>%player% <gray>triggered %type% filtering. Message: <white>%message% <gray>Matches: <aqua>%censored_list%";
        @Comment("Staff notification for a violation received from Velocity.")
        public String noticeOperatorProxy = "<white>[<aqua>ASW<gray>Notify<white>] <red>%player% <gray>on <aqua>%server_name% <gray>triggered %type% filtering. Message: <white>%message% <gray>Matches: <aqua>%censored_list%";
        @Comment("Update notification for staff.")
        public String updateAvailable = "<white>[<aqua>ASW<gray>Notify<white>] <gray>A new version is available. Latest: <aqua>%latest_version%<gray>, current: <aqua>%current_version%";
        @Comment("Player information command output.")
        public String messageOnCommandInfo = """
                <aqua>AdvancedSensitiveWords<reset>---<aqua> Player Info
                   <gray>Name: <aqua>%player%
                   <gray>Violations: <aqua>%violation%""";
        @Comment("Message sent after resetting a player's violation counter.")
        public String messageOnCommandReset = "<green>Reset violation count for %player%.";
    }

    @Configuration
    public static final class CommandTest {
        @Comment("Output when the test input contains blocked words.")
        public String testResultTrue = """
                <gray>Original message: <red>%original_msg%
                <gray>Filtered message: <green>%processed_msg%
                <gray>Matched words: <aqua>%censored_list%""";
        @Comment("Output when the test input contains no blocked words.")
        public String testResultPass = "<green>No blocked words were found.";
        @Comment("Output while the word filter is still initializing.")
        public String testNotInit = "<red>The plugin has not finished initializing.";
    }

    @Configuration
    public static final class CommandPunish {
        @Comment("Output when a punishment method cannot be parsed.")
        public String parseError = "<red>Could not parse the punishment method. Please check the syntax.";
        @Comment("Output after a player is punished.")
        public String success = "<green>Punished %player%.";
    }

    @Configuration
    public static final class CommandAdd {
        @Comment("Output after blocked words are added.")
        public String success = "<green>Added to the word filter.";
    }

    @Configuration
    public static final class CommandRemove {
        @Comment("Output after blocked words are removed.")
        public String success = "<green>Removed from the word filter.";
    }

    @Configuration
    public static final class CommandWord {
        @Comment("Explains that command changes are not persisted.")
        public String runtimeOnly = "<yellow>Command changes are temporary and will be discarded when the filter reloads or the server restarts.";
    }
}
