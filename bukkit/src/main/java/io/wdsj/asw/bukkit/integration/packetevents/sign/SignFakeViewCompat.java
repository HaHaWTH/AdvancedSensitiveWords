package io.wdsj.asw.bukkit.integration.packetevents.sign;

import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import org.bukkit.Bukkit;

public final class SignFakeViewCompat {
    private static final String PACKET_EVENTS_PLUGIN_NAME = "packetevents";
    private static boolean unavailableLogged;
    private static boolean registered;

    private SignFakeViewCompat() {
    }

    public static void tryRegister(AdvancedSensitiveWords plugin) {
        SignFakeViewService.setOperational(false);
        if (!AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_SIGN_EDIT_CHECK)
                || !AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.SIGN_FAKE_ON_CANCEL)) {
            return;
        }

        if (!AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.SIGN_METHOD).equalsIgnoreCase("cancel")) {
            AdvancedSensitiveWords.LOGGER.info("Sign.fakeOnCancel is enabled but Sign.method is not cancel; sign fake view is disabled.");
            return;
        }

        if (!isPacketEventsAvailable()) {
            logPacketEventsUnavailable();
            return;
        }

        SignFakeViewPacketListener.register();
        SignFakeViewService.setOperational(true);
        registered = true;
        AdvancedSensitiveWords.LOGGER.info("Sign fake view support is enabled through PacketEvents.");
    }

    public static void unregister() {
        if (registered && isPacketEventsAvailable()) {
            SignFakeViewPacketListener.unregister();
        }
        registered = false;
        SignFakeViewService.setOperational(false);
    }

    private static boolean isPacketEventsAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled(PACKET_EVENTS_PLUGIN_NAME);
    }

    private static void logPacketEventsUnavailable() {
        if (unavailableLogged) {
            return;
        }
        unavailableLogged = true;
        AdvancedSensitiveWords.LOGGER.warn("Sign.fakeOnCancel requires PacketEvents. Falling back to normal sign cancel behavior.");
    }
}
