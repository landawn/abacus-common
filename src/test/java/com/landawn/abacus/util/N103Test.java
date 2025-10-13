package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class N103Test extends TestBase {

    private boolean[] booleanArray;
    private char[] charArray;
    private byte[] byteArray;
    private short[] shortArray;
    private int[] intArray;
    private long[] longArray;
    private float[] floatArray;
    private double[] doubleArray;
    private String[] stringArray;
    private Integer[] integerArray;
    private List<String> stringList;
    private List<Integer> integerList;
    private Set<String> stringSet;
    private Map<String, Integer> stringIntMap;

    private boolean[] emptyBooleanArray = new boolean[0];
    private char[] emptyCharArray = new char[0];
    private int[] emptyIntArray = new int[0];
    private String[] emptyStringArray = new String[0];

    private boolean[] singleBooleanArray = new boolean[] { true };
    private int[] singleIntArray = new int[] { 42 };
    private String[] singleStringArray = new String[] { "single" };

    private int[] duplicateIntArray = new int[] { 1, 1, 1, 1, 1 };
    private String[] duplicateStringArray = new String[] { "dup", "dup", "dup" };

    private Integer[] nullContainingArray = new Integer[] { 1, null, 3, null, 5 };
    private List<String> nullContainingList = Arrays.asList("a", null, "c", null, "e");

    private int[] largeIntArray;
    private List<Integer> largeIntList;

    @BeforeEach
    public void setUp() {
        booleanArray = new boolean[] { true, false, true, false, true };
        charArray = new char[] { 'a', 'b', 'c', 'd', 'e' };
        byteArray = new byte[] { 1, 2, 3, 4, 5 };
        shortArray = new short[] { 1, 2, 3, 4, 5 };
        intArray = new int[] { 1, 2, 3, 4, 5 };
        longArray = new long[] { 1L, 2L, 3L, 4L, 5L };
        floatArray = new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        doubleArray = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 };
        stringArray = new String[] { "one", "two", "three", "four", "five" };
        integerArray = new Integer[] { 1, 2, 3, 4, 5 };
        stringList = Arrays.asList("one", "two", "three", "four", "five");
        integerList = Arrays.asList(1, 2, 3, 4, 5);
        stringSet = new HashSet<>(stringList);
        stringIntMap = new HashMap<>();
        stringIntMap.put("one", 1);
        stringIntMap.put("two", 2);
        stringIntMap.put("three", 3);
        largeIntArray = new int[10000];
        largeIntList = new ArrayList<>(10000);
        for (int i = 0; i < 10000; i++) {
            largeIntArray[i] = i;
            largeIntList.add(i);
        }
    }

    @Test
    public void testFilterBooleanArray() {
        boolean[] result = N.filter(booleanArray, b -> b);
        assertArrayEquals(new boolean[] { true, true, true }, result);

        result = N.filter(booleanArray, b -> !b);
        assertArrayEquals(new boolean[] { false, false }, result);

        result = N.filter(new boolean[0], b -> b);
        assertArrayEquals(new boolean[0], result);

        result = N.filter((boolean[]) null, b -> b);
        assertArrayEquals(new boolean[0], result);
    }

    @Test
    public void testFilterBooleanArrayWithRange() {
        boolean[] result = N.filter(booleanArray, 1, 4, b -> b);
        assertArrayEquals(new boolean[] { true }, result);

        result = N.filter(booleanArray, 0, 3, b -> !b);
        assertArrayEquals(new boolean[] { false }, result);
    }

    @Test
    public void testFilterCharArray() {
        char[] result = N.filter(charArray, c -> c > 'b');
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, result);

        result = N.filter(charArray, c -> c <= 'c');
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testFilterCharArrayWithRange() {
        char[] result = N.filter(charArray, 1, 4, c -> c > 'b');
        assertArrayEquals(new char[] { 'c', 'd' }, result);
    }

    @Test
    public void testFilterByteArray() {
        byte[] result = N.filter(byteArray, b -> b > 2);
        assertArrayEquals(new byte[] { 3, 4, 5 }, result);

        result = N.filter(byteArray, b -> b % 2 == 0);
        assertArrayEquals(new byte[] { 2, 4 }, result);
    }

    @Test
    public void testFilterByteArrayWithRange() {
        byte[] result = N.filter(byteArray, 1, 4, b -> b > 2);
        assertArrayEquals(new byte[] { 3, 4 }, result);
    }

    @Test
    public void testFilterShortArray() {
        short[] result = N.filter(shortArray, s -> s > 3);
        assertArrayEquals(new short[] { 4, 5 }, result);

        result = N.filter(shortArray, s -> s <= 3);
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testFilterShortArrayWithRange() {
        short[] result = N.filter(shortArray, 2, 5, s -> s > 3);
        assertArrayEquals(new short[] { 4, 5 }, result);
    }

    @Test
    public void testFilterIntArray() {
        int[] result = N.filter(intArray, i -> i % 2 == 0);
        assertArrayEquals(new int[] { 2, 4 }, result);

        result = N.filter(intArray, i -> i > 3);
        assertArrayEquals(new int[] { 4, 5 }, result);
    }

    @Test
    public void testFilterIntArrayWithRange() {
        int[] result = N.filter(intArray, 1, 4, i -> i % 2 == 0);
        assertArrayEquals(new int[] { 2, 4 }, result);
    }

    @Test
    public void testFilterLongArray() {
        long[] result = N.filter(longArray, l -> l > 2L);
        assertArrayEquals(new long[] { 3L, 4L, 5L }, result);

        result = N.filter(longArray, l -> l % 2 == 0);
        assertArrayEquals(new long[] { 2L, 4L }, result);
    }

    @Test
    public void testFilterLongArrayWithRange() {
        long[] result = N.filter(longArray, 0, 3, l -> l > 1L);
        assertArrayEquals(new long[] { 2L, 3L }, result);
    }

    @Test
    public void testFilterFloatArray() {
        float[] result = N.filter(floatArray, f -> f > 3.0f);
        assertArrayEquals(new float[] { 4.0f, 5.0f }, result, 0.001f);

        result = N.filter(floatArray, f -> f <= 3.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, 0.001f);
    }

    @Test
    public void testFilterFloatArrayWithRange() {
        float[] result = N.filter(floatArray, 1, 4, f -> f > 2.0f);
        assertArrayEquals(new float[] { 3.0f, 4.0f }, result, 0.001f);
    }

    @Test
    public void testFilterDoubleArray() {
        double[] result = N.filter(doubleArray, d -> d > 3.0);
        assertArrayEquals(new double[] { 4.0, 5.0 }, result, 0.001);

        result = N.filter(doubleArray, d -> d <= 3.0);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testFilterDoubleArrayWithRange() {
        double[] result = N.filter(doubleArray, 2, 5, d -> d > 3.0);
        assertArrayEquals(new double[] { 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testFilterObjectArray() {
        List<String> result = N.filter(stringArray, s -> s.length() > 3);
        assertEquals(Arrays.asList("three", "four", "five"), result);

        result = N.filter(stringArray, s -> s.startsWith("t"));
        assertEquals(Arrays.asList("two", "three"), result);
    }

    @Test
    public void testFilterObjectArrayWithSupplier() {
        Set<String> result = N.filter(stringArray, s -> s.length() > 3, size -> new HashSet<>());
        assertEquals(new HashSet<>(Arrays.asList("three", "four", "five")), result);
    }

    @Test
    public void testFilterObjectArrayWithRange() {
        List<String> result = N.filter(stringArray, 1, 4, s -> s.length() > 3);
        assertEquals(Arrays.asList("three", "four"), result);
    }

    @Test
    public void testFilterCollection() {
        List<String> result = N.filter(stringList, s -> s.length() > 3);
        assertEquals(Arrays.asList("three", "four", "five"), result);
    }

    @Test
    public void testFilterCollectionWithRange() {
        List<String> result = N.filter(stringList, 1, 4, s -> s.length() > 3);
        assertEquals(Arrays.asList("three", "four"), result);
    }

    @Test
    public void testFilterIterable() {
        List<Integer> result = N.filter((Iterable<Integer>) integerList, i -> i % 2 == 0);
        assertEquals(Arrays.asList(2, 4), result);
    }

    @Test
    public void testFilterIterator() {
        List<Integer> result = N.filter(integerList.iterator(), i -> i > 3);
        assertEquals(Arrays.asList(4, 5), result);
    }

    @Test
    public void testMapToBoolean() {
        boolean[] result = N.mapToBoolean(stringArray, s -> s.length() > 3);
        assertArrayEquals(new boolean[] { false, false, true, true, true }, result);
    }

    @Test
    public void testMapToBooleanWithRange() {
        boolean[] result = N.mapToBoolean(stringArray, 1, 4, s -> s.length() > 3);
        assertArrayEquals(new boolean[] { false, true, true }, result);
    }

    @Test
    public void testMapToBooleanFromCollection() {
        boolean[] result = N.mapToBoolean(stringList, s -> s.startsWith("t"));
        assertArrayEquals(new boolean[] { false, true, true, false, false }, result);
    }

    @Test
    public void testMapToChar() {
        char[] result = N.mapToChar(stringArray, s -> s.charAt(0));
        assertArrayEquals(new char[] { 'o', 't', 't', 'f', 'f' }, result);
    }

    @Test
    public void testMapToCharWithRange() {
        char[] result = N.mapToChar(stringArray, 1, 3, s -> s.charAt(0));
        assertArrayEquals(new char[] { 't', 't' }, result);
    }

    @Test
    public void testMapToByte() {
        byte[] result = N.mapToByte(integerArray, i -> i.byteValue());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testMapToByteWithRange() {
        byte[] result = N.mapToByte(integerArray, 2, 5, i -> i.byteValue());
        assertArrayEquals(new byte[] { 3, 4, 5 }, result);
    }

    @Test
    public void testMapToShort() {
        short[] result = N.mapToShort(integerArray, i -> i.shortValue());
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testMapToShortWithRange() {
        short[] result = N.mapToShort(integerArray, 1, 4, i -> i.shortValue());
        assertArrayEquals(new short[] { 2, 3, 4 }, result);
    }

    @Test
    public void testMapToInt() {
        int[] result = N.mapToInt(stringArray, String::length);
        assertArrayEquals(new int[] { 3, 3, 5, 4, 4 }, result);
    }

    @Test
    public void testMapToIntWithRange() {
        int[] result = N.mapToInt(stringArray, 0, 3, String::length);
        assertArrayEquals(new int[] { 3, 3, 5 }, result);
    }

    @Test
    public void testMapToIntFromLongArray() {
        int[] result = N.mapToInt(longArray, l -> (int) l);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testMapToIntFromDoubleArray() {
        int[] result = N.mapToInt(doubleArray, d -> (int) d);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testMapToLong() {
        long[] result = N.mapToLong(integerArray, i -> i.longValue());
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testMapToLongWithRange() {
        long[] result = N.mapToLong(integerArray, 2, 5, i -> i.longValue());
        assertArrayEquals(new long[] { 3L, 4L, 5L }, result);
    }

    @Test
    public void testMapToLongFromIntArray() {
        long[] result = N.mapToLong(intArray, i -> (long) i);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testMapToLongFromDoubleArray() {
        long[] result = N.mapToLong(doubleArray, d -> (long) d);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testMapToFloat() {
        float[] result = N.mapToFloat(integerArray, i -> i.floatValue());
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, result, 0.001f);
    }

    @Test
    public void testMapToFloatWithRange() {
        float[] result = N.mapToFloat(integerArray, 1, 4, i -> i.floatValue());
        assertArrayEquals(new float[] { 2.0f, 3.0f, 4.0f }, result, 0.001f);
    }

    @Test
    public void testMapToDouble() {
        double[] result = N.mapToDouble(integerArray, i -> i.doubleValue());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testMapToDoubleWithRange() {
        double[] result = N.mapToDouble(integerArray, 0, 3, i -> i.doubleValue());
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testMapToDoubleFromIntArray() {
        double[] result = N.mapToDouble(intArray, i -> (double) i);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testMapToDoubleFromLongArray() {
        double[] result = N.mapToDouble(longArray, l -> (double) l);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testMap() {
        List<Integer> result = N.map(stringArray, String::length);
        assertEquals(Arrays.asList(3, 3, 5, 4, 4), result);
    }

    @Test
    public void testMapWithSupplier() {
        Set<Integer> result = N.map(stringArray, String::length, size -> new HashSet<>());
        assertEquals(new HashSet<>(Arrays.asList(3, 4, 5)), result);
    }

    @Test
    public void testMapWithRange() {
        List<Integer> result = N.map(stringArray, 1, 4, String::length);
        assertEquals(Arrays.asList(3, 5, 4), result);
    }

    @Test
    public void testMapCollection() {
        List<Integer> result = N.map(stringList, 0, 3, String::length);
        assertEquals(Arrays.asList(3, 3, 5), result);
    }

    @Test
    public void testMapIterable() {
        List<String> result = N.map((Iterable<Integer>) integerList, i -> "num" + i);
        assertEquals(Arrays.asList("num1", "num2", "num3", "num4", "num5"), result);
    }

    @Test
    public void testMapIterator() {
        List<String> result = N.map(integerList.iterator(), i -> "num" + i);
        assertEquals(Arrays.asList("num1", "num2", "num3", "num4", "num5"), result);
    }

    @Test
    public void testFlatMap() {
        List<Character> result = N.flatMap(stringArray, s -> Arrays.asList(s.charAt(0), s.charAt(1)));
        assertEquals(Arrays.asList('o', 'n', 't', 'w', 't', 'h', 'f', 'o', 'f', 'i'), result);
    }

    @Test
    public void testFlatMapWithSupplier() {
        Set<Character> result = N.flatMap(new String[] { "ab", "cd", "ef" }, s -> Arrays.asList(s.charAt(0), s.charAt(1)), IntFunctions.ofSet());
        assertEquals(new HashSet<>(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f')), result);
    }

    @Test
    public void testFlatMapWithRange() {
        List<Character> result = N.flatMap(stringArray, 0, 2, s -> Arrays.asList(s.charAt(0), s.charAt(1)));
        assertEquals(Arrays.asList('o', 'n', 't', 'w'), result);
    }

    @Test
    public void testFlatMapCollection() {
        List<Character> result = N.flatMap(Arrays.asList("ab", "cd"), 1, 2, s -> Arrays.asList(s.charAt(0), s.charAt(1)));
        assertEquals(Arrays.asList('c', 'd'), result);
    }

    @Test
    public void testFlatMapIterable() {
        List<Integer> result = N.flatMap(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)), list -> list);
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlatMapIterator() {
        List<Integer> result = N.flatMap(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)).iterator(), list -> list);
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlatMapTwice() {
        List<Integer> result = N.flatMap(new String[] { "1,2", "3,4" }, s -> Arrays.asList(s.split(",")),
                str -> Arrays.asList(Integer.parseInt(String.valueOf(str))), IntFunctions.ofList());
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testTakeWhile() {
        List<Integer> result = N.takeWhile(integerArray, i -> i < 4);
        assertEquals(Arrays.asList(1, 2, 3), result);

        result = N.takeWhile(integerArray, i -> i < 1);
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void testTakeWhileIterable() {
        List<String> result = N.takeWhile(stringList, s -> s.length() == 3);
        assertEquals(Arrays.asList("one", "two"), result);
    }

    @Test
    public void testTakeWhileIterator() {
        List<Integer> result = N.takeWhile(integerList.iterator(), i -> i < 3);
        assertEquals(Arrays.asList(1, 2), result);
    }

    @Test
    public void testTakeWhileInclusive() {
        List<Integer> result = N.takeWhileInclusive(integerArray, i -> i < 3);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testTakeWhileInclusiveIterable() {
        List<String> result = N.takeWhileInclusive(stringList, s -> s.length() == 3);
        assertEquals(Arrays.asList("one", "two", "three"), result);
    }

    @Test
    public void testTakeWhileInclusiveIterator() {
        List<Integer> result = N.takeWhileInclusive(integerList.iterator(), i -> i < 3);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testDropWhile() {
        List<Integer> result = N.dropWhile(integerArray, i -> i < 3);
        assertEquals(Arrays.asList(3, 4, 5), result);

        result = N.dropWhile(integerArray, i -> i < 10);
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void testDropWhileIterable() {
        List<String> result = N.dropWhile(stringList, s -> s.length() == 3);
        assertEquals(Arrays.asList("three", "four", "five"), result);
    }

    @Test
    public void testDropWhileIterator() {
        List<Integer> result = N.dropWhile(integerList.iterator(), i -> i < 4);
        assertEquals(Arrays.asList(4, 5), result);
    }

    @Test
    public void testSkipUntil() {
        List<Integer> result = N.skipUntil(integerArray, i -> i > 3);
        assertEquals(Arrays.asList(4, 5), result);
    }

    @Test
    public void testSkipUntilIterable() {
        List<String> result = N.skipUntil(stringList, s -> s.length() > 3);
        assertEquals(Arrays.asList("three", "four", "five"), result);
    }

    @Test
    public void testSkipUntilIterator() {
        List<Integer> result = N.skipUntil(integerList.iterator(), i -> i == 3);
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testMapAndFilter() {
        List<Integer> result = N.mapAndFilter(stringList, String::length, len -> len > 3);
        assertEquals(Arrays.asList(5, 4, 4), result);
    }

    @Test
    public void testMapAndFilterWithSupplier() {
        Set<Integer> result = N.mapAndFilter(stringList, String::length, len -> len > 3, size -> new HashSet<>());
        assertEquals(new HashSet<>(Arrays.asList(5, 4)), result);
    }

    @Test
    public void testFilterAndMap() {
        List<Integer> result = N.filterAndMap(stringList, s -> s.length() > 3, String::length);
        assertEquals(Arrays.asList(5, 4, 4), result);
    }

    @Test
    public void testFilterAndMapWithSupplier() {
        Set<String> result = N.filterAndMap(integerList, i -> i % 2 == 0, i -> "even:" + i, size -> new HashSet<>());
        assertEquals(new HashSet<>(Arrays.asList("even:2", "even:4")), result);
    }

    @Test
    public void testFlatMapAndFilter() {
        List<Character> result = N.flatMapAndFilter(Arrays.asList("abc", "de", "fghi"), s -> s.chars().mapToObj(c -> (char) c).collect(Collectors.toList()),
                c -> c > 'c');
        assertEquals(Arrays.asList('d', 'e', 'f', 'g', 'h', 'i'), result);
    }

    @Test
    public void testFilterAndFlatMap() {
        List<Character> result = N.filterAndFlatMap(Arrays.asList("abc", "de", "fghi"), s -> s.length() > 2,
                s -> s.chars().mapToObj(c -> (char) c).collect(Collectors.toList()));
        assertEquals(Arrays.asList('a', 'b', 'c', 'f', 'g', 'h', 'i'), result);
    }

    @Test
    public void testDistinctCharArray() {
        char[] input = { 'a', 'b', 'a', 'c', 'b', 'd' };
        char[] result = N.distinct(input);
        assertEquals(4, result.length);
        assertTrue(contains(result, 'a'));
        assertTrue(contains(result, 'b'));
        assertTrue(contains(result, 'c'));
        assertTrue(contains(result, 'd'));
    }

    @Test
    public void testDistinctIntArray() {
        int[] input = { 1, 2, 3, 2, 4, 3, 5 };
        int[] result = N.distinct(input);
        assertEquals(5, result.length);
        assertTrue(contains(result, 1));
        assertTrue(contains(result, 2));
        assertTrue(contains(result, 3));
        assertTrue(contains(result, 4));
        assertTrue(contains(result, 5));
    }

    @Test
    public void testDistinctObjectArray() {
        String[] input = { "one", "two", "one", "three", "two" };
        List<String> result = N.distinct(input);
        assertEquals(3, result.size());
        assertTrue(result.contains("one"));
        assertTrue(result.contains("two"));
        assertTrue(result.contains("three"));
    }

    @Test
    public void testDistinctCollection() {
        List<Integer> input = Arrays.asList(1, 2, 3, 2, 4, 3, 5);
        List<Integer> result = N.distinct(input, 1, 6);
        assertEquals(3, result.size());
    }

    @Test
    public void testDistinctIterable() {
        List<String> input = Arrays.asList("a", "b", "a", "c", "b");
        List<String> result = N.distinct((Iterable<String>) input);
        assertEquals(3, result.size());
    }

    @Test
    public void testDistinctIterator() {
        List<Integer> input = Arrays.asList(1, 2, 1, 3, 2);
        List<Integer> result = N.distinct(input.iterator());
        assertEquals(3, result.size());
    }

    @Test
    public void testDistinctBy() {
        List<String> result = N.distinctBy(stringArray, String::length);
        assertEquals(3, result.size());
    }

    @Test
    public void testDistinctByWithSupplier() {
        Set<String> result = N.distinctBy(stringArray, String::length, HashSet::new);
        assertEquals(3, result.size());
    }

    @Test
    public void testAllMatch() {
        assertTrue(N.allMatch(new Integer[] { 2, 4, 6 }, i -> i % 2 == 0));
        assertFalse(N.allMatch(integerArray, i -> i % 2 == 0));
        assertTrue(N.allMatch(new Integer[0], i -> false));
    }

    @Test
    public void testAllMatchIterable() {
        assertTrue(N.allMatch(Arrays.asList(2, 4, 6), i -> i % 2 == 0));
        assertFalse(N.allMatch(integerList, i -> i > 3));
    }

    @Test
    public void testAllMatchIterator() {
        assertTrue(N.allMatch(Arrays.asList(1, 2, 3).iterator(), i -> i < 10));
        assertFalse(N.allMatch(integerList.iterator(), i -> i > 3));
    }

    @Test
    public void testAnyMatch() {
        assertTrue(N.anyMatch(integerArray, i -> i > 4));
        assertFalse(N.anyMatch(integerArray, i -> i > 10));
        assertFalse(N.anyMatch(new Integer[0], i -> true));
    }

    @Test
    public void testAnyMatchIterable() {
        assertTrue(N.anyMatch(integerList, i -> i % 2 == 0));
        assertFalse(N.anyMatch(integerList, i -> i > 10));
    }

    @Test
    public void testAnyMatchIterator() {
        assertTrue(N.anyMatch(integerList.iterator(), i -> i == 3));
        assertFalse(N.anyMatch(integerList.iterator(), i -> i < 0));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(N.noneMatch(integerArray, i -> i > 10));
        assertFalse(N.noneMatch(integerArray, i -> i > 3));
        assertTrue(N.noneMatch(new Integer[0], i -> true));
    }

    @Test
    public void testNoneMatchIterable() {
        assertTrue(N.noneMatch(integerList, i -> i > 10));
        assertFalse(N.noneMatch(integerList, i -> i % 2 == 0));
    }

    @Test
    public void testNoneMatchIterator() {
        assertTrue(N.noneMatch(integerList.iterator(), i -> i < 0));
        assertFalse(N.noneMatch(integerList.iterator(), i -> i == 3));
    }

    @Test
    public void testNMatch() {
        assertTrue(N.nMatch(integerArray, 2, 3, i -> i % 2 == 0));
        assertFalse(N.nMatch(integerArray, 3, 4, i -> i % 2 == 0));
        assertTrue(N.nMatch(new Integer[0], 0, 0, i -> true));
    }

    @Test
    public void testNMatchIterable() {
        assertTrue(N.nMatch(integerList, 1, 2, i -> i > 3));
        assertFalse(N.nMatch(integerList, 0, 1, i -> i > 3));
    }

    @Test
    public void testNMatchIterator() {
        assertTrue(N.nMatch(integerList.iterator(), 2, 2, i -> i % 2 == 0));
        assertFalse(N.nMatch(integerList.iterator(), 3, 5, i -> i % 2 == 0));
    }

    @Test
    public void testAllTrue() {
        assertTrue(N.allTrue(new boolean[] { true, true, true }));
        assertFalse(N.allTrue(booleanArray));
        assertTrue(N.allTrue(new boolean[0]));
    }

    @Test
    public void testAllFalse() {
        assertTrue(N.allFalse(new boolean[] { false, false, false }));
        assertFalse(N.allFalse(booleanArray));
        assertTrue(N.allFalse(new boolean[0]));
    }

    @Test
    public void testAnyTrue() {
        assertTrue(N.anyTrue(booleanArray));
        assertFalse(N.anyTrue(new boolean[] { false, false }));
        assertFalse(N.anyTrue(new boolean[0]));
    }

    @Test
    public void testAnyFalse() {
        assertTrue(N.anyFalse(booleanArray));
        assertFalse(N.anyFalse(new boolean[] { true, true }));
        assertFalse(N.anyFalse(new boolean[0]));
    }

    @Test
    public void testCountBooleanArray() {
        assertEquals(3, N.count(booleanArray, b -> b));
        assertEquals(2, N.count(booleanArray, b -> !b));
        assertEquals(0, N.count(new boolean[0], b -> b));
    }

    @Test
    public void testCountBooleanArrayWithRange() {
        assertEquals(1, N.count(booleanArray, 1, 4, b -> b));
        assertEquals(2, N.count(booleanArray, 1, 4, b -> !b));
    }

    @Test
    public void testCountCharArray() {
        assertEquals(3, N.count(charArray, c -> c > 'b'));
        assertEquals(2, N.count(charArray, c -> c <= 'b'));
    }

    @Test
    public void testCountObjectArray() {
        assertEquals(3, N.count(stringArray, s -> s.length() > 3));
        assertEquals(2, N.count(stringArray, s -> s.startsWith("t")));
    }

    @Test
    public void testCountCollection() {
        assertEquals(3, N.count(stringList, 0, 5, s -> s.length() > 3));
        assertEquals(2, N.count(stringList, 1, 4, s -> s.length() > 3));
        assertEquals(2, N.count(N.newLinkedList(stringList), 1, 4, s -> s.length() > 3));
    }

    @Test
    public void testCountIterable() {
        assertEquals(2, N.count((Iterable<Integer>) integerList, i -> i % 2 == 0));
        assertEquals(3, N.count((Iterable<Integer>) integerList, i -> i > 2));
    }

    @Test
    public void testCountIterator() {
        assertEquals(5, N.count(integerList.iterator()));
        assertEquals(2, N.count(integerList.iterator(), i -> i > 3));
    }

    @Test
    public void testMergeArrays() {
        Integer[] a1 = { 1, 3, 5 };
        Integer[] a2 = { 2, 4, 6 };
        List<Integer> result = N.merge(a1, a2, Fn.f((x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeIterables() {
        List<Integer> a1 = Arrays.asList(1, 3, 5);
        List<Integer> a2 = Arrays.asList(2, 4, 6);
        List<Integer> result = N.merge(a1, a2, Fn.f((x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeMultipleIterables() {
        List<List<Integer>> lists = Arrays.asList(Arrays.asList(1, 4), Arrays.asList(2, 5), Arrays.asList(3, 6));
        List<Integer> result = N.merge(lists, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testZipArrays() {
        Integer[] a1 = { 1, 2, 3 };
        String[] a2 = { "a", "b", "c" };
        List<Pair<Integer, String>> result = N.zip(a1, a2, Fn.pair());
        assertEquals(3, result.size());
        assertEquals(Pair.of(1, "a"), result.get(0));
        assertEquals(Pair.of(2, "b"), result.get(1));
        assertEquals(Pair.of(3, "c"), result.get(2));
    }

    @Test
    public void testZipIterables() {
        List<Integer> a1 = Arrays.asList(1, 2, 3);
        List<String> a2 = Arrays.asList("a", "b", "c");
        List<String> result = N.zip(a1, a2, (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "2b", "3c"), result);
    }

    @Test
    public void testZipThreeArrays() {
        Integer[] a1 = { 1, 2 };
        String[] a2 = { "a", "b" };
        Double[] a3 = { 1.0, 2.0 };
        List<Triple<Integer, String, Double>> result = N.zip(a1, a2, a3, Fn.triple());
        assertEquals(2, result.size());
        assertEquals(Triple.of(1, "a", 1.0), result.get(0));
    }

    @Test
    public void testZipWithDefaults() {
        Integer[] a1 = { 1, 2, 3 };
        String[] a2 = { "a", "b" };
        List<Pair<Integer, String>> result = N.zip(a1, a2, 0, "z", Fn.pair());
        assertEquals(3, result.size());
        assertEquals(Pair.of(3, "z"), result.get(2));
    }

    @Test
    public void testZipToArray() {
        Integer[] a1 = { 1, 2, 3 };
        Integer[] a2 = { 10, 20, 30 };
        Integer[] result = N.zip(a1, a2, (x, y) -> x + y, Integer.class);
        assertArrayEquals(new Integer[] { 11, 22, 33 }, result);
    }

    @Test
    public void testUnzip() {
        List<Pair<Integer, String>> pairs = Arrays.asList(Pair.of(1, "a"), Pair.of(2, "b"), Pair.of(3, "c"));
        Pair<List<Integer>, List<String>> result = N.unzip(pairs, (p, out) -> {
            out.setLeft(p.left());
            out.setRight(p.right());
        });
        assertEquals(Arrays.asList(1, 2, 3), result.left());
        assertEquals(Arrays.asList("a", "b", "c"), result.right());
    }

    @Test
    public void testUnzipp() {
        List<Triple<Integer, String, Double>> triples = Arrays.asList(Triple.of(1, "a", 1.0), Triple.of(2, "b", 2.0));
        Triple<List<Integer>, List<String>, List<Double>> result = N.unzipp(triples, (t, out) -> {
            out.setLeft(t.left());
            out.setMiddle(t.middle());
            out.setRight(t.right());
        });
        assertEquals(Arrays.asList(1, 2), result.left());
        assertEquals(Arrays.asList("a", "b"), result.middle());
        assertEquals(Arrays.asList(1.0, 2.0), result.right());
    }

    @Test
    public void testGroupByArray() {
        Map<Integer, List<String>> result = N.groupBy(stringArray, String::length);
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("one", "two"), result.get(3));
        assertEquals(Arrays.asList("three"), result.get(5));
        assertEquals(Arrays.asList("four", "five"), result.get(4));
    }

    @Test
    public void testGroupByArrayWithRange() {
        Map<Integer, List<String>> result = N.groupBy(stringArray, 1, 4, String::length);
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("two"), result.get(3));
        assertEquals(Arrays.asList("three"), result.get(5));
        assertEquals(Arrays.asList("four"), result.get(4));
    }

    @Test
    public void testGroupByIterable() {
        Map<Character, List<String>> result = N.groupBy(stringList, s -> s.charAt(0));
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("one"), result.get('o'));
        assertEquals(Arrays.asList("two", "three"), result.get('t'));
        assertEquals(Arrays.asList("four", "five"), result.get('f'));
    }

    @Test
    public void testGroupByIterator() {
        Map<Boolean, List<Integer>> result = N.groupBy(integerList.iterator(), i -> i % 2 == 0);
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(2, 4), result.get(true));
        assertEquals(Arrays.asList(1, 3, 5), result.get(false));
    }

    @Test
    public void testGroupByWithValueExtractor() {
        Map<Integer, List<Character>> result = N.groupBy(stringList, String::length, s -> s.charAt(0));
        assertEquals(3, result.size());
        assertEquals(Arrays.asList('o', 't'), result.get(3));
        assertEquals(Arrays.asList('t'), result.get(5));
        assertEquals(Arrays.asList('f', 'f'), result.get(4));
    }

    @Test
    public void testGroupByWithCollector() {
        Map<Integer, Long> result = N.groupBy(stringList, String::length, Collectors.counting());
        assertEquals(3, result.size());
        assertEquals(Long.valueOf(2), result.get(3));
        assertEquals(Long.valueOf(1), result.get(5));
        assertEquals(Long.valueOf(2), result.get(4));
    }

    @Test
    public void testCountBy() {
        Map<Integer, Integer> result = N.countBy(stringList, String::length);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(2), result.get(3));
        assertEquals(Integer.valueOf(1), result.get(5));
        assertEquals(Integer.valueOf(2), result.get(4));
    }

    @Test
    public void testCountByIterator() {
        Map<Boolean, Integer> result = N.countBy(integerList.iterator(), i -> i % 2 == 0);
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(2), result.get(true));
        assertEquals(Integer.valueOf(3), result.get(false));
    }

    @Test
    public void testIterateArray() {
        ObjIterator<String> iter = N.iterate(stringArray);
        assertTrue(iter.hasNext());
        assertEquals("one", iter.next());
    }

    @Test
    public void testIterateArrayWithRange() {
        Iterator<String> iter = N.iterate(stringArray, 1, 3);
        assertTrue(iter.hasNext());
        assertEquals("two", iter.next());
        assertEquals("three", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterateMap() {
        Iterator<Map.Entry<String, Integer>> iter = N.iterate(stringIntMap);
        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void testIterateIterable() {
        Iterator<String> iter = N.iterate(stringList);
        assertTrue(iter.hasNext());
        assertEquals("one", iter.next());
    }

    @Test
    public void testIterateEach() {
        List<List<Integer>> lists = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6));
        List<Iterator<Integer>> iterators = N.iterateEach(lists);
        assertEquals(3, iterators.size());
        assertEquals(Integer.valueOf(1), iterators.get(0).next());
        assertEquals(Integer.valueOf(3), iterators.get(1).next());
        assertEquals(Integer.valueOf(5), iterators.get(2).next());
    }

    @Test
    public void testIterateAll() {
        List<List<Integer>> lists = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6));
        ObjIterator<Integer> iter = N.iterateAll(lists);
        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testDisjointArrays() {
        Integer[] a1 = { 1, 2, 3 };
        Integer[] a2 = { 4, 5, 6 };
        assertTrue(N.disjoint(a1, a2));

        Integer[] a3 = { 3, 4, 5 };
        assertFalse(N.disjoint(a1, a3));
    }

    @Test
    public void testDisjointCollections() {
        List<Integer> c1 = Arrays.asList(1, 2, 3);
        List<Integer> c2 = Arrays.asList(4, 5, 6);
        assertTrue(N.disjoint(c1, c2));

        Set<Integer> c3 = new HashSet<>(Arrays.asList(3, 4, 5));
        assertFalse(N.disjoint(c1, c3));
    }

    @Test
    public void testDisjointEmptyCollections() {
        assertTrue(N.disjoint(Collections.emptyList(), Collections.emptyList()));
        assertTrue(N.disjoint(integerList, Collections.emptyList()));
        assertTrue(N.disjoint(Collections.emptyList(), integerList));
    }

    private boolean contains(char[] array, char value) {
        for (char c : array) {
            if (c == value)
                return true;
        }
        return false;
    }

    private boolean contains(int[] array, int value) {
        for (int i : array) {
            if (i == value)
                return true;
        }
        return false;
    }

    @Test
    public void testNullHandling() {
        assertArrayEquals(new boolean[0], N.filter((boolean[]) null, b -> b));
        assertEquals(Collections.emptyList(), N.filter((String[]) null, s -> true));

        assertArrayEquals(new boolean[0], N.mapToBoolean((String[]) null, s -> true));
        assertEquals(Collections.emptyList(), N.map((String[]) null, s -> s));

        assertEquals(0, N.count((int[]) null, i -> true));
        assertEquals(0, N.count((Iterator<?>) null));

        assertArrayEquals(new boolean[0], N.distinct((boolean[]) null));
        assertEquals(Collections.emptyList(), N.distinct((String[]) null));
    }

    @Test
    public void testEmptyCollectionHandling() {
        String[] emptyArray = new String[0];
        assertEquals(Collections.emptyList(), N.filter(emptyArray, s -> true));
        assertEquals(Collections.emptyList(), N.map(emptyArray, s -> s));
        assertEquals(Collections.emptyList(), N.distinct(emptyArray));

        List<String> emptyList = Collections.emptyList();
        assertEquals(Collections.emptyList(), N.filter(emptyList, s -> true));
        assertEquals(Collections.emptyList(), N.map(emptyList, s -> s));
        assertEquals(Collections.emptyList(), N.distinct(emptyList));
    }

    @Test
    public void testInvalidRangeFromIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(intArray, -1, 3, i -> true));
    }

    @Test
    public void testInvalidRangeToIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(intArray, 0, 10, i -> true));
    }

    @Test
    public void testInvalidRangeFromGreaterThanTo() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.filter(intArray, 3, 2, i -> true));
    }

    @Test
    public void testNMatchNegativeAtLeast() {
        assertThrows(IllegalArgumentException.class, () -> N.nMatch(integerArray, -1, 2, i -> true));
    }

    @Test
    public void testNMatchNegativeAtMost() {
        assertThrows(IllegalArgumentException.class, () -> N.nMatch(integerArray, 1, -1, i -> true));
    }

    @Test
    public void testNMatchAtLeastGreaterThanAtMost() {
        assertThrows(IllegalArgumentException.class, () -> N.nMatch(integerArray, 3, 2, i -> true));
    }

    @Test
    public void testFilterWithEmptyArrays() {
        assertArrayEquals(new boolean[0], N.filter(emptyBooleanArray, b -> b));
        assertArrayEquals(new char[0], N.filter(emptyCharArray, c -> true));
        assertArrayEquals(new int[0], N.filter(emptyIntArray, i -> true));
        assertEquals(Collections.emptyList(), N.filter(emptyStringArray, s -> true));
    }

    @Test
    public void testFilterWithSingleElement() {
        assertArrayEquals(new boolean[] { true }, N.filter(singleBooleanArray, b -> b));
        assertArrayEquals(new boolean[0], N.filter(singleBooleanArray, b -> !b));
        assertArrayEquals(new int[] { 42 }, N.filter(singleIntArray, i -> i > 0));
        assertEquals(Arrays.asList("single"), N.filter(singleStringArray, s -> s != null));
    }

    @Test
    public void testFilterAllElementsMatch() {
        int[] allEven = { 2, 4, 6, 8, 10 };
        assertArrayEquals(allEven, N.filter(allEven, i -> i % 2 == 0));
    }

    @Test
    public void testFilterNoElementsMatch() {
        int[] allOdd = { 1, 3, 5, 7, 9 };
        assertArrayEquals(new int[0], N.filter(allOdd, i -> i % 2 == 0));
    }

    @Test
    public void testFilterWithNullElements() {
        List<Integer> result = N.filter(nullContainingArray, i -> i != null);
        assertEquals(Arrays.asList(1, 3, 5), result);

        result = N.filter(nullContainingArray, i -> i == null);
        assertEquals(Arrays.asList(null, null), result);
    }

    @Test
    public void testFilterLargeDataset() {
        int[] result = N.filter(largeIntArray, i -> i % 1000 == 0);
        assertEquals(10, result.length);
        assertEquals(0, result[0]);
        assertEquals(9000, result[9]);
    }

    @Test
    public void testFilterRangeEdgeCases() {
        int[] arr = { 1, 2, 3, 4, 5 };

        assertArrayEquals(new int[0], N.filter(arr, 2, 2, i -> true));

        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, N.filter(arr, 0, 5, i -> true));

        assertArrayEquals(new int[] { 3 }, N.filter(arr, 2, 3, i -> true));
    }

    @Test
    public void testMapWithComplexTransformations() {
        String[] words = { "hello", "world", "java", "test" };

        List<Character> firstChars = N.map(words, s -> Character.toUpperCase(s.charAt(0)));
        assertEquals(Arrays.asList('H', 'W', 'J', 'T'), firstChars);

        List<String> stats = N.map(words, s -> s + ":" + s.length());
        assertEquals(Arrays.asList("hello:5", "world:5", "java:4", "test:4"), stats);
    }

    @Test
    public void testMapToIntWithComplexFunctions() {
        String[] sentences = { "Hello world", "Java programming", "Unit testing" };

        int[] spaceCounts = N.mapToInt(sentences, s -> (int) s.chars().filter(c -> c == ' ').count());
        assertArrayEquals(new int[] { 1, 1, 1 }, spaceCounts);

        int[] hashCodes = N.mapToInt(sentences, String::hashCode);
        assertEquals(3, hashCodes.length);
    }

    @Test
    public void testMapPrimitiveConversions() {
        int[] ints = { 1, 2, 3 };
        assertArrayEquals(new long[] { 1L, 2L, 3L }, N.mapToLong(ints, i -> (long) i));
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, N.mapToDouble(ints, i -> (double) i), 0.001);

        long[] longs = { 100L, 200L, 300L };
        assertArrayEquals(new int[] { 100, 200, 300 }, N.mapToInt(longs, l -> (int) l));
        assertArrayEquals(new double[] { 100.0, 200.0, 300.0 }, N.mapToDouble(longs, l -> (double) l), 0.001);

        double[] doubles = { 1.5, 2.5, 3.5 };
        assertArrayEquals(new int[] { 1, 2, 3 }, N.mapToInt(doubles, d -> (int) d));
        assertArrayEquals(new long[] { 1L, 2L, 3L }, N.mapToLong(doubles, d -> (long) d));
    }

    @Test
    public void testMapWithNullHandling() {
        List<String> result = N.map(nullContainingArray, i -> i == null ? "NULL" : i.toString());
        assertEquals(Arrays.asList("1", "NULL", "3", "NULL", "5"), result);
    }

    @Test
    public void testFlatMapEmptyCollections() {
        String[] arr = { "", "a", "", "bc" };
        List<Character> result = N.flatMap(arr, s -> s.isEmpty() ? Collections.emptyList() : Arrays.asList(s.charAt(0)));
        assertEquals(Arrays.asList('a', 'b'), result);
    }

    @Test
    public void testFlatMapNestedStructures() {
        List<List<List<Integer>>> nested = Arrays.asList(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)),
                Arrays.asList(Arrays.asList(5, 6), Arrays.asList(7, 8)));

        List<Integer> result = N.flatMap(nested, Fn.identity(), Fn.identity());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), result);
    }

    @Test
    public void testFlatMapWithNullCollections() {
        List<List<String>> lists = Arrays.asList(Arrays.asList("a", "b"), null, Arrays.asList("c", "d"), null);

        List<String> result = N.flatMap(lists, list -> list == null ? Collections.emptyList() : list);
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    }

    @Test
    public void testDistinctAllDuplicates() {
        assertArrayEquals(new int[] { 1 }, N.distinct(duplicateIntArray));
        assertEquals(Arrays.asList("dup"), N.distinct(duplicateStringArray));
    }

    @Test
    public void testDistinctAlreadyDistinct() {
        int[] distinct = { 1, 2, 3, 4, 5 };
        assertArrayEquals(distinct, N.distinct(distinct));
    }

    @Test
    public void testDistinctWithNulls() {
        Integer[] withNulls = { 1, null, 2, null, 3, null, 1, 2, 3 };
        List<Integer> result = N.distinct(withNulls);
        assertEquals(4, result.size());
        assertTrue(result.contains(null));
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));
    }

    @Test
    public void testDistinctByComplexKeys() {
        class Person {
            String name;
            int age;

            Person(String name, int age) {
                this.name = name;
                this.age = age;
            }
        }

        Person[] people = { new Person("John", 25), new Person("Jane", 25), new Person("Bob", 30), new Person("Alice", 25) };

        List<Person> byAge = N.distinctBy(people, p -> p.age);
        assertEquals(2, byAge.size());

        List<Person> byNameLength = N.distinctBy(people, p -> p.name.length());
        assertEquals(3, byNameLength.size());
    }

    @Test
    public void testMatchWithEmptyPredicates() {
        assertTrue(N.allMatch(new Integer[] { 1, 2, 3 }, i -> true));
        assertTrue(N.anyMatch(new Integer[] { 1, 2, 3 }, i -> true));
        assertFalse(N.noneMatch(new Integer[] { 1, 2, 3 }, i -> true));

        assertFalse(N.allMatch(new Integer[] { 1, 2, 3 }, i -> false));
        assertFalse(N.anyMatch(new Integer[] { 1, 2, 3 }, i -> false));
        assertTrue(N.noneMatch(new Integer[] { 1, 2, 3 }, i -> false));
    }

    @Test
    public void testNMatchEdgeCases() {
        Integer[] arr = { 1, 2, 3, 4, 5 };

        assertTrue(N.nMatch(arr, 2, 2, i -> i % 2 == 0));

        assertTrue(N.nMatch(arr, 0, 5, i -> false));

        assertFalse(N.nMatch(arr, 10, 10, i -> true));
    }

    @Test
    public void testCountAllPrimitiveTypes() {
        assertEquals(2, N.count(new boolean[] { true, false, true }, b -> b));
        assertEquals(3, N.count(new char[] { 'a', 'b', 'c' }, c -> c >= 'a'));
        assertEquals(2, N.count(new byte[] { 1, 2, 3 }, b -> b % 2 == 1));
        assertEquals(3, N.count(new short[] { 10, 20, 30 }, s -> s >= 10));
        assertEquals(2, N.count(new float[] { 1.5f, 2.5f, 3.5f }, f -> f < 3.0f));
        assertEquals(1, N.count(new double[] { 1.1, 2.2, 3.3 }, d -> d > 3.0));
    }

    @Test
    public void testCountWithComplexPredicates() {
        String[] words = { "hello", "world", "java", "programming", "test" };

        assertEquals(0, N.count(words, s -> s.equals(new StringBuilder(s).reverse().toString())));

        assertEquals(2, N.count(words, s -> s.contains("a")));

        assertEquals(2, N.count(words, s -> s.length() % 2 == 0));
    }

    @Test
    public void testMergeWithEmptyArrays() {
        Integer[] empty = new Integer[0];
        Integer[] nonEmpty = { 1, 2, 3 };

        List<Integer> result1 = N.merge(empty, nonEmpty, Fn.f((a, b) -> MergeResult.TAKE_FIRST));
        assertEquals(Arrays.asList(1, 2, 3), result1);

        List<Integer> result2 = N.merge(nonEmpty, empty, Fn.f((a, b) -> MergeResult.TAKE_FIRST));
        assertEquals(Arrays.asList(1, 2, 3), result2);

        List<Integer> result3 = N.merge(empty, empty, Fn.f((a, b) -> MergeResult.TAKE_FIRST));
        assertEquals(Collections.emptyList(), result3);
    }

    @Test
    public void testMergeComplexDecisions() {
        String[] arr1 = { "apple", "cherry", "elderberry" };
        String[] arr2 = { "banana", "date", "fig" };

        List<String> result = N.merge(arr1, arr2, Fn.f((a, b) -> a.compareTo(b) < 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND));
        assertEquals(Arrays.asList("apple", "banana", "cherry", "date", "elderberry", "fig"), result);
    }

    @Test
    public void testMergeMultipleCollections() {
        List<List<Integer>> lists = Arrays.asList(Arrays.asList(1, 5, 9), Arrays.asList(2, 6, 10), Arrays.asList(3, 7, 11), Arrays.asList(4, 8, 12));

        List<Integer> result = N.merge(lists, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), result);
    }

    @Test
    public void testZipDifferentLengths() {
        Integer[] short1 = { 1, 2 };
        String[] long1 = { "a", "b", "c", "d" };

        List<Pair<Integer, String>> result = N.zip(short1, long1, Fn.pair());
        assertEquals(2, result.size());
        assertEquals(Pair.of(1, "a"), result.get(0));
        assertEquals(Pair.of(2, "b"), result.get(1));
    }

    @Test
    public void testZipWithTransformation() {
        Double[] prices = { 10.5, 20.0, 15.75 };
        Integer[] quantities = { 2, 3, 1 };

        List<Double> totals = N.zip(prices, quantities, (p, q) -> p * q);
        assertEquals(Arrays.asList(21.0, 60.0, 15.75), totals);
    }

    @Test
    public void testZipThreeWithDefaults() {
        Integer[] a1 = { 1, 2 };
        String[] a2 = { "a" };
        Double[] a3 = { 1.0, 2.0, 3.0 };

        List<String> result = N.zip(a1, a2, a3, 0, "z", 0.0, (i, s, d) -> i + s + d);
        assertEquals(Arrays.asList("1a1.0", "2z2.0", "0z3.0"), result);
    }

    @Test
    public void testUnzipEmptyCollection() {
        List<Pair<String, Integer>> empty = Collections.emptyList();
        Pair<List<String>, List<Integer>> result = N.unzip(empty, (p, out) -> {
            out.setLeft(p.left());
            out.setRight(p.right());
        });

        assertTrue(result.left().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    @Test
    public void testUnzipWithTransformation() {
        List<String> items = Arrays.asList("a:1", "b:2", "c:3");
        Pair<List<String>, List<Integer>> result = N.unzip(items, (item, out) -> {
            String[] parts = item.split(":");
            out.setLeft(parts[0]);
            out.setRight(Integer.parseInt(parts[1]));
        });

        assertEquals(Arrays.asList("a", "b", "c"), result.left());
        assertEquals(Arrays.asList(1, 2, 3), result.right());
    }

    @Test
    public void testGroupByEmptyCollection() {
        Map<String, List<String>> result = N.groupBy(Collections.<String> emptyList(), s -> s);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGroupByAllSameGroup() {
        String[] allSame = { "aa", "bb", "cc", "dd" };
        Map<Integer, List<String>> result = N.groupBy(allSame, String::length);
        assertEquals(1, result.size());
        assertEquals(4, result.get(2).size());
    }

    @Test
    public void testGroupByWithNullKeys() {
        String[] words = { "one", null, "two", null, "three" };
        Map<Integer, List<String>> result = N.groupBy(words, s -> s == null ? null : s.length());

        assertTrue(result.containsKey(null));
        assertEquals(Arrays.asList(null, null), result.get(null));
        assertEquals(Arrays.asList("one", "two"), result.get(3));
        assertEquals(Arrays.asList("three"), result.get(5));
    }

    @Test
    public void testGroupByWithComplexCollector() {
        Integer[] numbers = { 1, 2, 3, 4, 5, 6 };
        Map<Boolean, String> result = N.groupBy(Arrays.asList(numbers), n -> n % 2 == 0, Collectors.mapping(Object::toString, Collectors.joining(",")));

        assertEquals("2,4,6", result.get(true));
        assertEquals("1,3,5", result.get(false));
    }

    @Test
    public void testCountByWithTransformations() {
        String[] words = { "Hello", "World", "JAVA", "test", "CODE" };

        Map<Boolean, Integer> byCase = N.countBy(Arrays.asList(words), s -> s.equals(s.toUpperCase()));
        assertEquals(Integer.valueOf(2), byCase.get(true));
        assertEquals(Integer.valueOf(3), byCase.get(false));
    }

    @Test
    public void testIterateWithModification() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Iterator<String> iter = N.iterate(list);

        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());

        list.add("d");

        assertThrows(ConcurrentModificationException.class, () -> iter.next());
    }

    @Test
    public void testIterateAllEmpty() {
        List<List<String>> empty = Arrays.asList(Collections.<String> emptyList(), Collections.<String> emptyList());

        ObjIterator<String> iter = N.iterateAll(empty);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterateAllMixed() {
        List<List<Integer>> mixed = Arrays.asList(Collections.<Integer> emptyList(), Arrays.asList(1, 2), Collections.<Integer> emptyList(),
                Arrays.asList(3, 4, 5), Collections.<Integer> emptyList());

        ObjIterator<Integer> iter = N.iterateAll(mixed);
        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testDisjointWithDuplicates() {
        Integer[] a1 = { 1, 1, 2, 2, 3, 3 };
        Integer[] a2 = { 4, 4, 5, 5, 6, 6 };
        assertTrue(N.disjoint(a1, a2));

        Integer[] a3 = { 3, 3, 4, 4, 5, 5 };
        assertFalse(N.disjoint(a1, a3));
    }

    @Test
    public void testDisjointWithNulls() {
        Object[] a1 = { 1, null, 3 };
        Object[] a2 = { 2, null, 4 };
        assertFalse(N.disjoint(a1, a2));

        Object[] a3 = { 2, 4, 6 };
        assertTrue(N.disjoint(a1, a3));
    }

    @Test
    public void testDisjointPerformance() {
        Set<Integer> bigSet = new HashSet<>();
        List<Integer> smallList = new ArrayList<>();

        for (int i = 0; i < 10000; i++) {
            bigSet.add(i);
        }

        for (int i = 20000; i < 20010; i++) {
            smallList.add(i);
        }

        assertTrue(N.disjoint(bigSet, smallList));
        assertTrue(N.disjoint(smallList, bigSet));
    }

    @Test
    public void testLargeDatasetOperations() {
        int[] filtered = N.filter(largeIntArray, i -> i % 100 == 0);
        assertEquals(100, filtered.length);

        boolean[] mapped = N.mapToBoolean(largeIntList, i -> i % 2 == 0);
        assertEquals(10000, mapped.length);

        int count = N.count(largeIntArray, i -> i < 5000);
        assertEquals(5000, count);

        int[] manyDupes = new int[10000];
        Arrays.fill(manyDupes, 0, 5000, 1);
        Arrays.fill(manyDupes, 5000, 10000, 2);
        int[] distinct = N.distinct(manyDupes);
        assertEquals(2, distinct.length);
    }

    @Test
    public void testComplexPredicates() {
        class Product {
            String name;
            double price;
            String category;
            boolean inStock;

            Product(String name, double price, String category, boolean inStock) {
                this.name = name;
                this.price = price;
                this.category = category;
                this.inStock = inStock;
            }
        }

        Product[] products = { new Product("Laptop", 999.99, "Electronics", true), new Product("Mouse", 29.99, "Electronics", true),
                new Product("Desk", 299.99, "Furniture", false), new Product("Chair", 199.99, "Furniture", true),
                new Product("Monitor", 399.99, "Electronics", false) };

        List<Product> result = N.filter(products, p -> p.inStock && p.price < 500 && "Electronics".equals(p.category));
        assertEquals(1, result.size());
        assertEquals("Mouse", result.get(0).name);

        Map<String, List<Product>> byCategory = N.groupBy(products, p -> p.category);
        assertEquals(2, byCategory.size());
        assertEquals(3, byCategory.get("Electronics").size());
        assertEquals(2, byCategory.get("Furniture").size());

        Map<Boolean, Integer> stockCount = N.countBy(Arrays.asList(products), p -> p.inStock);
        assertEquals(Integer.valueOf(3), stockCount.get(true));
        assertEquals(Integer.valueOf(2), stockCount.get(false));
    }

    @Test
    public void testStatelessOperations() {
        int[] arr = { 1, 2, 3, 4, 5 };

        int[] result1 = N.filter(arr, i -> i > 2);
        int[] result2 = N.filter(arr, i -> i > 2);
        assertArrayEquals(result1, result2);

        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, arr);
    }

    @Test
    public void testSpecialNumberCases() {
        float[] floats = { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0f, -0.0f };
        float[] filtered = N.filter(floats, f -> !Float.isNaN(f) && Float.isFinite(f));
        assertArrayEquals(new float[] { 0.0f, -0.0f }, filtered, 0.001f);

        double[] doubles = { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, -0.0 };
        int count = N.count(doubles, d -> Double.isInfinite(d));
        assertEquals(2, count);
    }

    @Test
    public void testBoundaryValues() {
        byte[] maxBytes = new byte[Byte.MAX_VALUE];
        Arrays.fill(maxBytes, (byte) 1);
        assertEquals(Byte.MAX_VALUE, N.count(maxBytes, b -> b == 1));

        int[] minValues = { Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE };
        int[] positives = N.filter(minValues, i -> i > 0);
        assertArrayEquals(new int[] { 1, Integer.MAX_VALUE }, positives);
    }

    @Test
    public void testFunctionalComposition() {
        String[] words = { "hello", "world", "java", "programming" };

        List<String> filtered = N.filter(words, s -> s.length() > 4);
        List<Integer> lengths = N.map(filtered.toArray(new String[0]), String::length);
        assertEquals(Arrays.asList(5, 5, 11), lengths);

        int[] wordLengths = N.mapToInt(words, String::length);
        int[] longLengths = N.filter(wordLengths, len -> len > 4);
        assertArrayEquals(new int[] { 5, 5, 11 }, longLengths);
    }

    @Test
    public void testGroupByWithCustomCollectors() {
        String[] words = { "one", "two", "three", "four", "five", "six" };

        Map<Integer, String> joined = N.groupBy(Arrays.asList(words), String::length, Collectors.joining("-"));

        assertEquals("one-two-six", joined.get(3));
        assertEquals("four-five", joined.get(4));
        assertEquals("three", joined.get(5));

        Map<Integer, Optional<String>> firstByLength = N.groupBy(Arrays.asList(words), String::length, Collectors.reducing((a, b) -> a));

        assertEquals("one", firstByLength.get(3).get());
        assertEquals("four", firstByLength.get(4).get());
        assertEquals("three", firstByLength.get(5).get());
    }

    @Test
    public void testIteratorExhaustion() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        Iterator<Integer> iter = list.iterator();

        assertEquals(Integer.valueOf(1), iter.next());

        List<Integer> result = N.filter(iter, i -> i > 1);
        assertEquals(Arrays.asList(2, 3), result);

        assertFalse(iter.hasNext());
    }

    @Test
    public void testComplexMapOperations() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("evens", Arrays.asList(2, 4, 6));
        map.put("odds", Arrays.asList(1, 3, 5));
        map.put("mixed", Arrays.asList(1, 2, 3));

        Iterator<Map.Entry<String, List<Integer>>> iter = N.iterate(map);
        int entryCount = 0;
        while (iter.hasNext()) {
            iter.next();
            entryCount++;
        }
        assertEquals(3, entryCount);
    }
}
