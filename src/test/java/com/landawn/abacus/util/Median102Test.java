package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
public class Median102Test extends TestBase {

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
    public void testOfCharArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new char[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((char[]) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new char[] { 'a' }, 0, 0));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Median.of(new char[] { 'a' }, -1, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Median.of(new char[] { 'a' }, 0, 2));
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
    public void testOfByteArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new byte[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((byte[]) null));
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
    public void testOfShortArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new short[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((short[]) null));
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
    public void testOfIntArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new int[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((int[]) null));
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
    public void testOfLongArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new long[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((long[]) null));
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
    public void testOfFloatArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new float[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((float[]) null));
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
    public void testOfDoubleArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new double[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((double[]) null));
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
    public void testOfComparableArrayWithRange() {
        String[] words = { "zebra", "apple", "banana", "cherry" };

        Pair<String, Optional<String>> result = Median.of(words, 1, 4);
        Assertions.assertEquals("banana", result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfComparableArrayExceptions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of(new String[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Median.of((String[]) null));
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
    public void testOfArrayWithComparatorAndRange() {
        String[] words = { "elephant", "ant", "bee", "tiger" };

        Pair<String, Optional<String>> result = Median.of(words, 1, 4, Comparator.comparing(String::length));
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
    public void testOfCollectionWithComparator() {
        Set<String> words = new HashSet<>(Arrays.asList("apple", "pie", "banana"));

        Pair<String, Optional<String>> result = Median.of(words, Comparator.comparing(String::length));
        Assertions.assertEquals("apple", result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfCollectionWithRange() {
        List<Integer> numbers = Arrays.asList(100, 50, 75, 25, 90);

        Pair<Integer, Optional<Integer>> result = Median.of(numbers, 1, 4);
        Assertions.assertEquals(50, result.left());
        Assertions.assertFalse(result.right().isPresent());
    }

    @Test
    public void testOfCollectionWithRangeAndComparator() {
        List<String> words = Arrays.asList("elephant", "ant", "bee", "tiger", "cat");

        Pair<String, Optional<String>> result = Median.of(words, 1, 4, Comparator.comparing(String::length));
        Assertions.assertEquals("bee", result.left());
        Assertions.assertFalse(result.right().isPresent());
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
}
