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
import com.higherfrequencytrading.chronicle.ExcerptMarshallable;
import com.higherfrequencytrading.chronicle.tools.ChronicleTools;

import java.io.Externalizable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author peter.lawrey
 */
public class DataStore {
    private static final Logger LOGGER = Logger.getLogger(DataStore.class.getName());
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

    @SuppressWarnings("unchecked")
    public <Model> void inject(Model model) {
        try {
            for (Class type = model.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    field.setAccessible(true);
                    Class<?> fieldType = field.getType();
                    if (fieldType.isInterface()) {
                        if (fieldType == Map.class || fieldType == ObservableMap.class) {
                            Class[] genericTypes = ChronicleTools.getGenericTypes(field.getGenericType(), 2);
                            Map underlying = (Map) field.get(model);
                            if (underlying == null)
                                underlying = new LinkedHashMap();
                            ObservableMap map = new MapWrapper(this, field.getName(), genericTypes[0], genericTypes[1], underlying, 1024);
                            field.set(model, map);
                        } else if (fieldType == List.class || fieldType == ObservableList.class) {
                            Class[] genericTypes = ChronicleTools.getGenericTypes(field.getGenericType(), 1);
                            List underlying = (List) field.get(model);
                            if (underlying == null)
                                underlying = new ArrayList();
                            ObservableList list = new ListWrapper(this, field.getName(), genericTypes[0], underlying, 1024);
                            field.set(model, list);
                        } else if (fieldType == Set.class || fieldType == ObservableSet.class) {
                            Class[] genericTypes = ChronicleTools.getGenericTypes(field.getGenericType(), 1);
                            Set underlying = (Set) field.get(model);
                            if (underlying == null)
                                underlying = new LinkedHashSet();
                            ObservableSet set = new SetWrapper(this, field.getName(), genericTypes[0], underlying, 1024);
                            field.set(model, set);
                        } else {
                            LOGGER.info("Skipping field of type " + fieldType + " as this is not supported interface");
                        }
                    } else {
                        LOGGER.info("Skipping field of type " + fieldType + " as injecting concrete classes is not supported");
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
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
        if (ExcerptMarshallable.class.isAssignableFrom(eClass) || Externalizable.class.isAssignableFrom(eClass))
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
