package io.wdsj.asw.bukkit.util.context;

import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.common.datatype.TimedString;
import org.bukkit.entity.Player;

import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class ChatContext {
    private static final ConcurrentHashMap<UUID, Deque<TimedString>> chatHistory = new ConcurrentHashMap<>();
    /**
     * Add player message to history
     */
    public static void addMessage(Player player, String message) {
        final UUID uuid = player.getUniqueId();
        chatHistory.computeIfAbsent(uuid, k -> new ConcurrentLinkedDeque<>());
        Deque<TimedString> history = chatHistory.get(uuid);
        while (history.size() >= settingsManager.getProperty(PluginSettings.CHAT_CONTEXT_MAX_SIZE)) {
            history.pollFirst();
        }
        if (message.trim().isEmpty()) return;
        history.offerLast(TimedString.of(message.trim()));
    }

    public static Deque<String> getHistory(Player player) {
        final UUID uuid = player.getUniqueId();
        Deque<TimedString> tsHistory = chatHistory.getOrDefault(uuid, new ConcurrentLinkedDeque<>());
        if (tsHistory.isEmpty()) return new ConcurrentLinkedDeque<>();
        tsHistory.removeIf(timedString -> (System.currentTimeMillis() - timedString.getTime()) / 1000 > settingsManager.getProperty(PluginSettings.CHAT_CONTEXT_TIME_LIMIT));
        return tsHistory.stream()
                .map(TimedString::getString)
                .collect(ConcurrentLinkedDeque::new, ConcurrentLinkedDeque::offerLast, ConcurrentLinkedDeque::addAll);
    }

    public static void clearPlayerContext(Player player) {
        final UUID uuid = player.getUniqueId();
        if (chatHistory.get(uuid) == null) return;
        chatHistory.remove(uuid);
    }

    public static void pollPlayerContext(Player player) {
        final UUID uuid = player.getUniqueId();
        Deque<TimedString> history = chatHistory.get(uuid);
        if (history != null) history.pollLast();
    }
    public static void forceClearContext() {
        chatHistory.clear();
    }

    private ChatContext() {}
}
