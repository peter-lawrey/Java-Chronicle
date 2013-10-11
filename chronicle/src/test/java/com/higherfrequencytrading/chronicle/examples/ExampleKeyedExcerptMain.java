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

package com.higherfrequencytrading.chronicle.examples;

import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;
import com.higherfrequencytrading.chronicle.tools.ChronicleTools;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

/**
 * @author peter.lawrey
 */
public class ExampleKeyedExcerptMain {
    private static final String TMP = System.getProperty("java.io.tmpdir");

    @NotNull
    private final Chronicle chronicle;
    @NotNull
    private final Excerpt excerpt;
    private final TObjectLongHashMap<String> keyToExcerpt = new TObjectLongHashMap<String>() {
        @Override
        public long getNoEntryValue() {
            return -1;
        }
    };

    public ExampleKeyedExcerptMain(String basePath) throws IOException {
        chronicle = new IndexedChronicle(basePath);
        excerpt = chronicle.createExcerpt();
    }

    public void load() {
        while (excerpt.nextIndex()) {
            String key = excerpt.readUTF();
            keyToExcerpt.put(key, excerpt.index());
        }
    }

    public void putMapFor(String key, Map<String, String> map) {
        excerpt.startExcerpt(4096); // a guess
        excerpt.writeUTF(key);
        excerpt.writeMap(map);
        excerpt.finish();
    }

    public Map<String, String> getMapFor(String key) {

        long value = keyToExcerpt.get(key);
        if (value < 0) return Collections.emptyMap();
        excerpt.index(value);
        // skip the key
        excerpt.skip(excerpt.readStopBit());
        return excerpt.readMap(String.class, String.class);
    }

    public void close() {
        chronicle.close();
    }

    public static void main(String... ignored) throws IOException {
        String basePath = TMP + "/ExampleKeyedExcerptMain";
        ChronicleTools.deleteOnExit(basePath);
        ExampleKeyedExcerptMain map = new ExampleKeyedExcerptMain(basePath);
        map.load();
        long start = System.nanoTime();
        int keys = 10000000;
        for (int i = 0; i < keys; i++) {
            Map<String, String> props = new LinkedHashMap<String, String>();
            props.put("a", Integer.toString(i)); // an int.
            props.put("b", "value-" + i); // String
            props.put("c", Double.toString(i / 1000.0)); // a double
            map.putMapFor(Integer.toHexString(i), props);
        }
        map.close();

        ExampleKeyedExcerptMain map2 = new ExampleKeyedExcerptMain(basePath);
        map2.load();
        long start2 = System.nanoTime();
        for (int i = 0; i < keys; i++) {
            Map<String, Object> props = new LinkedHashMap<String, Object>();
            props.put("a", Integer.toString(i)); // an int.
            props.put("b", "value-" + i); // String
            props.put("c", Double.toString(i / 1000.0)); // a double
            Map<String, String> props2 = map2.getMapFor(Integer.toHexString(i));
            assertEquals(props, props2);
        }
        map2.close();
        long time = System.nanoTime() - start;
        long time2 = System.nanoTime() - start2;
        System.out.printf("Took an average of %,d ns to write and read each entry, an average of %,d ns to lookup%n", time / keys, time2 / keys);
    }
}
