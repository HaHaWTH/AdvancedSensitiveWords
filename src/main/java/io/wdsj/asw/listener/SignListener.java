package io.wdsj.asw.listener;

import com.github.houbb.heaven.util.lang.StringUtil;
import io.wdsj.asw.AdvancedSensitiveWords;
import io.wdsj.asw.event.ASWFilterEvent;
import io.wdsj.asw.event.EventType;
import io.wdsj.asw.impl.list.AdvancedList;
import io.wdsj.asw.manage.notice.Notifier;
import io.wdsj.asw.manage.punish.Punishment;
import io.wdsj.asw.setting.PluginMessages;
import io.wdsj.asw.setting.PluginSettings;
import io.wdsj.asw.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.util.List;

import static io.wdsj.asw.AdvancedSensitiveWords.*;
import static io.wdsj.asw.util.TimingUtils.addProcessStatistic;
import static io.wdsj.asw.util.Utils.messagesFilteredNum;

public class SignListener implements Listener {
    private String outMessage = "";
    private List<String> outList = new AdvancedList<>();
    private String outProcessedMessage = "";


    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onSign(SignChangeEvent event) {
        if (!isInitialized) return;
        if (!settingsManager.getProperty(PluginSettings.ENABLE_SIGN_EDIT_CHECK)) return;
        Player player = event.getPlayer();
        if (player.hasPermission("advancedsensitivewords.bypass")) return;
        boolean shouldSendMessage = false;
        long startTime = System.currentTimeMillis();
        List<Integer> indexList = new AdvancedList<>();
        StringBuilder originalMultiMessages = new StringBuilder();
        for (int line = 0; line < 4; ++line) {
            String originalMessage = event.getLine(line);
            if (settingsManager.getProperty(PluginSettings.IGNORE_FORMAT_CODE) && originalMessage != null) originalMessage = originalMessage.replaceAll(Utils.getIgnoreFormatCodeRegex(), "");
            assert originalMessage != null;
            List<String> censoredWordList = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalMessage);
            if (!censoredWordList.isEmpty()) {
                String processedMessage = AdvancedSensitiveWords.sensitiveWordBs.replace(originalMessage);
                outMessage = originalMessage;
                outProcessedMessage = processedMessage;
                outList = censoredWordList;
                if (settingsManager.getProperty(PluginSettings.SIGN_METHOD).equalsIgnoreCase("cancel")) {
                    shouldSendMessage = true;
                    event.setCancelled(true);
                    break;
                }
                event.setLine(line, processedMessage);
                shouldSendMessage = true;
            } else if (StringUtil.isNotEmptyTrim(originalMessage)) {
                indexList.add(line);
                originalMultiMessages.append(originalMessage);
            }
        }
        if (settingsManager.getProperty(PluginSettings.SIGN_MULTI_LINE_CHECK) && !indexList.isEmpty()) {
            String originalMessagesString = originalMultiMessages.toString();
            List<String> censoredWordList = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalMessagesString);
            if (!censoredWordList.isEmpty()) {
                String processedMessagesString = AdvancedSensitiveWords.sensitiveWordBs.replace(originalMessagesString);
                outMessage = originalMessagesString;
                outProcessedMessage = processedMessagesString;
                outList = censoredWordList;
                if (settingsManager.getProperty(PluginSettings.SIGN_METHOD).equalsIgnoreCase("cancel")) {
                    shouldSendMessage = true;
                    event.setCancelled(true);
                } else {
                    shouldSendMessage = true;
                    for (int i : indexList) {
                        event.setLine(i, processedMessagesString);
                    }
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
