/*
 * Copyright 2013 Peter Lawrey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.higherfrequencytrading.chronicle.impl;

import sun.misc.Cleaner;
import sun.nio.ch.DirectBuffer;

import java.nio.MappedByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class MappedMemory {
    private final MappedByteBuffer buffer;
    private final long index;
    private final AtomicInteger refCount = new AtomicInteger(1);
    private volatile boolean unmapped = false;

    public MappedMemory(MappedByteBuffer buffer, long index) {
        this.buffer = buffer;
        this.index = index;
    }

    private static void unmap(MappedByteBuffer bb) {
        Cleaner cl = ((DirectBuffer) bb).cleaner();
        if (cl != null)
            cl.clean();
    }

    public long index() {
        return index;
    }

    public void reserve() {
        if (unmapped) throw new IllegalStateException();
        refCount.incrementAndGet();
    }

    public void release() {
        if (unmapped) throw new IllegalStateException();
        if (refCount.decrementAndGet() > 0) return;
        close();
    }

    public MappedByteBuffer buffer() {
        return buffer;
    }

    public long address() {
        return ((DirectBuffer) buffer).address();
    }

    public int refCount() {
        return refCount.get();
    }

    public static void release(MappedMemory mapmem) {
        if (mapmem != null)
            mapmem.release();
    }

    public void force() {
        if (!unmapped)
            buffer.force();
    }

    public void close() {
        unmap(buffer);
        unmapped = true;
    }
}
