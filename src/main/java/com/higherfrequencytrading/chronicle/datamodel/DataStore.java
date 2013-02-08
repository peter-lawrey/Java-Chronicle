package com.higherfrequencytrading.chronicle.datamodel;

import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.Excerpt;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author peter.lawrey
 */
public class DataStore {
    private final Chronicle chronicle;
    private final ModelMode mode;
    private final Excerpt excerpt;
    private final Map<String, Wrapper> wrappers = new LinkedHashMap<String, Wrapper>();

    private Boolean notifyOff = null;

    public DataStore(Chronicle chronicle, ModelMode mode) {
        this.chronicle = chronicle;
        this.mode = mode;
        excerpt = chronicle.createExcerpt();
    }

    public void start() {
        start(-1);
    }

    void notifyOff(boolean notifyOff) {
        if ((Boolean) notifyOff != this.notifyOff) {
            for (Wrapper wrapper : wrappers.values()) {
                wrapper.notifyOff(notifyOff);
            }
            this.notifyOff = notifyOff;
        }
    }

    public void start(long lastEvent) {
        excerpt.index(-1);
        notifyOff(lastEvent >= 0);
        while (excerpt.nextIndex()) {
            String name = excerpt.readEnum(String.class);
            Wrapper wrapper = wrappers.get(name);
            wrapper.onExcerpt(excerpt);

            if (notifyOff && lastEvent >= excerpt.index())
                notifyOff(false);
        }
        notifyOff(false);
    }

    public ModelMode mode() {
        return mode;
    }

    public void add(String name, Wrapper wrapper) {
        wrappers.put(name, wrapper);
    }

    public Excerpt startExcerpt(int capacity, String name) {
        excerpt.startExcerpt(capacity + 2 + name.length());
        excerpt.writeEnum(name);
        return excerpt;
    }
}
