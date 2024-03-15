package io.wdsj.asw.listener;

import io.wdsj.asw.AdvancedSensitiveWords;
import io.wdsj.asw.event.ASWFilterEvent;
import io.wdsj.asw.event.EventType;
import io.wdsj.asw.manage.notice.Notifier;
import io.wdsj.asw.manage.punish.Punishment;
import io.wdsj.asw.setting.PluginMessages;
import io.wdsj.asw.setting.PluginSettings;
import io.wdsj.asw.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.List;

import static io.wdsj.asw.AdvancedSensitiveWords.*;
import static io.wdsj.asw.util.TimingUtils.addProcessStatistic;
import static io.wdsj.asw.util.Utils.isClassLoaded;
import static io.wdsj.asw.util.Utils.messagesFilteredNum;

public class BlockSignListener implements Listener {
    private static final boolean isModernVersion;
    private static String outMessage;
    private static String outProcessedMessage;
    private static List<String> outList;
    static {
        isModernVersion = isClassLoaded("org.bukkit.block.sign.SignSide");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlace(BlockPlaceEvent event) {
        if (!isInitialized || !settingsManager.getProperty(PluginSettings.SIGN_ENHANCED_CHECK)) return;
        Player player = event.getPlayer();
        if (player.hasPermission("advancedsensitivewords.bypass")) return;
        boolean shouldSendMessage = false;
        BlockState block = event.getBlock().getState();
        if (!(block instanceof Sign)) return;
        Sign sign = (Sign) block;
        long startTime = System.currentTimeMillis();
        if (isModernVersion) {
            org.bukkit.block.sign.SignSide front = sign.getSide(org.bukkit.block.sign.Side.FRONT);
            for (int line = 0; line < 4; line++) {
                String originalFrontLine = front.getLine(line);
                if (settingsManager.getProperty(PluginSettings.IGNORE_FORMAT_CODE)) originalFrontLine = originalFrontLine.replaceAll(Utils.getIgnoreFormatCodeRegex(), "");
                List<String> censoredWordList = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalFrontLine);
                if (!censoredWordList.isEmpty()) {
                    String processedMessage = AdvancedSensitiveWords.sensitiveWordBs.replace(originalFrontLine);
                    outMessage = originalFrontLine;
                    outProcessedMessage = processedMessage;
                    outList = censoredWordList;
                    if (settingsManager.getProperty(PluginSettings.SIGN_METHOD).equalsIgnoreCase("cancel")) {
                        shouldSendMessage = true;
                        event.setCancelled(true);
                        break;
                    }
                    front.setLine(line, processedMessage);
                    shouldSendMessage = true;
                }
            }
            org.bukkit.block.sign.SignSide back = sign.getSide(org.bukkit.block.sign.Side.BACK);
            for (int line = 0; line < 4; line++) {
                String originalBackLine = back.getLine(line);
                if (settingsManager.getProperty(PluginSettings.IGNORE_FORMAT_CODE)) originalBackLine = originalBackLine.replaceAll(Utils.getIgnoreFormatCodeRegex(), "");
                List<String> censoredWordList = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalBackLine);
                if (!censoredWordList.isEmpty()) {
                    String processedMessage = AdvancedSensitiveWords.sensitiveWordBs.replace(originalBackLine);
                    outMessage = originalBackLine;
                    outProcessedMessage = processedMessage;
                    outList = censoredWordList;
                    if (settingsManager.getProperty(PluginSettings.SIGN_METHOD).equalsIgnoreCase("cancel")) {
                        shouldSendMessage = true;
                        event.setCancelled(true);
                        break;
                    }
                    back.setLine(line, processedMessage);
                    shouldSendMessage = true;
                }
            }
        } else {
            for (int line = 0; line < 4; line++) {
                String originalLine = sign.getLine(line);
                if (settingsManager.getProperty(PluginSettings.IGNORE_FORMAT_CODE)) originalLine = originalLine.replaceAll(Utils.getIgnoreFormatCodeRegex(), "");
                List<String> censoredWordList = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalLine);
                if (!censoredWordList.isEmpty()) {
                    String processedMessage = AdvancedSensitiveWords.sensitiveWordBs.replace(originalLine);
                    outMessage = originalLine;
                    outProcessedMessage = processedMessage;
                    outList = censoredWordList;
                    if (settingsManager.getProperty(PluginSettings.SIGN_METHOD).equalsIgnoreCase("cancel")) {
                        shouldSendMessage = true;
                        event.setCancelled(true);
                        break;
                    }
                    sign.setLine(line, processedMessage);
                    shouldSendMessage = true;
                }
            }
        }
        if (shouldSendMessage && settingsManager.getProperty(PluginSettings.ENABLE_API)) {
            Bukkit.getPluginManager().callEvent(new ASWFilterEvent(player, outMessage, outProcessedMessage, outList, EventType.SIGN, false));
        }
        if (settingsManager.getProperty(PluginSettings.SIGN_SEND_MESSAGE) && shouldSendMessage) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_SIGN)));
        }
        if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION) && shouldSendMessage) {
            Utils.logViolation(player.getName() + "(IP: " + Utils.getPlayerIp(player) + ")(Sign)", outMessage + outList);
        }
        if (shouldSendMessage) {
            messagesFilteredNum.getAndIncrement();
            long endTime = System.currentTimeMillis();
            addProcessStatistic(endTime, startTime);
            if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, EventType.SIGN, outMessage);
            if (settingsManager.getProperty(PluginSettings.SIGN_PUNISH)) Punishment.punish(player);
        }

    }
}
