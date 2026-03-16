package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.landawn.abacus.AbstractTest;
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
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;

@Tag("old-test")
public class IterablesTest extends AbstractTest {

    private static final Comparator<Integer> REVERSE_ORDER_NULLS_FIRST = Comparator.nullsFirst(Comparator.reverseOrder());
    private static final Comparator<Integer> REVERSE_ORDER_NULLS_LAST = Comparator.nullsLast(Comparator.reverseOrder());

    private List<Integer> intList;
    private List<String> stringList;
    private List<Double> doubleList;
    private List<BigInteger> bigIntList;
    private List<BigDecimal> bigDecimalList;

    @BeforeEach
    public void setUp() {
        intList = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
        stringList = Arrays.asList("apple", "banana", "cherry", "date");
        doubleList = Arrays.asList(3.14, 2.71, 1.41, 1.73);
        bigIntList = Arrays.asList(BigInteger.valueOf(100), BigInteger.valueOf(200), BigInteger.valueOf(300));
        bigDecimalList = Arrays.asList(BigDecimal.valueOf(10.5), BigDecimal.valueOf(20.5), BigDecimal.valueOf(30.5));
    }

    @SafeVarargs
    private static <T> List<T> list(T... elements) {
        return new ArrayList<>(Arrays.asList(elements));
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
            final List<Integer> reversed = CommonUtil.reverseToList(list);
            assertEquals(CommonUtil.toList(5, 4, 3, 2, 1), reversed);
        }

        {
            final Collection<Integer> c = CommonUtil.toLinkedHashSet(1, 2, 3, 4, 5);
            CommonUtil.reverse(c);
            assertEquals(CommonUtil.toLinkedHashSet(5, 4, 3, 2, 1), c);
        }
    }

    @Test
    public void testFirstNonNullTwoArgs() {
        assertEquals("a", Iterables.firstNonNull("a", "b"));
        assertEquals("b", Iterables.firstNonNull(null, "b"));
        assertEquals("a", Iterables.firstNonNull("a", null));
        assertNull(Iterables.firstNonNull(null, null));
    }

    @Test
    public void testFirstNonNullThreeArgs() {
        assertEquals("a", Iterables.firstNonNull("a", "b", "c"));
        assertEquals("b", Iterables.firstNonNull(null, "b", "c"));
        assertEquals("c", Iterables.firstNonNull(null, null, "c"));
        assertNull(Iterables.firstNonNull(null, null, null));
    }

    @Test
    public void testFirstNonNullVarArgs() {
        String[] arr = { null, null, "found", "second" };
        assertEquals("found", Iterables.firstNonNull(arr));

        String[] allNull = { null, null, null };
        assertNull(Iterables.firstNonNull(allNull));

        assertNull(Iterables.firstNonNull((String[]) null));
        assertNull(Iterables.firstNonNull(new String[0]));
    }

    @Test
    public void testFirstNonNullIterable() {
        List<String> list = Arrays.asList(null, null, "found", "second");
        assertEquals("found", Iterables.firstNonNull(list));

        List<String> allNull = Arrays.asList(null, null, null);
        assertNull(Iterables.firstNonNull(allNull));

        assertNull(Iterables.firstNonNull((Iterable<String>) null));
        assertNull(Iterables.firstNonNull(new ArrayList<String>()));
    }

    @Test
    public void testFirstNonNullIterator() {
        List<String> list = Arrays.asList(null, null, "found", "second");
        assertEquals("found", Iterables.firstNonNull(list.iterator()));

        List<String> allNull = Arrays.asList(null, null, null);
        assertNull(Iterables.firstNonNull(allNull.iterator()));

        assertNull(Iterables.firstNonNull((Iterator<String>) null));
    }

    @Test
    public void testFirstNonNullOrDefaultIterable() {
        List<String> list = Arrays.asList(null, null, "found", "second");
        assertEquals("found", CommonUtil.firstNonNullOrDefault(list, "default"));

        List<String> allNull = Arrays.asList(null, null, null);
        assertEquals("default", CommonUtil.firstNonNullOrDefault(allNull, "default"));

        assertEquals("default", CommonUtil.firstNonNullOrDefault((Iterable<String>) null, "default"));
        assertEquals("default", CommonUtil.firstNonNullOrDefault(new ArrayList<String>(), "default"));
    }

    @Test
    public void testFirstNonNullOrDefaultIterator() {
        List<String> list = Arrays.asList(null, null, "found", "second");
        assertEquals("found", CommonUtil.firstNonNullOrDefault(list.iterator(), "default"));

        List<String> allNull = Arrays.asList(null, null, null);
        assertEquals("default", CommonUtil.firstNonNullOrDefault(allNull.iterator(), "default"));

        assertEquals("default", CommonUtil.firstNonNullOrDefault((Iterator<String>) null, "default"));
    }

    @Test
    public void testLastNonNullTwoArgs() {
        assertEquals("b", Iterables.lastNonNull("a", "b"));
        assertEquals("a", Iterables.lastNonNull("a", null));
        assertEquals("b", Iterables.lastNonNull(null, "b"));
        assertNull(Iterables.lastNonNull(null, null));
    }

    @Test
    public void testLastNonNullThreeArgs() {
        assertEquals("c", Iterables.lastNonNull("a", "b", "c"));
        assertEquals("b", Iterables.lastNonNull("a", "b", null));
        assertEquals("a", Iterables.lastNonNull("a", null, null));
        assertNull(Iterables.lastNonNull(null, null, null));
    }

    @Test
    public void testLastNonNullVarArgs() {
        String[] arr = { "first", "second", null, null };
        assertEquals("second", Iterables.lastNonNull(arr));

        String[] allNull = { null, null, null };
        assertNull(Iterables.lastNonNull(allNull));

        assertNull(Iterables.lastNonNull((String[]) null));
        assertNull(Iterables.lastNonNull(new String[0]));
    }

    @Test
    public void testLastNonNullIterable() {
        List<String> list = Arrays.asList("first", "second", null, null);
        assertEquals("second", Iterables.lastNonNull(list));

        List<String> allNull = Arrays.asList(null, null, null);
        assertNull(Iterables.lastNonNull(allNull));

        assertNull(Iterables.lastNonNull((Iterable<String>) null));
        assertNull(Iterables.lastNonNull(new ArrayList<String>()));
    }

    @Test
    public void testLastNonNullIterator() {
        List<String> list = Arrays.asList("first", "second", null, null);
        assertEquals("second", Iterables.lastNonNull(list.iterator()));

        List<String> allNull = Arrays.asList(null, null, null);
        assertNull(Iterables.lastNonNull(allNull.iterator()));

        assertNull(Iterables.lastNonNull((Iterator<String>) null));
    }

    @Test
    public void testLastNonNullOrDefaultIterable() {
        List<String> list = Arrays.asList("first", "second", null, null);
        assertEquals("second", CommonUtil.lastNonNullOrDefault(list, "default"));

        List<String> allNull = Arrays.asList(null, null, null);
        assertEquals("default", CommonUtil.lastNonNullOrDefault(allNull, "default"));

        assertEquals("default", CommonUtil.lastNonNullOrDefault((Iterable<String>) null, "default"));
        assertEquals("default", CommonUtil.lastNonNullOrDefault(new ArrayList<String>(), "default"));
    }

    @Test
    public void testLastNonNullOrDefaultIterator() {
        List<String> list = Arrays.asList("first", "second", null, null);
        assertEquals("second", CommonUtil.lastNonNullOrDefault(list.iterator(), "default"));

        List<String> allNull = Arrays.asList(null, null, null);
        assertEquals("default", CommonUtil.lastNonNullOrDefault(allNull.iterator(), "default"));

        assertEquals("default", CommonUtil.lastNonNullOrDefault((Iterator<String>) null, "default"));
    }

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
    public void testMinIntArray() {
        int[] arr = { 3, 1, 4, 1, 5, 9 };
        OptionalInt result = Iterables.min(arr);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        assertFalse(Iterables.min((int[]) null).isPresent());
        assertFalse(Iterables.min(new int[0]).isPresent());
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
    public void testMinFloatArray() {
        float[] arr = { 3.14f, 1.41f, 2.71f };
        OptionalFloat result = Iterables.min(arr);
        assertTrue(result.isPresent());
        assertEquals(1.41f, result.get(), 0.001f);

        assertFalse(Iterables.min((float[]) null).isPresent());
        assertFalse(Iterables.min(new float[0]).isPresent());
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
    public void testMinIntArrayWithFunction() {
        ToIntFunction<String> lengthFunction = String::length;
        String[] arr = { "hi", "hello", "world" };

        OptionalInt result = Iterables.minInt(arr, lengthFunction);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        assertFalse(Iterables.minInt(new String[0], lengthFunction).isPresent());
        assertFalse(Iterables.minInt((String[]) null, lengthFunction).isPresent());
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
    public void testMinLongArrayWithFunction() {
        ToLongFunction<Integer> toLong = i -> i.longValue();
        Integer[] arr = { 5, 2, 8, 1 };

        OptionalLong result = Iterables.minLong(arr, toLong);
        assertTrue(result.isPresent());
        assertEquals(1L, result.getAsLong());

        assertFalse(Iterables.minLong(new Integer[0], toLong).isPresent());
        assertFalse(Iterables.minLong((Integer[]) null, toLong).isPresent());
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
    public void testMinDoubleArrayWithFunction() {
        ToDoubleFunction<Integer> toDouble = i -> i.doubleValue();
        Integer[] arr = { 5, 2, 8, 1 };

        OptionalDouble result = Iterables.minDouble(arr, toDouble);
        assertTrue(result.isPresent());
        assertEquals(1.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.minDouble(new Integer[0], toDouble).isPresent());
        assertFalse(Iterables.minDouble((Integer[]) null, toDouble).isPresent());
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
    public void testMaxIntArray() {
        int[] arr = { 3, 1, 4, 1, 5, 9 };
        OptionalInt result = Iterables.max(arr);
        assertTrue(result.isPresent());
        assertEquals(9, result.getAsInt());

        assertFalse(Iterables.max((int[]) null).isPresent());
        assertFalse(Iterables.max(new int[0]).isPresent());
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
    public void testMaxFloatArray() {
        float[] arr = { 3.14f, 1.41f, 2.71f };
        OptionalFloat result = Iterables.max(arr);
        assertTrue(result.isPresent());
        assertEquals(3.14f, result.get(), 0.001f);

        assertFalse(Iterables.max((float[]) null).isPresent());
        assertFalse(Iterables.max(new float[0]).isPresent());
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
    public void testMaxIntArrayWithFunction() {
        ToIntFunction<String> lengthFunction = String::length;
        String[] arr = { "hi", "hello", "world" };

        OptionalInt result = Iterables.maxInt(arr, lengthFunction);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());

        assertFalse(Iterables.maxInt(new String[0], lengthFunction).isPresent());
        assertFalse(Iterables.maxInt((String[]) null, lengthFunction).isPresent());
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
    public void testMaxLongArrayWithFunction() {
        ToLongFunction<Integer> toLong = i -> i.longValue();
        Integer[] arr = { 5, 2, 8, 1 };

        OptionalLong result = Iterables.maxLong(arr, toLong);
        assertTrue(result.isPresent());
        assertEquals(8L, result.getAsLong());

        assertFalse(Iterables.maxLong(new Integer[0], toLong).isPresent());
        assertFalse(Iterables.maxLong((Integer[]) null, toLong).isPresent());
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
    public void testMaxDoubleArrayWithFunction() {
        ToDoubleFunction<Integer> toDouble = i -> i.doubleValue();
        Integer[] arr = { 5, 2, 8, 1 };

        OptionalDouble result = Iterables.maxDouble(arr, toDouble);
        assertTrue(result.isPresent());
        assertEquals(8.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.maxDouble(new Integer[0], toDouble).isPresent());
        assertFalse(Iterables.maxDouble((Integer[]) null, toDouble).isPresent());
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
    public void testMedianArray() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Nullable<Integer> result = Iterables.median(arr);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(3), result.get());

        assertTrue(Iterables.median(new Integer[0]).isEmpty());
        assertTrue(Iterables.median((Integer[]) null).isEmpty());
    }

    @Test
    public void testMedianArrayWithComparator() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Comparator<Integer> naturalOrder = Comparator.naturalOrder();
        Nullable<Integer> result = Iterables.median(arr, naturalOrder);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(3), result.get());
    }

    @Test
    public void testMedianCollection() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        Nullable<Integer> result = Iterables.median(list);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(3), result.get());

        assertTrue(Iterables.median((Collection<Integer>) null).isEmpty());
        assertTrue(Iterables.median(new ArrayList<Integer>()).isEmpty());
    }

    @Test
    public void testMedianCollectionWithComparator() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        Comparator<Integer> naturalOrder = Comparator.naturalOrder();
        Nullable<Integer> result = Iterables.median(list, naturalOrder);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(3), result.get());
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
    public void testSumIntIterable() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        OptionalInt result = Iterables.sumInt(numbers);
        assertTrue(result.isPresent());
        assertEquals(15, result.getAsInt());

        assertFalse(Iterables.sumInt((Iterable<Integer>) null).isPresent());
        assertFalse(Iterables.sumInt(new ArrayList<Integer>()).isPresent());
    }

    @Test
    public void testSumIntIterableWithFunction() {
        ToIntFunction<String> lengthFunction = String::length;
        OptionalInt result = Iterables.sumInt(stringList, lengthFunction);
        assertTrue(result.isPresent());
        assertEquals(21, result.getAsInt());

        assertFalse(Iterables.sumInt((Iterable<String>) null, lengthFunction).isPresent());
    }

    @Test
    public void testSumIntToLongIterable() {
        List<Integer> largeNumbers = Arrays.asList(Integer.MAX_VALUE, 1);
        OptionalLong result = Iterables.sumIntToLong(largeNumbers);
        assertTrue(result.isPresent());
        assertEquals((long) Integer.MAX_VALUE + 1, result.getAsLong());

        assertFalse(Iterables.sumIntToLong((Iterable<Integer>) null).isPresent());
        assertFalse(Iterables.sumIntToLong(new ArrayList<Integer>()).isPresent());
    }

    @Test
    public void testSumIntToLongIterableWithFunction() {
        ToIntFunction<String> lengthFunction = String::length;
        OptionalLong result = Iterables.sumIntToLong(stringList, lengthFunction);
        assertTrue(result.isPresent());
        assertEquals(21L, result.getAsLong());

        assertFalse(Iterables.sumIntToLong((Iterable<String>) null, lengthFunction).isPresent());
    }

    @Test
    public void testSumLongIterable() {
        List<Long> numbers = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        OptionalLong result = Iterables.sumLong(numbers);
        assertTrue(result.isPresent());
        assertEquals(15L, result.getAsLong());

        assertFalse(Iterables.sumLong((Iterable<Long>) null).isPresent());
        assertFalse(Iterables.sumLong(new ArrayList<Long>()).isPresent());
    }

    @Test
    public void testSumLongIterableWithFunction() {
        ToLongFunction<Integer> toLong = i -> i.longValue();
        OptionalLong result = Iterables.sumLong(intList, toLong);
        assertTrue(result.isPresent());
        assertEquals(31L, result.getAsLong());

        assertFalse(Iterables.sumLong((Iterable<Integer>) null, toLong).isPresent());
    }

    @Test
    public void testSumDoubleIterable() {
        OptionalDouble result = Iterables.sumDouble(doubleList);
        assertTrue(result.isPresent());
        assertEquals(8.99, result.getAsDouble(), 0.001);

        assertFalse(Iterables.sumDouble((Iterable<Double>) null).isPresent());
        assertFalse(Iterables.sumDouble(new ArrayList<Double>()).isPresent());
    }

    @Test
    public void testSumDoubleIterableWithFunction() {
        ToDoubleFunction<Integer> toDouble = i -> i.doubleValue();
        OptionalDouble result = Iterables.sumDouble(intList, toDouble);
        assertTrue(result.isPresent());
        assertEquals(31.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.sumDouble((Iterable<Integer>) null, toDouble).isPresent());
    }

    @Test
    public void testSumBigIntegerIterable() {
        Optional<BigInteger> result = Iterables.sumBigInteger(bigIntList);
        assertTrue(result.isPresent());
        assertEquals(BigInteger.valueOf(600), result.get());

        assertFalse(Iterables.sumBigInteger((Iterable<BigInteger>) null).isPresent());
        assertFalse(Iterables.sumBigInteger(new ArrayList<>()).isPresent());
    }

    @Test
    public void testSumBigIntegerIterableWithFunction() {
        Function<Integer, BigInteger> toBigInt = i -> BigInteger.valueOf(i.longValue());
        Optional<BigInteger> result = Iterables.sumBigInteger(intList, toBigInt);
        assertTrue(result.isPresent());
        assertEquals(BigInteger.valueOf(31), result.get());

        assertFalse(Iterables.sumBigInteger((Iterable<Integer>) null, toBigInt).isPresent());
    }

    @Test
    public void testSumBigDecimalIterable() {
        Optional<BigDecimal> result = Iterables.sumBigDecimal(bigDecimalList);
        assertTrue(result.isPresent());
        assertEquals(BigDecimal.valueOf(61.5), result.get());

        assertFalse(Iterables.sumBigDecimal((Iterable<BigDecimal>) null).isPresent());
        assertFalse(Iterables.sumBigDecimal(new ArrayList<>()).isPresent());
    }

    @Test
    public void testSumBigDecimalIterableWithFunction() {
        Function<Integer, BigDecimal> toBigDec = i -> BigDecimal.valueOf(i.longValue());
        Optional<BigDecimal> result = Iterables.sumBigDecimal(intList, toBigDec);
        assertTrue(result.isPresent());
        assertEquals(BigDecimal.valueOf(31), result.get());

        assertFalse(Iterables.sumBigDecimal((Iterable<Integer>) null, toBigDec).isPresent());
    }

    @Test
    public void testAverageIntArray() {
        Integer[] arr = { 2, 4, 6, 8 };
        OptionalDouble result = Iterables.averageInt(arr);
        assertTrue(result.isPresent());
        assertEquals(5.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageInt(new Integer[0]).isPresent());
        assertFalse(Iterables.averageInt((Integer[]) null).isPresent());
    }

    @Test
    public void testAverageIntArrayWithRange() {
        Integer[] arr = { 2, 4, 6, 8, 10 };
        OptionalDouble result = Iterables.averageInt(arr, 1, 4);
        assertTrue(result.isPresent());
        assertEquals(6.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageInt(arr, 2, 2).isPresent());
    }

    @Test
    public void testAverageIntArrayWithFunction() {
        ToIntFunction<String> lengthFunction = String::length;
        String[] arr = { "hi", "hello", "world" };
        OptionalDouble result = Iterables.averageInt(arr, lengthFunction);
        assertTrue(result.isPresent());
        assertEquals(4.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageInt(new String[0], lengthFunction).isPresent());
    }

    @Test
    public void testAverageIntArrayWithRangeAndFunction() {
        ToIntFunction<String> lengthFunction = String::length;
        String[] arr = { "a", "hi", "hello", "world", "x" };
        OptionalDouble result = Iterables.averageInt(arr, 1, 4, lengthFunction);
        assertTrue(result.isPresent());
        assertEquals(4.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testAverageIntCollectionWithRange() {
        List<Integer> list = Arrays.asList(2, 4, 6, 8, 10);
        OptionalDouble result = Iterables.averageInt(list, 1, 4);
        assertTrue(result.isPresent());
        assertEquals(6.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageInt(list, 2, 2).isPresent());
    }

    @Test
    public void testAverageIntCollectionWithRangeAndFunction() {
        ToIntFunction<String> lengthFunction = String::length;
        List<String> list = Arrays.asList("a", "hi", "hello", "world", "x");
        OptionalDouble result = Iterables.averageInt(list, 1, 4, lengthFunction);
        assertTrue(result.isPresent());
        assertEquals(4.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testAverageIntIterableWithFunction() {
        ToIntFunction<String> lengthFunction = String::length;
        OptionalDouble result = Iterables.averageInt(stringList, lengthFunction);
        assertTrue(result.isPresent());
        assertEquals(5.25, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageInt((Iterable<String>) null, lengthFunction).isPresent());
    }

    @Test
    public void testAverageLongArray() {
        Long[] arr = { 2L, 4L, 6L, 8L };
        OptionalDouble result = Iterables.averageLong(arr);
        assertTrue(result.isPresent());
        assertEquals(5.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageLong(new Long[0]).isPresent());
        assertFalse(Iterables.averageLong((Long[]) null).isPresent());
    }

    @Test
    public void testAverageLongArrayWithRange() {
        Long[] arr = { 2L, 4L, 6L, 8L, 10L };
        OptionalDouble result = Iterables.averageLong(arr, 1, 4);
        assertTrue(result.isPresent());
        assertEquals(6.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageLong(arr, 2, 2).isPresent());
    }

    @Test
    public void testAverageLongArrayWithFunction() {
        ToLongFunction<Integer> toLong = i -> i.longValue();
        Integer[] arr = { 2, 4, 6, 8 };
        OptionalDouble result = Iterables.averageLong(arr, toLong);
        assertTrue(result.isPresent());
        assertEquals(5.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageLong(new Integer[0], toLong).isPresent());
    }

    @Test
    public void testAverageLongArrayWithRangeAndFunction() {
        ToLongFunction<Integer> toLong = i -> i.longValue();
        Integer[] arr = { 1, 2, 4, 6, 8 };
        OptionalDouble result = Iterables.averageLong(arr, 1, 4, toLong);
        assertTrue(result.isPresent());
        assertEquals(4.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testAverageLongCollectionWithRange() {
        List<Long> list = Arrays.asList(2L, 4L, 6L, 8L, 10L);
        OptionalDouble result = Iterables.averageLong(list, 1, 4);
        assertTrue(result.isPresent());
        assertEquals(6.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageLong(list, 2, 2).isPresent());
    }

    @Test
    public void testAverageLongCollectionWithRangeAndFunction() {
        ToLongFunction<Integer> toLong = i -> i.longValue();
        List<Integer> list = Arrays.asList(1, 2, 4, 6, 8);
        OptionalDouble result = Iterables.averageLong(list, 1, 4, toLong);
        assertTrue(result.isPresent());
        assertEquals(4.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testAverageLongIterableWithFunction() {
        ToLongFunction<Integer> toLong = i -> i.longValue();
        OptionalDouble result = Iterables.averageLong(intList, toLong);
        assertTrue(result.isPresent());
        assertEquals(3.875, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageLong((Iterable<Integer>) null, toLong).isPresent());
    }

    @Test
    public void testAverageDoubleArray() {
        Double[] arr = { 2.5, 3.5, 4.5 };
        OptionalDouble result = Iterables.averageDouble(arr);
        assertTrue(result.isPresent());
        assertEquals(3.5, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageDouble(new Double[0]).isPresent());
        assertFalse(Iterables.averageDouble((Double[]) null).isPresent());
    }

    @Test
    public void testAverageDoubleArrayWithRange() {
        Double[] arr = { 1.0, 2.5, 3.5, 4.5, 6.0 };
        OptionalDouble result = Iterables.averageDouble(arr, 1, 4);
        assertTrue(result.isPresent());
        assertEquals(3.5, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageDouble(arr, 2, 2).isPresent());
    }

    @Test
    public void testAverageDoubleArrayWithFunction() {
        ToDoubleFunction<Integer> toDouble = i -> i.doubleValue();
        Integer[] arr = { 2, 4, 6, 8 };
        OptionalDouble result = Iterables.averageDouble(arr, toDouble);
        assertTrue(result.isPresent());
        assertEquals(5.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageDouble(new Integer[0], toDouble).isPresent());
    }

    @Test
    public void testAverageDoubleArrayWithRangeAndFunction() {
        ToDoubleFunction<Integer> toDouble = i -> i.doubleValue();
        Integer[] arr = { 1, 2, 4, 6, 8 };
        OptionalDouble result = Iterables.averageDouble(arr, 1, 4, toDouble);
        assertTrue(result.isPresent());
        assertEquals(4.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testAverageDoubleCollectionWithRange() {
        List<Double> list = Arrays.asList(1.0, 2.5, 3.5, 4.5, 6.0);
        OptionalDouble result = Iterables.averageDouble(list, 1, 4);
        assertTrue(result.isPresent());
        assertEquals(3.5, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageDouble(list, 2, 2).isPresent());
    }

    @Test
    public void testAverageDoubleCollectionWithRangeAndFunction() {
        ToDoubleFunction<Integer> toDouble = i -> i.doubleValue();
        List<Integer> list = Arrays.asList(1, 2, 4, 6, 8);
        OptionalDouble result = Iterables.averageDouble(list, 1, 4, toDouble);
        assertTrue(result.isPresent());
        assertEquals(4.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testAverageDoubleIterable() {
        OptionalDouble result = Iterables.averageDouble(doubleList);
        assertTrue(result.isPresent());
        assertEquals(2.2475, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageDouble((Iterable<Double>) null).isPresent());
    }

    @Test
    public void testAverageDoubleIterableWithFunction() {
        ToDoubleFunction<Integer> toDouble = i -> i.doubleValue();
        OptionalDouble result = Iterables.averageDouble(intList, toDouble);
        assertTrue(result.isPresent());
        assertEquals(3.875, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageDouble((Iterable<Integer>) null, toDouble).isPresent());
    }

    @Test
    public void testAverageBigIntegerIterable() {
        Optional<BigDecimal> result = Iterables.averageBigInteger(bigIntList);
        assertTrue(result.isPresent());
        assertEquals(BigDecimal.valueOf(200), result.get());

        assertFalse(Iterables.averageBigInteger((Iterable<BigInteger>) null).isPresent());
        assertFalse(Iterables.averageBigInteger(new ArrayList<>()).isPresent());
    }

    @Test
    public void testAverageBigIntegerIterableWithFunction() {
        Function<Integer, BigInteger> toBigInt = i -> BigInteger.valueOf(i.longValue());
        Optional<BigDecimal> result = Iterables.averageBigInteger(intList, toBigInt);
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("3.875"), result.get());

        assertFalse(Iterables.averageBigInteger((Iterable<Integer>) null, toBigInt).isPresent());
    }

    @Test
    public void testAverageBigDecimalIterable() {
        Optional<BigDecimal> result = Iterables.averageBigDecimal(bigDecimalList);
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("20.5"), result.get());

        assertFalse(Iterables.averageBigDecimal((Iterable<BigDecimal>) null).isPresent());
        assertFalse(Iterables.averageBigDecimal(new ArrayList<>()).isPresent());
    }

    @Test
    public void testAverageBigDecimalIterableWithFunction() {
        Function<Integer, BigDecimal> toBigDec = i -> BigDecimal.valueOf(i.longValue());
        Optional<BigDecimal> result = Iterables.averageBigDecimal(intList, toBigDec);
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("3.875"), result.get());

        assertFalse(Iterables.averageBigDecimal((Iterable<Integer>) null, toBigDec).isPresent());
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
    public void testLastIndexOfArray() {
        Integer[] arr = { 1, 2, 3, 2, 4 };
        OptionalInt result = Iterables.lastIndexOf(arr, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        assertFalse(Iterables.lastIndexOf(arr, 5).isPresent());
        assertFalse(Iterables.lastIndexOf((Object[]) null, 1).isPresent());
    }

    @Test
    public void testLastIndexOfCollection() {
        List<Integer> list = Arrays.asList(1, 2, 3, 2, 4);
        OptionalInt result = Iterables.lastIndexOf(list, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        assertFalse(Iterables.lastIndexOf(list, 5).isPresent());
        assertFalse(Iterables.lastIndexOf((Collection<?>) null, 1).isPresent());
    }

    @Test
    public void testFindFirstOrLastArray() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> isOdd = n -> n % 2 == 1;

        Nullable<Integer> result = Iterables.findFirstOrLast(arr, isEven, isOdd);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(2), result.get());

        Predicate<Integer> isGreaterThan10 = n -> n > 10;
        result = Iterables.findFirstOrLast(arr, isGreaterThan10, isOdd);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(5), result.get());

        assertTrue(Iterables.findFirstOrLast((Integer[]) null, isEven, isOdd).isEmpty());
    }

    @Test
    public void testFindFirstOrLastCollection() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> isOdd = n -> n % 2 == 1;

        Nullable<Integer> result = Iterables.findFirstOrLast(list, isEven, isOdd);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(2), result.get());

        assertTrue(Iterables.findFirstOrLast((Collection<Integer>) null, isEven, isOdd).isEmpty());
    }

    @Test
    public void testFindFirstOrLastIndexArray() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> isOdd = n -> n % 2 == 1;

        OptionalInt result = Iterables.findFirstOrLastIndex(arr, isEven, isOdd);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        Predicate<Integer> isGreaterThan10 = n -> n > 10;
        result = Iterables.findFirstOrLastIndex(arr, isGreaterThan10, isOdd);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        assertFalse(Iterables.findFirstOrLastIndex((Integer[]) null, isEven, isOdd).isPresent());
    }

    @Test
    public void testFindFirstOrLastIndexCollection() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> isOdd = n -> n % 2 == 1;

        OptionalInt result = Iterables.findFirstOrLastIndex(list, isEven, isOdd);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        assertFalse(Iterables.findFirstOrLastIndex((Collection<Integer>) null, isEven, isOdd).isPresent());
    }

    @Test
    public void testFindFirstAndLastArraySinglePredicate() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Predicate<Integer> isEven = n -> n % 2 == 0;

        Pair<Nullable<Integer>, Nullable<Integer>> result = Iterables.findFirstAndLast(arr, isEven);
        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals(Integer.valueOf(2), result.left().get());
        assertEquals(Integer.valueOf(4), result.right().get());

        Pair<Nullable<Integer>, Nullable<Integer>> emptyResult = Iterables.findFirstAndLast((Integer[]) null, isEven);
        assertFalse(emptyResult.left().isPresent());
        assertFalse(emptyResult.right().isPresent());
    }

    @Test
    public void testFindFirstAndLastArrayTwoPredicates() {
        Integer[] arr = { 1, 2, 3, 4, 5, 6 };
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> greaterThan3 = n -> n > 3;

        Pair<Nullable<Integer>, Nullable<Integer>> result = Iterables.findFirstAndLast(arr, isEven, greaterThan3);
        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals(Integer.valueOf(2), result.left().get());
        assertEquals(Integer.valueOf(6), result.right().get());
    }

    @Test
    public void testFindFirstAndLastCollectionSinglePredicate() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        Predicate<Integer> isEven = n -> n % 2 == 0;

        Pair<Nullable<Integer>, Nullable<Integer>> result = Iterables.findFirstAndLast(list, isEven);
        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals(Integer.valueOf(2), result.left().get());
        assertEquals(Integer.valueOf(4), result.right().get());

        Pair<Nullable<Integer>, Nullable<Integer>> emptyResult = Iterables.findFirstAndLast((Collection<Integer>) null, isEven);
        assertFalse(emptyResult.left().isPresent());
        assertFalse(emptyResult.right().isPresent());
    }

    @Test
    public void testFindFirstAndLastCollectionTwoPredicates() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> greaterThan3 = n -> n > 3;

        Pair<Nullable<Integer>, Nullable<Integer>> result = Iterables.findFirstAndLast(list, isEven, greaterThan3);
        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals(Integer.valueOf(2), result.left().get());
        assertEquals(Integer.valueOf(6), result.right().get());
    }

    @Test
    public void testFindFirstAndLastIndexArraySinglePredicate() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Predicate<Integer> isEven = n -> n % 2 == 0;

        Pair<OptionalInt, OptionalInt> result = Iterables.findFirstAndLastIndex(arr, isEven);
        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals(1, result.left().getAsInt());
        assertEquals(3, result.right().getAsInt());

        Pair<OptionalInt, OptionalInt> emptyResult = Iterables.findFirstAndLastIndex((Integer[]) null, isEven);
        assertFalse(emptyResult.left().isPresent());
        assertFalse(emptyResult.right().isPresent());
    }

    @Test
    public void testFindFirstAndLastIndexArrayTwoPredicates() {
        Integer[] arr = { 1, 2, 3, 4, 5, 6 };
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> greaterThan3 = n -> n > 3;

        Pair<OptionalInt, OptionalInt> result = Iterables.findFirstAndLastIndex(arr, isEven, greaterThan3);
        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals(1, result.left().getAsInt());
        assertEquals(5, result.right().getAsInt());
    }

    @Test
    public void testFindFirstAndLastIndexCollectionSinglePredicate() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        Predicate<Integer> isEven = n -> n % 2 == 0;

        Pair<OptionalInt, OptionalInt> result = Iterables.findFirstAndLastIndex(list, isEven);
        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals(1, result.left().getAsInt());
        assertEquals(3, result.right().getAsInt());

        Pair<OptionalInt, OptionalInt> emptyResult = Iterables.findFirstAndLastIndex((Collection<Integer>) null, isEven);
        assertFalse(emptyResult.left().isPresent());
        assertFalse(emptyResult.right().isPresent());
    }

    @Test
    public void testFindFirstAndLastIndexCollectionTwoPredicates() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);
        Predicate<Integer> isEven = n -> n % 2 == 0;
        Predicate<Integer> greaterThan3 = n -> n > 3;

        Pair<OptionalInt, OptionalInt> result = Iterables.findFirstAndLastIndex(list, isEven, greaterThan3);
        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals(1, result.left().getAsInt());
        assertEquals(5, result.right().getAsInt());
    }

    @Test
    public void testFillArray() {
        String[] arr = new String[5];
        Supplier<String> supplier = () -> "test";

        Iterables.fill(arr, supplier);

        for (String s : arr) {
            assertEquals("test", s);
        }
    }

    @Test
    public void testFillArrayWithRange() {
        String[] arr = new String[5];
        Supplier<String> supplier = () -> "test";

        Iterables.fill(arr, 1, 4, supplier);

        assertNull(arr[0]);
        assertEquals("test", arr[1]);
        assertEquals("test", arr[2]);
        assertEquals("test", arr[3]);
        assertNull(arr[4]);
    }

    @Test
    public void testFillList() {
        List<String> list = new ArrayList<>(Arrays.asList(null, null, null));
        Supplier<String> supplier = () -> "filled";

        Iterables.fill(list, supplier);

        for (String s : list) {
            assertEquals("filled", s);
        }
    }

    @Test
    public void testFillListWithRange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        Supplier<String> supplier = () -> "X";

        Iterables.fill(list, 1, 4, supplier);

        assertEquals("a", list.get(0));
        assertEquals("X", list.get(1));
        assertEquals("X", list.get(2));
        assertEquals("X", list.get(3));
        assertEquals("e", list.get(4));
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
    public void testCopyListsWithPositions() {
        List<String> src = Arrays.asList("a", "b", "c", "d", "e");
        List<String> dest = new ArrayList<>(Arrays.asList("1", "2", "3", "4", "5", "6"));

        Iterables.copyRange(src, 1, dest, 2, 3);

        assertEquals("1", dest.get(0));
        assertEquals("2", dest.get(1));
        assertEquals("b", dest.get(2));
        assertEquals("c", dest.get(3));
        assertEquals("d", dest.get(4));
        assertEquals("6", dest.get(5));
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
    public void testPowerSetEmpty() {
        Set<Integer> input = new HashSet<>();
        Set<Set<Integer>> powerSet = Iterables.powerSet(input);

        assertEquals(1, powerSet.size());
        assertTrue(powerSet.contains(new HashSet<>()));
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
    public void testRollupEmpty() {
        List<String> input = new ArrayList<>();
        List<List<String>> rollup = Iterables.rollup(input);

        assertEquals(1, rollup.size());
        assertEquals(Arrays.asList(), rollup.get(0));
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
    public void testPermutationsEmpty() {
        List<Integer> input = new ArrayList<>();
        Collection<List<Integer>> perms = Iterables.permutations(input);

        assertEquals(1, perms.size());
        assertTrue(perms.iterator().next().isEmpty());
    }

    @Test
    public void testPermutationsWithDuplicates() {
        List<Integer> input = Arrays.asList(1, 1, 2);
        Collection<List<Integer>> perms = Iterables.permutations(input);

        assertEquals(6, perms.size());
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
    public void testCartesianProductVarargs() {
        List<List<Integer>> product = Iterables.cartesianProduct(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6));

        assertEquals(8, product.size());

        assertEquals(Arrays.asList(1, 3, 5), product.get(0));
        assertEquals(Arrays.asList(2, 4, 6), product.get(7));
    }

    @Test
    public void testCartesianProductCollection() {
        Collection<Collection<Integer>> collections = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6));
        List<List<Integer>> product = Iterables.cartesianProduct(collections);

        assertEquals(8, product.size());

        assertEquals(Arrays.asList(1, 3, 5), product.get(0));
        assertEquals(Arrays.asList(2, 4, 6), product.get(7));
    }

    @Test
    public void testCartesianProductWithEmptyList() {
        List<List<Integer>> product = Iterables.cartesianProduct(Arrays.asList(1, 2), Arrays.asList(), Arrays.asList(5, 6));

        assertEquals(0, product.size());
    }

    @Test
    public void testCartesianProductNoLists() {
        List<List<Integer>> product = Iterables.cartesianProduct(new ArrayList<Collection<Integer>>());

        assertEquals(1, product.size());
        assertTrue(product.get(0).isEmpty());
    }

    @Test
    public void testCartesianProductSingleList() {
        List<List<Integer>> product = Iterables.cartesianProduct(Arrays.asList(Arrays.asList(1, 2, 3)));

        assertEquals(3, product.size());
        assertEquals(Arrays.asList(1), product.get(0));
        assertEquals(Arrays.asList(2), product.get(1));
        assertEquals(Arrays.asList(3), product.get(2));
    }

    @Test
    public void testAverageIntArrayNoArgs() {
        Integer[] arr = { 2, 4, 6, 8 };
        OptionalDouble result = Iterables.averageInt(arr);
        assertTrue(result.isPresent());
        assertEquals(5.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageInt(new Integer[0]).isPresent());
    }

    @Test
    public void testAverageLongArrayNoArgs() {
        Long[] arr = { 2L, 4L, 6L, 8L };
        OptionalDouble result = Iterables.averageLong(arr);
        assertTrue(result.isPresent());
        assertEquals(5.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageLong(new Long[0]).isPresent());
    }

    @Test
    public void testAverageDoubleArrayNoArgs() {
        Double[] arr = { 2.0, 4.0, 6.0, 8.0 };
        OptionalDouble result = Iterables.averageDouble(arr);
        assertTrue(result.isPresent());
        assertEquals(5.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageDouble(new Double[0]).isPresent());
    }

    @Test
    public void testAverageIntIterableNoArgs() {
        List<Integer> list = Arrays.asList(2, 4, 6, 8);
        OptionalDouble result = Iterables.averageInt(list);
        assertTrue(result.isPresent());
        assertEquals(5.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageInt((Iterable<Integer>) null).isPresent());
    }

    @Test
    public void testAverageLongIterableNoArgs() {
        List<Long> list = Arrays.asList(2L, 4L, 6L, 8L);
        OptionalDouble result = Iterables.averageLong(list);
        assertTrue(result.isPresent());
        assertEquals(5.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageLong((Iterable<Long>) null).isPresent());
    }

    @Test
    public void testAverageDoubleIterableNoArgs() {
        List<Double> list = Arrays.asList(2.0, 4.0, 6.0, 8.0);
        OptionalDouble result = Iterables.averageDouble(list);
        assertTrue(result.isPresent());
        assertEquals(5.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageDouble((Iterable<Double>) null).isPresent());
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

    @Test
    public void testAverageIntArrayFromTo() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        assertTrue(Iterables.averageInt(arr, 1, 1).isEmpty());
        assertEquals(3.0, Iterables.averageInt(arr, 1, 4).orElseThrow(), 0.0);
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, 3, 2));
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
    public void testAverageLongArrayFromTo() {
        Long[] arr = { 1L, 2L, 3L, 4L, 5L };
        assertTrue(Iterables.averageLong(arr, 1, 1).isEmpty());
        assertEquals(3.0, Iterables.averageLong(arr, 1, 4).orElseThrow(), 0.0);
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
    public void testAverageDoubleArrayFromTo() {
        Double[] arr = { 1.1, 2.2, 3.3, 4.4, 5.5 };
        assertTrue(Iterables.averageDouble(arr, 1, 1).isEmpty());
        assertEquals(3.3, Iterables.averageDouble(arr, 1, 4).orElseThrow(), 0.00001);
    }

    @Test
    public void testAverageDoubleArrayFromToWithFunction() {
        TestObject[] arr = { new TestObject(1, ""), new TestObject(2, ""), new TestObject(3, ""), new TestObject(4, ""), new TestObject(5, "") };
        ToDoubleFunction<TestObject> extractor = obj -> obj.getId() + 0.1;
        assertTrue(Iterables.averageDouble(arr, 1, 1, extractor).isEmpty());
        assertEquals(3.1, Iterables.averageDouble(arr, 1, 4, extractor).orElseThrow(), 0.00001);
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
        Predicate<Integer> isSmallEven = x -> x != null && x % 2 == 0 && x < 5;
        Predicate<Integer> isLargeOdd = x -> x != null && x % 2 != 0 && x > 3;
        Pair<Nullable<Integer>, Nullable<Integer>> result;

        result = Iterables.findFirstAndLast(new Integer[] { 1, 2, 3, 4, 5, 6 }, isSmallEven, isLargeOdd);
        assertEquals(2, result.left().orElse(null));
        assertEquals(5, result.right().orElse(null));
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

        result = Iterables.findFirstAndLastIndex(new Integer[] { 1, 2, 3, 4, 5, 6 }, isEven);
        assertEquals(1, result.left().orElse(-1));
        assertEquals(5, result.right().orElse(-1));

        result = Iterables.findFirstAndLastIndex(new Integer[] { 1, 3, 5 }, isEven);
        assertTrue(result.left().isEmpty() && result.right().isEmpty());
    }

    @Test
    public void testFindFirstAndLastIndexArrayWithTwoPredicates() {
        Predicate<Integer> isSmallEven = x -> x != null && x % 2 == 0 && x < 5;
        Predicate<Integer> isLargeOdd = x -> x != null && x % 2 != 0 && x > 3;
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

    @Test
    public void testFillArrayWithSupplier() {
        Iterables.fill((String[]) null, () -> "a");

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
        Iterables.fill((String[]) null, 0, 0, () -> "a");

        String[] arr = new String[5];
        Arrays.fill(arr, "original");
        Iterables.fill(arr, 1, 4, () -> "filled");
        assertArrayEquals(new String[] { "original", "filled", "filled", "filled", "original" }, arr);

        Iterables.fill(arr, 1, 1, () -> "no-fill");
        assertArrayEquals(new String[] { "original", "filled", "filled", "filled", "original" }, arr);

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
        Iterables.fill(list, 1, 4, () -> "filled");
        assertEquals(Arrays.asList("a", "filled", "filled", "filled", "e"), list);

        List<String> shortList = new ArrayList<>(Arrays.asList("a"));
        Iterables.fill(shortList, 0, 3, () -> "new");
        assertEquals(Arrays.asList("new", "new", "new"), shortList);
        assertEquals(3, shortList.size());

        List<String> listToExtend = new ArrayList<>(Arrays.asList("a", "b"));
        Iterables.fill(listToExtend, 1, 4, () -> "Z");
        assertEquals(Arrays.asList("a", "Z", "Z", "Z"), listToExtend);

        List<Integer> listInt = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        final int[] counter = { 10 };
        Supplier<Integer> supplier = () -> counter[0]++;
        Iterables.fill(listInt, 2, 5, supplier);
        assertEquals(Arrays.asList(1, 2, 10, 11, 12), listInt);

        List<Integer> listFillAndExtend = new ArrayList<>(Arrays.asList(1, 2));
        final int[] counter2 = { 100 };
        Supplier<Integer> supplier2 = () -> counter2[0]++;
        Iterables.fill(listFillAndExtend, 1, 4, supplier2);
        assertEquals(Arrays.asList(1, 100, 101, 102), listFillAndExtend);

        List<Integer> listFillFromSize = new ArrayList<>(Arrays.asList(1, 2));
        final int[] counter3 = { 200 };
        Supplier<Integer> supplier3 = () -> counter3[0]++;
        Iterables.fill(listFillFromSize, 2, 4, supplier3);
        assertEquals(Arrays.asList(1, 2, 200, 201), listFillFromSize);

        List<Integer> listFillFromSize2 = new ArrayList<>(Arrays.asList(1, 2));
        final int[] counter4 = { 300 };
        Supplier<Integer> supplier4 = () -> counter4[0]++;
        Iterables.fill(listFillFromSize2, 3, 5, supplier4);
        assertEquals(Arrays.asList(1, 2, null, 300, 301), listFillFromSize2);

        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.fill(list, -1, 2, () -> "fail"));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.fill(list, 3, 2, () -> "fail"));

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

        List<List<Object>> cpNullInput = Iterables.cartesianProduct((Collection<? extends Collection<?>>) null);
        assertEquals(1, cpNullInput.size());
        assertTrue(cpNullInput.get(0).isEmpty());
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
    public void testMinIntArrayExtractor() {
        assertTrue(Iterables.minInt((String[]) null, String::length).isEmpty());
        assertTrue(Iterables.minInt(new String[0], String::length).isEmpty());
        assertEquals(1, Iterables.minInt(new String[] { "aaa", "a", "bb" }, String::length).get());
        assertEquals(3, Iterables.minInt(new String[] { "aaa" }, String::length).get());
    }

    @Test
    public void testMinIntIterableExtractor() {
        assertTrue(Iterables.minInt((List<String>) null, String::length).isEmpty());
        assertTrue(Iterables.minInt(list(), String::length).isEmpty());
        assertEquals(1, Iterables.minInt(list("aaa", "a", "bb"), String::length).get());
    }

    @Test
    public void testMinIntIteratorExtractor() {
        assertTrue(Iterables.minInt((Iterator<String>) null, String::length).isEmpty());
        assertTrue(Iterables.minInt(list(CommonUtil.EMPTY_STRING_ARRAY).iterator(), String::length).isEmpty());
        assertEquals(1, Iterables.minInt(list("aaa", "a", "bb").iterator(), String::length).get());
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
    public void testMinLongIterableExtractor() {
        ToLongFunction<String> lengthToLong = s -> (long) s.length();
        assertTrue(Iterables.minLong((List<String>) null, lengthToLong).isEmpty());
        assertTrue(Iterables.minLong(list(), lengthToLong).isEmpty());
        assertEquals(1L, Iterables.minLong(list("aaa", "a", "bb"), lengthToLong).get());
    }

    @Test
    public void testMinLongIteratorExtractor() {
        ToLongFunction<String> lengthToLong = s -> (long) s.length();
        assertTrue(Iterables.minLong((Iterator<String>) null, lengthToLong).isEmpty());
        assertTrue(Iterables.minLong(list(CommonUtil.EMPTY_STRING_ARRAY).iterator(), lengthToLong).isEmpty());
        assertEquals(1L, Iterables.minLong(list("aaa", "a", "bb").iterator(), lengthToLong).get());
    }

    @Test
    public void testMinDoubleArrayExtractor() {
        ToDoubleFunction<String> lengthToDouble = s -> (double) s.length();
        assertTrue(Iterables.minDouble((String[]) null, lengthToDouble).isEmpty());
        assertTrue(Iterables.minDouble(new String[0], lengthToDouble).isEmpty());
        assertEquals(1.0, Iterables.minDouble(new String[] { "aaa", "a", "bb" }, lengthToDouble).get());
    }

    @Test
    public void testMinDoubleIterableExtractor() {
        ToDoubleFunction<String> lengthToDouble = s -> (double) s.length();
        assertTrue(Iterables.minDouble((List<String>) null, lengthToDouble).isEmpty());
        assertTrue(Iterables.minDouble(list(), lengthToDouble).isEmpty());
        assertEquals(1.0, Iterables.minDouble(list("aaa", "a", "bb"), lengthToDouble).get());
    }

    @Test
    public void testMinDoubleIteratorExtractor() {
        ToDoubleFunction<String> lengthToDouble = s -> (double) s.length();
        assertTrue(Iterables.minDouble((Iterator<String>) null, lengthToDouble).isEmpty());
        assertTrue(Iterables.minDouble(list(CommonUtil.EMPTY_STRING_ARRAY).iterator(), lengthToDouble).isEmpty());
        assertEquals(1.0, Iterables.minDouble(list("aaa", "a", "bb").iterator(), lengthToDouble).get());
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
        assertEquals(null, result.left());
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
    public void testMedianArrayComparable() {
        assertTrue(Iterables.median((String[]) null).isEmpty());
        assertTrue(Iterables.median(new String[0]).isEmpty());
        assertEquals("b", Iterables.median(new String[] { "a", "b", "c" }).get());
        assertEquals("b", Iterables.median(new String[] { "c", "b", "a" }).get());
        assertEquals("a", Iterables.median(new String[] { "a", "b" }).get());
        assertEquals("b", Iterables.median(new String[] { "a", "b", "c", "d" }).get());
    }

    @Test
    public void testMedianArrayComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.median((String[]) null, reverseOrder).isEmpty());
        assertTrue(Iterables.median(new String[0], reverseOrder).isEmpty());
        assertEquals("b", Iterables.median(new String[] { "a", "b", "c" }, reverseOrder).get());
        assertEquals("b", Iterables.median(new String[] { "a", "b" }, reverseOrder).get());
    }

    @Test
    public void testMedianCollectionComparable() {
        assertTrue(Iterables.median((Collection<String>) null).isEmpty());
        assertTrue(Iterables.median(list()).isEmpty());
        assertEquals("b", Iterables.median(list("a", "b", "c")).get());
    }

    @Test
    public void testMedianCollectionComparator() {
        Comparator<String> reverseOrder = Comparator.reverseOrder();
        assertTrue(Iterables.median((Collection<String>) null, reverseOrder).isEmpty());
        assertTrue(Iterables.median(list(), reverseOrder).isEmpty());
        assertEquals("b", Iterables.median(list("a", "b", "c"), reverseOrder).get());
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
    public void testSumIntIterableNumber() {
        assertTrue(Iterables.sumInt((Iterable<Integer>) null).isEmpty());
        assertTrue(Iterables.sumInt(list()).isEmpty());
        assertEquals(6, Iterables.sumInt(list(1, 2, 3)).get());
        assertEquals(0, Iterables.sumInt(list(1, -1, 0)).get());
    }

    @Test
    public void testSumIntIterableExtractor() {
        ToIntFunction<String> len = String::length;
        assertTrue(Iterables.sumInt((Iterable<String>) null, len).isEmpty());
        assertTrue(Iterables.sumInt(list(), len).isEmpty());
        assertEquals(6, Iterables.sumInt(list("a", "bb", "ccc"), len).get());
    }

    @Test
    public void testSumIntToLongIterableNumber() {
        assertTrue(Iterables.sumIntToLong((Iterable<Integer>) null).isEmpty());
        assertTrue(Iterables.sumIntToLong(list()).isEmpty());
        assertEquals(6L, Iterables.sumIntToLong(list(1, 2, 3)).get());
        assertEquals(2L * Integer.MAX_VALUE, Iterables.sumIntToLong(list(Integer.MAX_VALUE, Integer.MAX_VALUE)).get());
    }

    @Test
    public void testSumIntToLongIterableExtractor() {
        ToIntFunction<String> len = String::length;
        assertTrue(Iterables.sumIntToLong((Iterable<String>) null, len).isEmpty());
        assertTrue(Iterables.sumIntToLong(list(), len).isEmpty());
        assertEquals(6L, Iterables.sumIntToLong(list("a", "bb", "ccc"), len).get());
    }

    @Test
    public void testSumLongIterableNumber() {
        assertTrue(Iterables.sumLong((Iterable<Long>) null).isEmpty());
        assertTrue(Iterables.sumLong(list()).isEmpty());
        assertEquals(6L, Iterables.sumLong(list(1L, 2L, 3L)).get());
    }

    @Test
    public void testSumLongIterableExtractor() {
        ToLongFunction<String> len = s -> (long) s.length();
        assertTrue(Iterables.sumLong((Iterable<String>) null, len).isEmpty());
        assertTrue(Iterables.sumLong(list(), len).isEmpty());
        assertEquals(6L, Iterables.sumLong(list("a", "bb", "ccc"), len).get());
    }

    @Test
    public void testSumDoubleIterableNumber() {
        assertTrue(Iterables.sumDouble((Iterable<Double>) null).isEmpty());
        assertTrue(Iterables.sumDouble(list()).isEmpty());
        assertEquals(6.0, Iterables.sumDouble(list(1.0, 2.5, 2.5)).get(), 0.001);
    }

    @Test
    public void testSumDoubleIterableExtractor() {
        ToDoubleFunction<String> len = s -> (double) s.length();
        assertTrue(Iterables.sumDouble((Iterable<String>) null, len).isEmpty());
        assertTrue(Iterables.sumDouble(list(), len).isEmpty());
        assertEquals(6.0, Iterables.sumDouble(list("a", "bb", "ccc"), len).get(), 0.001);
    }

    @Test
    public void testSumBigIntegerIterableExtractor() {
        Function<String, BigInteger> lenToBi = s -> BigInteger.valueOf(s.length());
        assertTrue(Iterables.sumBigInteger((Iterable<String>) null, lenToBi).isEmpty());
        assertTrue(Iterables.sumBigInteger(list(), lenToBi).isEmpty());
        assertEquals(new BigInteger("6"), Iterables.sumBigInteger(list("a", "bb", "ccc"), lenToBi).get());
    }

    @Test
    public void testSumBigDecimalIterableExtractor() {
        Function<String, BigDecimal> lenToBd = s -> BigDecimal.valueOf(s.length());
        assertTrue(Iterables.sumBigDecimal((Iterable<String>) null, lenToBd).isEmpty());
        assertTrue(Iterables.sumBigDecimal(list(), lenToBd).isEmpty());
        assertEquals(new BigDecimal("6"), Iterables.sumBigDecimal(list("a", "bb", "ccc"), lenToBd).get());
    }

    @Test
    public void testAverageIntArrayNumber() {
        assertTrue(Iterables.averageInt((Integer[]) null).isEmpty());
        assertTrue(Iterables.averageInt(new Integer[0]).isEmpty());
        assertEquals(2.0, Iterables.averageInt(new Integer[] { 1, 2, 3 }).get(), 0.001);
    }

    @Test
    public void testAverageIntArrayRangeNumber() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        assertEquals(3.0, Iterables.averageInt(arr, 0, 5).get(), 0.001);
        assertEquals(2.0, Iterables.averageInt(arr, 0, 3).get(), 0.001);
        assertEquals(4.0, Iterables.averageInt(arr, 2, 5).get(), 0.001);
        assertTrue(Iterables.averageInt(arr, 1, 1).isEmpty());
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(arr, 3, 2));
    }

    @Test
    public void testAverageIntArrayExtractor() {
        ToIntFunction<String> len = String::length;
        assertTrue(Iterables.averageInt((String[]) null, len).isEmpty());
        assertTrue(Iterables.averageInt(new String[0], len).isEmpty());
        assertEquals(2.0, Iterables.averageInt(new String[] { "a", "bb", "ccc" }, len).get(), 0.001);
    }

    @Test
    public void testAverageIntArrayRangeExtractor() {
        String[] arr = { "a", "bb", "ccc", "dddd", "eeeee" };
        ToIntFunction<String> len = String::length;
        assertEquals(3.0, Iterables.averageInt(arr, 0, 5, len).get(), 0.001);
        assertEquals(2.0, Iterables.averageInt(arr, 0, 3, len).get(), 0.001);
        assertTrue(Iterables.averageInt(arr, 1, 1, len).isEmpty());
    }

    @Test
    public void testAverageIntCollectionRangeNumber() {
        List<Integer> coll = list(1, 2, 3, 4, 5);
        assertEquals(3.0, Iterables.averageInt(coll, 0, 5).get(), 0.001);
        assertEquals(2.0, Iterables.averageInt(coll, 0, 3).get(), 0.001);
        assertTrue(Iterables.averageInt(coll, 1, 1).isEmpty());
    }

    @Test
    public void testAverageIntCollectionRangeExtractor() {
        List<String> coll = list("a", "bb", "ccc", "dddd", "eeeee");
        ToIntFunction<String> len = String::length;
        assertEquals(3.0, Iterables.averageInt(coll, 0, 5, len).get(), 0.001);
        assertEquals(2.0, Iterables.averageInt(coll, 0, 3, len).get(), 0.001);
    }

    @Test
    public void testAverageIntIterableNumber() {
        assertTrue(Iterables.averageInt((Iterable<Integer>) null).isEmpty());
        assertTrue(Iterables.averageInt(list()).isEmpty());
        assertEquals(2.0, Iterables.averageInt(list(1, 2, 3)).get(), 0.001);
    }

    @Test
    public void testAverageIntIterableExtractor() {
        ToIntFunction<String> len = String::length;
        assertTrue(Iterables.averageInt((Iterable<String>) null, len).isEmpty());
        assertTrue(Iterables.averageInt(list(), len).isEmpty());
        assertEquals(2.0, Iterables.averageInt(list("a", "bb", "ccc"), len).get(), 0.001);
    }

    @Test
    public void testAverageDoubleArrayExtractor() {
        ToDoubleFunction<String> len = s -> (double) s.length();
        assertTrue(Iterables.averageDouble((String[]) null, len).isEmpty());
        assertTrue(Iterables.averageDouble(new String[0], len).isEmpty());
        assertEquals(2.0, Iterables.averageDouble(new String[] { "a", "bb", "ccc" }, len).get(), 0.001);
    }

    @Test
    public void testAverageDoubleIterableExtractor() {
        ToDoubleFunction<String> len = s -> (double) s.length();
        assertTrue(Iterables.averageDouble((Iterable<String>) null, len).isEmpty());
        assertTrue(Iterables.averageDouble(list(), len).isEmpty());
        assertEquals(2.0, Iterables.averageDouble(list("a", "bb", "ccc"), len).get(), 0.001);
        List<Double> doubles = new ArrayList<>();
        doubles.add(1.0e100);
        doubles.add(1.0);
        doubles.add(-1.0e100);
        assertEquals(0.0 / 3.0, Iterables.averageDouble(doubles, d -> d).get(), 1e-15);

        doubles = new ArrayList<>();
        doubles.add(1.0e10);
        doubles.add(1.0);
        doubles.add(-1.0e10);
        assertEquals(1.0 / 3.0, Iterables.averageDouble(doubles, d -> d).get(), 1e-15);
    }

    @Test
    public void testAverageDoubleCollectionRangeExtractor() {
        List<String> coll = list("a", "bb", "ccc", "dddd", "eeeee");
        ToDoubleFunction<String> len = s -> (double) s.length();
        assertEquals(3.0, Iterables.averageDouble(coll, 0, 5, len).get(), 0.001);
        assertEquals(2.0, Iterables.averageDouble(coll, 0, 3, len).get(), 0.001);
        assertTrue(Iterables.averageDouble(coll, 1, 1, len).isEmpty());

        LinkedList<String> linkedList = new LinkedList<>(coll);
        assertEquals(3.0, Iterables.averageDouble(linkedList, 0, 5, len).get(), 0.001);
        assertEquals(4.0, Iterables.averageDouble(linkedList, 2, 5, len).get(), 0.001);
    }

    @Test
    public void testFindFirstAndLastArrayDifferentPredicates() {
        Predicate<String> startsA = s -> s.startsWith("a");
        Predicate<String> endsO = s -> s.endsWith("o");
        Pair<Nullable<String>, Nullable<String>> result = Iterables.findFirstAndLast(new String[] { "apple", "avocado", "banana", "mango", "orange" }, startsA,
                endsO);
        assertEquals("apple", result.left().get());
        assertEquals("mango", result.right().get());

        result = Iterables.findFirstAndLast(new String[] { "apple", "avocado" }, s -> s.startsWith("x"), endsO);
        assertTrue(result.left().isEmpty());
        assertEquals("avocado", result.right().get());
    }

    @Test
    public void testFillArraySupplier() {
        String[] arr = new String[3];
        Supplier<String> supplier = () -> "test";
        Iterables.fill(arr, supplier);
        assertArrayEquals(new String[] { "test", "test", "test" }, arr);

        Iterables.fill((String[]) null, supplier);
        Iterables.fill(new String[0], supplier);
    }

    @Test
    public void testFillArrayRangeSupplier() {
        String[] arr = { "a", "b", "c", "d" };
        Supplier<String> supplier = () -> "x";
        Iterables.fill(arr, 1, 3, supplier);
        assertArrayEquals(new String[] { "a", "x", "x", "d" }, arr);

        Iterables.fill(arr, 1, 1, supplier);
        assertArrayEquals(new String[] { "a", "x", "x", "d" }, arr);

        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.fill(arr, 0, 5, supplier));
    }

    @Test
    public void testFillListSupplier() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Supplier<String> supplier = () -> "test";
        Iterables.fill(list, supplier);
        assertEquals(list("test", "test", "test"), list);

        List<String> emptyList = new ArrayList<>();
        Iterables.fill(emptyList, supplier);
        assertTrue(emptyList.isEmpty());

        assertThrows(IllegalArgumentException.class, () -> Iterables.fill((List<String>) null, supplier));
    }

    @Test
    public void testFillListRangeSupplier() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        Supplier<String> supplier = () -> "x";
        Iterables.fill(list, 1, 3, supplier);
        assertEquals(list("a", "x", "x", "d"), list);

        List<String> shortList = new ArrayList<>(Arrays.asList("a"));
        Iterables.fill(shortList, 0, 3, supplier);
        assertEquals(list("x", "x", "x"), shortList);

        List<String> shortList2 = new ArrayList<>(Arrays.asList("a"));
        Iterables.fill(shortList2, 2, 4, supplier);
        assertEquals(list("a", null, "x", "x"), shortList2);
        Iterables.fill(list, 0, 5, supplier);
        assertEquals(list("x", "x", "x", "x", "x"), list);

        assertThrows(IllegalArgumentException.class, () -> Iterables.fill((List<String>) null, 0, 1, supplier));
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
    public void testCopyListRange() {
        List<String> src = list("s1", "s2", "s3", "s4");
        List<String> dest = new ArrayList<>(Arrays.asList("d1", "d2", "d3", "d4", "d5"));

        Iterables.copyRange(src, 1, dest, 2, 2);
        assertEquals(list("d1", "d2", "s2", "s3", "d5"), dest);

        List<String> destNonRandom = new LinkedList<>(Arrays.asList("d1", "d2", "d3", "d4", "d5"));
        Iterables.copyRange(src, 1, destNonRandom, 2, 2);
        assertEquals(list("d1", "d2", "s2", "s3", "d5"), destNonRandom);

        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.copyRange(src, 0, dest, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.copyRange(src, 3, dest, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.copyRange(src, 0, dest, 4, 2));
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

        assertThrows(IllegalArgumentException.class, () -> Iterables.orderedPermutations(null));
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
    @SuppressWarnings("unchecked")
    public void testCartesianProductVarArgs() {
        List<List<Integer>> cp = Iterables.cartesianProduct(list(1, 2), list(3, 4));
        assertEquals(4, cp.size());
        assertTrue(cp.contains(list(1, 3)));
        assertTrue(cp.contains(list(1, 4)));
        assertTrue(cp.contains(list(2, 3)));
        assertTrue(cp.contains(list(2, 4)));
        assertEquals(list(1, 3), cp.get(0));
        assertEquals(list(1, 4), cp.get(1));
        assertEquals(list(2, 3), cp.get(2));
        assertEquals(list(2, 4), cp.get(3));

        List<List<Object>> cpMixed = Iterables.cartesianProduct(list(1, 2), list("a"), list(true, false));
        assertEquals(4, cpMixed.size());
        assertTrue(cpMixed.contains(list(1, "a", true)));
        assertTrue(cpMixed.contains(list(2, "a", false)));
        assertEquals(list(1, "a", true), cpMixed.get(0));

        List<List<Integer>> cpEmptyList = Iterables.cartesianProduct(list(1, 2), list());
        assertTrue(cpEmptyList.isEmpty());

        List<List<Integer>> cpNoLists = Iterables.cartesianProduct();
        assertEquals(1, cpNoLists.size());
        assertTrue(cpNoLists.get(0).isEmpty());
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
    public void testMinInt() {
        int[] array = { 30, 10, 20 };
        OptionalInt result = Iterables.min(array);
        assertTrue(result.isPresent());
        assertEquals(10, result.get());

        assertFalse(Iterables.min((int[]) null).isPresent());
        assertFalse(Iterables.min(new int[0]).isPresent());
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
    public void testMinFloat() {
        float[] array = { 3.0f, 1.0f, 2.0f };
        OptionalFloat result = Iterables.min(array);
        assertTrue(result.isPresent());
        assertEquals(1.0f, result.get());

        assertFalse(Iterables.min((float[]) null).isPresent());
        assertFalse(Iterables.min(new float[0]).isPresent());
    }

    @Test
    public void testMinDouble() {
        double[] array = { 3.0, 1.0, 2.0 };
        OptionalDouble result = Iterables.min(array);
        assertTrue(result.isPresent());
        assertEquals(1.0, result.get());

        assertFalse(Iterables.min((double[]) null).isPresent());
        assertFalse(Iterables.min(new double[0]).isPresent());
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
    public void testMinInt_valueExtractor() {
        List<String> list = Arrays.asList("aaa", "bb", "c");
        ToIntFunction<String> lengthExtractor = String::length;

        OptionalInt result = Iterables.minInt(list, lengthExtractor);
        assertTrue(result.isPresent());
        assertEquals(1, result.get());

        assertFalse(Iterables.minInt((Iterable<String>) null, lengthExtractor).isPresent());
        assertFalse(Iterables.minInt(Collections.emptyList(), lengthExtractor).isPresent());
    }

    @Test
    public void testMinLong_valueExtractor() {
        List<String> list = Arrays.asList("aaa", "bb", "c");
        ToLongFunction<String> lengthExtractor = s -> (long) s.length();

        OptionalLong result = Iterables.minLong(list, lengthExtractor);
        assertTrue(result.isPresent());
        assertEquals(1L, result.get());
    }

    @Test
    public void testMinDouble_valueExtractor() {
        List<String> list = Arrays.asList("aaa", "bb", "c");
        ToDoubleFunction<String> lengthExtractor = s -> (double) s.length();

        OptionalDouble result = Iterables.minDouble(list, lengthExtractor);
        assertTrue(result.isPresent());
        assertEquals(1.0, result.get());
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
    public void testMaxInt() {
        int[] array = { 30, 10, 20 };
        OptionalInt result = Iterables.max(array);
        assertTrue(result.isPresent());
        assertEquals(30, result.get());
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
    public void testMaxBy() {
        List<String> list = Arrays.asList("a", "bb", "ccc");
        Function<String, Integer> lengthExtractor = String::length;

        Nullable<String> result = Iterables.maxBy(list, lengthExtractor);
        assertTrue(result.isPresent());
        assertEquals("ccc", result.get());
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
    public void testMinMaxWithComparator() {
        List<String> list = Arrays.asList("a", "bb", "ccc");
        Comparator<String> lengthComparator = Comparator.comparing(String::length);

        Optional<Pair<String, String>> result = Iterables.minMax(list, lengthComparator);
        assertTrue(result.isPresent());
        assertEquals("a", result.get().left());
        assertEquals("ccc", result.get().right());
    }

    @Test
    public void testMedianWithComparator() {
        List<String> list = Arrays.asList("a", "bb", "ccc");
        Comparator<String> lengthComparator = Comparator.comparing(String::length);

        Nullable<String> result = Iterables.median(list, lengthComparator);
        assertTrue(result.isPresent());
        assertEquals("bb", result.get());
    }

    @Test
    public void testKthLargestWithComparator() {
        List<String> list = Arrays.asList("a", "bb", "ccc", "dddd");
        Comparator<String> lengthComparator = Comparator.comparing(String::length);

        Nullable<String> result = Iterables.kthLargest(list, 2);
        assertTrue(result.isPresent());
        assertEquals("ccc", result.get());
    }

    @Test
    public void testSumInt() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        OptionalInt result = Iterables.sumInt(list);
        assertTrue(result.isPresent());
        assertEquals(6, result.get());

        assertFalse(Iterables.sumInt((Iterable<Integer>) null).isPresent());
        assertFalse(Iterables.sumInt(Collections.emptyList()).isPresent());
    }

    @Test
    public void testSumIntWithFunction() {
        List<String> list = Arrays.asList("a", "bb", "ccc");
        ToIntFunction<String> lengthExtractor = String::length;

        OptionalInt result = Iterables.sumInt(list, lengthExtractor);
        assertTrue(result.isPresent());
        assertEquals(6, result.get());
    }

    @Test
    public void testSumIntToLong() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        OptionalLong result = Iterables.sumIntToLong(list);
        assertTrue(result.isPresent());
        assertEquals(6L, result.get());
    }

    @Test
    public void testSumLong() {
        List<Long> list = Arrays.asList(1L, 2L, 3L);
        OptionalLong result = Iterables.sumLong(list);
        assertTrue(result.isPresent());
        assertEquals(6L, result.get());
    }

    @Test
    public void testSumDouble() {
        List<Double> list = Arrays.asList(1.0, 2.0, 3.0);
        OptionalDouble result = Iterables.sumDouble(list);
        assertTrue(result.isPresent());
        assertEquals(6.0, result.get());
    }

    @Test
    public void testSumBigInteger() {
        List<BigInteger> list = Arrays.asList(BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3));
        Optional<BigInteger> result = Iterables.sumBigInteger(list);
        assertTrue(result.isPresent());
        assertEquals(BigInteger.valueOf(6), result.get());

        assertFalse(Iterables.sumBigInteger((Iterable<BigInteger>) null).isPresent());
        assertFalse(Iterables.sumBigInteger(Collections.emptyList()).isPresent());
    }

    @Test
    public void testSumBigDecimal() {
        List<BigDecimal> list = Arrays.asList(BigDecimal.valueOf(1.0), BigDecimal.valueOf(2.0), BigDecimal.valueOf(3.0));
        Optional<BigDecimal> result = Iterables.sumBigDecimal(list);
        assertTrue(result.isPresent());
        assertEquals(BigDecimal.valueOf(6.0), result.get());
    }

    @Test
    public void testAverageInt() {
        Integer[] array = { 1, 2, 3 };
        OptionalDouble result = Iterables.averageInt(array);
        assertTrue(result.isPresent());
        assertEquals(2.0, result.get());

        assertFalse(Iterables.averageInt((Integer[]) null).isPresent());
        assertFalse(Iterables.averageInt(new Integer[0]).isPresent());
    }

    @Test
    public void testAverageIntWithRange() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        OptionalDouble result = Iterables.averageInt(array, 1, 4);
        assertTrue(result.isPresent());
        assertEquals(3.0, result.get());

        OptionalDouble emptyResult = Iterables.averageInt(array, 2, 2);
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testAverageIntWithFunction() {
        List<String> list = Arrays.asList("a", "bb", "ccc");
        ToIntFunction<String> lengthExtractor = String::length;

        OptionalDouble result = Iterables.averageInt(list, lengthExtractor);
        assertTrue(result.isPresent());
        assertEquals(2.0, result.get());
    }

    @Test
    public void testAverageLong() {
        Long[] array = { 1L, 2L, 3L };
        OptionalDouble result = Iterables.averageLong(array);
        assertTrue(result.isPresent());
        assertEquals(2.0, result.get());
    }

    @Test
    public void testAverageDouble() {
        Double[] array = { 1.0, 2.0, 3.0 };
        OptionalDouble result = Iterables.averageDouble(array);
        assertTrue(result.isPresent());
        assertEquals(2.0, result.get());
    }

    @Test
    public void testAverageBigInteger() {
        List<BigInteger> list = Arrays.asList(BigInteger.valueOf(2), BigInteger.valueOf(4), BigInteger.valueOf(6));
        Optional<BigDecimal> result = Iterables.averageBigInteger(list);
        assertTrue(result.isPresent());
        assertEquals(BigDecimal.valueOf(4), result.get());
    }

    @Test
    public void testAverageBigDecimal() {
        List<BigDecimal> list = Arrays.asList(BigDecimal.valueOf(2.0), BigDecimal.valueOf(4.0), BigDecimal.valueOf(6.0));
        Optional<BigDecimal> result = Iterables.averageBigDecimal(list);
        assertTrue(result.isPresent());
        assertEquals(BigDecimal.valueOf(4.0), result.get());
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
    public void testLastIndexOf() {
        Object[] array = { "a", "b", "c", "b" };
        OptionalInt result = Iterables.lastIndexOf(array, "b");
        assertTrue(result.isPresent());
        assertEquals(3, result.get());

        OptionalInt notFound = Iterables.lastIndexOf(array, "d");
        assertFalse(notFound.isPresent());
    }

    @Test
    public void testFindFirstOrLast() {
        String[] array = { "a", "bb", "ccc", "dd" };

        Nullable<String> result = Iterables.findFirstOrLast(array, s -> s.length() > 2, s -> s.length() == 2);
        assertTrue(result.isPresent());
        assertEquals("ccc", result.get());

        Nullable<String> fallbackResult = Iterables.findFirstOrLast(array, s -> s.length() > 10, s -> s.length() == 2);
        assertTrue(fallbackResult.isPresent());
        assertEquals("dd", fallbackResult.get());
    }

    @Test
    public void testFindFirstOrLastIndex() {
        String[] array = { "a", "bb", "ccc", "dd" };

        OptionalInt result = Iterables.findFirstOrLastIndex(array, s -> s.length() > 2, s -> s.length() == 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.get());
    }

    @Test
    public void testFindFirstAndLast() {
        String[] array = { "a", "bb", "ccc", "dd", "eee" };

        Pair<Nullable<String>, Nullable<String>> result = Iterables.findFirstAndLast(array, s -> s.length() >= 2);

        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals("bb", result.left().get());
        assertEquals("eee", result.right().get());
    }

    @Test
    public void testFindFirstAndLastWithDifferentPredicates() {
        String[] array = { "a", "bb", "ccc", "dd", "eee" };

        Pair<Nullable<String>, Nullable<String>> result = Iterables.findFirstAndLast(array, s -> s.length() == 2, s -> s.length() == 3);

        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals("bb", result.left().get());
        assertEquals("eee", result.right().get());
    }

    @Test
    public void testFindFirstAndLastIndex() {
        String[] array = { "a", "bb", "ccc", "dd", "eee" };

        Pair<OptionalInt, OptionalInt> result = Iterables.findFirstAndLastIndex(array, s -> s.length() >= 2);

        assertTrue(result.left().isPresent());
        assertTrue(result.right().isPresent());
        assertEquals(1, result.left().get());
        assertEquals(4, result.right().get());
    }

    @Test
    public void testFillArrayRangeWithSupplier() {
        String[] array = new String[5];
        Arrays.fill(array, "initial");
        Supplier<String> supplier = () -> "test";

        Iterables.fill(array, 1, 4, supplier);

        assertArrayEquals(new String[] { "initial", "test", "test", "test", "initial" }, array);
    }

    @Test
    public void testFillListRangeWithSupplier() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        Supplier<String> supplier = () -> "test";

        Iterables.fill(list, 1, 4, supplier);

        assertEquals(Arrays.asList("a", "test", "test", "test", "e"), list);
    }

    @Test
    public void testFillListExtension() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        Supplier<String> supplier = () -> "test";

        Iterables.fill(list, 0, 5, supplier);

        assertEquals(5, list.size());
        assertEquals(Arrays.asList("test", "test", "test", "test", "test"), list);
    }

    @Test
    public void testFillNullArray() {
        assertDoesNotThrow(() -> Iterables.fill((String[]) null, () -> "test"));
    }

    @Test
    public void testFillNullList() {
        assertThrows(IllegalArgumentException.class, () -> Iterables.fill((List<String>) null, () -> "test"));
    }

    @Test
    public void testReverseRandomAccessList() {
        List<String> list = Arrays.asList("a", "b", "c");
        List<String> reversed = Iterables.asReversed(list);

        assertEquals(Arrays.asList("c", "b", "a"), reversed);
        assertTrue(reversed instanceof RandomAccess);
    }

    @Test
    public void testReverseEmptyList() {
        List<String> emptyList = new ArrayList<>();
        List<String> reversed = Iterables.asReversed(emptyList);

        assertTrue(reversed.isEmpty());
    }

    @Test
    public void testReverseDoubleReverse() {
        List<String> list = Arrays.asList("a", "b", "c");
        List<String> doubleReversed = Iterables.asReversed(Iterables.asReversed(list));

        assertEquals(list, doubleReversed);
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
    public void testUnionCopyInto() {
        Set<String> set1 = new HashSet<>(Arrays.asList("a", "b"));
        Set<String> set2 = new HashSet<>(Arrays.asList("c", "d"));

        Iterables.SetView<String> union = Iterables.union(set1, set2);
        Set<String> result = union.copyInto(new HashSet<>());

        assertEquals(4, result.size());
        assertTrue(result.containsAll(Arrays.asList("a", "b", "c", "d")));
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
    public void testDifferenceWithEmptySets() {
        Set<String> emptySet = Collections.emptySet();
        Set<String> nonEmptySet = new HashSet<>(Arrays.asList("a", "b"));

        Iterables.SetView<String> difference1 = Iterables.difference(emptySet, nonEmptySet);
        assertTrue(difference1.isEmpty());

        Iterables.SetView<String> difference2 = Iterables.difference(nonEmptySet, emptySet);
        assertEquals(2, difference2.size());
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
    public void testSubSetEmptySet() {
        NavigableSet<Integer> emptySet = new TreeSet<>();
        Range<Integer> range = Range.closed(1, 5);

        NavigableSet<Integer> subset = Iterables.subSet(emptySet, range);

        assertTrue(subset.isEmpty());
    }

    @Test
    public void testPowerSetSingleElement() {
        Set<String> singleElementSet = Collections.singleton("a");
        Set<Set<String>> powerSet = Iterables.powerSet(singleElementSet);

        assertEquals(2, powerSet.size());
        assertTrue(powerSet.contains(Collections.emptySet()));
        assertTrue(powerSet.contains(Collections.singleton("a")));
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
    public void testRollupNull() {
        List<List<String>> rollup = Iterables.rollup(null);

        assertEquals(1, rollup.size());
        assertEquals(Collections.emptyList(), rollup.get(0));
    }

    @Test
    public void testRollupSingleElement() {
        List<String> singleElement = Arrays.asList("a");
        List<List<String>> rollup = Iterables.rollup(singleElement);

        assertEquals(2, rollup.size());
        assertEquals(Collections.emptyList(), rollup.get(0));
        assertEquals(Arrays.asList("a"), rollup.get(1));
    }

    @Test
    public void testPermutationsSingleElement() {
        List<String> singleElement = Arrays.asList("a");
        Collection<List<String>> perms = Iterables.permutations(singleElement);

        assertEquals(1, perms.size());
        assertEquals(Arrays.asList("a"), perms.iterator().next());
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
    public void testOrderedPermutationsEmpty() {
        List<String> emptyList = Collections.emptyList();
        Collection<List<String>> perms = Iterables.orderedPermutations(emptyList);

        assertFalse(perms.isEmpty());
        assertEquals(1, perms.size());
        assertEquals(Collections.emptyList(), perms.iterator().next());
    }

    @Test
    public void testCartesianProductThreeSets() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("1", "2");
        List<String> list3 = Arrays.asList("x", "y");

        List<List<String>> product = Iterables.cartesianProduct(list1, list2, list3);

        assertEquals(8, product.size());
        assertEquals(Arrays.asList("a", "1", "x"), product.get(0));
        assertEquals(Arrays.asList("b", "2", "y"), product.get(7));
    }

    @Test
    public void testCartesianProductWithEmptySet() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> emptyList = Collections.emptyList();

        List<List<String>> product = Iterables.cartesianProduct(list1, emptyList);

        assertTrue(product.isEmpty());
    }

    @Test
    public void testCartesianProductEmpty() {
        List<Collection<String>> emptyCollections = Collections.emptyList();
        List<List<String>> product = Iterables.cartesianProduct(emptyCollections);

        assertEquals(1, product.size());
        assertEquals(Collections.emptyList(), product.get(0));
    }

    @Test
    public void testCartesianProductSingleElement() {
        List<String> singleList = Arrays.asList("a");
        List<List<String>> product = Iterables.cartesianProduct(singleList);

        assertEquals(1, product.size());
        assertEquals(Arrays.asList("a"), product.get(0));
    }

    @Test
    public void testCartesianProductContains() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("1", "2");

        List<List<String>> product = Iterables.cartesianProduct(list1, list2);

        assertTrue(product.contains(Arrays.asList("a", "1")));
        assertTrue(product.contains(Arrays.asList("b", "2")));
        assertFalse(product.contains(Arrays.asList("c", "1")));
        assertFalse(product.contains(Arrays.asList("a", "1", "extra")));
        assertFalse(product.contains("not a list"));
    }

    @Test
    public void testCartesianProductRandomAccess() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("1", "2");

        List<List<String>> product = Iterables.cartesianProduct(list1, list2);

        assertTrue(product instanceof RandomAccess);

        assertEquals(Arrays.asList("b", "1"), product.get(2));
        assertEquals(Arrays.asList("a", "2"), product.get(1));
    }

    @Test
    public void testCartesianProductIndexOutOfBounds() {
        List<String> list1 = Arrays.asList("a");
        List<String> list2 = Arrays.asList("1");

        List<List<String>> product = Iterables.cartesianProduct(list1, list2);

        assertThrows(IndexOutOfBoundsException.class, () -> product.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> product.get(-1));

        List<List<String>> product2 = Lists.cartesianProduct(list1, list2);

        assertThrows(IndexOutOfBoundsException.class, () -> product2.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> product2.get(-1));
    }

    @Test
    public void testNullInputHandling() {
        assertFalse(Iterables.min((int[]) null).isPresent());
        assertFalse(Iterables.max((String[]) null).isPresent());
        assertFalse(Iterables.median((Integer[]) null).isPresent());
        assertFalse(Iterables.sumInt((Iterable<Integer>) null).isPresent());
        assertFalse(Iterables.averageDouble((Double[]) null).isPresent());
    }

    @Test
    public void testEmptyInputHandling() {
        assertFalse(Iterables.min(new int[0]).isPresent());
        assertFalse(Iterables.max(new String[0]).isPresent());
        assertFalse(Iterables.median(Collections.emptyList()).isPresent());
        assertFalse(Iterables.sumLong(Collections.emptyList()).isPresent());
        assertFalse(Iterables.averageInt(new Integer[0]).isPresent());
    }

    @Test
    public void testSingleElementCollections() {
        assertEquals(5, Iterables.min(new int[] { 5 }).get());
        assertEquals("test", Iterables.max(new String[] { "test" }).get());
        assertEquals(Integer.valueOf(42), Iterables.median(Arrays.asList(42)).get());
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
    public void testRangeValidation() {
        Integer[] array = { 1, 2, 3, 4, 5 };

        assertTrue(Iterables.averageInt(array, 1, 4).isPresent());

        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(array, -1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(array, 2, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.averageInt(array, 3, 2));
    }

    @Test
    public void testFirstNonNull_TwoParams_BothNull() {
        String result = Iterables.firstNonNull(null, null);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_TwoParams_FirstNonNull() {
        String result = Iterables.firstNonNull("first", null);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_TwoParams_SecondNonNull() {
        String result = Iterables.firstNonNull(null, "second");
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testFirstNonNull_TwoParams_BothNonNull() {
        String result = Iterables.firstNonNull("first", "second");
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_ThreeParams_AllNull() {
        String result = Iterables.firstNonNull(null, null, null);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_ThreeParams_FirstNonNull() {
        String result = Iterables.firstNonNull("first", null, null);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_ThreeParams_SecondNonNull() {
        String result = Iterables.firstNonNull(null, "second", null);
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testFirstNonNull_ThreeParams_ThirdNonNull() {
        String result = Iterables.firstNonNull(null, null, "third");
        Assertions.assertEquals("third", result);
    }

    @Test
    public void testFirstNonNull_ThreeParams_AllNonNull() {
        String result = Iterables.firstNonNull("first", "second", "third");
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_ThreeParams_SecondAndThirdNonNull() {
        String result = Iterables.firstNonNull(null, "second", "third");
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testFirstNonNull_Varargs_NullArray() {
        String result = Iterables.firstNonNull((String[]) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Varargs_EmptyArray() {
        String result = Iterables.firstNonNull(new String[0]);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Varargs_AllNull() {
        String result = Iterables.firstNonNull(null, null, null, null);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Varargs_MiddleNonNull() {
        String result = Iterables.firstNonNull(null, null, "middle", null, null);
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testFirstNonNull_Varargs_LastNonNull() {
        String result = Iterables.firstNonNull(null, null, null, "last");
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testFirstNonNull_Varargs_MultipleNonNull() {
        String result = Iterables.firstNonNull(null, "first", "second", null, "third");
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_Iterable_Null() {
        String result = Iterables.firstNonNull((Iterable<String>) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Iterable_Empty() {
        List<String> list = Collections.emptyList();
        String result = Iterables.firstNonNull(list);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Iterable_AllNull() {
        List<String> list = Arrays.asList(null, null, null);
        String result = Iterables.firstNonNull(list);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Iterable_FirstNonNull() {
        List<String> list = Arrays.asList("first", null, null);
        String result = Iterables.firstNonNull(list);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_Iterable_MiddleNonNull() {
        List<String> list = Arrays.asList(null, null, "middle", null);
        String result = Iterables.firstNonNull(list);
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testFirstNonNull_Iterable_MultipleNonNull() {
        List<String> list = Arrays.asList(null, "first", "second", null);
        String result = Iterables.firstNonNull(list);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_Iterator_Null() {
        String result = Iterables.firstNonNull((Iterator<String>) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Iterator_Empty() {
        Iterator<String> iter = Collections.<String> emptyList().iterator();
        String result = Iterables.firstNonNull(iter);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Iterator_AllNull() {
        Iterator<String> iter = Arrays.asList((String) null, (String) null, (String) null).iterator();
        String result = Iterables.firstNonNull(iter);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Iterator_FirstNonNull() {
        Iterator<String> iter = Arrays.asList("first", null, null).iterator();
        String result = Iterables.firstNonNull(iter);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_Iterator_MiddleNonNull() {
        Iterator<String> iter = Arrays.asList(null, null, "middle", null).iterator();
        String result = Iterables.firstNonNull(iter);
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testFirstNonNull_Iterator_MultipleNonNull() {
        Iterator<String> iter = Arrays.asList(null, "first", "second", null).iterator();
        String result = Iterables.firstNonNull(iter);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterable_NullIterableReturnsDefault() {
        String result = CommonUtil.firstNonNullOrDefault((Iterable<String>) null, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterable_EmptyReturnsDefault() {
        List<String> list = Collections.emptyList();
        String result = CommonUtil.firstNonNullOrDefault(list, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterable_AllNullReturnsDefault() {
        List<String> list = Arrays.asList(null, null, null);
        String result = CommonUtil.firstNonNullOrDefault(list, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterable_FirstNonNull() {
        List<String> list = Arrays.asList("first", null, null);
        String result = CommonUtil.firstNonNullOrDefault(list, "default");
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterable_MiddleNonNull() {
        List<String> list = Arrays.asList(null, null, "middle", null);
        String result = CommonUtil.firstNonNullOrDefault(list, "default");
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterable_NullDefaultThrowsException() {
        List<String> list = Arrays.asList("test");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.firstNonNullOrDefault(list, null);
        });
    }

    @Test
    public void testFirstNonNullOrDefault_Iterator_NullIteratorReturnsDefault() {
        String result = CommonUtil.firstNonNullOrDefault((Iterator<String>) null, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterator_EmptyReturnsDefault() {
        Iterator<String> iter = Collections.<String> emptyList().iterator();
        String result = CommonUtil.firstNonNullOrDefault(iter, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterator_AllNullReturnsDefault() {
        Iterator<String> iter = Arrays.asList((String) null, (String) null, (String) null).iterator();
        String result = CommonUtil.firstNonNullOrDefault(iter, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterator_FirstNonNull() {
        Iterator<String> iter = Arrays.asList("first", null, null).iterator();
        String result = CommonUtil.firstNonNullOrDefault(iter, "default");
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterator_MiddleNonNull() {
        Iterator<String> iter = Arrays.asList(null, null, "middle", null).iterator();
        String result = CommonUtil.firstNonNullOrDefault(iter, "default");
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterator_NullDefaultThrowsException() {
        Iterator<String> iter = Arrays.asList("test").iterator();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.firstNonNullOrDefault(iter, null);
        });
    }

    @Test
    public void testLastNonNull_TwoParams_BothNull() {
        String result = Iterables.lastNonNull(null, null);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_TwoParams_FirstNonNull() {
        String result = Iterables.lastNonNull("first", null);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testLastNonNull_TwoParams_SecondNonNull() {
        String result = Iterables.lastNonNull(null, "second");
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testLastNonNull_TwoParams_BothNonNull() {
        String result = Iterables.lastNonNull("first", "second");
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testLastNonNull_ThreeParams_AllNull() {
        String result = Iterables.lastNonNull(null, null, null);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_ThreeParams_FirstNonNull() {
        String result = Iterables.lastNonNull("first", null, null);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testLastNonNull_ThreeParams_SecondNonNull() {
        String result = Iterables.lastNonNull(null, "second", null);
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testLastNonNull_ThreeParams_ThirdNonNull() {
        String result = Iterables.lastNonNull(null, null, "third");
        Assertions.assertEquals("third", result);
    }

    @Test
    public void testLastNonNull_ThreeParams_AllNonNull() {
        String result = Iterables.lastNonNull("first", "second", "third");
        Assertions.assertEquals("third", result);
    }

    @Test
    public void testLastNonNull_ThreeParams_FirstAndSecondNonNull() {
        String result = Iterables.lastNonNull("first", "second", null);
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testLastNonNull_Varargs_NullArray() {
        String result = Iterables.lastNonNull((String[]) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Varargs_EmptyArray() {
        String result = Iterables.lastNonNull(new String[0]);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Varargs_AllNull() {
        String result = Iterables.lastNonNull(null, null, null, null);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Varargs_MiddleNonNull() {
        String result = Iterables.lastNonNull(null, null, "middle", null, null);
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testLastNonNull_Varargs_LastNonNull() {
        String result = Iterables.lastNonNull(null, null, null, "last");
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testLastNonNull_Varargs_MultipleNonNull() {
        String result = Iterables.lastNonNull(null, "first", "second", null, "third");
        Assertions.assertEquals("third", result);
    }

    @Test
    public void testLastNonNull_Iterable_Null() {
        String result = Iterables.lastNonNull((Iterable<String>) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Iterable_Empty() {
        List<String> list = Collections.emptyList();
        String result = Iterables.lastNonNull(list);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Iterable_AllNull() {
        List<String> list = Arrays.asList(null, null, null);
        String result = Iterables.lastNonNull(list);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Iterable_LastNonNull() {
        List<String> list = Arrays.asList(null, null, "last");
        String result = Iterables.lastNonNull(list);
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testLastNonNull_Iterable_MiddleNonNull() {
        List<String> list = Arrays.asList(null, "middle", null, null);
        String result = Iterables.lastNonNull(list);
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testLastNonNull_Iterable_MultipleNonNull() {
        List<String> list = Arrays.asList(null, "first", "second", null);
        String result = Iterables.lastNonNull(list);
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testLastNonNull_Iterable_RandomAccessList() {
        ArrayList<String> list = new ArrayList<>();
        list.add(null);
        list.add("first");
        list.add(null);
        list.add("last");
        list.add(null);
        String result = Iterables.lastNonNull(list);
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testLastNonNull_Iterable_NonRandomAccessList() {
        List<String> list = new LinkedList<>();
        list.add(null);
        list.add("first");
        list.add(null);
        list.add("last");
        list.add(null);
        String result = Iterables.lastNonNull(list);
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testLastNonNull_Iterator_Null() {
        String result = Iterables.lastNonNull((Iterator<String>) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Iterator_Empty() {
        Iterator<String> iter = Collections.<String> emptyList().iterator();
        String result = Iterables.lastNonNull(iter);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Iterator_AllNull() {
        Iterator<String> iter = Arrays.asList((String) null, (String) null, (String) null).iterator();
        String result = Iterables.lastNonNull(iter);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Iterator_LastNonNull() {
        Iterator<String> iter = Arrays.asList(null, null, "last").iterator();
        String result = Iterables.lastNonNull(iter);
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testLastNonNull_Iterator_MiddleNonNull() {
        Iterator<String> iter = Arrays.asList(null, "middle", null, null).iterator();
        String result = Iterables.lastNonNull(iter);
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testLastNonNull_Iterator_MultipleNonNull() {
        Iterator<String> iter = Arrays.asList(null, "first", "second", null, "third").iterator();
        String result = Iterables.lastNonNull(iter);
        Assertions.assertEquals("third", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterable_NullIterableReturnsDefault() {
        String result = CommonUtil.lastNonNullOrDefault((Iterable<String>) null, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterable_EmptyReturnsDefault() {
        List<String> list = Collections.emptyList();
        String result = CommonUtil.lastNonNullOrDefault(list, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterable_AllNullReturnsDefault() {
        List<String> list = Arrays.asList(null, null, null);
        String result = CommonUtil.lastNonNullOrDefault(list, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterable_LastNonNull() {
        List<String> list = Arrays.asList(null, null, "last");
        String result = CommonUtil.lastNonNullOrDefault(list, "default");
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterable_MiddleNonNull() {
        List<String> list = Arrays.asList(null, "middle", null, null);
        String result = CommonUtil.lastNonNullOrDefault(list, "default");
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterable_NullDefaultThrowsException() {
        List<String> list = Arrays.asList("test");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.lastNonNullOrDefault(list, null);
        });
    }

    @Test
    public void testLastNonNullOrDefault_Iterator_NullIteratorReturnsDefault() {
        String result = CommonUtil.lastNonNullOrDefault((Iterator<String>) null, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterator_EmptyReturnsDefault() {
        Iterator<String> iter = Collections.<String> emptyList().iterator();
        String result = CommonUtil.lastNonNullOrDefault(iter, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterator_AllNullReturnsDefault() {
        Iterator<String> iter = Arrays.asList((String) null, (String) null, (String) null).iterator();
        String result = CommonUtil.lastNonNullOrDefault(iter, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterator_LastNonNull() {
        Iterator<String> iter = Arrays.asList(null, null, "last").iterator();
        String result = CommonUtil.lastNonNullOrDefault(iter, "default");
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterator_MiddleNonNull() {
        Iterator<String> iter = Arrays.asList(null, "middle", null, null).iterator();
        String result = CommonUtil.lastNonNullOrDefault(iter, "default");
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterator_NullDefaultThrowsException() {
        Iterator<String> iter = Arrays.asList("test").iterator();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.lastNonNullOrDefault(iter, null);
        });
    }

    @Test
    public void testFirstNonNull_WithIntegers() {
        Integer result = Iterables.firstNonNull(null, 42, 100);
        Assertions.assertEquals(42, result);
    }

    @Test
    public void testLastNonNull_WithIntegers() {
        Integer result = Iterables.lastNonNull(10, null, 42);
        Assertions.assertEquals(42, result);
    }

    @Test
    public void testFirstNonNull_WithCustomObjects() {
        Object obj1 = new Object();
        Object obj2 = new Object();
        Object result = Iterables.firstNonNull(null, obj1, obj2);
        Assertions.assertSame(obj1, result);
    }

    @Test
    public void testLastNonNull_WithCustomObjects() {
        Object obj1 = new Object();
        Object obj2 = new Object();
        Object result = Iterables.lastNonNull(obj1, null, obj2);
        Assertions.assertSame(obj2, result);
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
    public void testMedian() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Nullable<Integer> result = Iterables.median(arr);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(3), result.get());

        assertTrue(Iterables.median(new Integer[0]).isEmpty());
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
    public void testEmptyCollectionHandling() {
        List<Integer> empty = new ArrayList<>();

        assertTrue(Iterables.min(empty).isEmpty());
        assertTrue(Iterables.max(empty).isEmpty());
        assertFalse(Iterables.sumInt(empty).isPresent());
        assertFalse(Iterables.averageInt(empty).isPresent());
        assertTrue(Iterables.median(empty).isEmpty());
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

    // ===================== firstNonNull / lastNonNull edge cases =====================

    @Test
    public void testFirstNonNull_SingleElement() {
        assertEquals("a", Iterables.firstNonNull(new String[] { "a" }));
        assertNull(Iterables.firstNonNull(new String[] { null }));
    }

    @Test
    public void testLastNonNull_SingleElement() {
        assertEquals("a", Iterables.lastNonNull(new String[] { "a" }));
        assertNull(Iterables.lastNonNull(new String[] { null }));
    }

    // ===================== min primitive overloads =====================

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
    public void testMinInt_Present() {
        OptionalInt result = Iterables.min(3, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testMinLong_Present() {
        OptionalLong result = Iterables.min(3L, 1L, 2L);
        assertTrue(result.isPresent());
        assertEquals(1L, result.get());
    }

    @Test
    public void testMinFloat_Present() {
        OptionalFloat result = Iterables.min(3.0f, 1.0f, 2.0f);
        assertTrue(result.isPresent());
        assertEquals(1.0f, result.get());
    }

    @Test
    public void testMinDouble_Present() {
        OptionalDouble result = Iterables.min(3.0, 1.0, 2.0);
        assertTrue(result.isPresent());
        assertEquals(1.0, result.get());
    }

    // ===================== max primitive overloads =====================

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
    public void testMaxInt_Present() {
        OptionalInt result = Iterables.max(3, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.get());
    }

    @Test
    public void testMaxLong_Present() {
        OptionalLong result = Iterables.max(3L, 1L, 2L);
        assertTrue(result.isPresent());
        assertEquals(3L, result.get());
    }

    @Test
    public void testMaxFloat_Present() {
        OptionalFloat result = Iterables.max(3.0f, 1.0f, 2.0f);
        assertTrue(result.isPresent());
        assertEquals(3.0f, result.get());
    }

    @Test
    public void testMaxDouble_Present() {
        OptionalDouble result = Iterables.max(3.0, 1.0, 2.0);
        assertTrue(result.isPresent());
        assertEquals(3.0, result.get());
    }

    // ===================== min/max empty array overloads =====================

    @Test
    public void testMinChar_Empty() {
        assertFalse(Iterables.min(new char[0]).isPresent());
        assertFalse(Iterables.min((char[]) null).isPresent());
    }

    @Test
    public void testMinByte_Empty() {
        assertFalse(Iterables.min(new byte[0]).isPresent());
        assertFalse(Iterables.min((byte[]) null).isPresent());
    }

    @Test
    public void testMinShort_Empty() {
        assertFalse(Iterables.min(new short[0]).isPresent());
        assertFalse(Iterables.min((short[]) null).isPresent());
    }

    @Test
    public void testMinInt_Empty() {
        assertFalse(Iterables.min(new int[0]).isPresent());
        assertFalse(Iterables.min((int[]) null).isPresent());
    }

    @Test
    public void testMinLong_Empty() {
        assertFalse(Iterables.min(new long[0]).isPresent());
        assertFalse(Iterables.min((long[]) null).isPresent());
    }

    @Test
    public void testMinFloat_Empty() {
        assertFalse(Iterables.min(new float[0]).isPresent());
        assertFalse(Iterables.min((float[]) null).isPresent());
    }

    @Test
    public void testMinDouble_Empty() {
        assertFalse(Iterables.min(new double[0]).isPresent());
        assertFalse(Iterables.min((double[]) null).isPresent());
    }

    @Test
    public void testMaxChar_Empty() {
        assertFalse(Iterables.max(new char[0]).isPresent());
        assertFalse(Iterables.max((char[]) null).isPresent());
    }

    @Test
    public void testMaxByte_Empty() {
        assertFalse(Iterables.max(new byte[0]).isPresent());
        assertFalse(Iterables.max((byte[]) null).isPresent());
    }

    @Test
    public void testMaxShort_Empty() {
        assertFalse(Iterables.max(new short[0]).isPresent());
        assertFalse(Iterables.max((short[]) null).isPresent());
    }

    @Test
    public void testMaxInt_Empty() {
        assertFalse(Iterables.max(new int[0]).isPresent());
        assertFalse(Iterables.max((int[]) null).isPresent());
    }

    @Test
    public void testMaxLong_Empty() {
        assertFalse(Iterables.max(new long[0]).isPresent());
        assertFalse(Iterables.max((long[]) null).isPresent());
    }

    @Test
    public void testMaxFloat_Empty() {
        assertFalse(Iterables.max(new float[0]).isPresent());
        assertFalse(Iterables.max((float[]) null).isPresent());
    }

    @Test
    public void testMaxDouble_Empty() {
        assertFalse(Iterables.max(new double[0]).isPresent());
        assertFalse(Iterables.max((double[]) null).isPresent());
    }

    // ===================== minBy/maxBy with Iterator =====================

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

    // ===================== minInt/maxInt/minLong/maxLong/minDouble/maxDouble Iterator overloads =====================

    @Test
    public void testMinIntIterator_Dedicated() {
        OptionalInt result = Iterables.minInt(Arrays.asList("a", "bb", "ccc").iterator(),
                (com.landawn.abacus.util.function.ToIntFunction<String>) String::length);
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testMaxIntIterator_Dedicated() {
        OptionalInt result = Iterables.maxInt(Arrays.asList("a", "bb", "ccc").iterator(),
                (com.landawn.abacus.util.function.ToIntFunction<String>) String::length);
        assertTrue(result.isPresent());
        assertEquals(3, result.get());
    }

    @Test
    public void testMinLongIterator_Dedicated() {
        OptionalLong result = Iterables.minLong(Arrays.asList("a", "bb", "ccc").iterator(),
                (com.landawn.abacus.util.function.ToLongFunction<String>) s -> (long) s.length());
        assertTrue(result.isPresent());
        assertEquals(1L, result.get());
    }

    @Test
    public void testMaxLongIterator_Dedicated() {
        OptionalLong result = Iterables.maxLong(Arrays.asList("a", "bb", "ccc").iterator(),
                (com.landawn.abacus.util.function.ToLongFunction<String>) s -> (long) s.length());
        assertTrue(result.isPresent());
        assertEquals(3L, result.get());
    }

    @Test
    public void testMinDoubleIterator_Dedicated() {
        OptionalDouble result = Iterables.minDouble(Arrays.asList("a", "bb", "ccc").iterator(),
                (com.landawn.abacus.util.function.ToDoubleFunction<String>) s -> (double) s.length());
        assertTrue(result.isPresent());
        assertEquals(1.0, result.get());
    }

    @Test
    public void testMaxDoubleIterator_Dedicated() {
        OptionalDouble result = Iterables.maxDouble(Arrays.asList("a", "bb", "ccc").iterator(),
                (com.landawn.abacus.util.function.ToDoubleFunction<String>) s -> (double) s.length());
        assertTrue(result.isPresent());
        assertEquals(3.0, result.get());
    }

    // ===================== minMax Iterator overloads =====================

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

    // ===================== sumBigInteger / sumBigDecimal with function =====================

    @Test
    public void testSumBigIntegerWithFunction_Dedicated() {
        u.Optional<BigInteger> result = Iterables.sumBigInteger(Arrays.asList("10", "20", "30"), s -> new BigInteger(s));
        assertTrue(result.isPresent());
        assertEquals(BigInteger.valueOf(60), result.get());
    }

    @Test
    public void testSumBigIntegerWithFunction_Empty() {
        u.Optional<BigInteger> result = Iterables.sumBigInteger(Collections.<String> emptyList(), s -> new BigInteger(s));
        assertFalse(result.isPresent());
    }

    @Test
    public void testSumBigDecimalWithFunction_Dedicated() {
        u.Optional<BigDecimal> result = Iterables.sumBigDecimal(Arrays.asList("10.5", "20.5"), s -> new BigDecimal(s));
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("31.0"), result.get());
    }

    @Test
    public void testSumBigDecimalWithFunction_Empty() {
        u.Optional<BigDecimal> result = Iterables.sumBigDecimal(Collections.<String> emptyList(), s -> new BigDecimal(s));
        assertFalse(result.isPresent());
    }

    // ===================== averageBigInteger / averageBigDecimal with function =====================

    @Test
    public void testAverageBigIntegerWithFunction_Dedicated() {
        u.Optional<BigDecimal> result = Iterables.averageBigInteger(Arrays.asList("10", "20", "30"), s -> new BigInteger(s));
        assertTrue(result.isPresent());
    }

    @Test
    public void testAverageBigIntegerWithFunction_Empty() {
        u.Optional<BigDecimal> result = Iterables.averageBigInteger(Collections.<String> emptyList(), s -> new BigInteger(s));
        assertFalse(result.isPresent());
    }

    @Test
    public void testAverageBigDecimalWithFunction_Dedicated() {
        u.Optional<BigDecimal> result = Iterables.averageBigDecimal(Arrays.asList("10.0", "20.0"), s -> new BigDecimal(s));
        assertTrue(result.isPresent());
    }

    @Test
    public void testAverageBigDecimalWithFunction_Empty() {
        u.Optional<BigDecimal> result = Iterables.averageBigDecimal(Collections.<String> emptyList(), s -> new BigDecimal(s));
        assertFalse(result.isPresent());
    }

    // ===================== indexOf / lastIndexOf Array overloads =====================

    @Test
    public void testIndexOfArray_Dedicated() {
        OptionalInt result = Iterables.indexOf(new Object[] { "a", "b", "c" }, "b");
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testIndexOfArray_NotFound() {
        OptionalInt result = Iterables.indexOf(new Object[] { "a", "b", "c" }, "z");
        assertFalse(result.isPresent());
    }

    @Test
    public void testLastIndexOfArray_Dedicated() {
        OptionalInt result = Iterables.lastIndexOf(new Object[] { "a", "b", "a", "c" }, "a");
        assertTrue(result.isPresent());
        assertEquals(2, result.get());
    }

    @Test
    public void testLastIndexOfArray_NotFound() {
        OptionalInt result = Iterables.lastIndexOf(new Object[] { "a", "b", "c" }, "z");
        assertFalse(result.isPresent());
    }

    // ===================== indexOf / lastIndexOf Collection overloads =====================

    @Test
    public void testIndexOfCollection_Dedicated() {
        OptionalInt result = Iterables.indexOf(Arrays.asList("a", "b", "c"), "b");
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testIndexOfCollection_NotFound() {
        OptionalInt result = Iterables.indexOf(Arrays.asList("a", "b", "c"), "z");
        assertFalse(result.isPresent());
    }

    @Test
    public void testLastIndexOfCollection_Dedicated() {
        OptionalInt result = Iterables.lastIndexOf(Arrays.asList("a", "b", "a", "c"), "a");
        assertTrue(result.isPresent());
        assertEquals(2, result.get());
    }

    @Test
    public void testLastIndexOfCollection_NotFound() {
        OptionalInt result = Iterables.lastIndexOf(Arrays.asList("a", "b", "c"), "z");
        assertFalse(result.isPresent());
    }

    // ===================== fill Array/List overloads =====================

    @Test
    public void testFillArray_Dedicated() {
        String[] arr = new String[3];
        Iterables.fill(arr, () -> "x");
        assertArrayEquals(new String[] { "x", "x", "x" }, arr);
    }

    @Test
    public void testFillArrayRange_Dedicated() {
        String[] arr = new String[5];
        Iterables.fill(arr, 1, 4, () -> "y");
        assertNull(arr[0]);
        assertEquals("y", arr[1]);
        assertEquals("y", arr[2]);
        assertEquals("y", arr[3]);
        assertNull(arr[4]);
    }

    @Test
    public void testFillList_Dedicated() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Iterables.fill(list, () -> "z");
        assertEquals(Arrays.asList("z", "z", "z"), list);
    }

    @Test
    public void testFillListRange_Dedicated() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        Iterables.fill(list, 1, 3, () -> "z");
        assertEquals(Arrays.asList("a", "z", "z", "d"), list);
    }

    // ===================== copyInto / copyRange =====================

    @Test
    public void testCopyInto_Dedicated() {
        List<Integer> src = Arrays.asList(1, 2, 3);
        List<Integer> dest = new ArrayList<>(Arrays.asList(10, 20, 30));
        Iterables.copyInto(src, dest);
        assertEquals(Arrays.asList(1, 2, 3), dest);
    }

    @Test
    public void testCopyRange_Dedicated() {
        List<Integer> src = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> dest = new ArrayList<>(Arrays.asList(10, 20, 30, 40, 50));
        Iterables.copyRange(src, 1, dest, 2, 2);
        assertEquals(Integer.valueOf(2), dest.get(2));
        assertEquals(Integer.valueOf(3), dest.get(3));
    }

    // ===================== asReversed =====================

    @Test
    public void testAsReversed_Dedicated() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        List<Integer> reversed = Iterables.asReversed(list);
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), reversed);
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

    // ===================== union / intersection / difference / symmetricDifference =====================

    @Test
    public void testUnion_Dedicated() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(3, 4, 5));
        Set<Integer> result = Iterables.union(s1, s2).copyInto(new HashSet<>());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3, 4, 5)), result);
    }

    @Test
    public void testIntersection_Dedicated() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(2, 3, 4));
        Set<Integer> result = Iterables.intersection(s1, s2).copyInto(new HashSet<>());
        assertEquals(new HashSet<>(Arrays.asList(2, 3)), result);
    }

    @Test
    public void testDifference_Dedicated() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(2, 3, 4));
        Set<Integer> result = Iterables.difference(s1, s2).copyInto(new HashSet<>());
        assertEquals(new HashSet<>(Arrays.asList(1)), result);
    }

    @Test
    public void testSymmetricDifference_Dedicated() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(2, 3, 4));
        Set<Integer> result = Iterables.symmetricDifference(s1, s2).copyInto(new HashSet<>());
        assertEquals(new HashSet<>(Arrays.asList(1, 4)), result);
    }

    // ===================== subSet =====================

    @Test
    public void testSubSet_Dedicated() {
        NavigableSet<Integer> set = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        NavigableSet<Integer> result = Iterables.subSet(set, Range.closed(3, 7));
        assertEquals(new TreeSet<>(Arrays.asList(3, 4, 5, 6, 7)), result);
    }

    // ===================== powerSet =====================

    @Test
    public void testPowerSet_Dedicated() {
        Set<Set<Integer>> result = Iterables.powerSet(new HashSet<>(Arrays.asList(1, 2)));
        assertEquals(4, result.size());
    }

    // ===================== rollup =====================

    @Test
    public void testRollup_Dedicated() {
        List<List<Integer>> result = Iterables.rollup(Arrays.asList(1, 2, 3));
        assertEquals(4, result.size());
        assertEquals(Arrays.asList(), result.get(0));
        assertEquals(Arrays.asList(1), result.get(1));
        assertEquals(Arrays.asList(1, 2), result.get(2));
        assertEquals(Arrays.asList(1, 2, 3), result.get(3));
    }

    // ===================== permutations =====================

    @Test
    public void testPermutations_Dedicated() {
        Collection<List<Integer>> result = Iterables.permutations(Arrays.asList(1, 2, 3));
        assertEquals(6, result.size());
    }

    // ===================== orderedPermutations =====================

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

    // ===================== cartesianProduct =====================

    @Test
    @SuppressWarnings("unchecked")
    public void testCartesianProduct_Dedicated() {
        List<List<Integer>> result = Iterables.cartesianProduct(Arrays.asList(1, 2), Arrays.asList(3, 4));
        assertEquals(4, result.size());
        assertTrue(result.contains(Arrays.asList(1, 3)));
        assertTrue(result.contains(Arrays.asList(1, 4)));
        assertTrue(result.contains(Arrays.asList(2, 3)));
        assertTrue(result.contains(Arrays.asList(2, 4)));
    }

    @Test
    public void testCartesianProductCollection_Dedicated() {
        List<Collection<Integer>> collections = new ArrayList<>();
        collections.add(Arrays.asList(1, 2));
        collections.add(Arrays.asList(3, 4));
        List<List<Integer>> result = Iterables.cartesianProduct(collections);
        assertEquals(4, result.size());
    }

    // ===================== findFirstOrLast Array/Collection =====================

    @Test
    public void testFindFirstOrLastArray_Dedicated() {
        Nullable<Integer> result = Iterables.findFirstOrLast(new Integer[] { 1, 2, 3, 4, 5 }, i -> i > 10, i -> i < 3);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(2), result.get());
    }

    @Test
    public void testFindFirstOrLastArray_FirstFound() {
        Nullable<Integer> result = Iterables.findFirstOrLast(new Integer[] { 1, 2, 3, 4, 5 }, i -> i > 3, i -> i < 3);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(4), result.get());
    }

    @Test
    public void testFindFirstOrLastArray_NoneFound() {
        Nullable<Integer> result = Iterables.findFirstOrLast(new Integer[] { 1, 2, 3 }, i -> i > 10, i -> i > 10);
        assertFalse(result.isPresent());
    }

    // ===================== findFirstOrLastIndex Array/Collection =====================

    @Test
    public void testFindFirstOrLastIndexArray_Dedicated() {
        OptionalInt result = Iterables.findFirstOrLastIndex(new Integer[] { 1, 2, 3, 4, 5 }, i -> i > 10, i -> i < 3);
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testFindFirstOrLastIndexArray_NoneFound() {
        OptionalInt result = Iterables.findFirstOrLastIndex(new Integer[] { 1, 2, 3 }, i -> i > 10, i -> i > 10);
        assertFalse(result.isPresent());
    }

    // ===================== findFirstAndLast =====================

    @Test
    public void testFindFirstAndLastArray_SinglePredicate_Dedicated() {
        Pair<Nullable<Integer>, Nullable<Integer>> result = Iterables.findFirstAndLast(new Integer[] { 1, 2, 3, 4, 5 }, i -> i % 2 == 0);
        assertTrue(result.left().isPresent());
        assertEquals(Integer.valueOf(2), result.left().get());
        assertTrue(result.right().isPresent());
        assertEquals(Integer.valueOf(4), result.right().get());
    }

    @Test
    public void testFindFirstAndLastArray_TwoPredicates_Dedicated() {
        Pair<Nullable<Integer>, Nullable<Integer>> result = Iterables.findFirstAndLast(new Integer[] { 1, 2, 3, 4, 5 }, i -> i > 3, i -> i < 3);
        assertTrue(result.left().isPresent());
        assertEquals(Integer.valueOf(4), result.left().get());
        assertTrue(result.right().isPresent());
        assertEquals(Integer.valueOf(2), result.right().get());
    }

    // ===================== findFirstAndLastIndex =====================

    @Test
    public void testFindFirstAndLastIndexArray_SinglePredicate_Dedicated() {
        Pair<OptionalInt, OptionalInt> result = Iterables.findFirstAndLastIndex(new Integer[] { 1, 2, 3, 4, 5 }, i -> i % 2 == 0);
        assertTrue(result.left().isPresent());
        assertEquals(1, result.left().get());
        assertTrue(result.right().isPresent());
        assertEquals(3, result.right().get());
    }

    @Test
    public void testFindFirstAndLastIndexCollection_SinglePredicate_Dedicated() {
        Pair<OptionalInt, OptionalInt> result = Iterables.findFirstAndLastIndex(Arrays.asList(1, 2, 3, 4, 5), i -> i % 2 == 0);
        assertTrue(result.left().isPresent());
        assertEquals(1, result.left().get());
        assertTrue(result.right().isPresent());
        assertEquals(3, result.right().get());
    }

    // ===================== sumInt / sumLong / sumDouble empty iterable =====================

    @Test
    public void testSumInt_EmptyIterable() {
        assertFalse(Iterables.sumInt(Collections.<Integer> emptyList()).isPresent());
    }

    @Test
    public void testSumLong_EmptyIterable() {
        assertFalse(Iterables.sumLong(Collections.<Long> emptyList()).isPresent());
    }

    @Test
    public void testSumDouble_EmptyIterable() {
        assertFalse(Iterables.sumDouble(Collections.<Double> emptyList()).isPresent());
    }

    @Test
    public void testSumIntToLong_EmptyIterable() {
        assertFalse(Iterables.sumIntToLong(Collections.<Integer> emptyList()).isPresent());
    }

    @Test
    public void testSumBigInteger_EmptyIterable() {
        assertFalse(Iterables.sumBigInteger(Collections.<BigInteger> emptyList()).isPresent());
    }

    @Test
    public void testSumBigDecimal_EmptyIterable() {
        assertFalse(Iterables.sumBigDecimal(Collections.<BigDecimal> emptyList()).isPresent());
    }

    // ===================== averageInt / averageLong / averageDouble empty inputs =====================

    @Test
    public void testAverageInt_EmptyArray() {
        assertFalse(Iterables.averageInt(new Integer[0]).isPresent());
    }

    @Test
    public void testAverageInt_EmptyIterable() {
        assertFalse(Iterables.averageInt(Collections.<Integer> emptyList()).isPresent());
    }

    @Test
    public void testAverageLong_EmptyArray() {
        assertFalse(Iterables.averageLong(new Long[0]).isPresent());
    }

    @Test
    public void testAverageLong_EmptyIterable() {
        assertFalse(Iterables.averageLong(Collections.<Long> emptyList()).isPresent());
    }

    @Test
    public void testAverageDouble_EmptyArray() {
        assertFalse(Iterables.averageDouble(new Double[0]).isPresent());
    }

    @Test
    public void testAverageDouble_EmptyIterable() {
        assertFalse(Iterables.averageDouble(Collections.<Double> emptyList()).isPresent());
    }

    @Test
    public void testAverageBigInteger_EmptyIterable() {
        assertFalse(Iterables.averageBigInteger(Collections.<BigInteger> emptyList()).isPresent());
    }

    @Test
    public void testAverageBigDecimal_EmptyIterable() {
        assertFalse(Iterables.averageBigDecimal(Collections.<BigDecimal> emptyList()).isPresent());
    }

    // ===================== median / kthLargest empty inputs =====================

    @Test
    public void testMedian_EmptyArray() {
        assertFalse(Iterables.median(new Integer[0]).isPresent());
    }

    @Test
    public void testMedian_EmptyCollection() {
        assertFalse(Iterables.median(Collections.<Integer> emptyList()).isPresent());
    }

    @Test
    public void testKthLargest_EmptyArray() {
        assertFalse(Iterables.kthLargest(new Integer[0], 1).isPresent());
    }

    @Test
    public void testKthLargest_EmptyCollection() {
        assertFalse(Iterables.kthLargest(Collections.<Integer> emptyList(), 1).isPresent());
    }

    // ===================== min/max with Iterator and Comparator =====================

    @Test
    public void testMinIteratorWithComparator_Dedicated() {
        Nullable<Integer> result = Iterables.min(Arrays.asList(3, 1, 4, 1, 5).iterator(), Comparator.reverseOrder());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(5), result.get());
    }

    @Test
    public void testMaxIteratorWithComparator_Dedicated() {
        Nullable<Integer> result = Iterables.max(Arrays.asList(3, 1, 4, 1, 5).iterator(), Comparator.reverseOrder());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get());
    }

    // ===================== min/max with Iterator Comparable =====================

    @Test
    public void testMinIterator_Dedicated() {
        Nullable<Integer> result = Iterables.min(Arrays.asList(3, 1, 4, 1, 5).iterator());
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
    public void testMinIterator_Empty() {
        Nullable<Integer> result = Iterables.min(Collections.<Integer> emptyList().iterator());
        assertFalse(result.isPresent());
    }

    @Test
    public void testMaxIterator_Empty() {
        Nullable<Integer> result = Iterables.max(Collections.<Integer> emptyList().iterator());
        assertFalse(result.isPresent());
    }

    // ===================== asReversed - remove / subList / clear =====================

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
    public void testAsReversed_Clear() {
        List<Integer> original = list(1, 2, 3);
        List<Integer> reversed = Iterables.asReversed(original);

        reversed.clear();
        assertTrue(original.isEmpty());
        assertTrue(reversed.isEmpty());
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
    public void testAsReversed_DoubleReverseLinkedList() {
        LinkedList<Integer> original = new LinkedList<>(Arrays.asList(1, 2, 3));
        List<Integer> doubleReversed = Iterables.asReversed(Iterables.asReversed(original));

        // double reverse should return the original forward list
        assertEquals(list(1, 2, 3), doubleReversed);
    }

    // ===================== copyRange edge cases =====================

    @Test
    public void testCopyRange_ZeroLength() {
        List<String> src = list("a", "b", "c");
        List<String> dest = list("x", "y", "z");

        Iterables.copyRange(src, 1, dest, 1, 0);
        assertEquals(list("x", "y", "z"), dest);
    }

    // ===================== fill edge cases =====================

    @Test
    public void testFillArrayRange_NullArray() {
        assertDoesNotThrow(() -> Iterables.fill((String[]) null, 0, 0, () -> "x"));
    }

    // ===================== SetView immutability =====================

    @Test
    public void testUnion_ImmutableView() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(3, 4));
        Iterables.SetView<Integer> union = Iterables.union(s1, s2);

        assertThrows(UnsupportedOperationException.class, () -> union.add(5));
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
    public void testDifference_ImmutableView() {
        Set<Integer> s1 = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> s2 = new HashSet<>(Arrays.asList(2, 3, 4));
        Iterables.SetView<Integer> diff = Iterables.difference(s1, s2);

        assertThrows(UnsupportedOperationException.class, () -> diff.add(5));
        assertTrue(diff.contains(1));
    }

    // ===================== min/max with null elements =====================

    @Test
    public void testMinBy_NullArray() {
        Nullable<String> result = Iterables.minBy((String[]) null, String::length);
        assertFalse(result.isPresent());
    }

    @Test
    public void testMaxBy_NullArray() {
        Nullable<String> result = Iterables.maxBy((String[]) null, String::length);
        assertFalse(result.isPresent());
    }

    @Test
    public void testMinBy_EmptyArray() {
        Nullable<String> result = Iterables.minBy(new String[0], String::length);
        assertFalse(result.isPresent());
    }

    @Test
    public void testMaxBy_EmptyArray() {
        Nullable<String> result = Iterables.maxBy(new String[0], String::length);
        assertFalse(result.isPresent());
    }

    // ===================== minMax edge cases =====================

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

    // ===================== indexOf / lastIndexOf edge cases =====================

    @Test
    public void testIndexOf_NullArray() {
        OptionalInt result = Iterables.indexOf((Object[]) null, "x");
        assertFalse(result.isPresent());
    }

    @Test
    public void testLastIndexOf_NullArray() {
        OptionalInt result = Iterables.lastIndexOf((Object[]) null, "x");
        assertFalse(result.isPresent());
    }

    @Test
    public void testIndexOf_NullCollection() {
        OptionalInt result = Iterables.indexOf((Collection<?>) null, "x");
        assertFalse(result.isPresent());
    }

    @Test
    public void testLastIndexOf_NullCollection() {
        OptionalInt result = Iterables.lastIndexOf((Collection<?>) null, "x");
        assertFalse(result.isPresent());
    }

    @Test
    public void testIndexOf_FindNull() {
        OptionalInt result = Iterables.indexOf(new Object[] { "a", null, "b" }, null);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());
    }

    @Test
    public void testLastIndexOf_FindNull() {
        OptionalInt result = Iterables.lastIndexOf(new Object[] { "a", null, "b", null }, null);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    // ===================== kthLargest edge cases =====================

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

    // ===================== median edge cases =====================

    @Test
    public void testMedian_SingleElement() {
        Nullable<Integer> result = Iterables.median(new Integer[] { 42 });
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(42), result.get());
    }

    @Test
    public void testMedian_TwoElements() {
        Nullable<Integer> result = Iterables.median(new Integer[] { 1, 2 });
        assertTrue(result.isPresent());
        // median of [1,2] is element at index (2-1)/2 = 0, which is the smaller element
        assertEquals(Integer.valueOf(1), result.get());
    }

    // ===================== sum with single element =====================

    @Test
    public void testSumInt_SingleElement() {
        OptionalInt result = Iterables.sumInt(Arrays.asList(42));
        assertTrue(result.isPresent());
        assertEquals(42, result.getAsInt());
    }

    @Test
    public void testSumLong_SingleElement() {
        OptionalLong result = Iterables.sumLong(Arrays.asList(42L));
        assertTrue(result.isPresent());
        assertEquals(42L, result.getAsLong());
    }

    @Test
    public void testSumDouble_SingleElement() {
        OptionalDouble result = Iterables.sumDouble(Arrays.asList(3.14));
        assertTrue(result.isPresent());
        assertEquals(3.14, result.getAsDouble(), 0.001);
    }

    // ===================== permutations edge cases =====================

    @Test
    public void testPermutations_SingleElement() {
        Collection<List<String>> perms = Iterables.permutations(Arrays.asList("a"));
        assertEquals(1, perms.size());
        assertEquals(list("a"), perms.iterator().next());
    }

    @Test
    public void testPermutations_ContainsCheck() {
        Collection<List<Integer>> perms = Iterables.permutations(Arrays.asList(1, 2));
        assertTrue(perms.contains(list(1, 2)));
        assertTrue(perms.contains(list(2, 1)));
        assertFalse(perms.contains(list(1, 3)));
        assertFalse(perms.contains("not a list"));
    }

    @Test
    public void testOrderedPermutations_ContainsCheck() {
        Collection<List<Integer>> perms = Iterables.orderedPermutations(Arrays.asList(1, 2));
        assertTrue(perms.contains(list(1, 2)));
        assertTrue(perms.contains(list(2, 1)));
        assertFalse(perms.contains(list(1, 3)));
        assertFalse(perms.contains("not a list"));
    }

    // ===================== rollup edge cases =====================

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

    // ===================== cartesianProduct edge cases =====================

    @Test
    public void testCartesianProduct_ContainsCheck() {
        List<List<Integer>> product = Iterables.cartesianProduct(Arrays.asList(1, 2), Arrays.asList(3, 4));
        assertTrue(product.contains(list(1, 3)));
        assertTrue(product.contains(list(2, 4)));
        assertFalse(product.contains(list(1, 5)));
        assertFalse(product.contains("not a list"));
    }

    // ===================== powerSet edge cases =====================

    @Test
    public void testPowerSet_SingleElement() {
        Set<Set<String>> result = Iterables.powerSet(new HashSet<>(Arrays.asList("a")));
        assertEquals(2, result.size());
        assertTrue(result.contains(new HashSet<>()));
        assertTrue(result.contains(new HashSet<>(Arrays.asList("a"))));
    }

    @Test
    public void testPowerSet_ContainsCheck() {
        Set<Set<Integer>> result = Iterables.powerSet(new HashSet<>(Arrays.asList(1, 2)));
        assertTrue(result.contains(new HashSet<>(Arrays.asList(1))));
        assertTrue(result.contains(new HashSet<>(Arrays.asList(1, 2))));
        assertFalse(result.contains(new HashSet<>(Arrays.asList(3))));
        assertFalse(result.contains("not a set"));
    }

    // ===================== firstNonNull / lastNonNull error paths =====================

    @Test
    public void testFirstNonNull_AllNull_TwoArgs() {
        assertNull(Iterables.firstNonNull(null, null));
    }

    @Test
    public void testLastNonNull_AllNull_TwoArgs() {
        assertNull(Iterables.lastNonNull(null, null));
    }

    @Test
    public void testFirstNonNull_AllNull_ThreeArgs() {
        assertNull(Iterables.firstNonNull(null, null, null));
    }

    @Test
    public void testLastNonNull_AllNull_ThreeArgs() {
        assertNull(Iterables.lastNonNull(null, null, null));
    }

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
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

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

}
