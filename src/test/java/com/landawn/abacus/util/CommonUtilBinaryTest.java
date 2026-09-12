package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

public class CommonUtilBinaryTest extends CommonUtilTestSupport {

    @Test
    public void testBinarySearch_arrays() {
        assertEquals(-1, CommonUtil.binarySearch((char[]) null, 'a'));
        assertEquals(-1, CommonUtil.binarySearch(new char[0], 'a'));
        char[] chars = { 'a', 'c', 'e', 'g' };
        assertEquals(1, CommonUtil.binarySearch(chars, 'c'));
        assertTrue(CommonUtil.binarySearch(chars, 'b') < 0);
        assertEquals(2, CommonUtil.binarySearch(new char[] { 'a', 'b', 'c', 'd', 'e' }, 1, 4, 'c'));
        assertTrue(CommonUtil.binarySearch(new char[] { 'a', 'b', 'c', 'd', 'e' }, 1, 4, 'a') < 0);

        assertEquals(-1, CommonUtil.binarySearch((byte[]) null, (byte) 1));
        byte[] bytes = { 1, 3, 5, 7 };
        assertTrue(CommonUtil.binarySearch(bytes, (byte) 3) >= 0);
        assertTrue(CommonUtil.binarySearch(bytes, (byte) 2) < 0);
        assertEquals(2, CommonUtil.binarySearch(new byte[] { 1, 2, 3, 4, 5 }, 1, 4, (byte) 3));

        assertEquals(-1, CommonUtil.binarySearch((short[]) null, (short) 5));
        assertEquals(-1, CommonUtil.binarySearch(new short[0], (short) 5));
        short[] shorts = { 1, 3, 5, 7, 9 };
        assertEquals(2, CommonUtil.binarySearch(shorts, (short) 5));
        assertEquals(-1, CommonUtil.binarySearch(shorts, (short) 0));
        assertEquals(-6, CommonUtil.binarySearch(shorts, (short) 10));
        assertEquals(3, CommonUtil.binarySearch(new short[] { 1, 3, 5, 7, 9, 11, 13 }, 1, 5, (short) 7));
        assertEquals(-2, CommonUtil.binarySearch(new short[] { 1, 3, 5, 7, 9, 11, 13 }, 1, 5, (short) 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.binarySearch(shorts, 5, 2, (short) 5));
        assertEquals(-1, CommonUtil.binarySearch(new short[0], 0, 0, (short) 5));

        assertEquals(-1, CommonUtil.binarySearch((int[]) null, 5));
        int[] ints = { 1, 3, 5, 7, 9 };
        assertEquals(2, CommonUtil.binarySearch(ints, 5));
        assertEquals(-1, CommonUtil.binarySearch(ints, 0));
        assertEquals(-6, CommonUtil.binarySearch(ints, 10));
        assertEquals(3, CommonUtil.binarySearch(new int[] { 1, 3, 5, 7, 9, 11, 13 }, 1, 5, 7));
        assertEquals(-2, CommonUtil.binarySearch(new int[] { 1, 3, 5, 7, 9, 11, 13 }, 1, 5, 2));
        assertEquals(-1, CommonUtil.binarySearch(new int[0], 0, 0, 5));

        assertEquals(-1, CommonUtil.binarySearch((long[]) null, 5L));
        long[] longs = { 1L, 3L, 5L, 7L, 9L };
        assertEquals(2, CommonUtil.binarySearch(longs, 5L));
        assertEquals(-6, CommonUtil.binarySearch(longs, 10L));
        assertEquals(3, CommonUtil.binarySearch(new long[] { 1L, 3L, 5L, 7L, 9L, 11L, 13L }, 1, 5, 7L));
        assertEquals(-1, CommonUtil.binarySearch(new long[0], 0, 0, 5L));

        assertEquals(-1, CommonUtil.binarySearch((float[]) null, 5.0f));
        float[] floats = { 1.0f, 3.0f, 5.0f, 7.0f, 9.0f };
        assertEquals(2, CommonUtil.binarySearch(floats, 5.0f));
        assertEquals(-6, CommonUtil.binarySearch(floats, 10.0f));
        assertEquals(3, CommonUtil.binarySearch(new float[] { 1.0f, 3.0f, 5.0f, 7.0f, 9.0f, 11.0f, 13.0f }, 1, 5, 7.0f));

        assertEquals(-1, CommonUtil.binarySearch((double[]) null, 5.0));
        double[] doubles = { 1.0, 3.0, 5.0, 7.0, 9.0 };
        assertEquals(2, CommonUtil.binarySearch(doubles, 5.0));
        assertEquals(-6, CommonUtil.binarySearch(doubles, 10.0));
        assertEquals(3, CommonUtil.binarySearch(new double[] { 1.0, 3.0, 5.0, 7.0, 9.0, 11.0, 13.0 }, 1, 5, 7.0));

        assertEquals(-1, CommonUtil.binarySearch((String[]) null, "e"));
        String[] strings = { "a", "c", "e", "g", "i" };
        assertEquals(2, CommonUtil.binarySearch(strings, "e"));
        assertEquals(-1, CommonUtil.binarySearch(strings, ""));
        assertEquals(-6, CommonUtil.binarySearch(strings, "z"));
        assertEquals(3, CommonUtil.binarySearch(new String[] { "a", "c", "e", "g", "i", "k", "m" }, 1, 5, "g"));
        assertEquals(-2, CommonUtil.binarySearch(new String[] { "a", "c", "e", "g", "i", "k", "m" }, 1, 5, "b"));
        assertEquals(-1, CommonUtil.binarySearch(new String[0], 0, 0, "e"));

        Comparator<String> cmp = String::compareTo;
        assertEquals(2, CommonUtil.binarySearch(strings, "e", cmp));
        assertEquals(-1, CommonUtil.binarySearch(strings, "", cmp));
        assertEquals(-1, CommonUtil.binarySearch((String[]) null, "e", cmp));
        assertEquals(3, CommonUtil.binarySearch(new String[] { "a", "c", "e", "g", "i", "k", "m" }, 1, 5, "g", cmp));
        assertEquals(-2, CommonUtil.binarySearch(new String[] { "a", "c", "e", "g", "i", "k", "m" }, 1, 5, "b", cmp));
        assertEquals(-1, CommonUtil.binarySearch(new String[0], 0, 0, "e", cmp));
    }

    @Test
    public void testBinarySearch_list() {
        List<Integer> list = Arrays.asList(1, 3, 5, 7, 9);
        assertEquals(2, CommonUtil.binarySearch(list, 5));
        assertEquals(-1, CommonUtil.binarySearch(list, 0));
        assertEquals(-6, CommonUtil.binarySearch(list, 10));
        assertEquals(-1, CommonUtil.binarySearch(new ArrayList<Integer>(), 5));
        assertEquals(-1, CommonUtil.binarySearch((List<Integer>) null, 5));
        assertEquals(3, CommonUtil.binarySearch(Arrays.asList(1, 3, 5, 7, 9, 11, 13), 1, 5, 7));
        assertEquals(-2, CommonUtil.binarySearch(Arrays.asList(1, 3, 5, 7, 9, 11, 13), 1, 5, 2));
        assertEquals(-1, CommonUtil.binarySearch(new ArrayList<Integer>(), 0, 0, 5));

        List<String> words = Arrays.asList("a", "c", "e", "g", "i");
        Comparator<String> cmp = String::compareTo;
        assertEquals(2, CommonUtil.binarySearch(words, "e", cmp));
        assertEquals(-1, CommonUtil.binarySearch(words, "", cmp));
        assertEquals(-1, CommonUtil.binarySearch((List<String>) null, "e", cmp));
        assertEquals(3, CommonUtil.binarySearch(Arrays.asList("a", "c", "e", "g", "i", "k", "m"), 1, 5, "g", cmp));
        assertEquals(-2, CommonUtil.binarySearch(Arrays.asList("a", "c", "e", "g", "i", "k", "m"), 1, 5, "b", cmp));
        assertEquals(2, CommonUtil.binarySearch(Arrays.asList(1, 3, 5, 7, 9, 11), 1, 4, 5, Integer::compareTo));
        assertTrue(CommonUtil.binarySearch(Arrays.asList(1, 3, 5, 7, 9), 0, 5, 4, Integer::compareTo) < 0);
    }
}
