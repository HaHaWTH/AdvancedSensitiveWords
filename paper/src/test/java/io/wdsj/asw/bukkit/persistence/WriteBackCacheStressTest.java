package io.wdsj.asw.bukkit.persistence;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WriteBackCacheStressTest {
    @Test
    void batchReadAndCustomQueryCanObserveFlushedDirtyWrites() {
        InMemoryRepository repository = new InMemoryRepository();
        repository.store.put(1, new TestValue(1, 100L));
        repository.store.put(2, new TestValue(2, 200L));

        try (WriteBackCache<Integer, TestValue> cache = newCache("BatchQuery", repository, 100L)) {
            Map<Integer, Optional<TestValue>> loaded = cache.getAllAsync(List.of(2, 3, 1)).join();

            assertEquals(List.of(2, 3, 1), new ArrayList<>(loaded.keySet()));
            assertEquals(Optional.of(new TestValue(2, 200L)), loaded.get(2));
            assertEquals(Optional.empty(), loaded.get(3));
            assertEquals(Optional.of(new TestValue(1, 100L)), loaded.get(1));

            cache.put(3, new TestValue(3, 300L));

            List<Integer> queried = cache.flushDirtyAsync()
                    .thenCompose(ignored -> cache.queryAsync(() -> repository.findUpdatedSince(250L)))
                    .join();
            assertEquals(List.of(3), queried);
        }
    }

    @Test
    void loadFailureCompletesFutureExceptionally() {
        withCaffeineAsyncLoadWarningsSuppressed(() -> {
            InMemoryRepository repository = new InMemoryRepository();
            repository.failLoads = true;

            try (WriteBackCache<Integer, TestValue> cache = newCache("LoadFailure", repository, 100L)) {
                assertThrows(RuntimeException.class, () -> cache.getAsync(1).join());
            }
        });
    }

    @Test
    void batchLoadFailureCompletesFutureExceptionally() {
        withCaffeineAsyncLoadWarningsSuppressed(() -> {
            InMemoryRepository repository = new InMemoryRepository();
            repository.store.put(1, new TestValue(1, 100L));
            repository.failLoads = true;

            try (WriteBackCache<Integer, TestValue> cache = newCache("BatchLoadFailure", repository, 100L)) {
                assertThrows(RuntimeException.class, () -> cache.getAllAsync(List.of(1, 2)).join());
            }
        });
    }

    @Test
    void staleFlushDoesNotClearNewerDirtyWriteForSameKey() throws Exception {
        InMemoryRepository repository = new InMemoryRepository();
        repository.blockNextSaveForKey(1);

        try (WriteBackCache<Integer, TestValue> cache = newCache("SameKeyRace", repository, 100L)) {
            cache.put(1, new TestValue(10, 10L));

            CompletableFuture<Void> firstFlush = cache.flushDirtyAsync();
            assertTrue(repository.awaitBlockedSave(), "first save did not start");

            cache.put(1, new TestValue(20, 20L));
            repository.releaseBlockedSave();
            firstFlush.join();

            cache.flushDirtyAsync().join();
        }

        assertEquals(new TestValue(20, 20L), repository.store.get(1));
    }

    @Test
    void dirtyFlushUsesSingleRepositoryBatch() {
        int entries = 128;
        InMemoryRepository repository = new InMemoryRepository();

        try (WriteBackCache<Integer, TestValue> cache = newCache("BatchFlush", repository, entries * 2L)) {
            for (int key = 0; key < entries; key++) {
                cache.put(key, new TestValue(key, key));
            }

            cache.flushDirtyAsync().join();
        }

        assertEquals(1, repository.flushCalls.get());
        assertEquals(entries, repository.saveCalls.get());
        assertEquals(entries, repository.store.size());
    }

    @Test
    void concurrentWritesAndInvalidationsArePersistedWithoutBlockingCallers() {
        int entries = 3_000;
        InMemoryRepository repository = new InMemoryRepository();

        try (WriteBackCache<Integer, TestValue> cache = newCache("Stress", repository, entries * 2L)) {
            ExecutorService callers = Executors.newFixedThreadPool(24);
            try {
                List<CompletableFuture<Void>> futures = new ArrayList<>(entries);
                for (int key = 0; key < entries; key++) {
                    int currentKey = key;
                    futures.add(CompletableFuture.runAsync(() -> {
                        cache.put(currentKey, new TestValue(currentKey, currentKey));
                        if ((currentKey & 1) == 0) {
                            cache.invalidate(currentKey);
                        }
                        if (currentKey % 17 == 0) {
                            cache.putAndFlushAsync(currentKey, new TestValue(currentKey, currentKey)).join();
                        }
                    }, callers));
                }
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            } finally {
                callers.shutdown();
                assertTrue(awaitTermination(callers), "caller executor did not stop");
            }
        }

        assertEquals(entries, repository.store.size());
        for (int key = 0; key < entries; key++) {
            assertEquals(new TestValue(key, key), repository.store.get(key));
        }
    }

    private static WriteBackCache<Integer, TestValue> newCache(
            String name,
            InMemoryRepository repository,
            long maximumSize
    ) {
        return new WriteBackCache<>(
                name,
                repository,
                LoggerFactory.getLogger(WriteBackCacheStressTest.class),
                Caffeine.newBuilder()
                        .expireAfterAccess(30L, TimeUnit.MINUTES)
                        .maximumSize(maximumSize),
                Duration.ofHours(1L),
                Duration.ofSeconds(30L),
                FlushPolicy.PERIODIC
        );
    }

    private static boolean awaitTermination(ExecutorService executor) {
        try {
            return executor.awaitTermination(30L, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void withCaffeineAsyncLoadWarningsSuppressed(Runnable runnable) {
        Logger logger = Logger.getLogger("com.github.benmanes.caffeine.cache.LocalAsyncCache");
        Level previousLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
        try {
            runnable.run();
        } finally {
            logger.setLevel(previousLevel);
        }
    }

    private record TestValue(int value, long updatedAt) {
    }

    private static final class InMemoryRepository implements WriteBackRepository<Integer, TestValue> {
        private final Map<Integer, TestValue> store = new ConcurrentHashMap<>();
        private final AtomicBoolean blockSave = new AtomicBoolean();
        private final AtomicInteger flushCalls = new AtomicInteger();
        private final AtomicInteger saveCalls = new AtomicInteger();
        private final AtomicInteger deleteCalls = new AtomicInteger();
        private volatile boolean failLoads;
        private volatile int blockedKey;
        private volatile CountDownLatch saveStarted = new CountDownLatch(0);
        private volatile CountDownLatch releaseSave = new CountDownLatch(0);

        @Override
        public Optional<TestValue> load(Integer key) {
            if (failLoads) {
                throw new IllegalStateException("load failed");
            }
            return Optional.ofNullable(store.get(key));
        }

        @Override
        public void save(Integer key, TestValue value) throws InterruptedException {
            saveCalls.incrementAndGet();
            if (key == blockedKey && blockSave.compareAndSet(true, false)) {
                saveStarted.countDown();
                assertTrue(releaseSave.await(30L, TimeUnit.SECONDS), "blocked save was not released");
            }
            store.put(key, value);
        }

        @Override
        public void delete(Integer key) {
            deleteCalls.incrementAndGet();
            store.remove(key);
        }

        @Override
        public void flush(Map<Integer, Optional<TestValue>> entries) throws Exception {
            flushCalls.incrementAndGet();
            WriteBackRepository.super.flush(entries);
        }

        private List<Integer> findUpdatedSince(long since) {
            return store.entrySet().stream()
                    .filter(entry -> entry.getValue().updatedAt() >= since)
                    .sorted(Map.Entry.comparingByValue((left, right) -> Long.compare(right.updatedAt(), left.updatedAt())))
                    .collect(
                            LinkedHashMap<Integer, TestValue>::new,
                            (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                            LinkedHashMap::putAll
                    )
                    .keySet()
                    .stream()
                    .toList();
        }

        private void blockNextSaveForKey(int key) {
            blockedKey = key;
            saveStarted = new CountDownLatch(1);
            releaseSave = new CountDownLatch(1);
            blockSave.set(true);
        }

        private boolean awaitBlockedSave() throws InterruptedException {
            return saveStarted.await(30L, TimeUnit.SECONDS);
        }

        private void releaseBlockedSave() {
            releaseSave.countDown();
        }
    }
}
