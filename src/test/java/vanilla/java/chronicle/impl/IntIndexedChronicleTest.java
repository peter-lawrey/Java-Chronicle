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

package vanilla.java.chronicle.impl;

import org.junit.Test;
import vanilla.java.chronicle.Excerpt;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.*;

/**
 * @author peter.lawrey
 */
public class IntIndexedChronicleTest {
    @Test
    public void rewritibleEntries() throws IOException {
        doRewriteableEntries(false);
        doRewriteableEntries(true);
    }

    private void doRewriteableEntries(boolean useUnsafe) throws IOException {
        String basePath = "/tmp/deleteme.iict";
        IndexedChronicle tsc = new IntIndexedChronicle(basePath, 12);
        tsc.useUnsafe(useUnsafe);
        deleteOnExit(basePath);

        tsc.clear();
        Excerpt excerpt = tsc.createExcerpt();

        int counter = 0;
        for (int i = 0; i < 1024; i++) {
            excerpt.startExcerpt(129);
            for (int j = 0; j < 128; j += 8)
                excerpt.writeLong(counter++);
            excerpt.write(-1);
            excerpt.finish();
        }

        counter = 0;
        for (int i = 0; i < 1024; i++) {
            assertTrue(excerpt.index(i));
            for (int j = 0; j < 128; j += 8) {
                long actual = excerpt.readLong();
                long expected = counter++;
                if (expected != actual)
                    assertEquals(expected, actual);
            }
            assertEquals(-1, excerpt.readByte());
            excerpt.finish();
        }
        assertFalse(excerpt.index(1024));
        tsc.close();
    }


    private static void deleteOnExit(String basePath) {
        new File(basePath + ".data").deleteOnExit();
        new File(basePath + ".index").deleteOnExit();
    }
}
