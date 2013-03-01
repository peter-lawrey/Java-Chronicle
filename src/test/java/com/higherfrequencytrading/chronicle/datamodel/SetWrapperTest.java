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
import com.higherfrequencytrading.chronicle.tools.ChronicleTools;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashSet;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

/**
 * @author peter.lawrey
 */
public class SetWrapperTest {
    static final String TMP = System.getProperty("java.io.tmpdir");

    @Test
    public void testSetMethods() throws IOException {
        String name = TMP + "/testSetMethods";
        ChronicleTools.deleteOnExit(name);
        {
            SetListener stringsListener = createMock("strings", SetListener.class);
            stringsListener.eventStart(1, "strings");
            stringsListener.add("Hello");
            stringsListener.eventEnd(true);

            stringsListener.eventStart(3, "strings");
            stringsListener.add("World");
            stringsListener.eventEnd(true);

            SetListener intListener = createMock("ints", SetListener.class);
            for (int i = 0; i < 3; i++) {
                intListener.eventStart(i * 2, "ints");
                intListener.add(i);
                intListener.eventEnd(true);
            }

            stringsListener.eventStart(5, "strings");
            stringsListener.onEvent("bye");
            stringsListener.eventEnd(true);

            intListener.eventStart(6, "ints");
            intListener.onEvent("now");
            intListener.eventEnd(true);

            stringsListener.inSync();
            intListener.inSync();

            replay(stringsListener);
            replay(intListener);

            Chronicle chronicle = new IndexedChronicle(name);
            DataStore dataStore = new DataStore(chronicle, ModelMode.MASTER);
            SetWrapper<String> strings = new SetWrapper<String>(dataStore, "strings", String.class, new LinkedHashSet<String>(), 8);
            strings.addListener(stringsListener);
            SetWrapper<Integer> ints = new SetWrapper<Integer>(dataStore, "ints", Integer.class, new LinkedHashSet<Integer>(), 6);
            ints.addListener(intListener);

            dataStore.start();

            ints.add(0);
            strings.add("Hello");
            ints.add(1);
            strings.add("World");
            ints.add(2);

            strings.publishEvent("bye");
            ints.publishEvent("now");

            verify(stringsListener);
            verify(intListener);

            assertEquals("[Hello, World]", strings.toString());
            assertEquals("[0, 1, 2]", ints.toString());
            assertEquals(String[].class, strings.toArray().getClass());

            chronicle.close();
        }
        {
            SetListener stringsListener = createMock("strings", SetListener.class);
            stringsListener.eventStart(7, "strings");
            stringsListener.add("!");
            stringsListener.eventEnd(true);

            SetListener intListener = createMock("ints", SetListener.class);

            intListener.eventStart(8, "ints");
            intListener.add(3);
            intListener.eventEnd(true);

            stringsListener.inSync();
            intListener.inSync();

            replay(stringsListener);
            replay(intListener);
            Chronicle chronicle = new IndexedChronicle(name);
            DataStore dataStore = new DataStore(chronicle, ModelMode.MASTER);
            SetWrapper<String> strings = new SetWrapper<String>(dataStore, "strings", String.class, new LinkedHashSet<String>(), 8);
            strings.addListener(stringsListener);
            SetWrapper<Integer> ints = new SetWrapper<Integer>(dataStore, "ints", Integer.class, new LinkedHashSet<Integer>(), 6);
            ints.addListener(intListener);
            // assume we have  all the events written so far
            dataStore.start(chronicle.size());

            strings.add("!");
            ints.add(3);

            verify(stringsListener);
            verify(intListener);

            assertEquals("[Hello, World, !]", strings.toString());
            assertEquals("[0, 1, 2, 3]", ints.toString());
            chronicle.close();
        }
    }

    @Test
    public void testSetPerformance() throws IOException {
        String name = TMP + "/testSetPerformance";
        ChronicleTools.deleteOnExit(name);
        long start = System.nanoTime();
        int size;
        {
            Chronicle chronicle = new IndexedChronicle(name);
            DataStore dataStore = new DataStore(chronicle, ModelMode.MASTER);
            SetWrapper<String> strings = new SetWrapper<String>(dataStore, "test", String.class, new LinkedHashSet<String>(), 9);
            SetWrapper<Integer> ints = new SetWrapper<Integer>(dataStore, "ints", Integer.class, new LinkedHashSet<Integer>(), 9);
            dataStore.start();
            ints.clear();
            strings.clear();
            for (int i = 0; i < 1000000; i++) {
                ints.add(i);
                strings.add(Integer.toString(i));
            }
            size = Math.min(strings.size(), ints.size());
            for (int i = 0; i < 1000000; i++) {
                ints.remove(i);
                strings.remove(Integer.toString(i));
            }

            chronicle.close();
        }
        long mid = System.nanoTime();
        {
            Chronicle chronicle = new IndexedChronicle(name);
            DataStore dataStore = new DataStore(chronicle, ModelMode.MASTER);
            SetWrapper<String> strings = new SetWrapper<String>(dataStore, "test", String.class, new LinkedHashSet<String>(), 9);
            SetWrapper<Integer> ints = new SetWrapper<Integer>(dataStore, "ints", Integer.class, new LinkedHashSet<Integer>(), 9);
            dataStore.start();
            chronicle.close();
        }
        long end = System.nanoTime();
        System.out.printf("Took %.1f seconds avg to add&remove %,d elements and %.1f seconds avg to reload them%n",
                (mid - start) / 2e9, size, (end - mid) / 2e9);
    }
}
