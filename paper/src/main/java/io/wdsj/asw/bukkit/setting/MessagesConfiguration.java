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
        @Comment("Message sent when chat anti-spam cancels a message.")
        public String messageOnChatAntiSpam = "<yellow>Please slow down and avoid repeating similar messages.";
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
        public String messageOnViolationReset = "<gradient:#22d3ee:#4ade80><bold>ASWNotify</bold></gradient> <dark_gray>| <green>Reset all player violation counts.";
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
                <dark_gray>  Enabled <aqua>%enabled% <dark_gray>| <gray>Model <aqua>%model% <dark_gray>| <gray>API <aqua>%api_mode%
                <dark_gray>  Submitted <aqua>%submitted% <dark_gray>| <gray>Dropped <aqua>%dropped% <dark_gray>| <gray>Failed <aqua>%failed%
                <dark_gray>  Invalid <aqua>%invalid% <dark_gray>| <gray>Enforced <aqua>%enforced%
                <dark_gray>  Queue <aqua>%active% active <gray>/ <aqua>%queued% queued <gray>/ <aqua>%pool_size% workers
                <dark_gray>  Notify/Punish thresholds <aqua>%thresholds%""";
        @Comment("Message sent to NEWBIE players when they join.")
        public String playerGroupNewbieJoin = "<yellow>New players are temporarily moderated more strictly. Please avoid spam, long repeated messages, and links.";
        @Comment("Message sent when NEWBIE rate limiting cancels chat or a command.")
        public String playerGroupNewbieRateLimit = "<yellow>Please slow down. New players have a temporary chat rate limit.";
        @Comment("Message sent when NEWBIE link checking cancels chat or a command.")
        public String playerGroupNewbieLinkBlocked = "<red>New players cannot send links yet.";
        @Comment("Player group information command output.")
        public String playerGroupInfo = """
                <gradient:#22d3ee:#4ade80><bold>Player Group</bold></gradient> <dark_gray>| <aqua>%player%
                <dark_gray>  Group <aqua>%group% <dark_gray>| <gray>Source <aqua>%source%
                <dark_gray>  Activity <aqua>%score% <gray>/ <aqua>%threshold%
                <dark_gray>  Play <aqua>%play_hours%h <dark_gray>| <gray>Move <aqua>%moved_blocks% <gray>blocks <dark_gray>| <gray>Mined <aqua>%mined_blocks%
                <dark_gray>  Mobs <aqua>%mob_kills% <dark_gray>| <gray>Use <aqua>%used_items% <dark_gray>| <gray>Break <aqua>%broken_items% <dark_gray>| <gray>Craft <aqua>%crafted_items%
                <dark_gray>  Damage <aqua>%damage_dealt%/%damage_taken% <dark_gray>| <gray>Deaths <aqua>%deaths% <dark_gray>| <gray>Enchant <aqua>%enchanted_items%
                <dark_gray>  Fish <aqua>%fish_caught% <dark_gray>| <gray>Trades <aqua>%villager_trades%""";
        @Comment("Message sent after manually setting a player group.")
        public String playerGroupSet = "<green>Set <aqua>%player% <green>to group <aqua>%group%<green>.";
        @Comment("Message sent after clearing a manual player group override.")
        public String playerGroupClear = "<green>Cleared manual group override for <aqua>%player%<green>.";
        @Comment("Message sent when player groups are disabled.")
        public String playerGroupDisabled = "<red>Player groups are disabled.";
        @Comment("Message sent when player group storage fails.")
        public String playerGroupStorageError = "<red>Player group storage operation failed.";
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
        @Comment("Staff notification for an AI classification that does not reach the punishment threshold.")
        public String aiObservation = "<gradient:#22d3ee:#4ade80><bold>ASWObserve</bold></gradient> <dark_gray>| <red>%player% <gray>was classified as <aqua>%category% <dark_gray>| <gray>Confidence <aqua>%confidence%\n<dark_gray>  Message <white>%message%";
        @Comment("Staff notification for an AI observation received from Velocity.")
        public String aiObservationProxy = "<gradient:#22d3ee:#4ade80><bold>ASWObserve</bold></gradient> <dark_gray>| <red>%player% <gray>on <aqua>%server_name% <gray>was classified as <aqua>%category% <dark_gray>| <gray>Confidence <aqua>%confidence%\n<dark_gray>  Message <white>%message%";
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
        public String messageOnCommandReset = "<gradient:#22d3ee:#4ade80><bold>AdvancedSensitiveWords</bold></gradient> <dark_gray>| <green>Reset <aqua>%module% <green>VL for <aqua>%player%<green>.";
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
