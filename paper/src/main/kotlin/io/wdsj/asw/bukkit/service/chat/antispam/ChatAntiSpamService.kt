package io.wdsj.asw.bukkit.service.chat.antispam

import io.wdsj.asw.bukkit.setting.PaperConfigurationService
import io.wdsj.asw.bukkit.setting.PluginSettings
import io.wdsj.asw.bukkit.util.list.EvictingRingList
import io.wdsj.asw.common.utils.MessageEntropy
import java.text.Normalizer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

class ChatAntiSpamService(private val configuration: PaperConfigurationService) {
    private val preprocessPattern: Pattern? = configuration.get(PluginSettings.CHAT_ANTI_SPAM_PREPROCESS_REGEX)
        .takeIf(String::isNotBlank)
        ?.let(Pattern::compile)

    fun check(playerId: UUID, message: String, nowMillis: Long = System.currentTimeMillis()): Result {
        if (!configuration.get(PluginSettings.CHAT_ANTI_SPAM_ENABLED)) return Result.CLEAN

        val preprocessed = preprocess(message)
        val visibleCodePoints = MessageEntropy.visibleCodePointCount(preprocessed)
        val normalized = normalize(preprocessed)
        if (visibleCodePoints >= configuration.get(PluginSettings.CHAT_ANTI_SPAM_MINIMUM_ENTROPY_CODE_POINTS)) {
            val entropy = MessageEntropy.shannonEntropyBits(normalized)
            val minimumEntropy = configuration.get(PluginSettings.CHAT_ANTI_SPAM_MINIMUM_ENTROPY_BITS)
            if (minimumEntropy >= 0.0 && entropy < minimumEntropy) {
                remember(playerId, normalized, nowMillis)
                return Result.SPAM
            }

            val minimumAverageEntropy = configuration.get(PluginSettings.CHAT_ANTI_SPAM_MINIMUM_AVERAGE_ENTROPY)
            if (minimumAverageEntropy >= 0.0 && averageEntropy(visibleCodePoints, entropy) < minimumAverageEntropy) {
                remember(playerId, normalized, nowMillis)
                return Result.SPAM
            }
        }

        val result = if (visibleCodePoints >= configuration.get(PluginSettings.CHAT_ANTI_SPAM_MINIMUM_SIMILARITY_CODE_POINTS)) {
            checkSimilarity(playerId, normalized, nowMillis)
        } else {
            Result.CLEAN
        }
        remember(playerId, normalized, nowMillis)
        return result
    }

    private fun checkSimilarity(playerId: UUID, normalized: String, nowMillis: Long): Result {
        val history = histories[playerId] ?: return Result.CLEAN
        val maxAgeMillis = configuration.get(PluginSettings.CHAT_ANTI_SPAM_HISTORY_MAX_AGE_SECONDS) * 1000L
        val compared = synchronized(history.list) {
            history.list.removeIf { nowMillis - it.createdAtMillis > maxAgeMillis }
            history.list.takeLast(configuration.get(PluginSettings.CHAT_ANTI_SPAM_SIMILAR_CHECK_AMOUNT)).toList()
        }
        if (compared.isEmpty()) return Result.CLEAN

        val maxSimilarity = configuration.get(PluginSettings.CHAT_ANTI_SPAM_SIMILAR_MAX_SIMILARITY)
        val minDistance = configuration.get(PluginSettings.CHAT_ANTI_SPAM_SIMILAR_MIN_DISTANCE)
        for (entry in compared) {
            val distance = editDistance(normalized, entry.message)
            val similarity = similarity(normalized, entry.message, distance)
            if (maxSimilarity <= 1.0 && similarity >= maxSimilarity) {
                return Result.SPAM
            }
            if (minDistance >= 0 && distance <= minDistance) {
                return Result.SPAM
            }
        }
        return Result.CLEAN
    }

    private fun remember(playerId: UUID, normalized: String, nowMillis: Long) {
        val maxSize = configuration.get(PluginSettings.CHAT_ANTI_SPAM_HISTORY_SIZE)
        val history = histories.compute(playerId) { _, existing ->
            existing?.takeIf { it.capacity == maxSize } ?: History(maxSize)
        } ?: return
        synchronized(history.list) {
            history.list.add(Entry(normalized, nowMillis))
        }
    }

    private fun preprocess(message: String): String {
        return preprocessPattern?.matcher(message)?.replaceAll("") ?: message
    }

    private data class Entry(val message: String, val createdAtMillis: Long)

    private data class History(
        val capacity: Int,
        val list: EvictingRingList<Entry> = EvictingRingList<Entry>(capacity),
    )

    enum class Result {
        CLEAN,
        SPAM,
    }

    companion object {
        private val histories = ConcurrentHashMap<UUID, History>()

        @JvmStatic
        fun clear(playerId: UUID) {
            histories.remove(playerId)
        }

        @JvmStatic
        fun clearAll() {
            histories.clear()
        }

        private fun normalize(message: String): String {
            val builder = StringBuilder()
            Normalizer.normalize(message, Normalizer.Form.NFKC)
                .codePoints()
                .filter { !Character.isISOControl(it) }
                .forEach(builder::appendCodePoint)
            return builder.toString().trim().uppercase()
        }

        private fun averageEntropy(visibleCodePoints: Int, entropy: Double): Double {
            if (visibleCodePoints <= 1) return 0.0
            return entropy / (ln(visibleCodePoints.toDouble()) / ln(2.0))
        }

        private fun similarity(left: String, right: String, distance: Int): Double {
            val maxLength = max(left.length, right.length)
            if (maxLength == 0) return 1.0
            return 1.0 - distance.toDouble() / maxLength
        }

        private fun editDistance(left: String, right: String): Int {
            if (left.isEmpty()) return right.length
            if (right.isEmpty()) return left.length

            var previous = IntArray(right.length + 1) { it }
            var current = IntArray(right.length + 1)
            for (i in 1..left.length) {
                current[0] = i
                for (j in 1..right.length) {
                    current[j] = if (left[i - 1] == right[j - 1]) {
                        previous[j - 1]
                    } else {
                        min(min(previous[j - 1], previous[j]), current[j - 1]) + 1
                    }
                }
                val swap = previous
                previous = current
                current = swap
            }
            return previous[right.length]
        }
    }
}
