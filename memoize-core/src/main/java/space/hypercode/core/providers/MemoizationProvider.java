package space.hypercode.core.providers;

import java.time.Duration;
import java.util.Optional;

public abstract class MemoizationProvider {

    private final String memoizationName;
    private final Duration ttl;
    private final long maxSize;

    public MemoizationProvider(
            final String memoizationName,
            final Duration ttl,
            final long maxSize) {
       this.memoizationName = memoizationName;
       this.ttl = ttl;
       this.maxSize = maxSize;
    }

    public String getMemoizationName() {
        return memoizationName;
    }

    public Duration getTtl() {
        return ttl;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public abstract Optional<Object> getValueIfPresent(final String key);
    public abstract void put(final String key, final Object value);
    public abstract void evictIfPresent(final String key);
}
