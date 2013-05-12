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
