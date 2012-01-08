package vanilla.java.chronicle;

import java.io.DataInput;
import java.io.IOException;
import java.util.RandomAccess;

/**
 * @author peter.lawrey
 */
public interface RandomDataInput extends DataInput, RandomAccess {
    @Override
    void readFully(byte[] b);

    @Override
    void readFully(byte[] b, int off, int len);

    @Override
    int skipBytes(int n);

    @Override
    boolean readBoolean();

    boolean readBoolean(int offset);

    @Override
    byte readByte();

    byte readByte(int offset);

    @Override
    int readUnsignedByte();

    int readUnsignedByte(int offset);

    @Override
    short readShort();

    short readShort(int offset);

    @Override
    int readUnsignedShort();

    int readUnsignedShort(int offset);

    @Override
    char readChar();

    char readChar(int offset);

    @Override
    int readInt();

    int readInt(int offset);

    @Override
    long readLong();

    long readLong(int offset);

    @Override
    float readFloat();

    float readFloat(int offset);

    @Override
    double readDouble();

    double readDouble(int offset);

    @Override
    String readLine();
    
    void readAscii(AsciiString as);
    void readAscii(int offset, AsciiString as);
    void readAscii(StringBuilder sb);
    void readAscii(int offset, StringBuilder sb);
    String readAscii();

    @Override
    String readUTF();
    String readUTF(int offset);
}
