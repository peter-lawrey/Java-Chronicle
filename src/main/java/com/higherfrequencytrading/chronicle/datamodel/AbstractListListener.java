package com.higherfrequencytrading.chronicle.datamodel;

import java.util.Collection;

/**
 * @author peter.lawrey
 */
public abstract class AbstractListListener<E> extends AbstractCollectionListener<E> implements ListListener<E> {
    @Override
    public void set(int index, E oldElement, E element) {
        remove(index, oldElement);
        add(index, element);
    }

    @Override
    public void addAll(int index, Collection<E> eList) {
        addAll(eList);
    }

    @Override
    public void add(int index, E element) {
        add(element);
    }

    @Override
    public void remove(int index, E element) {
        remove(element);
    }
}
