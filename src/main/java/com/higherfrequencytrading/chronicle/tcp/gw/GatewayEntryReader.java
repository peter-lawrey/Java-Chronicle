package com.higherfrequencytrading.chronicle.tcp.gw;

import com.higherfrequencytrading.chronicle.Excerpt;

/**
 * @author peter.lawrey
 */
public abstract class GatewayEntryReader {
    private final Excerpt excerpt;
    private final boolean targetReader;

    public GatewayEntryReader(Excerpt excerpt, boolean targetReader) {
        this.excerpt = excerpt;
        this.targetReader = targetReader;
    }

    public boolean readEntry() {
        if (!excerpt.nextIndex()) return false;
        long writeTimeNS = excerpt.readLong();
        long writeTimeMS = excerpt.readLong();
        int pos = excerpt.position();
        long readTimeMS = excerpt.readLong();
        if (targetReader && readTimeMS == 0)
            excerpt.writeLong(pos, readTimeMS = System.nanoTime());
        int length = excerpt.readInt24();
        char type = (char) excerpt.readUnsignedByte();
        onEntry(writeTimeNS, writeTimeMS, readTimeMS, length, type, excerpt);
        return true;
    }

    protected abstract void onEntry(long writeTimeNS, long writeTimeMS, long readTimeMS, int length, char type, Excerpt excerpt);
}
