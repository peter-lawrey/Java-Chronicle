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

package com.higherfrequencytrading.chronicle.datamodel;

import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;
import com.higherfrequencytrading.chronicle.tcp.InProcessChronicleSink;
import com.higherfrequencytrading.chronicle.tcp.InProcessChronicleSource;
import com.higherfrequencytrading.chronicle.tools.ChronicleTools;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author peter.lawrey
 */
public class MapWrapperTest {
    static final String TMP = System.getProperty("java.io.tmpdir");

    @Test
    public void testMethods() throws IOException {
        String name = TMP + "/set-methods";
        ChronicleTools.deleteOnExit(name);
        {
            MapListener stringsListener = createMock("strings", MapListener.class);
            stringsListener.eventStart(1, "strings");
            stringsListener.add("Hello", "hi");
            stringsListener.eventEnd(true);

            stringsListener.eventStart(3, "strings");
            stringsListener.add("World", "all");
            stringsListener.eventEnd(true);

            MapListener intListener = createMock("ints", MapListener.class);
            for (int i = 0; i < 3; i++) {
                intListener.eventStart(i * 2, "ints");
                intListener.add(i, i + 1000);
                intListener.eventEnd(true);
            }

            stringsListener.eventStart(5, "strings");
            stringsListener.onEvent("bye");
            stringsListener.eventEnd(true);

            intListener.eventStart(6, "ints");
            intListener.onEvent("now");
            intListener.eventEnd(true);

            replay(stringsListener);
            replay(intListener);
            Chronicle chronicle = new IndexedChronicle(name);
            DataStore dataStore = new DataStore(chronicle, ModelMode.MASTER);
            MapWrapper<String, String> strings = new MapWrapper<String, String>(dataStore, "strings", String.class, String.class, new LinkedHashMap<String, String>(), 16);
            strings.addListener(stringsListener);
            MapWrapper<Integer, Integer> ints = new MapWrapper<Integer, Integer>(dataStore, "ints", Integer.class, Integer.class, new LinkedHashMap<Integer, Integer>(), 16);
            ints.addListener(intListener);

            dataStore.start();

            ints.put(0, 1000);
            strings.put("Hello", "hi");
            ints.put(1, 1001);
            strings.put("World", "all");
            ints.put(2, 1002);

            strings.publishEvent("bye");
            ints.publishEvent("now");

            verify(stringsListener);
            verify(intListener);

            assertEquals("{Hello=hi, World=all}", strings.toString());
            assertEquals("{0=1000, 1=1001, 2=1002}", ints.toString());

            chronicle.close();
        }
        {
            MapListener stringsListener = createMock("strings", MapListener.class);
            stringsListener.eventStart(7, "strings");
            stringsListener.add("!", "end");
            stringsListener.eventEnd(true);

            MapListener intListener = createMock("ints", MapListener.class);

            intListener.eventStart(8, "ints");
            intListener.add(3, 1003);
            intListener.eventEnd(true);

            replay(stringsListener);
            replay(intListener);

            Chronicle chronicle = new IndexedChronicle(name);
            DataStore dataStore = new DataStore(chronicle, ModelMode.MASTER);
            MapWrapper<String, String> strings = new MapWrapper<String, String>(dataStore, "strings", String.class, String.class, new LinkedHashMap<String, String>(), 16);
            strings.addListener(stringsListener);
            MapWrapper<Integer, Integer> ints = new MapWrapper<Integer, Integer>(dataStore, "ints", Integer.class, Integer.class, new LinkedHashMap<Integer, Integer>(), 16);
            ints.addListener(intListener);

            // assume we have  all the events written so far
            dataStore.start(chronicle.size());

            strings.put("!", "end");
            ints.put(3, 1003);

            verify(stringsListener);
            verify(intListener);

            assertEquals("{Hello=hi, World=all, !=end}", strings.toString());
            assertEquals("{0=1000, 1=1001, 2=1002, 3=1003}", ints.toString());
            chronicle.close();
        }
    }

    @Test
    public void testMapPerformance() throws IOException {
        String name = TMP + "/map-perf";
        ChronicleTools.deleteOnExit(name);
        long start = System.nanoTime();
        int size = 0;
        {
            Chronicle chronicle = new IndexedChronicle(name);
            DataStore dataStore = new DataStore(chronicle, ModelMode.MASTER);
            MapWrapper<String, String> strings = new MapWrapper<String, String>(dataStore, "strings", String.class, String.class, new LinkedHashMap<String, String>(), 16);
            MapWrapper<Integer, Integer> ints = new MapWrapper<Integer, Integer>(dataStore, "ints", Integer.class, Integer.class, new LinkedHashMap<Integer, Integer>(), 16);
            dataStore.start();
            ints.clear();
            strings.clear();

            for (int j = 0; j < 10000; j++) {
                for (int i = 0; i < 100; i++) {
                    ints.put(i, i + j);
                    strings.put(Integer.toString(i), Integer.toString(i + j));
                }
                size += Math.min(strings.size(), ints.size());
                for (int i = 0; i < 100; i++) {
                    ints.remove(i);
                    strings.remove(Integer.toString(i));
                }
            }

            chronicle.close();
        }
        long mid = System.nanoTime();
        {
            Chronicle chronicle = new IndexedChronicle(name);
            DataStore dataStore = new DataStore(chronicle, ModelMode.MASTER);
            MapWrapper<String, String> strings = new MapWrapper<String, String>(dataStore, "strings", String.class, String.class, new LinkedHashMap<String, String>(), 16);
            MapWrapper<Integer, Integer> ints = new MapWrapper<Integer, Integer>(dataStore, "ints", Integer.class, Integer.class, new LinkedHashMap<Integer, Integer>(), 16);
            dataStore.start();
            chronicle.close();
        }
        long end = System.nanoTime();
        System.out.printf("Took %.1f seconds avg to add&remove %,d elements and %.1f seconds avg to reload them%n",
                (mid - start) / 2e9, size, (end - mid) / 2e9);
    }

    @Test
    public void testOverTcp() throws IOException, InterruptedException {
        String name = TMP + "/testOverTcp0";
        String name2 = TMP + "/testOverTcp2";
        ChronicleTools.deleteOnExit(name);
        ChronicleTools.deleteOnExit(name2);

        long start = System.nanoTime();
        int PORT = 12346;
        int size = 0;

        InProcessChronicleSource chronicle = new InProcessChronicleSource(new IndexedChronicle(name), PORT);
        DataStore dataStore = new DataStore(chronicle, ModelMode.MASTER);
        MapWrapper<String, String> strings = new MapWrapper<String, String>(dataStore, "strings", String.class, String.class, new LinkedHashMap<String, String>(), 16);
        MapWrapper<Integer, Integer> ints = new MapWrapper<Integer, Integer>(dataStore, "ints", Integer.class, Integer.class, new LinkedHashMap<Integer, Integer>(), 16);
        dataStore.start();
        ints.clear();
        strings.clear();

        InProcessChronicleSink chronicle2 = new InProcessChronicleSink(new IndexedChronicle(name2), "localhost", PORT);
        DataStore dataStore2 = new DataStore(chronicle2, ModelMode.READ_ONLY);
        MapWrapper<String, String> strings2 = new MapWrapper<String, String>(dataStore2, "strings", String.class, String.class, new LinkedHashMap<String, String>(), 16);
        MapWrapper<Integer, Integer> ints2 = new MapWrapper<Integer, Integer>(dataStore2, "ints", Integer.class, Integer.class, new LinkedHashMap<Integer, Integer>(), 16);

        final AtomicInteger sai = new AtomicInteger();
        MapListener<String, String> stringsListener = new AbstractMapListener<String, String>() {
            @Override
            public void update(String key, String oldValue, String newValue) {
//                System.out.println(key + " " + oldValue + " => " + newValue);
                sai.incrementAndGet();
            }
        };
        strings2.addListener(stringsListener);

        final AtomicInteger iai = new AtomicInteger();
        MapListener<Integer, Integer> intsListener = new AbstractMapListener<Integer, Integer>() {
            @Override
            public void update(Integer key, Integer oldValue, Integer newValue) {
//                System.out.println(key + " " + oldValue + " => " + newValue);
                iai.incrementAndGet();
            }
        };
        ints2.addListener(intsListener);
        dataStore2.start();

        int count = 0;
        for (int j = 0; j < 1000; j++) {
            int collectionSize = 1000;
            for (int i = 0; i < collectionSize; i++) {
                ints.put(i, i + j);
                strings.put(Integer.toString(i), Integer.toString(i + j));
            }
            size += Math.min(strings.size(), ints.size());
            for (int i = 0; i < collectionSize; i++) {
                ints.remove(i);
                strings.remove(Integer.toString(i));
            }
            count += 4 * collectionSize;
        }
        long mid = System.nanoTime();

//        int timeout = 0;
        while (dataStore2.events() < count) {
//            if (timeout++ % 10000 == 0)
//                System.out.println(dataStore2.events());
            Thread.sleep(1);
        }

        chronicle.close();
        chronicle2.close();
        long end = System.nanoTime();

        System.out.printf("Startup and write took %.2f us on average and read and shutdown took %.2f on average%n",
                (mid - start) / count / 1e3, (end - mid) / count / 1e3);
    }

    @Test
    public void testOverTcpPutAllClear() throws IOException, InterruptedException {
        String name = TMP + "/testOverTcpPutAllClear0";
        String name2 = TMP + "/testOverTcpPutAllClear2";
        ChronicleTools.deleteOnExit(name);
        ChronicleTools.deleteOnExit(name2);

        long start = System.nanoTime();
        int PORT = 12347;

        InProcessChronicleSource chronicle = new InProcessChronicleSource(new IndexedChronicle(name), PORT);
        DataStore dataStore = new DataStore(chronicle, ModelMode.MASTER);
        MapWrapper<String, String> strings = new MapWrapper<String, String>(dataStore, "strings", String.class, String.class, new LinkedHashMap<String, String>(), 16);
        MapWrapper<Integer, Integer> ints = new MapWrapper<Integer, Integer>(dataStore, "ints", Integer.class, Integer.class, new LinkedHashMap<Integer, Integer>(), 16);
        dataStore.start();
        ints.clear();
        strings.clear();

        InProcessChronicleSink chronicle2 = new InProcessChronicleSink(new IndexedChronicle(name2), "localhost", PORT);
        DataStore dataStore2 = new DataStore(chronicle2, ModelMode.READ_ONLY);
        MapWrapper<String, String> strings2 = new MapWrapper<String, String>(dataStore2, "strings", String.class, String.class, new LinkedHashMap<String, String>(), 16);
        MapWrapper<Integer, Integer> ints2 = new MapWrapper<Integer, Integer>(dataStore2, "ints", Integer.class, Integer.class, new LinkedHashMap<Integer, Integer>(), 16);

        final AtomicInteger sai = new AtomicInteger();
        MapListener<String, String> stringsListener = new AbstractMapListener<String, String>() {
            @Override
            public void update(String key, String oldValue, String newValue) {
//                System.out.println(key + " " + oldValue + " => " + newValue);
                sai.incrementAndGet();
            }
        };
        strings2.addListener(stringsListener);

        final AtomicInteger iai = new AtomicInteger();
        MapListener<Integer, Integer> intsListener = new AbstractMapListener<Integer, Integer>() {
            @Override
            public void update(Integer key, Integer oldValue, Integer newValue) {
//                System.out.println(key + " " + oldValue + " => " + newValue);
                iai.incrementAndGet();
            }
        };
        ints2.addListener(intsListener);
        dataStore2.start();

        Map<String, String> ssMap = new LinkedHashMap<String, String>();
        Map<Integer, Integer> iiMap = new LinkedHashMap<Integer, Integer>();
        int count = 0;
        int collectionSize = 2000;
        for (int i = 0; i < collectionSize; i++) {
            iiMap.put(i, i);
            ssMap.put(Integer.toString(i), Integer.toString(i));
        }
        for (int j = 0; j < 2500; j++) {
            strings.putAll(ssMap);
            ints.putAll(iiMap);
            strings.clear();
            ints.clear();
            count += 4;
        }
        long mid = System.nanoTime();

//        int timeout = 0;
        while (dataStore2.events() < count) {
            Thread.sleep(1);
        }

        long end = System.nanoTime();

        System.out.printf("Startup and write took %.2f us on average (per key) and read and shutdown took %.2f us on average (per key)%n",
                (mid - start) / count / collectionSize / 1e3, (end - mid) / count / collectionSize / 1e3);

        chronicle.close();
//        System.gc();
        chronicle2.close();
    }

    @Test
    public void testOverTcpGetPerf() throws IOException, InterruptedException {
        String name = TMP + "/testOverTcpGetPerf0";
        String name2 = TMP + "/testOverTcpGetPerf2";
        ChronicleTools.deleteOnExit(name);
        ChronicleTools.deleteOnExit(name2);

        long start = System.nanoTime();
        int PORT = 12348;

        InProcessChronicleSource chronicle = new InProcessChronicleSource(new IndexedChronicle(name), PORT);
        DataStore dataStore = new DataStore(chronicle, ModelMode.MASTER);
        MapWrapper<String, String> strings = new MapWrapper<String, String>(dataStore, "strings", String.class, String.class, new LinkedHashMap<String, String>(), 16);
        MapWrapper<Integer, Integer> ints = new MapWrapper<Integer, Integer>(dataStore, "ints", Integer.class, Integer.class, new LinkedHashMap<Integer, Integer>(), 16);
        dataStore.start();
        ints.clear();
        strings.clear();

        InProcessChronicleSink chronicle2 = new InProcessChronicleSink(new IndexedChronicle(name2), "localhost", PORT);
        DataStore dataStore2 = new DataStore(chronicle2, ModelMode.READ_ONLY);
        MapWrapper<String, String> strings2 = new MapWrapper<String, String>(dataStore2, "strings", String.class, String.class, new LinkedHashMap<String, String>(), 16);
        MapWrapper<Integer, Integer> ints2 = new MapWrapper<Integer, Integer>(dataStore2, "ints", Integer.class, Integer.class, new LinkedHashMap<Integer, Integer>(), 16);

        final AtomicInteger sai = new AtomicInteger();
        MapListener<String, String> stringsListener = new AbstractMapListener<String, String>() {
            @Override
            public void update(String key, String oldValue, String newValue) {
//                System.out.println(key + " " + oldValue + " => " + newValue);
                sai.incrementAndGet();
            }
        };
        strings2.addListener(stringsListener);

        final AtomicInteger iai = new AtomicInteger();
        MapListener<Integer, Integer> intsListener = new AbstractMapListener<Integer, Integer>() {
            @Override
            public void update(Integer key, Integer oldValue, Integer newValue) {
//                System.out.println(key + " " + oldValue + " => " + newValue);
                iai.incrementAndGet();
            }
        };
        ints2.addListener(intsListener);
        dataStore2.start();

        Map<String, String> ssMap = new LinkedHashMap<String, String>();
        Map<Integer, Integer> iiMap = new LinkedHashMap<Integer, Integer>();
        int count = 2; // one clear per collection
        int collectionSize = 2000;
        for (int i = 0; i < collectionSize; i++) {
            iiMap.put(i, i);
            ssMap.put(Integer.toString(i), Integer.toString(i));
        }

        strings.putAll(ssMap);
        ints.putAll(iiMap);
        count += 2;

//        int timeout = 0;
        while (dataStore2.events() < count || ints2.size() < collectionSize) {
//            if (timeout++ % 10000 == 0)
//                System.out.println(dataStore2.events());
            Thread.sleep(1);
        }

        assertEquals(collectionSize, strings.size());
        assertEquals(collectionSize, strings2.size());
        assertEquals(collectionSize, ints.size());
        assertEquals(collectionSize, ints2.size());
        System.out.println("=== performing get test ===");
        int gets = 0;
        for (int j = 0; j < 10000; j++) {
            for (String s : ssMap.keySet()) {
                String s1 = strings.get(s);
                String s2 = strings2.get(s);
                if (s1 == null)
                    assertNotNull(s1);
                if (!s1.equals(s2))
                    assertEquals(s1, s2);
            }
            gets += ssMap.size();
            for (Integer i : iiMap.keySet()) {
                Integer i1 = ints.get(i);
                Integer i2 = ints2.get(i);
                if (i1 == null)
                    assertNotNull(i1);
                if (!i1.equals(i2))
                    assertEquals(i1, i2);
            }
            gets += iiMap.size();
        }

        chronicle.close();
        chronicle2.close();
        long end = System.nanoTime();

        System.out.printf("Average get time including startup, bootstrap and shutdown, took %.3f us average per key%n",
                (end - start) / gets / 1e3);
    }

}
