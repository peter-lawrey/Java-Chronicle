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
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * @author peter.lawrey
 */
public class DataStoreTest {
    static final String TMP = System.getProperty("java.io.tmpdir");

    @Test
    public void testInject() throws IOException {
        String name = TMP + "/inject";
        ChronicleTools.deleteOnExit(name);
        for (int i = 0; i < 10; i++) {
            ExampleDataModel model = new ExampleDataModel();

            Chronicle chronicle = new IndexedChronicle(name);
            DataStore dataStore = new DataStore(chronicle, ModelMode.MASTER);
            dataStore.inject(model);
            final AtomicInteger map2Count = new AtomicInteger();
            model.map2.addListener(new AbstractMapListener<Date, ExampleDataModel.MyType>() {
                @Override
                public void update(Date key, ExampleDataModel.MyType oldValue, ExampleDataModel.MyType newValue) {
                    map2Count.incrementAndGet();
                }
            });
            final AtomicInteger list2Count = new AtomicInteger();
            model.list2.addListener(new AbstractCollectionListener<ExampleDataModel.MyType>() {
                @Override
                public void add(ExampleDataModel.MyType element) {
                    list2Count.incrementAndGet();
                }
            });
            final AtomicInteger set2Count = new AtomicInteger();
            model.set2.addListener(new AbstractCollectionListener<ExampleDataModel.MyType>() {
                @Override
                public void add(ExampleDataModel.MyType element) {
                    set2Count.incrementAndGet();
                }
            });
            dataStore.start();

            model.map.put(new Date(i * 1000), new ExampleDataModel.MyType());
            model.map2.put(new Date(i * 1000), new ExampleDataModel.MyType());
            model.list.add(new ExampleDataModel.MyType());
            model.list2.add(new ExampleDataModel.MyType());
            model.set.add(new ExampleDataModel.MyType());
            model.set2.add(new ExampleDataModel.MyType());

            assertEquals(i + 1, model.map.size());
            assertEquals(i + 1, map2Count.get());
            assertEquals(i + 1, model.list.size());
            assertEquals(i + 1, list2Count.get());
            assertEquals(i + 1, model.set.size());
            assertEquals(i + 1, set2Count.get());
            chronicle.close();
        }
    }
}
