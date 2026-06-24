package space.hypercode.core.converters;

import org.junit.jupiter.api.Test;
import space.hypercode.core.models.Memoizable;

import static org.junit.jupiter.api.Assertions.*;

class MemoizableKeyConverterTest {

    private final MemoizableKeyConverter converter = new MemoizableKeyConverter();

    @Test
    void toKey_validMemoizable_returnsMemoizationKey() {
        final Memoizable memoizable = () -> "my-unique-key";

        final String key = converter.toKey(memoizable);

        assertEquals("my-unique-key", key);
    }

    @Test
    void toKey_nullInput_returnsNull() {
        final String key = converter.toKey(null);

        assertNull(key);
    }

    @Test
    void toKey_memoizableReturnsNull_returnsNull() {
        final Memoizable memoizable = () -> null;

        final String key = converter.toKey(memoizable);

        assertNull(key);
    }

    @Test
    void toKey_memoizableReturnsEmpty_returnsEmpty() {
        final Memoizable memoizable = () -> "";

        final String key = converter.toKey(memoizable);

        assertEquals("", key);
    }
}
