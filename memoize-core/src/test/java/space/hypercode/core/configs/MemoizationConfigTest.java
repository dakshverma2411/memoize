package space.hypercode.core.configs;

import org.junit.jupiter.api.Test;
import space.hypercode.core.converters.MemoizableKeyConverter;
import space.hypercode.core.converters.MemoizationKeyConverter;
import space.hypercode.core.eligibility.EligibilityCriterias;
import space.hypercode.core.eligibility.MemoizeAlways;
import space.hypercode.core.eligibility.MemoizeNonNulls;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class MemoizationConfigTest {

    @Test
    void builder_setsAllFields() {
        final MemoizationKeyConverter converter = new MemoizableKeyConverter();
        final MemoizationConfig config = MemoizationConfig.builder()
            .ttl(Duration.ofMinutes(10))
            .maxSize(500)
            .converter(converter)
            .eligibilityCriteria(EligibilityCriterias.ONLY_NON_NULLS)
            .build();

        assertEquals(Duration.ofMinutes(10), config.getTtl());
        assertEquals(500, config.getMaxSize());
        assertSame(converter, config.getConverter());
        assertSame(MemoizeNonNulls.getInstance(), config.getEligibilityCriteria());
    }

    @Test
    void builder_cacheNullsDefaultsToTrue() {
        final MemoizationConfig config = MemoizationConfig.builder()
            .ttl(Duration.ofMinutes(1))
            .maxSize(10)
            .converter(new MemoizableKeyConverter())
            .build();

        assertSame(MemoizeAlways.getInstance(), config.getEligibilityCriteria());
    }

    @Test
    void builder_nullTtl_throws() {
        assertThrows(NullPointerException.class, () ->
            MemoizationConfig.builder()
                .ttl(null)
                .maxSize(10)
                .converter(new MemoizableKeyConverter())
                .build()
        );
    }

    @Test
    void builder_nullConverter_throws() {
        assertThrows(NullPointerException.class, () ->
            MemoizationConfig.builder()
                .ttl(Duration.ofMinutes(1))
                .maxSize(10)
                .converter(null)
                .build()
        );
    }
}
