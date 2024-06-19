package io.wdsj.asw.bukkit.listener.packet

import cc.baka9.catseedlogin.bukkit.CatSeedLoginAPI
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommand
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage
import fr.xephi.authme.api.v3.AuthMeApi
import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.event.ASWFilterEvent
import io.wdsj.asw.bukkit.event.EventType
import io.wdsj.asw.bukkit.manage.notice.Notifier
import io.wdsj.asw.bukkit.manage.permission.Permissions
import io.wdsj.asw.bukkit.manage.punish.Punishment
import io.wdsj.asw.bukkit.proxy.bungee.BungeeSender
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.context.ChatContext
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player

/**
 * @author HaHaWTH & HeyWTF_IS_That and 0D00_0721
 * Made with ❤
 */
class ASWChatPacketListener : PacketListenerAbstract(PacketListenerPriority.LOW) {
    override fun onPacketReceive(event: PacketReceiveEvent) {
        val packetType = event.packetType
        val user = event.user
        val player = event.player as Player
        val userName = user.name
        val isCancelMode = AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.CHAT_METHOD).equals("cancel", ignoreCase = true)
        if (packetType === PacketType.Play.Client.CHAT_MESSAGE) {
            val wrapperPlayClientChatMessage = WrapperPlayClientChatMessage(event)
            val originalMessage = if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.PRE_PROCESS)) wrapperPlayClientChatMessage.message.replace(Utils.getPreProcessRegex().toRegex(), "") else wrapperPlayClientChatMessage.message
            if (shouldNotProcess(player, originalMessage)) return
            val startTime = System.currentTimeMillis()
            // Word check
            val censoredWords = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalMessage)
            if (censoredWords.isNotEmpty()) {
                Utils.messagesFilteredNum.getAndIncrement()
                val processedMessage = AdvancedSensitiveWords.sensitiveWordBs.replace(originalMessage)
                if (isCancelMode) {
                    event.isCancelled = true
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL) && Utils.isNotCommand(originalMessage)) {
                        val fakeMessage = if (Bukkit.getServer().pluginManager.isPluginEnabled("PlaceholderAPI")) PlaceholderAPI.setPlaceholders(player, AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.CHAT_FAKE_MESSAGE)).replace("%integrated_player%", userName).replace("%integrated_message%", originalMessage) else AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.CHAT_FAKE_MESSAGE).replace("%integrated_player%", userName).replace("%integrated_message%", originalMessage)
                        user.sendMessage(ChatColor.translateAlternateColorCodes('&', fakeMessage))
                    }
                } else {
                    val maxLength = 256
                    if (processedMessage.length > maxLength) {
                        wrapperPlayClientChatMessage.message = processedMessage.substring(0, maxLength)
                    } else {
                        wrapperPlayClientChatMessage.message = processedMessage
                    }
                }
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                    user.sendMessage(ChatColor.translateAlternateColorCodes('&', AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", userName).replace("%integrated_message%", originalMessage)))
                }

                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_API)) {
                    AdvancedSensitiveWords.getScheduler().runTask { Bukkit.getPluginManager().callEvent(ASWFilterEvent(player, originalMessage, processedMessage, censoredWords, EventType.CHAT, false)) }
                }
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                    Utils.logViolation(userName + "(IP: " + user.address.address.hostAddress + ")(Chat)", originalMessage + censoredWords)
                }
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                    VelocitySender.send(player, EventType.CHAT, originalMessage, censoredWords)
                }
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                    BungeeSender.send(player, EventType.CHAT, originalMessage, censoredWords)
                }
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                    AdvancedSensitiveWords.databaseManager.checkAndUpdatePlayer(player.name)
                }
                val endTime = System.currentTimeMillis()
                TimingUtils.addProcessStatistic(endTime, startTime)
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, EventType.CHAT, originalMessage, censoredWords)
                AdvancedSensitiveWords.getScheduler().runTask {
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) Punishment.punish(player)
                }
                return
            }

            // Context check
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.CHAT_CONTEXT_CHECK) && Utils.isNotCommand(originalMessage)) {
                ChatContext.addMessage(player, originalMessage)
                val queue = ChatContext.getHistory(player)
                val originalContext = java.lang.String.join("", queue)
                val censoredContextList = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalContext)
                if (censoredContextList.isNotEmpty()) {
                    ChatContext.removePlayerContext(player)
                    Utils.messagesFilteredNum.getAndIncrement()
                    val processedContext = AdvancedSensitiveWords.sensitiveWordBs.replace(originalContext)
                    event.isCancelled = true
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL)) {
                        val fakeMessage = if (Bukkit.getServer().pluginManager.isPluginEnabled("PlaceholderAPI")) PlaceholderAPI.setPlaceholders(player, AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.CHAT_FAKE_MESSAGE)).replace("%integrated_player%", userName).replace("%integrated_message%", originalMessage) else AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.CHAT_FAKE_MESSAGE).replace("%integrated_player%", userName).replace("%integrated_message%", originalMessage)
                        user.sendMessage(ChatColor.translateAlternateColorCodes('&', fakeMessage))
                    }
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                        user.sendMessage(ChatColor.translateAlternateColorCodes('&', AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", userName).replace("%integrated_message%", originalMessage)))
                    }
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_API)) {
                        AdvancedSensitiveWords.getScheduler().runTask { Bukkit.getPluginManager().callEvent(ASWFilterEvent(player, originalContext, processedContext, censoredContextList, EventType.CHAT, false)) }
                    }
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                        Utils.logViolation(userName + "(IP: " + user.address.address.hostAddress + ")(Chat)(Context)", originalContext + censoredContextList)
                    }
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                        VelocitySender.send(player, EventType.CHAT, originalContext, censoredContextList)
                    }
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                        BungeeSender.send(player, EventType.CHAT, originalContext, censoredContextList)
                    }
                    val endTime = System.currentTimeMillis()
                    TimingUtils.addProcessStatistic(endTime, startTime)
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) {
                        AdvancedSensitiveWords.getScheduler().runTask {
                            Notifier.notice(player, EventType.CHAT, originalContext, censoredContextList)
                        }
                    }
                    if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) {
                        AdvancedSensitiveWords.getScheduler().runTask {
                            Punishment.punish(player)
                        }
                    }
                }
            }
        } else if (packetType === PacketType.Play.Client.CHAT_COMMAND) {
            val wrapperPlayClientChatCommand = WrapperPlayClientChatCommand(event)
            val originalCommand = if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.PRE_PROCESS)) wrapperPlayClientChatCommand.command.replace(Utils.getPreProcessRegex().toRegex(), "") else wrapperPlayClientChatCommand.command
            if (shouldNotProcess(player, "/$originalCommand")) return
            val startTime = System.currentTimeMillis()
            val censoredWords = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalCommand)
            if (censoredWords.isNotEmpty()) {
                Utils.messagesFilteredNum.getAndIncrement()
                val processedCommand = AdvancedSensitiveWords.sensitiveWordBs.replace(originalCommand)
                if (isCancelMode) {
                    event.isCancelled = true
                } else {
                    val commandMaxLength = 255 // because there is a slash before the command, so we should minus 1
                    if (processedCommand.length > commandMaxLength) {
                        wrapperPlayClientChatCommand.command = processedCommand.substring(0, commandMaxLength)
                    } else {
                        wrapperPlayClientChatCommand.command = processedCommand
                    }
                }
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                    user.sendMessage(ChatColor.translateAlternateColorCodes('&', AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", userName).replace("%integrated_message%", originalCommand)))
                }
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_API)) {
                    AdvancedSensitiveWords.getScheduler().runTask { Bukkit.getPluginManager().callEvent(ASWFilterEvent(player, originalCommand, processedCommand, censoredWords, EventType.CHAT, false)) }
                }
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                    Utils.logViolation(userName + "(IP: " + user.address.address.hostAddress + ")(Chat)", "/$originalCommand$censoredWords")
                }
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                    VelocitySender.send(player, EventType.CHAT, originalCommand, censoredWords)
                }
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                    BungeeSender.send(player, EventType.CHAT, originalCommand, censoredWords)
                }
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                    AdvancedSensitiveWords.databaseManager.checkAndUpdatePlayer(player.name)
                }
                val endTime = System.currentTimeMillis()
                TimingUtils.addProcessStatistic(endTime, startTime)
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, EventType.CHAT, originalCommand, censoredWords)
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) {
                    AdvancedSensitiveWords.getScheduler().runTask {
                        Punishment.punish(player)
                    }
                }
            }
        }
    }

    private fun shouldNotProcess(player: Player, message: String): Boolean {
        if (AdvancedSensitiveWords.isInitialized && !player.hasPermission(Permissions.BYPASS) && !Utils.isCommandAndWhiteListed(message)) {
            if (AdvancedSensitiveWords.isAuthMeAvailable && AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_AUTHME_COMPATIBILITY)) {
                if (!AuthMeApi.getInstance().isAuthenticated(player)) return true
            }
            if (AdvancedSensitiveWords.isCslAvailable && AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_CSL_COMPATIBILITY)) {
                return !CatSeedLoginAPI.isLogin(player.name) || !CatSeedLoginAPI.isRegister(player.name)
            }
            return false
        }
        return true
    }
}
