package vanilla.java.stages;

import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;
import com.higherfrequencytrading.chronicle.tcp.InProcessChronicleSource;
import com.higherfrequencytrading.chronicle.tools.ChronicleTools;
import vanilla.java.stages.api.EventsWriter;
import vanilla.java.stages.api.Update;
import vanilla.java.stages.api.UpdateLevel;

import java.io.IOException;

/**
 * User: peter
 * Date: 05/08/13
 * Time: 17:32
 */
public class SourceMain {
    static final int RATE = Integer.getInteger("rate", 10); // per milli-second.
    static final int MESSAGES = Integer.getInteger("messages", 100 * 1000);
    static final int PORT = Integer.getInteger("port", 54321);
    static final String TMP = System.getProperty("java.io.tmpdir");

    public static void main(String... ignored) throws IOException, InterruptedException {
        String basePath = TMP + "/1-source";
        ChronicleTools.deleteOnExit(basePath);

        Chronicle chronicle = new IndexedChronicle(basePath);
        InProcessChronicleSource source = new InProcessChronicleSource(chronicle, PORT);

        EventsWriter writer = new EventsWriter(source);

        Update update = new Update();
        System.out.println("Allowing connection.");
        Thread.sleep(1000);
        System.out.println("Sending messages.");

        for (int i = 0; i < MESSAGES; i += RATE) {
            Thread.sleep(1);
            for (int j = 0; j < RATE; j++) {
                update.resetLevels("EUR/USD");
                for (int k = 0; k < 5; k++) {
                    UpdateLevel upLevel = update.acquireLevel();
                }
                writer.onMarketData(null, update);
            }
        }
        System.out.println("Messages written");
        Thread.sleep(1000);
        source.close();
    }
}
