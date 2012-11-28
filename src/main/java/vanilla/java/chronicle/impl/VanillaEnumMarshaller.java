package vanilla.java.chronicle.impl;

import vanilla.java.chronicle.EnumeratedMarshaller;
import vanilla.java.chronicle.Excerpt;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author plawrey
 */
public class VanillaEnumMarshaller<E extends Enum<E>> implements EnumeratedMarshaller<E> {
    private final Class<E> classMarshaled;
    private final E[] interner = (E[]) new Enum[1024];
    private final BitSet internerDup = new BitSet(1024);
    private final Map<String, E> map = new LinkedHashMap<String, E>();
    private final E defaultValue;

    public VanillaEnumMarshaller(Class<E> classMarshaled, E defaultValue) {
        this.classMarshaled = classMarshaled;
        this.defaultValue = defaultValue;

        for (E e : classMarshaled.getEnumConstants()) {
            map.put(e.name(), e);
            int idx = hashFor(e.name()) & (interner.length - 1);
            if (!internerDup.get(idx)) {
                if (interner[idx] != null) {
                    interner[idx] = null;
                    internerDup.set(idx);
                } else {
                    interner[idx] = e;
                }
            }
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

    private int hashFor(CharSequence cs) {
        int h = 0;

        for (int i = 0, length = cs.length(); i < length; i++)
            h = 31 * h + cs.charAt(i);

        h ^= (h >>> 20) ^ (h >>> 12);
        h ^= (h >>> 7) ^ (h >>> 4);
        return h & (interner.length - 1);
    }

    private final StringBuilder reader = new StringBuilder();

    @Override
    public E read(Excerpt excerpt) {
        reader.setLength(0);
        excerpt.readUTF(reader);
        int num = hashFor(reader);
        int idx = num & (interner.length - 1);
        E e = interner[idx];
        if (e != null) return e;
        if (!internerDup.get(idx)) return defaultValue;
        e = map.get(reader.toString());
        return e == null ? defaultValue : e;
    }
}
