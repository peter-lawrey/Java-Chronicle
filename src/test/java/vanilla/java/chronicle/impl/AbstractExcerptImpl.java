package vanilla.java.chronicle.impl;

import vanilla.java.chronicle.*;

/**
 * @author peter.lawrey
 */
public class AbstractExcerptImpl extends AbstractExcerpt {
    final Excerpt e;

    public AbstractExcerptImpl(Excerpt e) {
        super(e.chronicle());
        this.e = e;
    }


    public boolean index(long index) throws IndexOutOfBoundsException {
        return e.index(index);
    }


    public void type(short type) {
        e.type(type);
    }


    public void startExcerpt(short type, int capacity) {
        e.startExcerpt(type, capacity);
    }

    public void finish() {
        e.finish();
    }

    @Override
    public void readFully(byte[] b, int off, int len) {
        e.readFully(b, off, len);
    }

    public byte readByte() {
        return e.readByte();
    }

    public byte readByte(int offset) {
        return e.readByte(offset);
    }

    public short readShort() {
        return e.readShort();
    }

    public short readShort(int offset) {
        return e.readShort(offset);
    }

    public char readChar() {
        return e.readChar();
    }

    public char readChar(int offset) {
        return e.readChar(offset);
    }

    public int readInt() {
        return e.readInt();
    }

    public int readInt(int offset) {
        return e.readInt(offset);
    }

    public long readLong() {
        return e.readLong();
    }

    public long readLong(int offset) {
        return e.readLong(offset);
    }

    public float readFloat() {
        return e.readFloat();
    }

    public float readFloat(int offset) {
        return e.readFloat(offset);
    }

    public double readDouble() {
        return e.readDouble();
    }

    public double readDouble(int offset) {
        return e.readDouble(offset);
    }

    public String readLine() {
        return e.readLine();
    }

    public void readAscii(AsciiString as) {
        e.readAscii(as);
    }

    public void readAscii(int offset, AsciiString as) {
        e.readAscii(offset, as);
    }

    public void readAscii(StringBuilder sb) {
        e.readAscii(sb);
    }

    public void readAscii(int offset, StringBuilder sb) {
        e.readAscii(offset, sb);
    }

    public String readAscii() {
        return e.readAscii();
    }

    public void write(int b) {
        e.write(b);
    }

    public void write(int offset, int b) {
        e.write(offset, b);
    }

    public void write(int offset, byte[] b) {
        e.write(offset, b);
    }

    public void write(byte[] b, int off, int len) {
        e.write(b, off, len);
    }

    public void writeByte(int v) {
        e.writeByte(v);
    }

    public void writeByte(int offset, int v) {
        e.writeByte(offset, v);
    }

    public void writeShort(int v) {
        e.writeShort(v);
    }

    public void writeShort(int offset, int v) {
        e.writeShort(offset, v);
    }

    public void writeChar(int v) {
        e.writeChar(v);
    }

    public void writeChar(int offset, int v) {
        e.writeChar(offset, v);
    }

    public void writeInt(int v) {
        e.writeInt(v);
    }

    public void writeInt(int offset, int v) {
        e.writeInt(offset, v);
    }

    public void writeLong(long v) {
        e.writeLong(v);
    }

    public void writeLong(int offset, long v) {
        e.writeLong(offset, v);
    }

    public void writeFloat(float v) {
        e.writeFloat(v);
    }

    public void writeFloat(int offset, float v) {
        e.writeFloat(offset, v);
    }

    public void writeDouble(double v) {
        e.writeDouble(v);
    }

    public void writeDouble(int offset, double v) {
        e.writeDouble(offset, v);
    }
}
