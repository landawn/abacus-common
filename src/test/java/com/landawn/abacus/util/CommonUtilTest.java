package com.landawn.abacus.util;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.u.OptionalInt;

@Tag("2025")
public class CommonUtilTest extends TestBase {

    enum DayOfWeek {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    private static class TestBean {
        private String name;
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    private static class MyClass {
    }

    private static class Person {
        private final String name;
        private final int age;

        private Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }

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
    public void testCheckFromToIndex_NegativeLength() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkFromToIndex(0, 0, -1));

        assertTrue(exception.getMessage().contains("negative"));
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
        assertTrue(list instanceof ImmutableList);
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));

        List<String> listWithNull = CommonUtil.asList("a", null, "c");
        assertEquals(3, listWithNull.size());
        assertNull(listWithNull.get(1));

        assertThrows(UnsupportedOperationException.class, () -> list.add("d"));
    }

    @Test
    public void testAsSet() {
        Set<String> set = CommonUtil.asSet("a", "b", "c", "a");
        assertTrue(set instanceof ImmutableSet);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));

        Set<String> setWithNull = CommonUtil.asSet("a", null, "a");
        assertEquals(2, setWithNull.size());
        assertTrue(setWithNull.contains(null));

        assertThrows(UnsupportedOperationException.class, () -> set.add("d"));
    }

    //     @Test
    //     public void testAsLinkedHashMap() {
    //         Map<String, Integer> map = CommonUtil.asLinkedHashMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9);
    //         assertEquals(9, map.size());
    //         assertEquals(Integer.valueOf(1), map.get("a"));
    //         assertEquals(Integer.valueOf(2), map.get("b"));
    //         assertEquals(Integer.valueOf(9), map.get("i"));
    //     }

    //

    @Test
    public void testToLinkedHashMap() {
        Map<String, Integer> map = CommonUtil.toLinkedHashMap("a", 1);
        assertTrue(map instanceof LinkedHashMap);
        assertEquals(1, map.size());

        map = CommonUtil.toLinkedHashMap("a", 1, "b", 2);
        assertEquals(2, map.size());

        map = CommonUtil.toLinkedHashMap("a", 1, "b", 2, "c", 3);
        assertEquals(3, map.size());

        map = CommonUtil.toLinkedHashMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
        assertEquals(5, map.size());

        Iterator<String> iter = map.keySet().iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertEquals("e", iter.next());

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.toLinkedHashMap("a", 1, "b"));
    }

    @Test
    public void testAsMap() {
        Map<String, Integer> map = CommonUtil.asMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9);
        assertTrue(map instanceof ImmutableMap);
        assertEquals(9, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertEquals(Integer.valueOf(3), map.get("c"));
        assertEquals(Integer.valueOf(9), map.get("i"));
        assertThrows(UnsupportedOperationException.class, () -> map.put("j", 10));
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
        java.util.Deque<String> deque = CommonUtil.toDeque("a", "b", "c");
        assertEquals(3, deque.size());
        assertEquals("a", deque.getFirst());
        assertEquals("c", deque.getLast());
    }

    @Test
    public void testAsArrayDeque() {
        java.util.ArrayDeque<String> deque = CommonUtil.toArrayDeque("a", "b", "c");
        assertEquals(3, deque.size());
        assertEquals("a", deque.getFirst());
        assertEquals("c", deque.getLast());
    }

    @Test
    public void testAsQueue() {
        java.util.Queue<String> queue = CommonUtil.toQueue("a", "b", "c");
        assertEquals(3, queue.size());
        assertEquals("a", queue.peek());
    }

    @Test
    public void testAsPriorityQueue() {
        java.util.PriorityQueue<Integer> queue = CommonUtil.toPriorityQueue(3, 1, 2);
        assertEquals(3, queue.size());
        assertEquals(Integer.valueOf(1), queue.poll());
        assertEquals(Integer.valueOf(2), queue.poll());
        assertEquals(Integer.valueOf(3), queue.poll());
    }

    @Test
    public void testAsNavigableSet() {
        java.util.NavigableSet<Integer> set = CommonUtil.toNavigableSet(3, 1, 2);
        assertEquals(3, set.size());
        assertEquals(Integer.valueOf(1), set.first());
        assertEquals(Integer.valueOf(3), set.last());
    }

    @Test
    public void testAsSortedSet() {
        java.util.SortedSet<Integer> set = CommonUtil.toSortedSet(3, 1, 2);
        assertEquals(3, set.size());
        assertEquals(Integer.valueOf(1), set.first());
        assertEquals(Integer.valueOf(3), set.last());
    }

    @Test
    public void testAsArrayBlockingQueue() {
        java.util.concurrent.ArrayBlockingQueue<String> queue = CommonUtil.toArrayBlockingQueue("a", "b");
        assertEquals(2, queue.size());
        assertEquals("a", queue.peek());
    }

    @Test
    public void testAsLinkedBlockingQueue() {
        java.util.concurrent.LinkedBlockingQueue<String> queue = CommonUtil.toLinkedBlockingQueue("a", "b");
        assertEquals(2, queue.size());
        assertEquals("a", queue.peek());
    }

    @Test
    public void testAsLinkedBlockingDeque() {
        java.util.concurrent.LinkedBlockingDeque<String> deque = CommonUtil.toLinkedBlockingDeque("a", "b");
        assertEquals(2, deque.size());
        assertEquals("a", deque.getFirst());
    }

    @Test
    public void testAsConcurrentLinkedQueue() {
        java.util.concurrent.ConcurrentLinkedQueue<String> queue = CommonUtil.toConcurrentLinkedQueue("a", "b");
        assertEquals(2, queue.size());
        assertEquals("a", queue.peek());
    }

    @Test
    public void testAsConcurrentLinkedDeque() {
        java.util.concurrent.ConcurrentLinkedDeque<String> deque = CommonUtil.toConcurrentLinkedDeque("a", "b");
        assertEquals(2, deque.size());
        assertEquals("a", deque.getFirst());
    }

    @Test
    public void testAsDelayQueue() {
        java.util.concurrent.DelayQueue<java.util.concurrent.Delayed> queue = CommonUtil.toDelayQueue();
        assertNotNull(queue);
        assertTrue(queue.isEmpty());
    }

    @Test
    public void testAsMultiset() {
        com.landawn.abacus.util.Multiset<String> multiset = CommonUtil.toMultiset("a", "b", "a", "c");
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
    public void testGreaterThanOrEqual() {
        assertTrue(CommonUtil.greaterThanOrEqual(5, 3));
        assertTrue(CommonUtil.greaterThanOrEqual(3, 3));
        assertFalse(CommonUtil.greaterThanOrEqual(2, 3));
    }

    @Test
    public void testLessThan() {
        assertTrue(CommonUtil.lessThan(3, 5));
        assertFalse(CommonUtil.lessThan(5, 3));
        assertFalse(CommonUtil.lessThan(3, 3));
    }

    @Test
    public void testLessThanOrEqual() {
        assertTrue(CommonUtil.lessThanOrEqual(3, 5));
        assertTrue(CommonUtil.lessThanOrEqual(3, 3));
        assertFalse(CommonUtil.lessThanOrEqual(5, 3));
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
        assertEquals(0, Collections.indexOfSubList(CommonUtil.emptyList(), CommonUtil.emptyList()));
        assertEquals(0, CommonUtil.indexOfSubList(CommonUtil.emptyList(), CommonUtil.emptyList()));
        assertEquals(0, Index.ofSubList(CommonUtil.emptyList(), CommonUtil.emptyList()).orElseThrow());
        assertEquals(0, Index.ofSubList(CommonUtil.emptyList(), 0, CommonUtil.emptyList()).orElseThrow());
        assertEquals(Collections.indexOfSubList(CommonUtil.emptyList(), CommonUtil.emptyList()),
                CommonUtil.indexOfSubList(CommonUtil.emptyList(), CommonUtil.emptyList()));
    }

    @Test
    public void testLastIndexOfSubList() {
        List<Integer> list = Arrays.asList(1, 2, 3, 2, 3, 4);
        List<Integer> subList = Arrays.asList(2, 3);
        assertEquals(3, CommonUtil.lastIndexOfSubList(list, subList));

        assertEquals(0, Strings.lastIndexOf("", ""));
        assertEquals(0, "".lastIndexOf(""));
        assertEquals(0, Collections.lastIndexOfSubList(CommonUtil.emptyList(), CommonUtil.emptyList()));
        assertEquals(0, CommonUtil.lastIndexOfSubList(CommonUtil.emptyList(), CommonUtil.emptyList()));
        assertEquals(0, Index.lastOfSubList(CommonUtil.emptyList(), CommonUtil.emptyList()).orElseThrow());
        assertEquals(0, Index.lastOfSubList(CommonUtil.emptyList(), 0, CommonUtil.emptyList()).orElseThrow());
        assertEquals(Collections.lastIndexOfSubList(CommonUtil.emptyList(), CommonUtil.emptyList()),
                CommonUtil.lastIndexOfSubList(CommonUtil.emptyList(), CommonUtil.emptyList()));
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
        assertEquals(String.class, type.javaType());
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
        Collection<String> custom = new AbstractCollection<>() {
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
        Collection<String> custom = new AbstractCollection<>() {
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
        Map<String, String> custom = new AbstractMap<>() {
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
        Map<String, String> custom = new AbstractMap<>() {
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

    @Test
    public void testNullToEmpty_List() {
        List<String> nonNullList = new ArrayList<>();
        nonNullList.add("a");
        assertSame(nonNullList, CommonUtil.nullToEmpty(nonNullList));

        List<?> emptyList = CommonUtil.nullToEmpty((List<?>) null);
        assertTrue(emptyList.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyList.add(null));
    }

    @Test
    public void testNullToEmpty_Set() {
        Set<String> nonNullSet = new HashSet<>();
        nonNullSet.add("a");
        assertSame(nonNullSet, CommonUtil.nullToEmpty(nonNullSet));

        Set<?> emptySet = CommonUtil.nullToEmpty((Set<?>) null);
        assertTrue(emptySet.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptySet.add(null));
    }

    @Test
    public void testNullToEmpty_SortedSet() {
        SortedSet<String> nonNullSet = new TreeSet<>();
        nonNullSet.add("a");
        assertSame(nonNullSet, CommonUtil.nullToEmpty(nonNullSet));
        SortedSet<?> emptySet = CommonUtil.nullToEmpty((SortedSet<?>) null);
        assertTrue(emptySet.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptySet.add(null));
    }

    @Test
    public void testNullToEmpty_NavigableSet() {
        NavigableSet<String> nonNullSet = new TreeSet<>();
        nonNullSet.add("a");
        assertSame(nonNullSet, CommonUtil.nullToEmpty(nonNullSet));
        NavigableSet<?> emptySet = CommonUtil.nullToEmpty((NavigableSet<?>) null);
        assertTrue(emptySet.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptySet.add(null));
    }

    @Test
    public void testNullToEmpty_Collection() {
        Collection<String> nonNullCollection = new ArrayList<>();
        nonNullCollection.add("a");
        assertSame(nonNullCollection, CommonUtil.nullToEmpty(nonNullCollection));
        Collection<?> emptyCollection = CommonUtil.nullToEmpty((Collection<?>) null);
        assertTrue(emptyCollection.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyCollection.add(null));
    }

    @Test
    public void testNullToEmpty_Map() {
        Map<String, String> nonNullMap = new HashMap<>();
        nonNullMap.put("a", "b");
        assertSame(nonNullMap, CommonUtil.nullToEmpty(nonNullMap));
        Map<?, ?> emptyMap = CommonUtil.nullToEmpty((Map<?, ?>) null);
        assertTrue(emptyMap.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyMap.put(null, null));
    }

    @Test
    public void testNullToEmpty_SortedMap() {
        SortedMap<String, String> nonNullMap = new TreeMap<>();
        nonNullMap.put("a", "b");
        assertSame(nonNullMap, CommonUtil.nullToEmpty(nonNullMap));
        SortedMap<?, ?> emptyMap = CommonUtil.nullToEmpty((SortedMap<?, ?>) null);
        assertTrue(emptyMap.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyMap.put(null, null));
    }

    @Test
    public void testNullToEmpty_NavigableMap() {
        NavigableMap<String, String> nonNullMap = new TreeMap<>();
        nonNullMap.put("a", "b");
        assertSame(nonNullMap, CommonUtil.nullToEmpty(nonNullMap));
        NavigableMap<?, ?> emptyMap = CommonUtil.nullToEmpty((NavigableMap<?, ?>) null);
        assertTrue(emptyMap.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyMap.put(null, null));
    }

    @Test
    public void testNullToEmpty_Iterator() {
        List<String> list = Arrays.asList("a");
        Iterator<String> nonEmptyIterator = list.iterator();
        assertSame(nonEmptyIterator, CommonUtil.nullToEmpty(nonEmptyIterator));

        Iterator<?> emptyIter = CommonUtil.nullToEmpty((Iterator<?>) null);
        assertFalse(emptyIter.hasNext());
        assertThrows(NoSuchElementException.class, emptyIter::next);
    }

    @Test
    public void testNullToEmpty_ListIterator() {
        List<String> list = Arrays.asList("a");
        ListIterator<String> nonEmptyListIterator = list.listIterator();
        assertSame(nonEmptyListIterator, CommonUtil.nullToEmpty(nonEmptyListIterator));

        ListIterator<?> emptyIter = CommonUtil.nullToEmpty((ListIterator<?>) null);
        assertFalse(emptyIter.hasNext());
        assertFalse(emptyIter.hasPrevious());
        assertThrows(NoSuchElementException.class, emptyIter::next);
        assertThrows(NoSuchElementException.class, emptyIter::previous);
    }

    @Test
    public void testNullToEmpty_booleanArray() {
        assertArrayEquals(CommonUtil.EMPTY_BOOLEAN_ARRAY, CommonUtil.nullToEmpty((boolean[]) null));
        boolean[] arr = { true, false };
        assertSame(arr, CommonUtil.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_charArray() {
        assertArrayEquals(CommonUtil.EMPTY_CHAR_ARRAY, CommonUtil.nullToEmpty((char[]) null));
        char[] arr = { 'a', 'b' };
        assertSame(arr, CommonUtil.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_byteArray() {
        assertArrayEquals(CommonUtil.EMPTY_BYTE_ARRAY, CommonUtil.nullToEmpty((byte[]) null));
        byte[] arr = { 1, 2 };
        assertSame(arr, CommonUtil.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_shortArray() {
        assertArrayEquals(CommonUtil.EMPTY_SHORT_ARRAY, CommonUtil.nullToEmpty((short[]) null));
        short[] arr = { 1, 2 };
        assertSame(arr, CommonUtil.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_intArray() {
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, CommonUtil.nullToEmpty((int[]) null));
        int[] arr = { 1, 2 };
        assertSame(arr, CommonUtil.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_longArray() {
        assertArrayEquals(CommonUtil.EMPTY_LONG_ARRAY, CommonUtil.nullToEmpty((long[]) null));
        long[] arr = { 1L, 2L };
        assertSame(arr, CommonUtil.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_floatArray() {
        assertArrayEquals(CommonUtil.EMPTY_FLOAT_ARRAY, CommonUtil.nullToEmpty((float[]) null), 0.0f);
        float[] arr = { 1.0f, 2.0f };
        assertSame(arr, CommonUtil.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_doubleArray() {
        assertArrayEquals(CommonUtil.EMPTY_DOUBLE_ARRAY, CommonUtil.nullToEmpty((double[]) null), 0.0);
        double[] arr = { 1.0, 2.0 };
        assertSame(arr, CommonUtil.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_BigIntegerArray() {
        assertArrayEquals(CommonUtil.EMPTY_BIG_INTEGER_ARRAY, CommonUtil.nullToEmpty((BigInteger[]) null));
        BigInteger[] arr = { BigInteger.ONE };
        assertSame(arr, CommonUtil.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_BigDecimalArray() {
        assertArrayEquals(CommonUtil.EMPTY_BIG_DECIMAL_ARRAY, CommonUtil.nullToEmpty((BigDecimal[]) null));
        BigDecimal[] arr = { BigDecimal.ONE };
        assertSame(arr, CommonUtil.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_StringArray() {
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, CommonUtil.nullToEmpty((String[]) null));
        String[] arr = { "a", "b" };
        assertSame(arr, CommonUtil.nullToEmpty(arr));
        String[] arrWithNull = { "a", null };
        String[] resultArrWithNull = CommonUtil.nullToEmpty(arrWithNull);
        assertSame(arrWithNull, resultArrWithNull);
        assertNull(resultArrWithNull[1]);
    }

    @Test
    public void testNullToEmpty_JavaUtilDateArray() {
        assertArrayEquals(CommonUtil.EMPTY_JU_DATE_ARRAY, CommonUtil.nullToEmpty((java.util.Date[]) null));
        java.util.Date[] arr = { new java.util.Date() };
        assertSame(arr, CommonUtil.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_JavaSqlDateArray() {
        assertArrayEquals(CommonUtil.EMPTY_DATE_ARRAY, CommonUtil.nullToEmpty((java.sql.Date[]) null));
        java.sql.Date[] arr = { new java.sql.Date(System.currentTimeMillis()) };
        assertSame(arr, CommonUtil.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_JavaSqlTimeArray() {
        assertArrayEquals(CommonUtil.EMPTY_TIME_ARRAY, CommonUtil.nullToEmpty((java.sql.Time[]) null));
        java.sql.Time[] arr = { new java.sql.Time(System.currentTimeMillis()) };
        assertSame(arr, CommonUtil.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_JavaSqlTimestampArray() {
        assertArrayEquals(CommonUtil.EMPTY_TIMESTAMP_ARRAY, CommonUtil.nullToEmpty((java.sql.Timestamp[]) null));
        java.sql.Timestamp[] arr = { new java.sql.Timestamp(System.currentTimeMillis()) };
        assertSame(arr, CommonUtil.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_CalendarArray() {
        assertArrayEquals(CommonUtil.EMPTY_CALENDAR_ARRAY, CommonUtil.nullToEmpty((Calendar[]) null));
        Calendar[] arr = { Calendar.getInstance() };
        assertSame(arr, CommonUtil.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_ObjectArray() {
        assertArrayEquals(CommonUtil.EMPTY_OBJECT_ARRAY, CommonUtil.nullToEmpty((Object[]) null));
        Object[] arr = { new Object(), "string" };
        assertSame(arr, CommonUtil.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_ImmutableCollection() {
        com.landawn.abacus.util.ImmutableList<String> nonNullList = com.landawn.abacus.util.ImmutableList.of("a");
        assertSame(nonNullList, CommonUtil.nullToEmpty(nonNullList));

        com.landawn.abacus.util.ImmutableCollection<?> emptyCol = CommonUtil.nullToEmpty((com.landawn.abacus.util.ImmutableCollection<?>) null);
        assertTrue(emptyCol.isEmpty());
    }

    @Test
    public void testNullToEmpty_ImmutableList() {
        com.landawn.abacus.util.ImmutableList<String> nonNullList = com.landawn.abacus.util.ImmutableList.of("a");
        assertSame(nonNullList, CommonUtil.nullToEmpty(nonNullList));
        com.landawn.abacus.util.ImmutableList<?> emptyList = CommonUtil.nullToEmpty((com.landawn.abacus.util.ImmutableList<?>) null);
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testNullToEmpty_ImmutableSet() {
        com.landawn.abacus.util.ImmutableSet<String> nonNullSet = com.landawn.abacus.util.ImmutableSet.of("a");
        assertSame(nonNullSet, CommonUtil.nullToEmpty(nonNullSet));
        com.landawn.abacus.util.ImmutableSet<?> emptySet = CommonUtil.nullToEmpty((com.landawn.abacus.util.ImmutableSet<?>) null);
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testNullToEmpty_ImmutableSortedSet() {
        com.landawn.abacus.util.ImmutableSortedSet<String> nonNullSet = com.landawn.abacus.util.ImmutableSortedSet.of("a");
        assertSame(nonNullSet, CommonUtil.nullToEmpty(nonNullSet));
        com.landawn.abacus.util.ImmutableSortedSet<?> emptySet = CommonUtil.nullToEmpty((com.landawn.abacus.util.ImmutableSortedSet<?>) null);
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testNullToEmpty_ImmutableNavigableSet() {
        com.landawn.abacus.util.ImmutableNavigableSet<String> nonNullSet = com.landawn.abacus.util.ImmutableNavigableSet.of("a");
        assertSame(nonNullSet, CommonUtil.nullToEmpty(nonNullSet));
        com.landawn.abacus.util.ImmutableNavigableSet<?> emptySet = CommonUtil.nullToEmpty((com.landawn.abacus.util.ImmutableNavigableSet<?>) null);
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testNullToEmpty_ImmutableMap() {
        com.landawn.abacus.util.ImmutableMap<String, String> nonNullMap = com.landawn.abacus.util.ImmutableMap.of("a", "b");
        assertSame(nonNullMap, CommonUtil.nullToEmpty(nonNullMap));
        com.landawn.abacus.util.ImmutableMap<?, ?> emptyMap = CommonUtil.nullToEmpty((com.landawn.abacus.util.ImmutableMap<?, ?>) null);
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testNullToEmpty_ImmutableSortedMap() {
        com.landawn.abacus.util.ImmutableSortedMap<String, String> nonNullMap = com.landawn.abacus.util.ImmutableSortedMap.of("a", "b");
        assertSame(nonNullMap, CommonUtil.nullToEmpty(nonNullMap));
        com.landawn.abacus.util.ImmutableSortedMap<?, ?> emptyMap = CommonUtil.nullToEmpty((com.landawn.abacus.util.ImmutableSortedMap<?, ?>) null);
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testNullToEmpty_ImmutableNavigableMap() {
        com.landawn.abacus.util.ImmutableNavigableMap<String, String> nonNullMap = com.landawn.abacus.util.ImmutableNavigableMap.of("a", "b");
        assertSame(nonNullMap, CommonUtil.nullToEmpty(nonNullMap));
        com.landawn.abacus.util.ImmutableNavigableMap<?, ?> emptyMap = CommonUtil.nullToEmpty((com.landawn.abacus.util.ImmutableNavigableMap<?, ?>) null);
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testNullToEmpty_ImmutableBiMap() {
        com.landawn.abacus.util.ImmutableBiMap<String, String> nonNullMap = com.landawn.abacus.util.ImmutableBiMap.of("a", "b");
        assertSame(nonNullMap, CommonUtil.nullToEmpty(nonNullMap));
        com.landawn.abacus.util.ImmutableBiMap<?, ?> emptyMap = CommonUtil.nullToEmpty((com.landawn.abacus.util.ImmutableBiMap<?, ?>) null);
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testDefaultIfNull_Boolean_noDefaultArg() {
        assertFalse(CommonUtil.defaultIfNull((Boolean) null));
        assertTrue(CommonUtil.defaultIfNull(Boolean.TRUE));
        assertFalse(CommonUtil.defaultIfNull(Boolean.FALSE));
    }

    @Test
    public void testDefaultIfNull_Boolean_withDefaultArg() {
        assertTrue(CommonUtil.defaultIfNull(null, true));
        assertFalse(CommonUtil.defaultIfNull(null, false));
        assertTrue(CommonUtil.defaultIfNull(Boolean.TRUE, false));
        assertFalse(CommonUtil.defaultIfNull(Boolean.FALSE, true));
    }

    @Test
    public void testDefaultIfNull_Double_noDefaultArg() {
        assertEquals(0.0, CommonUtil.defaultIfNull((Double) null), 0.0);
        assertEquals(10.0, CommonUtil.defaultIfNull(Double.valueOf(10.0)), 0.0);
    }

    @Test
    public void testDefaultIfNull_Double_withDefaultArg() {
        assertEquals(5.0, CommonUtil.defaultIfNull(null, 5.0), 0.0);
        assertEquals(10.0, CommonUtil.defaultIfNull(Double.valueOf(10.0), 5.0), 0.0);
    }

    @Test
    public void testDefaultIfNull_Generic_withDefaultArg() {
        String defaultStr = "default";
        assertEquals(defaultStr, CommonUtil.defaultIfNull(null, defaultStr));
        String actualStr = "actual";
        assertEquals(actualStr, CommonUtil.defaultIfNull(actualStr, defaultStr));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfNull("any", (String) null));
    }

    @Test
    public void testDefaultIfNull_Generic_withSupplier() {
        String defaultStr = "default";
        Supplier<String> supplier = () -> defaultStr;
        assertEquals(defaultStr, CommonUtil.defaultIfNull(null, supplier));

        String actualStr = "actual";
        assertEquals(actualStr, CommonUtil.defaultIfNull(actualStr, supplier));

        Supplier<String> nullSupplier = () -> null;
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfNull(null, nullSupplier));
    }

    @Test
    public void testDefaultIfEmpty_CharSequence_withDefaultArg() {
        String defaultStr = "default";
        assertEquals(defaultStr, CommonUtil.defaultIfEmpty(null, defaultStr));
        assertEquals(defaultStr, CommonUtil.defaultIfEmpty("", defaultStr));
        assertEquals("actual", CommonUtil.defaultIfEmpty("actual", defaultStr));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty("any", ""));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty("any", (String) null));

        StringBuilder sbNull = null;
        StringBuilder sbEmpty = new StringBuilder();
        StringBuilder sbVal = new StringBuilder("val");
        StringBuilder defaultSb = new StringBuilder("defaultSb");

        assertSame(defaultSb, CommonUtil.defaultIfEmpty(sbNull, defaultSb));
        assertSame(defaultSb, CommonUtil.defaultIfEmpty(sbEmpty, defaultSb));
        assertSame(sbVal, CommonUtil.defaultIfEmpty(sbVal, defaultSb));
    }

    @Test
    public void testDefaultIfEmpty_CharSequence_withSupplier() {
        String defaultStr = "default";
        Supplier<String> supplier = () -> defaultStr;
        assertEquals(defaultStr, CommonUtil.defaultIfEmpty(null, supplier));
        assertEquals(defaultStr, CommonUtil.defaultIfEmpty("", supplier));
        assertEquals("actual", CommonUtil.defaultIfEmpty("actual", supplier));

        Supplier<String> emptySupplier = () -> "";
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty("", emptySupplier));
        Supplier<String> nullSupplier = () -> null;
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty("", nullSupplier));
    }

    @Test
    public void testDefaultIfBlank_CharSequence_withDefaultArg() {
        String defaultStr = "default";
        assertEquals(defaultStr, CommonUtil.defaultIfBlank(null, defaultStr));
        assertEquals(defaultStr, CommonUtil.defaultIfBlank("", defaultStr));
        assertEquals(defaultStr, CommonUtil.defaultIfBlank("   ", defaultStr));
        assertEquals("actual", CommonUtil.defaultIfBlank("actual", defaultStr));
        assertEquals("  actual  ", CommonUtil.defaultIfBlank("  actual  ", defaultStr));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfBlank("any", ""));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfBlank("any", "   "));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfBlank("any", (String) null));
    }

    @Test
    public void testDefaultIfBlank_CharSequence_withSupplier() {
        String defaultStr = "default";
        Supplier<String> supplier = () -> defaultStr;
        assertEquals(defaultStr, CommonUtil.defaultIfBlank(null, supplier));
        assertEquals(defaultStr, CommonUtil.defaultIfBlank("", supplier));
        assertEquals(defaultStr, CommonUtil.defaultIfBlank("   ", supplier));
        assertEquals("actual", CommonUtil.defaultIfBlank("actual", supplier));

        Supplier<String> blankSupplier = () -> "   ";
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfBlank("   ", blankSupplier));
        Supplier<String> nullSupplier = () -> null;
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfBlank("   ", nullSupplier));
    }

    @Test
    public void testLen_booleanArray() {
        assertEquals(0, CommonUtil.len((boolean[]) null));
        assertEquals(0, CommonUtil.len(new boolean[0]));
        assertEquals(2, CommonUtil.len(new boolean[] { true, false }));
    }

    @Test
    public void testLen_charArray() {
        assertEquals(0, CommonUtil.len((char[]) null));
        assertEquals(0, CommonUtil.len(new char[0]));
        assertEquals(3, CommonUtil.len(new char[] { 'a', 'b', 'c' }));
    }

    @Test
    public void testLen_byteArray() {
        assertEquals(0, CommonUtil.len((byte[]) null));
        assertEquals(0, CommonUtil.len(new byte[0]));
        assertEquals(2, CommonUtil.len(new byte[] { 1, 2 }));
    }

    @Test
    public void testLen_shortArray() {
        assertEquals(0, CommonUtil.len((short[]) null));
        assertEquals(0, CommonUtil.len(new short[0]));
        assertEquals(2, CommonUtil.len(new short[] { 10, 20 }));
    }

    @Test
    public void testLen_intArray() {
        assertEquals(0, CommonUtil.len((int[]) null));
        assertEquals(0, CommonUtil.len(new int[0]));
        assertEquals(2, CommonUtil.len(new int[] { 100, 200 }));
    }

    @Test
    public void testLen_longArray() {
        assertEquals(0, CommonUtil.len((long[]) null));
        assertEquals(0, CommonUtil.len(new long[0]));
        assertEquals(2, CommonUtil.len(new long[] { 1000L, 2000L }));
    }

    @Test
    public void testLen_floatArray() {
        assertEquals(0, CommonUtil.len((float[]) null));
        assertEquals(0, CommonUtil.len(new float[0]));
        assertEquals(2, CommonUtil.len(new float[] { 1.0f, 2.0f }));
    }

    @Test
    public void testLen_doubleArray() {
        assertEquals(0, CommonUtil.len((double[]) null));
        assertEquals(0, CommonUtil.len(new double[0]));
        assertEquals(2, CommonUtil.len(new double[] { 1.0, 2.0 }));
    }

    @Test
    public void testLen_ObjectArray() {
        assertEquals(0, CommonUtil.len((Object[]) null));
        assertEquals(0, CommonUtil.len(new Object[0]));
        assertEquals(2, CommonUtil.len(new Object[] { "a", "b" }));
    }

    @Test
    public void testSize_PrimitiveList() {
        assertEquals(0, CommonUtil.size((PrimitiveList) null));
    }

    @Test
    public void testIsEmpty_floatArray() {
        assertTrue(CommonUtil.isEmpty((float[]) null));
        assertTrue(CommonUtil.isEmpty(new float[0]));
        assertFalse(CommonUtil.isEmpty(new float[] { 1f }));
    }

    @Test
    public void testIsEmpty_Iterable() {
        assertTrue(CommonUtil.isEmpty((Iterable<?>) null));
        assertTrue(CommonUtil.isEmpty(Collections.emptyList()));
        assertFalse(CommonUtil.isEmpty(Arrays.asList(1)));

        Iterable<Integer> emptyIterable = () -> Collections.<Integer> emptyIterator();
        assertTrue(CommonUtil.isEmpty(emptyIterable));

        Iterable<Integer> nonEmptyIterable = () -> Arrays.asList(1, 2).iterator();
        assertFalse(CommonUtil.isEmpty(nonEmptyIterable));
    }

    @Test
    public void testIsEmpty_Iterator() {
        assertTrue(CommonUtil.isEmpty((Iterator<?>) null));
        assertTrue(CommonUtil.isEmpty(Collections.emptyIterator()));
        assertFalse(CommonUtil.isEmpty(Arrays.asList(1, 2).iterator()));
    }

    @Test
    public void testIsEmpty_PrimitiveList() {
        assertTrue(CommonUtil.isEmpty((PrimitiveList) null));
    }

    @Test
    public void testIsEmpty_Multiset() {
        assertTrue(CommonUtil.isEmpty((Multiset<?>) null));
    }

    @Test
    public void testIsEmpty_Multimap() {
        assertTrue(CommonUtil.isEmpty((Multimap<?, ?, ?>) null));
    }

    @Test
    public void testIsEmpty_Dataset() {
        assertTrue(CommonUtil.isEmpty((Dataset) null));
    }

    @Test
    public void testNotEmpty_Iterable() {
        assertFalse(CommonUtil.notEmpty((Iterable<?>) null));
        assertFalse(CommonUtil.notEmpty(Collections.emptyList()));
        assertTrue(CommonUtil.notEmpty(Arrays.asList(1)));

        Iterable<Integer> emptyIterable = () -> Collections.<Integer> emptyIterator();
        assertFalse(CommonUtil.notEmpty(emptyIterable));

        Iterable<Integer> nonEmptyIterable = () -> Arrays.asList(1, 2).iterator();
        assertTrue(CommonUtil.notEmpty(nonEmptyIterable));
    }

    @Test
    public void testNotEmpty_Iterator() {
        assertFalse(CommonUtil.notEmpty((Iterator<?>) null));
        assertFalse(CommonUtil.notEmpty(Collections.emptyIterator()));
        assertTrue(CommonUtil.notEmpty(Arrays.asList(1).iterator()));
    }

    @Test
    public void testAnyNull_ThreeObjects() {
        assertTrue(CommonUtil.anyNull(null, "a", "b"));
        assertTrue(CommonUtil.anyNull("a", null, "b"));
        assertTrue(CommonUtil.anyNull("a", "b", null));
        assertTrue(CommonUtil.anyNull(null, null, "b"));
        assertFalse(CommonUtil.anyNull("a", "b", "c"));
    }

    @Test
    public void testAnyNull_VarArgs_explicitNullArray() {
        assertFalse(CommonUtil.anyNull((Object[]) null));
    }

    @Test
    public void testAnyNull_Iterable() {
        assertFalse(CommonUtil.anyNull((Iterable<?>) null));
        assertFalse(CommonUtil.anyNull(Collections.emptyList()));
        assertFalse(CommonUtil.anyNull(Arrays.asList("a", "b")));
        assertTrue(CommonUtil.anyNull(Arrays.asList("a", null, "b")));
        List<String> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        assertTrue(CommonUtil.anyNull(listWithNull));

    }

    @Test
    public void testAnyEmpty_CharSequenceVarArgs() {
        assertTrue(CommonUtil.anyEmpty((String) null));
        assertFalse(CommonUtil.anyEmpty((String[]) null));
        assertTrue(CommonUtil.anyEmpty(null, "foo"));
        assertTrue(CommonUtil.anyEmpty("", "bar"));
        assertFalse(CommonUtil.anyEmpty("foo", "bar"));
        assertFalse(CommonUtil.anyEmpty(new String[] {}));
        assertTrue(CommonUtil.anyEmpty(new String[] { "" }));
    }

    @Test
    public void testAnyEmpty_CharSequenceIterable() {
        assertTrue(CommonUtil.anyEmpty(Arrays.asList(null, "a")));
        assertTrue(CommonUtil.anyEmpty(Arrays.asList("", "a")));
        assertFalse(CommonUtil.anyEmpty(Arrays.asList("a", "b")));
        assertFalse(CommonUtil.anyEmpty(Collections.<CharSequence> emptyList()));
    }

    @Test
    public void testAnyEmpty_TwoObjectArrays() {
        assertTrue(CommonUtil.anyEmpty(null, new Object[] { "a" }));
        assertTrue(CommonUtil.anyEmpty(new Object[0], new Object[] { "a" }));
        assertTrue(CommonUtil.anyEmpty(new Object[] { "a" }, null));
        assertTrue(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[0]));
        assertFalse(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[] { "b" }));
    }

    @Test
    public void testAnyEmpty_ThreeObjectArrays() {
        assertTrue(CommonUtil.anyEmpty(null, new Object[] { "a" }, new Object[] { "b" }));
        assertTrue(CommonUtil.anyEmpty(new Object[0], new Object[] { "a" }, new Object[] { "b" }));
        assertTrue(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[0], new Object[] { "b" }));
        assertFalse(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[] { "b" }, new Object[] { "c" }));
    }

    @Test
    public void testAnyEmpty_TwoCollections() {
        assertTrue(CommonUtil.anyEmpty(null, Arrays.asList("a")));
        assertTrue(CommonUtil.anyEmpty(Collections.emptyList(), Arrays.asList("a")));
        assertFalse(CommonUtil.anyEmpty(Arrays.asList("a"), Arrays.asList("b")));
    }

    @Test
    public void testAnyEmpty_ThreeCollections() {
        assertTrue(CommonUtil.anyEmpty(null, Arrays.asList("a"), Arrays.asList("b")));
        assertTrue(CommonUtil.anyEmpty(Collections.emptyList(), Arrays.asList("a"), Arrays.asList("b")));
        assertTrue(CommonUtil.anyEmpty(Arrays.asList("a"), Collections.emptyList(), Arrays.asList("b")));
        assertFalse(CommonUtil.anyEmpty(Arrays.asList("a"), Arrays.asList("b"), Arrays.asList("c")));
    }

    @Test
    public void testAnyEmpty_TwoMaps() {
        Map<String, String> nonEmptyMap = new HashMap<>();
        nonEmptyMap.put("k", "v");
        assertTrue(CommonUtil.anyEmpty(null, nonEmptyMap));
        assertTrue(CommonUtil.anyEmpty(Collections.emptyMap(), nonEmptyMap));
        assertFalse(CommonUtil.anyEmpty(nonEmptyMap, nonEmptyMap));
    }

    @Test
    public void testAnyEmpty_ThreeMaps() {
        Map<String, String> nonEmptyMap = new HashMap<>();
        nonEmptyMap.put("k", "v");
        assertTrue(CommonUtil.anyEmpty(null, nonEmptyMap, nonEmptyMap));
        assertTrue(CommonUtil.anyEmpty(Collections.emptyMap(), nonEmptyMap, nonEmptyMap));
        assertTrue(CommonUtil.anyEmpty(nonEmptyMap, Collections.emptyMap(), nonEmptyMap));
        assertFalse(CommonUtil.anyEmpty(nonEmptyMap, nonEmptyMap, nonEmptyMap));
    }

    @Test
    public void testAnyBlank_TwoCharSequences() {
        assertTrue(CommonUtil.anyBlank(null, "a"));
        assertTrue(CommonUtil.anyBlank(" ", "a"));
        assertTrue(CommonUtil.anyBlank("a", null));
        assertTrue(CommonUtil.anyBlank("a", " "));
        assertFalse(CommonUtil.anyBlank("a", "b"));
    }

    @Test
    public void testAnyBlank_ThreeCharSequences() {
        assertTrue(CommonUtil.anyBlank(null, "a", "b"));
        assertTrue(CommonUtil.anyBlank(" ", "a", "b"));
        assertTrue(CommonUtil.anyBlank("a", " ", "b"));
        assertFalse(CommonUtil.anyBlank("a", "b", "c"));
    }

    @Test
    public void testAnyBlank_CharSequenceVarArgs() {
        assertFalse(CommonUtil.anyBlank((CharSequence[]) null));
        assertFalse(CommonUtil.anyBlank());
        assertTrue(CommonUtil.anyBlank((CharSequence) null));
        assertTrue(CommonUtil.anyBlank(" "));
        assertTrue(CommonUtil.anyBlank("a", " "));
        assertFalse(CommonUtil.anyBlank("a", "b"));
    }

    @Test
    public void testAnyBlank_CharSequenceIterable() {
        assertFalse(CommonUtil.anyBlank((Iterable<CharSequence>) null));
        assertFalse(CommonUtil.anyBlank(Collections.emptyList()));
        assertTrue(CommonUtil.anyBlank(Arrays.asList(null, "a")));
        assertTrue(CommonUtil.anyBlank(Arrays.asList(" ", "a")));
        assertFalse(CommonUtil.anyBlank(Arrays.asList("a", "b")));
    }

    @Test
    public void testAllNull_TwoObjects() {
        assertFalse(CommonUtil.allNull(null, "a"));
        assertFalse(CommonUtil.allNull("a", null));
        assertTrue(CommonUtil.allNull(null, null));
        assertFalse(CommonUtil.allNull("a", "b"));
    }

    @Test
    public void testAllNull_ThreeObjects() {
        assertFalse(CommonUtil.allNull(null, "a", "b"));
        assertTrue(CommonUtil.allNull(null, null, null));
        assertFalse(CommonUtil.allNull("a", "b", "c"));
    }

    @Test
    public void testAllNull_Iterable() {
        assertTrue(CommonUtil.allNull((Iterable<?>) null));
        assertTrue(CommonUtil.allNull(Collections.emptyList()));
        assertFalse(CommonUtil.allNull(Arrays.asList("a", "b")));
        assertFalse(CommonUtil.allNull(Arrays.asList("a", null, "b")));
        assertTrue(CommonUtil.allNull(Arrays.asList(null, null)));
        List<String> listWithNulls = new ArrayList<>();
        listWithNulls.add(null);
        listWithNulls.add(null);
        assertTrue(CommonUtil.allNull(listWithNulls));
    }

    @Test
    public void testAllEmpty_CharSequenceVarArgs() {
        assertTrue(CommonUtil.allEmpty((CharSequence[]) null));
        assertTrue(CommonUtil.allEmpty());
        assertTrue(CommonUtil.allEmpty((CharSequence) null));
        assertTrue(CommonUtil.allEmpty(""));
        assertTrue(CommonUtil.allEmpty(null, ""));
        assertFalse(CommonUtil.allEmpty(null, "a"));
    }

    @Test
    public void testAllEmpty_CharSequenceIterable() {
        assertTrue(CommonUtil.allEmpty((Iterable<CharSequence>) null));
        assertTrue(CommonUtil.allEmpty(Collections.emptyList()));
        assertTrue(CommonUtil.allEmpty(Arrays.asList(null, "")));
        assertFalse(CommonUtil.allEmpty(Arrays.asList(null, "a")));
    }

    @Test
    public void testAllEmpty_TwoObjectArrays() {
        assertTrue(CommonUtil.allEmpty((Object[]) null, (Object[]) null));
        assertTrue(CommonUtil.allEmpty(new Object[0], null));
        assertTrue(CommonUtil.allEmpty(null, new Object[0]));
        assertTrue(CommonUtil.allEmpty(new Object[0], new Object[0]));
        assertFalse(CommonUtil.allEmpty(new Object[] { "a" }, new Object[0]));
    }

    @Test
    public void testAllEmpty_ThreeObjectArrays() {
        assertTrue(CommonUtil.allEmpty((Object[]) null, (Object[]) null, (Object[]) null));
        assertTrue(CommonUtil.allEmpty(new Object[0], null, new Object[0]));
        assertFalse(CommonUtil.allEmpty(new Object[] { "a" }, new Object[0], null));
    }

    @Test
    public void testAllEmpty_TwoCollections() {
        assertTrue(CommonUtil.allEmpty((List) null, (List) null));
        assertTrue(CommonUtil.allEmpty(Collections.emptyList(), null));
        assertTrue(CommonUtil.allEmpty(null, Collections.emptyList()));
        assertTrue(CommonUtil.allEmpty(Collections.emptyList(), Collections.emptyList()));
        assertFalse(CommonUtil.allEmpty(Arrays.asList("a"), Collections.emptyList()));
    }

    @Test
    public void testAllEmpty_ThreeCollections() {
        assertTrue(CommonUtil.allEmpty((List) null, (List) null, (List) null));
        assertTrue(CommonUtil.allEmpty(Collections.emptyList(), null, Collections.emptyList()));
        assertFalse(CommonUtil.allEmpty(Arrays.asList("a"), Collections.emptyList(), null));
    }

    @Test
    public void testAllEmpty_TwoMaps() {
        assertTrue(CommonUtil.allEmpty((Map) null, (Map) null));
        assertTrue(CommonUtil.allEmpty(Collections.emptyMap(), null));
        assertTrue(CommonUtil.allEmpty(null, Collections.emptyMap()));
        assertTrue(CommonUtil.allEmpty(Collections.emptyMap(), Collections.emptyMap()));
        Map<String, String> nonEmptyMap = new HashMap<>();
        nonEmptyMap.put("k", "v");
        assertFalse(CommonUtil.allEmpty(nonEmptyMap, Collections.emptyMap()));
    }

    @Test
    public void testAllEmpty_ThreeMaps() {
        assertTrue(CommonUtil.allEmpty((Map) null, (Map) null, (Map) null));
        assertTrue(CommonUtil.allEmpty(Collections.emptyMap(), null, Collections.emptyMap()));
        Map<String, String> nonEmptyMap = new HashMap<>();
        nonEmptyMap.put("k", "v");
        assertFalse(CommonUtil.allEmpty(nonEmptyMap, Collections.emptyMap(), null));
    }

    @Test
    public void testAllBlank_TwoCharSequences() {
        assertFalse(CommonUtil.allBlank(null, "a"));
        assertTrue(CommonUtil.allBlank(null, " "));
        assertTrue(CommonUtil.allBlank(null, null));
        assertTrue(CommonUtil.allBlank(" ", " "));
        assertFalse(CommonUtil.allBlank("a", " "));
    }

    @Test
    public void testAllBlank_ThreeCharSequences() {
        assertFalse(CommonUtil.allBlank(null, " ", "a"));
        assertTrue(CommonUtil.allBlank(null, " ", null));
        assertFalse(CommonUtil.allBlank("a", "b", "c"));
    }

    @Test
    public void testAllBlank_CharSequenceVarArgs() {
        assertTrue(CommonUtil.allBlank((CharSequence[]) null));
        assertTrue(CommonUtil.allBlank());
        assertTrue(CommonUtil.allBlank((CharSequence) null));
        assertTrue(CommonUtil.allBlank(" "));
        assertTrue(CommonUtil.allBlank(null, " ", "\t"));
        assertFalse(CommonUtil.allBlank(null, "a"));
    }

    @Test
    public void testAllBlank_CharSequenceIterable() {
        assertTrue(CommonUtil.allBlank((Iterable<CharSequence>) null));
        assertTrue(CommonUtil.allBlank(Collections.emptyList()));
        assertTrue(CommonUtil.allBlank(Arrays.asList(null, " ", "\t")));
        assertFalse(CommonUtil.allBlank(Arrays.asList(null, "a")));
    }

    @Test
    public void testEquals_boolean() {
        assertTrue(CommonUtil.equals(true, true));
        assertTrue(CommonUtil.equals(false, false));
        assertFalse(CommonUtil.equals(true, false));
    }

    @Test
    public void testEquals_short() {
        assertTrue(CommonUtil.equals((short) 10, (short) 10));
        assertFalse(CommonUtil.equals((short) 10, (short) 20));
    }

    @Test
    public void testEquals_int() {
        assertTrue(CommonUtil.equals(100, 100));
        assertFalse(CommonUtil.equals(100, 200));
    }

    @Test
    public void testEquals_long() {
        assertTrue(CommonUtil.equals(1000L, 1000L));
        assertFalse(CommonUtil.equals(1000L, 2000L));
    }

    @Test
    public void testEquals_float() {
        assertTrue(CommonUtil.equals(1.0f, 1.0f));
        assertFalse(CommonUtil.equals(1.0f, 1.1f));
        assertTrue(CommonUtil.equals(Float.NaN, Float.NaN));
    }

    @Test
    public void testEquals_double() {
        assertTrue(CommonUtil.equals(1.0, 1.0));
        assertFalse(CommonUtil.equals(1.0, 1.1));
        assertTrue(CommonUtil.equals(Double.NaN, Double.NaN));
    }

    @Test
    public void testEquals_booleanArray() {
        assertTrue(CommonUtil.equals(new boolean[] { true, false }, new boolean[] { true, false }));
        assertFalse(CommonUtil.equals(new boolean[] { true }, new boolean[] { false }));
        assertTrue(CommonUtil.equals((boolean[]) null, (boolean[]) null));
        assertFalse(CommonUtil.equals(new boolean[0], (boolean[]) null));
        assertFalse(CommonUtil.equals(new boolean[] { true }, new boolean[] { true, false }));
    }

    @Test
    public void testEquals_booleanArrayRange() {
        boolean[] a1 = { true, false, true, true };
        boolean[] a2 = { false, true, true, false };
        boolean[] a3 = { true, false, true, true };

        assertTrue(CommonUtil.equals(a1, 1, a3, 1, 2));
        assertFalse(CommonUtil.equals(a1, 0, a2, 0, 2));
        assertTrue(CommonUtil.equals(a1, 0, a1, 0, 0));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(a1, 0, a3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equals(a1, 0, a3, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equals(a1, 3, a3, 0, 2));
    }

    @Test
    public void testEquals_intArray() {
        assertTrue(CommonUtil.equals(new int[] { 1, 2 }, new int[] { 1, 2 }));
        assertFalse(CommonUtil.equals(new int[] { 1 }, new int[] { 2 }));
    }

    @Test
    public void testEquals_intArrayRange() {
        int[] a1 = { 1, 2, 3, 4 };
        int[] a2 = { 0, 2, 3, 5 };
        int[] a3 = { 1, 2, 3, 4 };

        assertTrue(CommonUtil.equals(a1, 1, a3, 1, 2));
        assertFalse(CommonUtil.equals(a1, 0, a2, 0, 2));
        assertTrue(CommonUtil.equals(a1, 0, a1, 0, 0));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(a1, 0, a3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equals(a1, 0, a3, 0, 5));
    }

    @Test
    public void testEquals_floatArrayRange() {
        float[] a1 = { 1.0f, 2.0f, Float.NaN, 4.0f };
        float[] a2 = { 0.0f, 2.0f, Float.NaN, 5.0f };
        float[] a3 = { 1.0f, 2.0f, Float.NaN, 4.0f };

        assertTrue(CommonUtil.equals(a1, 1, a3, 1, 2));
        assertFalse(CommonUtil.equals(a1, 0, a2, 0, 2));
    }

    @Test
    public void testEquals_doubleArrayRange() {
        double[] a1 = { 1.0, 2.0, Double.NaN, 4.0 };
        double[] a2 = { 0.0, 2.0, Double.NaN, 5.0 };
        double[] a3 = { 1.0, 2.0, Double.NaN, 4.0 };

        assertTrue(CommonUtil.equals(a1, 1, a3, 1, 2));
        assertFalse(CommonUtil.equals(a1, 0, a2, 0, 2));
    }

    @Test
    public void testEquals_ObjectArrayRange() {
        Object[] a1 = { "hello", "world", "!" };
        Object[] a2 = { "start", "world", "!" };
        Object[] a3 = { "hello", "world", "!" };

        assertTrue(CommonUtil.equals(a1, 1, a3, 1, 2));
        assertFalse(CommonUtil.equals(a1, 0, a2, 0, 2));
        assertTrue(CommonUtil.equals(a1, 0, a1, 0, 0));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(a1, 0, a3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equals(a1, 0, a3, 0, 4));
    }

    @Test
    public void testDeepEquals_ObjectArray() {
        Object[] a = { 1, new String[] { "a", "b" }, new int[] { 10, 20 } };
        Object[] b = { 1, new String[] { "a", "b" }, new int[] { 10, 20 } };
        Object[] c = { 1, new String[] { "a", "DIFFERENT" }, new int[] { 10, 20 } };
        assertTrue(CommonUtil.deepEquals(a, b));
        assertFalse(CommonUtil.deepEquals(a, c));
        assertTrue(CommonUtil.deepEquals((Object[]) null, (Object[]) null));
    }

    @Test
    public void testDeepEquals_ObjectArrayRange() {
        Object[] arr1 = { "first", new int[] { 1, 2 }, "mid", new String[] { "x", "y" }, "last" };
        Object[] arr2 = { "other", new int[] { 1, 2 }, "mid", new String[] { "x", "y" }, "end" };
        Object[] arr3 = { "first", new int[] { 1, 2 }, "mid", new String[] { "x", "y" }, "last" };

        assertTrue(CommonUtil.deepEquals(arr1, 1, arr3, 1, 3));
        assertFalse(CommonUtil.deepEquals(arr1, 0, arr2, 0, 2));
        assertTrue(CommonUtil.deepEquals(arr1, 0, arr1, 0, 0));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.deepEquals(arr1, 0, arr3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.deepEquals(arr1, 0, arr3, 0, 6));

        Object[] cyclic1 = new Object[1];
        cyclic1[0] = cyclic1;
        Object[] cyclic2 = new Object[1];
        cyclic2[0] = cyclic2;
        Object[] cyclic3 = new Object[1];
        cyclic3[0] = new Object[1];

    }

    @Test
    public void testEqualsIgnoreCase_StringArrayRange() {
        String[] a1 = { "Hello", "WORLD", "Java", "Test" };
        String[] a2 = { "start", "world", "java", "end" };
        String[] a3 = { "HELLO", "world", "JAVA", "TEST" };

        assertTrue(CommonUtil.equalsIgnoreCase(a1, 1, a3, 1, 2));
        assertFalse(CommonUtil.equalsIgnoreCase(a1, 0, a2, 0, 2));
        assertTrue(CommonUtil.equalsIgnoreCase(a1, 0, a1, 0, 0));

        String[] an1 = { "Test", null, "Me" };
        String[] an2 = { "test", null, "me" };
        String[] an3 = { "test", "NotNULL", "me" };
        assertTrue(CommonUtil.equalsIgnoreCase(an1, 0, an2, 0, 3));
        assertFalse(CommonUtil.equalsIgnoreCase(an1, 0, an3, 0, 3));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equalsIgnoreCase(a1, 0, a3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equalsIgnoreCase(a1, 0, a3, 0, 5));
    }

    @Test
    public void testEqualsByKeys() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 1);
        map2.put("b", 2);
        map2.put("d", 4);

        Map<String, Integer> map3 = new HashMap<>();
        map3.put("a", 1);
        map3.put("b", 2);
        map3.put("c", 3);

        Map<String, Integer> map4 = new HashMap<>();
        map4.put("a", 1);
        map4.put("b", 20);
        map4.put("c", 3);

        Collection<String> keys = Arrays.asList("a", "b");
        Collection<String> keysWithC = Arrays.asList("a", "b", "c");

        assertTrue(CommonUtil.equalsByKeys(map1, map3, keysWithC));
        assertTrue(CommonUtil.equalsByKeys(map1, map3, keys));
        assertFalse(CommonUtil.equalsByKeys(map1, map2, keysWithC));
        assertTrue(CommonUtil.equalsByKeys(map1, map2, keys));
        assertFalse(CommonUtil.equalsByKeys(map1, map4, keysWithC));

        assertTrue(CommonUtil.equalsByKeys(null, null, keys));
        assertFalse(CommonUtil.equalsByKeys(map1, null, keys));
        assertFalse(CommonUtil.equalsByKeys(null, map1, keys));

        Map<String, Integer> mapWithNullValue1 = new HashMap<>();
        mapWithNullValue1.put("k1", null);
        Map<String, Integer> mapWithNullValue2 = new HashMap<>();
        mapWithNullValue2.put("k1", null);
        Map<String, Integer> mapWithNonNullValue = new HashMap<>();
        mapWithNonNullValue.put("k1", 1);

        assertTrue(CommonUtil.equalsByKeys(mapWithNullValue1, mapWithNullValue2, Collections.singletonList("k1")));
        assertFalse(CommonUtil.equalsByKeys(mapWithNullValue1, mapWithNonNullValue, Collections.singletonList("k1")));

        Map<String, Integer> emptyMap = Collections.emptyMap();
        assertFalse(CommonUtil.equalsByKeys(mapWithNullValue1, emptyMap, Collections.singletonList("k1")));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equalsByKeys(map1, map2, Collections.emptyList()));
    }

    @Test
    public void testHashCode_char() {
        assertEquals('a', CommonUtil.hashCode('a'));
    }

    @Test
    public void testHashCode_byte() {
        assertEquals((byte) 5, CommonUtil.hashCode((byte) 5));
    }

    @Test
    public void testHashCode_short() {
        assertEquals((short) 100, CommonUtil.hashCode((short) 100));
    }

    @Test
    public void testHashCode_int() {
        assertEquals(12345, CommonUtil.hashCode(12345));
    }

    @Test
    public void testHashCode_long() {
        assertEquals(Long.hashCode(123456789L), CommonUtil.hashCode(123456789L));
    }

    @Test
    public void testHashCode_float() {
        assertEquals(Float.floatToIntBits(1.23f), CommonUtil.hashCode(1.23f));
        assertEquals(Float.floatToIntBits(Float.NaN), CommonUtil.hashCode(Float.NaN));
    }

    @Test
    public void testHashCode_double() {
        assertEquals(Double.hashCode(1.2345), CommonUtil.hashCode(1.2345));
        assertEquals(Double.hashCode(Double.NaN), CommonUtil.hashCode(Double.NaN));
    }

    @Test
    public void testHashCode_booleanArray() {
        assertEquals(0, CommonUtil.hashCode((boolean[]) null));
        boolean[] ba1 = { true, false };
        boolean[] ba2 = { true, false };
        boolean[] ba3 = { false, true };
        assertEquals(CommonUtil.hashCode(ba1), CommonUtil.hashCode(ba2));
        assertNotEquals(CommonUtil.hashCode(ba1), CommonUtil.hashCode(ba3));
        assertEquals(Arrays.hashCode(new boolean[0]), CommonUtil.hashCode(new boolean[0]));
    }

    @Test
    public void testHashCode_booleanArrayRange() {
        boolean[] arr = { true, false, true, false, true };
        assertEquals(0, CommonUtil.hashCode((boolean[]) null, 0, 0));

        int fullHash = CommonUtil.hashCode(arr, 0, arr.length);
        int partialHash = CommonUtil.hashCode(arr, 1, 3);

        assertNotEquals(0, fullHash);
        assertNotEquals(0, partialHash);
        assertNotEquals(fullHash, partialHash);

        assertEquals(1, CommonUtil.hashCode(arr, 1, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(arr, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(arr, 3, 2));
    }

    @Test
    public void testHashCode_intArray() {
        assertEquals(0, CommonUtil.hashCode((int[]) null));
        int[] arr = { 1, 2 };
        int expected = 1;
        expected = 31 * expected + 1;
        expected = 31 * expected + 2;
        assertEquals(expected, CommonUtil.hashCode(arr));
    }

    @Test
    public void testHashCode_intArrayRange() {
        int[] arr = { 10, 20, 30, 40, 50 };
        int expected = 1;
        expected = 31 * expected + 20;
        expected = 31 * expected + 30;
        assertEquals(expected, CommonUtil.hashCode(arr, 1, 3));
        assertEquals(1, CommonUtil.hashCode(arr, 1, 1));
    }

    @Test
    public void testHashCode_ObjectArrayRange() {
        Object[] arr = { "A", "B", null, "D" };
        int expected = 1;
        expected = 31 * expected + "B".hashCode();
        expected = 31 * expected + 0;
        assertEquals(expected, CommonUtil.hashCode(arr, 1, 3));
        assertEquals(1, CommonUtil.hashCode(arr, 1, 1));
    }

    @Test
    public void testDeepHashCode_ObjectArrayRange() {
        Object[] arr = { "X", new int[] { 100, 200 }, new String[] { "deep" } };
        int expected = 1;
        expected = 31 * expected + CommonUtil.deepHashCode(new int[] { 100, 200 });
        expected = 31 * expected + CommonUtil.deepHashCode(new String[] { "deep" });
        assertEquals(expected, CommonUtil.deepHashCode(arr, 1, 3));
        assertEquals(1, CommonUtil.deepHashCode(arr, 1, 1));

    }

    @Test
    public void testToString_boolean() {
        assertEquals(String.valueOf(true), CommonUtil.toString(true));
    }

    @Test
    public void testToString_char() {
        assertEquals(String.valueOf('z'), CommonUtil.toString('z'));
    }

    @Test
    public void testToString_byte() {
        assertEquals(String.valueOf((byte) 12), CommonUtil.toString((byte) 12));
    }

    @Test
    public void testToString_short() {
        assertEquals(String.valueOf((short) 123), CommonUtil.toString((short) 123));
    }

    @Test
    public void testToString_int() {
        assertEquals(String.valueOf(12345), CommonUtil.toString(12345));
    }

    @Test
    public void testToString_long() {
        assertEquals(String.valueOf(1234567L), CommonUtil.toString(1234567L));
    }

    @Test
    public void testToString_float() {
        assertEquals(String.valueOf(1.2f), CommonUtil.toString(1.2f));
    }

    @Test
    public void testToString_double() {
        assertEquals(String.valueOf(1.23), CommonUtil.toString(1.23));
    }

    @Test
    public void testToString_Object_defaultIfNull() {
        assertEquals("default", CommonUtil.toString(null, "default"));
        assertEquals("test", CommonUtil.toString("test", "default"));
    }

    @Test
    public void testToString_booleanArray() {
        assertEquals("null", CommonUtil.toString((boolean[]) null));
        assertEquals("[]", CommonUtil.toString(new boolean[0]));
        assertEquals("[true, false, true]", CommonUtil.toString(new boolean[] { true, false, true }));
    }

    @Test
    public void testToString_booleanArrayRange() {
        boolean[] arr = { true, false, true, true, false };
        assertEquals("[false, true, true]", CommonUtil.toString(arr, 1, 4));
        assertEquals("[]", CommonUtil.toString(arr, 1, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toString(arr, 0, 6));
    }

    @Test
    public void testToString_charArray() {
        assertEquals("null", CommonUtil.toString((char[]) null));
        assertEquals("[]", CommonUtil.toString(new char[0]));
        assertEquals("[a, b, c]", CommonUtil.toString(new char[] { 'a', 'b', 'c' }));
    }

    @Test
    public void testToString_charArrayRange() {
        char[] arr = { 'h', 'e', 'l', 'l', 'o' };
        assertEquals("[e, l, l]", CommonUtil.toString(arr, 1, 4));
        assertEquals("[]", CommonUtil.toString(arr, 1, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toString(arr, 0, 6));
    }

    @Test
    public void testToString_intArray() {
        assertEquals("null", CommonUtil.toString((int[]) null));
        assertEquals("[]", CommonUtil.toString(new int[0]));
        assertEquals("[1, 2, 3]", CommonUtil.toString(new int[] { 1, 2, 3 }));
    }

    @Test
    public void testToString_intArrayRange() {
        int[] arr = { 10, 20, 30, 40 };
        assertEquals("[20, 30]", CommonUtil.toString(arr, 1, 3));
        assertEquals("[]", CommonUtil.toString(arr, 1, 1));
    }

    @Test
    public void testToString_ObjectArrayRange() {
        Object[] arr = { "one", 2, "three", null, 5.0 };
        assertEquals("[2, three, null]", CommonUtil.toString(arr, 1, 4));
        assertEquals("[]", CommonUtil.toString(arr, 1, 1));
    }

    @Test
    public void testDeepToString_ObjectArrayRange() {
        Object[] arr = { "start", new Object[] { "nested1", new int[] { 10, 20 } }, "middle", new String[] { "s1", "s2" }, "end" };
        String expected = "[[nested1, [10, 20]], middle, [s1, s2]]";
        assertEquals(expected, CommonUtil.deepToString(arr, 1, 4));
        assertEquals("[]", CommonUtil.deepToString(arr, 1, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.deepToString(arr, 0, 6));
    }

    @Test
    public void testDeepToString_ObjectArray_Cyclic() {
        Object[] cyclicArray = new Object[2];
        cyclicArray[0] = "Element 1";
        cyclicArray[1] = cyclicArray;
        assertEquals("[Element 1, [...]]", CommonUtil.deepToString(cyclicArray));

        Object[] arr = new Object[1];
        arr[0] = arr;
        assertEquals("[[...]]", CommonUtil.deepToString(arr));

        Object[] a = new Object[2];
        Object[] b = new Object[] { "b" };
        a[0] = b;
        a[1] = b;
        assertEquals("[[b], [b]]", CommonUtil.deepToString(a));

        Object[] parent = new Object[1];
        Object[] child = new Object[1];
        parent[0] = child;
        child[0] = parent;
        assertEquals("[[[...]]]", CommonUtil.deepToString(parent));
    }

    @Test
    public void testDeepToString_ObjectArray_defaultIfNull() {
        assertEquals("fallback", CommonUtil.deepToString(null, "fallback"));
        assertEquals("[]", CommonUtil.deepToString(new Object[0], "fallback"));
    }

    @Test
    public void checkFromToIndex_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkFromToIndex(0, 0, 0));
        assertDoesNotThrow(() -> CommonUtil.checkFromToIndex(0, 5, 10));
        assertDoesNotThrow(() -> CommonUtil.checkFromToIndex(5, 10, 10));
        assertDoesNotThrow(() -> CommonUtil.checkFromToIndex(0, 0, 10));
        assertDoesNotThrow(() -> CommonUtil.checkFromToIndex(10, 10, 10));
    }

    @Test
    public void checkFromToIndex_invalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(-1, 5, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(0, 11, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(6, 5, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(0, 1, 0));
    }

    @Test
    public void checkFromIndexSize_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkFromIndexSize(0, 0, 0));
        assertDoesNotThrow(() -> CommonUtil.checkFromIndexSize(0, 5, 10));
        assertDoesNotThrow(() -> CommonUtil.checkFromIndexSize(5, 5, 10));
        assertDoesNotThrow(() -> CommonUtil.checkFromIndexSize(0, 0, 10));
        assertDoesNotThrow(() -> CommonUtil.checkFromIndexSize(10, 0, 10));
    }

    @Test
    public void checkFromIndexSize_invalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromIndexSize(-1, 5, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromIndexSize(0, 11, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromIndexSize(6, 5, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromIndexSize(0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkFromIndexSize(0, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkFromIndexSize(0, 1, -1));
    }

    @Test
    public void checkIndex_valid() {
        assertEquals(0, CommonUtil.checkIndex(0, 1));
        assertEquals(5, CommonUtil.checkIndex(5, 10));
        assertEquals(9, CommonUtil.checkIndex(9, 10));
    }

    @Test
    public void checkIndex_invalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkIndex(-1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkIndex(10, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkIndex(0, 0));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkIndex(0, -1));
    }

    @Test
    public void checkElementIndex_valid() {
        assertEquals(0, CommonUtil.checkElementIndex(0, 1));
        assertEquals(5, CommonUtil.checkElementIndex(5, 10));
        assertEquals(9, CommonUtil.checkElementIndex(9, 10));
    }

    @Test
    public void checkElementIndex_invalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(-1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(10, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(0, 0));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementIndex(0, -1));
    }

    @Test
    public void checkElementIndexWithDesc_valid() {
        assertEquals(0, CommonUtil.checkElementIndex(0, 1, "testIndex"));
        assertEquals(5, CommonUtil.checkElementIndex(5, 10, "testIndex"));
    }

    @Test
    public void checkElementIndexWithDesc_invalid() {
        IndexOutOfBoundsException ex1 = assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(-1, 10, "testIndex"));
        assertTrue(ex1.getMessage().contains("testIndex (-1) must not be negative"));

        IndexOutOfBoundsException ex2 = assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(10, 10, "testIndex"));
        assertTrue(ex2.getMessage().contains("testIndex (10) must be less than size (10)"));

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementIndex(0, -1, "testIndex"));
        assertTrue(ex3.getMessage().contains("negative size: -1"));
    }

    @Test
    public void checkPositionIndex_valid() {
        assertEquals(0, CommonUtil.checkPositionIndex(0, 0));
        assertEquals(0, CommonUtil.checkPositionIndex(0, 10));
        assertEquals(5, CommonUtil.checkPositionIndex(5, 10));
        assertEquals(10, CommonUtil.checkPositionIndex(10, 10));
    }

    @Test
    public void checkPositionIndex_invalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkPositionIndex(-1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkPositionIndex(11, 10));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkPositionIndex(0, -1));
    }

    @Test
    public void checkPositionIndexWithDesc_valid() {
        assertEquals(0, CommonUtil.checkPositionIndex(0, 0, "testPosIndex"));
        assertEquals(5, CommonUtil.checkPositionIndex(5, 10, "testPosIndex"));
        assertEquals(10, CommonUtil.checkPositionIndex(10, 10, "testPosIndex"));
    }

    @Test
    public void checkPositionIndexWithDesc_invalid() {
        IndexOutOfBoundsException ex1 = assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkPositionIndex(-1, 10, "testPosIndex"));
        assertTrue(ex1.getMessage().contains("testPosIndex (-1) must not be negative"));

        IndexOutOfBoundsException ex2 = assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkPositionIndex(11, 10, "testPosIndex"));
        assertTrue(ex2.getMessage().contains("testPosIndex (11) must not be greater than size (10)"));

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkPositionIndex(0, -1, "testPosIndex"));
        assertTrue(ex3.getMessage().contains("negative size: -1"));
    }

    @Test
    public void checkArgNotNull_object_valid() {
        String obj = "test";
        assertSame(obj, CommonUtil.checkArgNotNull(obj));
        Integer num = 1;
        assertSame(num, CommonUtil.checkArgNotNull(num));
    }

    @Test
    public void checkArgNotNull_object_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null));
    }

    @Test
    public void checkArgNotNull_objectWithMessage_valid() {
        String obj = "test";
        assertSame(obj, CommonUtil.checkArgNotNull(obj, "testObject"));
    }

    @Test
    public void checkArgNotNull_objectWithMessage_invalid() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null, "testObject"));
        assertEquals("'testObject' cannot be null", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.checkArgNotNull(null, "Custom error message for null object"));
        assertEquals("Custom error message for null object", ex2.getMessage());
    }

    @Test
    public void checkArgNotEmpty_charSequence_valid() {
        String s = "test";
        assertSame(s, CommonUtil.checkArgNotEmpty(s, "charSeq"));
        StringBuilder sb = new StringBuilder("abc");
        assertSame(sb, CommonUtil.checkArgNotEmpty(sb, "charSeqBuilder"));
    }

    @Test
    public void checkArgNotEmpty_charSequence_invalid() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((String) null, "charSeq"));
        assertEquals("'charSeq' cannot be null or empty", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty("", "charSeq"));
        assertEquals("'charSeq' cannot be null or empty", ex2.getMessage());

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.checkArgNotEmpty((String) null, "Custom error message for null/empty charSeq"));
        assertEquals("Custom error message for null/empty charSeq", ex3.getMessage());
    }

    @Test
    public void checkArgNotEmpty_booleanArray_valid() {
        boolean[] arr = { true, false };
        assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "boolArray"));
    }

    @Test
    public void checkArgNotEmpty_booleanArray_invalid() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((boolean[]) null, "boolArray"));
        assertEquals("'boolArray' cannot be null or empty", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new boolean[0], "boolArray"));
        assertEquals("'boolArray' cannot be null or empty", ex2.getMessage());
    }

    @Test
    public void checkArgNotEmpty_charArray_valid() {
        char[] arr = { 'a', 'b' };
        assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "charArr"));
    }

    @Test
    public void checkArgNotEmpty_charArray_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((char[]) null, "charArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new char[0], "charArr"));
    }

    @Test
    public void checkArgNotEmpty_byteArray_valid() {
        byte[] arr = { 1, 2 };
        assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "byteArr"));
    }

    @Test
    public void checkArgNotEmpty_byteArray_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((byte[]) null, "byteArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new byte[0], "byteArr"));
    }

    @Test
    public void checkArgNotEmpty_shortArray_valid() {
        short[] arr = { 1, 2 };
        assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "shortArr"));
    }

    @Test
    public void checkArgNotEmpty_shortArray_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((short[]) null, "shortArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new short[0], "shortArr"));
    }

    @Test
    public void checkArgNotEmpty_intArray_valid() {
        int[] arr = { 1, 2 };
        assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "intArr"));
    }

    @Test
    public void checkArgNotEmpty_intArray_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((int[]) null, "intArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new int[0], "intArr"));
    }

    @Test
    public void checkArgNotEmpty_longArray_valid() {
        long[] arr = { 1L, 2L };
        assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "longArr"));
    }

    @Test
    public void checkArgNotEmpty_longArray_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((long[]) null, "longArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new long[0], "longArr"));
    }

    @Test
    public void checkArgNotEmpty_floatArray_valid() {
        float[] arr = { 1.0f, 2.0f };
        assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "floatArr"));
    }

    @Test
    public void checkArgNotEmpty_floatArray_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((float[]) null, "floatArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new float[0], "floatArr"));
    }

    @Test
    public void checkArgNotEmpty_doubleArray_valid() {
        double[] arr = { 1.0, 2.0 };
        assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "doubleArr"));
    }

    @Test
    public void checkArgNotEmpty_doubleArray_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((double[]) null, "doubleArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new double[0], "doubleArr"));
    }

    @Test
    public void checkArgNotEmpty_objectArray_valid() {
        String[] arr = { "a", "b" };
        assertSame(arr, CommonUtil.checkArgNotEmpty(arr, "objArr"));
    }

    @Test
    public void checkArgNotEmpty_objectArray_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Object[]) null, "objArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new Object[0], "objArr"));
    }

    @Test
    public void checkArgNotEmpty_collection_valid() {
        List<String> list = Arrays.asList("a", "b");
        assertSame(list, CommonUtil.checkArgNotEmpty(list, "coll"));
    }

    @Test
    public void checkArgNotEmpty_collection_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Collection<?>) null, "coll"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(Collections.emptyList(), "coll"));
    }

    @Test
    public void checkArgNotEmpty_iterable_valid() {
        Iterable<String> iterable = Arrays.asList("a", "b");
        assertSame(iterable, CommonUtil.checkArgNotEmpty(iterable, "iterable"));
    }

    @Test
    public void checkArgNotEmpty_iterable_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Iterable<?>) null, "iterable"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(Collections.emptyList(), "iterable"));
    }

    @Test
    public void checkArgNotEmpty_iterator_valid() {
        Iterator<String> iterator = Arrays.asList("a", "b").iterator();
        assertSame(iterator, CommonUtil.checkArgNotEmpty(iterator, "iterator"));
    }

    @Test
    public void checkArgNotEmpty_iterator_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Iterator<?>) null, "iterator"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(Collections.emptyIterator(), "iterator"));

        List<String> listWithOne = new ArrayList<>(Collections.singletonList("a"));
        Iterator<String> iter = listWithOne.iterator();
        iter.next();
        Iterator<String> consumedIterator = Arrays.asList("a").iterator();
        consumedIterator.next();
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(consumedIterator, "consumedIterator"));

        Iterator<String> trulyEmptyIterator = Collections.emptyIterator();
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(trulyEmptyIterator, "trulyEmptyIterator"));

    }

    @Test
    public void checkArgNotEmpty_map_valid() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertSame(map, CommonUtil.checkArgNotEmpty(map, "map"));
    }

    @Test
    public void checkArgNotEmpty_map_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Map<?, ?>) null, "map"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(Collections.emptyMap(), "map"));
    }

    @Test
    public void checkArgNotEmpty_primitiveList_valid() {
        BooleanList pList = BooleanList.of(true, false);
        assertSame(pList, CommonUtil.checkArgNotEmpty(pList, "pList"));
    }

    @Test
    public void checkArgNotEmpty_primitiveList_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((BooleanList) null, "pList"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new BooleanList(), "pList"));
    }

    @Test
    public void checkArgNotEmpty_multiset_valid() {
        Multiset<String> multiset = Multiset.of("a", "b");
        assertSame(multiset, CommonUtil.checkArgNotEmpty(multiset, "multiset"));
    }

    @Test
    public void checkArgNotEmpty_multiset_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Multiset<String>) null, "multiset"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(CommonUtil.newMultiset(), "multiset"));
    }

    @Test
    public void checkArgNotEmpty_multimap_valid() {
        Multimap<String, Integer, List<Integer>> multimap = CommonUtil.newListMultimap(Map.of("a", 1));
        assertSame(multimap, CommonUtil.checkArgNotEmpty(multimap, "multimap"));
    }

    @Test
    public void checkArgNotEmpty_multimap_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Multimap<?, ?, ?>) null, "multimap"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(CommonUtil.newListMultimap(), "multimap"));
    }

    @Test
    public void checkArgNotEmpty_dataset_valid() {
        List<String> columnNames = Arrays.asList("col1");
        List<List<?>> rows = new ArrayList<>();
        rows.add(Arrays.asList("val1"));
        Dataset dataset = CommonUtil.newDataset(columnNames, rows);
        assertSame(dataset, CommonUtil.checkArgNotEmpty(dataset, "dataset"));
    }

    @Test
    public void checkArgNotEmpty_dataset_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Dataset) null, "dataset"));
        Dataset emptyDs = CommonUtil.emptyDataset();
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(emptyDs, "dataset"));
    }

    @Test
    public void checkArgNotBlank_valid() {
        String s = "test";
        assertSame(s, CommonUtil.checkArgNotBlank(s, "notBlankStr"));
        String s2 = "  test  ";
        assertSame(s2, CommonUtil.checkArgNotBlank(s2, "notBlankStrWithSpaces"));
    }

    @Test
    public void checkArgNotBlank_invalid() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank(null, "notBlankStr"));
        assertEquals("'notBlankStr' cannot be null or empty or blank", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank("", "notBlankStr"));
        assertEquals("'notBlankStr' cannot be null or empty or blank", ex2.getMessage());

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank("   ", "notBlankStr"));
        assertEquals("'notBlankStr' cannot be null or empty or blank", ex3.getMessage());

        IllegalArgumentException ex4 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank(null, "Custom error for blank string"));
        assertEquals("Custom error for blank string", ex4.getMessage());
    }

    @Test
    public void checkArgNotNegative_byte_valid() {
        assertEquals((byte) 0, CommonUtil.checkArgNotNegative((byte) 0, "byteArg"));
        assertEquals((byte) 10, CommonUtil.checkArgNotNegative((byte) 10, "byteArg"));
    }

    @Test
    public void checkArgNotNegative_byte_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative((byte) -1, "byteArg"));
        assertEquals("'byteArg' cannot be negative: -1", ex.getMessage());
        IllegalArgumentException exCustom = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative((byte) -1, "Custom msg"));
        assertEquals("Custom msg", exCustom.getMessage());
    }

    @Test
    public void checkArgNotNegative_short_valid() {
        assertEquals((short) 0, CommonUtil.checkArgNotNegative((short) 0, "shortArg"));
        assertEquals((short) 100, CommonUtil.checkArgNotNegative((short) 100, "shortArg"));
    }

    @Test
    public void checkArgNotNegative_short_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative((short) -1, "shortArg"));
    }

    @Test
    public void checkArgNotNegative_int_valid() {
        assertEquals(0, CommonUtil.checkArgNotNegative(0, "intArg"));
        assertEquals(1000, CommonUtil.checkArgNotNegative(1000, "intArg"));
    }

    @Test
    public void checkArgNotNegative_int_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1, "intArg"));
    }

    @Test
    public void checkArgNotNegative_long_valid() {
        assertEquals(0L, CommonUtil.checkArgNotNegative(0L, "longArg"));
        assertEquals(10000L, CommonUtil.checkArgNotNegative(10000L, "longArg"));
    }

    @Test
    public void checkArgNotNegative_long_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1L, "longArg"));
    }

    @Test
    public void checkArgNotNegative_float_valid() {
        assertEquals(0.0f, CommonUtil.checkArgNotNegative(0.0f, "floatArg"), 0.0f);
        assertEquals(10.5f, CommonUtil.checkArgNotNegative(10.5f, "floatArg"), 0.0f);
    }

    @Test
    public void checkArgNotNegative_float_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-0.1f, "floatArg"));
    }

    @Test
    public void checkArgNotNegative_double_valid() {
        assertEquals(0.0, CommonUtil.checkArgNotNegative(0.0, "doubleArg"), 0.0);
        assertEquals(10.5, CommonUtil.checkArgNotNegative(10.5, "doubleArg"), 0.0);
    }

    @Test
    public void checkArgNotNegative_double_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-0.1, "doubleArg"));
    }

    @Test
    public void checkArgPositive_byte_valid() {
        assertEquals((byte) 1, CommonUtil.checkArgPositive((byte) 1, "byteArg"));
        assertEquals((byte) 127, CommonUtil.checkArgPositive((byte) 127, "byteArg"));
    }

    @Test
    public void checkArgPositive_byte_invalid() {
        IllegalArgumentException ex0 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((byte) 0, "byteArg"));
        assertEquals("'byteArg' cannot be zero or negative: 0", ex0.getMessage());

        IllegalArgumentException exNeg = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((byte) -1, "byteArg"));
        assertEquals("'byteArg' cannot be zero or negative: -1", exNeg.getMessage());

        IllegalArgumentException exCustom = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((byte) 0, "Custom msg"));
        assertEquals("Custom msg", exCustom.getMessage());
    }

    @Test
    public void checkArgPositive_short_valid() {
        assertEquals((short) 1, CommonUtil.checkArgPositive((short) 1, "shortArg"));
    }

    @Test
    public void checkArgPositive_short_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((short) 0, "shortArg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((short) -1, "shortArg"));
    }

    @Test
    public void checkArgPositive_int_valid() {
        assertEquals(1, CommonUtil.checkArgPositive(1, "intArg"));
    }

    @Test
    public void checkArgPositive_int_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0, "intArg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-1, "intArg"));
    }

    @Test
    public void checkArgPositive_long_valid() {
        assertEquals(1L, CommonUtil.checkArgPositive(1L, "longArg"));
    }

    @Test
    public void checkArgPositive_long_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0L, "longArg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-1L, "longArg"));
    }

    @Test
    public void checkArgPositive_float_valid() {
        assertEquals(0.1f, CommonUtil.checkArgPositive(0.1f, "floatArg"), 0.0f);
    }

    @Test
    public void checkArgPositive_float_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0.0f, "floatArg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-0.1f, "floatArg"));
    }

    @Test
    public void checkArgPositive_double_valid() {
        assertEquals(0.1, CommonUtil.checkArgPositive(0.1, "doubleArg"), 0.0);
    }

    @Test
    public void checkArgPositive_double_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0.0, "doubleArg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-0.1, "doubleArg"));
    }

    @Test
    public void checkElementNotNull_array_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull(new String[] { "a", "b" }));
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull(new String[0]));
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull((Object[]) null));
    }

    @Test
    public void checkElementNotNull_array_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(new String[] { "a", null, "b" }));
        assertEquals("null element is found in array", ex.getMessage());
    }

    @Test
    public void checkElementNotNull_arrayWithMessage_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull(new String[] { "a", "b" }, "myArray"));
    }

    @Test
    public void checkElementNotNull_arrayWithMessage_invalid() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.checkElementNotNull(new String[] { "a", null }, "myArray"));
        assertEquals("null element is found in myArray", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.checkElementNotNull(new String[] { null }, "Custom error for null element in array"));
        assertEquals("Custom error for null element in array", ex2.getMessage());
    }

    @Test
    public void checkElementNotNull_collection_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull(Arrays.asList("a", "b")));
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull(Collections.emptyList()));
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull((Collection<?>) null));
    }

    @Test
    public void checkElementNotNull_collection_invalid() {
        List<String> listWithNull = new ArrayList<>();
        listWithNull.add("a");
        listWithNull.add(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(listWithNull));
        assertEquals("null element is found in collection", ex.getMessage());
    }

    @Test
    public void checkElementNotNull_collectionWithMessage_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull(Arrays.asList("a", "b"), "myColl"));
    }

    @Test
    public void checkElementNotNull_collectionWithMessage_invalid() {
        List<String> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(listWithNull, "myColl"));
        assertEquals("null element is found in myColl", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.checkElementNotNull(listWithNull, "Custom error for null element in collection"));
        assertEquals("Custom error for null element in collection", ex2.getMessage());
    }

    @Test
    public void checkKeyNotNull_map_valid() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", null);
        assertDoesNotThrow(() -> CommonUtil.checkKeyNotNull(map));
        assertDoesNotThrow(() -> CommonUtil.checkKeyNotNull(Collections.emptyMap()));
        assertDoesNotThrow(() -> CommonUtil.checkKeyNotNull((Map<?, ?>) null));
    }

    @Test
    public void checkKeyNotNull_map_invalid() {
        Map<String, Integer> mapWithNullKey = new HashMap<>();
        mapWithNullKey.put(null, 1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkKeyNotNull(mapWithNullKey));
        assertEquals("null key is found in Map", ex.getMessage());
    }

    @Test
    public void checkKeyNotNull_mapWithMessage_valid() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertDoesNotThrow(() -> CommonUtil.checkKeyNotNull(map, "myMap"));
    }

    @Test
    public void checkKeyNotNull_mapWithMessage_invalid() {
        Map<String, Integer> mapWithNullKey = new HashMap<>();
        mapWithNullKey.put(null, 1);
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkKeyNotNull(mapWithNullKey, "myMap"));
        assertEquals("null key is found in myMap", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.checkKeyNotNull(mapWithNullKey, "Custom error for null key"));
        assertEquals("Custom error for null key", ex2.getMessage());
    }

    @Test
    public void checkValueNotNull_map_valid() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertDoesNotThrow(() -> CommonUtil.checkValueNotNull(map));
        Map<Object, String> mapWithNullKey = new HashMap<>();
        mapWithNullKey.put(null, "value");
        assertDoesNotThrow(() -> CommonUtil.checkValueNotNull(mapWithNullKey));
        assertDoesNotThrow(() -> CommonUtil.checkValueNotNull(Collections.emptyMap()));
        assertDoesNotThrow(() -> CommonUtil.checkValueNotNull((Map<?, ?>) null));
    }

    @Test
    public void checkValueNotNull_map_invalid() {
        Map<String, Integer> mapWithNullValue = new HashMap<>();
        mapWithNullValue.put("a", null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkValueNotNull(mapWithNullValue));
        assertEquals("null value is found in Map", ex.getMessage());
    }

    @Test
    public void checkValueNotNull_mapWithMessage_valid() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertDoesNotThrow(() -> CommonUtil.checkValueNotNull(map, "myMap"));
    }

    @Test
    public void checkValueNotNull_mapWithMessage_invalid() {
        Map<String, Integer> mapWithNullValue = new HashMap<>();
        mapWithNullValue.put("a", null);
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkValueNotNull(mapWithNullValue, "myMap"));
        assertEquals("null value is found in myMap", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.checkValueNotNull(mapWithNullValue, "Custom error for null value"));
        assertEquals("Custom error for null value", ex2.getMessage());
    }

    @Test
    public void checkArgument_boolean_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true));
    }

    @Test
    public void checkArgument_boolean_invalid() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false));
    }

    @Test
    public void checkArgument_booleanObject_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "Error message"));
    }

    @Test
    public void checkArgument_booleanObject_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Error message"));
        assertEquals("Error message", ex.getMessage());
        IllegalArgumentException exNum = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, 123));
        assertEquals("123", exNum.getMessage());
    }

    @Test
    public void checkArgument_booleanTemplateArgs_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "Message with %s", "arg"));
    }

    @Test
    public void checkArgument_booleanTemplateArgs_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Message with {}", "arg"));
        assertEquals("Message with arg", ex.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "{} and %s", "arg1", "arg2"));
        assertEquals("arg1 and %s: [arg2]", ex2.getMessage());

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s and %s", "arg1", "arg2"));
        assertEquals("arg1 and arg2", ex3.getMessage());

        IllegalArgumentException ex4 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "No placeholder", "arg1", "arg2"));
        assertEquals("No placeholder: [arg1, arg2]", ex4.getMessage());

        IllegalArgumentException ex5 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "One {} two", "arg1", "arg2"));
        assertEquals("One arg1 two: [arg2]", ex5.getMessage());
    }

    @Test
    public void checkArgument_booleanTemplateChar_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "Char: {}", 'a'));
    }

    @Test
    public void checkArgument_booleanTemplateChar_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Char: {}", 'a'));
        assertEquals("Char: a", ex.getMessage());
    }

    @Test
    public void checkArgument_booleanTemplateInt_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "Int: {}", 10));
    }

    @Test
    public void checkArgument_booleanTemplateInt_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Int: {}", 10));
        assertEquals("Int: 10", ex.getMessage());
    }

    @Test
    public void checkArgument_booleanTemplateLong_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "Long: {}", 100L));
    }

    @Test
    public void checkArgument_booleanTemplateLong_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Long: {}", 100L));
        assertEquals("Long: 100", ex.getMessage());
    }

    @Test
    public void checkArgument_booleanTemplateDouble_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "Double: {}", 3.14));
    }

    @Test
    public void checkArgument_booleanTemplateDouble_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Double: {}", 3.14));
        assertEquals("Double: 3.14", ex.getMessage());
    }

    @Test
    public void checkArgument_booleanTemplateObject_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "Object: {}", "test"));
    }

    @Test
    public void checkArgument_booleanTemplateObject_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Object: {}", "test"));
        assertEquals("Object: test", ex.getMessage());
    }

    @Test
    public void checkArgument_booleanTemplateCharChar_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "{}, {}", 'a', 'b'));
    }

    @Test
    public void checkArgument_booleanTemplateCharChar_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "{}, {}", 'a', 'b'));
        assertEquals("a, b", ex.getMessage());
    }

    @Test
    public void checkArgument_booleanTemplateIntObject_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "{}, {}", 10, "obj"));
    }

    @Test
    public void checkArgument_booleanTemplateIntObject_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "{}, {}", 10, "obj"));
        assertEquals("10, obj", ex.getMessage());
    }

    @Test
    public void checkArgument_booleanSupplier_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, () -> "Supplier message"));
    }

    @Test
    public void checkArgument_booleanSupplier_invalid() {
        Supplier<String> supplier = () -> "Supplier error message";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, supplier));
        assertEquals("Supplier error message", ex.getMessage());

        final boolean[] supplierCalled = { false };
        Supplier<String> trackingSupplier = () -> {
            supplierCalled[0] = true;
            return "Called";
        };
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, trackingSupplier));
        assertFalse(supplierCalled[0], "Supplier should not be called when condition is true");

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, trackingSupplier));
        assertTrue(supplierCalled[0], "Supplier should be called when condition is false");
    }

    @Test
    public void checkState_boolean_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkState(true));
    }

    @Test
    public void checkState_boolean_invalid() {
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false));
    }

    @Test
    public void checkState_booleanObject_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkState(true, "Error message"));
    }

    @Test
    public void checkState_booleanObject_invalid() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Error message"));
        assertEquals("Error message", ex.getMessage());
        IllegalStateException exNum = assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, 123));
        assertEquals("123", exNum.getMessage());
    }

    @Test
    public void checkState_booleanTemplateArgs_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkState(true, "Message with %s", "arg"));
    }

    @Test
    public void checkState_booleanTemplateArgs_invalid() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Message with {}", "arg"));
        assertEquals("Message with arg", ex.getMessage());

        IllegalStateException ex2 = assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "{} and %s", "arg1", "arg2"));
        assertEquals("arg1 and %s: [arg2]", ex2.getMessage());

        IllegalStateException ex3 = assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "%s and %s", "arg1", "arg2"));
        assertEquals("arg1 and arg2", ex3.getMessage());

        IllegalStateException ex4 = assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "No placeholder", "arg1", "arg2"));
        assertEquals("No placeholder: [arg1, arg2]", ex4.getMessage());
    }

    @Test
    public void checkState_booleanTemplateChar_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkState(true, "Char: {}", 'a'));
    }

    @Test
    public void checkState_booleanTemplateChar_invalid() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Char: {}", 'a'));
        assertEquals("Char: a", ex.getMessage());
    }

    @Test
    public void checkState_booleanTemplateInt_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkState(true, "Int: {}", 10));
    }

    @Test
    public void checkState_booleanTemplateInt_invalid() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Int: {}", 10));
        assertEquals("Int: 10", ex.getMessage());
    }

    @Test
    public void checkState_booleanTemplateLong_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkState(true, "Long: {}", 100L));
    }

    @Test
    public void checkState_booleanTemplateLong_invalid() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Long: {}", 100L));
        assertEquals("Long: 100", ex.getMessage());
    }

    @Test
    public void checkState_booleanTemplateDouble_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkState(true, "Double: {}", 3.14));
    }

    @Test
    public void checkState_booleanTemplateDouble_invalid() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Double: {}", 3.14));
        assertEquals("Double: 3.14", ex.getMessage());
    }

    @Test
    public void checkState_booleanTemplateObject_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkState(true, "Object: {}", "test"));
    }

    @Test
    public void checkState_booleanTemplateObject_invalid() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "Object: {}", "test"));
        assertEquals("Object: test", ex.getMessage());
    }

    @Test
    public void checkState_booleanTemplateCharChar_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkState(true, "{}, {}", 'a', 'b'));
    }

    @Test
    public void checkState_booleanTemplateCharChar_invalid() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "{}, {}", 'a', 'b'));
        assertEquals("a, b", ex.getMessage());
    }

    @Test
    public void checkState_booleanTemplateIntObject_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkState(true, "{}, {}", 10, "obj"));
    }

    @Test
    public void checkState_booleanTemplateIntObject_invalid() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, "{}, {}", 10, "obj"));
        assertEquals("10, obj", ex.getMessage());
    }

    @Test
    public void checkState_booleanSupplier_valid() {
        assertDoesNotThrow(() -> CommonUtil.checkState(true, () -> "Supplier message"));
    }

    @Test
    public void checkState_booleanSupplier_invalid() {
        Supplier<String> supplier = () -> "Supplier error message for state";
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, supplier));
        assertEquals("Supplier error message for state", ex.getMessage());

        final boolean[] supplierCalled = { false };
        Supplier<String> trackingSupplier = () -> {
            supplierCalled[0] = true;
            return "Called";
        };
        assertDoesNotThrow(() -> CommonUtil.checkState(true, trackingSupplier));
        assertFalse(supplierCalled[0], "Supplier should not be called when condition is true");

        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, trackingSupplier));
        assertTrue(supplierCalled[0], "Supplier should be called when condition is false");
    }

    @Test
    public void requireNonNull_object_valid() {
        String obj = "test";
        assertSame(obj, CommonUtil.requireNonNull(obj));
        Integer num = 1;
        assertSame(num, CommonUtil.requireNonNull(num));
    }

    @Test
    public void requireNonNull_object_invalid() {
        assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null));
    }

    @Test
    public void requireNonNull_objectWithMessage_valid() {
        String obj = "test";
        assertSame(obj, CommonUtil.requireNonNull(obj, "testObject"));
    }

    @Test
    public void requireNonNull_objectWithMessage_invalid() {
        NullPointerException ex1 = assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null, "testObject"));
        assertEquals("'testObject' cannot be null", ex1.getMessage());

        NullPointerException ex2 = assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null, "Custom error message for null object"));
        assertEquals("Custom error message for null object", ex2.getMessage());
    }

    @Test
    public void requireNonNull_objectWithSupplier_valid() {
        String obj = "test";
        assertSame(obj, CommonUtil.requireNonNull(obj, () -> "This should not be called"));
    }

    @Test
    public void requireNonNull_objectWithSupplier_invalid() {
        final boolean[] supplierCalled = { false };
        Supplier<String> supplier = () -> {
            supplierCalled[0] = true;
            return "paramName";
        };

        NullPointerException ex1 = assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null, supplier));
        assertTrue(supplierCalled[0]);
        assertEquals("'paramName' cannot be null", ex1.getMessage());

        supplierCalled[0] = false;
        Supplier<String> supplierCustomMsg = () -> {
            supplierCalled[0] = true;
            return "Custom detailed error message from supplier";
        };
        NullPointerException ex2 = assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null, supplierCustomMsg));
        assertTrue(supplierCalled[0]);
        assertEquals("Custom detailed error message from supplier", ex2.getMessage());

        final boolean[] supplierNotCalled = { false };
        Supplier<String> trackingSupplier = () -> {
            supplierNotCalled[0] = true;
            return "Should not be called";
        };
        String validObj = "I am valid";
        assertSame(validObj, CommonUtil.requireNonNull(validObj, trackingSupplier));
        assertFalse(supplierNotCalled[0], "Supplier should not be called for non-null object");

    }

    @Test
    public void testCheckArgNotEmpty_CharSequence_Valid() {
        String result = CommonUtil.checkArgNotEmpty("test", "argName");
        Assertions.assertEquals("test", result);

        StringBuilder sb = new StringBuilder("builder");
        StringBuilder result2 = CommonUtil.checkArgNotEmpty(sb, "argName");
        Assertions.assertEquals(sb, result2);
    }

    @Test
    public void testCheckArgNotEmpty_CharSequence_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty((String) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_CharSequence_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty("", "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_BooleanArray_Valid() {
        boolean[] arr = { true, false };
        boolean[] result = CommonUtil.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_BooleanArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty((boolean[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_BooleanArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty(new boolean[0], "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_CharArray_Valid() {
        char[] arr = { 'a', 'b' };
        char[] result = CommonUtil.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_CharArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty((char[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_CharArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty(new char[0], "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_ByteArray_Valid() {
        byte[] arr = { 1, 2 };
        byte[] result = CommonUtil.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_ByteArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty((byte[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_ByteArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty(new byte[0], "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_ShortArray_Valid() {
        short[] arr = { 1, 2 };
        short[] result = CommonUtil.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_ShortArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty((short[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_ShortArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty(new short[0], "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_IntArray_Valid() {
        int[] arr = { 1, 2 };
        int[] result = CommonUtil.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_IntArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty((int[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_IntArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty(new int[0], "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_LongArray_Valid() {
        long[] arr = { 1L, 2L };
        long[] result = CommonUtil.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_LongArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty((long[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_LongArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty(new long[0], "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_FloatArray_Valid() {
        float[] arr = { 1.0f, 2.0f };
        float[] result = CommonUtil.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_FloatArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty((float[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_FloatArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty(new float[0], "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_DoubleArray_Valid() {
        double[] arr = { 1.0, 2.0 };
        double[] result = CommonUtil.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_DoubleArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty((double[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_DoubleArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty(new double[0], "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_ObjectArray_Valid() {
        String[] arr = { "a", "b" };
        String[] result = CommonUtil.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_ObjectArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty((String[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_ObjectArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty(new String[0], "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_Collection_Valid() {
        List<String> list = Arrays.asList("a", "b");
        List<String> result = CommonUtil.checkArgNotEmpty(list, "argName");
        Assertions.assertEquals(list, result);

        Set<Integer> set = new HashSet<>();
        set.add(1);
        Set<Integer> result2 = CommonUtil.checkArgNotEmpty(set, "argName");
        Assertions.assertEquals(set, result2);
    }

    @Test
    public void testCheckArgNotEmpty_Collection_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty((Collection<?>) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_Collection_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty(new ArrayList<>(), "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_Iterable_Valid() {
        List<String> list = Arrays.asList("a", "b");
        Iterable<String> iterable = list;
        Iterable<String> result = CommonUtil.checkArgNotEmpty(iterable, "argName");
        Assertions.assertEquals(iterable, result);
    }

    @Test
    public void testCheckArgNotEmpty_Iterable_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty((Iterable<?>) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_Iterable_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            List<String> list = new ArrayList<>();
            CommonUtil.checkArgNotEmpty((Iterable<String>) list, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_Iterator_Valid() {
        List<String> list = Arrays.asList("a", "b");
        Iterator<String> iterator = list.iterator();
        Iterator<String> result = CommonUtil.checkArgNotEmpty(iterator, "argName");
        Assertions.assertEquals(iterator, result);
    }

    @Test
    public void testCheckArgNotEmpty_Iterator_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty((Iterator<?>) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_Iterator_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            List<String> list = new ArrayList<>();
            CommonUtil.checkArgNotEmpty(list.iterator(), "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_Map_Valid() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Map<String, Integer> result = CommonUtil.checkArgNotEmpty(map, "argName");
        Assertions.assertEquals(map, result);
    }

    @Test
    public void testCheckArgNotEmpty_Map_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty((Map<?, ?>) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_Map_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgNotEmpty(new HashMap<>(), "argName");
        });
    }

    @Test
    public void testCheckArgument_Varargs_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error message %s %s", "arg1", "arg2");
            CommonUtil.checkArgument(true, "Error message {} {}", "arg1", "arg2");
        });
    }

    @Test
    public void testCheckArgument_Varargs_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error message %s %s", "arg1", "arg2");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error message {} {}", "arg1", "arg2");
        });
    }

    @Test
    public void testCheckArgument_Char_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error with char: %s", 'a');
        });
    }

    @Test
    public void testCheckArgument_Char_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error with char: %s", 'a');
        });
    }

    @Test
    public void testCheckArgument_Int_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error with int: %s", 42);
        });
    }

    @Test
    public void testCheckArgument_Int_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error with int: %s", 42);
        });
    }

    @Test
    public void testCheckArgument_Long_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error with long: %s", 42L);
        });
    }

    @Test
    public void testCheckArgument_Long_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error with long: %s", 42L);
        });
    }

    @Test
    public void testCheckArgument_Double_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error with double: %s", 3.14);
        });
    }

    @Test
    public void testCheckArgument_Double_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error with double: %s", 3.14);
        });
    }

    @Test
    public void testCheckArgument_Object_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error with object: %s", "test");
        });
    }

    @Test
    public void testCheckArgument_Object_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error with object: %s", "test");
        });
    }

    @Test
    public void testCheckArgument_CharChar_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", 'a', 'b');
        });
    }

    @Test
    public void testCheckArgument_CharChar_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", 'a', 'b');
        });
    }

    @Test
    public void testCheckArgument_CharInt_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", 'a', 42);
        });
    }

    @Test
    public void testCheckArgument_CharInt_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", 'a', 42);
        });
    }

    @Test
    public void testCheckArgument_CharLong_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", 'a', 42L);
        });
    }

    @Test
    public void testCheckArgument_CharLong_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", 'a', 42L);
        });
    }

    @Test
    public void testCheckArgument_CharDouble_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", 'a', 3.14);
        });
    }

    @Test
    public void testCheckArgument_CharDouble_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", 'a', 3.14);
        });
    }

    @Test
    public void testCheckArgument_CharObject_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", 'a', "test");
        });
    }

    @Test
    public void testCheckArgument_CharObject_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", 'a', "test");
        });
    }

    @Test
    public void testCheckArgument_IntChar_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", 42, 'a');
        });
    }

    @Test
    public void testCheckArgument_IntChar_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", 42, 'a');
        });
    }

    @Test
    public void testCheckArgument_IntInt_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", 42, 24);
        });
    }

    @Test
    public void testCheckArgument_IntInt_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", 42, 24);
        });
    }

    @Test
    public void testCheckArgument_IntLong_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", 42, 24L);
        });
    }

    @Test
    public void testCheckArgument_IntLong_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", 42, 24L);
        });
    }

    @Test
    public void testCheckArgument_IntDouble_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", 42, 3.14);
        });
    }

    @Test
    public void testCheckArgument_IntDouble_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", 42, 3.14);
        });
    }

    @Test
    public void testCheckArgument_IntObject_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", 42, "test");
        });
    }

    @Test
    public void testCheckArgument_IntObject_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", 42, "test");
        });
    }

    @Test
    public void testCheckArgument_LongChar_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", 42L, 'a');
        });
    }

    @Test
    public void testCheckArgument_LongChar_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", 42L, 'a');
        });
    }

    @Test
    public void testCheckArgument_LongInt_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", 42L, 24);
        });
    }

    @Test
    public void testCheckArgument_LongInt_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", 42L, 24);
        });
    }

    @Test
    public void testCheckArgument_LongLong_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", 42L, 24L);
        });
    }

    @Test
    public void testCheckArgument_LongLong_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", 42L, 24L);
        });
    }

    @Test
    public void testCheckArgument_LongDouble_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", 42L, 3.14);
        });
    }

    @Test
    public void testCheckArgument_LongDouble_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", 42L, 3.14);
        });
    }

    @Test
    public void testCheckArgument_LongObject_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", 42L, "test");
        });
    }

    @Test
    public void testCheckArgument_LongObject_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", 42L, "test");
        });
    }

    @Test
    public void testCheckArgument_DoubleChar_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", 3.14, 'a');
        });
    }

    @Test
    public void testCheckArgument_DoubleChar_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", 3.14, 'a');
        });
    }

    @Test
    public void testCheckArgument_DoubleInt_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", 3.14, 42);
        });
    }

    @Test
    public void testCheckArgument_DoubleInt_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", 3.14, 42);
        });
    }

    @Test
    public void testCheckArgument_DoubleLong_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", 3.14, 42L);
        });
    }

    @Test
    public void testCheckArgument_DoubleLong_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", 3.14, 42L);
        });
    }

    @Test
    public void testCheckArgument_DoubleDouble_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", 3.14, 2.71);
        });
    }

    @Test
    public void testCheckArgument_DoubleDouble_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", 3.14, 2.71);
        });
    }

    @Test
    public void testCheckArgument_DoubleObject_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", 3.14, "test");
        });
    }

    @Test
    public void testCheckArgument_DoubleObject_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", 3.14, "test");
        });
    }

    @Test
    public void testCheckArgument_ObjectChar_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", "test", 'a');
        });
    }

    @Test
    public void testCheckArgument_ObjectChar_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", "test", 'a');
        });
    }

    @Test
    public void testCheckArgument_ObjectInt_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", "test", 42);
        });
    }

    @Test
    public void testCheckArgument_ObjectInt_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", "test", 42);
        });
    }

    @Test
    public void testCheckArgument_ObjectLong_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", "test", 42L);
        });
    }

    @Test
    public void testCheckArgument_ObjectLong_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", "test", 42L);
        });
    }

    @Test
    public void testCheckArgument_ObjectDouble_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "Error: %s %s", "test", 3.14);
        });
    }

    @Test
    public void testCheckArgument_ObjectDouble_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.checkArgument(false, "Error: %s %s", "test", 3.14);
        });
    }

    @Test
    public void testCheckState_CharChar_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", 'a', 'b');
        });
    }

    @Test
    public void testCheckState_CharChar_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", 'a', 'b');
        });
    }

    @Test
    public void testCheckState_CharInt_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", 'a', 42);
        });
    }

    @Test
    public void testCheckState_CharInt_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", 'a', 42);
        });
    }

    @Test
    public void testCheckState_CharLong_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", 'a', 42L);
        });
    }

    @Test
    public void testCheckState_CharLong_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", 'a', 42L);
        });
    }

    @Test
    public void testCheckState_CharDouble_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", 'a', 3.14);
        });
    }

    @Test
    public void testCheckState_CharDouble_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", 'a', 3.14);
        });
    }

    @Test
    public void testCheckState_CharObject_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", 'a', "test");
        });
    }

    @Test
    public void testCheckState_CharObject_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", 'a', "test");
        });
    }

    @Test
    public void testCheckState_IntChar_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", 42, 'a');
        });
    }

    @Test
    public void testCheckState_IntChar_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", 42, 'a');
        });
    }

    @Test
    public void testCheckState_IntInt_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", 42, 24);
        });
    }

    @Test
    public void testCheckState_IntInt_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", 42, 24);
        });
    }

    @Test
    public void testCheckState_IntLong_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", 42, 24L);
        });
    }

    @Test
    public void testCheckState_IntLong_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", 42, 24L);
        });
    }

    @Test
    public void testCheckState_IntDouble_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", 42, 3.14);
        });
    }

    @Test
    public void testCheckState_IntDouble_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", 42, 3.14);
        });
    }

    @Test
    public void testCheckState_IntObject_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", 42, "test");
        });
    }

    @Test
    public void testCheckState_IntObject_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", 42, "test");
        });
    }

    @Test
    public void testCheckState_LongChar_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", 42L, 'a');
        });
    }

    @Test
    public void testCheckState_LongChar_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", 42L, 'a');
        });
    }

    @Test
    public void testCheckState_LongInt_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", 42L, 24);
        });
    }

    @Test
    public void testCheckState_LongInt_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", 42L, 24);
        });
    }

    @Test
    public void testCheckState_LongLong_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", 42L, 24L);
        });
    }

    @Test
    public void testCheckState_LongLong_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", 42L, 24L);
        });
    }

    @Test
    public void testCheckState_LongDouble_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", 42L, 3.14);
        });
    }

    @Test
    public void testCheckState_LongDouble_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", 42L, 3.14);
        });
    }

    @Test
    public void testCheckState_LongObject_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", 42L, "test");
        });
    }

    @Test
    public void testCheckState_LongObject_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", 42L, "test");
        });
    }

    @Test
    public void testCheckState_DoubleChar_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", 3.14, 'a');
        });
    }

    @Test
    public void testCheckState_DoubleChar_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", 3.14, 'a');
        });
    }

    @Test
    public void testCheckState_DoubleInt_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", 3.14, 42);
        });
    }

    @Test
    public void testCheckState_DoubleInt_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", 3.14, 42);
        });
    }

    @Test
    public void testCheckState_DoubleLong_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", 3.14, 42L);
        });
    }

    @Test
    public void testCheckState_DoubleLong_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", 3.14, 42L);
        });
    }

    @Test
    public void testCheckState_DoubleDouble_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", 3.14, 2.71);
        });
    }

    @Test
    public void testCheckState_DoubleDouble_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", 3.14, 2.71);
        });
    }

    @Test
    public void testCheckState_DoubleObject_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", 3.14, "test");
        });
    }

    @Test
    public void testCheckState_DoubleObject_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", 3.14, "test");
        });
    }

    @Test
    public void testCheckState_ObjectChar_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", "test", 'a');
        });
    }

    @Test
    public void testCheckState_ObjectChar_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", "test", 'a');
        });
    }

    @Test
    public void testCheckState_ObjectInt_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", "test", 42);
        });
    }

    @Test
    public void testCheckState_ObjectInt_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", "test", 42);
        });
    }

    @Test
    public void testCheckState_ObjectLong_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", "test", 42L);
        });
    }

    @Test
    public void testCheckState_ObjectLong_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", "test", 42L);
        });
    }

    @Test
    public void testCheckState_ObjectDouble_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", "test", 3.14);
        });
    }

    @Test
    public void testCheckState_ObjectDouble_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", "test", 3.14);
        });
    }

    @Test
    public void testCheckState_ObjectObject_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s", "test1", "test2");
        });
    }

    @Test
    public void testCheckState_ObjectObject_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s", "test1", "test2");
        });
    }

    @Test
    public void testCheckState_ThreeObjects_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s %s", "test1", "test2", "test3");
        });
    }

    @Test
    public void testCheckState_ThreeObjects_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s %s", "test1", "test2", "test3");
        });
    }

    @Test
    public void testCheckState_FourObjects_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, "State error: %s %s %s %s", "test1", "test2", "test3", "test4");
        });
    }

    @Test
    public void testCheckState_FourObjects_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, "State error: %s %s %s %s", "test1", "test2", "test3", "test4");
        });
    }

    @Test
    public void testCheckState_Supplier_Pass() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkState(true, () -> "This message should not be created");
        });
    }

    @Test
    public void testCheckState_Supplier_Fail() {
        Supplier<String> messageSupplier = () -> "State error from supplier";
        Assertions.assertThrows(IllegalStateException.class, () -> {
            CommonUtil.checkState(false, messageSupplier);
        });
    }

    @Test
    public void testCheckState_Supplier_MessageCreatedOnlyWhenNeeded() {
        final boolean[] supplierCalled = { false };
        CommonUtil.checkState(true, () -> {
            supplierCalled[0] = true;
            return "Should not be called";
        });
        Assertions.assertFalse(supplierCalled[0]);

        try {
            CommonUtil.checkState(false, () -> {
                supplierCalled[0] = true;
                return "Should be called";
            });
        } catch (IllegalStateException e) {
            Assertions.assertTrue(supplierCalled[0]);
        }
    }

    @Test
    public void testCheckArgument_MessageFormatting() {
        try {
            CommonUtil.checkArgument(false, "Value {} should be less than {}", 10, 5);
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("10"));
            Assertions.assertTrue(e.getMessage().contains("5"));
        }

        try {
            CommonUtil.checkArgument(false, "Value %s should be less than %s", 10, 5);
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("10"));
            Assertions.assertTrue(e.getMessage().contains("5"));
        }

        try {
            CommonUtil.checkArgument(false, "No placeholders", 10, 5);
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("No placeholders"));
            Assertions.assertTrue(e.getMessage().contains("[10, 5]"));
        }

        try {
            CommonUtil.checkArgument(false, "Only one {}", 10, 5);
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("10"));
            Assertions.assertTrue(e.getMessage().contains("[5]"));
        }
    }

    @Test
    public void testCheckArgument_NullFormatting() {
        try {
            CommonUtil.checkArgument(false, "Value is {}", (Object) null);
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("null"));
        }

        try {
            CommonUtil.checkArgument(false, null, "arg1", "arg2");
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("null"));
        }
    }

    @Test
    public void testCompareBooleanArrays() {
        Assertions.assertEquals(0, CommonUtil.compare(new boolean[] {}, new boolean[] {}));
        Assertions.assertEquals(-1, CommonUtil.compare(new boolean[] {}, new boolean[] { true }));
        Assertions.assertEquals(1, CommonUtil.compare(new boolean[] { true }, new boolean[] {}));

        Assertions.assertEquals(0, CommonUtil.compare((boolean[]) null, (boolean[]) null));
        Assertions.assertEquals(-1, CommonUtil.compare((boolean[]) null, new boolean[] { true }));
        Assertions.assertEquals(1, CommonUtil.compare(new boolean[] { true }, (boolean[]) null));

        boolean[] arr1 = { true, false, true };
        boolean[] arr2 = { true, false, true };
        Assertions.assertEquals(0, CommonUtil.compare(arr1, arr2));

        Assertions.assertEquals(-1, CommonUtil.compare(new boolean[] { false }, new boolean[] { true }));
        Assertions.assertEquals(1, CommonUtil.compare(new boolean[] { true }, new boolean[] { false }));

        Assertions.assertEquals(-1, CommonUtil.compare(new boolean[] { true }, new boolean[] { true, false }));
        Assertions.assertEquals(1, CommonUtil.compare(new boolean[] { true, false }, new boolean[] { true }));

        boolean[] largeArr1 = new boolean[2000];
        boolean[] largeArr2 = new boolean[2000];
        Arrays.fill(largeArr1, true);
        Arrays.fill(largeArr2, true);
        largeArr2[1999] = false;
        Assertions.assertEquals(1, CommonUtil.compare(largeArr1, largeArr2));
    }

    @Test
    public void testCompareBooleanArraysWithRange() {
        boolean[] arr1 = { true, false, true, false };
        boolean[] arr2 = { false, true, false, true };

        Assertions.assertEquals(0, CommonUtil.compare(arr1, 1, arr2, 2, 2));

        Assertions.assertEquals(1, CommonUtil.compare(arr1, 0, arr2, 0, 1));
        Assertions.assertEquals(-1, CommonUtil.compare(arr1, 1, arr2, 1, 1));

        Assertions.assertEquals(0, CommonUtil.compare(arr1, 0, arr2, 0, 0));

        Assertions.assertEquals(0, CommonUtil.compare(arr1, 1, arr1, 1, 2));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.compare(arr1, 0, arr2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.compare(arr1, 0, arr2, 0, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.compare(arr1, 3, arr2, 0, 2));

        boolean[] largeArr1 = new boolean[2000];
        boolean[] largeArr2 = new boolean[2000];
        Arrays.fill(largeArr1, true);
        Arrays.fill(largeArr2, true);
        largeArr2[1500] = false;
        Assertions.assertEquals(1, CommonUtil.compare(largeArr1, 0, largeArr2, 0, 1600));
    }

    @Test
    public void testCompareCharArrays() {
        Assertions.assertEquals(0, CommonUtil.compare(new char[] {}, new char[] {}));
        Assertions.assertEquals(-1, CommonUtil.compare(new char[] {}, new char[] { 'a' }));
        Assertions.assertEquals(1, CommonUtil.compare(new char[] { 'a' }, new char[] {}));

        Assertions.assertEquals(0, CommonUtil.compare((char[]) null, (char[]) null));
        Assertions.assertEquals(-1, CommonUtil.compare((char[]) null, new char[] { 'a' }));
        Assertions.assertEquals(1, CommonUtil.compare(new char[] { 'a' }, (char[]) null));

        char[] arr1 = { 'a', 'b', 'c' };
        char[] arr2 = { 'a', 'b', 'c' };
        Assertions.assertEquals(0, CommonUtil.compare(arr1, arr2));

        Assertions.assertEquals(-1, CommonUtil.compare(new char[] { 'a' }, new char[] { 'b' }));
        Assertions.assertEquals(1, CommonUtil.compare(new char[] { 'b' }, new char[] { 'a' }));

        Assertions.assertEquals(-1, CommonUtil.compare(new char[] { 'a' }, new char[] { 'a', 'b' }));
        Assertions.assertEquals(1, CommonUtil.compare(new char[] { 'a', 'b' }, new char[] { 'a' }));

        char[] largeArr1 = new char[2000];
        char[] largeArr2 = new char[2000];
        Arrays.fill(largeArr1, 'x');
        Arrays.fill(largeArr2, 'x');
        largeArr2[1999] = 'y';
        Assertions.assertEquals(-1, CommonUtil.compare(largeArr1, largeArr2));
    }

    @Test
    public void testCompareCharArraysWithRange() {
        char[] arr1 = { 'a', 'b', 'c', 'd' };
        char[] arr2 = { 'x', 'b', 'c', 'y' };

        Assertions.assertEquals(0, CommonUtil.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, CommonUtil.compare(arr1, 0, arr2, 0, 1));
        Assertions.assertEquals(-1, CommonUtil.compare(arr1, 3, arr2, 3, 1));

        Assertions.assertEquals(0, CommonUtil.compare(arr1, 0, arr2, 0, 0));

        Assertions.assertEquals(0, CommonUtil.compare(arr1, 1, arr1, 1, 2));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.compare(arr1, 0, arr2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.compare(arr1, 0, arr2, 0, 10));

        char[] bugArr1 = { 'a', 'b' };
        char[] bugArr2 = { 'x', 'a' };
        Assertions.assertTrue(CommonUtil.compare(bugArr1, 1, bugArr2, 1, 1) > 0);
    }

    @Test
    public void testCompareCharArraysWithRange001() {

        {
            char[] arr1 = { 1, 2, 3, 4 };
            char[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, CommonUtil.compare(arr1, 0, arr2, 1, 3));
        }

        {
            byte[] arr1 = { 1, 2, 3, 4 };
            byte[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, CommonUtil.compare(arr1, 0, arr2, 1, 3));
        }

        {
            short[] arr1 = { 1, 2, 3, 4 };
            short[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, CommonUtil.compare(arr1, 0, arr2, 1, 3));
        }

        {
            int[] arr1 = { 1, 2, 3, 4 };
            int[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, CommonUtil.compare(arr1, 0, arr2, 1, 3));
        }

        {
            long[] arr1 = { 1, 2, 3, 4 };
            long[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, CommonUtil.compare(arr1, 0, arr2, 1, 3));
        }

        {
            float[] arr1 = { 1, 2, 3, 4 };
            float[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, CommonUtil.compare(arr1, 0, arr2, 1, 3));
        }

        {
            double[] arr1 = { 1, 2, 3, 4 };
            double[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, CommonUtil.compare(arr1, 0, arr2, 1, 3));
        }

    }

    @Test
    public void testCompareByteArrays() {
        Assertions.assertEquals(0, CommonUtil.compare(new byte[] {}, new byte[] {}));
        Assertions.assertEquals(-1, CommonUtil.compare(new byte[] {}, new byte[] { 1 }));
        Assertions.assertEquals(1, CommonUtil.compare(new byte[] { 1 }, new byte[] {}));

        byte[] arr1 = { 1, 2, 3 };
        byte[] arr2 = { 1, 2, 3 };
        Assertions.assertEquals(0, CommonUtil.compare(arr1, arr2));

        Assertions.assertEquals(-1, CommonUtil.compare(new byte[] { 1 }, new byte[] { 2 }));
        Assertions.assertEquals(1, CommonUtil.compare(new byte[] { 2 }, new byte[] { 1 }));

        Assertions.assertEquals(-1, CommonUtil.compare(new byte[] { -128 }, new byte[] { 127 }));
        Assertions.assertEquals(1, CommonUtil.compare(new byte[] { 127 }, new byte[] { -128 }));
    }

    @Test
    public void testCompareByteArraysWithRange() {
        byte[] arr1 = { 1, 2, 3, 4 };
        byte[] arr2 = { 5, 2, 3, 6 };

        Assertions.assertEquals(0, CommonUtil.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, CommonUtil.compare(arr1, 0, arr2, 0, 1));
        Assertions.assertEquals(-1, CommonUtil.compare(arr1, 3, arr2, 0, 1));
    }

    @Test
    public void testCompareUnsignedByteArrays() {
        byte[] arr1 = { (byte) 255 };
        byte[] arr2 = { 1 };
        Assertions.assertTrue(CommonUtil.compareUnsigned(arr1, arr2) > 0);

        Assertions.assertEquals(0, CommonUtil.compareUnsigned(new byte[] {}, new byte[] {}));
        Assertions.assertEquals(-1, CommonUtil.compareUnsigned(new byte[] {}, new byte[] { 1 }));
        Assertions.assertEquals(1, CommonUtil.compareUnsigned(new byte[] { 1 }, new byte[] {}));
    }

    @Test
    public void testCompareUnsignedByteArraysWithRange() {
        byte[] arr1 = { 1, (byte) 255, 3 };
        byte[] arr2 = { 5, 1, 3 };

        Assertions.assertTrue(CommonUtil.compareUnsigned(arr1, 1, arr2, 1, 1) > 0);

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.compareUnsigned(arr1, 0, arr2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.compareUnsigned(arr1, 0, arr2, 0, 10));
    }

    @Test
    public void testCompareShortArrays() {
        Assertions.assertEquals(0, CommonUtil.compare(new short[] {}, new short[] {}));
        Assertions.assertEquals(-1, CommonUtil.compare(new short[] {}, new short[] { 1 }));
        Assertions.assertEquals(1, CommonUtil.compare(new short[] { 1 }, new short[] {}));

        short[] arr1 = { 1, 2, 3 };
        short[] arr2 = { 1, 2, 3 };
        Assertions.assertEquals(0, CommonUtil.compare(arr1, arr2));

        Assertions.assertEquals(-1, CommonUtil.compare(new short[] { 1 }, new short[] { 2 }));
        Assertions.assertEquals(1, CommonUtil.compare(new short[] { 2 }, new short[] { 1 }));
    }

    @Test
    public void testCompareShortArraysWithRange() {
        short[] arr1 = { 1, 2, 3, 4 };
        short[] arr2 = { 5, 2, 3, 6 };

        Assertions.assertEquals(0, CommonUtil.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, CommonUtil.compare(arr1, 0, arr2, 0, 1));
    }

    @Test
    public void testCompareUnsignedShortArrays() {
        short[] arr1 = { (short) 65535 };
        short[] arr2 = { 1 };
        Assertions.assertTrue(CommonUtil.compareUnsigned(arr1, arr2) > 0);

        Assertions.assertEquals(0, CommonUtil.compareUnsigned(new short[] {}, new short[] {}));
    }

    @Test
    public void testCompareUnsignedShortArraysWithRange() {
        short[] arr1 = { 1, (short) 65535, 3 };
        short[] arr2 = { 5, 1, 3 };

        Assertions.assertTrue(CommonUtil.compareUnsigned(arr1, 1, arr2, 1, 1) > 0);
    }

    @Test
    public void testCompareIntArrays() {
        Assertions.assertEquals(0, CommonUtil.compare(new int[] {}, new int[] {}));
        Assertions.assertEquals(-1, CommonUtil.compare(new int[] {}, new int[] { 1 }));
        Assertions.assertEquals(1, CommonUtil.compare(new int[] { 1 }, new int[] {}));

        int[] arr1 = { 1, 2, 3 };
        int[] arr2 = { 1, 2, 3 };
        Assertions.assertEquals(0, CommonUtil.compare(arr1, arr2));

        Assertions.assertEquals(-1, CommonUtil.compare(new int[] { 1 }, new int[] { 2 }));
        Assertions.assertEquals(1, CommonUtil.compare(new int[] { 2 }, new int[] { 1 }));
    }

    @Test
    public void testCompareIntArraysWithRange() {
        int[] arr1 = { 1, 2, 3, 4 };
        int[] arr2 = { 5, 2, 3, 6 };

        Assertions.assertEquals(0, CommonUtil.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, CommonUtil.compare(arr1, 0, arr2, 0, 1));
    }

    @Test
    public void testCompareUnsignedIntArrays() {
        int[] arr1 = { -1 };
        int[] arr2 = { 1 };
        Assertions.assertTrue(CommonUtil.compareUnsigned(arr1, arr2) > 0);

        Assertions.assertEquals(0, CommonUtil.compareUnsigned(new int[] {}, new int[] {}));
    }

    @Test
    public void testCompareUnsignedIntArraysWithRange() {
        int[] arr1 = { 1, -1, 3 };
        int[] arr2 = { 5, 1, 3 };

        Assertions.assertTrue(CommonUtil.compareUnsigned(arr1, 1, arr2, 1, 1) > 0);
    }

    @Test
    public void testCompareLongArrays() {
        Assertions.assertEquals(0, CommonUtil.compare(new long[] {}, new long[] {}));
        Assertions.assertEquals(-1, CommonUtil.compare(new long[] {}, new long[] { 1 }));
        Assertions.assertEquals(1, CommonUtil.compare(new long[] { 1 }, new long[] {}));

        long[] arr1 = { 1L, 2L, 3L };
        long[] arr2 = { 1L, 2L, 3L };
        Assertions.assertEquals(0, CommonUtil.compare(arr1, arr2));

        Assertions.assertEquals(-1, CommonUtil.compare(new long[] { 1L }, new long[] { 2L }));
        Assertions.assertEquals(1, CommonUtil.compare(new long[] { 2L }, new long[] { 1L }));
    }

    @Test
    public void testCompareLongArraysWithRange() {
        long[] arr1 = { 1L, 2L, 3L, 4L };
        long[] arr2 = { 5L, 2L, 3L, 6L };

        Assertions.assertEquals(0, CommonUtil.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, CommonUtil.compare(arr1, 0, arr2, 0, 1));
    }

    @Test
    public void testCompareUnsignedLongArrays() {
        long[] arr1 = { -1L };
        long[] arr2 = { 1L };
        Assertions.assertTrue(CommonUtil.compareUnsigned(arr1, arr2) > 0);

        Assertions.assertEquals(0, CommonUtil.compareUnsigned(new long[] {}, new long[] {}));
    }

    @Test
    public void testCompareUnsignedLongArraysWithRange() {
        long[] arr1 = { 1L, -1L, 3L };
        long[] arr2 = { 5L, 1L, 3L };

        Assertions.assertTrue(CommonUtil.compareUnsigned(arr1, 1, arr2, 1, 1) > 0);
    }

    @Test
    public void testCompareFloatArrays() {
        Assertions.assertEquals(0, CommonUtil.compare(new float[] {}, new float[] {}));
        Assertions.assertEquals(-1, CommonUtil.compare(new float[] {}, new float[] { 1.0f }));
        Assertions.assertEquals(1, CommonUtil.compare(new float[] { 1.0f }, new float[] {}));

        float[] arr1 = { 1.0f, 2.0f, 3.0f };
        float[] arr2 = { 1.0f, 2.0f, 3.0f };
        Assertions.assertEquals(0, CommonUtil.compare(arr1, arr2));

        Assertions.assertEquals(-1, CommonUtil.compare(new float[] { 1.0f }, new float[] { 2.0f }));
        Assertions.assertEquals(1, CommonUtil.compare(new float[] { 2.0f }, new float[] { 1.0f }));

        Assertions.assertTrue(CommonUtil.compare(new float[] { Float.NaN }, new float[] { 1.0f }) > 0);
        Assertions.assertEquals(0, CommonUtil.compare(new float[] { Float.NaN }, new float[] { Float.NaN }));
    }

    @Test
    public void testCompareFloatArraysWithRange() {
        float[] arr1 = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] arr2 = { 5.0f, 2.0f, 3.0f, 6.0f };

        Assertions.assertEquals(0, CommonUtil.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, CommonUtil.compare(arr1, 0, arr2, 0, 1));
    }

    @Test
    public void testCompareDoubleArrays() {
        Assertions.assertEquals(0, CommonUtil.compare(new double[] {}, new double[] {}));
        Assertions.assertEquals(-1, CommonUtil.compare(new double[] {}, new double[] { 1.0 }));
        Assertions.assertEquals(1, CommonUtil.compare(new double[] { 1.0 }, new double[] {}));

        double[] arr1 = { 1.0, 2.0, 3.0 };
        double[] arr2 = { 1.0, 2.0, 3.0 };
        Assertions.assertEquals(0, CommonUtil.compare(arr1, arr2));

        Assertions.assertEquals(-1, CommonUtil.compare(new double[] { 1.0 }, new double[] { 2.0 }));
        Assertions.assertEquals(1, CommonUtil.compare(new double[] { 2.0 }, new double[] { 1.0 }));

        Assertions.assertTrue(CommonUtil.compare(new double[] { Double.NaN }, new double[] { 1.0 }) > 0);
        Assertions.assertEquals(0, CommonUtil.compare(new double[] { Double.NaN }, new double[] { Double.NaN }));
    }

    @Test
    public void testCompareDoubleArraysWithRange() {
        double[] arr1 = { 1.0, 2.0, 3.0, 4.0 };
        double[] arr2 = { 5.0, 2.0, 3.0, 6.0 };

        Assertions.assertEquals(0, CommonUtil.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, CommonUtil.compare(arr1, 0, arr2, 0, 1));
    }

    @Test
    public void testCompareObjectArrays() {
        Assertions.assertEquals(0, CommonUtil.compare(new String[] {}, new String[] {}));
        Assertions.assertEquals(-1, CommonUtil.compare(new String[] {}, new String[] { "a" }));
        Assertions.assertEquals(1, CommonUtil.compare(new String[] { "a" }, new String[] {}));

        String[] arr1 = { "a", "b", "c" };
        String[] arr2 = { "a", "b", "c" };
        Assertions.assertEquals(0, CommonUtil.compare(arr1, arr2));

        Assertions.assertEquals(-1, CommonUtil.compare(new String[] { "a" }, new String[] { "b" }));
        Assertions.assertEquals(1, CommonUtil.compare(new String[] { "b" }, new String[] { "a" }));

        Assertions.assertEquals(-1, CommonUtil.compare(new String[] { null }, new String[] { "a" }));
        Assertions.assertEquals(1, CommonUtil.compare(new String[] { "a" }, new String[] { null }));
        Assertions.assertEquals(0, CommonUtil.compare(new String[] { null }, new String[] { null }));
    }

    @Test
    public void testCompareObjectArraysWithRange() {
        String[] arr1 = { "a", "b", "c", "d" };
        String[] arr2 = { "x", "b", "c", "y" };

        Assertions.assertEquals(0, CommonUtil.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertTrue(CommonUtil.compare(arr1, 0, arr2, 0, 1) < 0);

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.compare(arr1, 0, arr2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.compare(arr1, 0, arr2, 0, 10));
    }

    @Test
    public void testCompareObjectArraysWithComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);

        String[] arr1 = { "a", "b" };
        String[] arr2 = { "a", "c" };
        Assertions.assertTrue(CommonUtil.compare(arr1, arr2, reverseComparator) > 0);

        Assertions.assertEquals(0, CommonUtil.compare(new String[] {}, new String[] {}, reverseComparator));

        Assertions.assertTrue(CommonUtil.compare(arr1, arr2, (Comparator<String>) null) < 0);
    }

    @Test
    public void testCompareObjectArraysWithRangeAndComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        String[] arr1 = { "a", "b", "c", "d" };
        String[] arr2 = { "x", "b", "c", "y" };

        Assertions.assertEquals(0, CommonUtil.compare(arr1, 1, arr2, 1, 2, reverseComparator));

        Assertions.assertTrue(CommonUtil.compare(arr1, 0, arr2, 0, 1, reverseComparator) > 0);
    }

    @Test
    public void testCompareCollectionsWithRange() {
        List<String> list1 = Arrays.asList("a", "b", "c", "d");
        List<String> list2 = Arrays.asList("x", "b", "c", "y");

        Assertions.assertEquals(0, CommonUtil.compare(list1, 1, list2, 1, 2));

        Assertions.assertTrue(CommonUtil.compare(list1, 0, list2, 0, 1) < 0);

        Assertions.assertEquals(0, CommonUtil.compare(list1, 1, list1, 1, 2));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.compare(list1, 0, list2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.compare(list1, 0, list2, 0, 10));
    }

    @Test
    public void testCompareIterables() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        Set<String> set1 = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));

        Assertions.assertEquals(0, CommonUtil.compare(list1, list2));
        Assertions.assertEquals(0, CommonUtil.compare(list1, set1));

        List<String> list3 = Arrays.asList("a", "b", "d");
        Assertions.assertTrue(CommonUtil.compare(list1, list3) < 0);

        Assertions.assertEquals(0, CommonUtil.compare(Collections.<String> emptyList(), Collections.<String> emptyList()));
        Assertions.assertEquals(-1, CommonUtil.compare(Collections.emptyList(), list1));
        Assertions.assertEquals(1, CommonUtil.compare(list1, Collections.emptyList()));
    }

    @Test
    public void testCompareIterators() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");

        Assertions.assertEquals(0, CommonUtil.compare(list1.iterator(), list2.iterator()));

        List<String> list3 = Arrays.asList("a", "b", "d");
        Assertions.assertTrue(CommonUtil.compare(list1.iterator(), list3.iterator()) < 0);

        List<String> list4 = Arrays.asList("a", "b");
        Assertions.assertTrue(CommonUtil.compare(list1.iterator(), list4.iterator()) > 0);

        Assertions.assertEquals(0, CommonUtil.compare((Iterator<String>) null, (Iterator<String>) null));
        Assertions.assertEquals(-1, CommonUtil.compare((Iterator<String>) null, list1.iterator()));
        Assertions.assertEquals(1, CommonUtil.compare(list1.iterator(), (Iterator<String>) null));
    }

    @Test
    public void testCompareCollectionsWithRangeAndComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        List<String> list1 = Arrays.asList("a", "b", "c", "d");
        List<String> list2 = Arrays.asList("x", "b", "c", "y");

        Assertions.assertEquals(0, CommonUtil.compare(list1, 1, list2, 1, 2, reverseComparator));

        Assertions.assertTrue(CommonUtil.compare(list1, 0, list2, 0, 1, reverseComparator) > 0);

        Set<String> set1 = new LinkedHashSet<>(list1);
        Set<String> set2 = new LinkedHashSet<>(list2);
        Assertions.assertEquals(0, CommonUtil.compare(set1, 1, set2, 1, 2, reverseComparator));
    }

    @Test
    public void testCompareIterablesWithComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "d");

        Assertions.assertTrue(CommonUtil.compare(list1, list2, (Comparator<String>) null) < 0);

        Assertions.assertTrue(CommonUtil.compare(list1, list2, reverseComparator) > 0);

        Assertions.assertEquals(0, CommonUtil.compare(Collections.emptyList(), Collections.emptyList(), reverseComparator));
    }

    @Test
    public void testCompareIteratorsWithComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "d");

        Assertions.assertTrue(CommonUtil.compare(list1.iterator(), list2.iterator(), (Comparator<String>) null) < 0);

        Assertions.assertTrue(CommonUtil.compare(list1.iterator(), list2.iterator(), reverseComparator) > 0);
    }

    @Test
    public void testCompareIgnoreCaseArrays() {
        String[] arr1 = { "Hello", "World" };
        String[] arr2 = { "HELLO", "WORLD" };
        Assertions.assertEquals(0, CommonUtil.compareIgnoreCase(arr1, arr2));

        String[] arr3 = { "Hello", "World" };
        String[] arr4 = { "HELLO", "EARTH" };
        Assertions.assertTrue(CommonUtil.compareIgnoreCase(arr3, arr4) > 0);

        Assertions.assertEquals(0, CommonUtil.compareIgnoreCase(new String[] {}, new String[] {}));
        Assertions.assertEquals(-1, CommonUtil.compareIgnoreCase(new String[] {}, arr1));
    }

    @Test
    public void testLessThanWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(CommonUtil.lessThan(1, 2, null));

        Assertions.assertFalse(CommonUtil.lessThan(1, 2, reverseComparator));
        Assertions.assertTrue(CommonUtil.lessThan(2, 1, reverseComparator));
    }

    @Test
    public void testLessThanOrEqualWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(CommonUtil.lessThanOrEqual(1, 2, null));
        Assertions.assertTrue(CommonUtil.lessThanOrEqual(1, 1, null));

        Assertions.assertFalse(CommonUtil.lessThanOrEqual(1, 2, reverseComparator));
        Assertions.assertTrue(CommonUtil.lessThanOrEqual(2, 1, reverseComparator));
    }

    @Test
    public void testGreaterThanWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertFalse(CommonUtil.greaterThan(1, 2, null));

        Assertions.assertTrue(CommonUtil.greaterThan(1, 2, reverseComparator));
        Assertions.assertFalse(CommonUtil.greaterThan(2, 1, reverseComparator));
    }

    @Test
    public void testGreaterThanOrEqualWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertFalse(CommonUtil.greaterThanOrEqual(1, 2, null));
        Assertions.assertTrue(CommonUtil.greaterThanOrEqual(1, 1, null));

        Assertions.assertTrue(CommonUtil.greaterThanOrEqual(1, 2, reverseComparator));
        Assertions.assertFalse(CommonUtil.greaterThanOrEqual(2, 1, reverseComparator));
    }

    @Test
    public void testGtAndLtWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(CommonUtil.gtAndLt(5, 1, 10, null));

        Assertions.assertTrue(CommonUtil.gtAndLt(5, 10, 1, reverseComparator));
        Assertions.assertFalse(CommonUtil.gtAndLt(5, 1, 10, reverseComparator));
    }

    @Test
    public void testGeAndLtWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(CommonUtil.geAndLt(5, 1, 10, null));
        Assertions.assertTrue(CommonUtil.geAndLt(1, 1, 10, null));

        Assertions.assertTrue(CommonUtil.geAndLt(5, 10, 1, reverseComparator));
        Assertions.assertTrue(CommonUtil.geAndLt(10, 10, 1, reverseComparator));
    }

    @Test
    public void testGeAndLeWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(CommonUtil.geAndLe(5, 1, 10, null));
        Assertions.assertTrue(CommonUtil.geAndLe(1, 1, 10, null));
        Assertions.assertTrue(CommonUtil.geAndLe(10, 1, 10, null));

        Assertions.assertTrue(CommonUtil.geAndLe(5, 10, 1, reverseComparator));
        Assertions.assertTrue(CommonUtil.geAndLe(10, 10, 1, reverseComparator));
        Assertions.assertTrue(CommonUtil.geAndLe(1, 10, 1, reverseComparator));
    }

    @Test
    public void testGtAndLeWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(CommonUtil.gtAndLe(5, 1, 10, null));
        Assertions.assertFalse(CommonUtil.gtAndLe(1, 1, 10, null));
        Assertions.assertTrue(CommonUtil.gtAndLe(10, 1, 10, null));

        Assertions.assertTrue(CommonUtil.gtAndLe(5, 10, 1, reverseComparator));
        Assertions.assertFalse(CommonUtil.gtAndLe(10, 10, 1, reverseComparator));
        Assertions.assertTrue(CommonUtil.gtAndLe(1, 10, 1, reverseComparator));
    }

    @Test
    public void testIsBetweenWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(CommonUtil.isBetween(5, 1, 10, null));
        Assertions.assertTrue(CommonUtil.isBetween(5, 10, 1, reverseComparator));
    }

    @Test
    public void testGetElementFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c", "d");

        Assertions.assertEquals("a", CommonUtil.getElement(list, 0));
        Assertions.assertEquals("b", CommonUtil.getElement(list, 1));
        Assertions.assertEquals("d", CommonUtil.getElement(list, 3));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.getElement(list, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.getElement(list, 4));

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals("a", CommonUtil.getElement(set, 0));
        Assertions.assertEquals("b", CommonUtil.getElement(set, 1));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.getElement((Iterable<String>) null, 0));
    }

    @Test
    public void testGetElementFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d");

        Assertions.assertEquals("a", CommonUtil.getElement(list.iterator(), 0));
        Assertions.assertEquals("b", CommonUtil.getElement(list.iterator(), 1));
        Assertions.assertEquals("d", CommonUtil.getElement(list.iterator(), 3));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.getElement(list.iterator(), -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.getElement(list.iterator(), 4));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.getElement((Iterator<String>) null, 0));
    }

    @Test
    public void testGetOnlyElementFromIterable() {
        List<String> singleList = Arrays.asList("only");
        Assertions.assertEquals("only", CommonUtil.getOnlyElement(singleList).get());

        Assertions.assertFalse(CommonUtil.getOnlyElement(Collections.emptyList()).isPresent());

        List<String> multiList = Arrays.asList("a", "b");
        Assertions.assertThrows(TooManyElementsException.class, () -> CommonUtil.getOnlyElement(multiList));

        Assertions.assertFalse(CommonUtil.getOnlyElement((Iterable<String>) null).isPresent());

        Set<String> singleSet = Collections.singleton("only");
        Assertions.assertEquals("only", CommonUtil.getOnlyElement(singleSet).get());
    }

    @Test
    public void testGetOnlyElementFromIterator() {
        List<String> singleList = Arrays.asList("only");
        Assertions.assertEquals("only", CommonUtil.getOnlyElement(singleList.iterator()).get());

        Assertions.assertFalse(CommonUtil.getOnlyElement(Collections.emptyIterator()).isPresent());

        List<String> multiList = Arrays.asList("a", "b");
        Assertions.assertThrows(TooManyElementsException.class, () -> CommonUtil.getOnlyElement(multiList.iterator()));

        Assertions.assertFalse(CommonUtil.getOnlyElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testFirstElementFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", CommonUtil.firstElement(list).get());

        Assertions.assertFalse(CommonUtil.firstElement(Collections.emptyList()).isPresent());

        Assertions.assertFalse(CommonUtil.firstElement((Iterable<String>) null).isPresent());

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("a", CommonUtil.firstElement(arrayList).get());

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals("a", CommonUtil.firstElement(set).get());
    }

    @Test
    public void testFirstElementFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", CommonUtil.firstElement(list.iterator()).get());

        Assertions.assertFalse(CommonUtil.firstElement(Collections.emptyIterator()).isPresent());

        Assertions.assertFalse(CommonUtil.firstElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testLastElementFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", CommonUtil.lastElement(list).get());

        Assertions.assertFalse(CommonUtil.lastElement(Collections.emptyList()).isPresent());

        Assertions.assertFalse(CommonUtil.lastElement((Iterable<String>) null).isPresent());

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("c", CommonUtil.lastElement(arrayList).get());

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals("c", CommonUtil.lastElement(set).get());

        Deque<String> deque = new ArrayDeque<>(list);
        Assertions.assertEquals("c", CommonUtil.lastElement(deque).get());
    }

    @Test
    public void testLastElementFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", CommonUtil.lastElement(list.iterator()).get());

        Assertions.assertFalse(CommonUtil.lastElement(Collections.emptyIterator()).isPresent());

        Assertions.assertFalse(CommonUtil.lastElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testFirstElementsFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), CommonUtil.firstElements(list, 3));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), CommonUtil.firstElements(list, 5));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), CommonUtil.firstElements(list, 10));
        Assertions.assertEquals(Collections.emptyList(), CommonUtil.firstElements(list, 0));

        Assertions.assertEquals(Collections.emptyList(), CommonUtil.firstElements(Collections.emptyList(), 5));

        Assertions.assertEquals(Collections.emptyList(), CommonUtil.firstElements((Iterable<String>) null, 5));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.firstElements(list, -1));

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), CommonUtil.firstElements(set, 3));
    }

    @Test
    public void testFirstElementsFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), CommonUtil.firstElements(list.iterator(), 3));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), CommonUtil.firstElements(list.iterator(), 5));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), CommonUtil.firstElements(list.iterator(), 10));
        Assertions.assertEquals(Collections.emptyList(), CommonUtil.firstElements(list.iterator(), 0));

        Assertions.assertEquals(Collections.emptyList(), CommonUtil.firstElements(Collections.emptyIterator(), 5));

        Assertions.assertEquals(Collections.emptyList(), CommonUtil.firstElements((Iterator<String>) null, 5));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.firstElements(list.iterator(), -1));
    }

    @Test
    public void testLastElementsFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        Assertions.assertEquals(Arrays.asList("c", "d", "e"), CommonUtil.lastElements(list, 3));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), CommonUtil.lastElements(list, 5));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), CommonUtil.lastElements(list, 10));
        Assertions.assertEquals(Collections.emptyList(), CommonUtil.lastElements(list, 0));

        Assertions.assertEquals(Collections.emptyList(), CommonUtil.lastElements(Collections.emptyList(), 5));

        Assertions.assertEquals(Collections.emptyList(), CommonUtil.lastElements((Iterable<String>) null, 5));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.lastElements(list, -1));

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals(Arrays.asList("c", "d", "e"), CommonUtil.lastElements(set, 3));
    }

    @Test
    public void testLastElementsFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        Assertions.assertEquals(Arrays.asList("c", "d", "e"), CommonUtil.lastElements(list.iterator(), 3));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), CommonUtil.lastElements(list.iterator(), 5));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), CommonUtil.lastElements(list.iterator(), 10));
        Assertions.assertEquals(Collections.emptyList(), CommonUtil.lastElements(list.iterator(), 0));

        Assertions.assertEquals(Collections.emptyList(), CommonUtil.lastElements(Collections.emptyIterator(), 5));

        Assertions.assertEquals(Collections.emptyList(), CommonUtil.lastElements((Iterator<String>) null, 5));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.lastElements(list.iterator(), -1));
    }

    @Test
    public void testFirstNonNullTwo() {
        Assertions.assertEquals("a", CommonUtil.firstNonNull("a", "b").get());

        Assertions.assertEquals("b", CommonUtil.firstNonNull(null, "b").get());

        Assertions.assertEquals("a", CommonUtil.firstNonNull("a", null).get());

        Assertions.assertFalse(CommonUtil.firstNonNull(null, null).isPresent());
    }

    @Test
    public void testFirstNonNullThree() {
        Assertions.assertEquals("a", CommonUtil.firstNonNull("a", "b", "c").get());

        Assertions.assertEquals("b", CommonUtil.firstNonNull(null, "b", "c").get());

        Assertions.assertEquals("c", CommonUtil.firstNonNull(null, null, "c").get());

        Assertions.assertFalse(CommonUtil.firstNonNull(null, null, null).isPresent());
    }

    @Test
    public void testFirstNonNullVarargs() {
        Assertions.assertEquals("c", CommonUtil.firstNonNull(null, null, "c", "d").get());

        Assertions.assertFalse(CommonUtil.firstNonNull(new String[] {}).isPresent());

        Assertions.assertFalse(CommonUtil.firstNonNull((String[]) null).isPresent());

        Assertions.assertFalse(CommonUtil.firstNonNull(new String[] { null, null, null }).isPresent());
    }

    @Test
    public void testFirstNonNullIterable() {
        List<String> list = Arrays.asList(null, null, "c", "d");
        Assertions.assertEquals("c", CommonUtil.firstNonNull(list).get());

        Assertions.assertFalse(CommonUtil.firstNonNull(Collections.emptyList()).isPresent());

        Assertions.assertFalse(CommonUtil.firstNonNull((Iterable<String>) null).isPresent());

        List<String> allNulls = Arrays.asList(null, null, null);
        Assertions.assertFalse(CommonUtil.firstNonNull(allNulls).isPresent());
    }

    @Test
    public void testFirstNonNullIterator() {
        List<String> list = Arrays.asList(null, null, "c", "d");
        Assertions.assertEquals("c", CommonUtil.firstNonNull(list.iterator()).get());

        Assertions.assertFalse(CommonUtil.firstNonNull(Collections.emptyIterator()).isPresent());

        Assertions.assertFalse(CommonUtil.firstNonNull((Iterator<String>) null).isPresent());

        List<String> allNulls = Arrays.asList(null, null, null);
        Assertions.assertFalse(CommonUtil.firstNonNull(allNulls.iterator()).isPresent());
    }

    @Test
    public void testLastNonNullTwo() {
        Assertions.assertEquals("b", CommonUtil.lastNonNull("a", "b").get());

        Assertions.assertEquals("b", CommonUtil.lastNonNull(null, "b").get());

        Assertions.assertEquals("a", CommonUtil.lastNonNull("a", null).get());

        Assertions.assertFalse(CommonUtil.lastNonNull(null, null).isPresent());
    }

    @Test
    public void testLastNonNullThree() {
        Assertions.assertEquals("c", CommonUtil.lastNonNull("a", "b", "c").get());

        Assertions.assertEquals("b", CommonUtil.lastNonNull("a", "b", null).get());

        Assertions.assertEquals("a", CommonUtil.lastNonNull("a", null, null).get());

        Assertions.assertFalse(CommonUtil.lastNonNull(null, null, null).isPresent());
    }

    @Test
    public void testLastNonNullVarargs() {
        Assertions.assertEquals("d", CommonUtil.lastNonNull("a", "b", null, "d", null).get());

        Assertions.assertFalse(CommonUtil.lastNonNull(new String[] {}).isPresent());

        Assertions.assertFalse(CommonUtil.lastNonNull((String[]) null).isPresent());

        Assertions.assertFalse(CommonUtil.lastNonNull(new String[] { null, null, null }).isPresent());
    }

    @Test
    public void testLastNonNullIterable() {
        List<String> list = Arrays.asList("a", "b", null, "d", null);
        Assertions.assertEquals("d", CommonUtil.lastNonNull(list).get());

        Assertions.assertFalse(CommonUtil.lastNonNull(Collections.emptyList()).isPresent());

        Assertions.assertFalse(CommonUtil.lastNonNull((Iterable<String>) null).isPresent());

        List<String> allNulls = Arrays.asList(null, null, null);
        Assertions.assertFalse(CommonUtil.lastNonNull(allNulls).isPresent());

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("d", CommonUtil.lastNonNull(arrayList).get());

    }

    @Test
    public void testLastNonNullIterator() {
        List<String> list = Arrays.asList("a", "b", null, "d", null);
        Assertions.assertEquals("d", CommonUtil.lastNonNull(list.iterator()).get());

        Assertions.assertFalse(CommonUtil.lastNonNull(Collections.emptyIterator()).isPresent());

        Assertions.assertFalse(CommonUtil.lastNonNull((Iterator<String>) null).isPresent());

        List<String> allNulls = Arrays.asList(null, null, null);
        Assertions.assertFalse(CommonUtil.lastNonNull(allNulls.iterator()).isPresent());
    }

    @Test
    public void testFirstNonEmptyArraysTwo() {
        String[] arr1 = { "a", "b" };
        String[] arr2 = { "c", "d" };
        String[] empty = {};

        Assertions.assertArrayEquals(arr1, CommonUtil.firstNonEmpty(arr1, arr2).get());

        Assertions.assertArrayEquals(arr2, CommonUtil.firstNonEmpty(empty, arr2).get());

        Assertions.assertArrayEquals(arr1, CommonUtil.firstNonEmpty(arr1, empty).get());

        Assertions.assertFalse(CommonUtil.firstNonEmpty(empty, empty).isPresent());

        Assertions.assertArrayEquals(arr1, CommonUtil.firstNonEmpty(null, arr1).get());
        Assertions.assertArrayEquals(arr1, CommonUtil.firstNonEmpty(arr1, null).get());
        Assertions.assertFalse(CommonUtil.firstNonEmpty((String[]) null, (String[]) null).isPresent());
    }

    @Test
    public void testFirstNonEmptyArraysThree() {
        String[] arr1 = { "a", "b" };
        String[] arr2 = { "c", "d" };
        String[] arr3 = { "e", "f" };
        String[] empty = {};

        Assertions.assertArrayEquals(arr1, CommonUtil.firstNonEmpty(arr1, arr2, arr3).get());

        Assertions.assertArrayEquals(arr2, CommonUtil.firstNonEmpty(empty, arr2, arr3).get());

        Assertions.assertArrayEquals(arr3, CommonUtil.firstNonEmpty(empty, empty, arr3).get());

        Assertions.assertFalse(CommonUtil.firstNonEmpty(empty, empty, empty).isPresent());
    }

    @Test
    public void testFirstNonEmptyCollectionsTwo() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("c", "d");
        List<String> empty = Collections.emptyList();

        Assertions.assertEquals(list1, CommonUtil.firstNonEmpty(list1, list2).get());

        Assertions.assertEquals(list2, CommonUtil.firstNonEmpty(empty, list2).get());

        Assertions.assertEquals(list1, CommonUtil.firstNonEmpty(list1, empty).get());

        Assertions.assertFalse(CommonUtil.firstNonEmpty(empty, empty).isPresent());

        Assertions.assertEquals(list1, CommonUtil.firstNonEmpty(null, list1).get());
        Assertions.assertEquals(list1, CommonUtil.firstNonEmpty(list1, null).get());
        Assertions.assertFalse(CommonUtil.firstNonEmpty((List<String>) null, (List<String>) null).isPresent());
    }

    @Test
    public void testFirstNonEmptyCollectionsThree() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("c", "d");
        List<String> list3 = Arrays.asList("e", "f");
        List<String> empty = Collections.emptyList();

        Assertions.assertEquals(list1, CommonUtil.firstNonEmpty(list1, list2, list3).get());

        Assertions.assertEquals(list2, CommonUtil.firstNonEmpty(empty, list2, list3).get());

        Assertions.assertEquals(list3, CommonUtil.firstNonEmpty(empty, empty, list3).get());

        Assertions.assertFalse(CommonUtil.firstNonEmpty(empty, empty, empty).isPresent());
    }

    @Test
    public void testFirstNonEmptyMapsTwo() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        Map<String, String> empty = Collections.emptyMap();

        Assertions.assertEquals(map1, CommonUtil.firstNonEmpty(map1, map2).get());

        Assertions.assertEquals(map2, CommonUtil.firstNonEmpty(empty, map2).get());

        Assertions.assertEquals(map1, CommonUtil.firstNonEmpty(map1, empty).get());

        Assertions.assertFalse(CommonUtil.firstNonEmpty(empty, empty).isPresent());

        Assertions.assertEquals(map1, CommonUtil.firstNonEmpty(null, map1).get());
        Assertions.assertEquals(map1, CommonUtil.firstNonEmpty(map1, null).get());
        Assertions.assertFalse(CommonUtil.firstNonEmpty((Map<String, String>) null, (Map<String, String>) null).isPresent());
    }

    @Test
    public void testFirstNonEmptyMapsThree() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        Map<String, String> map3 = new HashMap<>();
        map3.put("c", "3");
        Map<String, String> empty = Collections.emptyMap();

        Assertions.assertEquals(map1, CommonUtil.firstNonEmpty(map1, map2, map3).get());

        Assertions.assertEquals(map2, CommonUtil.firstNonEmpty(empty, map2, map3).get());

        Assertions.assertEquals(map3, CommonUtil.firstNonEmpty(empty, empty, map3).get());

        Assertions.assertFalse(CommonUtil.firstNonEmpty(empty, empty, empty).isPresent());
    }

    @Test
    public void testFirstNonEmptyCharSequencesTwo() {
        Assertions.assertEquals("hello", CommonUtil.firstNonEmpty("hello", "world").get());

        Assertions.assertEquals("world", CommonUtil.firstNonEmpty("", "world").get());

        Assertions.assertEquals("hello", CommonUtil.firstNonEmpty("hello", "").get());

        Assertions.assertFalse(CommonUtil.firstNonEmpty("", "").isPresent());

        Assertions.assertEquals("hello", CommonUtil.firstNonEmpty(null, "hello").get());
        Assertions.assertEquals("hello", CommonUtil.firstNonEmpty("hello", null).get());
        Assertions.assertFalse(CommonUtil.firstNonEmpty((String) null, (String) null).isPresent());

        StringBuilder sb = new StringBuilder("builder");
        StringBuffer buf = new StringBuffer("buffer");
        Assertions.assertEquals(sb, CommonUtil.firstNonEmpty(sb, buf).get());
    }

    @Test
    public void testFirstNonEmptyCharSequencesThree() {
        Assertions.assertEquals("a", CommonUtil.firstNonEmpty("a", "b", "c").get());

        Assertions.assertEquals("b", CommonUtil.firstNonEmpty("", "b", "c").get());

        Assertions.assertEquals("c", CommonUtil.firstNonEmpty("", "", "c").get());

        Assertions.assertFalse(CommonUtil.firstNonEmpty("", "", "").isPresent());

        Assertions.assertEquals("c", CommonUtil.firstNonEmpty(null, "", "c").get());
    }

    @Test
    public void testFirstNonEmptyCharSequencesVarargs() {
        Assertions.assertEquals("c", CommonUtil.firstNonEmpty("", null, "c", "d").get());

        Assertions.assertFalse(CommonUtil.firstNonEmpty(new String[] {}).isPresent());

        Assertions.assertFalse(CommonUtil.firstNonEmpty((String[]) null).isPresent());

        Assertions.assertFalse(CommonUtil.firstNonEmpty("", "", "").isPresent());
    }

    @Test
    public void testFirstNonEmptyCharSequencesIterable() {
        List<String> list = Arrays.asList("", null, "c", "d");
        Assertions.assertEquals("c", CommonUtil.firstNonEmpty(list).get());

        Assertions.assertFalse(CommonUtil.firstNonEmpty(Collections.<String> emptyList()).isPresent());

        Assertions.assertFalse(CommonUtil.firstNonEmpty((Iterable<String>) null).isPresent());

        List<String> allEmpty = Arrays.asList("", "", null);
        Assertions.assertFalse(CommonUtil.firstNonEmpty(allEmpty).isPresent());
    }

    @Test
    public void testFirstNonBlankTwo() {
        Assertions.assertEquals("hello", CommonUtil.firstNonBlank("hello", "world").get());

        Assertions.assertEquals("world", CommonUtil.firstNonBlank("  ", "world").get());

        Assertions.assertEquals("hello", CommonUtil.firstNonBlank("hello", "  ").get());

        Assertions.assertFalse(CommonUtil.firstNonBlank("  ", "  ").isPresent());

        Assertions.assertEquals("hello", CommonUtil.firstNonBlank(null, "hello").get());
        Assertions.assertEquals("hello", CommonUtil.firstNonBlank("hello", null).get());
        Assertions.assertFalse(CommonUtil.firstNonBlank((String) null, (String) null).isPresent());
    }

    @Test
    public void testFirstNonBlankThree() {
        Assertions.assertEquals("a", CommonUtil.firstNonBlank("a", "b", "c").get());

        Assertions.assertEquals("b", CommonUtil.firstNonBlank("  ", "b", "c").get());

        Assertions.assertEquals("c", CommonUtil.firstNonBlank("  ", "  ", "c").get());

        Assertions.assertFalse(CommonUtil.firstNonBlank("  ", "  ", "  ").isPresent());

        Assertions.assertEquals("c", CommonUtil.firstNonBlank(null, "", "c").get());
        Assertions.assertEquals("c", CommonUtil.firstNonBlank("  ", "\t", "c").get());
    }

    @Test
    public void testFirstNonBlankVarargs() {
        Assertions.assertEquals("c", CommonUtil.firstNonBlank("  ", null, "c", "d").get());

        Assertions.assertFalse(CommonUtil.firstNonBlank(new String[] {}).isPresent());

        Assertions.assertFalse(CommonUtil.firstNonBlank((String[]) null).isPresent());

        Assertions.assertFalse(CommonUtil.firstNonBlank("  ", "\t", "\n").isPresent());
    }

    @Test
    public void testFirstNonBlankIterable() {
        List<String> list = Arrays.asList("  ", null, "c", "d");
        Assertions.assertEquals("c", CommonUtil.firstNonBlank(list).get());

        Assertions.assertFalse(CommonUtil.firstNonBlank(Collections.<String> emptyList()).isPresent());

        Assertions.assertFalse(CommonUtil.firstNonBlank((Iterable<String>) null).isPresent());

        List<String> allBlank = Arrays.asList("  ", "\t", null);
        Assertions.assertFalse(CommonUtil.firstNonBlank(allBlank).isPresent());
    }

    @Test
    public void testFirstOrNullIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals("a", CommonUtil.firstOrNullIfEmpty(arr));

        Assertions.assertNull(CommonUtil.firstOrNullIfEmpty(new String[] {}));

        Assertions.assertNull(CommonUtil.firstOrNullIfEmpty((String[]) null));
    }

    @Test
    public void testFirstOrNullIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", CommonUtil.firstOrNullIfEmpty(list));

        Assertions.assertNull(CommonUtil.firstOrNullIfEmpty(Collections.emptyList()));

        Assertions.assertNull(CommonUtil.firstOrNullIfEmpty((Iterable<String>) null));
    }

    @Test
    public void testFirstOrNullIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", CommonUtil.firstOrNullIfEmpty(list.iterator()));

        Assertions.assertNull(CommonUtil.firstOrNullIfEmpty(Collections.emptyIterator()));

        Assertions.assertNull(CommonUtil.firstOrNullIfEmpty((Iterator<String>) null));
    }

    @Test
    public void testFirstOrDefaultIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals("a", CommonUtil.firstOrDefaultIfEmpty(arr, "default"));

        Assertions.assertEquals("default", CommonUtil.firstOrDefaultIfEmpty(new String[] {}, "default"));

        Assertions.assertEquals("default", CommonUtil.firstOrDefaultIfEmpty((String[]) null, "default"));
    }

    @Test
    public void testFirstOrDefaultIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", CommonUtil.firstOrDefaultIfEmpty(list, "default"));

        Assertions.assertEquals("default", CommonUtil.firstOrDefaultIfEmpty(Collections.emptyList(), "default"));

        Assertions.assertEquals("default", CommonUtil.firstOrDefaultIfEmpty((Iterable<String>) null, "default"));

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("a", CommonUtil.firstOrDefaultIfEmpty(arrayList, "default"));
    }

    @Test
    public void testFirstOrDefaultIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", CommonUtil.firstOrDefaultIfEmpty(list.iterator(), "default"));

        Assertions.assertEquals("default", CommonUtil.firstOrDefaultIfEmpty(Collections.emptyIterator(), "default"));

        Assertions.assertEquals("default", CommonUtil.firstOrDefaultIfEmpty((Iterator<String>) null, "default"));
    }

    @Test
    public void testLastOrNullIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals("c", CommonUtil.lastOrNullIfEmpty(arr));

        Assertions.assertNull(CommonUtil.lastOrNullIfEmpty(new String[] {}));

        Assertions.assertNull(CommonUtil.lastOrNullIfEmpty((String[]) null));
    }

    @Test
    public void testLastOrNullIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", CommonUtil.lastOrNullIfEmpty(list));

        Assertions.assertNull(CommonUtil.lastOrNullIfEmpty(Collections.emptyList()));

        Assertions.assertNull(CommonUtil.lastOrNullIfEmpty((Iterable<String>) null));
    }

    @Test
    public void testLastOrNullIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", CommonUtil.lastOrNullIfEmpty(list.iterator()));

        Assertions.assertNull(CommonUtil.lastOrNullIfEmpty(Collections.emptyIterator()));

        Assertions.assertNull(CommonUtil.lastOrNullIfEmpty((Iterator<String>) null));
    }

    @Test
    public void testLastOrDefaultIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals("c", CommonUtil.lastOrDefaultIfEmpty(arr, "default"));

        Assertions.assertEquals("default", CommonUtil.lastOrDefaultIfEmpty(new String[] {}, "default"));

        Assertions.assertEquals("default", CommonUtil.lastOrDefaultIfEmpty((String[]) null, "default"));
    }

    @Test
    public void testLastOrDefaultIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", CommonUtil.lastOrDefaultIfEmpty(list, "default"));

        Assertions.assertEquals("default", CommonUtil.lastOrDefaultIfEmpty(Collections.emptyList(), "default"));

        Assertions.assertEquals("default", CommonUtil.lastOrDefaultIfEmpty((Iterable<String>) null, "default"));

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("c", CommonUtil.lastOrDefaultIfEmpty(arrayList, "default"));

        Deque<String> deque = new ArrayDeque<>(list);
        Assertions.assertEquals("c", CommonUtil.lastOrDefaultIfEmpty(deque, "default"));
    }

    @Test
    public void testLastOrDefaultIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", CommonUtil.lastOrDefaultIfEmpty(list.iterator(), "default"));

        Assertions.assertEquals("default", CommonUtil.lastOrDefaultIfEmpty(Collections.emptyIterator(), "default"));

        Assertions.assertEquals("default", CommonUtil.lastOrDefaultIfEmpty((Iterator<String>) null, "default"));
    }

    @Test
    public void testFindFirstArray() {
        String[] arr = { "apple", "banana", "cherry", "date" };
        Predicate<String> startsWithC = s -> s.startsWith("c");
        Predicate<String> startsWithX = s -> s.startsWith("x");

        Assertions.assertEquals("cherry", CommonUtil.findFirst(arr, startsWithC).get());

        Assertions.assertFalse(CommonUtil.findFirst(arr, startsWithX).isPresent());

        Assertions.assertFalse(CommonUtil.findFirst(new String[] {}, startsWithC).isPresent());

        Assertions.assertFalse(CommonUtil.findFirst((String[]) null, startsWithC).isPresent());
    }

    @Test
    public void testFindFirstIterable() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date");
        Predicate<String> startsWithC = s -> s.startsWith("c");
        Predicate<String> startsWithX = s -> s.startsWith("x");

        Assertions.assertEquals("cherry", CommonUtil.findFirst(list, startsWithC).get());

        Assertions.assertFalse(CommonUtil.findFirst(list, startsWithX).isPresent());

        Assertions.assertFalse(CommonUtil.findFirst(Collections.emptyList(), startsWithC).isPresent());

        Assertions.assertFalse(CommonUtil.findFirst((Iterable<String>) null, startsWithC).isPresent());
    }

    @Test
    public void testFindFirstIterator() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date");
        Predicate<String> startsWithC = s -> s.startsWith("c");
        Predicate<String> startsWithX = s -> s.startsWith("x");

        Assertions.assertEquals("cherry", CommonUtil.findFirst(list.iterator(), startsWithC).get());

        Assertions.assertFalse(CommonUtil.findFirst(list.iterator(), startsWithX).isPresent());

        Assertions.assertFalse(CommonUtil.findFirst(Collections.emptyIterator(), startsWithC).isPresent());

        Assertions.assertFalse(CommonUtil.findFirst((Iterator<String>) null, startsWithC).isPresent());
    }

    @Test
    public void testFindLastArray() {
        String[] arr = { "apple", "banana", "cherry", "date", "cucumber" };
        Predicate<String> startsWithC = s -> s.startsWith("c");
        Predicate<String> startsWithX = s -> s.startsWith("x");

        Assertions.assertEquals("cucumber", CommonUtil.findLast(arr, startsWithC).get());

        Assertions.assertFalse(CommonUtil.findLast(arr, startsWithX).isPresent());

        Assertions.assertFalse(CommonUtil.findLast(new String[] {}, startsWithC).isPresent());

        Assertions.assertFalse(CommonUtil.findLast((String[]) null, startsWithC).isPresent());
    }

    @Test
    public void testFindLastIterable() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date", "cucumber");
        Predicate<String> startsWithC = s -> s.startsWith("c");
        Predicate<String> startsWithX = s -> s.startsWith("x");

        Assertions.assertEquals("cucumber", CommonUtil.findLast(list, startsWithC).get());

        Assertions.assertFalse(CommonUtil.findLast(list, startsWithX).isPresent());

        Assertions.assertFalse(CommonUtil.findLast(Collections.emptyList(), startsWithC).isPresent());

        Assertions.assertFalse(CommonUtil.findLast((Iterable<String>) null, startsWithC).isPresent());

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("cucumber", CommonUtil.findLast(arrayList, startsWithC).get());

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals("cucumber", CommonUtil.findLast(set, startsWithC).get());

        Deque<String> deque = new ArrayDeque<>(list);
        Assertions.assertEquals("cucumber", CommonUtil.findLast(deque, startsWithC).get());
    }

    @Test
    public void testConvertNullToClass() {
        assertNull(CommonUtil.convert(null, String.class));
        assertNull(CommonUtil.convert(null, Integer.class));
        assertNull(CommonUtil.convert(null, Long.class));
        assertNull(CommonUtil.convert(null, Float.class));
        assertNull(CommonUtil.convert(null, Double.class));
        assertNull(CommonUtil.convert(null, Boolean.class));
        assertNull(CommonUtil.convert(null, Object.class));
        assertNull(CommonUtil.convert(null, Date.class));
        assertNull(CommonUtil.convert(null, List.class));
        assertNull(CommonUtil.convert(null, Map.class));
    }

    @Test
    public void testConvertStringToNumbers() {
        assertEquals(123, CommonUtil.convert("123", Integer.class).intValue());
        assertEquals(123L, CommonUtil.convert("123", Long.class).longValue());
        assertEquals(123.45f, CommonUtil.convert("123.45", Float.class).floatValue(), 0.001);
        assertEquals(123.45d, CommonUtil.convert("123.45", Double.class).doubleValue(), 0.001);
        assertEquals((byte) 123, CommonUtil.convert("123", Byte.class).byteValue());
        assertEquals((short) 123, CommonUtil.convert("123", Short.class).shortValue());
        assertEquals(new BigInteger("123456789012345678901234567890"), CommonUtil.convert("123456789012345678901234567890", BigInteger.class));
        assertEquals(new BigDecimal("123.456789"), CommonUtil.convert("123.456789", BigDecimal.class));
        assertNull(CommonUtil.convert("", Integer.class));
    }

    @Test
    public void testConvertStringToNumbersWithInvalidFormat() {
        assertThrows(NumberFormatException.class, () -> CommonUtil.convert("abc", Integer.class));
        assertThrows(NumberFormatException.class, () -> CommonUtil.convert("12.34.56", Double.class));
    }

    @Test
    public void testConvertStringToBoolean() {
        assertTrue(CommonUtil.convert("true", Boolean.class));
        assertTrue(CommonUtil.convert("TRUE", Boolean.class));
        assertTrue(CommonUtil.convert("True", Boolean.class));
        assertFalse(CommonUtil.convert("false", Boolean.class));
        assertFalse(CommonUtil.convert("FALSE", Boolean.class));
        assertFalse(CommonUtil.convert("False", Boolean.class));
        assertFalse(CommonUtil.convert("anything else", Boolean.class));
    }

    @Test
    public void testConvertStringToCharacter() {
        assertEquals('A', CommonUtil.convert("A", Character.class).charValue());
        assertEquals('1', CommonUtil.convert("1", Character.class).charValue());
        assertThrows(RuntimeException.class, () -> CommonUtil.convert("AB", Character.class));
        assertNull(CommonUtil.convert("", Character.class));
    }

    @Test
    public void testConvertNumberToNumber() {
        Integer intVal = 123;
        assertEquals(123L, CommonUtil.convert(intVal, Long.class).longValue());
        assertEquals(123.0f, CommonUtil.convert(intVal, Float.class).floatValue(), 0.000001f);
        assertEquals(123.0d, CommonUtil.convert(intVal, Double.class).doubleValue(), 0.000001d);
        assertEquals((byte) 123, CommonUtil.convert(intVal, Byte.class).byteValue());
        assertEquals((short) 123, CommonUtil.convert(intVal, Short.class).shortValue());

        Long longVal = 456L;
        assertEquals(456, CommonUtil.convert(longVal, Integer.class).intValue());
        assertEquals(456.0f, CommonUtil.convert(longVal, Float.class).floatValue(), 0.000001f);
        assertEquals(456.0d, CommonUtil.convert(longVal, Double.class).doubleValue(), 0.000001d);

        Double doubleVal = 789.5;
        assertEquals(789, CommonUtil.convert(doubleVal, Integer.class).intValue());
        assertEquals(789L, CommonUtil.convert(doubleVal, Long.class).longValue());
        assertEquals(789.5f, CommonUtil.convert(doubleVal, Float.class).floatValue(), 0.001);
    }

    @Test
    public void testConvertNumberToBoolean() {
        assertTrue(CommonUtil.convert(1, Boolean.class));
        assertTrue(CommonUtil.convert(123, Boolean.class));
        assertFalse(CommonUtil.convert(-1, Boolean.class));
        assertFalse(CommonUtil.convert(0, Boolean.class));

        assertTrue(CommonUtil.convert(1L, Boolean.class));
        assertFalse(CommonUtil.convert(0L, Boolean.class));

        assertTrue(CommonUtil.convert(1.5, Boolean.class));
        assertFalse(CommonUtil.convert(0.0, Boolean.class));
    }

    @Test
    public void testConvertNumberToString() {
        assertEquals("123", CommonUtil.convert(123, String.class));
        assertEquals("456", CommonUtil.convert(456L, String.class));
        assertEquals("78.9", CommonUtil.convert(78.9, String.class));
        assertEquals("true", CommonUtil.convert(true, String.class));
        assertEquals("false", CommonUtil.convert(false, String.class));
    }

    @Test
    public void testConvertCharacterToInteger() {
        assertEquals(65, CommonUtil.convert('A', Integer.class).intValue());
        assertEquals(65, CommonUtil.convert('A', int.class).intValue());
        assertEquals(97, CommonUtil.convert('a', Integer.class).intValue());
        assertEquals(48, CommonUtil.convert('0', Integer.class).intValue());
    }

    @Test
    public void testConvertIntegerToCharacter() {
        assertEquals('A', CommonUtil.convert(65, Character.class).charValue());
        assertEquals('A', CommonUtil.convert(65, char.class).charValue());
        assertEquals('a', CommonUtil.convert(97, Character.class).charValue());
        assertEquals('0', CommonUtil.convert(48, Character.class).charValue());
    }

    @Test
    public void testConvertDateToLong() {
        Date date = new Date(1234567890L);
        assertEquals(1234567890L, CommonUtil.convert(date, Long.class).longValue());
        assertEquals(1234567890L, CommonUtil.convert(date, long.class).longValue());

        java.sql.Timestamp timestamp = new java.sql.Timestamp(1234567890L);
        assertEquals(1234567890L, CommonUtil.convert(timestamp, Long.class).longValue());

        java.sql.Date sqlDate = new java.sql.Date(1234567890L);
        assertEquals(1234567890L, CommonUtil.convert(sqlDate, Long.class).longValue());
    }

    @Test
    public void testConvertLongToDate() {
        Long timeMillis = 1234567890L;

        Date date = CommonUtil.convert(timeMillis, Date.class);
        assertEquals(timeMillis.longValue(), date.getTime());

        java.sql.Timestamp timestamp = CommonUtil.convert(timeMillis, java.sql.Timestamp.class);
        assertEquals(timeMillis.longValue(), timestamp.getTime());

        java.sql.Date sqlDate = CommonUtil.convert(timeMillis, java.sql.Date.class);
        assertEquals(timeMillis.longValue(), sqlDate.getTime());

        java.sql.Time sqlTime = CommonUtil.convert(timeMillis, java.sql.Time.class);
        assertEquals(timeMillis.longValue(), sqlTime.getTime());
    }

    @Test
    public void testConvertCollectionToCollection() {
        List<String> list = Arrays.asList("a", "b", "c", "b");
        Set<String> set = CommonUtil.convert(list, Set.class);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));

        Set<Integer> intSet = new HashSet<>(Arrays.asList(1, 2, 3));
        List<Integer> intList = CommonUtil.convert(intSet, List.class);
        assertEquals(3, intList.size());
        assertTrue(intList.containsAll(intSet));

        List<String> emptyList = new ArrayList<>();
        Set<String> emptySet = CommonUtil.convert(emptyList, Set.class);
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testConvertArrayToCollection() {
        String[] array = { "x", "y", "z" };
        List<String> list = CommonUtil.convert(array, List.class);
        assertEquals(3, list.size());
        assertEquals("x", list.get(0));
        assertEquals("y", list.get(1));
        assertEquals("z", list.get(2));

        Integer[] intArray = { 1, 2, 3 };
        Set<Integer> set = CommonUtil.convert(intArray, Set.class);
        assertEquals(3, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
    }

    @Test
    public void testConvertCollectionToArray() {
        List<String> list = Arrays.asList("a", "b", "c");
        String[] array = CommonUtil.convert(list, String[].class);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);

        Set<Integer> set = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        Integer[] intArray = CommonUtil.convert(set, Integer[].class);
        assertEquals(3, intArray.length);

        List<String> emptyList = new ArrayList<>();
        String[] emptyArray = CommonUtil.convert(emptyList, String[].class);
        assertEquals(0, emptyArray.length);
    }

    @Test
    public void testConvertSingleObjectToCollection() {
        String str = "hello";
        List<String> list = CommonUtil.convert(str, List.class);
        assertEquals(1, list.size());
        assertEquals("hello", list.get(0));

        Integer num = 42;
        Set<Integer> set = CommonUtil.convert(num, Set.class);
        assertEquals(1, set.size());
        assertTrue(set.contains(42));
    }

    @Test
    public void testConvertSingleObjectToArray() {
        String str = "world";
        String[] array = CommonUtil.convert(str, String[].class);
        assertEquals(1, array.length);
        assertEquals("world", array[0]);

        Integer num = 99;
        Integer[] intArray = CommonUtil.convert(num, Integer[].class);
        assertEquals(1, intArray.length);
        assertEquals(99, intArray[0].intValue());
    }

    @Test
    public void testConvertMapToMap() {
        Map<String, Integer> srcMap = new HashMap<>();
        srcMap.put("one", 1);
        srcMap.put("two", 2);
        srcMap.put("three", 3);

        Map<String, Integer> destMap = CommonUtil.convert(srcMap, Map.class);
        assertEquals(3, destMap.size());
        assertEquals(1, destMap.get("one").intValue());
        assertEquals(2, destMap.get("two").intValue());
        assertEquals(3, destMap.get("three").intValue());

        Map<String, String> emptyMap = new HashMap<>();
        Map<String, String> convertedEmpty = CommonUtil.convert(emptyMap, Map.class);
        assertTrue(convertedEmpty.isEmpty());
    }

    @Test
    public void testConvertByteArrayFromBlob() throws SQLException {
        Blob blob = new Blob() {
            private boolean freed = false;

            @Override
            public long length() throws SQLException {
                return 5;
            }

            @Override
            public byte[] getBytes(long pos, int length) throws SQLException {
                if (freed)
                    throw new SQLException("Blob already freed");
                return new byte[] { 1, 2, 3, 4, 5 };
            }

            @Override
            public InputStream getBinaryStream() throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public long position(byte[] pattern, long start) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public long position(Blob pattern, long start) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public int setBytes(long pos, byte[] bytes) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public OutputStream setBinaryStream(long pos) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void truncate(long len) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void free() throws SQLException {
                freed = true;
            }

            @Override
            public InputStream getBinaryStream(long pos, long length) throws SQLException {
                throw new UnsupportedOperationException();
            }
        };

        byte[] result = CommonUtil.convert(blob, byte[].class);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testConvertByteArrayFromInputStream() {
        byte[] data = { 10, 20, 30, 40, 50 };
        InputStream is = new ByteArrayInputStream(data);

        byte[] result = CommonUtil.convert(is, byte[].class);
        assertArrayEquals(data, result);
    }

    @Test
    public void testConvertCharArrayFromReader() {
        String data = "World";
        Reader reader = new StringReader(data);

        char[] result = CommonUtil.convert(reader, char[].class);
        assertArrayEquals(data.toCharArray(), result);
    }

    @Test
    public void testConvertCharArrayFromInputStream() {
        String data = "['T','e','s','t']";
        InputStream is = new ByteArrayInputStream(data.getBytes());

        char[] result = CommonUtil.convert(is, char[].class);
        assertEquals("Test", String.valueOf(result));
    }

    @Test
    public void testConvertStringFromCharSequence() {
        StringBuilder sb = new StringBuilder("Hello");
        assertEquals("Hello", CommonUtil.convert(sb, String.class));

        StringBuffer sbuf = new StringBuffer("World");
        assertEquals("World", CommonUtil.convert(sbuf, String.class));

        CharSequence cs = "Test";
        assertEquals("Test", CommonUtil.convert(cs, String.class));
    }

    @Test
    public void testConvertStringFromReader() {
        String data = "Reader Content";
        Reader reader = new StringReader(data);

        String result = CommonUtil.convert(reader, String.class);
        assertEquals(data, result);
    }

    @Test
    public void testConvertStringFromInputStream() {
        String data = "InputStream Content";
        InputStream is = new ByteArrayInputStream(data.getBytes());

        String result = CommonUtil.convert(is, String.class);
        assertEquals(data, result);
    }

    @Test
    public void testConvertInputStreamFromByteArray() {
        byte[] data = { 1, 2, 3, 4, 5 };
        InputStream is = CommonUtil.convert(data, InputStream.class);

        assertNotNull(is);
        assertTrue(is instanceof ByteArrayInputStream);

        byte[] readData = new byte[5];
        try {
            is.read(readData);
            assertArrayEquals(data, readData);
        } catch (Exception e) {
            fail("Should not throw exception");
        }
    }

    @Test
    public void testConvertReaderFromCharSequence() {
        String data = "Test String";
        Reader reader = CommonUtil.convert(data, Reader.class);

        assertNotNull(reader);
        assertTrue(reader instanceof StringReader);

        char[] buffer = new char[data.length()];
        try {
            reader.read(buffer);
            assertArrayEquals(data.toCharArray(), buffer);
        } catch (Exception e) {
            fail("Should not throw exception");
        }
    }

    @Test
    public void testConvertSameType() {
        String str = "test";
        assertSame(str, CommonUtil.convert(str, String.class));

        Integer num = 42;
        assertSame(num, CommonUtil.convert(num, Integer.class));

        List<String> list = new ArrayList<>();
        assertSame(list, CommonUtil.convert(list, ArrayList.class));
    }

    @Test
    public void testConvertPrimitiveTypes() {
        assertEquals(123, CommonUtil.convert(123, int.class).intValue());
        assertEquals(456L, CommonUtil.convert(456L, long.class).longValue());
        assertEquals(78.9f, CommonUtil.convert(78.9f, float.class).floatValue(), 0.001);
        assertEquals(12.34d, CommonUtil.convert(12.34d, double.class).doubleValue(), 0.001);
        assertTrue(CommonUtil.convert(true, boolean.class));
        assertFalse(CommonUtil.convert(false, boolean.class));
        assertEquals('A', CommonUtil.convert('A', char.class).charValue());
        assertEquals((byte) 99, CommonUtil.convert((byte) 99, byte.class).byteValue());
        assertEquals((short) 999, CommonUtil.convert((short) 999, short.class).shortValue());
    }

    @Test
    public void testConvertNullToType() {
        Type<String> stringType = TypeFactory.getType(String.class);
        Type<Integer> intType = TypeFactory.getType(Integer.class);
        Type<List> listType = TypeFactory.getType(List.class);

        assertNull(CommonUtil.convert(null, stringType));
        assertNull(CommonUtil.convert(null, intType));
        assertNull(CommonUtil.convert(null, listType));
    }

    @Test
    public void testConvertUsingType() {
        Type<String> stringType = TypeFactory.getType(String.class);
        Type<Integer> intType = TypeFactory.getType(Integer.class);
        Type<Boolean> boolType = TypeFactory.getType(Boolean.class);

        assertEquals(123, CommonUtil.convert("123", intType).intValue());

        assertEquals("456", CommonUtil.convert(456, stringType));

        assertTrue(CommonUtil.convert(1, boolType));
        assertFalse(CommonUtil.convert(0, boolType));
    }

    @Test
    public void testConvertCollectionWithType() {
        Type<List> listType = TypeFactory.getType(List.class);
        Type<Set> setType = TypeFactory.getType(Set.class);

        List<String> list = Arrays.asList("a", "b", "c");
        Set<String> set = CommonUtil.convert(list, setType);
        assertEquals(3, set.size());

        Set<Integer> intSet = new HashSet<>(Arrays.asList(1, 2, 3));
        List<Integer> intList = CommonUtil.convert(intSet, listType);
        assertEquals(3, intList.size());
    }

    @Test
    public void testConvertWithParameterizedType() {
        Type<List> listType = TypeFactory.getType(List.class);
        Type<Map> mapType = TypeFactory.getType(Map.class);

        String[] array = { "x", "y", "z" };
        List<String> list = CommonUtil.convert(array, listType);
        assertEquals(3, list.size());

        Map<String, Integer> srcMap = new HashMap<>();
        srcMap.put("one", 1);
        srcMap.put("two", 2);

        Map<String, Integer> destMap = CommonUtil.convert(srcMap, mapType);
        assertEquals(2, destMap.size());
    }

    @Test
    public void testConvertAutoCloseableResources() {
        String data = "AutoCloseable test";

        class TestAutoCloseable implements AutoCloseable {
            boolean closed = false;
            String value = data;

            @Override
            public void close() {
                closed = true;
            }

            @Override
            public String toString() {
                return value;
            }
        }

        TestAutoCloseable resource = new TestAutoCloseable();
        String result = CommonUtil.convert(resource, String.class);

        assertEquals(data, result);
        assertFalse(resource.closed);
    }

    @Test
    public void testConvertWithSQLException() {
        Blob blob = new Blob() {
            @Override
            public long length() throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public byte[] getBytes(long pos, int length) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public InputStream getBinaryStream() throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public long position(byte[] pattern, long start) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public long position(Blob pattern, long start) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public int setBytes(long pos, byte[] bytes) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public OutputStream setBinaryStream(long pos) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public void truncate(long len) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public void free() throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public InputStream getBinaryStream(long pos, long length) throws SQLException {
                throw new SQLException("Test exception");
            }
        };

        assertThrows(UncheckedSQLException.class, () -> CommonUtil.convert(blob, byte[].class));
    }

    @Test
    public void testConvertEdgeCases() {

        String bigNum = "9223372036854775807";
        assertEquals(Long.MAX_VALUE, CommonUtil.convert(bigNum, Long.class).longValue());

        assertThrows(ArithmeticException.class, () -> CommonUtil.convert(Long.MAX_VALUE, Integer.class).intValue());
        assertThrows(ArithmeticException.class, () -> CommonUtil.convert(1000, Byte.class).byteValue());

        assertEquals((Float) Float.POSITIVE_INFINITY, CommonUtil.convert("Infinity", Float.class));
        assertEquals((Double) Double.NEGATIVE_INFINITY, CommonUtil.convert("-Infinity", Double.class));
        assertTrue(Double.isNaN(CommonUtil.convert("NaN", Double.class).doubleValue()));
    }

    @Test
    public void testConvertUnsupportedConversions() {

        Map<String, String> map = new HashMap<>();
        map.put("key", "value");

        assertThrows(RuntimeException.class, () -> CommonUtil.convert(map, Integer.class));
        assertFalse(CommonUtil.convert(map, Boolean.class));

        List<Map<String, Object>> complexList = new ArrayList<>();
        complexList.add(new HashMap<>());

        List<Map<String, Object>> convertedList = CommonUtil.convert(complexList, List.class);
        assertNotNull(convertedList);
        assertEquals(1, convertedList.size());
    }

    @Test
    public void testBinarySearch_shortArray() {
        short[] arr = { 1, 3, 5, 7, 9 };
        Assertions.assertEquals(2, CommonUtil.binarySearch(arr, (short) 5));
        Assertions.assertEquals(-1, CommonUtil.binarySearch(arr, (short) 0));
        Assertions.assertEquals(-6, CommonUtil.binarySearch(arr, (short) 10));

        short[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, (short) 5));

        Assertions.assertEquals(-1, CommonUtil.binarySearch((short[]) null, (short) 5));
    }

    @Test
    public void testBinarySearch_shortArray_withRange() {
        short[] arr = { 1, 3, 5, 7, 9, 11, 13 };
        Assertions.assertEquals(3, CommonUtil.binarySearch(arr, 1, 5, (short) 7));
        Assertions.assertEquals(-2, CommonUtil.binarySearch(arr, 1, 5, (short) 2));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.binarySearch(arr, 5, 2, (short) 5));

        short[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 0, 0, (short) 5));
    }

    @Test
    public void testBinarySearch_intArray() {
        int[] arr = { 1, 3, 5, 7, 9 };
        Assertions.assertEquals(2, CommonUtil.binarySearch(arr, 5));
        Assertions.assertEquals(-1, CommonUtil.binarySearch(arr, 0));
        Assertions.assertEquals(-6, CommonUtil.binarySearch(arr, 10));

        int[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 5));

        Assertions.assertEquals(-1, CommonUtil.binarySearch((int[]) null, 5));
    }

    @Test
    public void testBinarySearch_intArray_withRange() {
        int[] arr = { 1, 3, 5, 7, 9, 11, 13 };
        Assertions.assertEquals(3, CommonUtil.binarySearch(arr, 1, 5, 7));
        Assertions.assertEquals(-2, CommonUtil.binarySearch(arr, 1, 5, 2));

        int[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 0, 0, 5));
    }

    @Test
    public void testBinarySearch_longArray() {
        long[] arr = { 1L, 3L, 5L, 7L, 9L };
        Assertions.assertEquals(2, CommonUtil.binarySearch(arr, 5L));
        Assertions.assertEquals(-1, CommonUtil.binarySearch(arr, 0L));
        Assertions.assertEquals(-6, CommonUtil.binarySearch(arr, 10L));

        long[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 5L));

        Assertions.assertEquals(-1, CommonUtil.binarySearch((long[]) null, 5L));
    }

    @Test
    public void testBinarySearch_longArray_withRange() {
        long[] arr = { 1L, 3L, 5L, 7L, 9L, 11L, 13L };
        Assertions.assertEquals(3, CommonUtil.binarySearch(arr, 1, 5, 7L));
        Assertions.assertEquals(-2, CommonUtil.binarySearch(arr, 1, 5, 2L));

        long[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 0, 0, 5L));
    }

    @Test
    public void testBinarySearch_floatArray() {
        float[] arr = { 1.0f, 3.0f, 5.0f, 7.0f, 9.0f };
        Assertions.assertEquals(2, CommonUtil.binarySearch(arr, 5.0f));
        Assertions.assertEquals(-1, CommonUtil.binarySearch(arr, 0.0f));
        Assertions.assertEquals(-6, CommonUtil.binarySearch(arr, 10.0f));

        float[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 5.0f));

        Assertions.assertEquals(-1, CommonUtil.binarySearch((float[]) null, 5.0f));
    }

    @Test
    public void testBinarySearch_floatArray_withRange() {
        float[] arr = { 1.0f, 3.0f, 5.0f, 7.0f, 9.0f, 11.0f, 13.0f };
        Assertions.assertEquals(3, CommonUtil.binarySearch(arr, 1, 5, 7.0f));
        Assertions.assertEquals(-2, CommonUtil.binarySearch(arr, 1, 5, 2.0f));

        float[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 0, 0, 5.0f));
    }

    @Test
    public void testBinarySearch_doubleArray() {
        double[] arr = { 1.0, 3.0, 5.0, 7.0, 9.0 };
        Assertions.assertEquals(2, CommonUtil.binarySearch(arr, 5.0));
        Assertions.assertEquals(-1, CommonUtil.binarySearch(arr, 0.0));
        Assertions.assertEquals(-6, CommonUtil.binarySearch(arr, 10.0));

        double[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 5.0));

        Assertions.assertEquals(-1, CommonUtil.binarySearch((double[]) null, 5.0));
    }

    @Test
    public void testBinarySearch_doubleArray_withRange() {
        double[] arr = { 1.0, 3.0, 5.0, 7.0, 9.0, 11.0, 13.0 };
        Assertions.assertEquals(3, CommonUtil.binarySearch(arr, 1, 5, 7.0));
        Assertions.assertEquals(-2, CommonUtil.binarySearch(arr, 1, 5, 2.0));

        double[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 0, 0, 5.0));
    }

    @Test
    public void testBinarySearch_objectArray() {
        String[] arr = { "a", "c", "e", "g", "i" };
        Assertions.assertEquals(2, CommonUtil.binarySearch(arr, "e"));
        Assertions.assertEquals(-1, CommonUtil.binarySearch(arr, ""));
        Assertions.assertEquals(-6, CommonUtil.binarySearch(arr, "z"));

        String[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, "e"));

        Assertions.assertEquals(-1, CommonUtil.binarySearch((String[]) null, "e"));
    }

    @Test
    public void testBinarySearch_objectArray_withRange() {
        String[] arr = { "a", "c", "e", "g", "i", "k", "m" };
        Assertions.assertEquals(3, CommonUtil.binarySearch(arr, 1, 5, "g"));
        Assertions.assertEquals(-2, CommonUtil.binarySearch(arr, 1, 5, "b"));

        String[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 0, 0, "e"));
    }

    @Test
    public void testBinarySearch_genericArray_withComparator() {
        String[] arr = { "a", "c", "e", "g", "i" };
        Comparator<String> cmp = String::compareTo;
        Assertions.assertEquals(2, CommonUtil.binarySearch(arr, "e", cmp));
        Assertions.assertEquals(-1, CommonUtil.binarySearch(arr, "", cmp));

        String[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, "e", cmp));

        Assertions.assertEquals(-1, CommonUtil.binarySearch((String[]) null, "e", cmp));
    }

    @Test
    public void testBinarySearch_genericArray_withRangeAndComparator() {
        String[] arr = { "a", "c", "e", "g", "i", "k", "m" };
        Comparator<String> cmp = String::compareTo;
        Assertions.assertEquals(3, CommonUtil.binarySearch(arr, 1, 5, "g", cmp));
        Assertions.assertEquals(-2, CommonUtil.binarySearch(arr, 1, 5, "b", cmp));

        String[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 0, 0, "e", cmp));
    }

    @Test
    public void testBinarySearch_list() {
        List<Integer> list = Arrays.asList(1, 3, 5, 7, 9);
        Assertions.assertEquals(2, CommonUtil.binarySearch(list, 5));
        Assertions.assertEquals(-1, CommonUtil.binarySearch(list, 0));
        Assertions.assertEquals(-6, CommonUtil.binarySearch(list, 10));

        List<Integer> empty = new ArrayList<>();
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 5));

        Assertions.assertEquals(-1, CommonUtil.binarySearch((List<Integer>) null, 5));
    }

    @Test
    public void testBinarySearch_list_withRange() {
        List<Integer> list = Arrays.asList(1, 3, 5, 7, 9, 11, 13);
        Assertions.assertEquals(3, CommonUtil.binarySearch(list, 1, 5, 7));
        Assertions.assertEquals(-2, CommonUtil.binarySearch(list, 1, 5, 2));

        List<Integer> empty = new ArrayList<>();
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 0, 0, 5));
    }

    @Test
    public void testBinarySearch_list_withComparator() {
        List<String> list = Arrays.asList("a", "c", "e", "g", "i");
        Comparator<String> cmp = String::compareTo;
        Assertions.assertEquals(2, CommonUtil.binarySearch(list, "e", cmp));
        Assertions.assertEquals(-1, CommonUtil.binarySearch(list, "", cmp));

        List<String> empty = new ArrayList<>();
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, "e", cmp));

        Assertions.assertEquals(-1, CommonUtil.binarySearch((List<String>) null, "e", cmp));
    }

    @Test
    public void testBinarySearch_list_withRangeAndComparator() {
        List<String> list = Arrays.asList("a", "c", "e", "g", "i", "k", "m");
        Comparator<String> cmp = String::compareTo;
        Assertions.assertEquals(3, CommonUtil.binarySearch(list, 1, 5, "g", cmp));
        Assertions.assertEquals(-2, CommonUtil.binarySearch(list, 1, 5, "b", cmp));

        List<String> empty = new ArrayList<>();
        Assertions.assertEquals(-1, CommonUtil.binarySearch(empty, 0, 0, "e", cmp));
    }

    @Test
    public void testIndexOf_booleanArray() {
        boolean[] arr = { true, false, true, false, true };
        Assertions.assertEquals(0, CommonUtil.indexOf(arr, true));
        Assertions.assertEquals(1, CommonUtil.indexOf(arr, false));

        boolean[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, true));

        Assertions.assertEquals(-1, CommonUtil.indexOf((boolean[]) null, true));
    }

    @Test
    public void testIndexOf_booleanArray_withFromIndex() {
        boolean[] arr = { true, false, true, false, true };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, true, 1));
        Assertions.assertEquals(3, CommonUtil.indexOf(arr, false, 2));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, true, 10));

        Assertions.assertEquals(0, CommonUtil.indexOf(arr, true, -1));
    }

    @Test
    public void testIndexOf_charArray() {
        char[] arr = { 'a', 'b', 'c', 'd', 'e' };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, 'c'));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 'z'));

        char[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, 'c'));

        Assertions.assertEquals(-1, CommonUtil.indexOf((char[]) null, 'c'));
    }

    @Test
    public void testIndexOf_charArray_withFromIndex() {
        char[] arr = { 'a', 'b', 'c', 'd', 'c' };
        Assertions.assertEquals(4, CommonUtil.indexOf(arr, 'c', 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 'c', 10));
    }

    @Test
    public void testIndexOf_byteArray() {
        byte[] arr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, (byte) 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, (byte) 10));

        byte[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, (byte) 3));

        Assertions.assertEquals(-1, CommonUtil.indexOf((byte[]) null, (byte) 3));
    }

    @Test
    public void testIndexOf_byteArray_withFromIndex() {
        byte[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(4, CommonUtil.indexOf(arr, (byte) 3, 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, (byte) 3, 10));
    }

    @Test
    public void testIndexOf_shortArray() {
        short[] arr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, (short) 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, (short) 10));

        short[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, (short) 3));

        Assertions.assertEquals(-1, CommonUtil.indexOf((short[]) null, (short) 3));
    }

    @Test
    public void testIndexOf_shortArray_withFromIndex() {
        short[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(4, CommonUtil.indexOf(arr, (short) 3, 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, (short) 3, 10));
    }

    @Test
    public void testIndexOf_intArray() {
        int[] arr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 10));

        int[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, 3));

        Assertions.assertEquals(-1, CommonUtil.indexOf((int[]) null, 3));
    }

    @Test
    public void testIndexOf_intArray_withFromIndex() {
        int[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(4, CommonUtil.indexOf(arr, 3, 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 3, 10));
    }

    @Test
    public void testIndexOf_longArray() {
        long[] arr = { 1L, 2L, 3L, 4L, 5L };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, 3L));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 10L));

        long[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, 3L));

        Assertions.assertEquals(-1, CommonUtil.indexOf((long[]) null, 3L));
    }

    @Test
    public void testIndexOf_longArray_withFromIndex() {
        long[] arr = { 1L, 2L, 3L, 4L, 3L };
        Assertions.assertEquals(4, CommonUtil.indexOf(arr, 3L, 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 3L, 10));
    }

    @Test
    public void testIndexOf_floatArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, 3.0f));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 10.0f));

        float[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, 3.0f));

        Assertions.assertEquals(-1, CommonUtil.indexOf((float[]) null, 3.0f));
    }

    @Test
    public void testIndexOf_floatArray_withFromIndex() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 3.0f };
        Assertions.assertEquals(4, CommonUtil.indexOf(arr, 3.0f, 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 3.0f, 10));
    }

    @Test
    public void testIndexOf_doubleArray() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, 3.0));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 10.0));

        double[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, 3.0));

        Assertions.assertEquals(-1, CommonUtil.indexOf((double[]) null, 3.0));
    }

    @Test
    public void testIndexOf_doubleArray_withFromIndex() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 3.0 };
        Assertions.assertEquals(4, CommonUtil.indexOf(arr, 3.0, 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 3.0, 10));
    }

    @Test
    public void testIndexOf_doubleArray_withTolerance() {
        double[] arr = { 1.0, 2.0, 3.001, 4.0, 5.0 };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, 3.0, 0.01));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 3.0, 0.0001));

        double[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, 3.0, 0.01));

        Assertions.assertEquals(-1, CommonUtil.indexOf((double[]) null, 3.0, 0.01));
    }

    @Test
    public void testIndexOf_doubleArray_withToleranceAndFromIndex() {
        double[] arr = { 1.0, 2.0, 3.001, 4.0, 3.002 };
        Assertions.assertEquals(4, CommonUtil.indexOf(arr, 3.0, 0.01, 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, 3.0, 0.0001, 3));
    }

    @Test
    public void testIndexOf_objectArray() {
        String[] arr = { "a", "b", "c", "d", "e" };
        Assertions.assertEquals(2, CommonUtil.indexOf(arr, "c"));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, "z"));

        String[] arrWithNull = { "a", null, "c" };
        Assertions.assertEquals(1, CommonUtil.indexOf(arrWithNull, null));

        String[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, "c"));

        Assertions.assertEquals(-1, CommonUtil.indexOf((String[]) null, "c"));
    }

    @Test
    public void testIndexOf_objectArray_withFromIndex() {
        String[] arr = { "a", "b", "c", "d", "c" };
        Assertions.assertEquals(4, CommonUtil.indexOf(arr, "c", 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, "c", 10));
    }

    @Test
    public void testIndexOf_collection() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Assertions.assertEquals(2, CommonUtil.indexOf(list, "c"));
        Assertions.assertEquals(-1, CommonUtil.indexOf(list, "z"));

        List<String> empty = new ArrayList<>();
        Assertions.assertEquals(-1, CommonUtil.indexOf(empty, "c"));

        Assertions.assertEquals(-1, CommonUtil.indexOf((Collection<?>) null, "c"));
    }

    @Test
    public void testIndexOf_collection_withFromIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "c");
        Assertions.assertEquals(4, CommonUtil.indexOf(list, "c", 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(list, "c", 10));

        LinkedList<String> linkedList = new LinkedList<>(list);
        Assertions.assertEquals(4, CommonUtil.indexOf(linkedList, "c", 3));
    }

    @Test
    public void testIndexOf_iterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Assertions.assertEquals(2, CommonUtil.indexOf(list.iterator(), "c"));
        Assertions.assertEquals(-1, CommonUtil.indexOf(list.iterator(), "z"));

        Assertions.assertEquals(-1, CommonUtil.indexOf((Iterator<?>) null, "c"));
    }

    @Test
    public void testIndexOf_iterator_withFromIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "c");
        Assertions.assertEquals(4, CommonUtil.indexOf(list.iterator(), "c", 3));
        Assertions.assertEquals(-1, CommonUtil.indexOf(list.iterator(), "c", 10));
    }

    @Test
    public void testIndexOfSubList_withFromIndex() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d");
        List<String> sub = Arrays.asList("c", "d");
        Assertions.assertEquals(4, CommonUtil.indexOfSubList(source, sub, 3));
        Assertions.assertEquals(-1, CommonUtil.indexOfSubList(source, sub, 10));
    }

    @Test
    public void testIndexOfIgnoreCase_withFromIndex() {
        String[] arr = { "A", "B", "C", "D", "c" };
        Assertions.assertEquals(4, CommonUtil.indexOfIgnoreCase(arr, "C", 3));
        Assertions.assertEquals(-1, CommonUtil.indexOfIgnoreCase(arr, "C", 10));
    }

    @Test
    public void testLastIndexOf_booleanArray() {
        boolean[] arr = { true, false, true, false, true };
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(arr, true));
        Assertions.assertEquals(3, CommonUtil.lastIndexOf(arr, false));

        boolean[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, true));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((boolean[]) null, true));
    }

    @Test
    public void testLastIndexOf_booleanArray_withStartIndex() {
        boolean[] arr = { true, false, true, false, true };
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(arr, true, 3));
        Assertions.assertEquals(1, CommonUtil.lastIndexOf(arr, false, 2));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, true, -1));
    }

    @Test
    public void testLastIndexOf_charArray() {
        char[] arr = { 'a', 'b', 'c', 'd', 'c' };
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(arr, 'c'));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 'z'));

        char[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, 'c'));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((char[]) null, 'c'));
    }

    @Test
    public void testLastIndexOf_charArray_withStartIndex() {
        char[] arr = { 'a', 'b', 'c', 'd', 'c' };
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(arr, 'c', 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 'c', -1));
    }

    @Test
    public void testLastIndexOf_byteArray() {
        byte[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(arr, (byte) 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, (byte) 10));

        byte[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, (byte) 3));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((byte[]) null, (byte) 3));
    }

    @Test
    public void testLastIndexOf_byteArray_withStartIndex() {
        byte[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(arr, (byte) 3, 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, (byte) 3, -1));
    }

    @Test
    public void testLastIndexOf_shortArray() {
        short[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(arr, (short) 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, (short) 10));

        short[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, (short) 3));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((short[]) null, (short) 3));
    }

    @Test
    public void testLastIndexOf_shortArray_withStartIndex() {
        short[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(arr, (short) 3, 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, (short) 3, -1));
    }

    @Test
    public void testLastIndexOf_intArray() {
        int[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(arr, 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 10));

        int[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, 3));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((int[]) null, 3));
    }

    @Test
    public void testLastIndexOf_intArray_withStartIndex() {
        int[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(arr, 3, 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 3, -1));
    }

    @Test
    public void testLastIndexOf_longArray() {
        long[] arr = { 1L, 2L, 3L, 4L, 3L };
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(arr, 3L));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 10L));

        long[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, 3L));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((long[]) null, 3L));
    }

    @Test
    public void testLastIndexOf_longArray_withStartIndex() {
        long[] arr = { 1L, 2L, 3L, 4L, 3L };
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(arr, 3L, 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 3L, -1));
    }

    @Test
    public void testLastIndexOf_floatArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 3.0f };
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(arr, 3.0f));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 10.0f));

        float[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, 3.0f));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((float[]) null, 3.0f));
    }

    @Test
    public void testLastIndexOf_floatArray_withStartIndex() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 3.0f };
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(arr, 3.0f, 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 3.0f, -1));
    }

    @Test
    public void testLastIndexOf_doubleArray() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 3.0 };
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(arr, 3.0));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 10.0));

        double[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, 3.0));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((double[]) null, 3.0));
    }

    @Test
    public void testLastIndexOf_doubleArray_withStartIndex() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 3.0 };
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(arr, 3.0, 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 3.0, -1));
    }

    @Test
    public void testLastIndexOf_doubleArray_withTolerance() {
        double[] arr = { 1.0, 2.0, 3.001, 4.0, 3.002 };
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(arr, 3.0, 0.01));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 3.0, 0.0001));

        double[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, 3.0, 0.01));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((double[]) null, 3.0, 0.01));
    }

    @Test
    public void testLastIndexOf_doubleArray_withToleranceAndStartIndex() {
        double[] arr = { 1.0, 2.0, 3.001, 4.0, 3.002 };
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(arr, 3.0, 0.01, 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, 3.0, 0.0001, 3));
    }

    @Test
    public void testLastIndexOf_objectArray() {
        String[] arr = { "a", "b", "c", "d", "c" };
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(arr, "c"));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, "z"));

        String[] arrWithNull = { "a", null, "c", null };
        Assertions.assertEquals(3, CommonUtil.lastIndexOf(arrWithNull, null));

        String[] empty = {};
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, "c"));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((String[]) null, "c"));
    }

    @Test
    public void testLastIndexOf_objectArray_withStartIndex() {
        String[] arr = { "a", "b", "c", "d", "c" };
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(arr, "c", 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, "c", -1));
    }

    @Test
    public void testLastIndexOf_collection() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "c");
        Assertions.assertEquals(4, CommonUtil.lastIndexOf(list, "c"));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(list, "z"));

        List<String> empty = new ArrayList<>();
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(empty, "c"));

        Assertions.assertEquals(-1, CommonUtil.lastIndexOf((Collection<?>) null, "c"));
    }

    @Test
    public void testLastIndexOf_collection_withStartIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "c");
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(list, "c", 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(list, "c", -1));

        LinkedList<String> linkedList = new LinkedList<>(list);
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(linkedList, "c", 3));
    }

    @Test
    public void testLastIndexOfSubList_withStartIndex() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d");
        List<String> sub = Arrays.asList("c", "d");
        Assertions.assertEquals(2, CommonUtil.lastIndexOfSubList(source, sub, 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOfSubList(source, sub, 1));
    }

    @Test
    public void testLastIndexOfIgnoreCase_withStartIndex() {
        String[] arr = { "A", "B", "C", "D", "c" };
        Assertions.assertEquals(2, CommonUtil.lastIndexOfIgnoreCase(arr, "C", 3));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOfIgnoreCase(arr, "C", -1));
    }

    @Test
    public void testFindFirstIndex_array() {
        String[] arr = { "a", "b", "c", "d", "e" };
        OptionalInt result = CommonUtil.findFirstIndex(arr, s -> s.equals("c"));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.getAsInt());

        OptionalInt notFound = CommonUtil.findFirstIndex(arr, s -> s.equals("z"));
        Assertions.assertFalse(notFound.isPresent());

        String[] empty = {};
        OptionalInt emptyResult = CommonUtil.findFirstIndex(empty, s -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testFindFirstIndex_array_withBiPredicate() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String prefix = "c";
        OptionalInt result = CommonUtil.findFirstIndex(arr, prefix, (s, p) -> s.startsWith(p));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.getAsInt());

        String[] empty = {};
        OptionalInt emptyResult = CommonUtil.findFirstIndex(empty, prefix, (s, p) -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testFindFirstIndex_collection() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        OptionalInt result = CommonUtil.findFirstIndex(list, s -> s.equals("c"));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.getAsInt());

        OptionalInt notFound = CommonUtil.findFirstIndex(list, s -> s.equals("z"));
        Assertions.assertFalse(notFound.isPresent());

        List<String> empty = new ArrayList<>();
        OptionalInt emptyResult = CommonUtil.findFirstIndex(empty, s -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testFindFirstIndex_collection_withBiPredicate() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        String prefix = "c";
        OptionalInt result = CommonUtil.findFirstIndex(list, prefix, (s, p) -> s.startsWith(p));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.getAsInt());

        List<String> empty = new ArrayList<>();
        OptionalInt emptyResult = CommonUtil.findFirstIndex(empty, prefix, (s, p) -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testFindLastIndex_array() {
        String[] arr = { "a", "b", "c", "d", "c" };
        OptionalInt result = CommonUtil.findLastIndex(arr, s -> s.equals("c"));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.getAsInt());

        OptionalInt notFound = CommonUtil.findLastIndex(arr, s -> s.equals("z"));
        Assertions.assertFalse(notFound.isPresent());

        String[] empty = {};
        OptionalInt emptyResult = CommonUtil.findLastIndex(empty, s -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testFindLastIndex_array_withBiPredicate() {
        String[] arr = { "a", "b", "c", "d", "ca" };
        String prefix = "c";
        OptionalInt result = CommonUtil.findLastIndex(arr, prefix, (s, p) -> s.startsWith(p));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.getAsInt());

        String[] empty = {};
        OptionalInt emptyResult = CommonUtil.findLastIndex(empty, prefix, (s, p) -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testFindLastIndex_collection() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "c");
        OptionalInt result = CommonUtil.findLastIndex(list, s -> s.equals("c"));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.getAsInt());

        OptionalInt notFound = CommonUtil.findLastIndex(list, s -> s.equals("z"));
        Assertions.assertFalse(notFound.isPresent());

        List<String> empty = new ArrayList<>();
        OptionalInt emptyResult = CommonUtil.findLastIndex(empty, s -> true);
        Assertions.assertFalse(emptyResult.isPresent());

        LinkedList<String> linkedList = new LinkedList<>(list);
        OptionalInt linkedResult = CommonUtil.findLastIndex(linkedList, s -> s.equals("c"));
        Assertions.assertTrue(linkedResult.isPresent());
        Assertions.assertEquals(4, linkedResult.getAsInt());
    }

    @Test
    public void testFindLastIndex_collection_withBiPredicate() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "ca");
        String prefix = "c";
        OptionalInt result = CommonUtil.findLastIndex(list, prefix, (s, p) -> s.startsWith(p));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.getAsInt());

        List<String> empty = new ArrayList<>();
        OptionalInt emptyResult = CommonUtil.findLastIndex(empty, prefix, (s, p) -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testIndicesOfAllMin_array() {
        Integer[] arr = { 3, 1, 4, 1, 5, 1 };
        int[] indices = CommonUtil.indicesOfMin(arr);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        Integer[] single = { 5 };
        int[] singleIndices = CommonUtil.indicesOfMin(single);
        Assertions.assertArrayEquals(new int[] { 0 }, singleIndices);

        Integer[] empty = {};
        int[] emptyIndices = CommonUtil.indicesOfMin(empty);
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);
    }

    @Test
    public void testIndicesOfAllMin_array_withComparator() {
        String[] arr = { "cat", "a", "dog", "a", "bird", "a" };
        Comparator<String> cmp = Comparator.comparing(String::length);
        int[] indices = CommonUtil.indicesOfMin(arr, cmp);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        String[] arrWithNull = { null, "a", "b", null };
        int[] nullIndices = CommonUtil.indicesOfMin(arrWithNull, null);
        Assertions.assertArrayEquals(new int[] { 1 }, nullIndices);
    }

    @Test
    public void testIndicesOfAllMin_collection() {
        List<Integer> list = Arrays.asList(3, 1, 4, 1, 5, 1);
        int[] indices = CommonUtil.indicesOfMin(list);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        List<Integer> empty = new ArrayList<>();
        int[] emptyIndices = CommonUtil.indicesOfMin(empty);
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);
    }

    @Test
    public void testIndicesOfAllMin_collection_withComparator() {
        List<String> list = Arrays.asList("cat", "a", "dog", "a", "bird", "a");
        Comparator<String> cmp = Comparator.comparing(String::length);
        int[] indices = CommonUtil.indicesOfMin(list, cmp);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);
    }

    @Test
    public void testIndicesOfAllMax_array() {
        Integer[] arr = { 3, 5, 4, 5, 1, 5 };
        int[] indices = CommonUtil.indicesOfMax(arr);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        Integer[] single = { 5 };
        int[] singleIndices = CommonUtil.indicesOfMax(single);
        Assertions.assertArrayEquals(new int[] { 0 }, singleIndices);

        Integer[] empty = {};
        int[] emptyIndices = CommonUtil.indicesOfMax(empty);
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);
    }

    @Test
    public void testIndicesOfAllMax_array_withComparator() {
        String[] arr = { "a", "cat", "b", "dog", "c", "dog" };
        Comparator<String> cmp = Comparator.comparing(String::length);
        int[] indices = CommonUtil.indicesOfMax(arr, cmp);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        String[] arrWithNull = { "a", null, "b", null };
        int[] nullIndices = CommonUtil.indicesOfMax(arrWithNull, null);
        Assertions.assertArrayEquals(new int[] { 2 }, nullIndices);
    }

    @Test
    public void testIndicesOfAllMax_collection() {
        List<Integer> list = Arrays.asList(3, 5, 4, 5, 1, 5);
        int[] indices = CommonUtil.indicesOfMax(list);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        List<Integer> empty = new ArrayList<>();
        int[] emptyIndices = CommonUtil.indicesOfMax(empty);
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);
    }

    @Test
    public void testIndicesOfAllMax_collection_withComparator() {
        List<String> list = Arrays.asList("a", "cat", "b", "dog", "c", "dog");
        Comparator<String> cmp = Comparator.comparing(String::length);
        int[] indices = CommonUtil.indicesOfMax(list, cmp);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);
    }

    @Test
    public void testIndicesOfAll_objectArray() {
        String[] arr = { "a", "b", "c", "b", "d", "b" };
        int[] indices = CommonUtil.indicesOfAll(arr, "b");
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        int[] notFound = CommonUtil.indicesOfAll(arr, "z");
        Assertions.assertArrayEquals(new int[] {}, notFound);

        String[] arrWithNull = { "a", null, "b", null };
        int[] nullIndices = CommonUtil.indicesOfAll(arrWithNull, (String) null);
        Assertions.assertArrayEquals(new int[] { 1, 3 }, nullIndices);

        String[] empty = {};
        int[] emptyIndices = CommonUtil.indicesOfAll(empty, "a");
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);
    }

    @Test
    public void testIndicesOfAll_objectArray_withStartIndex() {
        String[] arr = { "a", "b", "c", "b", "d", "b" };
        int[] indices = CommonUtil.indicesOfAll(arr, "b", 2);
        Assertions.assertArrayEquals(new int[] { 3, 5 }, indices);

        int[] beyondIndices = CommonUtil.indicesOfAll(arr, "b", 10);
        Assertions.assertArrayEquals(new int[] {}, beyondIndices);

        int[] negativeIndices = CommonUtil.indicesOfAll(arr, "b", -1);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, negativeIndices);
    }

    @Test
    public void testIndicesOfAll_collection() {
        List<String> list = Arrays.asList("a", "b", "c", "b", "d", "b");
        int[] indices = CommonUtil.indicesOfAll(list, "b");
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        List<String> empty = new ArrayList<>();
        int[] emptyIndices = CommonUtil.indicesOfAll(empty, "a");
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);

        LinkedList<String> linkedList = new LinkedList<>(list);
        int[] linkedIndices = CommonUtil.indicesOfAll(linkedList, "b");
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, linkedIndices);
    }

    @Test
    public void testIndicesOfAll_collection_withStartIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "b", "d", "b");
        int[] indices = CommonUtil.indicesOfAll(list, "b", 2);
        Assertions.assertArrayEquals(new int[] { 3, 5 }, indices);

        int[] beyondIndices = CommonUtil.indicesOfAll(list, "b", 10);
        Assertions.assertArrayEquals(new int[] {}, beyondIndices);
    }

    @Test
    public void testIndicesOfAll_array_withPredicate() {
        String[] arr = { "apple", "banana", "apricot", "cherry", "avocado" };
        int[] indices = CommonUtil.indicesOfAll(arr, s -> s.startsWith("a"));
        Assertions.assertArrayEquals(new int[] { 0, 2, 4 }, indices);

        String[] empty = {};
        int[] emptyIndices = CommonUtil.indicesOfAll(empty, s -> true);
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);
    }

    @Test
    public void testIndicesOfAll_array_withPredicateAndStartIndex() {
        String[] arr = { "apple", "banana", "apricot", "cherry", "avocado" };
        int[] indices = CommonUtil.indicesOfAll(arr, s -> s.startsWith("a"), 1);
        Assertions.assertArrayEquals(new int[] { 2, 4 }, indices);

        int[] beyondIndices = CommonUtil.indicesOfAll(arr, s -> s.startsWith("a"), 10);
        Assertions.assertArrayEquals(new int[] {}, beyondIndices);
    }

    @Test
    public void testIndicesOfAll_collection_withPredicate() {
        List<String> list = Arrays.asList("apple", "banana", "apricot", "cherry", "avocado");
        int[] indices = CommonUtil.indicesOfAll(list, s -> s.startsWith("a"));
        Assertions.assertArrayEquals(new int[] { 0, 2, 4 }, indices);

        List<String> empty = new ArrayList<>();
        int[] emptyIndices = CommonUtil.indicesOfAll(empty, s -> true);
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);

        LinkedList<String> linkedList = new LinkedList<>(list);
        int[] linkedIndices = CommonUtil.indicesOfAll(linkedList, s -> s.startsWith("a"));
        Assertions.assertArrayEquals(new int[] { 0, 2, 4 }, linkedIndices);
    }

    @Test
    public void testIndicesOfAll_collection_withPredicateAndFromIndex() {
        List<String> list = Arrays.asList("apple", "banana", "apricot", "cherry", "avocado");
        int[] indices = CommonUtil.indicesOfAll(list, s -> s.startsWith("a"), 1);
        Assertions.assertArrayEquals(new int[] { 2, 4 }, indices);

        int[] beyondIndices = CommonUtil.indicesOfAll(list, s -> s.startsWith("a"), 10);
        Assertions.assertArrayEquals(new int[] {}, beyondIndices);
    }

    @Test
    public void testEdgeCases() {
        Integer[] allSame = { 5, 5, 5, 5, 5 };
        int[] minIndices = CommonUtil.indicesOfMin(allSame);
        Assertions.assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, minIndices);
        int[] maxIndices = CommonUtil.indicesOfMax(allSame);
        Assertions.assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, maxIndices);

        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals(-1, CommonUtil.indexOf(arr, "a", Integer.MAX_VALUE));
        Assertions.assertEquals(-1, CommonUtil.lastIndexOf(arr, "a", Integer.MIN_VALUE));
    }

    @Test
    public void testWithSpecialFloatingPointValues() {
        double[] arrWithNaN = { 1.0, Double.NaN, 3.0, Double.NaN };
        Assertions.assertEquals(1, CommonUtil.indexOf(arrWithNaN, Double.NaN));
        Assertions.assertEquals(3, CommonUtil.lastIndexOf(arrWithNaN, Double.NaN));

        double[] arrWithInf = { 1.0, Double.POSITIVE_INFINITY, 3.0, Double.NEGATIVE_INFINITY };
        Assertions.assertEquals(1, CommonUtil.indexOf(arrWithInf, Double.POSITIVE_INFINITY));
        Assertions.assertEquals(3, CommonUtil.indexOf(arrWithInf, Double.NEGATIVE_INFINITY));
    }

    @Test
    public void testGetDescendingIteratorIfPossible() {
        Deque<String> deque = new ArrayDeque<>(Arrays.asList("a", "b", "c"));
        Iterator<String> descIter = CommonUtil.getDescendingIteratorIfPossible(deque);
        Assertions.assertNotNull(descIter);

        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Iterator<String> listIter = CommonUtil.getDescendingIteratorIfPossible(list);
        Assertions.assertNull(listIter);
    }

    @Test
    public void testCreateMask() {
        Runnable mask = CommonUtil.createMask(Runnable.class);
        Assertions.assertNotNull(mask);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> mask.run());

        Comparator<?> compMask = CommonUtil.createMask(Comparator.class);
        Assertions.assertNotNull(compMask);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> compMask.compare(null, null));
    }

    @Test
    public void testCloneDouble3DArray() {
        double[][][] nullArray = null;
        assertNull(CommonUtil.clone(nullArray));

        double[][][] emptyArray = new double[0][][];
        double[][][] clonedEmpty = CommonUtil.clone(emptyArray);
        assertNotNull(clonedEmpty);
        assertEquals(0, clonedEmpty.length);

        double[][][] original = new double[][][] { { { 1.0, 2.0 }, { 3.0, 4.0 } }, { { 5.0, 6.0 }, { 7.0, 8.0 } } };

        double[][][] cloned = CommonUtil.clone(original);
        assertNotNull(cloned);
        assertNotSame(original, cloned);
        assertEquals(original.length, cloned.length);

        for (int i = 0; i < original.length; i++) {
            assertNotSame(original[i], cloned[i]);
            assertEquals(original[i].length, cloned[i].length);

            for (int j = 0; j < original[i].length; j++) {
                assertNotSame(original[i][j], cloned[i][j]);
                assertArrayEquals(original[i][j], cloned[i][j]);
            }
        }

        cloned[0][0][0] = 99.0;
        assertNotEquals(original[0][0][0], cloned[0][0][0]);
    }

    @Test
    public void testCloneGeneric3DArray() {
        String[][][] nullArray = null;
        assertNull(CommonUtil.clone(nullArray));

        String[][][] emptyArray = new String[0][][];
        String[][][] clonedEmpty = CommonUtil.clone(emptyArray);
        assertNotNull(clonedEmpty);
        assertEquals(0, clonedEmpty.length);

        String[][][] original = new String[][][] { { { "a", "b" }, { "c", "d" } }, { { "e", "f" }, { "g", "h" } } };

        String[][][] cloned = CommonUtil.clone(original);
        assertNotNull(cloned);
        assertNotSame(original, cloned);
        assertEquals(original.length, cloned.length);

        for (int i = 0; i < original.length; i++) {
            assertNotSame(original[i], cloned[i]);
            assertEquals(original[i].length, cloned[i].length);

            for (int j = 0; j < original[i].length; j++) {
                assertNotSame(original[i][j], cloned[i][j]);
                assertArrayEquals(original[i][j], cloned[i][j]);
            }
        }
    }

    @Test
    public void testIsSortedBooleanArray() {
        assertTrue(CommonUtil.isSorted((boolean[]) null));
        assertTrue(CommonUtil.isSorted(new boolean[0]));
        assertTrue(CommonUtil.isSorted(new boolean[] { true }));

        assertTrue(CommonUtil.isSorted(new boolean[] { false, false, true, true }));
        assertTrue(CommonUtil.isSorted(new boolean[] { false, true }));

        assertFalse(CommonUtil.isSorted(new boolean[] { true, false }));
        assertFalse(CommonUtil.isSorted(new boolean[] { false, true, false }));
    }

    @Test
    public void testIsSortedBooleanArrayRange() {
        boolean[] array = { true, false, false, true, true };

        assertTrue(CommonUtil.isSorted(array, 1, 3));
        assertTrue(CommonUtil.isSorted(array, 3, 5));
        assertFalse(CommonUtil.isSorted(array, 0, 3));

        assertTrue(CommonUtil.isSorted(array, 2, 2));
        assertTrue(CommonUtil.isSorted(array, 2, 3));

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.isSorted(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.isSorted(array, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.isSorted(array, 3, 2));
    }

    @Test
    public void testIsSortedCharArray() {
        assertTrue(CommonUtil.isSorted((char[]) null));
        assertTrue(CommonUtil.isSorted(new char[0]));
        assertTrue(CommonUtil.isSorted(new char[] { 'a' }));

        assertTrue(CommonUtil.isSorted(new char[] { 'a', 'b', 'c', 'd' }));
        assertTrue(CommonUtil.isSorted(new char[] { 'a', 'a', 'b', 'b' }));

        assertFalse(CommonUtil.isSorted(new char[] { 'b', 'a' }));
        assertFalse(CommonUtil.isSorted(new char[] { 'a', 'c', 'b' }));
    }

    @Test
    public void testIsSortedCharArrayRange() {
        char[] array = { 'd', 'a', 'b', 'c', 'e' };

        assertTrue(CommonUtil.isSorted(array, 1, 4));
        assertFalse(CommonUtil.isSorted(array, 0, 3));

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.isSorted(array, -1, 2));
    }

    @Test
    public void testIsSortedByteArray() {
        assertTrue(CommonUtil.isSorted((byte[]) null));
        assertTrue(CommonUtil.isSorted(new byte[0]));
        assertTrue(CommonUtil.isSorted(new byte[] { 1, 2, 3 }));
        assertFalse(CommonUtil.isSorted(new byte[] { 3, 2, 1 }));
    }

    @Test
    public void testIsSortedByteArrayRange() {
        byte[] array = { 5, 1, 2, 3, 0 };
        assertTrue(CommonUtil.isSorted(array, 1, 4));
        assertFalse(CommonUtil.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedShortArray() {
        assertTrue(CommonUtil.isSorted((short[]) null));
        assertTrue(CommonUtil.isSorted(new short[0]));
        assertTrue(CommonUtil.isSorted(new short[] { 1, 2, 3 }));
        assertFalse(CommonUtil.isSorted(new short[] { 3, 2, 1 }));
    }

    @Test
    public void testIsSortedShortArrayRange() {
        short[] array = { 5, 1, 2, 3, 0 };
        assertTrue(CommonUtil.isSorted(array, 1, 4));
        assertFalse(CommonUtil.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedIntArray() {
        assertTrue(CommonUtil.isSorted((int[]) null));
        assertTrue(CommonUtil.isSorted(new int[0]));
        assertTrue(CommonUtil.isSorted(new int[] { 1, 2, 3 }));
        assertFalse(CommonUtil.isSorted(new int[] { 3, 2, 1 }));
    }

    @Test
    public void testIsSortedIntArrayRange() {
        int[] array = { 5, 1, 2, 3, 0 };
        assertTrue(CommonUtil.isSorted(array, 1, 4));
        assertFalse(CommonUtil.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedLongArray() {
        assertTrue(CommonUtil.isSorted((long[]) null));
        assertTrue(CommonUtil.isSorted(new long[0]));
        assertTrue(CommonUtil.isSorted(new long[] { 1L, 2L, 3L }));
        assertFalse(CommonUtil.isSorted(new long[] { 3L, 2L, 1L }));
    }

    @Test
    public void testIsSortedLongArrayRange() {
        long[] array = { 5L, 1L, 2L, 3L, 0L };
        assertTrue(CommonUtil.isSorted(array, 1, 4));
        assertFalse(CommonUtil.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedFloatArray() {
        assertTrue(CommonUtil.isSorted((float[]) null));
        assertTrue(CommonUtil.isSorted(new float[0]));
        assertTrue(CommonUtil.isSorted(new float[] { 1.0f, 2.0f, 3.0f }));
        assertFalse(CommonUtil.isSorted(new float[] { 3.0f, 2.0f, 1.0f }));

        assertTrue(CommonUtil.isSorted(new float[] { 1.0f, 2.0f, Float.NaN }));
    }

    @Test
    public void testIsSortedFloatArrayRange() {
        float[] array = { 5.0f, 1.0f, 2.0f, 3.0f, 0.0f };
        assertTrue(CommonUtil.isSorted(array, 1, 4));
        assertFalse(CommonUtil.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedDoubleArray() {
        assertTrue(CommonUtil.isSorted((double[]) null));
        assertTrue(CommonUtil.isSorted(new double[0]));
        assertTrue(CommonUtil.isSorted(new double[] { 1.0, 2.0, 3.0 }));
        assertFalse(CommonUtil.isSorted(new double[] { 3.0, 2.0, 1.0 }));

        assertTrue(CommonUtil.isSorted(new double[] { 1.0, 2.0, Double.NaN }));
    }

    @Test
    public void testIsSortedDoubleArrayRange() {
        double[] array = { 5.0, 1.0, 2.0, 3.0, 0.0 };
        assertTrue(CommonUtil.isSorted(array, 1, 4));
        assertFalse(CommonUtil.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedObjectArray() {
        assertTrue(CommonUtil.isSorted((String[]) null));
        assertTrue(CommonUtil.isSorted(new String[0]));
        assertTrue(CommonUtil.isSorted(new String[] { "a", "b", "c" }));
        assertFalse(CommonUtil.isSorted(new String[] { "c", "b", "a" }));

        assertTrue(CommonUtil.isSorted(new String[] { null, null, "a", "b" }));
        assertFalse(CommonUtil.isSorted(new String[] { "a", null, "b" }));
    }

    @Test
    public void testIsSortedObjectArrayRange() {
        String[] array = { "d", "a", "b", "c", "e" };
        assertTrue(CommonUtil.isSorted(array, 1, 4));
        assertFalse(CommonUtil.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedObjectArrayWithComparator() {
        String[] array = { "aaa", "bb", "c" };

        assertTrue(CommonUtil.isSorted(array, Comparator.naturalOrder()));

        assertTrue(CommonUtil.isSorted(array, Comparator.comparing(String::length).reversed()));

        assertTrue(CommonUtil.isSorted(array, null));
    }

    @Test
    public void testIsSortedObjectArrayRangeWithComparator() {
        String[] array = { "d", "aaa", "bb", "c", "e" };

        assertTrue(CommonUtil.isSorted(array, 1, 4, Comparator.comparing(String::length).reversed()));
        assertFalse(CommonUtil.isSorted(array, 0, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testIsSortedCollection() {
        assertTrue(CommonUtil.isSorted((Collection<String>) null));
        assertTrue(CommonUtil.isSorted(new ArrayList<String>()));
        assertTrue(CommonUtil.isSorted(Arrays.asList("a", "b", "c")));
        assertFalse(CommonUtil.isSorted(Arrays.asList("c", "b", "a")));

        assertTrue(CommonUtil.isSorted(Arrays.asList(null, null, "a", "b")));
    }

    @Test
    public void testIsSortedCollectionRange() {
        List<String> list = Arrays.asList("d", "a", "b", "c", "e");
        assertTrue(CommonUtil.isSorted(list, 1, 4));
        assertFalse(CommonUtil.isSorted(list, 0, 3));

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.isSorted(list, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.isSorted(list, 0, 6));
    }

    @Test
    public void testIsSortedCollectionWithComparator() {
        List<String> list = Arrays.asList("aaa", "bb", "c");

        assertTrue(CommonUtil.isSorted(list, Comparator.naturalOrder()));
        assertTrue(CommonUtil.isSorted(list, Comparator.comparing(String::length).reversed()));
    }

    @Test
    public void testIsSortedCollectionRangeWithComparator() {
        List<String> list = Arrays.asList("d", "aaa", "bb", "c", "e");

        assertTrue(CommonUtil.isSorted(list, 1, 4, Comparator.comparing(String::length).reversed()));
        assertFalse(CommonUtil.isSorted(list, 0, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testSortBooleanArray() {
        CommonUtil.sort((boolean[]) null);
        CommonUtil.sort(new boolean[0]);

        boolean[] array = { true, false, true, false, false };
        CommonUtil.sort(array);
        assertArrayEquals(new boolean[] { false, false, false, true, true }, array);

        boolean[] sorted = { false, false, true, true };
        CommonUtil.sort(sorted);
        assertArrayEquals(new boolean[] { false, false, true, true }, sorted);
    }

    @Test
    public void testSortBooleanArrayRange() {
        boolean[] array = { true, true, false, true, false };
        CommonUtil.sort(array, 1, 4);
        assertArrayEquals(new boolean[] { true, false, true, true, false }, array);

        boolean[] array2 = { true, false };
        CommonUtil.sort(array2, 1, 1);
        assertArrayEquals(new boolean[] { true, false }, array2);

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.sort(array, -1, 2));
    }

    @Test
    public void testSortCharArray() {
        CommonUtil.sort((char[]) null);
        CommonUtil.sort(new char[0]);

        char[] array = { 'd', 'b', 'a', 'c' };
        CommonUtil.sort(array);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, array);
    }

    @Test
    public void testSortCharArrayRange() {
        char[] array = { 'd', 'c', 'b', 'a', 'e' };
        CommonUtil.sort(array, 1, 4);
        assertArrayEquals(new char[] { 'd', 'a', 'b', 'c', 'e' }, array);
    }

    @Test
    public void testSortByteArray() {
        CommonUtil.sort((byte[]) null);
        CommonUtil.sort(new byte[0]);

        byte[] array = { 4, 2, 1, 3 };
        CommonUtil.sort(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortByteArrayRange() {
        byte[] array = { 5, 3, 2, 1, 4 };
        CommonUtil.sort(array, 1, 4);
        assertArrayEquals(new byte[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortShortArray() {
        CommonUtil.sort((short[]) null);
        CommonUtil.sort(new short[0]);

        short[] array = { 4, 2, 1, 3 };
        CommonUtil.sort(array);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortShortArrayRange() {
        short[] array = { 5, 3, 2, 1, 4 };
        CommonUtil.sort(array, 1, 4);
        assertArrayEquals(new short[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortIntArray() {
        CommonUtil.sort((int[]) null);
        CommonUtil.sort(new int[0]);

        int[] array = { 4, 2, 1, 3 };
        CommonUtil.sort(array);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortIntArrayRange() {
        int[] array = { 5, 3, 2, 1, 4 };
        CommonUtil.sort(array, 1, 4);
        assertArrayEquals(new int[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortLongArray() {
        CommonUtil.sort((long[]) null);
        CommonUtil.sort(new long[0]);

        long[] array = { 4L, 2L, 1L, 3L };
        CommonUtil.sort(array);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, array);
    }

    @Test
    public void testSortLongArrayRange() {
        long[] array = { 5L, 3L, 2L, 1L, 4L };
        CommonUtil.sort(array, 1, 4);
        assertArrayEquals(new long[] { 5L, 1L, 2L, 3L, 4L }, array);
    }

    @Test
    public void testSortFloatArray() {
        CommonUtil.sort((float[]) null);
        CommonUtil.sort(new float[0]);

        float[] array = { 4.0f, 2.0f, 1.0f, 3.0f };
        CommonUtil.sort(array);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, array);
    }

    @Test
    public void testSortFloatArrayRange() {
        float[] array = { 5.0f, 3.0f, 2.0f, 1.0f, 4.0f };
        CommonUtil.sort(array, 1, 4);
        assertArrayEquals(new float[] { 5.0f, 1.0f, 2.0f, 3.0f, 4.0f }, array);
    }

    @Test
    public void testSortDoubleArray() {
        CommonUtil.sort((double[]) null);
        CommonUtil.sort(new double[0]);

        double[] array = { 4.0, 2.0, 1.0, 3.0 };
        CommonUtil.sort(array);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, array);
    }

    @Test
    public void testSortDoubleArrayRange() {
        double[] array = { 5.0, 3.0, 2.0, 1.0, 4.0 };
        CommonUtil.sort(array, 1, 4);
        assertArrayEquals(new double[] { 5.0, 1.0, 2.0, 3.0, 4.0 }, array);
    }

    @Test
    public void testSortObjectArray() {
        CommonUtil.sort((Object[]) null);
        CommonUtil.sort(new Object[0]);

        String[] array = { "d", "b", "a", "c" };
        CommonUtil.sort(array);
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, array);

        String[] arrayWithNulls = { "b", null, "a", null };
        CommonUtil.sort(arrayWithNulls);
        assertArrayEquals(new String[] { null, null, "a", "b" }, arrayWithNulls);
    }

    @Test
    public void testSortObjectArrayRange() {
        String[] array = { "e", "d", "c", "b", "a" };
        CommonUtil.sort(array, 1, 4);
        assertArrayEquals(new String[] { "e", "b", "c", "d", "a" }, array);
    }

    @Test
    public void testSortObjectArrayWithComparator() {
        String[] array = { "aaa", "b", "cc" };
        CommonUtil.sort(array, Comparator.comparing(String::length));
        assertArrayEquals(new String[] { "b", "cc", "aaa" }, array);
    }

    @Test
    public void testSortObjectArrayRangeWithComparator() {
        String[] array = { "e", "aaa", "b", "cc", "d" };
        CommonUtil.sort(array, 1, 4, Comparator.comparing(String::length));
        assertArrayEquals(new String[] { "e", "b", "cc", "aaa", "d" }, array);
    }

    @Test
    public void testSortList() {
        CommonUtil.sort((List<String>) null);
        CommonUtil.sort(new ArrayList<String>());

        List<String> list = new ArrayList<>(Arrays.asList("d", "b", "a", "c"));
        CommonUtil.sort(list);
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
    }

    @Test
    public void testSortListRange() {
        List<String> list = new ArrayList<>(Arrays.asList("e", "d", "c", "b", "a"));
        CommonUtil.sort(list, 1, 4);
        assertEquals(Arrays.asList("e", "b", "c", "d", "a"), list);
    }

    @Test
    public void testSortListWithComparator() {
        List<String> list = new ArrayList<>(Arrays.asList("aaa", "b", "cc"));
        CommonUtil.sort(list, Comparator.comparing(String::length));
        assertEquals(Arrays.asList("b", "cc", "aaa"), list);
    }

    @Test
    public void testSortListRangeWithComparator() {
        List<String> list = new ArrayList<>(Arrays.asList("e", "aaa", "b", "cc", "d"));
        CommonUtil.sort(list, 1, 4, Comparator.comparing(String::length));
        assertEquals(Arrays.asList("e", "b", "cc", "aaa", "d"), list);

        List<String> emptyList = new ArrayList<>();
        CommonUtil.sort(emptyList, 0, 0, Comparator.naturalOrder());
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testParallelSortCharArray() {
        CommonUtil.parallelSort((char[]) null);
        CommonUtil.parallelSort(new char[0]);

        char[] array = { 'd', 'b', 'a', 'c' };
        CommonUtil.parallelSort(array);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, array);
    }

    @Test
    public void testParallelSortCharArrayRange() {
        char[] array = { 'e', 'd', 'c', 'b', 'a' };
        CommonUtil.parallelSort(array, 1, 4);
        assertArrayEquals(new char[] { 'e', 'b', 'c', 'd', 'a' }, array);
    }

    @Test
    public void testParallelSortByteArray() {
        CommonUtil.parallelSort((byte[]) null);
        CommonUtil.parallelSort(new byte[0]);

        byte[] array = { 4, 2, 1, 3 };
        CommonUtil.parallelSort(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortByteArrayRange() {
        byte[] array = { 5, 3, 2, 1, 4 };
        CommonUtil.parallelSort(array, 1, 4);
        assertArrayEquals(new byte[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortShortArray() {
        CommonUtil.parallelSort((short[]) null);
        CommonUtil.parallelSort(new short[0]);

        short[] array = { 4, 2, 1, 3 };
        CommonUtil.parallelSort(array);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortShortArrayRange() {
        short[] array = { 5, 3, 2, 1, 4 };
        CommonUtil.parallelSort(array, 1, 4);
        assertArrayEquals(new short[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortIntArray() {
        CommonUtil.parallelSort((int[]) null);
        CommonUtil.parallelSort(new int[0]);

        int[] array = { 4, 2, 1, 3 };
        CommonUtil.parallelSort(array);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortIntArrayRange() {
        int[] array = { 5, 3, 2, 1, 4 };
        CommonUtil.parallelSort(array, 1, 4);
        assertArrayEquals(new int[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortLongArray() {
        CommonUtil.parallelSort((long[]) null);
        CommonUtil.parallelSort(new long[0]);

        long[] array = { 4L, 2L, 1L, 3L };
        CommonUtil.parallelSort(array);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, array);
    }

    @Test
    public void testParallelSortLongArrayRange() {
        long[] array = { 5L, 3L, 2L, 1L, 4L };
        CommonUtil.parallelSort(array, 1, 4);
        assertArrayEquals(new long[] { 5L, 1L, 2L, 3L, 4L }, array);
    }

    @Test
    public void testParallelSortFloatArray() {
        CommonUtil.parallelSort((float[]) null);
        CommonUtil.parallelSort(new float[0]);

        float[] array = { 4.0f, 2.0f, 1.0f, 3.0f };
        CommonUtil.parallelSort(array);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, array);
    }

    @Test
    public void testParallelSortFloatArrayRange() {
        float[] array = { 5.0f, 3.0f, 2.0f, 1.0f, 4.0f };
        CommonUtil.parallelSort(array, 1, 4);
        assertArrayEquals(new float[] { 5.0f, 1.0f, 2.0f, 3.0f, 4.0f }, array);
    }

    @Test
    public void testParallelSortDoubleArray() {
        CommonUtil.parallelSort((double[]) null);
        CommonUtil.parallelSort(new double[0]);

        double[] array = { 4.0, 2.0, 1.0, 3.0 };
        CommonUtil.parallelSort(array);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, array);
    }

    @Test
    public void testParallelSortDoubleArrayRange() {
        double[] array = { 5.0, 3.0, 2.0, 1.0, 4.0 };
        CommonUtil.parallelSort(array, 1, 4);
        assertArrayEquals(new double[] { 5.0, 1.0, 2.0, 3.0, 4.0 }, array);
    }

    @Test
    public void testParallelSortObjectArray() {
        CommonUtil.parallelSort((String[]) null);
        CommonUtil.parallelSort(new String[0]);

        String[] array = { "d", "b", "a", "c" };
        CommonUtil.parallelSort(array);
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, array);
    }

    @Test
    public void testParallelSortObjectArrayRange() {
        String[] array = { "e", "d", "c", "b", "a" };
        CommonUtil.parallelSort(array, 1, 4);
        assertArrayEquals(new String[] { "e", "b", "c", "d", "a" }, array);
    }

    @Test
    public void testParallelSortObjectArrayWithComparator() {
        String[] array = { "aaa", "b", "cc" };
        CommonUtil.parallelSort(array, Comparator.comparing(String::length));
        assertArrayEquals(new String[] { "b", "cc", "aaa" }, array);
    }

    @Test
    public void testParallelSortObjectArrayRangeWithComparator() {
        String[] array = { "e", "aaa", "b", "cc", "d" };
        CommonUtil.parallelSort(array, 1, 4, Comparator.comparing(String::length));
        assertArrayEquals(new String[] { "e", "b", "cc", "aaa", "d" }, array);
    }

    @Test
    public void testParallelSortList() {
        CommonUtil.parallelSort((List<String>) null);
        CommonUtil.parallelSort(new ArrayList<String>());

        List<String> list = new ArrayList<>(Arrays.asList("d", "b", "a", "c"));
        CommonUtil.parallelSort(list);
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
    }

    @Test
    public void testParallelSortListRange() {
        List<String> list = new ArrayList<>(Arrays.asList("e", "d", "c", "b", "a"));
        CommonUtil.parallelSort(list, 1, 4);
        assertEquals(Arrays.asList("e", "b", "c", "d", "a"), list);
    }

    @Test
    public void testParallelSortListWithComparator() {
        List<String> list = new ArrayList<>(Arrays.asList("aaa", "b", "cc"));
        CommonUtil.parallelSort(list, Comparator.comparing(String::length));
        assertEquals(Arrays.asList("b", "cc", "aaa"), list);
    }

    @Test
    public void testParallelSortListRangeWithComparator() {
        List<String> list = new ArrayList<>(Arrays.asList("e", "aaa", "b", "cc", "d"));
        CommonUtil.parallelSort(list, 1, 4, Comparator.comparing(String::length));
        assertEquals(Arrays.asList("e", "b", "cc", "aaa", "d"), list);
    }

    @Test
    public void testReverseSortBooleanArray() {
        CommonUtil.reverseSort((boolean[]) null);

        boolean[] array = { false, true, false, true, true };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new boolean[] { true, true, true, false, false }, array);
    }

    @Test
    public void testReverseSortBooleanArrayRange() {
        boolean[] array = { false, false, true, false, true };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new boolean[] { false, true, false, false, true }, array);
    }

    @Test
    public void testReverseSortCharArray() {
        char[] array = { 'a', 'c', 'b', 'd' };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new char[] { 'd', 'c', 'b', 'a' }, array);
    }

    @Test
    public void testReverseSortCharArrayRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new char[] { 'a', 'd', 'c', 'b', 'e' }, array);
    }

    @Test
    public void testReverseSortByteArray() {
        byte[] array = { 1, 3, 2, 4 };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new byte[] { 4, 3, 2, 1 }, array);
    }

    @Test
    public void testReverseSortByteArrayRange() {
        byte[] array = { 1, 2, 3, 4, 5 };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new byte[] { 1, 4, 3, 2, 5 }, array);
    }

    @Test
    public void testReverseSortShortArray() {
        short[] array = { 1, 3, 2, 4 };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new short[] { 4, 3, 2, 1 }, array);
    }

    @Test
    public void testReverseSortShortArrayRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new short[] { 1, 4, 3, 2, 5 }, array);
    }

    @Test
    public void testReverseSortIntArray() {
        int[] array = { 1, 3, 2, 4 };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new int[] { 4, 3, 2, 1 }, array);
    }

    @Test
    public void testReverseSortIntArrayRange() {
        int[] array = { 1, 2, 3, 4, 5 };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new int[] { 1, 4, 3, 2, 5 }, array);
    }

    @Test
    public void testReverseSortLongArray() {
        long[] array = { 1L, 3L, 2L, 4L };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new long[] { 4L, 3L, 2L, 1L }, array);
    }

    @Test
    public void testReverseSortLongArrayRange() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new long[] { 1L, 4L, 3L, 2L, 5L }, array);
    }

    @Test
    public void testReverseSortFloatArray() {
        float[] array = { 1.0f, 3.0f, 2.0f, 4.0f };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new float[] { 4.0f, 3.0f, 2.0f, 1.0f }, array);
    }

    @Test
    public void testReverseSortFloatArrayRange() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new float[] { 1.0f, 4.0f, 3.0f, 2.0f, 5.0f }, array);
    }

    @Test
    public void testReverseSortDoubleArray() {
        double[] array = { 1.0, 3.0, 2.0, 4.0 };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new double[] { 4.0, 3.0, 2.0, 1.0 }, array);
    }

    @Test
    public void testReverseSortDoubleArrayRange() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new double[] { 1.0, 4.0, 3.0, 2.0, 5.0 }, array);
    }

    @Test
    public void testReverseSortObjectArray() {
        String[] array = { "a", "c", "b", "d" };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new String[] { "d", "c", "b", "a" }, array);
    }

    @Test
    public void testReverseSortObjectArrayRange() {
        String[] array = { "a", "b", "c", "d", "e" };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new String[] { "a", "d", "c", "b", "e" }, array);
    }

    @Test
    public void testReverseSortList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "c", "b", "d"));
        CommonUtil.reverseSort(list);
        assertEquals(Arrays.asList("d", "c", "b", "a"), list);
    }

    @Test
    public void testReverseSortListRange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        CommonUtil.reverseSort(list, 1, 4);
        assertEquals(Arrays.asList("a", "d", "c", "b", "e"), list);
    }

    @Test
    public void testBinarySearchCharArray() {
        assertEquals(-1, CommonUtil.binarySearch((char[]) null, 'a'));
        assertEquals(-1, CommonUtil.binarySearch(new char[0], 'a'));

        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        assertEquals(0, CommonUtil.binarySearch(array, 'a'));
        assertEquals(2, CommonUtil.binarySearch(array, 'c'));
        assertEquals(4, CommonUtil.binarySearch(array, 'e'));
        assertTrue(CommonUtil.binarySearch(array, 'f') < 0);
    }

    @Test
    public void testBinarySearchCharArrayRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        assertEquals(2, CommonUtil.binarySearch(array, 1, 4, 'c'));
        assertTrue(CommonUtil.binarySearch(array, 1, 4, 'a') < 0);
        assertTrue(CommonUtil.binarySearch(array, 1, 4, 'e') < 0);
    }

    @Test
    public void testBinarySearchByteArray() {
        assertEquals(-1, CommonUtil.binarySearch((byte[]) null, (byte) 1));
        assertEquals(-1, CommonUtil.binarySearch(new byte[0], (byte) 1));

        byte[] array = { 1, 2, 3, 4, 5 };
        assertEquals(0, CommonUtil.binarySearch(array, (byte) 1));
        assertEquals(2, CommonUtil.binarySearch(array, (byte) 3));
        assertEquals(4, CommonUtil.binarySearch(array, (byte) 5));
        assertTrue(CommonUtil.binarySearch(array, (byte) 6) < 0);
    }

    @Test
    public void testBinarySearchByteArrayRange() {
        byte[] array = { 1, 2, 3, 4, 5 };
        assertEquals(2, CommonUtil.binarySearch(array, 1, 4, (byte) 3));
        assertTrue(CommonUtil.binarySearch(array, 1, 4, (byte) 1) < 0);
        assertTrue(CommonUtil.binarySearch(array, 1, 4, (byte) 5) < 0);
    }

    @Test
    public void testBinarySearchShortArray() {
        assertEquals(-1, CommonUtil.binarySearch((short[]) null, (short) 1));
        assertEquals(-1, CommonUtil.binarySearch(new short[0], (short) 1));

        short[] array = { 1, 2, 3, 4, 5 };
        assertEquals(0, CommonUtil.binarySearch(array, (short) 1));
        assertEquals(2, CommonUtil.binarySearch(array, (short) 3));
        assertEquals(4, CommonUtil.binarySearch(array, (short) 5));
        assertTrue(CommonUtil.binarySearch(array, (short) 6) < 0);
    }

    @Test
    public void testShuffleFloatArrayWithRange() {

        for (int i = 0; i < 10000; i++) {
            float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
            float[] original = arr.clone();
            CommonUtil.shuffle(arr, 1, 4);

            Assertions.assertEquals(original[0], arr[0]);
            Assertions.assertEquals(original[4], arr[4], 0.000001);

            Arrays.sort(arr);
            Arrays.sort(original);
            Assertions.assertArrayEquals(original, arr);
        }
    }

    @Test
    public void testShuffleFloatArrayWithRandom() {

        for (int i = 0; i < 10000; i++) {
            float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
            float[] original = arr.clone();
            Random rnd = new Random(42);
            CommonUtil.shuffle(arr, rnd);

            Arrays.sort(arr);
            Arrays.sort(original);
            Assertions.assertArrayEquals(original, arr);
        }
    }

    @Test
    public void testShuffleFloatArrayWithRangeAndRandom() {

        for (int i = 0; i < 10000; i++) {
            float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
            float[] original = arr.clone();
            Random rnd = new Random(42);
            CommonUtil.shuffle(arr, 1, 4, rnd);

            Assertions.assertEquals(original[0], arr[0]);
            Assertions.assertEquals(original[4], arr[4]);

            Arrays.sort(arr);
            Arrays.sort(original);
            Assertions.assertArrayEquals(original, arr);
        }
    }

    @Test
    public void testShuffleFloatArrayEmpty() {
        float[] arr = {};
        CommonUtil.shuffle(arr, 0, 0, new Random());
        Assertions.assertEquals(0, arr.length);
    }

    @Test
    public void testShuffleFloatArrayNull() {
        float[] arr = null;
        CommonUtil.shuffle(arr, new Random());
        Assertions.assertNull(arr);
    }

    @Test
    public void testShuffleDoubleArray() {

        for (int i = 0; i < 10000; i++) {
            double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
            double[] original = arr.clone();
            CommonUtil.shuffle(arr);

            Arrays.sort(arr);
            Arrays.sort(original);
            Assertions.assertArrayEquals(original, arr);
        }
    }

    @Test
    public void testShuffleDoubleArrayWithRange() {
        for (int i = 0; i < 10000; i++) {
            double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
            double[] original = arr.clone();
            CommonUtil.shuffle(arr, 1, 4);

            Assertions.assertEquals(original[0], arr[0]);
            Assertions.assertEquals(original[4], arr[4], 0.000001);

            Arrays.sort(arr);
            Arrays.sort(original);
            Assertions.assertArrayEquals(original, arr);
        }
    }

    @Test
    public void testShuffleDoubleArrayWithRandom() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] original = arr.clone();
        Random rnd = new Random(42);
        CommonUtil.shuffle(arr, rnd);

        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    @Test
    public void testShuffleDoubleArrayWithRangeAndRandom() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] original = arr.clone();
        Random rnd = new Random(42);
        CommonUtil.shuffle(arr, 1, 4, rnd);

        Assertions.assertEquals(original[0], arr[0]);
        Assertions.assertEquals(original[4], arr[4]);

        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    @Test
    public void testShuffleObjectArray() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String[] original = arr.clone();
        CommonUtil.shuffle(arr);

        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    @Test
    public void testShuffleObjectArrayWithRange() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String[] original = arr.clone();
        CommonUtil.shuffle(arr, 1, 4);

        Assertions.assertEquals(original[0], arr[0]);
        Assertions.assertEquals(original[4], arr[4]);

        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    @Test
    public void testShuffleObjectArrayWithRandom() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String[] original = arr.clone();
        Random rnd = new Random(42);
        CommonUtil.shuffle(arr, rnd);

        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    @Test
    public void testShuffleObjectArrayWithRangeAndRandom() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String[] original = arr.clone();
        Random rnd = new Random(42);
        CommonUtil.shuffle(arr, 1, 4, rnd);

        Assertions.assertEquals(original[0], arr[0]);
        Assertions.assertEquals(original[4], arr[4]);

        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    @Test
    public void testShuffleList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        List<String> original = new ArrayList<>(list);
        CommonUtil.shuffle(list);

        Collections.sort(list);
        Collections.sort(original);
        Assertions.assertEquals(original, list);
    }

    @Test
    public void testShuffleListWithRandom() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        List<String> original = new ArrayList<>(list);
        Random rnd = new Random(42);
        CommonUtil.shuffle(list, rnd);

        Collections.sort(list);
        Collections.sort(original);
        Assertions.assertEquals(original, list);
    }

    @Test
    public void testShuffleListEmpty() {
        List<String> list = new ArrayList<>();
        CommonUtil.shuffle(list);
        Assertions.assertTrue(list.isEmpty());
    }

    @Test
    public void testShuffleListSingleElement() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        CommonUtil.shuffle(list);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("a", list.get(0));
    }

    @Test
    public void testShuffleCollection() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        List<String> original = new ArrayList<>(coll);
        CommonUtil.shuffle(coll);

        List<String> sorted = new ArrayList<>(coll);
        Collections.sort(sorted);
        Collections.sort(original);
        Assertions.assertEquals(original, sorted);
    }

    @Test
    public void testShuffleCollectionWithRandom() {
        Collection<String> coll = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d", "e"));
        List<String> original = new ArrayList<>(coll);
        Random rnd = new Random(42);
        CommonUtil.shuffle(coll, rnd);

        List<String> sorted = new ArrayList<>(coll);
        Collections.sort(sorted);
        Collections.sort(original);
        Assertions.assertEquals(original, sorted);
    }

    @Test
    public void testShuffleCollectionEmpty() {
        Collection<String> coll = new ArrayList<>();
        CommonUtil.shuffle(coll);
        Assertions.assertTrue(coll.isEmpty());
    }

    @Test
    public void testShuffleCollectionLessThanTwoElements() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a"));
        CommonUtil.shuffle(coll);
        Assertions.assertEquals(1, coll.size());
        Assertions.assertTrue(coll.contains("a"));
    }

    @Test
    public void testSwapBooleanArray() {
        boolean[] arr = { true, false, true, false };
        CommonUtil.swap(arr, 0, 3);
        Assertions.assertFalse(arr[0]);
        Assertions.assertTrue(arr[3]);
    }

    @Test
    public void testSwapCharArray() {
        char[] arr = { 'a', 'b', 'c', 'd' };
        CommonUtil.swap(arr, 1, 2);
        Assertions.assertEquals('c', arr[1]);
        Assertions.assertEquals('b', arr[2]);
    }

    @Test
    public void testSwapByteArray() {
        byte[] arr = { 1, 2, 3, 4 };
        CommonUtil.swap(arr, 0, 3);
        Assertions.assertEquals(4, arr[0]);
        Assertions.assertEquals(1, arr[3]);
    }

    @Test
    public void testSwapShortArray() {
        short[] arr = { 10, 20, 30, 40 };
        CommonUtil.swap(arr, 1, 3);
        Assertions.assertEquals(40, arr[1]);
        Assertions.assertEquals(20, arr[3]);
    }

    @Test
    public void testSwapIntArray() {
        int[] arr = { 100, 200, 300, 400 };
        CommonUtil.swap(arr, 0, 2);
        Assertions.assertEquals(300, arr[0]);
        Assertions.assertEquals(100, arr[2]);
    }

    @Test
    public void testSwapLongArray() {
        long[] arr = { 1000L, 2000L, 3000L, 4000L };
        CommonUtil.swap(arr, 1, 2);
        Assertions.assertEquals(3000L, arr[1]);
        Assertions.assertEquals(2000L, arr[2]);
    }

    @Test
    public void testSwapFloatArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f };
        CommonUtil.swap(arr, 0, 3);
        Assertions.assertEquals(4.0f, arr[0]);
        Assertions.assertEquals(1.0f, arr[3]);
    }

    @Test
    public void testSwapDoubleArray() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0 };
        CommonUtil.swap(arr, 1, 3);
        Assertions.assertEquals(4.0, arr[1]);
        Assertions.assertEquals(2.0, arr[3]);
    }

    @Test
    public void testSwapObjectArray() {
        String[] arr = { "a", "b", "c", "d" };
        CommonUtil.swap(arr, 0, 2);
        Assertions.assertEquals("c", arr[0]);
        Assertions.assertEquals("a", arr[2]);
    }

    @Test
    public void testSwapList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.swap(list, 1, 3);
        Assertions.assertEquals("d", list.get(1));
        Assertions.assertEquals("b", list.get(3));
    }

    @Test
    public void testSwapPair() {
        Pair<String, String> pair = Pair.of("left", "right");
        CommonUtil.swap(pair);
        Assertions.assertEquals("right", pair.left());
        Assertions.assertEquals("left", pair.right());
    }

    @Test
    public void testSwapIfPair() {
        Pair<Integer, Integer> pair = Pair.of(1, 2);
        boolean swapped = CommonUtil.swapIf(pair, p -> p.left() < p.right());
        Assertions.assertTrue(swapped);
        Assertions.assertEquals(2, pair.left());
        Assertions.assertEquals(1, pair.right());

        swapped = CommonUtil.swapIf(pair, p -> p.left() < p.right());
        Assertions.assertFalse(swapped);
    }

    @Test
    public void testSwapTriple() {
        Triple<String, Integer, String> triple = Triple.of("left", 42, "right");
        CommonUtil.swap(triple);
        Assertions.assertEquals("right", triple.left());
        Assertions.assertEquals("left", triple.right());
        Assertions.assertEquals(42, triple.middle());
    }

    @Test
    public void testSwapIfTriple() {
        Triple<Integer, String, Integer> triple = Triple.of(1, "middle", 2);
        boolean swapped = CommonUtil.swapIf(triple, t -> t.left() < t.right());
        Assertions.assertTrue(swapped);
        Assertions.assertEquals(2, triple.left());
        Assertions.assertEquals(1, triple.right());

        swapped = CommonUtil.swapIf(triple, t -> t.left() < t.right());
        Assertions.assertFalse(swapped);
    }

    @Test
    public void testFillBooleanArray() {
        boolean[] arr = new boolean[5];
        CommonUtil.fill(arr, true);
        for (boolean b : arr) {
            Assertions.assertTrue(b);
        }
    }

    @Test
    public void testFillBooleanArrayWithRange() {
        boolean[] arr = new boolean[5];
        CommonUtil.fill(arr, 1, 4, true);
        Assertions.assertFalse(arr[0]);
        Assertions.assertTrue(arr[1]);
        Assertions.assertTrue(arr[2]);
        Assertions.assertTrue(arr[3]);
        Assertions.assertFalse(arr[4]);
    }

    @Test
    public void testFillBooleanArrayEmpty() {
        boolean[] arr = new boolean[0];
        CommonUtil.fill(arr, true);
        Assertions.assertEquals(0, arr.length);
    }

    @Test
    public void testFillCharArray() {
        char[] arr = new char[5];
        CommonUtil.fill(arr, 'x');
        for (char c : arr) {
            Assertions.assertEquals('x', c);
        }
    }

    @Test
    public void testFillCharArrayWithRange() {
        char[] arr = new char[5];
        CommonUtil.fill(arr, 1, 4, 'x');
        Assertions.assertEquals('\0', arr[0]);
        Assertions.assertEquals('x', arr[1]);
        Assertions.assertEquals('x', arr[2]);
        Assertions.assertEquals('x', arr[3]);
        Assertions.assertEquals('\0', arr[4]);
    }

    @Test
    public void testFillByteArray() {
        byte[] arr = new byte[5];
        CommonUtil.fill(arr, (byte) 42);
        for (byte b : arr) {
            Assertions.assertEquals(42, b);
        }
    }

    @Test
    public void testFillByteArrayWithRange() {
        byte[] arr = new byte[5];
        CommonUtil.fill(arr, 2, 5, (byte) 42);
        Assertions.assertEquals(0, arr[0]);
        Assertions.assertEquals(0, arr[1]);
        Assertions.assertEquals(42, arr[2]);
        Assertions.assertEquals(42, arr[3]);
        Assertions.assertEquals(42, arr[4]);
    }

    @Test
    public void testFillShortArray() {
        short[] arr = new short[5];
        CommonUtil.fill(arr, (short) 100);
        for (short s : arr) {
            Assertions.assertEquals(100, s);
        }
    }

    @Test
    public void testFillShortArrayWithRange() {
        short[] arr = new short[5];
        CommonUtil.fill(arr, 0, 3, (short) 100);
        Assertions.assertEquals(100, arr[0]);
        Assertions.assertEquals(100, arr[1]);
        Assertions.assertEquals(100, arr[2]);
        Assertions.assertEquals(0, arr[3]);
        Assertions.assertEquals(0, arr[4]);
    }

    @Test
    public void testFillIntArray() {
        int[] arr = new int[5];
        CommonUtil.fill(arr, 999);
        for (int i : arr) {
            Assertions.assertEquals(999, i);
        }
    }

    @Test
    public void testFillIntArrayWithRange() {
        int[] arr = new int[5];
        CommonUtil.fill(arr, 1, 3, 999);
        Assertions.assertEquals(0, arr[0]);
        Assertions.assertEquals(999, arr[1]);
        Assertions.assertEquals(999, arr[2]);
        Assertions.assertEquals(0, arr[3]);
        Assertions.assertEquals(0, arr[4]);
    }

    @Test
    public void testFillLongArray() {
        long[] arr = new long[5];
        CommonUtil.fill(arr, 1000L);
        for (long l : arr) {
            Assertions.assertEquals(1000L, l);
        }
    }

    @Test
    public void testFillLongArrayWithRange() {
        long[] arr = new long[5];
        CommonUtil.fill(arr, 2, 4, 1000L);
        Assertions.assertEquals(0L, arr[0]);
        Assertions.assertEquals(0L, arr[1]);
        Assertions.assertEquals(1000L, arr[2]);
        Assertions.assertEquals(1000L, arr[3]);
        Assertions.assertEquals(0L, arr[4]);
    }

    @Test
    public void testFillFloatArray() {
        float[] arr = new float[5];
        CommonUtil.fill(arr, 3.14f);
        for (float f : arr) {
            Assertions.assertEquals(3.14f, f);
        }
    }

    @Test
    public void testFillFloatArrayWithRange() {
        float[] arr = new float[5];
        CommonUtil.fill(arr, 0, 2, 3.14f);
        Assertions.assertEquals(3.14f, arr[0]);
        Assertions.assertEquals(3.14f, arr[1]);
        Assertions.assertEquals(0.0f, arr[2]);
        Assertions.assertEquals(0.0f, arr[3]);
        Assertions.assertEquals(0.0f, arr[4]);
    }

    @Test
    public void testFillDoubleArray() {
        double[] arr = new double[5];
        CommonUtil.fill(arr, 2.718);
        for (double d : arr) {
            Assertions.assertEquals(2.718, d);
        }
    }

    @Test
    public void testFillDoubleArrayWithRange() {
        double[] arr = new double[5];
        CommonUtil.fill(arr, 3, 5, 2.718);
        Assertions.assertEquals(0.0, arr[0]);
        Assertions.assertEquals(0.0, arr[1]);
        Assertions.assertEquals(0.0, arr[2]);
        Assertions.assertEquals(2.718, arr[3]);
        Assertions.assertEquals(2.718, arr[4]);
    }

    @Test
    public void testFillObjectArrayWithRange() {
        String[] arr = new String[5];
        CommonUtil.fill(arr, 1, 4, "test");
        Assertions.assertNull(arr[0]);
        Assertions.assertEquals("test", arr[1]);
        Assertions.assertEquals("test", arr[2]);
        Assertions.assertEquals("test", arr[3]);
        Assertions.assertNull(arr[4]);
    }

    @Test
    public void testFillObjectArrayEmpty() {
        String[] arr = new String[0];
        CommonUtil.fill(arr, "test");
        Assertions.assertEquals(0, arr.length);
    }

    @Test
    public void testFillList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        CommonUtil.fill(list, "x");
        for (String s : list) {
            Assertions.assertEquals("x", s);
        }
    }

    @Test
    public void testFillListWithRange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        CommonUtil.fill(list, 1, 4, "x");
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertEquals("x", list.get(1));
        Assertions.assertEquals("x", list.get(2));
        Assertions.assertEquals("x", list.get(3));
        Assertions.assertEquals("e", list.get(4));
    }

    @Test
    public void testFillListExtending() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        CommonUtil.fill(list, 0, 5, "x");
        Assertions.assertEquals(5, list.size());
        for (String s : list) {
            Assertions.assertEquals("x", s);
        }
    }

    @Test
    public void testFillListWithGaps() {
        List<String> list = new ArrayList<>();
        CommonUtil.fill(list, 2, 5, "x");
        Assertions.assertEquals(5, list.size());
        Assertions.assertNull(list.get(0));
        Assertions.assertNull(list.get(1));
        Assertions.assertEquals("x", list.get(2));
        Assertions.assertEquals("x", list.get(3));
        Assertions.assertEquals("x", list.get(4));
    }

    @Test
    public void testPadLeftNoChange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        boolean result = CommonUtil.padLeft(list, 3, "x");
        Assertions.assertFalse(result);
        Assertions.assertEquals(3, list.size());
    }

    @Test
    public void testPadLeftWithNull() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        boolean result = CommonUtil.padLeft(list, 3, null);
        Assertions.assertTrue(result);
        Assertions.assertEquals(3, list.size());
        Assertions.assertNull(list.get(0));
        Assertions.assertNull(list.get(1));
        Assertions.assertEquals("a", list.get(2));
    }

    @Test
    public void testPadRightNoChange() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "c"));
        boolean result = CommonUtil.padRight(coll, 2, "x");
        Assertions.assertFalse(result);
        Assertions.assertEquals(3, coll.size());
    }

    @Test
    public void testPadRightWithNull() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a"));
        boolean result = CommonUtil.padRight(coll, 3, null);
        Assertions.assertTrue(result);
        Assertions.assertEquals(3, coll.size());
        List<String> list = new ArrayList<>(coll);
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertNull(list.get(1));
        Assertions.assertNull(list.get(2));
    }

    @Test
    public void testRepeat() {
        List<String> result = CommonUtil.repeat("x", 5);
        Assertions.assertEquals(5, result.size());
        for (String s : result) {
            Assertions.assertEquals("x", s);
        }
    }

    @Test
    public void testRepeatZero() {
        List<String> result = CommonUtil.repeat("x", 0);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testRepeatNull() {
        List<String> result = CommonUtil.repeat(null, 3);
        Assertions.assertEquals(3, result.size());
        for (String s : result) {
            Assertions.assertNull(s);
        }
    }

    @Test
    public void testRepeatElementsZero() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = CommonUtil.repeatElements(input, 0);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testRepeatElementsEmpty() {
        List<String> input = new ArrayList<>();
        List<String> result = CommonUtil.repeatElements(input, 3);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testRepeatCollectionZero() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = CommonUtil.cycle(input, 0);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testRepeatCollectionEmpty() {
        List<String> input = new ArrayList<>();
        List<String> result = CommonUtil.cycle(input, 3);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testRepeatElementsToSizeExact() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = CommonUtil.repeatElementsToSize(input, 6);
        Assertions.assertEquals(6, result.size());
        Assertions.assertEquals(Arrays.asList("a", "a", "b", "b", "c", "c"), result);
    }

    @Test
    public void testRepeatElementsToSizeZero() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = CommonUtil.repeatElementsToSize(input, 0);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testRepeatCollectionToSizeExact() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = CommonUtil.cycleToSize(input, 6);
        Assertions.assertEquals(6, result.size());
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "a", "b", "c"), result);
    }

    @Test
    public void testRepeatCollectionToSizeZero() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = CommonUtil.cycleToSize(input, 0);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testCopyBooleanArray() {
        boolean[] src = { true, false, true, false, true };
        boolean[] dest = new boolean[5];
        CommonUtil.copy(src, 1, dest, 2, 2);
        Assertions.assertFalse(dest[0]);
        Assertions.assertFalse(dest[1]);
        Assertions.assertFalse(dest[2]);
        Assertions.assertTrue(dest[3]);
        Assertions.assertFalse(dest[4]);
    }

    @Test
    public void testCopyBooleanArraySameArray() {
        boolean[] arr = { true, false, true, false, true };
        CommonUtil.copy(arr, 0, arr, 2, 3);
        Assertions.assertTrue(arr[0]);
        Assertions.assertFalse(arr[1]);
        Assertions.assertTrue(arr[2]);
        Assertions.assertFalse(arr[3]);
        Assertions.assertTrue(arr[4]);
    }

    @Test
    public void testCopyBooleanArrayEmpty() {
        boolean[] src = {};
        boolean[] dest = new boolean[5];
        CommonUtil.copy(src, 0, dest, 0, 0);
        assertNotNull(dest);
    }

    @Test
    public void testCopyCharArray() {
        char[] src = { 'a', 'b', 'c', 'd', 'e' };
        char[] dest = new char[5];
        CommonUtil.copy(src, 1, dest, 0, 3);
        Assertions.assertEquals('b', dest[0]);
        Assertions.assertEquals('c', dest[1]);
        Assertions.assertEquals('d', dest[2]);
        Assertions.assertEquals('\0', dest[3]);
        Assertions.assertEquals('\0', dest[4]);
    }

    @Test
    public void testCopyByteArray() {
        byte[] src = { 1, 2, 3, 4, 5 };
        byte[] dest = new byte[5];
        CommonUtil.copy(src, 2, dest, 1, 3);
        Assertions.assertEquals(0, dest[0]);
        Assertions.assertEquals(3, dest[1]);
        Assertions.assertEquals(4, dest[2]);
        Assertions.assertEquals(5, dest[3]);
        Assertions.assertEquals(0, dest[4]);
    }

    @Test
    public void testCopyShortArray() {
        short[] src = { 10, 20, 30, 40, 50 };
        short[] dest = new short[5];
        CommonUtil.copy(src, 0, dest, 2, 2);
        Assertions.assertEquals(0, dest[0]);
        Assertions.assertEquals(0, dest[1]);
        Assertions.assertEquals(10, dest[2]);
        Assertions.assertEquals(20, dest[3]);
        Assertions.assertEquals(0, dest[4]);
    }

    @Test
    public void testCopyIntArray() {
        int[] src = { 100, 200, 300, 400, 500 };
        int[] dest = new int[5];
        CommonUtil.copy(src, 1, dest, 1, 3);
        Assertions.assertEquals(0, dest[0]);
        Assertions.assertEquals(200, dest[1]);
        Assertions.assertEquals(300, dest[2]);
        Assertions.assertEquals(400, dest[3]);
        Assertions.assertEquals(0, dest[4]);
    }

    @Test
    public void testCopyLongArray() {
        long[] src = { 1000L, 2000L, 3000L, 4000L, 5000L };
        long[] dest = new long[5];
        CommonUtil.copy(src, 2, dest, 0, 3);
        Assertions.assertEquals(3000L, dest[0]);
        Assertions.assertEquals(4000L, dest[1]);
        Assertions.assertEquals(5000L, dest[2]);
        Assertions.assertEquals(0L, dest[3]);
        Assertions.assertEquals(0L, dest[4]);
    }

    @Test
    public void testCopyFloatArray() {
        float[] src = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] dest = new float[5];
        CommonUtil.copy(src, 0, dest, 0, 5);
        Assertions.assertArrayEquals(src, dest);
    }

    @Test
    public void testCopyDoubleArray() {
        double[] src = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] dest = new double[5];
        CommonUtil.copy(src, 3, dest, 3, 2);
        Assertions.assertEquals(0.0, dest[0]);
        Assertions.assertEquals(0.0, dest[1]);
        Assertions.assertEquals(0.0, dest[2]);
        Assertions.assertEquals(4.0, dest[3]);
        Assertions.assertEquals(5.0, dest[4]);
    }

    @Test
    public void testCopyObjectArray() {
        String[] src = { "a", "b", "c", "d", "e" };
        String[] dest = new String[5];
        CommonUtil.copy(src, 1, dest, 2, 2);
        Assertions.assertNull(dest[0]);
        Assertions.assertNull(dest[1]);
        Assertions.assertEquals("b", dest[2]);
        Assertions.assertEquals("c", dest[3]);
        Assertions.assertNull(dest[4]);
    }

    @Test
    public void testCopyGenericObject() {
        int[] src = { 1, 2, 3, 4, 5 };
        int[] dest = new int[5];
        CommonUtil.copy((Object) src, 0, (Object) dest, 1, 4);
        Assertions.assertEquals(0, dest[0]);
        Assertions.assertEquals(1, dest[1]);
        Assertions.assertEquals(2, dest[2]);
        Assertions.assertEquals(3, dest[3]);
        Assertions.assertEquals(4, dest[4]);
    }

    @Test
    public void testCopyOfBooleanArray() {
        boolean[] original = { true, false, true };
        boolean[] copy = CommonUtil.copyOf(original, 5);
        Assertions.assertEquals(5, copy.length);
        Assertions.assertTrue(copy[0]);
        Assertions.assertFalse(copy[1]);
        Assertions.assertTrue(copy[2]);
        Assertions.assertFalse(copy[3]);
        Assertions.assertFalse(copy[4]);
    }

    @Test
    public void testCopyOfBooleanArrayTruncate() {
        boolean[] original = { true, false, true, false, true };
        boolean[] copy = CommonUtil.copyOf(original, 3);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertTrue(copy[0]);
        Assertions.assertFalse(copy[1]);
        Assertions.assertTrue(copy[2]);
    }

    @Test
    public void testCopyOfBooleanArraySameLength() {
        boolean[] original = { true, false, true };
        boolean[] copy = CommonUtil.copyOf(original, 3);
        Assertions.assertArrayEquals(original, copy);
        Assertions.assertNotSame(original, copy);
    }

    @Test
    public void testCopyOfCharArray() {
        char[] original = { 'a', 'b', 'c' };
        char[] copy = CommonUtil.copyOf(original, 5);
        Assertions.assertEquals(5, copy.length);
        Assertions.assertEquals('a', copy[0]);
        Assertions.assertEquals('b', copy[1]);
        Assertions.assertEquals('c', copy[2]);
        Assertions.assertEquals('\0', copy[3]);
        Assertions.assertEquals('\0', copy[4]);
    }

    @Test
    public void testCopyOfByteArray() {
        byte[] original = { 1, 2, 3 };
        byte[] copy = CommonUtil.copyOf(original, 2);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(1, copy[0]);
        Assertions.assertEquals(2, copy[1]);
    }

    @Test
    public void testCopyOfShortArray() {
        short[] original = { 10, 20, 30 };
        short[] copy = CommonUtil.copyOf(original, 4);
        Assertions.assertEquals(4, copy.length);
        Assertions.assertEquals(10, copy[0]);
        Assertions.assertEquals(20, copy[1]);
        Assertions.assertEquals(30, copy[2]);
        Assertions.assertEquals(0, copy[3]);
    }

    @Test
    public void testCopyOfIntArray() {
        int[] original = { 100, 200, 300 };
        int[] copy = CommonUtil.copyOf(original, 3);
        Assertions.assertArrayEquals(original, copy);
        Assertions.assertNotSame(original, copy);
    }

    @Test
    public void testCopyOfLongArray() {
        long[] original = { 1000L, 2000L, 3000L };
        long[] copy = CommonUtil.copyOf(original, 5);
        Assertions.assertEquals(5, copy.length);
        Assertions.assertEquals(1000L, copy[0]);
        Assertions.assertEquals(2000L, copy[1]);
        Assertions.assertEquals(3000L, copy[2]);
        Assertions.assertEquals(0L, copy[3]);
        Assertions.assertEquals(0L, copy[4]);
    }

    @Test
    public void testCopyOfFloatArray() {
        float[] original = { 1.0f, 2.0f, 3.0f };
        float[] copy = CommonUtil.copyOf(original, 2);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(1.0f, copy[0]);
        Assertions.assertEquals(2.0f, copy[1]);
    }

    @Test
    public void testCopyOfDoubleArray() {
        double[] original = { 1.0, 2.0, 3.0 };
        double[] copy = CommonUtil.copyOf(original, 4);
        Assertions.assertEquals(4, copy.length);
        Assertions.assertEquals(1.0, copy[0]);
        Assertions.assertEquals(2.0, copy[1]);
        Assertions.assertEquals(3.0, copy[2]);
        Assertions.assertEquals(0.0, copy[3]);
    }

    @Test
    public void testCopyOfObjectArray() {
        String[] original = { "a", "b", "c" };
        String[] copy = CommonUtil.copyOf(original, 5);
        Assertions.assertEquals(5, copy.length);
        Assertions.assertEquals("a", copy[0]);
        Assertions.assertEquals("b", copy[1]);
        Assertions.assertEquals("c", copy[2]);
        Assertions.assertNull(copy[3]);
        Assertions.assertNull(copy[4]);
    }

    @Test
    public void testCopyOfObjectArrayWithType() {
        Number[] original = { 1, 2, 3 };
        Integer[] copy = CommonUtil.copyOf(original, 4, Integer[].class);
        Assertions.assertEquals(4, copy.length);
        Assertions.assertEquals(1, copy[0]);
        Assertions.assertEquals(2, copy[1]);
        Assertions.assertEquals(3, copy[2]);
        Assertions.assertNull(copy[3]);
    }

    @Test
    public void testCopyOfRangeBooleanArray() {
        boolean[] original = { true, false, true, false, true };
        boolean[] copy = CommonUtil.copyOfRange(original, 1, 4);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertFalse(copy[0]);
        Assertions.assertTrue(copy[1]);
        Assertions.assertFalse(copy[2]);
    }

    @Test
    public void testCopyOfRangeBooleanArrayFullRange() {
        boolean[] original = { true, false, true };
        boolean[] copy = CommonUtil.copyOfRange(original, 0, 3);
        Assertions.assertArrayEquals(original, copy);
        Assertions.assertNotSame(original, copy);
    }

    @Test
    public void testCopyOfRangeBooleanArrayWithStep() {
        boolean[] original = { true, false, true, false, true, false };
        boolean[] copy = CommonUtil.copyOfRange(original, 0, 6, 2);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertTrue(copy[0]);
        Assertions.assertTrue(copy[1]);
        Assertions.assertTrue(copy[2]);
    }

    @Test
    public void testCopyOfRangeBooleanArrayWithNegativeStep() {
        boolean[] original = { true, false, true, false, true };
        boolean[] copy = CommonUtil.copyOfRange(original, 4, 1, -1);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertTrue(copy[0]);
        Assertions.assertFalse(copy[1]);
        Assertions.assertTrue(copy[2]);
    }

    @Test
    public void testCopyOfRangeBooleanArrayWithNegativeStepToBeginning() {
        boolean[] original = { true, false, true, false, true };
        boolean[] copy = CommonUtil.copyOfRange(original, 4, -1, -1);
        Assertions.assertEquals(5, copy.length);
        Assertions.assertTrue(copy[0]);
        Assertions.assertFalse(copy[1]);
        Assertions.assertTrue(copy[2]);
        Assertions.assertFalse(copy[3]);
        Assertions.assertTrue(copy[4]);
    }

    @Test
    public void testCopyOfRangeCharArray() {
        char[] original = { 'a', 'b', 'c', 'd', 'e' };
        char[] copy = CommonUtil.copyOfRange(original, 1, 4);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals('b', copy[0]);
        Assertions.assertEquals('c', copy[1]);
        Assertions.assertEquals('d', copy[2]);
    }

    @Test
    public void testCopyOfRangeCharArrayWithStep() {
        char[] original = { 'a', 'b', 'c', 'd', 'e', 'f' };
        char[] copy = CommonUtil.copyOfRange(original, 1, 6, 2);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals('b', copy[0]);
        Assertions.assertEquals('d', copy[1]);
        Assertions.assertEquals('f', copy[2]);
    }

    @Test
    public void testCopyOfRangeByteArray() {
        byte[] original = { 1, 2, 3, 4, 5 };
        byte[] copy = CommonUtil.copyOfRange(original, 2, 5);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals(3, copy[0]);
        Assertions.assertEquals(4, copy[1]);
        Assertions.assertEquals(5, copy[2]);
    }

    @Test
    public void testCopyOfRangeByteArrayWithStep() {
        byte[] original = { 1, 2, 3, 4, 5, 6 };
        byte[] copy = CommonUtil.copyOfRange(original, 0, 5, 2);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals(1, copy[0]);
        Assertions.assertEquals(3, copy[1]);
        Assertions.assertEquals(5, copy[2]);
    }

    @Test
    public void testCopyOfRangeShortArray() {
        short[] original = { 10, 20, 30, 40, 50 };
        short[] copy = CommonUtil.copyOfRange(original, 0, 3);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals(10, copy[0]);
        Assertions.assertEquals(20, copy[1]);
        Assertions.assertEquals(30, copy[2]);
    }

    @Test
    public void testCopyOfRangeShortArrayWithStep() {
        short[] original = { 10, 20, 30, 40, 50, 60 };
        short[] copy = CommonUtil.copyOfRange(original, 1, 5, 2);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(20, copy[0]);
        Assertions.assertEquals(40, copy[1]);
    }

    @Test
    public void testCopyOfRangeIntArray() {
        int[] original = { 100, 200, 300, 400, 500 };
        int[] copy = CommonUtil.copyOfRange(original, 1, 3);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(200, copy[0]);
        Assertions.assertEquals(300, copy[1]);
    }

    @Test
    public void testCopyOfRangeIntArrayWithStep() {
        int[] original = { 100, 200, 300, 400, 500, 600 };
        int[] copy = CommonUtil.copyOfRange(original, 0, 6, 3);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(100, copy[0]);
        Assertions.assertEquals(400, copy[1]);
    }

    @Test
    public void testCopyOfRangeLongArray() {
        long[] original = { 1000L, 2000L, 3000L, 4000L, 5000L };
        long[] copy = CommonUtil.copyOfRange(original, 2, 4);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(3000L, copy[0]);
        Assertions.assertEquals(4000L, copy[1]);
    }

    @Test
    public void testCopyOfRangeLongArrayWithStep() {
        long[] original = { 1000L, 2000L, 3000L, 4000L, 5000L };
        long[] copy = CommonUtil.copyOfRange(original, 4, 0, -2);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(5000L, copy[0]);
        Assertions.assertEquals(3000L, copy[1]);
    }

    @Test
    public void testCopyOfRangeFloatArray() {
        float[] original = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] copy = CommonUtil.copyOfRange(original, 3, 5);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(4.0f, copy[0]);
        Assertions.assertEquals(5.0f, copy[1]);
    }

    @Test
    public void testCopyOfRangeFloatArrayWithStep() {
        float[] original = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f };
        float[] copy = CommonUtil.copyOfRange(original, 0, 6, 2);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals(1.0f, copy[0]);
        Assertions.assertEquals(3.0f, copy[1]);
        Assertions.assertEquals(5.0f, copy[2]);
    }

    @Test
    public void testCopyOfRangeDoubleArray() {
        double[] original = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] copy = CommonUtil.copyOfRange(original, 0, 2);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(1.0, copy[0]);
        Assertions.assertEquals(2.0, copy[1]);
    }

    @Test
    public void testCopyOfRangeDoubleArrayWithStep() {
        double[] original = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] copy = CommonUtil.copyOfRange(original, 1, 5, 2);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(2.0, copy[0]);
        Assertions.assertEquals(4.0, copy[1]);
    }

    @Test
    public void testCopyOfRangeObjectArray() {
        String[] original = { "a", "b", "c", "d", "e" };
        String[] copy = CommonUtil.copyOfRange(original, 1, 4);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals("b", copy[0]);
        Assertions.assertEquals("c", copy[1]);
        Assertions.assertEquals("d", copy[2]);
    }

    @Test
    public void testCopyOfRangeObjectArrayWithStep() {
        String[] original = { "a", "b", "c", "d", "e", "f" };
        String[] copy = CommonUtil.copyOfRange(original, 0, 6, 2);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals("a", copy[0]);
        Assertions.assertEquals("c", copy[1]);
        Assertions.assertEquals("e", copy[2]);
    }

    @Test
    public void testCopyOfRangeObjectArrayWithType() {
        Number[] original = { 1, 2, 3, 4, 5 };
        Integer[] copy = CommonUtil.copyOfRange(original, 1, 4, Integer[].class);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals(2, copy[0]);
        Assertions.assertEquals(3, copy[1]);
        Assertions.assertEquals(4, copy[2]);
    }

    @Test
    public void testCopyOfRangeObjectArrayWithTypeAndStep() {
        Number[] original = { 1, 2, 3, 4, 5, 6 };
        Integer[] copy = CommonUtil.copyOfRange(original, 0, 6, 3, Integer[].class);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(1, copy[0]);
        Assertions.assertEquals(4, copy[1]);
    }

    @Test
    public void testCopyOfRangeList() {
        List<String> original = Arrays.asList("a", "b", "c", "d", "e");
        List<String> copy = CommonUtil.copyOfRange(original, 1, 4);
        Assertions.assertEquals(3, copy.size());
        Assertions.assertEquals("b", copy.get(0));
        Assertions.assertEquals("c", copy.get(1));
        Assertions.assertEquals("d", copy.get(2));
    }

    @Test
    public void testCopyOfRangeListWithStep() {
        List<String> original = Arrays.asList("a", "b", "c", "d", "e", "f");
        List<String> copy = CommonUtil.copyOfRange(original, 0, 5, 2);
        Assertions.assertEquals(3, copy.size());
        Assertions.assertEquals("a", copy.get(0));
        Assertions.assertEquals("c", copy.get(1));
        Assertions.assertEquals("e", copy.get(2));
    }

    @Test
    public void testCopyOfRangeString() {
        String original = "abcde";
        String copy = CommonUtil.copyOfRange(original, 1, 4);
        Assertions.assertEquals("bcd", copy);
    }

    @Test
    public void testCopyOfRangeStringFullRange() {
        String original = "abcde";
        String copy = CommonUtil.copyOfRange(original, 0, 5);
        Assertions.assertEquals(original, copy);
    }

    @Test
    public void testCopyOfRangeStringWithStep() {
        String original = "abcdef";
        String copy = CommonUtil.copyOfRange(original, 0, 6, 2);
        Assertions.assertEquals("ace", copy);
    }

    @Test
    public void testCloneBooleanArray() {
        boolean[] original = { true, false, true };
        boolean[] cloned = CommonUtil.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    @Test
    public void testCloneBooleanArrayNull() {
        boolean[] original = null;
        boolean[] cloned = CommonUtil.clone(original);
        Assertions.assertNull(cloned);
    }

    @Test
    public void testCloneCharArray() {
        char[] original = { 'a', 'b', 'c' };
        char[] cloned = CommonUtil.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    @Test
    public void testCloneByteArray() {
        byte[] original = { 1, 2, 3 };
        byte[] cloned = CommonUtil.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    @Test
    public void testCloneShortArray() {
        short[] original = { 10, 20, 30 };
        short[] cloned = CommonUtil.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    @Test
    public void testCloneIntArray() {
        int[] original = { 100, 200, 300 };
        int[] cloned = CommonUtil.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    @Test
    public void testCloneLongArray() {
        long[] original = { 1000L, 2000L, 3000L };
        long[] cloned = CommonUtil.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    @Test
    public void testCloneFloatArray() {
        float[] original = { 1.0f, 2.0f, 3.0f };
        float[] cloned = CommonUtil.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    @Test
    public void testCloneDoubleArray() {
        double[] original = { 1.0, 2.0, 3.0 };
        double[] cloned = CommonUtil.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    @Test
    public void testCloneObjectArray() {
        String[] original = { "a", "b", "c" };
        String[] cloned = CommonUtil.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    @Test
    public void testClone2DBoolean() {
        assertNull(CommonUtil.clone((boolean[][]) null));

        final boolean[][] a = new boolean[][] { { true, false }, { false, true } };
        final boolean[][] cloneA = CommonUtil.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = false;
        assertTrue(a[0][0]);

        final boolean[][] b = new boolean[][] { { true, false }, null };
        final boolean[][] cloneB = CommonUtil.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final boolean[][] c = new boolean[0][0];
        final boolean[][] cloneC = CommonUtil.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DChar() {
        assertNull(CommonUtil.clone((char[][]) null));

        final char[][] a = new char[][] { { 'a', 'b' }, { 'c', 'd' } };
        final char[][] cloneA = CommonUtil.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 'z';
        assertEquals('a', a[0][0]);

        final char[][] b = new char[][] { { 'a', 'b' }, null };
        final char[][] cloneB = CommonUtil.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final char[][] c = new char[0][0];
        final char[][] cloneC = CommonUtil.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DByte() {
        assertNull(CommonUtil.clone((byte[][]) null));

        final byte[][] a = new byte[][] { { 1, 2 }, { 3, 4 } };
        final byte[][] cloneA = CommonUtil.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 9;
        assertEquals(1, a[0][0]);

        final byte[][] b = new byte[][] { { 1, 2 }, null };
        final byte[][] cloneB = CommonUtil.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final byte[][] c = new byte[0][0];
        final byte[][] cloneC = CommonUtil.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DShort() {
        assertNull(CommonUtil.clone((short[][]) null));

        final short[][] a = new short[][] { { 1, 2 }, { 3, 4 } };
        final short[][] cloneA = CommonUtil.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 9;
        assertEquals(1, a[0][0]);

        final short[][] b = new short[][] { { 1, 2 }, null };
        final short[][] cloneB = CommonUtil.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final short[][] c = new short[0][0];
        final short[][] cloneC = CommonUtil.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DInt() {
        assertNull(CommonUtil.clone((int[][]) null));

        final int[][] a = new int[][] { { 1, 2 }, { 3, 4 } };
        final int[][] cloneA = CommonUtil.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 9;
        assertEquals(1, a[0][0]);

        final int[][] b = new int[][] { { 1, 2 }, null };
        final int[][] cloneB = CommonUtil.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final int[][] c = new int[0][0];
        final int[][] cloneC = CommonUtil.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DLong() {
        assertNull(CommonUtil.clone((long[][]) null));

        final long[][] a = new long[][] { { 1L, 2L }, { 3L, 4L } };
        final long[][] cloneA = CommonUtil.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 9L;
        assertEquals(1L, a[0][0]);

        final long[][] b = new long[][] { { 1L, 2L }, null };
        final long[][] cloneB = CommonUtil.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final long[][] c = new long[0][0];
        final long[][] cloneC = CommonUtil.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DFloat() {
        assertNull(CommonUtil.clone((float[][]) null));

        final float[][] a = new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
        final float[][] cloneA = CommonUtil.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 9.0f;
        assertEquals(1.0f, a[0][0], 0.000001);

        final float[][] b = new float[][] { { 1.0f, 2.0f }, null };
        final float[][] cloneB = CommonUtil.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final float[][] c = new float[0][0];
        final float[][] cloneC = CommonUtil.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DDouble() {
        assertNull(CommonUtil.clone((double[][]) null));

        final double[][] a = new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } };
        final double[][] cloneA = CommonUtil.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 9.0;
        assertEquals(1.0, a[0][0], 0.000001);

        final double[][] b = new double[][] { { 1.0, 2.0 }, null };
        final double[][] cloneB = CommonUtil.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final double[][] c = new double[0][0];
        final double[][] cloneC = CommonUtil.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DGeneric() {
        assertNull(CommonUtil.clone((String[][]) null));

        final String[][] a = new String[][] { { "a", "b" }, { "c", "d" } };
        final String[][] cloneA = CommonUtil.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = "z";
        assertEquals("a", a[0][0]);

        final String[][] b = new String[][] { { "a", "b" }, null };
        final String[][] cloneB = CommonUtil.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final String[][] c = new String[0][0];
        final String[][] cloneC = CommonUtil.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone3DBoolean() {
        assertNull(CommonUtil.clone((boolean[][][]) null));

        final boolean[][][] a = new boolean[][][] { { { true, false } }, { { false, true } } };
        final boolean[][][] cloneA = CommonUtil.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = false;
        assertTrue(a[0][0][0]);

        final boolean[][][] b = new boolean[][][] { { { true, false } }, null };
        final boolean[][][] cloneB = CommonUtil.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final boolean[][][] c = new boolean[0][0][0];
        final boolean[][][] cloneC = CommonUtil.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DChar() {
        assertNull(CommonUtil.clone((char[][][]) null));

        final char[][][] a = new char[][][] { { { 'a', 'b' } }, { { 'c', 'd' } } };
        final char[][][] cloneA = CommonUtil.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 'z';
        assertEquals('a', a[0][0][0]);

        final char[][][] b = new char[][][] { { { 'a', 'b' } }, null };
        final char[][][] cloneB = CommonUtil.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final char[][][] c = new char[0][0][0];
        final char[][][] cloneC = CommonUtil.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DByte() {
        assertNull(CommonUtil.clone((byte[][][]) null));

        final byte[][][] a = new byte[][][] { { { 1, 2 } }, { { 3, 4 } } };
        final byte[][][] cloneA = CommonUtil.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 9;
        assertEquals(1, a[0][0][0]);

        final byte[][][] b = new byte[][][] { { { 1, 2 } }, null };
        final byte[][][] cloneB = CommonUtil.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final byte[][][] c = new byte[0][0][0];
        final byte[][][] cloneC = CommonUtil.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DShort() {
        assertNull(CommonUtil.clone((short[][][]) null));

        final short[][][] a = new short[][][] { { { 1, 2 } }, { { 3, 4 } } };
        final short[][][] cloneA = CommonUtil.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 9;
        assertEquals(1, a[0][0][0]);

        final short[][][] b = new short[][][] { { { 1, 2 } }, null };
        final short[][][] cloneB = CommonUtil.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final short[][][] c = new short[0][0][0];
        final short[][][] cloneC = CommonUtil.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DInt() {
        assertNull(CommonUtil.clone((int[][][]) null));

        final int[][][] a = new int[][][] { { { 1, 2 } }, { { 3, 4 } } };
        final int[][][] cloneA = CommonUtil.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 9;
        assertEquals(1, a[0][0][0]);

        final int[][][] b = new int[][][] { { { 1, 2 } }, null };
        final int[][][] cloneB = CommonUtil.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final int[][][] c = new int[0][0][0];
        final int[][][] cloneC = CommonUtil.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DLong() {
        assertNull(CommonUtil.clone((long[][][]) null));

        final long[][][] a = new long[][][] { { { 1L, 2L } }, { { 3L, 4L } } };
        final long[][][] cloneA = CommonUtil.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 9L;
        assertEquals(1L, a[0][0][0]);

        final long[][][] b = new long[][][] { { { 1L, 2L } }, null };
        final long[][][] cloneB = CommonUtil.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final long[][][] c = new long[0][0][0];
        final long[][][] cloneC = CommonUtil.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DFloat() {
        assertNull(CommonUtil.clone((float[][][]) null));

        final float[][][] a = new float[][][] { { { 1.0f, 2.0f } }, { { 3.0f, 4.0f } } };
        final float[][][] cloneA = CommonUtil.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 9.0f;
        assertEquals(1.0f, a[0][0][0], 0.000001);

        final float[][][] b = new float[][][] { { { 1.0f, 2.0f } }, null };
        final float[][][] cloneB = CommonUtil.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final float[][][] c = new float[0][0][0];
        final float[][][] cloneC = CommonUtil.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DDouble() {
        assertNull(CommonUtil.clone((double[][][]) null));

        final double[][][] a = new double[][][] { { { 1.0, 2.0 } }, { { 3.0, 4.0 } } };
        final double[][][] cloneA = CommonUtil.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 9.0;
        assertEquals(1.0, a[0][0][0], 0.000001);

        final double[][][] b = new double[][][] { { { 1.0, 2.0 } }, null };
        final double[][][] cloneB = CommonUtil.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final double[][][] c = new double[0][0][0];
        final double[][][] cloneC = CommonUtil.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DGeneric() {
        assertNull(CommonUtil.clone((String[][][]) null));

        final String[][][] a = new String[][][] { { { "a", "b" } }, { { "c", "d" } } };
        final String[][][] cloneA = CommonUtil.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = "z";
        assertEquals("a", a[0][0][0]);

        final String[][][] b = new String[][][] { { { "a", "b" } }, null };
        final String[][][] cloneB = CommonUtil.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final String[][][] c = new String[0][0][0];
        final String[][][] cloneC = CommonUtil.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testFindLastNonNullArray() {
        String[] array = { null, "a", null, "b", "c", null };

        Assertions.assertEquals("b", CommonUtil.findLastNonNull(array, s -> s.startsWith("b")).orElse(null));
        Assertions.assertEquals("c", CommonUtil.findLastNonNull(array, s -> s.startsWith("c")).orElse(null));
        Assertions.assertFalse(CommonUtil.findLastNonNull(array, s -> s.equals("x")).isPresent());
        Assertions.assertFalse(CommonUtil.findLastNonNull((String[]) null, s -> true).isPresent());

        String[] allNull = { null, null, null };
        Assertions.assertFalse(CommonUtil.findLastNonNull(allNull, s -> true).isPresent());
    }

    @Test
    public void testFindLastNonNullIterable() {
        List<String> list = Arrays.asList(null, "a", null, "b", "c", null);

        Assertions.assertEquals("b", CommonUtil.findLastNonNull(list, s -> s.startsWith("b")).orElse(null));
        Assertions.assertEquals("c", CommonUtil.findLastNonNull(list, s -> s.startsWith("c")).orElse(null));
        Assertions.assertFalse(CommonUtil.findLastNonNull(list, s -> s.equals("x")).isPresent());
        Assertions.assertFalse(CommonUtil.findLastNonNull((Iterable<String>) null, s -> true).isPresent());

        List<String> allNull = Arrays.asList(null, null, null);
        Assertions.assertFalse(CommonUtil.findLastNonNull(allNull, s -> true).isPresent());
    }

    @Test
    public void testHaveSameElementsBoolean() {
        boolean[] a = { true, false, true, false };
        boolean[] b = { false, true, false, true };
        boolean[] c = { true, true, false, false };
        boolean[] d = { true, false, true };

        Assertions.assertTrue(CommonUtil.containsSameElements(a, b));
        Assertions.assertTrue(CommonUtil.containsSameElements(a, c));
        Assertions.assertFalse(CommonUtil.containsSameElements(a, d));
        Assertions.assertTrue(CommonUtil.containsSameElements((boolean[]) null, (boolean[]) null));
        Assertions.assertTrue(CommonUtil.containsSameElements(new boolean[0], new boolean[0]));
        Assertions.assertFalse(CommonUtil.containsSameElements(a, null));
        Assertions.assertFalse(CommonUtil.containsSameElements(null, a));

        Assertions.assertTrue(CommonUtil.containsSameElements(a, a));
    }

    @Test
    public void testHaveSameElementsChar() {
        char[] a = { 'a', 'b', 'c', 'b' };
        char[] b = { 'b', 'a', 'c', 'b' };
        char[] c = { 'a', 'b', 'b', 'c' };
        char[] d = { 'a', 'b', 'c' };

        Assertions.assertTrue(CommonUtil.containsSameElements(a, b));
        Assertions.assertTrue(CommonUtil.containsSameElements(a, c));
        Assertions.assertFalse(CommonUtil.containsSameElements(a, d));
        Assertions.assertTrue(CommonUtil.containsSameElements((char[]) null, (char[]) null));
        Assertions.assertTrue(CommonUtil.containsSameElements(new char[0], new char[0]));
    }

    @Test
    public void testHaveSameElementsByte() {
        byte[] a = { 1, 2, 3, 2 };
        byte[] b = { 2, 1, 3, 2 };
        byte[] c = { 1, 2, 2, 3 };
        byte[] d = { 1, 2, 3 };

        Assertions.assertTrue(CommonUtil.containsSameElements(a, b));
        Assertions.assertTrue(CommonUtil.containsSameElements(a, c));
        Assertions.assertFalse(CommonUtil.containsSameElements(a, d));
        Assertions.assertTrue(CommonUtil.containsSameElements((byte[]) null, (byte[]) null));
        Assertions.assertTrue(CommonUtil.containsSameElements(new byte[0], new byte[0]));
    }

    @Test
    public void testHaveSameElementsShort() {
        short[] a = { 1, 2, 3, 2 };
        short[] b = { 2, 1, 3, 2 };
        short[] c = { 1, 2, 2, 3 };
        short[] d = { 1, 2, 3 };

        Assertions.assertTrue(CommonUtil.containsSameElements(a, b));
        Assertions.assertTrue(CommonUtil.containsSameElements(a, c));
        Assertions.assertFalse(CommonUtil.containsSameElements(a, d));
        Assertions.assertTrue(CommonUtil.containsSameElements((short[]) null, (short[]) null));
        Assertions.assertTrue(CommonUtil.containsSameElements(new short[0], new short[0]));
    }

    @Test
    public void testHaveSameElementsInt() {
        int[] a = { 1, 2, 3, 2 };
        int[] b = { 2, 1, 3, 2 };
        int[] c = { 1, 2, 2, 3 };
        int[] d = { 1, 2, 3 };
        int[] e = { 1, 2, 2, 2 };

        Assertions.assertTrue(CommonUtil.containsSameElements(a, b));
        Assertions.assertTrue(CommonUtil.containsSameElements(a, c));
        Assertions.assertFalse(CommonUtil.containsSameElements(a, d));
        Assertions.assertFalse(CommonUtil.containsSameElements(a, e));
        Assertions.assertTrue(CommonUtil.containsSameElements((int[]) null, (int[]) null));
        Assertions.assertTrue(CommonUtil.containsSameElements(new int[0], new int[0]));
    }

    @Test
    public void testHaveSameElementsLong() {
        long[] a = { 1L, 2L, 3L, 2L };
        long[] b = { 2L, 1L, 3L, 2L };
        long[] c = { 1L, 2L, 2L, 3L };
        long[] d = { 1L, 2L, 3L };

        Assertions.assertTrue(CommonUtil.containsSameElements(a, b));
        Assertions.assertTrue(CommonUtil.containsSameElements(a, c));
        Assertions.assertFalse(CommonUtil.containsSameElements(a, d));
        Assertions.assertTrue(CommonUtil.containsSameElements((long[]) null, (long[]) null));
        Assertions.assertTrue(CommonUtil.containsSameElements(new long[0], new long[0]));
    }

    @Test
    public void testHaveSameElementsFloat() {
        float[] a = { 1.0f, 2.0f, 3.0f, 2.0f };
        float[] b = { 2.0f, 1.0f, 3.0f, 2.0f };
        float[] c = { 1.0f, 2.0f, 2.0f, 3.0f };
        float[] d = { 1.0f, 2.0f, 3.0f };

        Assertions.assertTrue(CommonUtil.containsSameElements(a, b));
        Assertions.assertTrue(CommonUtil.containsSameElements(a, c));
        Assertions.assertFalse(CommonUtil.containsSameElements(a, d));
        Assertions.assertTrue(CommonUtil.containsSameElements((float[]) null, (float[]) null));
        Assertions.assertTrue(CommonUtil.containsSameElements(new float[0], new float[0]));
    }

    @Test
    public void testHaveSameElementsDouble() {
        double[] a = { 1.0, 2.0, 3.0, 2.0 };
        double[] b = { 2.0, 1.0, 3.0, 2.0 };
        double[] c = { 1.0, 2.0, 2.0, 3.0 };
        double[] d = { 1.0, 2.0, 3.0 };

        Assertions.assertTrue(CommonUtil.containsSameElements(a, b));
        Assertions.assertTrue(CommonUtil.containsSameElements(a, c));
        Assertions.assertFalse(CommonUtil.containsSameElements(a, d));
        Assertions.assertTrue(CommonUtil.containsSameElements((double[]) null, (double[]) null));
        Assertions.assertTrue(CommonUtil.containsSameElements(new double[0], new double[0]));
    }

    @Test
    public void testHaveSameElementsObject() {
        String[] a = { "a", "b", "c", "b" };
        String[] b = { "b", "a", "c", "b" };
        String[] c = { "a", "b", "b", "c" };
        String[] d = { "a", "b", "c" };

        Assertions.assertTrue(CommonUtil.containsSameElements(a, b));
        Assertions.assertTrue(CommonUtil.containsSameElements(a, c));
        Assertions.assertFalse(CommonUtil.containsSameElements(a, d));
        Assertions.assertTrue(CommonUtil.containsSameElements((Object[]) null, (Object[]) null));
        Assertions.assertTrue(CommonUtil.containsSameElements(new Object[0], new Object[0]));

        String[] withNulls1 = { "a", null, "b", null };
        String[] withNulls2 = { null, "a", null, "b" };
        Assertions.assertTrue(CommonUtil.containsSameElements(withNulls1, withNulls2));
    }

    @Test
    public void testHaveSameElementsCollection() {
        List<String> a = Arrays.asList("a", "b", "c", "b");
        List<String> b = Arrays.asList("b", "a", "c", "b");
        List<String> c = Arrays.asList("a", "b", "b", "c");
        List<String> d = Arrays.asList("a", "b", "c");

        Assertions.assertTrue(CommonUtil.containsSameElements(a, b));
        Assertions.assertTrue(CommonUtil.containsSameElements(a, c));
        Assertions.assertFalse(CommonUtil.containsSameElements(a, d));
        Assertions.assertTrue(CommonUtil.containsSameElements((Collection<?>) null, (Collection<?>) null));
        Assertions.assertTrue(CommonUtil.containsSameElements(new ArrayList<>(), new ArrayList<>()));

        Assertions.assertTrue(CommonUtil.containsSameElements(a, a));
    }

    @Test
    public void testMismatchBoolean() {
        boolean[] a = { true, false, true, false };
        boolean[] b = { true, false, true, false };
        boolean[] c = { true, true, false, false };
        boolean[] d = { true, false, true };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c));
        Assertions.assertEquals(3, CommonUtil.mismatch(a, d));
        Assertions.assertEquals(-1, CommonUtil.mismatch((boolean[]) null, (boolean[]) null));
        Assertions.assertEquals(-1, CommonUtil.mismatch(new boolean[0], new boolean[0]));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, null));
        Assertions.assertEquals(0, CommonUtil.mismatch(null, a));

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatchBooleanRange() {
        boolean[] a = { true, false, true, false };
        boolean[] b = { false, false, true, true };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.mismatch(a, 0, b, 0, -1));

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, a, 1, 2));

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 0, b, 0, 0));
    }

    @Test
    public void testMismatchChar() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] b = { 'a', 'b', 'c', 'd' };
        char[] c = { 'a', 'x', 'c', 'd' };
        char[] d = { 'a', 'b', 'c' };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c));
        Assertions.assertEquals(3, CommonUtil.mismatch(a, d));
        Assertions.assertEquals(-1, CommonUtil.mismatch((char[]) null, (char[]) null));
        Assertions.assertEquals(-1, CommonUtil.mismatch(new char[0], new char[0]));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, null));
        Assertions.assertEquals(0, CommonUtil.mismatch(null, a));
    }

    @Test
    public void testMismatchCharRange() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] b = { 'x', 'b', 'c', 'y' };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchByte() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] b = { 1, 2, 3, 4 };
        byte[] c = { 1, 5, 3, 4 };
        byte[] d = { 1, 2, 3 };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c));
        Assertions.assertEquals(3, CommonUtil.mismatch(a, d));
        Assertions.assertEquals(-1, CommonUtil.mismatch((byte[]) null, (byte[]) null));
        Assertions.assertEquals(-1, CommonUtil.mismatch(new byte[0], new byte[0]));
    }

    @Test
    public void testMismatchByteRange() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] b = { 5, 2, 3, 6 };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchShort() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 1, 2, 3, 4 };
        short[] c = { 1, 5, 3, 4 };
        short[] d = { 1, 2, 3 };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c));
        Assertions.assertEquals(3, CommonUtil.mismatch(a, d));
        Assertions.assertEquals(-1, CommonUtil.mismatch((short[]) null, (short[]) null));
        Assertions.assertEquals(-1, CommonUtil.mismatch(new short[0], new short[0]));
    }

    @Test
    public void testMismatchShortRange() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 5, 2, 3, 6 };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchInt() {
        int[] a = { 1, 2, 3, 4 };
        int[] b = { 1, 2, 3, 4 };
        int[] c = { 1, 5, 3, 4 };
        int[] d = { 1, 2, 3 };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c));
        Assertions.assertEquals(3, CommonUtil.mismatch(a, d));
        Assertions.assertEquals(-1, CommonUtil.mismatch((int[]) null, (int[]) null));
        Assertions.assertEquals(-1, CommonUtil.mismatch(new int[0], new int[0]));
    }

    @Test
    public void testMismatchIntRange() {
        int[] a = { 1, 2, 3, 4 };
        int[] b = { 5, 2, 3, 6 };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchLong() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 1L, 2L, 3L, 4L };
        long[] c = { 1L, 5L, 3L, 4L };
        long[] d = { 1L, 2L, 3L };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c));
        Assertions.assertEquals(3, CommonUtil.mismatch(a, d));
        Assertions.assertEquals(-1, CommonUtil.mismatch((long[]) null, (long[]) null));
        Assertions.assertEquals(-1, CommonUtil.mismatch(new long[0], new long[0]));
    }

    @Test
    public void testMismatchLongRange() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 5L, 2L, 3L, 6L };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchFloat() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] b = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] c = { 1.0f, 5.0f, 3.0f, 4.0f };
        float[] d = { 1.0f, 2.0f, 3.0f };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c));
        Assertions.assertEquals(3, CommonUtil.mismatch(a, d));
        Assertions.assertEquals(-1, CommonUtil.mismatch((float[]) null, (float[]) null));
        Assertions.assertEquals(-1, CommonUtil.mismatch(new float[0], new float[0]));
    }

    @Test
    public void testMismatchFloatRange() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] b = { 5.0f, 2.0f, 3.0f, 6.0f };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchDouble() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] b = { 1.0, 2.0, 3.0, 4.0 };
        double[] c = { 1.0, 5.0, 3.0, 4.0 };
        double[] d = { 1.0, 2.0, 3.0 };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c));
        Assertions.assertEquals(3, CommonUtil.mismatch(a, d));
        Assertions.assertEquals(-1, CommonUtil.mismatch((double[]) null, (double[]) null));
        Assertions.assertEquals(-1, CommonUtil.mismatch(new double[0], new double[0]));
    }

    @Test
    public void testMismatchDoubleRange() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] b = { 5.0, 2.0, 3.0, 6.0 };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchObjectArrays() {
        String[] a = { "a", "b", "c", "d" };
        String[] b = { "a", "b", "c", "d" };
        String[] c = { "a", "x", "c", "d" };
        String[] d = { "a", "b", "c" };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c));
        Assertions.assertEquals(3, CommonUtil.mismatch(a, d));
        Assertions.assertEquals(-1, CommonUtil.mismatch((String[]) null, (String[]) null));
        Assertions.assertEquals(-1, CommonUtil.mismatch(new String[0], new String[0]));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, null));
        Assertions.assertEquals(0, CommonUtil.mismatch(null, a));
    }

    @Test
    public void testMismatchObjectArraysRange() {
        String[] a = { "a", "b", "c", "d" };
        String[] b = { "x", "b", "c", "y" };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchObjectArraysWithComparator() {
        String[] a = { "A", "B", "C", "D" };
        String[] b = { "a", "b", "c", "d" };
        String[] c = { "a", "x", "c", "d" };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b, String.CASE_INSENSITIVE_ORDER));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c, String.CASE_INSENSITIVE_ORDER));
        Assertions.assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatchObjectArraysRangeWithComparator() {
        String[] a = { "A", "B", "C", "D" };
        String[] b = { "x", "b", "c", "y" };

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2, String.CASE_INSENSITIVE_ORDER));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10, String.CASE_INSENSITIVE_ORDER));
    }

    @Test
    public void testMismatchCollections() {
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("x", "b", "c", "y");

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchCollectionsWithComparator() {
        List<String> a = Arrays.asList("A", "B", "C", "D");
        List<String> b = Arrays.asList("x", "b", "c", "y");

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, b, 1, 2, String.CASE_INSENSITIVE_ORDER));
        Assertions.assertEquals(-1, CommonUtil.mismatch(a, 1, a, 1, 2, String.CASE_INSENSITIVE_ORDER));
    }

    @Test
    public void testMismatchIterables() {
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("a", "b", "c", "d");
        List<String> c = Arrays.asList("a", "x", "c", "d");
        List<String> d = Arrays.asList("a", "b", "c");

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c));
        Assertions.assertEquals(3, CommonUtil.mismatch(a, d));
        Assertions.assertEquals(-1, CommonUtil.mismatch((Iterable<String>) null, (Iterable<String>) null));
        Assertions.assertEquals(-1, CommonUtil.mismatch(new ArrayList<String>(), new ArrayList<String>()));
        Assertions.assertEquals(0, CommonUtil.mismatch(a, (Iterable<String>) null));
        Assertions.assertEquals(0, CommonUtil.mismatch((Iterable<String>) null, a));

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, a));
    }

    @Test
    public void testMismatchIterablesWithComparator() {
        List<String> a = Arrays.asList("A", "B", "C", "D");
        List<String> b = Arrays.asList("a", "b", "c", "d");
        List<String> c = Arrays.asList("a", "x", "c", "d");

        Assertions.assertEquals(-1, CommonUtil.mismatch(a, b, String.CASE_INSENSITIVE_ORDER));
        Assertions.assertEquals(1, CommonUtil.mismatch(a, c, String.CASE_INSENSITIVE_ORDER));
    }

    @Test
    public void testMismatchIterators() {
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("a", "b", "c", "d");
        List<String> c = Arrays.asList("a", "x", "c", "d");
        List<String> d = Arrays.asList("a", "b", "c");
        List<String> e = Arrays.asList("a", "b", "c", "d", "e");

        Assertions.assertEquals(-1, CommonUtil.mismatch(a.iterator(), b.iterator()));
        Assertions.assertEquals(1, CommonUtil.mismatch(a.iterator(), c.iterator()));
        Assertions.assertEquals(3, CommonUtil.mismatch(a.iterator(), d.iterator()));
        Assertions.assertEquals(4, CommonUtil.mismatch(a.iterator(), e.iterator()));
        Assertions.assertEquals(-1, CommonUtil.mismatch((Iterator<String>) null, (Iterator<String>) null));

        Iterator<String> iter = a.iterator();
        Assertions.assertEquals(-1, CommonUtil.mismatch(iter, iter));
    }

    @Test
    public void testMismatchIteratorsWithComparator() {
        List<String> a = Arrays.asList("A", "B", "C", "D");
        List<String> b = Arrays.asList("a", "b", "c", "d");
        List<String> c = Arrays.asList("a", "x", "c", "d");

        Assertions.assertEquals(-1, CommonUtil.mismatch(a.iterator(), b.iterator(), String.CASE_INSENSITIVE_ORDER));
        Assertions.assertEquals(1, CommonUtil.mismatch(a.iterator(), c.iterator(), String.CASE_INSENSITIVE_ORDER));
    }

    @Test
    public void testReverseBoolean() {
        boolean[] a = { true, false, true, false };
        boolean[] expected = { false, true, false, true };
        CommonUtil.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        boolean[] empty = {};
        CommonUtil.reverse(empty);
        Assertions.assertArrayEquals(new boolean[0], empty);

        CommonUtil.reverse((boolean[]) null);

        boolean[] single = { true };
        CommonUtil.reverse(single);
        Assertions.assertArrayEquals(new boolean[] { true }, single);
    }

    @Test
    public void testReverseBooleanRange() {
        boolean[] a = { true, false, true, false, true };
        boolean[] expected = { true, false, true, false, true };
        CommonUtil.reverse(a, 1, 4);
        expected[1] = false;
        expected[2] = true;
        expected[3] = false;
        Assertions.assertArrayEquals(expected, a);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.reverse(a, -1, 3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.reverse(a, 0, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.reverse(a, 3, 2));

        boolean[] b = { true, false, true };
        boolean[] original = b.clone();
        CommonUtil.reverse(b, 1, 1);
        Assertions.assertArrayEquals(original, b);
    }

    @Test
    public void testReverseChar() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] expected = { 'd', 'c', 'b', 'a' };
        CommonUtil.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        char[] empty = {};
        CommonUtil.reverse(empty);
        Assertions.assertArrayEquals(new char[0], empty);

        CommonUtil.reverse((char[]) null);
    }

    @Test
    public void testReverseCharRange() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        char[] expected = { 'a', 'd', 'c', 'b', 'e' };
        CommonUtil.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.reverse(a, -1, 3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.reverse(a, 0, 10));
    }

    @Test
    public void testReverseByte() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] expected = { 4, 3, 2, 1 };
        CommonUtil.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        byte[] empty = {};
        CommonUtil.reverse(empty);
        Assertions.assertArrayEquals(new byte[0], empty);

        CommonUtil.reverse((byte[]) null);
    }

    @Test
    public void testReverseByteRange() {
        byte[] a = { 1, 2, 3, 4, 5 };
        byte[] expected = { 1, 4, 3, 2, 5 };
        CommonUtil.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testReverseShort() {
        short[] a = { 1, 2, 3, 4 };
        short[] expected = { 4, 3, 2, 1 };
        CommonUtil.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        short[] empty = {};
        CommonUtil.reverse(empty);
        Assertions.assertArrayEquals(new short[0], empty);

        CommonUtil.reverse((short[]) null);
    }

    @Test
    public void testReverseShortRange() {
        short[] a = { 1, 2, 3, 4, 5 };
        short[] expected = { 1, 4, 3, 2, 5 };
        CommonUtil.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testReverseInt() {
        int[] a = { 1, 2, 3, 4 };
        int[] expected = { 4, 3, 2, 1 };
        CommonUtil.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        int[] empty = {};
        CommonUtil.reverse(empty);
        Assertions.assertArrayEquals(new int[0], empty);

        CommonUtil.reverse((int[]) null);
    }

    @Test
    public void testReverseIntRange() {
        int[] a = { 1, 2, 3, 4, 5 };
        int[] expected = { 1, 4, 3, 2, 5 };
        CommonUtil.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testReverseLong() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] expected = { 4L, 3L, 2L, 1L };
        CommonUtil.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        long[] empty = {};
        CommonUtil.reverse(empty);
        Assertions.assertArrayEquals(new long[0], empty);

        CommonUtil.reverse((long[]) null);
    }

    @Test
    public void testReverseLongRange() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        long[] expected = { 1L, 4L, 3L, 2L, 5L };
        CommonUtil.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testReverseFloat() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] expected = { 4.0f, 3.0f, 2.0f, 1.0f };
        CommonUtil.reverse(a);
        Assertions.assertArrayEquals(expected, a, 0.0f);

        float[] empty = {};
        CommonUtil.reverse(empty);
        Assertions.assertArrayEquals(new float[0], empty, 0.0f);

        CommonUtil.reverse((float[]) null);
    }

    @Test
    public void testReverseFloatRange() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] expected = { 1.0f, 4.0f, 3.0f, 2.0f, 5.0f };
        CommonUtil.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a, 0.0f);
    }

    @Test
    public void testReverseDouble() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] expected = { 4.0, 3.0, 2.0, 1.0 };
        CommonUtil.reverse(a);
        Assertions.assertArrayEquals(expected, a, 0.0);

        double[] empty = {};
        CommonUtil.reverse(empty);
        Assertions.assertArrayEquals(new double[0], empty, 0.0);

        CommonUtil.reverse((double[]) null);
    }

    @Test
    public void testReverseDoubleRange() {
        double[] a = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] expected = { 1.0, 4.0, 3.0, 2.0, 5.0 };
        CommonUtil.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a, 0.0);
    }

    @Test
    public void testReverseObject() {
        String[] a = { "a", "b", "c", "d" };
        String[] expected = { "d", "c", "b", "a" };
        CommonUtil.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        String[] empty = {};
        CommonUtil.reverse(empty);
        Assertions.assertArrayEquals(new String[0], empty);

        CommonUtil.reverse((String[]) null);
    }

    @Test
    public void testReverseObjectRange() {
        String[] a = { "a", "b", "c", "d", "e" };
        String[] expected = { "a", "d", "c", "b", "e" };
        CommonUtil.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testReverseList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.reverse(list);
        Assertions.assertEquals(Arrays.asList("d", "c", "b", "a"), list);

        List<String> empty = new ArrayList<>();
        CommonUtil.reverse(empty);
        Assertions.assertTrue(empty.isEmpty());

        CommonUtil.reverse((List<?>) null);

        List<String> single = new ArrayList<>(Arrays.asList("a"));
        CommonUtil.reverse(single);
        Assertions.assertEquals(Arrays.asList("a"), single);
    }

    @Test
    public void testReverseListRange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        CommonUtil.reverse(list, 1, 4);
        Assertions.assertEquals(Arrays.asList("a", "d", "c", "b", "e"), list);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.reverse(list, -1, 3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.reverse(list, 0, 10));

        List<String> list2 = new ArrayList<>(Arrays.asList("a", "b", "c"));
        CommonUtil.reverse(list2, 1, 1);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), list2);
    }

    @Test
    public void testReverseCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.reverse((Collection<?>) list);
        Assertions.assertEquals(Arrays.asList("d", "c", "b", "a"), list);

        Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.reverse(set);
        Assertions.assertEquals(4, set.size());

        Collection<String> empty = new ArrayList<>();
        CommonUtil.reverse(empty);
        Assertions.assertTrue(empty.isEmpty());

        CommonUtil.reverse((Collection<?>) null);

        Collection<String> single = new ArrayList<>(Arrays.asList("a"));
        CommonUtil.reverse(single);
        Assertions.assertEquals(1, single.size());
    }

    @Test
    public void testRotateBoolean() {
        boolean[] a = { true, false, true, false };
        boolean[] expected = { false, true, false, true };
        CommonUtil.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);

        boolean[] b = { true, false, true, false };
        boolean[] expected2 = { false, true, false, true };
        CommonUtil.rotate(b, -1);
        Assertions.assertArrayEquals(expected2, b);

        boolean[] c = { true, false, true, false };
        boolean[] original = c.clone();
        CommonUtil.rotate(c, 4);
        Assertions.assertArrayEquals(original, c);

        boolean[] empty = {};
        CommonUtil.rotate(empty, 1);
        Assertions.assertArrayEquals(new boolean[0], empty);

        CommonUtil.rotate((boolean[]) null, 1);
    }

    @Test
    public void testRotateBooleanRange() {
        boolean[] a = { true, false, true, false, true };
        boolean[] expected = { true, false, true, false, true };
        CommonUtil.rotate(a, 1, 4, 1);
        expected[1] = false;
        expected[2] = false;
        expected[3] = true;
        Assertions.assertArrayEquals(expected, a);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.rotate(a, -1, 3, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.rotate(a, 0, 10, 1));

        boolean[] b = { true, false, true };
        boolean[] original = b.clone();
        CommonUtil.rotate(b, 1, 1, 1);
        Assertions.assertArrayEquals(original, b);
    }

    @Test
    public void testRotateChar() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] expected = { 'd', 'a', 'b', 'c' };
        CommonUtil.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);

        char[] b = { 'a', 'b', 'c', 'd' };
        char[] expected2 = { 'b', 'c', 'd', 'a' };
        CommonUtil.rotate(b, -1);
        Assertions.assertArrayEquals(expected2, b);

        char[] c = { 'a', 'b', 'c', 'd' };
        char[] expected3 = { 'b', 'c', 'd', 'a' };
        CommonUtil.rotate(c, 7);
        Assertions.assertArrayEquals(expected3, c);
    }

    @Test
    public void testRotateCharRange() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        char[] expected = { 'a', 'd', 'b', 'c', 'e' };
        CommonUtil.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateByte() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] expected = { 4, 1, 2, 3 };
        CommonUtil.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateByteRange() {
        byte[] a = { 1, 2, 3, 4, 5 };
        byte[] expected = { 1, 4, 2, 3, 5 };
        CommonUtil.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateShort() {
        short[] a = { 1, 2, 3, 4 };
        short[] expected = { 4, 1, 2, 3 };
        CommonUtil.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateShortRange() {
        short[] a = { 1, 2, 3, 4, 5 };
        short[] expected = { 1, 4, 2, 3, 5 };
        CommonUtil.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateInt() {
        int[] a = { 1, 2, 3, 4, 5 };
        int[] expected = { 4, 5, 1, 2, 3 };
        CommonUtil.rotate(a, 2);
        Assertions.assertArrayEquals(expected, a);

        int[] b = { 1, 2, 3, 4, 5 };
        int[] expected2 = { 2, 3, 4, 5, 1 };
        CommonUtil.rotate(b, -1);
        Assertions.assertArrayEquals(expected2, b);

        int[] c = { 1, 2, 3, 4, 5 };
        int[] expected3 = { 4, 5, 1, 2, 3 };
        CommonUtil.rotate(c, 7);
        Assertions.assertArrayEquals(expected3, c);
    }

    @Test
    public void testRotateIntRange() {

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 2, 3, 4, 5 };
            CommonUtil.rotate(a, 0);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 5, 1, 2, 3, 4 };
            CommonUtil.rotate(a, 1);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 4, 5, 1, 2, 3 };
            CommonUtil.rotate(a, 2);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 3, 4, 5, 1, 2 };
            CommonUtil.rotate(a, 3);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 2, 3, 4, 5, 1 };
            CommonUtil.rotate(a, 4);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 2, 3, 4, 5 };
            CommonUtil.rotate(a, 5);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 5, 1, 2, 3, 4 };
            CommonUtil.rotate(a, 6);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 2, 3, 4, 5, 1 };
            CommonUtil.rotate(a, -1);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 3, 4, 5, 1, 2 };
            CommonUtil.rotate(a, -2);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 4, 2, 3, 5 };
            CommonUtil.rotate(a, 1, 4, 1);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 3, 4, 2, 5 };
            CommonUtil.rotate(a, 1, 4, 2);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 2, 3, 4, 5 };
            CommonUtil.rotate(a, 1, 4, 3);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 3, 4, 2, 5 };
            CommonUtil.rotate(a, 1, 4, -1);
            Assertions.assertArrayEquals(expected, a);
        }
    }

    @Test
    public void testRotateLong() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] expected = { 4L, 1L, 2L, 3L };
        CommonUtil.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateLongRange() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        long[] expected = { 1L, 4L, 2L, 3L, 5L };
        CommonUtil.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateFloat() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] expected = { 4.0f, 1.0f, 2.0f, 3.0f };
        CommonUtil.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a, 0.0f);
    }

    @Test
    public void testRotateFloatRange() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] expected = { 1.0f, 4.0f, 2.0f, 3.0f, 5.0f };
        CommonUtil.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a, 0.0f);
    }

    @Test
    public void testRotateDouble() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] expected = { 4.0, 1.0, 2.0, 3.0 };
        CommonUtil.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a, 0.0);
    }

    @Test
    public void testRotateDoubleRange() {
        double[] a = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] expected = { 1.0, 4.0, 2.0, 3.0, 5.0 };
        CommonUtil.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a, 0.0);
    }

    @Test
    public void testRotateObject() {
        String[] a = { "a", "b", "c", "d" };
        String[] expected = { "d", "a", "b", "c" };
        CommonUtil.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateObjectRange() {
        String[] a = { "a", "b", "c", "d", "e" };
        String[] expected = { "a", "d", "b", "c", "e" };
        CommonUtil.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.rotate(list, 1);
        Assertions.assertEquals(Arrays.asList("d", "a", "b", "c"), list);

        CommonUtil.rotate((List<?>) null, 1);

        List<String> single = new ArrayList<>(Arrays.asList("a"));
        CommonUtil.rotate(single, 1);
        Assertions.assertEquals(Arrays.asList("a"), single);

        List<String> list2 = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.rotate(list2, 4);
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d"), list2);
    }

    @Test
    public void testRotateCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.rotate((Collection<?>) list, 1);
        Assertions.assertEquals(Arrays.asList("d", "a", "b", "c"), list);

        Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.rotate(set, 1);
        Assertions.assertEquals(4, set.size());

        Collection<String> empty = new ArrayList<>();
        CommonUtil.rotate(empty, 1);
        Assertions.assertTrue(empty.isEmpty());

        CommonUtil.rotate((Collection<?>) null, 1);

        Collection<String> single = new ArrayList<>(Arrays.asList("a"));
        CommonUtil.rotate(single, 1);
        Assertions.assertEquals(1, single.size());
    }

    @Test
    public void testShuffleBoolean() {
        boolean[] a = { true, false, true, false, true };
        boolean[] original = a.clone();
        CommonUtil.shuffle(a);
        Assertions.assertEquals(original.length, a.length);

        boolean[] empty = {};
        CommonUtil.shuffle(empty);
        Assertions.assertArrayEquals(new boolean[0], empty);

        CommonUtil.shuffle((boolean[]) null);

        boolean[] single = { true };
        CommonUtil.shuffle(single);
        Assertions.assertArrayEquals(new boolean[] { true }, single);
    }

    @Test
    public void testShuffleBooleanRange() {
        boolean[] a = { true, false, true, false, true };
        boolean first = a[0];
        boolean last = a[4];
        CommonUtil.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.shuffle(a, -1, 3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.shuffle(a, 0, 10));
    }

    @Test
    public void testShuffleBooleanWithRandom() {
        boolean[] a = { true, false, true, false, true };
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleBooleanRangeWithRandom() {
        boolean[] a = { true, false, true, false, true };
        boolean first = a[0];
        boolean last = a[4];
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleChar() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        char[] original = a.clone();
        CommonUtil.shuffle(a);
        Assertions.assertEquals(original.length, a.length);
        Arrays.sort(original);
        Arrays.sort(a);
        Assertions.assertArrayEquals(original, a);
    }

    @Test
    public void testShuffleCharRange() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        char first = a[0];
        char last = a[4];
        CommonUtil.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleCharWithRandom() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleCharRangeWithRandom() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        char first = a[0];
        char last = a[4];
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleByte() {
        byte[] a = { 1, 2, 3, 4, 5 };
        byte[] original = a.clone();
        CommonUtil.shuffle(a);
        Assertions.assertEquals(original.length, a.length);
        Arrays.sort(original);
        Arrays.sort(a);
        Assertions.assertArrayEquals(original, a);
    }

    @Test
    public void testShuffleByteRange() {
        byte[] a = { 1, 2, 3, 4, 5 };
        byte first = a[0];
        byte last = a[4];
        CommonUtil.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleByteWithRandom() {
        byte[] a = { 1, 2, 3, 4, 5 };
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleByteRangeWithRandom() {
        byte[] a = { 1, 2, 3, 4, 5 };
        byte first = a[0];
        byte last = a[4];
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleShort() {
        short[] a = { 1, 2, 3, 4, 5 };
        short[] original = a.clone();
        CommonUtil.shuffle(a);
        Assertions.assertEquals(original.length, a.length);
        Arrays.sort(original);
        Arrays.sort(a);
        Assertions.assertArrayEquals(original, a);
    }

    @Test
    public void testShuffleShortRange() {
        short[] a = { 1, 2, 3, 4, 5 };
        short first = a[0];
        short last = a[4];
        CommonUtil.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleShortWithRandom() {
        short[] a = { 1, 2, 3, 4, 5 };
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleShortRangeWithRandom() {
        short[] a = { 1, 2, 3, 4, 5 };
        short first = a[0];
        short last = a[4];
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleInt() {
        int[] a = { 1, 2, 3, 4, 5 };
        int[] original = a.clone();
        CommonUtil.shuffle(a);
        Assertions.assertEquals(original.length, a.length);
        Arrays.sort(original);
        Arrays.sort(a);
        Assertions.assertArrayEquals(original, a);
    }

    @Test
    public void testShuffleIntRange() {
        int[] a = { 1, 2, 3, 4, 5 };
        int first = a[0];
        int last = a[4];
        CommonUtil.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleIntWithRandom() {
        int[] a = { 1, 2, 3, 4, 5 };
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleIntRangeWithRandom() {
        int[] a = { 1, 2, 3, 4, 5 };
        int first = a[0];
        int last = a[4];
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleLong() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        long[] original = a.clone();
        CommonUtil.shuffle(a);
        Assertions.assertEquals(original.length, a.length);
        Arrays.sort(original);
        Arrays.sort(a);
        Assertions.assertArrayEquals(original, a);
    }

    @Test
    public void testShuffleLongRange() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        long first = a[0];
        long last = a[4];
        CommonUtil.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleLongWithRandom() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleLongRangeWithRandom() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        long first = a[0];
        long last = a[4];
        Random rnd = new Random(12345);
        CommonUtil.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleFloat() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] original = a.clone();
        CommonUtil.shuffle(a);
        Assertions.assertEquals(original.length, a.length);
        Arrays.sort(original);
        Arrays.sort(a);
        Assertions.assertArrayEquals(original, a, 0.0f);
    }

    @Test
    public void testEmptyMapReturnsSameInstance() {
        Map<String, String> map1 = CommonUtil.emptyMap();
        Map<Integer, Integer> map2 = CommonUtil.emptyMap();

        assertSame(map1, map2);
    }

    @Test
    public void testCompareBoolean() {
        assertEquals(0, CommonUtil.compare(true, true));
        assertEquals(0, CommonUtil.compare(false, false));
        assertEquals(1, CommonUtil.compare(true, false));
        assertEquals(-1, CommonUtil.compare(false, true));
    }

    @Test
    public void testCompareChar() {
        assertEquals(0, CommonUtil.compare('a', 'a'));
        assertEquals(-1, CommonUtil.compare('a', 'b'));
        assertEquals(1, CommonUtil.compare('b', 'a'));
    }

    @Test
    public void testCompareByte() {
        assertEquals(0, CommonUtil.compare((byte) 10, (byte) 10));
        assertTrue(CommonUtil.compare((byte) 5, (byte) 10) < 0);
        assertTrue(CommonUtil.compare((byte) 10, (byte) 5) > 0);
    }

    @Test
    public void testCompareUnsignedByte() {
        assertEquals(0, CommonUtil.compareUnsigned((byte) 10, (byte) 10));
        assertTrue(CommonUtil.compareUnsigned((byte) 5, (byte) 10) < 0);
        assertTrue(CommonUtil.compareUnsigned((byte) -1, (byte) 127) > 0);
    }

    @Test
    public void testCompareShort() {
        assertEquals(0, CommonUtil.compare((short) 100, (short) 100));
        assertTrue(CommonUtil.compare((short) 50, (short) 100) < 0);
        assertTrue(CommonUtil.compare((short) 100, (short) 50) > 0);
    }

    @Test
    public void testCompareUnsignedShort() {
        assertEquals(0, CommonUtil.compareUnsigned((short) 100, (short) 100));
        assertTrue(CommonUtil.compareUnsigned((short) 50, (short) 100) < 0);
        assertTrue(CommonUtil.compareUnsigned((short) -1, (short) 32767) > 0);
    }

    @Test
    public void testCompareInt() {
        assertEquals(0, CommonUtil.compare(100, 100));
        assertTrue(CommonUtil.compare(50, 100) < 0);
        assertTrue(CommonUtil.compare(100, 50) > 0);
    }

    @Test
    public void testCompareUnsignedInt() {
        assertEquals(0, CommonUtil.compareUnsigned(100, 100));
        assertTrue(CommonUtil.compareUnsigned(50, 100) < 0);
        assertTrue(CommonUtil.compareUnsigned(-1, Integer.MAX_VALUE) > 0);
    }

    @Test
    public void testCompareLong() {
        assertEquals(0, CommonUtil.compare(100L, 100L));
        assertTrue(CommonUtil.compare(50L, 100L) < 0);
        assertTrue(CommonUtil.compare(100L, 50L) > 0);
    }

    @Test
    public void testCompareUnsignedLong() {
        assertEquals(0, CommonUtil.compareUnsigned(100L, 100L));
        assertEquals(-1, CommonUtil.compareUnsigned(50L, 100L));
        assertEquals(1, CommonUtil.compareUnsigned(-1L, Long.MAX_VALUE));
    }

    @Test
    public void testCompareFloat() {
        assertEquals(0, CommonUtil.compare(1.5f, 1.5f));
        assertEquals(-1, CommonUtil.compare(1.0f, 1.5f));
        assertEquals(1, CommonUtil.compare(1.5f, 1.0f));
        assertEquals(1, CommonUtil.compare(Float.NaN, 1.0f));
    }

    @Test
    public void testCompareDouble() {
        assertEquals(0, CommonUtil.compare(1.5, 1.5));
        assertEquals(-1, CommonUtil.compare(1.0, 1.5));
        assertEquals(1, CommonUtil.compare(1.5, 1.0));
        assertEquals(1, CommonUtil.compare(Double.NaN, 1.0));
    }

    @Test
    public void testCompareComparable() {
        assertEquals(0, CommonUtil.compare("abc", "abc"));
        assertTrue(CommonUtil.compare("abc", "def") < 0);
        assertTrue(CommonUtil.compare("def", "abc") > 0);
        assertTrue(CommonUtil.compare(null, "abc") < 0);
        assertTrue(CommonUtil.compare("abc", null) > 0);
        assertEquals(0, CommonUtil.compare((String) null, null));
    }

    @Test
    public void testCompareWithComparator() {
        Comparator<String> comp = String.CASE_INSENSITIVE_ORDER;
        assertEquals(0, CommonUtil.compare("ABC", "abc", comp));
        assertTrue(CommonUtil.compare("abc", "def", comp) < 0);
        assertTrue(CommonUtil.compare("def", "abc", comp) > 0);

        assertEquals(0, CommonUtil.compare("abc", "abc", null));
    }

    @Test
    public void testCompareMultiplePairs2() {
        assertEquals(0, CommonUtil.compare("a", "a", 1, 1));
        assertEquals(-1, CommonUtil.compare("a", "b", 1, 1));
        assertEquals(1, CommonUtil.compare("b", "a", 1, 1));
        assertEquals(-1, CommonUtil.compare("a", "a", 1, 2));
        assertEquals(1, CommonUtil.compare("a", "a", 2, 1));
    }

    @Test
    public void testCompareMultiplePairs3() {
        assertEquals(0, CommonUtil.compare("a", "a", 1, 1, 5L, 5L));
        assertEquals(-1, CommonUtil.compare("a", "b", 1, 1, 5L, 5L));
        assertEquals(1, CommonUtil.compare("a", "a", 2, 1, 5L, 5L));
        assertEquals(-1, CommonUtil.compare("a", "a", 1, 1, 5L, 6L));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCompareMultiplePairs4() {
        assertEquals(0, CommonUtil.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0));
        assertEquals(-1, CommonUtil.compare("a", "b", 1, 1, 5L, 5L, 1.0, 1.0));
        assertEquals(-1, CommonUtil.compare("a", "a", 1, 1, 5L, 5L, 1.0, 2.0));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCompareMultiplePairs5() {
        assertEquals(0, CommonUtil.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x"));
        assertEquals(1, CommonUtil.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "y", "x"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCompareMultiplePairs6() {
        assertEquals(0, CommonUtil.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x", true, true));
        assertEquals(-1, CommonUtil.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x", false, true));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCompareMultiplePairs7() {
        assertEquals(0, CommonUtil.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x", true, true, 'z', 'z'));
        assertEquals(1, CommonUtil.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x", true, true, 'z', 'y'));
    }

    @Test
    public void testCompareArraysWithRange_01() {
        {
            byte[] a1 = { 1, 2, 3, 4 };
            byte[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, CommonUtil.compare(a1, 1, a2, 1, 2));
        }
        {
            short[] a1 = { 1, 2, 3, 4 };
            short[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, CommonUtil.compare(a1, 1, a2, 1, 2));
        }
        {

            int[] a1 = { 1, 2, 3, 4 };
            int[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, CommonUtil.compare(a1, 1, a2, 1, 2));
        }
        {

            long[] a1 = { 1, 2, 3, 4 };
            long[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, CommonUtil.compare(a1, 1, a2, 1, 2));
        }
        {

            float[] a1 = { 1, 2, 3, 4 };
            float[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, CommonUtil.compare(a1, 1, a2, 1, 2));
        }
        {

            double[] a1 = { 1, 2, 3, 4 };
            double[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, CommonUtil.compare(a1, 1, a2, 1, 2));
        }
    }

    @Test
    public void testCompareCollections() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        List<String> list3 = Arrays.asList("a", "c", "b");

        assertEquals(0, CommonUtil.compare(list1, 0, list2, 0, 3));
        assertEquals(-1, CommonUtil.compare(list1, 1, list3, 1, 2));
    }

    @Test
    public void testFirstNonNull2() {
        assertEquals("a", CommonUtil.firstNonNull("a", "b").get());
        assertEquals("b", CommonUtil.firstNonNull(null, "b").get());
        assertFalse(CommonUtil.firstNonNull(null, null).isPresent());
    }

    @Test
    public void testLastNonNull2() {
        assertEquals("b", CommonUtil.lastNonNull("a", "b").get());
        assertEquals("a", CommonUtil.lastNonNull("a", null).get());
        assertFalse(CommonUtil.lastNonNull(null, null).isPresent());
    }

    @Test
    public void testFirstNonEmptyArrays() {
        String[] a1 = { "a", "b" };
        String[] a2 = { "c", "d" };
        String[] empty = {};

        assertEquals(a1, CommonUtil.firstNonEmpty(a1, a2).get());
        assertEquals(a2, CommonUtil.firstNonEmpty(null, a2).get());
        assertEquals(a2, CommonUtil.firstNonEmpty(empty, a2).get());
        assertFalse(CommonUtil.firstNonEmpty((String[]) null, (String[]) null).isPresent());
        assertFalse(CommonUtil.firstNonEmpty(empty, empty).isPresent());

        assertEquals(a1, CommonUtil.firstNonEmpty(a1, a2, empty).get());
        assertEquals(a2, CommonUtil.firstNonEmpty(empty, a2, a1).get());
        assertEquals(a1, CommonUtil.firstNonEmpty(empty, empty, a1).get());
    }

    @Test
    public void testFirstNonEmptyCollections() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("c", "d");
        List<String> empty = Collections.emptyList();

        assertEquals(list1, CommonUtil.firstNonEmpty(list1, list2).get());
        assertEquals(list2, CommonUtil.firstNonEmpty(null, list2).get());
        assertEquals(list2, CommonUtil.firstNonEmpty(empty, list2).get());
        assertFalse(CommonUtil.firstNonEmpty((List<String>) null, (List<String>) null).isPresent());
        assertFalse(CommonUtil.firstNonEmpty(empty, empty).isPresent());

        assertEquals(list1, CommonUtil.firstNonEmpty(list1, list2, empty).get());
    }

    @Test
    public void testFirstNonEmptyMaps() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        Map<String, String> empty = Collections.emptyMap();

        assertEquals(map1, CommonUtil.firstNonEmpty(map1, map2).get());
        assertEquals(map2, CommonUtil.firstNonEmpty(null, map2).get());
        assertEquals(map2, CommonUtil.firstNonEmpty(empty, map2).get());
        assertFalse(CommonUtil.firstNonEmpty((Map<String, String>) null, (Map<String, String>) null).isPresent());

        assertEquals(map1, CommonUtil.firstNonEmpty(map1, map2, empty).get());
    }

    @Test
    public void testFirstNonEmptyCharSequences() {
        assertEquals("abc", CommonUtil.firstNonEmpty("abc", "def").get());
        assertEquals("def", CommonUtil.firstNonEmpty("", "def").get());
        assertEquals("def", CommonUtil.firstNonEmpty(null, "def").get());
        assertFalse(CommonUtil.firstNonEmpty("", "").isPresent());
        assertFalse(CommonUtil.firstNonEmpty((String) null, (String) null).isPresent());

        assertEquals("abc", CommonUtil.firstNonEmpty("abc", "def", "ghi").get());
        assertEquals("def", CommonUtil.firstNonEmpty("", "def", "ghi").get());
        assertEquals("ghi", CommonUtil.firstNonEmpty("", "", "ghi").get());
    }

    @Test
    public void testFirstNonBlank2() {
        assertEquals("abc", CommonUtil.firstNonBlank("abc", "def").get());
        assertEquals("def", CommonUtil.firstNonBlank("  ", "def").get());
        assertEquals("def", CommonUtil.firstNonBlank(null, "def").get());
        assertFalse(CommonUtil.firstNonBlank("  ", "  ").isPresent());
        assertFalse(CommonUtil.firstNonBlank((String) null, (String) null).isPresent());
    }

    @Test
    public void testFirstNonBlank3() {
        assertEquals("abc", CommonUtil.firstNonBlank("abc", "def", "ghi").get());
        assertEquals("def", CommonUtil.firstNonBlank("  ", "def", "ghi").get());
        assertEquals("ghi", CommonUtil.firstNonBlank("  ", "  ", "ghi").get());
        assertFalse(CommonUtil.firstNonBlank("  ", null, "  ").isPresent());
    }

    @Test
    public void testFindFirstNonNullArray() {
        String[] arr = { null, "a", null, "b", "c" };
        assertEquals("a", CommonUtil.findFirstNonNull(arr, s -> true).get());
        assertEquals("b", CommonUtil.findFirstNonNull(arr, s -> s.equals("b")).get());
        assertFalse(CommonUtil.findFirstNonNull(arr, s -> s.equals("z")).isPresent());
        assertFalse(CommonUtil.findFirstNonNull(new String[] { null, null }, s -> true).isPresent());
        assertFalse(CommonUtil.findFirstNonNull((String[]) null, s -> true).isPresent());
    }

    @Test
    public void testFindFirstNonNullIterable() {
        List<String> list = Arrays.asList(null, "a", null, "b", "c");
        assertEquals("a", CommonUtil.findFirstNonNull(list, s -> true).get());
        assertEquals("b", CommonUtil.findFirstNonNull(list, s -> s.equals("b")).get());
        assertFalse(CommonUtil.findFirstNonNull(list, s -> s.equals("z")).isPresent());
        assertFalse(CommonUtil.findFirstNonNull(Arrays.asList(null, null), s -> true).isPresent());
        assertFalse(CommonUtil.findFirstNonNull((Iterable<String>) null, s -> true).isPresent());
    }

    @Test
    public void testFindFirstNonNullIterator() {
        List<String> list = Arrays.asList(null, "a", null, "b", "c");
        assertEquals("a", CommonUtil.findFirstNonNull(list.iterator(), s -> true).get());
        assertEquals("b", CommonUtil.findFirstNonNull(list.iterator(), s -> s.equals("b")).get());
        assertFalse(CommonUtil.findFirstNonNull(list.iterator(), s -> s.equals("z")).isPresent());
        assertFalse(CommonUtil.findFirstNonNull(Arrays.asList(null, null).iterator(), s -> true).isPresent());
        assertFalse(CommonUtil.findFirstNonNull((Iterator<String>) null, s -> true).isPresent());
    }

    @Test
    public void testToList_BooleanArray() {
        boolean[] array = { true, false, true };
        List<Boolean> list = CommonUtil.toList(array);
        assertEquals(Arrays.asList(true, false, true), list);

        assertEquals(new ArrayList<>(), CommonUtil.toList(new boolean[0]));
        assertEquals(new ArrayList<>(), CommonUtil.toList((boolean[]) null));
    }

    @Test
    public void testToList_BooleanArrayRange() {
        boolean[] array = { true, false, true, false, true };
        List<Boolean> list = CommonUtil.toList(array, 1, 4);
        assertEquals(Arrays.asList(false, true, false), list);

        assertEquals(new ArrayList<>(), CommonUtil.toList(array, 2, 2));

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toList(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toList(array, 0, 6));
    }

    @Test
    public void testToList_CharArray() {
        char[] array = { 'a', 'b', 'c' };
        List<Character> list = CommonUtil.toList(array);
        assertEquals(Arrays.asList('a', 'b', 'c'), list);

        assertEquals(new ArrayList<>(), CommonUtil.toList(new char[0]));
        assertEquals(new ArrayList<>(), CommonUtil.toList((char[]) null));
    }

    @Test
    public void testToList_CharArrayRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        List<Character> list = CommonUtil.toList(array, 1, 4);
        assertEquals(Arrays.asList('b', 'c', 'd'), list);
    }

    @Test
    public void testToList_ByteArray() {
        byte[] array = { 1, 2, 3 };
        List<Byte> list = CommonUtil.toList(array);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), list);

        assertEquals(new ArrayList<>(), CommonUtil.toList(new byte[0]));
        assertEquals(new ArrayList<>(), CommonUtil.toList((byte[]) null));
    }

    @Test
    public void testToList_ByteArrayRange() {
        byte[] array = { 1, 2, 3, 4, 5 };
        List<Byte> list = CommonUtil.toList(array, 1, 4);
        assertEquals(Arrays.asList((byte) 2, (byte) 3, (byte) 4), list);
    }

    @Test
    public void testToList_ShortArray() {
        short[] array = { 1, 2, 3 };
        List<Short> list = CommonUtil.toList(array);
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), list);

        assertEquals(new ArrayList<>(), CommonUtil.toList(new short[0]));
        assertEquals(new ArrayList<>(), CommonUtil.toList((short[]) null));
    }

    @Test
    public void testToList_ShortArrayRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        List<Short> list = CommonUtil.toList(array, 1, 4);
        assertEquals(Arrays.asList((short) 2, (short) 3, (short) 4), list);
    }

    @Test
    public void testToList_IntArrayRange() {
        int[] array = { 1, 2, 3, 4, 5 };
        List<Integer> list = CommonUtil.toList(array, 1, 4);
        assertEquals(Arrays.asList(2, 3, 4), list);
    }

    @Test
    public void testToList_LongArray() {
        long[] array = { 1L, 2L, 3L };
        List<Long> list = CommonUtil.toList(array);
        assertEquals(Arrays.asList(1L, 2L, 3L), list);

        assertEquals(new ArrayList<>(), CommonUtil.toList(new long[0]));
        assertEquals(new ArrayList<>(), CommonUtil.toList((long[]) null));
    }

    @Test
    public void testToList_LongArrayRange() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        List<Long> list = CommonUtil.toList(array, 1, 4);
        assertEquals(Arrays.asList(2L, 3L, 4L), list);
    }

    @Test
    public void testToList_FloatArray() {
        float[] array = { 1.0f, 2.0f, 3.0f };
        List<Float> list = CommonUtil.toList(array);
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), list);

        assertEquals(new ArrayList<>(), CommonUtil.toList(new float[0]));
        assertEquals(new ArrayList<>(), CommonUtil.toList((float[]) null));
    }

    @Test
    public void testToList_FloatArrayRange() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        List<Float> list = CommonUtil.toList(array, 1, 4);
        assertEquals(Arrays.asList(2.0f, 3.0f, 4.0f), list);
    }

    @Test
    public void testToList_DoubleArray() {
        double[] array = { 1.0, 2.0, 3.0 };
        List<Double> list = CommonUtil.toList(array);
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), list);

        assertEquals(new ArrayList<>(), CommonUtil.toList(new double[0]));
        assertEquals(new ArrayList<>(), CommonUtil.toList((double[]) null));
    }

    @Test
    public void testToList_DoubleArrayRange() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        List<Double> list = CommonUtil.toList(array, 1, 4);
        assertEquals(Arrays.asList(2.0, 3.0, 4.0), list);
    }

    @Test
    public void testToList_ObjectArray() {
        String[] array = { "a", "b", "c" };
        List<String> list = CommonUtil.toList(array);
        assertEquals(Arrays.asList("a", "b", "c"), list);

        assertEquals(new ArrayList<>(), CommonUtil.toList(new String[0]));
        assertEquals(new ArrayList<>(), CommonUtil.toList((String[]) null));
    }

    @Test
    public void testToList_ObjectArrayRange() {
        String[] array = { "a", "b", "c", "d", "e" };
        List<String> list = CommonUtil.toList(array, 1, 4);
        assertEquals(Arrays.asList("b", "c", "d"), list);

        List<String> fullList = CommonUtil.toList(array, 0, array.length);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), fullList);

        assertEquals(new ArrayList<>(), CommonUtil.toList(array, 2, 2));
    }

    @Test
    public void testToList_Iterator() {
        Iterator<String> iter = Arrays.asList("a", "b", "c").iterator();
        List<String> list = CommonUtil.toList(iter);
        assertEquals(Arrays.asList("a", "b", "c"), list);

        List<String> nullList = CommonUtil.toList((Iterator<String>) null);
        assertEquals(new ArrayList<>(), nullList);
    }

    @Test
    public void testToSet_BooleanArray() {
        boolean[] array = { true, false, true, false };
        Set<Boolean> set = CommonUtil.toSet(array);
        assertEquals(2, set.size());
        assertTrue(set.contains(true));
        assertTrue(set.contains(false));

        assertEquals(new HashSet<>(), CommonUtil.toSet(new boolean[0]));
        assertEquals(new HashSet<>(), CommonUtil.toSet((boolean[]) null));
    }

    @Test
    public void testToSet_BooleanArrayRange() {
        boolean[] array = { true, false, true, false, true };
        Set<Boolean> set = CommonUtil.toSet(array, 1, 4);
        assertEquals(2, set.size());
        assertTrue(set.contains(true));
        assertTrue(set.contains(false));
    }

    @Test
    public void testToSet_CharArray() {
        char[] array = { 'a', 'b', 'c', 'a' };
        Set<Character> set = CommonUtil.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains('a'));
        assertTrue(set.contains('b'));
        assertTrue(set.contains('c'));
    }

    @Test
    public void testToSet_CharArrayRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        Set<Character> set = CommonUtil.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains('b'));
        assertTrue(set.contains('c'));
        assertTrue(set.contains('d'));
    }

    @Test
    public void testToSet_ByteArray() {
        byte[] array = { 1, 2, 3, 1, 2 };
        Set<Byte> set = CommonUtil.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains((byte) 1));
        assertTrue(set.contains((byte) 2));
        assertTrue(set.contains((byte) 3));
    }

    @Test
    public void testToSet_ByteArrayRange() {
        byte[] array = { 1, 2, 3, 4, 5 };
        Set<Byte> set = CommonUtil.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains((byte) 2));
        assertTrue(set.contains((byte) 3));
        assertTrue(set.contains((byte) 4));
    }

    @Test
    public void testToSet_ShortArray() {
        short[] array = { 1, 2, 3, 1, 2 };
        Set<Short> set = CommonUtil.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains((short) 1));
        assertTrue(set.contains((short) 2));
        assertTrue(set.contains((short) 3));
    }

    @Test
    public void testToSet_ShortArrayRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        Set<Short> set = CommonUtil.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains((short) 2));
        assertTrue(set.contains((short) 3));
        assertTrue(set.contains((short) 4));
    }

    @Test
    public void testToSet_IntArray() {
        int[] array = { 1, 2, 3, 1, 2 };
        Set<Integer> set = CommonUtil.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
    }

    @Test
    public void testToSet_IntArrayRange() {
        int[] array = { 1, 2, 3, 4, 5 };
        Set<Integer> set = CommonUtil.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
        assertTrue(set.contains(4));
    }

    @Test
    public void testToSet_LongArray() {
        long[] array = { 1L, 2L, 3L, 1L, 2L };
        Set<Long> set = CommonUtil.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains(1L));
        assertTrue(set.contains(2L));
        assertTrue(set.contains(3L));
    }

    @Test
    public void testToSet_LongArrayRange() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        Set<Long> set = CommonUtil.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains(2L));
        assertTrue(set.contains(3L));
        assertTrue(set.contains(4L));
    }

    @Test
    public void testToSet_FloatArray() {
        float[] array = { 1.0f, 2.0f, 3.0f, 1.0f, 2.0f };
        Set<Float> set = CommonUtil.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains(1.0f));
        assertTrue(set.contains(2.0f));
        assertTrue(set.contains(3.0f));
    }

    @Test
    public void testToSet_FloatArrayRange() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        Set<Float> set = CommonUtil.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains(2.0f));
        assertTrue(set.contains(3.0f));
        assertTrue(set.contains(4.0f));
    }

    @Test
    public void testToSet_DoubleArray() {
        double[] array = { 1.0, 2.0, 3.0, 1.0, 2.0 };
        Set<Double> set = CommonUtil.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains(1.0));
        assertTrue(set.contains(2.0));
        assertTrue(set.contains(3.0));
    }

    @Test
    public void testToSet_DoubleArrayRange() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        Set<Double> set = CommonUtil.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains(2.0));
        assertTrue(set.contains(3.0));
        assertTrue(set.contains(4.0));
    }

    @Test
    public void testToSet_ObjectArray() {
        String[] array = { "a", "b", "c", "a", "b" };
        Set<String> set = CommonUtil.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));

        assertEquals(new HashSet<>(), CommonUtil.toSet(new String[0]));
        assertEquals(new HashSet<>(), CommonUtil.toSet((String[]) null));
    }

    @Test
    public void testToSet_ObjectArrayRange() {
        String[] array = { "a", "b", "c", "d", "e" };
        Set<String> set = CommonUtil.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
        assertTrue(set.contains("d"));
    }

    @Test
    public void testToSet_Iterator() {
        Iterator<String> iter = Arrays.asList("a", "b", "c", "a", "b").iterator();
        Set<String> set = CommonUtil.toSet(iter);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));

        Set<String> nullSet = CommonUtil.toSet((Iterator<String>) null);
        assertEquals(new HashSet<>(), nullSet);
    }

    @Test
    public void testToCollection_BooleanArray() {
        boolean[] array = { true, false, true };
        List<Boolean> list = CommonUtil.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(true, false, true), list);

        LinkedList<Boolean> linkedList = CommonUtil.toCollection(array, size -> new LinkedList<>());
        assertEquals(3, linkedList.size());
    }

    @Test
    public void testToCollection_BooleanArrayRange() {
        boolean[] array = { true, false, true, false, true };
        List<Boolean> list = CommonUtil.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(false, true, false), list);
    }

    @Test
    public void testToCollection_CharArray() {
        char[] array = { 'a', 'b', 'c' };
        List<Character> list = CommonUtil.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList('a', 'b', 'c'), list);
    }

    @Test
    public void testToCollection_CharArrayRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        List<Character> list = CommonUtil.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList('b', 'c', 'd'), list);
    }

    @Test
    public void testToCollection_ByteArray() {
        byte[] array = { 1, 2, 3 };
        List<Byte> list = CommonUtil.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), list);
    }

    @Test
    public void testToCollection_ByteArrayRange() {
        byte[] array = { 1, 2, 3, 4, 5 };
        List<Byte> list = CommonUtil.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList((byte) 2, (byte) 3, (byte) 4), list);
    }

    @Test
    public void testToCollection_ShortArray() {
        short[] array = { 1, 2, 3 };
        List<Short> list = CommonUtil.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), list);
    }

    @Test
    public void testToCollection_ShortArrayRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        List<Short> list = CommonUtil.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList((short) 2, (short) 3, (short) 4), list);
    }

    @Test
    public void testToCollection_IntArray() {
        int[] array = { 1, 2, 3 };
        List<Integer> list = CommonUtil.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testToCollection_IntArrayRange() {
        int[] array = { 1, 2, 3, 4, 5 };
        List<Integer> list = CommonUtil.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(2, 3, 4), list);
    }

    @Test
    public void testToCollection_LongArray() {
        long[] array = { 1L, 2L, 3L };
        List<Long> list = CommonUtil.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(1L, 2L, 3L), list);
    }

    @Test
    public void testToCollection_LongArrayRange() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        List<Long> list = CommonUtil.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(2L, 3L, 4L), list);
    }

    @Test
    public void testToCollection_FloatArray() {
        float[] array = { 1.0f, 2.0f, 3.0f };
        List<Float> list = CommonUtil.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), list);
    }

    @Test
    public void testToCollection_FloatArrayRange() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        List<Float> list = CommonUtil.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(2.0f, 3.0f, 4.0f), list);
    }

    @Test
    public void testToCollection_DoubleArray() {
        double[] array = { 1.0, 2.0, 3.0 };
        List<Double> list = CommonUtil.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), list);
    }

    @Test
    public void testToCollection_DoubleArrayRange() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        List<Double> list = CommonUtil.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(2.0, 3.0, 4.0), list);
    }

    @Test
    public void testToCollection_ObjectArray() {
        String[] array = { "a", "b", "c" };
        List<String> list = CommonUtil.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList("a", "b", "c"), list);

        List<String> emptyList = CommonUtil.toCollection(new String[0], size -> new ArrayList<>(size));
        assertEquals(new ArrayList<>(), emptyList);
    }

    @Test
    public void testToCollection_ObjectArrayRange() {
        String[] array = { "a", "b", "c", "d", "e" };
        List<String> list = CommonUtil.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList("b", "c", "d"), list);
    }

    @Test
    public void testToCollection_Iterable() {
        {
            List<String> source = Arrays.asList("a", "b", "c");
            Set<String> set = CommonUtil.toCollection(source, size -> new HashSet<>(size));
            assertEquals(3, set.size());
            assertTrue(set.contains("a"));
            assertTrue(set.contains("b"));
            assertTrue(set.contains("c"));

            Set<String> emptySet = CommonUtil.toCollection(new ArrayList<String>(), size -> new HashSet<>(size));
            assertTrue(emptySet.isEmpty());
        }
        {
            Iterable<String> source = createIterable("a", "b", "c");
            Set<String> set = CommonUtil.toCollection(source, size -> new HashSet<>(size));
            assertEquals(3, set.size());
            assertTrue(set.contains("a"));
            assertTrue(set.contains("b"));
            assertTrue(set.contains("c"));
        }

    }

    @Test
    public void testToCollection_Iterator() {
        Iterator<String> iter = Arrays.asList("a", "b", "c").iterator();
        List<String> list = CommonUtil.toCollection(iter, () -> new ArrayList<>());
        assertEquals(Arrays.asList("a", "b", "c"), list);

        List<String> nullList = CommonUtil.toCollection((Iterator<String>) null, () -> new ArrayList<>());
        assertTrue(nullList.isEmpty());
    }

    @Test
    public void testAsList_VarArgs() {
        String[] array = { "a", "b", "c" };
        List<String> list = CommonUtil.asList(array);
        assertTrue(list instanceof ImmutableList);
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));

        List<String> listWithNull = CommonUtil.asList("a", null, "c");
        assertEquals(3, listWithNull.size());
        assertNull(listWithNull.get(1));

        List<String> emptyList = CommonUtil.asList();
        assertTrue(emptyList.isEmpty());

        List<String> nullList = CommonUtil.asList((String[]) null);
        assertTrue(nullList.isEmpty());

        final List<String> immutableList = list;
        assertThrows(UnsupportedOperationException.class, () -> immutableList.add("d"));
    }

    @Test
    public void testAsLinkedList() {
        LinkedList<String> list = CommonUtil.toLinkedList("a");
        assertEquals(1, list.size());
        assertEquals("a", list.getFirst());

        list = CommonUtil.toLinkedList("a", "b");
        assertEquals(2, list.size());

        list = CommonUtil.toLinkedList("a", "b", "c");
        assertEquals(3, list.size());

        list = CommonUtil.toLinkedList("a", "b", "c", "d");
        assertEquals(4, list.size());

        list = CommonUtil.toLinkedList("a", "b", "c", "d", "e");
        assertEquals(5, list.size());

        list = CommonUtil.toLinkedList("a", "b", "c", "d", "e", "f");
        assertEquals(6, list.size());

        list = CommonUtil.toLinkedList("a", "b", "c", "d", "e", "f", "g");
        assertEquals(7, list.size());
    }

    @Test
    public void testAsLinkedList_VarArgs() {
        String[] array = { "a", "b", "c" };
        LinkedList<String> list = CommonUtil.toLinkedList(array);
        assertEquals(3, list.size());

        LinkedList<String> emptyList = CommonUtil.toLinkedList();
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testAsSet_VarArgs() {
        String[] array = { "a", "b", "c", "a" };
        Set<String> set = CommonUtil.asSet(array);
        assertTrue(set instanceof ImmutableSet);
        assertEquals(3, set.size());

        Set<String> setWithNull = CommonUtil.asSet("a", null, "a");
        assertEquals(2, setWithNull.size());
        assertTrue(setWithNull.contains(null));

        Set<String> emptySet = CommonUtil.asSet();
        assertTrue(emptySet.isEmpty());

        final Set<String> immutableSet = set;
        assertThrows(UnsupportedOperationException.class, () -> immutableSet.add("d"));
    }

    @Test
    public void testAsLinkedHashSet() {
        Set<String> set = CommonUtil.toLinkedHashSet("a");
        assertTrue(set instanceof LinkedHashSet);
        assertEquals(1, set.size());

        set = CommonUtil.toLinkedHashSet("a", "b");
        assertEquals(2, set.size());

        set = CommonUtil.toLinkedHashSet("a", "b", "c");
        assertEquals(3, set.size());

        set = CommonUtil.toLinkedHashSet("a", "b", "c", "d");
        assertEquals(4, set.size());

        set = CommonUtil.toLinkedHashSet("a", "b", "c", "d", "e");
        assertEquals(5, set.size());

        set = CommonUtil.toLinkedHashSet("a", "b", "c", "d", "e", "f");
        assertEquals(6, set.size());

        set = CommonUtil.toLinkedHashSet("a", "b", "c", "d", "e", "f", "g");
        assertEquals(7, set.size());

        Iterator<String> iter = set.iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
    }

    @Test
    public void testAsLinkedHashSet_VarArgs() {
        String[] array = { "a", "b", "c" };
        Set<String> set = CommonUtil.toLinkedHashSet(array);
        assertTrue(set instanceof LinkedHashSet);
        assertEquals(3, set.size());
    }

    @Test
    public void testAsSingletonListWithNull() {
        List<String> list = CommonUtil.asSingletonList(null);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertNull(list.get(0));
    }

    @Test
    public void testAsSingletonListWithDifferentTypes() {
        List<Integer> intList = CommonUtil.asSingletonList(42);
        assertEquals(42, intList.get(0));

        List<Double> doubleList = CommonUtil.asSingletonList(3.14);
        assertEquals(3.14, doubleList.get(0));

        List<Object> objList = CommonUtil.asSingletonList(new Object());
        assertNotNull(objList.get(0));
    }

    @Test
    public void testAsSingletonSetWithNull() {
        Set<String> set = CommonUtil.asSingletonSet(null);

        assertNotNull(set);
        assertEquals(1, set.size());
        assertTrue(set.contains(null));
    }

    @Test
    public void testAsSingletonSetWithDifferentTypes() {
        Set<Integer> intSet = CommonUtil.asSingletonSet(100);
        assertTrue(intSet.contains(100));

        Set<Boolean> boolSet = CommonUtil.asSingletonSet(true);
        assertTrue(boolSet.contains(true));
    }

    @Test
    public void testAsSingletonMapWithNullKey() {
        Map<String, String> map = CommonUtil.asSingletonMap(null, "value");

        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals("value", map.get(null));
        assertTrue(map.containsKey(null));
    }

    @Test
    public void testAsSingletonMapWithNullValue() {
        Map<String, String> map = CommonUtil.asSingletonMap("key", null);

        assertNotNull(map);
        assertEquals(1, map.size());
        assertNull(map.get("key"));
        assertTrue(map.containsKey("key"));
    }

    @Test
    public void testAsSingletonMapWithNullKeyAndValue() {
        Map<String, String> map = CommonUtil.asSingletonMap(null, null);

        assertNotNull(map);
        assertEquals(1, map.size());
        assertNull(map.get(null));
        assertTrue(map.containsKey(null));
    }

    @Test
    public void testEmptyListReturnsSameInstance() {
        List<String> list1 = CommonUtil.emptyList();
        List<Integer> list2 = CommonUtil.emptyList();

        assertSame(list1, list2);
    }

    @Test
    public void testEmptySetReturnsSameInstance() {
        Set<String> set1 = CommonUtil.emptySet();
        Set<Integer> set2 = CommonUtil.emptySet();

        assertSame(set1, set2);
    }

    @Test
    public void testEmptySortedSetReturnsSameInstance() {
        SortedSet<String> set1 = CommonUtil.emptySortedSet();
        SortedSet<Integer> set2 = CommonUtil.emptySortedSet();

        assertSame(set1, set2);
    }

    @Test
    public void testEmptyNavigableSetReturnsSameInstance() {
        NavigableSet<String> set1 = CommonUtil.emptyNavigableSet();
        NavigableSet<Integer> set2 = CommonUtil.emptyNavigableSet();

        assertSame(set1, set2);
    }

    @Test
    public void testNewInstance_SimpleClass() {
        String str = CommonUtil.newInstance(String.class);
        assertNotNull(str);
        assertEquals("", str);

        ArrayList<String> list = CommonUtil.newInstance(ArrayList.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testNewInstance_AbstractClass() {
        List<String> list = CommonUtil.newInstance(List.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());

        Map<String, String> map = CommonUtil.newInstance(Map.class);
        assertNotNull(map);
        assertTrue(map.isEmpty());

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(Number.class));
    }

    @Test
    public void testNewCollection_NoSize() {
        Collection<String> list = CommonUtil.newCollection(List.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());

        Collection<String> set = CommonUtil.<String> newCollection(Set.class);
        assertNotNull(set);
        assertTrue(set.isEmpty());

        Collection<String> queue = CommonUtil.newCollection(Queue.class);
        assertNotNull(queue);
        assertTrue(queue.isEmpty());
    }

    @Test
    public void testNewCollection_WithSize() {
        Collection<String> list = CommonUtil.newCollection(ArrayList.class, 10);
        assertNotNull(list);
        assertTrue(list.isEmpty());
        list.add("test");
        assertEquals(1, list.size());

        Collection<String> set = CommonUtil.newCollection(HashSet.class, 10);
        assertNotNull(set);
        assertTrue(set.isEmpty());

        Collection<String> collection = CommonUtil.newCollection(LinkedList.class, 5);
        assertNotNull(collection);
        assertTrue(collection instanceof LinkedList);
    }

    @Test
    public void testNewMap_NoSize() {
        Map<String, Integer> map = CommonUtil.newMap(Map.class);
        assertNotNull(map);
        assertTrue(map.isEmpty());

        Map<String, Integer> hashMap = CommonUtil.newMap(HashMap.class);
        assertNotNull(hashMap);
        assertTrue(hashMap.isEmpty());

        Map<String, Integer> treeMap = CommonUtil.newMap(TreeMap.class);
        assertNotNull(treeMap);
        assertTrue(treeMap.isEmpty());
    }

    @Test
    public void testNewMap_WithSize() {
        Map<String, Integer> map = CommonUtil.newMap(HashMap.class, 10);
        assertNotNull(map);
        assertTrue(map.isEmpty());
        map.put("test", 1);
        assertEquals(1, map.size());

        Map<String, Integer> linkedMap = CommonUtil.newMap(LinkedHashMap.class, 10);
        assertNotNull(linkedMap);
        assertTrue(linkedMap instanceof LinkedHashMap);
    }

    @Test
    public void testNewArray_SingleDimension() {
        int[] intArray = CommonUtil.newArray(int.class, 10);
        assertNotNull(intArray);
        assertEquals(10, intArray.length);

        String[] strArray = CommonUtil.newArray(String.class, 5);
        assertNotNull(strArray);
        assertEquals(5, strArray.length);

        Object[] objArray = CommonUtil.newArray(Object.class, 0);
        assertNotNull(objArray);
        assertEquals(0, objArray.length);

        assertThrows(NegativeArraySizeException.class, () -> CommonUtil.newArray(int.class, -1));
    }

    @Test
    public void testNewArray_MultiDimension() {
        int[][] int2D = CommonUtil.newArray(int.class, 3, 4);
        assertNotNull(int2D);
        assertEquals(3, int2D.length);
        assertEquals(4, int2D[0].length);

        String[][][] str3D = CommonUtil.newArray(String.class, 2, 3, 4);
        assertNotNull(str3D);
        assertEquals(2, str3D.length);
        assertEquals(3, str3D[0].length);
        assertEquals(4, str3D[0][0].length);

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newArray(null, 10));
        assertThrows(NegativeArraySizeException.class, () -> CommonUtil.newArray(int.class, 5, -1));
    }

    @Test
    public void testNewArrayList_NoArgs() {
        ArrayList<String> list = CommonUtil.newArrayList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
        list.add("test");
        assertEquals(1, list.size());
    }

    @Test
    public void testNewArrayList_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c");
        ArrayList<String> list = CommonUtil.newArrayList(source);
        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));

        ArrayList<String> emptyList = CommonUtil.newArrayList(new ArrayList<>());
        assertNotNull(emptyList);
        assertTrue(emptyList.isEmpty());

        ArrayList<String> nullList = CommonUtil.newArrayList(null);
        assertNotNull(nullList);
        assertTrue(nullList.isEmpty());
    }

    @Test
    public void testNewLinkedList_NoArgs() {
        LinkedList<String> list = CommonUtil.newLinkedList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
        list.add("test");
        assertEquals(1, list.size());
    }

    @Test
    public void testNewLinkedList_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c");
        LinkedList<String> list = CommonUtil.newLinkedList(source);
        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals("a", list.getFirst());
        assertEquals("c", list.getLast());

        LinkedList<String> emptyList = CommonUtil.newLinkedList(new ArrayList<>());
        assertNotNull(emptyList);
        assertTrue(emptyList.isEmpty());

        LinkedList<String> nullList = CommonUtil.newLinkedList(null);
        assertNotNull(nullList);
        assertTrue(nullList.isEmpty());
    }

    @Test
    public void testNewHashSet_NoArgs() {
        Set<String> set = CommonUtil.newHashSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        set.add("test");
        assertEquals(1, set.size());
    }

    @Test
    public void testNewHashSet_WithCapacity() {
        Set<String> set = CommonUtil.newHashSet(100);
        assertNotNull(set);
        assertTrue(set.isEmpty());
        set.add("test");
        assertEquals(1, set.size());
    }

    @Test
    public void testNewHashSet_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c", "a");
        Set<String> set = CommonUtil.newHashSet(source);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));

        Set<String> emptySet = CommonUtil.newHashSet(new ArrayList<>());
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());

        Set<String> nullSet = CommonUtil.newHashSet(null);
        assertNotNull(nullSet);
        assertTrue(nullSet.isEmpty());
    }

    @Test
    public void testNewLinkedHashSet_WithCapacity() {
        Set<String> set = CommonUtil.newLinkedHashSet(100);
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertTrue(set instanceof LinkedHashSet);
    }

    @Test
    public void testNewLinkedHashSet_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c");
        Set<String> set = CommonUtil.newLinkedHashSet(source);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertTrue(set instanceof LinkedHashSet);

        Iterator<String> iter = set.iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
    }

    @Test
    public void testNewTreeSet_NoArgs() {
        TreeSet<String> set = CommonUtil.newTreeSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        set.add("b");
        set.add("a");
        set.add("c");
        assertEquals("a", set.first());
        assertEquals("c", set.last());
    }

    @Test
    public void testNewTreeSet_WithComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        TreeSet<String> set = CommonUtil.newTreeSet(reverseComparator);
        assertNotNull(set);
        set.add("a");
        set.add("b");
        set.add("c");
        assertEquals("c", set.first());
        assertEquals("a", set.last());
    }

    @Test
    public void testNewTreeSet_WithCollection() {
        List<String> source = Arrays.asList("b", "a", "c");
        TreeSet<String> set = CommonUtil.newTreeSet(source);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertEquals("a", set.first());
        assertEquals("c", set.last());

        TreeSet<String> emptySet = CommonUtil.newTreeSet(new ArrayList<String>());
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testNewTreeSet_WithSortedSet() {
        SortedSet<String> source = new TreeSet<>();
        source.add("b");
        source.add("a");
        source.add("c");

        TreeSet<String> set = CommonUtil.newTreeSet(source);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertEquals("a", set.first());
        assertEquals("c", set.last());

        assertTrue(CommonUtil.newTreeSet((SortedSet<String>) null).isEmpty());
    }

    @Test
    public void testNewConcurrentHashSet_NoArgs() {
        Set<String> set = CommonUtil.newConcurrentHashSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        set.add("test");
        assertEquals(1, set.size());
    }

    @Test
    public void testNewConcurrentHashSet_WithCapacity() {
        Set<String> set = CommonUtil.newConcurrentHashSet(100);
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    @Test
    public void testNewConcurrentHashSet_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c");
        Set<String> set = CommonUtil.newConcurrentHashSet(source);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
    }

    @Test
    public void testNewMultiset_NoArgs() {
        Multiset<String> multiset = CommonUtil.newMultiset();
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testNewMultiset_WithCapacity() {
        Multiset<String> multiset = CommonUtil.newMultiset(100);
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testNewMultiset_WithMapType() {
        Multiset<String> multiset = CommonUtil.newMultiset(LinkedHashMap.class);
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testNewMultiset_WithMapSupplier() {
        Multiset<String> multiset = CommonUtil.newMultiset(() -> new TreeMap<String, MutableInt>());
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testNewMultiset_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "a", "c", "b", "a");
        Multiset<String> multiset = CommonUtil.newMultiset(source);
        assertNotNull(multiset);
        assertEquals(6, multiset.size());
        assertEquals(3, multiset.getCount("a"));
        assertEquals(2, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
    }

    @Test
    public void testNewArrayDeque_NoArgs() {
        ArrayDeque<String> deque = CommonUtil.newArrayDeque();
        assertNotNull(deque);
        assertTrue(deque.isEmpty());
    }

    @Test
    public void testNewArrayDeque_WithCapacity() {
        ArrayDeque<String> deque = CommonUtil.newArrayDeque(100);
        assertNotNull(deque);
        assertTrue(deque.isEmpty());
    }

    @Test
    public void testNewArrayDeque_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c");
        ArrayDeque<String> deque = CommonUtil.newArrayDeque(source);
        assertNotNull(deque);
        assertEquals(3, deque.size());
        assertEquals("a", deque.getFirst());
        assertEquals("c", deque.getLast());
    }

    @Test
    public void testNewHashMap_NoArgs() {
        Map<String, Integer> map = CommonUtil.newHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertTrue(map instanceof HashMap);
    }

    @Test
    public void testNewHashMap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        Map<String, Integer> map = CommonUtil.newHashMap(source);
        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals(1, map.get("a").intValue());
        assertEquals(2, map.get("b").intValue());

        Map<String, Integer> emptyMap = CommonUtil.newHashMap(new HashMap<>());
        assertNotNull(emptyMap);
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testNewLinkedHashMap_WithCapacity() {
        Map<String, Integer> map = CommonUtil.newLinkedHashMap(100);
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertTrue(map instanceof LinkedHashMap);
    }

    @Test
    public void testNewLinkedHashMap_WithMap() {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("c", 3);

        Map<String, Integer> map = CommonUtil.newLinkedHashMap(source);
        assertNotNull(map);
        assertEquals(3, map.size());

        Iterator<String> iter = map.keySet().iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
    }

    @Test
    public void testNewTreeMap_NoArgs() {
        TreeMap<String, Integer> map = CommonUtil.newTreeMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());

        map.put("b", 2);
        map.put("a", 1);
        map.put("c", 3);
        assertEquals("a", map.firstKey());
        assertEquals("c", map.lastKey());
    }

    @Test
    public void testNewTreeMap_WithComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        TreeMap<String, Integer> map = CommonUtil.newTreeMap(reverseComparator);
        assertNotNull(map);

        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals("c", map.firstKey());
        assertEquals("a", map.lastKey());
    }

    @Test
    public void testNewTreeMap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("b", 2);
        source.put("a", 1);
        source.put("c", 3);

        TreeMap<String, Integer> map = CommonUtil.newTreeMap(source);
        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals("a", map.firstKey());
        assertEquals("c", map.lastKey());
    }

    @Test
    public void testNewTreeMap_WithSortedMap() {
        Map<String, Integer> source = new TreeMap<>();
        source.put("b", 2);
        source.put("a", 1);
        source.put("c", 3);

        TreeMap<String, Integer> map = CommonUtil.newTreeMap(source);
        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals("a", map.firstKey());
        assertEquals("c", map.lastKey());

        assertTrue(CommonUtil.newTreeMap((SortedMap<String, Integer>) null).isEmpty());
    }

    @Test
    public void testNewIdentityHashMap_NoArgs() {
        IdentityHashMap<String, Integer> map = CommonUtil.newIdentityHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewIdentityHashMap_WithCapacity() {
        IdentityHashMap<String, Integer> map = CommonUtil.newIdentityHashMap(100);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewIdentityHashMap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        IdentityHashMap<String, Integer> map = CommonUtil.newIdentityHashMap(source);
        assertNotNull(map);
        assertEquals(2, map.size());
    }

    @Test
    public void testNewConcurrentHashMap_NoArgs() {
        ConcurrentHashMap<String, Integer> map = CommonUtil.newConcurrentHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewConcurrentHashMap_WithCapacity() {
        ConcurrentHashMap<String, Integer> map = CommonUtil.newConcurrentHashMap(100);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewConcurrentHashMap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        ConcurrentHashMap<String, Integer> map = CommonUtil.newConcurrentHashMap(source);
        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals(1, map.get("a").intValue());
        assertEquals(2, map.get("b").intValue());
    }

    @Test
    public void testNewBiMap_NoArgs() {
        BiMap<String, Integer> biMap = CommonUtil.newBiMap();
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewBiMap_WithCapacity() {
        BiMap<String, Integer> biMap = CommonUtil.newBiMap(100);
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewBiMap_WithCapacityAndLoadFactor() {
        BiMap<String, Integer> biMap = CommonUtil.newBiMap(100, 0.75f);
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewBiMap_WithMapTypes() {
        BiMap<String, Integer> biMap = CommonUtil.newBiMap(LinkedHashMap.class, TreeMap.class);
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewBiMap_WithMapSuppliers() {
        BiMap<String, Integer> biMap = CommonUtil.newBiMap(() -> new LinkedHashMap<String, Integer>(), () -> new TreeMap<Integer, String>());
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewListMultimap_NoArgs() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewListMultimap_WithCapacity() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap(100);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewListMultimap_WithMapType() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap(LinkedHashMap.class);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewListMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("a", 3);

        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap(source);
        assertNotNull(multimap);
        assertEquals(2, multimap.totalValueCount());
    }

    @Test
    public void testNewLinkedListMultimap_NoArgs() {
        ListMultimap<String, Integer> multimap = CommonUtil.newLinkedListMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedListMultimap_WithCapacity() {
        ListMultimap<String, Integer> multimap = CommonUtil.newLinkedListMultimap(100);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedListMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        ListMultimap<String, Integer> multimap = CommonUtil.newLinkedListMultimap(source);
        assertNotNull(multimap);
        assertEquals(2, multimap.totalValueCount());
    }

    @Test
    public void testNewSortedListMultimap_NoArgs() {
        ListMultimap<String, Integer> multimap = CommonUtil.newSortedListMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSortedListMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("b", 2);
        source.put("a", 1);
        source.put("c", 3);

        ListMultimap<String, Integer> multimap = CommonUtil.newSortedListMultimap(source);
        assertNotNull(multimap);
        assertEquals(3, multimap.totalValueCount());

        Iterator<String> iter = multimap.keySet().iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
    }

    @Test
    public void testNewSetMultimap_NoArgs() {
        SetMultimap<String, Integer> multimap = CommonUtil.newSetMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSetMultimap_WithCapacity() {
        SetMultimap<String, Integer> multimap = CommonUtil.newSetMultimap(100);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSetMultimap_WithMapType() {
        SetMultimap<String, Integer> multimap = CommonUtil.newSetMultimap(LinkedHashMap.class);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedSetMultimap_NoArgs() {
        SetMultimap<String, Integer> multimap = CommonUtil.newLinkedSetMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedSetMultimap_WithCapacity() {
        SetMultimap<String, Integer> multimap = CommonUtil.newLinkedSetMultimap(100);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedSetMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        SetMultimap<String, Integer> multimap = CommonUtil.newLinkedSetMultimap(source);
        assertNotNull(multimap);
        assertEquals(2, multimap.totalValueCount());
    }

    @Test
    public void testNewSortedSetMultimap_NoArgs() {
        SetMultimap<String, Integer> multimap = CommonUtil.newSortedSetMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSortedSetMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("b", 2);
        source.put("a", 1);
        source.put("c", 3);

        SetMultimap<String, Integer> multimap = CommonUtil.newSortedSetMultimap(source);
        assertNotNull(multimap);
        assertEquals(3, multimap.totalValueCount());

        Iterator<String> iter = multimap.keySet().iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
    }

    @Test
    public void testNewEmptyDataset_WithColumnNames() {
        List<String> columnNames = Arrays.asList("col1", "col2", "col3");
        Dataset ds = CommonUtil.newEmptyDataset(columnNames);
        assertNotNull(ds);
        assertTrue(ds.isEmpty());
        assertEquals(0, ds.size());
        assertEquals(3, ds.columnCount());
        assertEquals(columnNames, ds.columnNames());
    }

    @Test
    public void testNewEmptyDataset_WithProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("key", "value");

        List<String> columnNames = Arrays.asList("col1", "col2");
        Dataset ds = CommonUtil.newEmptyDataset(columnNames, properties);
        assertNotNull(ds);
        assertEquals(0, ds.size());
        assertEquals(2, ds.columnCount());
        assertEquals("value", ds.getProperties().get("key"));
    }

    @Test
    public void testNewDataset_WithColumnNamesAndRows() {
        List<String> columnNames = Arrays.asList("name", "age");
        List<Object[]> rows = Arrays.asList(new Object[] { "John", 30 }, new Object[] { "Jane", 25 });

        Dataset ds = CommonUtil.newDataset(columnNames, rows);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
        assertEquals(columnNames, ds.columnNames());
    }

    @Test
    public void testNewDataset_WithColumnNamesAndRowArray() {
        List<String> columnNames = Arrays.asList("name", "age");
        Object[][] rows = new Object[][] { { "John", 30 }, { "Jane", 25 } };

        Dataset ds = CommonUtil.newDataset(columnNames, rows);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
    }

    @Test
    public void testNewDataset_FromKeyValueMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("John", 30);
        map.put("Jane", 25);

        Dataset ds = CommonUtil.newDataset("Name", "Age", map);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
        assertEquals(Arrays.asList("Name", "Age"), ds.columnNames());
    }

    @Test
    public void testNewDataset_FromMapOfCollections() {
        Map<String, List<Object>> map = new LinkedHashMap<>();
        map.put("name", Arrays.asList("John", "Jane"));
        map.put("age", Arrays.asList(30, 25));

        Dataset ds = CommonUtil.newDataset(map);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
        assertEquals(Arrays.asList("name", "age"), ds.columnNames());
    }

    @Test
    public void testNewDataset_SingleColumn() {
        List<String> values = Arrays.asList("A", "B", "C");
        Dataset ds = CommonUtil.newDataset("Letter", values);
        assertNotNull(ds);
        assertEquals(3, ds.size());
        assertEquals(1, ds.columnCount());
        assertEquals("Letter", ds.columnNames().get(0));
    }

    @Test
    public void testMerge_TwoDatasets() {
        Dataset ds1 = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(new Object[] { "A" }, new Object[] { "B" }));

        Dataset ds2 = CommonUtil.newDataset(Arrays.asList("col2"), Arrays.asList(new Object[] { 1 }, new Object[] { 2 }));

        Dataset merged = CommonUtil.merge(ds1, ds2);
        assertNotNull(merged);
        assertEquals(4, merged.size());
        assertEquals(2, merged.columnCount());

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(null, ds2));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(ds1, null));
    }

    @Test
    public void testMerge_ThreeDatasets() {
        Dataset ds1 = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(new Object[] { "A" }));
        Dataset ds2 = CommonUtil.newDataset(Arrays.asList("col2"), Arrays.asList(new Object[] { 1 }));
        Dataset ds3 = CommonUtil.newDataset(Arrays.asList("col3"), Arrays.asList(new Object[] { true }));

        Dataset merged = CommonUtil.merge(ds1, ds2, ds3);
        assertNotNull(merged);
        assertEquals(3, merged.size());
        assertEquals(3, merged.columnCount());

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(null, ds2, ds3));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(ds1, null, ds3));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(ds1, ds2, (Dataset) null));
    }

    @Test
    public void testMerge_CollectionOfDatasets() {
        List<Dataset> datasets = Arrays.asList(CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(new Object[] { "A" })),
                CommonUtil.newDataset(Arrays.asList("col2"), Arrays.asList(new Object[] { 1 })),
                CommonUtil.newDataset(Arrays.asList("col3"), Arrays.asList(new Object[] { true })));

        Dataset merged = CommonUtil.merge(datasets);
        assertNotNull(merged);
        assertEquals(3, merged.size());
        assertEquals(3, merged.columnCount());

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(new ArrayList<>()));

        Dataset single = CommonUtil.merge(Arrays.asList(datasets.get(0)));
        assertNotNull(single);
        assertEquals(1, single.size());

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge((Collection<Dataset>) null));
    }

    @Test
    public void testMerge_WithRequiresSameColumns() {
        Dataset ds1 = CommonUtil.newDataset(Arrays.asList("col1", "col2"), CommonUtil.asSingletonList(new Object[] { "A", 1 }));

        Dataset ds2 = CommonUtil.newDataset(Arrays.asList("col1", "col2"), CommonUtil.asSingletonList(new Object[] { "B", 2 }));

        Dataset ds3 = CommonUtil.newDataset(Arrays.asList("col1", "col3"), CommonUtil.asSingletonList(new Object[] { "C", 3 }));

        Dataset merged = CommonUtil.merge(Arrays.asList(ds1, ds2), true);
        assertNotNull(merged);
        assertEquals(2, merged.size());

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(Arrays.asList(ds1, ds3), true));
    }

    @Test
    public void testToArray_CollectionRange() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Object[] array = CommonUtil.toArray(list, 1, 4);
        assertArrayEquals(new Object[] { "b", "c", "d" }, array);

        Object[] fullArray = CommonUtil.toArray(list, 0, list.size());
        assertArrayEquals(new Object[] { "a", "b", "c", "d", "e" }, fullArray);

        Object[] emptyArray = CommonUtil.toArray(list, 2, 2);
        assertEquals(0, emptyArray.length);

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toArray(list, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toArray(list, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toArray(list, 3, 2));
    }

    @Test
    public void testToArray_01() {
        {
            Collection<String> list = CommonUtil.toLinkedHashSet("a", "b", "c", "d", "e");
            assertArrayEquals(new Object[] { "b", "c", "d" }, CommonUtil.toArray(list, 1, 4));
            assertArrayEquals(new Object[] { "b", "c", "d" }, CommonUtil.toArray(list, 1, 4, new String[1]));
            assertArrayEquals(new Object[] { "b", "c", "d" }, CommonUtil.toArray(list, 1, 4, String[]::new));
            assertArrayEquals(new Object[] { "b", "c", "d" }, CommonUtil.toArray(list, 1, 4, String[].class));
        }
        {
            Collection<Boolean> c = CommonUtil.toLinkedList(true, true, false, null, true);
            assertArrayEquals(new boolean[] { true, false, false }, CommonUtil.toBooleanArray(c, 1, 4, false));
        }
        {
            Collection<Byte> c = CommonUtil.toLinkedList((byte) 1, (byte) 2, (byte) 3, null, (byte) 4);
            assertArrayEquals(new byte[] { 2, 3, 0 }, CommonUtil.toByteArray(c, 1, 4, (byte) 0));
        }
        {
            Collection<Short> c = CommonUtil.toLinkedList((short) 1, (short) 2, (short) 3, null, (short) 4);
            assertArrayEquals(new short[] { 2, 3, 0 }, CommonUtil.toShortArray(c, 1, 4, (short) 0));
        }
        {
            Collection<Integer> c = CommonUtil.toLinkedList(1, 2, 3, null, 4);
            assertArrayEquals(new int[] { 2, 3, 0 }, CommonUtil.toIntArray(c, 1, 4, 0));
        }
        {
            Collection<Long> c = CommonUtil.toLinkedList(1L, 2L, 3L, null, 4L);
            assertArrayEquals(new long[] { 2L, 3L, 0L }, CommonUtil.toLongArray(c, 1, 4, 0L));
        }
        {
            Collection<Float> c = CommonUtil.toLinkedList(1.0f, 2.0f, 3.0f, null, 4.0f);
            assertArrayEquals(new float[] { 2.0f, 3.0f, 0.0f }, CommonUtil.toFloatArray(c, 1, 4, 0.0f));
        }
        {
            Collection<Double> c = CommonUtil.toLinkedList(1.0d, 2.0d, 3.0d, null, 4.0d);
            assertArrayEquals(new double[] { 2.0d, 3.0d, 0.0d }, CommonUtil.toDoubleArray(c, 1, 4, 0.0d));
        }
    }

    @Test
    public void testToArray_WithTargetArray() {
        List<String> list = Arrays.asList("a", "b", "c");

        String[] target = new String[5];
        String[] result = CommonUtil.toArray(list, target);
        assertSame(target, result);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
        assertNull(result[3]);

        String[] smallTarget = new String[2];
        String[] newResult = CommonUtil.toArray(list, smallTarget);
        assertNotSame(smallTarget, newResult);
        assertEquals(3, newResult.length);

        String[] emptyResult = CommonUtil.toArray(new ArrayList<String>(), new String[0]);
        assertEquals(0, emptyResult.length);

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.toArray(list, (String[]) null));
    }

    @Test
    public void testToArray_RangeWithTargetArray() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        String[] target = new String[3];
        String[] result = CommonUtil.toArray(list, 1, 4, target);
        assertSame(target, result);
        assertArrayEquals(new String[] { "b", "c", "d" }, result);

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.toArray(list, 1, 4, (String[]) null));
    }

    @Test
    public void testToArray_WithArraySupplier() {
        List<String> list = Arrays.asList("a", "b", "c");
        String[] array = CommonUtil.toArray(list, String[]::new);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);

        String[] emptyArray = CommonUtil.toArray(new ArrayList<String>(), String[]::new);
        assertEquals(0, emptyArray.length);
    }

    @Test
    public void testToArray_RangeWithArraySupplier() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        String[] array = CommonUtil.toArray(list, 1, 4, String[]::new);
        assertArrayEquals(new String[] { "b", "c", "d" }, array);
    }

    @Test
    public void testToArray_WithTargetType() {
        List<String> list = Arrays.asList("a", "b", "c");
        String[] array = CommonUtil.toArray(list, String[].class);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);

        String[] emptyArray = CommonUtil.toArray(new ArrayList<String>(), String[].class);
        assertEquals(0, emptyArray.length);

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.toArray(list, (String[]) null));
    }

    @Test
    public void testToArray_RangeWithTargetType() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        String[] array = CommonUtil.toArray(list, 1, 4, String[].class);
        assertArrayEquals(new String[] { "b", "c", "d" }, array);

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.toArray(list, 1, 4, (String[]) null));
    }

    @Test
    public void testToBooleanArray_Range() {
        List<Boolean> list = Arrays.asList(true, false, true, null, false);

        boolean[] array = CommonUtil.toBooleanArray(list, 1, 4);
        assertArrayEquals(new boolean[] { false, true, false }, array);

        boolean[] arrayWithDefault = CommonUtil.toBooleanArray(list, 1, 4, true);
        assertArrayEquals(new boolean[] { false, true, true }, arrayWithDefault);

        boolean[] emptyArray = CommonUtil.toBooleanArray(list, 2, 2);
        assertEquals(0, emptyArray.length);

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toBooleanArray(list, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toBooleanArray(list, 0, 6));
    }

    @Test
    public void testToBooleanArray_FromByteArray() {
        byte[] bytes = { 0, 1, -1, 127, -128 };
        boolean[] expected = { false, true, false, true, false };
        assertArrayEquals(expected, CommonUtil.toBooleanArray(bytes));

        assertArrayEquals(new boolean[0], CommonUtil.toBooleanArray(new byte[0]));
        assertArrayEquals(new boolean[0], CommonUtil.toBooleanArray((byte[]) null));
    }

    @Test
    public void testToBooleanArray_FromIntArray() {
        int[] ints = { 0, 1, -1, 100, -100 };
        boolean[] expected = { false, true, false, true, false };
        assertArrayEquals(expected, CommonUtil.toBooleanArray(ints));

        assertArrayEquals(new boolean[0], CommonUtil.toBooleanArray(new int[0]));
        assertArrayEquals(new boolean[0], CommonUtil.toBooleanArray((int[]) null));
    }

    @Test
    public void testToCharArray_Range() {
        List<Character> list = Arrays.asList('a', 'b', 'c', null, 'd');

        char[] array = CommonUtil.toCharArray(list, 1, 4);
        assertArrayEquals(new char[] { 'b', 'c', '\0' }, array);

        char[] arrayWithDefault = CommonUtil.toCharArray(list, 1, 4, 'X');
        assertArrayEquals(new char[] { 'b', 'c', 'X' }, arrayWithDefault);
    }

    @Test
    public void testToByteArray_Range() {
        List<Number> list = Arrays.asList((byte) 1, 2, 3L, null, 5.0);

        byte[] array = CommonUtil.toByteArray(list, 1, 4);
        assertArrayEquals(new byte[] { 2, 3, 0 }, array);

        byte[] arrayWithDefault = CommonUtil.toByteArray(list, 1, 4, (byte) -1);
        assertArrayEquals(new byte[] { 2, 3, -1 }, arrayWithDefault);
    }

    @Test
    public void testToByteArray_FromBooleanArray() {
        boolean[] bools = { true, false, true, false };
        byte[] expected = { 1, 0, 1, 0 };
        assertArrayEquals(expected, CommonUtil.toByteArray(bools));

        assertArrayEquals(new byte[0], CommonUtil.toByteArray(new boolean[0]));
        assertArrayEquals(new byte[0], CommonUtil.toByteArray((boolean[]) null));
    }

    @Test
    public void testToShortArray_Range() {
        List<Number> list = Arrays.asList((short) 1, 2, 3L, null, 5.0);

        short[] array = CommonUtil.toShortArray(list, 1, 4);
        assertArrayEquals(new short[] { 2, 3, 0 }, array);

        short[] arrayWithDefault = CommonUtil.toShortArray(list, 1, 4, (short) -1);
        assertArrayEquals(new short[] { 2, 3, -1 }, arrayWithDefault);
    }

    @Test
    public void testToIntArray_Range() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        int[] array = CommonUtil.toIntArray(list, 1, 4);
        assertArrayEquals(new int[] { 2, 3, 0 }, array);

        int[] arrayWithDefault = CommonUtil.toIntArray(list, 1, 4, -1);
        assertArrayEquals(new int[] { 2, 3, -1 }, arrayWithDefault);
    }

    @Test
    public void testToIntArray_FromCharArray() {
        char[] chars = { 'A', 'B', 'C' };
        int[] expected = { 65, 66, 67 };
        assertArrayEquals(expected, CommonUtil.toIntArray(chars));

        assertArrayEquals(new int[0], CommonUtil.toIntArray(new char[0]));
        assertArrayEquals(new int[0], CommonUtil.toIntArray((char[]) null));
    }

    @Test
    public void testToIntArray_FromBooleanArray() {
        boolean[] bools = { true, false, true, false };
        int[] expected = { 1, 0, 1, 0 };
        assertArrayEquals(expected, CommonUtil.toIntArray(bools));

        assertArrayEquals(new int[0], CommonUtil.toIntArray(new boolean[0]));
        assertArrayEquals(new int[0], CommonUtil.toIntArray((boolean[]) null));
    }

    @Test
    public void testToLongArray_Range() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        long[] array = CommonUtil.toLongArray(list, 1, 4);
        assertArrayEquals(new long[] { 2, 3, 0 }, array);

        long[] arrayWithDefault = CommonUtil.toLongArray(list, 1, 4, -1L);
        assertArrayEquals(new long[] { 2, 3, -1 }, arrayWithDefault);
    }

    @Test
    public void testToFloatArray_Range() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        float[] array = CommonUtil.toFloatArray(list, 1, 4);
        assertArrayEquals(new float[] { 2, 3, 0 }, array, 0.0f);

        float[] arrayWithDefault = CommonUtil.toFloatArray(list, 1, 4, -1.0f);
        assertArrayEquals(new float[] { 2, 3, -1 }, arrayWithDefault, 0.0f);
    }

    @Test
    public void testToDoubleArray_Range() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        double[] array = CommonUtil.toDoubleArray(list, 1, 4);
        assertArrayEquals(new double[] { 2, 3, 0 }, array, 0.0);

        double[] arrayWithDefault = CommonUtil.toDoubleArray(list, 1, 4, -1.0);
        assertArrayEquals(new double[] { 2, 3, -1 }, arrayWithDefault, 0.0);
    }

    @Test
    public void testAnyBlank_CharSequenceArray() {
        assertFalse(CommonUtil.anyBlank((CharSequence[]) null));
        assertFalse(CommonUtil.anyBlank(new CharSequence[0]));
        assertTrue(CommonUtil.anyBlank((CharSequence) null));
        assertTrue(CommonUtil.anyBlank(null, "foo"));
        assertTrue(CommonUtil.anyBlank("", "bar"));
        assertTrue(CommonUtil.anyBlank("bob", ""));
        assertTrue(CommonUtil.anyBlank("  bob  ", null));
        assertTrue(CommonUtil.anyBlank(" ", "bar"));
        assertFalse(CommonUtil.anyBlank("foo", "bar"));
        assertFalse(CommonUtil.anyBlank(new String[] {}));
        assertTrue(CommonUtil.anyBlank(new String[] { "" }));
    }

    @Test
    public void testAnyBlank_Iterable() {
        assertFalse(CommonUtil.anyBlank((Iterable<CharSequence>) null));
        assertFalse(CommonUtil.anyBlank(new ArrayList<>()));

        List<CharSequence> list = new ArrayList<>();
        list.add("test");
        assertFalse(CommonUtil.anyBlank(list));

        list.add(null);
        assertTrue(CommonUtil.anyBlank(list));

        list.clear();
        list.add("");
        assertTrue(CommonUtil.anyBlank(list));

        list.clear();
        list.add(" ");
        assertTrue(CommonUtil.anyBlank(list));
    }

    @Test
    public void testAllNull_ObjectArray() {
        assertTrue(CommonUtil.allNull((Object[]) null));
        assertTrue(CommonUtil.allNull(new Object[0]));
        assertTrue(CommonUtil.allNull(new Object[] { null, null, null }));
        assertFalse(CommonUtil.allNull(new Object[] { null, "test", null }));
        assertFalse(CommonUtil.allNull(new Object[] { "test1", "test2" }));
    }

    @Test
    public void testAllEmpty_CharSequenceArray() {
        assertTrue(CommonUtil.allEmpty((CharSequence[]) null));
        assertTrue(CommonUtil.allEmpty(new CharSequence[0]));
        assertTrue(CommonUtil.allEmpty(null, ""));
        assertFalse(CommonUtil.allEmpty(null, "foo"));
        assertFalse(CommonUtil.allEmpty("", "bar"));
        assertFalse(CommonUtil.allEmpty("bob", ""));
        assertFalse(CommonUtil.allEmpty("  bob  ", null));
        assertFalse(CommonUtil.allEmpty(" ", "bar"));
        assertFalse(CommonUtil.allEmpty("foo", "bar"));
    }

    @Test
    public void testAllBlank_CharSequenceArray() {
        assertTrue(CommonUtil.allBlank((CharSequence[]) null));
        assertTrue(CommonUtil.allBlank(new CharSequence[0]));
        assertTrue(CommonUtil.allBlank(null, null));
        assertTrue(CommonUtil.allBlank("", " "));
        assertFalse(CommonUtil.allBlank(null, "foo"));
        assertFalse(CommonUtil.allBlank("", "bar"));
        assertFalse(CommonUtil.allBlank("bob", ""));
        assertFalse(CommonUtil.allBlank("  bob  ", null));
        assertFalse(CommonUtil.allBlank(" ", "bar"));
        assertFalse(CommonUtil.allBlank("foo", "bar"));
    }

    @Test
    public void testNullToEmpty_PrimitiveArrays() {
        assertArrayEquals(new boolean[0], CommonUtil.nullToEmpty((boolean[]) null));
        assertArrayEquals(new char[0], CommonUtil.nullToEmpty((char[]) null));
        assertArrayEquals(new byte[0], CommonUtil.nullToEmpty((byte[]) null));
        assertArrayEquals(new short[0], CommonUtil.nullToEmpty((short[]) null));
        assertArrayEquals(new int[0], CommonUtil.nullToEmpty((int[]) null));
        assertArrayEquals(new long[0], CommonUtil.nullToEmpty((long[]) null));
        assertArrayEquals(new float[0], CommonUtil.nullToEmpty((float[]) null), 0.0f);
        assertArrayEquals(new double[0], CommonUtil.nullToEmpty((double[]) null), 0.0);

        boolean[] boolArray = { true, false };
        assertSame(boolArray, CommonUtil.nullToEmpty(boolArray));
    }

    @Test
    public void testNullToEmpty_ObjectArrays() {
        assertArrayEquals(new BigInteger[0], CommonUtil.nullToEmpty((BigInteger[]) null));
        assertArrayEquals(new BigDecimal[0], CommonUtil.nullToEmpty((BigDecimal[]) null));
        assertArrayEquals(new String[0], CommonUtil.nullToEmpty((String[]) null));
        assertArrayEquals(new java.util.Date[0], CommonUtil.nullToEmpty((java.util.Date[]) null));
        assertArrayEquals(new java.sql.Date[0], CommonUtil.nullToEmpty((java.sql.Date[]) null));
        assertArrayEquals(new java.sql.Time[0], CommonUtil.nullToEmpty((java.sql.Time[]) null));
        assertArrayEquals(new java.sql.Timestamp[0], CommonUtil.nullToEmpty((java.sql.Timestamp[]) null));
        assertArrayEquals(new Calendar[0], CommonUtil.nullToEmpty((Calendar[]) null));
        assertArrayEquals(new Object[0], CommonUtil.nullToEmpty((Object[]) null));

        String[] strArray = { "test1", "test2" };
        assertSame(strArray, CommonUtil.nullToEmpty(strArray));
    }

    @Test
    public void testNullToEmpty_ImmutableCollections() {
        ImmutableCollection<String> nullColl = null;
        ImmutableCollection<String> emptyColl = CommonUtil.nullToEmpty(nullColl);
        assertNotNull(emptyColl);
        assertTrue(emptyColl.isEmpty());

        ImmutableList<String> nullList = null;
        ImmutableList<String> emptyList = CommonUtil.nullToEmpty(nullList);
        assertNotNull(emptyList);
        assertTrue(emptyList.isEmpty());

        ImmutableSet<String> nullSet = null;
        ImmutableSet<String> emptySet = CommonUtil.nullToEmpty(nullSet);
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());

        ImmutableSortedSet<String> nullSortedSet = null;
        ImmutableSortedSet<String> emptySortedSet = CommonUtil.nullToEmpty(nullSortedSet);
        assertNotNull(emptySortedSet);
        assertTrue(emptySortedSet.isEmpty());

        ImmutableNavigableSet<String> nullNavSet = null;
        ImmutableNavigableSet<String> emptyNavSet = CommonUtil.nullToEmpty(nullNavSet);
        assertNotNull(emptyNavSet);
        assertTrue(emptyNavSet.isEmpty());

        ImmutableMap<String, String> nullMap = null;
        ImmutableMap<String, String> emptyMap = CommonUtil.nullToEmpty(nullMap);
        assertNotNull(emptyMap);
        assertTrue(emptyMap.isEmpty());

        ImmutableSortedMap<String, String> nullSortedMap = null;
        ImmutableSortedMap<String, String> emptySortedMap = CommonUtil.nullToEmpty(nullSortedMap);
        assertNotNull(emptySortedMap);
        assertTrue(emptySortedMap.isEmpty());

        ImmutableNavigableMap<String, String> nullNavMap = null;
        ImmutableNavigableMap<String, String> emptyNavMap = CommonUtil.nullToEmpty(nullNavMap);
        assertNotNull(emptyNavMap);
        assertTrue(emptyNavMap.isEmpty());

        ImmutableBiMap<String, String> nullBiMap = null;
        ImmutableBiMap<String, String> emptyBiMap = CommonUtil.nullToEmpty(nullBiMap);
        assertNotNull(emptyBiMap);
        assertTrue(emptyBiMap.isEmpty());
    }

    @Test
    public void testDefaultIfNull_Primitives() {
        assertEquals(false, CommonUtil.defaultIfNull((Boolean) null));
        assertEquals(true, CommonUtil.defaultIfNull(Boolean.TRUE));
        assertEquals(true, CommonUtil.defaultIfNull((Boolean) null, true));
        assertEquals(false, CommonUtil.defaultIfNull(Boolean.FALSE, true));

        assertEquals('\0', CommonUtil.defaultIfNull((Character) null));
        assertEquals('a', CommonUtil.defaultIfNull(Character.valueOf('a')));
        assertEquals('b', CommonUtil.defaultIfNull((Character) null, 'b'));
        assertEquals('a', CommonUtil.defaultIfNull(Character.valueOf('a'), 'b'));

        assertEquals((byte) 0, CommonUtil.defaultIfNull((Byte) null));
        assertEquals((byte) 5, CommonUtil.defaultIfNull(Byte.valueOf((byte) 5)));
        assertEquals((byte) 10, CommonUtil.defaultIfNull((Byte) null, (byte) 10));
        assertEquals((byte) 5, CommonUtil.defaultIfNull(Byte.valueOf((byte) 5), (byte) 10));

        assertEquals((short) 0, CommonUtil.defaultIfNull((Short) null));
        assertEquals((short) 5, CommonUtil.defaultIfNull(Short.valueOf((short) 5)));
        assertEquals((short) 10, CommonUtil.defaultIfNull((Short) null, (short) 10));
        assertEquals((short) 5, CommonUtil.defaultIfNull(Short.valueOf((short) 5), (short) 10));

        assertEquals(0, CommonUtil.defaultIfNull((Integer) null));
        assertEquals(5, CommonUtil.defaultIfNull(Integer.valueOf(5)));
        assertEquals(10, CommonUtil.defaultIfNull((Integer) null, 10));
        assertEquals(5, CommonUtil.defaultIfNull(Integer.valueOf(5), 10));

        assertEquals(0L, CommonUtil.defaultIfNull((Long) null));
        assertEquals(5L, CommonUtil.defaultIfNull(Long.valueOf(5L)));
        assertEquals(10L, CommonUtil.defaultIfNull((Long) null, 10L));
        assertEquals(5L, CommonUtil.defaultIfNull(Long.valueOf(5L), 10L));

        assertEquals(0f, CommonUtil.defaultIfNull((Float) null), 0.0f);
        assertEquals(5f, CommonUtil.defaultIfNull(Float.valueOf(5f)), 0.0f);
        assertEquals(10f, CommonUtil.defaultIfNull((Float) null, 10f), 0.0f);
        assertEquals(5f, CommonUtil.defaultIfNull(Float.valueOf(5f), 10f), 0.0f);

        assertEquals(0d, CommonUtil.defaultIfNull((Double) null), 0.0);
        assertEquals(5d, CommonUtil.defaultIfNull(Double.valueOf(5d)), 0.0);
        assertEquals(10d, CommonUtil.defaultIfNull((Double) null, 10d), 0.0);
        assertEquals(5d, CommonUtil.defaultIfNull(Double.valueOf(5d), 10d), 0.0);
    }

    @Test
    public void testDefaultIfNull_Supplier() {
        String nullStr = null;
        assertEquals("default", CommonUtil.defaultIfNull(nullStr, Fn.s(() -> "default")));
        assertEquals("test", CommonUtil.defaultIfNull("test", Fn.s(() -> "default")));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfNull(nullStr, () -> null));
    }

    @Test
    public void testDefaultIfEmpty_CharSequenceSupplier() {
        assertEquals("default", CommonUtil.defaultIfEmpty("", Fn.s(Fn.s(() -> "default"))));
        assertEquals("default", CommonUtil.defaultIfEmpty((String) null, Fn.s(() -> "default")));
        assertEquals("test", CommonUtil.defaultIfEmpty("test", Fn.s(() -> "default")));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty("", (Supplier<? extends String>) () -> ""));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty("", (Supplier<? extends String>) () -> null));
    }

    @Test
    public void testDefaultIfBlank_CharSequence() {
        assertEquals("default", CommonUtil.defaultIfBlank("", "default"));
        assertEquals("default", CommonUtil.defaultIfBlank(" ", "default"));
        assertEquals("default", CommonUtil.defaultIfBlank(null, "default"));
        assertEquals("test", CommonUtil.defaultIfBlank("test", "default"));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfBlank(" ", " "));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfBlank(" ", (String) null));
    }

    @Test
    public void testDefaultIfBlank_CharSequenceSupplier() {
        assertEquals("default", CommonUtil.defaultIfBlank("", Fn.s(() -> "default")));
        assertEquals("default", CommonUtil.defaultIfBlank(" ", Fn.s(() -> "default")));
        assertEquals("default", CommonUtil.defaultIfBlank(null, Fn.s(() -> "default")));
        assertEquals("test", CommonUtil.defaultIfBlank("test", Fn.s(() -> "default")));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfBlank(" ", (Supplier<? extends String>) () -> " "));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfBlank(" ", (Supplier<? extends String>) () -> null));
    }

    @Test
    public void testDefaultValueOf_WithNonNullForPrimitiveWrapper() {
        assertEquals(false, CommonUtil.defaultValueOf(Boolean.class, true));
        assertEquals(null, CommonUtil.defaultValueOf(Boolean.class, false));
        assertEquals('\0', CommonUtil.defaultValueOf(Character.class, true));
        assertEquals(null, CommonUtil.defaultValueOf(Character.class, false));
        assertEquals((byte) 0, CommonUtil.defaultValueOf(Byte.class, true));
        assertEquals(null, CommonUtil.defaultValueOf(Byte.class, false));
        assertEquals((short) 0, CommonUtil.defaultValueOf(Short.class, true));
        assertEquals(null, CommonUtil.defaultValueOf(Short.class, false));
        assertEquals(0, CommonUtil.defaultValueOf(Integer.class, true));
        assertEquals(null, CommonUtil.defaultValueOf(Integer.class, false));
        assertEquals(0L, CommonUtil.defaultValueOf(Long.class, true));
        assertEquals(null, CommonUtil.defaultValueOf(Long.class, false));
        assertEquals(0f, CommonUtil.defaultValueOf(Float.class, true), 0.0f);
        assertEquals(null, CommonUtil.defaultValueOf(Float.class, false));
        assertEquals(0d, CommonUtil.defaultValueOf(Double.class, true), 0.0);
        assertEquals(null, CommonUtil.defaultValueOf(Double.class, false));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultValueOf(null, true));
    }

    @Test
    public void testTypeOf_String() {
        Type<String> stringType = CommonUtil.typeOf("java.lang.String");
        assertNotNull(stringType);
        assertEquals(String.class, stringType.javaType());

        Type<Integer> intType = CommonUtil.typeOf("int");
        assertNotNull(intType);
        assertEquals(int.class, intType.javaType());

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.typeOf((String) null));
    }

    @Test
    public void testTypeOf_Class() {
        Type<String> stringType = CommonUtil.typeOf(String.class);
        assertNotNull(stringType);
        assertEquals(String.class, stringType.javaType());

        Type<Integer> intType = CommonUtil.typeOf(int.class);
        assertNotNull(intType);
        assertEquals(int.class, intType.javaType());

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.typeOf((Class<?>) null));
    }

    @Test
    public void testStringOf_Primitives() {
        assertEquals("true", CommonUtil.stringOf(true));
        assertEquals("false", CommonUtil.stringOf(false));

        assertEquals("a", CommonUtil.stringOf('a'));
        assertEquals("A", CommonUtil.stringOf('A'));
        assertEquals("0", CommonUtil.stringOf('0'));

        assertEquals("0", CommonUtil.stringOf((byte) 0));
        assertEquals("127", CommonUtil.stringOf((byte) 127));
        assertEquals("-128", CommonUtil.stringOf((byte) -128));

        assertEquals("0", CommonUtil.stringOf((short) 0));
        assertEquals("32767", CommonUtil.stringOf(Short.MAX_VALUE));
        assertEquals("-32768", CommonUtil.stringOf(Short.MIN_VALUE));

        assertEquals("0", CommonUtil.stringOf(0));
        assertEquals("2147483647", CommonUtil.stringOf(Integer.MAX_VALUE));
        assertEquals("-2147483648", CommonUtil.stringOf(Integer.MIN_VALUE));

        assertEquals("0", CommonUtil.stringOf(0L));
        assertEquals("9223372036854775807", CommonUtil.stringOf(Long.MAX_VALUE));
        assertEquals("-9223372036854775808", CommonUtil.stringOf(Long.MIN_VALUE));

        assertEquals("0.0", CommonUtil.stringOf(0.0f));
        assertEquals("3.14", CommonUtil.stringOf(3.14f));
        assertEquals("-3.14", CommonUtil.stringOf(-3.14f));

        assertEquals("0.0", CommonUtil.stringOf(0.0d));
        assertEquals("3.14159", CommonUtil.stringOf(3.14159d));
        assertEquals("-3.14159", CommonUtil.stringOf(-3.14159d));
    }

    @Test
    public void testConvert_Basic() {
        assertEquals(0, CommonUtil.convert(null, int.class));
        assertEquals(null, CommonUtil.convert(null, Integer.class));
        assertEquals(null, CommonUtil.convert(null, String.class));

        assertEquals(Integer.valueOf(123), CommonUtil.convert("123", Integer.class));
        assertEquals(Long.valueOf(123L), CommonUtil.convert("123", Long.class));
        assertEquals(Double.valueOf(3.14), CommonUtil.convert("3.14", Double.class));
        assertEquals(Boolean.TRUE, CommonUtil.convert("true", Boolean.class));

        assertEquals(Integer.valueOf(123), CommonUtil.convert(123L, Integer.class));
        assertEquals(Long.valueOf(123L), CommonUtil.convert(123, Long.class));
        assertEquals(Float.valueOf(3.14f), CommonUtil.convert(3.14d, Float.class));
        assertEquals(Double.valueOf(3.14d), CommonUtil.convert(3.14f, Double.class));

        assertEquals(true, CommonUtil.convert(1, boolean.class));
        assertEquals(false, CommonUtil.convert(0, boolean.class));
        assertEquals(true, CommonUtil.convert(5L, Boolean.class));

        assertEquals(Character.valueOf('A'), CommonUtil.convert(65, Character.class));
        assertEquals(Integer.valueOf(65), CommonUtil.convert('A', Integer.class));
    }

    @Test
    public void testConvert_Collections() {
        List<String> strList = Arrays.asList("1", "2", "3");
        List<String> convertedList = CommonUtil.convert(strList, List.class);
        assertEquals(strList, convertedList);

        Set<String> strSet = new HashSet<>(Arrays.asList("1", "2", "3"));
        Set<String> convertedSet = CommonUtil.convert(strSet, Set.class);
        assertEquals(strSet, convertedSet);

        String[] strArray = { "1", "2", "3" };
        Collection<String> collection = CommonUtil.convert(strArray, Collection.class);
        assertEquals(3, collection.size());
        assertTrue(collection.contains("1"));
        assertTrue(collection.contains("2"));
        assertTrue(collection.contains("3"));

        List<String> list = Arrays.asList("1", "2", "3");
        String[] array = CommonUtil.convert(list, String[].class);
        assertArrayEquals(new String[] { "1", "2", "3" }, array);
    }

    @Test
    public void testConvert_Maps() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        Map<String, Integer> convertedMap = CommonUtil.convert(map, Map.class);
        assertEquals(map, convertedMap);
    }

    @Test
    public void testConvert_Dates() {
        long timestamp = 1000000L;
        assertEquals(new java.util.Date(timestamp), CommonUtil.convert(timestamp, java.util.Date.class));
        assertEquals(new java.sql.Timestamp(timestamp), CommonUtil.convert(timestamp, java.sql.Timestamp.class));
        assertEquals(new java.sql.Date(timestamp), CommonUtil.convert(timestamp, java.sql.Date.class));
        assertEquals(new java.sql.Time(timestamp), CommonUtil.convert(timestamp, java.sql.Time.class));

        java.util.Date date = new java.util.Date(timestamp);
        assertEquals(Long.valueOf(timestamp), CommonUtil.convert(date, Long.class));
    }

    @Test
    public void testConvert_WithType() {
        Type<Integer> intType = CommonUtil.typeOf(int.class);
        assertEquals(Integer.valueOf(123), CommonUtil.convert("123", intType));
        assertEquals(0, CommonUtil.convert(null, intType));

        Type<List> listType = CommonUtil.typeOf(List.class);
        String[] array = { "1", "2", "3" };
        List<String> list = CommonUtil.convert(array, listType);
        assertEquals(3, list.size());
    }

    @Test
    public void testGetPropNames_Class() {
        ImmutableList<String> propNames = Beans.getPropNameList(TestBean.class);
        assertNotNull(propNames);
        assertTrue(propNames.contains("name"));
        assertTrue(propNames.contains("value"));

        assertThrows(IllegalArgumentException.class, () -> Beans.getPropNameList((Class<?>) null));
    }

    @Test
    public void testGetPropNames_ClassWithExclude() {
        Set<String> exclude = new HashSet<>();
        exclude.add("name");

        List<String> propNames = Beans.getPropNames(TestBean.class, exclude);
        assertNotNull(propNames);
        assertFalse(propNames.contains("name"));
        assertTrue(propNames.contains("value"));

        assertThrows(IllegalArgumentException.class, () -> Beans.getPropNames(null, exclude));
    }

    @Test
    public void testGetPropValue_WithIgnoreUnmatched() {
        TestBean bean = new TestBean();
        bean.setName("test");

        assertEquals("test", Beans.getPropValue(bean, "name", false));
        assertEquals("test", Beans.getPropValue(bean, "name", true));

        assertThrows(RuntimeException.class, () -> Beans.getPropValue(bean, "nonExistent", false));
        assertNull(Beans.getPropValue(bean, "nonExistent", true));

        assertThrows(NullPointerException.class, () -> Beans.getPropValue(null, "name", true));
    }

    @Test
    public void testNegate_Boolean() {
        assertEquals(Boolean.FALSE, CommonUtil.negate(Boolean.TRUE));
        assertEquals(Boolean.TRUE, CommonUtil.negate(Boolean.FALSE));
        assertNull(CommonUtil.negate((Boolean) null));
    }

    @Test
    public void testNegate_BooleanArray() {
        boolean[] array = { true, false, true, false };
        CommonUtil.negate(array);
        assertArrayEquals(new boolean[] { false, true, false, true }, array);

        boolean[] emptyArray = {};
        CommonUtil.negate(emptyArray);

        CommonUtil.negate((boolean[]) null);
    }

    @Test
    public void testNegate_BooleanArrayRange() {
        boolean[] array = { true, false, true, false, true };
        CommonUtil.negate(array, 1, 4);
        assertArrayEquals(new boolean[] { true, true, false, true, true }, array);

        CommonUtil.negate(array, 0, 0);

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.negate(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.negate(array, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.negate(array, 3, 2));
    }

    @Test
    public void testEqualsLongArraysWithRange() {
        long[] arr1 = { 1L, 2L, 3L, 4L, 5L };
        long[] arr2 = { 10L, 2L, 3L, 40L, 50L };

        Assertions.assertTrue(CommonUtil.equals(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(CommonUtil.equals(arr1, 0, arr2, 0, 2));

        Assertions.assertTrue(CommonUtil.equals(arr1, 1, arr1, 1, 2));

        Assertions.assertTrue(CommonUtil.equals(arr1, 0, arr2, 0, 0));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(arr1, 0, arr2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equals(arr1, 0, arr2, 0, 10));
    }

    @Test
    public void testEqualsFloatArrays() {
        Assertions.assertTrue(CommonUtil.equals(new float[] { 1.0f, 2.0f }, new float[] { 1.0f, 2.0f }));
        Assertions.assertFalse(CommonUtil.equals(new float[] { 1.0f, 2.0f }, new float[] { 1.0f, 3.0f }));
        Assertions.assertTrue(CommonUtil.equals((float[]) null, null));
        Assertions.assertFalse(CommonUtil.equals(new float[] { 1.0f }, null));

        Assertions.assertTrue(CommonUtil.equals(new float[] { Float.NaN }, new float[] { Float.NaN }));
    }

    @Test
    public void testEqualsFloatArraysWithRange() {
        float[] arr1 = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] arr2 = { 10.0f, 2.0f, 3.0f, 40.0f };

        Assertions.assertTrue(CommonUtil.equals(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(CommonUtil.equals(arr1, 0, arr2, 0, 2));

        float[] nanArr1 = { Float.NaN, 2.0f };
        float[] nanArr2 = { Float.NaN, 2.0f };
        Assertions.assertTrue(CommonUtil.equals(nanArr1, 0, nanArr2, 0, 2));
    }

    @Test
    public void testEqualsFloatArraysWithDelta() {
        float[] arr1 = { 1.0f, 2.0f, 3.0f };
        float[] arr2 = { 1.01f, 2.01f, 3.01f };
        float[] arr3 = { 1.1f, 2.1f, 3.1f };

        Assertions.assertTrue(CommonUtil.equals(arr1, arr2, 0.02f));
        Assertions.assertFalse(CommonUtil.equals(arr1, arr3, 0.02f));

        Assertions.assertTrue(CommonUtil.equals((float[]) null, null, 0.01f));
        Assertions.assertTrue(CommonUtil.equals(new float[0], new float[0], 0.01f));
        Assertions.assertFalse(CommonUtil.equals(arr1, null, 0.01f));
        Assertions.assertFalse(CommonUtil.equals(arr1, new float[] { 1.0f, 2.0f }, 0.01f));
    }

    @Test
    public void testEqualsDoubleArrays() {
        Assertions.assertTrue(CommonUtil.equals(new double[] { 1.0, 2.0 }, new double[] { 1.0, 2.0 }));
        Assertions.assertFalse(CommonUtil.equals(new double[] { 1.0, 2.0 }, new double[] { 1.0, 3.0 }));
        Assertions.assertTrue(CommonUtil.equals((double[]) null, null));
        Assertions.assertFalse(CommonUtil.equals(new double[] { 1.0 }, null));

        Assertions.assertTrue(CommonUtil.equals(new double[] { Double.NaN }, new double[] { Double.NaN }));
    }

    @Test
    public void testEqualsDoubleArraysWithRange() {
        double[] arr1 = { 1.0, 2.0, 3.0, 4.0 };
        double[] arr2 = { 10.0, 2.0, 3.0, 40.0 };

        Assertions.assertTrue(CommonUtil.equals(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(CommonUtil.equals(arr1, 0, arr2, 0, 2));

        double[] nanArr1 = { Double.NaN, 2.0 };
        double[] nanArr2 = { Double.NaN, 2.0 };
        Assertions.assertTrue(CommonUtil.equals(nanArr1, 0, nanArr2, 0, 2));
    }

    @Test
    public void testEqualsDoubleArraysWithDelta() {
        double[] arr1 = { 1.0, 2.0, 3.0 };
        double[] arr2 = { 1.01, 2.01, 3.01 };
        double[] arr3 = { 1.1, 2.1, 3.1 };

        Assertions.assertTrue(CommonUtil.equals(arr1, arr2, 0.02));
        Assertions.assertFalse(CommonUtil.equals(arr1, arr3, 0.02));

        Assertions.assertTrue(CommonUtil.equals((double[]) null, null, 0.01));
        Assertions.assertTrue(CommonUtil.equals(new double[0], new double[0], 0.01));
        Assertions.assertFalse(CommonUtil.equals(arr1, null, 0.01));
        Assertions.assertFalse(CommonUtil.equals(arr1, new double[] { 1.0, 2.0 }, 0.01));
    }

    @Test
    public void testEqualsObjectArrays() {
        String[] arr1 = { "a", "b", "c" };
        String[] arr2 = { "a", "b", "c" };
        String[] arr3 = { "a", "b", "d" };

        Assertions.assertTrue(CommonUtil.equals(arr1, arr2));
        Assertions.assertFalse(CommonUtil.equals(arr1, arr3));
        Assertions.assertTrue(CommonUtil.equals(arr1, arr1));
        Assertions.assertTrue(CommonUtil.equals((Object[]) null, null));
        Assertions.assertFalse(CommonUtil.equals(arr1, null));

        String[] nullArr1 = { "a", null, "c" };
        String[] nullArr2 = { "a", null, "c" };
        String[] nullArr3 = { "a", "b", null };
        Assertions.assertTrue(CommonUtil.equals(nullArr1, nullArr2));
        Assertions.assertFalse(CommonUtil.equals(nullArr1, nullArr3));
    }

    @Test
    public void testEqualsObjectArraysWithRange() {
        String[] arr1 = { "a", "b", "c", "d" };
        String[] arr2 = { "x", "b", "c", "y" };

        Assertions.assertTrue(CommonUtil.equals(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(CommonUtil.equals(arr1, 0, arr2, 0, 2));

        Integer[] intArr = { 1, 2, 3 };
        Long[] longArr = { 1L, 2L, 3L };
        Assertions.assertFalse(CommonUtil.equals(intArr, 0, longArr, 0, 2));
    }

    @Test
    public void testDeepEquals() {
        Assertions.assertTrue(CommonUtil.deepEquals(5, 5));
        Assertions.assertFalse(CommonUtil.deepEquals(5, 6));
        Assertions.assertTrue(CommonUtil.deepEquals("test", "test"));
        Assertions.assertFalse(CommonUtil.deepEquals("test", "Test"));

        int[] intArr1 = { 1, 2, 3 };
        int[] intArr2 = { 1, 2, 3 };
        int[] intArr3 = { 1, 2, 4 };
        Assertions.assertTrue(CommonUtil.deepEquals(intArr1, intArr2));
        Assertions.assertFalse(CommonUtil.deepEquals(intArr1, intArr3));

        Object[] nested1 = { 1, new int[] { 2, 3 }, "test" };
        Object[] nested2 = { 1, new int[] { 2, 3 }, "test" };
        Object[] nested3 = { 1, new int[] { 2, 4 }, "test" };
        Assertions.assertTrue(CommonUtil.deepEquals(nested1, nested2));
        Assertions.assertFalse(CommonUtil.deepEquals(nested1, nested3));

        Assertions.assertTrue(CommonUtil.deepEquals(null, null));
        Assertions.assertFalse(CommonUtil.deepEquals(null, "test"));
        Assertions.assertFalse(CommonUtil.deepEquals("test", null));
    }

    @Test
    public void testDeepEqualsArrays() {
        Object[] arr1 = { "a", 1, true };
        Object[] arr2 = { "a", 1, true };
        Object[] arr3 = { "a", 1, false };

        Assertions.assertTrue(CommonUtil.deepEquals(arr1, arr2));
        Assertions.assertFalse(CommonUtil.deepEquals(arr1, arr3));
        Assertions.assertTrue(CommonUtil.deepEquals((Object[]) null, null));
        Assertions.assertFalse(CommonUtil.deepEquals(arr1, null));

        Object[] nested1 = { new int[] { 1, 2 }, new String[] { "a", "b" } };
        Object[] nested2 = { new int[] { 1, 2 }, new String[] { "a", "b" } };
        Object[] nested3 = { new int[] { 1, 3 }, new String[] { "a", "b" } };

        Assertions.assertTrue(CommonUtil.deepEquals(nested1, nested2));
        Assertions.assertFalse(CommonUtil.deepEquals(nested1, nested3));
    }

    @Test
    public void testDeepEqualsArraysWithRange() {
        Object[] arr1 = { "a", new int[] { 1, 2 }, "c", new int[] { 3, 4 } };
        Object[] arr2 = { "x", new int[] { 1, 2 }, "c", new int[] { 5, 6 } };

        Assertions.assertTrue(CommonUtil.deepEquals(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(CommonUtil.deepEquals(arr1, 2, arr2, 2, 2));

        Assertions.assertFalse(CommonUtil.deepEquals(new Integer[] { 1 }, 0, new Long[] { 1L }, 0, 1));
    }

    @Test
    public void testEqualsIgnoreCase() {
        String[] arr1 = { "Hello", "World", "TEST" };
        String[] arr2 = { "hello", "WORLD", "test" };
        String[] arr3 = { "hello", "WORLD", "testing" };

        Assertions.assertTrue(CommonUtil.equalsIgnoreCase(arr1, arr2));
        Assertions.assertFalse(CommonUtil.equalsIgnoreCase(arr1, arr3));
        Assertions.assertTrue(CommonUtil.equalsIgnoreCase((String[]) null, null));
        Assertions.assertFalse(CommonUtil.equalsIgnoreCase(arr1, null));
        Assertions.assertFalse(CommonUtil.equalsIgnoreCase(null, arr2));

        String[] nullArr1 = { "Hello", null, "TEST" };
        String[] nullArr2 = { "hello", null, "test" };
        String[] nullArr3 = { "hello", "world", "test" };
        Assertions.assertTrue(CommonUtil.equalsIgnoreCase(nullArr1, nullArr2));
        Assertions.assertFalse(CommonUtil.equalsIgnoreCase(nullArr1, nullArr3));
    }

    @Test
    public void testEqualsIgnoreCaseWithRange() {
        String[] arr1 = { "a", "Hello", "World", "d" };
        String[] arr2 = { "x", "HELLO", "world", "y" };

        Assertions.assertTrue(CommonUtil.equalsIgnoreCase(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(CommonUtil.equalsIgnoreCase(arr1, 0, arr2, 0, 2));
    }

    @Test
    public void testHashCodePrimitives() {
        Assertions.assertEquals(1231, CommonUtil.hashCode(true));
        Assertions.assertEquals(1237, CommonUtil.hashCode(false));

        Assertions.assertEquals('a', CommonUtil.hashCode('a'));

        Assertions.assertEquals(10, CommonUtil.hashCode((byte) 10));

        Assertions.assertEquals(100, CommonUtil.hashCode((short) 100));

        Assertions.assertEquals(1000, CommonUtil.hashCode(1000));

        Assertions.assertEquals(Long.valueOf(1000L).hashCode(), CommonUtil.hashCode(1000L));

        Assertions.assertEquals(Float.floatToIntBits(10.5f), CommonUtil.hashCode(10.5f));

        Assertions.assertEquals(Double.valueOf(10.5).hashCode(), CommonUtil.hashCode(10.5));
    }

    @Test
    public void testHashCodeObject() {
        Assertions.assertEquals(0, CommonUtil.hashCode((Object) null));

        String str = "test";
        Assertions.assertEquals(str.hashCode(), CommonUtil.hashCode(str));

        int[] intArr = { 1, 2, 3 };
        Assertions.assertEquals(Arrays.hashCode(intArr), CommonUtil.hashCode(intArr));

        String[] strArr = { "a", "b" };
        Assertions.assertEquals(Arrays.hashCode(strArr), CommonUtil.hashCode(strArr));
    }

    @Test
    public void testHashCodeArrays() {
        boolean[] boolArr = { true, false, true };
        Assertions.assertEquals(Arrays.hashCode(boolArr), CommonUtil.hashCode(boolArr));
        Assertions.assertEquals(0, CommonUtil.hashCode((boolean[]) null));

        boolean[] boolArr2 = { false, true, false, true };
        int expected = 1;
        expected = 31 * expected + 1231;
        expected = 31 * expected + 1237;
        Assertions.assertEquals(expected, CommonUtil.hashCode(boolArr2, 1, 3));

        char[] charArr = { 'a', 'b', 'c' };
        Assertions.assertEquals(Arrays.hashCode(charArr), CommonUtil.hashCode(charArr));
        Assertions.assertEquals(0, CommonUtil.hashCode((char[]) null));

        char[] charArr2 = { 'x', 'a', 'b', 'y' };
        int expected2 = 1;
        expected2 = 31 * expected2 + 'a';
        expected2 = 31 * expected2 + 'b';
        Assertions.assertEquals(expected2, CommonUtil.hashCode(charArr2, 1, 3));

        byte[] byteArr = { 1, 2, 3 };
        Assertions.assertEquals(Arrays.hashCode(byteArr), CommonUtil.hashCode(byteArr));
        Assertions.assertEquals(0, CommonUtil.hashCode((byte[]) null));

        short[] shortArr = { 1, 2, 3 };
        Assertions.assertEquals(Arrays.hashCode(shortArr), CommonUtil.hashCode(shortArr));
        Assertions.assertEquals(0, CommonUtil.hashCode((short[]) null));

        int[] intArr = { 1, 2, 3 };
        Assertions.assertEquals(Arrays.hashCode(intArr), CommonUtil.hashCode(intArr));
        Assertions.assertEquals(0, CommonUtil.hashCode((int[]) null));

        long[] longArr = { 1L, 2L, 3L };
        Assertions.assertEquals(Arrays.hashCode(longArr), CommonUtil.hashCode(longArr));
        Assertions.assertEquals(0, CommonUtil.hashCode((long[]) null));

        float[] floatArr = { 1.0f, 2.0f, 3.0f };
        Assertions.assertEquals(Arrays.hashCode(floatArr), CommonUtil.hashCode(floatArr));
        Assertions.assertEquals(0, CommonUtil.hashCode((float[]) null));

        double[] doubleArr = { 1.0, 2.0, 3.0 };
        Assertions.assertEquals(Arrays.hashCode(doubleArr), CommonUtil.hashCode(doubleArr));
        Assertions.assertEquals(0, CommonUtil.hashCode((double[]) null));

        String[] strArr = { "a", "b", "c" };
        Assertions.assertEquals(Arrays.hashCode(strArr), CommonUtil.hashCode(strArr));
        Assertions.assertEquals(0, CommonUtil.hashCode((Object[]) null));
    }

    @Test
    public void testHashCodeArraysWithRange() {

        byte[] byteArr = { 1, 2, 3, 4, 5 };
        int expected = 1;
        expected = 31 * expected + 2;
        expected = 31 * expected + 3;
        Assertions.assertEquals(expected, CommonUtil.hashCode(byteArr, 1, 3));

        short[] shortArr = { 1, 2, 3, 4, 5 };
        expected = 1;
        expected = 31 * expected + 2;
        expected = 31 * expected + 3;
        Assertions.assertEquals(expected, CommonUtil.hashCode(shortArr, 1, 3));

        int[] intArr = { 1, 2, 3, 4, 5 };
        expected = 1;
        expected = 31 * expected + 2;
        expected = 31 * expected + 3;
        Assertions.assertEquals(expected, CommonUtil.hashCode(intArr, 1, 3));

        long[] longArr = { 1L, 2L, 3L, 4L, 5L };
        expected = 1;
        expected = 31 * expected + Long.hashCode(2L);
        expected = 31 * expected + Long.hashCode(3L);
        Assertions.assertEquals(expected, CommonUtil.hashCode(longArr, 1, 3));

        float[] floatArr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        expected = 1;
        expected = 31 * expected + Float.floatToIntBits(2.0f);
        expected = 31 * expected + Float.floatToIntBits(3.0f);
        Assertions.assertEquals(expected, CommonUtil.hashCode(floatArr, 1, 3));

        double[] doubleArr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        expected = 1;
        expected = 31 * expected + Double.hashCode(2.0);
        expected = 31 * expected + Double.hashCode(3.0);
        Assertions.assertEquals(expected, CommonUtil.hashCode(doubleArr, 1, 3));

        String[] strArr = { "a", "b", "c", "d", "e" };
        expected = 1;
        expected = 31 * expected + "b".hashCode();
        expected = 31 * expected + "c".hashCode();
        Assertions.assertEquals(expected, CommonUtil.hashCode(strArr, 1, 3));
    }

    @Test
    public void testDeepHashCode() {
        Assertions.assertEquals(0, CommonUtil.deepHashCode((Object) null));

        String str = "test";
        Assertions.assertEquals(str.hashCode(), CommonUtil.deepHashCode(str));

        int[] intArr = { 1, 2, 3 };
        Assertions.assertEquals(Arrays.hashCode(intArr), CommonUtil.deepHashCode(intArr));

        Object[] nested = { 1, new int[] { 2, 3 }, "test" };
        Assertions.assertEquals(Arrays.deepHashCode(nested), CommonUtil.deepHashCode(nested));
    }

    @Test
    public void testDeepHashCodeArrays() {
        Object[] arr = { "a", 1, true };
        Assertions.assertEquals(Arrays.deepHashCode(arr), CommonUtil.deepHashCode(arr));
        Assertions.assertEquals(0, CommonUtil.deepHashCode((Object[]) null));

        Object[] nested = { new int[] { 1, 2 }, new String[] { "a", "b" } };
        Assertions.assertEquals(Arrays.deepHashCode(nested), CommonUtil.deepHashCode(nested));
    }

    @Test
    public void testDeepHashCodeArraysWithRange() {
        Object[] arr = { "a", new int[] { 1, 2 }, "c", new int[] { 3, 4 } };

        int expected = 1;
        expected = 31 * expected + Arrays.hashCode(new int[] { 1, 2 });
        expected = 31 * expected + "c".hashCode();

        Assertions.assertEquals(expected, CommonUtil.deepHashCode(arr, 1, 3));
    }

    @Test
    public void testToStringPrimitives() {
        Assertions.assertEquals("true", CommonUtil.toString(true));
        Assertions.assertEquals("false", CommonUtil.toString(false));

        Assertions.assertEquals("a", CommonUtil.toString('a'));

        Assertions.assertEquals("10", CommonUtil.toString((byte) 10));

        Assertions.assertEquals("100", CommonUtil.toString((short) 100));

        Assertions.assertEquals("1000", CommonUtil.toString(1000));

        Assertions.assertEquals("1000", CommonUtil.toString(1000L));

        Assertions.assertEquals("10.5", CommonUtil.toString(10.5f));

        Assertions.assertEquals("10.5", CommonUtil.toString(10.5));
    }

    @Test
    public void testToStringObject() {
        Assertions.assertEquals("null", CommonUtil.toString((Object) null));

        Assertions.assertEquals("test", CommonUtil.toString("test"));

        int[] intArr = { 1, 2, 3 };
        Assertions.assertEquals("[1, 2, 3]", CommonUtil.toString(intArr));

        List<String> list = Arrays.asList("a", "b", "c");
        Iterator<String> iter = list.iterator();
        Assertions.assertEquals("[a, b, c]", CommonUtil.toString(iter));

        Assertions.assertEquals("[a, b, c]", CommonUtil.toString(list));

        Object obj = new Object();
        Assertions.assertEquals(obj.toString(), CommonUtil.toString(obj));
    }

    @Test
    public void testToStringArrays() {
        Assertions.assertEquals("[true, false, true]", CommonUtil.toString(new boolean[] { true, false, true }));
        Assertions.assertEquals("null", CommonUtil.toString((boolean[]) null));
        Assertions.assertEquals("[]", CommonUtil.toString(new boolean[0]));

        boolean[] boolArr = { true, false, true, false };
        Assertions.assertEquals("[false, true]", CommonUtil.toString(boolArr, 1, 3));

        Assertions.assertEquals("[a, b, c]", CommonUtil.toString(new char[] { 'a', 'b', 'c' }));
        Assertions.assertEquals("null", CommonUtil.toString((char[]) null));
        Assertions.assertEquals("[]", CommonUtil.toString(new char[0]));

        char[] charArr = { 'a', 'b', 'c', 'd' };
        Assertions.assertEquals("[b, c]", CommonUtil.toString(charArr, 1, 3));

        Assertions.assertEquals("[1, 2, 3]", CommonUtil.toString(new byte[] { 1, 2, 3 }));
        Assertions.assertEquals("null", CommonUtil.toString((byte[]) null));
        Assertions.assertEquals("[]", CommonUtil.toString(new byte[0]));

        Assertions.assertEquals("[1, 2, 3]", CommonUtil.toString(new short[] { 1, 2, 3 }));
        Assertions.assertEquals("null", CommonUtil.toString((short[]) null));
        Assertions.assertEquals("[]", CommonUtil.toString(new short[0]));

        Assertions.assertEquals("[1, 2, 3]", CommonUtil.toString(new int[] { 1, 2, 3 }));
        Assertions.assertEquals("null", CommonUtil.toString((int[]) null));
        Assertions.assertEquals("[]", CommonUtil.toString(new int[0]));

        Assertions.assertEquals("[1, 2, 3]", CommonUtil.toString(new long[] { 1L, 2L, 3L }));
        Assertions.assertEquals("null", CommonUtil.toString((long[]) null));
        Assertions.assertEquals("[]", CommonUtil.toString(new long[0]));

        Assertions.assertEquals("[1.0, 2.0, 3.0]", CommonUtil.toString(new float[] { 1.0f, 2.0f, 3.0f }));
        Assertions.assertEquals("null", CommonUtil.toString((float[]) null));
        Assertions.assertEquals("[]", CommonUtil.toString(new float[0]));

        Assertions.assertEquals("[1.0, 2.0, 3.0]", CommonUtil.toString(new double[] { 1.0, 2.0, 3.0 }));
        Assertions.assertEquals("null", CommonUtil.toString((double[]) null));
        Assertions.assertEquals("[]", CommonUtil.toString(new double[0]));

        Assertions.assertEquals("[a, b, c]", CommonUtil.toString(new String[] { "a", "b", "c" }));
        Assertions.assertEquals("null", CommonUtil.toString((Object[]) null));
        Assertions.assertEquals("[]", CommonUtil.toString(new Object[0]));
    }

    @Test
    public void testToStringArraysWithRange() {

        byte[] byteArr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals("[2, 3]", CommonUtil.toString(byteArr, 1, 3));

        short[] shortArr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals("[2, 3]", CommonUtil.toString(shortArr, 1, 3));

        int[] intArr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals("[2, 3]", CommonUtil.toString(intArr, 1, 3));

        long[] longArr = { 1L, 2L, 3L, 4L, 5L };
        Assertions.assertEquals("[2, 3]", CommonUtil.toString(longArr, 1, 3));

        float[] floatArr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        Assertions.assertEquals("[2.0, 3.0]", CommonUtil.toString(floatArr, 1, 3));

        double[] doubleArr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        Assertions.assertEquals("[2.0, 3.0]", CommonUtil.toString(doubleArr, 1, 3));

        String[] strArr = { "a", "b", "c", "d", "e" };
        Assertions.assertEquals("[b, c]", CommonUtil.toString(strArr, 1, 3));
    }

    @Test
    public void testDeepToString() {
        Assertions.assertEquals("null", CommonUtil.deepToString(null));

        Assertions.assertEquals("test", CommonUtil.deepToString("test"));

        int[] intArr = { 1, 2, 3 };
        Assertions.assertEquals("[1, 2, 3]", CommonUtil.deepToString(intArr));

        Object[] nested = { 1, new int[] { 2, 3 }, "test" };
        Assertions.assertEquals("[1, [2, 3], test]", CommonUtil.deepToString(nested));

        Assertions.assertEquals("[true, false]", CommonUtil.deepToString(new boolean[] { true, false }));
        Assertions.assertEquals("[a, b]", CommonUtil.deepToString(new char[] { 'a', 'b' }));
        Assertions.assertEquals("[1, 2]", CommonUtil.deepToString(new byte[] { 1, 2 }));
        Assertions.assertEquals("[1, 2]", CommonUtil.deepToString(new short[] { 1, 2 }));
        Assertions.assertEquals("[1, 2]", CommonUtil.deepToString(new int[] { 1, 2 }));
        Assertions.assertEquals("[1, 2]", CommonUtil.deepToString(new long[] { 1L, 2L }));
        Assertions.assertEquals("[1.0, 2.0]", CommonUtil.deepToString(new float[] { 1.0f, 2.0f }));
        Assertions.assertEquals("[1.0, 2.0]", CommonUtil.deepToString(new double[] { 1.0, 2.0 }));
        Assertions.assertEquals("[a, b]", CommonUtil.deepToString(new String[] { "a", "b" }));
    }

    @Test
    public void testDeepToStringArrays() {
        Object[] arr = { "a", 1, true };
        Assertions.assertEquals("[a, 1, true]", CommonUtil.deepToString(arr));
        Assertions.assertEquals("null", CommonUtil.deepToString((Object[]) null));
        Assertions.assertEquals("[]", CommonUtil.deepToString(new Object[0]));

        Object[] nested = { new int[] { 1, 2 }, new String[] { "a", "b" } };
        Assertions.assertEquals("[[1, 2], [a, b]]", CommonUtil.deepToString(nested));

        Object[] circular = new Object[2];
        circular[0] = "test";
        circular[1] = circular;
        String result = CommonUtil.deepToString(circular);
        Assertions.assertTrue(result.contains("[...]"));
    }

    @Test
    public void testDeepToStringArraysWithRange() {
        Object[] arr = { "a", new int[] { 1, 2 }, "c", new int[] { 3, 4 } };
        Assertions.assertEquals("[[1, 2], c]", CommonUtil.deepToString(arr, 1, 3));
    }

    @Test
    public void testDeepToStringWithDefault() {
        Object[] arr = { "a", "b" };
        Assertions.assertEquals("[a, b]", CommonUtil.deepToString(arr, "default"));
        Assertions.assertEquals("default", CommonUtil.deepToString((Object[]) null, "default"));
    }

    @Test
    public void testLen() {
        Assertions.assertEquals(4, CommonUtil.len("test"));
        Assertions.assertEquals(0, CommonUtil.len(""));
        Assertions.assertEquals(0, CommonUtil.len((CharSequence) null));

        Assertions.assertEquals(3, CommonUtil.len(new boolean[] { true, false, true }));
        Assertions.assertEquals(0, CommonUtil.len(new boolean[0]));
        Assertions.assertEquals(0, CommonUtil.len((boolean[]) null));

        Assertions.assertEquals(3, CommonUtil.len(new char[] { 'a', 'b', 'c' }));
        Assertions.assertEquals(0, CommonUtil.len(new char[0]));
        Assertions.assertEquals(0, CommonUtil.len((char[]) null));

        Assertions.assertEquals(3, CommonUtil.len(new byte[] { 1, 2, 3 }));
        Assertions.assertEquals(0, CommonUtil.len(new byte[0]));
        Assertions.assertEquals(0, CommonUtil.len((byte[]) null));

        Assertions.assertEquals(3, CommonUtil.len(new short[] { 1, 2, 3 }));
        Assertions.assertEquals(0, CommonUtil.len(new short[0]));
        Assertions.assertEquals(0, CommonUtil.len((short[]) null));

        Assertions.assertEquals(3, CommonUtil.len(new int[] { 1, 2, 3 }));
        Assertions.assertEquals(0, CommonUtil.len(new int[0]));
        Assertions.assertEquals(0, CommonUtil.len((int[]) null));

        Assertions.assertEquals(3, CommonUtil.len(new long[] { 1L, 2L, 3L }));
        Assertions.assertEquals(0, CommonUtil.len(new long[0]));
        Assertions.assertEquals(0, CommonUtil.len((long[]) null));

        Assertions.assertEquals(3, CommonUtil.len(new float[] { 1.0f, 2.0f, 3.0f }));
        Assertions.assertEquals(0, CommonUtil.len(new float[0]));
        Assertions.assertEquals(0, CommonUtil.len((float[]) null));

        Assertions.assertEquals(3, CommonUtil.len(new double[] { 1.0, 2.0, 3.0 }));
        Assertions.assertEquals(0, CommonUtil.len(new double[0]));
        Assertions.assertEquals(0, CommonUtil.len((double[]) null));

        Assertions.assertEquals(3, CommonUtil.len(new Object[] { "a", "b", "c" }));
        Assertions.assertEquals(0, CommonUtil.len(new Object[0]));
        Assertions.assertEquals(0, CommonUtil.len((Object[]) null));
    }

    @Test
    public void testSize() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals(3, CommonUtil.size(list));
        Assertions.assertEquals(0, CommonUtil.size(new ArrayList<>()));
        Assertions.assertEquals(0, CommonUtil.size((Collection<?>) null));

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Assertions.assertEquals(2, CommonUtil.size(map));
        Assertions.assertEquals(0, CommonUtil.size(new HashMap<>()));
        Assertions.assertEquals(0, CommonUtil.size((Map<?, ?>) null));
    }

    @Test
    public void testIsEmpty() {
        Assertions.assertTrue(CommonUtil.isEmpty((CharSequence) null));
        Assertions.assertTrue(CommonUtil.isEmpty(""));
        Assertions.assertFalse(CommonUtil.isEmpty("test"));

        Assertions.assertTrue(CommonUtil.isEmpty((boolean[]) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new boolean[0]));
        Assertions.assertFalse(CommonUtil.isEmpty(new boolean[] { true }));

        Assertions.assertTrue(CommonUtil.isEmpty((char[]) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new char[0]));
        Assertions.assertFalse(CommonUtil.isEmpty(new char[] { 'a' }));

        Assertions.assertTrue(CommonUtil.isEmpty((byte[]) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new byte[0]));
        Assertions.assertFalse(CommonUtil.isEmpty(new byte[] { 1 }));

        Assertions.assertTrue(CommonUtil.isEmpty((short[]) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new short[0]));
        Assertions.assertFalse(CommonUtil.isEmpty(new short[] { 1 }));

        Assertions.assertTrue(CommonUtil.isEmpty((int[]) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new int[0]));
        Assertions.assertFalse(CommonUtil.isEmpty(new int[] { 1 }));

        Assertions.assertTrue(CommonUtil.isEmpty((long[]) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new long[0]));
        Assertions.assertFalse(CommonUtil.isEmpty(new long[] { 1L }));

        Assertions.assertTrue(CommonUtil.isEmpty((float[]) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new float[0]));
        Assertions.assertFalse(CommonUtil.isEmpty(new float[] { 1.0f }));

        Assertions.assertTrue(CommonUtil.isEmpty((double[]) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new double[0]));
        Assertions.assertFalse(CommonUtil.isEmpty(new double[] { 1.0 }));

        Assertions.assertTrue(CommonUtil.isEmpty((Object[]) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new Object[0]));
        Assertions.assertFalse(CommonUtil.isEmpty(new Object[] { "a" }));

        Assertions.assertTrue(CommonUtil.isEmpty((Collection<?>) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new ArrayList<>()));
        Assertions.assertFalse(CommonUtil.isEmpty(Arrays.asList("a")));

        Assertions.assertTrue(CommonUtil.isEmpty((Iterable<?>) null));
        Assertions.assertTrue(CommonUtil.isEmpty((Iterable<?>) new ArrayList<>()));
        Assertions.assertFalse(CommonUtil.isEmpty((Iterable<?>) Arrays.asList("a")));

        Assertions.assertTrue(CommonUtil.isEmpty((Iterator<?>) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new ArrayList<>().iterator()));
        Assertions.assertFalse(CommonUtil.isEmpty(Arrays.asList("a").iterator()));

        Assertions.assertTrue(CommonUtil.isEmpty((Map<?, ?>) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new HashMap<>()));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Assertions.assertFalse(CommonUtil.isEmpty(map));
    }

    @Test
    public void testBooleanChecks() {
        Assertions.assertTrue(CommonUtil.isTrue(Boolean.TRUE));
        Assertions.assertFalse(CommonUtil.isTrue(Boolean.FALSE));
        Assertions.assertFalse(CommonUtil.isTrue(null));

        Assertions.assertTrue(CommonUtil.isNotTrue(null));
        Assertions.assertTrue(CommonUtil.isNotTrue(Boolean.FALSE));
        Assertions.assertFalse(CommonUtil.isNotTrue(Boolean.TRUE));

        Assertions.assertTrue(CommonUtil.isFalse(Boolean.FALSE));
        Assertions.assertFalse(CommonUtil.isFalse(Boolean.TRUE));
        Assertions.assertFalse(CommonUtil.isFalse(null));

        Assertions.assertTrue(CommonUtil.isNotFalse(null));
        Assertions.assertTrue(CommonUtil.isNotFalse(Boolean.TRUE));
        Assertions.assertFalse(CommonUtil.isNotFalse(Boolean.FALSE));
    }

    @Test
    public void testNotEmpty() {
        Assertions.assertFalse(CommonUtil.notEmpty((CharSequence) null));
        Assertions.assertFalse(CommonUtil.notEmpty(""));
        Assertions.assertTrue(CommonUtil.notEmpty("test"));

        Assertions.assertFalse(CommonUtil.notEmpty((boolean[]) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new boolean[0]));
        Assertions.assertTrue(CommonUtil.notEmpty(new boolean[] { true }));

        Assertions.assertFalse(CommonUtil.notEmpty((char[]) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new char[0]));
        Assertions.assertTrue(CommonUtil.notEmpty(new char[] { 'a' }));

        Assertions.assertFalse(CommonUtil.notEmpty((byte[]) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new byte[0]));
        Assertions.assertTrue(CommonUtil.notEmpty(new byte[] { 1 }));

        Assertions.assertFalse(CommonUtil.notEmpty((short[]) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new short[0]));
        Assertions.assertTrue(CommonUtil.notEmpty(new short[] { 1 }));

        Assertions.assertFalse(CommonUtil.notEmpty((int[]) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new int[0]));
        Assertions.assertTrue(CommonUtil.notEmpty(new int[] { 1 }));

        Assertions.assertFalse(CommonUtil.notEmpty((long[]) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new long[0]));
        Assertions.assertTrue(CommonUtil.notEmpty(new long[] { 1L }));

        Assertions.assertFalse(CommonUtil.notEmpty((float[]) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new float[0]));
        Assertions.assertTrue(CommonUtil.notEmpty(new float[] { 1.0f }));

        Assertions.assertFalse(CommonUtil.notEmpty((double[]) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new double[0]));
        Assertions.assertTrue(CommonUtil.notEmpty(new double[] { 1.0 }));

        Assertions.assertFalse(CommonUtil.notEmpty((Object[]) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new Object[0]));
        Assertions.assertTrue(CommonUtil.notEmpty(new Object[] { "a" }));

        Assertions.assertFalse(CommonUtil.notEmpty((Collection<?>) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new ArrayList<>()));
        Assertions.assertTrue(CommonUtil.notEmpty(Arrays.asList("a")));

        Assertions.assertFalse(CommonUtil.notEmpty((Iterable<?>) null));
        Assertions.assertFalse(CommonUtil.notEmpty((Iterable<?>) new ArrayList<>()));
        Assertions.assertTrue(CommonUtil.notEmpty((Iterable<?>) Arrays.asList("a")));

        Assertions.assertFalse(CommonUtil.notEmpty((Iterator<?>) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new ArrayList<>().iterator()));
        Assertions.assertTrue(CommonUtil.notEmpty(Arrays.asList("a").iterator()));

        Assertions.assertFalse(CommonUtil.notEmpty((Map<?, ?>) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new HashMap<>()));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Assertions.assertTrue(CommonUtil.notEmpty(map));
    }

    @Test
    public void testAnyNull() {
        Assertions.assertTrue(CommonUtil.anyNull(null, "b"));
        Assertions.assertTrue(CommonUtil.anyNull("a", null));
        Assertions.assertTrue(CommonUtil.anyNull(null, null));
        Assertions.assertFalse(CommonUtil.anyNull("a", "b"));

        Assertions.assertTrue(CommonUtil.anyNull(null, "b", "c"));
        Assertions.assertTrue(CommonUtil.anyNull("a", null, "c"));
        Assertions.assertTrue(CommonUtil.anyNull("a", "b", null));
        Assertions.assertTrue(CommonUtil.anyNull(null, null, null));
        Assertions.assertFalse(CommonUtil.anyNull("a", "b", "c"));

        Assertions.assertFalse(CommonUtil.anyNull());
        Assertions.assertTrue(CommonUtil.anyNull("a", null, "c", "d"));
        Assertions.assertFalse(CommonUtil.anyNull("a", "b", "c", "d"));

        Assertions.assertFalse(CommonUtil.anyNull((Iterable<?>) null));
        Assertions.assertFalse(CommonUtil.anyNull(new ArrayList<>()));
        Assertions.assertTrue(CommonUtil.anyNull(Arrays.asList("a", null, "c")));
        Assertions.assertFalse(CommonUtil.anyNull(Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testAnyEmpty() {
        Assertions.assertTrue(CommonUtil.anyEmpty("", "b"));
        Assertions.assertTrue(CommonUtil.anyEmpty("a", ""));
        Assertions.assertTrue(CommonUtil.anyEmpty(null, "b"));
        Assertions.assertTrue(CommonUtil.anyEmpty("a", null));
        Assertions.assertFalse(CommonUtil.anyEmpty("a", "b"));

        Assertions.assertTrue(CommonUtil.anyEmpty("", "b", "c"));
        Assertions.assertTrue(CommonUtil.anyEmpty("a", "", "c"));
        Assertions.assertTrue(CommonUtil.anyEmpty("a", "b", ""));
        Assertions.assertTrue(CommonUtil.anyEmpty(null, "b", "c"));
        Assertions.assertFalse(CommonUtil.anyEmpty("a", "b", "c"));

        Assertions.assertFalse(CommonUtil.anyEmpty((CharSequence[]) null));
        Assertions.assertFalse(CommonUtil.anyEmpty(new CharSequence[0]));
        Assertions.assertTrue(CommonUtil.anyEmpty("a", "", "c"));
        Assertions.assertFalse(CommonUtil.anyEmpty("a", "b", "c"));

        Assertions.assertFalse(CommonUtil.anyEmpty((Iterable<? extends CharSequence>) null));
        Assertions.assertFalse(CommonUtil.anyEmpty(new ArrayList<>()));
        Assertions.assertTrue(CommonUtil.anyEmpty(Arrays.asList("a", "", "c")));
        Assertions.assertFalse(CommonUtil.anyEmpty(Arrays.asList("a", "b", "c")));

        Assertions.assertTrue(CommonUtil.anyEmpty(new Object[0], new Object[] { "b" }));
        Assertions.assertTrue(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[0]));
        Assertions.assertTrue(CommonUtil.anyEmpty((Object[]) null, new Object[] { "b" }));
        Assertions.assertFalse(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[] { "b" }));

        Assertions.assertTrue(CommonUtil.anyEmpty(new Object[0], new Object[] { "b" }, new Object[] { "c" }));
        Assertions.assertTrue(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[0], new Object[] { "c" }));
        Assertions.assertTrue(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[] { "b" }, new Object[0]));
        Assertions.assertFalse(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[] { "b" }, new Object[] { "c" }));

        Assertions.assertTrue(CommonUtil.anyEmpty(new ArrayList<>(), Arrays.asList("b")));
        Assertions.assertTrue(CommonUtil.anyEmpty(Arrays.asList("a"), new ArrayList<>()));
        Assertions.assertTrue(CommonUtil.anyEmpty((Collection<?>) null, Arrays.asList("b")));
        Assertions.assertFalse(CommonUtil.anyEmpty(Arrays.asList("a"), Arrays.asList("b")));

        Assertions.assertTrue(CommonUtil.anyEmpty(new ArrayList<>(), Arrays.asList("b"), Arrays.asList("c")));
        Assertions.assertTrue(CommonUtil.anyEmpty(Arrays.asList("a"), new ArrayList<>(), Arrays.asList("c")));
        Assertions.assertTrue(CommonUtil.anyEmpty(Arrays.asList("a"), Arrays.asList("b"), new ArrayList<>()));
        Assertions.assertFalse(CommonUtil.anyEmpty(Arrays.asList("a"), Arrays.asList("b"), Arrays.asList("c")));
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
    public void testCheckArgNotNull_NotNull() {
        String str = "test";
        Assertions.assertSame(str, CommonUtil.checkArgNotNull(str));

        Integer num = 42;
        Assertions.assertSame(num, CommonUtil.checkArgNotNull(num));
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
    public void testCheckArgNotEmpty_CharSequence_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((CharSequence) null, "str"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty("", "str"));
    }

    @Test
    public void testCheckArgNotEmpty_BooleanArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((boolean[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new boolean[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_CharArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((char[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new char[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ByteArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((byte[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new byte[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ShortArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((short[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new short[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_IntArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((int[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new int[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_LongArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((long[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new long[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_FloatArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((float[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new float[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_DoubleArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((double[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new double[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ObjectArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((String[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new String[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_Collection_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Collection<?>) null, "coll"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new ArrayList<>(), "coll"));
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
    public void testCheckArgNotNegative_Byte_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative((byte) -1, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(Byte.MIN_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Short_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative((short) -1, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(Short.MIN_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Int_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(Integer.MIN_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Long_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1L, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(Long.MIN_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Float_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-0.1f, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-Float.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Double_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-0.1, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-Double.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgPositive_Byte_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((byte) 0, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((byte) -1, "val"));
    }

    @Test
    public void testCheckArgPositive_Short_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((short) 0, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((short) -1, "val"));
    }

    @Test
    public void testCheckArgPositive_Int_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-1, "val"));
    }

    @Test
    public void testCheckArgPositive_Long_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0L, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-1L, "val"));
    }

    @Test
    public void testCheckArgPositive_Float_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0.0f, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-0.1f, "val"));
    }

    @Test
    public void testCheckArgPositive_Double_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0.0, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-0.1, "val"));
    }

    @Test
    public void testCheckElementNotNull_ObjectArray_NoNulls() {
        assertDoesNotThrow(() -> {
            CommonUtil.checkElementNotNull(new String[] { "a", "b", "c" });
            CommonUtil.checkElementNotNull(new Integer[] { 1, 2, 3 });
            CommonUtil.checkElementNotNull(new Object[0]);
        });
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
        assertDoesNotThrow(() -> {
            CommonUtil.checkElementNotNull(Arrays.asList("a", "b", "c"));
            CommonUtil.checkElementNotNull(new HashSet<>(Arrays.asList(1, 2, 3)));
            CommonUtil.checkElementNotNull(new ArrayList<>());
        });
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
    public void testCheckKeyNotNull_NoNullKeys() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        CommonUtil.checkKeyNotNull(map);

        CommonUtil.checkKeyNotNull(new HashMap<>());
        assertNotNull(map);
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
        assertNotNull(map);
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
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgument(true, "%s and %s", 'a', 10);
            CommonUtil.checkArgument(true, "%s and %s", 10, 3.14);
            CommonUtil.checkArgument(true, "%s and %s", 3.14, "test");
            CommonUtil.checkArgument(true, "%s and %s", "test", 100L);
        });
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
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgNotNegative(0, "zero");
            CommonUtil.checkArgPositive(1, "one");

            CommonUtil.checkElementIndex(0, 1);
            CommonUtil.checkPositionIndex(1, 1);

            CommonUtil.checkElementNotNull(new Object[0]);
            CommonUtil.checkElementNotNull(new ArrayList<>());
            CommonUtil.checkKeyNotNull(new HashMap<>());
            CommonUtil.checkValueNotNull(new HashMap<>());
        });
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

    @Test
    @DisplayName("copyOfRange with negative step on LinkedList should not throw")
    public void test_copyOfRange_negativeStep_linkedList() {
        // LinkedList is NOT RandomAccess
        LinkedList<Integer> list = new LinkedList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

        // Negative step: iterate from index 9 down to index 0 (exclusive), stepping by -1
        List<Integer> result = CommonUtil.copyOfRange(list, 9, -1, -1);

        assertEquals(Arrays.asList(9, 8, 7, 6, 5, 4, 3, 2, 1, 0), result);
    }

    @Test
    @DisplayName("copyOfRange with step=-2 on LinkedList")
    public void test_copyOfRange_negativeStep2_linkedList() {
        LinkedList<Integer> list = new LinkedList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

        List<Integer> result = CommonUtil.copyOfRange(list, 9, -1, -2);

        assertEquals(Arrays.asList(9, 7, 5, 3, 1), result);
    }

    @Test
    @DisplayName("copyOfRange with step=-3 on LinkedList")
    public void test_copyOfRange_negativeStep3_linkedList() {
        LinkedList<Integer> list = new LinkedList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

        List<Integer> result = CommonUtil.copyOfRange(list, 9, -1, -3);

        assertEquals(Arrays.asList(9, 6, 3, 0), result);
    }

    @Test
    @DisplayName("copyOfRange with positive step on LinkedList still works")
    public void test_copyOfRange_positiveStep_linkedList() {
        LinkedList<Integer> list = new LinkedList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

        List<Integer> result = CommonUtil.copyOfRange(list, 0, 10, 2);

        assertEquals(Arrays.asList(0, 2, 4, 6, 8), result);
    }

    @Test
    @DisplayName("copyOfRange with negative step on ArrayList still works")
    public void test_copyOfRange_negativeStep_arrayList() {
        ArrayList<Integer> list = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

        List<Integer> result = CommonUtil.copyOfRange(list, 9, -1, -1);

        assertEquals(Arrays.asList(9, 8, 7, 6, 5, 4, 3, 2, 1, 0), result);
    }

    @Test
    @DisplayName("copyOfRange results should be consistent between ArrayList and LinkedList")
    public void test_copyOfRange_negativeStep_consistentResults() {
        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        LinkedList<String> linkedList = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));

        List<String> resultArray = CommonUtil.copyOfRange(arrayList, 4, -1, -1);
        List<String> resultLinked = CommonUtil.copyOfRange(linkedList, 4, -1, -1);

        assertEquals(resultArray, resultLinked, "ArrayList and LinkedList should produce the same result");
        assertEquals(Arrays.asList("e", "d", "c", "b", "a"), resultArray);
    }

    @Test
    @DisplayName("copyOfRange step=-2 should be consistent between ArrayList and LinkedList")
    public void test_copyOfRange_negativeStep2_consistentResults() {
        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        LinkedList<String> linkedList = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));

        List<String> resultArray = CommonUtil.copyOfRange(arrayList, 4, -1, -2);
        List<String> resultLinked = CommonUtil.copyOfRange(linkedList, 4, -1, -2);

        assertEquals(resultArray, resultLinked, "ArrayList and LinkedList should produce the same result");
        assertEquals(Arrays.asList("e", "c", "a"), resultArray);
    }

    @Test
    @DisplayName("copyOfRange with step=1 on LinkedList for forward copy")
    public void test_copyOfRange_step1_linkedList() {
        LinkedList<Integer> list = new LinkedList<>(Arrays.asList(10, 20, 30, 40, 50));

        List<Integer> result = CommonUtil.copyOfRange(list, 1, 4, 1);

        assertEquals(Arrays.asList(20, 30, 40), result);
    }

    @Test
    @DisplayName("copyOfRange returns empty list when fromIndex equals toIndex")
    public void test_copyOfRange_emptyRange() {
        LinkedList<Integer> list = new LinkedList<>(Arrays.asList(1, 2, 3));

        List<Integer> result = CommonUtil.copyOfRange(list, 2, 2, -1);

        assertTrue(result.isEmpty());
    }

    // --- Tests for equalsInOrder ---

    @Test
    public void testEqualsInOrder_Collection() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<String> b = Arrays.asList("a", "b", "c");
        assertTrue(CommonUtil.equalsInOrder(a, b));

        List<String> c = Arrays.asList("c", "b", "a");
        assertFalse(CommonUtil.equalsInOrder(a, c));
    }

    @Test
    public void testEqualsInOrder_Collection_Null() {
        assertTrue(CommonUtil.equalsInOrder((Collection<?>) null, (Collection<?>) null));
        assertFalse(CommonUtil.equalsInOrder(Arrays.asList("a"), (Collection<?>) null));
        assertFalse(CommonUtil.equalsInOrder((Collection<?>) null, Arrays.asList("a")));
    }

    @Test
    public void testEqualsInOrder_Collection_DifferentSizes() {
        assertFalse(CommonUtil.equalsInOrder(Arrays.asList("a", "b"), Arrays.asList("a")));
    }

    @Test
    public void testEqualsInOrder_Map() {
        Map<String, Integer> a = new LinkedHashMap<>();
        a.put("x", 1);
        a.put("y", 2);
        Map<String, Integer> b = new LinkedHashMap<>();
        b.put("x", 1);
        b.put("y", 2);
        assertTrue(CommonUtil.equalsInOrder(a, b));
    }

    @Test
    public void testEqualsInOrder_Map_Null() {
        assertTrue(CommonUtil.equalsInOrder((Map<?, ?>) null, (Map<?, ?>) null));
        assertTrue(CommonUtil.equalsInOrder(new HashMap<>(), (Map<?, ?>) null));
    }

    // --- Tests for equalsByCommonProps ---

    @Test
    public void testEqualsByCommonProps() {
        TestBean b1 = new TestBean();
        b1.setName("Alice");
        b1.setValue("v1");

        TestBean b2 = new TestBean();
        b2.setName("Alice");
        b2.setValue("v1");

        assertTrue(CommonUtil.equalsByCommonProps(b1, b2));

        b2.setName("Bob");
        assertFalse(CommonUtil.equalsByCommonProps(b1, b2));
    }

    // --- Tests for slice ---

    @Test
    public void testSlice_Array() {
        String[] arr = { "a", "b", "c", "d", "e" };
        List<String> sliced = CommonUtil.slice(arr, 1, 3);
        assertEquals(2, sliced.size());
        assertEquals("b", sliced.get(0));
        assertEquals("c", sliced.get(1));
    }

    @Test
    public void testSlice_List() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        List<String> sliced = CommonUtil.slice(list, 1, 4);
        assertEquals(3, sliced.size());
        assertEquals("b", sliced.get(0));
    }

    @Test
    public void testSlice_Collection() {
        Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        Collection<String> sliced = CommonUtil.slice((Collection<String>) set, 1, 3);
        assertEquals(2, sliced.size());
    }

    // --- Tests for toLinkedList ---

    @Test
    public void testToLinkedList() {
        LinkedList<String> result = CommonUtil.toLinkedList("a", "b", "c");
        assertEquals(3, result.size());
        assertEquals("a", result.getFirst());
        assertEquals("c", result.getLast());
    }

    // --- Tests for toLinkedHashSet ---

    @Test
    public void testToLinkedHashSet() {
        Set<String> result = CommonUtil.toLinkedHashSet("a", "b", "c", "a");
        assertEquals(3, result.size());
        // Preserves insertion order
        Iterator<String> it = result.iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
    }

    // --- Tests for toSortedSet ---

    @Test
    public void testToSortedSet() {
        SortedSet<String> result = CommonUtil.toSortedSet("c", "a", "b");
        assertEquals(3, result.size());
        assertEquals("a", result.first());
        assertEquals("c", result.last());
    }

    // --- Tests for toNavigableSet ---

    @Test
    public void testToNavigableSet() {
        NavigableSet<String> result = CommonUtil.toNavigableSet("c", "a", "b");
        assertEquals(3, result.size());
        assertEquals("a", result.first());
        assertEquals("b", result.lower("c"));
    }

    // --- Tests for checkArgNotEmpty (PrimitiveList, Multiset, Multimap, Dataset) ---

    @Test
    public void testCheckArgNotEmpty_PrimitiveList() {
        IntList list = IntList.of(1, 2, 3);
        assertSame(list, CommonUtil.checkArgNotEmpty(list, "list"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((IntList) null, "list"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(IntList.of(), "list"));
    }

    @Test
    public void testCheckArgNotEmpty_Multiset() {
        Multiset<String> ms = new Multiset<>();
        ms.add("a");
        assertSame(ms, CommonUtil.checkArgNotEmpty(ms, "ms"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Multiset<String>) null, "ms"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new Multiset<>(), "ms"));
    }

    @Test
    public void testCheckArgNotEmpty_Multimap() {
        ListMultimap<String, Integer> mm = N.newListMultimap();
        mm.put("key", 1);
        assertSame(mm, CommonUtil.checkArgNotEmpty(mm, "mm"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((ListMultimap<String, Integer>) null, "mm"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(N.newListMultimap(), "mm"));
    }

    @Test
    public void testCheckArgNotEmpty_Dataset() {
        Dataset ds = N.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        assertSame(ds, CommonUtil.checkArgNotEmpty(ds, "ds"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Dataset) null, "ds"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(N.newEmptyDataset(), "ds"));
    }

    // --- Tests for defaultValueOf with nonNullForPrimitiveWrapper ---

    @Test
    public void testDefaultValueOf_nonNullForPrimitiveWrapper() {
        // With nonNullForPrimitiveWrapper = true, wrapper types should return non-null defaults
        Integer intDefault = CommonUtil.defaultValueOf(Integer.class, true);
        assertNotNull(intDefault);
        assertEquals(0, (int) intDefault);

        Boolean boolDefault = CommonUtil.defaultValueOf(Boolean.class, true);
        assertNotNull(boolDefault);
        assertFalse(boolDefault);

        // With nonNullForPrimitiveWrapper = false, wrapper types should return null
        assertNull(CommonUtil.defaultValueOf(Integer.class, false));
    }

    // --- Tests for castIfAssignable with Type parameter ---

    @Test
    public void testCastIfAssignable_WithType() {
        // TODO: Test castIfAssignable overload with Type parameter
        com.landawn.abacus.util.u.Nullable<String> result = CommonUtil.castIfAssignable("hello", String.class);
        assertTrue(result.isPresent());
        assertEquals("hello", result.get());

        com.landawn.abacus.util.u.Nullable<Integer> empty = CommonUtil.castIfAssignable("hello", Integer.class);
        assertFalse(empty.isPresent());
    }

    // --- Tests for notEmpty (PrimitiveList, Multiset, Multimap, Dataset) ---

    @Test
    public void testNotEmpty_PrimitiveList() {
        assertTrue(CommonUtil.notEmpty(IntList.of(1)));
        assertFalse(CommonUtil.notEmpty(IntList.of()));
    }

    @Test
    public void testNotEmpty_Multiset() {
        Multiset<String> ms = new Multiset<>();
        assertFalse(CommonUtil.notEmpty(ms));
        ms.add("a");
        assertTrue(CommonUtil.notEmpty(ms));
    }

    @Test
    public void testNotEmpty_Multimap() {
        ListMultimap<String, Integer> mm = N.newListMultimap();
        assertFalse(CommonUtil.notEmpty(mm));
        mm.put("key", 1);
        assertTrue(CommonUtil.notEmpty(mm));
    }

    @Test
    public void testNotEmpty_Dataset() {
        assertFalse(CommonUtil.notEmpty(N.newEmptyDataset()));
        Dataset ds = N.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        assertTrue(CommonUtil.notEmpty(ds));
    }

    // --- Tests for toQueue, toDeque, toMultiset ---

    @Test
    public void testToQueue() {
        Queue<String> result = CommonUtil.toQueue("a", "b", "c");
        assertEquals(3, result.size());
        assertEquals("a", result.peek());
    }

    @Test
    public void testToDeque() {
        Deque<String> result = CommonUtil.toDeque("a", "b", "c");
        assertEquals(3, result.size());
    }

    @Test
    public void testToArrayDeque() {
        ArrayDeque<String> result = CommonUtil.toArrayDeque("a", "b", "c");
        assertEquals(3, result.size());
    }

    @Test
    public void testToMultiset() {
        Multiset<String> result = CommonUtil.toMultiset("a", "b", "a");
        assertEquals(2, result.count("a"));
        assertEquals(1, result.count("b"));
    }

    // --- Tests for newHashMap with Collection and keyExtractor ---

    @Test
    public void testNewHashMap_WithCollectionAndKeyExtractor() {
        List<String> source = Arrays.asList("alice", "bob");
        Map<Character, String> result = CommonUtil.newHashMap(source, s -> s.charAt(0));
        assertEquals("alice", result.get('a'));
        assertEquals("bob", result.get('b'));
    }

    // --- Tests for newLinkedHashMap with Collection and keyExtractor ---

    @Test
    public void testNewLinkedHashMap_WithCollectionAndKeyExtractor() {
        List<String> source = Arrays.asList("alice", "bob");
        Map<Character, String> result = CommonUtil.newLinkedHashMap(source, s -> s.charAt(0));
        assertEquals(2, result.size());
    }

    // --- Tests for nullToEmpty with typed array ---

    @Test
    public void testNullToEmpty_TypedArray() {
        Integer[] nullArr = null;
        Integer[] result = CommonUtil.nullToEmpty(nullArr, Integer[].class);
        assertNotNull(result);
        assertEquals(0, result.length);

        Integer[] nonNull = { 1, 2 };
        assertSame(nonNull, CommonUtil.nullToEmpty(nonNull, Integer[].class));
    }

    // --- Tests for size(PrimitiveList) ---

    @Test
    public void testSize_PrimitiveList_Empty() {
        assertEquals(0, CommonUtil.size(IntList.of()));
    }

    @Test
    public void testSize_PrimitiveList_NonEmpty() {
        assertEquals(3, CommonUtil.size(IntList.of(1, 2, 3)));
    }

    // --- Tests for newProxyInstance ---

    @Test
    public void testNewProxyInstance_WithClass() {
        Runnable proxy = CommonUtil.newProxyInstance(Runnable.class, (p, method, args) -> null);
        assertNotNull(proxy);
    }

    // Cover nested Iterable/Iterator/Map branches in hashCodeEverything.
    @Test
    public void testHashCodeEverything_NestedStructures() {
        final List<Object> nestedValues = Arrays.asList("alpha", Arrays.asList(1, 2), new int[] { 3, 4 });
        final Map<String, Object> left = new LinkedHashMap<>();
        left.put("values", nestedValues);
        left.put("flag", true);

        final Map<String, Object> right = new LinkedHashMap<>();
        right.put("values", Arrays.asList("alpha", Arrays.asList(1, 2), new int[] { 3, 4 }));
        right.put("flag", true);

        assertEquals(CommonUtil.hashCodeEverything(left), CommonUtil.hashCodeEverything(right));
        assertEquals(CommonUtil.hashCodeEverything(nestedValues), CommonUtil.hashCodeEverything(nestedValues.iterator()));
        assertNotEquals(CommonUtil.hashCodeEverything(left), CommonUtil.hashCodeEverything(Collections.singletonMap("values", nestedValues)));
    }

    @Test
    public void testNewDataset_WithBeanRowsAndProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("source", "beanRows");

        Dataset dataset = CommonUtil.newDataset(Arrays.asList("name", "age", "missing"),
                Arrays.asList(new DatasetRowBean("Tom", 10), new DatasetRowBean("Jerry", 12)), properties);

        assertEquals(2, dataset.size());
        assertEquals(Arrays.asList("name", "age", "missing"), dataset.columnNames());
        assertEquals("beanRows", dataset.getProperties().get("source"));
    }

    private static final class DatasetRowBean {
        private String name;
        private int age;

        public DatasetRowBean() {
        }

        private DatasetRowBean(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

}
