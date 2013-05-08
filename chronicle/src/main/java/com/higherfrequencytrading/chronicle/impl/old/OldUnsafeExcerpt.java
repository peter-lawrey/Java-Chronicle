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

package com.higherfrequencytrading.chronicle.impl.old;

import com.higherfrequencytrading.chronicle.impl.DirectChronicle;
import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.lang.reflect.Field;

/**
 * @author peter.lawrey
 */
public class OldUnsafeExcerpt extends OldAbstractExcerpt {

    protected OldUnsafeExcerpt(DirectChronicle chronicle) {
        super(chronicle);
    }

    protected void index0(long index, long startPosition, long endPosition) {
        this.index = index;
        this.startPosition = startPosition;

        buffer = chronicle.acquireDataBuffer(startPosition);

        long address = ((DirectBuffer) buffer).address();
        start = position = address + chronicle.positionInBuffer(startPosition);
        limit = address + chronicle.positionInBuffer(endPosition - 1) + 1;

        assert limit > start && position < limit && endPosition > startPosition;
    }

    // RandomDataInput

    @Override
    public byte readByte() {
        return UNSAFE.getByte(position++);
    }

    @Override
    public byte readByte(int offset) {
        return UNSAFE.getByte(start + offset);
    }

    @Override
    public void readFully(byte[] b, int off, int len) {
        UNSAFE.copyMemory(null, position, b, BYTES_OFFSET + off, len);
        position += len;
    }

    @Override
    public short readShort() {
        short s = UNSAFE.getShort(position);
        position += 2;
        return s;
    }

    @Override
    public short readShort(int offset) {
        return UNSAFE.getShort(start + offset);
    }

    @Override
    public char readChar() {
        char ch = UNSAFE.getChar(position);
        position += 2;
        return ch;
    }

    @Override
    public char readChar(int offset) {
        return UNSAFE.getChar(start + offset);
    }

    @Override
    public int readInt() {
        int i = UNSAFE.getInt(position);
        position += 4;
        return i;
    }

    @Override
    public int readInt(int offset) {
        return UNSAFE.getInt(start + offset);
    }

    @Override
    public long readLong() {
        long l = UNSAFE.getLong(position);
        position += 8;
        return l;
    }

    @Override
    public long readLong(int offset) {
        return UNSAFE.getLong(start + offset);
    }

    @Override
    public float readFloat() {
        float f = UNSAFE.getFloat(position);
        position += 4;
        return f;
    }

    @Override
    public float readFloat(int offset) {
        return UNSAFE.getFloat(start + offset);
    }

    @Override
    public double readDouble() {
        double d = UNSAFE.getDouble(position);
        position += 8;
        return d;
    }

    @Override
    public double readDouble(int offset) {
        return UNSAFE.getDouble(start + offset);
    }

    @Override
    public void write(int b) {
        UNSAFE.putByte(position++, (byte) b);
    }

    @Override
    public void write(int offset, int b) {
        UNSAFE.putByte(start + offset, (byte) b);
    }

    @Override
    public void write(int offset, byte[] b) {
        UNSAFE.copyMemory(b, BYTES_OFFSET, null, position, b.length);
        position += b.length;
    }

    @Override
    public void write(byte[] b, int off, int len) {
        UNSAFE.copyMemory(b, BYTES_OFFSET + off, null, position, len);
        position += len;
    }

    @Override
    public void writeShort(int v) {
        UNSAFE.putShort(position, (short) v);
        position += 2;
    }

    @Override
    public void writeShort(int offset, int v) {
        UNSAFE.putShort(start + offset, (short) v);
    }

    @Override
    public void writeChar(int v) {
        UNSAFE.putChar(position, (char) v);
        position += 2;
    }

    @Override
    public void writeChar(int offset, int v) {
        UNSAFE.putChar(start + offset, (char) v);
    }

    @Override
    public void writeInt(int v) {
        UNSAFE.putInt(position, v);
        position += 4;
    }

    @Override
    public void writeInt(int offset, int v) {
        UNSAFE.putInt(start + offset, v);
    }

    @Override
    public void writeLong(long v) {
        UNSAFE.putLong(position, v);
        position += 8;
    }

    @Override
    public void writeLong(int offset, long v) {
        UNSAFE.putLong(start + offset, v);
    }

    @Override
    public void writeFloat(float v) {
        UNSAFE.putFloat(position, v);
        position += 4;
    }

    @Override
    public void writeFloat(int offset, float v) {
        UNSAFE.putFloat(start + offset, v);
    }

    @Override
    public void writeDouble(double v) {
        UNSAFE.putDouble(position, v);
        position += 8;
    }

    @Override
    public void writeDouble(int offset, double v) {
        UNSAFE.putDouble(start + offset, v);
    }

    @Override
    public int read(byte[] b, int off, int len) {
        if (len < 0 || off < 0 || off + len > b.length)
            throw new IllegalArgumentException();
        if (len > remaining())
            len = remaining();
        UNSAFE.copyMemory(null, position, b, BYTES_OFFSET + off, len);
        position += len;
        return len;
    }

    /**
     * *** Access the Unsafe class *****
     */
    @SuppressWarnings("ALL")
    private static final Unsafe UNSAFE;
    private static final int BYTES_OFFSET;

    static {
        try {
            @SuppressWarnings("ALL")
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
            BYTES_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
