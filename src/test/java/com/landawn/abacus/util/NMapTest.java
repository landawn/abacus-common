package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class NMapTest extends NTestSupport {

    @Test
    public void testMapToPrimitives() {
        assertArrayEquals(new boolean[] { false, false, true, true, true }, N.mapToBoolean(stringArray, s -> s.length() > 3));
        assertArrayEquals(new boolean[] { false, true, true }, N.mapToBoolean(stringArray, 1, 4, s -> s.length() > 3));
        assertArrayEquals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.mapToBoolean((String[]) null, Boolean::parseBoolean));
        assertArrayEquals(new boolean[] { false, true },
                N.mapToBoolean(new LinkedList<>(Arrays.asList("true", "false", "TRUE", "no")), 1, 3, Boolean::parseBoolean));

        assertArrayEquals(new char[] { 'o', 't', 't', 'f', 'f' }, N.mapToChar(stringArray, s -> s.charAt(0)));
        assertArrayEquals(new char[] { 't', 't' }, N.mapToChar(stringArray, 1, 3, s -> s.charAt(0)));
        assertArrayEquals(new char[] { 'h', 'w' }, N.mapToChar(Arrays.asList("hello", "world"), s -> s.charAt(0)));

        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, N.mapToByte(integerArray, Integer::byteValue));
        assertArrayEquals(new byte[] { 3, 4, 5 }, N.mapToByte(integerArray, 2, 5, Integer::byteValue));
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, N.mapToShort(integerArray, Integer::shortValue));
        assertArrayEquals(new short[] { 2, 3, 4 }, N.mapToShort(integerArray, 1, 4, Integer::shortValue));

        assertArrayEquals(new int[] { 3, 3, 5, 4, 4 }, N.mapToInt(stringArray, String::length));
        assertArrayEquals(new int[] { 3, 3, 5 }, N.mapToInt(stringArray, 0, 3, String::length));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.mapToInt((String[]) null, String::length));
        assertArrayEquals(new int[] { 6, 6 }, N.mapToInt(new LinkedList<>(Arrays.asList("apple", "banana", "cherry", "date")), 1, 3, String::length));
        assertArrayEquals(new int[] { 1, (int) 10000000000L, 3 }, N.mapToInt(new long[] { 1L, 10000000000L, 3L }, l -> (int) l));
        assertArrayEquals(new int[] { 1, 2, 3 }, N.mapToInt(new double[] { 1.1, 2.9, 3.5 }, d -> (int) d));

        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, N.mapToLong(integerArray, Integer::longValue));
        assertArrayEquals(new long[] { 1L, 2L, 3L }, N.mapToLong(new int[] { 1, 2, 3 }, i -> (long) i));
        assertArrayEquals(CommonUtil.EMPTY_LONG_ARRAY, N.mapToLong((int[]) null, i -> (long) i));

        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, N.mapToFloat(integerArray, Integer::floatValue), DELTAf);
        assertArrayEquals(new float[] { 2.5f, 3.0f }, N.mapToFloat(new String[] { "1.0", "2.5", "3.0", "4.5" }, 1, 3, Float::parseFloat), DELTAf);

        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, N.mapToDouble(integerArray, Integer::doubleValue), DELTA);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, N.mapToDouble(new int[] { 1, 2, 3 }, i -> (double) i), DELTA);
        assertArrayEquals(CommonUtil.EMPTY_DOUBLE_ARRAY, N.mapToDouble((long[]) null, l -> (double) l));
    }

    @Test
    public void testMap() {
        assertEquals(Arrays.asList(3, 3, 5, 4, 4), N.map(stringArray, String::length));
        assertEquals(new HashSet<>(Arrays.asList(3, 4, 5)), N.map(stringArray, String::length, size -> new HashSet<>()));
        assertEquals(Arrays.asList(3, 5, 4), N.map(stringArray, 1, 4, String::length));
        assertEquals(Set.of(3, 5), N.map(new String[] { "one", "two", "three", "two" }, 1, 4, String::length, HashSet::new));
        assertEquals(Set.of("1", "2", "3"), N.map((Iterable<Integer>) Arrays.asList(1, 2, 1, 3), String::valueOf, HashSet::new));
        assertEquals(Set.of("1", "2", "3"), N.map(Arrays.asList(1, 2, 1, 3).iterator(), String::valueOf, HashSet::new));
        assertTrue(N.map((String[]) null, String::length).isEmpty());
        assertTrue(N.map(new String[0], String::length).isEmpty());
        assertEquals(Arrays.asList(3, 3, 5), N.map(stringList, 0, 3, String::length));
    }

    @Test
    public void testMapAndFilter() {
        assertEquals(Arrays.asList("THREE", "FOUR", "FIVE"), N.mapAndFilter(stringList, String::toUpperCase, s -> s.length() > 3));
        assertEquals(new HashSet<>(Arrays.asList("THREE", "FOUR", "FIVE")), N.mapAndFilter(stringList, String::toUpperCase, s -> s.length() > 3, HashSet::new));
        assertTrue(N.mapAndFilter((Iterable<String>) null, String::toUpperCase, s -> true).isEmpty());
    }
}
