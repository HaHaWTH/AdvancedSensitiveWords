package io.wdsj.asw.bukkit.util.context;

import io.wdsj.asw.bukkit.setting.PluginSettings;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class ChatContext {
    private static final ConcurrentHashMap<Player, Deque<TimedString>> chatHistory = new ConcurrentHashMap<>();
    /**
     * Add player message to history
     */
    public static void addMessage(Player player, String message) {
        chatHistory.computeIfAbsent(player, k -> new ArrayDeque<>());
        Deque<TimedString> history = chatHistory.get(player);
        while (history.size() >= settingsManager.getProperty(PluginSettings.CHAT_CONTEXT_MAX_SIZE)) {
            history.pollFirst();
        }
        if (message.trim().isEmpty()) return;
        history.offerLast(new TimedString(message.trim(), System.currentTimeMillis()));
    }

    public static Deque<String> getHistory(Player player) {
        Deque<TimedString> tsHistory = chatHistory.getOrDefault(player, new ArrayDeque<>());
        return tsHistory.stream()
                .filter(timedString -> (System.currentTimeMillis() - timedString.getTime()) / 1000 < settingsManager.getProperty(PluginSettings.CHAT_CONTEXT_TIME_LIMIT))
                .map(TimedString::getMessage)
                .collect(ArrayDeque::new, ArrayDeque::add, ArrayDeque::addAll);
    }

    public static void clearPlayerContext(Player player) {
        if (chatHistory.get(player) == null) return;
        chatHistory.remove(player);
    }

    public static void removePlayerContext(Player player) {
        Deque<TimedString> history = chatHistory.get(player);
        if (history != null) history.pollLast();
    }
    public static void forceClearContext() {
        chatHistory.clear();
    }

    private static class TimedString {
        private final String message;
        private final long time;

        public TimedString(String message, long time) {
            this.message = message;
            this.time = time;
        }

        public String getMessage() {
            return message;
        }

        public long getTime() {
            return time;
        }
    }
    private ChatContext() {}
}
