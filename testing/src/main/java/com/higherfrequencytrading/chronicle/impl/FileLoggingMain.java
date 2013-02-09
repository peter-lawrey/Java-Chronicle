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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.logging.*;

import static com.higherfrequencytrading.chronicle.impl.GlobalSettings.USE_UNSAFE;
import static com.higherfrequencytrading.chronicle.impl.GlobalSettings.deleteOnExit;

/**
 * To log 2,000,000 messages took 0.416 seconds using Chronicle and 15.507 seconds using Logger
 */
public class FileLoggingMain {
    public static void main(String... args) throws IOException {
        int count = 2 * 1000 * 1000;
        long time1 = timeLogToChronicle(count);
        long time2 = timeLogToLogger(count);
        System.out.printf("To log %,d messages took %.3f seconds using Chronicle and %.3f seconds using Logger%n", count, time1 / 1e9, time2 / 1e9);
    }

    private static long timeLogToChronicle(int count) throws IOException {
        long start = System.nanoTime();

        final String basePath = System.getProperty("java.io.tmpdir", "/tmp") + "/my.logger.log";
        deleteOnExit(basePath);

        IntIndexedChronicle tsc = new IntIndexedChronicle(basePath);
        tsc.useUnsafe(USE_UNSAFE);

        Excerpt excerpt = tsc.createExcerpt();
        double d = 0.001, factor = 1 + 10.0 / count;
        long timeInMS = System.currentTimeMillis() % 86400000;
        for (int i = 0; i < count; i++) {
            d *= factor;
            excerpt.startExcerpt(128);
            excerpt.appendTime(timeInMS).append(" [ ");
            excerpt.append(Thread.currentThread().getName()).append(" ] FINE ");
            excerpt.append("result= ").append(d, 6).append('\n');
            excerpt.finish();
        }
        tsc.close();
        return System.nanoTime() - start;
    }

    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss.SSS");

    private static long timeLogToLogger(int count) throws IOException {
        long start = System.nanoTime();
        FileHandler handler = new FileHandler("my.logger.log");
        handler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return TIME.format(record.getMillis()) + " [ " + Thread.currentThread().getName() + " ] "
                        + record.getLevel() + " " + record.getMessage() + "\n";
            }
        });

        // Add to the desired logger
        Logger logger = Logger.getLogger("com.higherfrequencytrading.chronicle");
        logger.addHandler(handler);
        logger.setLevel(Level.FINE);

        double d = 0.001, factor = 1 + 10.0 / count;
        for (int i = 0; i < count; i++) {
            d *= factor;
            logger.log(Level.FINE, "result= " + d);
        }
        handler.close();
        return System.nanoTime() - start;
    }
}
