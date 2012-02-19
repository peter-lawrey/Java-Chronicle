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

/**
 * A String of bytes.  This is a mutable (not thread safe) array of up to 255 bytes.
 * <p/>
 * It is intended to be used for ASCII text based protocols with limited field lengths.  It perform no character encoding and has trivial Serialization, and De-serialization overhead.
 *
 * @author peter.lawrey
 */
public class ByteString implements CharSequence, Cloneable {
    private static final int MAX_LENGTH = 255;
    private final byte[] data;

    public ByteString() {
        this(MAX_LENGTH);
    }

    public ByteString(String text) {
        this(text.length());
        for (int i = 0, len = length(); i < len; i++)
            data[i + 1] = (byte) text.charAt(i);
    }

    public ByteString(int maxLength) {
        if (maxLength > MAX_LENGTH)
            throw new IllegalArgumentException("Length " + maxLength + " must be <= " + MAX_LENGTH);
        data = new byte[maxLength + 1];
    }

    @Override
    public int length() {
        return data[0] & 0xFF;
    }

    @Override
    public char charAt(int index) {
        if (index >= length()) throw new IndexOutOfBoundsException();
        return (char) (data[index + 1] & 0xFF);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (int i = 1, len = length(); i <= len; i++)
            hash = hash * 31 + (data[0] & 0xFF);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CharSequence)) return false;
        CharSequence cs = (CharSequence) obj;
        if (length() != cs.length()) return false;
        for (int i = 0, len = length(); i < len; i++)
            if (charAt(i) != cs.charAt(i)) return false;

        return true;
    }

    @Override
    public String toString() {
        int len = length();
        return new String(data, 0, 1, len);
    }

    public void clear() {
        data[0] = 0;
    }

    public void append(byte b) {
        int len = length();
        if (len >= 255)
            throw new IndexOutOfBoundsException("Cannot append len=" + len);
        data[len] = b;
        data[0]++;
    }
}
