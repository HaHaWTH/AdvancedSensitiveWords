package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.permission.PermissionsEnum
import io.wdsj.asw.bukkit.permission.cache.CachingPermTool
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.setting.PaperConfigurationService
import io.wdsj.asw.bukkit.util.PlayerUtils
import io.wdsj.asw.bukkit.util.message.MessageUtils
import io.wdsj.asw.common.environment.PluginBuildInfo
import io.wdsj.asw.common.update.Updater
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class JoinUpdateNotifier(private val configuration: PaperConfigurationService) : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (!configuration.get(PluginSettings.CHECK_FOR_UPDATE)
            || !CachingPermTool.hasPermission(PermissionsEnum.UPDATE, player)
            || PlayerUtils.isNpc(player)) return

        val result = AdvancedSensitiveWords.getInstance().updateResult
        if (result.isUpdateAvailable) {
            val latestVersion = if (Updater.isDevChannel()) {
                if (result.isReleaseUpdateAvailable) {
                    if (result.isError) {
                        "release ${result.latestReleaseVersion}; dev comparison unavailable"
                    } else {
                        "release ${result.latestReleaseVersion}; dev ${result.latestVersion} (${result.commitsBehind} commits behind)"
                    }
                } else {
                    "${result.latestVersion} (${result.commitsBehind} commits behind)"
                }
            } else {
                result.latestVersion
            }
            MessageUtils.sendMessage(player,
                configuration.message(PluginMessages.UPDATE_AVAILABLE)
                    .replace("%current_version%", if (Updater.isDevChannel()) PluginBuildInfo.COMMIT_HASH_SHORT else AdvancedSensitiveWords.PLUGIN_VERSION)
                    .replace("%latest_version%", latestVersion)
            )
        }
    }
}
