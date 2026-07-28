package io.wdsj.asw.bukkit.core.network;

import com.github.houbb.sensitive.word.api.IWordResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObfuscatedUrlDetectorTest {
    @Test
    void normalizesInsertedNoiseAndPreservesTheOriginalRange() {
        String source = "b哈a哈i度d～u．c哈o哈m";

        ObfuscatedUrlDetector.NormalizedText normalized = ObfuscatedUrlDetector.normalize(source);

        assertEquals("baidu.com", normalized.text());
        assertEquals(0, normalized.sourceStarts()[0]);
        assertEquals(source.length(), normalized.sourceEnds()[normalized.text().length() - 1]);
    }

    @Test
    void normalizesChineseDotPhrasesAndEmojiConfusables() {
        assertEquals(
                "example.com",
                ObfuscatedUrlDetector.normalize("example小數點com").text()
        );
        assertEquals(
                "baidu.com",
                ObfuscatedUrlDetector.normalize("🅱aidu🔴com").text()
        );
    }

    @Test
    void removesObfuscatingColonsButPreservesSchemesAndPorts() {
        assertEquals(
                "discord.gg",
                ObfuscatedUrlDetector.normalize("d:i:s:c:o:r:d.gg").text()
        );
        assertEquals(
                "https://example.com:25565",
                ObfuscatedUrlDetector.normalize("https://example.com:25565").text()
        );
    }

    @Test
    void returnsSourceCoordinatesForAnObfuscatedUrl() {
        String source = "b哈a哈i度d～u．c哈o哈m";
        try (ObfuscatedUrlDetector detector = new ObfuscatedUrlDetector(true)) {
            List<IWordResult> results = detector.findAll(source);

            assertFalse(results.isEmpty());
            IWordResult result = results.getFirst();
            assertEquals(0, result.startIndex());
            assertEquals(source.length(), result.endIndex());
            assertEquals(source, result.word());
        }
    }

    @Test
    void doesNotTreatOrdinaryTextAsAUrl() {
        try (ObfuscatedUrlDetector detector = new ObfuscatedUrlDetector(true)) {
            assertTrue(detector.findAll("傻.子").isEmpty());
            assertTrue(detector.findAll("ordinary minecraft chat").isEmpty());
        }
    }
}
