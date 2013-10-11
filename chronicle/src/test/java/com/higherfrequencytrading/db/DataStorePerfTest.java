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

package com.higherfrequencytrading.db;

import com.higherfrequencytrading.chronicle.datamodel.DataStore;
import com.higherfrequencytrading.chronicle.datamodel.MapWrapper;
import com.higherfrequencytrading.chronicle.datamodel.ModelMode;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;
import com.higherfrequencytrading.chronicle.tools.ChronicleTools;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Random;

/**
 * User: peter
 * Date: 14/08/13
 * Time: 15:59
 */
public class DataStorePerfTest {
    private static final String TMP = System.getProperty("java.io.tmpdir");

    public static void main(String[] args) throws IOException {
        String basePath = TMP + "/DataStorePerfTest";
        ChronicleTools.deleteOnExit(basePath);
        IndexedChronicle chronicle = new IndexedChronicle(basePath);
        chronicle.useUnsafe(true);
        DataStore dataStore = new DataStore(chronicle, ModelMode.MASTER);
        MapWrapper<String, byte[]> map = new MapWrapper<String, byte[]>(dataStore, "map",
                String.class, byte[].class, new LinkedHashMap<String, byte[]>(), 160);
        dataStore.add("map", map);
        dataStore.start();

        // generate keys and values
        int keyCount = 100000;
        String[] keys = new String[keyCount];
        byte[][] bytes = new byte[keyCount][];
        Random rand = new Random();
        for (int i = 0; i < keyCount; i++) {
            StringBuilder key = new StringBuilder();
            do {
                key.append(Integer.toString(rand.nextInt() & Integer.MAX_VALUE, 36));
            } while (key.length() < 16);
            key.setLength(16);
            keys[i] = key.toString();
            bytes[i] = new byte[100];
        }
        Collections.sort(Arrays.asList(keys));

        for (int t = 0; t < 10; t++) {
            map.clear();

            long time0 = System.nanoTime();
            // Start with sequential writes
            for (int i = 0; i < keyCount; i++) {
                map.put(keys[i], bytes[i]);
            }
            long time1 = System.nanoTime();
            // Sequential reads
            for (int i = 0; i < keyCount; i++) {
                byte[] bytes0 = map.get(keys[i]);
            }
            long time2 = System.nanoTime();
            String[] keys2 = keys.clone();
            Collections.shuffle(Arrays.asList(keys2));
            long time3 = System.nanoTime();
            // random writes
            for (int i = 0; i < keyCount; i++) {
                map.put(keys[i], bytes[i]);
            }
            long time4 = System.nanoTime();
            // random reads
            for (int i = 0; i < keyCount; i++) {
                byte[] bytes0 = map.get(keys[i]);
            }
            long time5 = System.nanoTime();
            System.out.printf("Seq write: %,d K/s, Seq read: %,d K/s, Rnd write: %,d K/s, Rnd read: %,d K/s%n",
                    keyCount * 1000000L / (time1 - time0),
                    keyCount * 1000000L / (time2 - time1),
                    keyCount * 1000000L / (time4 - time3),
                    keyCount * 1000000L / (time5 - time4)
            );
        }
        chronicle.close();
    }
}
