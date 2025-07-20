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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
// Import other static methods from N as needed or call them via N.methodName
import com.landawn.abacus.util.function.IntPredicate;

public class N203Test extends TestBase {

    private static final IntPredicate IS_EVEN_INT = x -> x % 2 == 0;
    private static final IntPredicate IS_ODD_INT = x -> x % 2 != 0;
    private static final Predicate<Integer> IS_EVEN_INTEGER = x -> x % 2 == 0;
    private static final Predicate<String> STRING_NOT_EMPTY = s -> s != null && !s.isEmpty();
    private static final Predicate<String> STRING_HAS_LETTER_A = s -> s != null && s.contains("a");

    // Helper to get EMPTY_..._ARRAY constants if they are in CommonUtil
    // If N itself provides them directly or inherits, this might not be strictly necessary
    // but helps clarify where they come from.
    // For this test, we assume N has access to these, e.g. N.EMPTY_INT_ARRAY
    // If they are protected/private in CommonUtil and not exposed by N,
    // then we compare with new T[0] or equivalent.
    // The provided N.java shows N extends CommonUtil and CommonUtil seems to define these.

    //-------------------------------------------------------------------------
    // filter methods
    //-------------------------------------------------------------------------

    @Test
    public void testFilterBooleanArray() {
        assertArrayEquals(N.EMPTY_BOOLEAN_ARRAY, N.filter((boolean[]) null, b -> b));
        assertArrayEquals(N.EMPTY_BOOLEAN_ARRAY, N.filter(new boolean[0], b -> b));
        assertArrayEquals(new boolean[] { true, true }, N.filter(new boolean[] { true, false, true }, b -> b));
        assertArrayEquals(new boolean[] { false }, N.filter(new boolean[] { true, false, true }, b -> !b));
    }

    @Test
    public void testFilterBooleanArrayWithRange() {
        assertArrayEquals(N.EMPTY_BOOLEAN_ARRAY, N.filter((boolean[]) null, 0, 0, b -> b));
        assertArrayEquals(N.EMPTY_BOOLEAN_ARRAY, N.filter(new boolean[0], 0, 0, b -> b));

        boolean[] arr = { true, false, true, true, false };
        assertArrayEquals(new boolean[] { true, true }, N.filter(arr, 0, 3, b -> b));
        // Corrected predicate for the last case, for example:
        assertArrayEquals(new boolean[] { true, true }, N.filter(arr, 2, 4, b -> b));
        assertArrayEquals(new boolean[] { false }, N.filter(arr, 3, 5, b -> !b));

        assertArrayEquals(N.EMPTY_BOOLEAN_ARRAY, N.filter(arr, 1, 1, b -> b));

        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(arr, -1, 2, b -> b));
        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(arr, 0, 6, b -> b));
        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(arr, 3, 1, b -> b)); // checkFromToIndex in CommonUtil
    }

    @Test
    public void testFilterCharArray() {
        assertArrayEquals(N.EMPTY_CHAR_ARRAY, N.filter((char[]) null, c -> c == 'a'));
        assertArrayEquals(N.EMPTY_CHAR_ARRAY, N.filter(new char[0], c -> c == 'a'));
        assertArrayEquals(new char[] { 'a', 'a' }, N.filter(new char[] { 'a', 'b', 'a' }, c -> c == 'a'));
        assertArrayEquals(new char[] { 'b' }, N.filter(new char[] { 'a', 'b', 'a' }, c -> c == 'b'));
    }

    @Test
    public void testFilterCharArrayWithRange() {
        char[] arr = { 'x', 'y', 'x', 'z', 'x' };
        assertArrayEquals(new char[] { 'x' }, N.filter(arr, 0, 2, c -> c == 'x'));
        assertArrayEquals(new char[] { 'z', 'x' }, N.filter(arr, 3, 5, c -> c == 'z' || c == 'x'));
        assertArrayEquals(N.EMPTY_CHAR_ARRAY, N.filter(arr, 1, 1, c -> c == 'y'));
    }

    @Test
    public void testFilterByteArray() {
        assertArrayEquals(N.EMPTY_BYTE_ARRAY, N.filter((byte[]) null, b -> b > 0));
        assertArrayEquals(N.EMPTY_BYTE_ARRAY, N.filter(new byte[0], b -> b > 0));
        assertArrayEquals(new byte[] { 1, 3 }, N.filter(new byte[] { 1, -2, 3 }, b -> b > 0));
    }

    @Test
    public void testFilterByteArrayWithRange() {
        byte[] arr = { 10, 20, -5, 30, -15 };
        assertArrayEquals(new byte[] { 10, 20 }, N.filter(arr, 0, 2, b -> b > 0));
        assertArrayEquals(new byte[] { 30 }, N.filter(arr, 2, 4, b -> b > 0));
        assertArrayEquals(N.EMPTY_BYTE_ARRAY, N.filter(arr, 1, 1, b -> b > 0));
    }

    @Test
    public void testFilterShortArray() {
        assertArrayEquals(N.EMPTY_SHORT_ARRAY, N.filter((short[]) null, s -> s > 0));
        assertArrayEquals(N.EMPTY_SHORT_ARRAY, N.filter(new short[0], s -> s > 0));
        assertArrayEquals(new short[] { 100, 300 }, N.filter(new short[] { 100, -200, 300 }, s -> s > 0));
    }

    @Test
    public void testFilterShortArrayWithRange() {
        short[] arr = { 1, 2, 0, 3, -1 };
        assertArrayEquals(new short[] { 1, 2 }, N.filter(arr, 0, 2, s -> s > 0));
        assertArrayEquals(new short[] { 3 }, N.filter(arr, 2, 4, s -> s > 0));
        assertArrayEquals(N.EMPTY_SHORT_ARRAY, N.filter(arr, 1, 1, s -> s > 0));
    }

    @Test
    public void testFilterIntArray() {
        assertArrayEquals(N.EMPTY_INT_ARRAY, N.filter((int[]) null, IS_EVEN_INT));
        assertArrayEquals(N.EMPTY_INT_ARRAY, N.filter(new int[0], IS_EVEN_INT));
        assertArrayEquals(new int[] { 2, 4 }, N.filter(new int[] { 1, 2, 3, 4, 5 }, IS_EVEN_INT));
        assertArrayEquals(new int[] { 1, 3, 5 }, N.filter(new int[] { 1, 2, 3, 4, 5 }, IS_ODD_INT));
    }

    @Test
    public void testFilterIntArrayWithRange() {
        int[] arr = { 1, 2, 3, 4, 5, 6 };
        assertArrayEquals(new int[] { 2, 4 }, N.filter(arr, 0, 4, IS_EVEN_INT));
        assertArrayEquals(new int[] { 5 }, N.filter(arr, 3, 5, IS_ODD_INT));
        assertArrayEquals(new int[] { 6 }, N.filter(arr, 5, 6, IS_EVEN_INT));
        assertArrayEquals(N.EMPTY_INT_ARRAY, N.filter(arr, 1, 1, IS_EVEN_INT));

        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(arr, -1, 2, IS_EVEN_INT));
        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(arr, 0, 7, IS_EVEN_INT));
        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(arr, 3, 1, IS_EVEN_INT));
    }

    @Test
    public void testFilterLongArray() {
        assertArrayEquals(N.EMPTY_LONG_ARRAY, N.filter((long[]) null, l -> l > 0));
        assertArrayEquals(N.EMPTY_LONG_ARRAY, N.filter(new long[0], l -> l > 0));
        assertArrayEquals(new long[] { 1L, 3L }, N.filter(new long[] { 1L, -2L, 3L }, l -> l > 0));
    }

    @Test
    public void testFilterLongArrayWithRange() {
        long[] arr = { 10L, 20L, -5L, 30L, -15L };
        assertArrayEquals(new long[] { 10L, 20L }, N.filter(arr, 0, 2, l -> l > 0));
        assertArrayEquals(new long[] { 30L }, N.filter(arr, 2, 4, l -> l > 0));
        assertArrayEquals(N.EMPTY_LONG_ARRAY, N.filter(arr, 1, 1, l -> l > 0));
    }

    @Test
    public void testFilterFloatArray() {
        assertArrayEquals(N.EMPTY_FLOAT_ARRAY, N.filter((float[]) null, f -> f > 0));
        assertArrayEquals(N.EMPTY_FLOAT_ARRAY, N.filter(new float[0], f -> f > 0));
        assertArrayEquals(new float[] { 1.0f, 3.0f }, N.filter(new float[] { 1.0f, -2.0f, 3.0f }, f -> f > 0), 0.001f);
    }

    @Test
    public void testFilterFloatArrayWithRange() {
        float[] arr = { 1.f, 2.f, 0.f, 3.f, -1.f };
        assertArrayEquals(new float[] { 1.f, 2.f }, N.filter(arr, 0, 2, f -> f > 0));
        assertArrayEquals(new float[] { 3.f }, N.filter(arr, 2, 4, f -> f > 0));
        assertArrayEquals(N.EMPTY_FLOAT_ARRAY, N.filter(arr, 1, 1, f -> f > 0));
    }

    @Test
    public void testFilterDoubleArray() {
        assertArrayEquals(N.EMPTY_DOUBLE_ARRAY, N.filter((double[]) null, d -> d > 0));
        assertArrayEquals(N.EMPTY_DOUBLE_ARRAY, N.filter(new double[0], d -> d > 0));
        assertArrayEquals(new double[] { 1.0, 3.0 }, N.filter(new double[] { 1.0, -2.0, 3.0 }, d -> d > 0), 0.001);
    }

    @Test
    public void testFilterDoubleArrayWithRange() {
        double[] arr = { 1., 2., 0., 3., -1. };
        assertArrayEquals(new double[] { 1., 2. }, N.filter(arr, 0, 2, d -> d > 0));
        assertArrayEquals(new double[] { 3. }, N.filter(arr, 2, 4, d -> d > 0));
        assertArrayEquals(N.EMPTY_DOUBLE_ARRAY, N.filter(arr, 1, 1, d -> d > 0));
    }

    @Test
    public void testFilterGenericArray() {
        String[] arr = { "apple", "banana", "avocado", "grape" };
        assertEquals(List.of("apple", "avocado"), N.filter(arr, s -> s.startsWith("a")));
        assertEquals(Collections.emptyList(), N.filter((String[]) null, STRING_NOT_EMPTY));
        assertEquals(Collections.emptyList(), N.filter(new String[0], STRING_NOT_EMPTY));
    }

    @Test
    public void testFilterGenericArrayWithSupplier() {
        String[] arr = { "one", "two", "three", "four" };
        Set<String> result = N.filter(arr, s -> s.length() == 3, size -> new HashSet<>());
        assertEquals(Set.of("one", "two"), result);

        List<String> listResult = N.filter(arr, s -> s.length() > 3, IntFunctions.ofList());
        assertEquals(List.of("three", "four"), listResult);

        assertTrue(N.filter((String[]) null, STRING_NOT_EMPTY, IntFunctions.ofList()).isEmpty());
        assertTrue(N.filter(new String[0], STRING_NOT_EMPTY, IntFunctions.ofSet()).isEmpty());

    }

    @Test
    public void testFilterGenericArrayWithRange() {
        Integer[] arr = { 1, 2, 3, 4, 5, 6 };
        assertEquals(List.of(2, 4), N.filter(arr, 0, 4, IS_EVEN_INTEGER));
        assertEquals(List.of(5), N.filter(arr, 3, 5, x -> x % 2 != 0));
        assertTrue(N.filter(arr, 1, 1, IS_EVEN_INTEGER).isEmpty());
    }

    @Test
    public void testFilterGenericArrayWithRangeAndSupplier() {
        Integer[] arr = { 1, 2, 3, 4, 5, 6 };
        Set<Integer> resultSet = N.filter(arr, 0, 4, IS_EVEN_INTEGER, size -> new HashSet<>());
        assertEquals(Set.of(2, 4), resultSet);

        List<Integer> resultList = N.filter(arr, 3, 5, x -> x % 2 != 0, IntFunctions.ofList());
        assertEquals(List.of(5), resultList);

        assertTrue(N.filter(arr, 1, 1, IS_EVEN_INTEGER, IntFunctions.ofList()).isEmpty());
    }

    @Test
    public void testFilterCollectionWithRange() {
        List<String> list = Arrays.asList("a", "b", "aa", "bb", "aaa");
        assertEquals(List.of("aa", "aaa"), N.filter(list, 1, 5, s -> s.contains("a") && s.length() > 1));
        assertTrue(N.filter(list, 1, 1, STRING_NOT_EMPTY).isEmpty());
        assertEquals(List.of("b", "bb"), N.filter(list, 1, 4, s -> s.startsWith("b")));
    }

    @Test
    public void testFilterIterable() {
        Iterable<String> iter = Arrays.asList("apple", "banana", "", "grape");
        assertEquals(List.of("apple", "banana", "grape"), N.filter(iter, STRING_NOT_EMPTY));
        assertTrue(N.filter((Iterable<String>) null, STRING_NOT_EMPTY).isEmpty());
        assertTrue(N.filter(Collections.emptyList(), STRING_NOT_EMPTY).isEmpty());
    }

    @Test
    public void testFilterIterableWithSupplier() {
        Iterable<String> iter = Arrays.asList("apple", "banana", "apricot", "grape");
        Set<String> resultSet = N.filter(iter, s -> s.startsWith("a"), HashSet::new);
        assertEquals(Set.of("apple", "apricot"), resultSet);
    }

    @Test
    public void testFilterIterator() {
        Iterator<String> iter = Arrays.asList("one", "two", "", "three").iterator();
        assertEquals(List.of("one", "two", "three"), N.filter(iter, STRING_NOT_EMPTY));
        assertTrue(N.filter((Iterator<String>) null, STRING_NOT_EMPTY).isEmpty());
        assertTrue(N.filter(Collections.emptyIterator(), STRING_NOT_EMPTY).isEmpty());
    }

    @Test
    public void testFilterIteratorWithSupplier() {
        Iterator<String> iter = Arrays.asList("one", "two", "three", "onetwo").iterator();
        Set<String> resultSet = N.filter(iter, s -> s.length() == 3, HashSet::new);
        assertEquals(Set.of("one", "two"), resultSet);
    }

    //-------------------------------------------------------------------------
    // mapToPrimitive methods
    //-------------------------------------------------------------------------

    @Test
    public void testMapToBooleanArray() {
        String[] arr = { "true", "false", "TRUE" };
        assertArrayEquals(new boolean[] { true, false, true }, N.mapToBoolean(arr, Boolean::parseBoolean));
        assertArrayEquals(N.EMPTY_BOOLEAN_ARRAY, N.mapToBoolean((String[]) null, Boolean::parseBoolean));
        assertArrayEquals(N.EMPTY_BOOLEAN_ARRAY, N.mapToBoolean(new String[0], Boolean::parseBoolean));
    }

    @Test
    public void testMapToBooleanArrayWithRange() {
        String[] arr = { "true", "false", "TRUE", "yes" };
        assertArrayEquals(new boolean[] { false, true }, N.mapToBoolean(arr, 1, 3, Boolean::parseBoolean));
        assertArrayEquals(N.EMPTY_BOOLEAN_ARRAY, N.mapToBoolean(arr, 1, 1, Boolean::parseBoolean));
    }

    @Test
    public void testMapToBooleanCollection() {
        List<String> list = Arrays.asList("true", "false", "TRUE");
        assertArrayEquals(new boolean[] { true, false, true }, N.mapToBoolean(list, Boolean::parseBoolean));
        assertArrayEquals(N.EMPTY_BOOLEAN_ARRAY, N.mapToBoolean((Collection<String>) null, Boolean::parseBoolean));
        assertArrayEquals(N.EMPTY_BOOLEAN_ARRAY, N.mapToBoolean(Collections.emptyList(), Boolean::parseBoolean));
    }

    @Test
    public void testMapToBooleanCollectionWithRange() {
        List<String> list = Arrays.asList("true", "false", "TRUE", "no");
        assertArrayEquals(new boolean[] { false, true }, N.mapToBoolean(list, 1, 3, Boolean::parseBoolean));
        assertArrayEquals(N.EMPTY_BOOLEAN_ARRAY, N.mapToBoolean(list, 0, 0, Boolean::parseBoolean));

        // Test with non-RandomAccess collection
        Collection<String> nonRaList = new LinkedList<>(list);
        assertArrayEquals(new boolean[] { false, true }, N.mapToBoolean(nonRaList, 1, 3, Boolean::parseBoolean));
    }

    @Test
    public void testMapToCharArray() {
        String[] arr = { "apple", "cat" };
        assertArrayEquals(new char[] { 'a', 'c' }, N.mapToChar(arr, s -> s.charAt(0)));
    }

    @Test
    public void testMapToCharArrayWithRange() {
        String[] arr = { "apple", "banana", "cat" };
        assertArrayEquals(new char[] { 'b', 'c' }, N.mapToChar(arr, 1, 3, s -> s.charAt(0)));
    }

    @Test
    public void testMapToCharCollection() {
        List<String> list = Arrays.asList("hello", "world");
        assertArrayEquals(new char[] { 'h', 'w' }, N.mapToChar(list, s -> s.charAt(0)));
    }

    @Test
    public void testMapToCharCollectionWithRange() {
        List<String> list = Arrays.asList("one", "two", "three");
        assertArrayEquals(new char[] { 't', 't' }, N.mapToChar(list, 1, 3, s -> s.charAt(0)));
    }

    @Test
    public void testMapToByteArray() {
        Integer[] arr = { 10, 20, -10 };
        assertArrayEquals(new byte[] { 10, 20, -10 }, N.mapToByte(arr, Integer::byteValue));
    }

    @Test
    public void testMapToByteArrayWithRange() {
        Integer[] arr = { 10, 20, 30, 40 };
        assertArrayEquals(new byte[] { 20, 30 }, N.mapToByte(arr, 1, 3, Integer::byteValue));
    }

    @Test
    public void testMapToByteCollection() {
        List<Integer> list = Arrays.asList(5, 15, 25);
        assertArrayEquals(new byte[] { 5, 15, 25 }, N.mapToByte(list, Integer::byteValue));
    }

    @Test
    public void testMapToByteCollectionWithRange() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        assertArrayEquals(new byte[] { 2, 3, 4 }, N.mapToByte(list, 1, 4, Integer::byteValue));
    }

    @Test
    public void testMapToShortArray() {
        Integer[] arr = { 100, 200, 300 };
        assertArrayEquals(new short[] { 100, 200, 300 }, N.mapToShort(arr, Integer::shortValue));
    }

    @Test
    public void testMapToShortArrayWithRange() {
        Integer[] arr = { 100, 200, 300, 400 };
        assertArrayEquals(new short[] { 200, 300 }, N.mapToShort(arr, 1, 3, Integer::shortValue));
    }

    @Test
    public void testMapToShortCollection() {
        List<Integer> list = Arrays.asList(10, 20, 30);
        assertArrayEquals(new short[] { 10, 20, 30 }, N.mapToShort(list, Integer::shortValue));
    }

    @Test
    public void testMapToShortCollectionWithRange() {
        List<Integer> list = Arrays.asList(10, 20, 30, 40, 50);
        assertArrayEquals(new short[] { 20, 30, 40 }, N.mapToShort(list, 1, 4, Integer::shortValue));
    }

    @Test
    public void testMapToIntArray() {
        String[] arr = { "1", "22", "333" };
        assertArrayEquals(new int[] { 1, 2, 3 }, N.mapToInt(arr, String::length));
        assertArrayEquals(N.EMPTY_INT_ARRAY, N.mapToInt((String[]) null, String::length));
        assertArrayEquals(N.EMPTY_INT_ARRAY, N.mapToInt(new String[0], String::length));
    }

    @Test
    public void testMapToIntArrayWithRange() {
        String[] arr = { "a", "bb", "ccc", "dddd" };
        assertArrayEquals(new int[] { 2, 3 }, N.mapToInt(arr, 1, 3, String::length));
        assertArrayEquals(N.EMPTY_INT_ARRAY, N.mapToInt(arr, 2, 2, String::length));
    }

    @Test
    public void testMapToIntCollection() {
        List<String> list = Arrays.asList("one", "two", "three");
        assertArrayEquals(new int[] { 3, 3, 5 }, N.mapToInt(list, String::length));
        assertArrayEquals(N.EMPTY_INT_ARRAY, N.mapToInt((Collection<String>) null, String::length));
        assertArrayEquals(N.EMPTY_INT_ARRAY, N.mapToInt(Collections.emptyList(), String::length));
    }

    @Test
    public void testMapToIntCollectionWithRange() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date");
        assertArrayEquals(new int[] { 6, 6 }, N.mapToInt(list, 1, 3, String::length));
        assertArrayEquals(N.EMPTY_INT_ARRAY, N.mapToInt(list, 0, 0, String::length));

        Collection<String> nonRaList = new LinkedList<>(list);
        assertArrayEquals(new int[] { 6, 6 }, N.mapToInt(nonRaList, 1, 3, String::length));
    }

    @Test
    public void testMapToIntFromLongArray() {
        long[] arr = { 1L, 10000000000L, 3L }; // Second value will overflow int
        assertArrayEquals(new int[] { 1, (int) 10000000000L, 3 }, N.mapToInt(arr, l -> (int) l));
        assertArrayEquals(N.EMPTY_INT_ARRAY, N.mapToInt((long[]) null, l -> (int) l));
    }

    @Test
    public void testMapToIntFromDoubleArray() {
        double[] arr = { 1.1, 2.9, 3.5 };
        assertArrayEquals(new int[] { 1, 2, 3 }, N.mapToInt(arr, d -> (int) d));
        assertArrayEquals(N.EMPTY_INT_ARRAY, N.mapToInt((double[]) null, d -> (int) d));
    }

    @Test
    public void testMapToLongArray() {
        String[] arr = { "1", "22", "333" };
        assertArrayEquals(new long[] { 1L, 2L, 3L }, N.mapToLong(arr, s -> (long) s.length()));
    }

    @Test
    public void testMapToLongArrayWithRange() {
        String[] arr = { "a", "bb", "ccc", "dddd" };
        assertArrayEquals(new long[] { 2L, 3L }, N.mapToLong(arr, 1, 3, s -> (long) s.length()));
    }

    @Test
    public void testMapToLongCollection() {
        List<String> list = Arrays.asList("one", "two", "three");
        assertArrayEquals(new long[] { 3L, 3L, 5L }, N.mapToLong(list, s -> (long) s.length()));
    }

    @Test
    public void testMapToLongCollectionWithRange() {
        List<String> list = Arrays.asList("apple", "banana", "cherry");
        assertArrayEquals(new long[] { 6L }, N.mapToLong(list, 1, 2, s -> (long) s.length()));
    }

    @Test
    public void testMapToLongFromIntArray() {
        int[] arr = { 1, 2, 3 };
        assertArrayEquals(new long[] { 1L, 2L, 3L }, N.mapToLong(arr, i -> (long) i));
        assertArrayEquals(N.EMPTY_LONG_ARRAY, N.mapToLong((int[]) null, i -> (long) i));
    }

    @Test
    public void testMapToLongFromDoubleArray() {
        double[] arr = { 1.1, 2.9, 3.5 };
        assertArrayEquals(new long[] { 1L, 2L, 3L }, N.mapToLong(arr, d -> (long) d));
        assertArrayEquals(N.EMPTY_LONG_ARRAY, N.mapToLong((double[]) null, d -> (long) d));
    }

    @Test
    public void testMapToFloatArray() {
        String[] arr = { "1.1", "2.2", "3.3" };
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f }, N.mapToFloat(arr, Float::parseFloat), 0.001f);
    }

    @Test
    public void testMapToFloatArrayWithRange() {
        String[] arr = { "1.0", "2.5", "3.0", "4.5" };
        assertArrayEquals(new float[] { 2.5f, 3.0f }, N.mapToFloat(arr, 1, 3, Float::parseFloat), 0.001f);
    }

    @Test
    public void testMapToFloatCollection() {
        List<String> list = Arrays.asList("0.5", "1.5");
        assertArrayEquals(new float[] { 0.5f, 1.5f }, N.mapToFloat(list, Float::parseFloat), 0.001f);
    }

    @Test
    public void testMapToFloatCollectionWithRange() {
        List<String> list = Arrays.asList("1.0", "2.5", "3.0", "4.5");
        assertArrayEquals(new float[] { 2.5f, 3.0f }, N.mapToFloat(list, 1, 3, Float::parseFloat), 0.001f);
    }

    @Test
    public void testMapToDoubleArray() {
        String[] arr = { "1.1", "2.2", "3.3" };
        assertArrayEquals(new double[] { 1.1, 2.2, 3.3 }, N.mapToDouble(arr, Double::parseDouble), 0.001);
    }

    @Test
    public void testMapToDoubleArrayWithRange() {
        String[] arr = { "1.0", "2.5", "3.0", "4.5" };
        assertArrayEquals(new double[] { 2.5, 3.0 }, N.mapToDouble(arr, 1, 3, Double::parseDouble), 0.001);
    }

    @Test
    public void testMapToDoubleCollection() {
        List<String> list = Arrays.asList("0.5", "1.5");
        assertArrayEquals(new double[] { 0.5, 1.5 }, N.mapToDouble(list, Double::parseDouble), 0.001);
    }

    @Test
    public void testMapToDoubleCollectionWithRange() {
        List<String> list = Arrays.asList("1.0", "2.5", "3.0", "4.5");
        assertArrayEquals(new double[] { 2.5, 3.0 }, N.mapToDouble(list, 1, 3, Double::parseDouble), 0.001);
    }

    @Test
    public void testMapToDoubleFromIntArray() {
        int[] arr = { 1, 2, 3 };
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, N.mapToDouble(arr, i -> (double) i), 0.001);
        assertArrayEquals(N.EMPTY_DOUBLE_ARRAY, N.mapToDouble((int[]) null, i -> (double) i));
    }

    @Test
    public void testMapToDoubleFromLongArray() {
        long[] arr = { 1L, 2L, 3L };
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, N.mapToDouble(arr, l -> (double) l), 0.001);
        assertArrayEquals(N.EMPTY_DOUBLE_ARRAY, N.mapToDouble((long[]) null, l -> (double) l));
    }

    //-------------------------------------------------------------------------
    // map (Object to Object) methods
    //-------------------------------------------------------------------------

    @Test
    public void testMapArray() {
        String[] arr = { "a", "bb", "ccc" };
        List<Integer> expected = Arrays.asList(1, 2, 3);
        assertEquals(expected, N.map(arr, String::length));
        assertTrue(N.map((String[]) null, String::length).isEmpty());
        assertTrue(N.map(new String[0], String::length).isEmpty());
    }

    @Test
    public void testMapArrayWithSupplier() {
        String[] arr = { "a", "bb", "a" };
        Set<Integer> expected = Set.of(1, 2);
        assertEquals(expected, N.map(arr, String::length, size -> new HashSet<>()));
        assertTrue(N.map((String[]) null, String::length, IntFunctions.ofList()).isEmpty());
    }

    @Test
    public void testMapArrayWithRange() {
        String[] arr = { "one", "two", "three", "four" };
        List<Integer> expected = Arrays.asList(3, 5); // length of "two", "three"
        assertEquals(expected, N.map(arr, 1, 3, String::length));
        assertTrue(N.map(arr, 1, 1, String::length).isEmpty());
    }

    @Test
    public void testMapArrayWithRangeAndSupplier() {
        String[] arr = { "one", "two", "three", "two" };
        Set<Integer> expected = Set.of(3, 5);
        assertEquals(expected, N.map(arr, 1, 4, String::length, HashSet::new));
    }

    @Test
    public void testMapCollectionWithRange() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date");
        assertEquals(List.of("BANANA", "CHERRY"), N.map(list, 1, 3, String::toUpperCase));
        assertTrue(N.map(list, 0, 0, String::toUpperCase).isEmpty());
    }

    @Test
    public void testMapCollectionWithRangeAndSupplier() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "banana");
        Set<String> expected = Set.of("BANANA", "CHERRY");
        assertEquals(expected, N.map(list, 1, 4, String::toUpperCase, HashSet::new));
    }

    @Test
    public void testMapIterable() {
        Iterable<Integer> iter = Arrays.asList(1, 2, 3);
        assertEquals(Arrays.asList("1", "2", "3"), N.map(iter, String::valueOf));
        assertTrue(N.map((Iterable<Integer>) null, String::valueOf).isEmpty());
    }

    @Test
    public void testMapIterableWithSupplier() {
        Iterable<Integer> iter = Arrays.asList(1, 2, 1, 3);
        assertEquals(Set.of("1", "2", "3"), N.map(iter, String::valueOf, HashSet::new));
    }

    @Test
    public void testMapIterator() {
        Iterator<Integer> iter = Arrays.asList(1, 2, 3).iterator();
        assertEquals(Arrays.asList("1", "2", "3"), N.map(iter, String::valueOf));
        assertTrue(N.map((Iterator<Integer>) null, String::valueOf).isEmpty());
    }

    @Test
    public void testMapIteratorWithSupplier() {
        Iterator<Integer> iter = Arrays.asList(1, 2, 1, 3).iterator();
        assertEquals(Set.of("1", "2", "3"), N.map(iter, String::valueOf, HashSet::new));
    }

    //-------------------------------------------------------------------------
    // flatMap methods
    //-------------------------------------------------------------------------

    private static Collection<String> splitToChars(String s) {
        if (s == null)
            return Collections.emptyList();
        return s.chars().mapToObj(c -> String.valueOf((char) c)).collect(Collectors.toList());
    }

    @Test
    public void testFlatMapArray() {
        String[] arr = { "ab", "c" };
        List<String> expected = Arrays.asList("a", "b", "c");
        assertEquals(expected, N.flatMap(arr, N203Test::splitToChars));
        assertTrue(N.flatMap((String[]) null, N203Test::splitToChars).isEmpty());
    }

    @Test
    public void testFlatMapArrayWithSupplier() {
        String[] arr = { "ab", "ca" };
        // Supplier for Set to test distinct flatmapped elements
        Set<String> expected = Set.of("a", "b", "c");
        assertEquals(expected, N.flatMap(arr, N203Test::splitToChars, IntFunctions.ofSet()));
    }

    @Test
    public void testFlatMapArrayWithRange() {
        String[] arr = { "hi", "world", "test" };
        List<String> expected = List.of('w', 'o', 'r', 'l', 'd').stream().map(String::valueOf).collect(Collectors.toList());
        assertEquals(expected, N.flatMap(arr, 1, 2, N203Test::splitToChars)); // Only "world"
    }

    @Test
    public void testFlatMapArrayWithRangeAndSupplier() {
        String[] arr = { "hi", "bob", "test", "bib" };
        Set<String> expected = Set.of("b", "o");
        assertEquals(expected, N.flatMap(arr, 1, 2, N203Test::splitToChars, HashSet::new)); // "bob"
    }

    @Test
    public void testFlatMapCollectionWithRange() {
        List<String> list = Arrays.asList("key", "lock", "door");
        List<String> expected = List.of('l', 'o', 'c', 'k', 'd', 'o', 'o', 'r').stream().map(String::valueOf).collect(Collectors.toList());
        assertEquals(expected, N.flatMap(list, 1, 3, N203Test::splitToChars));
    }

    @Test
    public void testFlatMapCollectionWithRangeAndSupplier() {
        List<String> list = Arrays.asList("key", "lock", "lol");
        Set<String> expected = Set.of("l", "o", "c", "k");
        assertEquals(expected, N.flatMap(list, 1, 3, N203Test::splitToChars, HashSet::new));
    }

    @Test
    public void testFlatMapIterable() {
        Iterable<String> iter = Arrays.asList("one", "two");
        assertEquals(List.of("o", "n", "e", "t", "w", "o"), N.flatMap(iter, N203Test::splitToChars));
    }

    @Test
    public void testFlatMapIterableWithSupplier() {
        Iterable<String> iter = Arrays.asList("one", "too");
        assertEquals(Set.of("o", "n", "e", "t"), N.flatMap(iter, N203Test::splitToChars, IntFunctions.ofSet()));
    }

    @Test
    public void testFlatMapIterator() {
        Iterator<String> iter = Arrays.asList("hi", "bye").iterator();
        assertEquals(List.of("h", "i", "b", "y", "e"), N.flatMap(iter, N203Test::splitToChars));
    }

    @Test
    public void testFlatMapIteratorWithSupplier() {
        Iterator<String> iter = Arrays.asList("hi", "bib").iterator();
        assertEquals(Set.of("h", "i", "b"), N.flatMap(iter, N203Test::splitToChars, IntFunctions.ofSet()));
    }

    @Test
    public void testFlatMapArrayTwoLevels() {
        String[] arr = { "ab-cd", "ef" };
        Function<String, Collection<String>> splitByDash = s -> Arrays.asList(s.split("-"));
        // "ab-cd" -> ["ab", "cd"] -> flatMap(splitToChars) -> ["a","b","c","d"]
        // "ef"    -> ["ef"]       -> flatMap(splitToChars) -> ["e","f"]
        List<String> expected = Arrays.asList("a", "b", "c", "d", "e", "f");
        assertEquals(expected, N.flatMap(arr, splitByDash, N203Test::splitToChars));
    }

    @Test
    public void testFlatMapArrayTwoLevelsWithSupplier() {
        String[] arr = { "ab-ca", "ef-fa" };
        Function<String, Collection<String>> splitByDash = s -> Arrays.asList(s.split("-"));
        Set<String> expected = Set.of("a", "b", "c", "e", "f");
        assertEquals(expected, N.flatMap(arr, splitByDash, N203Test::splitToChars, HashSet::new));
    }

    @Test
    public void testFlatMapIterableTwoLevels() {
        List<String> list = Arrays.asList("uv-wx", "yz");
        Function<String, Collection<String>> splitByDash = s -> Arrays.asList(s.split("-"));
        List<String> expected = Arrays.asList("u", "v", "w", "x", "y", "z");
        assertEquals(expected, N.flatMap(list, splitByDash, N203Test::splitToChars));
    }

    @Test
    public void testFlatMapIterableTwoLevelsWithSupplier() {
        List<String> list = Arrays.asList("uv-wa", "yz-za");
        Function<String, Collection<String>> splitByDash = s -> Arrays.asList(s.split("-"));
        Set<String> expected = Set.of("u", "v", "w", "a", "y", "z");
        assertEquals(expected, N.flatMap(list, splitByDash, N203Test::splitToChars, HashSet::new));
    }

    @Test
    public void testFlatMapIteratorTwoLevels() {
        Iterator<String> iter = Arrays.asList("12-34", "56").iterator();
        Function<String, Collection<String>> splitByDash = s -> Arrays.asList(s.split("-"));
        List<String> expected = Arrays.asList("1", "2", "3", "4", "5", "6");
        assertEquals(expected, N.flatMap(iter, splitByDash, N203Test::splitToChars));
    }

    @Test
    public void testFlatMapIteratorTwoLevelsWithSupplier() {
        Iterator<String> iter = Arrays.asList("12-31", "56-65").iterator();
        Function<String, Collection<String>> splitByDash = s -> Arrays.asList(s.split("-"));
        Set<String> expected = Set.of("1", "2", "3", "5", "6");
        assertEquals(expected, N.flatMap(iter, splitByDash, N203Test::splitToChars, HashSet::new));
    }

    //-------------------------------------------------------------------------
    // takeWhile/dropWhile/skipUntil methods
    //-------------------------------------------------------------------------

    @Test
    public void testTakeWhileArray() {
        Integer[] arr = { 2, 4, 5, 6, 8 };
        assertEquals(List.of(2, 4), N.takeWhile(arr, IS_EVEN_INTEGER));
        assertTrue(N.takeWhile((Integer[]) null, IS_EVEN_INTEGER).isEmpty());
        assertTrue(N.takeWhile(new Integer[0], IS_EVEN_INTEGER).isEmpty());
        assertEquals(List.of(1, 3), N.takeWhile(new Integer[] { 1, 3, 2, 5 }, x -> x % 2 != 0));
        assertTrue(N.takeWhile(new Integer[] { 1, 2, 3 }, x -> x > 10).isEmpty());
    }

    @Test
    public void testTakeWhileIterable() {
        List<Integer> list = Arrays.asList(2, 4, 5, 6, 8);
        assertEquals(List.of(2, 4), N.takeWhile(list, IS_EVEN_INTEGER));
        assertTrue(N.takeWhile((Iterable<Integer>) null, IS_EVEN_INTEGER).isEmpty());
    }

    @Test
    public void testTakeWhileIterator() {
        Iterator<Integer> iter = Arrays.asList(2, 4, 5, 6, 8).iterator();
        assertEquals(List.of(2, 4), N.takeWhile(iter, IS_EVEN_INTEGER));
        assertTrue(N.takeWhile((Iterator<Integer>) null, IS_EVEN_INTEGER).isEmpty());
    }

    @Test
    public void testTakeWhileInclusiveArray() {
        Integer[] arr = { 2, 4, 5, 6, 8 };
        assertEquals(List.of(2, 4, 5), N.takeWhileInclusive(arr, IS_EVEN_INTEGER));
        assertEquals(List.of(1, 3, 2), N.takeWhileInclusive(new Integer[] { 1, 3, 2, 5 }, x -> x % 2 != 0));
        assertEquals(List.of(10), N.takeWhileInclusive(new Integer[] { 10, 1, 2 }, x -> x < 5)); // takes 10, !filter.test(10) is true.
        assertTrue(N.takeWhileInclusive((Integer[]) null, IS_EVEN_INTEGER).isEmpty());
    }

    @Test
    public void testTakeWhileInclusiveIterable() {
        List<Integer> list = Arrays.asList(2, 4, 5, 6, 8);
        assertEquals(List.of(2, 4, 5), N.takeWhileInclusive(list, IS_EVEN_INTEGER));
    }

    @Test
    public void testTakeWhileInclusiveIterator() {
        Iterator<Integer> iter = Arrays.asList(2, 4, 5, 6, 8).iterator();
        assertEquals(List.of(2, 4, 5), N.takeWhileInclusive(iter, IS_EVEN_INTEGER));
    }

    @Test
    public void testDropWhileArray() {
        Integer[] arr = { 2, 4, 5, 6, 8 };
        assertEquals(List.of(5, 6, 8), N.dropWhile(arr, IS_EVEN_INTEGER));
        assertTrue(N.dropWhile((Integer[]) null, IS_EVEN_INTEGER).isEmpty());
        assertEquals(List.of(2, 5), N.dropWhile(new Integer[] { 1, 3, 2, 5 }, x -> x % 2 != 0));
        assertEquals(List.of(1, 2, 3), N.dropWhile(new Integer[] { 1, 2, 3 }, x -> x > 10));
    }

    @Test
    public void testDropWhileIterable() {
        List<Integer> list = Arrays.asList(2, 4, 5, 6, 8);
        assertEquals(List.of(5, 6, 8), N.dropWhile(list, IS_EVEN_INTEGER));
        assertTrue(N.dropWhile((Iterable<Integer>) null, IS_EVEN_INTEGER).isEmpty());
    }

    @Test
    public void testDropWhileIterator() {
        Iterator<Integer> iter = Arrays.asList(2, 4, 5, 6, 8).iterator();
        assertEquals(List.of(5, 6, 8), N.dropWhile(iter, IS_EVEN_INTEGER));
        assertTrue(N.dropWhile((Iterator<Integer>) null, IS_EVEN_INTEGER).isEmpty());
    }

    @Test
    public void testSkipUntilArray() {
        Integer[] arr = { 1, 3, 4, 5, 6 };
        assertEquals(List.of(4, 5, 6), N.skipUntil(arr, IS_EVEN_INTEGER)); // Skips 1, 3, starts at 4
        assertTrue(N.skipUntil((Integer[]) null, IS_EVEN_INTEGER).isEmpty());
        assertEquals(List.of(2, 5), N.skipUntil(new Integer[] { 1, 3, 2, 5 }, IS_EVEN_INTEGER));
        assertTrue(N.skipUntil(new Integer[] { 1, 3, 5 }, IS_EVEN_INTEGER).isEmpty());
    }

    @Test
    public void testSkipUntilIterable() {
        List<Integer> list = Arrays.asList(1, 3, 4, 5, 6);
        assertEquals(List.of(4, 5, 6), N.skipUntil(list, IS_EVEN_INTEGER));
    }

    @Test
    public void testSkipUntilIterator() {
        Iterator<Integer> iter = Arrays.asList(1, 3, 4, 5, 6).iterator();
        assertEquals(List.of(4, 5, 6), N.skipUntil(iter, IS_EVEN_INTEGER));
    }

    //-------------------------------------------------------------------------
    // mapAndFilter / filterAndMap / flatMapAndFilter / filterAndFlatMap
    //-------------------------------------------------------------------------

    @Test
    public void testMapAndFilterIterable() {
        List<String> input = Arrays.asList("1", "2a", "3", "4b", "5");
        Function<String, Integer> mapper = s -> s.length(); // map to length
        Predicate<Integer> filter = i -> i == 1; // filter if length is 1
        List<Integer> result = N.mapAndFilter(input, mapper, filter);
        assertEquals(Arrays.asList(1, 1, 1), result); // Lengths of "1", "3", "5"

        assertTrue(N.mapAndFilter(null, mapper, filter).isEmpty());
        assertTrue(N.mapAndFilter(Collections.emptyList(), mapper, filter).isEmpty());
    }

    @Test
    public void testMapAndFilterIterableWithSupplier() {
        List<String> input = Arrays.asList("1", "2a", "3", "2a", "5");
        Function<String, Integer> mapper = s -> s.length();
        Predicate<Integer> filter = i -> i == 2; // filter if length is 2
        Set<Integer> result = N.mapAndFilter(input, mapper, filter, size -> new HashSet<>());
        assertEquals(Set.of(2), result); // Length of "2a" is 2
    }

    @Test
    public void testFilterAndMapIterable() {
        List<String> input = Arrays.asList("apple", "banana", "kiwi", "avocado");
        Predicate<String> filter = s -> s.startsWith("a"); // filter strings starting with "a"
        Function<String, Integer> mapper = String::length; // map to length
        List<Integer> result = N.filterAndMap(input, filter, mapper);
        assertEquals(Arrays.asList(5, 7), result); // Lengths of "apple", "avocado"

        assertTrue(N.filterAndMap(null, filter, mapper).isEmpty());
    }

    @Test
    public void testFilterAndMapIterableWithSupplier() {
        List<String> input = Arrays.asList("apple", "apricot", "banana", "apricot");
        Predicate<String> filter = s -> s.length() > 5;
        Function<String, String> mapper = String::toUpperCase;
        Set<String> result = N.filterAndMap(input, filter, mapper, HashSet::new);
        assertEquals(Set.of("APRICOT", "BANANA"), result);
    }

    @Test
    public void testFlatMapAndFilterIterable() {
        List<String> input = Arrays.asList("a b", "c", "d e f");
        Function<String, Collection<String>> mapper = s -> Arrays.asList(s.split(" ")); // flatMap to words
        Predicate<String> filter = s -> s.length() == 1; // filter words of length 1
        List<String> result = N.flatMapAndFilter(input, mapper, filter);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), result);

        assertTrue(N.flatMapAndFilter(null, mapper, filter).isEmpty());
    }

    @Test
    public void testFlatMapAndFilterIterableWithSupplier() {
        List<String> input = Arrays.asList("a b a", "c c", "d e f d");
        Function<String, Collection<String>> mapper = s -> Arrays.asList(s.split(" "));
        Predicate<String> filter = s -> s.length() == 1;
        Set<String> result = N.flatMapAndFilter(input, mapper, filter, HashSet::new);
        assertEquals(Set.of("a", "b", "c", "d", "e", "f"), result);
    }

    @Test
    public void testFilterAndFlatMapIterable() {
        List<String> input = Arrays.asList("one two", "three", "four five six", "seven");
        Predicate<String> filter = s -> s.contains(" "); // filter strings with spaces
        Function<String, Collection<String>> mapper = s -> Arrays.asList(s.split(" ")); // flatMap to words
        List<String> result = N.filterAndFlatMap(input, filter, mapper);
        assertEquals(Arrays.asList("one", "two", "four", "five", "six"), result);

        assertTrue(N.filterAndFlatMap(null, filter, mapper).isEmpty());
    }

    @Test
    public void testFilterAndFlatMapIterableWithSupplier() {
        List<String> input = Arrays.asList("one two one", "three", "four five six four", "seven");
        Predicate<String> filter = s -> s.contains(" ");
        Function<String, Collection<String>> mapper = s -> Arrays.asList(s.split(" "));
        Set<String> result = N.filterAndFlatMap(input, filter, mapper, HashSet::new);
        assertEquals(Set.of("one", "two", "four", "five", "six"), result);
    }

    //-------------------------------------------------------------------------
    // distinct methods
    //-------------------------------------------------------------------------

    @Test
    public void testDistinctIntArray() {
        assertArrayEquals(new int[] { 1, 2, 3 }, N.distinct(new int[] { 1, 2, 2, 3, 1, 3 }));
        assertArrayEquals(N.EMPTY_INT_ARRAY, N.distinct((int[]) null));
        assertArrayEquals(N.EMPTY_INT_ARRAY, N.distinct(new int[0]));
        assertArrayEquals(new int[] { 1 }, N.distinct(new int[] { 1, 1, 1 }));
    }

    @Test
    public void testDistinctIntArrayWithRange() {
        int[] arr = { 1, 2, 1, 3, 2, 4, 1 };
        assertArrayEquals(new int[] { 1, 2, 3 }, N.distinct(arr, 0, 4)); // {1,2,1,3} -> {1,2,3}
        assertArrayEquals(new int[] { 2, 4, 1 }, N.distinct(arr, 4, 7)); // {2,4,1} -> {2,4,1}
        assertArrayEquals(N.EMPTY_INT_ARRAY, N.distinct(arr, 2, 2));
    }

    // Omitting other primitive distinct tests for brevity, assume similar pattern

    @Test
    public void testDistinctGenericArray() {
        String[] arr = { "a", "b", "a", "c", "b" };
        assertEquals(List.of("a", "b", "c"), N.distinct(arr));
        assertTrue(N.distinct((String[]) null).isEmpty());
    }

    @Test
    public void testDistinctGenericArrayWithRange() {
        String[] arr = { "a", "b", "a", "c", "b", "d", "a" };
        assertEquals(List.of("a", "b", "c"), N.distinct(arr, 0, 4)); // {"a","b","a","c"}
        assertEquals(List.of("b", "d", "a"), N.distinct(arr, 4, 7)); // {"b","d","a"}
    }

    @Test
    public void testDistinctCollectionWithRange() {
        List<String> list = Arrays.asList("a", "b", "a", "c", "b", "d", "a");
        assertEquals(List.of("a", "b", "c"), N.distinct(list, 0, 4));
    }

    @Test
    public void testDistinctIterable() {
        Iterable<String> iter = Arrays.asList("x", "y", "x", "z", "y");
        assertEquals(List.of("x", "y", "z"), N.distinct(iter));
        assertTrue(N.distinct((Iterable<String>) null).isEmpty());

        // Test with a Set as iterable (already distinct)
        Set<String> set = Set.of("1", "2", "3");
        List<String> distinctFromSet = N.distinct(set);
        assertEquals(3, distinctFromSet.size());
        assertTrue(distinctFromSet.containsAll(Set.of("1", "2", "3")));
    }

    @Test
    public void testDistinctIterator() {
        Iterator<String> iter = Arrays.asList("x", "y", "x", "z", "y").iterator();
        assertEquals(List.of("x", "y", "z"), N.distinct(iter));
        assertTrue(N.distinct((Iterator<String>) null).isEmpty());
    }

    @Test
    public void testDistinctByArray() {
        String[] arr = { "apple", "apricot", "banana", "blueberry" };
        // Distinct by first char
        List<String> result = N.distinctBy(arr, s -> s.charAt(0));
        assertEquals(List.of("apple", "banana"), result); // or "apricot", "blueberry" depending on stability
                                                          // The implementation uses a Set, so order of first occurrence matters.
                                                          // "apple" (a), "banana" (b)
        assertTrue(N.distinctBy((String[]) null, s -> s.charAt(0)).isEmpty());
    }

    @Test
    public void testDistinctByArrayWithRange() {
        String[] arr = { "apple", "apricot", "banana", "avocado", "blueberry" };
        // Range {"apricot", "banana", "avocado"}
        // Keys: 'a', 'b', 'a'
        List<String> result = N.distinctBy(arr, 1, 4, s -> s.charAt(0));
        assertEquals(List.of("apricot", "banana"), result);
    }

    @Test
    public void testDistinctByArrayWithSupplier() {
        String[] arr = { "apple", "apricot", "banana", "apricot" };
        // Using Set as the result collection
        Set<String> result = N.distinctBy(arr, s -> s.charAt(0), HashSet::new);
        assertEquals(Set.of("apple", "banana"), result); // First 'a' is apple, first 'b' is banana
    }

    @Test
    public void testDistinctByCollectionWithRange() {
        List<String> list = Arrays.asList("apple", "apricot", "banana", "avocado", "blueberry");
        List<String> result = N.distinctBy(list, 1, 4, s -> s.charAt(0)); // "apricot", "banana", "avocado"
        assertEquals(List.of("apricot", "banana"), result);
    }

    @Test
    public void testDistinctByIterable() {
        List<String> list = Arrays.asList("cat", "cow", "dog", "deer");
        List<String> result = N.distinctBy(list, s -> s.charAt(0)); // c, d
        assertEquals(List.of("cat", "dog"), result);
    }

    @Test
    public void testDistinctByIterableWithSupplier() {
        List<String> list = Arrays.asList("cat", "cow", "dog", "deer", "dove");
        Set<String> result = N.distinctBy(list, s -> s.charAt(0), HashSet::new);
        assertEquals(Set.of("cat", "dog"), result); // First c is cat, first d is dog
    }

    @Test
    public void testDistinctByIterator() {
        Iterator<String> iter = Arrays.asList("cat", "cow", "dog", "deer").iterator();
        List<String> result = N.distinctBy(iter, s -> s.charAt(0));
        assertEquals(List.of("cat", "dog"), result);
    }

    @Test
    public void testDistinctByIteratorWithSupplier() {
        Iterator<String> iter = Arrays.asList("cat", "cow", "dog", "deer", "dove").iterator();
        Set<String> result = N.distinctBy(iter, s -> s.charAt(0), HashSet::new);
        assertEquals(Set.of("cat", "dog"), result);
    }

    //-------------------------------------------------------------------------
    // match methods
    //-------------------------------------------------------------------------

    @Test
    public void testAllMatchArray() {
        assertTrue(N.allMatch(new Integer[] { 2, 4, 6 }, IS_EVEN_INTEGER));
        assertFalse(N.allMatch(new Integer[] { 2, 3, 6 }, IS_EVEN_INTEGER));
        assertTrue(N.allMatch((Integer[]) null, IS_EVEN_INTEGER));
        assertTrue(N.allMatch(new Integer[0], IS_EVEN_INTEGER));
    }

    @Test
    public void testAllMatchIterable() {
        assertTrue(N.allMatch(List.of(2, 4, 6), IS_EVEN_INTEGER));
        assertFalse(N.allMatch(List.of(2, 3, 6), IS_EVEN_INTEGER));
    }

    @Test
    public void testAllMatchIterator() {
        assertTrue(N.allMatch(List.of(2, 4, 6).iterator(), IS_EVEN_INTEGER));
        assertFalse(N.allMatch(List.of(2, 3, 6).iterator(), IS_EVEN_INTEGER));
    }

    @Test
    public void testAnyMatchArray() {
        assertTrue(N.anyMatch(new Integer[] { 1, 3, 4 }, IS_EVEN_INTEGER));
        assertFalse(N.anyMatch(new Integer[] { 1, 3, 5 }, IS_EVEN_INTEGER));
        assertFalse(N.anyMatch((Integer[]) null, IS_EVEN_INTEGER));
    }

    @Test
    public void testAnyMatchIterable() {
        assertTrue(N.anyMatch(List.of(1, 3, 4), IS_EVEN_INTEGER));
        assertFalse(N.anyMatch(List.of(1, 3, 5), IS_EVEN_INTEGER));
    }

    @Test
    public void testAnyMatchIterator() {
        assertTrue(N.anyMatch(List.of(1, 3, 4).iterator(), IS_EVEN_INTEGER));
        assertFalse(N.anyMatch(List.of(1, 3, 5).iterator(), IS_EVEN_INTEGER));
    }

    @Test
    public void testNoneMatchArray() {
        assertTrue(N.noneMatch(new Integer[] { 1, 3, 5 }, IS_EVEN_INTEGER));
        assertFalse(N.noneMatch(new Integer[] { 1, 2, 5 }, IS_EVEN_INTEGER));
        assertTrue(N.noneMatch((Integer[]) null, IS_EVEN_INTEGER));
    }

    @Test
    public void testNoneMatchIterable() {
        assertTrue(N.noneMatch(List.of(1, 3, 5), IS_EVEN_INTEGER));
        assertFalse(N.noneMatch(List.of(1, 2, 5), IS_EVEN_INTEGER));
    }

    @Test
    public void testNoneMatchIterator() {
        assertTrue(N.noneMatch(List.of(1, 3, 5).iterator(), IS_EVEN_INTEGER));
        assertFalse(N.noneMatch(List.of(1, 2, 5).iterator(), IS_EVEN_INTEGER));
    }

    @Test
    public void testNMatchArray() {
        Integer[] arr = { 1, 2, 3, 4, 5, 6 };
        assertTrue(N.nMatch(arr, 3, 3, IS_EVEN_INTEGER)); // 2,4,6
        assertTrue(N.nMatch(arr, 2, 4, IS_EVEN_INTEGER));
        assertFalse(N.nMatch(arr, 4, 5, IS_EVEN_INTEGER));
        assertTrue(N.nMatch(arr, 0, 0, x -> x > 10));
        assertFalse(N.nMatch(arr, 1, 1, x -> x > 10));

        assertTrue(N.nMatch((Integer[]) null, 0, 0, IS_EVEN_INTEGER));
        assertFalse(N.nMatch((Integer[]) null, 1, 1, IS_EVEN_INTEGER));

        assertThrows(IllegalArgumentException.class, () -> N.nMatch(arr, -1, 2, IS_EVEN_INTEGER));
        assertThrows(IllegalArgumentException.class, () -> N.nMatch(arr, 3, 1, IS_EVEN_INTEGER));
    }

    @Test
    public void testNMatchIterable() {
        List<Integer> list = List.of(1, 2, 3, 4, 5, 6);
        assertTrue(N.nMatch(list, 3, 3, IS_EVEN_INTEGER));
        assertTrue(N.nMatch(list, 1, 2, x -> x > 5)); // Only 6 matches
        assertTrue(N.nMatch(list, 1, 1, x -> x > 5));
    }

    @Test
    public void testNMatchIterator() {
        Iterator<Integer> iter = List.of(1, 2, 3, 4, 5, 6).iterator();
        assertTrue(N.nMatch(iter, 3, 3, IS_EVEN_INTEGER));
    }

    @Test
    public void testAllTrue() {
        assertTrue(N.allTrue(new boolean[] { true, true, true }));
        assertFalse(N.allTrue(new boolean[] { true, false, true }));
        assertTrue(N.allTrue(null));
        assertTrue(N.allTrue(new boolean[0]));
    }

    @Test
    public void testAllFalse() {
        assertTrue(N.allFalse(new boolean[] { false, false, false }));
        assertFalse(N.allFalse(new boolean[] { false, true, false }));
        assertTrue(N.allFalse(null));
    }

    @Test
    public void testAnyTrue() {
        assertTrue(N.anyTrue(new boolean[] { false, true, false }));
        assertFalse(N.anyTrue(new boolean[] { false, false, false }));
        assertFalse(N.anyTrue(null));
    }

    @Test
    public void testAnyFalse() {
        assertTrue(N.anyFalse(new boolean[] { true, false, true }));
        assertFalse(N.anyFalse(new boolean[] { true, true, true }));
        assertFalse(N.anyFalse(null));
    }

    //-------------------------------------------------------------------------
    // count methods
    //-------------------------------------------------------------------------

    @Test
    public void testCountBooleanArray() {
        assertEquals(2, N.count(new boolean[] { true, false, true, false }, b -> b));
        assertEquals(0, N.count((boolean[]) null, b -> b));
    }

    @Test
    public void testCountBooleanArrayWithRange() {
        assertEquals(1, N.count(new boolean[] { true, false, true, false }, 1, 3, b -> b)); // false, true
    }

    // ... other primitive count methods would follow a similar pattern ...

    @Test
    public void testCountIntArray() {
        assertEquals(3, N.count(new int[] { 1, 2, 3, 4, 5, 6 }, IS_EVEN_INT));
        assertEquals(0, N.count((int[]) null, IS_EVEN_INT));
        assertEquals(0, N.count(new int[0], IS_EVEN_INT));
    }

    @Test
    public void testCountIntArrayWithRange() {
        assertEquals(2, N.count(new int[] { 1, 2, 3, 4, 5, 6 }, 0, 4, IS_EVEN_INT)); // 2, 4
        assertEquals(0, N.count(new int[] { 1, 2, 3, 4, 5, 6 }, 0, 1, IS_EVEN_INT));
    }

    @Test
    public void testCountGenericArray() {
        String[] arr = { "a", "b", "", "d", "" };
        assertEquals(3, N.count(arr, STRING_NOT_EMPTY));
        assertEquals(0, N.count((String[]) null, STRING_NOT_EMPTY));
    }

    @Test
    public void testCountGenericArrayWithRange() {
        String[] arr = { "a", "b", "", "d", "" };
        assertEquals(1, N.count(arr, 1, 3, STRING_NOT_EMPTY)); // "b", "" -> "b"
    }

    @Test
    public void testCountCollectionWithRange() {
        List<String> list = Arrays.asList("apple", "banana", "", "grape");
        assertEquals(2, N.count(list, 0, 3, STRING_NOT_EMPTY)); // apple, banana
    }

    @Test
    public void testCountIterable() {
        Iterable<String> iter = Arrays.asList("a", "", "c", "");
        assertEquals(2, N.count(iter, STRING_NOT_EMPTY));
        assertEquals(0, N.count((Iterable<String>) null, STRING_NOT_EMPTY));
    }

    @Test
    public void testCountIterator() {
        assertEquals(3, N.count(Arrays.asList(1, 2, 3).iterator()));
        assertEquals(0, N.count((Iterator<Integer>) null));
        assertEquals(0, N.count(Collections.emptyIterator()));
    }

    @Test
    public void testCountIteratorWithPredicate() {
        Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
        assertEquals(2, N.count(iter, IS_EVEN_INTEGER));
        assertEquals(0, N.count((Iterator<Integer>) null, IS_EVEN_INTEGER));
    }

    //-------------------------------------------------------------------------
    // merge methods
    //-------------------------------------------------------------------------
    @Test
    public void testMergeArrays() {
        Integer[] a1 = { 1, 3, 5 };
        Integer[] a2 = { 2, 4, 6 };
        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(a1, a2, selector);
        assertEquals(List.of(1, 2, 3, 4, 5, 6), result);

        assertTrue(N.merge((Integer[]) null, (Integer[]) null, selector).isEmpty());
        assertEquals(List.of(1, 2), N.merge(new Integer[] { 1, 2 }, null, selector));
        assertEquals(List.of(1, 2), N.merge(null, new Integer[] { 1, 2 }, selector));

        Integer[] a3 = { 1, 2, 7 };
        Integer[] a4 = { 3, 4, 5, 6 };
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7), N.merge(a3, a4, selector));
    }

    @Test
    public void testMergeIterables() {
        List<Integer> l1 = List.of(1, 3, 5);
        List<Integer> l2 = List.of(2, 4, 6);
        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        List<Integer> result = N.merge(l1, l2, selector);
        assertEquals(List.of(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeCollectionOfIterables() {
        List<Integer> l1 = List.of(1, 5);
        List<Integer> l2 = List.of(2, 4);
        List<Integer> l3 = List.of(3, 6);
        Collection<Iterable<Integer>> coll = List.of(l1, l2, l3);
        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        List<Integer> result = N.merge(coll, selector);
        assertEquals(List.of(1, 2, 3, 4, 5, 6), result);

        assertTrue(N.merge(Collections.emptyList(), selector).isEmpty());
        assertEquals(List.of(1, 5), N.merge(List.of(l1), selector));
    }

    @Test
    public void testMergeCollectionOfIterablesWithSupplier() {
        List<Integer> l1 = List.of(1, 5);
        List<Integer> l2 = List.of(2, 4);
        List<Integer> l3 = List.of(3, 6, 2); // Has duplicate for Set test
        Collection<Iterable<Integer>> coll = List.of(l1, l2, l3);
        BiFunction<Integer, Integer, MergeResult> selector = (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        Set<Integer> resultSet = N.merge(coll, selector, size -> new HashSet<>());
        // Order is not guaranteed in set, but elements should be {1,2,3,4,5,6}
        assertEquals(Set.of(1, 2, 3, 4, 5, 6), resultSet);
    }

    //-------------------------------------------------------------------------
    // zip/unzip methods
    //-------------------------------------------------------------------------

    @Test
    public void testZipArrays() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2, 3 }; // b is longer
        List<Pair<String, Integer>> result = N.zip(a, b, Pair::of);
        assertEquals(List.of(Pair.of("a", 1), Pair.of("b", 2)), result);
        assertTrue(N.zip(null, b, Pair::of).isEmpty());
        assertTrue(N.zip(a, null, Pair::of).isEmpty());
    }

    @Test
    public void testZipIterables() {
        List<String> l1 = List.of("x", "y", "z");
        List<Integer> l2 = List.of(10, 20); // l2 is shorter
        List<String> result = N.zip(l1, l2, (s, i) -> s + i);
        assertEquals(List.of("x10", "y20"), result);
    }

    @Test
    public void testZipArraysThree() {
        Character[] c = { 'P', 'Q', 'R' };
        String[] s = { "One", "Two" };
        Integer[] i = { 100, 200, 300 };
        List<Triple<Character, String, Integer>> result = N.zip(c, s, i, Triple::of);
        assertEquals(List.of(Triple.of('P', "One", 100), Triple.of('Q', "Two", 200)), result);
    }

    @Test
    public void testZipIterablesThree() {
        List<Character> l1 = List.of('A', 'B');
        List<String> l2 = List.of("Apple", "Ball", "Cat");
        List<Double> l3 = List.of(1.0, 2.0, 3.0, 4.0);
        List<String> result = N.zip(l1, l2, l3, (c, s, d) -> c + ":" + s + ":" + d);
        assertEquals(List.of("A:Apple:1.0", "B:Ball:2.0"), result);
    }

    @Test
    public void testZipArraysWithDefaults() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2, 3 };
        List<Pair<String, Integer>> result = N.zip(a, b, "defaultA", 0, Pair::of);
        assertEquals(List.of(Pair.of("a", 1), Pair.of("b", 2), Pair.of("defaultA", 3)), result);

        List<Pair<String, Integer>> result2 = N.zip(b, a, 0, "defaultA", (i, s) -> Pair.of(s, i)); // Swapped
        assertEquals(List.of(Pair.of("a", 1), Pair.of("b", 2), Pair.of("defaultA", 3)), result2);
    }

    @Test
    public void testZipIterablesWithDefaults() {
        List<String> l1 = List.of("x");
        List<Integer> l2 = List.of(10, 20);
        List<String> result = N.zip(l1, l2, "def", 0, (s, i) -> s + i);
        assertEquals(List.of("x10", "def20"), result);
    }

    @Test
    public void testZipArraysThreeWithDefaults() {
        Character[] c = { 'P' };
        String[] s = { "One", "Two" };
        Integer[] i = { 100, 200, 300 };
        List<String> result = N.zip(c, s, i, 'Z', "DefS", 0, (ch, str, in) -> "" + ch + str + in);
        assertEquals(List.of("POne100", "ZTwo200", "ZDefS300"), result);
    }

    @Test
    public void testZipIterablesThreeWithDefaults() {
        List<Character> l1 = List.of('A', 'B');
        List<String> l2 = List.of("Apple");
        List<Double> l3 = List.of(1.0, 2.0, 3.0);

        List<String> result = N.zip(l1, l2, l3, 'X', "YYY", 0.0, (ch, str, d) -> "" + ch + str + d);
        assertEquals(List.of("AApple1.0", "BYYY2.0", "XYYY3.0"), result);
    }

    @Test
    public void testZipArraysToTargetArray() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2, 3 };
        String[] result = N.zip(a, b, (s, i) -> s + i, String.class);
        assertArrayEquals(new String[] { "a1", "b2" }, result);
    }

    @Test
    public void testZipArraysWithDefaultsToTargetArray() {
        String[] a = { "a" };
        Integer[] b = { 1, 2 };
        String[] result = N.zip(a, b, "defA", 0, (s, i) -> s + i, String.class);
        assertArrayEquals(new String[] { "a1", "defA2" }, result);
    }

    @Test
    public void testZipArraysThreeToTargetArray() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2, 3 };
        Character[] c = { 'X', 'Y' };
        String[] result = N.zip(a, b, c, (s, i, ch) -> s + i + ch, String.class);
        assertArrayEquals(new String[] { "a1X", "b2Y" }, result);
    }

    @Test
    public void testZipArraysThreeWithDefaultsToTargetArray() {
        String[] a = { "a" };
        Integer[] b = { 1, 2 };
        Character[] c = { 'X', 'Y', 'Z' };
        String[] result = N.zip(a, b, c, "defA", 0, 'D', (s, i, ch) -> s + i + ch, String.class);
        assertArrayEquals(new String[] { "a1X", "defA2Y", "defA0Z" }, result);
    }

    @Test
    public void testUnzipIterable() {
        List<String> input = Arrays.asList("a:1", "b:2", "c:3");
        BiConsumer<String, Pair<String, Integer>> unzipper = (item, pair) -> {
            String[] parts = item.split(":");
            pair.setLeft(parts[0]);
            pair.setRight(Integer.parseInt(parts[1]));
        };
        Pair<List<String>, List<Integer>> result = N.unzip(input, unzipper);
        assertEquals(List.of("a", "b", "c"), result.left());
        assertEquals(List.of(1, 2, 3), result.right());

        Pair<List<String>, List<Integer>> emptyResult = N.unzip(null, unzipper);
        assertTrue(emptyResult.left().isEmpty());
        assertTrue(emptyResult.right().isEmpty());
    }

    @Test
    public void testUnzipIterableWithSupplier() {
        List<String> input = Arrays.asList("a:1:x", "b:2:y", "a:3:z"); // Duplicate key 'a' for first element
        BiConsumer<String, Pair<String, String>> unzipper = (item, pair) -> {
            String[] parts = item.split(":", 2); // Split into 2 parts
            pair.setLeft(parts[0]);
            pair.setRight(parts[1]);
        };
        // Supplier for HashSet for the first list, ArrayList for the second
        IntFunction<Collection<?>> supplier = size -> (size % 2 == 0) ? new HashSet<>(size) : new ArrayList<>(size);
        // This supplier logic is a bit arbitrary, just to show different types
        // In reality, the supplier is called with capacity, not to decide type based on capacity.
        // Corrected use:
        IntFunction<List<String>> listSupplier = ArrayList::new; // This would be passed to a method that expects it
        IntFunction<Set<String>> setSupplier = HashSet::new; // This too

        // The provided N.unzip takes a single IntFunction for *both* collections' initial capacity.
        // It always creates ArrayLists internally if a more specific Collection type isn't requested via a different signature.
        // Let's test with the existing signature
        Pair<Set<String>, List<String>> result = N.unzip(input, unzipper, size -> (size == 0 ? new HashSet<>() : new ArrayList<>()));
        // The provided supplier in N.unzip is actually for the initial capacity of internal ArrayLists
        // To get Set/List, one would typically use the version of unzip that goes to a Collector or
        // post-process. The current N.unzip (non-deprecated) always makes List<A>, List<B>.
        // The signature is: Pair<LC, RC> unzip(Iterable, BiConsumer, IntFunction<? extends Collection<?>> supplier)
        // This implies supplier.apply(len) is used for *both* lc and rc.
        // The implementation: lc = (LC) supplier.apply(len); rc = (RC) supplier.apply(len);
        // This means both collections will be of the type the supplier creates.

        // Let's re-test with that understanding. It will create two collections of the *same* type.
        Pair<List<String>, List<String>> resultDefault = N.unzip(input, (item, pair) -> {
            String[] parts = item.split(":", 2);
            pair.setLeft(parts[0]);
            pair.setRight(parts[1]);
        }, ArrayList::new); // Supplier that creates ArrayList

        assertEquals(List.of("a", "b", "a"), resultDefault.left());
        assertEquals(List.of("1:x", "2:y", "3:z"), resultDefault.right());
    }

    // Deprecated unzipp tests
    @Test
    @SuppressWarnings("deprecation")
    public void testUnzippIterable() {
        List<String> input = Arrays.asList("a:1:x", "b:2:y");
        BiConsumer<String, Triple<String, Integer, Character>> unzipper = (item, triple) -> {
            String[] parts = item.split(":");
            triple.setLeft(parts[0]);
            triple.setMiddle(Integer.parseInt(parts[1]));
            triple.setRight(parts[2].charAt(0));
        };
        Triple<List<String>, List<Integer>, List<Character>> result = N.unzipp(input, unzipper);
        assertEquals(List.of("a", "b"), result.left());
        assertEquals(List.of(1, 2), result.middle());
        assertEquals(List.of('x', 'y'), result.right());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testUnzippIterableWithSupplier() {
        List<String> input = Arrays.asList("a:1:x", "b:2:y", "a:3:z");
        BiConsumer<String, Triple<String, Integer, Character>> unzipper = (item, triple) -> {
            String[] parts = item.split(":");
            triple.setLeft(parts[0]);
            triple.setMiddle(Integer.parseInt(parts[1]));
            triple.setRight(parts[2].charAt(0));
        };
        // Supplier creates ArrayLists for all three
        Triple<List<String>, List<Integer>, List<Character>> result = N.unzipp(input, unzipper, ArrayList::new);
        assertEquals(List.of("a", "b", "a"), result.left());
        assertEquals(List.of(1, 2, 3), result.middle());
        assertEquals(List.of('x', 'y', 'z'), result.right());
    }

    //-------------------------------------------------------------------------
    // groupBy/countBy methods
    //-------------------------------------------------------------------------
    @Test
    public void testGroupByArray() {
        String[] arr = { "apple", "apricot", "banana", "blueberry", "avocado" };
        Map<Character, List<String>> result = N.groupBy(arr, s -> s.charAt(0));
        assertEquals(List.of("apple", "apricot", "avocado"), result.get('a'));
        assertEquals(List.of("banana", "blueberry"), result.get('b'));
        assertTrue(N.groupBy((String[]) null, s -> s.charAt(0)).isEmpty());
    }

    @Test
    public void testGroupByArrayWithMapSupplier() {
        String[] arr = { "apple", "apricot", "banana" };
        // Using TreeMap to ensure key order for assertion
        TreeMap<Character, List<String>> result = N.groupBy(arr, s -> s.charAt(0), TreeMap::new);
        assertEquals(List.of("apple", "apricot"), result.get('a'));
        assertEquals(List.of("banana"), result.get('b'));
        assertEquals(Arrays.asList('a', 'b'), new ArrayList<>(result.keySet())); // Check order
    }

    @Test
    public void testGroupByArrayWithRange() {
        String[] arr = { "one", "two", "three", "four", "five" };
        // Range: "two", "three", "four"
        // Keys: t (two, three), f (four)
        Map<Character, List<String>> result = N.groupBy(arr, 1, 4, s -> s.charAt(0));
        assertEquals(List.of("two", "three"), result.get('t'));
        assertEquals(List.of("four"), result.get('f'));
        assertNull(result.get('o'));
    }

    @Test
    public void testGroupByArrayWithRangeAndMapSupplier() {
        String[] arr = { "one", "two", "three", "four", "five" };
        TreeMap<Character, List<String>> result = N.groupBy(arr, 1, 4, s -> s.charAt(0), TreeMap::new);
        assertEquals(List.of("four"), result.get('f'));
        assertEquals(List.of("two", "three"), result.get('t'));
        assertEquals(Arrays.asList('f', 't'), new ArrayList<>(result.keySet()));
    }

    @Test
    public void testGroupByCollectionWithRange() {
        List<String> list = Arrays.asList("one", "two", "three", "four", "five");
        Map<Character, List<String>> result = N.groupBy(list, 1, 4, s -> s.charAt(0));
        assertEquals(List.of("two", "three"), result.get('t'));
        assertEquals(List.of("four"), result.get('f'));
    }

    @Test
    public void testGroupByIterable() {
        Iterable<String> iter = Arrays.asList("apple", "apricot", "banana");
        Map<Character, List<String>> result = N.groupBy(iter, s -> s.charAt(0));
        assertEquals(List.of("apple", "apricot"), result.get('a'));
    }

    @Test
    public void testGroupByIterableWithMapSupplier() {
        Iterable<String> iter = Arrays.asList("apple", "apricot", "banana");
        TreeMap<Character, List<String>> result = N.groupBy(iter, s -> s.charAt(0), Suppliers.ofTreeMap());
        assertEquals(List.of("apple", "apricot"), result.get('a'));
    }

    @Test
    public void testGroupByIterator() {
        Iterator<String> iter = Arrays.asList("apple", "apricot", "banana").iterator();
        Map<Character, List<String>> result = N.groupBy(iter, s -> s.charAt(0));
        assertEquals(List.of("apple", "apricot"), result.get('a'));
    }

    @Test
    public void testGroupByIteratorWithMapSupplier() {
        Iterator<String> iter = Arrays.asList("apple", "apricot", "banana").iterator();
        TreeMap<Character, List<String>> result = N.groupBy(iter, s -> s.charAt(0), Suppliers.ofTreeMap());
        assertEquals(List.of("apple", "apricot"), result.get('a'));
    }

    @Test
    public void testGroupByIterableWithValueExtractor() {
        List<String> list = Arrays.asList("apple:red", "banana:yellow", "apricot:orange");
        Function<String, Character> keyExtractor = s -> s.charAt(0);
        Function<String, String> valueExtractor = s -> s.split(":")[1];
        Map<Character, List<String>> result = N.groupBy(list, keyExtractor, valueExtractor);
        assertEquals(List.of("red", "orange"), result.get('a'));
        assertEquals(List.of("yellow"), result.get('b'));
    }

    @Test
    public void testGroupByIterableWithValueExtractorAndMapSupplier() {
        List<String> list = Arrays.asList("apple:red", "banana:yellow", "apricot:orange");
        TreeMap<Character, List<String>> result = N.groupBy(list, s -> s.charAt(0), s -> s.split(":")[1], TreeMap::new);
        assertEquals(List.of("red", "orange"), result.get('a'));
    }

    @Test
    public void testGroupByIteratorWithValueExtractor() {
        Iterator<String> iter = Arrays.asList("apple:red", "banana:yellow", "apricot:orange").iterator();
        Map<Character, List<String>> result = N.groupBy(iter, s -> s.charAt(0), s -> s.split(":")[1]);
        assertEquals(List.of("red", "orange"), result.get('a'));
    }

    @Test
    public void testGroupByIteratorWithValueExtractorAndMapSupplier() {
        Iterator<String> iter = Arrays.asList("apple:red", "banana:yellow", "apricot:orange").iterator();
        TreeMap<Character, List<String>> result = N.groupBy(iter, s -> s.charAt(0), s -> s.split(":")[1], TreeMap::new);
        assertEquals(List.of("red", "orange"), result.get('a'));
    }

    @Test
    public void testGroupByIterableWithCollector() {
        List<String> list = Arrays.asList("apple", "apricot", "banana", "blueberry");
        // Group by first char, collect to comma-separated string of lengths
        Collector<String, ?, String> valueCollector = Collectors.mapping(it -> String.valueOf(it.length()), Collectors.joining(","));
        Map<Character, String> result = N.groupBy(list, s -> s.charAt(0), valueCollector);
        assertEquals("5,7", result.get('a')); // apple (5), apricot (7)
        assertEquals("6,9", result.get('b')); // banana (6), blueberry (9)
    }

    @Test
    public void testGroupByIterableWithCollectorAndMapSupplier() {
        List<String> list = Arrays.asList("apple", "apricot", "banana");
        Collector<String, ?, Long> countingCollector = Collectors.counting();
        TreeMap<Character, Long> result = N.groupBy(list, s -> s.charAt(0), countingCollector, TreeMap::new);
        assertEquals(2L, result.get('a'));
        assertEquals(1L, result.get('b'));
    }

    @Test
    public void testGroupByIteratorWithCollector() {
        Iterator<String> iter = Arrays.asList("apple", "apricot", "banana").iterator();
        Collector<String, ?, Long> countingCollector = Collectors.counting();
        Map<Character, Long> result = N.groupBy(iter, s -> s.charAt(0), countingCollector);
        assertEquals(2L, result.get('a'));
    }

    @Test
    public void testGroupByIteratorWithCollectorAndMapSupplier() {
        Iterator<String> iter = Arrays.asList("apple", "apricot", "banana").iterator();
        Collector<String, ?, Long> countingCollector = Collectors.counting();
        TreeMap<Character, Long> result = N.groupBy(iter, s -> s.charAt(0), countingCollector, TreeMap::new);
        assertEquals(2L, result.get('a'));
    }

    @Test
    public void testCountByIterable() {
        List<String> list = Arrays.asList("apple", "apricot", "banana", "apricot");
        Map<Character, Integer> result = N.countBy(list, s -> s.charAt(0));
        assertEquals(3, result.get('a')); // apple, apricot, apricot
        assertEquals(1, result.get('b'));
        assertTrue(N.countBy((List<String>) null, s -> s).isEmpty());
    }

    @Test
    public void testCountByIterableWithMapSupplier() {
        List<String> list = Arrays.asList("apple", "apricot", "banana", "apricot");
        TreeMap<Character, Integer> result = N.countBy(list, s -> s.charAt(0), TreeMap::new);
        assertEquals(3, result.get('a'));
    }

    @Test
    public void testCountByIterator() {
        Iterator<String> iter = Arrays.asList("apple", "apricot", "banana", "apricot").iterator();
        Map<Character, Integer> result = N.countBy(iter, s -> s.charAt(0));
        assertEquals(3, result.get('a'));
    }

    @Test
    public void testCountByIteratorWithMapSupplier() {
        Iterator<String> iter = Arrays.asList("apple", "apricot", "banana", "apricot").iterator();
        TreeMap<Character, Integer> result = N.countBy(iter, s -> s.charAt(0), TreeMap::new);
        assertEquals(3, result.get('a'));
    }

    //-------------------------------------------------------------------------
    // iterate methods
    //-------------------------------------------------------------------------
    @Test
    public void testIterateArray() {
        String[] arr = { "a", "b" };
        Iterator<String> iter = N.iterate(arr);
        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());
        assertTrue(iter.hasNext());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());

        assertFalse(N.iterate((String[]) null).hasNext());
        assertFalse(N.iterate(new String[0]).hasNext());
    }

    @Test
    public void testIterateArrayWithRange() {
        String[] arr = { "a", "b", "c", "d" };
        Iterator<String> iter = N.iterate(arr, 1, 3); // "b", "c"
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        assertFalse(N.iterate(arr, 1, 1).hasNext());
    }

    @Test
    public void testIterateMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        Iterator<Map.Entry<String, Integer>> iter = N.iterate(map);
        int count = 0;
        while (iter.hasNext()) {
            Map.Entry<String, Integer> entry = iter.next();
            assertTrue(map.containsKey(entry.getKey()));
            assertEquals(map.get(entry.getKey()), entry.getValue());
            count++;
        }
        assertEquals(2, count);
        assertFalse(N.iterate((Map<String, Integer>) null).hasNext());
    }

    @Test
    public void testIterateIterable() {
        List<String> list = List.of("x", "y");
        Iterator<String> iter = N.iterate(list);
        assertEquals("x", iter.next());
        assertEquals("y", iter.next());
        assertFalse(iter.hasNext());
        assertFalse(N.iterate((Iterable<String>) null).hasNext());
    }

    @Test
    public void testIterateEach() {
        List<String> l1 = List.of("a", "b");
        List<String> l2 = List.of("c");
        List<String> l3 = Collections.emptyList();
        Collection<Iterable<String>> iterables = N.asList(l1, l2, l3, null); // null iterable

        List<Iterator<String>> resultIterators = N.iterateEach(iterables);
        assertEquals(4, resultIterators.size());

        Iterator<String> iter1 = resultIterators.get(0);
        assertEquals("a", iter1.next());
        assertEquals("b", iter1.next());
        assertFalse(iter1.hasNext());

        Iterator<String> iter2 = resultIterators.get(1);
        assertEquals("c", iter2.next());
        assertFalse(iter2.hasNext());

        Iterator<String> iter3 = resultIterators.get(2);
        assertFalse(iter3.hasNext());

        Iterator<String> iter4 = resultIterators.get(3); // Iterator for null iterable is empty
        assertFalse(iter4.hasNext());

        assertTrue(N.iterateEach(null).isEmpty());
    }

    @Test
    public void testIterateAll() {
        List<String> l1 = List.of("a", "b");
        List<String> l2 = List.of("c");
        List<String> l3 = Collections.emptyList();
        Collection<Iterable<String>> iterables = N.asList(l1, null, l2, l3);

        Iterator<String> combinedIter = N.iterateAll(iterables);
        assertEquals("a", combinedIter.next());
        assertEquals("b", combinedIter.next());
        assertEquals("c", combinedIter.next());
        assertFalse(combinedIter.hasNext());

        assertFalse(N.iterateAll(null).hasNext());
        assertFalse(N.iterateAll(Collections.emptyList()).hasNext());
    }

    //-------------------------------------------------------------------------
    // disjoint methods
    //-------------------------------------------------------------------------
    @Test
    public void testDisjointArrays() {
        assertTrue(N.disjoint(new String[] { "a", "b" }, new String[] { "c", "d" }));
        assertFalse(N.disjoint(new String[] { "a", "b" }, new String[] { "b", "c" }));
        assertTrue(N.disjoint(null, new String[] { "a" }));
        assertTrue(N.disjoint(new String[] { "a" }, null));
        assertTrue(N.disjoint(new String[0], new String[] { "a" }));
    }

    @Test
    public void testDisjointCollections() {
        assertTrue(N.disjoint(List.of("a", "b"), Set.of("c", "d")));
        assertFalse(N.disjoint(Set.of("a", "b"), List.of("b", "c")));
        assertTrue(N.disjoint(null, List.of("a")));
        assertTrue(N.disjoint(Collections.emptySet(), List.of("a")));

        // Test different collection types and sizes for internal optimization paths
        assertTrue(N.disjoint(new HashSet<>(List.of("a", "b")), new ArrayList<>(List.of("c", "d"))));
        assertFalse(N.disjoint(new ArrayList<>(List.of("c", "d", "a")), new HashSet<>(List.of("a", "b"))));
    }
}
