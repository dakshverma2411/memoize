package space.hypercode.providers.caffeine;

import space.hypercode.core.providers.MemoizationProvider;
import space.hypercode.core.providers.MemoizationProviderFactory;

import java.time.Duration;

/**
 * Factory that creates {@link CaffeineMemoizationProvider} instances.
 */
public class CaffeineMemoizationProviderFactory implements MemoizationProviderFactory {

    @Override
    public MemoizationProvider create(final String memoizationName, final Duration ttl, final long maxSize) {
        return new CaffeineMemoizationProvider(memoizationName, ttl, maxSize);
    }
}
