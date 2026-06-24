package space.hypercode.core.providers;

import lombok.Getter;

import java.time.Duration;
import java.util.Optional;

@Getter
public abstract class MemoizationProvider {

    private final String cacheName;
    private final Duration ttl;
    private final long maxSize;

    public MemoizationProvider(
            final String cacheName,
            final Duration ttl,
            final long maxSize) {
       this.cacheName = cacheName;
       this.ttl = ttl;
       this.maxSize = maxSize;
    }

    public abstract Optional<Object> getValueIfPresent(final String key);
    public abstract void put(final String key, final Object value);
    public abstract void evictIfPresent(final String key);
}
