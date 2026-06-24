package space.hypercode.core.configs;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MemoizationConfigs {
    private final Map<String, MemoizationConfig> configs;

    public MemoizationConfigs() {
        this.configs = new ConcurrentHashMap<>();
    }

    public void add(final String name, final MemoizationConfig memoizationConfig) {
        this.configs.put(name, memoizationConfig);
    }

    public Optional<MemoizationConfig> get(final String name) {
        return Optional.ofNullable(
                configs.get(name)
        );
    }
}
