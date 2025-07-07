package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.BooleanPredicate;
import com.landawn.abacus.util.function.BooleanUnaryOperator;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.ByteUnaryOperator;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.CharUnaryOperator;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.FloatUnaryOperator;
import com.landawn.abacus.util.function.IntToBooleanFunction;
import com.landawn.abacus.util.function.IntToByteFunction;
import com.landawn.abacus.util.function.IntToCharFunction;
import com.landawn.abacus.util.function.IntToFloatFunction;
import com.landawn.abacus.util.function.IntToShortFunction;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortUnaryOperator;

public class N201Test extends TestBase {

    // Helper for comparing float arrays due to potential precision issues if directly using assertArrayEquals
    public void assertFloatArrayEquals(float[] expected, float[] actual, float delta) {
        assertEquals(expected.length, actual.length, "Array lengths differ");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], delta, "Array elements at index " + i + " differ");
        }
    }

    // Helper for comparing double arrays due to potential precision issues
    public void assertDoubleArrayEquals(double[] expected, double[] actual, double delta) {
        assertEquals(expected.length, actual.length, "Array lengths differ");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], delta, "Array elements at index " + i + " differ");
        }
    }

    // region replaceIf tests
    @Test
    public void testReplaceIfBooleanArray() {
        boolean[] arr = { true, false, true, false };
        BooleanPredicate predicate = b -> b; // replace true values
        int replacements = N.replaceIf(arr, predicate, false);
        assertArrayEquals(new boolean[] { false, false, false, false }, arr);
        assertEquals(2, replacements);

        arr = new boolean[] { true, false, true, false };
        BooleanPredicate predicateFalse = b -> !b; // replace false values
        replacements = N.replaceIf(arr, predicateFalse, true);
        assertArrayEquals(new boolean[] { true, true, true, true }, arr);
        assertEquals(2, replacements);

        assertEquals(0, N.replaceIf((boolean[]) null, predicate, true));
        assertEquals(0, N.replaceIf(new boolean[0], predicate, true));
    }

    @Test
    public void testReplaceIfCharArray() {
        char[] arr = { 'a', 'b', 'a', 'c' };
        CharPredicate predicate = c -> c == 'a';
        int replacements = N.replaceIf(arr, predicate, 'x');
        assertArrayEquals(new char[] { 'x', 'b', 'x', 'c' }, arr);
        assertEquals(2, replacements);

        assertEquals(0, N.replaceIf((char[]) null, predicate, 'x'));
        assertEquals(0, N.replaceIf(new char[0], predicate, 'x'));
    }

    @Test
    public void testReplaceIfByteArray() {
        byte[] arr = { 1, 2, 1, 3 };
        BytePredicate predicate = b -> b == 1;
        int replacements = N.replaceIf(arr, predicate, (byte) 5);
        assertArrayEquals(new byte[] { 5, 2, 5, 3 }, arr);
        assertEquals(2, replacements);

        assertEquals(0, N.replaceIf((byte[]) null, predicate, (byte) 5));
        assertEquals(0, N.replaceIf(new byte[0], predicate, (byte) 5));
    }

    @Test
    public void testReplaceIfShortArray() {
        short[] arr = { 10, 20, 10, 30 };
        ShortPredicate predicate = s -> s == 10;
        int replacements = N.replaceIf(arr, predicate, (short) 50);
        assertArrayEquals(new short[] { 50, 20, 50, 30 }, arr);
        assertEquals(2, replacements);

        assertEquals(0, N.replaceIf((short[]) null, predicate, (short) 50));
        assertEquals(0, N.replaceIf(new short[0], predicate, (short) 50));
    }

    @Test
    public void testReplaceIfIntArray() {
        int[] arr = { 100, 200, 100, 300 };
        IntPredicate predicate = i -> i == 100;
        int replacements = N.replaceIf(arr, predicate, 500);
        assertArrayEquals(new int[] { 500, 200, 500, 300 }, arr);
        assertEquals(2, replacements);

        assertEquals(0, N.replaceIf((int[]) null, predicate, 500));
        assertEquals(0, N.replaceIf(new int[0], predicate, 500));
    }

    @Test
    public void testReplaceIfLongArray() {
        long[] arr = { 1000L, 2000L, 1000L, 3000L };
        LongPredicate predicate = l -> l == 1000L;
        int replacements = N.replaceIf(arr, predicate, 5000L);
        assertArrayEquals(new long[] { 5000L, 2000L, 5000L, 3000L }, arr);
        assertEquals(2, replacements);

        assertEquals(0, N.replaceIf((long[]) null, predicate, 5000L));
        assertEquals(0, N.replaceIf(new long[0], predicate, 5000L));
    }

    @Test
    public void testReplaceIfFloatArray() {
        float[] arr = { 1.0f, 2.0f, 1.0f, 3.0f };
        FloatPredicate predicate = f -> f == 1.0f;
        int replacements = N.replaceIf(arr, predicate, 5.0f);
        assertFloatArrayEquals(new float[] { 5.0f, 2.0f, 5.0f, 3.0f }, arr, 0.001f);
        assertEquals(2, replacements);

        assertEquals(0, N.replaceIf((float[]) null, predicate, 5.0f));
        assertEquals(0, N.replaceIf(new float[0], predicate, 5.0f));
    }

    @Test
    public void testReplaceIfDoubleArray() {
        double[] arr = { 1.0, 2.0, 1.0, 3.0 };
        DoublePredicate predicate = d -> d == 1.0;
        int replacements = N.replaceIf(arr, predicate, 5.0);
        assertDoubleArrayEquals(new double[] { 5.0, 2.0, 5.0, 3.0 }, arr, 0.001);
        assertEquals(2, replacements);

        assertEquals(0, N.replaceIf((double[]) null, predicate, 5.0));
        assertEquals(0, N.replaceIf(new double[0], predicate, 5.0));
    }

    @Test
    public void testReplaceIfGenericArray() {
        String[] arr = { "apple", "banana", "apple", "cherry" };
        Predicate<String> predicate = s -> "apple".equals(s);
        int replacements = N.replaceIf(arr, predicate, "orange");
        assertArrayEquals(new String[] { "orange", "banana", "orange", "cherry" }, arr);
        assertEquals(2, replacements);

        Integer[] intArr = { 1, null, 1, 3 };
        Predicate<Integer> nullPredicate = Objects::isNull;
        replacements = N.replaceIf(intArr, nullPredicate, 0);
        assertArrayEquals(new Integer[] { 1, 0, 1, 3 }, intArr);
        assertEquals(1, replacements);

        assertEquals(0, N.replaceIf((String[]) null, predicate, "x"));
        assertEquals(0, N.replaceIf(new String[0], predicate, "x"));
    }

    @Test
    public void testReplaceIfList() {
        List<String> list = new ArrayList<>(Arrays.asList("apple", "banana", "apple", "cherry"));
        Predicate<String> predicate = s -> "apple".equals(s);
        int replacements = N.replaceIf(list, predicate, "orange");
        assertEquals(Arrays.asList("orange", "banana", "orange", "cherry"), list);
        assertEquals(2, replacements);

        List<Integer> intList = new LinkedList<>(Arrays.asList(1, null, 1, 3)); // Test non-RandomAccess
        Predicate<Integer> nullPredicate = Objects::isNull;
        replacements = N.replaceIf(intList, nullPredicate, 0);
        assertEquals(Arrays.asList(1, 0, 1, 3), intList);
        assertEquals(1, replacements);

        assertEquals(0, N.replaceIf((List<String>) null, predicate, "x"));
        assertEquals(0, N.replaceIf(new ArrayList<String>(), predicate, "x"));
    }
    // endregion

    // region replaceAll (oldVal, newVal) tests
    @Test
    public void testReplaceAllBooleanArray() {
        boolean[] arr = { true, false, true, true };
        int replacements = N.replaceAll(arr, true, false);
        assertArrayEquals(new boolean[] { false, false, false, false }, arr);
        assertEquals(3, replacements);

        assertEquals(0, N.replaceAll((boolean[]) null, true, false));
        assertEquals(0, N.replaceAll(new boolean[0], true, false));
    }

    @Test
    public void testReplaceAllCharArray() {
        char[] arr = { 'a', 'b', 'a', 'a' };
        int replacements = N.replaceAll(arr, 'a', 'x');
        assertArrayEquals(new char[] { 'x', 'b', 'x', 'x' }, arr);
        assertEquals(3, replacements);

        assertEquals(0, N.replaceAll((char[]) null, 'a', 'x'));
        assertEquals(0, N.replaceAll(new char[0], 'a', 'x'));
    }

    @Test
    public void testReplaceAllByteArray() {
        byte[] arr = { 1, 2, 1, 1 };
        int replacements = N.replaceAll(arr, (byte) 1, (byte) 5);
        assertArrayEquals(new byte[] { 5, 2, 5, 5 }, arr);
        assertEquals(3, replacements);

        assertEquals(0, N.replaceAll((byte[]) null, (byte) 1, (byte) 5));
        assertEquals(0, N.replaceAll(new byte[0], (byte) 1, (byte) 5));
    }

    @Test
    public void testReplaceAllShortArray() {
        short[] arr = { 10, 20, 10, 10 };
        int replacements = N.replaceAll(arr, (short) 10, (short) 50);
        assertArrayEquals(new short[] { 50, 20, 50, 50 }, arr);
        assertEquals(3, replacements);

        assertEquals(0, N.replaceAll((short[]) null, (short) 10, (short) 50));
        assertEquals(0, N.replaceAll(new short[0], (short) 10, (short) 50));
    }

    @Test
    public void testReplaceAllIntArray() {
        int[] arr = { 100, 200, 100, 100 };
        int replacements = N.replaceAll(arr, 100, 500);
        assertArrayEquals(new int[] { 500, 200, 500, 500 }, arr);
        assertEquals(3, replacements);

        assertEquals(0, N.replaceAll((int[]) null, 100, 500));
        assertEquals(0, N.replaceAll(new int[0], 100, 500));
    }

    @Test
    public void testReplaceAllLongArray() {
        long[] arr = { 1000L, 2000L, 1000L, 1000L };
        int replacements = N.replaceAll(arr, 1000L, 5000L);
        assertArrayEquals(new long[] { 5000L, 2000L, 5000L, 5000L }, arr);
        assertEquals(3, replacements);

        assertEquals(0, N.replaceAll((long[]) null, 1000L, 5000L));
        assertEquals(0, N.replaceAll(new long[0], 1000L, 5000L));
    }

    @Test
    public void testReplaceAllFloatArray() {
        float[] arr = { 1.0f, 2.0f, 1.0f, 1.0f, Float.NaN, Float.NaN };
        int replacements = N.replaceAll(arr, 1.0f, 5.0f);
        assertFloatArrayEquals(new float[] { 5.0f, 2.0f, 5.0f, 5.0f, Float.NaN, Float.NaN }, arr, 0.001f);
        assertEquals(3, replacements);

        replacements = N.replaceAll(arr, Float.NaN, 7.0f); // NaN comparison uses Float.compare
        assertFloatArrayEquals(new float[] { 5.0f, 2.0f, 5.0f, 5.0f, 7.0f, 7.0f }, arr, 0.001f);
        assertEquals(2, replacements);

        assertEquals(0, N.replaceAll((float[]) null, 1.0f, 5.0f));
        assertEquals(0, N.replaceAll(new float[0], 1.0f, 5.0f));
    }

    @Test
    public void testReplaceAllDoubleArray() {
        double[] arr = { 1.0, 2.0, 1.0, 1.0, Double.NaN, Double.NaN };
        int replacements = N.replaceAll(arr, 1.0, 5.0);
        assertDoubleArrayEquals(new double[] { 5.0, 2.0, 5.0, 5.0, Double.NaN, Double.NaN }, arr, 0.001);
        assertEquals(3, replacements);

        replacements = N.replaceAll(arr, Double.NaN, 7.0); // NaN comparison uses Double.compare
        assertDoubleArrayEquals(new double[] { 5.0, 2.0, 5.0, 5.0, 7.0, 7.0 }, arr, 0.001);
        assertEquals(2, replacements);

        assertEquals(0, N.replaceAll((double[]) null, 1.0, 5.0));
        assertEquals(0, N.replaceAll(new double[0], 1.0, 5.0));
    }

    @Test
    public void testReplaceAllGenericArray() {
        String[] arr = { "apple", "banana", "apple", "apple", null };
        int replacements = N.replaceAll(arr, "apple", "orange");
        assertArrayEquals(new String[] { "orange", "banana", "orange", "orange", null }, arr);
        assertEquals(3, replacements);

        replacements = N.replaceAll(arr, null, "grape");
        assertArrayEquals(new String[] { "orange", "banana", "orange", "orange", "grape" }, arr);
        assertEquals(1, replacements);

        assertEquals(0, N.replaceAll((String[]) null, "a", "x"));
        assertEquals(0, N.replaceAll(new String[0], "a", "x"));
    }

    @Test
    public void testReplaceAllList() {
        List<String> list = new ArrayList<>(Arrays.asList("apple", "banana", "apple", "apple", null));
        int replacements = N.replaceAll(list, "apple", "orange");
        assertEquals(Arrays.asList("orange", "banana", "orange", "orange", null), list);
        assertEquals(3, replacements);

        replacements = N.replaceAll(list, null, "grape");
        assertEquals(Arrays.asList("orange", "banana", "orange", "orange", "grape"), list);
        assertEquals(1, replacements);

        // Test with LinkedList (non-RandomAccess)
        List<Integer> linkedList = new LinkedList<>(Arrays.asList(1, 2, 1, 1, null));
        replacements = N.replaceAll(linkedList, 1, 5);
        assertEquals(Arrays.asList(5, 2, 5, 5, null), linkedList);
        assertEquals(3, replacements);

        replacements = N.replaceAll(linkedList, null, 0);
        assertEquals(Arrays.asList(5, 2, 5, 5, 0), linkedList);
        assertEquals(1, replacements);

        assertEquals(0, N.replaceAll((List<String>) null, "a", "x"));
        assertEquals(0, N.replaceAll(new ArrayList<String>(), "a", "x"));
    }
    // endregion

    // region replaceAll (UnaryOperator) tests
    @Test
    public void testReplaceAllBooleanArrayWithOperator() {
        boolean[] arr = { true, false, true };
        BooleanUnaryOperator operator = b -> !b;
        N.replaceAll(arr, operator);
        assertArrayEquals(new boolean[] { false, true, false }, arr);

        N.replaceAll((boolean[]) null, operator); // Should not throw
        N.replaceAll(new boolean[0], operator); // Should not throw
    }

    @Test
    public void testReplaceAllCharArrayWithOperator() {
        char[] arr = { 'a', 'b', 'c' };
        CharUnaryOperator operator = c -> (char) (c + 1);
        N.replaceAll(arr, operator);
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, arr);

        N.replaceAll((char[]) null, operator);
        N.replaceAll(new char[0], operator);
    }

    @Test
    public void testReplaceAllByteArrayWithOperator() {
        byte[] arr = { 1, 2, 3 };
        ByteUnaryOperator operator = b -> (byte) (b * 2);
        N.replaceAll(arr, operator);
        assertArrayEquals(new byte[] { 2, 4, 6 }, arr);

        N.replaceAll((byte[]) null, operator);
        N.replaceAll(new byte[0], operator);
    }

    @Test
    public void testReplaceAllShortArrayWithOperator() {
        short[] arr = { 10, 20, 30 };
        ShortUnaryOperator operator = s -> (short) (s / 2);
        N.replaceAll(arr, operator);
        assertArrayEquals(new short[] { 5, 10, 15 }, arr);

        N.replaceAll((short[]) null, operator);
        N.replaceAll(new short[0], operator);
    }

    @Test
    public void testReplaceAllIntArrayWithOperator() {
        int[] arr = { 1, 2, 3 };
        IntUnaryOperator operator = i -> i * i;
        N.replaceAll(arr, operator);
        assertArrayEquals(new int[] { 1, 4, 9 }, arr);

        N.replaceAll((int[]) null, operator);
        N.replaceAll(new int[0], operator);
    }

    @Test
    public void testReplaceAllLongArrayWithOperator() {
        long[] arr = { 10L, 20L, 30L };
        LongUnaryOperator operator = l -> l - 5L;
        N.replaceAll(arr, operator);
        assertArrayEquals(new long[] { 5L, 15L, 25L }, arr);

        N.replaceAll((long[]) null, operator);
        N.replaceAll(new long[0], operator);
    }

    @Test
    public void testReplaceAllFloatArrayWithOperator() {
        float[] arr = { 1.0f, 2.0f, 3.0f };
        FloatUnaryOperator operator = f -> f * 1.5f;
        N.replaceAll(arr, operator);
        assertFloatArrayEquals(new float[] { 1.5f, 3.0f, 4.5f }, arr, 0.001f);

        N.replaceAll((float[]) null, operator);
        N.replaceAll(new float[0], operator);
    }

    @Test
    public void testReplaceAllDoubleArrayWithOperator() {
        double[] arr = { 1.0, 2.0, 3.0 };
        DoubleUnaryOperator operator = d -> d / 2.0;
        N.replaceAll(arr, operator);
        assertDoubleArrayEquals(new double[] { 0.5, 1.0, 1.5 }, arr, 0.001);

        N.replaceAll((double[]) null, operator);
        N.replaceAll(new double[0], operator);
    }

    @Test
    public void testReplaceAllGenericArrayWithOperator() {
        String[] arr = { "a", "b", "c" };
        UnaryOperator<String> operator = s -> s.toUpperCase();
        N.replaceAll(arr, operator);
        assertArrayEquals(new String[] { "A", "B", "C" }, arr);

        N.replaceAll((String[]) null, operator);
        N.replaceAll(new String[0], operator);
    }

    @Test
    public void testReplaceAllListWithOperator() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        UnaryOperator<String> operator = s -> s.toUpperCase();
        N.replaceAll(list, operator);
        assertEquals(Arrays.asList("A", "B", "C"), list);

        List<Integer> linkedList = new LinkedList<>(Arrays.asList(1, 2, 3)); // Test non-RandomAccess
        UnaryOperator<Integer> intOp = i -> i * 2;
        N.replaceAll(linkedList, intOp);
        assertEquals(Arrays.asList(2, 4, 6), linkedList);

        N.replaceAll((List<String>) null, operator);
        N.replaceAll(new ArrayList<String>(), operator);
    }
    // endregion

    // region updateAll (Throwables.UnaryOperator) tests
    @Test
    public void testUpdateAllArray() throws IOException {
        String[] arr = { "a", "b" };
        Throwables.UnaryOperator<String, IOException> operator = s -> {
            if ("b".equals(s))
                throw new IOException("Test Exception");
            return s.toUpperCase();
        };

        assertThrows(IOException.class, () -> N.updateAll(arr, operator));
        // arr[0] would be "A", arr[1] would be "b" (original due to exception)
        assertEquals("A", arr[0]);
        assertEquals("b", arr[1]);

        String[] arr2 = { "c", "d" };
        Throwables.UnaryOperator<String, IOException> noExceptionOp = String::toUpperCase;
        N.updateAll(arr2, noExceptionOp);
        assertArrayEquals(new String[] { "C", "D" }, arr2);

        N.updateAll((String[]) null, noExceptionOp);
        N.updateAll(new String[0], noExceptionOp);
    }

    @Test
    public void testUpdateAllList() throws IOException {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        Throwables.UnaryOperator<String, IOException> operator = s -> {
            if ("b".equals(s))
                throw new IOException("Test Exception on List");
            return s.toUpperCase();
        };

        assertThrows(IOException.class, () -> N.updateAll(list, operator));
        assertEquals("A", list.get(0));
        assertEquals("b", list.get(1));

        List<String> list2 = new LinkedList<>(Arrays.asList("c", "d")); // Non-RandomAccess
        Throwables.UnaryOperator<String, IOException> noExceptionOp = String::toUpperCase;
        N.updateAll(list2, noExceptionOp);
        assertEquals(Arrays.asList("C", "D"), list2);

        N.updateAll((List<String>) null, noExceptionOp);
        N.updateAll(new ArrayList<String>(), noExceptionOp);
    }
    // endregion

    // region Deprecated update methods
    @Test
    public void testUpdateAllUsingReplaceAllInstead() {
        assertThrows(UnsupportedOperationException.class, N::updateAllUsingReplaceAllInstead);
    }

    @Test
    public void testUpdateIfUsingReplaceIfInstead() {
        assertThrows(UnsupportedOperationException.class, N::updateIfUsingReplaceIfInstead);
    }
    // endregion

    // region setAll (generator) tests
    @Test
    public void testSetAllBooleanArray() {
        boolean[] arr = new boolean[3];
        IntToBooleanFunction generator = i -> i % 2 == 0;
        N.setAll(arr, generator);
        assertArrayEquals(new boolean[] { true, false, true }, arr);

        N.setAll((boolean[]) null, generator);
        N.setAll(new boolean[0], generator);
    }

    @Test
    public void testSetAllCharArray() {
        char[] arr = new char[3];
        IntToCharFunction generator = i -> (char) ('a' + i);
        N.setAll(arr, generator);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, arr);

        N.setAll((char[]) null, generator);
        N.setAll(new char[0], generator);
    }

    @Test
    public void testSetAllByteArray() {
        byte[] arr = new byte[3];
        IntToByteFunction generator = i -> (byte) (i + 1);
        N.setAll(arr, generator);
        assertArrayEquals(new byte[] { 1, 2, 3 }, arr);

        N.setAll((byte[]) null, generator);
        N.setAll(new byte[0], generator);
    }

    @Test
    public void testSetAllShortArray() {
        short[] arr = new short[3];
        IntToShortFunction generator = i -> (short) ((i + 1) * 10);
        N.setAll(arr, generator);
        assertArrayEquals(new short[] { 10, 20, 30 }, arr);

        N.setAll((short[]) null, generator);
        N.setAll(new short[0], generator);
    }

    @Test
    public void testSetAllIntArray() {
        int[] arr = new int[3];
        IntUnaryOperator generator = i -> (i + 1) * 100; // N.setAll delegates to Arrays.setAll
        N.setAll(arr, generator);
        assertArrayEquals(new int[] { 100, 200, 300 }, arr);

        N.setAll((int[]) null, generator);
        N.setAll(new int[0], generator);
    }

    @Test
    public void testSetAllLongArray() {
        long[] arr = new long[3];
        IntToLongFunction generator = i -> (i + 1) * 1000L; // N.setAll delegates to Arrays.setAll
        N.setAll(arr, generator);
        assertArrayEquals(new long[] { 1000L, 2000L, 3000L }, arr);

        N.setAll((long[]) null, generator);
        N.setAll(new long[0], generator);
    }

    @Test
    public void testSetAllFloatArray() {
        float[] arr = new float[3];
        IntToFloatFunction generator = i -> (i + 1) * 1.5f;
        N.setAll(arr, generator);
        assertFloatArrayEquals(new float[] { 1.5f, 3.0f, 4.5f }, arr, 0.001f);

        N.setAll((float[]) null, generator);
        N.setAll(new float[0], generator);
    }

    @Test
    public void testSetAllDoubleArray() {
        double[] arr = new double[3];
        IntToDoubleFunction generator = i -> (i + 1) * 1.1; // N.setAll delegates to Arrays.setAll
        N.setAll(arr, generator);
        assertDoubleArrayEquals(new double[] { 1.1, 2.2, 3.3 }, arr, 0.001);

        N.setAll((double[]) null, generator);
        N.setAll(new double[0], generator);
    }

    @Test
    public void testSetAllGenericArray() {
        String[] arr = new String[3];
        IntFunction<String> generator = i -> "Val" + i; // N.setAll delegates to Arrays.setAll
        N.setAll(arr, generator);
        assertArrayEquals(new String[] { "Val0", "Val1", "Val2" }, arr);

        N.setAll((String[]) null, generator);
        N.setAll(new String[0], generator);
    }

    @Test
    public void testSetAllList() {
        List<String> list = new ArrayList<>(Arrays.asList("x", "y", "z"));
        IntFunction<String> generator = i -> "Item" + i;
        N.setAll(list, generator);
        assertEquals(Arrays.asList("Item0", "Item1", "Item2"), list);

        List<Integer> linkedList = new LinkedList<>(Arrays.asList(0, 0, 0)); // Non-RandomAccess
        IntFunction<Integer> intGen = i -> i * 10;
        N.setAll(linkedList, intGen);
        assertEquals(Arrays.asList(0, 10, 20), linkedList);

        N.setAll((List<String>) null, generator);
        N.setAll(new ArrayList<String>(), generator);
    }

    @Test
    public void testSetAllGenericArrayWithConverter() throws IOException {
        String[] arr = { "a", "b", "c" };
        Throwables.IntObjFunction<String, String, IOException> converter = (idx, val) -> {
            if ("b".equals(val))
                throw new IOException("Converter Exception");
            return val.toUpperCase() + idx;
        };
        assertThrows(IOException.class, () -> N.setAll(arr, converter));
        assertEquals("A0", arr[0]); // "a" becomes "A0"
        assertEquals("b", arr[1]); // "b" causes exception, remains "b"
        assertEquals("c", arr[2]); // "c" is not processed

        String[] arr2 = { "x", "y" };
        Throwables.IntObjFunction<String, String, IOException> noExConverter = (idx, val) -> val.toUpperCase() + idx;
        N.setAll(arr2, noExConverter);
        assertArrayEquals(new String[] { "X0", "Y1" }, arr2);

        N.setAll((String[]) null, noExConverter);
        N.setAll(new String[0], noExConverter);
    }

    @Test
    public void testSetAllListWithConverter() throws IOException {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Throwables.IntObjFunction<String, String, IOException> converter = (idx, val) -> {
            if ("b".equals(val) && idx == 1)
                throw new IOException("Converter Exception on List");
            return val.toUpperCase() + idx;
        };
        assertThrows(IOException.class, () -> N.setAll(list, converter));
        assertEquals("A0", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));

        List<String> list2 = new LinkedList<>(Arrays.asList("x", "y")); // Non-RandomAccess
        Throwables.IntObjFunction<String, String, IOException> noExConverter = (idx, val) -> val.toUpperCase() + idx;
        N.setAll(list2, noExConverter);
        assertEquals(Arrays.asList("X0", "Y1"), list2);

        N.setAll((List<String>) null, noExConverter);
        N.setAll(new ArrayList<String>(), noExConverter);
    }
    // endregion

    // region copyThenSetAll tests
    @Test
    public void testCopyThenSetAllArrayWithGenerator() {
        String[] original = { "a", "b", "c" };
        String[] originalCopy = Arrays.copyOf(original, original.length); // To check original is unchanged
        IntFunction<String> generator = i -> "New" + i;

        String[] result = N.copyThenSetAll(original, generator);
        assertArrayEquals(originalCopy, original, "Original array should not be modified");
        assertArrayEquals(new String[] { "New0", "New1", "New2" }, result);

        assertNull(N.copyThenSetAll((String[]) null, generator));
        assertArrayEquals(new String[0], N.copyThenSetAll(new String[0], generator));
    }

    @Test
    public void testCopyThenSetAllArrayWithConverter() throws IOException {
        String[] original = { "a", "b", "c" };
        String[] originalCopy = Arrays.copyOf(original, original.length);
        Throwables.IntObjFunction<String, String, IOException> converter = (idx, val) -> val.toUpperCase() + idx;

        String[] result = N.copyThenSetAll(original, converter);
        assertArrayEquals(originalCopy, original, "Original array should not be modified");
        assertArrayEquals(new String[] { "A0", "B1", "C2" }, result);

        Throwables.IntObjFunction<String, String, IOException> exConverter = (idx, val) -> {
            if (idx == 1)
                throw new IOException("Test");
            return val;
        };
        assertThrows(IOException.class, () -> N.copyThenSetAll(original, exConverter));

        assertNull(N.copyThenSetAll((String[]) null, converter));
        assertArrayEquals(new String[0], N.copyThenSetAll(new String[0], converter));
    }
    // endregion

    // region copyThenReplaceAll tests
    @Test
    public void testCopyThenReplaceAllArray() {
        String[] original = { "a", "b", "a" };
        String[] originalCopy = Arrays.copyOf(original, original.length);
        UnaryOperator<String> operator = s -> "a".equals(s) ? "x" : s;

        String[] result = N.copyThenReplaceAll(original, operator);
        assertArrayEquals(originalCopy, original, "Original array should not be modified");
        assertArrayEquals(new String[] { "x", "b", "x" }, result);

        assertNull(N.copyThenReplaceAll(null, operator));
        assertArrayEquals(new String[0], N.copyThenReplaceAll(new String[0], operator));
    }
    // endregion

    // region copyThenUpdateAll tests
    @Test
    public void testCopyThenUpdateAllArray() throws IOException {
        String[] original = { "a", "b", "c" };
        String[] originalCopy = Arrays.copyOf(original, original.length);
        Throwables.UnaryOperator<String, IOException> operator = s -> s.toUpperCase();

        String[] result = N.copyThenUpdateAll(original, operator);
        assertArrayEquals(originalCopy, original, "Original array should not be modified");
        assertArrayEquals(new String[] { "A", "B", "C" }, result);

        Throwables.UnaryOperator<String, IOException> exOperator = s -> {
            if ("b".equals(s))
                throw new IOException("Test");
            return s;
        };
        assertThrows(IOException.class, () -> N.copyThenUpdateAll(original, exOperator));

        assertNull(N.copyThenUpdateAll(null, operator));
        assertArrayEquals(new String[0], N.copyThenUpdateAll(new String[0], operator));
    }
    // endregion

    // region add (single element) tests
    @Test
    public void testAddBoolean() {
        assertArrayEquals(new boolean[] { true }, N.add(new boolean[0], true));
        assertArrayEquals(new boolean[] { false, true }, N.add(new boolean[] { false }, true));
        assertArrayEquals(new boolean[] { true }, N.add((boolean[]) null, true)); // Based on isEmpty behavior
    }

    @Test
    public void testAddChar() {
        assertArrayEquals(new char[] { 'a' }, N.add(new char[0], 'a'));
        assertArrayEquals(new char[] { 'x', 'a' }, N.add(new char[] { 'x' }, 'a'));
        assertArrayEquals(new char[] { 'a' }, N.add((char[]) null, 'a'));
    }

    @Test
    public void testAddByte() {
        assertArrayEquals(new byte[] { 1 }, N.add(new byte[0], (byte) 1));
        assertArrayEquals(new byte[] { 5, 1 }, N.add(new byte[] { 5 }, (byte) 1));
        assertArrayEquals(new byte[] { 1 }, N.add((byte[]) null, (byte) 1));
    }

    @Test
    public void testAddShort() {
        assertArrayEquals(new short[] { 10 }, N.add(new short[0], (short) 10));
        assertArrayEquals(new short[] { 50, 10 }, N.add(new short[] { 50 }, (short) 10));
        assertArrayEquals(new short[] { 10 }, N.add((short[]) null, (short) 10));
    }

    @Test
    public void testAddInt() {
        assertArrayEquals(new int[] { 100 }, N.add(new int[0], 100));
        assertArrayEquals(new int[] { 500, 100 }, N.add(new int[] { 500 }, 100));
        assertArrayEquals(new int[] { 100 }, N.add((int[]) null, 100));
    }

    @Test
    public void testAddLong() {
        assertArrayEquals(new long[] { 1000L }, N.add(new long[0], 1000L));
        assertArrayEquals(new long[] { 5000L, 1000L }, N.add(new long[] { 5000L }, 1000L));
        assertArrayEquals(new long[] { 1000L }, N.add((long[]) null, 1000L));
    }

    @Test
    public void testAddFloat() {
        assertFloatArrayEquals(new float[] { 1.5f }, N.add(new float[0], 1.5f), 0.0f);
        assertFloatArrayEquals(new float[] { 5.5f, 1.5f }, N.add(new float[] { 5.5f }, 1.5f), 0.0f);
        assertFloatArrayEquals(new float[] { 1.5f }, N.add((float[]) null, 1.5f), 0.0f);
    }

    @Test
    public void testAddDouble() {
        assertDoubleArrayEquals(new double[] { 1.5 }, N.add(new double[0], 1.5), 0.0);
        assertDoubleArrayEquals(new double[] { 5.5, 1.5 }, N.add(new double[] { 5.5 }, 1.5), 0.0);
        assertDoubleArrayEquals(new double[] { 1.5 }, N.add((double[]) null, 1.5), 0.0);
    }

    @Test
    public void testAddStringArray() {
        assertArrayEquals(new String[] { "a" }, N.add(new String[0], "a"));
        assertArrayEquals(new String[] { "x", "a" }, N.add(new String[] { "x" }, "a"));
        assertArrayEquals(new String[] { "a" }, N.add((String[]) null, "a"));
    }

    @Test
    public void testAddGenericArray() {
        assertArrayEquals(new Integer[] { 1 }, N.add(new Integer[0], 1));
        assertArrayEquals(new Integer[] { 5, 1 }, N.add(new Integer[] { 5 }, 1));
        assertThrows(IllegalArgumentException.class, () -> N.add((Integer[]) null, 1)); // @NotNull
    }
    // endregion

    // region addAll (varargs/Iterable/Iterator) tests
    @Test
    public void testAddAllBooleanArray() {
        assertArrayEquals(new boolean[] { true, false }, N.addAll(new boolean[0], true, false));
        assertArrayEquals(new boolean[] { true, true, false }, N.addAll(new boolean[] { true }, true, false));
        assertArrayEquals(new boolean[] { true }, N.addAll(new boolean[] { true })); // empty elementsToAdd
        assertArrayEquals(new boolean[0], N.addAll(new boolean[0]));
        assertArrayEquals(new boolean[] { true, false }, N.addAll((boolean[]) null, true, false));
    }

    @Test
    public void testAddAllCharArray() {
        assertArrayEquals(new char[] { 'a', 'b' }, N.addAll(new char[0], 'a', 'b'));
        assertArrayEquals(new char[] { 'x', 'a', 'b' }, N.addAll(new char[] { 'x' }, 'a', 'b'));
        assertArrayEquals(new char[] { 'x' }, N.addAll(new char[] { 'x' }));
        assertArrayEquals(new char[0], N.addAll(new char[0]));
        assertArrayEquals(new char[] { 'a', 'b' }, N.addAll((char[]) null, 'a', 'b'));
    }

    // Similar tests for byte, short, int, long, float, double, String arrays
    @Test
    public void testAddAllIntArray() {
        assertArrayEquals(new int[] { 1, 2 }, N.addAll(new int[0], 1, 2));
        assertArrayEquals(new int[] { 0, 1, 2 }, N.addAll(new int[] { 0 }, 1, 2));
        assertArrayEquals(new int[] { 1, 2 }, N.addAll((int[]) null, 1, 2));
    }

    @Test
    public void testAddAllGenericArray() {
        assertArrayEquals(new Integer[] { 1, 2 }, N.addAll(new Integer[0], 1, 2));
        assertArrayEquals(new Integer[] { 0, 1, 2 }, N.addAll(new Integer[] { 0 }, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> N.addAll((Integer[]) null, 1, 2));

        Integer[] arr = { 1 };
        Integer[] result = N.addAll(arr, (Integer[]) null); // elementsToAdd is null
        assertArrayEquals(new Integer[] { 1 }, result); // based on how `copy` handles null src

        Integer[] arr2 = {};
        Integer[] result2 = N.addAll(arr2, (Integer[]) null);
        assertArrayEquals(new Integer[] {}, result2); //
    }

    @Test
    public void testAddAllToCollectionVarArgs() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a"));
        assertTrue(N.addAll(coll, "b", "c"));
        assertEquals(Arrays.asList("a", "b", "c"), coll);
        assertFalse(N.addAll(coll)); // no elements to add
        assertThrows(IllegalArgumentException.class, () -> N.addAll((Collection<String>) null, "a"));
    }

    @Test
    public void testAddAllToCollectionIterable() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a"));
        assertTrue(N.addAll(coll, Arrays.asList("b", "c")));
        assertEquals(Arrays.asList("a", "b", "c"), coll);
        assertFalse(N.addAll(coll, (Iterable<String>) null));
        assertFalse(N.addAll(coll, new ArrayList<String>()));
        assertThrows(IllegalArgumentException.class, () -> N.addAll((Collection<String>) null, Arrays.asList("a")));
    }

    @Test
    public void testAddAllToCollectionIterator() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a"));
        assertTrue(N.addAll(coll, Arrays.asList("b", "c").iterator()));
        assertEquals(Arrays.asList("a", "b", "c"), coll);
        assertFalse(N.addAll(coll, (Iterator<String>) null));
        assertFalse(N.addAll(coll, new ArrayList<String>().iterator()));
        assertThrows(IllegalArgumentException.class, () -> N.addAll((Collection<String>) null, Arrays.asList("a").iterator()));
    }
    // endregion

    // region insert tests
    @Test
    public void testInsertBoolean() {
        assertArrayEquals(new boolean[] { true }, N.insert(new boolean[0], 0, true));
        assertArrayEquals(new boolean[] { true, false }, N.insert(new boolean[] { false }, 0, true));
        assertArrayEquals(new boolean[] { false, true }, N.insert(new boolean[] { false }, 1, true));
        assertArrayEquals(new boolean[] { false, true, true }, N.insert(new boolean[] { false, true }, 1, true));
        assertThrows(IndexOutOfBoundsException.class, () -> N.insert(new boolean[] { true }, 2, false));
        assertThrows(IndexOutOfBoundsException.class, () -> N.insert(new boolean[] { true }, -1, false));
    }

    // Similar for char, byte, short, int, long, float, double, String arrays
    @Test
    public void testInsertInt() {
        assertArrayEquals(new int[] { 5, 1, 2 }, N.insert(new int[] { 1, 2 }, 0, 5));
        assertArrayEquals(new int[] { 1, 5, 2 }, N.insert(new int[] { 1, 2 }, 1, 5));
        assertArrayEquals(new int[] { 1, 2, 5 }, N.insert(new int[] { 1, 2 }, 2, 5));
    }

    @Test
    public void testInsertGenericArray() {
        assertArrayEquals(new Integer[] { 5, 1, 2 }, N.insert(new Integer[] { 1, 2 }, 0, 5));
        assertArrayEquals(new Integer[] { 1, 5, 2 }, N.insert(new Integer[] { 1, 2 }, 1, 5));
        assertThrows(IllegalArgumentException.class, () -> N.insert((Integer[]) null, 0, 1));
    }

    @Test
    public void testInsertString() {
        assertEquals("ab", N.insert("b", 0, "a"));
        assertEquals("ba", N.insert("b", 1, "a"));
        assertEquals("hello world", N.insert("helloworld", 5, " "));
        assertEquals("test", N.insert("test", 0, ""));
        assertEquals("test", N.insert("test", 4, null)); // nullToEmpty for strToInsert
        assertEquals("abc", N.insert("", 0, "abc"));
        assertThrows(IndexOutOfBoundsException.class, () -> N.insert("abc", 4, "d"));
    }
    // endregion

    // region insertAll tests
    @Test
    public void testInsertAllBooleanArray() {
        assertArrayEquals(new boolean[] { true, false, true, false }, N.insertAll(new boolean[] { true, false }, 0, true, false));
        assertArrayEquals(new boolean[] { true, true, false, false }, N.insertAll(new boolean[] { true, false }, 1, true, false));
        assertArrayEquals(new boolean[] { true, false, true, false }, N.insertAll(new boolean[] { true, false }, 2, true, false));
        assertArrayEquals(new boolean[] { true, false }, N.insertAll(new boolean[0], 0, true, false));
    }

    // Similar for other primitive types and String
    @Test
    public void testInsertAllIntArray() {
        assertArrayEquals(new int[] { 5, 6, 1, 2, 3 }, N.insertAll(new int[] { 1, 2, 3 }, 0, 5, 6));
        assertArrayEquals(new int[] { 1, 5, 6, 2, 3 }, N.insertAll(new int[] { 1, 2, 3 }, 1, 5, 6));
    }

    @Test
    public void testInsertAllGenericArray() {
        assertArrayEquals(new Integer[] { 5, 6, 1, 2, 3 }, N.insertAll(new Integer[] { 1, 2, 3 }, 0, 5, 6));
        assertThrows(IllegalArgumentException.class, () -> N.insertAll((Integer[]) null, 0, 1, 2));
    }

    @Test
    public void testInsertAllListVarArgs() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "d"));
        assertTrue(N.insertAll(list, 1, "b", "c"));
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
        assertFalse(N.insertAll(list, 1)); // empty elements to insert
        assertThrows(IllegalArgumentException.class, () -> N.insertAll((List<String>) null, 0, "a"));
        assertThrows(IndexOutOfBoundsException.class, () -> N.insertAll(list, 10, "x"));
    }

    @Test
    public void testInsertAllListCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "d"));
        assertTrue(N.insertAll(list, 1, Arrays.asList("b", "c")));
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
        assertFalse(N.insertAll(list, 1, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> N.insertAll((List<String>) null, 0, Arrays.asList("a")));
        assertThrows(IndexOutOfBoundsException.class, () -> N.insertAll(list, 10, Arrays.asList("x")));
    }
    // endregion

    // region deleteByIndex tests
    @Test
    public void testDeleteByIndexBoolean() {
        assertArrayEquals(new boolean[] { false }, N.deleteByIndex(new boolean[] { true, false }, 0));
        assertArrayEquals(new boolean[] { true }, N.deleteByIndex(new boolean[] { true, false }, 1));
        assertArrayEquals(new boolean[0], N.deleteByIndex(new boolean[] { true }, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.deleteByIndex(new boolean[] { true }, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.deleteByIndex(new boolean[0], 0));
    }

    // Similar for other primitive types and String
    @Test
    public void testDeleteByIndexInt() {
        assertArrayEquals(new int[] { 2, 3 }, N.deleteByIndex(new int[] { 1, 2, 3 }, 0));
        assertArrayEquals(new int[] { 1, 3 }, N.deleteByIndex(new int[] { 1, 2, 3 }, 1));
        assertArrayEquals(new int[] { 1, 2 }, N.deleteByIndex(new int[] { 1, 2, 3 }, 2));
    }

    @Test
    public void testDeleteByIndexGeneric() {
        assertArrayEquals(new String[] { "b", "c" }, N.deleteByIndex(new String[] { "a", "b", "c" }, 0));
        // The original code has `// checkArgNotNull(a, cs.a);` commented out for primitive arrays.
        // For generic arrays, it's not explicitly checked in deleteByIndex if `a` is null, but `len(a)` would fail.
        // Assuming len(a) or other helpers would throw NPE or IAE if `a` is null.
        // If `a` is non-null but empty, `len(a)` is 0, `checkElementIndex` will throw.
        String[] emptyArr = {};
        assertThrows(IndexOutOfBoundsException.class, () -> N.deleteByIndex(emptyArr, 0));
        String[] nullArr = null;
        assertThrows(IndexOutOfBoundsException.class, () -> N.deleteByIndex(nullArr, 0)); // Due to len(a) internally

    }
    // endregion

    // region deleteAllByIndices tests
    @Test
    public void testDeleteAllByIndicesBoolean() {
        assertArrayEquals(new boolean[] { true, true }, N.deleteAllByIndices(new boolean[] { true, false, true }, 1));
        assertArrayEquals(new boolean[] { false }, N.deleteAllByIndices(new boolean[] { true, false, true }, 0, 2));
        assertArrayEquals(new boolean[] { true, false, true }, N.deleteAllByIndices(new boolean[] { true, false, true })); // no indices
        assertArrayEquals(new boolean[0], N.deleteAllByIndices(new boolean[] { true, false, true }, 0, 1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.deleteAllByIndices(new boolean[0], 0, 1, 2)); // empty array, indices out of bound
        assertArrayEquals(new boolean[0], N.deleteAllByIndices(new boolean[0])); // empty array, no indices

        boolean[] arrToClone = { true, false };
        boolean[] cloned = N.deleteAllByIndices(arrToClone);
        assertNotSame(arrToClone, cloned); // Should be a clone
        assertArrayEquals(arrToClone, cloned);

        assertThrows(IndexOutOfBoundsException.class, () -> N.deleteAllByIndices(new boolean[] { true }, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.deleteAllByIndices(new boolean[] { true }, -1));
    }

    // Similar for other primitive types and String
    @Test
    public void testDeleteAllByIndicesInt() {
        assertArrayEquals(new int[] { 1, 4 }, N.deleteAllByIndices(new int[] { 1, 2, 3, 4 }, 1, 2));
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, N.deleteAllByIndices(new int[] { 1, 2, 3, 4 }));
        assertArrayEquals(new int[] { 4 }, N.deleteAllByIndices(new int[] { 1, 2, 3, 4 }, 0, 1, 2));
        assertArrayEquals(new int[] {}, N.deleteAllByIndices(new int[] { 1, 2, 3, 4 }, 0, 1, 2, 3));
    }

    @Test
    public void testDeleteAllByIndicesGeneric() {
        assertArrayEquals(new String[] { "a", "d" }, N.deleteAllByIndices(new String[] { "a", "b", "c", "d" }, 1, 2));
        String[] arrNull = null;
        // Depending on how N.isEmpty(indices) and N.len(a) behave with null.
        // If indices is empty, it clones 'a'. N.clone(null) might be an issue or specific behavior.
        // The code uses `a == null ? EMPTY_..._ARRAY : a.clone();` for primitives
        // For generic: `isEmpty(a) ? a : a.clone();` if indices are empty
        assertThrows(NullPointerException.class, () -> N.deleteAllByIndices(arrNull, 1, 2));
    }

    @Test
    public void testDeleteAllByIndicesList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        assertTrue(N.deleteAllByIndices(list, 1, 3)); // delete "b", "d"
        assertEquals(Arrays.asList("a", "c", "e"), list);

        List<String> list2 = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e")); // Test LinkedList
        assertTrue(N.deleteAllByIndices(list2, 0, 4)); // delete "a", "e"
        assertEquals(Arrays.asList("b", "c", "d"), list2);

        assertFalse(N.deleteAllByIndices(list, new int[0])); // no indices
        assertThrows(IllegalArgumentException.class, () -> N.deleteAllByIndices((List<String>) null, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.deleteAllByIndices(list, 10));

        List<Integer> intList = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        N.deleteAllByIndices(intList, 0, 0, 1); // Duplicate indices
        assertEquals(Arrays.asList(3, 4, 5), intList);

    }
    // endregion

    // region remove (single value, first occurrence) tests
    @Test
    public void testRemoveBoolean() {
        assertArrayEquals(new boolean[] { false }, N.remove(new boolean[] { true, false }, true));
        assertArrayEquals(new boolean[] { true, true }, N.remove(new boolean[] { true, false, true }, false));
        assertArrayEquals(new boolean[] { true }, N.remove(new boolean[] { true }, false)); // value not found
        assertArrayEquals(new boolean[0], N.remove(new boolean[0], true));
        assertArrayEquals(new boolean[0], N.remove(null, true));
    }

    // Similar for other primitive types and String
    @Test
    public void testRemoveInt() {
        assertArrayEquals(new int[] { 2, 3, 1 }, N.remove(new int[] { 1, 2, 3, 1 }, 1));
        assertArrayEquals(new int[] { 1, 3 }, N.remove(new int[] { 1, 2, 3 }, 2));
    }

    @Test
    public void testRemoveGenericArray() {
        assertArrayEquals(new String[] { "b", "c", "a" }, N.remove(new String[] { "a", "b", "c", "a" }, "a"));
        assertArrayEquals(new String[] { "a", "b" }, N.remove(new String[] { "a", "b" }, "c")); // not found
        String[] arrNull = null;
        assertArrayEquals(N.EMPTY_STRING_ARRAY, N.remove(arrNull, "a")); // returns a if empty or null
        String[] emptyArr = {};
        assertArrayEquals(N.EMPTY_STRING_ARRAY, N.remove(emptyArr, "a"));
    }

    @Test
    public void testRemoveFromCollection() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "a", "c"));
        assertTrue(N.remove(coll, "a"));
        assertEquals(Arrays.asList("b", "a", "c"), coll);
        assertFalse(N.remove(coll, "d")); // not found
        assertFalse(N.remove(new ArrayList<String>(), "a"));
        assertFalse(N.remove((Collection<String>) null, "a"));
    }
    // endregion

    // region removeAll (multiple values, all occurrences) tests
    @Test
    public void testRemoveAllBooleanArray() {
        assertArrayEquals(new boolean[] { false }, N.removeAll(new boolean[] { true, false, true }, true)); // single value to remove
        assertArrayEquals(new boolean[0], N.removeAll(new boolean[] { true, false, true }, true, false));
        assertArrayEquals(new boolean[] { true, false }, N.removeAll(new boolean[] { true, false }, new boolean[0])); // no values to remove
        assertArrayEquals(new boolean[0], N.removeAll(new boolean[0], true));
        assertArrayEquals(new boolean[0], N.removeAll((boolean[]) null, true));
    }

    // Similar for other primitive types and String
    @Test
    public void testRemoveAllIntArray() {
        assertArrayEquals(new int[] { 3 }, N.removeAll(new int[] { 1, 2, 1, 3, 2 }, 1, 2));
        assertArrayEquals(new int[] { 1, 2, 3 }, N.removeAll(new int[] { 1, 2, 3 }, 4, 5)); // none to remove
    }

    @Test
    public void testRemoveAllGenericArray() {
        assertArrayEquals(new String[] { "c" }, N.removeAll(new String[] { "a", "b", "a", "c", "b" }, "a", "b"));
        String[] arr = { "a", "b" };
        String[] result = N.removeAll(arr, "x", "y");
        assertNotSame(arr, result); // Should be a clone if no removal
        assertArrayEquals(arr, result);

        String[] arrNull = null;
        assertArrayEquals(N.EMPTY_STRING_ARRAY, N.removeAll(arrNull, "a"));
        String[] emptyArr = {};
        assertArrayEquals(N.EMPTY_STRING_ARRAY, N.removeAll(emptyArr, "a"));
    }

    @Test
    public void testRemoveAllFromCollectionVarArgs() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "a", "c", "b", "d"));
        assertTrue(N.removeAll(coll, "a", "b"));
        assertEquals(Arrays.asList("c", "d"), coll);
        assertFalse(N.removeAll(coll, "x", "y")); // none to remove
        assertFalse(N.removeAll(new ArrayList<String>(), "a"));
        assertFalse(N.removeAll(coll)); // no varargs
    }

    @Test
    public void testRemoveAllFromCollectionIterable() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "a", "c", "b", "d"));
        assertTrue(N.removeAll(coll, Arrays.asList("a", "b")));
        assertEquals(Arrays.asList("c", "d"), coll);
        assertFalse(N.removeAll(coll, Arrays.asList("x", "y")));
        assertFalse(N.removeAll(coll, (Iterable<String>) null));

        // Test with HashSet for specific path
        Collection<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        assertTrue(N.removeAll(set, Arrays.asList("a", "x"))); // remove "a", "x" is not there
        assertEquals(new HashSet<>(Arrays.asList("b", "c")), set);
    }

    @Test
    public void testRemoveAllFromCollectionIterator() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "a", "c", "b", "d"));
        assertTrue(N.removeAll(coll, Arrays.asList("a", "b").iterator()));
        assertEquals(Arrays.asList("c", "d"), coll);
        assertFalse(N.removeAll(coll, Arrays.asList("x", "y").iterator()));
        assertFalse(N.removeAll(coll, (Iterator<String>) null));

        // Test with Set for specific path
        Collection<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        assertTrue(N.removeAll(set, Arrays.asList("a", "x").iterator()));
        assertEquals(new HashSet<>(Arrays.asList("b", "c")), set);
    }
    // endregion

    // region removeAllOccurrences (single value, all its occurrences)
    @Test
    public void testRemoveAllOccurrencesBoolean() {
        assertArrayEquals(new boolean[] { false }, N.removeAllOccurrences(new boolean[] { true, false, true }, true));
        assertArrayEquals(new boolean[] { true, true }, N.removeAllOccurrences(new boolean[] { true, false, true }, false));
        assertArrayEquals(new boolean[0], N.removeAllOccurrences(new boolean[0], true));
        assertArrayEquals(new boolean[0], N.removeAllOccurrences(null, true));
    }

    // Similar for other primitives and String
    @Test
    public void testRemoveAllOccurrencesInt() {
        assertArrayEquals(new int[] { 2, 3 }, N.removeAllOccurrences(new int[] { 1, 2, 1, 3, 1 }, 1));
    }

    @Test
    public void testRemoveAllOccurrencesGenericArray() {
        assertArrayEquals(new String[] { "b", "c" }, N.removeAllOccurrences(new String[] { "a", "b", "a", "c", "a" }, "a"));
        String[] arrNull = null;
        assertArrayEquals(N.EMPTY_STRING_ARRAY, N.removeAllOccurrences(arrNull, "a"));
    }

    @Test
    public void testRemoveAllOccurrencesFromCollection() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "a", "c", "a"));
        assertTrue(N.removeAllOccurrences(coll, "a"));
        assertEquals(Arrays.asList("b", "c"), coll);
        assertFalse(N.removeAllOccurrences(coll, "d"));
        assertFalse(N.removeAllOccurrences(new ArrayList<String>(), "a"));
    }
    // endregion

    // region removeDuplicates tests
    @Test
    public void testRemoveDuplicatesBooleanArrayDeprecated() {
        assertArrayEquals(new boolean[] { true, false }, N.removeDuplicates(new boolean[] { true, false, true }));
        assertArrayEquals(new boolean[] { true }, N.removeDuplicates(new boolean[] { true, true, true }));
        assertArrayEquals(new boolean[0], N.removeDuplicates(new boolean[0]));
        assertArrayEquals(new boolean[0], N.removeDuplicates((boolean[]) null));
    }

    @Test
    public void testRemoveDuplicatesCharArray() {
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, N.removeDuplicates(new char[] { 'a', 'b', 'a', 'c', 'b' })); // unsorted
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, N.removeDuplicates(new char[] { 'a', 'a', 'b', 'c', 'c' }, true)); // sorted
        assertArrayEquals(new char[] { 'a' }, N.removeDuplicates(new char[] { 'a', 'a', 'a' }));
        assertArrayEquals(new char[0], N.removeDuplicates(new char[0]));
        assertArrayEquals(new char[0], N.removeDuplicates((char[]) null));
    }

    @Test
    public void testRemoveDuplicatesCharArrayIsSorted() {
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, N.removeDuplicates(new char[] { 'a', 'b', 'a', 'c', 'b' }, false));
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, N.removeDuplicates(new char[] { 'a', 'a', 'b', 'c', 'c' }, true));
    }

    @Test
    public void testRemoveDuplicatesCharArrayRangeIsSorted() {
        assertArrayEquals(new char[] { 'b', 'a' }, N.removeDuplicates(new char[] { 'x', 'b', 'b', 'a', 'y' }, 1, 4, false)); // b,b,a -> b,a
        assertArrayEquals(new char[] { 'a', 'b' }, N.removeDuplicates(new char[] { 'x', 'a', 'a', 'b', 'y' }, 1, 4, true)); // a,a,b -> a,b
        assertArrayEquals(new char[0], N.removeDuplicates(new char[] { 'a', 'b' }, 1, 1, true));
        assertArrayEquals(new char[] { 'b' }, N.removeDuplicates(new char[] { 'a', 'b', 'c' }, 1, 2, false));
    }

    // Similar tests for byte, short, int, long, float, double arrays.
    // Only showing one example for brevity:
    @Test
    public void testRemoveDuplicatesIntArray() {
        assertArrayEquals(new int[] { 1, 2, 3 }, N.removeDuplicates(new int[] { 1, 2, 1, 3, 2 })); // unsorted
        assertArrayEquals(new int[] { 1, 2, 3 }, N.removeDuplicates(new int[] { 1, 1, 2, 3, 3 }, true)); // sorted
    }

    @Test
    public void testRemoveDuplicatesStringArray() {
        assertArrayEquals(new String[] { "a", "b", "c" }, N.removeDuplicates(new String[] { "a", "b", "a", "c", "b" }));
        assertArrayEquals(new String[] { "a", "b", "c" }, N.removeDuplicates(new String[] { "a", "a", "b", "c", "c" }, true));
        String[] arrNull = null;
        assertArrayEquals(N.EMPTY_STRING_ARRAY, N.removeDuplicates(arrNull));
    }

    @Test
    public void testRemoveDuplicatesGenericArray() {
        Integer[] in = { 1, 2, 1, 3, null, 2, null };
        Integer[] expected = { 1, 2, 3, null };
        assertArrayEquals(expected, N.removeDuplicates(in));

        Integer[] inSorted = { 1, 1, 2, 2, 3, null, null };
        Integer[] expectedSorted = { 1, 2, 3, null };
        assertArrayEquals(expectedSorted, N.removeDuplicates(inSorted, true));

        Integer[] arrNull = null;
        assertSame(arrNull, N.removeDuplicates(arrNull));
    }

    @Test
    public void testRemoveDuplicatesCollection() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "a", "c", "b"));
        assertTrue(N.removeDuplicates(coll));
        // Order is preserved due to ArrayList -> distinct -> clear/addAll
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), new HashSet<>(coll)); // Check content
        assertEquals(3, coll.size());

        Collection<String> noDups = new ArrayList<>(Arrays.asList("a", "b", "c"));
        assertFalse(N.removeDuplicates(noDups));
        assertEquals(Arrays.asList("a", "b", "c"), noDups);

        Collection<String> emptyColl = new ArrayList<>();
        assertFalse(N.removeDuplicates(emptyColl));

        Collection<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        assertFalse(N.removeDuplicates(set)); // Set already has no duplicates

        Collection<Integer> sortedList = new LinkedList<>(Arrays.asList(1, 1, 2, 3, 3, 3, 4)); // LinkedList
        assertTrue(N.removeDuplicates(sortedList, true));
        assertEquals(Arrays.asList(1, 2, 3, 4), sortedList);

        Collection<Integer> unsortedList = new LinkedList<>(Arrays.asList(3, 1, 2, 1, 3));
        assertTrue(N.removeDuplicates(unsortedList, false));
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(unsortedList)); // Content check for unsorted
        assertEquals(3, unsortedList.size());

    }
    // endregion
}
