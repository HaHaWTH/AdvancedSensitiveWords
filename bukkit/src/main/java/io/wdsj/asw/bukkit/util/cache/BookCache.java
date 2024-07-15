package io.wdsj.asw.bukkit.util.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.wdsj.asw.bukkit.setting.PluginSettings;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

/**
 * Cache utilities class for Book detections.
 * @author HaHaWTH & HeyWTF_IS_That and 0D00_0721
 */
public class BookCache {
    private static final Cache<String, BookCacheEntry> cache = CacheBuilder.newBuilder()
            .maximumSize(settingsManager.getProperty(PluginSettings.BOOK_MAXIMUM_CACHE_SIZE))
            .expireAfterWrite(settingsManager.getProperty(PluginSettings.BOOK_CACHE_EXPIRE_TIME), TimeUnit.MINUTES)
            .build();
    public static boolean isBookCached(String content) {
        return cache.getIfPresent(content) != null;
    }

    public static void addToBookCache(String content, String processedContent, List<String> sensitiveWordList) {
        cache.put(content, new BookCacheEntry(processedContent, sensitiveWordList));
    }

    /**
     * Retrieves the processed book content from the cache.
     * Warning: This method should only be called after checking if the book is cached.
     * @param content The content of the book.
     * @return The processed book content.
     */
    public static String getCachedProcessedBookContent(String content) {
        BookCacheEntry entry = Objects.requireNonNull(cache.getIfPresent(content));
        return entry.getProcessedContent();
    }

    /**
     * Retrieves the list of sensitive words from the cache.
     * Warning: This method should only be called after checking if the book is cached.
     * @param content The content of the book.
     * @return The list of sensitive words.
     */
    public static List<String> getCachedBookSensitiveWordList(String content) {
        BookCacheEntry entry = Objects.requireNonNull(cache.getIfPresent(content));
        return entry.getSensitiveWordList();
    }

    public static void invalidateAll() {
        cache.invalidateAll();
    }


    private BookCache() {
    }

    /**
     * Inner class to encapsulate the processed book content and its list of sensitive words.
     */
    private static class BookCacheEntry {
        private final String processedContent;
        private final List<String> sensitiveWordList;

        public BookCacheEntry(String processedContent, List<String> sensitiveWordList) {
            this.processedContent = processedContent;
            this.sensitiveWordList = sensitiveWordList;
        }

        public String getProcessedContent() {
            return processedContent;
        }

        public List<String> getSensitiveWordList() {
            return sensitiveWordList;
        }
    }
}