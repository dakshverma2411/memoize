package space.hypercode.metrics.dw;

import com.codahale.metrics.MetricRegistry;
import space.hypercode.core.metrics.MemoizationMetrics;

import java.util.concurrent.TimeUnit;

/**
 * {@link MemoizationMetrics} implementation backed by Dropwizard Metrics.
 *
 * <p>Registers the following metrics per cache name:
 * <ul>
 *   <li>{@code memoize.<cacheName>.hits} — counter</li>
 *   <li>{@code memoize.<cacheName>.misses} — counter</li>
 *   <li>{@code memoize.<cacheName>.puts} — counter</li>
 *   <li>{@code memoize.<cacheName>.evictions} — counter</li>
 *   <li>{@code memoize.<cacheName>.get} — timer (cache lookup duration)</li>
 *   <li>{@code memoize.<cacheName>.put} — timer (cache store duration)</li>
 * </ul>
 *
 * <p>Thread safety is provided by the underlying {@link MetricRegistry}.
 */
public class DropwizardMemoizationMetrics implements MemoizationMetrics {

    private static final String PREFIX = "memoize";

    private final MetricRegistry registry;

    public DropwizardMemoizationMetrics(final MetricRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("MetricRegistry must not be null");
        }
        this.registry = registry;
    }

    @Override
    public void recordHit(final String cacheName) {
        registry.counter(name(cacheName, "hits")).inc();
    }

    @Override
    public void recordMiss(final String cacheName) {
        registry.counter(name(cacheName, "misses")).inc();
    }

    @Override
    public void recordPut(final String cacheName) {
        registry.counter(name(cacheName, "puts")).inc();
    }

    @Override
    public void recordEviction(final String cacheName) {
        registry.counter(name(cacheName, "evictions")).inc();
    }

    @Override
    public void recordGetDuration(final String cacheName, final long durationNanos) {
        registry.timer(name(cacheName, "get")).update(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordPutDuration(final String cacheName, final long durationNanos) {
        registry.timer(name(cacheName, "put")).update(durationNanos, TimeUnit.NANOSECONDS);
    }

    private static String name(final String cacheName, final String metric) {
        return MetricRegistry.name(PREFIX, cacheName, metric);
    }
}
