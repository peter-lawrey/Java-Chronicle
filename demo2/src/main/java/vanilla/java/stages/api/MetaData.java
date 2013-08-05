package vanilla.java.stages.api;

import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.ExcerptMarshallable;

/**
 * User: peter
 * Date: 05/08/13
 * Time: 17:34
 */
public class MetaData implements ExcerptMarshallable {
    private final TimingStage readStage;
    private final TimingStage writeStage;

    private long startMS;
    private int count = 0;
    private final long[] timings = new long[TimingStage.VALUES.length];

    public MetaData(TimingStage readStage, TimingStage writeStage) {
        this.writeStage = writeStage;
        this.readStage = readStage;
    }

    public void startTiming() {
        startMS = System.currentTimeMillis();
        count = 1;
        setTimeStamp(TimingStage.Start);
    }

    private long setTimeStamp(TimingStage timingStage) {
        int ordinal = timingStage.ordinal();
        long now = timings[ordinal] = System.nanoTime();
        if (count <= ordinal)
            count = ordinal + 1;
        return now;
    }

    private void padTimeStampAfter(TimingStage timingStage) {
        int ordinal = timingStage.ordinal() + 1;
        timings[ordinal] = 0;
        if (count <= ordinal)
            count = ordinal + 1;
    }

    public long getTimeStamp(TimingStage timingStage) {
        int ordinal = timingStage.ordinal();
        if (ordinal >= count) return Long.MIN_VALUE;
        return timings[ordinal];
    }


    @Override
    public void readMarshallable(Excerpt in) throws IllegalStateException {
        startMS = in.readLong();
        count = (int) in.readStopBit();
        for (int i = 0; i < count; i++)
            timings[i] = in.readLong();
        if (readStage != null && timings[readStage.ordinal()] != 0) {
            long now = setTimeStamp(readStage);
            in.writeLong(in.position() - 8, now);
        }
    }

    @Override
    public void writeMarshallable(Excerpt out) {
        setTimeStamp(writeStage);
        padTimeStampAfter(writeStage);

        out.writeLong(startMS);
        out.writeStopBit(count);
        for (int i = 0; i < count; i++)
            out.writeLong(timings[i]);
    }
}
