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

package com.higherfrequencytrading.chronicle.impl;

import org.jetbrains.annotations.NotNull;

/**
 * @author peter.lawrey
 */
public class ByteBufferExcerpt extends AbstractExcerpt {
    protected ByteBufferExcerpt(DirectChronicle chronicle) {
        super(chronicle);
    }

    protected void index0(long index, long startPosition, long endPosition) {
        this.index = index;
        this.startPosition = startPosition;

        buffer = chronicle.acquireDataBuffer(startPosition);

        start = position = chronicle.positionInBuffer(startPosition);
        limit = chronicle.positionInBuffer(endPosition - 1) + 1;

        assert limit > start && position < limit && endPosition > startPosition;
    }

    // RandomDataInput

    @Override
    public byte readByte() {
        assert buffer != null;
        return buffer.get((int) position++);
    }

    @Override
    public byte readByte(int offset) {
        assert buffer != null;
        return buffer.get((int) (start + offset));
    }

    @Override
    public short readShort() {
        assert buffer != null;
        short s = buffer.getShort((int) position);
        position += 2;
        return s;
    }

    @Override
    public short readShort(int offset) {
        assert buffer != null;
        return buffer.getShort((int) (start + offset));
    }

    @Override
    public char readChar() {
        assert buffer != null;
        char ch = buffer.getChar((int) position);
        position += 2;
        return ch;
    }

    @Override
    public char readChar(int offset) {
        assert buffer != null;
        return buffer.getChar((int) (start + offset));
    }

    @Override
    public int readInt() {
        assert buffer != null;
        int i = buffer.getInt((int) position);
        position += 4;
        return i;
    }

    @Override
    public int readInt(int offset) {
        assert buffer != null;
        return buffer.getInt((int) (start + offset));
    }

    @Override
    public long readLong() {
        assert buffer != null;
        long l = buffer.getLong((int) position);
        position += 8;
        return l;
    }

    @Override
    public long readLong(int offset) {
        assert buffer != null;
        return buffer.getLong((int) (start + offset));
    }

    @Override
    public float readFloat() {
        assert buffer != null;
        float f = buffer.getFloat((int) position);
        position += 4;
        return f;
    }

    @Override
    public float readFloat(int offset) {
        assert buffer != null;
        return buffer.getFloat((int) (start + offset));
    }

    @Override
    public double readDouble() {
        assert buffer != null;
        double d = buffer.getDouble((int) position);
        position += 8;
        return d;
    }

    @Override
    public double readDouble(int offset) {
        assert buffer != null;
        return buffer.getDouble((int) (start + offset));
    }

    @Override
    public void write(int b) {
        assert buffer != null;
        buffer.put((int) position++, (byte) b);
    }

    @Override
    public void write(int offset, int b) {
        assert buffer != null;
        buffer.put((int) (start + offset), (byte) b);
    }

    @Override
    public void writeShort(int v) {
        assert buffer != null;
        buffer.putShort((int) position, (short) v);
        position += 2;
    }

    @Override
    public void writeShort(int offset, int v) {
        assert buffer != null;
        buffer.putShort((int) (start + offset), (short) v);
    }

    @Override
    public void writeChar(int v) {
        assert buffer != null;
        buffer.putChar((int) position, (char) v);
        position += 2;
    }

    @Override
    public void writeChar(int offset, int v) {
        assert buffer != null;
        buffer.putChar((int) (start + offset), (char) v);
    }

    @Override
    public void writeInt(int v) {
        assert buffer != null;
        buffer.putInt((int) position, v);
        position += 4;
    }

    @Override
    public void writeInt(int offset, int v) {
        assert buffer != null;
        buffer.putInt((int) (start + offset), v);
    }

    @Override
    public void writeLong(long v) {
        assert buffer != null;
        buffer.putLong((int) position, v);
        position += 8;
    }

    @Override
    public void writeLong(int offset, long v) {
        assert buffer != null;
        buffer.putLong((int) (start + offset), v);
    }

    @Override
    public void writeFloat(float v) {
        assert buffer != null;
        buffer.putFloat((int) position, v);
        position += 4;
    }

    @Override
    public void writeFloat(int offset, float v) {
        assert buffer != null;
        buffer.putFloat((int) (start + offset), v);
    }

    @Override
    public void writeDouble(double v) {
        assert buffer != null;
        buffer.putDouble((int) position, v);
        position += 8;
    }

    @Override
    public void writeDouble(int offset, double v) {
        assert buffer != null;
        buffer.putDouble((int) (start + offset), v);
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) {
        if (len > remaining())
            len = remaining();
        for (int i = 0; i < len; i++) {
            assert buffer != null;
            b[i + off] = buffer.get((int) (i + position));
        }
        return len;
    }
}
