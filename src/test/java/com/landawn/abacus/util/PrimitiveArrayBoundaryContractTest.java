package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

public class PrimitiveArrayBoundaryContractTest extends TestBase {
    static List<Class<?>> types() {
        return List.of(boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class);
    }

    @ParameterizedTest
    @MethodSource("types")
    void multisetArrayOperationsMatchAListReference(Class<?> type) throws Throwable {
        Class<?> arrayType = java.lang.reflect.Array.newInstance(type, 0).getClass();
        List<Object> arrays = Arrays.asList(null, array(type), array(type, 1), array(type, 1, 1, 2), array(type, 1, 2, 3), array(type, 3, 3, 2, 1));
        for (Object left : arrays) {
            for (Object right : arrays) {
                List<Object> a = values(left);
                List<Object> b = values(right);
                List<Object> remaining = new ArrayList<>(b);
                List<Object> difference = new ArrayList<>();
                List<Object> intersection = new ArrayList<>();
                for (Object value : a) {
                    if (remaining.remove(value))
                        intersection.add(value);
                    else
                        difference.add(value);
                }
                List<Object> symmetric = new ArrayList<>(difference);
                // Primitive symmetric difference selects the right-hand surplus from its first occurrences.
                for (Object value : b)
                    if (remaining.remove(value))
                        symmetric.add(value);
                for (String name : List.of("difference", "intersection", "symmetricDifference")) {
                    Method method = N.class.getMethod(name, arrayType, arrayType);
                    List<Object> expected = switch (name) {
                        case "difference" -> difference;
                        case "intersection" -> intersection;
                        default -> symmetric;
                    };
                    assertEquals(expected, values(invoke(method, left, right)), name + ": " + a + " / " + b);
                    assertEquals(a, values(left));
                    assertEquals(b, values(right));
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("types")
    void removalAcceptsUnsortedDuplicateIndicesAndPreservesInputs(Class<?> type) throws Throwable {
        Object input = array(type, 1, 2, 3, 4, 5);
        Class<?> arrayType = input.getClass();
        Method method = N.class.getMethod("removeAt", arrayType, int[].class);
        for (int[] indices : new int[][] { {}, { 0 }, { 4 }, { 3, 1, 1 }, { 4, 0, 2, 2 }, { 0, 1, 2, 3, 4 } }) {
            int[] originalIndices = indices.clone();
            List<Object> expected = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                boolean removed = false;
                for (int index : indices)
                    if (index == i)
                        removed = true;
                if (!removed)
                    expected.add(java.lang.reflect.Array.get(input, i));
            }
            assertEquals(expected, values(invoke(method, input, indices)));
            assertArrayEquals(originalIndices, indices);
            assertEquals(values(array(type, 1, 2, 3, 4, 5)), values(input));
        }
        assertEquals(List.of(), values(invoke(method, null, new int[0])));
        assertThrows(IndexOutOfBoundsException.class, () -> invoke(method, null, new int[] { 0 }));
        assertThrows(IndexOutOfBoundsException.class, () -> invoke(method, input, new int[] { -1, 0 }));
        assertThrows(IndexOutOfBoundsException.class, () -> invoke(method, input, new int[] { 0, 5 }));
    }

    @ParameterizedTest
    @MethodSource("types")
    void comparisonAndMismatchAgreeWithJdkAcrossSmallAndLargeArrays(Class<?> type) throws Throwable {
        Class<?> arrayType = array(type).getClass();
        Method compare = N.class.getMethod("compare", arrayType, arrayType);
        Method mismatch = N.class.getMethod("mismatch", arrayType, arrayType);
        Method jdkCompare = Arrays.class.getMethod("compare", arrayType, arrayType);
        Method jdkMismatch = Arrays.class.getMethod("mismatch", arrayType, arrayType);
        Method rangeCompare = N.class.getMethod("compare", arrayType, int.class, arrayType, int.class, int.class);
        Method rangeMismatch = N.class.getMethod("mismatch", arrayType, int.class, arrayType, int.class, int.class);
        for (int size : new int[] { 0, 1, 7, 33, 1024 }) {
            int[] data = new int[size];
            for (int i = 0; i < size; i++)
                data[i] = i % 5;
            Object a = array(type, data);
            for (int position : new int[] { -1, size / 2, size - 1 }) {
                int[] changed = data.clone();
                if (position >= 0 && position < size)
                    changed[position]++;
                Object b = array(type, changed);
                assertEquals(Integer.signum((int) invoke(jdkCompare, a, b)), Integer.signum((int) invoke(compare, a, b)));
                assertEquals(invoke(jdkMismatch, a, b), invoke(mismatch, a, b));
                assertEquals(Integer.signum((int) invoke(jdkCompare, a, b)), Integer.signum((int) invoke(rangeCompare, a, 0, b, 0, size)));
                assertEquals(invoke(jdkMismatch, a, b), invoke(rangeMismatch, a, 0, b, 0, size));
                assertEquals(0, invoke(rangeCompare, a, 0, a, 0, size));
                assertEquals(-1, invoke(rangeMismatch, a, 0, a, 0, size));
            }
        }
        assertEquals(0, invoke(compare, null, array(type)));
        assertEquals(-1, invoke(compare, null, array(type, 1)));
        assertEquals(1, invoke(compare, array(type, 1), null));
    }

    @ParameterizedTest
    @MethodSource("types")
    void subrangeRotationsMatchTheJdkListReference(Class<?> type) throws Throwable {
        Class<?> arrayType = array(type).getClass();
        Method rotate = N.class.getMethod("rotate", arrayType, int.class, int.class, int.class);
        for (int distance : new int[] { 0, 1, -1, 3, -3, Integer.MIN_VALUE, Integer.MAX_VALUE }) {
            for (int[] range : new int[][] { { 0, 0 }, { 0, 5 }, { 1, 4 }, { 2, 3 } }) {
                Object input = array(type, 1, 2, 3, 4, 5);
                List<Object> expected = values(input);
                Collections.rotate(expected.subList(range[0], range[1]), distance);
                invoke(rotate, input, range[0], range[1], distance);
                assertEquals(expected, values(input));
            }
        }
    }

    @Test
    void charDuplicateScanHandlesSeparatedBitsetWordsAndPigeonholeBoundary() {
        char[] values = new char[128];
        for (int i = 0; i < values.length; i++)
            values[i] = (char) (i * 257);
        assertFalse(N.containsDuplicates(values, false));
        assertFalse(N.containsDuplicates(values, true));
        values[127] = values[0];
        assertTrue(N.containsDuplicates(values, false));
        assertFalse(N.containsDuplicates(values, 1, 127, false));
        assertFalse(N.containsDuplicates(values, true));
        values[126] = values[127];
        assertTrue(N.containsDuplicates(values, true));
        char[] allValues = new char[65536];
        for (int i = 0; i < allValues.length; i++)
            allValues[i] = (char) i;
        assertFalse(N.containsDuplicates(allValues, false));
        assertTrue(N.containsDuplicates(Arrays.copyOf(allValues, 65537), false));
    }

    private static Object array(Class<?> type, int... values) {
        Object array = java.lang.reflect.Array.newInstance(type, values.length);
        for (int i = 0; i < values.length; i++) {
            Object value;
            if (type == boolean.class)
                value = values[i] % 2 != 0;
            else if (type == char.class)
                value = (char) values[i];
            else if (type == byte.class)
                value = (byte) values[i];
            else if (type == short.class)
                value = (short) values[i];
            else if (type == int.class)
                value = values[i];
            else if (type == long.class)
                value = (long) values[i];
            else if (type == float.class)
                value = (float) values[i];
            else
                value = (double) values[i];
            java.lang.reflect.Array.set(array, i, value);
        }
        return array;
    }

    private static List<Object> values(Object array) {
        List<Object> values = new ArrayList<>();
        if (array != null)
            for (int i = 0; i < java.lang.reflect.Array.getLength(array); i++)
                values.add(java.lang.reflect.Array.get(array, i));
        return values;
    }

    private static Object invoke(Method method, Object... args) throws Throwable {
        try {
            return method.invoke(null, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
