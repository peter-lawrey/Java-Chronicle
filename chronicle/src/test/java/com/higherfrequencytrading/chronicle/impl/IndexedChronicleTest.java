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
import com.higherfrequencytrading.chronicle.tools.ChronicleTools;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.*;

import static junit.framework.Assert.*;

/**
 * @author peter.lawrey
 */
public class IndexedChronicleTest {
    static final String TMP = System.getProperty("java.io.tmpdir");

    @Test
    public void rewritibleEntries() throws IOException {
        boolean[] booleans = {false, true};
        for (boolean useUnsafe : booleans)
            for (boolean minimiseByteBuffers : booleans)
                for (boolean synchronousMode : booleans)
                    doRewriteableEntries(useUnsafe, minimiseByteBuffers, synchronousMode);

    }

    private void doRewriteableEntries(boolean useUnsafe, boolean minimiseByteBuffers, boolean synchronousMode) throws IOException {
        String basePath = TMP + File.separator + "deleteme.ict";
        IndexedChronicle tsc = ChronicleBuilder.newIndexedChronicleBuilder(basePath)
                .dataBitSizeHint(IndexedChronicle.DEFAULT_DATA_BITS_SIZE32)
                .minimiseByteBuffers(minimiseByteBuffers)
                .useSynchronousMode(synchronousMode)
                .useUnsafe(useUnsafe).build();

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
     * Tests that <code>IndexedChronicle.close()</code> does not blow up (anymore) when you reopen an existing chronicle
     * due to the null data buffers created internally.
     *
     * @throws java.io.IOException if opening chronicle fails
     */
    @Test
    public void testCloseWithNullBuffers() throws IOException {
        String basePath = TMP + File.separator + "deleteme.ict";
        deleteOnExit(basePath);
        IndexedChronicle tsc = new IndexedChronicle(basePath, 12);

        tsc.clear();
        Excerpt excerpt = tsc.createExcerpt();
        for (int i = 0; i < 512; i++) {
            excerpt.startExcerpt(1);
            excerpt.writeByte(1);
            excerpt.finish();
        }
        // used to throw NPE if you have finished already.
        excerpt.close();
        tsc.close();

        tsc = new IndexedChronicle(basePath, 12);
        tsc.createExcerpt().close();
        tsc.close(); // used to throw an exception.
    }

    @Test
    @Ignore
    public void testTimeTenMillion() throws IOException {
        int repeats = 3;
        for (int j = 0; j < repeats; j++) {
            long start = System.nanoTime();
            String basePath = TMP + File.separator + "testTimeTenMillion";
            deleteOnExit(basePath);
            int records = 10 * 1000 * 1000;
            {
                IndexedChronicle ic = new IndexedChronicle(basePath);
                ic.useUnsafe(true);
                ic.clear();
                Excerpt excerpt = ic.createExcerpt();
                for (int i = 1; i <= records; i++) {
                    excerpt.startExcerpt(16);
                    excerpt.writeLong(i);
                    excerpt.writeDouble(i);
                    excerpt.finish();
                }
                ic.close();
            }
            {
                IndexedChronicle ic = new IndexedChronicle(basePath);
                ic.useUnsafe(true);
                Excerpt excerpt = ic.createExcerpt();
                for (int i = 1; i <= records; i++) {
                    boolean found = excerpt.nextIndex();
                    if (!found)
                        assertTrue(found);
                    long l = excerpt.readLong();
                    double d = excerpt.readDouble();
                    if (l != i)
                        assertEquals(i, l);
                    if (d != i)
                        assertEquals((double) i, d);
                    excerpt.finish();
                }
                ic.close();
            }
            long time = System.nanoTime() - start;
            System.out.printf("Time taken %,d ms", time / 1000000);
        }
    }


    /**
     * https://github.com/peter-lawrey/Java-Chronicle/issues/9
     *
     * @author AndrasMilassin
     */
    @Test
    public void test_boolean() throws Exception {
        String testPath = TMP + File.separator + "chroncle-bool-test";
        deleteOnExit(testPath);
        IndexedChronicle tsc = new IndexedChronicle(testPath, 12);
        tsc.useUnsafe(false);

        Excerpt excerpt = tsc.createExcerpt();
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
    public void testStopBitEncoded() throws IOException {
        String testPath = TMP + File.separator + "chroncle-stop-bit";
        IndexedChronicle tsc = new IndexedChronicle(testPath, 12);
        deleteOnExit(testPath);

        Excerpt reader = tsc.createExcerpt();
        Excerpt writer = tsc.createExcerpt();
        long[] longs = {Long.MIN_VALUE, Integer.MIN_VALUE, Short.MIN_VALUE, Character.MIN_VALUE, Byte.MIN_VALUE,
                Long.MAX_VALUE, Integer.MAX_VALUE, Short.MAX_VALUE, Character.MAX_CODE_POINT, Character.MAX_VALUE, Byte.MAX_VALUE};
        for (long l : longs) {
            writer.startExcerpt(12);
            writer.writeChar('T');
            writer.writeStopBit(l);
            writer.finish();

            reader.nextIndex();
            reader.readChar();
            long l2 = reader.readStopBit();
            reader.finish();
            assertEquals(l, l2);
        }
        writer.startExcerpt(longs.length * 10);
        writer.writeChar('t');
        for (long l : longs)
            writer.writeStopBit(l);
        writer.finish();

        reader.nextIndex();
        reader.readChar();
        for (long l : longs) {
            long l2 = reader.readStopBit();
            assertEquals(l, l2);
        }
        assertEquals(0, reader.remaining());
        reader.finish();
    }

    @Test
    public void testEnum() throws IOException {
        String testPath = TMP + File.separator + "chroncle-bool-enum";
        IndexedChronicle tsc = new IndexedChronicle(testPath, 12);
        tsc.useUnsafe(false);
        deleteOnExit(testPath);

        tsc.clear();

        Excerpt excerpt = tsc.createExcerpt();
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
    public void testSerializationPerformance() throws IOException, ClassNotFoundException, InterruptedException {
        String testPath = TMP + File.separator + "chronicle-object";
        IndexedChronicle tsc = new IndexedChronicle(testPath, 16, ByteOrder.nativeOrder(), true);
        tsc.useUnsafe(true);
        deleteOnExit(testPath);

        tsc.clear();
        Excerpt excerpt = tsc.createExcerpt();
        int objects = 5000000;
        long start = System.nanoTime();
        for (int i = 0; i < objects; i++) {
            excerpt.startExcerpt(28);
            excerpt.writeObject(BigDecimal.valueOf(i % 1000));
            excerpt.finish();
        }
        for (int i = 0; i < objects; i++) {
            assertTrue(excerpt.index(i));
            BigDecimal bd = (BigDecimal) excerpt.readObject();
            assertEquals(i % 1000, bd.longValue());
            excerpt.finish();
        }
//        System.out.println("waiting");
//        Thread.sleep(20000);
//        System.out.println("waited");
//        System.gc();
        tsc.close();
        long time = System.nanoTime() - start;
        System.out.printf("The average time to write and read a double was %.1f us%n", time / 1e3 / objects / 10);
//        tsc = null;
//        System.gc();
//        Thread.sleep(10000);
    }

    static void assertEquals(long a, long b) {
        if (a != b)
            Assert.assertEquals(a, b);
    }

    static <T> void assertEquals(@Nullable T a, @Nullable T b) {
        if (a == null) {
            if (b == null) return;
        } else if (a.equals(b)) {
            return;
        }
        Assert.assertEquals(a, b);
    }


    @Test
    public void testFindRange() throws IOException {
        final String basePath = TMP + "/testFindRange";
        ChronicleTools.deleteOnExit(basePath);

        IndexedChronicle chronicle = new IndexedChronicle(basePath);
        Excerpt appender = chronicle.createExcerpt();
        List<Integer> ints = new ArrayList<Integer>();
        for (int i = 0; i < 1000; i += 10) {
            appender.startExcerpt(4);
            appender.writeInt(i);
            appender.finish();
            ints.add(i);
        }
        Excerpt excerpt = chronicle.createExcerpt();
        final MyExcerptComparator mec = new MyExcerptComparator();
        // exact matches at a the start

        mec.lo = mec.hi = -1;
        assertEquals(~0, excerpt.findExact(mec));
        mec.lo = mec.hi = 0;
        assertEquals(0, excerpt.findExact(mec));
        mec.lo = mec.hi = 9;
        assertEquals(~1, excerpt.findExact(mec));
        mec.lo = mec.hi = 10;
        assertEquals(1, excerpt.findExact(mec));

        // exact matches at a the end
        mec.lo = mec.hi = 980;
        assertEquals(98, excerpt.findExact(mec));
        mec.lo = mec.hi = 981;
        assertEquals(~99, excerpt.findExact(mec));
        mec.lo = mec.hi = 990;
        assertEquals(99, excerpt.findExact(mec));
        mec.lo = mec.hi = 1000;
        assertEquals(~100, excerpt.findExact(mec));


        // range match near the start
        long[] startEnd = new long[2];

        mec.lo = 0;
        mec.hi = 3;
        excerpt.findRange(startEnd, mec);
        assertEquals("[0, 1]", Arrays.toString(startEnd));

        mec.lo = 21;
        mec.hi = 29;
        excerpt.findRange(startEnd, mec);
        assertEquals("[3, 3]", Arrays.toString(startEnd));

        /*
        mec.lo = 129;
        mec.hi = 631;
        testSearchRange(ints, excerpt, mec, startEnd);
*/
        Random rand = new Random(1);
        for (int i = 0; i < 1000; i++) {
            int x = rand.nextInt(1010) - 5;
            int y = rand.nextInt(1010) - 5;
            mec.lo = Math.min(x, y);
            mec.hi = Math.max(x, y);
            testSearchRange(ints, excerpt, mec, startEnd);
        }

        chronicle.close();
    }


    static void testSearchRange(List<Integer> ints, Excerpt excerpt, MyExcerptComparator mec, long[] startEnd) {
        int elo = Collections.binarySearch(ints, mec.lo);
        if (elo < 0) elo = ~elo;
        int ehi = Collections.binarySearch(ints, mec.hi);
        if (ehi < 0)
            ehi = ~ehi;
        else ehi++;
        excerpt.findRange(startEnd, mec);
        Assert.assertEquals("lo: " + mec.lo + ", hi: " + mec.hi,
                "[" + elo + ", " + ehi + "]",
                Arrays.toString(startEnd));
    }


    static class MyExcerptComparator implements ExcerptComparator {
        int lo, hi;

        @Override
        public int compare(Excerpt excerpt) {
            final int x = excerpt.readInt();
            return x < lo ? -1 : x > hi ? +1 : 0;
        }
    }
}
