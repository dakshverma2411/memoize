package space.hypercode.providers.caffeine;

import org.junit.jupiter.api.Test;
import space.hypercode.core.providers.MemoizationProvider;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class CaffeineMemoizationProviderFactoryTest {

    private final CaffeineMemoizationProviderFactory factory = new CaffeineMemoizationProviderFactory();

    @Test
    void create_returnsCaffeineProvider() {
        final MemoizationProvider provider = factory.create("myCache", Duration.ofMinutes(5), 100);

        assertNotNull(provider);
        assertInstanceOf(CaffeineMemoizationProvider.class, provider);
    }

    @Test
    void create_setsFieldsCorrectly() {
        final MemoizationProvider provider = factory.create("myCache", Duration.ofMinutes(5), 100);

        assertEquals("myCache", provider.getMemoizationName());
        assertEquals(Duration.ofMinutes(5), provider.getTtl());
        assertEquals(100, provider.getMaxSize());
    }

    @Test
    void create_multipleCallsReturnDistinctInstances() {
        final MemoizationProvider p1 = factory.create("cache1", Duration.ofMinutes(1), 50);
        final MemoizationProvider p2 = factory.create("cache2", Duration.ofMinutes(2), 100);

        assertNotSame(p1, p2);
        assertEquals("cache1", p1.getMemoizationName());
        assertEquals("cache2", p2.getMemoizationName());
    }
}
