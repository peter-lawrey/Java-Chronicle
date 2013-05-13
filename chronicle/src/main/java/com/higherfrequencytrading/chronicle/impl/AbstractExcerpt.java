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

import com.higherfrequencytrading.chronicle.ByteStringAppender;
import com.higherfrequencytrading.chronicle.EnumeratedMarshaller;
import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.StopCharTester;
import com.higherfrequencytrading.chronicle.math.MutableDecimal;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author peter.lawrey
 */
public abstract class AbstractExcerpt implements Excerpt {
    private static final int MIN_SIZE = 8;
    public static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

    protected final DirectChronicle chronicle;
    protected long index = -1;
    protected long start = 0;
    protected long position = 0;
    private int capacity = 0;
    protected long limit = 0;

    protected long startPosition;

    protected long size = 0;

    protected MappedByteBuffer buffer;
    private boolean forWrite = false;

    private static final byte[] MIN_VALUE_TEXT = ("" + Long.MIN_VALUE).getBytes();
    private static final byte[] Infinity = "Infinity".getBytes();
    private static final byte[] NaN = "NaN".getBytes();
    private static final long MAX_VALUE_DIVIDE_5 = Long.MAX_VALUE / 5;
    // extra 1 for decimal place.
    static final int MAX_NUMBER_LENGTH = 1 + (int) Math.ceil(Math.log10(Long.MAX_VALUE));

    private final byte[] numberBuffer = new byte[MAX_NUMBER_LENGTH];
    private ExcerptInputStream inputStream = null;
    private ExcerptOutputStream outputStream = null;

    protected AbstractExcerpt(DirectChronicle chronicle) {
        this.chronicle = chronicle;
    }

    @Override
    public DirectChronicle chronicle() {
        return chronicle;
    }

    @Override
    public boolean nextIndex() {
        return index(index() + 1);
    }

    @Override
    public boolean hasNextIndex() {
        readMemoryBarrier();
        long nextIndex = index + 1;
        long endPosition = chronicle.getIndexData(nextIndex + 1);
        return endPosition != 0;
    }

    @Override
    public boolean index(long index) throws IndexOutOfBoundsException {
        forWrite = false;

        readMemoryBarrier();
        long endPosition = chronicle.getIndexData(index + 1);
        if (endPosition == 0) {
            capacity = 0;
            buffer = null;
//            System.out.println("ep");
            // rewind?
            if (index == -1) {
                this.index = -1;
                limit = startPosition = position = capacity = 0;
                return true;
            }
            return false;
        }
        long startPosition = chronicle.getIndexData(index);
        capacity = (int) (endPosition - startPosition);
        assert capacity >= MIN_SIZE : "end=" + endPosition + ", start=" + startPosition;
        index0(index, startPosition, endPosition);
        // TODO Assumes the start of the record won't be all 0's
        // TODO Need to determine whether this is required as a safety check or not.
        long l = readLong(0);
        return l != 0L;
    }

    @Override
    public long size() {
        readMemoryBarrier();
        long size = this.size;
        while (chronicle.getIndexData(size + 1) != 0)
            size++;
        return this.size = size;
    }

    private void readMemoryBarrier() {
        barrier.get();
    }

    @Override
    public void startExcerpt(int capacity) {
        this.capacity = capacity < MIN_SIZE ? MIN_SIZE : capacity;
        long startPosition = chronicle.startExcerpt(capacity);
        long endPosition = startPosition + capacity;
        index0(chronicle.size(), startPosition, endPosition);
        forWrite = true;
    }

    private Thread lastThread = null;

    @SuppressWarnings("SameReturnValue")
    private boolean checkThread() {
        Thread thread = Thread.currentThread();
        if (lastThread == null)
            lastThread = thread;
        else if (lastThread != thread)
            throw new AssertionError("Excerpt used by two threads " + thread + " and " + lastThread);
        return true;
    }

    @Override
    public void finish() {
        assert checkThread();
        long length = checkEndOfBuffer();
        if (forWrite) {
            if (chronicle.synchronousMode())
                buffer.force();
            final long endPosition = startPosition + length;
            chronicle.setIndexData(index + 1, endPosition);
            chronicle.incrementSize();
            capacity = (int) length;
            assert capacity >= MIN_SIZE : "len=" + length;
            writeMemoryBarrier();
        }
        buffer = null;
    }

    private long checkEndOfBuffer() {
        long length = position - start;
        if (length < MIN_SIZE)
            length = MIN_SIZE;
        if (position > limit)
            throw new IllegalStateException("Capacity allowed: " + capacity + " data read/written: " + length);
        if (readLong(0) == 0)
            throw new IllegalStateException("The first 8 bytes cannot be all zero");
        return length;
    }

    protected abstract void index0(long index, long startPosition, long endPosition);

    private final AtomicBoolean barrier = new AtomicBoolean();

    private void writeMemoryBarrier() {
        barrier.lazySet(true);
    }

    @Override
    public long index() {
        return index;
    }

    @Override
    public Excerpt position(int position) {
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
        if (readUTF(acquireUtfReader()))
            return utfReader.toString();
        return null;
    }

    private StringBuilder acquireUtfReader() {
        if (utfReader == null) utfReader = new StringBuilder();
        utfReader.setLength(0);
        return utfReader;
    }

    @Override
    public boolean readUTF(Appendable appendable) {
        return appendUTF(appendable);
    }

    @Override
    public boolean readUTF(StringBuilder stringBuilder) {
        try {
            stringBuilder.setLength(0);
            return appendUTF0(stringBuilder);
        } catch (IOException unexpected) {
            throw new AssertionError(unexpected);
        }
    }

    @Override
    public boolean appendUTF(Appendable appendable) {
        try {
            return appendUTF0(appendable);
        } catch (IOException unexpected) {
            throw new AssertionError(unexpected);
        }
    }

    private boolean appendUTF0(Appendable appendable) throws IOException {
        long len = readStopBit();
        if (len < -1 || len > Integer.MAX_VALUE)
            throw new StreamCorruptedException("UTF length invalid " + len);
        if (len == -1)
            return false;
        int utflen = (int) len;
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
        return true;
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
    public boolean stepBackAndSkipTo(StopCharTester tester) {
        if (position() > 0)
            position(position() - 1);
        return skipTo(tester);
    }

    @Override
    public boolean skipTo(StopCharTester tester) {
        while (remaining() > 0) {
            int ch = readByte();
            if (tester.isStopChar(ch))
                return true;
        }
        return false;
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

    @Override
    public long readStopBit() {
        long l = 0, b;
        int count = 0;
        while ((b = readByte()) < 0) {
            l |= (b & 0x7FL) << count;
            count += 7;
        }
        if (b == 0 && count > 0)
            return ~l;
        return l | (b << count);
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

    @SuppressWarnings("deprecation")
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
        int len = Math.min(bb.remaining(), remaining());
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

    @Override
    public void writeUTF(String s) {
        writeUTF((CharSequence) s);
    }

    @Override
    public void writeUTF(CharSequence str) {
        if (str == null) {
            writeStopBit(-1);
            return;
        }
        long strlen = str.length();
        int utflen = 0;
        int c;

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

        if (utflen > remaining())
            throw new IllegalArgumentException(
                    "encoded string too long: " + utflen + " bytes, remaining=" + remaining());

        writeStopBit(utflen);

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
    public void writeStopBit(long n) {
        boolean neg = false;
        if (n < 0) {
            neg = true;
            n = ~n;
        }
        while (true) {
            long n2 = n >>> 7;
            if (n2 != 0) {
                writeByte((byte) (0x80 | (n & 0x7F)));
                n = n2;
            } else {
                if (neg) {
                    writeByte((byte) (0x80 | (n & 0x7F)));
                    writeByte(0);
                } else {
                    writeByte((byte) (n & 0x7F));
                }
                break;
            }
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

    private SimpleDateFormat dateFormat = null;
    private long lastDay = Long.MIN_VALUE;
    private byte[] lastDateStr = null;

    @Override
    public ByteStringAppender appendDate(long timeInMS) {
        if (dateFormat == null) {
            dateFormat = new SimpleDateFormat("yyyy/MM/dd");
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        }
        long date = timeInMS / 86400000;
        if (lastDay != date) {
            lastDateStr = dateFormat.format(new Date(timeInMS)).getBytes(ISO_8859_1);
            lastDay = date;
        }
        append(lastDateStr);
        return this;
    }

    @Override
    public ByteStringAppender appendDateTime(long timeInMS) {
        appendDate(timeInMS);
        writeByte('T');
        appendTime(timeInMS);
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

    private static double asDouble(long value, int exp, boolean negative, int decimalPlaces) {
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

    private static final long MAX_VALUE_DIVIDE_10 = Long.MAX_VALUE / 10;

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
    public MutableDecimal parseDecimal(MutableDecimal decimal) {
        long num = 0, scale = Long.MIN_VALUE;
        boolean negative = false;
        while (true) {
            byte b = readByte();
//            if (b >= '0' && b <= '9')
            if ((b - ('0' + Integer.MIN_VALUE)) <= 9 + Integer.MIN_VALUE) {
                num = num * 10 + b - '0';
                scale++;
            } else if (b == '.') {
                scale = 0;
            } else if (b == '-') {
                negative = true;
            } else {
                break;
            }
        }
        if (negative)
            num = -num;
        decimal.set(num, scale > 0 ? (int) scale : 0);
        return decimal;
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
        // Extract digits into the end of the numberBuffer
        int endIndex = appendLong1(num);

        // Bulk copy the digits into the front of the buffer
        write(numberBuffer, endIndex, MAX_NUMBER_LENGTH - endIndex);
    }

    private int appendLong1(long num) {
        numberBuffer[19] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return 19;
        numberBuffer[18] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return 18;
        numberBuffer[17] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return 17;
        numberBuffer[16] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return 16;
        numberBuffer[15] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return 15;
        numberBuffer[14] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return 14;
        numberBuffer[13] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return 13;
        numberBuffer[12] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return 12;
        numberBuffer[11] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return 11;
        numberBuffer[10] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return 10;
        numberBuffer[9] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return 9;
        numberBuffer[8] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return 8;
        numberBuffer[7] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return 7;
        numberBuffer[6] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return 6;
        numberBuffer[5] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return 5;
        numberBuffer[4] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return 4;
        numberBuffer[3] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return 3;
        numberBuffer[2] = (byte) (num % 10L + '0');
        num /= 10;
        return 2;
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
        // Extract digits into the end of the numberBuffer
        // Once desired precision is reached, write the '.'
        int endIndex = appendDouble1(num, precision);

        // Bulk copy the digits into the front of the buffer
        // TODO: Can this be avoided with use of correctly offset bulk appends on Excerpt?
        // Uses (numberBufferIdx - 1) because index was advanced one too many times

        write(numberBuffer, endIndex, MAX_NUMBER_LENGTH - endIndex);
    }

    private int appendDouble1(long num, final int precision) {
        int endIndex = AbstractExcerpt.MAX_NUMBER_LENGTH;
        numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return endIndex;
        if (precision == 1)
            numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return endIndex;
        if (precision == 2)
            numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return endIndex;
        if (precision == 3)
            numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return endIndex;
        if (precision == 4)
            numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return endIndex;
        if (precision == 5)
            numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return endIndex;
        if (precision == 6)
            numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return endIndex;
        if (precision == 7)
            numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return endIndex;
        if (precision == 8)
            numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return endIndex;
        if (precision == 9)
            numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return endIndex;
        if (precision == 10)
            numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return endIndex;
        if (precision == 11)
            numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return endIndex;
        if (precision == 12)
            numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return endIndex;
        if (precision == 13)
            numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return endIndex;
        if (precision == 14)
            numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return endIndex;
        if (precision == 15)
            numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return endIndex;
        if (precision == 16)
            numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return endIndex;
        if (precision == 17)
            numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        num /= 10;
        if (num <= 0) return endIndex;
        if (precision == 18)
            numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        numberBuffer[--endIndex] = (byte) (num % 10L + '0');
        return endIndex;
    }

    private static final long[] TENS = new long[19];

    static {
        TENS[0] = 1;
        for (int i = 1; i < TENS.length; i++)
            TENS[i] = TENS[i - 1] * 10;
    }

    private static long power10(long l) {
        int idx = Arrays.binarySearch(TENS, l);
        return idx >= 0 ? TENS[idx] : TENS[~idx - 1];
    }

    @Override
    public ByteStringAppender append(MutableDecimal md) {
        StringBuilder sb = acquireUtfReader();
        md.toString(sb);
        append(sb);
        return this;
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

    @SuppressWarnings("unchecked")
    @Override
    public <E> void writeEnum(E e) {
        Class aClass;
        if (e == null)
            aClass = String.class;
        else
            aClass = (Class) e.getClass();
        EnumeratedMarshaller<E> em = chronicle().acquireMarshaller(aClass);
        em.write(this, e);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> E readEnum(Class<E> eClass) {
        EnumeratedMarshaller<E> em = chronicle().acquireMarshaller(eClass);
        return em.read(this);
    }

    @SuppressWarnings("unchecked")
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
    public <E> void writeList(Collection<E> list) {
        writeInt(list.size());
        for (E e : list)
            writeObject(e);
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
    public <E> void readEnums(Class<E> eClass, List<E> eList) {
        eList.clear();
        int len = readInt();
        if (len == 0)
            return;
        for (int i = 0; i < len; i++)
            eList.add(readEnum(eClass));
    }

    @Override
    public <E> void readList(Collection<E> list) {
        int len = readInt();
        list.clear();
        for (int i = 0; i < len; i++) {
            @SuppressWarnings("unchecked")
            E e = (E) readObject();
            list.add(e);
        }
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

    @Override
    public int available() {
        return remaining();
    }

    @Override
    public int read() {
        return remaining() > 0 ? readByte() : -1;
    }

    @Override
    public int read(byte[] b) {
        return read(b, 0, b.length);
    }

    @Override
    public abstract int read(byte[] b, int off, int len);

    @Override
    public long skip(long n) {
        if (n < 0)
            throw new IllegalArgumentException("Skip bytes out of range, was " + n);
        if (n > remaining())
            n = remaining();
        skipBytes((int) n);
        return n;
    }

    @Override
    public void close() {
        if (!isFinished())
            finish();
    }

    @Override
    public void flush() {
        checkEndOfBuffer();
    }

    @Override
    public Object readObject() {
        int type = readByte();
        switch (type) {
            case NULL:
                return null;
            case ENUMED: {
                Class clazz = readEnum(Class.class);
                return readEnum(clazz);
            }
            case SERIALIZED: {
                try {
                    return new ObjectInputStream(this.inputStream()).readObject();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            default:
                throw new IllegalStateException("Unknown type " + (char) type);
        }
    }

    private static final byte NULL = 'N';
    private static final byte ENUMED = 'E';
    private static final byte SERIALIZED = 'S';

    @SuppressWarnings("unchecked")
    @Override
    public void writeObject(Object obj) {
        if (obj == null) {
            writeByte(NULL);
            return;
        }

        Class<?> clazz = obj.getClass();
        EnumeratedMarshaller em = obj instanceof Comparable || obj instanceof Externalizable ?
                chronicle.acquireMarshaller(clazz) :
                chronicle.getMarshaller(clazz);
        if (em != null) {
            writeByte(ENUMED);
            writeEnum(clazz);
            em.write(this, obj);
            return;
        }
        writeByte(SERIALIZED);
        // TODO this is the lame implementation, but it works.
        try {
            ObjectOutputStream oos = new ObjectOutputStream(this.outputStream());
            oos.writeObject(obj);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        checkEndOfBuffer();
    }

    @Override
    public Excerpt toStart() {
        index(-1);
        return this;
    }

    @Override
    public Excerpt toEnd() {
        index(size() - 1);
        return this;
    }

    @Override
    public boolean isFinished() {
        return buffer == null;
    }
}
