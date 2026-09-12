package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;

public class IterablesMinTest extends IterablesTestSupport {
    @Test
    public void testMinCharArray() {
        char[] arr = { 'd', 'a', 'c', 'b' };
        OptionalChar result = Iterables.min(arr);
        assertTrue(result.isPresent());
        assertEquals('a', result.get());

        assertFalse(Iterables.min((char[]) null).isPresent());
        assertFalse(Iterables.min(new char[0]).isPresent());
    }

    @Test
    public void testMinByteArray() {
        byte[] arr = { 5, 2, 8, 1, 9 };
        OptionalByte result = Iterables.min(arr);
        assertTrue(result.isPresent());
        assertEquals((byte) 1, result.get());

        assertFalse(Iterables.min((byte[]) null).isPresent());
        assertFalse(Iterables.min(new byte[0]).isPresent());
    }

    @Test
    public void testMinShortArray() {
        short[] arr = { 100, 50, 200, 25 };
        OptionalShort result = Iterables.min(arr);
        assertTrue(result.isPresent());
        assertEquals((short) 25, result.get());

        assertFalse(Iterables.min((short[]) null).isPresent());
        assertFalse(Iterables.min(new short[0]).isPresent());
    }

    @Test
    public void testMinFloatArray() {
        float[] arr = { 3.14f, 1.41f, 2.71f };
        OptionalFloat result = Iterables.min(arr);
        assertTrue(result.isPresent());
        assertEquals(1.41f, result.get(), 0.001f);

        assertFalse(Iterables.min((float[]) null).isPresent());
        assertFalse(Iterables.min(new float[0]).isPresent());
    }

    @Test
    public void testMinObjectArray() {
        Integer[] arr = { 3, 1, 4, 1, 5, 9 };
        Nullable<Integer> result = Iterables.min(arr);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get());

        assertTrue(Iterables.min(new Integer[0]).isEmpty());
        assertTrue(Iterables.min((Integer[]) null).isEmpty());
    }

    @Test
    public void testMinObjectArrayWithComparator() {
        String[] arr = { "apple", "banana", "cherry" };
        Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
        Nullable<String> result = Iterables.min(arr, lengthComparator);
        assertTrue(result.isPresent());
        assertEquals("apple", result.get());

        assertTrue(Iterables.min(new String[0], lengthComparator).isEmpty());
        assertTrue(Iterables.min((String[]) null, lengthComparator).isEmpty());
    }

    @Test
    public void testMinIterable() {
        Nullable<Integer> result = Iterables.min(intList);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get());

        assertTrue(Iterables.min((Iterable<Integer>) null).isEmpty());
        assertTrue(Iterables.min(new ArrayList<Integer>()).isEmpty());
    }

    @Test
    public void testMinIterableWithComparator() {
        Comparator<String> reverseComparator = Comparator.reverseOrder();
        Nullable<String> result = Iterables.min(stringList, reverseComparator);
        assertTrue(result.isPresent());
        assertEquals("date", result.get());

        assertTrue(Iterables.min((Iterable<String>) null, reverseComparator).isEmpty());
    }

    @Test
    public void testMinIterator() {
        Nullable<Integer> result = Iterables.min(intList.iterator());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get());

        assertTrue(Iterables.min((Iterator<Integer>) null).isEmpty());
    }

    @Test
    public void testMinIteratorWithComparator() {
        Comparator<Integer> reverseComparator = Comparator.reverseOrder();
        Nullable<Integer> result = Iterables.min(intList.iterator(), reverseComparator);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(9), result.get());
    }

    @Test
    public void testMinGenericArray() {
        assertTrue(Iterables.min((Integer[]) null).isEmpty());
        assertTrue(Iterables.min(new Integer[0]).isEmpty());
        assertEquals(1, Iterables.min(new Integer[] { 3, 1, 2 }).orElse(null));
        assertEquals(1, Iterables.min(new Integer[] { null, 3, 1, 2 }).orElse(null));
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
    public void testMinGenericArrayComparable() {
        assertTrue(Iterables.min((String[]) null).isEmpty());
        assertTrue(Iterables.min(new String[0]).isEmpty());
        assertEquals("a", Iterables.min(new String[] { "a", "b", "c" }).get());
        assertEquals("a", Iterables.min(new String[] { "c", "b", "a" }).get());
        assertEquals("a", Iterables.min(new String[] { "a", null, "c" }).get());
        assertEquals("a", Iterables.min(new String[] { null, "a", "c" }).get());
    }

    @Test
    public void testMinGenericArrayComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.min((String[]) null, reverseOrder).isEmpty());
        assertTrue(Iterables.min(new String[0], reverseOrder).isEmpty());
        assertEquals("c", Iterables.min(new String[] { "a", "b", "c" }, reverseOrder).get());
        assertEquals("a", Iterables.min(new String[] { "a", "b", "c" }, Comparator.naturalOrder()).get());
        assertEquals("a", Iterables.min(new String[] { "a", null, "c" }, Comparators.nullsLast(Comparator.<String> naturalOrder())).get());
    }

    @Test
    public void testMinIterableComparable() {
        assertTrue(Iterables.min((Iterable<String>) null).isEmpty());
        assertTrue(Iterables.min(list()).isEmpty());
        assertEquals("a", Iterables.min(list("a", "b", "c")).get());
        assertEquals("a", Iterables.min(list("c", "b", "a")).get());
        assertEquals("a", Iterables.min(list("a", null, "c")).get());
    }

    @Test
    public void testMinIterableComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.min((Iterable<String>) null, reverseOrder).isEmpty());
        assertTrue(Iterables.min(list(), reverseOrder).isEmpty());
        assertEquals("c", Iterables.min(list("a", "b", "c"), reverseOrder).get());
        assertEquals("a", Iterables.min(list("a", "b", "c"), Comparator.naturalOrder()).get());
        assertEquals("a", Iterables.min(list("a", null, "c"), Comparator.nullsLast(Comparator.naturalOrder())).get());
        assertNull(Iterables.min(list("a", null, "c"), Comparator.nullsFirst(Comparator.naturalOrder())).get());
    }

    @Test
    public void testMinIteratorComparable() {
        assertTrue(Iterables.min((Iterator<String>) null).isEmpty());
        assertTrue(Iterables.min(list(CommonUtil.EMPTY_STRING_ARRAY).iterator()).isEmpty());
        assertEquals("a", Iterables.min(list("a", "b", "c").iterator()).get());
        assertEquals("a", Iterables.min(list("c", "b", "a").iterator()).get());
        assertEquals("a", Iterables.min(list("a", null, "c").iterator()).get());
    }

    @Test
    public void testMinIteratorComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.min((Iterator<String>) null, reverseOrder).isEmpty());
        assertTrue(Iterables.min(list(CommonUtil.EMPTY_STRING_ARRAY).iterator(), reverseOrder).isEmpty());
        assertEquals("c", Iterables.min(list("a", "b", "c").iterator(), reverseOrder).get());
        assertEquals("a", Iterables.min(list("a", "b", "c").iterator(), Comparator.naturalOrder()).get());
        assertEquals("a", Iterables.min(list("a", null, "c").iterator(), Comparator.nullsLast(Comparator.naturalOrder())).get());
        assertNull(Iterables.min(list("a", null, "c").iterator(), Comparator.nullsFirst(Comparator.naturalOrder())).get());
    }

    @Test
    public void testMinChar() {
        char[] array = { 'c', 'a', 'b' };
        OptionalChar result = Iterables.min(array);
        assertTrue(result.isPresent());
        assertEquals('a', result.get());

        OptionalChar nullResult = Iterables.min((char[]) null);
        assertFalse(nullResult.isPresent());

        OptionalChar emptyResult = Iterables.min(new char[0]);
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testMinByte() {
        byte[] array = { 3, 1, 2 };
        OptionalByte result = Iterables.min(array);
        assertTrue(result.isPresent());
        assertEquals(1, result.get());

        assertFalse(Iterables.min((byte[]) null).isPresent());
        assertFalse(Iterables.min(new byte[0]).isPresent());
    }

    @Test
    public void testMinShort() {
        short[] array = { 30, 10, 20 };
        OptionalShort result = Iterables.min(array);
        assertTrue(result.isPresent());
        assertEquals(10, result.get());

        assertFalse(Iterables.min((short[]) null).isPresent());
        assertFalse(Iterables.min(new short[0]).isPresent());
    }

    @Test
    public void testMinFloat() {
        float[] array = { 3.0f, 1.0f, 2.0f };
        OptionalFloat result = Iterables.min(array);
        assertTrue(result.isPresent());
        assertEquals(1.0f, result.get());

        assertFalse(Iterables.min((float[]) null).isPresent());
        assertFalse(Iterables.min(new float[0]).isPresent());
    }

    @Test
    public void testMinComparableArray() {
        String[] array = { "c", "a", "b" };
        Nullable<String> result = Iterables.min(array);
        assertTrue(result.isPresent());
        assertEquals("a", result.get());

        String[] arrayWithNulls = { "c", null, "a" };
        Nullable<String> resultWithNulls = Iterables.min(arrayWithNulls);
        assertTrue(resultWithNulls.isPresent());
        assertEquals("a", resultWithNulls.get());

        assertFalse(Iterables.min((String[]) null).isPresent());
        assertFalse(Iterables.min(new String[0]).isPresent());
    }

    @Test
    public void testMinArrayWithComparator() {
        String[] array = { "aaa", "bb", "c" };
        Comparator<String> lengthComparator = Comparator.comparing(String::length);

        Nullable<String> result = Iterables.min(array, lengthComparator);
        assertTrue(result.isPresent());
        assertEquals("c", result.get());

        assertFalse(Iterables.min((String[]) null, lengthComparator).isPresent());
        assertFalse(Iterables.min(new String[0], lengthComparator).isPresent());
    }

    @Test
    public void testMinChar_Present() {
        OptionalChar result = Iterables.min('b', 'a', 'c');
        assertTrue(result.isPresent());
        assertEquals('a', result.get());
    }

    @Test
    public void testMinByte_Present() {
        OptionalByte result = Iterables.min((byte) 3, (byte) 1, (byte) 2);
        assertTrue(result.isPresent());
        assertEquals((byte) 1, result.get());
    }

    @Test
    public void testMinShort_Present() {
        OptionalShort result = Iterables.min((short) 3, (short) 1, (short) 2);
        assertTrue(result.isPresent());
        assertEquals((short) 1, result.get());
    }

    @Test
    public void testMinFloat_Present() {
        OptionalFloat result = Iterables.min(3.0f, 1.0f, 2.0f);
        assertTrue(result.isPresent());
        assertEquals(1.0f, result.get());
    }

    // ===================== min/max empty array overloads =====================

    @Test
    public void testMinIteratorWithComparator_Dedicated() {
        Nullable<Integer> result = Iterables.min(Arrays.asList(3, 1, 4, 1, 5).iterator(), Comparator.reverseOrder());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(5), result.get());
    }

    @Test
    public void testMinIterator_Dedicated() {
        Nullable<Integer> result = Iterables.min(Arrays.asList(3, 1, 4, 1, 5).iterator());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get());
    }

    @Test
    public void testMinBy_NullArray() {
        Nullable<String> result = Iterables.minBy((String[]) null, String::length);
        assertFalse(result.isPresent());
    }

    @Test
    public void testMin_Iterator_RejectsNullComparator() {
        List<Integer> list = Arrays.asList(3, 1, 4, 1, 5);
        assertThrows(IllegalArgumentException.class, () -> Iterables.min(list.iterator(), (Comparator<Integer>) null));
    }

    @Test
    public void testMin_Iterator_Empty() {
        Iterator<Integer> emptyIter = Collections.<Integer> emptyList().iterator();
        assertTrue(Iterables.min(emptyIter, Comparator.naturalOrder()).isEmpty());
    }

    @Test
    public void testMinByArray() {
        String[] arr = { "cat", "elephant", "dog", "bird" };
        Function<String, Integer> lengthExtractor = String::length;

        Nullable<String> result = Iterables.minBy(arr, lengthExtractor);
        assertTrue(result.isPresent());
        assertEquals("cat", result.get());

        assertTrue(Iterables.minBy(new String[0], lengthExtractor).isEmpty());
        assertTrue(Iterables.minBy((String[]) null, lengthExtractor).isEmpty());
    }

    @Test
    public void testMinByIterable() {
        String[] arr = { "cat", "elephant", "dog", "bird" };
        Function<String, Integer> lengthExtractor = String::length;

        Nullable<String> result = Iterables.minBy(Arrays.asList(arr), lengthExtractor);
        assertTrue(result.isPresent());
        assertEquals("cat", result.get());

        assertTrue(Iterables.minBy((Iterable<String>) null, lengthExtractor).isEmpty());
    }

    @Test
    public void testMinByIterator() {
        String[] arr = { "cat", "elephant", "dog", "bird" };
        Function<String, Integer> lengthExtractor = String::length;

        Nullable<String> result = Iterables.minBy(Arrays.asList(arr).iterator(), lengthExtractor);
        assertTrue(result.isPresent());
        assertEquals("cat", result.get());

        assertTrue(Iterables.minBy((Iterator<String>) null, lengthExtractor).isEmpty());
    }

    @Test
    public void testMinBy() {
        List<String> list = Arrays.asList("aaa", "bb", "c");
        Function<String, Integer> lengthExtractor = String::length;

        Nullable<String> result = Iterables.minBy(list, lengthExtractor);
        assertTrue(result.isPresent());
        assertEquals("c", result.get());

        String[] array = { "aaa", "bb", "c" };
        Nullable<String> arrayResult = Iterables.minBy(array, lengthExtractor);
        assertTrue(arrayResult.isPresent());
        assertEquals("c", arrayResult.get());
    }

    @Test
    public void testMinByIterator_Dedicated() {
        Nullable<String> result = Iterables.minBy(Arrays.asList("apple", "banana", "cherry").iterator(), String::length);
        assertTrue(result.isPresent());
        assertEquals("apple", result.get());
    }

    @Test
    public void testMinByIterator_Empty() {
        Nullable<String> result = Iterables.minBy(Collections.<String> emptyList().iterator(), String::length);
        assertFalse(result.isPresent());
    }

    @Test
    public void testMinBy_EmptyArray() {
        Nullable<String> result = Iterables.minBy(new String[0], String::length);
        assertFalse(result.isPresent());
    }

    @Test
    public void testMinIntArray() {
        int[] arr = { 3, 1, 4, 1, 5, 9 };
        OptionalInt result = Iterables.min(arr);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        assertFalse(Iterables.min((int[]) null).isPresent());
        assertFalse(Iterables.min(new int[0]).isPresent());
    }

    @Test
    public void testMinIntIterable() {
        ToIntFunction<String> lengthFunction = String::length;
        String[] arr = { "hi", "hello", "world" };

        OptionalInt result = Iterables.minInt(Arrays.asList(arr), lengthFunction);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        assertFalse(Iterables.minInt((Iterable<String>) null, lengthFunction).isPresent());
    }

    @Test
    public void testMinIntIterator() {
        ToIntFunction<String> lengthFunction = String::length;
        String[] arr = { "hi", "hello", "world" };

        OptionalInt result = Iterables.minInt(Arrays.asList(arr).iterator(), lengthFunction);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        assertFalse(Iterables.minInt((Iterator<String>) null, lengthFunction).isPresent());
    }

    @Test
    public void testMinIntArrayExtractor() {
        assertTrue(Iterables.minInt((String[]) null, String::length).isEmpty());
        assertTrue(Iterables.minInt(new String[0], String::length).isEmpty());
        assertEquals(1, Iterables.minInt(new String[] { "aaa", "a", "bb" }, String::length).get());
        assertEquals(3, Iterables.minInt(new String[] { "aaa" }, String::length).get());
    }

    @Test
    public void testMinInt() {
        int[] array = { 30, 10, 20 };
        OptionalInt result = Iterables.min(array);
        assertTrue(result.isPresent());
        assertEquals(10, result.get());

        assertFalse(Iterables.min((int[]) null).isPresent());
        assertFalse(Iterables.min(new int[0]).isPresent());
    }

    @Test
    public void testMinInt_Present() {
        OptionalInt result = Iterables.min(3, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
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
    public void testMinLongArray() {
        long[] arr = { 100L, 50L, 200L, 25L };
        OptionalLong result = Iterables.min(arr);
        assertTrue(result.isPresent());
        assertEquals(25L, result.getAsLong());

        assertFalse(Iterables.min((long[]) null).isPresent());
        assertFalse(Iterables.min(new long[0]).isPresent());
    }

    @Test
    public void testMinLongIterable() {
        ToLongFunction<Integer> toLong = i -> i.longValue();

        OptionalLong result = Iterables.minLong(intList, toLong);
        assertTrue(result.isPresent());
        assertEquals(1L, result.getAsLong());

        assertFalse(Iterables.minLong((Iterable<Integer>) null, toLong).isPresent());
    }

    @Test
    public void testMinLongIterator() {
        ToLongFunction<Integer> toLong = i -> i.longValue();

        OptionalLong result = Iterables.minLong(intList.iterator(), toLong);
        assertTrue(result.isPresent());
        assertEquals(1L, result.getAsLong());

        assertFalse(Iterables.minLong((Iterator<Integer>) null, toLong).isPresent());
    }

    @Test
    public void testMinLongArrayExtractor() {
        ToIntFunction<String> lengthToInt = String::length;
        ToLongFunction<String> lengthToLong = s -> (long) lengthToInt.applyAsInt(s);
        assertTrue(Iterables.minLong((String[]) null, lengthToLong).isEmpty());
        assertTrue(Iterables.minLong(new String[0], lengthToLong).isEmpty());
        assertEquals(1L, Iterables.minLong(new String[] { "aaa", "a", "bb" }, lengthToLong).get());
    }

    @Test
    public void testMinLong() {
        long[] array = { 30L, 10L, 20L };
        OptionalLong result = Iterables.min(array);
        assertTrue(result.isPresent());
        assertEquals(10L, result.get());

        assertFalse(Iterables.min((long[]) null).isPresent());
        assertFalse(Iterables.min(new long[0]).isPresent());
    }

    @Test
    public void testMinLong_Present() {
        OptionalLong result = Iterables.min(3L, 1L, 2L);
        assertTrue(result.isPresent());
        assertEquals(1L, result.get());
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
    public void testMinDoubleArray() {
        double[] arr = { 3.14, 1.41, 2.71 };
        OptionalDouble result = Iterables.min(arr);
        assertTrue(result.isPresent());
        assertEquals(1.41, result.getAsDouble(), 0.001);

        assertFalse(Iterables.min((double[]) null).isPresent());
        assertFalse(Iterables.min(new double[0]).isPresent());
    }

    @Test
    public void testMinDoubleIterable() {
        ToDoubleFunction<Integer> toDouble = i -> i.doubleValue();

        OptionalDouble result = Iterables.minDouble(intList, toDouble);
        assertTrue(result.isPresent());
        assertEquals(1.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.minDouble((Iterable<Integer>) null, toDouble).isPresent());
    }

    @Test
    public void testMinDoubleIterator() {
        ToDoubleFunction<Integer> toDouble = i -> i.doubleValue();

        OptionalDouble result = Iterables.minDouble(intList.iterator(), toDouble);
        assertTrue(result.isPresent());
        assertEquals(1.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.minDouble((Iterator<Integer>) null, toDouble).isPresent());
    }

    @Test
    public void testMinDoubleArrayExtractor() {
        ToDoubleFunction<String> lengthToDouble = s -> (double) s.length();
        assertTrue(Iterables.minDouble((String[]) null, lengthToDouble).isEmpty());
        assertTrue(Iterables.minDouble(new String[0], lengthToDouble).isEmpty());
        assertEquals(1.0, Iterables.minDouble(new String[] { "aaa", "a", "bb" }, lengthToDouble).get());
    }

    @Test
    public void testMinDouble_Present() {
        OptionalDouble result = Iterables.min(3.0, 1.0, 2.0);
        assertTrue(result.isPresent());
        assertEquals(1.0, result.get());
    }

    // ===================== minInt/maxInt/minLong/maxLong/minDouble/maxDouble Iterator overloads =====================

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

    @Test
    public void testMinMaxArray() {
        Integer[] arr = { 3, 1, 4, 1, 5, 9 };
        Optional<Pair<Integer, Integer>> result = Iterables.minMax(arr);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get().left());
        assertEquals(Integer.valueOf(9), result.get().right());

        assertFalse(Iterables.minMax(new Integer[0]).isPresent());
        assertFalse(Iterables.minMax((Integer[]) null).isPresent());
    }

    @Test
    public void testMinMaxArrayWithComparator() {
        String[] arr = { "apple", "banana", "cherry" };
        Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
        Optional<Pair<String, String>> result = Iterables.minMax(arr, lengthComparator);
        assertTrue(result.isPresent());
        assertEquals("apple", result.get().left());
        assertEquals("banana", result.get().right());

        assertFalse(Iterables.minMax(new String[0], lengthComparator).isPresent());
    }

    @Test
    public void testMinMaxIterable() {
        Optional<Pair<Integer, Integer>> result = Iterables.minMax(intList);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get().left());
        assertEquals(Integer.valueOf(9), result.get().right());

        assertFalse(Iterables.minMax((Iterable<Integer>) null).isPresent());
        assertFalse(Iterables.minMax(new ArrayList<Integer>()).isPresent());
    }

    @Test
    public void testMinMaxIterableWithComparator() {
        Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
        Optional<Pair<String, String>> result = Iterables.minMax(stringList, lengthComparator);
        assertTrue(result.isPresent());
        assertEquals("date", result.get().left());
        assertEquals("banana", result.get().right());
    }

    @Test
    public void testMinMaxIterator() {
        Optional<Pair<Integer, Integer>> result = Iterables.minMax(intList.iterator());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get().left());
        assertEquals(Integer.valueOf(9), result.get().right());

        assertFalse(Iterables.minMax((Iterator<Integer>) null).isPresent());
    }

    @Test
    public void testMinMaxIteratorWithComparator() {
        Comparator<Integer> reverseComparator = Comparator.reverseOrder();
        Optional<Pair<Integer, Integer>> result = Iterables.minMax(intList.iterator(), reverseComparator);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(9), result.get().left());
        assertEquals(Integer.valueOf(1), result.get().right());
    }

    @Test
    public void testMinMaxArrayComparable() {
        assertTrue(Iterables.minMax((String[]) null).isEmpty());
        assertTrue(Iterables.minMax(new String[0]).isEmpty());
        Pair<String, String> result = Iterables.minMax(new String[] { "b", "a", "c" }).get();
        assertEquals("a", result.left());
        assertEquals("c", result.right());
        result = Iterables.minMax(new String[] { "a" }).get();
        assertEquals("a", result.left());
        assertEquals("a", result.right());
        result = Iterables.minMax(new String[] { "b", null, "a", "c" }).get();
        assertEquals("a", result.left());
        assertEquals("c", result.right());
    }

    @Test
    public void testMinMaxArrayComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.minMax((String[]) null, reverseOrder).isEmpty());
        assertTrue(Iterables.minMax(new String[0], reverseOrder).isEmpty());
        Pair<String, String> result = Iterables.minMax(new String[] { "b", "a", "c" }, reverseOrder).get();
        assertEquals("c", result.left());
        assertEquals("a", result.right());
    }

    @Test
    public void testMinMaxIterableComparable() {
        assertTrue(Iterables.minMax((Iterable<String>) null).isEmpty());
        assertTrue(Iterables.minMax(list()).isEmpty());
        Pair<String, String> result = Iterables.minMax(list("b", "a", "c")).get();
        assertEquals("a", result.left());
        assertEquals("c", result.right());
    }

    @Test
    public void testMinMaxIterableComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.minMax((Iterable<String>) null, reverseOrder).isEmpty());
        assertTrue(Iterables.minMax(list(), reverseOrder).isEmpty());
        Pair<String, String> result = Iterables.minMax(list("b", "a", "c"), reverseOrder).get();
        assertEquals("c", result.left());
        assertEquals("a", result.right());
    }

    @Test
    public void testMinMaxIteratorComparable() {
        assertTrue(Iterables.minMax((Iterator<String>) null).isEmpty());
        assertTrue(Iterables.minMax(list(CommonUtil.EMPTY_STRING_ARRAY).iterator()).isEmpty());
        Pair<String, String> result = Iterables.minMax(list("b", "a", "c").iterator()).get();
        assertEquals("a", result.left());
        assertEquals("c", result.right());
    }

    @Test
    public void testMinMaxIteratorComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.minMax((Iterator<String>) null, reverseOrder).isEmpty());
        assertTrue(Iterables.minMax(list(CommonUtil.EMPTY_STRING_ARRAY).iterator(), reverseOrder).isEmpty());
        Pair<String, String> result = Iterables.minMax(list("b", "a", "c").iterator(), reverseOrder).get();
        assertEquals("c", result.left());
        assertEquals("a", result.right());
    }

    @Test
    public void testMinMaxWithComparator() {
        List<String> list = Arrays.asList("a", "bb", "ccc");
        Comparator<String> lengthComparator = Comparator.comparing(String::length);

        Optional<Pair<String, String>> result = Iterables.minMax(list, lengthComparator);
        assertTrue(result.isPresent());
        assertEquals("a", result.get().left());
        assertEquals("ccc", result.get().right());
    }

    @Test
    public void testMinMax() {
        Integer[] arr = { 3, 1, 4, 1, 5, 9 };
        Optional<Pair<Integer, Integer>> result = Iterables.minMax(arr);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get().left());
        assertEquals(Integer.valueOf(9), result.get().right());

        assertFalse(Iterables.minMax(new Integer[0]).isPresent());
    }

    @Test
    public void testMinMaxIterator_Dedicated() {
        u.Optional<Pair<Integer, Integer>> result = Iterables.minMax(Arrays.asList(3, 1, 4, 1, 5).iterator());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get().left());
        assertEquals(Integer.valueOf(5), result.get().right());
    }

    @Test
    public void testMinMaxIterator_Empty() {
        u.Optional<Pair<Integer, Integer>> result = Iterables.minMax(Collections.<Integer> emptyList().iterator());
        assertFalse(result.isPresent());
    }

    @Test
    public void testMinMaxIteratorWithComparator_Dedicated() {
        u.Optional<Pair<Integer, Integer>> result = Iterables.minMax(Arrays.asList(3, 1, 4).iterator(), Comparator.reverseOrder());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(4), result.get().left());
        assertEquals(Integer.valueOf(1), result.get().right());
    }

    @Test
    public void testMinMax_SingleElement() {
        Optional<Pair<Integer, Integer>> result = Iterables.minMax(new Integer[] { 42 });
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(42), result.get().left());
        assertEquals(Integer.valueOf(42), result.get().right());
    }

    @Test
    public void testMinMax_NullArray() {
        Optional<Pair<Integer, Integer>> result = Iterables.minMax((Integer[]) null);
        assertFalse(result.isPresent());
    }

    @Test
    public void testMinMax_EmptyIterator() {
        Optional<Pair<Integer, Integer>> result = Iterables.minMax(Collections.<Integer> emptyList().iterator());
        assertFalse(result.isPresent());
    }

    @Test
    public void testMin_NullsFirstShortCircuits() {
        Iterator<String> it = Arrays.asList("b", null, "a").iterator();
        Nullable<String> r = Iterables.min(it, Comparator.nullsFirst(Comparator.<String> naturalOrder()));
        assertTrue(r.isPresent());
        assertNull(r.get());
    }

    @Test
    public void testMinMaxNaturalOrderingIgnoresNullWhenNonNullExists() {
        final List<Integer> withNull = Arrays.asList(3, null, 1, 2);

        final Optional<Pair<Integer, Integer>> mm = Iterables.minMax(withNull);
        assertTrue(mm.isPresent());
        assertEquals(Integer.valueOf(1), mm.get().left());
        assertEquals(Integer.valueOf(3), mm.get().right());

        assertEquals(Integer.valueOf(1), Iterables.min(withNull).get());

        final Optional<Pair<Integer, Integer>> allNull = Iterables.minMax(new Integer[] { null, null });
        assertTrue(allNull.isPresent());
        assertEquals(Pair.of(null, null), allNull.get());
    }

    @Test
    public void testMinIterator_nullsFirstComparatorLeavesIteratorPartiallyConsumed() {
        final List<String> list = Arrays.asList("b", null, "z", "q");

        final Iterator<String> shortCircuit = list.iterator();
        assertEquals(Nullable.of((String) null), Iterables.min(shortCircuit, Comparators.<String> nullsFirst()));
        final List<String> remaining = new ArrayList<>();
        shortCircuit.forEachRemaining(remaining::add);
        assertEquals(Arrays.asList("z", "q"), remaining, "documented: the iterator is left partially consumed");

        final Iterator<String> drained = list.iterator();
        assertEquals(Nullable.of("b"), Iterables.min(drained, Comparators.<String> nullsLast()));
        assertFalse(drained.hasNext(), "every other comparator drains the iterator");
    }

    @Test
    public void testMinMaxIterator_firstElementNullIsStillHandled() {
        assertEquals(Nullable.of((String) null), Iterables.min(Arrays.asList((String) null, "a").iterator(), Comparators.<String> nullsFirst()));
        assertEquals(Nullable.of("a"), Iterables.min(Arrays.asList((String) null, "a").iterator(), Comparators.<String> nullsLast()));
        assertEquals(Nullable.of((String) null), Iterables.max(Arrays.asList((String) null, "a").iterator(), Comparators.<String> nullsLast()));
        assertEquals(Nullable.of("a"), Iterables.max(Arrays.asList((String) null, "a").iterator(), Comparators.<String> nullsFirst()));
    }

    @Test
    public void testMinNaN_arrayAndExtractorGenuinelyDiverge() {
        final Double[] boxed = { 1.0, Double.NaN, 3.0 };

        // Math.min propagates NaN ...
        assertTrue(Double.isNaN(Iterables.min(new double[] { 1.0, Double.NaN, 3.0 }).get()));
        assertTrue(Float.isNaN(Iterables.min(new float[] { 1.0f, Float.NaN, 3.0f }).get()));

        // ... while the total order makes NaN the greatest value, so minDouble prefers the non-NaN one
        assertEquals(1.0, Iterables.minDouble(boxed, d -> d).get());
        assertEquals(1.0, Iterables.minDouble(Arrays.asList(boxed), d -> d).get());
        assertEquals(1.0, Iterables.minDouble(Arrays.asList(boxed).iterator(), d -> d).get());
    }

    @Test
    public void testMinMax_comparatorOverloadsDoNotSkipNulls() {
        final Integer[] a = { 3, null, 1 };

        // the natural-ordering overloads skip nulls ...
        assertEquals(Pair.of(1, 3), Iterables.minMax(a).get());
        assertEquals(Pair.of(1, 3), Iterables.minMax(Arrays.asList(a)).get());
        assertEquals(Pair.of(1, 3), Iterables.minMax(Arrays.asList(a).iterator()).get());

        // ... the comparator overloads do not, so a comparator that is not null-safe throws
        assertThrows(NullPointerException.class, () -> Iterables.minMax(a, Comparator.<Integer> naturalOrder()));
        assertThrows(NullPointerException.class, () -> Iterables.minMax(Arrays.asList(a), Comparator.<Integer> naturalOrder()));
        assertThrows(NullPointerException.class, () -> Iterables.minMax(Arrays.asList(a).iterator(), Comparator.<Integer> naturalOrder()));

        // ... and a null-safe one gives nulls a defined position, as the javadoc suggests
        assertEquals(Pair.of(null, 3), Iterables.minMax(a, Comparators.<Integer> nullsFirst()).get());
        assertEquals(Pair.of(1, null), Iterables.minMax(a, Comparators.<Integer> nullsLast()).get());
    }

    @Test
    public void testMinIterator_shortCircuitCoversNaturalOrderToo() {
        // Comparators.naturalOrder() and Comparators.nullsFirst() are the same instance, so both short-circuit
        for (final Comparator<String> cmp : Arrays.asList(Comparators.<String> naturalOrder(), Comparators.<String> nullsFirst())) {
            final Iterator<String> iter = Arrays.asList("b", null, "a", "zzz").iterator();
            assertNull(Iterables.min(iter, cmp).get());
            assertTrue(iter.hasNext(), "the iterator must be left only partially consumed");
            assertEquals("a", iter.next());
        }

        // any other comparator drains it
        final Iterator<String> drained = Arrays.asList("b", "a", "zzz").iterator();
        assertEquals("a", Iterables.min(drained, Comparator.<String> naturalOrder()).get());
        assertFalse(drained.hasNext());
    }

    @Test
    public void testMinIterator_nullFirstElementShortCircuitsToo() {
        // The short-circuit tests the running candidate, so a null *first* element settles the answer as soon
        // as one more element has been read - the same point N.min(Iterator, Comparator) stops at.
        final Iterator<String> iter = Arrays.asList(null, "a", "b").iterator();
        assertNull(Iterables.min(iter, Comparators.<String> nullsFirst()).get());
        assertTrue(iter.hasNext(), "a null first element settles the minimum; the rest must stay unread");
        assertEquals("b", iter.next());
    }
}
