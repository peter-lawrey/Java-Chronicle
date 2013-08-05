package vanilla.java.stages;

import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;
import com.higherfrequencytrading.chronicle.tcp.InProcessChronicleSink;
import com.higherfrequencytrading.chronicle.tools.ChronicleTools;
import vanilla.java.stages.api.*;
import vanilla.java.stages.testing.Differencer;
import vanilla.java.stages.testing.RunningMinimum;
import vanilla.java.stages.testing.VanillaDifferencer;

import java.io.IOException;
import java.util.Arrays;

/**
 * User: peter
 * Date: 05/08/13
 * Time: 17:33
 */
public class SinkMain {
    static final String HOST3 = System.getProperty("host3", "localhost");
    static final int PORT3 = Integer.getInteger("port3", SourceMain.PORT + 2);
    static final String TMP = System.getProperty("java.io.tmpdir");

    public static void main(String... ignored) throws IOException, InterruptedException {
        String basePath3 = TMP + "/3-sink";

        ChronicleTools.deleteOnExit(basePath3);

        Chronicle chronicle3 = new IndexedChronicle(basePath3);
        InProcessChronicleSink sink3 = new InProcessChronicleSink(chronicle3, HOST3, PORT3);
        final EventsReader sinkReader = new EventsReader(sink3.createExcerpt(), new LatencyEvents(), TimingStage.SinkRead, TimingStage.EngineWrite);

        while (true) {
            if (!sinkReader.read())
                pause();
        }
    }

    private static void pause() {
        // nothing for now.
    }

    static class LatencyEvents implements Events {
        static final TimingStage[] VALUES = TimingStage.values();
        final int[][] timings = new int[VALUES.length - 1][SourceMain.MESSAGES];
        final Differencer[] differencers = {
                new VanillaDifferencer(), // same host
                new RunningMinimum(30 * 1000), // source to engine
                new VanillaDifferencer(), // same host
                new VanillaDifferencer(), // same host
                new VanillaDifferencer(), // same host
                new RunningMinimum(30 * 1000), // engine to sink
        };
        int count = 0;

        @Override
        public void onMarketData(MetaData metaData, Update update) {
            for (int i = 0; i < timings.length; i++) {
                long start = metaData.getTimeStamp(VALUES[i]);
                long end = metaData.getTimeStamp(VALUES[i + 1]);
                long delay = differencers[i].sample(start, end);
                timings[i][count] = (int) (delay / 1000);
            }
            count++;

            if (count == timings.length) {
                System.out.printf("latencies\t50%%\t90%%\t99%%\t99.9%%n");
                for (int i = 0; i < timings.length; i++) {
                    Arrays.sort(timings[i]);
                    System.out.printf("%s-%s\t%,d\t%,d\t%,d\t%,d%n",
                            VALUES[i].name(), VALUES[i + 1].name(),
                            timings[i][timings.length / 2],
                            timings[i][timings.length * 9 / 10],
                            timings[i][timings.length * 99 / 100],
                            timings[i][timings.length * 999 / 1000]
                    );
                }
                System.out.println();
                count = 0;
            }
        }
    }
}
