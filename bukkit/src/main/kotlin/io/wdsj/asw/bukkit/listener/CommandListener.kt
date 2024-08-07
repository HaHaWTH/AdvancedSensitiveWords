package io.wdsj.asw.bukkit.listener

import cc.baka9.catseedlogin.bukkit.CatSeedLoginAPI
import fr.xephi.authme.api.v3.AuthMeApi
import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.type.ModuleType
import io.wdsj.asw.bukkit.manage.notice.Notifier
import io.wdsj.asw.bukkit.manage.permission.Permissions
import io.wdsj.asw.bukkit.manage.punish.Punishment
import io.wdsj.asw.bukkit.proxy.bungee.BungeeSender
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent

@Suppress("unused")
class CommandListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        val player = event.player
        val originalCommand =
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.PRE_PROCESS)) event.message.replace(
                Utils.getPreProcessRegex().toRegex(), ""
            ) else event.message
        if (shouldNotProcess(player, originalCommand)) return
        val censoredWordList = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalCommand)
        val startTime = System.currentTimeMillis()
        if (censoredWordList.isNotEmpty()) {
            Utils.messagesFilteredNum.getAndIncrement()
            val processedCommand = AdvancedSensitiveWords.sensitiveWordBs.replace(originalCommand)
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.CHAT_METHOD)
                    .equals("cancel", ignoreCase = true)
            ) {
                event.isCancelled = true
            } else {
                if (Utils.isCommand(processedCommand)) {
                    event.message = processedCommand
                } else {
                    event.message = "/$processedCommand"
                }
            }
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.CHAT_SEND_MESSAGE)) {
                player.sendMessage(
                    ChatColor.translateAlternateColorCodes(
                        '&',
                        AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_CHAT)
                            .replace("%integrated_player%", player.name)
                            .replace("%integrated_message%", originalCommand)
                    )
                )
            }
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                Utils.logViolation(
                    player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Chat)",
                    originalCommand + censoredWordList
                )
            }
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                VelocitySender.send(player, ModuleType.CHAT, originalCommand, censoredWordList)
            }
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                BungeeSender.send(player, ModuleType.CHAT, originalCommand, censoredWordList)
            }
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                AdvancedSensitiveWords.databaseManager.checkAndUpdatePlayer(player.name)
            }
            val endTime = System.currentTimeMillis()
            TimingUtils.addProcessStatistic(endTime, startTime)
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(
                player,
                ModuleType.CHAT,
                originalCommand,
                censoredWordList
            )
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.CHAT_PUNISH)) Punishment.punish(player)
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
