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
}
