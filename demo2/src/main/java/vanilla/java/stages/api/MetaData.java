/*
 * Copyright 2013 Peter Lawrey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        if (readStage != null && timings[readStage.ordinal()] == 0) {
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
