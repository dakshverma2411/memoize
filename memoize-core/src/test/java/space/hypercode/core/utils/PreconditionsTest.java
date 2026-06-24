package space.hypercode.core.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PreconditionsTest {

    @Test
    void validateNotNullOrEmpty_validString_returnsString() {
        final String result = Preconditions.validateNotNullOrEmpty("hello", "error");
        assertEquals("hello", result);
    }

    @Test
    void validateNotNullOrEmpty_nullString_throws() {
        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Preconditions.validateNotNullOrEmpty(null, "value is required")
        );
        assertEquals("value is required", ex.getMessage());
    }

    @Test
    void validateNotNullOrEmpty_emptyString_throws() {
        final IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Preconditions.validateNotNullOrEmpty("", "cannot be empty")
        );
        assertEquals("cannot be empty", ex.getMessage());
    }
}
