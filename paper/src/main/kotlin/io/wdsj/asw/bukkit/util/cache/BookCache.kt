package io.wdsj.asw.bukkit.util.cache

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.wdsj.asw.bukkit.AdvancedSensitiveWords.setting
import io.wdsj.asw.bukkit.setting.PluginSettings
import java.util.concurrent.TimeUnit

object BookCache {
    private lateinit var cache: Cache<String, BookCacheEntry>
    fun isBookCached(content: String): Boolean {
        return cache.getIfPresent(content) != null
    }

    fun addToBookCache(content: String, processedContent: String, sensitiveWordList: List<String>) {
        cache.put(content, BookCacheEntry(processedContent, sensitiveWordList))
    }

    /**
     * Retrieves the processed book content from the cache.
     * Warning: This method is only safely called after checking if the book is cached.
     * @param content The content of the book.
     * @return The processed book content.
     */
    @Throws(NoSuchElementException::class)
    fun getCachedProcessedBookContent(content: String): String {
        val entry = cache.getIfPresent(content) ?: throw NoSuchElementException("Book not found in cache")
        return entry.processedContent
    }

    /**
     * Retrieves the list of sensitive words from the cache.
     * Warning: This method is only safely called after checking if the book is cached.
     * @param content The content of the book.
     * @return The list of sensitive words.
     */
    @Throws(NoSuchElementException::class)
    fun getCachedBookSensitiveWordList(content: String): List<String> {
        val entry = cache.getIfPresent(content) ?: throw NoSuchElementException("Book not found in cache")
        return entry.sensitiveWordList
    }

    @JvmStatic
    fun invalidateAll() {
        cache.invalidateAll()
    }

    @JvmStatic
    fun initialize() {
        cache = CacheBuilder.newBuilder()
            .maximumSize(
                setting(PluginSettings.BOOK_MAXIMUM_CACHE_SIZE).toLong()
            )
            .expireAfterWrite(
                setting(PluginSettings.BOOK_CACHE_EXPIRE_TIME).toLong(),
                TimeUnit.MINUTES
            )
            .build()
    }


    /**
     * Inner class to encapsulate the processed book content and its list of sensitive words.
     */
    private data class BookCacheEntry(val processedContent: String, val sensitiveWordList: List<String>)
}
