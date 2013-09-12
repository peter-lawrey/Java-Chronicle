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

package com.higherfrequencytrading.chronicle;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.util.RandomAccess;

/**
 * @author peter.lawrey
 */
public interface RandomDataOutput extends ObjectOutput, RandomAccess {
    @Override
    void write(int b);

    @Override
    public void writeByte(int v);

    public void writeUnsignedByte(int v);

    void write(int offset, int b);

    public void writeUnsignedByte(int offset, int v);

    @Override
    void write(byte[] b);

    void write(int offset, byte[] b);

    @Override
    void write(byte[] b, int off, int len);

    @Override
    void writeBoolean(boolean v);

    void writeBoolean(int offset, boolean v);

    @Override
    void writeShort(int v);

    void writeShort(int offset, int v);

    void writeUnsignedShort(int v);

    void writeUnsignedShort(int offset, int v);

    void writeCompactShort(int v);

    void writeCompactUnsignedShort(int v);

    @Override
    void writeChar(int v);

    void writeChar(int offset, int v);

    /**
     * @param v 24-bit integer to write
     */
    void writeInt24(int v);

    void writeInt24(int offset, int v);

    @Override
    void writeInt(int v);

    void writeInt(int offset, int v);

    void writeUnsignedInt(long v);

    void writeUnsignedInt(int offset, long v);

    void writeCompactInt(int v);

    void writeCompactUnsignedInt(long v);

    /**
     * @param v 48-bit long to write
     */
    void writeInt48(long v);

    void writeInt48(int offset, long v);

    @Override
    void writeLong(long v);

    void writeLong(int offset, long v);

    void writeCompactLong(long v);

    /**
     * Stop bit encoding numbers. This will write the same number of bytes whether you used a byte, short or int.
     */
    void writeStopBit(long n);

    @Override
    void writeFloat(float v);

    void writeFloat(int offset, float v);

    @Override
    void writeDouble(double v);

    void writeDouble(int offset, double v);

    void writeCompactDouble(double v);

    @Override
    void writeBytes(@NotNull String s);

    void writeBytes(@NotNull CharSequence s);

    void writeBytes(int offset, @NotNull CharSequence s);

    @Override
    void writeChars(@NotNull String s);

    void writeChars(@NotNull CharSequence s);

    void writeChars(int offset, @NotNull CharSequence s);

    @Override
    void writeUTF(@Nullable String s);

    void writeUTF(@Nullable CharSequence s);

    void write(@NotNull ByteBuffer bb);

    // ObjectOutput

    @Override
    void writeObject(@Nullable Object obj);

    @Override
    void flush();

    @Override
    void close();
}
