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

package com.higherfrequencytrading.chronicle;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.nio.ByteOrder;

/**
 * Generic interface for all time-series, indexed data sets.
 *
 * @author peter.lawrey
 */
public interface Chronicle extends Closeable {
    /**
     * @return A name for logging purposes for this Chronicle.
     */
    @NotNull
    String name();

    /**
     * @return a new Excerpt of this Chronicle
     */
    @NotNull
    Excerpt createExcerpt();

    /**
     * @return The size of this Chronicle in number of Excerpts.
     */
    long size();

    /**
     * @return The size of this Chronicle in bytes.
     */
    long sizeInBytes();

    /**
     * @return The byte order of the index and data in the chronicle.
     * @deprecated This will be dropped in Chronicle 2.0
     */
    @Deprecated
    ByteOrder byteOrder();

    /**
     * Close this resource.
     */
    void close();

    /**
     * @param multiThreaded if true, allow multiple threads to access the Excerpt
     * @deprecated This will be dropped in Chronicle 2.0
     */
    void multiThreaded(boolean multiThreaded);

    /**
     * Add an enumerated type or override the default implementation for a class.
     *
     * @param marshaller to add for marshaled types.
     * @param <E>        type marshaled.
     */
    <E> void setEnumeratedMarshaller(@NotNull EnumeratedMarshaller<E> marshaller);

    /**
     * Get the marshaller for a class
     *
     * @return the marshaller or null for there is not already.
     */
    @Nullable
    <E> EnumeratedMarshaller<E> getMarshaller(@NotNull Class<E> eClass);
}
