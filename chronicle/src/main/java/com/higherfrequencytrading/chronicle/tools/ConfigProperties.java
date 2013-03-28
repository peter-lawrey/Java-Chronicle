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

import java.io.IOException;
import java.util.*;

import static com.higherfrequencytrading.chronicle.tools.IOTools.loadProperties;

/**
 * Add support for scoped properties.  A single properties file can use scoping rules to minimise duplication in your config.
 * <p/>
 * For example, say you have a scope of "host.application", when you lookup a properties like "port" it will look for "host.application.port", "host.port", "application.port" and finally "port"
 *
 * @author peter.lawrey
 */
public class ConfigProperties extends AbstractMap<String, String> {
    private static final String[] NO_STRINGS = {};

    private final Properties properties;
    private final String scope;
    private final String[] scopeArray;

    public ConfigProperties(Properties properties, String scope) {
        this.properties = properties;
        this.scope = scope;
        this.scopeArray = decompose(scope);
    }

    private String[] decompose(String scope) {
        if (scope.length() == 0) return NO_STRINGS;
        String[] parts = scope.split("\\.");
        List<String> ret = new ArrayList<String>();
        for (int len = parts.length; len >= 1; len--) {
            for (int i = 1, max = 1 << parts.length; i < max; i++) {
                if (Integer.bitCount(i) == len) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < parts.length; j++) {
                        if (((i >> j) & 1) != 0) {
                            sb.append(parts[j]).append('.');
                        }
                    }
                    ret.add(sb.toString());
                }
            }
        }
        return ret.toArray(new String[ret.size()]);
    }

    public ConfigProperties(String path, String scopeArray) throws IOException {
        this(loadProperties(path), scopeArray);
    }

    @Override
    public String get(Object key) {
        for (String s : scopeArray) {
            String key2 = s + key;
            Object obj = properties.get(key2);
            if (obj == null)
                continue;
            return String.valueOf(obj);
        }
        return super.get(String.valueOf(key));
    }

    public String get(String name, String defaultValue) {
        String value = get(name);
        return value == null ? defaultValue : value;
    }

    public int getInt(String name, int defaultValue) {
        String value = get(name);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(name);
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    public long getLong(String name, long defaultValue) {
        String value = get(name);
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(name);
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    public double getDouble(String name, double defaultValue) {
        String value = get(name);
        if (value == null) return defaultValue;
        try {
            return Double.parseDouble(name);
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String name, boolean defaultValue) {
        String value = get(name);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<Entry<String, String>> entrySet() {
        return (Set) properties.entrySet();
    }

    public ConfigProperties addToScope(String name) {
        if (!scope.isEmpty()) {
            if (Arrays.asList(scope.split("\\.")).contains(name))
                return this;
        }
        String scope2 = scope.length() == 0 ? name : (scope + "." + name);
        return new ConfigProperties(properties, scope2);
    }
}
