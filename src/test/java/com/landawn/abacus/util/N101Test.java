package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class N101Test extends TestBase {

    // Test data
    private boolean[] booleanArray;
    private char[] charArray;
    private byte[] byteArray;
    private short[] shortArray;
    private int[] intArray;
    private long[] longArray;
    private float[] floatArray;
    private double[] doubleArray;
    private String[] stringArray;
    private Integer[] integerArray;

    @BeforeEach
    public void setUp() {
        booleanArray = new boolean[] { true, false, true, false };
        charArray = new char[] { 'a', 'b', 'c', 'd' };
        byteArray = new byte[] { 1, 2, 3, 4 };
        shortArray = new short[] { 1, 2, 3, 4 };
        intArray = new int[] { 1, 2, 3, 4 };
        longArray = new long[] { 1L, 2L, 3L, 4L };
        floatArray = new float[] { 1.0f, 2.0f, 3.0f, 4.0f };
        doubleArray = new double[] { 1.0, 2.0, 3.0, 4.0 };
        stringArray = new String[] { "a", "b", "c", "d" };
        integerArray = new Integer[] { 1, 2, 3, 4 };
    }

    // Tests for replaceIf methods

    @Test
    public void testReplaceIfBooleanArray() {
        boolean[] arr = { true, false, true, false };
        int count = N.replaceIf(arr, val -> val == true, false);
        assertEquals(2, count);
        assertArrayEquals(new boolean[] { false, false, false, false }, arr);

        // Test with empty array
        assertEquals(0, N.replaceIf(new boolean[0], val -> val == true, false));
        assertEquals(0, N.replaceIf((boolean[]) null, val -> val == true, false));
    }

    @Test
    public void testReplaceIfCharArray() {
        char[] arr = { 'a', 'b', 'a', 'c' };
        int count = N.replaceIf(arr, val -> val == 'a', 'x');
        assertEquals(2, count);
        assertArrayEquals(new char[] { 'x', 'b', 'x', 'c' }, arr);
    }

    @Test
    public void testReplaceIfByteArray() {
        byte[] arr = { 1, 2, 1, 3 };
        int count = N.replaceIf(arr, val -> val == 1, (byte) 9);
        assertEquals(2, count);
        assertArrayEquals(new byte[] { 9, 2, 9, 3 }, arr);
    }

    @Test
    public void testReplaceIfShortArray() {
        short[] arr = { 1, 2, 1, 3 };
        int count = N.replaceIf(arr, val -> val == 1, (short) 9);
        assertEquals(2, count);
        assertArrayEquals(new short[] { 9, 2, 9, 3 }, arr);
    }

    @Test
    public void testReplaceIfIntArray() {
        int[] arr = { 1, 2, 1, 3 };
        int count = N.replaceIf(arr, val -> val == 1, 9);
        assertEquals(2, count);
        assertArrayEquals(new int[] { 9, 2, 9, 3 }, arr);
    }

    @Test
    public void testReplaceIfLongArray() {
        long[] arr = { 1L, 2L, 1L, 3L };
        int count = N.replaceIf(arr, val -> val == 1L, 9L);
        assertEquals(2, count);
        assertArrayEquals(new long[] { 9L, 2L, 9L, 3L }, arr);
    }

    @Test
    public void testReplaceIfFloatArray() {
        float[] arr = { 1.0f, 2.0f, 1.0f, 3.0f };
        int count = N.replaceIf(arr, val -> val == 1.0f, 9.0f);
        assertEquals(2, count);
        assertArrayEquals(new float[] { 9.0f, 2.0f, 9.0f, 3.0f }, arr, 0.001f);
    }

    @Test
    public void testReplaceIfDoubleArray() {
        double[] arr = { 1.0, 2.0, 1.0, 3.0 };
        int count = N.replaceIf(arr, val -> val == 1.0, 9.0);
        assertEquals(2, count);
        assertArrayEquals(new double[] { 9.0, 2.0, 9.0, 3.0 }, arr, 0.001);
    }

    @Test
    public void testReplaceIfObjectArray() {
        String[] arr = { "a", "b", "a", "c" };
        int count = N.replaceIf(arr, val -> "a".equals(val), "x");
        assertEquals(2, count);
        assertArrayEquals(new String[] { "x", "b", "x", "c" }, arr);
    }

    @Test
    public void testReplaceIfList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "a", "c"));
        int count = N.replaceIf(list, val -> "a".equals(val), "x");
        assertEquals(2, count);
        assertEquals(Arrays.asList("x", "b", "x", "c"), list);
    }

    // Tests for replaceAll methods

    @Test
    public void testReplaceAllBooleanArray() {
        boolean[] arr = { true, false, true, false };
        int count = N.replaceAll(arr, true, false);
        assertEquals(2, count);
        assertArrayEquals(new boolean[] { false, false, false, false }, arr);

        // Test with empty array
        assertEquals(0, N.replaceAll(new boolean[0], true, false));
        assertEquals(0, N.replaceAll((boolean[]) null, true, false));
    }

    @Test
    public void testReplaceAllCharArray() {
        char[] arr = { 'a', 'b', 'a', 'c' };
        int count = N.replaceAll(arr, 'a', 'x');
        assertEquals(2, count);
        assertArrayEquals(new char[] { 'x', 'b', 'x', 'c' }, arr);
    }

    @Test
    public void testReplaceAllByteArray() {
        byte[] arr = { 1, 2, 1, 3 };
        int count = N.replaceAll(arr, (byte) 1, (byte) 9);
        assertEquals(2, count);
        assertArrayEquals(new byte[] { 9, 2, 9, 3 }, arr);
    }

    @Test
    public void testReplaceAllShortArray() {
        short[] arr = { 1, 2, 1, 3 };
        int count = N.replaceAll(arr, (short) 1, (short) 9);
        assertEquals(2, count);
        assertArrayEquals(new short[] { 9, 2, 9, 3 }, arr);
    }

    @Test
    public void testReplaceAllIntArray() {
        int[] arr = { 1, 2, 1, 3 };
        int count = N.replaceAll(arr, 1, 9);
        assertEquals(2, count);
        assertArrayEquals(new int[] { 9, 2, 9, 3 }, arr);
    }

    @Test
    public void testReplaceAllLongArray() {
        long[] arr = { 1L, 2L, 1L, 3L };
        int count = N.replaceAll(arr, 1L, 9L);
        assertEquals(2, count);
        assertArrayEquals(new long[] { 9L, 2L, 9L, 3L }, arr);
    }

    @Test
    public void testReplaceAllFloatArray() {
        float[] arr = { 1.0f, 2.0f, 1.0f, 3.0f };
        int count = N.replaceAll(arr, 1.0f, 9.0f);
        assertEquals(2, count);
        assertArrayEquals(new float[] { 9.0f, 2.0f, 9.0f, 3.0f }, arr, 0.001f);
    }

    @Test
    public void testReplaceAllDoubleArray() {
        double[] arr = { 1.0, 2.0, 1.0, 3.0 };
        int count = N.replaceAll(arr, 1.0, 9.0);
        assertEquals(2, count);
        assertArrayEquals(new double[] { 9.0, 2.0, 9.0, 3.0 }, arr, 0.001);
    }

    @Test
    public void testReplaceAllObjectArray() {
        String[] arr = { "a", "b", "a", "c" };
        int count = N.replaceAll(arr, "a", "x");
        assertEquals(2, count);
        assertArrayEquals(new String[] { "x", "b", "x", "c" }, arr);

        // Test with null values
        String[] arrWithNull = { "a", null, "a", null };
        count = N.replaceAll(arrWithNull, null, "x");
        assertEquals(2, count);
        assertArrayEquals(new String[] { "a", "x", "a", "x" }, arrWithNull);
    }

    @Test
    public void testReplaceAllList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "a", "c"));
        int count = N.replaceAll(list, "a", "x");
        assertEquals(2, count);
        assertEquals(Arrays.asList("x", "b", "x", "c"), list);
    }

    // Tests for replaceAll with operators

    @Test
    public void testReplaceAllBooleanArrayWithOperator() {
        boolean[] arr = { true, false, true, false };
        N.replaceAll(arr, val -> !val);
        assertArrayEquals(new boolean[] { false, true, false, true }, arr);
    }

    @Test
    public void testReplaceAllCharArrayWithOperator() {
        char[] arr = { 'a', 'b', 'c', 'd' };
        N.replaceAll(arr, val -> (char) (val + 1));
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, arr);
    }

    @Test
    public void testReplaceAllByteArrayWithOperator() {
        byte[] arr = { 1, 2, 3, 4 };
        N.replaceAll(arr, val -> (byte) (val * 2));
        assertArrayEquals(new byte[] { 2, 4, 6, 8 }, arr);
    }

    @Test
    public void testReplaceAllShortArrayWithOperator() {
        short[] arr = { 1, 2, 3, 4 };
        N.replaceAll(arr, val -> (short) (val * 2));
        assertArrayEquals(new short[] { 2, 4, 6, 8 }, arr);
    }

    @Test
    public void testReplaceAllIntArrayWithOperator() {
        int[] arr = { 1, 2, 3, 4 };
        N.replaceAll(arr, val -> val * 2);
        assertArrayEquals(new int[] { 2, 4, 6, 8 }, arr);
    }

    @Test
    public void testReplaceAllLongArrayWithOperator() {
        long[] arr = { 1L, 2L, 3L, 4L };
        N.replaceAll(arr, val -> val * 2L);
        assertArrayEquals(new long[] { 2L, 4L, 6L, 8L }, arr);
    }

    @Test
    public void testReplaceAllFloatArrayWithOperator() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f };
        N.replaceAll(arr, val -> val * 2.0f);
        assertArrayEquals(new float[] { 2.0f, 4.0f, 6.0f, 8.0f }, arr, 0.001f);
    }

    @Test
    public void testReplaceAllDoubleArrayWithOperator() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0 };
        N.replaceAll(arr, val -> val * 2.0);
        assertArrayEquals(new double[] { 2.0, 4.0, 6.0, 8.0 }, arr, 0.001);
    }

    @Test
    public void testReplaceAllObjectArrayWithOperator() {
        String[] arr = { "a", "b", "c", "d" };
        N.replaceAll(arr, String::toUpperCase);
        assertArrayEquals(new String[] { "A", "B", "C", "D" }, arr);
    }

    @Test
    public void testReplaceAllListWithOperator() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        N.replaceAll(list, String::toUpperCase);
        assertEquals(Arrays.asList("A", "B", "C", "D"), list);
    }

    // Tests for setAll methods

    @Test
    public void testSetAllBooleanArray() {
        boolean[] arr = new boolean[4];
        N.setAll(arr, i -> i % 2 == 0);
        assertArrayEquals(new boolean[] { true, false, true, false }, arr);
    }

    @Test
    public void testSetAllCharArray() {
        char[] arr = new char[4];
        N.setAll(arr, i -> (char) ('a' + i));
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, arr);
    }

    @Test
    public void testSetAllByteArray() {
        byte[] arr = new byte[4];
        N.setAll(arr, i -> (byte) (i + 1));
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, arr);
    }

    @Test
    public void testSetAllShortArray() {
        short[] arr = new short[4];
        N.setAll(arr, i -> (short) (i + 1));
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, arr);
    }

    @Test
    public void testSetAllIntArray() {
        int[] arr = new int[4];
        N.setAll(arr, i -> i + 1);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, arr);
    }

    @Test
    public void testSetAllLongArray() {
        long[] arr = new long[4];
        N.setAll(arr, i -> (long) (i + 1));
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, arr);
    }

    @Test
    public void testSetAllFloatArray() {
        float[] arr = new float[4];
        N.setAll(arr, i -> (float) (i + 1));
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, arr, 0.001f);
    }

    @Test
    public void testSetAllDoubleArray() {
        double[] arr = new double[4];
        N.setAll(arr, i -> (double) (i + 1));
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, arr, 0.001);
    }

    @Test
    public void testSetAllObjectArray() {
        String[] arr = new String[4];
        N.setAll(arr, i -> String.valueOf((char) ('a' + i)));
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, arr);
    }

    @Test
    public void testSetAllList() {
        List<String> list = new ArrayList<>(Arrays.asList("", "", "", ""));
        N.setAll(list, i -> String.valueOf((char) ('a' + i)));
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
    }

    // Tests for add methods

    @Test
    public void testAddBoolean() {
        boolean[] arr = { true, false };
        boolean[] result = N.add(arr, true);
        assertArrayEquals(new boolean[] { true, false, true }, result);

        // Test with empty array
        result = N.add(new boolean[0], true);
        assertArrayEquals(new boolean[] { true }, result);

        // Test with null array
        result = N.add((boolean[]) null, true);
        assertArrayEquals(new boolean[] { true }, result);
    }

    @Test
    public void testAddChar() {
        char[] arr = { 'a', 'b' };
        char[] result = N.add(arr, 'c');
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testAddByte() {
        byte[] arr = { 1, 2 };
        byte[] result = N.add(arr, (byte) 3);
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testAddShort() {
        short[] arr = { 1, 2 };
        short[] result = N.add(arr, (short) 3);
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testAddInt() {
        int[] arr = { 1, 2 };
        int[] result = N.add(arr, 3);
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testAddLong() {
        long[] arr = { 1L, 2L };
        long[] result = N.add(arr, 3L);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testAddFloat() {
        float[] arr = { 1.0f, 2.0f };
        float[] result = N.add(arr, 3.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, 0.001f);
    }

    @Test
    public void testAddDouble() {
        double[] arr = { 1.0, 2.0 };
        double[] result = N.add(arr, 3.0);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testAddString() {
        String[] arr = { "a", "b" };
        String[] result = N.add(arr, "c");
        assertArrayEquals(new String[] { "a", "b", "c" }, result);
    }

    @Test
    public void testAddObject() {
        Integer[] arr = { 1, 2 };
        Integer[] result = N.add(arr, 3);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, result);
    }

    // Tests for addAll methods

    @Test
    public void testAddAllBoolean() {
        boolean[] arr = { true, false };
        boolean[] result = N.addAll(arr, true, false);
        assertArrayEquals(new boolean[] { true, false, true, false }, result);

        // Test with empty arrays
        result = N.addAll(new boolean[0], true, false);
        assertArrayEquals(new boolean[] { true, false }, result);

        result = N.addAll(arr, new boolean[0]);
        assertArrayEquals(arr, result);
    }

    @Test
    public void testAddAllChar() {
        char[] arr = { 'a', 'b' };
        char[] result = N.addAll(arr, 'c', 'd');
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    @Test
    public void testAddAllByte() {
        byte[] arr = { 1, 2 };
        byte[] result = N.addAll(arr, (byte) 3, (byte) 4);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testAddAllShort() {
        short[] arr = { 1, 2 };
        short[] result = N.addAll(arr, (short) 3, (short) 4);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testAddAllInt() {
        int[] arr = { 1, 2 };
        int[] result = N.addAll(arr, 3, 4);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testAddAllLong() {
        long[] arr = { 1L, 2L };
        long[] result = N.addAll(arr, 3L, 4L);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    public void testAddAllFloat() {
        float[] arr = { 1.0f, 2.0f };
        float[] result = N.addAll(arr, 3.0f, 4.0f);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result, 0.001f);
    }

    @Test
    public void testAddAllDouble() {
        double[] arr = { 1.0, 2.0 };
        double[] result = N.addAll(arr, 3.0, 4.0);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testAddAllString() {
        String[] arr = { "a", "b" };
        String[] result = N.addAll(arr, "c", "d");
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, result);
    }

    @Test
    public void testAddAllObject() {
        Integer[] arr = { 1, 2 };
        Integer[] result = N.addAll(arr, 3, 4);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, result);
    }

    @Test
    public void testAddAllCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        boolean result = N.addAll(list, "c", "d");
        assertTrue(result);
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);

        // Test with empty elements to add
        result = N.addAll(list, new String[0]);
        assertFalse(result);
    }

    @Test
    public void testAddAllCollectionIterable() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        List<String> toAdd = Arrays.asList("c", "d");
        boolean result = N.addAll(list, toAdd);
        assertTrue(result);
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
    }

    @Test
    public void testAddAllCollectionIterator() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        List<String> toAdd = Arrays.asList("c", "d");
        boolean result = N.addAll(list, toAdd.iterator());
        assertTrue(result);
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
    }

    // Tests for insert methods

    @Test
    public void testInsertBoolean() {
        boolean[] arr = { true, false, true };
        boolean[] result = N.insert(arr, 1, false);
        assertArrayEquals(new boolean[] { true, false, false, true }, result);

        // Test at beginning
        result = N.insert(arr, 0, false);
        assertArrayEquals(new boolean[] { false, true, false, true }, result);

        // Test at end
        result = N.insert(arr, 3, false);
        assertArrayEquals(new boolean[] { true, false, true, false }, result);
    }

    @Test
    public void testInsertChar() {
        char[] arr = { 'a', 'b', 'c' };
        char[] result = N.insert(arr, 1, 'x');
        assertArrayEquals(new char[] { 'a', 'x', 'b', 'c' }, result);
    }

    @Test
    public void testInsertByte() {
        byte[] arr = { 1, 2, 3 };
        byte[] result = N.insert(arr, 1, (byte) 9);
        assertArrayEquals(new byte[] { 1, 9, 2, 3 }, result);
    }

    @Test
    public void testInsertShort() {
        short[] arr = { 1, 2, 3 };
        short[] result = N.insert(arr, 1, (short) 9);
        assertArrayEquals(new short[] { 1, 9, 2, 3 }, result);
    }

    @Test
    public void testInsertInt() {
        int[] arr = { 1, 2, 3 };
        int[] result = N.insert(arr, 1, 9);
        assertArrayEquals(new int[] { 1, 9, 2, 3 }, result);
    }

    @Test
    public void testInsertLong() {
        long[] arr = { 1L, 2L, 3L };
        long[] result = N.insert(arr, 1, 9L);
        assertArrayEquals(new long[] { 1L, 9L, 2L, 3L }, result);
    }

    @Test
    public void testInsertFloat() {
        float[] arr = { 1.0f, 2.0f, 3.0f };
        float[] result = N.insert(arr, 1, 9.0f);
        assertArrayEquals(new float[] { 1.0f, 9.0f, 2.0f, 3.0f }, result, 0.001f);
    }

    @Test
    public void testInsertDouble() {
        double[] arr = { 1.0, 2.0, 3.0 };
        double[] result = N.insert(arr, 1, 9.0);
        assertArrayEquals(new double[] { 1.0, 9.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testInsertObject() {
        Integer[] arr = { 1, 2, 3 };
        Integer[] result = N.insert(arr, 1, 9);
        assertArrayEquals(new Integer[] { 1, 9, 2, 3 }, result);
    }

    @Test
    public void testInsertInvalidIndex() {
        int[] arr = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> N.insert(arr, 4, 9));
    }

    // Tests for insertAll methods

    @Test
    public void testInsertAllBoolean() {
        boolean[] arr = { true, false };
        boolean[] result = N.insertAll(arr, 1, true, false);
        assertArrayEquals(new boolean[] { true, true, false, false }, result);
    }

    @Test
    public void testInsertAllChar() {
        char[] arr = { 'a', 'b' };
        char[] result = N.insertAll(arr, 1, 'x', 'y');
        assertArrayEquals(new char[] { 'a', 'x', 'y', 'b' }, result);
    }

    @Test
    public void testInsertAllByte() {
        byte[] arr = { 1, 2 };
        byte[] result = N.insertAll(arr, 1, (byte) 8, (byte) 9);
        assertArrayEquals(new byte[] { 1, 8, 9, 2 }, result);
    }

    @Test
    public void testInsertAllShort() {
        short[] arr = { 1, 2 };
        short[] result = N.insertAll(arr, 1, (short) 8, (short) 9);
        assertArrayEquals(new short[] { 1, 8, 9, 2 }, result);
    }

    @Test
    public void testInsertAllInt() {
        int[] arr = { 1, 2 };
        int[] result = N.insertAll(arr, 1, 8, 9);
        assertArrayEquals(new int[] { 1, 8, 9, 2 }, result);
    }

    @Test
    public void testInsertAllLong() {
        long[] arr = { 1L, 2L };
        long[] result = N.insertAll(arr, 1, 8L, 9L);
        assertArrayEquals(new long[] { 1L, 8L, 9L, 2L }, result);
    }

    @Test
    public void testInsertAllFloat() {
        float[] arr = { 1.0f, 2.0f };
        float[] result = N.insertAll(arr, 1, 8.0f, 9.0f);
        assertArrayEquals(new float[] { 1.0f, 8.0f, 9.0f, 2.0f }, result, 0.001f);
    }

    @Test
    public void testInsertAllDouble() {
        double[] arr = { 1.0, 2.0 };
        double[] result = N.insertAll(arr, 1, 8.0, 9.0);
        assertArrayEquals(new double[] { 1.0, 8.0, 9.0, 2.0 }, result, 0.001);
    }

    @Test
    public void testInsertAllString() {
        String[] arr = { "a", "b" };
        String[] result = N.insertAll(arr, 1, "x", "y");
        assertArrayEquals(new String[] { "a", "x", "y", "b" }, result);
    }

    @Test
    public void testInsertAllObject() {
        Integer[] arr = { 1, 2 };
        Integer[] result = N.insertAll(arr, 1, 8, 9);
        assertArrayEquals(new Integer[] { 1, 8, 9, 2 }, result);
    }

    @Test
    public void testInsertAllList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        boolean result = N.insertAll(list, 1, "x", "y");
        assertTrue(result);
        assertEquals(Arrays.asList("a", "x", "y", "b", "c"), list);
    }

    @Test
    public void testInsertAllListCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        List<String> toInsert = Arrays.asList("x", "y");
        boolean result = N.insertAll(list, 1, toInsert);
        assertTrue(result);
        assertEquals(Arrays.asList("a", "x", "y", "b", "c"), list);
    }

    // Tests for deleteByIndex methods

    @Test
    public void testDeleteByIndexBoolean() {
        boolean[] arr = { true, false, true, false };
        boolean[] result = N.deleteByIndex(arr, 1);
        assertArrayEquals(new boolean[] { true, true, false }, result);

        // Test deleting first element
        result = N.deleteByIndex(arr, 0);
        assertArrayEquals(new boolean[] { false, true, false }, result);

        // Test deleting last element
        result = N.deleteByIndex(arr, 3);
        assertArrayEquals(new boolean[] { true, false, true }, result);
    }

    @Test
    public void testDeleteByIndexChar() {
        char[] arr = { 'a', 'b', 'c', 'd' };
        char[] result = N.deleteByIndex(arr, 1);
        assertArrayEquals(new char[] { 'a', 'c', 'd' }, result);
    }

    @Test
    public void testDeleteByIndexByte() {
        byte[] arr = { 1, 2, 3, 4 };
        byte[] result = N.deleteByIndex(arr, 1);
        assertArrayEquals(new byte[] { 1, 3, 4 }, result);
    }

    @Test
    public void testDeleteByIndexShort() {
        short[] arr = { 1, 2, 3, 4 };
        short[] result = N.deleteByIndex(arr, 1);
        assertArrayEquals(new short[] { 1, 3, 4 }, result);
    }

    @Test
    public void testDeleteByIndexInt() {
        int[] arr = { 1, 2, 3, 4 };
        int[] result = N.deleteByIndex(arr, 1);
        assertArrayEquals(new int[] { 1, 3, 4 }, result);
    }

    @Test
    public void testDeleteByIndexLong() {
        long[] arr = { 1L, 2L, 3L, 4L };
        long[] result = N.deleteByIndex(arr, 1);
        assertArrayEquals(new long[] { 1L, 3L, 4L }, result);
    }

    @Test
    public void testDeleteByIndexFloat() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] result = N.deleteByIndex(arr, 1);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 4.0f }, result, 0.001f);
    }

    @Test
    public void testDeleteByIndexDouble() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0 };
        double[] result = N.deleteByIndex(arr, 1);
        assertArrayEquals(new double[] { 1.0, 3.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testDeleteByIndexObject() {
        Integer[] arr = { 1, 2, 3, 4 };
        Integer[] result = N.deleteByIndex(arr, 1);
        assertArrayEquals(new Integer[] { 1, 3, 4 }, result);
    }

    @Test
    public void testDeleteByIndexInvalidIndex() {
        int[] arr = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> N.deleteByIndex(arr, 3));
    }

    // Tests for deleteAllByIndices methods

    @Test
    public void testDeleteAllByIndicesBoolean() {
        boolean[] arr = { true, false, true, false, true };
        boolean[] result = N.deleteAllByIndices(arr, 1, 3);
        assertArrayEquals(new boolean[] { true, true, true }, result);

        // Test with duplicate indices
        result = N.deleteAllByIndices(arr, 1, 1, 3);
        assertArrayEquals(new boolean[] { true, true, true }, result);

        // Test with unsorted indices
        result = N.deleteAllByIndices(arr, 3, 1);
        assertArrayEquals(new boolean[] { true, true, true }, result);
    }

    @Test
    public void testDeleteAllByIndicesChar() {
        char[] arr = { 'a', 'b', 'c', 'd', 'e' };
        char[] result = N.deleteAllByIndices(arr, 1, 3);
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, result);
    }

    @Test
    public void testDeleteAllByIndicesByte() {
        byte[] arr = { 1, 2, 3, 4, 5 };
        byte[] result = N.deleteAllByIndices(arr, 1, 3);
        assertArrayEquals(new byte[] { 1, 3, 5 }, result);
    }

    @Test
    public void testDeleteAllByIndicesShort() {
        short[] arr = { 1, 2, 3, 4, 5 };
        short[] result = N.deleteAllByIndices(arr, 1, 3);
        assertArrayEquals(new short[] { 1, 3, 5 }, result);
    }

    @Test
    public void testDeleteAllByIndicesInt() {
        int[] arr = { 1, 2, 3, 4, 5 };
        int[] result = N.deleteAllByIndices(arr, 1, 3);
        assertArrayEquals(new int[] { 1, 3, 5 }, result);
    }

    @Test
    public void testDeleteAllByIndicesLong() {
        long[] arr = { 1L, 2L, 3L, 4L, 5L };
        long[] result = N.deleteAllByIndices(arr, 1, 3);
        assertArrayEquals(new long[] { 1L, 3L, 5L }, result);
    }

    @Test
    public void testDeleteAllByIndicesFloat() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] result = N.deleteAllByIndices(arr, 1, 3);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 5.0f }, result, 0.001f);
    }

    @Test
    public void testDeleteAllByIndicesDouble() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] result = N.deleteAllByIndices(arr, 1, 3);
        assertArrayEquals(new double[] { 1.0, 3.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testDeleteAllByIndicesString() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String[] result = N.deleteAllByIndices(arr, 1, 3);
        assertArrayEquals(new String[] { "a", "c", "e" }, result);
    }

    @Test
    public void testDeleteAllByIndicesObject() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Integer[] result = N.deleteAllByIndices(arr, 1, 3);
        assertArrayEquals(new Integer[] { 1, 3, 5 }, result);
    }

    @Test
    public void testDeleteAllByIndicesList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        boolean result = N.deleteAllByIndices(list, 1, 3);
        assertTrue(result);
        assertEquals(Arrays.asList("a", "c", "e"), list);

        // Test with empty indices
        list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        result = N.deleteAllByIndices(list);
        assertFalse(result);
        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    // Tests for remove methods

    @Test
    public void testRemoveBoolean() {
        boolean[] arr = { true, false, true, false };
        boolean[] result = N.remove(arr, true);
        assertArrayEquals(new boolean[] { false, true, false }, result);

        // Test removing non-existent value
        result = N.remove(new boolean[] { false, false }, true);
        assertArrayEquals(new boolean[] { false, false }, result);

        // Test with empty array
        result = N.remove(new boolean[0], true);
        assertEquals(0, result.length);
    }

    @Test
    public void testRemoveChar() {
        char[] arr = { 'a', 'b', 'a', 'c' };
        char[] result = N.remove(arr, 'a');
        assertArrayEquals(new char[] { 'b', 'a', 'c' }, result);
    }

    @Test
    public void testRemoveByte() {
        byte[] arr = { 1, 2, 1, 3 };
        byte[] result = N.remove(arr, (byte) 1);
        assertArrayEquals(new byte[] { 2, 1, 3 }, result);
    }

    @Test
    public void testRemoveShort() {
        short[] arr = { 1, 2, 1, 3 };
        short[] result = N.remove(arr, (short) 1);
        assertArrayEquals(new short[] { 2, 1, 3 }, result);
    }

    @Test
    public void testRemoveInt() {
        int[] arr = { 1, 2, 1, 3 };
        int[] result = N.remove(arr, 1);
        assertArrayEquals(new int[] { 2, 1, 3 }, result);
    }

    @Test
    public void testRemoveLong() {
        long[] arr = { 1L, 2L, 1L, 3L };
        long[] result = N.remove(arr, 1L);
        assertArrayEquals(new long[] { 2L, 1L, 3L }, result);
    }

    @Test
    public void testRemoveFloat() {
        float[] arr = { 1.0f, 2.0f, 1.0f, 3.0f };
        float[] result = N.remove(arr, 1.0f);
        assertArrayEquals(new float[] { 2.0f, 1.0f, 3.0f }, result, 0.001f);
    }

    @Test
    public void testRemoveDouble() {
        double[] arr = { 1.0, 2.0, 1.0, 3.0 };
        double[] result = N.remove(arr, 1.0);
        assertArrayEquals(new double[] { 2.0, 1.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testRemoveString() {
        String[] arr = { "a", "b", "a", "c" };
        String[] result = N.remove(arr, "a");
        assertArrayEquals(new String[] { "b", "a", "c" }, result);
    }

    @Test
    public void testRemoveObject() {
        Integer[] arr = { 1, 2, 1, 3 };
        Integer[] result = N.remove(arr, 1);
        assertArrayEquals(new Integer[] { 2, 1, 3 }, result);
    }

    @Test
    public void testRemoveCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "a", "c"));
        boolean result = N.remove(list, "a");
        assertTrue(result);
        assertEquals(Arrays.asList("b", "a", "c"), list);

        // Test removing non-existent value
        result = N.remove(list, "x");
        assertFalse(result);
    }

    // Tests for removeAll methods

    @Test
    public void testRemoveAllBoolean() {
        boolean[] arr = { true, false, true, false, true };
        boolean[] result = N.removeAll(arr, true, false);
        assertEquals(0, result.length);

        // Test removing single value
        result = N.removeAll(arr, true);
        assertArrayEquals(new boolean[] { false, false }, result);

        // Test with empty values to remove
        result = N.removeAll(arr);
        assertArrayEquals(arr, result);
    }

    @Test
    public void testRemoveAllChar() {
        char[] arr = { 'a', 'b', 'c', 'd', 'a' };
        char[] result = N.removeAll(arr, 'a', 'c');
        assertArrayEquals(new char[] { 'b', 'd' }, result);
    }

    @Test
    public void testRemoveAllByte() {
        byte[] arr = { 1, 2, 3, 4, 1 };
        byte[] result = N.removeAll(arr, (byte) 1, (byte) 3);
        assertArrayEquals(new byte[] { 2, 4 }, result);
    }

    @Test
    public void testRemoveAllShort() {
        short[] arr = { 1, 2, 3, 4, 1 };
        short[] result = N.removeAll(arr, (short) 1, (short) 3);
        assertArrayEquals(new short[] { 2, 4 }, result);
    }

    @Test
    public void testRemoveAllInt() {
        int[] arr = { 1, 2, 3, 4, 1 };
        int[] result = N.removeAll(arr, 1, 3);
        assertArrayEquals(new int[] { 2, 4 }, result);
    }

    @Test
    public void testRemoveAllLong() {
        long[] arr = { 1L, 2L, 3L, 4L, 1L };
        long[] result = N.removeAll(arr, 1L, 3L);
        assertArrayEquals(new long[] { 2L, 4L }, result);
    }

    @Test
    public void testRemoveAllFloat() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 1.0f };
        float[] result = N.removeAll(arr, 1.0f, 3.0f);
        assertArrayEquals(new float[] { 2.0f, 4.0f }, result, 0.001f);
    }

    @Test
    public void testRemoveAllDouble() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 1.0 };
        double[] result = N.removeAll(arr, 1.0, 3.0);
        assertArrayEquals(new double[] { 2.0, 4.0 }, result, 0.001);
    }

    @Test
    public void testRemoveAllString() {
        String[] arr = { "a", "b", "c", "d", "a" };
        String[] result = N.removeAll(arr, "a", "c");
        assertArrayEquals(new String[] { "b", "d" }, result);
    }

    @Test
    public void testRemoveAllObject() {
        Integer[] arr = { 1, 2, 3, 4, 1 };
        Integer[] result = N.removeAll(arr, 1, 3);
        assertArrayEquals(new Integer[] { 2, 4 }, result);
    }

    @Test
    public void testRemoveAllCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "a"));
        boolean result = N.removeAll(list, "a", "c");
        assertTrue(result);
        assertEquals(Arrays.asList("b", "d"), list);
    }

    @Test
    public void testRemoveAllCollectionIterable() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "a"));
        List<String> toRemove = Arrays.asList("a", "c");
        boolean result = N.removeAll(list, toRemove);
        assertTrue(result);
        assertEquals(Arrays.asList("b", "d"), list);
    }

    @Test
    public void testRemoveAllCollectionIterator() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "a"));
        List<String> toRemove = Arrays.asList("a", "c");
        boolean result = N.removeAll(list, toRemove.iterator());
        assertTrue(result);
        assertEquals(Arrays.asList("b", "d"), list);
    }

    // Tests for removeAllOccurrences methods

    @Test
    public void testRemoveAllOccurrencesBoolean() {
        boolean[] arr = { true, false, true, false, true };
        boolean[] result = N.removeAllOccurrences(arr, true);
        assertArrayEquals(new boolean[] { false, false }, result);

        // Test with array containing only the value to remove
        result = N.removeAllOccurrences(new boolean[] { true, true, true }, true);
        assertEquals(0, result.length);

        // Test with empty array
        result = N.removeAllOccurrences(new boolean[0], true);
        assertEquals(0, result.length);
    }

    @Test
    public void testRemoveAllOccurrencesChar() {
        char[] arr = { 'a', 'b', 'a', 'c', 'a' };
        char[] result = N.removeAllOccurrences(arr, 'a');
        assertArrayEquals(new char[] { 'b', 'c' }, result);
    }

    @Test
    public void testRemoveAllOccurrencesByte() {
        byte[] arr = { 1, 2, 1, 3, 1 };
        byte[] result = N.removeAllOccurrences(arr, (byte) 1);
        assertArrayEquals(new byte[] { 2, 3 }, result);
    }

    @Test
    public void testRemoveAllOccurrencesShort() {
        short[] arr = { 1, 2, 1, 3, 1 };
        short[] result = N.removeAllOccurrences(arr, (short) 1);
        assertArrayEquals(new short[] { 2, 3 }, result);
    }

    @Test
    public void testRemoveAllOccurrencesInt() {
        int[] arr = { 1, 2, 1, 3, 1 };
        int[] result = N.removeAllOccurrences(arr, 1);
        assertArrayEquals(new int[] { 2, 3 }, result);
    }

    @Test
    public void testRemoveAllOccurrencesLong() {
        long[] arr = { 1L, 2L, 1L, 3L, 1L };
        long[] result = N.removeAllOccurrences(arr, 1L);
        assertArrayEquals(new long[] { 2L, 3L }, result);
    }

    @Test
    public void testRemoveAllOccurrencesFloat() {
        float[] arr = { 1.0f, 2.0f, 1.0f, 3.0f, 1.0f };
        float[] result = N.removeAllOccurrences(arr, 1.0f);
        assertArrayEquals(new float[] { 2.0f, 3.0f }, result, 0.001f);
    }

    @Test
    public void testRemoveAllOccurrencesDouble() {
        double[] arr = { 1.0, 2.0, 1.0, 3.0, 1.0 };
        double[] result = N.removeAllOccurrences(arr, 1.0);
        assertArrayEquals(new double[] { 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testRemoveAllOccurrencesString() {
        String[] arr = { "a", "b", "a", "c", "a" };
        String[] result = N.removeAllOccurrences(arr, "a");
        assertArrayEquals(new String[] { "b", "c" }, result);

        // Test with null values
        String[] arrWithNull = { "a", null, "a", null, "b" };
        result = N.removeAllOccurrences(arrWithNull, null);
        assertArrayEquals(new String[] { "a", "a", "b" }, result);
    }

    @Test
    public void testRemoveAllOccurrencesObject() {
        Integer[] arr = { 1, 2, 1, 3, 1 };
        Integer[] result = N.removeAllOccurrences(arr, 1);
        assertArrayEquals(new Integer[] { 2, 3 }, result);
    }

    @Test
    public void testRemoveAllOccurrencesCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "a", "c", "a"));
        boolean result = N.removeAllOccurrences(list, "a");
        assertTrue(result);
        assertEquals(Arrays.asList("b", "c"), list);
    }

    // Tests for removeDuplicates methods

    @Test
    public void testRemoveDuplicatesBoolean() {
        boolean[] arr = { true, false, true, false, true };
        boolean[] result = N.removeDuplicates(arr);
        assertArrayEquals(new boolean[] { true, false }, result);

        // Test with no duplicates
        result = N.removeDuplicates(new boolean[] { true, false });
        assertArrayEquals(new boolean[] { true, false }, result);

        // Test with all same values
        result = N.removeDuplicates(new boolean[] { true, true, true });
        assertArrayEquals(new boolean[] { true }, result);
    }

    @Test
    public void testRemoveDuplicatesChar() {
        char[] arr = { 'a', 'b', 'a', 'c', 'b' };
        char[] result = N.removeDuplicates(arr);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);

        // Test with sorted array
        char[] sortedArr = { 'a', 'a', 'b', 'b', 'c' };
        result = N.removeDuplicates(sortedArr, true);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testRemoveDuplicatesByte() {
        byte[] arr = { 1, 2, 1, 3, 2 };
        byte[] result = N.removeDuplicates(arr);
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);

        // Test with sorted array
        byte[] sortedArr = { 1, 1, 2, 2, 3 };
        result = N.removeDuplicates(sortedArr, true);
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testRemoveDuplicatesShort() {
        short[] arr = { 1, 2, 1, 3, 2 };
        short[] result = N.removeDuplicates(arr);
        assertArrayEquals(new short[] { 1, 2, 3 }, result);

        // Test with sorted array
        short[] sortedArr = { 1, 1, 2, 2, 3 };
        result = N.removeDuplicates(sortedArr, true);
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testRemoveDuplicatesInt() {
        int[] arr = { 1, 2, 1, 3, 2 };
        int[] result = N.removeDuplicates(arr);
        assertArrayEquals(new int[] { 1, 2, 3 }, result);

        // Test with sorted array
        int[] sortedArr = { 1, 1, 2, 2, 3 };
        result = N.removeDuplicates(sortedArr, true);
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testRemoveDuplicatesLong() {
        long[] arr = { 1L, 2L, 1L, 3L, 2L };
        long[] result = N.removeDuplicates(arr);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);

        // Test with sorted array
        long[] sortedArr = { 1L, 1L, 2L, 2L, 3L };
        result = N.removeDuplicates(sortedArr, true);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testRemoveDuplicatesFloat() {
        float[] arr = { 1.0f, 2.0f, 1.0f, 3.0f, 2.0f };
        float[] result = N.removeDuplicates(arr);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, 0.001f);

        // Test with sorted array
        float[] sortedArr = { 1.0f, 1.0f, 2.0f, 2.0f, 3.0f };
        result = N.removeDuplicates(sortedArr, true);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result, 0.001f);
    }

    @Test
    public void testRemoveDuplicatesDouble() {
        double[] arr = { 1.0, 2.0, 1.0, 3.0, 2.0 };
        double[] result = N.removeDuplicates(arr);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);

        // Test with sorted array
        double[] sortedArr = { 1.0, 1.0, 2.0, 2.0, 3.0 };
        result = N.removeDuplicates(sortedArr, true);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result, 0.001);
    }

    @Test
    public void testRemoveDuplicatesString() {
        String[] arr = { "a", "b", "a", "c", "b" };
        String[] result = N.removeDuplicates(arr);
        assertArrayEquals(new String[] { "a", "b", "c" }, result);

        // Test with sorted array
        String[] sortedArr = { "a", "a", "b", "b", "c" };
        result = N.removeDuplicates(sortedArr, true);
        assertArrayEquals(new String[] { "a", "b", "c" }, result);
    }

    @Test
    public void testRemoveDuplicatesObject() {
        Integer[] arr = { 1, 2, 1, 3, 2 };
        Integer[] result = N.removeDuplicates(arr);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, result);

        // Test with sorted array
        Integer[] sortedArr = { 1, 1, 2, 2, 3 };
        result = N.removeDuplicates(sortedArr, true);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, result);
    }

    @Test
    public void testRemoveDuplicatesCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "a", "c", "b"));
        boolean result = N.removeDuplicates(list);
        assertTrue(result);
        assertEquals(Arrays.asList("a", "b", "c"), list);

        // Test with no duplicates
        list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        result = N.removeDuplicates(list);
        assertFalse(result);

        // Test with sorted collection
        list = new ArrayList<>(Arrays.asList("a", "a", "b", "b", "c"));
        result = N.removeDuplicates(list, true);
        assertTrue(result);
        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    // Tests for updateAll methods (which throw exceptions)

    @Test
    public void testUpdateAllUsingReplaceAllInstead() {
        assertThrows(UnsupportedOperationException.class, () -> N.updateAllUsingReplaceAllInstead());
    }

    @Test
    public void testUpdateIfUsingReplaceIfInstead() {
        assertThrows(UnsupportedOperationException.class, () -> N.updateIfUsingReplaceIfInstead());
    }

    // Tests for copyThenSetAll methods

    @Test
    public void testCopyThenSetAll() {
        Integer[] arr = { 1, 2, 3, 4 };
        Integer[] result = N.copyThenSetAll(arr, i -> i * 10);
        assertArrayEquals(new Integer[] { 0, 10, 20, 30 }, result);
        // Verify original array is unchanged
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, arr);

        // Test with null array
        assertNull(N.copyThenSetAll((Integer[]) null, i -> i));

        // Test with empty array
        Integer[] empty = new Integer[0];
        result = N.copyThenSetAll(empty, i -> i);
        assertEquals(0, result.length);
    }

    @Test
    public void testCopyThenReplaceAll() {
        Integer[] arr = { 1, 2, 3, 4 };
        Integer[] result = N.copyThenReplaceAll(arr, val -> val * 2);
        assertArrayEquals(new Integer[] { 2, 4, 6, 8 }, result);
        // Verify original array is unchanged
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, arr);

        // Test with null array
        assertNull(N.copyThenReplaceAll((Integer[]) null, val -> val));

        // Test with empty array
        Integer[] empty = new Integer[0];
        result = N.copyThenReplaceAll(empty, val -> val);
        assertEquals(0, result.length);
    }

    // Tests for setAll with IntObjFunction

    @Test
    public void testSetAllArrayWithIntObjFunction() throws Exception {
        String[] arr = { "a", "b", "c", "d" };
        N.setAll(arr, (i, val) -> val + i);
        assertArrayEquals(new String[] { "a0", "b1", "c2", "d3" }, arr);
    }

    @Test
    public void testSetAllListWithIntObjFunction() throws Exception {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        N.setAll(list, (i, val) -> val + i);
        assertEquals(Arrays.asList("a0", "b1", "c2", "d3"), list);
    }

    // Tests for updateAll methods

    @Test
    public void testUpdateAllArray() throws Exception {
        String[] arr = { "a", "b", "c", "d" };
        N.updateAll(arr, String::toUpperCase);
        assertArrayEquals(new String[] { "A", "B", "C", "D" }, arr);
    }

    @Test
    public void testUpdateAllList() throws Exception {
        {
            List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
            N.updateAll(list, String::toUpperCase);
            assertEquals(Arrays.asList("A", "B", "C", "D"), list);
        }
        {
            List<Character> list = N.toCollection(Array.rangeClosed('a', 'z'), IntFunctions.ofLinkedList());
            N.updateAll(list, Strings::toUpperCase);
            assertEquals(N.toCollection(Array.rangeClosed('A', 'Z'), IntFunctions.ofLinkedList()), list);
        }
        {
            List<Character> list = N.toCollection(Array.rangeClosed('a', 'z'), IntFunctions.ofLinkedList());
            N.setAll(list, i -> Strings.toUpperCase(list.get(i)));
            assertEquals(N.toCollection(Array.rangeClosed('A', 'Z'), IntFunctions.ofLinkedList()), list);
        }
        {
            List<Character> list = N.toCollection(Array.rangeClosed('a', 'z'), IntFunctions.ofLinkedList());
            N.setAll(list, (i, e) -> Strings.toUpperCase((char) (list.get(i) + e - e)));
            assertEquals(N.toCollection(Array.rangeClosed('A', 'Z'), IntFunctions.ofLinkedList()), list);
        }
    }

    // Tests for copyThenSetAll with IntObjFunction

    @Test
    public void testCopyThenSetAllWithIntObjFunction() throws Exception {
        String[] arr = { "a", "b", "c", "d" };
        String[] result = N.copyThenSetAll(arr, (i, val) -> val + i);
        assertArrayEquals(new String[] { "a0", "b1", "c2", "d3" }, result);
        // Verify original array is unchanged
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, arr);
    }

    // Tests for copyThenUpdateAll

    @Test
    public void testCopyThenUpdateAll() throws Exception {
        String[] arr = { "a", "b", "c", "d" };
        String[] result = N.copyThenUpdateAll(arr, String::toUpperCase);
        assertArrayEquals(new String[] { "A", "B", "C", "D" }, result);
        // Verify original array is unchanged
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, arr);
    }

    // Additional tests for insert with String

    @Test
    public void testInsertString() {
        {
            String str = "hello";
            String result = N.insert(str, 2, "XX");
            assertEquals("heXXllo", result);

            // Test at beginning
            result = N.insert(str, 0, "XX");
            assertEquals("XXhello", result);

            // Test at end
            result = N.insert(str, 5, "XX");
            assertEquals("helloXX", result);

            // Test with empty string to insert
            result = N.insert(str, 2, "");
            assertEquals("hello", result);

            // Test with null string
            result = N.insert((String) null, 0, "XX");
            assertEquals("XX", result);

            // Test with empty string
            result = N.insert("", 0, "XX");
            assertEquals("XX", result);
        }
        {
            String[] arr = { "a", "b", "c" };
            String[] result = N.insert(arr, 1, "x");
            assertArrayEquals(new String[] { "a", "x", "b", "c" }, result);
        }

    }

    // Tests for removeDuplicates with range

    //    @Test
    //    public void testRemoveDuplicatesBooleanRange() {
    //        boolean[] arr = { true, false, true, false, true };
    //        boolean[] result = N.removeDuplicates(arr, 1, 4);
    //        assertArrayEquals(new boolean[] { false, true }, result);
    //    }

    @Test
    public void testRemoveDuplicatesCharRange() {
        char[] arr = { 'a', 'b', 'a', 'b', 'c' };
        char[] result = N.removeDuplicates(arr, 1, 4, false);
        assertArrayEquals(new char[] { 'b', 'a' }, result);

        // Test with sorted range
        char[] sortedArr = { 'x', 'a', 'a', 'b', 'y' };
        result = N.removeDuplicates(sortedArr, 1, 4, true);
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testRemoveDuplicatesByteRange() {
        byte[] arr = { 5, 1, 2, 1, 5 };
        byte[] result = N.removeDuplicates(arr, 1, 4, false);
        assertArrayEquals(new byte[] { 1, 2 }, result);
    }

    @Test
    public void testRemoveDuplicatesShortRange() {
        short[] arr = { 5, 1, 2, 1, 5 };
        short[] result = N.removeDuplicates(arr, 1, 4, false);
        assertArrayEquals(new short[] { 1, 2 }, result);
    }

    @Test
    public void testRemoveDuplicatesIntRange() {
        int[] arr = { 5, 1, 2, 1, 5 };
        int[] result = N.removeDuplicates(arr, 1, 4, false);
        assertArrayEquals(new int[] { 1, 2 }, result);
    }

    @Test
    public void testRemoveDuplicatesLongRange() {
        long[] arr = { 5L, 1L, 2L, 1L, 5L };
        long[] result = N.removeDuplicates(arr, 1, 4, false);
        assertArrayEquals(new long[] { 1L, 2L }, result);
    }

    @Test
    public void testRemoveDuplicatesFloatRange() {
        float[] arr = { 5.0f, 1.0f, 2.0f, 1.0f, 5.0f };
        float[] result = N.removeDuplicates(arr, 1, 4, false);
        assertArrayEquals(new float[] { 1.0f, 2.0f }, result, 0.001f);
    }

    @Test
    public void testRemoveDuplicatesDoubleRange() {
        double[] arr = { 5.0, 1.0, 2.0, 1.0, 5.0 };
        double[] result = N.removeDuplicates(arr, 1, 4, false);
        assertArrayEquals(new double[] { 1.0, 2.0 }, result, 0.001);
    }

    @Test
    public void testRemoveDuplicatesStringRange() {
        String[] arr = { "x", "a", "b", "a", "y" };
        String[] result = N.removeDuplicates(arr, 1, 4, false);
        assertArrayEquals(new String[] { "a", "b" }, result);
    }

    @Test
    public void testRemoveDuplicatesObjectRange() {
        Integer[] arr = { 5, 1, 2, 1, 5 };
        Integer[] result = N.removeDuplicates(arr, 1, 4, false);
        assertArrayEquals(new Integer[] { 1, 2 }, result);
    }

    // Edge case tests

    @Test
    public void testEmptyArrayOperations() {
        // Test replaceIf with empty array
        assertEquals(0, N.replaceIf(new int[0], val -> true, 1));

        // Test replaceAll with empty array
        assertEquals(0, N.replaceAll(new int[0], 1, 2));

        // Test add with empty array
        assertArrayEquals(new int[] { 1 }, N.add(new int[0], 1));

        // Test remove with empty array
        assertArrayEquals(new int[0], N.remove(new int[0], 1));

        // Test removeAll with empty array
        assertArrayEquals(new int[0], N.removeAll(new int[0], 1, 2));

        // Test removeDuplicates with empty array
        assertArrayEquals(new int[0], N.removeDuplicates(new int[0]));
    }

    @Test
    public void testNullArrayOperations() {
        // Test replaceIf with null array
        assertEquals(0, N.replaceIf((int[]) null, val -> true, 1));

        // Test replaceAll with null array
        assertEquals(0, N.replaceAll((int[]) null, 1, 2));

        // Test add with null array
        assertArrayEquals(new int[] { 1 }, N.add((int[]) null, 1));

        // Test remove with null array
        assertArrayEquals(new int[0], N.remove((int[]) null, 1));

        // Test removeAll with null array
        assertArrayEquals(new int[0], N.removeAll((int[]) null, 1, 2));

        // Test removeDuplicates with null array
        assertArrayEquals(new int[0], N.removeDuplicates((int[]) null));
    }

    @Test
    public void testLargeArrayOperations() {
        // Create a large array
        int[] largeArray = new int[1000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i % 100; // Creates duplicates
        }

        // Test replaceIf
        int count = N.replaceIf(largeArray, val -> val < 50, -1);
        assertEquals(500, count);

        // Test removeDuplicates
        int[] unique = N.removeDuplicates(largeArray);
        assertEquals(51, unique.length);
    }

    @Test
    public void testCollectionWithNullElements() {
        List<String> list = new ArrayList<>(Arrays.asList("a", null, "b", null, "c"));

        // Test replaceAll with null
        int count = N.replaceAll(list, null, "x");
        assertEquals(2, count);
        assertEquals(Arrays.asList("a", "x", "b", "x", "c"), list);

        // Test remove with null
        list = new ArrayList<>(Arrays.asList("a", null, "b", null, "c"));
        boolean removed = N.remove(list, null);
        assertTrue(removed);
        assertEquals(Arrays.asList("a", "b", null, "c"), list);

        // Test removeAllOccurrences with null
        list = new ArrayList<>(Arrays.asList("a", null, "b", null, "c"));
        removed = N.removeAllOccurrences(list, null);
        assertTrue(removed);
        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    @Test
    public void testSpecialFloatingPointValues() {
        // Test with NaN
        float[] floatArr = { 1.0f, Float.NaN, 2.0f, Float.NaN };
        int count = N.replaceAll(floatArr, Float.NaN, 0.0f);
        assertEquals(2, count); // NaN != NaN

        double[] doubleArr = { 1.0, Double.NaN, 2.0, Double.NaN };
        count = N.replaceAll(doubleArr, Double.NaN, 0.0);
        assertEquals(2, count); // NaN != NaN

        // Test with positive/negative infinity
        floatArr = new float[] { 1.0f, Float.POSITIVE_INFINITY, 2.0f, Float.NEGATIVE_INFINITY };
        count = N.replaceAll(floatArr, Float.POSITIVE_INFINITY, 999.0f);
        assertEquals(1, count);
        assertArrayEquals(new float[] { 1.0f, 999.0f, 2.0f, Float.NEGATIVE_INFINITY }, floatArr, 0.001f);

        doubleArr = new double[] { 1.0, Double.POSITIVE_INFINITY, 2.0, Double.NEGATIVE_INFINITY };
        count = N.replaceAll(doubleArr, Double.POSITIVE_INFINITY, 999.0);
        assertEquals(1, count);
        assertArrayEquals(new double[] { 1.0, 999.0, 2.0, Double.NEGATIVE_INFINITY }, doubleArr, 0.001);
    }

    @Test
    public void testLinkedListOperations() {
        // LinkedList has different performance characteristics
        LinkedList<String> linkedList = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));

        // Test deleteAllByIndices with LinkedList
        boolean result = N.deleteAllByIndices(linkedList, 1, 3);
        assertTrue(result);
        assertEquals(Arrays.asList("a", "c", "e"), linkedList);

        // Test replaceAll with LinkedList
        linkedList = new LinkedList<>(Arrays.asList("a", "b", "a", "c"));
        int count = N.replaceAll(linkedList, "a", "x");
        assertEquals(2, count);
        assertEquals(Arrays.asList("x", "b", "x", "c"), linkedList);
    }

    @Test
    public void testBoundaryIndices() {
        int[] arr = { 1, 2, 3, 4, 5 };

        // Test insert at boundary
        int[] result = N.insert(arr, 0, 0);
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5 }, result);

        result = N.insert(arr, 5, 6);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5, 6 }, result);

        // Test deleteByIndex at boundaries
        result = N.deleteByIndex(arr, 0);
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, result);

        result = N.deleteByIndex(arr, 4);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result);

        // Test removeDuplicates with single element range
        result = N.removeDuplicates(arr, 2, 3, false);
        assertArrayEquals(new int[] { 3 }, result);
    }

    @Test
    public void testPerformanceConsiderations() {
        // Test with RandomAccess list (ArrayList)
        List<Integer> arrayList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            arrayList.add(i);
        }
        N.replaceAll(arrayList, val -> val * 2);

        // Test with non-RandomAccess list (LinkedList)
        List<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < 100; i++) {
            linkedList.add(i);
        }
        N.replaceAll(linkedList, val -> val * 2);

        // Both should produce same result
        assertEquals(arrayList, linkedList);
    }
}
