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
        long writeTimeMS = excerpt.readLong();
        long writeTimeNS = excerpt.readLong();
        int pos = excerpt.position();
        long readTimeNS = excerpt.readLong();
        if (targetReader && readTimeNS == 0)
            excerpt.writeLong(pos, readTimeNS = System.nanoTime());
        int length = excerpt.readInt24();
        char type = (char) excerpt.readUnsignedByte();
        onEntry(writeTimeMS, writeTimeNS, readTimeNS, length, type, excerpt);
        return true;
    }

    protected abstract void onEntry(long writeTimeNS, long writeTimeMS, long readTimeMS, int length, char type, Excerpt excerpt);
}
