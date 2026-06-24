package space.hypercode.memoize.examples;

import lombok.Builder;
import lombok.Value;
import space.hypercode.core.models.Memoizable;


@Value
@Builder
public class MyLong implements Memoizable {

    Long value;

    @Override
    public String memoizationKey() {
        return value.toString();
    }
}
