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

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author peter.lawrey
 */
public class TcpHiccupMain {
    // total messages to send.
    static final int WARMUP = Integer.getInteger("warmup", 12000);
    // total messages to send.
    static final int RUNS = Integer.getInteger("runs", 10000000);
    // per milli-second. (note: every message is sent twice)
    static final int RATE = Integer.getInteger("rate", 25);
    // busy waiting
    static boolean BUSY = Boolean.getBoolean("busy");
    // number of tests
    static final int TESTS = Integer.getInteger("tests", 3);

    public static void main(String... args) throws IOException {
        String hostname = "localhost";
        int port = 65432;

        for (int i = 0; i < TESTS; i++) {
            for (int busy = 0; busy <= 1; busy++) {
                BUSY = busy != 0;

                switch (args.length) {
                    case 0:
                        Thread thread = new Thread(new Acceptor(port + i));
                        thread.setDaemon(true);
                        thread.start();
                        new Sender(hostname, port + i).run();
                        thread.interrupt();
                        break;

                    case 1:
                        new Acceptor(port).run();
                        // only run one.
                        return;

                    case 2:
                        new Sender(hostname, port).run();
                        break;
                }
            }
        }
    }

    static class Acceptor implements Runnable {
        private final ServerSocketChannel ssc;

        public Acceptor(int port) throws IOException {
            ssc = ServerSocketChannel.open();
            ssc.socket().setReuseAddress(true);
            for (int i = 0; i < 3; i++)
                try {
                    ssc.socket().bind(new InetSocketAddress(port));
                    break;
                } catch (BindException be) {
                    System.err.println("Failed to bind, retrying... ");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new IOException(e);
                    }
                }
        }

        @Override
        public void run() {
            try {
                ByteBuffer bb = ByteBuffer.allocateDirect(64 * 1024);
                System.err.println("Accepting connect on " + ssc.socket().getLocalPort());
                while (!Thread.interrupted()) {
                    SocketChannel sc = ssc.accept();
                    if (BUSY)
                        sc.configureBlocking(false);

                    String ra = sc.socket().getInetAddress().toString();
                    System.err.println("... connect to " + ra);
                    try {
                        sc.socket().setTcpNoDelay(true);
                        while (sc.read(bb) >= 0) {
                            bb.flip();
//                            System.out.println("e "+bb.remaining());
                            while (bb.remaining() > 0)
                                sc.write(bb);
                            bb.clear();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        sc.close();
                    }
                }
            } catch (ClosedByInterruptException ignored) {
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    ssc.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    static class Sender implements Runnable {
        private final SocketChannel sc;
        private final ExecutorService reader = Executors.newSingleThreadExecutor();
        private final Future<?> future;

        public Sender(String hostname, int port) throws IOException {
            sc = SocketChannel.open(new InetSocketAddress(hostname, port));
            if (BUSY)
                sc.configureBlocking(false);
            sc.socket().setTcpNoDelay(true);
            future = reader.submit(new Reader(sc));
        }

        @Override
        public void run() {
            try {
                ByteBuffer time = ByteBuffer.allocateDirect(12).order(ByteOrder.nativeOrder());
                long start = System.nanoTime();
                for (int i = 1; i <= WARMUP + RUNS; i++) {
                    long next = start + i * 1000000L / RATE;
                    while (System.nanoTime() < next) ;
                    time.clear();
                    // when it should have been sent, not when it was.
                    time.putInt(i);
                    time.putLong(next);
                    time.flip();
//                    System.out.println("w " + time.getInt(0) + " " + time.remaining());
                    while (time.remaining() > 0)
                        sc.write(time);
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
        private final SocketChannel sc;

        public Reader(SocketChannel sc) {
            this.sc = sc;
        }

        @Override
        public void run() {
            System.err.println("... starting reader.");
            Histogram warmup = new Histogram(1000, 10000);
            Histogram[] histograms = new Histogram[5];
            int scale = 1000;
            for (int i = 0; i < histograms.length; i++) {
                histograms[i] = new Histogram(1000, scale);
                scale *= 10;
            }
            ByteBuffer time = ByteBuffer.allocateDirect(4096).order(ByteOrder.nativeOrder());
            try {
                for (int i = 1; i <= WARMUP + RUNS; i++) {
                    while (time.position() < 12)
                        sc.read(time);
                    time.flip();
                    int sendI = time.getInt();
//                    System.out.println("r " + sendI + " " + time.remaining());
                    if (sendI != i)
                        throw new AssertionError("sendI=" + sendI + ", i=" + i);
                    long next = time.getLong();
                    long took = System.nanoTime() - next;
                    if (i >= WARMUP)
                        for (Histogram histogram : histograms) {
                            histogram.sample(took);
                        }
                    else
                        warmup.sample(took);
                    time.compact();
                }

                StringBuilder heading = new StringBuilder("runs\trate\twarmup\tbusy");
                StringBuilder values = new StringBuilder(RUNS + "\t" + RATE + "\t" + WARMUP + "\t" + BUSY);
                for (double perc : new double[]{50, 90, 93, 99, 99.3, 99.9, 99.93, 99.99, 99.993, 99.999}) {
                    double oneIn = 1.0 / (1 - perc / 100);
                    heading.append("\t").append(perc).append("%");
                    long value = findPercentile(histograms, perc);
                    values.append("\t").append(inNanos(value));
                    if (RUNS <= oneIn * oneIn)
                        break;
                }
                heading.append("\tworst");
                long worst = findPercentile(histograms, 99.9999);
                values.append("\t").append(inNanos(worst)).append("\tmicro-seconds");
                System.out.println(heading);
                System.out.println(values);
            } catch (Throwable e) {
                e.printStackTrace();
                System.exit(-1);

            } finally {
                try {
                    System.err.println("... disconnected from " + sc.socket().getInetAddress());
                    sc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private long findPercentile(Histogram[] histograms, double perc) {
            long value = Long.MAX_VALUE;
            for (Histogram histogram : histograms) {
                value = histogram.percentile(perc);
                if (value < Long.MAX_VALUE)
                    break;
            }
            return value;
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
