package io.wdsj.asw.bukkit.integration.trchat;

import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.manage.punish.PlayerAltController;
import io.wdsj.asw.bukkit.manage.punish.PlayerShadowController;
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

public final class TrChatCompat {
    private static final Listener SEND_EVENT_LISTENER = new Listener() {
    };
    private static final EventExecutor SEND_EVENT_EXECUTOR = TrChatCompat::handleSendEvent;
    private static boolean REGISTERED;
    private static boolean warned;

    private TrChatCompat() {
    }

    private static boolean trchatInstalled;
    public static void tryRegister(Plugin plugin) {
        if (!TrChatReflection.isAvailable()) {
            return;
        }
        if (REGISTERED) {
            return;
        }
        trchatInstalled = Bukkit.getPluginManager().getPlugin(TrChatReflection.PLUGIN_NAME) != null;

        assert TrChatReflection.SEND_EVENT_CLASS != null;
        Bukkit.getPluginManager().registerEvent(
                TrChatReflection.SEND_EVENT_CLASS,
                SEND_EVENT_LISTENER,
                EventPriority.HIGHEST,
                SEND_EVENT_EXECUTOR,
                plugin,
                false
        );
        REGISTERED = true;
        AdvancedSensitiveWords.LOGGER.info("TrChat compatibility for fake chat messages and shadowban is enabled.");
    }

    public static boolean tryMarkFakeMessage(Player player) {
        if (!isEnabled()) {
            return false;
        }

        TrChatPendingFakeMessages.increment(player);
        return true;
    }

    public static boolean isEnabled() {
        return REGISTERED && trchatInstalled;
    }

    private static void handleSendEvent(Listener listener, Event event) {
        if (!TrChatReflection.isSendEvent(event)) {
            return;
        }
        if (!(event instanceof Cancellable cancellable)) {
            return;
        }

        Player player = TrChatReflection.getPlayer(event);
        if (player == null) {
            return;
        }

        if (!TrChatPendingFakeMessages.hasPending(player)) {
            handleShadowSendEvent(cancellable, event, player);
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

    private static void handleShadowSendEvent(Cancellable cancellable, Event event, Player player) {
        if (!PlayerShadowController.isShadowed(player) || cancellable.isCancelled()) {
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
                break;
            case "COMMON":
            default:
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
        if (!AdvancedSensitiveWords.setting(PluginSettings.ENABLE_ALTS_CHECK) || !PlayerAltController.hasAlt(player)) {
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
            AdvancedSensitiveWords.LOGGER.warn("Failed to send TrChat component. Falling back to legacy text.");
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
