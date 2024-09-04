package io.wdsj.asw.bukkit.util;

import io.wdsj.asw.bukkit.AdvancedSensitiveWords;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;

public class LoggingUtils {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
    public static void logViolation(String playerName, String violationReason) {
        String formattedDate = dateFormat.format(new Date());
        String logMessage = "[" + formattedDate + "] " + playerName + " " + violationReason;
        File logFile = new File(AdvancedSensitiveWords.getInstance().getDataFolder(), "violations.log");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                LOGGER.severe("Failed to create violations.log file: " + e.getMessage());
                return;
            }
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(logFile, true), StandardCharsets.UTF_8)) {
            writer.write(logMessage + System.lineSeparator());
        } catch (IOException e) {
            LOGGER.severe("Failed to write to violations.log file: " + e.getMessage());
        }
    }
}
