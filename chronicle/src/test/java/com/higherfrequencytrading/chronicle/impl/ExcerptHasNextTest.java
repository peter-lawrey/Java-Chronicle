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

package com.higherfrequencytrading.chronicle.impl;

import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.tools.ChronicleTools;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Test correct behaviour of hasNextIndex() and nextIndex() methods
 *
 * @author kenota
 */
public class ExcerptHasNextTest {
    static final String TMP = System.getProperty("java.io.tmpdir");
    public static final int NUMBER_OF_ENTRIES = 12;


    @Test
    public void testHasNextIndexFail() throws IOException {
        final Chronicle chr = createChronicle("hasNextFail");

        final Excerpt readExcerpt = chr.createExcerpt();
        assertTrue("Read excerpt should have next index", readExcerpt.hasNextIndex());
        assertTrue("It should be possible to move to next index", readExcerpt.nextIndex());
    }

    @Test
    public void testHasNextIndexPass() throws IOException {
        final Chronicle chr = createChronicle("hasNextPass");

        final Excerpt readExcerpt = chr.createExcerpt();
        assertTrue("It should be possible to move to next index", readExcerpt.nextIndex());
        assertTrue("Read excerpt should have next index", readExcerpt.hasNextIndex());
    }

    @Test
    public void testHasNextIndexIteration() throws IOException {
        final Chronicle chr = createChronicle("testIteration");

        final Excerpt readExcerpt = chr.createExcerpt();
        readExcerpt.index(0);

        while (readExcerpt.hasNextIndex()) {
            assertTrue("I would expect nextIndex() return true after hasNextIndex() returns true",
                    readExcerpt.nextIndex());
        }
    }

    private Chronicle createChronicle(String name) throws IOException {
        final String basePath = TMP + File.separator + name;
        final IndexedChronicle chr = new IndexedChronicle(basePath);

        ChronicleTools.deleteOnExit(basePath);

        final Excerpt excerpt = chr.createExcerpt();

        for (int i = 0; i < NUMBER_OF_ENTRIES; ++i) {
            excerpt.startExcerpt(128);
            excerpt.writeBytes("test");
            excerpt.finish();
        }

        assertTrue("Chronicle should hold all values", NUMBER_OF_ENTRIES == excerpt.size());

        return chr;
    }
}

