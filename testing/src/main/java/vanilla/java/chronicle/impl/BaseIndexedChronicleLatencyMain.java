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
import vanilla.java.testing.Histogram;

import java.io.IOException;

import static vanilla.java.chronicle.impl.GlobalSettings.*;

/**
 * @author peter.lawrey
 *         <p/>
 *         on a 4.6 GHz, i7-2600, Centos 6.2
 *         The average RTT latency was 200 ns. The 50/99 / 99.9/99.99%tile latencies were 190/220 / 2,850/4,320. There were 5 delays over 100 μs - ByteBuffer (tmpfs)
 *         The average RTT latency was 189 ns. The 50/99 / 99.9/99.99%tile latencies were 180/220 / 2,820/4,280. There were 2 delays over 100 μs- Using Unsafe (tmpfs)
 */
public class BaseIndexedChronicleLatencyMain {

    public static void main(String... args) throws IOException, InterruptedException {
        final String basePath = BASE_DIR + "request";
        final String basePath2 = BASE_DIR + "response";
        deleteOnExit(basePath);
        deleteOnExit(basePath2);

        IndexedChronicle tsc = new IndexedChronicle(basePath, DATA_BIT_SIZE_HINT);
        tsc.useUnsafe(USE_UNSAFE);
        IndexedChronicle tsc2 = new IndexedChronicle(basePath2, DATA_BIT_SIZE_HINT);
        tsc2.useUnsafe(USE_UNSAFE);
        tsc.clear();


        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final IndexedChronicle tsc = new IndexedChronicle(basePath, DATA_BIT_SIZE_HINT);
                    tsc.useUnsafe(USE_UNSAFE);
                    final IndexedChronicle tsc2 = new IndexedChronicle(basePath2, DATA_BIT_SIZE_HINT);
                    tsc2.useUnsafe(USE_UNSAFE);
                    tsc2.clear();

                    Excerpt excerpt = tsc.createExcerpt();
                    Excerpt excerpt2 = tsc2.createExcerpt();
                    for (int i = 0; i < RUNS; i++) {
                        while (!excerpt.index(i)) ;

                        long time = excerpt.readLong();
                        excerpt.finish();

                        excerpt2.startExcerpt(8);
                        excerpt2.writeLong(time);
                        excerpt2.finish();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();

        Excerpt excerpt = tsc.createExcerpt();
        Excerpt excerpt2 = tsc2.createExcerpt();

        Histogram hist = new Histogram(100000, 1);
        long totalTime = 0, longDelays = 0;
        for (int i = 0; i < RUNS; i++) {
            excerpt.startExcerpt(8);
            excerpt.writeLong(System.nanoTime());
            excerpt.finish();

            while (!excerpt2.index(i)) ;

            long time1 = System.nanoTime();
            long time0 = excerpt2.readLong();
            excerpt2.finish();
            if (i >= WARMUP) {
                final long latency = time1 - time0;
                if (latency > 100000) {
                    longDelays++;
                    System.out.println(latency);
                }
                hist.sample(latency);
                totalTime += latency;
            }
        }

        t.join();
        tsc.close();
        tsc2.close();

        System.out.printf("The average RTT latency was %,d ns. The 50/99 / 99.9/99.99%%tile latencies were %,d/%,d / %,d/%,d. There were %,d delays over 100 μs%n",
                totalTime / RUNS, hist.percentile(0.5), hist.percentile(0.99), hist.percentile(0.999), hist.percentile(0.9999), longDelays);
    }
}
