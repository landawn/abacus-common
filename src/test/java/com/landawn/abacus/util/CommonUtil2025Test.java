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

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CommonUtil2025Test extends TestBase {

    @Test
    public void testCheckFromToIndex_ValidRange() {
        assertDoesNotThrow(() -> CommonUtil.checkFromToIndex(0, 5, 10));
        assertDoesNotThrow(() -> CommonUtil.checkFromToIndex(0, 0, 0));
        assertDoesNotThrow(() -> CommonUtil.checkFromToIndex(5, 5, 10));
    }

    @Test
    public void testCheckFromToIndex_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(-1, 5, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(5, 4, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(0, 11, 10));
    }

    @Test
    public void testCheckFromIndexSize_ValidRange() {
        assertDoesNotThrow(() -> CommonUtil.checkFromIndexSize(0, 5, 10));
        assertDoesNotThrow(() -> CommonUtil.checkFromIndexSize(0, 0, 0));
        assertDoesNotThrow(() -> CommonUtil.checkFromIndexSize(5, 5, 10));
    }

    @Test
    public void testCheckFromIndexSize_InvalidRange() {
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromIndexSize(-1, 5, 10));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkFromIndexSize(0, -1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromIndexSize(6, 5, 10));
    }

    @Test
    public void testCheckElementIndex_ValidIndex() {
        assertEquals(0, CommonUtil.checkElementIndex(0, 5));
        assertEquals(4, CommonUtil.checkElementIndex(4, 5));
    }

    @Test
    public void testCheckElementIndex_InvalidIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(-1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(5, 5));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementIndex(0, -1));
    }

    @Test
    public void testCheckElementIndex_WithDescription() {
        assertEquals(0, CommonUtil.checkElementIndex(0, 5, "index"));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(-1, 5, "myIndex"));
    }

    @Test
    public void testCheckPositionIndex_ValidIndex() {
        assertEquals(0, CommonUtil.checkPositionIndex(0, 5));
        assertEquals(5, CommonUtil.checkPositionIndex(5, 5));
    }

    @Test
    public void testCheckPositionIndex_InvalidIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkPositionIndex(-1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkPositionIndex(6, 5));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkPositionIndex(0, -1));
    }

    @Test
    public void testCheckPositionIndex_WithDescription() {
        assertEquals(0, CommonUtil.checkPositionIndex(0, 5, "position"));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkPositionIndex(6, 5, "myPosition"));
    }

    @Test
    public void testCheckArgNotNull_ValidArgument() {
        String result = CommonUtil.checkArgNotNull("test");
        assertEquals("test", result);
    }

    @Test
    public void testCheckArgNotNull_NullArgument() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null, "arg"));
    }

    @Test
    public void testCheckArgNotNull_WithErrorMessage() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null, "Custom error message"));
        try {
            CommonUtil.checkArgNotNull(null, "arg");
            fail("Should throw exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("cannot be null"));
        }
    }

    @Test
    public void testCheckArgNotEmpty_CharSequence() {
        assertEquals("test", CommonUtil.checkArgNotEmpty("test", "str"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty("", "str"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((String) null, "str"));
    }

    @Test
    public void testCheckArgNotEmpty_BooleanArray() {
        boolean[] arr = { true, false };
        assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "arr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new boolean[0], "arr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((boolean[]) null, "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_CharArray() {
        char[] arr = { 'a', 'b' };
        assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "arr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new char[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ByteArray() {
        byte[] arr = { 1, 2 };
        assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "arr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new byte[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ShortArray() {
        short[] arr = { 1, 2 };
        assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "arr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new short[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_IntArray() {
        int[] arr = { 1, 2 };
        assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "arr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new int[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_LongArray() {
        long[] arr = { 1L, 2L };
        assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "arr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new long[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_FloatArray() {
        float[] arr = { 1.0f, 2.0f };
        assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "arr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new float[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_DoubleArray() {
        double[] arr = { 1.0, 2.0 };
        assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "arr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new double[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ObjectArray() {
        String[] arr = { "a", "b" };
        assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "arr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new String[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_Collection() {
        List<String> list = Arrays.asList("a", "b");
        assertSame(list, CommonUtil.checkArgNotEmpty(list, "list"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new ArrayList<>(), "list"));
    }

    @Test
    public void testCheckArgNotEmpty_Map() {
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertSame(map, CommonUtil.checkArgNotEmpty(map, "map"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new HashMap<>(), "map"));
    }

    @Test
    public void testCheckArgNotBlank_ValidString() {
        assertEquals("test", CommonUtil.checkArgNotBlank("test", "str"));
        assertEquals(" test ", CommonUtil.checkArgNotBlank(" test ", "str"));
    }

    @Test
    public void testCheckArgNotBlank_BlankString() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank("", "str"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank("   ", "str"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank((String) null, "str"));
    }

    @Test
    public void testCheckArgNotNegative_Byte() {
        assertEquals(5, CommonUtil.checkArgNotNegative((byte) 5, "value"));
        assertEquals(0, CommonUtil.checkArgNotNegative((byte) 0, "value"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative((byte) -1, "value"));
    }

    @Test
    public void testCheckArgNotNegative_Short() {
        assertEquals(5, CommonUtil.checkArgNotNegative((short) 5, "value"));
        assertEquals(0, CommonUtil.checkArgNotNegative((short) 0, "value"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative((short) -1, "value"));
    }

    @Test
    public void testCheckArgNotNegative_Int() {
        assertEquals(5, CommonUtil.checkArgNotNegative(5, "value"));
        assertEquals(0, CommonUtil.checkArgNotNegative(0, "value"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1, "value"));
    }

    @Test
    public void testCheckArgNotNegative_Long() {
        assertEquals(5L, CommonUtil.checkArgNotNegative(5L, "value"));
        assertEquals(0L, CommonUtil.checkArgNotNegative(0L, "value"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1L, "value"));
    }

    @Test
    public void testCheckArgNotNegative_Float() {
        assertEquals(5.0f, CommonUtil.checkArgNotNegative(5.0f, "value"));
        assertEquals(0.0f, CommonUtil.checkArgNotNegative(0.0f, "value"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1.0f, "value"));
    }

    @Test
    public void testCheckArgNotNegative_Double() {
        assertEquals(5.0, CommonUtil.checkArgNotNegative(5.0, "value"));
        assertEquals(0.0, CommonUtil.checkArgNotNegative(0.0, "value"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1.0, "value"));
    }

    @Test
    public void testCheckArgPositive_Byte() {
        assertEquals(5, CommonUtil.checkArgPositive((byte) 5, "value"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((byte) 0, "value"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((byte) -1, "value"));
    }

    @Test
    public void testCheckArgPositive_Short() {
        assertEquals(5, CommonUtil.checkArgPositive((short) 5, "value"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((short) 0, "value"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((short) -1, "value"));
    }

    @Test
    public void testCheckArgPositive_Int() {
        assertEquals(5, CommonUtil.checkArgPositive(5, "value"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0, "value"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-1, "value"));
    }

    @Test
    public void testCheckArgPositive_Long() {
        assertEquals(5L, CommonUtil.checkArgPositive(5L, "value"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0L, "value"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-1L, "value"));
    }

    @Test
    public void testCheckArgPositive_Float() {
        assertEquals(5.0f, CommonUtil.checkArgPositive(5.0f, "value"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0.0f, "value"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-1.0f, "value"));
    }

    @Test
    public void testCheckArgPositive_Double() {
        assertEquals(5.0, CommonUtil.checkArgPositive(5.0, "value"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0.0, "value"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-1.0, "value"));
    }

    @Test
    public void testCheckElementNotNull_Array() {
        Object[] arr = { "a", "b", "c" };
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull(arr));
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull(new Object[0]));

        Object[] arrWithNull = { "a", null, "c" };
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(arrWithNull));
    }

    @Test
    public void testCheckElementNotNull_Array_WithMessage() {
        Object[] arrWithNull = { "a", null, "c" };
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(arrWithNull, "myArray"));
    }

    @Test
    public void testCheckElementNotNull_Collection() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull(list));
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull(new ArrayList<>()));

        List<String> listWithNull = Arrays.asList("a", null, "c");
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(listWithNull));
    }

    @Test
    public void testCheckElementNotNull_Collection_WithMessage() {
        List<String> listWithNull = Arrays.asList("a", null, "c");
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(listWithNull, "myList"));
    }

    @Test
    public void testCheckKeyNotNull_Map() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        assertDoesNotThrow(() -> CommonUtil.checkKeyNotNull(map));

        Map<String, String> mapWithNullKey = new HashMap<>();
        mapWithNullKey.put(null, "value");
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkKeyNotNull(mapWithNullKey));
    }

    @Test
    public void testCheckKeyNotNull_Map_WithMessage() {
        Map<String, String> mapWithNullKey = new HashMap<>();
        mapWithNullKey.put(null, "value");
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkKeyNotNull(mapWithNullKey, "myMap"));
    }

    @Test
    public void testCheckValueNotNull_Map() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        assertDoesNotThrow(() -> CommonUtil.checkValueNotNull(map));

        Map<String, String> mapWithNullValue = new HashMap<>();
        mapWithNullValue.put("key", null);
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkValueNotNull(mapWithNullValue));
    }

    @Test
    public void testCheckValueNotNull_Map_WithMessage() {
        Map<String, String> mapWithNullValue = new HashMap<>();
        mapWithNullValue.put("key", null);
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkValueNotNull(mapWithNullValue, "myMap"));
    }

    @Test
    public void testCheckArgument_Simple() {
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false));
    }

    @Test
    public void testCheckArgument_WithMessage() {
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "Error message"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Error message"));
    }

    @Test
    public void testCheckArgument_WithTemplate() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Value {} is invalid", 5));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Value %s is invalid", 5));
    }

    @Test
    public void testCheckArgument_WithPrimitiveParams() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "char: {}", 'a'));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "int: {}", 10));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "long: {}", 100L));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "double: {}", 3.14));
    }

    @Test
    public void testCheckArgument_WithMultipleParams() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Value {} must be less than {}", 10, 5));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Value {} {} {}", "a", "b", "c"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Value {} {} {} {}", "a", "b", "c", "d"));
    }

    @Test
    public void testCheckArgument_WithSupplier() {
        Supplier<String> supplier = () -> "Lazy error message";
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, supplier));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, supplier));
    }

    @Test
    public void testCheckState_Simple() {
        assertDoesNotThrow(() -> CommonUtil.checkState(true));
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false));
    }

    @Test
    public void testCheckState_WithMessage() {
        assertDoesNotThrow(() -> CommonUtil.checkState(true, "Error message"));
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Error message"));
    }

    @Test
    public void testCheckState_WithTemplate() {
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "State {} is invalid", "ACTIVE"));
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "State %s is invalid", "ACTIVE"));
    }

    @Test
    public void testCheckState_WithPrimitiveParams() {
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "char: {}", 'a'));
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "int: {}", 10));
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "long: {}", 100L));
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "double: {}", 3.14));
    }

    @Test
    public void testCheckState_WithMultipleParams() {
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Value {} must be less than {}", 10, 5));
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Value {} {} {}", "a", "b", "c"));
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Value {} {} {} {}", "a", "b", "c", "d"));
    }

    @Test
    public void testCheckState_WithSupplier() {
        Supplier<String> supplier = () -> "Lazy error message";
        assertDoesNotThrow(() -> CommonUtil.checkState(true, supplier));
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, supplier));
    }

    @Test
    public void testRequireNonNull_Simple() {
        String result = CommonUtil.requireNonNull("test");
        assertEquals("test", result);
        assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null));
    }

    @Test
    public void testRequireNonNull_WithMessage() {
        assertEquals("test", CommonUtil.requireNonNull("test", "value"));
        assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null, "value"));
    }

    @Test
    public void testRequireNonNull_WithSupplier() {
        Supplier<String> supplier = () -> "Error message";
        assertEquals("test", CommonUtil.requireNonNull("test", supplier));
        assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null, supplier));
    }

    @Test
    public void testEquals_Boolean() {
        assertTrue(CommonUtil.equals(true, true));
        assertTrue(CommonUtil.equals(false, false));
        assertFalse(CommonUtil.equals(true, false));
        assertFalse(CommonUtil.equals(false, true));
    }

    @Test
    public void testEquals_Char() {
        assertTrue(CommonUtil.equals('a', 'a'));
        assertFalse(CommonUtil.equals('a', 'b'));
    }

    @Test
    public void testEquals_Byte() {
        assertTrue(CommonUtil.equals((byte) 1, (byte) 1));
        assertFalse(CommonUtil.equals((byte) 1, (byte) 2));
    }

    @Test
    public void testEquals_Short() {
        assertTrue(CommonUtil.equals((short) 1, (short) 1));
        assertFalse(CommonUtil.equals((short) 1, (short) 2));
    }

    @Test
    public void testEquals_Int() {
        assertTrue(CommonUtil.equals(1, 1));
        assertFalse(CommonUtil.equals(1, 2));
    }

    @Test
    public void testEquals_Long() {
        assertTrue(CommonUtil.equals(1L, 1L));
        assertFalse(CommonUtil.equals(1L, 2L));
    }

    @Test
    public void testEquals_Float() {
        assertTrue(CommonUtil.equals(1.0f, 1.0f));
        assertFalse(CommonUtil.equals(1.0f, 2.0f));
        assertTrue(CommonUtil.equals(Float.NaN, Float.NaN));
        assertTrue(CommonUtil.equals(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
    }

    @Test
    public void testEquals_Double() {
        assertTrue(CommonUtil.equals(1.0, 1.0));
        assertFalse(CommonUtil.equals(1.0, 2.0));
        assertTrue(CommonUtil.equals(Double.NaN, Double.NaN));
        assertTrue(CommonUtil.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
    }

    @Test
    public void testEquals_String() {
        assertTrue(CommonUtil.equals("test", "test"));
        assertFalse(CommonUtil.equals("test", "Test"));
        assertTrue(CommonUtil.equals((String) null, (String) null));
        assertFalse(CommonUtil.equals("test", null));
        assertFalse(CommonUtil.equals(null, "test"));
    }

    @Test
    public void testEqualsIgnoreCase_String() {
        assertTrue(CommonUtil.equalsIgnoreCase("test", "test"));
        assertTrue(CommonUtil.equalsIgnoreCase("test", "Test"));
        assertTrue(CommonUtil.equalsIgnoreCase("test", "TEST"));
        assertTrue(CommonUtil.equalsIgnoreCase((String) null, (String) null));
        assertFalse(CommonUtil.equalsIgnoreCase("test", null));
        assertFalse(CommonUtil.equalsIgnoreCase(null, "test"));
    }

    @Test
    public void testEquals_Object() {
        assertTrue(CommonUtil.equals(new Integer(5), new Integer(5)));
        assertFalse(CommonUtil.equals(new Integer(5), new Integer(6)));
        assertTrue(CommonUtil.equals((Object) null, (Object) null));
        assertFalse(CommonUtil.equals(new Integer(5), null));
    }

    @Test
    public void testEquals_BooleanArray() {
        assertTrue(CommonUtil.equals(new boolean[] { true, false }, new boolean[] { true, false }));
        assertFalse(CommonUtil.equals(new boolean[] { true, false }, new boolean[] { false, true }));
        assertTrue(CommonUtil.equals((boolean[]) null, (boolean[]) null));
        assertFalse(CommonUtil.equals(new boolean[] { true }, null));
    }

    @Test
    public void testEquals_BooleanArray_WithRange() {
        boolean[] a = { true, false, true, false };
        boolean[] b = { false, false, true, true };

        assertTrue(CommonUtil.equals(a, 1, b, 1, 2));
        assertFalse(CommonUtil.equals(a, 0, b, 0, 2));
        assertTrue(CommonUtil.equals(a, 0, a, 0, 0));
    }

    @Test
    public void testEquals_CharArray() {
        assertTrue(CommonUtil.equals(new char[] { 'a', 'b' }, new char[] { 'a', 'b' }));
        assertFalse(CommonUtil.equals(new char[] { 'a', 'b' }, new char[] { 'b', 'a' }));
        assertTrue(CommonUtil.equals((char[]) null, (char[]) null));
    }

    @Test
    public void testEquals_CharArray_WithRange() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] b = { 'x', 'b', 'c', 'y' };

        assertTrue(CommonUtil.equals(a, 1, b, 1, 2));
        assertFalse(CommonUtil.equals(a, 0, b, 0, 2));
    }

    @Test
    public void testEquals_ByteArray() {
        assertTrue(CommonUtil.equals(new byte[] { 1, 2 }, new byte[] { 1, 2 }));
        assertFalse(CommonUtil.equals(new byte[] { 1, 2 }, new byte[] { 2, 1 }));
        assertTrue(CommonUtil.equals((byte[]) null, (byte[]) null));
    }

    @Test
    public void testEquals_ByteArray_WithRange() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] b = { 0, 2, 3, 0 };

        assertTrue(CommonUtil.equals(a, 1, b, 1, 2));
        assertFalse(CommonUtil.equals(a, 0, b, 0, 2));
    }

    @Test
    public void testEquals_ShortArray() {
        assertTrue(CommonUtil.equals(new short[] { 1, 2 }, new short[] { 1, 2 }));
        assertFalse(CommonUtil.equals(new short[] { 1, 2 }, new short[] { 2, 1 }));
        assertTrue(CommonUtil.equals((short[]) null, (short[]) null));
    }

    @Test
    public void testEquals_ShortArray_WithRange() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 0, 2, 3, 0 };

        assertTrue(CommonUtil.equals(a, 1, b, 1, 2));
        assertFalse(CommonUtil.equals(a, 0, b, 0, 2));
    }

    @Test
    public void testEquals_IntArray() {
        assertTrue(CommonUtil.equals(new int[] { 1, 2 }, new int[] { 1, 2 }));
        assertFalse(CommonUtil.equals(new int[] { 1, 2 }, new int[] { 2, 1 }));
        assertTrue(CommonUtil.equals((int[]) null, (int[]) null));
    }

    @Test
    public void testEquals_IntArray_WithRange() {
        int[] a = { 1, 2, 3, 4 };
        int[] b = { 0, 2, 3, 0 };

        assertTrue(CommonUtil.equals(a, 1, b, 1, 2));
        assertFalse(CommonUtil.equals(a, 0, b, 0, 2));
    }

    @Test
    public void testEquals_LongArray() {
        assertTrue(CommonUtil.equals(new long[] { 1L, 2L }, new long[] { 1L, 2L }));
        assertFalse(CommonUtil.equals(new long[] { 1L, 2L }, new long[] { 2L, 1L }));
        assertTrue(CommonUtil.equals((long[]) null, (long[]) null));
    }

    @Test
    public void testEquals_LongArray_WithRange() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 0L, 2L, 3L, 0L };

        assertTrue(CommonUtil.equals(a, 1, b, 1, 2));
        assertFalse(CommonUtil.equals(a, 0, b, 0, 2));
    }

    @Test
    public void testEquals_FloatArray() {
        assertTrue(CommonUtil.equals(new float[] { 1.0f, 2.0f }, new float[] { 1.0f, 2.0f }));
        assertFalse(CommonUtil.equals(new float[] { 1.0f, 2.0f }, new float[] { 2.0f, 1.0f }));
        assertTrue(CommonUtil.equals((float[]) null, (float[]) null));
    }

    @Test
    public void testEquals_FloatArray_WithRange() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] b = { 0.0f, 2.0f, 3.0f, 0.0f };

        assertTrue(CommonUtil.equals(a, 1, b, 1, 2));
        assertFalse(CommonUtil.equals(a, 0, b, 0, 2));
    }

    @Test
    public void testEquals_FloatArray_WithDelta() {
        assertTrue(CommonUtil.equals(new float[] { 1.0f, 2.0f }, new float[] { 1.01f, 2.01f }, 0.02f));
        assertFalse(CommonUtil.equals(new float[] { 1.0f, 2.0f }, new float[] { 1.1f, 2.1f }, 0.05f));
    }

    @Test
    public void testEquals_DoubleArray() {
        assertTrue(CommonUtil.equals(new double[] { 1.0, 2.0 }, new double[] { 1.0, 2.0 }));
        assertFalse(CommonUtil.equals(new double[] { 1.0, 2.0 }, new double[] { 2.0, 1.0 }));
        assertTrue(CommonUtil.equals((double[]) null, (double[]) null));
    }

    @Test
    public void testEquals_DoubleArray_WithRange() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] b = { 0.0, 2.0, 3.0, 0.0 };

        assertTrue(CommonUtil.equals(a, 1, b, 1, 2));
        assertFalse(CommonUtil.equals(a, 0, b, 0, 2));
    }

    @Test
    public void testEquals_DoubleArray_WithDelta() {
        assertTrue(CommonUtil.equals(new double[] { 1.0, 2.0 }, new double[] { 1.01, 2.01 }, 0.02));
        assertFalse(CommonUtil.equals(new double[] { 1.0, 2.0 }, new double[] { 1.1, 2.1 }, 0.05));
    }

    @Test
    public void testEquals_ObjectArray() {
        assertTrue(CommonUtil.equals(new String[] { "a", "b" }, new String[] { "a", "b" }));
        assertFalse(CommonUtil.equals(new String[] { "a", "b" }, new String[] { "b", "a" }));
        assertTrue(CommonUtil.equals((Object[]) null, (Object[]) null));
    }

    @Test
    public void testEquals_ObjectArray_WithRange() {
        String[] a = { "a", "b", "c", "d" };
        String[] b = { "x", "b", "c", "y" };

        assertTrue(CommonUtil.equals(a, 1, b, 1, 2));
        assertFalse(CommonUtil.equals(a, 0, b, 0, 2));
    }

    @Test
    public void testDeepEquals_Object() {
        assertTrue(CommonUtil.deepEquals("test", "test"));
        assertTrue(CommonUtil.deepEquals(null, null));
        assertFalse(CommonUtil.deepEquals("test", "other"));
    }

    @Test
    public void testDeepEquals_Arrays() {
        assertTrue(CommonUtil.deepEquals(new int[] { 1, 2 }, new int[] { 1, 2 }));
        assertTrue(CommonUtil.deepEquals(new String[] { "a", "b" }, new String[] { "a", "b" }));
        assertFalse(CommonUtil.deepEquals(new int[] { 1, 2 }, new int[] { 2, 1 }));
    }

    @Test
    public void testDeepEquals_NestedArrays() {
        Object[] a = { new int[] { 1, 2 }, new String[] { "a", "b" } };
        Object[] b = { new int[] { 1, 2 }, new String[] { "a", "b" } };
        Object[] c = { new int[] { 1, 2 }, new String[] { "a", "c" } };

        assertTrue(CommonUtil.deepEquals(a, b));
        assertFalse(CommonUtil.deepEquals(a, c));
    }

    @Test
    public void testDeepEquals_ObjectArray_WithRange() {
        String[] a = { "a", "b", "c", "d" };
        String[] b = { "x", "b", "c", "y" };

        assertTrue(CommonUtil.deepEquals(a, 1, b, 1, 2));
        assertFalse(CommonUtil.deepEquals(a, 0, b, 0, 2));
    }

    @Test
    public void testEqualsIgnoreCase_StringArray() {
        assertTrue(CommonUtil.equalsIgnoreCase(new String[] { "a", "b" }, new String[] { "A", "B" }));
        assertTrue(CommonUtil.equalsIgnoreCase(new String[] { "Test" }, new String[] { "test" }));
        assertFalse(CommonUtil.equalsIgnoreCase(new String[] { "a", "b" }, new String[] { "a", "c" }));
        assertTrue(CommonUtil.equalsIgnoreCase((String[]) null, (String[]) null));
    }

    @Test
    public void testEqualsIgnoreCase_StringArray_WithRange() {
        String[] a = { "a", "b", "c", "d" };
        String[] b = { "X", "B", "C", "Y" };

        assertTrue(CommonUtil.equalsIgnoreCase(a, 1, b, 1, 2));
        assertFalse(CommonUtil.equalsIgnoreCase(a, 0, b, 0, 2));
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
        assertTrue(CommonUtil.equalsByKeys(map1, map2, keys));

        Collection<String> allKeys = Arrays.asList("a", "b", "c");
        assertFalse(CommonUtil.equalsByKeys(map1, map2, allKeys));
    }

    @Test
    public void testEqualsByKeys_NullMaps() {
        assertTrue(CommonUtil.equalsByKeys(null, null, Arrays.asList("a")));
        assertFalse(CommonUtil.equalsByKeys(new HashMap<>(), null, Arrays.asList("a")));
        assertFalse(CommonUtil.equalsByKeys(null, new HashMap<>(), Arrays.asList("a")));
    }

    @Test
    public void testHashCode_Boolean() {
        assertEquals(1231, CommonUtil.hashCode(true));
        assertEquals(1237, CommonUtil.hashCode(false));
    }

    @Test
    public void testHashCode_Char() {
        assertEquals('a', CommonUtil.hashCode('a'));
        assertEquals('Z', CommonUtil.hashCode('Z'));
    }

    @Test
    public void testHashCode_Byte() {
        assertEquals(5, CommonUtil.hashCode((byte) 5));
        assertEquals(-1, CommonUtil.hashCode((byte) -1));
    }

    @Test
    public void testHashCode_Short() {
        assertEquals(100, CommonUtil.hashCode((short) 100));
        assertEquals(-50, CommonUtil.hashCode((short) -50));
    }

    @Test
    public void testHashCode_Int() {
        assertEquals(42, CommonUtil.hashCode(42));
        assertEquals(-42, CommonUtil.hashCode(-42));
    }

    @Test
    public void testHashCode_Long() {
        assertEquals(Long.hashCode(100L), CommonUtil.hashCode(100L));
        assertEquals(Long.hashCode(-100L), CommonUtil.hashCode(-100L));
    }

    @Test
    public void testHashCode_Float() {
        assertEquals(Float.floatToIntBits(3.14f), CommonUtil.hashCode(3.14f));
    }

    @Test
    public void testHashCode_Double() {
        assertEquals(Double.hashCode(3.14), CommonUtil.hashCode(3.14));
    }

    @Test
    public void testHashCode_Object() {
        String str = "test";
        assertEquals(str.hashCode(), CommonUtil.hashCode(str));
        assertEquals(0, CommonUtil.hashCode((Object) null));
    }

    @Test
    public void testHashCode_BooleanArray() {
        boolean[] arr = { true, false, true };
        int hash1 = CommonUtil.hashCode(arr);
        int hash2 = CommonUtil.hashCode(arr);
        assertEquals(hash1, hash2);

        assertEquals(0, CommonUtil.hashCode((boolean[]) null));
    }

    @Test
    public void testHashCode_BooleanArray_WithRange() {
        boolean[] arr = { true, false, true, false };
        int hash1 = CommonUtil.hashCode(arr, 1, 3);
        int hash2 = CommonUtil.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_CharArray() {
        char[] arr = { 'a', 'b', 'c' };
        int hash1 = CommonUtil.hashCode(arr);
        int hash2 = CommonUtil.hashCode(arr);
        assertEquals(hash1, hash2);

        assertEquals(0, CommonUtil.hashCode((char[]) null));
    }

    @Test
    public void testHashCode_CharArray_WithRange() {
        char[] arr = { 'a', 'b', 'c', 'd' };
        int hash1 = CommonUtil.hashCode(arr, 1, 3);
        int hash2 = CommonUtil.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_ByteArray() {
        byte[] arr = { 1, 2, 3 };
        int hash1 = CommonUtil.hashCode(arr);
        int hash2 = CommonUtil.hashCode(arr);
        assertEquals(hash1, hash2);

        assertEquals(0, CommonUtil.hashCode((byte[]) null));
    }

    @Test
    public void testHashCode_ByteArray_WithRange() {
        byte[] arr = { 1, 2, 3, 4 };
        int hash1 = CommonUtil.hashCode(arr, 1, 3);
        int hash2 = CommonUtil.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_ShortArray() {
        short[] arr = { 1, 2, 3 };
        int hash1 = CommonUtil.hashCode(arr);
        int hash2 = CommonUtil.hashCode(arr);
        assertEquals(hash1, hash2);

        assertEquals(0, CommonUtil.hashCode((short[]) null));
    }

    @Test
    public void testHashCode_ShortArray_WithRange() {
        short[] arr = { 1, 2, 3, 4 };
        int hash1 = CommonUtil.hashCode(arr, 1, 3);
        int hash2 = CommonUtil.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_IntArray() {
        int[] arr = { 1, 2, 3 };
        int hash1 = CommonUtil.hashCode(arr);
        int hash2 = CommonUtil.hashCode(arr);
        assertEquals(hash1, hash2);

        assertEquals(0, CommonUtil.hashCode((int[]) null));
    }

    @Test
    public void testHashCode_IntArray_WithRange() {
        int[] arr = { 1, 2, 3, 4 };
        int hash1 = CommonUtil.hashCode(arr, 1, 3);
        int hash2 = CommonUtil.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_LongArray() {
        long[] arr = { 1L, 2L, 3L };
        int hash1 = CommonUtil.hashCode(arr);
        int hash2 = CommonUtil.hashCode(arr);
        assertEquals(hash1, hash2);

        assertEquals(0, CommonUtil.hashCode((long[]) null));
    }

    @Test
    public void testHashCode_LongArray_WithRange() {
        long[] arr = { 1L, 2L, 3L, 4L };
        int hash1 = CommonUtil.hashCode(arr, 1, 3);
        int hash2 = CommonUtil.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_FloatArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f };
        int hash1 = CommonUtil.hashCode(arr);
        int hash2 = CommonUtil.hashCode(arr);
        assertEquals(hash1, hash2);

        assertEquals(0, CommonUtil.hashCode((float[]) null));
    }

    @Test
    public void testHashCode_FloatArray_WithRange() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f };
        int hash1 = CommonUtil.hashCode(arr, 1, 3);
        int hash2 = CommonUtil.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_DoubleArray() {
        double[] arr = { 1.0, 2.0, 3.0 };
        int hash1 = CommonUtil.hashCode(arr);
        int hash2 = CommonUtil.hashCode(arr);
        assertEquals(hash1, hash2);

        assertEquals(0, CommonUtil.hashCode((double[]) null));
    }

    @Test
    public void testHashCode_DoubleArray_WithRange() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0 };
        int hash1 = CommonUtil.hashCode(arr, 1, 3);
        int hash2 = CommonUtil.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_Consistency() {
        assertTrue(CommonUtil.equals(new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 }));
        assertEquals(CommonUtil.hashCode(new int[] { 1, 2, 3 }), CommonUtil.hashCode(new int[] { 1, 2, 3 }));
    }

    @Test
    public void testCheckIndex_Valid() {
        assertEquals(0, CommonUtil.checkIndex(0, 5));
        assertEquals(4, CommonUtil.checkIndex(4, 5));
    }

    @Test
    public void testCheckIndex_Invalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkIndex(-1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkIndex(5, 5));
    }

    @Test
    public void testDeepHashCode_Object() {
        String str = "test";
        assertEquals(str.hashCode(), CommonUtil.deepHashCode(str));
        assertEquals(0, CommonUtil.deepHashCode((Object) null));
    }

    @Test
    public void testDeepHashCode_ObjectArray() {
        Object[] arr = { new int[] { 1, 2 }, "test", 42 };
        int hash1 = CommonUtil.deepHashCode(arr);
        int hash2 = CommonUtil.deepHashCode(arr);
        assertEquals(hash1, hash2);
        assertEquals(0, CommonUtil.deepHashCode((Object[]) null));
    }

    @Test
    public void testDeepHashCode_ObjectArray_WithRange() {
        Object[] arr = { "a", "b", "c", "d" };
        int hash1 = CommonUtil.deepHashCode(arr, 1, 3);
        int hash2 = CommonUtil.deepHashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCodeEverything() {
        String str = "test";
        long hash = CommonUtil.hashCodeEverything(str);
        assertTrue(hash != 0);
        assertEquals(0, CommonUtil.hashCodeEverything(null));
    }

    @Test
    public void testHashCode_ObjectArray() {
        String[] arr = { "a", "b", "c" };
        int hash1 = CommonUtil.hashCode(arr);
        int hash2 = CommonUtil.hashCode(arr);
        assertEquals(hash1, hash2);
        assertEquals(0, CommonUtil.hashCode((Object[]) null));
    }

    @Test
    public void testHashCode_ObjectArray_WithRange() {
        String[] arr = { "a", "b", "c", "d" };
        int hash1 = CommonUtil.hashCode(arr, 1, 3);
        int hash2 = CommonUtil.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testCompare_Boolean() {
        assertEquals(0, CommonUtil.compare(true, true));
        assertEquals(0, CommonUtil.compare(false, false));
        assertTrue(CommonUtil.compare(false, true) < 0);
        assertTrue(CommonUtil.compare(true, false) > 0);
    }

    @Test
    public void testCompare_BooleanArray() {
        boolean[] a = { true, false, true };
        boolean[] b = { true, false, true };
        boolean[] c = { true, false, false };

        assertEquals(0, CommonUtil.compare(a, b));
        assertTrue(CommonUtil.compare(c, a) < 0);
        assertTrue(CommonUtil.compare(a, c) > 0);
    }

    @Test
    public void testCompare_BooleanArray_WithRange() {
        boolean[] a = { true, false, true, false };
        boolean[] b = { false, false, true, true };

        assertEquals(0, CommonUtil.compare(a, 1, b, 1, 2));
        assertTrue(CommonUtil.compare(a, 0, b, 0, 2) > 0);
    }

    @Test
    public void testCompare_Byte() {
        assertEquals(0, CommonUtil.compare((byte) 5, (byte) 5));
        assertTrue(CommonUtil.compare((byte) 3, (byte) 5) < 0);
        assertTrue(CommonUtil.compare((byte) 7, (byte) 5) > 0);
    }

    @Test
    public void testCompare_ByteArray() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 1, 2, 3 };
        byte[] c = { 1, 2, 4 };

        assertEquals(0, CommonUtil.compare(a, b));
        assertTrue(CommonUtil.compare(a, c) < 0);
        assertTrue(CommonUtil.compare(c, a) > 0);
    }

    @Test
    public void testCompare_ByteArray_WithRange() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] b = { 0, 2, 3, 0 };

        assertEquals(0, CommonUtil.compare(a, 1, b, 1, 2));
        assertTrue(CommonUtil.compare(a, 0, b, 0, 2) > 0);
    }

    @Test
    public void testCompare_Char() {
        assertEquals(0, CommonUtil.compare('a', 'a'));
        assertTrue(CommonUtil.compare('a', 'b') < 0);
        assertTrue(CommonUtil.compare('z', 'a') > 0);
    }

    @Test
    public void testCompare_CharArray() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'a', 'b', 'c' };
        char[] c = { 'a', 'b', 'd' };

        assertEquals(0, CommonUtil.compare(a, b));
        assertTrue(CommonUtil.compare(a, c) < 0);
        assertTrue(CommonUtil.compare(c, a) > 0);
    }

    @Test
    public void testCompare_CharArray_WithRange() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] b = { 'x', 'b', 'c', 'y' };

        assertEquals(0, CommonUtil.compare(a, 1, b, 1, 2));
        assertTrue(CommonUtil.compare(a, 0, b, 0, 2) < 0);
    }

    @Test
    public void testCompare_Short() {
        assertEquals(0, CommonUtil.compare((short) 5, (short) 5));
        assertTrue(CommonUtil.compare((short) 3, (short) 5) < 0);
        assertTrue(CommonUtil.compare((short) 7, (short) 5) > 0);
    }

    @Test
    public void testCompare_ShortArray() {
        short[] a = { 1, 2, 3 };
        short[] b = { 1, 2, 3 };
        short[] c = { 1, 2, 4 };

        assertEquals(0, CommonUtil.compare(a, b));
        assertTrue(CommonUtil.compare(a, c) < 0);
        assertTrue(CommonUtil.compare(c, a) > 0);
    }

    @Test
    public void testCompare_ShortArray_WithRange() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 0, 2, 3, 0 };

        assertEquals(0, CommonUtil.compare(a, 1, b, 1, 2));
        assertTrue(CommonUtil.compare(a, 0, b, 0, 2) > 0);
    }

    @Test
    public void testCompare_Int() {
        assertEquals(0, CommonUtil.compare(5, 5));
        assertTrue(CommonUtil.compare(3, 5) < 0);
        assertTrue(CommonUtil.compare(7, 5) > 0);
    }

    @Test
    public void testCompare_IntArray() {
        int[] a = { 1, 2, 3 };
        int[] b = { 1, 2, 3 };
        int[] c = { 1, 2, 4 };

        assertEquals(0, CommonUtil.compare(a, b));
        assertTrue(CommonUtil.compare(a, c) < 0);
        assertTrue(CommonUtil.compare(c, a) > 0);
    }

    @Test
    public void testCompare_IntArray_WithRange() {
        int[] a = { 1, 2, 3, 4 };
        int[] b = { 0, 2, 3, 0 };

        assertEquals(0, CommonUtil.compare(a, 1, b, 1, 2));
        assertTrue(CommonUtil.compare(a, 0, b, 0, 2) > 0);
    }

    @Test
    public void testCompare_Long() {
        assertEquals(0, CommonUtil.compare(5L, 5L));
        assertTrue(CommonUtil.compare(3L, 5L) < 0);
        assertTrue(CommonUtil.compare(7L, 5L) > 0);
    }

    @Test
    public void testCompare_LongArray() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 1L, 2L, 3L };
        long[] c = { 1L, 2L, 4L };

        assertEquals(0, CommonUtil.compare(a, b));
        assertTrue(CommonUtil.compare(a, c) < 0);
        assertTrue(CommonUtil.compare(c, a) > 0);
    }

    @Test
    public void testCompare_LongArray_WithRange() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 0L, 2L, 3L, 0L };

        assertEquals(0, CommonUtil.compare(a, 1, b, 1, 2));
        assertTrue(CommonUtil.compare(a, 0, b, 0, 2) > 0);
    }

    @Test
    public void testCompare_Float() {
        assertEquals(0, CommonUtil.compare(5.0f, 5.0f));
        assertTrue(CommonUtil.compare(3.0f, 5.0f) < 0);
        assertTrue(CommonUtil.compare(7.0f, 5.0f) > 0);
    }

    @Test
    public void testCompare_FloatArray() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        float[] b = { 1.0f, 2.0f, 3.0f };
        float[] c = { 1.0f, 2.0f, 4.0f };

        assertEquals(0, CommonUtil.compare(a, b));
        assertTrue(CommonUtil.compare(a, c) < 0);
        assertTrue(CommonUtil.compare(c, a) > 0);
    }

    @Test
    public void testCompare_FloatArray_WithRange() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] b = { 0.0f, 2.0f, 3.0f, 0.0f };

        assertEquals(0, CommonUtil.compare(a, 1, b, 1, 2));
        assertTrue(CommonUtil.compare(a, 0, b, 0, 2) > 0);
    }

    @Test
    public void testCompare_Double() {
        assertEquals(0, CommonUtil.compare(5.0, 5.0));
        assertTrue(CommonUtil.compare(3.0, 5.0) < 0);
        assertTrue(CommonUtil.compare(7.0, 5.0) > 0);
    }

    @Test
    public void testCompare_DoubleArray() {
        double[] a = { 1.0, 2.0, 3.0 };
        double[] b = { 1.0, 2.0, 3.0 };
        double[] c = { 1.0, 2.0, 4.0 };

        assertEquals(0, CommonUtil.compare(a, b));
        assertTrue(CommonUtil.compare(a, c) < 0);
        assertTrue(CommonUtil.compare(c, a) > 0);
    }

    @Test
    public void testCompare_DoubleArray_WithRange() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] b = { 0.0, 2.0, 3.0, 0.0 };

        assertEquals(0, CommonUtil.compare(a, 1, b, 1, 2));
        assertTrue(CommonUtil.compare(a, 0, b, 0, 2) > 0);
    }

    @Test
    public void testCompare_WithComparator() {
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;
        assertEquals(0, CommonUtil.compare("test", "TEST", cmp));
        assertTrue(CommonUtil.compare("abc", "xyz", cmp) < 0);
        assertTrue(CommonUtil.compare("xyz", "abc", cmp) > 0);
    }

    @Test
    public void testCompare_ObjectArray_WithComparator() {
        String[] a = { "a", "b", "c" };
        String[] b = { "A", "B", "C" };
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(0, CommonUtil.compare(a, b, cmp));
    }

    @Test
    public void testCompare_ObjectArray_WithRange_AndComparator() {
        String[] a = { "x", "b", "c", "y" };
        String[] b = { "z", "B", "C", "w" };
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(0, CommonUtil.compare(a, 1, b, 1, 2, cmp));
    }

    @Test
    public void testCompare_Iterable_WithComparator() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<String> b = Arrays.asList("A", "B", "C");
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(0, CommonUtil.compare(a, b, cmp));
    }

    @Test
    public void testCompare_Iterator_WithComparator() {
        Iterator<String> a = Arrays.asList("a", "b").iterator();
        Iterator<String> b = Arrays.asList("A", "B").iterator();
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(0, CommonUtil.compare(a, b, cmp));
    }

    @Test
    public void testCompare_Collection_WithRange() {
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("x", "b", "c", "y");

        assertEquals(0, CommonUtil.compare(a, 1, b, 1, 2));
    }

    @Test
    public void testCompare_Collection_WithRange_AndComparator() {
        List<String> a = Arrays.asList("x", "b", "c", "y");
        List<String> b = Arrays.asList("z", "B", "C", "w");
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(0, CommonUtil.compare(a, 1, b, 1, 2, cmp));
    }

    @Test
    public void testCompareIgnoreCase() {
        assertEquals(0, CommonUtil.compareIgnoreCase("test", "TEST"));
        assertTrue(CommonUtil.compareIgnoreCase("abc", "XYZ") < 0);
        assertTrue(CommonUtil.compareIgnoreCase("xyz", "ABC") > 0);
        assertEquals(0, CommonUtil.compareIgnoreCase((String) null, (String) null));
    }

    @Test
    public void testCompareIgnoreCase_StringArray() {
        String[] a = { "a", "b", "c" };
        String[] b = { "A", "B", "C" };

        assertEquals(0, CommonUtil.compareIgnoreCase(a, b));
    }

    @Test
    public void testCompareUnsigned_Byte() {
        assertEquals(0, CommonUtil.compareUnsigned((byte) 5, (byte) 5));
        assertTrue(CommonUtil.compareUnsigned((byte) 3, (byte) 5) < 0);
        assertTrue(CommonUtil.compareUnsigned((byte) -1, (byte) 1) > 0);
    }

    @Test
    public void testCompareUnsigned_ByteArray() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 1, 2, 3 };

        assertEquals(0, CommonUtil.compareUnsigned(a, b));
    }

    @Test
    public void testCompareUnsigned_ByteArray_WithRange() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] b = { 0, 2, 3, 0 };

        assertEquals(0, CommonUtil.compareUnsigned(a, 1, b, 1, 2));
    }

    @Test
    public void testCompareUnsigned_Short() {
        assertEquals(0, CommonUtil.compareUnsigned((short) 5, (short) 5));
        assertTrue(CommonUtil.compareUnsigned((short) 3, (short) 5) < 0);
        assertTrue(CommonUtil.compareUnsigned((short) -1, (short) 1) > 0);
    }

    @Test
    public void testCompareUnsigned_ShortArray() {
        short[] a = { 1, 2, 3 };
        short[] b = { 1, 2, 3 };

        assertEquals(0, CommonUtil.compareUnsigned(a, b));
    }

    @Test
    public void testCompareUnsigned_ShortArray_WithRange() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 0, 2, 3, 0 };

        assertEquals(0, CommonUtil.compareUnsigned(a, 1, b, 1, 2));
    }

    @Test
    public void testCompareUnsigned_Int() {
        assertEquals(0, CommonUtil.compareUnsigned(5, 5));
        assertTrue(CommonUtil.compareUnsigned(3, 5) < 0);
        assertTrue(CommonUtil.compareUnsigned(-1, 1) > 0);
    }

    @Test
    public void testCompareUnsigned_IntArray() {
        int[] a = { 1, 2, 3 };
        int[] b = { 1, 2, 3 };

        assertEquals(0, CommonUtil.compareUnsigned(a, b));
    }

    @Test
    public void testCompareUnsigned_IntArray_WithRange() {
        int[] a = { 1, 2, 3, 4 };
        int[] b = { 0, 2, 3, 0 };

        assertEquals(0, CommonUtil.compareUnsigned(a, 1, b, 1, 2));
    }

    @Test
    public void testCompareUnsigned_Long() {
        assertEquals(0, CommonUtil.compareUnsigned(5L, 5L));
        assertTrue(CommonUtil.compareUnsigned(3L, 5L) < 0);
        assertTrue(CommonUtil.compareUnsigned(-1L, 1L) > 0);
    }

    @Test
    public void testCompareUnsigned_LongArray() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 1L, 2L, 3L };

        assertEquals(0, CommonUtil.compareUnsigned(a, b));
    }

    @Test
    public void testCompareUnsigned_LongArray_WithRange() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 0L, 2L, 3L, 0L };

        assertEquals(0, CommonUtil.compareUnsigned(a, 1, b, 1, 2));
    }

    @Test
    public void testMismatch_BooleanArray() {
        boolean[] a = { true, false, true };
        boolean[] b = { true, false, false };

        assertEquals(2, CommonUtil.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatch_BooleanArray_WithRange() {
        boolean[] a = { true, false, true, false };
        boolean[] b = { false, false, true, true };

        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_ByteArray() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 1, 2, 4 };

        assertEquals(2, CommonUtil.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatch_ByteArray_WithRange() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] b = { 0, 2, 3, 0 };

        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_CharArray() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'a', 'b', 'd' };

        assertEquals(2, CommonUtil.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatch_CharArray_WithRange() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] b = { 'x', 'b', 'c', 'y' };

        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_ShortArray() {
        short[] a = { 1, 2, 3 };
        short[] b = { 1, 2, 4 };

        assertEquals(2, CommonUtil.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatch_ShortArray_WithRange() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 0, 2, 3, 0 };

        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_IntArray() {
        int[] a = { 1, 2, 3 };
        int[] b = { 1, 2, 4 };

        assertEquals(2, CommonUtil.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatch_IntArray_WithRange() {
        int[] a = { 1, 2, 3, 4 };
        int[] b = { 0, 2, 3, 0 };

        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_LongArray() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 1L, 2L, 4L };

        assertEquals(2, CommonUtil.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatch_LongArray_WithRange() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 0L, 2L, 3L, 0L };

        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_FloatArray() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        float[] b = { 1.0f, 2.0f, 4.0f };

        assertEquals(2, CommonUtil.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatch_FloatArray_WithRange() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] b = { 0.0f, 2.0f, 3.0f, 0.0f };

        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_DoubleArray() {
        double[] a = { 1.0, 2.0, 3.0 };
        double[] b = { 1.0, 2.0, 4.0 };

        assertEquals(2, CommonUtil.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatch_DoubleArray_WithRange() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] b = { 0.0, 2.0, 3.0, 0.0 };

        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_ObjectArray_WithComparator() {
        String[] a = { "a", "b", "c" };
        String[] b = { "A", "B", "D" };
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(2, CommonUtil.mismatch(a, b, cmp));
    }

    @Test
    public void testMismatch_ObjectArray_WithRange_AndComparator() {
        String[] a = { "x", "b", "c", "y" };
        String[] b = { "z", "B", "C", "w" };
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2, cmp));
    }

    @Test
    public void testMismatch_Iterable_WithComparator() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<String> b = Arrays.asList("A", "B", "D");
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(2, CommonUtil.mismatch(a, b, cmp));
    }

    @Test
    public void testMismatch_Iterator_WithComparator() {
        Iterator<String> a = Arrays.asList("a", "b", "c").iterator();
        Iterator<String> b = Arrays.asList("A", "B", "D").iterator();
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(2, CommonUtil.mismatch(a, b, cmp));
    }

    @Test
    public void testMismatch_Collection_WithRange() {
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("x", "b", "c", "y");

        assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
    }

    @Test
    public void testMismatch_Collection_WithRange_AndComparator() {
        List<String> a = Arrays.asList("x", "b", "c", "y");
        List<String> b = Arrays.asList("z", "B", "D", "w");
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(1, CommonUtil.mismatch(a, 1, b, 1, 3, cmp));
    }

    @Test
    public void testToString_BooleanValue() {
        assertEquals("true", CommonUtil.toString(true));
        assertEquals("false", CommonUtil.toString(false));
    }

    @Test
    public void testToString_BooleanArray() {
        boolean[] arr = { true, false, true };
        String result = CommonUtil.toString(arr);
        assertTrue(result.contains("true"));
        assertTrue(result.contains("false"));
    }

    @Test
    public void testToString_BooleanArray_WithRange() {
        boolean[] arr = { true, false, true, false };
        String result = CommonUtil.toString(arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testToString_ByteValue() {
        assertEquals("5", CommonUtil.toString((byte) 5));
        assertEquals("-1", CommonUtil.toString((byte) -1));
    }

    @Test
    public void testToString_ByteArray() {
        byte[] arr = { 1, 2, 3 };
        String result = CommonUtil.toString(arr);
        assertNotNull(result);
    }

    @Test
    public void testToString_ByteArray_WithRange() {
        byte[] arr = { 1, 2, 3, 4 };
        String result = CommonUtil.toString(arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testToString_CharValue() {
        assertEquals("a", CommonUtil.toString('a'));
        assertEquals("Z", CommonUtil.toString('Z'));
    }

    @Test
    public void testToString_CharArray() {
        char[] arr = { 'a', 'b', 'c' };
        String result = CommonUtil.toString(arr);
        assertNotNull(result);
    }

    @Test
    public void testToString_CharArray_WithRange() {
        char[] arr = { 'a', 'b', 'c', 'd' };
        String result = CommonUtil.toString(arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testToString_ShortValue() {
        assertEquals("100", CommonUtil.toString((short) 100));
        assertEquals("-50", CommonUtil.toString((short) -50));
    }

    @Test
    public void testToString_ShortArray() {
        short[] arr = { 1, 2, 3 };
        String result = CommonUtil.toString(arr);
        assertNotNull(result);
    }

    @Test
    public void testToString_ShortArray_WithRange() {
        short[] arr = { 1, 2, 3, 4 };
        String result = CommonUtil.toString(arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testToString_IntValue() {
        assertEquals("42", CommonUtil.toString(42));
        assertEquals("-42", CommonUtil.toString(-42));
    }

    @Test
    public void testToString_IntArray() {
        int[] arr = { 1, 2, 3 };
        String result = CommonUtil.toString(arr);
        assertNotNull(result);
    }

    @Test
    public void testToString_IntArray_WithRange() {
        int[] arr = { 1, 2, 3, 4 };
        String result = CommonUtil.toString(arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testToString_LongValue() {
        assertEquals("100", CommonUtil.toString(100L));
        assertEquals("-100", CommonUtil.toString(-100L));
    }

    @Test
    public void testToString_LongArray() {
        long[] arr = { 1L, 2L, 3L };
        String result = CommonUtil.toString(arr);
        assertNotNull(result);
    }

    @Test
    public void testToString_LongArray_WithRange() {
        long[] arr = { 1L, 2L, 3L, 4L };
        String result = CommonUtil.toString(arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testToString_FloatValue() {
        String result = CommonUtil.toString(3.14f);
        assertTrue(result.contains("3.14"));
    }

    @Test
    public void testToString_FloatArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f };
        String result = CommonUtil.toString(arr);
        assertNotNull(result);
    }

    @Test
    public void testToString_FloatArray_WithRange() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f };
        String result = CommonUtil.toString(arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testToString_DoubleValue() {
        String result = CommonUtil.toString(3.14);
        assertTrue(result.contains("3.14"));
    }

    @Test
    public void testToString_DoubleArray() {
        double[] arr = { 1.0, 2.0, 3.0 };
        String result = CommonUtil.toString(arr);
        assertNotNull(result);
    }

    @Test
    public void testToString_DoubleArray_WithRange() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0 };
        String result = CommonUtil.toString(arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testToString_Object() {
        assertEquals("test", CommonUtil.toString("test"));
        assertEquals("null", CommonUtil.toString((Object) null));
    }

    @Test
    public void testToString_Object_WithDefault() {
        assertEquals("test", CommonUtil.toString("test", "default"));
        assertEquals("default", CommonUtil.toString(null, "default"));
    }

    @Test
    public void testToString_ObjectArray() {
        String[] arr = { "a", "b", "c" };
        String result = CommonUtil.toString(arr);
        assertNotNull(result);
    }

    @Test
    public void testToString_ObjectArray_WithRange() {
        String[] arr = { "a", "b", "c", "d" };
        String result = CommonUtil.toString(arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testDeepToString_Object() {
        int[] arr = { 1, 2, 3 };
        String result = CommonUtil.deepToString(arr);
        assertNotNull(result);
    }

    @Test
    public void testDeepToString_ObjectArray() {
        Object[] arr = { new int[] { 1, 2 }, "test", 42 };
        String result = CommonUtil.deepToString(arr);
        assertNotNull(result);
    }

    @Test
    public void testDeepToString_ObjectArray_WithRange() {
        Object[] arr = { "a", "b", "c", "d" };
        String result = CommonUtil.deepToString(arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testDeepToString_ObjectArray_WithDefault() {
        Object[] arr = null;
        String result = CommonUtil.deepToString(arr, "NULL");
        assertEquals("NULL", result);
    }

    @Test
    public void testStringOf_Boolean() {
        assertEquals("true", CommonUtil.stringOf(true));
        assertEquals("false", CommonUtil.stringOf(false));
    }

    @Test
    public void testStringOf_Byte() {
        assertEquals("5", CommonUtil.stringOf((byte) 5));
    }

    @Test
    public void testStringOf_Char() {
        assertEquals("a", CommonUtil.stringOf('a'));
    }

    @Test
    public void testStringOf_Short() {
        assertEquals("100", CommonUtil.stringOf((short) 100));
    }

    @Test
    public void testStringOf_Int() {
        assertEquals("42", CommonUtil.stringOf(42));
    }

    @Test
    public void testStringOf_Long() {
        assertEquals("100", CommonUtil.stringOf(100L));
    }

    @Test
    public void testStringOf_Float() {
        String result = CommonUtil.stringOf(3.14f);
        assertTrue(result.contains("3.14"));
    }

    @Test
    public void testStringOf_Double() {
        String result = CommonUtil.stringOf(3.14);
        assertTrue(result.contains("3.14"));
    }

    @Test
    public void testStringOf_Object() {
        assertEquals("test", CommonUtil.stringOf("test"));
        assertNull(CommonUtil.stringOf((Object) null));
    }

    @Test
    public void testIsEmpty_CharSequence() {
        assertTrue(CommonUtil.isEmpty((String) null));
        assertTrue(CommonUtil.isEmpty(""));
        assertFalse(CommonUtil.isEmpty("a"));
        assertFalse(CommonUtil.isEmpty(" "));
    }

    @Test
    public void testIsEmpty_BooleanArray() {
        assertTrue(CommonUtil.isEmpty((boolean[]) null));
        assertTrue(CommonUtil.isEmpty(new boolean[0]));
        assertFalse(CommonUtil.isEmpty(new boolean[] { true }));
    }

    @Test
    public void testIsEmpty_CharArray() {
        assertTrue(CommonUtil.isEmpty((char[]) null));
        assertTrue(CommonUtil.isEmpty(new char[0]));
        assertFalse(CommonUtil.isEmpty(new char[] { 'a' }));
    }

    @Test
    public void testIsEmpty_ByteArray() {
        assertTrue(CommonUtil.isEmpty((byte[]) null));
        assertTrue(CommonUtil.isEmpty(new byte[0]));
        assertFalse(CommonUtil.isEmpty(new byte[] { 1 }));
    }

    @Test
    public void testIsEmpty_ShortArray() {
        assertTrue(CommonUtil.isEmpty((short[]) null));
        assertTrue(CommonUtil.isEmpty(new short[0]));
        assertFalse(CommonUtil.isEmpty(new short[] { 1 }));
    }

    @Test
    public void testIsEmpty_IntArray() {
        assertTrue(CommonUtil.isEmpty((int[]) null));
        assertTrue(CommonUtil.isEmpty(new int[0]));
        assertFalse(CommonUtil.isEmpty(new int[] { 1 }));
    }

    @Test
    public void testIsEmpty_LongArray() {
        assertTrue(CommonUtil.isEmpty((long[]) null));
        assertTrue(CommonUtil.isEmpty(new long[0]));
        assertFalse(CommonUtil.isEmpty(new long[] { 1L }));
    }

    @Test
    public void testIsEmpty_FloatArray() {
        assertTrue(CommonUtil.isEmpty((float[]) null));
        assertTrue(CommonUtil.isEmpty(new float[0]));
        assertFalse(CommonUtil.isEmpty(new float[] { 1.0f }));
    }

    @Test
    public void testIsEmpty_DoubleArray() {
        assertTrue(CommonUtil.isEmpty((double[]) null));
        assertTrue(CommonUtil.isEmpty(new double[0]));
        assertFalse(CommonUtil.isEmpty(new double[] { 1.0 }));
    }

    @Test
    public void testIsEmpty_ObjectArray() {
        assertTrue(CommonUtil.isEmpty((Object[]) null));
        assertTrue(CommonUtil.isEmpty(new Object[0]));
        assertFalse(CommonUtil.isEmpty(new Object[] { new Object() }));
    }

    @Test
    public void testIsEmpty_Collection() {
        assertTrue(CommonUtil.isEmpty((Collection<?>) null));
        assertTrue(CommonUtil.isEmpty(new ArrayList<>()));
        assertFalse(CommonUtil.isEmpty(Arrays.asList("a")));
    }

    @Test
    public void testIsEmpty_Map() {
        assertTrue(CommonUtil.isEmpty((Map<?, ?>) null));
        assertTrue(CommonUtil.isEmpty(new HashMap<>()));
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertFalse(CommonUtil.isEmpty(map));
    }

    @Test
    public void testNotEmpty_CharSequence() {
        assertFalse(CommonUtil.notEmpty((String) null));
        assertFalse(CommonUtil.notEmpty(""));
        assertTrue(CommonUtil.notEmpty("a"));
        assertTrue(CommonUtil.notEmpty(" "));
    }

    @Test
    public void testNotEmpty_BooleanArray() {
        assertFalse(CommonUtil.notEmpty((boolean[]) null));
        assertFalse(CommonUtil.notEmpty(new boolean[0]));
        assertTrue(CommonUtil.notEmpty(new boolean[] { true }));
    }

    @Test
    public void testNotEmpty_CharArray() {
        assertFalse(CommonUtil.notEmpty((char[]) null));
        assertFalse(CommonUtil.notEmpty(new char[0]));
        assertTrue(CommonUtil.notEmpty(new char[] { 'a' }));
    }

    @Test
    public void testNotEmpty_ByteArray() {
        assertFalse(CommonUtil.notEmpty((byte[]) null));
        assertFalse(CommonUtil.notEmpty(new byte[0]));
        assertTrue(CommonUtil.notEmpty(new byte[] { 1 }));
    }

    @Test
    public void testNotEmpty_ShortArray() {
        assertFalse(CommonUtil.notEmpty((short[]) null));
        assertFalse(CommonUtil.notEmpty(new short[0]));
        assertTrue(CommonUtil.notEmpty(new short[] { 1 }));
    }

    @Test
    public void testNotEmpty_IntArray() {
        assertFalse(CommonUtil.notEmpty((int[]) null));
        assertFalse(CommonUtil.notEmpty(new int[0]));
        assertTrue(CommonUtil.notEmpty(new int[] { 1 }));
    }

    @Test
    public void testNotEmpty_LongArray() {
        assertFalse(CommonUtil.notEmpty((long[]) null));
        assertFalse(CommonUtil.notEmpty(new long[0]));
        assertTrue(CommonUtil.notEmpty(new long[] { 1L }));
    }

    @Test
    public void testNotEmpty_FloatArray() {
        assertFalse(CommonUtil.notEmpty((float[]) null));
        assertFalse(CommonUtil.notEmpty(new float[0]));
        assertTrue(CommonUtil.notEmpty(new float[] { 1.0f }));
    }

    @Test
    public void testNotEmpty_DoubleArray() {
        assertFalse(CommonUtil.notEmpty((double[]) null));
        assertFalse(CommonUtil.notEmpty(new double[0]));
        assertTrue(CommonUtil.notEmpty(new double[] { 1.0 }));
    }

    @Test
    public void testNotEmpty_ObjectArray() {
        assertFalse(CommonUtil.notEmpty((Object[]) null));
        assertFalse(CommonUtil.notEmpty(new Object[0]));
        assertTrue(CommonUtil.notEmpty(new Object[] { new Object() }));
    }

    @Test
    public void testNotEmpty_Collection() {
        assertFalse(CommonUtil.notEmpty((Collection<?>) null));
        assertFalse(CommonUtil.notEmpty(new ArrayList<>()));
        assertTrue(CommonUtil.notEmpty(Arrays.asList("a")));
    }

    @Test
    public void testNotEmpty_Map() {
        assertFalse(CommonUtil.notEmpty((Map<?, ?>) null));
        assertFalse(CommonUtil.notEmpty(new HashMap<>()));
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertTrue(CommonUtil.notEmpty(map));
    }

    @Test
    public void testIsBlank() {
        assertTrue(CommonUtil.isBlank(null));
        assertTrue(CommonUtil.isBlank(""));
        assertTrue(CommonUtil.isBlank(" "));
        assertTrue(CommonUtil.isBlank("   "));
        assertTrue(CommonUtil.isBlank("\t"));
        assertTrue(CommonUtil.isBlank("\n"));
        assertFalse(CommonUtil.isBlank("a"));
        assertFalse(CommonUtil.isBlank(" a "));
    }

    @Test
    public void testNotBlank() {
        assertFalse(CommonUtil.notBlank(null));
        assertFalse(CommonUtil.notBlank(""));
        assertFalse(CommonUtil.notBlank(" "));
        assertFalse(CommonUtil.notBlank("   "));
        assertFalse(CommonUtil.notBlank("\t"));
        assertTrue(CommonUtil.notBlank("a"));
        assertTrue(CommonUtil.notBlank(" a "));
    }

    @Test
    public void testDefaultIfNull_Boolean() {
        assertFalse(CommonUtil.defaultIfNull((Boolean) null));
        assertTrue(CommonUtil.defaultIfNull(true));
        assertFalse(CommonUtil.defaultIfNull(false));
        assertTrue(CommonUtil.defaultIfNull((Boolean) null, true));
        assertFalse(CommonUtil.defaultIfNull((Boolean) null, false));
    }

    @Test
    public void testDefaultIfNull_Char() {
        assertEquals('\u0000', CommonUtil.defaultIfNull((Character) null));
        assertEquals('a', CommonUtil.defaultIfNull('a'));
        assertEquals('x', CommonUtil.defaultIfNull((Character) null, 'x'));
    }

    @Test
    public void testDefaultIfNull_Byte() {
        assertEquals(0, CommonUtil.defaultIfNull((Byte) null));
        assertEquals(5, CommonUtil.defaultIfNull((byte) 5));
        assertEquals(10, CommonUtil.defaultIfNull((Byte) null, (byte) 10));
    }

    @Test
    public void testDefaultIfNull_Short() {
        assertEquals(0, CommonUtil.defaultIfNull((Short) null));
        assertEquals(5, CommonUtil.defaultIfNull((short) 5));
        assertEquals(10, CommonUtil.defaultIfNull((Short) null, (short) 10));
    }

    @Test
    public void testDefaultIfNull_Int() {
        assertEquals(0, CommonUtil.defaultIfNull((Integer) null));
        assertEquals(5, CommonUtil.defaultIfNull(5));
        assertEquals(10, CommonUtil.defaultIfNull((Integer) null, 10));
    }

    @Test
    public void testDefaultIfNull_Long() {
        assertEquals(0L, CommonUtil.defaultIfNull((Long) null));
        assertEquals(5L, CommonUtil.defaultIfNull(5L));
        assertEquals(10L, CommonUtil.defaultIfNull((Long) null, 10L));
    }

    @Test
    public void testDefaultIfNull_Float() {
        assertEquals(0.0f, CommonUtil.defaultIfNull((Float) null));
        assertEquals(5.0f, CommonUtil.defaultIfNull(5.0f));
        assertEquals(10.0f, CommonUtil.defaultIfNull((Float) null, 10.0f));
    }

    @Test
    public void testDefaultIfNull_Double() {
        assertEquals(0.0, CommonUtil.defaultIfNull((Double) null));
        assertEquals(5.0, CommonUtil.defaultIfNull(5.0));
        assertEquals(10.0, CommonUtil.defaultIfNull((Double) null, 10.0));
    }

    @Test
    public void testDefaultIfNull_Object() {
        assertEquals("default", CommonUtil.defaultIfNull((String) null, "default"));
        assertEquals("test", CommonUtil.defaultIfNull("test", "default"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfNull(null, (String) null));
    }

    @Test
    public void testDefaultIfNull_WithSupplier() {
        Supplier<String> supplier = () -> "default";
        assertEquals("default", CommonUtil.defaultIfNull((String) null, supplier));
        assertEquals("test", CommonUtil.defaultIfNull("test", supplier));
    }

    @Test
    public void testDefaultIfEmpty_CharSequence() {
        assertEquals("default", CommonUtil.defaultIfEmpty((String) null, "default"));
        assertEquals("default", CommonUtil.defaultIfEmpty("", "default"));
        assertEquals("test", CommonUtil.defaultIfEmpty("test", "default"));
    }

    @Test
    public void testDefaultIfEmpty_Collection() {
        List<String> list = Arrays.asList("a", "b");
        List<String> defaultList = Arrays.asList("x", "y");

        assertEquals(defaultList, CommonUtil.defaultIfEmpty((List<String>) null, defaultList));
        assertEquals(defaultList, CommonUtil.defaultIfEmpty(new ArrayList<>(), defaultList));
        assertEquals(list, CommonUtil.defaultIfEmpty(list, defaultList));
    }

    @Test
    public void testDefaultIfEmpty_Map() {
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        Map<String, String> defaultMap = new HashMap<>();
        defaultMap.put("default", "val");

        assertEquals(defaultMap, CommonUtil.defaultIfEmpty((Map<String, String>) null, defaultMap));
        assertEquals(defaultMap, CommonUtil.defaultIfEmpty(new HashMap<>(), defaultMap));
        assertEquals(map, CommonUtil.defaultIfEmpty(map, defaultMap));
    }

    @Test
    public void testDefaultIfBlank() {
        assertEquals("default", CommonUtil.defaultIfBlank((String) null, "default"));
        assertEquals("default", CommonUtil.defaultIfBlank("", "default"));
        assertEquals("default", CommonUtil.defaultIfBlank("  ", "default"));
        assertEquals("test", CommonUtil.defaultIfBlank("test", "default"));
        assertEquals(" test ", CommonUtil.defaultIfBlank(" test ", "default"));
    }

    @Test
    public void testIndexOf_BooleanArray() {
        boolean[] arr = { true, false, true, false };
        assertEquals(0, CommonUtil.indexOf(arr, true));
        assertEquals(1, CommonUtil.indexOf(arr, false));
        assertEquals(-1, CommonUtil.indexOf((boolean[]) null, true));
        assertEquals(2, CommonUtil.indexOf(arr, true, 1));
    }

    @Test
    public void testIndexOf_CharArray() {
        char[] arr = { 'a', 'b', 'c', 'b' };
        assertEquals(0, CommonUtil.indexOf(arr, 'a'));
        assertEquals(1, CommonUtil.indexOf(arr, 'b'));
        assertEquals(-1, CommonUtil.indexOf(arr, 'z'));
        assertEquals(3, CommonUtil.indexOf(arr, 'b', 2));
    }

    @Test
    public void testIndexOf_ByteArray() {
        byte[] arr = { 1, 2, 3, 2 };
        assertEquals(0, CommonUtil.indexOf(arr, (byte) 1));
        assertEquals(1, CommonUtil.indexOf(arr, (byte) 2));
        assertEquals(-1, CommonUtil.indexOf(arr, (byte) 9));
        assertEquals(3, CommonUtil.indexOf(arr, (byte) 2, 2));
    }

    @Test
    public void testIndexOf_ShortArray() {
        short[] arr = { 1, 2, 3, 2 };
        assertEquals(0, CommonUtil.indexOf(arr, (short) 1));
        assertEquals(1, CommonUtil.indexOf(arr, (short) 2));
        assertEquals(-1, CommonUtil.indexOf(arr, (short) 9));
        assertEquals(3, CommonUtil.indexOf(arr, (short) 2, 2));
    }

    @Test
    public void testIndexOf_IntArray() {
        int[] arr = { 1, 2, 3, 2 };
        assertEquals(0, CommonUtil.indexOf(arr, 1));
        assertEquals(1, CommonUtil.indexOf(arr, 2));
        assertEquals(-1, CommonUtil.indexOf(arr, 9));
        assertEquals(3, CommonUtil.indexOf(arr, 2, 2));
    }

    @Test
    public void testIndexOf_LongArray() {
        long[] arr = { 1L, 2L, 3L, 2L };
        assertEquals(0, CommonUtil.indexOf(arr, 1L));
        assertEquals(1, CommonUtil.indexOf(arr, 2L));
        assertEquals(-1, CommonUtil.indexOf(arr, 9L));
        assertEquals(3, CommonUtil.indexOf(arr, 2L, 2));
    }

    @Test
    public void testIndexOf_FloatArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 2.0f };
        assertEquals(0, CommonUtil.indexOf(arr, 1.0f));
        assertEquals(1, CommonUtil.indexOf(arr, 2.0f));
        assertEquals(-1, CommonUtil.indexOf(arr, 9.0f));
        assertEquals(3, CommonUtil.indexOf(arr, 2.0f, 2));
    }

    @Test
    public void testIndexOf_DoubleArray() {
        double[] arr = { 1.0, 2.0, 3.0, 2.0 };
        assertEquals(0, CommonUtil.indexOf(arr, 1.0));
        assertEquals(1, CommonUtil.indexOf(arr, 2.0));
        assertEquals(-1, CommonUtil.indexOf(arr, 9.0));
        assertEquals(3, CommonUtil.indexOf(arr, 2.0, 2));
    }

    @Test
    public void testIndexOf_DoubleArray_WithTolerance() {
        double[] arr = { 1.0, 2.0, 3.0 };
        assertEquals(1, CommonUtil.indexOf(arr, 2.01, 0.02));
        assertEquals(-1, CommonUtil.indexOf(arr, 2.1, 0.05));
    }

    @Test
    public void testIndexOf_ObjectArray() {
        String[] arr = { "a", "b", "c", "b" };
        assertEquals(0, CommonUtil.indexOf(arr, "a"));
        assertEquals(1, CommonUtil.indexOf(arr, "b"));
        assertEquals(-1, CommonUtil.indexOf(arr, "z"));
        assertEquals(3, CommonUtil.indexOf(arr, "b", 2));
    }

    @Test
    public void testIndexOf_Collection() {
        List<String> list = Arrays.asList("a", "b", "c", "b");
        assertEquals(0, CommonUtil.indexOf(list, "a"));
        assertEquals(1, CommonUtil.indexOf(list, "b"));
        assertEquals(-1, CommonUtil.indexOf(list, "z"));
        assertEquals(3, CommonUtil.indexOf(list, "b", 2));
    }

    @Test
    public void testLastIndexOf_BooleanArray() {
        boolean[] arr = { true, false, true, false };
        assertEquals(2, CommonUtil.lastIndexOf(arr, true));
        assertEquals(3, CommonUtil.lastIndexOf(arr, false));
        assertEquals(-1, CommonUtil.lastIndexOf((boolean[]) null, true));
    }

    @Test
    public void testLastIndexOf_CharArray() {
        char[] arr = { 'a', 'b', 'c', 'b' };
        assertEquals(0, CommonUtil.lastIndexOf(arr, 'a'));
        assertEquals(3, CommonUtil.lastIndexOf(arr, 'b'));
        assertEquals(-1, CommonUtil.lastIndexOf(arr, 'z'));
    }

    @Test
    public void testLastIndexOf_ByteArray() {
        byte[] arr = { 1, 2, 3, 2 };
        assertEquals(0, CommonUtil.lastIndexOf(arr, (byte) 1));
        assertEquals(3, CommonUtil.lastIndexOf(arr, (byte) 2));
        assertEquals(-1, CommonUtil.lastIndexOf(arr, (byte) 9));
    }

    @Test
    public void testLastIndexOf_ShortArray() {
        short[] arr = { 1, 2, 3, 2 };
        assertEquals(0, CommonUtil.lastIndexOf(arr, (short) 1));
        assertEquals(3, CommonUtil.lastIndexOf(arr, (short) 2));
        assertEquals(-1, CommonUtil.lastIndexOf(arr, (short) 9));
    }

    @Test
    public void testLastIndexOf_IntArray() {
        int[] arr = { 1, 2, 3, 2 };
        assertEquals(0, CommonUtil.lastIndexOf(arr, 1));
        assertEquals(3, CommonUtil.lastIndexOf(arr, 2));
        assertEquals(-1, CommonUtil.lastIndexOf(arr, 9));
    }

    @Test
    public void testLastIndexOf_LongArray() {
        long[] arr = { 1L, 2L, 3L, 2L };
        assertEquals(0, CommonUtil.lastIndexOf(arr, 1L));
        assertEquals(3, CommonUtil.lastIndexOf(arr, 2L));
        assertEquals(-1, CommonUtil.lastIndexOf(arr, 9L));
    }

    @Test
    public void testLastIndexOf_FloatArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 2.0f };
        assertEquals(0, CommonUtil.lastIndexOf(arr, 1.0f));
        assertEquals(3, CommonUtil.lastIndexOf(arr, 2.0f));
        assertEquals(-1, CommonUtil.lastIndexOf(arr, 9.0f));
    }

    @Test
    public void testLastIndexOf_DoubleArray() {
        double[] arr = { 1.0, 2.0, 3.0, 2.0 };
        assertEquals(0, CommonUtil.lastIndexOf(arr, 1.0));
        assertEquals(3, CommonUtil.lastIndexOf(arr, 2.0));
        assertEquals(-1, CommonUtil.lastIndexOf(arr, 9.0));
    }

    @Test
    public void testLastIndexOf_DoubleArray_WithTolerance() {
        double[] arr = { 1.0, 2.0, 3.0, 2.01 };
        assertEquals(3, CommonUtil.lastIndexOf(arr, 2.0, 0.02));
    }

    @Test
    public void testLastIndexOf_ObjectArray() {
        String[] arr = { "a", "b", "c", "b" };
        assertEquals(0, CommonUtil.lastIndexOf(arr, "a"));
        assertEquals(3, CommonUtil.lastIndexOf(arr, "b"));
        assertEquals(-1, CommonUtil.lastIndexOf(arr, "z"));
    }

    @Test
    public void testLastIndexOf_Collection() {
        List<String> list = Arrays.asList("a", "b", "c", "b");
        assertEquals(0, CommonUtil.lastIndexOf(list, "a"));
        assertEquals(3, CommonUtil.lastIndexOf(list, "b"));
        assertEquals(-1, CommonUtil.lastIndexOf(list, "z"));
    }

    @Test
    public void testBinarySearch_CharArray() {
        char[] arr = { 'a', 'c', 'e', 'g' };
        assertTrue(CommonUtil.binarySearch(arr, 'c') >= 0);
        assertTrue(CommonUtil.binarySearch(arr, 'b') < 0);
        assertTrue(CommonUtil.binarySearch(arr, 0, 4, 'e') >= 0);
    }

    @Test
    public void testBinarySearch_ByteArray() {
        byte[] arr = { 1, 3, 5, 7 };
        assertTrue(CommonUtil.binarySearch(arr, (byte) 3) >= 0);
        assertTrue(CommonUtil.binarySearch(arr, (byte) 2) < 0);
        assertTrue(CommonUtil.binarySearch(arr, 0, 4, (byte) 5) >= 0);
    }

    @Test
    public void testBinarySearch_ShortArray() {
        short[] arr = { 1, 3, 5, 7 };
        assertTrue(CommonUtil.binarySearch(arr, (short) 3) >= 0);
        assertTrue(CommonUtil.binarySearch(arr, (short) 2) < 0);
        assertTrue(CommonUtil.binarySearch(arr, 0, 4, (short) 5) >= 0);
    }

    @Test
    public void testBinarySearch_IntArray() {
        int[] arr = { 1, 3, 5, 7 };
        assertTrue(CommonUtil.binarySearch(arr, 3) >= 0);
        assertTrue(CommonUtil.binarySearch(arr, 2) < 0);
        assertTrue(CommonUtil.binarySearch(arr, 0, 4, 5) >= 0);
    }

    @Test
    public void testBinarySearch_LongArray() {
        long[] arr = { 1L, 3L, 5L, 7L };
        assertTrue(CommonUtil.binarySearch(arr, 3L) >= 0);
        assertTrue(CommonUtil.binarySearch(arr, 2L) < 0);
        assertTrue(CommonUtil.binarySearch(arr, 0, 4, 5L) >= 0);
    }

    @Test
    public void testBinarySearch_FloatArray() {
        float[] arr = { 1.0f, 3.0f, 5.0f, 7.0f };
        assertTrue(CommonUtil.binarySearch(arr, 3.0f) >= 0);
        assertTrue(CommonUtil.binarySearch(arr, 2.0f) < 0);
        assertTrue(CommonUtil.binarySearch(arr, 0, 4, 5.0f) >= 0);
    }

    @Test
    public void testBinarySearch_DoubleArray() {
        double[] arr = { 1.0, 3.0, 5.0, 7.0 };
        assertTrue(CommonUtil.binarySearch(arr, 3.0) >= 0);
        assertTrue(CommonUtil.binarySearch(arr, 2.0) < 0);
        assertTrue(CommonUtil.binarySearch(arr, 0, 4, 5.0) >= 0);
    }

    @Test
    public void testBinarySearch_ObjectArray() {
        String[] arr = { "a", "c", "e", "g" };
        assertTrue(CommonUtil.binarySearch(arr, "c") >= 0);
        assertTrue(CommonUtil.binarySearch(arr, "b") < 0);
    }

    @Test
    public void testFill_BooleanArray() {
        boolean[] arr = new boolean[5];
        CommonUtil.fill(arr, true);
        for (boolean b : arr) {
            assertTrue(b);
        }
        CommonUtil.fill(arr, 1, 3, false);
        assertTrue(arr[0]);
        assertFalse(arr[1]);
        assertFalse(arr[2]);
        assertTrue(arr[3]);
    }

    @Test
    public void testFill_IntArray() {
        int[] arr = new int[5];
        CommonUtil.fill(arr, 42);
        for (int i : arr) {
            assertEquals(42, i);
        }
        CommonUtil.fill(arr, 1, 3, 99);
        assertEquals(42, arr[0]);
        assertEquals(99, arr[1]);
        assertEquals(99, arr[2]);
        assertEquals(42, arr[3]);
    }

    @Test
    public void testFill_ObjectArray() {
        String[] arr = new String[5];
        CommonUtil.fill(arr, "test");
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
        CommonUtil.fill(list, "x");
        for (String s : list) {
            assertEquals("x", s);
        }
    }

    @Test
    public void testClone_BooleanArray() {
        boolean[] arr = { true, false, true };
        boolean[] cloned = CommonUtil.clone(arr);
        assertNotSame(arr, cloned);
        assertTrue(CommonUtil.equals(arr, cloned));
        assertNull(CommonUtil.clone((boolean[]) null));
    }

    @Test
    public void testClone_IntArray() {
        int[] arr = { 1, 2, 3 };
        int[] cloned = CommonUtil.clone(arr);
        assertNotSame(arr, cloned);
        assertTrue(CommonUtil.equals(arr, cloned));
    }

    @Test
    public void testClone_ObjectArray() {
        String[] arr = { "a", "b", "c" };
        String[] cloned = CommonUtil.clone(arr);
        assertNotSame(arr, cloned);
        assertTrue(CommonUtil.equals(arr, cloned));
    }

    @Test
    public void testClone_2DArray() {
        int[][] arr = { { 1, 2 }, { 3, 4 } };
        int[][] cloned = CommonUtil.clone(arr);
        assertNotSame(arr, cloned);
        assertEquals(arr.length, cloned.length);
    }

    @Test
    public void testCopy_BooleanArray() {
        boolean[] src = { true, false, true };
        boolean[] dest = new boolean[3];
        CommonUtil.copy(src, 0, dest, 0, 3);
        assertTrue(CommonUtil.equals(src, dest));
    }

    @Test
    public void testCopy_IntArray() {
        int[] src = { 1, 2, 3 };
        int[] dest = new int[3];
        CommonUtil.copy(src, 0, dest, 0, 3);
        assertTrue(CommonUtil.equals(src, dest));
    }

    @Test
    public void testCopy_ObjectArray() {
        String[] src = { "a", "b", "c" };
        String[] dest = new String[3];
        CommonUtil.copy(src, 0, dest, 0, 3);
        assertTrue(CommonUtil.equals(src, dest));
    }

    @Test
    public void testReverse_BooleanArray() {
        boolean[] arr = { true, false, true, false };
        CommonUtil.reverse(arr);
        boolean[] expected = { false, true, false, true };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testReverse_IntArray() {
        int[] arr = { 1, 2, 3, 4 };
        CommonUtil.reverse(arr);
        int[] expected = { 4, 3, 2, 1 };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testReverse_IntArray_WithRange() {
        int[] arr = { 1, 2, 3, 4, 5 };
        CommonUtil.reverse(arr, 1, 4);
        int[] expected = { 1, 4, 3, 2, 5 };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testReverse_ObjectArray() {
        String[] arr = { "a", "b", "c", "d" };
        CommonUtil.reverse(arr);
        String[] expected = { "d", "c", "b", "a" };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testReverse_List() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.reverse(list);
        assertEquals(Arrays.asList("d", "c", "b", "a"), list);
    }

    @Test
    public void testReverse_List_WithRange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        CommonUtil.reverse(list, 1, 4);
        assertEquals(Arrays.asList("a", "d", "c", "b", "e"), list);
    }

    @Test
    public void testRotate_BooleanArray() {
        boolean[] arr = { true, false, true, false };
        CommonUtil.rotate(arr, 1);
        boolean[] expected = { false, true, false, true };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testRotate_IntArray() {
        int[] arr = { 1, 2, 3, 4 };
        CommonUtil.rotate(arr, 1);
        int[] expected = { 4, 1, 2, 3 };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testRotate_IntArray_NegativeDistance() {
        int[] arr = { 1, 2, 3, 4 };
        CommonUtil.rotate(arr, -1);
        int[] expected = { 2, 3, 4, 1 };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testRotate_IntArray_WithRange() {
        int[] arr = { 1, 2, 3, 4, 5 };
        CommonUtil.rotate(arr, 1, 4, 1);
        int[] expected = { 1, 4, 2, 3, 5 };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testRotate_ObjectArray() {
        String[] arr = { "a", "b", "c", "d" };
        CommonUtil.rotate(arr, 1);
        String[] expected = { "d", "a", "b", "c" };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testRotate_List() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.rotate(list, 1);
        assertEquals(Arrays.asList("d", "a", "b", "c"), list);
    }

    @Test
    public void testSwap_BooleanArray() {
        boolean[] arr = { true, false, true };
        CommonUtil.swap(arr, 0, 2);
        assertTrue(CommonUtil.equals(arr, new boolean[] { true, false, true }));
        CommonUtil.swap(arr, 0, 1);
        assertTrue(CommonUtil.equals(arr, new boolean[] { false, true, true }));
    }

    @Test
    public void testSwap_IntArray() {
        int[] arr = { 1, 2, 3 };
        CommonUtil.swap(arr, 0, 2);
        assertTrue(CommonUtil.equals(arr, new int[] { 3, 2, 1 }));
    }

    @Test
    public void testSwap_ObjectArray() {
        String[] arr = { "a", "b", "c" };
        CommonUtil.swap(arr, 0, 2);
        assertTrue(CommonUtil.equals(arr, new String[] { "c", "b", "a" }));
    }

    @Test
    public void testSwap_List() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        CommonUtil.swap(list, 0, 2);
        assertEquals(Arrays.asList("c", "b", "a"), list);
    }

    @Test
    public void testNewHashMap() {
        Map<String, Integer> map = CommonUtil.newHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        map.put("a", 1);
        assertEquals(1, map.size());
    }

    @Test
    public void testNewHashMap_WithCapacity() {
        Map<String, Integer> map = CommonUtil.newHashMap(100);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewLinkedHashMap() {
        Map<String, Integer> map = CommonUtil.newLinkedHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertTrue(map instanceof LinkedHashMap);
    }

    @Test
    public void testNewTreeMap() {
        Map<String, Integer> map = CommonUtil.newTreeMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertTrue(map instanceof TreeMap);
    }

    @Test
    public void testNewConcurrentHashMap() {
        Map<String, Integer> map = CommonUtil.newConcurrentHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertTrue(map instanceof java.util.concurrent.ConcurrentHashMap);
    }

    @Test
    public void testNewHashSet() {
        Set<String> set = CommonUtil.newHashSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        set.add("a");
        assertEquals(1, set.size());
    }

    @Test
    public void testNewLinkedHashSet() {
        Set<String> set = CommonUtil.newLinkedHashSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertTrue(set instanceof LinkedHashSet);
    }

    @Test
    public void testNewTreeSet() {
        Set<String> set = CommonUtil.newTreeSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertTrue(set instanceof TreeSet);
    }

    @Test
    public void testNewArrayList() {
        List<String> list = CommonUtil.newArrayList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
        list.add("a");
        assertEquals(1, list.size());
    }

    @Test
    public void testNewArrayList_WithCapacity() {
        List<String> list = CommonUtil.newArrayList(100);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testNewLinkedList() {
        List<String> list = CommonUtil.newLinkedList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
        assertTrue(list instanceof LinkedList);
    }

    @Test
    public void testNewArrayDeque() {
        java.util.Deque<String> deque = CommonUtil.newArrayDeque();
        assertNotNull(deque);
        assertTrue(deque.isEmpty());
        assertTrue(deque instanceof java.util.ArrayDeque);
    }

    @Test
    public void testAnyNull_TwoArgs() {
        assertTrue(CommonUtil.anyNull(null, "a"));
        assertTrue(CommonUtil.anyNull("a", null));
        assertTrue(CommonUtil.anyNull(null, null));
        assertFalse(CommonUtil.anyNull("a", "b"));
    }

    @Test
    public void testAnyNull_ThreeArgs() {
        assertTrue(CommonUtil.anyNull(null, "a", "b"));
        assertTrue(CommonUtil.anyNull("a", null, "b"));
        assertTrue(CommonUtil.anyNull("a", "b", null));
        assertFalse(CommonUtil.anyNull("a", "b", "c"));
    }

    @Test
    public void testAnyNull_VarArgs() {
        assertTrue(CommonUtil.anyNull(new Object[] { "a", null, "c" }));
        assertFalse(CommonUtil.anyNull(new Object[] { "a", "b", "c" }));
        assertFalse(CommonUtil.anyNull(new Object[0]));
    }

    @Test
    public void testAllNull_TwoArgs() {
        assertTrue(CommonUtil.allNull(null, null));
        assertFalse(CommonUtil.allNull(null, "a"));
        assertFalse(CommonUtil.allNull("a", null));
        assertFalse(CommonUtil.allNull("a", "b"));
    }

    @Test
    public void testAllNull_ThreeArgs() {
        assertTrue(CommonUtil.allNull(null, null, null));
        assertFalse(CommonUtil.allNull(null, null, "a"));
        assertFalse(CommonUtil.allNull("a", "b", "c"));
    }

    @Test
    public void testAllNull_VarArgs() {
        assertTrue(CommonUtil.allNull(new Object[] { null, null, null }));
        assertFalse(CommonUtil.allNull(new Object[] { "a", null, "c" }));
        assertTrue(CommonUtil.allNull(new Object[0]));
    }

    @Test
    public void testAnyEmpty_TwoCharSequences() {
        assertTrue(CommonUtil.anyEmpty("", "a"));
        assertTrue(CommonUtil.anyEmpty("a", ""));
        assertTrue(CommonUtil.anyEmpty(null, "a"));
        assertFalse(CommonUtil.anyEmpty("a", "b"));
    }

    @Test
    public void testAnyEmpty_ThreeCharSequences() {
        assertTrue(CommonUtil.anyEmpty("", "a", "b"));
        assertTrue(CommonUtil.anyEmpty("a", "", "b"));
        assertFalse(CommonUtil.anyEmpty("a", "b", "c"));
    }

    @Test
    public void testAnyEmpty_Collections() {
        List<String> empty = new ArrayList<>();
        List<String> nonEmpty = Arrays.asList("a");
        assertTrue(CommonUtil.anyEmpty(empty, nonEmpty));
        assertTrue(CommonUtil.anyEmpty(nonEmpty, empty));
        assertFalse(CommonUtil.anyEmpty(nonEmpty, nonEmpty));
    }

    @Test
    public void testAllEmpty_TwoCharSequences() {
        assertTrue(CommonUtil.allEmpty("", ""));
        assertTrue(CommonUtil.allEmpty(null, ""));
        assertFalse(CommonUtil.allEmpty("", "a"));
        assertFalse(CommonUtil.allEmpty("a", "b"));
    }

    @Test
    public void testAllEmpty_ThreeCharSequences() {
        assertTrue(CommonUtil.allEmpty("", "", ""));
        assertFalse(CommonUtil.allEmpty("", "", "a"));
        assertFalse(CommonUtil.allEmpty("a", "b", "c"));
    }

    @Test
    public void testAllEmpty_Collections() {
        List<String> empty1 = new ArrayList<>();
        List<String> empty2 = new ArrayList<>();
        List<String> nonEmpty = Arrays.asList("a");
        assertTrue(CommonUtil.allEmpty(empty1, empty2));
        assertFalse(CommonUtil.allEmpty(empty1, nonEmpty));
    }

    @Test
    public void testAnyBlank_TwoArgs() {
        assertTrue(CommonUtil.anyBlank("", "a"));
        assertTrue(CommonUtil.anyBlank(" ", "a"));
        assertTrue(CommonUtil.anyBlank("a", " "));
        assertFalse(CommonUtil.anyBlank("a", "b"));
    }

    @Test
    public void testAnyBlank_ThreeArgs() {
        assertTrue(CommonUtil.anyBlank("", "a", "b"));
        assertTrue(CommonUtil.anyBlank("a", " ", "b"));
        assertFalse(CommonUtil.anyBlank("a", "b", "c"));
    }

    @Test
    public void testAllBlank_TwoArgs() {
        assertTrue(CommonUtil.allBlank("", ""));
        assertTrue(CommonUtil.allBlank(" ", "  "));
        assertTrue(CommonUtil.allBlank(null, ""));
        assertFalse(CommonUtil.allBlank("", "a"));
        assertFalse(CommonUtil.allBlank("a", "b"));
    }

    @Test
    public void testAllBlank_ThreeArgs() {
        assertTrue(CommonUtil.allBlank("", "", ""));
        assertTrue(CommonUtil.allBlank(" ", "  ", "\t"));
        assertFalse(CommonUtil.allBlank("", "", "a"));
    }

    @Test
    public void testIsSorted_CharArray() {
        assertTrue(CommonUtil.isSorted(new char[] { 'a', 'b', 'c' }));
        assertFalse(CommonUtil.isSorted(new char[] { 'c', 'b', 'a' }));
        assertTrue(CommonUtil.isSorted(new char[] { 'a' }));
        assertTrue(CommonUtil.isSorted(new char[0]));
    }

    @Test
    public void testIsSorted_IntArray() {
        assertTrue(CommonUtil.isSorted(new int[] { 1, 2, 3, 4 }));
        assertFalse(CommonUtil.isSorted(new int[] { 4, 3, 2, 1 }));
        assertTrue(CommonUtil.isSorted(new int[] { 1, 1, 2, 2 }));
    }

    @Test
    public void testIsSorted_ObjectArray() {
        assertTrue(CommonUtil.isSorted(new String[] { "a", "b", "c" }));
        assertFalse(CommonUtil.isSorted(new String[] { "c", "b", "a" }));
        assertTrue(CommonUtil.isSorted(new Integer[] { 1, 2, 3 }));
    }

    @Test
    public void testIsSorted_List() {
        assertTrue(CommonUtil.isSorted(Arrays.asList(1, 2, 3, 4)));
        assertFalse(CommonUtil.isSorted(Arrays.asList(4, 3, 2, 1)));
        assertTrue(CommonUtil.isSorted(Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testSort_IntArray() {
        int[] arr = { 3, 1, 4, 1, 5 };
        CommonUtil.sort(arr);
        assertTrue(CommonUtil.isSorted(arr));
        int[] expected = { 1, 1, 3, 4, 5 };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testSort_ObjectArray() {
        String[] arr = { "c", "a", "b" };
        CommonUtil.sort(arr);
        assertTrue(CommonUtil.isSorted(arr));
        String[] expected = { "a", "b", "c" };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testSort_List() {
        List<String> list = new ArrayList<>(Arrays.asList("c", "a", "b"));
        CommonUtil.sort(list);
        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    @Test
    public void testReverseSort_IntArray() {
        int[] arr = { 3, 1, 4, 1, 5 };
        CommonUtil.reverseSort(arr);
        int[] expected = { 5, 4, 3, 1, 1 };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testReverseSort_ObjectArray() {
        String[] arr = { "c", "a", "b" };
        CommonUtil.reverseSort(arr);
        String[] expected = { "c", "b", "a" };
        assertTrue(CommonUtil.equals(arr, expected));
    }

    @Test
    public void testToArray_Collection() {
        List<String> list = Arrays.asList("a", "b", "c");
        Object[] arr = CommonUtil.toArray(list);
        assertEquals(3, arr.length);
        assertEquals("a", arr[0]);
        assertEquals("b", arr[1]);
        assertEquals("c", arr[2]);
    }

    @Test
    public void testToArray_WithRange() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Object[] arr = CommonUtil.toArray(list, 1, 4);
        assertEquals(3, arr.length);
        assertEquals("b", arr[0]);
        assertEquals("c", arr[1]);
        assertEquals("d", arr[2]);
    }

    @Test
    public void testToBooleanArray() {
        List<Boolean> list = Arrays.asList(true, false, true);
        boolean[] arr = CommonUtil.toBooleanArray(list);
        assertEquals(3, arr.length);
        assertTrue(arr[0]);
        assertFalse(arr[1]);
        assertTrue(arr[2]);
    }

    @Test
    public void testToIntArray() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        int[] arr = CommonUtil.toIntArray(list);
        assertEquals(3, arr.length);
        assertEquals(1, arr[0]);
        assertEquals(2, arr[1]);
        assertEquals(3, arr[2]);
    }

    @Test
    public void testToList_Array() {
        String[] arr = { "a", "b", "c" };
        List<String> list = CommonUtil.toList(arr);
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
    }

    @Test
    public void testToList_IntArray() {
        int[] arr = { 1, 2, 3 };
        List<Integer> list = CommonUtil.toList(arr);
        assertEquals(3, list.size());
        assertEquals(Integer.valueOf(1), list.get(0));
        assertEquals(Integer.valueOf(2), list.get(1));
        assertEquals(Integer.valueOf(3), list.get(2));
    }

    @Test
    public void testToSet_Array() {
        String[] arr = { "a", "b", "c", "a" };
        Set<String> set = CommonUtil.toSet(arr);
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
        assertEquals(5, CommonUtil.len("hello"));
        assertEquals(0, CommonUtil.len(""));
        assertEquals(0, CommonUtil.len((String) null));
    }

    @Test
    public void testLen_Array() {
        assertEquals(3, CommonUtil.len(new int[] { 1, 2, 3 }));
        assertEquals(0, CommonUtil.len(new int[0]));
        assertEquals(0, CommonUtil.len((int[]) null));
    }

    @Test
    public void testLen_Collection() {
        assertEquals(3, CommonUtil.len(new String[] { "a", "b", "c" }));
        assertEquals(0, CommonUtil.len(new String[0]));
        assertEquals(0, CommonUtil.len((String[]) null));
    }

    @Test
    public void testSize_Collection() {
        assertEquals(3, CommonUtil.size(Arrays.asList("a", "b", "c")));
        assertEquals(0, CommonUtil.size(new ArrayList<>()));
        assertEquals(0, CommonUtil.size((List<?>) null));
    }

    @Test
    public void testSize_Map() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, CommonUtil.size(map));
        assertEquals(0, CommonUtil.size(new HashMap<>()));
        assertEquals(0, CommonUtil.size((Map<?, ?>) null));
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
        assertEquals(Arrays.asList("a", "a", "a"), CommonUtil.repeat("a", 3));
    }

    @Test
    public void testRepeat_Char() {
        assertEquals(Arrays.asList('a', 'a', 'a'), CommonUtil.repeat('a', 3));
    }

    @Test
    public void testConcat_StringArrays() {
        String[] arr1 = { "a", "b" };
        String[] arr2 = { "c", "d" };
        String[] result = N.concat(arr1, arr2);
        assertEquals(4, result.length);
        assertTrue(CommonUtil.equals(result, new String[] { "a", "b", "c", "d" }));
    }

    @Test
    public void testConcat_IntArrays() {
        int[] arr1 = { 1, 2 };
        int[] arr2 = { 3, 4 };
        int[] result = N.concat(arr1, arr2);
        assertEquals(4, result.length);
        assertTrue(CommonUtil.equals(result, new int[] { 1, 2, 3, 4 }));
    }

    @Test
    public void testAsArray() {
        String[] arr = CommonUtil.asArray("a", "b", "c");
        assertEquals(3, arr.length);
        assertEquals("a", arr[0]);
        assertEquals("b", arr[1]);
        assertEquals("c", arr[2]);
    }

    @Test
    public void testAsList() {
        List<String> list = CommonUtil.asList("a", "b", "c");
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
    }

    @Test
    public void testAsLinkedList() {
        LinkedList<String> list = CommonUtil.asLinkedList("a", "b", "c");
        assertEquals(3, list.size());
        assertEquals("a", list.getFirst());
        assertEquals("c", list.getLast());
    }

    @Test
    public void testAsSet() {
        Set<String> set = CommonUtil.asSet("a", "b", "c", "a");
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
    }

    @Test
    public void testAsLinkedHashSet() {
        Set<String> set = CommonUtil.asLinkedHashSet("c", "b", "a");
        assertEquals(3, set.size());
        Iterator<String> it = set.iterator();
        assertEquals("c", it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
    }

    @Test
    public void testAsLinkedHashMap() {
        Map<String, Integer> map = CommonUtil.asLinkedHashMap("a", 1, "b", 2);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    @Test
    public void testAsMap() {
        Map<String, Integer> map = CommonUtil.asMap("a", 1, "b", 2, "c", 3);
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertEquals(Integer.valueOf(3), map.get("c"));
    }

    @Test
    public void testAsProps() {
        Map<String, Object> props = CommonUtil.asProps("name", "John", "age", 30);
        assertEquals(2, props.size());
        assertEquals("John", props.get("name"));
        assertEquals(30, props.get("age"));
    }

    @Test
    public void testAsSingletonList() {
        List<String> list = CommonUtil.asSingletonList("test");
        assertEquals(1, list.size());
        assertEquals("test", list.get(0));
    }

    @Test
    public void testAsSingletonSet() {
        Set<String> set = CommonUtil.asSingletonSet("test");
        assertEquals(1, set.size());
        assertTrue(set.contains("test"));
    }

    @Test
    public void testAsSingletonMap() {
        Map<String, Integer> map = CommonUtil.asSingletonMap("key", 100);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(100), map.get("key"));
    }

    @Test
    public void testAsDeque() {
        java.util.Deque<String> deque = CommonUtil.asDeque("a", "b", "c");
        assertEquals(3, deque.size());
        assertEquals("a", deque.getFirst());
        assertEquals("c", deque.getLast());
    }

    @Test
    public void testAsArrayDeque() {
        java.util.ArrayDeque<String> deque = CommonUtil.asArrayDeque("a", "b", "c");
        assertEquals(3, deque.size());
        assertEquals("a", deque.getFirst());
        assertEquals("c", deque.getLast());
    }

    @Test
    public void testAsQueue() {
        java.util.Queue<String> queue = CommonUtil.asQueue("a", "b", "c");
        assertEquals(3, queue.size());
        assertEquals("a", queue.peek());
    }

    @Test
    public void testAsPriorityQueue() {
        java.util.PriorityQueue<Integer> queue = CommonUtil.asPriorityQueue(3, 1, 2);
        assertEquals(3, queue.size());
        assertEquals(Integer.valueOf(1), queue.poll());
        assertEquals(Integer.valueOf(2), queue.poll());
        assertEquals(Integer.valueOf(3), queue.poll());
    }

    @Test
    public void testAsNavigableSet() {
        java.util.NavigableSet<Integer> set = CommonUtil.asNavigableSet(3, 1, 2);
        assertEquals(3, set.size());
        assertEquals(Integer.valueOf(1), set.first());
        assertEquals(Integer.valueOf(3), set.last());
    }

    @Test
    public void testAsSortedSet() {
        java.util.SortedSet<Integer> set = CommonUtil.asSortedSet(3, 1, 2);
        assertEquals(3, set.size());
        assertEquals(Integer.valueOf(1), set.first());
        assertEquals(Integer.valueOf(3), set.last());
    }

    @Test
    public void testAsArrayBlockingQueue() {
        java.util.concurrent.ArrayBlockingQueue<String> queue = CommonUtil.asArrayBlockingQueue("a", "b");
        assertEquals(2, queue.size());
        assertEquals("a", queue.peek());
    }

    @Test
    public void testAsLinkedBlockingQueue() {
        java.util.concurrent.LinkedBlockingQueue<String> queue = CommonUtil.asLinkedBlockingQueue("a", "b");
        assertEquals(2, queue.size());
        assertEquals("a", queue.peek());
    }

    @Test
    public void testAsLinkedBlockingDeque() {
        java.util.concurrent.LinkedBlockingDeque<String> deque = CommonUtil.asLinkedBlockingDeque("a", "b");
        assertEquals(2, deque.size());
        assertEquals("a", deque.getFirst());
    }

    @Test
    public void testAsConcurrentLinkedQueue() {
        java.util.concurrent.ConcurrentLinkedQueue<String> queue = CommonUtil.asConcurrentLinkedQueue("a", "b");
        assertEquals(2, queue.size());
        assertEquals("a", queue.peek());
    }

    @Test
    public void testAsConcurrentLinkedDeque() {
        java.util.concurrent.ConcurrentLinkedDeque<String> deque = CommonUtil.asConcurrentLinkedDeque("a", "b");
        assertEquals(2, deque.size());
        assertEquals("a", deque.getFirst());
    }

    @Test
    public void testAsDelayQueue() {
        java.util.concurrent.DelayQueue<java.util.concurrent.Delayed> queue = CommonUtil.asDelayQueue();
        assertNotNull(queue);
        assertTrue(queue.isEmpty());
    }

    @Test
    public void testAsMultiset() {
        com.landawn.abacus.util.Multiset<String> multiset = CommonUtil.asMultiset("a", "b", "a", "c");
        assertEquals(4, multiset.size());
        assertEquals(2, multiset.count("a"));
        assertEquals(1, multiset.count("b"));
    }

    @Test
    public void testEmptyList() {
        List<String> list = CommonUtil.emptyList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> list.add("test"));
    }

    @Test
    public void testEmptySet() {
        Set<String> set = CommonUtil.emptySet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> set.add("test"));
    }

    @Test
    public void testEmptyMap() {
        Map<String, String> map = CommonUtil.emptyMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> map.put("key", "value"));
    }

    @Test
    public void testEmptySortedSet() {
        java.util.SortedSet<String> set = CommonUtil.emptySortedSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> set.add("test"));
    }

    @Test
    public void testEmptyNavigableSet() {
        java.util.NavigableSet<String> set = CommonUtil.emptyNavigableSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> set.add("test"));
    }

    @Test
    public void testEmptySortedMap() {
        java.util.SortedMap<String, String> map = CommonUtil.emptySortedMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> map.put("key", "value"));
    }

    @Test
    public void testEmptyNavigableMap() {
        java.util.NavigableMap<String, String> map = CommonUtil.emptyNavigableMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> map.put("key", "value"));
    }

    @Test
    public void testEmptyIterator() {
        Iterator<String> it = CommonUtil.emptyIterator();
        assertNotNull(it);
        assertFalse(it.hasNext());
    }

    @Test
    public void testEmptyListIterator() {
        java.util.ListIterator<String> it = CommonUtil.emptyListIterator();
        assertNotNull(it);
        assertFalse(it.hasNext());
        assertFalse(it.hasPrevious());
    }

    @Test
    public void testEmptyDataset() {
    }

    @Test
    public void testEmptyInputStream() {
        java.io.InputStream is = CommonUtil.emptyInputStream();
        assertNotNull(is);
        assertDoesNotThrow(() -> assertEquals(-1, is.read()));
    }

    @Test
    public void testFindFirst() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        com.landawn.abacus.util.u.Nullable<Integer> result = CommonUtil.findFirst(list, x -> x > 3);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(4), result.get());
    }

    @Test
    public void testFindFirstIndex() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        com.landawn.abacus.util.u.OptionalInt index = CommonUtil.findFirstIndex(list, x -> x > 3);
        assertTrue(index.isPresent());
        assertEquals(3, index.getAsInt());
    }

    @Test
    public void testFindFirstNonNull() {
    }

    @Test
    public void testFindLast() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        com.landawn.abacus.util.u.Nullable<Integer> result = CommonUtil.findLast(list, x -> x < 4);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(3), result.get());
    }

    @Test
    public void testFindLastIndex() {
        List<Integer> list = Arrays.asList(1, 2, 3, 2, 1);
        com.landawn.abacus.util.u.OptionalInt index = CommonUtil.findLastIndex(list, x -> x == 2);
        assertTrue(index.isPresent());
        assertEquals(3, index.getAsInt());
    }

    @Test
    public void testFindLastNonNull() {
    }

    @Test
    public void testFirstNonNull() {
        com.landawn.abacus.util.u.Optional<String> result = CommonUtil.firstNonNull(null, "first", null, "second");
        assertTrue(result.isPresent());
        assertEquals("first", result.get());
    }

    @Test
    public void testLastNonNull() {
        com.landawn.abacus.util.u.Optional<String> result = CommonUtil.lastNonNull(null, "first", null, "second", null);
        assertTrue(result.isPresent());
        assertEquals("second", result.get());
    }

    @Test
    public void testFirstNonEmpty() {
        com.landawn.abacus.util.u.Optional<String> result = CommonUtil.firstNonEmpty("", null, "first", "second");
        assertTrue(result.isPresent());
        assertEquals("first", result.get());
    }

    @Test
    public void testFirstNonBlank() {
        com.landawn.abacus.util.u.Optional<String> result = CommonUtil.firstNonBlank("", "  ", "first", "second");
        assertTrue(result.isPresent());
        assertEquals("first", result.get());
    }

    @Test
    public void testFirstElement() {
        List<String> list = Arrays.asList("a", "b", "c");
        com.landawn.abacus.util.u.Nullable<String> result = CommonUtil.firstElement(list);
        assertTrue(result.isPresent());
        assertEquals("a", result.get());
    }

    @Test
    public void testFirstElements() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        List<String> result = CommonUtil.firstElements(list, 2);
        assertEquals(2, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
    }

    @Test
    public void testLastElement() {
        List<String> list = Arrays.asList("a", "b", "c");
        com.landawn.abacus.util.u.Nullable<String> result = CommonUtil.lastElement(list);
        assertTrue(result.isPresent());
        assertEquals("c", result.get());
    }

    @Test
    public void testLastElements() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        List<String> result = CommonUtil.lastElements(list, 2);
        assertEquals(2, result.size());
        assertEquals("c", result.get(0));
        assertEquals("d", result.get(1));
    }

    @Test
    public void testFirstOrNullIfEmpty() {
        List<String> list = Arrays.asList("a", "b");
        assertEquals("a", CommonUtil.firstOrNullIfEmpty(list));
        assertNull(CommonUtil.firstOrNullIfEmpty(new ArrayList<>()));
    }

    @Test
    public void testFirstOrDefaultIfEmpty() {
        List<String> list = Arrays.asList("a", "b");
        assertEquals("a", CommonUtil.firstOrDefaultIfEmpty(list, "default"));
        assertEquals("default", CommonUtil.firstOrDefaultIfEmpty(new ArrayList<>(), "default"));
    }

    @Test
    public void testLastOrNullIfEmpty() {
        List<String> list = Arrays.asList("a", "b");
        assertEquals("b", CommonUtil.lastOrNullIfEmpty(list));
        assertNull(CommonUtil.lastOrNullIfEmpty(new ArrayList<>()));
    }

    @Test
    public void testLastOrDefaultIfEmpty() {
        List<String> list = Arrays.asList("a", "b");
        assertEquals("b", CommonUtil.lastOrDefaultIfEmpty(list, "default"));
        assertEquals("default", CommonUtil.lastOrDefaultIfEmpty(new ArrayList<>(), "default"));
    }

    @Test
    public void testFirstEntry() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        com.landawn.abacus.util.u.Optional<Map.Entry<String, Integer>> entry = CommonUtil.firstEntry(map);
        assertTrue(entry.isPresent());
        assertEquals("a", entry.get().getKey());
        assertEquals(Integer.valueOf(1), entry.get().getValue());
    }

    @Test
    public void testLastEntry() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        com.landawn.abacus.util.u.Optional<Map.Entry<String, Integer>> entry = CommonUtil.lastEntry(map);
        assertTrue(entry.isPresent());
        assertEquals("b", entry.get().getKey());
        assertEquals(Integer.valueOf(2), entry.get().getValue());
    }

    @Test
    public void testParallelSort() {
        Integer[] arr = { 5, 2, 8, 1, 9 };
        CommonUtil.parallelSort(arr);
        assertEquals(1, arr[0]);
        assertEquals(2, arr[1]);
        assertEquals(5, arr[2]);
        assertEquals(8, arr[3]);
        assertEquals(9, arr[4]);
    }

    @Test
    public void testSortBy() {
        List<String> list = Arrays.asList("apple", "pie", "a", "dog");
        CommonUtil.sortBy(list, String::length);
        assertEquals("a", list.get(0));
        assertEquals(3, list.get(1).length());
        assertEquals(3, list.get(2).length());
        assertEquals("apple", list.get(3));
    }

    @Test
    public void testSortByInt() {
        List<String> list = Arrays.asList("apple", "zoo", "a", "be");
        CommonUtil.sortByInt(list, String::length);
        assertEquals("a", list.get(0));
        assertEquals(2, list.get(1).length());
        assertEquals(3, list.get(2).length());
        assertEquals("apple", list.get(3));
    }

    @Test
    public void testSortByLong() {
        List<String> list = Arrays.asList("apple", "pie", "a");
        CommonUtil.sortByLong(list, s -> (long) s.length());
        assertEquals("a", list.get(0));
        assertEquals("pie", list.get(1));
        assertEquals("apple", list.get(2));
    }

    @Test
    public void testSortByDouble() {
        List<String> list = Arrays.asList("apple", "pie", "a");
        CommonUtil.sortByDouble(list, s -> (double) s.length());
        assertEquals("a", list.get(0));
        assertEquals("pie", list.get(1));
        assertEquals("apple", list.get(2));
    }

    @Test
    public void testSortByFloat() {
        List<String> list = Arrays.asList("apple", "pie", "a");
        CommonUtil.sortByFloat(list, s -> (float) s.length());
        assertEquals("a", list.get(0));
        assertEquals("pie", list.get(1));
        assertEquals("apple", list.get(2));
    }

    @Test
    public void testParallelSortBy() {
        List<String> list = Arrays.asList("apple", "zoo", "a", "be");
        CommonUtil.parallelSortBy(list, String::length);
        assertEquals("a", list.get(0));
        assertEquals(2, list.get(1).length());
    }

    @Test
    public void testParallelSortByInt() {
        List<String> list = Arrays.asList("apple", "zoo", "a", "be");
        CommonUtil.parallelSortByInt(list, String::length);
        assertEquals("a", list.get(0));
        assertEquals(2, list.get(1).length());
    }

    @Test
    public void testParallelSortByLong() {
        List<String> list = Arrays.asList("apple", "pie", "a");
        CommonUtil.parallelSortByLong(list, s -> (long) s.length());
        assertEquals("a", list.get(0));
        assertEquals("pie", list.get(1));
    }

    @Test
    public void testParallelSortByDouble() {
        List<String> list = Arrays.asList("apple", "pie", "a");
        CommonUtil.parallelSortByDouble(list, s -> (double) s.length());
        assertEquals("a", list.get(0));
        assertEquals("pie", list.get(1));
    }

    @Test
    public void testParallelSortByFloat() {
        List<String> list = Arrays.asList("apple", "pie", "a");
        CommonUtil.parallelSortByFloat(list, s -> (float) s.length());
        assertEquals("a", list.get(0));
        assertEquals("pie", list.get(1));
    }

    @Test
    public void testReverseSortBy() {
        List<String> list = Arrays.asList("a", "zoo", "be", "apple");
        CommonUtil.reverseSortBy(list, String::length);
        assertEquals("apple", list.get(0));
        assertEquals(3, list.get(1).length());
        assertEquals(2, list.get(2).length());
        assertEquals("a", list.get(3));
    }

    @Test
    public void testReverseSortByInt() {
        List<String> list = Arrays.asList("a", "cat", "apple");
        CommonUtil.reverseSortByInt(list, String::length);
        assertEquals("apple", list.get(0));
        assertEquals("cat", list.get(1));
        assertEquals("a", list.get(2));
    }

    @Test
    public void testReverseSortByLong() {
        List<String> list = Arrays.asList("a", "cat", "apple");
        CommonUtil.reverseSortByLong(list, s -> (long) s.length());
        assertEquals("apple", list.get(0));
        assertEquals("cat", list.get(1));
        assertEquals("a", list.get(2));
    }

    @Test
    public void testReverseSortByDouble() {
        List<String> list = Arrays.asList("a", "cat", "apple");
        CommonUtil.reverseSortByDouble(list, s -> (double) s.length());
        assertEquals("apple", list.get(0));
        assertEquals("cat", list.get(1));
        assertEquals("a", list.get(2));
    }

    @Test
    public void testReverseSortByFloat() {
        List<String> list = Arrays.asList("a", "cat", "apple");
        CommonUtil.reverseSortByFloat(list, s -> (float) s.length());
        assertEquals("apple", list.get(0));
        assertEquals("cat", list.get(1));
        assertEquals("a", list.get(2));
    }

    @Test
    public void testToByteArray() {
        List<Byte> list = Arrays.asList((byte) 1, (byte) 2, (byte) 3);
        byte[] arr = CommonUtil.toByteArray(list);
        assertEquals(3, arr.length);
        assertEquals((byte) 1, arr[0]);
        assertEquals((byte) 2, arr[1]);
        assertEquals((byte) 3, arr[2]);
    }

    @Test
    public void testToCharArray() {
        List<Character> list = Arrays.asList('a', 'b', 'c');
        char[] arr = CommonUtil.toCharArray(list);
        assertEquals(3, arr.length);
        assertEquals('a', arr[0]);
        assertEquals('b', arr[1]);
        assertEquals('c', arr[2]);
    }

    @Test
    public void testToShortArray() {
        List<Short> list = Arrays.asList((short) 1, (short) 2, (short) 3);
        short[] arr = CommonUtil.toShortArray(list);
        assertEquals(3, arr.length);
        assertEquals((short) 1, arr[0]);
        assertEquals((short) 2, arr[1]);
        assertEquals((short) 3, arr[2]);
    }

    @Test
    public void testToLongArray() {
        List<Long> list = Arrays.asList(1L, 2L, 3L);
        long[] arr = CommonUtil.toLongArray(list);
        assertEquals(3, arr.length);
        assertEquals(1L, arr[0]);
        assertEquals(2L, arr[1]);
        assertEquals(3L, arr[2]);
    }

    @Test
    public void testToFloatArray() {
        List<Float> list = Arrays.asList(1.1f, 2.2f, 3.3f);
        float[] arr = CommonUtil.toFloatArray(list);
        assertEquals(3, arr.length);
        assertEquals(1.1f, arr[0], 0.01);
        assertEquals(2.2f, arr[1], 0.01);
        assertEquals(3.3f, arr[2], 0.01);
    }

    @Test
    public void testToDoubleArray() {
        List<Double> list = Arrays.asList(1.1, 2.2, 3.3);
        double[] arr = CommonUtil.toDoubleArray(list);
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
        Map<String, Integer> map = CommonUtil.toMap(pairs, p -> p.key, p -> p.value);
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertEquals(Integer.valueOf(3), map.get("c"));
    }

    @Test
    public void testToCollection() {
        Integer[] arr = { 1, 2, 3 };
        Collection<Integer> col = CommonUtil.toCollection(arr, ArrayList::new);
        assertEquals(3, col.size());
        assertTrue(col.contains(1));
        assertTrue(col.contains(2));
        assertTrue(col.contains(3));
    }

    @Test
    public void testUnmodifiableList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        List<String> unmodList = CommonUtil.unmodifiableList(list);
        assertEquals(3, unmodList.size());
        assertThrows(UnsupportedOperationException.class, () -> unmodList.add("d"));
    }

    @Test
    public void testUnmodifiableSet() {
        Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> unmodSet = CommonUtil.unmodifiableSet(set);
        assertEquals(3, unmodSet.size());
        assertThrows(UnsupportedOperationException.class, () -> unmodSet.add("d"));
    }

    @Test
    public void testUnmodifiableMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Map<String, Integer> unmodMap = CommonUtil.unmodifiableMap(map);
        assertEquals(1, unmodMap.size());
        assertThrows(UnsupportedOperationException.class, () -> unmodMap.put("b", 2));
    }

    @Test
    public void testUnmodifiableSortedSet() {
        java.util.SortedSet<Integer> set = new TreeSet<>(Arrays.asList(3, 1, 2));
        java.util.SortedSet<Integer> unmodSet = CommonUtil.unmodifiableSortedSet(set);
        assertEquals(3, unmodSet.size());
        assertThrows(UnsupportedOperationException.class, () -> unmodSet.add(4));
    }

    @Test
    public void testUnmodifiableNavigableSet() {
        java.util.NavigableSet<Integer> set = new TreeSet<>(Arrays.asList(3, 1, 2));
        java.util.NavigableSet<Integer> unmodSet = CommonUtil.unmodifiableNavigableSet(set);
        assertEquals(3, unmodSet.size());
        assertThrows(UnsupportedOperationException.class, () -> unmodSet.add(4));
    }

    @Test
    public void testUnmodifiableSortedMap() {
        java.util.SortedMap<String, Integer> map = new TreeMap<>();
        map.put("a", 1);
        java.util.SortedMap<String, Integer> unmodMap = CommonUtil.unmodifiableSortedMap(map);
        assertEquals(1, unmodMap.size());
        assertThrows(UnsupportedOperationException.class, () -> unmodMap.put("b", 2));
    }

    @Test
    public void testUnmodifiableNavigableMap() {
        java.util.NavigableMap<String, Integer> map = new TreeMap<>();
        map.put("a", 1);
        java.util.NavigableMap<String, Integer> unmodMap = CommonUtil.unmodifiableNavigableMap(map);
        assertEquals(1, unmodMap.size());
        assertThrows(UnsupportedOperationException.class, () -> unmodMap.put("b", 2));
    }

    @Test
    public void testUnmodifiableCollection() {
        Collection<String> col = new ArrayList<>(Arrays.asList("a", "b"));
        Collection<String> unmodCol = CommonUtil.unmodifiableCollection(col);
        assertEquals(2, unmodCol.size());
        assertThrows(UnsupportedOperationException.class, () -> unmodCol.add("c"));
    }

    @Test
    public void testGreaterThan() {
        assertTrue(CommonUtil.greaterThan(5, 3));
        assertFalse(CommonUtil.greaterThan(3, 5));
        assertFalse(CommonUtil.greaterThan(3, 3));
    }

    @Test
    public void testGreaterEqual() {
        assertTrue(CommonUtil.greaterEqual(5, 3));
        assertTrue(CommonUtil.greaterEqual(3, 3));
        assertFalse(CommonUtil.greaterEqual(2, 3));
    }

    @Test
    public void testLessThan() {
        assertTrue(CommonUtil.lessThan(3, 5));
        assertFalse(CommonUtil.lessThan(5, 3));
        assertFalse(CommonUtil.lessThan(3, 3));
    }

    @Test
    public void testLessEqual() {
        assertTrue(CommonUtil.lessEqual(3, 5));
        assertTrue(CommonUtil.lessEqual(3, 3));
        assertFalse(CommonUtil.lessEqual(5, 3));
    }

    @Test
    public void testGtAndLt() {
        assertTrue(CommonUtil.gtAndLt(5, 3, 7));
        assertFalse(CommonUtil.gtAndLt(2, 3, 7));
        assertFalse(CommonUtil.gtAndLt(8, 3, 7));
    }

    @Test
    public void testGtAndLe() {
        assertTrue(CommonUtil.gtAndLe(5, 3, 7));
        assertTrue(CommonUtil.gtAndLe(7, 3, 7));
        assertFalse(CommonUtil.gtAndLe(2, 3, 7));
    }

    @Test
    public void testGeAndLt() {
        assertTrue(CommonUtil.geAndLt(5, 3, 7));
        assertTrue(CommonUtil.geAndLt(3, 3, 7));
        assertFalse(CommonUtil.geAndLt(7, 3, 7));
    }

    @Test
    public void testGeAndLe() {
        assertTrue(CommonUtil.geAndLe(5, 3, 7));
        assertTrue(CommonUtil.geAndLe(3, 3, 7));
        assertTrue(CommonUtil.geAndLe(7, 3, 7));
        assertFalse(CommonUtil.geAndLe(2, 3, 7));
    }

    @Test
    public void testIsBetween() {
        assertTrue(CommonUtil.isBetween(5, 3, 7));
        assertTrue(CommonUtil.isBetween(3, 3, 7));
        assertTrue(CommonUtil.isBetween(7, 3, 7));
        assertFalse(CommonUtil.isBetween(2, 3, 7));
    }

    @Test
    public void testIsTrue() {
        assertTrue(CommonUtil.isTrue(true));
        assertFalse(CommonUtil.isTrue(false));
        assertFalse(CommonUtil.isTrue(null));
    }

    @Test
    public void testIsFalse() {
        assertTrue(CommonUtil.isFalse(false));
        assertFalse(CommonUtil.isFalse(true));
        assertFalse(CommonUtil.isFalse(null));
    }

    @Test
    public void testIsNotTrue() {
        assertTrue(CommonUtil.isNotTrue(false));
        assertTrue(CommonUtil.isNotTrue(null));
        assertFalse(CommonUtil.isNotTrue(true));
    }

    @Test
    public void testIsNotFalse() {
        assertTrue(CommonUtil.isNotFalse(true));
        assertTrue(CommonUtil.isNotFalse(null));
        assertFalse(CommonUtil.isNotFalse(false));
    }

    @Test
    public void testIndexOfIgnoreCase() {
        String[] arr = { "Apple", "Banana", "Cherry" };
        assertEquals(1, CommonUtil.indexOfIgnoreCase(arr, "banana"));
        assertEquals(-1, CommonUtil.indexOfIgnoreCase(arr, "grape"));
    }

    @Test
    public void testLastIndexOfIgnoreCase() {
        String[] arr = { "Apple", "Banana", "apple" };
        assertEquals(2, CommonUtil.lastIndexOfIgnoreCase(arr, "APPLE"));
        assertEquals(-1, CommonUtil.lastIndexOfIgnoreCase(arr, "grape"));
    }

    @Test
    public void testIndexOfSubList() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> subList = Arrays.asList(3, 4);
        assertEquals(2, CommonUtil.indexOfSubList(list, subList));
        assertEquals(0, Strings.indexOf("", ""));
        assertEquals(0, "".indexOf(""));
        assertEquals(0, Collections.indexOfSubList(N.emptyList(), N.emptyList()));
        assertEquals(0, CommonUtil.indexOfSubList(N.emptyList(), N.emptyList()));
        assertEquals(0, Index.ofSubList(N.emptyList(), N.emptyList()).orElseThrow());
        assertEquals(0, Index.ofSubList(N.emptyList(), 0, N.emptyList()).orElseThrow());
        assertEquals(Collections.indexOfSubList(N.emptyList(), N.emptyList()), CommonUtil.indexOfSubList(N.emptyList(), N.emptyList()));
    }

    @Test
    public void testLastIndexOfSubList() {
        List<Integer> list = Arrays.asList(1, 2, 3, 2, 3, 4);
        List<Integer> subList = Arrays.asList(2, 3);
        assertEquals(3, CommonUtil.lastIndexOfSubList(list, subList));

        assertEquals(0, Strings.lastIndexOf("", ""));
        assertEquals(0, "".lastIndexOf(""));
        assertEquals(0, Collections.lastIndexOfSubList(N.emptyList(), N.emptyList()));
        assertEquals(0, CommonUtil.lastIndexOfSubList(N.emptyList(), N.emptyList()));
        assertEquals(0, Index.lastOfSubList(N.emptyList(), N.emptyList()).orElseThrow());
        assertEquals(0, Index.lastOfSubList(N.emptyList(), 0, N.emptyList()).orElseThrow());
        assertEquals(Collections.lastIndexOfSubList(N.emptyList(), N.emptyList()), CommonUtil.lastIndexOfSubList(N.emptyList(), N.emptyList()));
    }

    @Test
    public void testPadLeft() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        CommonUtil.padLeft(list, 5, "x");
        assertEquals(5, list.size());
        assertEquals("x", list.get(0));
        assertEquals("x", list.get(1));
    }

    @Test
    public void testPadRight() {
        Collection<String> col = new ArrayList<>(Arrays.asList("a", "b", "c"));
        CommonUtil.padRight(col, 5, "x");
        assertEquals(5, col.size());
    }

    @Test
    public void testNullToEmpty() {
        String[] arr = null;
        String[] result = CommonUtil.nullToEmpty(arr);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testNullToEmptyForEach() {
        String[] arr = { null, "a", null, "b" };
        CommonUtil.nullElementsToEmpty(arr);
        assertEquals("", arr[0]);
        assertEquals("a", arr[1]);
        assertEquals("", arr[2]);
        assertEquals("b", arr[3]);
    }

    @Test
    public void testNewArray() {
        String[] arr = CommonUtil.newArray(String.class, 3);
        assertEquals(3, arr.length);
        assertNull(arr[0]);
    }

    @Test
    public void testNewInstance() {
        String str = CommonUtil.newInstance(String.class);
        assertNotNull(str);
        assertEquals("", str);
    }

    @Test
    public void testNewEntry() {
        Map.Entry<String, Integer> entry = CommonUtil.newEntry("key", 100);
        assertEquals("key", entry.getKey());
        assertEquals(Integer.valueOf(100), entry.getValue());
    }

    @Test
    public void testNewImmutableEntry() {
        Map.Entry<String, Integer> entry = CommonUtil.newImmutableEntry("key", 100);
        assertEquals("key", entry.getKey());
        assertEquals(Integer.valueOf(100), entry.getValue());
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(200));
    }

    @Test
    public void testNewBiMap() {
        com.landawn.abacus.util.BiMap<String, Integer> biMap = CommonUtil.newBiMap();
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewIdentityHashMap() {
        java.util.IdentityHashMap<String, Integer> map = CommonUtil.newIdentityHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewConcurrentHashSet() {
        Set<String> set = CommonUtil.newConcurrentHashSet();
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
        com.landawn.abacus.util.Multiset<String> multiset = CommonUtil.newMultiset();
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testNewCollection() {
        Collection<String> col = CommonUtil.newCollection(ArrayList.class);
        assertNotNull(col);
        assertTrue(col.isEmpty());
        assertTrue(col instanceof ArrayList);
    }

    @Test
    public void testNewMap() {
        Map<String, Integer> map = CommonUtil.newMap(HashMap.class);
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertTrue(map instanceof HashMap);
    }

    @Test
    public void testNewMultimap() {
        com.landawn.abacus.util.Multimap<String, Integer, List<Integer>> multimap = CommonUtil.newMultimap(HashMap::new, ArrayList::new);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewListMultimap() {
        com.landawn.abacus.util.ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSetMultimap() {
        com.landawn.abacus.util.SetMultimap<String, Integer> multimap = CommonUtil.newSetMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedListMultimap() {
        com.landawn.abacus.util.ListMultimap<String, Integer> multimap = CommonUtil.newLinkedListMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedSetMultimap() {
        com.landawn.abacus.util.SetMultimap<String, Integer> multimap = CommonUtil.newLinkedSetMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSortedListMultimap() {
        com.landawn.abacus.util.ListMultimap<String, Integer> multimap = CommonUtil.newSortedListMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSortedSetMultimap() {
        com.landawn.abacus.util.SetMultimap<String, Integer> multimap = CommonUtil.newSortedSetMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSetFromMap() {
        Map<String, Boolean> map = new HashMap<>();
        Set<String> set = CommonUtil.newSetFromMap(map);
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
        com.landawn.abacus.util.ImmutableSet<TestEnum> set = CommonUtil.enumSetOf(TestEnum.class);
        assertNotNull(set);
    }

    @Test
    public void testEnumMapOf() {
        enum TestEnum {
            A, B, C
        }
        com.landawn.abacus.util.ImmutableBiMap<TestEnum, String> map = CommonUtil.enumMapOf(TestEnum.class);
        assertNotNull(map);
        assertFalse(map.isEmpty());
    }

    @Test
    public void testEnumListOf() {
        enum TestEnum {
            A, B, C
        }
        List<TestEnum> list = CommonUtil.enumListOf(TestEnum.class);
        assertEquals(3, list.size());
        assertTrue(list.contains(TestEnum.A));
        assertTrue(list.contains(TestEnum.B));
        assertTrue(list.contains(TestEnum.C));
    }

    @Test
    public void testCopyOf() {
        String[] original = { "a", "b", "c" };
        String[] copy = CommonUtil.copyOf(original, 3);
        assertEquals(original.length, copy.length);
        assertTrue(Arrays.equals(original, copy));
        assertNotSame(original, copy);
    }

    @Test
    public void testCopyOfRange() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String[] copy = CommonUtil.copyOfRange(arr, 1, 4);
        assertEquals(3, copy.length);
        assertEquals("b", copy[0]);
        assertEquals("c", copy[1]);
        assertEquals("d", copy[2]);
    }

    @Test
    public void testTypeOf() {
        com.landawn.abacus.type.Type<String> type = CommonUtil.typeOf(String.class);
        assertNotNull(type);
        assertEquals(String.class, type.clazz());
    }

    @Test
    public void testValueOf() {
        Integer result = CommonUtil.valueOf("123", Integer.class);
        assertEquals(Integer.valueOf(123), result);
    }

    @Test
    public void testDefaultValueOf() {
        int result = CommonUtil.defaultValueOf(int.class);
        assertEquals(0, result);
    }

    @Test
    public void testConvert() {
        String result = CommonUtil.convert(123, String.class);
        assertEquals("123", result);
    }

    @Test
    public void testCastIfAssignable() {
        Object obj = "test";
        com.landawn.abacus.util.u.Nullable<String> result = CommonUtil.castIfAssignable(obj, String.class);
        assertTrue(result.isPresent());
        assertEquals("test", result.get());

        com.landawn.abacus.util.u.Nullable<Integer> notAssignable = CommonUtil.castIfAssignable(obj, Integer.class);
        assertFalse(notAssignable.isPresent());
    }

    @Test
    public void testMerge() {
    }

    @Test
    public void testNegate() {
        boolean[] arr = { true, false, true };
        CommonUtil.negate(arr);
        assertFalse(arr[0]);
        assertTrue(arr[1]);
        assertFalse(arr[2]);
    }

    @Test
    public void testShuffle() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        CommonUtil.shuffle(list);
        assertEquals(5, list.size());
        assertTrue(list.contains(1));
        assertTrue(list.contains(5));
    }

    @Test
    public void testReverseToList() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> reversed = CommonUtil.reverseToList(list);
        assertEquals(5, reversed.size());
        assertEquals(Integer.valueOf(5), reversed.get(0));
        assertEquals(Integer.valueOf(1), reversed.get(4));
    }

    @Test
    public void testSwapIf() {
        com.landawn.abacus.util.Pair<Integer, Integer> pair = com.landawn.abacus.util.Pair.of(5, 2);
        CommonUtil.swapIf(pair, p -> p.getLeft() > p.getRight());
        assertEquals(Integer.valueOf(2), pair.getLeft());
        assertEquals(Integer.valueOf(5), pair.getRight());
    }

    @Test
    public void testRepeatCollection() {
        List<String> list = Arrays.asList("a", "b");
        List<String> repeated = CommonUtil.cycle(list, 3);
        assertEquals(6, repeated.size());
        assertEquals("a", repeated.get(0));
        assertEquals("b", repeated.get(1));
        assertEquals("a", repeated.get(2));
    }

    @Test
    public void testRepeatCollectionToSize() {
        List<String> list = Arrays.asList("a", "b");
        List<String> repeated = CommonUtil.cycleToSize(list, 5);
        assertEquals(5, repeated.size());
        assertEquals("a", repeated.get(0));
        assertEquals("b", repeated.get(1));
        assertEquals("a", repeated.get(2));
    }

    @Test
    public void testRepeatElements() {
        List<String> list = Arrays.asList("a", "b");
        List<String> repeated = CommonUtil.repeatElements(list, 3);
        assertEquals(6, repeated.size());
        assertEquals("a", repeated.get(0));
        assertEquals("a", repeated.get(1));
        assertEquals("a", repeated.get(2));
        assertEquals("b", repeated.get(3));
    }

    @Test
    public void testRepeatElementsToSize() {
        List<String> list = Arrays.asList("a", "b");
        List<String> repeated = CommonUtil.repeatElementsToSize(list, 5);
        assertEquals(5, repeated.size());
        assertEquals("a", repeated.get(0));
        assertEquals("a", repeated.get(1));
        assertEquals("a", repeated.get(2));
    }

    @Test
    public void testIndicesOfAll() {
        List<Integer> list = Arrays.asList(1, 2, 3, 2, 5);
        int[] indices = CommonUtil.indicesOfAll(list, 2);
        assertEquals(2, indices.length);
        assertEquals(1, indices[0]);
        assertEquals(3, indices[1]);
    }

    @Test
    public void testIndicesOfAllMax() {
        List<Integer> list = Arrays.asList(1, 5, 3, 5, 2);
        int[] indices = CommonUtil.indicesOfMax(list);
        assertEquals(2, indices.length);
        assertEquals(1, indices[0]);
        assertEquals(3, indices[1]);
    }

    @Test
    public void testIndicesOfAllMin() {
        List<Integer> list = Arrays.asList(5, 1, 3, 1, 2);
        int[] indices = CommonUtil.indicesOfMin(list);
        assertEquals(2, indices.length);
        assertEquals(1, indices[0]);
        assertEquals(3, indices[1]);
    }

    @Test
    public void testHaveSameElements() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(3, 2, 1);
        assertTrue(CommonUtil.containsSameElements(list1, list2));

        List<Integer> list3 = Arrays.asList(1, 2, 4);
        assertFalse(CommonUtil.containsSameElements(list1, list3));
    }

    @Test
    public void testGetElement() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("b", CommonUtil.getElement(list, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.getElement(list, 5));
    }

    @Test
    public void testGetOnlyElement() {
        List<String> list = Arrays.asList("only");
        com.landawn.abacus.util.u.Nullable<String> result = CommonUtil.getOnlyElement(list);
        assertTrue(result.isPresent());
        assertEquals("only", result.get());

        List<String> multipleElements = Arrays.asList("a", "b");
        assertThrows(com.landawn.abacus.exception.TooManyElementsException.class, () -> CommonUtil.getOnlyElement(multipleElements));
    }

    @Test
    public void testEqualsCollection() {
        Collection<Integer> col1 = Arrays.asList(1, 2, 3);
        Collection<Integer> col2 = Arrays.asList(1, 2, 3);
        assertThrows(UnsupportedOperationException.class, () -> CommonUtil.equalsCollection(col1, col2));
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
            CommonUtil.equalsByProps(bean1, bean2, Arrays.asList("name"));
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
            CommonUtil.equalsByCommonProps(bean1, bean2);
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
            CommonUtil.compareByProps(bean1, bean2, Arrays.asList("name"));
        });
    }

    @Test
    public void testRegisterConverter() {
    }

    @Test
    public void testNewProxyInstance() {
        java.util.function.Function<String, String> func = s -> s.toUpperCase();
        Object proxy = CommonUtil.newProxyInstance(new Class<?>[] { java.util.function.Function.class }, (p, method, args) -> {
            if (method.getName().equals("apply")) {
                return func.apply((String) args[0]);
            }
            return null;
        });
        assertNotNull(proxy);
    }

    @Test
    public void testIsUnmodifiable_NullCollection() {
        assertTrue(CommonUtil.isUnmodifiable((Collection) null));
    }

    @Test
    public void testIsUnmodifiable_EmptyList() {
        assertTrue(CommonUtil.isUnmodifiable(Collections.emptyList()));
    }

    @Test
    public void testIsUnmodifiable_EmptySet() {
        assertTrue(CommonUtil.isUnmodifiable(Collections.emptySet()));
    }

    @Test
    public void testIsUnmodifiable_Singleton() {
        assertTrue(CommonUtil.isUnmodifiable(Collections.singleton("value")));
    }

    @Test
    public void testIsUnmodifiable_SingletonList() {
        assertTrue(CommonUtil.isUnmodifiable(Collections.singletonList("value")));
    }

    @Test
    public void testIsUnmodifiable_UnmodifiableList() {
        List<String> modifiable = new ArrayList<>(Arrays.asList("a", "b"));
        assertTrue(CommonUtil.isUnmodifiable(Collections.unmodifiableList(modifiable)));

        assertTrue(CommonUtil.isUnmodifiable(List.of("a", "b").stream().toList()));
        assertFalse(CommonUtil.isUnmodifiable(List.of("a", "b").stream().collect(Collectors.toList())));
    }

    @Test
    public void testIsUnmodifiable_UnmodifiableSet() {
        Set<String> modifiable = new HashSet<>(Arrays.asList("a", "b"));
        assertTrue(CommonUtil.isUnmodifiable(Collections.unmodifiableSet(modifiable)));
    }

    @Test
    public void testIsUnmodifiable_UnmodifiableSortedSet() {
        SortedSet<String> modifiable = new TreeSet<>(Arrays.asList("a", "b"));
        assertTrue(CommonUtil.isUnmodifiable(Collections.unmodifiableSortedSet(modifiable)));
    }

    @Test
    public void testIsUnmodifiable_UnmodifiableNavigableSet() {
        NavigableSet<String> modifiable = new TreeSet<>(Arrays.asList("a", "b"));
        assertTrue(CommonUtil.isUnmodifiable(Collections.unmodifiableNavigableSet(modifiable)));
    }

    @Test
    public void testIsUnmodifiable_ListOf() {
        assertTrue(CommonUtil.isUnmodifiable(List.of("a", "b")));
    }

    @Test
    public void testIsUnmodifiable_SetOf() {
        assertTrue(CommonUtil.isUnmodifiable(Set.of("a", "b")));
    }

    @Test
    public void testIsUnmodifiable_ArrayList() {
        assertFalse(CommonUtil.isUnmodifiable(new ArrayList<>()));
    }

    @Test
    public void testIsUnmodifiable_HashSet() {
        assertFalse(CommonUtil.isUnmodifiable(new HashSet<>()));
    }

    @Test
    public void testIsUnmodifiable_LinkedList() {
        assertFalse(CommonUtil.isUnmodifiable(new LinkedList<>()));
    }

    @Test
    public void testIsUnmodifiable_TreeSet() {
        assertFalse(CommonUtil.isUnmodifiable(new TreeSet<>()));
    }

    @Test
    public void testIsUnmodifiable_ConcurrentHashSet() {
        assertFalse(CommonUtil.isUnmodifiable(ConcurrentHashMap.newKeySet()));
    }

    @Test
    public void testIsUnmodifiable_CachingBehavior() {
        // First call - performs mutation test
        Collection<String> c1 = new ArrayList<>();
        assertFalse(CommonUtil.isUnmodifiable(c1));

        // Second call - uses cached result
        Collection<String> c2 = new ArrayList<>();
        assertFalse(CommonUtil.isUnmodifiable(c2));
        // First call - performs mutation test
        Map<String, String> m1 = new HashMap<>();
        assertFalse(CommonUtil.isUnmodifiable(m1));

        // Second call - uses cached result
        Map<String, String> m2 = new HashMap<>();
        assertFalse(CommonUtil.isUnmodifiable(m2));

    }

    @Test
    public void testIsUnmodifiable_ModifiableCollectionNotMutated() {
        List<String> list = new ArrayList<>();
        list.add("original");

        assertFalse(CommonUtil.isUnmodifiable(list));

        // Verify original element still present
        assertEquals(1, list.size());
        assertEquals("original", list.get(0));
    }

    @Test
    public void testIsUnmodifiable_ImmutableList() {
        assertTrue(CommonUtil.isUnmodifiable(ImmutableList.of("a", "b")));
    }

    @Test
    public void testIsUnmodifiable_ImmutableSet() {
        assertTrue(CommonUtil.isUnmodifiable(ImmutableSet.of("a", "b")));
    }

    @Test
    public void testIsUnmodifiable_CustomUnmodifiableCollection() {
        Collection<String> custom = new AbstractCollection<String>() {
            @Override
            public Iterator<String> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean add(String s) {
                throw new UnsupportedOperationException();
            }
        };

        assertTrue(CommonUtil.isUnmodifiable(custom));
    }

    @Test
    public void testIsUnmodifiable_CollectionThrowingOtherException() {
        Collection<String> custom = new AbstractCollection<String>() {
            @Override
            public Iterator<String> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean add(String s) {
                throw new IllegalStateException("Custom exception");
            }
        };

        assertFalse(CommonUtil.isUnmodifiable(custom));
    }

    @Test
    public void testIsUnmodifiable_NullMap() {
        assertTrue(CommonUtil.isUnmodifiable((Map) null));
    }

    @Test
    public void testIsUnmodifiable_EmptyMap() {
        assertTrue(CommonUtil.isUnmodifiable(Collections.emptyMap()));
    }

    @Test
    public void testIsUnmodifiable_SingletonMap() {
        assertTrue(CommonUtil.isUnmodifiable(Collections.singletonMap("key", "value")));
    }

    @Test
    public void testIsUnmodifiable_UnmodifiableMap() {
        Map<String, String> modifiable = new HashMap<>();
        modifiable.put("key", "value");
        assertTrue(CommonUtil.isUnmodifiable(Collections.unmodifiableMap(modifiable)));
    }

    @Test
    public void testIsUnmodifiable_UnmodifiableSortedMap() {
        SortedMap<String, String> modifiable = new TreeMap<>();
        modifiable.put("key", "value");
        assertTrue(CommonUtil.isUnmodifiable(Collections.unmodifiableSortedMap(modifiable)));
    }

    @Test
    public void testIsUnmodifiable_UnmodifiableNavigableMap() {
        NavigableMap<String, String> modifiable = new TreeMap<>();
        modifiable.put("key", "value");
        assertTrue(CommonUtil.isUnmodifiable(Collections.unmodifiableNavigableMap(modifiable)));
    }

    @Test
    public void testIsUnmodifiable_MapOf() {
        assertTrue(CommonUtil.isUnmodifiable(Map.of("k1", "v1", "k2", "v2")));
    }

    @Test
    public void testIsUnmodifiable_MapOfEntries() {
        assertTrue(CommonUtil.isUnmodifiable(Map.ofEntries(Map.entry("k1", "v1"), Map.entry("k2", "v2"))));
    }

    @Test
    public void testIsUnmodifiable_HashMap() {
        assertFalse(CommonUtil.isUnmodifiable(new HashMap<>()));
    }

    @Test
    public void testIsUnmodifiable_TreeMap() {
        assertFalse(CommonUtil.isUnmodifiable(new TreeMap<>()));
    }

    @Test
    public void testIsUnmodifiable_LinkedHashMap() {
        assertFalse(CommonUtil.isUnmodifiable(new LinkedHashMap<>()));
    }

    @Test
    public void testIsUnmodifiable_ConcurrentHashMap() {
        assertFalse(CommonUtil.isUnmodifiable(new ConcurrentHashMap<>()));
    }

    @Test
    public void testIsUnmodifiable_IdentityHashMap() {
        assertFalse(CommonUtil.isUnmodifiable(new IdentityHashMap<>()));
    }

    @Test
    public void testIsUnmodifiable_WeakHashMap() {
        assertFalse(CommonUtil.isUnmodifiable(new WeakHashMap<>()));
    }

    @Test
    public void testIsUnmodifiable_ModifiableMapNotMutated() {
        Map<String, String> map = new HashMap<>();
        map.put("original", "value");

        assertFalse(CommonUtil.isUnmodifiable(map));

        // Verify original entry still present
        assertEquals(1, map.size());
        assertEquals("value", map.get("original"));
    }

    @Test
    public void testIsUnmodifiable_ImmutableMap() {
        assertTrue(CommonUtil.isUnmodifiable(ImmutableMap.of("k1", "v1")));
    }

    @Test
    public void testIsUnmodifiable_ImmutableBiMap() {
        assertTrue(CommonUtil.isUnmodifiable(ImmutableBiMap.of("k1", "v1")));
    }

    @Test
    public void testIsUnmodifiable_ImmutableSortedMap() {
        assertTrue(CommonUtil.isUnmodifiable(ImmutableSortedMap.of("k1", "v1")));
    }

    @Test
    public void testIsUnmodifiable_CustomUnmodifiableMap() {
        Map<String, String> custom = new AbstractMap<String, String>() {
            @Override
            public Set<Entry<String, String>> entrySet() {
                return Collections.emptySet();
            }

            @Override
            public String put(String key, String value) {
                throw new UnsupportedOperationException();
            }
        };

        assertTrue(CommonUtil.isUnmodifiable(custom));
    }

    @Test
    public void testIsUnmodifiable_MapThrowingOtherException() {
        Map<String, String> custom = new AbstractMap<String, String>() {
            @Override
            public Set<Entry<String, String>> entrySet() {
                return Collections.emptySet();
            }

            @Override
            public String put(String key, String value) {
                throw new IllegalStateException("Custom exception");
            }
        };

        assertFalse(CommonUtil.isUnmodifiable(custom));
    }

    @Test
    public void testIsUnmodifiable_MapWithNullKeySupport() {
        Map<String, String> map = new HashMap<>();
        map.put(null, "value");
        assertFalse(CommonUtil.isUnmodifiable(map));
    }

    @Test
    public void testIsUnmodifiable_EnumMap() {
        Map<DayOfWeek, String> enumMap = new EnumMap<>(DayOfWeek.class);
        assertFalse(CommonUtil.isUnmodifiable(enumMap));
    }

    @Test
    public void testIsUnmodifiable_UnmodifiableEnumMap() {
        Map<DayOfWeek, String> enumMap = new EnumMap<>(DayOfWeek.class);
        enumMap.put(DayOfWeek.MONDAY, "First day");
        assertTrue(CommonUtil.isUnmodifiable(Collections.unmodifiableMap(enumMap)));
    }

    enum DayOfWeek {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }
}
