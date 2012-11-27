package vanilla.java.chronicle.impl;

import vanilla.java.chronicle.EnumeratedMarshaller;
import vanilla.java.chronicle.Excerpt;

/**
 * @author plawrey
 */
public class StringMarshaller implements EnumeratedMarshaller<String> {
    private final String[] interner;

    public StringMarshaller(int size) {
        int size2 = 128;
        while (size2 < size && size2 < (1 << 20)) size2 <<= 1;
        interner = new String[size2];
    }

    @Override
    public Class<String> classMarshaled() {
        return String.class;
    }

    @Override
    public void write(Excerpt excerpt, String s) {
        excerpt.writeUTF(s);
    }

    @Override
    public String read(Excerpt excerpt) {
        String s = excerpt.readUTF();
        int h = s.hashCode();
        h ^= (h >>> 20) ^ (h >>> 12);
        h ^= (h >>> 7) ^ (h >>> 4);
        int idx = h & (interner.length - 1);
        String s2 = interner[idx];
        if (s.equals(s2))
            return s2;
        return interner[idx] = s;
    }
}
