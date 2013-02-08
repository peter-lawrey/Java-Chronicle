package com.higherfrequencytrading.chronicle;

/**
 * @author peter.lawrey
 */
public interface MapListener<K, V> extends EventListener {

    public void add(K key, V value);

    public void update(K key, V oldValue, V newValue);

    public void remove(K key, V value);

}
