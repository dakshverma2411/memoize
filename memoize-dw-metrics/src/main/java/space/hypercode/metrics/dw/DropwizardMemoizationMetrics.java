package space.hypercode.metrics.dw;

import com.codahale.metrics.MetricRegistry;
import space.hypercode.core.metrics.MemoizationMetrics;

import java.util.concurrent.TimeUnit;

/**
 * {@link MemoizationMetrics} implementation backed by Dropwizard Metrics.
 *
 * <p>Registers the following metrics per cache name:
 * <ul>
 *   <li>{@code memoize.<memoizationName>.hits} — counter</li>
 *   <li>{@code memoize.<memoizationName>.misses} — counter</li>
 *   <li>{@code memoize.<memoizationName>.puts} — counter</li>
 *   <li>{@code memoize.<memoizationName>.evictions} — counter</li>
 *   <li>{@code memoize.<memoizationName>.get} — timer (cache lookup duration)</li>
 *   <li>{@code memoize.<memoizationName>.put} — timer (cache store duration)</li>
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
    public void recordHit(final String memoizationName) {
        registry.counter(name(memoizationName, "hits")).inc();
    }

    @Override
    public void recordMiss(final String memoizationName) {
        registry.counter(name(memoizationName, "misses")).inc();
    }

    @Override
    public void recordPut(final String memoizationName) {
        registry.counter(name(memoizationName, "puts")).inc();
    }

    @Override
    public void recordEviction(final String memoizationName) {
        registry.counter(name(memoizationName, "evictions")).inc();
    }

    @Override
    public void recordGetDuration(final String memoizationName, final long durationNanos) {
        registry.timer(name(memoizationName, "get")).update(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordPutDuration(final String memoizationName, final long durationNanos) {
        registry.timer(name(memoizationName, "put")).update(durationNanos, TimeUnit.NANOSECONDS);
    }

    private static String name(final String memoizationName, final String metric) {
        return MetricRegistry.name(PREFIX, memoizationName, metric);
    }
}
