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

package com.higherfrequencytrading.chronicle.impl;

import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.impl.old.OldIndexedChronicle;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * @author andrew.bissell
 */
public class NewVsOldNumberAppendTest {
    static final String TMP = System.getProperty("java.io.tmpdir");

    private static final int NUM_ENTRIES_PER_RECORD = 20;
    private static final int NUM_RECORDS = 400 * 1000;
    private static final int NUM_WARMUP_RECORDS = 40 * 1000;
    private static final int TOTAL_RECORDS = NUM_RECORDS + NUM_WARMUP_RECORDS;
    private static final long[][] RANDOM_LONGS = new long[TOTAL_RECORDS][NUM_ENTRIES_PER_RECORD];
    private static final double[][] RANDOM_DOUBLES = new double[TOTAL_RECORDS][NUM_ENTRIES_PER_RECORD];

    private static final Random random = new Random();

    public NewVsOldNumberAppendTest() {

    }

    @Before
    public void fillRandoms() {
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            for (int j = 0; j < NUM_ENTRIES_PER_RECORD; j++) {
                RANDOM_LONGS[i][j] = random.nextLong();
                RANDOM_DOUBLES[i][j] = random.nextDouble();
            }
        }
    }

    @Test
    public void testNumberAppends() throws IOException {
        System.gc();
        timeAppends(Generation.NEW, ExcerptType.BYTE_BUFFER);
        System.gc();
        timeAppends(Generation.NEW, ExcerptType.UNSAFE);
        System.gc();
        timeAppends(Generation.OLD, ExcerptType.BYTE_BUFFER);
        System.gc();
        timeAppends(Generation.OLD, ExcerptType.UNSAFE);
    }

    private enum Generation {
        OLD("old"),
        NEW("new");

        private final String stringRep;
        private Generation(String stringRep) {
            this.stringRep = stringRep;
        }

        private String getStringRep() {
            return stringRep;
        }
    }

    private enum ExcerptType {
        BYTE_BUFFER("ByteBuffer"),
        UNSAFE("Unsafe");

        private final String stringRep;
        private ExcerptType(String stringRep) {
            this.stringRep = stringRep;
        }

        private String getStringRep() {
            return stringRep;
        }
    }

    private void timeAppends(Generation gen, ExcerptType excerptType) throws IOException {
        String basePath = TMP + File.separator;

        String newPath = basePath + gen.getStringRep() + excerptType.getStringRep() + "Ic";
        deleteOnExit(newPath);
        AbstractChronicle newIc;
        if (gen == Generation.NEW) {
            newIc = new IndexedChronicle(newPath);
            if (excerptType == ExcerptType.UNSAFE) {
                ((IndexedChronicle) newIc).useUnsafe(true);
            }
        } else {
            newIc = new OldIndexedChronicle(newPath);
            if (excerptType == ExcerptType.UNSAFE) {
                ((OldIndexedChronicle) newIc).useUnsafe(true);
            }
        }

        Excerpt excerpt = newIc.createExcerpt();

        long start = 0;
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            if (start == 0 && i == NUM_WARMUP_RECORDS)
                start = System.nanoTime();

            excerpt.startExcerpt(2 * 10 * NUM_ENTRIES_PER_RECORD);
            for (int j = 0; j < NUM_ENTRIES_PER_RECORD; j++) {
                excerpt.append(RANDOM_LONGS[i][j]);
                // excerpt.append(RANDOM_DOUBLES[i][j]);
            }
            excerpt.finish();
        }
        newIc.close();

        long time = System.nanoTime() - start;
        System.out.println(gen + " " + excerptType + " time taken " +
                           (time / 1000000) + " ms");

        /*
        // Read out values to ensure they were correct and prevent possible DCE
        IndexedChronicle ic = new IndexedChronicle(newPath);
        ic.useUnsafe(excerptType == ExcerptType.UNSAFE);
        Excerpt readExcerpt = ic.createExcerpt();
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            boolean found = readExcerpt.nextIndex();
            if (!found)
                assertTrue(found);
            for (int j = 0; j < NUM_ENTRIES_PER_RECORD; j++) {
                long l = readExcerpt.readLong();
                double d = readExcerpt.readDouble();
                if (l != RANDOM_LONGS[i][j])
                    assertEquals(l, RANDOM_LONGS[i][j]);
                if (d != RANDOM_DOUBLES[i][j])
                    assertEquals(d, RANDOM_DOUBLES[i][j]);
            }
            readExcerpt.finish();
        }
        ic.close();
        */
    }

    private static void deleteOnExit(String basePath) {
        new File(basePath + ".data").deleteOnExit();
        new File(basePath + ".index").deleteOnExit();
    }
}


