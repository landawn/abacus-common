package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class N102Test extends TestBase {

    private char[] charArray;
    private byte[] byteArray;
    private short[] shortArray;
    private int[] intArray;
    private long[] longArray;
    private float[] floatArray;
    private double[] doubleArray;
    private String[] stringArray;
    private Integer[] integerArray;
    private List<Integer> integerList;
    private List<String> stringList;

    @BeforeEach
    public void setUp() {
        charArray = new char[] { 'a', 'b', 'c', 'd', 'e' };
        byteArray = new byte[] { 1, 2, 3, 4, 5 };
        shortArray = new short[] { 1, 2, 3, 4, 5 };
        intArray = new int[] { 1, 2, 3, 4, 5 };
        longArray = new long[] { 1L, 2L, 3L, 4L, 5L };
        floatArray = new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        doubleArray = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 };
        stringArray = new String[] { "one", "two", "three", "four", "five" };
        integerArray = new Integer[] { 1, 2, 3, 4, 5 };
        integerList = Arrays.asList(1, 2, 3, 4, 5);
        stringList = Arrays.asList("one", "two", "three", "four", "five");
    }

    @Test
    public void testDeleteRange_boolean() {
        boolean[] arr = { true, false, true, false, true };
        boolean[] result = N.deleteRange(arr, 1, 3);
        assertArrayEquals(new boolean[] { true, false, true }, result);

        assertEquals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.deleteRange((boolean[]) null, 0, 0));

        boolean[] original = { true, false };
        boolean[] noDeletion = N.deleteRange(original, 0, 0);
        assertArrayEquals(original, noDeletion);
    }

    @Test
    public void testDeleteRange_char() {
        char[] result = N.deleteRange(charArray, 1, 3);
        assertArrayEquals(new char[] { 'a', 'd', 'e' }, result);

        assertEquals(CommonUtil.EMPTY_CHAR_ARRAY, N.deleteRange((char[]) null, 0, 0));
        assertArrayEquals(charArray.clone(), N.deleteRange(charArray, 0, 0));
    }

    @Test
    public void testDeleteRange_byte() {
        byte[] result = N.deleteRange(byteArray, 1, 3);
        assertArrayEquals(new byte[] { 1, 4, 5 }, result);

        assertArrayEquals(CommonUtil.EMPTY_BYTE_ARRAY, N.deleteRange((byte[]) null, 0, 0));
    }

    @Test
    public void testDeleteRange_short() {
        short[] result = N.deleteRange(shortArray, 1, 3);
        assertArrayEquals(new short[] { 1, 4, 5 }, result);

        assertArrayEquals(CommonUtil.EMPTY_SHORT_ARRAY, N.deleteRange((short[]) null, 0, 0));
    }

    @Test
    public void testDeleteRange_int() {
        int[] result = N.deleteRange(intArray, 1, 3);
        assertArrayEquals(new int[] { 1, 4, 5 }, result);

        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.deleteRange((int[]) null, 0, 0));
    }

    @Test
    public void testDeleteRange_long() {
        long[] result = N.deleteRange(longArray, 1, 3);
        assertArrayEquals(new long[] { 1L, 4L, 5L }, result);

        assertEquals(CommonUtil.EMPTY_LONG_ARRAY, N.deleteRange((long[]) null, 0, 0));
    }

    @Test
    public void testDeleteRange_float() {
        float[] result = N.deleteRange(floatArray, 1, 3);
        assertArrayEquals(new float[] { 1.0f, 4.0f, 5.0f }, result);

        assertEquals(CommonUtil.EMPTY_FLOAT_ARRAY, N.deleteRange((float[]) null, 0, 0));
    }

    @Test
    public void testDeleteRange_double() {
        double[] result = N.deleteRange(doubleArray, 1, 3);
        assertArrayEquals(new double[] { 1.0, 4.0, 5.0 }, result);

        assertEquals(CommonUtil.EMPTY_DOUBLE_ARRAY, N.deleteRange((double[]) null, 0, 0));
    }

    @Test
    public void testDeleteRange_String() {
        String[] result = N.deleteRange(stringArray, 1, 3);
        assertArrayEquals(new String[] { "one", "four", "five" }, result);

        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, N.deleteRange((String[]) null, 0, 0));
    }

    @Test
    public void testDeleteRange_generic() {
        Integer[] result = N.deleteRange(integerArray, 1, 3);
        assertArrayEquals(new Integer[] { 1, 4, 5 }, result);
    }

    @Test
    public void testDeleteRange_List() {
        List<Integer> list = new ArrayList<>(integerList);
        assertTrue(N.deleteRange(list, 1, 3));
        assertEquals(Arrays.asList(1, 4, 5), list);

        List<Integer> emptyList = new ArrayList<>();
        assertFalse(N.deleteRange(emptyList, 0, 0));
    }

    @Test
    public void testDeleteRange_String_method() {
        String result = N.deleteRange("hello world", 5, 6);
        assertEquals("helloworld", result);
    }

    @Test
    public void testReplaceRange_boolean() {
        boolean[] arr = { true, false, true, false, true };
        boolean[] replacement = { false, false };
        boolean[] result = N.replaceRange(arr, 1, 3, replacement);
        assertArrayEquals(new boolean[] { true, false, false, false, true }, result);

        boolean[] emptyReplacement = {};
        result = N.replaceRange(arr, 1, 3, emptyReplacement);
        assertArrayEquals(new boolean[] { true, false, true }, result);
    }

    @Test
    public void testReplaceRange_char() {
        char[] replacement = { 'x', 'y' };
        char[] result = N.replaceRange(charArray, 1, 3, replacement);
        assertArrayEquals(new char[] { 'a', 'x', 'y', 'd', 'e' }, result);
    }

    @Test
    public void testReplaceRange_byte() {
        byte[] replacement = { 10, 20 };
        byte[] result = N.replaceRange(byteArray, 1, 3, replacement);
        assertArrayEquals(new byte[] { 1, 10, 20, 4, 5 }, result);
    }

    @Test
    public void testReplaceRange_short() {
        short[] replacement = { 10, 20 };
        short[] result = N.replaceRange(shortArray, 1, 3, replacement);
        assertArrayEquals(new short[] { 1, 10, 20, 4, 5 }, result);
    }

    @Test
    public void testReplaceRange_int() {
        int[] replacement = { 10, 20 };
        int[] result = N.replaceRange(intArray, 1, 3, replacement);
        assertArrayEquals(new int[] { 1, 10, 20, 4, 5 }, result);
    }

    @Test
    public void testReplaceRange_long() {
        long[] replacement = { 10L, 20L };
        long[] result = N.replaceRange(longArray, 1, 3, replacement);
        assertArrayEquals(new long[] { 1L, 10L, 20L, 4L, 5L }, result);
    }

    @Test
    public void testReplaceRange_float() {
        float[] replacement = { 10.0f, 20.0f };
        float[] result = N.replaceRange(floatArray, 1, 3, replacement);
        assertArrayEquals(new float[] { 1.0f, 10.0f, 20.0f, 4.0f, 5.0f }, result);
    }

    @Test
    public void testReplaceRange_double() {
        double[] replacement = { 10.0, 20.0 };
        double[] result = N.replaceRange(doubleArray, 1, 3, replacement);
        assertArrayEquals(new double[] { 1.0, 10.0, 20.0, 4.0, 5.0 }, result);
    }

    @Test
    public void testReplaceRange_String() {
        String[] replacement = { "ten", "twenty" };
        String[] result = N.replaceRange(stringArray, 1, 3, replacement);
        assertArrayEquals(new String[] { "one", "ten", "twenty", "four", "five" }, result);
    }

    @Test
    public void testReplaceRange_generic() {
        Integer[] replacement = { 10, 20 };
        Integer[] result = N.replaceRange(integerArray, 1, 3, replacement);
        assertArrayEquals(new Integer[] { 1, 10, 20, 4, 5 }, result);
    }

    @Test
    public void testReplaceRange_List() {
        List<Integer> list = new ArrayList<>(integerList);
        List<Integer> replacement = Arrays.asList(10, 20);
        assertTrue(N.replaceRange(list, 1, 3, replacement));
        assertEquals(Arrays.asList(1, 10, 20, 4, 5), list);

        assertFalse(N.replaceRange(list, 0, 0, Collections.emptyList()));
    }

    @Test
    public void testReplaceRange_String_method() {
        String result = N.replaceRange("hello world", 5, 6, " my ");
        assertEquals("hello my world", result);
    }

    @Test
    public void testMoveRange_boolean() {
        boolean[] arr = { true, false, true, false, true };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new boolean[] { true, false, true, false, true }, arr);
    }

    @Test
    public void testMoveRange_char() {
        char[] arr = { 'a', 'b', 'c', 'd', 'e' };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new char[] { 'a', 'd', 'e', 'b', 'c' }, arr);

        char[] arr2 = { 'a', 'b', 'c' };
        N.moveRange(arr2, 1, 1, 0);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, arr2);

        N.moveRange(arr2, 1, 2, 1);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, arr2);
    }

    @Test
    public void testMoveRange_byte() {
        byte[] arr = { 1, 2, 3, 4, 5 };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new byte[] { 1, 4, 5, 2, 3 }, arr);
    }

    @Test
    public void testMoveRange_short() {
        short[] arr = { 1, 2, 3, 4, 5 };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new short[] { 1, 4, 5, 2, 3 }, arr);
    }

    @Test
    public void testMoveRange_int() {
        int[] arr = { 1, 2, 3, 4, 5 };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new int[] { 1, 4, 5, 2, 3 }, arr);
    }

    @Test
    public void testMoveRange_long() {
        long[] arr = { 1L, 2L, 3L, 4L, 5L };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new long[] { 1L, 4L, 5L, 2L, 3L }, arr);
    }

    @Test
    public void testMoveRange_float() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new float[] { 1.0f, 4.0f, 5.0f, 2.0f, 3.0f }, arr);
    }

    @Test
    public void testMoveRange_double() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new double[] { 1.0, 4.0, 5.0, 2.0, 3.0 }, arr);
    }

    @Test
    public void testMoveRange_generic() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        N.moveRange(arr, 1, 3, 3);
        assertArrayEquals(new Integer[] { 1, 4, 5, 2, 3 }, arr);
    }

    @Test
    public void testMoveRange_List() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        assertTrue(N.moveRange(list, 1, 3, 3));
        assertEquals(Arrays.asList(1, 4, 5, 2, 3), list);

        assertFalse(N.moveRange(list, 1, 1, 0));
    }

    @Test
    public void testMoveRange_String_method() {
        String result = N.moveRange("hello", 1, 3, 3);
        assertEquals("hloel", result);
    }

    @Test
    public void testSkipRange_generic() {
        Integer[] result = N.skipRange(integerArray, 1, 3);
        assertArrayEquals(new Integer[] { 1, 4, 5 }, result);

        assertNull(N.skipRange((Integer[]) null, 0, 0));

        assertArrayEquals(integerArray.clone(), N.skipRange(integerArray, 0, 0));
    }

    @Test
    public void testSkipRange_Collection() {
        List<Integer> result = N.skipRange(integerList, 1, 3);
        assertEquals(Arrays.asList(1, 4, 5), result);

        Set<Integer> resultSet = N.skipRange(integerList, 1, 3, HashSet::new);
        assertEquals(new HashSet<>(Arrays.asList(1, 4, 5)), resultSet);
    }

    @Test
    public void testHasDuplicates_boolean() {
        boolean[] noDups = { true, false };
        boolean[] hasDups = { true, true, false };

        assertFalse(N.hasDuplicates(noDups));
        assertTrue(N.hasDuplicates(hasDups));
        assertFalse(N.hasDuplicates(new boolean[] {}));
    }

    @Test
    public void testHasDuplicates_char() {
        char[] noDups = { 'a', 'b', 'c' };
        char[] hasDups = { 'a', 'b', 'a' };

        assertFalse(N.hasDuplicates(noDups));
        assertTrue(N.hasDuplicates(hasDups));
        assertFalse(N.hasDuplicates(new char[] {}));

        char[] sortedDups = { 'a', 'a', 'b' };
        assertTrue(N.hasDuplicates(sortedDups, true));
    }

    @Test
    public void testHasDuplicates_byte() {
        byte[] noDups = { 1, 2, 3 };
        byte[] hasDups = { 1, 2, 1 };

        assertFalse(N.hasDuplicates(noDups));
        assertTrue(N.hasDuplicates(hasDups));
        assertFalse(N.hasDuplicates(new byte[] {}));

        byte[] sortedDups = { 1, 1, 2 };
        assertTrue(N.hasDuplicates(sortedDups, true));
    }

    @Test
    public void testHasDuplicates_short() {
        short[] noDups = { 1, 2, 3 };
        short[] hasDups = { 1, 2, 1 };

        assertFalse(N.hasDuplicates(noDups));
        assertTrue(N.hasDuplicates(hasDups));
        assertFalse(N.hasDuplicates(new short[] {}));

        short[] sortedDups = { 1, 1, 2 };
        assertTrue(N.hasDuplicates(sortedDups, true));
    }

    @Test
    public void testHasDuplicates_int() {
        int[] noDups = { 1, 2, 3 };
        int[] hasDups = { 1, 2, 1 };

        assertFalse(N.hasDuplicates(noDups));
        assertTrue(N.hasDuplicates(hasDups));
        assertFalse(N.hasDuplicates(new int[] {}));

        int[] sortedDups = { 1, 1, 2 };
        assertTrue(N.hasDuplicates(sortedDups, true));
    }

    @Test
    public void testHasDuplicates_long() {
        long[] noDups = { 1L, 2L, 3L };
        long[] hasDups = { 1L, 2L, 1L };

        assertFalse(N.hasDuplicates(noDups));
        assertTrue(N.hasDuplicates(hasDups));
        assertFalse(N.hasDuplicates(new long[] {}));

        long[] sortedDups = { 1L, 1L, 2L };
        assertTrue(N.hasDuplicates(sortedDups, true));
    }

    @Test
    public void testHasDuplicates_float() {
        float[] noDups = { 1.0f, 2.0f, 3.0f };
        float[] hasDups = { 1.0f, 2.0f, 1.0f };

        assertFalse(N.hasDuplicates(noDups));
        assertTrue(N.hasDuplicates(hasDups));
        assertFalse(N.hasDuplicates(new float[] {}));

        float[] sortedDups = { 1.0f, 1.0f, 2.0f };
        assertTrue(N.hasDuplicates(sortedDups, true));
    }

    @Test
    public void testHasDuplicates_double() {
        double[] noDups = { 1.0, 2.0, 3.0 };
        double[] hasDups = { 1.0, 2.0, 1.0 };

        assertFalse(N.hasDuplicates(noDups));
        assertTrue(N.hasDuplicates(hasDups));
        assertFalse(N.hasDuplicates(new double[] {}));

        double[] sortedDups = { 1.0, 1.0, 2.0 };
        assertTrue(N.hasDuplicates(sortedDups, true));
    }

    @Test
    public void testHasDuplicates_generic() {
        String[] noDups = { "a", "b", "c" };
        String[] hasDups = { "a", "b", "a" };

        assertFalse(N.hasDuplicates(noDups));
        assertTrue(N.hasDuplicates(hasDups));
        assertFalse(N.hasDuplicates(new String[] {}));

        String[] sortedDups = { "a", "a", "b" };
        assertTrue(N.hasDuplicates(sortedDups, true));
    }

    @Test
    public void testHasDuplicates_Collection() {
        List<String> noDups = Arrays.asList("a", "b", "c");
        List<String> hasDups = Arrays.asList("a", "b", "a");

        assertFalse(N.hasDuplicates(noDups));
        assertTrue(N.hasDuplicates(hasDups));
        assertFalse(N.hasDuplicates(Collections.emptyList()));

        List<String> sortedDups = Arrays.asList("a", "a", "b");
        assertTrue(N.hasDuplicates(sortedDups, true));
    }

    @Test
    public void testRetainAll() {
        Collection<Integer> c = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        Collection<Integer> toKeep = Arrays.asList(2, 4);

        assertTrue(N.retainAll(c, toKeep));
        assertEquals(Arrays.asList(2, 4), c);

        Collection<Integer> empty = new ArrayList<>();
        assertFalse(N.retainAll(empty, toKeep));

        Collection<Integer> c2 = new ArrayList<>(Arrays.asList(1, 2, 3));
        assertTrue(N.retainAll(c2, Collections.emptyList()));
        assertTrue(c2.isEmpty());
    }

    @Test
    public void testSum_char() {
        assertEquals(294, N.sum('a', 'b', 'c'));
        assertEquals(0, N.sum(new char[] {}));

        char[] chars = { 'a', 'b', 'c', 'd', 'e' };
        assertEquals(N.sum('b', 'c', 'd'), N.sum(chars, 1, 4));
    }

    @Test
    public void testSum_byte() {
        assertEquals(15, N.sum(byteArray));
        assertEquals(0, N.sum(new byte[] {}));
        assertEquals(9, N.sum(byteArray, 1, 4));
    }

    @Test
    public void testSum_short() {
        assertEquals(15, N.sum(shortArray));
        assertEquals(0, N.sum(new short[] {}));
        assertEquals(9, N.sum(shortArray, 1, 4));
    }

    @Test
    public void testSum_int() {
        assertEquals(15, N.sum(intArray));
        assertEquals(0, N.sum(new int[] {}));
        assertEquals(9, N.sum(intArray, 1, 4));
    }

    @Test
    public void testSumToLong_int() {
        assertEquals(15L, N.sumToLong(intArray));
        assertEquals(0L, N.sumToLong(new int[] {}));
        assertEquals(9L, N.sumToLong(intArray, 1, 4));
    }

    @Test
    public void testSum_long() {
        assertEquals(15L, N.sum(longArray));
        assertEquals(0L, N.sum(new long[] {}));
        assertEquals(9L, N.sum(longArray, 1, 4));
    }

    @Test
    public void testSum_float() {
        assertEquals(15.0f, N.sum(floatArray), 0.001);
        assertEquals(0f, N.sum(new float[] {}), 0.001);
        assertEquals(9.0f, N.sum(floatArray, 1, 4), 0.001);
    }

    @Test
    public void testSumToDouble_float() {
        assertEquals(15.0, N.sumToDouble(floatArray), 0.001);
        assertEquals(0.0, N.sumToDouble(new float[] {}), 0.001);
        assertEquals(9.0, N.sumToDouble(floatArray, 1, 4), 0.001);
    }

    @Test
    public void testSum_double() {
        assertEquals(15.0, N.sum(doubleArray), 0.001);
        assertEquals(0.0, N.sum(new double[] {}), 0.001);
        assertEquals(9.0, N.sum(doubleArray, 1, 4), 0.001);
    }

    @Test
    public void testAverage_char() {
        char[] chars = { 'a', 'b', 'c' };
        assertEquals(98.0, N.average(chars), 0.001);
        assertEquals(0.0, N.average(new char[] {}), 0.001);
        assertEquals(98.5, N.average(chars, 1, 3), 0.001);
    }

    @Test
    public void testAverage_byte() {
        assertEquals(3.0, N.average(byteArray), 0.001);
        assertEquals(0.0, N.average(new byte[] {}), 0.001);
        assertEquals(3.0, N.average(byteArray, 1, 4), 0.001);
    }

    @Test
    public void testAverage_short() {
        assertEquals(3.0, N.average(shortArray), 0.001);
        assertEquals(0.0, N.average(new short[] {}), 0.001);
        assertEquals(3.0, N.average(shortArray, 1, 4), 0.001);
    }

    @Test
    public void testAverage_int() {
        assertEquals(3.0, N.average(intArray), 0.001);
        assertEquals(0.0, N.average(new int[] {}), 0.001);
        assertEquals(3.0, N.average(intArray, 1, 4), 0.001);
    }

    @Test
    public void testAverage_long() {
        assertEquals(3.0, N.average(longArray), 0.001);
        assertEquals(0.0, N.average(new long[] {}), 0.001);
        assertEquals(3.0, N.average(longArray, 1, 4), 0.001);
    }

    @Test
    public void testAverage_float() {
        assertEquals(3.0, N.average(floatArray), 0.001);
        assertEquals(0.0, N.average(new float[] {}), 0.001);
        assertEquals(3.0, N.average(floatArray, 1, 4), 0.001);
    }

    @Test
    public void testAverage_double() {
        assertEquals(3.0, N.average(doubleArray), 0.001);
        assertEquals(0.0, N.average(new double[] {}), 0.001);
        assertEquals(3.0, N.average(doubleArray, 1, 4), 0.001);
    }

    @Test
    public void testSumInt_array() {
        assertEquals(15, N.sumInt(integerArray));
        assertEquals(9, N.sumInt(integerArray, 1, 4));
        assertEquals(30, N.sumInt(integerArray, x -> x * 2));
        assertEquals(18, N.sumInt(integerArray, 1, 4, x -> x * 2));
    }

    @Test
    public void testSumInt_collection() {
        assertEquals(9, N.sumInt(integerList, 1, 4));
        assertEquals(18, N.sumInt(integerList, 1, 4, x -> x * 2));
    }

    @Test
    public void testSumInt_iterable() {
        assertEquals(15, N.sumInt((Iterable<Integer>) integerList));
        assertEquals(30, N.sumInt((Iterable<Integer>) integerList, x -> x * 2));
    }

    @Test
    public void testSumIntToLong_iterable() {
        assertEquals(15L, N.sumIntToLong((Iterable<Integer>) integerList));
        assertEquals(30L, N.sumIntToLong((Iterable<Integer>) integerList, x -> x * 2));
    }

    @Test
    public void testSumLong_array() {
        Long[] longObjArray = { 1L, 2L, 3L, 4L, 5L };
        assertEquals(15L, N.sumLong(longObjArray));
        assertEquals(9L, N.sumLong(longObjArray, 1, 4));
        assertEquals(30L, N.sumLong(longObjArray, x -> x * 2));
        assertEquals(18L, N.sumLong(longObjArray, 1, 4, x -> x * 2));
    }

    @Test
    public void testSumLong_collection() {
        List<Long> longList = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        assertEquals(9L, N.sumLong(longList, 1, 4));
        assertEquals(18L, N.sumLong(longList, 1, 4, x -> x * 2));
    }

    @Test
    public void testSumLong_iterable() {
        List<Long> longList = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        assertEquals(15L, N.sumLong((Iterable<Long>) longList));
        assertEquals(30L, N.sumLong((Iterable<Long>) longList, x -> x * 2));
    }

    @Test
    public void testSumDouble_array() {
        Double[] doubleObjArray = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        assertEquals(15.0, N.sumDouble(doubleObjArray), 0.001);
        assertEquals(9.0, N.sumDouble(doubleObjArray, 1, 4), 0.001);
        assertEquals(30.0, N.sumDouble(doubleObjArray, x -> x * 2), 0.001);
        assertEquals(18.0, N.sumDouble(doubleObjArray, 1, 4, x -> x * 2), 0.001);
    }

    @Test
    public void testSumDouble_collection() {
        List<Double> doubleList = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        assertEquals(9.0, N.sumDouble(doubleList, 1, 4), 0.001);
        assertEquals(18.0, N.sumDouble(doubleList, 1, 4, x -> x * 2), 0.001);
    }

    @Test
    public void testSumDouble_iterable() {
        List<Double> doubleList = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        assertEquals(15.0, N.sumDouble((Iterable<Double>) doubleList), 0.001);
        assertEquals(30.0, N.sumDouble((Iterable<Double>) doubleList, x -> x * 2), 0.001);
    }

    @Test
    public void testSumBigInteger() {
        List<BigInteger> bigIntList = Arrays.asList(BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3));
        assertEquals(BigInteger.valueOf(6), N.sumBigInteger(bigIntList));
        assertEquals(BigInteger.valueOf(12), N.sumBigInteger(bigIntList, x -> x.multiply(BigInteger.valueOf(2))));

        assertEquals(BigInteger.ZERO, N.sumBigInteger(Collections.emptyList()));
    }

    @Test
    public void testSumBigDecimal() {
        List<BigDecimal> bigDecList = Arrays.asList(BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3));
        assertEquals(BigDecimal.valueOf(6), N.sumBigDecimal(bigDecList));
        assertEquals(BigDecimal.valueOf(12), N.sumBigDecimal(bigDecList, x -> x.multiply(BigDecimal.valueOf(2))));

        assertEquals(BigDecimal.ZERO, N.sumBigDecimal(Collections.emptyList()));
    }

    @Test
    public void testAverageInt_array() {
        assertEquals(3.0, N.averageInt(integerArray), 0.001);
        assertEquals(3.0, N.averageInt(integerArray, 1, 4), 0.001);
        assertEquals(6.0, N.averageInt(integerArray, x -> x * 2), 0.001);
        assertEquals(6.0, N.averageInt(integerArray, 1, 4, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageInt_collection() {
        assertEquals(3.0, N.averageInt(integerList, 1, 4), 0.001);
        assertEquals(6.0, N.averageInt(integerList, 1, 4, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageInt_iterable() {
        assertEquals(3.0, N.averageInt((Iterable<Integer>) integerList), 0.001);
        assertEquals(6.0, N.averageInt((Iterable<Integer>) integerList, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageLong_array() {
        Long[] longObjArray = { 1L, 2L, 3L, 4L, 5L };
        assertEquals(3.0, N.averageLong(longObjArray), 0.001);
        assertEquals(3.0, N.averageLong(longObjArray, 1, 4), 0.001);
        assertEquals(6.0, N.averageLong(longObjArray, x -> x * 2), 0.001);
        assertEquals(6.0, N.averageLong(longObjArray, 1, 4, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageLong_collection() {
        List<Long> longList = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        assertEquals(3.0, N.averageLong(longList, 1, 4), 0.001);
        assertEquals(6.0, N.averageLong(longList, 1, 4, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageLong_iterable() {
        List<Long> longList = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        assertEquals(3.0, N.averageLong((Iterable<Long>) longList), 0.001);
        assertEquals(6.0, N.averageLong((Iterable<Long>) longList, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageDouble_array() {
        Double[] doubleObjArray = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        assertEquals(3.0, N.averageDouble(doubleObjArray), 0.001);
        assertEquals(3.0, N.averageDouble(doubleObjArray, 1, 4), 0.001);
        assertEquals(6.0, N.averageDouble(doubleObjArray, x -> x * 2), 0.001);
        assertEquals(6.0, N.averageDouble(doubleObjArray, 1, 4, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageDouble_collection() {
        List<Double> doubleList = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        assertEquals(3.0, N.averageDouble(doubleList, 1, 4), 0.001);
        assertEquals(6.0, N.averageDouble(doubleList, 1, 4, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageDouble_iterable() {
        List<Double> doubleList = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        assertEquals(3.0, N.averageDouble((Iterable<Double>) doubleList), 0.001);
        assertEquals(6.0, N.averageDouble((Iterable<Double>) doubleList, x -> x * 2), 0.001);
    }

    @Test
    public void testAverageBigInteger() {
        List<BigInteger> bigIntList = Arrays.asList(BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3));
        assertEquals(BigDecimal.valueOf(2), N.averageBigInteger(bigIntList));
        assertEquals(BigDecimal.valueOf(4), N.averageBigInteger(bigIntList, x -> x.multiply(BigInteger.valueOf(2))));

        assertEquals(BigDecimal.ZERO, N.averageBigInteger(Collections.emptyList()));
    }

    @Test
    public void testAverageBigDecimal() {
        List<BigDecimal> bigDecList = Arrays.asList(BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3));
        assertEquals(BigDecimal.valueOf(2), N.averageBigDecimal(bigDecList));
        assertEquals(BigDecimal.valueOf(4), N.averageBigDecimal(bigDecList, x -> x.multiply(BigDecimal.valueOf(2))));

        assertEquals(BigDecimal.ZERO, N.averageBigDecimal(Collections.emptyList()));
    }

    @Test
    public void testMin_primitives() {
        assertEquals('a', N.min('a', 'b'));
        assertEquals((byte) 1, N.min((byte) 1, (byte) 2));
        assertEquals((short) 1, N.min((short) 1, (short) 2));
        assertEquals(1, N.min(1, 2));
        assertEquals(1L, N.min(1L, 2L));
        assertEquals(1.0f, N.min(1.0f, 2.0f), 0.001);
        assertEquals(1.0, N.min(1.0, 2.0), 0.001);
    }

    @Test
    public void testMin_three_primitives() {
        assertEquals('a', N.min('a', 'b', 'c'));
        assertEquals((byte) 1, N.min((byte) 1, (byte) 2, (byte) 3));
        assertEquals((short) 1, N.min((short) 1, (short) 2, (short) 3));
        assertEquals(1, N.min(1, 2, 3));
        assertEquals(1L, N.min(1L, 2L, 3L));
        assertEquals(1.0f, N.min(1.0f, 2.0f, 3.0f), 0.001);
        assertEquals(1.0, N.min(1.0, 2.0, 3.0), 0.001);
    }

    @Test
    public void testMin_comparable() {
        assertEquals("a", N.min("a", "b"));
        assertEquals("a", N.min("a", "b", "c"));

        assertEquals("a", N.min(null, "a"));
        assertEquals("a", N.min("a", null));
    }

    @Test
    public void testMin_withComparator() {
        Comparator<String> reverseComparator = Comparator.reverseOrder();
        assertEquals("b", N.min("a", "b", reverseComparator));
        assertEquals("c", N.min("a", "b", "c", reverseComparator));
    }

    @Test
    public void testMin_array_primitives() {
        assertThrows(IllegalArgumentException.class, () -> N.min(new char[] {}));

        assertEquals('a', N.min(charArray));
        assertEquals('b', N.min(charArray, 1, 4));

        assertEquals((byte) 1, N.min(byteArray));
        assertEquals((byte) 2, N.min(byteArray, 1, 4));

        assertEquals((short) 1, N.min(shortArray));
        assertEquals((short) 2, N.min(shortArray, 1, 4));

        assertEquals(1, N.min(intArray));
        assertEquals(2, N.min(intArray, 1, 4));

        assertEquals(1L, N.min(longArray));
        assertEquals(2L, N.min(longArray, 1, 4));

        assertEquals(1.0f, N.min(floatArray), 0.001);
        assertEquals(2.0f, N.min(floatArray, 1, 4), 0.001);

        assertEquals(1.0, N.min(doubleArray), 0.001);
        assertEquals(2.0, N.min(doubleArray, 1, 4), 0.001);
    }

    @Test
    public void testMin_array_generic() {
        assertThrows(IllegalArgumentException.class, () -> N.min(new String[] {}));

        assertEquals("five", N.min(stringArray));
        assertEquals("four", N.min(stringArray, 1, 4));
        assertEquals("five", N.min(stringArray, Comparator.naturalOrder()));
        assertEquals("two", N.min(stringArray, Comparator.reverseOrder()));
    }

    @Test
    public void testMin_collection() {
        assertThrows(IllegalArgumentException.class, () -> N.min(Collections.emptyList(), 0, 0));

        assertEquals("five", N.min(stringList, 0, 5));
        assertEquals("four", N.min(stringList, 1, 4));
        assertEquals("five", N.min(stringList, 0, 5, Comparator.naturalOrder()));
    }

    @Test
    public void testMin_iterable() {
        assertEquals("five", N.min((Iterable<String>) stringList));
        assertEquals("five", N.min((Iterable<String>) stringList, Comparator.naturalOrder()));

        assertThrows(IllegalArgumentException.class, () -> N.min(Collections.<String> emptyList()));
    }

    @Test
    public void testMin_iterator() {
        assertEquals("five", N.min(stringList.iterator()));
        assertEquals("five", N.min(stringList.iterator(), Comparator.naturalOrder()));

        assertThrows(IllegalArgumentException.class, () -> N.min(Collections.<String> emptyList().iterator()));
    }

    @Test
    public void testMinBy() {
        assertEquals("c", N.minBy(new String[] { "aaa", "bb", "c" }, String::length));
        assertEquals("c", N.minBy(Arrays.asList("aaa", "bb", "c"), String::length));
        assertEquals("c", N.minBy(Arrays.asList("aaa", "bb", "c").iterator(), String::length));
    }

    @Test
    public void testMinAll() {
        String[] arr = { "a", "a", "b", "c" };
        List<String> result = N.minAll(arr);
        assertEquals(Arrays.asList("a", "a"), result);

        List<String> coll = Arrays.asList("a", "a", "b", "c");
        result = N.minAll(coll);
        assertEquals(Arrays.asList("a", "a"), result);

        result = N.minAll(coll.iterator());
        assertEquals(Arrays.asList("a", "a"), result);

        result = N.minAll(arr, Comparator.reverseOrder());
        assertEquals(Arrays.asList("c"), result);
    }

    @Test
    public void testMinOrDefaultIfEmpty() {
        String[] emptyArr = {};
        assertEquals("default", N.minOrDefaultIfEmpty(emptyArr, Fn.identity(), "default"));

        String[] arr = { "aaa", "bb", "c" };
        assertEquals("aaa", N.minOrDefaultIfEmpty(arr, Fn.identity(), "default"));

        List<String> emptyColl = Collections.emptyList();
        assertEquals("default", N.minOrDefaultIfEmpty(emptyColl, Fn.identity(), "default"));

        List<String> coll = Arrays.asList("aaa", "bb", "c");
        assertEquals("aaa", N.minOrDefaultIfEmpty(coll, Fn.identity(), "default"));

        assertEquals("default", N.minOrDefaultIfEmpty(emptyColl.iterator(), Fn.identity(), "default"));
        assertEquals("aaa", N.minOrDefaultIfEmpty(coll.iterator(), Fn.identity(), "default"));
    }

    @Test
    public void testMinIntOrDefaultIfEmpty() {
        Integer[] emptyArr = {};
        assertEquals(10, N.minIntOrDefaultIfEmpty(emptyArr, x -> x * 2, 10));

        Integer[] arr = { 3, 1, 2 };
        assertEquals(2, N.minIntOrDefaultIfEmpty(arr, x -> x * 2, 10));

        List<Integer> emptyColl = Collections.emptyList();
        assertEquals(10, N.minIntOrDefaultIfEmpty(emptyColl, x -> x * 2, 10));

        List<Integer> coll = Arrays.asList(3, 1, 2);
        assertEquals(2, N.minIntOrDefaultIfEmpty(coll, x -> x * 2, 10));

        assertEquals(10, N.minIntOrDefaultIfEmpty(emptyColl.iterator(), x -> x * 2, 10));
        assertEquals(2, N.minIntOrDefaultIfEmpty(coll.iterator(), x -> x * 2, 10));
    }

    @Test
    public void testMinLongOrDefaultIfEmpty() {
        Long[] emptyArr = {};
        assertEquals(10L, N.minLongOrDefaultIfEmpty(emptyArr, x -> x * 2, 10L));

        Long[] arr = { 3L, 1L, 2L };
        assertEquals(2L, N.minLongOrDefaultIfEmpty(arr, x -> x * 2, 10L));

        List<Long> emptyColl = Collections.emptyList();
        assertEquals(10L, N.minLongOrDefaultIfEmpty(emptyColl, x -> x * 2, 10L));

        List<Long> coll = Arrays.asList(3L, 1L, 2L);
        assertEquals(2L, N.minLongOrDefaultIfEmpty(coll, x -> x * 2, 10L));

        assertEquals(10L, N.minLongOrDefaultIfEmpty(emptyColl.iterator(), x -> x * 2, 10L));
        assertEquals(2L, N.minLongOrDefaultIfEmpty(coll.iterator(), x -> x * 2, 10L));
    }

    @Test
    public void testMinDoubleOrDefaultIfEmpty() {
        Double[] emptyArr = {};
        assertEquals(10.0, N.minDoubleOrDefaultIfEmpty(emptyArr, x -> x * 2, 10.0), 0.001);

        Double[] arr = { 3.0, 1.0, 2.0 };
        assertEquals(2.0, N.minDoubleOrDefaultIfEmpty(arr, x -> x * 2, 10.0), 0.001);

        List<Double> emptyColl = Collections.emptyList();
        assertEquals(10.0, N.minDoubleOrDefaultIfEmpty(emptyColl, x -> x * 2, 10.0), 0.001);

        List<Double> coll = Arrays.asList(3.0, 1.0, 2.0);
        assertEquals(2.0, N.minDoubleOrDefaultIfEmpty(coll, x -> x * 2, 10.0), 0.001);

        assertEquals(10.0, N.minDoubleOrDefaultIfEmpty(emptyColl.iterator(), x -> x * 2, 10.0), 0.001);
        assertEquals(2.0, N.minDoubleOrDefaultIfEmpty(coll.iterator(), x -> x * 2, 10.0), 0.001);
    }

    @Test
    public void testMinMax_array() {
        assertThrows(IllegalArgumentException.class, () -> N.minMax(new String[] {}));

        String[] singleElement = { "a" };
        assertEquals(Pair.of("a", "a"), N.minMax(singleElement));

        assertEquals(Pair.of("five", "two"), N.minMax(stringArray));
        assertEquals(Pair.of("five", "two"), N.minMax(stringArray, Comparator.naturalOrder()));
    }

    @Test
    public void testMinMax_iterable() {
        assertThrows(IllegalArgumentException.class, () -> N.minMax(Collections.emptyList()));

        assertEquals(Pair.of("five", "two"), N.minMax((Iterable<String>) stringList));
        assertEquals(Pair.of("five", "two"), N.minMax((Iterable<String>) stringList, Comparator.naturalOrder()));
    }

    @Test
    public void testMinMax_iterator() {
        assertThrows(IllegalArgumentException.class, () -> N.minMax(Collections.<String> emptyList().iterator()));

        assertEquals(Pair.of("five", "two"), N.minMax(stringList.iterator()));
        assertEquals(Pair.of("five", "two"), N.minMax(stringList.iterator(), Comparator.naturalOrder()));
    }

    @Test
    public void testMax_primitives() {
        assertEquals('b', N.max('a', 'b'));
        assertEquals((byte) 2, N.max((byte) 1, (byte) 2));
        assertEquals((short) 2, N.max((short) 1, (short) 2));
        assertEquals(2, N.max(1, 2));
        assertEquals(2L, N.max(1L, 2L));
        assertEquals(2.0f, N.max(1.0f, 2.0f), 0.001);
        assertEquals(2.0, N.max(1.0, 2.0), 0.001);
    }

    @Test
    public void testMax_three_primitives() {
        assertEquals('c', N.max('a', 'b', 'c'));
        assertEquals((byte) 3, N.max((byte) 1, (byte) 2, (byte) 3));
        assertEquals((short) 3, N.max((short) 1, (short) 2, (short) 3));
        assertEquals(3, N.max(1, 2, 3));
        assertEquals(3L, N.max(1L, 2L, 3L));
        assertEquals(3.0f, N.max(1.0f, 2.0f, 3.0f), 0.001);
        assertEquals(3.0, N.max(1.0, 2.0, 3.0), 0.001);
    }

    @Test
    public void testMax_comparable() {
        assertEquals("b", N.max("a", "b"));
        assertEquals("c", N.max("a", "b", "c"));

        assertEquals("a", N.max(null, "a"));
        assertEquals("a", N.max("a", null));
    }

    @Test
    public void testMax_withComparator() {
        Comparator<String> reverseComparator = Comparator.reverseOrder();
        assertEquals("a", N.max("a", "b", reverseComparator));
        assertEquals("a", N.max("a", "b", "c", reverseComparator));
    }

    @Test
    public void testMax_array_primitives() {
        assertThrows(IllegalArgumentException.class, () -> N.max(new char[] {}));

        assertEquals('e', N.max(charArray));
        assertEquals('d', N.max(charArray, 1, 4));

        assertEquals((byte) 5, N.max(byteArray));
        assertEquals((byte) 4, N.max(byteArray, 1, 4));

        assertEquals((short) 5, N.max(shortArray));
        assertEquals((short) 4, N.max(shortArray, 1, 4));

        assertEquals(5, N.max(intArray));
        assertEquals(4, N.max(intArray, 1, 4));

        assertEquals(5L, N.max(longArray));
        assertEquals(4L, N.max(longArray, 1, 4));

        assertEquals(5.0f, N.max(floatArray), 0.001);
        assertEquals(4.0f, N.max(floatArray, 1, 4), 0.001);

        assertEquals(5.0, N.max(doubleArray), 0.001);
        assertEquals(4.0, N.max(doubleArray, 1, 4), 0.001);
    }

    @Test
    public void testMax_array_generic() {
        assertThrows(IllegalArgumentException.class, () -> N.max(new String[] {}));

        assertEquals("two", N.max(stringArray));
        assertEquals("two", N.max(stringArray, 1, 4));
        assertEquals("two", N.max(stringArray, Comparator.naturalOrder()));
        assertEquals("five", N.max(stringArray, Comparator.reverseOrder()));
    }

    @Test
    public void testMax_collection() {
        assertThrows(IllegalArgumentException.class, () -> N.max(Collections.emptyList(), 0, 0));

        assertEquals("two", N.max(stringList, 0, 5));
        assertEquals("two", N.max(stringList, 1, 4));
        assertEquals("two", N.max(stringList, 0, 5, Comparator.naturalOrder()));
    }

    @Test
    public void testMax_iterable() {
        assertEquals("two", N.max((Iterable<String>) stringList));
        assertEquals("two", N.max((Iterable<String>) stringList, Comparator.naturalOrder()));

        assertThrows(IllegalArgumentException.class, () -> N.max(Collections.<String> emptyList()));
    }

    @Test
    public void testMax_iterator() {
        assertEquals("two", N.max(stringList.iterator()));
        assertEquals("two", N.max(stringList.iterator(), Comparator.naturalOrder()));

        assertThrows(IllegalArgumentException.class, () -> N.max(Collections.<String> emptyList().iterator()));
    }

    @Test
    public void testMaxBy() {
        assertEquals("aaa", N.maxBy(new String[] { "aaa", "bb", "c" }, String::length));
        assertEquals("aaa", N.maxBy(Arrays.asList("aaa", "bb", "c"), String::length));
        assertEquals("aaa", N.maxBy(Arrays.asList("aaa", "bb", "c").iterator(), String::length));
    }

    @Test
    public void testMaxAll() {
        String[] arr = { "a", "b", "c", "c" };
        List<String> result = N.maxAll(arr);
        assertEquals(Arrays.asList("c", "c"), result);

        List<String> coll = Arrays.asList("a", "b", "c", "c");
        result = N.maxAll(coll);
        assertEquals(Arrays.asList("c", "c"), result);

        result = N.maxAll(coll.iterator());
        assertEquals(Arrays.asList("c", "c"), result);

        result = N.maxAll(arr, Comparator.reverseOrder());
        assertEquals(Arrays.asList("a"), result);
    }

    @Test
    public void testMaxOrDefaultIfEmpty() {
        String[] emptyArr = {};
        assertEquals("default", N.maxOrDefaultIfEmpty(emptyArr, Fn.identity(), "default"));

        String[] arr = { "a", "bb", "ccc" };
        assertEquals("ccc", N.maxOrDefaultIfEmpty(arr, Fn.identity(), "default"));

        List<String> emptyColl = Collections.emptyList();
        assertEquals("default", N.maxOrDefaultIfEmpty(emptyColl, Fn.identity(), "default"));

        List<String> coll = Arrays.asList("a", "bb", "ccc");
        assertEquals("ccc", N.maxOrDefaultIfEmpty(coll, Fn.identity(), "default"));

        assertEquals("default", N.maxOrDefaultIfEmpty(emptyColl.iterator(), Fn.identity(), "default"));
        assertEquals("ccc", N.maxOrDefaultIfEmpty(coll.iterator(), Fn.identity(), "default"));
    }

    @Test
    public void testMaxIntOrDefaultIfEmpty() {
        Integer[] emptyArr = {};
        assertEquals(10, N.maxIntOrDefaultIfEmpty(emptyArr, x -> x * 2, 10));

        Integer[] arr = { 1, 2, 3 };
        assertEquals(6, N.maxIntOrDefaultIfEmpty(arr, x -> x * 2, 10));

        List<Integer> emptyColl = Collections.emptyList();
        assertEquals(10, N.maxIntOrDefaultIfEmpty(emptyColl, x -> x * 2, 10));

        List<Integer> coll = Arrays.asList(1, 2, 3);
        assertEquals(6, N.maxIntOrDefaultIfEmpty(coll, x -> x * 2, 10));

        assertEquals(10, N.maxIntOrDefaultIfEmpty(emptyColl.iterator(), x -> x * 2, 10));
        assertEquals(6, N.maxIntOrDefaultIfEmpty(coll.iterator(), x -> x * 2, 10));
    }

    @Test
    public void testMaxLongOrDefaultIfEmpty() {
        Long[] emptyArr = {};
        assertEquals(10L, N.maxLongOrDefaultIfEmpty(emptyArr, x -> x * 2, 10L));

        Long[] arr = { 1L, 2L, 3L };
        assertEquals(6L, N.maxLongOrDefaultIfEmpty(arr, x -> x * 2, 10L));

        List<Long> emptyColl = Collections.emptyList();
        assertEquals(10L, N.maxLongOrDefaultIfEmpty(emptyColl, x -> x * 2, 10L));

        List<Long> coll = Arrays.asList(1L, 2L, 3L);
        assertEquals(6L, N.maxLongOrDefaultIfEmpty(coll, x -> x * 2, 10L));

        assertEquals(10L, N.maxLongOrDefaultIfEmpty(emptyColl.iterator(), x -> x * 2, 10L));
        assertEquals(6L, N.maxLongOrDefaultIfEmpty(coll.iterator(), x -> x * 2, 10L));
    }

    @Test
    public void testMaxDoubleOrDefaultIfEmpty() {
        Double[] emptyArr = {};
        assertEquals(10.0, N.maxDoubleOrDefaultIfEmpty(emptyArr, x -> x * 2, 10.0), 0.001);

        Double[] arr = { 1.0, 2.0, 3.0 };
        assertEquals(6.0, N.maxDoubleOrDefaultIfEmpty(arr, x -> x * 2, 10.0), 0.001);

        List<Double> emptyColl = Collections.emptyList();
        assertEquals(10.0, N.maxDoubleOrDefaultIfEmpty(emptyColl, x -> x * 2, 10.0), 0.001);

        List<Double> coll = Arrays.asList(1.0, 2.0, 3.0);
        assertEquals(6.0, N.maxDoubleOrDefaultIfEmpty(coll, x -> x * 2, 10.0), 0.001);

        assertEquals(10.0, N.maxDoubleOrDefaultIfEmpty(emptyColl.iterator(), x -> x * 2, 10.0), 0.001);
        assertEquals(6.0, N.maxDoubleOrDefaultIfEmpty(coll.iterator(), x -> x * 2, 10.0), 0.001);
    }

    @Test
    public void testMedian_three_primitives() {
        assertEquals('b', N.median('a', 'b', 'c'));
        assertEquals('b', N.median('c', 'b', 'a'));

        assertEquals((byte) 2, N.median((byte) 1, (byte) 2, (byte) 3));
        assertEquals((byte) 2, N.median((byte) 3, (byte) 2, (byte) 1));

        assertEquals((short) 2, N.median((short) 1, (short) 2, (short) 3));
        assertEquals((short) 2, N.median((short) 3, (short) 2, (short) 1));

        assertEquals(2, N.median(1, 2, 3));
        assertEquals(2, N.median(3, 2, 1));

        assertEquals(2L, N.median(1L, 2L, 3L));
        assertEquals(2L, N.median(3L, 2L, 1L));

        assertEquals(2.0f, N.median(1.0f, 2.0f, 3.0f), 0.001);
        assertEquals(2.0f, N.median(3.0f, 2.0f, 1.0f), 0.001);

        assertEquals(2.0, N.median(1.0, 2.0, 3.0), 0.001);
        assertEquals(2.0, N.median(3.0, 2.0, 1.0), 0.001);
    }

    @Test
    public void testMedian_three_comparable() {
        assertEquals("b", N.median("a", "b", "c"));
        assertEquals("b", N.median("c", "b", "a"));

        assertEquals("bee", N.median("ant", "bee", "tiger", Comparator.comparing(String::length)));
        assertEquals("ant", N.median("bee", "ant", "tiger", Comparator.comparing(String::length)));
        assertEquals("ant", N.median("bee", "tiger", "ant", Comparator.comparing(String::length)));
        assertEquals("bee", N.median("tiger", "bee", "ant", Comparator.comparing(String::length)));
        assertEquals("ant", N.median("be", "ant", "tiger", Comparator.comparing(String::length)));
        assertEquals("ant", N.median("be", "tiger", "ant", Comparator.comparing(String::length)));
        assertEquals("ant", N.median("tiger", "be", "ant", Comparator.comparing(String::length)));
    }

    @Test
    public void testMedian_three_withComparator() {
        Comparator<String> reverseComparator = Comparator.reverseOrder();
        assertEquals("b", N.median("a", "b", "c", reverseComparator));
        assertEquals("b", N.median("c", "b", "a", reverseComparator));
    }

    @Test
    public void testMedian_array_primitives() {
        assertThrows(IllegalArgumentException.class, () -> N.median(new char[] {}));

        assertEquals('a', N.median(new char[] { 'a' }));
        assertEquals('a', N.median(new char[] { 'a', 'b' }));
        assertEquals('b', N.median(new char[] { 'a', 'b', 'c' }));
        assertEquals('c', N.median(charArray));
        assertEquals('c', N.median(charArray, 1, 4));

        assertEquals((byte) 3, N.median(byteArray));
        assertEquals((byte) 3, N.median(byteArray, 1, 4));

        assertEquals((short) 3, N.median(shortArray));
        assertEquals((short) 3, N.median(shortArray, 1, 4));

        assertEquals(3, N.median(intArray));
        assertEquals(3, N.median(intArray, 1, 4));

        assertEquals(3L, N.median(longArray));
        assertEquals(3L, N.median(longArray, 1, 4));

        assertEquals(3.0f, N.median(floatArray), 0.001);
        assertEquals(3.0f, N.median(floatArray, 1, 4), 0.001);

        assertEquals(3.0, N.median(doubleArray), 0.001);
        assertEquals(3.0, N.median(doubleArray, 1, 4), 0.001);
    }

    @Test
    public void testMedian_array_generic() {
        assertThrows(IllegalArgumentException.class, () -> N.median(new String[] {}));

        String[] sorted = { "a", "b", "c", "d", "e" };
        assertEquals("c", N.median(sorted));
        assertEquals("c", N.median(sorted, 1, 4));
        assertEquals("c", N.median(sorted, Comparator.naturalOrder()));
        assertEquals("c", N.median(sorted, 0, 5, Comparator.naturalOrder()));

        assertEquals("bee", N.median(Array.of("ant", "bee", "tiger"), Comparator.comparing(String::length)));
    }

    @Test
    public void testMedian_collection() {
        assertThrows(IllegalArgumentException.class, () -> N.median(Collections.emptyList()));

        List<String> sorted = Arrays.asList("a", "b", "c", "d", "e");
        assertEquals("c", N.median(sorted));
        assertEquals("c", N.median(sorted, 1, 4));
        assertEquals("c", N.median(sorted, Comparator.naturalOrder()));
        assertEquals("c", N.median(sorted, 0, 5, Comparator.naturalOrder()));
        assertEquals("bee", N.median(CommonUtil.asList("ant", "bee", "tiger"), Comparator.comparing(String::length)));
    }

    @Test
    public void testKthLargest_array_primitives() {
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new char[] {}, 1));

        assertEquals('e', N.kthLargest(charArray, 1));
        assertEquals('a', N.kthLargest(charArray, 5));
        assertEquals('c', N.kthLargest(charArray, 3));
        assertEquals('d', N.kthLargest(charArray, 1, 4, 1));

        assertEquals((byte) 5, N.kthLargest(byteArray, 1));
        assertEquals((byte) 1, N.kthLargest(byteArray, 5));
        assertEquals((byte) 3, N.kthLargest(byteArray, 3));

        assertEquals((short) 5, N.kthLargest(shortArray, 1));
        assertEquals((short) 1, N.kthLargest(shortArray, 5));
        assertEquals((short) 3, N.kthLargest(shortArray, 3));

        assertEquals(5, N.kthLargest(intArray, 1));
        assertEquals(1, N.kthLargest(intArray, 5));
        assertEquals(3, N.kthLargest(intArray, 3));

        assertEquals(5L, N.kthLargest(longArray, 1));
        assertEquals(1L, N.kthLargest(longArray, 5));
        assertEquals(3L, N.kthLargest(longArray, 3));

        assertEquals(5.0f, N.kthLargest(floatArray, 1), 0.001);
        assertEquals(1.0f, N.kthLargest(floatArray, 5), 0.001);
        assertEquals(3.0f, N.kthLargest(floatArray, 3), 0.001);

        assertEquals(5.0, N.kthLargest(doubleArray, 1), 0.001);
        assertEquals(1.0, N.kthLargest(doubleArray, 5), 0.001);
        assertEquals(3.0, N.kthLargest(doubleArray, 3), 0.001);
    }

    @Test
    public void testKthLargest_array_generic() {
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(new String[] {}, 1));

        String[] arr = { "a", "b", "c", "d", "e" };
        assertEquals("e", N.kthLargest(arr, 1));
        assertEquals("a", N.kthLargest(arr, 5));
        assertEquals("c", N.kthLargest(arr, 3));
        assertEquals("d", N.kthLargest(arr, 1, 4, 1));
        assertEquals("e", N.kthLargest(arr, 1, Comparator.naturalOrder()));
        assertEquals("c", N.kthLargest(arr, 0, 5, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testKthLargest_collection() {
        assertThrows(IllegalArgumentException.class, () -> N.kthLargest(Collections.emptyList(), 1));

        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        assertEquals("e", N.kthLargest(list, 1));
        assertEquals("a", N.kthLargest(list, 5));
        assertEquals("c", N.kthLargest(list, 3));
        assertEquals("d", N.kthLargest(list, 1, 4, 1));
        assertEquals("e", N.kthLargest(list, 1, Comparator.naturalOrder()));
        assertEquals("c", N.kthLargest(list, 0, 5, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testTop_array_primitives() {
        assertThrows(IllegalArgumentException.class, () -> N.top(shortArray, -1));

        assertArrayEquals(new short[] {}, N.top(new short[] {}, 3));
        assertArrayEquals(new short[] {}, N.top(shortArray, 0));
        assertArrayEquals(shortArray.clone(), N.top(shortArray, 10));
        assertArrayEquals(new short[] { 3, 4, 5 }, N.top(shortArray, 3));
        assertArrayEquals(new short[] { 3, 4 }, N.top(shortArray, 1, 4, 2));
        assertArrayEquals(new short[] { 3, 4, 5 }, N.top(shortArray, 3, Comparator.naturalOrder()));
        assertArrayEquals(new short[] { 3, 4 }, N.top(shortArray, 1, 4, 2, Comparator.naturalOrder()));

        assertArrayEquals(new int[] { 3, 4, 5 }, N.top(intArray, 3));
        assertArrayEquals(new long[] { 3, 4, 5 }, N.top(longArray, 3));
        assertArrayEquals(new float[] { 3, 4, 5 }, N.top(floatArray, 3));
        assertArrayEquals(new double[] { 3, 4, 5 }, N.top(doubleArray, 3));
    }

    @Test
    public void testTop_array_generic() {
        assertThrows(IllegalArgumentException.class, () -> N.top(stringArray, -1));

        List<String> result = N.top(stringArray, 3);
        assertEquals(3, result.size());
        assertTrue(result.contains("two"));
        assertTrue(result.contains("three"));

        result = N.top(stringArray, 1, 4, 2);
        assertEquals(2, result.size());

        result = N.top(stringArray, 3, Comparator.naturalOrder());
        assertEquals(3, result.size());

        String[] arr = { "d", "b", "e", "a", "c" };
        result = N.top(arr, 3, true);
        assertEquals(Arrays.asList("d", "e", "c"), result);

        result = N.top(arr, 3, Comparator.naturalOrder(), true);
        assertEquals(Arrays.asList("d", "e", "c"), result);

        result = N.top(arr, 1, 4, 2, true);
        assertEquals(Arrays.asList("b", "e"), result);

        result = N.top(arr, 1, 4, 2, Comparator.naturalOrder(), true);
        assertEquals(Arrays.asList("b", "e"), result);
    }

    @Test
    public void testTop_collection() {
        assertThrows(IllegalArgumentException.class, () -> N.top(stringList, -1));

        List<String> result = N.top(stringList, 3);
        assertEquals(3, result.size());
        assertTrue(result.contains("two"));
        assertTrue(result.contains("three"));

        result = N.top(stringList, 1, 4, 2);
        assertEquals(2, result.size());

        result = N.top(stringList, 3, Comparator.naturalOrder());
        assertEquals(3, result.size());

        List<String> list = Arrays.asList("d", "b", "e", "a", "c");
        result = N.top(list, 3, true);
        assertEquals(Arrays.asList("d", "e", "c"), result);

        result = N.top(list, 3, Comparator.naturalOrder(), true);
        assertEquals(Arrays.asList("d", "e", "c"), result);

        result = N.top(list, 1, 4, 2, true);
        assertEquals(Arrays.asList("b", "e"), result);

        result = N.top(list, 1, 4, 2, Comparator.naturalOrder(), true);
        assertEquals(Arrays.asList("b", "e"), result);
    }

    @Test
    public void testPercentiles_primitives() {
        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted(new char[] {}));

        char[] sortedChars = { 'a', 'b', 'c', 'd', 'e' };
        Map<Percentage, Character> charPercentiles = N.percentilesOfSorted(sortedChars);
        assertNotNull(charPercentiles);
        assertEquals(Percentage.values().length, charPercentiles.size());

        byte[] sortedBytes = { 1, 2, 3, 4, 5 };
        Map<Percentage, Byte> bytePercentiles = N.percentilesOfSorted(sortedBytes);
        assertNotNull(bytePercentiles);
        assertEquals(Percentage.values().length, bytePercentiles.size());

        short[] sortedShorts = { 1, 2, 3, 4, 5 };
        Map<Percentage, Short> shortPercentiles = N.percentilesOfSorted(sortedShorts);
        assertNotNull(shortPercentiles);
        assertEquals(Percentage.values().length, shortPercentiles.size());

        int[] sortedInts = new int[100];
        for (int i = 0; i < 100; i++) {
            sortedInts[i] = i + 1;
        }
        Map<Percentage, Integer> intPercentiles = N.percentilesOfSorted(sortedInts);
        assertNotNull(intPercentiles);
        assertEquals(2, intPercentiles.get(Percentage._1).intValue());
        assertEquals(51, intPercentiles.get(Percentage._50).intValue());
        assertEquals(100, intPercentiles.get(Percentage._99).intValue());

        long[] sortedLongs = { 1L, 2L, 3L, 4L, 5L };
        Map<Percentage, Long> longPercentiles = N.percentilesOfSorted(sortedLongs);
        assertNotNull(longPercentiles);
        assertEquals(Percentage.values().length, longPercentiles.size());

        float[] sortedFloats = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        Map<Percentage, Float> floatPercentiles = N.percentilesOfSorted(sortedFloats);
        assertNotNull(floatPercentiles);
        assertEquals(Percentage.values().length, floatPercentiles.size());

        double[] sortedDoubles = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        Map<Percentage, Double> doublePercentiles = N.percentilesOfSorted(sortedDoubles);
        assertNotNull(doublePercentiles);
        assertEquals(Percentage.values().length, doublePercentiles.size());
    }

    @Test
    public void testPercentiles_generic() {
        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted(new String[] {}));

        String[] sortedStrings = { "a", "b", "c", "d", "e" };
        Map<Percentage, String> stringPercentiles = N.percentilesOfSorted(sortedStrings);
        assertNotNull(stringPercentiles);
        assertEquals(Percentage.values().length, stringPercentiles.size());
    }

    @Test
    public void testPercentiles_list() {
        assertThrows(IllegalArgumentException.class, () -> N.percentilesOfSorted(Collections.emptyList()));

        List<String> sortedList = Arrays.asList("a", "b", "c", "d", "e");
        Map<Percentage, String> listPercentiles = N.percentilesOfSorted(sortedList);
        assertNotNull(listPercentiles);
        assertEquals(Percentage.values().length, listPercentiles.size());
    }
}
