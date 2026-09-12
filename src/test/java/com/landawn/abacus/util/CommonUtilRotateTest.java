package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.Iterator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CommonUtilRotateTest extends CommonUtilTestSupport {
    @Test
    public void testRotate_BooleanArray() {
        boolean[] arr = { true, false, true, false };
        CommonUtil.rotate(arr, 1);
        boolean[] expected = { false, true, false, true };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testRotate_IntArray() {
        int[] arr = { 1, 2, 3, 4 };
        CommonUtil.rotate(arr, 1);
        int[] expected = { 4, 1, 2, 3 };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testRotate_IntArray_WithRange() {
        int[] arr = { 1, 2, 3, 4, 5 };
        CommonUtil.rotate(arr, 1, 4, 1);
        int[] expected = { 1, 4, 2, 3, 5 };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testRotate_ObjectArray() {
        String[] arr = { "a", "b", "c", "d" };
        CommonUtil.rotate(arr, 1);
        String[] expected = { "d", "a", "b", "c" };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testRotate_List() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.rotate(list, 1);
        assertEquals(Arrays.asList("d", "a", "b", "c"), list);
    }

    @Test
    public void testRotateChar() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] expected = { 'd', 'a', 'b', 'c' };
        CommonUtil.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);

        char[] b = { 'a', 'b', 'c', 'd' };
        char[] expected2 = { 'b', 'c', 'd', 'a' };
        CommonUtil.rotate(b, -1);
        Assertions.assertArrayEquals(expected2, b);

        char[] c = { 'a', 'b', 'c', 'd' };
        char[] expected3 = { 'b', 'c', 'd', 'a' };
        CommonUtil.rotate(c, 7);
        Assertions.assertArrayEquals(expected3, c);
    }

    @Test
    public void testRotateCharRange() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        char[] expected = { 'a', 'd', 'b', 'c', 'e' };
        CommonUtil.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateByte() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] expected = { 4, 1, 2, 3 };
        CommonUtil.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateByteRange() {
        byte[] a = { 1, 2, 3, 4, 5 };
        byte[] expected = { 1, 4, 2, 3, 5 };
        CommonUtil.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateShort() {
        short[] a = { 1, 2, 3, 4 };
        short[] expected = { 4, 1, 2, 3 };
        CommonUtil.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateShortRange() {
        short[] a = { 1, 2, 3, 4, 5 };
        short[] expected = { 1, 4, 2, 3, 5 };
        CommonUtil.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateInt() {
        int[] a = { 1, 2, 3, 4, 5 };
        int[] expected = { 4, 5, 1, 2, 3 };
        CommonUtil.rotate(a, 2);
        Assertions.assertArrayEquals(expected, a);

        int[] b = { 1, 2, 3, 4, 5 };
        int[] expected2 = { 2, 3, 4, 5, 1 };
        CommonUtil.rotate(b, -1);
        Assertions.assertArrayEquals(expected2, b);

        int[] c = { 1, 2, 3, 4, 5 };
        int[] expected3 = { 4, 5, 1, 2, 3 };
        CommonUtil.rotate(c, 7);
        Assertions.assertArrayEquals(expected3, c);
    }

    @Test
    public void testRotateIntRange() {

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 2, 3, 4, 5 };
            CommonUtil.rotate(a, 0);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 5, 1, 2, 3, 4 };
            CommonUtil.rotate(a, 1);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 4, 5, 1, 2, 3 };
            CommonUtil.rotate(a, 2);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 3, 4, 5, 1, 2 };
            CommonUtil.rotate(a, 3);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 2, 3, 4, 5, 1 };
            CommonUtil.rotate(a, 4);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 2, 3, 4, 5 };
            CommonUtil.rotate(a, 5);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 5, 1, 2, 3, 4 };
            CommonUtil.rotate(a, 6);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 2, 3, 4, 5, 1 };
            CommonUtil.rotate(a, -1);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 3, 4, 5, 1, 2 };
            CommonUtil.rotate(a, -2);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 4, 2, 3, 5 };
            CommonUtil.rotate(a, 1, 4, 1);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 3, 4, 2, 5 };
            CommonUtil.rotate(a, 1, 4, 2);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 2, 3, 4, 5 };
            CommonUtil.rotate(a, 1, 4, 3);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 3, 4, 2, 5 };
            CommonUtil.rotate(a, 1, 4, -1);
            Assertions.assertArrayEquals(expected, a);
        }
    }

    @Test
    public void testRotateLong() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] expected = { 4L, 1L, 2L, 3L };
        CommonUtil.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateLongRange() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        long[] expected = { 1L, 4L, 2L, 3L, 5L };
        CommonUtil.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateFloat() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] expected = { 4.0f, 1.0f, 2.0f, 3.0f };
        CommonUtil.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a, 0.0f);
    }

    @Test
    public void testRotateFloatRange() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] expected = { 1.0f, 4.0f, 2.0f, 3.0f, 5.0f };
        CommonUtil.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a, 0.0f);
    }

    @Test
    public void testRotateDouble() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] expected = { 4.0, 1.0, 2.0, 3.0 };
        CommonUtil.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a, 0.0);
    }

    @Test
    public void testRotateDoubleRange() {
        double[] a = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] expected = { 1.0, 4.0, 2.0, 3.0, 5.0 };
        CommonUtil.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a, 0.0);
    }

    @Test
    public void testRotateObject() {
        String[] a = { "a", "b", "c", "d" };
        String[] expected = { "d", "a", "b", "c" };
        CommonUtil.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateObjectRange() {
        String[] a = { "a", "b", "c", "d", "e" };
        String[] expected = { "a", "d", "b", "c", "e" };
        CommonUtil.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotate_CharArray_WithRange() {
        char[] arr = { 'a', 'b', 'c', 'd', 'e' };
        CommonUtil.rotate(arr, 1, 4, 1);
        assertArrayEquals(new char[] { 'a', 'd', 'b', 'c', 'e' }, arr);
    }

    @Test
    public void testRotate_ShortArray_WithRange() {
        short[] arr = { 1, 2, 3, 4, 5 };
        CommonUtil.rotate(arr, 1, 4, 1);
        assertArrayEquals(new short[] { 1, 4, 2, 3, 5 }, arr);
    }

    @Test
    public void testRotate_LongArray_WithRange() {
        long[] arr = { 1L, 2L, 3L, 4L, 5L };
        CommonUtil.rotate(arr, 1, 4, 1);
        assertArrayEquals(new long[] { 1L, 4L, 2L, 3L, 5L }, arr);
    }

    @Test
    public void testRotate_FloatArray_WithRange() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        CommonUtil.rotate(arr, 1, 4, 1);
        assertArrayEquals(new float[] { 1.0f, 4.0f, 2.0f, 3.0f, 5.0f }, arr, 0.0f);
    }

    @Test
    public void testRotate_DoubleArray_WithRange() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        CommonUtil.rotate(arr, 1, 4, 1);
        assertArrayEquals(new double[] { 1.0, 4.0, 2.0, 3.0, 5.0 }, arr, 0.0);
    }

    @Test
    public void testRotate_IntArray_NegativeDistance() {
        int[] arr = { 1, 2, 3, 4 };
        CommonUtil.rotate(arr, -1);
        int[] expected = { 2, 3, 4, 1 };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testRotateBoolean() {
        boolean[] a = { true, false, true, false };
        boolean[] expected = { false, true, false, true };
        CommonUtil.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);

        boolean[] b = { true, false, true, false };
        boolean[] expected2 = { false, true, false, true };
        CommonUtil.rotate(b, -1);
        Assertions.assertArrayEquals(expected2, b);

        boolean[] c = { true, false, true, false };
        boolean[] original = c.clone();
        CommonUtil.rotate(c, 4);
        Assertions.assertArrayEquals(original, c);

        boolean[] empty = {};
        CommonUtil.rotate(empty, 1);
        Assertions.assertArrayEquals(new boolean[0], empty);

        CommonUtil.rotate((boolean[]) null, 1);
    }

    @Test
    public void testRotateList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.rotate(list, 1);
        Assertions.assertEquals(Arrays.asList("d", "a", "b", "c"), list);

        CommonUtil.rotate((List<?>) null, 1);

        List<String> single = new ArrayList<>(Arrays.asList("a"));
        CommonUtil.rotate(single, 1);
        Assertions.assertEquals(Arrays.asList("a"), single);

        List<String> list2 = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.rotate(list2, 4);
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d"), list2);
    }

    @Test
    public void testRotateCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.rotate((Collection<?>) list, 1);
        Assertions.assertEquals(Arrays.asList("d", "a", "b", "c"), list);

        Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.rotate(set, 1);
        Assertions.assertEquals(4, set.size());

        Collection<String> empty = new ArrayList<>();
        CommonUtil.rotate(empty, 1);
        Assertions.assertTrue(empty.isEmpty());

        CommonUtil.rotate((Collection<?>) null, 1);

        Collection<String> single = new ArrayList<>(Arrays.asList("a"));
        CommonUtil.rotate(single, 1);
        Assertions.assertEquals(1, single.size());
    }

    @Test
    public void testRotate_CharArray_WithRange_NegativeDistance() {
        char[] arr = { 'a', 'b', 'c', 'd', 'e' };
        CommonUtil.rotate(arr, 1, 4, -1);
        assertArrayEquals(new char[] { 'a', 'c', 'd', 'b', 'e' }, arr);
    }

    @Test
    public void testRotate_ShortArray_WithRange_NegativeDistance() {
        short[] arr = { 1, 2, 3, 4, 5 };
        CommonUtil.rotate(arr, 1, 4, -1);
        assertArrayEquals(new short[] { 1, 3, 4, 2, 5 }, arr);
    }

    @Test
    public void testRotate_LongArray_WithRange_NegativeDistance() {
        long[] arr = { 1L, 2L, 3L, 4L, 5L };
        CommonUtil.rotate(arr, 1, 4, -1);
        assertArrayEquals(new long[] { 1L, 3L, 4L, 2L, 5L }, arr);
    }

    @Test
    public void testRotate_FloatArray_WithRange_NegativeDistance() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        CommonUtil.rotate(arr, 1, 4, -1);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 4.0f, 2.0f, 5.0f }, arr, 0.0f);
    }

    @Test
    public void testRotate_DoubleArray_WithRange_NegativeDistance() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        CommonUtil.rotate(arr, 1, 4, -1);
        assertArrayEquals(new double[] { 1.0, 3.0, 4.0, 2.0, 5.0 }, arr, 0.0);
    }

    @Test
    public void testRotateBooleanRange() {
        boolean[] a = { true, false, true, false, true };
        boolean[] expected = { true, false, true, false, true };
        CommonUtil.rotate(a, 1, 4, 1);
        expected[1] = false;
        expected[2] = false;
        expected[3] = true;
        Assertions.assertArrayEquals(expected, a);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.rotate(a, -1, 3, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.rotate(a, 0, 10, 1));

        boolean[] b = { true, false, true };
        boolean[] original = b.clone();
        CommonUtil.rotate(b, 1, 1, 1);
        Assertions.assertArrayEquals(original, b);
    }
    @Test
    public void testRotate_effectiveDistanceZeroIsANoOpForNonListCollections() {
        final Collection<String> unmodifiable = Collections.unmodifiableCollection(new LinkedHashSet<>(Arrays.asList("a", "b", "c")));

        Assertions.assertDoesNotThrow(() -> CommonUtil.rotate(unmodifiable, 0));
        Assertions.assertDoesNotThrow(() -> CommonUtil.rotate(unmodifiable, 3));
        Assertions.assertDoesNotThrow(() -> CommonUtil.rotate(unmodifiable, -3));
        Assertions.assertDoesNotThrow(() -> CommonUtil.rotate(unmodifiable, 6));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> CommonUtil.rotate(unmodifiable, 1));

        final Set<String> modifiable = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        final Iterator<String> outstanding = modifiable.iterator();

        CommonUtil.rotate(modifiable, 3);

        assertEquals("a", outstanding.next());
        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(modifiable));
    }

}
