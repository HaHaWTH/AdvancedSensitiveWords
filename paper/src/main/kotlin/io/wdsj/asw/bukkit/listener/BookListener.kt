package io.wdsj.asw.bukkit.listener

import io.wdsj.asw.bukkit.AdvancedSensitiveWords.sensitiveWordBs
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager
import io.wdsj.asw.bukkit.setting.PluginMessages
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.type.ModuleType
import io.wdsj.asw.bukkit.util.PlayerProcessingGuard
import io.wdsj.asw.bukkit.util.Utils
import io.wdsj.asw.bukkit.util.ViolationReporter
import io.wdsj.asw.bukkit.util.cache.BookCache
import io.wdsj.asw.bukkit.util.message.MessageUtils
import net.kyori.adventure.text.Component
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerEditBookEvent

class BookListener : Listener {
    private val processingGuard = PlayerProcessingGuard()
    private val violationReporter = ViolationReporter()

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBook(event: PlayerEditBookEvent) {
        if (!settingsManager.getProperty(PluginSettings.ENABLE_BOOK_EDIT_CHECK)) return

        val player = event.player
        if (processingGuard.shouldSkipBasic(player)) return

        val startTime = System.currentTimeMillis()
        val bookMeta = event.newBookMeta
        var violation = censorPages(event, bookMeta)

        if (violation == null) {
            violation = censorCrossPage(event)
        }
        censorAuthor(event, bookMeta)?.let { violation = it }
        censorTitle(event, bookMeta)?.let { violation = it }
        val finalViolation = violation ?: return

        event.newBookMeta = bookMeta

        if (settingsManager.getProperty(PluginSettings.BOOK_SEND_MESSAGE)) {
            MessageUtils.sendMessage(player, PluginMessages.MESSAGE_ON_BOOK)
        }

        violationReporter.report(
            player = player,
            moduleType = ModuleType.BOOK,
            content = finalViolation.content,
            censoredWords = finalViolation.censoredWords,
            logSource = "Book",
            startTime = startTime,
            punish = settingsManager.getProperty(PluginSettings.BOOK_PUNISH),
        )
    }

    private fun censorPages(event: PlayerEditBookEvent, bookMeta: BookMeta): BookViolation? {
        if (!bookMeta.hasPages()) return null

        var violation: BookViolation? = null
        for (pageIndex in 1..bookMeta.pageCount) {
            val page = bookMeta.page(pageIndex)
            val originalPage = preprocessPage(page)
            val censoredWords = findPageCensoredWords(originalPage)
            if (censoredWords.isEmpty()) continue

            violation = BookViolation(originalPage, censoredWords)
            if (isCancelMode()) {
                event.isCancelled = true
                break
            }

            bookMeta.page(pageIndex, replacePage(page, originalPage, censoredWords))
        }
        return violation
    }

    private fun censorCrossPage(event: PlayerEditBookEvent): BookViolation? {
        if (!settingsManager.getProperty(PluginSettings.BOOK_CROSS_PAGE)) return null

        val originalPageCrossed = (1..event.newBookMeta.pageCount)
            .joinToString("") { MessageUtils.plainText(event.newBookMeta.page(it)) }
            .let { preprocessPageText(it) }
        val censoredWords = sensitiveWordBs.findAll(originalPageCrossed)
        if (censoredWords.isEmpty()) return null

        event.isCancelled = true
        return BookViolation(originalPageCrossed, censoredWords)
    }

    private fun censorAuthor(event: PlayerEditBookEvent, bookMeta: BookMeta): BookViolation? {
        val author = event.newBookMeta.author() ?: return null
        val originalAuthor = preprocessText(MessageUtils.plainText(author))
        val censoredWords = sensitiveWordBs.findAll(originalAuthor)
        if (censoredWords.isEmpty()) return null

        if (isCancelMode()) {
            event.isCancelled = true
        } else {
            bookMeta.author(MessageUtils.replaceLiteral(author, originalAuthor, sensitiveWordBs.replace(originalAuthor)))
        }
        return BookViolation(originalAuthor, censoredWords)
    }

    private fun censorTitle(event: PlayerEditBookEvent, bookMeta: BookMeta): BookViolation? {
        val title = event.newBookMeta.title() ?: return null
        val originalTitle = preprocessText(MessageUtils.plainText(title))
        val censoredWords = sensitiveWordBs.findAll(originalTitle)
        if (censoredWords.isEmpty()) return null

        if (isCancelMode()) {
            event.isCancelled = true
        } else {
            bookMeta.title(MessageUtils.replaceLiteral(title, originalTitle, sensitiveWordBs.replace(originalTitle)))
        }
        return BookViolation(originalTitle, censoredWords)
    }

    private fun findPageCensoredWords(page: String): List<String> {
        if (!settingsManager.getProperty(PluginSettings.BOOK_CACHE)) {
            return sensitiveWordBs.findAll(page)
        }
        if (BookCache.isBookCached(page)) {
            return BookCache.getCachedBookSensitiveWordList(page)
        }
        return sensitiveWordBs.findAll(page)
    }

    private fun replacePage(page: Component, pagePlainText: String, censoredWords: List<String>): Component {
        if (!settingsManager.getProperty(PluginSettings.BOOK_CACHE)) {
            return MessageUtils.replaceLiteral(page, pagePlainText, sensitiveWordBs.replace(pagePlainText))
        }
        if (BookCache.isBookCached(pagePlainText)) {
            return MessageUtils.replaceLiteral(page, pagePlainText, BookCache.getCachedProcessedBookContent(pagePlainText))
        }

        val processedPage = sensitiveWordBs.replace(pagePlainText)
        BookCache.addToBookCache(pagePlainText, processedPage, censoredWords)
        return MessageUtils.replaceLiteral(page, pagePlainText, processedPage)
    }

    private fun preprocessPage(page: Component): String {
        return preprocessPageText(MessageUtils.plainText(page))
    }

    private fun preprocessPageText(text: String): String {
        val lineNormalized = if (settingsManager.getProperty(PluginSettings.BOOK_IGNORE_NEWLINE)) {
            text.replace("\n", "").replace("§0", "")
        } else {
            text
        }
        return preprocessText(lineNormalized)
    }

    private fun preprocessText(text: String): String {
        if (!settingsManager.getProperty(PluginSettings.PRE_PROCESS)) return text
        return text.replace(Utils.preProcessRegex.toRegex(), "")
    }

    private fun isCancelMode(): Boolean {
        return settingsManager.getProperty(PluginSettings.BOOK_METHOD).equals("cancel", ignoreCase = true)
    }

    private data class BookViolation(
        val content: String,
        val censoredWords: List<String>,
    )
}
