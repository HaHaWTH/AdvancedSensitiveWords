package io.wdsj.asw.bukkit.util.context

import io.wdsj.asw.bukkit.util.list.EvictingRingList
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class ContextHistory<T> {
    private val histories = ConcurrentHashMap<UUID, Buffer<T>>()

    fun add(playerId: UUID, capacity: Int, entry: T) {
        val buffer = bufferFor(playerId, capacity)
        synchronized(buffer.entries) {
            buffer.entries.add(entry)
        }
    }

    fun snapshot(playerId: UUID, capacity: Int, maxAgeMillis: Long, timestamp: (T) -> Long): List<T> {
        val buffer = bufferFor(playerId, capacity)
        synchronized(buffer.entries) {
            val now = System.currentTimeMillis()
            buffer.entries.removeIf { now - timestamp(it) > maxAgeMillis }
            return buffer.entries.toList()
        }
    }

    fun removeLast(playerId: UUID) {
        val buffer = histories[playerId] ?: return
        synchronized(buffer.entries) {
            if (buffer.entries.isNotEmpty()) {
                buffer.entries.removeAt(buffer.entries.lastIndex)
            }
        }
    }

    fun removeMatching(playerId: UUID, predicate: (T) -> Boolean) {
        val buffer = histories[playerId] ?: return
        synchronized(buffer.entries) {
            buffer.entries.removeIf(predicate)
        }
    }

    fun clear(playerId: UUID) {
        histories.remove(playerId)
    }

    fun clearAll() {
        histories.clear()
    }

    private fun bufferFor(playerId: UUID, requestedCapacity: Int): Buffer<T> {
        val capacity = requestedCapacity.coerceAtLeast(1)
        return histories.compute(playerId) { _, existing ->
            if (existing == null || existing.capacity == capacity) {
                return@compute existing ?: Buffer(capacity, EvictingRingList(capacity))
            }

            val replacement = EvictingRingList<T>(capacity)
            synchronized(existing.entries) {
                val firstIndex = (existing.entries.size - capacity).coerceAtLeast(0)
                for (index in firstIndex until existing.entries.size) {
                    replacement.add(existing.entries[index])
                }
            }
            Buffer(capacity, replacement)
        }!!
    }

    private data class Buffer<T>(
        val capacity: Int,
        val entries: EvictingRingList<T>,
    )
}
