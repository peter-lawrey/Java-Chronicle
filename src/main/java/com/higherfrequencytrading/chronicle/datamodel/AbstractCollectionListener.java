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

/**
 * @author peter.lawrey
 */
public abstract class AbstractCollectionListener<E> implements CollectionListener<E> {
    @Override
    public void eventStart(long eventId, String name) {
    }

    @Override
    public void eventEnd(boolean lastEvent) {
    }

    // the only one which must be implemented.
    @Override
    public abstract void add(E element);

    @Override
    public void remove(E e) {
    }

    @Override
    public void onEvent(Object object) {
    }
}
