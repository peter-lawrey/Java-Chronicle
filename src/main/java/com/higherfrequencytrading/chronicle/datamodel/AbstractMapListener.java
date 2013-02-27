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
public abstract class AbstractMapListener<K, V> implements MapListener<K, V> {
    @Override
    public void eventStart(long eventId, String name) {
    }

    @Override
    public void add(K key, V value) {
        update(key, null, value);
    }

    // This is the only one which must be implemented.
    public abstract void update(K key, V oldValue, V newValue);

    @Override
    public void remove(K key, V value) {
        update(key, value, null);
    }

    @Override
    public void onEvent(Object object) {
    }

    @Override
    public void eventEnd(boolean lastEvent) {
    }

    @Override
    public void inSync() {
    }
}
