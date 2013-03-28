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
import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.StopCharTester;

import java.io.Externalizable;
import java.io.IOException;
import java.lang.reflect.Constructor;

/**
 * @author peter.lawrey
 */
public class ExternalizableMarshaller<E extends Externalizable> implements EnumeratedMarshaller<E> {
    private final Class<E> classMarshaled;
    private final Constructor<E> constructor;

    public ExternalizableMarshaller(Class<E> classMarshaled) {
        this.classMarshaled = classMarshaled;
        try {
            constructor = classMarshaled.getConstructor();
            constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public Class<E> classMarshaled() {
        return classMarshaled;
    }

    @Override
    public void write(Excerpt excerpt, E e) {
        try {
            e.writeExternal(excerpt);
        } catch (IOException e2) {
            throw new IllegalStateException(e2);
        }
    }

    @Override
    public E read(Excerpt excerpt) {
        try {
            E e = constructor.newInstance();
            e.readExternal(excerpt);
            return e;
        } catch (Exception e2) {
            throw new IllegalStateException(e2);
        }
    }

    @Override
    public E parse(Excerpt excerpt, StopCharTester tester) {
        return read(excerpt);
    }
}
