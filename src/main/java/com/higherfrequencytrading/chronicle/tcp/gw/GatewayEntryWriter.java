package com.higherfrequencytrading.chronicle.tcp.gw;

import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.impl.WrappedExcerpt;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author peter.lawrey
 */
public class GatewayEntryWriter {
    static final int HEADER_LENGTH = 8 + 8 + 8 + 3 + 1;
    private final Excerpt excerpt;

    public GatewayEntryWriter(Excerpt excerpt) {
        this.excerpt = new WrappedExcerpt(excerpt) {
            @Override
            public void finish() {
                writeInt24(HEADER_LENGTH - 4, position() - HEADER_LENGTH);
                super.finish();
            }
        };
    }

    public Excerpt startExceprt(int capacity, char type) {
        excerpt.startExcerpt(HEADER_LENGTH + capacity);
        excerpt.writeLong(System.currentTimeMillis());
        excerpt.writeLong(System.nanoTime());
        excerpt.writeLong(0L); // read timestamp
        excerpt.writeInt24(0); // the length.
        excerpt.writeByte(type); // the message type.
        return excerpt;
    }

    public void onException(String message, Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        String text = sw.toString();
        Excerpt excerpt = startExceprt(2 + message.length() + 2 + text.length(), 'X');
        excerpt.writeUTF(message);
        excerpt.writeUTF(text);
        excerpt.finish();
    }
}
