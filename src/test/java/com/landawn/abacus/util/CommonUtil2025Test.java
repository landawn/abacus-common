package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CommonUtil2025Test extends TestBase {

    @Test
    public void testCheckFromToIndex_ValidRange() {
        assertDoesNotThrow(() -> N.checkFromToIndex(0, 5, 10));
        assertDoesNotThrow(() -> N.checkFromToIndex(0, 0, 0));
        assertDoesNotThrow(() -> N.checkFromToIndex(5, 5, 10));
    }

    @Test
    public void testCheckFromToIndex_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(-1, 5, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(5, 4, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(0, 11, 10));
    }

    @Test
    public void testCheckFromIndexSize_ValidRange() {
        assertDoesNotThrow(() -> N.checkFromIndexSize(0, 5, 10));
        assertDoesNotThrow(() -> N.checkFromIndexSize(0, 0, 0));
        assertDoesNotThrow(() -> N.checkFromIndexSize(5, 5, 10));
    }

    @Test
    public void testCheckFromIndexSize_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(-1, 5, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(0, -1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(6, 5, 10));
    }

    @Test
    public void testCheckElementIndex_ValidIndex() {
        assertEquals(0, N.checkElementIndex(0, 5));
        assertEquals(4, N.checkElementIndex(4, 5));
    }

    @Test
    public void testCheckElementIndex_InvalidIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(-1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(5, 5));
        assertThrows(IllegalArgumentException.class, () -> N.checkElementIndex(0, -1));
    }

    @Test
    public void testCheckElementIndex_WithDescription() {
        assertEquals(0, N.checkElementIndex(0, 5, "index"));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(-1, 5, "myIndex"));
    }

    @Test
    public void testCheckPositionIndex_ValidIndex() {
        assertEquals(0, N.checkPositionIndex(0, 5));
        assertEquals(5, N.checkPositionIndex(5, 5));
    }

    @Test
    public void testCheckPositionIndex_InvalidIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkPositionIndex(-1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkPositionIndex(6, 5));
        assertThrows(IllegalArgumentException.class, () -> N.checkPositionIndex(0, -1));
    }

    @Test
    public void testCheckPositionIndex_WithDescription() {
        assertEquals(0, N.checkPositionIndex(0, 5, "position"));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkPositionIndex(6, 5, "myPosition"));
    }

    @Test
    public void testCheckArgNotNull_ValidArgument() {
        String result = N.checkArgNotNull("test");
        assertEquals("test", result);
    }

    @Test
    public void testCheckArgNotNull_NullArgument() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null, "arg"));
    }

    @Test
    public void testCheckArgNotNull_WithErrorMessage() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null, "Custom error message"));
        try {
            N.checkArgNotNull(null, "arg");
            fail("Should throw exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("cannot be null"));
        }
    }

    @Test
    public void testCheckArgNotEmpty_CharSequence() {
        assertEquals("test", N.checkArgNotEmpty("test", "str"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty("", "str"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((String) null, "str"));
    }

    @Test
    public void testCheckArgNotEmpty_BooleanArray() {
        boolean[] arr = { true, false };
        assertSame(arr, N.checkArgNotEmpty(arr, "arr"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new boolean[0], "arr"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((boolean[]) null, "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_CharArray() {
        char[] arr = { 'a', 'b' };
        assertSame(arr, N.checkArgNotEmpty(arr, "arr"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new char[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ByteArray() {
        byte[] arr = { 1, 2 };
        assertSame(arr, N.checkArgNotEmpty(arr, "arr"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new byte[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ShortArray() {
        short[] arr = { 1, 2 };
        assertSame(arr, N.checkArgNotEmpty(arr, "arr"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new short[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_IntArray() {
        int[] arr = { 1, 2 };
        assertSame(arr, N.checkArgNotEmpty(arr, "arr"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new int[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_LongArray() {
        long[] arr = { 1L, 2L };
        assertSame(arr, N.checkArgNotEmpty(arr, "arr"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new long[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_FloatArray() {
        float[] arr = { 1.0f, 2.0f };
        assertSame(arr, N.checkArgNotEmpty(arr, "arr"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new float[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_DoubleArray() {
        double[] arr = { 1.0, 2.0 };
        assertSame(arr, N.checkArgNotEmpty(arr, "arr"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new double[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ObjectArray() {
        String[] arr = { "a", "b" };
        assertSame(arr, N.checkArgNotEmpty(arr, "arr"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new String[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_Collection() {
        List<String> list = Arrays.asList("a", "b");
        assertSame(list, N.checkArgNotEmpty(list, "list"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new ArrayList<>(), "list"));
    }

    @Test
    public void testCheckArgNotEmpty_Map() {
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertSame(map, N.checkArgNotEmpty(map, "map"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new HashMap<>(), "map"));
    }

    @Test
    public void testCheckArgNotBlank_ValidString() {
        assertEquals("test", N.checkArgNotBlank("test", "str"));
        assertEquals(" test ", N.checkArgNotBlank(" test ", "str"));
    }

    @Test
    public void testCheckArgNotBlank_BlankString() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank("", "str"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank("   ", "str"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank((String) null, "str"));
    }

    @Test
    public void testCheckArgNotNegative_Byte() {
        assertEquals(5, N.checkArgNotNegative((byte) 5, "value"));
        assertEquals(0, N.checkArgNotNegative((byte) 0, "value"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative((byte) -1, "value"));
    }

    @Test
    public void testCheckArgNotNegative_Short() {
        assertEquals(5, N.checkArgNotNegative((short) 5, "value"));
        assertEquals(0, N.checkArgNotNegative((short) 0, "value"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative((short) -1, "value"));
    }

    @Test
    public void testCheckArgNotNegative_Int() {
        assertEquals(5, N.checkArgNotNegative(5, "value"));
        assertEquals(0, N.checkArgNotNegative(0, "value"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-1, "value"));
    }

    @Test
    public void testCheckArgNotNegative_Long() {
        assertEquals(5L, N.checkArgNotNegative(5L, "value"));
        assertEquals(0L, N.checkArgNotNegative(0L, "value"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-1L, "value"));
    }

    @Test
    public void testCheckArgNotNegative_Float() {
        assertEquals(5.0f, N.checkArgNotNegative(5.0f, "value"));
        assertEquals(0.0f, N.checkArgNotNegative(0.0f, "value"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-1.0f, "value"));
    }

    @Test
    public void testCheckArgNotNegative_Double() {
        assertEquals(5.0, N.checkArgNotNegative(5.0, "value"));
        assertEquals(0.0, N.checkArgNotNegative(0.0, "value"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-1.0, "value"));
    }

    @Test
    public void testCheckArgPositive_Byte() {
        assertEquals(5, N.checkArgPositive((byte) 5, "value"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((byte) 0, "value"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((byte) -1, "value"));
    }

    @Test
    public void testCheckArgPositive_Short() {
        assertEquals(5, N.checkArgPositive((short) 5, "value"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((short) 0, "value"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((short) -1, "value"));
    }

    @Test
    public void testCheckArgPositive_Int() {
        assertEquals(5, N.checkArgPositive(5, "value"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0, "value"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-1, "value"));
    }

    @Test
    public void testCheckArgPositive_Long() {
        assertEquals(5L, N.checkArgPositive(5L, "value"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0L, "value"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-1L, "value"));
    }

    @Test
    public void testCheckArgPositive_Float() {
        assertEquals(5.0f, N.checkArgPositive(5.0f, "value"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0.0f, "value"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-1.0f, "value"));
    }

    @Test
    public void testCheckArgPositive_Double() {
        assertEquals(5.0, N.checkArgPositive(5.0, "value"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0.0, "value"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-1.0, "value"));
    }

    @Test
    public void testCheckElementNotNull_Array() {
        Object[] arr = { "a", "b", "c" };
        assertDoesNotThrow(() -> N.checkElementNotNull(arr));
        assertDoesNotThrow(() -> N.checkElementNotNull(new Object[0]));

        Object[] arrWithNull = { "a", null, "c" };
        assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(arrWithNull));
    }

    @Test
    public void testCheckElementNotNull_Array_WithMessage() {
        Object[] arrWithNull = { "a", null, "c" };
        assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(arrWithNull, "myArray"));
    }

    @Test
    public void testCheckElementNotNull_Collection() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertDoesNotThrow(() -> N.checkElementNotNull(list));
        assertDoesNotThrow(() -> N.checkElementNotNull(new ArrayList<>()));

        List<String> listWithNull = Arrays.asList("a", null, "c");
        assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(listWithNull));
    }

    @Test
    public void testCheckElementNotNull_Collection_WithMessage() {
        List<String> listWithNull = Arrays.asList("a", null, "c");
        assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(listWithNull, "myList"));
    }

    @Test
    public void testCheckKeyNotNull_Map() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        assertDoesNotThrow(() -> N.checkKeyNotNull(map));

        Map<String, String> mapWithNullKey = new HashMap<>();
        mapWithNullKey.put(null, "value");
        assertThrows(IllegalArgumentException.class, () -> N.checkKeyNotNull(mapWithNullKey));
    }

    @Test
    public void testCheckKeyNotNull_Map_WithMessage() {
        Map<String, String> mapWithNullKey = new HashMap<>();
        mapWithNullKey.put(null, "value");
        assertThrows(IllegalArgumentException.class, () -> N.checkKeyNotNull(mapWithNullKey, "myMap"));
    }

    @Test
    public void testCheckValueNotNull_Map() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        assertDoesNotThrow(() -> N.checkValueNotNull(map));

        Map<String, String> mapWithNullValue = new HashMap<>();
        mapWithNullValue.put("key", null);
        assertThrows(IllegalArgumentException.class, () -> N.checkValueNotNull(mapWithNullValue));
    }

    @Test
    public void testCheckValueNotNull_Map_WithMessage() {
        Map<String, String> mapWithNullValue = new HashMap<>();
        mapWithNullValue.put("key", null);
        assertThrows(IllegalArgumentException.class, () -> N.checkValueNotNull(mapWithNullValue, "myMap"));
    }

    @Test
    public void testCheckArgument_Simple() {
        assertDoesNotThrow(() -> N.checkArgument(true));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false));
    }

    @Test
    public void testCheckArgument_WithMessage() {
        assertDoesNotThrow(() -> N.checkArgument(true, "Error message"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Error message"));
    }

    @Test
    public void testCheckArgument_WithTemplate() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Value {} is invalid", 5));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Value %s is invalid", 5));
    }

    @Test
    public void testCheckArgument_WithPrimitiveParams() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "char: {}", 'a'));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "int: {}", 10));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "long: {}", 100L));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "double: {}", 3.14));
    }

    @Test
    public void testCheckArgument_WithMultipleParams() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Value {} must be less than {}", 10, 5));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Value {} {} {}", "a", "b", "c"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Value {} {} {} {}", "a", "b", "c", "d"));
    }

    @Test
    public void testCheckArgument_WithSupplier() {
        Supplier<String> supplier = () -> "Lazy error message";
        assertDoesNotThrow(() -> N.checkArgument(true, supplier));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, supplier));
    }

    @Test
    public void testCheckState_Simple() {
        assertDoesNotThrow(() -> N.checkState(true));
        assertThrows(IllegalStateException.class, () -> N.checkState(false));
    }

    @Test
    public void testCheckState_WithMessage() {
        assertDoesNotThrow(() -> N.checkState(true, "Error message"));
        assertThrows(IllegalStateException.class, () -> N.checkState(false, "Error message"));
    }

    @Test
    public void testCheckState_WithTemplate() {
        assertThrows(IllegalStateException.class, () -> N.checkState(false, "State {} is invalid", "ACTIVE"));
        assertThrows(IllegalStateException.class, () -> N.checkState(false, "State %s is invalid", "ACTIVE"));
    }

    @Test
    public void testCheckState_WithPrimitiveParams() {
        assertThrows(IllegalStateException.class, () -> N.checkState(false, "char: {}", 'a'));
        assertThrows(IllegalStateException.class, () -> N.checkState(false, "int: {}", 10));
        assertThrows(IllegalStateException.class, () -> N.checkState(false, "long: {}", 100L));
        assertThrows(IllegalStateException.class, () -> N.checkState(false, "double: {}", 3.14));
    }

    @Test
    public void testCheckState_WithMultipleParams() {
        assertThrows(IllegalStateException.class, () -> N.checkState(false, "Value {} must be less than {}", 10, 5));
        assertThrows(IllegalStateException.class, () -> N.checkState(false, "Value {} {} {}", "a", "b", "c"));
        assertThrows(IllegalStateException.class, () -> N.checkState(false, "Value {} {} {} {}", "a", "b", "c", "d"));
    }

    @Test
    public void testCheckState_WithSupplier() {
        Supplier<String> supplier = () -> "Lazy error message";
        assertDoesNotThrow(() -> N.checkState(true, supplier));
        assertThrows(IllegalStateException.class, () -> N.checkState(false, supplier));
    }

    @Test
    public void testRequireNonNull_Simple() {
        String result = N.requireNonNull("test");
        assertEquals("test", result);
        assertThrows(NullPointerException.class, () -> N.requireNonNull(null));
    }

    @Test
    public void testRequireNonNull_WithMessage() {
        assertEquals("test", N.requireNonNull("test", "value"));
        assertThrows(NullPointerException.class, () -> N.requireNonNull(null, "value"));
    }

    @Test
    public void testRequireNonNull_WithSupplier() {
        Supplier<String> supplier = () -> "Error message";
        assertEquals("test", N.requireNonNull("test", supplier));
        assertThrows(NullPointerException.class, () -> N.requireNonNull(null, supplier));
    }

    @Test
    public void testEquals_Boolean() {
        assertTrue(N.equals(true, true));
        assertTrue(N.equals(false, false));
        assertFalse(N.equals(true, false));
        assertFalse(N.equals(false, true));
    }

    @Test
    public void testEquals_Char() {
        assertTrue(N.equals('a', 'a'));
        assertFalse(N.equals('a', 'b'));
    }

    @Test
    public void testEquals_Byte() {
        assertTrue(N.equals((byte) 1, (byte) 1));
        assertFalse(N.equals((byte) 1, (byte) 2));
    }

    @Test
    public void testEquals_Short() {
        assertTrue(N.equals((short) 1, (short) 1));
        assertFalse(N.equals((short) 1, (short) 2));
    }

    @Test
    public void testEquals_Int() {
        assertTrue(N.equals(1, 1));
        assertFalse(N.equals(1, 2));
    }

    @Test
    public void testEquals_Long() {
        assertTrue(N.equals(1L, 1L));
        assertFalse(N.equals(1L, 2L));
    }

    @Test
    public void testEquals_Float() {
        assertTrue(N.equals(1.0f, 1.0f));
        assertFalse(N.equals(1.0f, 2.0f));
        assertTrue(N.equals(Float.NaN, Float.NaN));
        assertTrue(N.equals(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
    }

    @Test
    public void testEquals_Double() {
        assertTrue(N.equals(1.0, 1.0));
        assertFalse(N.equals(1.0, 2.0));
        assertTrue(N.equals(Double.NaN, Double.NaN));
        assertTrue(N.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
    }

    @Test
    public void testEquals_String() {
        assertTrue(N.equals("test", "test"));
        assertFalse(N.equals("test", "Test"));
        assertTrue(N.equals((String) null, (String) null));
        assertFalse(N.equals("test", null));
        assertFalse(N.equals(null, "test"));
    }

    @Test
    public void testEqualsIgnoreCase_String() {
        assertTrue(N.equalsIgnoreCase("test", "test"));
        assertTrue(N.equalsIgnoreCase("test", "Test"));
        assertTrue(N.equalsIgnoreCase("test", "TEST"));
        assertTrue(N.equalsIgnoreCase((String) null, (String) null));
        assertFalse(N.equalsIgnoreCase("test", null));
        assertFalse(N.equalsIgnoreCase(null, "test"));
    }

    @Test
    public void testEquals_Object() {
        assertTrue(N.equals(new Integer(5), new Integer(5)));
        assertFalse(N.equals(new Integer(5), new Integer(6)));
        assertTrue(N.equals((Object) null, (Object) null));
        assertFalse(N.equals(new Integer(5), null));
    }

    @Test
    public void testEquals_BooleanArray() {
        assertTrue(N.equals(new boolean[] { true, false }, new boolean[] { true, false }));
        assertFalse(N.equals(new boolean[] { true, false }, new boolean[] { false, true }));
        assertTrue(N.equals((boolean[]) null, (boolean[]) null));
        assertFalse(N.equals(new boolean[] { true }, null));
    }

    @Test
    public void testEquals_BooleanArray_WithRange() {
        boolean[] a = { true, false, true, false };
        boolean[] b = { false, false, true, true };

        assertTrue(N.equals(a, 1, b, 1, 2));
        assertFalse(N.equals(a, 0, b, 0, 2));
        assertTrue(N.equals(a, 0, a, 0, 0));
    }

    @Test
    public void testEquals_CharArray() {
        assertTrue(N.equals(new char[] { 'a', 'b' }, new char[] { 'a', 'b' }));
        assertFalse(N.equals(new char[] { 'a', 'b' }, new char[] { 'b', 'a' }));
        assertTrue(N.equals((char[]) null, (char[]) null));
    }

    @Test
    public void testEquals_CharArray_WithRange() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] b = { 'x', 'b', 'c', 'y' };

        assertTrue(N.equals(a, 1, b, 1, 2));
        assertFalse(N.equals(a, 0, b, 0, 2));
    }

    @Test
    public void testEquals_ByteArray() {
        assertTrue(N.equals(new byte[] { 1, 2 }, new byte[] { 1, 2 }));
        assertFalse(N.equals(new byte[] { 1, 2 }, new byte[] { 2, 1 }));
        assertTrue(N.equals((byte[]) null, (byte[]) null));
    }

    @Test
    public void testEquals_ByteArray_WithRange() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] b = { 0, 2, 3, 0 };

        assertTrue(N.equals(a, 1, b, 1, 2));
        assertFalse(N.equals(a, 0, b, 0, 2));
    }

    @Test
    public void testEquals_ShortArray() {
        assertTrue(N.equals(new short[] { 1, 2 }, new short[] { 1, 2 }));
        assertFalse(N.equals(new short[] { 1, 2 }, new short[] { 2, 1 }));
        assertTrue(N.equals((short[]) null, (short[]) null));
    }

    @Test
    public void testEquals_ShortArray_WithRange() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 0, 2, 3, 0 };

        assertTrue(N.equals(a, 1, b, 1, 2));
        assertFalse(N.equals(a, 0, b, 0, 2));
    }

    @Test
    public void testEquals_IntArray() {
        assertTrue(N.equals(new int[] { 1, 2 }, new int[] { 1, 2 }));
        assertFalse(N.equals(new int[] { 1, 2 }, new int[] { 2, 1 }));
        assertTrue(N.equals((int[]) null, (int[]) null));
    }

    @Test
    public void testEquals_IntArray_WithRange() {
        int[] a = { 1, 2, 3, 4 };
        int[] b = { 0, 2, 3, 0 };

        assertTrue(N.equals(a, 1, b, 1, 2));
        assertFalse(N.equals(a, 0, b, 0, 2));
    }

    @Test
    public void testEquals_LongArray() {
        assertTrue(N.equals(new long[] { 1L, 2L }, new long[] { 1L, 2L }));
        assertFalse(N.equals(new long[] { 1L, 2L }, new long[] { 2L, 1L }));
        assertTrue(N.equals((long[]) null, (long[]) null));
    }

    @Test
    public void testEquals_LongArray_WithRange() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 0L, 2L, 3L, 0L };

        assertTrue(N.equals(a, 1, b, 1, 2));
        assertFalse(N.equals(a, 0, b, 0, 2));
    }

    @Test
    public void testEquals_FloatArray() {
        assertTrue(N.equals(new float[] { 1.0f, 2.0f }, new float[] { 1.0f, 2.0f }));
        assertFalse(N.equals(new float[] { 1.0f, 2.0f }, new float[] { 2.0f, 1.0f }));
        assertTrue(N.equals((float[]) null, (float[]) null));
    }

    @Test
    public void testEquals_FloatArray_WithRange() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] b = { 0.0f, 2.0f, 3.0f, 0.0f };

        assertTrue(N.equals(a, 1, b, 1, 2));
        assertFalse(N.equals(a, 0, b, 0, 2));
    }

    @Test
    public void testEquals_FloatArray_WithDelta() {
        assertTrue(N.equals(new float[] { 1.0f, 2.0f }, new float[] { 1.01f, 2.01f }, 0.02f));
        assertFalse(N.equals(new float[] { 1.0f, 2.0f }, new float[] { 1.1f, 2.1f }, 0.05f));
    }

    @Test
    public void testEquals_DoubleArray() {
        assertTrue(N.equals(new double[] { 1.0, 2.0 }, new double[] { 1.0, 2.0 }));
        assertFalse(N.equals(new double[] { 1.0, 2.0 }, new double[] { 2.0, 1.0 }));
        assertTrue(N.equals((double[]) null, (double[]) null));
    }

    @Test
    public void testEquals_DoubleArray_WithRange() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] b = { 0.0, 2.0, 3.0, 0.0 };

        assertTrue(N.equals(a, 1, b, 1, 2));
        assertFalse(N.equals(a, 0, b, 0, 2));
    }

    @Test
    public void testEquals_DoubleArray_WithDelta() {
        assertTrue(N.equals(new double[] { 1.0, 2.0 }, new double[] { 1.01, 2.01 }, 0.02));
        assertFalse(N.equals(new double[] { 1.0, 2.0 }, new double[] { 1.1, 2.1 }, 0.05));
    }

    @Test
    public void testEquals_ObjectArray() {
        assertTrue(N.equals(new String[] { "a", "b" }, new String[] { "a", "b" }));
        assertFalse(N.equals(new String[] { "a", "b" }, new String[] { "b", "a" }));
        assertTrue(N.equals((Object[]) null, (Object[]) null));
    }

    @Test
    public void testEquals_ObjectArray_WithRange() {
        String[] a = { "a", "b", "c", "d" };
        String[] b = { "x", "b", "c", "y" };

        assertTrue(N.equals(a, 1, b, 1, 2));
        assertFalse(N.equals(a, 0, b, 0, 2));
    }

    @Test
    public void testDeepEquals_Object() {
        assertTrue(N.deepEquals("test", "test"));
        assertTrue(N.deepEquals(null, null));
        assertFalse(N.deepEquals("test", "other"));
    }

    @Test
    public void testDeepEquals_Arrays() {
        assertTrue(N.deepEquals(new int[] { 1, 2 }, new int[] { 1, 2 }));
        assertTrue(N.deepEquals(new String[] { "a", "b" }, new String[] { "a", "b" }));
        assertFalse(N.deepEquals(new int[] { 1, 2 }, new int[] { 2, 1 }));
    }

    @Test
    public void testDeepEquals_NestedArrays() {
        Object[] a = { new int[] { 1, 2 }, new String[] { "a", "b" } };
        Object[] b = { new int[] { 1, 2 }, new String[] { "a", "b" } };
        Object[] c = { new int[] { 1, 2 }, new String[] { "a", "c" } };

        assertTrue(N.deepEquals(a, b));
        assertFalse(N.deepEquals(a, c));
    }

    @Test
    public void testDeepEquals_ObjectArray_WithRange() {
        String[] a = { "a", "b", "c", "d" };
        String[] b = { "x", "b", "c", "y" };

        assertTrue(N.deepEquals(a, 1, b, 1, 2));
        assertFalse(N.deepEquals(a, 0, b, 0, 2));
    }

    @Test
    public void testEqualsIgnoreCase_StringArray() {
        assertTrue(N.equalsIgnoreCase(new String[] { "a", "b" }, new String[] { "A", "B" }));
        assertTrue(N.equalsIgnoreCase(new String[] { "Test" }, new String[] { "test" }));
        assertFalse(N.equalsIgnoreCase(new String[] { "a", "b" }, new String[] { "a", "c" }));
        assertTrue(N.equalsIgnoreCase((String[]) null, (String[]) null));
    }

    @Test
    public void testEqualsIgnoreCase_StringArray_WithRange() {
        String[] a = { "a", "b", "c", "d" };
        String[] b = { "X", "B", "C", "Y" };

        assertTrue(N.equalsIgnoreCase(a, 1, b, 1, 2));
        assertFalse(N.equalsIgnoreCase(a, 0, b, 0, 2));
    }

    @Test
    public void testEqualsByKeys_Map() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 1);
        map2.put("b", 2);
        map2.put("c", 4);

        Collection<String> keys = Arrays.asList("a", "b");
        assertTrue(N.equalsByKeys(map1, map2, keys));

        Collection<String> allKeys = Arrays.asList("a", "b", "c");
        assertFalse(N.equalsByKeys(map1, map2, allKeys));
    }

    @Test
    public void testEqualsByKeys_NullMaps() {
        assertTrue(N.equalsByKeys(null, null, Arrays.asList("a")));
        assertFalse(N.equalsByKeys(new HashMap<>(), null, Arrays.asList("a")));
        assertFalse(N.equalsByKeys(null, new HashMap<>(), Arrays.asList("a")));
    }

    @Test
    public void testHashCode_Boolean() {
        assertEquals(1231, N.hashCode(true));
        assertEquals(1237, N.hashCode(false));
    }

    @Test
    public void testHashCode_Char() {
        assertEquals((int) 'a', N.hashCode('a'));
        assertEquals((int) 'Z', N.hashCode('Z'));
    }

    @Test
    public void testHashCode_Byte() {
        assertEquals(5, N.hashCode((byte) 5));
        assertEquals(-1, N.hashCode((byte) -1));
    }

    @Test
    public void testHashCode_Short() {
        assertEquals(100, N.hashCode((short) 100));
        assertEquals(-50, N.hashCode((short) -50));
    }

    @Test
    public void testHashCode_Int() {
        assertEquals(42, N.hashCode(42));
        assertEquals(-42, N.hashCode(-42));
    }

    @Test
    public void testHashCode_Long() {
        assertEquals(Long.hashCode(100L), N.hashCode(100L));
        assertEquals(Long.hashCode(-100L), N.hashCode(-100L));
    }

    @Test
    public void testHashCode_Float() {
        assertEquals(Float.floatToIntBits(3.14f), N.hashCode(3.14f));
    }

    @Test
    public void testHashCode_Double() {
        assertEquals(Double.hashCode(3.14), N.hashCode(3.14));
    }

    @Test
    public void testHashCode_Object() {
        String str = "test";
        assertEquals(str.hashCode(), N.hashCode(str));
        assertEquals(0, N.hashCode((Object) null));
    }

    @Test
    public void testHashCode_BooleanArray() {
        boolean[] arr = { true, false, true };
        int hash1 = N.hashCode(arr);
        int hash2 = N.hashCode(arr);
        assertEquals(hash1, hash2);

        assertEquals(0, N.hashCode((boolean[]) null));
    }

    @Test
    public void testHashCode_BooleanArray_WithRange() {
        boolean[] arr = { true, false, true, false };
        int hash1 = N.hashCode(arr, 1, 3);
        int hash2 = N.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_CharArray() {
        char[] arr = { 'a', 'b', 'c' };
        int hash1 = N.hashCode(arr);
        int hash2 = N.hashCode(arr);
        assertEquals(hash1, hash2);

        assertEquals(0, N.hashCode((char[]) null));
    }

    @Test
    public void testHashCode_CharArray_WithRange() {
        char[] arr = { 'a', 'b', 'c', 'd' };
        int hash1 = N.hashCode(arr, 1, 3);
        int hash2 = N.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_ByteArray() {
        byte[] arr = { 1, 2, 3 };
        int hash1 = N.hashCode(arr);
        int hash2 = N.hashCode(arr);
        assertEquals(hash1, hash2);

        assertEquals(0, N.hashCode((byte[]) null));
    }

    @Test
    public void testHashCode_ByteArray_WithRange() {
        byte[] arr = { 1, 2, 3, 4 };
        int hash1 = N.hashCode(arr, 1, 3);
        int hash2 = N.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_ShortArray() {
        short[] arr = { 1, 2, 3 };
        int hash1 = N.hashCode(arr);
        int hash2 = N.hashCode(arr);
        assertEquals(hash1, hash2);

        assertEquals(0, N.hashCode((short[]) null));
    }

    @Test
    public void testHashCode_ShortArray_WithRange() {
        short[] arr = { 1, 2, 3, 4 };
        int hash1 = N.hashCode(arr, 1, 3);
        int hash2 = N.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_IntArray() {
        int[] arr = { 1, 2, 3 };
        int hash1 = N.hashCode(arr);
        int hash2 = N.hashCode(arr);
        assertEquals(hash1, hash2);

        assertEquals(0, N.hashCode((int[]) null));
    }

    @Test
    public void testHashCode_IntArray_WithRange() {
        int[] arr = { 1, 2, 3, 4 };
        int hash1 = N.hashCode(arr, 1, 3);
        int hash2 = N.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_LongArray() {
        long[] arr = { 1L, 2L, 3L };
        int hash1 = N.hashCode(arr);
        int hash2 = N.hashCode(arr);
        assertEquals(hash1, hash2);

        assertEquals(0, N.hashCode((long[]) null));
    }

    @Test
    public void testHashCode_LongArray_WithRange() {
        long[] arr = { 1L, 2L, 3L, 4L };
        int hash1 = N.hashCode(arr, 1, 3);
        int hash2 = N.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_FloatArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f };
        int hash1 = N.hashCode(arr);
        int hash2 = N.hashCode(arr);
        assertEquals(hash1, hash2);

        assertEquals(0, N.hashCode((float[]) null));
    }

    @Test
    public void testHashCode_FloatArray_WithRange() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f };
        int hash1 = N.hashCode(arr, 1, 3);
        int hash2 = N.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_DoubleArray() {
        double[] arr = { 1.0, 2.0, 3.0 };
        int hash1 = N.hashCode(arr);
        int hash2 = N.hashCode(arr);
        assertEquals(hash1, hash2);

        assertEquals(0, N.hashCode((double[]) null));
    }

    @Test
    public void testHashCode_DoubleArray_WithRange() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0 };
        int hash1 = N.hashCode(arr, 1, 3);
        int hash2 = N.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_Consistency() {
        assertTrue(N.equals(new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 }));
        assertEquals(N.hashCode(new int[] { 1, 2, 3 }), N.hashCode(new int[] { 1, 2, 3 }));
    }

    @Test
    public void testCheckIndex_Valid() {
        assertEquals(0, N.checkIndex(0, 5));
        assertEquals(4, N.checkIndex(4, 5));
    }

    @Test
    public void testCheckIndex_Invalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkIndex(-1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkIndex(5, 5));
    }

    @Test
    public void testDeepHashCode_Object() {
        String str = "test";
        assertEquals(str.hashCode(), N.deepHashCode(str));
        assertEquals(0, N.deepHashCode((Object) null));
    }

    @Test
    public void testDeepHashCode_ObjectArray() {
        Object[] arr = { new int[] { 1, 2 }, "test", 42 };
        int hash1 = N.deepHashCode(arr);
        int hash2 = N.deepHashCode(arr);
        assertEquals(hash1, hash2);
        assertEquals(0, N.deepHashCode((Object[]) null));
    }

    @Test
    public void testDeepHashCode_ObjectArray_WithRange() {
        Object[] arr = { "a", "b", "c", "d" };
        int hash1 = N.deepHashCode(arr, 1, 3);
        int hash2 = N.deepHashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCodeEverything() {
        String str = "test";
        long hash = N.hashCodeEverything(str);
        assertTrue(hash != 0);
        assertEquals(0, N.hashCodeEverything(null));
    }

    @Test
    public void testHashCode_ObjectArray() {
        String[] arr = { "a", "b", "c" };
        int hash1 = N.hashCode((Object[]) arr);
        int hash2 = N.hashCode((Object[]) arr);
        assertEquals(hash1, hash2);
        assertEquals(0, N.hashCode((Object[]) null));
    }

    @Test
    public void testHashCode_ObjectArray_WithRange() {
        String[] arr = { "a", "b", "c", "d" };
        int hash1 = N.hashCode((Object[]) arr, 1, 3);
        int hash2 = N.hashCode((Object[]) arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testCompare_Boolean() {
        assertEquals(0, N.compare(true, true));
        assertEquals(0, N.compare(false, false));
        assertTrue(N.compare(false, true) < 0);
        assertTrue(N.compare(true, false) > 0);
    }

    @Test
    public void testCompare_BooleanArray() {
        boolean[] a = { true, false, true };
        boolean[] b = { true, false, true };
        boolean[] c = { true, false, false };

        assertEquals(0, N.compare(a, b));
        assertTrue(N.compare(c, a) < 0);
        assertTrue(N.compare(a, c) > 0);
    }

    @Test
    public void testCompare_BooleanArray_WithRange() {
        boolean[] a = { true, false, true, false };
        boolean[] b = { false, false, true, true };

        assertEquals(0, N.compare(a, 1, b, 1, 2));
        assertTrue(N.compare(a, 0, b, 0, 2) > 0);
    }

    @Test
    public void testCompare_Byte() {
        assertEquals(0, N.compare((byte) 5, (byte) 5));
        assertTrue(N.compare((byte) 3, (byte) 5) < 0);
        assertTrue(N.compare((byte) 7, (byte) 5) > 0);
    }

    @Test
    public void testCompare_ByteArray() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 1, 2, 3 };
        byte[] c = { 1, 2, 4 };

        assertEquals(0, N.compare(a, b));
        assertTrue(N.compare(a, c) < 0);
        assertTrue(N.compare(c, a) > 0);
    }

    @Test
    public void testCompare_ByteArray_WithRange() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] b = { 0, 2, 3, 0 };

        assertEquals(0, N.compare(a, 1, b, 1, 2));
        assertTrue(N.compare(a, 0, b, 0, 2) > 0);
    }

    @Test
    public void testCompare_Char() {
        assertEquals(0, N.compare('a', 'a'));
        assertTrue(N.compare('a', 'b') < 0);
        assertTrue(N.compare('z', 'a') > 0);
    }

    @Test
    public void testCompare_CharArray() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'a', 'b', 'c' };
        char[] c = { 'a', 'b', 'd' };

        assertEquals(0, N.compare(a, b));
        assertTrue(N.compare(a, c) < 0);
        assertTrue(N.compare(c, a) > 0);
    }

    @Test
    public void testCompare_CharArray_WithRange() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] b = { 'x', 'b', 'c', 'y' };

        assertEquals(0, N.compare(a, 1, b, 1, 2));
        assertTrue(N.compare(a, 0, b, 0, 2) < 0);
    }

    @Test
    public void testCompare_Short() {
        assertEquals(0, N.compare((short) 5, (short) 5));
        assertTrue(N.compare((short) 3, (short) 5) < 0);
        assertTrue(N.compare((short) 7, (short) 5) > 0);
    }

    @Test
    public void testCompare_ShortArray() {
        short[] a = { 1, 2, 3 };
        short[] b = { 1, 2, 3 };
        short[] c = { 1, 2, 4 };

        assertEquals(0, N.compare(a, b));
        assertTrue(N.compare(a, c) < 0);
        assertTrue(N.compare(c, a) > 0);
    }

    @Test
    public void testCompare_ShortArray_WithRange() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 0, 2, 3, 0 };

        assertEquals(0, N.compare(a, 1, b, 1, 2));
        assertTrue(N.compare(a, 0, b, 0, 2) > 0);
    }

    @Test
    public void testCompare_Int() {
        assertEquals(0, N.compare(5, 5));
        assertTrue(N.compare(3, 5) < 0);
        assertTrue(N.compare(7, 5) > 0);
    }

    @Test
    public void testCompare_IntArray() {
        int[] a = { 1, 2, 3 };
        int[] b = { 1, 2, 3 };
        int[] c = { 1, 2, 4 };

        assertEquals(0, N.compare(a, b));
        assertTrue(N.compare(a, c) < 0);
        assertTrue(N.compare(c, a) > 0);
    }

    @Test
    public void testCompare_IntArray_WithRange() {
        int[] a = { 1, 2, 3, 4 };
        int[] b = { 0, 2, 3, 0 };

        assertEquals(0, N.compare(a, 1, b, 1, 2));
        assertTrue(N.compare(a, 0, b, 0, 2) > 0);
    }

    @Test
    public void testCompare_Long() {
        assertEquals(0, N.compare(5L, 5L));
        assertTrue(N.compare(3L, 5L) < 0);
        assertTrue(N.compare(7L, 5L) > 0);
    }

    @Test
    public void testCompare_LongArray() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 1L, 2L, 3L };
        long[] c = { 1L, 2L, 4L };

        assertEquals(0, N.compare(a, b));
        assertTrue(N.compare(a, c) < 0);
        assertTrue(N.compare(c, a) > 0);
    }

    @Test
    public void testCompare_LongArray_WithRange() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 0L, 2L, 3L, 0L };

        assertEquals(0, N.compare(a, 1, b, 1, 2));
        assertTrue(N.compare(a, 0, b, 0, 2) > 0);
    }

    @Test
    public void testCompare_Float() {
        assertEquals(0, N.compare(5.0f, 5.0f));
        assertTrue(N.compare(3.0f, 5.0f) < 0);
        assertTrue(N.compare(7.0f, 5.0f) > 0);
    }

    @Test
    public void testCompare_FloatArray() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        float[] b = { 1.0f, 2.0f, 3.0f };
        float[] c = { 1.0f, 2.0f, 4.0f };

        assertEquals(0, N.compare(a, b));
        assertTrue(N.compare(a, c) < 0);
        assertTrue(N.compare(c, a) > 0);
    }

    @Test
    public void testCompare_FloatArray_WithRange() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] b = { 0.0f, 2.0f, 3.0f, 0.0f };

        assertEquals(0, N.compare(a, 1, b, 1, 2));
        assertTrue(N.compare(a, 0, b, 0, 2) > 0);
    }

    @Test
    public void testCompare_Double() {
        assertEquals(0, N.compare(5.0, 5.0));
        assertTrue(N.compare(3.0, 5.0) < 0);
        assertTrue(N.compare(7.0, 5.0) > 0);
    }

    @Test
    public void testCompare_DoubleArray() {
        double[] a = { 1.0, 2.0, 3.0 };
        double[] b = { 1.0, 2.0, 3.0 };
        double[] c = { 1.0, 2.0, 4.0 };

        assertEquals(0, N.compare(a, b));
        assertTrue(N.compare(a, c) < 0);
        assertTrue(N.compare(c, a) > 0);
    }

    @Test
    public void testCompare_DoubleArray_WithRange() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] b = { 0.0, 2.0, 3.0, 0.0 };

        assertEquals(0, N.compare(a, 1, b, 1, 2));
        assertTrue(N.compare(a, 0, b, 0, 2) > 0);
    }

    @Test
    public void testCompare_WithComparator() {
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;
        assertEquals(0, N.compare("test", "TEST", cmp));
        assertTrue(N.compare("abc", "xyz", cmp) < 0);
        assertTrue(N.compare("xyz", "abc", cmp) > 0);
    }

    @Test
    public void testCompare_ObjectArray_WithComparator() {
        String[] a = { "a", "b", "c" };
        String[] b = { "A", "B", "C" };
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(0, N.compare(a, b, cmp));
    }

    @Test
    public void testCompare_ObjectArray_WithRange_AndComparator() {
        String[] a = { "x", "b", "c", "y" };
        String[] b = { "z", "B", "C", "w" };
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(0, N.compare(a, 1, b, 1, 2, cmp));
    }

    @Test
    public void testCompare_Iterable_WithComparator() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<String> b = Arrays.asList("A", "B", "C");
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(0, N.compare(a, b, cmp));
    }

    @Test
    public void testCompare_Iterator_WithComparator() {
        Iterator<String> a = Arrays.asList("a", "b").iterator();
        Iterator<String> b = Arrays.asList("A", "B").iterator();
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(0, N.compare(a, b, cmp));
    }

    @Test
    public void testCompare_Collection_WithRange() {
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("x", "b", "c", "y");

        assertEquals(0, N.compare(a, 1, b, 1, 2));
    }

    @Test
    public void testCompare_Collection_WithRange_AndComparator() {
        List<String> a = Arrays.asList("x", "b", "c", "y");
        List<String> b = Arrays.asList("z", "B", "C", "w");
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(0, N.compare(a, 1, b, 1, 2, cmp));
    }

    @Test
    public void testCompareIgnoreCase() {
        assertEquals(0, N.compareIgnoreCase((String) "test", (String) "TEST"));
        assertTrue(N.compareIgnoreCase((String) "abc", (String) "XYZ") < 0);
        assertTrue(N.compareIgnoreCase((String) "xyz", (String) "ABC") > 0);
        assertEquals(0, N.compareIgnoreCase((String) null, (String) null));
    }

    @Test
    public void testCompareIgnoreCase_StringArray() {
        String[] a = { "a", "b", "c" };
        String[] b = { "A", "B", "C" };

        assertEquals(0, N.compareIgnoreCase(a, b));
    }

    @Test
    public void testCompareUnsigned_Byte() {
        assertEquals(0, N.compareUnsigned((byte) 5, (byte) 5));
        assertTrue(N.compareUnsigned((byte) 3, (byte) 5) < 0);
        assertTrue(N.compareUnsigned((byte) -1, (byte) 1) > 0);
    }

    @Test
    public void testCompareUnsigned_ByteArray() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 1, 2, 3 };

        assertEquals(0, N.compareUnsigned(a, b));
    }

    @Test
    public void testCompareUnsigned_ByteArray_WithRange() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] b = { 0, 2, 3, 0 };

        assertEquals(0, N.compareUnsigned(a, 1, b, 1, 2));
    }

    @Test
    public void testCompareUnsigned_Short() {
        assertEquals(0, N.compareUnsigned((short) 5, (short) 5));
        assertTrue(N.compareUnsigned((short) 3, (short) 5) < 0);
        assertTrue(N.compareUnsigned((short) -1, (short) 1) > 0);
    }

    @Test
    public void testCompareUnsigned_ShortArray() {
        short[] a = { 1, 2, 3 };
        short[] b = { 1, 2, 3 };

        assertEquals(0, N.compareUnsigned(a, b));
    }

    @Test
    public void testCompareUnsigned_ShortArray_WithRange() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 0, 2, 3, 0 };

        assertEquals(0, N.compareUnsigned(a, 1, b, 1, 2));
    }

    @Test
    public void testCompareUnsigned_Int() {
        assertEquals(0, N.compareUnsigned(5, 5));
        assertTrue(N.compareUnsigned(3, 5) < 0);
        assertTrue(N.compareUnsigned(-1, 1) > 0);
    }

    @Test
    public void testCompareUnsigned_IntArray() {
        int[] a = { 1, 2, 3 };
        int[] b = { 1, 2, 3 };

        assertEquals(0, N.compareUnsigned(a, b));
    }

    @Test
    public void testCompareUnsigned_IntArray_WithRange() {
        int[] a = { 1, 2, 3, 4 };
        int[] b = { 0, 2, 3, 0 };

        assertEquals(0, N.compareUnsigned(a, 1, b, 1, 2));
    }

    @Test
    public void testCompareUnsigned_Long() {
        assertEquals(0, N.compareUnsigned(5L, 5L));
        assertTrue(N.compareUnsigned(3L, 5L) < 0);
        assertTrue(N.compareUnsigned(-1L, 1L) > 0);
    }

    @Test
    public void testCompareUnsigned_LongArray() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 1L, 2L, 3L };

        assertEquals(0, N.compareUnsigned(a, b));
    }

    @Test
    public void testCompareUnsigned_LongArray_WithRange() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 0L, 2L, 3L, 0L };

        assertEquals(0, N.compareUnsigned(a, 1, b, 1, 2));
    }

    @Test
    public void testMismatch_BooleanArray() {
        boolean[] a = { true, false, true };
        boolean[] b = { true, false, false };

        assertEquals(2, N.mismatch(a, b));
        assertEquals(-1, N.mismatch(a, a));
    }

    @Test
    public void testMismatch_BooleanArray_WithRange() {
        boolean[] a = { true, false, true, false };
        boolean[] b = { false, false, true, true };

        assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        assertEquals(0, N.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_ByteArray() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 1, 2, 4 };

        assertEquals(2, N.mismatch(a, b));
        assertEquals(-1, N.mismatch(a, a));
    }

    @Test
    public void testMismatch_ByteArray_WithRange() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] b = { 0, 2, 3, 0 };

        assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        assertEquals(0, N.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_CharArray() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'a', 'b', 'd' };

        assertEquals(2, N.mismatch(a, b));
        assertEquals(-1, N.mismatch(a, a));
    }

    @Test
    public void testMismatch_CharArray_WithRange() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] b = { 'x', 'b', 'c', 'y' };

        assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        assertEquals(0, N.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_ShortArray() {
        short[] a = { 1, 2, 3 };
        short[] b = { 1, 2, 4 };

        assertEquals(2, N.mismatch(a, b));
        assertEquals(-1, N.mismatch(a, a));
    }

    @Test
    public void testMismatch_ShortArray_WithRange() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 0, 2, 3, 0 };

        assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        assertEquals(0, N.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_IntArray() {
        int[] a = { 1, 2, 3 };
        int[] b = { 1, 2, 4 };

        assertEquals(2, N.mismatch(a, b));
        assertEquals(-1, N.mismatch(a, a));
    }

    @Test
    public void testMismatch_IntArray_WithRange() {
        int[] a = { 1, 2, 3, 4 };
        int[] b = { 0, 2, 3, 0 };

        assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        assertEquals(0, N.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_LongArray() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 1L, 2L, 4L };

        assertEquals(2, N.mismatch(a, b));
        assertEquals(-1, N.mismatch(a, a));
    }

    @Test
    public void testMismatch_LongArray_WithRange() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 0L, 2L, 3L, 0L };

        assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        assertEquals(0, N.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_FloatArray() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        float[] b = { 1.0f, 2.0f, 4.0f };

        assertEquals(2, N.mismatch(a, b));
        assertEquals(-1, N.mismatch(a, a));
    }

    @Test
    public void testMismatch_FloatArray_WithRange() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] b = { 0.0f, 2.0f, 3.0f, 0.0f };

        assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        assertEquals(0, N.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_DoubleArray() {
        double[] a = { 1.0, 2.0, 3.0 };
        double[] b = { 1.0, 2.0, 4.0 };

        assertEquals(2, N.mismatch(a, b));
        assertEquals(-1, N.mismatch(a, a));
    }

    @Test
    public void testMismatch_DoubleArray_WithRange() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] b = { 0.0, 2.0, 3.0, 0.0 };

        assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        assertEquals(0, N.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_ObjectArray_WithComparator() {
        String[] a = { "a", "b", "c" };
        String[] b = { "A", "B", "D" };
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(2, N.mismatch(a, b, cmp));
    }

    @Test
    public void testMismatch_ObjectArray_WithRange_AndComparator() {
        String[] a = { "x", "b", "c", "y" };
        String[] b = { "z", "B", "C", "w" };
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(-1, N.mismatch(a, 1, b, 1, 2, cmp));
    }

    @Test
    public void testMismatch_Iterable_WithComparator() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<String> b = Arrays.asList("A", "B", "D");
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(2, N.mismatch(a, b, cmp));
    }

    @Test
    public void testMismatch_Iterator_WithComparator() {
        Iterator<String> a = Arrays.asList("a", "b", "c").iterator();
        Iterator<String> b = Arrays.asList("A", "B", "D").iterator();
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(2, N.mismatch(a, b, cmp));
    }

    @Test
    public void testMismatch_Collection_WithRange() {
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("x", "b", "c", "y");

        assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        assertEquals(0, N.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_Collection_WithRange_AndComparator() {
        List<String> a = Arrays.asList("x", "b", "c", "y");
        List<String> b = Arrays.asList("z", "B", "D", "w");
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(1, N.mismatch(a, 1, b, 1, 3, cmp));
    }

    @Test
    public void testToString_BooleanValue() {
        assertEquals("true", N.toString(true));
        assertEquals("false", N.toString(false));
    }

    @Test
    public void testToString_BooleanArray() {
        boolean[] arr = { true, false, true };
        String result = N.toString(arr);
        assertTrue(result.contains("true"));
        assertTrue(result.contains("false"));
    }

    @Test
    public void testToString_BooleanArray_WithRange() {
        boolean[] arr = { true, false, true, false };
        String result = N.toString(arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testToString_ByteValue() {
        assertEquals("5", N.toString((byte) 5));
        assertEquals("-1", N.toString((byte) -1));
    }

    @Test
    public void testToString_ByteArray() {
        byte[] arr = { 1, 2, 3 };
        String result = N.toString(arr);
        assertNotNull(result);
    }

    @Test
    public void testToString_ByteArray_WithRange() {
        byte[] arr = { 1, 2, 3, 4 };
        String result = N.toString(arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testToString_CharValue() {
        assertEquals("a", N.toString('a'));
        assertEquals("Z", N.toString('Z'));
    }

    @Test
    public void testToString_CharArray() {
        char[] arr = { 'a', 'b', 'c' };
        String result = N.toString(arr);
        assertNotNull(result);
    }

    @Test
    public void testToString_CharArray_WithRange() {
        char[] arr = { 'a', 'b', 'c', 'd' };
        String result = N.toString(arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testToString_ShortValue() {
        assertEquals("100", N.toString((short) 100));
        assertEquals("-50", N.toString((short) -50));
    }

    @Test
    public void testToString_ShortArray() {
        short[] arr = { 1, 2, 3 };
        String result = N.toString(arr);
        assertNotNull(result);
    }

    @Test
    public void testToString_ShortArray_WithRange() {
        short[] arr = { 1, 2, 3, 4 };
        String result = N.toString(arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testToString_IntValue() {
        assertEquals("42", N.toString(42));
        assertEquals("-42", N.toString(-42));
    }

    @Test
    public void testToString_IntArray() {
        int[] arr = { 1, 2, 3 };
        String result = N.toString(arr);
        assertNotNull(result);
    }

    @Test
    public void testToString_IntArray_WithRange() {
        int[] arr = { 1, 2, 3, 4 };
        String result = N.toString(arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testToString_LongValue() {
        assertEquals("100", N.toString(100L));
        assertEquals("-100", N.toString(-100L));
    }

    @Test
    public void testToString_LongArray() {
        long[] arr = { 1L, 2L, 3L };
        String result = N.toString(arr);
        assertNotNull(result);
    }

    @Test
    public void testToString_LongArray_WithRange() {
        long[] arr = { 1L, 2L, 3L, 4L };
        String result = N.toString(arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testToString_FloatValue() {
        String result = N.toString(3.14f);
        assertTrue(result.contains("3.14"));
    }

    @Test
    public void testToString_FloatArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f };
        String result = N.toString(arr);
        assertNotNull(result);
    }

    @Test
    public void testToString_FloatArray_WithRange() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f };
        String result = N.toString(arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testToString_DoubleValue() {
        String result = N.toString(3.14);
        assertTrue(result.contains("3.14"));
    }

    @Test
    public void testToString_DoubleArray() {
        double[] arr = { 1.0, 2.0, 3.0 };
        String result = N.toString(arr);
        assertNotNull(result);
    }

    @Test
    public void testToString_DoubleArray_WithRange() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0 };
        String result = N.toString(arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testToString_Object() {
        assertEquals("test", N.toString("test"));
        assertEquals("null", N.toString((Object) null));
    }

    @Test
    public void testToString_Object_WithDefault() {
        assertEquals("test", N.toString("test", "default"));
        assertEquals("default", N.toString(null, "default"));
    }

    @Test
    public void testToString_ObjectArray() {
        String[] arr = { "a", "b", "c" };
        String result = N.toString((Object[]) arr);
        assertNotNull(result);
    }

    @Test
    public void testToString_ObjectArray_WithRange() {
        String[] arr = { "a", "b", "c", "d" };
        String result = N.toString((Object[]) arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testDeepToString_Object() {
        int[] arr = { 1, 2, 3 };
        String result = N.deepToString(arr);
        assertNotNull(result);
    }

    @Test
    public void testDeepToString_ObjectArray() {
        Object[] arr = { new int[] { 1, 2 }, "test", 42 };
        String result = N.deepToString(arr);
        assertNotNull(result);
    }

    @Test
    public void testDeepToString_ObjectArray_WithRange() {
        Object[] arr = { "a", "b", "c", "d" };
        String result = N.deepToString(arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testDeepToString_ObjectArray_WithDefault() {
        Object[] arr = null;
        String result = N.deepToString(arr, "NULL");
        assertEquals("NULL", result);
    }

    @Test
    public void testStringOf_Boolean() {
        assertEquals("true", N.stringOf(true));
        assertEquals("false", N.stringOf(false));
    }

    @Test
    public void testStringOf_Byte() {
        assertEquals("5", N.stringOf((byte) 5));
    }

    @Test
    public void testStringOf_Char() {
        assertEquals("a", N.stringOf('a'));
    }

    @Test
    public void testStringOf_Short() {
        assertEquals("100", N.stringOf((short) 100));
    }

    @Test
    public void testStringOf_Int() {
        assertEquals("42", N.stringOf(42));
    }

    @Test
    public void testStringOf_Long() {
        assertEquals("100", N.stringOf(100L));
    }

    @Test
    public void testStringOf_Float() {
        String result = N.stringOf(3.14f);
        assertTrue(result.contains("3.14"));
    }

    @Test
    public void testStringOf_Double() {
        String result = N.stringOf(3.14);
        assertTrue(result.contains("3.14"));
    }

    @Test
    public void testStringOf_Object() {
        assertEquals("test", N.stringOf("test"));
        assertNull(N.stringOf((Object) null));
    }

    @Test
    public void testIsEmpty_CharSequence() {
        assertTrue(N.isEmpty((String) null));
        assertTrue(N.isEmpty(""));
        assertFalse(N.isEmpty("a"));
        assertFalse(N.isEmpty(" "));
    }

    @Test
    public void testIsEmpty_BooleanArray() {
        assertTrue(N.isEmpty((boolean[]) null));
        assertTrue(N.isEmpty(new boolean[0]));
        assertFalse(N.isEmpty(new boolean[] { true }));
    }

    @Test
    public void testIsEmpty_CharArray() {
        assertTrue(N.isEmpty((char[]) null));
        assertTrue(N.isEmpty(new char[0]));
        assertFalse(N.isEmpty(new char[] { 'a' }));
    }

    @Test
    public void testIsEmpty_ByteArray() {
        assertTrue(N.isEmpty((byte[]) null));
        assertTrue(N.isEmpty(new byte[0]));
        assertFalse(N.isEmpty(new byte[] { 1 }));
    }

    @Test
    public void testIsEmpty_ShortArray() {
        assertTrue(N.isEmpty((short[]) null));
        assertTrue(N.isEmpty(new short[0]));
        assertFalse(N.isEmpty(new short[] { 1 }));
    }

    @Test
    public void testIsEmpty_IntArray() {
        assertTrue(N.isEmpty((int[]) null));
        assertTrue(N.isEmpty(new int[0]));
        assertFalse(N.isEmpty(new int[] { 1 }));
    }

    @Test
    public void testIsEmpty_LongArray() {
        assertTrue(N.isEmpty((long[]) null));
        assertTrue(N.isEmpty(new long[0]));
        assertFalse(N.isEmpty(new long[] { 1L }));
    }

    @Test
    public void testIsEmpty_FloatArray() {
        assertTrue(N.isEmpty((float[]) null));
        assertTrue(N.isEmpty(new float[0]));
        assertFalse(N.isEmpty(new float[] { 1.0f }));
    }

    @Test
    public void testIsEmpty_DoubleArray() {
        assertTrue(N.isEmpty((double[]) null));
        assertTrue(N.isEmpty(new double[0]));
        assertFalse(N.isEmpty(new double[] { 1.0 }));
    }

    @Test
    public void testIsEmpty_ObjectArray() {
        assertTrue(N.isEmpty((Object[]) null));
        assertTrue(N.isEmpty(new Object[0]));
        assertFalse(N.isEmpty(new Object[] { new Object() }));
    }

    @Test
    public void testIsEmpty_Collection() {
        assertTrue(N.isEmpty((Collection<?>) null));
        assertTrue(N.isEmpty(new ArrayList<>()));
        assertFalse(N.isEmpty(Arrays.asList("a")));
    }

    @Test
    public void testIsEmpty_Map() {
        assertTrue(N.isEmpty((Map<?, ?>) null));
        assertTrue(N.isEmpty(new HashMap<>()));
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertFalse(N.isEmpty(map));
    }

    @Test
    public void testNotEmpty_CharSequence() {
        assertFalse(N.notEmpty((String) null));
        assertFalse(N.notEmpty(""));
        assertTrue(N.notEmpty("a"));
        assertTrue(N.notEmpty(" "));
    }

    @Test
    public void testNotEmpty_BooleanArray() {
        assertFalse(N.notEmpty((boolean[]) null));
        assertFalse(N.notEmpty(new boolean[0]));
        assertTrue(N.notEmpty(new boolean[] { true }));
    }

    @Test
    public void testNotEmpty_CharArray() {
        assertFalse(N.notEmpty((char[]) null));
        assertFalse(N.notEmpty(new char[0]));
        assertTrue(N.notEmpty(new char[] { 'a' }));
    }

    @Test
    public void testNotEmpty_ByteArray() {
        assertFalse(N.notEmpty((byte[]) null));
        assertFalse(N.notEmpty(new byte[0]));
        assertTrue(N.notEmpty(new byte[] { 1 }));
    }

    @Test
    public void testNotEmpty_ShortArray() {
        assertFalse(N.notEmpty((short[]) null));
        assertFalse(N.notEmpty(new short[0]));
        assertTrue(N.notEmpty(new short[] { 1 }));
    }

    @Test
    public void testNotEmpty_IntArray() {
        assertFalse(N.notEmpty((int[]) null));
        assertFalse(N.notEmpty(new int[0]));
        assertTrue(N.notEmpty(new int[] { 1 }));
    }

    @Test
    public void testNotEmpty_LongArray() {
        assertFalse(N.notEmpty((long[]) null));
        assertFalse(N.notEmpty(new long[0]));
        assertTrue(N.notEmpty(new long[] { 1L }));
    }

    @Test
    public void testNotEmpty_FloatArray() {
        assertFalse(N.notEmpty((float[]) null));
        assertFalse(N.notEmpty(new float[0]));
        assertTrue(N.notEmpty(new float[] { 1.0f }));
    }

    @Test
    public void testNotEmpty_DoubleArray() {
        assertFalse(N.notEmpty((double[]) null));
        assertFalse(N.notEmpty(new double[0]));
        assertTrue(N.notEmpty(new double[] { 1.0 }));
    }

    @Test
    public void testNotEmpty_ObjectArray() {
        assertFalse(N.notEmpty((Object[]) null));
        assertFalse(N.notEmpty(new Object[0]));
        assertTrue(N.notEmpty(new Object[] { new Object() }));
    }

    @Test
    public void testNotEmpty_Collection() {
        assertFalse(N.notEmpty((Collection<?>) null));
        assertFalse(N.notEmpty(new ArrayList<>()));
        assertTrue(N.notEmpty(Arrays.asList("a")));
    }

    @Test
    public void testNotEmpty_Map() {
        assertFalse(N.notEmpty((Map<?, ?>) null));
        assertFalse(N.notEmpty(new HashMap<>()));
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertTrue(N.notEmpty(map));
    }

    @Test
    public void testIsBlank() {
        assertTrue(N.isBlank(null));
        assertTrue(N.isBlank(""));
        assertTrue(N.isBlank(" "));
        assertTrue(N.isBlank("   "));
        assertTrue(N.isBlank("\t"));
        assertTrue(N.isBlank("\n"));
        assertFalse(N.isBlank("a"));
        assertFalse(N.isBlank(" a "));
    }

    @Test
    public void testNotBlank() {
        assertFalse(N.notBlank(null));
        assertFalse(N.notBlank(""));
        assertFalse(N.notBlank(" "));
        assertFalse(N.notBlank("   "));
        assertFalse(N.notBlank("\t"));
        assertTrue(N.notBlank("a"));
        assertTrue(N.notBlank(" a "));
    }

    @Test
    public void testDefaultIfNull_Boolean() {
        assertFalse(N.defaultIfNull((Boolean) null));
        assertTrue(N.defaultIfNull(true));
        assertFalse(N.defaultIfNull(false));
        assertTrue(N.defaultIfNull((Boolean) null, true));
        assertFalse(N.defaultIfNull((Boolean) null, false));
    }

    @Test
    public void testDefaultIfNull_Char() {
        assertEquals('\u0000', N.defaultIfNull((Character) null));
        assertEquals('a', N.defaultIfNull('a'));
        assertEquals('x', N.defaultIfNull((Character) null, 'x'));
    }

    @Test
    public void testDefaultIfNull_Byte() {
        assertEquals(0, N.defaultIfNull((Byte) null));
        assertEquals(5, N.defaultIfNull((byte) 5));
        assertEquals(10, N.defaultIfNull((Byte) null, (byte) 10));
    }

    @Test
    public void testDefaultIfNull_Short() {
        assertEquals(0, N.defaultIfNull((Short) null));
        assertEquals(5, N.defaultIfNull((short) 5));
        assertEquals(10, N.defaultIfNull((Short) null, (short) 10));
    }

    @Test
    public void testDefaultIfNull_Int() {
        assertEquals(0, N.defaultIfNull((Integer) null));
        assertEquals(5, N.defaultIfNull(5));
        assertEquals(10, N.defaultIfNull((Integer) null, 10));
    }

    @Test
    public void testDefaultIfNull_Long() {
        assertEquals(0L, N.defaultIfNull((Long) null));
        assertEquals(5L, N.defaultIfNull(5L));
        assertEquals(10L, N.defaultIfNull((Long) null, 10L));
    }

    @Test
    public void testDefaultIfNull_Float() {
        assertEquals(0.0f, N.defaultIfNull((Float) null));
        assertEquals(5.0f, N.defaultIfNull(5.0f));
        assertEquals(10.0f, N.defaultIfNull((Float) null, 10.0f));
    }

    @Test
    public void testDefaultIfNull_Double() {
        assertEquals(0.0, N.defaultIfNull((Double) null));
        assertEquals(5.0, N.defaultIfNull(5.0));
        assertEquals(10.0, N.defaultIfNull((Double) null, 10.0));
    }

    @Test
    public void testDefaultIfNull_Object() {
        assertEquals("default", N.defaultIfNull((String) null, "default"));
        assertEquals("test", N.defaultIfNull("test", "default"));
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfNull(null, (String) null));
    }

    @Test
    public void testDefaultIfNull_WithSupplier() {
        Supplier<String> supplier = () -> "default";
        assertEquals("default", N.defaultIfNull((String) null, supplier));
        assertEquals("test", N.defaultIfNull("test", supplier));
    }

    @Test
    public void testDefaultIfEmpty_CharSequence() {
        assertEquals("default", N.defaultIfEmpty((String) null, "default"));
        assertEquals("default", N.defaultIfEmpty("", "default"));
        assertEquals("test", N.defaultIfEmpty("test", "default"));
    }

    @Test
    public void testDefaultIfEmpty_Collection() {
        List<String> list = Arrays.asList("a", "b");
        List<String> defaultList = Arrays.asList("x", "y");

        assertEquals(defaultList, N.defaultIfEmpty((List<String>) null, defaultList));
        assertEquals(defaultList, N.defaultIfEmpty(new ArrayList<>(), defaultList));
        assertEquals(list, N.defaultIfEmpty(list, defaultList));
    }

    @Test
    public void testDefaultIfEmpty_Map() {
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        Map<String, String> defaultMap = new HashMap<>();
        defaultMap.put("default", "val");

        assertEquals(defaultMap, N.defaultIfEmpty((Map<String, String>) null, defaultMap));
        assertEquals(defaultMap, N.defaultIfEmpty(new HashMap<>(), defaultMap));
        assertEquals(map, N.defaultIfEmpty(map, defaultMap));
    }

    @Test
    public void testDefaultIfBlank() {
        assertEquals("default", N.defaultIfBlank((String) null, "default"));
        assertEquals("default", N.defaultIfBlank("", "default"));
        assertEquals("default", N.defaultIfBlank("  ", "default"));
        assertEquals("test", N.defaultIfBlank("test", "default"));
        assertEquals(" test ", N.defaultIfBlank(" test ", "default"));
    }

    @Test
    public void testIndexOf_BooleanArray() {
        boolean[] arr = { true, false, true, false };
        assertEquals(0, N.indexOf(arr, true));
        assertEquals(1, N.indexOf(arr, false));
        assertEquals(-1, N.indexOf((boolean[]) null, true));
        assertEquals(2, N.indexOf(arr, true, 1));
    }

    @Test
    public void testIndexOf_CharArray() {
        char[] arr = { 'a', 'b', 'c', 'b' };
        assertEquals(0, N.indexOf(arr, 'a'));
        assertEquals(1, N.indexOf(arr, 'b'));
        assertEquals(-1, N.indexOf(arr, 'z'));
        assertEquals(3, N.indexOf(arr, 'b', 2));
    }

    @Test
    public void testIndexOf_ByteArray() {
        byte[] arr = { 1, 2, 3, 2 };
        assertEquals(0, N.indexOf(arr, (byte) 1));
        assertEquals(1, N.indexOf(arr, (byte) 2));
        assertEquals(-1, N.indexOf(arr, (byte) 9));
        assertEquals(3, N.indexOf(arr, (byte) 2, 2));
    }

    @Test
    public void testIndexOf_ShortArray() {
        short[] arr = { 1, 2, 3, 2 };
        assertEquals(0, N.indexOf(arr, (short) 1));
        assertEquals(1, N.indexOf(arr, (short) 2));
        assertEquals(-1, N.indexOf(arr, (short) 9));
        assertEquals(3, N.indexOf(arr, (short) 2, 2));
    }

    @Test
    public void testIndexOf_IntArray() {
        int[] arr = { 1, 2, 3, 2 };
        assertEquals(0, N.indexOf(arr, 1));
        assertEquals(1, N.indexOf(arr, 2));
        assertEquals(-1, N.indexOf(arr, 9));
        assertEquals(3, N.indexOf(arr, 2, 2));
    }

    @Test
    public void testIndexOf_LongArray() {
        long[] arr = { 1L, 2L, 3L, 2L };
        assertEquals(0, N.indexOf(arr, 1L));
        assertEquals(1, N.indexOf(arr, 2L));
        assertEquals(-1, N.indexOf(arr, 9L));
        assertEquals(3, N.indexOf(arr, 2L, 2));
    }

    @Test
    public void testIndexOf_FloatArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 2.0f };
        assertEquals(0, N.indexOf(arr, 1.0f));
        assertEquals(1, N.indexOf(arr, 2.0f));
        assertEquals(-1, N.indexOf(arr, 9.0f));
        assertEquals(3, N.indexOf(arr, 2.0f, 2));
    }

    @Test
    public void testIndexOf_DoubleArray() {
        double[] arr = { 1.0, 2.0, 3.0, 2.0 };
        assertEquals(0, N.indexOf(arr, 1.0));
        assertEquals(1, N.indexOf(arr, 2.0));
        assertEquals(-1, N.indexOf(arr, 9.0));
        assertEquals(3, N.indexOf(arr, 2.0, 2));
    }

    @Test
    public void testIndexOf_DoubleArray_WithTolerance() {
        double[] arr = { 1.0, 2.0, 3.0 };
        assertEquals(1, N.indexOf(arr, 2.01, 0.02));
        assertEquals(-1, N.indexOf(arr, 2.1, 0.05));
    }

    @Test
    public void testIndexOf_ObjectArray() {
        String[] arr = { "a", "b", "c", "b" };
        assertEquals(0, N.indexOf(arr, "a"));
        assertEquals(1, N.indexOf(arr, "b"));
        assertEquals(-1, N.indexOf(arr, "z"));
        assertEquals(3, N.indexOf(arr, "b", 2));
    }

    @Test
    public void testIndexOf_Collection() {
        List<String> list = Arrays.asList("a", "b", "c", "b");
        assertEquals(0, N.indexOf(list, "a"));
        assertEquals(1, N.indexOf(list, "b"));
        assertEquals(-1, N.indexOf(list, "z"));
        assertEquals(3, N.indexOf(list, "b", 2));
    }

    @Test
    public void testLastIndexOf_BooleanArray() {
        boolean[] arr = { true, false, true, false };
        assertEquals(2, N.lastIndexOf(arr, true));
        assertEquals(3, N.lastIndexOf(arr, false));
        assertEquals(-1, N.lastIndexOf((boolean[]) null, true));
    }

    @Test
    public void testLastIndexOf_CharArray() {
        char[] arr = { 'a', 'b', 'c', 'b' };
        assertEquals(0, N.lastIndexOf(arr, 'a'));
        assertEquals(3, N.lastIndexOf(arr, 'b'));
        assertEquals(-1, N.lastIndexOf(arr, 'z'));
    }

    @Test
    public void testLastIndexOf_ByteArray() {
        byte[] arr = { 1, 2, 3, 2 };
        assertEquals(0, N.lastIndexOf(arr, (byte) 1));
        assertEquals(3, N.lastIndexOf(arr, (byte) 2));
        assertEquals(-1, N.lastIndexOf(arr, (byte) 9));
    }

    @Test
    public void testLastIndexOf_ShortArray() {
        short[] arr = { 1, 2, 3, 2 };
        assertEquals(0, N.lastIndexOf(arr, (short) 1));
        assertEquals(3, N.lastIndexOf(arr, (short) 2));
        assertEquals(-1, N.lastIndexOf(arr, (short) 9));
    }

    @Test
    public void testLastIndexOf_IntArray() {
        int[] arr = { 1, 2, 3, 2 };
        assertEquals(0, N.lastIndexOf(arr, 1));
        assertEquals(3, N.lastIndexOf(arr, 2));
        assertEquals(-1, N.lastIndexOf(arr, 9));
    }

    @Test
    public void testLastIndexOf_LongArray() {
        long[] arr = { 1L, 2L, 3L, 2L };
        assertEquals(0, N.lastIndexOf(arr, 1L));
        assertEquals(3, N.lastIndexOf(arr, 2L));
        assertEquals(-1, N.lastIndexOf(arr, 9L));
    }

    @Test
    public void testLastIndexOf_FloatArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 2.0f };
        assertEquals(0, N.lastIndexOf(arr, 1.0f));
        assertEquals(3, N.lastIndexOf(arr, 2.0f));
        assertEquals(-1, N.lastIndexOf(arr, 9.0f));
    }

    @Test
    public void testLastIndexOf_DoubleArray() {
        double[] arr = { 1.0, 2.0, 3.0, 2.0 };
        assertEquals(0, N.lastIndexOf(arr, 1.0));
        assertEquals(3, N.lastIndexOf(arr, 2.0));
        assertEquals(-1, N.lastIndexOf(arr, 9.0));
    }

    @Test
    public void testLastIndexOf_DoubleArray_WithTolerance() {
        double[] arr = { 1.0, 2.0, 3.0, 2.01 };
        assertEquals(3, N.lastIndexOf(arr, 2.0, 0.02));
    }

    @Test
    public void testLastIndexOf_ObjectArray() {
        String[] arr = { "a", "b", "c", "b" };
        assertEquals(0, N.lastIndexOf(arr, "a"));
        assertEquals(3, N.lastIndexOf(arr, "b"));
        assertEquals(-1, N.lastIndexOf(arr, "z"));
    }

    @Test
    public void testLastIndexOf_Collection() {
        List<String> list = Arrays.asList("a", "b", "c", "b");
        assertEquals(0, N.lastIndexOf(list, "a"));
        assertEquals(3, N.lastIndexOf(list, "b"));
        assertEquals(-1, N.lastIndexOf(list, "z"));
    }

    @Test
    public void testBinarySearch_CharArray() {
        char[] arr = { 'a', 'c', 'e', 'g' };
        assertTrue(N.binarySearch(arr, 'c') >= 0);
        assertTrue(N.binarySearch(arr, 'b') < 0);
        assertTrue(N.binarySearch(arr, 0, 4, 'e') >= 0);
    }

    @Test
    public void testBinarySearch_ByteArray() {
        byte[] arr = { 1, 3, 5, 7 };
        assertTrue(N.binarySearch(arr, (byte) 3) >= 0);
        assertTrue(N.binarySearch(arr, (byte) 2) < 0);
        assertTrue(N.binarySearch(arr, 0, 4, (byte) 5) >= 0);
    }

    @Test
    public void testBinarySearch_ShortArray() {
        short[] arr = { 1, 3, 5, 7 };
        assertTrue(N.binarySearch(arr, (short) 3) >= 0);
        assertTrue(N.binarySearch(arr, (short) 2) < 0);
        assertTrue(N.binarySearch(arr, 0, 4, (short) 5) >= 0);
    }

    @Test
    public void testBinarySearch_IntArray() {
        int[] arr = { 1, 3, 5, 7 };
        assertTrue(N.binarySearch(arr, 3) >= 0);
        assertTrue(N.binarySearch(arr, 2) < 0);
        assertTrue(N.binarySearch(arr, 0, 4, 5) >= 0);
    }

    @Test
    public void testBinarySearch_LongArray() {
        long[] arr = { 1L, 3L, 5L, 7L };
        assertTrue(N.binarySearch(arr, 3L) >= 0);
        assertTrue(N.binarySearch(arr, 2L) < 0);
        assertTrue(N.binarySearch(arr, 0, 4, 5L) >= 0);
    }

    @Test
    public void testBinarySearch_FloatArray() {
        float[] arr = { 1.0f, 3.0f, 5.0f, 7.0f };
        assertTrue(N.binarySearch(arr, 3.0f) >= 0);
        assertTrue(N.binarySearch(arr, 2.0f) < 0);
        assertTrue(N.binarySearch(arr, 0, 4, 5.0f) >= 0);
    }

    @Test
    public void testBinarySearch_DoubleArray() {
        double[] arr = { 1.0, 3.0, 5.0, 7.0 };
        assertTrue(N.binarySearch(arr, 3.0) >= 0);
        assertTrue(N.binarySearch(arr, 2.0) < 0);
        assertTrue(N.binarySearch(arr, 0, 4, 5.0) >= 0);
    }

    @Test
    public void testBinarySearch_ObjectArray() {
        String[] arr = { "a", "c", "e", "g" };
        assertTrue(N.binarySearch(arr, "c") >= 0);
        assertTrue(N.binarySearch(arr, "b") < 0);
    }

    @Test
    public void testFill_BooleanArray() {
        boolean[] arr = new boolean[5];
        N.fill(arr, true);
        for (boolean b : arr) {
            assertTrue(b);
        }
        N.fill(arr, 1, 3, false);
        assertTrue(arr[0]);
        assertFalse(arr[1]);
        assertFalse(arr[2]);
        assertTrue(arr[3]);
    }

    @Test
    public void testFill_IntArray() {
        int[] arr = new int[5];
        N.fill(arr, 42);
        for (int i : arr) {
            assertEquals(42, i);
        }
        N.fill(arr, 1, 3, 99);
        assertEquals(42, arr[0]);
        assertEquals(99, arr[1]);
        assertEquals(99, arr[2]);
        assertEquals(42, arr[3]);
    }

    @Test
    public void testFill_ObjectArray() {
        String[] arr = new String[5];
        N.fill(arr, "test");
        for (String s : arr) {
            assertEquals("test", s);
        }
    }

    @Test
    public void testFill_List() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        N.fill(list, "x");
        for (String s : list) {
            assertEquals("x", s);
        }
    }

    @Test
    public void testClone_BooleanArray() {
        boolean[] arr = { true, false, true };
        boolean[] cloned = N.clone(arr);
        assertNotSame(arr, cloned);
        assertTrue(N.equals(arr, cloned));
        assertNull(N.clone((boolean[]) null));
    }

    @Test
    public void testClone_IntArray() {
        int[] arr = { 1, 2, 3 };
        int[] cloned = N.clone(arr);
        assertNotSame(arr, cloned);
        assertTrue(N.equals(arr, cloned));
    }

    @Test
    public void testClone_ObjectArray() {
        String[] arr = { "a", "b", "c" };
        String[] cloned = N.clone(arr);
        assertNotSame(arr, cloned);
        assertTrue(N.equals(arr, cloned));
    }

    @Test
    public void testClone_2DArray() {
        int[][] arr = { { 1, 2 }, { 3, 4 } };
        int[][] cloned = N.clone(arr);
        assertNotSame(arr, cloned);
        assertEquals(arr.length, cloned.length);
    }

    @Test
    public void testCopy_BooleanArray() {
        boolean[] src = { true, false, true };
        boolean[] dest = new boolean[3];
        N.copy(src, 0, dest, 0, 3);
        assertTrue(N.equals(src, dest));
    }

    @Test
    public void testCopy_IntArray() {
        int[] src = { 1, 2, 3 };
        int[] dest = new int[3];
        N.copy(src, 0, dest, 0, 3);
        assertTrue(N.equals(src, dest));
    }

    @Test
    public void testCopy_ObjectArray() {
        String[] src = { "a", "b", "c" };
        String[] dest = new String[3];
        N.copy(src, 0, dest, 0, 3);
        assertTrue(N.equals(src, dest));
    }

    @Test
    public void testReverse_BooleanArray() {
        boolean[] arr = { true, false, true, false };
        N.reverse(arr);
        boolean[] expected = { false, true, false, true };
        assertTrue(N.equals(arr, expected));
    }

    @Test
    public void testReverse_IntArray() {
        int[] arr = { 1, 2, 3, 4 };
        N.reverse(arr);
        int[] expected = { 4, 3, 2, 1 };
        assertTrue(N.equals(arr, expected));
    }

    @Test
    public void testReverse_IntArray_WithRange() {
        int[] arr = { 1, 2, 3, 4, 5 };
        N.reverse(arr, 1, 4);
        int[] expected = { 1, 4, 3, 2, 5 };
        assertTrue(N.equals(arr, expected));
    }

    @Test
    public void testReverse_ObjectArray() {
        String[] arr = { "a", "b", "c", "d" };
        N.reverse(arr);
        String[] expected = { "d", "c", "b", "a" };
        assertTrue(N.equals(arr, expected));
    }

    @Test
    public void testReverse_List() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        N.reverse(list);
        assertEquals(Arrays.asList("d", "c", "b", "a"), list);
    }

    @Test
    public void testReverse_List_WithRange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        N.reverse(list, 1, 4);
        assertEquals(Arrays.asList("a", "d", "c", "b", "e"), list);
    }

    @Test
    public void testRotate_BooleanArray() {
        boolean[] arr = { true, false, true, false };
        N.rotate(arr, 1);
        boolean[] expected = { false, true, false, true };
        assertTrue(N.equals(arr, expected));
    }

    @Test
    public void testRotate_IntArray() {
        int[] arr = { 1, 2, 3, 4 };
        N.rotate(arr, 1);
        int[] expected = { 4, 1, 2, 3 };
        assertTrue(N.equals(arr, expected));
    }

    @Test
    public void testRotate_IntArray_NegativeDistance() {
        int[] arr = { 1, 2, 3, 4 };
        N.rotate(arr, -1);
        int[] expected = { 2, 3, 4, 1 };
        assertTrue(N.equals(arr, expected));
    }

    @Test
    public void testRotate_IntArray_WithRange() {
        int[] arr = { 1, 2, 3, 4, 5 };
        N.rotate(arr, 1, 4, 1);
        int[] expected = { 1, 4, 2, 3, 5 };
        assertTrue(N.equals(arr, expected));
    }

    @Test
    public void testRotate_ObjectArray() {
        String[] arr = { "a", "b", "c", "d" };
        N.rotate(arr, 1);
        String[] expected = { "d", "a", "b", "c" };
        assertTrue(N.equals(arr, expected));
    }

    @Test
    public void testRotate_List() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        N.rotate(list, 1);
        assertEquals(Arrays.asList("d", "a", "b", "c"), list);
    }

    @Test
    public void testSwap_BooleanArray() {
        boolean[] arr = { true, false, true };
        N.swap(arr, 0, 2);
        assertTrue(N.equals(arr, new boolean[] { true, false, true }));
        N.swap(arr, 0, 1);
        assertTrue(N.equals(arr, new boolean[] { false, true, true }));
    }

    @Test
    public void testSwap_IntArray() {
        int[] arr = { 1, 2, 3 };
        N.swap(arr, 0, 2);
        assertTrue(N.equals(arr, new int[] { 3, 2, 1 }));
    }

    @Test
    public void testSwap_ObjectArray() {
        String[] arr = { "a", "b", "c" };
        N.swap(arr, 0, 2);
        assertTrue(N.equals(arr, new String[] { "c", "b", "a" }));
    }

    @Test
    public void testSwap_List() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        N.swap(list, 0, 2);
        assertEquals(Arrays.asList("c", "b", "a"), list);
    }

    @Test
    public void testNewHashMap() {
        Map<String, Integer> map = N.newHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        map.put("a", 1);
        assertEquals(1, map.size());
    }

    @Test
    public void testNewHashMap_WithCapacity() {
        Map<String, Integer> map = N.newHashMap(100);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewLinkedHashMap() {
        Map<String, Integer> map = N.newLinkedHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertTrue(map instanceof LinkedHashMap);
    }

    @Test
    public void testNewTreeMap() {
        Map<String, Integer> map = N.newTreeMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertTrue(map instanceof TreeMap);
    }

    @Test
    public void testNewConcurrentHashMap() {
        Map<String, Integer> map = N.newConcurrentHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertTrue(map instanceof java.util.concurrent.ConcurrentHashMap);
    }

    @Test
    public void testNewHashSet() {
        Set<String> set = N.newHashSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        set.add("a");
        assertEquals(1, set.size());
    }

    @Test
    public void testNewLinkedHashSet() {
        Set<String> set = N.newLinkedHashSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertTrue(set instanceof LinkedHashSet);
    }

    @Test
    public void testNewTreeSet() {
        Set<String> set = N.newTreeSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertTrue(set instanceof TreeSet);
    }

    @Test
    public void testNewArrayList() {
        List<String> list = N.newArrayList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
        list.add("a");
        assertEquals(1, list.size());
    }

    @Test
    public void testNewArrayList_WithCapacity() {
        List<String> list = N.newArrayList(100);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testNewLinkedList() {
        List<String> list = N.newLinkedList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
        assertTrue(list instanceof LinkedList);
    }

    @Test
    public void testNewArrayDeque() {
        java.util.Deque<String> deque = N.newArrayDeque();
        assertNotNull(deque);
        assertTrue(deque.isEmpty());
        assertTrue(deque instanceof java.util.ArrayDeque);
    }

    @Test
    public void testAnyNull_TwoArgs() {
        assertTrue(N.anyNull(null, "a"));
        assertTrue(N.anyNull("a", null));
        assertTrue(N.anyNull(null, null));
        assertFalse(N.anyNull("a", "b"));
    }

    @Test
    public void testAnyNull_ThreeArgs() {
        assertTrue(N.anyNull(null, "a", "b"));
        assertTrue(N.anyNull("a", null, "b"));
        assertTrue(N.anyNull("a", "b", null));
        assertFalse(N.anyNull("a", "b", "c"));
    }

    @Test
    public void testAnyNull_VarArgs() {
        assertTrue(N.anyNull(new Object[] { "a", null, "c" }));
        assertFalse(N.anyNull(new Object[] { "a", "b", "c" }));
        assertFalse(N.anyNull(new Object[0]));
    }

    @Test
    public void testAllNull_TwoArgs() {
        assertTrue(N.allNull(null, null));
        assertFalse(N.allNull(null, "a"));
        assertFalse(N.allNull("a", null));
        assertFalse(N.allNull("a", "b"));
    }

    @Test
    public void testAllNull_ThreeArgs() {
        assertTrue(N.allNull(null, null, null));
        assertFalse(N.allNull(null, null, "a"));
        assertFalse(N.allNull("a", "b", "c"));
    }

    @Test
    public void testAllNull_VarArgs() {
        assertTrue(N.allNull(new Object[] { null, null, null }));
        assertFalse(N.allNull(new Object[] { "a", null, "c" }));
        assertTrue(N.allNull(new Object[0]));
    }

    @Test
    public void testAnyEmpty_TwoCharSequences() {
        assertTrue(N.anyEmpty("", "a"));
        assertTrue(N.anyEmpty("a", ""));
        assertTrue(N.anyEmpty(null, "a"));
        assertFalse(N.anyEmpty("a", "b"));
    }

    @Test
    public void testAnyEmpty_ThreeCharSequences() {
        assertTrue(N.anyEmpty("", "a", "b"));
        assertTrue(N.anyEmpty("a", "", "b"));
        assertFalse(N.anyEmpty("a", "b", "c"));
    }

    @Test
    public void testAnyEmpty_Collections() {
        List<String> empty = new ArrayList<>();
        List<String> nonEmpty = Arrays.asList("a");
        assertTrue(N.anyEmpty(empty, nonEmpty));
        assertTrue(N.anyEmpty(nonEmpty, empty));
        assertFalse(N.anyEmpty(nonEmpty, nonEmpty));
    }

    @Test
    public void testAllEmpty_TwoCharSequences() {
        assertTrue(N.allEmpty("", ""));
        assertTrue(N.allEmpty(null, ""));
        assertFalse(N.allEmpty("", "a"));
        assertFalse(N.allEmpty("a", "b"));
    }

    @Test
    public void testAllEmpty_ThreeCharSequences() {
        assertTrue(N.allEmpty("", "", ""));
        assertFalse(N.allEmpty("", "", "a"));
        assertFalse(N.allEmpty("a", "b", "c"));
    }

    @Test
    public void testAllEmpty_Collections() {
        List<String> empty1 = new ArrayList<>();
        List<String> empty2 = new ArrayList<>();
        List<String> nonEmpty = Arrays.asList("a");
        assertTrue(N.allEmpty(empty1, empty2));
        assertFalse(N.allEmpty(empty1, nonEmpty));
    }

    @Test
    public void testAnyBlank_TwoArgs() {
        assertTrue(N.anyBlank("", "a"));
        assertTrue(N.anyBlank(" ", "a"));
        assertTrue(N.anyBlank("a", " "));
        assertFalse(N.anyBlank("a", "b"));
    }

    @Test
    public void testAnyBlank_ThreeArgs() {
        assertTrue(N.anyBlank("", "a", "b"));
        assertTrue(N.anyBlank("a", " ", "b"));
        assertFalse(N.anyBlank("a", "b", "c"));
    }

    @Test
    public void testAllBlank_TwoArgs() {
        assertTrue(N.allBlank("", ""));
        assertTrue(N.allBlank(" ", "  "));
        assertTrue(N.allBlank(null, ""));
        assertFalse(N.allBlank("", "a"));
        assertFalse(N.allBlank("a", "b"));
    }

    @Test
    public void testAllBlank_ThreeArgs() {
        assertTrue(N.allBlank("", "", ""));
        assertTrue(N.allBlank(" ", "  ", "\t"));
        assertFalse(N.allBlank("", "", "a"));
    }

    @Test
    public void testIsSorted_CharArray() {
        assertTrue(N.isSorted(new char[] { 'a', 'b', 'c' }));
        assertFalse(N.isSorted(new char[] { 'c', 'b', 'a' }));
        assertTrue(N.isSorted(new char[] { 'a' }));
        assertTrue(N.isSorted(new char[0]));
    }

    @Test
    public void testIsSorted_IntArray() {
        assertTrue(N.isSorted(new int[] { 1, 2, 3, 4 }));
        assertFalse(N.isSorted(new int[] { 4, 3, 2, 1 }));
        assertTrue(N.isSorted(new int[] { 1, 1, 2, 2 }));
    }

    @Test
    public void testIsSorted_ObjectArray() {
        assertTrue(N.isSorted(new String[] { "a", "b", "c" }));
        assertFalse(N.isSorted(new String[] { "c", "b", "a" }));
        assertTrue(N.isSorted(new Integer[] { 1, 2, 3 }));
    }

    @Test
    public void testIsSorted_List() {
        assertTrue(N.isSorted(Arrays.asList(1, 2, 3, 4)));
        assertFalse(N.isSorted(Arrays.asList(4, 3, 2, 1)));
        assertTrue(N.isSorted(Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testSort_IntArray() {
        int[] arr = { 3, 1, 4, 1, 5 };
        N.sort(arr);
        assertTrue(N.isSorted(arr));
        int[] expected = { 1, 1, 3, 4, 5 };
        assertTrue(N.equals(arr, expected));
    }

    @Test
    public void testSort_ObjectArray() {
        String[] arr = { "c", "a", "b" };
        N.sort(arr);
        assertTrue(N.isSorted(arr));
        String[] expected = { "a", "b", "c" };
        assertTrue(N.equals(arr, expected));
    }

    @Test
    public void testSort_List() {
        List<String> list = new ArrayList<>(Arrays.asList("c", "a", "b"));
        N.sort(list);
        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    @Test
    public void testReverseSort_IntArray() {
        int[] arr = { 3, 1, 4, 1, 5 };
        N.reverseSort(arr);
        int[] expected = { 5, 4, 3, 1, 1 };
        assertTrue(N.equals(arr, expected));
    }

    @Test
    public void testReverseSort_ObjectArray() {
        String[] arr = { "c", "a", "b" };
        N.reverseSort(arr);
        String[] expected = { "c", "b", "a" };
        assertTrue(N.equals(arr, expected));
    }

    @Test
    public void testToArray_Collection() {
        List<String> list = Arrays.asList("a", "b", "c");
        Object[] arr = N.toArray(list);
        assertEquals(3, arr.length);
        assertEquals("a", arr[0]);
        assertEquals("b", arr[1]);
        assertEquals("c", arr[2]);
    }

    @Test
    public void testToArray_WithRange() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Object[] arr = N.toArray(list, 1, 4);
        assertEquals(3, arr.length);
        assertEquals("b", arr[0]);
        assertEquals("c", arr[1]);
        assertEquals("d", arr[2]);
    }

    @Test
    public void testToBooleanArray() {
        List<Boolean> list = Arrays.asList(true, false, true);
        boolean[] arr = N.toBooleanArray(list);
        assertEquals(3, arr.length);
        assertTrue(arr[0]);
        assertFalse(arr[1]);
        assertTrue(arr[2]);
    }

    @Test
    public void testToIntArray() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        int[] arr = N.toIntArray(list);
        assertEquals(3, arr.length);
        assertEquals(1, arr[0]);
        assertEquals(2, arr[1]);
        assertEquals(3, arr[2]);
    }

    @Test
    public void testToList_Array() {
        String[] arr = { "a", "b", "c" };
        List<String> list = N.toList(arr);
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
    }

    @Test
    public void testToList_IntArray() {
        int[] arr = { 1, 2, 3 };
        List<Integer> list = N.toList(arr);
        assertEquals(3, list.size());
        assertEquals(Integer.valueOf(1), list.get(0));
        assertEquals(Integer.valueOf(2), list.get(1));
        assertEquals(Integer.valueOf(3), list.get(2));
    }

    @Test
    public void testToSet_Array() {
        String[] arr = { "a", "b", "c", "a" };
        Set<String> set = N.toSet(arr);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
    }

    @Test
    public void testToMap_List() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertEquals(Integer.valueOf(3), map.get("c"));
    }

    @Test
    public void testLen_CharSequence() {
        assertEquals(5, N.len("hello"));
        assertEquals(0, N.len(""));
        assertEquals(0, N.len((String) null));
    }

    @Test
    public void testLen_Array() {
        assertEquals(3, N.len(new int[] { 1, 2, 3 }));
        assertEquals(0, N.len(new int[0]));
        assertEquals(0, N.len((int[]) null));
    }

    @Test
    public void testLen_Collection() {
        assertEquals(3, N.len(new String[] { "a", "b", "c" }));
        assertEquals(0, N.len(new String[0]));
        assertEquals(0, N.len((String[]) null));
    }

    @Test
    public void testSize_Collection() {
        assertEquals(3, N.size(Arrays.asList("a", "b", "c")));
        assertEquals(0, N.size(new ArrayList<>()));
        assertEquals(0, N.size((List<?>) null));
    }

    @Test
    public void testSize_Map() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, N.size(map));
        assertEquals(0, N.size(new HashMap<>()));
        assertEquals(0, N.size((Map<?, ?>) null));
    }

    @Test
    public void testContains_BooleanArray() {
        boolean[] arr = { true, false, true };
        assertTrue(N.contains(arr, true));
        assertTrue(N.contains(arr, false));
        assertFalse(N.contains((boolean[]) null, true));
    }

    @Test
    public void testContains_IntArray() {
        int[] arr = { 1, 2, 3 };
        assertTrue(N.contains(arr, 2));
        assertFalse(N.contains(arr, 5));
    }

    @Test
    public void testContains_ObjectArray() {
        String[] arr = { "a", "b", "c" };
        assertTrue(N.contains(arr, "b"));
        assertFalse(N.contains(arr, "z"));
    }

    @Test
    public void testContainsAll_Collection() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        assertTrue(N.containsAll(list, Arrays.asList("a", "c")));
        assertFalse(N.containsAll(list, Arrays.asList("a", "z")));
    }

    @Test
    public void testContainsAny_Collection() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertTrue(N.containsAny(list, Arrays.asList("a", "z")));
        assertFalse(N.containsAny(list, Arrays.asList("x", "z")));
    }

    @Test
    public void testDisjoint_Collections() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("d", "e", "f");
        List<String> list3 = Arrays.asList("c", "d", "e");
        assertTrue(N.disjoint(list1, list2));
        assertFalse(N.disjoint(list1, list3));
    }

    @Test
    public void testMin_IntArray() {
        int[] arr = { 3, 1, 4, 1, 5 };
        assertEquals(1, N.min(arr));
    }

    @Test
    public void testMin_ObjectArray() {
        String[] arr = { "c", "a", "b" };
        assertEquals("a", N.min(arr));
    }

    @Test
    public void testMax_IntArray() {
        int[] arr = { 3, 1, 4, 1, 5 };
        assertEquals(5, N.max(arr));
    }

    @Test
    public void testMax_ObjectArray() {
        String[] arr = { "c", "a", "b" };
        assertEquals("c", N.max(arr));
    }

    @Test
    public void testMedian_IntArray() {
        int[] arr = { 3, 1, 4, 1, 5 };
        assertEquals(3, N.median(arr));
    }

    @Test
    public void testSum_IntArray() {
        int[] arr = { 1, 2, 3, 4, 5 };
        assertEquals(15, N.sum(arr));
    }

    @Test
    public void testAverage_IntArray() {
        int[] arr = { 1, 2, 3, 4, 5 };
        assertEquals(3.0, N.average(arr), 0.001);
    }

    @Test
    public void testRepeat_String() {
        assertEquals(Arrays.asList("a", "a", "a"), N.repeat("a", 3));
    }

    @Test
    public void testRepeat_Char() {
        assertEquals(Arrays.asList('a', 'a', 'a'), N.repeat('a', 3));
    }

    @Test
    public void testConcat_StringArrays() {
        String[] arr1 = { "a", "b" };
        String[] arr2 = { "c", "d" };
        String[] result = N.concat(arr1, arr2);
        assertEquals(4, result.length);
        assertTrue(N.equals(result, new String[] { "a", "b", "c", "d" }));
    }

    @Test
    public void testConcat_IntArrays() {
        int[] arr1 = { 1, 2 };
        int[] arr2 = { 3, 4 };
        int[] result = N.concat(arr1, arr2);
        assertEquals(4, result.length);
        assertTrue(N.equals(result, new int[] { 1, 2, 3, 4 }));
    }

    @Test
    public void testAsArray() {
        String[] arr = N.asArray("a", "b", "c");
        assertEquals(3, arr.length);
        assertEquals("a", arr[0]);
        assertEquals("b", arr[1]);
        assertEquals("c", arr[2]);
    }

    @Test
    public void testAsList() {
        List<String> list = N.asList("a", "b", "c");
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
    }

    @Test
    public void testAsLinkedList() {
        LinkedList<String> list = N.asLinkedList("a", "b", "c");
        assertEquals(3, list.size());
        assertEquals("a", list.getFirst());
        assertEquals("c", list.getLast());
    }

    @Test
    public void testAsSet() {
        Set<String> set = N.asSet("a", "b", "c", "a");
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
    }

    @Test
    public void testAsLinkedHashSet() {
        Set<String> set = N.asLinkedHashSet("c", "b", "a");
        assertEquals(3, set.size());
        Iterator<String> it = set.iterator();
        assertEquals("c", it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
    }

    @Test
    public void testAsLinkedHashMap() {
        Map<String, Integer> map = N.asLinkedHashMap("a", 1, "b", 2);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    @Test
    public void testAsMap() {
        Map<String, Integer> map = N.asMap("a", 1, "b", 2, "c", 3);
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertEquals(Integer.valueOf(3), map.get("c"));
    }

    @Test
    public void testAsProps() {
        Map<String, Object> props = N.asProps("name", "John", "age", 30);
        assertEquals(2, props.size());
        assertEquals("John", props.get("name"));
        assertEquals(30, props.get("age"));
    }

    @Test
    public void testAsSingletonList() {
        List<String> list = N.asSingletonList("test");
        assertEquals(1, list.size());
        assertEquals("test", list.get(0));
    }

    @Test
    public void testAsSingletonSet() {
        Set<String> set = N.asSingletonSet("test");
        assertEquals(1, set.size());
        assertTrue(set.contains("test"));
    }

    @Test
    public void testAsSingletonMap() {
        Map<String, Integer> map = N.asSingletonMap("key", 100);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(100), map.get("key"));
    }

    @Test
    public void testAsDeque() {
        java.util.Deque<String> deque = N.asDeque("a", "b", "c");
        assertEquals(3, deque.size());
        assertEquals("a", deque.getFirst());
        assertEquals("c", deque.getLast());
    }

    @Test
    public void testAsArrayDeque() {
        java.util.ArrayDeque<String> deque = N.asArrayDeque("a", "b", "c");
        assertEquals(3, deque.size());
        assertEquals("a", deque.getFirst());
        assertEquals("c", deque.getLast());
    }

    @Test
    public void testAsQueue() {
        java.util.Queue<String> queue = N.asQueue("a", "b", "c");
        assertEquals(3, queue.size());
        assertEquals("a", queue.peek());
    }

    @Test
    public void testAsPriorityQueue() {
        java.util.PriorityQueue<Integer> queue = N.asPriorityQueue(3, 1, 2);
        assertEquals(3, queue.size());
        assertEquals(Integer.valueOf(1), queue.poll());
        assertEquals(Integer.valueOf(2), queue.poll());
        assertEquals(Integer.valueOf(3), queue.poll());
    }

    @Test
    public void testAsNavigableSet() {
        java.util.NavigableSet<Integer> set = N.asNavigableSet(3, 1, 2);
        assertEquals(3, set.size());
        assertEquals(Integer.valueOf(1), set.first());
        assertEquals(Integer.valueOf(3), set.last());
    }

    @Test
    public void testAsSortedSet() {
        java.util.SortedSet<Integer> set = N.asSortedSet(3, 1, 2);
        assertEquals(3, set.size());
        assertEquals(Integer.valueOf(1), set.first());
        assertEquals(Integer.valueOf(3), set.last());
    }

    @Test
    public void testAsArrayBlockingQueue() {
        java.util.concurrent.ArrayBlockingQueue<String> queue = N.asArrayBlockingQueue("a", "b");
        assertEquals(2, queue.size());
        assertEquals("a", queue.peek());
    }

    @Test
    public void testAsLinkedBlockingQueue() {
        java.util.concurrent.LinkedBlockingQueue<String> queue = N.asLinkedBlockingQueue("a", "b");
        assertEquals(2, queue.size());
        assertEquals("a", queue.peek());
    }

    @Test
    public void testAsLinkedBlockingDeque() {
        java.util.concurrent.LinkedBlockingDeque<String> deque = N.asLinkedBlockingDeque("a", "b");
        assertEquals(2, deque.size());
        assertEquals("a", deque.getFirst());
    }

    @Test
    public void testAsConcurrentLinkedQueue() {
        java.util.concurrent.ConcurrentLinkedQueue<String> queue = N.asConcurrentLinkedQueue("a", "b");
        assertEquals(2, queue.size());
        assertEquals("a", queue.peek());
    }

    @Test
    public void testAsConcurrentLinkedDeque() {
        java.util.concurrent.ConcurrentLinkedDeque<String> deque = N.asConcurrentLinkedDeque("a", "b");
        assertEquals(2, deque.size());
        assertEquals("a", deque.getFirst());
    }

    @Test
    public void testAsDelayQueue() {
        java.util.concurrent.DelayQueue<java.util.concurrent.Delayed> queue = N.asDelayQueue();
        assertNotNull(queue);
        assertTrue(queue.isEmpty());
    }

    @Test
    public void testAsMultiset() {
        com.landawn.abacus.util.Multiset<String> multiset = N.asMultiset("a", "b", "a", "c");
        assertEquals(4, multiset.size());
        assertEquals(2, multiset.count("a"));
        assertEquals(1, multiset.count("b"));
    }

    @Test
    public void testEmptyList() {
        List<String> list = N.emptyList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> list.add("test"));
    }

    @Test
    public void testEmptySet() {
        Set<String> set = N.emptySet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> set.add("test"));
    }

    @Test
    public void testEmptyMap() {
        Map<String, String> map = N.emptyMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> map.put("key", "value"));
    }

    @Test
    public void testEmptySortedSet() {
        java.util.SortedSet<String> set = N.emptySortedSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> set.add("test"));
    }

    @Test
    public void testEmptyNavigableSet() {
        java.util.NavigableSet<String> set = N.emptyNavigableSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> set.add("test"));
    }

    @Test
    public void testEmptySortedMap() {
        java.util.SortedMap<String, String> map = N.emptySortedMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> map.put("key", "value"));
    }

    @Test
    public void testEmptyNavigableMap() {
        java.util.NavigableMap<String, String> map = N.emptyNavigableMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> map.put("key", "value"));
    }

    @Test
    public void testEmptyIterator() {
        Iterator<String> it = N.emptyIterator();
        assertNotNull(it);
        assertFalse(it.hasNext());
    }

    @Test
    public void testEmptyListIterator() {
        java.util.ListIterator<String> it = N.emptyListIterator();
        assertNotNull(it);
        assertFalse(it.hasNext());
        assertFalse(it.hasPrevious());
    }

    @Test
    public void testEmptyDataset() {
    }

    @Test
    public void testEmptyInputStream() {
        java.io.InputStream is = N.emptyInputStream();
        assertNotNull(is);
        assertDoesNotThrow(() -> assertEquals(-1, is.read()));
    }

    @Test
    public void testFindFirst() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        com.landawn.abacus.util.u.Nullable<Integer> result = N.findFirst(list, x -> x > 3);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(4), result.get());
    }

    @Test
    public void testFindFirstIndex() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        com.landawn.abacus.util.u.OptionalInt index = N.findFirstIndex(list, x -> x > 3);
        assertTrue(index.isPresent());
        assertEquals(3, index.getAsInt());
    }

    @Test
    public void testFindFirstNonNull() {
    }

    @Test
    public void testFindLast() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        com.landawn.abacus.util.u.Nullable<Integer> result = N.findLast(list, x -> x < 4);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(3), result.get());
    }

    @Test
    public void testFindLastIndex() {
        List<Integer> list = Arrays.asList(1, 2, 3, 2, 1);
        com.landawn.abacus.util.u.OptionalInt index = N.findLastIndex(list, x -> x == 2);
        assertTrue(index.isPresent());
        assertEquals(3, index.getAsInt());
    }

    @Test
    public void testFindLastNonNull() {
    }

    @Test
    public void testFirstNonNull() {
        com.landawn.abacus.util.u.Optional<String> result = N.firstNonNull(null, "first", null, "second");
        assertTrue(result.isPresent());
        assertEquals("first", result.get());
    }

    @Test
    public void testLastNonNull() {
        com.landawn.abacus.util.u.Optional<String> result = N.lastNonNull(null, "first", null, "second", null);
        assertTrue(result.isPresent());
        assertEquals("second", result.get());
    }

    @Test
    public void testFirstNonEmpty() {
        com.landawn.abacus.util.u.Optional<String> result = N.firstNonEmpty("", null, "first", "second");
        assertTrue(result.isPresent());
        assertEquals("first", result.get());
    }

    @Test
    public void testFirstNonBlank() {
        com.landawn.abacus.util.u.Optional<String> result = N.firstNonBlank("", "  ", "first", "second");
        assertTrue(result.isPresent());
        assertEquals("first", result.get());
    }

    @Test
    public void testFirstElement() {
        List<String> list = Arrays.asList("a", "b", "c");
        com.landawn.abacus.util.u.Nullable<String> result = N.firstElement(list);
        assertTrue(result.isPresent());
        assertEquals("a", result.get());
    }

    @Test
    public void testFirstElements() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        List<String> result = N.firstElements(list, 2);
        assertEquals(2, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
    }

    @Test
    public void testLastElement() {
        List<String> list = Arrays.asList("a", "b", "c");
        com.landawn.abacus.util.u.Nullable<String> result = N.lastElement(list);
        assertTrue(result.isPresent());
        assertEquals("c", result.get());
    }

    @Test
    public void testLastElements() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        List<String> result = N.lastElements(list, 2);
        assertEquals(2, result.size());
        assertEquals("c", result.get(0));
        assertEquals("d", result.get(1));
    }

    @Test
    public void testFirstOrNullIfEmpty() {
        List<String> list = Arrays.asList("a", "b");
        assertEquals("a", N.firstOrNullIfEmpty(list));
        assertNull(N.firstOrNullIfEmpty(new ArrayList<>()));
    }

    @Test
    public void testFirstOrDefaultIfEmpty() {
        List<String> list = Arrays.asList("a", "b");
        assertEquals("a", N.firstOrDefaultIfEmpty(list, "default"));
        assertEquals("default", N.firstOrDefaultIfEmpty(new ArrayList<>(), "default"));
    }

    @Test
    public void testLastOrNullIfEmpty() {
        List<String> list = Arrays.asList("a", "b");
        assertEquals("b", N.lastOrNullIfEmpty(list));
        assertNull(N.lastOrNullIfEmpty(new ArrayList<>()));
    }

    @Test
    public void testLastOrDefaultIfEmpty() {
        List<String> list = Arrays.asList("a", "b");
        assertEquals("b", N.lastOrDefaultIfEmpty(list, "default"));
        assertEquals("default", N.lastOrDefaultIfEmpty(new ArrayList<>(), "default"));
    }

    @Test
    public void testFirstEntry() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        com.landawn.abacus.util.u.Optional<Map.Entry<String, Integer>> entry = N.firstEntry(map);
        assertTrue(entry.isPresent());
        assertEquals("a", entry.get().getKey());
        assertEquals(Integer.valueOf(1), entry.get().getValue());
    }

    @Test
    public void testLastEntry() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        com.landawn.abacus.util.u.Optional<Map.Entry<String, Integer>> entry = N.lastEntry(map);
        assertTrue(entry.isPresent());
        assertEquals("b", entry.get().getKey());
        assertEquals(Integer.valueOf(2), entry.get().getValue());
    }

    @Test
    public void testParallelSort() {
        Integer[] arr = { 5, 2, 8, 1, 9 };
        N.parallelSort(arr);
        assertEquals(1, arr[0]);
        assertEquals(2, arr[1]);
        assertEquals(5, arr[2]);
        assertEquals(8, arr[3]);
        assertEquals(9, arr[4]);
    }

    @Test
    public void testSortBy() {
        List<String> list = Arrays.asList("apple", "pie", "a", "dog");
        N.sortBy(list, String::length);
        assertEquals("a", list.get(0));
        assertEquals(3, list.get(1).length());
        assertEquals(3, list.get(2).length());
        assertEquals("apple", list.get(3));
    }

    @Test
    public void testSortByInt() {
        List<String> list = Arrays.asList("apple", "zoo", "a", "be");
        N.sortByInt(list, String::length);
        assertEquals("a", list.get(0));
        assertEquals(2, list.get(1).length());
        assertEquals(3, list.get(2).length());
        assertEquals("apple", list.get(3));
    }

    @Test
    public void testSortByLong() {
        List<String> list = Arrays.asList("apple", "pie", "a");
        N.sortByLong(list, s -> (long) s.length());
        assertEquals("a", list.get(0));
        assertEquals("pie", list.get(1));
        assertEquals("apple", list.get(2));
    }

    @Test
    public void testSortByDouble() {
        List<String> list = Arrays.asList("apple", "pie", "a");
        N.sortByDouble(list, s -> (double) s.length());
        assertEquals("a", list.get(0));
        assertEquals("pie", list.get(1));
        assertEquals("apple", list.get(2));
    }

    @Test
    public void testSortByFloat() {
        List<String> list = Arrays.asList("apple", "pie", "a");
        N.sortByFloat(list, s -> (float) s.length());
        assertEquals("a", list.get(0));
        assertEquals("pie", list.get(1));
        assertEquals("apple", list.get(2));
    }

    @Test
    public void testParallelSortBy() {
        List<String> list = Arrays.asList("apple", "zoo", "a", "be");
        N.parallelSortBy(list, String::length);
        assertEquals("a", list.get(0));
        assertEquals(2, list.get(1).length());
    }

    @Test
    public void testParallelSortByInt() {
        List<String> list = Arrays.asList("apple", "zoo", "a", "be");
        N.parallelSortByInt(list, String::length);
        assertEquals("a", list.get(0));
        assertEquals(2, list.get(1).length());
    }

    @Test
    public void testParallelSortByLong() {
        List<String> list = Arrays.asList("apple", "pie", "a");
        N.parallelSortByLong(list, s -> (long) s.length());
        assertEquals("a", list.get(0));
        assertEquals("pie", list.get(1));
    }

    @Test
    public void testParallelSortByDouble() {
        List<String> list = Arrays.asList("apple", "pie", "a");
        N.parallelSortByDouble(list, s -> (double) s.length());
        assertEquals("a", list.get(0));
        assertEquals("pie", list.get(1));
    }

    @Test
    public void testParallelSortByFloat() {
        List<String> list = Arrays.asList("apple", "pie", "a");
        N.parallelSortByFloat(list, s -> (float) s.length());
        assertEquals("a", list.get(0));
        assertEquals("pie", list.get(1));
    }

    @Test
    public void testReverseSortBy() {
        List<String> list = Arrays.asList("a", "zoo", "be", "apple");
        N.reverseSortBy(list, String::length);
        assertEquals("apple", list.get(0));
        assertEquals(3, list.get(1).length());
        assertEquals(2, list.get(2).length());
        assertEquals("a", list.get(3));
    }

    @Test
    public void testReverseSortByInt() {
        List<String> list = Arrays.asList("a", "cat", "apple");
        N.reverseSortByInt(list, String::length);
        assertEquals("apple", list.get(0));
        assertEquals("cat", list.get(1));
        assertEquals("a", list.get(2));
    }

    @Test
    public void testReverseSortByLong() {
        List<String> list = Arrays.asList("a", "cat", "apple");
        N.reverseSortByLong(list, s -> (long) s.length());
        assertEquals("apple", list.get(0));
        assertEquals("cat", list.get(1));
        assertEquals("a", list.get(2));
    }

    @Test
    public void testReverseSortByDouble() {
        List<String> list = Arrays.asList("a", "cat", "apple");
        N.reverseSortByDouble(list, s -> (double) s.length());
        assertEquals("apple", list.get(0));
        assertEquals("cat", list.get(1));
        assertEquals("a", list.get(2));
    }

    @Test
    public void testReverseSortByFloat() {
        List<String> list = Arrays.asList("a", "cat", "apple");
        N.reverseSortByFloat(list, s -> (float) s.length());
        assertEquals("apple", list.get(0));
        assertEquals("cat", list.get(1));
        assertEquals("a", list.get(2));
    }

    @Test
    public void testToByteArray() {
        List<Byte> list = Arrays.asList((byte) 1, (byte) 2, (byte) 3);
        byte[] arr = N.toByteArray(list);
        assertEquals(3, arr.length);
        assertEquals((byte) 1, arr[0]);
        assertEquals((byte) 2, arr[1]);
        assertEquals((byte) 3, arr[2]);
    }

    @Test
    public void testToCharArray() {
        List<Character> list = Arrays.asList('a', 'b', 'c');
        char[] arr = N.toCharArray(list);
        assertEquals(3, arr.length);
        assertEquals('a', arr[0]);
        assertEquals('b', arr[1]);
        assertEquals('c', arr[2]);
    }

    @Test
    public void testToShortArray() {
        List<Short> list = Arrays.asList((short) 1, (short) 2, (short) 3);
        short[] arr = N.toShortArray(list);
        assertEquals(3, arr.length);
        assertEquals((short) 1, arr[0]);
        assertEquals((short) 2, arr[1]);
        assertEquals((short) 3, arr[2]);
    }

    @Test
    public void testToLongArray() {
        List<Long> list = Arrays.asList(1L, 2L, 3L);
        long[] arr = N.toLongArray(list);
        assertEquals(3, arr.length);
        assertEquals(1L, arr[0]);
        assertEquals(2L, arr[1]);
        assertEquals(3L, arr[2]);
    }

    @Test
    public void testToFloatArray() {
        List<Float> list = Arrays.asList(1.1f, 2.2f, 3.3f);
        float[] arr = N.toFloatArray(list);
        assertEquals(3, arr.length);
        assertEquals(1.1f, arr[0], 0.01);
        assertEquals(2.2f, arr[1], 0.01);
        assertEquals(3.3f, arr[2], 0.01);
    }

    @Test
    public void testToDoubleArray() {
        List<Double> list = Arrays.asList(1.1, 2.2, 3.3);
        double[] arr = N.toDoubleArray(list);
        assertEquals(3, arr.length);
        assertEquals(1.1, arr[0], 0.01);
        assertEquals(2.2, arr[1], 0.01);
        assertEquals(3.3, arr[2], 0.01);
    }

    @Test
    public void testToMap() {
        class Pair {
            String key;
            Integer value;

            Pair(String k, Integer v) {
                key = k;
                value = v;
            }
        }
        List<Pair> pairs = Arrays.asList(new Pair("a", 1), new Pair("b", 2), new Pair("c", 3));
        Map<String, Integer> map = N.toMap(pairs, p -> p.key, p -> p.value);
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertEquals(Integer.valueOf(3), map.get("c"));
    }

    @Test
    public void testToCollection() {
        Integer[] arr = { 1, 2, 3 };
        Collection<Integer> col = N.toCollection(arr, ArrayList::new);
        assertEquals(3, col.size());
        assertTrue(col.contains(1));
        assertTrue(col.contains(2));
        assertTrue(col.contains(3));
    }

    @Test
    public void testUnmodifiableList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        List<String> unmodList = N.unmodifiableList(list);
        assertEquals(3, unmodList.size());
        assertThrows(UnsupportedOperationException.class, () -> unmodList.add("d"));
    }

    @Test
    public void testUnmodifiableSet() {
        Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> unmodSet = N.unmodifiableSet(set);
        assertEquals(3, unmodSet.size());
        assertThrows(UnsupportedOperationException.class, () -> unmodSet.add("d"));
    }

    @Test
    public void testUnmodifiableMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Map<String, Integer> unmodMap = N.unmodifiableMap(map);
        assertEquals(1, unmodMap.size());
        assertThrows(UnsupportedOperationException.class, () -> unmodMap.put("b", 2));
    }

    @Test
    public void testUnmodifiableSortedSet() {
        java.util.SortedSet<Integer> set = new TreeSet<>(Arrays.asList(3, 1, 2));
        java.util.SortedSet<Integer> unmodSet = N.unmodifiableSortedSet(set);
        assertEquals(3, unmodSet.size());
        assertThrows(UnsupportedOperationException.class, () -> unmodSet.add(4));
    }

    @Test
    public void testUnmodifiableNavigableSet() {
        java.util.NavigableSet<Integer> set = new TreeSet<>(Arrays.asList(3, 1, 2));
        java.util.NavigableSet<Integer> unmodSet = N.unmodifiableNavigableSet(set);
        assertEquals(3, unmodSet.size());
        assertThrows(UnsupportedOperationException.class, () -> unmodSet.add(4));
    }

    @Test
    public void testUnmodifiableSortedMap() {
        java.util.SortedMap<String, Integer> map = new TreeMap<>();
        map.put("a", 1);
        java.util.SortedMap<String, Integer> unmodMap = N.unmodifiableSortedMap(map);
        assertEquals(1, unmodMap.size());
        assertThrows(UnsupportedOperationException.class, () -> unmodMap.put("b", 2));
    }

    @Test
    public void testUnmodifiableNavigableMap() {
        java.util.NavigableMap<String, Integer> map = new TreeMap<>();
        map.put("a", 1);
        java.util.NavigableMap<String, Integer> unmodMap = N.unmodifiableNavigableMap(map);
        assertEquals(1, unmodMap.size());
        assertThrows(UnsupportedOperationException.class, () -> unmodMap.put("b", 2));
    }

    @Test
    public void testUnmodifiableCollection() {
        Collection<String> col = new ArrayList<>(Arrays.asList("a", "b"));
        Collection<String> unmodCol = N.unmodifiableCollection(col);
        assertEquals(2, unmodCol.size());
        assertThrows(UnsupportedOperationException.class, () -> unmodCol.add("c"));
    }

    @Test
    public void testGreaterThan() {
        assertTrue(N.greaterThan(5, 3));
        assertFalse(N.greaterThan(3, 5));
        assertFalse(N.greaterThan(3, 3));
    }

    @Test
    public void testGreaterEqual() {
        assertTrue(N.greaterEqual(5, 3));
        assertTrue(N.greaterEqual(3, 3));
        assertFalse(N.greaterEqual(2, 3));
    }

    @Test
    public void testLessThan() {
        assertTrue(N.lessThan(3, 5));
        assertFalse(N.lessThan(5, 3));
        assertFalse(N.lessThan(3, 3));
    }

    @Test
    public void testLessEqual() {
        assertTrue(N.lessEqual(3, 5));
        assertTrue(N.lessEqual(3, 3));
        assertFalse(N.lessEqual(5, 3));
    }

    @Test
    public void testGtAndLt() {
        assertTrue(N.gtAndLt(5, 3, 7));
        assertFalse(N.gtAndLt(2, 3, 7));
        assertFalse(N.gtAndLt(8, 3, 7));
    }

    @Test
    public void testGtAndLe() {
        assertTrue(N.gtAndLe(5, 3, 7));
        assertTrue(N.gtAndLe(7, 3, 7));
        assertFalse(N.gtAndLe(2, 3, 7));
    }

    @Test
    public void testGeAndLt() {
        assertTrue(N.geAndLt(5, 3, 7));
        assertTrue(N.geAndLt(3, 3, 7));
        assertFalse(N.geAndLt(7, 3, 7));
    }

    @Test
    public void testGeAndLe() {
        assertTrue(N.geAndLe(5, 3, 7));
        assertTrue(N.geAndLe(3, 3, 7));
        assertTrue(N.geAndLe(7, 3, 7));
        assertFalse(N.geAndLe(2, 3, 7));
    }

    @Test
    public void testIsBetween() {
        assertTrue(N.isBetween(5, 3, 7));
        assertTrue(N.isBetween(3, 3, 7));
        assertTrue(N.isBetween(7, 3, 7));
        assertFalse(N.isBetween(2, 3, 7));
    }

    @Test
    public void testIsTrue() {
        assertTrue(N.isTrue(true));
        assertFalse(N.isTrue(false));
        assertFalse(N.isTrue(null));
    }

    @Test
    public void testIsFalse() {
        assertTrue(N.isFalse(false));
        assertFalse(N.isFalse(true));
        assertFalse(N.isFalse(null));
    }

    @Test
    public void testIsNotTrue() {
        assertTrue(N.isNotTrue(false));
        assertTrue(N.isNotTrue(null));
        assertFalse(N.isNotTrue(true));
    }

    @Test
    public void testIsNotFalse() {
        assertTrue(N.isNotFalse(true));
        assertTrue(N.isNotFalse(null));
        assertFalse(N.isNotFalse(false));
    }

    @Test
    public void testIndexOfIgnoreCase() {
        String[] arr = { "Apple", "Banana", "Cherry" };
        assertEquals(1, N.indexOfIgnoreCase(arr, "banana"));
        assertEquals(-1, N.indexOfIgnoreCase(arr, "grape"));
    }

    @Test
    public void testLastIndexOfIgnoreCase() {
        String[] arr = { "Apple", "Banana", "apple" };
        assertEquals(2, N.lastIndexOfIgnoreCase(arr, "APPLE"));
        assertEquals(-1, N.lastIndexOfIgnoreCase(arr, "grape"));
    }

    @Test
    public void testIndexOfSubList() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> subList = Arrays.asList(3, 4);
        assertEquals(2, N.indexOfSubList(list, subList));
    }

    @Test
    public void testLastIndexOfSubList() {
        List<Integer> list = Arrays.asList(1, 2, 3, 2, 3, 4);
        List<Integer> subList = Arrays.asList(2, 3);
        assertEquals(3, N.lastIndexOfSubList(list, subList));
    }

    @Test
    public void testPadLeft() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        N.padLeft(list, 5, "x");
        assertEquals(5, list.size());
        assertEquals("x", list.get(0));
        assertEquals("x", list.get(1));
    }

    @Test
    public void testPadRight() {
        Collection<String> col = new ArrayList<>(Arrays.asList("a", "b", "c"));
        N.padRight(col, 5, "x");
        assertEquals(5, col.size());
    }

    @Test
    public void testNullToEmpty() {
        String[] arr = null;
        String[] result = N.nullToEmpty(arr);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testNullToEmptyForEach() {
        String[] arr = { null, "a", null, "b" };
        N.nullToEmptyForEach(arr);
        assertEquals("", arr[0]);
        assertEquals("a", arr[1]);
        assertEquals("", arr[2]);
        assertEquals("b", arr[3]);
    }

    @Test
    public void testNewArray() {
        String[] arr = N.newArray(String.class, 3);
        assertEquals(3, arr.length);
        assertNull(arr[0]);
    }

    @Test
    public void testNewInstance() {
        String str = N.newInstance(String.class);
        assertNotNull(str);
        assertEquals("", str);
    }

    @Test
    public void testNewEntry() {
        Map.Entry<String, Integer> entry = N.newEntry("key", 100);
        assertEquals("key", entry.getKey());
        assertEquals(Integer.valueOf(100), entry.getValue());
    }

    @Test
    public void testNewImmutableEntry() {
        Map.Entry<String, Integer> entry = N.newImmutableEntry("key", 100);
        assertEquals("key", entry.getKey());
        assertEquals(Integer.valueOf(100), entry.getValue());
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(200));
    }

    @Test
    public void testNewBiMap() {
        com.landawn.abacus.util.BiMap<String, Integer> biMap = N.newBiMap();
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewIdentityHashMap() {
        java.util.IdentityHashMap<String, Integer> map = N.newIdentityHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewConcurrentHashSet() {
        Set<String> set = N.newConcurrentHashSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    @Test
    public void testNewDataset() {
    }

    @Test
    public void testNewEmptyDataset() {
    }

    @Test
    public void testNewMultiset() {
        com.landawn.abacus.util.Multiset<String> multiset = N.newMultiset();
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testNewCollection() {
        Collection<String> col = N.newCollection(ArrayList.class);
        assertNotNull(col);
        assertTrue(col.isEmpty());
        assertTrue(col instanceof ArrayList);
    }

    @Test
    public void testNewMap() {
        Map<String, Integer> map = N.newMap(HashMap.class);
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertTrue(map instanceof HashMap);
    }

    @Test
    public void testNewMultimap() {
        com.landawn.abacus.util.Multimap<String, Integer, List<Integer>> multimap = N.newMultimap(HashMap::new, ArrayList::new);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewListMultimap() {
        com.landawn.abacus.util.ListMultimap<String, Integer> multimap = N.newListMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSetMultimap() {
        com.landawn.abacus.util.SetMultimap<String, Integer> multimap = N.newSetMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedListMultimap() {
        com.landawn.abacus.util.ListMultimap<String, Integer> multimap = N.newLinkedListMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedSetMultimap() {
        com.landawn.abacus.util.SetMultimap<String, Integer> multimap = N.newLinkedSetMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSortedListMultimap() {
        com.landawn.abacus.util.ListMultimap<String, Integer> multimap = N.newSortedListMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSortedSetMultimap() {
        com.landawn.abacus.util.SetMultimap<String, Integer> multimap = N.newSortedSetMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSetFromMap() {
        Map<String, Boolean> map = new HashMap<>();
        Set<String> set = N.newSetFromMap(map);
        assertNotNull(set);
        assertTrue(set.isEmpty());
        set.add("test");
        assertTrue(map.containsKey("test"));
    }

    @Test
    public void testEnumSetOf() {
        enum TestEnum {
            A, B, C
        }
        com.landawn.abacus.util.ImmutableSet<TestEnum> set = N.enumSetOf(TestEnum.class);
        assertNotNull(set);
    }

    @Test
    public void testEnumMapOf() {
        enum TestEnum {
            A, B, C
        }
        com.landawn.abacus.util.ImmutableBiMap<TestEnum, String> map = N.enumMapOf(TestEnum.class);
        assertNotNull(map);
        assertFalse(map.isEmpty());
    }

    @Test
    public void testEnumListOf() {
        enum TestEnum {
            A, B, C
        }
        List<TestEnum> list = N.enumListOf(TestEnum.class);
        assertEquals(3, list.size());
        assertTrue(list.contains(TestEnum.A));
        assertTrue(list.contains(TestEnum.B));
        assertTrue(list.contains(TestEnum.C));
    }

    @Test
    public void testCopyOf() {
        String[] original = { "a", "b", "c" };
        String[] copy = N.copyOf(original, 3);
        assertEquals(original.length, copy.length);
        assertTrue(Arrays.equals(original, copy));
        assertNotSame(original, copy);
    }

    @Test
    public void testCopyOfRange() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String[] copy = N.copyOfRange(arr, 1, 4);
        assertEquals(3, copy.length);
        assertEquals("b", copy[0]);
        assertEquals("c", copy[1]);
        assertEquals("d", copy[2]);
    }

    @Test
    public void testTypeOf() {
        com.landawn.abacus.type.Type<String> type = N.typeOf(String.class);
        assertNotNull(type);
        assertEquals(String.class, type.clazz());
    }

    @Test
    public void testValueOf() {
        Integer result = N.valueOf("123", Integer.class);
        assertEquals(Integer.valueOf(123), result);
    }

    @Test
    public void testDefaultValueOf() {
        int result = N.defaultValueOf(int.class);
        assertEquals(0, result);
    }

    @Test
    public void testConvert() {
        String result = N.convert(123, String.class);
        assertEquals("123", result);
    }

    @Test
    public void testCastIfAssignable() {
        Object obj = "test";
        com.landawn.abacus.util.u.Nullable<String> result = N.castIfAssignable(obj, String.class);
        assertTrue(result.isPresent());
        assertEquals("test", result.get());

        com.landawn.abacus.util.u.Nullable<Integer> notAssignable = N.castIfAssignable(obj, Integer.class);
        assertFalse(notAssignable.isPresent());
    }

    @Test
    public void testMerge() {
    }

    @Test
    public void testNegate() {
        boolean[] arr = { true, false, true };
        N.negate(arr);
        assertFalse(arr[0]);
        assertTrue(arr[1]);
        assertFalse(arr[2]);
    }

    @Test
    public void testShuffle() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        N.shuffle(list);
        assertEquals(5, list.size());
        assertTrue(list.contains(1));
        assertTrue(list.contains(5));
    }

    @Test
    public void testReverseToList() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> reversed = N.reverseToList(list);
        assertEquals(5, reversed.size());
        assertEquals(Integer.valueOf(5), reversed.get(0));
        assertEquals(Integer.valueOf(1), reversed.get(4));
    }

    @Test
    public void testSwapIf() {
        com.landawn.abacus.util.Pair<Integer, Integer> pair = com.landawn.abacus.util.Pair.of(5, 2);
        N.swapIf(pair, p -> p.getLeft() > p.getRight());
        assertEquals(Integer.valueOf(2), pair.getLeft());
        assertEquals(Integer.valueOf(5), pair.getRight());
    }

    @Test
    public void testRepeatCollection() {
        List<String> list = Arrays.asList("a", "b");
        List<String> repeated = N.repeatCollection(list, 3);
        assertEquals(6, repeated.size());
        assertEquals("a", repeated.get(0));
        assertEquals("b", repeated.get(1));
        assertEquals("a", repeated.get(2));
    }

    @Test
    public void testRepeatCollectionToSize() {
        List<String> list = Arrays.asList("a", "b");
        List<String> repeated = N.repeatCollectionToSize(list, 5);
        assertEquals(5, repeated.size());
        assertEquals("a", repeated.get(0));
        assertEquals("b", repeated.get(1));
        assertEquals("a", repeated.get(2));
    }

    @Test
    public void testRepeatElements() {
        List<String> list = Arrays.asList("a", "b");
        List<String> repeated = N.repeatElements(list, 3);
        assertEquals(6, repeated.size());
        assertEquals("a", repeated.get(0));
        assertEquals("a", repeated.get(1));
        assertEquals("a", repeated.get(2));
        assertEquals("b", repeated.get(3));
    }

    @Test
    public void testRepeatElementsToSize() {
        List<String> list = Arrays.asList("a", "b");
        List<String> repeated = N.repeatElementsToSize(list, 5);
        assertEquals(5, repeated.size());
        assertEquals("a", repeated.get(0));
        assertEquals("a", repeated.get(1));
        assertEquals("a", repeated.get(2));
    }

    @Test
    public void testIndicesOfAll() {
        List<Integer> list = Arrays.asList(1, 2, 3, 2, 5);
        int[] indices = N.indicesOfAll(list, 2);
        assertEquals(2, indices.length);
        assertEquals(1, indices[0]);
        assertEquals(3, indices[1]);
    }

    @Test
    public void testIndicesOfAllMax() {
        List<Integer> list = Arrays.asList(1, 5, 3, 5, 2);
        int[] indices = N.indicesOfAllMax(list);
        assertEquals(2, indices.length);
        assertEquals(1, indices[0]);
        assertEquals(3, indices[1]);
    }

    @Test
    public void testIndicesOfAllMin() {
        List<Integer> list = Arrays.asList(5, 1, 3, 1, 2);
        int[] indices = N.indicesOfAllMin(list);
        assertEquals(2, indices.length);
        assertEquals(1, indices[0]);
        assertEquals(3, indices[1]);
    }

    @Test
    public void testHaveSameElements() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(3, 2, 1);
        assertTrue(N.haveSameElements(list1, list2));

        List<Integer> list3 = Arrays.asList(1, 2, 4);
        assertFalse(N.haveSameElements(list1, list3));
    }

    @Test
    public void testGetElement() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("b", N.getElement(list, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.getElement(list, 5));
    }

    @Test
    public void testGetOnlyElement() {
        List<String> list = Arrays.asList("only");
        com.landawn.abacus.util.u.Nullable<String> result = N.getOnlyElement(list);
        assertTrue(result.isPresent());
        assertEquals("only", result.get());

        List<String> multipleElements = Arrays.asList("a", "b");
        assertThrows(com.landawn.abacus.exception.TooManyElementsException.class, () -> N.getOnlyElement(multipleElements));
    }

    @Test
    public void testEqualsCollection() {
        Collection<Integer> col1 = Arrays.asList(1, 2, 3);
        Collection<Integer> col2 = Arrays.asList(1, 2, 3);
        assertThrows(UnsupportedOperationException.class, () -> N.equalsCollection(col1, col2));
    }

    @Test
    public void testEqualsByProps() {
        assertDoesNotThrow(() -> {
            class TestBean {
                private String name;

                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }
            }
            TestBean bean1 = new TestBean();
            bean1.setName("test");
            TestBean bean2 = new TestBean();
            bean2.setName("test");
            N.equalsByProps(bean1, bean2, Arrays.asList("name"));
        });
    }

    @Test
    public void testEqualsByCommonProps() {
        assertDoesNotThrow(() -> {
            class TestBean {
                private String name;

                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }
            }
            TestBean bean1 = new TestBean();
            bean1.setName("test");
            TestBean bean2 = new TestBean();
            bean2.setName("test");
            N.equalsByCommonProps(bean1, bean2);
        });
    }

    @Test
    public void testCompareByProps() {
        assertDoesNotThrow(() -> {
            class TestBean {
                private String name;

                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }
            }
            TestBean bean1 = new TestBean();
            bean1.setName("apple");
            TestBean bean2 = new TestBean();
            bean2.setName("banana");
            N.compareByProps(bean1, bean2, Arrays.asList("name"));
        });
    }

    @Test
    public void testRegisterConverter() {
    }

    @Test
    public void testNewProxyInstance() {
        java.util.function.Function<String, String> func = s -> s.toUpperCase();
        Object proxy = N.newProxyInstance(new Class<?>[] { java.util.function.Function.class }, (p, method, args) -> {
            if (method.getName().equals("apply")) {
                return func.apply((String) args[0]);
            }
            return null;
        });
        assertNotNull(proxy);
    }

}
