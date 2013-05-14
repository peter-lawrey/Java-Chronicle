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
        if (buffer.get(--pos) != END_OF_FIX_MESSAGE) {
            do {
                if (pos <= 0)
                    return false;
            } while (buffer.get(--pos) != END_OF_FIX_MESSAGE);
        }
        do {
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
        } while (true);
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
