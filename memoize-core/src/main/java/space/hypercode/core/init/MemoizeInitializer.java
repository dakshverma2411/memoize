package space.hypercode.core.init;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;
import space.hypercode.core.Memoize;
import space.hypercode.core.annotations.MemoizeThis;
import space.hypercode.core.configs.MemoizationConfig;
import space.hypercode.core.configs.MemoizationConfigs;
import space.hypercode.core.converters.MemoizationKeyConverter;
import space.hypercode.core.models.Memoizable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Performs startup validation by scanning for {@code @Memoize}-annotated methods
 * and verifying that all required configuration is in place.
 */
public class MemoizeInitializer {

    private static final Logger LOG = Logger.getLogger(MemoizeInitializer.class.getName());

    private final Memoize memoize;

    public MemoizeInitializer(final Memoize memoize) {
        this.memoize = memoize;
    }

    /**
     * Scans the configured package for @Memoize-annotated methods and validates them.
     * Fails fast on hard errors, logs warnings for suspicious patterns.
     */
    public void initialize() {
        final String packageName = memoize.getPackageName();
        final MemoizationConfigs configs = memoize.getConfigs();

        LOG.info("Memoize: Scanning package '" + packageName + "' for @Memoize annotations...");

        final List<Method> annotatedMethods = scanForAnnotatedMethods(packageName);

        LOG.info("Memoize: Found " + annotatedMethods.size() + " @Memoize-annotated method(s).");

        final List<String> errors = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();

        for (final Method method : annotatedMethods) {
            validateMethod(method, configs, errors, warnings);
        }

        // Log warnings
        for (final String warning : warnings) {
            LOG.warning("Memoize: " + warning);
        }

        // Fail fast on errors
        if (!errors.isEmpty()) {
            final String errorMessage = "Memoize initialization failed with " + errors.size() + " error(s):\n  - "
                    + String.join("\n  - ", errors);
            throw new IllegalStateException(errorMessage);
        }

        LOG.info("Memoize: Initialization complete. Memoization is now active.");
    }

    private List<Method> scanForAnnotatedMethods(final String packageName) {
        final List<Method> methods = new ArrayList<>();

        try (final ScanResult scanResult = new ClassGraph()
                .acceptPackages(packageName)
                .enableClassInfo()
                .enableMethodInfo()
                .enableAnnotationInfo()
                .scan()) {

            for (final ClassInfo classInfo : scanResult.getAllClasses()) {
                for (final MethodInfo methodInfo : classInfo.getMethodInfo()) {
                    if (methodInfo.hasAnnotation(MemoizeThis.class)) {
                        try {
                            final Method method = methodInfo.loadClassAndGetMethod();
                            methods.add(method);
                        } catch (final Exception e) {
                            LOG.warning("Memoize: Failed to load method " + classInfo.getName()
                                    + "." + methodInfo.getName() + ": " + e.getMessage());
                        }
                    }
                }
            }
        }

        return methods;
    }

    private void validateMethod(final Method method,
                                final MemoizationConfigs configs,
                                final List<String> errors,
                                final List<String> warnings) {

        final MemoizeThis annotation =
                method.getAnnotation(MemoizeThis.class);
        final String methodRef = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        // Warn: void return type
        if (method.getReturnType() == void.class || method.getReturnType() == Void.class) {
            warnings.add(methodRef + ": Annotated with @Memoize but returns void. Caching has no effect.");
        }

        // Validate useConfig path
        if (annotation.useConfig()) {
            final String memoizationName = resolvememoizationName(annotation, method);
            final Optional<MemoizationConfig> config = configs.get(memoizationName);

            if (config.isEmpty()) {
                errors.add(methodRef + ": useConfig=true but no config registered for cache name '" + memoizationName + "'.");
            } else if (config.get().getConverter() == null) {
                errors.add(methodRef + ": useConfig=true but config for '" + memoizationName + "' has no converter.");
            }
            return;
        }

        // Validate explicit converter in annotation
        if (hasExplicitConverter(annotation)) {
            validateConverterClass(annotation.converter(), methodRef, errors);
            return;
        }

        // Check if single Memoizable arg (auto-resolution possible)
        if (hasSingleMemoizableArg(method)) {
            return; // MemoizableKeyConverter will handle it
        }

        // No converter resolvable — warn (will passthrough at runtime)
        warnings.add(methodRef + ": No converter resolvable. Method has "
                + method.getParameterCount() + " arg(s), none implement Memoizable. "
                + "Caching will be skipped at runtime.");
    }

    private void validateConverterClass(final Class<? extends MemoizationKeyConverter> converterClass,
                                        final String methodRef,
                                        final List<String> errors) {
        try {
            converterClass.getDeclaredConstructor();
        } catch (final NoSuchMethodException e) {
            errors.add(methodRef + ": Converter " + converterClass.getName()
                    + " does not have a public no-arg constructor.");
        }
    }

    private boolean hasExplicitConverter(final MemoizeThis annotation) {
        return annotation.converter() != MemoizationKeyConverter.class;
    }

    private boolean hasSingleMemoizableArg(final Method method) {
        final Class<?>[] paramTypes = method.getParameterTypes();
        return paramTypes.length == 1 && Memoizable.class.isAssignableFrom(paramTypes[0]);
    }

    private String resolvememoizationName(final MemoizeThis annotation, final Method method) {
        if (annotation.name() != null && !annotation.name().isEmpty()) {
            return annotation.name();
        }
        final StringBuilder name = new StringBuilder();
        name.append(method.getDeclaringClass().getName())
                .append(".")
                .append(method.getName())
                .append("(");
        final Class<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) {
                name.append(",");
            }
            name.append(paramTypes[i].getName());
        }
        name.append(")");
        return name.toString();
    }
}
