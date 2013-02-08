package com.higherfrequencytrading.chronicle;

import java.util.Collection;

/**
 * @author peter.lawrey
 */
public interface ListListener<E> extends CollectionListener<E> {
    public void set(int index, E oldElement, E element);

    public void add(int index, E element);

    public void addAll(int index, Collection<E> eList);

    public void remove(int index, E element);
}
