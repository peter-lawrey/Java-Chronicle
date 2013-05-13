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

package com.higherfrequencytrading.chronicle.examples;

import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;

import java.io.IOException;

/**
 * @author peter.lawrey
 */
public class ExampleSimpleWriteReadMain {
    public static void main(String... args) throws IOException {
        final int runs = 100 * 1000000;
        long start = System.nanoTime();
        final String basePath = System.getProperty("user.home") + "/ExampleSimpleWriteReadMain";
//        ChronicleTools.deleteOnExit(basePath);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    IndexedChronicle ic = new IndexedChronicle(basePath);
                    ic.useUnsafe(true); // for benchmarks
                    Excerpt excerpt = ic.createExcerpt();
                    for (int i = 1; i <= runs; i++) {
                        excerpt.startExcerpt(17);
                        excerpt.writeUnsignedByte('M'); // message type
                        excerpt.writeLong(i); // e.g. time stamp
                        excerpt.writeDouble(i);
                        excerpt.finish();
                    }
                    ic.close();
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            }
        }).start();

        IndexedChronicle ic = new IndexedChronicle(basePath);
        ic.useUnsafe(true); // for benchmarks
        Excerpt excerpt = ic.createExcerpt();
        for (int i = 1; i <= runs; i++) {
            while (!excerpt.nextIndex()) {
                // busy wait
            }
            char ch = (char) excerpt.readUnsignedByte();
            long l = excerpt.readLong();
            double d = excerpt.readDouble();
            assert ch == 'M';
            assert l == i;
            assert d == i;
            excerpt.finish();
        }
        ic.close();

        long time = System.nanoTime() - start;
        System.out.printf("Took %.2f to write and read %,d entries%n", time / 1e9, runs);
    }
}
