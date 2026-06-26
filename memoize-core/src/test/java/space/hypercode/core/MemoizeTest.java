package space.hypercode.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import space.hypercode.core.configs.MemoizationConfigs;
import space.hypercode.core.metrics.NoOpMetrics;
import space.hypercode.core.providers.MemoizationProvider;
import space.hypercode.core.providers.MemoizationProviderFactory;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MemoizeTest {

    @AfterEach
    void resetSingleton() throws Exception {
        final Field instanceField = Memoize.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    void start_setsInstance() {
        final Memoize memoize = createMemoize();
        memoize.start();

        assertSame(memoize, Memoize.getInstance());
    }

    @Test
    void start_calledTwice_throwsIllegalStateException() {
        createMemoize().start();

        assertThrows(IllegalStateException.class, () -> createMemoize().start());
    }

    @Test
    void getInstance_beforeStart_returnsNull() {
        assertNull(Memoize.getInstance());
    }

    @Test
    void builder_nullPackageName_throwsIAE() {
        assertThrows(IllegalArgumentException.class, () ->
                Memoize.create().scanIn(null)
        );
    }

    @Test
    void builder_emptyPackageName_throwsIAE() {
        assertThrows(IllegalArgumentException.class, () ->
                Memoize.create().scanIn("")
        );
    }

    @Test
    void builder_setsAllFields() {
        final Memoize memoize = createMemoize();
        memoize.start();

        assertNotNull(memoize.getConfigs());
        assertNotNull(memoize.getProviderFactory());
        assertNotNull(memoize.getConverterResolver());
        assertInstanceOf(NoOpMetrics.class, memoize.getMetrics(), "Metrics should be no op when not configured");
    }

    private Memoize createMemoize() {
        return Memoize.create()
                .scanIn("space.hypercode.core")
                .configs(new MemoizationConfigs())
                .providerFactory(new StubProviderFactory())
                .build();
    }

    /**
     * Minimal provider factory for testing. Creates stub providers that do nothing.
     */
    private static class StubProviderFactory implements MemoizationProviderFactory {
        @Override
        public MemoizationProvider create(final String cacheName, final Duration ttl, final long maxSize) {
            return new StubProvider(cacheName, ttl, maxSize);
        }
    }

    private static class StubProvider extends MemoizationProvider {
        StubProvider(final String cacheName, final Duration ttl, final long maxSize) {
            super(cacheName, ttl, maxSize);
        }

        @Override
        public Optional<Object> getValueIfPresent(final String key) {
            return Optional.empty();
        }

        @Override
        public void put(final String key, final Object value) { }

        @Override
        public void evictIfPresent(final String key) { }
    }
}
