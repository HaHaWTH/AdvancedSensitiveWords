package io.wdsj.asw.util.cache;

import io.wdsj.asw.setting.PluginSettings;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import static io.wdsj.asw.AdvancedSensitiveWords.settingsManager;

/**
 * Cache utilities class for Book detections.
 * @author HaHaWTH & HeyWTF_IS_That and 0D00_0721
 */
public class BookCache {
    private static final LinkedHashMap<String, String> bookContentCache = new LinkedHashMap<>();
    private static final LinkedHashMap<String, List<String>> bookSensitiveWordCache = new LinkedHashMap<>();

    public static boolean isBookCached(String content) {
        return bookContentCache.containsKey(content) && bookSensitiveWordCache.containsKey(content);
    }
    public static void addToBookCache(String content, String processedContent, List<String> sensitiveWordList) {
        trimCache(bookContentCache);
        trimCache(bookSensitiveWordCache);
        bookContentCache.put(content, processedContent);
        bookSensitiveWordCache.put(content, sensitiveWordList);
    }

    public static String getCachedProcessedBookContent(String content) {
        return bookContentCache.get(content);
    }
    public static List<String> getCachedBookSensitiveWordList(String content) {
        return bookSensitiveWordCache.get(content);
    }
    public static void forceClearCache() {
        bookContentCache.clear();
        bookSensitiveWordCache.clear();
    }

    private static void trimCache(LinkedHashMap<String, ?> cache) {
        if (cache.size() > settingsManager.getProperty(PluginSettings.BOOK_MAXIMUM_CACHE_SIZE)) {
            Iterator<String> contentIterator = cache.keySet().iterator();
            for (int i = settingsManager.getProperty(PluginSettings.BOOK_MAXIMUM_CACHE_SIZE); i < cache.size(); i++) {
                if (contentIterator.hasNext()) {
                    contentIterator.next();
                    contentIterator.remove();
                }
            }
        }
    }


    private BookCache() {
    }
}
