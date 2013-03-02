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

package vanilla.java.processingengine;

import com.higherfrequencytrading.affinity.AffinitySupport;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;
import vanilla.java.processingengine.api.*;
import vanilla.java.processingengine.testing.Histogram;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author peter.lawrey
 */
public class GWMain {
    public static void main(String... args) throws IOException, InterruptedException {
        if (args.length < 2) {
            System.err.print("java " + GWMain.class.getName() + " [1 or 2] {throughput}");
            System.exit(-1);
        }
        final int gwId = Integer.parseInt(args[0]);
        final boolean throughputTest = Boolean.parseBoolean(args[1]);
        AffinitySupport.setAffinity(1 << (3 - gwId));

        int orders = 10000000;

        String tmp = System.getProperty("java.io.tmpdir");
        String gw2pePath = tmp + "/demo/gw2pe" + gwId;
        String pePath = tmp + "/demo/pe";

        // setup
        IndexedChronicle gw2pe = new IndexedChronicle(gw2pePath);
        gw2pe.useUnsafe(true);
        Gw2PeWriter gw2PeWriter = new Gw2PeWriter(gw2pe.createExcerpt());

        IndexedChronicle pe2gw = new IndexedChronicle(pePath);
        pe2gw.useUnsafe(true);
        final Histogram times = new Histogram(10000, 100);
        final AtomicInteger reportCount = new AtomicInteger();
        Pe2GwEvents listener = new Pe2GwEvents() {
            @Override
            public void report(MetaData metaData, SmallReport smallReport) {
                if (metaData.sourceId != gwId) return;

                if (!throughputTest)
                    times.sample(metaData.inReadTimestamp7Delta * 100);
                reportCount.getAndIncrement();
            }
        };
        Pe2GwReader pe2GwReader = new Pe2GwReader(gwId, pe2gw.createExcerpt(), listener);

        // synchronize the start.
        if (gwId > 1) {
            int startTime = (int) ((System.currentTimeMillis() / 1000 - 5) % 10) + 5;
            System.out.println("Count down");
            for (int i = startTime; i > 0; i--) {
                System.out.print(i + " ");
                System.out.flush();
                Thread.sleep(1000);
            }
        }
        System.out.println("Started");
        long start = System.nanoTime();
        // run loop
        SmallCommand command = new SmallCommand();
        StringBuilder clientOrderId = command.clientOrderId;
        for (int i = 0; i < orders; i++) {
            clientOrderId.setLength(0);
            clientOrderId.append("clientOrderId-");
            clientOrderId.append(gwId);
            clientOrderId.append('-');
            clientOrderId.append(i);
            command.instrument = "XAU/EUR";
            command.price = 1209.41;
            command.quantity = 1000;
            command.side = (i & 1) == 0 ? Side.BUY : Side.SELL;
            gw2PeWriter.small(null, command);

            if (throughputTest) {
                while (pe2GwReader.readOne()) ;
            } else {
                while (pe2GwReader.readOne() || reportCount.get() < i - 1) ;
            }
        }

        while (reportCount.get() < orders) {
            pe2GwReader.readOne();
        }
        long time = System.nanoTime() - start;
        System.out.printf("Processed %,d events in and out in %.1f seconds%n", orders, time / 1e9);
        if (!throughputTest) {
            System.out.printf("The latency distribution was %.1f, %.1f/%.1f/%.1f us for the 1, 90/99/99.9 %%tile%n",
                    times.percentile(0.01) / 1e3,
                    times.percentile(0.90) / 1e3,
                    times.percentile(0.99) / 1e3,
                    times.percentile(0.999) / 1e3
            );
        }
        gw2pe.close();
        pe2gw.close();
    }
}
