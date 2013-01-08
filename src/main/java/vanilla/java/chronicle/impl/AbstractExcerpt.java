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

import vanilla.java.chronicle.*;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author peter.lawrey
 */
public abstract class AbstractExcerpt<C extends DirectChronicle> implements Excerpt<C> {
    protected final C chronicle;
    protected long index = -1;
    protected long start = 0;
    protected long position = 0;
    protected int capacity = 0;
    protected long limit = 0;

    protected long startPosition;

    protected ByteBuffer buffer;
    private boolean forWrite = false;

    private static final byte[] MIN_VALUE_TEXT = ("" + Long.MIN_VALUE).getBytes();
    private static final byte[] Infinity = "Infinity".getBytes();
    private static final byte[] NaN = "NaN".getBytes();
    private static final long MAX_VALUE_DIVIDE_5 = Long.MAX_VALUE / 5;
    private ExcerptInputStream inputStream = null;
    private ExcerptOutputStream outputStream = null;

    protected AbstractExcerpt(C chronicle) {
        this.chronicle = chronicle;
    }

    @Override
    public C chronicle() {
        return chronicle;
    }

    @Override
    public boolean nextIndex() {
        return index(index() + 1);
    }

    @Override
    public boolean index(long index) throws IndexOutOfBoundsException {
        readMemoryBarrier();
        long endPosition = chronicle.getIndexData(index + 1);
        if (endPosition == 0) {
            capacity = 0;
            buffer = null;
            return false;
        }
        long startPosition = chronicle.getIndexData(index);
        capacity = (int) (endPosition - startPosition);
        index0(index, startPosition, endPosition);
        forWrite = false;
        // TODO Assumes the start of the record won't be all 0's
        // TODO Need to determine whether this is required as a safety check or not.
        return readLong(0) != 0L;
    }

    private boolean readMemoryBarrier() {
        return barrier.get();
    }

    @Override
    public void startExcerpt(int capacity) {
        this.capacity = capacity;
        long startPosition = chronicle.startExcerpt(capacity);
        long endPosition = startPosition + capacity;
        index0(chronicle.size(), startPosition, endPosition);
        forWrite = true;
    }

    @Override
    public void finish() {
        if (position > limit)
            throw new IllegalStateException("Capacity allowed: " + capacity + " data read/written: " + (position - start));
        if (forWrite) {
            final long endPosition = startPosition + (position - start);
            chronicle.setIndexData(index + 1, endPosition);
            chronicle.incrementSize();
            capacity = (int) (position - start);
            writeMemoryBarrier();
        }
    }

    protected abstract void index0(long index, long startPosition, long endPosition);

    final AtomicBoolean barrier = new AtomicBoolean();

    private void writeMemoryBarrier() {
        barrier.lazySet(true);
    }

    @Override
    public long index() {
        return index;
    }

    @Override
    public Excerpt<C> position(int position) {
        if (position < 0 || position > capacity())
            throw new IndexOutOfBoundsException();
        this.position = start + position; // start has to be added
        return this;
    }

    @Override
    public int position() {
        return (int) (position - start);
    }

    @Override
    public int capacity() {
        return (int) (limit - start);
    }

    @Override
    public int remaining() {
        return (int) (limit - position);
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
        return readShort() & 0xFFFF;
    }

    @Override
    public int readUnsignedShort(int offset) {
        return readShort(offset) & 0xFFFF;
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

    private StringBuilder utfReader = null;

    @Override
    public String readUTF() {
        readUTF(acquireUtfReader());
        return utfReader.toString();
    }

    private StringBuilder acquireUtfReader() {
        if (utfReader == null) utfReader = new StringBuilder();
        utfReader.setLength(0);
        return utfReader;
    }

    @Override
    public void readUTF(Appendable appendable) {
        try {
            readUTF0(appendable);
        } catch (IOException unexpected) {
            throw new AssertionError(unexpected);
        }
    }

    private void readUTF0(Appendable appendable) throws IOException {
        int utflen = readUnsignedShort();
        int count = 0;
        while (count < utflen) {
            int c = readByte();
            if (c < 0) {
                position(position() - 1);
                break;
            }
            count++;
            appendable.append((char) c);
        }

        while (count < utflen) {
            int c = readUnsignedByte();
            switch (c >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    /* 0xxxxxxx*/
                    count++;
                    appendable.append((char) c);
                    break;
                case 12:
                case 13: {
                    /* 110x xxxx   10xx xxxx*/
                    count += 2;
                    if (count > utflen)
                        throw new UTFDataFormatException(
                                "malformed input: partial character at end");
                    int char2 = readUnsignedByte();
                    if ((char2 & 0xC0) != 0x80)
                        throw new UTFDataFormatException(
                                "malformed input around byte " + count);
                    int c2 = (char) (((c & 0x1F) << 6) |
                            (char2 & 0x3F));
                    appendable.append((char) c2);
                    break;
                }
                case 14: {
                    /* 1110 xxxx  10xx xxxx  10xx xxxx */
                    count += 3;
                    if (count > utflen)
                        throw new UTFDataFormatException(
                                "malformed input: partial character at end");
                    int char2 = readUnsignedByte();
                    int char3 = readUnsignedByte();

                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
                        throw new UTFDataFormatException(
                                "malformed input around byte " + (count - 1));
                    int c3 = (char) (((c & 0x0F) << 12) |
                            ((char2 & 0x3F) << 6) |
                            (char3 & 0x3F));
                    appendable.append((char) c3);
                    break;
                }
                default:
                    /* 10xx xxxx,  1111 xxxx */
                    throw new UTFDataFormatException(
                            "malformed input around byte " + count);
            }
        }
    }

    @Override
    public String parseUTF(StopCharTester tester) {
        parseUTF(acquireUtfReader(), tester);
        return utfReader.toString();
    }

    @Override
    public void parseUTF(Appendable builder, StopCharTester tester) {
        try {
            readUTF0(builder, tester);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private void readUTF0(Appendable appendable, StopCharTester tester) throws IOException {
        while (remaining() > 0) {
            int c = readByte();
            if (c < 0) {
                position(position() - 1);
                break;
            }
            if (tester.isStopChar(c))
                return;
            appendable.append((char) c);
        }

        while (remaining() > 0) {
            int c = readUnsignedByte();
            switch (c >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    /* 0xxxxxxx*/
                    if (tester.isStopChar(c))
                        return;
                    appendable.append((char) c);
                    break;
                case 12:
                case 13: {
                    /* 110x xxxx   10xx xxxx*/
                    int char2 = readUnsignedByte();
                    if ((char2 & 0xC0) != 0x80)
                        throw new UTFDataFormatException(
                                "malformed input around byte");
                    int c2 = (char) (((c & 0x1F) << 6) |
                            (char2 & 0x3F));
                    if (tester.isStopChar(c2))
                        return;
                    appendable.append((char) c2);
                    break;
                }
                case 14: {
                    /* 1110 xxxx  10xx xxxx  10xx xxxx */

                    int char2 = readUnsignedByte();
                    int char3 = readUnsignedByte();

                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
                        throw new UTFDataFormatException(
                                "malformed input around byte ");
                    int c3 = (char) (((c & 0x0F) << 12) |
                            ((char2 & 0x3F) << 6) |
                            (char3 & 0x3F));
                    if (tester.isStopChar(c3))
                        return;
                    appendable.append((char) c3);
                    break;
                }
                default:
                    /* 10xx xxxx,  1111 xxxx */
                    throw new UTFDataFormatException(
                            "malformed input around byte ");
            }
        }
    }


    @Override
    public String readUTF(int offset) {
        long oldPosition = position;
        position = offset;
        try {
            return readUTF();
        } finally {
            position = oldPosition;
        }
    }

    private static final byte BYTE_MIN_VALUE = Byte.MIN_VALUE;
    private static final byte BYTE_EXTENDED = Byte.MIN_VALUE + 1;
    private static final byte BYTE_MAX_VALUE = Byte.MIN_VALUE + 2;

    private static final short UBYTE_EXTENDED = 0xff;

    @Override
    public short readCompactShort() {
        byte b = readByte();
        switch (b) {
            case BYTE_MIN_VALUE:
                return Short.MIN_VALUE;
            case BYTE_MAX_VALUE:
                return Short.MAX_VALUE;
            case BYTE_EXTENDED:
                return readShort();
            default:
                return b;
        }
    }

    @Override
    public int readCompactUnsignedShort() {
        int b = readUnsignedByte();
        if (b == UBYTE_EXTENDED)
            return readUnsignedShort();
        return b;
    }


    @Override
    public int readInt24() {
        if (chronicle.byteOrder() == ByteOrder.BIG_ENDIAN)
            return (readUnsignedByte() << 24 + readUnsignedShort() << 8) >> 8;
        // extra shifting to get sign extension.
        return (readUnsignedByte() << 8 + readUnsignedShort() << 16) >> 8;
    }

    @Override
    public int readInt24(int offset) {
        if (chronicle.byteOrder() == ByteOrder.BIG_ENDIAN)
            return (readUnsignedByte(offset) << 24 + readUnsignedShort(offset + 1) << 8) >> 8;
        // extra shifting to get sign extension.
        return (readUnsignedByte(offset) << 8 + readUnsignedShort(offset + 1) << 16) >> 8;
    }

    @Override
    public long readUnsignedInt() {
        return readInt() & 0xFFFFFFFFL;
    }

    @Override
    public long readUnsignedInt(int offset) {
        return readInt(offset) & 0xFFFFFFFFL;
    }

    private static final short SHORT_MIN_VALUE = Short.MIN_VALUE;
    private static final short SHORT_EXTENDED = Short.MIN_VALUE + 1;
    private static final short SHORT_MAX_VALUE = Short.MIN_VALUE + 2;

    private static final int USHORT_EXTENDED = 0xFFFF;

    @Override
    public int readCompactInt() {
        short b = readShort();
        switch (b) {
            case SHORT_MIN_VALUE:
                return Integer.MIN_VALUE;
            case SHORT_MAX_VALUE:
                return Integer.MAX_VALUE;
            case SHORT_EXTENDED:
                return readInt();
            default:
                return b;
        }
    }


    @Override
    public long readCompactUnsignedInt() {
        int b = readUnsignedByte();
        if (b == USHORT_EXTENDED)
            return readUnsignedInt();
        return b;
    }

    @Override
    public long readInt48() {
        if (chronicle.byteOrder() == ByteOrder.BIG_ENDIAN)
            return ((long) readUnsignedShort() << 48 + readUnsignedInt() << 16) >> 16;
        // extra shifting to get sign extension.
        return (readUnsignedShort() << 16 + readUnsignedInt() << 32) >> 8;
    }

    @Override
    public long readInt48(int offset) {
        if (chronicle.byteOrder() == ByteOrder.BIG_ENDIAN)
            return ((long) readUnsignedShort(offset) << 48 + readUnsignedInt(offset + 2) << 16) >> 16;
        // extra shifting to get sign extension.
        return (readUnsignedShort(offset) << 16 + readUnsignedInt(offset + 2) << 32) >> 16;
    }

    private static final int INT_MIN_VALUE = Integer.MIN_VALUE;
    private static final int INT_EXTENDED = Integer.MIN_VALUE + 1;
    private static final int INT_MAX_VALUE = Integer.MIN_VALUE + 2;

    @Override
    public long readCompactLong() {
        int b = readInt();
        switch (b) {
            case INT_MIN_VALUE:
                return Long.MIN_VALUE;
            case INT_MAX_VALUE:
                return Long.MAX_VALUE;
            case INT_EXTENDED:
                return readLong();
            default:
                return b;
        }
    }

    // RandomDataOutput

    @Override
    public double readCompactDouble() {
        float f = readFloat();
        if (Float.isNaN(f))
            return readDouble();
        return f;
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
    public void readChars(StringBuilder sb) {
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

    @Override
    public ByteOrder order() {
        return buffer.order();
    }

    @Override
    public void read(ByteBuffer bb) {
        int len = Math.min(bb.remaining(), length());
        if (bb.order() == order()) {
            while (len >= 8) {
                bb.putLong(readLong());
                len -= 8;
            }
        }
        while (len > 0) {
            bb.put(readByte());
            len--;
        }
    }

    //// RandomOutputStream
    @Override
    public void write(byte[] b) {
        write(b, 0, b.length);
    }

    @Override
    public void writeBoolean(boolean v) {
        write(v ? -1 : 0);
    }

    @Override
    public void writeBoolean(int offset, boolean v) {
        write(offset, v ? -1 : 0);
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
        writeUTF((CharSequence) s);
    }

    @Override
    public void writeUTF(CharSequence str) {

        int strlen = str.length();
        int utflen = 0;
        int c = 0;

        /* use charAt instead of copying String to char array */
        for (int i = 0; i < strlen; i++) {
            c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                utflen++;
            } else if (c > 0x07FF) {
                utflen += 3;
            } else {
                utflen += 2;
            }
        }

        if (utflen > 65535)
            throw new IllegalArgumentException(new UTFDataFormatException(
                    "encoded string too long: " + utflen + " bytes"));
        if (utflen > remaining())
            throw new IllegalArgumentException(
                    "encoded string too long: " + utflen + " bytes, remaining=" + remaining());

        writeUnsignedShort(utflen);

        int i;
        for (i = 0; i < strlen; i++) {
            c = str.charAt(i);
            if (!((c >= 0x0001) && (c <= 0x007F))) break;
            write(c);
        }

        for (; i < strlen; i++) {
            c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                write(c);

            } else if (c > 0x07FF) {
                write((byte) (0xE0 | ((c >> 12) & 0x0F)));
                write((byte) (0x80 | ((c >> 6) & 0x3F)));
                write((byte) (0x80 | (c & 0x3F)));
            } else {
                write((byte) (0xC0 | ((c >> 6) & 0x1F)));
                write((byte) (0x80 | c & 0x3F));
            }
        }
    }

    @Override
    public void writeByte(int v) {
        write(v);
    }

    @Override
    public void writeUnsignedByte(int v) {
        writeByte(v);
    }

    @Override
    public void writeUnsignedByte(int offset, int v) {
        write(offset, v);
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
    public void writeUnsignedShort(int v) {
        writeShort(v);
    }

    @Override
    public void writeUnsignedShort(int offset, int v) {
        writeShort(offset, v);
    }

    @Override
    public void writeCompactShort(int v) {
        if (v > BYTE_MAX_VALUE && v <= Byte.MAX_VALUE)
            writeByte(v);
        else switch (v) {
            case Short.MIN_VALUE:
                writeByte(BYTE_MIN_VALUE);
                break;
            case Short.MAX_VALUE:
                writeByte(BYTE_MAX_VALUE);
                break;
            default:
                writeByte(BYTE_EXTENDED);
                writeShort(v);
                break;
        }
    }

    @Override
    public void writeCompactUnsignedShort(int v) {
        if (v >= 0 && v < USHORT_EXTENDED) {
            writeByte(v);
        } else {
            writeUnsignedShort(USHORT_EXTENDED);
            writeUnsignedShort(v);
        }
    }

    @Override
    public void writeInt24(int v) {
        if (chronicle.byteOrder() == ByteOrder.BIG_ENDIAN) {
            writeUnsignedByte(v >>> 16);
            writeUnsignedShort(v);
        } else {
            writeUnsignedByte(v);
            writeUnsignedShort(v >>> 8);
        }
    }

    @Override
    public void writeInt24(int offset, int v) {
        if (chronicle.byteOrder() == ByteOrder.BIG_ENDIAN) {
            writeUnsignedByte(offset, v >>> 16);
            writeUnsignedShort(offset + 1, v);
        } else {
            writeUnsignedByte(offset, v);
            writeUnsignedShort(offset + 1, v >>> 8);
        }
    }

    @Override
    public void writeUnsignedInt(long v) {
        writeInt((int) v);
    }

    @Override
    public void writeUnsignedInt(int offset, long v) {
        writeInt(offset, (int) v);
    }

    @Override
    public void writeCompactInt(int v) {
        if (v > SHORT_MAX_VALUE && v <= Short.MAX_VALUE)
            writeShort(v);
        else switch (v) {
            case Integer.MIN_VALUE:
                writeShort(SHORT_MIN_VALUE);
                break;
            case Integer.MAX_VALUE:
                writeShort(SHORT_MAX_VALUE);
                break;
            default:
                writeShort(BYTE_EXTENDED);
                writeInt(v);
                break;
        }
    }


    @Override
    public void writeCompactUnsignedInt(long v) {
        if (v >= 0 && v < USHORT_EXTENDED) {
            writeShort((int) v);
        } else {
            writeShort(USHORT_EXTENDED);
            writeUnsignedInt(v);
        }
    }

    @Override
    public void writeInt48(long v) {
        if (chronicle.byteOrder() == ByteOrder.BIG_ENDIAN) {
            writeUnsignedShort((int) (v >>> 32));
            writeUnsignedInt(v);
        } else {
            writeUnsignedShort((int) v);
            writeUnsignedInt(v >>> 16);
        }
    }

    @Override
    public void writeInt48(int offset, long v) {
        if (chronicle.byteOrder() == ByteOrder.BIG_ENDIAN) {
            writeUnsignedShort(offset, (int) (v >>> 32));
            writeUnsignedInt(offset + 2, v);
        } else {
            writeUnsignedShort(offset, (int) v);
            writeUnsignedInt(offset + 2, v >>> 16);
        }
    }

    @Override
    public void writeCompactLong(long v) {
        if (v > INT_MAX_VALUE && v <= Integer.MAX_VALUE) {
            writeInt((int) v);

        } else if (v == Long.MIN_VALUE) {
            writeInt(BYTE_MIN_VALUE);

        } else if (v == Long.MAX_VALUE) {
            writeInt(BYTE_MAX_VALUE);

        } else {
            writeInt(BYTE_EXTENDED);
            writeLong(v);

        }
    }


    @Override
    public void writeCompactDouble(double v) {
        float f = (float) v;
        if (f == v) {
            writeFloat(f);
        } else {
            writeFloat(Float.NaN);
            writeDouble(v);
        }
    }

    @Override
    public void write(ByteBuffer bb) {
        if (bb.order() == order())
            while (bb.remaining() >= 8)
                writeLong(bb.getLong());
        while (bb.remaining() >= 1)
            writeByte(bb.get());
    }

    //// ByteStringAppender

    @Override
    public int length() {
        return position();
    }

    @Override
    public ByteStringAppender append(CharSequence s) {
        for (int i = 0, len = s.length(); i < len; i++)
            writeByte(s.charAt(i));
        return this;
    }

    @Override
    public ByteStringAppender append(CharSequence s, int start, int end) {
        for (int i = start, len = Math.min(end, s.length()); i < len; i++)
            writeByte(s.charAt(i));
        return this;
    }

    @Override
    public ByteStringAppender append(Enum value) {
        return append(value.toString());
    }

    @Override
    public ByteStringAppender append(byte[] str) {
        write(str);
        return this;
    }

    @Override
    public ByteStringAppender append(byte[] str, int offset, int len) {
        write(str, offset, len);
        return this;
    }

    @Override
    public ByteStringAppender append(boolean b) {
        append(b ? "true" : "false");
        return this;
    }

    @Override
    public ByteStringAppender append(char c) {
        writeByte(c);
        return this;
    }

    @Override
    public ByteStringAppender append(int num) {
        return append((long) num);
    }

    @Override
    public ByteStringAppender append(long num) {
        if (num < 0) {
            if (num == Long.MIN_VALUE) {
                append(MIN_VALUE_TEXT);
                return this;
            }
            writeByte('-');
            num = -num;
        }
        if (num == 0) {
            writeByte('0');

        } else {
            appendLong0(num);
        }
        return this;
    }

    @Override
    public ByteStringAppender appendTime(long timeInMS) {
        int hours = (int) (timeInMS / (60 * 60 * 1000));
        if (hours > 99) {
            appendLong0(hours); // can have over 24 hours.
        } else {
            writeByte((char) (hours / 10 + '0'));
            writeByte((char) (hours % 10 + '0'));
        }
        writeByte(':');
        int minutes = (int) ((timeInMS / (60 * 1000)) % 60);
        writeByte((char) (minutes / 10 + '0'));
        writeByte((char) (minutes % 10 + '0'));
        writeByte(':');
        int seconds = (int) ((timeInMS / 1000) % 60);
        writeByte((char) (seconds / 10 + '0'));
        writeByte((char) (seconds % 10 + '0'));
        writeByte('.');
        int millis = (int) (timeInMS % 1000);
        writeByte((char) (millis / 100 + '0'));
        writeByte((char) (millis / 10 % 10 + '0'));
        writeByte((char) (millis % 10 + '0'));
        return this;
    }

    @Override
    public ByteStringAppender append(double d) {
        long val = Double.doubleToRawLongBits(d);
        int sign = (int) (val >>> 63);
        int exp = (int) ((val >>> 52) & 2047);
        long mantissa = val & ((1L << 52) - 1);
        if (sign != 0) {
            writeByte('-');
        }
        if (exp == 0 && mantissa == 0) {
            writeByte('0');
            return this;
        } else if (exp == 2047) {
            if (mantissa == 0) {
                buffer.put(Infinity);
            } else {
                buffer.put(NaN);
            }
            return this;
        } else if (exp > 0) {
            mantissa += 1L << 52;
        }
        final int shift = (1023 + 52) - exp;
        if (shift > 0) {
            // integer and faction
            if (shift < 53) {
                long intValue = mantissa >> shift;
                appendLong0(intValue);
                mantissa -= intValue << shift;
                if (mantissa > 0) {
                    writeByte('.');
                    mantissa <<= 1;
                    mantissa++;
                    int precision = shift + 1;
                    long error = 1;

                    long value = intValue;
                    int decimalPlaces = 0;
                    while (mantissa > error) {
                        // times 5*2 = 10
                        mantissa *= 5;
                        error *= 5;
                        precision--;
                        long num = (mantissa >> precision);
                        value = value * 10 + num;
                        writeByte((char) ('0' + num));
                        mantissa -= num << precision;

                        final double parsedValue = asDouble(value, 0, sign != 0, ++decimalPlaces);
                        if (parsedValue == d)
                            break;
                    }
                }
                return this;

            } else {
                // faction.
                writeByte('0');
                writeByte('.');
                mantissa <<= 6;
                mantissa += (1 << 5);
                int precision = shift + 6;

                long error = (1 << 5);

                long value = 0;
                int decimalPlaces = 0;
                while (mantissa > error) {
                    while (mantissa > MAX_VALUE_DIVIDE_5) {
                        mantissa >>>= 1;
                        error = (error + 1) >>> 1;
                        precision--;
                    }
                    // times 5*2 = 10
                    mantissa *= 5;
                    error *= 5;
                    precision--;
                    if (precision >= 64) {
                        decimalPlaces++;
                        writeByte('0');
                        continue;
                    }
                    long num = (mantissa >>> precision);
                    value = value * 10 + num;
                    final char c = (char) ('0' + num);
                    assert !(c < '0' || c > '9');
                    writeByte(c);
                    mantissa -= num << precision;
                    final double parsedValue = asDouble(value, 0, sign != 0, ++decimalPlaces);
                    if (parsedValue == d)
                        break;
                }
                return this;
            }
        }
        // large number
        mantissa <<= 10;
        int precision = -10 - shift;
        int digits = 0;
        while ((precision > 53 || mantissa > Long.MAX_VALUE >> precision) && precision > 0) {
            digits++;
            precision--;
            long mod = mantissa % 5;
            mantissa /= 5;
            int modDiv = 1;
            while (mantissa < MAX_VALUE_DIVIDE_5 && precision > 1) {
                precision -= 1;
                mantissa <<= 1;
                modDiv <<= 1;
            }
            mantissa += modDiv * mod / 5;
        }
        long val2 = precision > 0 ? mantissa << precision : mantissa >>> -precision;

        appendLong0(val2);
        for (int i = 0; i < digits; i++)
            writeByte('0');

        return this;
    }

    static double asDouble(long value, int exp, boolean negative, int decimalPlaces) {
        if (decimalPlaces > 0 && value < Long.MAX_VALUE / 2) {
            if (value < Long.MAX_VALUE / (1L << 32)) {
                exp -= 32;
                value <<= 32;
            }
            if (value < Long.MAX_VALUE / (1L << 16)) {
                exp -= 16;
                value <<= 16;
            }
            if (value < Long.MAX_VALUE / (1L << 8)) {
                exp -= 8;
                value <<= 8;
            }
            if (value < Long.MAX_VALUE / (1L << 4)) {
                exp -= 4;
                value <<= 4;
            }
            if (value < Long.MAX_VALUE / (1L << 2)) {
                exp -= 2;
                value <<= 2;
            }
            if (value < Long.MAX_VALUE / (1L << 1)) {
                exp -= 1;
                value <<= 1;
            }
        }
        for (; decimalPlaces > 0; decimalPlaces--) {
            exp--;
            long mod = value % 5;
            value /= 5;
            int modDiv = 1;
            if (value < Long.MAX_VALUE / (1L << 4)) {
                exp -= 4;
                value <<= 4;
                modDiv <<= 4;
            }
            if (value < Long.MAX_VALUE / (1L << 2)) {
                exp -= 2;
                value <<= 2;
                modDiv <<= 2;
            }
            if (value < Long.MAX_VALUE / (1L << 1)) {
                exp -= 1;
                value <<= 1;
                modDiv <<= 1;
            }
            value += modDiv * mod / 5;
        }
        final double d = Math.scalb((double) value, exp);
        return negative ? -d : d;
    }

    public static final long MAX_VALUE_DIVIDE_10 = Long.MAX_VALUE / 10;

    @Override
    public double parseDouble() {
        long value = 0;
        int exp = 0;
        boolean negative = false;
        int decimalPlaces = Integer.MIN_VALUE;
        while (true) {
            byte ch = readByte();
            if (ch >= '0' && ch <= '9') {
                while (value >= MAX_VALUE_DIVIDE_10) {
                    value >>>= 1;
                    exp++;
                }
                value = value * 10 + (ch - '0');
                decimalPlaces++;
            } else if (ch == '-') {
                negative = true;
            } else if (ch == '.') {
                decimalPlaces = 0;
            } else {
                break;
            }
        }

        return asDouble(value, exp, negative, decimalPlaces);
    }

    @Override
    public long parseLong() {
        long num = 0;
        boolean negative = false;
        while (true) {
            byte b = readByte();
//            if (b >= '0' && b <= '9')
            if ((b - ('0' + Integer.MIN_VALUE)) <= 9 + Integer.MIN_VALUE)
                num = num * 10 + b - '0';
            else if (b == '-')
                negative = true;
            else
                break;
        }
        return negative ? -num : num;
    }

    private void appendLong0(long num) {
        // find the number of digits
        long power10 = power10(num);
        // starting from the end, write each digit
        while (power10 > 0) {
            // write the lowest digit.
            writeByte((byte) (num / power10 % 10 + '0'));
            // remove that digit.
            power10 /= 10;
        }
    }

    @Override
    public ByteStringAppender append(double d, int precision) {
        if (precision < 0) precision = 0;
        if (precision >= TENS.length) precision = TENS.length - 1;
        long power10 = TENS[precision];
        if (d < 0) {
            d = -d;
            writeByte('-');
        }
        double d2 = d * power10;
        if (d2 > Long.MAX_VALUE || d2 < Long.MIN_VALUE + 1)
            return append(d);
        long val = (long) (d2 + 0.5);
        while (precision > 0 && val % 10 == 0) {
            val /= 10;
            precision--;
        }
        if (precision > 0)
            appendDouble0(val, precision);
        else
            appendLong0(val);
        return this;
    }

    private void appendDouble0(long num, int precision) {
        // find the number of digits
        long power10 = Math.max(TENS[precision], power10(num));
        // starting from the end, write each digit
        long decimalPoint = TENS[precision - 1];
        while (power10 > 0) {
            if (decimalPoint == power10)
                writeByte('.');
            // write the lowest digit.
            writeByte((byte) (num / power10 % 10 + '0'));
            // remove that digit.
            power10 /= 10;
        }
    }

    static final long[] TENS = new long[19];

    static {
        TENS[0] = 1;
        for (int i = 1; i < TENS.length; i++)
            TENS[i] = TENS[i - 1] * 10;
    }

    public static long power10(long l) {
        int idx = Arrays.binarySearch(TENS, l);
        return idx >= 0 ? TENS[idx] : TENS[~idx - 1];
    }

    @Override
    public InputStream inputStream() {
        if (inputStream == null)
            inputStream = new ExcerptInputStream();
        return inputStream;
    }

    @Override
    public OutputStream outputStream() {
        if (outputStream == null)
            outputStream = new ExcerptOutputStream();
        return outputStream;
    }

    protected class ExcerptInputStream extends InputStream {
        private int mark = 0;

        @Override
        public int available() throws IOException {
            return remaining();
        }

        @Override
        public void close() throws IOException {
            finish();
        }

        @Override
        public void mark(int readlimit) {
            mark = position();
        }

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            AbstractExcerpt.this.readFully(b, off, len);
            return len;
        }

        @Override
        public void reset() throws IOException {
            position(mark);
        }

        @Override
        public long skip(long n) throws IOException {
            if (n > Integer.MAX_VALUE) throw new IOException("Skip too large");
            return skipBytes((int) n);
        }

        @Override
        public int read() throws IOException {
            if (remaining() > 0)
                return readUnsignedByte();
            return -1;
        }
    }

    protected class ExcerptOutputStream extends OutputStream {
        @Override
        public void close() throws IOException {
            finish();
        }

        @Override
        public void write(byte[] b) throws IOException {
            AbstractExcerpt.this.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            AbstractExcerpt.this.write(b, off, len);
        }

        @Override
        public void write(int b) throws IOException {
            writeUnsignedByte(b);
        }
    }

    @Override
    public <E> void writeEnum(E e) {
        EnumeratedMarshaller<E> em = chronicle().acquireMarshaller((Class) e.getClass());
        em.write(this, e);
    }

    @Override
    public <E> E readEnum(Class<E> eClass) {
        EnumeratedMarshaller<E> em = chronicle().acquireMarshaller(eClass);
        return em.read(this);
    }

    @Override
    public <E> E parseEnum(Class<E> eClass, StopCharTester tester) {
        EnumeratedMarshaller<E> em = chronicle().acquireMarshaller(eClass);
        return em.parse(this, tester);
    }

    @Override
    public <E> void writeEnums(Collection<E> eList) {
        writeInt(eList.size());
        for (E e : eList)
            writeEnum(e);
    }

    @Override
    public <K, V> void writeMap(Map<K, V> map) {
        writeInt(map.size());
        for (Map.Entry<K, V> entry : map.entrySet()) {
            writeEnum(entry.getKey());
            writeEnum(entry.getValue());
        }
    }

    @Override
    public <E> List<E> readEnums(Class<E> eClass) {
        int len = readInt();
        if (len == 0) return Collections.emptyList();
        List<E> list = new ArrayList<E>(len);
        for (int i = 0; i < len; i++)
            list.add(readEnum(eClass));
        return list;
    }

    @Override
    public <K, V> Map<K, V> readMap(Class<K> kClass, Class<V> vClass) {
        int len = readInt();
        if (len == 0) return Collections.emptyMap();
        Map<K, V> map = new LinkedHashMap<K, V>(len * 10 / 7);
        for (int i = 0; i < len; i++)
            map.put(readEnum(kClass), readEnum(vClass));
        return map;
    }
}
