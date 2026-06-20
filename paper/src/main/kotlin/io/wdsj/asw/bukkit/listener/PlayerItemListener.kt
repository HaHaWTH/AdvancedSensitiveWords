package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.sensitiveWordBs
import io.wdsj.asw.bukkit.setting.PaperConfigurationService
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.type.ModuleType
import io.wdsj.asw.bukkit.util.PlayerProcessingGuard
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.ViolationReporter
import io.wdsj.asw.bukkit.util.message.MessageUtils
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.inventory.ItemStack

class PlayerItemListener(private val configuration: PaperConfigurationService) : Listener {
    private val processingGuard = PlayerProcessingGuard(configuration)
    private val violationReporter = ViolationReporter(configuration)

    @EventHandler(priority = EventPriority.LOW)
    fun onPlayerHeldItem(event: PlayerItemHeldEvent) {
        if (!configuration.get(PluginSettings.ENABLE_PLAYER_ITEM_CHECK)) return

        val player = event.player
        if (processingGuard.shouldSkipBasic(player)) return

        val item = player.inventory.getItem(event.newSlot) ?: return
        censorItemName(player, item, event)
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onDrop(event: PlayerDropItemEvent) {
        if (!configuration.get(PluginSettings.ENABLE_PLAYER_ITEM_CHECK)) return

        val player = event.player
        if (processingGuard.shouldSkipBasic(player)) return

        censorItemName(player, event.itemDrop.itemStack, event)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onClick(event: InventoryClickEvent) {
        if (!configuration.get(PluginSettings.ENABLE_PLAYER_ITEM_CHECK)) return

        val player = event.whoClicked as? Player ?: return
        if (processingGuard.shouldSkipBasic(player)) return
        if (event.clickedInventory?.type != InventoryType.PLAYER) return

        val item = event.currentItem ?: return
        censorItemName(player, item, event)
    }

    private fun censorItemName(player: Player, item: ItemStack, event: Cancellable) {
        if (!item.hasItemMeta()) return

        val meta = item.itemMeta ?: return
        if (!meta.hasDisplayName()) return

        val originalNameComponent = meta.displayName() ?: return
        val originalName = preprocess(MessageUtils.plainText(originalNameComponent))
        val startTime = System.currentTimeMillis()
        val censoredWords = sensitiveWordBs.findAll(originalName)
        if (censoredWords.isEmpty()) return

        if (isCancelMode()) {
            event.isCancelled = true
        } else {
            meta.displayName(
                MessageUtils.replaceLiteral(
                    originalNameComponent,
                    originalName,
                    sensitiveWordBs.replace(originalName),
                ),
            )
            item.setItemMeta(meta)
        }

        if (configuration.get(PluginSettings.ITEM_SEND_MESSAGE)) {
            MessageUtils.sendMessage(player, PluginMessages.MESSAGE_ON_ITEM)
        }

        violationReporter.report(
            player = player,
            moduleType = ModuleType.ITEM,
            content = originalName,
            censoredWords = censoredWords,
            logSource = "Item",
            startTime = startTime,
            punishmentActions = configuration.get(PluginSettings.ITEM_PUNISHMENT),
        )
    }

    private fun preprocess(text: String): String {
        if (!configuration.get(PluginSettings.PRE_PROCESS)) return text
        return text.replace(Utils.preProcessRegex.toRegex(), "")
    }

    private fun isCancelMode(): Boolean {
        return configuration.get(PluginSettings.ITEM_METHOD).isCancel
    }
}
