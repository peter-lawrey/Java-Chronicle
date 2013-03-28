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
import java.lang.reflect.Array;
import java.util.*;

import static com.higherfrequencytrading.chronicle.datamodel.WrapperEvent.*;

/**
 * @author peter.lawrey
 */
public class SetWrapper<E> implements ObservableSet<E> {
    private final DataStore dataStore;
    private final String name;
    private final Class<E> eClass;
    private final Set<E> underlying;
    private final int maxMessageSize;
    private final List<CollectionListener<E>> listeners = new ArrayList<CollectionListener<E>>();
    private boolean notifyOff = false;
    private final boolean enumClass;

    public SetWrapper(DataStore dataStore, String name, Class<E> eClass, Set<E> underlying, int maxMessageSize) {
        this.dataStore = dataStore;
        this.name = name;
        this.eClass = eClass;
        this.underlying = underlying;
        this.maxMessageSize = maxMessageSize;
        enumClass = dataStore.enumeratedClass(eClass);
        dataStore.add(name, this);
    }

    @Override
    public void addListener(CollectionListener<E> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(CollectionListener<E> listener) {
        listeners.remove(listener);
    }

    @Override
    public void inSync() {
        for (CollectionListener<E> listener : listeners) {
            listener.inSync();
        }
    }

    private Annotation[] annotations = {};

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Annotation[] annotations) {
        this.annotations = annotations;
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
    public boolean add(E e) {
        checkWritable();
        if (!underlying.add(e)) return false;
        writeAdd(e);
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        checkWritable();
        List<E> added = new ArrayList<E>();
        for (E e : c)
            if (underlying.add(e))
                added.add(e);
        if (added.isEmpty())
            return false;
        if (added.size() == 1)
            writeAdd(added.get(0));
        else
            writeAddAll(added);
        return true;
    }

    void checkWritable() {
        dataStore.checkWritable();
    }

    @Override
    public boolean contains(Object o) {
        return underlying.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return underlying.containsAll(c);
    }

    @Override
    public void clear() {
        writeClear();
        underlying.clear();
    }

    @Override
    public boolean equals(Object o) {
        return underlying.equals(o);
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
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            Iterator<E> iter = underlying.iterator();
            E last = null;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public E next() {
                return last = iter.next();
            }

            @Override
            public void remove() {
                checkWritable();
                iter.remove();
                int maxSize = maxMessageSize;
                Excerpt excerpt = getExcerpt(maxSize, remove);
                writeElement(excerpt, last);
                excerpt.finish();
            }
        };
    }

    @Override
    public void notifyOff(boolean notifyOff) {
        this.notifyOff = notifyOff;
    }

    @Override
    public void onExcerpt(Excerpt excerpt) {
        WrapperEvent event = excerpt.readEnum(WrapperEvent.class);
        if (!notifyOff) {
            for (int i = 0; i < listeners.size(); i++)
                listeners.get(i).eventStart(excerpt.index(), name);
        }
        try {
            switch (event) {
                case add: {
                    @SuppressWarnings("unchecked")
                    E e = readElement(excerpt);
                    underlying.add(e);
                    if (!notifyOff)
                        for (int i = 0; i < listeners.size(); i++)
                            listeners.get(i).add(e);

                    break;

                }
                case addAll: {
                    List<E> eList = readList(excerpt);
                    underlying.addAll(eList);
                    if (!notifyOff)
                        for (int i = 0; i < listeners.size(); i++) {
                            CollectionListener<E> listener = listeners.get(i);
                            for (int j = 0; j < eList.size(); j++)
                                listener.add(eList.get(j));
                        }

                    break;
                }

                case remove: {
                    @SuppressWarnings("unchecked")
                    E e = readElement(excerpt);
                    underlying.remove(e);
                    if (!notifyOff)
                        for (int i = 0; i < listeners.size(); i++)
                            listeners.get(i).remove(e);

                    break;
                }
                case removeAll: {
                    List<E> eList = readList(excerpt);
                    underlying.removeAll(eList);
                    if (!notifyOff)
                        for (int i = 0; i < listeners.size(); i++) {
                            CollectionListener<E> listener = listeners.get(i);
                            for (int j = 0; j < eList.size(); j++)
                                listener.remove(eList.get(j));
                        }
                    break;
                }
                case clear: {
                    if (!notifyOff && !listeners.isEmpty()) {
                        E[] elements = (E[]) underlying.toArray(new Object[underlying.size()]);
                        for (int i = 0; i < listeners.size(); i++) {
                            CollectionListener<E> listener = listeners.get(i);
                            for (int j = 0; j < elements.length; j++)
                                listener.remove(elements[j]);
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

    @Override
    public boolean remove(Object o) {
        checkWritable();
        if (!underlying.remove(o)) return false;
        writeRemove(o);
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        checkWritable();
        List<E> removed = new ArrayList<E>();
        for (Object o : c)
            if (underlying.remove(o))
                removed.add((E) o);
        if (removed.isEmpty())
            return false;
        if (removed.size() == 0)
            writeRemove(removed.get(0));
        else
            writeRemoveAll(removed);
        return true;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        checkWritable();
        List<Object> toremove = new ArrayList<Object>(size());
        for (E e : this) {
            if (!c.contains(e))
                toremove.add(e);
        }
        if (toremove.isEmpty())
            return false;
        return removeAll(toremove);
    }

    @Override
    public int size() {
        return underlying.size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public E[] toArray() {
        return underlying.toArray((E[]) Array.newInstance(eClass, underlying.size()));
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return underlying.toArray(a);
    }

    @Override
    public String toString() {
        return underlying.toString();
    }

    @Override
    public void publishEvent(Object object) {
        Excerpt excerpt = getExcerpt(maxMessageSize + 128, event);
        long eventId = excerpt.index();
        excerpt.writeObject(event);
        excerpt.finish();

        if (!notifyOff && !listeners.isEmpty()) {
            for (int i = 0; i < listeners.size(); i++) {
                CollectionListener<E> listener = listeners.get(i);
                listener.eventStart(eventId, name);
                listener.onEvent(object);
                listener.eventEnd(true);
            }
        }
    }

    private Excerpt getExcerpt(int maxSize, WrapperEvent event) {
        Excerpt excerpt = dataStore.startExcerpt(maxSize + 2 + event.name().length(), name);
        excerpt.writeEnum(event);
        return excerpt;
    }

    private void writeAdd(E element) {
        Excerpt excerpt = getExcerpt(maxMessageSize, add);
        long eventId = excerpt.index();
        writeElement(excerpt, element);
        excerpt.finish();
        if (!notifyOff && !listeners.isEmpty()) {
            for (int i = 0; i < listeners.size(); i++) {
                CollectionListener<E> listener = listeners.get(i);
                listener.eventStart(eventId, name);
                listener.add(element);
                listener.eventEnd(true);
            }
        }
    }

    private void writeAddAll(Collection<E> added) {
        Excerpt excerpt = getExcerpt(maxMessageSize * added.size(), addAll);
        long eventId = excerpt.index();
        writeList(excerpt, added);
        excerpt.finish();
        if (!notifyOff && !listeners.isEmpty()) {
            for (int i = 0; i < listeners.size(); i++) {
                CollectionListener<E> listener = listeners.get(i);
                listener.eventStart(eventId, name);
                for (E e : added)
                    listener.add(e);
                listener.eventEnd(true);
            }
        }
    }

    private void writeClear() {
        Excerpt excerpt = getExcerpt(10, clear);
        long eventId = excerpt.index();
        excerpt.finish();
        if (!notifyOff && !listeners.isEmpty()) {
            E[] elements = (E[]) underlying.toArray(new Object[underlying.size()]);
            for (int i = 0; i < listeners.size(); i++) {
                CollectionListener<E> listener = listeners.get(i);
                listener.eventStart(eventId, name);
                for (int j = 0; j < elements.length; j++) {
                    listener.remove(elements[j]);
                }
                listener.eventEnd(true);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void writeRemove(Object o) {
        Excerpt excerpt = getExcerpt(maxMessageSize, remove);
        long eventId = excerpt.index();
        writeElement(excerpt, (E) o);
        excerpt.finish();
        if (!notifyOff && !listeners.isEmpty()) {
            for (int i = 0; i < listeners.size(); i++) {
                CollectionListener<E> listener = listeners.get(i);
                listener.eventStart(eventId, name);
                listener.remove((E) o);
                listener.eventEnd(true);
            }
        }
    }

    private void writeRemoveAll(List<E> removed) {
        Excerpt excerpt = getExcerpt(maxMessageSize * removed.size(), removeAll);
        long eventId = excerpt.index();
        writeList(excerpt, removed);
        excerpt.finish();
        if (!notifyOff && !listeners.isEmpty()) {
            for (int i = 0; i < listeners.size(); i++) {
                CollectionListener<E> listener = listeners.get(i);
                listener.eventStart(eventId, name);
                for (int j = 0; j < removed.size(); j++) {
                    listener.remove(removed.get(j));
                }
                listener.eventEnd(true);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private E readElement(Excerpt excerpt) {
        if (enumClass)
            return excerpt.readEnum(eClass);
        return (E) excerpt.readObject();
    }

    private void writeElement(Excerpt excerpt, E element) {
        if (enumClass)
            excerpt.writeEnum(element);
        else
            excerpt.writeObject(element);
    }

    private List<E> readList(Excerpt excerpt) {
        List<E> eList = new ArrayList<E>();
        if (enumClass)
            excerpt.readEnums(eClass, eList);
        else
            excerpt.readList(eList);
        return eList;
    }

    private void writeList(Excerpt excerpt, Collection<E> list) {
        if (enumClass)
            excerpt.writeEnums(list);
        else
            excerpt.writeList(list);
    }
}
