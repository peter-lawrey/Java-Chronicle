package com.higherfrequencytrading.chronicle.fix;

import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.StopCharTesters;

import java.io.IOException;

/**
 * @author peter.lawrey
 */
public class FixDecoder {
    private final FixSocketReader reader;
    private final Excerpt excerpt;

    public FixDecoder(FixSocketReader reader) {
        this.reader = reader;
        excerpt = reader.excerpt().chronicle().createExcerpt();
    }

    public boolean readMessages(FixDecodeListener listener) throws IOException {
        boolean more = false;
        if (!reader.readMessages())
            return false;
        while (excerpt.nextIndex()) {
            more = true;
            int length = (int) excerpt.readStopBit();
            while (excerpt.position() < length) {
                int fid = (int) excerpt.parseLong();
                if (fid == 10) {
                    listener.onEndOfMessage();
                    excerpt.skipTo(StopCharTesters.FIX_TEXT);
                } else {
                    listener.onField(fid, excerpt);
                    // skip tot he end of the field in case we didn't consume it.
                    excerpt.stepBackAndSkipTo(StopCharTesters.FIX_TEXT);
                }
            }
        }
        if (more)
            listener.onEndOfBatch();
        return more;
    }
}
