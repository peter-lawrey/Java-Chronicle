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

package com.higherfrequencytrading.chronicle.impl;

import com.higherfrequencytrading.chronicle.EnumeratedMarshaller;
import com.higherfrequencytrading.chronicle.ExcerptMarshallable;

import java.io.Externalizable;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author peter.lawrey
 */
public abstract class AbstractChronicle implements DirectChronicle {
    private final String name;
    private final Map<Class, EnumeratedMarshaller> marshallerMap = new LinkedHashMap<Class, EnumeratedMarshaller>();

    protected long size = 0;

    protected AbstractChronicle(String name) {
        this.name = name;
        StringMarshaller stringMarshaller = new StringMarshaller(16 * 1024);
        marshallerMap.put(String.class, stringMarshaller);
        marshallerMap.put(CharSequence.class, stringMarshaller);
        marshallerMap.put(Class.class, new ClassEnumMarshaller(Thread.currentThread().getContextClassLoader()));
        marshallerMap.put(Date.class, new DateMarshaller(10191));
    }

    public String name() {
        return name;
    }

    @Override
    public long size() {
        return size;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> void setEnumeratedMarshaller(EnumeratedMarshaller<E> marshaller) {
        marshallerMap.put(marshaller.classMarshaled(), marshaller);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> EnumeratedMarshaller<E> acquireMarshaller(Class<E> aClass) {
        EnumeratedMarshaller<E> em = marshallerMap.get(aClass);
        if (em == null)
            if (aClass.isEnum())
                marshallerMap.put(aClass, em = new VanillaEnumMarshaller(aClass, null));
            else if (ExcerptMarshallable.class.isAssignableFrom(aClass))
                marshallerMap.put(aClass, em = new ExcerptMarshaller((Class) aClass));
            else if (Externalizable.class.isAssignableFrom(aClass))
                marshallerMap.put(aClass, em = new ExternalizableMarshaller((Class) aClass));
            else
                marshallerMap.put(aClass, em = new GenericEnumMarshaller<E>(aClass, 1000));
        return em;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> EnumeratedMarshaller<E> getMarshaller(Class<E> aClass) {
        return marshallerMap.get(aClass);
    }
}
