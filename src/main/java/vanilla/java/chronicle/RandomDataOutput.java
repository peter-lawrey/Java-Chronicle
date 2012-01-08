package vanilla.java.chronicle;

import java.io.DataOutput;
import java.io.IOException;
import java.util.RandomAccess;

/**
 * @author peter.lawrey
 */
public interface RandomDataOutput extends DataOutput, RandomAccess {
    @Override
    void write(int b);

    void write(int offset, int b);

    @Override
    void write(byte[] b);

    void write(int offset, byte[] b);

    @Override
    void write(byte[] b, int off, int len);

    @Override
    void writeBoolean(boolean v);

    void writeBoolean(int offset, boolean v);

    @Override
    void writeByte(int v);
    void writeByte(int offset, int v);

    @Override
    void writeShort(int v);
    void writeShort(int offset, int v);

    @Override
    void writeChar(int v);
    void writeChar(int offset, int v);

    @Override
    void writeInt(int v);
    void writeInt(int offset, int v);

    @Override
    void writeLong(long v);
    void writeLong(int offset, long v);

    @Override
    void writeFloat(float v);
    void writeFloat(int offset, float v);

    @Override
    void writeDouble(double v);
    void writeDouble(int offset, double v);

    @Override
    void writeBytes(String s);
    void writeBytes(CharSequence s);
    void writeBytes(int offset, CharSequence s);

    @Override
    void writeChars(String s);
    void writeChars(CharSequence s);
    void writeChars(int offset, CharSequence s);

    @Override
    void writeUTF(String s);
}
