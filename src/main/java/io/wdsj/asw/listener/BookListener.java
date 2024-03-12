package io.wdsj.asw.listener;

import io.wdsj.asw.AdvancedSensitiveWords;
import io.wdsj.asw.event.ASWFilterEvent;
import io.wdsj.asw.event.EventType;
import io.wdsj.asw.impl.list.AdvancedList;
import io.wdsj.asw.manage.notice.Notifier;
import io.wdsj.asw.manage.punish.Punishment;
import io.wdsj.asw.setting.PluginMessages;
import io.wdsj.asw.setting.PluginSettings;
import io.wdsj.asw.util.Utils;
import io.wdsj.asw.util.cache.BookCache;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

import static io.wdsj.asw.AdvancedSensitiveWords.*;
import static io.wdsj.asw.util.TimingUtils.addProcessStatistic;
import static io.wdsj.asw.util.Utils.messagesFilteredNum;

public class BookListener implements Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onBook(PlayerEditBookEvent event) {
        if (!isInitialized) return;
        if (!settingsManager.getProperty(PluginSettings.ENABLE_BOOK_EDIT_CHECK)) return;
        Player player = event.getPlayer();
        if (player.hasPermission("advancedsensitivewords.bypass")) return;
        boolean isCacheEnabled = settingsManager.getProperty(PluginSettings.BOOK_CACHE);
        boolean skipReturnLine = settingsManager.getProperty(PluginSettings.BOOK_IGNORE_NEWLINE);
        String outMessage = "";
        String processedOutMessage = "";
        List<String> outList = new AdvancedList<>();
        List<String> originalPages = event.getNewBookMeta().getPages();
        boolean shouldSendMessage = false;
        BookMeta bookMeta = event.getNewBookMeta();
        int pageIndex = 1;
        long startTime = System.currentTimeMillis();
        if (bookMeta.hasPages()) {
            for (String originalPage : originalPages) {
                if (skipReturnLine) originalPage = originalPage.replace("\n", "").replace("§0", "");
                if (settingsManager.getProperty(PluginSettings.IGNORE_FORMAT_CODE)) originalPage = originalPage.replaceAll(Utils.getIgnoreFormatCodeRegex(), "");
                boolean isBookCached = BookCache.isBookCached(originalPage);
                List<String> censoredWordList = isBookCached && isCacheEnabled ? BookCache.getCachedBookSensitiveWordList(originalPage) : AdvancedSensitiveWords.sensitiveWordBs.findAll(originalPage);
                if (!censoredWordList.isEmpty()) {
                    String processedPage = isBookCached && isCacheEnabled ? BookCache.getCachedProcessedBookContent(originalPage) : AdvancedSensitiveWords.sensitiveWordBs.replace(originalPage);
                    if (!isBookCached && isCacheEnabled) BookCache.addToBookCache(originalPage, processedPage, censoredWordList);
                    if (settingsManager.getProperty(PluginSettings.BOOK_METHOD).equalsIgnoreCase("cancel")) {
                        event.setCancelled(true);
                        shouldSendMessage = true;
                        outMessage = originalPage;
                        outList = censoredWordList;
                        processedOutMessage = processedPage;
                        break;
                    }

                    bookMeta.setPage(pageIndex++, processedPage);
                    shouldSendMessage = true;
                    outMessage = originalPage;
                    outList = censoredWordList;
                    processedOutMessage = processedPage;
                }
            }
        }
        String originalAuthor = event.getNewBookMeta().getAuthor();
        if (originalAuthor != null) {
            if (settingsManager.getProperty(PluginSettings.IGNORE_FORMAT_CODE)) originalAuthor = originalAuthor.replaceAll(Utils.getIgnoreFormatCodeRegex(), "");
            List<String> censoredWordListAuthor = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalAuthor);
            if (!censoredWordListAuthor.isEmpty()) {
                String processedAuthor = AdvancedSensitiveWords.sensitiveWordBs.replace(originalAuthor);
                if (settingsManager.getProperty(PluginSettings.BOOK_METHOD).equalsIgnoreCase("cancel")) {
                    event.setCancelled(true);
                } else {
                    bookMeta.setAuthor(processedAuthor);
                }
                shouldSendMessage = true;
                outMessage = originalAuthor;
                outList = censoredWordListAuthor;
                processedOutMessage = processedAuthor;
            }
        }


        String originalTitle = event.getNewBookMeta().getTitle();
        if (originalTitle != null) {
            if (settingsManager.getProperty(PluginSettings.IGNORE_FORMAT_CODE)) originalTitle = originalTitle.replaceAll(Utils.getIgnoreFormatCodeRegex(), "");
            List<String> censoredWordListTitle = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalTitle);
            if (!censoredWordListTitle.isEmpty()) {
                String processedTitle = AdvancedSensitiveWords.sensitiveWordBs.replace(originalTitle);
                if (settingsManager.getProperty(PluginSettings.BOOK_METHOD).equalsIgnoreCase("cancel")) {
                    event.setCancelled(true);
                } else {
                    bookMeta.setTitle(processedTitle);
                }
                shouldSendMessage = true;
                outMessage = originalTitle;
                outList = censoredWordListTitle;
                processedOutMessage = processedTitle;
            }
        }

        if (shouldSendMessage) {
            event.setNewBookMeta(bookMeta);
            messagesFilteredNum.getAndIncrement();
            if (settingsManager.getProperty(PluginSettings.ENABLE_API)) {
                Bukkit.getPluginManager().callEvent(new ASWFilterEvent(player, outMessage, processedOutMessage, outList, EventType.BOOK, false));
            }
            long endTime = System.currentTimeMillis();
            addProcessStatistic(endTime, startTime);
            if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, EventType.BOOK, outMessage);
            Punishment.punish(player);
        }

        if (settingsManager.getProperty(PluginSettings.BOOK_SEND_MESSAGE) && shouldSendMessage) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_BOOK)));
        }

        if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION) && shouldSendMessage) {
            Utils.logViolation(player.getName() + "(IP: " + Utils.getPlayerIp(player) + ")(Book)", outMessage + outList);
        }
    }
}