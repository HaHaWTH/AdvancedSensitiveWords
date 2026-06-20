package io.wdsj.asw.bukkit.benchmark;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.deny.WordDenys;
import io.wdsj.asw.bukkit.util.cache.BookCache;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Measures raw DFA lookup and the BookCache hit path with a maximum-sized writable-book payload.
 * Vanilla writable books allow 100 pages of 1,024 characters, yielding 102,400 characters.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(1)
public class SensitiveWordBookBenchmark {
    private static final int MAX_BOOK_PAGES = 100;
    private static final int MAX_PAGE_CHARACTERS = 1024;
    private static final int MAX_BOOK_CHARACTERS = MAX_BOOK_PAGES * MAX_PAGE_CHARACTERS;

    private SensitiveWordBs sensitiveWordBs;
    private String bookContent;

    @Setup(Level.Trial)
    public void setUp() {
        sensitiveWordBs = SensitiveWordBs.newInstance()
                .wordDeny(WordDenys.defaults())
                .init();
        bookContent = "x".repeat(MAX_BOOK_CHARACTERS);

        List<String> cachedMatches = sensitiveWordBs.findAll(bookContent);
        BookCache.initialize(2L, 1L);
        BookCache.INSTANCE.addToBookCache(bookContent, bookContent, cachedMatches);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        BookCache.invalidateAll();
        sensitiveWordBs.destroy();
    }

    @Benchmark
    public int directDfaLookup() {
        return sensitiveWordBs.findAll(bookContent).size();
    }

    @Benchmark
    public int cachedBookLookup() {
        if (!BookCache.INSTANCE.isBookCached(bookContent)) {
            List<String> matches = sensitiveWordBs.findAll(bookContent);
            BookCache.INSTANCE.addToBookCache(bookContent, bookContent, matches);
            return matches.size();
        }
        return BookCache.INSTANCE.getCachedBookSensitiveWordList(bookContent).size();
    }
}
