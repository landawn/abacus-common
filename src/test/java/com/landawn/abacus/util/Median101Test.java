package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

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
public class Median101Test extends TestBase {

    @Test
    public void testOfCharArray() {
        Pair<Character, OptionalChar> result1 = Median.of('a');
        Assertions.assertEquals('a', result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Character, OptionalChar> result2 = Median.of('a', 'b');
        Assertions.assertEquals('a', result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals('b', result2.right().get());

        Pair<Character, OptionalChar> result3 = Median.of('a', 'c', 'b');
        Assertions.assertEquals('b', result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        Pair<Character, OptionalChar> result4 = Median.of('a', 'b', 'c', 'd');
        Assertions.assertEquals('b', result4.left());
        Assertions.assertTrue(result4.right().isPresent());
        Assertions.assertEquals('c', result4.right().get());

        Pair<Character, OptionalChar> result5 = Median.of('e', 'b', 'a', 'd', 'c');
        Assertions.assertEquals('c', result5.left());
        Assertions.assertFalse(result5.right().isPresent());
    }

    @Test
    public void testOfCharArrayWithRange() {
        char[] array = { 'z', 'a', 'c', 'b', 'd', 'y' };

        Pair<Character, OptionalChar> result1 = Median.of(array, 1, 5);
        Assertions.assertEquals('b', result1.left());
        Assertions.assertTrue(result1.right().isPresent());
        Assertions.assertEquals('c', result1.right().get());

        Pair<Character, OptionalChar> result2 = Median.of(array, 2, 3);
        Assertions.assertEquals('c', result2.left());
        Assertions.assertFalse(result2.right().isPresent());

        Pair<Character, OptionalChar> result3 = Median.of(array, 0, 2);
        Assertions.assertEquals('a', result3.left());
        Assertions.assertTrue(result3.right().isPresent());
        Assertions.assertEquals('z', result3.right().get());
    }

    @Test
    public void testOfCharArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((char[]) null));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new char[0]));

        char[] array = { 'a', 'b', 'c' };
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(array, 1, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Median.of(array, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Median.of(array, 1, 4));
    }

    @Test
    public void testOfByteArray() {
        Pair<Byte, OptionalByte> result1 = Median.of((byte) 1);
        Assertions.assertEquals((byte) 1, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Byte, OptionalByte> result2 = Median.of((byte) 1, (byte) 2);
        Assertions.assertEquals((byte) 1, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals((byte) 2, result2.right().get());

        Pair<Byte, OptionalByte> result3 = Median.of((byte) 3, (byte) 1, (byte) 2);
        Assertions.assertEquals((byte) 2, result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        Pair<Byte, OptionalByte> result4 = Median.of((byte) 1, (byte) 3, (byte) 2, (byte) 4);
        Assertions.assertEquals((byte) 2, result4.left());
        Assertions.assertTrue(result4.right().isPresent());
        Assertions.assertEquals((byte) 3, result4.right().get());

        Pair<Byte, OptionalByte> result5 = Median.of((byte) -2, (byte) -1, (byte) 0, (byte) 1, (byte) 2);
        Assertions.assertEquals((byte) 0, result5.left());
        Assertions.assertFalse(result5.right().isPresent());
    }

    @Test
    public void testOfByteArrayWithRange() {
        byte[] array = { 5, 1, 3, 2, 4, 6 };

        Pair<Byte, OptionalByte> result1 = Median.of(array, 1, 5);
        Assertions.assertEquals((byte) 2, result1.left());
        Assertions.assertTrue(result1.right().isPresent());
        Assertions.assertEquals((byte) 3, result1.right().get());

        Pair<Byte, OptionalByte> result2 = Median.of(array, 0, 6);
        Assertions.assertEquals((byte) 3, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals((byte) 4, result2.right().get());
    }

    @Test
    public void testOfByteArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((byte[]) null));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new byte[0]));

        byte[] array = { 1, 2, 3 };
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(array, 2, 2));
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

        Pair<Short, OptionalShort> result3 = Median.of((short) 300, (short) 100, (short) 200);
        Assertions.assertEquals((short) 200, result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        Pair<Short, OptionalShort> result4 = Median.of((short) 10, (short) 30, (short) 20, (short) 40);
        Assertions.assertEquals((short) 20, result4.left());
        Assertions.assertTrue(result4.right().isPresent());
        Assertions.assertEquals((short) 30, result4.right().get());
    }

    @Test
    public void testOfShortArrayWithRange() {
        short[] array = { 50, 10, 30, 20, 40, 60 };

        Pair<Short, OptionalShort> result = Median.of(array, 1, 5);
        Assertions.assertEquals((short) 20, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals((short) 30, result.right().get());
    }

    @Test
    public void testOfShortArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((short[]) null));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new short[0]));
    }

    @Test
    public void testOfIntArray() {
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
        int[] array = { 9, 1, 5, 3, 7, 2, 8 };

        Pair<Integer, OptionalInt> result1 = Median.of(array, 1, 5);
        Assertions.assertEquals(3, result1.left());
        Assertions.assertTrue(result1.right().isPresent());
        Assertions.assertEquals(5, result1.right().getAsInt());

        Pair<Integer, OptionalInt> result2 = Median.of(array, 2, 5);
        Assertions.assertEquals(5, result2.left());
        Assertions.assertFalse(result2.right().isPresent());
    }

    @Test
    public void testOfIntArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((int[]) null));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new int[0]));

        int[] array = { 1, 2, 3 };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Median.of(array, 0, 4));
    }

    @Test
    public void testOfLongArray() {
        Pair<Long, OptionalLong> result1 = Median.of(1000L);
        Assertions.assertEquals(1000L, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Long, OptionalLong> result2 = Median.of(1000L, 2000L);
        Assertions.assertEquals(1000L, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals(2000L, result2.right().getAsLong());

        Pair<Long, OptionalLong> result3 = Median.of(3000L, 1000L, 2000L);
        Assertions.assertEquals(2000L, result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        Pair<Long, OptionalLong> result4 = Median.of(Long.MAX_VALUE, Long.MIN_VALUE, 0L, 1000L);
        Assertions.assertEquals(0L, result4.left());
        Assertions.assertTrue(result4.right().isPresent());
        Assertions.assertEquals(1000L, result4.right().getAsLong());
    }

    @Test
    public void testOfLongArrayWithRange() {
        long[] array = { 5000L, 1000L, 3000L, 2000L, 4000L, 6000L };

        Pair<Long, OptionalLong> result = Median.of(array, 1, 5);
        Assertions.assertEquals(2000L, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(3000L, result.right().getAsLong());
    }

    @Test
    public void testOfLongArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((long[]) null));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new long[0]));
    }

    @Test
    public void testOfFloatArray() {
        Pair<Float, OptionalFloat> result1 = Median.of(1.5f);
        Assertions.assertEquals(1.5f, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Float, OptionalFloat> result2 = Median.of(1.5f, 2.5f);
        Assertions.assertEquals(1.5f, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals(2.5f, result2.right().get());

        Pair<Float, OptionalFloat> result3 = Median.of(3.5f, 1.5f, 2.5f);
        Assertions.assertEquals(2.5f, result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        Pair<Float, OptionalFloat> result4 = Median.of(Float.NaN, 1.0f, Float.POSITIVE_INFINITY);
        Assertions.assertEquals(Float.POSITIVE_INFINITY, result4.left());
        Assertions.assertFalse(result4.right().isPresent());
    }

    @Test
    public void testOfFloatArrayWithRange() {
        float[] array = { 5.5f, 1.5f, 3.5f, 2.5f, 4.5f, 6.5f };

        Pair<Float, OptionalFloat> result = Median.of(array, 1, 5);
        Assertions.assertEquals(2.5f, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(3.5f, result.right().get());
    }

    @Test
    public void testOfFloatArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((float[]) null));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new float[0]));
    }

    @Test
    public void testOfDoubleArray() {
        Pair<Double, OptionalDouble> result1 = Median.of(1.0);
        Assertions.assertEquals(1.0, result1.left());
        Assertions.assertFalse(result1.right().isPresent());

        Pair<Double, OptionalDouble> result2 = Median.of(1.0, 2.0);
        Assertions.assertEquals(1.0, result2.left());
        Assertions.assertTrue(result2.right().isPresent());
        Assertions.assertEquals(2.0, result2.right().getAsDouble());

        Pair<Double, OptionalDouble> result3 = Median.of(3.0, 1.0, 2.0);
        Assertions.assertEquals(2.0, result3.left());
        Assertions.assertFalse(result3.right().isPresent());

        Pair<Double, OptionalDouble> result4 = Median.of(1.1, 3.3, 2.2, 4.4);
        Assertions.assertEquals(2.2, result4.left());
        Assertions.assertTrue(result4.right().isPresent());
        Assertions.assertEquals(3.3, result4.right().getAsDouble());

        Pair<Double, OptionalDouble> result5 = Median.of(Double.NEGATIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY);
        Assertions.assertEquals(0.0, result5.left());
        Assertions.assertFalse(result5.right().isPresent());
    }

    @Test
    public void testOfDoubleArrayWithRange() {
        double[] array = { 5.0, 1.0, 3.0, 2.0, 4.0, 6.0 };

        Pair<Double, OptionalDouble> result = Median.of(array, 1, 5);
        Assertions.assertEquals(2.0, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(3.0, result.right().getAsDouble());
    }

    @Test
    public void testOfDoubleArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((double[]) null));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new double[0]));
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
    public void testOfGenericArrayWithRangeAndComparator() {
        Integer[] array = { 9, 1, 5, 3, 7, 2, 8 };
        Comparator<Integer> reverseComparator = Comparator.reverseOrder();

        Pair<Integer, Optional<Integer>> result = Median.of(array, 1, 5, reverseComparator);
        Assertions.assertEquals(5, result.left());
        Assertions.assertTrue(result.right().isPresent());
        Assertions.assertEquals(3, result.right().get());
    }

    @Test
    public void testOfGenericArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((String[]) null));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new String[0]));

        String[] array = { "a", "b", "c" };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Median.of(array, -1, 2));
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
    public void testOfCollectionExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((List<Integer>) null));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new ArrayList<Integer>()));

        List<Integer> list = Arrays.asList(1, 2, 3);
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Median.of(list, 0, 4));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(list, 1, 1));
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
}
