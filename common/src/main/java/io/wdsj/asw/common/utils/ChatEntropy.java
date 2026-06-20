package io.wdsj.asw.common.utils;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

public final class ChatEntropy {
    private ChatEntropy() {
    }

    public static int visibleCodePointCount(String message) {
        return visibleCodePointCounts(message).values().stream().mapToInt(Integer::intValue).sum();
    }

    public static double shannonEntropyBits(String message) {
        Map<Integer, Integer> counts = visibleCodePointCounts(message);
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return 0.0D;
        }

        double entropy = 0.0D;
        for (int count : counts.values()) {
            double probability = (double) count / total;
            entropy -= probability * (Math.log(probability) / Math.log(2.0D));
        }
        return entropy;
    }

    private static Map<Integer, Integer> visibleCodePointCounts(String message) {
        Map<Integer, Integer> counts = new HashMap<>();
        if (message == null || message.isEmpty()) {
            return counts;
        }

        String normalized = Normalizer.normalize(message, Normalizer.Form.NFKC);
        normalized.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint) && !Character.isISOControl(codePoint))
                .forEach(codePoint -> counts.merge(codePoint, 1, Integer::sum));
        return counts;
    }
}
