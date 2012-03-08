/*
 * Copyright 2011 Peter Lawrey
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

package vanilla.java.chronicle.impl;

/**
 * @author peter.lawrey
 */
public class ByteBufferExcerpt<C extends DirectChronicle> extends AbstractExcerpt<C> {
    protected ByteBufferExcerpt(C chronicle) {
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
        return buffer.get((int) position++);
    }

    @Override
    public byte readByte(int offset) {
        return buffer.get((int) (start + offset));
    }

    @Override
    public short readShort() {
        short s = buffer.getShort((int) position);
        position += 2;
        return s;
    }

    @Override
    public short readShort(int offset) {
        return buffer.getShort((int) (start + offset));
    }

    @Override
    public char readChar() {
        char ch = buffer.getChar((int) position);
        position += 2;
        return ch;
    }

    @Override
    public char readChar(int offset) {
        return buffer.getChar((int) (start + offset));
    }

    @Override
    public int readInt() {
        int i = buffer.getInt((int) position);
        position += 4;
        return i;
    }

    @Override
    public int readInt(int offset) {
        return buffer.getInt((int) (start + offset));
    }

    @Override
    public long readLong() {
        long l = buffer.getLong((int) position);
        position += 8;
        return l;
    }

    @Override
    public long readLong(int offset) {
        return buffer.getLong((int) (start + offset));
    }

    @Override
    public float readFloat() {
        float f = buffer.getFloat((int) position);
        position += 4;
        return f;
    }

    @Override
    public float readFloat(int offset) {
        return buffer.getFloat((int) (start + offset));
    }

    @Override
    public double readDouble() {
        double d = buffer.getDouble((int) position);
        position += 8;
        return d;
    }

    @Override
    public double readDouble(int offset) {
        return buffer.getDouble((int) (start + offset));
    }

    @Override
    public void write(int b) {
        buffer.put((int) position++, (byte) b);
    }

    @Override
    public void write(int offset, int b) {
        buffer.put((int) (start + offset), (byte) b);
    }

    @Override
    public void writeShort(int v) {
        buffer.putShort((int) position, (short) v);
        position += 2;
    }

    @Override
    public void writeShort(int offset, int v) {
        buffer.putShort((int) (start + offset), (short) v);
    }

    @Override
    public void writeChar(int v) {
        buffer.putChar((int) position, (char) v);
        position += 2;
    }

    @Override
    public void writeChar(int offset, int v) {
        buffer.putChar((int) (start + offset), (char) v);
    }

    @Override
    public void writeInt(int v) {
        buffer.putInt((int) position, v);
        position += 4;
    }

    @Override
    public void writeInt(int offset, int v) {
        buffer.putInt((int) (start + offset), v);
    }

    @Override
    public void writeLong(long v) {
        buffer.putLong((int) position, v);
        position += 8;
    }

    @Override
    public void writeLong(int offset, long v) {
        buffer.putLong((int) (start + offset), v);
    }

    @Override
    public void writeFloat(float v) {
        buffer.putFloat((int) position, v);
        position += 4;
    }

    @Override
    public void writeFloat(int offset, float v) {
        buffer.putFloat((int) (start + offset), v);
    }

    @Override
    public void writeDouble(double v) {
        buffer.putDouble((int) position, v);
        position += 8;
    }

    @Override
    public void writeDouble(int offset, double v) {
        buffer.putDouble((int) (start + offset), v);
    }
}
