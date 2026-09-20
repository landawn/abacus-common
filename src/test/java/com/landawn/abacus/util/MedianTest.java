package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

public class MedianTest extends TestBase {

    @Test
    public void testOf_CharArray() {
        Pair<Character, OptionalChar> odd = Median.of('a', 'c', 'b');
        assertEquals('b', odd.left());
        assertFalse(odd.right().isPresent());

        Pair<Character, OptionalChar> even = Median.of('a', 'b', 'c', 'd');
        assertEquals('b', even.left());
        assertEquals('c', even.right().get());

        Pair<Character, OptionalChar> two = Median.of('z', 'a');
        assertEquals('a', two.left());
        assertEquals('z', two.right().get());

        Pair<Character, OptionalChar> single = Median.of('x');
        assertEquals('x', single.left());
        assertFalse(single.right().isPresent());
    }

    @Test
    public void testOf_CharArray_Range() {
        final char[] arr = { 'a', 'z', 'b', 'y', 'c', 'x' };
        Pair<Character, OptionalChar> odd = Median.of(arr, 1, 4);
        assertEquals('y', odd.left());
        assertFalse(odd.right().isPresent());

        Pair<Character, OptionalChar> even = Median.of(arr, 2, 6);
        assertEquals('c', even.left());
        assertEquals('x', even.right().get());
    }

    @Test
    public void testOf_ByteArray() {
        Pair<Byte, OptionalByte> odd = Median.of((byte) 1, (byte) 3, (byte) 2);
        assertEquals((byte) 2, odd.left());
        assertFalse(odd.right().isPresent());

        Pair<Byte, OptionalByte> even = Median.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        assertEquals((byte) 2, even.left());
        assertEquals((byte) 3, even.right().get());

        Pair<Byte, OptionalByte> negative = Median.of((byte) -5, (byte) -1, (byte) -3);
        assertEquals((byte) -3, negative.left());
        assertFalse(negative.right().isPresent());
    }

    @Test
    public void testOf_ByteArray_Range() {
        final byte[] arr = { 10, 20, 30, 40, 50, 60 };
        Pair<Byte, OptionalByte> result = Median.of(arr, 1, 5);
        assertEquals((byte) 30, result.left());
        assertEquals((byte) 40, result.right().get());

        Pair<Byte, OptionalByte> descending = Median.of(new byte[] { 3, 1 }, 0, 2);
        assertEquals((byte) 1, descending.left());
        assertEquals((byte) 3, descending.right().get());
    }

    @Test
    public void testOf_ShortArray() {
        Pair<Short, OptionalShort> odd = Median.of((short) 100, (short) 300, (short) 200);
        assertEquals((short) 200, odd.left());
        assertFalse(odd.right().isPresent());

        Pair<Short, OptionalShort> even = Median.of((short) 10, (short) 20, (short) 30, (short) 40);
        assertEquals((short) 20, even.left());
        assertEquals((short) 30, even.right().get());
    }

    @Test
    public void testOf_ShortArray_Range() {
        Pair<Short, OptionalShort> result = Median.of(new short[] { 100, 200, 300, 400, 500 }, 0, 3);
        assertEquals((short) 200, result.left());
        assertFalse(result.right().isPresent());

        Pair<Short, OptionalShort> descending = Median.of(new short[] { 3, 1 }, 0, 2);
        assertEquals((short) 1, descending.left());
        assertEquals((short) 3, descending.right().get());
    }

    @Test
    public void testOf_IntArray() {
        Pair<Integer, OptionalInt> odd = Median.of(50, 10, 30);
        assertEquals(30, odd.left());
        assertFalse(odd.right().isPresent());

        Pair<Integer, OptionalInt> even = Median.of(10, 40, 20, 30);
        assertEquals(20, even.left());
        assertEquals(30, even.right().getAsInt());

        Pair<Integer, OptionalInt> unsorted = Median.of(5, 3, 1, 4, 2);
        assertEquals(3, unsorted.left());
        assertFalse(unsorted.right().isPresent());
    }

    @Test
    public void testOf_IntArray_Range() {
        final int[] arr = { 5, 1, 3, 9, 7, 2, 8 };
        Pair<Integer, OptionalInt> odd = Median.of(arr, 1, 4);
        assertEquals(3, odd.left());
        assertFalse(odd.right().isPresent());

        Pair<Integer, OptionalInt> even = Median.of(arr, 2, 6);
        assertEquals(3, even.left());
        assertEquals(7, even.right().getAsInt());
    }

    @Test
    public void testOf_LongArray() {
        Pair<Long, OptionalLong> odd = Median.of(1000L, 3000L, 2000L);
        assertEquals(2000L, odd.left());
        assertFalse(odd.right().isPresent());

        Pair<Long, OptionalLong> even = Median.of(1L, 2L, 3L, 4L);
        assertEquals(2L, even.left());
        assertEquals(3L, even.right().getAsLong());

        Pair<Long, OptionalLong> max = Median.of(Long.MAX_VALUE - 2, Long.MAX_VALUE - 1, Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE - 1, max.left());
        assertFalse(max.right().isPresent());
    }

    @Test
    public void testOf_LongArray_Range() {
        Pair<Long, OptionalLong> result = Median.of(new long[] { 10L, 20L, 30L, 40L, 50L }, 1, 4);
        assertEquals(30L, result.left());
        assertFalse(result.right().isPresent());

        Pair<Long, OptionalLong> descending = Median.of(new long[] { 3L, 1L }, 0, 2);
        assertEquals(1L, descending.left());
        assertEquals(3L, descending.right().get());
    }

    @Test
    public void testOf_FloatArray() {
        Pair<Float, OptionalFloat> odd = Median.of(1.5f, 3.5f, 2.5f);
        assertEquals(2.5f, odd.left());
        assertFalse(odd.right().isPresent());

        Pair<Float, OptionalFloat> even = Median.of(1.0f, 2.0f, 3.0f, 4.0f);
        assertEquals(2.0f, even.left());
        assertEquals(3.0f, even.right().get());

        Pair<Float, OptionalFloat> negative = Median.of(-5.5f, -1.5f, -3.5f);
        assertEquals(-3.5f, negative.left());
        assertFalse(negative.right().isPresent());
    }

    @Test
    public void testOf_FloatArray_Range() {
        Pair<Float, OptionalFloat> result = Median.of(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f }, 0, 2);
        assertEquals(1.1f, result.left());
        assertEquals(2.2f, result.right().get());

        Pair<Float, OptionalFloat> descending = Median.of(new float[] { 3f, 1f }, 0, 2);
        assertEquals(1f, descending.left());
        assertEquals(3f, descending.right().get());
    }

    @Test
    public void testOf_DoubleArray() {
        Pair<Double, OptionalDouble> odd = Median.of(1.5, 3.5, 2.5);
        assertEquals(2.5, odd.left());
        assertFalse(odd.right().isPresent());

        Pair<Double, OptionalDouble> even = Median.of(1.0, 2.0, 3.0, 4.0);
        assertEquals(2.0, even.left());
        assertEquals(3.0, even.right().getAsDouble());

        Pair<Double, OptionalDouble> inf = Median.of(1.0, Double.POSITIVE_INFINITY, 2.0);
        assertEquals(2.0, inf.left());
        assertFalse(inf.right().isPresent());
    }

    @Test
    public void testOf_DoubleArray_Range() {
        Pair<Double, OptionalDouble> result = Median.of(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 }, 1, 5);
        assertEquals(3.3, result.left());
        assertEquals(4.4, result.right().getAsDouble());

        Pair<Double, OptionalDouble> descending = Median.of(new double[] { 3d, 1d }, 0, 2);
        assertEquals(1d, descending.left());
        assertEquals(3d, descending.right().get());
    }

    @Test
    public void testOf_GenericArray() {
        Pair<Integer, Nullable<Integer>> ints = Median.of(new Integer[] { 5, 1, 3, 2, 4 });
        assertEquals(3, ints.left());
        assertFalse(ints.right().isPresent());

        Pair<String, Nullable<String>> strings = Median.of(new String[] { "apple", "banana", "cherry", "date" });
        assertEquals("banana", strings.left());
        assertEquals("cherry", strings.right().get());

        Pair<String, Nullable<String>> single = Median.of(new String[] { "test" });
        assertEquals("test", single.left());
        assertFalse(single.right().isPresent());
    }

    @Test
    public void testOf_GenericArray_RangeAndComparator() {
        Pair<Integer, Nullable<Integer>> range = Median.of(new Integer[] { 10, 20, 30, 40, 50, 60 }, 1, 4);
        assertEquals(30, range.left());
        assertFalse(range.right().isPresent());

        Pair<Integer, Nullable<Integer>> reversed = Median.of(new Integer[] { 5, 1, 3, 2, 4 }, Comparator.reverseOrder());
        assertEquals(3, reversed.left());
        assertFalse(reversed.right().isPresent());

        Pair<String, Nullable<String>> byLength = Median.of(new String[] { "aa", "b", "ccc", "dd" }, Comparator.comparingInt(String::length));
        assertEquals("aa", byLength.left());
        assertEquals("dd", byLength.right().get());

        Pair<String, Nullable<String>> ranged = Median.of(new String[] { "apple", "banana", "cherry", "date", "elderberry", "fig" }, 1, 5,
                Comparator.naturalOrder());
        assertEquals("cherry", ranged.left());
        assertEquals("date", ranged.right().get());

        Pair<Integer, Nullable<Integer>> descending = Median.of(new Integer[] { 3, 1 }, 0, 2, Comparator.naturalOrder());
        assertEquals(1, descending.left());
        assertEquals(3, descending.right().get());
    }

    @Test
    public void testOf_GenericArray_NullAwareComparator() {
        Pair<Integer, Nullable<Integer>> result = Median.of(new Integer[] { 3, null, 1, 2 }, Comparator.nullsFirst(Integer::compareTo));
        assertEquals(1, result.left());
        assertEquals(2, result.right().get());

        Comparator<Integer> comparator = Comparator.nullsFirst(Integer::compareTo);
        Pair<Integer, Nullable<Integer>> twoNulls = Median.of(new Integer[] { null, null }, comparator);
        assertNull(twoNulls.left());
        assertNull(twoNulls.right().get());

        Pair<Integer, Nullable<Integer>> sortedFallback = Median.of(new Integer[] { null, null, null, 1 }, comparator);
        assertNull(sortedFallback.left());
        assertNull(sortedFallback.right().get());
    }

    @Test
    public void testOf_Collection() {
        Pair<Integer, Nullable<Integer>> odd = Median.of(Arrays.asList(5, 1, 3, 2, 4));
        assertEquals(3, odd.left());
        assertFalse(odd.right().isPresent());

        Pair<Integer, Nullable<Integer>> even = Median.of(Arrays.asList(1, 2, 3, 4));
        assertEquals(2, even.left());
        assertEquals(3, even.right().get());
    }

    @Test
    public void testOf_Collection_RangeAndComparator() {
        Pair<Integer, Nullable<Integer>> range = Median.of(Arrays.asList(10, 20, 30, 40, 50), 1, 4);
        assertEquals(30, range.left());
        assertFalse(range.right().isPresent());

        Pair<Integer, Nullable<Integer>> reversed = Median.of(Arrays.asList(5, 1, 3, 2, 4), Comparator.reverseOrder());
        assertEquals(3, reversed.left());
        assertFalse(reversed.right().isPresent());

        Pair<String, Nullable<String>> ranged = Median.of(Arrays.asList("apple", "banana", "cherry", "date"), 0, 4, Comparator.naturalOrder());
        assertEquals("banana", ranged.left());
        assertEquals("cherry", ranged.right().get());
    }

    @Test
    public void testOf_Collection_NullAwareComparator() {
        Pair<Integer, Nullable<Integer>> result = Median.of(Arrays.asList(3, null, 1, 2), Comparator.nullsFirst(Integer::compareTo));
        assertEquals(1, result.left());
        assertEquals(2, result.right().get());

        Comparator<Integer> comparator = Comparator.nullsFirst(Integer::compareTo);
        Pair<Integer, Nullable<Integer>> twoNulls = Median.of(Arrays.asList(null, null), comparator);
        assertNull(twoNulls.left());
        assertNull(twoNulls.right().get());

        Pair<Integer, Nullable<Integer>> sortedFallback = Median.of(Arrays.asList(null, null, null, 1), comparator);
        assertNull(sortedFallback.left());
        assertNull(sortedFallback.right().get());
    }

    @Test
    public void testOf_NullHostileCollection() {
        Pair<Integer, Nullable<Integer>> listOf = Median.of(List.of(10, 5, 20, 15));
        assertEquals(10, listOf.left());
        assertEquals(15, listOf.right().get());

        final Set<Integer> treeSet = new TreeSet<>(Arrays.asList(40, 10, 30, 20, 50));
        Pair<Integer, Nullable<Integer>> tree = Median.of(treeSet);
        assertEquals(30, tree.left());
        assertFalse(tree.right().isPresent());

        Pair<Integer, Nullable<Integer>> sliced = Median.of(List.of(100, 50, 75, 25, 90), 0, 4);
        assertEquals(50, sliced.left());
        assertEquals(75, sliced.right().get());
    }

    @Test
    public void testOf_DuplicatesAndConsistency() {
        Pair<Integer, OptionalInt> allDupes = Median.of(5, 5, 5, 5, 5);
        assertEquals(5, allDupes.left());
        assertFalse(allDupes.right().isPresent());

        Pair<Integer, OptionalInt> mostlyDupes = Median.of(1, 2, 2, 2, 2, 2, 3);
        assertEquals(2, mostlyDupes.left());
        assertFalse(mostlyDupes.right().isPresent());

        assertEquals(3, Median.of(1, 2, 3, 4, 5).left().intValue());
        assertEquals(3L, Median.of(1L, 2L, 3L, 4L, 5L).left().longValue());
        assertEquals(3.0, Median.of(1.0, 2.0, 3.0, 4.0, 5.0).left().doubleValue());
    }

    @Test
    public void testOf_RejectsNullComparator() {
        assertThrows(IllegalArgumentException.class, () -> Median.of(new Integer[] { 3, 1, 2 }, null));
        assertThrows(IllegalArgumentException.class, () -> Median.of(new String[] { "a", "b" }, 0, 2, null));
        assertThrows(IllegalArgumentException.class, () -> Median.of(Arrays.asList("a", "b"), null));
        assertThrows(IllegalArgumentException.class, () -> Median.of(Arrays.asList("a", "b"), 0, 2, null));
    }

    @Test
    public void testOf_EmptyAndNullInputs() {
        assertThrows(IllegalArgumentException.class, () -> Median.of((char[]) null));
        assertThrows(IllegalArgumentException.class, () -> Median.of(new char[0]));
        assertThrows(IllegalArgumentException.class, () -> Median.of((byte[]) null));
        assertThrows(IllegalArgumentException.class, () -> Median.of(new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> Median.of((short[]) null));
        assertThrows(IllegalArgumentException.class, () -> Median.of(new short[0]));
        assertThrows(IllegalArgumentException.class, () -> Median.of((int[]) null));
        assertThrows(IllegalArgumentException.class, () -> Median.of(new int[0]));
        assertThrows(IllegalArgumentException.class, () -> Median.of((long[]) null));
        assertThrows(IllegalArgumentException.class, () -> Median.of(new long[0]));
        assertThrows(IllegalArgumentException.class, () -> Median.of((float[]) null));
        assertThrows(IllegalArgumentException.class, () -> Median.of(new float[0]));
        assertThrows(IllegalArgumentException.class, () -> Median.of((double[]) null));
        assertThrows(IllegalArgumentException.class, () -> Median.of(new double[0]));
        assertThrows(IllegalArgumentException.class, () -> Median.of((String[]) null));
        assertThrows(IllegalArgumentException.class, () -> Median.of(new String[0]));
        assertThrows(IllegalArgumentException.class, () -> Median.of((Collection<Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> Median.of(new ArrayList<Integer>()));
        assertThrows(IllegalArgumentException.class, () -> Median.of(new int[0], 0, 0));
        assertThrows(IllegalArgumentException.class, () -> Median.of((int[]) null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> Median.of(Arrays.asList(1, 2, 3), 1, 1));
        assertThrows(IllegalArgumentException.class, () -> Median.of(new char[] { 'a' }, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> Median.of(new byte[] { 10, 20, 30 }, 2, 2));
    }

    @Test
    public void testOf_RangeBoundsValidatedBeforeEmptiness() {
        final int[] a = { 1, 2, 3, 4, 5 };
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(a, 5, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> N.lowerMedian(a, 5, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(a, 10, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(a, 10, 20));
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(a, -1, 3));

        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(new char[] { 'a', 'b' }, 5, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(new byte[] { 1, 2 }, 5, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(new short[] { 1, 2 }, 5, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(new long[] { 1L, 2L }, 5, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(new float[] { 1f, 2f }, 5, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(new double[] { 1d, 2d }, 5, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(new String[] { "a", "b" }, 5, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(new String[] { "a", "b" }, 5, 3, Comparator.naturalOrder()));
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(Arrays.asList(1, 2), 5, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(Arrays.asList(1, 2), 5, 3, Comparator.<Integer> naturalOrder()));

        assertThrows(IndexOutOfBoundsException.class, () -> Median.of((Collection<String>) null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> Median.of((Collection<String>) null, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of((Collection<String>) null, 0, 3, Comparator.comparing(String::length)));
        assertThrows(IllegalArgumentException.class, () -> Median.of((Collection<String>) null, 0, 0, Comparator.comparing(String::length)));
    }

    @Test
    public void testOf_PrimitiveMatchesReferenceSortOverRandomRanges() {
        final java.util.Random rnd = new java.util.Random(20260831L);

        for (int it = 0; it < 4000; it++) {
            final int len = 1 + rnd.nextInt(12);
            final int from = rnd.nextInt(3);
            final int[] data = new int[from + len + rnd.nextInt(3)];
            for (int j = 0; j < data.length; j++) {
                data[j] = rnd.nextInt(8) - 4;
            }

            final Pair<Integer, OptionalInt> actual = Median.of(data, from, from + len);
            final int[] sorted = Arrays.copyOfRange(data, from, from + len);
            Arrays.sort(sorted);

            assertEquals(sorted[(len - 1) / 2], actual.left().intValue(), () -> Arrays.toString(data));
            if (len % 2 == 0) {
                assertTrue(actual.right().isPresent());
                assertEquals(sorted[len / 2], actual.right().get());
            } else {
                assertFalse(actual.right().isPresent());
            }
        }
    }

    @Test
    public void testOf_Double_NaNAndSignedZero() {
        Pair<Double, OptionalDouble> r = Median.of(new double[] { 0.0d, -0.0d });
        assertEquals(Double.doubleToRawLongBits(-0.0d), Double.doubleToRawLongBits(r.left()));
        assertEquals(Double.doubleToRawLongBits(0.0d), Double.doubleToRawLongBits(r.right().get()));

        r = Median.of(new double[] { -0.0d, 0.0d });
        assertEquals(Double.doubleToRawLongBits(-0.0d), Double.doubleToRawLongBits(r.left()));

        r = Median.of(new double[] { Double.NaN, 1d, 2d, 3d });
        assertEquals(2d, r.left());
        assertEquals(3d, r.right().get());

        r = Median.of(new double[] { 1d, 2d, Double.NaN, Double.NaN, 3d });
        assertEquals(3d, r.left());
        assertFalse(r.right().isPresent());

        r = Median.of(new double[] { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN, 0d });
        assertEquals(0d, r.left());
        assertEquals(Double.POSITIVE_INFINITY, r.right().get());
    }

    @Test
    public void testOf_Float_NaNAndSignedZero() {
        Pair<Float, OptionalFloat> r = Median.of(new float[] { 0.0f, -0.0f });
        assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits(r.left()));

        r = Median.of(new float[] { Float.NaN, 1f, 2f, 3f });
        assertEquals(2f, r.left());
        assertEquals(3f, r.right().get());
    }

    @Test
    public void testOf_DoesNotModifyInput() {
        final int[] ints = { 9, 3, 7, 1, 5, 8 };
        final int[] intsBefore = ints.clone();
        Median.of(ints);
        assertArrayEquals(intsBefore, ints);

        final double[] doubles = { 9d, 3d, 7d, 1d, 5d, 8d };
        final double[] doublesBefore = doubles.clone();
        Median.of(doubles, 1, 5);
        assertArrayEquals(doublesBefore, doubles);

        final String[] objects = { "d", "a", "c", "b", "e" };
        final String[] objectsBefore = objects.clone();
        Median.of(objects);
        assertArrayEquals(objectsBefore, objects);

        final List<String> list = new ArrayList<>(Arrays.asList("d", "a", "c", "b", "e"));
        final List<String> listBefore = new ArrayList<>(list);
        Median.of(list);
        assertEquals(listBefore, list);
    }

    @Test
    public void testOf_CollectionTraversedOnce() {
        final int[] iteratorCalls = { 0 };
        final List<Integer> backing = Arrays.asList(5, 3, 9, 1, 7, 2);
        final Collection<Integer> counting = new java.util.AbstractCollection<>() {
            @Override
            public java.util.Iterator<Integer> iterator() {
                iteratorCalls[0]++;
                return backing.iterator();
            }

            @Override
            public int size() {
                return backing.size();
            }
        };

        final Pair<Integer, Nullable<Integer>> result = Median.of(counting, Comparators.naturalOrder());
        assertEquals(1, iteratorCalls[0]);
        assertEquals(3, result.left());
        assertEquals(5, result.right().get());
    }

    @Test
    public void testOf_ObjectArray_NullElements() {
        for (int len = 1; len <= 6; len++) {
            final String[] source = new String[len];
            for (int i = 0; i < len; i++) {
                source[i] = i == len / 2 ? null : "v" + i;
            }

            final Pair<String, Nullable<String>> actual = Median.of(source);
            final String[] sorted = source.clone();
            Arrays.sort(sorted, Comparators.naturalOrder());

            assertEquals(sorted[(len - 1) / 2], actual.left());
            if (len % 2 == 0) {
                assertEquals(sorted[len / 2], actual.right().orElse(null));
            } else {
                assertFalse(actual.right().isPresent());
            }
        }
    }

    @Test
    public void testOf_ObjectArray_NullsMatchReferenceSort() {
        final java.util.Random rnd = new java.util.Random(20260831L);

        for (int it = 0; it < 2000; it++) {
            final int len = 1 + rnd.nextInt(10);
            final String[] source = new String[len];
            for (int i = 0; i < len; i++) {
                final int v = rnd.nextInt(5);
                source[i] = v == 0 ? null : "v" + v;
            }

            final Pair<String, Nullable<String>> actual = Median.of(source);
            final String[] sorted = source.clone();
            Arrays.sort(sorted, Comparators.naturalOrder());

            assertEquals(sorted[(len - 1) / 2], actual.left(), () -> Arrays.toString(source));
            assertEquals(len % 2 == 0 ? sorted[len / 2] : null, actual.right().orElse(null), () -> Arrays.toString(source));
        }
    }

    @Test
    public void testOf_NullLowerMedianReturnedAsNull() {
        final Pair<String, Nullable<String>> result = Median.of(new String[] { "a", null });
        assertNull(result.left());
        assertEquals("a", result.right().get());
    }

    @Test
    public void testOf_RangeIsValidatedBeforeTheComparator() {
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(new String[] { "a", "b" }, -1, 99, (Comparator<String>) null));
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(Arrays.asList("a", "b"), -1, 99, (Comparator<String>) null));

        // The range check also precedes the empty-input check.
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(new String[] { "a", "b" }, -1, 99, Comparator.<String> naturalOrder()));
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(Arrays.asList("a", "b"), -1, 99, Comparator.<String> naturalOrder()));
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of((String[]) null, -1, 99, Comparator.<String> naturalOrder()));

        // An in-range but empty range is the IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> Median.of(new String[] { "a", "b" }, 0, 0, Comparator.<String> naturalOrder()));
        assertThrows(IllegalArgumentException.class, () -> Median.of(Arrays.asList("a", "b"), 0, 0, Comparator.<String> naturalOrder()));

        // The comparator-less overloads really do check the range first, as their javadoc still says.
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(new String[] { "a", "b" }, -1, 99));
        assertThrows(IndexOutOfBoundsException.class, () -> Median.of(Arrays.asList("a", "b"), -1, 99));
    }

    @Test
    public void testOf_TwoArgComparatorOverloads_ValidateTheSourceFirst() {
        assertEquals("The specified array 'source' cannot be null or empty",
                assertThrows(IllegalArgumentException.class, () -> Median.of(new String[0], (Comparator<String>) null)).getMessage());
        assertEquals("The specified array 'source' cannot be null or empty",
                assertThrows(IllegalArgumentException.class, () -> Median.of((String[]) null, (Comparator<String>) null)).getMessage());
        // with a non-empty array the comparator check is still reached
        assertEquals("'cmp' cannot be null",
                assertThrows(IllegalArgumentException.class, () -> Median.of(new String[] { "a", "b" }, (Comparator<String>) null)).getMessage());

        assertEquals("Source collection is null or empty",
                assertThrows(IllegalArgumentException.class, () -> Median.of(Collections.<String> emptyList(), (Comparator<String>) null)).getMessage());
        assertEquals("Source collection is null or empty",
                assertThrows(IllegalArgumentException.class, () -> Median.of((Collection<String>) null, (Comparator<String>) null)).getMessage());
        assertEquals("'cmp' cannot be null",
                assertThrows(IllegalArgumentException.class, () -> Median.of(Arrays.asList("a", "b"), (Comparator<String>) null)).getMessage());
    }
}
