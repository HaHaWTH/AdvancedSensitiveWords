package io.wdsj.asw.bukkit.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatEntropyTest {
    @Test
    void ignoresWhitespaceAndControlsForEntropy() {
        assertEquals(3, io.wdsj.asw.common.utils.ChatEntropy.visibleCodePointCount(" a\n b\t c "));
        assertEquals(0.0D, io.wdsj.asw.common.utils.ChatEntropy.shannonEntropyBits("aaaa"));
        assertEquals(2.0D, io.wdsj.asw.common.utils.ChatEntropy.shannonEntropyBits("abcd"), 0.000001D);
    }

    @Test
    void normalizesFullWidthCharactersBeforeCounting() {
        assertEquals(2, io.wdsj.asw.common.utils.ChatEntropy.visibleCodePointCount("ＡA"));
        assertEquals(0.0D, io.wdsj.asw.common.utils.ChatEntropy.shannonEntropyBits("ＡA"));
        assertTrue(io.wdsj.asw.common.utils.ChatEntropy.shannonEntropyBits("abca") > 1.0D);
    }
}
