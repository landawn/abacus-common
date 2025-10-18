package com.landawn.abacus.util;

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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

@Tag("2025")
public class Iterables2025Test extends TestBase {

    private List<Integer> intList;
    private List<String> stringList;
    private List<Double> doubleList;
    private Set<Integer> intSet;
    private List<BigInteger> bigIntList;
    private List<BigDecimal> bigDecimalList;

    @BeforeEach
    public void setUp() {
        intList = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
        stringList = Arrays.asList("apple", "banana", "cherry", "date");
        doubleList = Arrays.asList(3.14, 2.71, 1.41, 1.73);
        intSet = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
        bigIntList = Arrays.asList(BigInteger.valueOf(100), BigInteger.valueOf(200), BigInteger.valueOf(300));
        bigDecimalList = Arrays.asList(BigDecimal.valueOf(10.5), BigDecimal.valueOf(20.5), BigDecimal.valueOf(30.5));
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
        assertEquals("found", Iterables.firstNonNullOrDefault(list, "default"));

        List<String> allNull = Arrays.asList(null, null, null);
        assertEquals("default", Iterables.firstNonNullOrDefault(allNull, "default"));

        assertEquals("default", Iterables.firstNonNullOrDefault((Iterable<String>) null, "default"));
        assertEquals("default", Iterables.firstNonNullOrDefault(new ArrayList<String>(), "default"));
    }

    @Test
    public void testFirstNonNullOrDefaultIterator() {
        List<String> list = Arrays.asList(null, null, "found", "second");
        assertEquals("found", Iterables.firstNonNullOrDefault(list.iterator(), "default"));

        List<String> allNull = Arrays.asList(null, null, null);
        assertEquals("default", Iterables.firstNonNullOrDefault(allNull.iterator(), "default"));

        assertEquals("default", Iterables.firstNonNullOrDefault((Iterator<String>) null, "default"));
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
        assertEquals("second", Iterables.lastNonNullOrDefault(list, "default"));

        List<String> allNull = Arrays.asList(null, null, null);
        assertEquals("default", Iterables.lastNonNullOrDefault(allNull, "default"));

        assertEquals("default", Iterables.lastNonNullOrDefault((Iterable<String>) null, "default"));
        assertEquals("default", Iterables.lastNonNullOrDefault(new ArrayList<String>(), "default"));
    }

    @Test
    public void testLastNonNullOrDefaultIterator() {
        List<String> list = Arrays.asList("first", "second", null, null);
        assertEquals("second", Iterables.lastNonNullOrDefault(list.iterator(), "default"));

        List<String> allNull = Arrays.asList(null, null, null);
        assertEquals("default", Iterables.lastNonNullOrDefault(allNull.iterator(), "default"));

        assertEquals("default", Iterables.lastNonNullOrDefault((Iterator<String>) null, "default"));
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
    public void testAverageIntIterable() {
        List<Integer> list = Arrays.asList(2, 4, 6, 8);
        OptionalDouble result = Iterables.averageInt(list);
        assertTrue(result.isPresent());
        assertEquals(5.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageInt((Iterable<Integer>) null).isPresent());
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
    public void testAverageLongIterable() {
        List<Long> list = Arrays.asList(2L, 4L, 6L, 8L);
        OptionalDouble result = Iterables.averageLong(list);
        assertTrue(result.isPresent());
        assertEquals(5.0, result.getAsDouble(), 0.001);

        assertFalse(Iterables.averageLong((Iterable<Long>) null).isPresent());
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

        Iterables.copy(src, dest);

        assertEquals("a", dest.get(0));
        assertEquals("b", dest.get(1));
        assertEquals("c", dest.get(2));
        assertEquals("w", dest.get(3));
    }

    @Test
    public void testCopyListsWithPositions() {
        List<String> src = Arrays.asList("a", "b", "c", "d", "e");
        List<String> dest = new ArrayList<>(Arrays.asList("1", "2", "3", "4", "5", "6"));

        Iterables.copy(src, 1, dest, 2, 3);

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
        List<Integer> reversed = Iterables.reverse(original);

        assertEquals(5, reversed.size());
        assertEquals(Integer.valueOf(5), reversed.get(0));
        assertEquals(Integer.valueOf(4), reversed.get(1));
        assertEquals(Integer.valueOf(3), reversed.get(2));
        assertEquals(Integer.valueOf(2), reversed.get(3));
        assertEquals(Integer.valueOf(1), reversed.get(4));

        List<Integer> mutableOriginal = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        List<Integer> mutableReversed = Iterables.reverse(mutableOriginal);
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
}
