package io.wdsj.asw.util.cache;

import java.util.LinkedHashMap;
import java.util.List;

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
        cache.clear();
    }


    private BookCache() {
    }
}
