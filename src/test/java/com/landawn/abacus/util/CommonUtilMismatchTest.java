package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CommonUtilMismatchTest extends CommonUtilTestSupport {
    @Test
    public void testMismatch_BooleanArray() {
        boolean[] a = { true, false, true };
        boolean[] b = { true, false, false };

        assertEquals(2, CommonUtil.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatch_BooleanArray_WithRange() {
        boolean[] a = { true, false, true, false };
        boolean[] b = { false, false, true, true };

        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_ByteArray() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 1, 2, 4 };

        assertEquals(2, CommonUtil.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatch_ByteArray_WithRange() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] b = { 0, 2, 3, 0 };

        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_CharArray() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'a', 'b', 'd' };

        assertEquals(2, CommonUtil.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatch_CharArray_WithRange() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] b = { 'x', 'b', 'c', 'y' };

        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_ShortArray() {
        short[] a = { 1, 2, 3 };
        short[] b = { 1, 2, 4 };

        assertEquals(2, CommonUtil.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatch_ShortArray_WithRange() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 0, 2, 3, 0 };

        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_IntArray() {
        int[] a = { 1, 2, 3 };
        int[] b = { 1, 2, 4 };

        assertEquals(2, CommonUtil.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatch_IntArray_WithRange() {
        int[] a = { 1, 2, 3, 4 };
        int[] b = { 0, 2, 3, 0 };

        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_LongArray() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 1L, 2L, 4L };

        assertEquals(2, CommonUtil.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatch_LongArray_WithRange() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 0L, 2L, 3L, 0L };

        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_FloatArray() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        float[] b = { 1.0f, 2.0f, 4.0f };

        assertEquals(2, CommonUtil.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatch_FloatArray_WithRange() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] b = { 0.0f, 2.0f, 3.0f, 0.0f };

        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_DoubleArray() {
        double[] a = { 1.0, 2.0, 3.0 };
        double[] b = { 1.0, 2.0, 4.0 };

        assertEquals(2, CommonUtil.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatch_DoubleArray_WithRange() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] b = { 0.0, 2.0, 3.0, 0.0 };

        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_Collection_WithRange() {
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("x", "b", "c", "y");

        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_ObjectArray_WithKeyExtractor() {
        String[] a = { "a", "b", "c" };
        String[] b = { "A", "B", "D" };

        assertEquals(2, CommonUtil.mismatch(a, b, String::toLowerCase));
    }

    @Test
    public void testMismatch_ObjectArray_WithRange_AndKeyExtractor() {
        String[] a = { "x", "b", "c", "y" };
        String[] b = { "z", "B", "C", "w" };

        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2, String::toLowerCase));
    }

    @Test
    public void testMismatch_Iterable_WithKeyExtractor() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<String> b = Arrays.asList("A", "B", "D");

        assertEquals(2, CommonUtil.mismatch(a, b, String::toLowerCase));
    }

    @Test
    public void testMismatch_Iterator_WithKeyExtractor() {
        Iterator<String> a = Arrays.asList("a", "b", "c").iterator();
        Iterator<String> b = Arrays.asList("A", "B", "D").iterator();

        assertEquals(2, CommonUtil.mismatch(a, b, String::toLowerCase));
    }

    @Test
    public void testMismatch_Collection_WithRange_AndKeyExtractor() {
        List<String> a = Arrays.asList("x", "b", "c", "y");
        List<String> b = Arrays.asList("z", "B", "D", "w");

        assertEquals(1, CommonUtil.mismatch(a, 1, b, 1, 3, String::toLowerCase));
    }

    @Test
    public void testMismatchBoolean() {
        boolean[] a = { true, false, true, false };
        boolean[] b = { true, false, true, false };
        boolean[] c = { true, true, false, false };
        boolean[] d = { true, false, true };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c));
        Assertions.assertEquals(3, CommonUtil.mismatch(a, d));
        Assertions.assertEquals(-1, CommonUtil.mismatch((boolean[]) null, (boolean[]) null));
        Assertions.assertEquals(-1, CommonUtil.mismatch(new boolean[0], new boolean[0]));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, null));
        Assertions.assertEquals(0, CommonUtil.mismatch(null, a));

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatchChar() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] b = { 'a', 'b', 'c', 'd' };
        char[] c = { 'a', 'x', 'c', 'd' };
        char[] d = { 'a', 'b', 'c' };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c));
        Assertions.assertEquals(3, CommonUtil.mismatch(a, d));
        Assertions.assertEquals(-1, CommonUtil.mismatch((char[]) null, (char[]) null));
        Assertions.assertEquals(-1, CommonUtil.mismatch(new char[0], new char[0]));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, null));
        Assertions.assertEquals(0, CommonUtil.mismatch(null, a));
    }

    @Test
    public void testMismatchByte() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] b = { 1, 2, 3, 4 };
        byte[] c = { 1, 5, 3, 4 };
        byte[] d = { 1, 2, 3 };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c));
        Assertions.assertEquals(3, CommonUtil.mismatch(a, d));
        Assertions.assertEquals(-1, CommonUtil.mismatch((byte[]) null, (byte[]) null));
        Assertions.assertEquals(-1, CommonUtil.mismatch(new byte[0], new byte[0]));
    }

    @Test
    public void testMismatchShort() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 1, 2, 3, 4 };
        short[] c = { 1, 5, 3, 4 };
        short[] d = { 1, 2, 3 };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c));
        Assertions.assertEquals(3, CommonUtil.mismatch(a, d));
        Assertions.assertEquals(-1, CommonUtil.mismatch((short[]) null, (short[]) null));
        Assertions.assertEquals(-1, CommonUtil.mismatch(new short[0], new short[0]));
    }

    @Test
    public void testMismatchInt() {
        int[] a = { 1, 2, 3, 4 };
        int[] b = { 1, 2, 3, 4 };
        int[] c = { 1, 5, 3, 4 };
        int[] d = { 1, 2, 3 };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c));
        Assertions.assertEquals(3, CommonUtil.mismatch(a, d));
        Assertions.assertEquals(-1, CommonUtil.mismatch((int[]) null, (int[]) null));
        Assertions.assertEquals(-1, CommonUtil.mismatch(new int[0], new int[0]));
    }

    @Test
    public void testMismatchLong() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 1L, 2L, 3L, 4L };
        long[] c = { 1L, 5L, 3L, 4L };
        long[] d = { 1L, 2L, 3L };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c));
        Assertions.assertEquals(3, CommonUtil.mismatch(a, d));
        Assertions.assertEquals(-1, CommonUtil.mismatch((long[]) null, (long[]) null));
        Assertions.assertEquals(-1, CommonUtil.mismatch(new long[0], new long[0]));
    }

    @Test
    public void testMismatchFloat() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] b = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] c = { 1.0f, 5.0f, 3.0f, 4.0f };
        float[] d = { 1.0f, 2.0f, 3.0f };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c));
        Assertions.assertEquals(3, CommonUtil.mismatch(a, d));
        Assertions.assertEquals(-1, CommonUtil.mismatch((float[]) null, (float[]) null));
        Assertions.assertEquals(-1, CommonUtil.mismatch(new float[0], new float[0]));
    }

    @Test
    public void testMismatchDouble() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] b = { 1.0, 2.0, 3.0, 4.0 };
        double[] c = { 1.0, 5.0, 3.0, 4.0 };
        double[] d = { 1.0, 2.0, 3.0 };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c));
        Assertions.assertEquals(3, CommonUtil.mismatch(a, d));
        Assertions.assertEquals(-1, CommonUtil.mismatch((double[]) null, (double[]) null));
        Assertions.assertEquals(-1, CommonUtil.mismatch(new double[0], new double[0]));
    }

    @Test
    public void testMismatchObjectArrays() {
        String[] a = { "a", "b", "c", "d" };
        String[] b = { "a", "b", "c", "d" };
        String[] c = { "a", "x", "c", "d" };
        String[] d = { "a", "b", "c" };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c));
        Assertions.assertEquals(3, CommonUtil.mismatch(a, d));
        Assertions.assertEquals(-1, CommonUtil.mismatch((String[]) null, (String[]) null));
        Assertions.assertEquals(-1, CommonUtil.mismatch(new String[0], new String[0]));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, null));
        Assertions.assertEquals(0, CommonUtil.mismatch(null, a));
    }

    @Test
    public void testMismatchObjectArraysWithKeyExtractor() {
        String[] a = { "A", "B", "C", "D" };
        String[] b = { "a", "b", "c", "d" };
        String[] c = { "a", "x", "c", "d" };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b, String::toLowerCase));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c, String::toLowerCase));
        Assertions.assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatchCollectionsWithKeyExtractor() {
        List<String> a = Arrays.asList("A", "B", "C", "D");
        List<String> b = Arrays.asList("x", "b", "c", "y");

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2, String::toLowerCase));
        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, a, 1, 2, String::toLowerCase));
    }

    @Test
    public void testMismatchIterables() {
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("a", "b", "c", "d");
        List<String> c = Arrays.asList("a", "x", "c", "d");
        List<String> d = Arrays.asList("a", "b", "c");

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c));
        Assertions.assertEquals(3, CommonUtil.mismatch(a, d));
        Assertions.assertEquals(-1, CommonUtil.mismatch((Iterable<String>) null, (Iterable<String>) null));
        Assertions.assertEquals(-1, CommonUtil.mismatch(new ArrayList<String>(), new ArrayList<String>()));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, (Iterable<String>) null));
        Assertions.assertEquals(0, CommonUtil.mismatch((Iterable<String>) null, a));

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatchIterablesWithKeyExtractor() {
        List<String> a = Arrays.asList("A", "B", "C", "D");
        List<String> b = Arrays.asList("a", "b", "c", "d");
        List<String> c = Arrays.asList("a", "x", "c", "d");

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b, String::toLowerCase));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c, String::toLowerCase));
    }

    @Test
    public void testMismatchIterators() {
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("a", "b", "c", "d");
        List<String> c = Arrays.asList("a", "x", "c", "d");
        List<String> d = Arrays.asList("a", "b", "c");
        List<String> e = Arrays.asList("a", "b", "c", "d", "e");

        Assertions.assertEquals(-1, CommonUtil.mismatch(a.iterator(), b.iterator()));
        Assertions.assertEquals(1, CommonUtil.mismatch(a.iterator(), c.iterator()));
        Assertions.assertEquals(3, CommonUtil.mismatch(a.iterator(), d.iterator()));
        Assertions.assertEquals(4, CommonUtil.mismatch(a.iterator(), e.iterator()));
        Assertions.assertEquals(-1, CommonUtil.mismatch((Iterator<String>) null, (Iterator<String>) null));

        Iterator<String> iter = a.iterator();
        Assertions.assertEquals(-1, CommonUtil.mismatch(iter, iter));
    }

    @Test
    public void testMismatchIteratorsWithKeyExtractor() {
        List<String> a = Arrays.asList("A", "B", "C", "D");
        List<String> b = Arrays.asList("a", "b", "c", "d");
        List<String> c = Arrays.asList("a", "x", "c", "d");

        Assertions.assertEquals(-1, CommonUtil.mismatch(a.iterator(), b.iterator(), String::toLowerCase));
        Assertions.assertEquals(1, CommonUtil.mismatch(a.iterator(), c.iterator(), String::toLowerCase));
    }

    @Test
    public void testMismatch_Collection_WithFromIndex_AndKeyExtractor() {
        List<String> a = Arrays.asList("x", "a", "b", "c", "y");
        List<String> b = Arrays.asList("z", "a", "b", "d", "w");
        // Compare [a,b,c] vs [a,b,d] - mismatch at index 2 (relative)
        assertEquals(2, CommonUtil.mismatch(a, 1, b, 1, 3, Function.identity()));
    }

    @Test
    public void testMismatch_Collection_WithFromIndex_NoMismatch() {
        List<String> a = Arrays.asList("x", "a", "b", "c", "y");
        List<String> b = Arrays.asList("z", "a", "b", "c", "w");
        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 3, Function.identity()));
    }

    @Test
    public void testMismatch_Collection_SameRef_ReturnsMinus1() {
        List<String> a = Arrays.asList("x", "a", "b");
        assertEquals(-1, CommonUtil.mismatch(a, 0, a, 0, 2, Function.identity()));
    }

    @Test
    public void testMismatchBooleanRange() {
        boolean[] a = { true, false, true, false };
        boolean[] b = { false, false, true, true };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.mismatch(a, 0, b, 0, -1));

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, a, 1, 2));

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 0, b, 0, 0));
    }

    @Test
    public void testMismatchCharRange() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] b = { 'x', 'b', 'c', 'y' };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchByteRange() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] b = { 5, 2, 3, 6 };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchShortRange() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 5, 2, 3, 6 };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchIntRange() {
        int[] a = { 1, 2, 3, 4 };
        int[] b = { 5, 2, 3, 6 };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchLongRange() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 5L, 2L, 3L, 6L };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchFloatRange() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] b = { 5.0f, 2.0f, 3.0f, 6.0f };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchDoubleRange() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] b = { 5.0, 2.0, 3.0, 6.0 };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchObjectArraysRange() {
        String[] a = { "a", "b", "c", "d" };
        String[] b = { "x", "b", "c", "y" };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchObjectArraysRangeWithKeyExtractor() {
        String[] a = { "A", "B", "C", "D" };
        String[] b = { "x", "b", "c", "y" };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2, String::toLowerCase));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10, String::toLowerCase));
    }

    @Test
    public void testMismatchCollections() {
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("x", "b", "c", "y");

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10));
    }
}
