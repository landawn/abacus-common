package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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

public class MedianTest extends TestBase {

    @Test
    public void testMedianOfCharArray() {
        Pair<Character, OptionalChar> result1 = Median.of('a', 'c', 'b');
        assertEquals('b', result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Character, OptionalChar> result2 = Median.of('a', 'b', 'c', 'd');
        assertEquals('b', result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        assertEquals('c', result2.right().get());

        Pair<Character, OptionalChar> result3 = Median.of('x');
        assertEquals('x', result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        Pair<Character, OptionalChar> result4 = Median.of('z', 'a');
        assertEquals('a', result4.left());
        Assertions.assertTrue(result4.right().isPresent());
        assertEquals('z', result4.right().get());

        Pair<Character, OptionalChar> result5 = Median.of('a', 'a', 'b', 'b');
        assertEquals('a', result5.left());
        Assertions.assertTrue(result5.right().isPresent());
        assertEquals('b', result5.right().get());
    }

    @Test
    public void testMedianOfCharArrayWithRange() {
        char[] arr = { 'a', 'z', 'b', 'y', 'c', 'x' };

        Pair<Character, OptionalChar> result1 = Median.of(arr, 1, 4);
        assertEquals('y', result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Character, OptionalChar> result2 = Median.of(arr, 2, 6);
        assertEquals('c', result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        assertEquals('x', result2.right().get());

        Pair<Character, OptionalChar> result3 = Median.of(arr, 0, arr.length);
        assertEquals('c', result3.left());
        Assertions.assertTrue(result3.right().isPresent());
        assertEquals('x', result3.right().get());
    }

    @Test
    public void testMedianOfByteArray() {
        Pair<Byte, OptionalByte> result1 = Median.of((byte) 1, (byte) 3, (byte) 2);
        assertEquals((byte) 2, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Byte, OptionalByte> result2 = Median.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        assertEquals((byte) 2, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        assertEquals((byte) 3, result2.right().get());

        Pair<Byte, OptionalByte> result3 = Median.of((byte) -5, (byte) -1, (byte) -3);
        assertEquals((byte) -3, result3.left());
        Assertions.assertFalse(result3.right().isPresent());
    }

    @Test
    public void testMedianOfByteArrayWithRange() {
        byte[] arr = { 10, 20, 30, 40, 50, 60 };

        Pair<Byte, OptionalByte> result = Median.of(arr, 1, 5);
        assertEquals((byte) 30, result.left());
        Assertions.assertTrue(result.right().isPresent());
        assertEquals((byte) 40, result.right().get());
    }

    @Test
    public void testMedianOfShortArray() {
        Pair<Short, OptionalShort> result1 = Median.of((short) 100, (short) 300, (short) 200);
        assertEquals((short) 200, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Short, OptionalShort> result2 = Median.of((short) 10, (short) 20, (short) 30, (short) 40);
        assertEquals((short) 20, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        assertEquals((short) 30, result2.right().get());
    }

    @Test
    public void testMedianOfShortArrayWithRange() {
        short[] arr = { 100, 200, 300, 400, 500 };

        Pair<Short, OptionalShort> result = Median.of(arr, 0, 3);
        assertEquals((short) 200, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testMedianOfIntArrayWithRange() {
        int[] arr = { 5, 1, 3, 9, 7, 2, 8 };

        Pair<Integer, OptionalInt> result1 = Median.of(arr, 1, 4);
        assertEquals(3, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Integer, OptionalInt> result2 = Median.of(arr, 2, 6);
        assertEquals(3, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        assertEquals(7, result2.right().getAsInt());
    }

    @Test
    public void testMedianOfLongArrayWithRange() {
        long[] arr = { 10L, 20L, 30L, 40L, 50L };

        Pair<Long, OptionalLong> result = Median.of(arr, 1, 4);
        assertEquals(30L, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testMedianOfFloatArray() {
        Pair<Float, OptionalFloat> result1 = Median.of(1.5f, 3.5f, 2.5f);
        assertEquals(2.5f, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Float, OptionalFloat> result2 = Median.of(1.0f, 2.0f, 3.0f, 4.0f);
        assertEquals(2.0f, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        assertEquals(3.0f, result2.right().get());

        Pair<Float, OptionalFloat> result3 = Median.of(-5.5f, -1.5f, -3.5f);
        assertEquals(-3.5f, result3.left());
        Assertions.assertFalse(result3.right().isPresent());
    }

    @Test
    public void testMedianOfFloatArrayWithRange() {
        float[] arr = { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f };

        Pair<Float, OptionalFloat> result = Median.of(arr, 0, 2);
        assertEquals(1.1f, result.left());
        Assertions.assertTrue(result.right().isPresent());
        assertEquals(2.2f, result.right().get());
    }

    @Test
    public void testMedianOfDoubleArrayWithRange() {
        double[] arr = { 1.1, 2.2, 3.3, 4.4, 5.5 };

        Pair<Double, OptionalDouble> result = Median.of(arr, 1, 5);
        assertEquals(3.3, result.left());
        Assertions.assertTrue(result.right().isPresent());
        assertEquals(4.4, result.right().getAsDouble());
    }

    @Test
    public void testMedianOfGenericArrayWithRange() {
        Integer[] arr = { 10, 20, 30, 40, 50, 60 };

        Pair<Integer, Optional<Integer>> result = Median.of(arr, 1, 4);
        assertEquals(30, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testMedianOfGenericArrayWithComparator() {
        Integer[] arr = { 5, 1, 3, 2, 4 };
        Pair<Integer, Optional<Integer>> result1 = Median.of(arr, Comparator.reverseOrder());
        assertEquals(3, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        String[] strArr = { "aa", "b", "ccc", "dd" };
        Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
        Pair<String, Optional<String>> result2 = Median.of(strArr, lengthComparator);
        assertEquals("aa", result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        assertEquals("dd", result2.right().get());
    }

    @Test
    public void testMedianOfGenericArrayWithRangeAndComparator() {
        String[] arr = { "apple", "banana", "cherry", "date", "elderberry", "fig" };

        Pair<String, Optional<String>> result = Median.of(arr, 1, 5, Comparator.naturalOrder());
        assertEquals("cherry", result.left());
        Assertions.assertTrue(result.right().isPresent());
        assertEquals("date", result.right().get());
    }

    @Test
    public void testMedianOfCollection() {
        List<Integer> list1 = Arrays.asList(5, 1, 3, 2, 4);
        Pair<Integer, Optional<Integer>> result1 = Median.of(list1);
        assertEquals(3, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        List<Integer> list2 = Arrays.asList(1, 2, 3, 4);
        Pair<Integer, Optional<Integer>> result2 = Median.of(list2);
        assertEquals(2, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        assertEquals(3, result2.right().get());

        Set<Integer> set = new TreeSet<>(Arrays.asList(10, 20, 30));
        Pair<Integer, Optional<Integer>> result3 = Median.of(set);
        assertEquals(20, result3.left());
        Assertions.assertFalse(result3.right().isPresent());
    }

    @Test
    public void testMedianOfCollectionWithComparator() {
        List<String> list = Arrays.asList("aa", "b", "ccc", "dd");
        Comparator<String> lengthComparator = Comparator.comparingInt(String::length);

        Pair<String, Optional<String>> result = Median.of(list, lengthComparator);
        assertEquals("aa", result.left());
        Assertions.assertTrue(result.right().isPresent());
        assertEquals("dd", result.right().get());
    }

    @Test
    public void testMedianOfCollectionWithRange() {
        List<Integer> list = Arrays.asList(10, 20, 30, 40, 50);

        Pair<Integer, Optional<Integer>> result = Median.of(list, 1, 4);
        assertEquals(30, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testMedianOfCollectionWithRangeAndComparator() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date", "elderberry");

        Pair<String, Optional<String>> result = Median.of(list, 0, 4, Comparator.reverseOrder());
        assertEquals("cherry", result.left());
        Assertions.assertTrue(result.right().isPresent());
        assertEquals("banana", result.right().get());
    }

    @Test
    public void testMedianWithLargeArrays() {
        int[] largeArr = new int[1000];
        for (int i = 0; i < 1000; i++) {
            largeArr[i] = i;
        }

        Pair<Integer, OptionalInt> result = Median.of(largeArr);
        assertEquals(499, result.left());
        Assertions.assertTrue(result.right().isPresent());
        assertEquals(500, result.right().getAsInt());
    }

    @Test
    public void testMedianWithDuplicateValues() {
        int[] allDupes = { 5, 5, 5, 5, 5 };
        Pair<Integer, OptionalInt> result1 = Median.of(allDupes);
        assertEquals(5, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        int[] mostlyDupes = { 1, 2, 2, 2, 2, 2, 3 };
        Pair<Integer, OptionalInt> result2 = Median.of(mostlyDupes);
        assertEquals(2, result2.left());
        Assertions.assertFalse(result2.right().isPresent());
    }

    @Test
    public void testMedianConsistencyAcrossTypes() {
        Pair<Integer, OptionalInt> intResult = Median.of(1, 2, 3, 4, 5);
        Pair<Long, OptionalLong> longResult = Median.of(1L, 2L, 3L, 4L, 5L);
        Pair<Double, OptionalDouble> doubleResult = Median.of(1.0, 2.0, 3.0, 4.0, 5.0);

        assertEquals(3, intResult.left().intValue());
        assertEquals(3L, longResult.left().longValue());
        assertEquals(3.0, doubleResult.left().doubleValue());

        Assertions.assertFalse(intResult.right().isPresent());
        Assertions.assertFalse(longResult.right().isPresent());
        Assertions.assertFalse(doubleResult.right().isPresent());
    }

    @Test
    public void testMedianOfIntArray() {
        Pair<Integer, OptionalInt> result1 = Median.of(50, 10, 30);
        assertEquals(30, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Integer, OptionalInt> result2 = Median.of(10, 40, 20, 30);
        assertEquals(20, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        assertEquals(30, result2.right().getAsInt());

        Pair<Integer, OptionalInt> result3 = Median.of(5, 3, 1, 4, 2);
        assertEquals(3, result3.left());
        Assertions.assertFalse(result3.right().isPresent());
    }

    @Test
    public void testMedianOfLongArray() {
        Pair<Long, OptionalLong> result1 = Median.of(1000L, 3000L, 2000L);
        assertEquals(2000L, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Long, OptionalLong> result2 = Median.of(1L, 2L, 3L, 4L);
        assertEquals(2L, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        assertEquals(3L, result2.right().getAsLong());

        Pair<Long, OptionalLong> result3 = Median.of(Long.MAX_VALUE - 2, Long.MAX_VALUE - 1, Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE - 1, result3.left());
        Assertions.assertFalse(result3.right().isPresent());
    }

    @Test
    public void testMedianOfDoubleArray() {
        Pair<Double, OptionalDouble> result1 = Median.of(1.5, 3.5, 2.5);
        assertEquals(2.5, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Double, OptionalDouble> result2 = Median.of(1.0, 2.0, 3.0, 4.0);
        assertEquals(2.0, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        assertEquals(3.0, result2.right().getAsDouble());

        Pair<Double, OptionalDouble> result3 = Median.of(1.0, Double.POSITIVE_INFINITY, 2.0);
        assertEquals(2.0, result3.left());
        Assertions.assertFalse(result3.right().isPresent());
    }

    @Test
    public void testMedianOfGenericArray() {
        Integer[] intArr = { 5, 1, 3, 2, 4 };
        Pair<Integer, Optional<Integer>> result1 = Median.of(intArr);
        assertEquals(3, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        String[] strArr = { "apple", "banana", "cherry", "date" };
        Pair<String, Optional<String>> result2 = Median.of(strArr);
        assertEquals("banana", result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        assertEquals("cherry", result2.right().get());

        String[] singleArr = { "test" };
        Pair<String, Optional<String>> result3 = Median.of(singleArr);
        assertEquals("test", result3.left());
        Assertions.assertFalse(result3.right().isPresent());
    }

    @Test
    public void testMedianWithNullComparator() {
        Integer[] arr = { 3, 1, 2 };
        Pair<Integer, Optional<Integer>> result = Median.of(arr, null);
        assertEquals(2, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testMedianOfCharArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((char[]) null);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new char[0]);
        });

        char[] arr = { 'a', 'b', 'c' };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            Median.of(arr, -1, 2);
        });

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            Median.of(arr, 0, 4);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(arr, 1, 1);
        });
    }

    @Test
    public void testMedianOfByteArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((byte[]) null);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new byte[0]);
        });
    }

    @Test
    public void testMedianOfShortArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((short[]) null);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new short[0]);
        });
    }

    @Test
    public void testMedianOfIntArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((int[]) null);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new int[0]);
        });

        int[] arr = { 1, 2, 3 };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            Median.of(arr, -1, 2);
        });
    }

    @Test
    public void testMedianOfLongArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((long[]) null);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new long[0]);
        });
    }

    @Test
    public void testMedianOfFloatArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((float[]) null);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new float[0]);
        });
    }

    @Test
    public void testMedianOfDoubleArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((double[]) null);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new double[0]);
        });
    }

    @Test
    public void testMedianOfGenericArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((Integer[]) null);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new String[0]);
        });

        String[] arr = { "a", "b", "c" };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            Median.of(arr, 0, 4);
        });
    }

    @Test
    public void testMedianOfCollectionExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((List<Integer>) null);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new ArrayList<String>());
        });

        List<Integer> list = Arrays.asList(1, 2, 3);
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            Median.of(list, -1, 2);
        });

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            Median.of(list, 0, 4);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(list, 1, 1);
        });
    }

    @Test
    public void testOfChar_TwoElements_Sorted() {
        Pair<Character, OptionalChar> result = Median.of('a', 'c');
        Assertions.assertEquals('a', result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals('c', result.right().get());
    }

    @Test
    public void testOfChar_TwoElements_Unsorted() {
        Pair<Character, OptionalChar> result = Median.of('c', 'a');
        Assertions.assertEquals('a', result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals('c', result.right().get());
    }

    @Test
    public void testOfChar_ThreeElements() {
        Pair<Character, OptionalChar> result = Median.of('a', 'c', 'b');
        Assertions.assertEquals('b', result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfChar_FourElements() {
        Pair<Character, OptionalChar> result = Median.of('a', 'c', 'b', 'd');
        Assertions.assertEquals('b', result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals('c', result.right().get());
    }

    @Test
    public void testOfChar_FiveElements() {
        Pair<Character, OptionalChar> result = Median.of('z', 'a', 'm', 'x', 'k');
        Assertions.assertEquals('m', result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfChar_SixElements() {
        Pair<Character, OptionalChar> result = Median.of('f', 'a', 'c', 'e', 'b', 'd');
        Assertions.assertEquals('c', result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals('d', result.right().get());
    }

    @Test
    public void testOfCharRange_TwoElements() {
        char[] chars = { 'a', 'b', 'c' };
        Pair<Character, OptionalChar> result = Median.of(chars, 0, 2);
        Assertions.assertEquals('a', result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals('b', result.right().get());
    }

    @Test
    public void testOfCharRange_ThreeElements() {
        char[] chars = { 'a', 'b', 'c' };
        Pair<Character, OptionalChar> result = Median.of(chars, 0, 3);
        Assertions.assertEquals('b', result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfCharRange_FourElements() {
        char[] chars = { 'a', 'b', 'c', 'd' };
        Pair<Character, OptionalChar> result = Median.of(chars, 0, 4);
        Assertions.assertEquals('b', result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals('c', result.right().get());
    }

    @Test
    public void testOfCharRange_Subarray() {
        char[] chars = { 'z', 'a', 'm', 'x' };
        Pair<Character, OptionalChar> result = Median.of(chars, 1, 3);
        Assertions.assertEquals('a', result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals('m', result.right().get());
    }

    @Test
    public void testOfByte_TwoElements() {
        Pair<Byte, OptionalByte> result = Median.of((byte) 10, (byte) 20);
        Assertions.assertEquals((byte) 10, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals((byte) 20, result.right().get());
    }

    @Test
    public void testOfByte_ThreeElements() {
        Pair<Byte, OptionalByte> result = Median.of((byte) 10, (byte) 5, (byte) 15);
        Assertions.assertEquals((byte) 10, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfByte_FourElements() {
        Pair<Byte, OptionalByte> result = Median.of((byte) 10, (byte) 20, (byte) 5, (byte) 15);
        Assertions.assertEquals((byte) 10, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals((byte) 15, result.right().get());
    }

    @Test
    public void testOfByteRange_TwoElements() {
        byte[] bytes = { 30, 10, 20, 40 };
        Pair<Byte, OptionalByte> result = Median.of(bytes, 1, 3);
        Assertions.assertEquals((byte) 10, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals((byte) 20, result.right().get());
    }

    @Test
    public void testOfByteRange_ThreeElements() {
        byte[] bytes = { 30, 10, 20, 40 };
        Pair<Byte, OptionalByte> result = Median.of(bytes, 0, 3);
        Assertions.assertEquals((byte) 20, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfShort_TwoElements() {
        Pair<Short, OptionalShort> result = Median.of((short) 100, (short) 200);
        Assertions.assertEquals((short) 100, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals((short) 200, result.right().get());
    }

    @Test
    public void testOfShort_ThreeElements() {
        Pair<Short, OptionalShort> result = Median.of((short) 100, (short) 50, (short) 200);
        Assertions.assertEquals((short) 100, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfShort_FourElements() {
        Pair<Short, OptionalShort> result = Median.of((short) 100, (short) 200, (short) 50, (short) 150);
        Assertions.assertEquals((short) 100, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals((short) 150, result.right().get());
    }

    @Test
    public void testOfShortRange_TwoElements() {
        short[] values = { 300, 100, 200, 400 };
        Pair<Short, OptionalShort> result = Median.of(values, 1, 3);
        Assertions.assertEquals((short) 100, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals((short) 200, result.right().get());
    }

    @Test
    public void testOfShortRange_ThreeElements() {
        short[] values = { 300, 100, 200, 400 };
        Pair<Short, OptionalShort> result = Median.of(values, 0, 3);
        Assertions.assertEquals((short) 200, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfInt_TwoElements() {
        Pair<Integer, OptionalInt> result = Median.of(10, 20);
        Assertions.assertEquals(10, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(20, result.right().get());
    }

    @Test
    public void testOfInt_ThreeElements() {
        Pair<Integer, OptionalInt> result = Median.of(10, 5, 20);
        Assertions.assertEquals(10, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfInt_FourElements() {
        Pair<Integer, OptionalInt> result = Median.of(10, 5, 20, 15);
        Assertions.assertEquals(10, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(15, result.right().get());
    }

    @Test
    public void testOfInt_FiveElements() {
        Pair<Integer, OptionalInt> result = Median.of(50, 10, 30, 20, 40);
        Assertions.assertEquals(30, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfInt_SixElements() {
        Pair<Integer, OptionalInt> result = Median.of(60, 10, 30, 20, 50, 40);
        Assertions.assertEquals(30, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(40, result.right().get());
    }

    @Test
    public void testOfIntRange_TwoElements() {
        int[] numbers = { 100, 50, 75, 25, 90 };
        Pair<Integer, OptionalInt> result = Median.of(numbers, 0, 2);
        Assertions.assertEquals(50, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(100, result.right().get());
    }

    @Test
    public void testOfIntRange_ThreeElements() {
        int[] numbers = { 100, 50, 75, 25, 90 };
        Pair<Integer, OptionalInt> result = Median.of(numbers, 1, 4);
        Assertions.assertEquals(50, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfLong_TwoElements() {
        Pair<Long, OptionalLong> result = Median.of(1000L, 2000L);
        Assertions.assertEquals(1000L, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(2000L, result.right().get());
    }

    @Test
    public void testOfLong_ThreeElements() {
        Pair<Long, OptionalLong> result = Median.of(1000L, 500L, 1500L);
        Assertions.assertEquals(1000L, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfLong_FourElements() {
        Pair<Long, OptionalLong> result = Median.of(1000L, 500L, 1500L, 750L);
        Assertions.assertEquals(750L, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(1000L, result.right().get());
    }

    @Test
    public void testOfLongRange_TwoElements() {
        long[] values = { 3000L, 1000L, 2000L, 4000L, 1500L };
        Pair<Long, OptionalLong> result = Median.of(values, 1, 3);
        Assertions.assertEquals(1000L, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(2000L, result.right().get());
    }

    @Test
    public void testOfLongRange_ThreeElements() {
        long[] values = { 3000L, 1000L, 2000L, 4000L, 1500L };
        Pair<Long, OptionalLong> result = Median.of(values, 1, 4);
        Assertions.assertEquals(2000L, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfFloat_TwoElements() {
        Pair<Float, OptionalFloat> result = Median.of(10.5f, 20.8f);
        Assertions.assertEquals(10.5f, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(20.8f, result.right().get());
    }

    @Test
    public void testOfFloat_ThreeElements() {
        Pair<Float, OptionalFloat> result = Median.of(10.5f, 5.2f, 20.8f);
        Assertions.assertEquals(10.5f, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfFloat_FourElements() {
        Pair<Float, OptionalFloat> result = Median.of(10.5f, 5.2f, 20.8f, 15.1f);
        Assertions.assertEquals(10.5f, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(15.1f, result.right().get());
    }

    @Test
    public void testOfFloatRange_TwoElements() {
        float[] values = { 30.5f, 10.2f, 20.8f, 40.1f };
        Pair<Float, OptionalFloat> result = Median.of(values, 1, 3);
        Assertions.assertEquals(10.2f, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(20.8f, result.right().get());
    }

    @Test
    public void testOfFloatRange_ThreeElements() {
        float[] values = { 30.5f, 10.2f, 20.8f, 40.1f };
        Pair<Float, OptionalFloat> result = Median.of(values, 0, 3);
        Assertions.assertEquals(20.8f, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfDouble_TwoElements() {
        Pair<Double, OptionalDouble> result = Median.of(10.5, 20.8);
        Assertions.assertEquals(10.5, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(20.8, result.right().get());
    }

    @Test
    public void testOfDouble_ThreeElements() {
        Pair<Double, OptionalDouble> result = Median.of(10.5, 5.2, 20.8);
        Assertions.assertEquals(10.5, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfDouble_FourElements() {
        Pair<Double, OptionalDouble> result = Median.of(10.5, 5.2, 20.8, 15.1);
        Assertions.assertEquals(10.5, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(15.1, result.right().get());
    }

    @Test
    public void testOfDoubleRange_TwoElements() {
        double[] values = { 30.5, 10.2, 20.8, 40.1, 15.3 };
        Pair<Double, OptionalDouble> result = Median.of(values, 1, 3);
        Assertions.assertEquals(10.2, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(20.8, result.right().get());
    }

    @Test
    public void testOfDoubleRange_ThreeElements() {
        double[] values = { 30.5, 10.2, 20.8, 40.1, 15.3 };
        Pair<Double, OptionalDouble> result = Median.of(values, 1, 4);
        Assertions.assertEquals(20.8, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfComparableArray_TwoElements() {
        String[] words = { "apple", "banana" };
        Pair<String, Optional<String>> result = Median.of(words);
        Assertions.assertEquals("apple", result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals("banana", result.right().get());
    }

    @Test
    public void testOfComparableArray_ThreeElements() {
        String[] words = { "apple", "banana", "cherry" };
        Pair<String, Optional<String>> result = Median.of(words);
        Assertions.assertEquals("banana", result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfComparableArray_FourElements() {
        String[] words = { "apple", "banana", "cherry", "date" };
        Pair<String, Optional<String>> result = Median.of(words);
        Assertions.assertEquals("banana", result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals("cherry", result.right().get());
    }

    @Test
    public void testOfComparableArrayRange_TwoElements() {
        String[] words = { "zebra", "apple", "banana", "cherry" };
        Pair<String, Optional<String>> result = Median.of(words, 1, 3);
        Assertions.assertEquals("apple", result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals("banana", result.right().get());
    }

    @Test
    public void testOfComparableArrayRange_ThreeElements() {
        String[] words = { "zebra", "apple", "banana", "cherry" };
        Pair<String, Optional<String>> result = Median.of(words, 1, 4);
        Assertions.assertEquals("banana", result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfArrayWithComparator_TwoElements() {
        String[] words = { "pie", "apple" };
        Pair<String, Optional<String>> result = Median.of(words, Comparator.comparing(String::length));
        Assertions.assertEquals("pie", result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals("apple", result.right().get());
    }

    @Test
    public void testOfArrayWithComparator_ThreeElements() {
        String[] words = { "apple", "pie", "banana" };
        Pair<String, Optional<String>> result = Median.of(words, Comparator.comparing(String::length));
        Assertions.assertEquals("apple", result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfArrayWithComparator_FourElements() {
        String[] words = { "cat", "dog", "apple", "banana" };
        Pair<String, Optional<String>> result = Median.of(words, Comparator.comparing(String::length));
        Assertions.assertEquals("dog", result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals("apple", result.right().get());
    }

    @Test
    public void testOfArrayRangeWithComparator_TwoElements() {
        String[] words = { "elephant", "ant", "bee", "tiger" };
        Pair<String, Optional<String>> result = Median.of(words, 1, 3, Comparator.comparing(String::length));
        Assertions.assertEquals("ant", result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals("bee", result.right().get());
    }

    @Test
    public void testOfArrayRangeWithComparator_ThreeElements() {
        String[] words = { "elephant", "ant", "bee", "tiger" };
        Pair<String, Optional<String>> result = Median.of(words, 1, 4, Comparator.comparing(String::length));
        Assertions.assertEquals("bee", result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfArrayRangeWithComparator_FourElements() {
        String[] words = { "a", "bb", "ccc", "dddd", "eeeee" };
        Pair<String, Optional<String>> result = Median.of(words, 1, 5, Comparator.comparing(String::length));
        Assertions.assertEquals("ccc", result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals("dddd", result.right().get());
    }

    @Test
    public void testOfCollection_TwoElements() {
        List<Integer> numbers = Arrays.asList(10, 20);
        Pair<Integer, Optional<Integer>> result = Median.of(numbers);
        Assertions.assertEquals(10, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(20, result.right().get());
    }

    @Test
    public void testOfCollection_ThreeElements() {
        List<Integer> numbers = Arrays.asList(10, 20, 15);
        Pair<Integer, Optional<Integer>> result = Median.of(numbers);
        Assertions.assertEquals(15, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfCollection_FourElements() {
        List<Integer> numbers = Arrays.asList(10, 5, 20, 15);
        Pair<Integer, Optional<Integer>> result = Median.of(numbers);
        Assertions.assertEquals(10, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(15, result.right().get());
    }

    @Test
    public void testOfCollection_FiveElements() {
        List<Integer> numbers = Arrays.asList(10, 5, 20, 15, 25);
        Pair<Integer, Optional<Integer>> result = Median.of(numbers);
        Assertions.assertEquals(15, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfCollectionWithComparator_TwoElements() {
        List<String> words = Arrays.asList("pie", "apple");
        Pair<String, Optional<String>> result = Median.of(words, Comparator.comparing(String::length));
        Assertions.assertEquals("pie", result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals("apple", result.right().get());
    }

    @Test
    public void testOfCollectionWithComparator_ThreeElements() {
        Set<String> words = new HashSet<>(Arrays.asList("apple", "pie", "banana"));
        Pair<String, Optional<String>> result = Median.of(words, Comparator.comparing(String::length));
        Assertions.assertEquals("apple", result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfCollectionWithComparator_FourElements() {
        List<String> words = Arrays.asList("cat", "dog", "apple", "banana");
        Pair<String, Optional<String>> result = Median.of(words, Comparator.comparing(String::length));
        Assertions.assertEquals("dog", result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals("apple", result.right().get());
    }

    @Test
    public void testOfCollectionRange_TwoElements() {
        List<Integer> numbers = Arrays.asList(100, 50, 75, 25, 90);
        Pair<Integer, Optional<Integer>> result = Median.of(numbers, 0, 2);
        Assertions.assertEquals(50, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(100, result.right().get());
    }

    @Test
    public void testOfCollectionRange_ThreeElements() {
        List<Integer> numbers = Arrays.asList(100, 50, 75, 25, 90);
        Pair<Integer, Optional<Integer>> result = Median.of(numbers, 1, 4);
        Assertions.assertEquals(50, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfCollectionRangeWithComparator_TwoElements() {
        List<String> words = Arrays.asList("elephant", "ant", "bee", "tiger", "cat");
        Pair<String, Optional<String>> result = Median.of(words, 1, 3, Comparator.comparing(String::length));
        Assertions.assertEquals("ant", result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals("bee", result.right().get());
    }

    @Test
    public void testOfCollectionRangeWithComparator_ThreeElements() {
        List<String> words = Arrays.asList("elephant", "ant", "bee", "tiger", "cat");
        Pair<String, Optional<String>> result = Median.of(words, 1, 4, Comparator.comparing(String::length));
        Assertions.assertEquals("bee", result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfCollectionRangeWithComparator_FourElements() {
        List<String> words = Arrays.asList("elephant", "a", "bb", "ccc", "dddd");
        Pair<String, Optional<String>> result = Median.of(words, 1, 5, Comparator.comparing(String::length));
        Assertions.assertEquals("bb", result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals("ccc", result.right().get());
    }

    @Test
    public void testOfCharArray() {
        Pair<Character, OptionalChar> result1 = Median.of('a');
        Assertions.assertEquals('a', result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Character, OptionalChar> result2 = Median.of('a', 'c');
        Assertions.assertEquals('a', result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals('c', result2.right().get());

        Pair<Character, OptionalChar> result3 = Median.of('a', 'c', 'b');
        Assertions.assertEquals('b', result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        Pair<Character, OptionalChar> result4 = Median.of('a', 'c', 'b', 'd');
        Assertions.assertEquals('b', result4.left());
        Assertions.assertTrue(result4.right().isPresent());
        Assertions.assertEquals('c', result4.right().get());

        Pair<Character, OptionalChar> result5 = Median.of('z', 'a', 'm', 'b', 'x');
        Assertions.assertEquals('m', result5.left());
        Assertions.assertFalse(result5.right().isPresent());
    }

    @Test
    public void testOfCharArrayWithRange() {
        char[] chars = { 'z', 'a', 'm', 'x', 'b' };

        Pair<Character, OptionalChar> result1 = Median.of(chars, 0, 1);
        Assertions.assertEquals('z', result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Character, OptionalChar> result2 = Median.of(chars, 1, 3);
        Assertions.assertEquals('a', result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals('m', result2.right().get());

        Pair<Character, OptionalChar> result3 = Median.of(chars, 0, 3);
        Assertions.assertEquals('m', result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        Pair<Character, OptionalChar> result4 = Median.of(chars, 0, chars.length);
        Assertions.assertEquals('m', result4.left());
        Assertions.assertFalse(result4.right().isPresent());
    }

    @Test
    public void testOfByteArray() {
        Pair<Byte, OptionalByte> result1 = Median.of((byte) 10);
        Assertions.assertEquals((byte) 10, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Byte, OptionalByte> result2 = Median.of((byte) 10, (byte) 20);
        Assertions.assertEquals((byte) 10, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals((byte) 20, result2.right().get());

        Pair<Byte, OptionalByte> result3 = Median.of((byte) 10, (byte) 5, (byte) 15);
        Assertions.assertEquals((byte) 10, result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        Pair<Byte, OptionalByte> result4 = Median.of((byte) 30, (byte) 10, (byte) 20, (byte) 40);
        Assertions.assertEquals((byte) 20, result4.left());
        Assertions.assertTrue(result4.right().isPresent());
        Assertions.assertEquals((byte) 30, result4.right().get());
    }

    @Test
    public void testOfByteArrayWithRange() {
        byte[] bytes = { 30, 10, 20, 40, 5 };

        Pair<Byte, OptionalByte> result1 = Median.of(bytes, 1, 3);
        Assertions.assertEquals((byte) 10, result1.left());
        Assertions.assertTrue(result1.right().isPresent());
        Assertions.assertEquals((byte) 20, result1.right().get());

        Pair<Byte, OptionalByte> result2 = Median.of(bytes, 1, 4);
        Assertions.assertEquals((byte) 20, result2.left());
        Assertions.assertFalse(result2.right().isPresent());
    }

    @Test
    public void testOfShortArray() {
        Pair<Short, OptionalShort> result1 = Median.of((short) 100);
        Assertions.assertEquals((short) 100, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Short, OptionalShort> result2 = Median.of((short) 100, (short) 200);
        Assertions.assertEquals((short) 100, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals((short) 200, result2.right().get());

        Pair<Short, OptionalShort> result3 = Median.of((short) 100, (short) 50, (short) 200);
        Assertions.assertEquals((short) 100, result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        Pair<Short, OptionalShort> result4 = Median.of((short) 300, (short) 100, (short) 200, (short) 400);
        Assertions.assertEquals((short) 200, result4.left());
        Assertions.assertTrue(result4.right().isPresent());
        Assertions.assertEquals((short) 300, result4.right().get());
    }

    @Test
    public void testOfShortArrayWithRange() {
        short[] shorts = { 300, 100, 200, 400, 50 };

        Pair<Short, OptionalShort> result1 = Median.of(shorts, 1, 3);
        Assertions.assertEquals((short) 100, result1.left());
        Assertions.assertTrue(result1.right().isPresent());
        Assertions.assertEquals((short) 200, result1.right().get());
    }

    @Test
    public void testOfIntArray() {
        Pair<Integer, OptionalInt> result1 = Median.of(1);
        Assertions.assertEquals(1, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Integer, OptionalInt> result2 = Median.of(1, 3);
        Assertions.assertEquals(1, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals(3, result2.right().get());

        Pair<Integer, OptionalInt> result3 = Median.of(1, 3, 5);
        Assertions.assertEquals(3, result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        Pair<Integer, OptionalInt> result4 = Median.of(1, 3, 5, 7);
        Assertions.assertEquals(3, result4.left());
        Assertions.assertTrue(result4.right().isPresent());
        Assertions.assertEquals(5, result4.right().get());

        Pair<Integer, OptionalInt> result5 = Median.of(1, 1, 3, 5);
        Assertions.assertEquals(1, result5.left());
        Assertions.assertTrue(result5.right().isPresent());
        Assertions.assertEquals(3, result5.right().get());
    }

    @Test
    public void testOfIntArrayWithRange() {
        int[] numbers = { 100, 50, 75, 25, 90 };

        Pair<Integer, OptionalInt> result1 = Median.of(numbers, 1, 4);
        Assertions.assertEquals(50, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Integer, OptionalInt> result2 = Median.of(numbers, 0, 4);
        Assertions.assertEquals(50, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals(75, result2.right().get());
    }

    @Test
    public void testOfLongArray() {
        Pair<Long, OptionalLong> result1 = Median.of(1000L);
        Assertions.assertEquals(1000L, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Long, OptionalLong> result2 = Median.of(500L, 1000L);
        Assertions.assertEquals(500L, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals(1000L, result2.right().get());

        Pair<Long, OptionalLong> result3 = Median.of(1000L, 500L, 1500L, 750L);
        Assertions.assertEquals(750L, result3.left());
        Assertions.assertTrue(result3.right().isPresent());
        Assertions.assertEquals(1000L, result3.right().get());
    }

    @Test
    public void testOfLongArrayWithRange() {
        long[] values = { 3000L, 1000L, 2000L, 4000L, 1500L };

        Pair<Long, OptionalLong> result1 = Median.of(values, 1, 4);
        Assertions.assertEquals(2000L, result1.left());
        Assertions.assertFalse(result1.right().isPresent());
    }

    @Test
    public void testOfFloatArray() {
        Pair<Float, OptionalFloat> result1 = Median.of(10.5f);
        Assertions.assertEquals(10.5f, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Float, OptionalFloat> result2 = Median.of(10.5f, 20.5f);
        Assertions.assertEquals(10.5f, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals(20.5f, result2.right().get());

        Pair<Float, OptionalFloat> result3 = Median.of(10.5f, 5.2f, 20.8f);
        Assertions.assertEquals(10.5f, result3.left());
        Assertions.assertFalse(result3.right().isPresent());
    }

    @Test
    public void testOfFloatArrayWithRange() {
        float[] values = { 30.5f, 10.2f, 20.8f, 40.1f };

        Pair<Float, OptionalFloat> result1 = Median.of(values, 1, 3);
        Assertions.assertEquals(10.2f, result1.left());
        Assertions.assertTrue(result1.right().isPresent());
        Assertions.assertEquals(20.8f, result1.right().get());
    }

    @Test
    public void testOfDoubleArray() {
        Pair<Double, OptionalDouble> result1 = Median.of(10.5);
        Assertions.assertEquals(10.5, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Double, OptionalDouble> result2 = Median.of(10.5, 20.5);
        Assertions.assertEquals(10.5, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals(20.5, result2.right().get());

        Pair<Double, OptionalDouble> result3 = Median.of(10.5, 5.2, 20.8, 15.1);
        Assertions.assertEquals(10.5, result3.left());
        Assertions.assertTrue(result3.right().isPresent());
        Assertions.assertEquals(15.1, result3.right().get());
    }

    @Test
    public void testOfDoubleArrayWithRange() {
        double[] values = { 30.5, 10.2, 20.8, 40.1, 15.3 };

        Pair<Double, OptionalDouble> result1 = Median.of(values, 1, 4);
        Assertions.assertEquals(20.8, result1.left());
        Assertions.assertFalse(result1.right().isPresent());
    }

    @Test
    public void testOfComparableArray() {
        Pair<String, Optional<String>> result1 = Median.of(new String[] { "apple" });
        Assertions.assertEquals("apple", result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<String, Optional<String>> result2 = Median.of(new String[] { "apple", "banana", "cherry" });
        Assertions.assertEquals("banana", result2.left());
        Assertions.assertFalse(result2.right().isPresent());

        Pair<String, Optional<String>> result3 = Median.of(new String[] { "apple", "banana", "cherry", "date" });
        Assertions.assertEquals("banana", result3.left());
        Assertions.assertTrue(result3.right().isPresent());
        Assertions.assertEquals("cherry", result3.right().get());
    }

    @Test
    public void testOfArrayWithComparator() {
        String[] words = { "apple", "pie", "banana" };

        Pair<String, Optional<String>> result1 = Median.of(words, Comparator.comparing(String::length));
        Assertions.assertEquals("apple", result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        String[] words2 = { "a", "bb", "ccc", "dddd" };
        Pair<String, Optional<String>> result2 = Median.of(words2, Comparator.comparing(String::length));
        Assertions.assertEquals("bb", result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals("ccc", result2.right().get());
    }

    @Test
    public void testOfComparableCollection() {
        List<Integer> numbers1 = Arrays.asList(10, 5, 20, 15, 25);
        Pair<Integer, Optional<Integer>> result1 = Median.of(numbers1);
        Assertions.assertEquals(15, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        List<Integer> numbers2 = Arrays.asList(10, 5, 20, 15);
        Pair<Integer, Optional<Integer>> result2 = Median.of(numbers2);
        Assertions.assertEquals(10, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals(15, result2.right().get());

        Set<String> words = new HashSet<>(Arrays.asList("apple", "banana", "cherry"));
        Pair<String, Optional<String>> result3 = Median.of(words);
        Assertions.assertEquals("banana", result3.left());
        Assertions.assertFalse(result3.right().isPresent());
    }

    @Test
    public void testWithLargeArray() {
        int[] large = new int[100];
        for (int i = 0; i < 100; i++) {
            large[i] = i;
        }

        Pair<Integer, OptionalInt> result = Median.of(large);
        Assertions.assertEquals(49, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(50, result.right().get());
    }

    @Test
    public void testWithDuplicateValues() {
        Pair<Integer, OptionalInt> result1 = Median.of(5, 5, 5, 5);
        Assertions.assertEquals(5, result1.left());
        Assertions.assertTrue(result1.right().isPresent());
        Assertions.assertEquals(5, result1.right().get());

        Pair<Integer, OptionalInt> result2 = Median.of(1, 2, 2, 2, 3);
        Assertions.assertEquals(2, result2.left());
        Assertions.assertFalse(result2.right().isPresent());
    }

    @Test
    public void testCollectionWithTwoElements() {
        List<String> two = Arrays.asList("first", "second");
        Pair<String, Optional<String>> result = Median.of(two);
        Assertions.assertEquals("first", result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals("second", result.right().get());
    }

    @Test
    public void testCollectionWithThreeElements() {
        List<Integer> three = Arrays.asList(30, 10, 20);
        Pair<Integer, Optional<Integer>> result = Median.of(three);
        Assertions.assertEquals(20, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfGenericArray() {
        Pair<String, Optional<String>> result1 = Median.of(new String[] { "apple" });
        Assertions.assertEquals("apple", result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<String, Optional<String>> result2 = Median.of(new String[] { "apple", "banana" });
        Assertions.assertEquals("apple", result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals("banana", result2.right().get());

        Pair<String, Optional<String>> result3 = Median.of(new String[] { "banana", "apple", "cherry" });
        Assertions.assertEquals("banana", result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        Pair<Integer, Optional<Integer>> result4 = Median.of(new Integer[] { 3, 1, 4, 1, 5 });
        Assertions.assertEquals(3, result4.left());
        Assertions.assertFalse(result4.right().isPresent());
    }

    @Test
    public void testOfGenericArrayWithRange() {
        String[] array = { "zebra", "apple", "cherry", "banana", "date", "fig" };

        Pair<String, Optional<String>> result = Median.of(array, 1, 5);
        Assertions.assertEquals("banana", result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals("cherry", result.right().get());
    }

    @Test
    public void testOfGenericArrayWithRangeAndComparator() {
        Integer[] array = { 9, 1, 5, 3, 7, 2, 8 };
        Comparator<Integer> reverseComparator = Comparator.reverseOrder();

        Pair<Integer, Optional<Integer>> result = Median.of(array, 1, 5, reverseComparator);
        Assertions.assertEquals(5, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(3, result.right().get());
    }

    @Test
    public void testOfCollection() {
        List<Integer> list1 = Arrays.asList(5);
        Pair<Integer, Optional<Integer>> result1 = Median.of(list1);
        Assertions.assertEquals(5, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        List<Integer> list2 = Arrays.asList(5, 10);
        Pair<Integer, Optional<Integer>> result2 = Median.of(list2);
        Assertions.assertEquals(5, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals(10, result2.right().get());

        List<Integer> list3 = Arrays.asList(10, 5, 15);
        Pair<Integer, Optional<Integer>> result3 = Median.of(list3);
        Assertions.assertEquals(10, result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        List<Integer> list4 = Arrays.asList(4, 2, 6, 8);
        Pair<Integer, Optional<Integer>> result4 = Median.of(list4);
        Assertions.assertEquals(4, result4.left());
        Assertions.assertTrue(result4.right().isPresent());
        Assertions.assertEquals(6, result4.right().get());

        List<String> stringList = Arrays.asList("dog", "cat", "bird", "ant", "elephant");
        Pair<String, Optional<String>> result5 = Median.of(stringList);
        Assertions.assertEquals("cat", result5.left());
        Assertions.assertFalse(result5.right().isPresent());
    }

    @Test
    public void testOfCollectionWithRange() {
        List<Integer> list = Arrays.asList(9, 1, 5, 3, 7, 2, 8);

        Pair<Integer, Optional<Integer>> result = Median.of(list, 1, 5);
        Assertions.assertEquals(3, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(5, result.right().get());

        Pair<Integer, Optional<Integer>> result2 = Median.of(list, 2, 3);
        Assertions.assertEquals(5, result2.left());
        Assertions.assertFalse(result2.right().isPresent());
    }

    @Test
    public void testOfCollectionWithRangeAndComparator() {
        List<String> list = Arrays.asList("zebra", "apple", "cherry", "banana", "date", "fig");
        Comparator<String> reverseComparator = Comparator.reverseOrder();

        Pair<String, Optional<String>> result = Median.of(list, 1, 5, reverseComparator);
        Assertions.assertEquals("cherry", result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals("banana", result.right().get());
    }

    @Test
    public void testDuplicateValues() {
        Pair<Integer, OptionalInt> result1 = Median.of(5, 5, 5, 5);
        Assertions.assertEquals(5, result1.left());
        Assertions.assertTrue(result1.right().isPresent());
        Assertions.assertEquals(5, result1.right().getAsInt());

        Pair<Integer, OptionalInt> result2 = Median.of(1, 1, 1, 2);
        Assertions.assertEquals(1, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals(1, result2.right().getAsInt());
    }

    @Test
    public void testLargeArrayPerformance() {
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
    public void testOfByte_FiveElements() {
        Pair<Byte, OptionalByte> result = Median.of((byte) 50, (byte) 10, (byte) 30, (byte) 20, (byte) 40);
        Assertions.assertEquals((byte) 30, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfShort_FiveElements() {
        Pair<Short, OptionalShort> result = Median.of((short) 500, (short) 100, (short) 300, (short) 200, (short) 400);
        Assertions.assertEquals((short) 300, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfLong_FiveElements() {
        Pair<Long, OptionalLong> result = Median.of(50L, 10L, 30L, 20L, 40L);
        Assertions.assertEquals(30L, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfFloat_FiveElements() {
        Pair<Float, OptionalFloat> result = Median.of(5.0f, 1.0f, 3.0f, 2.0f, 4.0f);
        Assertions.assertEquals(3.0f, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfDouble_FiveElements() {
        Pair<Double, OptionalDouble> result = Median.of(5.0, 1.0, 3.0, 2.0, 4.0);
        Assertions.assertEquals(3.0, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfChar_SingleElement() {
        Pair<Character, OptionalChar> result = Median.of('a');
        Assertions.assertEquals('a', result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfCharRange_SingleElement() {
        char[] chars = { 'a', 'b', 'c' };
        Pair<Character, OptionalChar> result = Median.of(chars, 0, 1);
        Assertions.assertEquals('a', result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfByte_SingleElement() {
        Pair<Byte, OptionalByte> result = Median.of((byte) 10);
        Assertions.assertEquals((byte) 10, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfByteRange_SingleElement() {
        byte[] bytes = { 30, 10, 20, 40 };
        Pair<Byte, OptionalByte> result = Median.of(bytes, 0, 1);
        Assertions.assertEquals((byte) 30, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfShort_SingleElement() {
        Pair<Short, OptionalShort> result = Median.of((short) 100);
        Assertions.assertEquals((short) 100, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfShortRange_SingleElement() {
        short[] values = { 300, 100, 200, 400 };
        Pair<Short, OptionalShort> result = Median.of(values, 0, 1);
        Assertions.assertEquals((short) 300, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfInt_SingleElement() {
        Pair<Integer, OptionalInt> result = Median.of(10);
        Assertions.assertEquals(10, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfIntRange_SingleElement() {
        int[] numbers = { 100, 50, 75, 25, 90 };
        Pair<Integer, OptionalInt> result = Median.of(numbers, 0, 1);
        Assertions.assertEquals(100, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfLong_SingleElement() {
        Pair<Long, OptionalLong> result = Median.of(1000L);
        Assertions.assertEquals(1000L, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfLongRange_SingleElement() {
        long[] values = { 3000L, 1000L, 2000L, 4000L, 1500L };
        Pair<Long, OptionalLong> result = Median.of(values, 0, 1);
        Assertions.assertEquals(3000L, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfFloat_SingleElement() {
        Pair<Float, OptionalFloat> result = Median.of(10.5f);
        Assertions.assertEquals(10.5f, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfFloatRange_SingleElement() {
        float[] values = { 30.5f, 10.2f, 20.8f, 40.1f };
        Pair<Float, OptionalFloat> result = Median.of(values, 0, 1);
        Assertions.assertEquals(30.5f, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfDouble_SingleElement() {
        Pair<Double, OptionalDouble> result = Median.of(10.5);
        Assertions.assertEquals(10.5, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfDoubleRange_SingleElement() {
        double[] values = { 30.5, 10.2, 20.8, 40.1, 15.3 };
        Pair<Double, OptionalDouble> result = Median.of(values, 0, 1);
        Assertions.assertEquals(30.5, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfComparableArray_SingleElement() {
        String[] words = { "apple" };
        Pair<String, Optional<String>> result = Median.of(words);
        Assertions.assertEquals("apple", result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfComparableArrayRange_SingleElement() {
        String[] words = { "zebra", "apple", "banana", "cherry" };
        Pair<String, Optional<String>> result = Median.of(words, 1, 2);
        Assertions.assertEquals("apple", result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfArrayWithComparator_SingleElement() {
        String[] words = { "apple" };
        Pair<String, Optional<String>> result = Median.of(words, Comparator.comparing(String::length));
        Assertions.assertEquals("apple", result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfArrayWithComparator_NullComparator() {
        String[] words = { "apple", "banana", "cherry" };
        Pair<String, Optional<String>> result = Median.of(words, null);
        Assertions.assertEquals("banana", result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfArrayRangeWithComparator_SingleElement() {
        String[] words = { "elephant", "ant", "bee", "tiger" };
        Pair<String, Optional<String>> result = Median.of(words, 1, 2, Comparator.comparing(String::length));
        Assertions.assertEquals("ant", result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfArrayRangeWithComparator_NullComparator() {
        String[] words = { "elephant", "ant", "bee", "tiger" };
        Pair<String, Optional<String>> result = Median.of(words, 1, 4, null);
        Assertions.assertEquals("bee", result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfCollection_SingleElement() {
        List<Integer> numbers = Arrays.asList(10);
        Pair<Integer, Optional<Integer>> result = Median.of(numbers);
        Assertions.assertEquals(10, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfCollectionWithComparator_SingleElement() {
        Set<String> words = new HashSet<>(Arrays.asList("apple"));
        Pair<String, Optional<String>> result = Median.of(words, Comparator.comparing(String::length));
        Assertions.assertEquals("apple", result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfCollectionWithComparator_NullComparator() {
        List<String> words = Arrays.asList("apple", "banana", "cherry");
        Pair<String, Optional<String>> result = Median.of(words, null);
        Assertions.assertEquals("banana", result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfCollectionRange_SingleElement() {
        List<Integer> numbers = Arrays.asList(100, 50, 75, 25, 90);
        Pair<Integer, Optional<Integer>> result = Median.of(numbers, 0, 1);
        Assertions.assertEquals(100, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfCollectionRangeWithComparator_SingleElement() {
        List<String> words = Arrays.asList("elephant", "ant", "bee", "tiger", "cat");
        Pair<String, Optional<String>> result = Median.of(words, 1, 2, Comparator.comparing(String::length));
        Assertions.assertEquals("ant", result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfCollectionRangeWithComparator_NullComparator() {
        List<String> words = Arrays.asList("elephant", "ant", "bee", "tiger", "cat");
        Pair<String, Optional<String>> result = Median.of(words, 1, 4, null);
        Assertions.assertEquals("bee", result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfArrayWithNullComparator() {
        Integer[] numbers = { 3, 1, 2 };

        Pair<Integer, Optional<Integer>> result = Median.of(numbers, null);
        Assertions.assertEquals(2, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testWithNegativeValues() {
        Pair<Integer, OptionalInt> result = Median.of(-5, -1, -3, -2, -4);
        Assertions.assertEquals(-3, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testWithMixedPositiveNegativeValues() {
        Pair<Integer, OptionalInt> result = Median.of(-2, -1, 0, 1, 2);
        Assertions.assertEquals(0, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testCollectionWithSingleElement() {
        List<Integer> single = Collections.singletonList(42);
        Pair<Integer, Optional<Integer>> result = Median.of(single);
        Assertions.assertEquals(42, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfGenericArrayWithComparator() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        Comparator<Integer> reverseComparator = Comparator.reverseOrder();

        Pair<Integer, Optional<Integer>> result1 = Median.of(array, reverseComparator);
        Assertions.assertEquals(3, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        String[] strings = { "APPLE", "banana", "Cherry", "date" };
        Comparator<String> caseInsensitive = String.CASE_INSENSITIVE_ORDER;

        Pair<String, Optional<String>> result2 = Median.of(strings, caseInsensitive);
        Assertions.assertEquals("banana", result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals("Cherry", result2.right().get());
    }

    @Test
    public void testOfCollectionWithComparator() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);
        Comparator<Integer> reverseComparator = Comparator.reverseOrder();

        Pair<Integer, Optional<Integer>> result = Median.of(list, reverseComparator);
        Assertions.assertEquals(4, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(3, result.right().get());

        List<String> strings = Arrays.asList("AAA", "bbb", "CCC", "ddd");
        Comparator<String> caseInsensitive = String.CASE_INSENSITIVE_ORDER;

        Pair<String, Optional<String>> result2 = Median.of(strings, caseInsensitive);
        Assertions.assertEquals("bbb", result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals("CCC", result2.right().get());
    }

    @Test
    public void testNullComparator() {
        Integer[] array = { 3, 1, 4, 1, 5 };
        Pair<Integer, Optional<Integer>> result = Median.of(array, null);
        Assertions.assertEquals(3, result.left());
        Assertions.assertFalse(result.right().isPresent());

        List<Integer> list = Arrays.asList(3, 1, 4, 1, 5);
        Pair<Integer, Optional<Integer>> result2 = Median.of(list, null);
        Assertions.assertEquals(3, result2.left());
        Assertions.assertFalse(result2.right().isPresent());
    }

    @Test
    public void testOfChar_NullArray() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((char[]) null);
        });
    }

    @Test
    public void testOfChar_EmptyArray() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new char[0]);
        });
    }

    @Test
    public void testOfCharRange_InvalidRange() {
        char[] chars = { 'a', 'b', 'c' };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(chars, 2, 2);
        });
    }

    @Test
    public void testOfByte_NullArray() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((byte[]) null);
        });
    }

    @Test
    public void testOfByte_EmptyArray() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new byte[0]);
        });
    }

    @Test
    public void testOfShort_NullArray() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((short[]) null);
        });
    }

    @Test
    public void testOfShort_EmptyArray() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new short[0]);
        });
    }

    @Test
    public void testOfInt_NullArray() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((int[]) null);
        });
    }

    @Test
    public void testOfInt_EmptyArray() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new int[0]);
        });
    }

    @Test
    public void testOfIntRange_InvalidRange() {
        int[] numbers = { 100, 50, 75 };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            Median.of(numbers, 0, 5);
        });
    }

    @Test
    public void testOfLong_NullArray() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((long[]) null);
        });
    }

    @Test
    public void testOfLong_EmptyArray() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new long[0]);
        });
    }

    @Test
    public void testOfFloat_NullArray() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((float[]) null);
        });
    }

    @Test
    public void testOfFloat_EmptyArray() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new float[0]);
        });
    }

    @Test
    public void testOfDouble_NullArray() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((double[]) null);
        });
    }

    @Test
    public void testOfDouble_EmptyArray() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new double[0]);
        });
    }

    @Test
    public void testOfComparableArray_NullArray() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((String[]) null);
        });
    }

    @Test
    public void testOfComparableArray_EmptyArray() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new String[0]);
        });
    }

    @Test
    public void testOfCollection_NullCollection() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((Collection<Integer>) null);
        });
    }

    @Test
    public void testOfCollection_EmptyCollection() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new ArrayList<Integer>());
        });
    }

    @Test
    public void testOfCollectionWithComparator_NullCollection() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((Collection<String>) null, Comparator.comparing(String::length));
        });
    }

    @Test
    public void testOfCollectionWithComparator_EmptyCollection() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new ArrayList<String>(), Comparator.comparing(String::length));
        });
    }

    @Test
    public void testOfCollectionRange_InvalidRange() {
        List<Integer> numbers = Arrays.asList(100, 50, 75);
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            Median.of(numbers, 0, 5);
        });
    }

    @Test
    public void testOfCollectionRangeWithComparator_NullCollection() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((Collection<String>) null, 0, 3, Comparator.comparing(String::length));
        });
    }

    @Test
    public void testOfCollectionRangeWithComparator_EmptyRange() {
        List<String> words = Arrays.asList("ant", "bee", "cat");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(words, 2, 2, Comparator.comparing(String::length));
        });
    }

    @Test
    public void testOfCharArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new char[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((char[]) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new char[] { 'a' }, 0, 0));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Median.of(new char[] { 'a' }, -1, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Median.of(new char[] { 'a' }, 0, 2));
    }

    @Test
    public void testOfByteArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new byte[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((byte[]) null));
    }

    @Test
    public void testOfShortArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new short[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((short[]) null));
    }

    @Test
    public void testOfIntArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new int[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((int[]) null));
    }

    @Test
    public void testOfLongArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new long[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((long[]) null));
    }

    @Test
    public void testOfFloatArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new float[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((float[]) null));
    }

    @Test
    public void testOfDoubleArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new double[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((double[]) null));
    }

    @Test
    public void testOfComparableArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new String[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((String[]) null));
    }

    @Test
    public void testOfCollectionExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new ArrayList<Integer>()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((Collection<Integer>) null));

        List<Integer> numbers = Arrays.asList(1, 2, 3);
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(numbers, 0, 0));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Median.of(numbers, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Median.of(numbers, 0, 4));
    }

    @Test
    public void testOfGenericArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((String[]) null));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new String[0]));

        String[] array = { "a", "b", "c" };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Median.of(array, -1, 2));
    }

    @Test
    public void testOfByteRange_InvalidRange() {
        byte[] bytes = { 10, 20, 30 };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(bytes, 2, 2);
        });
    }

    @Test
    public void testOfShortRange_InvalidRange() {
        short[] shorts = { 100, 200, 300 };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(shorts, 2, 2);
        });
    }

    @Test
    public void testOfLongRange_InvalidRange() {
        long[] longs = { 100L, 200L, 300L };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(longs, 2, 2);
        });
    }

    @Test
    public void testOfFloatRange_InvalidRange() {
        float[] floats = { 1.0f, 2.0f, 3.0f };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(floats, 2, 2);
        });
    }

    @Test
    public void testOfDoubleRange_InvalidRange() {
        double[] doubles = { 1.0, 2.0, 3.0 };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(doubles, 2, 2);
        });
    }

    @Test
    public void testOfComparableArrayRange_InvalidRange() {
        String[] strings = { "a", "b", "c" };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(strings, 2, 2);
        });
    }

    @Test
    public void testOfArrayRangeWithComparator_InvalidRange() {
        String[] strings = { "a", "b", "c" };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(strings, 2, 2, Comparator.naturalOrder());
        });
    }

    @Test
    public void testOfCollectionRange_NullCollection() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((Collection<String>) null, 0, 1);
        });
    }

}
