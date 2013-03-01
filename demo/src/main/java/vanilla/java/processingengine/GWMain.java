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
import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;
import com.higherfrequencytrading.clock.ClockSupport;
import vanilla.java.processingengine.api.*;
import vanilla.java.processingengine.testing.Histogram;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author peter.lawrey
 */
public class GWMain {
    public static void main(String... args) throws IOException, InterruptedException {
        if (args.length < 0) {
            System.err.print("java " + GWMain.class.getName() + " [1 or 2]");
            System.exit(-1);
        }
        final int gwId = Integer.parseInt(args[0]);
        AffinitySupport.setAffinity(1 << (7 - gwId));

        int orders = 5000000;

        String tmp = System.getProperty("java.io.tmpdir");
        String gw2pePath = tmp + "/demo/gw2pe" + gwId;
        String pePath = tmp + "/demo/pe";

        // setup
        Chronicle gw2pe = new IndexedChronicle(gw2pePath);
        Gw2PeWriter gw2PeWriter = new Gw2PeWriter(gw2pe.createExcerpt());

        Chronicle pe2gw = new IndexedChronicle(pePath);
        final Histogram times = new Histogram(10000, 100);
        final AtomicInteger reportCount = new AtomicInteger();
        Pe2GwEvents listener = new Pe2GwEvents() {
            @Override
            public void report(MetaData metaData, SmallReport smallReport) {
                if (metaData.sourceId != gwId) return;

                times.sample(ClockSupport.nanoTime() - metaData.writeTimestampNanos);
                reportCount.getAndIncrement();
            }
        };
        Pe2GwReader pe2GwReader = new Pe2GwReader(gwId, pe2gw.createExcerpt(), listener);

        // synchronize the start.
        long startTime = System.currentTimeMillis() / 10000 * 10000;
        while (System.currentTimeMillis() < startTime + 10000)
            Thread.sleep(1);
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

            while (pe2GwReader.readOne() || reportCount.get() < i - 10) {
            }
        }

        while (reportCount.get() < orders) {
            pe2GwReader.readOne();
        }
        long time = System.nanoTime() - start;
        System.out.printf("Processed %,d events in and out in %.1f seconds%n", orders, time / 1e9);
        System.out.printf("The latency distribution was %.1f, %.1f/%.1f/%.1f us for the 1, 90/99/99.9 %%tile%n",
                times.percentile(0.01) / 1e3,
                times.percentile(0.90) / 1e3,
                times.percentile(0.99) / 1e3,
                times.percentile(0.999) / 1e3
        );
        gw2pe.close();
        pe2gw.close();
    }
}
