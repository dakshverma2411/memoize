package space.hypercode.core.metrics;

/**
 * Interface for recording cache operation metrics.
 *
 * <p>Implementations are responsible for thread safety.
 * All methods receive the cache name so metrics can be scoped per cache.
 *
 * <p>Hit/miss/put are counters; duration methods record timing for the
 * underlying cache operations (not the original method execution).
 * Eviction is provided for cache providers to call when entries are evicted.
 */
public interface MemoizationMetrics {

    /**
     * Records a cache hit (value found in cache).
     */
    void recordHit(String cacheName);

    /**
     * Records a cache miss (value not found, original method will execute).
     */
    void recordMiss(String cacheName);

    /**
     * Records a value being stored in the cache.
     */
    void recordPut(String cacheName);

    /**
     * Records a cache eviction. Intended for provider implementations to call
     * when entries are evicted (e.g. via TTL expiry or size limit).
     */
    void recordEviction(String cacheName);

    /**
     * Records the duration of a cache get (lookup) operation.
     *
     * @param cacheName    the cache name
     * @param durationNanos time taken in nanoseconds
     */
    void recordGetDuration(String cacheName, long durationNanos);

    /**
     * Records the duration of a cache put (store) operation.
     *
     * @param cacheName    the cache name
     * @param durationNanos time taken in nanoseconds
     */
    void recordPutDuration(String cacheName, long durationNanos);
}
