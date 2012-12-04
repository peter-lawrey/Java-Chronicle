package vanilla.java.chronicle.tools;

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
}
