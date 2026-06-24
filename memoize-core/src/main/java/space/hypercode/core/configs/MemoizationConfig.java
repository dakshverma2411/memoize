package space.hypercode.core.configs;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import space.hypercode.core.converters.MemoizationKeyConverter;

import java.time.Duration;

@Value
@Builder
public class MemoizationConfig {
    @NonNull
    Duration ttl;
    long maxSize;
    @NonNull
    MemoizationKeyConverter converter;
    @Builder.Default
    boolean cacheNulls = true;
}
