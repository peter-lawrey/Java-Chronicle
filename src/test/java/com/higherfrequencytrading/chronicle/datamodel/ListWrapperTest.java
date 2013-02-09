package com.higherfrequencytrading.chronicle.datamodel;

import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;
import com.higherfrequencytrading.chronicle.tools.ChronicleTest;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

/**
 * @author plawrey
 */
public class ListWrapperTest {
    static final String TMP = System.getProperty("java.io.tmpdir");

    @Test
    public void testMethods() throws IOException {
        String name = TMP + "/set-methods";
        ChronicleTest.deleteOnExit(name);
        {
            ListListener stringsListener = createMock("strings", ListListener.class);
            stringsListener.eventStart(1, "strings");
            stringsListener.add("Hello");
            stringsListener.eventEnd(true);

            stringsListener.eventStart(3, "strings");
            stringsListener.add("World");
            stringsListener.eventEnd(true);

            ListListener intListener = createMock("ints", ListListener.class);
            for (int i = 0; i < 3; i++) {
                intListener.eventStart(i * 2, "ints");
                intListener.add(i);
                intListener.eventEnd(true);
            }

            replay(stringsListener);
            replay(intListener);
            Chronicle chronicle = new IndexedChronicle(name);
            DataStore dataStore = new DataStore(chronicle, ModelMode.MASTER);
            ListWrapper<String> strings = new ListWrapper<String>(dataStore, "strings", String.class, new ArrayList<String>(), 8);
            strings.addListener(stringsListener);
            ListWrapper<Integer> ints = new ListWrapper<Integer>(dataStore, "ints", Integer.class, new ArrayList<Integer>(), 6);
            ints.addListener(intListener);

            dataStore.start();

            ints.add(0);
            strings.add("Hello");
            ints.add(1);
            strings.add("World");
            ints.add(2);

            verify(stringsListener);
            verify(intListener);

            assertEquals("[Hello, World]", strings.toString());
            assertEquals("[0, 1, 2]", ints.toString());
            assertEquals(String[].class, strings.toArray().getClass());

            chronicle.close();
        }
        {
            ListListener stringsListener = createMock("strings", ListListener.class);
            stringsListener.eventStart(5, "strings");
            stringsListener.add("!");
            stringsListener.eventEnd(true);

            ListListener intListener = createMock("ints", ListListener.class);

            intListener.eventStart(6, "ints");
            intListener.add(3);
            intListener.eventEnd(true);

            replay(stringsListener);
            replay(intListener);
            Chronicle chronicle = new IndexedChronicle(name);
            DataStore dataStore = new DataStore(chronicle, ModelMode.MASTER);
            ListWrapper<String> strings = new ListWrapper<String>(dataStore, "strings", String.class, new ArrayList<String>(), 8);
            strings.addListener(stringsListener);
            ListWrapper<Integer> ints = new ListWrapper<Integer>(dataStore, "ints", Integer.class, new ArrayList<Integer>(), 6);
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
        String name = TMP + "/set-perf";
        ChronicleTest.deleteOnExit(name);
        long start = System.nanoTime();
        int size = 0;
        {
            Chronicle chronicle = new IndexedChronicle(name);
            DataStore dataStore = new DataStore(chronicle, ModelMode.MASTER);
            ListWrapper<String> strings = new ListWrapper<String>(dataStore, "test", String.class, new ArrayList<String>(), 9);
            ListWrapper<Integer> ints = new ListWrapper<Integer>(dataStore, "ints", Integer.class, new ArrayList<Integer>(), 9);
            dataStore.start();
            ints.clear();
            strings.clear();
            for (int j = 0; j < 10000; j++) {
                for (int i = 0; i < 100; i++) {
                    ints.add(i);
                    strings.add(Integer.toString(i));
                }
                size += Math.min(strings.size(), ints.size());
                for (int i = 0; i < 100; i++) {
                    ints.remove((Integer) i);
                    strings.remove(i);
                }
            }

            chronicle.close();
        }
        long mid = System.nanoTime();
        {
            Chronicle chronicle = new IndexedChronicle(name);
            DataStore dataStore = new DataStore(chronicle, ModelMode.MASTER);
            ListWrapper<String> strings = new ListWrapper<String>(dataStore, "test", String.class, new ArrayList<String>(), 9);
            ListWrapper<Integer> ints = new ListWrapper<Integer>(dataStore, "ints", Integer.class, new ArrayList<Integer>(), 9);
            dataStore.start();
            chronicle.close();
        }
        long end = System.nanoTime();
        System.out.printf("Took %.1f seconds avg to add&remove %,d elements and %.1f seconds avg to reload them",
                (mid - start) / 2e9, size, (end - mid) / 2e9);
    }
}
