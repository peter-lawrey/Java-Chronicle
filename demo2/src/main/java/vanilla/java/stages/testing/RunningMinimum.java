package vanilla.java.stages.testing;

/**
 * User: peter
 * Date: 05/08/13
 * Time: 19:06
 */
public class RunningMinimum implements Differencer {
    private final long actualMinimum;
    private final int drift;

    private long lastStartTime = Long.MIN_VALUE;
    private long minimum = Long.MAX_VALUE;

    public RunningMinimum(long actualMinimum) {
        this(actualMinimum, 100 * 1000);
    }

    public RunningMinimum(long actualMinimum, int drift) {
        this.actualMinimum = actualMinimum;
        this.drift = drift;
    }

    @Override
    public long sample(long startTime, long endTime) {
        if (lastStartTime + drift <= startTime) {
            if (lastStartTime != Long.MIN_VALUE)
                minimum += (startTime - lastStartTime) / drift;
            lastStartTime = startTime;
        }
        long delta = endTime - startTime;
        if (minimum > delta)
            minimum = delta;
        return delta - minimum + actualMinimum;
    }

    public long minimum() {
        return minimum;
    }
}
