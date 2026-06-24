package space.hypercode.core.metrics;

public class NoOpMetrics implements MemoizationMetrics {
    @Override
    public void recordHit(String cacheName) {

    }

    @Override
    public void recordMiss(String cacheName) {

    }

    @Override
    public void recordPut(String cacheName) {

    }

    @Override
    public void recordEviction(String cacheName) {

    }

    @Override
    public void recordGetDuration(String cacheName, long durationNanos) {

    }

    @Override
    public void recordPutDuration(String cacheName, long durationNanos) {

    }
}
