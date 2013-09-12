package com.higherfrequencytrading.chronicle.impl;

import com.higherfrequencytrading.chronicle.EnumeratedMarshaller;
import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.StopCharTester;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

/**
 * User: peter Date: 14/08/13 Time: 16:18
 */
public class BytesMarshaller implements EnumeratedMarshaller<byte[]> {
    @NotNull
    @Override
    public Class<byte[]> classMarshaled() {
        return byte[].class;
    }

    @Override
    public void write(@NotNull Excerpt excerpt, @NotNull byte[] bytes) {
        excerpt.writeStopBit(bytes.length);
        excerpt.write(bytes);
    }

    @NotNull
    @Override
    public byte[] read(@NotNull Excerpt excerpt) {
        int len = (int) excerpt.readStopBit();
        byte[] bytes = new byte[len];
        excerpt.read(bytes);
        return bytes;
    }

    @Override
    public byte[] parse(@NotNull Excerpt excerpt, @NotNull StopCharTester tester) {
        return excerpt.parseUTF(tester).getBytes(Charset.forName("UTF-8"));
    }
}
