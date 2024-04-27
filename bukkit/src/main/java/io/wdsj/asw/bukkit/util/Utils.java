package io.wdsj.asw.bukkit.util;

import com.github.houbb.heaven.util.io.FileUtil;
import com.github.houbb.heaven.util.lang.StringUtil;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class Utils {
    public static AtomicLong messagesFilteredNum = new AtomicLong(0);

    public static String getPlayerIp(Player player) {
        InetSocketAddress address = player.getAddress();
        if (address != null && address.getAddress() != null) return address.getAddress().getHostAddress();
        return "null";
    }

    public static boolean isClassLoaded(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }


    public static void logViolation(String playerName, String violationReason) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
        String formattedDate = dateFormat.format(new Date());
        String logMessage = "[" + formattedDate + "] " + playerName + " " + violationReason;
        File logFile = new File(AdvancedSensitiveWords.getInstance().getDataFolder(), "violations.log");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        try {
            Writer writer = new OutputStreamWriter(new FileOutputStream(logFile, true), StandardCharsets.UTF_8);

            try {
                writer.write(logMessage + System.lineSeparator());
            } catch (Throwable th) {
                try {
                    writer.close();
                } catch (Throwable t) {
                    th.addSuppressed(t);
                }
                throw th;
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static boolean checkProtocolLib() {
        Plugin protocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib");
        if (protocolLib != null) {
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

    private Utils() {
    }
}
