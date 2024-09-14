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
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.listener.FakeMessageExecutor
import io.wdsj.asw.bukkit.manage.notice.Notifier
import io.wdsj.asw.bukkit.manage.permission.PermissionsConstant
import io.wdsj.asw.bukkit.manage.permission.cache.CachingPermTool
import io.wdsj.asw.bukkit.manage.punish.Punishment
import io.wdsj.asw.bukkit.manage.punish.ViolationCounter
import io.wdsj.asw.bukkit.proxy.bungee.BungeeSender
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.type.ModuleType
import io.wdsj.asw.bukkit.util.LoggingUtils
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.context.ChatContext
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.util.*

/**
 * @author HaHaWTH & HeyWTF_IS_That and 0D00_0721
 * Made with ❤
 */
class ASWChatPacketListener : PacketListenerAbstract(PacketListenerPriority.LOW) {
    override fun onPacketReceive(event: PacketReceiveEvent) {
        if (!settingsManager.getProperty(PluginSettings.ENABLE_CHAT_CHECK)) return
        val packetType = event.packetType
        val user = event.user
        val userName = user.name
        val isCancelMode = settingsManager.getProperty(PluginSettings.CHAT_METHOD).equals("cancel", ignoreCase = true)
        if (packetType === PacketType.Play.Client.CHAT_MESSAGE) {
            val player = event.getPlayer() as Player
            val wrapperPlayClientChatMessage = WrapperPlayClientChatMessage(event)
            val originalMessage = if (settingsManager.getProperty(PluginSettings.PRE_PROCESS)) wrapperPlayClientChatMessage.message.replace(Utils.getPreProcessRegex().toRegex(), "") else wrapperPlayClientChatMessage.message
            if (shouldNotProcess(player, originalMessage)) return
            val startTime = System.currentTimeMillis()
            // Word check
            val censoredWords = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalMessage)
            if (censoredWords.isNotEmpty()) {
                Utils.messagesFilteredNum.getAndIncrement()
                val processedMessage = AdvancedSensitiveWords.sensitiveWordBs.replace(originalMessage)
                if (isCancelMode) {
                    if (settingsManager.getProperty(PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL) && Utils.isNotCommand(originalMessage)) {
                        FakeMessageExecutor.selfIncrement(player)
                    } else if (!settingsManager.getProperty(PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL)) {
                        event.isCancelled = true
                    }
                } else {
                    val maxLength = 256
                    if (processedMessage.length > maxLength) {
                        wrapperPlayClientChatMessage.message = processedMessage.substring(0, maxLength)
                    } else {
                        wrapperPlayClientChatMessage.message = processedMessage
                    }
                }
                if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                    user.sendMessage(ChatColor.translateAlternateColorCodes('&', AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", userName).replace("%integrated_message%", originalMessage)))
                }

                if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                    LoggingUtils.logViolation(userName + "(IP: " + user.address.address.hostAddress + ")(Chat)", originalMessage + censoredWords)
                }
                ViolationCounter.incrementViolationCount(player)
                if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                    VelocitySender.sendNotifyMessage(player, ModuleType.CHAT, originalMessage, censoredWords)
                }
                if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                    BungeeSender.sendNotifyMessage(player, ModuleType.CHAT, originalMessage, censoredWords)
                }
                val endTime = System.currentTimeMillis()
                TimingUtils.addProcessStatistic(endTime, startTime)
                if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, ModuleType.CHAT, originalMessage, censoredWords)
                if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) {
                    AdvancedSensitiveWords.getScheduler().runTask {
                        Punishment.punish(player)
                    }
                }
                return
            } else {
                if (settingsManager.getProperty(PluginSettings.ENABLE_OLLAMA_AI_MODEL_CHECK)
                    && AdvancedSensitiveWords.getOllamaProcessor().isOllamaInit && Utils.isNotCommand(originalMessage)) {
                    val processor = AdvancedSensitiveWords.getOllamaProcessor()
                    processor.process(originalMessage)
                        .thenAccept {
                            try {
                                val rating = it?.toInt() ?: 0
                                if (rating > settingsManager.getProperty(PluginSettings.OLLAMA_AI_SENSITIVE_THRESHOLD)) {
                                    val unsupportedList = Collections.singletonList("Unsupported")
                                    Utils.messagesFilteredNum.getAndIncrement()
                                    if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                                        player.sendMessage(
                                            ChatColor.translateAlternateColorCodes(
                                                '&',
                                                AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT)
                                                    .replace("%integrated_player%", player.name)
                                                    .replace("%integrated_message%", originalMessage)
                                            )
                                        )
                                    }
                                    if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                                        LoggingUtils.logViolation(
                                            player.name + "(IP: " + user.address.address.hostAddress + ")(Chat AI)(LLM output: $it)",
                                            originalMessage + unsupportedList
                                        )
                                    }
                                    ViolationCounter.incrementViolationCount(player)
                                    if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                                        VelocitySender.sendNotifyMessage(
                                            player,
                                            ModuleType.CHAT_AI,
                                            originalMessage,
                                            unsupportedList
                                        )
                                    }
                                    if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                                        BungeeSender.sendNotifyMessage(player, ModuleType.CHAT_AI, originalMessage, unsupportedList)
                                    }
                                    if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) {
                                        Notifier.notice(player, ModuleType.CHAT_AI, originalMessage, unsupportedList)
                                    }
                                    if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH) && settingsManager.getProperty(PluginSettings.OLLAMA_AI_PUNISH)) {
                                        AdvancedSensitiveWords.getScheduler().runTask { Punishment.punish(player) }
                                    }
                                }
                            } catch (e: NumberFormatException) {
                                LOGGER.warning("Failed to parse Ollama output to a number: $it")
                            }
                        }
                }

                if (settingsManager.getProperty(PluginSettings.ENABLE_OPENAI_AI_MODEL_CHECK)
                    && AdvancedSensitiveWords.getOpenAIProcessor().isOpenAiInit && Utils.isNotCommand(originalMessage)) {
                    val processor = AdvancedSensitiveWords.getOpenAIProcessor()
                    processor.process(originalMessage)
                        .thenAccept {
                            val results = it.results() ?: return@thenAccept
                            for (result in results) {
                                if (result.isFlagged) {
                                    val categories = result.categories()
                                    var isViolated = false
                                    if (categories.hateThreatening() && settingsManager.getProperty(PluginSettings.OPENAI_ENABLE_HATE_THREATENING_CHECK)) {
                                        isViolated = true
                                    } else if (categories.hate() && settingsManager.getProperty(PluginSettings.OPENAI_ENABLE_HATE_CHECK)) {
                                        isViolated = true
                                    } else if (categories.selfHarm() && settingsManager.getProperty(PluginSettings.OPENAI_ENABLE_SELF_HARM_CHECK)) {
                                        isViolated = true
                                    } else if (categories.sexual() && settingsManager.getProperty(PluginSettings.OPENAI_ENABLE_SEXUAL_CONTENT_CHECK)) {
                                        isViolated = true
                                    } else if (categories.sexualMinors() && settingsManager.getProperty(PluginSettings.OPENAI_ENABLE_SEXUAL_MINORS_CHECK)) {
                                        isViolated = true
                                    } else if (categories.violence() && settingsManager.getProperty(PluginSettings.OPENAI_ENABLE_VIOLENCE_CHECK)) {
                                        isViolated = true
                                    }
                                    if (isViolated) {
                                        val unsupportedList = Collections.singletonList("Unsupported")
                                        Utils.messagesFilteredNum.getAndIncrement()
                                        if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                                            player.sendMessage(
                                                ChatColor.translateAlternateColorCodes(
                                                    '&',
                                                    AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT)
                                                        .replace("%integrated_player%", player.name)
                                                        .replace("%integrated_message%", originalMessage)
                                                )
                                            )
                                        }
                                        if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                                            LoggingUtils.logViolation(
                                                player.name + "(IP: " + user.address.address.hostAddress + ")(Chat AI)(OPENAI)",
                                                originalMessage + unsupportedList
                                            )
                                        }
                                        ViolationCounter.incrementViolationCount(player)
                                        if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                                            VelocitySender.sendNotifyMessage(
                                                player,
                                                ModuleType.CHAT_AI,
                                                originalMessage,
                                                unsupportedList
                                            )
                                        }
                                        if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                                            BungeeSender.sendNotifyMessage(
                                                player,
                                                ModuleType.CHAT_AI,
                                                originalMessage,
                                                unsupportedList
                                            )
                                        }
                                        if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) {
                                            Notifier.notice(
                                                player,
                                                ModuleType.CHAT_AI,
                                                originalMessage,
                                                unsupportedList
                                            )
                                        }
                                        if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH) && settingsManager.getProperty(PluginSettings.OPENAI_AI_PUNISH)) {
                                            AdvancedSensitiveWords.getScheduler().runTask { Punishment.punish(player) }
                                        }
                                    }
                                }
                            }
                        }
                }
            }

            // Context check
            if (settingsManager.getProperty(PluginSettings.CHAT_CONTEXT_CHECK) && Utils.isNotCommand(originalMessage)) {
                ChatContext.addMessage(player, originalMessage)
                val queue = ChatContext.getHistory(player)
                val originalContext = queue.joinToString("")
                val censoredContextList = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalContext)
                if (censoredContextList.isNotEmpty()) {
                    ChatContext.pollPlayerContext(player)
                    Utils.messagesFilteredNum.getAndIncrement()
                    if (settingsManager.getProperty(PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL)) {
                        FakeMessageExecutor.selfIncrement(player)
                    } else {
                        event.isCancelled = true
                    }
                    if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                        user.sendMessage(ChatColor.translateAlternateColorCodes('&', AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", userName).replace("%integrated_message%", originalMessage)))
                    }
                    if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                        LoggingUtils.logViolation(userName + "(IP: " + Utils.getPlayerIp(player) + ")(Chat)(Context)", originalContext + censoredContextList)
                    }
                    ViolationCounter.incrementViolationCount(player)
                    if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                        VelocitySender.sendNotifyMessage(player, ModuleType.CHAT, originalContext, censoredContextList)
                    }
                    if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                        BungeeSender.sendNotifyMessage(player, ModuleType.CHAT, originalContext, censoredContextList)
                    }
                    val endTime = System.currentTimeMillis()
                    TimingUtils.addProcessStatistic(endTime, startTime)
                    if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) {
                        Notifier.notice(player, ModuleType.CHAT, originalContext, censoredContextList)
                    }
                    if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) {
                        AdvancedSensitiveWords.getScheduler().runTask {
                            Punishment.punish(player)
                        }
                    }
                }
            }
        } else if (packetType === PacketType.Play.Client.CHAT_COMMAND) {
            val player = event.getPlayer() as Player
            val wrapperPlayClientChatCommand = WrapperPlayClientChatCommand(event)
            val originalCommand = if (settingsManager.getProperty(PluginSettings.PRE_PROCESS)) wrapperPlayClientChatCommand.command.replace(Utils.getPreProcessRegex().toRegex(), "") else wrapperPlayClientChatCommand.command
            if (shouldNotProcess(player, "/$originalCommand")) return
            val startTime = System.currentTimeMillis()
            val censoredWords = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalCommand)
            if (censoredWords.isNotEmpty()) {
                Utils.messagesFilteredNum.getAndIncrement()
                val processedCommand = AdvancedSensitiveWords.sensitiveWordBs.replace(originalCommand)
                if (isCancelMode) {
                    event.isCancelled = true
                } else {
                    val commandMaxLength = 256
                    if (processedCommand.length > commandMaxLength) {
                        wrapperPlayClientChatCommand.command = processedCommand.substring(0, commandMaxLength)
                    } else {
                        wrapperPlayClientChatCommand.command = processedCommand
                    }
                }
                if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                    user.sendMessage(ChatColor.translateAlternateColorCodes('&', AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", userName).replace("%integrated_message%", originalCommand)))
                }
                if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                    LoggingUtils.logViolation(userName + "(IP: " + Utils.getPlayerIp(player) + ")(Chat)", "/$originalCommand$censoredWords")
                }
                ViolationCounter.incrementViolationCount(player)
                if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                    VelocitySender.sendNotifyMessage(player, ModuleType.CHAT, originalCommand, censoredWords)
                }
                if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                    BungeeSender.sendNotifyMessage(player, ModuleType.CHAT, originalCommand, censoredWords)
                }
                val endTime = System.currentTimeMillis()
                TimingUtils.addProcessStatistic(endTime, startTime)
                if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, ModuleType.CHAT, originalCommand, censoredWords)
                if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) {
                    AdvancedSensitiveWords.getScheduler().runTask {
                        Punishment.punish(player)
                    }
                }
            }
        }
    }

    private fun shouldNotProcess(player: Player, message: String): Boolean {
        if (AdvancedSensitiveWords.isInitialized && !CachingPermTool.hasPermission(PermissionsConstant.BYPASS, player) && !Utils.isCommandAndWhiteListed(message)) {
            if (AdvancedSensitiveWords.isAuthMeAvailable && settingsManager.getProperty(PluginSettings.ENABLE_AUTHME_COMPATIBILITY)) {
                if (!AuthMeApi.getInstance().isAuthenticated(player)) return true
            }
            if (AdvancedSensitiveWords.isCslAvailable && settingsManager.getProperty(PluginSettings.ENABLE_CSL_COMPATIBILITY)) {
                return !CatSeedLoginAPI.isLogin(player.name) || !CatSeedLoginAPI.isRegister(player.name)
            }
            return false
        }
        return true
    }
}
