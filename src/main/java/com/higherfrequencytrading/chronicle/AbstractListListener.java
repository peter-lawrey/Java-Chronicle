package com.higherfrequencytrading.chronicle;

import java.util.Collection;

/**
 * @author peter.lawrey
 */
public abstract class AbstractListListener<E> extends AbstractCollectionListener<E> implements ListListener<E> {
    @Override
    public void set(long eventId, int index, E oldElement, E element) {
        remove(eventId, index, oldElement);
        add(eventId, index, element);
    }

    @Override
    public void addAll(long eventId, int index, Collection<E> eList) {
        addAll(eventId, eList);
    }

    @Override
    public void add(long eventId, int index, E element) {
        add(eventId, element);
    }

    @Override
    public void remove(long eventId, int index, E element) {
        remove(eventId, element);
    }
}
