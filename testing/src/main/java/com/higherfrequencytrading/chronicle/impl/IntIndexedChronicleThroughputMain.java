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

package com.higherfrequencytrading.chronicle.impl;

import com.higherfrequencytrading.affinity.AffinityLock;
import com.higherfrequencytrading.affinity.AffinityStrategies;
import com.higherfrequencytrading.busywaiting.BusyWaiter;
import com.higherfrequencytrading.chronicle.Excerpt;

import java.io.IOException;

import static com.higherfrequencytrading.chronicle.impl.GlobalSettings.*;
import static junit.framework.Assert.assertEquals;

/**
 * @author peter.lawrey
 *         <p/>
 *         on a 4.6 GHz, i7-2600, Centos 6.2 -Dtest.size=100
 *         Took 6.325 seconds to write/read 100,000,000 entries, rate was 15.8 M entries/sec - ByteBuffer (tmpfs)
 *         Took 9.593 seconds to write/read 200,000,000 entries, rate was 20.8 M entries/sec - Using Unsafe (tmpfs)
 */
public class IntIndexedChronicleThroughputMain {

    public static void main(String... args) throws IOException, InterruptedException {
        final String basePath = BASE_DIR + "request";
        final String basePath2 = BASE_DIR + "response";
        deleteOnExit(basePath);
        deleteOnExit(basePath2);

        IntIndexedChronicle tsc = new IntIndexedChronicle(basePath);
        tsc.useUnsafe(USE_UNSAFE);
        IntIndexedChronicle tsc2 = new IntIndexedChronicle(basePath2);
        tsc2.useUnsafe(USE_UNSAFE);
        tsc.clear();

        AffinityLock al = AffinityLock.acquireLock(false);
        final AffinityLock al2 = al.acquireLock(AffinityStrategies.SAME_SOCKET, AffinityStrategies.DIFFERENT_CORE);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                al2.bind();
                try {
                    StringBuilder sb = new StringBuilder();
                    final IntIndexedChronicle tsc = new IntIndexedChronicle(basePath);
                    tsc.useUnsafe(USE_UNSAFE);
                    final IntIndexedChronicle tsc2 = new IntIndexedChronicle(basePath2);
                    tsc2.useUnsafe(USE_UNSAFE);
                    tsc2.clear();

                    Excerpt excerpt = tsc.createExcerpt();
                    Excerpt excerpt2 = tsc2.createExcerpt();
                    for (int i = 0; i < RUNS; i++) {
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
        for (int i = 0; i < RUNS; i++) {
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

        for (; i2 < RUNS; i2++) {
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
        System.out.printf("Took %.3f seconds to write/read %,d entries, rate was %.1f M entries/sec%n", time / 1e9, 2 * RUNS, 2 * RUNS * 1e3 / time);
        al.release();
    }
}
