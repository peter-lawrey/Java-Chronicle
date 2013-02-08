package com.higherfrequencytrading.chronicle.datamodel;

import com.higherfrequencytrading.chronicle.ExcerptListener;

/**
 * @author peter.lawrey
 */
public interface Wrapper extends ExcerptListener {
    public void notifyOff(boolean notifyOn);

/*    public class DequeWrapper<E> extends QueueWrapper<E> implements Deque<E> {

    }

    public class ListWrapper<E> extends AbstractSequentialList<E> implements List<E>, RandomAccess {
    }

    public class MapWrapper<K, V> extends AbstractMap<K,V> implements Map<K,V> {

    }

    public class NavigableMapWrapper<K,V> extends MapWrapper<K,V> implements NavigableMap<K,V> {
    }


    public class NavigableSetWrapper<E> extends AbstractSet<E> implements NavigableSet<E> {
    }

    public class QueueWrapper<E> extends AbstractQueue<E> implements Queue<E> {
    }*/
}
