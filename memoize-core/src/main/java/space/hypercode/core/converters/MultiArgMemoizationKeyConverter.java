package space.hypercode.core.converters;

public interface MultiArgMemoizationKeyConverter extends MemoizationKeyConverter {
    String toKey(Object ... args);
}
