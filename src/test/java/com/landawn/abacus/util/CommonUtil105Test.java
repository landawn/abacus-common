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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;

@Tag("new-test")
public class CommonUtil105Test extends TestBase {

    @Test
    public void testEmptyMap() {
        Map<String, Integer> map = CommonUtil.emptyMap();

        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        assertThrows(UnsupportedOperationException.class, () -> {
            map.put("key", 1);
        });
    }

    @Test
    public void testEmptyMapReturnsSameInstance() {
        Map<String, String> map1 = CommonUtil.emptyMap();
        Map<Integer, Integer> map2 = CommonUtil.emptyMap();

        assertSame(map1, map2);
    }

    @Test
    public void testEmptySortedMap() {
        SortedMap<String, Integer> map = CommonUtil.emptySortedMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> map.put("key", 1));
    }

    @Test
    public void testEmptyNavigableMap() {
        NavigableMap<String, Integer> map = CommonUtil.emptyNavigableMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> map.put("key", 1));
    }

    @Test
    public void testEmptyIterator() {
        Iterator<String> iter = CommonUtil.emptyIterator();
        assertNotNull(iter);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testEmptyListIterator() {
        ListIterator<String> iter = CommonUtil.emptyListIterator();
        assertNotNull(iter);
        assertFalse(iter.hasNext());
        assertFalse(iter.hasPrevious());
        assertThrows(NoSuchElementException.class, () -> iter.next());
        assertThrows(NoSuchElementException.class, () -> iter.previous());
    }

    @Test
    public void testEmptyInputStream() throws IOException {
        InputStream stream = CommonUtil.emptyInputStream();
        assertNotNull(stream);
        assertEquals(-1, stream.read());
        assertEquals(0, stream.available());
    }

    @Test
    public void testEmptyDataset() {
        Dataset ds = CommonUtil.emptyDataset();
        assertNotNull(ds);
        assertTrue(ds.isEmpty());
    }

    @Test
    public void testCompareBoolean() {
        assertEquals(0, CommonUtil.compare(true, true));
        assertEquals(0, CommonUtil.compare(false, false));
        assertEquals(1, CommonUtil.compare(true, false));
        assertEquals(-1, CommonUtil.compare(false, true));
    }

    @Test
    public void testCompareChar() {
        assertEquals(0, CommonUtil.compare('a', 'a'));
        assertEquals(-1, CommonUtil.compare('a', 'b'));
        assertEquals(1, CommonUtil.compare('b', 'a'));
    }

    @Test
    public void testCompareByte() {
        assertEquals(0, CommonUtil.compare((byte) 10, (byte) 10));
        assertTrue(CommonUtil.compare((byte) 5, (byte) 10) < 0);
        assertTrue(CommonUtil.compare((byte) 10, (byte) 5) > 0);
    }

    @Test
    public void testCompareUnsignedByte() {
        assertEquals(0, CommonUtil.compareUnsigned((byte) 10, (byte) 10));
        assertTrue(CommonUtil.compareUnsigned((byte) 5, (byte) 10) < 0);
        assertTrue(CommonUtil.compareUnsigned((byte) -1, (byte) 127) > 0);
    }

    @Test
    public void testCompareShort() {
        assertEquals(0, CommonUtil.compare((short) 100, (short) 100));
        assertTrue(CommonUtil.compare((short) 50, (short) 100) < 0);
        assertTrue(CommonUtil.compare((short) 100, (short) 50) > 0);
    }

    @Test
    public void testCompareUnsignedShort() {
        assertEquals(0, CommonUtil.compareUnsigned((short) 100, (short) 100));
        assertTrue(CommonUtil.compareUnsigned((short) 50, (short) 100) < 0);
        assertTrue(CommonUtil.compareUnsigned((short) -1, (short) 32767) > 0);
    }

    @Test
    public void testCompareInt() {
        assertEquals(0, CommonUtil.compare(100, 100));
        assertTrue(CommonUtil.compare(50, 100) < 0);
        assertTrue(CommonUtil.compare(100, 50) > 0);
    }

    @Test
    public void testCompareUnsignedInt() {
        assertEquals(0, CommonUtil.compareUnsigned(100, 100));
        assertTrue(CommonUtil.compareUnsigned(50, 100) < 0);
        assertTrue(CommonUtil.compareUnsigned(-1, Integer.MAX_VALUE) > 0);
    }

    @Test
    public void testCompareLong() {
        assertEquals(0, CommonUtil.compare(100L, 100L));
        assertTrue(CommonUtil.compare(50L, 100L) < 0);
        assertTrue(CommonUtil.compare(100L, 50L) > 0);
    }

    @Test
    public void testCompareUnsignedLong() {
        assertEquals(0, CommonUtil.compareUnsigned(100L, 100L));
        assertEquals(-1, CommonUtil.compareUnsigned(50L, 100L));
        assertEquals(1, CommonUtil.compareUnsigned(-1L, Long.MAX_VALUE));
    }

    @Test
    public void testCompareFloat() {
        assertEquals(0, CommonUtil.compare(1.5f, 1.5f));
        assertEquals(-1, CommonUtil.compare(1.0f, 1.5f));
        assertEquals(1, CommonUtil.compare(1.5f, 1.0f));
        assertEquals(1, CommonUtil.compare(Float.NaN, 1.0f));
    }

    @Test
    public void testCompareDouble() {
        assertEquals(0, CommonUtil.compare(1.5, 1.5));
        assertEquals(-1, CommonUtil.compare(1.0, 1.5));
        assertEquals(1, CommonUtil.compare(1.5, 1.0));
        assertEquals(1, CommonUtil.compare(Double.NaN, 1.0));
    }

    @Test
    public void testCompareComparable() {
        assertEquals(0, CommonUtil.compare("abc", "abc"));
        assertTrue(CommonUtil.compare("abc", "def") < 0);
        assertTrue(CommonUtil.compare("def", "abc") > 0);
        assertTrue(CommonUtil.compare(null, "abc") < 0);
        assertTrue(CommonUtil.compare("abc", null) > 0);
        assertEquals(0, CommonUtil.compare((String) null, null));
    }

    @Test
    public void testCompareWithComparator() {
        Comparator<String> comp = String.CASE_INSENSITIVE_ORDER;
        assertEquals(0, CommonUtil.compare("ABC", "abc", comp));
        assertTrue(CommonUtil.compare("abc", "def", comp) < 0);
        assertTrue(CommonUtil.compare("def", "abc", comp) > 0);

        assertEquals(0, CommonUtil.compare("abc", "abc", null));
    }

    @Test
    public void testCompareMultiplePairs2() {
        assertEquals(0, CommonUtil.compare("a", "a", 1, 1));
        assertEquals(-1, CommonUtil.compare("a", "b", 1, 1));
        assertEquals(1, CommonUtil.compare("b", "a", 1, 1));
        assertEquals(-1, CommonUtil.compare("a", "a", 1, 2));
        assertEquals(1, CommonUtil.compare("a", "a", 2, 1));
    }

    @Test
    public void testCompareMultiplePairs3() {
        assertEquals(0, CommonUtil.compare("a", "a", 1, 1, 5L, 5L));
        assertEquals(-1, CommonUtil.compare("a", "b", 1, 1, 5L, 5L));
        assertEquals(1, CommonUtil.compare("a", "a", 2, 1, 5L, 5L));
        assertEquals(-1, CommonUtil.compare("a", "a", 1, 1, 5L, 6L));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCompareMultiplePairs4() {
        assertEquals(0, CommonUtil.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0));
        assertEquals(-1, CommonUtil.compare("a", "b", 1, 1, 5L, 5L, 1.0, 1.0));
        assertEquals(-1, CommonUtil.compare("a", "a", 1, 1, 5L, 5L, 1.0, 2.0));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCompareMultiplePairs5() {
        assertEquals(0, CommonUtil.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x"));
        assertEquals(1, CommonUtil.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "y", "x"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCompareMultiplePairs6() {
        assertEquals(0, CommonUtil.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x", true, true));
        assertEquals(-1, CommonUtil.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x", false, true));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCompareMultiplePairs7() {
        assertEquals(0, CommonUtil.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x", true, true, 'z', 'z'));
        assertEquals(1, CommonUtil.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x", true, true, 'z', 'y'));
    }

    @Test
    public void testCompareBooleanArrays() {
        boolean[] a1 = { true, false, true };
        boolean[] a2 = { true, false, true };
        boolean[] a3 = { true, true, false };
        boolean[] a4 = { true, false };

        assertEquals(0, CommonUtil.compare(a1, a2));
        assertEquals(-1, CommonUtil.compare(a1, a3));
        assertEquals(1, CommonUtil.compare(a1, a4));
        assertEquals(-1, CommonUtil.compare(null, a1));
        assertEquals(1, CommonUtil.compare(a1, null));
        assertEquals(0, CommonUtil.compare((boolean[]) null, (boolean[]) null));
        assertEquals(-1, CommonUtil.compare(new boolean[] {}, a1));
    }

    @Test
    public void testCompareBooleanArraysWithRange() {
        boolean[] a1 = { true, false, true, false };
        boolean[] a2 = { false, false, true, true };

        assertEquals(0, CommonUtil.compare(a1, 1, a2, 1, 2));
        assertTrue(CommonUtil.compare(a1, 0, a2, 0, 2) > 0);
        assertEquals(0, CommonUtil.compare(a1, 0, a1, 0, 2));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.compare(a1, 0, a2, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.compare(a1, 0, a2, 0, 10));
    }

    @Test
    public void testCompareCharArrays() {
        char[] a1 = { 'a', 'b', 'c' };
        char[] a2 = { 'a', 'b', 'c' };
        char[] a3 = { 'a', 'c', 'b' };

        assertEquals(0, CommonUtil.compare(a1, a2));
        assertEquals(-1, CommonUtil.compare(a1, a3));
        assertEquals(-1, CommonUtil.compare(null, a1));
        assertEquals(1, CommonUtil.compare(a1, null));
    }

    @Test
    public void testCompareCharArraysWithRange() {
        char[] a1 = { 'a', 'b', 'c', 'd' };
        char[] a2 = { 'x', 'b', 'c', 'y' };

        assertEquals(0, CommonUtil.compare(a1, 1, a2, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.compare(a1, 0, a2, 0, -1));
    }

    @Test
    public void testCompareByteArrays() {
        byte[] a1 = { 1, 2, 3 };
        byte[] a2 = { 1, 2, 3 };
        byte[] a3 = { 1, 3, 2 };

        assertEquals(0, CommonUtil.compare(a1, a2));
        assertEquals(-1, CommonUtil.compare(a1, a3));
    }

    @Test
    public void testCompareByteArraysWithRange() {
        byte[] a1 = { 1, 2, 3, 4 };
        byte[] a2 = { 5, 2, 3, 6 };

        assertEquals(0, CommonUtil.compare(a1, 1, a2, 1, 2));
    }

    @Test
    public void testCompareUnsignedByteArrays() {
        byte[] a1 = { 1, 2, (byte) 255 };
        byte[] a2 = { 1, 2, 3 };

        assertTrue(CommonUtil.compareUnsigned(a1, a2) > 0);
    }

    @Test
    public void testCompareUnsignedByteArraysWithRange() {
        byte[] a1 = { 1, 2, (byte) 255, 4 };
        byte[] a2 = { 5, 2, 3, 6 };

        assertTrue(CommonUtil.compareUnsigned(a1, 2, a2, 2, 1) > 0);
    }

    @Test
    public void testCompareShortArrays() {
        short[] a1 = { 10, 20, 30 };
        short[] a2 = { 10, 20, 30 };
        short[] a3 = { 10, 30, 20 };

        assertEquals(0, CommonUtil.compare(a1, a2));
        assertEquals(-1, CommonUtil.compare(a1, a3));
    }

    @Test
    public void testCompareIntArrays() {
        int[] a1 = { 100, 200, 300 };
        int[] a2 = { 100, 200, 300 };
        int[] a3 = { 100, 300, 200 };

        assertEquals(0, CommonUtil.compare(a1, a2));
        assertEquals(-1, CommonUtil.compare(a1, a3));
    }

    @Test
    public void testCompareLongArrays() {
        long[] a1 = { 1000L, 2000L, 3000L };
        long[] a2 = { 1000L, 2000L, 3000L };
        long[] a3 = { 1000L, 3000L, 2000L };

        assertEquals(0, CommonUtil.compare(a1, a2));
        assertEquals(-1, CommonUtil.compare(a1, a3));
    }

    @Test
    public void testCompareFloatArrays() {
        float[] a1 = { 1.0f, 2.0f, 3.0f };
        float[] a2 = { 1.0f, 2.0f, 3.0f };
        float[] a3 = { 1.0f, 3.0f, 2.0f };

        assertEquals(0, CommonUtil.compare(a1, a2));
        assertEquals(-1, CommonUtil.compare(a1, a3));
    }

    @Test
    public void testCompareDoubleArrays() {
        double[] a1 = { 1.0, 2.0, 3.0 };
        double[] a2 = { 1.0, 2.0, 3.0 };
        double[] a3 = { 1.0, 3.0, 2.0 };

        assertEquals(0, CommonUtil.compare(a1, a2));
        assertEquals(-1, CommonUtil.compare(a1, a3));
    }

    @Test
    public void testCompareObjectArrays() {
        String[] a1 = { "a", "b", "c" };
        String[] a2 = { "a", "b", "c" };
        String[] a3 = { "a", "c", "b" };

        assertEquals(0, CommonUtil.compare(a1, a2));
        assertEquals(-1, CommonUtil.compare(a1, a3));
        assertEquals(-1, CommonUtil.compare(null, a1));
        assertEquals(1, CommonUtil.compare(a1, null));
    }

    @Test
    public void testCompareObjectArraysWithRange() {
        String[] a1 = { "x", "b", "c", "y" };
        String[] a2 = { "z", "b", "c", "w" };

        assertEquals(0, CommonUtil.compare(a1, 1, a2, 1, 2));
    }

    @Test
    public void testCompareObjectArraysWithComparator() {
        String[] a1 = { "ABC", "DEF" };
        String[] a2 = { "abc", "def" };

        assertEquals(0, CommonUtil.compare(a1, a2, String.CASE_INSENSITIVE_ORDER));
        assertTrue(CommonUtil.compare(a1, a2, (Comparator<String>) null) < 0);
    }

    @Test
    public void testCompareArraysWithRange_01() {
        {
            byte[] a1 = { 1, 2, 3, 4 };
            byte[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, CommonUtil.compare(a1, 1, a2, 1, 2));
        }
        {
            short[] a1 = { 1, 2, 3, 4 };
            short[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, CommonUtil.compare(a1, 1, a2, 1, 2));
        }
        {

            int[] a1 = { 1, 2, 3, 4 };
            int[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, CommonUtil.compare(a1, 1, a2, 1, 2));
        }
        {

            long[] a1 = { 1, 2, 3, 4 };
            long[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, CommonUtil.compare(a1, 1, a2, 1, 2));
        }
        {

            float[] a1 = { 1, 2, 3, 4 };
            float[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, CommonUtil.compare(a1, 1, a2, 1, 2));
        }
        {

            double[] a1 = { 1, 2, 3, 4 };
            double[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, CommonUtil.compare(a1, 1, a2, 1, 2));
        }
    }

    @Test
    public void testCompareCollections() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        List<String> list3 = Arrays.asList("a", "c", "b");

        assertEquals(0, CommonUtil.compare(list1, 0, list2, 0, 3));
        assertEquals(-1, CommonUtil.compare(list1, 1, list3, 1, 2));
    }

    @Test
    public void testCompareIterables() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        List<String> list3 = Arrays.asList("a", "c", "b");

        assertEquals(0, CommonUtil.compare((Iterable<String>) list1, (Iterable<String>) list2));
        assertEquals(-1, CommonUtil.compare((Iterable<String>) list1, (Iterable<String>) list3));
        assertEquals(-1, CommonUtil.compare((Iterable<String>) null, (Iterable<String>) list1));
        assertEquals(1, CommonUtil.compare((Iterable<String>) list1, (Iterable<String>) null));
    }

    @Test
    public void testCompareIterators() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        List<String> list3 = Arrays.asList("a", "c", "b");

        assertEquals(0, CommonUtil.compare(list1.iterator(), list2.iterator()));
        assertEquals(-1, CommonUtil.compare(list1.iterator(), list3.iterator()));
        assertEquals(-1, CommonUtil.compare(null, list1.iterator()));
        assertEquals(1, CommonUtil.compare(list1.iterator(), null));
    }

    @Test
    public void testCompareIgnoreCase() {
        assertEquals(0, CommonUtil.compareIgnoreCase("ABC", "abc"));
        assertTrue(CommonUtil.compareIgnoreCase("abc", "def") < 0);
        assertTrue(CommonUtil.compareIgnoreCase("def", "abc") > 0);
        assertTrue(CommonUtil.compareIgnoreCase(null, "abc") < 0);
        assertTrue(CommonUtil.compareIgnoreCase("abc", null) > 0);
        assertEquals(0, CommonUtil.compareIgnoreCase((String) null, null));
    }

    @Test
    public void testCompareIgnoreCaseArrays() {
        String[] a1 = { "ABC", "DEF" };
        String[] a2 = { "abc", "def" };
        String[] a3 = { "abc", "xyz" };

        assertEquals(0, CommonUtil.compareIgnoreCase(a1, a2));
        assertTrue(CommonUtil.compareIgnoreCase(a1, a3) < 0);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCompareByProps() {
    }

    @Test
    public void testLessThan() {
        assertTrue(CommonUtil.lessThan(1, 2));
        assertFalse(CommonUtil.lessThan(2, 1));
        assertFalse(CommonUtil.lessThan(1, 1));
        assertTrue(CommonUtil.lessThan(null, 1));
        assertFalse(CommonUtil.lessThan(1, null));
    }

    @Test
    public void testLessThanWithComparator() {
        assertTrue(CommonUtil.lessThan("abc", "def", String.CASE_INSENSITIVE_ORDER));
        assertFalse(CommonUtil.lessThan("DEF", "abc", String.CASE_INSENSITIVE_ORDER));
    }

    @Test
    public void testLessEqual() {
        assertTrue(CommonUtil.lessEqual(1, 2));
        assertFalse(CommonUtil.lessEqual(2, 1));
        assertTrue(CommonUtil.lessEqual(1, 1));
    }

    @Test
    public void testGreaterThan() {
        assertFalse(CommonUtil.greaterThan(1, 2));
        assertTrue(CommonUtil.greaterThan(2, 1));
        assertFalse(CommonUtil.greaterThan(1, 1));
    }

    @Test
    public void testGreaterEqual() {
        assertFalse(CommonUtil.greaterEqual(1, 2));
        assertTrue(CommonUtil.greaterEqual(2, 1));
        assertTrue(CommonUtil.greaterEqual(1, 1));
    }

    @Test
    public void testGtAndLt() {
        assertTrue(CommonUtil.gtAndLt(5, 1, 10));
        assertFalse(CommonUtil.gtAndLt(1, 1, 10));
        assertFalse(CommonUtil.gtAndLt(10, 1, 10));
        assertFalse(CommonUtil.gtAndLt(15, 1, 10));
    }

    @Test
    public void testGtAndLtWithComparator() {
        assertTrue(CommonUtil.gtAndLt("m", "a", "z", String.CASE_INSENSITIVE_ORDER));
        assertFalse(CommonUtil.gtAndLt("a", "a", "z", String.CASE_INSENSITIVE_ORDER));
    }

    @Test
    public void testGeAndLt() {
        assertTrue(CommonUtil.geAndLt(5, 1, 10));
        assertTrue(CommonUtil.geAndLt(1, 1, 10));
        assertFalse(CommonUtil.geAndLt(10, 1, 10));
        assertFalse(CommonUtil.geAndLt(0, 1, 10));
    }

    @Test
    public void testGeAndLe() {
        assertTrue(CommonUtil.geAndLe(5, 1, 10));
        assertTrue(CommonUtil.geAndLe(1, 1, 10));
        assertTrue(CommonUtil.geAndLe(10, 1, 10));
        assertFalse(CommonUtil.geAndLe(0, 1, 10));
        assertFalse(CommonUtil.geAndLe(11, 1, 10));
    }

    @Test
    public void testGtAndLe() {
        assertTrue(CommonUtil.gtAndLe(5, 1, 10));
        assertFalse(CommonUtil.gtAndLe(1, 1, 10));
        assertTrue(CommonUtil.gtAndLe(10, 1, 10));
        assertFalse(CommonUtil.gtAndLe(0, 1, 10));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testIsBetween() {
        assertTrue(CommonUtil.isBetween(5, 1, 10));
        assertTrue(CommonUtil.isBetween(1, 1, 10));
        assertTrue(CommonUtil.isBetween(10, 1, 10));
        assertFalse(CommonUtil.isBetween(0, 1, 10));
        assertFalse(CommonUtil.isBetween(11, 1, 10));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testIsBetweenWithComparator() {
        assertTrue(CommonUtil.isBetween("m", "a", "z", String.CASE_INSENSITIVE_ORDER));
        assertTrue(CommonUtil.isBetween("a", "a", "z", String.CASE_INSENSITIVE_ORDER));
        assertTrue(CommonUtil.isBetween("z", "a", "z", String.CASE_INSENSITIVE_ORDER));
    }

    @Test
    public void testGetElementFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a", CommonUtil.getElement(list, 0));
        assertEquals("b", CommonUtil.getElement(list, 1));
        assertEquals("c", CommonUtil.getElement(list, 2));

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.getElement(list, 3));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.getElement((Iterable<String>) null, 0));
    }

    @Test
    public void testGetElementFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a", CommonUtil.getElement(list.iterator(), 0));
        assertEquals("b", CommonUtil.getElement(list.iterator(), 1));
        assertEquals("c", CommonUtil.getElement(list.iterator(), 2));

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.getElement(list.iterator(), 3));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.getElement((Iterator<String>) null, 0));
    }

    @Test
    public void testGetOnlyElementFromIterable() {
        List<String> single = Arrays.asList("only");
        List<String> multiple = Arrays.asList("a", "b");
        List<String> empty = Collections.emptyList();

        assertEquals("only", CommonUtil.getOnlyElement(single).orElse(null));
        assertThrows(TooManyElementsException.class, () -> CommonUtil.getOnlyElement(multiple));
        assertFalse(CommonUtil.getOnlyElement(empty).isPresent());
        assertFalse(CommonUtil.getOnlyElement((List<String>) null).isPresent());
    }

    @Test
    public void testGetOnlyElementFromIterator() {
        List<String> single = Arrays.asList("only");
        List<String> multiple = Arrays.asList("a", "b");

        assertEquals("only", CommonUtil.getOnlyElement(single.iterator()).orElse(null));
        assertThrows(TooManyElementsException.class, () -> CommonUtil.getOnlyElement(multiple.iterator()));
        assertFalse(CommonUtil.getOnlyElement(Collections.emptyIterator()).isPresent());
        assertFalse(CommonUtil.getOnlyElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testFirstElement() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a", CommonUtil.firstElement(list).orElse(null));
        assertFalse(CommonUtil.firstElement(Collections.emptyList()).isPresent());
        assertFalse(CommonUtil.firstElement((Iterable<String>) null).isPresent());
    }

    @Test
    public void testFirstElementFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a", CommonUtil.firstElement(list.iterator()).orElse(null));
        assertFalse(CommonUtil.firstElement(Collections.emptyIterator()).isPresent());
        assertFalse(CommonUtil.firstElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testLastElement() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("c", CommonUtil.lastElement(list).orElse(null));
        assertFalse(CommonUtil.lastElement(Collections.emptyList()).isPresent());
        assertFalse(CommonUtil.lastElement((Iterable<String>) null).isPresent());
    }

    @Test
    public void testLastElementFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("c", CommonUtil.lastElement(list.iterator()).orElse(null));
        assertFalse(CommonUtil.lastElement(Collections.emptyIterator()).isPresent());
        assertFalse(CommonUtil.lastElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testFirstElements() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        assertEquals(Arrays.asList("a", "b", "c"), CommonUtil.firstElements(list, 3));
        assertEquals(list, CommonUtil.firstElements(list, 10));
        assertEquals(Collections.emptyList(), CommonUtil.firstElements(list, 0));
        assertEquals(Collections.emptyList(), CommonUtil.firstElements(Collections.emptyList(), 3));
        assertEquals(Collections.emptyList(), CommonUtil.firstElements((Iterable<String>) null, 3));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.firstElements(list, -1));
    }

    @Test
    public void testFirstElementsFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        assertEquals(Arrays.asList("a", "b", "c"), CommonUtil.firstElements(list.iterator(), 3));
        assertEquals(list, CommonUtil.firstElements(list.iterator(), 10));
        assertEquals(Collections.emptyList(), CommonUtil.firstElements(list.iterator(), 0));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.firstElements(list.iterator(), -1));
    }

    @Test
    public void testLastElements() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        assertEquals(Arrays.asList("c", "d", "e"), CommonUtil.lastElements(list, 3));
        assertEquals(list, CommonUtil.lastElements(list, 10));
        assertEquals(Collections.emptyList(), CommonUtil.lastElements(list, 0));
        assertEquals(Collections.emptyList(), CommonUtil.lastElements(Collections.emptyList(), 3));
        assertEquals(Collections.emptyList(), CommonUtil.lastElements((Iterable<String>) null, 3));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.lastElements(list, -1));
    }

    @Test
    public void testLastElementsFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        assertEquals(Arrays.asList("c", "d", "e"), CommonUtil.lastElements(list.iterator(), 3));
        assertEquals(list, CommonUtil.lastElements(list.iterator(), 10));
        assertEquals(Collections.emptyList(), CommonUtil.lastElements(list.iterator(), 0));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.lastElements(list.iterator(), -1));
    }

    @Test
    public void testFirstNonNull2() {
        assertEquals("a", CommonUtil.firstNonNull("a", "b").get());
        assertEquals("b", CommonUtil.firstNonNull(null, "b").get());
        assertFalse(CommonUtil.firstNonNull(null, null).isPresent());
    }

    @Test
    public void testFirstNonNull3() {
        assertEquals("a", CommonUtil.firstNonNull("a", "b", "c").get());
        assertEquals("b", CommonUtil.firstNonNull(null, "b", "c").get());
        assertEquals("c", CommonUtil.firstNonNull(null, null, "c").get());
        assertFalse(CommonUtil.firstNonNull(null, null, null).isPresent());
    }

    @Test
    public void testFirstNonNullVarargs() {
        assertEquals("a", CommonUtil.firstNonNull("a", "b", "c", "d").get());
        assertEquals("c", CommonUtil.firstNonNull(null, null, "c", "d").get());
        assertFalse(CommonUtil.firstNonNull((String[]) null).isPresent());
        assertFalse(CommonUtil.firstNonNull(new String[] {}).isPresent());
        assertFalse(CommonUtil.firstNonNull(null, null, null).isPresent());
    }

    @Test
    public void testFirstNonNullIterable() {
        assertEquals("b", CommonUtil.firstNonNull(Arrays.asList(null, "b", "c")).get());
        assertFalse(CommonUtil.firstNonNull(Collections.emptyList()).isPresent());
        assertFalse(CommonUtil.firstNonNull((Iterable<String>) null).isPresent());
        assertFalse(CommonUtil.firstNonNull(Arrays.asList(null, null)).isPresent());
    }

    @Test
    public void testFirstNonNullIterator() {
        assertEquals("b", CommonUtil.firstNonNull(Arrays.asList(null, "b", "c").iterator()).get());
        assertFalse(CommonUtil.firstNonNull(Collections.emptyIterator()).isPresent());
        assertFalse(CommonUtil.firstNonNull((Iterator<String>) null).isPresent());
    }

    @Test
    public void testLastNonNull2() {
        assertEquals("b", CommonUtil.lastNonNull("a", "b").get());
        assertEquals("a", CommonUtil.lastNonNull("a", null).get());
        assertFalse(CommonUtil.lastNonNull(null, null).isPresent());
    }

    @Test
    public void testLastNonNull3() {
        assertEquals("c", CommonUtil.lastNonNull("a", "b", "c").get());
        assertEquals("b", CommonUtil.lastNonNull("a", "b", null).get());
        assertEquals("a", CommonUtil.lastNonNull("a", null, null).get());
        assertFalse(CommonUtil.lastNonNull(null, null, null).isPresent());
    }

    @Test
    public void testLastNonNullVarargs() {
        assertEquals("d", CommonUtil.lastNonNull("a", "b", "c", "d").get());
        assertEquals("b", CommonUtil.lastNonNull("a", "b", null, null).get());
        assertFalse(CommonUtil.lastNonNull((String[]) null).isPresent());
        assertFalse(CommonUtil.lastNonNull(new String[] {}).isPresent());
    }

    @Test
    public void testLastNonNullIterable() {
        assertEquals("c", CommonUtil.lastNonNull(Arrays.asList("a", "b", "c")).get());
        assertEquals("b", CommonUtil.lastNonNull(Arrays.asList("a", "b", null)).get());
        assertFalse(CommonUtil.lastNonNull(Collections.emptyList()).isPresent());
        assertFalse(CommonUtil.lastNonNull((Iterable<String>) null).isPresent());
    }

    @Test
    public void testLastNonNullIterator() {
        assertEquals("c", CommonUtil.lastNonNull(Arrays.asList("a", "b", "c").iterator()).get());
        assertEquals("b", CommonUtil.lastNonNull(Arrays.asList("a", "b", null).iterator()).get());
        assertFalse(CommonUtil.lastNonNull(Collections.emptyIterator()).isPresent());
        assertFalse(CommonUtil.lastNonNull((Iterator<String>) null).isPresent());
    }

    @Test
    public void testFirstNonEmptyArrays() {
        String[] a1 = { "a", "b" };
        String[] a2 = { "c", "d" };
        String[] empty = {};

        assertEquals(a1, CommonUtil.firstNonEmpty(a1, a2).get());
        assertEquals(a2, CommonUtil.firstNonEmpty(null, a2).get());
        assertEquals(a2, CommonUtil.firstNonEmpty(empty, a2).get());
        assertFalse(CommonUtil.firstNonEmpty((String[]) null, (String[]) null).isPresent());
        assertFalse(CommonUtil.firstNonEmpty(empty, empty).isPresent());

        assertEquals(a1, CommonUtil.firstNonEmpty(a1, a2, empty).get());
        assertEquals(a2, CommonUtil.firstNonEmpty(empty, a2, a1).get());
        assertEquals(a1, CommonUtil.firstNonEmpty(empty, empty, a1).get());
    }

    @Test
    public void testFirstNonEmptyCollections() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("c", "d");
        List<String> empty = Collections.emptyList();

        assertEquals(list1, CommonUtil.firstNonEmpty(list1, list2).get());
        assertEquals(list2, CommonUtil.firstNonEmpty(null, list2).get());
        assertEquals(list2, CommonUtil.firstNonEmpty(empty, list2).get());
        assertFalse(CommonUtil.firstNonEmpty((List<String>) null, (List<String>) null).isPresent());
        assertFalse(CommonUtil.firstNonEmpty(empty, empty).isPresent());

        assertEquals(list1, CommonUtil.firstNonEmpty(list1, list2, empty).get());
    }

    @Test
    public void testFirstNonEmptyMaps() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        Map<String, String> empty = Collections.emptyMap();

        assertEquals(map1, CommonUtil.firstNonEmpty(map1, map2).get());
        assertEquals(map2, CommonUtil.firstNonEmpty(null, map2).get());
        assertEquals(map2, CommonUtil.firstNonEmpty(empty, map2).get());
        assertFalse(CommonUtil.firstNonEmpty((Map<String, String>) null, (Map<String, String>) null).isPresent());

        assertEquals(map1, CommonUtil.firstNonEmpty(map1, map2, empty).get());
    }

    @Test
    public void testFirstNonEmptyCharSequences() {
        assertEquals("abc", CommonUtil.firstNonEmpty("abc", "def").get());
        assertEquals("def", CommonUtil.firstNonEmpty("", "def").get());
        assertEquals("def", CommonUtil.firstNonEmpty(null, "def").get());
        assertFalse(CommonUtil.firstNonEmpty("", "").isPresent());
        assertFalse(CommonUtil.firstNonEmpty((String) null, (String) null).isPresent());

        assertEquals("abc", CommonUtil.firstNonEmpty("abc", "def", "ghi").get());
        assertEquals("def", CommonUtil.firstNonEmpty("", "def", "ghi").get());
        assertEquals("ghi", CommonUtil.firstNonEmpty("", "", "ghi").get());
    }

    @Test
    public void testFirstNonEmptyCharSequencesVarargs() {
        assertEquals("abc", CommonUtil.firstNonEmpty("abc", "def", "ghi", "jkl").get());
        assertEquals("ghi", CommonUtil.firstNonEmpty("", null, "ghi", "jkl").get());
        assertFalse(CommonUtil.firstNonEmpty((String[]) null).isPresent());
        assertFalse(CommonUtil.firstNonEmpty(new String[] {}).isPresent());
        assertFalse(CommonUtil.firstNonEmpty("", null, "", null).isPresent());
    }

    @Test
    public void testFirstNonEmptyCharSequencesIterable() {
        assertEquals("abc", CommonUtil.firstNonEmpty(Arrays.asList("abc", "def")).get());
        assertEquals("def", CommonUtil.firstNonEmpty(Arrays.asList("", "def")).get());
        assertFalse(CommonUtil.firstNonEmpty(Collections.<String> emptyList()).isPresent());
        assertFalse(CommonUtil.firstNonEmpty((Iterable<String>) null).isPresent());
        assertFalse(CommonUtil.firstNonEmpty(Arrays.asList("", null, "")).isPresent());
    }

    @Test
    public void testFirstNonBlank2() {
        assertEquals("abc", CommonUtil.firstNonBlank("abc", "def").get());
        assertEquals("def", CommonUtil.firstNonBlank("  ", "def").get());
        assertEquals("def", CommonUtil.firstNonBlank(null, "def").get());
        assertFalse(CommonUtil.firstNonBlank("  ", "  ").isPresent());
        assertFalse(CommonUtil.firstNonBlank((String) null, (String) null).isPresent());
    }

    @Test
    public void testFirstNonBlank3() {
        assertEquals("abc", CommonUtil.firstNonBlank("abc", "def", "ghi").get());
        assertEquals("def", CommonUtil.firstNonBlank("  ", "def", "ghi").get());
        assertEquals("ghi", CommonUtil.firstNonBlank("  ", "  ", "ghi").get());
        assertFalse(CommonUtil.firstNonBlank("  ", null, "  ").isPresent());
    }

    @Test
    public void testFirstNonBlankVarargs() {
        assertEquals("abc", CommonUtil.firstNonBlank("abc", "def", "ghi", "jkl").get());
        assertEquals("ghi", CommonUtil.firstNonBlank("  ", null, "ghi", "jkl").get());
        assertFalse(CommonUtil.firstNonBlank((String[]) null).isPresent());
        assertFalse(CommonUtil.firstNonBlank(new String[] {}).isPresent());
        assertFalse(CommonUtil.firstNonBlank("  ", null, "  ", null).isPresent());
    }

    @Test
    public void testFirstNonBlankIterable() {
        assertEquals("abc", CommonUtil.firstNonBlank(Arrays.asList("abc", "def")).get());
        assertEquals("def", CommonUtil.firstNonBlank(Arrays.asList("  ", "def")).get());
        assertFalse(CommonUtil.firstNonBlank(Collections.<String> emptyList()).isPresent());
        assertFalse(CommonUtil.firstNonBlank((Iterable<String>) null).isPresent());
        assertFalse(CommonUtil.firstNonBlank(Arrays.asList("  ", null, "  ")).isPresent());
    }

    @Test
    public void testFirstEntry() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Map.Entry<String, Integer> entry = CommonUtil.firstEntry(map).get();
        assertEquals("a", entry.getKey());
        assertEquals(Integer.valueOf(1), entry.getValue());

        assertFalse(CommonUtil.firstEntry(Collections.<String, Integer> emptyMap()).isPresent());
        assertFalse(CommonUtil.firstEntry(null).isPresent());
    }

    @Test
    public void testLastEntry() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Map.Entry<String, Integer> entry = CommonUtil.lastEntry(map).get();
        assertEquals("c", entry.getKey());
        assertEquals(Integer.valueOf(3), entry.getValue());

        assertFalse(CommonUtil.lastEntry(Collections.<String, Integer> emptyMap()).isPresent());
        assertFalse(CommonUtil.lastEntry(null).isPresent());
    }

    @Test
    public void testFirstOrNullIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        assertEquals("a", CommonUtil.firstOrNullIfEmpty(arr));
        assertNull(CommonUtil.firstOrNullIfEmpty(new String[] {}));
        assertNull(CommonUtil.firstOrNullIfEmpty((String[]) null));
    }

    @Test
    public void testFirstOrNullIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a", CommonUtil.firstOrNullIfEmpty(list));
        assertNull(CommonUtil.firstOrNullIfEmpty(Collections.emptyList()));
        assertNull(CommonUtil.firstOrNullIfEmpty((Iterable<String>) null));
    }

    @Test
    public void testFirstOrNullIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a", CommonUtil.firstOrNullIfEmpty(list.iterator()));
        assertNull(CommonUtil.firstOrNullIfEmpty(Collections.emptyIterator()));
        assertNull(CommonUtil.firstOrNullIfEmpty((Iterator<String>) null));
    }

    @Test
    public void testFirstOrDefaultIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        assertEquals("a", CommonUtil.firstOrDefaultIfEmpty(arr, "default"));
        assertEquals("default", CommonUtil.firstOrDefaultIfEmpty(new String[] {}, "default"));
        assertEquals("default", CommonUtil.firstOrDefaultIfEmpty((String[]) null, "default"));
    }

    @Test
    public void testFirstOrDefaultIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a", CommonUtil.firstOrDefaultIfEmpty(list, "default"));
        assertEquals("default", CommonUtil.firstOrDefaultIfEmpty(Collections.<String> emptyList(), "default"));
        assertEquals("default", CommonUtil.firstOrDefaultIfEmpty((Iterable<String>) null, "default"));
    }

    @Test
    public void testFirstOrDefaultIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("a", CommonUtil.firstOrDefaultIfEmpty(list.iterator(), "default"));
        assertEquals("default", CommonUtil.firstOrDefaultIfEmpty(Collections.<String> emptyIterator(), "default"));
        assertEquals("default", CommonUtil.firstOrDefaultIfEmpty((Iterator<String>) null, "default"));
    }

    @Test
    public void testLastOrNullIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        assertEquals("c", CommonUtil.lastOrNullIfEmpty(arr));
        assertNull(CommonUtil.lastOrNullIfEmpty(new String[] {}));
        assertNull(CommonUtil.lastOrNullIfEmpty((String[]) null));
    }

    @Test
    public void testLastOrNullIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("c", CommonUtil.lastOrNullIfEmpty(list));
        assertNull(CommonUtil.lastOrNullIfEmpty(Collections.emptyList()));
        assertNull(CommonUtil.lastOrNullIfEmpty((Iterable<String>) null));
    }

    @Test
    public void testLastOrNullIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("c", CommonUtil.lastOrNullIfEmpty(list.iterator()));
        assertNull(CommonUtil.lastOrNullIfEmpty(Collections.emptyIterator()));
        assertNull(CommonUtil.lastOrNullIfEmpty((Iterator<String>) null));
    }

    @Test
    public void testLastOrDefaultIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        assertEquals("c", CommonUtil.lastOrDefaultIfEmpty(arr, "default"));
        assertEquals("default", CommonUtil.lastOrDefaultIfEmpty(new String[] {}, "default"));
        assertEquals("default", CommonUtil.lastOrDefaultIfEmpty((String[]) null, "default"));
    }

    @Test
    public void testLastOrDefaultIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("c", CommonUtil.lastOrDefaultIfEmpty(list, "default"));
        assertEquals("default", CommonUtil.lastOrDefaultIfEmpty(Collections.<String> emptyList(), "default"));
        assertEquals("default", CommonUtil.lastOrDefaultIfEmpty((Iterable<String>) null, "default"));
    }

    @Test
    public void testLastOrDefaultIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("c", CommonUtil.lastOrDefaultIfEmpty(list.iterator(), "default"));
        assertEquals("default", CommonUtil.lastOrDefaultIfEmpty(Collections.<String> emptyIterator(), "default"));
        assertEquals("default", CommonUtil.lastOrDefaultIfEmpty((Iterator<String>) null, "default"));
    }

    @Test
    public void testFindFirstArray() {
        String[] arr = { "a", "b", "c", "d" };
        assertEquals("b", CommonUtil.findFirst(arr, s -> s.equals("b")).orElse(null));
        assertEquals("c", CommonUtil.findFirst(arr, s -> s.compareTo("b") > 0).orElse(null));
        assertFalse(CommonUtil.findFirst(arr, s -> s.equals("z")).isPresent());
        assertFalse(CommonUtil.findFirst(new String[] {}, s -> true).isPresent());
        assertFalse(CommonUtil.findFirst((String[]) null, s -> true).isPresent());
    }

    @Test
    public void testFindFirstIterable() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        assertEquals("b", CommonUtil.findFirst(list, s -> s.equals("b")).orElse(null));
        assertEquals("c", CommonUtil.findFirst(list, s -> s.compareTo("b") > 0).orElse(null));
        assertFalse(CommonUtil.findFirst(list, s -> s.equals("z")).isPresent());
        assertFalse(CommonUtil.findFirst(Collections.<String> emptyList(), s -> true).isPresent());
        assertFalse(CommonUtil.findFirst((Iterable<String>) null, s -> true).isPresent());
    }

    @Test
    public void testFindFirstIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        assertEquals("b", CommonUtil.findFirst(list.iterator(), s -> s.equals("b")).orElse(null));
        assertEquals("c", CommonUtil.findFirst(list.iterator(), s -> s.compareTo("b") > 0).orElse(null));
        assertFalse(CommonUtil.findFirst(list.iterator(), s -> s.equals("z")).isPresent());
        assertFalse(CommonUtil.findFirst(Collections.<String> emptyIterator(), s -> true).isPresent());
        assertFalse(CommonUtil.findFirst((Iterator<String>) null, s -> true).isPresent());
    }

    @Test
    public void testFindLastArray() {
        String[] arr = { "a", "b", "c", "d" };
        assertEquals("b", CommonUtil.findLast(arr, s -> s.equals("b")).orElse(null));
        assertEquals("d", CommonUtil.findLast(arr, s -> s.compareTo("b") > 0).orElse(null));
        assertFalse(CommonUtil.findLast(arr, s -> s.equals("z")).isPresent());
        assertFalse(CommonUtil.findLast(new String[] {}, s -> true).isPresent());
        assertFalse(CommonUtil.findLast((String[]) null, s -> true).isPresent());
    }

    @Test
    public void testFindLastIterable() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        assertEquals("b", CommonUtil.findLast(list, s -> s.equals("b")).orElse(null));
        assertEquals("d", CommonUtil.findLast(list, s -> s.compareTo("b") > 0).orElse(null));
        assertFalse(CommonUtil.findLast(list, s -> s.equals("z")).isPresent());
        assertFalse(CommonUtil.findLast(Collections.<String> emptyList(), s -> true).isPresent());
        assertFalse(CommonUtil.findLast((Iterable<String>) null, s -> true).isPresent());
    }

    @Test
    public void testFindFirstNonNullArray() {
        String[] arr = { null, "a", null, "b", "c" };
        assertEquals("a", CommonUtil.findFirstNonNull(arr, s -> true).get());
        assertEquals("b", CommonUtil.findFirstNonNull(arr, s -> s.equals("b")).get());
        assertFalse(CommonUtil.findFirstNonNull(arr, s -> s.equals("z")).isPresent());
        assertFalse(CommonUtil.findFirstNonNull(new String[] { null, null }, s -> true).isPresent());
        assertFalse(CommonUtil.findFirstNonNull((String[]) null, s -> true).isPresent());
    }

    @Test
    public void testFindFirstNonNullIterable() {
        List<String> list = Arrays.asList(null, "a", null, "b", "c");
        assertEquals("a", CommonUtil.findFirstNonNull(list, s -> true).get());
        assertEquals("b", CommonUtil.findFirstNonNull(list, s -> s.equals("b")).get());
        assertFalse(CommonUtil.findFirstNonNull(list, s -> s.equals("z")).isPresent());
        assertFalse(CommonUtil.findFirstNonNull(Arrays.asList(null, null), s -> true).isPresent());
        assertFalse(CommonUtil.findFirstNonNull((Iterable<String>) null, s -> true).isPresent());
    }

    @Test
    public void testFindFirstNonNullIterator() {
        List<String> list = Arrays.asList(null, "a", null, "b", "c");
        assertEquals("a", CommonUtil.findFirstNonNull(list.iterator(), s -> true).get());
        assertEquals("b", CommonUtil.findFirstNonNull(list.iterator(), s -> s.equals("b")).get());
        assertFalse(CommonUtil.findFirstNonNull(list.iterator(), s -> s.equals("z")).isPresent());
        assertFalse(CommonUtil.findFirstNonNull(Arrays.asList(null, null).iterator(), s -> true).isPresent());
        assertFalse(CommonUtil.findFirstNonNull((Iterator<String>) null, s -> true).isPresent());
    }
}
