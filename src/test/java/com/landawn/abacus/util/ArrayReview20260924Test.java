package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Tests for the {@code Array} findings of the 2026-09-24 review (ledger C-302..C-310).
 */
public class ArrayReview20260924Test extends TestBase {

    // ============================================================ C-304 repeat(.., Class): primitive / void element class

    private static void assertElementClassIae(final org.junit.jupiter.api.function.Executable call) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
        assertNotNull(e.getMessage());
        assertTrue(e.getMessage().contains("elementClass"), e.getMessage());
    }

    @Test
    public void testC304_repeatElementWithPrimitiveClassThrowsNamedIae() {
        assertElementClassIae(() -> Array.repeat(1, 3, int.class));
        assertElementClassIae(() -> Array.repeat(1, 0, int.class));
        assertElementClassIae(() -> Array.repeat(1L, 2, long.class));
        assertElementClassIae(() -> Array.repeat(Boolean.TRUE, 2, boolean.class));
        assertElementClassIae(() -> Array.repeat((Object) null, 3, void.class));
        assertElementClassIae(() -> Array.repeat((Object) null, 0, void.class));
    }

    @Test
    public void testC304_repeatArrayWithPrimitiveClassThrowsNamedIae() {
        assertElementClassIae(() -> Array.repeat(new Integer[] { 1 }, 2, int.class));
        assertElementClassIae(() -> Array.repeat(new Integer[0], 2, int.class));
        assertElementClassIae(() -> Array.repeat((Integer[]) null, 2, int.class));
        assertElementClassIae(() -> Array.repeat(new Double[] { 1.5 }, 0, double.class));
        assertElementClassIae(() -> Array.repeat(new Object[] { 1 }, 2, void.class));
    }

    @Test
    public void testC304_repeatWithReferenceClassStillWorks() {
        final String[] s = Array.repeat((String) null, 3, String.class);
        assertArrayEquals(new String[] { null, null, null }, s);
        assertSame(String[].class, s.getClass());

        final Number[] widened = Array.<Number> repeat(new Integer[] { 1, 2 }, 2, Number.class);
        assertSame(Number[].class, widened.getClass());
        assertArrayEquals(new Number[] { 1, 2, 1, 2 }, widened);

        final Void[] voids = Array.repeat((Void) null, 2, Void.class); // the wrapper class is a reference type
        assertEquals(2, voids.length);

        // null elementClass and negative n keep their own messages
        final IllegalArgumentException nullClass = assertThrows(IllegalArgumentException.class, () -> Array.repeat("x", 1, (Class<String>) null));
        assertTrue(nullClass.getMessage().contains("elementClass"), nullClass.getMessage());
        final IllegalArgumentException negN = assertThrows(IllegalArgumentException.class, () -> Array.repeat(1, -1, int.class));
        assertTrue(negN.getMessage().contains("'n'"), negN.getMessage());
    }

    @Test
    public void testC304_repeatArrayStoreExceptionOnlyWhenAnElementIsCopied() {
        assertThrows(ArrayStoreException.class, () -> Array.<Number> repeat(new Number[] { 1.5 }, 2, Integer.class));
        final Number[] empty = Array.<Number> repeat(new Number[] { 1.5 }, 0, Integer.class);
        assertEquals(0, empty.length);
        assertSame(Integer[].class, empty.getClass());
    }

    // ============================================================ C-306 newInstance(void.class, ..) message

    @Test
    public void testC306_newInstanceVoidHasMessage() {
        for (final org.junit.jupiter.api.function.Executable call : List.<org.junit.jupiter.api.function.Executable> of(
                () -> Array.newInstance(void.class, 0), () -> Array.newInstance(void.class, 5), () -> Array.newInstance(void.class, new int[] { 1 }),
                () -> Array.newInstance(void.class, new int[] { 1, 2 }), () -> Array.newInstance(void.class, new int[0]), () -> N.newArray(void.class, 3))) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
            assertEquals("'componentType' cannot be void", e.getMessage());
        }
    }

    @Test
    public void testC306_newInstanceOtherCasesUnchanged() {
        final Void[] v = Array.newInstance(Void.class, 2);
        assertEquals(2, v.length);
        final int[] ints = Array.newInstance(int.class, 0);
        assertEquals(0, ints.length);
        final IllegalArgumentException nullType = assertThrows(IllegalArgumentException.class, () -> Array.newInstance(null, 1));
        assertTrue(nullType.getMessage().contains("componentType"), nullType.getMessage());
        assertThrows(NegativeArraySizeException.class, () -> Array.newInstance(String.class, -1));
        final IllegalArgumentException noDims = assertThrows(IllegalArgumentException.class, () -> Array.newInstance(String.class, new int[0]));
        assertTrue(noDims.getMessage().contains("dimensions"), noDims.getMessage());
    }

    // ============================================================ C-310 exact element count in the long stepped overflow messages

    @Test
    public void testC310_rangeClosedLongStepMessageReportsLengthNotQuotient() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE, 1L));
        assertTrue(e.getMessage().endsWith(": 18446744073709551616"), e.getMessage()); // 2^64 elements, not 2^64 - 1

        final IllegalArgumentException neg = assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(Long.MAX_VALUE, Long.MIN_VALUE, -1L));
        assertTrue(neg.getMessage().endsWith(": 18446744073709551616"), neg.getMessage());
    }

    @Test
    public void testC310_rangeLongStepMessageReportsExactLength() {
        // (MAX - MIN) / 2 = 2^63 - 1 full steps, and the last step stops short of MAX: 2^63 elements
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Array.range(Long.MIN_VALUE, Long.MAX_VALUE, 2L));
        assertTrue(e.getMessage().endsWith(": 9223372036854775808"), e.getMessage());

        // exact multiple: the count is the quotient itself
        final IllegalArgumentException exact = assertThrows(IllegalArgumentException.class, () -> Array.range(Long.MIN_VALUE, Long.MAX_VALUE, 1L));
        assertTrue(exact.getMessage().endsWith(": 18446744073709551615"), exact.getMessage());
    }

    @Test
    public void testC310_longStepBigIntegerPathStillProducesCorrectValues() {
        assertArrayEquals(new long[] { Long.MIN_VALUE, -1L, Long.MAX_VALUE - 1 }, Array.range(Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE));
        assertArrayEquals(new long[] { Long.MIN_VALUE, -1L }, Array.range(Long.MIN_VALUE, Long.MAX_VALUE - 1, Long.MAX_VALUE));
        assertArrayEquals(new long[] { Long.MIN_VALUE, -1L, Long.MAX_VALUE - 1 }, Array.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE));
        assertArrayEquals(new long[] { Long.MIN_VALUE, -1L, Long.MAX_VALUE - 1 }, Array.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE - 1, Long.MAX_VALUE));
        assertArrayEquals(new long[] { Long.MAX_VALUE, 0L, Long.MIN_VALUE + 1 }, Array.range(Long.MAX_VALUE, Long.MIN_VALUE, -Long.MAX_VALUE));
        assertArrayEquals(new long[] { Long.MAX_VALUE, 0L, Long.MIN_VALUE + 1 }, Array.rangeClosed(Long.MAX_VALUE, Long.MIN_VALUE, -Long.MAX_VALUE));
        // unchanged small cases
        assertArrayEquals(new long[] { 0, 3, 6, 9 }, Array.range(0L, 10L, 3L));
        assertArrayEquals(new long[] { 0, 5, 10 }, Array.rangeClosed(0L, 10L, 5L));
        assertArrayEquals(new long[] { 7 }, Array.rangeClosed(7L, 7L, -3L));
        assertEquals(0, Array.range(7L, 7L, 3L).length);
    }

    // ============================================================ C-305 whole-array repeat (doubling fill): output unchanged

    @Test
    public void testC305_repeatIntMatchesNaiveForManyShapes() {
        for (int aLen = 1; aLen <= 7; aLen++) {
            final int[] a = new int[aLen];
            for (int i = 0; i < aLen; i++) {
                a[i] = i * 31 - 7;
            }
            for (int n = 0; n <= 70; n++) {
                final int[] expected = new int[aLen * n];
                for (int i = 0; i < expected.length; i++) {
                    expected[i] = a[i % aLen];
                }
                final int[] actual = Array.repeat(a, n);
                assertArrayEquals(expected, actual, "aLen=" + aLen + ", n=" + n);
                if (aLen * n > 0) {
                    assertNotSame(a, actual);
                }
            }
        }
    }

    @Test
    public void testC305_repeatAllPrimitiveOverloads() {
        assertArrayEquals(new boolean[] { true, false, true, false, true, false }, Array.repeat(new boolean[] { true, false }, 3));
        assertArrayEquals(new char[] { 'a', 'b', 'a', 'b', 'a', 'b', 'a', 'b', 'a', 'b' }, Array.repeat(new char[] { 'a', 'b' }, 5));
        assertArrayEquals(new byte[] { 1, 2, 3, 1, 2, 3, 1, 2, 3 }, Array.repeat(new byte[] { 1, 2, 3 }, 3));
        assertArrayEquals(new short[] { -1, -1, -1, -1, -1, -1, -1 }, Array.repeat(new short[] { -1 }, 7));
        assertArrayEquals(new long[] { Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE }, Array.repeat(new long[] { Long.MIN_VALUE, Long.MAX_VALUE }, 2));
        assertArrayEquals(new float[] { Float.NaN, -0.0f, Float.NaN, -0.0f, Float.NaN, -0.0f }, Array.repeat(new float[] { Float.NaN, -0.0f }, 3));
        assertArrayEquals(new double[] { 1.5, Double.NaN, -0.0, 1.5, Double.NaN, -0.0 }, Array.repeat(new double[] { 1.5, Double.NaN, -0.0 }, 2));
        assertArrayEquals(new String[] { "x", null, "x", null, "x", null }, Array.repeat(new String[] { "x", null }, 3));
        assertArrayEquals(new Integer[] { 1, null, 1, null }, Array.repeat(new Integer[] { 1, null }, 2, Integer.class));
    }

    @Test
    public void testC305_repeatNullEmptyAndZero() {
        assertEquals(0, Array.repeat((int[]) null, 5).length);
        assertEquals(0, Array.repeat(new int[0], 5).length);
        assertEquals(0, Array.repeat(new int[] { 1, 2 }, 0).length);
        assertEquals(0, Array.repeat(new String[] { "a" }, 0).length);
        assertEquals(0, Array.repeat(new Integer[] { 1 }, 0, Integer.class).length);
        assertThrows(IllegalArgumentException.class, () -> Array.repeat(new int[] { 1 }, -1));
        final IllegalArgumentException overflow = assertThrows(IllegalArgumentException.class, () -> Array.repeat(new byte[] { 1, 2, 3 }, Integer.MAX_VALUE));
        assertTrue(overflow.getMessage().contains("6442450941"), overflow.getMessage());
    }

    @Test
    public void testC305_repeatUnicodeAndLargeCount() {
        final char[] emoji = "😀é".toCharArray(); // a surrogate pair plus a BMP letter
        assertEquals("😀é😀é😀é", new String(Array.repeat(emoji, 3)));
        assertArrayEquals(new String[] { "中", "😀", "中", "😀" }, Array.repeat(new String[] { "中", "😀" }, 2));

        final int n = 1_000_003; // not a power of two, so the last doubling step is partial
        final int[] big = Array.repeat(new int[] { 4, 5, 6 }, n);
        assertEquals(3 * n, big.length);
        for (int i = 0; i < big.length; i++) {
            if (big[i] != 4 + i % 3) {
                throw new AssertionError("mismatch at " + i + ": " + big[i]);
            }
        }
    }

    @Test
    public void testC305_repeatDoesNotAliasOrMutateSource() {
        final long[] a = { 1, 2 };
        final long[] r = Array.repeat(a, 4);
        r[0] = 99;
        assertArrayEquals(new long[] { 1, 2 }, a);
        assertArrayEquals(new long[] { 99, 2, 1, 2, 1, 2, 1, 2 }, r);
    }

    // ============================================================ C-303 / C-307 documented contrasts with N

    @Test
    public void testC303_NConcatOnTwoDimensionalArraysAppendsWhileArrayConcatMerges() {
        final int[][] p = { { 1, 2 }, { 3 } };
        final int[][] q = { { 4 }, { 5, 6 } };
        final int[][] appended = N.concat(p, q);
        assertEquals(4, appended.length);
        assertArrayEquals(new int[] { 5, 6 }, appended[3]);
        final int[][] merged = Array.concat(p, q);
        assertEquals(2, merged.length);
        assertArrayEquals(new int[] { 1, 2, 4 }, merged[0]);
        assertArrayEquals(new int[] { 3, 5, 6 }, merged[1]);
    }

    @Test
    public void testC302_concatNormalisesNullRowsAndLayersOfTheCopiedSide() {
        final long[][] onlyB = Array.concat((long[][]) null, new long[][] { { 1L }, null });
        assertArrayEquals(new long[] { 1L }, onlyB[0]);
        assertEquals(0, onlyB[1].length);

        final double[][][] onlyA = Array.concat(new double[][][] { { null, { 2.0 } }, null }, (double[][][]) null);
        assertEquals(2, onlyA.length);
        assertEquals(0, onlyA[0][0].length);
        assertArrayEquals(new double[] { 2.0 }, onlyA[0][1]);
        assertEquals(0, onlyA[1].length);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testC307_arrayAsListIsAViewWhileNAsListIsACopy() {
        final String[] arr = { "a", "b" };
        final List<String> view = Array.asList(arr);
        view.set(0, "x");
        assertEquals("x", arr[0]);
        arr[1] = "y";
        assertEquals("y", view.get(1));

        final List<String> copy = N.asList(arr);
        assertThrows(UnsupportedOperationException.class, () -> copy.set(0, "z"));
        arr[0] = "changed";
        assertEquals("x", copy.get(0));

        assertTrue(Array.asList((String[]) null).isEmpty());
        assertTrue(Array.asList(new String[0]).isEmpty());
    }
}
