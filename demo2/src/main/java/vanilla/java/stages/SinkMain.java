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

package vanilla.java.stages;

import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;
import com.higherfrequencytrading.chronicle.tcp.InProcessChronicleSink;
import com.higherfrequencytrading.chronicle.tools.ChronicleTools;
import org.jetbrains.annotations.NotNull;
import vanilla.java.stages.api.*;
import vanilla.java.stages.testing.Differencer;
import vanilla.java.stages.testing.RunningMinimum;
import vanilla.java.stages.testing.VanillaDifferencer;

import java.io.IOException;
import java.util.Arrays;

/**
 * User: peter Date: 05/08/13 Time: 17:33
 */

/*
All on one machine

latencies	50%	90%	99%	99.9%	99.99%
Start-SourceWrite	0.1	0.3	1.2	2	5 us
SourceWrite-SourceRead	9.9	13.3	20.1	40	57 us
SourceRead-EngineWrite	0.9	1.3	1.5	4	11 us
EngineWrite-EngineRead	0.8	1.2	4.7	6	12 us
EngineRead-SinkWrite	0.3	0.5	1.1	1	4 us
SinkWrite-SinkRead	10.7	13.8	19.3	42	78 us
Start-SinkRead	23.4	29.2	39.9	61	125 us

On a home 1 Gb/s network.
latencies	50%	90%	99%	99.9%	99.99%
Start-SourceWrite	0.3	1.0	1.6	12	75 us
SourceWrite-SourceRead	88.8	152.7	1279.6	1391	5416 us
SourceRead-EngineWrite	0.3	0.9	1.0	2	11 us
EngineWrite-EngineRead	1.6	3.1	4.5	6	15 us
EngineRead-SinkWrite	0.3	0.5	1.1	1	5 us
SinkWrite-SinkRead	86.8	127.8	202.8	1345	4375 us
 */
public class SinkMain {
    static final String HOST3 = System.getProperty("host3", "localhost");
    static final int PORT3 = Integer.getInteger("port3", SourceMain.PORT + 2);
    static final int WARMUP = SourceMain.WARMUP;
    static final int MESSAGES = SourceMain.MESSAGES;
    static final String TMP = System.getProperty("java.io.tmpdir");
    public static final int NET_LATENCY = Integer.getInteger("net.latency", 30 * 1000);

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
        final long[][] timings = new long[VALUES.length - 1][SourceMain.MESSAGES];
        final long[] endToEndTimings = new long[SourceMain.MESSAGES];
        final Differencer[] differencers = {
                new VanillaDifferencer(), // same host
                NET_LATENCY == 0 ? new VanillaDifferencer() : new RunningMinimum(NET_LATENCY), // source to engine
                new VanillaDifferencer(), // same host
                new VanillaDifferencer(), // same host
                new VanillaDifferencer(), // same host
                NET_LATENCY == 0 ? new VanillaDifferencer() : new RunningMinimum(NET_LATENCY), // engine to sink
        };
        int count = -WARMUP;

        @Override
        public void onMarketData(@NotNull MetaData metaData, Update update) {
            int c = count < 0 ? 0 : count;
            for (int i = 0; i < timings.length; i++) {
                long start = metaData.getTimeStamp(VALUES[i]);
                long end = metaData.getTimeStamp(VALUES[i + 1]);
                long delay = differencers[i].sample(start, end);
                timings[i][c] = delay;
            }
            endToEndTimings[c] = metaData.getTimeStamp(VALUES[VALUES.length - 1]) -
                    metaData.getTimeStamp(VALUES[0]);
            if (count % 10000 == 0) {
                System.out.println(count);
//                for (int i = 0; i < VALUES.length; i++) {
//                    System.out.println(VALUES[i] + ": " + metaData.getTimeStamp(VALUES[i]));
//                }
            }
            count++;

            if (count == MESSAGES) {
                System.out.printf("latencies\t50%%\t90%%\t99%%\t99.9%%\t99.99%%%n");
                for (int i = 0; i < timings.length; i++) {
                    Arrays.sort(timings[i]);
                    long t50 = timings[i][count / 2];
                    long t90 = timings[i][count * 9 / 10];
                    long t99 = timings[i][count * 99 / 100];
                    long t99_9 = timings[i][count * 999 / 1000];
                    long t99_99 = timings[i][count - count / 10000];
                    System.out.printf("%s-%s\t%s\t%s\t%s\t%s\t%s us%n",
                            VALUES[i].name(), VALUES[i + 1].name(),
                            t50 / 100 / 10.0,
                            t90 / 100 / 10.0,
                            t99 / 100 / 10.0,
                            t99_9 / 1000,
                            t99_99 / 1000
                    );
                }
                Arrays.sort(endToEndTimings);
                long t50 = endToEndTimings[count / 2];
                long t90 = endToEndTimings[count * 9 / 10];
                long t99 = endToEndTimings[count * 99 / 100];
                long t99_9 = endToEndTimings[count * 999 / 1000];
                long t99_99 = endToEndTimings[count - count / 10000];
                System.out.printf("%s-%s\t%s\t%s\t%s\t%s\t%s us%n",
                        VALUES[0].name(), VALUES[VALUES.length - 1].name(),
                        t50 / 100 / 10.0,
                        t90 / 100 / 10.0,
                        t99 / 100 / 10.0,
                        t99_9 / 1000,
                        t99_99 / 1000
                );
                System.out.println();
                count = 0;
            }
        }
    }
}
