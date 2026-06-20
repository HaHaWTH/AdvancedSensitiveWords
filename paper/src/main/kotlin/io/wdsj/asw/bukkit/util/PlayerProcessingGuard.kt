package io.wdsj.asw.bukkit.util

import fr.xephi.authme.api.v3.AuthMeApi
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.isAuthMeAvailable
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.isInitialized
import io.wdsj.asw.bukkit.setting.PaperConfigurationService
import io.wdsj.asw.bukkit.permission.PermissionsEnum
import io.wdsj.asw.bukkit.permission.cache.CachingPermTool
import io.wdsj.asw.bukkit.setting.PluginSettings
import org.bukkit.entity.Player

class PlayerProcessingGuard(private val configuration: PaperConfigurationService) {
    fun shouldSkip(player: Player, commandMessage: String? = null): Boolean {
        if (shouldSkipBasic(player)) return true
        if (commandMessage != null && Utils.isCommandAndWhiteListed(commandMessage)) return true

        return isUnauthenticated(player)
    }

    fun shouldSkipBasic(player: Player): Boolean {
        if (!isInitialized) return true
        return CachingPermTool.hasPermission(PermissionsEnum.BYPASS, player)
    }

    private fun isUnauthenticated(player: Player): Boolean {
        return isAuthMeAvailable &&
            configuration.get(PluginSettings.ENABLE_AUTHME_COMPATIBILITY) &&
            !AuthMeApi.getInstance().isAuthenticated(player)
    }
}
