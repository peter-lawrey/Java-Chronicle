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

package vanilla.java.chronicle.impl;

import vanilla.java.chronicle.Excerpt;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author peter.lawrey
 */
public class TimeSeriesChroniclePerfMain {

    public static final int DATA_BIT_SIZE_HINT = 26;

    public static void main(String... args) throws FileNotFoundException, InterruptedException {
        final String basePath = "/tmp/deleteme.request";
        final String basePath2 = "/tmp/deleteme.response";
//        deleteOnExit(basePath);
//        deleteOnExit(basePath2);

        TimeSeriesChronicle tsc = new TimeSeriesChronicle(basePath, DATA_BIT_SIZE_HINT);
        TimeSeriesChronicle tsc2 = new TimeSeriesChronicle(basePath2, DATA_BIT_SIZE_HINT);
        tsc.clear();
        final int runs = 20 * 1000 * 1000;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StringBuilder sb = new StringBuilder();
                    final TimeSeriesChronicle tsc = new TimeSeriesChronicle(basePath, DATA_BIT_SIZE_HINT);
                    final TimeSeriesChronicle tsc2 = new TimeSeriesChronicle(basePath2, DATA_BIT_SIZE_HINT);
                    Excerpt excerpt = tsc.createExcerpt();
                    Excerpt excerpt2 = tsc2.createExcerpt();
                    for (int i = 0; i < runs; i++) {
                        while (!excerpt.index(i)) ;
                        int n = excerpt.readInt();
//                        if (i != n)
//                            assertEquals(i, n);
                        excerpt.readChars(sb);
                        excerpt.readLong();
                        excerpt.readDouble();
//                        excerpt.finish();

                        excerpt2.startExcerpt('R', 4);
                        excerpt2.writeInt(n);
                        excerpt2.finish();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
        Excerpt excerpt = tsc.createExcerpt();
        Excerpt excerpt2 = tsc2.createExcerpt();
        long start = System.nanoTime();
        for (int i = 0; i < runs; i++) {
            excerpt.startExcerpt('T', 32);
            excerpt.writeInt(i);
            excerpt.writeChars("Hello");
            excerpt.writeLong(0L);
            excerpt.writeDouble(0.0);
            excerpt.finish();
        }

        for (int i = 0; i < runs; i++) {
            while (!excerpt2.index(i)) ;
            int n = excerpt2.readInt();
//            if (i != n)
//                assertEquals(i, n);
//            excerpt2.finish();
        }

        t.join();
        tsc.close();
        tsc2.close();
        long time = System.nanoTime() - start;
        System.out.printf("Took %.3f seconds to write/read %,d entries, rate was %.1f M records/sec%n", time / 1e9, 2 * runs, 2 * runs * 1e3 / time);
    }

    private static void deleteOnExit(String basePath) {
        new File(basePath + ".data").deleteOnExit();
        new File(basePath + ".index").deleteOnExit();
    }
}
