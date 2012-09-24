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
package vanilla.java.mailgroup.stringipc;

/**
 * @author plawrey
 */

import vanilla.java.chronicle.Excerpt;
import vanilla.java.chronicle.impl.IndexedChronicle;

import java.io.IOException;

public class DirectMemoryWriter {
    public static final String BASE_DIR = System.getProperty("java.io.tmpdir") + "/deleteme.iictm.";
    final String basePath = BASE_DIR + "request";
    final IndexedChronicle tsc;
    final Excerpt<IndexedChronicle> excerpt;

    public DirectMemoryWriter() throws IOException {
        tsc = new IndexedChronicle(basePath, DirectMemoryReader.DATA_BIT_SIZE_HINT);
        tsc.useUnsafe(DirectMemoryReader.USE_UNSAFE);

        excerpt = tsc.createExcerpt();
    }

    public void write(CharSequence s) {
        excerpt.startExcerpt(s.length() * 3);
        excerpt.writeUTF(s);
        excerpt.finish();
    }

    public static void main(String[] args) throws IOException {
        DirectMemoryWriter dmw = new DirectMemoryWriter();
        for (int i = 0; i < 100; i++) {
            dmw.write("atest");
            dmw.write("btest");
        }
    }
}


