package io.wdsj.asw.bukkit.listener.packet

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEditBook
import io.wdsj.asw.bukkit.AdvancedSensitiveWords
import io.wdsj.asw.bukkit.event.ASWFilterEvent
import io.wdsj.asw.bukkit.event.EventType
import io.wdsj.asw.bukkit.manage.notice.Notifier
import io.wdsj.asw.bukkit.manage.permission.Permissions
import io.wdsj.asw.bukkit.manage.punish.Punishment
import io.wdsj.asw.bukkit.proxy.bungee.BungeeSender
import io.wdsj.asw.bukkit.proxy.velocity.VelocitySender
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.cache.BookCache
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player

class ASWBookPacketListener : PacketListenerAbstract(PacketListenerPriority.LOW) {
    override fun onPacketReceive(event: PacketReceiveEvent) {
        if (!AdvancedSensitiveWords.isInitialized) return
        if (!AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_BOOK_EDIT_CHECK)) return
        val packetType = event.packetType
        val user = event.user
        val player = event.player as Player
        if (player.hasPermission(Permissions.BYPASS)) return
        val userName = user.name
        if (packetType === PacketType.Play.Client.EDIT_BOOK) {
            var processedOutMessage: String? = ""
            var outMessage = ""
            var outList: List<String?> = ArrayList()
            val skipReturnLine = AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.BOOK_IGNORE_NEWLINE)
            val isCacheEnabled = AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.BOOK_CACHE)
            var shouldSendMessage = false
            val isCancelMode = AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.BOOK_METHOD).equals("cancel", ignoreCase = true)
            val wrapper = WrapperPlayClientEditBook(event)

            // Book content check
            val originalPages = wrapper.pages
            val processedPages: MutableList<String> = ArrayList(originalPages.size)
            val startTime = System.currentTimeMillis()
            for (i in 0 until originalPages.size) {
                var originalPage = originalPages[i]
                if (skipReturnLine) {
                    originalPage = originalPage.replace("\n", "").replace("§0", "")
                }
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.PRE_PROCESS)) originalPage = originalPage.replace(Utils.getPreProcessRegex().toRegex(), "")
                val isBookCached = BookCache.isBookCached(originalPage)
                val censoredWordList = if (isBookCached && isCacheEnabled) BookCache.getCachedBookSensitiveWordList(originalPage) else AdvancedSensitiveWords.sensitiveWordBs.findAll(originalPage)
                if (censoredWordList.isNotEmpty()) {
                    val processedPage = if (isBookCached && isCacheEnabled) BookCache.getCachedProcessedBookContent(originalPage) else AdvancedSensitiveWords.sensitiveWordBs.replace(originalPage)
                    if (!isBookCached && isCacheEnabled) BookCache.addToBookCache(originalPage, processedPage, censoredWordList)
                    if (isCancelMode) {
                        event.isCancelled = true
                        shouldSendMessage = true
                        outMessage = originalPage
                        outList = censoredWordList
                        processedOutMessage = processedPage
                        break
                    }

                    shouldSendMessage = true
                    outMessage = originalPage
                    outList = censoredWordList
                    processedOutMessage = processedPage
                    processedPages.add(processedPage)
                } else {
                    processedPages.add(originalPage)
                }
            }
            if (!isCancelMode && shouldSendMessage) {
                wrapper.pages = processedPages
            }

            // Book title check
            var originalTitle = wrapper.title
            if (originalTitle != null) {
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.PRE_PROCESS)) originalTitle = originalTitle.replace(Utils.getPreProcessRegex().toRegex(), "")
                val censoredWordListTitle = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalTitle)
                if (censoredWordListTitle.isNotEmpty()) {
                    val processedTitle = AdvancedSensitiveWords.sensitiveWordBs.replace(originalTitle)
                    if (isCancelMode) {
                        event.isCancelled = true
                    } else {
                        wrapper.title = processedTitle
                    }
                    shouldSendMessage = true
                    outMessage = originalTitle
                    outList = censoredWordListTitle
                    processedOutMessage = processedTitle
                }
            }

            if (shouldSendMessage) {
                Utils.messagesFilteredNum.getAndIncrement()
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_API)) {
                    Bukkit.getPluginManager().callEvent(ASWFilterEvent(player, outMessage, processedOutMessage, outList, EventType.BOOK, true))
                }
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                    VelocitySender.send(player, EventType.BOOK, outMessage, outList)
                }
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                    BungeeSender.send(player, EventType.BOOK, outMessage, outList)
                }
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                    AdvancedSensitiveWords.databaseManager.checkAndUpdatePlayer(player.name)
                }
                val endTime = System.currentTimeMillis()
                TimingUtils.addProcessStatistic(endTime, startTime)
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) {
                    Notifier.notice(player, EventType.BOOK, outMessage, outList)
                }
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.BOOK_PUNISH)) Punishment.punish(player)
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.BOOK_SEND_MESSAGE)) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_BOOK)))
                }
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.LOG_VIOLATION)) {
                    Utils.logViolation(userName + "(IP: " + user.address.address.hostAddress + ")(Book)", outMessage + outList)
                }
            }
        }
    }
}
