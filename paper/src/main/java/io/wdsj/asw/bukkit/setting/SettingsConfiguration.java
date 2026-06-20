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
    @Comment({"LLM-assisted chat moderation settings."})
    public Ai ai = new Ai();
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
        @Comment({
                "VL conditions in this manual punishment list use the player's total violations across all punishment modules.",
                "Default actions for /asw player punish <player> when no method is supplied.",
                "Each item uses TYPE|argument...|VLcondition. An empty list disables default manual punishments.",
                "COMMAND|command; COMMAND_PROXY|command; DAMAGE|amount; HOSTILE|radius.",
                "EFFECT|effect[|seconds[|amplifier]]; SHADOW|seconds.",
                "Use %player% or %PLAYER% in commands. Append VL=3, VL>3, or VL<3 to condition an action."
        })
        public List<String> manualPunishment = new ArrayList<>();
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
        @Comment("Punishment actions for chat and command violations. Leave empty to disable automatic punishment.")
        public List<String> punishment = new ArrayList<>();
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
    public static final class Ai {
        @Comment("Whether to enable LLM-assisted chat moderation.")
        public boolean enabled = false;
        @Comment("OpenAI-compatible API base URL.")
        public String baseUrl = "https://api.deepseek.com";
        @Comment("Environment variable that contains the API key. It overrides api-key when set.")
        public String apiKeyEnvironment = "DEEPSEEK_API_KEY";
        @Comment("Fallback API key. Prefer an environment variable instead.")
        public String apiKey = "";
        @Comment("OpenAI-compatible model name.")
        public String modelName = "deepseek-v4-flash";
        @Comment("LLM request timeout in seconds. Must be at least 1; 5-30 is recommended.")
        public int requestTimeoutSeconds = 12;
        @Comment("Maximum completion tokens returned by the model. Must be at least 1; 128-256 is recommended for the fixed JSON response.")
        public int maxOutputTokens = 256;
        @Comment("Sampling temperature used for classification. Must be a finite value >= 0; use 0.0 for deterministic moderation.")
        public double temperature = 0.0D;
        @Comment("Whether to log raw LLM responses at INFO level for debugging. Keep disabled in production because a provider can return sensitive text.")
        public boolean logResponses = false;
        @Comment("Maximum concurrent virtual-thread LLM requests. Must be at least 1; keep this within your provider rate limit, usually 2-8.")
        public int maxConcurrentRequests = 4;
        @Comment("Maximum queued LLM requests before new candidates are dropped. Must be at least 1; 16-64 limits memory and delayed punishments.")
        public int queueCapacity = 32;
        @Comment("Minimum seconds between LLM requests from the same player. Must be >= 0; 5-20 reduces spam and provider cost.")
        public int perPlayerCooldownSeconds = 10;
        @Comment("Minimum visible Unicode code points before an LLM request is considered. Must be >= 1; 4-8 saves requests for short messages.")
        public int minimumMessageCodePoints = 6;
        @Comment("Maximum total Unicode code points accepted by the LLM request gate. Must be >= minimum-message-code-points; 128-512 is recommended.")
        public int maximumMessageCodePoints = 256;
        @Comment("Minimum Shannon entropy in bits per visible Unicode code point. Must be finite and >= 0; 2.0-3.5 is a typical request-saving gate.")
        public double minimumEntropyBits = 2.5D;
        @Comment("Minimum LLM confidence required for ASW automatic enforcement. Must be between 0.0 and 1.0; 0.85-0.95 is recommended.")
        public double minimumConfidence = 0.90D;
        @Comment("LLM categories that ASW may automatically enforce.")
        public List<String> enforcedCategories = new ArrayList<>(List.of(
                "harassment",
                "hate",
                "sexual",
                "sexual_minors",
                "self_harm",
                "violence_threat",
                "illegal",
                "privacy_doxxing",
                "spam_scam"
        ));
        @Comment("Punishment actions for enforced AI classifications. Leave empty to record and notify without automatic punishment.")
        public List<String> punishment = new ArrayList<>();
        @Comment("Trusted server context sent with each request. Leave blank unless required.")
        public String serverContext = "";
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
        @Comment("Punishment actions for book violations. Leave empty to disable automatic punishment.")
        public List<String> punishment = new ArrayList<>();
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
        @Comment("Punishment actions for sign violations. Leave empty to disable automatic punishment.")
        public List<String> punishment = new ArrayList<>();
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
        @Comment("Punishment actions for anvil violations. Leave empty to disable automatic punishment.")
        public List<String> punishment = new ArrayList<>();
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
        @Comment("Punishment actions for item violations. Leave empty to disable automatic punishment.")
        public List<String> punishment = new ArrayList<>();
    }
}
