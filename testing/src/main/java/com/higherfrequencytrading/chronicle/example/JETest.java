package com.higherfrequencytrading.chronicle.example;

import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.impl.IntIndexedChronicle;
import com.higherfrequencytrading.chronicle.tools.ChronicleTools;

/**
 * @author Julien Eluard
 * @author peter.lawrey
 */
public class JETest {
    public static void main(String[] args) throws Exception {
        final String basePath = "test";
        ChronicleTools.deleteOnExit(basePath);
        final Chronicle chronicle = new IntIndexedChronicle(basePath);
        final Excerpt excerpt = chronicle.createExcerpt();
        final int[] consolidates = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        int repeats = 10000;
        for (int i = 0; i < repeats; i++) {
            excerpt.startExcerpt(8 + 4 + 4 * consolidates.length);
            excerpt.writeLong(System.nanoTime());
            excerpt.writeInt(consolidates.length);
            for (final int consolidate : consolidates) {
                excerpt.writeInt(consolidate);
            }
            excerpt.finish();
        }

        long[] times = new long[repeats];
        int[] nbcs = new int[repeats];
        int count = 0;
        final Excerpt excerpt2 = chronicle.createExcerpt();
        while (excerpt2.nextIndex()) {
            final long timestamp = excerpt2.readLong();
            long time = System.nanoTime() - timestamp;
            times[count] = time;
            final int nbConsolidates = excerpt2.readInt();
            nbcs[count] = nbConsolidates;
            for (int i = 0; i < nbConsolidates; i++) {
                excerpt2.readInt();
            }
            excerpt2.finish();
            count++;
        }
        for (int i = 0; i < count; i++) {
            System.out.print("latency: " + times[i] / repeats / 1e3 + " us average, ");
            System.out.println("nbConsolidates: " + nbcs[i]);
        }
        chronicle.close();
    }
}