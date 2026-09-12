package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

public class CommonUtilParallelTest extends CommonUtilTestSupport {

    @Test
    public void testParallelSort_arrays() {
        CommonUtil.parallelSort((char[]) null);
        CommonUtil.parallelSort(new char[0]);
        char[] chars = { 'd', 'b', 'a', 'c' };
        CommonUtil.parallelSort(chars);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, chars);

        CommonUtil.parallelSort((byte[]) null);
        CommonUtil.parallelSort(new byte[0]);
        byte[] bytes = { 4, 2, 1, 3 };
        CommonUtil.parallelSort(bytes);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, bytes);

        CommonUtil.parallelSort((short[]) null);
        short[] shorts = { 4, 2, 1, 3 };
        CommonUtil.parallelSort(shorts);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, shorts);

        CommonUtil.parallelSort((int[]) null);
        int[] ints = { 4, 2, 1, 3 };
        CommonUtil.parallelSort(ints);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, ints);

        CommonUtil.parallelSort((long[]) null);
        long[] longs = { 4L, 2L, 1L, 3L };
        CommonUtil.parallelSort(longs);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, longs);

        CommonUtil.parallelSort((float[]) null);
        float[] floats = { 4.0f, 2.0f, 1.0f, 3.0f };
        CommonUtil.parallelSort(floats);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, floats);

        CommonUtil.parallelSort((double[]) null);
        double[] doubles = { 4.0, 2.0, 1.0, 3.0 };
        CommonUtil.parallelSort(doubles);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, doubles);

        CommonUtil.parallelSort((String[]) null);
        CommonUtil.parallelSort(new String[0]);
        String[] strings = { "d", "b", "a", "c" };
        CommonUtil.parallelSort(strings);
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, strings);

        Integer[] boxed = { 5, 2, 8, 1, 9 };
        CommonUtil.parallelSort(boxed);
        assertArrayEquals(new Integer[] { 1, 2, 5, 8, 9 }, boxed);

        String[] byLength = { "aaa", "b", "cc" };
        CommonUtil.parallelSort(byLength, Comparator.comparing(String::length));
        assertArrayEquals(new String[] { "b", "cc", "aaa" }, byLength);
    }

    @Test
    public void testParallelSort_range() {
        char[] chars = { 'e', 'd', 'c', 'b', 'a' };
        CommonUtil.parallelSort(chars, 1, 4);
        assertArrayEquals(new char[] { 'e', 'b', 'c', 'd', 'a' }, chars);

        byte[] bytes = { 5, 3, 2, 1, 4 };
        CommonUtil.parallelSort(bytes, 1, 4);
        assertArrayEquals(new byte[] { 5, 1, 2, 3, 4 }, bytes);

        short[] shorts = { 5, 3, 2, 1, 4 };
        CommonUtil.parallelSort(shorts, 1, 4);
        assertArrayEquals(new short[] { 5, 1, 2, 3, 4 }, shorts);

        int[] ints = { 5, 3, 2, 1, 4 };
        CommonUtil.parallelSort(ints, 1, 4);
        assertArrayEquals(new int[] { 5, 1, 2, 3, 4 }, ints);

        long[] longs = { 5L, 3L, 2L, 1L, 4L };
        CommonUtil.parallelSort(longs, 1, 4);
        assertArrayEquals(new long[] { 5L, 1L, 2L, 3L, 4L }, longs);

        float[] floats = { 5.0f, 3.0f, 2.0f, 1.0f, 4.0f };
        CommonUtil.parallelSort(floats, 1, 4);
        assertArrayEquals(new float[] { 5.0f, 1.0f, 2.0f, 3.0f, 4.0f }, floats);

        double[] doubles = { 5.0, 3.0, 2.0, 1.0, 4.0 };
        CommonUtil.parallelSort(doubles, 1, 4);
        assertArrayEquals(new double[] { 5.0, 1.0, 2.0, 3.0, 4.0 }, doubles);

        String[] strings = { "e", "d", "c", "b", "a" };
        CommonUtil.parallelSort(strings, 1, 4);
        assertArrayEquals(new String[] { "e", "b", "c", "d", "a" }, strings);

        String[] byLength = { "e", "aaa", "b", "cc", "d" };
        CommonUtil.parallelSort(byLength, 1, 4, Comparator.comparing(String::length));
        assertArrayEquals(new String[] { "e", "b", "cc", "aaa", "d" }, byLength);
    }

    @Test
    public void testParallelSort_listAndBy() {
        CommonUtil.parallelSort((List<String>) null);
        CommonUtil.parallelSort(new ArrayList<String>());
        List<String> list = new ArrayList<>(Arrays.asList("d", "b", "a", "c"));
        CommonUtil.parallelSort(list);
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);

        List<String> ranged = new ArrayList<>(Arrays.asList("e", "d", "c", "b", "a"));
        CommonUtil.parallelSort(ranged, 1, 4);
        assertEquals(Arrays.asList("e", "b", "c", "d", "a"), ranged);

        List<String> byLength = new ArrayList<>(Arrays.asList("aaa", "b", "cc"));
        CommonUtil.parallelSort(byLength, Comparator.comparing(String::length));
        assertEquals(Arrays.asList("b", "cc", "aaa"), byLength);

        List<String> rangedBy = new ArrayList<>(Arrays.asList("e", "aaa", "b", "cc", "d"));
        CommonUtil.parallelSort(rangedBy, 1, 4, Comparator.comparing(String::length));
        assertEquals(Arrays.asList("e", "b", "cc", "aaa", "d"), rangedBy);

        List<String> by = new ArrayList<>(Arrays.asList("apple", "zoo", "a", "be"));
        CommonUtil.parallelSortBy(by, String::length);
        assertEquals("a", by.get(0));
        assertEquals(2, by.get(1).length());

        List<String> byInt = new ArrayList<>(Arrays.asList("apple", "zoo", "a", "be"));
        CommonUtil.parallelSortByInt(byInt, String::length);
        assertEquals("a", byInt.get(0));

        List<String> byLong = new ArrayList<>(Arrays.asList("apple", "pie", "a"));
        CommonUtil.parallelSortByLong(byLong, s -> (long) s.length());
        assertEquals(Arrays.asList("a", "pie", "apple"), byLong);

        List<String> byFloat = new ArrayList<>(Arrays.asList("apple", "pie", "a"));
        CommonUtil.parallelSortByFloat(byFloat, s -> (float) s.length());
        assertEquals(Arrays.asList("a", "pie", "apple"), byFloat);

        List<String> byDouble = new ArrayList<>(Arrays.asList("apple", "pie", "a"));
        CommonUtil.parallelSortByDouble(byDouble, s -> (double) s.length());
        assertEquals(Arrays.asList("a", "pie", "apple"), byDouble);
    }
}
