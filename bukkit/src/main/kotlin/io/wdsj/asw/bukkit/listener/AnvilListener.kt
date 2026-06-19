package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.sensitiveWordBs
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.type.ModuleType
import io.wdsj.asw.bukkit.util.PlayerProcessingGuard
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.ViolationReporter
import io.wdsj.asw.bukkit.util.message.MessageUtils
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType

class AnvilListener : Listener {
    private val processingGuard = PlayerProcessingGuard()
    private val violationReporter = ViolationReporter()

    @EventHandler(priority = EventPriority.LOWEST)
    fun onAnvil(event: InventoryClickEvent) {
        if (!settingsManager.getProperty(PluginSettings.ENABLE_ANVIL_EDIT_CHECK)) return
        if (event.inventory.type != InventoryType.ANVIL) return
        if (event.rawSlot != 2) return

        val player = event.whoClicked as? Player ?: return
        if (processingGuard.shouldSkipBasic(player)) return

        val outputItem = event.currentItem ?: return
        if (!outputItem.hasItemMeta()) return

        val itemMeta = outputItem.itemMeta ?: return
        if (!itemMeta.hasDisplayName()) return

        val originalNameComponent = itemMeta.displayName() ?: return
        val originalItemName = preprocess(MessageUtils.plainText(originalNameComponent))
        val startTime = System.currentTimeMillis()
        val censoredWords = sensitiveWordBs.findAll(originalItemName)
        if (censoredWords.isEmpty()) return

        if (isCancelMode()) {
            event.isCancelled = true
        } else {
            itemMeta.displayName(
                MessageUtils.replaceLiteral(
                    originalNameComponent,
                    originalItemName,
                    sensitiveWordBs.replace(originalItemName),
                ),
            )
            outputItem.setItemMeta(itemMeta)
        }

        if (settingsManager.getProperty(PluginSettings.ANVIL_SEND_MESSAGE)) {
            MessageUtils.sendMessage(player, PluginMessages.MESSAGE_ON_ANVIL_RENAME)
        }

        violationReporter.report(
            player = player,
            moduleType = ModuleType.ANVIL,
            content = originalItemName,
            censoredWords = censoredWords,
            logSource = "Anvil",
            startTime = startTime,
            punish = settingsManager.getProperty(PluginSettings.ANVIL_PUNISH),
        )
    }

    private fun preprocess(text: String): String {
        if (!settingsManager.getProperty(PluginSettings.PRE_PROCESS)) return text
        return text.replace(Utils.preProcessRegex.toRegex(), "")
    }

    private fun isCancelMode(): Boolean {
        return settingsManager.getProperty(PluginSettings.ANVIL_METHOD).equals("cancel", ignoreCase = true)
    }
}
