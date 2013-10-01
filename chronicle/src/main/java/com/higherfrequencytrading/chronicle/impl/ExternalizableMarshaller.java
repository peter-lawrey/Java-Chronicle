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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Externalizable;
import java.io.IOException;

/**
 * @author peter.lawrey
 */
public class ExternalizableMarshaller<E extends Externalizable> implements EnumeratedMarshaller<E> {
    @NotNull
    private final Class<E> classMarshaled;

    public ExternalizableMarshaller(@NotNull Class<E> classMarshaled) {
        this.classMarshaled = classMarshaled;
    }

    @Override
    public void write(Excerpt bytes, @NotNull E e) {
        try {
            e.writeExternal(bytes);
        } catch (IOException e2) {
            throw new IllegalStateException(e2);
        }
    }

    @Override
    public E read(Excerpt bytes) {
        E e;
        try {
            e = (E) UnsafeExcerpt.UNSAFE.allocateInstance(classMarshaled);
            e.readExternal(bytes);
        } catch (Exception e2) {
            throw new IllegalStateException(e2);
        }
        return e;
    }

    @NotNull
    @Override
    public Class<E> classMarshaled() {
        return classMarshaled;
    }

    @Nullable
    @Override
    public E parse(@NotNull Excerpt excerpt, @NotNull StopCharTester tester) {
        throw new UnsupportedOperationException();
    }
}
