package io.wdsj.asw.bukkit.util;

import com.github.houbb.heaven.util.io.FileUtil;
import com.github.houbb.heaven.util.lang.StringUtil;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class Utils {
    public static AtomicLong messagesFilteredNum = new AtomicLong(0);

    public static String getPlayerIp(Player player) {
        InetSocketAddress address = player.getAddress();
        if (address != null && address.getAddress() != null) return address.getAddress().getHostAddress();
        throw new IllegalStateException("Player address is null");
    }

    public static boolean isClassLoaded(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
    public static boolean isAnyClassLoaded(String... classNames) {
        for (String className : classNames) {
            if (isClassLoaded(className)) return true;
        }
        return false;
    }
    public static boolean canUsePE() {
        Plugin protocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib");
        Plugin packetevents = Bukkit.getPluginManager().getPlugin("packetevents");
        if (packetevents == null) return false;
        if (protocolLib != null && protocolLib.isEnabled()) {
            // ProtocolLib is loaded
            try {
                return StringUtil.toInt(String.valueOf(protocolLib.getDescription().getVersion().charAt(0))) >= 5;
            } catch (Exception e) {
                return true;
            }
        }
        return true;
    }

    public static void purgeLog() {
        File logFile = new File(AdvancedSensitiveWords.getInstance().getDataFolder(), "violations.log");
        if (!logFile.exists()) return;
        FileUtil.deleteFile(logFile);
        LOGGER.info("Successfully purged violations");
    }
    public static boolean isCommand(String command) {
        return command.startsWith("/");
    }
    public static String getSplitCommandArgs(String command) {
        String[] splitCommand = command.split(" ");
        if (splitCommand.length <= 1) return "";
        return String.join(" ", Arrays.copyOfRange(splitCommand, 1, splitCommand.length));
    }

    public static String getSplitCommandHeaders(String command) {
        String[] splitCommand = command.split(" ");
        if (splitCommand.length < 1) return "";
        return splitCommand[0];
    }
    public static String getPreProcessRegex() {
        return settingsManager.getProperty(PluginSettings.PRE_PROCESS_REGEX);
    }


    public static boolean isCommandAndWhiteListed(String command) {
        if (!command.startsWith("/")) return false;
        List<String> whitelist = settingsManager.getProperty(PluginSettings.CHAT_COMMAND_WHITE_LIST);
        String[] splitCommand = command.split(" ");
        for (String s : whitelist) {
            if (splitCommand[0].equalsIgnoreCase(s)) {
                return !settingsManager.getProperty(PluginSettings.CHAT_INVERT_WHITELIST);
            }
        }
        return settingsManager.getProperty(PluginSettings.CHAT_INVERT_WHITELIST);
    }

    public static String getMinecraftVersion() {
        return Bukkit.getBukkitVersion().split("-")[0];
    }

    public static boolean isNotCommand(String command) {
        return !command.startsWith("/");
    }

    /**
     * Checks if the given object is null, and returns the fallback value if it is.
     * @param obj The object to check
     * @param fallback The fallback value to return if the object is null
     */
    public static <T> T checkNotNullWithFallback(T obj, T fallback) {
        if (obj == null) {
            return fallback;
        }
        return obj;
    }

    private Utils() {
    }
}
