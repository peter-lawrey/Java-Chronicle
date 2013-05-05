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

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author peter.lawrey
 */
public class ClassEnumMarshaller implements EnumeratedMarshaller<Class> {
    private static final int CACHE_SIZE = 1019;
    private static final Map<String, Class> SC_SHORT_NAME = new LinkedHashMap<String, Class>();
    private static final Map<Class, String> CS_SHORT_NAME = new LinkedHashMap<Class, String>();

    static {
        Class[] classes = {Boolean.class, Byte.class, Character.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
                String.class, Class.class, BigInteger.class, BigDecimal.class, Date.class};
        for (Class clazz : classes) {
            String simpleName = clazz.getSimpleName();
            SC_SHORT_NAME.put(simpleName, clazz);
            CS_SHORT_NAME.put(clazz, simpleName);
        }
    }

    public ClassEnumMarshaller(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @SuppressWarnings("unchecked")
    private WeakReference<Class>[] classWeakReference = null;
    private final ClassLoader classLoader;

    @Override
    public Class<Class> classMarshaled() {
        return Class.class;
    }

    @Override
    public void write(Excerpt excerpt, Class aClass) {
        String s = CS_SHORT_NAME.get(aClass);
        if (s == null)
            s = aClass.getName();
        excerpt.writeUTF(s);
    }

    @Override
    public Class read(Excerpt excerpt) {
        String name = excerpt.readUTF();
        return load(name);
    }

    private Class load(String name) {
        int hash = (name.hashCode() & 0x7fffffff) % CACHE_SIZE;
        if (classWeakReference == null)
            classWeakReference = new WeakReference[CACHE_SIZE];
        WeakReference<Class> ref = classWeakReference[hash];
        if (ref != null) {
            Class clazz = ref.get();
            if (clazz != null && clazz.getName().equals(name))
                return clazz;
        }
        try {

            Class<?> clazz = SC_SHORT_NAME.get(name);
            if (clazz != null)
                return clazz;
            clazz = classLoader.loadClass(name);
            classWeakReference[hash] = new WeakReference<Class>(clazz);
            return clazz;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Class parse(Excerpt excerpt, StopCharTester tester) {
        String name = excerpt.parseUTF(tester);
        return load(name);
    }
}
