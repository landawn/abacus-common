package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CommonUtilIndexTest extends CommonUtilTestSupport {
    @Test
    public void testIndexOf_CharArray() {
        char[] arr = { 'a', 'b', 'c', 'b' };
        assertEquals(0, CommonUtil.indexOf(arr, 'a'));
        assertEquals(1, CommonUtil.indexOf(arr, 'b'));
        assertEquals(-1, CommonUtil.indexOf(arr, 'z'));
        assertEquals(3, CommonUtil.indexOf(arr, 'b', 2));
    }

    @Test
    public void testIndexOf_ByteArray() {
        byte[] arr = { 1, 2, 3, 2 };
        assertEquals(0, CommonUtil.indexOf(arr, (byte) 1));
        assertEquals(1, CommonUtil.indexOf(arr, (byte) 2));
        assertEquals(-1, CommonUtil.indexOf(arr, (byte) 9));
        assertEquals(3, CommonUtil.indexOf(arr, (byte) 2, 2));
    }

    @Test
    public void testIndexOf_ShortArray() {
        short[] arr = { 1, 2, 3, 2 };
        assertEquals(0, CommonUtil.indexOf(arr, (short) 1));
        assertEquals(1, CommonUtil.indexOf(arr, (short) 2));
        assertEquals(-1, CommonUtil.indexOf(arr, (short) 9));
        assertEquals(3, CommonUtil.indexOf(arr, (short) 2, 2));
    }

    @Test
    public void testIndexOf_IntArray() {
        int[] arr = { 1, 2, 3, 2 };
        assertEquals(0, CommonUtil.indexOf(arr, 1));
        assertEquals(1, CommonUtil.indexOf(arr, 2));
        assertEquals(-1, CommonUtil.indexOf(arr, 9));
        assertEquals(3, CommonUtil.indexOf(arr, 2, 2));
    }

    @Test
    public void testIndexOf_LongArray() {
        long[] arr = { 1L, 2L, 3L, 2L };
        assertEquals(0, CommonUtil.indexOf(arr, 1L));
        assertEquals(1, CommonUtil.indexOf(arr, 2L));
        assertEquals(-1, CommonUtil.indexOf(arr, 9L));
        assertEquals(3, CommonUtil.indexOf(arr, 2L, 2));
    }

    @Test
    public void testIndexOf_FloatArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 2.0f };
        assertEquals(0, CommonUtil.indexOf(arr, 1.0f));
        assertEquals(1, CommonUtil.indexOf(arr, 2.0f));
        assertEquals(-1, CommonUtil.indexOf(arr, 9.0f));
        assertEquals(3, CommonUtil.indexOf(arr, 2.0f, 2));
    }

    @Test
    public void testIndexOf_DoubleArray() {
        double[] arr = { 1.0, 2.0, 3.0, 2.0 };
        assertEquals(0, CommonUtil.indexOf(arr, 1.0));
        assertEquals(1, CommonUtil.indexOf(arr, 2.0));
        assertEquals(-1, CommonUtil.indexOf(arr, 9.0));
        assertEquals(3, CommonUtil.indexOf(arr, 2.0, 2));
    }

    @Test
    public void testIndexOf_DoubleArray_WithTolerance() {
        double[] arr = { 1.0, 2.0, 3.0 };
        assertEquals(1, CommonUtil.indexOf(arr, 2.01, 0, 0.02));
        assertEquals(-1, CommonUtil.indexOf(arr, 2.1, 0, 0.05));
    }

    @Test
    public void testIndexOf_ObjectArray() {
        String[] arr = { "a", "b", "c", "b" };
        assertEquals(0, CommonUtil.indexOf(arr, "a"));
        assertEquals(1, CommonUtil.indexOf(arr, "b"));
        assertEquals(-1, CommonUtil.indexOf(arr, "z"));
        assertEquals(3, CommonUtil.indexOf(arr, "b", 2));
    }

    @Test
    public void testIndexOf_Collection() {
        List<String> list = Arrays.asList("a", "b", "c", "b");
        assertEquals(0, CommonUtil.indexOf(list, "a"));
        assertEquals(1, CommonUtil.indexOf(list, "b"));
        assertEquals(-1, CommonUtil.indexOf(list, "z"));
        assertEquals(3, CommonUtil.indexOf(list, "b", 2));
    }

    @Test
    public void testIndexOf_BooleanArray() {
        boolean[] arr = { true, false, true, false };
        assertEquals(0, CommonUtil.indexOf(arr, true));
        assertEquals(1, CommonUtil.indexOf(arr, false));
        assertEquals(-1, CommonUtil.indexOf((boolean[]) null, true));
        assertEquals(2, CommonUtil.indexOf(arr, true, 1));
    }

    @Test
    public void testIndexOf_booleanArray() {
        boolean[] arr = { true, false, true, false, true };
        Assertions.assertEquals(0, CommonUtil.indexOf(arr, true));
        Assertions.assertEquals(1, CommonUtil.indexOf(arr, false));

        boolean[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, true));

        Assertions.assertEquals(-1, CommonUtil.indexOf((boolean[]) null, true));
    }

    @Test
    public void testIndexOf_booleanArray_withFromIndex() {
        boolean[] arr = { true, false, true, false, true };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, true, 1));
        Assertions.assertEquals(3, CommonUtil.indexOf(arr, false, 2));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, true, 10));

        Assertions.assertEquals(0, CommonUtil.indexOf(arr, true, -1));
    }

    @Test
    public void testIndexOf_charArray() {
        char[] arr = { 'a', 'b', 'c', 'd', 'e' };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, 'c'));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 'z'));

        char[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, 'c'));

        Assertions.assertEquals(-1, CommonUtil.indexOf((char[]) null, 'c'));
    }

    @Test
    public void testIndexOf_charArray_withFromIndex() {
        char[] arr = { 'a', 'b', 'c', 'd', 'c' };
        Assertions.assertEquals(4, CommonUtil.indexOf(arr, 'c', 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 'c', 10));
    }

    @Test
    public void testIndexOf_byteArray() {
        byte[] arr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, (byte) 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, (byte) 10));

        byte[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, (byte) 3));

        Assertions.assertEquals(-1, CommonUtil.indexOf((byte[]) null, (byte) 3));
    }

    @Test
    public void testIndexOf_byteArray_withFromIndex() {
        byte[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(4, CommonUtil.indexOf(arr, (byte) 3, 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, (byte) 3, 10));
    }

    @Test
    public void testIndexOf_shortArray() {
        short[] arr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, (short) 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, (short) 10));

        short[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, (short) 3));

        Assertions.assertEquals(-1, CommonUtil.indexOf((short[]) null, (short) 3));
    }

    @Test
    public void testIndexOf_shortArray_withFromIndex() {
        short[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(4, CommonUtil.indexOf(arr, (short) 3, 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, (short) 3, 10));
    }

    @Test
    public void testIndexOf_intArray() {
        int[] arr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 10));

        int[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, 3));

        Assertions.assertEquals(-1, CommonUtil.indexOf((int[]) null, 3));
    }

    @Test
    public void testIndexOf_intArray_withFromIndex() {
        int[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(4, CommonUtil.indexOf(arr, 3, 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 3, 10));
    }

    @Test
    public void testIndexOf_longArray() {
        long[] arr = { 1L, 2L, 3L, 4L, 5L };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, 3L));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 10L));

        long[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, 3L));

        Assertions.assertEquals(-1, CommonUtil.indexOf((long[]) null, 3L));
    }

    @Test
    public void testIndexOf_longArray_withFromIndex() {
        long[] arr = { 1L, 2L, 3L, 4L, 3L };
        Assertions.assertEquals(4, CommonUtil.indexOf(arr, 3L, 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 3L, 10));
    }

    @Test
    public void testIndexOf_floatArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, 3.0f));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 10.0f));

        float[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, 3.0f));

        Assertions.assertEquals(-1, CommonUtil.indexOf((float[]) null, 3.0f));
    }

    @Test
    public void testIndexOf_floatArray_withFromIndex() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 3.0f };
        Assertions.assertEquals(4, CommonUtil.indexOf(arr, 3.0f, 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 3.0f, 10));
    }

    @Test
    public void testIndexOf_floatArray_withToleranceAndFromIndex() {
        float[] arr = { 1.0f, 2.0f, 3.001f, 4.0f, 3.002f };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, 3.0f, 0, 0.01f));
        Assertions.assertEquals(4, CommonUtil.indexOf(arr, 3.0f, 3, 0.01f));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 3.0f, 3, 0.0001f));

        float[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, 3.0f, 0, 0.01f));

        Assertions.assertEquals(-1, CommonUtil.indexOf((float[]) null, 3.0f, 0, 0.01f));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.indexOf(arr, 3.0f, 0, -0.01f));
    }

    @Test
    public void testIndexOf_doubleArray() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, 3.0));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 10.0));

        double[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, 3.0));

        Assertions.assertEquals(-1, CommonUtil.indexOf((double[]) null, 3.0));
    }

    @Test
    public void testIndexOf_doubleArray_withFromIndex() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 3.0 };
        Assertions.assertEquals(4, CommonUtil.indexOf(arr, 3.0, 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 3.0, 10));
    }

    @Test
    public void testIndexOf_doubleArray_withTolerance() {
        double[] arr = { 1.0, 2.0, 3.001, 4.0, 5.0 };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, 3.0, 0, 0.01));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 3.0, 0, 0.0001));

        double[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, 3.0, 0, 0.01));

        Assertions.assertEquals(-1, CommonUtil.indexOf((double[]) null, 3.0, 0, 0.01));
    }

    @Test
    public void testIndexOf_doubleArray_withToleranceAndFromIndex() {
        double[] arr = { 1.0, 2.0, 3.001, 4.0, 3.002 };
        Assertions.assertEquals(4, CommonUtil.indexOf(arr, 3.0, 3, 0.01));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 3.0, 3, 0.0001));
    }

    @Test
    public void testIndexOf_withTolerance_rejectsInvalidToleranceBeforeEarlyReturn() {
        float[] floatArray = { 1.0f };
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.indexOf((float[]) null, 1.0f, 0, -0.01f));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.indexOf(new float[0], 1.0f, 0, Float.NaN));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.indexOf(floatArray, 1.0f, floatArray.length, -0.01f));

        double[] doubleArray = { 1.0 };
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.indexOf((double[]) null, 1.0, 0, -0.01));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.indexOf(new double[0], 1.0, 0, Double.NaN));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.indexOf(doubleArray, 1.0, doubleArray.length, -0.01));
    }

    @Test
    public void testIndexOf_objectArray() {
        String[] arr = { "a", "b", "c", "d", "e" };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, "c"));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, "z"));

        String[] arrWithNull = { "a", null, "c" };
        Assertions.assertEquals(1, CommonUtil.indexOf(arrWithNull, null));

        String[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, "c"));

        Assertions.assertEquals(-1, CommonUtil.indexOf((String[]) null, "c"));
    }

    @Test
    public void testIndexOf_objectArray_withFromIndex() {
        String[] arr = { "a", "b", "c", "d", "c" };
        Assertions.assertEquals(4, CommonUtil.indexOf(arr, "c", 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, "c", 10));
    }

    @Test
    public void testIndexOf_collection() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Assertions.assertEquals(2, CommonUtil.indexOf(list, "c"));
        Assertions.assertEquals(-1, CommonUtil.indexOf(list, "z"));

        List<String> empty = new ArrayList<>();
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, "c"));

        Assertions.assertEquals(-1, CommonUtil.indexOf((Collection<?>) null, "c"));
    }

    @Test
    public void testIndexOf_collection_withFromIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "c");
        Assertions.assertEquals(4, CommonUtil.indexOf(list, "c", 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(list, "c", 10));

        LinkedList<String> linkedList = new LinkedList<>(list);
        Assertions.assertEquals(4, CommonUtil.indexOf(linkedList, "c", 3));
    }

    @Test
    public void testIndexOf_iterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Assertions.assertEquals(2, CommonUtil.indexOf(list.iterator(), "c"));
        Assertions.assertEquals(-1, CommonUtil.indexOf(list.iterator(), "z"));

        Assertions.assertEquals(-1, CommonUtil.indexOf((Iterator<?>) null, "c"));
    }

    @Test
    public void testIndexOf_iterator_withFromIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "c");
        Assertions.assertEquals(4, CommonUtil.indexOf(list.iterator(), "c", 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(list.iterator(), "c", 10));
    }

    @Test
    public void testIndexOf_Collection_WithFromIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "b", "d");
        assertEquals(3, CommonUtil.indexOf(list, "b", 2));
        assertEquals(1, CommonUtil.indexOf(list, "b", 1));
        assertEquals(-1, CommonUtil.indexOf(list, "b", 4));
        assertEquals(-1, CommonUtil.indexOf(list, "z", 0));
    }

    @Test
    public void testIndexOf_Collection_WithFromIndex_LinkedList() {
        // LinkedList is not RandomAccess - hits the iterator path
        java.util.LinkedList<String> list = new java.util.LinkedList<>(Arrays.asList("a", "b", "c", "b", "d"));
        assertEquals(3, CommonUtil.indexOf(list, "b", 2));
        assertEquals(1, CommonUtil.indexOf(list, "b", 1));
        assertEquals(-1, CommonUtil.indexOf(list, "b", 5));
    }

    @Test
    public void testIndexOfSubList_withFromIndex() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d");
        List<String> sub = Arrays.asList("c", "d");
        Assertions.assertEquals(4, CommonUtil.indexOfSubList(source, sub, 3));
        Assertions.assertEquals(-1, CommonUtil.indexOfSubList(source, sub, 10));
    }

    @Test
    public void testIndexOfSubList() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> subList = Arrays.asList(3, 4);
        assertEquals(2, CommonUtil.indexOfSubList(list, subList));
        assertEquals(0, Strings.indexOf("", ""));
        assertEquals(0, "".indexOf(""));
        assertEquals(0, Collections.indexOfSubList(CommonUtil.emptyList(), CommonUtil.emptyList()));
        assertEquals(0, CommonUtil.indexOfSubList(CommonUtil.emptyList(), CommonUtil.emptyList()));
        assertEquals(0, Index.ofSubList(CommonUtil.emptyList(), CommonUtil.emptyList()).orElseThrow());
        assertEquals(0, Index.ofSubList(CommonUtil.emptyList(), 0, CommonUtil.emptyList()).orElseThrow());
        assertEquals(Collections.indexOfSubList(CommonUtil.emptyList(), CommonUtil.emptyList()),
                CommonUtil.indexOfSubList(CommonUtil.emptyList(), CommonUtil.emptyList()));
    }

    @Test
    public void testIndexOfIgnoreCase() {
        String[] arr = { "Apple", "Banana", "Cherry" };
        assertEquals(1, CommonUtil.indexOfIgnoreCase(arr, "banana"));
        assertEquals(-1, CommonUtil.indexOfIgnoreCase(arr, "grape"));
    }

    @Test
    public void testIndexOfIgnoreCase_withFromIndex() {
        String[] arr = { "A", "B", "C", "D", "c" };
        Assertions.assertEquals(4, CommonUtil.indexOfIgnoreCase(arr, "C", 3));
        Assertions.assertEquals(-1, CommonUtil.indexOfIgnoreCase(arr, "C", 10));
    }
}
