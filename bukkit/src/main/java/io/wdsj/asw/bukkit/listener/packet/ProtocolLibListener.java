package io.wdsj.asw.bukkit.listener.packet;

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
import io.wdsj.asw.bukkit.util.context.ChatContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.List;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.*;
import static io.wdsj.asw.bukkit.util.TimingUtils.addProcessStatistic;
import static io.wdsj.asw.bukkit.util.Utils.*;

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
                    String message = settingsManager.getProperty(PluginSettings.PRE_PROCESS) ? event.getPacket().getStrings().read(0).replaceAll(getPreProcessRegex(), "") : event.getPacket().getStrings().read(0);
                    if (isCommandAndWhiteListed(message) || player.hasPermission(Permissions.BYPASS))
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
                        if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                            VelocitySender.send(player, EventType.CHAT, message);
                        }
                        if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                            BungeeSender.send(player, EventType.CHAT, message);
                        }
                        if (settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                            databaseManager.checkAndUpdatePlayer(player.getName());
                        }
                        long endTime = System.currentTimeMillis();
                        addProcessStatistic(endTime, startTime);
                        getScheduler().runTask(()-> {
                            if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, EventType.CHAT, message);
                            if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) Punishment.punish(player);
                        });
                        return;
                    }

                    // Context check
                    if (settingsManager.getProperty(PluginSettings.CHAT_CONTEXT_CHECK) && isNotCommand(message)) {
                        ChatContext.addMessage(player, message);
                        Deque<String> history = ChatContext.getHistory(player);
                        String originalContext = String.join("", history);
                        List<String> censoredContextList = sensitiveWordBs.findAll(originalContext);
                        if (!censoredContextList.isEmpty()) {
                            ChatContext.removePlayerContext(player);
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
                            if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                                VelocitySender.send(player, EventType.CHAT, originalContext);
                            }
                            if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                                BungeeSender.send(player, EventType.CHAT, originalContext);
                            }
                            if (settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                                databaseManager.checkAndUpdatePlayer(player.getName());
                            }
                            long endTime = System.currentTimeMillis();
                            addProcessStatistic(endTime, startTime);
                            getScheduler().runTask(()-> {
                                if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, EventType.CHAT, originalContext);
                                if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) Punishment.punish(player);
                            });
                        }
                    }
                }
            }
        };
        com.comphenix.protocol.ProtocolLibrary.getProtocolManager().addPacketListener(protocolChat);
    }
}
