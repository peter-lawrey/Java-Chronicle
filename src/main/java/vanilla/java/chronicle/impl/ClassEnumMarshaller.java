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
