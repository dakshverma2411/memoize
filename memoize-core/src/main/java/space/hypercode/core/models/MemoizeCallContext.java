package space.hypercode.core.models;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MemoizeCallContext {
    Object returnValue;
    Object[] args;
}
