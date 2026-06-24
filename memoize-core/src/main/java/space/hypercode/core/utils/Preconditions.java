package space.hypercode.core.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Preconditions {
    public <T> T validateNonNull(final T obj, final String errorMsg) {
        if(obj == null) {
            throw new IllegalArgumentException(errorMsg);
        }
        return obj;
    }

    public static String validateNotNullOrEmpty(final String value, final String errorMsg) {
        if(value == null || value.isEmpty()) {
            throw new IllegalArgumentException(errorMsg);
        }
        return value;
    }
}
