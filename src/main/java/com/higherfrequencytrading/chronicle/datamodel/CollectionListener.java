package com.higherfrequencytrading.chronicle.datamodel;

import com.higherfrequencytrading.chronicle.EventListener;

import java.util.Collection;

/**
 * @author peter.lawrey
 */
public interface CollectionListener<E> extends EventListener {
    void add(E e);

    void remove(E e);

    void addAll(Collection<E> eList);

    void removeAll(Collection<E> eList);
}
