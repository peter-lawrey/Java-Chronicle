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
