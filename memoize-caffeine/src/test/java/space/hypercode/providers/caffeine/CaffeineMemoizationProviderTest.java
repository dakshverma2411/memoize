package space.hypercode.providers.caffeine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CaffeineMemoizationProviderTest {

    private CaffeineMemoizationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new CaffeineMemoizationProvider("testCache", Duration.ofMinutes(5), 100);
    }

    @Test
    void getValueIfPresent_miss_returnsEmpty() {
        final Optional<Object> result = provider.getValueIfPresent("missing-key");
        assertTrue(result.isEmpty());
    }

    @Test
    void put_thenGet_returnsValue() {
        provider.put("key1", "value1");

        final Optional<Object> result = provider.getValueIfPresent("key1");
        assertTrue(result.isPresent());
        assertEquals("value1", result.get());
    }

    @Test
    void put_overwritesExistingValue() {
        provider.put("key1", "old");
        provider.put("key1", "new");

        final Optional<Object> result = provider.getValueIfPresent("key1");
        assertTrue(result.isPresent());
        assertEquals("new", result.get());
    }

    @Test
    void evictIfPresent_removesEntry() {
        provider.put("key1", "value1");
        provider.evictIfPresent("key1");

        final Optional<Object> result = provider.getValueIfPresent("key1");
        assertTrue(result.isEmpty());
    }

    @Test
    void evictIfPresent_missingKey_noException() {
        assertDoesNotThrow(() -> provider.evictIfPresent("nonexistent"));
    }

    @Test
    void constructor_setsBaseFields() {
        assertEquals("testCache", provider.getMemoizationName());
        assertEquals(Duration.ofMinutes(5), provider.getTtl());
        assertEquals(100, provider.getMaxSize());
    }

    @Test
    void constructor_zeroTtl_doesNotThrow() {
        assertDoesNotThrow(() ->
                new CaffeineMemoizationProvider("cache", Duration.ZERO, 100)
        );
    }

    @Test
    void constructor_zeroMaxSize_doesNotThrow() {
        assertDoesNotThrow(() ->
                new CaffeineMemoizationProvider("cache", Duration.ofMinutes(1), 0)
        );
    }

    @Test
    void constructor_nullTtl_doesNotThrow() {
        assertDoesNotThrow(() ->
                new CaffeineMemoizationProvider("cache", null, 100)
        );
    }
}
