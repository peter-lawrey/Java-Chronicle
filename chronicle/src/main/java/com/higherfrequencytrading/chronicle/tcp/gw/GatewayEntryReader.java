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
