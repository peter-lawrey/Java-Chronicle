package com.higherfrequencytrading.chronicle.impl;

import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.tcp.InProcessChronicleSink;
import com.higherfrequencytrading.chronicle.tcp.InProcessChronicleSource;
import com.higherfrequencytrading.chronicle.tools.ChronicleTest;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.assertEquals;

/**
 * @author plawrey
 */
public class InProcessChronicleTest {

    public static final int PORT = 12345;

    @Test
    public void testOverTCP() throws IOException, InterruptedException {
        String baseDir = System.getProperty("java.io.tmpdir");
        // NOTE: the sink and source must have different chronicle files.
        final int messages = 3000000;
        final Chronicle source = new InProcessChronicleSource(new IndexedChronicle(baseDir + "/source"), PORT);
        ChronicleTest.deleteOnExit(baseDir + "/source");
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Excerpt excerpt = source.createExcerpt();
                    for (int i = 1; i <= messages; i++) {
                        // use a size which will cause mis-alignment.
                        excerpt.startExcerpt(9);
                        excerpt.writeLong(i);
                        excerpt.writeByte(i);
                        excerpt.finish();
//                        Thread.sleep(1);
                    }
                    System.out.println(System.currentTimeMillis() + ": Finished writing messages");
                } catch (Exception e) {
                    throw new AssertionError(e);
                }

            }
        });

        Chronicle sink = new InProcessChronicleSink(new IndexedChronicle(baseDir + "/sink"), "localhost", PORT);
        ChronicleTest.deleteOnExit(baseDir + "/sink");

        long start = System.nanoTime();
        t.start();
        Excerpt excerpt = sink.createExcerpt();
        int count = 0;
        for (int i = 1; i <= messages; i++) {
            while (!excerpt.nextIndex())
                count++;
            long n = excerpt.readLong();
            assertEquals(i, n);
            excerpt.finish();
        }
        sink.close();
        System.out.println("There were " + count + " misses");
        t.join();
        source.close();
        long time = System.nanoTime() - start;
        System.out.printf("Messages per second %,d", (int) (messages * 1e9 / time));
    }

    interface PriceListener {
        public void onPrice(long timeInMicros, String symbol, double bp, int bq, double ap, int aq);
    }

    static class PriceWriter implements PriceListener {
        private final Excerpt excerpt;

        PriceWriter(Excerpt excerpt) {
            this.excerpt = excerpt;
        }

        @Override
        public void onPrice(long timeInMicros, String symbol, double bp, int bq, double ap, int aq) {
            excerpt.startExcerpt(1 + 8 + (2 + symbol.length()) + 8 + 4 + 8 + 4);
            excerpt.writeByte('P'); // code for a price
            excerpt.writeLong(timeInMicros);
            excerpt.writeEnum(symbol);
            excerpt.writeDouble(bp);
            excerpt.writeInt(bq);
            excerpt.writeDouble(ap);
            excerpt.writeInt(aq);
            excerpt.finish();
        }
    }

    static class PriceReader {
        private final Excerpt excerpt;
        private final PriceListener listener;

        PriceReader(Excerpt excerpt, PriceListener listener) {
            this.excerpt = excerpt;
            this.listener = listener;
        }

        public boolean read() {
            if (!excerpt.nextIndex()) return false;
            char ch = (char) excerpt.readByte();
            switch (ch) {
                case 'P': {
                    long timeInMicros = excerpt.readLong();
                    String symbol = excerpt.readEnum(String.class);
                    double bp = excerpt.readDouble();
                    int bq = excerpt.readInt();
                    double ap = excerpt.readDouble();
                    int aq = excerpt.readInt();
                    listener.onPrice(timeInMicros, symbol, bp, bq, ap, aq);
                    break;
                }
                default:
                    throw new AssertionError("Unexpected code " + ch);
            }
            return true;
        }
    }

    // Took an average of 0.42 us to write and 0.61 us to read (Java 6)
    // Took an average of 0.35 us to write and 0.59 us to read (Java 7)

    @Test
    public void testPricePublishing() throws IOException, InterruptedException {
        String baseDir = System.getProperty("java.io.tmpdir");
        String sourceName = baseDir + "/price.source";
        Chronicle source = new InProcessChronicleSource(new IndexedChronicle(sourceName), PORT);
        ChronicleTest.deleteOnExit(sourceName);
        PriceWriter pw = new PriceWriter(source.createExcerpt());

        String sinkName = baseDir + "/price.sink";
        Chronicle sink = new InProcessChronicleSink(new IndexedChronicle(sinkName), "localhost", PORT);
        ChronicleTest.deleteOnExit(sinkName);

        final AtomicInteger count = new AtomicInteger();
        PriceReader reader = new PriceReader(sink.createExcerpt(), new PriceListener() {
            @Override
            public void onPrice(long timeInMicros, String symbol, double bp, int bq, double ap, int aq) {
                count.incrementAndGet();
            }
        });
        pw.onPrice(1, "symbol", 99.9, 1, 100.1, 2);
        reader.read();

        long start = System.nanoTime();
        int prices = 12000000;
        for (int i = 1; i <= prices; i++) {
            pw.onPrice(i, "symbol", 99.9, i, 100.1, i + 1);
        }

        long mid = System.nanoTime();
        while (count.get() < prices)
            reader.read();

        long end = System.nanoTime();
        System.out.printf("Took an average of %.2f us to write and %.2f us to read",
                (mid - start) / prices / 1e3, (end - mid) / prices / 1e3);


        source.close();
        sink.close();
    }

    static class PriceUpdate implements Externalizable, Serializable {
        private long timeInMicros;
        private String symbol;
        private double bp;
        private int bq;
        private double ap;
        private int aq;

        public PriceUpdate() {
        }

        PriceUpdate(long timeInMicros, String symbol, double bp, int bq, double ap, int aq) {
            this.timeInMicros = timeInMicros;
            this.symbol = symbol;
            this.bp = bp;
            this.bq = bq;
            this.ap = ap;
            this.aq = aq;
        }

        //        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeLong(timeInMicros);
            out.writeUTF(symbol);
            out.writeDouble(bp);
            out.writeInt(bq);
            out.writeDouble(ap);
            out.writeInt(aq);
        }

        //        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            timeInMicros = in.readLong();
            symbol = in.readUTF();
            bp = in.readDouble();
            bq = in.readInt();
            ap = in.readDouble();
            aq = in.readInt();
        }
    }

    // Took an average of 2.8 us to write and 7.6 us to read (Java 7)
    @Test
    public void testSerializationPerformance() throws IOException, ClassNotFoundException {
        List<byte[]> bytes = new ArrayList<byte[]>();
        long start = System.nanoTime();
        int prices = 1000000;
        for (int i = 0; i < prices; i++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            PriceUpdate pu = new PriceUpdate(1 + i, "symbol", 99.9, i + 1, 100.1, i + 2);
            oos.writeObject(pu);
            oos.close();
            bytes.add(baos.toByteArray());
        }

        long mid = System.nanoTime();
        for (byte[] bs : bytes) {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bs));
            PriceUpdate pu = (PriceUpdate) ois.readObject();
        }

        long end = System.nanoTime();
        System.out.printf("Took an average of %.1f us to write and %.1f us to read",
                (mid - start) / prices / 1e3, (end - mid) / prices / 1e3);
    }
}
