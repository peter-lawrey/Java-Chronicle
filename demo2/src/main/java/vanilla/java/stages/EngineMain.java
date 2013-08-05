package vanilla.java.stages;

import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;
import com.higherfrequencytrading.chronicle.tcp.InProcessChronicleSink;
import com.higherfrequencytrading.chronicle.tcp.InProcessChronicleSource;
import com.higherfrequencytrading.chronicle.tools.ChronicleTools;
import vanilla.java.stages.api.*;

import java.io.IOException;

/**
 * User: peter
 * Date: 05/08/13
 * Time: 17:32
 */
public class EngineMain {
    static final String HOST = System.getProperty("host", "localhost");
    static final int PORT = Integer.getInteger("port", 54321);
    static final int PORT3 = Integer.getInteger("port3", PORT + 2);
    static final String TMP = System.getProperty("java.io.tmpdir");

    public static void main(String... ignored) throws IOException, InterruptedException {
        String basePath1 = TMP + "/1-sink";
        String basePath2 = TMP + "/2-engine";
        String basePath3 = TMP + "/3-source";

        ChronicleTools.deleteOnExit(basePath1);
        ChronicleTools.deleteOnExit(basePath2);
        ChronicleTools.deleteOnExit(basePath3);

        Chronicle chronicle2w = new IndexedChronicle(basePath2);
        EventsWriter writer2 = new EventsWriter(chronicle2w);

        Chronicle chronicle1 = new IndexedChronicle(basePath1);
        InProcessChronicleSink sink = new InProcessChronicleSink(chronicle1, HOST, PORT);
        final EventsReader sinkReader = new EventsReader(sink.createExcerpt(), new BrokerEvents(writer2), TimingStage.SinkRead, TimingStage.EngineWrite);

        Thread cbt = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("CB: Waiting to consume events");
                // first broker thread.
                while (true) {
                    if (!sinkReader.read())
                        pause();
                }
            }
        }, "central broker");
        cbt.start();

        Chronicle chronicle3 = new IndexedChronicle(basePath3);
        Chronicle source3 = new InProcessChronicleSource(chronicle3, PORT3);
        EventsWriter writer3 = new EventsWriter(source3);

        Chronicle chronicle2r = new IndexedChronicle(basePath2);
        final EventsReader reader2 = new EventsReader(chronicle2r.createExcerpt(),
                new BrokerEvents(writer3), TimingStage.EngineRead, TimingStage.SinkWrite);

        Thread pet = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("PE: Waiting to consume events");
                // first broker thread.
                while (true) {
                    if (!reader2.read())
                        pause();
                }
            }
        }, "processing engine");
        pet.start();

    }

    private static void pause() {
        // nothing for now.
    }

    static class BrokerEvents implements Events {
        final EventsWriter writer;

        BrokerEvents(EventsWriter writer) {
            this.writer = writer;
        }

        @Override
        public void onMarketData(MetaData metaData, Update update) {
            // don't do anything with the updates, just pass them on.
            writer.onMarketData(metaData, update);
        }
    }
}
