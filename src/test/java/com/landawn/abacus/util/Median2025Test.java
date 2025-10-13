package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

@Tag("2025")
public class Median2025Test extends TestBase {

    @Test
    public void testOfChar_SingleElement() {
        Pair<Character, OptionalChar> result = Median.of('a');
        Assertions.assertEquals('a', result.left());
        Assertions.assertFalse(result.right().isPresent());
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
    public void testOfCharRange_SingleElement() {
        char[] chars = { 'a', 'b', 'c' };
        Pair<Character, OptionalChar> result = Median.of(chars, 0, 1);
        Assertions.assertEquals('a', result.left());
        Assertions.assertFalse(result.right().isPresent());
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
    public void testOfCharRange_InvalidRange() {
        char[] chars = { 'a', 'b', 'c' };
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Median.of(chars, 2, 2);
        });
    }

    @Test
    public void testOfByte_SingleElement() {
        Pair<Byte, OptionalByte> result = Median.of((byte) 10);
        Assertions.assertEquals((byte) 10, result.left());
        Assertions.assertFalse(result.right().isPresent());
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
    public void testOfByteRange_SingleElement() {
        byte[] bytes = { 30, 10, 20, 40 };
        Pair<Byte, OptionalByte> result = Median.of(bytes, 0, 1);
        Assertions.assertEquals((byte) 30, result.left());
        Assertions.assertFalse(result.right().isPresent());
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
    public void testOfShort_SingleElement() {
        Pair<Short, OptionalShort> result = Median.of((short) 100);
        Assertions.assertEquals((short) 100, result.left());
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
    public void testOfShortRange_SingleElement() {
        short[] values = { 300, 100, 200, 400 };
        Pair<Short, OptionalShort> result = Median.of(values, 0, 1);
        Assertions.assertEquals((short) 300, result.left());
        Assertions.assertFalse(result.right().isPresent());
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
    public void testOfInt_SingleElement() {
        Pair<Integer, OptionalInt> result = Median.of(10);
        Assertions.assertEquals(10, result.left());
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
    public void testOfIntRange_SingleElement() {
        int[] numbers = { 100, 50, 75, 25, 90 };
        Pair<Integer, OptionalInt> result = Median.of(numbers, 0, 1);
        Assertions.assertEquals(100, result.left());
        Assertions.assertFalse(result.right().isPresent());
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
    public void testOfIntRange_InvalidRange() {
        int[] numbers = { 100, 50, 75 };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            Median.of(numbers, 0, 5);
        });
    }

    @Test
    public void testOfLong_SingleElement() {
        Pair<Long, OptionalLong> result = Median.of(1000L);
        Assertions.assertEquals(1000L, result.left());
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
    public void testOfLongRange_SingleElement() {
        long[] values = { 3000L, 1000L, 2000L, 4000L, 1500L };
        Pair<Long, OptionalLong> result = Median.of(values, 0, 1);
        Assertions.assertEquals(3000L, result.left());
        Assertions.assertFalse(result.right().isPresent());
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
    public void testOfFloat_SingleElement() {
        Pair<Float, OptionalFloat> result = Median.of(10.5f);
        Assertions.assertEquals(10.5f, result.left());
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
    public void testOfFloatRange_SingleElement() {
        float[] values = { 30.5f, 10.2f, 20.8f, 40.1f };
        Pair<Float, OptionalFloat> result = Median.of(values, 0, 1);
        Assertions.assertEquals(30.5f, result.left());
        Assertions.assertFalse(result.right().isPresent());
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
    public void testOfDouble_SingleElement() {
        Pair<Double, OptionalDouble> result = Median.of(10.5);
        Assertions.assertEquals(10.5, result.left());
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
    public void testOfDoubleRange_SingleElement() {
        double[] values = { 30.5, 10.2, 20.8, 40.1, 15.3 };
        Pair<Double, OptionalDouble> result = Median.of(values, 0, 1);
        Assertions.assertEquals(30.5, result.left());
        Assertions.assertFalse(result.right().isPresent());
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
    public void testOfComparableArray_SingleElement() {
        String[] words = { "apple" };
        Pair<String, Optional<String>> result = Median.of(words);
        Assertions.assertEquals("apple", result.left());
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
    public void testOfComparableArrayRange_SingleElement() {
        String[] words = { "zebra", "apple", "banana", "cherry" };
        Pair<String, Optional<String>> result = Median.of(words, 1, 2);
        Assertions.assertEquals("apple", result.left());
        Assertions.assertFalse(result.right().isPresent());
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
    public void testOfArrayWithComparator_SingleElement() {
        String[] words = { "apple" };
        Pair<String, Optional<String>> result = Median.of(words, Comparator.comparing(String::length));
        Assertions.assertEquals("apple", result.left());
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
    public void testOfCollectionWithComparator_SingleElement() {
        Set<String> words = new HashSet<>(Arrays.asList("apple"));
        Pair<String, Optional<String>> result = Median.of(words, Comparator.comparing(String::length));
        Assertions.assertEquals("apple", result.left());
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
    public void testOfCollectionWithComparator_NullComparator() {
        List<String> words = Arrays.asList("apple", "banana", "cherry");
        Pair<String, Optional<String>> result = Median.of(words, null);
        Assertions.assertEquals("banana", result.left());
        Assertions.assertFalse(result.right().isPresent());
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
    public void testOfCollectionRange_SingleElement() {
        List<Integer> numbers = Arrays.asList(100, 50, 75, 25, 90);
        Pair<Integer, Optional<Integer>> result = Median.of(numbers, 0, 1);
        Assertions.assertEquals(100, result.left());
        Assertions.assertFalse(result.right().isPresent());
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
    public void testOfCollectionRange_InvalidRange() {
        List<Integer> numbers = Arrays.asList(100, 50, 75);
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            Median.of(numbers, 0, 5);
        });
    }

    @Test
    public void testOfCollectionRangeWithComparator_SingleElement() {
        List<String> words = Arrays.asList("elephant", "ant", "bee", "tiger", "cat");
        Pair<String, Optional<String>> result = Median.of(words, 1, 2, Comparator.comparing(String::length));
        Assertions.assertEquals("ant", result.left());
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
    public void testOfCollectionRangeWithComparator_NullComparator() {
        List<String> words = Arrays.asList("elephant", "ant", "bee", "tiger", "cat");
        Pair<String, Optional<String>> result = Median.of(words, 1, 4, null);
        Assertions.assertEquals("bee", result.left());
        Assertions.assertFalse(result.right().isPresent());
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
}
