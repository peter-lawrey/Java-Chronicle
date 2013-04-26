package com.higherfrequencytrading.chronicle.fix;

import com.higherfrequencytrading.chronicle.Excerpt;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author peter.lawrey
 */
public class FixSocketReader {
    private static final int MIN_FIX_MSG_SIZE = 30;
    private static final byte END_OF_FIX_MESSAGE = 1; // ^A
    private final SocketChannel sc;
    private final Excerpt excerpt;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 1024);
    private volatile boolean closed = false;

    public FixSocketReader(SocketChannel sc, Excerpt excerpt) {
        this.sc = sc;
        this.excerpt = excerpt;
    }

    public boolean readMessages() throws IOException {
        readMoreData(buffer);
        if (buffer.position() < MIN_FIX_MSG_SIZE)
            return false;
        // search back for the end of the message
        int end = buffer.position(), pos = end;
        while (buffer.get(--pos) != END_OF_FIX_MESSAGE)
            if (pos <= 0)
                return false;
        while (true) {
            int pos2 = pos;
            while (buffer.get(--pos2) != END_OF_FIX_MESSAGE)
                if (pos2 <= MIN_FIX_MSG_SIZE)
                    return false;
            if (buffer.get(pos2 + 1) == '1' && buffer.get(pos2 + 2) == '0' && buffer.get(pos2 + 3) == '=') {
                pos++; // include the ^A
                // found an end.
                excerpt.startExcerpt(1 + 8 + 4 + pos);
                excerpt.writeStopBit(pos);
                buffer.position(0);
                buffer.limit(pos);
                excerpt.write(buffer);
                // metadata in the footer.
                excerpt.writeLong(currentTimeNS());
                excerpt.finish();
                buffer.limit(end);
                buffer.position(pos);
                if (buffer.remaining() > 0)
                    buffer.compact();
                else
                    buffer.clear();
                return true;
            }
            pos = pos2;
        }
    }

    protected long currentTimeNS() {
        return System.nanoTime();
    }

    protected void readMoreData(ByteBuffer buffer) throws IOException {
        if (sc.read(buffer) < 0) {
            close();
        }
    }

    public void close() {
        closed = true;
        try {
            if (sc != null) sc.close();
        } catch (IOException ignored) {
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public Excerpt excerpt() {
        return excerpt;
    }
}
