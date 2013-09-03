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

import com.higherfrequencytrading.chronicle.Excerpt;

import java.lang.annotation.Annotation;
import java.util.*;

import static com.higherfrequencytrading.chronicle.datamodel.WrapperEvent.*;

/**
 * @author peter.lawrey
 */
public class MapWrapper<K, V> implements ObservableMap<K, V> {
    private final DataStore dataStore;
    private final String name;
    private final Class<K> kClass;
    private final Class<V> vClass;
    private final Map<K, V> underlying;
    private final int maxMessageSize;
    private final List<MapListener<K, V>> listeners = new ArrayList<MapListener<K, V>>();
    private final Set<K> keySet;
    private final Collection<V> values;
    private final Set<Entry<K, V>> entrySet;
    private final boolean kEnumClass;
    private final boolean vEnumClass;
    private boolean notifyOff = false;
    private Annotation[] annotations = {};

    public MapWrapper(DataStore dataStore, String name, Class<K> kClass, Class<V> vClass, Map<K, V> underlying, int maxMessageSize) {
        this.dataStore = dataStore;
        this.name = name;
        this.kClass = kClass;
        this.vClass = vClass;
        this.underlying = underlying;
        this.maxMessageSize = maxMessageSize;
        kEnumClass = dataStore.enumeratedClass(kClass);
        vEnumClass = dataStore.enumeratedClass(vClass);

        keySet = Collections.unmodifiableSet(underlying.keySet());
        values = Collections.unmodifiableCollection(underlying.values());
        entrySet = Collections.unmodifiableSet(underlying.entrySet());

        dataStore.add(name, this);
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Annotation[] annotations) {
        this.annotations = Arrays.copyOf(annotations, annotations.length);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        for (Annotation annotation : annotations) {
            if (annotationClass.isInstance(annotation))
                return (A) annotation;
        }
        return null;
    }

    @Override
    public void addListener(MapListener<K, V> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(MapListener<K, V> listener) {
        listeners.remove(listener);
    }

    @Override
    public void inSync() {
        for (MapListener<K, V> listener : listeners) {
            listener.inSync();
        }
    }

    // reload, and synchronise the map.
    @Override
    public void onExcerpt(Excerpt excerpt) {
        int position = excerpt.position();
        WrapperEvent event = excerpt.readEnum(WrapperEvent.class);
        if (event == null) {
            excerpt.position(position);
            System.err.println("Unknown event type " + excerpt.readUTF());
            return;
        }
        if (!notifyOff) {
            for (int i = 0; i < listeners.size(); i++)
                listeners.get(i).eventStart(excerpt.index(), name);
        }
        try {
            switch (event) {
                case put: {
                    onExcerptPut(excerpt);
                    break;

                }
                case putAll: {
                    int count = excerpt.readInt();
                    for (int i = 0; i < count; i++)
                        onExcerptPut(excerpt);
                    break;
                }
                case remove: {
                    @SuppressWarnings("unchecked")
                    K key = readKey(excerpt);
                    V value = underlying.remove(key);
                    if (!notifyOff)
                        for (int i = 0; i < listeners.size(); i++)
                            listeners.get(i).remove(key, value);

                    break;
                }

                case clear: {
                    if (!notifyOff) {
                        @SuppressWarnings("unchecked")
                        Entry<K, V>[] entrySet = underlying.entrySet().toArray(new Entry[underlying.size()]);
                        for (int i = 0; i < listeners.size(); i++) {
                            MapListener<K, V> listener = listeners.get(i);
                            for (int j = 0; j < entrySet.length; j++) {
                                listener.remove(entrySet[j].getKey(), entrySet[j].getValue());
                            }
                        }
                    }
                    underlying.clear();
                    break;
                }
                case event: {
                    if (!notifyOff) {
                        Object object = excerpt.readObject();
                        for (int i = 0; i < listeners.size(); i++)
                            listeners.get(i).onEvent(object);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!notifyOff) {
            boolean lastEvent = !excerpt.hasNextIndex();

            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).eventEnd(lastEvent);
            }
        }

    }

    private void onExcerptPut(Excerpt excerpt) {
        K key = readKey(excerpt);
        V value = readValue(excerpt);
        V previous = underlying.put(key, value);
        if (!notifyOff)
            for (int i = 0; i < listeners.size(); i++) {
                MapListener<K, V> listener = listeners.get(i);
                if (previous == null)
                    listener.add(key, value);
                else
                    listener.update(key, previous, value);
            }
    }

    @SuppressWarnings("unchecked")
    private V readValue(Excerpt excerpt) {
        if (vEnumClass)
            return excerpt.readEnum(vClass);
        return (V) excerpt.readObject();
    }

    @SuppressWarnings("unchecked")
    private K readKey(Excerpt excerpt) {
        if (kEnumClass)
            return excerpt.readEnum(kClass);
        return (K) excerpt.readObject();
    }

    @Override
    public void notifyOff(boolean notifyOff) {
        this.notifyOff = notifyOff;
    }

    //// Map
    @Override
    public void clear() {
        checkWritable();
        writeClear();
    }

    private void writeClear() {
        Excerpt excerpt = getExcerpt(16, clear);
        long eventId = excerpt.index();
        excerpt.writeEnum(clear);
        excerpt.finish();
        if (!notifyOff && !listeners.isEmpty()) {
            @SuppressWarnings("unchecked")
            Entry<K, V>[] entrySet = underlying.entrySet().toArray(new Entry[underlying.size()]);
            for (int i = 0; i < listeners.size(); i++) {
                MapListener<K, V> listener = listeners.get(i);
                listener.eventStart(eventId, name);
                for (int j = 0; j < entrySet.length; j++) {
                    listener.remove(entrySet[j].getKey(), entrySet[j].getValue());
                }
                listener.eventEnd(true);
            }
        }
    }

    private Excerpt getExcerpt(int maxSize, WrapperEvent event) {
        Excerpt excerpt = dataStore.startExcerpt(maxSize + 2 + event.name().length(), name);
        excerpt.writeEnum(event);
        return excerpt;
    }

    void checkWritable() {
        dataStore.checkWritable();
    }

    @Override
    public boolean containsKey(Object key) {
        return underlying.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return underlying.containsValue(value);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return entrySet;
    }

    @Override
    public boolean equals(Object o) {
        return underlying.equals(o);
    }

    @Override
    public V get(Object key) {
        return underlying.get(key);
    }

    @Override
    public int hashCode() {
        return underlying.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return underlying.isEmpty();
    }

    @Override
    public Set<K> keySet() {
        return keySet;
    }

    @Override
    public V put(K key, V value) {
        checkWritable();
        V previous = underlying.put(key, value);
        if (sameOrNotEqual(previous, value))
            writePut(key, previous, value);
        return previous;
    }

    private void writePut(K key, V previous, V value) {
        Excerpt excerpt = getExcerpt(maxMessageSize, put);
        long eventId = excerpt.index();
        writeKey(excerpt, key);
        writeValue(excerpt, value);
        excerpt.finish();
        if (!notifyOff && !listeners.isEmpty()) {
            for (int i = 0; i < listeners.size(); i++) {
                MapListener<K, V> listener = listeners.get(i);
                listener.eventStart(eventId, name);
                if (previous == null)
                    listener.add(key, value);
                else
                    listener.update(key, previous, value);
                listener.eventEnd(true);
            }
        }
    }

    private void writeValue(Excerpt excerpt, V value) {
        if (vEnumClass)
            excerpt.writeEnum(value);
        else
            excerpt.writeObject(value);
    }

    private void writeKey(Excerpt excerpt, K key) {
        if (kEnumClass)
            excerpt.writeEnum(key);
        else
            excerpt.writeObject(key);
    }

    protected boolean sameOrNotEqual(V previous, V value) {
        return previous == value || previous == null || !previous.equals(value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        checkWritable();
        performAndWritePutAll(m);
    }

    private void performAndWritePutAll(Map<? extends K, ? extends V> m) {
        Excerpt excerpt = getExcerpt(m.size() * maxMessageSize, putAll);
        long eventId = excerpt.index();
        int pos = excerpt.position();
        excerpt.writeInt(0); // place holder for the actual size.
        int count = 0;
        for (int i = 0; i < listeners.size(); i++) {
            MapListener<K, V> listener = listeners.get(i);
            listener.eventStart(eventId, name);
        }
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();
            V previous = underlying.put(key, value);
            if (sameOrNotEqual(previous, value)) {
                writeKey(excerpt, key);
                writeValue(excerpt, value);
                for (int i = 0; i < listeners.size(); i++) {
                    MapListener<K, V> listener = listeners.get(i);
                    if (previous == null)
                        listener.add(key, value);
                    else
                        listener.update(key, previous, value);
                }
                count++;
            }
            for (int i = 0; i < listeners.size(); i++) {
                MapListener<K, V> listener = listeners.get(i);
                listener.eventEnd(true);
            }
        }
        excerpt.writeInt(pos, count);
        excerpt.finish();
    }

    @Override
    public V remove(Object key) {
        checkWritable();
        V value = underlying.remove(key);
        if (value != null)
            writeRemove(key, value);
        return value;
    }

    @SuppressWarnings("unchecked")
    private void writeRemove(Object key, V value) {
        Excerpt excerpt = getExcerpt(maxMessageSize, remove);
        long eventId = excerpt.index();
        writeKey(excerpt, (K) key);
        writeValue(excerpt, value);
        excerpt.finish();
        if (!notifyOff && !listeners.isEmpty()) {
            for (int i = 0; i < listeners.size(); i++) {
                MapListener<K, V> listener = listeners.get(i);
                listener.eventStart(eventId, name);
                listener.remove((K) key, value);
                listener.eventEnd(true);
            }
        }
    }

    @Override
    public int size() {
        return underlying.size();
    }

    @Override
    public String toString() {
        return underlying.toString();
    }

    @Override
    public Collection<V> values() {
        return values;
    }

    @Override
    public void publishEvent(Object object) {
        Excerpt excerpt = getExcerpt(maxMessageSize + 128, event);
        long eventId = excerpt.index();
        excerpt.writeObject(event);
        excerpt.finish();

        if (!notifyOff && !listeners.isEmpty()) {
            for (int i = 0; i < listeners.size(); i++) {
                MapListener<K, V> listener = listeners.get(i);
                listener.eventStart(eventId, name);
                listener.onEvent(object);
                listener.eventEnd(true);
            }
        }
    }
}
