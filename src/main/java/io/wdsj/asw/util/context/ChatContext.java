package io.wdsj.asw.util.context;

import io.wdsj.asw.setting.PluginSettings;
import org.bukkit.entity.Player;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import static io.wdsj.asw.AdvancedSensitiveWords.settingsManager;

public class ChatContext {
    private static final ConcurrentHashMap<Player, Deque<String>> chatHistory = new ConcurrentHashMap<>();
    /**
     * Add player message to history
     */
    public static void addMessage(Player player, String message) {
        chatHistory.computeIfAbsent(player, k -> new LinkedList<>());
        Deque<String> history = chatHistory.get(player);
        while (history.size() >= settingsManager.getProperty(PluginSettings.CHAT_CONTEXT_MAX_SIZE)) {
            history.poll();
        }
        history.offer(message.trim());
    }

    public static Deque<String> getHistory(Player player) {
        return chatHistory.getOrDefault(player, new LinkedList<>());
    }

    public static void clearPlayerContext(Player player) {
        if (chatHistory.get(player) == null) return;
        chatHistory.remove(player);
    }

    public static void removePlayerContext(Player player) {
        Deque<String> history = chatHistory.get(player);
        if (history != null) history.pollLast();
    }
    public static void forceClearContext() {
        chatHistory.clear();
    }

    private ChatContext() {}
}
