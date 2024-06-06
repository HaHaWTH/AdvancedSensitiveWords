package io.wdsj.asw.bukkit.listener.packet;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;
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

import java.util.Deque;
import java.util.List;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.*;
import static io.wdsj.asw.bukkit.util.TimingUtils.addProcessStatistic;
import static io.wdsj.asw.bukkit.util.Utils.*;

/**
 * @author HaHaWTH & HeyWTF_IS_That and 0D00_0721
 * Made with ❤
 */
public class ASWPacketListener extends PacketListenerAbstract {
    public ASWPacketListener() {
        super(PacketListenerPriority.LOW);
    }

    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon packetType = event.getPacketType();
        User user = event.getUser();
        Player player = (Player) event.getPlayer();
        String userName = user.getName();
        if (packetType == PacketType.Play.Client.CHAT_MESSAGE) {
            WrapperPlayClientChatMessage wrapperPlayClientChatMessage = new WrapperPlayClientChatMessage(event);
            String originalMessage = settingsManager.getProperty(PluginSettings.PRE_PROCESS) ? wrapperPlayClientChatMessage.getMessage().replaceAll(getPreProcessRegex(), "") : wrapperPlayClientChatMessage.getMessage();            if (shouldNotProcess(player, originalMessage)) return;
            long startTime = System.currentTimeMillis();
            // Word check
            List<String> censoredWords = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalMessage);
            if (!censoredWords.isEmpty()) {
                messagesFilteredNum.getAndIncrement();
                String processedMessage = AdvancedSensitiveWords.sensitiveWordBs.replace(originalMessage);
                if (settingsManager.getProperty(PluginSettings.CHAT_METHOD).equalsIgnoreCase("cancel")) {
                    event.setCancelled(true);
                    if (settingsManager.getProperty(PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL) && isNotCommand(originalMessage)) {
                        String fakeMessage = Bukkit.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI") ? me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, messagesManager.getProperty(PluginMessages.CHAT_FAKE_MESSAGE)).replace("%integrated_player%", userName).replace("%integrated_message%", originalMessage) : messagesManager.getProperty(PluginMessages.CHAT_FAKE_MESSAGE).replace("%integrated_player%", userName).replace("%integrated_message%", originalMessage);
                        user.sendMessage(ChatColor.translateAlternateColorCodes('&', fakeMessage));
                    }
                } else {
                    int maxLength = 256;
                    if (processedMessage.length() > maxLength) {
                        wrapperPlayClientChatMessage.setMessage(processedMessage.substring(0, maxLength));
                    } else {
                        wrapperPlayClientChatMessage.setMessage(processedMessage);
                    }
                }
                if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                    user.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", userName).replace("%integrated_message%", originalMessage)));
                }

                if (settingsManager.getProperty(PluginSettings.ENABLE_API)) {
                    getScheduler().runTask(() -> Bukkit.getPluginManager().callEvent(new ASWFilterEvent(player, originalMessage, processedMessage, censoredWords, EventType.CHAT, false)));
                }
                if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                    Utils.logViolation(userName + "(IP: " + user.getAddress().getAddress().getHostAddress() + ")(Chat)", originalMessage + censoredWords);
                }
                if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                    VelocitySender.send(player, EventType.CHAT, originalMessage, censoredWords);
                }
                if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                    BungeeSender.send(player, EventType.CHAT, originalMessage, censoredWords);
                }
                if (settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                    databaseManager.checkAndUpdatePlayer(player.getName());
                }
                long endTime = System.currentTimeMillis();
                addProcessStatistic(endTime, startTime);
                getScheduler().runTask(()-> {
                    if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, EventType.CHAT, originalMessage, censoredWords);
                    if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) Punishment.punish(player);
                });
                return;
            }

            // Context check
            if (settingsManager.getProperty(PluginSettings.CHAT_CONTEXT_CHECK) && isNotCommand(originalMessage)) {
                ChatContext.addMessage(player, originalMessage);
                Deque<String> queue = ChatContext.getHistory(player);
                String originalContext = String.join("", queue);
                List<String> censoredContextList = sensitiveWordBs.findAll(originalContext);
                if (!censoredContextList.isEmpty()) {
                    ChatContext.removePlayerContext(player);
                    messagesFilteredNum.getAndIncrement();
                    String processedContext = AdvancedSensitiveWords.sensitiveWordBs.replace(originalContext);
                    event.setCancelled(true);
                    if (settingsManager.getProperty(PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL)) {
                        String fakeMessage = Bukkit.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI") ? me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, messagesManager.getProperty(PluginMessages.CHAT_FAKE_MESSAGE)).replace("%integrated_player%", userName).replace("%integrated_message%", originalMessage) : messagesManager.getProperty(PluginMessages.CHAT_FAKE_MESSAGE).replace("%integrated_player%", userName).replace("%integrated_message%", originalMessage);
                        user.sendMessage(ChatColor.translateAlternateColorCodes('&', fakeMessage));
                    }
                    if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                        user.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", userName).replace("%integrated_message%", originalMessage)));
                    }
                    if (settingsManager.getProperty(PluginSettings.ENABLE_API)) {
                        getScheduler().runTask(() -> Bukkit.getPluginManager().callEvent(new ASWFilterEvent(player, originalContext, processedContext, censoredContextList, EventType.CHAT, false)));
                    }
                    if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                        Utils.logViolation(userName + "(IP: " + user.getAddress().getAddress().getHostAddress() + ")(Chat)(Context)", originalContext + censoredContextList);
                    }
                    if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                        VelocitySender.send(player, EventType.CHAT, originalContext, censoredContextList);
                    }
                    if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                        BungeeSender.send(player, EventType.CHAT, originalContext, censoredContextList);
                    }
                    long endTime = System.currentTimeMillis();
                    addProcessStatistic(endTime, startTime);
                    getScheduler().runTask(() -> {
                        if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, EventType.CHAT, originalContext, censoredContextList);
                        if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) Punishment.punish(player);
                    });
                }
            }
        } else if (packetType == PacketType.Play.Client.CHAT_COMMAND) {
            WrapperPlayClientChatCommand wrapperPlayClientChatCommand = new WrapperPlayClientChatCommand(event);
            String originalCommand = settingsManager.getProperty(PluginSettings.PRE_PROCESS) ? wrapperPlayClientChatCommand.getCommand().replaceAll(getPreProcessRegex(), "") : wrapperPlayClientChatCommand.getCommand();
            if (shouldNotProcess(player, "/" + originalCommand)) return;
            long startTime = System.currentTimeMillis();
            List<String> censoredWords = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalCommand);
            if (!censoredWords.isEmpty()) {
                messagesFilteredNum.getAndIncrement();
                String processedCommand = AdvancedSensitiveWords.sensitiveWordBs.replace(originalCommand);
                if (settingsManager.getProperty(PluginSettings.CHAT_METHOD).equalsIgnoreCase("cancel")) {
                    event.setCancelled(true);
                } else {
                    int commandMaxLength = 255; // because there is a slash before the command, so we should minus 1
                    if (processedCommand.length() > commandMaxLength) {
                        wrapperPlayClientChatCommand.setCommand(processedCommand.substring(0, commandMaxLength));
                    } else {
                        wrapperPlayClientChatCommand.setCommand(processedCommand);
                    }
                }
                if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                    user.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", userName).replace("%integrated_message%", originalCommand)));
                }
                if (settingsManager.getProperty(PluginSettings.ENABLE_API)) {
                    getScheduler().runTask(() -> Bukkit.getPluginManager().callEvent(new ASWFilterEvent(player, originalCommand, processedCommand, censoredWords, EventType.CHAT, false)));
                }
                if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                    Utils.logViolation(userName + "(IP: " + user.getAddress().getAddress().getHostAddress() + ")(Chat)", "/" + originalCommand + censoredWords);
                }
                if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                    VelocitySender.send(player, EventType.CHAT, originalCommand, censoredWords);
                }
                if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                    BungeeSender.send(player, EventType.CHAT, originalCommand, censoredWords);
                }
                if (settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                    databaseManager.checkAndUpdatePlayer(player.getName());
                }
                long endTime = System.currentTimeMillis();
                addProcessStatistic(endTime, startTime);
                getScheduler().runTask(()-> {
                    if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, EventType.CHAT, originalCommand, censoredWords);
                    if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) Punishment.punish(player);
                });
            }
        }
    }
    private boolean shouldNotProcess(Player player, String message) {
        if (isInitialized && !player.hasPermission(Permissions.BYPASS) && !isCommandAndWhiteListed(message)) {
            if (isAuthMeAvailable && settingsManager.getProperty(PluginSettings.ENABLE_AUTHME_COMPATIBILITY)) {
                if (!fr.xephi.authme.api.v3.AuthMeApi.getInstance().isAuthenticated(player)) return true;
            }
            if (isCslAvailable && settingsManager.getProperty(PluginSettings.ENABLE_CSL_COMPATIBILITY)) {
                return !cc.baka9.catseedlogin.bukkit.CatSeedLoginAPI.isLogin(player.getName()) || !cc.baka9.catseedlogin.bukkit.CatSeedLoginAPI.isRegister(player.getName());
            }
            return false;
        }
        return true;
    }
}
