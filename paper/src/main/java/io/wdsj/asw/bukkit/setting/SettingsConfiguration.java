package io.wdsj.asw.bukkit.setting;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import io.wdsj.asw.bukkit.type.ProcessMethod;

import java.util.ArrayList;
import java.util.List;

@Configuration
public final class SettingsConfiguration {
    @Comment({"General plugin settings."})
    public Plugin plugin = new Plugin();
    @Comment({"Chat and command filtering settings."})
    public Chat chat = new Chat();
    @Comment({"Book filtering settings."})
    public Book book = new Book();
    @Comment({"Sign filtering settings."})
    public Sign sign = new Sign();
    @Comment({"Anvil filtering settings."})
    public Anvil anvil = new Anvil();
    @Comment({"Player-name filtering settings."})
    public Name name = new Name();
    @Comment({"Item filtering settings."})
    public Item item = new Item();

    @Configuration
    public static final class Plugin {
        @Comment("Message language: en or zhcn.")
        public String language = "en";
        @Comment("Whether to load the bundled default word dictionaries.")
        public boolean enableDefaultWords = true;
        @Comment("Whether to check GitHub for updates after startup.")
        public boolean checkForUpdate = true;
        @Comment("Whether to load blocked words from the configured remote source.")
        public boolean enableOnlineWords = false;
        @Comment("Remote word-list URL.")
        public String onlineWordsUrl = "https://raw.githubusercontent.com/HaHaWTH/ASW-OnlineWordList/main/lists.txt";
        @Comment("Character encoding used by the remote word list.")
        public String onlineWordsEncoding = "UTF-8";
        @Comment("Whether to cache the remote word list.")
        public boolean cacheOnlineWords = true;
        @Comment("Remote word-list cache duration in minutes.")
        public long onlineWordsCacheTimeout = 60L;
        @Comment("Whether to write violations to the log file.")
        public boolean logViolation = true;
        @Comment("Whether to notify online staff about violations.")
        public boolean noticeOperator = true;
        @Comment("Punishment actions executed for violations. Leave empty to disable punishment.")
        public List<String> punishment = new ArrayList<>();
        @Comment("Violation counter reset interval in minutes.")
        public long violationResetTime = 20L;
        @Comment("Whether to reset only the violation counters of online players.")
        public boolean onlyResetOnlinePlayers = false;
        @Comment("Whether to clear old violation logs on startup.")
        public boolean purgeLogFile = false;
        @Comment("Replacement character used for blocked content.")
        public String replacement = "*";
        @Comment("Custom replacements in the format blocked-word|replacement.")
        public List<String> definedReplacement = new ArrayList<>(List.of("fuck|love"));
        @Comment("Whether to check player chat and commands.")
        public boolean enableChatCheck = true;
        @Comment("Whether to check sign edits.")
        public boolean enableSignEditCheck = true;
        @Comment("Whether to check anvil rename results.")
        public boolean enableAnvilEditCheck = true;
        @Comment("Whether to check book edits.")
        public boolean enableBookEditCheck = true;
        @Comment("Whether to check player names.")
        public boolean enablePlayerNameCheck = false;
        @Comment("Whether to check player item names and lore.")
        public boolean enablePlayerItemCheck = false;
        @Comment("Whether to track alternate accounts by IP address.")
        public boolean enableAltsCheck = false;
        @Comment("Whether to clear player-related caches when players leave.")
        public boolean cleanPlayerDataCache = true;
        @Comment("Whether to enable PlaceholderAPI support.")
        public boolean enablePlaceholder = false;
        @Comment("Whether to send violation notifications through Velocity.")
        public boolean hookVelocity = false;
        @Comment("Plugin compatibility settings.")
        public Compatibility compatibility = new Compatibility();
        @Comment("Characters ignored by the sensitive-word matcher.")
        public String ignoreChar = "`-—=~～!！@#$%^&§*()_+[]{}\\|;:'\"“”,，.。、（）<>?？¥【】《》 ";
        @Comment("Whether to remove formatting before detection.")
        public boolean enablePreProcess = false;
        @Comment("Regular expression used by preprocessing.")
        public String preProcessRegex = "[§&][0-9A-Fa-fK-Ok-oRr]";
        @Comment("Whether to ignore letter case.")
        public boolean ignoreCase = true;
        @Comment("Whether to normalize half-width and full-width characters.")
        public boolean ignoreWidth = true;
        @Comment("Whether to normalize styled numbers.")
        public boolean ignoreNumStyle = true;
        @Comment("Whether to normalize simplified and traditional Chinese characters.")
        public boolean ignoreChineseStyle = true;
        @Comment("Whether to normalize styled English characters.")
        public boolean ignoreEnglishStyle = true;
        @Comment("Full-match mode: 0 off, 1 English, 2 English and numbers, 3 numbers.")
        public int fullMatchMode = 0;
        @Comment("Whether to normalize repeated characters.")
        public boolean ignoreRepeat = true;
        @Comment("Whether to check consecutive numbers.")
        public boolean enableNumCheck = true;
        @Comment("Minimum length considered a consecutive number sequence.")
        public int numCheckLen = 9;
        @Comment("Whether to check email addresses.")
        public boolean enableEmailCheck = false;
        @Comment("Whether to check URLs.")
        public boolean enableUrlCheck = true;
        @Comment("Whether URLs without an HTTP(S) prefix should be checked.")
        public boolean urlCheckNoPrefix = false;
        @Comment("Whether to check sensitive English words.")
        public boolean enableWordCheck = true;
        @Comment("Whether to check IPv4 addresses.")
        public boolean enableIpCheck = false;
        @Comment("Whether to stop matching after the first short-word match.")
        public boolean failFast = true;
        @Comment("Additional blocked words.")
        public List<String> blackList = new ArrayList<>(List.of("失业"));
        @Comment("Words exempt from filtering.")
        public List<String> whiteList = new ArrayList<>(List.of("3p"));
    }

    @Configuration
    public static final class Compatibility {
        @Comment("Whether to skip checks for players who have not logged in through AuthMe.")
        public boolean authMe = false;
    }

    @Configuration
    public static final class Chat {
        @Comment("Action after detection: REPLACE or CANCEL.")
        public ProcessMethod method = ProcessMethod.REPLACE;
        @Comment("Whether to show a fake message to the sender after cancellation.")
        public boolean fakeMessageOnCancel = false;
        @Comment("Whether to send the player a violation message.")
        public boolean sendMessage = true;
        @Comment("Whether to apply configured punishments.")
        public boolean punish = true;
        @Comment("Whether to check server broadcast messages.")
        public boolean broadcastCheck = false;
        @Comment("Whether to detect blocked words split across recent chat messages.")
        public boolean contextCheck = true;
        @Comment("Maximum number of chat messages retained for context checks.")
        public int contextMaxSize = 4;
        @Comment("Maximum chat context age in seconds.")
        public int contextMaxTime = 90;
        @Comment("Whether command whitelist matching is inverted into a blacklist.")
        public boolean invertCommandWhiteList = true;
        @Comment("Commands excluded from filtering unless the whitelist is inverted.")
        public List<String> commandWhiteList = new ArrayList<>(List.of(
                "/tell", "/msg", "/normal", "/message", "/private", "/msg", "/w", "/whisper", "/m"
        ));
    }

    @Configuration
    public static final class Book {
        @Comment("Action after detection: REPLACE or CANCEL.")
        public ProcessMethod method = ProcessMethod.REPLACE;
        @Comment("Whether to remove newline characters before checking pages.")
        public boolean ignoreNewLine = true;
        @Comment("Whether to check blocked words split across pages. Cancel mode only.")
        public boolean crossPageCheck = false;
        @Comment("Whether to send the player a violation message.")
        public boolean sendMessage = true;
        @Comment("Whether to apply configured punishments.")
        public boolean punish = true;
        @Comment("Book detection cache settings.")
        public Cache cache = new Cache();
    }

    @Configuration
    public static final class Cache {
        @Comment("Whether to cache processed book content.")
        public boolean enableCache = true;
        @Comment("Maximum number of cached books.")
        public int maximumCacheSize = 400;
        @Comment("Book-cache expiry time in minutes.")
        public int expireTime = 10;
        @Comment("Whether to clear the book cache during a full reload.")
        public boolean clearOnReload = false;
    }

    @Configuration
    public static final class Sign {
        @Comment("Action after detection: REPLACE or CANCEL.")
        public ProcessMethod method = ProcessMethod.REPLACE;
        @Comment("Whether cancelled edits should be shown only to their author through PacketEvents.")
        public boolean fakeOnCancel = false;
        @Comment("Whether to apply configured punishments.")
        public boolean punish = true;
        @Comment("Whether to check words split across sign lines.")
        public boolean multiLineCheck = true;
        @Comment("Whether to check blocked words split across recently edited signs.")
        public boolean contextCheck = false;
        @Comment("Maximum number of sign edits retained for context checks.")
        public int contextMaxSize = 4;
        @Comment("Maximum sign context age in seconds.")
        public int contextMaxTime = 120;
        @Comment("Whether to send the player a violation message.")
        public boolean sendMessage = true;
    }

    @Configuration
    public static final class Anvil {
        @Comment("Action after detection: REPLACE or CANCEL.")
        public ProcessMethod method = ProcessMethod.REPLACE;
        @Comment("Whether to send the player a violation message.")
        public boolean sendMessage = true;
        @Comment("Whether to apply configured punishments.")
        public boolean punish = true;
    }

    @Configuration
    public static final class Name {
        @Comment("Whether to send a kick message when a name is blocked.")
        public boolean sendMessage = true;
        @Comment("Whether to skip Floodgate Bedrock player names.")
        public boolean ignoreBedrock = false;
    }

    @Configuration
    public static final class Item {
        @Comment("Action after detection: REPLACE or CANCEL.")
        public ProcessMethod method = ProcessMethod.REPLACE;
        @Comment("Whether to send the player a violation message.")
        public boolean sendMessage = true;
        @Comment("Whether to apply configured punishments.")
        public boolean punish = true;
    }
}
