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

package com.higherfrequencytrading.chronicle.impl;

import com.higherfrequencytrading.chronicle.Excerpt;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import static junit.framework.Assert.*;

/**
 * @author peter.lawrey
 */
public class IndexedChronicleTest {
    static final String TMP = System.getProperty("java.io.tmpdir");

    @Test
    public void rewritibleEntries() throws IOException {
        doRewriteableEntries(false);
        doRewriteableEntries(true);
    }

    private void doRewriteableEntries(boolean useUnsafe) throws IOException {
        String basePath = TMP + File.separator + "deleteme.ict";
        IndexedChronicle tsc = new IndexedChronicle(basePath);
        tsc.useUnsafe(useUnsafe);
        deleteOnExit(basePath);

        tsc.clear();
        Excerpt excerpt = tsc.createExcerpt();

        int counter = 1;
        for (int i = 0; i < 1024; i++) {
            excerpt.startExcerpt(129);
            for (int j = 0; j < 128; j += 8)
                excerpt.writeLong(counter++);
            excerpt.write(-1);
            excerpt.finish();
        }

        int counter2 = 1;
        Excerpt excerpt2 = tsc.createExcerpt();
        while (excerpt2.nextIndex()) {
            for (int j = 0; j < 128; j += 8) {
                long actual = excerpt2.readLong();
                long expected = counter2++;
                if (expected != actual)
                    assertEquals(expected, actual);
            }
            assertEquals(-1, excerpt2.readByte());
            excerpt2.finish();
        }
        assertEquals(counter, counter2);
        assertFalse(excerpt2.index(1024));
        tsc.close();
    }

    /**
     * Tests that <code>IndexedChronicle.close()</code> does not blow up (anymore) when you
     * reopen an existing chronicle due to the null data buffers created internally.
     *
     * @throws java.io.IOException if opening chronicle fails
     */
    @Test
    public void testCloseWithNullBuffers() throws IOException {
        String basePath = TMP + File.separator + "deleteme.ict";
        IndexedChronicle tsc = new IndexedChronicle(basePath, 12);
        deleteOnExit(basePath);

        tsc.clear();
        Excerpt excerpt = tsc.createExcerpt();
        for (int i = 0; i < 512; i++) {
            excerpt.startExcerpt(1);
            excerpt.writeByte(1);
            excerpt.finish();
        }
        tsc.close();

        tsc = new IndexedChronicle(basePath, 12);
        tsc.close();
    }


    /**
     * https://github.com/peter-lawrey/Java-Chronicle/issues/9
     *
     * @author AndrasMilassin
     */
    @Test
    public void test_boolean() throws Exception {
        String testPath = TMP + File.separator + "chroncle-bool-test";
        IndexedChronicle tsc = new IndexedChronicle(testPath, 12);
        tsc.useUnsafe(false);
        deleteOnExit(testPath);

        tsc.clear();

        Excerpt<IndexedChronicle> excerpt = tsc.createExcerpt();
        excerpt.startExcerpt(2);
        excerpt.writeBoolean(false);
        excerpt.writeBoolean(true);
        excerpt.finish();

        excerpt.index(0);
        boolean one = excerpt.readBoolean();
        boolean onetwo = excerpt.readBoolean();
        tsc.close();

        Assert.assertEquals(false, one);
        Assert.assertEquals(true, onetwo);
    }

    @Test
    public void testEnum() throws IOException {
        String testPath = TMP + File.separator + "chroncle-bool-enum";
        IndexedChronicle tsc = new IndexedChronicle(testPath, 12);
        tsc.useUnsafe(false);
        deleteOnExit(testPath);

        tsc.clear();

        Excerpt<IndexedChronicle> excerpt = tsc.createExcerpt();
        excerpt.startExcerpt(42);
        excerpt.writeEnum(AccessMode.EXECUTE);
        excerpt.writeEnum(AccessMode.READ);
        excerpt.writeEnum(AccessMode.WRITE);
        excerpt.writeEnum(BigInteger.ONE);
        excerpt.writeEnum(BigInteger.TEN);
        excerpt.writeEnum(BigInteger.ZERO);
        excerpt.writeEnum(BigInteger.ONE);
        excerpt.writeEnum(BigInteger.TEN);
        excerpt.writeEnum(BigInteger.ZERO);
        excerpt.finish();
        System.out.println("size=" + excerpt.position());

        excerpt.index(0);
        AccessMode e = excerpt.readEnum(AccessMode.class);
        AccessMode r = excerpt.readEnum(AccessMode.class);
        AccessMode w = excerpt.readEnum(AccessMode.class);
        BigInteger one = excerpt.readEnum(BigInteger.class);
        BigInteger ten = excerpt.readEnum(BigInteger.class);
        BigInteger zero = excerpt.readEnum(BigInteger.class);
        BigInteger one2 = excerpt.readEnum(BigInteger.class);
        BigInteger ten2 = excerpt.readEnum(BigInteger.class);
        BigInteger zero2 = excerpt.readEnum(BigInteger.class);
        tsc.close();

        assertSame(AccessMode.EXECUTE, e);
        assertSame(AccessMode.READ, r);
        assertSame(AccessMode.WRITE, w);
        assertEquals(BigInteger.ONE, one);
        assertEquals(BigInteger.TEN, ten);
        assertEquals(BigInteger.ZERO, zero);
        assertSame(one, one2);
        assertSame(ten, ten2);
        assertSame(zero, zero2);
    }

    private static void deleteOnExit(String basePath) {
        new File(basePath + ".data").deleteOnExit();
        new File(basePath + ".index").deleteOnExit();
    }

    @Test
    public void testSerializationPerformance() throws IOException, ClassNotFoundException {
        String testPath = TMP + File.separator + "chroncle-object";
        IndexedChronicle tsc = new IndexedChronicle(testPath);
        tsc.useUnsafe(true);
        deleteOnExit(testPath);

        tsc.clear();
        Excerpt excerpt = tsc.createExcerpt();
        int objects = 5000000;
        long start = System.nanoTime();
        for (int i = 0; i < objects; i++) {
            excerpt.startExcerpt(300);
            excerpt.writeObject(BigDecimal.valueOf(i % 1000));
            excerpt.finish();
        }
        for (int i = 0; i < objects; i++) {
            assertTrue(excerpt.index(i));
            BigDecimal bd = (BigDecimal) excerpt.readObject();
            assertEquals(i % 1000, bd.longValue());
            excerpt.finish();
        }
        tsc.close();
        long time = System.nanoTime() - start;
        System.out.printf("The average time to write and read a BigDecimal was %.1f us%n", time / 1e3 / objects);
    }

    static void assertEquals(long a, long b) {
        if (a != b)
            Assert.assertEquals(a, b);
    }

    static <T> void assertEquals(T a, T b) {
        if (a == null) {
            if (b == null) return;
        } else if (a.equals(b)) {
            return;
        }
        Assert.assertEquals(a, b);
    }
}
