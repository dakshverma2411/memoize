package space.hypercode.core;

import space.hypercode.core.configs.MemoizationConfigs;
import space.hypercode.core.converters.ConverterResolver;
import space.hypercode.core.init.MemoizeInitializer;
import space.hypercode.core.metrics.MemoizationMetrics;
import space.hypercode.core.metrics.NoOpMetrics;
import space.hypercode.core.providers.MemoizationProviderFactory;
import space.hypercode.core.utils.Preconditions;


public class Memoize {

    private static volatile Memoize INSTANCE;

    private final String packageName;
    private final MemoizationConfigs configs;
    private final MemoizationProviderFactory providerFactory;
    private final MemoizationMetrics metrics;
    private final ConverterResolver converterResolver;

    private Memoize(final String packageName,
                    final MemoizationConfigs configs,
                    final MemoizationProviderFactory providerFactory,
                    final MemoizationMetrics metrics) {

        this.packageName = Preconditions.validateNotNullOrEmpty(packageName, "packageName can't be null or empty");
        this.configs = configs == null ? new MemoizationConfigs() : configs;
        this.providerFactory = Preconditions.validateNonNull(providerFactory, "providerFactory can't be null");
        this.metrics = metrics == null ? new NoOpMetrics() : metrics;
        this.converterResolver = new ConverterResolver(this.configs);
    }

    public String getPackageName() {
        return packageName;
    }

    public MemoizationConfigs getConfigs() {
        return configs;
    }

    public MemoizationProviderFactory getProviderFactory() {
        return providerFactory;
    }

    public MemoizationMetrics getMetrics() {
        return metrics;
    }

    public ConverterResolver getConverterResolver() {
        return converterResolver;
    }

    /**
     * Initializes the singleton, runs validation, and makes memoization active.
     * Throws if already started.
     */
    public void start() {
        synchronized (Memoize.class) {
            if (INSTANCE != null) {
                throw new IllegalStateException("Memoize has already been started. Only one instance is allowed per JVM.");
            }
            INSTANCE = this;
        }
        new MemoizeInitializer(this).initialize();
    }

    /**
     * Returns the singleton instance, or null if not yet started.
     */
    public static Memoize getInstance() {
        return INSTANCE;
    }

    public static MemoizeBuilder create() {
        return new MemoizeBuilder();
    }

    public static class MemoizeBuilder {
        private String packageName;
        private MemoizationConfigs configs;
        private MemoizationProviderFactory providerFactory;
        private MemoizationMetrics metrics;

        private MemoizeBuilder() {
            // to prevent initialization
        }

        public MemoizeBuilder scanIn(final String packageName) {
            this.packageName = Preconditions.validateNotNullOrEmpty(packageName, "packageName can't be null or empty");
            return this;
        }

        public MemoizeBuilder configs(final MemoizationConfigs memoizationConfigs) {
            this.configs = Preconditions.validateNonNull(memoizationConfigs, "configs can't be null");
            return this;
        }

        public MemoizeBuilder providerFactory(final MemoizationProviderFactory providerFactory) {
            this.providerFactory = Preconditions.validateNonNull(providerFactory, "providerFactory can't be null");
            return this;
        }

        public MemoizeBuilder metrics(final MemoizationMetrics metrics) {
            this.metrics = metrics;
            return this;
        }

        public Memoize build() {
            return new Memoize(packageName, configs, providerFactory, metrics);
        }

    }
}
