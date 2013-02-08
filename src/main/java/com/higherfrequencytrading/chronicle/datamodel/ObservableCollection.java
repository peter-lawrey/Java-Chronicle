package com.higherfrequencytrading.chronicle.datamodel;

import com.higherfrequencytrading.chronicle.CollectionListener;

import java.util.Collection;

/**
 * @author peter.lawrey
 */
public interface ObservableCollection<E> extends Collection<E>, Wrapper {
    public void addListener(CollectionListener<E> listener);

    public void removeListener(CollectionListener<E> listener);
}
