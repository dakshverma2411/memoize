package space.hypercode.core.converters;

import space.hypercode.core.models.Memoizable;

/**
 * Internal converter that delegates key generation to {@link Memoizable#memoizationKey()}.
 * Used automatically when a method has a single argument implementing {@link Memoizable}
 * and no explicit converter is specified.
 */
public final class MemoizableKeyConverter implements SingleArgMemoizationKeyConverter<Memoizable> {

    @Override
    public String toKey(final Memoizable input) {
        if (input == null) {
            return null;
        }
        return input.memoizationKey();
    }
}
