package io.wdsj.asw.bukkit.core.persistence;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import org.jetbrains.annotations.Blocking;
import org.slf4j.Logger;

/**
 * Caffeine based write-back/write-behind cache.
 *
 * <p>Runtime code should treat this cache as the source of truth. Calls to {@link #put(Object, Object)}
 * and {@link #delete(Object)} update memory first and mark the key dirty. Dirty keys are later
 * persisted by the periodic flush task, explicit flush calls, invalidation, or {@link #close()}.</p>
 *
 * <p>All reads and writes for the same data domain must go through this cache after it is created.
 * Writing directly to the database bypasses the in-memory source of truth and can be overwritten by
 * a later dirty cache flush. Reading directly from the database can also observe stale data because
 * write-back entries may not have been flushed yet.</p>
 *
 * <p>Concurrent callers are supported. Dirty entries carry a monotonic version so an older flush
 * cannot clear a newer write for the same key.</p>
 *
 * <p>Use {@link FlushPolicy#PERIODIC} for frequently changing data. Use
 * {@link FlushPolicy#ASYNC_AFTER_WRITE}, {@link #putAndFlushAsync(Object, Object)}, or
 * {@link #deleteAndFlushAsync(Object)} for writes that should be queued for persistence immediately
 * without blocking the caller.</p>
 */
@SuppressWarnings({"OptionalAssignedToNull", "OptionalUsedAsFieldOrParameterType", "unused"})
public final class WriteBackCache<K, V> implements AutoCloseable {
    private final String name;
    private final WriteBackRepository<K, V> repository;
    private final Logger logger;
    private final Duration shutdownTimeout;
    private final FlushPolicy flushPolicy;
    private final ExecutorService readExecutor;
    private final ScheduledExecutorService writeExecutor;
    private final ConcurrentMap<K, Long> dirtyVersions = new ConcurrentHashMap<>();
    private final AtomicLong versionSequence = new AtomicLong();
    private final AsyncLoadingCache<K, Optional<V>> cache;
    private final LoadingCache<K, Optional<V>> syncCache;
    private final AtomicBoolean closed = new AtomicBoolean();

    public WriteBackCache(
            String name,
            WriteBackRepository<K, V> repository,
            Logger logger,
            Caffeine<Object, Object> cacheBuilder,
            Duration flushInterval,
            Duration shutdownTimeout,
            FlushPolicy flushPolicy
    ) {
        this.name = name;
        this.repository = repository;
        this.logger = logger;
        this.shutdownTimeout = shutdownTimeout;
        this.flushPolicy = flushPolicy;
        this.readExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.writeExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, name + "-writeback");
            thread.setDaemon(true);
            return thread;
        });

        RemovalListener<K, Optional<V>> removalListener = (key, value, cause) -> {
            Long version = key == null ? null : dirtyVersions.get(key);
            if (key != null && value != null && version != null && cause != RemovalCause.REPLACED) {
                flushSnapshotAsync(key, value, version);
            }
        };
        this.cache = buildCache(cacheBuilder, removalListener);
        this.syncCache = cache.synchronous();

        long flushMillis = Math.max(1000L, flushInterval.toMillis());
        writeExecutor.scheduleAtFixedRate(this::flushDirtySafely, flushMillis, flushMillis, TimeUnit.MILLISECONDS);
    }

    public static <K, V> WriteBackCache<K, V> create(
            String name,
            WriteBackRepository<K, V> repository,
            Logger logger,
            Duration flushInterval
    ) {
        return new WriteBackCache<>(
                name,
                repository,
                logger,
                Caffeine.newBuilder().expireAfterAccess(30L, TimeUnit.MINUTES),
                flushInterval,
                Duration.ofSeconds(10L),
                FlushPolicy.PERIODIC
        );
    }

    /**
     * Asynchronously reads an entry through the Caffeine cache loader.
     *
     * <p>The returned callbacks run on the thread that completes the future unless the caller uses
     * an async continuation with an explicit executor. Do not access Bukkit world/entity APIs from
     * those callbacks without switching back to the correct server thread.</p>
     */
    public CompletableFuture<Optional<V>> getAsync(K key) {
        return cache.get(key).handle((value, exception) -> {
            if (exception != null) {
                logger.error("{} failed to load cache entry {}.", name, key, exception);
                throw new CompletionException(name + " failed to load cache entry " + key, exception);
            }
            return value;
        });
    }

    /**
     * Asynchronously reads several entries through the cache.
     *
     * <p>This is for key-based batch reads. Each returned map entry contains the loaded value or
     * {@link Optional#empty()} when that key does not exist in durable storage. The map preserves
     * the iteration order of {@code keys}.</p>
     *
     * <p>Like {@link #getAsync(Object)}, continuations run on the completion thread unless the
     * caller uses an explicit executor.</p>
     */
    public CompletableFuture<Map<K, Optional<V>>> getAllAsync(Collection<K> keys) {
        return cache.getAll(keys).handle((values, exception) -> {
            if (exception != null) {
                logger.error("{} failed to load cache entries {}.", name, keys, exception);
                throw new CompletionException(name + " failed to load cache entries", exception);
            }
            Map<K, Optional<V>> result = new Object2ObjectLinkedOpenHashMap<>(keys.size());
            for (K key : keys) {
                result.put(key, values.getOrDefault(key, Optional.empty()));
            }
            return result;
        });
    }

    /**
     * Runs an arbitrary asynchronous read query.
     *
     * <p>Use this for reads that are not naturally addressed by cache key, such as "latest rows",
     * "created between two timestamps", ranking queries, or moderation review lists.</p>
     *
     * <p>This method does not flush dirty cache entries first. It is suitable for eventually
     * consistent dashboards or queries that tolerate write-behind delay. If the query must observe
     * pending cached writes, call {@link #flushDirtyAsync()} first and then compose into this
     * method.</p>
     */
    public <R> CompletableFuture<R> queryAsync(Callable<R> query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return query.call();
            } catch (Exception exception) {
                throw new IllegalStateException(name + " query failed", exception);
            }
        }, readExecutor);
    }

    public Optional<V> getIfCached(K key) {
        Optional<V> value = syncCache.getIfPresent(key);
        return value == null ? Optional.empty() : value;
    }

    /**
     * Returns a point-in-time snapshot of entries currently resident in memory.
     *
     * <p>This method does not load missing keys from durable storage. It is intended for cache
     * maintenance tasks such as pruning expired elements inside cached aggregate values. Mutating
     * the returned map has no effect; use {@link #put(Object, Object)} or {@link #delete(Object)}
     * for changes so dirty tracking remains correct.</p>
     */
    public Map<K, Optional<V>> cachedEntriesSnapshot() {
        return new LinkedHashMap<>(syncCache.asMap());
    }

    public void put(K key, V value) {
        ensureOpen();
        syncCache.put(key, Optional.of(value));
        markDirty(key);
        if (flushPolicy == FlushPolicy.ASYNC_AFTER_WRITE) {
            flushKeyAsync(key);
        }
    }

    /**
     * Updates the cache and immediately queues an asynchronous flush for this key.
     *
     * <p>This is useful for high-value writes such as economy transactions, bans, or completed
     * moderation actions while keeping the default cache policy periodic.</p>
     */
    public CompletableFuture<Void> putAndFlushAsync(K key, V value) {
        put(key, value);
        return flushKeyFuture(key);
    }

    /**
     * Loads the current value asynchronously, computes the replacement in memory, and marks the key
     * dirty. With {@link FlushPolicy#ASYNC_AFTER_WRITE}, the changed key is also queued for flush.
     */
    public CompletableFuture<Optional<V>> computeAsync(K key, Function<Optional<V>, Optional<V>> remapper) {
        ensureOpen();
        return getAsync(key).thenApply(current -> {
            Optional<V> next = remapper.apply(current);
            if (next.isPresent()) {
                put(key, next.get());
            } else {
                delete(key);
            }
            return next;
        });
    }

    public void delete(K key) {
        ensureOpen();
        syncCache.put(key, Optional.empty());
        markDirty(key);
        if (flushPolicy == FlushPolicy.ASYNC_AFTER_WRITE) {
            flushKeyAsync(key);
        }
    }

    /**
     * Marks an entry deleted in memory and immediately queues an asynchronous delete flush.
     */
    public CompletableFuture<Void> deleteAndFlushAsync(K key) {
        delete(key);
        return flushKeyFuture(key);
    }

    /**
     * Invalidates an entry without blocking the caller on database I/O.
     *
     * <p>If the entry is dirty, Caffeine's removal listener queues an asynchronous flush on the
     * write-behind executor. This is the method to use from latency-sensitive paths such as player
     * quit handlers. Use {@link #invalidateAndFlushBlocking(Object)} only when the caller can safely
     * block.</p>
     */
    public void invalidate(K key) {
        syncCache.invalidate(key);
        syncCache.cleanUp();
    }

    /**
     * Queues a flush for the key and invalidates the entry after that flush task has run.
     *
     * <p>The returned future completes on the write-behind executor. Use an explicit scheduler or
     * executor before touching server APIs from continuations.</p>
     */
    public CompletableFuture<Void> invalidateAndFlushAsync(K key) {
        ensureOpen();
        CompletableFuture<Void> future = new CompletableFuture<>();
        writeExecutor.execute(() -> {
            try {
                flushKeyBlocking(key);
                syncCache.invalidate(key);
                syncCache.cleanUp();
                future.complete(null);
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    /**
     * Flushes a dirty entry synchronously before invalidating it.
     *
     * <p>This may perform blocking database I/O on the caller thread. It is suitable for controlled
     * shutdown or maintenance code, not for the Minecraft main thread or Folia region threads.</p>
     */
    public void invalidateAndFlushBlocking(K key) {
        flushKeyBlocking(key);
        syncCache.invalidate(key);
        syncCache.cleanUp();
    }

    public CompletableFuture<Void> flushDirtyAsync() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        writeExecutor.execute(() -> {
            try {
                flushDirty();
                future.complete(null);
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    @Blocking
    public void flushDirtyBlocking() {
        flushDirtySafely();
    }

    private void flushDirty() {
        try {
            flushDirtyThrowing();
        } catch (Exception exception) {
            logger.error("{} failed to flush dirty entries.", name, exception);
        }
    }

    private void flushDirtyThrowing() throws Exception {
        Object2ObjectLinkedOpenHashMap<K, VersionedSnapshot<V>> snapshots = collectDirtySnapshots();
        if (snapshots.isEmpty()) {
            return;
        }

        Object2ObjectLinkedOpenHashMap<K, Optional<V>> entries = new Object2ObjectLinkedOpenHashMap<>();
        for (Map.Entry<K, VersionedSnapshot<V>> entry : snapshots.object2ObjectEntrySet()) {
            entries.put(entry.getKey(), entry.getValue().value());
        }
        repository.flush(entries);
        for (Map.Entry<K, VersionedSnapshot<V>> entry : snapshots.object2ObjectEntrySet()) {
            dirtyVersions.remove(entry.getKey(), entry.getValue().version());
        }
    }

    private void flushDirtySafely() {
        flushDirty();
    }

    private void flushKeyAsync(K key) {
        writeExecutor.execute(() -> flushKeyBlocking(key));
    }

    private void flushSnapshotAsync(K key, Optional<V> value, long version) {
        writeExecutor.execute(() -> {
            try {
                flushSnapshotBlocking(key, value, version);
            } catch (Exception exception) {
                logger.error("{} failed to save cache entry {}.", name, key, exception);
            }
        });
    }

    private CompletableFuture<Void> flushKeyFuture(K key) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        writeExecutor.execute(() -> {
            try {
                flushKeyBlocking(key);
                future.complete(null);
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    private void flushKeyBlocking(K key) {
        try {
            flushKeyThrowing(key);
        } catch (Exception exception) {
            logger.error("{} failed to save cache entry {}.", name, key, exception);
        }
    }

    private void flushKeyThrowing(K key) throws Exception {
        Optional<V> value = syncCache.getIfPresent(key);
        Long version = dirtyVersions.get(key);
        if (value == null || version == null) {
            return;
        }
        flushSnapshotBlocking(key, value, version);
    }

    private void flushSnapshotBlocking(K key, Optional<V> value, long version) throws Exception {
        repository.flush(Map.of(key, value));
        dirtyVersions.remove(key, version);
    }

    private void markDirty(K key) {
        dirtyVersions.put(key, versionSequence.incrementAndGet());
    }

    private Object2ObjectLinkedOpenHashMap<K, VersionedSnapshot<V>> collectDirtySnapshots() {
        Object2ObjectLinkedOpenHashMap<K, VersionedSnapshot<V>> snapshots = new Object2ObjectLinkedOpenHashMap<>();
        for (K key : new ArrayList<>(dirtyVersions.keySet())) {
            Optional<V> value = syncCache.getIfPresent(key);
            Long version = dirtyVersions.get(key);
            if (value != null && version != null) {
                snapshots.put(key, new VersionedSnapshot<>(value, version));
            }
        }
        return snapshots;
    }

    private Optional<V> loadFromRepository(K key) {
        try {
            return repository.load(key);
        } catch (Exception exception) {
            throw new CompletionException(exception);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private AsyncLoadingCache<K, Optional<V>> buildCache(
            Caffeine<Object, Object> cacheBuilder,
            RemovalListener<K, Optional<V>> removalListener
    ) {
        return ((Caffeine<K, Optional<V>>) (Caffeine) cacheBuilder)
                .executor(readExecutor)
                .removalListener(removalListener)
                .buildAsync((key, executor) -> CompletableFuture.supplyAsync(() -> loadFromRepository(key), executor));
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException(name + " cache is closed");
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        logger.info("Shutting down {} cache...", name);
        flushDirtySafely();
        syncCache.invalidateAll();
        syncCache.cleanUp();
        flushDirtySafely();
        writeExecutor.shutdown();
        readExecutor.shutdown();
        try {
            if (!writeExecutor.awaitTermination(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                logger.warn("{} writeback executor did not stop within {}.", name, shutdownTimeout);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private record VersionedSnapshot<V>(Optional<V> value, long version) {
    }

}
