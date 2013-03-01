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

package vanilla.java.processingengine.api;

import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.ExcerptMarshallable;
import com.higherfrequencytrading.clock.ClockSupport;

/**
 * @author peter.lawrey
 */
public class MetaData implements ExcerptMarshallable {
    boolean targetReader;

    public int sourceId;
    public long excerptId;
    public long writeTimestampMillis;
    public long writeTimestampNanos;
    public long readTimestampNanos;

    public MetaData(boolean targetReader) {
        this.targetReader = targetReader;
    }

    @Override
    public void readMarshallable(Excerpt in) throws IllegalStateException {
        sourceId = in.readInt();
        excerptId = in.readLong();
        readTimestamps(in);
    }

    @Override
    public void writeMarshallable(Excerpt out) {
        out.writeInt(sourceId);
        out.writeLong(excerptId);
        out.writeLong(writeTimestampMillis);
        out.writeLong(writeTimestampNanos);
        out.writeLong(readTimestampNanos);
    }

    public void readFromGateway(Excerpt in, int sourceId) throws IllegalStateException {
        this.sourceId = sourceId;
        excerptId = in.index();
        readTimestamps(in);
    }

    private void readTimestamps(Excerpt in) {
        writeTimestampMillis = in.readLong();
        writeTimestampNanos = in.readLong();
        readTimestampNanos = in.readLong();
        if (readTimestampNanos == 0 && targetReader)
            in.writeLong(in.position() - 8, ClockSupport.nanoTime());
    }

    public static void writeFromGateway(Excerpt out) {
        out.writeLong(-1L); //System.currentTimeMillis());
        out.writeLong(ClockSupport.nanoTime());
        out.writeLong(0L);
    }

    public void readFromEngine(Excerpt in, int sourceId) {
        this.sourceId = (int) in.readInt();
        excerptId = in.readLong();
        targetReader = sourceId == this.sourceId;
        readTimestamps(in);
    }
}
