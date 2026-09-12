package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class NDeleteTest extends NTestSupport {

    @Test
    public void testRemoveAt_arrays() {
        assertArrayEquals(new boolean[] { false }, N.removeAt(new boolean[] { true, false }, 0));
        assertArrayEquals(new char[] { 'a', 'c', 'd' }, N.removeAt(new char[] { 'a', 'b', 'c', 'd' }, 1));
        assertArrayEquals(new byte[] { 1, 3, 4 }, N.removeAt(new byte[] { 1, 2, 3, 4 }, 1));
        assertArrayEquals(new short[] { 1, 3, 4 }, N.removeAt(new short[] { 1, 2, 3, 4 }, 1));
        assertArrayEquals(new int[] { 1, 3 }, N.removeAt(new int[] { 1, 2, 3 }, 1));
        assertArrayEquals(new long[] { 1L, 3L, 4L }, N.removeAt(new long[] { 1L, 2L, 3L, 4L }, 1));
        assertArrayEquals(new float[] { 1.0f, 3.0f, 4.0f }, N.removeAt(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, 1), DELTAf);
        assertArrayEquals(new double[] { 1.0, 3.0, 4.0 }, N.removeAt(new double[] { 1.0, 2.0, 3.0, 4.0 }, 1), DELTA);
        assertArrayEquals(new Integer[] { 1, 3, 4 }, N.removeAt(new Integer[] { 1, 2, 3, 4 }, 1));
        assertArrayEquals(new String[] { "b", "c" }, N.removeAt(new String[] { "a", "b", "c" }, 0));

        assertArrayEquals(new boolean[] { false }, N.removeAt(new boolean[] { true, false, true }, 0, 2));
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, N.removeAt(new char[] { 'a', 'b', 'c', 'd', 'e' }, 1, 3));
        assertArrayEquals(new byte[] { 1, 3, 5 }, N.removeAt(new byte[] { 1, 2, 3, 4, 5 }, 1, 3));
        assertArrayEquals(new int[] { 1, 4 }, N.removeAt(new int[] { 1, 2, 3, 4 }, 1, 2));
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, N.removeAt(new int[] { 1, 2, 3, 4 }));
        assertTrue(CommonUtil.equals(Array.of(3, 5), N.removeAt(new int[] { 1, 2, 3, 4, 5 }, 0, 0, 1, 3)));
        assertArrayEquals(new String[] { "a", "d" }, N.removeAt(new String[] { "a", "b", "c", "d" }, 1, 2));

        boolean[] toClone = { true, false };
        boolean[] cloned = N.removeAt(toClone);
        assertNotSame(toClone, cloned);
        assertArrayEquals(toClone, cloned);

        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(new boolean[] { true }, 1));
        assertThrows(IllegalArgumentException.class, () -> N.removeAt((boolean[]) null, 0));
        assertThrows(IllegalArgumentException.class, () -> N.removeAt((String[]) null, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt((String[]) null, 1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(new int[] { 1, 2, 3 }, 3));
    }

    @Test
    public void testRemoveAt_list() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        assertTrue(N.removeAt(list, 1, 3));
        assertEquals(Arrays.asList("a", "c", "e"), list);

        List<String> linked = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));
        assertTrue(N.removeAt(linked, 0, 4));
        assertEquals(Arrays.asList("b", "c", "d"), linked);

        assertFalse(N.removeAt(list, new int[0]));
        assertThrows(IllegalArgumentException.class, () -> N.removeAt((List<String>) null, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeAt(list, 10));

        List<Integer> ints = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        N.removeAt(ints, 0, 0, 1);
        assertEquals(Arrays.asList(3, 4, 5), ints);
    }

    @Test
    public void testRemoveRange() {
        assertArrayEquals(new boolean[] { true, true, false }, N.removeRange(new boolean[] { true, false, true, true, false }, 1, 3));
        assertArrayEquals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.removeRange((boolean[]) null, 0, 0));
        assertArrayEquals(new char[] { 'a', 'd', 'e' }, N.removeRange(charArray, 1, 3));
        assertArrayEquals(new byte[] { 1, 4, 5 }, N.removeRange(byteArray, 1, 3));
        assertArrayEquals(new short[] { 1, 4, 5 }, N.removeRange(shortArray, 1, 3));
        assertArrayEquals(new int[] { 1, 4, 5 }, N.removeRange(intArray, 1, 3));
        assertArrayEquals(new long[] { 1L, 4L, 5L }, N.removeRange(longArray, 1, 3));
        assertArrayEquals(new float[] { 1.0f, 4.0f, 5.0f }, N.removeRange(floatArray, 1, 3), DELTAf);
        assertArrayEquals(new double[] { 1.0, 4.0, 5.0 }, N.removeRange(doubleArray, 1, 3), DELTA);
        assertArrayEquals(new String[] { "one", "four", "five" }, N.removeRange(stringArray, 1, 3));
        assertArrayEquals(new Integer[] { 1, 4, 5 }, N.removeRange(integerArray, 1, 3));

        int[] original = { 1, 2, 3, 4, 5 };
        assertArrayEquals(new int[] { 1, 4, 5 }, N.removeRange(original, 1, 3));
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, original);
        assertArrayEquals(new int[] { 1, 2, 3 }, N.removeRange(new int[] { 1, 2, 3 }, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> N.removeRange((Integer[]) null, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeRange(new boolean[] { true }, 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeRange(new char[] { 'a' }, -1, 0));

        assertEquals("helloworld", N.removeRange("hello world", 5, 6));
        assertEquals("ac", N.removeRange("abc", 1, 2));
        assertEquals("", N.removeRange("abc", 0, 3));
        assertEquals(null, N.removeRange((String) null, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeRange("a", 0, 2));

        List<Integer> list = new ArrayList<>(integerList);
        assertTrue(N.removeRange(list, 1, 3));
        assertEquals(Arrays.asList(1, 4, 5), list);
        assertFalse(N.removeRange(new ArrayList<Integer>(), 0, 0));

        List<String> linked = new LinkedList<>(Arrays.asList("a", "b", "c", "d"));
        assertTrue(N.removeRange(linked, 1, 3));
        assertEquals(Arrays.asList("a", "d"), linked);
        assertThrows(IndexOutOfBoundsException.class, () -> N.removeRange(toMutableList("a"), 1, 0));
        N.removeRange((List<String>) null, 0, 0);
    }
}
