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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class CommonUtil100Test extends TestBase {

    @Test
    public void testCheckFromToIndex_ValidRange() {
        CommonUtil.checkFromToIndex(0, 5, 10);
        CommonUtil.checkFromToIndex(2, 7, 10);
        CommonUtil.checkFromToIndex(0, 0, 10);
        CommonUtil.checkFromToIndex(10, 10, 10);
    }

    @Test
    public void testCheckFromToIndex_InvalidRange() {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(-1, 5, 10));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(6, 5, 10));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(0, 11, 10));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(-1, 11, 10));
    }

    @Test
    public void testCheckFromIndexSize_ValidRange() {
        CommonUtil.checkFromIndexSize(0, 5, 10);
        CommonUtil.checkFromIndexSize(5, 5, 10);
        CommonUtil.checkFromIndexSize(0, 0, 10);
        CommonUtil.checkFromIndexSize(10, 0, 10);
    }

    @Test
    public void testCheckFromIndexSize_InvalidRange() {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromIndexSize(-1, 5, 10));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkFromIndexSize(0, -1, 10));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkFromIndexSize(0, 5, -1));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromIndexSize(5, 6, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromIndexSize(0, 11, 10));
    }

    @Test
    public void testCheckIndex_ValidIndex() {
        Assertions.assertEquals(0, CommonUtil.checkIndex(0, 10));
        Assertions.assertEquals(5, CommonUtil.checkIndex(5, 10));
        Assertions.assertEquals(9, CommonUtil.checkIndex(9, 10));
    }

    @Test
    public void testCheckIndex_InvalidIndex() {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkIndex(-1, 10));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkIndex(10, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkIndex(11, 10));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkIndex(0, -1));
    }

    @Test
    public void testCheckElementIndex_ValidIndex() {
        Assertions.assertEquals(0, CommonUtil.checkElementIndex(0, 10));
        Assertions.assertEquals(5, CommonUtil.checkElementIndex(5, 10));
        Assertions.assertEquals(9, CommonUtil.checkElementIndex(9, 10));
    }

    @Test
    public void testCheckElementIndex_InvalidIndex() {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(-1, 10));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(10, 10));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementIndex(0, -1));
    }

    @Test
    public void testCheckElementIndex_WithDescription() {
        Assertions.assertEquals(5, CommonUtil.checkElementIndex(5, 10, "myIndex"));

        Exception ex = Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(-1, 10, "myIndex"));
        Assertions.assertTrue(ex.getMessage().contains("myIndex"));

        ex = Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(10, 10, "myIndex"));
        Assertions.assertTrue(ex.getMessage().contains("myIndex"));
    }

    @Test
    public void testCheckPositionIndex_ValidIndex() {
        Assertions.assertEquals(0, CommonUtil.checkPositionIndex(0, 10));
        Assertions.assertEquals(5, CommonUtil.checkPositionIndex(5, 10));
        Assertions.assertEquals(10, CommonUtil.checkPositionIndex(10, 10));
    }

    @Test
    public void testCheckPositionIndex_InvalidIndex() {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkPositionIndex(-1, 10));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkPositionIndex(11, 10));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkPositionIndex(0, -1));
    }

    @Test
    public void testCheckPositionIndex_WithDescription() {
        Assertions.assertEquals(5, CommonUtil.checkPositionIndex(5, 10, "myPosition"));

        Exception ex = Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkPositionIndex(-1, 10, "myPosition"));
        Assertions.assertTrue(ex.getMessage().contains("myPosition"));
    }

    @Test
    public void testCheckArgNotNull_NotNull() {
        String str = "test";
        Assertions.assertSame(str, CommonUtil.checkArgNotNull(str));

        Integer num = 42;
        Assertions.assertSame(num, CommonUtil.checkArgNotNull(num));
    }

    @Test
    public void testCheckArgNotNull_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null));
    }

    @Test
    public void testCheckArgNotNull_WithMessage() {
        String str = "test";
        Assertions.assertSame(str, CommonUtil.checkArgNotNull(str, "str"));

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null, "myArg"));
        Assertions.assertTrue(ex.getMessage().contains("myArg"));
        Assertions.assertTrue(ex.getMessage().contains("cannot be null"));

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null, "Custom error message here"));
        Assertions.assertEquals("Custom error message here", ex.getMessage());
    }

    @Test
    public void testCheckArgNotEmpty_CharSequence() {
        String str = "test";
        Assertions.assertSame(str, CommonUtil.checkArgNotEmpty(str, "str"));

        StringBuilder sb = new StringBuilder("test");
        Assertions.assertSame(sb, CommonUtil.checkArgNotEmpty(sb, "sb"));
    }

    @Test
    public void testCheckArgNotEmpty_CharSequence_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((CharSequence) null, "str"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty("", "str"));
    }

    @Test
    public void testCheckArgNotEmpty_BooleanArray() {
        boolean[] arr = { true, false };
        Assertions.assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_BooleanArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((boolean[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new boolean[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_CharArray() {
        char[] arr = { 'a', 'b' };
        Assertions.assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_CharArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((char[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new char[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ByteArray() {
        byte[] arr = { 1, 2 };
        Assertions.assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ByteArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((byte[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new byte[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ShortArray() {
        short[] arr = { 1, 2 };
        Assertions.assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ShortArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((short[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new short[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_IntArray() {
        int[] arr = { 1, 2 };
        Assertions.assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_IntArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((int[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new int[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_LongArray() {
        long[] arr = { 1L, 2L };
        Assertions.assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_LongArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((long[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new long[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_FloatArray() {
        float[] arr = { 1.0f, 2.0f };
        Assertions.assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_FloatArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((float[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new float[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_DoubleArray() {
        double[] arr = { 1.0, 2.0 };
        Assertions.assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_DoubleArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((double[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new double[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ObjectArray() {
        String[] arr = { "a", "b" };
        Assertions.assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ObjectArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((String[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new String[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_Collection() {
        List<String> list = Arrays.asList("a", "b");
        Assertions.assertSame(list, CommonUtil.checkArgNotEmpty(list, "list"));

        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2));
        Assertions.assertSame(set, CommonUtil.checkArgNotEmpty(set, "set"));
    }

    @Test
    public void testCheckArgNotEmpty_Collection_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Collection<?>) null, "coll"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new ArrayList<>(), "coll"));
    }

    @Test
    public void testCheckArgNotEmpty_Map() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Assertions.assertSame(map, CommonUtil.checkArgNotEmpty(map, "map"));
    }

    @Test
    public void testCheckArgNotEmpty_Map_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Map<?, ?>) null, "map"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new HashMap<>(), "map"));
    }

    @Test
    public void testCheckArgNotBlank_Valid() {
        String str = "test";
        Assertions.assertSame(str, CommonUtil.checkArgNotBlank(str, "str"));

        StringBuilder sb = new StringBuilder("  test  ");
        Assertions.assertSame(sb, CommonUtil.checkArgNotBlank(sb, "sb"));
    }

    @Test
    public void testCheckArgNotBlank_Invalid() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank(null, "str"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank("", "str"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank("   ", "str"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank("\t\n", "str"));
    }

    @Test
    public void testCheckArgNotNegative_Byte() {
        Assertions.assertEquals((byte) 0, CommonUtil.checkArgNotNegative((byte) 0, "val"));
        Assertions.assertEquals((byte) 10, CommonUtil.checkArgNotNegative((byte) 10, "val"));
        Assertions.assertEquals(Byte.MAX_VALUE, CommonUtil.checkArgNotNegative(Byte.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Byte_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative((byte) -1, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(Byte.MIN_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Short() {
        Assertions.assertEquals((short) 0, CommonUtil.checkArgNotNegative((short) 0, "val"));
        Assertions.assertEquals((short) 10, CommonUtil.checkArgNotNegative((short) 10, "val"));
        Assertions.assertEquals(Short.MAX_VALUE, CommonUtil.checkArgNotNegative(Short.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Short_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative((short) -1, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(Short.MIN_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Int() {
        Assertions.assertEquals(0, CommonUtil.checkArgNotNegative(0, "val"));
        Assertions.assertEquals(10, CommonUtil.checkArgNotNegative(10, "val"));
        Assertions.assertEquals(Integer.MAX_VALUE, CommonUtil.checkArgNotNegative(Integer.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Int_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(Integer.MIN_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Long() {
        Assertions.assertEquals(0L, CommonUtil.checkArgNotNegative(0L, "val"));
        Assertions.assertEquals(10L, CommonUtil.checkArgNotNegative(10L, "val"));
        Assertions.assertEquals(Long.MAX_VALUE, CommonUtil.checkArgNotNegative(Long.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Long_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1L, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(Long.MIN_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Float() {
        Assertions.assertEquals(0.0f, CommonUtil.checkArgNotNegative(0.0f, "val"));
        Assertions.assertEquals(10.5f, CommonUtil.checkArgNotNegative(10.5f, "val"));
        Assertions.assertEquals(Float.MAX_VALUE, CommonUtil.checkArgNotNegative(Float.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Float_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-0.1f, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-Float.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Double() {
        Assertions.assertEquals(0.0, CommonUtil.checkArgNotNegative(0.0, "val"));
        Assertions.assertEquals(10.5, CommonUtil.checkArgNotNegative(10.5, "val"));
        Assertions.assertEquals(Double.MAX_VALUE, CommonUtil.checkArgNotNegative(Double.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Double_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-0.1, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-Double.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgPositive_Byte() {
        Assertions.assertEquals((byte) 1, CommonUtil.checkArgPositive((byte) 1, "val"));
        Assertions.assertEquals((byte) 10, CommonUtil.checkArgPositive((byte) 10, "val"));
        Assertions.assertEquals(Byte.MAX_VALUE, CommonUtil.checkArgPositive(Byte.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgPositive_Byte_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((byte) 0, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((byte) -1, "val"));
    }

    @Test
    public void testCheckArgPositive_Short() {
        Assertions.assertEquals((short) 1, CommonUtil.checkArgPositive((short) 1, "val"));
        Assertions.assertEquals((short) 10, CommonUtil.checkArgPositive((short) 10, "val"));
        Assertions.assertEquals(Short.MAX_VALUE, CommonUtil.checkArgPositive(Short.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgPositive_Short_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((short) 0, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((short) -1, "val"));
    }

    @Test
    public void testCheckArgPositive_Int() {
        Assertions.assertEquals(1, CommonUtil.checkArgPositive(1, "val"));
        Assertions.assertEquals(10, CommonUtil.checkArgPositive(10, "val"));
        Assertions.assertEquals(Integer.MAX_VALUE, CommonUtil.checkArgPositive(Integer.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgPositive_Int_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-1, "val"));
    }

    @Test
    public void testCheckArgPositive_Long() {
        Assertions.assertEquals(1L, CommonUtil.checkArgPositive(1L, "val"));
        Assertions.assertEquals(10L, CommonUtil.checkArgPositive(10L, "val"));
        Assertions.assertEquals(Long.MAX_VALUE, CommonUtil.checkArgPositive(Long.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgPositive_Long_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0L, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-1L, "val"));
    }

    @Test
    public void testCheckArgPositive_Float() {
        Assertions.assertEquals(0.1f, CommonUtil.checkArgPositive(0.1f, "val"));
        Assertions.assertEquals(10.5f, CommonUtil.checkArgPositive(10.5f, "val"));
        Assertions.assertEquals(Float.MAX_VALUE, CommonUtil.checkArgPositive(Float.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgPositive_Float_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0.0f, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-0.1f, "val"));
    }

    @Test
    public void testCheckArgPositive_Double() {
        Assertions.assertEquals(0.1, CommonUtil.checkArgPositive(0.1, "val"));
        Assertions.assertEquals(10.5, CommonUtil.checkArgPositive(10.5, "val"));
        Assertions.assertEquals(Double.MAX_VALUE, CommonUtil.checkArgPositive(Double.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgPositive_Double_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0.0, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-0.1, "val"));
    }

    @Test
    public void testCheckElementNotNull_ObjectArray_NoNulls() {
        CommonUtil.checkElementNotNull(new String[] { "a", "b", "c" });
        CommonUtil.checkElementNotNull(new Integer[] { 1, 2, 3 });
        CommonUtil.checkElementNotNull(new Object[0]);
    }

    @Test
    public void testCheckElementNotNull_ObjectArray_WithNulls() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(new String[] { "a", null, "c" }));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(new Object[] { null }));
    }

    @Test
    public void testCheckElementNotNull_ObjectArray_WithMessage() {
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(new String[] { "a", null }, "myArray"));
        Assertions.assertTrue(ex.getMessage().contains("myArray"));
    }

    @Test
    public void testCheckElementNotNull_Collection_NoNulls() {
        CommonUtil.checkElementNotNull(Arrays.asList("a", "b", "c"));
        CommonUtil.checkElementNotNull(new HashSet<>(Arrays.asList(1, 2, 3)));
        CommonUtil.checkElementNotNull(new ArrayList<>());
    }

    @Test
    public void testCheckElementNotNull_Collection_WithNulls() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(Arrays.asList("a", null, "c")));

        List<String> list = new ArrayList<>();
        list.add("a");
        list.add(null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(list));
    }

    @Test
    public void testCheckElementNotNull_Collection_WithMessage() {
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(Arrays.asList("a", null), "myList"));
        Assertions.assertTrue(ex.getMessage().contains("myList"));
    }

    @Test
    public void testCheckKeyNotNull_NoNullKeys() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        CommonUtil.checkKeyNotNull(map);

        CommonUtil.checkKeyNotNull(new HashMap<>());
    }

    @Test
    public void testCheckKeyNotNull_WithNullKeys() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put(null, 2);
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkKeyNotNull(map));
    }

    @Test
    public void testCheckKeyNotNull_WithMessage() {
        Map<String, Integer> map = new HashMap<>();
        map.put(null, 1);
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkKeyNotNull(map, "myMap"));
        Assertions.assertTrue(ex.getMessage().contains("myMap"));
    }

    @Test
    public void testCheckValueNotNull_NoNullValues() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        CommonUtil.checkValueNotNull(map);

        CommonUtil.checkValueNotNull(new HashMap<>());
    }

    @Test
    public void testCheckValueNotNull_WithNullValues() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkValueNotNull(map));
    }

    @Test
    public void testCheckValueNotNull_WithMessage() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", null);
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkValueNotNull(map, "myMap"));
        Assertions.assertTrue(ex.getMessage().contains("myMap"));
    }

    @Test
    public void testCheckArgument_BooleanOnly() {
        CommonUtil.checkArgument(true);

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false));
    }

    @Test
    public void testCheckArgument_WithObjectMessage() {
        CommonUtil.checkArgument(true, "message");
        CommonUtil.checkArgument(true, 123);
        CommonUtil.checkArgument(true, null);

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "error message"));
        Assertions.assertEquals("error message", ex.getMessage());

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, 123));
        Assertions.assertEquals("123", ex.getMessage());
    }

    @Test
    public void testCheckArgument_WithTemplateAndVarargs() {
        CommonUtil.checkArgument(true, "value is %s", 10);

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "value %s is invalid", 10));
        Assertions.assertTrue(ex.getMessage().contains("10"));

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "values %s and %s are invalid", 10, 20));
        Assertions.assertTrue(ex.getMessage().contains("10"));
        Assertions.assertTrue(ex.getMessage().contains("20"));

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "value %s", 10, 20, 30));
        Assertions.assertTrue(ex.getMessage().contains("[20, 30]"));
    }

    @Test
    public void testCheckArgument_WithTemplateAndChar() {
        CommonUtil.checkArgument(true, "char is %s", 'a');

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "char %s is invalid", 'x'));
        Assertions.assertTrue(ex.getMessage().contains("x"));
    }

    @Test
    public void testCheckArgument_WithTemplateAndInt() {
        CommonUtil.checkArgument(true, "int is %s", 42);

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "int %s is invalid", 42));
        Assertions.assertTrue(ex.getMessage().contains("42"));
    }

    @Test
    public void testCheckArgument_WithTemplateAndLong() {
        CommonUtil.checkArgument(true, "long is %s", 42L);

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "long %s is invalid", 42L));
        Assertions.assertTrue(ex.getMessage().contains("42"));
    }

    @Test
    public void testCheckArgument_WithTemplateAndDouble() {
        CommonUtil.checkArgument(true, "double is %s", 3.14);

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "double %s is invalid", 3.14));
        Assertions.assertTrue(ex.getMessage().contains("3.14"));
    }

    @Test
    public void testCheckArgument_WithTemplateAndObject() {
        CommonUtil.checkArgument(true, "object is %s", "test");

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "object %s is invalid", "test"));
        Assertions.assertTrue(ex.getMessage().contains("test"));
    }

    @Test
    public void testCheckArgument_TwoParams_CharChar() {
        CommonUtil.checkArgument(true, "chars are %s and %s", 'a', 'b');

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "chars %s and %s", 'x', 'y'));
        Assertions.assertTrue(ex.getMessage().contains("x"));
        Assertions.assertTrue(ex.getMessage().contains("y"));
    }

    @Test
    public void testCheckArgument_TwoParams_IntInt() {
        CommonUtil.checkArgument(true, "ints are %s and %s", 1, 2);

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "ints %s and %s", 10, 20));
        Assertions.assertTrue(ex.getMessage().contains("10"));
        Assertions.assertTrue(ex.getMessage().contains("20"));
    }

    @Test
    public void testCheckArgument_TwoParams_LongLong() {
        CommonUtil.checkArgument(true, "longs are %s and %s", 1L, 2L);

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "longs %s and %s", 10L, 20L));
        Assertions.assertTrue(ex.getMessage().contains("10"));
        Assertions.assertTrue(ex.getMessage().contains("20"));
    }

    @Test
    public void testCheckArgument_TwoParams_DoubleDouble() {
        CommonUtil.checkArgument(true, "doubles are %s and %s", 1.1, 2.2);

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "doubles %s and %s", 1.1, 2.2));
        Assertions.assertTrue(ex.getMessage().contains("1.1"));
        Assertions.assertTrue(ex.getMessage().contains("2.2"));
    }

    @Test
    public void testCheckArgument_TwoParams_ObjectObject() {
        CommonUtil.checkArgument(true, "objects are %s and %s", "a", "b");

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "objects %s and %s", "x", "y"));
        Assertions.assertTrue(ex.getMessage().contains("x"));
        Assertions.assertTrue(ex.getMessage().contains("y"));
    }

    @Test
    public void testCheckArgument_TwoParams_MixedTypes() {
        CommonUtil.checkArgument(true, "%s and %s", 'a', 10);
        CommonUtil.checkArgument(true, "%s and %s", 10, 3.14);
        CommonUtil.checkArgument(true, "%s and %s", 3.14, "test");
        CommonUtil.checkArgument(true, "%s and %s", "test", 100L);
    }

    @Test
    public void testCheckArgument_ThreeParams() {
        CommonUtil.checkArgument(true, "values are %s, %s, %s", "a", "b", "c");

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "values %s, %s, %s", 1, 2, 3));
        Assertions.assertTrue(ex.getMessage().contains("1"));
        Assertions.assertTrue(ex.getMessage().contains("2"));
        Assertions.assertTrue(ex.getMessage().contains("3"));
    }

    @Test
    public void testCheckArgument_FourParams() {
        CommonUtil.checkArgument(true, "values are %s, %s, %s, %s", "a", "b", "c", "d");

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "values %s, %s, %s, %s", 1, 2, 3, 4));
        Assertions.assertTrue(ex.getMessage().contains("1"));
        Assertions.assertTrue(ex.getMessage().contains("2"));
        Assertions.assertTrue(ex.getMessage().contains("3"));
        Assertions.assertTrue(ex.getMessage().contains("4"));
    }

    @Test
    public void testCheckArgument_WithSupplier() {
        CommonUtil.checkArgument(true, () -> "should not be called");

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, () -> "error from supplier"));
        Assertions.assertEquals("error from supplier", ex.getMessage());
    }

    @Test
    public void testCheckState_BooleanOnly() {
        CommonUtil.checkState(true);

        Assertions.assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false));
    }

    @Test
    public void testCheckState_WithObjectMessage() {
        CommonUtil.checkState(true, "message");

        Exception ex = Assertions.assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "state error"));
        Assertions.assertEquals("state error", ex.getMessage());
    }

    @Test
    public void testCheckState_WithTemplateAndVarargs() {
        CommonUtil.checkState(true, "state is %s", "valid");

        Exception ex = Assertions.assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "state %s is invalid", "xyz"));
        Assertions.assertTrue(ex.getMessage().contains("xyz"));
    }

    @Test
    public void testCheckState_WithTemplateAndPrimitives() {
        CommonUtil.checkState(true, "value is %s", 'a');
        CommonUtil.checkState(true, "value is %s", 10);
        CommonUtil.checkState(true, "value is %s", 10L);
        CommonUtil.checkState(true, "value is %s", 3.14);
        CommonUtil.checkState(true, "value is %s", "test");

        Exception ex = Assertions.assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "char %s", 'x'));
        Assertions.assertTrue(ex.getMessage().contains("x"));
    }

    @Test
    public void testCheckState_TwoParams() {
        CommonUtil.checkState(true, "values %s and %s", 10, 20);
        CommonUtil.checkState(true, "values %s and %s", "a", "b");

        Exception ex = Assertions.assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "invalid %s and %s", 1, 2));
        Assertions.assertTrue(ex.getMessage().contains("1"));
        Assertions.assertTrue(ex.getMessage().contains("2"));
    }

    @Test
    public void testCheckState_ThreeAndFourParams() {
        CommonUtil.checkState(true, "values %s, %s, %s", 1, 2, 3);
        CommonUtil.checkState(true, "values %s, %s, %s, %s", 1, 2, 3, 4);

        Exception ex = Assertions.assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "bad %s, %s, %s", "x", "y", "z"));
        Assertions.assertTrue(ex.getMessage().contains("x"));
        Assertions.assertTrue(ex.getMessage().contains("y"));
        Assertions.assertTrue(ex.getMessage().contains("z"));
    }

    @Test
    public void testFormatWithCurlyBraces() {
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "value {} is invalid", 42));
        Assertions.assertEquals("value 42 is invalid", ex.getMessage());

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "{} and {}", "first", "second"));
        Assertions.assertEquals("first and second", ex.getMessage());
    }

    @Test
    public void testFormatWithPercentS() {
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "value %s is invalid", 42));
        Assertions.assertEquals("value 42 is invalid", ex.getMessage());

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s and %s", "first", "second"));
        Assertions.assertEquals("first and second", ex.getMessage());
    }

    @Test
    public void testFormatWithNoPlaceholders() {
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "error message", "arg1", "arg2"));
        Assertions.assertEquals("error message: [arg1, arg2]", ex.getMessage());
    }

    @Test
    public void testFormatWithFewerPlaceholdersThanArgs() {
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "value %s", 1, 2, 3));
        Assertions.assertEquals("value 1: [2, 3]", ex.getMessage());
    }

    @Test
    public void testFormatWithMorePlaceholdersThanArgs() {
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s %s", "only one"));
        Assertions.assertEquals("only one %s %s", ex.getMessage());
    }

    @Test
    public void testNullHandling() {
        CommonUtil.checkArgument(true, "value is %s", (Object) null);
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "value %s", (Object) null));
        Assertions.assertTrue(ex.getMessage().contains("null"));

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s and %s", null, null));
        Assertions.assertEquals("null and null", ex.getMessage());
    }

    @Test
    public void testEmptyStringHandling() {
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, ""));
        Assertions.assertEquals("", ex.getMessage());

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "value is '%s'", ""));
        Assertions.assertEquals("value is ''", ex.getMessage());
    }

    @Test
    public void testSpecialCharactersInMessages() {
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Special chars: \n\t\r"));
        Assertions.assertEquals("Special chars: \n\t\r", ex.getMessage());

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Unicode: \u2605 %s", "\u2764"));
        Assertions.assertTrue(ex.getMessage().contains("\u2605"));
        Assertions.assertTrue(ex.getMessage().contains("\u2764"));
    }

    @Test
    public void testBoundaryValues() {
        CommonUtil.checkArgNotNegative(0, "zero");
        CommonUtil.checkArgPositive(1, "one");

        CommonUtil.checkElementIndex(0, 1);
        CommonUtil.checkPositionIndex(1, 1);

        CommonUtil.checkElementNotNull(new Object[0]);
        CommonUtil.checkElementNotNull(new ArrayList<>());
        CommonUtil.checkKeyNotNull(new HashMap<>());
        CommonUtil.checkValueNotNull(new HashMap<>());
    }

    @Test
    public void testMessageFormatConsistency() {
        Exception ex1 = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null, "myArg"));
        Exception ex2 = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty("", "myArg"));
        Exception ex3 = Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank("  ", "myArg"));

        Assertions.assertTrue(ex1.getMessage().contains("myArg"));
        Assertions.assertTrue(ex2.getMessage().contains("myArg"));
        Assertions.assertTrue(ex3.getMessage().contains("myArg"));
    }

    @Test
    public void testCheckFromToIndex() {
        CommonUtil.checkFromToIndex(0, 5, 10);
        CommonUtil.checkFromToIndex(2, 7, 10);
        CommonUtil.checkFromToIndex(0, 0, 10);
        CommonUtil.checkFromToIndex(10, 10, 10);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(-1, 5, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(5, 2, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(0, 11, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(11, 11, 10));
    }

    @Test
    public void testCheckFromIndexSize() {
        CommonUtil.checkFromIndexSize(0, 5, 10);
        CommonUtil.checkFromIndexSize(5, 5, 10);
        CommonUtil.checkFromIndexSize(0, 0, 10);
        CommonUtil.checkFromIndexSize(10, 0, 10);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromIndexSize(-1, 5, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkFromIndexSize(0, -1, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkFromIndexSize(0, 5, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromIndexSize(6, 5, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromIndexSize(0, 11, 10));
    }

    @Test
    public void testCheckIndex() {
        Assertions.assertEquals(0, CommonUtil.checkIndex(0, 10));
        Assertions.assertEquals(5, CommonUtil.checkIndex(5, 10));
        Assertions.assertEquals(9, CommonUtil.checkIndex(9, 10));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkIndex(-1, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkIndex(10, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkIndex(0, -1));
    }

    @Test
    public void testCheckElementIndex() {
        Assertions.assertEquals(0, CommonUtil.checkElementIndex(0, 10));
        Assertions.assertEquals(5, CommonUtil.checkElementIndex(5, 10));
        Assertions.assertEquals(9, CommonUtil.checkElementIndex(9, 10));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(-1, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(10, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementIndex(0, -1));

        Assertions.assertEquals(5, CommonUtil.checkElementIndex(5, 10, "myIndex"));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(-1, 10, "myIndex"));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(10, 10, "myIndex"));
    }

    @Test
    public void testCheckPositionIndex() {
        Assertions.assertEquals(0, CommonUtil.checkPositionIndex(0, 10));
        Assertions.assertEquals(5, CommonUtil.checkPositionIndex(5, 10));
        Assertions.assertEquals(10, CommonUtil.checkPositionIndex(10, 10));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkPositionIndex(-1, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkPositionIndex(11, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkPositionIndex(0, -1));

        Assertions.assertEquals(5, CommonUtil.checkPositionIndex(5, 10, "myPosition"));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkPositionIndex(-1, 10, "myPosition"));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkPositionIndex(11, 10, "myPosition"));
    }

    @Test
    public void testCheckArgNotNull() {
        String str = "test";
        Assertions.assertEquals(str, CommonUtil.checkArgNotNull(str));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null));

        Assertions.assertEquals(str, CommonUtil.checkArgNotNull(str, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null, "this is a longer error message"));
    }

    @Test
    public void testCheckArgNotEmptyCharSequence() {
        Assertions.assertEquals("test", CommonUtil.checkArgNotEmpty("test", "myArg"));
        Assertions.assertEquals("a", CommonUtil.checkArgNotEmpty("a", "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((CharSequence) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty("", "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((CharSequence) null, "this is a longer error message"));
    }

    @Test
    public void testCheckArgNotEmptyBooleanArray() {
        boolean[] arr = { true, false };
        Assertions.assertArrayEquals(arr, CommonUtil.checkArgNotEmpty(arr, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((boolean[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new boolean[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyCharArray() {
        char[] arr = { 'a', 'b' };
        Assertions.assertArrayEquals(arr, CommonUtil.checkArgNotEmpty(arr, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((char[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new char[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyByteArray() {
        byte[] arr = { 1, 2 };
        Assertions.assertArrayEquals(arr, CommonUtil.checkArgNotEmpty(arr, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((byte[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new byte[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyShortArray() {
        short[] arr = { 1, 2 };
        Assertions.assertArrayEquals(arr, CommonUtil.checkArgNotEmpty(arr, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((short[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new short[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyIntArray() {
        int[] arr = { 1, 2 };
        Assertions.assertArrayEquals(arr, CommonUtil.checkArgNotEmpty(arr, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((int[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new int[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyLongArray() {
        long[] arr = { 1L, 2L };
        Assertions.assertArrayEquals(arr, CommonUtil.checkArgNotEmpty(arr, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((long[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new long[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyFloatArray() {
        float[] arr = { 1.0f, 2.0f };
        Assertions.assertArrayEquals(arr, CommonUtil.checkArgNotEmpty(arr, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((float[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new float[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyDoubleArray() {
        double[] arr = { 1.0, 2.0 };
        Assertions.assertArrayEquals(arr, CommonUtil.checkArgNotEmpty(arr, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((double[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new double[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyObjectArray() {
        String[] arr = { "a", "b" };
        Assertions.assertArrayEquals(arr, CommonUtil.checkArgNotEmpty(arr, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((String[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new String[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyCollection() {
        List<String> list = Arrays.asList("a", "b");
        Assertions.assertEquals(list, CommonUtil.checkArgNotEmpty(list, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Collection<?>) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new ArrayList<>(), "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyIterable() {
        List<String> list = Arrays.asList("a", "b");
        Assertions.assertEquals(list, CommonUtil.checkArgNotEmpty((Iterable<?>) list, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Iterable<?>) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Iterable<?>) new ArrayList<>(), "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyIterator() {
        List<String> list = Arrays.asList("a", "b");
        Iterator<String> iter = list.iterator();
        Assertions.assertEquals(iter, CommonUtil.checkArgNotEmpty(iter, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Iterator<?>) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new ArrayList<>().iterator(), "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Assertions.assertEquals(map, CommonUtil.checkArgNotEmpty(map, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Map<?, ?>) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new HashMap<>(), "myArg"));
    }

    @Test
    public void testCheckArgNotBlank() {
        Assertions.assertEquals("test", CommonUtil.checkArgNotBlank("test", "myArg"));
        Assertions.assertEquals("a", CommonUtil.checkArgNotBlank("a", "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank(null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank("", "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank(" ", "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank("   ", "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank("\t\n", "myArg"));
    }

    @Test
    public void testCheckArgNotNegativeByte() {
        Assertions.assertEquals((byte) 0, CommonUtil.checkArgNotNegative((byte) 0, "myArg"));
        Assertions.assertEquals((byte) 10, CommonUtil.checkArgNotNegative((byte) 10, "myArg"));
        Assertions.assertEquals(Byte.MAX_VALUE, CommonUtil.checkArgNotNegative(Byte.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative((byte) -1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(Byte.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgNotNegativeShort() {
        Assertions.assertEquals((short) 0, CommonUtil.checkArgNotNegative((short) 0, "myArg"));
        Assertions.assertEquals((short) 10, CommonUtil.checkArgNotNegative((short) 10, "myArg"));
        Assertions.assertEquals(Short.MAX_VALUE, CommonUtil.checkArgNotNegative(Short.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative((short) -1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(Short.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgNotNegativeInt() {
        Assertions.assertEquals(0, CommonUtil.checkArgNotNegative(0, "myArg"));
        Assertions.assertEquals(10, CommonUtil.checkArgNotNegative(10, "myArg"));
        Assertions.assertEquals(Integer.MAX_VALUE, CommonUtil.checkArgNotNegative(Integer.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(Integer.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgNotNegativeLong() {
        Assertions.assertEquals(0L, CommonUtil.checkArgNotNegative(0L, "myArg"));
        Assertions.assertEquals(10L, CommonUtil.checkArgNotNegative(10L, "myArg"));
        Assertions.assertEquals(Long.MAX_VALUE, CommonUtil.checkArgNotNegative(Long.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1L, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(Long.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgNotNegativeFloat() {
        Assertions.assertEquals(0.0f, CommonUtil.checkArgNotNegative(0.0f, "myArg"));
        Assertions.assertEquals(10.5f, CommonUtil.checkArgNotNegative(10.5f, "myArg"));
        Assertions.assertEquals(Float.MAX_VALUE, CommonUtil.checkArgNotNegative(Float.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-0.1f, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-Float.MAX_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgNotNegativeDouble() {
        Assertions.assertEquals(0.0, CommonUtil.checkArgNotNegative(0.0, "myArg"));
        Assertions.assertEquals(10.5, CommonUtil.checkArgNotNegative(10.5, "myArg"));
        Assertions.assertEquals(Double.MAX_VALUE, CommonUtil.checkArgNotNegative(Double.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-0.1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-Double.MAX_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgPositiveByte() {
        Assertions.assertEquals((byte) 1, CommonUtil.checkArgPositive((byte) 1, "myArg"));
        Assertions.assertEquals((byte) 10, CommonUtil.checkArgPositive((byte) 10, "myArg"));
        Assertions.assertEquals(Byte.MAX_VALUE, CommonUtil.checkArgPositive(Byte.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((byte) 0, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((byte) -1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(Byte.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgPositiveShort() {
        Assertions.assertEquals((short) 1, CommonUtil.checkArgPositive((short) 1, "myArg"));
        Assertions.assertEquals((short) 10, CommonUtil.checkArgPositive((short) 10, "myArg"));
        Assertions.assertEquals(Short.MAX_VALUE, CommonUtil.checkArgPositive(Short.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((short) 0, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((short) -1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(Short.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgPositiveInt() {
        Assertions.assertEquals(1, CommonUtil.checkArgPositive(1, "myArg"));
        Assertions.assertEquals(10, CommonUtil.checkArgPositive(10, "myArg"));
        Assertions.assertEquals(Integer.MAX_VALUE, CommonUtil.checkArgPositive(Integer.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(Integer.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgPositiveLong() {
        Assertions.assertEquals(1L, CommonUtil.checkArgPositive(1L, "myArg"));
        Assertions.assertEquals(10L, CommonUtil.checkArgPositive(10L, "myArg"));
        Assertions.assertEquals(Long.MAX_VALUE, CommonUtil.checkArgPositive(Long.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0L, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-1L, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(Long.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgPositiveFloat() {
        Assertions.assertEquals(0.1f, CommonUtil.checkArgPositive(0.1f, "myArg"));
        Assertions.assertEquals(10.5f, CommonUtil.checkArgPositive(10.5f, "myArg"));
        Assertions.assertEquals(Float.MAX_VALUE, CommonUtil.checkArgPositive(Float.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0.0f, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-0.1f, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-Float.MAX_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgPositiveDouble() {
        Assertions.assertEquals(0.1, CommonUtil.checkArgPositive(0.1, "myArg"));
        Assertions.assertEquals(10.5, CommonUtil.checkArgPositive(10.5, "myArg"));
        Assertions.assertEquals(Double.MAX_VALUE, CommonUtil.checkArgPositive(Double.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0.0, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-0.1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-Double.MAX_VALUE, "myArg"));
    }

    @Test
    public void testCheckElementNotNullArray() {
        String[] arr = { "a", "b", "c" };
        CommonUtil.checkElementNotNull(arr);

        String[] nullArr = { "a", null, "c" };
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(nullArr));

        CommonUtil.checkElementNotNull(new String[0]);
        CommonUtil.checkElementNotNull((String[]) null);

        CommonUtil.checkElementNotNull(arr, "myArray");
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(nullArr, "myArray"));
    }

    @Test
    public void testCheckElementNotNullCollection() {
        List<String> list = Arrays.asList("a", "b", "c");
        CommonUtil.checkElementNotNull(list);

        List<String> nullList = Arrays.asList("a", null, "c");
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(nullList));

        CommonUtil.checkElementNotNull(new ArrayList<>());
        CommonUtil.checkElementNotNull((Collection<?>) null);

        CommonUtil.checkElementNotNull(list, "myList");
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(nullList, "myList"));
    }

    @Test
    public void testCheckKeyNotNull() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        CommonUtil.checkKeyNotNull(map);

        Map<String, Integer> nullKeyMap = new HashMap<>();
        nullKeyMap.put("a", 1);
        nullKeyMap.put(null, 2);
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkKeyNotNull(nullKeyMap));

        CommonUtil.checkKeyNotNull(new HashMap<>());
        CommonUtil.checkKeyNotNull((Map<?, ?>) null);

        CommonUtil.checkKeyNotNull(map, "myMap");
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkKeyNotNull(nullKeyMap, "myMap"));
    }

    @Test
    public void testCheckValueNotNull() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        CommonUtil.checkValueNotNull(map);

        Map<String, Integer> nullValueMap = new HashMap<>();
        nullValueMap.put("a", 1);
        nullValueMap.put("b", null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkValueNotNull(nullValueMap));

        CommonUtil.checkValueNotNull(new HashMap<>());
        CommonUtil.checkValueNotNull((Map<?, ?>) null);

        CommonUtil.checkValueNotNull(map, "myMap");
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkValueNotNull(nullValueMap, "myMap"));
    }

    @Test
    public void testCheckArgument() {
        CommonUtil.checkArgument(true);
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false));

        CommonUtil.checkArgument(true, "Error");
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Error"));

        CommonUtil.checkArgument(true, "Error: %s", "test");
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Error: %s", "test"));

        CommonUtil.checkArgument(true, "Error: %s", 'c');
        CommonUtil.checkArgument(true, "Error: %s", 10);
        CommonUtil.checkArgument(true, "Error: %s", 10L);
        CommonUtil.checkArgument(true, "Error: %s", 10.5);
        CommonUtil.checkArgument(true, "Error: %s", new Object());

        CommonUtil.checkArgument(true, "Error: %s %s", 'a', 'b');
        CommonUtil.checkArgument(true, "Error: %s %s", 'a', 10);
        CommonUtil.checkArgument(true, "Error: %s %s", 10, 20);
        CommonUtil.checkArgument(true, "Error: %s %s", 10L, 20L);
        CommonUtil.checkArgument(true, "Error: %s %s", new Object(), new Object());

        CommonUtil.checkArgument(true, "Error: %s %s %s", "a", "b", "c");
        CommonUtil.checkArgument(true, "Error: %s %s %s %s", "a", "b", "c", "d");

        CommonUtil.checkArgument(true, () -> "Error from supplier");
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, () -> "Error from supplier"));
    }

    @Test
    public void testCheckState() {
        CommonUtil.checkState(true);
        Assertions.assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false));

        CommonUtil.checkState(true, "Error");
        Assertions.assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Error"));

        CommonUtil.checkState(true, "Error: %s", "test");
        Assertions.assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Error: %s", "test"));

        CommonUtil.checkState(true, "Error: %s", 'c');
        CommonUtil.checkState(true, "Error: %s", 10);
        CommonUtil.checkState(true, "Error: %s", 10L);
        CommonUtil.checkState(true, "Error: %s", 10.5);
        CommonUtil.checkState(true, "Error: %s", new Object());

        CommonUtil.checkState(true, "Error: %s %s", 'a', 'b');
        CommonUtil.checkState(true, "Error: %s %s", 'a', 10);
        CommonUtil.checkState(true, "Error: %s %s", 10, 20);
        CommonUtil.checkState(true, "Error: %s %s", 10L, 20L);
        CommonUtil.checkState(true, "Error: %s %s", new Object(), new Object());

        CommonUtil.checkState(true, "Error: %s %s %s", "a", "b", "c");
        CommonUtil.checkState(true, "Error: %s %s %s %s", "a", "b", "c", "d");

        CommonUtil.checkState(true, () -> "Error from supplier");
        Assertions.assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, () -> "Error from supplier"));
    }

    @Test
    public void testRequireNonNull() {
        String str = "test";
        Assertions.assertEquals(str, CommonUtil.requireNonNull(str));
        Assertions.assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null));

        Assertions.assertEquals(str, CommonUtil.requireNonNull(str, "myArg"));
        Assertions.assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null, "myArg"));
        Assertions.assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null, "this is a longer error message"));

        Assertions.assertEquals(str, CommonUtil.requireNonNull(str, () -> "myArg"));
        Assertions.assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null, () -> "myArg"));
    }

    @Test
    public void testEqualsPrimitives() {
        Assertions.assertTrue(CommonUtil.equals(true, true));
        Assertions.assertTrue(CommonUtil.equals(false, false));
        Assertions.assertFalse(CommonUtil.equals(true, false));

        Assertions.assertTrue(CommonUtil.equals('a', 'a'));
        Assertions.assertFalse(CommonUtil.equals('a', 'b'));

        Assertions.assertTrue(CommonUtil.equals((byte) 10, (byte) 10));
        Assertions.assertFalse(CommonUtil.equals((byte) 10, (byte) 20));

        Assertions.assertTrue(CommonUtil.equals((short) 10, (short) 10));
        Assertions.assertFalse(CommonUtil.equals((short) 10, (short) 20));

        Assertions.assertTrue(CommonUtil.equals(10, 10));
        Assertions.assertFalse(CommonUtil.equals(10, 20));

        Assertions.assertTrue(CommonUtil.equals(10L, 10L));
        Assertions.assertFalse(CommonUtil.equals(10L, 20L));

        Assertions.assertTrue(CommonUtil.equals(10.5f, 10.5f));
        Assertions.assertFalse(CommonUtil.equals(10.5f, 20.5f));
        Assertions.assertTrue(CommonUtil.equals(Float.NaN, Float.NaN));

        Assertions.assertTrue(CommonUtil.equals(10.5, 10.5));
        Assertions.assertFalse(CommonUtil.equals(10.5, 20.5));
        Assertions.assertTrue(CommonUtil.equals(Double.NaN, Double.NaN));
    }

    @Test
    public void testEqualsStrings() {
        Assertions.assertTrue(CommonUtil.equals("test", "test"));
        Assertions.assertFalse(CommonUtil.equals("test", "Test"));
        Assertions.assertTrue(CommonUtil.equals((String) null, null));
        Assertions.assertFalse(CommonUtil.equals("test", null));
        Assertions.assertFalse(CommonUtil.equals(null, "test"));

        Assertions.assertTrue(CommonUtil.equalsIgnoreCase("test", "TEST"));
        Assertions.assertTrue(CommonUtil.equalsIgnoreCase("Test", "test"));
        Assertions.assertTrue(CommonUtil.equalsIgnoreCase((String) null, null));
        Assertions.assertFalse(CommonUtil.equalsIgnoreCase("test", null));
        Assertions.assertFalse(CommonUtil.equalsIgnoreCase(null, "test"));
    }

    @Test
    public void testEqualsObjects() {
        Object a = new Object();
        Object b = new Object();

        Assertions.assertTrue(CommonUtil.equals(a, a));
        Assertions.assertFalse(CommonUtil.equals(a, b));
        Assertions.assertTrue(CommonUtil.equals((Object) null, null));
        Assertions.assertFalse(CommonUtil.equals(a, null));
        Assertions.assertFalse(CommonUtil.equals(null, b));

        int[] arr1 = { 1, 2, 3 };
        int[] arr2 = { 1, 2, 3 };
        int[] arr3 = { 1, 2, 4 };

        Assertions.assertTrue(CommonUtil.equals(arr1, arr1));
        Assertions.assertTrue(CommonUtil.equals(arr1, arr2));
        Assertions.assertFalse(CommonUtil.equals(arr1, arr3));

        Assertions.assertFalse(CommonUtil.equals(new int[] { 1 }, new long[] { 1 }));
    }

    @Test
    public void testEqualsArrays() {
        Assertions.assertTrue(CommonUtil.equals(new boolean[] { true, false }, new boolean[] { true, false }));
        Assertions.assertFalse(CommonUtil.equals(new boolean[] { true, false }, new boolean[] { false, true }));
        Assertions.assertTrue(CommonUtil.equals((boolean[]) null, null));
        Assertions.assertFalse(CommonUtil.equals(new boolean[] { true }, null));

        boolean[] boolArr1 = { true, false, true, false };
        boolean[] boolArr2 = { false, true, false, true };
        Assertions.assertTrue(CommonUtil.equals(boolArr1, 1, boolArr2, 2, 2));
        Assertions.assertFalse(CommonUtil.equals(boolArr1, 0, boolArr2, 0, 2));

        Assertions.assertTrue(CommonUtil.equals(new char[] { 'a', 'b' }, new char[] { 'a', 'b' }));
        Assertions.assertFalse(CommonUtil.equals(new char[] { 'a', 'b' }, new char[] { 'b', 'a' }));

        char[] charArr1 = { 'a', 'b', 'c', 'd' };
        char[] charArr2 = { 'x', 'b', 'c', 'y' };
        Assertions.assertTrue(CommonUtil.equals(charArr1, 1, charArr2, 1, 2));

        Assertions.assertTrue(CommonUtil.equals(new byte[] { 1, 2 }, new byte[] { 1, 2 }));
        Assertions.assertFalse(CommonUtil.equals(new byte[] { 1, 2 }, new byte[] { 2, 1 }));

        byte[] byteArr1 = { 1, 2, 3, 4 };
        byte[] byteArr2 = { 5, 2, 3, 6 };
        Assertions.assertTrue(CommonUtil.equals(byteArr1, 1, byteArr2, 1, 2));

        Assertions.assertTrue(CommonUtil.equals(new short[] { 1, 2 }, new short[] { 1, 2 }));
        Assertions.assertFalse(CommonUtil.equals(new short[] { 1, 2 }, new short[] { 2, 1 }));

        short[] shortArr1 = { 1, 2, 3, 4 };
        short[] shortArr2 = { 5, 2, 3, 6 };
        Assertions.assertTrue(CommonUtil.equals(shortArr1, 1, shortArr2, 1, 2));

        Assertions.assertTrue(CommonUtil.equals(new int[] { 1, 2 }, new int[] { 1, 2 }));
        Assertions.assertFalse(CommonUtil.equals(new int[] { 1, 2 }, new int[] { 2, 1 }));

        int[] intArr1 = { 1, 2, 3, 4 };
        int[] intArr2 = { 5, 2, 3, 6 };
        Assertions.assertTrue(CommonUtil.equals(intArr1, 1, intArr2, 1, 2));

        Assertions.assertTrue(CommonUtil.equals(new long[] { 1L, 2L }, new long[] { 1L, 2L }));
        Assertions.assertFalse(CommonUtil.equals(new long[] { 1L, 2L }, new long[] { 2L, 1L }));

        long[] longArr1 = { 1L, 2L, 3L, 4L };
        long[] longArr2 = { 5L, 2L, 3L, 6L };
        Assertions.assertTrue(CommonUtil.equals(longArr1, 1, longArr2, 1, 2));
    }
}
