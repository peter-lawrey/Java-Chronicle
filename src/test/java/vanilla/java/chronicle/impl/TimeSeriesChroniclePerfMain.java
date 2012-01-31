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
    public static void main(String... args) throws FileNotFoundException {
        String basePath = "/tmp/deleteme";
        deleteOnExit(basePath);

        TimeSeriesChronicle tsc = new TimeSeriesChronicle(basePath, 24);
        tsc.clear();
        Excerpt excerpt = tsc.createExcerpt();
        int runs = 10 * 1000 * 1000;
        long start = System.nanoTime();
        for (int i = 0; i < runs; i++) {
            excerpt.startExcerpt('T', 32);
            excerpt.writeChars("Hello");
            excerpt.writeInt(0);
            excerpt.writeLong(0L);
            excerpt.writeDouble(0.0);
            excerpt.finish();
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < runs; i++) {
            excerpt.index(i);
            excerpt.readChars(sb);
            excerpt.readInt();
            excerpt.readLong();
            excerpt.readDouble();
            excerpt.finish();
        }
        tsc.close();
        long time = System.nanoTime() - start;
        System.out.printf("Took %.3f seconds to write/read %,d entries%n", time / 1e9, runs);
    }

    private static void deleteOnExit(String basePath) {
        new File(basePath + ".data").deleteOnExit();
        new File(basePath + ".index").deleteOnExit();
    }
}
