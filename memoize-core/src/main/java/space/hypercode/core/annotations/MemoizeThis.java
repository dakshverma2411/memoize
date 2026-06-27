package space.hypercode.core.annotations;

import space.hypercode.core.converters.MemoizationKeyConverter;
import space.hypercode.core.eligibility.EligibilityCriteria;
import space.hypercode.core.eligibility.MemoizeAlways;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MemoizeThis {
    boolean useConfig() default false;

    String name() default "";

    long ttlInMs() default 0L;

    long size() default 0L;

    Class<? extends MemoizationKeyConverter> converter() default MemoizationKeyConverter.class;

    Class<? extends EligibilityCriteria> criteria() default MemoizeAlways.class;
}
