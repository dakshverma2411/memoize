package space.hypercode.core.eligibility;

import space.hypercode.core.models.MemoizeCallContext;

public final class MemoizeAlways implements EligibilityCriteria {

    private static final MemoizeAlways INSTANCE = new MemoizeAlways();

    public static MemoizeAlways getInstance() {
        return INSTANCE;
    }

    private MemoizeAlways() {
        // to avoid instantiation
    }

    @Override
    public boolean shouldMemoize(final MemoizeCallContext context) {
        return true;
    }
}
