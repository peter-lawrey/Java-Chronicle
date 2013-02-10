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
import com.higherfrequencytrading.chronicle.Excerpt;

import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author peter.lawrey
 */
public class DataStore {
    private final Chronicle chronicle;
    private final ModelMode mode;
    private final Excerpt excerpt;
    private final Map<String, Wrapper> wrappers = new LinkedHashMap<String, Wrapper>();

    private Boolean notifyOff = null;

    public DataStore(Chronicle chronicle, ModelMode mode) {
        this.chronicle = chronicle;
        this.mode = mode;
        excerpt = chronicle.createExcerpt();
    }

    public void start() {
        start(-1);
    }

    void notifyOff(boolean notifyOff) {
        if ((Boolean) notifyOff != this.notifyOff) {
            for (Wrapper wrapper : wrappers.values()) {
                wrapper.notifyOff(notifyOff);
            }
            this.notifyOff = notifyOff;
        }
    }

    public void start(long lastEvent) {
        excerpt.index(-1);
        long size = excerpt.size();
        notifyOff(lastEvent >= 0);
        while (excerpt.index() < size && excerpt.nextIndex()) {
            if (processNextEvent()) continue;

            if (notifyOff && lastEvent <= excerpt.index())
                notifyOff(false);
        }
        notifyOff(false);
    }

    private boolean processNextEvent() {
//        System.out.println(excerpt.index()+": "+ ChronicleTools.asString(excerpt));
        String name = excerpt.readEnum(String.class);
        Wrapper wrapper = wrappers.get(name);
        if (wrapper == null)
            return true;
        wrapper.onExcerpt(excerpt);
        return false;
    }

    public void add(String name, Wrapper wrapper) {
        wrappers.put(name, wrapper);
    }

    public Excerpt startExcerpt(int capacity, String name) {
        excerpt.startExcerpt(capacity + 2 + name.length());
        excerpt.writeEnum(name);
        return excerpt;
    }

    public boolean enumeratedClass(Class eClass) {
        if (Comparable.class.isAssignableFrom(eClass) && (eClass.getModifiers() & Modifier.FINAL) != 0)
            return true;
        return chronicle.getMarshaller(eClass) != null;
    }

    public void checkWritable() {
        if (!mode.writable) throw new IllegalStateException("ModelModel=" + mode);
    }

    public long events() {
        return excerpt.index() + 1;
    }

    public boolean nextEvent() {
        if (excerpt.nextIndex()) {
            processNextEvent();
            return true;
        }
        return false;
    }
}
