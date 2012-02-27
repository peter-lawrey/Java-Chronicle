package vanilla.java.chronicle.impl;

import vanilla.java.chronicle.Excerpt;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.logging.*;

import static vanilla.java.chronicle.impl.IntIndexedChronicleThroughputMain.deleteOnExit;

/**
 * @author peterlawrey
 */
public class FileLoggingMain {
    private static final int DATA_BIT_SIZE_HINT = 24;
    private static final boolean USE_UNSAFE = true;

    public static void main(String... args) throws IOException {
        int count = 1000000;
        long time1 = timeLogToChronicle(count);
        long time2 = timeLogToLogger(count);
        System.out.printf("To log %,d messages took %.3f seconds using Chronicle and %.3f seconds using Logger%n", count, time1 / 1e9, time2 / 1e9);
    }

    private static long timeLogToChronicle(int count) throws IOException {
        long start = System.nanoTime();

        final String basePath = "my.logger.log";
        deleteOnExit(basePath);

        IntIndexedChronicle tsc = new IntIndexedChronicle(basePath, DATA_BIT_SIZE_HINT);
        tsc.useUnsafe(USE_UNSAFE);

        Excerpt<IndexedChronicle> excerpt = tsc.createExcerpt();
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
        Logger logger = Logger.getLogger("vanilla.java.chronicle");
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
