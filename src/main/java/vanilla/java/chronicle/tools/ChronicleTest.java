package vanilla.java.chronicle.tools;

import vanilla.java.chronicle.Excerpt;

import java.io.File;

/**
 * @author plawrey
 */
public enum ChronicleTest {
    ;

    public static void deleteOnExit(String basePath) {
        new File(basePath + ".data").deleteOnExit();
        new File(basePath + ".index").deleteOnExit();
    }

    public static String asString(Excerpt excerpt) {
        return asString(excerpt, 256);
    }

    public static String asString(Excerpt excerpt, int length) {
        int position = excerpt.position();
        int limit = Math.min(position + length, excerpt.length());
        StringBuilder sb = new StringBuilder(limit - position);
        for (int i = position; i < limit; i++) {
            char ch = (char) excerpt.readUnsignedByte(i);
            if (ch < ' ' || ch > 127) ch = '.';
            sb.append(ch);
        }
        return sb.toString();
    }
}
