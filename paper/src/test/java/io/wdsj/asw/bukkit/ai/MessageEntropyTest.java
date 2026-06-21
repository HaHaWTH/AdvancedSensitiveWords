package io.wdsj.asw.bukkit.ai;

import io.wdsj.asw.common.utils.MessageEntropy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageEntropyTest {
    @Test
    void ignoresWhitespaceAndControlsForEntropy() {
        assertEquals(3, MessageEntropy.visibleCodePointCount(" a\n b\t c "));
        assertEquals(0.0D, MessageEntropy.shannonEntropyBits("aaaa"));
        assertEquals(2.0D, MessageEntropy.shannonEntropyBits("abcd"), 0.000001D);
    }

    @Test
    void normalizesFullWidthCharactersBeforeCounting() {
        assertEquals(2, MessageEntropy.visibleCodePointCount("ＡA"));
        assertEquals(0.0D, MessageEntropy.shannonEntropyBits("ＡA"));
        assertTrue(MessageEntropy.shannonEntropyBits("abca") > 1.0D);
    }
}
