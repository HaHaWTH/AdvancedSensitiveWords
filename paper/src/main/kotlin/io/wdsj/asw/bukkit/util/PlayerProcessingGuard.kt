package io.wdsj.asw.bukkit.util

import fr.xephi.authme.api.v3.AuthMeApi
import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.isAuthMeAvailable
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.isInitialized
import io.wdsj.asw.bukkit.playergroup.GroupModule
import io.wdsj.asw.bukkit.setting.PaperConfigurationService
import io.wdsj.asw.bukkit.permission.PermissionsEnum
import io.wdsj.asw.bukkit.permission.cache.CachingPermTool
import io.wdsj.asw.bukkit.setting.PluginSettings
import org.bukkit.entity.Player

class PlayerProcessingGuard(private val configuration: PaperConfigurationService) {
    fun shouldSkip(player: Player, bypassPermission: PermissionsEnum): Boolean {
        if (shouldSkipBasic(player, bypassPermission)) return true
        return isUnauthenticated(player)
    }

    fun shouldSkipBasic(player: Player, bypassPermission: PermissionsEnum): Boolean {
        if (!isInitialized) return true
        return CachingPermTool.hasPermission(PermissionsEnum.BYPASS, player) ||
            CachingPermTool.hasPermission(PermissionsEnum.BYPASS_ALL, player) ||
            CachingPermTool.hasPermission(bypassPermission, player)
    }

    fun shouldSkipGroupModule(player: Player, module: GroupModule, globalEnabled: Boolean): Boolean {
        val service = AdvancedSensitiveWords.getInstance().playerGroupService ?: return false
        return !service.isModuleEnabled(player, module, globalEnabled)
    }

    private fun isUnauthenticated(player: Player): Boolean {
        return isAuthMeAvailable &&
            configuration.get(PluginSettings.ENABLE_AUTHME_COMPATIBILITY) &&
            !AuthMeApi.getInstance().isAuthenticated(player)
    }
}
