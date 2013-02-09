package com.higherfrequencytrading.chronicle.datamodel;

import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;
import com.higherfrequencytrading.chronicle.tools.ChronicleTest;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;

/**
 * @author plawrey
 */
public class SetWrapperTest {
    static final String TMP = System.getProperty("java.io.tmpdir");

    @Test
    public void testMethods() throws IOException {
        String name = TMP + "/set-methods";
        ChronicleTest.deleteOnExit(name);
        {
            Chronicle chronicle = new IndexedChronicle(name);
            DataStore dataStore = new DataStore(chronicle, ModelMode.MASTER);
            SetWrapper<String> strings = new SetWrapper<String>(dataStore, "test", String.class, new LinkedHashSet<String>(), 8);
            SetWrapper<Integer> ints = new SetWrapper<Integer>(dataStore, "ints", Integer.class, new LinkedHashSet<Integer>(), 6);
            dataStore.start();

            ints.add(0);
            strings.add("Hello");
            ints.add(1);
            strings.add("World");
            ints.add(2);

            assertEquals("[Hello, World]", strings.toString());
            assertEquals("[0, 1, 2]", ints.toString());
            assertEquals(String[].class, strings.toArray().getClass());

            chronicle.close();
        }
        {
            Chronicle chronicle = new IndexedChronicle(name);
            DataStore dataStore = new DataStore(chronicle, ModelMode.MASTER);
            SetWrapper<String> strings = new SetWrapper<String>(dataStore, "test", String.class, new LinkedHashSet<String>(), 8);
            SetWrapper<Integer> ints = new SetWrapper<Integer>(dataStore, "ints", Integer.class, new LinkedHashSet<Integer>(), 6);
            dataStore.start();
            strings.add("!");
            ints.add(3);
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
        System.out.printf("Took %.1f seconds avg to add&remove %,d elements and %.1f seconds avg to reload them",
                (mid - start) / 2e9, size, (end - mid) / 2e9);
    }
}
