package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class CommonUtil100Test extends TestBase {

    // Tests for checkFromToIndex
    @Test
    public void testCheckFromToIndex_ValidRange() {
        // Should not throw exception for valid range
        N.checkFromToIndex(0, 5, 10);
        N.checkFromToIndex(2, 7, 10);
        N.checkFromToIndex(0, 0, 10);
        N.checkFromToIndex(10, 10, 10);
    }

    @Test
    public void testCheckFromToIndex_InvalidRange() {
        // fromIndex < 0
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(-1, 5, 10));

        // fromIndex > toIndex
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(6, 5, 10));

        // toIndex > length
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(0, 11, 10));

        // All invalid
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(-1, 11, 10));
    }

    // Tests for checkFromIndexSize
    @Test
    public void testCheckFromIndexSize_ValidRange() {
        // Should not throw exception for valid range
        N.checkFromIndexSize(0, 5, 10);
        N.checkFromIndexSize(5, 5, 10);
        N.checkFromIndexSize(0, 0, 10);
        N.checkFromIndexSize(10, 0, 10);
    }

    @Test
    public void testCheckFromIndexSize_InvalidRange() {
        // fromIndex < 0
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(-1, 5, 10));

        // size < 0
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(0, -1, 10));

        // length < 0
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(0, 5, -1));

        // size > length - fromIndex
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(5, 6, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(0, 11, 10));
    }

    // Tests for checkIndex (deprecated) and checkElementIndex
    @Test
    public void testCheckIndex_ValidIndex() {
        Assertions.assertEquals(0, N.checkIndex(0, 10));
        Assertions.assertEquals(5, N.checkIndex(5, 10));
        Assertions.assertEquals(9, N.checkIndex(9, 10));
    }

    @Test
    public void testCheckIndex_InvalidIndex() {
        // index < 0
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkIndex(-1, 10));

        // index >= size
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkIndex(10, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkIndex(11, 10));

        // size < 0
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkIndex(0, -1));
    }

    @Test
    public void testCheckElementIndex_ValidIndex() {
        Assertions.assertEquals(0, N.checkElementIndex(0, 10));
        Assertions.assertEquals(5, N.checkElementIndex(5, 10));
        Assertions.assertEquals(9, N.checkElementIndex(9, 10));
    }

    @Test
    public void testCheckElementIndex_InvalidIndex() {
        // index < 0
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(-1, 10));

        // index >= size
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(10, 10));

        // size < 0
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkElementIndex(0, -1));
    }

    @Test
    public void testCheckElementIndex_WithDescription() {
        Assertions.assertEquals(5, N.checkElementIndex(5, 10, "myIndex"));

        Exception ex = Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(-1, 10, "myIndex"));
        Assertions.assertTrue(ex.getMessage().contains("myIndex"));

        ex = Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(10, 10, "myIndex"));
        Assertions.assertTrue(ex.getMessage().contains("myIndex"));
    }

    // Tests for checkPositionIndex
    @Test
    public void testCheckPositionIndex_ValidIndex() {
        Assertions.assertEquals(0, N.checkPositionIndex(0, 10));
        Assertions.assertEquals(5, N.checkPositionIndex(5, 10));
        Assertions.assertEquals(10, N.checkPositionIndex(10, 10)); // Note: position can equal size
    }

    @Test
    public void testCheckPositionIndex_InvalidIndex() {
        // index < 0
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkPositionIndex(-1, 10));

        // index > size
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkPositionIndex(11, 10));

        // size < 0
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkPositionIndex(0, -1));
    }

    @Test
    public void testCheckPositionIndex_WithDescription() {
        Assertions.assertEquals(5, N.checkPositionIndex(5, 10, "myPosition"));

        Exception ex = Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkPositionIndex(-1, 10, "myPosition"));
        Assertions.assertTrue(ex.getMessage().contains("myPosition"));
    }

    // Tests for checkArgNotNull
    @Test
    public void testCheckArgNotNull_NotNull() {
        String str = "test";
        Assertions.assertSame(str, N.checkArgNotNull(str));

        Integer num = 42;
        Assertions.assertSame(num, N.checkArgNotNull(num));
    }

    @Test
    public void testCheckArgNotNull_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null));
    }

    @Test
    public void testCheckArgNotNull_WithMessage() {
        String str = "test";
        Assertions.assertSame(str, N.checkArgNotNull(str, "str"));

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null, "myArg"));
        Assertions.assertTrue(ex.getMessage().contains("myArg"));
        Assertions.assertTrue(ex.getMessage().contains("cannot be null"));

        // Test with custom error message
        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null, "Custom error message here"));
        Assertions.assertEquals("Custom error message here", ex.getMessage());
    }

    // Tests for checkArgNotEmpty - CharSequence
    @Test
    public void testCheckArgNotEmpty_CharSequence() {
        String str = "test";
        Assertions.assertSame(str, N.checkArgNotEmpty(str, "str"));

        StringBuilder sb = new StringBuilder("test");
        Assertions.assertSame(sb, N.checkArgNotEmpty(sb, "sb"));
    }

    @Test
    public void testCheckArgNotEmpty_CharSequence_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((CharSequence) null, "str"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty("", "str"));
    }

    // Tests for checkArgNotEmpty - boolean[]
    @Test
    public void testCheckArgNotEmpty_BooleanArray() {
        boolean[] arr = { true, false };
        Assertions.assertSame(arr, N.checkArgNotEmpty(arr, "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_BooleanArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((boolean[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new boolean[0], "arr"));
    }

    // Tests for checkArgNotEmpty - char[]
    @Test
    public void testCheckArgNotEmpty_CharArray() {
        char[] arr = { 'a', 'b' };
        Assertions.assertSame(arr, N.checkArgNotEmpty(arr, "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_CharArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((char[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new char[0], "arr"));
    }

    // Tests for checkArgNotEmpty - byte[]
    @Test
    public void testCheckArgNotEmpty_ByteArray() {
        byte[] arr = { 1, 2 };
        Assertions.assertSame(arr, N.checkArgNotEmpty(arr, "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ByteArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((byte[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new byte[0], "arr"));
    }

    // Tests for checkArgNotEmpty - short[]
    @Test
    public void testCheckArgNotEmpty_ShortArray() {
        short[] arr = { 1, 2 };
        Assertions.assertSame(arr, N.checkArgNotEmpty(arr, "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ShortArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((short[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new short[0], "arr"));
    }

    // Tests for checkArgNotEmpty - int[]
    @Test
    public void testCheckArgNotEmpty_IntArray() {
        int[] arr = { 1, 2 };
        Assertions.assertSame(arr, N.checkArgNotEmpty(arr, "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_IntArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((int[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new int[0], "arr"));
    }

    // Tests for checkArgNotEmpty - long[]
    @Test
    public void testCheckArgNotEmpty_LongArray() {
        long[] arr = { 1L, 2L };
        Assertions.assertSame(arr, N.checkArgNotEmpty(arr, "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_LongArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((long[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new long[0], "arr"));
    }

    // Tests for checkArgNotEmpty - float[]
    @Test
    public void testCheckArgNotEmpty_FloatArray() {
        float[] arr = { 1.0f, 2.0f };
        Assertions.assertSame(arr, N.checkArgNotEmpty(arr, "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_FloatArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((float[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new float[0], "arr"));
    }

    // Tests for checkArgNotEmpty - double[]
    @Test
    public void testCheckArgNotEmpty_DoubleArray() {
        double[] arr = { 1.0, 2.0 };
        Assertions.assertSame(arr, N.checkArgNotEmpty(arr, "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_DoubleArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((double[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new double[0], "arr"));
    }

    // Tests for checkArgNotEmpty - Object[]
    @Test
    public void testCheckArgNotEmpty_ObjectArray() {
        String[] arr = { "a", "b" };
        Assertions.assertSame(arr, N.checkArgNotEmpty(arr, "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ObjectArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((String[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new String[0], "arr"));
    }

    // Tests for checkArgNotEmpty - Collection
    @Test
    public void testCheckArgNotEmpty_Collection() {
        List<String> list = Arrays.asList("a", "b");
        Assertions.assertSame(list, N.checkArgNotEmpty(list, "list"));

        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2));
        Assertions.assertSame(set, N.checkArgNotEmpty(set, "set"));
    }

    @Test
    public void testCheckArgNotEmpty_Collection_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Collection<?>) null, "coll"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new ArrayList<>(), "coll"));
    }

    // Tests for checkArgNotEmpty - Map
    @Test
    public void testCheckArgNotEmpty_Map() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Assertions.assertSame(map, N.checkArgNotEmpty(map, "map"));
    }

    @Test
    public void testCheckArgNotEmpty_Map_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Map<?, ?>) null, "map"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new HashMap<>(), "map"));
    }

    // Tests for checkArgNotBlank
    @Test
    public void testCheckArgNotBlank_Valid() {
        String str = "test";
        Assertions.assertSame(str, N.checkArgNotBlank(str, "str"));

        StringBuilder sb = new StringBuilder("  test  ");
        Assertions.assertSame(sb, N.checkArgNotBlank(sb, "sb"));
    }

    @Test
    public void testCheckArgNotBlank_Invalid() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank(null, "str"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank("", "str"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank("   ", "str"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank("\t\n", "str"));
    }

    // Tests for checkArgNotNegative - byte
    @Test
    public void testCheckArgNotNegative_Byte() {
        Assertions.assertEquals((byte) 0, N.checkArgNotNegative((byte) 0, "val"));
        Assertions.assertEquals((byte) 10, N.checkArgNotNegative((byte) 10, "val"));
        Assertions.assertEquals(Byte.MAX_VALUE, N.checkArgNotNegative(Byte.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Byte_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative((byte) -1, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(Byte.MIN_VALUE, "val"));
    }

    // Tests for checkArgNotNegative - short
    @Test
    public void testCheckArgNotNegative_Short() {
        Assertions.assertEquals((short) 0, N.checkArgNotNegative((short) 0, "val"));
        Assertions.assertEquals((short) 10, N.checkArgNotNegative((short) 10, "val"));
        Assertions.assertEquals(Short.MAX_VALUE, N.checkArgNotNegative(Short.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Short_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative((short) -1, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(Short.MIN_VALUE, "val"));
    }

    // Tests for checkArgNotNegative - int
    @Test
    public void testCheckArgNotNegative_Int() {
        Assertions.assertEquals(0, N.checkArgNotNegative(0, "val"));
        Assertions.assertEquals(10, N.checkArgNotNegative(10, "val"));
        Assertions.assertEquals(Integer.MAX_VALUE, N.checkArgNotNegative(Integer.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Int_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-1, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(Integer.MIN_VALUE, "val"));
    }

    // Tests for checkArgNotNegative - long
    @Test
    public void testCheckArgNotNegative_Long() {
        Assertions.assertEquals(0L, N.checkArgNotNegative(0L, "val"));
        Assertions.assertEquals(10L, N.checkArgNotNegative(10L, "val"));
        Assertions.assertEquals(Long.MAX_VALUE, N.checkArgNotNegative(Long.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Long_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-1L, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(Long.MIN_VALUE, "val"));
    }

    // Tests for checkArgNotNegative - float
    @Test
    public void testCheckArgNotNegative_Float() {
        Assertions.assertEquals(0.0f, N.checkArgNotNegative(0.0f, "val"));
        Assertions.assertEquals(10.5f, N.checkArgNotNegative(10.5f, "val"));
        Assertions.assertEquals(Float.MAX_VALUE, N.checkArgNotNegative(Float.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Float_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-0.1f, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-Float.MAX_VALUE, "val"));
    }

    // Tests for checkArgNotNegative - double
    @Test
    public void testCheckArgNotNegative_Double() {
        Assertions.assertEquals(0.0, N.checkArgNotNegative(0.0, "val"));
        Assertions.assertEquals(10.5, N.checkArgNotNegative(10.5, "val"));
        Assertions.assertEquals(Double.MAX_VALUE, N.checkArgNotNegative(Double.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Double_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-0.1, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-Double.MAX_VALUE, "val"));
    }

    // Tests for checkArgPositive - byte
    @Test
    public void testCheckArgPositive_Byte() {
        Assertions.assertEquals((byte) 1, N.checkArgPositive((byte) 1, "val"));
        Assertions.assertEquals((byte) 10, N.checkArgPositive((byte) 10, "val"));
        Assertions.assertEquals(Byte.MAX_VALUE, N.checkArgPositive(Byte.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgPositive_Byte_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((byte) 0, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((byte) -1, "val"));
    }

    // Tests for checkArgPositive - short
    @Test
    public void testCheckArgPositive_Short() {
        Assertions.assertEquals((short) 1, N.checkArgPositive((short) 1, "val"));
        Assertions.assertEquals((short) 10, N.checkArgPositive((short) 10, "val"));
        Assertions.assertEquals(Short.MAX_VALUE, N.checkArgPositive(Short.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgPositive_Short_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((short) 0, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((short) -1, "val"));
    }

    // Tests for checkArgPositive - int
    @Test
    public void testCheckArgPositive_Int() {
        Assertions.assertEquals(1, N.checkArgPositive(1, "val"));
        Assertions.assertEquals(10, N.checkArgPositive(10, "val"));
        Assertions.assertEquals(Integer.MAX_VALUE, N.checkArgPositive(Integer.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgPositive_Int_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-1, "val"));
    }

    // Tests for checkArgPositive - long
    @Test
    public void testCheckArgPositive_Long() {
        Assertions.assertEquals(1L, N.checkArgPositive(1L, "val"));
        Assertions.assertEquals(10L, N.checkArgPositive(10L, "val"));
        Assertions.assertEquals(Long.MAX_VALUE, N.checkArgPositive(Long.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgPositive_Long_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0L, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-1L, "val"));
    }

    // Tests for checkArgPositive - float
    @Test
    public void testCheckArgPositive_Float() {
        Assertions.assertEquals(0.1f, N.checkArgPositive(0.1f, "val"));
        Assertions.assertEquals(10.5f, N.checkArgPositive(10.5f, "val"));
        Assertions.assertEquals(Float.MAX_VALUE, N.checkArgPositive(Float.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgPositive_Float_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0.0f, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-0.1f, "val"));
    }

    // Tests for checkArgPositive - double
    @Test
    public void testCheckArgPositive_Double() {
        Assertions.assertEquals(0.1, N.checkArgPositive(0.1, "val"));
        Assertions.assertEquals(10.5, N.checkArgPositive(10.5, "val"));
        Assertions.assertEquals(Double.MAX_VALUE, N.checkArgPositive(Double.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgPositive_Double_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0.0, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-0.1, "val"));
    }

    // Tests for checkElementNotNull - Object[]
    @Test
    public void testCheckElementNotNull_ObjectArray_NoNulls() {
        // Should not throw for arrays without nulls
        N.checkElementNotNull(new String[] { "a", "b", "c" });
        N.checkElementNotNull(new Integer[] { 1, 2, 3 });
        N.checkElementNotNull(new Object[0]); // Empty array
    }

    @Test
    public void testCheckElementNotNull_ObjectArray_WithNulls() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(new String[] { "a", null, "c" }));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(new Object[] { null }));
    }

    @Test
    public void testCheckElementNotNull_ObjectArray_WithMessage() {
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(new String[] { "a", null }, "myArray"));
        Assertions.assertTrue(ex.getMessage().contains("myArray"));
    }

    // Tests for checkElementNotNull - Collection
    @Test
    public void testCheckElementNotNull_Collection_NoNulls() {
        N.checkElementNotNull(Arrays.asList("a", "b", "c"));
        N.checkElementNotNull(new HashSet<>(Arrays.asList(1, 2, 3)));
        N.checkElementNotNull(new ArrayList<>()); // Empty collection
    }

    @Test
    public void testCheckElementNotNull_Collection_WithNulls() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(Arrays.asList("a", null, "c")));

        List<String> list = new ArrayList<>();
        list.add("a");
        list.add(null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(list));
    }

    @Test
    public void testCheckElementNotNull_Collection_WithMessage() {
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(Arrays.asList("a", null), "myList"));
        Assertions.assertTrue(ex.getMessage().contains("myList"));
    }

    // Tests for checkKeyNotNull
    @Test
    public void testCheckKeyNotNull_NoNullKeys() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        N.checkKeyNotNull(map);

        N.checkKeyNotNull(new HashMap<>()); // Empty map
    }

    @Test
    public void testCheckKeyNotNull_WithNullKeys() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put(null, 2);
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkKeyNotNull(map));
    }

    @Test
    public void testCheckKeyNotNull_WithMessage() {
        Map<String, Integer> map = new HashMap<>();
        map.put(null, 1);
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkKeyNotNull(map, "myMap"));
        Assertions.assertTrue(ex.getMessage().contains("myMap"));
    }

    // Tests for checkValueNotNull
    @Test
    public void testCheckValueNotNull_NoNullValues() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        N.checkValueNotNull(map);

        N.checkValueNotNull(new HashMap<>()); // Empty map
    }

    @Test
    public void testCheckValueNotNull_WithNullValues() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkValueNotNull(map));
    }

    @Test
    public void testCheckValueNotNull_WithMessage() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", null);
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkValueNotNull(map, "myMap"));
        Assertions.assertTrue(ex.getMessage().contains("myMap"));
    }

    // Tests for checkArgument - boolean only
    @Test
    public void testCheckArgument_BooleanOnly() {
        // Should not throw
        N.checkArgument(true);

        // Should throw
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false));
    }

    // Tests for checkArgument - with Object message
    @Test
    public void testCheckArgument_WithObjectMessage() {
        N.checkArgument(true, "message");
        N.checkArgument(true, 123);
        N.checkArgument(true, null);

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "error message"));
        Assertions.assertEquals("error message", ex.getMessage());

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, 123));
        Assertions.assertEquals("123", ex.getMessage());
    }

    // Tests for checkArgument - with template and varargs
    @Test
    public void testCheckArgument_WithTemplateAndVarargs() {
        N.checkArgument(true, "value is %s", 10);

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "value %s is invalid", 10));
        Assertions.assertTrue(ex.getMessage().contains("10"));

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "values %s and %s are invalid", 10, 20));
        Assertions.assertTrue(ex.getMessage().contains("10"));
        Assertions.assertTrue(ex.getMessage().contains("20"));

        // Test with extra args
        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "value %s", 10, 20, 30));
        Assertions.assertTrue(ex.getMessage().contains("[20, 30]"));
    }

    // Tests for checkArgument - with template and primitive parameters
    @Test
    public void testCheckArgument_WithTemplateAndChar() {
        N.checkArgument(true, "char is %s", 'a');

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "char %s is invalid", 'x'));
        Assertions.assertTrue(ex.getMessage().contains("x"));
    }

    @Test
    public void testCheckArgument_WithTemplateAndInt() {
        N.checkArgument(true, "int is %s", 42);

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "int %s is invalid", 42));
        Assertions.assertTrue(ex.getMessage().contains("42"));
    }

    @Test
    public void testCheckArgument_WithTemplateAndLong() {
        N.checkArgument(true, "long is %s", 42L);

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "long %s is invalid", 42L));
        Assertions.assertTrue(ex.getMessage().contains("42"));
    }

    @Test
    public void testCheckArgument_WithTemplateAndDouble() {
        N.checkArgument(true, "double is %s", 3.14);

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "double %s is invalid", 3.14));
        Assertions.assertTrue(ex.getMessage().contains("3.14"));
    }

    @Test
    public void testCheckArgument_WithTemplateAndObject() {
        N.checkArgument(true, "object is %s", "test");

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "object %s is invalid", "test"));
        Assertions.assertTrue(ex.getMessage().contains("test"));
    }

    // Tests for checkArgument - with two parameters (all combinations)
    @Test
    public void testCheckArgument_TwoParams_CharChar() {
        N.checkArgument(true, "chars are %s and %s", 'a', 'b');

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "chars %s and %s", 'x', 'y'));
        Assertions.assertTrue(ex.getMessage().contains("x"));
        Assertions.assertTrue(ex.getMessage().contains("y"));
    }

    @Test
    public void testCheckArgument_TwoParams_IntInt() {
        N.checkArgument(true, "ints are %s and %s", 1, 2);

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "ints %s and %s", 10, 20));
        Assertions.assertTrue(ex.getMessage().contains("10"));
        Assertions.assertTrue(ex.getMessage().contains("20"));
    }

    @Test
    public void testCheckArgument_TwoParams_LongLong() {
        N.checkArgument(true, "longs are %s and %s", 1L, 2L);

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "longs %s and %s", 10L, 20L));
        Assertions.assertTrue(ex.getMessage().contains("10"));
        Assertions.assertTrue(ex.getMessage().contains("20"));
    }

    @Test
    public void testCheckArgument_TwoParams_DoubleDouble() {
        N.checkArgument(true, "doubles are %s and %s", 1.1, 2.2);

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "doubles %s and %s", 1.1, 2.2));
        Assertions.assertTrue(ex.getMessage().contains("1.1"));
        Assertions.assertTrue(ex.getMessage().contains("2.2"));
    }

    @Test
    public void testCheckArgument_TwoParams_ObjectObject() {
        N.checkArgument(true, "objects are %s and %s", "a", "b");

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "objects %s and %s", "x", "y"));
        Assertions.assertTrue(ex.getMessage().contains("x"));
        Assertions.assertTrue(ex.getMessage().contains("y"));
    }

    @Test
    public void testCheckArgument_TwoParams_MixedTypes() {
        // Test various mixed type combinations
        N.checkArgument(true, "%s and %s", 'a', 10);
        N.checkArgument(true, "%s and %s", 10, 3.14);
        N.checkArgument(true, "%s and %s", 3.14, "test");
        N.checkArgument(true, "%s and %s", "test", 100L);
    }

    // Tests for checkArgument - with three and four parameters
    @Test
    public void testCheckArgument_ThreeParams() {
        N.checkArgument(true, "values are %s, %s, %s", "a", "b", "c");

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "values %s, %s, %s", 1, 2, 3));
        Assertions.assertTrue(ex.getMessage().contains("1"));
        Assertions.assertTrue(ex.getMessage().contains("2"));
        Assertions.assertTrue(ex.getMessage().contains("3"));
    }

    @Test
    public void testCheckArgument_FourParams() {
        N.checkArgument(true, "values are %s, %s, %s, %s", "a", "b", "c", "d");

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "values %s, %s, %s, %s", 1, 2, 3, 4));
        Assertions.assertTrue(ex.getMessage().contains("1"));
        Assertions.assertTrue(ex.getMessage().contains("2"));
        Assertions.assertTrue(ex.getMessage().contains("3"));
        Assertions.assertTrue(ex.getMessage().contains("4"));
    }

    // Tests for checkArgument - with Supplier
    @Test
    public void testCheckArgument_WithSupplier() {
        N.checkArgument(true, () -> "should not be called");

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, () -> "error from supplier"));
        Assertions.assertEquals("error from supplier", ex.getMessage());
    }

    // Tests for checkState - boolean only
    @Test
    public void testCheckState_BooleanOnly() {
        N.checkState(true);

        Assertions.assertThrows(IllegalStateException.class, () -> N.checkState(false));
    }

    // Tests for checkState - with Object message
    @Test
    public void testCheckState_WithObjectMessage() {
        N.checkState(true, "message");

        Exception ex = Assertions.assertThrows(IllegalStateException.class, () -> N.checkState(false, "state error"));
        Assertions.assertEquals("state error", ex.getMessage());
    }

    // Tests for checkState - with template and varargs
    @Test
    public void testCheckState_WithTemplateAndVarargs() {
        N.checkState(true, "state is %s", "valid");

        Exception ex = Assertions.assertThrows(IllegalStateException.class, () -> N.checkState(false, "state %s is invalid", "xyz"));
        Assertions.assertTrue(ex.getMessage().contains("xyz"));
    }

    // Tests for checkState - with template and primitive parameters
    @Test
    public void testCheckState_WithTemplateAndPrimitives() {
        N.checkState(true, "value is %s", 'a');
        N.checkState(true, "value is %s", 10);
        N.checkState(true, "value is %s", 10L);
        N.checkState(true, "value is %s", 3.14);
        N.checkState(true, "value is %s", "test");

        // Test failures
        Exception ex = Assertions.assertThrows(IllegalStateException.class, () -> N.checkState(false, "char %s", 'x'));
        Assertions.assertTrue(ex.getMessage().contains("x"));
    }

    // Tests for checkState - with two parameters (sample combinations)
    @Test
    public void testCheckState_TwoParams() {
        N.checkState(true, "values %s and %s", 10, 20);
        N.checkState(true, "values %s and %s", "a", "b");

        Exception ex = Assertions.assertThrows(IllegalStateException.class, () -> N.checkState(false, "invalid %s and %s", 1, 2));
        Assertions.assertTrue(ex.getMessage().contains("1"));
        Assertions.assertTrue(ex.getMessage().contains("2"));
    }

    // Tests for checkState - with three and four parameters
    @Test
    public void testCheckState_ThreeAndFourParams() {
        N.checkState(true, "values %s, %s, %s", 1, 2, 3);
        N.checkState(true, "values %s, %s, %s, %s", 1, 2, 3, 4);

        Exception ex = Assertions.assertThrows(IllegalStateException.class, () -> N.checkState(false, "bad %s, %s, %s", "x", "y", "z"));
        Assertions.assertTrue(ex.getMessage().contains("x"));
        Assertions.assertTrue(ex.getMessage().contains("y"));
        Assertions.assertTrue(ex.getMessage().contains("z"));
    }

    // Tests for format method variations (testing through error messages)
    @Test
    public void testFormatWithCurlyBraces() {
        // Test {} placeholder
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "value {} is invalid", 42));
        Assertions.assertEquals("value 42 is invalid", ex.getMessage());

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "{} and {}", "first", "second"));
        Assertions.assertEquals("first and second", ex.getMessage());
    }

    @Test
    public void testFormatWithPercentS() {
        // Test %s placeholder
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "value %s is invalid", 42));
        Assertions.assertEquals("value 42 is invalid", ex.getMessage());

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "%s and %s", "first", "second"));
        Assertions.assertEquals("first and second", ex.getMessage());
    }

    @Test
    public void testFormatWithNoPlaceholders() {
        // When no placeholders, args are appended
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "error message", "arg1", "arg2"));
        Assertions.assertEquals("error message: [arg1, arg2]", ex.getMessage());
    }

    @Test
    public void testFormatWithFewerPlaceholdersThanArgs() {
        // Extra args are appended in brackets
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "value %s", 1, 2, 3));
        Assertions.assertEquals("value 1: [2, 3]", ex.getMessage());
    }

    @Test
    public void testFormatWithMorePlaceholdersThanArgs() {
        // Extra placeholders remain as-is
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "%s %s %s", "only one"));
        Assertions.assertEquals("only one %s %s", ex.getMessage());
    }

    // Edge case tests
    @Test
    public void testNullHandling() {
        // checkArgument with null values
        N.checkArgument(true, "value is %s", (Object) null);
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "value %s", (Object) null));
        Assertions.assertTrue(ex.getMessage().contains("null"));

        // Arrays containing nulls in format
        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "%s and %s", null, null));
        Assertions.assertEquals("null and null", ex.getMessage());
    }

    @Test
    public void testEmptyStringHandling() {
        // Empty string as message
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, ""));
        Assertions.assertEquals("", ex.getMessage());

        // Empty string as argument
        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "value is '%s'", ""));
        Assertions.assertEquals("value is ''", ex.getMessage());
    }

    @Test
    public void testSpecialCharactersInMessages() {
        // Test with special characters
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Special chars: \n\t\r"));
        Assertions.assertEquals("Special chars: \n\t\r", ex.getMessage());

        // Test with unicode
        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Unicode: \u2605 %s", "\u2764"));
        Assertions.assertTrue(ex.getMessage().contains("\u2605"));
        Assertions.assertTrue(ex.getMessage().contains("\u2764"));
    }

    @Test
    public void testBoundaryValues() {
        // Test with MAX and MIN values
        N.checkArgNotNegative(0, "zero");
        N.checkArgPositive(1, "one");

        // Test index boundaries
        N.checkElementIndex(0, 1);
        N.checkPositionIndex(1, 1);

        // Test with empty collections/arrays
        N.checkElementNotNull(new Object[0]);
        N.checkElementNotNull(new ArrayList<>());
        N.checkKeyNotNull(new HashMap<>());
        N.checkValueNotNull(new HashMap<>());
    }

    @Test
    public void testMessageFormatConsistency() {
        // Test that error messages are consistent for similar methods
        Exception ex1 = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null, "myArg"));
        Exception ex2 = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty("", "myArg"));
        Exception ex3 = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank("  ", "myArg"));

        // All should contain the argument name
        Assertions.assertTrue(ex1.getMessage().contains("myArg"));
        Assertions.assertTrue(ex2.getMessage().contains("myArg"));
        Assertions.assertTrue(ex3.getMessage().contains("myArg"));
    }

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
        boolean[] arr = { true, false };
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));

        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((boolean[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new boolean[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyCharArray() {
        // Normal cases
        char[] arr = { 'a', 'b' };
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));

        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((char[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new char[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyByteArray() {
        // Normal cases
        byte[] arr = { 1, 2 };
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));

        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((byte[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new byte[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyShortArray() {
        // Normal cases
        short[] arr = { 1, 2 };
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));

        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((short[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new short[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyIntArray() {
        // Normal cases
        int[] arr = { 1, 2 };
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));

        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((int[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new int[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyLongArray() {
        // Normal cases
        long[] arr = { 1L, 2L };
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));

        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((long[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new long[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyFloatArray() {
        // Normal cases
        float[] arr = { 1.0f, 2.0f };
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));

        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((float[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new float[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyDoubleArray() {
        // Normal cases
        double[] arr = { 1.0, 2.0 };
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));

        // Edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((double[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new double[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyObjectArray() {
        // Normal cases
        String[] arr = { "a", "b" };
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
        String[] arr = { "a", "b", "c" };
        N.checkElementNotNull(arr);

        String[] nullArr = { "a", null, "c" };
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
        int[] arr1 = { 1, 2, 3 };
        int[] arr2 = { 1, 2, 3 };
        int[] arr3 = { 1, 2, 4 };

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
        boolean[] boolArr1 = { true, false, true, false };
        boolean[] boolArr2 = { false, true, false, true };
        Assertions.assertTrue(N.equals(boolArr1, 1, boolArr2, 2, 2));
        Assertions.assertFalse(N.equals(boolArr1, 0, boolArr2, 0, 2));

        // char arrays
        Assertions.assertTrue(N.equals(new char[] { 'a', 'b' }, new char[] { 'a', 'b' }));
        Assertions.assertFalse(N.equals(new char[] { 'a', 'b' }, new char[] { 'b', 'a' }));

        // Test with range
        char[] charArr1 = { 'a', 'b', 'c', 'd' };
        char[] charArr2 = { 'x', 'b', 'c', 'y' };
        Assertions.assertTrue(N.equals(charArr1, 1, charArr2, 1, 2));

        // byte arrays
        Assertions.assertTrue(N.equals(new byte[] { 1, 2 }, new byte[] { 1, 2 }));
        Assertions.assertFalse(N.equals(new byte[] { 1, 2 }, new byte[] { 2, 1 }));

        // Test with range
        byte[] byteArr1 = { 1, 2, 3, 4 };
        byte[] byteArr2 = { 5, 2, 3, 6 };
        Assertions.assertTrue(N.equals(byteArr1, 1, byteArr2, 1, 2));

        // short arrays
        Assertions.assertTrue(N.equals(new short[] { 1, 2 }, new short[] { 1, 2 }));
        Assertions.assertFalse(N.equals(new short[] { 1, 2 }, new short[] { 2, 1 }));

        // Test with range
        short[] shortArr1 = { 1, 2, 3, 4 };
        short[] shortArr2 = { 5, 2, 3, 6 };
        Assertions.assertTrue(N.equals(shortArr1, 1, shortArr2, 1, 2));

        // int arrays
        Assertions.assertTrue(N.equals(new int[] { 1, 2 }, new int[] { 1, 2 }));
        Assertions.assertFalse(N.equals(new int[] { 1, 2 }, new int[] { 2, 1 }));

        // Test with range
        int[] intArr1 = { 1, 2, 3, 4 };
        int[] intArr2 = { 5, 2, 3, 6 };
        Assertions.assertTrue(N.equals(intArr1, 1, intArr2, 1, 2));

        // long arrays
        Assertions.assertTrue(N.equals(new long[] { 1L, 2L }, new long[] { 1L, 2L }));
        Assertions.assertFalse(N.equals(new long[] { 1L, 2L }, new long[] { 2L, 1L }));

        // Test with range
        long[] longArr1 = { 1L, 2L, 3L, 4L };
        long[] longArr2 = { 5L, 2L, 3L, 6L };
        Assertions.assertTrue(N.equals(longArr1, 1, longArr2, 1, 2));
    }
}