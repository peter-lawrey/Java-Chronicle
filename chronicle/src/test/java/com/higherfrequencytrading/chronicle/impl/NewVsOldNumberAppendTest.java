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
    private static final int NUM_RECORDS = 200 * 1000;
    private static final int NUM_WARMUP_RECORDS = 40 * 1000;
    private static final int TOTAL_RECORDS = NUM_RECORDS + NUM_WARMUP_RECORDS;
    private static final long[][] RANDOM_LONGS = new long[TOTAL_RECORDS][NUM_ENTRIES_PER_RECORD];
    private static final double[][] RANDOM_DOUBLES = new double[TOTAL_RECORDS][NUM_ENTRIES_PER_RECORD];
    private static final int MAX_PRECISION = 8;

    private static final Random random = new Random();

    public NewVsOldNumberAppendTest() {

    }

    @Before
    public void fillRandoms() {
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            for (int j = 0; j < NUM_ENTRIES_PER_RECORD; j++) {
                RANDOM_LONGS[i][j] = random.nextLong();
                RANDOM_DOUBLES[i][j] = Math.max(123456.789,
                                                random.nextDouble() % (10 * 1000 * 1000));
            }
        }
    }

    private enum NumberType {
        LONG,
        DOUBLE
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

    @Test
    public void testNumberAppends() throws IOException {
        for (NumberType type : NumberType.values()) {
            System.gc();
            timeAppends(Generation.NEW, ExcerptType.BYTE_BUFFER, type);
            System.gc();
            timeAppends(Generation.NEW, ExcerptType.UNSAFE, type);
            System.gc();
            timeAppends(Generation.OLD, ExcerptType.BYTE_BUFFER, type);
            System.gc();
            timeAppends(Generation.OLD, ExcerptType.UNSAFE, type);
        }
    }

    private void timeAppends(Generation gen,
                             ExcerptType excerptType,
                             NumberType numType) throws IOException {
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
                if (numType == NumberType.LONG)
                    excerpt.append(RANDOM_LONGS[i][j]);
                else if (numType == NumberType.DOUBLE)
                    excerpt.append(RANDOM_DOUBLES[i][j], (j % MAX_PRECISION + 2));
                else
                    throw new AssertionError();
            }
            excerpt.finish();
        }
        newIc.close();

        long time = System.nanoTime() - start;
        System.out.println(numType + " " + gen + " " + excerptType + " time taken " +
                           (time / 1000000) + " ms");
    }

    private static void deleteOnExit(String basePath) {
        new File(basePath + ".data").deleteOnExit();
        new File(basePath + ".index").deleteOnExit();
    }
}


