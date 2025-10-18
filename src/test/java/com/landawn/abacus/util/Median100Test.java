package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
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

@Tag("new-test")
public class Median100Test extends TestBase {

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
    public void testMedianOfByteArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((byte[]) null);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new byte[0]);
        });
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
    public void testMedianOfShortArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((short[]) null);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new short[0]);
        });
    }

    @Test
    public void testMedianOfIntArray() {
        Pair<Integer, OptionalInt> result1 = Median.of(1);
        assertEquals(1, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Integer, OptionalInt> result2 = Median.of(1, 3);
        assertEquals(1, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        assertEquals(3, result2.right().getAsInt());

        Pair<Integer, OptionalInt> result3 = Median.of(1, 3, 5);
        assertEquals(3, result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        Pair<Integer, OptionalInt> result4 = Median.of(1, 1);
        assertEquals(1, result4.left());
        Assertions.assertTrue(result4.right().isPresent());
        assertEquals(1, result4.right().getAsInt());

        Pair<Integer, OptionalInt> result5 = Median.of(1, 1, 3);
        assertEquals(1, result5.left());
        Assertions.assertFalse(result5.right().isPresent());

        Pair<Integer, OptionalInt> result6 = Median.of(1, 1, 3, 5);
        assertEquals(1, result6.left());
        Assertions.assertTrue(result6.right().isPresent());
        assertEquals(3, result6.right().getAsInt());

        Pair<Integer, OptionalInt> result7 = Median.of(1, 1, 1, 3, 5);
        assertEquals(1, result7.left());
        Assertions.assertFalse(result7.right().isPresent());

        Pair<Integer, OptionalInt> result8 = Median.of(1, 1, 1, 3, 3, 5);
        assertEquals(1, result8.left());
        Assertions.assertTrue(result8.right().isPresent());
        assertEquals(3, result8.right().getAsInt());

        Pair<Integer, OptionalInt> result9 = Median.of(1, 1, 1, 3, 3, 3, 5);
        assertEquals(3, result9.left());
        Assertions.assertFalse(result9.right().isPresent());
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
    public void testMedianOfLongArrayWithRange() {
        long[] arr = { 10L, 20L, 30L, 40L, 50L };

        Pair<Long, OptionalLong> result = Median.of(arr, 1, 4);
        assertEquals(30L, result.left());
        Assertions.assertFalse(result.right().isPresent());
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
    public void testMedianOfFloatArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of((float[]) null);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(new float[0]);
        });
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
    public void testMedianOfDoubleArrayWithRange() {
        double[] arr = { 1.1, 2.2, 3.3, 4.4, 5.5 };

        Pair<Double, OptionalDouble> result = Median.of(arr, 1, 5);
        assertEquals(3.3, result.left());
        Assertions.assertTrue(result.right().isPresent());
        assertEquals(4.4, result.right().getAsDouble());
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
    public void testMedianWithNullComparator() {
        Integer[] arr = { 3, 1, 2 };
        Pair<Integer, Optional<Integer>> result = Median.of(arr, null);
        assertEquals(2, result.left());
        Assertions.assertFalse(result.right().isPresent());
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
}
