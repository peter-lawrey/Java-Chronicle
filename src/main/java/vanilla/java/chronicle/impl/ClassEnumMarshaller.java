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
import vanilla.java.chronicle.Excerpt;
import vanilla.java.chronicle.StopCharTester;

/**
 * @author plawrey
 */
public class ClassEnumMarshaller implements EnumeratedMarshaller<Class> {
    private final ClassLoader classLoader;

    public ClassEnumMarshaller(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Class<Class> classMarshaled() {
        return Class.class;
    }

    @Override
    public void write(Excerpt excerpt, Class aClass) {
        excerpt.writeUTF(aClass.getName());
    }

    @Override
    public Class read(Excerpt excerpt) {
        String name = excerpt.readUTF();
        return load(name);
    }

    private Class load(String name) {
        try {
            return classLoader.loadClass(name);
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
