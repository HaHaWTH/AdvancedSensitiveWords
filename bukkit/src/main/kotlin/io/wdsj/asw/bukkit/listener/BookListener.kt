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
import io.wdsj.asw.bukkit.util.LoggingUtils
import io.wdsj.asw.bukkit.util.TimingUtils
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.cache.BookCache
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerEditBookEvent

class BookListener : Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onBook(event: PlayerEditBookEvent) {
        if (!AdvancedSensitiveWords.isInitialized) return
        if (!AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_BOOK_EDIT_CHECK)) return
        val player = event.player
        if (player.hasPermission(Permissions.BYPASS)) return
        val isCacheEnabled = AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.BOOK_CACHE)
        val skipReturnLine = AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.BOOK_IGNORE_NEWLINE)
        val isCancelMode = AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.BOOK_METHOD).equals("cancel", ignoreCase = true)
        var outMessage = ""
        var outList: List<String?> = ArrayList()
        val originalPages = event.newBookMeta.pages
        var shouldSendMessage = false
        val bookMeta = event.newBookMeta
        var pageIndex = 1
        val startTime = System.currentTimeMillis()
        if (bookMeta.hasPages()) {
            for (i in 0 until originalPages.size) {
                var originalPage = originalPages[i]
                if (skipReturnLine) originalPage = originalPage.replace("\n", "").replace("§0", "")
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.PRE_PROCESS)) originalPage =
                    originalPage.replace(
                        Utils.getPreProcessRegex().toRegex(), ""
                    )
                val isBookCached = BookCache.isBookCached(originalPage)
                val censoredWordList =
                    if (isBookCached && isCacheEnabled) BookCache.getCachedBookSensitiveWordList(originalPage) else AdvancedSensitiveWords.sensitiveWordBs.findAll(
                        originalPage
                    )
                if (censoredWordList.isNotEmpty()) {
                    val processedPage =
                        if (isBookCached && isCacheEnabled) BookCache.getCachedProcessedBookContent(originalPage) else AdvancedSensitiveWords.sensitiveWordBs.replace(
                            originalPage
                        )
                    if (!isBookCached && isCacheEnabled) BookCache.addToBookCache(
                        originalPage,
                        processedPage,
                        censoredWordList
                    )
                    if (isCancelMode) {
                        event.isCancelled = true
                        shouldSendMessage = true
                        outMessage = originalPage
                        outList = censoredWordList
                        break
                    }

                    bookMeta.setPage(pageIndex++, processedPage)
                    shouldSendMessage = true
                    outMessage = originalPage
                    outList = censoredWordList
                }
            }

            // Cross page check
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.BOOK_CROSS_PAGE) && !shouldSendMessage) {
                var originalPageCrossed = originalPages.joinToString("").replace("\n", "").replace("§0", "")
                if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.PRE_PROCESS)) {
                    originalPageCrossed =
                        originalPageCrossed.replace(
                            Utils.getPreProcessRegex().toRegex(), ""
                        )
                }
                val censoredWordListCrossed = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalPageCrossed)
                if (censoredWordListCrossed.isNotEmpty()) {
                    event.isCancelled = true
                    outList = censoredWordListCrossed
                    outMessage = originalPageCrossed
                    shouldSendMessage = true
                }
            }
        }

        // Author check
        var originalAuthor = event.newBookMeta.author
        if (originalAuthor != null) {
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.PRE_PROCESS)) originalAuthor =
                originalAuthor.replace(
                    Utils.getPreProcessRegex().toRegex(), ""
                )
            val censoredWordListAuthor = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalAuthor)
            if (censoredWordListAuthor.isNotEmpty()) {
                val processedAuthor = AdvancedSensitiveWords.sensitiveWordBs.replace(originalAuthor)
                if (isCancelMode) {
                    event.isCancelled = true
                } else {
                    bookMeta.author = processedAuthor
                }
                shouldSendMessage = true
                outMessage = originalAuthor
                outList = censoredWordListAuthor
            }
        }


        var originalTitle = event.newBookMeta.title
        if (originalTitle != null) {
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.PRE_PROCESS)) originalTitle =
                originalTitle.replace(
                    Utils.getPreProcessRegex().toRegex(), ""
                )
            val censoredWordListTitle = AdvancedSensitiveWords.sensitiveWordBs.findAll(originalTitle)
            if (censoredWordListTitle.isNotEmpty()) {
                val processedTitle = AdvancedSensitiveWords.sensitiveWordBs.replace(originalTitle)
                if (isCancelMode) {
                    event.isCancelled = true
                } else {
                    bookMeta.setTitle(processedTitle)
                }
                shouldSendMessage = true
                outMessage = originalTitle
                outList = censoredWordListTitle
            }
        }

        if (shouldSendMessage) {
            event.newBookMeta = bookMeta
            Utils.messagesFilteredNum.getAndIncrement()
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_VELOCITY)) {
                VelocitySender.sendNotifyMessage(player, ModuleType.BOOK, outMessage, outList)
            }
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.HOOK_BUNGEECORD)) {
                BungeeSender.sendNotifyMessage(player, ModuleType.BOOK, outMessage, outList)
            }
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.ENABLE_DATABASE)) {
                AdvancedSensitiveWords.databaseManager.checkAndUpdatePlayer(player.name)
            }
            val endTime = System.currentTimeMillis()
            TimingUtils.addProcessStatistic(endTime, startTime)
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.NOTICE_OPERATOR)) Notifier.notice(
                player,
                ModuleType.BOOK,
                outMessage,
                outList
            )
            if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.BOOK_PUNISH)) Punishment.punish(player)
        }

        if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.BOOK_SEND_MESSAGE) && shouldSendMessage) {
            player.sendMessage(
                ChatColor.translateAlternateColorCodes(
                    '&',
                    AdvancedSensitiveWords.messagesManager.getProperty(PluginMessages.MESSAGE_ON_BOOK)
                )
            )
        }

        if (AdvancedSensitiveWords.settingsManager.getProperty(PluginSettings.LOG_VIOLATION) && shouldSendMessage) {
            LoggingUtils.logViolation(player.name + "(IP: " + Utils.getPlayerIp(player) + ")(Book)", outMessage + outList)
        }
    }
}