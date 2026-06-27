package space.hypercode.core.eligibility;

import space.hypercode.core.models.MemoizeCallContext;

public final class MemoizeNonNulls implements EligibilityCriteria {

    private static final MemoizeNonNulls INSTANCE = new MemoizeNonNulls();

    public static MemoizeNonNulls getInstance() {
        return INSTANCE;
    }

    private MemoizeNonNulls() {
        // to avoid instantiation
    }

    /**
     * non values are memoized
     */
    @Override
    public boolean shouldMemoize(final MemoizeCallContext context) {
        return context.getReturnValue() != null;
    }
}
