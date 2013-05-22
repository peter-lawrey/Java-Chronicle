package com.higherfrequencytrading.chronicle.examples;

import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;
import com.higherfrequencytrading.chronicle.tools.ChronicleTools;

import java.io.IOException;

/**
 * @author peter.lawrey
 */
public class TestManyUpdatesMain {
    public static void main(String... ignored) throws IOException {
        String basePath = System.getProperty("java.io.tmpdir") + "/updates";
        ChronicleTools.deleteOnExit(basePath);
        long start = System.nanoTime();
        IndexedChronicle chronicle = new IndexedChronicle(basePath);
        int count = 1000 * 1000;
        for (Excerpt e = chronicle.createExcerpt(); e.index() < count; ) {
            e.startExcerpt(100);
            e.append("id=").append(e.index()).append(",name=lyj").append(e.index());
            e.finish();
        }
        chronicle.close();
        long time = System.nanoTime() - start;
        System.out.printf("%,d inserts took %.3f seconds%n", count, time / 1e9);
    }
}
