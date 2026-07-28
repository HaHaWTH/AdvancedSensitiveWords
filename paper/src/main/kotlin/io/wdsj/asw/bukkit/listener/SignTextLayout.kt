package io.wdsj.asw.bukkit.listener

internal data class SignReplacementSpan(
    val start: Int,
    val end: Int,
    val replacement: String,
)

internal object SignTextLayout {
    fun replaceSegments(
        segmentLengths: List<Int>,
        content: String,
        spans: List<SignReplacementSpan>,
    ): List<String> {
        if (segmentLengths.isEmpty()) return emptyList()

        val lengths = segmentLengths.toIntArray()
        require(lengths.all { it > 0 }) { "Segment lengths must be positive" }
        require(lengths.sum() == content.length) { "Segment lengths must cover the content exactly" }

        val starts = segmentStarts(lengths)
        val replacements = Array(lengths.size) { StringBuilder() }
        var cursor = 0
        for (span in spans.sortedWith(compareBy<SignReplacementSpan> { it.start }.thenByDescending { it.end })) {
            val start = span.start.coerceIn(0, content.length)
            val end = span.end.coerceIn(start, content.length)
            if (start < cursor || start == end) continue

            appendUnchanged(replacements, lengths, starts, content, cursor, start)
            appendReplacement(replacements, lengths, starts, start, end, span.replacement)
            cursor = end
        }
        appendUnchanged(replacements, lengths, starts, content, cursor, content.length)
        return replacements.map(StringBuilder::toString)
    }

    private fun segmentStarts(lengths: IntArray): IntArray {
        val starts = IntArray(lengths.size)
        for (index in 1 until lengths.size) {
            starts[index] = starts[index - 1] + lengths[index - 1]
        }
        return starts
    }

    private fun appendUnchanged(
        replacements: Array<StringBuilder>,
        lengths: IntArray,
        starts: IntArray,
        content: String,
        start: Int,
        end: Int,
    ) {
        var cursor = start
        while (cursor < end) {
            val segmentIndex = segmentIndexAt(starts, cursor)
            val segmentEnd = starts[segmentIndex] + lengths[segmentIndex]
            val copyEnd = minOf(end, segmentEnd)
            replacements[segmentIndex].append(content, cursor, copyEnd)
            cursor = copyEnd
        }
    }

    private fun appendReplacement(
        replacements: Array<StringBuilder>,
        lengths: IntArray,
        starts: IntArray,
        start: Int,
        end: Int,
        replacement: String,
    ) {
        if (replacement.length != end - start) {
            replacements[segmentIndexAt(starts, start)].append(replacement)
            return
        }

        var contentOffset = start
        var replacementOffset = 0
        while (contentOffset < end) {
            val segmentIndex = segmentIndexAt(starts, contentOffset)
            val segmentEnd = starts[segmentIndex] + lengths[segmentIndex]
            val copyEnd = minOf(end, segmentEnd)
            val copyLength = copyEnd - contentOffset
            replacements[segmentIndex].append(
                replacement,
                replacementOffset,
                replacementOffset + copyLength,
            )
            contentOffset = copyEnd
            replacementOffset += copyLength
        }
    }

    private fun segmentIndexAt(starts: IntArray, index: Int): Int {
        for (segmentIndex in starts.indices.reversed()) {
            if (index >= starts[segmentIndex]) return segmentIndex
        }
        return 0
    }
}
