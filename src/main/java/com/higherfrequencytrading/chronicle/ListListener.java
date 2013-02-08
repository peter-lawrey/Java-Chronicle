package com.higherfrequencytrading.chronicle;

import java.util.Collection;

/**
 * @author peter.lawrey
 */
public interface ListListener<E> extends CollectionListener<E> {
    public void set(long eventId, int index, E oldElement, E element);

    public void add(long eventId, int index, E element);

    public void addAll(long eventId, int index, Collection<E> eList);

    public void remove(long eventId, int index, E element);
}
