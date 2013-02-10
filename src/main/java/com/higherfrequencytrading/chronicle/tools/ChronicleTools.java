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

package com.higherfrequencytrading.chronicle.tools;

import com.higherfrequencytrading.chronicle.Excerpt;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Was ChronicleTest but the name was confusing.
 *
 * @author peter.lawrey
 */
public enum ChronicleTools {
    ;

    /**
     * Delete a chronicle on exit, for testing
     *
     * @param basePath of the chronicle
     */
    public static void deleteOnExit(String basePath) {
        new File(basePath + ".data").deleteOnExit();
        new File(basePath + ".index").deleteOnExit();
    }

    /**
     * Take a text copy of the contents of the Excerpt without changing it's position. Can be called in the debugger.
     *
     * @param excerpt to get text from
     * @return 256 bytes as text with `.` replacing special bytes.
     */
    public static String asString(Excerpt excerpt) {
        return asString(excerpt, excerpt.position());
    }

    /**
     * Take a text copy of the contents of the Excerpt without changing it's position. Can be called in the debugger.
     *
     * @param excerpt  to get text from
     * @param position the position to get text from
     * @return up to 1024 bytes as text with `.` replacing special bytes.
     */
    public static String asString(Excerpt excerpt, int position) {
        return asString(excerpt, position, 1024);
    }

    /**
     * Take a text copy of the contents of the Excerpt without changing it's position. Can be called in the debugger.
     *
     * @param excerpt  to get text from
     * @param position the position to get text from
     * @param length   the maximum length
     * @return length bytes as text with `.` replacing special bytes.
     */
    public static String asString(Excerpt excerpt, int position, int length) {
        int limit = Math.min(position + length, excerpt.capacity());
        StringBuilder sb = new StringBuilder(limit - position);
        for (int i = position; i < limit; i++) {
            char ch = (char) excerpt.readUnsignedByte(i);
            if (ch < ' ' || ch > 127) ch = '.';
            sb.append(ch);
        }
        return sb.toString();
    }

    public static String asString(ByteBuffer bb) {
        StringBuilder sb = new StringBuilder();
        for (int i = bb.position(); i < bb.limit(); i++) {
            byte b = bb.get(i);
            if (b < ' ') {
                int h = b & 0xFF;
                if (h < 16)
                    sb.append('0');
                sb.append(Integer.toHexString(h));
            } else {
                sb.append(' ').append((char) b);
            }
        }
        return sb.toString();
    }

    private static final boolean IS64BIT = is64Bit0();

    public static boolean is64Bit() {
        return IS64BIT;
    }

    private static boolean is64Bit0() {
        String systemProp;
        systemProp = System.getProperty("com.ibm.vm.bitmode");
        if (systemProp != null) {
            return systemProp.equals("64");
        }
        systemProp = System.getProperty("sun.arch.data.model");
        if (systemProp != null) {
            return systemProp.equals("64");
        }
        systemProp = System.getProperty("java.vm.version");
        if (systemProp != null) {
            return systemProp.contains("_64");
        }
        return false;
    }

    public static Class[] getGenericTypes(Type type, int count) {
        Class[] types = new Class[count];
        Arrays.fill(types, Object.class);
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Type[] arguments = pt.getActualTypeArguments();
            for (int i = 0; i < arguments.length && i < count; i++) {
                if (arguments[i] instanceof Class) {
                    types[i] = (Class) arguments[i];
                } else if (arguments[i] instanceof ParameterizedType) {
                    types[i] = (Class) ((ParameterizedType) arguments[i]).getRawType();
                }
            }
        }
        return types;
    }
}
