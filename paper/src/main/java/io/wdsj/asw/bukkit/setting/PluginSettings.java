package io.wdsj.asw.bukkit.setting;

import java.util.List;
import io.wdsj.asw.bukkit.ai.LlmApiMode;
import io.wdsj.asw.bukkit.type.ProcessMethod;

/**
 * Type-safe keys for values in {@link SettingsConfiguration}.
 */
public final class PluginSettings {
    public static final SettingKey<String> PLUGIN_LANGUAGE = key(settings -> settings.plugin.language);
    public static final SettingKey<Boolean> ENABLE_DEFAULT_WORDS = key(settings -> settings.plugin.enableDefaultWords);
    public static final SettingKey<Boolean> CHECK_FOR_UPDATE = key(settings -> settings.plugin.checkForUpdate);
    public static final SettingKey<Boolean> ENABLE_ONLINE_WORDS = key(settings -> settings.plugin.enableOnlineWords);
    public static final SettingKey<String> ONLINE_WORDS_URL = key(settings -> settings.plugin.onlineWordsUrl);
    public static final SettingKey<String> ONLINE_WORDS_ENCODING = key(settings -> settings.plugin.onlineWordsEncoding);
    public static final SettingKey<Boolean> CACHE_ONLINE_WORDS = key(settings -> settings.plugin.cacheOnlineWords);
    public static final SettingKey<Long> ONLINE_WORDS_CACHE_TIMEOUT = key(settings -> settings.plugin.onlineWordsCacheTimeout);
    public static final SettingKey<Boolean> LOG_VIOLATION = key(settings -> settings.plugin.logViolation);
    public static final SettingKey<Boolean> NOTICE_OPERATOR = key(settings -> settings.plugin.noticeOperator);
    public static final SettingKey<List<String>> MANUAL_PUNISHMENT = key(settings -> settings.plugin.manualPunishment);
    public static final SettingKey<Long> VIOLATION_RESET_TIME = key(settings -> settings.plugin.violationResetTime);
    public static final SettingKey<Boolean> ONLY_RESET_ONLINE_PLAYERS = key(settings -> settings.plugin.onlyResetOnlinePlayers);
    public static final SettingKey<Boolean> PURGE_LOG_FILE = key(settings -> settings.plugin.purgeLogFile);
    public static final SettingKey<String> REPLACEMENT = key(settings -> settings.plugin.replacement);
    public static final SettingKey<List<String>> DEFINED_REPLACEMENT = key(settings -> settings.plugin.definedReplacement);
    public static final SettingKey<Boolean> ENABLE_CHAT_CHECK = key(settings -> settings.plugin.enableChatCheck);
    public static final SettingKey<Boolean> ENABLE_SIGN_EDIT_CHECK = key(settings -> settings.plugin.enableSignEditCheck);
    public static final SettingKey<Boolean> ENABLE_ANVIL_EDIT_CHECK = key(settings -> settings.plugin.enableAnvilEditCheck);
    public static final SettingKey<Boolean> ENABLE_BOOK_EDIT_CHECK = key(settings -> settings.plugin.enableBookEditCheck);
    public static final SettingKey<Boolean> ENABLE_PLAYER_NAME_CHECK = key(settings -> settings.plugin.enablePlayerNameCheck);
    public static final SettingKey<Boolean> ENABLE_PLAYER_ITEM_CHECK = key(settings -> settings.plugin.enablePlayerItemCheck);
    public static final SettingKey<Boolean> ENABLE_ALTS_CHECK = key(settings -> settings.plugin.enableAltsCheck);
    public static final SettingKey<Boolean> CLEAN_PLAYER_DATA_CACHE = key(settings -> settings.plugin.cleanPlayerDataCache);
    public static final SettingKey<Boolean> ENABLE_PLACEHOLDER = key(settings -> settings.plugin.enablePlaceholder);
    public static final SettingKey<Boolean> HOOK_VELOCITY = key(settings -> settings.plugin.hookVelocity);
    public static final SettingKey<Boolean> ENABLE_AUTHME_COMPATIBILITY = key(settings -> settings.plugin.compatibility.authMe);
    public static final SettingKey<String> IGNORE_CHAR = key(settings -> settings.plugin.ignoreChar);
    public static final SettingKey<Boolean> PRE_PROCESS = key(settings -> settings.plugin.enablePreProcess);
    public static final SettingKey<String> PRE_PROCESS_REGEX = key(settings -> settings.plugin.preProcessRegex);
    public static final SettingKey<Boolean> IGNORE_CASE = key(settings -> settings.plugin.ignoreCase);
    public static final SettingKey<Boolean> IGNORE_WIDTH = key(settings -> settings.plugin.ignoreWidth);
    public static final SettingKey<Boolean> IGNORE_NUM_STYLE = key(settings -> settings.plugin.ignoreNumStyle);
    public static final SettingKey<Boolean> IGNORE_CHINESE_STYLE = key(settings -> settings.plugin.ignoreChineseStyle);
    public static final SettingKey<Boolean> IGNORE_ENGLISH_STYLE = key(settings -> settings.plugin.ignoreEnglishStyle);
    public static final SettingKey<Integer> FULL_MATCH_MODE = key(settings -> settings.plugin.fullMatchMode);
    public static final SettingKey<Boolean> IGNORE_REPEAT = key(settings -> settings.plugin.ignoreRepeat);
    public static final SettingKey<Boolean> ENABLE_NUM_CHECK = key(settings -> settings.plugin.enableNumCheck);
    public static final SettingKey<Integer> NUM_CHECK_LEN = key(settings -> settings.plugin.numCheckLen);
    public static final SettingKey<Boolean> ENABLE_EMAIL_CHECK = key(settings -> settings.plugin.enableEmailCheck);
    public static final SettingKey<Boolean> ENABLE_URL_CHECK = key(settings -> settings.plugin.enableUrlCheck);
    public static final SettingKey<Boolean> URL_CHECK_NO_PREFIX = key(settings -> settings.plugin.urlCheckNoPrefix);
    public static final SettingKey<Boolean> ENABLE_WORD_CHECK = key(settings -> settings.plugin.enableWordCheck);
    public static final SettingKey<Boolean> ENABLE_IP_CHECK = key(settings -> settings.plugin.enableIpCheck);
    public static final SettingKey<Boolean> FAIL_FAST = key(settings -> settings.plugin.failFast);
    public static final SettingKey<List<String>> BLACK_LIST = key(settings -> settings.plugin.blackList);
    public static final SettingKey<List<String>> WHITE_LIST = key(settings -> settings.plugin.whiteList);

    public static final SettingKey<ProcessMethod> CHAT_METHOD = key(settings -> settings.chat.method);
    public static final SettingKey<Boolean> CHAT_FAKE_MESSAGE_ON_CANCEL = key(settings -> settings.chat.fakeMessageOnCancel);
    public static final SettingKey<Boolean> CHAT_SEND_MESSAGE = key(settings -> settings.chat.sendMessage);
    public static final SettingKey<List<String>> CHAT_PUNISHMENT = key(settings -> settings.chat.punishment);
    public static final SettingKey<Boolean> CHAT_BROADCAST_CHECK = key(settings -> settings.chat.broadcastCheck);
    public static final SettingKey<Boolean> CHAT_CONTEXT_CHECK = key(settings -> settings.chat.contextCheck);
    public static final SettingKey<Integer> CHAT_CONTEXT_MAX_SIZE = key(settings -> settings.chat.contextMaxSize);
    public static final SettingKey<Integer> CHAT_CONTEXT_TIME_LIMIT = key(settings -> settings.chat.contextMaxTime);
    public static final SettingKey<Boolean> CHAT_INVERT_WHITELIST = key(settings -> settings.chat.invertCommandWhiteList);
    public static final SettingKey<List<String>> CHAT_COMMAND_WHITE_LIST = key(settings -> settings.chat.commandWhiteList);

    public static final SettingKey<Boolean> AI_ENABLED = key(settings -> settings.ai.enabled);
    public static final SettingKey<String> AI_BASE_URL = key(settings -> settings.ai.baseUrl);
    public static final SettingKey<LlmApiMode> AI_API_MODE = key(settings -> settings.ai.apiMode);
    public static final SettingKey<String> AI_ANTHROPIC_VERSION = key(settings -> settings.ai.anthropicVersion);
    public static final SettingKey<Boolean> AI_ANTHROPIC_THINKING_ENABLED = key(settings -> settings.ai.anthropicThinkingEnabled);
    public static final SettingKey<String> AI_API_KEY_ENVIRONMENT = key(settings -> settings.ai.apiKeyEnvironment);
    public static final SettingKey<String> AI_API_KEY = key(settings -> settings.ai.apiKey);
    public static final SettingKey<String> AI_MODEL_NAME = key(settings -> settings.ai.modelName);
    public static final SettingKey<Integer> AI_REQUEST_TIMEOUT_SECONDS = key(settings -> settings.ai.requestTimeoutSeconds);
    public static final SettingKey<Integer> AI_MAX_OUTPUT_TOKENS = key(settings -> settings.ai.maxOutputTokens);
    public static final SettingKey<Double> AI_TEMPERATURE = key(settings -> settings.ai.temperature);
    public static final SettingKey<Boolean> AI_LOG_RESPONSES = key(settings -> settings.ai.logResponses);
    public static final SettingKey<Integer> AI_MAX_CONCURRENT_REQUESTS = key(settings -> settings.ai.maxConcurrentRequests);
    public static final SettingKey<Integer> AI_QUEUE_CAPACITY = key(settings -> settings.ai.queueCapacity);
    public static final SettingKey<Integer> AI_PLAYER_COOLDOWN_SECONDS = key(settings -> settings.ai.perPlayerCooldownSeconds);
    public static final SettingKey<Integer> AI_MINIMUM_MESSAGE_CODE_POINTS = key(settings -> settings.ai.minimumMessageCodePoints);
    public static final SettingKey<Integer> AI_MAXIMUM_MESSAGE_CODE_POINTS = key(settings -> settings.ai.maximumMessageCodePoints);
    public static final SettingKey<Double> AI_MINIMUM_ENTROPY_BITS = key(settings -> settings.ai.minimumEntropyBits);
    public static final SettingKey<java.util.Map<String, SettingsConfiguration.Ai.CategoryPolicy>> AI_CATEGORY_POLICY = key(settings -> settings.ai.categoryPolicy);
    public static final SettingKey<String> AI_SERVER_CONTEXT = key(settings -> settings.ai.serverContext);
    public static final SettingKey<Boolean> AI_SERVER_CONTEXT_CAN_OVERRIDE = key(settings -> settings.ai.serverContextCanOverride);

    public static final SettingKey<ProcessMethod> BOOK_METHOD = key(settings -> settings.book.method);
    public static final SettingKey<Boolean> BOOK_IGNORE_NEWLINE = key(settings -> settings.book.ignoreNewLine);
    public static final SettingKey<Boolean> BOOK_CROSS_PAGE = key(settings -> settings.book.crossPageCheck);
    public static final SettingKey<Boolean> BOOK_SEND_MESSAGE = key(settings -> settings.book.sendMessage);
    public static final SettingKey<List<String>> BOOK_PUNISHMENT = key(settings -> settings.book.punishment);
    public static final SettingKey<Boolean> BOOK_CACHE = key(settings -> settings.book.cache.enableCache);
    public static final SettingKey<Integer> BOOK_MAXIMUM_CACHE_SIZE = key(settings -> settings.book.cache.maximumCacheSize);
    public static final SettingKey<Integer> BOOK_CACHE_EXPIRE_TIME = key(settings -> settings.book.cache.expireTime);
    public static final SettingKey<Boolean> BOOK_CACHE_CLEAR_ON_RELOAD = key(settings -> settings.book.cache.clearOnReload);

    public static final SettingKey<ProcessMethod> SIGN_METHOD = key(settings -> settings.sign.method);
    public static final SettingKey<Boolean> SIGN_FAKE_ON_CANCEL = key(settings -> settings.sign.fakeOnCancel);
    public static final SettingKey<List<String>> SIGN_PUNISHMENT = key(settings -> settings.sign.punishment);
    public static final SettingKey<Boolean> SIGN_MULTI_LINE_CHECK = key(settings -> settings.sign.multiLineCheck);
    public static final SettingKey<Boolean> SIGN_CONTEXT_CHECK = key(settings -> settings.sign.contextCheck);
    public static final SettingKey<Integer> SIGN_CONTEXT_MAX_SIZE = key(settings -> settings.sign.contextMaxSize);
    public static final SettingKey<Integer> SIGN_CONTEXT_TIME_LIMIT = key(settings -> settings.sign.contextMaxTime);
    public static final SettingKey<Boolean> SIGN_SEND_MESSAGE = key(settings -> settings.sign.sendMessage);

    public static final SettingKey<ProcessMethod> ANVIL_METHOD = key(settings -> settings.anvil.method);
    public static final SettingKey<Boolean> ANVIL_SEND_MESSAGE = key(settings -> settings.anvil.sendMessage);
    public static final SettingKey<List<String>> ANVIL_PUNISHMENT = key(settings -> settings.anvil.punishment);
    public static final SettingKey<Boolean> NAME_SEND_MESSAGE = key(settings -> settings.name.sendMessage);
    public static final SettingKey<Boolean> NAME_IGNORE_BEDROCK = key(settings -> settings.name.ignoreBedrock);
    public static final SettingKey<ProcessMethod> ITEM_METHOD = key(settings -> settings.item.method);
    public static final SettingKey<Boolean> ITEM_SEND_MESSAGE = key(settings -> settings.item.sendMessage);
    public static final SettingKey<List<String>> ITEM_PUNISHMENT = key(settings -> settings.item.punishment);

    private PluginSettings() {
    }

    private static <T> SettingKey<T> key(java.util.function.Function<SettingsConfiguration, T> accessor) {
        return new SettingKey<>(accessor);
    }
}
