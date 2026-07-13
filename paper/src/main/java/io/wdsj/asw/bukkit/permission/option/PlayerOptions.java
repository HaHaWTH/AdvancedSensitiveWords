package io.wdsj.asw.bukkit.permission.option;

/**
 * Player-scoped option permission paths below {@code advancedsensitivewords.option}.
 */
public final class PlayerOptions {
    public static final String CHAT_METHOD = "chat.method";
    public static final String CHAT_FAKE_MESSAGE_ON_CANCEL = "chat.fake-message-on-cancel";
    public static final String CHAT_SEND_MESSAGE = "chat.send-message";
    public static final String CHAT_CONTEXT_CHECK = "chat.context-check";
    public static final String CHAT_ANTI_SPAM_ENABLED = "chat.anti-spam.enabled";
    public static final String CHAT_ANTI_SPAM_SEND_MESSAGE = "chat.anti-spam.send-message";
    public static final String CHAT_ANTI_SPAM_RATE_LIMIT_ENABLED = "chat.anti-spam.rate-limit.enabled";
    public static final String CHAT_ANTI_SPAM_MINIMUM_ENTROPY_CODE_POINTS = "chat.anti-spam.minimum-entropy-code-points";
    public static final String CHAT_ANTI_SPAM_MINIMUM_SIMILARITY_CODE_POINTS = "chat.anti-spam.minimum-similarity-code-points";
    public static final String CHAT_ANTI_SPAM_HISTORY_SIZE = "chat.anti-spam.history-size";
    public static final String CHAT_ANTI_SPAM_HISTORY_MAX_AGE_SECONDS = "chat.anti-spam.history-max-age-seconds";
    public static final String CHAT_ANTI_SPAM_SIMILAR_CHECK_AMOUNT = "chat.anti-spam.similar-check-amount";
    public static final String CHAT_ANTI_SPAM_SIMILAR_MIN_DISTANCE = "chat.anti-spam.similar-min-distance";
    public static final String CHAT_ANTI_SPAM_SIMILAR_MAX_SIMILARITY = "chat.anti-spam.similar-max-similarity";
    public static final String CHAT_ANTI_SPAM_RATE_LIMIT_CAPACITY = "chat.anti-spam.rate-limit.capacity";
    public static final String CHAT_ANTI_SPAM_RATE_LIMIT_REFILL_INTERVAL_SECONDS = "chat.anti-spam.rate-limit.refill-interval-seconds";

    public static final String BOOK_METHOD = "book.method";
    public static final String BOOK_IGNORE_NEW_LINE = "book.ignore-new-line";
    public static final String BOOK_CROSS_PAGE_CHECK = "book.cross-page-check";
    public static final String BOOK_SEND_MESSAGE = "book.send-message";
    public static final String BOOK_CACHE_ENABLE_CACHE = "book.cache.enable-cache";

    public static final String SIGN_METHOD = "sign.method";
    public static final String SIGN_FAKE_ON_CANCEL = "sign.fake-on-cancel";
    public static final String SIGN_MULTI_LINE_CHECK = "sign.multi-line-check";
    public static final String SIGN_CONTEXT_CHECK = "sign.context-check";
    public static final String SIGN_CONTEXT_MAX_SIZE = "sign.context-max-size";
    public static final String SIGN_CONTEXT_MAX_TIME = "sign.context-max-time";
    public static final String SIGN_SEND_MESSAGE = "sign.send-message";

    public static final String ANVIL_METHOD = "anvil.method";
    public static final String ANVIL_SEND_MESSAGE = "anvil.send-message";

    public static final String ITEM_METHOD = "item.method";
    public static final String ITEM_SEND_MESSAGE = "item.send-message";

    public static final String AI_ENABLED = "ai.enabled";
    public static final String AI_MINIMUM_MESSAGE_CODE_POINTS = "ai.minimum-message-code-points";
    public static final String AI_MAXIMUM_MESSAGE_CODE_POINTS = "ai.maximum-message-code-points";
    public static final String AI_MINIMUM_ENTROPY_BITS = "ai.minimum-entropy-bits";
    public static final String AI_PER_PLAYER_COOLDOWN_SECONDS = "ai.per-player-cooldown-seconds";

    private PlayerOptions() {
    }
}
