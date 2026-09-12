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
import java.util.LinkedList;
import java.util.ListIterator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CommonUtilReverseTest extends CommonUtilTestSupport {
    @Test
    public void testReverse_BooleanArray() {
        boolean[] arr = { true, false, true, false };
        CommonUtil.reverse(arr);
        boolean[] expected = { false, true, false, true };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testReverse_IntArray() {
        int[] arr = { 1, 2, 3, 4 };
        CommonUtil.reverse(arr);
        int[] expected = { 4, 3, 2, 1 };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testReverse_IntArray_WithRange() {
        int[] arr = { 1, 2, 3, 4, 5 };
        CommonUtil.reverse(arr, 1, 4);
        int[] expected = { 1, 4, 3, 2, 5 };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testReverse_ObjectArray() {
        String[] arr = { "a", "b", "c", "d" };
        CommonUtil.reverse(arr);
        String[] expected = { "d", "c", "b", "a" };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testReverse_List() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.reverse(list);
        assertEquals(Arrays.asList("d", "c", "b", "a"), list);
    }

    @Test
    public void testReverse_List_WithRange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        CommonUtil.reverse(list, 1, 4);
        assertEquals(Arrays.asList("a", "d", "c", "b", "e"), list);
    }

    @Test
    public void testReverseByteRange() {
        byte[] a = { 1, 2, 3, 4, 5 };
        byte[] expected = { 1, 4, 3, 2, 5 };
        CommonUtil.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testReverseShortRange() {
        short[] a = { 1, 2, 3, 4, 5 };
        short[] expected = { 1, 4, 3, 2, 5 };
        CommonUtil.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testReverseIntRange() {
        int[] a = { 1, 2, 3, 4, 5 };
        int[] expected = { 1, 4, 3, 2, 5 };
        CommonUtil.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testReverseLongRange() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        long[] expected = { 1L, 4L, 3L, 2L, 5L };
        CommonUtil.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testReverseFloatRange() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] expected = { 1.0f, 4.0f, 3.0f, 2.0f, 5.0f };
        CommonUtil.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a, 0.0f);
    }

    @Test
    public void testReverseDoubleRange() {
        double[] a = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] expected = { 1.0, 4.0, 3.0, 2.0, 5.0 };
        CommonUtil.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a, 0.0);
    }

    @Test
    public void testReverseObjectRange() {
        String[] a = { "a", "b", "c", "d", "e" };
        String[] expected = { "a", "d", "c", "b", "e" };
        CommonUtil.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testReverseBoolean() {
        boolean[] a = { true, false, true, false };
        boolean[] expected = { false, true, false, true };
        CommonUtil.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        boolean[] empty = {};
        CommonUtil.reverse(empty);
        Assertions.assertArrayEquals(new boolean[0], empty);

        CommonUtil.reverse((boolean[]) null);

        boolean[] single = { true };
        CommonUtil.reverse(single);
        Assertions.assertArrayEquals(new boolean[] { true }, single);
    }

    @Test
    public void testReverseChar() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] expected = { 'd', 'c', 'b', 'a' };
        CommonUtil.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        char[] empty = {};
        CommonUtil.reverse(empty);
        Assertions.assertArrayEquals(new char[0], empty);

        CommonUtil.reverse((char[]) null);
    }

    @Test
    public void testReverseByte() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] expected = { 4, 3, 2, 1 };
        CommonUtil.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        byte[] empty = {};
        CommonUtil.reverse(empty);
        Assertions.assertArrayEquals(new byte[0], empty);

        CommonUtil.reverse((byte[]) null);
    }

    @Test
    public void testReverseShort() {
        short[] a = { 1, 2, 3, 4 };
        short[] expected = { 4, 3, 2, 1 };
        CommonUtil.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        short[] empty = {};
        CommonUtil.reverse(empty);
        Assertions.assertArrayEquals(new short[0], empty);

        CommonUtil.reverse((short[]) null);
    }

    @Test
    public void testReverseInt() {
        int[] a = { 1, 2, 3, 4 };
        int[] expected = { 4, 3, 2, 1 };
        CommonUtil.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        int[] empty = {};
        CommonUtil.reverse(empty);
        Assertions.assertArrayEquals(new int[0], empty);

        CommonUtil.reverse((int[]) null);
    }

    @Test
    public void testReverseLong() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] expected = { 4L, 3L, 2L, 1L };
        CommonUtil.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        long[] empty = {};
        CommonUtil.reverse(empty);
        Assertions.assertArrayEquals(new long[0], empty);

        CommonUtil.reverse((long[]) null);
    }

    @Test
    public void testReverseFloat() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] expected = { 4.0f, 3.0f, 2.0f, 1.0f };
        CommonUtil.reverse(a);
        Assertions.assertArrayEquals(expected, a, 0.0f);

        float[] empty = {};
        CommonUtil.reverse(empty);
        Assertions.assertArrayEquals(new float[0], empty, 0.0f);

        CommonUtil.reverse((float[]) null);
    }

    @Test
    public void testReverseDouble() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] expected = { 4.0, 3.0, 2.0, 1.0 };
        CommonUtil.reverse(a);
        Assertions.assertArrayEquals(expected, a, 0.0);

        double[] empty = {};
        CommonUtil.reverse(empty);
        Assertions.assertArrayEquals(new double[0], empty, 0.0);

        CommonUtil.reverse((double[]) null);
    }

    @Test
    public void testReverseObject() {
        String[] a = { "a", "b", "c", "d" };
        String[] expected = { "d", "c", "b", "a" };
        CommonUtil.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        String[] empty = {};
        CommonUtil.reverse(empty);
        Assertions.assertArrayEquals(new String[0], empty);

        CommonUtil.reverse((String[]) null);
    }

    @Test
    public void testReverseList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.reverse(list);
        Assertions.assertEquals(Arrays.asList("d", "c", "b", "a"), list);

        List<String> empty = new ArrayList<>();
        CommonUtil.reverse(empty);
        Assertions.assertTrue(empty.isEmpty());

        CommonUtil.reverse((List<?>) null);

        List<String> single = new ArrayList<>(Arrays.asList("a"));
        CommonUtil.reverse(single);
        Assertions.assertEquals(Arrays.asList("a"), single);
    }

    @Test
    public void testReverseCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.reverse((Collection<?>) list);
        Assertions.assertEquals(Arrays.asList("d", "c", "b", "a"), list);

        Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.reverse(set);
        Assertions.assertEquals(4, set.size());

        Collection<String> empty = new ArrayList<>();
        CommonUtil.reverse(empty);
        Assertions.assertTrue(empty.isEmpty());

        CommonUtil.reverse((Collection<?>) null);

        Collection<String> single = new ArrayList<>(Arrays.asList("a"));
        CommonUtil.reverse(single);
        Assertions.assertEquals(1, single.size());
    }

    @Test
    public void testReverseBooleanRange() {
        boolean[] a = { true, false, true, false, true };
        boolean[] expected = { true, false, true, false, true };
        CommonUtil.reverse(a, 1, 4);
        expected[1] = false;
        expected[2] = true;
        expected[3] = false;
        Assertions.assertArrayEquals(expected, a);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.reverse(a, -1, 3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.reverse(a, 0, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.reverse(a, 3, 2));

        boolean[] b = { true, false, true };
        boolean[] original = b.clone();
        CommonUtil.reverse(b, 1, 1);
        Assertions.assertArrayEquals(original, b);
    }

    @Test
    public void testReverseCharRange() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        char[] expected = { 'a', 'd', 'c', 'b', 'e' };
        CommonUtil.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.reverse(a, -1, 3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.reverse(a, 0, 10));
    }

    @Test
    public void testReverseListRange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        CommonUtil.reverse(list, 1, 4);
        Assertions.assertEquals(Arrays.asList("a", "d", "c", "b", "e"), list);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.reverse(list, -1, 3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.reverse(list, 0, 10));

        List<String> list2 = new ArrayList<>(Arrays.asList("a", "b", "c"));
        CommonUtil.reverse(list2, 1, 1);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), list2);
    }

    @Test
    public void testReverseToList() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> reversed = CommonUtil.toReversedList(list);
        assertEquals(5, reversed.size());
        assertEquals(Integer.valueOf(5), reversed.get(0));
        assertEquals(Integer.valueOf(1), reversed.get(4));
    }

    @Test
    public void testReverseSort_IntArray() {
        int[] arr = { 3, 1, 4, 1, 5 };
        CommonUtil.reverseSort(arr);
        int[] expected = { 5, 4, 3, 1, 1 };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testReverseSort_ObjectArray() {
        String[] arr = { "c", "a", "b" };
        CommonUtil.reverseSort(arr);
        String[] expected = { "c", "b", "a" };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testReverseSortBooleanArrayRange() {
        boolean[] array = { false, false, true, false, true };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new boolean[] { false, true, false, false, true }, array);
    }

    @Test
    public void testReverseSortCharArray() {
        char[] array = { 'a', 'c', 'b', 'd' };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new char[] { 'd', 'c', 'b', 'a' }, array);
    }

    @Test
    public void testReverseSortCharArrayRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new char[] { 'a', 'd', 'c', 'b', 'e' }, array);
    }

    @Test
    public void testReverseSortByteArray() {
        byte[] array = { 1, 3, 2, 4 };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new byte[] { 4, 3, 2, 1 }, array);
    }

    @Test
    public void testReverseSortByteArrayRange() {
        byte[] array = { 1, 2, 3, 4, 5 };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new byte[] { 1, 4, 3, 2, 5 }, array);
    }

    @Test
    public void testReverseSortShortArray() {
        short[] array = { 1, 3, 2, 4 };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new short[] { 4, 3, 2, 1 }, array);
    }

    @Test
    public void testReverseSortShortArrayRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new short[] { 1, 4, 3, 2, 5 }, array);
    }

    @Test
    public void testReverseSortIntArray() {
        int[] array = { 1, 3, 2, 4 };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new int[] { 4, 3, 2, 1 }, array);
    }

    @Test
    public void testReverseSortIntArrayRange() {
        int[] array = { 1, 2, 3, 4, 5 };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new int[] { 1, 4, 3, 2, 5 }, array);
    }

    @Test
    public void testReverseSortLongArray() {
        long[] array = { 1L, 3L, 2L, 4L };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new long[] { 4L, 3L, 2L, 1L }, array);
    }

    @Test
    public void testReverseSortLongArrayRange() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new long[] { 1L, 4L, 3L, 2L, 5L }, array);
    }

    @Test
    public void testReverseSortFloatArray() {
        float[] array = { 1.0f, 3.0f, 2.0f, 4.0f };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new float[] { 4.0f, 3.0f, 2.0f, 1.0f }, array);
    }

    @Test
    public void testReverseSortFloatArrayRange() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new float[] { 1.0f, 4.0f, 3.0f, 2.0f, 5.0f }, array);
    }

    @Test
    public void testReverseSortDoubleArray() {
        double[] array = { 1.0, 3.0, 2.0, 4.0 };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new double[] { 4.0, 3.0, 2.0, 1.0 }, array);
    }

    @Test
    public void testReverseSortDoubleArrayRange() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new double[] { 1.0, 4.0, 3.0, 2.0, 5.0 }, array);
    }

    @Test
    public void testReverseSortObjectArray() {
        String[] array = { "a", "c", "b", "d" };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new String[] { "d", "c", "b", "a" }, array);
    }

    @Test
    public void testReverseSortObjectArrayRange() {
        String[] array = { "a", "b", "c", "d", "e" };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new String[] { "a", "d", "c", "b", "e" }, array);
    }

    @Test
    public void testReverseSortList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "c", "b", "d"));
        CommonUtil.reverseSort(list);
        assertEquals(Arrays.asList("d", "c", "b", "a"), list);
    }

    @Test
    public void testReverseSortListRange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        CommonUtil.reverseSort(list, 1, 4);
        assertEquals(Arrays.asList("a", "d", "c", "b", "e"), list);
    }

    @Test
    public void testReverseSortBooleanArray() {
        CommonUtil.reverseSort((boolean[]) null);

        boolean[] array = { false, true, false, true, true };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new boolean[] { true, true, true, false, false }, array);
    }

    @Test
    public void testReverseSortBy() {
        List<String> list = Arrays.asList("a", "zoo", "be", "apple");
        CommonUtil.reverseSortBy(list, String::length);
        assertEquals("apple", list.get(0));
        assertEquals(3, list.get(1).length());
        assertEquals(2, list.get(2).length());
        assertEquals("a", list.get(3));
    }

    @Test
    public void testReverseSortByInt() {
        List<String> list = Arrays.asList("a", "cat", "apple");
        CommonUtil.reverseSortByInt(list, String::length);
        assertEquals("apple", list.get(0));
        assertEquals("cat", list.get(1));
        assertEquals("a", list.get(2));
    }

    @Test
    public void testReverseSortByLong() {
        List<String> list = Arrays.asList("a", "cat", "apple");
        CommonUtil.reverseSortByLong(list, s -> (long) s.length());
        assertEquals("apple", list.get(0));
        assertEquals("cat", list.get(1));
        assertEquals("a", list.get(2));
    }

    @Test
    public void testReverseSortByFloat() {
        List<String> list = Arrays.asList("a", "cat", "apple");
        CommonUtil.reverseSortByFloat(list, s -> (float) s.length());
        assertEquals("apple", list.get(0));
        assertEquals("cat", list.get(1));
        assertEquals("a", list.get(2));
    }

    @Test
    public void testReverseSortByDouble() {
        List<String> list = Arrays.asList("a", "cat", "apple");
        CommonUtil.reverseSortByDouble(list, s -> (double) s.length());
        assertEquals("apple", list.get(0));
        assertEquals("cat", list.get(1));
        assertEquals("a", list.get(2));
    }
    private static final class CountingLinkedList extends LinkedList<Object> {
        private static final long serialVersionUID = 1L;

        int gets;
        int sets;
        int listIterators;

        @Override
        public Object get(final int index) {
            gets++;
            return super.get(index);
        }

        @Override
        public Object set(final int index, final Object element) {
            sets++;
            return super.set(index, element);
        }

        @Override
        public ListIterator<Object> listIterator(final int index) {
            listIterators++;
            return super.listIterator(index);
        }
    }

    @Test
    public void testReverse_listStrategyDependsOnSizeNotOnlyOnRandomAccess() {
        final CountingLinkedList small = new CountingLinkedList();

        for (int i = 0; i < 3; i++) {
            small.add("e" + i);
        }

        CommonUtil.reverse(small);

        final int smallListIterators = small.listIterators;
        final int smallSets = small.sets;
        final int smallGets = small.gets;

        assertEquals(0, smallListIterators);
        assertEquals(2, smallSets);
        assertEquals(1, smallGets);
        assertEquals(Arrays.asList("e2", "e1", "e0"), new ArrayList<>(small));

        final CountingLinkedList large = new CountingLinkedList();

        for (int i = 0; i < 20; i++) {
            large.add("e" + i);
        }

        CommonUtil.reverse(large);

        final int largeListIterators = large.listIterators;
        final int largeSets = large.sets;
        final int largeGets = large.gets;

        assertEquals(2, largeListIterators);
        assertEquals(0, largeSets);
        assertEquals(0, largeGets);
        assertEquals("e19", new ArrayList<>(large).get(0));
    }

}
