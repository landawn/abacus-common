package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CommonUtilShuffleTest extends CommonUtilTestSupport {
    @Test
    public void testShuffle() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        CommonUtil.shuffle(list);
        assertEquals(5, list.size());
        assertTrue(list.contains(1));
        assertTrue(list.contains(5));
    }

    @Test
    public void testShuffleFloatArrayWithRange() {

        for (int i = 0; i < 10000; i++) {
            float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
            float[] original = arr.clone();
            CommonUtil.shuffle(arr, 1, 4);

            Assertions.assertEquals(original[0], arr[0]);
            Assertions.assertEquals(original[4], arr[4], 0.000001);

            Arrays.sort(arr);
            Arrays.sort(original);
            Assertions.assertArrayEquals(original, arr);
        }
    }

    @Test
    public void testShuffleFloatArrayWithRandom() {

        for (int i = 0; i < 10000; i++) {
            float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
            float[] original = arr.clone();
            Random rnd = new Random(42);
            CommonUtil.shuffle(arr, rnd);

            Arrays.sort(arr);
            Arrays.sort(original);
            Assertions.assertArrayEquals(original, arr);
        }
    }

    @Test
    public void testShuffleFloatArrayWithRangeAndRandom() {

        for (int i = 0; i < 10000; i++) {
            float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
            float[] original = arr.clone();
            Random rnd = new Random(42);
            CommonUtil.shuffle(arr, 1, 4, rnd);

            Assertions.assertEquals(original[0], arr[0]);
            Assertions.assertEquals(original[4], arr[4]);

            Arrays.sort(arr);
            Arrays.sort(original);
            Assertions.assertArrayEquals(original, arr);
        }
    }

    @Test
    public void testShuffleDoubleArray() {

        for (int i = 0; i < 10000; i++) {
            double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
            double[] original = arr.clone();
            CommonUtil.shuffle(arr);

            Arrays.sort(arr);
            Arrays.sort(original);
            Assertions.assertArrayEquals(original, arr);
        }
    }

    @Test
    public void testShuffleDoubleArrayWithRange() {
        for (int i = 0; i < 10000; i++) {
            double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
            double[] original = arr.clone();
            CommonUtil.shuffle(arr, 1, 4);

            Assertions.assertEquals(original[0], arr[0]);
            Assertions.assertEquals(original[4], arr[4], 0.000001);

            Arrays.sort(arr);
            Arrays.sort(original);
            Assertions.assertArrayEquals(original, arr);
        }
    }

    @Test
    public void testShuffleDoubleArrayWithRandom() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] original = arr.clone();
        Random rnd = new Random(42);
        CommonUtil.shuffle(arr, rnd);

        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    @Test
    public void testShuffleDoubleArrayWithRangeAndRandom() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] original = arr.clone();
        Random rnd = new Random(42);
        CommonUtil.shuffle(arr, 1, 4, rnd);

        Assertions.assertEquals(original[0], arr[0]);
        Assertions.assertEquals(original[4], arr[4]);

        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    @Test
    public void testShuffleObjectArray() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String[] original = arr.clone();
        CommonUtil.shuffle(arr);

        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    @Test
    public void testShuffleObjectArrayWithRange() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String[] original = arr.clone();
        CommonUtil.shuffle(arr, 1, 4);

        Assertions.assertEquals(original[0], arr[0]);
        Assertions.assertEquals(original[4], arr[4]);

        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    @Test
    public void testShuffleObjectArrayWithRandom() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String[] original = arr.clone();
        Random rnd = new Random(42);
        CommonUtil.shuffle(arr, rnd);

        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    @Test
    public void testShuffleObjectArrayWithRangeAndRandom() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String[] original = arr.clone();
        Random rnd = new Random(42);
        CommonUtil.shuffle(arr, 1, 4, rnd);

        Assertions.assertEquals(original[0], arr[0]);
        Assertions.assertEquals(original[4], arr[4]);

        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    @Test
    public void testShuffleList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        List<String> original = new ArrayList<>(list);
        CommonUtil.shuffle(list);

        Collections.sort(list);
        Collections.sort(original);
        Assertions.assertEquals(original, list);
    }

    @Test
    public void testShuffleListWithRandom() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        List<String> original = new ArrayList<>(list);
        Random rnd = new Random(42);
        CommonUtil.shuffle(list, rnd);

        Collections.sort(list);
        Collections.sort(original);
        Assertions.assertEquals(original, list);
    }

    @Test
    public void testShuffleCollection() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        List<String> original = new ArrayList<>(coll);
        CommonUtil.shuffle(coll);

        List<String> sorted = new ArrayList<>(coll);
        Collections.sort(sorted);
        Collections.sort(original);
        Assertions.assertEquals(original, sorted);
    }

    @Test
    public void testShuffleCollectionWithRandom() {
        Collection<String> coll = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d", "e"));
        List<String> original = new ArrayList<>(coll);
        Random rnd = new Random(42);
        CommonUtil.shuffle(coll, rnd);

        List<String> sorted = new ArrayList<>(coll);
        Collections.sort(sorted);
        Collections.sort(original);
        Assertions.assertEquals(original, sorted);
    }

    @Test
    public void testShuffleCollectionLessThanTwoElements() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a"));
        CommonUtil.shuffle(coll);
        Assertions.assertEquals(1, coll.size());
        Assertions.assertTrue(coll.contains("a"));
    }

    @Test
    public void testShuffleBooleanWithRandom() {
        boolean[] a = { true, false, true, false, true };
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleBooleanRangeWithRandom() {
        boolean[] a = { true, false, true, false, true };
        boolean first = a[0];
        boolean last = a[4];
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleChar() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        char[] original = a.clone();
        CommonUtil.shuffle(a);
        Assertions.assertEquals(original.length, a.length);
        Arrays.sort(original);
        Arrays.sort(a);
        Assertions.assertArrayEquals(original, a);
    }

    @Test
    public void testShuffleCharRange() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        char first = a[0];
        char last = a[4];
        CommonUtil.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleCharWithRandom() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleCharRangeWithRandom() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        char first = a[0];
        char last = a[4];
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleByte() {
        byte[] a = { 1, 2, 3, 4, 5 };
        byte[] original = a.clone();
        CommonUtil.shuffle(a);
        Assertions.assertEquals(original.length, a.length);
        Arrays.sort(original);
        Arrays.sort(a);
        Assertions.assertArrayEquals(original, a);
    }

    @Test
    public void testShuffleByteRange() {
        byte[] a = { 1, 2, 3, 4, 5 };
        byte first = a[0];
        byte last = a[4];
        CommonUtil.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleByteWithRandom() {
        byte[] a = { 1, 2, 3, 4, 5 };
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleByteRangeWithRandom() {
        byte[] a = { 1, 2, 3, 4, 5 };
        byte first = a[0];
        byte last = a[4];
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleShort() {
        short[] a = { 1, 2, 3, 4, 5 };
        short[] original = a.clone();
        CommonUtil.shuffle(a);
        Assertions.assertEquals(original.length, a.length);
        Arrays.sort(original);
        Arrays.sort(a);
        Assertions.assertArrayEquals(original, a);
    }

    @Test
    public void testShuffleShortRange() {
        short[] a = { 1, 2, 3, 4, 5 };
        short first = a[0];
        short last = a[4];
        CommonUtil.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleShortWithRandom() {
        short[] a = { 1, 2, 3, 4, 5 };
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleShortRangeWithRandom() {
        short[] a = { 1, 2, 3, 4, 5 };
        short first = a[0];
        short last = a[4];
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleInt() {
        int[] a = { 1, 2, 3, 4, 5 };
        int[] original = a.clone();
        CommonUtil.shuffle(a);
        Assertions.assertEquals(original.length, a.length);
        Arrays.sort(original);
        Arrays.sort(a);
        Assertions.assertArrayEquals(original, a);
    }

    @Test
    public void testShuffleIntRange() {
        int[] a = { 1, 2, 3, 4, 5 };
        int first = a[0];
        int last = a[4];
        CommonUtil.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleIntWithRandom() {
        int[] a = { 1, 2, 3, 4, 5 };
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleIntRangeWithRandom() {
        int[] a = { 1, 2, 3, 4, 5 };
        int first = a[0];
        int last = a[4];
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleLong() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        long[] original = a.clone();
        CommonUtil.shuffle(a);
        Assertions.assertEquals(original.length, a.length);
        Arrays.sort(original);
        Arrays.sort(a);
        Assertions.assertArrayEquals(original, a);
    }

    @Test
    public void testShuffleLongRange() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        long first = a[0];
        long last = a[4];
        CommonUtil.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleLongWithRandom() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleLongRangeWithRandom() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        long first = a[0];
        long last = a[4];
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleFloat() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] original = a.clone();
        CommonUtil.shuffle(a);
        Assertions.assertEquals(original.length, a.length);
        Arrays.sort(original);
        Arrays.sort(a);
        Assertions.assertArrayEquals(original, a, 0.0f);
    }

    @Test
    public void testShuffleFloatArrayEmpty() {
        float[] arr = {};
        CommonUtil.shuffle(arr, 0, 0, new Random());
        Assertions.assertEquals(0, arr.length);
    }

    @Test
    public void testShuffleFloatArrayNull() {
        float[] arr = null;
        CommonUtil.shuffle(arr, new Random());
        Assertions.assertNull(arr);
    }

    @Test
    public void testShuffleListEmpty() {
        List<String> list = new ArrayList<>();
        CommonUtil.shuffle(list);
        Assertions.assertTrue(list.isEmpty());
    }

    @Test
    public void testShuffleListSingleElement() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        CommonUtil.shuffle(list);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("a", list.get(0));
    }

    @Test
    public void testShuffleCollectionEmpty() {
        Collection<String> coll = new ArrayList<>();
        CommonUtil.shuffle(coll);
        Assertions.assertTrue(coll.isEmpty());
    }

    @Test
    public void testShuffleBoolean() {
        boolean[] a = { true, false, true, false, true };
        boolean[] original = a.clone();
        CommonUtil.shuffle(a);
        Assertions.assertEquals(original.length, a.length);

        boolean[] empty = {};
        CommonUtil.shuffle(empty);
        Assertions.assertArrayEquals(new boolean[0], empty);

        CommonUtil.shuffle((boolean[]) null);

        boolean[] single = { true };
        CommonUtil.shuffle(single);
        Assertions.assertArrayEquals(new boolean[] { true }, single);
    }

    @Test
    public void testShuffleBooleanRange() {
        boolean[] a = { true, false, true, false, true };
        boolean first = a[0];
        boolean last = a[4];
        CommonUtil.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.shuffle(a, -1, 3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.shuffle(a, 0, 10));
    }
}
