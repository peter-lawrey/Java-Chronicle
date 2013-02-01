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
        // NOTE: the sink and source must have different chronicle files.
        final int messages = 1000000;
        final Chronicle source = new InProcessChronicleSource(new IndexedChronicle("source", 14), PORT);
        ChronicleTest.deleteOnExit("source");
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Excerpt excerpt = source.createExcerpt();
                    for (int i = 1; i <= messages; i++) {
                        excerpt.startExcerpt(8);
                        excerpt.writeLong(i);
                        excerpt.finish();
//                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    throw new AssertionError(e);
                }

            }
        });

        Chronicle sink = new InProcessChronicleSink(new IndexedChronicle("sink", 14), "localhost", PORT);
        ChronicleTest.deleteOnExit("sink");

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
}
