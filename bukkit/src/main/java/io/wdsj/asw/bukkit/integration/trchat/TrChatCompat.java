package io.wdsj.asw.bukkit.integration.trchat;

import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.manage.punish.PlayerAltController;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TrChatCompat {
    private static final Listener SEND_EVENT_LISTENER = new Listener() {
    };
    private static final EventExecutor SEND_EVENT_EXECUTOR = TrChatCompat::handleSendEvent;
    private static boolean REGISTERED;
    private static boolean warned;

    private TrChatCompat() {
    }

    public static void tryRegister(Plugin plugin) {
        if (!TrChatReflection.isAvailable()) {
            return;
        }
        if (REGISTERED) {
            return;
        }

        Bukkit.getPluginManager().registerEvent(
                TrChatReflection.SEND_EVENT_CLASS,
                SEND_EVENT_LISTENER,
                EventPriority.HIGHEST,
                SEND_EVENT_EXECUTOR,
                plugin,
                false
        );
        REGISTERED = true;
        AdvancedSensitiveWords.LOGGER.info("TrChat compatibility for fake chat messages is enabled.");
    }

    public static boolean tryMarkFakeMessage(Player player) {
        if (!REGISTERED) {
            return false;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled(TrChatReflection.PLUGIN_NAME)) {
            return false;
        }

        TrChatPendingFakeMessages.increment(player);
        return true;
    }

    private static void handleSendEvent(Listener listener, Event event) {
        if (!TrChatReflection.isSendEvent(event)) return;
        if (!(event instanceof Cancellable cancellable)) {
            return;
        }

        Player player = TrChatReflection.getPlayer(event);
        if (player == null) {
            return;
        }

        if (!TrChatPendingFakeMessages.hasPending(player)) {
            return;
        }

        if (cancellable.isCancelled()) {
            TrChatPendingFakeMessages.consume(player);
            return;
        }

        String type = TrChatReflection.getTypeName(event).toUpperCase(Locale.ROOT);
        switch (type) {
            case "SENDER":
                cancellable.setCancelled(true);
                sendFormattedMessage(event, player);
                break;
            case "RECEIVER":
                cancellable.setCancelled(true);
                TrChatPendingFakeMessages.consume(player);
                break;
            case "COMMON":
            default:
                if (!TrChatPendingFakeMessages.consume(player)) {
                    return;
                }
                cancellable.setCancelled(true);
                sendFormattedMessage(event, player);
                break;
        }
    }

    private static void sendFormattedMessage(Event event, Player player) {
        Object component = TrChatReflection.getComponent(event);
        if (component == null) {
            sendLegacyFallback(event, player);
            return;
        }

        if (!sendComponent(player, component, player)) {
            sendLegacyFallback(event, player);
        }
        if (!AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_ALTS_CHECK) || !PlayerAltController.hasAlt(player)) {
            return;
        }

        for (UUID alt : PlayerAltController.getAlts(player)) {
            Player altPlayer = Bukkit.getPlayer(alt);
            if (altPlayer != null) {
                if (!sendComponent(altPlayer, component, player)) {
                    sendLegacyFallback(event, altPlayer);
                }
            }
        }
    }

    private static boolean sendComponent(Player receiver, Object component, Player sender) {
        if (TrChatReflection.sendComponent(receiver, component, sender)) {
            return true;
        }
        if (!warned) {
            AdvancedSensitiveWords.LOGGER.warning("Failed to send TrChat fake message component. Falling back to legacy text.");
            warned = true;
        }
        return false;
    }

    private static void sendLegacyFallback(Event event, Player player) {
        String legacyMessage = TrChatReflection.getLegacyMessage(event);
        if (!legacyMessage.isEmpty()) {
            player.sendMessage(legacyMessage);
        }
    }
}
