package space.hypercode.metrics.dw;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DropwizardMemoizationMetricsTest {

    private MetricRegistry registry;
    private DropwizardMemoizationMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new MetricRegistry();
        metrics = new DropwizardMemoizationMetrics(registry);
    }

    @Test
    void constructor_nullRegistry_throwsIAE() {
        assertThrows(IllegalArgumentException.class, () ->
                new DropwizardMemoizationMetrics(null)
        );
    }

    @Test
    void recordHit_incrementsCounter() {
        metrics.recordHit("users");
        metrics.recordHit("users");

        final Counter counter = registry.counter("memoize.users.hits");
        assertEquals(2, counter.getCount());
    }

    @Test
    void recordMiss_incrementsCounter() {
        metrics.recordMiss("users");

        final Counter counter = registry.counter("memoize.users.misses");
        assertEquals(1, counter.getCount());
    }

    @Test
    void recordPut_incrementsCounter() {
        metrics.recordPut("orders");
        metrics.recordPut("orders");
        metrics.recordPut("orders");

        final Counter counter = registry.counter("memoize.orders.puts");
        assertEquals(3, counter.getCount());
    }

    @Test
    void recordEviction_incrementsCounter() {
        metrics.recordEviction("sessions");

        final Counter counter = registry.counter("memoize.sessions.evictions");
        assertEquals(1, counter.getCount());
    }

    @Test
    void recordGetDuration_updatesTimer() {
        metrics.recordGetDuration("users", 1_000_000L); // 1ms in nanos

        final Timer timer = registry.timer("memoize.users.get");
        assertEquals(1, timer.getCount());
    }

    @Test
    void recordPutDuration_updatesTimer() {
        metrics.recordPutDuration("users", 500_000L);
        metrics.recordPutDuration("users", 750_000L);

        final Timer timer = registry.timer("memoize.users.put");
        assertEquals(2, timer.getCount());
    }

    @Test
    void differentCacheNames_useSeparateMetrics() {
        metrics.recordHit("cache-a");
        metrics.recordHit("cache-b");
        metrics.recordHit("cache-b");

        assertEquals(1, registry.counter("memoize.cache-a.hits").getCount());
        assertEquals(2, registry.counter("memoize.cache-b.hits").getCount());
    }
}
