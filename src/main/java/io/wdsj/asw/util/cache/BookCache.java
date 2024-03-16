package io.wdsj.asw.util.cache;

import io.wdsj.asw.setting.PluginSettings;

import java.util.LinkedHashMap;
import java.util.List;

import static io.wdsj.asw.AdvancedSensitiveWords.settingsManager;

/**
 * Cache utilities class for Book detections.
 * @author HaHaWTH & HeyWTF_IS_That and 0D00_0721
 */
public class BookCache {
    private static final LinkedHashMap<String, BookCacheEntry> bookCache = new LinkedHashMap<>();

    public static boolean isBookCached(String content) {
        return bookCache.containsKey(content);
    }

    public static void addToBookCache(String content, String processedContent, List<String> sensitiveWordList) {
        trimCache();
        bookCache.put(content, new BookCacheEntry(processedContent, sensitiveWordList));
    }

    public static String getCachedProcessedBookContent(String content) {
        BookCacheEntry entry = bookCache.get(content);
        return entry.getProcessedContent();
    }

    public static List<String> getCachedBookSensitiveWordList(String content) {
        BookCacheEntry entry = bookCache.get(content);
        return entry.getSensitiveWordList();
    }

    public static void forceClearCache() {
        bookCache.clear();
    }

    private static void trimCache() {
        if (bookCache.size() > settingsManager.getProperty(PluginSettings.BOOK_MAXIMUM_CACHE_SIZE)) {
            bookCache.clear();
        }
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