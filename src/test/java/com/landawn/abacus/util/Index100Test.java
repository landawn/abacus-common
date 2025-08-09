package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalInt;

public class Index100Test extends TestBase {

    // Tests for of() methods with primitive arrays

    @Test
    public void testOf_BooleanArray() {
        boolean[] arr = { true, false, true, false, true };

        assertEquals(OptionalInt.of(0), Index.of(arr, true));
        assertEquals(OptionalInt.of(1), Index.of(arr, false));
        assertEquals(OptionalInt.empty(), Index.of(arr, true, 5));
        assertEquals(OptionalInt.of(2), Index.of(arr, true, 1));
        assertEquals(OptionalInt.of(3), Index.of(arr, false, 2));

        // Test with null array
        assertEquals(OptionalInt.empty(), Index.of((boolean[]) null, true));

        // Test with empty array
        assertEquals(OptionalInt.empty(), Index.of(new boolean[0], true));
    }

    @Test
    public void testOf_CharArray() {
        char[] arr = { 'a', 'b', 'c', 'b', 'd' };

        assertEquals(OptionalInt.of(0), Index.of(arr, 'a'));
        assertEquals(OptionalInt.of(1), Index.of(arr, 'b'));
        assertEquals(OptionalInt.of(2), Index.of(arr, 'c'));
        assertEquals(OptionalInt.empty(), Index.of(arr, 'e'));
        assertEquals(OptionalInt.of(3), Index.of(arr, 'b', 2));

        // Test with null array
        assertEquals(OptionalInt.empty(), Index.of((char[]) null, 'a'));
    }

    @Test
    public void testOf_ByteArray() {
        byte[] arr = { 1, 2, 3, 2, 4 };

        assertEquals(OptionalInt.of(0), Index.of(arr, (byte) 1));
        assertEquals(OptionalInt.of(1), Index.of(arr, (byte) 2));
        assertEquals(OptionalInt.empty(), Index.of(arr, (byte) 5));
        assertEquals(OptionalInt.of(3), Index.of(arr, (byte) 2, 2));
    }

    @Test
    public void testOf_ShortArray() {
        short[] arr = { 10, 20, 30, 20, 40 };

        assertEquals(OptionalInt.of(0), Index.of(arr, (short) 10));
        assertEquals(OptionalInt.of(1), Index.of(arr, (short) 20));
        assertEquals(OptionalInt.empty(), Index.of(arr, (short) 50));
        assertEquals(OptionalInt.of(3), Index.of(arr, (short) 20, 2));
    }

    @Test
    public void testOf_IntArray() {
        int[] arr = { 100, 200, 300, 200, 400 };

        assertEquals(OptionalInt.of(0), Index.of(arr, 100));
        assertEquals(OptionalInt.of(1), Index.of(arr, 200));
        assertEquals(OptionalInt.empty(), Index.of(arr, 500));
        assertEquals(OptionalInt.of(3), Index.of(arr, 200, 2));

        // Test with negative fromIndex
        assertEquals(OptionalInt.of(0), Index.of(arr, 100, -1));
    }

    @Test
    public void testOf_LongArray() {
        long[] arr = { 1000L, 2000L, 3000L, 2000L, 4000L };

        assertEquals(OptionalInt.of(0), Index.of(arr, 1000L));
        assertEquals(OptionalInt.of(1), Index.of(arr, 2000L));
        assertEquals(OptionalInt.empty(), Index.of(arr, 5000L));
        assertEquals(OptionalInt.of(3), Index.of(arr, 2000L, 2));
    }

    @Test
    public void testOf_FloatArray() {
        float[] arr = { 1.1f, 2.2f, 3.3f, 2.2f, 4.4f };

        assertEquals(OptionalInt.of(0), Index.of(arr, 1.1f));
        assertEquals(OptionalInt.of(1), Index.of(arr, 2.2f));
        assertEquals(OptionalInt.empty(), Index.of(arr, 5.5f));
        assertEquals(OptionalInt.of(3), Index.of(arr, 2.2f, 2));

        // Test with NaN
        float[] nanArr = { 1.0f, Float.NaN, 3.0f };
        assertEquals(OptionalInt.of(1), Index.of(nanArr, Float.NaN));
    }

    @Test
    public void testOf_DoubleArray() {
        double[] arr = { 1.1, 2.2, 3.3, 2.2, 4.4 };

        assertEquals(OptionalInt.of(0), Index.of(arr, 1.1));
        assertEquals(OptionalInt.of(1), Index.of(arr, 2.2));
        assertEquals(OptionalInt.empty(), Index.of(arr, 5.5));
        assertEquals(OptionalInt.of(3), Index.of(arr, 2.2, 0.01, 2));

        // Test with tolerance
        assertEquals(OptionalInt.of(1), Index.of(arr, 2.19, 0.02));
        assertEquals(OptionalInt.empty(), Index.of(arr, 2.19, 0.005));
        assertEquals(OptionalInt.of(3), Index.of(arr, 2.21, 0.02, 2));
    }

    @Test
    public void testOf_ObjectArray() {
        String[] arr = { "a", "b", "c", "b", null, "d" };

        assertEquals(OptionalInt.of(0), Index.of(arr, "a"));
        assertEquals(OptionalInt.of(1), Index.of(arr, "b"));
        assertEquals(OptionalInt.of(4), Index.of(arr, null));
        assertEquals(OptionalInt.empty(), Index.of(arr, "e"));
        assertEquals(OptionalInt.of(3), Index.of(arr, "b", 2));

        // Test with null array
        assertEquals(OptionalInt.empty(), Index.of((Object[]) null, "a"));
    }

    @Test
    public void testOf_Collection() {
        List<String> list = Arrays.asList("a", "b", "c", "b", null, "d");

        assertEquals(OptionalInt.of(0), Index.of(list, "a"));
        assertEquals(OptionalInt.of(1), Index.of(list, "b"));
        assertEquals(OptionalInt.of(4), Index.of(list, null));
        assertEquals(OptionalInt.empty(), Index.of(list, "e"));
        assertEquals(OptionalInt.of(3), Index.of(list, "b", 2));

        // Test with LinkedList (non-RandomAccess)
        LinkedList<String> linkedList = new LinkedList<>(list);
        assertEquals(OptionalInt.of(1), Index.of(linkedList, "b"));
        assertEquals(OptionalInt.of(3), Index.of(linkedList, "b", 2));
    }

    @Test
    public void testOf_Iterator() {
        List<String> list = Arrays.asList("a", "b", "c", "b", "d");

        assertEquals(OptionalInt.of(0), Index.of(list.iterator(), "a"));
        assertEquals(OptionalInt.of(1), Index.of(list.iterator(), "b"));
        assertEquals(OptionalInt.empty(), Index.of(list.iterator(), "e"));
        assertEquals(OptionalInt.of(3), Index.of(list.iterator(), "b", 2));
    }

    @Test
    public void testOf_String() {
        String str = "hello world";

        assertEquals(OptionalInt.of(0), Index.of(str, 'h'));
        assertEquals(OptionalInt.of(2), Index.of(str, 'l'));
        assertEquals(OptionalInt.of(3), Index.of(str, 'l', 3));
        assertEquals(OptionalInt.empty(), Index.of(str, 'z'));

        assertEquals(OptionalInt.of(0), Index.of(str, "hello"));
        assertEquals(OptionalInt.of(6), Index.of(str, "world"));
        assertEquals(OptionalInt.empty(), Index.of(str, "test"));
        assertEquals(OptionalInt.of(2), Index.of(str, "ll"));

        // Test with null string
        assertEquals(OptionalInt.empty(), Index.of((String) null, 'a'));
        assertEquals(OptionalInt.empty(), Index.of((String) null, "test"));
    }

    @Test
    public void testOfIgnoreCase_String() {
        String str = "Hello World";

        assertEquals(OptionalInt.of(0), Index.ofIgnoreCase(str, "hello"));
        assertEquals(OptionalInt.of(6), Index.ofIgnoreCase(str, "WORLD"));
        assertEquals(OptionalInt.empty(), Index.ofIgnoreCase(str, "test"));
        assertEquals(OptionalInt.of(0), Index.ofIgnoreCase(str, "HELLO", 0));
    }

    // Tests for ofSubArray() methods

    @Test
    public void testOfSubArray_BooleanArray() {
        boolean[] source = { true, false, true, false, true };
        boolean[] sub1 = { true, false };
        boolean[] sub2 = { false, true };
        boolean[] sub3 = { true, true };

        assertEquals(OptionalInt.of(0), Index.ofSubArray(source, sub1));
        assertEquals(OptionalInt.of(1), Index.ofSubArray(source, sub2));
        assertEquals(OptionalInt.empty(), Index.ofSubArray(source, sub3));
        assertEquals(OptionalInt.of(2), Index.ofSubArray(source, 1, sub1));

        // Test empty subarray
        assertEquals(OptionalInt.of(0), Index.ofSubArray(source, new boolean[0]));
        assertEquals(OptionalInt.of(3), Index.ofSubArray(source, 3, new boolean[0]));

        // Test with sizeToMatch parameter
        assertEquals(OptionalInt.of(0), Index.ofSubArray(source, 0, sub1, 0, 1));
    }

    @Test
    public void testOfSubArray_IntArray() {
        int[] source = { 1, 2, 3, 4, 2, 3, 5 };
        int[] sub1 = { 2, 3 };
        int[] sub2 = { 3, 5 };
        int[] sub3 = { 1, 3 };

        assertEquals(OptionalInt.of(1), Index.ofSubArray(source, sub1));
        assertEquals(OptionalInt.of(5), Index.ofSubArray(source, sub2));
        assertEquals(OptionalInt.empty(), Index.ofSubArray(source, sub3));
        assertEquals(OptionalInt.of(4), Index.ofSubArray(source, 2, sub1));
    }

    @Test
    public void testOfSubArray_ObjectArray() {
        String[] source = { "a", "b", "c", "d", "b", "c", "e" };
        String[] sub1 = { "b", "c" };
        String[] sub2 = { "c", "e" };
        String[] sub3 = { "a", "c" };

        assertEquals(OptionalInt.of(1), Index.ofSubArray(source, sub1));
        assertEquals(OptionalInt.of(5), Index.ofSubArray(source, sub2));
        assertEquals(OptionalInt.empty(), Index.ofSubArray(source, sub3));
        assertEquals(OptionalInt.of(4), Index.ofSubArray(source, 2, sub1));

        // Test with null elements
        String[] sourceWithNull = { "a", null, "b", null, "b" };
        String[] subWithNull = { null, "b" };
        assertEquals(OptionalInt.of(1), Index.ofSubArray(sourceWithNull, subWithNull));
    }

    @Test
    public void testOfSubList() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "b", "c", "e");
        List<String> sub1 = Arrays.asList("b", "c");
        List<String> sub2 = Arrays.asList("c", "e");
        List<String> sub3 = Arrays.asList("a", "c");

        assertEquals(OptionalInt.of(1), Index.ofSubList(source, sub1));
        assertEquals(OptionalInt.of(5), Index.ofSubList(source, sub2));
        assertEquals(OptionalInt.empty(), Index.ofSubList(source, sub3));
        assertEquals(OptionalInt.of(4), Index.ofSubList(source, 2, sub1));

        // Test with LinkedList (non-RandomAccess)
        LinkedList<String> linkedSource = new LinkedList<>(source);
        LinkedList<String> linkedSub = new LinkedList<>(sub1);
        assertEquals(OptionalInt.of(1), Index.ofSubList(linkedSource, linkedSub));
    }

    // Tests for last() methods

    @Test
    public void testLast_IntArray() {
        int[] arr = { 1, 2, 3, 2, 4, 2 };

        assertEquals(OptionalInt.of(5), Index.last(arr, 2));
        assertEquals(OptionalInt.of(0), Index.last(arr, 1));
        assertEquals(OptionalInt.empty(), Index.last(arr, 5));
        assertEquals(OptionalInt.of(3), Index.last(arr, 2, 4));
        assertEquals(OptionalInt.of(3), Index.last(arr, 2, 3));
    }

    @Test
    public void testLast_ObjectArray() {
        String[] arr = { "a", "b", "c", "b", "d", "b" };

        assertEquals(OptionalInt.of(5), Index.last(arr, "b"));
        assertEquals(OptionalInt.of(0), Index.last(arr, "a"));
        assertEquals(OptionalInt.empty(), Index.last(arr, "e"));
        assertEquals(OptionalInt.of(3), Index.last(arr, "b", 4));
    }

    @Test
    public void testLast_Collection() {
        List<String> list = Arrays.asList("a", "b", "c", "b", "d", "b");

        assertEquals(OptionalInt.of(5), Index.last(list, "b"));
        assertEquals(OptionalInt.of(0), Index.last(list, "a"));
        assertEquals(OptionalInt.empty(), Index.last(list, "e"));
        assertEquals(OptionalInt.of(3), Index.last(list, "b", 4));
    }

    @Test
    public void testLast_String() {
        String str = "hello world hello";

        assertEquals(OptionalInt.of(15), Index.last(str, 'l'));
        assertEquals(OptionalInt.of(12), Index.last(str, 'h'));
        assertEquals(OptionalInt.empty(), Index.last(str, 'z'));

        assertEquals(OptionalInt.of(12), Index.last(str, "hello"));
        assertEquals(OptionalInt.of(6), Index.last(str, "world"));
        assertEquals(OptionalInt.empty(), Index.last(str, "test"));
    }

    @Test
    public void testLastOfIgnoreCase_String() {
        String str = "Hello World HELLO";

        assertEquals(OptionalInt.of(12), Index.lastOfIgnoreCase(str, "hello"));
        assertEquals(OptionalInt.of(6), Index.lastOfIgnoreCase(str, "WORLD"));
        assertEquals(OptionalInt.empty(), Index.lastOfIgnoreCase(str, "test"));
    }

    // Tests for lastOfSubArray() methods

    @Test
    public void testLastOfSubArray_IntArray() {
        int[] source = { 1, 2, 3, 4, 2, 3, 5, 2, 3 };
        int[] sub1 = { 2, 3 };
        int[] sub2 = { 3, 5 };
        int[] sub3 = { 1, 3 };

        assertEquals(OptionalInt.of(7), Index.lastOfSubArray(source, sub1));
        assertEquals(OptionalInt.of(5), Index.lastOfSubArray(source, sub2));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(source, sub3));
        assertEquals(OptionalInt.of(4), Index.lastOfSubArray(source, 6, sub1));

        // Test empty subarray
        assertEquals(OptionalInt.of(9), Index.lastOfSubArray(source, new int[0]));
        assertEquals(OptionalInt.of(5), Index.lastOfSubArray(source, 5, new int[0]));
    }

    @Test
    public void testLastOfSubList() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "b", "c", "e", "b", "c");
        List<String> sub1 = Arrays.asList("b", "c");
        List<String> sub2 = Arrays.asList("c", "e");

        assertEquals(OptionalInt.of(7), Index.lastOfSubList(source, sub1));
        assertEquals(OptionalInt.of(5), Index.lastOfSubList(source, sub2));
        assertEquals(OptionalInt.of(4), Index.lastOfSubList(source, 6, sub1));
    }

    // Tests for allOf() methods

    @Test
    public void testAllOf_IntArray() {
        int[] arr = { 1, 2, 3, 2, 4, 2, 5 };

        BitSet result = Index.allOf(arr, 2);
        assertTrue(result.get(1));
        assertTrue(result.get(3));
        assertTrue(result.get(5));
        assertFalse(result.get(0));
        assertFalse(result.get(2));
        assertFalse(result.get(4));
        assertFalse(result.get(6));

        // Test with fromIndex
        result = Index.allOf(arr, 2, 2);
        assertFalse(result.get(1));
        assertTrue(result.get(3));
        assertTrue(result.get(5));

        // Test with value not in array
        result = Index.allOf(arr, 10);
        assertTrue(result.isEmpty());

        // Test with null array
        result = Index.allOf((int[]) null, 2);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testAllOf_DoubleArray_WithTolerance() {
        double[] arr = { 1.0, 2.1, 3.0, 2.05, 4.0, 2.15, 5.0 };

        BitSet result = Index.allOf(arr, 2.1, 0.1);
        assertTrue(result.get(1));
        assertTrue(result.get(3));
        assertTrue(result.get(5));
        assertFalse(result.get(0));
        assertFalse(result.get(2));
        assertFalse(result.get(4));
        assertFalse(result.get(6));
    }

    @Test
    public void testAllOf_ObjectArray() {
        String[] arr = { "a", "b", "c", "b", null, "b", "d" };

        BitSet result = Index.allOf(arr, "b");
        assertTrue(result.get(1));
        assertTrue(result.get(3));
        assertTrue(result.get(5));
        assertFalse(result.get(0));
        assertFalse(result.get(2));
        assertFalse(result.get(4));
        assertFalse(result.get(6));

        // Test with null
        result = Index.allOf(arr, (String) null);
        assertTrue(result.get(4));
        assertFalse(result.get(0));
        assertFalse(result.get(1));
    }

    @Test
    public void testAllOf_Collection() {
        List<String> list = Arrays.asList("a", "b", "c", "b", null, "b", "d");

        BitSet result = Index.allOf(list, "b");
        assertTrue(result.get(1));
        assertTrue(result.get(3));
        assertTrue(result.get(5));
        assertFalse(result.get(0));
        assertFalse(result.get(2));
        assertFalse(result.get(4));
        assertFalse(result.get(6));

        // Test with LinkedList (non-RandomAccess)
        LinkedList<String> linkedList = new LinkedList<>(list);
        result = Index.allOf(linkedList, "b");
        assertTrue(result.get(1));
        assertTrue(result.get(3));
        assertTrue(result.get(5));
    }

    @Test
    public void testAllOf_WithPredicate_Array() {
        Integer[] arr = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

        // Find all even numbers
        BitSet result = Index.allOf(arr, n -> n % 2 == 0);
        assertTrue(result.get(1)); // 2
        assertTrue(result.get(3)); // 4
        assertTrue(result.get(5)); // 6
        assertTrue(result.get(7)); // 8
        assertTrue(result.get(9)); // 10
        assertFalse(result.get(0)); // 1
        assertFalse(result.get(2)); // 3
        assertFalse(result.get(4)); // 5
        assertFalse(result.get(6)); // 7
        assertFalse(result.get(8)); // 9

        // Test with fromIndex
        result = Index.allOf(arr, n -> n > 5, 5);
        assertTrue(result.get(5)); // 6
        assertTrue(result.get(6)); // 7
        assertTrue(result.get(7)); // 8
        assertTrue(result.get(8)); // 9
        assertTrue(result.get(9)); // 10
        assertFalse(result.get(0));
        assertFalse(result.get(4));
    }

    @Test
    public void testAllOf_WithPredicate_Collection() {
        List<String> list = Arrays.asList("apple", "banana", "apricot", "cherry", "avocado");

        // Find all strings starting with 'a'
        BitSet result = Index.allOf(list, s -> s.startsWith("a"));
        assertTrue(result.get(0)); // apple
        assertTrue(result.get(2)); // apricot
        assertTrue(result.get(4)); // avocado
        assertFalse(result.get(1)); // banana
        assertFalse(result.get(3)); // cherry
    }

    // Edge cases and special tests

    @Test
    public void testEdgeCases_EmptyArraysAndCollections() {
        // Empty arrays
        assertEquals(OptionalInt.empty(), Index.of(new int[0], 1));
        assertEquals(OptionalInt.empty(), Index.last(new int[0], 1));
        assertTrue(Index.allOf(new int[0], 1).isEmpty());

        // Empty collections
        List<String> emptyList = new ArrayList<>();
        assertEquals(OptionalInt.empty(), Index.of(emptyList, "a"));
        assertEquals(OptionalInt.empty(), Index.last(emptyList, "a"));
        assertTrue(Index.allOf(emptyList, "a").isEmpty());
    }

    @Test
    public void testEdgeCases_LargeFromIndex() {
        int[] arr = { 1, 2, 3, 4, 5 };

        // fromIndex >= array length
        assertEquals(OptionalInt.empty(), Index.of(arr, 3, 10));
        assertTrue(Index.allOf(arr, 3, 10).isEmpty());
    }

    @Test
    public void testEdgeCases_NegativeFromIndex() {
        int[] arr = { 1, 2, 3, 4, 5 };

        // Negative fromIndex should be treated as 0
        assertEquals(OptionalInt.of(0), Index.of(arr, 1, -5));
        assertEquals(OptionalInt.of(2), Index.of(arr, 3, -1));
    }

    @Test
    public void testOfSubArray_InvalidRange() {
        int[] source = { 1, 2, 3, 4, 5 };
        int[] sub = { 2, 3, 4 };

        // This should throw IndexOutOfBoundsException
        assertThrows(IndexOutOfBoundsException.class, () -> Index.ofSubArray(source, 0, sub, 1, 5)); // startIndex=1, size=5 exceeds sub array bounds
    }

    @Test
    public void testSpecialValues_NaN() {
        double[] arr = { 1.0, Double.NaN, 3.0, Double.NaN, 5.0 };

        assertEquals(OptionalInt.of(1), Index.of(arr, Double.NaN));
        assertEquals(OptionalInt.of(3), Index.last(arr, Double.NaN));

        BitSet result = Index.allOf(arr, Double.NaN);
        assertTrue(result.get(1));
        assertTrue(result.get(3));
        assertFalse(result.get(0));
        assertFalse(result.get(2));
        assertFalse(result.get(4));
    }

    @Test
    public void testSpecialValues_InfinityValues() {
        double[] arr = { 1.0, Double.POSITIVE_INFINITY, 3.0, Double.NEGATIVE_INFINITY, 5.0 };

        assertEquals(OptionalInt.of(1), Index.of(arr, Double.POSITIVE_INFINITY));
        assertEquals(OptionalInt.of(3), Index.of(arr, Double.NEGATIVE_INFINITY));
    }
}
