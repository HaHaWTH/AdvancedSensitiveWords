package io.wdsj.asw.bukkit.integration.packetevents.sign;

import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;

import java.util.List;

public final class SignFakeViewCompat {
    private static final String PACKET_EVENTS_PLUGIN_NAME = "packetevents";
    private static boolean unavailableLogged;
    private static boolean registered;

    private SignFakeViewCompat() {
    }

    public static void tryRegister(AdvancedSensitiveWords plugin) {
        if (!AdvancedSensitiveWords.setting(PluginSettings.ENABLE_SIGN_EDIT_CHECK) || !AdvancedSensitiveWords.setting(PluginSettings.SIGN_FAKE_ON_CANCEL)) {
            return;
        }

        if (!isPacketEventsAvailable()) {
            logPacketEventsUnavailable();
            return;
        }
        SignFakeViewService.setOperational(false);
        SignFakeViewPacketListener.register();
        SignFakeViewService.setOperational(true);
        registered = true;
        AdvancedSensitiveWords.LOGGER.info("Sign fake view support is available through PacketEvents.");
    }

    public static void unregister() {
        if (registered && isPacketEventsAvailable()) {
            SignFakeViewPacketListener.unregister();
            SignFakeViewService.setOperational(false);
        }
        registered = false;
    }

    public static void recordCancelledEdit(
            SignChangeEvent event,
            Player player,
            List<Component> attemptedLines,
            String violationContent,
            List<String> censoredWords
    ) {
        if (!registered) {
            return;
        }
        SignFakeViewService.recordCancelledEdit(event, player, attemptedLines, violationContent, censoredWords);
    }

    public static boolean recordShadowEdit(
            SignChangeEvent event,
            Player player,
            List<Component> attemptedLines
    ) {
        if (!registered) {
            return false;
        }
        return SignFakeViewService.recordShadowEdit(event, player, attemptedLines);
    }

    private static boolean isPacketEventsAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled(PACKET_EVENTS_PLUGIN_NAME);
    }

    private static void logPacketEventsUnavailable() {
        if (unavailableLogged) {
            return;
        }
        unavailableLogged = true;
        AdvancedSensitiveWords.LOGGER.warn("Sign fake view requires PacketEvents. Falling back to normal sign cancel behavior.");
    }
}
