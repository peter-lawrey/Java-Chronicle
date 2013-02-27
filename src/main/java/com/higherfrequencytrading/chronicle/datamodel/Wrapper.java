/*
 * Copyright 2013 Peter Lawrey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.higherfrequencytrading.chronicle.datamodel;

import com.higherfrequencytrading.chronicle.ExcerptListener;

import java.lang.annotation.Annotation;

/**
 * @author peter.lawrey
 */
public interface Wrapper extends ExcerptListener {
    /**
     * Publish any Serializable or ExcerptMarshallable or class with an EnumeratedMarshaller
     *
     * @param object to publish to the stream for listeners to pick up.
     */
    public void publishEvent(Object object);

    public void notifyOff(boolean notifyOff);

    public <A extends Annotation> A getAnnotation(Class<A> annotationClass);

/*    public class DequeWrapper<E> extends QueueWrapper<E> implements Deque<E> {

    }

    public class NavigableMapWrapper<K,V> extends MapWrapper<K,V> implements NavigableMap<K,V> {
    }


    public class NavigableSetWrapper<E> extends AbstractSet<E> implements NavigableSet<E> {
    }

    public class QueueWrapper<E> extends AbstractQueue<E> implements Queue<E> {
    }*/
}
