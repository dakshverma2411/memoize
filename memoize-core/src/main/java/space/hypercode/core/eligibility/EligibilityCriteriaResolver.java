package space.hypercode.core.eligibility;

import space.hypercode.core.annotations.MemoizeThis;
import space.hypercode.core.configs.MemoizationConfig;
import space.hypercode.core.configs.MemoizationConfigs;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the appropriate {@link EligibilityCriteria} for a given {@code @MemoizeThis}-annotated method call.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>If {@code useConfig=true} &rarr; get criteria from {@link MemoizationConfig} (looked up by cache name)</li>
 *   <li>Else if {@code criteria} is explicitly set in annotation (not the sentinel default) &rarr; instantiate via reflection</li>
 *   <li>Else &rarr; return default {@link MemoizeAlways}</li>
 * </ol>
 */
public class EligibilityCriteriaResolver {

    private static final MemoizeAlways DEFAULT_CRITERIA = EligibilityCriterias.ALWAYS;

    private final MemoizationConfigs configs;
    private final ConcurrentHashMap<Class<? extends EligibilityCriteria>, EligibilityCriteria> criteriaCache;

    public EligibilityCriteriaResolver(final MemoizationConfigs configs) {
        this.configs = configs;
        this.criteriaCache = new ConcurrentHashMap<>();
    }

    /**
     * Resolves the eligibility criteria for a method call.
     *
     * @param annotation      the @MemoizeThis annotation on the method
     * @param memoizationName the resolved cache name
     * @return the criteria to use for determining whether a result should be memoized
     */
    public EligibilityCriteria resolve(final MemoizeThis annotation,
                                       final String memoizationName) {

        // 1. useConfig=true -> get criteria from config
        if (annotation.useConfig()) {
            return resolveFromConfig(memoizationName);
        }

        // 2. Explicit criteria in annotation (not the sentinel default)
        if (hasExplicitCriteria(annotation)) {
            return resolveFromAnnotation(annotation.criteria());
        }

        // 3. Default -> always memoize
        return DEFAULT_CRITERIA;
    }

    private EligibilityCriteria resolveFromConfig(final String memoizationName) {
        return configs.get(memoizationName)
                .map(MemoizationConfig::getEligibilityCriteria)
                .orElse(DEFAULT_CRITERIA);
    }

    private EligibilityCriteria resolveFromAnnotation(
            final Class<? extends EligibilityCriteria> criteriaClass) {
        try {
            return criteriaCache.computeIfAbsent(
                    criteriaClass,
                    this::instantiateCriteria
            );
        } catch (final RuntimeException e) {
            return DEFAULT_CRITERIA;
        }
    }

    private EligibilityCriteria instantiateCriteria(final Class<? extends EligibilityCriteria> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (final Exception e) {
            throw new IllegalStateException(
                    "Failed to instantiate eligibility criteria: " + clazz.getName()
                            + ". Ensure it has a public no-arg constructor.", e);
        }
    }

    private boolean hasExplicitCriteria(final MemoizeThis annotation) {
        return annotation.criteria() != MemoizeAlways.class;
    }
}
