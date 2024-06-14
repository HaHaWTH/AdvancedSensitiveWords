package io.wdsj.asw.bukkit.listener.packet;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEditBook;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.event.ASWFilterEvent;
import io.wdsj.asw.bukkit.event.EventType;
import io.wdsj.asw.bukkit.manage.notice.Notifier;
import io.wdsj.asw.bukkit.manage.permission.Permissions;
import io.wdsj.asw.bukkit.manage.punish.Punishment;
import io.wdsj.asw.bukkit.proxy.bungee.BungeeSender;
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.util.Utils;
import io.wdsj.asw.bukkit.util.cache.BookCache;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.*;
import static io.wdsj.asw.bukkit.util.TimingUtils.addProcessStatistic;
import static io.wdsj.asw.bukkit.util.Utils.messagesFilteredNum;

public class ASWBookPacketListener extends PacketListenerAbstract {
    public ASWBookPacketListener() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isInitialized) return;
        if (!settingsManager.getProperty(PluginSettings.ENABLE_BOOK_EDIT_CHECK)) return;
        PacketTypeCommon packetType = event.getPacketType();
        User user = event.getUser();
        Player player = (Player) event.getPlayer();
        if (player.hasPermission(Permissions.BYPASS)) return;
        String userName = user.getName();
        if (packetType == PacketType.Play.Client.EDIT_BOOK) {
            String processedOutMessage = "";
            String outMessage = "";
            List<String> outList = new ArrayList<>();
            boolean skipReturnLine = settingsManager.getProperty(PluginSettings.BOOK_IGNORE_NEWLINE);
            boolean isCacheEnabled = settingsManager.getProperty(PluginSettings.BOOK_CACHE);
            boolean shouldSendMessage = false;
            boolean isCancelMode = settingsManager.getProperty(PluginSettings.BOOK_METHOD).equalsIgnoreCase("cancel");
            WrapperPlayClientEditBook wrapper = new WrapperPlayClientEditBook(event);

            // Book content check
            List<String> originalPages = wrapper.getPages();
            List<String> processedPages = new ArrayList<>(originalPages.size());
            long startTime = System.currentTimeMillis();
            for (String originalPage : originalPages) {
                if (skipReturnLine) originalPage = originalPage.replace("\n", "").replace("§0", "");
                if (settingsManager.getProperty(PluginSettings.PRE_PROCESS)) originalPage = originalPage.replaceAll(Utils.getPreProcessRegex(), "");
                boolean isBookCached = BookCache.isBookCached(originalPage);
                List<String> censoredWordList = isBookCached && isCacheEnabled ? BookCache.getCachedBookSensitiveWordList(originalPage) : AdvancedSensitiveWords.sensitiveWordBs.findAll(originalPage);
                String processedPage = isBookCached && isCacheEnabled ? BookCache.getCachedProcessedBookContent(originalPage) : AdvancedSensitiveWords.sensitiveWordBs.replace(originalPage);
                if (!censoredWordList.isEmpty()) {
                    if (!isBookCached && isCacheEnabled) BookCache.addToBookCache(originalPage, processedPage, censoredWordList);
                    if (isCancelMode) {
                        event.setCancelled(true);
                        shouldSendMessage = true;
                        outMessage = originalPage;
                        outList = censoredWordList;
                        processedOutMessage = processedPage;
                        break;
                    }

                    shouldSendMessage = true;
                    outMessage = originalPage;
                    outList = censoredWordList;
                    processedOutMessage = processedPage;
                }
                processedPages.add(processedPage);
            }
            if (!isCancelMode && shouldSendMessage) {
                wrapper.setPages(processedPages);
            }

            // Book title check
            String originalTitle = wrapper.getTitle();
            if (originalTitle != null) {
                if (settingsManager.getProperty(PluginSettings.PRE_PROCESS)) originalTitle = originalTitle.replaceAll(Utils.getPreProcessRegex(), "");
                List<String> censoredWordListTitle = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalTitle);
                if (!censoredWordListTitle.isEmpty()) {
                    String processedTitle = AdvancedSensitiveWords.sensitiveWordBs.replace(originalTitle);
                    if (isCancelMode) {
                        event.setCancelled(true);
                    } else {
                        wrapper.setTitle(processedTitle);
                    }
                    shouldSendMessage = true;
                    outMessage = originalTitle;
                    outList = censoredWordListTitle;
                    processedOutMessage = processedTitle;
                }
            }

            if (shouldSendMessage) {
                messagesFilteredNum.getAndIncrement();
                if (settingsManager.getProperty(PluginSettings.ENABLE_API)) {
                    Bukkit.getPluginManager().callEvent(new ASWFilterEvent(player, outMessage, processedOutMessage, outList, EventType.BOOK, true));
                }
                if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                    VelocitySender.send(player, EventType.BOOK, outMessage, outList);
                }
                if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                    BungeeSender.send(player, EventType.BOOK, outMessage, outList);
                }
                if (settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                    databaseManager.checkAndUpdatePlayer(player.getName());
                }
                long endTime = System.currentTimeMillis();
                addProcessStatistic(endTime, startTime);
                if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) {
                    Notifier.notice(player, EventType.BOOK, outMessage, outList);
                }
                if (settingsManager.getProperty(PluginSettings.BOOK_PUNISH)) Punishment.punish(player);
                if (settingsManager.getProperty(PluginSettings.BOOK_SEND_MESSAGE)) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_BOOK)));
                }
                if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                    Utils.logViolation(userName + "(IP: " + user.getAddress().getAddress().getHostAddress() + ")(Book)",outMessage + outList);
                }
            }
        }
    }
}
