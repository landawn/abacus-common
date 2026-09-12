package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import org.junit.jupiter.api.Test;

public class CommonUtilCopyTest extends CommonUtilTestSupport {

    @Test
    public void testCopy_arrays() {
        boolean[] boolSrc = { true, false, true };
        boolean[] boolDest = new boolean[3];
        CommonUtil.copy(boolSrc, 0, boolDest, 0, 3);
        assertTrue(CommonUtil.equals(boolSrc, boolDest));

        char[] charDest = new char[5];
        CommonUtil.copy(new char[] { 'a', 'b', 'c', 'd', 'e' }, 1, charDest, 0, 3);
        assertArrayEquals(new char[] { 'b', 'c', 'd', '\0', '\0' }, charDest);

        byte[] byteDest = new byte[5];
        CommonUtil.copy(new byte[] { 1, 2, 3, 4, 5 }, 2, byteDest, 1, 3);
        assertArrayEquals(new byte[] { 0, 3, 4, 5, 0 }, byteDest);

        short[] shortDest = new short[5];
        CommonUtil.copy(new short[] { 10, 20, 30, 40, 50 }, 0, shortDest, 2, 2);
        assertArrayEquals(new short[] { 0, 0, 10, 20, 0 }, shortDest);

        int[] intDest = new int[5];
        CommonUtil.copy(new int[] { 100, 200, 300, 400, 500 }, 1, intDest, 1, 3);
        assertArrayEquals(new int[] { 0, 200, 300, 400, 0 }, intDest);

        long[] longDest = new long[5];
        CommonUtil.copy(new long[] { 1000L, 2000L, 3000L, 4000L, 5000L }, 2, longDest, 0, 3);
        assertArrayEquals(new long[] { 3000L, 4000L, 5000L, 0L, 0L }, longDest);

        float[] floatSrc = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] floatDest = new float[5];
        CommonUtil.copy(floatSrc, 0, floatDest, 0, 5);
        assertArrayEquals(floatSrc, floatDest);

        double[] doubleDest = new double[5];
        CommonUtil.copy(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 3, doubleDest, 3, 2);
        assertArrayEquals(new double[] { 0.0, 0.0, 0.0, 4.0, 5.0 }, doubleDest);

        String[] objDest = new String[5];
        CommonUtil.copy(new String[] { "a", "b", "c", "d", "e" }, 1, objDest, 2, 2);
        assertArrayEquals(new String[] { null, null, "b", "c", null }, objDest);

        int[] genericDest = new int[5];
        CommonUtil.copy((Object) new int[] { 1, 2, 3, 4, 5 }, 0, (Object) genericDest, 1, 4);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, genericDest);

        boolean[] emptyDest = new boolean[5];
        CommonUtil.copy(new boolean[0], 0, emptyDest, 0, 0);
        assertArrayEquals(new boolean[5], emptyDest);

        boolean[] overlap = { true, false, true, false, true };
        CommonUtil.copy(overlap, 0, overlap, 2, 3);
        assertArrayEquals(new boolean[] { true, false, true, false, true }, overlap);
    }

    @Test
    public void testCopyOf() {
        boolean[] boolOrig = { true, false, true };
        assertArrayEquals(new boolean[] { true, false, true, false, false }, CommonUtil.copyOf(boolOrig, 5));
        assertArrayEquals(new boolean[] { true, false, true }, CommonUtil.copyOf(new boolean[] { true, false, true, false, true }, 3));
        assertNotSame(boolOrig, CommonUtil.copyOf(boolOrig, 3));

        assertArrayEquals(new char[] { 'a', 'b', 'c', '\0', '\0' }, CommonUtil.copyOf(new char[] { 'a', 'b', 'c' }, 5));
        assertArrayEquals(new byte[] { 1, 2 }, CommonUtil.copyOf(new byte[] { 1, 2, 3 }, 2));
        assertArrayEquals(new short[] { 10, 20, 30, 0 }, CommonUtil.copyOf(new short[] { 10, 20, 30 }, 4));

        int[] intOrig = { 100, 200, 300 };
        int[] intCopy = CommonUtil.copyOf(intOrig, 3);
        assertArrayEquals(intOrig, intCopy);
        assertNotSame(intOrig, intCopy);

        assertArrayEquals(new long[] { 1000L, 2000L, 3000L, 0L, 0L }, CommonUtil.copyOf(new long[] { 1000L, 2000L, 3000L }, 5));
        assertArrayEquals(new float[] { 1.0f, 2.0f }, CommonUtil.copyOf(new float[] { 1.0f, 2.0f, 3.0f }, 2));
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 0.0 }, CommonUtil.copyOf(new double[] { 1.0, 2.0, 3.0 }, 4));

        String[] strOrig = { "a", "b", "c" };
        assertArrayEquals(new String[] { "a", "b", "c", null, null }, CommonUtil.copyOf(strOrig, 5));
        assertNotSame(strOrig, CommonUtil.copyOf(strOrig, 3));

        Integer[] typed = CommonUtil.copyOf(new Number[] { 1, 2, 3 }, 4, Integer[].class);
        assertEquals(4, typed.length);
        assertEquals(1, typed[0]);
        assertNull(typed[3]);
    }

    @Test
    public void testCopyOfRange_arrays() {
        boolean[] boolFull = { true, false, true };
        boolean[] boolFullCopy = CommonUtil.copyOfRange(boolFull, 0, 3);
        assertArrayEquals(boolFull, boolFullCopy);
        assertNotSame(boolFull, boolFullCopy);
        assertArrayEquals(new boolean[] { false, true, false }, CommonUtil.copyOfRange(new boolean[] { true, false, true, false, true }, 1, 4));

        assertArrayEquals(new char[] { 'b', 'c', 'd' }, CommonUtil.copyOfRange(new char[] { 'a', 'b', 'c', 'd', 'e' }, 1, 4));
        assertArrayEquals(new byte[] { 3, 4, 5 }, CommonUtil.copyOfRange(new byte[] { 1, 2, 3, 4, 5 }, 2, 5));
        assertArrayEquals(new short[] { 10, 20, 30 }, CommonUtil.copyOfRange(new short[] { 10, 20, 30, 40, 50 }, 0, 3));
        assertArrayEquals(new int[] { 200, 300 }, CommonUtil.copyOfRange(new int[] { 100, 200, 300, 400, 500 }, 1, 3));
        assertArrayEquals(new long[] { 3000L, 4000L }, CommonUtil.copyOfRange(new long[] { 1000L, 2000L, 3000L, 4000L, 5000L }, 2, 4));
        assertArrayEquals(new float[] { 4.0f, 5.0f }, CommonUtil.copyOfRange(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }, 3, 5));
        assertArrayEquals(new double[] { 1.0, 2.0 }, CommonUtil.copyOfRange(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 0, 2));
        assertArrayEquals(new String[] { "b", "c", "d" }, CommonUtil.copyOfRange(new String[] { "a", "b", "c", "d", "e" }, 1, 4));
        assertArrayEquals(new Integer[] { 2, 3, 4 }, CommonUtil.copyOfRange(new Number[] { 1, 2, 3, 4, 5 }, 1, 4, Integer[].class));
    }

    @Test
    public void testCopyOfRange_withStep() {
        assertArrayEquals(new boolean[] { true, true, true }, CommonUtil.copyOfRange(new boolean[] { true, false, true, false, true, false }, 0, 6, 2));
        assertArrayEquals(new boolean[] { true, false, true }, CommonUtil.copyOfRange(new boolean[] { true, false, true, false, true }, 4, 1, -1));
        assertArrayEquals(new boolean[] { true, false, true, false, true },
                CommonUtil.copyOfRange(new boolean[] { true, false, true, false, true }, 4, -1, -1));

        assertArrayEquals(new char[] { 'b', 'd', 'f' }, CommonUtil.copyOfRange(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, 1, 6, 2));
        assertArrayEquals(new byte[] { 1, 3, 5 }, CommonUtil.copyOfRange(new byte[] { 1, 2, 3, 4, 5, 6 }, 0, 5, 2));
        assertArrayEquals(new byte[] { 5, 4, 3 }, CommonUtil.copyOfRange(new byte[] { 1, 2, 3, 4, 5 }, 4, 1, -1));
        assertArrayEquals(new short[] { 20, 40 }, CommonUtil.copyOfRange(new short[] { 10, 20, 30, 40, 50, 60 }, 1, 5, 2));
        assertArrayEquals(new int[] { 100, 400 }, CommonUtil.copyOfRange(new int[] { 100, 200, 300, 400, 500, 600 }, 0, 6, 3));
        assertArrayEquals(new long[] { 5000L, 3000L }, CommonUtil.copyOfRange(new long[] { 1000L, 2000L, 3000L, 4000L, 5000L }, 4, 0, -2));
        assertArrayEquals(new float[] { 1.0f, 3.0f, 5.0f }, CommonUtil.copyOfRange(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, 0, 6, 2));
        assertArrayEquals(new double[] { 2.0, 4.0 }, CommonUtil.copyOfRange(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, 1, 5, 2));
        assertArrayEquals(new String[] { "a", "c", "e" }, CommonUtil.copyOfRange(new String[] { "a", "b", "c", "d", "e", "f" }, 0, 6, 2));
        assertArrayEquals(new Integer[] { 1, 4 }, CommonUtil.copyOfRange(new Number[] { 1, 2, 3, 4, 5, 6 }, 0, 6, 3, Integer[].class));
        assertArrayEquals(new String[] { "a", "c", "e" }, CommonUtil.copyOfRange(new String[] { "a", "b", "c", "d", "e" }, 0, 5, 2, String[].class));
    }

    @Test
    public void testCopyOfRange_listAndString() {
        assertEquals(Arrays.asList("b", "c", "d"), CommonUtil.copyOfRange(Arrays.asList("a", "b", "c", "d", "e"), 1, 4));
        assertEquals(Arrays.asList(1, 3, 5), CommonUtil.copyOfRange(Arrays.asList(1, 2, 3, 4, 5, 6), 0, 6, 2));
        assertEquals(Arrays.asList(1, 4, 7), CommonUtil.copyOfRange(Arrays.asList(1, 2, 3, 4, 5, 6, 7), 0, 7, 3));
        assertEquals(0, CommonUtil.copyOfRange(Arrays.asList(1, 2, 3), 2, 2, 1).size());
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange(Arrays.asList(1, 2, 3), 0, 3, 0));

        LinkedList<Integer> linked = new LinkedList<>(Arrays.asList(10, 20, 30, 40, 50));
        assertEquals(Arrays.asList(20, 30, 40), CommonUtil.copyOfRange(linked, 1, 4, 1));
        assertEquals(Arrays.asList(1, 3, 5), CommonUtil.copyOfRange(new LinkedList<>(Arrays.asList(1, 2, 3, 4, 5, 6)), 0, 6, 2));

        LinkedList<Integer> ten = new LinkedList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        assertEquals(Arrays.asList(9, 8, 7, 6, 5, 4, 3, 2, 1, 0), CommonUtil.copyOfRange(ten, 9, -1, -1));
        assertEquals(Arrays.asList(9, 7, 5, 3, 1), CommonUtil.copyOfRange(ten, 9, -1, -2));
        assertEquals(Arrays.asList(9, 6, 3, 0), CommonUtil.copyOfRange(ten, 9, -1, -3));
        assertEquals(Arrays.asList(0, 2, 4, 6, 8), CommonUtil.copyOfRange(ten, 0, 10, 2));
        assertTrue(CommonUtil.copyOfRange(new LinkedList<>(Arrays.asList(1, 2, 3)), 2, 2, -1).isEmpty());

        ArrayList<Integer> arrayList = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
        assertEquals(Arrays.asList(9, 8, 7, 6, 5, 4, 3, 2, 1, 0), CommonUtil.copyOfRange(arrayList, 9, -1, -1));

        ArrayList<String> a = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        LinkedList<String> l = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));
        assertEquals(CommonUtil.copyOfRange(a, 4, -1, -1), CommonUtil.copyOfRange(l, 4, -1, -1));
        assertEquals(Arrays.asList("e", "d", "c", "b", "a"), CommonUtil.copyOfRange(a, 4, -1, -1));
        assertEquals(Arrays.asList("e", "c", "a"), CommonUtil.copyOfRange(a, 4, -1, -2));
        assertEquals(CommonUtil.copyOfRange(a, 4, -1, -2), CommonUtil.copyOfRange(l, 4, -1, -2));

        assertEquals("bcd", CommonUtil.copyOfRange("abcde", 1, 4));
        assertEquals("abcde", CommonUtil.copyOfRange("abcde", 0, 5));
        assertEquals("ace", CommonUtil.copyOfRange("abcdef", 0, 6, 2));
        assertEquals("edc", CommonUtil.copyOfRange("abcde", 4, 1, -1));
    }

    @Test
    public void testCopyOfRange_steppedOverloadsAcceptFromIndexGreaterThanToIndex() {
        // Pins the class-javadoc "Index Conventions" bullet: the stepped copyOfRange overloads are the documented
        // exception to "fromIndex > toIndex is rejected" - a negative step copies in reverse and honours the
        // toIndex == -1 sentinel, a positive step yields an empty result.
        assertArrayEquals(new int[] { 5, 4, 3 }, N.copyOfRange(new int[] { 1, 2, 3, 4, 5 }, 4, 1, -1));
        assertArrayEquals(new int[] { 3, 2, 1 }, N.copyOfRange(new int[] { 1, 2, 3 }, 2, -1, -1));
        assertArrayEquals(new int[] {}, N.copyOfRange(new int[] { 1, 2, 3, 4, 5 }, 4, 1, 2));
        assertEquals("edc", N.copyOfRange("abcde", 4, 1, -1));
    }
}
