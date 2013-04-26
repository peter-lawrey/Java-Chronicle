package com.higherfrequencytrading.chronicle.fix;

import com.higherfrequencytrading.chronicle.Excerpt;

/**
 * @author peter.lawrey
 */
public interface FixDecodeListener {
    public void onField(int fid, Excerpt value);

    public void onEndOfMessage();

    public void onEndOfBatch();
}
