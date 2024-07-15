package io.wdsj.asw.bukkit.util.context;

import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.type.TimedString;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class SignContext {
    private static final ConcurrentHashMap<Player, Deque<TimedString>> signEditHistory = new ConcurrentHashMap<>();
    /**
     * Add player message to history
     */
    public static void addMessage(Player player, String message) {
        signEditHistory.computeIfAbsent(player, k -> new ArrayDeque<>());
        Deque<TimedString> history = signEditHistory.get(player);
        while (history.size() >= settingsManager.getProperty(PluginSettings.SIGN_CONTEXT_MAX_SIZE)) {
            history.pollFirst();
        }
        if (message.trim().isEmpty()) return;
        history.offerLast(new TimedString(message.trim(), System.currentTimeMillis()));
    }

    public static Deque<String> getHistory(Player player) {
        Deque<TimedString> tsHistory = signEditHistory.getOrDefault(player, new ArrayDeque<>());
        return tsHistory.stream()
                .filter(timedString -> (System.currentTimeMillis() - timedString.getTime()) / 1000 < settingsManager.getProperty(PluginSettings.SIGN_CONTEXT_TIME_LIMIT))
                .map(TimedString::getMessage)
                .collect(ArrayDeque::new, ArrayDeque::add, ArrayDeque::addAll);
    }

    public static void clearPlayerContext(Player player) {
        if (signEditHistory.get(player) == null) return;
        signEditHistory.remove(player);
    }

    public static void pollPlayerContext(Player player) {
        Deque<TimedString> history = signEditHistory.get(player);
        if (history != null) history.pollLast();
    }
    public static void forceClearContext() {
        signEditHistory.clear();
    }

    private SignContext() {}
}
