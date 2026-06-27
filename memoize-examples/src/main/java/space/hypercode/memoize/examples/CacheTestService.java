package space.hypercode.memoize.examples;

import space.hypercode.core.annotations.MemoizeThis;
import space.hypercode.core.eligibility.MemoizeNonNulls;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test service with @MemoizeThis-annotated methods that track invocation counts.
 * Each method increments an AtomicInteger counter when actually executed (cache miss).
 * On cache hit, the method body is not called so the counter remains unchanged.
 * Used by ExhaustiveMemoizeTests for deterministic caching verification.
 */
public class CacheTestService {

    private final AtomicInteger cachedSquareCount = new AtomicInteger(0);
    private final AtomicInteger cachedMemoizableCount = new AtomicInteger(0);
    private final AtomicInteger shortTtlCount = new AtomicInteger(0);
    private final AtomicInteger nonNullOnlyCount = new AtomicInteger(0);
    private final AtomicInteger alwaysNullCount = new AtomicInteger(0);
    private final AtomicInteger noConverterCount = new AtomicInteger(0);
    private final AtomicInteger namedCacheCount = new AtomicInteger(0);
    private final AtomicInteger throwingCount = new AtomicInteger(0);
    private final AtomicInteger configBasedCount = new AtomicInteger(0);

    // --- Memoized methods ---

    /**
     * Explicit converter (LongConverter). TTL 60s, size 100.
     * Tests: explicit converter resolution, basic caching.
     */
    @MemoizeThis(ttlInMs = 60000, size = 100, converter = LongConverter.class)
    public long cachedSquare(final Long x) {
        cachedSquareCount.incrementAndGet();
        return x * x;
    }

    /**
     * Auto-resolved via Memoizable interface (MyLong implements Memoizable). TTL 60s, size 100.
     * Tests: Memoizable interface key resolution.
     */
    @MemoizeThis(ttlInMs = 60000, size = 100)
    public MyLong cachedMemoizable(final MyLong x) {
        cachedMemoizableCount.incrementAndGet();
        return new MyLong(x.getValue() * x.getValue());
    }

    /**
     * Very short TTL (1ms). TTL should expire almost immediately.
     * Tests: TTL expiration causes re-computation.
     */
    @MemoizeThis(ttlInMs = 1, size = 100)
    public MyLong shortTtl(final MyLong x) {
        shortTtlCount.incrementAndGet();
        return new MyLong(x.getValue() * 2);
    }

    /**
     * MemoizeNonNulls criteria. Returns null for odd values, non-null for even.
     * Tests: eligibility criteria - null results not cached, non-null cached.
     */
    @MemoizeThis(ttlInMs = 60000, size = 100, criteria = MemoizeNonNulls.class)
    public MyLong nonNullOnly(final MyLong x) {
        nonNullOnlyCount.incrementAndGet();
        if (x.getValue() % 2 != 0) {
            return null;
        }
        return new MyLong(x.getValue() * x.getValue());
    }

    /**
     * Default criteria (MemoizeAlways) but always returns null.
     * Tests: null values cannot be stored in Caffeine, so method re-executes each time.
     */
    @MemoizeThis(ttlInMs = 60000, size = 100)
    public MyLong alwaysNull(final MyLong x) {
        alwaysNullCount.incrementAndGet();
        return null;
    }

    /**
     * String arg does not implement Memoizable and no converter is specified.
     * ConverterResolver returns empty -> caching is skipped entirely.
     * Tests: no converter resolvable means no caching.
     */
    @MemoizeThis(ttlInMs = 60000, size = 100)
    public String noConverter(final String x) {
        noConverterCount.incrementAndGet();
        return x.toUpperCase();
    }

    /**
     * Explicit cache name override. TTL 60s, size 100.
     * Tests: named cache works correctly.
     */
    @MemoizeThis(name = "test-named-cache", ttlInMs = 60000, size = 100)
    public MyLong namedCache(final MyLong x) {
        namedCacheCount.incrementAndGet();
        return new MyLong(x.getValue() + 1);
    }

    /**
     * Throws IllegalArgumentException for negative values.
     * Tests: exceptions propagate to caller, do not corrupt cache.
     */
    @MemoizeThis(ttlInMs = 60000, size = 100)
    public MyLong throwingMethod(final MyLong x) {
        throwingCount.incrementAndGet();
        if (x.getValue() < 0) {
            throw new IllegalArgumentException("Negative value: " + x.getValue());
        }
        return new MyLong(x.getValue());
    }

    /**
     * Config-based resolution (useConfig=true). Converter and settings come from MemoizationConfig.
     * Tests: config-driven caching.
     */
    @MemoizeThis(name = "test-config-cache", useConfig = true)
    public MyLong configBased(final MyLong x) {
        configBasedCount.incrementAndGet();
        return new MyLong(x.getValue() * 10);
    }

    // --- Counter accessors ---

    public int getCachedSquareCount() {
        return cachedSquareCount.get();
    }

    public int getCachedMemoizableCount() {
        return cachedMemoizableCount.get();
    }

    public int getShortTtlCount() {
        return shortTtlCount.get();
    }

    public int getNonNullOnlyCount() {
        return nonNullOnlyCount.get();
    }

    public int getAlwaysNullCount() {
        return alwaysNullCount.get();
    }

    public int getNoConverterCount() {
        return noConverterCount.get();
    }

    public int getNamedCacheCount() {
        return namedCacheCount.get();
    }

    public int getThrowingCount() {
        return throwingCount.get();
    }

    public int getConfigBasedCount() {
        return configBasedCount.get();
    }
}
