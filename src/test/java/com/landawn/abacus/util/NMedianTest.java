package com.landawn.abacus.util;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class NMedianTest extends NTestSupport {
    @Test
    public void testMedianThreeValues() {
        assertEquals('b', N.median('a', 'b', 'c'));
        assertEquals('b', N.median('c', 'b', 'a'));
        assertEquals((byte) 2, N.median((byte) 1, (byte) 2, (byte) 3));
        assertEquals((short) 2, N.median((short) 1, (short) 2, (short) 3));
        assertEquals(2, N.median(1, 2, 3));
        assertEquals(2L, N.median(1L, 2L, 3L));
        assertEquals(2.0f, N.median(1.0f, 2.0f, 3.0f), 0.001f);
        assertEquals(2.0, N.median(1.0, 2.0, 3.0), 0.001);
        assertEquals(2, N.median(1, 2, 3));
        assertEquals((Integer) 2, N.median(1, 2, 3, Comparator.naturalOrder()));
        assertEquals("b", N.median("a", "b", "c"));
        assertEquals("b", N.median("a", "b", "c", Comparator.naturalOrder()));
    }

    @Test
    public void testMedianArray() {
        assertEquals('b', N.lowerMedian('a', 'b', 'c', 'd'));
        assertEquals((byte) 2, N.lowerMedian((byte) 1, (byte) 2, (byte) 3, (byte) 4));
        assertEquals((short) 2, N.lowerMedian((short) 1, (short) 2, (short) 3, (short) 4));
        assertEquals(2, N.lowerMedian(1, 2, 3, 4));
        assertEquals(2L, N.lowerMedian(1L, 2L, 3L, 4L));
        assertEquals(2.0f, N.lowerMedian(1.0f, 2.0f, 3.0f, 4.0f), 0.001f);
        assertEquals(2.0, N.lowerMedian(1.0, 2.0, 3.0, 4.0), 0.001);
        assertEquals("b", N.lowerMedian(new String[] { "a", "b", "c", "d" }));
        assertEquals("b", N.lowerMedian(new String[] { "a", "b", "c", "d" }, Comparator.naturalOrder()));

        assertEquals(1, N.lowerMedian(1));

        assertEquals(1, N.lowerMedian(1, 2));

        assertEquals('b', N.lowerMedian(new char[] { 'a', 'b', 'c', 'd' }, 0, 3));
        assertEquals((byte) 2, N.lowerMedian(new byte[] { 1, 2, 3, 4 }, 0, 3));
        assertEquals((short) 2, N.lowerMedian(new short[] { 1, 2, 3, 4 }, 0, 3));
        assertEquals(2, N.lowerMedian(new int[] { 1, 2, 3, 4 }, 0, 3));
        assertEquals(2L, N.lowerMedian(new long[] { 1L, 2L, 3L, 4L }, 0, 3));
        assertEquals(2.0f, N.lowerMedian(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, 0, 3), 0.001f);
        assertEquals(2.0, N.lowerMedian(new double[] { 1.0, 2.0, 3.0, 4.0 }, 0, 3), 0.001);
        assertEquals("b", N.lowerMedian(new String[] { "a", "b", "c", "d" }, 0, 3));
        assertEquals("b", N.lowerMedian(new String[] { "a", "b", "c", "d" }, 0, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testMedianCollection() {
        assertEquals(Integer.valueOf(2), N.lowerMedian(Arrays.asList(1, 2, 3, 4)));
        assertEquals(Integer.valueOf(2), N.lowerMedian(Arrays.asList(1, 2, 3, 4), 0, 4));
        assertEquals(Integer.valueOf(2), N.lowerMedian(Arrays.asList(1, 2, 3, 4), Comparator.naturalOrder()));
        assertEquals(Integer.valueOf(2), N.lowerMedian(Arrays.asList(1, 2, 3, 4), 0, 4, Comparator.naturalOrder()));
    }

    @Test
    public void testMedian_three_primitives() {
        assertEquals('b', N.median('a', 'b', 'c'));
        assertEquals('b', N.median('c', 'b', 'a'));

        assertEquals((byte) 2, N.median((byte) 1, (byte) 2, (byte) 3));
        assertEquals((byte) 2, N.median((byte) 3, (byte) 2, (byte) 1));

        assertEquals((short) 2, N.median((short) 1, (short) 2, (short) 3));
        assertEquals((short) 2, N.median((short) 3, (short) 2, (short) 1));

        assertEquals(2, N.median(1, 2, 3));
        assertEquals(2, N.median(3, 2, 1));

        assertEquals(2L, N.median(1L, 2L, 3L));
        assertEquals(2L, N.median(3L, 2L, 1L));

        assertEquals(2.0f, N.median(1.0f, 2.0f, 3.0f), 0.001);
        assertEquals(2.0f, N.median(3.0f, 2.0f, 1.0f), 0.001);

        assertEquals(2.0, N.median(1.0, 2.0, 3.0), 0.001);
        assertEquals(2.0, N.median(3.0, 2.0, 1.0), 0.001);
    }

    @Test
    public void testMedian_three_comparable() {
        assertEquals("b", N.median("a", "b", "c"));
        assertEquals("b", N.median("c", "b", "a"));

        assertEquals("bee", N.median("ant", "bee", "tiger", Comparator.comparing(String::length)));
        assertEquals("ant", N.median("bee", "ant", "tiger", Comparator.comparing(String::length)));
        assertEquals("ant", N.median("bee", "tiger", "ant", Comparator.comparing(String::length)));
        assertEquals("bee", N.median("tiger", "bee", "ant", Comparator.comparing(String::length)));
        assertEquals("ant", N.median("be", "ant", "tiger", Comparator.comparing(String::length)));
        assertEquals("ant", N.median("be", "tiger", "ant", Comparator.comparing(String::length)));
        assertEquals("ant", N.median("tiger", "be", "ant", Comparator.comparing(String::length)));
    }

    @Test
    public void testMedian_three_withComparator() {
        Comparator<String> reverseComparator = Comparator.reverseOrder();
        assertEquals("b", N.median("a", "b", "c", reverseComparator));
        assertEquals("b", N.median("c", "b", "a", reverseComparator));
    }

    @Test
    public void testMedianLongThreeValues() {
        assertEquals(2L, N.median(1L, 2L, 3L));
        assertEquals(2L, N.median(3L, 1L, 2L));
        assertEquals(2L, N.median(2L, 3L, 1L));
        assertEquals(5L, N.median(5L, 5L, 5L));
        assertEquals(3L, N.median(1L, 3L, 5L));
    }

    @Test
    public void testMedianDoubleThreeValues() {
        assertEquals(2.0, N.median(1.0, 2.0, 3.0), DELTA);
        assertEquals(2.0, N.median(3.0, 1.0, 2.0), DELTA);
        assertEquals(2.5, N.median(1.0, 2.5, 5.0), DELTA);
        assertEquals(3.0, N.median(3.0, 3.0, 3.0), DELTA);
    }

    @Test
    public void testMedianPrimitives() {
        assertEquals(2, N.median(1, 3, 2));
        assertEquals('b', N.median('c', 'a', 'b'));
        assertEquals(2.0f, N.median(1.0f, 3.0f, 2.0f), DELTA);

        assertEquals(3, N.lowerMedian(new int[] { 5, 1, 4, 2, 3 }));
        assertEquals(2, N.lowerMedian(new int[] { 1, 2, 3, 4 }));

        assertThrows(IllegalArgumentException.class, () -> N.lowerMedian(new int[] {}));
    }

    @Test
    public void testMedianGeneric() {
        assertEquals("banana", N.lowerMedian(new String[] { "apple", "cherry", "banana" }));
        assertEquals(Integer.valueOf(3), N.lowerMedian(Arrays.asList(5, 1, 4, 2, 3)));

        List<String> strList = Arrays.asList("zebra", "apple", "Banana");
        assertEquals("Banana", N.lowerMedian(strList, String.CASE_INSENSITIVE_ORDER));

        assertThrows(IllegalArgumentException.class, () -> N.lowerMedian(Collections.emptyList()));
    }

    @Test
    public void testMedianArrayEmpty() {
        assertThrows(IllegalArgumentException.class, () -> N.lowerMedian(new int[] {}));
    }

    @Test
    public void testMedianArrayNull() {
        assertThrows(IllegalArgumentException.class, () -> N.lowerMedian((int[]) null));
    }

    @Test
    public void testMedian_array_primitives() {
        assertThrows(IllegalArgumentException.class, () -> N.lowerMedian(new char[] {}));

        assertEquals('a', N.lowerMedian(new char[] { 'a' }));
        assertEquals('a', N.lowerMedian(new char[] { 'a', 'b' }));
        assertEquals('b', N.lowerMedian(new char[] { 'a', 'b', 'c' }));
        assertEquals('c', N.lowerMedian(charArray));
        assertEquals('c', N.lowerMedian(charArray, 1, 4));

        assertEquals((byte) 3, N.lowerMedian(byteArray));
        assertEquals((byte) 3, N.lowerMedian(byteArray, 1, 4));

        assertEquals((short) 3, N.lowerMedian(shortArray));
        assertEquals((short) 3, N.lowerMedian(shortArray, 1, 4));

        assertEquals(3, N.lowerMedian(intArray));
        assertEquals(3, N.lowerMedian(intArray, 1, 4));

        assertEquals(3L, N.lowerMedian(longArray));
        assertEquals(3L, N.lowerMedian(longArray, 1, 4));

        assertEquals(3.0f, N.lowerMedian(floatArray), 0.001);
        assertEquals(3.0f, N.lowerMedian(floatArray, 1, 4), 0.001);

        assertEquals(3.0, N.lowerMedian(doubleArray), 0.001);
        assertEquals(3.0, N.lowerMedian(doubleArray, 1, 4), 0.001);
    }

    @Test
    public void testMedian_statistical_array_primitives() {
        assertThrows(IllegalArgumentException.class, () -> N.median(new byte[] {}));
        assertThrows(IllegalArgumentException.class, () -> N.median(new short[] {}));
        assertThrows(IllegalArgumentException.class, () -> N.median(new int[] {}));
        assertThrows(IllegalArgumentException.class, () -> N.median(new long[] {}));
        assertThrows(IllegalArgumentException.class, () -> N.median(new float[] {}));
        assertThrows(IllegalArgumentException.class, () -> N.median(new double[] {}));

        assertThrows(IllegalArgumentException.class, () -> N.median((byte[]) null));
        assertThrows(IllegalArgumentException.class, () -> N.median((short[]) null));
        assertThrows(IllegalArgumentException.class, () -> N.median((int[]) null));
        assertThrows(IllegalArgumentException.class, () -> N.median((long[]) null));
        assertThrows(IllegalArgumentException.class, () -> N.median((float[]) null));
        assertThrows(IllegalArgumentException.class, () -> N.median((double[]) null));

        assertThrows(IllegalArgumentException.class, () -> N.median(new byte[] { 1, 2, 3 }, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> N.median(new int[] { 1, 2, 3 }, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> N.median(new long[] { 1L, 2L, 3L }, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> N.median(new double[] { 1.0, 2.0, 3.0 }, 1, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> N.median(new byte[] { 1, 2, 3 }, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> N.median(new int[] { 1, 2, 3 }, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> N.median(new long[] { 1L, 2L, 3L }, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> N.median(new double[] { 1.0, 2.0, 3.0 }, 0, 5));

        assertEquals(9.0d, N.median(new byte[] { 9 }), 0.0d);
        assertEquals(9.0d, N.median(new short[] { 9 }), 0.0d);
        assertEquals(9.0d, N.median(new int[] { 9 }), 0.0d);
        assertEquals(9.0d, N.median(new long[] { 9L }), 0.0d);
        assertEquals(9.0d, N.median(new float[] { 9.0f }), 0.0d);
        assertEquals(9.0d, N.median(new double[] { 9.0 }), 0.0d);

        assertEquals(1.5d, N.median((byte) 1, (byte) 2), 0.0d);
        assertEquals(1.5d, N.median((short) 1, (short) 2), 0.0d);
        assertEquals(1.5d, N.median(1, 2), 0.0d);
        assertEquals(1.5d, N.median(1L, 2L), 0.0d);
        assertEquals(1.5d, N.median(1.0f, 2.0f), 0.001d);
        assertEquals(1.5d, N.median(1.0, 2.0), 0.0d);

        assertEquals(15.0d, N.median(new byte[] { 5, 30, 15 }), 0.0d);
        assertEquals(150.0d, N.median(new short[] { 50, 300, 150 }), 0.0d);
        assertEquals(15.0d, N.median(new int[] { 5, 30, 15 }), 0.0d);
        assertEquals(150.0d, N.median(new long[] { 50L, 300L, 150L }), 0.0d);
        assertEquals(15.0d, N.median(new float[] { 5.0f, 30.0f, 15.0f }), 0.001d);
        assertEquals(15.0d, N.median(new double[] { 5.0, 30.0, 15.0 }), 0.0d);

        assertEquals(12.5d, N.median((byte) 10, (byte) 5, (byte) 20, (byte) 15), 0.0d);
        assertEquals(12.5d, N.median((short) 10, (short) 5, (short) 20, (short) 15), 0.0d);
        assertEquals(12.5d, N.median(10, 5, 20, 15), 0.0d);
        assertEquals(12.5d, N.median(10L, 5L, 20L, 15L), 0.0d);
        assertEquals(12.8d, N.median(10.5f, 5.2f, 20.8f, 15.1f), 0.001d);
        assertEquals(12.8d, N.median(10.5, 5.2, 20.8, 15.1), 0.001d);

        assertEquals(15.0d, N.median(new byte[] { 5, 30, 15, 8, 20 }), 0.0d);
        assertEquals(150.0d, N.median(new short[] { 50, 300, 150, 80, 200 }), 0.0d);
        assertEquals(15.0d, N.median(new int[] { 5, 30, 15, 8, 20 }), 0.0d);
        assertEquals(1500.0d, N.median(new long[] { 500L, 3000L, 1500L, 800L, 2000L }), 0.0d);
        assertEquals(15.0d, N.median(new float[] { 5.0f, 30.0f, 15.0f, 8.0f, 20.0f }), 0.001d);
        assertEquals(15.0d, N.median(new double[] { 5.0, 30.0, 15.0, 8.0, 20.0 }), 0.0d);

        assertEquals(15.0d, N.median(new byte[] { 5, 30, 15, 8, 20 }, 1, 4), 0.0d);
        assertEquals(150.0d, N.median(new short[] { 50, 300, 150, 80, 200 }, 1, 4), 0.0d);
        assertEquals(15.0d, N.median(new int[] { 5, 30, 15, 8, 20 }, 1, 4), 0.0d);
        assertEquals(1500.0d, N.median(new long[] { 500L, 3000L, 1500L, 800L, 2000L }, 1, 4), 0.0d);
        assertEquals(15.0d, N.median(new float[] { 5.0f, 30.0f, 15.0f, 8.0f, 20.0f }, 1, 4), 0.001d);
        assertEquals(15.0d, N.median(new double[] { 5.0, 30.0, 15.0, 8.0, 20.0 }, 1, 4), 0.0d);

        assertEquals(2.5d, N.median(new byte[] { 1, 2, 3, 4 }, 0, 4), 0.0d);
        assertEquals(2.5d, N.median(new short[] { 1, 2, 3, 4 }, 0, 4), 0.0d);
        assertEquals(2.5d, N.median(new int[] { 1, 2, 3, 4 }, 0, 4), 0.0d);
        assertEquals(2.5d, N.median(new long[] { 1L, 2L, 3L, 4L }, 0, 4), 0.0d);
        assertEquals(2.5d, N.median(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, 0, 4), 0.001d);
        assertEquals(2.5d, N.median(new double[] { 1.0, 2.0, 3.0, 4.0 }, 0, 4), 0.0d);

        assertEquals(2147483647.0d, N.median(Integer.MAX_VALUE, Integer.MAX_VALUE), 0.0d);
        assertEquals(Long.MAX_VALUE, N.median(Long.MAX_VALUE, Long.MAX_VALUE), 0.0d);

        // The two-element shortcut used to overflow before division, while the larger even-length
        // branch divided first and underflowed the least positive subnormal values to zero.
        assertEquals(Double.MAX_VALUE, N.median(Double.MAX_VALUE, Double.MAX_VALUE), 0.0d);
        assertEquals(-Double.MAX_VALUE, N.median(-Double.MAX_VALUE, -Double.MAX_VALUE), 0.0d);
        assertEquals(Double.MIN_VALUE, N.median(Double.MIN_VALUE, Double.MIN_VALUE), 0.0d);
        assertEquals(Double.MAX_VALUE, N.median(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE), 0.0d);
        assertEquals(Double.MIN_VALUE, N.median(Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE), 0.0d);

        int[] original = { 5, 30, 15, 8, 20 };
        int[] snapshot = original.clone();
        assertEquals(15.0d, N.median(original), 0.0d);
        assertArrayEquals(snapshot, original);

        double[] originalD = { 5.0, 30.0, 15.0, 8.0, 20.0 };
        double[] snapshotD = originalD.clone();
        assertEquals(15.0d, N.median(originalD, 0, 5), 0.0d);
        assertArrayEquals(snapshotD, originalD, 0.0d);
    }

    @Test
    public void testMedian_array_generic() {
        assertThrows(IllegalArgumentException.class, () -> N.lowerMedian(new String[] {}));

        String[] sorted = { "a", "b", "c", "d", "e" };
        assertEquals("c", N.lowerMedian(sorted));
        assertEquals("c", N.lowerMedian(sorted, 1, 4));
        assertEquals("c", N.lowerMedian(sorted, Comparator.naturalOrder()));
        assertEquals("c", N.lowerMedian(sorted, 0, 5, Comparator.naturalOrder()));

        assertEquals("bee", N.lowerMedian(Array.of("ant", "bee", "tiger"), Comparator.comparing(String::length)));
    }

    @Test
    public void testMedian_collection() {
        assertThrows(IllegalArgumentException.class, () -> N.lowerMedian(Collections.emptyList()));

        List<String> sorted = Arrays.asList("a", "b", "c", "d", "e");
        assertEquals("c", N.lowerMedian(sorted));
        assertEquals("c", N.lowerMedian(sorted, 1, 4));
        assertEquals("c", N.lowerMedian(sorted, Comparator.naturalOrder()));
        assertEquals("c", N.lowerMedian(sorted, 0, 5, Comparator.naturalOrder()));
        assertEquals("bee", N.lowerMedian(CommonUtil.toList("ant", "bee", "tiger"), Comparator.comparing(String::length)));
    }

    @Test
    public void testMedianThreeWithComparator_AllBranches_uncovered() {
        Comparator<Integer> c = Comparator.naturalOrder();
        // ab<=0, bc<=0 -> b
        assertEquals(Integer.valueOf(2), N.median(1, 2, 3, c));
        // ab<=0, bc>0, a<=c -> c ; here a=1,b=3,c=2 -> ab<=0, bc>0(3>2), a<=c(1<=2) -> c=2
        assertEquals(Integer.valueOf(2), N.median(1, 3, 2, c));
        // ab<=0, bc>0, a>c -> a ; a=2,b=3,c=1 -> ab<=0, bc>0, a>c(2>1) -> a=2
        assertEquals(Integer.valueOf(2), N.median(2, 3, 1, c));
        // ab>0, bc>=0 -> b ; a=3,b=2,c=1 -> ab>0, bc>=0 -> b=2
        assertEquals(Integer.valueOf(2), N.median(3, 2, 1, c));
        // ab>0, bc<0, a<=c -> a ; a=2,b=1,c=3 -> ab>0, bc<0, a<=c(2<=3) -> a=2
        assertEquals(Integer.valueOf(2), N.median(2, 1, 3, c));
        // ab>0, bc<0, a>c -> c ; a=3,b=1,c=2 -> ab>0, bc<0, a>c(3>2) -> c=2
        assertEquals(Integer.valueOf(2), N.median(3, 1, 2, c));
    }

    @Test
    public void testMedianCharRange_uncovered() {
        char[] a = { 'a', 'z', 'm', 'b', 'x' };
        assertEquals('m', N.lowerMedian(a, 1, 4));
        // 2-element range -> min
        assertEquals('b', N.lowerMedian(new char[] { 'm', 'b' }, 0, 2));
        // single element
        assertEquals('q', N.lowerMedian(new char[] { 'q' }, 0, 1));
    }

    @Test
    public void testMedianArrayRange_EmptyThrows_uncovered() {
        assertThrows(IllegalArgumentException.class, () -> N.lowerMedian(new char[0], 0, 0));
        assertThrows(IllegalArgumentException.class, () -> N.lowerMedian(new int[0], 0, 0));
        assertThrows(IllegalArgumentException.class, () -> N.lowerMedian(new long[0], 0, 0));
        assertThrows(IllegalArgumentException.class, () -> N.lowerMedian(new double[0], 0, 0));
    }

    @Test
    public void testMedianTwoElementsWithNaN_treatsNaNAsLargest() {
        // regression: the len == 2 shortcut used Math.min, returning NaN instead of treating NaN as the
        // largest value per the total ordering used by the rest of the median family
        assertEquals(1.0f, N.lowerMedian(new float[] { 1.0f, Float.NaN }), 0.0f);
        assertEquals(1.0f, N.lowerMedian(new float[] { Float.NaN, 1.0f }), 0.0f);
        assertEquals(1.0d, N.lowerMedian(new double[] { 1.0d, Double.NaN }), 0.0d);
        assertEquals(1.0d, N.lowerMedian(new double[] { Double.NaN, 1.0d }), 0.0d);
        // unchanged for normal values: lower of the two middle elements
        assertEquals(1.0f, N.lowerMedian(new float[] { 2.0f, 1.0f }), 0.0f);
        assertEquals(1.0d, N.lowerMedian(new double[] { 2.0d, 1.0d }), 0.0d);
    }

    @Test
    public void testMedianOddRange_matchesSortedMiddle() {
        // the odd-length branch used to select through kthLargest(); it now shares the even branch's
        // sorted copy. Both must agree with the middle element of the sorted range, for every overload.
        final Random rnd = new Random(20260908L);

        for (int len = 5; len <= 21; len += 2) {
            final int from = 1;
            final int to = from + len;

            final byte[] bytes = new byte[len + 2];
            final short[] shorts = new short[len + 2];
            final int[] ints = new int[len + 2];
            final long[] longs = new long[len + 2];
            final float[] floats = new float[len + 2];
            final double[] doubles = new double[len + 2];

            for (int i = 0; i < ints.length; i++) {
                final int v = rnd.nextInt(200) - 100;
                bytes[i] = (byte) v;
                shorts[i] = (short) v;
                ints[i] = v;
                longs[i] = v;
                floats[i] = v;
                doubles[i] = v;
            }

            final byte[] sortedBytes = Arrays.copyOfRange(bytes, from, to);
            Arrays.sort(sortedBytes);
            assertEquals(sortedBytes[len / 2], N.median(bytes, from, to), 0.0d);

            final short[] sortedShorts = Arrays.copyOfRange(shorts, from, to);
            Arrays.sort(sortedShorts);
            assertEquals(sortedShorts[len / 2], N.median(shorts, from, to), 0.0d);

            final int[] sortedInts = Arrays.copyOfRange(ints, from, to);
            Arrays.sort(sortedInts);
            assertEquals(sortedInts[len / 2], N.median(ints, from, to), 0.0d);

            final long[] sortedLongs = Arrays.copyOfRange(longs, from, to);
            Arrays.sort(sortedLongs);
            assertEquals(sortedLongs[len / 2], N.median(longs, from, to), 0.0d);

            final float[] sortedFloats = Arrays.copyOfRange(floats, from, to);
            Arrays.sort(sortedFloats);
            assertEquals(sortedFloats[len / 2], N.median(floats, from, to), 0.0d);

            final double[] sortedDoubles = Arrays.copyOfRange(doubles, from, to);
            Arrays.sort(sortedDoubles);
            assertEquals(sortedDoubles[len / 2], N.median(doubles, from, to), 0.0d);
        }

        // the range really is honoured: only [1, 6) participates below
        assertEquals(30.0d, N.median(new int[] { 1000, 10, 20, 30, 40, 50, -1000 }, 1, 6), 0.0d);
    }

    @Test
    public void testMedianOddRange_nanAndNegativeZeroFollowTotalOrder() {
        // NaN sorts last and -0.0 before 0.0, the same total order (Float/Double.compare) the
        // even-length branch already relied on.
        assertEquals(3.0d, N.median(new double[] { Double.NaN, 1.0d, 3.0d, Double.NaN, 2.0d }), 0.0d);
        assertEquals(3.0d, N.median(new float[] { Float.NaN, 1.0f, 3.0f, Float.NaN, 2.0f }), 0.0d);
        assertEquals(0, Double.compare(Double.NaN, N.median(new double[] { Double.NaN, 1.0d, Double.NaN, Double.NaN, 2.0d })));

        // the middle of [-0.0, -0.0, 0.0, 0.0, 1.0] is +0.0, not -0.0
        assertEquals(0, Double.compare(0.0d, N.median(new double[] { -0.0d, 0.0d, -0.0d, 0.0d, 1.0d })));
        assertEquals(0, Double.compare(0.0d, N.median(new float[] { -0.0f, 0.0f, -0.0f, 0.0f, 1.0f })));
    }

    @Test
    public void testMedian_nanFollowsTotalOrderAndIsNotPropagated() {
        // NaN sorts last, so it is not one of the middle values here and the result is not NaN - unlike max.
        assertEquals(2.5d, N.median(1.0f, 2.0f, 3.0f, Float.NaN), 0.0d);
        assertEquals(2.5d, N.median(1.0d, 2.0d, 3.0d, Double.NaN), 0.0d);
        org.junit.jupiter.api.Assertions.assertTrue(Float.isNaN(N.max(1.0f, 2.0f, 3.0f, Float.NaN)));

        // It is counted like any other element when the middle position is chosen, so a NaN that is not itself
        // a middle value still changes the answer.
        assertEquals(2.5d, N.median(1.0f, 2.0f, 3.0f, 4.0f), 0.0d);
        assertEquals(3.0d, N.median(1.0f, 2.0f, 3.0f, 4.0f, Float.NaN), 0.0d);
        assertEquals(2.5d, N.median(1.0d, 2.0d, 3.0d, 4.0d), 0.0d);
        assertEquals(3.0d, N.median(1.0d, 2.0d, 3.0d, 4.0d, Double.NaN), 0.0d);

        // The result is NaN only when a middle value is itself NaN.
        org.junit.jupiter.api.Assertions.assertTrue(Double.isNaN(N.median(1.0f, Float.NaN)));
        org.junit.jupiter.api.Assertions.assertTrue(Double.isNaN(N.median(1.0d, Double.NaN)));
        assertEquals(2.0d, N.median(new float[] { 1.0f, 2.0f, Float.NaN }, 0, 3), 0.0d);
        assertEquals(2.0d, N.median(new double[] { 1.0d, 2.0d, Double.NaN }, 0, 3), 0.0d);
    }
}
