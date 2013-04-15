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

package com.higherfrequencytrading.hiccup;

import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;
import com.higherfrequencytrading.chronicle.tcp.InProcessChronicleSink;
import com.higherfrequencytrading.chronicle.tcp.InProcessChronicleSource;
import com.higherfrequencytrading.chronicle.tools.ChronicleTools;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * TODO More than a few bugs to fix, but the test basically runs.
 *
 * @author peter.lawrey
 */
public class ChronicleTcpHiccupMain {
    // total messages to send.
    static final int WARMUP = Integer.getInteger("warmup", 12000);
    // total messages to send.
    static int RUNS = Integer.getInteger("runs", 200000);
    // per milli-second. (note: every message is sent twice)
    static int RATE = Integer.getInteger("rate", 10);
    // number of tests
    static final int TESTS = Integer.getInteger("tests", 3);
    // directory for chronicles
    static final String DIR = System.getProperty("dir", System.getProperty("java.io.tmp"));

    public static void main(String... args) throws IOException {
        String hostname = "localhost", hostname2 = "localhost";
        int port = 65410, port2 = 65420;

        switch (args.length) {
            case 0:
                for (int i = 0; i < TESTS; i++) {
                    ChronicleTools.deleteOnExit(DIR + "/sender-" + i);
                    ChronicleTools.deleteOnExit(DIR + "/repeater-" + i);
                    ChronicleTools.deleteOnExit(DIR + "/receiver-" + i);

                    Thread thread = new Thread(new Repeater(i, port + i, hostname, port2 + i));
                    thread.setDaemon(true);
                    thread.start();
                    new Sender(i, hostname, port + i, port2 + i).run();
                    thread.interrupt();
                }
                break;

            case 3:
                try {
                    // local host and remote hostname/port
                    port = Integer.parseInt(args[0]);
                    hostname2 = args[1];
                    port2 = Integer.parseInt(args[2]);
                    ChronicleTools.deleteOnExit(DIR + "/repeater-0");
                    new Repeater(0, port, hostname2, port2).run();

                } catch (NumberFormatException e) {
                    // or remote hostname/port and local port
                    hostname = args[0];
                    port = Integer.parseInt(args[1]);
                    port2 = Integer.parseInt(args[2]);

                    ChronicleTools.deleteOnExit(DIR + "/sender-0");
                    ChronicleTools.deleteOnExit(DIR + "/receiver-0");
                    for (int rate : new int[]{5, 10, 25}) {
                        RATE = rate;
                        RUNS = rate * 10000;
                        for (int i = 0; i < TESTS; i++) {
                            new Sender(0, hostname, port, port2).run();
                        }
                    }
                }
                break;
        }
    }


    static class Repeater implements Runnable {
        private final InProcessChronicleSink chronicle;

        public Repeater(int test, int port, String hostname2, int port2) throws IOException {
            IndexedChronicle store = new IndexedChronicle(DIR + "/repeater-" + test);
            InProcessChronicleSource source = new InProcessChronicleSource(store, port);
            InProcessChronicleSink sink = new InProcessChronicleSink(source, hostname2, port2);
            chronicle = sink;
        }

        @Override
        public void run() {
            Excerpt excerpt = chronicle.createExcerpt();
            while (!Thread.interrupted()) {
                excerpt.nextIndex();
            }
            chronicle.close();
        }
    }

    static class Sender implements Runnable {
        private final ExecutorService reader = Executors.newSingleThreadExecutor();
        private final Future<?> future;
        private final InProcessChronicleSink sink;
        private final InProcessChronicleSource source;

        public Sender(int test, String hostname, int port, int port2) throws IOException {
            IndexedChronicle receiverStore = new IndexedChronicle(DIR + "/receiver-" + test);
            IndexedChronicle senderStore = new IndexedChronicle(DIR + "/sender-" + test);
            sink = new InProcessChronicleSink(receiverStore, hostname, port);
            source = new InProcessChronicleSource(senderStore, port2);
            future = reader.submit(new Reader(sink));
        }

        @Override
        public void run() {
            try {
                long start = System.nanoTime();
                Excerpt excerpt = source.createExcerpt();
                for (int i = 1; i <= WARMUP + RUNS; i++) {
                    long next = start + i * 1000000L / RATE;
                    while (System.nanoTime() < next) ;
                    // when it should have been sent, not when it was.
                    excerpt.startExcerpt(128);
                    excerpt.writeLong(next);
                    excerpt.position(128);
                    excerpt.finish();
                }
                future.get();

            } catch (Throwable e) {
                e.printStackTrace();
                System.exit(-1);
            }
            reader.shutdown();
        }
    }

    static class Reader implements Runnable {
        private final InProcessChronicleSink sink;

        public Reader(InProcessChronicleSink sink) {
            this.sink = sink;
        }

        @Override
        public void run() {
            System.err.println("... starting reader.");
            Histogram warmup = new Histogram(1000, 100, 7);
            Histogram histo = new Histogram(1000, 100, 7);

            Excerpt excerpt = sink.createExcerpt();
            try {
                for (int i = 0; i < WARMUP + RUNS; i++) {
                    while (!excerpt.index(i)) ;
                    long next = excerpt.readLong();
                    long took = System.nanoTime() - next;
                    if (i >= WARMUP)
                        histo.sample(took);
                    else
                        warmup.sample(took);
                    excerpt.finish();
                }

                StringBuilder heading = new StringBuilder("runs\trate\twarmup");
                StringBuilder values = new StringBuilder(RUNS + "\t" + RATE + "\t" + WARMUP);
                for (double perc : new double[]{50, 90, 99, 99.9, 99.99, 99.999}) {
                    double oneIn = 1.0 / (1 - perc / 100);
                    heading.append("\t").append(perc).append("%");
                    long value = histo.percentile(perc);
                    values.append("\t").append(inNanos(value));
                    if (RUNS <= oneIn * oneIn)
                        break;
                }
                heading.append("\tworst");
                long worst = histo.percentile(100);
                values.append("\t").append(inNanos(worst)).append("\tmicro-seconds");
                System.out.println(heading);
                System.out.println(values);
            } catch (Throwable e) {
                e.printStackTrace();
                System.exit(-1);

            } finally {
                sink.close();
            }
        }

        private String inNanos(long value) {
            return (value < 1e4) ? "" + value / 1e3 :
                    (value < 1e6) ? "" + value / 1000 :
                            (value < 1e7) ? value / 1e6 + "e3" :
                                    (value < 1e9) ? value / 1000000 + "e3" :
                                            value / 1e9 + "e6";
        }
    }
}
