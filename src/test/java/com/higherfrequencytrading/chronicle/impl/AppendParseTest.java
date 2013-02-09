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

import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.StopCharTester;
import com.higherfrequencytrading.chronicle.StopCharTesters;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static com.higherfrequencytrading.chronicle.tools.ChronicleTest.deleteOnExit;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * @author peter.lawrey
 */
public class AppendParseTest {
    static final String TMP = System.getProperty("java.io.tmpdir");
    public static final StopCharTester CTRL_A = StopCharTesters.forChar((char) 1);

    enum IOITransType {
        C, N, R
    }

    enum IOIQltyInd {
        H, M, L
    }

    @Test
    public void testFix() throws IOException {
        // 8=FIX.4.1^A9=154^A35=6^A49=BRKR^A56=INVMGR^A34=238^A52=19980604-07:59:56^A23=115686^A28=N^A55=FIA.MI^A54=2^A27=250000^A44=7900.000000^A25=H^A10=231^A
        String basePath = TMP + File.separator + "test-fix.ict";
        IndexedChronicle tsc = new IndexedChronicle(basePath, 12);
        deleteOnExit(basePath);

        Excerpt excerpt = tsc.createExcerpt();
        excerpt.startExcerpt(200);
        appendText(excerpt, 8, "FIX.4.1");
        appendNum(excerpt, 9, 154);
        appendNum(excerpt, 35, 6);
        appendText(excerpt, 49, "BRKR");
        appendText(excerpt, 56, "INVMGR");
        appendNum(excerpt, 34, 238);
        appendText(excerpt, 52, "19980604-07:59:56");
        appendNum(excerpt, 23, 115686);
        appendText(excerpt, 28, IOITransType.N);
        appendText(excerpt, 55, "FIA.MI");
        appendNum(excerpt, 54, 2);
        appendNum(excerpt, 27, 250000);
        appendNum(excerpt, 44, 7900.000000, 6);
        appendText(excerpt, 25, IOIQltyInd.H);
        appendNum(excerpt, 10, 231);
        excerpt.finish();

        assertTrue(excerpt.index(0));
        assertText(excerpt, 8, "FIX.4.1");
        assertNum(excerpt, 9, 154);
        assertNum(excerpt, 35, 6);
        assertText(excerpt, 49, "BRKR");
        assertText(excerpt, 56, "INVMGR");
        assertNum(excerpt, 34, 238);
        assertText(excerpt, 52, "19980604-07:59:56");
        assertNum(excerpt, 23, 115686);
        assertText(excerpt, 28, IOITransType.N);
        assertText(excerpt, 55, "FIA.MI");
        assertNum(excerpt, 54, 2);
        assertNum(excerpt, 27, 250000);
        assertNum(excerpt, 44, 7900.000000);
        assertText(excerpt, 25, IOIQltyInd.H);
        assertNum(excerpt, 10, 231);
        tsc.close();
    }

    private void assertText(Excerpt excerpt, int fid, String text) {
        assertEquals(fid, excerpt.parseLong());
        assertEquals(text, excerpt.parseEnum(String.class, CTRL_A));
    }

    private void assertText(Excerpt excerpt, int fid, Enum value) {
        assertEquals(fid, excerpt.parseLong());
        assertEquals(value, excerpt.parseEnum(value.getClass(), CTRL_A));
    }

    private void assertNum(Excerpt excerpt, int fid, long value) {
        assertEquals(fid, excerpt.parseLong());
        assertEquals(value, excerpt.parseLong());
    }

    private void assertNum(Excerpt excerpt, int fid, double value) {
        assertEquals(fid, excerpt.parseLong());
        assertEquals(value, excerpt.parseDouble());
    }

    private static void appendNum(Excerpt excerpt, int fid, long value) {
        excerpt.append(fid).append('=').append(value).append((char) 1);
    }

    private static void appendNum(Excerpt excerpt, int fid, double value, int precision) {
        excerpt.append(fid).append('=').append(value, precision).append((char) 1);
    }

    private static void appendText(Excerpt excerpt, int fid, String chars) {
        excerpt.append(fid).append('=').append(chars).append((char) 1);
    }

    private static void appendText(Excerpt excerpt, int fid, Enum value) {
        excerpt.append(fid).append('=').append(value).append((char) 1);
    }
}
