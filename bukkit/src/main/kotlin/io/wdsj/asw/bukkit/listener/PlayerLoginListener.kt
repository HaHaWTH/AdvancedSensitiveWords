package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.manage.notice.Notifier
import io.wdsj.asw.bukkit.manage.permission.Permissions
import io.wdsj.asw.bukkit.manage.punish.Punishment
import io.wdsj.asw.bukkit.proxy.bungee.BungeeSender
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.type.ModuleType
import io.wdsj.asw.bukkit.util.PlayerUtils
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.geysermc.floodgate.api.FloodgateApi

class PlayerLoginListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    fun onLogin(event: PlayerLoginEvent) {
        if (!AdvancedSensitiveWords.isInitialized || !AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_PLAYER_NAME_CHECK)) return
        val player = event.player
        if (player.hasPermission(Permissions.BYPASS)) return
        if (PlayerUtils.isNpc(player) && AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.NAME_IGNORE_NPC)) return
        if (Bukkit.getPluginManager()
                .getPlugin("floodgate") != null && AdvancedSensitiveWords.settingsManager.getProperty(
                PluginSettings.NAME_IGNORE_BEDROCK
            )
        ) {
            if (FloodgateApi.getInstance().isFloodgatePlayer(player.uniqueId)) return
        }
        val playerName = player.name
        val startTime = System.currentTimeMillis()
        val censoredWordList = AdvancedSensitiveWords.sensitiveWordBs.findAll(playerName)
        if (censoredWordList.isNotEmpty()) {
            val processedPlayerName = AdvancedSensitiveWords.sensitiveWordBs.replace(playerName)
            val playerIp = event.address.hostAddress
            Utils.messagesFilteredNum.getAndIncrement()
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.NAME_METHOD)
                    .equals("replace", ignoreCase = true)
            ) {
                player.setDisplayName(processedPlayerName)
                player.setPlayerListName(processedPlayerName)
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.NAME_SEND_MESSAGE)) {
                    AdvancedSensitiveWords.getScheduler().runTaskLater({
                        player.sendMessage(
                            ChatColor.translateAlternateColorCodes(
                                '&',
                                AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_NAME)
                            )
                        )
                    }, 60L)
                }
            } else {
                event.disallow(
                    PlayerLoginEvent.Result.KICK_OTHER,
                    ChatColor.translateAlternateColorCodes(
                        '&',
                        AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_NAME)
                    )
                )
            }
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                Utils.logViolation(player.name + "(IP: " + playerIp + ")(Name)", playerName + censoredWordList)
            }
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                VelocitySender.sendNotifyMessage(player, ModuleType.NAME, playerName, censoredWordList)
            }
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                BungeeSender.sendNotifyMessage(player, ModuleType.NAME, playerName, censoredWordList)
            }
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                AdvancedSensitiveWords.databaseManager.checkAndUpdatePlayer(playerName)
            }
            val endTime = System.currentTimeMillis()
            TimingUtils.addProcessStatistic(endTime, startTime)
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(
                player,
                ModuleType.NAME,
                playerName,
                censoredWordList
            )
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.NAME_PUNISH)) Punishment.punish(player)
        }
    }
}
