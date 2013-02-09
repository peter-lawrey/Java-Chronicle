package com.higherfrequencytrading.chronicle.datamodel;

/**
 * @author peter.lawrey
 */
public interface ObservableMap<K, V> extends Wrapper {
    public void addListener(MapListener<K, V> listener);

    public void removeListener(MapListener<K, V> listener);
}
