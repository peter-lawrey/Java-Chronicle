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

package vanilla.java.chronicle;

import java.io.DataInput;
import java.nio.ByteBuffer;
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

    short readCompactShort();

    int readCompactUnsignedShort();

    @Override
    char readChar();

    char readChar(int offset);

    /**
     * @return a 24-bit integer value.
     */
    int readInt24();

    /**
     * @param offset of start.
     * @return a 24-bit integer value.
     */
    int readInt24(int offset);

    @Override
    int readInt();

    int readInt(int offset);

    long readUnsignedInt();

    long readUnsignedInt(int offset);

    int readCompactInt();

    long readCompactUnsignedInt();

    @Override
    long readLong();

    long readLong(int offset);

    /**
     * @return read a 48 bit long value.
     */
    long readInt48();

    /**
     * @return read a 48 bit long value.
     */
    long readInt48(int offset);

    long readCompactLong();

    @Override
    float readFloat();

    float readFloat(int offset);

    @Override
    double readDouble();

    double readDouble(int offset);

    double readCompactDouble();

    @Override
    String readLine();

    void readByteString(ByteString as);

    int readByteString(int offset, ByteString as);

    void readByteString(StringBuilder sb);

    int readByteString(int offset, StringBuilder sb);

    String readByteString();

    void readChars(StringBuilder sb);

    String readChars();

    @Override
    String readUTF();

    String readUTF(int offset);

    void read(ByteBuffer bb);
}
