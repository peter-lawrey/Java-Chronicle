package vanilla.java.chronicle;

/**
 * @author peter.lawrey
 */
public class AsciiString implements CharSequence, Cloneable {
    private static final int MAX_LENGTH = 255;
    private final byte[] data;

    public AsciiString() {
        this(MAX_LENGTH);
    }

    public AsciiString(String text) {
        this(text.length());
        for (int i = 0, len = length(); i < len; i++)
            data[i + 1] = (byte) text.charAt(i);
    }

    public AsciiString(int maxLength) {
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
        for(int i=0,len=length();i<len;i++)
            if (charAt(i) != cs.charAt(i)) return false;

            return true;
    }

    @Override
    public String toString() {
        int len = length();
        return new String(data, 0, 1, len);
    }
}
