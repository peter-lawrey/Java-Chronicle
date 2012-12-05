package vanilla.java.chronicle.impl;

import vanilla.java.chronicle.EnumeratedMarshaller;
import vanilla.java.chronicle.Excerpt;
import vanilla.java.chronicle.StopCharTester;

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

    private final StringBuilder reader = new StringBuilder();

    @Override
    public String read(Excerpt excerpt) {
        reader.setLength(0);
        excerpt.readUTF(reader);
        return builderToString();
    }

    @Override
    public String parse(Excerpt excerpt, StopCharTester tester) {
        reader.setLength(0);
        excerpt.parseUTF(reader, tester);
        return builderToString();
    }

    private String builderToString() {
        int idx = hashFor(reader);
        String s2 = interner[idx];
        if (s2 != null && s2.length() == reader.length())
            NOT_FOUND:{
                for (int i = 0, len = s2.length(); i < len; i++) {
                    if (s2.charAt(i) != reader.charAt(i))
                        break NOT_FOUND;
                }
                return s2;
            }
        return interner[idx] = reader.toString();
    }

    private int hashFor(CharSequence cs) {
        int h = 0;

        for (int i = 0, length = cs.length(); i < length; i++)
            h = 31 * h + cs.charAt(i);

        h ^= (h >>> 20) ^ (h >>> 12);
        h ^= (h >>> 7) ^ (h >>> 4);
        return h & (interner.length - 1);
    }
}
