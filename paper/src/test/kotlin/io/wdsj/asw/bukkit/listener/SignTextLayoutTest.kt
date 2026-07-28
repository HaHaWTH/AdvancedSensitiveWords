package io.wdsj.asw.bukkit.listener

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SignTextLayoutTest {
    @Test
    fun `equal-length replacement stays on the original lines`() {
        val result = SignTextLayout.replaceSegments(
            segmentLengths = listOf(3, 1),
            content = "你个傻b",
            spans = listOf(SignReplacementSpan(2, 4, "**")),
        )

        assertEquals(listOf("你个*", "*"), result)
    }

    @Test
    fun `long custom replacement stays on the line where the match starts`() {
        val result = SignTextLayout.replaceSegments(
            segmentLengths = listOf(4, 4),
            content = "xxfuckyy",
            spans = listOf(SignReplacementSpan(2, 6, "iloveyou")),
        )

        assertEquals(listOf("xxiloveyou", "yy"), result)
    }

    @Test
    fun `short custom replacement does not move unaffected text across lines`() {
        val result = SignTextLayout.replaceSegments(
            segmentLengths = listOf(4, 4),
            content = "xxfuckyy",
            spans = listOf(SignReplacementSpan(2, 6, "*")),
        )

        assertEquals(listOf("xx*", "yy"), result)
    }

    @Test
    fun `overlapping matches prefer the longest match at the same start`() {
        val result = SignTextLayout.replaceSegments(
            segmentLengths = listOf(3, 3),
            content = "abcdef",
            spans = listOf(
                SignReplacementSpan(1, 3, "**"),
                SignReplacementSpan(1, 5, "####"),
                SignReplacementSpan(2, 6, "xxxx"),
            ),
        )

        assertEquals(listOf("a##", "##f"), result)
    }
}
