package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class LongMedianTest extends TestBase {

    @Test
    void longMedianPreservesCancellationAndRoundsTheExactMean() {
        assertMean(-0.5d, Long.MIN_VALUE, Long.MAX_VALUE);
        assertMean(-1.5d, Long.MIN_VALUE, Long.MAX_VALUE - 2);
        assertMean(9007199254740994d, 9007199254740993L, 9007199254740994L);
        assertMean(-9007199254740994d, -9007199254740993L, -9007199254740994L);
        assertMean(Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE);
        assertMean(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);
        assertMean(0d, -1, 1);
        assertMean(0.5d, 0, 1);
        assertMean(-0.5d, 0, -1);
    }

    @Test
    void longMedianMatchesAnExactDecimalOracle() {
        final Random random = new Random(27092026L);
        for (int i = 0; i < 2500; i++) {
            final long a = random.nextLong();
            final long b = random.nextLong();
            final double expected = BigDecimal.valueOf(a).add(BigDecimal.valueOf(b)).divide(BigDecimal.valueOf(2)).doubleValue();
            assertMean(expected, a, b);
        }
    }

    @Test
    void longMedianPreservesOddInputsAndRangeValidation() {
        assertEquals(Long.MAX_VALUE, N.median(new long[] { Long.MAX_VALUE }));
        assertEquals(7d, N.median(new long[] { Long.MAX_VALUE, 7, Long.MIN_VALUE }));
        assertEquals(7d, N.median(new long[] { 11, 7, 5, 3, Long.MAX_VALUE }));
        assertEquals(7d, N.median(new long[] { 0, 7, 0 }, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> N.median((long[]) null));
        assertThrows(IllegalArgumentException.class, () -> N.median(new long[0]));
        assertThrows(IllegalArgumentException.class, () -> N.median((long[]) null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> N.median(new long[] { 1 }, 1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.median((long[]) null, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.median(new long[] { 1 }, -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.median(new long[] { 1 }, 1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.median(new long[] { 1 }, 0, 2));
    }

    private static void assertMean(final double expected, final long a, final long b) {
        assertEquals(expected, N.median(new long[] { a, b }));
        assertEquals(expected, N.median(new long[] { b, a }));
        final long[] values = { b, a, b, a };
        final long[] original = values.clone();
        assertEquals(expected, N.median(values));
        assertArrayEquals(original, values);
        assertEquals(expected, N.median(new long[] { 7, a, b, 9 }, 1, 3));
        assertEquals(expected, N.median(new long[] { 7, b, a, b, a, 9 }, 1, 5));
    }
}
