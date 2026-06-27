package space.hypercode.memoize.examples;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import space.hypercode.core.Memoize;
import space.hypercode.core.configs.MemoizationConfigs;
import space.hypercode.providers.caffeine.CaffeineMemoizationProviderFactory;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExampleTests {

    private App app;

    @BeforeEach
    void setup() {
        app = new App();
        Memoize.create()
                .scanIn("space.hypercode.memoize.examples")
                .configs(new MemoizationConfigs())
                .providerFactory(new CaffeineMemoizationProviderFactory())
                .build()
                .start();
    }

    @AfterEach
    void teardown() throws Exception {
        final Field instanceField = Memoize.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    void testSingleArgUsingConverter() {
        long startMs = System.currentTimeMillis();
        app.square(2L);
        app.square(2L); // query again - should be cached
        long elapsedMs = System.currentTimeMillis() - startMs;
        // First call takes ~2s (cache miss), second should be instant (cache hit).
        // Total should be ~2s. Allow up to 3s for slow CI, but fail if >3s (indicates no caching).
        assertTrue(elapsedMs < 3000,
                "Expected < 3000ms (caching should avoid second 2s wait), but took " + elapsedMs + "ms");
    }

    @Test
    void testSingleUsingInterface() {
        long startMs = System.currentTimeMillis();
        assertEquals(new MyLong(4L), app.square(new MyLong(2L)));
        assertEquals(new MyLong(4L), app.square(new MyLong(2L))); // query again - should be cached
        long elapsedMs = System.currentTimeMillis() - startMs;
        assertTrue(elapsedMs < 3000,
                "Expected < 3000ms (caching should avoid second 2s wait), but took " + elapsedMs + "ms");
    }

    @Test
    void testTtl() {
        long startMs = System.currentTimeMillis();
        assertEquals(new MyLong(8L), app.cube(new MyLong(2L)));
        tryWait(5L);
        assertEquals(new MyLong(8L), app.cube(new MyLong(2L)));
        long elapsedMs = System.currentTimeMillis() - startMs;
        assertTrue(elapsedMs > 4000, "TTL should expire, should be more than 4sec execution time");

    }

    private void tryWait(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
            // do nothing
        }
    }

}
