package io.wdsj.asw.bukkit.listener

import cc.baka9.catseedlogin.bukkit.CatSeedLoginAPI
import fr.xephi.authme.api.v3.AuthMeApi
import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.manage.notice.Notifier
import io.wdsj.asw.bukkit.manage.permission.Permissions
import io.wdsj.asw.bukkit.manage.punish.PlayerAltController
import io.wdsj.asw.bukkit.manage.punish.Punishment
import io.wdsj.asw.bukkit.proxy.bungee.BungeeSender
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.type.ModuleType
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.context.ChatContext
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import java.util.*

@Suppress("unused")
class ChatListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onChat(event: AsyncPlayerChatEvent) {
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
                    val players: MutableCollection<Player> = event.recipients
                    players.clear()
                    if (settingsManager.getProperty(PluginSettings.ENABLE_ALTS_CHECK) && PlayerAltController.hasAlt(player)) {
                        val alts = PlayerAltController.getAlts(player)
                        for (alt in alts) {
                            val altPlayer = Bukkit.getPlayer(alt)
                            altPlayer?.let { players.add(it) }
                        }
                    }
                    players.add(player)
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
                VelocitySender.send(player, ModuleType.CHAT, originalMessage, censoredWordList)
            }
            if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                BungeeSender.send(player, ModuleType.CHAT, originalMessage, censoredWordList)
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
        } else if (settingsManager.getProperty(PluginSettings.ENABLE_AI_MODEL_CHECK)
            && AdvancedSensitiveWords.getOllamaProcessor().isOllamaInit) {
            val processor = AdvancedSensitiveWords.getOllamaProcessor()
            processor.process(originalMessage)
                .thenAccept {
                    val rating = it?.toInt() ?: 0
                    if (rating > settingsManager.getProperty(PluginSettings.AI_SENSITIVE_THRESHOLD)) {
                        val unsupportedList = Collections.singletonList("Unsupported")
                        Utils.messagesFilteredNum.getAndIncrement()
                        if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", player.name).replace("%integrated_message%", originalMessage)))
                        }
                        if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                            Utils.logViolation(player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Chat AI)(LLM output: $it)", originalMessage + unsupportedList)
                        }
                        if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                            VelocitySender.send(player, ModuleType.CHAT_AI, originalMessage, unsupportedList)
                        }
                        if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                            BungeeSender.send(player, ModuleType.CHAT_AI, originalMessage, unsupportedList)
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

        if (settingsManager.getProperty(PluginSettings.CHAT_CONTEXT_CHECK)) {
            ChatContext.addMessage(player, originalMessage)
            val queue = ChatContext.getHistory(player)
            val originalContext = queue.joinToString("")
            val censoredContextList = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalContext)
            if (censoredContextList.isNotEmpty()) {
                ChatContext.pollPlayerContext(player)
                Utils.messagesFilteredNum.getAndIncrement()
                event.isCancelled = true
                if (settingsManager.getProperty(PluginSettings.CHAT_FAKE_MESSAGE_ON_CANCEL)) {
                    val players: MutableCollection<Player> = event.recipients
                    players.clear()
                    if (settingsManager.getProperty(PluginSettings.ENABLE_ALTS_CHECK) && PlayerAltController.hasAlt(player)) {
                        val alts = PlayerAltController.getAlts(player)
                        for (alt in alts) {
                            val altPlayer = Bukkit.getPlayer(alt)
                            altPlayer?.let { players.add(it) }
                        }
                    }
                    players.add(player)
                }
                if (settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT).replace("%integrated_player%", player.name).replace("%integrated_message%", originalMessage)))
                }
                if (settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                    Utils.logViolation(player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Chat)(Context)", originalContext + censoredContextList)
                }
                if (settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                    VelocitySender.send(player, ModuleType.CHAT, originalContext, censoredContextList)
                }
                if (settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                    BungeeSender.send(player, ModuleType.CHAT, originalContext, censoredContextList)
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
