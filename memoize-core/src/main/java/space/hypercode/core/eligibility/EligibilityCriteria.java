package space.hypercode.core.eligibility;

import space.hypercode.core.models.MemoizeCallContext;

public interface EligibilityCriteria {
    /**
     * if return true, memoize it
     */
    boolean shouldMemoize(final MemoizeCallContext context);
}
