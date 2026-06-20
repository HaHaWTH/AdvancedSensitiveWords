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
        public String messageOnViolationReset = "<gradient:#22d3ee:#4ade80><bold>ASW</bold></gradient> <dark_gray>| <green>Reset all player violation counts.";
        @Comment("Status command output.")
        public String messageOnCommandStatus = """
                <gradient:#22d3ee:#4ade80><bold>AdvancedSensitiveWords</bold></gradient> <dark_gray>| <aqua>Plugin Status
                <dark_gray>  Version <gray>%version% <dark_gray>| <gray>MC %mc_version%
                <dark_gray>  System <gray>%platform% %bit% <dark_gray>| <gray>Java %java_version% (%java_vendor%)
                <dark_gray>  Initialized <aqua>%init% <dark_gray>| <gray>Mode <aqua>%mode%
                <dark_gray>  Filtered <aqua>%num% <dark_gray>| <gray>Average <aqua>%ms%""";
        @Comment("AI status command output.")
        public String messageOnAiStatus = """
                <gradient:#22d3ee:#4ade80><bold>AdvancedSensitiveWords</bold></gradient> <dark_gray>| <aqua>AI Moderation
                <dark_gray>  Enabled <aqua>%enabled% <dark_gray>| <gray>Model <aqua>%model%
                <dark_gray>  Submitted <aqua>%submitted% <dark_gray>| <gray>Dropped <aqua>%dropped% <dark_gray>| <gray>Failed <aqua>%failed%
                <dark_gray>  Invalid <aqua>%invalid% <dark_gray>| <gray>Enforced <aqua>%enforced%
                <dark_gray>  Queue <aqua>%active% active <gray>/ <aqua>%queued% queued <gray>/ <aqua>%pool_size% workers
                <dark_gray>  Enforcement threshold <aqua>%threshold%""";
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
        @Comment("Message sent when a violation-counter module is invalid.")
        public String invalidViolationModule = "<red>Unknown violation module. Choose chat, ai, book, sign, anvil, or item.";
        @Comment("Message sent when an online player cannot be found.")
        public String noSuchPlayer = "<red>That player could not be found.";
        @Comment("Staff notification for a local violation.")
        public String noticeOperator = "<gradient:#22d3ee:#4ade80><bold>ASWNotify</bold></gradient> <dark_gray>| <red>%player% <gray>triggered <aqua>%type% <gray>filtering <dark_gray>| <gray>VL <aqua>%violation%\n<dark_gray>  Message <white>%message%\n<dark_gray>  Matches <aqua>%censored_list%";
        @Comment("Staff notification for a violation received from Velocity.")
        public String noticeOperatorProxy = "<gradient:#22d3ee:#4ade80><bold>ASWNotify</bold></gradient> <dark_gray>| <red>%player% <gray>on <aqua>%server_name% <gray>triggered <aqua>%type% <gray>filtering <dark_gray>| <gray>VL <aqua>%violation%\n<dark_gray>  Message <white>%message%\n<dark_gray>  Matches <aqua>%censored_list%";
        @Comment("Update notification for staff.")
        public String updateAvailable = "<gradient:#22d3ee:#4ade80><bold>ASWNotify</bold></gradient> <dark_gray>| <yellow>Update available <dark_gray>| <gray>Latest <aqua>%latest_version% <dark_gray>| <gray>Current <aqua>%current_version%";
        @Comment("Player information command output.")
        public String messageOnCommandInfo = """
                <gradient:#22d3ee:#4ade80><bold>AdvancedSensitiveWords</bold></gradient> <dark_gray>| <aqua>Player Violations
                <dark_gray>  Player <aqua>%player%
                <dark_gray>  Chat <aqua>%chat_violation% <dark_gray>| <gray>AI <aqua>%ai_violation% <dark_gray>| <gray>Book <aqua>%book_violation%
                <dark_gray>  Sign <aqua>%sign_violation% <dark_gray>| <gray>Anvil <aqua>%anvil_violation% <dark_gray>| <gray>Item <aqua>%item_violation%
                <dark_gray>  Total VL <gradient:#fbbf24:#fb7185><bold>%violation%</bold></gradient>""";
        @Comment("Message sent after resetting a player's violation counter.")
        public String messageOnCommandReset = "<gradient:#22d3ee:#4ade80><bold>ASW</bold></gradient> <dark_gray>| <green>Reset <aqua>%module% <green>VL for <aqua>%player%<green>.";
    }

    @Configuration
    public static final class CommandTest {
        @Comment("Output when the test input contains blocked words.")
        public String testResultTrue = """
                <gradient:#22d3ee:#4ade80><bold>Test Result</bold></gradient>
                <dark_gray>  Original <red>%original_msg%
                <dark_gray>  Filtered <green>%processed_msg%
                <dark_gray>  Matches <aqua>%censored_list%""";
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
