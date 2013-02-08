package com.higherfrequencytrading.chronicle;

import java.util.Collection;

/**
 * @author peter.lawrey
 */
public abstract class AbstractCollectionListener<E> implements CollectionListener<E> {
    @Override
    public void eventStart(long eventId, String name) {
    }

    @Override
    public void eventEnd(boolean lastEvent) {
    }

    // the only one which must be implemented.
    @Override
    public abstract void add(E element);

    @Override
    public void addAll(Collection<E> eList) {
        for (E e : eList) {
            add(e);
        }
    }

    @Override
    public void removeAll(Collection<E> eList) {
        for (E e : eList) {
            remove(e);
        }
    }

    @Override
    public void remove(E e) {
    }
}
