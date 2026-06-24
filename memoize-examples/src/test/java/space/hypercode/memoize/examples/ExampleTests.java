package space.hypercode.memoize.examples;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import space.hypercode.core.Memoize;
import space.hypercode.core.configs.MemoizationConfigs;
import space.hypercode.providers.caffeine.CaffeineMemoizationProviderFactory;

import java.lang.reflect.Field;
import java.time.Instant;

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
        Instant startTime = Instant.now();
        System.out.println(app.square(2L));
        System.out.println(app.square(2L)); // query again
        Instant endTime = Instant.now();
        assertTrue(endTime.getEpochSecond() - startTime.getEpochSecond() < 3);
    }

    @Test
    void testSingleUsingInterface() {
        Instant startTime = Instant.now();
        app.square(new MyLong(2L));
        app.square(new MyLong(2L)); // query again
        Instant endTime = Instant.now();
        assertTrue(endTime.getEpochSecond() - startTime.getEpochSecond() < 3);
    }

}
