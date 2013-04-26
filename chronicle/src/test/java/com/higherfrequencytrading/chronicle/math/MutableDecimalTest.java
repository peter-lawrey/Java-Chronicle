package com.higherfrequencytrading.chronicle.math;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Random;

import static junit.framework.Assert.assertEquals;

/**
 * @author peter.lawrey
 */
public class MutableDecimalTest {

    public static final BigDecimal BD_2_63 = BigDecimal.valueOf(2).pow(63);

    @Test
    public void testLongDoubleValue() throws Exception {
        for (int i = -20; i <= 20; i++) {
            testCompare(Long.MIN_VALUE, i);
            testCompare(Long.MAX_VALUE, i);
            testCompare(0, i);
            testCompare(-1, i);
            testCompare(1, i);
        }
        Random rand = new Random();
        for (int i = 0; i < 200000; i++) {
            long value = rand.nextLong();
            int scale = rand.nextInt(40) - 20;
            testCompare(value, scale);
        }
    }

    private void testCompare(long value, int scale) {
        BigDecimal bd = BigDecimal.valueOf(value, scale);
        MutableDecimal md = new MutableDecimal(value, scale);
        assertEquals(bd.toPlainString(), md.toString());
        String message = "value=" + value + ", scale=" + scale;
        if (bd.abs().compareTo(BD_2_63) < 0)
            assertEquals(message, bd.longValue(), md.longValue());
        assertEquals(message, bd.doubleValue(), md.doubleValue(), Math.abs(bd.doubleValue() / 1e15));

        MutableDecimal md1 = new MutableDecimal(value - 1, scale);
        BigDecimal bd1 = BigDecimal.valueOf(value - 1, scale);
        assertEquals(message, bd.compareTo(bd1), md.compareTo(md1));
        MutableDecimal md2 = new MutableDecimal(value + 1, scale);
        BigDecimal bd2 = BigDecimal.valueOf(value + 1, scale);
        assertEquals(message, bd.compareTo(bd2), md.compareTo(md2));
        MutableDecimal md3 = new MutableDecimal(value / 10 - 1, scale - 1);
        BigDecimal bd3 = BigDecimal.valueOf(value / 10 - 1, scale - 1);
        assertEquals(message, bd.compareTo(bd3), md.compareTo(md3));
        MutableDecimal md4 = new MutableDecimal(value / 10 + 1, scale - 1);
        BigDecimal bd4 = BigDecimal.valueOf(value / 10 + 1, scale - 1);
        assertEquals(message, bd.compareTo(bd4), md.compareTo(md4));
    }
}
