package space.hypercode.core.converters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.hypercode.core.annotations.MemoizeThis;
import space.hypercode.core.configs.MemoizationConfig;
import space.hypercode.core.configs.MemoizationConfigs;
import space.hypercode.core.models.Memoizable;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConverterResolverTest {

    @Mock
    private MemoizeThis annotation;

    private MemoizationConfigs configs;
    private ConverterResolver resolver;

    @BeforeEach
    void setUp() {
        configs = new MemoizationConfigs();
        resolver = new ConverterResolver(configs);
    }

    // --- Path 1: useConfig=true → converter from config ---

    @Test
    void resolve_useConfigTrue_returnsConverterFromConfig() {
        final TestSingleArgConverter configConverter = new TestSingleArgConverter();
        configs.add("myCache", MemoizationConfig.builder()
                .ttl(Duration.ofMinutes(5))
                .maxSize(100)
                .converter(configConverter)
                .build());

        when(annotation.useConfig()).thenReturn(true);

        final Optional<MemoizationKeyConverter> result = resolver.resolve(annotation, "myCache", new Object[]{"arg"});

        assertTrue(result.isPresent());
        assertSame(configConverter, result.get());
    }

    @Test
    void resolve_useConfigTrue_missingConfig_returnsEmpty() {
        when(annotation.useConfig()).thenReturn(true);

        final Optional<MemoizationKeyConverter> result = resolver.resolve(annotation, "nonExistent", new Object[]{"arg"});

        assertTrue(result.isEmpty());
    }

    // --- Path 2: explicit converter in annotation ---

    @Test
    void resolve_explicitConverter_returnsInstantiatedConverter() {
        when(annotation.useConfig()).thenReturn(false);
        doReturn(TestSingleArgConverter.class).when(annotation).converter();

        final Optional<MemoizationKeyConverter> result = resolver.resolve(annotation, "myCache", new Object[]{"arg"});

        assertTrue(result.isPresent());
        assertInstanceOf(TestSingleArgConverter.class, result.get());
    }

    @Test
    void resolve_explicitConverter_cachesInstance() {
        when(annotation.useConfig()).thenReturn(false);
        doReturn(TestSingleArgConverter.class).when(annotation).converter();

        final Optional<MemoizationKeyConverter> first = resolver.resolve(annotation, "myCache", new Object[]{"arg"});
        final Optional<MemoizationKeyConverter> second = resolver.resolve(annotation, "myCache", new Object[]{"arg"});

        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
        assertSame(first.get(), second.get(), "Converter instances should be cached and reused");
    }

    @Test
    void resolve_explicitConverter_noNoArgConstructor_returnsEmpty() {
        when(annotation.useConfig()).thenReturn(false);
        doReturn(NoNoArgConstructorConverter.class).when(annotation).converter();

        final Optional<MemoizationKeyConverter> result = resolver.resolve(annotation, "myCache", new Object[]{"arg"});

        assertTrue(result.isEmpty());
    }

    // --- Path 3: single Memoizable arg ---

    @Test
    void resolve_singleMemoizableArg_returnsMemoizableKeyConverter() {
        when(annotation.useConfig()).thenReturn(false);
        doReturn(MemoizationKeyConverter.class).when(annotation).converter();

        final Memoizable memoizable = () -> "key-123";
        final Optional<MemoizationKeyConverter> result = resolver.resolve(annotation, "myCache", new Object[]{memoizable});

        assertTrue(result.isPresent());
        assertInstanceOf(MemoizableKeyConverter.class, result.get());
    }

    // --- Path 4: no converter resolvable → passthrough ---

    @Test
    void resolve_noConverterResolvable_returnsEmpty() {
        when(annotation.useConfig()).thenReturn(false);
        doReturn(MemoizationKeyConverter.class).when(annotation).converter();

        final Optional<MemoizationKeyConverter> result = resolver.resolve(annotation, "myCache", new Object[]{"plainString"});

        assertTrue(result.isEmpty());
    }

    @Test
    void resolve_nullArgs_returnsEmpty() {
        when(annotation.useConfig()).thenReturn(false);
        doReturn(MemoizationKeyConverter.class).when(annotation).converter();

        final Optional<MemoizationKeyConverter> result = resolver.resolve(annotation, "myCache", null);

        assertTrue(result.isEmpty());
    }

    @Test
    void resolve_emptyArgs_returnsEmpty() {
        when(annotation.useConfig()).thenReturn(false);
        doReturn(MemoizationKeyConverter.class).when(annotation).converter();

        final Optional<MemoizationKeyConverter> result = resolver.resolve(annotation, "myCache", new Object[]{});

        assertTrue(result.isEmpty());
    }

    @Test
    void resolve_multipleArgs_noneMemoizable_returnsEmpty() {
        when(annotation.useConfig()).thenReturn(false);
        doReturn(MemoizationKeyConverter.class).when(annotation).converter();

        final Optional<MemoizationKeyConverter> result = resolver.resolve(annotation, "myCache", new Object[]{"a", "b"});

        assertTrue(result.isEmpty());
    }

    // --- Test helpers ---

    /** A simple converter with a public no-arg constructor for testing. */
    public static class TestSingleArgConverter implements SingleArgMemoizationKeyConverter<String> {
        @Override
        public String toKey(final String input) {
            return "test-" + input;
        }
    }

    /** A converter with NO no-arg constructor — should fail to instantiate. */
    public static class NoNoArgConstructorConverter implements SingleArgMemoizationKeyConverter<String> {
        @SuppressWarnings("unused")
        private final String required;

        public NoNoArgConstructorConverter(final String required) {
            this.required = required;
        }

        @Override
        public String toKey(final String input) {
            return input;
        }
    }
}
