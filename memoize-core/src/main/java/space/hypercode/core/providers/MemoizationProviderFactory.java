package space.hypercode.core.providers;

import java.time.Duration;

/**
 * Factory interface for creating {@link MemoizationProvider} instances.
 * One provider instance is created per named cache.
 */
public interface MemoizationProviderFactory {

    /**
     * Creates a new provider instance for a specific named cache.
     *
     * @param cacheName the name of the cache
     * @param ttl       the time-to-live for cache entries
     * @param maxSize   the maximum number of entries in the cache
     * @return a new provider instance configured for this cache
     */
    MemoizationProvider create(String cacheName, Duration ttl, long maxSize);
}
