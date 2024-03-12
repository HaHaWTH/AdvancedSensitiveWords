package io.wdsj.asw.setting;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.Property;

import java.util.List;

import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public class PluginSettings implements SettingsHolder {
    @Comment({"插件语言(zhcn/en)(重启服务器生效)",
            "Plugin language: (zhcn/en)(Require a server restart)"})
    public static final Property<String> PLUGIN_LANGUAGE = newProperty("Plugin.language", "zhcn");

    @Comment({"是否启用默认词库(6w+)(强烈建议开启)",
            "Whether to enable the default word library (60k+ words) (strongly recommended)"})
    public static final Property<Boolean> ENABLE_DEFAULT_WORDS = newProperty("Plugin.enableDefaultWords", true);
    @Comment({"是否启用更新检查",
            "Whether to enable updater"})
    public static final Property<Boolean> CHECK_FOR_UPDATE = newProperty("Plugin.checkForUpdate", true);

    @Comment({"是否启用在线词库",
            "Whether to enable online word library"})
    public static final Property<Boolean> ENABLE_ONLINE_WORDS = newProperty("Plugin.enableOnlineWords", false);

    @Comment({"在线词库地址",
            "Online word library URL"})
    public static final Property<String> ONLINE_WORDS_URL = newProperty("Plugin.onlineWordsUrl", "https://raw.githubusercontent.com/HaHaWTH/ASW-OnlineWordList/main/lists.txt");

    @Comment({"在线文本字符编码(UTF-8/GBK)",
            "Online words text encoding (UTF-8/GBK)"})
    public static final Property<String> ONLINE_WORDS_ENCODING = newProperty("Plugin.onlineWordsEncoding", "UTF-8");

    @Comment({"是否记录违规消息(存储在violations.log中)",
            "Whether to log violations (stored in violations.log)"})
    public static final Property<Boolean> LOG_VIOLATION = newProperty("Plugin.logViolation", true);
    @Comment({"玩家违规时是否提醒管理员(需权限advancedsensitivewords.notice)",
            "Notify operators when player violated"})
    public static final Property<Boolean> NOTICE_OPERATOR = newProperty("Plugin.noticeOperator", true);
    @Comment({"玩家违规时的惩罚列表(留空为不进行惩罚)",
            "Punishment after player swore",
            "Visit https://github.com/HaHaWTH/AdvancedSensitiveWords/wiki/ for more detail"})
    public static final Property<List<String>> PUNISHMENT = newListProperty("Plugin.punishment");
    @Comment({"是否在插件启动时自动清除旧的日志文件",
            "Whether to automatically clear old log files on plugin startup"})
    public static final Property<Boolean> PURGE_LOG_FILE = newProperty("Plugin.purgeLogFile", false);

    @Comment({"敏感词替换符号",
            "Replacement symbol for sensitive words"})
    public static final Property<String> REPLACEMENT = newProperty("Plugin.replacement", "*");

    @Comment({"预定义替换(需要先加入blackList)(如包含相同敏感词,长的放前面)(用|来分隔敏感词和替换词)",
            "Predefined replacements (need to be added to blacklist first) (If there are identical sensitive words, place the longer one first) (Use '|' to separate sensitive words and their replacements)"})
    public static final Property<List<String>> DEFINED_REPLACEMENT = newListProperty("Plugin.definedReplacement", "失业|灵活就业");

    @Comment({"*是否启用告示牌检测",
            "*Whether to enable sign edit checks"})
    public static final Property<Boolean> ENABLE_SIGN_EDIT_CHECK = newProperty("Plugin.enableSignEditCheck", true);

    @Comment({"*是否启用铁砧重命名检测",
            "*Whether to enable anvil edit checks"})
    public static final Property<Boolean> ENABLE_ANVIL_EDIT_CHECK = newProperty("Plugin.enableAnvilEditCheck", true);

    @Comment({"*是否启用书检测",
            "*Whether to enable book checks"})
    public static final Property<Boolean> ENABLE_BOOK_EDIT_CHECK = newProperty("Plugin.enableBookEditCheck", true);

    @Comment({"*是否启用玩家名称检测(推荐支持中文名的服务器开启)",
            "*Whether to enable player name checks (recommended for servers supporting Non-ASCII character names)"})
    public static final Property<Boolean> ENABLE_PLAYER_NAME_CHECK = newProperty("Plugin.enablePlayerNameCheck", false);
    @Comment({"*是否启用玩家物品检测",
            "*Whether to enable player item check"})
    public static final Property<Boolean> ENABLE_PLAYER_ITEM_CHECK = newProperty("Plugin.enableItemCheck", true);

    @Comment({"是否启用API接口(非必要请勿关闭)",
            "Whether to enable API (do not disable unless necessary)"})
    public static final Property<Boolean> ENABLE_API = newProperty("Plugin.enableApi", true);

    @Comment({"是否启用AuthMe兼容(在玩家未登录时不进行检测, 避免误判)",
            "Whether to enable AuthMe compatibility (no checks on players not logged in to avoid false positives)"})
    public static final Property<Boolean> ENABLE_AUTHME_COMPATIBILITY = newProperty("Plugin.compatibility.authMe", false);

    @Comment({"是否启用CatSeedLogin兼容(在玩家未登录时不进行检测, 避免误判)",
            "Whether to enable CatSeedLogin compatibility (no checks on players not logged in to avoid false positives)"})
    public static final Property<Boolean> ENABLE_CSL_COMPATIBILITY = newProperty("Plugin.compatibility.catSeedLogin", false);

    @Comment({"默认跳过字符",
            "Default characters to ignore"})
    public static final Property<String> IGNORE_CHAR = newProperty("Plugin.ignoreChar", "`-—=~～!！@#$%^&§*()_+[]{}\\|;:'\"“”,，.。、（）<>?？¥【】《》 ");
    @Comment({"忽略格式化符号§",
            "Ignore format codes"})
    public static final Property<Boolean> IGNORE_FORMAT_CODE = newProperty("Plugin.ignoreFormatCode", false);
    @Comment({"忽略备用格式化符号",
            "Alternate code codes to ignore"})
    public static final Property<String> ALT_COLOR_CODE = newProperty("Plugin.altColorCode", "&");

    @Comment({"检测大小写",
            "Whether to ignore case"})
    public static final Property<Boolean> IGNORE_CASE = newProperty("Plugin.ignoreCase", true);

    @Comment({"检测半角&全角字符",
            "Whether to ignore width (half-width and full-width characters)"})
    public static final Property<Boolean> IGNORE_WIDTH = newProperty("Plugin.ignoreWidth", true);

    @Comment({"检测变着花样发送的数字(例:1贰叁④)",
            "Whether to ignore styled numbers (e.g., 1贰叁④)"})
    public static final Property<Boolean> IGNORE_NUM_STYLE = newProperty("Plugin.ignoreNumStyle", true);

    @Comment({"检测变着花样发送的中文(简繁)",
            "Whether to ignore styled Chinese characters (Simplified and Traditional)"})
    public static final Property<Boolean> IGNORE_CHINESE_STYLE = newProperty("Plugin.ignoreChineseStyle", true);

    @Comment({"检测变着花样发送的英文(例：Ⓕⓤc⒦)",
            "Whether to ignore styled English characters (e.g., Ⓕⓤc⒦)"})
    public static final Property<Boolean> IGNORE_ENGLISH_STYLE = newProperty("Plugin.ignoreEnglishStyle", true);

    @Comment({"是否强制英文全词匹配",
            "(例:屏蔽了av但又不想让have被屏蔽)",
            "Whether to force full-word matches for English",
            "(e.g., to block 'av' without blocking 'have')"})
    public static final Property<Boolean> FORCE_ENGLISH_FULL_MATCH = newProperty("Plugin.forceEnglishFullMatch", false);

    @Comment({"检测重复字符(例：SSSSSBBBBB)",
            "Whether to ignore repeated characters (e.g., SSSSSBBBBB)"})
    public static final Property<Boolean> IGNORE_REPEAT = newProperty("Plugin.ignoreRepeat", true);

    @Comment({"启用连续数字检测(通常用于过滤群号&身份证号&手机号等)",
            "Whether to enable consecutive number checks (commonly used for filtering group numbers, ID numbers, phone numbers, etc.)"})
    public static final Property<Boolean> ENABLE_NUM_CHECK = newProperty("Plugin.enableNumCheck", true);

    @Comment({"判定为连续数字的长度",
            "Length considered as consecutive numbers"})
    public static final Property<Integer> NUM_CHECK_LEN = newProperty("Plugin.numCheckLen", 9);

    @Comment({"启用邮箱检测(xxx@xxx.xxx)",
            "Whether to enable email address checks (xxx@xxx.xxx)"})
    public static final Property<Boolean> ENABLE_EMAIL_CHECK = newProperty("Plugin.enableEmailCheck", false);

    @Comment({"启用网址检测(http://xxx.xxx)",
            "Whether to enable URL checks (http://xxx.xxx)"})
    public static final Property<Boolean> ENABLE_URL_CHECK = newProperty("Plugin.enableUrlCheck", true);

    @Comment({"启用敏感英文单词检测",
            "Whether to enable sensitive English word checks"})
    public static final Property<Boolean> ENABLE_WORD_CHECK = newProperty("Plugin.enableWordCheck", true);

    @Comment({"自定义敏感词列表",
            "Custom sensitive word list"})
    public static final Property<List<String>> BLACK_LIST = newListProperty("Plugin.blackList", "失业");

    @Comment({"敏感词白名单",
            "Sensitive word whitelist"})
    public static final Property<List<String>> WHITE_LIST = newListProperty("Plugin.whiteList", "3p");

    @Comment({"替换还是取消(replace/cancel)",
            "Replace or cancel (replace/cancel)"})
    public static final Property<String> CHAT_METHOD = newProperty("Chat.method", "replace");

    @Comment({"取消后是否发送假消息(仅取消模式可用)(支持PAPI)(Inspired by Bilibili Avalon System)",
            "如果命令被取消则不会发送假消息. 避免隐私泄露",
            "Whether to send a fake message after cancellation (only available in cancel mode) (supports PAPI) (Inspired by Bilibili Avalon System)",
            "If a command is cancelled, no fake message will be sent to avoid privacy leaks"})
    public static final Property<Boolean> CHAT_FAKE_MESSAGE_ON_CANCEL = newProperty("Chat.fakeMessageOnCancel", false);

    @Comment({"是否发送消息提醒(和假消息冲突)",
            "Whether to send message alerts (conflicts with fake messages)"})
    public static final Property<Boolean> CHAT_SEND_MESSAGE = newProperty("Chat.sendMessage", true);

    @Comment({"*是否启用服务器广播消息检测(仅提供取消和替换模式,配置跟随聊天检测)(不会触发API事件)",
            "*Whether to enable server broadcast message checks (only offers cancel and replace modes, configuration follows chat checks) (does not trigger API events)"})
    public static final Property<Boolean> CHAT_BROADCAST_CHECK = newProperty("Chat.broadcastCheck", true);

    @Comment({"是否开启聊天上下文检测(仅提供取消和假消息模式,配置跟随聊天检测)",
            "Whether to enable chat context checks (only offers cancel and fake message modes, configuration follows chat checks)"})
    public static final Property<Boolean> CHAT_CONTEXT_CHECK = newProperty("Chat.contextCheck", false);

    @Comment({"最大检测上下文大小",
            "Maximum context size for checks"})
    public static final Property<Integer> CHAT_CONTEXT_MAX_SIZE = newProperty("Chat.contextMaxSize", 4);

    @Comment({"是否反转指令白名单为黑名单",
            "Whether to invert the command whitelist to a blacklist"})
    public static final Property<Boolean> CHAT_INVERT_WHITELIST = newProperty("Chat.invertCommandWhiteList", false);

    @Comment({"指令白名单(白名单的指令如含敏感词不会被检测)",
            "Command whitelist (commands on the whitelist will not be checked for sensitive words)"})
    public static final Property<List<String>> CHAT_COMMAND_WHITE_LIST = newListProperty("Chat.commandWhiteList", "/asw", "/reload", "/help", "/ban",
            "/mute", "/unmute", "/kick", "/unban", "/res", "/sethome", "/home", "/l", "/tp", "/tpa", "/login", "/log", "/register", "/reg", "/lp");

    @Comment({"替换还是取消(replace/cancel)",
            "Replace or cancel (replace/cancel)"})
    public static final Property<String> BOOK_METHOD = newProperty("Book.method", "replace");

    @Comment({"是否跳过换行",
            "Whether to skip newline characters"})
    public static final Property<Boolean> BOOK_IGNORE_NEWLINE = newProperty("Book.ignoreNewLine", true);

    @Comment({"存在敏感词时是否发送消息提醒",
            "Whether to send a message alert when sensitive words are found"})
    public static final Property<Boolean> BOOK_SEND_MESSAGE = newProperty("Book.sendMessage", true);

    @Comment({"是否启用书检测缓存(有助于优化性能)",
            "Whether to enable book check caching (helps optimize performance)"})
    public static final Property<Boolean> BOOK_CACHE = newProperty("Book.cache.enableCache", false);

    @Comment({"最大缓存数量(默认200)",
            "Maximum cache size (default is 200)"})
    public static final Property<Integer> BOOK_MAXIMUM_CACHE_SIZE = newProperty("Book.cache.maximumCacheSize", 200);

    @Comment({"重载时是否清空缓存",
            "Whether to clear cache on reload"})
    public static final Property<Boolean> BOOK_CACHE_CLEAR_ON_RELOAD = newProperty("Book.cache.clearOnReload", false);

    @Comment({"替换还是取消(replace/cancel)",
            "Replace or cancel (replace/cancel)"})
    public static final Property<String> SIGN_METHOD = newProperty("Sign.method", "replace");

    @Comment({"是否启用跨行检测",
            "Whether to enable multi-line check"})
    public static final Property<Boolean> SIGN_MULTI_LINE_CHECK = newProperty("Sign.multiLineCheck", true);

    @Comment({"存在敏感词时是否发送消息提醒",
            "Whether to send a message alert when sensitive words are found"})
    public static final Property<Boolean> SIGN_SEND_MESSAGE = newProperty("Sign.sendMessage", true);

    @Comment({"替换还是取消(replace/cancel)",
            "Replace or cancel (replace/cancel)"})
    public static final Property<String> ANVIL_METHOD = newProperty("Anvil.method", "replace");

    @Comment({"存在敏感词时是否发送消息提醒",
            "Whether to send a message alert when sensitive words are found"})
    public static final Property<Boolean> ANVIL_SEND_MESSAGE = newProperty("Anvil.sendMessage", true);

    @Comment({"替换还是禁止登录(replace/cancel)",
            "Replace or forbid login (replace/cancel)"})
    public static final Property<String> NAME_METHOD = newProperty("Name.method", "cancel");

    @Comment({"存在敏感词时是否发送消息/踢出消息",
            "Whether to send a message/kick message when sensitive words are found"})
    public static final Property<Boolean> NAME_SEND_MESSAGE = newProperty("Name.sendMessage", true);

    @Comment({"是否跳过对基岩版玩家名称的检测(需要floodgate)",
            "Whether to skip checking player names for Bedrock Edition players (requires floodgate)"})
    public static final Property<Boolean> NAME_IGNORE_BEDROCK = newProperty("Name.ignoreBedrock", false);

    @Comment({"是否启用NPC兼容(支持Leaves NPC)",
            "Whether to enable NPC compatibility (supports Leaves NPC)"})
    public static final Property<Boolean> NAME_IGNORE_NPC = newProperty("Name.ignoreNPC", true);
    @Comment({"替换还是取消(replace/cancel)",
            "Replace or cancel (replace/cancel)"})
    public static final Property<String> ITEM_METHOD = newProperty("Item.method", "replace");
    @Comment({"存在敏感词时是否发送消息提醒",
            "Whether to send a message alert when sensitive words are found"})
    public static final Property<Boolean> ITEM_SEND_MESSAGE = newProperty("Item.sendMessage", true);


    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("", "AdvancedSensitiveWords 配置文件", "所有配置项均支持重载(标*的配置项仅支持重载关闭)");
        conf.setComment("Plugin", "插件总配置");
        conf.setComment("Plugin.compatibility", "插件兼容配置");
        conf.setComment("Chat", "聊天检测配置");
        conf.setComment("Book", "书检测配置");
        conf.setComment("Book.cache", "书检测缓存配置");
        conf.setComment("Sign", "告示牌检测配置");
        conf.setComment("Anvil", "铁砧重命名检测配置");
        conf.setComment("Name", "玩家名检测配置");
        conf.setComment("Item", "物品检测配置");
    }

    // Do not instantiate.
    private PluginSettings() {
    }
}
