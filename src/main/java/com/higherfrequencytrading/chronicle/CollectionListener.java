package com.higherfrequencytrading.chronicle;

import java.util.Collection;

/**
 * @author peter.lawrey
 */
public interface CollectionListener<E> {
    void add(long eventId, E e);

    void remove(long eventId, E e);

    void addAll(long eventId, Collection<E> eList);

    void removeAll(long eventId, Collection<E> eList);
}
