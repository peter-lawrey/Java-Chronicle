/*
 * Copyright 2011 Peter Lawrey
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
package com.higherfrequencytrading.stringipc;

import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;

import java.io.IOException;

public class DirectMemoryReader {

    static final int DATA_BIT_SIZE_HINT = 24;
    static final boolean USE_UNSAFE = false;
    static final String BASE_DIR = System.getProperty("java.io.tmpdir") + "/deleteme.iictm.";

    final String basePath = BASE_DIR + "request";
    final IndexedChronicle tsc;
    final Excerpt<IndexedChronicle> excerpt;
    int currentExcerptIndex = 0;


    public DirectMemoryReader() throws IOException {
        tsc = new IndexedChronicle(basePath, DATA_BIT_SIZE_HINT);
        tsc.useUnsafe(USE_UNSAFE);

        excerpt = tsc.createExcerpt();
        excerpt.startExcerpt(100);
    }

    public boolean read(StringBuilder sb) {
        if (excerpt.index(currentExcerptIndex)) {
            currentExcerptIndex++;
            excerpt.readUTF(sb);
            excerpt.finish();
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws IOException {
        DirectMemoryReader dmr = new DirectMemoryReader();

        int i = 0;
        long last = System.nanoTime();
        StringBuilder sb = new StringBuilder();
        //noinspection InfiniteLoopStatement
        while (true) {
            if (dmr.read(sb)) {
                System.out.println(sb);
                sb.setLength(0);

            } else {
                long now = System.nanoTime();
                if (now - last >= 1e9) {
                    System.out.println("waited " + ++i + " sec");
                    last = now;
                }
            }
        }
    }
}