package space.hypercode.providers.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import space.hypercode.core.providers.MemoizationProvider;

import java.time.Duration;
import java.util.Optional;

public class CaffeineMemoizationProvider extends MemoizationProvider {

    private final Cache<String, Object> cache;

    public CaffeineMemoizationProvider(final String memoizationName,
                                       final Duration ttl,
                                       final long maxSize) {
        super(memoizationName, ttl, maxSize);

        final Caffeine<Object, Object> builder = Caffeine.newBuilder();

        if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
            builder.expireAfterWrite(ttl);
        }

        if (maxSize > 0) {
            builder.maximumSize(maxSize);
        }

        this.cache = builder.build();
    }

    @Override
    public Optional<Object> getValueIfPresent(final String key) {
        return Optional.ofNullable(cache.getIfPresent(key));
    }

    @Override
    public void put(final String key, final Object value) {
        cache.put(key, value);
    }

    @Override
    public void evictIfPresent(final String key) {
        cache.invalidate(key);
    }
}
