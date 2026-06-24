package space.hypercode.core.configs;

import org.junit.jupiter.api.Test;
import space.hypercode.core.converters.MemoizableKeyConverter;
import space.hypercode.core.converters.MemoizationKeyConverter;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MemoizationConfigsTest {

    private final MemoizationConfigs configs = new MemoizationConfigs();

    @Test
    void get_existingConfig_returnsPresent() {
        final MemoizationKeyConverter converter = new MemoizableKeyConverter();
        final MemoizationConfig config = MemoizationConfig.builder()
                .ttl(Duration.ofMinutes(10))
                .maxSize(500)
                .converter(converter)
                .build();

        configs.add("userCache", config);

        final Optional<MemoizationConfig> result = configs.get("userCache");
        assertTrue(result.isPresent());
        assertSame(config, result.get());
    }

    @Test
    void get_missingConfig_returnsEmpty() {
        final Optional<MemoizationConfig> result = configs.get("nonExistent");
        assertTrue(result.isEmpty());
    }

    @Test
    void add_overwritesExisting() {
        final MemoizationKeyConverter converter = new MemoizableKeyConverter();
        final MemoizationConfig first = MemoizationConfig.builder()
                .ttl(Duration.ofMinutes(5))
                .maxSize(100)
                .converter(converter)
                .build();
        final MemoizationConfig second = MemoizationConfig.builder()
                .ttl(Duration.ofMinutes(15))
                .maxSize(200)
                .converter(converter)
                .build();

        configs.add("cache", first);
        configs.add("cache", second);

        final Optional<MemoizationConfig> result = configs.get("cache");
        assertTrue(result.isPresent());
        assertSame(second, result.get());
    }
}
