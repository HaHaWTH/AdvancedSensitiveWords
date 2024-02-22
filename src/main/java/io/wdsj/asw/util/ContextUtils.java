package io.wdsj.asw.util;

import io.wdsj.asw.setting.PluginSettings;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import static io.wdsj.asw.AdvancedSensitiveWords.settingsManager;

public class ContextUtils {
    private static final ConcurrentHashMap<Player, Queue<String>> chatHistory = new ConcurrentHashMap<>();

    /**
     * Add player message to history
     */
    public static void addMessage(Player player, String message) {
        chatHistory.computeIfAbsent(player, k -> new LinkedList<>());
        Queue<String> history = chatHistory.get(player);
        while (history.size() >= settingsManager.getProperty(PluginSettings.CHAT_CONTEXT_MAX_SIZE)) {
            history.poll();
        }
        history.offer(message.trim());
    }

    public static Queue<String> getHistory(Player player) {
        return chatHistory.getOrDefault(player, new LinkedList<>());
    }

    public static void clearPlayerContext(Player player) {
        if (chatHistory.get(player) == null) return;
        chatHistory.remove(player);
    }
    public static void forceClearContext() {
        chatHistory.clear();
    }

    private ContextUtils() {}
}
