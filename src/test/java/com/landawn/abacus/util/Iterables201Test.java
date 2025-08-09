package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;

public class Iterables201Test extends TestBase {

    // Helper for comparators
    private static final Comparator<Integer> REVERSE_ORDER_NULLS_FIRST = Comparator.nullsFirst(Comparator.reverseOrder());
    private static final Comparator<Integer> REVERSE_ORDER_NULLS_LAST = Comparator.nullsLast(Comparator.reverseOrder());

    private static class TestObject implements Comparable<TestObject> {
        final int id;
        final String value;

        TestObject(int id, String value) {
            this.id = id;
            this.value = value;
        }

        public int getId() {
            return id;
        }

        public String getValue() {
            return value;
        }

        @Override
        public int compareTo(TestObject o) {
            return Integer.compare(this.id, o.id);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TestObject that = (TestObject) o;
            return id == that.id && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, value);
        }

        @Override
        public String toString() {
            return "TestObject{id=" + id + ", value='" + value + "'}";
        }
    }

    //region Min Methods
    @Test
    public void testMinCharArray() {
        assertTrue(Iterables.min((char[]) null).isEmpty());
        assertTrue(Iterables.min(new char[0]).isEmpty());
        assertEquals('a', Iterables.min('c', 'a', 'b').orElseThrow());
    }

    @Test
    public void testMinByteArray() {
        assertTrue(Iterables.min((byte[]) null).isEmpty());
        assertTrue(Iterables.min(new byte[0]).isEmpty());
        assertEquals((byte) 1, Iterables.min((byte) 3, (byte) 1, (byte) 2).orElseThrow());
    }

    @Test
    public void testMinShortArray() {
        assertTrue(Iterables.min((short[]) null).isEmpty());
        assertTrue(Iterables.min(new short[0]).isEmpty());
        assertEquals((short) 1, Iterables.min((short) 3, (short) 1, (short) 2).orElseThrow());
    }

    @Test
    public void testMinIntArray() {
        assertTrue(Iterables.min((int[]) null).isEmpty());
        assertTrue(Iterables.min(new int[0]).isEmpty());
        assertEquals(1, Iterables.min(3, 1, 2).orElseThrow());
    }

    @Test
    public void testMinLongArray() {
        assertTrue(Iterables.min((long[]) null).isEmpty());
        assertTrue(Iterables.min(new long[0]).isEmpty());
        assertEquals(1L, Iterables.min(3L, 1L, 2L).orElseThrow());
    }

    @Test
    public void testMinFloatArray() {
        assertTrue(Iterables.min((float[]) null).isEmpty());
        assertTrue(Iterables.min(new float[0]).isEmpty());
        assertEquals(1.0f, Iterables.min(3.0f, 1.0f, 2.0f).orElseThrow(), 0.0f);
    }

    @Test
    public void testMinDoubleArray() {
        assertTrue(Iterables.min((double[]) null).isEmpty());
        assertTrue(Iterables.min(new double[0]).isEmpty());
        assertEquals(1.0, Iterables.min(3.0, 1.0, 2.0).orElseThrow(), 0.0);
    }

    @Test
    public void testMinGenericArray() {
        assertTrue(Iterables.min((Integer[]) null).isEmpty());
        assertTrue(Iterables.min(new Integer[0]).isEmpty());
        assertEquals(1, Iterables.min(new Integer[] { 3, 1, 2 }).orElse(null));
        assertEquals(1, Iterables.min(new Integer[] { null, 3, 1, 2 }).orElse(null)); // nulls are max
    }

    @Test
    public void testMinGenericArrayWithComparator() {
        assertTrue(Iterables.min((Integer[]) null, REVERSE_ORDER_NULLS_LAST).isEmpty());
        assertTrue(Iterables.min(new Integer[0], REVERSE_ORDER_NULLS_LAST).isEmpty());
        assertEquals(3, Iterables.min(new Integer[] { 3, 1, 2 }, REVERSE_ORDER_NULLS_LAST).orElse(null));
        assertEquals(3, Iterables.min(new Integer[] { null, 3, 1, 2 }, REVERSE_ORDER_NULLS_LAST).orElse(null));
        assertNull(Iterables.min(new Integer[] { null, 3, 1, 2 }, REVERSE_ORDER_NULLS_FIRST).orElse(Integer.MAX_VALUE));
    }

    @Test
    public void testMinIterable() {
        assertTrue(Iterables.min((Iterable<Integer>) null).isEmpty());
        assertTrue(Iterables.min(Collections.<Integer> emptyList()).isEmpty());
        assertEquals(1, Iterables.min(Arrays.asList(3, 1, 2)).orElse(null));
        assertEquals(1, Iterables.min(Arrays.asList(null, 3, 1, 2)).orElse(null));
    }

    @Test
    public void testMinIterableWithComparator() {
        assertTrue(Iterables.min((Iterable<Integer>) null, REVERSE_ORDER_NULLS_LAST).isEmpty());
        assertTrue(Iterables.min(Collections.<Integer> emptyList(), REVERSE_ORDER_NULLS_LAST).isEmpty());
        assertEquals(3, Iterables.min(Arrays.asList(3, 1, 2), REVERSE_ORDER_NULLS_LAST).orElse(null));
        assertEquals(3, Iterables.min(Arrays.asList(null, 3, 1, 2), REVERSE_ORDER_NULLS_LAST).orElse(null));
        assertNull(Iterables.min(Arrays.asList(null, 3, 1, 2), REVERSE_ORDER_NULLS_FIRST).orElse(Integer.MAX_VALUE));
    }

    @Test
    public void testMinIterator() {
        assertTrue(Iterables.min((Iterator<Integer>) null).isEmpty());
        assertTrue(Iterables.min(Collections.<Integer> emptyList().iterator()).isEmpty());
        assertEquals(1, Iterables.min(Arrays.asList(3, 1, 2).iterator()).orElse(null));
        assertEquals(1, Iterables.min(Arrays.asList(null, 3, 1, 2).iterator()).orElse(null));
    }

    @Test
    public void testMinIteratorWithComparator() {
        assertTrue(Iterables.min((Iterator<Integer>) null, REVERSE_ORDER_NULLS_LAST).isEmpty());
        assertTrue(Iterables.min(Collections.<Integer> emptyList().iterator(), REVERSE_ORDER_NULLS_LAST).isEmpty());
        assertEquals(3, Iterables.min(Arrays.asList(3, 1, 2).iterator(), REVERSE_ORDER_NULLS_LAST).orElse(null));
        assertEquals(3, Iterables.min(Arrays.asList(null, 3, 1, 2).iterator(), REVERSE_ORDER_NULLS_LAST).orElse(null));
        assertNull(Iterables.min(Arrays.asList(null, 3, 1, 2).iterator(), REVERSE_ORDER_NULLS_FIRST).orElse(Integer.MAX_VALUE));
        // Test specific null handling in min(Iterator, Comparator)
        assertNull(Iterables.min(Arrays.asList(null, 1, 2).iterator(), Comparator.nullsFirst(Integer::compareTo)).get());
        assertEquals(1, Iterables.min(Arrays.asList(1, null, 2).iterator(), Comparator.nullsLast(Integer::compareTo)).get());
    }

    @Test
    public void testMinByArray() {
        TestObject[] arr = { new TestObject(3, "ccc"), new TestObject(1, "a"), new TestObject(2, "bb") };
        assertTrue(Iterables.minBy((TestObject[]) null, TestObject::getId).isEmpty());
        assertTrue(Iterables.minBy(new TestObject[0], TestObject::getId).isEmpty());
        assertEquals(arr[1], Iterables.minBy(arr, TestObject::getId).orElse(null));
        assertEquals(arr[1], Iterables.minBy(arr, TestObject::getValue).orElse(null)); // "a" is shortest
    }

    @Test
    public void testMinByIterable() {
        List<TestObject> list = Arrays.asList(new TestObject(3, "ccc"), new TestObject(1, "a"), new TestObject(2, "bb"));
        assertTrue(Iterables.minBy((List<TestObject>) null, TestObject::getId).isEmpty());
        assertTrue(Iterables.minBy(Collections.<TestObject> emptyList(), TestObject::getId).isEmpty());
        assertEquals(list.get(1), Iterables.minBy(list, TestObject::getId).orElse(null));
    }

    @Test
    public void testMinByIterator() {
        List<TestObject> list = Arrays.asList(new TestObject(3, "ccc"), new TestObject(1, "a"), new TestObject(2, "bb"));
        assertTrue(Iterables.minBy((Iterator<TestObject>) null, TestObject::getId).isEmpty());
        assertTrue(Iterables.minBy(Collections.<TestObject> emptyList().iterator(), TestObject::getId).isEmpty());
        assertEquals(list.get(1), Iterables.minBy(list.iterator(), TestObject::getId).orElse(null));
    }

    @Test
    public void testMinIntArrayWithValueExtractor() {
        TestObject[] arr = { new TestObject(3, "c"), new TestObject(1, "a"), new TestObject(2, "b") };
        assertTrue(Iterables.minInt((TestObject[]) null, TestObject::getId).isEmpty());
        assertTrue(Iterables.minInt(new TestObject[0], TestObject::getId).isEmpty());
        assertEquals(1, Iterables.minInt(arr, TestObject::getId).orElseThrow());
    }

    @Test
    public void testMinIntIterableWithValueExtractor() {
        List<TestObject> list = Arrays.asList(new TestObject(3, "c"), new TestObject(1, "a"), new TestObject(2, "b"));
        assertTrue(Iterables.minInt((List<TestObject>) null, TestObject::getId).isEmpty());
        assertTrue(Iterables.minInt(Collections.<TestObject> emptyList(), TestObject::getId).isEmpty());
        assertEquals(1, Iterables.minInt(list, TestObject::getId).orElseThrow());
    }

    @Test
    public void testMinIntIteratorWithValueExtractor() {
        List<TestObject> list = Arrays.asList(new TestObject(3, "c"), new TestObject(1, "a"), new TestObject(2, "b"));
        assertTrue(Iterables.minInt((Iterator<TestObject>) null, TestObject::getId).isEmpty());
        assertTrue(Iterables.minInt(Collections.<TestObject> emptyList().iterator(), TestObject::getId).isEmpty());
        assertEquals(1, Iterables.minInt(list.iterator(), TestObject::getId).orElseThrow());
    }

    @Test
    public void testMinLongArrayWithValueExtractor() {
        TestObject[] arr = { new TestObject(3, "c"), new TestObject(1, "a"), new TestObject(2, "b") };
        ToLongFunction<TestObject> extractor = obj -> (long) obj.getId();
        assertTrue(Iterables.minLong((TestObject[]) null, extractor).isEmpty());
        assertTrue(Iterables.minLong(new TestObject[0], extractor).isEmpty());
        assertEquals(1L, Iterables.minLong(arr, extractor).orElseThrow());
    }

    @Test
    public void testMinLongIterableWithValueExtractor() {
        List<TestObject> list = Arrays.asList(new TestObject(3, "c"), new TestObject(1, "a"), new TestObject(2, "b"));
        ToLongFunction<TestObject> extractor = obj -> (long) obj.getId();
        assertTrue(Iterables.minLong((List<TestObject>) null, extractor).isEmpty());
        assertTrue(Iterables.minLong(Collections.emptyList(), extractor).isEmpty());
        assertEquals(1L, Iterables.minLong(list, extractor).orElseThrow());
    }

    @Test
    public void testMinLongIteratorWithValueExtractor() {
        List<TestObject> list = Arrays.asList(new TestObject(3, "c"), new TestObject(1, "a"), new TestObject(2, "b"));
        ToLongFunction<TestObject> extractor = obj -> (long) obj.getId();
        assertTrue(Iterables.minLong((Iterator<TestObject>) null, extractor).isEmpty());
        assertTrue(Iterables.minLong(Collections.<TestObject> emptyList().iterator(), extractor).isEmpty());
        assertEquals(1L, Iterables.minLong(list.iterator(), extractor).orElseThrow());
    }

    @Test
    public void testMinDoubleArrayWithValueExtractor() {
        TestObject[] arr = { new TestObject(3, "c"), new TestObject(1, "a"), new TestObject(2, "b") };
        ToDoubleFunction<TestObject> extractor = obj -> (double) obj.getId();
        assertTrue(Iterables.minDouble((TestObject[]) null, extractor).isEmpty());
        assertTrue(Iterables.minDouble(new TestObject[0], extractor).isEmpty());
        assertEquals(1.0, Iterables.minDouble(arr, extractor).orElseThrow(), 0.0);
    }

    @Test
    public void testMinDoubleIterableWithValueExtractor() {
        List<TestObject> list = Arrays.asList(new TestObject(3, "c"), new TestObject(1, "a"), new TestObject(2, "b"));
        ToDoubleFunction<TestObject> extractor = obj -> (double) obj.getId();
        assertTrue(Iterables.minDouble((List<TestObject>) null, extractor).isEmpty());
        assertTrue(Iterables.minDouble(Collections.emptyList(), extractor).isEmpty());
        assertEquals(1.0, Iterables.minDouble(list, extractor).orElseThrow(), 0.0);
    }

    @Test
    public void testMinDoubleIteratorWithValueExtractor() {
        List<TestObject> list = Arrays.asList(new TestObject(3, "c"), new TestObject(1, "a"), new TestObject(2, "b"));
        ToDoubleFunction<TestObject> extractor = obj -> (double) obj.getId();
        assertTrue(Iterables.minDouble((Iterator<TestObject>) null, extractor).isEmpty());
        assertTrue(Iterables.minDouble(Collections.<TestObject> emptyList().iterator(), extractor).isEmpty());
        assertEquals(1.0, Iterables.minDouble(list.iterator(), extractor).orElseThrow(), 0.0);
    }
    //endregion

    //region Max Methods
    @Test
    public void testMaxCharArray() {
        assertTrue(Iterables.max((char[]) null).isEmpty());
        assertTrue(Iterables.max(new char[0]).isEmpty());
        assertEquals('c', Iterables.max('c', 'a', 'b').orElseThrow());
    }

    @Test
    public void testMaxByteArray() {
        assertTrue(Iterables.max((byte[]) null).isEmpty());
        assertTrue(Iterables.max(new byte[0]).isEmpty());
        assertEquals((byte) 3, Iterables.max((byte) 3, (byte) 1, (byte) 2).orElseThrow());
    }

    @Test
    public void testMaxShortArray() {
        assertTrue(Iterables.max((short[]) null).isEmpty());
        assertTrue(Iterables.max(new short[0]).isEmpty());
        assertEquals((short) 3, Iterables.max((short) 3, (short) 1, (short) 2).orElseThrow());
    }

    @Test
    public void testMaxIntArray() {
        assertTrue(Iterables.max((int[]) null).isEmpty());
        assertTrue(Iterables.max(new int[0]).isEmpty());
        assertEquals(3, Iterables.max(3, 1, 2).orElseThrow());
    }

    @Test
    public void testMaxLongArray() {
        assertTrue(Iterables.max((long[]) null).isEmpty());
        assertTrue(Iterables.max(new long[0]).isEmpty());
        assertEquals(3L, Iterables.max(3L, 1L, 2L).orElseThrow());
    }

    @Test
    public void testMaxFloatArray() {
        assertTrue(Iterables.max((float[]) null).isEmpty());
        assertTrue(Iterables.max(new float[0]).isEmpty());
        assertEquals(3.0f, Iterables.max(3.0f, 1.0f, 2.0f).orElseThrow(), 0.0f);
    }

    @Test
    public void testMaxDoubleArray() {
        assertTrue(Iterables.max((double[]) null).isEmpty());
        assertTrue(Iterables.max(new double[0]).isEmpty());
        assertEquals(3.0, Iterables.max(3.0, 1.0, 2.0).orElseThrow(), 0.0);
    }

    @Test
    public void testMaxGenericArray() {
        assertTrue(Iterables.max((Integer[]) null).isEmpty());
        assertTrue(Iterables.max(new Integer[0]).isEmpty());
        assertEquals(3, Iterables.max(new Integer[] { 3, 1, 2 }).orElse(null));
        assertEquals(3, Iterables.max(new Integer[] { null, 3, 1, 2 }).orElse(null)); // nulls are min
    }

    @Test
    public void testMaxGenericArrayWithComparator() {
        assertTrue(Iterables.max((Integer[]) null, Comparator.naturalOrder()).isEmpty());
        assertTrue(Iterables.max(new Integer[0], Comparator.naturalOrder()).isEmpty());
        assertEquals(1, Iterables.max(new Integer[] { 3, 1, 2 }, REVERSE_ORDER_NULLS_FIRST).orElse(null));
        assertNull(Iterables.max(new Integer[] { null, 3, 1, 2 }, REVERSE_ORDER_NULLS_LAST).orElse(Integer.MAX_VALUE));
        assertEquals(1, Iterables.max(new Integer[] { null, 3, 1, 2 }, REVERSE_ORDER_NULLS_FIRST).orElse(null));
    }

    @Test
    public void testMaxIterable() {
        assertTrue(Iterables.max((Iterable<Integer>) null).isEmpty());
        assertTrue(Iterables.max(Collections.<Integer> emptyList()).isEmpty());
        assertEquals(3, Iterables.max(Arrays.asList(3, 1, 2)).orElse(null));
        assertEquals(3, Iterables.max(Arrays.asList(null, 3, 1, 2)).orElse(null));
    }

    @Test
    public void testMaxIterableWithComparator() {
        assertTrue(Iterables.max((Iterable<Integer>) null, Comparator.naturalOrder()).isEmpty());
        assertTrue(Iterables.max(Collections.<Integer> emptyList(), Comparator.naturalOrder()).isEmpty());
        assertEquals(1, Iterables.max(Arrays.asList(3, 1, 2), REVERSE_ORDER_NULLS_FIRST).orElse(null));
        assertNull(Iterables.max(Arrays.asList(null, 3, 1, 2), REVERSE_ORDER_NULLS_LAST).orElse(Integer.MAX_VALUE));
    }

    @Test
    public void testMaxIterator() {
        assertTrue(Iterables.max((Iterator<Integer>) null).isEmpty());
        assertTrue(Iterables.max(Collections.<Integer> emptyList().iterator()).isEmpty());
        assertEquals(3, Iterables.max(Arrays.asList(3, 1, 2).iterator()).orElse(null));
        assertEquals(3, Iterables.max(Arrays.asList(null, 3, 1, 2).iterator()).orElse(null));
    }

    @Test
    public void testMaxIteratorWithComparator() {
        assertTrue(Iterables.max((Iterator<Integer>) null, Comparator.naturalOrder()).isEmpty());
        assertTrue(Iterables.max(Collections.<Integer> emptyList().iterator(), Comparator.naturalOrder()).isEmpty());
        assertEquals(1, Iterables.max(Arrays.asList(3, 1, 2).iterator(), REVERSE_ORDER_NULLS_FIRST).orElse(null));
        assertNull(Iterables.max(Arrays.asList(null, 3, 1, 2).iterator(), REVERSE_ORDER_NULLS_LAST).orElse(Integer.MAX_VALUE));
        // Test specific null handling in max(Iterator, Comparator)
        assertNull(Iterables.max(Arrays.asList(null, 1, 2).iterator(), Comparator.nullsLast(Integer::compareTo)).get());
        assertEquals(2, Iterables.max(Arrays.asList(1, null, 2).iterator(), Comparator.nullsFirst(Integer::compareTo)).get());
    }

    @Test
    public void testMaxByArray() {
        TestObject[] arr = { new TestObject(3, "ccc"), new TestObject(1, "a"), new TestObject(2, "bb") };
        assertTrue(Iterables.maxBy((TestObject[]) null, TestObject::getId).isEmpty());
        assertTrue(Iterables.maxBy(new TestObject[0], TestObject::getId).isEmpty());
        assertEquals(arr[0], Iterables.maxBy(arr, TestObject::getId).orElse(null));
        assertEquals(arr[0], Iterables.maxBy(arr, TestObject::getValue).orElse(null)); // "ccc" is longest
    }

    @Test
    public void testMaxByIterable() {
        List<TestObject> list = Arrays.asList(new TestObject(3, "ccc"), new TestObject(1, "a"), new TestObject(2, "bb"));
        assertTrue(Iterables.maxBy((List<TestObject>) null, TestObject::getId).isEmpty());
        assertTrue(Iterables.maxBy(Collections.<TestObject> emptyList(), TestObject::getId).isEmpty());
        assertEquals(list.get(0), Iterables.maxBy(list, TestObject::getId).orElse(null));
    }

    @Test
    public void testMaxByIterator() {
        List<TestObject> list = Arrays.asList(new TestObject(3, "ccc"), new TestObject(1, "a"), new TestObject(2, "bb"));
        assertTrue(Iterables.maxBy((Iterator<TestObject>) null, TestObject::getId).isEmpty());
        assertTrue(Iterables.maxBy(Collections.<TestObject> emptyList().iterator(), TestObject::getId).isEmpty());
        assertEquals(list.get(0), Iterables.maxBy(list.iterator(), TestObject::getId).orElse(null));
    }

    @Test
    public void testMaxIntArrayWithValueExtractor() {
        TestObject[] arr = { new TestObject(3, "c"), new TestObject(1, "a"), new TestObject(2, "b") };
        assertTrue(Iterables.maxInt((TestObject[]) null, TestObject::getId).isEmpty());
        assertTrue(Iterables.maxInt(new TestObject[0], TestObject::getId).isEmpty());
        assertEquals(3, Iterables.maxInt(arr, TestObject::getId).orElseThrow());
    }

    @Test
    public void testMaxIntIterableWithValueExtractor() {
        List<TestObject> list = Arrays.asList(new TestObject(3, "c"), new TestObject(1, "a"), new TestObject(2, "b"));
        assertTrue(Iterables.maxInt((List<TestObject>) null, TestObject::getId).isEmpty());
        assertTrue(Iterables.maxInt(Collections.<TestObject> emptyList(), TestObject::getId).isEmpty());
        assertEquals(3, Iterables.maxInt(list, TestObject::getId).orElseThrow());
    }

    @Test
    public void testMaxIntIteratorWithValueExtractor() {
        List<TestObject> list = Arrays.asList(new TestObject(3, "c"), new TestObject(1, "a"), new TestObject(2, "b"));
        assertTrue(Iterables.maxInt((Iterator<TestObject>) null, TestObject::getId).isEmpty());
        assertTrue(Iterables.maxInt(Collections.<TestObject> emptyList().iterator(), TestObject::getId).isEmpty());
        assertEquals(3, Iterables.maxInt(list.iterator(), TestObject::getId).orElseThrow());
    }

    @Test
    public void testMaxLongArrayWithValueExtractor() {
        TestObject[] arr = { new TestObject(3, "c"), new TestObject(1, "a"), new TestObject(2, "b") };
        ToLongFunction<TestObject> extractor = obj -> (long) obj.getId();
        assertTrue(Iterables.maxLong((TestObject[]) null, extractor).isEmpty());
        assertTrue(Iterables.maxLong(new TestObject[0], extractor).isEmpty());
        assertEquals(3L, Iterables.maxLong(arr, extractor).orElseThrow());
    }

    @Test
    public void testMaxLongIterableWithValueExtractor() {
        List<TestObject> list = Arrays.asList(new TestObject(3, "c"), new TestObject(1, "a"), new TestObject(2, "b"));
        ToLongFunction<TestObject> extractor = obj -> (long) obj.getId();
        assertTrue(Iterables.maxLong((List<TestObject>) null, extractor).isEmpty());
        assertTrue(Iterables.maxLong(Collections.emptyList(), extractor).isEmpty());
        assertEquals(3L, Iterables.maxLong(list, extractor).orElseThrow());
    }

    @Test
    public void testMaxLongIteratorWithValueExtractor() {
        List<TestObject> list = Arrays.asList(new TestObject(3, "c"), new TestObject(1, "a"), new TestObject(2, "b"));
        ToLongFunction<TestObject> extractor = obj -> (long) obj.getId();
        assertTrue(Iterables.maxLong((Iterator<TestObject>) null, extractor).isEmpty());
        assertTrue(Iterables.maxLong(Collections.<TestObject> emptyList().iterator(), extractor).isEmpty());
        assertEquals(3L, Iterables.maxLong(list.iterator(), extractor).orElseThrow());
    }

    @Test
    public void testMaxDoubleArrayWithValueExtractor() {
        TestObject[] arr = { new TestObject(3, "c"), new TestObject(1, "a"), new TestObject(2, "b") };
        ToDoubleFunction<TestObject> extractor = obj -> (double) obj.getId();
        assertTrue(Iterables.maxDouble((TestObject[]) null, extractor).isEmpty());
        assertTrue(Iterables.maxDouble(new TestObject[0], extractor).isEmpty());
        assertEquals(3.0, Iterables.maxDouble(arr, extractor).orElseThrow(), 0.0);
    }

    @Test
    public void testMaxDoubleIterableWithValueExtractor() {
        List<TestObject> list = Arrays.asList(new TestObject(3, "c"), new TestObject(1, "a"), new TestObject(2, "b"));
        ToDoubleFunction<TestObject> extractor = obj -> (double) obj.getId();
        assertTrue(Iterables.maxDouble((List<TestObject>) null, extractor).isEmpty());
        assertTrue(Iterables.maxDouble(Collections.emptyList(), extractor).isEmpty());
        assertEquals(3.0, Iterables.maxDouble(list, extractor).orElseThrow(), 0.0);
    }

    @Test
    public void testMaxDoubleIteratorWithValueExtractor() {
        List<TestObject> list = Arrays.asList(new TestObject(3, "c"), new TestObject(1, "a"), new TestObject(2, "b"));
        ToDoubleFunction<TestObject> extractor = obj -> (double) obj.getId();
        assertTrue(Iterables.maxDouble((Iterator<TestObject>) null, extractor).isEmpty());
        assertTrue(Iterables.maxDouble(Collections.<TestObject> emptyList().iterator(), extractor).isEmpty());
        assertEquals(3.0, Iterables.maxDouble(list.iterator(), extractor).orElseThrow(), 0.0);
    }

    //endregion

    //region MinMax Methods
    @Test
    public void testMinMaxArray() {
        assertTrue(Iterables.minMax((Integer[]) null).isEmpty());
        assertTrue(Iterables.minMax(new Integer[0]).isEmpty());
        Pair<Integer, Integer> result = Iterables.minMax(new Integer[] { 3, 1, 5, 2, 4 }).orElse(null);
        assertNotNull(result);
        assertEquals(1, result.left());
        assertEquals(5, result.right());

        result = Iterables.minMax(new Integer[] { null, 3, 1, 5, null, 2, 4 }).orElse(null); // nulls ignored by N.minMax by default for Comparable
        assertNotNull(result);
        assertEquals(null, result.left());
        assertEquals(5, result.right());
    }

    @Test
    public void testMinMaxArrayWithComparator() {
        assertTrue(Iterables.minMax((Integer[]) null, Comparator.naturalOrder()).isEmpty());
        assertTrue(Iterables.minMax(new Integer[0], Comparator.naturalOrder()).isEmpty());

        Pair<Integer, Integer> result = Iterables.minMax(new Integer[] { 3, 1, 5, 2, 4 }, REVERSE_ORDER_NULLS_FIRST).orElse(null);
        assertNotNull(result);
        assertEquals(5, result.left()); // Min in reverse order
        assertEquals(1, result.right()); // Max in reverse order

        result = Iterables.minMax(new Integer[] { null, 3, 1, 5, null, 2, 4 }, Comparator.nullsFirst(Comparator.naturalOrder())).orElse(null);
        assertNotNull(result);
        assertNull(result.left());
        assertEquals(5, result.right());

        result = Iterables.minMax(new Integer[] { null, 3, 1, 5, null, 2, 4 }, Comparator.nullsLast(Comparator.naturalOrder())).orElse(null);
        assertNotNull(result);
        assertEquals(1, result.left());
        assertNull(result.right());
    }

    @Test
    public void testMinMaxIterable() {
        assertTrue(Iterables.minMax((Iterable<Integer>) null).isEmpty());
        assertTrue(Iterables.minMax(Collections.<Integer> emptyList()).isEmpty());
        Pair<Integer, Integer> result = Iterables.minMax(Arrays.asList(3, 1, 5, 2, 4)).orElse(null);
        assertNotNull(result);
        assertEquals(1, result.left());
        assertEquals(5, result.right());
    }

    @Test
    public void testMinMaxIterableWithComparator() {
        assertTrue(Iterables.minMax((Iterable<Integer>) null, Comparator.naturalOrder()).isEmpty());
        assertTrue(Iterables.minMax(Collections.<Integer> emptyList(), Comparator.naturalOrder()).isEmpty());
        Pair<Integer, Integer> result = Iterables.minMax(Arrays.asList(3, 1, 5, 2, 4), REVERSE_ORDER_NULLS_FIRST).orElse(null);
        assertNotNull(result);
        assertEquals(5, result.left());
        assertEquals(1, result.right());
    }

    @Test
    public void testMinMaxIterator() {
        assertTrue(Iterables.minMax((Iterator<Integer>) null).isEmpty());
        assertTrue(Iterables.minMax(Collections.<Integer> emptyList().iterator()).isEmpty());
        Pair<Integer, Integer> result = Iterables.minMax(Arrays.asList(3, 1, 5, 2, 4).iterator()).orElse(null);
        assertNotNull(result);
        assertEquals(1, result.left());
        assertEquals(5, result.right());
    }

    @Test
    public void testMinMaxIteratorWithComparator() {
        assertTrue(Iterables.minMax((Iterator<Integer>) null, Comparator.naturalOrder()).isEmpty());
        assertTrue(Iterables.minMax(Collections.<Integer> emptyList().iterator(), Comparator.naturalOrder()).isEmpty());
        Pair<Integer, Integer> result = Iterables.minMax(Arrays.asList(3, 1, 5, 2, 4).iterator(), REVERSE_ORDER_NULLS_FIRST).orElse(null);
        assertNotNull(result);
        assertEquals(5, result.left());
        assertEquals(1, result.right());
    }
    //endregion

    //region Median and KthLargest Methods
    @Test
    public void testMedianArray() {
        assertTrue(Iterables.median((Integer[]) null).isEmpty());
        assertTrue(Iterables.median(new Integer[0]).isEmpty());
        assertEquals(3, Iterables.median(new Integer[] { 1, 5, 3, 2, 4 }).orElse(null)); // 1,2,3,4,5 -> 3
        assertEquals(3, Iterables.median(new Integer[] { 1, 5, 3, 2, 4, 6 }).orElse(null)); // 1,2,3,4,5,6 -> 3 or 4 (length/2 +1 -> 4th largest which is 3 by N.median def)
                                                                                            // N.median returns element at index length / 2 after sorting. (0-indexed)
                                                                                            // For {1,2,3,4,5,6}, sorted is {1,2,3,4,5,6}, length is 6, length/2 is 3. Element at index 3 is 4.
                                                                                            // The Javadoc says "length / 2 + 1 largest value", which means for length 6 -> 4th largest. (1-based)
                                                                                            // If sorted: 1,2,3,4,5,6. 1st largest=6, 2nd=5, 3rd=4, 4th=3.
                                                                                            // Let's recheck N.median logic or example
                                                                                            // N.median -> sorts and returns a[a.length / 2]
                                                                                            // for {1,2,3,4,5,6}, sorted is [1,2,3,4,5,6], a.length/2 = 3, a[3] = 4.
                                                                                            // for {1,2,3,4,5}, sorted is [1,2,3,4,5], a.length/2 = 2, a[2] = 3.
        assertEquals(3, Iterables.median(new Integer[] { 1, 5, 3, 2, 4, 6 }).orElse(null));
    }

    @Test
    public void testMedianArrayWithComparator() {
        assertTrue(Iterables.median((Integer[]) null, REVERSE_ORDER_NULLS_FIRST).isEmpty());
        assertTrue(Iterables.median(new Integer[0], REVERSE_ORDER_NULLS_FIRST).isEmpty());
        // Array: {1, 5, 3, 2, 4} -> Sorted by REVERSE: {5, 4, 3, 2, 1} -> Median (index 2): 3
        assertEquals(3, Iterables.median(new Integer[] { 1, 5, 3, 2, 4 }, REVERSE_ORDER_NULLS_FIRST).orElse(null));
        // Array: {1, 5, 3, 2, 4, 6} -> Sorted by REVERSE: {6, 5, 4, 3, 2, 1} -> Median (index 3): 3
        assertEquals(4, Iterables.median(new Integer[] { 1, 5, 3, 2, 4, 6 }, REVERSE_ORDER_NULLS_FIRST).orElse(null));
    }

    @Test
    public void testMedianCollection() {
        assertTrue(Iterables.median((Collection<Integer>) null).isEmpty());
        assertTrue(Iterables.median(Collections.<Integer> emptyList()).isEmpty());
        assertEquals(3, Iterables.median(Arrays.asList(1, 5, 3, 2, 4)).orElse(null));
        assertEquals(3, Iterables.median(Arrays.asList(1, 5, 3, 2, 4, 6)).orElse(null));
    }

    @Test
    public void testMedianCollectionWithComparator() {
        assertTrue(Iterables.median((Collection<Integer>) null, REVERSE_ORDER_NULLS_FIRST).isEmpty());
        assertTrue(Iterables.median(Collections.<Integer> emptyList(), REVERSE_ORDER_NULLS_FIRST).isEmpty());
        assertEquals(3, Iterables.median(Arrays.asList(1, 5, 3, 2, 4), REVERSE_ORDER_NULLS_FIRST).orElse(null));
        assertEquals(4, Iterables.median(Arrays.asList(1, 5, 3, 2, 4, 6), REVERSE_ORDER_NULLS_FIRST).orElse(null));

    }

    @Test
    public void testKthLargestArray() {
        assertTrue(Iterables.kthLargest((Integer[]) null, 1).isEmpty());
        assertTrue(Iterables.kthLargest(new Integer[0], 1).isEmpty());
        assertTrue(Iterables.kthLargest(new Integer[] { 1, 2 }, 3).isEmpty()); // k > length
        assertEquals(5, Iterables.kthLargest(new Integer[] { 1, 5, 3, 2, 4 }, 1).orElse(null)); // 1st largest
        assertEquals(4, Iterables.kthLargest(new Integer[] { 1, 5, 3, 2, 4 }, 2).orElse(null)); // 2nd largest
        assertEquals(1, Iterables.kthLargest(new Integer[] { 1, 5, 3, 2, 4 }, 5).orElse(null)); // 5th largest
    }

    @Test
    public void testKthLargestArrayWithComparator() {
        Integer[] arr = { 1, 5, 3, 2, 4 }; // Sorted reverse: 5,4,3,2,1
        assertTrue(Iterables.kthLargest((Integer[]) null, 1, REVERSE_ORDER_NULLS_FIRST).isEmpty());
        assertTrue(Iterables.kthLargest(new Integer[0], 1, REVERSE_ORDER_NULLS_FIRST).isEmpty());
        assertTrue(Iterables.kthLargest(arr, 6, REVERSE_ORDER_NULLS_FIRST).isEmpty());
        assertEquals(1, Iterables.kthLargest(arr, 1, REVERSE_ORDER_NULLS_FIRST).orElse(null)); // 1st largest in reverse sort (smallest)
        assertEquals(2, Iterables.kthLargest(arr, 2, REVERSE_ORDER_NULLS_FIRST).orElse(null)); // 2nd largest in reverse sort
    }

    @Test
    public void testKthLargestCollection() {
        List<Integer> list = Arrays.asList(1, 5, 3, 2, 4);
        assertTrue(Iterables.kthLargest((Collection<Integer>) null, 1).isEmpty());
        assertTrue(Iterables.kthLargest(Collections.<Integer> emptyList(), 1).isEmpty());
        assertTrue(Iterables.kthLargest(list, 6).isEmpty());
        assertEquals(5, Iterables.kthLargest(list, 1).orElse(null));
        assertEquals(4, Iterables.kthLargest(list, 2).orElse(null));
    }

    @Test
    public void testKthLargestCollectionWithComparator() {
        {
            List<Integer> list = Arrays.asList(1, 5, 3, 2, 4);
            assertTrue(Iterables.kthLargest((Collection<Integer>) null, 1, REVERSE_ORDER_NULLS_FIRST).isEmpty());
            assertTrue(Iterables.kthLargest(Collections.<Integer> emptyList(), 1, REVERSE_ORDER_NULLS_FIRST).isEmpty());
            assertTrue(Iterables.kthLargest(list, 6, REVERSE_ORDER_NULLS_FIRST).isEmpty());
            assertEquals(1, Iterables.kthLargest(list, 1, REVERSE_ORDER_NULLS_FIRST).orElse(null));
            assertEquals(2, Iterables.kthLargest(list, 2, REVERSE_ORDER_NULLS_FIRST).orElse(null));
            assertEquals(3, Iterables.kthLargest(list, 3, REVERSE_ORDER_NULLS_FIRST).orElse(null));
            assertEquals(4, Iterables.kthLargest(list, 4, REVERSE_ORDER_NULLS_FIRST).orElse(null));
            assertEquals(5, Iterables.kthLargest(list, 5, REVERSE_ORDER_NULLS_FIRST).orElse(null));
        }
        {
            List<Integer> list = Arrays.asList(1, 5, 3, 2, 6, 4);
            assertTrue(Iterables.kthLargest((Collection<Integer>) null, 1, REVERSE_ORDER_NULLS_FIRST).isEmpty());
            assertTrue(Iterables.kthLargest(Collections.<Integer> emptyList(), 1, REVERSE_ORDER_NULLS_FIRST).isEmpty());
            assertTrue(Iterables.kthLargest(list, 7, REVERSE_ORDER_NULLS_FIRST).isEmpty());
            assertEquals(1, Iterables.kthLargest(list, 1, REVERSE_ORDER_NULLS_FIRST).orElse(null));
            assertEquals(2, Iterables.kthLargest(list, 2, REVERSE_ORDER_NULLS_FIRST).orElse(null));
            assertEquals(3, Iterables.kthLargest(list, 3, REVERSE_ORDER_NULLS_FIRST).orElse(null));
            assertEquals(4, Iterables.kthLargest(list, 4, REVERSE_ORDER_NULLS_FIRST).orElse(null));
            assertEquals(5, Iterables.kthLargest(list, 5, REVERSE_ORDER_NULLS_FIRST).orElse(null));
            assertEquals(6, Iterables.kthLargest(list, 6, REVERSE_ORDER_NULLS_FIRST).orElse(null));
        }
    }
    //endregion

    //region Sum Methods
    @Test
    public void testSumIntIterable() {
        assertTrue(Iterables.sumInt((Iterable<Integer>) null).isEmpty());
        assertTrue(Iterables.sumInt(Collections.<Integer> emptyList()).isEmpty());
        assertEquals(15, Iterables.sumInt(Arrays.asList(1, 2, 3, 4, 5)).orElseThrow());
    }

    @Test
    public void testSumIntIterableWithFunction() {
        List<TestObject> list = Arrays.asList(new TestObject(1, "a"), new TestObject(2, "b"));
        assertTrue(Iterables.sumInt((Iterable<TestObject>) null, TestObject::getId).isEmpty());
        assertTrue(Iterables.sumInt(Collections.<TestObject> emptyList(), TestObject::getId).isEmpty());
        assertEquals(3, Iterables.sumInt(list, TestObject::getId).orElseThrow());
    }

    @Test
    public void testSumIntToLongIterable() {
        assertTrue(Iterables.sumIntToLong((Iterable<Integer>) null).isEmpty());
        assertTrue(Iterables.sumIntToLong(Collections.<Integer> emptyList()).isEmpty());
        assertEquals(15L, Iterables.sumIntToLong(Arrays.asList(1, 2, 3, 4, 5)).orElseThrow());
        assertEquals((long) Integer.MAX_VALUE + 1L, Iterables.sumIntToLong(Arrays.asList(Integer.MAX_VALUE, 1)).orElseThrow());
    }

    @Test
    public void testSumIntToLongIterableWithFunction() {
        List<TestObject> list = Arrays.asList(new TestObject(Integer.MAX_VALUE, "a"), new TestObject(1, "b"));
        assertTrue(Iterables.sumIntToLong((Iterable<TestObject>) null, TestObject::getId).isEmpty());
        assertTrue(Iterables.sumIntToLong(Collections.<TestObject> emptyList(), TestObject::getId).isEmpty());
        assertEquals((long) Integer.MAX_VALUE + 1L, Iterables.sumIntToLong(list, TestObject::getId).orElseThrow());
    }

    @Test
    public void testSumLongIterable() {
        assertTrue(Iterables.sumLong((Iterable<Long>) null).isEmpty());
        assertTrue(Iterables.sumLong(Collections.<Long> emptyList()).isEmpty());
        assertEquals(15L, Iterables.sumLong(Arrays.asList(1L, 2L, 3L, 4L, 5L)).orElseThrow());
    }

    @Test
    public void testSumLongIterableWithFunction() {
        List<TestObject> list = Arrays.asList(new TestObject(1, "a"), new TestObject(2, "b"));
        ToLongFunction<TestObject> extractor = obj -> (long) obj.getId();
        assertTrue(Iterables.sumLong((Iterable<TestObject>) null, extractor).isEmpty());
        assertTrue(Iterables.sumLong(Collections.<TestObject> emptyList(), extractor).isEmpty());
        assertEquals(3L, Iterables.sumLong(list, extractor).orElseThrow());
    }

    @Test
    public void testSumDoubleIterable() {
        assertTrue(Iterables.sumDouble((Iterable<Double>) null).isEmpty());
        assertTrue(Iterables.sumDouble(Collections.<Double> emptyList()).isEmpty());
        assertEquals(15.5, Iterables.sumDouble(Arrays.asList(1.0, 2.5, 3.0, 4.0, 5.0)).orElseThrow(), 0.0);
    }

    @Test
    public void testSumDoubleIterableWithFunction() {
        List<TestObject> list = Arrays.asList(new TestObject(1, "a"), new TestObject(2, "b"));
        ToDoubleFunction<TestObject> extractor = obj -> (double) obj.getId() + 0.5; // 1.5, 2.5
        assertTrue(Iterables.sumDouble((Iterable<TestObject>) null, extractor).isEmpty());
        assertTrue(Iterables.sumDouble(Collections.<TestObject> emptyList(), extractor).isEmpty());
        assertEquals(4.0, Iterables.sumDouble(list, extractor).orElseThrow(), 0.0);
    }

    @Test
    public void testSumBigIntegerIterable() {
        assertTrue(Iterables.sumBigInteger((Iterable<BigInteger>) null).isEmpty());
        assertTrue(Iterables.sumBigInteger(Collections.<BigInteger> emptyList()).isEmpty());
        List<BigInteger> biList = Arrays.asList(BigInteger.ONE, BigInteger.TWO, BigInteger.TEN);
        assertEquals(new BigInteger("13"), Iterables.sumBigInteger(biList).orElse(null));
    }

    @Test
    public void testSumBigIntegerIterableWithFunction() {
        List<String> list = Arrays.asList("1", "2", "10");
        Function<String, BigInteger> extractor = BigInteger::new;
        assertTrue(Iterables.sumBigInteger((Iterable<String>) null, extractor).isEmpty());
        assertTrue(Iterables.sumBigInteger(Collections.<String> emptyList(), extractor).isEmpty());
        assertEquals(new BigInteger("13"), Iterables.sumBigInteger(list, extractor).orElse(null));
    }

    @Test
    public void testSumBigDecimalIterable() {
        assertTrue(Iterables.sumBigDecimal((Iterable<BigDecimal>) null).isEmpty());
        assertTrue(Iterables.sumBigDecimal(Collections.<BigDecimal> emptyList()).isEmpty());
        List<BigDecimal> bdList = Arrays.asList(new BigDecimal("1.5"), new BigDecimal("2.5"), new BigDecimal("10.0"));
        assertEquals(new BigDecimal("14.0"), Iterables.sumBigDecimal(bdList).orElse(null));
    }

    @Test
    public void testSumBigDecimalIterableWithFunction() {
        List<String> list = Arrays.asList("1.5", "2.5", "10.0");
        Function<String, BigDecimal> extractor = BigDecimal::new;
        assertTrue(Iterables.sumBigDecimal((Iterable<String>) null, extractor).isEmpty());
        assertTrue(Iterables.sumBigDecimal(Collections.<String> emptyList(), extractor).isEmpty());
        assertEquals(new BigDecimal("14.0"), Iterables.sumBigDecimal(list, extractor).orElse(null));
    }
    //endregion

    //region Average Methods
    @Test
    public void testAverageIntArray() {
        assertTrue(Iterables.averageInt((Number[]) null).isEmpty());
        assertTrue(Iterables.averageInt(new Number[0]).isEmpty());
        assertEquals(3.0, Iterables.averageInt(new Integer[] { 1, 2, 3, 4, 5 }).orElseThrow(), 0.0);
    }

    @Test
    public void testAverageIntArrayFromTo() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        assertTrue(Iterables.averageInt(arr, 1, 1).isEmpty()); // Empty range
        assertEquals(3.0, Iterables.averageInt(arr, 1, 4).orElseThrow(), 0.0); // avg(2,3,4)
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, 3, 2));
    }

    @Test
    public void testAverageIntArrayWithFunction() {
        TestObject[] arr = { new TestObject(1, ""), new TestObject(2, ""), new TestObject(3, "") };
        assertTrue(Iterables.averageInt((TestObject[]) null, TestObject::getId).isEmpty());
        assertTrue(Iterables.averageInt(new TestObject[0], TestObject::getId).isEmpty());
        assertEquals(2.0, Iterables.averageInt(arr, TestObject::getId).orElseThrow(), 0.0);
    }

    @Test
    public void testAverageIntArrayFromToWithFunction() {
        TestObject[] arr = { new TestObject(1, ""), new TestObject(2, ""), new TestObject(3, ""), new TestObject(4, ""), new TestObject(5, "") };
        ToIntFunction<TestObject> extractor = TestObject::getId;
        assertTrue(Iterables.averageInt(arr, 1, 1, extractor).isEmpty());
        assertEquals(3.0, Iterables.averageInt(arr, 1, 4, extractor).orElseThrow(), 0.0);
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, -1, 2, extractor));
    }

    @Test
    public void testAverageIntCollectionFromTo() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        assertTrue(Iterables.averageInt(list, 1, 1).isEmpty());
        assertEquals(3.0, Iterables.averageInt(list, 1, 4).orElseThrow(), 0.0);
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(list, 0, 6));
    }

    @Test
    public void testAverageIntCollectionFromToWithFunction() {
        List<TestObject> list = Arrays.asList(new TestObject(1, ""), new TestObject(2, ""), new TestObject(3, ""), new TestObject(4, ""),
                new TestObject(5, ""));
        ToIntFunction<TestObject> extractor = TestObject::getId;
        assertTrue(Iterables.averageInt(list, 1, 1, extractor).isEmpty());
        assertEquals(3.0, Iterables.averageInt(list, 1, 4, extractor).orElseThrow(), 0.0);
    }

    @Test
    public void testAverageIntIterable() {
        assertTrue(Iterables.averageInt((Iterable<Integer>) null).isEmpty());
        assertTrue(Iterables.averageInt(Collections.<Integer> emptyList()).isEmpty());
        assertEquals(3.0, Iterables.averageInt(Arrays.asList(1, 2, 3, 4, 5)).orElseThrow(), 0.0);
    }

    @Test
    public void testAverageIntIterableWithFunction() {
        List<TestObject> list = Arrays.asList(new TestObject(1, ""), new TestObject(2, ""), new TestObject(3, ""));
        assertTrue(Iterables.averageInt((Iterable<TestObject>) null, TestObject::getId).isEmpty());
        assertTrue(Iterables.averageInt(Collections.<TestObject> emptyList(), TestObject::getId).isEmpty());
        assertEquals(2.0, Iterables.averageInt(list, TestObject::getId).orElseThrow(), 0.0);
    }

    @Test
    public void testAverageLongArray() {
        assertTrue(Iterables.averageLong((Number[]) null).isEmpty());
        assertTrue(Iterables.averageLong(new Number[0]).isEmpty());
        assertEquals(3.0, Iterables.averageLong(new Long[] { 1L, 2L, 3L, 4L, 5L }).orElseThrow(), 0.0);
    }

    @Test
    public void testAverageLongArrayFromTo() {
        Long[] arr = { 1L, 2L, 3L, 4L, 5L };
        assertTrue(Iterables.averageLong(arr, 1, 1).isEmpty()); // Empty range
        assertEquals(3.0, Iterables.averageLong(arr, 1, 4).orElseThrow(), 0.0); // avg(2,3,4)
    }

    @Test
    public void testAverageLongArrayWithFunction() {
        TestObject[] arr = { new TestObject(1, ""), new TestObject(2, ""), new TestObject(3, "") };
        ToLongFunction<TestObject> extractor = obj -> (long) obj.getId();
        assertTrue(Iterables.averageLong((TestObject[]) null, extractor).isEmpty());
        assertTrue(Iterables.averageLong(new TestObject[0], extractor).isEmpty());
        assertEquals(2.0, Iterables.averageLong(arr, extractor).orElseThrow(), 0.0);
    }

    @Test
    public void testAverageLongArrayFromToWithFunction() {
        TestObject[] arr = { new TestObject(1, ""), new TestObject(2, ""), new TestObject(3, ""), new TestObject(4, ""), new TestObject(5, "") };
        ToLongFunction<TestObject> extractor = obj -> (long) obj.getId();
        assertTrue(Iterables.averageLong(arr, 1, 1, extractor).isEmpty());
        assertEquals(3.0, Iterables.averageLong(arr, 1, 4, extractor).orElseThrow(), 0.0);
    }

    @Test
    public void testAverageLongCollectionFromTo() {
        List<Long> list = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        assertTrue(Iterables.averageLong(list, 1, 1).isEmpty());
        assertEquals(3.0, Iterables.averageLong(list, 1, 4).orElseThrow(), 0.0);
    }

    @Test
    public void testAverageLongCollectionFromToWithFunction() {
        List<TestObject> list = Arrays.asList(new TestObject(1, ""), new TestObject(2, ""), new TestObject(3, ""), new TestObject(4, ""),
                new TestObject(5, ""));
        ToLongFunction<TestObject> extractor = obj -> (long) obj.getId();
        assertTrue(Iterables.averageLong(list, 1, 1, extractor).isEmpty());
        assertEquals(3.0, Iterables.averageLong(list, 1, 4, extractor).orElseThrow(), 0.0);
    }

    @Test
    public void testAverageLongIterable() {
        assertTrue(Iterables.averageLong((Iterable<Long>) null).isEmpty());
        assertTrue(Iterables.averageLong(Collections.<Long> emptyList()).isEmpty());
        assertEquals(3.0, Iterables.averageLong(Arrays.asList(1L, 2L, 3L, 4L, 5L)).orElseThrow(), 0.0);
    }

    @Test
    public void testAverageLongIterableWithFunction() {
        List<TestObject> list = Arrays.asList(new TestObject(1, ""), new TestObject(2, ""), new TestObject(3, ""));
        ToLongFunction<TestObject> extractor = obj -> (long) obj.getId();
        assertTrue(Iterables.averageLong((Iterable<TestObject>) null, extractor).isEmpty());
        assertTrue(Iterables.averageLong(Collections.<TestObject> emptyList(), extractor).isEmpty());
        assertEquals(2.0, Iterables.averageLong(list, extractor).orElseThrow(), 0.0);
    }

    @Test
    public void testAverageDoubleArray() {
        assertTrue(Iterables.averageDouble((Number[]) null).isEmpty());
        assertTrue(Iterables.averageDouble(new Number[0]).isEmpty());
        assertEquals(3.3, Iterables.averageDouble(new Double[] { 1.1, 2.2, 3.3, 4.4, 5.5 }).orElseThrow(), 0.00001);
    }

    @Test
    public void testAverageDoubleArrayFromTo() {
        Double[] arr = { 1.1, 2.2, 3.3, 4.4, 5.5 };
        assertTrue(Iterables.averageDouble(arr, 1, 1).isEmpty());
        assertEquals(3.3, Iterables.averageDouble(arr, 1, 4).orElseThrow(), 0.00001); // avg(2.2, 3.3, 4.4)
    }

    @Test
    public void testAverageDoubleArrayWithFunction() {
        TestObject[] arr = { new TestObject(1, ""), new TestObject(2, ""), new TestObject(3, "") };
        ToDoubleFunction<TestObject> extractor = obj -> obj.getId() + 0.1; // 1.1, 2.1, 3.1
        assertTrue(Iterables.averageDouble((TestObject[]) null, extractor).isEmpty());
        assertTrue(Iterables.averageDouble(new TestObject[0], extractor).isEmpty());
        assertEquals(2.1, Iterables.averageDouble(arr, extractor).orElseThrow(), 0.00001);
    }

    @Test
    public void testAverageDoubleArrayFromToWithFunction() {
        TestObject[] arr = { new TestObject(1, ""), new TestObject(2, ""), new TestObject(3, ""), new TestObject(4, ""), new TestObject(5, "") };
        ToDoubleFunction<TestObject> extractor = obj -> obj.getId() + 0.1;
        assertTrue(Iterables.averageDouble(arr, 1, 1, extractor).isEmpty());
        assertEquals(3.1, Iterables.averageDouble(arr, 1, 4, extractor).orElseThrow(), 0.00001); // 2.1, 3.1, 4.1 -> avg = 3.1
    }

    @Test
    public void testAverageDoubleCollectionFromTo() {
        List<Double> list = Arrays.asList(1.1, 2.2, 3.3, 4.4, 5.5);
        assertTrue(Iterables.averageDouble(list, 1, 1).isEmpty());
        assertEquals(3.3, Iterables.averageDouble(list, 1, 4).orElseThrow(), 0.00001);
    }

    @Test
    public void testAverageDoubleCollectionFromToWithFunction() {
        List<TestObject> list = Arrays.asList(new TestObject(1, ""), new TestObject(2, ""), new TestObject(3, ""), new TestObject(4, ""),
                new TestObject(5, ""));
        ToDoubleFunction<TestObject> extractor = obj -> obj.getId() + 0.1;
        assertTrue(Iterables.averageDouble(list, 1, 1, extractor).isEmpty());
        assertEquals(3.1, Iterables.averageDouble(list, 1, 4, extractor).orElseThrow(), 0.00001);
    }

    @Test
    public void testAverageDoubleIterable() {
        assertTrue(Iterables.averageDouble((Iterable<Double>) null).isEmpty());
        assertTrue(Iterables.averageDouble(Collections.<Double> emptyList()).isEmpty());
        assertEquals(3.3, Iterables.averageDouble(Arrays.asList(1.1, 2.2, 3.3, 4.4, 5.5)).orElseThrow(), 0.00001);
    }

    @Test
    public void testAverageDoubleIterableWithFunction() {
        List<TestObject> list = Arrays.asList(new TestObject(1, ""), new TestObject(2, ""), new TestObject(3, ""));
        ToDoubleFunction<TestObject> extractor = obj -> obj.getId() + 0.1;
        assertTrue(Iterables.averageDouble((Iterable<TestObject>) null, extractor).isEmpty());
        assertTrue(Iterables.averageDouble(Collections.<TestObject> emptyList(), extractor).isEmpty());
        assertEquals(2.1, Iterables.averageDouble(list, extractor).orElseThrow(), 0.00001);
    }

    @Test
    public void testAverageBigIntegerIterable() {
        assertTrue(Iterables.averageBigInteger((Iterable<BigInteger>) null).isEmpty());
        assertTrue(Iterables.averageBigInteger(Collections.<BigInteger> emptyList()).isEmpty());
        List<BigInteger> biList = Arrays.asList(BigInteger.ONE, BigInteger.TWO, BigInteger.valueOf(3)); // Sum=6, Count=3
        assertEquals(new BigDecimal("2"), Iterables.averageBigInteger(biList).orElse(null));

        List<BigInteger> biList2 = Arrays.asList(BigInteger.ONE, BigInteger.TWO); // Sum=3, Count=2
        assertEquals(new BigDecimal("1.5"), Iterables.averageBigInteger(biList2).orElse(null));
    }

    @Test
    public void testAverageBigIntegerIterableWithFunction() {
        List<String> list = Arrays.asList("1", "2", "3");
        Function<String, BigInteger> extractor = BigInteger::new;
        assertTrue(Iterables.averageBigInteger((Iterable<String>) null, extractor).isEmpty());
        assertTrue(Iterables.averageBigInteger(Collections.<String> emptyList(), extractor).isEmpty());
        assertEquals(new BigDecimal("2"), Iterables.averageBigInteger(list, extractor).orElse(null));
    }

    @Test
    public void testAverageBigDecimalIterable() {
        assertTrue(Iterables.averageBigDecimal((Iterable<BigDecimal>) null).isEmpty());
        assertTrue(Iterables.averageBigDecimal(Collections.<BigDecimal> emptyList()).isEmpty());
        List<BigDecimal> bdList = Arrays.asList(new BigDecimal("1.5"), new BigDecimal("2.5"), new BigDecimal("3.5")); // Sum=7.5, Count=3
        assertEquals(new BigDecimal("2.5"), Iterables.averageBigDecimal(bdList).orElse(null));
    }

    @Test
    public void testAverageBigDecimalIterableWithFunction() {
        List<String> list = Arrays.asList("1.5", "2.5", "3.5");
        Function<String, BigDecimal> extractor = BigDecimal::new;
        assertTrue(Iterables.averageBigDecimal((Iterable<String>) null, extractor).isEmpty());
        assertTrue(Iterables.averageBigDecimal(Collections.<String> emptyList(), extractor).isEmpty());
        assertEquals(new BigDecimal("2.5"), Iterables.averageBigDecimal(list, extractor).orElse(null));
    }

    //endregion

    //region IndexOf and Find Methods
    @Test
    public void testIndexOfArray() {
        assertTrue(Iterables.indexOf((Object[]) null, "a").isEmpty());
        assertTrue(Iterables.indexOf(new String[0], "a").isEmpty());
        assertEquals(1, Iterables.indexOf(new String[] { "x", "a", "b" }, "a").orElse(-1));
        assertTrue(Iterables.indexOf(new String[] { "x", "b" }, "a").isEmpty());
        assertEquals(0, Iterables.indexOf(new String[] { null, "a" }, null).orElse(-1));
    }

    @Test
    public void testIndexOfCollection() {
        assertTrue(Iterables.indexOf((Collection<String>) null, "a").isEmpty());
        assertTrue(Iterables.indexOf(Collections.emptyList(), "a").isEmpty());
        assertEquals(1, Iterables.indexOf(Arrays.asList("x", "a", "b"), "a").orElse(-1));
        assertTrue(Iterables.indexOf(Arrays.asList("x", "b"), "a").isEmpty());
        assertEquals(0, Iterables.indexOf(Arrays.asList(null, "a"), null).orElse(-1));
    }

    @Test
    public void testLastIndexOfArray() {
        assertTrue(Iterables.lastIndexOf((Object[]) null, "a").isEmpty());
        assertTrue(Iterables.lastIndexOf(new String[0], "a").isEmpty());
        assertEquals(2, Iterables.lastIndexOf(new String[] { "x", "a", "a", "b" }, "a").orElse(-1));
        assertTrue(Iterables.lastIndexOf(new String[] { "x", "b" }, "a").isEmpty());
        assertEquals(2, Iterables.lastIndexOf(new String[] { "a", null, null }, null).orElse(-1));
    }

    @Test
    public void testLastIndexOfCollection() {
        assertTrue(Iterables.lastIndexOf((Collection<String>) null, "a").isEmpty());
        assertTrue(Iterables.lastIndexOf(Collections.emptyList(), "a").isEmpty());
        assertEquals(2, Iterables.lastIndexOf(Arrays.asList("x", "a", "a", "b"), "a").orElse(-1));
        assertTrue(Iterables.lastIndexOf(Arrays.asList("x", "b"), "a").isEmpty());
        assertEquals(2, Iterables.lastIndexOf(Arrays.asList("a", null, null), null).orElse(-1));
    }

    @Test
    public void testFindFirstOrLastArray() {
        Predicate<Integer> isEven = x -> x != null && x % 2 == 0;
        Predicate<Integer> isOdd = x -> x != null && x % 2 != 0;

        assertTrue(Iterables.findFirstOrLast((Integer[]) null, isEven, isOdd).isEmpty());
        assertTrue(Iterables.findFirstOrLast(new Integer[0], isEven, isOdd).isEmpty());

        assertEquals(2, Iterables.findFirstOrLast(new Integer[] { 1, 2, 3, 4 }, isEven, isOdd).orElse(null)); // First even is 2
        assertEquals(5, Iterables.findFirstOrLast(new Integer[] { 1, 3, 5 }, isEven, isOdd).orElse(null)); // No even, last odd is 5. Javadoc: last element that satisfies predicateForLast
                                                                                                           // N.findLast finds last that matches. So it should be 5.
                                                                                                           // The N.findLast will return the last matching. {1,3,5} -> predicateForFirst (isEven) fails. predicateForLast (isOdd) -> N.findLast is {5}. Correct.
        assertEquals(5, Iterables.findFirstOrLast(new Integer[] { 1, 3, 5 }, isEven, isOdd).orElse(null));
        assertTrue(Iterables.findFirstOrLast(new Integer[] { 10, 20 }, isEven, isOdd).get() == 10); // First even is 10
        assertTrue(Iterables.findFirstOrLast(new Integer[] { 11, 21 }, isEven, isOdd).get() == 21); // No even, last odd is 21.
    }

    @Test
    public void testFindFirstOrLastCollection() {
        Predicate<Integer> isEven = x -> x != null && x % 2 == 0;
        Predicate<Integer> isOdd = x -> x != null && x % 2 != 0;

        assertTrue(Iterables.findFirstOrLast((Collection<Integer>) null, isEven, isOdd).isEmpty());
        assertTrue(Iterables.findFirstOrLast(Collections.emptyList(), isEven, isOdd).isEmpty());

        assertEquals(2, Iterables.findFirstOrLast(Arrays.asList(1, 2, 3, 4), isEven, isOdd).orElse(null));
        assertEquals(5, Iterables.findFirstOrLast(Arrays.asList(1, 3, 5), isEven, isOdd).orElse(null));
    }

    @Test
    public void testFindFirstOrLastIndexArray() {
        Predicate<Integer> isEven = x -> x != null && x % 2 == 0;
        Predicate<Integer> isOdd = x -> x != null && x % 2 != 0;

        assertTrue(Iterables.findFirstOrLastIndex((Integer[]) null, isEven, isOdd).isEmpty());
        assertTrue(Iterables.findFirstOrLastIndex(new Integer[0], isEven, isOdd).isEmpty());

        assertEquals(1, Iterables.findFirstOrLastIndex(new Integer[] { 1, 2, 3, 4 }, isEven, isOdd).orElse(-1)); // Index of 2
        assertEquals(2, Iterables.findFirstOrLastIndex(new Integer[] { 1, 3, 5 }, isEven, isOdd).orElse(-1)); // Index of 5
    }

    @Test
    public void testFindFirstOrLastIndexCollection() {
        Predicate<Integer> isEven = x -> x != null && x % 2 == 0;
        Predicate<Integer> isOdd = x -> x != null && x % 2 != 0;

        assertTrue(Iterables.findFirstOrLastIndex((Collection<Integer>) null, isEven, isOdd).isEmpty());
        assertTrue(Iterables.findFirstOrLastIndex(Collections.emptyList(), isEven, isOdd).isEmpty());

        assertEquals(1, Iterables.findFirstOrLastIndex(Arrays.asList(1, 2, 3, 4), isEven, isOdd).orElse(-1));
        assertEquals(2, Iterables.findFirstOrLastIndex(Arrays.asList(1, 3, 5), isEven, isOdd).orElse(-1));
    }

    @Test
    public void testFindFirstAndLastArray() {
        Predicate<Integer> isEven = x -> x != null && x % 2 == 0;
        Pair<Nullable<Integer>, Nullable<Integer>> result;

        result = Iterables.findFirstAndLast((Integer[]) null, isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());

        result = Iterables.findFirstAndLast(new Integer[0], isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());

        result = Iterables.findFirstAndLast(new Integer[] { 1, 2, 3, 4, 5, 6 }, isEven);
        assertEquals(2, result.left().orElse(null));
        assertEquals(6, result.right().orElse(null));

        result = Iterables.findFirstAndLast(new Integer[] { 1, 3, 5 }, isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());
    }

    @Test
    public void testFindFirstAndLastArrayWithTwoPredicates() {
        Predicate<Integer> isSmallEven = x -> x != null && x % 2 == 0 && x < 5; // 2, 4
        Predicate<Integer> isLargeOdd = x -> x != null && x % 2 != 0 && x > 3; // 5
        Pair<Nullable<Integer>, Nullable<Integer>> result;

        result = Iterables.findFirstAndLast(new Integer[] { 1, 2, 3, 4, 5, 6 }, isSmallEven, isLargeOdd);
        assertEquals(2, result.left().orElse(null)); // first smallEven
        assertEquals(5, result.right().orElse(null)); // last largeOdd
    }

    @Test
    public void testFindFirstAndLastCollection() {
        Predicate<Integer> isEven = x -> x != null && x % 2 == 0;
        Pair<Nullable<Integer>, Nullable<Integer>> result;

        result = Iterables.findFirstAndLast((Collection<Integer>) null, isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());

        result = Iterables.findFirstAndLast(Collections.emptyList(), isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());

        result = Iterables.findFirstAndLast(Arrays.asList(1, 2, 3, 4, 5, 6), isEven);
        assertEquals(2, result.left().orElse(null));
        assertEquals(6, result.right().orElse(null));
    }

    @Test
    public void testFindFirstAndLastCollectionWithTwoPredicates() {
        Predicate<Integer> isSmallEven = x -> x != null && x % 2 == 0 && x < 5;
        Predicate<Integer> isLargeOdd = x -> x != null && x % 2 != 0 && x > 3;
        Pair<Nullable<Integer>, Nullable<Integer>> result;

        result = Iterables.findFirstAndLast(Arrays.asList(1, 2, 3, 4, 5, 6), isSmallEven, isLargeOdd);
        assertEquals(2, result.left().orElse(null));
        assertEquals(5, result.right().orElse(null));
    }

    @Test
    public void testFindFirstAndLastIndexArray() {
        Predicate<Integer> isEven = x -> x != null && x % 2 == 0;
        Pair<OptionalInt, OptionalInt> result;

        result = Iterables.findFirstAndLastIndex((Integer[]) null, isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());

        result = Iterables.findFirstAndLastIndex(new Integer[0], isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());

        result = Iterables.findFirstAndLastIndex(new Integer[] { 1, 2, 3, 4, 5, 6 }, isEven); // Indices: 1 (for 2), 5 (for 6)
        assertEquals(1, result.left().orElse(-1));
        assertEquals(5, result.right().orElse(-1));

        result = Iterables.findFirstAndLastIndex(new Integer[] { 1, 3, 5 }, isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());
    }

    @Test
    public void testFindFirstAndLastIndexArrayWithTwoPredicates() {
        Predicate<Integer> isSmallEven = x -> x != null && x % 2 == 0 && x < 5; // {1, *2*, 3, *4*, 5, 6} -> first is 2 (idx 1)
        Predicate<Integer> isLargeOdd = x -> x != null && x % 2 != 0 && x > 3; // {1, 2, 3, 4, *5*, 6} -> last is 5 (idx 4)
        Pair<OptionalInt, OptionalInt> result;

        result = Iterables.findFirstAndLastIndex(new Integer[] { 1, 2, 3, 4, 5, 6 }, isSmallEven, isLargeOdd);
        assertEquals(1, result.left().orElse(-1));
        assertEquals(4, result.right().orElse(-1));
    }

    @Test
    public void testFindFirstAndLastIndexCollection() {
        Predicate<Integer> isEven = x -> x != null && x % 2 == 0;
        Pair<OptionalInt, OptionalInt> result;

        result = Iterables.findFirstAndLastIndex((Collection<Integer>) null, isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());

        result = Iterables.findFirstAndLastIndex(Collections.emptyList(), isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());

        result = Iterables.findFirstAndLastIndex(Arrays.asList(1, 2, 3, 4, 5, 6), isEven);
        assertEquals(1, result.left().orElse(-1));
        assertEquals(5, result.right().orElse(-1));
    }

    @Test
    public void testFindFirstAndLastIndexCollectionWithTwoPredicates() {
        Predicate<Integer> isSmallEven = x -> x != null && x % 2 == 0 && x < 5;
        Predicate<Integer> isLargeOdd = x -> x != null && x % 2 != 0 && x > 3;
        Pair<OptionalInt, OptionalInt> result;

        result = Iterables.findFirstAndLastIndex(Arrays.asList(1, 2, 3, 4, 5, 6), isSmallEven, isLargeOdd);
        assertEquals(1, result.left().orElse(-1));
        assertEquals(4, result.right().orElse(-1));
    }

    //endregion

    //region Fill and Reverse Methods
    @Test
    public void testFillArrayWithSupplier() {
        Iterables.fill((String[]) null, () -> "a"); // Should not throw

        String[] arrEmpty = new String[0];
        Iterables.fill(arrEmpty, () -> "a");
        assertEquals(0, arrEmpty.length);

        String[] arr = new String[3];
        Iterables.fill(arr, () -> "filled");
        assertArrayEquals(new String[] { "filled", "filled", "filled" }, arr);

        final int[] counter = { 0 };
        Supplier<Integer> supplier = () -> counter[0]++;
        Integer[] arrInt = new Integer[3];
        Iterables.fill(arrInt, supplier);
        assertArrayEquals(new Integer[] { 0, 1, 2 }, arrInt);
    }

    @Test
    public void testFillArrayFromToWithSupplier() {
        Iterables.fill((String[]) null, 0, 0, () -> "a"); // Should not throw with null if len check handles it, but N.len(null) throws.
                                                          // N.checkFromToIndex(from, to, N.len(a)) will throw for null array.
                                                          //  assertThrows(NullPointerException.class, () -> Iterables.fill((String[]) null, 0, 0, () -> "a"));

        String[] arr = new String[5];
        Arrays.fill(arr, "original");
        Iterables.fill(arr, 1, 4, () -> "filled"); // Fill index 1, 2, 3
        assertArrayEquals(new String[] { "original", "filled", "filled", "filled", "original" }, arr);

        Iterables.fill(arr, 1, 1, () -> "no-fill"); // Empty range
        assertArrayEquals(new String[] { "original", "filled", "filled", "filled", "original" }, arr); // no change

        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.fill(arr, -1, 2, () -> "fail"));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.fill(arr, 0, 6, () -> "fail"));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.fill(arr, 3, 2, () -> "fail"));
    }

    @Test
    public void testFillListWithSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Iterables.fill((List<String>) null, () -> "a"));

        List<String> listEmpty = new ArrayList<>();
        Iterables.fill(listEmpty, () -> "a");
        assertTrue(listEmpty.isEmpty());

        List<String> list = new ArrayList<>(Arrays.asList("x", "y", "z"));
        Iterables.fill(list, () -> "filled");
        assertEquals(Arrays.asList("filled", "filled", "filled"), list);

        final int[] counter = { 0 };
        Supplier<Integer> supplier = () -> counter[0]++;
        List<Integer> listInt = new ArrayList<>(Arrays.asList(0, 0, 0));
        Iterables.fill(listInt, supplier);
        assertEquals(Arrays.asList(0, 1, 2), listInt);
    }

    @Test
    public void testFillListFromToWithSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Iterables.fill((List<String>) null, 0, 0, () -> "a"));

        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        Iterables.fill(list, 1, 4, () -> "filled"); // Fill index 1, 2, 3
        assertEquals(Arrays.asList("a", "filled", "filled", "filled", "e"), list);

        // Extend list
        List<String> shortList = new ArrayList<>(Arrays.asList("a")); // size 1
        Iterables.fill(shortList, 0, 3, () -> "new"); // from 0, to 3. current size 1.
                                                      // list.set(0, "new")
                                                      // list.add("new"), list.add("new")
        assertEquals(Arrays.asList("new", "new", "new"), shortList);
        assertEquals(3, shortList.size());

        List<String> listToExtend = new ArrayList<>(Arrays.asList("a", "b")); // size 2
        Iterables.fill(listToExtend, 1, 4, () -> "Z");
        // Expected: list[1] = Z. Then add Z, Z.
        // a, Z, Z, Z
        assertEquals(Arrays.asList("a", "Z", "Z", "Z"), listToExtend);

        List<Integer> listInt = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        final int[] counter = { 10 };
        Supplier<Integer> supplier = () -> counter[0]++;
        Iterables.fill(listInt, 2, 5, supplier); // indices 2,3,4 with 10,11,12
        assertEquals(Arrays.asList(1, 2, 10, 11, 12), listInt);

        List<Integer> listFillAndExtend = new ArrayList<>(Arrays.asList(1, 2)); // size 2
        final int[] counter2 = { 100 };
        Supplier<Integer> supplier2 = () -> counter2[0]++;
        Iterables.fill(listFillAndExtend, 1, 4, supplier2); // list[1]=100; list.add(101); list.add(102)
        assertEquals(Arrays.asList(1, 100, 101, 102), listFillAndExtend);

        // Fill with fromIndex >= size
        List<Integer> listFillFromSize = new ArrayList<>(Arrays.asList(1, 2)); // size 2
        final int[] counter3 = { 200 };
        Supplier<Integer> supplier3 = () -> counter3[0]++;
        Iterables.fill(listFillFromSize, 2, 4, supplier3); // list.add(200); list.add(201)
                                                           // The loop for (int i = size; i < fromIndex; i++) adds nulls.
                                                           // Here size=2, fromIndex=2. Loop does not run.
                                                           // Then for (int i = 0, len = toIndex - list.size(); i < len; i++)
                                                           // list.size() is now 2. toIndex is 4. len = 2.
                                                           // list.add(supplier.get()) twice.
        assertEquals(Arrays.asList(1, 2, 200, 201), listFillFromSize);

        List<Integer> listFillFromSize2 = new ArrayList<>(Arrays.asList(1, 2)); // size 2
        final int[] counter4 = { 300 };
        Supplier<Integer> supplier4 = () -> counter4[0]++;
        Iterables.fill(listFillFromSize2, 3, 5, supplier4); // fromIndex=3, toIndex=5
        // size=2.
        // for (int i = size; i < fromIndex; i++) -> for (i=2; i<3; i++) -> list.add(null) -> list is {1,2,null}
        // list.size() is now 3.
        // for (int i = 0, len = toIndex - list.size(); i < len; i++) -> len = 5-3 = 2.
        // list.add(supplier4.get()) -> 300
        // list.add(supplier4.get()) -> 301
        // Expected: {1,2,null,300,301}
        assertEquals(Arrays.asList(1, 2, null, 300, 301), listFillFromSize2);

        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.fill(list, -1, 2, () -> "fail"));
        // No: toIndex can be > list.size()
        // assertThrows(IndexOutOfBoundsException.class, () -> Iterables.fill(list, 0, list.size() + 2, () -> "fail"));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.fill(list, 3, 2, () -> "fail"));

    }

    @Test
    public void testReverseList() {
        List<Integer> original = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        List<Integer> reversed = Iterables.reverse(original);

        assertEquals(Arrays.asList(4, 3, 2, 1), reversed);
        assertEquals(4, reversed.size());
        assertEquals(Integer.valueOf(4), reversed.get(0));
        assertEquals(Integer.valueOf(1), reversed.get(3));

        // Modification through reversed view
        reversed.set(0, 10); // Changes original's last element
        assertEquals(Arrays.asList(1, 2, 3, 10), original);
        assertEquals(Arrays.asList(10, 3, 2, 1), reversed);

        reversed.add(0, 20); // Adds to beginning of reversed, which is end of original view (but adds before)
                             // original.add(original.size() - 0, 20) effectively.
                             // forwardList.add(reversePosition(index), element); reversePosition(0) is size().
                             // So, original.add(size(), 20)
        assertEquals(Arrays.asList(1, 2, 3, 10, 20), original);
        assertEquals(Arrays.asList(20, 10, 3, 2, 1), reversed);

        // Modification through original view
        original.add(0, 0); // {0,1,2,3,10,20}
        assertEquals(Arrays.asList(20, 10, 3, 2, 1, 0), reversed);

        List<Integer> single = new ArrayList<>(Collections.singletonList(1));
        assertEquals(Collections.singletonList(1), Iterables.reverse(single));

        List<Integer> empty = new ArrayList<>();
        assertEquals(Collections.emptyList(), Iterables.reverse(empty));

        // Test RandomAccessReverseList
        List<Integer> randomAccessOriginal = Arrays.asList(5, 6, 7); // This is RandomAccess
        List<Integer> randomAccessReversed = Iterables.reverse(randomAccessOriginal);
        assertEquals(Arrays.asList(7, 6, 5), randomAccessReversed);
        // Attempt to modify (Arrays.asList is fixed-size for add/remove, but set is ok)
        assertThrows(UnsupportedOperationException.class, () -> randomAccessReversed.add(33));
        randomAccessReversed.set(0, 77);
        assertEquals(Arrays.asList(5, 6, 77), randomAccessOriginal);
    }
    //endregion

    //region Set Operations
    @Test
    public void testUnion() {
        Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> set2 = new HashSet<>(Arrays.asList(3, 4, 5));

        {
            Iterables.SetView<Integer> union = Iterables.union(set1, set2);

            assertEquals(new HashSet<>(Arrays.asList(1, 2, 3, 4, 5)), union);
            assertEquals(5, union.size());
            assertTrue(union.contains(1));
            assertTrue(union.contains(4));
            assertFalse(union.contains(6));

            // Order of iteration: set1 then set2 (excluding duplicates)
            List<Integer> iterated = new ArrayList<>();
            union.iterator().forEachRemaining(iterated::add);
            // This is not strictly guaranteed for HashSet, but the implementation iterates set1 then set2.
            // For predictability, let's use LinkedHashSet
        }
        Set<Integer> lhSet1 = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> lhSet2 = new LinkedHashSet<>(Arrays.asList(3, 4, 5)); // 3 is common
        {
            Iterables.SetView<Integer> union = Iterables.union(lhSet1, lhSet2);
            List<Integer> expectedOrder = Arrays.asList(1, 2, 3, 4, 5); // Iterates 1,2,3 from lhSet1. Then 4,5 from lhSet2 (3 is skipped).
            List<Integer> iterated = new ArrayList<>();
            union.iterator().forEachRemaining(iterated::add);
            assertEquals(expectedOrder, iterated);

            assertTrue(Iterables.union(null, null).isEmpty());
            assertEquals(set1, Iterables.union(set1, null));
            assertEquals(set2, Iterables.union(null, set2));
            assertEquals(set1, Iterables.union(set1, Collections.emptySet()));
            assertEquals(set2, Iterables.union(Collections.emptySet(), set2));

            // Test copyInto
            HashSet<Integer> target = new HashSet<>();
            union.copyInto(target);
            assertEquals(new HashSet<>(Arrays.asList(1, 2, 3, 4, 5)), target);

            // Test immutability of view
            assertThrows(UnsupportedOperationException.class, () -> union.add(10));
        }
    }

    @Test
    public void testIntersection() {
        Set<Integer> set1 = new LinkedHashSet<>(Arrays.asList(1, 2, 3, 6)); // Use LinkedHashSet for predictable iteration
        Set<Integer> set2 = new HashSet<>(Arrays.asList(3, 4, 5, 6));
        Iterables.SetView<Integer> intersection = Iterables.intersection(set1, set2);

        assertEquals(new HashSet<>(Arrays.asList(3, 6)), intersection);
        assertEquals(2, intersection.size());
        assertTrue(intersection.contains(3));
        assertTrue(intersection.contains(6));
        assertFalse(intersection.contains(1));

        // Order of iteration: matches set1
        List<Integer> iterated = new ArrayList<>();
        intersection.iterator().forEachRemaining(iterated::add);
        assertEquals(Arrays.asList(3, 6), iterated); // 3 appears before 6 in set1

        assertTrue(Iterables.intersection(null, null).isEmpty());
        assertTrue(Iterables.intersection(set1, null).isEmpty());
        assertTrue(Iterables.intersection(null, set2).isEmpty());
        assertTrue(Iterables.intersection(set1, Collections.emptySet()).isEmpty());

        // Test immutability
        assertThrows(UnsupportedOperationException.class, () -> intersection.add(10));
    }

    @Test
    public void testDifference() {
        Set<Integer> set1 = new LinkedHashSet<>(Arrays.asList(1, 2, 3, 6)); // Use LinkedHashSet for predictable iteration
        Set<Integer> set2 = new HashSet<>(Arrays.asList(3, 4, 5, 6));
        Iterables.SetView<Integer> difference = Iterables.difference(set1, set2); // set1 - set2

        assertEquals(new HashSet<>(Arrays.asList(1, 2)), difference);
        assertEquals(2, difference.size());
        assertTrue(difference.contains(1));
        assertTrue(difference.contains(2));
        assertFalse(difference.contains(3));
        assertFalse(difference.contains(6));

        // Order of iteration: matches set1
        List<Integer> iterated = new ArrayList<>();
        difference.iterator().forEachRemaining(iterated::add);
        assertEquals(Arrays.asList(1, 2), iterated);

        assertTrue(Iterables.difference(null, null).isEmpty());
        assertEquals(set1, Iterables.difference(set1, null)); // set1 - emptySet = set1
        assertTrue(Iterables.difference(null, set2).isEmpty()); // emptySet - set2 = emptySet
        assertEquals(set1, Iterables.difference(set1, Collections.emptySet()));

        // Test immutability
        assertThrows(UnsupportedOperationException.class, () -> difference.add(10));
    }

    @Test
    public void testSymmetricDifference() {
        Set<Integer> set1 = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> set2 = new LinkedHashSet<>(Arrays.asList(3, 4, 5));
        Iterables.SetView<Integer> symDiff = Iterables.symmetricDifference(set1, set2);

        assertEquals(new HashSet<>(Arrays.asList(1, 2, 4, 5)), symDiff);
        assertEquals(4, symDiff.size());
        assertTrue(symDiff.contains(1));
        assertTrue(symDiff.contains(4));
        assertFalse(symDiff.contains(3)); // Common element removed

        // Iteration order is (set1-set2) then (set2-set1)
        List<Integer> iterated = new ArrayList<>();
        symDiff.iterator().forEachRemaining(iterated::add);
        assertEquals(Arrays.asList(1, 2, 4, 5), iterated);

        assertTrue(Iterables.symmetricDifference(null, null).isEmpty());
        assertEquals(set1, Iterables.symmetricDifference(set1, null));
        assertEquals(set2, Iterables.symmetricDifference(null, set2));
        assertEquals(set1, Iterables.symmetricDifference(set1, Collections.emptySet()));

        // Test immutability
        assertThrows(UnsupportedOperationException.class, () -> symDiff.add(10));
    }

    @Test
    public void testSubSetNavigableSet() {
        NavigableSet<Integer> set = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));

        assertTrue(Iterables.subSet(N.emptyNavigableSet(), Range.closed(1, 5)).isEmpty());

        // CLOSED_OPEN: [3, 7) -> {3, 4, 5, 6}
        Range<Integer> r1 = Range.closedOpen(3, 7);
        NavigableSet<Integer> sub1 = Iterables.subSet(set, r1);
        assertEquals(new TreeSet<>(Arrays.asList(3, 4, 5, 6)), sub1);

        // CLOSED_CLOSED: [3, 7] -> {3, 4, 5, 6, 7}
        Range<Integer> r2 = Range.closed(3, 7);
        NavigableSet<Integer> sub2 = Iterables.subSet(set, r2);
        assertEquals(new TreeSet<>(Arrays.asList(3, 4, 5, 6, 7)), sub2);

        // OPEN_CLOSED: (3, 7] -> {4, 5, 6, 7}
        Range<Integer> r3 = Range.openClosed(3, 7);
        NavigableSet<Integer> sub3 = Iterables.subSet(set, r3);
        assertEquals(new TreeSet<>(Arrays.asList(4, 5, 6, 7)), sub3);

        // OPEN_OPEN: (3, 7) -> {4, 5, 6}
        Range<Integer> r4 = Range.open(3, 7);
        NavigableSet<Integer> sub4 = Iterables.subSet(set, r4);
        assertEquals(new TreeSet<>(Arrays.asList(4, 5, 6)), sub4);

        // Test with custom comparator (should throw if range is not consistent with natural ordering, which N.subSet does not do)
        // The Iterables.subSet checks this.
        NavigableSet<Integer> customSet = new TreeSet<>(Comparator.reverseOrder());
        customSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        Range<Integer> naturalRange = Range.closed(2, 4); // 2 < 4 in natural order
        // customSet sorted: 5,4,3,2,1. Comparator is reverse.
        // comparator.compare(range.lowerEndpoint(), range.upperEndpoint()) <= 0
        // reverseComparator.compare(2,4) -> 4.compareTo(2) -> 1 which is > 0. So should throw.
        assertThrows(IllegalArgumentException.class, () -> Iterables.subSet(customSet, naturalRange));

        Range<Integer> reverseRange = Range.closed(Integer.MIN_VALUE, 2); // Effectively Range.closed(Integer.MIN_VALUE, 2) if not bounded below
                                                                          // Let's use a proper range for custom comparator
                                                                          //        Range<Integer> customConsistentRange = Range.closed(4, 2, Comparators.reverseOrder()); // 4 >= 2 by natural order. reverseComparator.compare(4,2) = 2.compareTo(4) = -1 <=0. OK.
                                                                          //                                                                   // This means lowerEndpoint by custom comparator is '4', upper is '2'.
                                                                          //                                                                   // So, elements >= 4 and <= 2 (by reverse order means elements <=4 and >=2 by natural)
                                                                          //                                                                   // subSet(fromElement, fromInclusive, toElement, toInclusive)
                                                                          //                                                                   // subSet(4, true, 2, true) -> should give {4,3,2}
                                                                          //        NavigableSet<Integer> customSub = Iterables.subSet(customSet, customConsistentRange);
                                                                          //        assertEquals(new TreeSet<Integer>(Comparator.reverseOrder()) {
                                                                          //            {
                                                                          //                addAll(Arrays.asList(2, 3, 4));
                                                                          //            }
                                                                          //        }, customSub);
    }

    @Test
    public void testPowerSet() {
        Set<Integer> empty = Collections.emptySet();
        Set<Set<Integer>> psEmpty = Iterables.powerSet(empty);
        assertEquals(1, psEmpty.size());
        assertTrue(psEmpty.contains(Collections.emptySet()));

        Set<Integer> single = Collections.singleton(1);
        Set<Set<Integer>> psSingle = Iterables.powerSet(single);
        assertEquals(2, psSingle.size());
        assertTrue(psSingle.contains(Collections.emptySet()));
        assertTrue(psSingle.contains(Collections.singleton(1)));

        Set<Integer> two = new LinkedHashSet<>(Arrays.asList(1, 2)); // LinkedHashSet for predictable element order in subsets if it mattered
        Set<Set<Integer>> psTwo = Iterables.powerSet(two);
        assertEquals(4, psTwo.size());
        assertTrue(psTwo.contains(Collections.emptySet()));
        assertTrue(psTwo.contains(Collections.singleton(1)));
        assertTrue(psTwo.contains(Collections.singleton(2)));
        assertTrue(psTwo.contains(new HashSet<>(Arrays.asList(1, 2))));

        // Test contains with different set type but same elements
        assertTrue(psTwo.contains(new TreeSet<>(Arrays.asList(1, 2))));

        // Test max size (30 elements)
        Set<Integer> largeSet = new HashSet<>();
        for (int i = 0; i < 30; i++)
            largeSet.add(i);
        Set<Set<Integer>> psLarge = Iterables.powerSet(largeSet);
        assertEquals(1 << 30, psLarge.size());

        Set<Integer> tooLargeSet = new HashSet<>();
        for (int i = 0; i < 31; i++)
            tooLargeSet.add(i);
        assertThrows(IllegalArgumentException.class, () -> Iterables.powerSet(tooLargeSet));

        // Immutability (cannot test directly on PowerSet, but its iterator's remove)
        Iterator<Set<Integer>> iter = psTwo.iterator();
        iter.next();
        assertThrows(UnsupportedOperationException.class, iter::remove);
    }
    //endregion

    //region Rollup, Permutations, Cartesian Product
    @Test
    public void testRollup() {
        assertTrue(Iterables.rollup(null).get(0).isEmpty() && Iterables.rollup(null).size() == 1);
        assertEquals(Collections.singletonList(Collections.emptyList()), Iterables.rollup(Collections.emptyList()));

        List<String> list = Arrays.asList("a", "b", "c");
        List<List<String>> rollup = Iterables.rollup(list);
        assertEquals(4, rollup.size());
        assertEquals(Collections.emptyList(), rollup.get(0));
        assertEquals(Arrays.asList("a"), rollup.get(1));
        assertEquals(Arrays.asList("a", "b"), rollup.get(2));
        assertEquals(Arrays.asList("a", "b", "c"), rollup.get(3));

        // Ensure lists are new instances
        assertNotSame(rollup.get(1), rollup.get(2));
    }

    @Test
    public void testPermutations() {
        assertThrows(IllegalArgumentException.class, () -> Iterables.permutations(null).isEmpty()); // N.nullToEmpty results in empty list, permutation of empty list is a list containing an empty list.
        // The PermutationCollection for empty inputList has size factorial(0)=1. Iterator returns one empty list.
        Collection<List<Integer>> permEmpty = Iterables.permutations(Collections.emptyList());
        assertEquals(1, permEmpty.size());
        assertEquals(Collections.emptyList(), permEmpty.iterator().next());

        Collection<List<Integer>> permSingle = Iterables.permutations(Collections.singletonList(1));
        assertEquals(1, permSingle.size());
        assertEquals(Collections.singletonList(1), permSingle.iterator().next());

        Collection<List<Integer>> permTwo = Iterables.permutations(Arrays.asList(1, 2));
        assertEquals(2, permTwo.size()); // 2! = 2
        assertTrue(permTwo.contains(Arrays.asList(1, 2)));
        assertTrue(permTwo.contains(Arrays.asList(2, 1)));

        Collection<List<Integer>> permThree = Iterables.permutations(Arrays.asList(1, 2, 3));
        assertEquals(6, permThree.size()); // 3! = 6
        assertTrue(permThree.contains(Arrays.asList(1, 2, 3)));
        assertTrue(permThree.contains(Arrays.asList(1, 3, 2)));
        assertTrue(permThree.contains(Arrays.asList(2, 1, 3)));
        assertTrue(permThree.contains(Arrays.asList(2, 3, 1)));
        assertTrue(permThree.contains(Arrays.asList(3, 1, 2)));
        assertTrue(permThree.contains(Arrays.asList(3, 2, 1)));

        // Test with duplicates - permutations are distinct in terms of list instances, but values might be same
        Collection<List<Integer>> permDuplicates = Iterables.permutations(Arrays.asList(1, 1, 2));
        assertEquals(6, permDuplicates.size()); // Still 3! permutations if elements treated as distinct by position
        // The PermutationCollection uses Plain Changes, which treats elements by position.
        // It will generate [1,1,2], [1,2,1], [2,1,1] etc multiple times if we only look at values.
        // Let's check if the count of distinct *value* lists is 3 for (1,1,2) which is 3!/2! = 3.
        // The returned collection contains all 3! permutations. Some will be equal in content.
        Set<List<Integer>> distinctPerms = new HashSet<>(permDuplicates);
        assertEquals(3, distinctPerms.size());
        assertTrue(distinctPerms.contains(Arrays.asList(1, 1, 2)));
        assertTrue(distinctPerms.contains(Arrays.asList(1, 2, 1)));
        assertTrue(distinctPerms.contains(Arrays.asList(2, 1, 1)));
    }

    @Test
    public void testOrderedPermutations() {
        Collection<List<Integer>> permEmpty = Iterables.orderedPermutations(Collections.<Integer> emptyList());
        assertEquals(1, permEmpty.size());
        assertEquals(Collections.emptyList(), permEmpty.iterator().next());

        Collection<List<Integer>> permSingle = Iterables.orderedPermutations(Collections.singletonList(1));
        assertEquals(1, permSingle.size());
        assertEquals(Collections.singletonList(1), permSingle.iterator().next());

        // For (1,2) -> (1,2), (2,1)
        List<Integer> twoElements = Arrays.asList(1, 2);
        Collection<List<Integer>> permTwo = Iterables.orderedPermutations(twoElements);
        assertEquals(2, permTwo.size());
        Iterator<List<Integer>> itTwo = permTwo.iterator();
        assertEquals(Arrays.asList(1, 2), itTwo.next());
        assertEquals(Arrays.asList(2, 1), itTwo.next());

        // For (1,1,2) -> (1,1,2), (1,2,1), (2,1,1)
        List<Integer> withDuplicates = Arrays.asList(1, 1, 2);
        Collection<List<Integer>> permDup = Iterables.orderedPermutations(withDuplicates);
        assertEquals(3, permDup.size()); // 3!/2! = 3
        Iterator<List<Integer>> itDup = permDup.iterator();
        assertEquals(Arrays.asList(1, 1, 2), itDup.next());
        assertEquals(Arrays.asList(1, 2, 1), itDup.next());
        assertEquals(Arrays.asList(2, 1, 1), itDup.next());

        // Example from Guava docs: ("b", "c", "a") with natural order
        // Sorted: ("a", "b", "c")
        // Output: ["a", "b", "c"], ["a", "c", "b"], ["b", "a", "c"], ["b", "c", "a"], ["c", "a", "b"], ["c", "b", "a"]
        List<String> strings = Arrays.asList("b", "c", "a");
        Collection<List<String>> permStr = Iterables.orderedPermutations(strings);
        assertEquals(6, permStr.size());
        Iterator<List<String>> itStr = permStr.iterator();
        assertEquals(Arrays.asList("a", "b", "c"), itStr.next());
        assertEquals(Arrays.asList("a", "c", "b"), itStr.next());
        assertEquals(Arrays.asList("b", "a", "c"), itStr.next());
        assertEquals(Arrays.asList("b", "c", "a"), itStr.next());
        assertEquals(Arrays.asList("c", "a", "b"), itStr.next());
        assertEquals(Arrays.asList("c", "b", "a"), itStr.next());
    }

    @Test
    public void testOrderedPermutationsWithComparator() {
        // (2,1) with reverse order comparator. Sorted input: (2,1)
        // Perms: (2,1), (1,2)
        List<Integer> list = Arrays.asList(2, 1); // Input order doesn't matter as it's sorted first
        Collection<List<Integer>> perms = Iterables.orderedPermutations(list, Comparator.reverseOrder());
        assertEquals(2, perms.size());
        Iterator<List<Integer>> it = perms.iterator();
        assertEquals(Arrays.asList(2, 1), it.next()); // Largest first by reverse comparator
        assertEquals(Arrays.asList(1, 2), it.next());

        // (1,1,2) with reverse comparator. Sorted: (2,1,1)
        // Perms: (2,1,1), (1,2,1), (1,1,2)
        List<Integer> listDup = Arrays.asList(1, 1, 2);
        Collection<List<Integer>> permsDup = Iterables.orderedPermutations(listDup, Comparator.reverseOrder());
        assertEquals(3, permsDup.size());
        Iterator<List<Integer>> itDup = permsDup.iterator();
        assertEquals(Arrays.asList(2, 1, 1), itDup.next());
        assertEquals(Arrays.asList(1, 2, 1), itDup.next());
        assertEquals(Arrays.asList(1, 1, 2), itDup.next());
    }

    @Test
    public void testCartesianProductVarargs() {
        List<List<Object>> cpEmpty = Iterables.cartesianProduct(); // No lists
        assertEquals(1, cpEmpty.size());
        assertTrue(cpEmpty.get(0).isEmpty());

        List<List<Object>> cpOneList = Iterables.cartesianProduct(Arrays.asList(1, 2));
        assertEquals(2, cpOneList.size());
        assertEquals(Collections.singletonList(1), cpOneList.get(0));
        assertEquals(Collections.singletonList(2), cpOneList.get(1));

        Collection<Integer> list1 = Arrays.asList(1, 2);
        Collection<String> list2 = Arrays.asList("A", "B");
        List<List<Object>> cpTwoLists = Iterables.cartesianProduct(list1, list2);
        assertEquals(4, cpTwoLists.size());
        assertEquals(Arrays.asList(1, "A"), cpTwoLists.get(0));
        assertEquals(Arrays.asList(1, "B"), cpTwoLists.get(1));
        assertEquals(Arrays.asList(2, "A"), cpTwoLists.get(2));
        assertEquals(Arrays.asList(2, "B"), cpTwoLists.get(3));

        // One empty list
        List<List<Object>> cpWithEmpty = Iterables.cartesianProduct(list1, Collections.emptyList(), list2);
        assertTrue(cpWithEmpty.isEmpty());

        // Test > Integer.MAX_VALUE (hard to test directly, relies on internal Numbers.multiplyExact)
        // Small test for contains
        assertTrue(cpTwoLists.contains(Arrays.asList(1, "A")));
        assertFalse(cpTwoLists.contains(Arrays.asList(1, "C")));
        assertFalse(cpTwoLists.contains(Arrays.asList(1))); // wrong size
        assertFalse(cpTwoLists.contains(Arrays.asList(3, "A"))); // element not in source
    }

    @Test
    public void testCartesianProductCollectionOfCollections() {
        List<Collection<?>> listOfColls = new ArrayList<>();
        List<List<Object>> cpEmptyOuter = Iterables.cartesianProduct(listOfColls);
        assertEquals(1, cpEmptyOuter.size());
        assertTrue(cpEmptyOuter.get(0).isEmpty());

        listOfColls.add(Arrays.asList(1, 2));
        List<List<Object>> cpOneList = Iterables.cartesianProduct(listOfColls);
        assertEquals(2, cpOneList.size());
        assertEquals(Collections.singletonList(1), cpOneList.get(0));
        assertEquals(Collections.singletonList(2), cpOneList.get(1));

        listOfColls.add(Arrays.asList("A", "B"));
        List<List<Object>> cpTwoLists = Iterables.cartesianProduct(listOfColls);
        assertEquals(4, cpTwoLists.size());
        assertEquals(Arrays.asList(1, "A"), cpTwoLists.get(0));
        assertEquals(Arrays.asList(1, "B"), cpTwoLists.get(1));
        assertEquals(Arrays.asList(2, "A"), cpTwoLists.get(2));
        assertEquals(Arrays.asList(2, "B"), cpTwoLists.get(3));

        // Test with null collection input
        List<List<Object>> cpNullInput = Iterables.cartesianProduct((Collection<? extends Collection<?>>) null);
        assertEquals(1, cpNullInput.size()); // N.nullToEmpty makes it an empty list of lists.
        assertTrue(cpNullInput.get(0).isEmpty());
    }
    //endregion
}
