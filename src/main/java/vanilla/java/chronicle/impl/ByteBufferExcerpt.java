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

import vanilla.java.chronicle.ByteString;
import vanilla.java.chronicle.Chronicle;
import vanilla.java.chronicle.Excerpt;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * @author peter.lawrey
 */
public class ByteBufferExcerpt<C extends DirectChronicle> implements Excerpt {
    protected static final int POSITION_BITS = 48;
    protected static final int TYPE_BITS = 16;
    protected static final long POSITION_MASK = (1L << POSITION_BITS) - 1;
    protected static final long TYPE_MASK = (1L << TYPE_BITS) - 1;

    protected final C chronicle;
    protected long index;
    private int start = 0;
    private int position = 0;
    protected short type = 0;
    private int capacity = 0;
    private int limit = 0;

    protected long endPosition;
    protected long startPosition;
    protected ByteBuffer buffer;


    protected ByteBufferExcerpt(C chronicle) {
        this.chronicle = chronicle;
    }

    @Override
    public Chronicle chronicle() {
        return chronicle;
    }

    @Override
    public boolean index(long index) throws IndexOutOfBoundsException {
        long endPosition = chronicle.getIndexData(index + 1);
        if (endPosition == 0) {
            buffer = null;
            return false;
        }
        long indexData = chronicle.getIndexData(index);
        long startPosition = indexData & POSITION_MASK;
        int capacity = (int) (endPosition - startPosition);
        type = (short) (indexData >>> POSITION_BITS);
        endPosition &= POSITION_MASK;
        index0(index, indexData, capacity, startPosition, endPosition);
        return true;
    }

    @Override
    public void startExcerpt(short type, int capacity) {
        long indexData = chronicle.startExcerpt(capacity);
        this.type = type;
        long startPosition = indexData & POSITION_MASK;
        long endPosition = startPosition + capacity;
        index0(chronicle.size(), indexData, capacity, startPosition, endPosition);
    }


    private void index0(long index, long indexData, int capacity, long startPosition, long endPosition) {
        this.index = index;
        this.capacity = capacity;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        buffer = chronicle.acquireDataBuffer(startPosition);
        start = position = chronicle.positionInBuffer(startPosition);
        limit = chronicle.positionInBuffer(endPosition - 1) + 1;
    }

    @Override
    public void finish() {
        if (position > limit)
            throw new IllegalStateException("Capacity allowed: " + capacity + " data read/written: " + position);
        if (index == chronicle.size()) {
            chronicle.setIndexData(index, ((long) type << POSITION_BITS) | startPosition);
            chronicle.setIndexData(index + 1, startPosition + (position - start));
            chronicle.incrSize();
        }
    }

    @Override
    public void type(short type) {
        if (type != this.type)
            chronicle.setIndexData(index, ((long) type << POSITION_BITS) | startPosition);
        this.type = type;
    }

    @Override
    public long index() {
        return index;
    }

    @Override
    public Excerpt position(int position) {
        if (position < 0 || position >= capacity()) throw new IndexOutOfBoundsException();
        this.position = position;
        return this;
    }

    @Override
    public int position() {
        return position;
    }

    @Override
    public short type() {
        return type;
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public void readFully(byte[] b) {
        readFully(b, 0, b.length);
    }

    // RandomDataInput

    @Override
    public int skipBytes(int n) {
        int position = position();
        int n2 = Math.min(n, capacity - position);
        position(position + n2);
        return n2;
    }

    @Override
    public void readFully(byte[] b, int off, int len) {
        while (len-- > 0)
            b[off++] = readByte();
    }


    @Override
    public boolean readBoolean() {
        return readByte() != 0;
    }

    @Override
    public boolean readBoolean(int offset) {
        return readByte(offset) != 0;
    }

    @Override
    public int readUnsignedByte() {
        return readByte() & 0xFF;
    }

    @Override
    public int readUnsignedByte(int offset) {
        return readByte(offset) & 0xFF;
    }

    @Override
    public int readUnsignedShort() {
        return readShort() & 0xFF;
    }

    @Override
    public int readUnsignedShort(int offset) {
        return readShort(offset) & 0xFF;
    }

    @Override
    public String readLine() {
        StringBuilder input = new StringBuilder();
        EOL:
        while (position() < capacity()) {
            int c = readUnsignedByte();
            switch (c) {
                case '\n':
                    break EOL;
                case '\r':
                    int cur = position();
                    if (cur < capacity() && readByte(cur) == '\n')
                        position(cur + 1);
                    break EOL;
                default:
                    input.append((char) c);
                    break;
            }
        }
        return input.toString();
    }

    @Override
    public String readUTF() {
        try {
            return DataInputStream.readUTF(this);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public String readUTF(int offset) {
        int oldPosition = position;
        position = offset;
        try {
            return readUTF();
        } finally {
            position = oldPosition;
        }
    }

    @Override
    public byte readByte() {
        return buffer.get(position++);
    }

    @Override
    public byte readByte(int offset) {
        return buffer.get(start + offset);
    }

    @Override
    public short readShort() {
        short s = buffer.getShort(position);
        position += 2;
        return s;
    }

    @Override
    public short readShort(int offset) {
        return buffer.getShort(start + offset);
    }

    @Override
    public char readChar() {
        char ch = buffer.getChar(position);
        position += 2;
        return ch;
    }

    @Override
    public char readChar(int offset) {
        return buffer.getChar(start + offset);
    }

    @Override
    public int readInt() {
        int i = buffer.getInt();
        position += 4;
        return i;
    }

    @Override
    public int readInt(int offset) {
        return buffer.getInt(start + offset);
    }

    @Override
    public long readLong() {
        long l = buffer.getLong(position);
        position += 8;
        return l;
    }

    @Override
    public long readLong(int offset) {
        return buffer.getLong(start + offset);
    }

    @Override
    public float readFloat() {
        float f = buffer.getFloat();
        position += 4;
        return f;
    }

    @Override
    public float readFloat(int offset) {
        return buffer.getFloat(start + offset);
    }

    @Override
    public double readDouble() {
        double d = buffer.getDouble(position);
        position += 8;
        return d;
    }

    @Override
    public double readDouble(int offset) {
        return buffer.getDouble(start + offset);
    }

    @Override
    public void readByteString(ByteString as) {
        as.clear();
        int len = readByte() & 0xFF;
        for (int i = 0; i < len; i++)
            as.append(readByte());
    }

    @Override
    public int readByteString(int offset, ByteString as) {
        as.clear();
        int len = readByte(offset) & 0xFF;
        for (int i = 1; i <= len; i++)
            as.append(readByte(offset + i));
        return offset + len + 1;
    }

    @Override
    public void readByteString(StringBuilder sb) {
        sb.setLength(0);
        int len = readByte() & 0xFF;
        for (int i = 0; i < len; i++)
            sb.append(readByte());
    }

    @Override
    public int readByteString(int offset, StringBuilder sb) {
        sb.setLength(0);
        int len = readByte(offset) & 0xFF;
        for (int i = 1; i <= len; i++)
            sb.append(readByte(offset + i));
        return offset + len + 1;
    }

    @Override
    public String readByteString() {
        int len = readByte() & 0xFF;
        if (len == 0) return "";
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++)
            bytes[i] = readByte();
        return new String(bytes, 0);
    }

    @Override
    public void readChars(StringBuffer sb) {
        int len = readChar();
        sb.setLength(0);
        for (int i = 0; i < len; i++)
            sb.append(readChar());
    }

    @Override
    public String readChars() {
        int len = readChar();
        if (len == 0) return "";
        char[] chars = new char[len];
        for (int i = 0; i < len; i++)
            chars[i] = readChar();
        return new String(chars);
    }

    //// RandomOutputStream
    @Override
    public void write(byte[] b) {
        write(b, 0, b.length);
    }

    @Override
    public void writeBoolean(boolean v) {
        write(v ? 0 : -1);
    }

    @Override
    public void writeBoolean(int offset, boolean v) {
        write(offset, v ? 0 : -1);
    }

    @Override
    public void writeBytes(String s) {
        writeBytes((CharSequence) s);
    }

    @Override
    public void writeBytes(CharSequence s) {
        int len = s.length();
        if (len > 255) throw new IllegalArgumentException("Len cannot be " + len + " > 255");
        write(len);
        for (int i = 0; i < len; i++)
            write(s.charAt(i));
    }

    @Override
    public void writeBytes(int offset, CharSequence s) {
        int len = s.length();
        if (len > 255) throw new IllegalArgumentException("Len cannot be " + len + " > 255");
        write(offset, len);
        for (int i = 0; i < len; i++)
            write(s.charAt(i));
        for (int i = 0; i < len; i++)
            write(offset + 1 + i, s.charAt(i));
    }

    @Override
    public void writeChars(String s) {
        writeChars((CharSequence) s);
    }

    @Override
    public void writeChars(CharSequence s) {
        int len = s.length();
        if (len > 65535) throw new IllegalArgumentException("Len cannot be " + len + " > 65535");
        writeChar(len);
        for (int i = 0; i < len; i++)
            writeChar(s.charAt(i));
    }

    @Override
    public void writeChars(int offset, CharSequence s) {
        int len = s.length();
        if (len > 65535) throw new IllegalArgumentException("Len cannot be " + len + " > 65535");
        writeChar(offset + len);
        for (int i = 0; i < len; i++)
            writeChar(offset + 2 + i, s.charAt(i));
    }

    static final Method writeUTFMethod;

    static {
        try {
            writeUTFMethod = DataOutputStream.class.getDeclaredMethod("writeUTF", String.class, DataOutput.class);
            writeUTFMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void writeUTF(String s) {
        try {
            writeUTFMethod.invoke(null, s, this);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            // rethrow the orginal exception.
            Thread.currentThread().stop(e.getCause());
        }
    }

    @Override
    public void write(int b) {
        buffer.put(position++, (byte) b);
    }

    @Override
    public void writeByte(int v) {
        write(v);
    }

    @Override
    public void write(int offset, int b) {
        buffer.put(start + offset, (byte) b);
    }

    @Override
    public void write(int offset, byte[] b) {
        for (int i = 0; i < b.length; i++)
            write(offset + i, b[i]);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        for (int i = 0; i < len; i++)
            write(b[off + i]);
    }

    @Override
    public void writeShort(int v) {
        buffer.putShort(position, (short) v);
        position += 2;
    }

    @Override
    public void writeShort(int offset, int v) {
        buffer.putShort(start + offset, (short) v);
    }

    @Override
    public void writeChar(int v) {
        buffer.putChar(position, (char) v);
        position += 2;
    }

    @Override
    public void writeChar(int offset, int v) {
        buffer.putChar(start + offset, (char) v);
    }

    @Override
    public void writeInt(int v) {
        buffer.putInt(position, v);
        position += 4;
    }

    @Override
    public void writeInt(int offset, int v) {
        buffer.putInt(start + offset, v);
    }

    @Override
    public void writeLong(long v) {
        buffer.putLong(position, v);
        position += 8;
    }

    @Override
    public void writeLong(int offset, long v) {
        buffer.putLong(start + offset, v);
    }

    @Override
    public void writeFloat(float v) {
        buffer.putFloat(position, v);
        position += 4;
    }

    @Override
    public void writeFloat(int offset, float v) {
        buffer.putFloat(start + offset, v);
    }

    @Override
    public void writeDouble(double v) {
        buffer.putDouble(position, v);
        position += 8;
    }

    @Override
    public void writeDouble(int offset, double v) {
        buffer.putDouble(start + offset, v);
    }
}
