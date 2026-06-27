package space.hypercode.core.metrics;

public class NoOpMetrics implements MemoizationMetrics {
    @Override
    public void recordHit(String memoizationName) {
        // no op
    }

    @Override
    public void recordMiss(String memoizationName) {
        // no op
    }

    @Override
    public void recordPut(String memoizationName) {
        // no op
    }

    @Override
    public void recordEviction(String memoizationName) {
        // no op
    }

    @Override
    public void recordGetDuration(String memoizationName, long durationNanos) {
        // no op
    }

    @Override
    public void recordPutDuration(String memoizationName, long durationNanos) {
        // no op
    }
}
