package space.hypercode.core.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import space.hypercode.core.Memoize;
import space.hypercode.core.annotations.MemoizeThis;
import space.hypercode.core.configs.MemoizationConfig;
import space.hypercode.core.configs.MemoizationConfigs;
import space.hypercode.core.converters.ConverterResolver;
import space.hypercode.core.converters.MemoizationKeyConverter;
import space.hypercode.core.converters.MultiArgMemoizationKeyConverter;
import space.hypercode.core.converters.SingleArgMemoizationKeyConverter;
import space.hypercode.core.providers.MemoizationProvider;
import space.hypercode.core.providers.MemoizationProviderFactory;

import space.hypercode.core.metrics.MemoizationMetrics;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AspectJ aspect that intercepts methods annotated with {@code @Memoize}
 * and provides transparent caching via the configured provider.
 *
 * <p>This aspect is designed to never throw exceptions that affect the original method.
 * All cache-related failures are logged as warnings and result in a passthrough
 * to the original method execution.
 */
@Aspect
public class MemoizeAspect {

    private static final Logger LOG = Logger.getLogger(MemoizeAspect.class.getName());

    private final ConcurrentHashMap<String, MemoizationProvider> providerCache = new ConcurrentHashMap<>();

    @Around("@annotation(memoizeAnnotation)")
    public Object aroundMemoized(final ProceedingJoinPoint joinPoint,
                                 final MemoizeThis memoizeAnnotation) throws Throwable {

        // 1. Get singleton (if null → warn + passthrough)
        final Memoize memoize = Memoize.getInstance();
        if (memoize == null) {
            LOG.warning("Memoize: @Memoize method called before Memoize.start(). Skipping cache.");
            return joinPoint.proceed();
        }

        // 2. Try to serve from cache (all cache-read logic is guarded)
        try {
            final Object cachedResult = tryGetFromCache(joinPoint, memoizeAnnotation, memoize);
            if (cachedResult != CACHE_MISS) {
                return cachedResult;
            }
        } catch (final Exception e) {
            LOG.log(Level.WARNING, "Memoize: Cache lookup failed. Proceeding without cache.", e);
        }

        // 3. Cache miss or cache error → proceed with original method
        //    Let the original method's exceptions propagate naturally.
        final Object result = joinPoint.proceed();

        // 4. Try to store result in cache (all cache-write logic is guarded)
        try {
            tryPutInCache(joinPoint, memoizeAnnotation, memoize, result);
        } catch (final Exception e) {
            LOG.log(Level.WARNING, "Memoize: Failed to store result in cache.", e);
        }

        return result;
    }

    /**
     * Sentinel object to distinguish "cache miss" from a cached null value.
     */
    private static final Object CACHE_MISS = new Object();

    /**
     * Attempts to retrieve a cached value. Returns the cached value on hit,
     * or {@link #CACHE_MISS} if the value is not cached or caching should be skipped.
     */
    private Object tryGetFromCache(final ProceedingJoinPoint joinPoint,
                                   final MemoizeThis annotation,
                                   final Memoize memoize) {
        final String memoizationName = resolveMemoizationName(annotation, joinPoint);

        final ConverterResolver converterResolver = memoize.getConverterResolver();
        final Object[] args = joinPoint.getArgs();
        final Optional<MemoizationKeyConverter> converterOpt = converterResolver.resolve(annotation, memoizationName, args);

        if (converterOpt.isEmpty()) {
            return CACHE_MISS;
        }

        final String key = generateKey(converterOpt.get(), args);
        if (key == null || key.isEmpty()) {
            return CACHE_MISS;
        }

        final MemoizationProvider provider = getOrCreateProvider(memoizationName, annotation, memoize);
        if (provider == null) {
            return CACHE_MISS;
        }

        final MemoizationMetrics metrics = memoize.getMetrics();

        final long start = System.nanoTime();
        final Optional<Object> cachedValue = provider.getValueIfPresent(key);
        final long durationNanos = System.nanoTime() - start;

        if (metrics != null) {
            recordMetricSafely(() -> metrics.recordGetDuration(memoizationName, durationNanos));
            if (cachedValue.isPresent()) {
                recordMetricSafely(() -> metrics.recordHit(memoizationName));
            } else {
                recordMetricSafely(() -> metrics.recordMiss(memoizationName));
            }
        }

        return cachedValue.orElse(CACHE_MISS);

    }

    /**
     * Attempts to store a result in the cache. Silently returns on any failure.
     */
    private void tryPutInCache(final ProceedingJoinPoint joinPoint,
                               final MemoizeThis annotation,
                               final Memoize memoize,
                               final Object result) {
        final String memoizationName = resolveMemoizationName(annotation, joinPoint);

        final ConverterResolver converterResolver = memoize.getConverterResolver();
        final Object[] args = joinPoint.getArgs();
        final Optional<MemoizationKeyConverter> converterOpt = converterResolver.resolve(annotation, memoizationName, args);

        if (converterOpt.isEmpty()) {
            return;
        }

        final String key = generateKey(converterOpt.get(), args);
        if (key == null || key.isEmpty()) {
            return;
        }

        final boolean cacheNulls = resolveCacheNulls(annotation, memoizationName, memoize.getConfigs());
        if (result == null && !cacheNulls) {
            return;
        }

        final MemoizationProvider provider = getOrCreateProvider(memoizationName, annotation, memoize);
        if (provider == null) {
            return;
        }

        final MemoizationMetrics metrics = memoize.getMetrics();

        final long start = System.nanoTime();
        provider.put(key, result);
        final long durationNanos = System.nanoTime() - start;

        if (metrics != null) {
            recordMetricSafely(() -> metrics.recordPut(memoizationName));
            recordMetricSafely(() -> metrics.recordPutDuration(memoizationName, durationNanos));
        }
    }

    private String resolveMemoizationName(final MemoizeThis annotation,
                                          final ProceedingJoinPoint joinPoint) {
        if (annotation.name() != null && !annotation.name().isEmpty()) {
            return annotation.name();
        }
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final StringBuilder name = new StringBuilder();
        name.append(signature.getDeclaringType().getName())
                .append(".")
                .append(signature.getName())
                .append("(");
        final Class<?>[] paramTypes = signature.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) {
                name.append(",");
            }
            name.append(paramTypes[i].getName());
        }
        name.append(")");
        return name.toString();
    }

    @SuppressWarnings("unchecked")
    private String generateKey(final MemoizationKeyConverter converter, final Object[] args) {
        try {
            if (converter instanceof SingleArgMemoizationKeyConverter) {
                final SingleArgMemoizationKeyConverter<Object> singleArgConverter =
                        (SingleArgMemoizationKeyConverter<Object>) converter;
                final Object arg = (args != null && args.length > 0) ? args[0] : null;
                return singleArgConverter.toKey(arg);
            } else if (converter instanceof MultiArgMemoizationKeyConverter multiArgConverter) {
                return multiArgConverter.toKey(args);
            }
            return null;
        } catch (final Exception e) {
            LOG.log(Level.WARNING, "Memoize: Key generation failed, skipping cache.", e);
            return null;
        }
    }

    private MemoizationProvider getOrCreateProvider(final String memoizationName,
                                                    final MemoizeThis annotation,
                                                    final Memoize memoize) {
        try {
            return providerCache.computeIfAbsent(memoizationName, name -> {
                final Duration ttl;
                final long maxSize;

                if (annotation.useConfig()) {
                    final Optional<MemoizationConfig> configOpt = memoize.getConfigs().get(name);
                    if (configOpt.isPresent()) {
                        final MemoizationConfig config = configOpt.get();
                        ttl = config.getTtl();
                        maxSize = config.getMaxSize();
                    } else {
                        ttl = Duration.ofMillis(annotation.ttlInMs());
                        maxSize = annotation.size();
                    }
                } else {
                    ttl = Duration.ofMillis(annotation.ttlInMs());
                    maxSize = annotation.size();
                }

                return createProvider(name, ttl, maxSize, memoize);
            });
        } catch (final Exception e) {
            LOG.log(Level.WARNING, "Memoize: Failed to create provider for cache '" + memoizationName + "'.", e);
            return null;
        }
    }

    private MemoizationProvider createProvider(final String memoizationName,
                                              final Duration ttl,
                                              final long maxSize,
                                              final Memoize memoize) {
        final MemoizationProviderFactory factory = memoize.getProviderFactory();
        return factory.create(memoizationName, ttl, maxSize);
    }

    private boolean resolveCacheNulls(final MemoizeThis annotation,
                                      final String memoizationName,
                                      final MemoizationConfigs configs) {
        if (annotation.useConfig()) {
            final Optional<MemoizationConfig> configOpt = configs.get(memoizationName);
            if (configOpt.isPresent()) {
                return configOpt.get().isCacheNulls();
            }
        }
        return annotation.cacheNulls();
    }

    /**
     * Executes a metrics recording action, swallowing any exception so that
     * a broken metrics implementation never affects cache or application behavior.
     */
    private void recordMetricSafely(final Runnable action) {
        try {
            action.run();
        } catch (final Exception e) {
            LOG.log(Level.WARNING, "Memoize: Metrics recording failed.", e);
        }
    }
}
