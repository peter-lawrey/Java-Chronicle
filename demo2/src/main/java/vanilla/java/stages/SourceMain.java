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
import com.higherfrequencytrading.chronicle.tcp.InProcessChronicleSource;
import com.higherfrequencytrading.chronicle.tools.ChronicleTools;
import vanilla.java.stages.api.EventsWriter;
import vanilla.java.stages.api.Update;

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
                update.acquireLevel().init(1.3256, 1e6, 1.3257, 2e6);
                update.acquireLevel().init(1.3255, 2e6, 1.3258, 4e6);
                update.acquireLevel().init(1.3254, 3e6, 1.3259, 6e6);
                update.acquireLevel().init(1.3253, 4e6, 1.3260, 8e6);
                update.acquireLevel().init(1.3252, 5e6, 1.3261, 10e6);
                writer.onMarketData(null, update);
            }
        }
        System.out.println("Messages written");
        Thread.sleep(1000);
        source.close();
    }
}
