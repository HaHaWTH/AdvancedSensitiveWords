package io.wdsj.asw.bukkit.listener;

import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.event.ASWFilterEvent;
import io.wdsj.asw.bukkit.event.EventType;
import io.wdsj.asw.bukkit.manage.notice.Notifier;
import io.wdsj.asw.bukkit.manage.punish.Punishment;
import io.wdsj.asw.bukkit.proxy.bungee.BungeeSender;
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender;
import io.wdsj.asw.bukkit.setting.PluginMessages;
import io.wdsj.asw.bukkit.setting.PluginSettings;
import io.wdsj.asw.bukkit.util.TimingUtils;
import io.wdsj.asw.bukkit.util.Utils;
import io.wdsj.asw.bukkit.util.context.ChatContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Collection;
import java.util.Deque;
import java.util.List;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.*;
import static io.wdsj.asw.bukkit.util.TimingUtils.addProcessStatistic;
import static io.wdsj.asw.bukkit.util.Utils.getPlayerIp;
import static io.wdsj.asw.bukkit.util.Utils.messagesFilteredNum;

@SuppressWarnings("unused")
public class ChatListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (shouldNotProcess(player)) return;
        String originalMessage = settingsManager.getProperty(PluginSettings.IGNORE_FORMAT_CODE) ? event.getMessage().replaceAll(Utils.getIgnoreFormatCodeRegex(), "") : event.getMessage();
        List<String> censoredWordList = sensitiveWordBs.findAll(originalMessage);
        long startTime = System.currentTimeMillis();
        if (!censoredWordList.isEmpty()) {
            messagesFilteredNum.getAndIncrement();
            String processedMessage = sensitiveWordBs.replace(originalMessage);
            if (settingsManager.getProperty(PluginSettings.CHAT_METHOD).equalsIgnoreCase("cancel")) {
                event.setCancelled(true);
                if (settingsManager.getProperty(PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL)) {
                    Collection<Player> players = event.getRecipients();
                    players.clear();
                    players.add(player);
                }
            } else {
                event.setMessage(processedMessage);
            }
            if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", player.getName()).replace("%integrated_message%", originalMessage)));
            }
            if (settingsManager.getProperty(PluginSettings.ENABLE_API)) {
                getScheduler().runTask(() -> Bukkit.getPluginManager().callEvent(new ASWFilterEvent(player, originalMessage, processedMessage, censoredWordList, EventType.CHAT, false)));
            }
            if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                Utils.logViolation(player.getName() + "(IP: " + getPlayerIp(player) + ")(Chat)", originalMessage + censoredWordList);
            }
            if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                VelocitySender.send(player, EventType.CHAT, originalMessage);
            }
            if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                BungeeSender.send(player, EventType.CHAT, originalMessage);
            }
            if (settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                databaseManager.checkAndUpdatePlayer(player.getName());
            }
            long endTime = System.currentTimeMillis();
            TimingUtils.addProcessStatistic(endTime, startTime);
            getScheduler().runTask(()-> {
                if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, EventType.CHAT, originalMessage);
                if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) Punishment.punish(player);
            });
            return;
        }

        if (settingsManager.getProperty(PluginSettings.CHAT_CONTEXT_CHECK)) {
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
                    Collection<Player> players = event.getRecipients();
                    players.clear();
                    players.add(player);
                }
                if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", player.getName()).replace("%integrated_message%", originalMessage)));
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



    private boolean shouldNotProcess(Player player) {
        if (isInitialized && !player.hasPermission("advancedsensitivewords.bypass")) {
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
