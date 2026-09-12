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

public class IterablesMaxTest extends IterablesTestSupport {
    @Test
    public void testMaxCharArray() {
        char[] arr = { 'd', 'a', 'c', 'b' };
        OptionalChar result = Iterables.max(arr);
        assertTrue(result.isPresent());
        assertEquals('d', result.get());

        assertFalse(Iterables.max((char[]) null).isPresent());
        assertFalse(Iterables.max(new char[0]).isPresent());
    }

    @Test
    public void testMaxByteArray() {
        byte[] arr = { 5, 2, 8, 1, 9 };
        OptionalByte result = Iterables.max(arr);
        assertTrue(result.isPresent());
        assertEquals((byte) 9, result.get());

        assertFalse(Iterables.max((byte[]) null).isPresent());
        assertFalse(Iterables.max(new byte[0]).isPresent());
    }

    @Test
    public void testMaxShortArray() {
        short[] arr = { 100, 50, 200, 25 };
        OptionalShort result = Iterables.max(arr);
        assertTrue(result.isPresent());
        assertEquals((short) 200, result.get());

        assertFalse(Iterables.max((short[]) null).isPresent());
        assertFalse(Iterables.max(new short[0]).isPresent());
    }

    @Test
    public void testMaxFloatArray() {
        float[] arr = { 3.14f, 1.41f, 2.71f };
        OptionalFloat result = Iterables.max(arr);
        assertTrue(result.isPresent());
        assertEquals(3.14f, result.get(), 0.001f);

        assertFalse(Iterables.max((float[]) null).isPresent());
        assertFalse(Iterables.max(new float[0]).isPresent());
    }

    @Test
    public void testMaxObjectArray() {
        Integer[] arr = { 3, 1, 4, 1, 5, 9 };
        Nullable<Integer> result = Iterables.max(arr);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(9), result.get());

        assertTrue(Iterables.max(new Integer[0]).isEmpty());
        assertTrue(Iterables.max((Integer[]) null).isEmpty());
    }

    @Test
    public void testMaxObjectArrayWithComparator() {
        String[] arr = { "apple", "banana", "cherry" };
        Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
        Nullable<String> result = Iterables.max(arr, lengthComparator);
        assertTrue(result.isPresent());
        assertEquals("banana", result.get());

        assertTrue(Iterables.max(new String[0], lengthComparator).isEmpty());
        assertTrue(Iterables.max((String[]) null, lengthComparator).isEmpty());
    }

    @Test
    public void testMaxIterable() {
        Nullable<Integer> result = Iterables.max(intList);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(9), result.get());

        assertTrue(Iterables.max((Iterable<Integer>) null).isEmpty());
        assertTrue(Iterables.max(new ArrayList<Integer>()).isEmpty());
    }

    @Test
    public void testMaxIterableWithComparator() {
        Comparator<String> reverseComparator = Comparator.reverseOrder();
        Nullable<String> result = Iterables.max(stringList, reverseComparator);
        assertTrue(result.isPresent());
        assertEquals("apple", result.get());

        assertTrue(Iterables.max((Iterable<String>) null, reverseComparator).isEmpty());
    }

    @Test
    public void testMaxIterator() {
        Nullable<Integer> result = Iterables.max(intList.iterator());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(9), result.get());

        assertTrue(Iterables.max((Iterator<Integer>) null).isEmpty());
    }

    @Test
    public void testMaxIteratorWithComparator() {
        Comparator<Integer> reverseComparator = Comparator.reverseOrder();
        Nullable<Integer> result = Iterables.max(intList.iterator(), reverseComparator);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get());
    }

    @Test
    public void testMaxGenericArray() {
        assertTrue(Iterables.max((Integer[]) null).isEmpty());
        assertTrue(Iterables.max(new Integer[0]).isEmpty());
        assertEquals(3, Iterables.max(new Integer[] { 3, 1, 2 }).orElse(null));
        assertEquals(3, Iterables.max(new Integer[] { null, 3, 1, 2 }).orElse(null));
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
    public void testMaxGenericArrayComparable() {
        assertTrue(Iterables.max((String[]) null).isEmpty());
        assertTrue(Iterables.max(new String[0]).isEmpty());
        assertEquals("c", Iterables.max(new String[] { "a", "b", "c" }).get());
        assertEquals("c", Iterables.max(new String[] { "a", null, "c" }).get());
        assertNull(Iterables.max(new String[] { null, null }).get());
    }

    @Test
    public void testMaxGenericArrayComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.max((String[]) null, reverseOrder).isEmpty());
        assertTrue(Iterables.max(new String[0], reverseOrder).isEmpty());
        assertEquals("a", Iterables.max(new String[] { "a", "b", "c" }, reverseOrder).get());
        assertEquals("c", Iterables.max(new String[] { "a", "b", "c" }, Comparator.naturalOrder()).get());
        assertEquals("c", Iterables.max(new String[] { "a", null, "c" }, Comparator.nullsFirst(Comparator.naturalOrder())).get());
        assertNull(Iterables.max(new String[] { "a", null, "c" }, Comparator.nullsLast(Comparator.naturalOrder())).get());
    }

    @Test
    public void testMaxIterableComparable() {
        assertTrue(Iterables.max((Iterable<String>) null).isEmpty());
        assertTrue(Iterables.max(list()).isEmpty());
        assertEquals("c", Iterables.max(list("a", "b", "c")).get());
        assertEquals("c", Iterables.max(list("a", null, "c")).get());
    }

    @Test
    public void testMaxIterableComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.max((Iterable<String>) null, reverseOrder).isEmpty());
        assertTrue(Iterables.max(list(), reverseOrder).isEmpty());
        assertEquals("a", Iterables.max(list("a", "b", "c"), reverseOrder).get());
        assertEquals("c", Iterables.max(list("a", "b", "c"), Comparator.naturalOrder()).get());
        assertEquals("c", Iterables.max(list("a", null, "c"), Comparator.nullsFirst(Comparator.naturalOrder())).get());
        assertNull(Iterables.max(list("a", null, "c"), Comparator.nullsLast(Comparator.naturalOrder())).get());
    }

    @Test
    public void testMaxIteratorComparable() {
        assertTrue(Iterables.max((Iterator<String>) null).isEmpty());
        assertTrue(Iterables.max(list(CommonUtil.EMPTY_STRING_ARRAY).iterator()).isEmpty());
        assertEquals("c", Iterables.max(list("a", "b", "c").iterator()).get());
        assertEquals("c", Iterables.max(list("a", null, "c").iterator()).get());
    }

    @Test
    public void testMaxIteratorComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.max((Iterator<String>) null, reverseOrder).isEmpty());
        assertTrue(Iterables.max(list(CommonUtil.EMPTY_STRING_ARRAY).iterator(), reverseOrder).isEmpty());
        assertEquals("a", Iterables.max(list("a", "b", "c").iterator(), reverseOrder).get());
        assertEquals("c", Iterables.max(list("a", "b", "c").iterator(), Comparator.naturalOrder()).get());
        assertEquals("c", Iterables.max(list("a", null, "c").iterator(), Comparator.nullsFirst(Comparator.naturalOrder())).get());
        assertNull(Iterables.max(list("a", null, "c").iterator(), Comparator.nullsLast(Comparator.naturalOrder())).get());
    }

    @Test
    public void testMaxChar() {
        char[] array = { 'c', 'a', 'b' };
        OptionalChar result = Iterables.max(array);
        assertTrue(result.isPresent());
        assertEquals('c', result.get());

        assertFalse(Iterables.max((char[]) null).isPresent());
        assertFalse(Iterables.max(new char[0]).isPresent());
    }

    @Test
    public void testMaxComparableArray() {
        String[] array = { "c", "a", "b" };
        Nullable<String> result = Iterables.max(array);
        assertTrue(result.isPresent());
        assertEquals("c", result.get());

        String[] arrayWithNulls = { "c", null, "a" };
        Nullable<String> resultWithNulls = Iterables.max(arrayWithNulls);
        assertTrue(resultWithNulls.isPresent());
        assertEquals("c", resultWithNulls.get());
    }

    @Test
    public void testMaxChar_Present() {
        OptionalChar result = Iterables.max('b', 'a', 'c');
        assertTrue(result.isPresent());
        assertEquals('c', result.get());
    }

    @Test
    public void testMaxByte_Present() {
        OptionalByte result = Iterables.max((byte) 3, (byte) 1, (byte) 2);
        assertTrue(result.isPresent());
        assertEquals((byte) 3, result.get());
    }

    @Test
    public void testMaxShort_Present() {
        OptionalShort result = Iterables.max((short) 3, (short) 1, (short) 2);
        assertTrue(result.isPresent());
        assertEquals((short) 3, result.get());
    }

    @Test
    public void testMaxFloat_Present() {
        OptionalFloat result = Iterables.max(3.0f, 1.0f, 2.0f);
        assertTrue(result.isPresent());
        assertEquals(3.0f, result.get());
    }

    @Test
    public void testMaxIteratorWithComparator_Dedicated() {
        Nullable<Integer> result = Iterables.max(Arrays.asList(3, 1, 4, 1, 5).iterator(), Comparator.reverseOrder());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get());
    }

    @Test
    public void testMaxIterator_Dedicated() {
        Nullable<Integer> result = Iterables.max(Arrays.asList(3, 1, 4, 1, 5).iterator());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(5), result.get());
    }

    @Test
    public void testMax_Iterator_RejectsNullComparator() {
        List<Integer> list = Arrays.asList(3, 1, 4, 1, 5);
        assertThrows(IllegalArgumentException.class, () -> Iterables.max(list.iterator(), (Comparator<Integer>) null));
    }

    @Test
    public void testMax_Iterator_Empty() {
        Iterator<Integer> emptyIter = Collections.<Integer> emptyList().iterator();
        assertTrue(Iterables.max(emptyIter, Comparator.naturalOrder()).isEmpty());
    }

    @Test
    public void testMaxByArray() {
        String[] arr = { "cat", "elephant", "dog", "bird" };
        Function<String, Integer> lengthExtractor = String::length;

        Nullable<String> result = Iterables.maxBy(arr, lengthExtractor);
        assertTrue(result.isPresent());
        assertEquals("elephant", result.get());

        assertTrue(Iterables.maxBy(new String[0], lengthExtractor).isEmpty());
        assertTrue(Iterables.maxBy((String[]) null, lengthExtractor).isEmpty());
    }

    @Test
    public void testMaxByIterable() {
        String[] arr = { "cat", "elephant", "dog", "bird" };
        Function<String, Integer> lengthExtractor = String::length;

        Nullable<String> result = Iterables.maxBy(Arrays.asList(arr), lengthExtractor);
        assertTrue(result.isPresent());
        assertEquals("elephant", result.get());

        assertTrue(Iterables.maxBy((Iterable<String>) null, lengthExtractor).isEmpty());
    }

    @Test
    public void testMaxByIterator() {
        String[] arr = { "cat", "elephant", "dog", "bird" };
        Function<String, Integer> lengthExtractor = String::length;

        Nullable<String> result = Iterables.maxBy(Arrays.asList(arr).iterator(), lengthExtractor);
        assertTrue(result.isPresent());
        assertEquals("elephant", result.get());

        assertTrue(Iterables.maxBy((Iterator<String>) null, lengthExtractor).isEmpty());
    }

    @Test
    public void testMaxBy() {
        List<String> list = Arrays.asList("a", "bb", "ccc");
        Function<String, Integer> lengthExtractor = String::length;

        Nullable<String> result = Iterables.maxBy(list, lengthExtractor);
        assertTrue(result.isPresent());
        assertEquals("ccc", result.get());
    }

    @Test
    public void testMaxByIterator_Dedicated() {
        Nullable<String> result = Iterables.maxBy(Arrays.asList("apple", "banana", "cherry").iterator(), String::length);
        assertTrue(result.isPresent());
        assertEquals("banana", result.get());
    }

    @Test
    public void testMaxByIterator_Empty() {
        Nullable<String> result = Iterables.maxBy(Collections.<String> emptyList().iterator(), String::length);
        assertFalse(result.isPresent());
    }

    @Test
    public void testMaxBy_NullArray() {
        Nullable<String> result = Iterables.maxBy((String[]) null, String::length);
        assertFalse(result.isPresent());
    }

    @Test
    public void testMaxBy_EmptyArray() {
        Nullable<String> result = Iterables.maxBy(new String[0], String::length);
        assertFalse(result.isPresent());
    }

    @Test
    public void testMaxIntArray() {
        int[] arr = { 3, 1, 4, 1, 5, 9 };
        OptionalInt result = Iterables.max(arr);
        assertTrue(result.isPresent());
        assertEquals(9, result.getAsInt());

        assertFalse(Iterables.max((int[]) null).isPresent());
        assertFalse(Iterables.max(new int[0]).isPresent());
    }

    @Test
    public void testMaxIntIterable() {
        ToIntFunction<String> lengthFunction = String::length;
        String[] arr = { "hi", "hello", "world" };

        OptionalInt result = Iterables.maxInt(Arrays.asList(arr), lengthFunction);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());

        assertFalse(Iterables.maxInt((Iterable<String>) null, lengthFunction).isPresent());
    }

    @Test
    public void testMaxIntIterator() {
        ToIntFunction<String> lengthFunction = String::length;
        String[] arr = { "hi", "hello", "world" };

        OptionalInt result = Iterables.maxInt(Arrays.asList(arr).iterator(), lengthFunction);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());

        assertFalse(Iterables.maxInt((Iterator<String>) null, lengthFunction).isPresent());
    }

    @Test
    public void testMaxIntArrayExtractor() {
        assertTrue(Iterables.maxInt((String[]) null, String::length).isEmpty());
        assertTrue(Iterables.maxInt(new String[0], String::length).isEmpty());
        assertEquals(3, Iterables.maxInt(new String[] { "aaa", "a", "bb" }, String::length).get());
    }

    @Test
    public void testMaxIntIterableExtractor() {
        assertTrue(Iterables.maxInt((List<String>) null, String::length).isEmpty());
        assertTrue(Iterables.maxInt(list(), String::length).isEmpty());
        assertEquals(3, Iterables.maxInt(list("aaa", "a", "bb"), String::length).get());
    }

    @Test
    public void testMaxIntIteratorExtractor() {
        assertTrue(Iterables.maxInt((Iterator<String>) null, String::length).isEmpty());
        assertTrue(Iterables.maxInt(list(CommonUtil.EMPTY_STRING_ARRAY).iterator(), String::length).isEmpty());
        assertEquals(3, Iterables.maxInt(list("aaa", "a", "bb").iterator(), String::length).get());
    }

    @Test
    public void testMaxInt() {
        int[] array = { 30, 10, 20 };
        OptionalInt result = Iterables.max(array);
        assertTrue(result.isPresent());
        assertEquals(30, result.get());
    }

    @Test
    public void testMaxInt_valueExtractor() {
        List<String> list = Arrays.asList("a", "bb", "ccc");
        ToIntFunction<String> lengthExtractor = String::length;

        OptionalInt result = Iterables.maxInt(list, lengthExtractor);
        assertTrue(result.isPresent());
        assertEquals(3, result.get());
    }

    @Test
    public void testMaxInt_Present() {
        OptionalInt result = Iterables.max(3, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.get());
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
    public void testMaxLongArray() {
        long[] arr = { 100L, 50L, 200L, 25L };
        OptionalLong result = Iterables.max(arr);
        assertTrue(result.isPresent());
        assertEquals(200L, result.getAsLong());

        assertFalse(Iterables.max((long[]) null).isPresent());
        assertFalse(Iterables.max(new long[0]).isPresent());
    }

    @Test
    public void testMaxLongIterable() {
        ToLongFunction<Integer> toLong = i -> i.longValue();

        OptionalLong result = Iterables.maxLong(intList, toLong);
        assertTrue(result.isPresent());
        assertEquals(9L, result.getAsLong());

        assertFalse(Iterables.maxLong((Iterable<Integer>) null, toLong).isPresent());
    }

    @Test
    public void testMaxLongIterator() {
        ToLongFunction<Integer> toLong = i -> i.longValue();

        OptionalLong result = Iterables.maxLong(intList.iterator(), toLong);
        assertTrue(result.isPresent());
        assertEquals(9L, result.getAsLong());

        assertFalse(Iterables.maxLong((Iterator<Integer>) null, toLong).isPresent());
    }

    @Test
    public void testMaxLongArrayExtractor() {
        ToLongFunction<String> lengthToLong = s -> (long) s.length();
        assertTrue(Iterables.maxLong((String[]) null, lengthToLong).isEmpty());
        assertTrue(Iterables.maxLong(new String[0], lengthToLong).isEmpty());
        assertEquals(3L, Iterables.maxLong(new String[] { "aaa", "a", "bb" }, lengthToLong).get());
    }

    @Test
    public void testMaxLongIterableExtractor() {
        ToLongFunction<String> lengthToLong = s -> (long) s.length();
        assertTrue(Iterables.maxLong((List<String>) null, lengthToLong).isEmpty());
        assertTrue(Iterables.maxLong(list(), lengthToLong).isEmpty());
        assertEquals(3L, Iterables.maxLong(list("aaa", "a", "bb"), lengthToLong).get());
    }

    @Test
    public void testMaxLongIteratorExtractor() {
        ToLongFunction<String> lengthToLong = s -> (long) s.length();
        assertTrue(Iterables.maxLong((Iterator<String>) null, lengthToLong).isEmpty());
        assertTrue(Iterables.maxLong(list(CommonUtil.EMPTY_STRING_ARRAY).iterator(), lengthToLong).isEmpty());
        assertEquals(3L, Iterables.maxLong(list("aaa", "a", "bb").iterator(), lengthToLong).get());
    }

    @Test
    public void testMaxLong_Present() {
        OptionalLong result = Iterables.max(3L, 1L, 2L);
        assertTrue(result.isPresent());
        assertEquals(3L, result.get());
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
    public void testMaxDoubleArray() {
        double[] arr = { 3.14, 1.41, 2.71 };
        OptionalDouble result = Iterables.max(arr);
        assertTrue(result.isPresent());
        assertEquals(3.14, result.getAsDouble(), 0.001);

        assertFalse(Iterables.max((double[]) null).isPresent());
        assertFalse(Iterables.max(new double[0]).isPresent());
    }

    @Test
    public void testMaxDoubleIterable() {
        ToDoubleFunction<Integer> toDouble = i -> i.doubleValue();

        OptionalDouble result = Iterables.maxDouble(intList, toDouble);
        assertTrue(result.isPresent());
        assertEquals(9.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.maxDouble((Iterable<Integer>) null, toDouble).isPresent());
    }

    @Test
    public void testMaxDoubleIterator() {
        ToDoubleFunction<Integer> toDouble = i -> i.doubleValue();

        OptionalDouble result = Iterables.maxDouble(intList.iterator(), toDouble);
        assertTrue(result.isPresent());
        assertEquals(9.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.maxDouble((Iterator<Integer>) null, toDouble).isPresent());
    }

    @Test
    public void testMaxDoubleArrayExtractor() {
        ToDoubleFunction<String> lengthToDouble = s -> (double) s.length();
        assertTrue(Iterables.maxDouble((String[]) null, lengthToDouble).isEmpty());
        assertTrue(Iterables.maxDouble(new String[0], lengthToDouble).isEmpty());
        assertEquals(3.0, Iterables.maxDouble(new String[] { "aaa", "a", "bb" }, lengthToDouble).get());
    }

    @Test
    public void testMaxDoubleIterableExtractor() {
        ToDoubleFunction<String> lengthToDouble = s -> (double) s.length();
        assertTrue(Iterables.maxDouble((List<String>) null, lengthToDouble).isEmpty());
        assertTrue(Iterables.maxDouble(list(), lengthToDouble).isEmpty());
        assertEquals(3.0, Iterables.maxDouble(list("aaa", "a", "bb"), lengthToDouble).get());
    }

    @Test
    public void testMaxDoubleIteratorExtractor() {
        ToDoubleFunction<String> lengthToDouble = s -> (double) s.length();
        assertTrue(Iterables.maxDouble((Iterator<String>) null, lengthToDouble).isEmpty());
        assertTrue(Iterables.maxDouble(list(CommonUtil.EMPTY_STRING_ARRAY).iterator(), lengthToDouble).isEmpty());
        assertEquals(3.0, Iterables.maxDouble(list("aaa", "a", "bb").iterator(), lengthToDouble).get());
    }

    @Test
    public void testMaxDouble_Present() {
        OptionalDouble result = Iterables.max(3.0, 1.0, 2.0);
        assertTrue(result.isPresent());
        assertEquals(3.0, result.get());
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

    @Test
    public void testMaxIterator_nullsLastComparatorLeavesIteratorPartiallyConsumed() {
        final List<String> list = Arrays.asList("b", null, "z", "q");

        final Iterator<String> shortCircuit = list.iterator();
        assertEquals(Nullable.of((String) null), Iterables.max(shortCircuit, Comparators.<String> nullsLast()));
        final List<String> remaining = new ArrayList<>();
        shortCircuit.forEachRemaining(remaining::add);
        assertEquals(Arrays.asList("z", "q"), remaining);

        final Iterator<String> drained = list.iterator();
        assertEquals(Nullable.of("z"), Iterables.max(drained, Comparators.<String> nullsFirst()));
        assertFalse(drained.hasNext());
    }

    @Test
    public void testMaxNaN_arrayAndExtractorAgree() {
        final Double[] boxed = { 1.0, Double.NaN, 3.0 };
        final Float[] boxedFloats = { 1.0f, Float.NaN, 3.0f };

        assertTrue(Double.isNaN(Iterables.max(new double[] { 1.0, Double.NaN, 3.0 }).get()));
        assertTrue(Double.isNaN(Iterables.maxDouble(boxed, d -> d).get()));
        assertTrue(Double.isNaN(Iterables.maxDouble(Arrays.asList(boxed), d -> d).get()));
        assertTrue(Double.isNaN(Iterables.maxDouble(Arrays.asList(boxed).iterator(), d -> d).get()));
        assertTrue(Float.isNaN(Iterables.max(new float[] { 1.0f, Float.NaN, 3.0f }).get()));
        assertTrue(Double.isNaN(Iterables.maxDouble(boxedFloats, f -> f).get()));

        // NaN is greater than +Infinity under the total order too
        assertTrue(Double.isNaN(Iterables.maxDouble(new Double[] { Double.POSITIVE_INFINITY, Double.NaN }, d -> d).get()));
    }

    @Test
    public void testMaxIterator_shortCircuitIncludingTheFirstElement() {
        final Iterator<String> iter = Arrays.asList("b", null, "a", "zzz").iterator();
        assertNull(Iterables.max(iter, Comparators.<String> nullsLast()).get());
        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());

        // A null first element settles the maximum too, once one more element has been read.
        final Iterator<String> first = Arrays.asList(null, "a", "b").iterator();
        assertNull(Iterables.max(first, Comparators.<String> nullsLast()).get());
        assertTrue(first.hasNext());
        assertEquals("b", first.next());

        // max(Iterator) itself uses the nulls-first comparator, under which null is the smallest: no short-circuit
        final Iterator<String> natural = Arrays.asList("b", null, "a").iterator();
        assertEquals("b", Iterables.max(natural).get());
        assertFalse(natural.hasNext());
    }
}
