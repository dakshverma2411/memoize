package space.hypercode.core.converters;

import space.hypercode.core.annotations.MemoizeThis;
import space.hypercode.core.configs.MemoizationConfig;
import space.hypercode.core.configs.MemoizationConfigs;
import space.hypercode.core.models.Memoizable;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the appropriate {@link MemoizationKeyConverter} for a given {@code @Memoize}-annotated method call.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>If {@code useConfig=true} → get converter from {@link MemoizationConfig} (looked up by cache name)</li>
 *   <li>Else if {@code converter} is explicitly set in annotation (not the sentinel default) → instantiate via reflection</li>
 *   <li>Else if single argument implements {@link Memoizable} → use {@link MemoizableKeyConverter}</li>
 *   <li>Else → return empty (passthrough, skip caching)</li>
 * </ol>
 */
public class ConverterResolver {

    private static final MemoizableKeyConverter MEMOIZABLE_KEY_CONVERTER = new MemoizableKeyConverter();

    private final MemoizationConfigs configs;
    private final ConcurrentHashMap<Class<? extends MemoizationKeyConverter>, MemoizationKeyConverter> converterCache;

    public ConverterResolver(final MemoizationConfigs configs) {
        this.configs = configs;
        this.converterCache = new ConcurrentHashMap<>();
    }

    /**
     * Resolves the converter for a method call.
     *
     * @param annotation the @Memoize annotation on the method
     * @param memoizationName  the resolved cache name
     * @param args       the method arguments
     * @return the converter to use, or empty if caching should be skipped
     */
    public Optional<MemoizationKeyConverter> resolve(final MemoizeThis annotation,
                                                     final String memoizationName,
                                                     final Object[] args) {

        // 1. useConfig=true → get converter from config
        if (annotation.useConfig()) {
            return resolveFromConfig(memoizationName);
        }

        // 2. Explicit converter in annotation (not the sentinel default)
        if (hasExplicitConverter(annotation)) {
            return resolveFromAnnotation(annotation.converter());
        }

        // 3. Single arg that implements Memoizable
        if (isSingleMemoizableArg(args)) {
            return Optional.of(MEMOIZABLE_KEY_CONVERTER);
        }

        // 4. No converter resolvable → passthrough
        return Optional.empty();
    }

    private Optional<MemoizationKeyConverter> resolveFromConfig(final String memoizationName) {
        return configs.get(memoizationName)
                .map(MemoizationConfig::getConverter);
    }

    private Optional<MemoizationKeyConverter> resolveFromAnnotation(
            final Class<? extends MemoizationKeyConverter> converterClass) {
        try {
            MemoizationKeyConverter converter = converterCache.computeIfAbsent(
                    converterClass,
                    this::instantiateConverter
            );
            return Optional.of(converter);
        } catch (final RuntimeException e) {
            return Optional.empty();
        }
    }

    private MemoizationKeyConverter instantiateConverter(final Class<? extends MemoizationKeyConverter> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (final Exception e) {
            throw new IllegalStateException(
                    "Failed to instantiate converter: " + clazz.getName() + ". Ensure it has a public no-arg constructor.", e);
        }
    }

    private boolean hasExplicitConverter(final MemoizeThis annotation) {
        return annotation.converter() != MemoizationKeyConverter.class;
    }

    private boolean isSingleMemoizableArg(final Object[] args) {
        return args != null
                && args.length == 1
                && args[0] instanceof Memoizable;
    }
}
