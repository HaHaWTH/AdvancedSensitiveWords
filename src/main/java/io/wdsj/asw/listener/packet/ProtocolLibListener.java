package io.wdsj.asw.listener.packet;

import io.wdsj.asw.AdvancedSensitiveWords;
import io.wdsj.asw.event.ASWFilterEvent;
import io.wdsj.asw.event.EventType;
import io.wdsj.asw.setting.PluginMessages;
import io.wdsj.asw.setting.PluginSettings;
import io.wdsj.asw.util.ContextUtils;
import io.wdsj.asw.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Queue;

import static io.wdsj.asw.AdvancedSensitiveWords.*;
import static io.wdsj.asw.util.TimingUtils.addProcessStatistic;
import static io.wdsj.asw.util.Utils.*;

/**
 * This class keeps compatibility with ProtocolLib 4.x and lower.
 * Alternative choice if PacketEvents api is not available.
 */
public class ProtocolLibListener {
    public static void addAlternateListener() {
        com.comphenix.protocol.events.PacketAdapter protocolChat = new com.comphenix.protocol.events.PacketAdapter(AdvancedSensitiveWords.getInstance(), com.comphenix.protocol.PacketType.Play.Client.CHAT) {
            @Override
            public void onPacketReceiving(@NotNull com.comphenix.protocol.events.PacketEvent event) {
                if (event.getPacketType() == com.comphenix.protocol.PacketType.Play.Client.CHAT && isInitialized) {
                    Player player = event.getPlayer();
                    assert player != null; // In some cases, player maybe null
                    String message = event.getPacket().getStrings().read(0);
                    if (isCommandAndWhiteListed(message) || player.hasPermission("advancedsensitivewords.bypass"))
                        return;
                    if (isAuthMeAvailable && settingsManager.getProperty(PluginSettings.ENABLE_AUTHME_COMPATIBILITY)) {
                        if (!fr.xephi.authme.api.v3.AuthMeApi.getInstance().isAuthenticated(player)) return;
                    }
                    if (isCslAvailable && settingsManager.getProperty(PluginSettings.ENABLE_CSL_COMPATIBILITY)) {
                        if (!cc.baka9.catseedlogin.bukkit.CatSeedLoginAPI.isLogin(player.getName()) || !cc.baka9.catseedlogin.bukkit.CatSeedLoginAPI.isRegister(player.getName())) return;
                    }
                    long startTime = System.currentTimeMillis();
                    // Chat check
                    List<String> censoredWords = AdvancedSensitiveWords.sensitiveWordBs.findAll(message);
                    if (!censoredWords.isEmpty()) {
                        messagesFilteredNum.getAndIncrement();
                        String processedMessage = AdvancedSensitiveWords.sensitiveWordBs.replace(message);
                        if (settingsManager.getProperty(PluginSettings.CHAT_METHOD).equalsIgnoreCase("cancel")) {
                            event.setCancelled(true);
                            if (settingsManager.getProperty(PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL) && isNotCommand(message)) {
                                String fakeMessage = Bukkit.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI") ? me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, messagesManager.getProperty(PluginMessages.CHAT_FAKE_MESSAGE)).replace("%integrated_player%", player.getName()).replace("%integrated_message%", message) : messagesManager.getProperty(PluginMessages.CHAT_FAKE_MESSAGE).replace("%integrated_player%", player.getName()).replace("%integrated_message%", message);
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', fakeMessage));
                            }
                        } else {
                            int maxLength = 256;
                            com.comphenix.protocol.events.PacketContainer packetContainer = event.getPacket();
                            if (processedMessage.length() > maxLength) {
                                packetContainer.getStrings().write(0, processedMessage.substring(0, maxLength));
                            } else {
                                packetContainer.getStrings().write(0, processedMessage);
                            }
                            event.setPacket(packetContainer);
                            if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", player.getName()).replace("%integrated_message%", message)));
                            }
                        }
                        if (settingsManager.getProperty(PluginSettings.ENABLE_API)) {
                            getScheduler().runTask(() -> Bukkit.getPluginManager().callEvent(new ASWFilterEvent(player, message, processedMessage, censoredWords, EventType.CHAT, false)));
                        }
                        if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                            Utils.logViolation(player.getName() + "(IP: " + getPlayerIp(player) + ")(Chat)", message + censoredWords);
                        }
                        long endTime = System.currentTimeMillis();
                        addProcessStatistic(endTime, startTime);
                        return;
                    }
                    // Context check
                    if (settingsManager.getProperty(PluginSettings.CHAT_CONTEXT_CHECK) && isNotCommand(message)) {
                        ContextUtils.addMessage(player, message);
                        Queue<String> history = ContextUtils.getHistory(player);
                        String originalContext = String.join("", history);
                        List<String> censoredContextList = sensitiveWordBs.findAll(originalContext);
                        if (!censoredContextList.isEmpty()) {
                            messagesFilteredNum.getAndIncrement();
                            String processedContext = AdvancedSensitiveWords.sensitiveWordBs.replace(String.join("", censoredContextList));
                            event.setCancelled(true);
                            if (settingsManager.getProperty(PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL)) {
                                String fakeMessage = Bukkit.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI") ? me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, messagesManager.getProperty(PluginMessages.CHAT_FAKE_MESSAGE)).replace("%integrated_player%", player.getName()).replace("%integrated_message%", message) : messagesManager.getProperty(PluginMessages.CHAT_FAKE_MESSAGE).replace("%integrated_player%", player.getName()).replace("%integrated_message%", message);
                                player.sendMessage(ChatColor.translateAlternateColorCodes('&', fakeMessage));
                            }
                            if (settingsManager.getProperty(PluginSettings.ENABLE_API)) {
                                getScheduler().runTask(() -> Bukkit.getPluginManager().callEvent(new ASWFilterEvent(player, originalContext, processedContext, censoredContextList, EventType.CHAT, false)));
                            }
                            if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                                Utils.logViolation(player.getName() + "(IP: " + getPlayerIp(player) + ")(Chat)(Context)", originalContext + censoredContextList);
                            }
                            long endTime = System.currentTimeMillis();
                            addProcessStatistic(endTime, startTime);
                        }
                    }
                }
            }
        };
        com.comphenix.protocol.ProtocolLibrary.getProtocolManager().addPacketListener(protocolChat);
    }

}
