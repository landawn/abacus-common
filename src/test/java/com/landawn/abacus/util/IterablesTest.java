package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NavigableSet;
import java.util.RandomAccess;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;

public class IterablesTest extends IterablesTestSupport {
    @Test
    public void testNaturalMinMaxWithOnlyNaNAndNull() {
        final Double[] values = { null, Double.NaN, null };
        assertTrue(Iterables.min(values).get().isNaN());
        assertTrue(Iterables.min(Arrays.asList(values)).get().isNaN());
        assertTrue(Iterables.min(Arrays.asList(values).iterator()).get().isNaN());
        assertTrue(Iterables.minMax(values).get().left().isNaN());
        assertTrue(Iterables.minMax(values).get().right().isNaN());
        assertTrue(Iterables.minMax(Arrays.asList(values)).get().left().isNaN());
        assertTrue(Iterables.minMax(Arrays.asList(values).iterator()).get().left().isNaN());
        assertTrue(Iterables.minMax(new Float[] { Float.NaN, null }).get().left().isNaN());
        assertEquals(1.0, Iterables.minMax(new Double[] { null, Double.NaN, 1.0 }).get().left());
    }

    @Test
    public void testNullExtremeShortCircuitConsumesInitialComparison() {
        final Iterator<Integer> minimum = Arrays.asList((Integer) null, 1, 2).iterator();
        assertTrue(Iterables.min(minimum, Comparators.nullsFirst()).isNull());
        assertEquals(Integer.valueOf(2), minimum.next());

        final Iterator<Integer> maximum = Arrays.asList((Integer) null, 1, 2).iterator();
        assertTrue(Iterables.max(maximum, Comparators.nullsLast()).isNull());
        assertEquals(Integer.valueOf(2), maximum.next());
    }

    @Test
    public void testFillExtensionDoesNotRequireSet() {
        final List<String> values = new ArrayList<>(Arrays.asList("original"));
        final List<String> appendable = new java.util.AbstractList<>() {
            @Override
            public String get(final int index) {
                return values.get(index);
            }

            @Override
            public int size() {
                return values.size();
            }

            @Override
            public boolean add(final String value) {
                return values.add(value);
            }
        };
        Iterables.fill(appendable, 2, 4, () -> "new");
        assertEquals(Arrays.asList("original", null, "new", "new"), values);
        assertThrows(UnsupportedOperationException.class, () -> Iterables.fill(appendable, 0, 1, () -> "replacement"));
    }

    @Test
    public void testNullInputHandling() {
        assertFalse(Iterables.min((int[]) null).isPresent());
        assertFalse(Iterables.max((String[]) null).isPresent());
        assertFalse(Iterables.lowerMedian((Integer[]) null).isPresent());
        assertFalse(Iterables.sumInt((Iterable<Integer>) null).isPresent());
        assertFalse(Iterables.averageDouble((Double[]) null).isPresent());
    }

    @Test
    public void testEmptyInputHandling() {
        assertFalse(Iterables.min(new int[0]).isPresent());
        assertFalse(Iterables.max(new String[0]).isPresent());
        assertFalse(Iterables.lowerMedian(Collections.emptyList()).isPresent());
        assertFalse(Iterables.sumLong(Collections.emptyList()).isPresent());
        assertFalse(Iterables.averageInt(new Integer[0]).isPresent());
    }

    @Test
    public void testSingleElementCollections() {
        assertEquals(5, Iterables.min(new int[] { 5 }).get());
        assertEquals("test", Iterables.max(new String[] { "test" }).get());
        assertEquals(Integer.valueOf(42), Iterables.lowerMedian(Arrays.asList(42)).get());
        assertEquals(10, Iterables.sumInt(Arrays.asList(10)).get());
        assertEquals(7.5, Iterables.averageDouble(new Double[] { 7.5 }).get());
    }

    @Test
    public void testLargeCollections() {
        List<Integer> largeList = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            largeList.add(i);
        }

        assertEquals(Integer.valueOf(1), Iterables.min(largeList).get());
        assertEquals(Integer.valueOf(1000), Iterables.max(largeList).get());
        assertEquals(500500, Iterables.sumInt(largeList).get());
        assertEquals(500.5, Iterables.averageInt(largeList).get());
    }

    @Test
    public void testWithNullElements() {
        List<String> listWithNulls = Arrays.asList("a", null, "b", null, "c");

        Nullable<String> minResult = Iterables.min(listWithNulls);
        assertTrue(minResult.isPresent());
        assertEquals("a", minResult.get());

        Nullable<String> maxResult = Iterables.max(listWithNulls);
        assertTrue(maxResult.isPresent());
        assertEquals("c", maxResult.get());
    }

    @Test
    public void testEmptyCollectionHandling() {
        List<Integer> empty = new ArrayList<>();

        assertTrue(Iterables.min(empty).isEmpty());
        assertTrue(Iterables.max(empty).isEmpty());
        assertFalse(Iterables.sumInt(empty).isPresent());
        assertFalse(Iterables.averageInt(empty).isPresent());
        assertTrue(Iterables.lowerMedian(empty).isEmpty());
    }

    @Test
    public void testLargeDatasetOperations() {
        List<Integer> largeList = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            largeList.add(i);
        }

        Nullable<Integer> min = Iterables.min(largeList);
        assertTrue(min.isPresent());
        assertEquals(Integer.valueOf(0), min.get());

        Nullable<Integer> max = Iterables.max(largeList);
        assertTrue(max.isPresent());
        assertEquals(Integer.valueOf(9999), max.get());

        OptionalDouble avg = Iterables.averageInt(largeList);
        assertTrue(avg.isPresent());
        assertEquals(4999.5, avg.getAsDouble(), 0.001);
    }

    @Test
    public void testNullHandlingInComparators() {
        Integer[] arrWithNulls = { 3, null, 1, 4, null, 2 };

        Nullable<Integer> min = Iterables.min(arrWithNulls);
        assertTrue(min.isPresent());
        assertEquals(Integer.valueOf(1), min.get());

        Nullable<Integer> max = Iterables.max(arrWithNulls);
        assertTrue(max.isPresent());
        assertEquals(Integer.valueOf(4), max.get());
    }

    @Test
    public void testMedianArray() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Nullable<Integer> result = Iterables.lowerMedian(arr);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(3), result.get());

        assertTrue(Iterables.lowerMedian(new Integer[0]).isEmpty());
        assertTrue(Iterables.lowerMedian((Integer[]) null).isEmpty());
    }

    @Test
    public void testMedianArrayWithComparator() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Comparator<Integer> naturalOrder = Comparator.naturalOrder();
        Nullable<Integer> result = Iterables.lowerMedian(arr, naturalOrder);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(3), result.get());
    }

    @Test
    public void testMedianCollection() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        Nullable<Integer> result = Iterables.lowerMedian(list);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(3), result.get());

        assertTrue(Iterables.lowerMedian((Collection<Integer>) null).isEmpty());
        assertTrue(Iterables.lowerMedian(new ArrayList<Integer>()).isEmpty());
    }

    @Test
    public void testMedianCollectionWithComparator() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        Comparator<Integer> naturalOrder = Comparator.naturalOrder();
        Nullable<Integer> result = Iterables.lowerMedian(list, naturalOrder);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(3), result.get());
    }

    @Test
    public void testMedianArrayComparable() {
        assertTrue(Iterables.lowerMedian((String[]) null).isEmpty());
        assertTrue(Iterables.lowerMedian(new String[0]).isEmpty());
        assertEquals("b", Iterables.lowerMedian(new String[] { "a", "b", "c" }).get());
        assertEquals("b", Iterables.lowerMedian(new String[] { "c", "b", "a" }).get());
        assertEquals("a", Iterables.lowerMedian(new String[] { "a", "b" }).get());
        assertEquals("b", Iterables.lowerMedian(new String[] { "a", "b", "c", "d" }).get());
    }

    @Test
    public void testMedianArrayComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.lowerMedian((String[]) null, reverseOrder).isEmpty());
        assertTrue(Iterables.lowerMedian(new String[0], reverseOrder).isEmpty());
        assertEquals("b", Iterables.lowerMedian(new String[] { "a", "b", "c" }, reverseOrder).get());
        assertEquals("b", Iterables.lowerMedian(new String[] { "a", "b" }, reverseOrder).get());
    }

    @Test
    public void testMedianCollectionComparable() {
        assertTrue(Iterables.lowerMedian((Collection<String>) null).isEmpty());
        assertTrue(Iterables.lowerMedian(list()).isEmpty());
        assertEquals("b", Iterables.lowerMedian(list("a", "b", "c")).get());
    }

    @Test
    public void testMedianCollectionComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.lowerMedian((Collection<String>) null, reverseOrder).isEmpty());
        assertTrue(Iterables.lowerMedian(list(), reverseOrder).isEmpty());
        assertEquals("b", Iterables.lowerMedian(list("a", "b", "c"), reverseOrder).get());
    }

    @Test
    public void testMedianWithComparator() {
        List<String> list = Arrays.asList("a", "bb", "ccc");
        Comparator<String> lengthComparator = Comparator.comparing(String::length);

        Nullable<String> result = Iterables.lowerMedian(list, lengthComparator);
        assertTrue(result.isPresent());
        assertEquals("bb", result.get());
    }

    @Test
    public void testMedian() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Nullable<Integer> result = Iterables.lowerMedian(arr);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(3), result.get());

        assertTrue(Iterables.lowerMedian(new Integer[0]).isEmpty());
    }

    @Test
    public void testMedian_EmptyCollection() {
        assertFalse(Iterables.lowerMedian(Collections.<Integer> emptyList()).isPresent());
    }

    @Test
    public void testMedian_SingleElement() {
        Nullable<Integer> result = Iterables.lowerMedian(new Integer[] { 42 });
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(42), result.get());
    }

    @Test
    public void testMedian_TwoElements() {
        Nullable<Integer> result = Iterables.lowerMedian(new Integer[] { 1, 2 });
        assertTrue(result.isPresent());
        // median of [1,2] is element at index (2-1)/2 = 0, which is the smaller element
        assertEquals(Integer.valueOf(1), result.get());
    }

    @Test
    public void testKthLargestArray() {
        Integer[] arr = { 3, 1, 4, 1, 5, 9 };
        Nullable<Integer> result = Iterables.kthLargest(arr, 2);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(5), result.get());

        assertTrue(Iterables.kthLargest(arr, 10).isEmpty());
        assertTrue(Iterables.kthLargest(new Integer[0], 1).isEmpty());
        assertTrue(Iterables.kthLargest((Integer[]) null, 1).isEmpty());
    }

    @Test
    public void testKthLargestArrayWithComparator() {
        Integer[] arr = { 3, 1, 4, 1, 5, 9 };
        Comparator<Integer> naturalOrder = Comparator.naturalOrder();
        Nullable<Integer> result = Iterables.kthLargest(arr, 2, naturalOrder);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(5), result.get());

        assertTrue(Iterables.kthLargest(arr, 10, naturalOrder).isEmpty());
    }

    @Test
    public void testKthLargestCollection() {
        List<Integer> list = Arrays.asList(3, 1, 4, 1, 5, 9);
        Nullable<Integer> result = Iterables.kthLargest(list, 2);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(5), result.get());

        assertTrue(Iterables.kthLargest(list, 10).isEmpty());
        assertTrue(Iterables.kthLargest((Collection<Integer>) null, 1).isEmpty());
    }

    @Test
    public void testKthLargestCollectionWithComparator() {
        List<Integer> list = Arrays.asList(3, 1, 4, 1, 5, 9);
        Comparator<Integer> naturalOrder = Comparator.naturalOrder();
        Nullable<Integer> result = Iterables.kthLargest(list, 2, naturalOrder);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(5), result.get());
    }

    @Test
    public void testKthLargestArrayComparable() {
        assertTrue(Iterables.kthLargest((String[]) null, 1).isEmpty());
        assertTrue(Iterables.kthLargest(new String[0], 1).isEmpty());
        assertTrue(Iterables.kthLargest(new String[] { "a", "b" }, 3).isEmpty());

        assertEquals("c", Iterables.kthLargest(new String[] { "a", "b", "c" }, 1).get());
        assertEquals("b", Iterables.kthLargest(new String[] { "a", "b", "c" }, 2).get());
        assertEquals("a", Iterables.kthLargest(new String[] { "a", "b", "c" }, 3).get());
    }

    @Test
    public void testKthLargestArrayComparator() {
        Comparator<String> reverseOrder = String.CASE_INSENSITIVE_ORDER.reversed();
        assertTrue(Iterables.kthLargest((String[]) null, 1, reverseOrder).isEmpty());
        String[] arr = { "c", "A", "b" };
        assertEquals("A", Iterables.kthLargest(arr, 1, reverseOrder).get());
        assertEquals("b", Iterables.kthLargest(arr, 2, reverseOrder).get());
        assertEquals("c", Iterables.kthLargest(arr, 3, reverseOrder).get());
    }

    @Test
    public void testKthLargestCollectionComparable() {
        assertTrue(Iterables.kthLargest((Collection<String>) null, 1).isEmpty());
        assertTrue(Iterables.kthLargest(list(), 1).isEmpty());
        List<String> l = list("a", "b", "c");
        assertEquals("c", Iterables.kthLargest(l, 1).get());
        assertEquals("b", Iterables.kthLargest(l, 2).get());
    }

    @Test
    public void testKthLargestCollectionComparator() {
        Comparator<String> reverseOrder = String.CASE_INSENSITIVE_ORDER.reversed();
        assertTrue(Iterables.kthLargest((Collection<String>) null, 1, reverseOrder).isEmpty());
        List<String> l = list("c", "A", "b");
        assertEquals("A", Iterables.kthLargest(l, 1, reverseOrder).get());
        assertEquals("b", Iterables.kthLargest(l, 2, reverseOrder).get());
    }

    @Test
    public void testKthLargest() {
        Integer[] arr = { 3, 1, 4, 1, 5, 9 };
        Nullable<Integer> result = Iterables.kthLargest(arr, 2);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(5), result.get());

        assertTrue(Iterables.kthLargest(arr, 10).isEmpty());
        assertTrue(Iterables.kthLargest(new Integer[0], 1).isEmpty());
    }

    @Test
    public void testMedian_EmptyArray() {
        assertFalse(Iterables.lowerMedian(new Integer[0]).isPresent());
    }

    @Test
    public void testKthLargest_EmptyArray() {
        assertFalse(Iterables.kthLargest(new Integer[0], 1).isPresent());
    }

    @Test
    public void testKthLargest_EmptyCollection() {
        assertFalse(Iterables.kthLargest(Collections.<Integer> emptyList(), 1).isPresent());
    }

    @Test
    public void testKthLargest_KEqualsSize() {
        Nullable<Integer> result = Iterables.kthLargest(new Integer[] { 3, 1, 2 }, 3);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get());
    }

    @Test
    public void testKthLargest_KGreaterThanSize() {
        Nullable<Integer> result = Iterables.kthLargest(new Integer[] { 3, 1, 2 }, 4);
        assertFalse(result.isPresent());
    }

    @Test
    public void testKthLargest_WithComparator() {
        List<Integer> list = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
        Nullable<Integer> result = Iterables.<Integer> kthLargest(list, 3, Comparator.naturalOrder());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(5), result.get());
    }

    @Test
    public void testKthLargest_EmptyList_WithComparator() {
        assertTrue(Iterables.<Integer> kthLargest(new ArrayList<Integer>(), 1, Comparator.naturalOrder()).isEmpty());
    }

    @Test
    public void testNumberTypeConversions() {
        List<Number> mixedNumbers = Arrays.asList((byte) 1, (short) 2, 3, 4L, 5.0f, 6.0);

        OptionalInt sumInt = Iterables.sumInt(mixedNumbers);
        assertTrue(sumInt.isPresent());
        assertEquals(21, sumInt.getAsInt());

        OptionalLong sumLong = Iterables.sumLong(mixedNumbers);
        assertTrue(sumLong.isPresent());
        assertEquals(21L, sumLong.getAsLong());

        OptionalDouble sumDouble = Iterables.sumDouble(mixedNumbers);
        assertTrue(sumDouble.isPresent());
        assertEquals(21.0, sumDouble.getAsDouble(), 0.001);
    }

    @Test
    public void testRangeValidation() {
        Integer[] array = { 1, 2, 3, 4, 5 };

        assertTrue(Iterables.averageInt(array, 1, 4).isPresent());

        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(array, -1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(array, 2, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(array, 3, 2));
    }

    @Test
    public void testIndexOfArray_NotFound() {
        OptionalInt result = Iterables.indexOf(new Object[] { "a", "b", "c" }, "z");
        assertFalse(result.isPresent());
    }

    @Test
    public void testIndexOfCollection_NotFound() {
        OptionalInt result = Iterables.indexOf(Arrays.asList("a", "b", "c"), "z");
        assertFalse(result.isPresent());
    }

    @Test
    public void testIndexOfArray() {
        Integer[] arr = { 1, 2, 3, 2, 4 };
        OptionalInt result = Iterables.indexOf(arr, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        assertFalse(Iterables.indexOf(arr, 5).isPresent());
        assertFalse(Iterables.indexOf((Object[]) null, 1).isPresent());
        assertFalse(Iterables.indexOf(new Integer[0], 1).isPresent());
    }

    @Test
    public void testIndexOfCollection() {
        List<Integer> list = Arrays.asList(1, 2, 3, 2, 4);
        OptionalInt result = Iterables.indexOf(list, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        assertFalse(Iterables.indexOf(list, 5).isPresent());
        assertFalse(Iterables.indexOf((Collection<?>) null, 1).isPresent());
    }

    @Test
    public void testIndexOf() {
        Object[] array = { "a", "b", "c", "b" };
        OptionalInt result = Iterables.indexOf(array, "b");
        assertTrue(result.isPresent());
        assertEquals(1, result.get());

        OptionalInt notFound = Iterables.indexOf(array, "d");
        assertFalse(notFound.isPresent());

        assertFalse(Iterables.indexOf((Object[]) null, "b").isPresent());
    }

    @Test
    public void testIndexOf_NullCollection() {
        OptionalInt result = Iterables.indexOf((Collection<?>) null, "x");
        assertFalse(result.isPresent());
    }

    @Test
    public void testIndexOf_FindNull() {
        OptionalInt result = Iterables.indexOf(new Object[] { "a", null, "b" }, null);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());
    }

    @Test
    public void testIndexOfArray_Dedicated() {
        OptionalInt result = Iterables.indexOf(new Object[] { "a", "b", "c" }, "b");
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testIndexOfCollection_Dedicated() {
        OptionalInt result = Iterables.indexOf(Arrays.asList("a", "b", "c"), "b");
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testIndexOf_NullArray() {
        OptionalInt result = Iterables.indexOf((Object[]) null, "x");
        assertFalse(result.isPresent());
    }

    @Test
    public void testCopyLists() {
        List<String> src = Arrays.asList("a", "b", "c");
        List<String> dest = new ArrayList<>(Arrays.asList("x", "y", "z", "w"));

        Iterables.copyInto(src, dest);

        assertEquals("a", dest.get(0));
        assertEquals("b", dest.get(1));
        assertEquals("c", dest.get(2));
        assertEquals("w", dest.get(3));
    }

    @Test
    public void testCopyListList() {
        List<String> src = list("a", "b");
        List<String> dest = new ArrayList<>(Arrays.asList("x", "y", "z"));
        Iterables.copyInto(src, dest);
        assertEquals(list("a", "b", "z"), dest);

        List<String> emptySrc = list();
        List<String> dest2 = new ArrayList<>(Arrays.asList("x", "y"));
        Iterables.copyInto(emptySrc, dest2);
        assertEquals(list("x", "y"), dest2);

        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.copyInto(list("a", "b", "c"), list("x", "y")));
    }

    @Test
    public void testCopyListsWithPositions() {
        List<String> src = Arrays.asList("a", "b", "c", "d", "e");
        List<String> dest = new ArrayList<>(Arrays.asList("1", "2", "3", "4", "5", "6"));

        Iterables.copyInto(src, 1, dest, 2, 3);

        assertEquals("1", dest.get(0));
        assertEquals("2", dest.get(1));
        assertEquals("b", dest.get(2));
        assertEquals("c", dest.get(3));
        assertEquals("d", dest.get(4));
        assertEquals("6", dest.get(5));
    }

    @Test
    public void testCopyInto_Dedicated() {
        List<Integer> src = Arrays.asList(1, 2, 3);
        List<Integer> dest = new ArrayList<>(Arrays.asList(10, 20, 30));
        Iterables.copyInto(src, dest);
        assertEquals(Arrays.asList(1, 2, 3), dest);
    }

    @Test
    public void testCopyIntoOverlappingRangesUseSourceSnapshot() {
        final List<Integer> randomAccess = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        Iterables.copyInto(randomAccess, 0, randomAccess, 1, 3);
        assertEquals(Arrays.asList(1, 1, 2, 3), randomAccess);

        final List<Integer> sequential = new LinkedList<>(Arrays.asList(1, 2, 3, 4));
        Iterables.copyInto(sequential, 0, sequential, 1, 3);
        assertEquals(Arrays.asList(1, 1, 2, 3), sequential);
    }

    @Test
    public void testCopyRange_Dedicated() {
        List<Integer> src = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> dest = new ArrayList<>(Arrays.asList(10, 20, 30, 40, 50));
        Iterables.copyInto(src, 1, dest, 2, 2);
        assertEquals(Integer.valueOf(2), dest.get(2));
        assertEquals(Integer.valueOf(3), dest.get(3));
    }

    @Test
    public void testCopyRange_RandomAccess() {
        List<Integer> src = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        List<Integer> dest = new ArrayList<>(Arrays.asList(0, 0, 0, 0, 0));
        Iterables.copyInto(src, 1, dest, 2, 3);
        assertEquals(Integer.valueOf(2), dest.get(2));
        assertEquals(Integer.valueOf(3), dest.get(3));
        assertEquals(Integer.valueOf(4), dest.get(4));
    }

    @Test
    public void testCopyRange_NonRandomAccess() {
        java.util.LinkedList<Integer> src = new java.util.LinkedList<>(Arrays.asList(10, 20, 30, 40));
        java.util.LinkedList<Integer> dest = new java.util.LinkedList<>(Arrays.asList(0, 0, 0, 0));
        Iterables.copyInto(src, 1, dest, 1, 2);
        assertEquals(Integer.valueOf(20), dest.get(1));
        assertEquals(Integer.valueOf(30), dest.get(2));
    }

    @Test
    public void testCopyRange_ZeroLength() {
        List<String> src = list("a", "b", "c");
        List<String> dest = list("x", "y", "z");

        Iterables.copyInto(src, 1, dest, 1, 0);
        assertEquals(list("x", "y", "z"), dest);
    }

    @Test
    public void testCopyListRange() {
        List<String> src = list("s1", "s2", "s3", "s4");
        List<String> dest = new ArrayList<>(Arrays.asList("d1", "d2", "d3", "d4", "d5"));

        Iterables.copyInto(src, 1, dest, 2, 2);
        assertEquals(list("d1", "d2", "s2", "s3", "d5"), dest);

        List<String> destNonRandom = new LinkedList<>(Arrays.asList("d1", "d2", "d3", "d4", "d5"));
        Iterables.copyInto(src, 1, destNonRandom, 2, 2);
        assertEquals(list("d1", "d2", "s2", "s3", "d5"), destNonRandom);

        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.copyInto(src, 0, dest, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.copyInto(src, 3, dest, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.copyInto(src, 0, dest, 4, 2));
    }

    @Test
    public void testCopyRange_EmptySrcZeroLength() {
        assertDoesNotThrow(() -> {
            List<Integer> src = new ArrayList<>();
            List<Integer> dest = new ArrayList<>();
            // Should not throw
            Iterables.copyInto(src, 0, dest, 0, 0);
        });
    }

    @Test
    public void test_reverse() {

        {
            final List<Integer> list = CommonUtil.toList(1, 2, 3, 4, 5);
            final List<Integer> reversed = Iterables.asReversed(list);
            assertEquals(CommonUtil.toList(5, 4, 3, 2, 1), reversed);

            reversed.add(6);
            assertEquals(CommonUtil.toList(6, 1, 2, 3, 4, 5), list);

            list.remove(1);
            assertEquals(CommonUtil.toList(5, 4, 3, 2, 6), reversed);
        }

        {
            final List<Integer> list = CommonUtil.toList(1, 2, 3, 4, 5);
            final List<Integer> reversed = Lists.reverse(list);
            assertEquals(CommonUtil.toList(5, 4, 3, 2, 1), reversed);

            reversed.add(6);
            assertEquals(CommonUtil.toList(6, 1, 2, 3, 4, 5), list);

            list.remove(1);
            assertEquals(CommonUtil.toList(5, 4, 3, 2, 6), reversed);
        }

        {
            final List<Integer> list = CommonUtil.toList(1, 2, 3, 4, 5);
            final List<Integer> reversed = CommonUtil.toReversedList(list);
            assertEquals(CommonUtil.toList(5, 4, 3, 2, 1), reversed);
        }

        {
            final Collection<Integer> c = CommonUtil.toLinkedHashSet(1, 2, 3, 4, 5);
            CommonUtil.reverse(c);
            assertEquals(CommonUtil.toLinkedHashSet(5, 4, 3, 2, 1), c);
        }
    }

    @Test
    public void testReverse() {
        List<Integer> original = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> reversed = Iterables.asReversed(original);

        assertEquals(5, reversed.size());
        assertEquals(Integer.valueOf(5), reversed.get(0));
        assertEquals(Integer.valueOf(4), reversed.get(1));
        assertEquals(Integer.valueOf(3), reversed.get(2));
        assertEquals(Integer.valueOf(2), reversed.get(3));
        assertEquals(Integer.valueOf(1), reversed.get(4));

        List<Integer> mutableOriginal = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        List<Integer> mutableReversed = Iterables.asReversed(mutableOriginal);
        mutableOriginal.set(0, 10);
        assertEquals(Integer.valueOf(10), mutableReversed.get(4));
    }

    @Test
    public void testReverseListIterator() {
        List<String> original = list("a", "b", "c", "d");
        List<String> reversed = Iterables.asReversed(original);
        ListIterator<String> iter = reversed.listIterator();

        assertTrue(iter.hasNext());
        assertEquals("d", iter.next());
        assertEquals(0, iter.previousIndex());
        assertEquals(1, iter.nextIndex());

        assertTrue(iter.hasNext());
        assertEquals("c", iter.next());

        assertTrue(iter.hasPrevious());
        assertEquals("c", iter.previous());

        iter.set("x");
        assertEquals(list("d", "x", "b", "a"), reversed);
        assertEquals(list("a", "b", "x", "d"), original);

        iter.add("y");
        assertEquals(list("a", "b", "x", "y", "d"), original);
        assertEquals(list("d", "y", "x", "b", "a"), reversed);

    }

    @Test
    public void testReverseRandomAccessList() {
        List<String> list = Arrays.asList("a", "b", "c");
        List<String> reversed = Iterables.asReversed(list);

        assertEquals(Arrays.asList("c", "b", "a"), reversed);
        assertTrue(reversed instanceof RandomAccess);
    }

    @Test
    public void testReverseDoubleReverse() {
        List<String> list = Arrays.asList("a", "b", "c");
        List<String> doubleReversed = Iterables.asReversed(Iterables.asReversed(list));

        assertEquals(list, doubleReversed);
    }

    @Test
    public void testAsReversed_Dedicated() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        List<Integer> reversed = Iterables.asReversed(list);
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), reversed);
    }

    @Test
    public void testAsReversed_Remove() {
        List<Integer> original = list(1, 2, 3, 4, 5);
        List<Integer> reversed = Iterables.asReversed(original);

        // remove by index from reversed (index 0 = last element of original)
        Integer removed = reversed.remove(0);
        assertEquals(Integer.valueOf(5), removed);
        assertEquals(list(1, 2, 3, 4), original);
        assertEquals(list(4, 3, 2, 1), reversed);

        // remove from middle
        removed = reversed.remove(1);
        assertEquals(Integer.valueOf(3), removed);
        assertEquals(list(1, 2, 4), original);
        assertEquals(list(4, 2, 1), reversed);
    }

    @Test
    public void testAsReversed_SubList() {
        List<Integer> original = list(1, 2, 3, 4, 5);
        List<Integer> reversed = Iterables.asReversed(original);

        // subList of reversed [1, 3) => indices 1..2 of reversed = elements at positions 3, 2 of original = [4, 3]
        List<Integer> sub = reversed.subList(1, 3);
        assertEquals(list(4, 3), sub);

        // subList should be a view
        sub.set(0, 40);
        assertEquals(Integer.valueOf(40), original.get(3));
    }

    @Test
    public void testAsReversed_LinkedList() {
        LinkedList<String> original = new LinkedList<>(Arrays.asList("a", "b", "c", "d"));
        List<String> reversed = Iterables.asReversed(original);

        // LinkedList is not RandomAccess, so it should use ReverseList
        assertFalse(reversed instanceof RandomAccess);
        assertEquals(list("d", "c", "b", "a"), reversed);

        reversed.set(0, "z");
        assertEquals("z", original.get(3));
    }

    @Test
    public void testAsReversed_ListIteratorRemove() {
        List<String> original = list("a", "b", "c", "d");
        List<String> reversed = Iterables.asReversed(original);
        ListIterator<String> iter = reversed.listIterator();

        iter.next(); // "d"
        iter.next(); // "c"
        iter.remove(); // remove "c"

        assertEquals(list("d", "b", "a"), reversed);
        assertEquals(list("a", "b", "d"), original);
    }

    @Test
    public void testAsReversed_DoubleReverseLinkedList() {
        LinkedList<Integer> original = new LinkedList<>(Arrays.asList(1, 2, 3));
        List<Integer> doubleReversed = Iterables.asReversed(Iterables.asReversed(original));

        // double reverse should return the original forward list
        assertEquals(list(1, 2, 3), doubleReversed);
    }

    @Test
    public void testAsReversed_ImmutableList() {
        ImmutableList<Integer> immutable = ImmutableList.of(1, 2, 3);
        List<Integer> reversed = Iterables.asReversed(immutable);
        assertEquals(3, reversed.size());
        assertEquals(Integer.valueOf(3), reversed.get(0));
        assertEquals(Integer.valueOf(1), reversed.get(2));
    }

    @Test
    public void testAsReversed_AlreadyReversed() {
        List<Integer> original = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<Integer> reversed = Iterables.asReversed(original);
        // Double-reversing should return original
        List<Integer> doubleReversed = Iterables.asReversed(reversed);
        assertEquals(original, doubleReversed);
    }

    @Test
    public void testAsReversed_NonRandomAccess() {
        // LinkedList is not RandomAccess
        java.util.LinkedList<Integer> linked = new java.util.LinkedList<>(Arrays.asList(1, 2, 3));
        List<Integer> reversed = Iterables.asReversed(linked);
        assertEquals(3, reversed.size());
        assertEquals(Integer.valueOf(3), reversed.get(0));
        assertEquals(Integer.valueOf(1), reversed.get(2));
    }

    @Test
    public void testReverseEmptyList() {
        List<String> emptyList = new ArrayList<>();
        List<String> reversed = Iterables.asReversed(emptyList);

        assertTrue(reversed.isEmpty());
    }

    @Test
    public void testAsReversed_Empty() {
        List<Integer> reversed = Iterables.asReversed(new ArrayList<>());
        assertTrue(reversed.isEmpty());
    }

    @Test
    public void testAsReversed_SingleElement() {
        List<Integer> reversed = Iterables.asReversed(new ArrayList<>(Arrays.asList(1)));
        assertEquals(Arrays.asList(1), reversed);
    }

    @Test
    public void testAsReversed_Clear() {
        List<Integer> original = list(1, 2, 3);
        List<Integer> reversed = Iterables.asReversed(original);

        reversed.clear();
        assertTrue(original.isEmpty());
        assertTrue(reversed.isEmpty());
    }

    @Test
    public void testAsReversed_ListIteratorFromIndex() {
        List<String> original = list("a", "b", "c", "d");
        List<String> reversed = Iterables.asReversed(original);
        ListIterator<String> iter = reversed.listIterator(2);

        assertTrue(iter.hasNext());
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testReverseList() {
        List<Integer> original = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        List<Integer> reversed = Iterables.asReversed(original);

        assertEquals(Arrays.asList(4, 3, 2, 1), reversed);
        assertEquals(4, reversed.size());
        assertEquals(Integer.valueOf(4), reversed.get(0));
        assertEquals(Integer.valueOf(1), reversed.get(3));

        reversed.set(0, 10);
        assertEquals(Arrays.asList(1, 2, 3, 10), original);
        assertEquals(Arrays.asList(10, 3, 2, 1), reversed);

        reversed.add(0, 20);
        assertEquals(Arrays.asList(1, 2, 3, 10, 20), original);
        assertEquals(Arrays.asList(20, 10, 3, 2, 1), reversed);

        original.add(0, 0);
        assertEquals(Arrays.asList(20, 10, 3, 2, 1, 0), reversed);

        List<Integer> single = new ArrayList<>(Collections.singletonList(1));
        assertEquals(Collections.singletonList(1), Iterables.asReversed(single));

        List<Integer> empty = new ArrayList<>();
        assertEquals(Collections.emptyList(), Iterables.asReversed(empty));

        List<Integer> randomAccessOriginal = Arrays.asList(5, 6, 7);
        List<Integer> randomAccessReversed = Iterables.asReversed(randomAccessOriginal);
        assertEquals(Arrays.asList(7, 6, 5), randomAccessReversed);
        assertThrows(UnsupportedOperationException.class, () -> randomAccessReversed.add(33));
        randomAccessReversed.set(0, 77);
        assertEquals(Arrays.asList(5, 6, 77), randomAccessOriginal);
    }

    @Test
    public void testUnion() {
        Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> set2 = new HashSet<>(Arrays.asList(3, 4, 5));

        Iterables.SetView<Integer> union = Iterables.union(set1, set2);

        assertEquals(5, union.size());
        assertTrue(union.contains(1));
        assertTrue(union.contains(2));
        assertTrue(union.contains(3));
        assertTrue(union.contains(4));
        assertTrue(union.contains(5));

        Set<Integer> target = new HashSet<>();
        union.copyInto(target);
        assertEquals(5, target.size());
    }

    @Test
    public void testUnionCopyInto() {
        Set<String> set1 = new HashSet<>(Arrays.asList("a", "b"));
        Set<String> set2 = new HashSet<>(Arrays.asList("c", "d"));

        Iterables.SetView<String> union = Iterables.union(set1, set2);
        Set<String> result = union.copyInto(new HashSet<>());

        assertEquals(4, result.size());
        assertTrue(result.containsAll(Arrays.asList("a", "b", "c", "d")));

        final Set<String> populated = new HashSet<>(Arrays.asList("existing"));
        assertEquals(populated, union.copyInto(populated));
        assertEquals(5, populated.size());
        assertTrue(populated.contains("existing"));
    }

    @Test
    public void testUnionWithEmptySets() {
        Set<String> emptySet = Collections.emptySet();
        Set<String> nonEmptySet = new HashSet<>(Arrays.asList("a", "b"));

        Iterables.SetView<String> union1 = Iterables.union(emptySet, nonEmptySet);
        assertEquals(2, union1.size());

        Iterables.SetView<String> union2 = Iterables.union(nonEmptySet, emptySet);
        assertEquals(2, union2.size());

        Iterables.SetView<String> union3 = Iterables.union(emptySet, emptySet);
        assertTrue(union3.isEmpty());
    }

    @Test
    public void testUnion_ImmutableView() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(3, 4));
        Iterables.SetView<Integer> union = Iterables.union(s1, s2);

        assertThrows(UnsupportedOperationException.class, () -> union.add(5));
    }

    @Test
    public void testIntersection() {
        Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3, 4));
        Set<Integer> set2 = new HashSet<>(Arrays.asList(3, 4, 5, 6));

        Iterables.SetView<Integer> intersection = Iterables.intersection(set1, set2);

        assertEquals(2, intersection.size());
        assertTrue(intersection.contains(3));
        assertTrue(intersection.contains(4));
        assertFalse(intersection.contains(1));
        assertFalse(intersection.contains(5));
    }

    @Test
    public void testIntersection_Dedicated() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(2, 3, 4));
        Set<Integer> result = Iterables.intersection(s1, s2).copyInto(new HashSet<>());
        assertEquals(new HashSet<>(Arrays.asList(2, 3)), result);
    }

    @Test
    public void testIntersectionWithEmptySets() {
        Set<String> emptySet = Collections.emptySet();
        Set<String> nonEmptySet = new HashSet<>(Arrays.asList("a", "b"));

        Iterables.SetView<String> intersection1 = Iterables.intersection(nonEmptySet, emptySet);
        assertTrue(intersection1.isEmpty());

        Iterables.SetView<String> intersection2 = Iterables.intersection(emptySet, nonEmptySet);
        assertTrue(intersection2.isEmpty());
    }

    @Test
    public void testIntersectionDisjoint() {
        Set<String> set1 = new HashSet<>(Arrays.asList("a", "b"));
        Set<String> set2 = new HashSet<>(Arrays.asList("c", "d"));

        Iterables.SetView<String> intersection = Iterables.intersection(set1, set2);

        assertTrue(intersection.isEmpty());
    }

    @Test
    public void testIntersection_ImmutableView() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(2, 3, 4));
        Iterables.SetView<Integer> inter = Iterables.intersection(s1, s2);

        assertThrows(UnsupportedOperationException.class, () -> inter.add(5));
        assertEquals(2, inter.size());
    }

    @Test
    public void testDifference() {
        Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3, 4));
        Set<Integer> set2 = new HashSet<>(Arrays.asList(3, 4, 5, 6));

        Iterables.SetView<Integer> difference = Iterables.difference(set1, set2);

        assertEquals(2, difference.size());
        assertTrue(difference.contains(1));
        assertTrue(difference.contains(2));
        assertFalse(difference.contains(3));
        assertFalse(difference.contains(4));
    }

    @Test
    public void testDifference_Dedicated() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(2, 3, 4));
        Set<Integer> result = Iterables.difference(s1, s2).copyInto(new HashSet<>());
        assertEquals(new HashSet<>(Arrays.asList(1)), result);
    }

    @Test
    public void testDifferenceWithEmptySets() {
        Set<String> emptySet = Collections.emptySet();
        Set<String> nonEmptySet = new HashSet<>(Arrays.asList("a", "b"));

        Iterables.SetView<String> difference1 = Iterables.difference(emptySet, nonEmptySet);
        assertTrue(difference1.isEmpty());

        Iterables.SetView<String> difference2 = Iterables.difference(nonEmptySet, emptySet);
        assertEquals(2, difference2.size());
    }

    @Test
    public void testDifference_ImmutableView() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(2, 3, 4));
        Iterables.SetView<Integer> diff = Iterables.difference(s1, s2);

        assertThrows(UnsupportedOperationException.class, () -> diff.add(5));
        assertTrue(diff.contains(1));
    }

    @Test
    public void testSymmetricDifference() {
        Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3, 4));
        Set<Integer> set2 = new HashSet<>(Arrays.asList(3, 4, 5, 6));

        Iterables.SetView<Integer> symDiff = Iterables.symmetricDifference(set1, set2);

        assertEquals(4, symDiff.size());
        assertTrue(symDiff.contains(1));
        assertTrue(symDiff.contains(2));
        assertTrue(symDiff.contains(5));
        assertTrue(symDiff.contains(6));
        assertFalse(symDiff.contains(3));
        assertFalse(symDiff.contains(4));
    }

    @Test
    public void testUnion_Dedicated() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(3, 4, 5));
        Set<Integer> result = Iterables.union(s1, s2).copyInto(new HashSet<>());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3, 4, 5)), result);
    }

    @Test
    public void testSymmetricDifference_Dedicated() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(2, 3, 4));
        Set<Integer> result = Iterables.symmetricDifference(s1, s2).copyInto(new HashSet<>());
        assertEquals(new HashSet<>(Arrays.asList(1, 4)), result);
    }

    @Test
    public void testSymmetricDifferenceWithEmptySets() {
        Set<String> emptySet = Collections.emptySet();
        Set<String> nonEmptySet = new HashSet<>(Arrays.asList("a", "b"));

        Iterables.SetView<String> symDiff1 = Iterables.symmetricDifference(emptySet, nonEmptySet);
        assertEquals(2, symDiff1.size());

        Iterables.SetView<String> symDiff2 = Iterables.symmetricDifference(nonEmptySet, emptySet);
        assertEquals(2, symDiff2.size());

        Iterables.SetView<String> symDiff3 = Iterables.symmetricDifference(emptySet, emptySet);
        assertTrue(symDiff3.isEmpty());
    }

    @Test
    public void testSymmetricDifferenceIdenticalSets() {
        Set<String> set1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> set2 = new HashSet<>(Arrays.asList("a", "b", "c"));

        Iterables.SetView<String> symDiff = Iterables.symmetricDifference(set1, set2);

        assertTrue(symDiff.isEmpty());
    }

    @Test
    public void testSetViewsRemainLiveWhenBackingSetsStartEmpty() {
        final Set<Integer> unionLeft = new HashSet<>();
        final Set<Integer> unionRight = new HashSet<>();
        final Iterables.SetView<Integer> union = Iterables.union(unionLeft, unionRight);
        unionLeft.add(1);
        unionRight.add(2);
        assertEquals(CommonUtil.asSet(1, 2), union.copyInto(new HashSet<>()));

        final Set<Integer> intersectionLeft = new HashSet<>();
        final Set<Integer> intersectionRight = new HashSet<>();
        final Iterables.SetView<Integer> intersection = Iterables.intersection(intersectionLeft, intersectionRight);
        intersectionLeft.add(3);
        intersectionRight.add(3);
        assertEquals(CommonUtil.asSet(3), intersection.copyInto(new HashSet<>()));

        final Set<Integer> differenceLeft = new HashSet<>();
        final Set<Integer> differenceRight = new HashSet<>();
        final Iterables.SetView<Integer> difference = Iterables.difference(differenceLeft, differenceRight);
        differenceLeft.add(4);
        assertTrue(difference.contains(4));
        differenceRight.add(4);
        assertFalse(difference.contains(4));

        final Set<Integer> symmetricLeft = new HashSet<>();
        final Set<Integer> symmetricRight = new HashSet<>();
        final Iterables.SetView<Integer> symmetricDifference = Iterables.symmetricDifference(symmetricLeft, symmetricRight);
        symmetricLeft.add(5);
        assertTrue(symmetricDifference.contains(5));
        symmetricRight.add(5);
        assertFalse(symmetricDifference.contains(5));
    }

    @Test
    public void testSubSet() {
        NavigableSet<Integer> set = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        Range<Integer> range = Range.closedOpen(3, 8);

        NavigableSet<Integer> subset = Iterables.subSet(set, range);

        assertEquals(5, subset.size());
        assertTrue(subset.contains(3));
        assertTrue(subset.contains(7));
        assertFalse(subset.contains(8));
        assertFalse(subset.contains(2));
    }

    @Test
    public void testSubSetOpenRange() {
        NavigableSet<Integer> set = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5));
        Range<Integer> range = Range.open(2, 5);

        NavigableSet<Integer> subset = Iterables.subSet(set, range);

        assertEquals(2, subset.size());
        assertTrue(subset.contains(3));
        assertTrue(subset.contains(4));
        assertFalse(subset.contains(2));
        assertFalse(subset.contains(5));
    }

    @Test
    public void testSubSet_Dedicated() {
        NavigableSet<Integer> set = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        NavigableSet<Integer> result = Iterables.subSet(set, Range.closed(3, 7));
        assertEquals(new TreeSet<>(Arrays.asList(3, 4, 5, 6, 7)), result);
    }

    @Test
    public void testSubSet_WithRange() {
        NavigableSet<Integer> set = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        Range<Integer> range = Range.closed(3, 7);
        NavigableSet<Integer> subset = Iterables.subSet(set, range);
        assertTrue(subset.contains(3));
        assertTrue(subset.contains(7));
        assertFalse(subset.contains(2));
        assertFalse(subset.contains(8));
    }

    @Test
    public void testSubSet_EmptySet() {
        NavigableSet<Integer> emptySet = new TreeSet<>();
        Range<Integer> range = Range.closed(1, 5);
        NavigableSet<Integer> subset = Iterables.subSet(emptySet, range);
        assertTrue(subset.isEmpty());

        emptySet.add(3);
        assertTrue(subset.contains(3));
    }

    @Test
    public void testSubSetNavigableSet() {
        NavigableSet<Integer> set = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));

        assertTrue(Iterables.subSet(CommonUtil.emptyNavigableSet(), Range.closed(1, 5)).isEmpty());

        Range<Integer> r1 = Range.closedOpen(3, 7);
        NavigableSet<Integer> sub1 = Iterables.subSet(set, r1);
        assertEquals(new TreeSet<>(Arrays.asList(3, 4, 5, 6)), sub1);

        Range<Integer> r2 = Range.closed(3, 7);
        NavigableSet<Integer> sub2 = Iterables.subSet(set, r2);
        assertEquals(new TreeSet<>(Arrays.asList(3, 4, 5, 6, 7)), sub2);

        Range<Integer> r3 = Range.openClosed(3, 7);
        NavigableSet<Integer> sub3 = Iterables.subSet(set, r3);
        assertEquals(new TreeSet<>(Arrays.asList(4, 5, 6, 7)), sub3);

        Range<Integer> r4 = Range.open(3, 7);
        NavigableSet<Integer> sub4 = Iterables.subSet(set, r4);
        assertEquals(new TreeSet<>(Arrays.asList(4, 5, 6)), sub4);

        NavigableSet<Integer> customSet = new TreeSet<>(Comparator.reverseOrder());
        customSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        Range<Integer> naturalRange = Range.closed(2, 4);
        assertThrows(IllegalArgumentException.class, () -> Iterables.subSet(customSet, naturalRange));

        Range<Integer> reverseRange = Range.closed(Integer.MIN_VALUE, 2);
    }

    @Test
    public void testSubSet_NullRangeRejectedEvenForNullSet() {
        assertThrows(IllegalArgumentException.class, () -> Iterables.subSet((NavigableSet<Integer>) null, null));
    }

    @Test
    public void testPowerSet() {
        Set<Integer> input = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Set<Integer>> powerSet = Iterables.powerSet(input);

        assertEquals(8, powerSet.size());

        assertTrue(powerSet.contains(new HashSet<>()));
        assertTrue(powerSet.contains(new HashSet<>(Arrays.asList(1))));
        assertTrue(powerSet.contains(new HashSet<>(Arrays.asList(2))));
        assertTrue(powerSet.contains(new HashSet<>(Arrays.asList(3))));
        assertTrue(powerSet.contains(new HashSet<>(Arrays.asList(1, 2))));
        assertTrue(powerSet.contains(new HashSet<>(Arrays.asList(1, 3))));
        assertTrue(powerSet.contains(new HashSet<>(Arrays.asList(2, 3))));
        assertTrue(powerSet.contains(new HashSet<>(Arrays.asList(1, 2, 3))));
    }

    @Test
    public void testPowerSetThreeElements() {
        Set<String> input = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<Set<String>> powerSet = Iterables.powerSet(input);

        assertEquals(8, powerSet.size());
    }

    @Test
    public void testPowerSetContains() {
        Set<String> input = new HashSet<>(Arrays.asList("a", "b"));
        Set<Set<String>> powerSet = Iterables.powerSet(input);

        assertTrue(powerSet.contains(new HashSet<>(Arrays.asList("a"))));
        assertFalse(powerSet.contains(new HashSet<>(Arrays.asList("c"))));
        assertFalse(powerSet.contains("not a set"));
    }

    @Test
    public void testPowerSetIteration() {
        Set<String> input = new HashSet<>(Arrays.asList("a", "b"));
        Set<Set<String>> powerSet = Iterables.powerSet(input);

        List<Set<String>> allSubsets = new ArrayList<>();
        for (Set<String> subset : powerSet) {
            allSubsets.add(new HashSet<>(subset));
        }

        assertEquals(4, allSubsets.size());
    }

    @Test
    public void testPowerSet_Dedicated() {
        Set<Set<Integer>> result = Iterables.powerSet(new HashSet<>(Arrays.asList(1, 2)));
        assertEquals(4, result.size());
    }

    @Test
    public void testPowerSet_ContainsCheck() {
        Set<Set<Integer>> result = Iterables.powerSet(new HashSet<>(Arrays.asList(1, 2)));
        assertTrue(result.contains(new HashSet<>(Arrays.asList(1))));
        assertTrue(result.contains(new HashSet<>(Arrays.asList(1, 2))));
        assertFalse(result.contains(new HashSet<>(Arrays.asList(3))));
        assertFalse(result.contains("not a set"));
    }

    @Test
    public void testPowerSet_SingleElement() {
        Set<Set<String>> result = Iterables.powerSet(new HashSet<>(Arrays.asList("a")));
        assertEquals(2, result.size());
        assertTrue(result.contains(new HashSet<>()));
        assertTrue(result.contains(new HashSet<>(Arrays.asList("a"))));
    }

    @Test
    public void testPowerSetTooLarge() {
        Set<Integer> input = new HashSet<>();
        for (int i = 0; i < 31; i++) {
            input.add(i);
        }

        assertThrows(IllegalArgumentException.class, () -> Iterables.powerSet(input));
    }

    @Test
    public void testRollup() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<List<String>> rollup = Iterables.rollup(input);

        assertEquals(4, rollup.size());
        assertEquals(Arrays.asList(), rollup.get(0));
        assertEquals(Arrays.asList("a"), rollup.get(1));
        assertEquals(Arrays.asList("a", "b"), rollup.get(2));
        assertEquals(Arrays.asList("a", "b", "c"), rollup.get(3));
    }

    @Test
    public void testRollup_Dedicated() {
        List<List<Integer>> result = Iterables.rollup(Arrays.asList(1, 2, 3));
        assertEquals(4, result.size());
        assertEquals(Arrays.asList(), result.get(0));
        assertEquals(Arrays.asList(1), result.get(1));
        assertEquals(Arrays.asList(1, 2), result.get(2));
        assertEquals(Arrays.asList(1, 2, 3), result.get(3));
    }

    @Test
    public void testRollupNull() {
        List<List<String>> rollup = Iterables.rollup(null);

        assertEquals(1, rollup.size());
        assertEquals(Collections.emptyList(), rollup.get(0));
    }

    @Test
    public void testRollup_SingleElement() {
        List<List<String>> result = Iterables.rollup(Arrays.asList("a"));
        assertEquals(2, result.size());
        assertTrue(result.get(0).isEmpty());
        assertEquals(list("a"), result.get(1));
    }

    @Test
    public void testRollup_NullCollection() {
        List<List<String>> result = Iterables.rollup(null);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isEmpty());
    }

    @Test
    public void testPermutations() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        Collection<List<Integer>> perms = Iterables.permutations(input);

        assertEquals(6, perms.size());

        Set<List<Integer>> uniquePerms = new HashSet<>(perms);
        assertEquals(6, uniquePerms.size());
    }

    @Test
    public void testPermutationsWithDuplicates() {
        List<Integer> input = Arrays.asList(1, 1, 2);
        Collection<List<Integer>> perms = Iterables.permutations(input);

        assertEquals(6, perms.size());
    }

    @Test
    public void testPermutationsContains() {
        List<String> input = Arrays.asList("a", "b");
        Collection<List<String>> perms = Iterables.permutations(input);

        assertTrue(perms.contains(Arrays.asList("a", "b")));
        assertTrue(perms.contains(Arrays.asList("b", "a")));
        assertFalse(perms.contains(Arrays.asList("a", "b", "c")));
        assertFalse(perms.contains(Arrays.asList("c", "d")));
    }

    @Test
    public void testPermutations_Dedicated() {
        Collection<List<Integer>> result = Iterables.permutations(Arrays.asList(1, 2, 3));
        assertEquals(6, result.size());
    }

    @Test
    public void testPermutations_ContainsCheck() {
        Collection<List<Integer>> perms = Iterables.permutations(Arrays.asList(1, 2));
        assertTrue(perms.contains(list(1, 2)));
        assertTrue(perms.contains(list(2, 1)));
        assertFalse(perms.contains(list(1, 3)));
        assertFalse(perms.contains(new LinkedHashSet<>(Arrays.asList(1, 2))));
        assertFalse(perms.contains("not a list"));
    }

    @Test
    public void testPermutations_SingleElement() {
        Collection<List<String>> perms = Iterables.permutations(Arrays.asList("a"));
        assertEquals(1, perms.size());
        assertEquals(list("a"), perms.iterator().next());
    }

    @Test
    public void testOrderedPermutations() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        Collection<List<Integer>> perms = Iterables.orderedPermutations(input);

        List<List<Integer>> permList = new ArrayList<>(perms);
        assertEquals(6, permList.size());

        assertEquals(Arrays.asList(1, 2, 3), permList.get(0));
        assertEquals(Arrays.asList(3, 2, 1), permList.get(permList.size() - 1));
    }

    @Test
    public void testOrderedPermutationsWithComparator() {
        List<String> input = Arrays.asList("a", "b", "c");
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        Collection<List<String>> perms = Iterables.orderedPermutations(input, reverseOrder);

        List<List<String>> permList = new ArrayList<>(perms);
        assertEquals(6, permList.size());

        assertEquals(Arrays.asList("c", "b", "a"), permList.get(0));
    }

    @Test
    public void testOrderedPermutationsWithDuplicates() {
        List<Integer> input = Arrays.asList(1, 1, 2);
        Collection<List<Integer>> perms = Iterables.orderedPermutations(input);

        Set<List<Integer>> uniquePerms = new HashSet<>(perms);
        assertEquals(3, uniquePerms.size());
    }

    @Test
    public void testOrderedPermutations_Dedicated() {
        Collection<List<Integer>> result = Iterables.orderedPermutations(Arrays.asList(1, 2, 3));
        assertEquals(6, result.size());
    }

    @Test
    public void testOrderedPermutationsWithComparator_Dedicated() {
        Collection<List<Integer>> result = Iterables.orderedPermutations(Arrays.asList(1, 2, 3), Comparator.reverseOrder());
        assertEquals(6, result.size());
    }

    @Test
    public void testOrderedPermutations_ContainsCheck() {
        Collection<List<Integer>> perms = Iterables.orderedPermutations(Arrays.asList(1, 2));
        assertTrue(perms.contains(list(1, 2)));
        assertTrue(perms.contains(list(2, 1)));
        assertFalse(perms.contains(list(1, 3)));
        assertFalse(perms.contains(new LinkedHashSet<>(Arrays.asList(1, 2))));
        assertFalse(perms.contains("not a list"));
    }

    @Test
    public void testOrderedPermutations_RejectsNullComparatorForEveryInputSize() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Iterables.orderedPermutations(Collections.<Integer> emptyList(), null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterables.orderedPermutations(Collections.singletonList(1), null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterables.orderedPermutations(Arrays.asList(2, 1, 3), null));
    }

    @Test
    public void testOrderedPermutationsComparator() {
        Collection<String> elements = list("c", "A", "b");
        Comparator<String> ciComparator = String.CASE_INSENSITIVE_ORDER;
        Collection<List<String>> op = Iterables.orderedPermutations(elements, ciComparator);
        assertEquals(6, op.size());
        Iterator<List<String>> it = op.iterator();
        assertEquals(list("A", "b", "c"), it.next());
    }

    @Test
    public void testOrderedPermutationsEmpty() {
        List<String> emptyList = Collections.emptyList();
        Collection<List<String>> perms = Iterables.orderedPermutations(emptyList);

        assertFalse(perms.isEmpty());
        assertEquals(1, perms.size());
        assertEquals(Collections.emptyList(), perms.iterator().next());
    }

    @Test
    public void testOrderedPermutationsComparable() {
        Collection<Integer> elements = list(2, 1);
        Collection<List<Integer>> op = Iterables.orderedPermutations(elements);
        assertEquals(2, op.size());
        Iterator<List<Integer>> it = op.iterator();
        assertEquals(list(1, 2), it.next());
        assertEquals(list(2, 1), it.next());

        Collection<Integer> elementsDup = list(1, 2, 1);
        Collection<List<Integer>> opDup = Iterables.orderedPermutations(elementsDup);
        assertEquals(3, opDup.size());
        Iterator<List<Integer>> itDup = opDup.iterator();
        assertEquals(list(1, 1, 2), itDup.next());
        assertEquals(list(1, 2, 1), itDup.next());
        assertEquals(list(2, 1, 1), itDup.next());

        // null is now treated as an empty collection (yields the single empty permutation)
        Collection<List<Integer>> opNull = Iterables.orderedPermutations((Collection<Integer>) null);
        assertEquals(1, opNull.size());
        assertTrue(opNull.iterator().next().isEmpty());
    }

    @Test
    public void testPowerSet_Empty() {
        Set<Set<Integer>> ps = Iterables.powerSet(Collections.<Integer> emptySet());
        assertEquals(1, ps.size());
        assertTrue(ps.contains(Collections.<Integer> emptySet()));
    }

    @Test
    public void testPowerSet_NullInputTreatedAsEmpty() {
        Set<Set<Integer>> ps = Iterables.powerSet(null);
        assertEquals(1, ps.size());
    }

    @Test
    public void testPowerSet_Three() {
        Set<Integer> in = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Set<Integer>> ps = Iterables.powerSet(in);
        assertEquals(8, ps.size());
    }

    @Test
    public void testPermutations_Empty() {
        Collection<List<Integer>> perms = Iterables.permutations(Collections.<Integer> emptyList());
        assertEquals(1, perms.size());
        assertTrue(perms.iterator().next().isEmpty());
    }

    @Test
    public void testPermutations_Count() {
        Collection<List<Integer>> perms = Iterables.permutations(Arrays.asList(1, 2, 3));
        assertEquals(6, perms.size());
    }

    @Test
    public void testPermutations_NullInputTreatedAsEmpty() {
        Collection<List<Integer>> perms = Iterables.permutations((Collection<Integer>) null);
        assertEquals(1, perms.size());
        assertTrue(perms.iterator().next().isEmpty());
    }

    @Test
    public void testOrderedPermutations_NullInputTreatedAsEmpty() {
        Collection<List<Integer>> perms = Iterables.orderedPermutations((Collection<Integer>) null);
        assertEquals(1, perms.size());
        assertTrue(perms.iterator().next().isEmpty());

        Collection<List<Integer>> permsWithCmp = Iterables.orderedPermutations((Collection<Integer>) null, Comparator.reverseOrder());
        assertEquals(1, permsWithCmp.size());
        assertTrue(permsWithCmp.iterator().next().isEmpty());
    }

    @Test
    public void testRollup_Empty() {
        List<List<Object>> r = Iterables.rollup(Collections.emptyList());
        assertEquals(1, r.size());
        assertTrue(r.get(0).isEmpty());
    }

    @Test
    public void testUnionCopyIntoWithNullInput() {
        // regression: copyInto called set.addAll(null) for the documented-legal null input
        final Set<Integer> result = Iterables.union(null, CommonUtil.asSet(1, 2)).copyInto(new HashSet<>());
        assertEquals(CommonUtil.asSet(1, 2), result);
    }

    @Test
    public void testPowerSetEqualsIsOrderInsensitive() {
        // regression: equals compared the element->index map including index values, so equal
        // power sets with different iteration orders were unequal (Set.equals transitivity)
        final Set<Set<Integer>> pa = Iterables.powerSet(new java.util.LinkedHashSet<>(Arrays.asList(1, 2)));
        final Set<Set<Integer>> pb = Iterables.powerSet(new java.util.LinkedHashSet<>(Arrays.asList(2, 1)));

        assertEquals(pa, pb);
        assertEquals(pa.hashCode(), pb.hashCode());
    }

    @Test
    public void testPowerSet_AllSubsetsHaveExpectedContent() {
        // regression: the element list was rebuilt per SubSet; it is now built once in PowerSet
        // and shared by all subset views — subset content and iteration must be unchanged.
        final Set<Integer> input = new java.util.LinkedHashSet<>(Arrays.asList(1, 2, 3, 4));
        final Set<Set<Integer>> powerSet = Iterables.powerSet(input);

        assertEquals(16, powerSet.size());

        final Set<Set<Integer>> materialized = new HashSet<>();
        for (final Set<Integer> subset : powerSet) {
            assertTrue(materialized.add(subset)); // subsets are pairwise distinct
            assertTrue(input.containsAll(subset)); // every subset is a real subset of the input
        }

        assertEquals(16, materialized.size());
        assertTrue(materialized.contains(Collections.emptySet()));
        assertTrue(materialized.contains(input));
        for (final Integer e : input) {
            assertTrue(materialized.contains(Collections.singleton(e)));
        }

        // a second full iteration yields the same subsets again
        final Set<Set<Integer>> secondPass = new HashSet<>();
        for (final Set<Integer> subset : powerSet) {
            secondPass.add(subset);
        }
        assertEquals(materialized, secondPass);
    }

    @Test
    public void testSetViewIteratorContract() {
        // hasNext() must be idempotent and next() must throw NoSuchElementException once drained.
        final Set<Integer> set1 = new java.util.LinkedHashSet<>(Arrays.asList(1, 2, 3));
        final Set<Integer> set2 = new java.util.LinkedHashSet<>(Arrays.asList(3, 4));

        final List<Set<Integer>> views = Arrays.asList(Iterables.union(set1, set2), Iterables.intersection(set1, set2), Iterables.difference(set1, set2),
                Iterables.symmetricDifference(set1, set2));

        for (final Set<Integer> view : views) {
            final Iterator<Integer> iter = view.iterator();
            while (iter.hasNext()) {
                assertTrue(iter.hasNext()); // idempotent
                iter.next();
            }
            assertFalse(iter.hasNext());
            assertThrows(java.util.NoSuchElementException.class, iter::next);
        }
    }

    @Test
    public void testSetViewsWithNullElements() {
        // null elements must flow through the NULL_SENTINEL-based view iterators
        final Set<Integer> set1 = new java.util.LinkedHashSet<>(Arrays.asList(1, null, 2));
        final Set<Integer> set2 = new java.util.LinkedHashSet<>(Arrays.asList(null, 2, 3));

        assertEquals(new HashSet<>(Arrays.asList(1, null, 2, 3)), new HashSet<>(Iterables.union(set1, set2)));
        assertEquals(new HashSet<>(Arrays.asList(null, 2)), new HashSet<>(Iterables.intersection(set1, set2)));
        assertEquals(new HashSet<>(Arrays.asList(1)), new HashSet<>(Iterables.difference(set1, set2)));
        assertEquals(new HashSet<>(Arrays.asList(1, 3)), new HashSet<>(Iterables.symmetricDifference(set1, set2)));
    }

    @Test
    public void testCopyIntoSameListBackwardOverlap() {
        // destPos < srcPos on the same list takes the direct (non-snapshot) path; reads stay ahead of writes
        final List<Integer> randomAccess = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        Iterables.copyInto(randomAccess, 2, randomAccess, 0, 2);
        assertEquals(Arrays.asList(3, 4, 3, 4), randomAccess);

        final List<Integer> sequential = new LinkedList<>(Arrays.asList(1, 2, 3, 4));
        Iterables.copyInto(sequential, 2, sequential, 0, 2);
        assertEquals(Arrays.asList(3, 4, 3, 4), sequential);
    }

    @Test
    public void testSubSet_CustomComparatorConsistentWithNaturalOrder() {
        // a custom comparator that orders like the natural ordering passes the consistency check
        final NavigableSet<Integer> set = new TreeSet<>(Comparator.<Integer> naturalOrder());
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));

        assertEquals(new TreeSet<>(Arrays.asList(3, 4)), Iterables.subSet(set, Range.closedOpen(3, 5)));
    }

    @Test
    public void testKthLargest_NonPositiveKThrows() {
        // documented: IllegalArgumentException if k <= 0 and the input is neither null nor empty
        assertThrows(IllegalArgumentException.class, () -> Iterables.kthLargest(new Integer[] { 3, 1, 2 }, 0));
        assertThrows(IllegalArgumentException.class, () -> Iterables.kthLargest(Arrays.asList(3, 1, 2), -1));

        // null/empty input still yields an empty Nullable for k <= 0
        assertTrue(Iterables.kthLargest(new Integer[0], 0).isEmpty());
        assertTrue(Iterables.kthLargest((List<Integer>) null, 0).isEmpty());
    }

    @Test
    public void testPowerSet_toString_rendersElementsNotIndexMap() {
        assertEquals("powerSet([1, 2, 3])", Iterables.powerSet(new LinkedHashSet<>(Arrays.asList(1, 2, 3))).toString());
        assertEquals("powerSet([])", Iterables.powerSet(new LinkedHashSet<String>()).toString());
        assertEquals("powerSet([a])", Iterables.powerSet(new LinkedHashSet<>(Arrays.asList("a"))).toString());
    }

    @Test
    public void testAsReversed_nullList_hasANamedMessage() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Iterables.asReversed(null));
        assertEquals("'list' cannot be null", e.getMessage());
    }

    @Test
    public void testAsReversed_listIteratorSetWithoutNext_hasAMessage() {
        final List<Integer> reversed = Iterables.asReversed(new ArrayList<>(Arrays.asList(1, 2, 3)));

        final IllegalStateException e = assertThrows(IllegalStateException.class, () -> reversed.listIterator().set(9));
        assertEquals("no calls to next() or previous() since the last call to remove() or add()", e.getMessage());

        final IllegalStateException onRemove = assertThrows(IllegalStateException.class, () -> reversed.listIterator().remove());
        assertEquals("no calls to next() or previous() since the last call to remove() or add()", onRemove.getMessage(),
                "remove() and set() guard the same flag - which previous() and add() also touch - so they share one message");
    }

    @Test
    public void testAsReversed_listIteratorSetAfterNextStillWorks() {
        final List<Integer> forward = new ArrayList<>(Arrays.asList(1, 2, 3));
        final List<Integer> reversed = Iterables.asReversed(forward);
        final ListIterator<Integer> iter = reversed.listIterator();

        iter.next();
        iter.set(99);

        assertEquals(Arrays.asList(99, 2, 1), reversed);
        assertEquals(Arrays.asList(1, 2, 99), forward);
    }

    @Test
    public void testPermutations_contains_multisetSemantics() {
        final Collection<List<Integer>> perms = Iterables.permutations(Arrays.asList(1, 1, 2));

        assertTrue(perms.contains(Arrays.asList(1, 1, 2)));
        assertTrue(perms.contains(Arrays.asList(1, 2, 1)));
        assertTrue(perms.contains(Arrays.asList(2, 1, 1)));
        assertFalse(perms.contains(Arrays.asList(1, 1, 1)), "wrong multiplicity must not match");
        assertFalse(perms.contains(Arrays.asList(2, 2, 1)), "wrong multiplicity must not match");
        assertFalse(perms.contains(Arrays.asList(1, 2, 3)));
        assertFalse(perms.contains(Arrays.asList(1, 2)), "a shorter list must not match");
        assertFalse(perms.contains(Arrays.asList(1, 1, 2, 2)), "a longer list must not match");
        assertFalse(perms.contains("not a list"));
    }

    @Test
    public void testPermutations_contains_withNullElements() {
        final Collection<List<String>> perms = Iterables.permutations(Arrays.asList("a", null));

        assertTrue(perms.contains(Arrays.asList("a", null)));
        assertTrue(perms.contains(Arrays.asList(null, "a")));
        assertFalse(perms.contains(Arrays.asList("a", "a")));
        assertFalse(perms.contains(Arrays.asList(null, null)));
    }

    @Test
    public void testPermutations_contains_emptyAndOrdered() {
        assertTrue(Iterables.permutations(new ArrayList<Integer>()).contains(new ArrayList<>()));

        final Collection<List<Integer>> ordered = Iterables.orderedPermutations(Arrays.asList(1, 1, 2));
        assertTrue(ordered.contains(Arrays.asList(2, 1, 1)));
        assertFalse(ordered.contains(Arrays.asList(2, 2, 1)));
    }

    @Test
    public void testPermutations_contains_agreesWithEnumeration() {
        final List<Integer> input = Arrays.asList(1, 2, 2, 3);
        final Collection<List<Integer>> perms = Iterables.permutations(input);
        final Set<List<Integer>> enumerated = new HashSet<>();
        perms.forEach(enumerated::add);

        for (final List<Integer> candidate : enumerated) {
            assertTrue(perms.contains(candidate));
        }

        assertFalse(perms.contains(Arrays.asList(1, 2, 3, 3)));
        assertFalse(perms.contains(Arrays.asList(2, 2, 2, 3)));
    }

    @Test
    public void testSetView_isConcrete() {
        assertFalse(java.lang.reflect.Modifier.isAbstract(Iterables.SetView.class.getModifiers()));
    }

    @Test
    public void testSetViews_stillUnmodifiableAndCorrect() {
        final Set<Integer> set1 = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        final Set<Integer> set2 = new LinkedHashSet<>(Arrays.asList(3, 4));

        assertEquals(Arrays.asList(1, 2, 3, 4), new ArrayList<>(Iterables.union(set1, set2)));
        assertEquals(Arrays.asList(3), new ArrayList<>(Iterables.intersection(set1, set2)));
        assertEquals(Arrays.asList(1, 2), new ArrayList<>(Iterables.difference(set1, set2)));
        assertEquals(Arrays.asList(1, 2, 4), new ArrayList<>(Iterables.symmetricDifference(set1, set2)));

        assertThrows(UnsupportedOperationException.class, () -> Iterables.union(set1, set2).add(9));
        assertThrows(UnsupportedOperationException.class, () -> Iterables.intersection(set1, set2).clear());
        assertThrows(UnsupportedOperationException.class, () -> Iterables.difference(set1, set2).remove(1));
        assertThrows(UnsupportedOperationException.class, () -> Iterables.symmetricDifference(set1, set2).removeIf(x -> true));

        final Iterator<Integer> iter = Iterables.union(set1, set2).iterator();
        iter.next();
        assertThrows(UnsupportedOperationException.class, iter::remove);

        assertEquals(new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4)), Iterables.union(set1, set2).copyInto(new LinkedHashSet<>()));
        assertEquals(new LinkedHashSet<>(Arrays.asList(1, 2)), Iterables.difference(set1, set2).copyInto(new LinkedHashSet<>()));
    }

    @Test
    public void testLowerMedian_nullSortsAsMinimum() {
        assertEquals(Nullable.of(1), Iterables.lowerMedian(new Integer[] { 3, null, 1 }));
        assertEquals(Nullable.of((Integer) null), Iterables.lowerMedian(new Integer[] { null, null, 5 }));
        assertEquals(Nullable.of(1), Iterables.lowerMedian(Arrays.asList(3, null, 1)));
        assertEquals(Nullable.of((Integer) null), Iterables.lowerMedian(Arrays.asList(null, null, 5)));
    }

    @Test
    public void testKthLargest_nullSortsAsMinimum() {
        assertEquals(Nullable.of(3), Iterables.kthLargest(new Integer[] { 3, null, 1 }, 1));
        assertEquals(Nullable.of(1), Iterables.kthLargest(new Integer[] { 3, null, 1 }, 2));
        assertEquals(Nullable.of((Integer) null), Iterables.kthLargest(new Integer[] { 3, null, 1 }, 3));
        assertEquals(Nullable.of(3), Iterables.kthLargest(Arrays.asList(3, null, 1), 1));
        assertEquals(Nullable.of((Integer) null), Iterables.kthLargest(Arrays.asList(3, null, 1), 3));
    }

    @Test
    public void testNullOrdering_lowerMedianAndKthLargestDifferFromMin() {
        final Integer[] a = { 3, null, 1 };

        // min() documents null as the MAXIMUM; max() and the median/kth family treat it as the MINIMUM
        assertEquals(Nullable.of(1), Iterables.min(a));
        assertEquals(Nullable.of(3), Iterables.max(a));
        assertEquals(Nullable.of((Integer) null), Iterables.kthLargest(a, 3));
    }

    @Test
    public void testPermutations_sizeSaturatesBeyondTwelveElements() {
        final List<Integer> twelve = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            twelve.add(i);
        }
        assertEquals(479001600, Iterables.permutations(twelve).size(), "12! still fits an int");

        final List<Integer> thirteen = new ArrayList<>(twelve);
        thirteen.add(12);
        assertEquals(Integer.MAX_VALUE, Iterables.permutations(thirteen).size(), "13! saturates - documented");

        // stream().count() short-circuits on the SIZED spliterator, so it reports the saturated size
        // rather than the true 6227020800 - documented on the method.
        assertEquals(Integer.MAX_VALUE, Iterables.permutations(thirteen).stream().count());
    }

    @Test
    public void testSubSet_nullSetReturnsAnImmutableEmptySet() {
        final NavigableSet<Integer> view = Iterables.subSet(null, Range.closed(2, 4));

        assertTrue(view.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> view.add(1));
    }

    @Test
    public void testSubSet_nonNullSetIsAMutableLiveView() {
        final NavigableSet<Integer> backing = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5));
        final NavigableSet<Integer> view = Iterables.subSet(backing, Range.closedOpen(2, 4));

        assertEquals(Arrays.asList(2, 3), new ArrayList<>(view));
        backing.remove(3);
        assertEquals(Arrays.asList(2), new ArrayList<>(view), "the view must read through to the backing set");
    }

    @Test
    public void testOrderedPermutations_containsRejectsArrangementsItNeverYields() {
        // Under CASE_INSENSITIVE_ORDER "a" and "A" compare equal, so they are never swapped: the collection holds
        // exactly one arrangement. contains() used to answer with an equals-based multiset test, which accepted the
        // arrangement that is never yielded.
        final Collection<List<String>> perms = Iterables.orderedPermutations(Arrays.asList("a", "A"), String.CASE_INSENSITIVE_ORDER);

        assertEquals(1, perms.size());

        final List<List<String>> yielded = new ArrayList<>(perms);
        assertEquals(1, yielded.size());

        final List<String> theOnlyMember = yielded.get(0);
        final List<String> reversed = new ArrayList<>(theOnlyMember);
        Collections.reverse(reversed);

        assertTrue(perms.contains(theOnlyMember));
        assertFalse(perms.contains(reversed), "an arrangement the collection never yields must not be reported as contained");
    }

    @Test
    public void testOrderedPermutations_containsStillAcceptsEveryYieldedPermutation() {
        // the ordinary cases must be unaffected: every element of the collection is contained in it
        for (final List<Integer> input : Arrays.asList(Arrays.asList(3, 1, 2), Arrays.asList(1, 1, 2), Arrays.<Integer> asList())) {
            final Collection<List<Integer>> perms = Iterables.orderedPermutations(input);

            for (final List<Integer> perm : perms) {
                assertTrue(perms.contains(perm), perm + " must be contained in " + input);
                assertTrue(perms.contains(new ArrayList<>(perm)), "membership must not depend on the List implementation");
            }

            assertFalse(perms.contains(Arrays.asList(9, 9, 9, 9)));
            assertFalse(perms.contains("not a list"));
        }

        // PermutationCollection permutes by position and does yield every arrangement, so it keeps the equals-based test
        final Collection<List<String>> plain = Iterables.permutations(Arrays.asList("a", "A"));
        assertEquals(2, plain.size());
        assertTrue(plain.contains(Arrays.asList("a", "A")));
        assertTrue(plain.contains(Arrays.asList("A", "a")));
    }

    @Test
    public void testOrderedPermutationsMembershipMatchesEmittedTieRepresentatives() {
        for (final List<String> input : Arrays.asList(Arrays.asList("a", "A", "b"), Arrays.asList("a", "A", "b", "B"))) {
            final Collection<List<String>> permutations = Iterables.orderedPermutations(input, String.CASE_INSENSITIVE_ORDER);
            final List<List<String>> emitted = new ArrayList<>(permutations);

            for (final List<String> candidate : Iterables.permutations(input)) {
                assertEquals(emitted.contains(candidate), permutations.contains(candidate), "membership for " + candidate);
                assertEquals(emitted.contains(candidate), permutations.contains(new LinkedList<>(candidate)));
            }

            assertFalse(permutations.contains(Collections.nCopies(input.size(), "foreign")));
        }
    }

    @Test
    public void testOrderedPermutations_containsAnswersInsteadOfThrowingForForeignElements() {
        final Collection<List<Integer>> perms = Iterables.orderedPermutations(Arrays.asList(1, 2));

        assertFalse(perms.contains(Arrays.asList("x", "y")), "elements the comparator cannot order are simply absent");
        assertFalse(perms.contains(Arrays.asList(null, null)));
        assertFalse(perms.contains(Arrays.asList(1, 2, 3)), "a different size is never a permutation");
    }

    @Test
    public void testCopyInto_zeroLengthIsAlwaysANoOp() {
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.copyInto(null, 1, null, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.copyInto(null, 0, null, 1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.copyInto(Arrays.asList("a"), 2, null, 0, 0));
        assertDoesNotThrow(() -> Iterables.copyInto(null, 0, null, 0, 0));
        assertDoesNotThrow(() -> Iterables.copyInto(Arrays.asList("a"), 0, null, 0, 0));
        assertDoesNotThrow(() -> Iterables.copyInto(null, 0, new ArrayList<String>(), 0, 0));
        assertDoesNotThrow(() -> Iterables.copyInto(Arrays.asList("a"), 1, new ArrayList<>(Arrays.asList("z")), 1, 0));

        final List<String> dest = new ArrayList<>(Arrays.asList("x", "y"));
        Iterables.copyInto(Arrays.asList("a", "b"), 0, dest, 0, 0);
        assertEquals(Arrays.asList("x", "y"), dest, "a zero-length copy must not touch the destination");
    }

    @Test
    public void testCopyInto_negativeLengthNamesTheOffendingParameter() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Iterables.copyInto(Arrays.asList(1, 2), 0, new ArrayList<>(Arrays.asList(1, 2)), 0, -1));
        assertTrue(e.getMessage().contains("length"), "message should name 'length', was: " + e.getMessage());
    }

    @Test
    public void testCopyInto_destTooSmallMessageNamesBothSizes() {
        final IndexOutOfBoundsException e = assertThrows(IndexOutOfBoundsException.class,
                () -> Iterables.copyInto(Arrays.asList(1, 2, 3), new ArrayList<>(Arrays.asList(1))));
        assertTrue(e.getMessage().contains("3") && e.getMessage().contains("1"), "message should name both sizes, was: " + e.getMessage());
    }

    @Test
    public void testCopyInto_nonRandomAccessListsStillCopyTheRightRange() {
        // the positioned listIterator(int) rewrite must not change behaviour on non-RandomAccess lists
        final List<String> src = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));
        final List<String> dest = new LinkedList<>(Arrays.asList("x", "y", "z", "w", "v"));

        Iterables.copyInto(src, 1, dest, 2, 3);

        assertEquals(Arrays.asList("x", "y", "b", "c", "d"), dest);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), src, "the source must not be modified");
    }

    @Test
    public void testCopyInto_overlappingSelfCopyStillBehavesAsIfSnapshotted() {
        final List<Integer> forward = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5));
        Iterables.copyInto(forward, 1, forward, 3, 3); // destPos > srcPos: needs the snapshot
        assertEquals(Arrays.asList(0, 1, 2, 1, 2, 3), forward);

        final List<Integer> backward = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5));
        Iterables.copyInto(backward, 3, backward, 1, 3); // destPos < srcPos: a forward copy is already safe
        assertEquals(Arrays.asList(0, 3, 4, 5, 4, 5), backward);
    }

    @Test
    public void testCombinatorialCollections_toStringNamesTheFactoryAndStaysLazy() {
        assertEquals("permutations([3, 1, 2])", Iterables.permutations(Arrays.asList(3, 1, 2)).toString());
        assertEquals("orderedPermutations([1, 2, 3])", Iterables.orderedPermutations(Arrays.asList(3, 1, 2)).toString());
        assertEquals("powerSet([1, 2])", Iterables.powerSet(new LinkedHashSet<>(Arrays.asList(1, 2))).toString());
        assertEquals("cartesianProduct([[1, 2], [A, B]])", Iterables.cartesianProduct(Arrays.asList(1, 2), Arrays.asList("A", "B")).toString());

        assertEquals("cartesianProduct([])", Iterables.cartesianProduct(Collections.<Collection<Object>> emptyList()).toString());
        assertEquals("cartesianProduct([[], [A]])", Iterables.cartesianProduct(Collections.<String> emptyList(), Arrays.asList("A")).toString());

        // the tuples themselves are still one wrapper away
        assertEquals("[[1, A], [1, B], [2, A], [2, B]]", new ArrayList<>(Iterables.cartesianProduct(Arrays.asList(1, 2), Arrays.asList("A", "B"))).toString());
    }

    @Test
    public void testPowerSet_rejectsMoreThan30ElementsBeforeMaterialising() {
        final Set<Integer> tooBig = new LinkedHashSet<>();

        for (int i = 0; i < 31; i++) {
            tooBig.add(i);
        }

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Iterables.powerSet(tooBig));
        assertTrue(e.getMessage().contains("31") && e.getMessage().contains("30"), e.getMessage());

        // 30 is still accepted, and still reports 2^30
        final Set<Integer> atTheLimit = new LinkedHashSet<>();

        for (int i = 0; i < 30; i++) {
            atTheLimit.add(i);
        }

        assertEquals(1 << 30, Iterables.powerSet(atTheLimit).size());
    }

    @Test
    public void testAsReversed_listIteratorStateMessagesMentionPreviousAndAdd() {
        final ListIterator<Integer> iter = Iterables.asReversed(new ArrayList<>(Arrays.asList(1, 2, 3))).listIterator();

        final String expected = "no calls to next() or previous() since the last call to remove() or add()";
        assertEquals(expected, assertThrows(IllegalStateException.class, iter::remove).getMessage());
        assertEquals(expected, assertThrows(IllegalStateException.class, () -> iter.set(9)).getMessage());

        // previous() also arms both operations, which is why the message names it
        final List<Integer> reversed = Iterables.asReversed(new ArrayList<>(Arrays.asList(1, 2, 3)));
        final ListIterator<Integer> atEnd = reversed.listIterator(3);
        assertEquals(1, atEnd.previous());
        assertDoesNotThrow(() -> atEnd.set(9));
        assertDoesNotThrow(atEnd::remove);
    }

    @Test
    public void testSetView_copyIntoRejectsNullTarget() {
        assertThrows(IllegalArgumentException.class, () -> Iterables.intersection(CommonUtil.asSet(1), CommonUtil.asSet(1)).copyInto(null));
        assertThrows(IllegalArgumentException.class, () -> Iterables.difference(CommonUtil.asSet(1), CommonUtil.asSet(2)).copyInto(null));
        assertThrows(IllegalArgumentException.class, () -> Iterables.symmetricDifference(CommonUtil.asSet(1), CommonUtil.asSet(2)).copyInto(null));
        assertThrows(IllegalArgumentException.class, () -> Iterables.union(CommonUtil.asSet(1), CommonUtil.asSet(2)).copyInto(null));

        // union's override skips both addAll calls when both inputs are empty, so it used to return null here
        assertThrows(IllegalArgumentException.class, () -> Iterables.union(new LinkedHashSet<Integer>(), new LinkedHashSet<Integer>()).copyInto(null));

        // the happy path is unchanged
        assertEquals(new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4)),
                Iterables.union(new LinkedHashSet<>(Arrays.asList(1, 2, 3)), new LinkedHashSet<>(Arrays.asList(3, 4))).copyInto(new LinkedHashSet<>()));
    }

    @Test
    public void testIterateNonEmpty() {
        assertEquals("'c' cannot be null or empty", assertThrows(IllegalArgumentException.class, () -> Iterables.iterateNonEmpty(null, "c")).getMessage());
        assertEquals("custom message here",
                assertThrows(IllegalArgumentException.class, () -> Iterables.iterateNonEmpty(null, "custom message here")).getMessage());
        assertThrows(IllegalArgumentException.class, () -> Iterables.iterateNonEmpty(new ArrayList<String>(), "c"));

        assertEquals(Arrays.asList(1, 2, 3), CommonUtil.toList(Iterables.iterateNonEmpty(Arrays.asList(1, 2, 3), "c")),
                "the returned iterator must not have been consumed");

        // a non-Collection Iterable is probed with hasNext(), which must not swallow the first element either
        final Iterable<Integer> iterable = () -> Arrays.asList(7, 8).iterator();
        assertEquals(Arrays.asList(7, 8), CommonUtil.toList(Iterables.iterateNonEmpty(iterable, "c")));

        final Iterable<Integer> emptyIterable = Collections::emptyIterator;
        assertThrows(IllegalArgumentException.class, () -> Iterables.iterateNonEmpty(emptyIterable, "c"));
    }

    @Test
    public void reviewFixes20260906_cartesianProductAndPowerSetTrustTheIterator() {
        final java.util.List<java.util.Collection<?>> real = java.util.Arrays.asList(java.util.Arrays.asList(1, 2), java.util.Arrays.asList("a", "b"));

        // CartesianList sized its axis array from cs.size() but filled it from cs.iterator() - two independent
        // reads. An under-reported size silently truncated the product; an over-reported one threw an
        // undocumented NoSuchElementException.
        final java.util.List<java.util.List<Object>> expected = Iterables.cartesianProduct(real);

        assertEquals(expected, Iterables.cartesianProduct(new java.util.AbstractCollection<java.util.Collection<?>>() {
            @Override
            public java.util.Iterator<java.util.Collection<?>> iterator() {
                return real.iterator();
            }

            @Override
            public int size() {
                return 1;
            }
        }));

        assertEquals(expected, Iterables.cartesianProduct(new java.util.AbstractCollection<java.util.Collection<?>>() {
            @Override
            public java.util.Iterator<java.util.Collection<?>> iterator() {
                return real.iterator();
            }

            @Override
            public int size() {
                return 3;
            }
        }));

        assertEquals(4, expected.size());

        // PowerSet checked input.size() but shifted by the count indexMap() actually saw. A Set that
        // under-reported slipped through and made size() return 1 << 31 == Integer.MIN_VALUE, so the power set
        // iterated as empty and toArray() threw NegativeArraySizeException.
        final java.util.LinkedHashSet<Integer> backing = new java.util.LinkedHashSet<>();

        for (int i = 0; i < 31; i++) {
            backing.add(i);
        }

        final java.util.Set<Integer> lying = new java.util.AbstractSet<>() {
            @Override
            public java.util.Iterator<Integer> iterator() {
                return backing.iterator();
            }

            @Override
            public int size() {
                return 30;
            }
        };

        assertThrows(IllegalArgumentException.class, () -> Iterables.powerSet(lying));
        assertThrows(IllegalArgumentException.class, () -> Iterables.powerSet(backing));

        // An honest set still works, and duplicates still collapse before the limit is applied.
        assertEquals(8, Iterables.powerSet(new java.util.LinkedHashSet<>(CommonUtil.asList(1, 2, 3))).size());
    }

    @Test
    public void reviewFixes20260906_powerSetAndCartesianProductAreUnmodifiable() {
        // permutations/orderedPermutations promise "an unmodifiable Collection"; powerSet and cartesianProduct are
        // just as unmodifiable (AbstractSet/AbstractList with no mutator overridden) but did not say so.
        final Set<Set<Integer>> ps = Iterables.powerSet(new LinkedHashSet<>(CommonUtil.asList(1, 2)));
        assertThrows(UnsupportedOperationException.class, () -> ps.add(new LinkedHashSet<>(CommonUtil.asList(9))));
        assertThrows(UnsupportedOperationException.class, () -> ps.remove(new LinkedHashSet<>(CommonUtil.asList(1))));
        assertThrows(UnsupportedOperationException.class, ps::clear);
        assertThrows(UnsupportedOperationException.class, () -> ps.removeIf(s -> true));

        final Iterator<Set<Integer>> psIter = ps.iterator();
        psIter.next();
        assertThrows(UnsupportedOperationException.class, psIter::remove);

        final List<List<Integer>> cp = Iterables.cartesianProduct(CommonUtil.asList(1, 2), CommonUtil.asList(3));
        assertThrows(UnsupportedOperationException.class, () -> cp.set(0, CommonUtil.asList(9, 9)));
        assertThrows(UnsupportedOperationException.class, () -> cp.add(CommonUtil.asList(9, 9)));
        assertThrows(UnsupportedOperationException.class, () -> cp.remove(0));
        assertThrows(UnsupportedOperationException.class, cp::clear);
        assertThrows(UnsupportedOperationException.class, () -> cp.removeIf(x -> true));

        // ... and none of that disturbed either result.
        assertEquals(4, ps.size());
        assertEquals(CommonUtil.asList(CommonUtil.asList(1, 3), CommonUtil.asList(2, 3)), cp);
    }

    /**
     * Pins the documented exception to "random-access in, random-access out": the reversed <i>view</i> of an
     * {@link ImmutableList} deliberately drops the {@link RandomAccess} marker, while a zero-/one-element
     * {@code ImmutableList} that owns its backing storage is returned unchanged and keeps it.
     */
    @Test
    public void testAsReversed_ImmutableListViewIsNotRandomAccess() {
        final ImmutableList<Integer> source = ImmutableList.of(1, 2, 3);
        assertTrue(source instanceof RandomAccess);

        final List<Integer> reversed = Iterables.asReversed(source);
        assertFalse(reversed instanceof RandomAccess, "the reversed view of an ImmutableList drops the marker");
        assertEquals(3, reversed.size());
        assertEquals(Integer.valueOf(3), reversed.get(0));
        assertEquals(Integer.valueOf(1), reversed.get(2));

        // A one-element ImmutableList that does NOT own its backing storage is a view too, so it drops it as well.
        final ImmutableList<Integer> wrapped = ImmutableList.wrap(new ArrayList<>(Arrays.asList(1)));
        assertTrue(wrapped instanceof RandomAccess);
        assertFalse(Iterables.asReversed(wrapped) instanceof RandomAccess);

        // The zero-/one-element lists that own their backing are returned unchanged, marker included.
        assertTrue(Iterables.asReversed(ImmutableList.of(1)) instanceof RandomAccess);
        assertTrue(Iterables.asReversed(ImmutableList.<Integer> empty()) instanceof RandomAccess);

        // Every other random-access input still honours the unqualified promise.
        assertTrue(Iterables.asReversed(Arrays.asList(1, 2, 3)) instanceof RandomAccess);
    }
}
