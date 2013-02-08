package com.higherfrequencytrading.chronicle;

import java.util.Collection;

/**
 * @author peter.lawrey
 */
public abstract class AbstractCollectionListener<E> implements CollectionListener<E> {
    // the only one which must be implemented.
    @Override
    public abstract void add(long eventId, E element);

    @Override
    public void addAll(long eventId, Collection<E> eList) {
        for (E e : eList) {
            add(eventId, e);
        }
    }

    @Override
    public void removeAll(long eventId, Collection<E> eList) {
        for (E e : eList) {
            remove(eventId, e);
        }
    }

    @Override
    public void remove(long eventId, E e) {
    }
}
