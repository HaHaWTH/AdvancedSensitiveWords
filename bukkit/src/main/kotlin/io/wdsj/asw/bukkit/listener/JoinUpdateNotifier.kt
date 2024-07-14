package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.messagesManager
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.manage.permission.Permissions
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.update.Updater
import io.wdsj.asw.bukkit.util.PlayerUtils
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class JoinUpdateNotifier : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (!settingsManager.getProperty(PluginSettings.CHECK_FOR_UPDATE)
            || !player.hasPermission(Permissions.UPDATE)
            || PlayerUtils.isNpc(player)) return

        if (Updater.hasUpdate()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                messagesManager.getProperty(PluginMessages.UPDATE_AVAILABLE)
                    .replace("%current_version%", Updater.getCurrentVersion())
                    .replace("%latest_version%", Updater.getLatestVersion())
            ))
        }
    }
}