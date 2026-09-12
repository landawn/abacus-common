package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.OptionalInt;

public class IndexAllTest extends IndexTestSupport {
    @Test
    public void testAllOf() {
        int[] array = { 1, 2, 1, 3, 1 };
        BitSet expected = new BitSet();
        expected.set(0);
        expected.set(2);
        expected.set(4);
        assertEquals(expected, Index.allOf(array, 1));
    }

    @Test
    public void testAllOfWithPredicate() {
        String[] array = { "apple", "banana", "avocado", "orange" };
        Predicate<String> startsWithA = s -> s.startsWith("a");
        BitSet expected = new BitSet();
        expected.set(0);
        expected.set(2);
        assertEquals(expected, Index.allOf(array, startsWithA));
    }

    @Test
    public void testAllOf_ByteArray() {
        byte[] array = { 1, 2, 1, 2, 1 };

        BitSet result = Index.allOf(array, (byte) 1);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_CharArray() {
        char[] array = { 'a', 'b', 'a', 'b', 'a' };

        BitSet result = Index.allOf(array, 'a');
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_ShortArray() {
        short[] array = { 10, 20, 10, 20, 10 };

        BitSet result = Index.allOf(array, (short) 10);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_IntArray() {
        int[] array = { 100, 200, 100, 200, 100 };

        BitSet result = Index.allOf(array, 100);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_LongArray() {
        long[] array = { 1000L, 2000L, 1000L, 2000L, 1000L };

        BitSet result = Index.allOf(array, 1000L);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_FloatArray() {
        float[] array = { 1.1f, 2.2f, 1.1f, 2.2f, 1.1f };

        BitSet result = Index.allOf(array, 1.1f);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_DoubleArray() {
        double[] array = { 1.11, 2.22, 1.11, 2.22, 1.11 };

        BitSet result = Index.allOf(array, 1.11);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_DoubleArray_WithTolerance() {
        double[] array = { 1.0, 2.001, 1.01, 2.999, 0.99 };

        BitSet result = Index.allOf(array, 1.0, 0, 0.02);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_Collection() {
        List<String> list = Arrays.asList("apple", "banana", "apple", "banana", "apple");

        BitSet result = Index.allOf(list, "apple");
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));

        LinkedList<String> linkedList = new LinkedList<>(list);
        result = Index.allOf(linkedList, "apple");
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_Array_WithPredicate() {
        String[] array = { "apple", "apricot", "banana", "avocado", "cherry" };

        BitSet result = Index.allOf(array, s -> s.startsWith("a"));
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(1));
        Assertions.assertTrue(result.get(3));

        result = Index.allOf(array, s -> s.length() > 10);
        Assertions.assertEquals(0, result.cardinality());
    }

    @Test
    public void testAllOf_Collection_WithPredicate() {
        List<String> list = Arrays.asList("apple", "apricot", "banana", "avocado", "cherry");

        BitSet result = Index.allOf(list, s -> s.startsWith("a"));
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(1));
        Assertions.assertTrue(result.get(3));

        LinkedList<String> linkedList = new LinkedList<>(list);
        result = Index.allOf(linkedList, s -> s.startsWith("a"));
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(1));
        Assertions.assertTrue(result.get(3));
    }

    @Test
    public void testAllOf_AllPrimitiveTypes() {
        boolean[] boolArr = { true, false, true, false, true };
        BitSet boolResult = Index.allOf(boolArr, true);
        assertTrue(boolResult.get(0));
        assertTrue(boolResult.get(2));
        assertTrue(boolResult.get(4));
        assertEquals(3, boolResult.cardinality());

        char[] charArr = { 'a', 'b', 'a', 'c', 'a' };
        BitSet charResult = Index.allOf(charArr, 'a');
        assertTrue(charResult.get(0));
        assertTrue(charResult.get(2));
        assertTrue(charResult.get(4));
        assertEquals(3, charResult.cardinality());

        byte[] byteArr = { 1, 2, 1, 3, 1 };
        BitSet byteResult = Index.allOf(byteArr, (byte) 1);
        assertTrue(byteResult.get(0));
        assertTrue(byteResult.get(2));
        assertTrue(byteResult.get(4));
        assertEquals(3, byteResult.cardinality());

        short[] shortArr = { 10, 20, 10, 30, 10 };
        BitSet shortResult = Index.allOf(shortArr, (short) 10);
        assertTrue(shortResult.get(0));
        assertTrue(shortResult.get(2));
        assertTrue(shortResult.get(4));
        assertEquals(3, shortResult.cardinality());

        long[] longArr = { 100L, 200L, 100L, 300L, 100L };
        BitSet longResult = Index.allOf(longArr, 100L);
        assertTrue(longResult.get(0));
        assertTrue(longResult.get(2));
        assertTrue(longResult.get(4));
        assertEquals(3, longResult.cardinality());

        float[] floatArr = { 1.1f, 2.2f, 1.1f, 3.3f, 1.1f };
        BitSet floatResult = Index.allOf(floatArr, 1.1f);
        assertTrue(floatResult.get(0));
        assertTrue(floatResult.get(2));
        assertTrue(floatResult.get(4));
        assertEquals(3, floatResult.cardinality());
    }

    @Test
    public void testAllOf_LargeArrayPerformance() {
        int size = 10000;
        int[] largeArray = new int[size];
        for (int i = 0; i < size; i++) {
            largeArray[i] = i % 10;
        }

        BitSet result = Index.allOf(largeArray, 5);
        assertEquals(1000, result.cardinality());

        assertTrue(result.get(5));
        assertTrue(result.get(15));
        assertTrue(result.get(9995));
        assertFalse(result.get(0));
        assertFalse(result.get(9999));
    }

    @Test
    public void testAllOf_WithPredicate_Array() {
        Integer[] arr = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

        BitSet result = Index.allOf(arr, n -> n % 2 == 0);
        assertTrue(result.get(1));
        assertTrue(result.get(3));
        assertTrue(result.get(5));
        assertTrue(result.get(7));
        assertTrue(result.get(9));
        assertFalse(result.get(0));
        assertFalse(result.get(2));
        assertFalse(result.get(4));
        assertFalse(result.get(6));
        assertFalse(result.get(8));

        result = Index.allOf(arr, n -> n > 5, 5);
        assertTrue(result.get(5));
        assertTrue(result.get(6));
        assertTrue(result.get(7));
        assertTrue(result.get(8));
        assertTrue(result.get(9));
        assertFalse(result.get(0));
        assertFalse(result.get(4));
    }

    @Test
    public void testAllOf_WithPredicate_Collection() {
        List<String> list = Arrays.asList("apple", "banana", "apricot", "cherry", "avocado");

        BitSet result = Index.allOf(list, s -> s.startsWith("a"));
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));
        assertFalse(result.get(1));
        assertFalse(result.get(3));
    }

    @Test
    public void test_allOf_boolean_array() {
        boolean[] a = { true, false, true, false, true };
        BitSet result = Index.allOf(a, true);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, false);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(1));
        assertTrue(result.get(3));

        boolean[] b = { true, true };
        result = Index.allOf(b, false);
        assertEquals(0, result.cardinality());

        result = Index.allOf((boolean[]) null, true);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_boolean_array_fromIndex() {
        boolean[] a = { true, false, true, false, true };
        BitSet result = Index.allOf(a, true, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, true, -1);
        assertEquals(3, result.cardinality());

        result = Index.allOf(a, true, 10);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_byte_array() {
        byte[] a = { 1, 2, 1, 3, 1 };
        BitSet result = Index.allOf(a, (byte) 1);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, (byte) 5);
        assertEquals(0, result.cardinality());

        result = Index.allOf((byte[]) null, (byte) 1);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_byte_array_fromIndex() {
        byte[] a = { 1, 2, 1, 3, 1 };
        BitSet result = Index.allOf(a, (byte) 1, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, (byte) 1, -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_char_array() {
        char[] a = { 'a', 'b', 'a', 'c', 'a' };
        BitSet result = Index.allOf(a, 'a');
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 'd');
        assertEquals(0, result.cardinality());

        result = Index.allOf((char[]) null, 'a');
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_char_array_fromIndex() {
        char[] a = { 'a', 'b', 'a', 'c', 'a' };
        BitSet result = Index.allOf(a, 'a', 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 'a', -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_short_array() {
        short[] a = { 1, 2, 1, 3, 1 };
        BitSet result = Index.allOf(a, (short) 1);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, (short) 5);
        assertEquals(0, result.cardinality());

        result = Index.allOf((short[]) null, (short) 1);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_short_array_fromIndex() {
        short[] a = { 1, 2, 1, 3, 1 };
        BitSet result = Index.allOf(a, (short) 1, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, (short) 1, -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_int_array() {
        int[] a = { 1, 2, 1, 3, 1 };
        BitSet result = Index.allOf(a, 1);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 5);
        assertEquals(0, result.cardinality());

        result = Index.allOf((int[]) null, 1);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_int_array_fromIndex() {
        int[] a = { 1, 2, 1, 3, 1 };
        BitSet result = Index.allOf(a, 1, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 1, -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_long_array() {
        long[] a = { 1L, 2L, 1L, 3L, 1L };
        BitSet result = Index.allOf(a, 1L);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 5L);
        assertEquals(0, result.cardinality());

        result = Index.allOf((long[]) null, 1L);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_long_array_fromIndex() {
        long[] a = { 1L, 2L, 1L, 3L, 1L };
        BitSet result = Index.allOf(a, 1L, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 1L, -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_float_array() {
        float[] a = { 1.0f, 2.0f, 1.0f, 3.0f, 1.0f };
        BitSet result = Index.allOf(a, 1.0f);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 5.0f);
        assertEquals(0, result.cardinality());

        result = Index.allOf((float[]) null, 1.0f);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_float_array_fromIndex() {
        float[] a = { 1.0f, 2.0f, 1.0f, 3.0f, 1.0f };
        BitSet result = Index.allOf(a, 1.0f, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 1.0f, -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_float_array_tolerance() {
        float[] a = { 1.0f, 2.0f, 2.1f, 3.0f, 2.05f };
        BitSet result = Index.allOf(a, 2.0f, 0, 0.2f);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(1));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 2.0f, 0, 0.01f);
        assertEquals(1, result.cardinality());
        assertTrue(result.get(1));

        result = Index.allOf((float[]) null, 2.0f, 0, 0.1f);
        assertEquals(0, result.cardinality());
        assertThrows(IllegalArgumentException.class, () -> Index.allOf(a, 2.0f, 0, -0.1f));
    }

    @Test
    public void test_allOf_float_array_tolerance_fromIndex() {
        float[] a = { 1.0f, 2.0f, 2.1f, 3.0f, 2.05f };
        BitSet result = Index.allOf(a, 2.0f, 2, 0.2f);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 2.0f, -1, 0.2f);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_double_array() {
        double[] a = { 1.0, 2.0, 1.0, 3.0, 1.0 };
        BitSet result = Index.allOf(a, 1.0);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 5.0);
        assertEquals(0, result.cardinality());

        result = Index.allOf((double[]) null, 1.0);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_double_array_fromIndex() {
        double[] a = { 1.0, 2.0, 1.0, 3.0, 1.0 };
        BitSet result = Index.allOf(a, 1.0, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 1.0, -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_double_array_tolerance() {
        double[] a = { 1.0, 2.0, 2.1, 3.0, 2.05 };
        BitSet result = Index.allOf(a, 2.0, 0, 0.2);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(1));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 2.0, 0, 0.01);
        assertEquals(1, result.cardinality());
        assertTrue(result.get(1));

        result = Index.allOf((double[]) null, 2.0, 0, 0.1);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_double_array_tolerance_fromIndex() {
        double[] a = { 1.0, 2.0, 2.1, 3.0, 2.05 };
        BitSet result = Index.allOf(a, 2.0, 2, 0.2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 2.0, -1, 0.2);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_Object_array() {
        String[] a = { "a", "b", "a", "c", "a" };
        BitSet result = Index.allOf(a, "a");
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, "d");
        assertEquals(0, result.cardinality());

        String[] b = { "a", null, "c", null };
        result = Index.allOf(b, (String) null);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(1));
        assertTrue(result.get(3));

        result = Index.allOf((Object[]) null, "a");
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_Object_array_fromIndex() {
        String[] a = { "a", "b", "a", "c", "a" };
        BitSet result = Index.allOf(a, "a", 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, "a", -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_Collection() {
        List<String> c = Arrays.asList("a", "b", "a", "c", "a");
        BitSet result = Index.allOf(c, "a");
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(c, "d");
        assertEquals(0, result.cardinality());

        List<String> b = Arrays.asList("a", null, "c", null);
        result = Index.allOf(b, (String) null);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(1));
        assertTrue(result.get(3));

        result = Index.allOf((Collection<?>) null, "a");
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_Collection_fromIndex() {
        List<String> c = Arrays.asList("a", "b", "a", "c", "a");
        BitSet result = Index.allOf(c, "a", 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(c, "a", -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_Object_array_Predicate() {
        String[] a = { "apple", "banana", "avocado", "cherry", "apricot" };
        BitSet result = Index.allOf(a, s -> s.startsWith("a"));
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, s -> s.length() > 6);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf((String[]) null, s -> s.startsWith("a"));
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_Object_array_Predicate_fromIndex() {
        String[] a = { "apple", "banana", "avocado", "cherry", "apricot" };
        BitSet result = Index.allOf(a, s -> s.startsWith("a"), 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, s -> s.startsWith("a"), -1);
        assertEquals(3, result.cardinality());

        result = Index.allOf(a, s -> s.startsWith("a"), 10);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_Collection_Predicate() {
        List<String> c = Arrays.asList("apple", "banana", "avocado", "cherry", "apricot");
        BitSet result = Index.allOf(c, s -> s.startsWith("a"));
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(c, s -> s.length() > 6);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf((Collection<String>) null, s -> s.startsWith("a"));
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_Collection_Predicate_fromIndex() {
        List<String> c = Arrays.asList("apple", "banana", "avocado", "cherry", "apricot");
        BitSet result = Index.allOf(c, s -> s.startsWith("a"), 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(c, s -> s.startsWith("a"), -1);
        assertEquals(3, result.cardinality());

        result = Index.allOf(c, s -> s.startsWith("a"), 10);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void testAllOf_withNullAndEmpty() {
        assertTrue(Index.allOf((int[]) null, 1).isEmpty());
        assertTrue(Index.allOf(new int[0], 1).isEmpty());

        assertTrue(Index.allOf((Object[]) null, "a").isEmpty());
        assertTrue(Index.allOf(new Object[0], "a").isEmpty());

        assertTrue(Index.allOf((List<String>) null, "a").isEmpty());
        assertTrue(Index.allOf(Collections.emptyList(), "a").isEmpty());

        Predicate<String> p = s -> s.length() > 0;
        assertTrue(Index.allOf((String[]) null, p).isEmpty());
        assertTrue(Index.allOf(new String[0], p).isEmpty());
        assertTrue(Index.allOf((List<String>) null, p).isEmpty());
        assertTrue(Index.allOf(Collections.emptyList(), p).isEmpty());
    }

    @Test
    public void testAllOf_withFromIndex() {
        int[] array = { 1, 2, 1, 3, 1 };
        BitSet expected = new BitSet();
        expected.set(2);
        expected.set(4);
        assertEquals(expected, Index.allOf(array, 1, 1));
        assertEquals(expected, Index.allOf(array, 1, 2));

        BitSet expected2 = new BitSet();
        expected2.set(4);
        assertEquals(expected2, Index.allOf(array, 1, 3));
        assertTrue(Index.allOf(array, 1, 5).isEmpty());
    }

    @Test
    public void testAllOf_collectionWithFromIndex() {
        List<String> list = Arrays.asList("a", "b", "a", "c", "a");
        BitSet expected = new BitSet();
        expected.set(2);
        expected.set(4);
        assertEquals(expected, Index.allOf(list, "a", 1));
    }

    @Test
    public void testAllOf_predicateWithFromIndex() {
        String[] array = { "apple", "banana", "avocado", "orange", "apricot" };
        Predicate<String> startsWithA = s -> s.startsWith("a");
        BitSet expected = new BitSet();
        expected.set(2);
        expected.set(4);
        assertEquals(expected, Index.allOf(array, startsWithA, 1));
    }

    @Test
    public void testAllOf_doubleWithTolerance() {
        double[] array = { 1.0, 1.05, 2.0, 1.1, 0.95 };
        // Use 0.11 rather than 0.1: |1.1 - 1.0| evaluates to ~0.10000000000000009 in IEEE-754,
        // so a tolerance of exactly 0.1 excludes index 3. 0.11 keeps the test off the FP boundary.
        BitSet expected = new BitSet();
        expected.set(0);
        expected.set(1);
        expected.set(3);
        expected.set(4);
        assertEquals(expected, Index.allOf(array, 1.0, 0, 0.11));

        BitSet expectedFromIndex = new BitSet();
        expectedFromIndex.set(1);
        expectedFromIndex.set(3);
        expectedFromIndex.set(4);
        assertEquals(expectedFromIndex, Index.allOf(array, 1.0, 1, 0.11));
    }

    @Test
    public void testAllOf_BooleanArray() {
        boolean[] array = { true, false, true, false, true };

        BitSet result = Index.allOf(array, true);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));

        result = Index.allOf(array, false);
        Assertions.assertEquals(2, result.cardinality());
        Assertions.assertTrue(result.get(1));
        Assertions.assertTrue(result.get(3));

        result = Index.allOf(null, true);
        Assertions.assertEquals(0, result.cardinality());
    }

    @Test
    public void testAllOf_BooleanArray_WithFromIndex() {
        boolean[] array = { true, false, true, false, true };

        BitSet result = Index.allOf(array, true, 2);
        Assertions.assertEquals(2, result.cardinality());
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));

        result = Index.allOf(array, true, 10);
        Assertions.assertEquals(0, result.cardinality());
    }

    @Test
    public void testAllOf_ObjectArray() {
        String[] array = { "apple", "banana", "apple", "banana", "apple" };

        BitSet result = Index.allOf(array, "apple");
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));

        result = Index.allOf(array, (String) null);
        Assertions.assertEquals(0, result.cardinality());

        String[] arrayWithNull = { "apple", null, "apple", null };
        result = Index.allOf(arrayWithNull, (String) null);
        Assertions.assertEquals(2, result.cardinality());
        Assertions.assertTrue(result.get(1));
        Assertions.assertTrue(result.get(3));
    }

    @Test
    public void testAllOf_Array_WithPredicate_FromIndex() {
        String[] array = { "apple", "apricot", "banana", "avocado", "cherry" };

        BitSet result = Index.allOf(array, s -> s.startsWith("a"), 2);
        Assertions.assertEquals(1, result.cardinality());
        Assertions.assertTrue(result.get(3));
    }

    @Test
    public void testAllOf_Collection_WithPredicate_FromIndex() {
        List<String> list = Arrays.asList("apple", "apricot", "banana", "avocado", "cherry");

        BitSet result = Index.allOf(list, s -> s.startsWith("a"), 2);
        Assertions.assertEquals(1, result.cardinality());
        Assertions.assertTrue(result.get(3));
    }

    @Test
    public void testAllOf_FloatSpecialValues() {
        float[] arr = { 1.0f, Float.NaN, 2.0f, Float.NaN, Float.POSITIVE_INFINITY, Float.NaN };

        BitSet nanResult = Index.allOf(arr, Float.NaN);
        assertTrue(nanResult.get(1));
        assertTrue(nanResult.get(3));
        assertTrue(nanResult.get(5));
        assertEquals(3, nanResult.cardinality());

        BitSet infResult = Index.allOf(arr, Float.POSITIVE_INFINITY);
        assertTrue(infResult.get(4));
        assertEquals(1, infResult.cardinality());

        BitSet fuzzyNanResult = Index.allOf(arr, Float.NaN, 0, 0.1f);
        assertTrue(fuzzyNanResult.get(1));
        assertTrue(fuzzyNanResult.get(3));
        assertTrue(fuzzyNanResult.get(5));
        assertEquals(3, fuzzyNanResult.cardinality());
        assertEquals(OptionalInt.of(1), Index.of(arr, Float.NaN, 0, 0.1f));
        assertEquals(OptionalInt.of(5), Index.last(arr, Float.NaN, arr.length - 1, 0.1f));

        BitSet fuzzyInfResult = Index.allOf(arr, Float.POSITIVE_INFINITY, 0, 0.0f);
        assertTrue(fuzzyInfResult.get(4));
        assertEquals(1, fuzzyInfResult.cardinality());
    }

    @Test
    public void testAllOf_WithPredicate_ComplexConditions() {
        String[] strArr = { "apple", null, "application", "banana", null, "apply" };

        BitSet nullResult = Index.allOf(strArr, s -> s == null);
        assertTrue(nullResult.get(1));
        assertTrue(nullResult.get(4));
        assertEquals(2, nullResult.cardinality());

        BitSet appResult = Index.allOf(strArr, s -> s != null && s.startsWith("app"));
        assertTrue(appResult.get(0));
        assertTrue(appResult.get(2));
        assertTrue(appResult.get(5));
        assertEquals(3, appResult.cardinality());

        Integer[] numbers = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };

        BitSet div3Result = Index.allOf(numbers, n -> n % 3 == 0);
        assertTrue(div3Result.get(2));
        assertTrue(div3Result.get(5));
        assertTrue(div3Result.get(8));
        assertTrue(div3Result.get(11));
        assertEquals(4, div3Result.cardinality());

        BitSet primeResult = Index.allOf(numbers, n -> {
            if (n < 2) {
                return false;
            }
            for (int i = 2; i * i <= n; i++) {
                if (n % i == 0) {
                    return false;
                }
            }
            return true;
        });
        assertTrue(primeResult.get(1));
        assertTrue(primeResult.get(2));
        assertTrue(primeResult.get(4));
        assertTrue(primeResult.get(6));
        assertTrue(primeResult.get(10));
        assertEquals(5, primeResult.cardinality());
    }

    @Test
    public void testAllOf_EmptyResultSet() {
        int[] arr = { 1, 2, 3, 4, 5 };

        BitSet result = Index.allOf(arr, 10);
        assertTrue(result.isEmpty());
        assertEquals(0, result.cardinality());

        Integer[] intArr = { 1, 2, 3, 4, 5 };
        result = Index.allOf(intArr, n -> n > 10);
        assertTrue(result.isEmpty());
        assertEquals(0, result.cardinality());
    }

    @Test
    public void testAllOf_CollectionWithFromIndex() {
        List<String> list = Arrays.asList("a", "b", "a", "c", "a");
        java.util.BitSet result = Index.allOf(list, "a", 1);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        // fromIndex >= size
        result = Index.allOf(list, "a", 10);
        assertEquals(0, result.cardinality());

        // null source
        result = Index.allOf((java.util.Collection<?>) null, "a", 0);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void testAllOf_Collection_NonRandomAccess_WithFromIndex() {
        // Non-RandomAccess collection path with fromIndex
        java.util.LinkedList<String> list = new java.util.LinkedList<>(Arrays.asList("x", "a", "b", "a", "c"));
        java.util.BitSet result = Index.allOf(list, "a", 0);
        assertEquals(2, result.cardinality());

        // With fromIndex skipping first match
        result = Index.allOf(list, "a", 2);
        assertEquals(1, result.cardinality());
    }

    @Test
    public void testAllOf_LongArrayWithFromIndex() {
        long[] source = { 1L, 2L, 1L, 3L, 1L };
        java.util.BitSet result = Index.allOf(source, 1L, 1);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        // fromIndex >= len
        result = Index.allOf(source, 1L, 10);
        assertEquals(0, result.cardinality());

        // null source
        result = Index.allOf((long[]) null, 1L, 0);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void testAllOf_FloatArrayWithFromIndex() {
        float[] source = { 1f, 2f, 1f, 3f, 1f };
        java.util.BitSet result = Index.allOf(source, 1f, 1);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(source, 1f, 10);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void testAllOf_DoubleArrayWithTolerance_FromIndex() {
        double[] source = { 1.0, 2.0, 1.05, 3.0, 0.95 };
        java.util.BitSet result = Index.allOf(source, 1.0, 1, 0.1);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        // fromIndex >= len
        result = Index.allOf(source, 1.0, 10, 0.1);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void testAllOf_ObjectArrayWithFromIndex() {
        String[] source = { "a", "b", "a", "c", "a" };
        java.util.BitSet result = Index.allOf(source, "a", 1);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        // fromIndex >= len
        result = Index.allOf(source, "a", 10);
        assertEquals(0, result.cardinality());

        // null value matching
        String[] withNulls = { "a", null, "b", null };
        result = Index.allOf(withNulls, (Object) null, 0);
        assertEquals(2, result.cardinality());
    }

    @Test
    public void testAllOf_ArrayWithPredicateAndFromIndex() {
        String[] source = { "apple", "banana", "avocado", "cherry", "apricot" };
        java.util.BitSet result = Index.allOf(source, (java.util.function.Predicate<String>) s -> s.startsWith("a"), 1);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        // fromIndex >= len
        result = Index.allOf(source, (java.util.function.Predicate<String>) s -> s.startsWith("a"), 10);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void testAllOf_CollectionWithPredicateAndFromIndex() {
        List<String> list = Arrays.asList("apple", "banana", "avocado", "cherry", "apricot");
        java.util.BitSet result = Index.allOf(list, (java.util.function.Predicate<String>) s -> s.startsWith("a"), 1);
        assertEquals(2, result.cardinality());

        // fromIndex >= size
        result = Index.allOf(list, (java.util.function.Predicate<String>) s -> s.startsWith("a"), 10);
        assertEquals(0, result.cardinality());

        // null source
        result = Index.allOf((java.util.Collection<String>) null, (java.util.function.Predicate<String>) s -> true, 0);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_Collection_NonRandomAccess_WithFromIndex() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));
        BitSet result = Index.allOf(list, (Predicate<String>) s -> s.compareTo("c") >= 0, 2);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(2)); // "c"
        assertTrue(result.get(3)); // "d"
        assertTrue(result.get(4)); // "e"
    }

    @Test
    public void test_allOf_doubleArray_tolerance_NaN_consistency() {
        // BUG FIX: allOf(double[], NaN, tolerance) used to use min/max range comparison which returns false for NaN,
        // while of(...) and last(...) use Numbers.fuzzyEquals which treats NaN==NaN as true.
        // After fix, allOf is consistent with of/last: NaN matches NaN.
        double[] arr = { 1.0, Double.NaN, 2.0, Double.NaN, 5.0 };
        BitSet result = Index.allOf(arr, Double.NaN, 0, 0.1);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(1));
        assertTrue(result.get(3));

        // Sanity: of/last/allOf all agree
        assertEquals(OptionalInt.of(1), Index.of(arr, Double.NaN, 0, 0.1));
        assertEquals(OptionalInt.of(3), Index.last(arr, Double.NaN, arr.length - 1, 0.1));
    }

    @Test
    public void test_allOf_doubleArray_tolerance_infinity() {
        // Infinities of the same sign should match each other under fuzzyEquals (which Index.of uses).
        double[] arr = { Double.POSITIVE_INFINITY, 1.0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY };
        BitSet result = Index.allOf(arr, Double.POSITIVE_INFINITY, 0, 0.0);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
    }

    @Test
    public void test_allOf_doubleArray_tolerance_negative_throws() {
        // The tolerance contract rejects negative values.
        double[] arr = { 1.0, 2.0, 3.0 };
        assertThrows(IllegalArgumentException.class, () -> Index.allOf(arr, 2.0, 0, -0.1));
    }
}
