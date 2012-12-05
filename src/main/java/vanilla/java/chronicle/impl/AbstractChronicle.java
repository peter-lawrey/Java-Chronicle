/*
 * Copyright 2011 Peter Lawrey
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

package vanilla.java.chronicle.impl;

import vanilla.java.chronicle.EnumeratedMarshaller;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author peter.lawrey
 */
public abstract class AbstractChronicle implements DirectChronicle {
    protected final String name;
    protected final Map<Class, EnumeratedMarshaller> marshallerMap = new LinkedHashMap<Class, EnumeratedMarshaller>();

    protected long size = 0;

    protected AbstractChronicle(String name) {
        this.name = name;
        marshallerMap.put(String.class, new StringMarshaller(16 * 1024));
        marshallerMap.put(Class.class, new ClassEnumMarshaller(Thread.currentThread().getContextClassLoader()));
    }

    public String name() {
        return name;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public <E> void setEnumeratedMarshaller(EnumeratedMarshaller<E> marshaller) {
        marshallerMap.put(marshaller.classMarshaled(), marshaller);
    }

    @Override
    public <E> EnumeratedMarshaller<E> acquireMarshaller(Class<E> aClass) {
        EnumeratedMarshaller<E> em = marshallerMap.get(aClass);
        if (em == null)
            if (aClass.isEnum())
                marshallerMap.put(aClass, em = new VanillaEnumMarshaller(aClass, null));
            else
                marshallerMap.put(aClass, em = new GenericEnumMarshaller<E>(aClass, 1000));
        return em;
    }
}
