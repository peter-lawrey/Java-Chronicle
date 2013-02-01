package vanilla.java.chronicle.impl;

import org.junit.Test;
import vanilla.java.chronicle.Chronicle;
import vanilla.java.chronicle.Excerpt;
import vanilla.java.chronicle.tcp.InProcessChronicleSink;
import vanilla.java.chronicle.tcp.InProcessChronicleSource;
import vanilla.java.chronicle.tools.ChronicleTest;

import java.io.IOException;

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
        public void onPrice(String symbol, double bp, int bq, double ap, int aq);
    }

    static class PriceWriter implements PriceListener {
        private final Excerpt excerpt;

        PriceWriter(Excerpt excerpt) {
            this.excerpt = excerpt;
        }

        @Override
        public void onPrice(String symbol, double bp, int bq, double ap, int aq) {
            excerpt.startExcerpt(1 + (2 + symbol.length()) + 8 + 4 + 8 + 4);
            excerpt.writeByte('P'); // code for a price
            excerpt.writeEnum(symbol);
            excerpt.writeDouble(bp);
            excerpt.writeInt(bq);
            excerpt.writeDouble(ap);
            excerpt.writeInt(aq);
            excerpt.finish();
        }
    }

    static class PriceReader {
        private final Excerpt<Chronicle> excerpt;
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
                    String symbol = excerpt.readEnum(String.class);
                    double bp = excerpt.readDouble();
                    int bq = excerpt.readInt();
                    double ap = excerpt.readDouble();
                    int aq = excerpt.readInt();
                    listener.onPrice(symbol, bp, bq, ap, aq);
                    break;
                }
                default:
                    throw new AssertionError("Unexpected code " + ch);
            }
            return true;
        }
    }

    @Test
    public void testPricePublishing() throws IOException {
        String baseDir = System.getProperty("java.io.tmpdir");
        String sourceName = baseDir + "/price.source";
        Chronicle source = new InProcessChronicleSource(new IndexedChronicle(sourceName), PORT);
        ChronicleTest.deleteOnExit(sourceName);
        PriceWriter pw = new PriceWriter(source.createExcerpt());

        String sinkName = baseDir + "/price.sink";
        Chronicle sink = new InProcessChronicleSink(new IndexedChronicle(sinkName), "localhost", PORT);
        ChronicleTest.deleteOnExit(sinkName);

        PriceReader reader = new PriceReader(sink.createExcerpt(), new PriceListener() {
            @Override
            public void onPrice(String symbol, double bp, int bq, double ap, int aq) {
            }
        });
        long start = System.nanoTime();
        int prices = 1000000;
        for (int i = 0; i < prices; i++) {
            pw.onPrice("symbol", 99.9, 10, 100.1, 11);
        }

        long mid = System.nanoTime();
        for (int i = 0; i < prices; i++) {
            while (!reader.read()) ;
//            System.out.println(i);
        }
        long end = System.nanoTime();
        System.out.printf("Took an average of %.1f us to write and %.1f us to read",
                (mid - start) / prices / 1e3, (end - mid) / prices / 1e3);


        source.close();
        sink.close();
    }
}
