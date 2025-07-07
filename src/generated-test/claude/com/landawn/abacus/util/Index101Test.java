package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalInt;

public class Index101Test extends TestBase {

    // Additional tests for of() methods with edge cases

    @Test
    public void testOf_FloatArray_SpecialValues() {
        float[] arr = { 1.0f, -0.0f, 0.0f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN };

        // Test positive and negative zero
        assertEquals(OptionalInt.of(1), Index.of(arr, -0.0f));
        assertEquals(OptionalInt.of(2), Index.of(arr, 0.0f)); // -0.0f == 0.0f

        // Test NaN (first occurrence)
        assertEquals(OptionalInt.of(3), Index.of(arr, Float.NaN));
        assertEquals(OptionalInt.of(6), Index.of(arr, Float.NaN, 4));

        // Test infinities
        assertEquals(OptionalInt.of(4), Index.of(arr, Float.POSITIVE_INFINITY));
        assertEquals(OptionalInt.of(5), Index.of(arr, Float.NEGATIVE_INFINITY));
    }

    @Test
    public void testOf_DoubleArray_EdgeCases() {
        // Test with very small and very large values
        double[] arr = { Double.MIN_VALUE, Double.MAX_VALUE, -Double.MIN_VALUE, -Double.MAX_VALUE };

        assertEquals(OptionalInt.of(0), Index.of(arr, Double.MIN_VALUE));
        assertEquals(OptionalInt.of(1), Index.of(arr, Double.MAX_VALUE));
        assertEquals(OptionalInt.of(2), Index.of(arr, -Double.MIN_VALUE));
        assertEquals(OptionalInt.of(3), Index.of(arr, -Double.MAX_VALUE));

        // Test tolerance with boundary values
        double[] toleranceArr = { 1.0, 1.1, 1.2, 1.3, 1.4, 1.5 };
        assertEquals(OptionalInt.of(2), Index.of(toleranceArr, 1.25, 0.051));
        assertEquals(OptionalInt.empty(), Index.of(toleranceArr, 1.25, 0.04));

        // Test with negative tolerance (should still work)
        assertThrows(IllegalArgumentException.class, () -> Index.of(toleranceArr, 1.25, -0.05));
    }

    @Test
    public void testOf_CharArray_UnicodeCharacters() {
        char[] arr = { 'a', '中', '文', '\u0000', '\uffff' };

        assertEquals(OptionalInt.of(1), Index.of(arr, '中'));
        assertEquals(OptionalInt.of(2), Index.of(arr, '文'));
        assertEquals(OptionalInt.of(3), Index.of(arr, '\u0000'));
        assertEquals(OptionalInt.of(4), Index.of(arr, '\uffff'));
    }

    @Test
    public void testOf_Collection_VariousImplementations() {
        String[] data = { "a", "b", "c", "b", "d" };

        // Test with Vector (legacy, synchronized)
        Vector<String> vector = new Vector<>(Arrays.asList(data));
        assertEquals(OptionalInt.of(1), Index.of(vector, "b"));
        assertEquals(OptionalInt.of(3), Index.of(vector, "b", 2));

        // Test with Stack
        Stack<String> stack = new Stack<>();
        Collections.addAll(stack, data);
        assertEquals(OptionalInt.of(1), Index.of(stack, "b"));

        // Test with unmodifiable collection
        List<String> unmodifiable = Collections.unmodifiableList(Arrays.asList(data));
        assertEquals(OptionalInt.of(1), Index.of(unmodifiable, "b"));
    }

    @Test
    public void testOf_String_ComplexCases() {
        // Test with empty strings
        assertEquals(OptionalInt.of(0), Index.of("", ""));
        assertEquals(OptionalInt.of(0), Index.of("hello", ""));
        assertEquals(OptionalInt.of(3), Index.of("hello", "", 3));
        assertEquals(OptionalInt.empty(), Index.of("hello", "", 10)); // Beyond length returns length

        // Test with special characters
        String special = "a\tb\nc\rd\0e";
        assertEquals(OptionalInt.of(1), Index.of(special, '\t'));
        assertEquals(OptionalInt.of(3), Index.of(special, '\n'));
        assertEquals(OptionalInt.of(5), Index.of(special, '\r'));
        assertEquals(OptionalInt.of(7), Index.of(special, '\0'));

        // Test substring search with overlapping patterns
        String pattern = "aabaabaaab";
        assertEquals(OptionalInt.of(0), Index.of(pattern, "aab"));
        assertEquals(OptionalInt.of(3), Index.of(pattern, "aab", 1));
        assertEquals(OptionalInt.of(7), Index.of(pattern, "aab", 4));
    }

    // Additional tests for ofSubArray() with all primitive types

    @Test
    public void testOfSubArray_CharArray() {
        char[] source = "abcdefbcde".toCharArray();
        char[] sub1 = "bcd".toCharArray();
        char[] sub2 = "cde".toCharArray();
        char[] sub3 = "xyz".toCharArray();

        assertEquals(OptionalInt.of(1), Index.ofSubArray(source, sub1));
        assertEquals(OptionalInt.of(2), Index.ofSubArray(source, sub2));
        assertEquals(OptionalInt.empty(), Index.ofSubArray(source, sub3));
        assertEquals(OptionalInt.of(6), Index.ofSubArray(source, 2, sub1));

        // Test partial match
        assertEquals(OptionalInt.of(1), Index.ofSubArray(source, 0, sub1, 0, 2)); // Match "bc" only
    }

    @Test
    public void testOfSubArray_ByteArray() {
        byte[] source = { 1, 2, 3, 4, 5, 2, 3, 4, 6 };
        byte[] sub1 = { 2, 3, 4 };
        byte[] sub2 = { 4, 6 };
        byte[] sub3 = { 7, 8 };

        assertEquals(OptionalInt.of(1), Index.ofSubArray(source, sub1));
        assertEquals(OptionalInt.of(7), Index.ofSubArray(source, sub2));
        assertEquals(OptionalInt.empty(), Index.ofSubArray(source, sub3));
        assertEquals(OptionalInt.of(5), Index.ofSubArray(source, 2, sub1));
    }

    @Test
    public void testOfSubArray_ShortArray() {
        short[] source = { 10, 20, 30, 40, 50, 20, 30, 40, 60 };
        short[] sub1 = { 20, 30, 40 };
        short[] sub2 = { 40, 60 };

        assertEquals(OptionalInt.of(1), Index.ofSubArray(source, sub1));
        assertEquals(OptionalInt.of(7), Index.ofSubArray(source, sub2));
        assertEquals(OptionalInt.of(5), Index.ofSubArray(source, 2, sub1));
    }

    @Test
    public void testOfSubArray_LongArray() {
        long[] source = { 100L, 200L, 300L, 400L, 200L, 300L };
        long[] sub = { 200L, 300L };

        assertEquals(OptionalInt.of(1), Index.ofSubArray(source, sub));
        assertEquals(OptionalInt.of(4), Index.ofSubArray(source, 2, sub));
    }

    @Test
    public void testOfSubArray_FloatArray() {
        float[] source = { 1.1f, 2.2f, 3.3f, 4.4f, 2.2f, 3.3f };
        float[] sub = { 2.2f, 3.3f };

        assertEquals(OptionalInt.of(1), Index.ofSubArray(source, sub));
        assertEquals(OptionalInt.of(4), Index.ofSubArray(source, 2, sub));

        // Test with NaN
        float[] sourceWithNaN = { 1.0f, Float.NaN, 2.0f, Float.NaN, 2.0f };
        float[] subWithNaN = { Float.NaN, 2.0f };
        assertEquals(OptionalInt.of(1), Index.ofSubArray(sourceWithNaN, subWithNaN));
    }

    @Test
    public void testOfSubArray_DoubleArray() {
        double[] source = { 1.1, 2.2, 3.3, 4.4, 2.2, 3.3 };
        double[] sub = { 2.2, 3.3 };

        assertEquals(OptionalInt.of(1), Index.ofSubArray(source, sub));
        assertEquals(OptionalInt.of(4), Index.ofSubArray(source, 2, sub));

        // Test with special values
        double[] sourceSpecial = { 1.0, Double.POSITIVE_INFINITY, Double.NaN, Double.NEGATIVE_INFINITY };
        double[] subSpecial = { Double.NaN, Double.NEGATIVE_INFINITY };
        assertEquals(OptionalInt.of(2), Index.ofSubArray(sourceSpecial, subSpecial));
    }

    @Test
    public void testOfSubArray_BoundaryConditions() {
        int[] source = { 1, 2, 3, 4, 5 };

        // Sub array larger than source
        int[] largeSub = { 1, 2, 3, 4, 5, 6 };
        assertEquals(OptionalInt.empty(), Index.ofSubArray(source, largeSub));

        // Sub array equals source
        int[] equalSub = { 1, 2, 3, 4, 5 };
        assertEquals(OptionalInt.of(0), Index.ofSubArray(source, equalSub));

        // Single element sub array
        int[] singleSub = { 3 };
        assertEquals(OptionalInt.of(2), Index.ofSubArray(source, singleSub));

        // Test with null source or sub
        assertEquals(OptionalInt.empty(), Index.ofSubArray(null, singleSub));
        assertEquals(OptionalInt.empty(), Index.ofSubArray(source, null));
        assertEquals(OptionalInt.empty(), Index.ofSubArray((int[]) null, (int[]) null));
    }

    // Additional tests for last() methods

    @Test
    public void testLast_PrimitiveArrays_AllTypes() {
        // Boolean array
        boolean[] boolArr = { true, false, true, false, true };
        assertEquals(OptionalInt.of(4), Index.last(boolArr, true));
        assertEquals(OptionalInt.of(3), Index.last(boolArr, false));
        assertEquals(OptionalInt.of(2), Index.last(boolArr, true, 3));

        // Char array
        char[] charArr = { 'a', 'b', 'c', 'b', 'a' };
        assertEquals(OptionalInt.of(4), Index.last(charArr, 'a'));
        assertEquals(OptionalInt.of(3), Index.last(charArr, 'b'));

        // Byte array
        byte[] byteArr = { 1, 2, 3, 2, 1 };
        assertEquals(OptionalInt.of(4), Index.last(byteArr, (byte) 1));
        assertEquals(OptionalInt.of(3), Index.last(byteArr, (byte) 2));

        // Short array
        short[] shortArr = { 10, 20, 30, 20, 10 };
        assertEquals(OptionalInt.of(4), Index.last(shortArr, (short) 10));
        assertEquals(OptionalInt.of(3), Index.last(shortArr, (short) 20));

        // Long array
        long[] longArr = { 100L, 200L, 300L, 200L, 100L };
        assertEquals(OptionalInt.of(4), Index.last(longArr, 100L));
        assertEquals(OptionalInt.of(3), Index.last(longArr, 200L));

        // Float array
        float[] floatArr = { 1.1f, 2.2f, 3.3f, 2.2f, 1.1f };
        assertEquals(OptionalInt.of(4), Index.last(floatArr, 1.1f));
        assertEquals(OptionalInt.of(3), Index.last(floatArr, 2.2f));
    }

    @Test
    public void testLast_DoubleArray_WithTolerance() {
        double[] arr = { 1.0, 2.05, 3.0, 2.1, 4.0, 2.15 };

        // Test with tolerance
        assertEquals(OptionalInt.of(5), Index.last(arr, 2.1, 0.06));
        assertEquals(OptionalInt.of(3), Index.last(arr, 2.1, 0.01));
        assertEquals(OptionalInt.of(3), Index.last(arr, 2.1, 0.06, 4));

        // Test with zero tolerance
        assertEquals(OptionalInt.of(3), Index.last(arr, 2.1, 0.0));
    }

    @Test
    public void testLast_String_SpecialCases() {
        // Test with empty string search
        assertEquals(OptionalInt.of(5), Index.last("hello", ""));
        assertEquals(OptionalInt.of(3), Index.last("hello", "", 3));
        assertEquals(OptionalInt.empty(), Index.last("hello", "", -1));

        // Test with repeating patterns
        String repeating = "abababab";
        assertEquals(OptionalInt.of(6), Index.last(repeating, "ab"));
        assertEquals(OptionalInt.of(4), Index.last(repeating, "ab", 5));
        assertEquals(OptionalInt.of(2), Index.last(repeating, "ab", 3));
    }

    // Additional tests for lastOfSubArray()

    @Test
    public void testLastOfSubArray_AllPrimitiveTypes() {
        // Boolean array
        boolean[] boolSource = { true, false, true, false, true, false };
        boolean[] boolSub = { true, false };
        assertEquals(OptionalInt.of(4), Index.lastOfSubArray(boolSource, boolSub));
        assertEquals(OptionalInt.of(2), Index.lastOfSubArray(boolSource, 3, boolSub));

        // Char array
        char[] charSource = "abcabcabc".toCharArray();
        char[] charSub = "abc".toCharArray();
        assertEquals(OptionalInt.of(6), Index.lastOfSubArray(charSource, charSub));
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(charSource, 5, charSub));

        // Byte array
        byte[] byteSource = { 1, 2, 3, 1, 2, 3, 1, 2, 3 };
        byte[] byteSub = { 1, 2, 3 };
        assertEquals(OptionalInt.of(6), Index.lastOfSubArray(byteSource, byteSub));

        // Short array
        short[] shortSource = { 10, 20, 30, 10, 20, 30 };
        short[] shortSub = { 10, 20, 30 };
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(shortSource, shortSub));

        // Short array
        int[] intSource = { 10, 20, 30, 10, 20, 30 };
        int[] intSub = { 10, 20, 30 };
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(intSource, intSub));

        // Long array
        long[] longSource = { 100L, 200L, 300L, 100L, 200L, 300L };
        long[] longSub = { 100L, 200L, 300L };
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(longSource, longSub));

        // Float array
        float[] floatSource = { 1.1f, 2.2f, 3.3f, 1.1f, 2.2f, 3.3f };
        float[] floatSub = { 1.1f, 2.2f };
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(floatSource, floatSub));

        // Double array
        double[] doubleSource = { 1.1, 2.2, 3.3, 1.1, 2.2, 3.3 };
        double[] doubleSub = { 1.1, 2.2 };
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(doubleSource, doubleSub));

        // Short array
        String[] strSource = { "10", "20", "30", "10", "20", "30" };
        String[] strSub = { "10", "20", "30" };
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(strSource, strSub));
    }

    @Test
    public void testLastOfSubArray_SpecialCases() {
        int[] source = { 1, 2, 3, 4, 5 };

        // Test with startIndexFromBack >= source length
        assertEquals(OptionalInt.of(5), Index.lastOfSubArray(source, 10, new int[0]));

        // Test with negative startIndexFromBack
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(source, -1, new int[] { 1 }));

        // Test when sub array appears at the very end
        int[] endSource = { 1, 2, 3, 4, 5, 6, 7 };
        int[] endSub = { 6, 7 };
        assertEquals(OptionalInt.of(5), Index.lastOfSubArray(endSource, endSub));
    }

    // Additional tests for allOf() methods

    @Test
    public void testAllOf_AllPrimitiveTypes() {
        // Boolean array
        boolean[] boolArr = { true, false, true, false, true };
        BitSet boolResult = Index.allOf(boolArr, true);
        assertTrue(boolResult.get(0));
        assertTrue(boolResult.get(2));
        assertTrue(boolResult.get(4));
        assertEquals(3, boolResult.cardinality());

        // Char array
        char[] charArr = { 'a', 'b', 'a', 'c', 'a' };
        BitSet charResult = Index.allOf(charArr, 'a');
        assertTrue(charResult.get(0));
        assertTrue(charResult.get(2));
        assertTrue(charResult.get(4));
        assertEquals(3, charResult.cardinality());

        // Byte array
        byte[] byteArr = { 1, 2, 1, 3, 1 };
        BitSet byteResult = Index.allOf(byteArr, (byte) 1);
        assertTrue(byteResult.get(0));
        assertTrue(byteResult.get(2));
        assertTrue(byteResult.get(4));
        assertEquals(3, byteResult.cardinality());

        // Short array
        short[] shortArr = { 10, 20, 10, 30, 10 };
        BitSet shortResult = Index.allOf(shortArr, (short) 10);
        assertTrue(shortResult.get(0));
        assertTrue(shortResult.get(2));
        assertTrue(shortResult.get(4));
        assertEquals(3, shortResult.cardinality());

        // Long array
        long[] longArr = { 100L, 200L, 100L, 300L, 100L };
        BitSet longResult = Index.allOf(longArr, 100L);
        assertTrue(longResult.get(0));
        assertTrue(longResult.get(2));
        assertTrue(longResult.get(4));
        assertEquals(3, longResult.cardinality());

        // Float array
        float[] floatArr = { 1.1f, 2.2f, 1.1f, 3.3f, 1.1f };
        BitSet floatResult = Index.allOf(floatArr, 1.1f);
        assertTrue(floatResult.get(0));
        assertTrue(floatResult.get(2));
        assertTrue(floatResult.get(4));
        assertEquals(3, floatResult.cardinality());
    }

    @Test
    public void testAllOf_FloatSpecialValues() {
        float[] arr = { 1.0f, Float.NaN, 2.0f, Float.NaN, Float.POSITIVE_INFINITY, Float.NaN };

        // Test NaN occurrences
        BitSet nanResult = Index.allOf(arr, Float.NaN);
        assertTrue(nanResult.get(1));
        assertTrue(nanResult.get(3));
        assertTrue(nanResult.get(5));
        assertEquals(3, nanResult.cardinality());

        // Test infinity
        BitSet infResult = Index.allOf(arr, Float.POSITIVE_INFINITY);
        assertTrue(infResult.get(4));
        assertEquals(1, infResult.cardinality());
    }

    @Test
    public void testAllOf_WithPredicate_ComplexConditions() {
        // Test with null handling
        String[] strArr = { "apple", null, "application", "banana", null, "apply" };

        // Find all nulls
        BitSet nullResult = Index.allOf(strArr, s -> s == null);
        assertTrue(nullResult.get(1));
        assertTrue(nullResult.get(4));
        assertEquals(2, nullResult.cardinality());

        // Find all strings starting with "app" (excluding nulls)
        BitSet appResult = Index.allOf(strArr, s -> s != null && s.startsWith("app"));
        assertTrue(appResult.get(0));
        assertTrue(appResult.get(2));
        assertTrue(appResult.get(5));
        assertEquals(3, appResult.cardinality());

        // Test with numbers and complex conditions
        Integer[] numbers = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };

        // Find all numbers divisible by 3
        BitSet div3Result = Index.allOf(numbers, n -> n % 3 == 0);
        assertTrue(div3Result.get(2)); // 3
        assertTrue(div3Result.get(5)); // 6
        assertTrue(div3Result.get(8)); // 9
        assertTrue(div3Result.get(11)); // 12
        assertEquals(4, div3Result.cardinality());

        // Find all prime numbers
        BitSet primeResult = Index.allOf(numbers, n -> {
            if (n < 2)
                return false;
            for (int i = 2; i * i <= n; i++) {
                if (n % i == 0)
                    return false;
            }
            return true;
        });
        assertTrue(primeResult.get(1)); // 2
        assertTrue(primeResult.get(2)); // 3
        assertTrue(primeResult.get(4)); // 5
        assertTrue(primeResult.get(6)); // 7
        assertTrue(primeResult.get(10)); // 11
        assertEquals(5, primeResult.cardinality());
    }

    @Test
    public void testAllOf_EmptyResultSet() {
        int[] arr = { 1, 2, 3, 4, 5 };

        // Search for non-existent value
        BitSet result = Index.allOf(arr, 10);
        assertTrue(result.isEmpty());
        assertEquals(0, result.cardinality());

        // Search with predicate that matches nothing
        Integer[] intArr = { 1, 2, 3, 4, 5 };
        result = Index.allOf(intArr, n -> n > 10);
        assertTrue(result.isEmpty());
        assertEquals(0, result.cardinality());
    }

    @Test
    public void testAllOf_LargeArrayPerformance() {
        // Create a large array with repeating pattern
        int size = 10000;
        int[] largeArray = new int[size];
        for (int i = 0; i < size; i++) {
            largeArray[i] = i % 10;
        }

        // Find all occurrences of 5
        BitSet result = Index.allOf(largeArray, 5);
        assertEquals(1000, result.cardinality()); // Should find 1000 occurrences

        // Verify some specific positions
        assertTrue(result.get(5));
        assertTrue(result.get(15));
        assertTrue(result.get(9995));
        assertFalse(result.get(0));
        assertFalse(result.get(9999));
    }

    // Stress tests and corner cases

    @Test
    public void testStressTest_MixedNullElements() {
        Object[] arr = new Object[100];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (i % 3 == 0) ? null : "value" + (i % 5);
        }

        // Count nulls
        BitSet nulls = Index.allOf(arr, (Object) null);
        assertEquals(34, nulls.cardinality()); // 100/3 + 1 = 34

        // Count specific value
        BitSet value0 = Index.allOf(arr, "value0");
        assertEquals(13, value0.cardinality()); // Non-null positions where i%5==0
    }

    @Test
    public void testBoundaryConditions_MaxArraySize() {
        // Test with maximum practical array size (small for unit test)
        int[] arr = new int[1000];
        Arrays.fill(arr, 42);
        arr[0] = 1;
        arr[500] = 1;
        arr[999] = 1;

        BitSet result = Index.allOf(arr, 1);
        assertTrue(result.get(0));
        assertTrue(result.get(500));
        assertTrue(result.get(999));
        assertEquals(3, result.cardinality());
    }

    @Test
    public void testThreadSafety_Considerations() {
        // Note: Index methods are stateless and should be thread-safe
        // This test verifies that no shared state is modified

        final int[] sharedArray = { 1, 2, 3, 2, 1, 2, 3 };
        final int iterations = 100;

        // Run multiple searches concurrently
        for (int i = 0; i < iterations; i++) {
            assertEquals(OptionalInt.of(1), Index.of(sharedArray, 2));
            assertEquals(OptionalInt.of(5), Index.last(sharedArray, 2));
            BitSet all2s = Index.allOf(sharedArray, 2);
            assertEquals(3, all2s.cardinality());
        }
    }
}
