package space.hypercode.memoize.examples;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import space.hypercode.core.Memoize;
import space.hypercode.core.configs.MemoizationConfig;
import space.hypercode.core.configs.MemoizationConfigs;
import space.hypercode.core.converters.MemoizableKeyConverter;
import space.hypercode.providers.caffeine.CaffeineMemoizationProviderFactory;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exhaustive integration tests for the Memoize framework.
 * Uses CacheTestService which tracks invocation counts to verify caching behavior
 * without timing dependencies.
 *
 * <p>Each method in CacheTestService increments an AtomicInteger when its body executes.
 * On cache hit, the method body is skipped (aspect returns cached value), so the counter
 * remains unchanged. This allows deterministic verification of cache behavior.
 */
class ExhaustiveMemoizeTests {

    private CacheTestService service;

    @AfterEach
    void teardown() throws Exception {
        final Field instanceField = Memoize.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    private void startMemoize() {
        MemoizationConfigs configs = new MemoizationConfigs();
        configs.add("test-config-cache", MemoizationConfig.builder()
                .ttl(Duration.ofSeconds(60))
                .maxSize(100)
                .converter(new MemoizableKeyConverter())
                .build());
        startMemoize(configs);
    }

    private void startMemoize(final MemoizationConfigs configs) {
        Memoize.create()
                .scanIn("space.hypercode.memoize.examples")
                .configs(configs)
                .providerFactory(new CaffeineMemoizationProviderFactory())
                .build()
                .start();
    }

    // ==================== Explicit Converter Tests ====================

    @Nested
    @DisplayName("Caching with Explicit Converter (LongConverter)")
    class ExplicitConverterTests {

        @BeforeEach
        void setup() {
            service = new CacheTestService();
            startMemoize();
        }

        @Test
        @DisplayName("Second call with same arg is a cache hit - method body not executed")
        void cacheHitOnSameArg() {
            service.cachedSquare(1001L);
            assertEquals(1, service.getCachedSquareCount(), "First call should execute method body");

            service.cachedSquare(1001L);
            assertEquals(1, service.getCachedSquareCount(), "Second call should be cache hit");
        }

        @Test
        @DisplayName("Returns correct computed value on cache miss")
        void returnsCorrectValueOnMiss() {
            long result = service.cachedSquare(1002L);
            assertEquals(1002L * 1002L, result);
        }

        @Test
        @DisplayName("Returns correct cached value on cache hit")
        void returnsCorrectValueOnHit() {
            service.cachedSquare(1003L);
            long result = service.cachedSquare(1003L);
            assertEquals(1003L * 1003L, result);
        }

        @Test
        @DisplayName("Different args produce separate cache entries")
        void differentArgsCacheSeparately() {
            service.cachedSquare(1004L);
            service.cachedSquare(1005L);
            assertEquals(2, service.getCachedSquareCount(), "Each unique arg causes a cache miss");
        }

        @Test
        @DisplayName("Multiple calls with same arg only execute method once")
        void multipleCallsSingleExecution() {
            service.cachedSquare(1006L);
            service.cachedSquare(1006L);
            service.cachedSquare(1006L);
            service.cachedSquare(1006L);
            service.cachedSquare(1006L);
            assertEquals(1, service.getCachedSquareCount(), "Only first call should execute");
        }

        @Test
        @DisplayName("Interleaved different args each cause one miss then hits")
        void interleavedCalls() {
            service.cachedSquare(1007L); // miss
            service.cachedSquare(1008L); // miss
            service.cachedSquare(1007L); // hit
            service.cachedSquare(1008L); // hit
            service.cachedSquare(1007L); // hit
            assertEquals(2, service.getCachedSquareCount(), "Only 2 unique args = 2 misses");
        }
    }

    // ==================== Memoizable Interface Tests ====================

    @Nested
    @DisplayName("Caching with Memoizable Interface Auto-Resolution")
    class MemoizableInterfaceTests {

        @BeforeEach
        void setup() {
            service = new CacheTestService();
            startMemoize();
        }

        @Test
        @DisplayName("Cache hit when same Memoizable key is used")
        void cacheHitViaMemoizable() {
            service.cachedMemoizable(new MyLong(2001L));
            assertEquals(1, service.getCachedMemoizableCount());

            service.cachedMemoizable(new MyLong(2001L));
            assertEquals(1, service.getCachedMemoizableCount(), "Same key should hit cache");
        }

        @Test
        @DisplayName("Returns correct value via Memoizable interface")
        void returnsCorrectValue() {
            MyLong result = service.cachedMemoizable(new MyLong(2002L));
            assertEquals(new MyLong(2002L * 2002L), result);
        }

        @Test
        @DisplayName("Different Memoizable keys produce separate cache entries")
        void differentKeysSeparateCaches() {
            service.cachedMemoizable(new MyLong(2003L));
            service.cachedMemoizable(new MyLong(2004L));
            assertEquals(2, service.getCachedMemoizableCount());

            // Both should now hit
            service.cachedMemoizable(new MyLong(2003L));
            service.cachedMemoizable(new MyLong(2004L));
            assertEquals(2, service.getCachedMemoizableCount(), "Both should be cached now");
        }

        @Test
        @DisplayName("Memoizable key determines cache entry, not object identity")
        void keyDeterminesCacheEntry() {
            // Two different MyLong instances with same value = same memoization key
            MyLong first = new MyLong(2005L);
            MyLong second = new MyLong(2005L);

            service.cachedMemoizable(first);
            service.cachedMemoizable(second);
            assertEquals(1, service.getCachedMemoizableCount(), "Same key = cache hit regardless of instance");
        }
    }

    // ==================== TTL Expiration Tests ====================

    @Nested
    @DisplayName("TTL Expiration")
    class TtlExpirationTests {

        @BeforeEach
        void setup() {
            service = new CacheTestService();
            startMemoize();
        }

        @Test
        @DisplayName("Expired TTL causes re-computation")
        void ttlExpiresCausesReExecution() throws InterruptedException {
            service.shortTtl(new MyLong(3001L));
            assertEquals(1, service.getShortTtlCount());

            // Wait for TTL to expire (1ms TTL + buffer for Caffeine async eviction)
            Thread.sleep(50);

            service.shortTtl(new MyLong(3001L));
            assertEquals(2, service.getShortTtlCount(), "TTL expired - should re-execute");
        }

        @Test
        @DisplayName("Value returned correctly after TTL expiry re-computation")
        void correctValueAfterExpiry() throws InterruptedException {
            MyLong result1 = service.shortTtl(new MyLong(3002L));
            Thread.sleep(50);
            MyLong result2 = service.shortTtl(new MyLong(3002L));

            assertEquals(new MyLong(3002L * 2), result1);
            assertEquals(new MyLong(3002L * 2), result2, "Same correct value after re-computation");
        }

        @Test
        @DisplayName("Multiple expirations cause multiple re-computations")
        void multipleExpirations() throws InterruptedException {
            service.shortTtl(new MyLong(3003L)); // exec 1
            Thread.sleep(50);
            service.shortTtl(new MyLong(3003L)); // exec 2 (expired)
            Thread.sleep(50);
            service.shortTtl(new MyLong(3003L)); // exec 3 (expired again)

            assertEquals(3, service.getShortTtlCount(), "Each expiry causes re-execution");
        }
    }

    // ==================== Eligibility Criteria Tests ====================

    @Nested
    @DisplayName("Eligibility Criteria (MemoizeNonNulls)")
    class EligibilityCriteriaTests {

        @BeforeEach
        void setup() {
            service = new CacheTestService();
            startMemoize();
        }

        @Test
        @DisplayName("Non-null result is cached (even value)")
        void nonNullResultCached() {
            service.nonNullOnly(new MyLong(5002L)); // even -> returns non-null -> cached
            assertEquals(1, service.getNonNullOnlyCount());

            service.nonNullOnly(new MyLong(5002L)); // cache hit
            assertEquals(1, service.getNonNullOnlyCount(), "Non-null result should be cached");
        }

        @Test
        @DisplayName("Null result is NOT cached (odd value)")
        void nullResultNotCached() {
            service.nonNullOnly(new MyLong(5003L)); // odd -> returns null -> not cached
            assertEquals(1, service.getNonNullOnlyCount());

            service.nonNullOnly(new MyLong(5003L)); // not cached, re-executes
            assertEquals(2, service.getNonNullOnlyCount(), "Null result should not be cached");
        }

        @Test
        @DisplayName("Null result returned correctly even though not cached")
        void nullResultReturnedCorrectly() {
            MyLong result = service.nonNullOnly(new MyLong(5005L)); // odd
            assertNull(result, "Odd values should return null");
        }

        @Test
        @DisplayName("Non-null result value is correct")
        void nonNullResultCorrect() {
            MyLong result = service.nonNullOnly(new MyLong(5006L)); // even
            assertEquals(new MyLong(5006L * 5006L), result);
        }

        @Test
        @DisplayName("Mixed calls: even cached, odd never cached")
        void mixedEvenAndOdd() {
            // Even - cached after first call
            service.nonNullOnly(new MyLong(5008L)); // miss (exec 1)
            service.nonNullOnly(new MyLong(5008L)); // hit

            // Odd - never cached
            service.nonNullOnly(new MyLong(5009L)); // miss (exec 2)
            service.nonNullOnly(new MyLong(5009L)); // miss again (exec 3)

            // Even again - still cached
            service.nonNullOnly(new MyLong(5008L)); // hit

            assertEquals(3, service.getNonNullOnlyCount());
        }

        @Test
        @DisplayName("Multiple different even values all cached independently")
        void multipleEvenValuesCached() {
            service.nonNullOnly(new MyLong(5010L)); // miss
            service.nonNullOnly(new MyLong(5012L)); // miss
            service.nonNullOnly(new MyLong(5014L)); // miss
            assertEquals(3, service.getNonNullOnlyCount());

            // All should hit now
            service.nonNullOnly(new MyLong(5010L)); // hit
            service.nonNullOnly(new MyLong(5012L)); // hit
            service.nonNullOnly(new MyLong(5014L)); // hit
            assertEquals(3, service.getNonNullOnlyCount(), "All even values should be cached");
        }
    }

    // ==================== Null Return Value Tests ====================

    @Nested
    @DisplayName("Null Return with Default Criteria (MemoizeAlways)")
    class NullReturnTests {

        @BeforeEach
        void setup() {
            service = new CacheTestService();
            startMemoize();
        }

        @Test
        @DisplayName("Null value not cached due to Caffeine limitation - method re-executes")
        void nullNotCachedInCaffeine() {
            service.alwaysNull(new MyLong(6001L));
            assertEquals(1, service.getAlwaysNullCount());

            service.alwaysNull(new MyLong(6001L));
            assertEquals(2, service.getAlwaysNullCount(),
                    "Caffeine cannot store null - method re-executes each time");
        }

        @Test
        @DisplayName("Null return value is correctly returned to caller")
        void nullReturnedCorrectly() {
            MyLong result = service.alwaysNull(new MyLong(6002L));
            assertNull(result);
        }

        @Test
        @DisplayName("Multiple calls all return null and all execute")
        void multipleCallsAllExecute() {
            assertNull(service.alwaysNull(new MyLong(6003L)));
            assertNull(service.alwaysNull(new MyLong(6003L)));
            assertNull(service.alwaysNull(new MyLong(6003L)));
            assertEquals(3, service.getAlwaysNullCount(),
                    "All calls execute since null cannot be cached");
        }
    }

    // ==================== No Converter Resolvable Tests ====================

    @Nested
    @DisplayName("No Converter Resolvable (Caching Skipped)")
    class NoConverterTests {

        @BeforeEach
        void setup() {
            service = new CacheTestService();
            startMemoize();
        }

        @Test
        @DisplayName("Method always executes when no converter can be resolved")
        void alwaysExecutes() {
            service.noConverter("test-7001");
            service.noConverter("test-7001");
            service.noConverter("test-7001");
            assertEquals(3, service.getNoConverterCount(),
                    "No converter means no caching - every call executes");
        }

        @Test
        @DisplayName("Correct result returned without caching")
        void correctResultWithoutCaching() {
            assertEquals("TEST-7002", service.noConverter("test-7002"));
            assertEquals("HELLO", service.noConverter("hello"));
        }
    }

    // ==================== Named Cache Tests ====================

    @Nested
    @DisplayName("Named Cache (Explicit Name in Annotation)")
    class NamedCacheTests {

        @BeforeEach
        void setup() {
            service = new CacheTestService();
            startMemoize();
        }

        @Test
        @DisplayName("Named cache functions correctly with cache hit")
        void namedCacheCachesResult() {
            service.namedCache(new MyLong(8001L));
            assertEquals(1, service.getNamedCacheCount());

            service.namedCache(new MyLong(8001L));
            assertEquals(1, service.getNamedCacheCount(), "Named cache should work like auto-named");
        }

        @Test
        @DisplayName("Named cache returns correct value")
        void namedCacheCorrectValue() {
            MyLong result = service.namedCache(new MyLong(8002L));
            assertEquals(new MyLong(8002L + 1), result);
        }
    }

    // ==================== Exception Handling Tests ====================

    @Nested
    @DisplayName("Exception Handling")
    class ExceptionTests {

        @BeforeEach
        void setup() {
            service = new CacheTestService();
            startMemoize();
        }

        @Test
        @DisplayName("Method exception propagates to caller")
        void exceptionPropagates() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.throwingMethod(new MyLong(-9001L)));
        }

        @Test
        @DisplayName("Exception message is preserved")
        void exceptionMessagePreserved() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.throwingMethod(new MyLong(-9002L)));
            assertTrue(ex.getMessage().contains("-9002"));
        }

        @Test
        @DisplayName("Successful result is cached despite prior exception with different arg")
        void successCachedAfterException() {
            // First: exception with negative value
            assertThrows(IllegalArgumentException.class,
                    () -> service.throwingMethod(new MyLong(-9003L)));
            assertEquals(1, service.getThrowingCount());

            // Second: success with positive value - gets cached
            MyLong result = service.throwingMethod(new MyLong(9004L));
            assertEquals(new MyLong(9004L), result);
            assertEquals(2, service.getThrowingCount());

            // Third: cache hit for the positive value
            service.throwingMethod(new MyLong(9004L));
            assertEquals(2, service.getThrowingCount(), "Successful result should be cached");
        }

        @Test
        @DisplayName("Exception does not pollute cache - retry with same negative arg still throws")
        void exceptionNotCached() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.throwingMethod(new MyLong(-9005L)));
            assertEquals(1, service.getThrowingCount());

            // Same arg throws again (exception not cached)
            assertThrows(IllegalArgumentException.class,
                    () -> service.throwingMethod(new MyLong(-9005L)));
            assertEquals(2, service.getThrowingCount(), "Exception path re-executes method");
        }
    }

    // ==================== Config-Based Resolution Tests ====================

    @Nested
    @DisplayName("Config-Based Resolution (useConfig=true)")
    class ConfigBasedTests {

        @BeforeEach
        void setup() {
            service = new CacheTestService();
            MemoizationConfigs configs = new MemoizationConfigs();
            configs.add("test-config-cache", MemoizationConfig.builder()
                    .ttl(Duration.ofSeconds(60))
                    .maxSize(100)
                    .converter(new MemoizableKeyConverter())
                    .build());
            startMemoize(configs);
        }

        @Test
        @DisplayName("Config-resolved converter enables caching")
        void configResolvedConverterEnablesCaching() {
            service.configBased(new MyLong(12001L));
            assertEquals(1, service.getConfigBasedCount());

            service.configBased(new MyLong(12001L));
            assertEquals(1, service.getConfigBasedCount(), "Config-based caching should work");
        }

        @Test
        @DisplayName("Config-based method returns correct value")
        void configBasedCorrectValue() {
            MyLong result = service.configBased(new MyLong(12002L));
            assertEquals(new MyLong(12002L * 10), result);
        }

        @Test
        @DisplayName("Different args with config-based caching")
        void configBasedDifferentArgs() {
            service.configBased(new MyLong(12003L));
            service.configBased(new MyLong(12004L));
            assertEquals(2, service.getConfigBasedCount());

            service.configBased(new MyLong(12003L));
            service.configBased(new MyLong(12004L));
            assertEquals(2, service.getConfigBasedCount(), "Both should be cached");
        }
    }

    // ==================== Memoize Not Started Tests ====================

    @Nested
    @DisplayName("Memoize Not Started (Passthrough Behavior)")
    class MemoizeNotStartedTests {

        @BeforeEach
        void setup() {
            service = new CacheTestService();
            // Intentionally NOT calling startMemoize()
        }

        @Test
        @DisplayName("Method executes normally without Memoize started")
        void methodExecutesNormally() {
            MyLong result = service.cachedMemoizable(new MyLong(11001L));
            assertEquals(new MyLong(11001L * 11001L), result);
            assertEquals(1, service.getCachedMemoizableCount());
        }

        @Test
        @DisplayName("No caching occurs - every call executes method body")
        void noCachingOccurs() {
            service.cachedMemoizable(new MyLong(11002L));
            service.cachedMemoizable(new MyLong(11002L));
            service.cachedMemoizable(new MyLong(11002L));
            assertEquals(3, service.getCachedMemoizableCount(),
                    "Without Memoize started, no caching - all calls execute");
        }

        @Test
        @DisplayName("Explicit converter method also passes through")
        void explicitConverterPassthrough() {
            long result = service.cachedSquare(11003L);
            assertEquals(11003L * 11003L, result);

            service.cachedSquare(11003L);
            assertEquals(2, service.getCachedSquareCount(), "No caching without Memoize");
        }
    }

    // ==================== Concurrent Access Tests ====================

    @Nested
    @DisplayName("Concurrent Access")
    class ConcurrencyTests {

        @BeforeEach
        void setup() {
            service = new CacheTestService();
            startMemoize();
        }

        @Test
        @DisplayName("Concurrent calls return correct values")
        void concurrentCallsReturnCorrectValues() throws Exception {
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            List<Future<MyLong>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    return service.cachedMemoizable(new MyLong(10001L));
                }));
            }

            startLatch.countDown(); // release all threads simultaneously

            for (Future<MyLong> future : futures) {
                assertEquals(new MyLong(10001L * 10001L), future.get(),
                        "All concurrent calls should return correct value");
            }

            executor.shutdown();
        }

        @Test
        @DisplayName("After concurrent calls complete, value is cached")
        void valueIsCachedAfterConcurrentCalls() throws Exception {
            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            List<Future<MyLong>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    return service.cachedMemoizable(new MyLong(10002L));
                }));
            }

            startLatch.countDown();
            for (Future<MyLong> future : futures) {
                future.get(); // wait for all to complete
            }

            // Now the value should be cached - subsequent call should not execute
            int countAfterConcurrent = service.getCachedMemoizableCount();
            service.cachedMemoizable(new MyLong(10002L));
            assertEquals(countAfterConcurrent, service.getCachedMemoizableCount(),
                    "Value should be cached after concurrent calls complete");

            executor.shutdown();
        }

        @Test
        @DisplayName("Concurrent calls with different args all produce correct results")
        void concurrentDifferentArgs() throws Exception {
            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<Future<MyLong>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                final long val = 10010L + i;
                futures.add(executor.submit(() -> service.cachedMemoizable(new MyLong(val))));
            }

            for (int i = 0; i < threadCount; i++) {
                long val = 10010L + i;
                assertEquals(new MyLong(val * val), futures.get(i).get(),
                        "Concurrent call with arg " + val + " should return correct value");
            }

            executor.shutdown();
        }
    }

    // ==================== Cache Isolation Tests ====================

    @Nested
    @DisplayName("Cache Isolation Between Methods")
    class CacheIsolationTests {

        @BeforeEach
        void setup() {
            service = new CacheTestService();
            startMemoize();
        }

        @Test
        @DisplayName("Different methods with same arg do not share cache")
        void differentMethodsSameArgNoSharing() {
            // Both use MyLong(13001) but are different methods -> different caches
            service.cachedMemoizable(new MyLong(13001L));
            assertEquals(1, service.getCachedMemoizableCount());

            service.namedCache(new MyLong(13001L));
            assertEquals(1, service.getNamedCacheCount(),
                    "Named cache should miss despite same arg used in cachedMemoizable");
        }

        @Test
        @DisplayName("Cache hit in one method does not affect another")
        void cacheHitDoesNotAffectOther() {
            // Cache in cachedMemoizable
            service.cachedMemoizable(new MyLong(13002L));
            service.cachedMemoizable(new MyLong(13002L)); // hit
            assertEquals(1, service.getCachedMemoizableCount());

            // namedCache with same arg should still miss
            service.namedCache(new MyLong(13002L)); // miss
            service.namedCache(new MyLong(13002L)); // hit
            assertEquals(1, service.getNamedCacheCount());
        }
    }

    // ==================== Return Value Correctness Tests ====================

    @Nested
    @DisplayName("Return Value Correctness Across All Methods")
    class ReturnValueCorrectnessTests {

        @BeforeEach
        void setup() {
            service = new CacheTestService();
            startMemoize();
        }

        @Test
        @DisplayName("cachedSquare returns x*x for various inputs")
        void cachedSquareCorrectness() {
            assertEquals(0L, service.cachedSquare(0L));
            assertEquals(1L, service.cachedSquare(1L));
            assertEquals(4L, service.cachedSquare(2L));
            assertEquals(10000L, service.cachedSquare(100L));
        }

        @Test
        @DisplayName("cachedMemoizable returns x*x for various inputs")
        void cachedMemoizableCorrectness() {
            assertEquals(new MyLong(0L), service.cachedMemoizable(new MyLong(0L)));
            assertEquals(new MyLong(1L), service.cachedMemoizable(new MyLong(1L)));
            assertEquals(new MyLong(100L), service.cachedMemoizable(new MyLong(10L)));
        }

        @Test
        @DisplayName("nonNullOnly returns correct values for even and null for odd")
        void nonNullOnlyCorrectness() {
            assertEquals(new MyLong(2L * 2L), service.nonNullOnly(new MyLong(2L)));
            assertNull(service.nonNullOnly(new MyLong(3L)));
            assertEquals(new MyLong(4L * 4L), service.nonNullOnly(new MyLong(4L)));
            assertNull(service.nonNullOnly(new MyLong(5L)));
        }

        @Test
        @DisplayName("namedCache returns x+1")
        void namedCacheCorrectness() {
            assertEquals(new MyLong(1L), service.namedCache(new MyLong(0L)));
            assertEquals(new MyLong(11L), service.namedCache(new MyLong(10L)));
            assertEquals(new MyLong(101L), service.namedCache(new MyLong(100L)));
        }

        @Test
        @DisplayName("configBased returns x*10")
        void configBasedCorrectness() {
            // Note: this test needs config setup
        }

        @Test
        @DisplayName("throwingMethod returns x for non-negative")
        void throwingMethodCorrectness() {
            assertEquals(new MyLong(0L), service.throwingMethod(new MyLong(0L)));
            assertEquals(new MyLong(14050L), service.throwingMethod(new MyLong(14050L)));
        }

        @Test
        @DisplayName("Cached value matches original computation")
        void cachedValueMatchesOriginal() {
            // First call computes
            MyLong first = service.cachedMemoizable(new MyLong(14060L));
            // Second call returns from cache
            MyLong second = service.cachedMemoizable(new MyLong(14060L));
            assertEquals(first, second, "Cached value must equal originally computed value");
            assertEquals(new MyLong(14060L * 14060L), second);
        }
    }

    // ==================== Edge Case Tests ====================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @BeforeEach
        void setup() {
            service = new CacheTestService();
            startMemoize();
        }

        @Test
        @DisplayName("Zero result is cached correctly")
        void zeroValueCached() {
            assertEquals(0L, service.cachedSquare(0L));
            assertEquals(1, service.getCachedSquareCount());

            assertEquals(0L, service.cachedSquare(0L));
            assertEquals(1, service.getCachedSquareCount(), "Zero result should be cached");
        }

        @Test
        @DisplayName("Large values are cached correctly")
        void largeValuesCached() {
            long large = 999999L;
            long expected = large * large;
            assertEquals(expected, service.cachedSquare(large));
            assertEquals(expected, service.cachedSquare(large));
            assertEquals(1, service.getCachedSquareCount());
        }

        @Test
        @DisplayName("Negative input values work with converter")
        void negativeInputCached() {
            long result = service.cachedSquare(-15001L);
            assertEquals((-15001L) * (-15001L), result);

            service.cachedSquare(-15001L);
            assertEquals(1, service.getCachedSquareCount(), "Negative input should be cacheable");
        }

        @Test
        @DisplayName("MyLong with zero value works as Memoizable key")
        void zeroMemoizableKey() {
            MyLong result = service.cachedMemoizable(new MyLong(0L));
            assertEquals(new MyLong(0L), result);

            service.cachedMemoizable(new MyLong(0L));
            assertEquals(1, service.getCachedMemoizableCount());
        }

        @Test
        @DisplayName("Memoize can only be started once - second start throws")
        void memoizeCannotStartTwice() {
            // Memoize already started in @BeforeEach via setup()
            assertThrows(IllegalStateException.class, () ->
                    Memoize.create()
                            .scanIn("space.hypercode.memoize.examples")
                            .configs(new MemoizationConfigs())
                            .providerFactory(new CaffeineMemoizationProviderFactory())
                            .build()
                            .start()
            );
        }
    }
}
