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

import vanilla.java.affinity.AffinityLock;
import vanilla.java.affinity.AffinityStrategies;
import vanilla.java.busywaiting.BusyWaiter;
import vanilla.java.chronicle.Excerpt;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;

/**
 * @author peter.lawrey
 *         <p/>
 *         on a 4.6 GHz, i7-2600
 *         Took 6.325 seconds to write/read 100,000,000 entries, rate was 15.8 M entries/sec - ByteBuffer (tmpfs)
 *         Took 4.590 seconds to write/read 100,000,000 entries, rate was 21.8 M entries/sec - Using Unsafe (tmpfs)
 *         <p/>
 *         Took 7.352 seconds to write/read 100,000,000 entries, rate was 13.6 M entries/sec - ByteBuffer (ext4)
 *         Took 5.283 seconds to write/read 100,000,000 entries, rate was 18.9 M entries/sec- Using Unsafe (ext4)
 */
public class IntIndexedChronicleThroughputMain {

    public static final int DATA_BIT_SIZE_HINT = 24;
    public static final boolean USE_UNSAFE = false;
    public static final String base = "/tmp/deleteme.";
    public static final int runs = 50 * 1000 * 1000;

    public static void main(String... args) throws IOException, InterruptedException {
        final String basePath = base + "request";
        final String basePath2 = base + "response";
        deleteOnExit(basePath);
        deleteOnExit(basePath2);

        IntIndexedChronicle tsc = new IntIndexedChronicle(basePath, DATA_BIT_SIZE_HINT);
        tsc.useUnsafe(USE_UNSAFE);
        IntIndexedChronicle tsc2 = new IntIndexedChronicle(basePath2, DATA_BIT_SIZE_HINT);
        tsc2.useUnsafe(USE_UNSAFE);
        tsc.clear();

        AffinityLock al = AffinityLock.acquireLock(false);
        final AffinityLock al2 = al.acquireLock(AffinityStrategies.DIFFERENT_CORE);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                al2.bind();
                try {
                    StringBuilder sb = new StringBuilder();
                    final IntIndexedChronicle tsc = new IntIndexedChronicle(basePath, DATA_BIT_SIZE_HINT);
                    tsc.useUnsafe(USE_UNSAFE);
                    final IntIndexedChronicle tsc2 = new IntIndexedChronicle(basePath2, DATA_BIT_SIZE_HINT);
                    tsc2.useUnsafe(USE_UNSAFE);
                    tsc2.clear();

                    Excerpt excerpt = tsc.createExcerpt();
                    Excerpt excerpt2 = tsc2.createExcerpt();
                    for (int i = 0; i < runs; i++) {
                        while (!excerpt.index(i))
                            BusyWaiter.pause();

                        char type = excerpt.readChar();
                        if ('T' != type)
                            assertEquals('T', type);
                        int n = excerpt.readInt();
                        if (i != n)
                            assertEquals(i, n);
                        excerpt.readChars(sb);
                        excerpt.readLong();
                        excerpt.readDouble();
                        excerpt.finish();

                        excerpt2.startExcerpt(6);
                        excerpt2.writeChar('R');
                        excerpt2.writeInt(n);
                        excerpt2.finish();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    al2.release();
                }
            }
        });
        t.start();

        al.bind();
        Excerpt excerpt = tsc.createExcerpt();
        Excerpt excerpt2 = tsc2.createExcerpt();
        long start = System.nanoTime();
        int i2 = 0;
        for (int i = 0; i < runs; i++) {
            excerpt.startExcerpt(34);
            excerpt.writeChar('T');
            excerpt.writeInt(i);
            excerpt.writeChars("Hello");
            excerpt.writeLong(0L);
            excerpt.writeDouble(0.0);
            excerpt.finish();

            while (excerpt2.index(i2)) {
                char type = excerpt2.readChar();
                if ('R' != type)
                    assertEquals('R', type);
                int n = excerpt2.readInt();
                if (i2 != n)
                    assertEquals(i2, n);
                excerpt2.finish();
                i2++;
            }
        }

        for (; i2 < runs; i2++) {
            while (!excerpt2.index(i2))
                BusyWaiter.pause();
            char type = excerpt2.readChar();
            if ('R' != type)
                assertEquals('R', type);
            int n = excerpt2.readInt();
            if (i2 != n)
                assertEquals(i2, n);
            excerpt2.finish();
        }

        t.join();
        tsc.close();
        tsc2.close();
        long time = System.nanoTime() - start;
        System.out.printf("Took %.3f seconds to write/read %,d entries, rate was %.1f M entries/sec%n", time / 1e9, 2 * runs, 2 * runs * 1e3 / time);
        al.release();
    }

    private static void deleteOnExit(String basePath) {
        new File(basePath + ".data").deleteOnExit();
        new File(basePath + ".index").deleteOnExit();
    }
}
