package com.landawn.abacus.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

import lombok.Data;

public class CommonUtil107Test extends TestBase {

    // Tests for shuffle methods - float array
    @Test
    public void testShuffleFloatArrayWithRange() {

        for (int i = 0; i < 10000; i++) {
            float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
            float[] original = arr.clone();
            N.shuffle(arr, 1, 4);

            // Check that elements outside range are unchanged
            Assertions.assertEquals(original[0], arr[0]);
            Assertions.assertEquals(original[4], arr[4], 0.000001);

            // Check that array still contains same elements
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
            N.shuffle(arr, rnd);

            // Check that array still contains same elements
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
            N.shuffle(arr, 1, 4, rnd);

            // Check that elements outside range are unchanged
            Assertions.assertEquals(original[0], arr[0]);
            Assertions.assertEquals(original[4], arr[4]);

            // Check that array still contains same elements
            Arrays.sort(arr);
            Arrays.sort(original);
            Assertions.assertArrayEquals(original, arr);
        }
    }

    @Test
    public void testShuffleFloatArrayEmpty() {
        float[] arr = {};
        N.shuffle(arr, 0, 0, new Random());
        Assertions.assertEquals(0, arr.length);
    }

    @Test
    public void testShuffleFloatArrayNull() {
        float[] arr = null;
        N.shuffle(arr, new Random());
        Assertions.assertNull(arr);
    }

    // Tests for shuffle methods - double array
    @Test
    public void testShuffleDoubleArray() {

        for (int i = 0; i < 10000; i++) {
            double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
            double[] original = arr.clone();
            N.shuffle(arr);

            // Check that array still contains same elements
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
            N.shuffle(arr, 1, 4);

            // Check that elements outside range are unchanged
            Assertions.assertEquals(original[0], arr[0]);
            Assertions.assertEquals(original[4], arr[4], 0.000001);

            // Check that array still contains same elements
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
        N.shuffle(arr, rnd);

        // Check that array still contains same elements
        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    @Test
    public void testShuffleDoubleArrayWithRangeAndRandom() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] original = arr.clone();
        Random rnd = new Random(42);
        N.shuffle(arr, 1, 4, rnd);

        // Check that elements outside range are unchanged
        Assertions.assertEquals(original[0], arr[0]);
        Assertions.assertEquals(original[4], arr[4]);

        // Check that array still contains same elements
        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    // Tests for shuffle methods - Object array
    @Test
    public void testShuffleObjectArray() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String[] original = arr.clone();
        N.shuffle(arr);

        // Check that array still contains same elements
        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    @Test
    public void testShuffleObjectArrayWithRange() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String[] original = arr.clone();
        N.shuffle(arr, 1, 4);

        // Check that elements outside range are unchanged
        Assertions.assertEquals(original[0], arr[0]);
        Assertions.assertEquals(original[4], arr[4]);

        // Check that array still contains same elements
        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    @Test
    public void testShuffleObjectArrayWithRandom() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String[] original = arr.clone();
        Random rnd = new Random(42);
        N.shuffle(arr, rnd);

        // Check that array still contains same elements
        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    @Test
    public void testShuffleObjectArrayWithRangeAndRandom() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String[] original = arr.clone();
        Random rnd = new Random(42);
        N.shuffle(arr, 1, 4, rnd);

        // Check that elements outside range are unchanged
        Assertions.assertEquals(original[0], arr[0]);
        Assertions.assertEquals(original[4], arr[4]);

        // Check that array still contains same elements
        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    // Tests for shuffle methods - List
    @Test
    public void testShuffleList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        List<String> original = new ArrayList<>(list);
        N.shuffle(list);

        // Check that list still contains same elements
        Collections.sort(list);
        Collections.sort(original);
        Assertions.assertEquals(original, list);
    }

    @Test
    public void testShuffleListWithRandom() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        List<String> original = new ArrayList<>(list);
        Random rnd = new Random(42);
        N.shuffle(list, rnd);

        // Check that list still contains same elements
        Collections.sort(list);
        Collections.sort(original);
        Assertions.assertEquals(original, list);
    }

    @Test
    public void testShuffleListEmpty() {
        List<String> list = new ArrayList<>();
        N.shuffle(list);
        Assertions.assertTrue(list.isEmpty());
    }

    @Test
    public void testShuffleListSingleElement() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        N.shuffle(list);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("a", list.get(0));
    }

    // Tests for shuffle methods - Collection
    @Test
    public void testShuffleCollection() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        List<String> original = new ArrayList<>(coll);
        N.shuffle(coll);

        // Check that collection still contains same elements
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
        N.shuffle(coll, rnd);

        // Check that collection still contains same elements
        List<String> sorted = new ArrayList<>(coll);
        Collections.sort(sorted);
        Collections.sort(original);
        Assertions.assertEquals(original, sorted);
    }

    @Test
    public void testShuffleCollectionEmpty() {
        Collection<String> coll = new ArrayList<>();
        N.shuffle(coll);
        Assertions.assertTrue(coll.isEmpty());
    }

    @Test
    public void testShuffleCollectionLessThanTwoElements() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a"));
        N.shuffle(coll);
        Assertions.assertEquals(1, coll.size());
        Assertions.assertTrue(coll.contains("a"));
    }

    // Tests for swap methods - boolean array
    @Test
    public void testSwapBooleanArray() {
        boolean[] arr = { true, false, true, false };
        N.swap(arr, 0, 3);
        Assertions.assertFalse(arr[0]);
        Assertions.assertTrue(arr[3]);
    }

    // Tests for swap methods - char array
    @Test
    public void testSwapCharArray() {
        char[] arr = { 'a', 'b', 'c', 'd' };
        N.swap(arr, 1, 2);
        Assertions.assertEquals('c', arr[1]);
        Assertions.assertEquals('b', arr[2]);
    }

    // Tests for swap methods - byte array
    @Test
    public void testSwapByteArray() {
        byte[] arr = { 1, 2, 3, 4 };
        N.swap(arr, 0, 3);
        Assertions.assertEquals(4, arr[0]);
        Assertions.assertEquals(1, arr[3]);
    }

    // Tests for swap methods - short array
    @Test
    public void testSwapShortArray() {
        short[] arr = { 10, 20, 30, 40 };
        N.swap(arr, 1, 3);
        Assertions.assertEquals(40, arr[1]);
        Assertions.assertEquals(20, arr[3]);
    }

    // Tests for swap methods - int array
    @Test
    public void testSwapIntArray() {
        int[] arr = { 100, 200, 300, 400 };
        N.swap(arr, 0, 2);
        Assertions.assertEquals(300, arr[0]);
        Assertions.assertEquals(100, arr[2]);
    }

    // Tests for swap methods - long array
    @Test
    public void testSwapLongArray() {
        long[] arr = { 1000L, 2000L, 3000L, 4000L };
        N.swap(arr, 1, 2);
        Assertions.assertEquals(3000L, arr[1]);
        Assertions.assertEquals(2000L, arr[2]);
    }

    // Tests for swap methods - float array
    @Test
    public void testSwapFloatArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f };
        N.swap(arr, 0, 3);
        Assertions.assertEquals(4.0f, arr[0]);
        Assertions.assertEquals(1.0f, arr[3]);
    }

    // Tests for swap methods - double array
    @Test
    public void testSwapDoubleArray() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0 };
        N.swap(arr, 1, 3);
        Assertions.assertEquals(4.0, arr[1]);
        Assertions.assertEquals(2.0, arr[3]);
    }

    // Tests for swap methods - Object array
    @Test
    public void testSwapObjectArray() {
        String[] arr = { "a", "b", "c", "d" };
        N.swap(arr, 0, 2);
        Assertions.assertEquals("c", arr[0]);
        Assertions.assertEquals("a", arr[2]);
    }

    // Tests for swap methods - List
    @Test
    public void testSwapList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        N.swap(list, 1, 3);
        Assertions.assertEquals("d", list.get(1));
        Assertions.assertEquals("b", list.get(3));
    }

    // Tests for swap methods - Pair
    @Test
    public void testSwapPair() {
        Pair<String, String> pair = Pair.of("left", "right");
        N.swap(pair);
        Assertions.assertEquals("right", pair.left());
        Assertions.assertEquals("left", pair.right());
    }

    @Test
    public void testSwapIfPair() {
        Pair<Integer, Integer> pair = Pair.of(1, 2);
        boolean swapped = N.swapIf(pair, p -> p.left() < p.right());
        Assertions.assertTrue(swapped);
        Assertions.assertEquals(2, pair.left());
        Assertions.assertEquals(1, pair.right());

        swapped = N.swapIf(pair, p -> p.left() < p.right());
        Assertions.assertFalse(swapped);
    }

    // Tests for swap methods - Triple
    @Test
    public void testSwapTriple() {
        Triple<String, Integer, String> triple = Triple.of("left", 42, "right");
        N.swap(triple);
        Assertions.assertEquals("right", triple.left());
        Assertions.assertEquals("left", triple.right());
        Assertions.assertEquals(42, triple.middle());
    }

    @Test
    public void testSwapIfTriple() {
        Triple<Integer, String, Integer> triple = Triple.of(1, "middle", 2);
        boolean swapped = N.swapIf(triple, t -> t.left() < t.right());
        Assertions.assertTrue(swapped);
        Assertions.assertEquals(2, triple.left());
        Assertions.assertEquals(1, triple.right());

        swapped = N.swapIf(triple, t -> t.left() < t.right());
        Assertions.assertFalse(swapped);
    }

    // Tests for fill methods - boolean array
    @Test
    public void testFillBooleanArray() {
        boolean[] arr = new boolean[5];
        N.fill(arr, true);
        for (boolean b : arr) {
            Assertions.assertTrue(b);
        }
    }

    @Test
    public void testFillBooleanArrayWithRange() {
        boolean[] arr = new boolean[5];
        N.fill(arr, 1, 4, true);
        Assertions.assertFalse(arr[0]);
        Assertions.assertTrue(arr[1]);
        Assertions.assertTrue(arr[2]);
        Assertions.assertTrue(arr[3]);
        Assertions.assertFalse(arr[4]);
    }

    @Test
    public void testFillBooleanArrayEmpty() {
        boolean[] arr = new boolean[0];
        N.fill(arr, true);
        Assertions.assertEquals(0, arr.length);
    }

    // Tests for fill methods - char array
    @Test
    public void testFillCharArray() {
        char[] arr = new char[5];
        N.fill(arr, 'x');
        for (char c : arr) {
            Assertions.assertEquals('x', c);
        }
    }

    @Test
    public void testFillCharArrayWithRange() {
        char[] arr = new char[5];
        N.fill(arr, 1, 4, 'x');
        Assertions.assertEquals('\0', arr[0]);
        Assertions.assertEquals('x', arr[1]);
        Assertions.assertEquals('x', arr[2]);
        Assertions.assertEquals('x', arr[3]);
        Assertions.assertEquals('\0', arr[4]);
    }

    // Tests for fill methods - byte array
    @Test
    public void testFillByteArray() {
        byte[] arr = new byte[5];
        N.fill(arr, (byte) 42);
        for (byte b : arr) {
            Assertions.assertEquals(42, b);
        }
    }

    @Test
    public void testFillByteArrayWithRange() {
        byte[] arr = new byte[5];
        N.fill(arr, 2, 5, (byte) 42);
        Assertions.assertEquals(0, arr[0]);
        Assertions.assertEquals(0, arr[1]);
        Assertions.assertEquals(42, arr[2]);
        Assertions.assertEquals(42, arr[3]);
        Assertions.assertEquals(42, arr[4]);
    }

    // Tests for fill methods - short array
    @Test
    public void testFillShortArray() {
        short[] arr = new short[5];
        N.fill(arr, (short) 100);
        for (short s : arr) {
            Assertions.assertEquals(100, s);
        }
    }

    @Test
    public void testFillShortArrayWithRange() {
        short[] arr = new short[5];
        N.fill(arr, 0, 3, (short) 100);
        Assertions.assertEquals(100, arr[0]);
        Assertions.assertEquals(100, arr[1]);
        Assertions.assertEquals(100, arr[2]);
        Assertions.assertEquals(0, arr[3]);
        Assertions.assertEquals(0, arr[4]);
    }

    // Tests for fill methods - int array
    @Test
    public void testFillIntArray() {
        int[] arr = new int[5];
        N.fill(arr, 999);
        for (int i : arr) {
            Assertions.assertEquals(999, i);
        }
    }

    @Test
    public void testFillIntArrayWithRange() {
        int[] arr = new int[5];
        N.fill(arr, 1, 3, 999);
        Assertions.assertEquals(0, arr[0]);
        Assertions.assertEquals(999, arr[1]);
        Assertions.assertEquals(999, arr[2]);
        Assertions.assertEquals(0, arr[3]);
        Assertions.assertEquals(0, arr[4]);
    }

    // Tests for fill methods - long array
    @Test
    public void testFillLongArray() {
        long[] arr = new long[5];
        N.fill(arr, 1000L);
        for (long l : arr) {
            Assertions.assertEquals(1000L, l);
        }
    }

    @Test
    public void testFillLongArrayWithRange() {
        long[] arr = new long[5];
        N.fill(arr, 2, 4, 1000L);
        Assertions.assertEquals(0L, arr[0]);
        Assertions.assertEquals(0L, arr[1]);
        Assertions.assertEquals(1000L, arr[2]);
        Assertions.assertEquals(1000L, arr[3]);
        Assertions.assertEquals(0L, arr[4]);
    }

    // Tests for fill methods - float array
    @Test
    public void testFillFloatArray() {
        float[] arr = new float[5];
        N.fill(arr, 3.14f);
        for (float f : arr) {
            Assertions.assertEquals(3.14f, f);
        }
    }

    @Test
    public void testFillFloatArrayWithRange() {
        float[] arr = new float[5];
        N.fill(arr, 0, 2, 3.14f);
        Assertions.assertEquals(3.14f, arr[0]);
        Assertions.assertEquals(3.14f, arr[1]);
        Assertions.assertEquals(0.0f, arr[2]);
        Assertions.assertEquals(0.0f, arr[3]);
        Assertions.assertEquals(0.0f, arr[4]);
    }

    // Tests for fill methods - double array
    @Test
    public void testFillDoubleArray() {
        double[] arr = new double[5];
        N.fill(arr, 2.718);
        for (double d : arr) {
            Assertions.assertEquals(2.718, d);
        }
    }

    @Test
    public void testFillDoubleArrayWithRange() {
        double[] arr = new double[5];
        N.fill(arr, 3, 5, 2.718);
        Assertions.assertEquals(0.0, arr[0]);
        Assertions.assertEquals(0.0, arr[1]);
        Assertions.assertEquals(0.0, arr[2]);
        Assertions.assertEquals(2.718, arr[3]);
        Assertions.assertEquals(2.718, arr[4]);
    }

    // Tests for fill methods - Object array
    @Test
    public void testFillObjectArray() {
        String[] arr = new String[5];
        N.fill(arr, "test");
        for (String s : arr) {
            Assertions.assertEquals("test", s);
        }
    }

    @Test
    public void testFillObjectArrayWithRange() {
        String[] arr = new String[5];
        N.fill(arr, 1, 4, "test");
        Assertions.assertNull(arr[0]);
        Assertions.assertEquals("test", arr[1]);
        Assertions.assertEquals("test", arr[2]);
        Assertions.assertEquals("test", arr[3]);
        Assertions.assertNull(arr[4]);
    }

    @Test
    public void testFillObjectArrayEmpty() {
        String[] arr = new String[0];
        N.fill(arr, "test");
        Assertions.assertEquals(0, arr.length);
    }

    // Tests for fill methods - List
    @Test
    public void testFillList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        N.fill(list, "x");
        for (String s : list) {
            Assertions.assertEquals("x", s);
        }
    }

    @Test
    public void testFillListWithRange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        N.fill(list, 1, 4, "x");
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertEquals("x", list.get(1));
        Assertions.assertEquals("x", list.get(2));
        Assertions.assertEquals("x", list.get(3));
        Assertions.assertEquals("e", list.get(4));
    }

    @Test
    public void testFillListExtending() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        N.fill(list, 0, 5, "x");
        Assertions.assertEquals(5, list.size());
        for (String s : list) {
            Assertions.assertEquals("x", s);
        }
    }

    @Test
    public void testFillListWithGaps() {
        List<String> list = new ArrayList<>();
        N.fill(list, 2, 5, "x");
        Assertions.assertEquals(5, list.size());
        Assertions.assertNull(list.get(0));
        Assertions.assertNull(list.get(1));
        Assertions.assertEquals("x", list.get(2));
        Assertions.assertEquals("x", list.get(3));
        Assertions.assertEquals("x", list.get(4));
    }

    // Tests for fill methods - Object (bean)
    @Test
    public void testFillBean() {
        TestBean bean = new TestBean();
        Beans.fill(bean);
        // Cannot verify exact values as they're random, but check not null
        Assertions.assertNotNull(bean);
    }

    @Test
    public void testFillBeanClass() {
        TestBean bean = Beans.fill(TestBean.class);
        Assertions.assertNotNull(bean);
    }

    @Test
    public void testFillBeanClassWithCount() {
        List<TestBean> beans = Beans.fill(TestBean.class, 3);
        Assertions.assertEquals(3, beans.size());
        for (TestBean bean : beans) {
            Assertions.assertNotNull(bean);
        }
    }

    // Tests for padLeft method
    @Test
    public void testPadLeft() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        boolean result = N.padLeft(list, 5, "x");
        Assertions.assertTrue(result);
        Assertions.assertEquals(5, list.size());
        Assertions.assertEquals("x", list.get(0));
        Assertions.assertEquals("x", list.get(1));
        Assertions.assertEquals("a", list.get(2));
        Assertions.assertEquals("b", list.get(3));
        Assertions.assertEquals("c", list.get(4));
    }

    @Test
    public void testPadLeftNoChange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        boolean result = N.padLeft(list, 3, "x");
        Assertions.assertFalse(result);
        Assertions.assertEquals(3, list.size());
    }

    @Test
    public void testPadLeftWithNull() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        boolean result = N.padLeft(list, 3, null);
        Assertions.assertTrue(result);
        Assertions.assertEquals(3, list.size());
        Assertions.assertNull(list.get(0));
        Assertions.assertNull(list.get(1));
        Assertions.assertEquals("a", list.get(2));
    }

    // Tests for padRight method
    @Test
    public void testPadRight() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "c"));
        boolean result = N.padRight(coll, 5, "x");
        Assertions.assertTrue(result);
        Assertions.assertEquals(5, coll.size());
        List<String> list = new ArrayList<>(coll);
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertEquals("b", list.get(1));
        Assertions.assertEquals("c", list.get(2));
        Assertions.assertEquals("x", list.get(3));
        Assertions.assertEquals("x", list.get(4));
    }

    @Test
    public void testPadRightNoChange() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "c"));
        boolean result = N.padRight(coll, 2, "x");
        Assertions.assertFalse(result);
        Assertions.assertEquals(3, coll.size());
    }

    @Test
    public void testPadRightWithNull() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a"));
        boolean result = N.padRight(coll, 3, null);
        Assertions.assertTrue(result);
        Assertions.assertEquals(3, coll.size());
        List<String> list = new ArrayList<>(coll);
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertNull(list.get(1));
        Assertions.assertNull(list.get(2));
    }

    // Tests for repeat method
    @Test
    public void testRepeat() {
        List<String> result = N.repeat("x", 5);
        Assertions.assertEquals(5, result.size());
        for (String s : result) {
            Assertions.assertEquals("x", s);
        }
    }

    @Test
    public void testRepeatZero() {
        List<String> result = N.repeat("x", 0);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testRepeatNull() {
        List<String> result = N.repeat(null, 3);
        Assertions.assertEquals(3, result.size());
        for (String s : result) {
            Assertions.assertNull(s);
        }
    }

    // Tests for repeatElements method
    @Test
    public void testRepeatElements() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = N.repeatElements(input, 2);
        Assertions.assertEquals(6, result.size());
        Assertions.assertEquals(Arrays.asList("a", "a", "b", "b", "c", "c"), result);
    }

    @Test
    public void testRepeatElementsZero() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = N.repeatElements(input, 0);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testRepeatElementsEmpty() {
        List<String> input = new ArrayList<>();
        List<String> result = N.repeatElements(input, 3);
        Assertions.assertTrue(result.isEmpty());
    }

    // Tests for repeatCollection method
    @Test
    public void testRepeatCollection() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = N.repeatCollection(input, 2);
        Assertions.assertEquals(6, result.size());
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "a", "b", "c"), result);
    }

    @Test
    public void testRepeatCollectionZero() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = N.repeatCollection(input, 0);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testRepeatCollectionEmpty() {
        List<String> input = new ArrayList<>();
        List<String> result = N.repeatCollection(input, 3);
        Assertions.assertTrue(result.isEmpty());
    }

    // Tests for repeatElementsToSize method
    @Test
    public void testRepeatElementsToSize() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = N.repeatElementsToSize(input, 8);
        Assertions.assertEquals(8, result.size());
        Assertions.assertEquals(Arrays.asList("a", "a", "a", "b", "b", "b", "c", "c"), result);
    }

    @Test
    public void testRepeatElementsToSizeExact() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = N.repeatElementsToSize(input, 6);
        Assertions.assertEquals(6, result.size());
        Assertions.assertEquals(Arrays.asList("a", "a", "b", "b", "c", "c"), result);
    }

    @Test
    public void testRepeatElementsToSizeZero() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = N.repeatElementsToSize(input, 0);
        Assertions.assertTrue(result.isEmpty());
    }

    // Tests for repeatCollectionToSize method
    @Test
    public void testRepeatCollectionToSize() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = N.repeatCollectionToSize(input, 7);
        Assertions.assertEquals(7, result.size());
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "a", "b", "c", "a"), result);
    }

    @Test
    public void testRepeatCollectionToSizeExact() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = N.repeatCollectionToSize(input, 6);
        Assertions.assertEquals(6, result.size());
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "a", "b", "c"), result);
    }

    @Test
    public void testRepeatCollectionToSizeZero() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = N.repeatCollectionToSize(input, 0);
        Assertions.assertTrue(result.isEmpty());
    }

    // Tests for copy methods - boolean array
    @Test
    public void testCopyBooleanArray() {
        boolean[] src = { true, false, true, false, true };
        boolean[] dest = new boolean[5];
        N.copy(src, 1, dest, 2, 2);
        Assertions.assertFalse(dest[0]);
        Assertions.assertFalse(dest[1]);
        Assertions.assertFalse(dest[2]);
        Assertions.assertTrue(dest[3]);
        Assertions.assertFalse(dest[4]);
    }

    @Test
    public void testCopyBooleanArraySameArray() {
        boolean[] arr = { true, false, true, false, true };
        N.copy(arr, 0, arr, 2, 3);
        Assertions.assertTrue(arr[0]);
        Assertions.assertFalse(arr[1]);
        Assertions.assertTrue(arr[2]);
        Assertions.assertFalse(arr[3]);
        Assertions.assertTrue(arr[4]);
    }

    @Test
    public void testCopyBooleanArrayEmpty() {
        boolean[] src = {};
        boolean[] dest = new boolean[5];
        N.copy(src, 0, dest, 0, 0);
        // Should not throw exception
    }

    // Tests for copy methods - char array
    @Test
    public void testCopyCharArray() {
        char[] src = { 'a', 'b', 'c', 'd', 'e' };
        char[] dest = new char[5];
        N.copy(src, 1, dest, 0, 3);
        Assertions.assertEquals('b', dest[0]);
        Assertions.assertEquals('c', dest[1]);
        Assertions.assertEquals('d', dest[2]);
        Assertions.assertEquals('\0', dest[3]);
        Assertions.assertEquals('\0', dest[4]);
    }

    // Tests for copy methods - byte array
    @Test
    public void testCopyByteArray() {
        byte[] src = { 1, 2, 3, 4, 5 };
        byte[] dest = new byte[5];
        N.copy(src, 2, dest, 1, 3);
        Assertions.assertEquals(0, dest[0]);
        Assertions.assertEquals(3, dest[1]);
        Assertions.assertEquals(4, dest[2]);
        Assertions.assertEquals(5, dest[3]);
        Assertions.assertEquals(0, dest[4]);
    }

    // Tests for copy methods - short array
    @Test
    public void testCopyShortArray() {
        short[] src = { 10, 20, 30, 40, 50 };
        short[] dest = new short[5];
        N.copy(src, 0, dest, 2, 2);
        Assertions.assertEquals(0, dest[0]);
        Assertions.assertEquals(0, dest[1]);
        Assertions.assertEquals(10, dest[2]);
        Assertions.assertEquals(20, dest[3]);
        Assertions.assertEquals(0, dest[4]);
    }

    // Tests for copy methods - int array
    @Test
    public void testCopyIntArray() {
        int[] src = { 100, 200, 300, 400, 500 };
        int[] dest = new int[5];
        N.copy(src, 1, dest, 1, 3);
        Assertions.assertEquals(0, dest[0]);
        Assertions.assertEquals(200, dest[1]);
        Assertions.assertEquals(300, dest[2]);
        Assertions.assertEquals(400, dest[3]);
        Assertions.assertEquals(0, dest[4]);
    }

    // Tests for copy methods - long array
    @Test
    public void testCopyLongArray() {
        long[] src = { 1000L, 2000L, 3000L, 4000L, 5000L };
        long[] dest = new long[5];
        N.copy(src, 2, dest, 0, 3);
        Assertions.assertEquals(3000L, dest[0]);
        Assertions.assertEquals(4000L, dest[1]);
        Assertions.assertEquals(5000L, dest[2]);
        Assertions.assertEquals(0L, dest[3]);
        Assertions.assertEquals(0L, dest[4]);
    }

    // Tests for copy methods - float array
    @Test
    public void testCopyFloatArray() {
        float[] src = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] dest = new float[5];
        N.copy(src, 0, dest, 0, 5);
        Assertions.assertArrayEquals(src, dest);
    }

    // Tests for copy methods - double array
    @Test
    public void testCopyDoubleArray() {
        double[] src = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] dest = new double[5];
        N.copy(src, 3, dest, 3, 2);
        Assertions.assertEquals(0.0, dest[0]);
        Assertions.assertEquals(0.0, dest[1]);
        Assertions.assertEquals(0.0, dest[2]);
        Assertions.assertEquals(4.0, dest[3]);
        Assertions.assertEquals(5.0, dest[4]);
    }

    // Tests for copy methods - Object array
    @Test
    public void testCopyObjectArray() {
        String[] src = { "a", "b", "c", "d", "e" };
        String[] dest = new String[5];
        N.copy(src, 1, dest, 2, 2);
        Assertions.assertNull(dest[0]);
        Assertions.assertNull(dest[1]);
        Assertions.assertEquals("b", dest[2]);
        Assertions.assertEquals("c", dest[3]);
        Assertions.assertNull(dest[4]);
    }

    // Tests for copy methods - generic Object
    @Test
    public void testCopyGenericObject() {
        int[] src = { 1, 2, 3, 4, 5 };
        int[] dest = new int[5];
        N.copy((Object) src, 0, (Object) dest, 1, 4);
        Assertions.assertEquals(0, dest[0]);
        Assertions.assertEquals(1, dest[1]);
        Assertions.assertEquals(2, dest[2]);
        Assertions.assertEquals(3, dest[3]);
        Assertions.assertEquals(4, dest[4]);
    }

    // Tests for copyOf methods - boolean array
    @Test
    public void testCopyOfBooleanArray() {
        boolean[] original = { true, false, true };
        boolean[] copy = N.copyOf(original, 5);
        Assertions.assertEquals(5, copy.length);
        Assertions.assertTrue(copy[0]);
        Assertions.assertFalse(copy[1]);
        Assertions.assertTrue(copy[2]);
        Assertions.assertFalse(copy[3]);
        Assertions.assertFalse(copy[4]);
    }

    @Test
    public void testCopyOfBooleanArrayTruncate() {
        boolean[] original = { true, false, true, false, true };
        boolean[] copy = N.copyOf(original, 3);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertTrue(copy[0]);
        Assertions.assertFalse(copy[1]);
        Assertions.assertTrue(copy[2]);
    }

    @Test
    public void testCopyOfBooleanArraySameLength() {
        boolean[] original = { true, false, true };
        boolean[] copy = N.copyOf(original, 3);
        Assertions.assertArrayEquals(original, copy);
        Assertions.assertNotSame(original, copy);
    }

    // Tests for copyOf methods - char array
    @Test
    public void testCopyOfCharArray() {
        char[] original = { 'a', 'b', 'c' };
        char[] copy = N.copyOf(original, 5);
        Assertions.assertEquals(5, copy.length);
        Assertions.assertEquals('a', copy[0]);
        Assertions.assertEquals('b', copy[1]);
        Assertions.assertEquals('c', copy[2]);
        Assertions.assertEquals('\0', copy[3]);
        Assertions.assertEquals('\0', copy[4]);
    }

    // Tests for copyOf methods - byte array
    @Test
    public void testCopyOfByteArray() {
        byte[] original = { 1, 2, 3 };
        byte[] copy = N.copyOf(original, 2);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(1, copy[0]);
        Assertions.assertEquals(2, copy[1]);
    }

    // Tests for copyOf methods - short array
    @Test
    public void testCopyOfShortArray() {
        short[] original = { 10, 20, 30 };
        short[] copy = N.copyOf(original, 4);
        Assertions.assertEquals(4, copy.length);
        Assertions.assertEquals(10, copy[0]);
        Assertions.assertEquals(20, copy[1]);
        Assertions.assertEquals(30, copy[2]);
        Assertions.assertEquals(0, copy[3]);
    }

    // Tests for copyOf methods - int array
    @Test
    public void testCopyOfIntArray() {
        int[] original = { 100, 200, 300 };
        int[] copy = N.copyOf(original, 3);
        Assertions.assertArrayEquals(original, copy);
        Assertions.assertNotSame(original, copy);
    }

    // Tests for copyOf methods - long array
    @Test
    public void testCopyOfLongArray() {
        long[] original = { 1000L, 2000L, 3000L };
        long[] copy = N.copyOf(original, 5);
        Assertions.assertEquals(5, copy.length);
        Assertions.assertEquals(1000L, copy[0]);
        Assertions.assertEquals(2000L, copy[1]);
        Assertions.assertEquals(3000L, copy[2]);
        Assertions.assertEquals(0L, copy[3]);
        Assertions.assertEquals(0L, copy[4]);
    }

    // Tests for copyOf methods - float array
    @Test
    public void testCopyOfFloatArray() {
        float[] original = { 1.0f, 2.0f, 3.0f };
        float[] copy = N.copyOf(original, 2);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(1.0f, copy[0]);
        Assertions.assertEquals(2.0f, copy[1]);
    }

    // Tests for copyOf methods - double array
    @Test
    public void testCopyOfDoubleArray() {
        double[] original = { 1.0, 2.0, 3.0 };
        double[] copy = N.copyOf(original, 4);
        Assertions.assertEquals(4, copy.length);
        Assertions.assertEquals(1.0, copy[0]);
        Assertions.assertEquals(2.0, copy[1]);
        Assertions.assertEquals(3.0, copy[2]);
        Assertions.assertEquals(0.0, copy[3]);
    }

    // Tests for copyOf methods - Object array
    @Test
    public void testCopyOfObjectArray() {
        String[] original = { "a", "b", "c" };
        String[] copy = N.copyOf(original, 5);
        Assertions.assertEquals(5, copy.length);
        Assertions.assertEquals("a", copy[0]);
        Assertions.assertEquals("b", copy[1]);
        Assertions.assertEquals("c", copy[2]);
        Assertions.assertNull(copy[3]);
        Assertions.assertNull(copy[4]);
    }

    @Test
    public void testCopyOfObjectArrayWithType() {
        Number[] original = { 1, 2, 3 };
        Integer[] copy = N.copyOf(original, 4, Integer[].class);
        Assertions.assertEquals(4, copy.length);
        Assertions.assertEquals(1, copy[0]);
        Assertions.assertEquals(2, copy[1]);
        Assertions.assertEquals(3, copy[2]);
        Assertions.assertNull(copy[3]);
    }

    // Tests for copyOfRange methods - boolean array
    @Test
    public void testCopyOfRangeBooleanArray() {
        boolean[] original = { true, false, true, false, true };
        boolean[] copy = N.copyOfRange(original, 1, 4);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertFalse(copy[0]);
        Assertions.assertTrue(copy[1]);
        Assertions.assertFalse(copy[2]);
    }

    @Test
    public void testCopyOfRangeBooleanArrayFullRange() {
        boolean[] original = { true, false, true };
        boolean[] copy = N.copyOfRange(original, 0, 3);
        Assertions.assertArrayEquals(original, copy);
        Assertions.assertNotSame(original, copy);
    }

    @Test
    public void testCopyOfRangeBooleanArrayWithStep() {
        boolean[] original = { true, false, true, false, true, false };
        boolean[] copy = N.copyOfRange(original, 0, 6, 2);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertTrue(copy[0]);
        Assertions.assertTrue(copy[1]);
        Assertions.assertTrue(copy[2]);
    }

    @Test
    public void testCopyOfRangeBooleanArrayWithNegativeStep() {
        boolean[] original = { true, false, true, false, true };
        boolean[] copy = N.copyOfRange(original, 4, 1, -1);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertTrue(copy[0]);
        Assertions.assertFalse(copy[1]);
        Assertions.assertTrue(copy[2]);
    }

    @Test
    public void testCopyOfRangeBooleanArrayWithNegativeStepToBeginning() {
        boolean[] original = { true, false, true, false, true };
        boolean[] copy = N.copyOfRange(original, 4, -1, -1);
        Assertions.assertEquals(5, copy.length);
        Assertions.assertTrue(copy[0]);
        Assertions.assertFalse(copy[1]);
        Assertions.assertTrue(copy[2]);
        Assertions.assertFalse(copy[3]);
        Assertions.assertTrue(copy[4]);
    }

    // Tests for copyOfRange methods - char array
    @Test
    public void testCopyOfRangeCharArray() {
        char[] original = { 'a', 'b', 'c', 'd', 'e' };
        char[] copy = N.copyOfRange(original, 1, 4);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals('b', copy[0]);
        Assertions.assertEquals('c', copy[1]);
        Assertions.assertEquals('d', copy[2]);
    }

    @Test
    public void testCopyOfRangeCharArrayWithStep() {
        char[] original = { 'a', 'b', 'c', 'd', 'e', 'f' };
        char[] copy = N.copyOfRange(original, 1, 6, 2);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals('b', copy[0]);
        Assertions.assertEquals('d', copy[1]);
        Assertions.assertEquals('f', copy[2]);
    }

    // Tests for copyOfRange methods - byte array
    @Test
    public void testCopyOfRangeByteArray() {
        byte[] original = { 1, 2, 3, 4, 5 };
        byte[] copy = N.copyOfRange(original, 2, 5);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals(3, copy[0]);
        Assertions.assertEquals(4, copy[1]);
        Assertions.assertEquals(5, copy[2]);
    }

    @Test
    public void testCopyOfRangeByteArrayWithStep() {
        byte[] original = { 1, 2, 3, 4, 5, 6 };
        byte[] copy = N.copyOfRange(original, 0, 5, 2);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals(1, copy[0]);
        Assertions.assertEquals(3, copy[1]);
        Assertions.assertEquals(5, copy[2]);
    }

    // Tests for copyOfRange methods - short array
    @Test
    public void testCopyOfRangeShortArray() {
        short[] original = { 10, 20, 30, 40, 50 };
        short[] copy = N.copyOfRange(original, 0, 3);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals(10, copy[0]);
        Assertions.assertEquals(20, copy[1]);
        Assertions.assertEquals(30, copy[2]);
    }

    @Test
    public void testCopyOfRangeShortArrayWithStep() {
        short[] original = { 10, 20, 30, 40, 50, 60 };
        short[] copy = N.copyOfRange(original, 1, 5, 2);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(20, copy[0]);
        Assertions.assertEquals(40, copy[1]);
    }

    // Tests for copyOfRange methods - int array
    @Test
    public void testCopyOfRangeIntArray() {
        int[] original = { 100, 200, 300, 400, 500 };
        int[] copy = N.copyOfRange(original, 1, 3);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(200, copy[0]);
        Assertions.assertEquals(300, copy[1]);
    }

    @Test
    public void testCopyOfRangeIntArrayWithStep() {
        int[] original = { 100, 200, 300, 400, 500, 600 };
        int[] copy = N.copyOfRange(original, 0, 6, 3);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(100, copy[0]);
        Assertions.assertEquals(400, copy[1]);
    }

    // Tests for copyOfRange methods - long array
    @Test
    public void testCopyOfRangeLongArray() {
        long[] original = { 1000L, 2000L, 3000L, 4000L, 5000L };
        long[] copy = N.copyOfRange(original, 2, 4);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(3000L, copy[0]);
        Assertions.assertEquals(4000L, copy[1]);
    }

    @Test
    public void testCopyOfRangeLongArrayWithStep() {
        long[] original = { 1000L, 2000L, 3000L, 4000L, 5000L };
        long[] copy = N.copyOfRange(original, 4, 0, -2);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(5000L, copy[0]);
        Assertions.assertEquals(3000L, copy[1]);
    }

    // Tests for copyOfRange methods - float array
    @Test
    public void testCopyOfRangeFloatArray() {
        float[] original = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] copy = N.copyOfRange(original, 3, 5);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(4.0f, copy[0]);
        Assertions.assertEquals(5.0f, copy[1]);
    }

    @Test
    public void testCopyOfRangeFloatArrayWithStep() {
        float[] original = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f };
        float[] copy = N.copyOfRange(original, 0, 6, 2);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals(1.0f, copy[0]);
        Assertions.assertEquals(3.0f, copy[1]);
        Assertions.assertEquals(5.0f, copy[2]);
    }

    // Tests for copyOfRange methods - double array
    @Test
    public void testCopyOfRangeDoubleArray() {
        double[] original = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] copy = N.copyOfRange(original, 0, 2);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(1.0, copy[0]);
        Assertions.assertEquals(2.0, copy[1]);
    }

    @Test
    public void testCopyOfRangeDoubleArrayWithStep() {
        double[] original = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] copy = N.copyOfRange(original, 1, 5, 2);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(2.0, copy[0]);
        Assertions.assertEquals(4.0, copy[1]);
    }

    // Tests for copyOfRange methods - Object array
    @Test
    public void testCopyOfRangeObjectArray() {
        String[] original = { "a", "b", "c", "d", "e" };
        String[] copy = N.copyOfRange(original, 1, 4);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals("b", copy[0]);
        Assertions.assertEquals("c", copy[1]);
        Assertions.assertEquals("d", copy[2]);
    }

    @Test
    public void testCopyOfRangeObjectArrayWithStep() {
        String[] original = { "a", "b", "c", "d", "e", "f" };
        String[] copy = N.copyOfRange(original, 0, 6, 2);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals("a", copy[0]);
        Assertions.assertEquals("c", copy[1]);
        Assertions.assertEquals("e", copy[2]);
    }

    @Test
    public void testCopyOfRangeObjectArrayWithType() {
        Number[] original = { 1, 2, 3, 4, 5 };
        Integer[] copy = N.copyOfRange(original, 1, 4, Integer[].class);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals(2, copy[0]);
        Assertions.assertEquals(3, copy[1]);
        Assertions.assertEquals(4, copy[2]);
    }

    @Test
    public void testCopyOfRangeObjectArrayWithTypeAndStep() {
        Number[] original = { 1, 2, 3, 4, 5, 6 };
        Integer[] copy = N.copyOfRange(original, 0, 6, 3, Integer[].class);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(1, copy[0]);
        Assertions.assertEquals(4, copy[1]);
    }

    // Tests for copyOfRange methods - List
    @Test
    public void testCopyOfRangeList() {
        List<String> original = Arrays.asList("a", "b", "c", "d", "e");
        List<String> copy = N.copyOfRange(original, 1, 4);
        Assertions.assertEquals(3, copy.size());
        Assertions.assertEquals("b", copy.get(0));
        Assertions.assertEquals("c", copy.get(1));
        Assertions.assertEquals("d", copy.get(2));
    }

    @Test
    public void testCopyOfRangeListWithStep() {
        List<String> original = Arrays.asList("a", "b", "c", "d", "e", "f");
        List<String> copy = N.copyOfRange(original, 0, 5, 2);
        Assertions.assertEquals(3, copy.size());
        Assertions.assertEquals("a", copy.get(0));
        Assertions.assertEquals("c", copy.get(1));
        Assertions.assertEquals("e", copy.get(2));
    }

    // Tests for copyOfRange methods - String
    @Test
    public void testCopyOfRangeString() {
        String original = "abcde";
        String copy = N.copyOfRange(original, 1, 4);
        Assertions.assertEquals("bcd", copy);
    }

    @Test
    public void testCopyOfRangeStringFullRange() {
        String original = "abcde";
        String copy = N.copyOfRange(original, 0, 5);
        Assertions.assertEquals(original, copy);
    }

    @Test
    public void testCopyOfRangeStringWithStep() {
        String original = "abcdef";
        String copy = N.copyOfRange(original, 0, 6, 2);
        Assertions.assertEquals("ace", copy);
    }

    // Tests for clone methods - boolean array
    @Test
    public void testCloneBooleanArray() {
        boolean[] original = { true, false, true };
        boolean[] cloned = N.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    @Test
    public void testCloneBooleanArrayNull() {
        boolean[] original = null;
        boolean[] cloned = N.clone(original);
        Assertions.assertNull(cloned);
    }

    // Tests for clone methods - char array
    @Test
    public void testCloneCharArray() {
        char[] original = { 'a', 'b', 'c' };
        char[] cloned = N.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    // Tests for clone methods - byte array
    @Test
    public void testCloneByteArray() {
        byte[] original = { 1, 2, 3 };
        byte[] cloned = N.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    // Tests for clone methods - short array
    @Test
    public void testCloneShortArray() {
        short[] original = { 10, 20, 30 };
        short[] cloned = N.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    // Tests for clone methods - int array
    @Test
    public void testCloneIntArray() {
        int[] original = { 100, 200, 300 };
        int[] cloned = N.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    // Tests for clone methods - long array
    @Test
    public void testCloneLongArray() {
        long[] original = { 1000L, 2000L, 3000L };
        long[] cloned = N.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    // Tests for clone methods - float array
    @Test
    public void testCloneFloatArray() {
        float[] original = { 1.0f, 2.0f, 3.0f };
        float[] cloned = N.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    // Tests for clone methods - double array
    @Test
    public void testCloneDoubleArray() {
        double[] original = { 1.0, 2.0, 3.0 };
        double[] cloned = N.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    // Tests for clone methods - Object array
    @Test
    public void testCloneObjectArray() {
        String[] original = { "a", "b", "c" };
        String[] cloned = N.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    // Tests for clone methods - 2D boolean array

    @Test
    public void testClone2DBoolean() {
        assertNull(N.clone((boolean[][]) null));

        final boolean[][] a = new boolean[][] { { true, false }, { false, true } };
        final boolean[][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = false;
        assertTrue(a[0][0]);

        final boolean[][] b = new boolean[][] { { true, false }, null };
        final boolean[][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final boolean[][] c = new boolean[0][0];
        final boolean[][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DChar() {
        assertNull(N.clone((char[][]) null));

        final char[][] a = new char[][] { { 'a', 'b' }, { 'c', 'd' } };
        final char[][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 'z';
        assertEquals('a', a[0][0]);

        final char[][] b = new char[][] { { 'a', 'b' }, null };
        final char[][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final char[][] c = new char[0][0];
        final char[][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DByte() {
        assertNull(N.clone((byte[][]) null));

        final byte[][] a = new byte[][] { { 1, 2 }, { 3, 4 } };
        final byte[][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 9;
        assertEquals(1, a[0][0]);

        final byte[][] b = new byte[][] { { 1, 2 }, null };
        final byte[][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final byte[][] c = new byte[0][0];
        final byte[][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DShort() {
        assertNull(N.clone((short[][]) null));

        final short[][] a = new short[][] { { 1, 2 }, { 3, 4 } };
        final short[][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 9;
        assertEquals(1, a[0][0]);

        final short[][] b = new short[][] { { 1, 2 }, null };
        final short[][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final short[][] c = new short[0][0];
        final short[][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DInt() {
        assertNull(N.clone((int[][]) null));

        final int[][] a = new int[][] { { 1, 2 }, { 3, 4 } };
        final int[][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 9;
        assertEquals(1, a[0][0]);

        final int[][] b = new int[][] { { 1, 2 }, null };
        final int[][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final int[][] c = new int[0][0];
        final int[][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DLong() {
        assertNull(N.clone((long[][]) null));

        final long[][] a = new long[][] { { 1L, 2L }, { 3L, 4L } };
        final long[][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 9L;
        assertEquals(1L, a[0][0]);

        final long[][] b = new long[][] { { 1L, 2L }, null };
        final long[][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final long[][] c = new long[0][0];
        final long[][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DFloat() {
        assertNull(N.clone((float[][]) null));

        final float[][] a = new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
        final float[][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 9.0f;
        assertEquals(1.0f, a[0][0], 0.000001);

        final float[][] b = new float[][] { { 1.0f, 2.0f }, null };
        final float[][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final float[][] c = new float[0][0];
        final float[][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DDouble() {
        assertNull(N.clone((double[][]) null));

        final double[][] a = new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } };
        final double[][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 9.0;
        assertEquals(1.0, a[0][0], 0.000001);

        final double[][] b = new double[][] { { 1.0, 2.0 }, null };
        final double[][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final double[][] c = new double[0][0];
        final double[][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DGeneric() {
        assertNull(N.clone((String[][]) null));

        final String[][] a = new String[][] { { "a", "b" }, { "c", "d" } };
        final String[][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = "z";
        assertEquals("a", a[0][0]);

        final String[][] b = new String[][] { { "a", "b" }, null };
        final String[][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final String[][] c = new String[0][0];
        final String[][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone3DBoolean() {
        assertNull(N.clone((boolean[][][]) null));

        final boolean[][][] a = new boolean[][][] { { { true, false } }, { { false, true } } };
        final boolean[][][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = false;
        assertTrue(a[0][0][0]);

        final boolean[][][] b = new boolean[][][] { { { true, false } }, null };
        final boolean[][][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final boolean[][][] c = new boolean[0][0][0];
        final boolean[][][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DChar() {
        assertNull(N.clone((char[][][]) null));

        final char[][][] a = new char[][][] { { { 'a', 'b' } }, { { 'c', 'd' } } };
        final char[][][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 'z';
        assertEquals('a', a[0][0][0]);

        final char[][][] b = new char[][][] { { { 'a', 'b' } }, null };
        final char[][][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final char[][][] c = new char[0][0][0];
        final char[][][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DByte() {
        assertNull(N.clone((byte[][][]) null));

        final byte[][][] a = new byte[][][] { { { 1, 2 } }, { { 3, 4 } } };
        final byte[][][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 9;
        assertEquals(1, a[0][0][0]);

        final byte[][][] b = new byte[][][] { { { 1, 2 } }, null };
        final byte[][][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final byte[][][] c = new byte[0][0][0];
        final byte[][][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DShort() {
        assertNull(N.clone((short[][][]) null));

        final short[][][] a = new short[][][] { { { 1, 2 } }, { { 3, 4 } } };
        final short[][][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 9;
        assertEquals(1, a[0][0][0]);

        final short[][][] b = new short[][][] { { { 1, 2 } }, null };
        final short[][][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final short[][][] c = new short[0][0][0];
        final short[][][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DInt() {
        assertNull(N.clone((int[][][]) null));

        final int[][][] a = new int[][][] { { { 1, 2 } }, { { 3, 4 } } };
        final int[][][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 9;
        assertEquals(1, a[0][0][0]);

        final int[][][] b = new int[][][] { { { 1, 2 } }, null };
        final int[][][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final int[][][] c = new int[0][0][0];
        final int[][][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DLong() {
        assertNull(N.clone((long[][][]) null));

        final long[][][] a = new long[][][] { { { 1L, 2L } }, { { 3L, 4L } } };
        final long[][][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 9L;
        assertEquals(1L, a[0][0][0]);

        final long[][][] b = new long[][][] { { { 1L, 2L } }, null };
        final long[][][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final long[][][] c = new long[0][0][0];
        final long[][][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DFloat() {
        assertNull(N.clone((float[][][]) null));

        final float[][][] a = new float[][][] { { { 1.0f, 2.0f } }, { { 3.0f, 4.0f } } };
        final float[][][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 9.0f;
        assertEquals(1.0f, a[0][0][0], 0.000001);

        final float[][][] b = new float[][][] { { { 1.0f, 2.0f } }, null };
        final float[][][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final float[][][] c = new float[0][0][0];
        final float[][][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DDouble() {
        assertNull(N.clone((double[][][]) null));

        final double[][][] a = new double[][][] { { { 1.0, 2.0 } }, { { 3.0, 4.0 } } };
        final double[][][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 9.0;
        assertEquals(1.0, a[0][0][0], 0.000001);

        final double[][][] b = new double[][][] { { { 1.0, 2.0 } }, null };
        final double[][][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final double[][][] c = new double[0][0][0];
        final double[][][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DGeneric() {
        assertNull(N.clone((String[][][]) null));

        final String[][][] a = new String[][][] { { { "a", "b" } }, { { "c", "d" } } };
        final String[][][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = "z";
        assertEquals("a", a[0][0][0]);

        final String[][][] b = new String[][][] { { { "a", "b" } }, null };
        final String[][][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final String[][][] c = new String[0][0][0];
        final String[][][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Data
    public static class TestBean {

        private String name;
        private int age;
        private boolean active;
    }

}