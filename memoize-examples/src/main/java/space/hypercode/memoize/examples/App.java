package space.hypercode.memoize.examples;

import space.hypercode.core.annotations.MemoizeThis;
import space.hypercode.core.eligibility.MemoizeNonNulls;

import java.time.Duration;

public class App {

    public App() {
        // nothing to set up
    }

    @MemoizeThis(ttlInMs = 10000L, size = 10, converter = LongConverter.class)
    public long square(final Long x) {
        tryWait(Duration.ofMillis(2000));
        return x * x;
    }

    @MemoizeThis
    public MyLong square(final MyLong myLong) {
        tryWait(Duration.ofMillis(2000));
        return new MyLong(myLong.getValue() * myLong.getValue());
    }

    @MemoizeThis(ttlInMs = 1L, size = 10)
    public MyLong cube(final MyLong myLong) {
        tryWait(Duration.ofMillis(2000));
        return new MyLong(myLong.getValue() * myLong.getValue() * myLong.getValue());
    }

    @MemoizeThis(ttlInMs = 10000, size = 10, criteria = MemoizeNonNulls.class)
    public MyLong evenCube(final MyLong myLong) {
        tryWait(Duration.ofMillis(2000));
        long result = myLong.getValue() * myLong.getValue() * myLong.getValue();
        if(result % 2 == 0) {
            return new MyLong(result);
        }
        // return null for odd values;
        return null;
    }

    private void tryWait(final Duration time) {
        try {
            Thread.sleep(time.toMillis());
        } catch (InterruptedException e) {
            // do nothing
        }

    }


}
