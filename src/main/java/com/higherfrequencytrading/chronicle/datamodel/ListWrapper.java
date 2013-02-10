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

import java.lang.reflect.Array;
import java.util.*;

import static com.higherfrequencytrading.chronicle.datamodel.WrapperEvent.*;

/**
 * @author peter.lawrey
 */
public class ListWrapper<E> implements ObservableList<E> {
    private final DataStore dataStore;
    private final String name;
    private final Class<E> eClass;
    private final List<E> underlying;
    private final int maxMessageSize;
    private final int offset;
    private final List<ListListener<E>> listeners = new ArrayList<ListListener<E>>();
    private boolean notifyOff = false;
    private final boolean enumClass;

    public ListWrapper(DataStore dataStore, String name, Class<E> eClass, List<E> underlying, int maxMessageSize) {
        this(dataStore, name, eClass, underlying, maxMessageSize, 0);
    }

    public ListWrapper(DataStore dataStore, String name, Class<E> eClass, List<E> underlying, int maxMessageSize, int offset) {
        this.dataStore = dataStore;
        this.name = name;
        this.eClass = eClass;
        this.underlying = underlying;
        this.maxMessageSize = maxMessageSize;
        this.offset = offset;
        enumClass = dataStore.enumeratedClass(eClass);
        dataStore.add(name, this);
    }

    @Override
    public void addListener(CollectionListener<E> listener) {
        listeners.add(new ListCollectionListener<E>(listener));
    }

    @Override
    public void removeListener(CollectionListener<E> listener) {
        for (Iterator<ListListener<E>> iterator = listeners.iterator(); iterator.hasNext(); ) {
            ListListener<E> listListener = iterator.next();
            if (listListener instanceof ListCollectionListener && ((ListCollectionListener) listListener).listener == listener) {
                iterator.remove();
            }
        }
    }

    public void addListener(ListListener<E> listener) {
        listeners.add(listener);
    }

    public void removeListener(ListListener<E> listener) {
        listeners.remove(listener);
    }

    @Override
    public boolean add(E e) {
        checkWritable();
        if (!underlying.add(e)) return false;
        writeAdd(e);
        return true;
    }

    @Override
    public void add(int index, E element) {
        checkWritable();
        underlying.add(index, element);
        writeAdd(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        checkWritable();
        underlying.addAll(c);
        if (c.isEmpty())
            return false;
        if (c.size() == 1)
            writeAdd(c instanceof List ? ((List<E>) c).get(0) : c.iterator().next());
        else
            writeAddAll((Collection<E>) c);
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
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
        checkWritable();
        writeClear();
        underlying.clear();
    }

    @Override
    public boolean equals(Object o) {
        return underlying.equals(o);
    }

    @Override
    public E get(int index) {
        return underlying.get(index);
    }

    @Override
    public int hashCode() {
        return underlying.hashCode();
    }

    @Override
    public int indexOf(Object o) {
        return underlying.indexOf(o);
    }

    @Override
    public boolean isEmpty() {
        return underlying.isEmpty();
    }

    @Override
    public Iterator<E> iterator() {
        return listIterator();
    }

    @Override
    public int lastIndexOf(Object o) {
        return underlying.lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(final int index) {
        return new ListIterator<E>() {
            ListIterator<E> iter = underlying.listIterator(index);
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

            @Override
            public boolean hasPrevious() {
                return iter.hasPrevious();
            }

            @Override
            public E previous() {
                return iter.previous();
            }

            @Override
            public int nextIndex() {
                return iter.nextIndex();
            }

            @Override
            public int previousIndex() {
                return iter.previousIndex();
            }

            @Override
            public void set(E e) {
                ListWrapper.this.set(iter.previousIndex() + 1, e);
            }

            @Override
            public void add(E e) {
                ListWrapper.this.add(iter.previousIndex() + 1, e);
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
                case addIndex: {
                    int index = excerpt.readInt();
                    @SuppressWarnings("unchecked")
                    E e = readElement(excerpt);
                    underlying.add(index, e);
                    if (!notifyOff)
                        for (int i = 0; i < listeners.size(); i++)
                            listeners.get(i).add(index, e);

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
                case addAllIndex: {
                    int index = excerpt.readInt();
                    List<E> eList = readList(excerpt);
                    underlying.addAll(index, eList);
                    if (!notifyOff)
                        for (int i = 0; i < listeners.size(); i++) {
                            ListListener<E> listener = listeners.get(i);
                            for (int j = 0; j < eList.size(); j++)
                                listener.add(index + j, eList.get(j));
                        }

                    break;
                }
                case set: {
                    int index = excerpt.readInt();
                    @SuppressWarnings("unchecked")
                    E e = readElement(excerpt);
                    E oldElement = underlying.set(index, e);
                    if (!notifyOff)
                        for (int i = 0; i < listeners.size(); i++)
                            listeners.get(i).set(index, oldElement, e);
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

                case removeIndex: {
                    int index = excerpt.readInt();
                    E oldElement = underlying.remove(index);
                    if (!notifyOff)
                        for (int i = 0; i < listeners.size(); i++)
                            listeners.get(i).remove(index, oldElement);
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
                    int offset = excerpt.readInt();
                    int size = excerpt.readInt();
                    int endToRemove = Math.min(size + offset, underlying.size());
                    if (!notifyOff)
                        for (int i = 0; i < listeners.size(); i++) {
                            ListListener<E> listener = listeners.get(i);
                            for (int j = offset; j < endToRemove; j++)
                                listener.remove(underlying.get(j));
                        }

                    if (offset != 0 || endToRemove != underlying.size()) {
                        underlying.subList(offset, offset + size).clear();
                    } else {
                        underlying.clear();
                    }
                    break;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        if (!notifyOff) {
            boolean lastEvent = !excerpt.hasNextIndex();

            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).eventEnd(lastEvent);
            }
        }
    }

    @Override
    public E remove(int index) {
        checkWritable();
        if (index < size()) {
            E e = underlying.get(index);
            underlying.remove(index);
            writeRemove(index, e);
            return e;
        }
        return null;
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
    public E set(int index, E element) {
        checkWritable();
        E e = underlying.set(index, element);
        if (element.equals(e))
            return e;
        writeSet(index, e, element);
        return e;
    }

    @Override
    public int size() {
        return underlying.size();
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex >= size() || toIndex < fromIndex)
            throw new IllegalArgumentException();
        return new ListWrapper<E>(dataStore, name, eClass, underlying.subList(fromIndex, toIndex), maxMessageSize);
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

    private void writeAdd(int index, E element) {
        Excerpt excerpt = getExcerpt(maxMessageSize, addIndex);
        long eventId = excerpt.index();
        excerpt.writeInt(offset + index);
        writeElement(excerpt, element);
        excerpt.finish();
        if (!notifyOff && !listeners.isEmpty()) {
            for (int i = 0; i < listeners.size(); i++) {
                ListListener<E> listener = listeners.get(i);
                listener.eventStart(eventId, name);
                listener.add(index, element);
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
        Excerpt excerpt = getExcerpt(16, clear);
        long eventId = excerpt.index();
        excerpt.writeInt(offset);
        excerpt.writeInt(size());
        excerpt.finish();
        if (!notifyOff && !listeners.isEmpty()) {
            for (int i = 0; i < listeners.size(); i++) {
                CollectionListener<E> listener = listeners.get(i);
                listener.eventStart(eventId, name);
                for (int j = 0; j < underlying.size(); j++) {
                    listener.remove(underlying.get(j));
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

    @SuppressWarnings("unchecked")
    private void writeRemove(int index, Object o) {
        Excerpt excerpt = getExcerpt(maxMessageSize, removeIndex);
        long eventId = excerpt.index();
        excerpt.writeInt(index);
        excerpt.finish();
        if (!notifyOff && !listeners.isEmpty()) {
            for (int i = 0; i < listeners.size(); i++) {
                ListListener<E> listener = listeners.get(i);
                listener.eventStart(eventId, name);
                listener.remove(index, (E) o);
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

    private void writeSet(int index, E oldElement, E element) {
        Excerpt excerpt = getExcerpt(maxMessageSize, set);
        long eventId = excerpt.index();
        excerpt.writeEnum(add);
        excerpt.writeInt(offset + index);
        writeElement(excerpt, (E) element);
        excerpt.finish();
        if (!notifyOff && !listeners.isEmpty()) {
            for (int i = 0; i < listeners.size(); i++) {
                ListListener<E> listener = listeners.get(i);
                listener.eventStart(eventId, name);
                listener.set(index, oldElement, element);
                listener.eventEnd(true);
            }
        }
    }

    static class ListCollectionListener<E> extends AbstractListListener<E> {
        final CollectionListener<E> listener;

        public ListCollectionListener(CollectionListener<E> listener) {
            this.listener = listener;
        }

        @Override
        public void add(E element) {
            listener.add(element);
        }

        @Override
        public void remove(E e) {
            listener.remove(e);
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
