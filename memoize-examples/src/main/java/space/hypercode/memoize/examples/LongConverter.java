package space.hypercode.memoize.examples;

import space.hypercode.core.converters.SingleArgMemoizationKeyConverter;

public class LongConverter implements SingleArgMemoizationKeyConverter<Long> {
    @Override
    public String toKey(Long input) {
        return input.toString();
    }
}
