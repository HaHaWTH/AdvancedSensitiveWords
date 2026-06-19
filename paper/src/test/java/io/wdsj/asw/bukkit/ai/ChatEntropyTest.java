package io.wdsj.asw.bukkit.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatEntropyTest {
    @Test
    void ignoresWhitespaceAndControlsForEntropy() {
        assertEquals(3, ChatEntropy.visibleCodePointCount(" a\n b\t c "));
        assertEquals(0.0D, ChatEntropy.shannonEntropyBits("aaaa"));
        assertEquals(2.0D, ChatEntropy.shannonEntropyBits("abcd"), 0.000001D);
    }

    @Test
    void normalizesFullWidthCharactersBeforeCounting() {
        assertEquals(2, ChatEntropy.visibleCodePointCount("ＡA"));
        assertEquals(0.0D, ChatEntropy.shannonEntropyBits("ＡA"));
        assertTrue(ChatEntropy.shannonEntropyBits("abca") > 1.0D);
    }
}
