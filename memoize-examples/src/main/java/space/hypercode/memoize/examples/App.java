package space.hypercode.memoize.examples;

import space.hypercode.core.annotations.MemoizeThis;

import java.time.Duration;

public class App {

    public App() {
        // nothing to set up
    }

    @MemoizeThis(ttlInMs = 10000L, size = 10, cacheNulls = false, converter = LongConverter.class)
    public long square(final Long x) {
        tryWait(Duration.ofMillis(2000));
        return x * x;
    }

    @MemoizeThis
    public MyLong square(final MyLong myLong) {
        tryWait(Duration.ofMillis(2000));
        return new MyLong(myLong.getValue() * myLong.getValue());
    }

    private void tryWait(final Duration time) {
        try {
            Thread.sleep(time.toMillis());
        } catch (InterruptedException e) {
            // do nothing
        }

    }


}
