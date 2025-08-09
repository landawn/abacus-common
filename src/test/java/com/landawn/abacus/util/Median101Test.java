package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

public class Median101Test extends TestBase {

    // Tests for char arrays
    @Test
    public void testOfCharArray() {
        // Test single element
        Pair<Character, OptionalChar> result1 = Median.of('a');
        Assertions.assertEquals('a', result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        // Test two elements
        Pair<Character, OptionalChar> result2 = Median.of('a', 'b');
        Assertions.assertEquals('a', result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals('b', result2.right().get());

        // Test three elements
        Pair<Character, OptionalChar> result3 = Median.of('a', 'c', 'b');
        Assertions.assertEquals('b', result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        // Test even number of elements
        Pair<Character, OptionalChar> result4 = Median.of('a', 'b', 'c', 'd');
        Assertions.assertEquals('b', result4.left());
        Assertions.assertTrue(result4.right().isPresent());
        Assertions.assertEquals('c', result4.right().get());

        // Test odd number of elements
        Pair<Character, OptionalChar> result5 = Median.of('e', 'b', 'a', 'd', 'c');
        Assertions.assertEquals('c', result5.left());
        Assertions.assertFalse(result5.right().isPresent());
    }

    @Test
    public void testOfCharArrayWithRange() {
        char[] array = {'z', 'a', 'c', 'b', 'd', 'y'};
        
        // Test partial range
        Pair<Character, OptionalChar> result1 = Median.of(array, 1, 5);
        Assertions.assertEquals('b', result1.left());
        Assertions.assertTrue(result1.right().isPresent());
        Assertions.assertEquals('c', result1.right().get());

        // Test single element range
        Pair<Character, OptionalChar> result2 = Median.of(array, 2, 3);
        Assertions.assertEquals('c', result2.left());
        Assertions.assertFalse(result2.right().isPresent());

        // Test two element range
        Pair<Character, OptionalChar> result3 = Median.of(array, 0, 2);
        Assertions.assertEquals('a', result3.left());
        Assertions.assertTrue(result3.right().isPresent());
        Assertions.assertEquals('z', result3.right().get());
    }

    @Test
    public void testOfCharArrayExceptions() {
        // Test null array
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((char[]) null));
        
        // Test empty array
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new char[0]));
        
        // Test invalid range
        char[] array = {'a', 'b', 'c'};
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(array, 1, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Median.of(array, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Median.of(array, 1, 4));
    }

    // Tests for byte arrays
    @Test
    public void testOfByteArray() {
        // Test single element
        Pair<Byte, OptionalByte> result1 = Median.of((byte) 1);
        Assertions.assertEquals((byte) 1, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        // Test two elements
        Pair<Byte, OptionalByte> result2 = Median.of((byte) 1, (byte) 2);
        Assertions.assertEquals((byte) 1, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals((byte) 2, result2.right().get());

        // Test three elements
        Pair<Byte, OptionalByte> result3 = Median.of((byte) 3, (byte) 1, (byte) 2);
        Assertions.assertEquals((byte) 2, result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        // Test even number
        Pair<Byte, OptionalByte> result4 = Median.of((byte) 1, (byte) 3, (byte) 2, (byte) 4);
        Assertions.assertEquals((byte) 2, result4.left());
        Assertions.assertTrue(result4.right().isPresent());
        Assertions.assertEquals((byte) 3, result4.right().get());

        // Test with negative values
        Pair<Byte, OptionalByte> result5 = Median.of((byte) -2, (byte) -1, (byte) 0, (byte) 1, (byte) 2);
        Assertions.assertEquals((byte) 0, result5.left());
        Assertions.assertFalse(result5.right().isPresent());
    }

    @Test
    public void testOfByteArrayWithRange() {
        byte[] array = {5, 1, 3, 2, 4, 6};
        
        // Test partial range
        Pair<Byte, OptionalByte> result1 = Median.of(array, 1, 5);
        Assertions.assertEquals((byte) 2, result1.left());
        Assertions.assertTrue(result1.right().isPresent());
        Assertions.assertEquals((byte) 3, result1.right().get());

        // Test full range
        Pair<Byte, OptionalByte> result2 = Median.of(array, 0, 6);
        Assertions.assertEquals((byte) 3, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals((byte) 4, result2.right().get());
    }

    @Test
    public void testOfByteArrayExceptions() {
        // Test null array
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((byte[]) null));
        
        // Test empty array
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new byte[0]));
        
        // Test invalid range
        byte[] array = {1, 2, 3};
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(array, 2, 2));
    }

    // Tests for short arrays
    @Test
    public void testOfShortArray() {
        // Test single element
        Pair<Short, OptionalShort> result1 = Median.of((short) 100);
        Assertions.assertEquals((short) 100, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        // Test two elements
        Pair<Short, OptionalShort> result2 = Median.of((short) 100, (short) 200);
        Assertions.assertEquals((short) 100, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals((short) 200, result2.right().get());

        // Test three elements
        Pair<Short, OptionalShort> result3 = Median.of((short) 300, (short) 100, (short) 200);
        Assertions.assertEquals((short) 200, result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        // Test even number
        Pair<Short, OptionalShort> result4 = Median.of((short) 10, (short) 30, (short) 20, (short) 40);
        Assertions.assertEquals((short) 20, result4.left());
        Assertions.assertTrue(result4.right().isPresent());
        Assertions.assertEquals((short) 30, result4.right().get());
    }

    @Test
    public void testOfShortArrayWithRange() {
        short[] array = {50, 10, 30, 20, 40, 60};
        
        // Test partial range
        Pair<Short, OptionalShort> result = Median.of(array, 1, 5);
        Assertions.assertEquals((short) 20, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals((short) 30, result.right().get());
    }

    @Test
    public void testOfShortArrayExceptions() {
        // Test null array
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((short[]) null));
        
        // Test empty array
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new short[0]));
    }

    // Tests for int arrays
    @Test
    public void testOfIntArray() {
        // Test examples from documentation
        Pair<Integer, OptionalInt> result1 = Median.of(1);
        Assertions.assertEquals(1, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Integer, OptionalInt> result2 = Median.of(1, 3);
        Assertions.assertEquals(1, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals(3, result2.right().getAsInt());

        Pair<Integer, OptionalInt> result3 = Median.of(1, 3, 5);
        Assertions.assertEquals(3, result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        Pair<Integer, OptionalInt> result4 = Median.of(1, 1);
        Assertions.assertEquals(1, result4.left());
        Assertions.assertTrue(result4.right().isPresent());
        Assertions.assertEquals(1, result4.right().getAsInt());

        Pair<Integer, OptionalInt> result5 = Median.of(1, 1, 3);
        Assertions.assertEquals(1, result5.left());
        Assertions.assertFalse(result5.right().isPresent());

        Pair<Integer, OptionalInt> result6 = Median.of(1, 1, 3, 5);
        Assertions.assertEquals(1, result6.left());
        Assertions.assertTrue(result6.right().isPresent());
        Assertions.assertEquals(3, result6.right().getAsInt());

        Pair<Integer, OptionalInt> result7 = Median.of(1, 1, 1, 3, 5);
        Assertions.assertEquals(1, result7.left());
        Assertions.assertFalse(result7.right().isPresent());

        Pair<Integer, OptionalInt> result8 = Median.of(1, 1, 1, 3, 3, 5);
        Assertions.assertEquals(1, result8.left());
        Assertions.assertTrue(result8.right().isPresent());
        Assertions.assertEquals(3, result8.right().getAsInt());

        Pair<Integer, OptionalInt> result9 = Median.of(1, 1, 1, 3, 3, 3, 5);
        Assertions.assertEquals(3, result9.left());
        Assertions.assertFalse(result9.right().isPresent());
    }

    @Test
    public void testOfIntArrayWithRange() {
        int[] array = {9, 1, 5, 3, 7, 2, 8};
        
        // Test partial range (1, 5, 3, 7)
        Pair<Integer, OptionalInt> result1 = Median.of(array, 1, 5);
        Assertions.assertEquals(3, result1.left());
        Assertions.assertTrue(result1.right().isPresent());
        Assertions.assertEquals(5, result1.right().getAsInt());

        // Test three element range
        Pair<Integer, OptionalInt> result2 = Median.of(array, 2, 5);
        Assertions.assertEquals(5, result2.left());
        Assertions.assertFalse(result2.right().isPresent());
    }

    @Test
    public void testOfIntArrayExceptions() {
        // Test null array
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((int[]) null));
        
        // Test empty array
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new int[0]));
        
        // Test invalid range
        int[] array = {1, 2, 3};
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Median.of(array, 0, 4));
    }

    // Tests for long arrays
    @Test
    public void testOfLongArray() {
        // Test single element
        Pair<Long, OptionalLong> result1 = Median.of(1000L);
        Assertions.assertEquals(1000L, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        // Test two elements
        Pair<Long, OptionalLong> result2 = Median.of(1000L, 2000L);
        Assertions.assertEquals(1000L, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals(2000L, result2.right().getAsLong());

        // Test three elements
        Pair<Long, OptionalLong> result3 = Median.of(3000L, 1000L, 2000L);
        Assertions.assertEquals(2000L, result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        // Test even number with large values
        Pair<Long, OptionalLong> result4 = Median.of(Long.MAX_VALUE, Long.MIN_VALUE, 0L, 1000L);
        Assertions.assertEquals(0L, result4.left());
        Assertions.assertTrue(result4.right().isPresent());
        Assertions.assertEquals(1000L, result4.right().getAsLong());
    }

    @Test
    public void testOfLongArrayWithRange() {
        long[] array = {5000L, 1000L, 3000L, 2000L, 4000L, 6000L};
        
        // Test partial range
        Pair<Long, OptionalLong> result = Median.of(array, 1, 5);
        Assertions.assertEquals(2000L, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(3000L, result.right().getAsLong());
    }

    @Test
    public void testOfLongArrayExceptions() {
        // Test null array
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((long[]) null));
        
        // Test empty array
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new long[0]));
    }

    // Tests for float arrays
    @Test
    public void testOfFloatArray() {
        // Test single element
        Pair<Float, OptionalFloat> result1 = Median.of(1.5f);
        Assertions.assertEquals(1.5f, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        // Test two elements
        Pair<Float, OptionalFloat> result2 = Median.of(1.5f, 2.5f);
        Assertions.assertEquals(1.5f, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals(2.5f, result2.right().get());

        // Test three elements
        Pair<Float, OptionalFloat> result3 = Median.of(3.5f, 1.5f, 2.5f);
        Assertions.assertEquals(2.5f, result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        // Test with NaN and infinity
        Pair<Float, OptionalFloat> result4 = Median.of(Float.NaN, 1.0f, Float.POSITIVE_INFINITY);
        Assertions.assertEquals(Float.POSITIVE_INFINITY, result4.left());
        Assertions.assertFalse(result4.right().isPresent());
    }

    @Test
    public void testOfFloatArrayWithRange() {
        float[] array = {5.5f, 1.5f, 3.5f, 2.5f, 4.5f, 6.5f};
        
        // Test partial range
        Pair<Float, OptionalFloat> result = Median.of(array, 1, 5);
        Assertions.assertEquals(2.5f, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(3.5f, result.right().get());
    }

    @Test
    public void testOfFloatArrayExceptions() {
        // Test null array
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((float[]) null));
        
        // Test empty array
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new float[0]));
    }

    // Tests for double arrays
    @Test
    public void testOfDoubleArray() {
        // Test single element
        Pair<Double, OptionalDouble> result1 = Median.of(1.0);
        Assertions.assertEquals(1.0, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        // Test two elements
        Pair<Double, OptionalDouble> result2 = Median.of(1.0, 2.0);
        Assertions.assertEquals(1.0, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals(2.0, result2.right().getAsDouble());

        // Test three elements
        Pair<Double, OptionalDouble> result3 = Median.of(3.0, 1.0, 2.0);
        Assertions.assertEquals(2.0, result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        // Test even number
        Pair<Double, OptionalDouble> result4 = Median.of(1.1, 3.3, 2.2, 4.4);
        Assertions.assertEquals(2.2, result4.left());
        Assertions.assertTrue(result4.right().isPresent());
        Assertions.assertEquals(3.3, result4.right().getAsDouble());

        // Test with special values
        Pair<Double, OptionalDouble> result5 = Median.of(Double.NEGATIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY);
        Assertions.assertEquals(0.0, result5.left());
        Assertions.assertFalse(result5.right().isPresent());
    }

    @Test
    public void testOfDoubleArrayWithRange() {
        double[] array = {5.0, 1.0, 3.0, 2.0, 4.0, 6.0};
        
        // Test partial range
        Pair<Double, OptionalDouble> result = Median.of(array, 1, 5);
        Assertions.assertEquals(2.0, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(3.0, result.right().getAsDouble());
    }

    @Test
    public void testOfDoubleArrayExceptions() {
        // Test null array
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((double[]) null));
        
        // Test empty array
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new double[0]));
    }

    // Tests for generic object arrays
    @Test
    public void testOfGenericArray() {
        // Test with Strings
        Pair<String, Optional<String>> result1 = Median.of(new String[]{"apple"});
        Assertions.assertEquals("apple", result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<String, Optional<String>> result2 = Median.of(new String[]{"apple", "banana"});
        Assertions.assertEquals("apple", result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals("banana", result2.right().get());

        Pair<String, Optional<String>> result3 = Median.of(new String[]{"banana", "apple", "cherry"});
        Assertions.assertEquals("banana", result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        // Test with Integers
        Pair<Integer, Optional<Integer>> result4 = Median.of(new Integer[]{3, 1, 4, 1, 5});
        Assertions.assertEquals(3, result4.left());
        Assertions.assertFalse(result4.right().isPresent());
    }

    @Test
    public void testOfGenericArrayWithRange() {
        String[] array = {"zebra", "apple", "cherry", "banana", "date", "fig"};
        
        // Test partial range
        Pair<String, Optional<String>> result = Median.of(array, 1, 5);
        Assertions.assertEquals("banana", result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals("cherry", result.right().get());
    }

    @Test
    public void testOfGenericArrayWithComparator() {
        // Test with custom comparator (reverse order)
        Integer[] array = {1, 2, 3, 4, 5};
        Comparator<Integer> reverseComparator = Comparator.reverseOrder();
        
        Pair<Integer, Optional<Integer>> result1 = Median.of(array, reverseComparator);
        Assertions.assertEquals(3, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        // Test with case-insensitive string comparator
        String[] strings = {"APPLE", "banana", "Cherry", "date"};
        Comparator<String> caseInsensitive = String.CASE_INSENSITIVE_ORDER;
        
        Pair<String, Optional<String>> result2 = Median.of(strings, caseInsensitive);
        Assertions.assertEquals("banana", result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals("Cherry", result2.right().get());
    }

    @Test
    public void testOfGenericArrayWithRangeAndComparator() {
        Integer[] array = {9, 1, 5, 3, 7, 2, 8};
        Comparator<Integer> reverseComparator = Comparator.reverseOrder();
        
        // Test partial range with reverse comparator
        Pair<Integer, Optional<Integer>> result = Median.of(array, 1, 5, reverseComparator);
        Assertions.assertEquals(5, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(3, result.right().get());
    }

    @Test
    public void testOfGenericArrayExceptions() {
        // Test null array
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((String[]) null));
        
        // Test empty array
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new String[0]));
        
        // Test invalid range
        String[] array = {"a", "b", "c"};
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Median.of(array, -1, 2));
    }

    // Tests for collections
    @Test
    public void testOfCollection() {
        // Test with single element
        List<Integer> list1 = Arrays.asList(5);
        Pair<Integer, Optional<Integer>> result1 = Median.of(list1);
        Assertions.assertEquals(5, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        // Test with two elements
        List<Integer> list2 = Arrays.asList(5, 10);
        Pair<Integer, Optional<Integer>> result2 = Median.of(list2);
        Assertions.assertEquals(5, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals(10, result2.right().get());

        // Test with three elements
        List<Integer> list3 = Arrays.asList(10, 5, 15);
        Pair<Integer, Optional<Integer>> result3 = Median.of(list3);
        Assertions.assertEquals(10, result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        // Test with even number
        List<Integer> list4 = Arrays.asList(4, 2, 6, 8);
        Pair<Integer, Optional<Integer>> result4 = Median.of(list4);
        Assertions.assertEquals(4, result4.left());
        Assertions.assertTrue(result4.right().isPresent());
        Assertions.assertEquals(6, result4.right().get());

        // Test with strings
        List<String> stringList = Arrays.asList("dog", "cat", "bird", "ant", "elephant");
        Pair<String, Optional<String>> result5 = Median.of(stringList);
        Assertions.assertEquals("cat", result5.left());
        Assertions.assertFalse(result5.right().isPresent());
    }

    @Test
    public void testOfCollectionWithComparator() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);
        Comparator<Integer> reverseComparator = Comparator.reverseOrder();
        
        Pair<Integer, Optional<Integer>> result = Median.of(list, reverseComparator);
        Assertions.assertEquals(4, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(3, result.right().get());

        // Test with custom objects
        List<String> strings = Arrays.asList("AAA", "bbb", "CCC", "ddd");
        Comparator<String> caseInsensitive = String.CASE_INSENSITIVE_ORDER;
        
        Pair<String, Optional<String>> result2 = Median.of(strings, caseInsensitive);
        Assertions.assertEquals("bbb", result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals("CCC", result2.right().get());
    }

    @Test
    public void testOfCollectionWithRange() {
        List<Integer> list = Arrays.asList(9, 1, 5, 3, 7, 2, 8);
        
        // Test partial range
        Pair<Integer, Optional<Integer>> result = Median.of(list, 1, 5);
        Assertions.assertEquals(3, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(5, result.right().get());

        // Test single element range
        Pair<Integer, Optional<Integer>> result2 = Median.of(list, 2, 3);
        Assertions.assertEquals(5, result2.left());
        Assertions.assertFalse(result2.right().isPresent());
    }

    @Test
    public void testOfCollectionWithRangeAndComparator() {
        List<String> list = Arrays.asList("zebra", "apple", "cherry", "banana", "date", "fig");
        Comparator<String> reverseComparator = Comparator.reverseOrder();
        
        // Test partial range with custom comparator
        Pair<String, Optional<String>> result = Median.of(list, 1, 5, reverseComparator);
        Assertions.assertEquals("cherry", result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals("banana", result.right().get());
    }

    @Test
    public void testOfCollectionExceptions() {
        // Test null collection
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((List<Integer>) null));
        
        // Test empty collection
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new ArrayList<Integer>()));
        
        // Test invalid range
        List<Integer> list = Arrays.asList(1, 2, 3);
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Median.of(list, 0, 4));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(list, 1, 1));
    }

    // Test edge cases and special scenarios
    @Test
    public void testDuplicateValues() {
        // Test with all same values
        Pair<Integer, OptionalInt> result1 = Median.of(5, 5, 5, 5);
        Assertions.assertEquals(5, result1.left());
        Assertions.assertTrue(result1.right().isPresent());
        Assertions.assertEquals(5, result1.right().getAsInt());

        // Test with mostly duplicates
        Pair<Integer, OptionalInt> result2 = Median.of(1, 1, 1, 2);
        Assertions.assertEquals(1, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals(1, result2.right().getAsInt());
    }

    @Test
    public void testLargeArrayPerformance() {
        // Test with larger array to ensure algorithm efficiency
        int[] largeArray = new int[1000];
        for (int i = 0; i < 1000; i++) {
            largeArray[i] = i;
        }
        
        Pair<Integer, OptionalInt> result = Median.of(largeArray);
        Assertions.assertEquals(499, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(500, result.right().getAsInt());
    }

    @Test
    public void testNullComparator() {
        // Test that null comparator defaults to natural ordering
        Integer[] array = {3, 1, 4, 1, 5};
        Pair<Integer, Optional<Integer>> result = Median.of(array, null);
        Assertions.assertEquals(3, result.left());
        Assertions.assertFalse(result.right().isPresent());

        List<Integer> list = Arrays.asList(3, 1, 4, 1, 5);
        Pair<Integer, Optional<Integer>> result2 = Median.of(list, null);
        Assertions.assertEquals(3, result2.left());
        Assertions.assertFalse(result2.right().isPresent());
    }
}