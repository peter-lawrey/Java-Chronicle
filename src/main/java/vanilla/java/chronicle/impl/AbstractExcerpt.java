package vanilla.java.chronicle.impl;

import vanilla.java.chronicle.Chronicle;
import vanilla.java.chronicle.Excerpt;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author peter.lawrey
 */
public abstract class AbstractExcerpt implements Excerpt {
    private final Chronicle chronicle;
    protected long index;
    private int position = 0;
    private short type = 0;
    private int capacity = 0;

    protected AbstractExcerpt(Chronicle chronicle) {
        this.chronicle = chronicle;
    }

    @Override
    public Chronicle chronicle() {
        return chronicle;
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

    @Override
    public int skipBytes(int n) {
        int position = position();
        int n2 = Math.min(n, capacity - position);
        position(position + n2);
        return n2;
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
    public void write(byte[] b) {
        write(b, 0, b.length);
    }

    @Override
    public void writeBoolean(boolean v) {
        writeByte(v ? 0 : -1);
    }

    @Override
    public void writeBoolean(int offset, boolean v) {
        writeByte(offset, v ? 0 : -1);
    }

    @Override
    public void writeBytes(String s) {
        writeBytes((CharSequence) s);
    }

    @Override
    public void writeBytes(CharSequence s) {
        for (int i = 0, len = s.length(); i < len; i++)
            writeByte(s.charAt(i));
    }

    @Override
    public void writeBytes(int offset, CharSequence s) {
        for (int i = 0, len = s.length(); i < len; i++)
            writeByte(offset + i, s.charAt(i));
    }

    @Override
    public void writeChars(String s) {
        writeChars((CharSequence) s);
    }

    @Override
    public void writeChars(CharSequence s) {
        for (int i = 0, len = s.length(); i < len; i++)
            writeChar(s.charAt(i));
    }

    @Override
    public void writeChars(int offset, CharSequence s) {
        for (int i = 0, len = s.length(); i < len; i++)
            writeChar(offset + i, s.charAt(i));
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
}
