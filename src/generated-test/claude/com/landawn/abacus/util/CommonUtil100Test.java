package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class CommonUtil100Test extends TestBase {

    @Test
    public void testCheckFromToIndex() {
        // Normal cases
        N.checkFromToIndex(0, 5, 10);
        N.checkFromToIndex(2, 7, 10);
        N.checkFromToIndex(0, 0, 10);
        N.checkFromToIndex(10, 10, 10);
        
        // Edge cases
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(-1, 5, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(5, 2, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(0, 11, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(11, 11, 10));
    }

    @Test
    public void testCheckFromIndexSize() {
        // Normal cases
        N.checkFromIndexSize(0, 5, 10);
        N.checkFromIndexSize(5, 5, 10);
        N.checkFromIndexSize(0, 0, 10);
        N.checkFromIndexSize(10, 0, 10);
        
        // Edge cases
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(-1, 5, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(0, -1, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(0, 5, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(6, 5, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(0, 11, 10));
    }

    @Test
    public void testCheckIndex() {
        // Normal cases
        Assertions.assertEquals(0, N.checkIndex(0, 10));
        Assertions.assertEquals(5, N.checkIndex(5, 10));
        Assertions.assertEquals(9, N.checkIndex(9, 10));
        
        // Edge cases
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkIndex(-1, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkIndex(10, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkIndex(0, -1));
    }

    @Test
    public void testCheckElementIndex() {
        // Test without description
        Assertions.assertEquals(0, N.checkElementIndex(0, 10));
        Assertions.assertEquals(5, N.checkElementIndex(5, 10));
        Assertions.assertEquals(9, N.checkElementIndex(9, 10));
        
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(-1, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(10, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkElementIndex(0, -1));
        
        // Test with description
        Assertions.assertEquals(5, N.checkElementIndex(5, 10, "myIndex"));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(-1, 10, "myIndex"));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(10, 10, "myIndex"));
    }

    @Test
    public void testCheckPositionIndex() {
        // Test without description
        Assertions.assertEquals(0, N.checkPositionIndex(0, 10));
        Assertions.assertEquals(5, N.checkPositionIndex(5, 10));
        Assertions.assertEquals(10, N.checkPositionIndex(10, 10));
        
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkPositionIndex(-1, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkPositionIndex(11, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkPositionIndex(0, -1));
        
        // Test with description
        Assertions.assertEquals(5, N.checkPositionIndex(5, 10, "myPosition"));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkPositionIndex(-1, 10, "myPosition"));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkPositionIndex(11, 10, "myPosition"));
    }

    @Test
    public void testCheckArgNotNull() {
        // Test without error message
        String str = "test";
        Assertions.assertEquals(str, N.checkArgNotNull(str));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null));
        
        // Test with error message
        Assertions.assertEquals(str, N.checkArgNotNull(str, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null, "this is a longer error message"));
    }

    @Test
    public void testCheckArgNotEmptyCharSequence() {
        // Normal cases
        Assertions.assertEquals("test", N.checkArgNotEmpty("test", "myArg"));
        Assertions.assertEquals("a", N.checkArgNotEmpty("a", "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((CharSequence) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty("", "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((CharSequence) null, "this is a longer error message"));
    }

    @Test
    public void testCheckArgNotEmptyBooleanArray() {
        // Normal cases
        boolean[] arr = {true, false};
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((boolean[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new boolean[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyCharArray() {
        // Normal cases
        char[] arr = {'a', 'b'};
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((char[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new char[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyByteArray() {
        // Normal cases
        byte[] arr = {1, 2};
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((byte[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new byte[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyShortArray() {
        // Normal cases
        short[] arr = {1, 2};
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((short[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new short[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyIntArray() {
        // Normal cases
        int[] arr = {1, 2};
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((int[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new int[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyLongArray() {
        // Normal cases
        long[] arr = {1L, 2L};
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((long[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new long[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyFloatArray() {
        // Normal cases
        float[] arr = {1.0f, 2.0f};
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((float[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new float[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyDoubleArray() {
        // Normal cases
        double[] arr = {1.0, 2.0};
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((double[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new double[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyObjectArray() {
        // Normal cases
        String[] arr = {"a", "b"};
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((String[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new String[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyCollection() {
        // Normal cases
        List<String> list = Arrays.asList("a", "b");
        Assertions.assertEquals(list, N.checkArgNotEmpty(list, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Collection<?>) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new ArrayList<>(), "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyIterable() {
        // Normal cases
        List<String> list = Arrays.asList("a", "b");
        Assertions.assertEquals(list, N.checkArgNotEmpty((Iterable<?>) list, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Iterable<?>) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Iterable<?>) new ArrayList<>(), "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyIterator() {
        // Normal cases
        List<String> list = Arrays.asList("a", "b");
        Iterator<String> iter = list.iterator();
        Assertions.assertEquals(iter, N.checkArgNotEmpty(iter, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Iterator<?>) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new ArrayList<>().iterator(), "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyMap() {
        // Normal cases
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Assertions.assertEquals(map, N.checkArgNotEmpty(map, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Map<?, ?>) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new HashMap<>(), "myArg"));
    }

    @Test
    public void testCheckArgNotBlank() {
        // Normal cases
        Assertions.assertEquals("test", N.checkArgNotBlank("test", "myArg"));
        Assertions.assertEquals("a", N.checkArgNotBlank("a", "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank(null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank("", "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank(" ", "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank("   ", "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank("\t\n", "myArg"));
    }

    @Test
    public void testCheckArgNotNegativeByte() {
        // Normal cases
        Assertions.assertEquals((byte) 0, N.checkArgNotNegative((byte) 0, "myArg"));
        Assertions.assertEquals((byte) 10, N.checkArgNotNegative((byte) 10, "myArg"));
        Assertions.assertEquals(Byte.MAX_VALUE, N.checkArgNotNegative(Byte.MAX_VALUE, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative((byte) -1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(Byte.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgNotNegativeShort() {
        // Normal cases
        Assertions.assertEquals((short) 0, N.checkArgNotNegative((short) 0, "myArg"));
        Assertions.assertEquals((short) 10, N.checkArgNotNegative((short) 10, "myArg"));
        Assertions.assertEquals(Short.MAX_VALUE, N.checkArgNotNegative(Short.MAX_VALUE, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative((short) -1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(Short.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgNotNegativeInt() {
        // Normal cases
        Assertions.assertEquals(0, N.checkArgNotNegative(0, "myArg"));
        Assertions.assertEquals(10, N.checkArgNotNegative(10, "myArg"));
        Assertions.assertEquals(Integer.MAX_VALUE, N.checkArgNotNegative(Integer.MAX_VALUE, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(Integer.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgNotNegativeLong() {
        // Normal cases
        Assertions.assertEquals(0L, N.checkArgNotNegative(0L, "myArg"));
        Assertions.assertEquals(10L, N.checkArgNotNegative(10L, "myArg"));
        Assertions.assertEquals(Long.MAX_VALUE, N.checkArgNotNegative(Long.MAX_VALUE, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-1L, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(Long.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgNotNegativeFloat() {
        // Normal cases
        Assertions.assertEquals(0.0f, N.checkArgNotNegative(0.0f, "myArg"));
        Assertions.assertEquals(10.5f, N.checkArgNotNegative(10.5f, "myArg"));
        Assertions.assertEquals(Float.MAX_VALUE, N.checkArgNotNegative(Float.MAX_VALUE, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-0.1f, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-Float.MAX_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgNotNegativeDouble() {
        // Normal cases
        Assertions.assertEquals(0.0, N.checkArgNotNegative(0.0, "myArg"));
        Assertions.assertEquals(10.5, N.checkArgNotNegative(10.5, "myArg"));
        Assertions.assertEquals(Double.MAX_VALUE, N.checkArgNotNegative(Double.MAX_VALUE, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-0.1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-Double.MAX_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgPositiveByte() {
        // Normal cases
        Assertions.assertEquals((byte) 1, N.checkArgPositive((byte) 1, "myArg"));
        Assertions.assertEquals((byte) 10, N.checkArgPositive((byte) 10, "myArg"));
        Assertions.assertEquals(Byte.MAX_VALUE, N.checkArgPositive(Byte.MAX_VALUE, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((byte) 0, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((byte) -1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(Byte.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgPositiveShort() {
        // Normal cases
        Assertions.assertEquals((short) 1, N.checkArgPositive((short) 1, "myArg"));
        Assertions.assertEquals((short) 10, N.checkArgPositive((short) 10, "myArg"));
        Assertions.assertEquals(Short.MAX_VALUE, N.checkArgPositive(Short.MAX_VALUE, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((short) 0, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((short) -1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(Short.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgPositiveInt() {
        // Normal cases
        Assertions.assertEquals(1, N.checkArgPositive(1, "myArg"));
        Assertions.assertEquals(10, N.checkArgPositive(10, "myArg"));
        Assertions.assertEquals(Integer.MAX_VALUE, N.checkArgPositive(Integer.MAX_VALUE, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(Integer.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgPositiveLong() {
        // Normal cases
        Assertions.assertEquals(1L, N.checkArgPositive(1L, "myArg"));
        Assertions.assertEquals(10L, N.checkArgPositive(10L, "myArg"));
        Assertions.assertEquals(Long.MAX_VALUE, N.checkArgPositive(Long.MAX_VALUE, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0L, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-1L, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(Long.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgPositiveFloat() {
        // Normal cases
        Assertions.assertEquals(0.1f, N.checkArgPositive(0.1f, "myArg"));
        Assertions.assertEquals(10.5f, N.checkArgPositive(10.5f, "myArg"));
        Assertions.assertEquals(Float.MAX_VALUE, N.checkArgPositive(Float.MAX_VALUE, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0.0f, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-0.1f, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-Float.MAX_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgPositiveDouble() {
        // Normal cases
        Assertions.assertEquals(0.1, N.checkArgPositive(0.1, "myArg"));
        Assertions.assertEquals(10.5, N.checkArgPositive(10.5, "myArg"));
        Assertions.assertEquals(Double.MAX_VALUE, N.checkArgPositive(Double.MAX_VALUE, "myArg"));
        
        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0.0, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-0.1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-Double.MAX_VALUE, "myArg"));
    }

    @Test
    public void testCheckElementNotNullArray() {
        // Test without error message
        String[] arr = {"a", "b", "c"};
        N.checkElementNotNull(arr);
        
        String[] nullArr = {"a", null, "c"};
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(nullArr));
        
        // Empty and null arrays should pass
        N.checkElementNotNull(new String[0]);
        N.checkElementNotNull((String[]) null);
        
        // Test with error message
        N.checkElementNotNull(arr, "myArray");
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(nullArr, "myArray"));
    }

    @Test
    public void testCheckElementNotNullCollection() {
        // Test without error message
        List<String> list = Arrays.asList("a", "b", "c");
        N.checkElementNotNull(list);
        
        List<String> nullList = Arrays.asList("a", null, "c");
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(nullList));
        
        // Empty and null collections should pass
        N.checkElementNotNull(new ArrayList<>());
        N.checkElementNotNull((Collection<?>) null);
        
        // Test with error message
        N.checkElementNotNull(list, "myList");
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(nullList, "myList"));
    }

    @Test
    public void testCheckKeyNotNull() {
        // Test without error message
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        N.checkKeyNotNull(map);
        
        Map<String, Integer> nullKeyMap = new HashMap<>();
        nullKeyMap.put("a", 1);
        nullKeyMap.put(null, 2);
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkKeyNotNull(nullKeyMap));
        
        // Empty and null maps should pass
        N.checkKeyNotNull(new HashMap<>());
        N.checkKeyNotNull((Map<?, ?>) null);
        
        // Test with error message
        N.checkKeyNotNull(map, "myMap");
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkKeyNotNull(nullKeyMap, "myMap"));
    }

    @Test
    public void testCheckValueNotNull() {
        // Test without error message
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        N.checkValueNotNull(map);
        
        Map<String, Integer> nullValueMap = new HashMap<>();
        nullValueMap.put("a", 1);
        nullValueMap.put("b", null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkValueNotNull(nullValueMap));
        
        // Empty and null maps should pass
        N.checkValueNotNull(new HashMap<>());
        N.checkValueNotNull((Map<?, ?>) null);
        
        // Test with error message
        N.checkValueNotNull(map, "myMap");
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkValueNotNull(nullValueMap, "myMap"));
    }

    @Test
    public void testCheckArgument() {
        // Test without parameters
        N.checkArgument(true);
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false));
        
        // Test with object error message
        N.checkArgument(true, "Error");
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Error"));
        
        // Test with template and args
        N.checkArgument(true, "Error: %s", "test");
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Error: %s", "test"));
        
        // Test with primitive parameters
        N.checkArgument(true, "Error: %s", 'c');
        N.checkArgument(true, "Error: %s", 10);
        N.checkArgument(true, "Error: %s", 10L);
        N.checkArgument(true, "Error: %s", 10.5);
        N.checkArgument(true, "Error: %s", new Object());
        
        // Test with two parameters
        N.checkArgument(true, "Error: %s %s", 'a', 'b');
        N.checkArgument(true, "Error: %s %s", 'a', 10);
        N.checkArgument(true, "Error: %s %s", 10, 20);
        N.checkArgument(true, "Error: %s %s", 10L, 20L);
        N.checkArgument(true, "Error: %s %s", new Object(), new Object());
        
        // Test with three and four parameters
        N.checkArgument(true, "Error: %s %s %s", "a", "b", "c");
        N.checkArgument(true, "Error: %s %s %s %s", "a", "b", "c", "d");
        
        // Test with supplier
        N.checkArgument(true, () -> "Error from supplier");
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, () -> "Error from supplier"));
    }

    @Test
    public void testCheckState() {
        // Test without parameters
        N.checkState(true);
        Assertions.assertThrows(IllegalStateException.class, () -> N.checkState(false));
        
        // Test with object error message
        N.checkState(true, "Error");
        Assertions.assertThrows(IllegalStateException.class, () -> N.checkState(false, "Error"));
        
        // Test with template and args
        N.checkState(true, "Error: %s", "test");
        Assertions.assertThrows(IllegalStateException.class, () -> N.checkState(false, "Error: %s", "test"));
        
        // Test with primitive parameters
        N.checkState(true, "Error: %s", 'c');
        N.checkState(true, "Error: %s", 10);
        N.checkState(true, "Error: %s", 10L);
        N.checkState(true, "Error: %s", 10.5);
        N.checkState(true, "Error: %s", new Object());
        
        // Test with two parameters
        N.checkState(true, "Error: %s %s", 'a', 'b');
        N.checkState(true, "Error: %s %s", 'a', 10);
        N.checkState(true, "Error: %s %s", 10, 20);
        N.checkState(true, "Error: %s %s", 10L, 20L);
        N.checkState(true, "Error: %s %s", new Object(), new Object());
        
        // Test with three and four parameters
        N.checkState(true, "Error: %s %s %s", "a", "b", "c");
        N.checkState(true, "Error: %s %s %s %s", "a", "b", "c", "d");
        
        // Test with supplier
        N.checkState(true, () -> "Error from supplier");
        Assertions.assertThrows(IllegalStateException.class, () -> N.checkState(false, () -> "Error from supplier"));
    }

    @Test
    public void testRequireNonNull() {
        // Test without error message
        String str = "test";
        Assertions.assertEquals(str, N.requireNonNull(str));
        Assertions.assertThrows(NullPointerException.class, () -> N.requireNonNull(null));
        
        // Test with error message
        Assertions.assertEquals(str, N.requireNonNull(str, "myArg"));
        Assertions.assertThrows(NullPointerException.class, () -> N.requireNonNull(null, "myArg"));
        Assertions.assertThrows(NullPointerException.class, () -> N.requireNonNull(null, "this is a longer error message"));
        
        // Test with supplier
        Assertions.assertEquals(str, N.requireNonNull(str, () -> "myArg"));
        Assertions.assertThrows(NullPointerException.class, () -> N.requireNonNull(null, () -> "myArg"));
    }

    @Test
    public void testEqualsPrimitives() {
        // boolean
        Assertions.assertTrue(N.equals(true, true));
        Assertions.assertTrue(N.equals(false, false));
        Assertions.assertFalse(N.equals(true, false));
        
        // char
        Assertions.assertTrue(N.equals('a', 'a'));
        Assertions.assertFalse(N.equals('a', 'b'));
        
        // byte
        Assertions.assertTrue(N.equals((byte) 10, (byte) 10));
        Assertions.assertFalse(N.equals((byte) 10, (byte) 20));
        
        // short
        Assertions.assertTrue(N.equals((short) 10, (short) 10));
        Assertions.assertFalse(N.equals((short) 10, (short) 20));
        
        // int
        Assertions.assertTrue(N.equals(10, 10));
        Assertions.assertFalse(N.equals(10, 20));
        
        // long
        Assertions.assertTrue(N.equals(10L, 10L));
        Assertions.assertFalse(N.equals(10L, 20L));
        
        // float
        Assertions.assertTrue(N.equals(10.5f, 10.5f));
        Assertions.assertFalse(N.equals(10.5f, 20.5f));
        Assertions.assertTrue(N.equals(Float.NaN, Float.NaN));
        
        // double
        Assertions.assertTrue(N.equals(10.5, 10.5));
        Assertions.assertFalse(N.equals(10.5, 20.5));
        Assertions.assertTrue(N.equals(Double.NaN, Double.NaN));
    }

    @Test
    public void testEqualsStrings() {
        Assertions.assertTrue(N.equals("test", "test"));
        Assertions.assertFalse(N.equals("test", "Test"));
        Assertions.assertTrue(N.equals((String) null, null));
        Assertions.assertFalse(N.equals("test", null));
        Assertions.assertFalse(N.equals(null, "test"));
        
        // equalsIgnoreCase
        Assertions.assertTrue(N.equalsIgnoreCase("test", "TEST"));
        Assertions.assertTrue(N.equalsIgnoreCase("Test", "test"));
        Assertions.assertTrue(N.equalsIgnoreCase((String) null, null));
        Assertions.assertFalse(N.equalsIgnoreCase("test", null));
        Assertions.assertFalse(N.equalsIgnoreCase(null, "test"));
    }

    @Test
    public void testEqualsObjects() {
        Object a = new Object();
        Object b = new Object();
        
        Assertions.assertTrue(N.equals(a, a));
        Assertions.assertFalse(N.equals(a, b));
        Assertions.assertTrue(N.equals((Object) null, null));
        Assertions.assertFalse(N.equals(a, null));
        Assertions.assertFalse(N.equals(null, b));
        
        // Test with arrays
        int[] arr1 = {1, 2, 3};
        int[] arr2 = {1, 2, 3};
        int[] arr3 = {1, 2, 4};
        
        Assertions.assertTrue(N.equals(arr1, arr1));
        Assertions.assertTrue(N.equals(arr1, arr2));
        Assertions.assertFalse(N.equals(arr1, arr3));
        
        // Test with different array types
        Assertions.assertFalse(N.equals(new int[] { 1 }, new long[] { 1 }));
    }

    @Test
    public void testEqualsArrays() {
        // boolean arrays
        Assertions.assertTrue(N.equals(new boolean[] { true, false }, new boolean[] { true, false }));
        Assertions.assertFalse(N.equals(new boolean[] { true, false }, new boolean[] { false, true }));
        Assertions.assertTrue(N.equals((boolean[]) null, null));
        Assertions.assertFalse(N.equals(new boolean[] { true }, null));
        
        // Test with range
        boolean[] boolArr1 = {true, false, true, false};
        boolean[] boolArr2 = {false, true, false, true};
        Assertions.assertTrue(N.equals(boolArr1, 1, boolArr2, 2, 2));
        Assertions.assertFalse(N.equals(boolArr1, 0, boolArr2, 0, 2));
        
        // char arrays
        Assertions.assertTrue(N.equals(new char[] { 'a', 'b' }, new char[] { 'a', 'b' }));
        Assertions.assertFalse(N.equals(new char[] { 'a', 'b' }, new char[] { 'b', 'a' }));
        
        // Test with range
        char[] charArr1 = {'a', 'b', 'c', 'd'};
        char[] charArr2 = {'x', 'b', 'c', 'y'};
        Assertions.assertTrue(N.equals(charArr1, 1, charArr2, 1, 2));
        
        // byte arrays
        Assertions.assertTrue(N.equals(new byte[] { 1, 2 }, new byte[] { 1, 2 }));
        Assertions.assertFalse(N.equals(new byte[] { 1, 2 }, new byte[] { 2, 1 }));
        
        // Test with range
        byte[] byteArr1 = {1, 2, 3, 4};
        byte[] byteArr2 = {5, 2, 3, 6};
        Assertions.assertTrue(N.equals(byteArr1, 1, byteArr2, 1, 2));
        
        // short arrays
        Assertions.assertTrue(N.equals(new short[] { 1, 2 }, new short[] { 1, 2 }));
        Assertions.assertFalse(N.equals(new short[] { 1, 2 }, new short[] { 2, 1 }));
        
        // Test with range
        short[] shortArr1 = {1, 2, 3, 4};
        short[] shortArr2 = {5, 2, 3, 6};
        Assertions.assertTrue(N.equals(shortArr1, 1, shortArr2, 1, 2));
        
        // int arrays
        Assertions.assertTrue(N.equals(new int[] { 1, 2 }, new int[] { 1, 2 }));
        Assertions.assertFalse(N.equals(new int[] { 1, 2 }, new int[] { 2, 1 }));
        
        // Test with range
        int[] intArr1 = {1, 2, 3, 4};
        int[] intArr2 = {5, 2, 3, 6};
        Assertions.assertTrue(N.equals(intArr1, 1, intArr2, 1, 2));
        
        // long arrays
        Assertions.assertTrue(N.equals(new long[] { 1L, 2L }, new long[] { 1L, 2L }));
        Assertions.assertFalse(N.equals(new long[] { 1L, 2L }, new long[] { 2L, 1L }));
        
        // Test with range
        long[] longArr1 = {1L, 2L, 3L, 4L};
        long[] longArr2 = {5L, 2L, 3L, 6L};
        Assertions.assertTrue(N.equals(longArr1, 1, longArr2, 1, 2));
    }
}