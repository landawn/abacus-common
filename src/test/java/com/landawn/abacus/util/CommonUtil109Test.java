package com.landawn.abacus.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalInt;

@Tag("new-test")
public class CommonUtil109Test extends TestBase {

    @Test
    public void testBinarySearch_shortArray() {
        short[] arr = { 1, 3, 5, 7, 9 };
        Assertions.assertEquals(2, CommonUtil.binarySearch(arr, (short) 5));
        Assertions.assertEquals(-1, CommonUtil.binarySearch(arr, (short) 0));
        Assertions.assertEquals(-6, CommonUtil.binarySearch(arr, (short) 10));

        short[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, (short) 5));

        Assertions.assertEquals(-1, CommonUtil.binarySearch((short[]) null, (short) 5));
    }

    @Test
    public void testBinarySearch_shortArray_withRange() {
        short[] arr = { 1, 3, 5, 7, 9, 11, 13 };
        Assertions.assertEquals(3, CommonUtil.binarySearch(arr, 1, 5, (short) 7));
        Assertions.assertEquals(-2, CommonUtil.binarySearch(arr, 1, 5, (short) 2));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.binarySearch(arr, 5, 2, (short) 5));

        short[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 0, 0, (short) 5));
    }

    @Test
    public void testBinarySearch_intArray() {
        int[] arr = { 1, 3, 5, 7, 9 };
        Assertions.assertEquals(2, CommonUtil.binarySearch(arr, 5));
        Assertions.assertEquals(-1, CommonUtil.binarySearch(arr, 0));
        Assertions.assertEquals(-6, CommonUtil.binarySearch(arr, 10));

        int[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 5));

        Assertions.assertEquals(-1, CommonUtil.binarySearch((int[]) null, 5));
    }

    @Test
    public void testBinarySearch_intArray_withRange() {
        int[] arr = { 1, 3, 5, 7, 9, 11, 13 };
        Assertions.assertEquals(3, CommonUtil.binarySearch(arr, 1, 5, 7));
        Assertions.assertEquals(-2, CommonUtil.binarySearch(arr, 1, 5, 2));

        int[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 0, 0, 5));
    }

    @Test
    public void testBinarySearch_longArray() {
        long[] arr = { 1L, 3L, 5L, 7L, 9L };
        Assertions.assertEquals(2, CommonUtil.binarySearch(arr, 5L));
        Assertions.assertEquals(-1, CommonUtil.binarySearch(arr, 0L));
        Assertions.assertEquals(-6, CommonUtil.binarySearch(arr, 10L));

        long[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 5L));

        Assertions.assertEquals(-1, CommonUtil.binarySearch((long[]) null, 5L));
    }

    @Test
    public void testBinarySearch_longArray_withRange() {
        long[] arr = { 1L, 3L, 5L, 7L, 9L, 11L, 13L };
        Assertions.assertEquals(3, CommonUtil.binarySearch(arr, 1, 5, 7L));
        Assertions.assertEquals(-2, CommonUtil.binarySearch(arr, 1, 5, 2L));

        long[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 0, 0, 5L));
    }

    @Test
    public void testBinarySearch_floatArray() {
        float[] arr = { 1.0f, 3.0f, 5.0f, 7.0f, 9.0f };
        Assertions.assertEquals(2, CommonUtil.binarySearch(arr, 5.0f));
        Assertions.assertEquals(-1, CommonUtil.binarySearch(arr, 0.0f));
        Assertions.assertEquals(-6, CommonUtil.binarySearch(arr, 10.0f));

        float[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 5.0f));

        Assertions.assertEquals(-1, CommonUtil.binarySearch((float[]) null, 5.0f));
    }

    @Test
    public void testBinarySearch_floatArray_withRange() {
        float[] arr = { 1.0f, 3.0f, 5.0f, 7.0f, 9.0f, 11.0f, 13.0f };
        Assertions.assertEquals(3, CommonUtil.binarySearch(arr, 1, 5, 7.0f));
        Assertions.assertEquals(-2, CommonUtil.binarySearch(arr, 1, 5, 2.0f));

        float[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 0, 0, 5.0f));
    }

    @Test
    public void testBinarySearch_doubleArray() {
        double[] arr = { 1.0, 3.0, 5.0, 7.0, 9.0 };
        Assertions.assertEquals(2, CommonUtil.binarySearch(arr, 5.0));
        Assertions.assertEquals(-1, CommonUtil.binarySearch(arr, 0.0));
        Assertions.assertEquals(-6, CommonUtil.binarySearch(arr, 10.0));

        double[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 5.0));

        Assertions.assertEquals(-1, CommonUtil.binarySearch((double[]) null, 5.0));
    }

    @Test
    public void testBinarySearch_doubleArray_withRange() {
        double[] arr = { 1.0, 3.0, 5.0, 7.0, 9.0, 11.0, 13.0 };
        Assertions.assertEquals(3, CommonUtil.binarySearch(arr, 1, 5, 7.0));
        Assertions.assertEquals(-2, CommonUtil.binarySearch(arr, 1, 5, 2.0));

        double[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 0, 0, 5.0));
    }

    @Test
    public void testBinarySearch_objectArray() {
        String[] arr = { "a", "c", "e", "g", "i" };
        Assertions.assertEquals(2, CommonUtil.binarySearch(arr, "e"));
        Assertions.assertEquals(-1, CommonUtil.binarySearch(arr, ""));
        Assertions.assertEquals(-6, CommonUtil.binarySearch(arr, "z"));

        String[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, "e"));

        Assertions.assertEquals(-1, CommonUtil.binarySearch((String[]) null, "e"));
    }

    @Test
    public void testBinarySearch_objectArray_withRange() {
        String[] arr = { "a", "c", "e", "g", "i", "k", "m" };
        Assertions.assertEquals(3, CommonUtil.binarySearch(arr, 1, 5, "g"));
        Assertions.assertEquals(-2, CommonUtil.binarySearch(arr, 1, 5, "b"));

        String[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 0, 0, "e"));
    }

    @Test
    public void testBinarySearch_genericArray_withComparator() {
        String[] arr = { "a", "c", "e", "g", "i" };
        Comparator<String> cmp = String::compareTo;
        Assertions.assertEquals(2, CommonUtil.binarySearch(arr, "e", cmp));
        Assertions.assertEquals(-1, CommonUtil.binarySearch(arr, "", cmp));

        String[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, "e", cmp));

        Assertions.assertEquals(-1, CommonUtil.binarySearch((String[]) null, "e", cmp));
    }

    @Test
    public void testBinarySearch_genericArray_withRangeAndComparator() {
        String[] arr = { "a", "c", "e", "g", "i", "k", "m" };
        Comparator<String> cmp = String::compareTo;
        Assertions.assertEquals(3, CommonUtil.binarySearch(arr, 1, 5, "g", cmp));
        Assertions.assertEquals(-2, CommonUtil.binarySearch(arr, 1, 5, "b", cmp));

        String[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 0, 0, "e", cmp));
    }

    @Test
    public void testBinarySearch_list() {
        List<Integer> list = Arrays.asList(1, 3, 5, 7, 9);
        Assertions.assertEquals(2, CommonUtil.binarySearch(list, 5));
        Assertions.assertEquals(-1, CommonUtil.binarySearch(list, 0));
        Assertions.assertEquals(-6, CommonUtil.binarySearch(list, 10));

        List<Integer> empty = new ArrayList<>();
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 5));

        Assertions.assertEquals(-1, CommonUtil.binarySearch((List<Integer>) null, 5));
    }

    @Test
    public void testBinarySearch_list_withRange() {
        List<Integer> list = Arrays.asList(1, 3, 5, 7, 9, 11, 13);
        Assertions.assertEquals(3, CommonUtil.binarySearch(list, 1, 5, 7));
        Assertions.assertEquals(-2, CommonUtil.binarySearch(list, 1, 5, 2));

        List<Integer> empty = new ArrayList<>();
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 0, 0, 5));
    }

    @Test
    public void testBinarySearch_list_withComparator() {
        List<String> list = Arrays.asList("a", "c", "e", "g", "i");
        Comparator<String> cmp = String::compareTo;
        Assertions.assertEquals(2, CommonUtil.binarySearch(list, "e", cmp));
        Assertions.assertEquals(-1, CommonUtil.binarySearch(list, "", cmp));

        List<String> empty = new ArrayList<>();
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, "e", cmp));

        Assertions.assertEquals(-1, CommonUtil.binarySearch((List<String>) null, "e", cmp));
    }

    @Test
    public void testBinarySearch_list_withRangeAndComparator() {
        List<String> list = Arrays.asList("a", "c", "e", "g", "i", "k", "m");
        Comparator<String> cmp = String::compareTo;
        Assertions.assertEquals(3, CommonUtil.binarySearch(list, 1, 5, "g", cmp));
        Assertions.assertEquals(-2, CommonUtil.binarySearch(list, 1, 5, "b", cmp));

        List<String> empty = new ArrayList<>();
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 0, 0, "e", cmp));
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
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, 3.0, 0.01));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 3.0, 0.0001));

        double[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, 3.0, 0.01));

        Assertions.assertEquals(-1, CommonUtil.indexOf((double[]) null, 3.0, 0.01));
    }

    @Test
    public void testIndexOf_doubleArray_withToleranceAndFromIndex() {
        double[] arr = { 1.0, 2.0, 3.001, 4.0, 3.002 };
        Assertions.assertEquals(4, CommonUtil.indexOf(arr, 3.0, 0.01, 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 3.0, 0.0001, 3));
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
    public void testIndexOfSubList() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "e");
        List<String> sub = Arrays.asList("c", "d");
        Assertions.assertEquals(2, CommonUtil.indexOfSubList(source, sub));

        List<String> notFound = Arrays.asList("x", "y");
        Assertions.assertEquals(-1, CommonUtil.indexOfSubList(source, notFound));

        Assertions.assertEquals(-1, CommonUtil.indexOfSubList(new ArrayList<>(), sub));
        Assertions.assertEquals(-1, CommonUtil.indexOfSubList(source, new ArrayList<>()));
    }

    @Test
    public void testIndexOfSubList_withFromIndex() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d");
        List<String> sub = Arrays.asList("c", "d");
        Assertions.assertEquals(4, CommonUtil.indexOfSubList(source, sub, 3));
        Assertions.assertEquals(-1, CommonUtil.indexOfSubList(source, sub, 10));
    }

    @Test
    public void testIndexOfIgnoreCase() {
        String[] arr = { "A", "B", "C", "D", "E" };
        Assertions.assertEquals(2, CommonUtil.indexOfIgnoreCase(arr, "c"));
        Assertions.assertEquals(2, CommonUtil.indexOfIgnoreCase(arr, "C"));
        Assertions.assertEquals(-1, CommonUtil.indexOfIgnoreCase(arr, "z"));

        String[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOfIgnoreCase(empty, "c"));

        Assertions.assertEquals(-1, CommonUtil.indexOfIgnoreCase((String[]) null, "c"));
    }

    @Test
    public void testIndexOfIgnoreCase_withFromIndex() {
        String[] arr = { "A", "B", "C", "D", "c" };
        Assertions.assertEquals(4, CommonUtil.indexOfIgnoreCase(arr, "C", 3));
        Assertions.assertEquals(-1, CommonUtil.indexOfIgnoreCase(arr, "C", 10));
    }

    @Test
    public void testLastIndexOf_booleanArray() {
        boolean[] arr = { true, false, true, false, true };
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(arr, true));
        Assertions.assertEquals(3, CommonUtil.lastIndexOf(arr, false));

        boolean[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, true));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((boolean[]) null, true));
    }

    @Test
    public void testLastIndexOf_booleanArray_withStartIndex() {
        boolean[] arr = { true, false, true, false, true };
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(arr, true, 3));
        Assertions.assertEquals(1, CommonUtil.lastIndexOf(arr, false, 2));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, true, -1));
    }

    @Test
    public void testLastIndexOf_charArray() {
        char[] arr = { 'a', 'b', 'c', 'd', 'c' };
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(arr, 'c'));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 'z'));

        char[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, 'c'));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((char[]) null, 'c'));
    }

    @Test
    public void testLastIndexOf_charArray_withStartIndex() {
        char[] arr = { 'a', 'b', 'c', 'd', 'c' };
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(arr, 'c', 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 'c', -1));
    }

    @Test
    public void testLastIndexOf_byteArray() {
        byte[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(arr, (byte) 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, (byte) 10));

        byte[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, (byte) 3));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((byte[]) null, (byte) 3));
    }

    @Test
    public void testLastIndexOf_byteArray_withStartIndex() {
        byte[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(arr, (byte) 3, 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, (byte) 3, -1));
    }

    @Test
    public void testLastIndexOf_shortArray() {
        short[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(arr, (short) 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, (short) 10));

        short[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, (short) 3));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((short[]) null, (short) 3));
    }

    @Test
    public void testLastIndexOf_shortArray_withStartIndex() {
        short[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(arr, (short) 3, 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, (short) 3, -1));
    }

    @Test
    public void testLastIndexOf_intArray() {
        int[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(arr, 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 10));

        int[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, 3));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((int[]) null, 3));
    }

    @Test
    public void testLastIndexOf_intArray_withStartIndex() {
        int[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(arr, 3, 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 3, -1));
    }

    @Test
    public void testLastIndexOf_longArray() {
        long[] arr = { 1L, 2L, 3L, 4L, 3L };
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(arr, 3L));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 10L));

        long[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, 3L));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((long[]) null, 3L));
    }

    @Test
    public void testLastIndexOf_longArray_withStartIndex() {
        long[] arr = { 1L, 2L, 3L, 4L, 3L };
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(arr, 3L, 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 3L, -1));
    }

    @Test
    public void testLastIndexOf_floatArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 3.0f };
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(arr, 3.0f));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 10.0f));

        float[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, 3.0f));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((float[]) null, 3.0f));
    }

    @Test
    public void testLastIndexOf_floatArray_withStartIndex() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 3.0f };
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(arr, 3.0f, 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 3.0f, -1));
    }

    @Test
    public void testLastIndexOf_doubleArray() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 3.0 };
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(arr, 3.0));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 10.0));

        double[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, 3.0));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((double[]) null, 3.0));
    }

    @Test
    public void testLastIndexOf_doubleArray_withStartIndex() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 3.0 };
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(arr, 3.0, 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 3.0, -1));
    }

    @Test
    public void testLastIndexOf_doubleArray_withTolerance() {
        double[] arr = { 1.0, 2.0, 3.001, 4.0, 3.002 };
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(arr, 3.0, 0.01));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 3.0, 0.0001));

        double[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, 3.0, 0.01));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((double[]) null, 3.0, 0.01));
    }

    @Test
    public void testLastIndexOf_doubleArray_withToleranceAndStartIndex() {
        double[] arr = { 1.0, 2.0, 3.001, 4.0, 3.002 };
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(arr, 3.0, 0.01, 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 3.0, 0.0001, 3));
    }

    @Test
    public void testLastIndexOf_objectArray() {
        String[] arr = { "a", "b", "c", "d", "c" };
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(arr, "c"));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, "z"));

        String[] arrWithNull = { "a", null, "c", null };
        Assertions.assertEquals(3, CommonUtil.lastIndexOf(arrWithNull, null));

        String[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, "c"));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((String[]) null, "c"));
    }

    @Test
    public void testLastIndexOf_objectArray_withStartIndex() {
        String[] arr = { "a", "b", "c", "d", "c" };
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(arr, "c", 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, "c", -1));
    }

    @Test
    public void testLastIndexOf_collection() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "c");
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(list, "c"));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(list, "z"));

        List<String> empty = new ArrayList<>();
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, "c"));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((Collection<?>) null, "c"));
    }

    @Test
    public void testLastIndexOf_collection_withStartIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "c");
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(list, "c", 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(list, "c", -1));

        LinkedList<String> linkedList = new LinkedList<>(list);
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(linkedList, "c", 3));
    }

    @Test
    public void testLastIndexOfSubList() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d");
        List<String> sub = Arrays.asList("c", "d");
        Assertions.assertEquals(4, CommonUtil.lastIndexOfSubList(source, sub));

        List<String> notFound = Arrays.asList("x", "y");
        Assertions.assertEquals(-1, CommonUtil.lastIndexOfSubList(source, notFound));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOfSubList(new ArrayList<>(), sub));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOfSubList(source, new ArrayList<>()));
    }

    @Test
    public void testLastIndexOfSubList_withStartIndex() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d");
        List<String> sub = Arrays.asList("c", "d");
        Assertions.assertEquals(2, CommonUtil.lastIndexOfSubList(source, sub, 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOfSubList(source, sub, 1));
    }

    @Test
    public void testLastIndexOfIgnoreCase() {
        String[] arr = { "A", "B", "C", "D", "c" };
        Assertions.assertEquals(4, CommonUtil.lastIndexOfIgnoreCase(arr, "C"));
        Assertions.assertEquals(4, CommonUtil.lastIndexOfIgnoreCase(arr, "c"));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOfIgnoreCase(arr, "z"));

        String[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOfIgnoreCase(empty, "c"));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOfIgnoreCase((String[]) null, "c"));
    }

    @Test
    public void testLastIndexOfIgnoreCase_withStartIndex() {
        String[] arr = { "A", "B", "C", "D", "c" };
        Assertions.assertEquals(2, CommonUtil.lastIndexOfIgnoreCase(arr, "C", 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOfIgnoreCase(arr, "C", -1));
    }

    @Test
    public void testFindFirstIndex_array() {
        String[] arr = { "a", "b", "c", "d", "e" };
        OptionalInt result = CommonUtil.findFirstIndex(arr, s -> s.equals("c"));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.getAsInt());

        OptionalInt notFound = CommonUtil.findFirstIndex(arr, s -> s.equals("z"));
        Assertions.assertFalse(notFound.isPresent());

        String[] empty = {};
        OptionalInt emptyResult = CommonUtil.findFirstIndex(empty, s -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testFindFirstIndex_array_withBiPredicate() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String prefix = "c";
        OptionalInt result = CommonUtil.findFirstIndex(arr, prefix, (s, p) -> s.startsWith(p));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.getAsInt());

        String[] empty = {};
        OptionalInt emptyResult = CommonUtil.findFirstIndex(empty, prefix, (s, p) -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testFindFirstIndex_collection() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        OptionalInt result = CommonUtil.findFirstIndex(list, s -> s.equals("c"));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.getAsInt());

        OptionalInt notFound = CommonUtil.findFirstIndex(list, s -> s.equals("z"));
        Assertions.assertFalse(notFound.isPresent());

        List<String> empty = new ArrayList<>();
        OptionalInt emptyResult = CommonUtil.findFirstIndex(empty, s -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testFindFirstIndex_collection_withBiPredicate() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        String prefix = "c";
        OptionalInt result = CommonUtil.findFirstIndex(list, prefix, (s, p) -> s.startsWith(p));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.getAsInt());

        List<String> empty = new ArrayList<>();
        OptionalInt emptyResult = CommonUtil.findFirstIndex(empty, prefix, (s, p) -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testFindLastIndex_array() {
        String[] arr = { "a", "b", "c", "d", "c" };
        OptionalInt result = CommonUtil.findLastIndex(arr, s -> s.equals("c"));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.getAsInt());

        OptionalInt notFound = CommonUtil.findLastIndex(arr, s -> s.equals("z"));
        Assertions.assertFalse(notFound.isPresent());

        String[] empty = {};
        OptionalInt emptyResult = CommonUtil.findLastIndex(empty, s -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testFindLastIndex_array_withBiPredicate() {
        String[] arr = { "a", "b", "c", "d", "ca" };
        String prefix = "c";
        OptionalInt result = CommonUtil.findLastIndex(arr, prefix, (s, p) -> s.startsWith(p));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.getAsInt());

        String[] empty = {};
        OptionalInt emptyResult = CommonUtil.findLastIndex(empty, prefix, (s, p) -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testFindLastIndex_collection() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "c");
        OptionalInt result = CommonUtil.findLastIndex(list, s -> s.equals("c"));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.getAsInt());

        OptionalInt notFound = CommonUtil.findLastIndex(list, s -> s.equals("z"));
        Assertions.assertFalse(notFound.isPresent());

        List<String> empty = new ArrayList<>();
        OptionalInt emptyResult = CommonUtil.findLastIndex(empty, s -> true);
        Assertions.assertFalse(emptyResult.isPresent());

        LinkedList<String> linkedList = new LinkedList<>(list);
        OptionalInt linkedResult = CommonUtil.findLastIndex(linkedList, s -> s.equals("c"));
        Assertions.assertTrue(linkedResult.isPresent());
        Assertions.assertEquals(4, linkedResult.getAsInt());
    }

    @Test
    public void testFindLastIndex_collection_withBiPredicate() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "ca");
        String prefix = "c";
        OptionalInt result = CommonUtil.findLastIndex(list, prefix, (s, p) -> s.startsWith(p));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.getAsInt());

        List<String> empty = new ArrayList<>();
        OptionalInt emptyResult = CommonUtil.findLastIndex(empty, prefix, (s, p) -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testIndicesOfAllMin_array() {
        Integer[] arr = { 3, 1, 4, 1, 5, 1 };
        int[] indices = CommonUtil.indicesOfAllMin(arr);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        Integer[] single = { 5 };
        int[] singleIndices = CommonUtil.indicesOfAllMin(single);
        Assertions.assertArrayEquals(new int[] { 0 }, singleIndices);

        Integer[] empty = {};
        int[] emptyIndices = CommonUtil.indicesOfAllMin(empty);
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);
    }

    @Test
    public void testIndicesOfAllMin_array_withComparator() {
        String[] arr = { "cat", "a", "dog", "a", "bird", "a" };
        Comparator<String> cmp = Comparator.comparing(String::length);
        int[] indices = CommonUtil.indicesOfAllMin(arr, cmp);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        String[] arrWithNull = { null, "a", "b", null };
        int[] nullIndices = CommonUtil.indicesOfAllMin(arrWithNull, null);
        Assertions.assertArrayEquals(new int[] { 1 }, nullIndices);
    }

    @Test
    public void testIndicesOfAllMin_collection() {
        List<Integer> list = Arrays.asList(3, 1, 4, 1, 5, 1);
        int[] indices = CommonUtil.indicesOfAllMin(list);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        List<Integer> empty = new ArrayList<>();
        int[] emptyIndices = CommonUtil.indicesOfAllMin(empty);
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);
    }

    @Test
    public void testIndicesOfAllMin_collection_withComparator() {
        List<String> list = Arrays.asList("cat", "a", "dog", "a", "bird", "a");
        Comparator<String> cmp = Comparator.comparing(String::length);
        int[] indices = CommonUtil.indicesOfAllMin(list, cmp);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);
    }

    @Test
    public void testIndicesOfAllMax_array() {
        Integer[] arr = { 3, 5, 4, 5, 1, 5 };
        int[] indices = CommonUtil.indicesOfAllMax(arr);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        Integer[] single = { 5 };
        int[] singleIndices = CommonUtil.indicesOfAllMax(single);
        Assertions.assertArrayEquals(new int[] { 0 }, singleIndices);

        Integer[] empty = {};
        int[] emptyIndices = CommonUtil.indicesOfAllMax(empty);
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);
    }

    @Test
    public void testIndicesOfAllMax_array_withComparator() {
        String[] arr = { "a", "cat", "b", "dog", "c", "dog" };
        Comparator<String> cmp = Comparator.comparing(String::length);
        int[] indices = CommonUtil.indicesOfAllMax(arr, cmp);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        String[] arrWithNull = { "a", null, "b", null };
        int[] nullIndices = CommonUtil.indicesOfAllMax(arrWithNull, null);
        Assertions.assertArrayEquals(new int[] { 2 }, nullIndices);
    }

    @Test
    public void testIndicesOfAllMax_collection() {
        List<Integer> list = Arrays.asList(3, 5, 4, 5, 1, 5);
        int[] indices = CommonUtil.indicesOfAllMax(list);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        List<Integer> empty = new ArrayList<>();
        int[] emptyIndices = CommonUtil.indicesOfAllMax(empty);
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);
    }

    @Test
    public void testIndicesOfAllMax_collection_withComparator() {
        List<String> list = Arrays.asList("a", "cat", "b", "dog", "c", "dog");
        Comparator<String> cmp = Comparator.comparing(String::length);
        int[] indices = CommonUtil.indicesOfAllMax(list, cmp);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);
    }

    @Test
    public void testIndicesOfAll_objectArray() {
        String[] arr = { "a", "b", "c", "b", "d", "b" };
        int[] indices = CommonUtil.indicesOfAll(arr, "b");
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        int[] notFound = CommonUtil.indicesOfAll(arr, "z");
        Assertions.assertArrayEquals(new int[] {}, notFound);

        String[] arrWithNull = { "a", null, "b", null };
        int[] nullIndices = CommonUtil.indicesOfAll(arrWithNull, (String) null);
        Assertions.assertArrayEquals(new int[] { 1, 3 }, nullIndices);

        String[] empty = {};
        int[] emptyIndices = CommonUtil.indicesOfAll(empty, "a");
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);
    }

    @Test
    public void testIndicesOfAll_objectArray_withStartIndex() {
        String[] arr = { "a", "b", "c", "b", "d", "b" };
        int[] indices = CommonUtil.indicesOfAll(arr, "b", 2);
        Assertions.assertArrayEquals(new int[] { 3, 5 }, indices);

        int[] beyondIndices = CommonUtil.indicesOfAll(arr, "b", 10);
        Assertions.assertArrayEquals(new int[] {}, beyondIndices);

        int[] negativeIndices = CommonUtil.indicesOfAll(arr, "b", -1);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, negativeIndices);
    }

    @Test
    public void testIndicesOfAll_collection() {
        List<String> list = Arrays.asList("a", "b", "c", "b", "d", "b");
        int[] indices = CommonUtil.indicesOfAll(list, "b");
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        List<String> empty = new ArrayList<>();
        int[] emptyIndices = CommonUtil.indicesOfAll(empty, "a");
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);

        LinkedList<String> linkedList = new LinkedList<>(list);
        int[] linkedIndices = CommonUtil.indicesOfAll(linkedList, "b");
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, linkedIndices);
    }

    @Test
    public void testIndicesOfAll_collection_withStartIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "b", "d", "b");
        int[] indices = CommonUtil.indicesOfAll(list, "b", 2);
        Assertions.assertArrayEquals(new int[] { 3, 5 }, indices);

        int[] beyondIndices = CommonUtil.indicesOfAll(list, "b", 10);
        Assertions.assertArrayEquals(new int[] {}, beyondIndices);
    }

    @Test
    public void testIndicesOfAll_array_withPredicate() {
        String[] arr = { "apple", "banana", "apricot", "cherry", "avocado" };
        int[] indices = CommonUtil.indicesOfAll(arr, s -> s.startsWith("a"));
        Assertions.assertArrayEquals(new int[] { 0, 2, 4 }, indices);

        String[] empty = {};
        int[] emptyIndices = CommonUtil.indicesOfAll(empty, s -> true);
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);
    }

    @Test
    public void testIndicesOfAll_array_withPredicateAndStartIndex() {
        String[] arr = { "apple", "banana", "apricot", "cherry", "avocado" };
        int[] indices = CommonUtil.indicesOfAll(arr, s -> s.startsWith("a"), 1);
        Assertions.assertArrayEquals(new int[] { 2, 4 }, indices);

        int[] beyondIndices = CommonUtil.indicesOfAll(arr, s -> s.startsWith("a"), 10);
        Assertions.assertArrayEquals(new int[] {}, beyondIndices);
    }

    @Test
    public void testIndicesOfAll_collection_withPredicate() {
        List<String> list = Arrays.asList("apple", "banana", "apricot", "cherry", "avocado");
        int[] indices = CommonUtil.indicesOfAll(list, s -> s.startsWith("a"));
        Assertions.assertArrayEquals(new int[] { 0, 2, 4 }, indices);

        List<String> empty = new ArrayList<>();
        int[] emptyIndices = CommonUtil.indicesOfAll(empty, s -> true);
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);

        LinkedList<String> linkedList = new LinkedList<>(list);
        int[] linkedIndices = CommonUtil.indicesOfAll(linkedList, s -> s.startsWith("a"));
        Assertions.assertArrayEquals(new int[] { 0, 2, 4 }, linkedIndices);
    }

    @Test
    public void testIndicesOfAll_collection_withPredicateAndFromIndex() {
        List<String> list = Arrays.asList("apple", "banana", "apricot", "cherry", "avocado");
        int[] indices = CommonUtil.indicesOfAll(list, s -> s.startsWith("a"), 1);
        Assertions.assertArrayEquals(new int[] { 2, 4 }, indices);

        int[] beyondIndices = CommonUtil.indicesOfAll(list, s -> s.startsWith("a"), 10);
        Assertions.assertArrayEquals(new int[] {}, beyondIndices);
    }

    @Test
    public void testEdgeCases() {
        Integer[] allSame = { 5, 5, 5, 5, 5 };
        int[] minIndices = CommonUtil.indicesOfAllMin(allSame);
        Assertions.assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, minIndices);
        int[] maxIndices = CommonUtil.indicesOfAllMax(allSame);
        Assertions.assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, maxIndices);

        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, "a", Integer.MAX_VALUE));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, "a", Integer.MIN_VALUE));
    }

    @Test
    public void testWithSpecialFloatingPointValues() {
        double[] arrWithNaN = { 1.0, Double.NaN, 3.0, Double.NaN };
        Assertions.assertEquals(1, CommonUtil.indexOf(arrWithNaN, Double.NaN));
        Assertions.assertEquals(3, CommonUtil.lastIndexOf(arrWithNaN, Double.NaN));

        double[] arrWithInf = { 1.0, Double.POSITIVE_INFINITY, 3.0, Double.NEGATIVE_INFINITY };
        Assertions.assertEquals(1, CommonUtil.indexOf(arrWithInf, Double.POSITIVE_INFINITY));
        Assertions.assertEquals(3, CommonUtil.indexOf(arrWithInf, Double.NEGATIVE_INFINITY));
    }

    @Test
    public void testGetDescendingIteratorIfPossible() {
        Deque<String> deque = new ArrayDeque<>(Arrays.asList("a", "b", "c"));
        Iterator<String> descIter = CommonUtil.getDescendingIteratorIfPossible(deque);
        Assertions.assertNotNull(descIter);

        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Iterator<String> listIter = CommonUtil.getDescendingIteratorIfPossible(list);
        Assertions.assertNull(listIter);
    }

    @Test
    public void testCreateMask() {
        Runnable mask = CommonUtil.createMask(Runnable.class);
        Assertions.assertNotNull(mask);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> mask.run());

        Comparator<?> compMask = CommonUtil.createMask(Comparator.class);
        Assertions.assertNotNull(compMask);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> compMask.compare(null, null));
    }
}
