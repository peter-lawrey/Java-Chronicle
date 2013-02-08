package com.higherfrequencytrading.chronicle.datamodel;

import com.higherfrequencytrading.chronicle.MapListener;

/**
 * @author peter.lawrey
 */
public interface ObservableMap<K, V> extends Wrapper {
    public void addListener(MapListener<K, V> listener);

    public void removeListener(MapListener<K, V> listener);
}
