package space.hypercode.core.converters;

public interface SingleArgMemoizationKeyConverter<T> extends MemoizationKeyConverter {
    String toKey(T input);
}