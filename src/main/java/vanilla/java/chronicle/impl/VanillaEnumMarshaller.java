package vanilla.java.chronicle.impl;

import vanilla.java.chronicle.EnumeratedMarshaller;
import vanilla.java.chronicle.Excerpt;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author plawrey
 */
public class VanillaEnumMarshaller<E extends Enum<E>> implements EnumeratedMarshaller<E> {
    private final Class<E> classMarshaled;
    private final Map<String, E> map = new LinkedHashMap<String, E>();
    private final E defaultValue;

    public VanillaEnumMarshaller(Class<E> classMarshaled, E defaultValue) {
        this.classMarshaled = classMarshaled;
        this.defaultValue = defaultValue;

        for (E e : classMarshaled.getEnumConstants()) {
            map.put(e.name(), e);
        }
    }

    @Override
    public Class<E> classMarshaled() {
        return classMarshaled;
    }

    @Override
    public void write(Excerpt excerpt, E e) {
        excerpt.writeUTF(e == null ? "" : e.name());
    }

    @Override
    public E read(Excerpt excerpt) {
        String s = excerpt.readUTF();
        E e = map.get(s);
        return e == null ? defaultValue : e;
    }
}
