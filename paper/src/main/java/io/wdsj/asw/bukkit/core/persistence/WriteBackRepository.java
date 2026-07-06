package io.wdsj.asw.bukkit.core.persistence;

import java.util.Map;
import java.util.Optional;

/**
 * Storage adapter used by {@link WriteBackCache}.
 *
 * <p>The cache is the authoritative runtime view. Callers should read and mutate
 * data through {@code WriteBackCache}; the repository is only called by the cache to load a
 * missing entry or to flush dirty cached data back to persistent storage.</p>
 *
 * <p>Implementation contract:</p>
 * <ul>
 *     <li>{@link #load(Object)} must be side effect free. It should only read one key from the
 *     backing store and return {@link Optional#empty()} when the row/document does not exist.</li>
 *     <li>{@link #save(Object, Object)} must be an idempotent upsert. It may be called multiple
 *     times for the same value because periodic flush, invalidation flush and shutdown flush can
 *     overlap in timing. Use {@code INSERT ... ON CONFLICT/ON DUPLICATE KEY UPDATE} or equivalent.</li>
 *     <li>{@link #delete(Object)} should remove the row/document for the key. Override it when
 *     callers use {@link WriteBackCache#delete(Object)}; otherwise the default no-op is acceptable
 *     for caches that never delete entries.</li>
 *     <li>{@link #flush(Map)} receives a complete dirty snapshot and may persist it in one
 *     transaction or JDBC batch. The default implementation loops over {@link #save(Object, Object)}
 *     and {@link #delete(Object)} for compatibility.</li>
 * </ul>
 *
 * @param <K> stable cache key type, for example {@code UUID}, {@code String}, or {@code Long}
 * @param <V> immutable or defensively-copied value type stored in the cache
 */
public interface WriteBackRepository<K, V> {
    /**
     * Loads one entry from persistent storage.
     *
     * @param key key to load
     * @return value if the backing store contains it, otherwise {@link Optional#empty()}
     * @throws Exception when storage access fails
     */
    Optional<V> load(K key) throws Exception;

    /**
     * Persists one cache entry.
     *
     * <p>This must behave as an upsert and should write the complete value, not a partial diff.
     * The cache removes the key from the dirty set only after this method returns successfully.</p>
     *
     * @param key key to save
     * @param value complete value to persist
     * @throws Exception when storage access fails
     */
    void save(K key, V value) throws Exception;

    /**
     * Removes one cache entry from durable storage.
     *
     * <p>The default implementation is a no-op for holders that never delete data. Override this
     * when {@link WriteBackCache#delete(Object)} is part of the holder's public API.</p>
     *
     * @param key key to delete
     * @throws Exception when storage access fails
     */
    default void delete(K key) throws Exception {
    }

    /**
     * Persists a batch of complete cache snapshots.
     *
     * <p>Each present value means upsert; each empty value means delete. Implementations that need
     * maximum throughput should override this method and execute a single transaction with batch
     * statements. The method must remain idempotent: the same snapshot can be retried after a
     * previous failure.</p>
     *
     * <p>The cache clears each dirty marker only after this method returns successfully. If this
     * method throws, all involved dirty markers remain dirty and can be retried later.</p>
     *
     * @param entries dirty snapshot keyed by cache key
     * @throws Exception when storage access fails
     */
    default void flush(Map<K, Optional<V>> entries) throws Exception {
        for (Map.Entry<K, Optional<V>> entry : entries.entrySet()) {
            if (entry.getValue().isPresent()) {
                save(entry.getKey(), entry.getValue().get());
            } else {
                delete(entry.getKey());
            }
        }
    }
}
