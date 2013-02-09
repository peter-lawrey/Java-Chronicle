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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;

/**
 * @author peter.lawrey
 */
public class ExampleDataModel {
    public final NavigableMap<String, MyType> nMap = new TreeMap<String, MyType>();

    public final Map<Date, MyType> map = new LinkedHashMap<Date, MyType>();

    public final List<MyType> list = new ArrayList<MyType>();

    public final NavigableSet<MyType> nSet = new TreeSet<MyType>();

    public final Set<MyType> set = new LinkedHashSet<MyType>();

    public final Queue<String> queue = new PriorityQueue<String>();

    public final Deque<String> deque = new ArrayDeque<String>();

    static class MyType implements Externalizable {

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {

        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

        }
    }
}
