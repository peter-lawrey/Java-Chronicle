package com.higherfrequencytrading.chronicle.datamodel;

import java.util.List;

/**
 * @author peter.lawrey
 */
public interface ObservableList<E> extends ObservableCollection<E>, List<E> {
    public void addListener(ListListener<E> listener);

    public void removeListener(ListListener<E> listener);
}
