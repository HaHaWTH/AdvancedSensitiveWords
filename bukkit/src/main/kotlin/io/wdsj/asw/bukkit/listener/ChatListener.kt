package io.wdsj.asw.bukkit.listener

import cc.baka9.catseedlogin.bukkit.CatSeedLoginAPI
import fr.xephi.authme.api.v3.AuthMeApi
import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.manage.notice.Notifier
import io.wdsj.asw.bukkit.manage.permission.Permissions
import io.wdsj.asw.bukkit.manage.punish.Punishment
import io.wdsj.asw.bukkit.proxy.bungee.BungeeSender
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.type.ModuleType
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.context.ChatContext
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import java.util.*

class ChatListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onChat(event: AsyncPlayerChatEvent) {
        if (!settingsManager.getProperty(PluginSettings.ENABLE_CHAT_CHECK)) return
        val player = event.player
        if (shouldNotProcess(player)) return
        val isCancelMode = settingsManager.getProperty(PluginSettings.CHAT_METHOD).equals("cancel", ignoreCase = true)
        val originalMessage = if (settingsManager.getProperty(PluginSettings.PRE_PROCESS)) event.message.replace(Utils.getPreProcessRegex().toRegex(), "") else event.message
        val censoredWordList = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalMessage)
        val startTime = System.currentTimeMillis()
        if (censoredWordList.isNotEmpty()) {
            Utils.messagesFilteredNum.getAndIncrement()
            val processedMessage = AdvancedSensitiveWords.sensitiveWordBs.replace(originalMessage)
            if (isCancelMode) {
                if (settingsManager.getProperty(PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL)) {
                    FakeMessageExecutor.selfIncrement(player)
                } else {
                    event.isCancelled = true
                }
            } else {
                event.message = processedMessage
            }
            if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", player.name).replace("%integrated_message%", originalMessage)))
            }
            if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                Utils.logViolation(player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Chat)", originalMessage + censoredWordList)
            }
            if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                VelocitySender.sendNotifyMessage(player, ModuleType.CHAT, originalMessage, censoredWordList)
            }
            if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                BungeeSender.sendNotifyMessage(player, ModuleType.CHAT, originalMessage, censoredWordList)
            }
            if (settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                AdvancedSensitiveWords.databaseManager.checkAndUpdatePlayer(player.name)
            }
            val endTime = System.currentTimeMillis()
            TimingUtils.addProcessStatistic(endTime, startTime)
            if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, ModuleType.CHAT, originalMessage, censoredWordList)
            AdvancedSensitiveWords.getScheduler().runTask {
                if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) Punishment.punish(player)
            }
            return
        } else {
            if (settingsManager.getProperty(PluginSettings.ENABLE_OLLAMA_AI_MODEL_CHECK)
                    && AdvancedSensitiveWords.getOllamaProcessor().isOllamaInit) {
                val processor = AdvancedSensitiveWords.getOllamaProcessor()
                processor.process(originalMessage)
                    .thenAccept {
                        try {
                            val rating = it?.toInt() ?: 0
                            if (rating > settingsManager.getProperty(PluginSettings.OLLAMA_AI_SENSITIVE_THRESHOLD)) {
                                val unsupportedList = Collections.singletonList("Unsupported")
                                Utils.messagesFilteredNum.getAndIncrement()
                                if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", player.name).replace("%integrated_message%", originalMessage)))
                                }
                                if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                                    Utils.logViolation(player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Chat AI)(LLM output: $it)", originalMessage + unsupportedList)
                                }
                                if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                                    VelocitySender.sendNotifyMessage(player, ModuleType.CHAT_AI, originalMessage, unsupportedList)
                                }
                                if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                                    BungeeSender.sendNotifyMessage(player, ModuleType.CHAT_AI, originalMessage, unsupportedList)
                                }
                                if (settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                                    AdvancedSensitiveWords.databaseManager.checkAndUpdatePlayer(player.name)
                                }
                                if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) {
                                    Notifier.notice(player, ModuleType.CHAT_AI, originalMessage, unsupportedList)
                                }
                                if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) {
                                    AdvancedSensitiveWords.getScheduler().runTask { Punishment.punish(player) }
                                }
                            }
                        } catch (e: NumberFormatException) {
                            LOGGER.severe("Failed to parse Ollama output to a number: $it")
                        }
                    }
            }
            if (settingsManager.getProperty(PluginSettings.ENABLE_OPENAI_AI_MODEL_CHECK)
                && AdvancedSensitiveWords.getOpenAIProcessor().isOpenAiInit) {
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
                                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", player.name).replace("%integrated_message%", originalMessage)))
                                    }
                                    if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                                        Utils.logViolation(player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Chat AI)(OPENAI)", originalMessage + unsupportedList)
                                    }
                                    if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                                        VelocitySender.sendNotifyMessage(player, ModuleType.CHAT_AI, originalMessage, unsupportedList)
                                    }
                                    if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                                        BungeeSender.sendNotifyMessage(player, ModuleType.CHAT_AI, originalMessage, unsupportedList)
                                    }
                                    if (settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                                        AdvancedSensitiveWords.databaseManager.checkAndUpdatePlayer(player.name)
                                    }
                                    if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) {
                                        Notifier.notice(player, ModuleType.CHAT_AI, originalMessage, unsupportedList)
                                    }
                                    if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) {
                                        AdvancedSensitiveWords.getScheduler().runTask { Punishment.punish(player) }
                                    }
                                }
                            }
                        }
                    }
            }
        }

        if (settingsManager.getProperty(PluginSettings.CHAT_CONTEXT_CHECK)) {
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
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", player.name).replace("%integrated_message%", originalMessage)))
                }
                if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                    Utils.logViolation(player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Chat)(Context)", originalContext + censoredContextList)
                }
                if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                    VelocitySender.sendNotifyMessage(player, ModuleType.CHAT, originalContext, censoredContextList)
                }
                if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                    BungeeSender.sendNotifyMessage(player, ModuleType.CHAT, originalContext, censoredContextList)
                }
                if (settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                    AdvancedSensitiveWords.databaseManager.checkAndUpdatePlayer(player.name)
                }
                val endTime = System.currentTimeMillis()
                TimingUtils.addProcessStatistic(endTime, startTime)
                if (settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(player, ModuleType.CHAT, originalContext, censoredContextList)
                if (settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) {
                    AdvancedSensitiveWords.getScheduler().runTask {
                        Punishment.punish(player)
                    }
                }
            }
        }
    }


    private fun shouldNotProcess(player: Player): Boolean {
        if (AdvancedSensitiveWords.isInitialized && !player.hasPermission(Permissions.BYPASS)) {
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
