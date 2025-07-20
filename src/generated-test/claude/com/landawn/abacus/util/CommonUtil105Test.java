package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.SortedMap;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;

public class CommonUtil105Test extends TestBase {

    // Test empty collection methods

    @Test
    public void testEmptyMap() {
        Map<String, Integer> map = N.emptyMap();

        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        // Test immutability
        assertThrows(UnsupportedOperationException.class, () -> {
            map.put("key", 1);
        });
    }

    @Test
    public void testEmptyMapReturnsSameInstance() {
        Map<String, String> map1 = N.emptyMap();
        Map<Integer, Integer> map2 = N.emptyMap();

        // Should return the same instance due to type erasure
        assertSame(map1, map2);
    }

    @Test
    public void testEmptySortedMap() {
        SortedMap<String, Integer> map = N.emptySortedMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> map.put("key", 1));
    }

    @Test
    public void testEmptyNavigableMap() {
        NavigableMap<String, Integer> map = N.emptyNavigableMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> map.put("key", 1));
    }

    @Test
    public void testEmptyIterator() {
        Iterator<String> iter = N.emptyIterator();
        assertNotNull(iter);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testEmptyListIterator() {
        ListIterator<String> iter = N.emptyListIterator();
        assertNotNull(iter);
        assertFalse(iter.hasNext());
        assertFalse(iter.hasPrevious());
        assertThrows(NoSuchElementException.class, () -> iter.next());
        assertThrows(NoSuchElementException.class, () -> iter.previous());
    }

    @Test
    public void testEmptyInputStream() throws IOException {
        InputStream stream = N.emptyInputStream();
        assertNotNull(stream);
        assertEquals(-1, stream.read());
        assertEquals(0, stream.available());
    }

    @Test
    public void testEmptyDataSet() {
        DataSet ds = N.emptyDataSet();
        assertNotNull(ds);
        assertTrue(ds.isEmpty());
    }

    // Test compare methods for primitive types
    @Test
    public void testCompareBoolean() {
        assertEquals(0, N.compare(true, true));
        assertEquals(0, N.compare(false, false));
        assertEquals(1, N.compare(true, false));
        assertEquals(-1, N.compare(false, true));
    }

    @Test
    public void testCompareChar() {
        assertEquals(0, N.compare('a', 'a'));
        assertEquals(-1, N.compare('a', 'b'));
        assertEquals(1, N.compare('b', 'a'));
    }

    @Test
    public void testCompareByte() {
        assertEquals(0, N.compare((byte) 10, (byte) 10));
        assertTrue(N.compare((byte) 5, (byte) 10) < 0);
        assertTrue(N.compare((byte) 10, (byte) 5) > 0);
    }

    @Test
    public void testCompareUnsignedByte() {
        assertEquals(0, N.compareUnsigned((byte) 10, (byte) 10));
        assertTrue(N.compareUnsigned((byte) 5, (byte) 10) < 0);
        assertTrue(N.compareUnsigned((byte) -1, (byte) 127) > 0);
    }

    @Test
    public void testCompareShort() {
        assertEquals(0, N.compare((short) 100, (short) 100));
        assertTrue(N.compare((short) 50, (short) 100) < 0);
        assertTrue(N.compare((short) 100, (short) 50) > 0);
    }

    @Test
    public void testCompareUnsignedShort() {
        assertEquals(0, N.compareUnsigned((short) 100, (short) 100));
        assertTrue(N.compareUnsigned((short) 50, (short) 100) < 0);
        assertTrue(N.compareUnsigned((short) -1, (short) 32767) > 0);
    }

    @Test
    public void testCompareInt() {
        assertEquals(0, N.compare(100, 100));
        assertTrue(N.compare(50, 100) < 0);
        assertTrue(N.compare(100, 50) > 0);
    }

    @Test
    public void testCompareUnsignedInt() {
        assertEquals(0, N.compareUnsigned(100, 100));
        assertTrue(N.compareUnsigned(50, 100) < 0);
        assertTrue(N.compareUnsigned(-1, Integer.MAX_VALUE) > 0);
    }

    @Test
    public void testCompareLong() {
        assertEquals(0, N.compare(100L, 100L));
        assertTrue(N.compare(50L, 100L) < 0);
        assertTrue(N.compare(100L, 50L) > 0);
    }

    @Test
    public void testCompareUnsignedLong() {
        assertEquals(0, N.compareUnsigned(100L, 100L));
        assertEquals(-1, N.compareUnsigned(50L, 100L));
        assertEquals(1, N.compareUnsigned(-1L, Long.MAX_VALUE));
    }

    @Test
    public void testCompareFloat() {
        assertEquals(0, N.compare(1.5f, 1.5f));
        assertEquals(-1, N.compare(1.0f, 1.5f));
        assertEquals(1, N.compare(1.5f, 1.0f));
        assertEquals(1, N.compare(Float.NaN, 1.0f));
    }

    @Test
    public void testCompareDouble() {
        assertEquals(0, N.compare(1.5, 1.5));
        assertEquals(-1, N.compare(1.0, 1.5));
        assertEquals(1, N.compare(1.5, 1.0));
        assertEquals(1, N.compare(Double.NaN, 1.0));
    }

    // Test compare methods for objects
    @Test
    public void testCompareComparable() {
        assertEquals(0, N.compare("abc", "abc"));
        assertTrue(N.compare("abc", "def") < 0);
        assertTrue(N.compare("def", "abc") > 0);
        assertTrue(N.compare(null, "abc") < 0);
        assertTrue(N.compare("abc", null) > 0);
        assertEquals(0, N.compare((String) null, null));
    }

    @Test
    public void testCompareWithComparator() {
        Comparator<String> comp = String.CASE_INSENSITIVE_ORDER;
        assertEquals(0, N.compare("ABC", "abc", comp));
        assertTrue(N.compare("abc", "def", comp) < 0);
        assertTrue(N.compare("def", "abc", comp) > 0);

        // Test with null comparator
        assertEquals(0, N.compare("abc", "abc", null));
    }

    @Test
    public void testCompareMultiplePairs2() {
        assertEquals(0, N.compare("a", "a", 1, 1));
        assertEquals(-1, N.compare("a", "b", 1, 1));
        assertEquals(1, N.compare("b", "a", 1, 1));
        assertEquals(-1, N.compare("a", "a", 1, 2));
        assertEquals(1, N.compare("a", "a", 2, 1));
    }

    @Test
    public void testCompareMultiplePairs3() {
        assertEquals(0, N.compare("a", "a", 1, 1, 5L, 5L));
        assertEquals(-1, N.compare("a", "b", 1, 1, 5L, 5L));
        assertEquals(1, N.compare("a", "a", 2, 1, 5L, 5L));
        assertEquals(-1, N.compare("a", "a", 1, 1, 5L, 6L));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCompareMultiplePairs4() {
        assertEquals(0, N.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0));
        assertEquals(-1, N.compare("a", "b", 1, 1, 5L, 5L, 1.0, 1.0));
        assertEquals(-1, N.compare("a", "a", 1, 1, 5L, 5L, 1.0, 2.0));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCompareMultiplePairs5() {
        assertEquals(0, N.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x"));
        assertEquals(1, N.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "y", "x"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCompareMultiplePairs6() {
        assertEquals(0, N.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x", true, true));
        assertEquals(-1, N.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x", false, true));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCompareMultiplePairs7() {
        assertEquals(0, N.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x", true, true, 'z', 'z'));
        assertEquals(1, N.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x", true, true, 'z', 'y'));
    }

    // Test array comparison methods
    @Test
    public void testCompareBooleanArrays() {
        boolean[] a1 = { true, false, true };
        boolean[] a2 = { true, false, true };
        boolean[] a3 = { true, true, false };
        boolean[] a4 = { true, false };

        assertEquals(0, N.compare(a1, a2));
        assertEquals(-1, N.compare(a1, a3));
        assertEquals(1, N.compare(a1, a4));
        assertEquals(-1, N.compare(null, a1));
        assertEquals(1, N.compare(a1, null));
        assertEquals(0, N.compare((boolean[]) null, (boolean[]) null));
        assertEquals(-1, N.compare(new boolean[] {}, a1));
    }

    @Test
    public void testCompareBooleanArraysWithRange() {
        boolean[] a1 = { true, false, true, false };
        boolean[] a2 = { false, false, true, true };

        assertEquals(0, N.compare(a1, 1, a2, 1, 2));
        assertTrue(N.compare(a1, 0, a2, 0, 2) > 0);
        assertEquals(0, N.compare(a1, 0, a1, 0, 2));

        assertThrows(IllegalArgumentException.class, () -> N.compare(a1, 0, a2, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.compare(a1, 0, a2, 0, 10));
    }

    @Test
    public void testCompareCharArrays() {
        char[] a1 = { 'a', 'b', 'c' };
        char[] a2 = { 'a', 'b', 'c' };
        char[] a3 = { 'a', 'c', 'b' };

        assertEquals(0, N.compare(a1, a2));
        assertEquals(-1, N.compare(a1, a3));
        assertEquals(-1, N.compare(null, a1));
        assertEquals(1, N.compare(a1, null));
    }

    @Test
    public void testCompareCharArraysWithRange() {
        char[] a1 = { 'a', 'b', 'c', 'd' };
        char[] a2 = { 'x', 'b', 'c', 'y' };

        assertEquals(0, N.compare(a1, 1, a2, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> N.compare(a1, 0, a2, 0, -1));
    }

    @Test
    public void testCompareByteArrays() {
        byte[] a1 = { 1, 2, 3 };
        byte[] a2 = { 1, 2, 3 };
        byte[] a3 = { 1, 3, 2 };

        assertEquals(0, N.compare(a1, a2));
        assertEquals(-1, N.compare(a1, a3));
    }

    @Test
    public void testCompareByteArraysWithRange() {
        byte[] a1 = { 1, 2, 3, 4 };
        byte[] a2 = { 5, 2, 3, 6 };

        assertEquals(0, N.compare(a1, 1, a2, 1, 2));
    }

    @Test
    public void testCompareUnsignedByteArrays() {
        byte[] a1 = { 1, 2, (byte) 255 };
        byte[] a2 = { 1, 2, 3 };

        assertTrue(N.compareUnsigned(a1, a2) > 0);
    }

    @Test
    public void testCompareUnsignedByteArraysWithRange() {
        byte[] a1 = { 1, 2, (byte) 255, 4 };
        byte[] a2 = { 5, 2, 3, 6 };

        assertTrue(N.compareUnsigned(a1, 2, a2, 2, 1) > 0);
    }

    @Test
    public void testCompareShortArrays() {
        short[] a1 = { 10, 20, 30 };
        short[] a2 = { 10, 20, 30 };
        short[] a3 = { 10, 30, 20 };

        assertEquals(0, N.compare(a1, a2));
        assertEquals(-1, N.compare(a1, a3));
    }

    @Test
    public void testCompareIntArrays() {
        int[] a1 = { 100, 200, 300 };
        int[] a2 = { 100, 200, 300 };
        int[] a3 = { 100, 300, 200 };

        assertEquals(0, N.compare(a1, a2));
        assertEquals(-1, N.compare(a1, a3));
    }

    @Test
    public void testCompareLongArrays() {
        long[] a1 = { 1000L, 2000L, 3000L };
        long[] a2 = { 1000L, 2000L, 3000L };
        long[] a3 = { 1000L, 3000L, 2000L };

        assertEquals(0, N.compare(a1, a2));
        assertEquals(-1, N.compare(a1, a3));
    }

    @Test
    public void testCompareFloatArrays() {
        float[] a1 = { 1.0f, 2.0f, 3.0f };
        float[] a2 = { 1.0f, 2.0f, 3.0f };
        float[] a3 = { 1.0f, 3.0f, 2.0f };

        assertEquals(0, N.compare(a1, a2));
        assertEquals(-1, N.compare(a1, a3));
    }

    @Test
    public void testCompareDoubleArrays() {
        double[] a1 = { 1.0, 2.0, 3.0 };
        double[] a2 = { 1.0, 2.0, 3.0 };
        double[] a3 = { 1.0, 3.0, 2.0 };

        assertEquals(0, N.compare(a1, a2));
        assertEquals(-1, N.compare(a1, a3));
    }

    @Test
    public void testCompareObjectArrays() {
        String[] a1 = { "a", "b", "c" };
        String[] a2 = { "a", "b", "c" };
        String[] a3 = { "a", "c", "b" };

        assertEquals(0, N.compare(a1, a2));
        assertEquals(-1, N.compare(a1, a3));
        assertEquals(-1, N.compare(null, a1));
        assertEquals(1, N.compare(a1, null));
    }

    @Test
    public void testCompareObjectArraysWithRange() {
        String[] a1 = { "x", "b", "c", "y" };
        String[] a2 = { "z", "b", "c", "w" };

        assertEquals(0, N.compare(a1, 1, a2, 1, 2));
    }

    @Test
    public void testCompareObjectArraysWithComparator() {
        String[] a1 = { "ABC", "DEF" };
        String[] a2 = { "abc", "def" };

        assertEquals(0, N.compare(a1, a2, String.CASE_INSENSITIVE_ORDER));
        assertTrue(N.compare(a1, a2, (Comparator<String>) null) < 0);
    }

    @Test
    public void testCompareArraysWithRange_01() {
        {
            byte[] a1 = { 1, 2, 3, 4 };
            byte[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, N.compare(a1, 1, a2, 1, 2));
        }
        {
            short[] a1 = { 1, 2, 3, 4 };
            short[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, N.compare(a1, 1, a2, 1, 2));
        }
        {

            int[] a1 = { 1, 2, 3, 4 };
            int[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, N.compare(a1, 1, a2, 1, 2));
        }
        {

            long[] a1 = { 1, 2, 3, 4 };
            long[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, N.compare(a1, 1, a2, 1, 2));
        }
        {

            float[] a1 = { 1, 2, 3, 4 };
            float[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, N.compare(a1, 1, a2, 1, 2));
        }
        {

            double[] a1 = { 1, 2, 3, 4 };
            double[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, N.compare(a1, 1, a2, 1, 2));
        }
    }

    @Test
    public void testCompareCollections() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        List<String> list3 = Arrays.asList("a", "c", "b");

        assertEquals(0, N.compare(list1, 0, list2, 0, 3));
        assertEquals(-1, N.compare(list1, 1, list3, 1, 2));
    }

    @Test
    public void testCompareIterables() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        List<String> list3 = Arrays.asList("a", "c", "b");

        assertEquals(0, N.compare((Iterable<String>) list1, (Iterable<String>) list2));
        assertEquals(-1, N.compare((Iterable<String>) list1, (Iterable<String>) list3));
        assertEquals(-1, N.compare((Iterable<String>) null, (Iterable<String>) list1));
        assertEquals(1, N.compare((Iterable<String>) list1, (Iterable<String>) null));
    }

    @Test
    public void testCompareIterators() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        List<String> list3 = Arrays.asList("a", "c", "b");

        assertEquals(0, N.compare(list1.iterator(), list2.iterator()));
        assertEquals(-1, N.compare(list1.iterator(), list3.iterator()));
        assertEquals(-1, N.compare(null, list1.iterator()));
        assertEquals(1, N.compare(list1.iterator(), null));
    }

    @Test
    public void testCompareIgnoreCase() {
        assertEquals(0, N.compareIgnoreCase("ABC", "abc"));
        assertTrue(N.compareIgnoreCase("abc", "def") < 0);
        assertTrue(N.compareIgnoreCase("def", "abc") > 0);
        assertTrue(N.compareIgnoreCase(null, "abc") < 0);
        assertTrue(N.compareIgnoreCase("abc", null) > 0);
        assertEquals(0, N.compareIgnoreCase((String) null, null));
    }

    @Test
    public void testCompareIgnoreCaseArrays() {
        String[] a1 = { "ABC", "DEF" };
        String[] a2 = { "abc", "def" };
        String[] a3 = { "abc", "xyz" };

        assertEquals(0, N.compareIgnoreCase(a1, a2));
        assertTrue(N.compareIgnoreCase(a1, a3) < 0);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCompareByProps() {
        // This test would require proper bean classes with properties
        // Skipping for now as it requires external dependencies
    }

    // Test comparison utility methods
    @Test
    public void testLessThan() {
        assertTrue(N.lessThan(1, 2));
        assertFalse(N.lessThan(2, 1));
        assertFalse(N.lessThan(1, 1));
        assertTrue(N.lessThan(null, 1));
        assertFalse(N.lessThan(1, null));
    }

    @Test
    public void testLessThanWithComparator() {
        assertTrue(N.lessThan("abc", "def", String.CASE_INSENSITIVE_ORDER));
        assertFalse(N.lessThan("DEF", "abc", String.CASE_INSENSITIVE_ORDER));
    }

    @Test
    public void testLessEqual() {
        assertTrue(N.lessEqual(1, 2));
        assertFalse(N.lessEqual(2, 1));
        assertTrue(N.lessEqual(1, 1));
    }

    @Test
    public void testGreaterThan() {
        assertFalse(N.greaterThan(1, 2));
        assertTrue(N.greaterThan(2, 1));
        assertFalse(N.greaterThan(1, 1));
    }

    @Test
    public void testGreaterEqual() {
        assertFalse(N.greaterEqual(1, 2));
        assertTrue(N.greaterEqual(2, 1));
        assertTrue(N.greaterEqual(1, 1));
    }

    @Test
    public void testGtAndLt() {
        assertTrue(N.gtAndLt(5, 1, 10));
        assertFalse(N.gtAndLt(1, 1, 10));
        assertFalse(N.gtAndLt(10, 1, 10));
        assertFalse(N.gtAndLt(15, 1, 10));
    }

    @Test
    public void testGtAndLtWithComparator() {
        assertTrue(N.gtAndLt("m", "a", "z", String.CASE_INSENSITIVE_ORDER));
        assertFalse(N.gtAndLt("a", "a", "z", String.CASE_INSENSITIVE_ORDER));
    }

    @Test
    public void testGeAndLt() {
        assertTrue(N.geAndLt(5, 1, 10));
        assertTrue(N.geAndLt(1, 1, 10));
        assertFalse(N.geAndLt(10, 1, 10));
        assertFalse(N.geAndLt(0, 1, 10));
    }

    @Test
    public void testGeAndLe() {
        assertTrue(N.geAndLe(5, 1, 10));
        assertTrue(N.geAndLe(1, 1, 10));
        assertTrue(N.geAndLe(10, 1, 10));
        assertFalse(N.geAndLe(0, 1, 10));
        assertFalse(N.geAndLe(11, 1, 10));
    }

    @Test
    public void testGtAndLe() {
        assertTrue(N.gtAndLe(5, 1, 10));
        assertFalse(N.gtAndLe(1, 1, 10));
        assertTrue(N.gtAndLe(10, 1, 10));
        assertFalse(N.gtAndLe(0, 1, 10));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testIsBetween() {
        assertTrue(N.isBetween(5, 1, 10));
        assertTrue(N.isBetween(1, 1, 10));
        assertTrue(N.isBetween(10, 1, 10));
        assertFalse(N.isBetween(0, 1, 10));
        assertFalse(N.isBetween(11, 1, 10));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testIsBetweenWithComparator() {
        assertTrue(N.isBetween("m", "a", "z", String.CASE_INSENSITIVE_ORDER));
        assertTrue(N.isBetween("a", "a", "z", String.CASE_INSENSITIVE_ORDER));
        assertTrue(N.isBetween("z", "a", "z", String.CASE_INSENSITIVE_ORDER));
    }

    // Test get element methods
    @Test
    public void testGetElementFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a", N.getElement(list, 0));
        assertEquals("b", N.getElement(list, 1));
        assertEquals("c", N.getElement(list, 2));

        assertThrows(IndexOutOfBoundsException.class, () -> N.getElement(list, 3));
        assertThrows(IllegalArgumentException.class, () -> N.getElement((Iterable<String>) null, 0));
    }

    @Test
    public void testGetElementFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a", N.getElement(list.iterator(), 0));
        assertEquals("b", N.getElement(list.iterator(), 1));
        assertEquals("c", N.getElement(list.iterator(), 2));

        assertThrows(IndexOutOfBoundsException.class, () -> N.getElement(list.iterator(), 3));
        assertThrows(IllegalArgumentException.class, () -> N.getElement((Iterator<String>) null, 0));
    }

    @Test
    public void testGetOnlyElementFromIterable() {
        List<String> single = Arrays.asList("only");
        List<String> multiple = Arrays.asList("a", "b");
        List<String> empty = Collections.emptyList();

        assertEquals("only", N.getOnlyElement(single).orElse(null));
        assertThrows(TooManyElementsException.class, () -> N.getOnlyElement(multiple));
        assertFalse(N.getOnlyElement(empty).isPresent());
        assertFalse(N.getOnlyElement((List<String>) null).isPresent());
    }

    @Test
    public void testGetOnlyElementFromIterator() {
        List<String> single = Arrays.asList("only");
        List<String> multiple = Arrays.asList("a", "b");

        assertEquals("only", N.getOnlyElement(single.iterator()).orElse(null));
        assertThrows(TooManyElementsException.class, () -> N.getOnlyElement(multiple.iterator()));
        assertFalse(N.getOnlyElement(Collections.emptyIterator()).isPresent());
        assertFalse(N.getOnlyElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testFirstElement() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a", N.firstElement(list).orElse(null));
        assertFalse(N.firstElement(Collections.emptyList()).isPresent());
        assertFalse(N.firstElement((Iterable<String>) null).isPresent());
    }

    @Test
    public void testFirstElementFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a", N.firstElement(list.iterator()).orElse(null));
        assertFalse(N.firstElement(Collections.emptyIterator()).isPresent());
        assertFalse(N.firstElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testLastElement() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("c", N.lastElement(list).orElse(null));
        assertFalse(N.lastElement(Collections.emptyList()).isPresent());
        assertFalse(N.lastElement((Iterable<String>) null).isPresent());
    }

    @Test
    public void testLastElementFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("c", N.lastElement(list.iterator()).orElse(null));
        assertFalse(N.lastElement(Collections.emptyIterator()).isPresent());
        assertFalse(N.lastElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testFirstElements() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        assertEquals(Arrays.asList("a", "b", "c"), N.firstElements(list, 3));
        assertEquals(list, N.firstElements(list, 10));
        assertEquals(Collections.emptyList(), N.firstElements(list, 0));
        assertEquals(Collections.emptyList(), N.firstElements(Collections.emptyList(), 3));
        assertEquals(Collections.emptyList(), N.firstElements((Iterable<String>) null, 3));

        assertThrows(IllegalArgumentException.class, () -> N.firstElements(list, -1));
    }

    @Test
    public void testFirstElementsFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        assertEquals(Arrays.asList("a", "b", "c"), N.firstElements(list.iterator(), 3));
        assertEquals(list, N.firstElements(list.iterator(), 10));
        assertEquals(Collections.emptyList(), N.firstElements(list.iterator(), 0));

        assertThrows(IllegalArgumentException.class, () -> N.firstElements(list.iterator(), -1));
    }

    @Test
    public void testLastElements() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        assertEquals(Arrays.asList("c", "d", "e"), N.lastElements(list, 3));
        assertEquals(list, N.lastElements(list, 10));
        assertEquals(Collections.emptyList(), N.lastElements(list, 0));
        assertEquals(Collections.emptyList(), N.lastElements(Collections.emptyList(), 3));
        assertEquals(Collections.emptyList(), N.lastElements((Iterable<String>) null, 3));

        assertThrows(IllegalArgumentException.class, () -> N.lastElements(list, -1));
    }

    @Test
    public void testLastElementsFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        assertEquals(Arrays.asList("c", "d", "e"), N.lastElements(list.iterator(), 3));
        assertEquals(list, N.lastElements(list.iterator(), 10));
        assertEquals(Collections.emptyList(), N.lastElements(list.iterator(), 0));

        assertThrows(IllegalArgumentException.class, () -> N.lastElements(list.iterator(), -1));
    }

    // Test firstNonNull methods
    @Test
    public void testFirstNonNull2() {
        assertEquals("a", N.firstNonNull("a", "b").get());
        assertEquals("b", N.firstNonNull(null, "b").get());
        assertFalse(N.firstNonNull(null, null).isPresent());
    }

    @Test
    public void testFirstNonNull3() {
        assertEquals("a", N.firstNonNull("a", "b", "c").get());
        assertEquals("b", N.firstNonNull(null, "b", "c").get());
        assertEquals("c", N.firstNonNull(null, null, "c").get());
        assertFalse(N.firstNonNull(null, null, null).isPresent());
    }

    @Test
    public void testFirstNonNullVarargs() {
        assertEquals("a", N.firstNonNull("a", "b", "c", "d").get());
        assertEquals("c", N.firstNonNull(null, null, "c", "d").get());
        assertFalse(N.firstNonNull((String[]) null).isPresent());
        assertFalse(N.firstNonNull(new String[] {}).isPresent());
        assertFalse(N.firstNonNull(null, null, null).isPresent());
    }

    @Test
    public void testFirstNonNullIterable() {
        assertEquals("b", N.firstNonNull(Arrays.asList(null, "b", "c")).get());
        assertFalse(N.firstNonNull(Collections.emptyList()).isPresent());
        assertFalse(N.firstNonNull((Iterable<String>) null).isPresent());
        assertFalse(N.firstNonNull(Arrays.asList(null, null)).isPresent());
    }

    @Test
    public void testFirstNonNullIterator() {
        assertEquals("b", N.firstNonNull(Arrays.asList(null, "b", "c").iterator()).get());
        assertFalse(N.firstNonNull(Collections.emptyIterator()).isPresent());
        assertFalse(N.firstNonNull((Iterator<String>) null).isPresent());
    }

    // Test lastNonNull methods
    @Test
    public void testLastNonNull2() {
        assertEquals("b", N.lastNonNull("a", "b").get());
        assertEquals("a", N.lastNonNull("a", null).get());
        assertFalse(N.lastNonNull(null, null).isPresent());
    }

    @Test
    public void testLastNonNull3() {
        assertEquals("c", N.lastNonNull("a", "b", "c").get());
        assertEquals("b", N.lastNonNull("a", "b", null).get());
        assertEquals("a", N.lastNonNull("a", null, null).get());
        assertFalse(N.lastNonNull(null, null, null).isPresent());
    }

    @Test
    public void testLastNonNullVarargs() {
        assertEquals("d", N.lastNonNull("a", "b", "c", "d").get());
        assertEquals("b", N.lastNonNull("a", "b", null, null).get());
        assertFalse(N.lastNonNull((String[]) null).isPresent());
        assertFalse(N.lastNonNull(new String[] {}).isPresent());
    }

    @Test
    public void testLastNonNullIterable() {
        assertEquals("c", N.lastNonNull(Arrays.asList("a", "b", "c")).get());
        assertEquals("b", N.lastNonNull(Arrays.asList("a", "b", null)).get());
        assertFalse(N.lastNonNull(Collections.emptyList()).isPresent());
        assertFalse(N.lastNonNull((Iterable<String>) null).isPresent());
    }

    @Test
    public void testLastNonNullIterator() {
        assertEquals("c", N.lastNonNull(Arrays.asList("a", "b", "c").iterator()).get());
        assertEquals("b", N.lastNonNull(Arrays.asList("a", "b", null).iterator()).get());
        assertFalse(N.lastNonNull(Collections.emptyIterator()).isPresent());
        assertFalse(N.lastNonNull((Iterator<String>) null).isPresent());
    }

    // Test firstNonEmpty methods
    @Test
    public void testFirstNonEmptyArrays() {
        String[] a1 = { "a", "b" };
        String[] a2 = { "c", "d" };
        String[] empty = {};

        assertEquals(a1, N.firstNonEmpty(a1, a2).get());
        assertEquals(a2, N.firstNonEmpty(null, a2).get());
        assertEquals(a2, N.firstNonEmpty(empty, a2).get());
        assertFalse(N.firstNonEmpty((String[]) null, (String[]) null).isPresent());
        assertFalse(N.firstNonEmpty(empty, empty).isPresent());

        assertEquals(a1, N.firstNonEmpty(a1, a2, empty).get());
        assertEquals(a2, N.firstNonEmpty(empty, a2, a1).get());
        assertEquals(a1, N.firstNonEmpty(empty, empty, a1).get());
    }

    @Test
    public void testFirstNonEmptyCollections() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("c", "d");
        List<String> empty = Collections.emptyList();

        assertEquals(list1, N.firstNonEmpty(list1, list2).get());
        assertEquals(list2, N.firstNonEmpty(null, list2).get());
        assertEquals(list2, N.firstNonEmpty(empty, list2).get());
        assertFalse(N.firstNonEmpty((List<String>) null, (List<String>) null).isPresent());
        assertFalse(N.firstNonEmpty(empty, empty).isPresent());

        assertEquals(list1, N.firstNonEmpty(list1, list2, empty).get());
    }

    @Test
    public void testFirstNonEmptyMaps() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        Map<String, String> empty = Collections.emptyMap();

        assertEquals(map1, N.firstNonEmpty(map1, map2).get());
        assertEquals(map2, N.firstNonEmpty(null, map2).get());
        assertEquals(map2, N.firstNonEmpty(empty, map2).get());
        assertFalse(N.firstNonEmpty((Map<String, String>) null, (Map<String, String>) null).isPresent());

        assertEquals(map1, N.firstNonEmpty(map1, map2, empty).get());
    }

    @Test
    public void testFirstNonEmptyCharSequences() {
        assertEquals("abc", N.firstNonEmpty("abc", "def").get());
        assertEquals("def", N.firstNonEmpty("", "def").get());
        assertEquals("def", N.firstNonEmpty(null, "def").get());
        assertFalse(N.firstNonEmpty("", "").isPresent());
        assertFalse(N.firstNonEmpty((String) null, (String) null).isPresent());

        assertEquals("abc", N.firstNonEmpty("abc", "def", "ghi").get());
        assertEquals("def", N.firstNonEmpty("", "def", "ghi").get());
        assertEquals("ghi", N.firstNonEmpty("", "", "ghi").get());
    }

    @Test
    public void testFirstNonEmptyCharSequencesVarargs() {
        assertEquals("abc", N.firstNonEmpty("abc", "def", "ghi", "jkl").get());
        assertEquals("ghi", N.firstNonEmpty("", null, "ghi", "jkl").get());
        assertFalse(N.firstNonEmpty((String[]) null).isPresent());
        assertFalse(N.firstNonEmpty(new String[] {}).isPresent());
        assertFalse(N.firstNonEmpty("", null, "", null).isPresent());
    }

    @Test
    public void testFirstNonEmptyCharSequencesIterable() {
        assertEquals("abc", N.firstNonEmpty(Arrays.asList("abc", "def")).get());
        assertEquals("def", N.firstNonEmpty(Arrays.asList("", "def")).get());
        assertFalse(N.firstNonEmpty(Collections.<String> emptyList()).isPresent());
        assertFalse(N.firstNonEmpty((Iterable<String>) null).isPresent());
        assertFalse(N.firstNonEmpty(Arrays.asList("", null, "")).isPresent());
    }

    // Test firstNonBlank methods
    @Test
    public void testFirstNonBlank2() {
        assertEquals("abc", N.firstNonBlank("abc", "def").get());
        assertEquals("def", N.firstNonBlank("  ", "def").get());
        assertEquals("def", N.firstNonBlank(null, "def").get());
        assertFalse(N.firstNonBlank("  ", "  ").isPresent());
        assertFalse(N.firstNonBlank((String) null, (String) null).isPresent());
    }

    @Test
    public void testFirstNonBlank3() {
        assertEquals("abc", N.firstNonBlank("abc", "def", "ghi").get());
        assertEquals("def", N.firstNonBlank("  ", "def", "ghi").get());
        assertEquals("ghi", N.firstNonBlank("  ", "  ", "ghi").get());
        assertFalse(N.firstNonBlank("  ", null, "  ").isPresent());
    }

    @Test
    public void testFirstNonBlankVarargs() {
        assertEquals("abc", N.firstNonBlank("abc", "def", "ghi", "jkl").get());
        assertEquals("ghi", N.firstNonBlank("  ", null, "ghi", "jkl").get());
        assertFalse(N.firstNonBlank((String[]) null).isPresent());
        assertFalse(N.firstNonBlank(new String[] {}).isPresent());
        assertFalse(N.firstNonBlank("  ", null, "  ", null).isPresent());
    }

    @Test
    public void testFirstNonBlankIterable() {
        assertEquals("abc", N.firstNonBlank(Arrays.asList("abc", "def")).get());
        assertEquals("def", N.firstNonBlank(Arrays.asList("  ", "def")).get());
        assertFalse(N.firstNonBlank(Collections.<String> emptyList()).isPresent());
        assertFalse(N.firstNonBlank((Iterable<String>) null).isPresent());
        assertFalse(N.firstNonBlank(Arrays.asList("  ", null, "  ")).isPresent());
    }

    // Test map entry methods
    @Test
    public void testFirstEntry() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Map.Entry<String, Integer> entry = N.firstEntry(map).get();
        assertEquals("a", entry.getKey());
        assertEquals(Integer.valueOf(1), entry.getValue());

        assertFalse(N.firstEntry(Collections.<String, Integer> emptyMap()).isPresent());
        assertFalse(N.firstEntry(null).isPresent());
    }

    @Test
    public void testLastEntry() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Map.Entry<String, Integer> entry = N.lastEntry(map).get();
        assertEquals("c", entry.getKey());
        assertEquals(Integer.valueOf(3), entry.getValue());

        assertFalse(N.lastEntry(Collections.<String, Integer> emptyMap()).isPresent());
        assertFalse(N.lastEntry(null).isPresent());
    }

    // Test firstOrNullIfEmpty methods
    @Test
    public void testFirstOrNullIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        assertEquals("a", N.firstOrNullIfEmpty(arr));
        assertNull(N.firstOrNullIfEmpty(new String[] {}));
        assertNull(N.firstOrNullIfEmpty((String[]) null));
    }

    @Test
    public void testFirstOrNullIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a", N.firstOrNullIfEmpty(list));
        assertNull(N.firstOrNullIfEmpty(Collections.emptyList()));
        assertNull(N.firstOrNullIfEmpty((Iterable<String>) null));
    }

    @Test
    public void testFirstOrNullIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a", N.firstOrNullIfEmpty(list.iterator()));
        assertNull(N.firstOrNullIfEmpty(Collections.emptyIterator()));
        assertNull(N.firstOrNullIfEmpty((Iterator<String>) null));
    }

    // Test firstOrDefaultIfEmpty methods
    @Test
    public void testFirstOrDefaultIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        assertEquals("a", N.firstOrDefaultIfEmpty(arr, "default"));
        assertEquals("default", N.firstOrDefaultIfEmpty(new String[] {}, "default"));
        assertEquals("default", N.firstOrDefaultIfEmpty((String[]) null, "default"));
    }

    @Test
    public void testFirstOrDefaultIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a", N.firstOrDefaultIfEmpty(list, "default"));
        assertEquals("default", N.firstOrDefaultIfEmpty(Collections.<String> emptyList(), "default"));
        assertEquals("default", N.firstOrDefaultIfEmpty((Iterable<String>) null, "default"));
    }

    @Test
    public void testFirstOrDefaultIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a", N.firstOrDefaultIfEmpty(list.iterator(), "default"));
        assertEquals("default", N.firstOrDefaultIfEmpty(Collections.<String> emptyIterator(), "default"));
        assertEquals("default", N.firstOrDefaultIfEmpty((Iterator<String>) null, "default"));
    }

    // Test lastOrNullIfEmpty methods
    @Test
    public void testLastOrNullIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        assertEquals("c", N.lastOrNullIfEmpty(arr));
        assertNull(N.lastOrNullIfEmpty(new String[] {}));
        assertNull(N.lastOrNullIfEmpty((String[]) null));
    }

    @Test
    public void testLastOrNullIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("c", N.lastOrNullIfEmpty(list));
        assertNull(N.lastOrNullIfEmpty(Collections.emptyList()));
        assertNull(N.lastOrNullIfEmpty((Iterable<String>) null));
    }

    @Test
    public void testLastOrNullIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("c", N.lastOrNullIfEmpty(list.iterator()));
        assertNull(N.lastOrNullIfEmpty(Collections.emptyIterator()));
        assertNull(N.lastOrNullIfEmpty((Iterator<String>) null));
    }

    // Test lastOrDefaultIfEmpty methods
    @Test
    public void testLastOrDefaultIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        assertEquals("c", N.lastOrDefaultIfEmpty(arr, "default"));
        assertEquals("default", N.lastOrDefaultIfEmpty(new String[] {}, "default"));
        assertEquals("default", N.lastOrDefaultIfEmpty((String[]) null, "default"));
    }

    @Test
    public void testLastOrDefaultIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("c", N.lastOrDefaultIfEmpty(list, "default"));
        assertEquals("default", N.lastOrDefaultIfEmpty(Collections.<String> emptyList(), "default"));
        assertEquals("default", N.lastOrDefaultIfEmpty((Iterable<String>) null, "default"));
    }

    @Test
    public void testLastOrDefaultIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("c", N.lastOrDefaultIfEmpty(list.iterator(), "default"));
        assertEquals("default", N.lastOrDefaultIfEmpty(Collections.<String> emptyIterator(), "default"));
        assertEquals("default", N.lastOrDefaultIfEmpty((Iterator<String>) null, "default"));
    }

    // Test find methods
    @Test
    public void testFindFirstArray() {
        String[] arr = { "a", "b", "c", "d" };
        assertEquals("b", N.findFirst(arr, s -> s.equals("b")).orElse(null));
        assertEquals("c", N.findFirst(arr, s -> s.compareTo("b") > 0).orElse(null));
        assertFalse(N.findFirst(arr, s -> s.equals("z")).isPresent());
        assertFalse(N.findFirst(new String[] {}, s -> true).isPresent());
        assertFalse(N.findFirst((String[]) null, s -> true).isPresent());
    }

    @Test
    public void testFindFirstIterable() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        assertEquals("b", N.findFirst(list, s -> s.equals("b")).orElse(null));
        assertEquals("c", N.findFirst(list, s -> s.compareTo("b") > 0).orElse(null));
        assertFalse(N.findFirst(list, s -> s.equals("z")).isPresent());
        assertFalse(N.findFirst(Collections.<String> emptyList(), s -> true).isPresent());
        assertFalse(N.findFirst((Iterable<String>) null, s -> true).isPresent());
    }

    @Test
    public void testFindFirstIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        assertEquals("b", N.findFirst(list.iterator(), s -> s.equals("b")).orElse(null));
        assertEquals("c", N.findFirst(list.iterator(), s -> s.compareTo("b") > 0).orElse(null));
        assertFalse(N.findFirst(list.iterator(), s -> s.equals("z")).isPresent());
        assertFalse(N.findFirst(Collections.<String> emptyIterator(), s -> true).isPresent());
        assertFalse(N.findFirst((Iterator<String>) null, s -> true).isPresent());
    }

    @Test
    public void testFindLastArray() {
        String[] arr = { "a", "b", "c", "d" };
        assertEquals("b", N.findLast(arr, s -> s.equals("b")).orElse(null));
        assertEquals("d", N.findLast(arr, s -> s.compareTo("b") > 0).orElse(null));
        assertFalse(N.findLast(arr, s -> s.equals("z")).isPresent());
        assertFalse(N.findLast(new String[] {}, s -> true).isPresent());
        assertFalse(N.findLast((String[]) null, s -> true).isPresent());
    }

    @Test
    public void testFindLastIterable() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        assertEquals("b", N.findLast(list, s -> s.equals("b")).orElse(null));
        assertEquals("d", N.findLast(list, s -> s.compareTo("b") > 0).orElse(null));
        assertFalse(N.findLast(list, s -> s.equals("z")).isPresent());
        assertFalse(N.findLast(Collections.<String> emptyList(), s -> true).isPresent());
        assertFalse(N.findLast((Iterable<String>) null, s -> true).isPresent());
    }

    @Test
    public void testFindFirstNonNullArray() {
        String[] arr = { null, "a", null, "b", "c" };
        assertEquals("a", N.findFirstNonNull(arr, s -> true).get());
        assertEquals("b", N.findFirstNonNull(arr, s -> s.equals("b")).get());
        assertFalse(N.findFirstNonNull(arr, s -> s.equals("z")).isPresent());
        assertFalse(N.findFirstNonNull(new String[] { null, null }, s -> true).isPresent());
        assertFalse(N.findFirstNonNull((String[]) null, s -> true).isPresent());
    }

    @Test
    public void testFindFirstNonNullIterable() {
        List<String> list = Arrays.asList(null, "a", null, "b", "c");
        assertEquals("a", N.findFirstNonNull(list, s -> true).get());
        assertEquals("b", N.findFirstNonNull(list, s -> s.equals("b")).get());
        assertFalse(N.findFirstNonNull(list, s -> s.equals("z")).isPresent());
        assertFalse(N.findFirstNonNull(Arrays.asList(null, null), s -> true).isPresent());
        assertFalse(N.findFirstNonNull((Iterable<String>) null, s -> true).isPresent());
    }

    @Test
    public void testFindFirstNonNullIterator() {
        List<String> list = Arrays.asList(null, "a", null, "b", "c");
        assertEquals("a", N.findFirstNonNull(list.iterator(), s -> true).get());
        assertEquals("b", N.findFirstNonNull(list.iterator(), s -> s.equals("b")).get());
        assertFalse(N.findFirstNonNull(list.iterator(), s -> s.equals("z")).isPresent());
        assertFalse(N.findFirstNonNull(Arrays.asList(null, null).iterator(), s -> true).isPresent());
        assertFalse(N.findFirstNonNull((Iterator<String>) null, s -> true).isPresent());
    }
}