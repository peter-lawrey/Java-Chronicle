package com.higherfrequencytrading.chronicle.impl;

import com.higherfrequencytrading.chronicle.ByteStringAppender;
import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.StopCharTester;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author plawrey
 */
public class WrappedExcerpt implements Excerpt {
    private final Excerpt excerpt;

    public WrappedExcerpt(Excerpt excerpt) {
        this.excerpt = excerpt;
    }

    public Chronicle chronicle() {
        return excerpt.chronicle();
    }

    public boolean nextIndex() {
        return excerpt.nextIndex();
    }

    public boolean index(long index) throws IndexOutOfBoundsException {
        return excerpt.index(index);
    }

    public void startExcerpt(int capacity) {
        excerpt.startExcerpt(capacity);
    }

    public void finish() {
        excerpt.finish();
    }

    public long index() {
        return excerpt.index();
    }

    public Excerpt position(int position) {
        return excerpt.position(position);
    }

    public int position() {
        return excerpt.position();
    }

    public int capacity() {
        return excerpt.capacity();
    }

    public int remaining() {
        return excerpt.remaining();
    }

    public void readFully(byte[] b) {
        excerpt.readFully(b);
    }

    public int skipBytes(int n) {
        return excerpt.skipBytes(n);
    }

    public void readFully(byte[] b, int off, int len) {
        excerpt.readFully(b, off, len);
    }

    public boolean readBoolean() {
        return excerpt.readBoolean();
    }

    public boolean readBoolean(int offset) {
        return excerpt.readBoolean(offset);
    }

    public int readUnsignedByte() {
        return excerpt.readUnsignedByte();
    }

    public int readUnsignedByte(int offset) {
        return excerpt.readUnsignedByte(offset);
    }

    public int readUnsignedShort() {
        return excerpt.readUnsignedShort();
    }

    public int readUnsignedShort(int offset) {
        return excerpt.readUnsignedShort(offset);
    }

    public String readLine() {
        return excerpt.readLine();
    }

    public String readUTF() {
        return excerpt.readUTF();
    }

    public void readUTF(Appendable appendable) {
        excerpt.readUTF(appendable);
    }

    public String parseUTF(StopCharTester tester) {
        return excerpt.parseUTF(tester);
    }

    public void parseUTF(Appendable builder, StopCharTester tester) {
        excerpt.parseUTF(builder, tester);
    }

    public String readUTF(int offset) {
        return excerpt.readUTF(offset);
    }

    public short readCompactShort() {
        return excerpt.readCompactShort();
    }

    public int readCompactUnsignedShort() {
        return excerpt.readCompactUnsignedShort();
    }

    public int readInt24() {
        return excerpt.readInt24();
    }

    public int readInt24(int offset) {
        return excerpt.readInt24(offset);
    }

    public long readUnsignedInt() {
        return excerpt.readUnsignedInt();
    }

    public long readUnsignedInt(int offset) {
        return excerpt.readUnsignedInt(offset);
    }

    public int readCompactInt() {
        return excerpt.readCompactInt();
    }

    public long readCompactUnsignedInt() {
        return excerpt.readCompactUnsignedInt();
    }

    public long readInt48() {
        return excerpt.readInt48();
    }

    public long readInt48(int offset) {
        return excerpt.readInt48(offset);
    }

    public long readCompactLong() {
        return excerpt.readCompactLong();
    }

    public double readCompactDouble() {
        return excerpt.readCompactDouble();
    }

    public void readByteString(StringBuilder sb) {
        excerpt.readByteString(sb);
    }

    public int readByteString(int offset, StringBuilder sb) {
        return excerpt.readByteString(offset, sb);
    }

    public String readByteString() {
        return excerpt.readByteString();
    }

    public void readChars(StringBuilder sb) {
        excerpt.readChars(sb);
    }

    public String readChars() {
        return excerpt.readChars();
    }

    public ByteOrder order() {
        return excerpt.order();
    }

    public void read(ByteBuffer bb) {
        excerpt.read(bb);
    }

    public void write(byte[] b) {
        excerpt.write(b);
    }

    public void writeBoolean(boolean v) {
        excerpt.writeBoolean(v);
    }

    public void writeBoolean(int offset, boolean v) {
        excerpt.writeBoolean(offset, v);
    }

    public void writeBytes(String s) {
        excerpt.writeBytes(s);
    }

    public void writeBytes(CharSequence s) {
        excerpt.writeBytes(s);
    }

    public void writeBytes(int offset, CharSequence s) {
        excerpt.writeBytes(offset, s);
    }

    public void writeChars(String s) {
        excerpt.writeChars(s);
    }

    public void writeChars(CharSequence s) {
        excerpt.writeChars(s);
    }

    public void writeChars(int offset, CharSequence s) {
        excerpt.writeChars(offset, s);
    }

    public void writeUTF(String s) {
        excerpt.writeUTF(s);
    }

    public void writeUTF(CharSequence str) {
        excerpt.writeUTF(str);
    }

    public void writeByte(int v) {
        excerpt.writeByte(v);
    }

    public void writeUnsignedByte(int v) {
        excerpt.writeUnsignedByte(v);
    }

    public void writeUnsignedByte(int offset, int v) {
        excerpt.writeUnsignedByte(offset, v);
    }

    public void write(int offset, byte[] b) {
        excerpt.write(offset, b);
    }

    public void write(byte[] b, int off, int len) {
        excerpt.write(b, off, len);
    }

    public void writeUnsignedShort(int v) {
        excerpt.writeUnsignedShort(v);
    }

    public void writeUnsignedShort(int offset, int v) {
        excerpt.writeUnsignedShort(offset, v);
    }

    public void writeCompactShort(int v) {
        excerpt.writeCompactShort(v);
    }

    public void writeCompactUnsignedShort(int v) {
        excerpt.writeCompactUnsignedShort(v);
    }

    public void writeInt24(int v) {
        excerpt.writeInt24(v);
    }

    public void writeInt24(int offset, int v) {
        excerpt.writeInt24(offset, v);
    }

    public void writeUnsignedInt(long v) {
        excerpt.writeUnsignedInt(v);
    }

    public void writeUnsignedInt(int offset, long v) {
        excerpt.writeUnsignedInt(offset, v);
    }

    public void writeCompactInt(int v) {
        excerpt.writeCompactInt(v);
    }

    public void writeCompactUnsignedInt(long v) {
        excerpt.writeCompactUnsignedInt(v);
    }

    public void writeInt48(long v) {
        excerpt.writeInt48(v);
    }

    public void writeInt48(int offset, long v) {
        excerpt.writeInt48(offset, v);
    }

    public void writeCompactLong(long v) {
        excerpt.writeCompactLong(v);
    }

    public void writeCompactDouble(double v) {
        excerpt.writeCompactDouble(v);
    }

    public void write(ByteBuffer bb) {
        excerpt.write(bb);
    }

    public int length() {
        return excerpt.length();
    }

    public ByteStringAppender append(CharSequence s) {
        return excerpt.append(s);
    }

    public ByteStringAppender append(CharSequence s, int start, int end) {
        return excerpt.append(s, start, end);
    }

    public ByteStringAppender append(Enum value) {
        return excerpt.append(value);
    }

    public ByteStringAppender append(byte[] str) {
        return excerpt.append(str);
    }

    public ByteStringAppender append(byte[] str, int offset, int len) {
        return excerpt.append(str, offset, len);
    }

    public ByteStringAppender append(boolean b) {
        return excerpt.append(b);
    }

    public ByteStringAppender append(char c) {
        return excerpt.append(c);
    }

    public ByteStringAppender append(int num) {
        return excerpt.append(num);
    }

    public ByteStringAppender append(long num) {
        return excerpt.append(num);
    }

    public ByteStringAppender appendTime(long timeInMS) {
        return excerpt.appendTime(timeInMS);
    }

    public ByteStringAppender append(double d) {
        return excerpt.append(d);
    }

    public double parseDouble() {
        return excerpt.parseDouble();
    }

    public long parseLong() {
        return excerpt.parseLong();
    }

    public ByteStringAppender append(double d, int precision) {
        return excerpt.append(d, precision);
    }

    public InputStream inputStream() {
        return excerpt.inputStream();
    }

    public OutputStream outputStream() {
        return excerpt.outputStream();
    }

    public <E> void writeEnum(E o) {
        excerpt.writeEnum(o);
    }

    public <E> E readEnum(Class<E> aClass) {
        return excerpt.readEnum(aClass);
    }

    public <E> E parseEnum(Class<E> aClass, StopCharTester tester) {
        return excerpt.parseEnum(aClass, tester);
    }

    public <E> void writeEnums(Collection<E> eList) {
        excerpt.writeEnums(eList);
    }

    public <K, V> void writeMap(Map<K, V> map) {
        excerpt.writeMap(map);
    }

    public <E> List<E> readEnums(Class<E> aClass) {
        return excerpt.readEnums(aClass);
    }

    public <K, V> Map<K, V> readMap(Class<K> aClass, Class<V> aClass2) {
        return excerpt.readMap(aClass, aClass2);
    }

    public byte readByte() {
        return excerpt.readByte();
    }

    public byte readByte(int offset) {
        return excerpt.readByte(offset);
    }

    public short readShort() {
        return excerpt.readShort();
    }

    public short readShort(int offset) {
        return excerpt.readShort(offset);
    }

    public char readChar() {
        return excerpt.readChar();
    }

    public char readChar(int offset) {
        return excerpt.readChar(offset);
    }

    public int readInt() {
        return excerpt.readInt();
    }

    public int readInt(int offset) {
        return excerpt.readInt(offset);
    }

    public long readLong() {
        return excerpt.readLong();
    }

    public long readLong(int offset) {
        return excerpt.readLong(offset);
    }

    public float readFloat() {
        return excerpt.readFloat();
    }

    public float readFloat(int offset) {
        return excerpt.readFloat(offset);
    }

    public double readDouble() {
        return excerpt.readDouble();
    }

    public double readDouble(int offset) {
        return excerpt.readDouble(offset);
    }

    public void write(int b) {
        excerpt.write(b);
    }

    public void write(int offset, int b) {
        excerpt.write(offset, b);
    }

    public void writeShort(int v) {
        excerpt.writeShort(v);
    }

    public void writeShort(int offset, int v) {
        excerpt.writeShort(offset, v);
    }

    public void writeChar(int v) {
        excerpt.writeChar(v);
    }

    public void writeChar(int offset, int v) {
        excerpt.writeChar(offset, v);
    }

    public void writeInt(int v) {
        excerpt.writeInt(v);
    }

    public void writeInt(int offset, int v) {
        excerpt.writeInt(offset, v);
    }

    public void writeLong(long v) {
        excerpt.writeLong(v);
    }

    public void writeLong(int offset, long v) {
        excerpt.writeLong(offset, v);
    }

    public void writeFloat(float v) {
        excerpt.writeFloat(v);
    }

    public void writeFloat(int offset, float v) {
        excerpt.writeFloat(offset, v);
    }

    public void writeDouble(double v) {
        excerpt.writeDouble(v);
    }

    public void writeDouble(int offset, double v) {
        excerpt.writeDouble(offset, v);
    }

    @Override
    public Object readObject() {
        return excerpt.readObject();
    }

    @Override
    public int read() {
        return excerpt.read();
    }

    @Override
    public int read(byte[] b) {
        return excerpt.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) {
        return excerpt.read(b, off, len);
    }

    @Override
    public long skip(long n) {
        return excerpt.skip(n);
    }

    @Override
    public int available() {
        return excerpt.available();
    }

    @Override
    public void close() {
        try {
            excerpt.close();
        } catch (Exception keepIntelliJHappy) {
        }
    }

    @Override
    public void writeObject(Object obj) {
        excerpt.writeObject(obj);
    }

    @Override
    public void flush() {
        excerpt.flush();
    }

    @Override
    public <E> void writeList(Collection<E> list) {
        writeList(list);
    }

    @Override
    public void readList(Collection list) {
        readList(list);
    }
}
