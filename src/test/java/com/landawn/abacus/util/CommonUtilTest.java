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
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.u.OptionalInt;

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
    public void testCheckFromToIndex_NegativeLength() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> N.checkFromToIndex(0, 0, -1));

        assertTrue(exception.getMessage().contains("negative"));
    }

    @Test
    public void checkFromToIndex_valid() {
        assertDoesNotThrow(() -> N.checkFromToIndex(0, 0, 0));
        assertDoesNotThrow(() -> N.checkFromToIndex(0, 5, 10));
        assertDoesNotThrow(() -> N.checkFromToIndex(5, 10, 10));
        assertDoesNotThrow(() -> N.checkFromToIndex(0, 0, 10));
        assertDoesNotThrow(() -> N.checkFromToIndex(10, 10, 10));
    }

    @Test
    public void checkFromToIndex_invalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(-1, 5, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(0, 11, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(6, 5, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(0, 1, 0));
    }

    @Test
    public void testCheckFromToIndex() {
        N.checkFromToIndex(0, 5, 10);
        N.checkFromToIndex(2, 7, 10);
        N.checkFromToIndex(0, 0, 10);
        N.checkFromToIndex(10, 10, 10);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(-1, 5, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(5, 2, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(0, 11, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromToIndex(11, 11, 10));
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
        assertThrows(IllegalArgumentException.class, () -> N.checkFromIndexSize(0, -1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(6, 5, 10));
    }

    @Test
    public void checkFromIndexSize_valid() {
        assertDoesNotThrow(() -> N.checkFromIndexSize(0, 0, 0));
        assertDoesNotThrow(() -> N.checkFromIndexSize(0, 5, 10));
        assertDoesNotThrow(() -> N.checkFromIndexSize(5, 5, 10));
        assertDoesNotThrow(() -> N.checkFromIndexSize(0, 0, 10));
        assertDoesNotThrow(() -> N.checkFromIndexSize(10, 0, 10));
    }

    @Test
    public void checkFromIndexSize_invalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(-1, 5, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(0, 11, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(6, 5, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> N.checkFromIndexSize(0, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> N.checkFromIndexSize(0, 1, -1));
    }

    @Test
    public void testCheckFromIndexSize() {
        N.checkFromIndexSize(0, 5, 10);
        N.checkFromIndexSize(5, 5, 10);
        N.checkFromIndexSize(0, 0, 10);
        N.checkFromIndexSize(10, 0, 10);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(-1, 5, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkFromIndexSize(0, -1, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkFromIndexSize(0, 5, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(6, 5, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(0, 11, 10));
    }

    @Test
    public void testCheckIndex_Valid() {
        assertEquals(0, N.checkIndex(0, 5));
        assertEquals(4, N.checkIndex(4, 5));
    }

    @Test
    public void checkIndex_valid() {
        assertEquals(0, N.checkIndex(0, 1));
        assertEquals(5, N.checkIndex(5, 10));
        assertEquals(9, N.checkIndex(9, 10));
    }

    @Test
    public void testCheckIndex_ValidIndex() {
        Assertions.assertEquals(0, N.checkIndex(0, 10));
        Assertions.assertEquals(5, N.checkIndex(5, 10));
        Assertions.assertEquals(9, N.checkIndex(9, 10));
    }

    @Test
    public void testCheckIndex_Invalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkIndex(-1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkIndex(5, 5));
    }

    @Test
    public void checkIndex_invalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkIndex(-1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkIndex(10, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkIndex(0, 0));
        assertThrows(IllegalArgumentException.class, () -> N.checkIndex(0, -1));
    }

    @Test
    public void testCheckIndex_InvalidIndex() {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkIndex(-1, 10));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkIndex(10, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkIndex(11, 10));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkIndex(0, -1));
    }

    @Test
    public void testCheckIndex() {
        Assertions.assertEquals(0, N.checkIndex(0, 10));
        Assertions.assertEquals(5, N.checkIndex(5, 10));
        Assertions.assertEquals(9, N.checkIndex(9, 10));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkIndex(-1, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkIndex(10, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkIndex(0, -1));
    }

    @Test
    public void testCheckElementIndex_ValidIndex() {
        assertEquals(0, N.checkElementIndex(0, 5));
        assertEquals(4, N.checkElementIndex(4, 5));
    }

    @Test
    public void checkElementIndex_valid() {
        assertEquals(0, N.checkElementIndex(0, 1));
        assertEquals(5, N.checkElementIndex(5, 10));
        assertEquals(9, N.checkElementIndex(9, 10));
    }

    @Test
    public void checkElementIndexWithDesc_valid() {
        assertEquals(0, N.checkElementIndex(0, 1, "testIndex"));
        assertEquals(5, N.checkElementIndex(5, 10, "testIndex"));
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
    public void checkElementIndex_invalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(-1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(10, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(0, 0));
        assertThrows(IllegalArgumentException.class, () -> N.checkElementIndex(0, -1));
    }

    @Test
    public void checkElementIndexWithDesc_invalid() {
        IndexOutOfBoundsException ex1 = assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(-1, 10, "testIndex"));
        assertTrue(ex1.getMessage().contains("testIndex (-1) must not be negative"));

        IndexOutOfBoundsException ex2 = assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(10, 10, "testIndex"));
        assertTrue(ex2.getMessage().contains("testIndex (10) must be less than size (10)"));

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () -> N.checkElementIndex(0, -1, "testIndex"));
        assertTrue(ex3.getMessage().contains("negative size: -1"));
    }

    @Test
    public void testCheckElementIndex() {
        Assertions.assertEquals(0, N.checkElementIndex(0, 10));
        Assertions.assertEquals(5, N.checkElementIndex(5, 10));
        Assertions.assertEquals(9, N.checkElementIndex(9, 10));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(-1, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(10, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkElementIndex(0, -1));

        Assertions.assertEquals(5, N.checkElementIndex(5, 10, "myIndex"));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(-1, 10, "myIndex"));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(10, 10, "myIndex"));
    }

    @Test
    public void testCheckPositionIndex_ValidIndex() {
        assertEquals(0, N.checkPositionIndex(0, 5));
        assertEquals(5, N.checkPositionIndex(5, 5));
    }

    @Test
    public void checkPositionIndex_valid() {
        assertEquals(0, N.checkPositionIndex(0, 0));
        assertEquals(0, N.checkPositionIndex(0, 10));
        assertEquals(5, N.checkPositionIndex(5, 10));
        assertEquals(10, N.checkPositionIndex(10, 10));
    }

    @Test
    public void checkPositionIndexWithDesc_valid() {
        assertEquals(0, N.checkPositionIndex(0, 0, "testPosIndex"));
        assertEquals(5, N.checkPositionIndex(5, 10, "testPosIndex"));
        assertEquals(10, N.checkPositionIndex(10, 10, "testPosIndex"));
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
    public void checkPositionIndex_invalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkPositionIndex(-1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkPositionIndex(11, 10));
        assertThrows(IllegalArgumentException.class, () -> N.checkPositionIndex(0, -1));
    }

    @Test
    public void checkPositionIndexWithDesc_invalid() {
        IndexOutOfBoundsException ex1 = assertThrows(IndexOutOfBoundsException.class, () -> N.checkPositionIndex(-1, 10, "testPosIndex"));
        assertTrue(ex1.getMessage().contains("testPosIndex (-1) must not be negative"));

        IndexOutOfBoundsException ex2 = assertThrows(IndexOutOfBoundsException.class, () -> N.checkPositionIndex(11, 10, "testPosIndex"));
        assertTrue(ex2.getMessage().contains("testPosIndex (11) must not be greater than size (10)"));

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () -> N.checkPositionIndex(0, -1, "testPosIndex"));
        assertTrue(ex3.getMessage().contains("negative size: -1"));
    }

    @Test
    public void testCheckPositionIndex() {
        Assertions.assertEquals(0, N.checkPositionIndex(0, 10));
        Assertions.assertEquals(5, N.checkPositionIndex(5, 10));
        Assertions.assertEquals(10, N.checkPositionIndex(10, 10));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkPositionIndex(-1, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkPositionIndex(11, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkPositionIndex(0, -1));

        Assertions.assertEquals(5, N.checkPositionIndex(5, 10, "myPosition"));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkPositionIndex(-1, 10, "myPosition"));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.checkPositionIndex(11, 10, "myPosition"));
    }

    @Test
    public void testCheckArgNotNull_ValidArgument() {
        String result = N.checkArgNotNull("test");
        assertEquals("test", result);
    }

    @Test
    public void checkArgNotNull_object_valid() {
        String obj = "test";
        assertSame(obj, N.checkArgNotNull(obj));
        Integer num = 1;
        assertSame(num, N.checkArgNotNull(num));
    }

    @Test
    public void checkArgNotNull_objectWithMessage_valid() {
        String obj = "test";
        assertSame(obj, N.checkArgNotNull(obj, "testObject"));
    }

    @Test
    public void testCheckArgNotNull_NotNull() {
        String str = "test";
        Assertions.assertSame(str, N.checkArgNotNull(str));

        Integer num = 42;
        Assertions.assertSame(num, N.checkArgNotNull(num));
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
    public void checkArgNotNull_object_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null));
    }

    @Test
    public void checkArgNotNull_objectWithMessage_invalid() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null, "testObject"));
        assertEquals("'testObject' cannot be null", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null, "Custom error message for null object"));
        assertEquals("Custom error message for null object", ex2.getMessage());
    }

    @Test
    public void testCheckArgNotNull_WithMessage() {
        String str = "test";
        Assertions.assertSame(str, N.checkArgNotNull(str, "str"));

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null, "myArg"));
        Assertions.assertTrue(ex.getMessage().contains("myArg"));
        Assertions.assertTrue(ex.getMessage().contains("cannot be null"));

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null, "Custom error message here"));
        Assertions.assertEquals("Custom error message here", ex.getMessage());
    }

    @Test
    public void testMessageFormatConsistency() {
        Exception ex1 = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null, "myArg"));
        Exception ex2 = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty("", "myArg"));
        Exception ex3 = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank("  ", "myArg"));

        Assertions.assertTrue(ex1.getMessage().contains("myArg"));
        Assertions.assertTrue(ex2.getMessage().contains("myArg"));
        Assertions.assertTrue(ex3.getMessage().contains("myArg"));
    }

    @Test
    public void testCheckArgNotNull() {
        String str = "test";
        Assertions.assertEquals(str, N.checkArgNotNull(str));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null));

        Assertions.assertEquals(str, N.checkArgNotNull(str, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null, "this is a longer error message"));
    }

    @Test
    public void checkArgNotEmpty_charSequence_valid() {
        String s = "test";
        assertSame(s, N.checkArgNotEmpty(s, "charSeq"));
        StringBuilder sb = new StringBuilder("abc");
        assertSame(sb, N.checkArgNotEmpty(sb, "charSeqBuilder"));
    }

    @Test
    public void checkArgNotEmpty_booleanArray_valid() {
        boolean[] arr = { true, false };
        assertSame(arr, N.checkArgNotEmpty(arr, "boolArray"));
    }

    @Test
    public void checkArgNotEmpty_charArray_valid() {
        char[] arr = { 'a', 'b' };
        assertSame(arr, N.checkArgNotEmpty(arr, "charArr"));
    }

    @Test
    public void checkArgNotEmpty_byteArray_valid() {
        byte[] arr = { 1, 2 };
        assertSame(arr, N.checkArgNotEmpty(arr, "byteArr"));
    }

    @Test
    public void checkArgNotEmpty_shortArray_valid() {
        short[] arr = { 1, 2 };
        assertSame(arr, N.checkArgNotEmpty(arr, "shortArr"));
    }

    @Test
    public void checkArgNotEmpty_intArray_valid() {
        int[] arr = { 1, 2 };
        assertSame(arr, N.checkArgNotEmpty(arr, "intArr"));
    }

    @Test
    public void checkArgNotEmpty_longArray_valid() {
        long[] arr = { 1L, 2L };
        assertSame(arr, N.checkArgNotEmpty(arr, "longArr"));
    }

    @Test
    public void checkArgNotEmpty_floatArray_valid() {
        float[] arr = { 1.0f, 2.0f };
        assertSame(arr, N.checkArgNotEmpty(arr, "floatArr"));
    }

    @Test
    public void checkArgNotEmpty_doubleArray_valid() {
        double[] arr = { 1.0, 2.0 };
        assertSame(arr, N.checkArgNotEmpty(arr, "doubleArr"));
    }

    @Test
    public void checkArgNotEmpty_objectArray_valid() {
        String[] arr = { "a", "b" };
        assertSame(arr, N.checkArgNotEmpty(arr, "objArr"));
    }

    @Test
    public void checkArgNotEmpty_map_valid() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertSame(map, N.checkArgNotEmpty(map, "map"));
    }

    @Test
    public void checkArgNotEmpty_primitiveList_valid() {
        BooleanList pList = BooleanList.of(true, false);
        assertSame(pList, N.checkArgNotEmpty(pList, "pList"));
    }

    @Test
    public void checkArgNotEmpty_multiset_valid() {
        Multiset<String> multiset = Multiset.of("a", "b");
        assertSame(multiset, N.checkArgNotEmpty(multiset, "multiset"));
    }

    @Test
    public void testCheckArgNotEmpty_CharSequence_Valid() {
        String result = N.checkArgNotEmpty("test", "argName");
        Assertions.assertEquals("test", result);

        StringBuilder sb = new StringBuilder("builder");
        StringBuilder result2 = N.checkArgNotEmpty(sb, "argName");
        Assertions.assertEquals(sb, result2);
    }

    @Test
    public void testCheckArgNotEmpty_BooleanArray_Valid() {
        boolean[] arr = { true, false };
        boolean[] result = N.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_CharArray_Valid() {
        char[] arr = { 'a', 'b' };
        char[] result = N.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_ByteArray_Valid() {
        byte[] arr = { 1, 2 };
        byte[] result = N.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_ShortArray_Valid() {
        short[] arr = { 1, 2 };
        short[] result = N.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_IntArray_Valid() {
        int[] arr = { 1, 2 };
        int[] result = N.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_LongArray_Valid() {
        long[] arr = { 1L, 2L };
        long[] result = N.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_FloatArray_Valid() {
        float[] arr = { 1.0f, 2.0f };
        float[] result = N.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_DoubleArray_Valid() {
        double[] arr = { 1.0, 2.0 };
        double[] result = N.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_ObjectArray_Valid() {
        String[] arr = { "a", "b" };
        String[] result = N.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_Collection_Valid() {
        List<String> list = Arrays.asList("a", "b");
        List<String> result = N.checkArgNotEmpty(list, "argName");
        Assertions.assertEquals(list, result);

        Set<Integer> set = new HashSet<>();
        set.add(1);
        Set<Integer> result2 = N.checkArgNotEmpty(set, "argName");
        Assertions.assertEquals(set, result2);
    }

    @Test
    public void testCheckArgNotEmpty_Iterable_Valid() {
        List<String> list = Arrays.asList("a", "b");
        Iterable<String> iterable = list;
        Iterable<String> result = N.checkArgNotEmpty(iterable, "argName");
        Assertions.assertEquals(iterable, result);
    }

    @Test
    public void testCheckArgNotEmpty_Iterator_Valid() {
        List<String> list = Arrays.asList("a", "b");
        Iterator<String> iterator = list.iterator();
        Iterator<String> result = N.checkArgNotEmpty(iterator, "argName");
        Assertions.assertEquals(iterator, result);
    }

    @Test
    public void testCheckArgNotEmpty_Map_Valid() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Map<String, Integer> result = N.checkArgNotEmpty(map, "argName");
        Assertions.assertEquals(map, result);
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
    public void checkArgNotEmpty_charSequence_invalid() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((String) null, "charSeq"));
        assertEquals("'charSeq' cannot be null or empty", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty("", "charSeq"));
        assertEquals("'charSeq' cannot be null or empty", ex2.getMessage());

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class,
                () -> N.checkArgNotEmpty((String) null, "Custom error message for null/empty charSeq"));
        assertEquals("Custom error message for null/empty charSeq", ex3.getMessage());
    }

    @Test
    public void checkArgNotEmpty_booleanArray_invalid() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((boolean[]) null, "boolArray"));
        assertEquals("'boolArray' cannot be null or empty", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new boolean[0], "boolArray"));
        assertEquals("'boolArray' cannot be null or empty", ex2.getMessage());
    }

    @Test
    public void checkArgNotEmpty_charArray_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((char[]) null, "charArr"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new char[0], "charArr"));
    }

    @Test
    public void checkArgNotEmpty_byteArray_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((byte[]) null, "byteArr"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new byte[0], "byteArr"));
    }

    @Test
    public void checkArgNotEmpty_shortArray_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((short[]) null, "shortArr"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new short[0], "shortArr"));
    }

    @Test
    public void checkArgNotEmpty_intArray_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((int[]) null, "intArr"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new int[0], "intArr"));
    }

    @Test
    public void checkArgNotEmpty_longArray_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((long[]) null, "longArr"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new long[0], "longArr"));
    }

    @Test
    public void checkArgNotEmpty_floatArray_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((float[]) null, "floatArr"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new float[0], "floatArr"));
    }

    @Test
    public void checkArgNotEmpty_doubleArray_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((double[]) null, "doubleArr"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new double[0], "doubleArr"));
    }

    @Test
    public void checkArgNotEmpty_objectArray_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Object[]) null, "objArr"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new Object[0], "objArr"));
    }

    @Test
    public void checkArgNotEmpty_collection_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Collection<?>) null, "coll"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(Collections.emptyList(), "coll"));
    }

    @Test
    public void checkArgNotEmpty_iterable_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Iterable<?>) null, "iterable"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(Collections.emptyList(), "iterable"));
    }

    @Test
    public void checkArgNotEmpty_iterator_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Iterator<?>) null, "iterator"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(Collections.emptyIterator(), "iterator"));

        List<String> listWithOne = new ArrayList<>(Collections.singletonList("a"));
        Iterator<String> iter = listWithOne.iterator();
        iter.next();
        Iterator<String> consumedIterator = Arrays.asList("a").iterator();
        consumedIterator.next();
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(consumedIterator, "consumedIterator"));

        Iterator<String> trulyEmptyIterator = Collections.emptyIterator();
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(trulyEmptyIterator, "trulyEmptyIterator"));

    }

    @Test
    public void checkArgNotEmpty_map_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Map<?, ?>) null, "map"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(Collections.emptyMap(), "map"));
    }

    @Test
    public void checkArgNotEmpty_primitiveList_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((BooleanList) null, "pList"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new BooleanList(), "pList"));
    }

    @Test
    public void checkArgNotEmpty_multiset_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Multiset<String>) null, "multiset"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(N.newMultiset(), "multiset"));
    }

    @Test
    public void checkArgNotEmpty_multimap_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Multimap<?, ?, ?>) null, "multimap"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(N.newListMultimap(), "multimap"));
    }

    @Test
    public void checkArgNotEmpty_dataset_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Dataset) null, "dataset"));
        Dataset emptyDs = N.emptyDataset();
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(emptyDs, "dataset"));
    }

    @Test
    public void testCheckArgNotEmpty_CharSequence_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((String) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_CharSequence_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty("", "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_BooleanArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((boolean[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_BooleanArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new boolean[0], "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_CharArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((char[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_CharArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new char[0], "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_ByteArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((byte[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_ByteArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new byte[0], "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_ShortArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((short[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_ShortArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new short[0], "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_IntArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((int[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_IntArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new int[0], "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_LongArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((long[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_LongArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new long[0], "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_FloatArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((float[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_FloatArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new float[0], "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_DoubleArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((double[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_DoubleArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new double[0], "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_ObjectArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((String[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_ObjectArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new String[0], "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_Collection_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((Collection<?>) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_Collection_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new ArrayList<>(), "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_Iterable_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((Iterable<?>) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_Iterable_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            List<String> list = new ArrayList<>();
            N.checkArgNotEmpty((Iterable<String>) list, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_Iterator_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((Iterator<?>) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_Iterator_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            List<String> list = new ArrayList<>();
            N.checkArgNotEmpty(list.iterator(), "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_Map_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((Map<?, ?>) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_Map_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new HashMap<>(), "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_CharSequence_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((CharSequence) null, "str"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty("", "str"));
    }

    @Test
    public void testCheckArgNotEmpty_BooleanArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((boolean[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new boolean[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_CharArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((char[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new char[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ByteArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((byte[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new byte[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ShortArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((short[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new short[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_IntArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((int[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new int[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_LongArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((long[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new long[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_FloatArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((float[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new float[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_DoubleArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((double[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new double[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_ObjectArray_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((String[]) null, "arr"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new String[0], "arr"));
    }

    @Test
    public void testCheckArgNotEmpty_Collection_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Collection<?>) null, "coll"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new ArrayList<>(), "coll"));
    }

    @Test
    public void testCheckArgNotEmpty_Map_NullOrEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Map<?, ?>) null, "map"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new HashMap<>(), "map"));
    }

    @Test
    public void testCheckArgNotEmptyCharSequence() {
        Assertions.assertEquals("test", N.checkArgNotEmpty("test", "myArg"));
        Assertions.assertEquals("a", N.checkArgNotEmpty("a", "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((CharSequence) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty("", "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((CharSequence) null, "this is a longer error message"));
    }

    @Test
    public void testCheckArgNotEmptyBooleanArray() {
        boolean[] arr = { true, false };
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((boolean[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new boolean[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyCharArray() {
        char[] arr = { 'a', 'b' };
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((char[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new char[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyByteArray() {
        byte[] arr = { 1, 2 };
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((byte[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new byte[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyShortArray() {
        short[] arr = { 1, 2 };
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((short[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new short[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyIntArray() {
        int[] arr = { 1, 2 };
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((int[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new int[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyLongArray() {
        long[] arr = { 1L, 2L };
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((long[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new long[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyFloatArray() {
        float[] arr = { 1.0f, 2.0f };
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((float[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new float[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyDoubleArray() {
        double[] arr = { 1.0, 2.0 };
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((double[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new double[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyObjectArray() {
        String[] arr = { "a", "b" };
        Assertions.assertArrayEquals(arr, N.checkArgNotEmpty(arr, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((String[]) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new String[0], "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyCollection() {
        List<String> list = Arrays.asList("a", "b");
        Assertions.assertEquals(list, N.checkArgNotEmpty(list, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Collection<?>) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new ArrayList<>(), "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyIterable() {
        List<String> list = Arrays.asList("a", "b");
        Assertions.assertEquals(list, N.checkArgNotEmpty((Iterable<?>) list, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Iterable<?>) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Iterable<?>) new ArrayList<>(), "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyIterator() {
        List<String> list = Arrays.asList("a", "b");
        Iterator<String> iter = list.iterator();
        Assertions.assertEquals(iter, N.checkArgNotEmpty(iter, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Iterator<?>) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new ArrayList<>().iterator(), "myArg"));
    }

    @Test
    public void testCheckArgNotEmptyMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Assertions.assertEquals(map, N.checkArgNotEmpty(map, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Map<?, ?>) null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new HashMap<>(), "myArg"));
    }

    // --- Tests for checkArgNotEmpty (PrimitiveList, Multiset, Multimap, Dataset) ---

    @Test
    public void testCheckArgNotEmpty_PrimitiveList() {
        IntList list = IntList.of(1, 2, 3);
        assertSame(list, N.checkArgNotEmpty(list, "list"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((IntList) null, "list"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(IntList.of(), "list"));
    }

    @Test
    public void testCheckArgNotEmpty_Multiset() {
        Multiset<String> ms = new Multiset<>();
        ms.add("a");
        assertSame(ms, N.checkArgNotEmpty(ms, "ms"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Multiset<String>) null, "ms"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new Multiset<>(), "ms"));
    }

    @Test
    public void testCheckArgNotEmpty_Multimap() {
        ListMultimap<String, Integer> mm = N.newListMultimap();
        mm.put("key", 1);
        assertSame(mm, N.checkArgNotEmpty(mm, "mm"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((ListMultimap<String, Integer>) null, "mm"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(N.newListMultimap(), "mm"));
    }

    @Test
    public void testCheckArgNotEmpty_Dataset() {
        Dataset ds = N.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        assertSame(ds, N.checkArgNotEmpty(ds, "ds"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Dataset) null, "ds"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(N.newEmptyDataset(), "ds"));
    }

    @Test
    public void testCheckArgNotBlank_ValidString() {
        assertEquals("test", N.checkArgNotBlank("test", "str"));
        assertEquals(" test ", N.checkArgNotBlank(" test ", "str"));
    }

    @Test
    public void checkArgNotBlank_valid() {
        String s = "test";
        assertSame(s, N.checkArgNotBlank(s, "notBlankStr"));
        String s2 = "  test  ";
        assertSame(s2, N.checkArgNotBlank(s2, "notBlankStrWithSpaces"));
    }

    @Test
    public void testCheckArgNotBlank_Valid() {
        String str = "test";
        Assertions.assertSame(str, N.checkArgNotBlank(str, "str"));

        StringBuilder sb = new StringBuilder("  test  ");
        Assertions.assertSame(sb, N.checkArgNotBlank(sb, "sb"));
    }

    @Test
    public void testCheckArgNotBlank_BlankString() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank("", "str"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank("   ", "str"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank((String) null, "str"));
    }

    @Test
    public void checkArgNotBlank_invalid() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank(null, "notBlankStr"));
        assertEquals("'notBlankStr' cannot be null or empty or blank", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank("", "notBlankStr"));
        assertEquals("'notBlankStr' cannot be null or empty or blank", ex2.getMessage());

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank("   ", "notBlankStr"));
        assertEquals("'notBlankStr' cannot be null or empty or blank", ex3.getMessage());

        IllegalArgumentException ex4 = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank(null, "Custom error for blank string"));
        assertEquals("Custom error for blank string", ex4.getMessage());
    }

    @Test
    public void testCheckArgNotBlank_Invalid() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank(null, "str"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank("", "str"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank("   ", "str"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank("\t\n", "str"));
    }

    @Test
    public void testCheckArgNotBlank() {
        Assertions.assertEquals("test", N.checkArgNotBlank("test", "myArg"));
        Assertions.assertEquals("a", N.checkArgNotBlank("a", "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank(null, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank("", "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank(" ", "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank("   ", "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotBlank("\t\n", "myArg"));
    }

    @Test
    public void checkArgNotNegative_byte_valid() {
        assertEquals((byte) 0, N.checkArgNotNegative((byte) 0, "byteArg"));
        assertEquals((byte) 10, N.checkArgNotNegative((byte) 10, "byteArg"));
    }

    @Test
    public void checkArgNotNegative_short_valid() {
        assertEquals((short) 0, N.checkArgNotNegative((short) 0, "shortArg"));
        assertEquals((short) 100, N.checkArgNotNegative((short) 100, "shortArg"));
    }

    @Test
    public void checkArgNotNegative_int_valid() {
        assertEquals(0, N.checkArgNotNegative(0, "intArg"));
        assertEquals(1000, N.checkArgNotNegative(1000, "intArg"));
    }

    @Test
    public void checkArgNotNegative_long_valid() {
        assertEquals(0L, N.checkArgNotNegative(0L, "longArg"));
        assertEquals(10000L, N.checkArgNotNegative(10000L, "longArg"));
    }

    @Test
    public void checkArgNotNegative_float_valid() {
        assertEquals(0.0f, N.checkArgNotNegative(0.0f, "floatArg"), 0.0f);
        assertEquals(10.5f, N.checkArgNotNegative(10.5f, "floatArg"), 0.0f);
    }

    @Test
    public void checkArgNotNegative_double_valid() {
        assertEquals(0.0, N.checkArgNotNegative(0.0, "doubleArg"), 0.0);
        assertEquals(10.5, N.checkArgNotNegative(10.5, "doubleArg"), 0.0);
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
    public void checkArgNotNegative_byte_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative((byte) -1, "byteArg"));
        assertEquals("'byteArg' cannot be negative: -1", ex.getMessage());
        IllegalArgumentException exCustom = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative((byte) -1, "Custom msg"));
        assertEquals("Custom msg", exCustom.getMessage());
    }

    @Test
    public void checkArgNotNegative_short_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative((short) -1, "shortArg"));
    }

    @Test
    public void checkArgNotNegative_int_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-1, "intArg"));
    }

    @Test
    public void checkArgNotNegative_long_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-1L, "longArg"));
    }

    @Test
    public void checkArgNotNegative_float_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-0.1f, "floatArg"));
    }

    @Test
    public void checkArgNotNegative_double_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-0.1, "doubleArg"));
    }

    @Test
    public void testCheckArgNotNegative_Byte_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative((byte) -1, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(Byte.MIN_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Short_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative((short) -1, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(Short.MIN_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Int_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-1, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(Integer.MIN_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Long_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-1L, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(Long.MIN_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Float_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-0.1f, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-Float.MAX_VALUE, "val"));
    }

    @Test
    public void testCheckArgNotNegative_Double_Negative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-0.1, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-Double.MAX_VALUE, "val"));
    }

    @Test
    public void testBoundaryValues() {
        assertDoesNotThrow(() -> {
            N.checkArgNotNegative(0, "zero");
            N.checkArgPositive(1, "one");

            N.checkElementIndex(0, 1);
            N.checkPositionIndex(1, 1);

            N.checkElementNotNull(new Object[0]);
            N.checkElementNotNull(new ArrayList<>());
            N.checkKeyNotNull(new HashMap<>());
            N.checkValueNotNull(new HashMap<>());
        });
    }

    @Test
    public void testCheckArgNotNegativeByte() {
        Assertions.assertEquals((byte) 0, N.checkArgNotNegative((byte) 0, "myArg"));
        Assertions.assertEquals((byte) 10, N.checkArgNotNegative((byte) 10, "myArg"));
        Assertions.assertEquals(Byte.MAX_VALUE, N.checkArgNotNegative(Byte.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative((byte) -1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(Byte.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgNotNegativeShort() {
        Assertions.assertEquals((short) 0, N.checkArgNotNegative((short) 0, "myArg"));
        Assertions.assertEquals((short) 10, N.checkArgNotNegative((short) 10, "myArg"));
        Assertions.assertEquals(Short.MAX_VALUE, N.checkArgNotNegative(Short.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative((short) -1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(Short.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgNotNegativeInt() {
        Assertions.assertEquals(0, N.checkArgNotNegative(0, "myArg"));
        Assertions.assertEquals(10, N.checkArgNotNegative(10, "myArg"));
        Assertions.assertEquals(Integer.MAX_VALUE, N.checkArgNotNegative(Integer.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(Integer.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgNotNegativeLong() {
        Assertions.assertEquals(0L, N.checkArgNotNegative(0L, "myArg"));
        Assertions.assertEquals(10L, N.checkArgNotNegative(10L, "myArg"));
        Assertions.assertEquals(Long.MAX_VALUE, N.checkArgNotNegative(Long.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-1L, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(Long.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgNotNegativeFloat() {
        Assertions.assertEquals(0.0f, N.checkArgNotNegative(0.0f, "myArg"));
        Assertions.assertEquals(10.5f, N.checkArgNotNegative(10.5f, "myArg"));
        Assertions.assertEquals(Float.MAX_VALUE, N.checkArgNotNegative(Float.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-0.1f, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-Float.MAX_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgNotNegativeDouble() {
        Assertions.assertEquals(0.0, N.checkArgNotNegative(0.0, "myArg"));
        Assertions.assertEquals(10.5, N.checkArgNotNegative(10.5, "myArg"));
        Assertions.assertEquals(Double.MAX_VALUE, N.checkArgNotNegative(Double.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-0.1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-Double.MAX_VALUE, "myArg"));
    }

    // ========== checkArgNotNegative with full error message (else branch) ==========

    @Test
    public void testCheckArgNotNegative_Short_WithFullErrorMessage_ThrowsIllegalArgument() {
        // "value must not be negative" is >9 chars with space -> isArgNameOnly=false -> else branch (L1694)
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative((short) -1, "value must not be negative"));
        assertEquals("value must not be negative", ex.getMessage());
    }

    @Test
    public void testCheckArgNotNegative_Int_WithFullErrorMessage_ThrowsIllegalArgument() {
        // else branch at L1724
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-5, "int value must not be negative"));
        assertEquals("int value must not be negative", ex.getMessage());
    }

    @Test
    public void testCheckArgNotNegative_Long_WithFullErrorMessage_ThrowsIllegalArgument() {
        // else branch at L1754
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-1L, "long value must not be negative"));
        assertEquals("long value must not be negative", ex.getMessage());
    }

    @Test
    public void testCheckArgNotNegative_Float_WithFullErrorMessage_ThrowsIllegalArgument() {
        // else branch at L1783
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-1.0f, "float value must not be negative"));
        assertEquals("float value must not be negative", ex.getMessage());
    }

    @Test
    public void testCheckArgNotNegative_Double_WithFullErrorMessage_ThrowsIllegalArgument() {
        // else branch at L1812
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-1.0, "double value must not be negative"));
        assertEquals("double value must not be negative", ex.getMessage());
    }

    @Test
    public void checkArgPositive_byte_valid() {
        assertEquals((byte) 1, N.checkArgPositive((byte) 1, "byteArg"));
        assertEquals((byte) 127, N.checkArgPositive((byte) 127, "byteArg"));
    }

    @Test
    public void checkArgPositive_short_valid() {
        assertEquals((short) 1, N.checkArgPositive((short) 1, "shortArg"));
    }

    @Test
    public void checkArgPositive_int_valid() {
        assertEquals(1, N.checkArgPositive(1, "intArg"));
    }

    @Test
    public void checkArgPositive_long_valid() {
        assertEquals(1L, N.checkArgPositive(1L, "longArg"));
    }

    @Test
    public void checkArgPositive_float_valid() {
        assertEquals(0.1f, N.checkArgPositive(0.1f, "floatArg"), 0.0f);
    }

    @Test
    public void checkArgPositive_double_valid() {
        assertEquals(0.1, N.checkArgPositive(0.1, "doubleArg"), 0.0);
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
    public void checkArgPositive_byte_invalid() {
        IllegalArgumentException ex0 = assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((byte) 0, "byteArg"));
        assertEquals("'byteArg' cannot be zero or negative: 0", ex0.getMessage());

        IllegalArgumentException exNeg = assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((byte) -1, "byteArg"));
        assertEquals("'byteArg' cannot be zero or negative: -1", exNeg.getMessage());

        IllegalArgumentException exCustom = assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((byte) 0, "Custom msg"));
        assertEquals("Custom msg", exCustom.getMessage());
    }

    @Test
    public void checkArgPositive_short_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((short) 0, "shortArg"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((short) -1, "shortArg"));
    }

    @Test
    public void checkArgPositive_int_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0, "intArg"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-1, "intArg"));
    }

    @Test
    public void checkArgPositive_long_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0L, "longArg"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-1L, "longArg"));
    }

    @Test
    public void checkArgPositive_float_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0.0f, "floatArg"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-0.1f, "floatArg"));
    }

    @Test
    public void checkArgPositive_double_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0.0, "doubleArg"));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-0.1, "doubleArg"));
    }

    @Test
    public void testCheckArgPositive_Byte_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((byte) 0, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((byte) -1, "val"));
    }

    @Test
    public void testCheckArgPositive_Short_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((short) 0, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((short) -1, "val"));
    }

    @Test
    public void testCheckArgPositive_Int_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-1, "val"));
    }

    @Test
    public void testCheckArgPositive_Long_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0L, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-1L, "val"));
    }

    @Test
    public void testCheckArgPositive_Float_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0.0f, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-0.1f, "val"));
    }

    @Test
    public void testCheckArgPositive_Double_NotPositive() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0.0, "val"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-0.1, "val"));
    }

    @Test
    public void testCheckArgPositiveByte() {
        Assertions.assertEquals((byte) 1, N.checkArgPositive((byte) 1, "myArg"));
        Assertions.assertEquals((byte) 10, N.checkArgPositive((byte) 10, "myArg"));
        Assertions.assertEquals(Byte.MAX_VALUE, N.checkArgPositive(Byte.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((byte) 0, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((byte) -1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(Byte.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgPositiveShort() {
        Assertions.assertEquals((short) 1, N.checkArgPositive((short) 1, "myArg"));
        Assertions.assertEquals((short) 10, N.checkArgPositive((short) 10, "myArg"));
        Assertions.assertEquals(Short.MAX_VALUE, N.checkArgPositive(Short.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((short) 0, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((short) -1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(Short.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgPositiveInt() {
        Assertions.assertEquals(1, N.checkArgPositive(1, "myArg"));
        Assertions.assertEquals(10, N.checkArgPositive(10, "myArg"));
        Assertions.assertEquals(Integer.MAX_VALUE, N.checkArgPositive(Integer.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(Integer.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgPositiveLong() {
        Assertions.assertEquals(1L, N.checkArgPositive(1L, "myArg"));
        Assertions.assertEquals(10L, N.checkArgPositive(10L, "myArg"));
        Assertions.assertEquals(Long.MAX_VALUE, N.checkArgPositive(Long.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0L, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-1L, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(Long.MIN_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgPositiveFloat() {
        Assertions.assertEquals(0.1f, N.checkArgPositive(0.1f, "myArg"));
        Assertions.assertEquals(10.5f, N.checkArgPositive(10.5f, "myArg"));
        Assertions.assertEquals(Float.MAX_VALUE, N.checkArgPositive(Float.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0.0f, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-0.1f, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-Float.MAX_VALUE, "myArg"));
    }

    @Test
    public void testCheckArgPositiveDouble() {
        Assertions.assertEquals(0.1, N.checkArgPositive(0.1, "myArg"));
        Assertions.assertEquals(10.5, N.checkArgPositive(10.5, "myArg"));
        Assertions.assertEquals(Double.MAX_VALUE, N.checkArgPositive(Double.MAX_VALUE, "myArg"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0.0, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-0.1, "myArg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-Double.MAX_VALUE, "myArg"));
    }

    // ========== checkArgPositive with full error message (else branch) ==========

    @Test
    public void testCheckArgPositive_Short_WithFullErrorMessage_ThrowsIllegalArgument() {
        // else branch at L1870
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((short) 0, "short value must be positive"));
        assertEquals("short value must be positive", ex.getMessage());
    }

    @Test
    public void testCheckArgPositive_Int_WithFullErrorMessage_ThrowsIllegalArgument() {
        // else branch at L1900
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0, "int value must be positive"));
        assertEquals("int value must be positive", ex.getMessage());
    }

    @Test
    public void testCheckArgPositive_Long_WithFullErrorMessage_ThrowsIllegalArgument() {
        // else branch at L1930
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0L, "long value must be positive"));
        assertEquals("long value must be positive", ex.getMessage());
    }

    @Test
    public void testCheckArgPositive_Float_WithFullErrorMessage_ThrowsIllegalArgument() {
        // else branch at L1959
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0.0f, "float value must be positive"));
        assertEquals("float value must be positive", ex.getMessage());
    }

    @Test
    public void testCheckArgPositive_Double_WithFullErrorMessage_ThrowsIllegalArgument() {
        // else branch at L1989
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0.0, "double value must be positive"));
        assertEquals("double value must be positive", ex.getMessage());
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
    public void checkElementNotNull_array_valid() {
        assertDoesNotThrow(() -> N.checkElementNotNull(new String[] { "a", "b" }));
        assertDoesNotThrow(() -> N.checkElementNotNull(new String[0]));
        assertDoesNotThrow(() -> N.checkElementNotNull((Object[]) null));
    }

    @Test
    public void checkElementNotNull_array_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(new String[] { "a", null, "b" }));
        assertEquals("null element is found in array", ex.getMessage());
    }

    @Test
    public void checkElementNotNull_arrayWithMessage_valid() {
        assertDoesNotThrow(() -> N.checkElementNotNull(new String[] { "a", "b" }, "myArray"));
    }

    @Test
    public void checkElementNotNull_arrayWithMessage_invalid() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(new String[] { "a", null }, "myArray"));
        assertEquals("null element is found in myArray", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> N.checkElementNotNull(new String[] { null }, "Custom error for null element in array"));
        assertEquals("Custom error for null element in array", ex2.getMessage());
    }

    @Test
    public void checkElementNotNull_collection_valid() {
        assertDoesNotThrow(() -> N.checkElementNotNull(Arrays.asList("a", "b")));
        assertDoesNotThrow(() -> N.checkElementNotNull(Collections.emptyList()));
        assertDoesNotThrow(() -> N.checkElementNotNull((Collection<?>) null));
    }

    @Test
    public void checkElementNotNull_collection_invalid() {
        List<String> listWithNull = new ArrayList<>();
        listWithNull.add("a");
        listWithNull.add(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(listWithNull));
        assertEquals("null element is found in collection", ex.getMessage());
    }

    @Test
    public void checkElementNotNull_collectionWithMessage_valid() {
        assertDoesNotThrow(() -> N.checkElementNotNull(Arrays.asList("a", "b"), "myColl"));
    }

    @Test
    public void checkElementNotNull_collectionWithMessage_invalid() {
        List<String> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(listWithNull, "myColl"));
        assertEquals("null element is found in myColl", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> N.checkElementNotNull(listWithNull, "Custom error for null element in collection"));
        assertEquals("Custom error for null element in collection", ex2.getMessage());
    }

    @Test
    public void testCheckElementNotNull_ObjectArray_NoNulls() {
        assertDoesNotThrow(() -> {
            N.checkElementNotNull(new String[] { "a", "b", "c" });
            N.checkElementNotNull(new Integer[] { 1, 2, 3 });
            N.checkElementNotNull(new Object[0]);
        });
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

    @Test
    public void testCheckElementNotNull_Collection_NoNulls() {
        assertDoesNotThrow(() -> {
            N.checkElementNotNull(Arrays.asList("a", "b", "c"));
            N.checkElementNotNull(new HashSet<>(Arrays.asList(1, 2, 3)));
            N.checkElementNotNull(new ArrayList<>());
        });
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
    public void testCheckElementNotNullArray() {
        String[] arr = { "a", "b", "c" };
        N.checkElementNotNull(arr);

        String[] nullArr = { "a", null, "c" };
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(nullArr));

        N.checkElementNotNull(new String[0]);
        N.checkElementNotNull((String[]) null);

        N.checkElementNotNull(arr, "myArray");
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(nullArr, "myArray"));
    }

    @Test
    public void testCheckElementNotNullCollection() {
        List<String> list = Arrays.asList("a", "b", "c");
        N.checkElementNotNull(list);

        List<String> nullList = Arrays.asList("a", null, "c");
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(nullList));

        N.checkElementNotNull(new ArrayList<>());
        N.checkElementNotNull((Collection<?>) null);

        N.checkElementNotNull(list, "myList");
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(nullList, "myList"));
    }

    // ========== checkElementNotNull / checkKeyNotNull / checkValueNotNull early return (empty container) ==========

    @Test
    public void testCheckElementNotNull_ObjectArrayWithString_EmptyArray_ReturnsEarly() {
        // L2041: isEmpty(a) -> return early (no exception)
        assertDoesNotThrow(() -> N.checkElementNotNull(new Object[0], "arr"));
        assertDoesNotThrow(() -> N.checkElementNotNull((Object[]) null, "arr"));
    }

    @Test
    public void testCheckElementNotNull_CollectionWithString_EmptyCollection_ReturnsEarly() {
        // L2100: isEmpty(c) -> return early (no exception)
        assertDoesNotThrow(() -> N.checkElementNotNull(Collections.emptyList(), "myList"));
        assertDoesNotThrow(() -> N.checkElementNotNull((Collection<?>) null, "myList"));
    }

    @Test
    public void testCheckKeyNotNull_NoNullKeys() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        N.checkKeyNotNull(map);

        N.checkKeyNotNull(new HashMap<>());
        assertNotNull(map);
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
    public void checkKeyNotNull_map_valid() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", null);
        assertDoesNotThrow(() -> N.checkKeyNotNull(map));
        assertDoesNotThrow(() -> N.checkKeyNotNull(Collections.emptyMap()));
        assertDoesNotThrow(() -> N.checkKeyNotNull((Map<?, ?>) null));
    }

    @Test
    public void checkKeyNotNull_map_invalid() {
        Map<String, Integer> mapWithNullKey = new HashMap<>();
        mapWithNullKey.put(null, 1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkKeyNotNull(mapWithNullKey));
        assertEquals("null key is found in Map", ex.getMessage());
    }

    @Test
    public void checkKeyNotNull_mapWithMessage_valid() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertDoesNotThrow(() -> N.checkKeyNotNull(map, "myMap"));
    }

    @Test
    public void checkKeyNotNull_mapWithMessage_invalid() {
        Map<String, Integer> mapWithNullKey = new HashMap<>();
        mapWithNullKey.put(null, 1);
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> N.checkKeyNotNull(mapWithNullKey, "myMap"));
        assertEquals("null key is found in myMap", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> N.checkKeyNotNull(mapWithNullKey, "Custom error for null key"));
        assertEquals("Custom error for null key", ex2.getMessage());
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

    @Test
    public void testCheckKeyNotNull() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        N.checkKeyNotNull(map);

        Map<String, Integer> nullKeyMap = new HashMap<>();
        nullKeyMap.put("a", 1);
        nullKeyMap.put(null, 2);
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkKeyNotNull(nullKeyMap));

        N.checkKeyNotNull(new HashMap<>());
        N.checkKeyNotNull((Map<?, ?>) null);

        N.checkKeyNotNull(map, "myMap");
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkKeyNotNull(nullKeyMap, "myMap"));
    }

    @Test
    public void testCheckKeyNotNull_MapWithString_EmptyMap_ReturnsEarly() {
        // L2163: isEmpty(m) -> return early (no exception)
        assertDoesNotThrow(() -> N.checkKeyNotNull(Collections.emptyMap(), "myMap"));
        assertDoesNotThrow(() -> N.checkKeyNotNull((Map<?, ?>) null, "myMap"));
    }

    @Test
    public void testCheckValueNotNull_NoNullValues() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        N.checkValueNotNull(map);

        N.checkValueNotNull(new HashMap<>());
        assertNotNull(map);
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
    public void checkValueNotNull_map_valid() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertDoesNotThrow(() -> N.checkValueNotNull(map));
        Map<Object, String> mapWithNullKey = new HashMap<>();
        mapWithNullKey.put(null, "value");
        assertDoesNotThrow(() -> N.checkValueNotNull(mapWithNullKey));
        assertDoesNotThrow(() -> N.checkValueNotNull(Collections.emptyMap()));
        assertDoesNotThrow(() -> N.checkValueNotNull((Map<?, ?>) null));
    }

    @Test
    public void checkValueNotNull_map_invalid() {
        Map<String, Integer> mapWithNullValue = new HashMap<>();
        mapWithNullValue.put("a", null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkValueNotNull(mapWithNullValue));
        assertEquals("null value is found in Map", ex.getMessage());
    }

    @Test
    public void checkValueNotNull_mapWithMessage_valid() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertDoesNotThrow(() -> N.checkValueNotNull(map, "myMap"));
    }

    @Test
    public void checkValueNotNull_mapWithMessage_invalid() {
        Map<String, Integer> mapWithNullValue = new HashMap<>();
        mapWithNullValue.put("a", null);
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> N.checkValueNotNull(mapWithNullValue, "myMap"));
        assertEquals("null value is found in myMap", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> N.checkValueNotNull(mapWithNullValue, "Custom error for null value"));
        assertEquals("Custom error for null value", ex2.getMessage());
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

    @Test
    public void testCheckValueNotNull() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        N.checkValueNotNull(map);

        Map<String, Integer> nullValueMap = new HashMap<>();
        nullValueMap.put("a", 1);
        nullValueMap.put("b", null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkValueNotNull(nullValueMap));

        N.checkValueNotNull(new HashMap<>());
        N.checkValueNotNull((Map<?, ?>) null);

        N.checkValueNotNull(map, "myMap");
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkValueNotNull(nullValueMap, "myMap"));
    }

    @Test
    public void testCheckValueNotNull_MapWithString_EmptyMap_ReturnsEarly() {
        // L2226: isEmpty(m) -> return early (no exception)
        assertDoesNotThrow(() -> N.checkValueNotNull(Collections.emptyMap(), "myMap"));
        assertDoesNotThrow(() -> N.checkValueNotNull((Map<?, ?>) null, "myMap"));
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
    public void checkArgument_boolean_valid() {
        assertDoesNotThrow(() -> N.checkArgument(true));
    }

    @Test
    public void checkArgument_boolean_invalid() {
        assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false));
    }

    @Test
    public void checkArgument_booleanObject_valid() {
        assertDoesNotThrow(() -> N.checkArgument(true, "Error message"));
    }

    @Test
    public void checkArgument_booleanObject_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Error message"));
        assertEquals("Error message", ex.getMessage());
        IllegalArgumentException exNum = assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, 123));
        assertEquals("123", exNum.getMessage());
    }

    @Test
    public void checkArgument_booleanTemplateArgs_valid() {
        assertDoesNotThrow(() -> N.checkArgument(true, "Message with %s", "arg"));
    }

    @Test
    public void checkArgument_booleanTemplateArgs_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Message with {}", "arg"));
        assertEquals("Message with arg", ex.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "{} and %s", "arg1", "arg2"));
        assertEquals("arg1 and %s: [arg2]", ex2.getMessage());

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "%s and %s", "arg1", "arg2"));
        assertEquals("arg1 and arg2", ex3.getMessage());

        IllegalArgumentException ex4 = assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "No placeholder", "arg1", "arg2"));
        assertEquals("No placeholder: [arg1, arg2]", ex4.getMessage());

        IllegalArgumentException ex5 = assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "One {} two", "arg1", "arg2"));
        assertEquals("One arg1 two: [arg2]", ex5.getMessage());
    }

    @Test
    public void checkArgument_booleanTemplateChar_valid() {
        assertDoesNotThrow(() -> N.checkArgument(true, "Char: {}", 'a'));
    }

    @Test
    public void checkArgument_booleanTemplateChar_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Char: {}", 'a'));
        assertEquals("Char: a", ex.getMessage());
    }

    @Test
    public void checkArgument_booleanTemplateInt_valid() {
        assertDoesNotThrow(() -> N.checkArgument(true, "Int: {}", 10));
    }

    @Test
    public void checkArgument_booleanTemplateInt_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Int: {}", 10));
        assertEquals("Int: 10", ex.getMessage());
    }

    @Test
    public void checkArgument_booleanTemplateLong_valid() {
        assertDoesNotThrow(() -> N.checkArgument(true, "Long: {}", 100L));
    }

    @Test
    public void checkArgument_booleanTemplateLong_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Long: {}", 100L));
        assertEquals("Long: 100", ex.getMessage());
    }

    @Test
    public void checkArgument_booleanTemplateDouble_valid() {
        assertDoesNotThrow(() -> N.checkArgument(true, "Double: {}", 3.14));
    }

    @Test
    public void checkArgument_booleanTemplateDouble_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Double: {}", 3.14));
        assertEquals("Double: 3.14", ex.getMessage());
    }

    @Test
    public void checkArgument_booleanTemplateObject_valid() {
        assertDoesNotThrow(() -> N.checkArgument(true, "Object: {}", "test"));
    }

    @Test
    public void checkArgument_booleanTemplateObject_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Object: {}", "test"));
        assertEquals("Object: test", ex.getMessage());
    }

    @Test
    public void checkArgument_booleanTemplateCharChar_valid() {
        assertDoesNotThrow(() -> N.checkArgument(true, "{}, {}", 'a', 'b'));
    }

    @Test
    public void checkArgument_booleanTemplateCharChar_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "{}, {}", 'a', 'b'));
        assertEquals("a, b", ex.getMessage());
    }

    @Test
    public void checkArgument_booleanTemplateIntObject_valid() {
        assertDoesNotThrow(() -> N.checkArgument(true, "{}, {}", 10, "obj"));
    }

    @Test
    public void checkArgument_booleanTemplateIntObject_invalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "{}, {}", 10, "obj"));
        assertEquals("10, obj", ex.getMessage());
    }

    @Test
    public void checkArgument_booleanSupplier_valid() {
        assertDoesNotThrow(() -> N.checkArgument(true, () -> "Supplier message"));
    }

    @Test
    public void checkArgument_booleanSupplier_invalid() {
        Supplier<String> supplier = () -> "Supplier error message";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, supplier));
        assertEquals("Supplier error message", ex.getMessage());

        final boolean[] supplierCalled = { false };
        Supplier<String> trackingSupplier = () -> {
            supplierCalled[0] = true;
            return "Called";
        };
        assertDoesNotThrow(() -> N.checkArgument(true, trackingSupplier));
        assertFalse(supplierCalled[0], "Supplier should not be called when condition is true");

        assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, trackingSupplier));
        assertTrue(supplierCalled[0], "Supplier should be called when condition is false");
    }

    @Test
    public void testCheckArgument_Varargs_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error message %s %s", "arg1", "arg2");
            N.checkArgument(true, "Error message {} {}", "arg1", "arg2");
        });
    }

    @Test
    public void testCheckArgument_Varargs_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error message %s %s", "arg1", "arg2");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error message {} {}", "arg1", "arg2");
        });
    }

    @Test
    public void testCheckArgument_Char_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error with char: %s", 'a');
        });
    }

    @Test
    public void testCheckArgument_Char_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error with char: %s", 'a');
        });
    }

    @Test
    public void testCheckArgument_Int_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error with int: %s", 42);
        });
    }

    @Test
    public void testCheckArgument_Int_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error with int: %s", 42);
        });
    }

    @Test
    public void testCheckArgument_Long_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error with long: %s", 42L);
        });
    }

    @Test
    public void testCheckArgument_Long_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error with long: %s", 42L);
        });
    }

    @Test
    public void testCheckArgument_Double_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error with double: %s", 3.14);
        });
    }

    @Test
    public void testCheckArgument_Double_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error with double: %s", 3.14);
        });
    }

    @Test
    public void testCheckArgument_Object_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error with object: %s", "test");
        });
    }

    @Test
    public void testCheckArgument_Object_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error with object: %s", "test");
        });
    }

    @Test
    public void testCheckArgument_CharChar_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", 'a', 'b');
        });
    }

    @Test
    public void testCheckArgument_CharChar_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 'a', 'b');
        });
    }

    @Test
    public void testCheckArgument_CharInt_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", 'a', 42);
        });
    }

    @Test
    public void testCheckArgument_CharInt_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 'a', 42);
        });
    }

    @Test
    public void testCheckArgument_CharLong_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", 'a', 42L);
        });
    }

    @Test
    public void testCheckArgument_CharLong_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 'a', 42L);
        });
    }

    @Test
    public void testCheckArgument_CharDouble_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", 'a', 3.14);
        });
    }

    @Test
    public void testCheckArgument_CharDouble_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 'a', 3.14);
        });
    }

    @Test
    public void testCheckArgument_CharObject_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", 'a', "test");
        });
    }

    @Test
    public void testCheckArgument_CharObject_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 'a', "test");
        });
    }

    @Test
    public void testCheckArgument_IntChar_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", 42, 'a');
        });
    }

    @Test
    public void testCheckArgument_IntChar_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 42, 'a');
        });
    }

    @Test
    public void testCheckArgument_IntInt_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", 42, 24);
        });
    }

    @Test
    public void testCheckArgument_IntInt_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 42, 24);
        });
    }

    @Test
    public void testCheckArgument_IntLong_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", 42, 24L);
        });
    }

    @Test
    public void testCheckArgument_IntLong_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 42, 24L);
        });
    }

    @Test
    public void testCheckArgument_IntDouble_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", 42, 3.14);
        });
    }

    @Test
    public void testCheckArgument_IntDouble_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 42, 3.14);
        });
    }

    @Test
    public void testCheckArgument_IntObject_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", 42, "test");
        });
    }

    @Test
    public void testCheckArgument_IntObject_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 42, "test");
        });
    }

    @Test
    public void testCheckArgument_LongChar_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", 42L, 'a');
        });
    }

    @Test
    public void testCheckArgument_LongChar_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 42L, 'a');
        });
    }

    @Test
    public void testCheckArgument_LongInt_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", 42L, 24);
        });
    }

    @Test
    public void testCheckArgument_LongInt_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 42L, 24);
        });
    }

    @Test
    public void testCheckArgument_LongLong_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", 42L, 24L);
        });
    }

    @Test
    public void testCheckArgument_LongLong_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 42L, 24L);
        });
    }

    @Test
    public void testCheckArgument_LongDouble_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", 42L, 3.14);
        });
    }

    @Test
    public void testCheckArgument_LongDouble_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 42L, 3.14);
        });
    }

    @Test
    public void testCheckArgument_LongObject_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", 42L, "test");
        });
    }

    @Test
    public void testCheckArgument_LongObject_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 42L, "test");
        });
    }

    @Test
    public void testCheckArgument_DoubleChar_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", 3.14, 'a');
        });
    }

    @Test
    public void testCheckArgument_DoubleChar_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 3.14, 'a');
        });
    }

    @Test
    public void testCheckArgument_DoubleInt_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", 3.14, 42);
        });
    }

    @Test
    public void testCheckArgument_DoubleInt_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 3.14, 42);
        });
    }

    @Test
    public void testCheckArgument_DoubleLong_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", 3.14, 42L);
        });
    }

    @Test
    public void testCheckArgument_DoubleLong_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 3.14, 42L);
        });
    }

    @Test
    public void testCheckArgument_DoubleDouble_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", 3.14, 2.71);
        });
    }

    @Test
    public void testCheckArgument_DoubleDouble_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 3.14, 2.71);
        });
    }

    @Test
    public void testCheckArgument_DoubleObject_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", 3.14, "test");
        });
    }

    @Test
    public void testCheckArgument_DoubleObject_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 3.14, "test");
        });
    }

    @Test
    public void testCheckArgument_ObjectChar_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", "test", 'a');
        });
    }

    @Test
    public void testCheckArgument_ObjectChar_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", "test", 'a');
        });
    }

    @Test
    public void testCheckArgument_ObjectInt_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", "test", 42);
        });
    }

    @Test
    public void testCheckArgument_ObjectInt_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", "test", 42);
        });
    }

    @Test
    public void testCheckArgument_ObjectLong_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", "test", 42L);
        });
    }

    @Test
    public void testCheckArgument_ObjectLong_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", "test", 42L);
        });
    }

    @Test
    public void testCheckArgument_ObjectDouble_Pass() {
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "Error: %s %s", "test", 3.14);
        });
    }

    @Test
    public void testCheckArgument_ObjectDouble_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", "test", 3.14);
        });
    }

    @Test
    public void testCheckArgument_MessageFormatting() {
        try {
            N.checkArgument(false, "Value {} should be less than {}", 10, 5);
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("10"));
            Assertions.assertTrue(e.getMessage().contains("5"));
        }

        try {
            N.checkArgument(false, "Value %s should be less than %s", 10, 5);
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("10"));
            Assertions.assertTrue(e.getMessage().contains("5"));
        }

        try {
            N.checkArgument(false, "No placeholders", 10, 5);
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("No placeholders"));
            Assertions.assertTrue(e.getMessage().contains("[10, 5]"));
        }

        try {
            N.checkArgument(false, "Only one {}", 10, 5);
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("10"));
            Assertions.assertTrue(e.getMessage().contains("[5]"));
        }
    }

    @Test
    public void testCheckArgument_NullFormatting() {
        try {
            N.checkArgument(false, "Value is {}", (Object) null);
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("null"));
        }

        try {
            N.checkArgument(false, null, "arg1", "arg2");
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("null"));
        }
    }

    @Test
    public void testCheckArgument_BooleanOnly() {
        N.checkArgument(true);

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false));
    }

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

    @Test
    public void testCheckArgument_WithTemplateAndVarargs() {
        N.checkArgument(true, "value is %s", 10);

        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "value %s is invalid", 10));
        Assertions.assertTrue(ex.getMessage().contains("10"));

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "values %s and %s are invalid", 10, 20));
        Assertions.assertTrue(ex.getMessage().contains("10"));
        Assertions.assertTrue(ex.getMessage().contains("20"));

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "value %s", 10, 20, 30));
        Assertions.assertTrue(ex.getMessage().contains("[20, 30]"));
    }

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
        assertDoesNotThrow(() -> {
            N.checkArgument(true, "%s and %s", 'a', 10);
            N.checkArgument(true, "%s and %s", 10, 3.14);
            N.checkArgument(true, "%s and %s", 3.14, "test");
            N.checkArgument(true, "%s and %s", "test", 100L);
        });
    }

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

    @Test
    public void testNullHandling() {
        N.checkArgument(true, "value is %s", (Object) null);
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "value %s", (Object) null));
        Assertions.assertTrue(ex.getMessage().contains("null"));

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "%s and %s", null, null));
        Assertions.assertEquals("null and null", ex.getMessage());
    }

    @Test
    public void testEmptyStringHandling() {
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, ""));
        Assertions.assertEquals("", ex.getMessage());

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "value is '%s'", ""));
        Assertions.assertEquals("value is ''", ex.getMessage());
    }

    @Test
    public void testSpecialCharactersInMessages() {
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Special chars: \n\t\r"));
        Assertions.assertEquals("Special chars: \n\t\r", ex.getMessage());

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Unicode: \u2605 %s", "\u2764"));
        Assertions.assertTrue(ex.getMessage().contains("\u2605"));
        Assertions.assertTrue(ex.getMessage().contains("\u2764"));
    }

    @Test
    public void testCheckArgument() {
        N.checkArgument(true);
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false));

        N.checkArgument(true, "Error");
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Error"));

        N.checkArgument(true, "Error: %s", "test");
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Error: %s", "test"));

        N.checkArgument(true, "Error: %s", 'c');
        N.checkArgument(true, "Error: %s", 10);
        N.checkArgument(true, "Error: %s", 10L);
        N.checkArgument(true, "Error: %s", 10.5);
        N.checkArgument(true, "Error: %s", new Object());

        N.checkArgument(true, "Error: %s %s", 'a', 'b');
        N.checkArgument(true, "Error: %s %s", 'a', 10);
        N.checkArgument(true, "Error: %s %s", 10, 20);
        N.checkArgument(true, "Error: %s %s", 10L, 20L);
        N.checkArgument(true, "Error: %s %s", new Object(), new Object());

        N.checkArgument(true, "Error: %s %s %s", "a", "b", "c");
        N.checkArgument(true, "Error: %s %s %s %s", "a", "b", "c", "d");

        N.checkArgument(true, () -> "Error from supplier");
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, () -> "Error from supplier"));
    }

    // ========== checkArgument / checkState varargs overloads ==========

    @Test
    public void testCheckArgument_Varargs_WhenTrue_DoesNotThrow() {
        // L2299: closing brace when expression is true (no-op path)
        assertDoesNotThrow(() -> N.checkArgument(true, "Value {} must be positive", 5));
    }

    @Test
    public void testCheckArgument_Varargs_WhenFalse_ThrowsWithFormattedMessage() {
        // L2297: throw when expression is false
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "Value {} is invalid", 42));
        assertTrue(ex.getMessage().contains("42"));
    }

    // ========== format(String, Object, Object, Object) uncovered branches ==========

    @Test
    public void testFormat3Args_NoPlaceholders() {
        // placeholderCount == 0: appends ": [arg1, arg2, arg3]"
        String result = N.format("template", "a", "b", "c");
        assertEquals("template: [a, b, c]", result);
    }

    @Test
    public void testFormat3Args_OnePlaceholder() {
        // placeholderCount == 1: uses arg1, then appends ": [arg2, arg3]"
        String result = N.format("Hello {}", "World", "arg2", "arg3");
        assertEquals("Hello World: [arg2, arg3]", result);
    }

    @Test
    public void testFormat3Args_TwoPlaceholders() {
        // placeholderCount == 2: uses arg1 and arg2, then appends ": [arg3]"
        String result = N.format("{} and {}", "a", "b", "c");
        assertEquals("a and b: [c]", result);
    }

    @Test
    public void testFormat3Args_ThreePlaceholders() {
        // placeholderCount == 3: all placeholders consumed, no suffix
        String result = N.format("{} {} {}", "x", "y", "z");
        assertEquals("x y z", result);
    }

    @Test
    public void testFormatVarargs_ExtraArgs() {
        // Extra args beyond placeholders are appended in square braces
        String result = N.format("Hello {}", new Object[] { "World", "extra1", "extra2" });
        assertEquals("Hello World [extra1, extra2]", result);
    }

    @Test
    public void testFormatVarargs_NoPlaceholders() {
        String result = N.format("no placeholders", new Object[] { "x", "y" });
        assertEquals("no placeholders [x, y]", result);
    }

    // ========== format() without placeholder (L2943) ==========

    @Test
    public void testFormat_SingleArg_NoPlaceholder_AppendsWithBrackets() {
        // L2943: no {} or %s placeholder -> appends ": [arg]"
        String result = N.format("template has no placeholder", "someArg");
        assertEquals("template has no placeholder: [someArg]", result);
    }

    @Test
    public void testFormatWithCurlyBraces() {
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "value {} is invalid", 42));
        Assertions.assertEquals("value 42 is invalid", ex.getMessage());

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "{} and {}", "first", "second"));
        Assertions.assertEquals("first and second", ex.getMessage());
    }

    @Test
    public void testFormatWithPercentS() {
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "value %s is invalid", 42));
        Assertions.assertEquals("value 42 is invalid", ex.getMessage());

        ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "%s and %s", "first", "second"));
        Assertions.assertEquals("first and second", ex.getMessage());
    }

    @Test
    public void testFormatWithNoPlaceholders() {
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "error message", "arg1", "arg2"));
        Assertions.assertEquals("error message: [arg1, arg2]", ex.getMessage());
    }

    @Test
    public void testFormatWithFewerPlaceholdersThanArgs() {
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "value %s", 1, 2, 3));
        Assertions.assertEquals("value 1: [2, 3]", ex.getMessage());
    }

    @Test
    public void testFormatWithMorePlaceholdersThanArgs() {
        Exception ex = Assertions.assertThrows(IllegalArgumentException.class, () -> N.checkArgument(false, "%s %s %s", "only one"));
        Assertions.assertEquals("only one %s %s", ex.getMessage());
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
    public void checkState_boolean_valid() {
        assertDoesNotThrow(() -> N.checkState(true));
    }

    @Test
    public void checkState_boolean_invalid() {
        assertThrows(IllegalStateException.class, () -> N.checkState(false));
    }

    @Test
    public void checkState_booleanObject_valid() {
        assertDoesNotThrow(() -> N.checkState(true, "Error message"));
    }

    @Test
    public void checkState_booleanObject_invalid() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> N.checkState(false, "Error message"));
        assertEquals("Error message", ex.getMessage());
        IllegalStateException exNum = assertThrows(IllegalStateException.class, () -> N.checkState(false, 123));
        assertEquals("123", exNum.getMessage());
    }

    @Test
    public void checkState_booleanTemplateArgs_valid() {
        assertDoesNotThrow(() -> N.checkState(true, "Message with %s", "arg"));
    }

    @Test
    public void checkState_booleanTemplateArgs_invalid() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> N.checkState(false, "Message with {}", "arg"));
        assertEquals("Message with arg", ex.getMessage());

        IllegalStateException ex2 = assertThrows(IllegalStateException.class, () -> N.checkState(false, "{} and %s", "arg1", "arg2"));
        assertEquals("arg1 and %s: [arg2]", ex2.getMessage());

        IllegalStateException ex3 = assertThrows(IllegalStateException.class, () -> N.checkState(false, "%s and %s", "arg1", "arg2"));
        assertEquals("arg1 and arg2", ex3.getMessage());

        IllegalStateException ex4 = assertThrows(IllegalStateException.class, () -> N.checkState(false, "No placeholder", "arg1", "arg2"));
        assertEquals("No placeholder: [arg1, arg2]", ex4.getMessage());
    }

    @Test
    public void checkState_booleanTemplateChar_valid() {
        assertDoesNotThrow(() -> N.checkState(true, "Char: {}", 'a'));
    }

    @Test
    public void checkState_booleanTemplateChar_invalid() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> N.checkState(false, "Char: {}", 'a'));
        assertEquals("Char: a", ex.getMessage());
    }

    @Test
    public void checkState_booleanTemplateInt_valid() {
        assertDoesNotThrow(() -> N.checkState(true, "Int: {}", 10));
    }

    @Test
    public void checkState_booleanTemplateInt_invalid() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> N.checkState(false, "Int: {}", 10));
        assertEquals("Int: 10", ex.getMessage());
    }

    @Test
    public void checkState_booleanTemplateLong_valid() {
        assertDoesNotThrow(() -> N.checkState(true, "Long: {}", 100L));
    }

    @Test
    public void checkState_booleanTemplateLong_invalid() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> N.checkState(false, "Long: {}", 100L));
        assertEquals("Long: 100", ex.getMessage());
    }

    @Test
    public void checkState_booleanTemplateDouble_valid() {
        assertDoesNotThrow(() -> N.checkState(true, "Double: {}", 3.14));
    }

    @Test
    public void checkState_booleanTemplateDouble_invalid() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> N.checkState(false, "Double: {}", 3.14));
        assertEquals("Double: 3.14", ex.getMessage());
    }

    @Test
    public void checkState_booleanTemplateObject_valid() {
        assertDoesNotThrow(() -> N.checkState(true, "Object: {}", "test"));
    }

    @Test
    public void checkState_booleanTemplateObject_invalid() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> N.checkState(false, "Object: {}", "test"));
        assertEquals("Object: test", ex.getMessage());
    }

    @Test
    public void checkState_booleanTemplateCharChar_valid() {
        assertDoesNotThrow(() -> N.checkState(true, "{}, {}", 'a', 'b'));
    }

    @Test
    public void checkState_booleanTemplateCharChar_invalid() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> N.checkState(false, "{}, {}", 'a', 'b'));
        assertEquals("a, b", ex.getMessage());
    }

    @Test
    public void checkState_booleanTemplateIntObject_valid() {
        assertDoesNotThrow(() -> N.checkState(true, "{}, {}", 10, "obj"));
    }

    @Test
    public void checkState_booleanTemplateIntObject_invalid() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> N.checkState(false, "{}, {}", 10, "obj"));
        assertEquals("10, obj", ex.getMessage());
    }

    @Test
    public void checkState_booleanSupplier_valid() {
        assertDoesNotThrow(() -> N.checkState(true, () -> "Supplier message"));
    }

    @Test
    public void checkState_booleanSupplier_invalid() {
        Supplier<String> supplier = () -> "Supplier error message for state";
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> N.checkState(false, supplier));
        assertEquals("Supplier error message for state", ex.getMessage());

        final boolean[] supplierCalled = { false };
        Supplier<String> trackingSupplier = () -> {
            supplierCalled[0] = true;
            return "Called";
        };
        assertDoesNotThrow(() -> N.checkState(true, trackingSupplier));
        assertFalse(supplierCalled[0], "Supplier should not be called when condition is true");

        assertThrows(IllegalStateException.class, () -> N.checkState(false, trackingSupplier));
        assertTrue(supplierCalled[0], "Supplier should be called when condition is false");
    }

    @Test
    public void testCheckState_CharChar_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", 'a', 'b');
        });
    }

    @Test
    public void testCheckState_CharChar_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 'a', 'b');
        });
    }

    @Test
    public void testCheckState_CharInt_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", 'a', 42);
        });
    }

    @Test
    public void testCheckState_CharInt_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 'a', 42);
        });
    }

    @Test
    public void testCheckState_CharLong_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", 'a', 42L);
        });
    }

    @Test
    public void testCheckState_CharLong_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 'a', 42L);
        });
    }

    @Test
    public void testCheckState_CharDouble_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", 'a', 3.14);
        });
    }

    @Test
    public void testCheckState_CharDouble_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 'a', 3.14);
        });
    }

    @Test
    public void testCheckState_CharObject_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", 'a', "test");
        });
    }

    @Test
    public void testCheckState_CharObject_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 'a', "test");
        });
    }

    @Test
    public void testCheckState_IntChar_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", 42, 'a');
        });
    }

    @Test
    public void testCheckState_IntChar_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 42, 'a');
        });
    }

    @Test
    public void testCheckState_IntInt_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", 42, 24);
        });
    }

    @Test
    public void testCheckState_IntInt_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 42, 24);
        });
    }

    @Test
    public void testCheckState_IntLong_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", 42, 24L);
        });
    }

    @Test
    public void testCheckState_IntLong_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 42, 24L);
        });
    }

    @Test
    public void testCheckState_IntDouble_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", 42, 3.14);
        });
    }

    @Test
    public void testCheckState_IntDouble_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 42, 3.14);
        });
    }

    @Test
    public void testCheckState_IntObject_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", 42, "test");
        });
    }

    @Test
    public void testCheckState_IntObject_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 42, "test");
        });
    }

    @Test
    public void testCheckState_LongChar_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", 42L, 'a');
        });
    }

    @Test
    public void testCheckState_LongChar_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 42L, 'a');
        });
    }

    @Test
    public void testCheckState_LongInt_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", 42L, 24);
        });
    }

    @Test
    public void testCheckState_LongInt_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 42L, 24);
        });
    }

    @Test
    public void testCheckState_LongLong_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", 42L, 24L);
        });
    }

    @Test
    public void testCheckState_LongLong_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 42L, 24L);
        });
    }

    @Test
    public void testCheckState_LongDouble_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", 42L, 3.14);
        });
    }

    @Test
    public void testCheckState_LongDouble_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 42L, 3.14);
        });
    }

    @Test
    public void testCheckState_LongObject_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", 42L, "test");
        });
    }

    @Test
    public void testCheckState_LongObject_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 42L, "test");
        });
    }

    @Test
    public void testCheckState_DoubleChar_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", 3.14, 'a');
        });
    }

    @Test
    public void testCheckState_DoubleChar_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 3.14, 'a');
        });
    }

    @Test
    public void testCheckState_DoubleInt_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", 3.14, 42);
        });
    }

    @Test
    public void testCheckState_DoubleInt_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 3.14, 42);
        });
    }

    @Test
    public void testCheckState_DoubleLong_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", 3.14, 42L);
        });
    }

    @Test
    public void testCheckState_DoubleLong_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 3.14, 42L);
        });
    }

    @Test
    public void testCheckState_DoubleDouble_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", 3.14, 2.71);
        });
    }

    @Test
    public void testCheckState_DoubleDouble_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 3.14, 2.71);
        });
    }

    @Test
    public void testCheckState_DoubleObject_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", 3.14, "test");
        });
    }

    @Test
    public void testCheckState_DoubleObject_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 3.14, "test");
        });
    }

    @Test
    public void testCheckState_ObjectChar_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", "test", 'a');
        });
    }

    @Test
    public void testCheckState_ObjectChar_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", "test", 'a');
        });
    }

    @Test
    public void testCheckState_ObjectInt_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", "test", 42);
        });
    }

    @Test
    public void testCheckState_ObjectInt_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", "test", 42);
        });
    }

    @Test
    public void testCheckState_ObjectLong_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", "test", 42L);
        });
    }

    @Test
    public void testCheckState_ObjectLong_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", "test", 42L);
        });
    }

    @Test
    public void testCheckState_ObjectDouble_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", "test", 3.14);
        });
    }

    @Test
    public void testCheckState_ObjectDouble_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", "test", 3.14);
        });
    }

    @Test
    public void testCheckState_ObjectObject_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s", "test1", "test2");
        });
    }

    @Test
    public void testCheckState_ObjectObject_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", "test1", "test2");
        });
    }

    @Test
    public void testCheckState_ThreeObjects_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s %s", "test1", "test2", "test3");
        });
    }

    @Test
    public void testCheckState_ThreeObjects_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s %s", "test1", "test2", "test3");
        });
    }

    @Test
    public void testCheckState_FourObjects_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, "State error: %s %s %s %s", "test1", "test2", "test3", "test4");
        });
    }

    @Test
    public void testCheckState_FourObjects_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s %s %s", "test1", "test2", "test3", "test4");
        });
    }

    @Test
    public void testCheckState_Supplier_Pass() {
        assertDoesNotThrow(() -> {
            N.checkState(true, () -> "This message should not be created");
        });
    }

    @Test
    public void testCheckState_Supplier_Fail() {
        Supplier<String> messageSupplier = () -> "State error from supplier";
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, messageSupplier);
        });
    }

    @Test
    public void testCheckState_Supplier_MessageCreatedOnlyWhenNeeded() {
        final boolean[] supplierCalled = { false };
        N.checkState(true, () -> {
            supplierCalled[0] = true;
            return "Should not be called";
        });
        Assertions.assertFalse(supplierCalled[0]);

        try {
            N.checkState(false, () -> {
                supplierCalled[0] = true;
                return "Should be called";
            });
        } catch (IllegalStateException e) {
            Assertions.assertTrue(supplierCalled[0]);
        }
    }

    @Test
    public void testCheckState_BooleanOnly() {
        N.checkState(true);

        Assertions.assertThrows(IllegalStateException.class, () -> N.checkState(false));
    }

    @Test
    public void testCheckState_WithObjectMessage() {
        N.checkState(true, "message");

        Exception ex = Assertions.assertThrows(IllegalStateException.class, () -> N.checkState(false, "state error"));
        Assertions.assertEquals("state error", ex.getMessage());
    }

    @Test
    public void testCheckState_WithTemplateAndVarargs() {
        N.checkState(true, "state is %s", "valid");

        Exception ex = Assertions.assertThrows(IllegalStateException.class, () -> N.checkState(false, "state %s is invalid", "xyz"));
        Assertions.assertTrue(ex.getMessage().contains("xyz"));
    }

    @Test
    public void testCheckState_WithTemplateAndPrimitives() {
        N.checkState(true, "value is %s", 'a');
        N.checkState(true, "value is %s", 10);
        N.checkState(true, "value is %s", 10L);
        N.checkState(true, "value is %s", 3.14);
        N.checkState(true, "value is %s", "test");

        Exception ex = Assertions.assertThrows(IllegalStateException.class, () -> N.checkState(false, "char %s", 'x'));
        Assertions.assertTrue(ex.getMessage().contains("x"));
    }

    @Test
    public void testCheckState_TwoParams() {
        N.checkState(true, "values %s and %s", 10, 20);
        N.checkState(true, "values %s and %s", "a", "b");

        Exception ex = Assertions.assertThrows(IllegalStateException.class, () -> N.checkState(false, "invalid %s and %s", 1, 2));
        Assertions.assertTrue(ex.getMessage().contains("1"));
        Assertions.assertTrue(ex.getMessage().contains("2"));
    }

    @Test
    public void testCheckState_ThreeAndFourParams() {
        N.checkState(true, "values %s, %s, %s", 1, 2, 3);
        N.checkState(true, "values %s, %s, %s, %s", 1, 2, 3, 4);

        Exception ex = Assertions.assertThrows(IllegalStateException.class, () -> N.checkState(false, "bad %s, %s, %s", "x", "y", "z"));
        Assertions.assertTrue(ex.getMessage().contains("x"));
        Assertions.assertTrue(ex.getMessage().contains("y"));
        Assertions.assertTrue(ex.getMessage().contains("z"));
    }

    @Test
    public void testCheckState() {
        N.checkState(true);
        Assertions.assertThrows(IllegalStateException.class, () -> N.checkState(false));

        N.checkState(true, "Error");
        Assertions.assertThrows(IllegalStateException.class, () -> N.checkState(false, "Error"));

        N.checkState(true, "Error: %s", "test");
        Assertions.assertThrows(IllegalStateException.class, () -> N.checkState(false, "Error: %s", "test"));

        N.checkState(true, "Error: %s", 'c');
        N.checkState(true, "Error: %s", 10);
        N.checkState(true, "Error: %s", 10L);
        N.checkState(true, "Error: %s", 10.5);
        N.checkState(true, "Error: %s", new Object());

        N.checkState(true, "Error: %s %s", 'a', 'b');
        N.checkState(true, "Error: %s %s", 'a', 10);
        N.checkState(true, "Error: %s %s", 10, 20);
        N.checkState(true, "Error: %s %s", 10L, 20L);
        N.checkState(true, "Error: %s %s", new Object(), new Object());

        N.checkState(true, "Error: %s %s %s", "a", "b", "c");
        N.checkState(true, "Error: %s %s %s %s", "a", "b", "c", "d");

        N.checkState(true, () -> "Error from supplier");
        Assertions.assertThrows(IllegalStateException.class, () -> N.checkState(false, () -> "Error from supplier"));
    }

    @Test
    public void testCheckState_Varargs_WhenTrue_DoesNotThrow() {
        // L3220: closing brace when expression is true
        assertDoesNotThrow(() -> N.checkState(true, "State {} is wrong", "ACTIVE"));
    }

    @Test
    public void testCheckState_Varargs_WhenFalse_ThrowsWithFormattedMessage() {
        // L3218: throw when expression is false
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> N.checkState(false, "State {} is invalid", "CLOSED"));
        assertTrue(ex.getMessage().contains("CLOSED"));
    }

    @Test
    public void requireNonNull_object_valid() {
        String obj = "test";
        assertSame(obj, N.requireNonNull(obj));
        Integer num = 1;
        assertSame(num, N.requireNonNull(num));
    }

    @Test
    public void requireNonNull_objectWithMessage_valid() {
        String obj = "test";
        assertSame(obj, N.requireNonNull(obj, "testObject"));
    }

    @Test
    public void requireNonNull_objectWithSupplier_valid() {
        String obj = "test";
        assertSame(obj, N.requireNonNull(obj, () -> "This should not be called"));
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
    public void requireNonNull_object_invalid() {
        assertThrows(NullPointerException.class, () -> N.requireNonNull(null));
    }

    @Test
    public void requireNonNull_objectWithMessage_invalid() {
        NullPointerException ex1 = assertThrows(NullPointerException.class, () -> N.requireNonNull(null, "testObject"));
        assertEquals("'testObject' cannot be null", ex1.getMessage());

        NullPointerException ex2 = assertThrows(NullPointerException.class, () -> N.requireNonNull(null, "Custom error message for null object"));
        assertEquals("Custom error message for null object", ex2.getMessage());
    }

    @Test
    public void requireNonNull_objectWithSupplier_invalid() {
        final boolean[] supplierCalled = { false };
        Supplier<String> supplier = () -> {
            supplierCalled[0] = true;
            return "paramName";
        };

        NullPointerException ex1 = assertThrows(NullPointerException.class, () -> N.requireNonNull(null, supplier));
        assertTrue(supplierCalled[0]);
        assertEquals("'paramName' cannot be null", ex1.getMessage());

        supplierCalled[0] = false;
        Supplier<String> supplierCustomMsg = () -> {
            supplierCalled[0] = true;
            return "Custom detailed error message from supplier";
        };
        NullPointerException ex2 = assertThrows(NullPointerException.class, () -> N.requireNonNull(null, supplierCustomMsg));
        assertTrue(supplierCalled[0]);
        assertEquals("Custom detailed error message from supplier", ex2.getMessage());

        final boolean[] supplierNotCalled = { false };
        Supplier<String> trackingSupplier = () -> {
            supplierNotCalled[0] = true;
            return "Should not be called";
        };
        String validObj = "I am valid";
        assertSame(validObj, N.requireNonNull(validObj, trackingSupplier));
        assertFalse(supplierNotCalled[0], "Supplier should not be called for non-null object");

    }

    @Test
    public void testRequireNonNull() {
        String str = "test";
        Assertions.assertEquals(str, N.requireNonNull(str));
        Assertions.assertThrows(NullPointerException.class, () -> N.requireNonNull(null));

        Assertions.assertEquals(str, N.requireNonNull(str, "myArg"));
        Assertions.assertThrows(NullPointerException.class, () -> N.requireNonNull(null, "myArg"));
        Assertions.assertThrows(NullPointerException.class, () -> N.requireNonNull(null, "this is a longer error message"));

        Assertions.assertEquals(str, N.requireNonNull(str, () -> "myArg"));
        Assertions.assertThrows(NullPointerException.class, () -> N.requireNonNull(null, () -> "myArg"));
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
    public void testEquals_BooleanArray_WithRange() {
        boolean[] a = { true, false, true, false };
        boolean[] b = { false, false, true, true };

        assertTrue(N.equals(a, 1, b, 1, 2));
        assertFalse(N.equals(a, 0, b, 0, 2));
        assertTrue(N.equals(a, 0, a, 0, 0));
    }

    @Test
    public void testEquals_CharArray_WithRange() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] b = { 'x', 'b', 'c', 'y' };

        assertTrue(N.equals(a, 1, b, 1, 2));
        assertFalse(N.equals(a, 0, b, 0, 2));
    }

    @Test
    public void testEquals_ByteArray_WithRange() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] b = { 0, 2, 3, 0 };

        assertTrue(N.equals(a, 1, b, 1, 2));
        assertFalse(N.equals(a, 0, b, 0, 2));
    }

    @Test
    public void testEquals_ShortArray_WithRange() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 0, 2, 3, 0 };

        assertTrue(N.equals(a, 1, b, 1, 2));
        assertFalse(N.equals(a, 0, b, 0, 2));
    }

    @Test
    public void testEquals_IntArray_WithRange() {
        int[] a = { 1, 2, 3, 4 };
        int[] b = { 0, 2, 3, 0 };

        assertTrue(N.equals(a, 1, b, 1, 2));
        assertFalse(N.equals(a, 0, b, 0, 2));
    }

    @Test
    public void testEquals_LongArray_WithRange() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 0L, 2L, 3L, 0L };

        assertTrue(N.equals(a, 1, b, 1, 2));
        assertFalse(N.equals(a, 0, b, 0, 2));
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
    public void testEquals_ObjectArray_WithRange() {
        String[] a = { "a", "b", "c", "d" };
        String[] b = { "x", "b", "c", "y" };

        assertTrue(N.equals(a, 1, b, 1, 2));
        assertFalse(N.equals(a, 0, b, 0, 2));
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
    public void testEquals_boolean() {
        assertTrue(N.equals(true, true));
        assertTrue(N.equals(false, false));
        assertFalse(N.equals(true, false));
    }

    @Test
    public void testEquals_short() {
        assertTrue(N.equals((short) 10, (short) 10));
        assertFalse(N.equals((short) 10, (short) 20));
    }

    @Test
    public void testEquals_int() {
        assertTrue(N.equals(100, 100));
        assertFalse(N.equals(100, 200));
    }

    @Test
    public void testEquals_long() {
        assertTrue(N.equals(1000L, 1000L));
        assertFalse(N.equals(1000L, 2000L));
    }

    @Test
    public void testEquals_float() {
        assertTrue(N.equals(1.0f, 1.0f));
        assertFalse(N.equals(1.0f, 1.1f));
        assertTrue(N.equals(Float.NaN, Float.NaN));
    }

    @Test
    public void testEquals_double() {
        assertTrue(N.equals(1.0, 1.0));
        assertFalse(N.equals(1.0, 1.1));
        assertTrue(N.equals(Double.NaN, Double.NaN));
    }

    @Test
    public void testEquals_intArray() {
        assertTrue(N.equals(new int[] { 1, 2 }, new int[] { 1, 2 }));
        assertFalse(N.equals(new int[] { 1 }, new int[] { 2 }));
    }

    @Test
    public void testEquals_floatArrayRange() {
        float[] a1 = { 1.0f, 2.0f, Float.NaN, 4.0f };
        float[] a2 = { 0.0f, 2.0f, Float.NaN, 5.0f };
        float[] a3 = { 1.0f, 2.0f, Float.NaN, 4.0f };

        assertTrue(N.equals(a1, 1, a3, 1, 2));
        assertFalse(N.equals(a1, 0, a2, 0, 2));
    }

    @Test
    public void testEquals_doubleArrayRange() {
        double[] a1 = { 1.0, 2.0, Double.NaN, 4.0 };
        double[] a2 = { 0.0, 2.0, Double.NaN, 5.0 };
        double[] a3 = { 1.0, 2.0, Double.NaN, 4.0 };

        assertTrue(N.equals(a1, 1, a3, 1, 2));
        assertFalse(N.equals(a1, 0, a2, 0, 2));
    }

    @Test
    public void testEqualsFloatArraysWithRange() {
        float[] arr1 = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] arr2 = { 10.0f, 2.0f, 3.0f, 40.0f };

        Assertions.assertTrue(N.equals(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(N.equals(arr1, 0, arr2, 0, 2));

        float[] nanArr1 = { Float.NaN, 2.0f };
        float[] nanArr2 = { Float.NaN, 2.0f };
        Assertions.assertTrue(N.equals(nanArr1, 0, nanArr2, 0, 2));
    }

    @Test
    public void testEqualsDoubleArraysWithRange() {
        double[] arr1 = { 1.0, 2.0, 3.0, 4.0 };
        double[] arr2 = { 10.0, 2.0, 3.0, 40.0 };

        Assertions.assertTrue(N.equals(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(N.equals(arr1, 0, arr2, 0, 2));

        double[] nanArr1 = { Double.NaN, 2.0 };
        double[] nanArr2 = { Double.NaN, 2.0 };
        Assertions.assertTrue(N.equals(nanArr1, 0, nanArr2, 0, 2));
    }

    @Test
    public void testEqualsObjectArraysWithRange() {
        String[] arr1 = { "a", "b", "c", "d" };
        String[] arr2 = { "x", "b", "c", "y" };

        Assertions.assertTrue(N.equals(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(N.equals(arr1, 0, arr2, 0, 2));

        Integer[] intArr = { 1, 2, 3 };
        Long[] longArr = { 1L, 2L, 3L };
        Assertions.assertFalse(N.equals(intArr, 0, longArr, 0, 2));
    }

    @Test
    public void testEqualsPrimitives() {
        Assertions.assertTrue(N.equals(true, true));
        Assertions.assertTrue(N.equals(false, false));
        Assertions.assertFalse(N.equals(true, false));

        Assertions.assertTrue(N.equals('a', 'a'));
        Assertions.assertFalse(N.equals('a', 'b'));

        Assertions.assertTrue(N.equals((byte) 10, (byte) 10));
        Assertions.assertFalse(N.equals((byte) 10, (byte) 20));

        Assertions.assertTrue(N.equals((short) 10, (short) 10));
        Assertions.assertFalse(N.equals((short) 10, (short) 20));

        Assertions.assertTrue(N.equals(10, 10));
        Assertions.assertFalse(N.equals(10, 20));

        Assertions.assertTrue(N.equals(10L, 10L));
        Assertions.assertFalse(N.equals(10L, 20L));

        Assertions.assertTrue(N.equals(10.5f, 10.5f));
        Assertions.assertFalse(N.equals(10.5f, 20.5f));
        Assertions.assertTrue(N.equals(Float.NaN, Float.NaN));

        Assertions.assertTrue(N.equals(10.5, 10.5));
        Assertions.assertFalse(N.equals(10.5, 20.5));
        Assertions.assertTrue(N.equals(Double.NaN, Double.NaN));
    }

    // Tests for equals(Object, Object) with array types
    @Test
    public void testEquals_Object_PrimitiveArrays() {
        int[] a = { 1, 2, 3 };
        int[] b = { 1, 2, 3 };
        int[] c = { 1, 2, 4 };
        assertTrue(N.equals((Object) a, (Object) b));
        assertFalse(N.equals((Object) a, (Object) c));
    }

    @Test
    public void testEquals_Object_ObjectArrays() {
        String[] a = { "x", "y" };
        String[] b = { "x", "y" };
        String[] c = { "x", "z" };
        assertTrue(N.equals((Object) a, (Object) b));
        assertFalse(N.equals((Object) a, (Object) c));
    }

    @Test
    public void testEquals_Object_DifferentTypes() {
        int[] intArr = { 1, 2 };
        long[] longArr = { 1L, 2L };
        assertFalse(N.equals(intArr, longArr));
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
    public void testEquals_CharArray() {
        assertTrue(N.equals(new char[] { 'a', 'b' }, new char[] { 'a', 'b' }));
        assertFalse(N.equals(new char[] { 'a', 'b' }, new char[] { 'b', 'a' }));
        assertTrue(N.equals((char[]) null, (char[]) null));
    }

    @Test
    public void testEquals_ByteArray() {
        assertTrue(N.equals(new byte[] { 1, 2 }, new byte[] { 1, 2 }));
        assertFalse(N.equals(new byte[] { 1, 2 }, new byte[] { 2, 1 }));
        assertTrue(N.equals((byte[]) null, (byte[]) null));
    }

    @Test
    public void testEquals_ShortArray() {
        assertTrue(N.equals(new short[] { 1, 2 }, new short[] { 1, 2 }));
        assertFalse(N.equals(new short[] { 1, 2 }, new short[] { 2, 1 }));
        assertTrue(N.equals((short[]) null, (short[]) null));
    }

    @Test
    public void testEquals_IntArray() {
        assertTrue(N.equals(new int[] { 1, 2 }, new int[] { 1, 2 }));
        assertFalse(N.equals(new int[] { 1, 2 }, new int[] { 2, 1 }));
        assertTrue(N.equals((int[]) null, (int[]) null));
    }

    @Test
    public void testEquals_LongArray() {
        assertTrue(N.equals(new long[] { 1L, 2L }, new long[] { 1L, 2L }));
        assertFalse(N.equals(new long[] { 1L, 2L }, new long[] { 2L, 1L }));
        assertTrue(N.equals((long[]) null, (long[]) null));
    }

    @Test
    public void testEquals_FloatArray() {
        assertTrue(N.equals(new float[] { 1.0f, 2.0f }, new float[] { 1.0f, 2.0f }));
        assertFalse(N.equals(new float[] { 1.0f, 2.0f }, new float[] { 2.0f, 1.0f }));
        assertTrue(N.equals((float[]) null, (float[]) null));
    }

    @Test
    public void testEquals_DoubleArray() {
        assertTrue(N.equals(new double[] { 1.0, 2.0 }, new double[] { 1.0, 2.0 }));
        assertFalse(N.equals(new double[] { 1.0, 2.0 }, new double[] { 2.0, 1.0 }));
        assertTrue(N.equals((double[]) null, (double[]) null));
    }

    @Test
    public void testEquals_ObjectArray() {
        assertTrue(N.equals(new String[] { "a", "b" }, new String[] { "a", "b" }));
        assertFalse(N.equals(new String[] { "a", "b" }, new String[] { "b", "a" }));
        assertTrue(N.equals((Object[]) null, (Object[]) null));
    }

    @Test
    public void testEquals_booleanArray() {
        assertTrue(N.equals(new boolean[] { true, false }, new boolean[] { true, false }));
        assertFalse(N.equals(new boolean[] { true }, new boolean[] { false }));
        assertTrue(N.equals((boolean[]) null, (boolean[]) null));
        assertFalse(N.equals(new boolean[0], (boolean[]) null));
        assertFalse(N.equals(new boolean[] { true }, new boolean[] { true, false }));
    }

    @Test
    public void testEqualsFloatArrays() {
        Assertions.assertTrue(N.equals(new float[] { 1.0f, 2.0f }, new float[] { 1.0f, 2.0f }));
        Assertions.assertFalse(N.equals(new float[] { 1.0f, 2.0f }, new float[] { 1.0f, 3.0f }));
        Assertions.assertTrue(N.equals((float[]) null, null));
        Assertions.assertFalse(N.equals(new float[] { 1.0f }, null));

        Assertions.assertTrue(N.equals(new float[] { Float.NaN }, new float[] { Float.NaN }));
    }

    @Test
    public void testEqualsFloatArraysWithDelta() {
        float[] arr1 = { 1.0f, 2.0f, 3.0f };
        float[] arr2 = { 1.01f, 2.01f, 3.01f };
        float[] arr3 = { 1.1f, 2.1f, 3.1f };

        Assertions.assertTrue(N.equals(arr1, arr2, 0.02f));
        Assertions.assertFalse(N.equals(arr1, arr3, 0.02f));

        Assertions.assertTrue(N.equals((float[]) null, null, 0.01f));
        Assertions.assertTrue(N.equals(new float[0], new float[0], 0.01f));
        Assertions.assertFalse(N.equals(arr1, null, 0.01f));
        Assertions.assertFalse(N.equals(arr1, new float[] { 1.0f, 2.0f }, 0.01f));
    }

    @Test
    public void testEqualsDoubleArrays() {
        Assertions.assertTrue(N.equals(new double[] { 1.0, 2.0 }, new double[] { 1.0, 2.0 }));
        Assertions.assertFalse(N.equals(new double[] { 1.0, 2.0 }, new double[] { 1.0, 3.0 }));
        Assertions.assertTrue(N.equals((double[]) null, null));
        Assertions.assertFalse(N.equals(new double[] { 1.0 }, null));

        Assertions.assertTrue(N.equals(new double[] { Double.NaN }, new double[] { Double.NaN }));
    }

    @Test
    public void testEqualsDoubleArraysWithDelta() {
        double[] arr1 = { 1.0, 2.0, 3.0 };
        double[] arr2 = { 1.01, 2.01, 3.01 };
        double[] arr3 = { 1.1, 2.1, 3.1 };

        Assertions.assertTrue(N.equals(arr1, arr2, 0.02));
        Assertions.assertFalse(N.equals(arr1, arr3, 0.02));

        Assertions.assertTrue(N.equals((double[]) null, null, 0.01));
        Assertions.assertTrue(N.equals(new double[0], new double[0], 0.01));
        Assertions.assertFalse(N.equals(arr1, null, 0.01));
        Assertions.assertFalse(N.equals(arr1, new double[] { 1.0, 2.0 }, 0.01));
    }

    @Test
    public void testEqualsObjectArrays() {
        String[] arr1 = { "a", "b", "c" };
        String[] arr2 = { "a", "b", "c" };
        String[] arr3 = { "a", "b", "d" };

        Assertions.assertTrue(N.equals(arr1, arr2));
        Assertions.assertFalse(N.equals(arr1, arr3));
        Assertions.assertTrue(N.equals(arr1, arr1));
        Assertions.assertTrue(N.equals((Object[]) null, null));
        Assertions.assertFalse(N.equals(arr1, null));

        String[] nullArr1 = { "a", null, "c" };
        String[] nullArr2 = { "a", null, "c" };
        String[] nullArr3 = { "a", "b", null };
        Assertions.assertTrue(N.equals(nullArr1, nullArr2));
        Assertions.assertFalse(N.equals(nullArr1, nullArr3));
    }

    @Test
    public void testEqualsStrings() {
        Assertions.assertTrue(N.equals("test", "test"));
        Assertions.assertFalse(N.equals("test", "Test"));
        Assertions.assertTrue(N.equals((String) null, null));
        Assertions.assertFalse(N.equals("test", null));
        Assertions.assertFalse(N.equals(null, "test"));

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

        int[] arr1 = { 1, 2, 3 };
        int[] arr2 = { 1, 2, 3 };
        int[] arr3 = { 1, 2, 4 };

        Assertions.assertTrue(N.equals(arr1, arr1));
        Assertions.assertTrue(N.equals(arr1, arr2));
        Assertions.assertFalse(N.equals(arr1, arr3));

        Assertions.assertFalse(N.equals(new int[] { 1 }, new long[] { 1 }));
    }

    @Test
    public void testEqualsArrays() {
        Assertions.assertTrue(N.equals(new boolean[] { true, false }, new boolean[] { true, false }));
        Assertions.assertFalse(N.equals(new boolean[] { true, false }, new boolean[] { false, true }));
        Assertions.assertTrue(N.equals((boolean[]) null, null));
        Assertions.assertFalse(N.equals(new boolean[] { true }, null));

        boolean[] boolArr1 = { true, false, true, false };
        boolean[] boolArr2 = { false, true, false, true };
        Assertions.assertTrue(N.equals(boolArr1, 1, boolArr2, 2, 2));
        Assertions.assertFalse(N.equals(boolArr1, 0, boolArr2, 0, 2));

        Assertions.assertTrue(N.equals(new char[] { 'a', 'b' }, new char[] { 'a', 'b' }));
        Assertions.assertFalse(N.equals(new char[] { 'a', 'b' }, new char[] { 'b', 'a' }));

        char[] charArr1 = { 'a', 'b', 'c', 'd' };
        char[] charArr2 = { 'x', 'b', 'c', 'y' };
        Assertions.assertTrue(N.equals(charArr1, 1, charArr2, 1, 2));

        Assertions.assertTrue(N.equals(new byte[] { 1, 2 }, new byte[] { 1, 2 }));
        Assertions.assertFalse(N.equals(new byte[] { 1, 2 }, new byte[] { 2, 1 }));

        byte[] byteArr1 = { 1, 2, 3, 4 };
        byte[] byteArr2 = { 5, 2, 3, 6 };
        Assertions.assertTrue(N.equals(byteArr1, 1, byteArr2, 1, 2));

        Assertions.assertTrue(N.equals(new short[] { 1, 2 }, new short[] { 1, 2 }));
        Assertions.assertFalse(N.equals(new short[] { 1, 2 }, new short[] { 2, 1 }));

        short[] shortArr1 = { 1, 2, 3, 4 };
        short[] shortArr2 = { 5, 2, 3, 6 };
        Assertions.assertTrue(N.equals(shortArr1, 1, shortArr2, 1, 2));

        Assertions.assertTrue(N.equals(new int[] { 1, 2 }, new int[] { 1, 2 }));
        Assertions.assertFalse(N.equals(new int[] { 1, 2 }, new int[] { 2, 1 }));

        int[] intArr1 = { 1, 2, 3, 4 };
        int[] intArr2 = { 5, 2, 3, 6 };
        Assertions.assertTrue(N.equals(intArr1, 1, intArr2, 1, 2));

        Assertions.assertTrue(N.equals(new long[] { 1L, 2L }, new long[] { 1L, 2L }));
        Assertions.assertFalse(N.equals(new long[] { 1L, 2L }, new long[] { 2L, 1L }));

        long[] longArr1 = { 1L, 2L, 3L, 4L };
        long[] longArr2 = { 5L, 2L, 3L, 6L };
        Assertions.assertTrue(N.equals(longArr1, 1, longArr2, 1, 2));
    }

    @Test
    public void testEquals_Object_NullAndNonNull() {
        assertFalse(N.equals((Object) null, "a"));
        assertFalse(N.equals("a", (Object) null));
        assertTrue(N.equals((Object) null, (Object) null));
    }

    // ========== equals with range len==0 shortcircuit ==========

    @Test
    public void testEquals_ByteArray_LenZero_ReturnsTrue() {
        // L4213: len == 0 -> return true immediately
        byte[] a = { 1, 2, 3 };
        byte[] b = { 9, 8, 7 };
        assertTrue(N.equals(a, 0, b, 0, 0));
    }

    @Test
    public void testEquals_ByteArray_SameArraySameOffset_ReturnsTrue() {
        // L4213: fromIndexA == fromIndexB && a == b -> return true
        byte[] a = { 1, 2, 3 };
        assertTrue(N.equals(a, 1, a, 1, 2));
    }

    @Test
    public void testEquals_FloatArray_LenZero_ReturnsTrue() {
        // L4421: len == 0 -> return true
        float[] a = { 1.0f, 2.0f };
        float[] b = { 9.0f, 8.0f };
        assertTrue(N.equals(a, 0, b, 0, 0));
    }

    @Test
    public void testEquals_booleanArrayRange() {
        boolean[] a1 = { true, false, true, true };
        boolean[] a2 = { false, true, true, false };
        boolean[] a3 = { true, false, true, true };

        assertTrue(N.equals(a1, 1, a3, 1, 2));
        assertFalse(N.equals(a1, 0, a2, 0, 2));
        assertTrue(N.equals(a1, 0, a1, 0, 0));

        assertThrows(IllegalArgumentException.class, () -> N.equals(a1, 0, a3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.equals(a1, 0, a3, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> N.equals(a1, 3, a3, 0, 2));
    }

    @Test
    public void testEquals_intArrayRange() {
        int[] a1 = { 1, 2, 3, 4 };
        int[] a2 = { 0, 2, 3, 5 };
        int[] a3 = { 1, 2, 3, 4 };

        assertTrue(N.equals(a1, 1, a3, 1, 2));
        assertFalse(N.equals(a1, 0, a2, 0, 2));
        assertTrue(N.equals(a1, 0, a1, 0, 0));

        assertThrows(IllegalArgumentException.class, () -> N.equals(a1, 0, a3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.equals(a1, 0, a3, 0, 5));
    }

    @Test
    public void testEquals_ObjectArrayRange() {
        Object[] a1 = { "hello", "world", "!" };
        Object[] a2 = { "start", "world", "!" };
        Object[] a3 = { "hello", "world", "!" };

        assertTrue(N.equals(a1, 1, a3, 1, 2));
        assertFalse(N.equals(a1, 0, a2, 0, 2));
        assertTrue(N.equals(a1, 0, a1, 0, 0));

        assertThrows(IllegalArgumentException.class, () -> N.equals(a1, 0, a3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.equals(a1, 0, a3, 0, 4));
    }

    @Test
    public void testEqualsLongArraysWithRange() {
        long[] arr1 = { 1L, 2L, 3L, 4L, 5L };
        long[] arr2 = { 10L, 2L, 3L, 40L, 50L };

        Assertions.assertTrue(N.equals(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(N.equals(arr1, 0, arr2, 0, 2));

        Assertions.assertTrue(N.equals(arr1, 1, arr1, 1, 2));

        Assertions.assertTrue(N.equals(arr1, 0, arr2, 0, 0));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.equals(arr1, 0, arr2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.equals(arr1, 0, arr2, 0, 10));
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
    public void testEqualsIgnoreCase() {
        String[] arr1 = { "Hello", "World", "TEST" };
        String[] arr2 = { "hello", "WORLD", "test" };
        String[] arr3 = { "hello", "WORLD", "testing" };

        Assertions.assertTrue(N.equalsIgnoreCase(arr1, arr2));
        Assertions.assertFalse(N.equalsIgnoreCase(arr1, arr3));
        Assertions.assertTrue(N.equalsIgnoreCase((String[]) null, null));
        Assertions.assertFalse(N.equalsIgnoreCase(arr1, null));
        Assertions.assertFalse(N.equalsIgnoreCase(null, arr2));

        String[] nullArr1 = { "Hello", null, "TEST" };
        String[] nullArr2 = { "hello", null, "test" };
        String[] nullArr3 = { "hello", "world", "test" };
        Assertions.assertTrue(N.equalsIgnoreCase(nullArr1, nullArr2));
        Assertions.assertFalse(N.equalsIgnoreCase(nullArr1, nullArr3));
    }

    @Test
    public void testEqualsIgnoreCaseWithRange() {
        String[] arr1 = { "a", "Hello", "World", "d" };
        String[] arr2 = { "x", "HELLO", "world", "y" };

        Assertions.assertTrue(N.equalsIgnoreCase(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(N.equalsIgnoreCase(arr1, 0, arr2, 0, 2));
    }

    // Tests for equalsIgnoreCase(String[], fromIndexA, String[], fromIndexB, len) - same array ref
    @Test
    public void testEqualsIgnoreCase_StringArray_SameRef() {
        String[] arr = { "Hello", "World" };
        // Same array ref should return true
        assertTrue(N.equalsIgnoreCase(arr, 0, arr, 0, 2));
    }

    @Test
    public void testEqualsIgnoreCase_StringArray_ZeroLen() {
        String[] a = { "Hello" };
        String[] b = { "World" };
        assertTrue(N.equalsIgnoreCase(a, 0, b, 0, 0));
    }

    @Test
    public void testEqualsIgnoreCase_StringArrayRange() {
        String[] a1 = { "Hello", "WORLD", "Java", "Test" };
        String[] a2 = { "start", "world", "java", "end" };
        String[] a3 = { "HELLO", "world", "JAVA", "TEST" };

        assertTrue(N.equalsIgnoreCase(a1, 1, a3, 1, 2));
        assertFalse(N.equalsIgnoreCase(a1, 0, a2, 0, 2));
        assertTrue(N.equalsIgnoreCase(a1, 0, a1, 0, 0));

        String[] an1 = { "Test", null, "Me" };
        String[] an2 = { "test", null, "me" };
        String[] an3 = { "test", "NotNULL", "me" };
        assertTrue(N.equalsIgnoreCase(an1, 0, an2, 0, 3));
        assertFalse(N.equalsIgnoreCase(an1, 0, an3, 0, 3));

        assertThrows(IllegalArgumentException.class, () -> N.equalsIgnoreCase(a1, 0, a3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.equalsIgnoreCase(a1, 0, a3, 0, 5));
    }

    @Test
    public void testDeepEquals_Arrays() {
        assertTrue(N.deepEquals(new int[] { 1, 2 }, new int[] { 1, 2 }));
        assertTrue(N.deepEquals(new String[] { "a", "b" }, new String[] { "a", "b" }));
        assertFalse(N.deepEquals(new int[] { 1, 2 }, new int[] { 2, 1 }));
    }

    @Test
    public void testDeepEquals_ObjectArray_WithRange() {
        String[] a = { "a", "b", "c", "d" };
        String[] b = { "x", "b", "c", "y" };

        assertTrue(N.deepEquals(a, 1, b, 1, 2));
        assertFalse(N.deepEquals(a, 0, b, 0, 2));
    }

    @Test
    public void testDeepEqualsArraysWithRange() {
        Object[] arr1 = { "a", new int[] { 1, 2 }, "c", new int[] { 3, 4 } };
        Object[] arr2 = { "x", new int[] { 1, 2 }, "c", new int[] { 5, 6 } };

        Assertions.assertTrue(N.deepEquals(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(N.deepEquals(arr1, 2, arr2, 2, 2));

        Assertions.assertFalse(N.deepEquals(new Integer[] { 1 }, 0, new Long[] { 1L }, 0, 1));
    }

    @Test
    public void testDeepEquals_Object() {
        assertTrue(N.deepEquals("test", "test"));
        assertTrue(N.deepEquals(null, null));
        assertFalse(N.deepEquals("test", "other"));
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
    public void testDeepEquals_ObjectArray() {
        Object[] a = { 1, new String[] { "a", "b" }, new int[] { 10, 20 } };
        Object[] b = { 1, new String[] { "a", "b" }, new int[] { 10, 20 } };
        Object[] c = { 1, new String[] { "a", "DIFFERENT" }, new int[] { 10, 20 } };
        assertTrue(N.deepEquals(a, b));
        assertFalse(N.deepEquals(a, c));
        assertTrue(N.deepEquals((Object[]) null, (Object[]) null));
    }

    @Test
    public void testDeepEquals() {
        Assertions.assertTrue(N.deepEquals(5, 5));
        Assertions.assertFalse(N.deepEquals(5, 6));
        Assertions.assertTrue(N.deepEquals("test", "test"));
        Assertions.assertFalse(N.deepEquals("test", "Test"));

        int[] intArr1 = { 1, 2, 3 };
        int[] intArr2 = { 1, 2, 3 };
        int[] intArr3 = { 1, 2, 4 };
        Assertions.assertTrue(N.deepEquals(intArr1, intArr2));
        Assertions.assertFalse(N.deepEquals(intArr1, intArr3));

        Object[] nested1 = { 1, new int[] { 2, 3 }, "test" };
        Object[] nested2 = { 1, new int[] { 2, 3 }, "test" };
        Object[] nested3 = { 1, new int[] { 2, 4 }, "test" };
        Assertions.assertTrue(N.deepEquals(nested1, nested2));
        Assertions.assertFalse(N.deepEquals(nested1, nested3));

        Assertions.assertTrue(N.deepEquals(null, null));
        Assertions.assertFalse(N.deepEquals(null, "test"));
        Assertions.assertFalse(N.deepEquals("test", null));
    }

    @Test
    public void testDeepEqualsArrays() {
        Object[] arr1 = { "a", 1, true };
        Object[] arr2 = { "a", 1, true };
        Object[] arr3 = { "a", 1, false };

        Assertions.assertTrue(N.deepEquals(arr1, arr2));
        Assertions.assertFalse(N.deepEquals(arr1, arr3));
        Assertions.assertTrue(N.deepEquals((Object[]) null, null));
        Assertions.assertFalse(N.deepEquals(arr1, null));

        Object[] nested1 = { new int[] { 1, 2 }, new String[] { "a", "b" } };
        Object[] nested2 = { new int[] { 1, 2 }, new String[] { "a", "b" } };
        Object[] nested3 = { new int[] { 1, 3 }, new String[] { "a", "b" } };

        Assertions.assertTrue(N.deepEquals(nested1, nested2));
        Assertions.assertFalse(N.deepEquals(nested1, nested3));
    }

    @Test
    public void testDeepEquals_ObjectArrayRange() {
        Object[] arr1 = { "first", new int[] { 1, 2 }, "mid", new String[] { "x", "y" }, "last" };
        Object[] arr2 = { "other", new int[] { 1, 2 }, "mid", new String[] { "x", "y" }, "end" };
        Object[] arr3 = { "first", new int[] { 1, 2 }, "mid", new String[] { "x", "y" }, "last" };

        assertTrue(N.deepEquals(arr1, 1, arr3, 1, 3));
        assertFalse(N.deepEquals(arr1, 0, arr2, 0, 2));
        assertTrue(N.deepEquals(arr1, 0, arr1, 0, 0));

        assertThrows(IllegalArgumentException.class, () -> N.deepEquals(arr1, 0, arr3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.deepEquals(arr1, 0, arr3, 0, 6));

        Object[] cyclic1 = new Object[1];
        cyclic1[0] = cyclic1;
        Object[] cyclic2 = new Object[1];
        cyclic2[0] = cyclic2;
        Object[] cyclic3 = new Object[1];
        cyclic3[0] = new Object[1];

    }

    // --- Tests for equalsInOrder ---

    @Test
    public void testEqualsInOrder_Collection() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<String> b = Arrays.asList("a", "b", "c");
        assertTrue(N.equalsInOrder(a, b));

        List<String> c = Arrays.asList("c", "b", "a");
        assertFalse(N.equalsInOrder(a, c));
    }

    @Test
    public void testEqualsInOrder_Collection_DifferentSizes() {
        assertFalse(N.equalsInOrder(Arrays.asList("a", "b"), Arrays.asList("a")));
    }

    @Test
    public void testEqualsInOrder_Map() {
        Map<String, Integer> a = new LinkedHashMap<>();
        a.put("x", 1);
        a.put("y", 2);
        Map<String, Integer> b = new LinkedHashMap<>();
        b.put("x", 1);
        b.put("y", 2);
        assertTrue(N.equalsInOrder(a, b));
    }

    // Tests for equalsInOrder(Map, Map)
    @Test
    public void testEqualsInOrder_LinkedHashMap() {
        java.util.LinkedHashMap<String, Integer> m1 = new java.util.LinkedHashMap<>();
        m1.put("a", 1);
        m1.put("b", 2);
        java.util.LinkedHashMap<String, Integer> m2 = new java.util.LinkedHashMap<>();
        m2.put("a", 1);
        m2.put("b", 2);
        assertTrue(N.equalsInOrder(m1, m2));
    }

    @Test
    public void testEqualsInOrder_DifferentOrder() {
        java.util.LinkedHashMap<String, Integer> m1 = new java.util.LinkedHashMap<>();
        m1.put("a", 1);
        m1.put("b", 2);
        java.util.LinkedHashMap<String, Integer> m2 = new java.util.LinkedHashMap<>();
        m2.put("b", 2);
        m2.put("a", 1);
        assertFalse(N.equalsInOrder(m1, m2));
    }

    @Test
    public void testEqualsInOrder_DifferentSize() {
        java.util.LinkedHashMap<String, Integer> m1 = new java.util.LinkedHashMap<>();
        m1.put("a", 1);
        java.util.LinkedHashMap<String, Integer> m2 = new java.util.LinkedHashMap<>();
        m2.put("a", 1);
        m2.put("b", 2);
        assertFalse(N.equalsInOrder(m1, m2));
    }

    @Test
    public void testEqualsInOrder_Collection_Null() {
        assertTrue(N.equalsInOrder((Collection<?>) null, (Collection<?>) null));
        assertFalse(N.equalsInOrder(Arrays.asList("a"), (Collection<?>) null));
        assertFalse(N.equalsInOrder((Collection<?>) null, Arrays.asList("a")));
    }

    @Test
    public void testEqualsInOrder_Map_Null() {
        assertTrue(N.equalsInOrder((Map<?, ?>) null, (Map<?, ?>) null));
        assertTrue(N.equalsInOrder(new HashMap<>(), (Map<?, ?>) null));
    }

    @Test
    public void testEqualsInOrder_BothNullOrEmpty() {
        assertTrue(N.equalsInOrder((java.util.Map<?, ?>) null, null));
        assertTrue(N.equalsInOrder(new HashMap<>(), null));
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

        assertTrue(N.equalsByKeys(map1, map3, keysWithC));
        assertTrue(N.equalsByKeys(map1, map3, keys));
        assertFalse(N.equalsByKeys(map1, map2, keysWithC));
        assertTrue(N.equalsByKeys(map1, map2, keys));
        assertFalse(N.equalsByKeys(map1, map4, keysWithC));

        assertTrue(N.equalsByKeys(null, null, keys));
        assertFalse(N.equalsByKeys(map1, null, keys));
        assertFalse(N.equalsByKeys(null, map1, keys));

        Map<String, Integer> mapWithNullValue1 = new HashMap<>();
        mapWithNullValue1.put("k1", null);
        Map<String, Integer> mapWithNullValue2 = new HashMap<>();
        mapWithNullValue2.put("k1", null);
        Map<String, Integer> mapWithNonNullValue = new HashMap<>();
        mapWithNonNullValue.put("k1", 1);

        assertTrue(N.equalsByKeys(mapWithNullValue1, mapWithNullValue2, Collections.singletonList("k1")));
        assertFalse(N.equalsByKeys(mapWithNullValue1, mapWithNonNullValue, Collections.singletonList("k1")));

        Map<String, Integer> emptyMap = Collections.emptyMap();
        assertFalse(N.equalsByKeys(mapWithNullValue1, emptyMap, Collections.singletonList("k1")));

        assertThrows(IllegalArgumentException.class, () -> N.equalsByKeys(map1, map2, Collections.emptyList()));
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

    // --- Tests for equalsByCommonProps ---

    @Test
    public void testEqualsByCommonProps() {
        TestBean b1 = new TestBean();
        b1.setName("Alice");
        b1.setValue("v1");

        TestBean b2 = new TestBean();
        b2.setName("Alice");
        b2.setValue("v1");

        assertTrue(N.equalsByCommonProps(b1, b2));

        b2.setName("Bob");
        assertFalse(N.equalsByCommonProps(b1, b2));
    }

    @Test
    public void testEqualsCollection() {
        Collection<Integer> col1 = Arrays.asList(1, 2, 3);
        Collection<Integer> col2 = Arrays.asList(1, 2, 3);
        assertThrows(UnsupportedOperationException.class, () -> N.equalsCollection(col1, col2));
    }

    @Test
    public void testHashCode_Boolean() {
        assertEquals(1231, N.hashCode(true));
        assertEquals(1237, N.hashCode(false));
    }

    @Test
    public void testHashCode_Char() {
        assertEquals('a', N.hashCode('a'));
        assertEquals('Z', N.hashCode('Z'));
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
    public void testHashCode_BooleanArray_WithRange() {
        boolean[] arr = { true, false, true, false };
        int hash1 = N.hashCode(arr, 1, 3);
        int hash2 = N.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_CharArray_WithRange() {
        char[] arr = { 'a', 'b', 'c', 'd' };
        int hash1 = N.hashCode(arr, 1, 3);
        int hash2 = N.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_ByteArray_WithRange() {
        byte[] arr = { 1, 2, 3, 4 };
        int hash1 = N.hashCode(arr, 1, 3);
        int hash2 = N.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_ShortArray_WithRange() {
        short[] arr = { 1, 2, 3, 4 };
        int hash1 = N.hashCode(arr, 1, 3);
        int hash2 = N.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_IntArray_WithRange() {
        int[] arr = { 1, 2, 3, 4 };
        int hash1 = N.hashCode(arr, 1, 3);
        int hash2 = N.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_LongArray_WithRange() {
        long[] arr = { 1L, 2L, 3L, 4L };
        int hash1 = N.hashCode(arr, 1, 3);
        int hash2 = N.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_FloatArray_WithRange() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f };
        int hash1 = N.hashCode(arr, 1, 3);
        int hash2 = N.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
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
    public void testHashCode_ObjectArray_WithRange() {
        String[] arr = { "a", "b", "c", "d" };
        int hash1 = N.hashCode(arr, 1, 3);
        int hash2 = N.hashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_char() {
        assertEquals('a', N.hashCode('a'));
    }

    @Test
    public void testHashCode_byte() {
        assertEquals((byte) 5, N.hashCode((byte) 5));
    }

    @Test
    public void testHashCode_short() {
        assertEquals((short) 100, N.hashCode((short) 100));
    }

    @Test
    public void testHashCode_int() {
        assertEquals(12345, N.hashCode(12345));
    }

    @Test
    public void testHashCode_long() {
        assertEquals(Long.hashCode(123456789L), N.hashCode(123456789L));
    }

    @Test
    public void testHashCode_float() {
        assertEquals(Float.floatToIntBits(1.23f), N.hashCode(1.23f));
        assertEquals(Float.floatToIntBits(Float.NaN), N.hashCode(Float.NaN));
    }

    @Test
    public void testHashCode_double() {
        assertEquals(Double.hashCode(1.2345), N.hashCode(1.2345));
        assertEquals(Double.hashCode(Double.NaN), N.hashCode(Double.NaN));
    }

    @Test
    public void testHashCode_intArrayRange() {
        int[] arr = { 10, 20, 30, 40, 50 };
        int expected = 1;
        expected = 31 * expected + 20;
        expected = 31 * expected + 30;
        assertEquals(expected, N.hashCode(arr, 1, 3));
        assertEquals(1, N.hashCode(arr, 1, 1));
    }

    @Test
    public void testHashCodePrimitives() {
        Assertions.assertEquals(1231, N.hashCode(true));
        Assertions.assertEquals(1237, N.hashCode(false));

        Assertions.assertEquals('a', N.hashCode('a'));

        Assertions.assertEquals(10, N.hashCode((byte) 10));

        Assertions.assertEquals(100, N.hashCode((short) 100));

        Assertions.assertEquals(1000, N.hashCode(1000));

        Assertions.assertEquals(Long.valueOf(1000L).hashCode(), N.hashCode(1000L));

        Assertions.assertEquals(Float.floatToIntBits(10.5f), N.hashCode(10.5f));

        Assertions.assertEquals(Double.valueOf(10.5).hashCode(), N.hashCode(10.5));
    }

    @Test
    public void testHashCodeArraysWithRange() {

        byte[] byteArr = { 1, 2, 3, 4, 5 };
        int expected = 1;
        expected = 31 * expected + 2;
        expected = 31 * expected + 3;
        Assertions.assertEquals(expected, N.hashCode(byteArr, 1, 3));

        short[] shortArr = { 1, 2, 3, 4, 5 };
        expected = 1;
        expected = 31 * expected + 2;
        expected = 31 * expected + 3;
        Assertions.assertEquals(expected, N.hashCode(shortArr, 1, 3));

        int[] intArr = { 1, 2, 3, 4, 5 };
        expected = 1;
        expected = 31 * expected + 2;
        expected = 31 * expected + 3;
        Assertions.assertEquals(expected, N.hashCode(intArr, 1, 3));

        long[] longArr = { 1L, 2L, 3L, 4L, 5L };
        expected = 1;
        expected = 31 * expected + Long.hashCode(2L);
        expected = 31 * expected + Long.hashCode(3L);
        Assertions.assertEquals(expected, N.hashCode(longArr, 1, 3));

        float[] floatArr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        expected = 1;
        expected = 31 * expected + Float.floatToIntBits(2.0f);
        expected = 31 * expected + Float.floatToIntBits(3.0f);
        Assertions.assertEquals(expected, N.hashCode(floatArr, 1, 3));

        double[] doubleArr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        expected = 1;
        expected = 31 * expected + Double.hashCode(2.0);
        expected = 31 * expected + Double.hashCode(3.0);
        Assertions.assertEquals(expected, N.hashCode(doubleArr, 1, 3));

        String[] strArr = { "a", "b", "c", "d", "e" };
        expected = 1;
        expected = 31 * expected + "b".hashCode();
        expected = 31 * expected + "c".hashCode();
        Assertions.assertEquals(expected, N.hashCode(strArr, 1, 3));
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
    public void testHashCode_CharArray() {
        char[] arr = { 'a', 'b', 'c' };
        int hash1 = N.hashCode(arr);
        int hash2 = N.hashCode(arr);
        assertEquals(hash1, hash2);

        assertEquals(0, N.hashCode((char[]) null));
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
    public void testHashCode_ShortArray() {
        short[] arr = { 1, 2, 3 };
        int hash1 = N.hashCode(arr);
        int hash2 = N.hashCode(arr);
        assertEquals(hash1, hash2);

        assertEquals(0, N.hashCode((short[]) null));
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
    public void testHashCode_LongArray() {
        long[] arr = { 1L, 2L, 3L };
        int hash1 = N.hashCode(arr);
        int hash2 = N.hashCode(arr);
        assertEquals(hash1, hash2);

        assertEquals(0, N.hashCode((long[]) null));
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
    public void testHashCode_DoubleArray() {
        double[] arr = { 1.0, 2.0, 3.0 };
        int hash1 = N.hashCode(arr);
        int hash2 = N.hashCode(arr);
        assertEquals(hash1, hash2);

        assertEquals(0, N.hashCode((double[]) null));
    }

    @Test
    public void testHashCode_ObjectArray() {
        String[] arr = { "a", "b", "c" };
        int hash1 = N.hashCode(arr);
        int hash2 = N.hashCode(arr);
        assertEquals(hash1, hash2);
        assertEquals(0, N.hashCode((Object[]) null));
    }

    @Test
    public void testHashCode_booleanArray() {
        assertEquals(0, N.hashCode((boolean[]) null));
        boolean[] ba1 = { true, false };
        boolean[] ba2 = { true, false };
        boolean[] ba3 = { false, true };
        assertEquals(N.hashCode(ba1), N.hashCode(ba2));
        assertNotEquals(N.hashCode(ba1), N.hashCode(ba3));
        assertEquals(Arrays.hashCode(new boolean[0]), N.hashCode(new boolean[0]));
    }

    @Test
    public void testHashCode_intArray() {
        assertEquals(0, N.hashCode((int[]) null));
        int[] arr = { 1, 2 };
        int expected = 1;
        expected = 31 * expected + 1;
        expected = 31 * expected + 2;
        assertEquals(expected, N.hashCode(arr));
    }

    @Test
    public void testHashCode_ObjectArrayRange() {
        Object[] arr = { "A", "B", null, "D" };
        int expected = 1;
        expected = 31 * expected + "B".hashCode();
        expected = 31 * expected + 0;
        assertEquals(expected, N.hashCode(arr, 1, 3));
        assertEquals(1, N.hashCode(arr, 1, 1));
    }

    @Test
    public void testHashCodeObject() {
        Assertions.assertEquals(0, N.hashCode((Object) null));

        String str = "test";
        Assertions.assertEquals(str.hashCode(), N.hashCode(str));

        int[] intArr = { 1, 2, 3 };
        Assertions.assertEquals(Arrays.hashCode(intArr), N.hashCode(intArr));

        String[] strArr = { "a", "b" };
        Assertions.assertEquals(Arrays.hashCode(strArr), N.hashCode(strArr));
    }

    @Test
    public void testHashCodeArrays() {
        boolean[] boolArr = { true, false, true };
        Assertions.assertEquals(Arrays.hashCode(boolArr), N.hashCode(boolArr));
        Assertions.assertEquals(0, N.hashCode((boolean[]) null));

        boolean[] boolArr2 = { false, true, false, true };
        int expected = 1;
        expected = 31 * expected + 1231;
        expected = 31 * expected + 1237;
        Assertions.assertEquals(expected, N.hashCode(boolArr2, 1, 3));

        char[] charArr = { 'a', 'b', 'c' };
        Assertions.assertEquals(Arrays.hashCode(charArr), N.hashCode(charArr));
        Assertions.assertEquals(0, N.hashCode((char[]) null));

        char[] charArr2 = { 'x', 'a', 'b', 'y' };
        int expected2 = 1;
        expected2 = 31 * expected2 + 'a';
        expected2 = 31 * expected2 + 'b';
        Assertions.assertEquals(expected2, N.hashCode(charArr2, 1, 3));

        byte[] byteArr = { 1, 2, 3 };
        Assertions.assertEquals(Arrays.hashCode(byteArr), N.hashCode(byteArr));
        Assertions.assertEquals(0, N.hashCode((byte[]) null));

        short[] shortArr = { 1, 2, 3 };
        Assertions.assertEquals(Arrays.hashCode(shortArr), N.hashCode(shortArr));
        Assertions.assertEquals(0, N.hashCode((short[]) null));

        int[] intArr = { 1, 2, 3 };
        Assertions.assertEquals(Arrays.hashCode(intArr), N.hashCode(intArr));
        Assertions.assertEquals(0, N.hashCode((int[]) null));

        long[] longArr = { 1L, 2L, 3L };
        Assertions.assertEquals(Arrays.hashCode(longArr), N.hashCode(longArr));
        Assertions.assertEquals(0, N.hashCode((long[]) null));

        float[] floatArr = { 1.0f, 2.0f, 3.0f };
        Assertions.assertEquals(Arrays.hashCode(floatArr), N.hashCode(floatArr));
        Assertions.assertEquals(0, N.hashCode((float[]) null));

        double[] doubleArr = { 1.0, 2.0, 3.0 };
        Assertions.assertEquals(Arrays.hashCode(doubleArr), N.hashCode(doubleArr));
        Assertions.assertEquals(0, N.hashCode((double[]) null));

        String[] strArr = { "a", "b", "c" };
        Assertions.assertEquals(Arrays.hashCode(strArr), N.hashCode(strArr));
        Assertions.assertEquals(0, N.hashCode((Object[]) null));
    }

    // ========== hashCode(primitive[], fromIndex, toIndex) null path ==========

    @Test
    public void testHashCode_CharArray_Range_NullArray_ReturnsZero() {
        // L5126: a == null after checkFromToIndex -> return 0
        assertEquals(0, N.hashCode((char[]) null, 0, 0));
    }

    @Test
    public void testHashCode_ByteArray_Range_NullArray_ReturnsZero() {
        // L5168: a == null -> return 0
        assertEquals(0, N.hashCode((byte[]) null, 0, 0));
    }

    @Test
    public void testHashCode_IntArray_Range_NullArray_ReturnsZero() {
        // L5252: null -> return 0
        assertEquals(0, N.hashCode((int[]) null, 0, 0));
    }

    @Test
    public void testHashCode_booleanArrayRange() {
        boolean[] arr = { true, false, true, false, true };
        assertEquals(0, N.hashCode((boolean[]) null, 0, 0));

        int fullHash = N.hashCode(arr, 0, arr.length);
        int partialHash = N.hashCode(arr, 1, 3);

        assertNotEquals(0, fullHash);
        assertNotEquals(0, partialHash);
        assertNotEquals(fullHash, partialHash);

        assertEquals(1, N.hashCode(arr, 1, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> N.hashCode(arr, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> N.hashCode(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.hashCode(arr, 3, 2));
    }

    @Test
    public void testDeepHashCode_ObjectArray_WithRange() {
        Object[] arr = { "a", "b", "c", "d" };
        int hash1 = N.deepHashCode(arr, 1, 3);
        int hash2 = N.deepHashCode(arr, 1, 3);
        assertEquals(hash1, hash2);
    }

    @Test
    public void testDeepHashCode_ObjectArrayRange() {
        Object[] arr = { "X", new int[] { 100, 200 }, new String[] { "deep" } };
        int expected = 1;
        expected = 31 * expected + N.deepHashCode(new int[] { 100, 200 });
        expected = 31 * expected + N.deepHashCode(new String[] { "deep" });
        assertEquals(expected, N.deepHashCode(arr, 1, 3));
        assertEquals(1, N.deepHashCode(arr, 1, 1));

    }

    @Test
    public void testDeepHashCodeArraysWithRange() {
        Object[] arr = { "a", new int[] { 1, 2 }, "c", new int[] { 3, 4 } };

        int expected = 1;
        expected = 31 * expected + Arrays.hashCode(new int[] { 1, 2 });
        expected = 31 * expected + "c".hashCode();

        Assertions.assertEquals(expected, N.deepHashCode(arr, 1, 3));
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
    public void testDeepHashCode() {
        Assertions.assertEquals(0, N.deepHashCode((Object) null));

        String str = "test";
        Assertions.assertEquals(str.hashCode(), N.deepHashCode(str));

        int[] intArr = { 1, 2, 3 };
        Assertions.assertEquals(Arrays.hashCode(intArr), N.deepHashCode(intArr));

        Object[] nested = { 1, new int[] { 2, 3 }, "test" };
        Assertions.assertEquals(Arrays.deepHashCode(nested), N.deepHashCode(nested));
    }

    @Test
    public void testDeepHashCodeArrays() {
        Object[] arr = { "a", 1, true };
        Assertions.assertEquals(Arrays.deepHashCode(arr), N.deepHashCode(arr));
        Assertions.assertEquals(0, N.deepHashCode((Object[]) null));

        Object[] nested = { new int[] { 1, 2 }, new String[] { "a", "b" } };
        Assertions.assertEquals(Arrays.deepHashCode(nested), N.deepHashCode(nested));
    }

    // ========== deepHashCode switch cases ==========

    @Test
    public void testDeepHashCode_BooleanArray_TriggersCase11() {
        // L5462: case 11 -> hashCode(boolean[])
        boolean[] arr = { true, false, true };
        assertEquals(N.hashCode(arr), N.deepHashCode(arr));
    }

    @Test
    public void testDeepHashCode_CharArray_TriggersCase12() {
        // L5465: case 12 -> hashCode(char[])
        char[] arr = { 'a', 'b' };
        assertEquals(N.hashCode(arr), N.deepHashCode(arr));
    }

    @Test
    public void testDeepHashCode_ByteArray_TriggersCase13() {
        // L5468: case 13 -> hashCode(byte[])
        byte[] arr = { 1, 2, 3 };
        assertEquals(N.hashCode(arr), N.deepHashCode(arr));
    }

    @Test
    public void testDeepHashCode_IntArray_TriggersCase15() {
        // L5474 (case 15) -> hashCode(int[])
        int[] arr = { 10, 20 };
        assertEquals(N.hashCode(arr), N.deepHashCode(arr));
    }

    @Test
    public void testDeepHashCode_ObjectArray_Null_ReturnsZero() {
        // L5533: a == null -> return 0 in deepHashCode(Object[])
        assertEquals(0, N.deepHashCode((Object[]) null));
    }

    @Test
    public void testDeepHashCode_ObjectArray_Default_UsesDeepHashCode() {
        // L5489: default case (array of Object[]) -> deepHashCode(Object[])
        String[][] nested = { { "a", "b" }, { "c" } };
        int hash = N.deepHashCode((Object) nested);
        assertTrue(hash != 0);
    }

    @Test
    public void testHashCodeEverything_Collection() {
        long h1 = N.hashCodeEverything(Arrays.asList(1, 2, 3));
        long h2 = N.hashCodeEverything(Arrays.asList(1, 2, 3));
        assertEquals(h1, h2);
        long h3 = N.hashCodeEverything(Arrays.asList(1, 2, 4));
        assertTrue(h1 != h3);
    }

    @Test
    public void testHashCodeEverything_Map() {
        java.util.LinkedHashMap<String, Integer> map = new java.util.LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        long h = N.hashCodeEverything(map);
        assertTrue(h != 0);
    }

    @Test
    public void testHashCodeEverything_PrimitiveArray() {
        long h = N.hashCodeEverything(new int[] { 1, 2, 3 });
        assertTrue(h != 0);
    }

    @Test
    public void testHashCodeEverything_ObjectArray() {
        long h1 = N.hashCodeEverything(new Object[] { "a", "b" });
        long h2 = N.hashCodeEverything(new Object[] { "a", "b" });
        assertEquals(h1, h2);
    }

    @Test
    public void testHashCodeEverything() {
        String str = "test";
        long hash = N.hashCodeEverything(str);
        assertTrue(hash != 0);
        assertEquals(0, N.hashCodeEverything(null));
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

        assertEquals(N.hashCodeEverything(left), N.hashCodeEverything(right));
        assertEquals(N.hashCodeEverything(nestedValues), N.hashCodeEverything(nestedValues.iterator()));
        assertNotEquals(N.hashCodeEverything(left), N.hashCodeEverything(Collections.singletonMap("values", nestedValues)));
    }

    // Tests for hashCodeEverything
    @Test
    public void testHashCodeEverything_Null() {
        assertEquals(0L, N.hashCodeEverything(null));
    }

    @Test
    public void testHashCodeEverything_SimpleObject() {
        long h = N.hashCodeEverything("hello");
        // same call produces same result
        assertEquals(h, N.hashCodeEverything("hello"));
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
    public void testToString_ByteValue() {
        assertEquals("5", N.toString((byte) 5));
        assertEquals("-1", N.toString((byte) -1));
    }

    @Test
    public void testToString_CharValue() {
        assertEquals("a", N.toString('a'));
        assertEquals("Z", N.toString('Z'));
    }

    @Test
    public void testToString_ShortValue() {
        assertEquals("100", N.toString((short) 100));
        assertEquals("-50", N.toString((short) -50));
    }

    @Test
    public void testToString_IntValue() {
        assertEquals("42", N.toString(42));
        assertEquals("-42", N.toString(-42));
    }

    @Test
    public void testToString_LongValue() {
        assertEquals("100", N.toString(100L));
        assertEquals("-100", N.toString(-100L));
    }

    @Test
    public void testToString_FloatValue() {
        String result = N.toString(3.14f);
        assertTrue(result.contains("3.14"));
    }

    @Test
    public void testToString_DoubleValue() {
        String result = N.toString(3.14);
        assertTrue(result.contains("3.14"));
    }

    @Test
    public void testToString_boolean() {
        assertEquals(String.valueOf(true), N.toString(true));
    }

    @Test
    public void testToString_char() {
        assertEquals(String.valueOf('z'), N.toString('z'));
    }

    @Test
    public void testToString_byte() {
        assertEquals(String.valueOf((byte) 12), N.toString((byte) 12));
    }

    @Test
    public void testToString_short() {
        assertEquals(String.valueOf((short) 123), N.toString((short) 123));
    }

    @Test
    public void testToString_int() {
        assertEquals(String.valueOf(12345), N.toString(12345));
    }

    @Test
    public void testToString_long() {
        assertEquals(String.valueOf(1234567L), N.toString(1234567L));
    }

    @Test
    public void testToString_float() {
        assertEquals(String.valueOf(1.2f), N.toString(1.2f));
    }

    @Test
    public void testToString_double() {
        assertEquals(String.valueOf(1.23), N.toString(1.23));
    }

    @Test
    public void testToString_intArrayRange() {
        int[] arr = { 10, 20, 30, 40 };
        assertEquals("[20, 30]", N.toString(arr, 1, 3));
        assertEquals("[]", N.toString(arr, 1, 1));
    }

    @Test
    public void testToStringPrimitives() {
        Assertions.assertEquals("true", N.toString(true));
        Assertions.assertEquals("false", N.toString(false));

        Assertions.assertEquals("a", N.toString('a'));

        Assertions.assertEquals("10", N.toString((byte) 10));

        Assertions.assertEquals("100", N.toString((short) 100));

        Assertions.assertEquals("1000", N.toString(1000));

        Assertions.assertEquals("1000", N.toString(1000L));

        Assertions.assertEquals("10.5", N.toString(10.5f));

        Assertions.assertEquals("10.5", N.toString(10.5));
    }

    @Test
    public void testToStringArraysWithRange() {

        byte[] byteArr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals("[2, 3]", N.toString(byteArr, 1, 3));

        short[] shortArr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals("[2, 3]", N.toString(shortArr, 1, 3));

        int[] intArr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals("[2, 3]", N.toString(intArr, 1, 3));

        long[] longArr = { 1L, 2L, 3L, 4L, 5L };
        Assertions.assertEquals("[2, 3]", N.toString(longArr, 1, 3));

        float[] floatArr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        Assertions.assertEquals("[2.0, 3.0]", N.toString(floatArr, 1, 3));

        double[] doubleArr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        Assertions.assertEquals("[2.0, 3.0]", N.toString(doubleArr, 1, 3));

        String[] strArr = { "a", "b", "c", "d", "e" };
        Assertions.assertEquals("[b, c]", N.toString(strArr, 1, 3));
    }

    @Test
    public void testToString_BooleanArray_WithRange() {
        boolean[] arr = { true, false, true, false };
        String result = N.toString(arr, 1, 3);
        assertNotNull(result);
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
        String result = N.toString(arr);
        assertNotNull(result);
    }

    @Test
    public void testToString_ObjectArray_WithRange() {
        String[] arr = { "a", "b", "c", "d" };
        String result = N.toString(arr, 1, 3);
        assertNotNull(result);
    }

    @Test
    public void testToString_Object_defaultIfNull() {
        assertEquals("default", N.toString(null, "default"));
        assertEquals("test", N.toString("test", "default"));
    }

    @Test
    public void testToString_booleanArray() {
        assertEquals("null", N.toString((boolean[]) null));
        assertEquals("[]", N.toString(new boolean[0]));
        assertEquals("[true, false, true]", N.toString(new boolean[] { true, false, true }));
    }

    @Test
    public void testToString_charArray() {
        assertEquals("null", N.toString((char[]) null));
        assertEquals("[]", N.toString(new char[0]));
        assertEquals("[a, b, c]", N.toString(new char[] { 'a', 'b', 'c' }));
    }

    @Test
    public void testToString_intArray() {
        assertEquals("null", N.toString((int[]) null));
        assertEquals("[]", N.toString(new int[0]));
        assertEquals("[1, 2, 3]", N.toString(new int[] { 1, 2, 3 }));
    }

    @Test
    public void testToString_ObjectArrayRange() {
        Object[] arr = { "one", 2, "three", null, 5.0 };
        assertEquals("[2, three, null]", N.toString(arr, 1, 4));
        assertEquals("[]", N.toString(arr, 1, 1));
    }

    @Test
    public void testToStringObject() {
        Assertions.assertEquals("null", N.toString((Object) null));

        Assertions.assertEquals("test", N.toString("test"));

        int[] intArr = { 1, 2, 3 };
        Assertions.assertEquals("[1, 2, 3]", N.toString(intArr));

        List<String> list = Arrays.asList("a", "b", "c");
        Iterator<String> iter = list.iterator();
        Assertions.assertEquals("[a, b, c]", N.toString(iter));

        Assertions.assertEquals("[a, b, c]", N.toString(list));

        Object obj = new Object();
        Assertions.assertEquals(obj.toString(), N.toString(obj));
    }

    @Test
    public void testToStringArrays() {
        Assertions.assertEquals("[true, false, true]", N.toString(new boolean[] { true, false, true }));
        Assertions.assertEquals("null", N.toString((boolean[]) null));
        Assertions.assertEquals("[]", N.toString(new boolean[0]));

        boolean[] boolArr = { true, false, true, false };
        Assertions.assertEquals("[false, true]", N.toString(boolArr, 1, 3));

        Assertions.assertEquals("[a, b, c]", N.toString(new char[] { 'a', 'b', 'c' }));
        Assertions.assertEquals("null", N.toString((char[]) null));
        Assertions.assertEquals("[]", N.toString(new char[0]));

        char[] charArr = { 'a', 'b', 'c', 'd' };
        Assertions.assertEquals("[b, c]", N.toString(charArr, 1, 3));

        Assertions.assertEquals("[1, 2, 3]", N.toString(new byte[] { 1, 2, 3 }));
        Assertions.assertEquals("null", N.toString((byte[]) null));
        Assertions.assertEquals("[]", N.toString(new byte[0]));

        Assertions.assertEquals("[1, 2, 3]", N.toString(new short[] { 1, 2, 3 }));
        Assertions.assertEquals("null", N.toString((short[]) null));
        Assertions.assertEquals("[]", N.toString(new short[0]));

        Assertions.assertEquals("[1, 2, 3]", N.toString(new int[] { 1, 2, 3 }));
        Assertions.assertEquals("null", N.toString((int[]) null));
        Assertions.assertEquals("[]", N.toString(new int[0]));

        Assertions.assertEquals("[1, 2, 3]", N.toString(new long[] { 1L, 2L, 3L }));
        Assertions.assertEquals("null", N.toString((long[]) null));
        Assertions.assertEquals("[]", N.toString(new long[0]));

        Assertions.assertEquals("[1.0, 2.0, 3.0]", N.toString(new float[] { 1.0f, 2.0f, 3.0f }));
        Assertions.assertEquals("null", N.toString((float[]) null));
        Assertions.assertEquals("[]", N.toString(new float[0]));

        Assertions.assertEquals("[1.0, 2.0, 3.0]", N.toString(new double[] { 1.0, 2.0, 3.0 }));
        Assertions.assertEquals("null", N.toString((double[]) null));
        Assertions.assertEquals("[]", N.toString(new double[0]));

        Assertions.assertEquals("[a, b, c]", N.toString(new String[] { "a", "b", "c" }));
        Assertions.assertEquals("null", N.toString((Object[]) null));
        Assertions.assertEquals("[]", N.toString(new Object[0]));
    }

    // ========== toString(primitive[], fromIndex, toIndex) null/empty path ==========

    @Test
    public void testToString_BooleanArray_Range_NullArray_ReturnsNullString() {
        // L5863: a == null -> return Strings.NULL
        assertEquals("null", N.toString((boolean[]) null, 0, 0));
    }

    @Test
    public void testToString_BooleanArray_Range_EmptyArray_ReturnsEmptyArrayString() {
        // L5865: a.length == 0 -> return STR_FOR_EMPTY_ARRAY ("[]")
        String result = N.toString(new boolean[0], 0, 0);
        assertEquals("[]", result);
    }

    @Test
    public void testToString_CharArray_Range_NullArray_ReturnsNullString() {
        // L5937: a == null -> return Strings.NULL
        assertEquals("null", N.toString((char[]) null, 0, 0));
    }

    @Test
    public void testToString_ByteArray_Range_NullArray_ReturnsNullString() {
        // L6013: a == null -> return Strings.NULL
        assertEquals("null", N.toString((byte[]) null, 0, 0));
    }

    @Test
    public void testToString_ByteArray_Range_EmptyArray_ReturnsEmptyArrayString() {
        // L6015: a.length == 0 -> return STR_FOR_EMPTY_ARRAY ("[]")
        assertEquals("[]", N.toString(new byte[0], 0, 0));
    }

    // ========== toString(StringBuilder, primitive[]) null/empty/nonempty paths ==========

    @Test
    public void testToString_StringBuilder_BooleanArray_Null() {
        // L5873: sb.append(Strings.NULL)
        StringBuilder sb = new StringBuilder();
        N.toString(sb, (boolean[]) null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testToString_StringBuilder_BooleanArray_Empty() {
        // L5875: sb.append(STR_FOR_EMPTY_ARRAY)
        StringBuilder sb = new StringBuilder();
        N.toString(sb, new boolean[0]);
        assertNotNull(sb.toString());
    }

    @Test
    public void testToString_StringBuilder_BooleanArray_NonEmpty() {
        // L5876: toString(sb, a, 0, a.length)
        StringBuilder sb = new StringBuilder();
        N.toString(sb, new boolean[] { true, false });
        assertTrue(sb.toString().contains("true"));
    }

    @Test
    public void testToString_StringBuilder_CharArray_Null() {
        // L5949: sb.append(Strings.NULL)
        StringBuilder sb = new StringBuilder();
        N.toString(sb, (char[]) null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testToString_StringBuilder_ByteArray_Null() {
        // L6023: sb.append(Strings.NULL)
        StringBuilder sb = new StringBuilder();
        N.toString(sb, (byte[]) null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testToString_StringBuilder_ByteArray_Empty() {
        // L6025
        StringBuilder sb = new StringBuilder();
        N.toString(sb, new byte[0]);
        assertNotNull(sb.toString());
    }

    @Test
    public void testToString_StringBuilder_ByteArray_NonEmpty() {
        // L6026
        StringBuilder sb = new StringBuilder();
        N.toString(sb, new byte[] { 1, 2 });
        assertTrue(sb.toString().contains("1"));
    }

    @Test
    public void testToString_booleanArrayRange() {
        boolean[] arr = { true, false, true, true, false };
        assertEquals("[false, true, true]", N.toString(arr, 1, 4));
        assertEquals("[]", N.toString(arr, 1, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> N.toString(arr, 0, 6));
    }

    @Test
    public void testToString_charArrayRange() {
        char[] arr = { 'h', 'e', 'l', 'l', 'o' };
        assertEquals("[e, l, l]", N.toString(arr, 1, 4));
        assertEquals("[]", N.toString(arr, 1, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> N.toString(arr, 0, 6));
    }

    @Test
    public void testDeepToString_ObjectArray_Cyclic() {
        Object[] cyclicArray = new Object[2];
        cyclicArray[0] = "Element 1";
        cyclicArray[1] = cyclicArray;
        assertEquals("[Element 1, [...]]", N.deepToString(cyclicArray));

        Object[] arr = new Object[1];
        arr[0] = arr;
        assertEquals("[[...]]", N.deepToString(arr));

        Object[] a = new Object[2];
        Object[] b = new Object[] { "b" };
        a[0] = b;
        a[1] = b;
        assertEquals("[[b], [b]]", N.deepToString(a));

        Object[] parent = new Object[1];
        Object[] child = new Object[1];
        parent[0] = child;
        child[0] = parent;
        assertEquals("[[[...]]]", N.deepToString(parent));
    }

    @Test
    public void testDeepToStringArraysWithRange() {
        Object[] arr = { "a", new int[] { 1, 2 }, "c", new int[] { 3, 4 } };
        Assertions.assertEquals("[[1, 2], c]", N.deepToString(arr, 1, 3));
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
    public void testDeepToString_ObjectArray_defaultIfNull() {
        assertEquals("fallback", N.deepToString(null, "fallback"));
        assertEquals("[]", N.deepToString(new Object[0], "fallback"));
    }

    @Test
    public void testDeepToString() {
        Assertions.assertEquals("null", N.deepToString(null));

        Assertions.assertEquals("test", N.deepToString("test"));

        int[] intArr = { 1, 2, 3 };
        Assertions.assertEquals("[1, 2, 3]", N.deepToString(intArr));

        Object[] nested = { 1, new int[] { 2, 3 }, "test" };
        Assertions.assertEquals("[1, [2, 3], test]", N.deepToString(nested));

        Assertions.assertEquals("[true, false]", N.deepToString(new boolean[] { true, false }));
        Assertions.assertEquals("[a, b]", N.deepToString(new char[] { 'a', 'b' }));
        Assertions.assertEquals("[1, 2]", N.deepToString(new byte[] { 1, 2 }));
        Assertions.assertEquals("[1, 2]", N.deepToString(new short[] { 1, 2 }));
        Assertions.assertEquals("[1, 2]", N.deepToString(new int[] { 1, 2 }));
        Assertions.assertEquals("[1, 2]", N.deepToString(new long[] { 1L, 2L }));
        Assertions.assertEquals("[1.0, 2.0]", N.deepToString(new float[] { 1.0f, 2.0f }));
        Assertions.assertEquals("[1.0, 2.0]", N.deepToString(new double[] { 1.0, 2.0 }));
        Assertions.assertEquals("[a, b]", N.deepToString(new String[] { "a", "b" }));
    }

    @Test
    public void testDeepToStringArrays() {
        Object[] arr = { "a", 1, true };
        Assertions.assertEquals("[a, 1, true]", N.deepToString(arr));
        Assertions.assertEquals("null", N.deepToString((Object[]) null));
        Assertions.assertEquals("[]", N.deepToString(new Object[0]));

        Object[] nested = { new int[] { 1, 2 }, new String[] { "a", "b" } };
        Assertions.assertEquals("[[1, 2], [a, b]]", N.deepToString(nested));

        Object[] circular = new Object[2];
        circular[0] = "test";
        circular[1] = circular;
        String result = N.deepToString(circular);
        Assertions.assertTrue(result.contains("[...]"));
    }

    @Test
    public void testDeepToStringWithDefault() {
        Object[] arr = { "a", "b" };
        Assertions.assertEquals("[a, b]", N.deepToString(arr, "default"));
        Assertions.assertEquals("default", N.deepToString((Object[]) null, "default"));
    }

    // ========== deepToString with null element ==========

    @Test
    public void testDeepToString_ObjectArray_WithNullElement_AppendsNullString() {
        // L6677: null element in array -> sb.append(Strings.NULL_CHAR_ARRAY)
        Object[] arr = { "first", null, "third" };
        String result = N.deepToString(arr);
        assertTrue(result.contains("null"));
        assertTrue(result.contains("first"));
    }

    @Test
    public void testDeepToString_ObjectArrayRange() {
        Object[] arr = { "start", new Object[] { "nested1", new int[] { 10, 20 } }, "middle", new String[] { "s1", "s2" }, "end" };
        String expected = "[[nested1, [10, 20]], middle, [s1, s2]]";
        assertEquals(expected, N.deepToString(arr, 1, 4));
        assertEquals("[]", N.deepToString(arr, 1, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> N.deepToString(arr, 0, 6));
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
    public void testLen_booleanArray() {
        assertEquals(0, N.len((boolean[]) null));
        assertEquals(0, N.len(new boolean[0]));
        assertEquals(2, N.len(new boolean[] { true, false }));
    }

    @Test
    public void testLen_charArray() {
        assertEquals(0, N.len((char[]) null));
        assertEquals(0, N.len(new char[0]));
        assertEquals(3, N.len(new char[] { 'a', 'b', 'c' }));
    }

    @Test
    public void testLen_byteArray() {
        assertEquals(0, N.len((byte[]) null));
        assertEquals(0, N.len(new byte[0]));
        assertEquals(2, N.len(new byte[] { 1, 2 }));
    }

    @Test
    public void testLen_shortArray() {
        assertEquals(0, N.len((short[]) null));
        assertEquals(0, N.len(new short[0]));
        assertEquals(2, N.len(new short[] { 10, 20 }));
    }

    @Test
    public void testLen_intArray() {
        assertEquals(0, N.len((int[]) null));
        assertEquals(0, N.len(new int[0]));
        assertEquals(2, N.len(new int[] { 100, 200 }));
    }

    @Test
    public void testLen_longArray() {
        assertEquals(0, N.len((long[]) null));
        assertEquals(0, N.len(new long[0]));
        assertEquals(2, N.len(new long[] { 1000L, 2000L }));
    }

    @Test
    public void testLen_floatArray() {
        assertEquals(0, N.len((float[]) null));
        assertEquals(0, N.len(new float[0]));
        assertEquals(2, N.len(new float[] { 1.0f, 2.0f }));
    }

    @Test
    public void testLen_doubleArray() {
        assertEquals(0, N.len((double[]) null));
        assertEquals(0, N.len(new double[0]));
        assertEquals(2, N.len(new double[] { 1.0, 2.0 }));
    }

    @Test
    public void testLen_ObjectArray() {
        assertEquals(0, N.len((Object[]) null));
        assertEquals(0, N.len(new Object[0]));
        assertEquals(2, N.len(new Object[] { "a", "b" }));
    }

    @Test
    public void testLen() {
        Assertions.assertEquals(4, N.len("test"));
        Assertions.assertEquals(0, N.len(""));
        Assertions.assertEquals(0, N.len((CharSequence) null));

        Assertions.assertEquals(3, N.len(new boolean[] { true, false, true }));
        Assertions.assertEquals(0, N.len(new boolean[0]));
        Assertions.assertEquals(0, N.len((boolean[]) null));

        Assertions.assertEquals(3, N.len(new char[] { 'a', 'b', 'c' }));
        Assertions.assertEquals(0, N.len(new char[0]));
        Assertions.assertEquals(0, N.len((char[]) null));

        Assertions.assertEquals(3, N.len(new byte[] { 1, 2, 3 }));
        Assertions.assertEquals(0, N.len(new byte[0]));
        Assertions.assertEquals(0, N.len((byte[]) null));

        Assertions.assertEquals(3, N.len(new short[] { 1, 2, 3 }));
        Assertions.assertEquals(0, N.len(new short[0]));
        Assertions.assertEquals(0, N.len((short[]) null));

        Assertions.assertEquals(3, N.len(new int[] { 1, 2, 3 }));
        Assertions.assertEquals(0, N.len(new int[0]));
        Assertions.assertEquals(0, N.len((int[]) null));

        Assertions.assertEquals(3, N.len(new long[] { 1L, 2L, 3L }));
        Assertions.assertEquals(0, N.len(new long[0]));
        Assertions.assertEquals(0, N.len((long[]) null));

        Assertions.assertEquals(3, N.len(new float[] { 1.0f, 2.0f, 3.0f }));
        Assertions.assertEquals(0, N.len(new float[0]));
        Assertions.assertEquals(0, N.len((float[]) null));

        Assertions.assertEquals(3, N.len(new double[] { 1.0, 2.0, 3.0 }));
        Assertions.assertEquals(0, N.len(new double[0]));
        Assertions.assertEquals(0, N.len((double[]) null));

        Assertions.assertEquals(3, N.len(new Object[] { "a", "b", "c" }));
        Assertions.assertEquals(0, N.len(new Object[0]));
        Assertions.assertEquals(0, N.len((Object[]) null));
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
    public void testSize_PrimitiveList() {
        assertEquals(0, N.size((PrimitiveList) null));
    }

    @Test
    public void testSize() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals(3, N.size(list));
        Assertions.assertEquals(0, N.size(new ArrayList<>()));
        Assertions.assertEquals(0, N.size((Collection<?>) null));

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Assertions.assertEquals(2, N.size(map));
        Assertions.assertEquals(0, N.size(new HashMap<>()));
        Assertions.assertEquals(0, N.size((Map<?, ?>) null));
    }

    // --- Tests for size(PrimitiveList) ---

    @Test
    public void testSize_PrimitiveList_Empty() {
        assertEquals(0, N.size(IntList.of()));
    }

    @Test
    public void testSize_PrimitiveList_NonEmpty() {
        assertEquals(3, N.size(IntList.of(1, 2, 3)));
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
    public void testIsEmpty_floatArray() {
        assertTrue(N.isEmpty((float[]) null));
        assertTrue(N.isEmpty(new float[0]));
        assertFalse(N.isEmpty(new float[] { 1f }));
    }

    @Test
    public void testIsEmpty_Iterable() {
        assertTrue(N.isEmpty((Iterable<?>) null));
        assertTrue(N.isEmpty(Collections.emptyList()));
        assertFalse(N.isEmpty(Arrays.asList(1)));

        Iterable<Integer> emptyIterable = () -> Collections.<Integer> emptyIterator();
        assertTrue(N.isEmpty(emptyIterable));

        Iterable<Integer> nonEmptyIterable = () -> Arrays.asList(1, 2).iterator();
        assertFalse(N.isEmpty(nonEmptyIterable));
    }

    @Test
    public void testIsEmpty_Iterator() {
        assertTrue(N.isEmpty((Iterator<?>) null));
        assertTrue(N.isEmpty(Collections.emptyIterator()));
        assertFalse(N.isEmpty(Arrays.asList(1, 2).iterator()));
    }

    @Test
    public void testIsEmpty_PrimitiveList() {
        assertTrue(N.isEmpty((PrimitiveList) null));
    }

    @Test
    public void testIsEmpty_Multiset() {
        assertTrue(N.isEmpty((Multiset<?>) null));
    }

    @Test
    public void testIsEmpty_Multimap() {
        assertTrue(N.isEmpty((Multimap<?, ?, ?>) null));
    }

    @Test
    public void testIsEmpty_Dataset() {
        assertTrue(N.isEmpty((Dataset) null));
    }

    @Test
    public void testIsEmpty() {
        Assertions.assertTrue(N.isEmpty((CharSequence) null));
        Assertions.assertTrue(N.isEmpty(""));
        Assertions.assertFalse(N.isEmpty("test"));

        Assertions.assertTrue(N.isEmpty((boolean[]) null));
        Assertions.assertTrue(N.isEmpty(new boolean[0]));
        Assertions.assertFalse(N.isEmpty(new boolean[] { true }));

        Assertions.assertTrue(N.isEmpty((char[]) null));
        Assertions.assertTrue(N.isEmpty(new char[0]));
        Assertions.assertFalse(N.isEmpty(new char[] { 'a' }));

        Assertions.assertTrue(N.isEmpty((byte[]) null));
        Assertions.assertTrue(N.isEmpty(new byte[0]));
        Assertions.assertFalse(N.isEmpty(new byte[] { 1 }));

        Assertions.assertTrue(N.isEmpty((short[]) null));
        Assertions.assertTrue(N.isEmpty(new short[0]));
        Assertions.assertFalse(N.isEmpty(new short[] { 1 }));

        Assertions.assertTrue(N.isEmpty((int[]) null));
        Assertions.assertTrue(N.isEmpty(new int[0]));
        Assertions.assertFalse(N.isEmpty(new int[] { 1 }));

        Assertions.assertTrue(N.isEmpty((long[]) null));
        Assertions.assertTrue(N.isEmpty(new long[0]));
        Assertions.assertFalse(N.isEmpty(new long[] { 1L }));

        Assertions.assertTrue(N.isEmpty((float[]) null));
        Assertions.assertTrue(N.isEmpty(new float[0]));
        Assertions.assertFalse(N.isEmpty(new float[] { 1.0f }));

        Assertions.assertTrue(N.isEmpty((double[]) null));
        Assertions.assertTrue(N.isEmpty(new double[0]));
        Assertions.assertFalse(N.isEmpty(new double[] { 1.0 }));

        Assertions.assertTrue(N.isEmpty((Object[]) null));
        Assertions.assertTrue(N.isEmpty(new Object[0]));
        Assertions.assertFalse(N.isEmpty(new Object[] { "a" }));

        Assertions.assertTrue(N.isEmpty((Collection<?>) null));
        Assertions.assertTrue(N.isEmpty(new ArrayList<>()));
        Assertions.assertFalse(N.isEmpty(Arrays.asList("a")));

        Assertions.assertTrue(N.isEmpty((Iterable<?>) null));
        Assertions.assertTrue(N.isEmpty((Iterable<?>) new ArrayList<>()));
        Assertions.assertFalse(N.isEmpty((Iterable<?>) Arrays.asList("a")));

        Assertions.assertTrue(N.isEmpty((Iterator<?>) null));
        Assertions.assertTrue(N.isEmpty(new ArrayList<>().iterator()));
        Assertions.assertFalse(N.isEmpty(Arrays.asList("a").iterator()));

        Assertions.assertTrue(N.isEmpty((Map<?, ?>) null));
        Assertions.assertTrue(N.isEmpty(new HashMap<>()));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Assertions.assertFalse(N.isEmpty(map));
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
    public void testIsTrue() {
        assertTrue(N.isTrue(true));
        assertFalse(N.isTrue(false));
        assertFalse(N.isTrue(null));
    }

    @Test
    public void testBooleanChecks() {
        Assertions.assertTrue(N.isTrue(Boolean.TRUE));
        Assertions.assertFalse(N.isTrue(Boolean.FALSE));
        Assertions.assertFalse(N.isTrue(null));

        Assertions.assertTrue(N.isNotTrue(null));
        Assertions.assertTrue(N.isNotTrue(Boolean.FALSE));
        Assertions.assertFalse(N.isNotTrue(Boolean.TRUE));

        Assertions.assertTrue(N.isFalse(Boolean.FALSE));
        Assertions.assertFalse(N.isFalse(Boolean.TRUE));
        Assertions.assertFalse(N.isFalse(null));

        Assertions.assertTrue(N.isNotFalse(null));
        Assertions.assertTrue(N.isNotFalse(Boolean.TRUE));
        Assertions.assertFalse(N.isNotFalse(Boolean.FALSE));
    }

    @Test
    public void testIsNotTrue() {
        assertTrue(N.isNotTrue(false));
        assertTrue(N.isNotTrue(null));
        assertFalse(N.isNotTrue(true));
    }

    @Test
    public void testIsFalse() {
        assertTrue(N.isFalse(false));
        assertFalse(N.isFalse(true));
        assertFalse(N.isFalse(null));
    }

    @Test
    public void testIsNotFalse() {
        assertTrue(N.isNotFalse(true));
        assertTrue(N.isNotFalse(null));
        assertFalse(N.isNotFalse(false));
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
    public void testNotEmpty_Iterable() {
        assertFalse(N.notEmpty((Iterable<?>) null));
        assertFalse(N.notEmpty(Collections.emptyList()));
        assertTrue(N.notEmpty(Arrays.asList(1)));

        Iterable<Integer> emptyIterable = () -> Collections.<Integer> emptyIterator();
        assertFalse(N.notEmpty(emptyIterable));

        Iterable<Integer> nonEmptyIterable = () -> Arrays.asList(1, 2).iterator();
        assertTrue(N.notEmpty(nonEmptyIterable));
    }

    @Test
    public void testNotEmpty_Iterator() {
        assertFalse(N.notEmpty((Iterator<?>) null));
        assertFalse(N.notEmpty(Collections.emptyIterator()));
        assertTrue(N.notEmpty(Arrays.asList(1).iterator()));
    }

    @Test
    public void testNotEmpty() {
        Assertions.assertFalse(N.notEmpty((CharSequence) null));
        Assertions.assertFalse(N.notEmpty(""));
        Assertions.assertTrue(N.notEmpty("test"));

        Assertions.assertFalse(N.notEmpty((boolean[]) null));
        Assertions.assertFalse(N.notEmpty(new boolean[0]));
        Assertions.assertTrue(N.notEmpty(new boolean[] { true }));

        Assertions.assertFalse(N.notEmpty((char[]) null));
        Assertions.assertFalse(N.notEmpty(new char[0]));
        Assertions.assertTrue(N.notEmpty(new char[] { 'a' }));

        Assertions.assertFalse(N.notEmpty((byte[]) null));
        Assertions.assertFalse(N.notEmpty(new byte[0]));
        Assertions.assertTrue(N.notEmpty(new byte[] { 1 }));

        Assertions.assertFalse(N.notEmpty((short[]) null));
        Assertions.assertFalse(N.notEmpty(new short[0]));
        Assertions.assertTrue(N.notEmpty(new short[] { 1 }));

        Assertions.assertFalse(N.notEmpty((int[]) null));
        Assertions.assertFalse(N.notEmpty(new int[0]));
        Assertions.assertTrue(N.notEmpty(new int[] { 1 }));

        Assertions.assertFalse(N.notEmpty((long[]) null));
        Assertions.assertFalse(N.notEmpty(new long[0]));
        Assertions.assertTrue(N.notEmpty(new long[] { 1L }));

        Assertions.assertFalse(N.notEmpty((float[]) null));
        Assertions.assertFalse(N.notEmpty(new float[0]));
        Assertions.assertTrue(N.notEmpty(new float[] { 1.0f }));

        Assertions.assertFalse(N.notEmpty((double[]) null));
        Assertions.assertFalse(N.notEmpty(new double[0]));
        Assertions.assertTrue(N.notEmpty(new double[] { 1.0 }));

        Assertions.assertFalse(N.notEmpty((Object[]) null));
        Assertions.assertFalse(N.notEmpty(new Object[0]));
        Assertions.assertTrue(N.notEmpty(new Object[] { "a" }));

        Assertions.assertFalse(N.notEmpty((Collection<?>) null));
        Assertions.assertFalse(N.notEmpty(new ArrayList<>()));
        Assertions.assertTrue(N.notEmpty(Arrays.asList("a")));

        Assertions.assertFalse(N.notEmpty((Iterable<?>) null));
        Assertions.assertFalse(N.notEmpty((Iterable<?>) new ArrayList<>()));
        Assertions.assertTrue(N.notEmpty((Iterable<?>) Arrays.asList("a")));

        Assertions.assertFalse(N.notEmpty((Iterator<?>) null));
        Assertions.assertFalse(N.notEmpty(new ArrayList<>().iterator()));
        Assertions.assertTrue(N.notEmpty(Arrays.asList("a").iterator()));

        Assertions.assertFalse(N.notEmpty((Map<?, ?>) null));
        Assertions.assertFalse(N.notEmpty(new HashMap<>()));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Assertions.assertTrue(N.notEmpty(map));
    }

    // --- Tests for notEmpty (PrimitiveList, Multiset, Multimap, Dataset) ---

    @Test
    public void testNotEmpty_PrimitiveList() {
        assertTrue(N.notEmpty(IntList.of(1)));
        assertFalse(N.notEmpty(IntList.of()));
    }

    @Test
    public void testNotEmpty_Multiset() {
        Multiset<String> ms = new Multiset<>();
        assertFalse(N.notEmpty(ms));
        ms.add("a");
        assertTrue(N.notEmpty(ms));
    }

    @Test
    public void testNotEmpty_Multimap() {
        ListMultimap<String, Integer> mm = N.newListMultimap();
        assertFalse(N.notEmpty(mm));
        mm.put("key", 1);
        assertTrue(N.notEmpty(mm));
    }

    @Test
    public void testNotEmpty_Dataset() {
        assertFalse(N.notEmpty(N.newEmptyDataset()));
        Dataset ds = N.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        assertTrue(N.notEmpty(ds));
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
    public void testAnyNull_ThreeObjects() {
        assertTrue(N.anyNull(null, "a", "b"));
        assertTrue(N.anyNull("a", null, "b"));
        assertTrue(N.anyNull("a", "b", null));
        assertTrue(N.anyNull(null, null, "b"));
        assertFalse(N.anyNull("a", "b", "c"));
    }

    @Test
    public void testAnyNull_VarArgs_explicitNullArray() {
        assertFalse(N.anyNull((Object[]) null));
    }

    @Test
    public void testAnyNull_Iterable() {
        assertFalse(N.anyNull((Iterable<?>) null));
        assertFalse(N.anyNull(Collections.emptyList()));
        assertFalse(N.anyNull(Arrays.asList("a", "b")));
        assertTrue(N.anyNull(Arrays.asList("a", null, "b")));
        List<String> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        assertTrue(N.anyNull(listWithNull));

    }

    @Test
    public void testAnyNull() {
        Assertions.assertTrue(N.anyNull(null, "b"));
        Assertions.assertTrue(N.anyNull("a", null));
        Assertions.assertTrue(N.anyNull(null, null));
        Assertions.assertFalse(N.anyNull("a", "b"));

        Assertions.assertTrue(N.anyNull(null, "b", "c"));
        Assertions.assertTrue(N.anyNull("a", null, "c"));
        Assertions.assertTrue(N.anyNull("a", "b", null));
        Assertions.assertTrue(N.anyNull(null, null, null));
        Assertions.assertFalse(N.anyNull("a", "b", "c"));

        Assertions.assertFalse(N.anyNull());
        Assertions.assertTrue(N.anyNull("a", null, "c", "d"));
        Assertions.assertFalse(N.anyNull("a", "b", "c", "d"));

        Assertions.assertFalse(N.anyNull((Iterable<?>) null));
        Assertions.assertFalse(N.anyNull(new ArrayList<>()));
        Assertions.assertTrue(N.anyNull(Arrays.asList("a", null, "c")));
        Assertions.assertFalse(N.anyNull(Arrays.asList("a", "b", "c")));
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
    public void testAnyEmpty_CharSequenceVarArgs() {
        assertTrue(N.anyEmpty((String) null));
        assertFalse(N.anyEmpty((String[]) null));
        assertTrue(N.anyEmpty(null, "foo"));
        assertTrue(N.anyEmpty("", "bar"));
        assertFalse(N.anyEmpty("foo", "bar"));
        assertFalse(N.anyEmpty(new String[] {}));
        assertTrue(N.anyEmpty(new String[] { "" }));
    }

    @Test
    public void testAnyEmpty_CharSequenceIterable() {
        assertTrue(N.anyEmpty(Arrays.asList(null, "a")));
        assertTrue(N.anyEmpty(Arrays.asList("", "a")));
        assertFalse(N.anyEmpty(Arrays.asList("a", "b")));
        assertFalse(N.anyEmpty(Collections.<CharSequence> emptyList()));
    }

    @Test
    public void testAnyEmpty_TwoObjectArrays() {
        assertTrue(N.anyEmpty(null, new Object[] { "a" }));
        assertTrue(N.anyEmpty(new Object[0], new Object[] { "a" }));
        assertTrue(N.anyEmpty(new Object[] { "a" }, null));
        assertTrue(N.anyEmpty(new Object[] { "a" }, new Object[0]));
        assertFalse(N.anyEmpty(new Object[] { "a" }, new Object[] { "b" }));
    }

    @Test
    public void testAnyEmpty_ThreeObjectArrays() {
        assertTrue(N.anyEmpty(null, new Object[] { "a" }, new Object[] { "b" }));
        assertTrue(N.anyEmpty(new Object[0], new Object[] { "a" }, new Object[] { "b" }));
        assertTrue(N.anyEmpty(new Object[] { "a" }, new Object[0], new Object[] { "b" }));
        assertFalse(N.anyEmpty(new Object[] { "a" }, new Object[] { "b" }, new Object[] { "c" }));
    }

    @Test
    public void testAnyEmpty_TwoCollections() {
        assertTrue(N.anyEmpty(null, Arrays.asList("a")));
        assertTrue(N.anyEmpty(Collections.emptyList(), Arrays.asList("a")));
        assertFalse(N.anyEmpty(Arrays.asList("a"), Arrays.asList("b")));
    }

    @Test
    public void testAnyEmpty_ThreeCollections() {
        assertTrue(N.anyEmpty(null, Arrays.asList("a"), Arrays.asList("b")));
        assertTrue(N.anyEmpty(Collections.emptyList(), Arrays.asList("a"), Arrays.asList("b")));
        assertTrue(N.anyEmpty(Arrays.asList("a"), Collections.emptyList(), Arrays.asList("b")));
        assertFalse(N.anyEmpty(Arrays.asList("a"), Arrays.asList("b"), Arrays.asList("c")));
    }

    @Test
    public void testAnyEmpty_TwoMaps() {
        Map<String, String> nonEmptyMap = new HashMap<>();
        nonEmptyMap.put("k", "v");
        assertTrue(N.anyEmpty(null, nonEmptyMap));
        assertTrue(N.anyEmpty(Collections.emptyMap(), nonEmptyMap));
        assertFalse(N.anyEmpty(nonEmptyMap, nonEmptyMap));
    }

    @Test
    public void testAnyEmpty_ThreeMaps() {
        Map<String, String> nonEmptyMap = new HashMap<>();
        nonEmptyMap.put("k", "v");
        assertTrue(N.anyEmpty(null, nonEmptyMap, nonEmptyMap));
        assertTrue(N.anyEmpty(Collections.emptyMap(), nonEmptyMap, nonEmptyMap));
        assertTrue(N.anyEmpty(nonEmptyMap, Collections.emptyMap(), nonEmptyMap));
        assertFalse(N.anyEmpty(nonEmptyMap, nonEmptyMap, nonEmptyMap));
    }

    @Test
    public void testAnyEmpty() {
        Assertions.assertTrue(N.anyEmpty("", "b"));
        Assertions.assertTrue(N.anyEmpty("a", ""));
        Assertions.assertTrue(N.anyEmpty(null, "b"));
        Assertions.assertTrue(N.anyEmpty("a", null));
        Assertions.assertFalse(N.anyEmpty("a", "b"));

        Assertions.assertTrue(N.anyEmpty("", "b", "c"));
        Assertions.assertTrue(N.anyEmpty("a", "", "c"));
        Assertions.assertTrue(N.anyEmpty("a", "b", ""));
        Assertions.assertTrue(N.anyEmpty(null, "b", "c"));
        Assertions.assertFalse(N.anyEmpty("a", "b", "c"));

        Assertions.assertFalse(N.anyEmpty((CharSequence[]) null));
        Assertions.assertFalse(N.anyEmpty(new CharSequence[0]));
        Assertions.assertTrue(N.anyEmpty("a", "", "c"));
        Assertions.assertFalse(N.anyEmpty("a", "b", "c"));

        Assertions.assertFalse(N.anyEmpty((Iterable<? extends CharSequence>) null));
        Assertions.assertFalse(N.anyEmpty(new ArrayList<>()));
        Assertions.assertTrue(N.anyEmpty(Arrays.asList("a", "", "c")));
        Assertions.assertFalse(N.anyEmpty(Arrays.asList("a", "b", "c")));

        Assertions.assertTrue(N.anyEmpty(new Object[0], new Object[] { "b" }));
        Assertions.assertTrue(N.anyEmpty(new Object[] { "a" }, new Object[0]));
        Assertions.assertTrue(N.anyEmpty((Object[]) null, new Object[] { "b" }));
        Assertions.assertFalse(N.anyEmpty(new Object[] { "a" }, new Object[] { "b" }));

        Assertions.assertTrue(N.anyEmpty(new Object[0], new Object[] { "b" }, new Object[] { "c" }));
        Assertions.assertTrue(N.anyEmpty(new Object[] { "a" }, new Object[0], new Object[] { "c" }));
        Assertions.assertTrue(N.anyEmpty(new Object[] { "a" }, new Object[] { "b" }, new Object[0]));
        Assertions.assertFalse(N.anyEmpty(new Object[] { "a" }, new Object[] { "b" }, new Object[] { "c" }));

        Assertions.assertTrue(N.anyEmpty(new ArrayList<>(), Arrays.asList("b")));
        Assertions.assertTrue(N.anyEmpty(Arrays.asList("a"), new ArrayList<>()));
        Assertions.assertTrue(N.anyEmpty((Collection<?>) null, Arrays.asList("b")));
        Assertions.assertFalse(N.anyEmpty(Arrays.asList("a"), Arrays.asList("b")));

        Assertions.assertTrue(N.anyEmpty(new ArrayList<>(), Arrays.asList("b"), Arrays.asList("c")));
        Assertions.assertTrue(N.anyEmpty(Arrays.asList("a"), new ArrayList<>(), Arrays.asList("c")));
        Assertions.assertTrue(N.anyEmpty(Arrays.asList("a"), Arrays.asList("b"), new ArrayList<>()));
        Assertions.assertFalse(N.anyEmpty(Arrays.asList("a"), Arrays.asList("b"), Arrays.asList("c")));
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
    public void testAnyBlank_TwoCharSequences() {
        assertTrue(N.anyBlank(null, "a"));
        assertTrue(N.anyBlank(" ", "a"));
        assertTrue(N.anyBlank("a", null));
        assertTrue(N.anyBlank("a", " "));
        assertFalse(N.anyBlank("a", "b"));
    }

    @Test
    public void testAnyBlank_ThreeCharSequences() {
        assertTrue(N.anyBlank(null, "a", "b"));
        assertTrue(N.anyBlank(" ", "a", "b"));
        assertTrue(N.anyBlank("a", " ", "b"));
        assertFalse(N.anyBlank("a", "b", "c"));
    }

    @Test
    public void testAnyBlank_CharSequenceVarArgs() {
        assertFalse(N.anyBlank((CharSequence[]) null));
        assertFalse(N.anyBlank());
        assertTrue(N.anyBlank((CharSequence) null));
        assertTrue(N.anyBlank(" "));
        assertTrue(N.anyBlank("a", " "));
        assertFalse(N.anyBlank("a", "b"));
    }

    @Test
    public void testAnyBlank_CharSequenceIterable() {
        assertFalse(N.anyBlank((Iterable<CharSequence>) null));
        assertFalse(N.anyBlank(Collections.emptyList()));
        assertTrue(N.anyBlank(Arrays.asList(null, "a")));
        assertTrue(N.anyBlank(Arrays.asList(" ", "a")));
        assertFalse(N.anyBlank(Arrays.asList("a", "b")));
    }

    @Test
    public void testAnyBlank_CharSequenceArray() {
        assertFalse(N.anyBlank((CharSequence[]) null));
        assertFalse(N.anyBlank(new CharSequence[0]));
        assertTrue(N.anyBlank((CharSequence) null));
        assertTrue(N.anyBlank(null, "foo"));
        assertTrue(N.anyBlank("", "bar"));
        assertTrue(N.anyBlank("bob", ""));
        assertTrue(N.anyBlank("  bob  ", null));
        assertTrue(N.anyBlank(" ", "bar"));
        assertFalse(N.anyBlank("foo", "bar"));
        assertFalse(N.anyBlank(new String[] {}));
        assertTrue(N.anyBlank(new String[] { "" }));
    }

    @Test
    public void testAnyBlank_Iterable() {
        assertFalse(N.anyBlank((Iterable<CharSequence>) null));
        assertFalse(N.anyBlank(new ArrayList<>()));

        List<CharSequence> list = new ArrayList<>();
        list.add("test");
        assertFalse(N.anyBlank(list));

        list.add(null);
        assertTrue(N.anyBlank(list));

        list.clear();
        list.add("");
        assertTrue(N.anyBlank(list));

        list.clear();
        list.add(" ");
        assertTrue(N.anyBlank(list));
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
    public void testAllNull_TwoObjects() {
        assertFalse(N.allNull(null, "a"));
        assertFalse(N.allNull("a", null));
        assertTrue(N.allNull(null, null));
        assertFalse(N.allNull("a", "b"));
    }

    @Test
    public void testAllNull_ThreeObjects() {
        assertFalse(N.allNull(null, "a", "b"));
        assertTrue(N.allNull(null, null, null));
        assertFalse(N.allNull("a", "b", "c"));
    }

    @Test
    public void testAllNull_Iterable() {
        assertTrue(N.allNull((Iterable<?>) null));
        assertTrue(N.allNull(Collections.emptyList()));
        assertFalse(N.allNull(Arrays.asList("a", "b")));
        assertFalse(N.allNull(Arrays.asList("a", null, "b")));
        assertTrue(N.allNull(Arrays.asList(null, null)));
        List<String> listWithNulls = new ArrayList<>();
        listWithNulls.add(null);
        listWithNulls.add(null);
        assertTrue(N.allNull(listWithNulls));
    }

    @Test
    public void testAllNull_ObjectArray() {
        assertTrue(N.allNull((Object[]) null));
        assertTrue(N.allNull(new Object[0]));
        assertTrue(N.allNull(new Object[] { null, null, null }));
        assertFalse(N.allNull(new Object[] { null, "test", null }));
        assertFalse(N.allNull(new Object[] { "test1", "test2" }));
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
    public void testAllEmpty_CharSequenceVarArgs() {
        assertTrue(N.allEmpty((CharSequence[]) null));
        assertTrue(N.allEmpty());
        assertTrue(N.allEmpty((CharSequence) null));
        assertTrue(N.allEmpty(""));
        assertTrue(N.allEmpty(null, ""));
        assertFalse(N.allEmpty(null, "a"));
    }

    @Test
    public void testAllEmpty_CharSequenceIterable() {
        assertTrue(N.allEmpty((Iterable<CharSequence>) null));
        assertTrue(N.allEmpty(Collections.emptyList()));
        assertTrue(N.allEmpty(Arrays.asList(null, "")));
        assertFalse(N.allEmpty(Arrays.asList(null, "a")));
    }

    @Test
    public void testAllEmpty_TwoObjectArrays() {
        assertTrue(N.allEmpty((Object[]) null, (Object[]) null));
        assertTrue(N.allEmpty(new Object[0], null));
        assertTrue(N.allEmpty(null, new Object[0]));
        assertTrue(N.allEmpty(new Object[0], new Object[0]));
        assertFalse(N.allEmpty(new Object[] { "a" }, new Object[0]));
    }

    @Test
    public void testAllEmpty_ThreeObjectArrays() {
        assertTrue(N.allEmpty((Object[]) null, (Object[]) null, (Object[]) null));
        assertTrue(N.allEmpty(new Object[0], null, new Object[0]));
        assertFalse(N.allEmpty(new Object[] { "a" }, new Object[0], null));
    }

    @Test
    public void testAllEmpty_TwoCollections() {
        assertTrue(N.allEmpty((List) null, (List) null));
        assertTrue(N.allEmpty(Collections.emptyList(), null));
        assertTrue(N.allEmpty(null, Collections.emptyList()));
        assertTrue(N.allEmpty(Collections.emptyList(), Collections.emptyList()));
        assertFalse(N.allEmpty(Arrays.asList("a"), Collections.emptyList()));
    }

    @Test
    public void testAllEmpty_ThreeCollections() {
        assertTrue(N.allEmpty((List) null, (List) null, (List) null));
        assertTrue(N.allEmpty(Collections.emptyList(), null, Collections.emptyList()));
        assertFalse(N.allEmpty(Arrays.asList("a"), Collections.emptyList(), null));
    }

    @Test
    public void testAllEmpty_TwoMaps() {
        assertTrue(N.allEmpty((Map) null, (Map) null));
        assertTrue(N.allEmpty(Collections.emptyMap(), null));
        assertTrue(N.allEmpty(null, Collections.emptyMap()));
        assertTrue(N.allEmpty(Collections.emptyMap(), Collections.emptyMap()));
        Map<String, String> nonEmptyMap = new HashMap<>();
        nonEmptyMap.put("k", "v");
        assertFalse(N.allEmpty(nonEmptyMap, Collections.emptyMap()));
    }

    @Test
    public void testAllEmpty_ThreeMaps() {
        assertTrue(N.allEmpty((Map) null, (Map) null, (Map) null));
        assertTrue(N.allEmpty(Collections.emptyMap(), null, Collections.emptyMap()));
        Map<String, String> nonEmptyMap = new HashMap<>();
        nonEmptyMap.put("k", "v");
        assertFalse(N.allEmpty(nonEmptyMap, Collections.emptyMap(), null));
    }

    @Test
    public void testAllEmpty_CharSequenceArray() {
        assertTrue(N.allEmpty((CharSequence[]) null));
        assertTrue(N.allEmpty(new CharSequence[0]));
        assertTrue(N.allEmpty(null, ""));
        assertFalse(N.allEmpty(null, "foo"));
        assertFalse(N.allEmpty("", "bar"));
        assertFalse(N.allEmpty("bob", ""));
        assertFalse(N.allEmpty("  bob  ", null));
        assertFalse(N.allEmpty(" ", "bar"));
        assertFalse(N.allEmpty("foo", "bar"));
    }

    // ========== allEmpty / allBlank early false return ==========

    @Test
    public void testAllEmpty_Varargs_WithNonEmptyElement_ReturnsFalse() {
        // L8339: notEmpty(cs) -> return false
        assertFalse(N.allEmpty("", "nonEmpty", ""));
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
    public void testAllBlank_TwoCharSequences() {
        assertFalse(N.allBlank(null, "a"));
        assertTrue(N.allBlank(null, " "));
        assertTrue(N.allBlank(null, null));
        assertTrue(N.allBlank(" ", " "));
        assertFalse(N.allBlank("a", " "));
    }

    @Test
    public void testAllBlank_ThreeCharSequences() {
        assertFalse(N.allBlank(null, " ", "a"));
        assertTrue(N.allBlank(null, " ", null));
        assertFalse(N.allBlank("a", "b", "c"));
    }

    @Test
    public void testAllBlank_CharSequenceVarArgs() {
        assertTrue(N.allBlank((CharSequence[]) null));
        assertTrue(N.allBlank());
        assertTrue(N.allBlank((CharSequence) null));
        assertTrue(N.allBlank(" "));
        assertTrue(N.allBlank(null, " ", "\t"));
        assertFalse(N.allBlank(null, "a"));
    }

    @Test
    public void testAllBlank_CharSequenceIterable() {
        assertTrue(N.allBlank((Iterable<CharSequence>) null));
        assertTrue(N.allBlank(Collections.emptyList()));
        assertTrue(N.allBlank(Arrays.asList(null, " ", "\t")));
        assertFalse(N.allBlank(Arrays.asList(null, "a")));
    }

    @Test
    public void testAllBlank_CharSequenceArray() {
        assertTrue(N.allBlank((CharSequence[]) null));
        assertTrue(N.allBlank(new CharSequence[0]));
        assertTrue(N.allBlank(null, null));
        assertTrue(N.allBlank("", " "));
        assertFalse(N.allBlank(null, "foo"));
        assertFalse(N.allBlank("", "bar"));
        assertFalse(N.allBlank("bob", ""));
        assertFalse(N.allBlank("  bob  ", null));
        assertFalse(N.allBlank(" ", "bar"));
        assertFalse(N.allBlank("foo", "bar"));
    }

    @Test
    public void testAllBlank_Varargs_WithNonBlankElement_ReturnsFalse() {
        // L8544: notBlank(cs) -> return false
        assertFalse(N.allBlank("  ", "nonBlank", "  "));
    }

    @Test
    public void testNullToEmpty() {
        String[] arr = null;
        String[] result = N.nullToEmpty(arr);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testNullToEmpty_booleanArray() {
        assertArrayEquals(N.EMPTY_BOOLEAN_ARRAY, N.nullToEmpty((boolean[]) null));
        boolean[] arr = { true, false };
        assertSame(arr, N.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_charArray() {
        assertArrayEquals(N.EMPTY_CHAR_ARRAY, N.nullToEmpty((char[]) null));
        char[] arr = { 'a', 'b' };
        assertSame(arr, N.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_byteArray() {
        assertArrayEquals(N.EMPTY_BYTE_ARRAY, N.nullToEmpty((byte[]) null));
        byte[] arr = { 1, 2 };
        assertSame(arr, N.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_shortArray() {
        assertArrayEquals(N.EMPTY_SHORT_ARRAY, N.nullToEmpty((short[]) null));
        short[] arr = { 1, 2 };
        assertSame(arr, N.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_intArray() {
        assertArrayEquals(N.EMPTY_INT_ARRAY, N.nullToEmpty((int[]) null));
        int[] arr = { 1, 2 };
        assertSame(arr, N.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_longArray() {
        assertArrayEquals(N.EMPTY_LONG_ARRAY, N.nullToEmpty((long[]) null));
        long[] arr = { 1L, 2L };
        assertSame(arr, N.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_floatArray() {
        assertArrayEquals(N.EMPTY_FLOAT_ARRAY, N.nullToEmpty((float[]) null), 0.0f);
        float[] arr = { 1.0f, 2.0f };
        assertSame(arr, N.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_doubleArray() {
        assertArrayEquals(N.EMPTY_DOUBLE_ARRAY, N.nullToEmpty((double[]) null), 0.0);
        double[] arr = { 1.0, 2.0 };
        assertSame(arr, N.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_BigIntegerArray() {
        assertArrayEquals(N.EMPTY_BIG_INTEGER_ARRAY, N.nullToEmpty((BigInteger[]) null));
        BigInteger[] arr = { BigInteger.ONE };
        assertSame(arr, N.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_BigDecimalArray() {
        assertArrayEquals(N.EMPTY_BIG_DECIMAL_ARRAY, N.nullToEmpty((BigDecimal[]) null));
        BigDecimal[] arr = { BigDecimal.ONE };
        assertSame(arr, N.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_StringArray() {
        assertArrayEquals(N.EMPTY_STRING_ARRAY, N.nullToEmpty((String[]) null));
        String[] arr = { "a", "b" };
        assertSame(arr, N.nullToEmpty(arr));
        String[] arrWithNull = { "a", null };
        String[] resultArrWithNull = N.nullToEmpty(arrWithNull);
        assertSame(arrWithNull, resultArrWithNull);
        assertNull(resultArrWithNull[1]);
    }

    @Test
    public void testNullToEmpty_JavaUtilDateArray() {
        assertArrayEquals(N.EMPTY_JU_DATE_ARRAY, N.nullToEmpty((java.util.Date[]) null));
        java.util.Date[] arr = { new java.util.Date() };
        assertSame(arr, N.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_JavaSqlDateArray() {
        assertArrayEquals(N.EMPTY_DATE_ARRAY, N.nullToEmpty((java.sql.Date[]) null));
        java.sql.Date[] arr = { new java.sql.Date(System.currentTimeMillis()) };
        assertSame(arr, N.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_JavaSqlTimeArray() {
        assertArrayEquals(N.EMPTY_TIME_ARRAY, N.nullToEmpty((java.sql.Time[]) null));
        java.sql.Time[] arr = { new java.sql.Time(System.currentTimeMillis()) };
        assertSame(arr, N.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_JavaSqlTimestampArray() {
        assertArrayEquals(N.EMPTY_TIMESTAMP_ARRAY, N.nullToEmpty((java.sql.Timestamp[]) null));
        java.sql.Timestamp[] arr = { new java.sql.Timestamp(System.currentTimeMillis()) };
        assertSame(arr, N.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_CalendarArray() {
        assertArrayEquals(N.EMPTY_CALENDAR_ARRAY, N.nullToEmpty((Calendar[]) null));
        Calendar[] arr = { Calendar.getInstance() };
        assertSame(arr, N.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_ObjectArray() {
        assertArrayEquals(N.EMPTY_OBJECT_ARRAY, N.nullToEmpty((Object[]) null));
        Object[] arr = { new Object(), "string" };
        assertSame(arr, N.nullToEmpty(arr));
    }

    @Test
    public void testNullToEmpty_ImmutableCollection() {
        com.landawn.abacus.util.ImmutableList<String> nonNullList = com.landawn.abacus.util.ImmutableList.of("a");
        assertSame(nonNullList, N.nullToEmpty(nonNullList));

        com.landawn.abacus.util.ImmutableCollection<?> emptyCol = N.nullToEmpty((com.landawn.abacus.util.ImmutableCollection<?>) null);
        assertTrue(emptyCol.isEmpty());
    }

    @Test
    public void testNullToEmpty_ImmutableList() {
        com.landawn.abacus.util.ImmutableList<String> nonNullList = com.landawn.abacus.util.ImmutableList.of("a");
        assertSame(nonNullList, N.nullToEmpty(nonNullList));
        com.landawn.abacus.util.ImmutableList<?> emptyList = N.nullToEmpty((com.landawn.abacus.util.ImmutableList<?>) null);
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testNullToEmpty_ImmutableSet() {
        com.landawn.abacus.util.ImmutableSet<String> nonNullSet = com.landawn.abacus.util.ImmutableSet.of("a");
        assertSame(nonNullSet, N.nullToEmpty(nonNullSet));
        com.landawn.abacus.util.ImmutableSet<?> emptySet = N.nullToEmpty((com.landawn.abacus.util.ImmutableSet<?>) null);
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testNullToEmpty_ImmutableSortedSet() {
        com.landawn.abacus.util.ImmutableSortedSet<String> nonNullSet = com.landawn.abacus.util.ImmutableSortedSet.of("a");
        assertSame(nonNullSet, N.nullToEmpty(nonNullSet));
        com.landawn.abacus.util.ImmutableSortedSet<?> emptySet = N.nullToEmpty((com.landawn.abacus.util.ImmutableSortedSet<?>) null);
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testNullToEmpty_ImmutableNavigableSet() {
        com.landawn.abacus.util.ImmutableNavigableSet<String> nonNullSet = com.landawn.abacus.util.ImmutableNavigableSet.of("a");
        assertSame(nonNullSet, N.nullToEmpty(nonNullSet));
        com.landawn.abacus.util.ImmutableNavigableSet<?> emptySet = N.nullToEmpty((com.landawn.abacus.util.ImmutableNavigableSet<?>) null);
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testNullToEmpty_ImmutableMap() {
        com.landawn.abacus.util.ImmutableMap<String, String> nonNullMap = com.landawn.abacus.util.ImmutableMap.of("a", "b");
        assertSame(nonNullMap, N.nullToEmpty(nonNullMap));
        com.landawn.abacus.util.ImmutableMap<?, ?> emptyMap = N.nullToEmpty((com.landawn.abacus.util.ImmutableMap<?, ?>) null);
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testNullToEmpty_ImmutableSortedMap() {
        com.landawn.abacus.util.ImmutableSortedMap<String, String> nonNullMap = com.landawn.abacus.util.ImmutableSortedMap.of("a", "b");
        assertSame(nonNullMap, N.nullToEmpty(nonNullMap));
        com.landawn.abacus.util.ImmutableSortedMap<?, ?> emptyMap = N.nullToEmpty((com.landawn.abacus.util.ImmutableSortedMap<?, ?>) null);
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testNullToEmpty_ImmutableNavigableMap() {
        com.landawn.abacus.util.ImmutableNavigableMap<String, String> nonNullMap = com.landawn.abacus.util.ImmutableNavigableMap.of("a", "b");
        assertSame(nonNullMap, N.nullToEmpty(nonNullMap));
        com.landawn.abacus.util.ImmutableNavigableMap<?, ?> emptyMap = N.nullToEmpty((com.landawn.abacus.util.ImmutableNavigableMap<?, ?>) null);
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testNullToEmpty_ImmutableBiMap() {
        com.landawn.abacus.util.ImmutableBiMap<String, String> nonNullMap = com.landawn.abacus.util.ImmutableBiMap.of("a", "b");
        assertSame(nonNullMap, N.nullToEmpty(nonNullMap));
        com.landawn.abacus.util.ImmutableBiMap<?, ?> emptyMap = N.nullToEmpty((com.landawn.abacus.util.ImmutableBiMap<?, ?>) null);
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testNullToEmpty_PrimitiveArrays() {
        assertArrayEquals(new boolean[0], N.nullToEmpty((boolean[]) null));
        assertArrayEquals(new char[0], N.nullToEmpty((char[]) null));
        assertArrayEquals(new byte[0], N.nullToEmpty((byte[]) null));
        assertArrayEquals(new short[0], N.nullToEmpty((short[]) null));
        assertArrayEquals(new int[0], N.nullToEmpty((int[]) null));
        assertArrayEquals(new long[0], N.nullToEmpty((long[]) null));
        assertArrayEquals(new float[0], N.nullToEmpty((float[]) null), 0.0f);
        assertArrayEquals(new double[0], N.nullToEmpty((double[]) null), 0.0);

        boolean[] boolArray = { true, false };
        assertSame(boolArray, N.nullToEmpty(boolArray));
    }

    @Test
    public void testNullToEmpty_ObjectArrays() {
        assertArrayEquals(new BigInteger[0], N.nullToEmpty((BigInteger[]) null));
        assertArrayEquals(new BigDecimal[0], N.nullToEmpty((BigDecimal[]) null));
        assertArrayEquals(new String[0], N.nullToEmpty((String[]) null));
        assertArrayEquals(new java.util.Date[0], N.nullToEmpty((java.util.Date[]) null));
        assertArrayEquals(new java.sql.Date[0], N.nullToEmpty((java.sql.Date[]) null));
        assertArrayEquals(new java.sql.Time[0], N.nullToEmpty((java.sql.Time[]) null));
        assertArrayEquals(new java.sql.Timestamp[0], N.nullToEmpty((java.sql.Timestamp[]) null));
        assertArrayEquals(new Calendar[0], N.nullToEmpty((Calendar[]) null));
        assertArrayEquals(new Object[0], N.nullToEmpty((Object[]) null));

        String[] strArray = { "test1", "test2" };
        assertSame(strArray, N.nullToEmpty(strArray));
    }

    @Test
    public void testNullToEmpty_ImmutableCollections() {
        ImmutableCollection<String> nullColl = null;
        ImmutableCollection<String> emptyColl = N.nullToEmpty(nullColl);
        assertNotNull(emptyColl);
        assertTrue(emptyColl.isEmpty());

        ImmutableList<String> nullList = null;
        ImmutableList<String> emptyList = N.nullToEmpty(nullList);
        assertNotNull(emptyList);
        assertTrue(emptyList.isEmpty());

        ImmutableSet<String> nullSet = null;
        ImmutableSet<String> emptySet = N.nullToEmpty(nullSet);
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());

        ImmutableSortedSet<String> nullSortedSet = null;
        ImmutableSortedSet<String> emptySortedSet = N.nullToEmpty(nullSortedSet);
        assertNotNull(emptySortedSet);
        assertTrue(emptySortedSet.isEmpty());

        ImmutableNavigableSet<String> nullNavSet = null;
        ImmutableNavigableSet<String> emptyNavSet = N.nullToEmpty(nullNavSet);
        assertNotNull(emptyNavSet);
        assertTrue(emptyNavSet.isEmpty());

        ImmutableMap<String, String> nullMap = null;
        ImmutableMap<String, String> emptyMap = N.nullToEmpty(nullMap);
        assertNotNull(emptyMap);
        assertTrue(emptyMap.isEmpty());

        ImmutableSortedMap<String, String> nullSortedMap = null;
        ImmutableSortedMap<String, String> emptySortedMap = N.nullToEmpty(nullSortedMap);
        assertNotNull(emptySortedMap);
        assertTrue(emptySortedMap.isEmpty());

        ImmutableNavigableMap<String, String> nullNavMap = null;
        ImmutableNavigableMap<String, String> emptyNavMap = N.nullToEmpty(nullNavMap);
        assertNotNull(emptyNavMap);
        assertTrue(emptyNavMap.isEmpty());

        ImmutableBiMap<String, String> nullBiMap = null;
        ImmutableBiMap<String, String> emptyBiMap = N.nullToEmpty(nullBiMap);
        assertNotNull(emptyBiMap);
        assertTrue(emptyBiMap.isEmpty());
    }

    // --- Tests for nullToEmpty with typed array ---

    @Test
    public void testNullToEmpty_TypedArray() {
        Integer[] nullArr = null;
        Integer[] result = N.nullToEmpty(nullArr, Integer[].class);
        assertNotNull(result);
        assertEquals(0, result.length);

        Integer[] nonNull = { 1, 2 };
        assertSame(nonNull, N.nullToEmpty(nonNull, Integer[].class));
    }

    @Test
    public void testNullToEmpty_List() {
        List<String> nonNullList = new ArrayList<>();
        nonNullList.add("a");
        assertSame(nonNullList, N.nullToEmpty(nonNullList));

        List<?> emptyList = N.nullToEmpty((List<?>) null);
        assertTrue(emptyList.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyList.add(null));
    }

    @Test
    public void testNullToEmpty_Set() {
        Set<String> nonNullSet = new HashSet<>();
        nonNullSet.add("a");
        assertSame(nonNullSet, N.nullToEmpty(nonNullSet));

        Set<?> emptySet = N.nullToEmpty((Set<?>) null);
        assertTrue(emptySet.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptySet.add(null));
    }

    @Test
    public void testNullToEmpty_SortedSet() {
        SortedSet<String> nonNullSet = new TreeSet<>();
        nonNullSet.add("a");
        assertSame(nonNullSet, N.nullToEmpty(nonNullSet));
        SortedSet<?> emptySet = N.nullToEmpty((SortedSet<?>) null);
        assertTrue(emptySet.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptySet.add(null));
    }

    @Test
    public void testNullToEmpty_NavigableSet() {
        NavigableSet<String> nonNullSet = new TreeSet<>();
        nonNullSet.add("a");
        assertSame(nonNullSet, N.nullToEmpty(nonNullSet));
        NavigableSet<?> emptySet = N.nullToEmpty((NavigableSet<?>) null);
        assertTrue(emptySet.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptySet.add(null));
    }

    @Test
    public void testNullToEmpty_Collection() {
        Collection<String> nonNullCollection = new ArrayList<>();
        nonNullCollection.add("a");
        assertSame(nonNullCollection, N.nullToEmpty(nonNullCollection));
        Collection<?> emptyCollection = N.nullToEmpty((Collection<?>) null);
        assertTrue(emptyCollection.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyCollection.add(null));
    }

    @Test
    public void testNullToEmpty_Map() {
        Map<String, String> nonNullMap = new HashMap<>();
        nonNullMap.put("a", "b");
        assertSame(nonNullMap, N.nullToEmpty(nonNullMap));
        Map<?, ?> emptyMap = N.nullToEmpty((Map<?, ?>) null);
        assertTrue(emptyMap.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyMap.put(null, null));
    }

    @Test
    public void testNullToEmpty_SortedMap() {
        SortedMap<String, String> nonNullMap = new TreeMap<>();
        nonNullMap.put("a", "b");
        assertSame(nonNullMap, N.nullToEmpty(nonNullMap));
        SortedMap<?, ?> emptyMap = N.nullToEmpty((SortedMap<?, ?>) null);
        assertTrue(emptyMap.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyMap.put(null, null));
    }

    @Test
    public void testNullToEmpty_NavigableMap() {
        NavigableMap<String, String> nonNullMap = new TreeMap<>();
        nonNullMap.put("a", "b");
        assertSame(nonNullMap, N.nullToEmpty(nonNullMap));
        NavigableMap<?, ?> emptyMap = N.nullToEmpty((NavigableMap<?, ?>) null);
        assertTrue(emptyMap.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyMap.put(null, null));
    }

    @Test
    public void testNullToEmpty_Iterator() {
        List<String> list = Arrays.asList("a");
        Iterator<String> nonEmptyIterator = list.iterator();
        assertSame(nonEmptyIterator, N.nullToEmpty(nonEmptyIterator));

        Iterator<?> emptyIter = N.nullToEmpty((Iterator<?>) null);
        assertFalse(emptyIter.hasNext());
        assertThrows(NoSuchElementException.class, emptyIter::next);
    }

    @Test
    public void testNullToEmpty_ListIterator() {
        List<String> list = Arrays.asList("a");
        ListIterator<String> nonEmptyListIterator = list.listIterator();
        assertSame(nonEmptyListIterator, N.nullToEmpty(nonEmptyListIterator));

        ListIterator<?> emptyIter = N.nullToEmpty((ListIterator<?>) null);
        assertFalse(emptyIter.hasNext());
        assertFalse(emptyIter.hasPrevious());
        assertThrows(NoSuchElementException.class, emptyIter::next);
        assertThrows(NoSuchElementException.class, emptyIter::previous);
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
    public void testDefaultIfNull_WithSupplier() {
        Supplier<String> supplier = () -> "default";
        assertEquals("default", N.defaultIfNull((String) null, supplier));
        assertEquals("test", N.defaultIfNull("test", supplier));
    }

    @Test
    public void testDefaultIfNull_Boolean_noDefaultArg() {
        assertFalse(N.defaultIfNull((Boolean) null));
        assertTrue(N.defaultIfNull(Boolean.TRUE));
        assertFalse(N.defaultIfNull(Boolean.FALSE));
    }

    @Test
    public void testDefaultIfNull_Boolean_withDefaultArg() {
        assertTrue(N.defaultIfNull(null, true));
        assertFalse(N.defaultIfNull(null, false));
        assertTrue(N.defaultIfNull(Boolean.TRUE, false));
        assertFalse(N.defaultIfNull(Boolean.FALSE, true));
    }

    @Test
    public void testDefaultIfNull_Double_noDefaultArg() {
        assertEquals(0.0, N.defaultIfNull((Double) null), 0.0);
        assertEquals(10.0, N.defaultIfNull(Double.valueOf(10.0)), 0.0);
    }

    @Test
    public void testDefaultIfNull_Double_withDefaultArg() {
        assertEquals(5.0, N.defaultIfNull(null, 5.0), 0.0);
        assertEquals(10.0, N.defaultIfNull(Double.valueOf(10.0), 5.0), 0.0);
    }

    @Test
    public void testDefaultIfNull_Primitives() {
        assertEquals(false, N.defaultIfNull((Boolean) null));
        assertEquals(true, N.defaultIfNull(Boolean.TRUE));
        assertEquals(true, N.defaultIfNull((Boolean) null, true));
        assertEquals(false, N.defaultIfNull(Boolean.FALSE, true));

        assertEquals('\0', N.defaultIfNull((Character) null));
        assertEquals('a', N.defaultIfNull(Character.valueOf('a')));
        assertEquals('b', N.defaultIfNull((Character) null, 'b'));
        assertEquals('a', N.defaultIfNull(Character.valueOf('a'), 'b'));

        assertEquals((byte) 0, N.defaultIfNull((Byte) null));
        assertEquals((byte) 5, N.defaultIfNull(Byte.valueOf((byte) 5)));
        assertEquals((byte) 10, N.defaultIfNull((Byte) null, (byte) 10));
        assertEquals((byte) 5, N.defaultIfNull(Byte.valueOf((byte) 5), (byte) 10));

        assertEquals((short) 0, N.defaultIfNull((Short) null));
        assertEquals((short) 5, N.defaultIfNull(Short.valueOf((short) 5)));
        assertEquals((short) 10, N.defaultIfNull((Short) null, (short) 10));
        assertEquals((short) 5, N.defaultIfNull(Short.valueOf((short) 5), (short) 10));

        assertEquals(0, N.defaultIfNull((Integer) null));
        assertEquals(5, N.defaultIfNull(Integer.valueOf(5)));
        assertEquals(10, N.defaultIfNull((Integer) null, 10));
        assertEquals(5, N.defaultIfNull(Integer.valueOf(5), 10));

        assertEquals(0L, N.defaultIfNull((Long) null));
        assertEquals(5L, N.defaultIfNull(Long.valueOf(5L)));
        assertEquals(10L, N.defaultIfNull((Long) null, 10L));
        assertEquals(5L, N.defaultIfNull(Long.valueOf(5L), 10L));

        assertEquals(0f, N.defaultIfNull((Float) null), 0.0f);
        assertEquals(5f, N.defaultIfNull(Float.valueOf(5f)), 0.0f);
        assertEquals(10f, N.defaultIfNull((Float) null, 10f), 0.0f);
        assertEquals(5f, N.defaultIfNull(Float.valueOf(5f), 10f), 0.0f);

        assertEquals(0d, N.defaultIfNull((Double) null), 0.0);
        assertEquals(5d, N.defaultIfNull(Double.valueOf(5d)), 0.0);
        assertEquals(10d, N.defaultIfNull((Double) null, 10d), 0.0);
        assertEquals(5d, N.defaultIfNull(Double.valueOf(5d), 10d), 0.0);
    }

    @Test
    public void testDefaultIfNull_Object() {
        assertEquals("default", N.defaultIfNull((String) null, "default"));
        assertEquals("test", N.defaultIfNull("test", "default"));
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfNull(null, (String) null));
    }

    @Test
    public void testDefaultIfNull_Generic_withDefaultArg() {
        String defaultStr = "default";
        assertEquals(defaultStr, N.defaultIfNull(null, defaultStr));
        String actualStr = "actual";
        assertEquals(actualStr, N.defaultIfNull(actualStr, defaultStr));

        assertThrows(IllegalArgumentException.class, () -> N.defaultIfNull("any", (String) null));
    }

    @Test
    public void testDefaultIfNull_Generic_withSupplier() {
        String defaultStr = "default";
        Supplier<String> supplier = () -> defaultStr;
        assertEquals(defaultStr, N.defaultIfNull(null, supplier));

        String actualStr = "actual";
        assertEquals(actualStr, N.defaultIfNull(actualStr, supplier));

        Supplier<String> nullSupplier = () -> null;
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfNull(null, nullSupplier));
    }

    @Test
    public void testDefaultIfNull_Supplier() {
        String nullStr = null;
        assertEquals("default", N.defaultIfNull(nullStr, Fn.s(() -> "default")));
        assertEquals("test", N.defaultIfNull("test", Fn.s(() -> "default")));

        assertThrows(IllegalArgumentException.class, () -> N.defaultIfNull(nullStr, () -> null));
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
    public void testDefaultIfEmpty_CharSequence_withDefaultArg() {
        String defaultStr = "default";
        assertEquals(defaultStr, N.defaultIfEmpty(null, defaultStr));
        assertEquals(defaultStr, N.defaultIfEmpty("", defaultStr));
        assertEquals("actual", N.defaultIfEmpty("actual", defaultStr));

        assertThrows(IllegalArgumentException.class, () -> N.defaultIfEmpty("any", ""));
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfEmpty("any", (String) null));

        StringBuilder sbNull = null;
        StringBuilder sbEmpty = new StringBuilder();
        StringBuilder sbVal = new StringBuilder("val");
        StringBuilder defaultSb = new StringBuilder("defaultSb");

        assertSame(defaultSb, N.defaultIfEmpty(sbNull, defaultSb));
        assertSame(defaultSb, N.defaultIfEmpty(sbEmpty, defaultSb));
        assertSame(sbVal, N.defaultIfEmpty(sbVal, defaultSb));
    }

    @Test
    public void testDefaultIfEmpty_CharSequence_withSupplier() {
        String defaultStr = "default";
        Supplier<String> supplier = () -> defaultStr;
        assertEquals(defaultStr, N.defaultIfEmpty(null, supplier));
        assertEquals(defaultStr, N.defaultIfEmpty("", supplier));
        assertEquals("actual", N.defaultIfEmpty("actual", supplier));

        Supplier<String> emptySupplier = () -> "";
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfEmpty("", emptySupplier));
        Supplier<String> nullSupplier = () -> null;
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfEmpty("", nullSupplier));
    }

    @Test
    public void testDefaultIfEmpty_CharSequenceSupplier() {
        assertEquals("default", N.defaultIfEmpty("", Fn.s(Fn.s(() -> "default"))));
        assertEquals("default", N.defaultIfEmpty((String) null, Fn.s(() -> "default")));
        assertEquals("test", N.defaultIfEmpty("test", Fn.s(() -> "default")));

        assertThrows(IllegalArgumentException.class, () -> N.defaultIfEmpty("", (Supplier<? extends String>) () -> ""));
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfEmpty("", (Supplier<? extends String>) () -> null));
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
    public void testDefaultIfBlank_CharSequence_withDefaultArg() {
        String defaultStr = "default";
        assertEquals(defaultStr, N.defaultIfBlank(null, defaultStr));
        assertEquals(defaultStr, N.defaultIfBlank("", defaultStr));
        assertEquals(defaultStr, N.defaultIfBlank("   ", defaultStr));
        assertEquals("actual", N.defaultIfBlank("actual", defaultStr));
        assertEquals("  actual  ", N.defaultIfBlank("  actual  ", defaultStr));

        assertThrows(IllegalArgumentException.class, () -> N.defaultIfBlank("any", ""));
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfBlank("any", "   "));
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfBlank("any", (String) null));
    }

    @Test
    public void testDefaultIfBlank_CharSequence_withSupplier() {
        String defaultStr = "default";
        Supplier<String> supplier = () -> defaultStr;
        assertEquals(defaultStr, N.defaultIfBlank(null, supplier));
        assertEquals(defaultStr, N.defaultIfBlank("", supplier));
        assertEquals(defaultStr, N.defaultIfBlank("   ", supplier));
        assertEquals("actual", N.defaultIfBlank("actual", supplier));

        Supplier<String> blankSupplier = () -> "   ";
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfBlank("   ", blankSupplier));
        Supplier<String> nullSupplier = () -> null;
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfBlank("   ", nullSupplier));
    }

    @Test
    public void testDefaultIfBlank_CharSequence() {
        assertEquals("default", N.defaultIfBlank("", "default"));
        assertEquals("default", N.defaultIfBlank(" ", "default"));
        assertEquals("default", N.defaultIfBlank(null, "default"));
        assertEquals("test", N.defaultIfBlank("test", "default"));

        assertThrows(IllegalArgumentException.class, () -> N.defaultIfBlank(" ", " "));
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfBlank(" ", (String) null));
    }

    @Test
    public void testDefaultIfBlank_CharSequenceSupplier() {
        assertEquals("default", N.defaultIfBlank("", Fn.s(() -> "default")));
        assertEquals("default", N.defaultIfBlank(" ", Fn.s(() -> "default")));
        assertEquals("default", N.defaultIfBlank(null, Fn.s(() -> "default")));
        assertEquals("test", N.defaultIfBlank("test", Fn.s(() -> "default")));

        assertThrows(IllegalArgumentException.class, () -> N.defaultIfBlank(" ", (Supplier<? extends String>) () -> " "));
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfBlank(" ", (Supplier<? extends String>) () -> null));
    }

    @Test
    public void testDefaultValueOf() {
        int result = N.defaultValueOf(int.class);
        assertEquals(0, result);
    }

    // --- Tests for defaultValueOf with nonNullForPrimitiveWrapper ---

    @Test
    public void testDefaultValueOf_nonNullForPrimitiveWrapper() {
        // With nonNullForPrimitiveWrapper = true, wrapper types should return non-null defaults
        Integer intDefault = N.defaultValueOf(Integer.class, true);
        assertNotNull(intDefault);
        assertEquals(0, (int) intDefault);

        Boolean boolDefault = N.defaultValueOf(Boolean.class, true);
        assertNotNull(boolDefault);
        assertFalse(boolDefault);

        // With nonNullForPrimitiveWrapper = false, wrapper types should return null
        assertNull(N.defaultValueOf(Integer.class, false));
    }

    @Test
    public void testDefaultValueOf_WithNonNullForPrimitiveWrapper() {
        assertEquals(false, N.defaultValueOf(Boolean.class, true));
        assertEquals(null, N.defaultValueOf(Boolean.class, false));
        assertEquals('\0', N.defaultValueOf(Character.class, true));
        assertEquals(null, N.defaultValueOf(Character.class, false));
        assertEquals((byte) 0, N.defaultValueOf(Byte.class, true));
        assertEquals(null, N.defaultValueOf(Byte.class, false));
        assertEquals((short) 0, N.defaultValueOf(Short.class, true));
        assertEquals(null, N.defaultValueOf(Short.class, false));
        assertEquals(0, N.defaultValueOf(Integer.class, true));
        assertEquals(null, N.defaultValueOf(Integer.class, false));
        assertEquals(0L, N.defaultValueOf(Long.class, true));
        assertEquals(null, N.defaultValueOf(Long.class, false));
        assertEquals(0f, N.defaultValueOf(Float.class, true), 0.0f);
        assertEquals(null, N.defaultValueOf(Float.class, false));
        assertEquals(0d, N.defaultValueOf(Double.class, true), 0.0);
        assertEquals(null, N.defaultValueOf(Double.class, false));

        assertThrows(IllegalArgumentException.class, () -> N.defaultValueOf(null, true));
    }

    @Test
    public void testTypeOf() {
        com.landawn.abacus.type.Type<String> type = N.typeOf(String.class);
        assertNotNull(type);
        assertEquals(String.class, type.javaType());
    }

    @Test
    public void testTypeOf_String() {
        Type<String> stringType = N.typeOf("java.lang.String");
        assertNotNull(stringType);
        assertEquals(String.class, stringType.javaType());

        Type<Integer> intType = N.typeOf("int");
        assertNotNull(intType);
        assertEquals(int.class, intType.javaType());

        assertThrows(IllegalArgumentException.class, () -> N.typeOf((String) null));
    }

    @Test
    public void testTypeOf_Class() {
        Type<String> stringType = N.typeOf(String.class);
        assertNotNull(stringType);
        assertEquals(String.class, stringType.javaType());

        Type<Integer> intType = N.typeOf(int.class);
        assertNotNull(intType);
        assertEquals(int.class, intType.javaType());

        assertThrows(IllegalArgumentException.class, () -> N.typeOf((Class<?>) null));
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
    public void testStringOf_Primitives() {
        assertEquals("true", N.stringOf(true));
        assertEquals("false", N.stringOf(false));

        assertEquals("a", N.stringOf('a'));
        assertEquals("A", N.stringOf('A'));
        assertEquals("0", N.stringOf('0'));

        assertEquals("0", N.stringOf((byte) 0));
        assertEquals("127", N.stringOf((byte) 127));
        assertEquals("-128", N.stringOf((byte) -128));

        assertEquals("0", N.stringOf((short) 0));
        assertEquals("32767", N.stringOf(Short.MAX_VALUE));
        assertEquals("-32768", N.stringOf(Short.MIN_VALUE));

        assertEquals("0", N.stringOf(0));
        assertEquals("2147483647", N.stringOf(Integer.MAX_VALUE));
        assertEquals("-2147483648", N.stringOf(Integer.MIN_VALUE));

        assertEquals("0", N.stringOf(0L));
        assertEquals("9223372036854775807", N.stringOf(Long.MAX_VALUE));
        assertEquals("-9223372036854775808", N.stringOf(Long.MIN_VALUE));

        assertEquals("0.0", N.stringOf(0.0f));
        assertEquals("3.14", N.stringOf(3.14f));
        assertEquals("-3.14", N.stringOf(-3.14f));

        assertEquals("0.0", N.stringOf(0.0d));
        assertEquals("3.14159", N.stringOf(3.14159d));
        assertEquals("-3.14159", N.stringOf(-3.14159d));
    }

    @Test
    public void testValueOf() {
        Integer result = N.valueOf("123", Integer.class);
        assertEquals(Integer.valueOf(123), result);
    }

    @Test
    public void testRegisterConverter() {
    }

    @Test
    public void testIsBuiltinClass_2DPrimitiveArray() {
        // 2D primitive array: no package, is array, component is array, inner is primitive
        assertTrue(N.isBuiltinClass(int[][].class));
    }

    @Test
    public void testIsBuiltinClass_CustomClass() {
        // java.util.ArrayList is a built-in class (java.* package)
        assertTrue(N.isBuiltinClass(java.util.ArrayList.class));
        // org.junit class is not in java.* or com.landawn.abacus.* so not builtin
        assertFalse(N.isBuiltinClass(org.junit.jupiter.api.Test.class));
    }

    // ========== isBuiltinClass uncovered branches ==========

    @Test
    public void testIsBuiltinClass_PrimitiveArray() {
        // primitive array type has null package; isPrimitiveArrayType returns true
        assertTrue(N.isBuiltinClass(int[].class));
    }

    @Test
    public void testConvert() {
        String result = N.convert(123, String.class);
        assertEquals("123", result);
    }

    @Test
    public void testConvertStringToBoolean() {
        assertTrue(N.convert("true", Boolean.class));
        assertTrue(N.convert("TRUE", Boolean.class));
        assertTrue(N.convert("True", Boolean.class));
        assertFalse(N.convert("false", Boolean.class));
        assertFalse(N.convert("FALSE", Boolean.class));
        assertFalse(N.convert("False", Boolean.class));
        assertFalse(N.convert("anything else", Boolean.class));
    }

    @Test
    public void testConvertNumberToNumber() {
        Integer intVal = 123;
        assertEquals(123L, N.convert(intVal, Long.class).longValue());
        assertEquals(123.0f, N.convert(intVal, Float.class).floatValue(), 0.000001f);
        assertEquals(123.0d, N.convert(intVal, Double.class).doubleValue(), 0.000001d);
        assertEquals((byte) 123, N.convert(intVal, Byte.class).byteValue());
        assertEquals((short) 123, N.convert(intVal, Short.class).shortValue());

        Long longVal = 456L;
        assertEquals(456, N.convert(longVal, Integer.class).intValue());
        assertEquals(456.0f, N.convert(longVal, Float.class).floatValue(), 0.000001f);
        assertEquals(456.0d, N.convert(longVal, Double.class).doubleValue(), 0.000001d);

        Double doubleVal = 789.5;
        assertEquals(789, N.convert(doubleVal, Integer.class).intValue());
        assertEquals(789L, N.convert(doubleVal, Long.class).longValue());
        assertEquals(789.5f, N.convert(doubleVal, Float.class).floatValue(), 0.001);
    }

    @Test
    public void testConvertNumberToBoolean() {
        assertTrue(N.convert(1, Boolean.class));
        assertTrue(N.convert(123, Boolean.class));
        assertFalse(N.convert(-1, Boolean.class));
        assertFalse(N.convert(0, Boolean.class));

        assertTrue(N.convert(1L, Boolean.class));
        assertFalse(N.convert(0L, Boolean.class));

        assertTrue(N.convert(1.5, Boolean.class));
        assertFalse(N.convert(0.0, Boolean.class));
    }

    @Test
    public void testConvertNumberToString() {
        assertEquals("123", N.convert(123, String.class));
        assertEquals("456", N.convert(456L, String.class));
        assertEquals("78.9", N.convert(78.9, String.class));
        assertEquals("true", N.convert(true, String.class));
        assertEquals("false", N.convert(false, String.class));
    }

    @Test
    public void testConvertCharacterToInteger() {
        assertEquals(65, N.convert('A', Integer.class).intValue());
        assertEquals(65, N.convert('A', int.class).intValue());
        assertEquals(97, N.convert('a', Integer.class).intValue());
        assertEquals(48, N.convert('0', Integer.class).intValue());
    }

    @Test
    public void testConvertIntegerToCharacter() {
        assertEquals('A', N.convert(65, Character.class).charValue());
        assertEquals('A', N.convert(65, char.class).charValue());
        assertEquals('a', N.convert(97, Character.class).charValue());
        assertEquals('0', N.convert(48, Character.class).charValue());
    }

    @Test
    public void testConvertDateToLong() {
        Date date = new Date(1234567890L);
        assertEquals(1234567890L, N.convert(date, Long.class).longValue());
        assertEquals(1234567890L, N.convert(date, long.class).longValue());

        java.sql.Timestamp timestamp = new java.sql.Timestamp(1234567890L);
        assertEquals(1234567890L, N.convert(timestamp, Long.class).longValue());

        java.sql.Date sqlDate = new java.sql.Date(1234567890L);
        assertEquals(1234567890L, N.convert(sqlDate, Long.class).longValue());
    }

    @Test
    public void testConvertLongToDate() {
        Long timeMillis = 1234567890L;

        Date date = N.convert(timeMillis, Date.class);
        assertEquals(timeMillis.longValue(), date.getTime());

        java.sql.Timestamp timestamp = N.convert(timeMillis, java.sql.Timestamp.class);
        assertEquals(timeMillis.longValue(), timestamp.getTime());

        java.sql.Date sqlDate = N.convert(timeMillis, java.sql.Date.class);
        assertEquals(timeMillis.longValue(), sqlDate.getTime());

        java.sql.Time sqlTime = N.convert(timeMillis, java.sql.Time.class);
        assertEquals(timeMillis.longValue(), sqlTime.getTime());
    }

    @Test
    public void testConvertArrayToCollection() {
        String[] array = { "x", "y", "z" };
        List<String> list = N.convert(array, List.class);
        assertEquals(3, list.size());
        assertEquals("x", list.get(0));
        assertEquals("y", list.get(1));
        assertEquals("z", list.get(2));

        Integer[] intArray = { 1, 2, 3 };
        Set<Integer> set = N.convert(intArray, Set.class);
        assertEquals(3, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
    }

    @Test
    public void testConvertCharArrayFromReader() {
        String data = "World";
        Reader reader = new StringReader(data);

        char[] result = N.convert(reader, char[].class);
        assertArrayEquals(data.toCharArray(), result);
    }

    @Test
    public void testConvertStringFromCharSequence() {
        StringBuilder sb = new StringBuilder("Hello");
        assertEquals("Hello", N.convert(sb, String.class));

        StringBuffer sbuf = new StringBuffer("World");
        assertEquals("World", N.convert(sbuf, String.class));

        CharSequence cs = "Test";
        assertEquals("Test", N.convert(cs, String.class));
    }

    @Test
    public void testConvertStringFromReader() {
        String data = "Reader Content";
        Reader reader = new StringReader(data);

        String result = N.convert(reader, String.class);
        assertEquals(data, result);
    }

    @Test
    public void testConvertPrimitiveTypes() {
        assertEquals(123, N.convert(123, int.class).intValue());
        assertEquals(456L, N.convert(456L, long.class).longValue());
        assertEquals(78.9f, N.convert(78.9f, float.class).floatValue(), 0.001);
        assertEquals(12.34d, N.convert(12.34d, double.class).doubleValue(), 0.001);
        assertTrue(N.convert(true, boolean.class));
        assertFalse(N.convert(false, boolean.class));
        assertEquals('A', N.convert('A', char.class).charValue());
        assertEquals((byte) 99, N.convert((byte) 99, byte.class).byteValue());
        assertEquals((short) 999, N.convert((short) 999, short.class).shortValue());
    }

    @Test
    public void testConvertUsingType() {
        Type<String> stringType = TypeFactory.getType(String.class);
        Type<Integer> intType = TypeFactory.getType(Integer.class);
        Type<Boolean> boolType = TypeFactory.getType(Boolean.class);

        assertEquals(123, N.convert("123", intType).intValue());

        assertEquals("456", N.convert(456, stringType));

        assertTrue(N.convert(1, boolType));
        assertFalse(N.convert(0, boolType));
    }

    @Test
    public void testConvertCollectionWithType() {
        Type<List> listType = TypeFactory.getType(List.class);
        Type<Set> setType = TypeFactory.getType(Set.class);

        List<String> list = Arrays.asList("a", "b", "c");
        Set<String> set = N.convert(list, setType);
        assertEquals(3, set.size());

        Set<Integer> intSet = new HashSet<>(Arrays.asList(1, 2, 3));
        List<Integer> intList = N.convert(intSet, listType);
        assertEquals(3, intList.size());
    }

    @Test
    public void testConvertWithParameterizedType() {
        Type<List> listType = TypeFactory.getType(List.class);
        Type<Map> mapType = TypeFactory.getType(Map.class);

        String[] array = { "x", "y", "z" };
        List<String> list = N.convert(array, listType);
        assertEquals(3, list.size());

        Map<String, Integer> srcMap = new HashMap<>();
        srcMap.put("one", 1);
        srcMap.put("two", 2);

        Map<String, Integer> destMap = N.convert(srcMap, mapType);
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
        String result = N.convert(resource, String.class);

        assertEquals(data, result);
        assertFalse(resource.closed);
    }

    @Test
    public void testConvert_Collections() {
        List<String> strList = Arrays.asList("1", "2", "3");
        List<String> convertedList = N.convert(strList, List.class);
        assertEquals(strList, convertedList);

        Set<String> strSet = new HashSet<>(Arrays.asList("1", "2", "3"));
        Set<String> convertedSet = N.convert(strSet, Set.class);
        assertEquals(strSet, convertedSet);

        String[] strArray = { "1", "2", "3" };
        Collection<String> collection = N.convert(strArray, Collection.class);
        assertEquals(3, collection.size());
        assertTrue(collection.contains("1"));
        assertTrue(collection.contains("2"));
        assertTrue(collection.contains("3"));

        List<String> list = Arrays.asList("1", "2", "3");
        String[] array = N.convert(list, String[].class);
        assertArrayEquals(new String[] { "1", "2", "3" }, array);
    }

    @Test
    public void testConvert_Maps() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        Map<String, Integer> convertedMap = N.convert(map, Map.class);
        assertEquals(map, convertedMap);
    }

    @Test
    public void testConvert_Dates() {
        long timestamp = 1000000L;
        assertEquals(new java.util.Date(timestamp), N.convert(timestamp, java.util.Date.class));
        assertEquals(new java.sql.Timestamp(timestamp), N.convert(timestamp, java.sql.Timestamp.class));
        assertEquals(new java.sql.Date(timestamp), N.convert(timestamp, java.sql.Date.class));
        assertEquals(new java.sql.Time(timestamp), N.convert(timestamp, java.sql.Time.class));

        java.util.Date date = new java.util.Date(timestamp);
        assertEquals(Long.valueOf(timestamp), N.convert(date, Long.class));
    }

    // ========== convert: CharSequence -> String ==========

    @Test
    public void testConvert_StringBuilderToString_UsesCharSequencePath() {
        // L10350: CharSequence -> String via ((CharSequence) srcObj).toString()
        StringBuilder sb = new StringBuilder("hello");
        String result = N.convert(sb, String.class);
        assertEquals("hello", result);
    }

    @Test
    public void testConvertNullToClass() {
        assertNull(N.convert(null, String.class));
        assertNull(N.convert(null, Integer.class));
        assertNull(N.convert(null, Long.class));
        assertNull(N.convert(null, Float.class));
        assertNull(N.convert(null, Double.class));
        assertNull(N.convert(null, Boolean.class));
        assertNull(N.convert(null, Object.class));
        assertNull(N.convert(null, Date.class));
        assertNull(N.convert(null, List.class));
        assertNull(N.convert(null, Map.class));
    }

    @Test
    public void testConvertStringToNumbers() {
        assertEquals(123, N.convert("123", Integer.class).intValue());
        assertEquals(123L, N.convert("123", Long.class).longValue());
        assertEquals(123.45f, N.convert("123.45", Float.class).floatValue(), 0.001);
        assertEquals(123.45d, N.convert("123.45", Double.class).doubleValue(), 0.001);
        assertEquals((byte) 123, N.convert("123", Byte.class).byteValue());
        assertEquals((short) 123, N.convert("123", Short.class).shortValue());
        assertEquals(new BigInteger("123456789012345678901234567890"), N.convert("123456789012345678901234567890", BigInteger.class));
        assertEquals(new BigDecimal("123.456789"), N.convert("123.456789", BigDecimal.class));
        assertNull(N.convert("", Integer.class));
    }

    @Test
    public void testConvertCollectionToCollection() {
        List<String> list = Arrays.asList("a", "b", "c", "b");
        Set<String> set = N.convert(list, Set.class);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));

        Set<Integer> intSet = new HashSet<>(Arrays.asList(1, 2, 3));
        List<Integer> intList = N.convert(intSet, List.class);
        assertEquals(3, intList.size());
        assertTrue(intList.containsAll(intSet));

        List<String> emptyList = new ArrayList<>();
        Set<String> emptySet = N.convert(emptyList, Set.class);
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testConvertCollectionToArray() {
        List<String> list = Arrays.asList("a", "b", "c");
        String[] array = N.convert(list, String[].class);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);

        Set<Integer> set = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        Integer[] intArray = N.convert(set, Integer[].class);
        assertEquals(3, intArray.length);

        List<String> emptyList = new ArrayList<>();
        String[] emptyArray = N.convert(emptyList, String[].class);
        assertEquals(0, emptyArray.length);
    }

    @Test
    public void testConvertSingleObjectToCollection() {
        String str = "hello";
        List<String> list = N.convert(str, List.class);
        assertEquals(1, list.size());
        assertEquals("hello", list.get(0));

        Integer num = 42;
        Set<Integer> set = N.convert(num, Set.class);
        assertEquals(1, set.size());
        assertTrue(set.contains(42));
    }

    @Test
    public void testConvertSingleObjectToArray() {
        String str = "world";
        String[] array = N.convert(str, String[].class);
        assertEquals(1, array.length);
        assertEquals("world", array[0]);

        Integer num = 99;
        Integer[] intArray = N.convert(num, Integer[].class);
        assertEquals(1, intArray.length);
        assertEquals(99, intArray[0].intValue());
    }

    @Test
    public void testConvertMapToMap() {
        Map<String, Integer> srcMap = new HashMap<>();
        srcMap.put("one", 1);
        srcMap.put("two", 2);
        srcMap.put("three", 3);

        Map<String, Integer> destMap = N.convert(srcMap, Map.class);
        assertEquals(3, destMap.size());
        assertEquals(1, destMap.get("one").intValue());
        assertEquals(2, destMap.get("two").intValue());
        assertEquals(3, destMap.get("three").intValue());

        Map<String, String> emptyMap = new HashMap<>();
        Map<String, String> convertedEmpty = N.convert(emptyMap, Map.class);
        assertTrue(convertedEmpty.isEmpty());
    }

    @Test
    public void testConvertByteArrayFromInputStream() {
        byte[] data = { 10, 20, 30, 40, 50 };
        InputStream is = new ByteArrayInputStream(data);

        byte[] result = N.convert(is, byte[].class);
        assertArrayEquals(data, result);
    }

    @Test
    public void testConvertCharArrayFromInputStream() {
        String data = "['T','e','s','t']";
        InputStream is = new ByteArrayInputStream(data.getBytes());

        char[] result = N.convert(is, char[].class);
        assertEquals("Test", String.valueOf(result));
    }

    @Test
    public void testConvertStringFromInputStream() {
        String data = "InputStream Content";
        InputStream is = new ByteArrayInputStream(data.getBytes());

        String result = N.convert(is, String.class);
        assertEquals(data, result);
    }

    @Test
    public void testConvertSameType() {
        String str = "test";
        assertSame(str, N.convert(str, String.class));

        Integer num = 42;
        assertSame(num, N.convert(num, Integer.class));

        List<String> list = new ArrayList<>();
        assertSame(list, N.convert(list, ArrayList.class));
    }

    @Test
    public void testConvertNullToType() {
        Type<String> stringType = TypeFactory.getType(String.class);
        Type<Integer> intType = TypeFactory.getType(Integer.class);
        Type<List> listType = TypeFactory.getType(List.class);

        assertNull(N.convert(null, stringType));
        assertNull(N.convert(null, intType));
        assertNull(N.convert(null, listType));
    }

    @Test
    public void testConvert_Basic() {
        assertEquals(0, N.convert(null, int.class));
        assertEquals(null, N.convert(null, Integer.class));
        assertEquals(null, N.convert(null, String.class));

        assertEquals(Integer.valueOf(123), N.convert("123", Integer.class));
        assertEquals(Long.valueOf(123L), N.convert("123", Long.class));
        assertEquals(Double.valueOf(3.14), N.convert("3.14", Double.class));
        assertEquals(Boolean.TRUE, N.convert("true", Boolean.class));

        assertEquals(Integer.valueOf(123), N.convert(123L, Integer.class));
        assertEquals(Long.valueOf(123L), N.convert(123, Long.class));
        assertEquals(Float.valueOf(3.14f), N.convert(3.14d, Float.class));
        assertEquals(Double.valueOf(3.14d), N.convert(3.14f, Double.class));

        assertEquals(true, N.convert(1, boolean.class));
        assertEquals(false, N.convert(0, boolean.class));
        assertEquals(true, N.convert(5L, Boolean.class));

        assertEquals(Character.valueOf('A'), N.convert(65, Character.class));
        assertEquals(Integer.valueOf(65), N.convert('A', Integer.class));
    }

    @Test
    public void testConvert_WithType() {
        Type<Integer> intType = N.typeOf(int.class);
        assertEquals(Integer.valueOf(123), N.convert("123", intType));
        assertEquals(0, N.convert(null, intType));

        Type<List> listType = N.typeOf(List.class);
        String[] array = { "1", "2", "3" };
        List<String> list = N.convert(array, listType);
        assertEquals(3, list.size());
    }

    @Test
    public void testConvertStringToNumbersWithInvalidFormat() {
        assertThrows(NumberFormatException.class, () -> N.convert("abc", Integer.class));
        assertThrows(NumberFormatException.class, () -> N.convert("12.34.56", Double.class));
    }

    @Test
    public void testConvertStringToCharacter() {
        assertEquals('A', N.convert("A", Character.class).charValue());
        assertEquals('1', N.convert("1", Character.class).charValue());
        assertThrows(RuntimeException.class, () -> N.convert("AB", Character.class));
        assertNull(N.convert("", Character.class));
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

        byte[] result = N.convert(blob, byte[].class);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testConvertInputStreamFromByteArray() {
        byte[] data = { 1, 2, 3, 4, 5 };
        InputStream is = N.convert(data, InputStream.class);

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
        Reader reader = N.convert(data, Reader.class);

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

        assertThrows(UncheckedSQLException.class, () -> N.convert(blob, byte[].class));
    }

    @Test
    public void testConvertEdgeCases() {

        String bigNum = "9223372036854775807";
        assertEquals(Long.MAX_VALUE, N.convert(bigNum, Long.class).longValue());

        assertThrows(ArithmeticException.class, () -> N.convert(Long.MAX_VALUE, Integer.class).intValue());
        assertThrows(ArithmeticException.class, () -> N.convert(1000, Byte.class).byteValue());

        assertEquals((Float) Float.POSITIVE_INFINITY, N.convert("Infinity", Float.class));
        assertEquals((Double) Double.NEGATIVE_INFINITY, N.convert("-Infinity", Double.class));
        assertTrue(Double.isNaN(N.convert("NaN", Double.class).doubleValue()));
    }

    @Test
    public void testConvertUnsupportedConversions() {

        Map<String, String> map = new HashMap<>();
        map.put("key", "value");

        assertThrows(RuntimeException.class, () -> N.convert(map, Integer.class));
        assertFalse(N.convert(map, Boolean.class));

        List<Map<String, Object>> complexList = new ArrayList<>();
        complexList.add(new HashMap<>());

        List<Map<String, Object>> convertedList = N.convert(complexList, List.class);
        assertNotNull(convertedList);
        assertEquals(1, convertedList.size());
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

    // --- Tests for castIfAssignable with Type parameter ---

    @Test
    public void testCastIfAssignable_WithType() {
        // TODO: Test castIfAssignable overload with Type parameter
        com.landawn.abacus.util.u.Nullable<String> result = N.castIfAssignable("hello", String.class);
        assertTrue(result.isPresent());
        assertEquals("hello", result.get());

        com.landawn.abacus.util.u.Nullable<Integer> empty = N.castIfAssignable("hello", Integer.class);
        assertFalse(empty.isPresent());
    }

    // ========== castIfAssignable(Object, Type) overload ==========

    @Test
    public void testCastIfAssignable_WithTypeOverload_ReturnsValue() {
        // L10442: castIfAssignable(val, type.javaType())
        com.landawn.abacus.util.u.Nullable<String> result = N.castIfAssignable("hello", N.typeOf(String.class));
        assertTrue(result.isPresent());
        assertEquals("hello", result.get());
    }

    @Test
    public void testCastIfAssignable_WithTypeOverload_IncompatibleType_ReturnsEmpty() {
        com.landawn.abacus.util.u.Nullable<Integer> result = N.castIfAssignable("notAnInt", N.typeOf(Integer.class));
        assertFalse(result.isPresent());
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
    public void testNegate_Boolean() {
        assertEquals(Boolean.FALSE, N.negate(Boolean.TRUE));
        assertEquals(Boolean.TRUE, N.negate(Boolean.FALSE));
        assertNull(N.negate((Boolean) null));
    }

    @Test
    public void testNegate_BooleanArray() {
        boolean[] array = { true, false, true, false };
        N.negate(array);
        assertArrayEquals(new boolean[] { false, true, false, true }, array);

        boolean[] emptyArray = {};
        N.negate(emptyArray);

        N.negate((boolean[]) null);
    }

    @Test
    public void testNegate_BooleanArrayRange() {
        boolean[] array = { true, false, true, false, true };
        N.negate(array, 1, 4);
        assertArrayEquals(new boolean[] { true, true, false, true, true }, array);

        N.negate(array, 0, 0);

        assertThrows(IndexOutOfBoundsException.class, () -> N.negate(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.negate(array, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> N.negate(array, 3, 2));
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
    public void testIsUnmodifiable_UnmodifiableList() {
        List<String> modifiable = new ArrayList<>(Arrays.asList("a", "b"));
        assertTrue(N.isUnmodifiable(Collections.unmodifiableList(modifiable)));

        assertTrue(N.isUnmodifiable(List.of("a", "b").stream().toList()));
        assertFalse(N.isUnmodifiable(List.of("a", "b").stream().collect(Collectors.toList())));
    }

    @Test
    public void testIsUnmodifiable_UnmodifiableSet() {
        Set<String> modifiable = new HashSet<>(Arrays.asList("a", "b"));
        assertTrue(N.isUnmodifiable(Collections.unmodifiableSet(modifiable)));
    }

    @Test
    public void testIsUnmodifiable_UnmodifiableSortedSet() {
        SortedSet<String> modifiable = new TreeSet<>(Arrays.asList("a", "b"));
        assertTrue(N.isUnmodifiable(Collections.unmodifiableSortedSet(modifiable)));
    }

    @Test
    public void testIsUnmodifiable_UnmodifiableNavigableSet() {
        NavigableSet<String> modifiable = new TreeSet<>(Arrays.asList("a", "b"));
        assertTrue(N.isUnmodifiable(Collections.unmodifiableNavigableSet(modifiable)));
    }

    @Test
    public void testIsUnmodifiable_ListOf() {
        assertTrue(N.isUnmodifiable(List.of("a", "b")));
    }

    @Test
    public void testIsUnmodifiable_SetOf() {
        assertTrue(N.isUnmodifiable(Set.of("a", "b")));
    }

    @Test
    public void testIsUnmodifiable_ArrayList() {
        assertFalse(N.isUnmodifiable(new ArrayList<>()));
    }

    @Test
    public void testIsUnmodifiable_HashSet() {
        assertFalse(N.isUnmodifiable(new HashSet<>()));
    }

    @Test
    public void testIsUnmodifiable_LinkedList() {
        assertFalse(N.isUnmodifiable(new LinkedList<>()));
    }

    @Test
    public void testIsUnmodifiable_TreeSet() {
        assertFalse(N.isUnmodifiable(new TreeSet<>()));
    }

    @Test
    public void testIsUnmodifiable_ConcurrentHashSet() {
        assertFalse(N.isUnmodifiable(ConcurrentHashMap.newKeySet()));
    }

    @Test
    public void testIsUnmodifiable_CachingBehavior() {
        // First call - performs mutation test
        Collection<String> c1 = new ArrayList<>();
        assertFalse(N.isUnmodifiable(c1));

        // Second call - uses cached result
        Collection<String> c2 = new ArrayList<>();
        assertFalse(N.isUnmodifiable(c2));
        // First call - performs mutation test
        Map<String, String> m1 = new HashMap<>();
        assertFalse(N.isUnmodifiable(m1));

        // Second call - uses cached result
        Map<String, String> m2 = new HashMap<>();
        assertFalse(N.isUnmodifiable(m2));

    }

    @Test
    public void testIsUnmodifiable_ModifiableCollectionNotMutated() {
        List<String> list = new ArrayList<>();
        list.add("original");

        assertFalse(N.isUnmodifiable(list));

        // Verify original element still present
        assertEquals(1, list.size());
        assertEquals("original", list.get(0));
    }

    @Test
    public void testIsUnmodifiable_ImmutableList() {
        assertTrue(N.isUnmodifiable(ImmutableList.of("a", "b")));
    }

    @Test
    public void testIsUnmodifiable_ImmutableSet() {
        assertTrue(N.isUnmodifiable(ImmutableSet.of("a", "b")));
    }

    @Test
    public void testIsUnmodifiable_UnmodifiableMap() {
        Map<String, String> modifiable = new HashMap<>();
        modifiable.put("key", "value");
        assertTrue(N.isUnmodifiable(Collections.unmodifiableMap(modifiable)));
    }

    @Test
    public void testIsUnmodifiable_UnmodifiableSortedMap() {
        SortedMap<String, String> modifiable = new TreeMap<>();
        modifiable.put("key", "value");
        assertTrue(N.isUnmodifiable(Collections.unmodifiableSortedMap(modifiable)));
    }

    @Test
    public void testIsUnmodifiable_UnmodifiableNavigableMap() {
        NavigableMap<String, String> modifiable = new TreeMap<>();
        modifiable.put("key", "value");
        assertTrue(N.isUnmodifiable(Collections.unmodifiableNavigableMap(modifiable)));
    }

    @Test
    public void testIsUnmodifiable_MapOf() {
        assertTrue(N.isUnmodifiable(Map.of("k1", "v1", "k2", "v2")));
    }

    @Test
    public void testIsUnmodifiable_MapOfEntries() {
        assertTrue(N.isUnmodifiable(Map.ofEntries(Map.entry("k1", "v1"), Map.entry("k2", "v2"))));
    }

    @Test
    public void testIsUnmodifiable_HashMap() {
        assertFalse(N.isUnmodifiable(new HashMap<>()));
    }

    @Test
    public void testIsUnmodifiable_TreeMap() {
        assertFalse(N.isUnmodifiable(new TreeMap<>()));
    }

    @Test
    public void testIsUnmodifiable_LinkedHashMap() {
        assertFalse(N.isUnmodifiable(new LinkedHashMap<>()));
    }

    @Test
    public void testIsUnmodifiable_ConcurrentHashMap() {
        assertFalse(N.isUnmodifiable(new ConcurrentHashMap<>()));
    }

    @Test
    public void testIsUnmodifiable_IdentityHashMap() {
        assertFalse(N.isUnmodifiable(new IdentityHashMap<>()));
    }

    @Test
    public void testIsUnmodifiable_WeakHashMap() {
        assertFalse(N.isUnmodifiable(new WeakHashMap<>()));
    }

    @Test
    public void testIsUnmodifiable_ModifiableMapNotMutated() {
        Map<String, String> map = new HashMap<>();
        map.put("original", "value");

        assertFalse(N.isUnmodifiable(map));

        // Verify original entry still present
        assertEquals(1, map.size());
        assertEquals("value", map.get("original"));
    }

    @Test
    public void testIsUnmodifiable_ImmutableMap() {
        assertTrue(N.isUnmodifiable(ImmutableMap.of("k1", "v1")));
    }

    @Test
    public void testIsUnmodifiable_ImmutableBiMap() {
        assertTrue(N.isUnmodifiable(ImmutableBiMap.of("k1", "v1")));
    }

    @Test
    public void testIsUnmodifiable_ImmutableSortedMap() {
        assertTrue(N.isUnmodifiable(ImmutableSortedMap.of("k1", "v1")));
    }

    @Test
    public void testIsUnmodifiable_EnumMap() {
        Map<DayOfWeek, String> enumMap = new EnumMap<>(DayOfWeek.class);
        assertFalse(N.isUnmodifiable(enumMap));
    }

    @Test
    public void testIsUnmodifiable_UnmodifiableEnumMap() {
        Map<DayOfWeek, String> enumMap = new EnumMap<>(DayOfWeek.class);
        enumMap.put(DayOfWeek.MONDAY, "First day");
        assertTrue(N.isUnmodifiable(Collections.unmodifiableMap(enumMap)));
    }

    @Test
    public void testIsUnmodifiable_NullCollection() {
        assertTrue(N.isUnmodifiable((Collection) null));
    }

    @Test
    public void testIsUnmodifiable_EmptyList() {
        assertTrue(N.isUnmodifiable(Collections.emptyList()));
    }

    @Test
    public void testIsUnmodifiable_EmptySet() {
        assertTrue(N.isUnmodifiable(Collections.emptySet()));
    }

    @Test
    public void testIsUnmodifiable_Singleton() {
        assertTrue(N.isUnmodifiable(Collections.singleton("value")));
    }

    @Test
    public void testIsUnmodifiable_SingletonList() {
        assertTrue(N.isUnmodifiable(Collections.singletonList("value")));
    }

    @Test
    public void testIsUnmodifiable_NullMap() {
        assertTrue(N.isUnmodifiable((Map) null));
    }

    @Test
    public void testIsUnmodifiable_EmptyMap() {
        assertTrue(N.isUnmodifiable(Collections.emptyMap()));
    }

    @Test
    public void testIsUnmodifiable_SingletonMap() {
        assertTrue(N.isUnmodifiable(Collections.singletonMap("key", "value")));
    }

    @Test
    public void testIsUnmodifiable_MapWithNullKeySupport() {
        Map<String, String> map = new HashMap<>();
        map.put(null, "value");
        assertFalse(N.isUnmodifiable(map));
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

        assertTrue(N.isUnmodifiable(custom));
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

        assertFalse(N.isUnmodifiable(custom));
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

        assertTrue(N.isUnmodifiable(custom));
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

        assertFalse(N.isUnmodifiable(custom));
    }

    // ========== unmodifiableCollection/List/Set null path ==========

    @Test
    public void testUnmodifiableCollection_Null_ReturnsEmptyList() {
        // L10992: c == null -> return emptyList()
        Collection<String> result = N.unmodifiableCollection(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUnmodifiableCollection() {
        Collection<String> col = new ArrayList<>(Arrays.asList("a", "b"));
        Collection<String> unmodCol = N.unmodifiableCollection(col);
        assertEquals(2, unmodCol.size());
        assertThrows(UnsupportedOperationException.class, () -> unmodCol.add("c"));
    }

    @Test
    public void testUnmodifiableList_Null_ReturnsEmptyList() {
        // L11040: list == null -> return emptyList()
        List<String> result = N.unmodifiableList(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUnmodifiableList_AlreadyUnmodifiable_ReturnsSameInstance() {
        // L11046: already unmodifiable class -> return (List<T>) list
        List<String> modifiable = new ArrayList<>(Arrays.asList("a", "b"));
        List<String> unmod1 = N.unmodifiableList(modifiable);
        // Second call with the already-wrapped list should return same instance
        List<String> unmod2 = N.unmodifiableList(unmod1);
        assertSame(unmod1, unmod2);
    }

    @Test
    public void testUnmodifiableList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        List<String> unmodList = N.unmodifiableList(list);
        assertEquals(3, unmodList.size());
        assertThrows(UnsupportedOperationException.class, () -> unmodList.add("d"));
    }

    @Test
    public void testUnmodifiableSet_Null_ReturnsEmptySet() {
        // L11088: set == null -> return emptySet()
        Set<String> result = N.unmodifiableSet(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUnmodifiableSet() {
        Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> unmodSet = N.unmodifiableSet(set);
        assertEquals(3, unmodSet.size());
        assertThrows(UnsupportedOperationException.class, () -> unmodSet.add("d"));
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
    public void testUnmodifiableMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Map<String, Integer> unmodMap = N.unmodifiableMap(map);
        assertEquals(1, unmodMap.size());
        assertThrows(UnsupportedOperationException.class, () -> unmodMap.put("b", 2));
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

    // --- Tests for newProxyInstance ---

    @Test
    public void testNewProxyInstance_WithClass() {
        Runnable proxy = N.newProxyInstance(Runnable.class, (p, method, args) -> null);
        assertNotNull(proxy);
    }

    @Test
    public void testNewInstance() {
        String str = N.newInstance(String.class);
        assertNotNull(str);
        assertEquals("", str);
    }

    @Test
    public void testNewInstance_SimpleClass() {
        String str = N.newInstance(String.class);
        assertNotNull(str);
        assertEquals("", str);

        ArrayList<String> list = N.newInstance(ArrayList.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testNewInstance_AbstractClass() {
        List<String> list = N.newInstance(List.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());

        Map<String, String> map = N.newInstance(Map.class);
        assertNotNull(map);
        assertTrue(map.isEmpty());

        assertThrows(IllegalArgumentException.class, () -> N.newInstance(Number.class));
    }

    @Test
    public void testNewCollection() {
        Collection<String> col = N.newCollection(ArrayList.class);
        assertNotNull(col);
        assertTrue(col.isEmpty());
        assertTrue(col instanceof ArrayList);
    }

    @Test
    public void testNewCollection_NoSize() {
        Collection<String> list = N.newCollection(List.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());

        Collection<String> set = N.<String> newCollection(Set.class);
        assertNotNull(set);
        assertTrue(set.isEmpty());

        Collection<String> queue = N.newCollection(Queue.class);
        assertNotNull(queue);
        assertTrue(queue.isEmpty());
    }

    @Test
    public void testNewCollection_WithSize() {
        Collection<String> list = N.newCollection(ArrayList.class, 10);
        assertNotNull(list);
        assertTrue(list.isEmpty());
        list.add("test");
        assertEquals(1, list.size());

        Collection<String> set = N.newCollection(HashSet.class, 10);
        assertNotNull(set);
        assertTrue(set.isEmpty());

        Collection<String> collection = N.newCollection(LinkedList.class, 5);
        assertNotNull(collection);
        assertTrue(collection instanceof LinkedList);
    }

    @Test
    public void testNewMap() {
        Map<String, Integer> map = N.newMap(HashMap.class);
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertTrue(map instanceof HashMap);
    }

    @Test
    public void testNewMap_NoSize() {
        Map<String, Integer> map = N.newMap(Map.class);
        assertNotNull(map);
        assertTrue(map.isEmpty());

        Map<String, Integer> hashMap = N.newMap(HashMap.class);
        assertNotNull(hashMap);
        assertTrue(hashMap.isEmpty());

        Map<String, Integer> treeMap = N.newMap(TreeMap.class);
        assertNotNull(treeMap);
        assertTrue(treeMap.isEmpty());
    }

    @Test
    public void testNewMap_WithSize() {
        Map<String, Integer> map = N.newMap(HashMap.class, 10);
        assertNotNull(map);
        assertTrue(map.isEmpty());
        map.put("test", 1);
        assertEquals(1, map.size());

        Map<String, Integer> linkedMap = N.newMap(LinkedHashMap.class, 10);
        assertNotNull(linkedMap);
        assertTrue(linkedMap instanceof LinkedHashMap);
    }

    @Test
    public void testNewArray() {
        String[] arr = N.newArray(String.class, 3);
        assertEquals(3, arr.length);
        assertNull(arr[0]);
    }

    @Test
    public void testNewArray_SingleDimension() {
        int[] intArray = N.newArray(int.class, 10);
        assertNotNull(intArray);
        assertEquals(10, intArray.length);

        String[] strArray = N.newArray(String.class, 5);
        assertNotNull(strArray);
        assertEquals(5, strArray.length);

        Object[] objArray = N.newArray(Object.class, 0);
        assertNotNull(objArray);
        assertEquals(0, objArray.length);

        assertThrows(NegativeArraySizeException.class, () -> N.newArray(int.class, -1));
    }

    @Test
    public void testNewArray_MultiDimension() {
        int[][] int2D = N.newArray(int.class, 3, 4);
        assertNotNull(int2D);
        assertEquals(3, int2D.length);
        assertEquals(4, int2D[0].length);

        String[][][] str3D = N.newArray(String.class, 2, 3, 4);
        assertNotNull(str3D);
        assertEquals(2, str3D.length);
        assertEquals(3, str3D[0].length);
        assertEquals(4, str3D[0][0].length);

        assertThrows(IllegalArgumentException.class, () -> N.newArray(null, 10));
        assertThrows(NegativeArraySizeException.class, () -> N.newArray(int.class, 5, -1));
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
    public void testNewArrayList_NoArgs() {
        ArrayList<String> list = N.newArrayList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
        list.add("test");
        assertEquals(1, list.size());
    }

    @Test
    public void testNewArrayList_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c");
        ArrayList<String> list = N.newArrayList(source);
        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));

        ArrayList<String> emptyList = N.newArrayList(new ArrayList<>());
        assertNotNull(emptyList);
        assertTrue(emptyList.isEmpty());

        ArrayList<String> nullList = N.newArrayList(null);
        assertNotNull(nullList);
        assertTrue(nullList.isEmpty());
    }

    @Test
    public void testNewLinkedList() {
        List<String> list = N.newLinkedList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
        assertTrue(list instanceof LinkedList);
    }

    @Test
    public void testNewLinkedList_NoArgs() {
        LinkedList<String> list = N.newLinkedList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
        list.add("test");
        assertEquals(1, list.size());
    }

    @Test
    public void testNewLinkedList_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c");
        LinkedList<String> list = N.newLinkedList(source);
        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals("a", list.getFirst());
        assertEquals("c", list.getLast());

        LinkedList<String> emptyList = N.newLinkedList(new ArrayList<>());
        assertNotNull(emptyList);
        assertTrue(emptyList.isEmpty());

        LinkedList<String> nullList = N.newLinkedList(null);
        assertNotNull(nullList);
        assertTrue(nullList.isEmpty());
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
    public void testNewHashSet_NoArgs() {
        Set<String> set = N.newHashSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        set.add("test");
        assertEquals(1, set.size());
    }

    @Test
    public void testNewHashSet_WithCapacity() {
        Set<String> set = N.newHashSet(100);
        assertNotNull(set);
        assertTrue(set.isEmpty());
        set.add("test");
        assertEquals(1, set.size());
    }

    @Test
    public void testNewHashSet_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c", "a");
        Set<String> set = N.newHashSet(source);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));

        Set<String> emptySet = N.newHashSet(new ArrayList<>());
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());

        Set<String> nullSet = N.newHashSet(null);
        assertNotNull(nullSet);
        assertTrue(nullSet.isEmpty());
    }

    @Test
    public void testNewLinkedHashSet() {
        Set<String> set = N.newLinkedHashSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertTrue(set instanceof LinkedHashSet);
    }

    @Test
    public void testNewLinkedHashSet_WithCapacity() {
        Set<String> set = N.newLinkedHashSet(100);
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertTrue(set instanceof LinkedHashSet);
    }

    @Test
    public void testNewLinkedHashSet_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c");
        Set<String> set = N.newLinkedHashSet(source);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertTrue(set instanceof LinkedHashSet);

        Iterator<String> iter = set.iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
    }

    @Test
    public void testNewTreeSet() {
        Set<String> set = N.newTreeSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertTrue(set instanceof TreeSet);
    }

    @Test
    public void testNewTreeSet_NoArgs() {
        TreeSet<String> set = N.newTreeSet();
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
        TreeSet<String> set = N.newTreeSet(reverseComparator);
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
        TreeSet<String> set = N.newTreeSet(source);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertEquals("a", set.first());
        assertEquals("c", set.last());

        TreeSet<String> emptySet = N.newTreeSet(new ArrayList<String>());
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testNewTreeSet_WithSortedSet() {
        SortedSet<String> source = new TreeSet<>();
        source.add("b");
        source.add("a");
        source.add("c");

        TreeSet<String> set = N.newTreeSet(source);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertEquals("a", set.first());
        assertEquals("c", set.last());

        assertTrue(N.newTreeSet((SortedSet<String>) null).isEmpty());
    }

    @Test
    public void testNewConcurrentHashSet() {
        Set<String> set = N.newConcurrentHashSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    @Test
    public void testNewConcurrentHashSet_NoArgs() {
        Set<String> set = N.newConcurrentHashSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        set.add("test");
        assertEquals(1, set.size());
    }

    @Test
    public void testNewConcurrentHashSet_WithCapacity() {
        Set<String> set = N.newConcurrentHashSet(100);
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    @Test
    public void testNewConcurrentHashSet_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c");
        Set<String> set = N.newConcurrentHashSet(source);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
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
    public void testNewMultiset() {
        com.landawn.abacus.util.Multiset<String> multiset = N.newMultiset();
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testNewMultiset_NoArgs() {
        Multiset<String> multiset = N.newMultiset();
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testNewMultiset_WithCapacity() {
        Multiset<String> multiset = N.newMultiset(100);
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testNewMultiset_WithMapType() {
        Multiset<String> multiset = N.newMultiset(LinkedHashMap.class);
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testNewMultiset_WithMapSupplier() {
        Multiset<String> multiset = N.newMultiset(() -> new TreeMap<String, MutableInt>());
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testNewMultiset_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "a", "c", "b", "a");
        Multiset<String> multiset = N.newMultiset(source);
        assertNotNull(multiset);
        assertEquals(6, multiset.size());
        assertEquals(3, multiset.getCount("a"));
        assertEquals(2, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
    }

    @Test
    public void testNewArrayDeque() {
        java.util.Deque<String> deque = N.newArrayDeque();
        assertNotNull(deque);
        assertTrue(deque.isEmpty());
        assertTrue(deque instanceof java.util.ArrayDeque);
    }

    @Test
    public void testNewArrayDeque_NoArgs() {
        ArrayDeque<String> deque = N.newArrayDeque();
        assertNotNull(deque);
        assertTrue(deque.isEmpty());
    }

    @Test
    public void testNewArrayDeque_WithCapacity() {
        ArrayDeque<String> deque = N.newArrayDeque(100);
        assertNotNull(deque);
        assertTrue(deque.isEmpty());
    }

    @Test
    public void testNewArrayDeque_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c");
        ArrayDeque<String> deque = N.newArrayDeque(source);
        assertNotNull(deque);
        assertEquals(3, deque.size());
        assertEquals("a", deque.getFirst());
        assertEquals("c", deque.getLast());
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

    // --- Tests for newHashMap with Collection and keyExtractor ---

    @Test
    public void testNewHashMap_WithCollectionAndKeyExtractor() {
        List<String> source = Arrays.asList("alice", "bob");
        Map<Character, String> result = N.newHashMap(source, s -> s.charAt(0));
        assertEquals("alice", result.get('a'));
        assertEquals("bob", result.get('b'));
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
    public void testNewHashMap_NoArgs() {
        Map<String, Integer> map = N.newHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertTrue(map instanceof HashMap);
    }

    @Test
    public void testNewHashMap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        Map<String, Integer> map = N.newHashMap(source);
        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals(1, map.get("a").intValue());
        assertEquals(2, map.get("b").intValue());

        Map<String, Integer> emptyMap = N.newHashMap(new HashMap<>());
        assertNotNull(emptyMap);
        assertTrue(emptyMap.isEmpty());
    }

    // --- Tests for newLinkedHashMap with Collection and keyExtractor ---

    @Test
    public void testNewLinkedHashMap_WithCollectionAndKeyExtractor() {
        List<String> source = Arrays.asList("alice", "bob");
        Map<Character, String> result = N.newLinkedHashMap(source, s -> s.charAt(0));
        assertEquals(2, result.size());
    }

    @Test
    public void testNewLinkedHashMap() {
        Map<String, Integer> map = N.newLinkedHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertTrue(map instanceof LinkedHashMap);
    }

    @Test
    public void testNewLinkedHashMap_WithCapacity() {
        Map<String, Integer> map = N.newLinkedHashMap(100);
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

        Map<String, Integer> map = N.newLinkedHashMap(source);
        assertNotNull(map);
        assertEquals(3, map.size());

        Iterator<String> iter = map.keySet().iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
    }

    // ========== newHashMap / newLinkedHashMap with empty collection ==========

    @Test
    public void testNewHashMap_EmptyCollection_ReturnsEmptyMap() {
        // L12115: isEmpty(c) -> return new HashMap<>()
        Map<String, String> result = N.newHashMap(Collections.<String> emptyList(), s -> s);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testNewLinkedHashMap_EmptyCollection_ReturnsEmptyMap() {
        // L12177: isEmpty(c) -> return newLinkedHashMap()
        Map<String, String> result = N.newLinkedHashMap(Collections.<String> emptyList(), s -> s);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testNewTreeMap() {
        Map<String, Integer> map = N.newTreeMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertTrue(map instanceof TreeMap);
    }

    @Test
    public void testNewTreeMap_NoArgs() {
        TreeMap<String, Integer> map = N.newTreeMap();
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
        TreeMap<String, Integer> map = N.newTreeMap(reverseComparator);
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

        TreeMap<String, Integer> map = N.newTreeMap(source);
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

        TreeMap<String, Integer> map = N.newTreeMap(source);
        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals("a", map.firstKey());
        assertEquals("c", map.lastKey());

        assertTrue(N.newTreeMap((SortedMap<String, Integer>) null).isEmpty());
    }

    @Test
    public void testNewIdentityHashMap() {
        java.util.IdentityHashMap<String, Integer> map = N.newIdentityHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewIdentityHashMap_NoArgs() {
        IdentityHashMap<String, Integer> map = N.newIdentityHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewIdentityHashMap_WithCapacity() {
        IdentityHashMap<String, Integer> map = N.newIdentityHashMap(100);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewIdentityHashMap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        IdentityHashMap<String, Integer> map = N.newIdentityHashMap(source);
        assertNotNull(map);
        assertEquals(2, map.size());
    }

    @Test
    public void testNewConcurrentHashMap() {
        Map<String, Integer> map = N.newConcurrentHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertTrue(map instanceof java.util.concurrent.ConcurrentHashMap);
    }

    @Test
    public void testNewConcurrentHashMap_NoArgs() {
        ConcurrentHashMap<String, Integer> map = N.newConcurrentHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewConcurrentHashMap_WithCapacity() {
        ConcurrentHashMap<String, Integer> map = N.newConcurrentHashMap(100);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewConcurrentHashMap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        ConcurrentHashMap<String, Integer> map = N.newConcurrentHashMap(source);
        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals(1, map.get("a").intValue());
        assertEquals(2, map.get("b").intValue());
    }

    @Test
    public void testNewBiMap() {
        com.landawn.abacus.util.BiMap<String, Integer> biMap = N.newBiMap();
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewBiMap_NoArgs() {
        BiMap<String, Integer> biMap = N.newBiMap();
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewBiMap_WithCapacity() {
        BiMap<String, Integer> biMap = N.newBiMap(100);
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewBiMap_WithCapacityAndLoadFactor() {
        BiMap<String, Integer> biMap = N.newBiMap(100, 0.75f);
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewBiMap_WithMapTypes() {
        BiMap<String, Integer> biMap = N.newBiMap(LinkedHashMap.class, TreeMap.class);
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewBiMap_WithMapSuppliers() {
        BiMap<String, Integer> biMap = N.newBiMap(() -> new LinkedHashMap<String, Integer>(), () -> new TreeMap<Integer, String>());
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
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
    public void checkArgNotEmpty_multimap_valid() {
        Multimap<String, Integer, List<Integer>> multimap = N.newListMultimap(Map.of("a", 1));
        assertSame(multimap, N.checkArgNotEmpty(multimap, "multimap"));
    }

    @Test
    public void testNewListMultimap_NoArgs() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewListMultimap_WithCapacity() {
        ListMultimap<String, Integer> multimap = N.newListMultimap(100);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewListMultimap_WithMapType() {
        ListMultimap<String, Integer> multimap = N.newListMultimap(LinkedHashMap.class);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewListMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("a", 3);

        ListMultimap<String, Integer> multimap = N.newListMultimap(source);
        assertNotNull(multimap);
        assertEquals(2, multimap.totalValueCount());
    }

    // ========== newListMultimap(Collection, keyExtractor) ==========

    @Test
    public void testNewListMultimap_CollectionAndKeyExtractor() {
        // L12491: delegates to ListMultimap.fromCollection(c, keyExtractor)
        List<String> items = Arrays.asList("apple", "banana", "avocado");
        ListMultimap<Character, String> result = N.newListMultimap(items, s -> s.charAt(0));
        assertNotNull(result);
        assertEquals(2, result.get('a').size());
    }

    @Test
    public void testNewListMultimap_CollectionKeyAndValueExtractor() {
        // L12507: delegates to ListMultimap.fromCollection(c, keyExtractor, valueExtractor)
        List<String> items = Arrays.asList("apple", "banana");
        ListMultimap<Character, Integer> result = N.newListMultimap(items, s -> s.charAt(0), String::length);
        assertNotNull(result);
        assertEquals(5, (int) result.get('a').get(0));
    }

    @Test
    public void testNewLinkedListMultimap() {
        com.landawn.abacus.util.ListMultimap<String, Integer> multimap = N.newLinkedListMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedListMultimap_NoArgs() {
        ListMultimap<String, Integer> multimap = N.newLinkedListMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedListMultimap_WithCapacity() {
        ListMultimap<String, Integer> multimap = N.newLinkedListMultimap(100);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedListMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        ListMultimap<String, Integer> multimap = N.newLinkedListMultimap(source);
        assertNotNull(multimap);
        assertEquals(2, multimap.totalValueCount());
    }

    @Test
    public void testNewSortedListMultimap() {
        com.landawn.abacus.util.ListMultimap<String, Integer> multimap = N.newSortedListMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSortedListMultimap_NoArgs() {
        ListMultimap<String, Integer> multimap = N.newSortedListMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSortedListMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("b", 2);
        source.put("a", 1);
        source.put("c", 3);

        ListMultimap<String, Integer> multimap = N.newSortedListMultimap(source);
        assertNotNull(multimap);
        assertEquals(3, multimap.totalValueCount());

        Iterator<String> iter = multimap.keySet().iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
    }

    @Test
    public void testNewSetMultimap() {
        com.landawn.abacus.util.SetMultimap<String, Integer> multimap = N.newSetMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSetMultimap_NoArgs() {
        SetMultimap<String, Integer> multimap = N.newSetMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSetMultimap_WithCapacity() {
        SetMultimap<String, Integer> multimap = N.newSetMultimap(100);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSetMultimap_WithMapType() {
        SetMultimap<String, Integer> multimap = N.newSetMultimap(LinkedHashMap.class);
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
    public void testNewLinkedSetMultimap_NoArgs() {
        SetMultimap<String, Integer> multimap = N.newLinkedSetMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedSetMultimap_WithCapacity() {
        SetMultimap<String, Integer> multimap = N.newLinkedSetMultimap(100);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedSetMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        SetMultimap<String, Integer> multimap = N.newLinkedSetMultimap(source);
        assertNotNull(multimap);
        assertEquals(2, multimap.totalValueCount());
    }

    @Test
    public void testNewSortedSetMultimap() {
        com.landawn.abacus.util.SetMultimap<String, Integer> multimap = N.newSortedSetMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSortedSetMultimap_NoArgs() {
        SetMultimap<String, Integer> multimap = N.newSortedSetMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSortedSetMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("b", 2);
        source.put("a", 1);
        source.put("c", 3);

        SetMultimap<String, Integer> multimap = N.newSortedSetMultimap(source);
        assertNotNull(multimap);
        assertEquals(3, multimap.totalValueCount());

        Iterator<String> iter = multimap.keySet().iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
    }

    @Test
    public void testNewEmptyDataset() {
    }

    @Test
    public void testNewEmptyDataset_WithColumnNames() {
        List<String> columnNames = Arrays.asList("col1", "col2", "col3");
        Dataset ds = N.newEmptyDataset(columnNames);
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
        Dataset ds = N.newEmptyDataset(columnNames, properties);
        assertNotNull(ds);
        assertEquals(0, ds.size());
        assertEquals(2, ds.columnCount());
        assertEquals("value", ds.getProperties().get("key"));
    }

    @Test
    public void testNewDataset() {
    }

    @Test
    public void testNewDataset_WithColumnNamesAndRows() {
        List<String> columnNames = Arrays.asList("name", "age");
        List<Object[]> rows = Arrays.asList(new Object[] { "John", 30 }, new Object[] { "Jane", 25 });

        Dataset ds = N.newDataset(columnNames, rows);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
        assertEquals(columnNames, ds.columnNames());
    }

    @Test
    public void testNewDataset_WithColumnNamesAndRowArray() {
        List<String> columnNames = Arrays.asList("name", "age");
        Object[][] rows = new Object[][] { { "John", 30 }, { "Jane", 25 } };

        Dataset ds = N.newDataset(columnNames, rows);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
    }

    @Test
    public void testNewDataset_FromKeyValueMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("John", 30);
        map.put("Jane", 25);

        Dataset ds = N.newDataset("Name", "Age", map);
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

        Dataset ds = N.newDataset(map);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
        assertEquals(Arrays.asList("name", "age"), ds.columnNames());
    }

    @Test
    public void testNewDataset_SingleColumn() {
        List<String> values = Arrays.asList("A", "B", "C");
        Dataset ds = N.newDataset("Letter", values);
        assertNotNull(ds);
        assertEquals(3, ds.size());
        assertEquals(1, ds.columnCount());
        assertEquals("Letter", ds.columnNames().get(0));
    }

    @Test
    public void testNewDataset_WithBeanRowsAndProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("source", "beanRows");

        Dataset dataset = N.newDataset(Arrays.asList("name", "age", "missing"), Arrays.asList(new DatasetRowBean("Tom", 10), new DatasetRowBean("Jerry", 12)),
                properties);

        assertEquals(2, dataset.size());
        assertEquals(Arrays.asList("name", "age", "missing"), dataset.columnNames());
        assertEquals("beanRows", dataset.getProperties().get("source"));
    }

    // Tests for newDataset with columnNames, rows, properties
    @Test
    public void testNewDataset_WithProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("author", "test");
        List<String> cols = Arrays.asList("name", "age");
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { "Alice", 30 });
        rows.add(new Object[] { "Bob", 25 });
        Dataset ds = N.newDataset(cols, rows, props);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
    }

    @Test
    public void testNewDataset_WithProperties_EmptyRows() {
        Map<String, Object> props = new HashMap<>();
        props.put("version", 1);
        List<String> cols = Arrays.asList("id", "name");
        Dataset ds = N.newDataset(cols, new ArrayList<>(), props);
        assertNotNull(ds);
        assertEquals(0, ds.size());
    }

    // ========== newDataset with Collection rows and Map rows path (via properties parameter) ==========

    @Test
    public void testNewDataset_WithProperties_MapRows() {
        List<String> columns = Arrays.asList("name", "age");
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("name", "Alice");
        row1.put("age", 30);
        rows.add(row1);
        Map<String, Object> row2 = new HashMap<>();
        row2.put("name", "Bob");
        row2.put("age", 25);
        rows.add(row2);
        Dataset ds = N.newDataset(columns, rows, null);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
    }

    @Test
    public void testNewDataset_WithProperties_CollectionRows() {
        List<String> columns = Arrays.asList("x", "y");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("a", 1), Arrays.asList("b", 2));
        Dataset ds = N.newDataset(columns, rows, null);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
    }

    @Test
    public void testNewDataset_WithProperties_EmptyRows_ReturnsEmptyDataset() {
        List<String> columns = Arrays.asList("col1");
        Dataset ds = N.newDataset(columns, new ArrayList<>(), null);
        assertNotNull(ds);
        assertEquals(0, ds.size());
        assertEquals(1, ds.columnCount());
    }

    @Test
    public void testMerge() {
    }

    // Tests for merge(Collection<Dataset>, boolean) - more scenarios
    @Test
    public void testMerge_CollectionDatasets_RequiresSameColumns_True() {
        Dataset ds1 = N.newDataset(Arrays.asList("col1", "col2"), N.asSingletonList(new Object[] { "A", 1 }));
        Dataset ds2 = N.newDataset(Arrays.asList("col1", "col2"), N.asSingletonList(new Object[] { "B", 2 }));
        Dataset ds3 = N.newDataset(Arrays.asList("col1", "col2"), N.asSingletonList(new Object[] { "C", 3 }));
        Dataset merged = N.merge(Arrays.asList(ds1, ds2, ds3), true);
        assertNotNull(merged);
        assertEquals(3, merged.size());
        assertEquals(2, merged.columnCount());
    }

    @Test
    public void testMerge_CollectionDatasets_RequiresSameColumns_False() {
        Dataset ds1 = N.newDataset(Arrays.asList("col1"), N.asSingletonList(new Object[] { "A" }));
        Dataset ds2 = N.newDataset(Arrays.asList("col2"), N.asSingletonList(new Object[] { 1 }));
        Dataset ds3 = N.newDataset(Arrays.asList("col1"), N.asSingletonList(new Object[] { "B" }));
        Dataset merged = N.merge(Arrays.asList(ds1, ds2, ds3), false);
        assertNotNull(merged);
        assertEquals(3, merged.size());
    }

    @Test
    public void testMerge_TwoDatasets() {
        Dataset ds1 = N.newDataset(Arrays.asList("col1"), Arrays.asList(new Object[] { "A" }, new Object[] { "B" }));

        Dataset ds2 = N.newDataset(Arrays.asList("col2"), Arrays.asList(new Object[] { 1 }, new Object[] { 2 }));

        Dataset merged = N.merge(ds1, ds2);
        assertNotNull(merged);
        assertEquals(4, merged.size());
        assertEquals(2, merged.columnCount());

        assertThrows(IllegalArgumentException.class, () -> N.merge(null, ds2));
        assertThrows(IllegalArgumentException.class, () -> N.merge(ds1, null));
    }

    @Test
    public void testMerge_ThreeDatasets() {
        Dataset ds1 = N.newDataset(Arrays.asList("col1"), Arrays.asList(new Object[] { "A" }));
        Dataset ds2 = N.newDataset(Arrays.asList("col2"), Arrays.asList(new Object[] { 1 }));
        Dataset ds3 = N.newDataset(Arrays.asList("col3"), Arrays.asList(new Object[] { true }));

        Dataset merged = N.merge(ds1, ds2, ds3);
        assertNotNull(merged);
        assertEquals(3, merged.size());
        assertEquals(3, merged.columnCount());

        assertThrows(IllegalArgumentException.class, () -> N.merge(null, ds2, ds3));
        assertThrows(IllegalArgumentException.class, () -> N.merge(ds1, null, ds3));
        assertThrows(IllegalArgumentException.class, () -> N.merge(ds1, ds2, (Dataset) null));
    }

    @Test
    public void testMerge_CollectionOfDatasets() {
        List<Dataset> datasets = Arrays.asList(N.newDataset(Arrays.asList("col1"), Arrays.asList(new Object[] { "A" })),
                N.newDataset(Arrays.asList("col2"), Arrays.asList(new Object[] { 1 })),
                N.newDataset(Arrays.asList("col3"), Arrays.asList(new Object[] { true })));

        Dataset merged = N.merge(datasets);
        assertNotNull(merged);
        assertEquals(3, merged.size());
        assertEquals(3, merged.columnCount());

        assertThrows(IllegalArgumentException.class, () -> N.merge(new ArrayList<>()));

        Dataset single = N.merge(Arrays.asList(datasets.get(0)));
        assertNotNull(single);
        assertEquals(1, single.size());

        assertThrows(IllegalArgumentException.class, () -> N.merge((Collection<Dataset>) null));
    }

    @Test
    public void testMerge_WithRequiresSameColumns() {
        Dataset ds1 = N.newDataset(Arrays.asList("col1", "col2"), N.asSingletonList(new Object[] { "A", 1 }));

        Dataset ds2 = N.newDataset(Arrays.asList("col1", "col2"), N.asSingletonList(new Object[] { "B", 2 }));

        Dataset ds3 = N.newDataset(Arrays.asList("col1", "col3"), N.asSingletonList(new Object[] { "C", 3 }));

        Dataset merged = N.merge(Arrays.asList(ds1, ds2), true);
        assertNotNull(merged);
        assertEquals(2, merged.size());

        assertThrows(IllegalArgumentException.class, () -> N.merge(Arrays.asList(ds1, ds3), true));
    }

    @Test
    public void testMerge_CollectionDatasets_RequiresSameColumns_Mismatch() {
        Dataset ds1 = N.newDataset(Arrays.asList("col1"), N.asSingletonList(new Object[] { "A" }));
        Dataset ds2 = N.newDataset(Arrays.asList("col2"), N.asSingletonList(new Object[] { 1 }));
        assertThrows(IllegalArgumentException.class, () -> N.merge(Arrays.asList(ds1, ds2), true));
    }

    // --- Tests for slice ---

    @Test
    public void testSlice_Array() {
        String[] arr = { "a", "b", "c", "d", "e" };
        List<String> sliced = N.slice(arr, 1, 3);
        assertEquals(2, sliced.size());
        assertEquals("b", sliced.get(0));
        assertEquals("c", sliced.get(1));
    }

    @Test
    public void testSlice_List() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        List<String> sliced = N.slice(list, 1, 4);
        assertEquals(3, sliced.size());
        assertEquals("b", sliced.get(0));
    }

    @Test
    public void testSlice_Collection() {
        Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        Collection<String> sliced = N.slice(set, 1, 3);
        assertEquals(2, sliced.size());
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
    public void testToArray_RangeWithArraySupplier() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        String[] array = N.toArray(list, 1, 4, String[]::new);
        assertArrayEquals(new String[] { "b", "c", "d" }, array);
    }

    // Tests for toArray(Collection, int, int, IntFunction) with non-List collection
    @Test
    public void testToArray_Collection_Range_IntFunction_LinkedList() {
        java.util.LinkedList<String> list = new java.util.LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));
        String[] result = N.toArray(list, 1, 4, String[]::new);
        assertArrayEquals(new String[] { "b", "c", "d" }, result);
    }

    // Tests for toArray(Collection, fromIndex, toIndex) with non-List path
    @Test
    public void testToArray_Collection_Range_NonList() {
        java.util.LinkedList<String> list = new java.util.LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));
        Object[] result = N.toArray(list, 1, 4);
        assertArrayEquals(new Object[] { "b", "c", "d" }, result);
    }

    @Test
    public void testToArray_01() {
        {
            Collection<String> list = N.toLinkedHashSet("a", "b", "c", "d", "e");
            assertArrayEquals(new Object[] { "b", "c", "d" }, N.toArray(list, 1, 4));
            assertArrayEquals(new Object[] { "b", "c", "d" }, N.toArray(list, 1, 4, new String[1]));
            assertArrayEquals(new Object[] { "b", "c", "d" }, N.toArray(list, 1, 4, String[]::new));
            assertArrayEquals(new Object[] { "b", "c", "d" }, N.toArray(list, 1, 4, String[].class));
        }
        {
            Collection<Boolean> c = N.toLinkedList(true, true, false, null, true);
            assertArrayEquals(new boolean[] { true, false, false }, N.toBooleanArray(c, 1, 4, false));
        }
        {
            Collection<Byte> c = N.toLinkedList((byte) 1, (byte) 2, (byte) 3, null, (byte) 4);
            assertArrayEquals(new byte[] { 2, 3, 0 }, N.toByteArray(c, 1, 4, (byte) 0));
        }
        {
            Collection<Short> c = N.toLinkedList((short) 1, (short) 2, (short) 3, null, (short) 4);
            assertArrayEquals(new short[] { 2, 3, 0 }, N.toShortArray(c, 1, 4, (short) 0));
        }
        {
            Collection<Integer> c = N.toLinkedList(1, 2, 3, null, 4);
            assertArrayEquals(new int[] { 2, 3, 0 }, N.toIntArray(c, 1, 4, 0));
        }
        {
            Collection<Long> c = N.toLinkedList(1L, 2L, 3L, null, 4L);
            assertArrayEquals(new long[] { 2L, 3L, 0L }, N.toLongArray(c, 1, 4, 0L));
        }
        {
            Collection<Float> c = N.toLinkedList(1.0f, 2.0f, 3.0f, null, 4.0f);
            assertArrayEquals(new float[] { 2.0f, 3.0f, 0.0f }, N.toFloatArray(c, 1, 4, 0.0f));
        }
        {
            Collection<Double> c = N.toLinkedList(1.0d, 2.0d, 3.0d, null, 4.0d);
            assertArrayEquals(new double[] { 2.0d, 3.0d, 0.0d }, N.toDoubleArray(c, 1, 4, 0.0d));
        }
    }

    @Test
    public void testToArray_WithArraySupplier() {
        List<String> list = Arrays.asList("a", "b", "c");
        String[] array = N.toArray(list, String[]::new);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);

        String[] emptyArray = N.toArray(new ArrayList<String>(), String[]::new);
        assertEquals(0, emptyArray.length);
    }

    @Test
    public void testToArray_Collection_Range_IntFunction_Empty() {
        List<String> list = Arrays.asList("a", "b", "c");
        String[] result = N.toArray(list, 1, 1, String[]::new);
        assertEquals(0, result.length);
    }

    @Test
    public void testToArray_Collection_Range_EmptySubRange() {
        List<String> list = Arrays.asList("a", "b", "c");
        Object[] result = N.toArray(list, 1, 1);
        assertEquals(0, result.length);
    }

    @Test
    public void testToArray_CollectionRange() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Object[] array = N.toArray(list, 1, 4);
        assertArrayEquals(new Object[] { "b", "c", "d" }, array);

        Object[] fullArray = N.toArray(list, 0, list.size());
        assertArrayEquals(new Object[] { "a", "b", "c", "d", "e" }, fullArray);

        Object[] emptyArray = N.toArray(list, 2, 2);
        assertEquals(0, emptyArray.length);

        assertThrows(IndexOutOfBoundsException.class, () -> N.toArray(list, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toArray(list, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toArray(list, 3, 2));
    }

    @Test
    public void testToArray_WithTargetArray() {
        List<String> list = Arrays.asList("a", "b", "c");

        String[] target = new String[5];
        String[] result = N.toArray(list, target);
        assertSame(target, result);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
        assertNull(result[3]);

        String[] smallTarget = new String[2];
        String[] newResult = N.toArray(list, smallTarget);
        assertNotSame(smallTarget, newResult);
        assertEquals(3, newResult.length);

        String[] emptyResult = N.toArray(new ArrayList<String>(), new String[0]);
        assertEquals(0, emptyResult.length);

        assertThrows(IllegalArgumentException.class, () -> N.toArray(list, (String[]) null));
    }

    @Test
    public void testToArray_RangeWithTargetArray() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        String[] target = new String[3];
        String[] result = N.toArray(list, 1, 4, target);
        assertSame(target, result);
        assertArrayEquals(new String[] { "b", "c", "d" }, result);

        assertThrows(IllegalArgumentException.class, () -> N.toArray(list, 1, 4, (String[]) null));
    }

    @Test
    public void testToArray_WithTargetType() {
        List<String> list = Arrays.asList("a", "b", "c");
        String[] array = N.toArray(list, String[].class);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);

        String[] emptyArray = N.toArray(new ArrayList<String>(), String[].class);
        assertEquals(0, emptyArray.length);

        assertThrows(IllegalArgumentException.class, () -> N.toArray(list, (String[]) null));
    }

    @Test
    public void testToArray_RangeWithTargetType() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        String[] array = N.toArray(list, 1, 4, String[].class);
        assertArrayEquals(new String[] { "b", "c", "d" }, array);

        assertThrows(IllegalArgumentException.class, () -> N.toArray(list, 1, 4, (String[]) null));
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
    public void testToBooleanArray_FromByteArray() {
        byte[] bytes = { 0, 1, -1, 127, -128 };
        boolean[] expected = { false, true, false, true, false };
        assertArrayEquals(expected, N.toBooleanArray(bytes));

        assertArrayEquals(new boolean[0], N.toBooleanArray(new byte[0]));
        assertArrayEquals(new boolean[0], N.toBooleanArray((byte[]) null));
    }

    @Test
    public void testToBooleanArray_FromIntArray() {
        int[] ints = { 0, 1, -1, 100, -100 };
        boolean[] expected = { false, true, false, true, false };
        assertArrayEquals(expected, N.toBooleanArray(ints));

        assertArrayEquals(new boolean[0], N.toBooleanArray(new int[0]));
        assertArrayEquals(new boolean[0], N.toBooleanArray((int[]) null));
    }

    // Tests for toBooleanArray/toByteArray etc. with List (RandomAccess) path
    @Test
    public void testToBooleanArray_Collection_Range_WithNull() {
        List<Boolean> list = Arrays.asList(true, null, false, true, null);
        boolean[] result = N.toBooleanArray(list, 1, 4, false);
        assertArrayEquals(new boolean[] { false, false, true }, result);
    }

    @Test
    public void testToBooleanArray_Range() {
        List<Boolean> list = Arrays.asList(true, false, true, null, false);

        boolean[] array = N.toBooleanArray(list, 1, 4);
        assertArrayEquals(new boolean[] { false, true, false }, array);

        boolean[] arrayWithDefault = N.toBooleanArray(list, 1, 4, true);
        assertArrayEquals(new boolean[] { false, true, true }, arrayWithDefault);

        boolean[] emptyArray = N.toBooleanArray(list, 2, 2);
        assertEquals(0, emptyArray.length);

        assertThrows(IndexOutOfBoundsException.class, () -> N.toBooleanArray(list, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toBooleanArray(list, 0, 6));
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
    public void testToCharArray_Range() {
        List<Character> list = Arrays.asList('a', 'b', 'c', null, 'd');

        char[] array = N.toCharArray(list, 1, 4);
        assertArrayEquals(new char[] { 'b', 'c', '\0' }, array);

        char[] arrayWithDefault = N.toCharArray(list, 1, 4, 'X');
        assertArrayEquals(new char[] { 'b', 'c', 'X' }, arrayWithDefault);
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
    public void testToByteArray_Range() {
        List<Number> list = Arrays.asList((byte) 1, 2, 3L, null, 5.0);

        byte[] array = N.toByteArray(list, 1, 4);
        assertArrayEquals(new byte[] { 2, 3, 0 }, array);

        byte[] arrayWithDefault = N.toByteArray(list, 1, 4, (byte) -1);
        assertArrayEquals(new byte[] { 2, 3, -1 }, arrayWithDefault);
    }

    @Test
    public void testToByteArray_FromBooleanArray() {
        boolean[] bools = { true, false, true, false };
        byte[] expected = { 1, 0, 1, 0 };
        assertArrayEquals(expected, N.toByteArray(bools));

        assertArrayEquals(new byte[0], N.toByteArray(new boolean[0]));
        assertArrayEquals(new byte[0], N.toByteArray((boolean[]) null));
    }

    @Test
    public void testToByteArray_Collection_Range_WithNull() {
        List<Byte> list = Arrays.asList((byte) 1, null, (byte) 3, (byte) 4);
        byte[] result = N.toByteArray(list, 1, 3, (byte) 99);
        assertArrayEquals(new byte[] { 99, 3 }, result);
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
    public void testToShortArray_Range() {
        List<Number> list = Arrays.asList((short) 1, 2, 3L, null, 5.0);

        short[] array = N.toShortArray(list, 1, 4);
        assertArrayEquals(new short[] { 2, 3, 0 }, array);

        short[] arrayWithDefault = N.toShortArray(list, 1, 4, (short) -1);
        assertArrayEquals(new short[] { 2, 3, -1 }, arrayWithDefault);
    }

    @Test
    public void testToShortArray_Collection_Range_WithNull() {
        List<Short> list = Arrays.asList((short) 1, null, (short) 3, (short) 4);
        short[] result = N.toShortArray(list, 1, 3, (short) 99);
        assertArrayEquals(new short[] { 99, 3 }, result);
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
    public void testToIntArray_Range() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        int[] array = N.toIntArray(list, 1, 4);
        assertArrayEquals(new int[] { 2, 3, 0 }, array);

        int[] arrayWithDefault = N.toIntArray(list, 1, 4, -1);
        assertArrayEquals(new int[] { 2, 3, -1 }, arrayWithDefault);
    }

    @Test
    public void testToIntArray_FromCharArray() {
        char[] chars = { 'A', 'B', 'C' };
        int[] expected = { 65, 66, 67 };
        assertArrayEquals(expected, N.toIntArray(chars));

        assertArrayEquals(new int[0], N.toIntArray(new char[0]));
        assertArrayEquals(new int[0], N.toIntArray((char[]) null));
    }

    @Test
    public void testToIntArray_FromBooleanArray() {
        boolean[] bools = { true, false, true, false };
        int[] expected = { 1, 0, 1, 0 };
        assertArrayEquals(expected, N.toIntArray(bools));

        assertArrayEquals(new int[0], N.toIntArray(new boolean[0]));
        assertArrayEquals(new int[0], N.toIntArray((boolean[]) null));
    }

    @Test
    public void testToIntArray_Collection_Range_WithNull() {
        List<Integer> list = Arrays.asList(1, null, 3, 4);
        int[] result = N.toIntArray(list, 1, 3, 99);
        assertArrayEquals(new int[] { 99, 3 }, result);
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
    public void testToLongArray_Range() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        long[] array = N.toLongArray(list, 1, 4);
        assertArrayEquals(new long[] { 2, 3, 0 }, array);

        long[] arrayWithDefault = N.toLongArray(list, 1, 4, -1L);
        assertArrayEquals(new long[] { 2, 3, -1 }, arrayWithDefault);
    }

    @Test
    public void testToLongArray_Collection_Range_WithNull() {
        List<Long> list = Arrays.asList(1L, null, 3L, 4L);
        long[] result = N.toLongArray(list, 1, 3, 99L);
        assertArrayEquals(new long[] { 99L, 3L }, result);
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
    public void testToFloatArray_Range() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        float[] array = N.toFloatArray(list, 1, 4);
        assertArrayEquals(new float[] { 2, 3, 0 }, array, 0.0f);

        float[] arrayWithDefault = N.toFloatArray(list, 1, 4, -1.0f);
        assertArrayEquals(new float[] { 2, 3, -1 }, arrayWithDefault, 0.0f);
    }

    @Test
    public void testToFloatArray_Collection_Range_WithNull() {
        List<Float> list = Arrays.asList(1.0f, null, 3.0f, 4.0f);
        float[] result = N.toFloatArray(list, 1, 3, 99.0f);
        assertArrayEquals(new float[] { 99.0f, 3.0f }, result, 0.0f);
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
    public void testToDoubleArray_Range() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        double[] array = N.toDoubleArray(list, 1, 4);
        assertArrayEquals(new double[] { 2, 3, 0 }, array, 0.0);

        double[] arrayWithDefault = N.toDoubleArray(list, 1, 4, -1.0);
        assertArrayEquals(new double[] { 2, 3, -1 }, arrayWithDefault, 0.0);
    }

    @Test
    public void testToDoubleArray_Collection_Range_WithNull() {
        List<Double> list = Arrays.asList(1.0, null, 3.0, 4.0);
        double[] result = N.toDoubleArray(list, 1, 3, 99.0);
        assertArrayEquals(new double[] { 99.0, 3.0 }, result, 0.0);
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
    public void testToList_CharArrayRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        List<Character> list = N.toList(array, 1, 4);
        assertEquals(Arrays.asList('b', 'c', 'd'), list);
    }

    @Test
    public void testToList_ByteArrayRange() {
        byte[] array = { 1, 2, 3, 4, 5 };
        List<Byte> list = N.toList(array, 1, 4);
        assertEquals(Arrays.asList((byte) 2, (byte) 3, (byte) 4), list);
    }

    @Test
    public void testToList_ShortArrayRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        List<Short> list = N.toList(array, 1, 4);
        assertEquals(Arrays.asList((short) 2, (short) 3, (short) 4), list);
    }

    @Test
    public void testToList_IntArrayRange() {
        int[] array = { 1, 2, 3, 4, 5 };
        List<Integer> list = N.toList(array, 1, 4);
        assertEquals(Arrays.asList(2, 3, 4), list);
    }

    @Test
    public void testToList_LongArrayRange() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        List<Long> list = N.toList(array, 1, 4);
        assertEquals(Arrays.asList(2L, 3L, 4L), list);
    }

    @Test
    public void testToList_FloatArrayRange() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        List<Float> list = N.toList(array, 1, 4);
        assertEquals(Arrays.asList(2.0f, 3.0f, 4.0f), list);
    }

    @Test
    public void testToList_DoubleArrayRange() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        List<Double> list = N.toList(array, 1, 4);
        assertEquals(Arrays.asList(2.0, 3.0, 4.0), list);
    }

    @Test
    public void testToList_ObjectArrayRange() {
        String[] array = { "a", "b", "c", "d", "e" };
        List<String> list = N.toList(array, 1, 4);
        assertEquals(Arrays.asList("b", "c", "d"), list);

        List<String> fullList = N.toList(array, 0, array.length);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), fullList);

        assertEquals(new ArrayList<>(), N.toList(array, 2, 2));
    }

    @Test
    public void testToList_BooleanArray() {
        boolean[] array = { true, false, true };
        List<Boolean> list = N.toList(array);
        assertEquals(Arrays.asList(true, false, true), list);

        assertEquals(new ArrayList<>(), N.toList(new boolean[0]));
        assertEquals(new ArrayList<>(), N.toList((boolean[]) null));
    }

    @Test
    public void testToList_CharArray() {
        char[] array = { 'a', 'b', 'c' };
        List<Character> list = N.toList(array);
        assertEquals(Arrays.asList('a', 'b', 'c'), list);

        assertEquals(new ArrayList<>(), N.toList(new char[0]));
        assertEquals(new ArrayList<>(), N.toList((char[]) null));
    }

    @Test
    public void testToList_ByteArray() {
        byte[] array = { 1, 2, 3 };
        List<Byte> list = N.toList(array);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), list);

        assertEquals(new ArrayList<>(), N.toList(new byte[0]));
        assertEquals(new ArrayList<>(), N.toList((byte[]) null));
    }

    @Test
    public void testToList_ShortArray() {
        short[] array = { 1, 2, 3 };
        List<Short> list = N.toList(array);
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), list);

        assertEquals(new ArrayList<>(), N.toList(new short[0]));
        assertEquals(new ArrayList<>(), N.toList((short[]) null));
    }

    @Test
    public void testToList_LongArray() {
        long[] array = { 1L, 2L, 3L };
        List<Long> list = N.toList(array);
        assertEquals(Arrays.asList(1L, 2L, 3L), list);

        assertEquals(new ArrayList<>(), N.toList(new long[0]));
        assertEquals(new ArrayList<>(), N.toList((long[]) null));
    }

    @Test
    public void testToList_FloatArray() {
        float[] array = { 1.0f, 2.0f, 3.0f };
        List<Float> list = N.toList(array);
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), list);

        assertEquals(new ArrayList<>(), N.toList(new float[0]));
        assertEquals(new ArrayList<>(), N.toList((float[]) null));
    }

    @Test
    public void testToList_DoubleArray() {
        double[] array = { 1.0, 2.0, 3.0 };
        List<Double> list = N.toList(array);
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), list);

        assertEquals(new ArrayList<>(), N.toList(new double[0]));
        assertEquals(new ArrayList<>(), N.toList((double[]) null));
    }

    @Test
    public void testToList_ObjectArray() {
        String[] array = { "a", "b", "c" };
        List<String> list = N.toList(array);
        assertEquals(Arrays.asList("a", "b", "c"), list);

        assertEquals(new ArrayList<>(), N.toList(new String[0]));
        assertEquals(new ArrayList<>(), N.toList((String[]) null));
    }

    @Test
    public void testToList_Iterator() {
        Iterator<String> iter = Arrays.asList("a", "b", "c").iterator();
        List<String> list = N.toList(iter);
        assertEquals(Arrays.asList("a", "b", "c"), list);

        List<String> nullList = N.toList((Iterator<String>) null);
        assertEquals(new ArrayList<>(), nullList);
    }

    @Test
    public void testToList_BooleanArrayRange() {
        boolean[] array = { true, false, true, false, true };
        List<Boolean> list = N.toList(array, 1, 4);
        assertEquals(Arrays.asList(false, true, false), list);

        assertEquals(new ArrayList<>(), N.toList(array, 2, 2));

        assertThrows(IndexOutOfBoundsException.class, () -> N.toList(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toList(array, 0, 6));
    }

    @Test
    public void testAsLinkedList() {
        LinkedList<String> list = N.toLinkedList("a");
        assertEquals(1, list.size());
        assertEquals("a", list.getFirst());

        list = N.toLinkedList("a", "b");
        assertEquals(2, list.size());

        list = N.toLinkedList("a", "b", "c");
        assertEquals(3, list.size());

        list = N.toLinkedList("a", "b", "c", "d");
        assertEquals(4, list.size());

        list = N.toLinkedList("a", "b", "c", "d", "e");
        assertEquals(5, list.size());

        list = N.toLinkedList("a", "b", "c", "d", "e", "f");
        assertEquals(6, list.size());

        list = N.toLinkedList("a", "b", "c", "d", "e", "f", "g");
        assertEquals(7, list.size());
    }

    // --- Tests for toLinkedList ---

    @Test
    public void testToLinkedList() {
        LinkedList<String> result = N.toLinkedList("a", "b", "c");
        assertEquals(3, result.size());
        assertEquals("a", result.getFirst());
        assertEquals("c", result.getLast());
    }

    @Test
    public void testAsLinkedList_VarArgs() {
        String[] array = { "a", "b", "c" };
        LinkedList<String> list = N.toLinkedList(array);
        assertEquals(3, list.size());

        LinkedList<String> emptyList = N.toLinkedList();
        assertTrue(emptyList.isEmpty());
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
    public void testToSet_BooleanArrayRange() {
        boolean[] array = { true, false, true, false, true };
        Set<Boolean> set = N.toSet(array, 1, 4);
        assertEquals(2, set.size());
        assertTrue(set.contains(true));
        assertTrue(set.contains(false));
    }

    @Test
    public void testToSet_CharArray() {
        char[] array = { 'a', 'b', 'c', 'a' };
        Set<Character> set = N.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains('a'));
        assertTrue(set.contains('b'));
        assertTrue(set.contains('c'));
    }

    @Test
    public void testToSet_CharArrayRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        Set<Character> set = N.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains('b'));
        assertTrue(set.contains('c'));
        assertTrue(set.contains('d'));
    }

    @Test
    public void testToSet_ByteArray() {
        byte[] array = { 1, 2, 3, 1, 2 };
        Set<Byte> set = N.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains((byte) 1));
        assertTrue(set.contains((byte) 2));
        assertTrue(set.contains((byte) 3));
    }

    @Test
    public void testToSet_ByteArrayRange() {
        byte[] array = { 1, 2, 3, 4, 5 };
        Set<Byte> set = N.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains((byte) 2));
        assertTrue(set.contains((byte) 3));
        assertTrue(set.contains((byte) 4));
    }

    @Test
    public void testToSet_ShortArray() {
        short[] array = { 1, 2, 3, 1, 2 };
        Set<Short> set = N.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains((short) 1));
        assertTrue(set.contains((short) 2));
        assertTrue(set.contains((short) 3));
    }

    @Test
    public void testToSet_ShortArrayRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        Set<Short> set = N.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains((short) 2));
        assertTrue(set.contains((short) 3));
        assertTrue(set.contains((short) 4));
    }

    @Test
    public void testToSet_IntArray() {
        int[] array = { 1, 2, 3, 1, 2 };
        Set<Integer> set = N.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
    }

    @Test
    public void testToSet_IntArrayRange() {
        int[] array = { 1, 2, 3, 4, 5 };
        Set<Integer> set = N.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
        assertTrue(set.contains(4));
    }

    @Test
    public void testToSet_LongArray() {
        long[] array = { 1L, 2L, 3L, 1L, 2L };
        Set<Long> set = N.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains(1L));
        assertTrue(set.contains(2L));
        assertTrue(set.contains(3L));
    }

    @Test
    public void testToSet_LongArrayRange() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        Set<Long> set = N.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains(2L));
        assertTrue(set.contains(3L));
        assertTrue(set.contains(4L));
    }

    @Test
    public void testToSet_FloatArray() {
        float[] array = { 1.0f, 2.0f, 3.0f, 1.0f, 2.0f };
        Set<Float> set = N.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains(1.0f));
        assertTrue(set.contains(2.0f));
        assertTrue(set.contains(3.0f));
    }

    @Test
    public void testToSet_FloatArrayRange() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        Set<Float> set = N.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains(2.0f));
        assertTrue(set.contains(3.0f));
        assertTrue(set.contains(4.0f));
    }

    @Test
    public void testToSet_DoubleArray() {
        double[] array = { 1.0, 2.0, 3.0, 1.0, 2.0 };
        Set<Double> set = N.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains(1.0));
        assertTrue(set.contains(2.0));
        assertTrue(set.contains(3.0));
    }

    @Test
    public void testToSet_DoubleArrayRange() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        Set<Double> set = N.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains(2.0));
        assertTrue(set.contains(3.0));
        assertTrue(set.contains(4.0));
    }

    @Test
    public void testToSet_ObjectArrayRange() {
        String[] array = { "a", "b", "c", "d", "e" };
        Set<String> set = N.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
        assertTrue(set.contains("d"));
    }

    @Test
    public void testToSet_BooleanArray() {
        boolean[] array = { true, false, true, false };
        Set<Boolean> set = N.toSet(array);
        assertEquals(2, set.size());
        assertTrue(set.contains(true));
        assertTrue(set.contains(false));

        assertEquals(new HashSet<>(), N.toSet(new boolean[0]));
        assertEquals(new HashSet<>(), N.toSet((boolean[]) null));
    }

    @Test
    public void testToSet_ObjectArray() {
        String[] array = { "a", "b", "c", "a", "b" };
        Set<String> set = N.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));

        assertEquals(new HashSet<>(), N.toSet(new String[0]));
        assertEquals(new HashSet<>(), N.toSet((String[]) null));
    }

    @Test
    public void testToSet_Iterator() {
        Iterator<String> iter = Arrays.asList("a", "b", "c", "a", "b").iterator();
        Set<String> set = N.toSet(iter);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));

        Set<String> nullSet = N.toSet((Iterator<String>) null);
        assertEquals(new HashSet<>(), nullSet);
    }

    @Test
    public void testAsLinkedHashSet() {
        Set<String> set = N.toLinkedHashSet("a");
        assertTrue(set instanceof LinkedHashSet);
        assertEquals(1, set.size());

        set = N.toLinkedHashSet("a", "b");
        assertEquals(2, set.size());

        set = N.toLinkedHashSet("a", "b", "c");
        assertEquals(3, set.size());

        set = N.toLinkedHashSet("a", "b", "c", "d");
        assertEquals(4, set.size());

        set = N.toLinkedHashSet("a", "b", "c", "d", "e");
        assertEquals(5, set.size());

        set = N.toLinkedHashSet("a", "b", "c", "d", "e", "f");
        assertEquals(6, set.size());

        set = N.toLinkedHashSet("a", "b", "c", "d", "e", "f", "g");
        assertEquals(7, set.size());

        Iterator<String> iter = set.iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
    }

    @Test
    public void testAsLinkedHashSet_VarArgs() {
        String[] array = { "a", "b", "c" };
        Set<String> set = N.toLinkedHashSet(array);
        assertTrue(set instanceof LinkedHashSet);
        assertEquals(3, set.size());
    }

    // --- Tests for toLinkedHashSet ---

    @Test
    public void testToLinkedHashSet() {
        Set<String> result = N.toLinkedHashSet("a", "b", "c", "a");
        assertEquals(3, result.size());
        // Preserves insertion order
        Iterator<String> it = result.iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
    }

    @Test
    public void testAsSortedSet() {
        java.util.SortedSet<Integer> set = N.toSortedSet(3, 1, 2);
        assertEquals(3, set.size());
        assertEquals(Integer.valueOf(1), set.first());
        assertEquals(Integer.valueOf(3), set.last());
    }

    // --- Tests for toSortedSet ---

    @Test
    public void testToSortedSet() {
        SortedSet<String> result = N.toSortedSet("c", "a", "b");
        assertEquals(3, result.size());
        assertEquals("a", result.first());
        assertEquals("c", result.last());
    }

    @Test
    public void testAsNavigableSet() {
        java.util.NavigableSet<Integer> set = N.toNavigableSet(3, 1, 2);
        assertEquals(3, set.size());
        assertEquals(Integer.valueOf(1), set.first());
        assertEquals(Integer.valueOf(3), set.last());
    }

    // --- Tests for toNavigableSet ---

    @Test
    public void testToNavigableSet() {
        NavigableSet<String> result = N.toNavigableSet("c", "a", "b");
        assertEquals(3, result.size());
        assertEquals("a", result.first());
        assertEquals("b", result.lower("c"));
    }

    @Test
    public void testAsQueue() {
        java.util.Queue<String> queue = N.toQueue("a", "b", "c");
        assertEquals(3, queue.size());
        assertEquals("a", queue.peek());
    }

    @Test
    public void testAsLinkedBlockingQueue() {
        java.util.concurrent.LinkedBlockingQueue<String> queue = N.toLinkedBlockingQueue("a", "b");
        assertEquals(2, queue.size());
        assertEquals("a", queue.peek());
    }

    @Test
    public void testAsConcurrentLinkedQueue() {
        java.util.concurrent.ConcurrentLinkedQueue<String> queue = N.toConcurrentLinkedQueue("a", "b");
        assertEquals(2, queue.size());
        assertEquals("a", queue.peek());
    }

    @Test
    public void testAsDelayQueue() {
        java.util.concurrent.DelayQueue<java.util.concurrent.Delayed> queue = N.toDelayQueue();
        assertNotNull(queue);
        assertTrue(queue.isEmpty());
    }

    @Test
    public void testAsPriorityQueue() {
        java.util.PriorityQueue<Integer> queue = N.toPriorityQueue(3, 1, 2);
        assertEquals(3, queue.size());
        assertEquals(Integer.valueOf(1), queue.poll());
        assertEquals(Integer.valueOf(2), queue.poll());
        assertEquals(Integer.valueOf(3), queue.poll());
    }

    @Test
    public void testAsDeque() {
        java.util.Deque<String> deque = N.toDeque("a", "b", "c");
        assertEquals(3, deque.size());
        assertEquals("a", deque.getFirst());
        assertEquals("c", deque.getLast());
    }

    @Test
    public void testToDeque() {
        Deque<String> result = N.toDeque("a", "b", "c");
        assertEquals(3, result.size());
    }

    @Test
    public void testToArrayDeque() {
        ArrayDeque<String> result = N.toArrayDeque("a", "b", "c");
        assertEquals(3, result.size());
    }

    @Test
    public void testAsLinkedBlockingDeque() {
        java.util.concurrent.LinkedBlockingDeque<String> deque = N.toLinkedBlockingDeque("a", "b");
        assertEquals(2, deque.size());
        assertEquals("a", deque.getFirst());
    }

    @Test
    public void testAsConcurrentLinkedDeque() {
        java.util.concurrent.ConcurrentLinkedDeque<String> deque = N.toConcurrentLinkedDeque("a", "b");
        assertEquals(2, deque.size());
        assertEquals("a", deque.getFirst());
    }

    @Test
    public void testAsMultiset() {
        com.landawn.abacus.util.Multiset<String> multiset = N.toMultiset("a", "b", "a", "c");
        assertEquals(4, multiset.size());
        assertEquals(2, multiset.count("a"));
        assertEquals(1, multiset.count("b"));
    }

    // --- Tests for toQueue, toDeque, toMultiset ---

    @Test
    public void testToQueue() {
        Queue<String> result = N.toQueue("a", "b", "c");
        assertEquals(3, result.size());
        assertEquals("a", result.peek());
    }

    @Test
    public void testToMultiset() {
        Multiset<String> result = N.toMultiset("a", "b", "a");
        assertEquals(2, result.count("a"));
        assertEquals(1, result.count("b"));
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
    public void testToCollection_BooleanArray() {
        boolean[] array = { true, false, true };
        List<Boolean> list = N.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(true, false, true), list);

        LinkedList<Boolean> linkedList = N.toCollection(array, size -> new LinkedList<>());
        assertEquals(3, linkedList.size());
    }

    @Test
    public void testToCollection_BooleanArrayRange() {
        boolean[] array = { true, false, true, false, true };
        List<Boolean> list = N.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(false, true, false), list);
    }

    @Test
    public void testToCollection_CharArray() {
        char[] array = { 'a', 'b', 'c' };
        List<Character> list = N.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList('a', 'b', 'c'), list);
    }

    @Test
    public void testToCollection_CharArrayRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        List<Character> list = N.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList('b', 'c', 'd'), list);
    }

    @Test
    public void testToCollection_ByteArray() {
        byte[] array = { 1, 2, 3 };
        List<Byte> list = N.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), list);
    }

    @Test
    public void testToCollection_ByteArrayRange() {
        byte[] array = { 1, 2, 3, 4, 5 };
        List<Byte> list = N.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList((byte) 2, (byte) 3, (byte) 4), list);
    }

    @Test
    public void testToCollection_ShortArray() {
        short[] array = { 1, 2, 3 };
        List<Short> list = N.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), list);
    }

    @Test
    public void testToCollection_ShortArrayRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        List<Short> list = N.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList((short) 2, (short) 3, (short) 4), list);
    }

    @Test
    public void testToCollection_IntArray() {
        int[] array = { 1, 2, 3 };
        List<Integer> list = N.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testToCollection_IntArrayRange() {
        int[] array = { 1, 2, 3, 4, 5 };
        List<Integer> list = N.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(2, 3, 4), list);
    }

    @Test
    public void testToCollection_LongArray() {
        long[] array = { 1L, 2L, 3L };
        List<Long> list = N.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(1L, 2L, 3L), list);
    }

    @Test
    public void testToCollection_LongArrayRange() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        List<Long> list = N.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(2L, 3L, 4L), list);
    }

    @Test
    public void testToCollection_FloatArray() {
        float[] array = { 1.0f, 2.0f, 3.0f };
        List<Float> list = N.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), list);
    }

    @Test
    public void testToCollection_FloatArrayRange() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        List<Float> list = N.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(2.0f, 3.0f, 4.0f), list);
    }

    @Test
    public void testToCollection_DoubleArray() {
        double[] array = { 1.0, 2.0, 3.0 };
        List<Double> list = N.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), list);
    }

    @Test
    public void testToCollection_DoubleArrayRange() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        List<Double> list = N.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(2.0, 3.0, 4.0), list);
    }

    @Test
    public void testToCollection_ObjectArrayRange() {
        String[] array = { "a", "b", "c", "d", "e" };
        List<String> list = N.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList("b", "c", "d"), list);
    }

    @Test
    public void testToCollection_ObjectArray() {
        String[] array = { "a", "b", "c" };
        List<String> list = N.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList("a", "b", "c"), list);

        List<String> emptyList = N.toCollection(new String[0], size -> new ArrayList<>(size));
        assertEquals(new ArrayList<>(), emptyList);
    }

    @Test
    public void testToCollection_Iterable() {
        {
            List<String> source = Arrays.asList("a", "b", "c");
            Set<String> set = N.toCollection(source, size -> new HashSet<>(size));
            assertEquals(3, set.size());
            assertTrue(set.contains("a"));
            assertTrue(set.contains("b"));
            assertTrue(set.contains("c"));

            Set<String> emptySet = N.toCollection(new ArrayList<String>(), size -> new HashSet<>(size));
            assertTrue(emptySet.isEmpty());
        }
        {
            Iterable<String> source = createIterable("a", "b", "c");
            Set<String> set = N.toCollection(source, size -> new HashSet<>(size));
            assertEquals(3, set.size());
            assertTrue(set.contains("a"));
            assertTrue(set.contains("b"));
            assertTrue(set.contains("c"));
        }

    }

    @Test
    public void testToCollection_Iterator() {
        Iterator<String> iter = Arrays.asList("a", "b", "c").iterator();
        List<String> list = N.toCollection(iter, () -> new ArrayList<>());
        assertEquals(Arrays.asList("a", "b", "c"), list);

        List<String> nullList = N.toCollection((Iterator<String>) null, () -> new ArrayList<>());
        assertTrue(nullList.isEmpty());
    }

    // ========== toCollection(T[], fromIndex, toIndex, supplier) - full-array fast path ==========

    @Test
    public void testToCollection_ObjectArray_Range_WithSupplier() {
        String[] arr = { "a", "b", "c" };
        // fromIndex==1, toIndex==3 -> range path
        List<String> result = N.toCollection(arr, 1, 3, n -> new ArrayList<>(n));
        assertEquals(Arrays.asList("b", "c"), result);
    }

    @Test
    public void testToCollection_ObjectArray_EmptyRange_WithSupplier() {
        String[] arr = { "a", "b", "c" };
        List<String> result = N.toCollection(arr, 1, 1, n -> new ArrayList<>(n));
        assertTrue(result.isEmpty());
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
    public void testToMap_Iterator_KeyExtractor() {
        List<String> words = Arrays.asList("apple", "banana", "cherry");
        Map<Integer, String> map = N.toMap(words.iterator(), String::length);
        assertNotNull(map);
        assertEquals("apple", map.get(5)); // "apple" has length 5
        // null iterator returns empty map
        Map<Integer, String> empty = N.toMap((java.util.Iterator<String>) null, String::length);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testToMap_Iterator_KeyExtractor_ValueExtractor() {
        List<String> words = Arrays.asList("apple", "banana");
        Map<String, Integer> map = N.toMap(words.iterator(), java.util.function.Function.identity(), String::length);
        assertNotNull(map);
        assertEquals(5, map.get("apple"));
        assertEquals(6, map.get("banana"));
    }

    @Test
    public void testToMap_Iterator_KeyExtractor_ValueExtractor_Supplier() {
        List<String> words = Arrays.asList("apple", "banana");
        Map<String, Integer> map = N.toMap(words.iterator(), java.util.function.Function.identity(), String::length, LinkedHashMap::new);
        assertNotNull(map);
        assertEquals(5, map.get("apple"));
        assertTrue(map instanceof LinkedHashMap);
    }

    @Test
    public void testToMap_Iterable_KeyExtractor_ValueExtractor_MapSupplier() {
        List<String> words = Arrays.asList("apple", "banana", "cherry");
        Map<String, Integer> map = N.toMap(words, java.util.function.Function.identity(), String::length,
                (java.util.function.IntFunction<LinkedHashMap<String, Integer>>) n -> new LinkedHashMap<>(n));
        assertNotNull(map);
        assertEquals(5, map.get("apple"));
        assertEquals(3, map.size());
        assertTrue(map instanceof LinkedHashMap);
    }

    // ========== toMap(Iterable, keyExtractor) - empty Iterable ==========

    @Test
    public void testToMap_Iterable_KeyExtractor_EmptyIterable() {
        List<String> empty = new ArrayList<>();
        Map<Integer, String> result = N.toMap(empty, String::length);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToMap_Iterable_KeyExtractor_NonEmpty() {
        List<String> words = Arrays.asList("hi", "hello");
        Map<Integer, String> result = N.toMap(words, String::length);
        assertNotNull(result);
        assertEquals("hi", result.get(2));
        assertEquals("hello", result.get(5));
    }

    // ========== toMap(Iterator, keyExtractor, valueExtractor, mergeFunction, mapSupplier) ==========

    @Test
    public void testToMap_Iterator_MergeFunction_NullIter() {
        Map<String, Integer> result = N.toMap((Iterator<String>) null, s -> s, String::length, Integer::sum, LinkedHashMap::new);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToMap_Iterator_MergeFunction_DuplicateKeys() {
        List<String> words = Arrays.asList("ab", "cd", "ef");
        // All have length 2, so mergeFunction (sum) gets called for duplicates
        Map<Integer, Integer> result = N.toMap(words.iterator(), String::length, String::length, Integer::sum, HashMap::new);
        assertNotNull(result);
        // key=2, values 2+2+2=6
        assertEquals(6, result.get(2));
    }

    // ========== toMap(Iterable, key, value, mergeFunction, mapSupplier) ==========

    @Test
    public void testToMap_Iterable_MergeFunction_EmptyIterable() {
        Map<String, Integer> result = N.toMap(new ArrayList<String>(), s -> s, String::length, Integer::sum,
                (java.util.function.IntFunction<LinkedHashMap<String, Integer>>) n -> new LinkedHashMap<>(n));
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToMap_Iterable_MergeFunction_DuplicateKeys() {
        List<String> words = Arrays.asList("aa", "bb", "aa");
        Map<String, Integer> result = N.toMap(words, s -> s, String::length, Integer::sum,
                (java.util.function.IntFunction<HashMap<String, Integer>>) n -> new HashMap<>(n));
        assertNotNull(result);
        // "aa" encountered twice with value 2 each time, merged by sum => 4
        assertEquals(4, result.get("aa"));
        assertEquals(2, result.get("bb"));
    }

    //     @Test
    //     public void testAsLinkedHashMap() {
    //         Map<String, Integer> map = N.asLinkedHashMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9);
    //         assertEquals(9, map.size());
    //         assertEquals(Integer.valueOf(1), map.get("a"));
    //         assertEquals(Integer.valueOf(2), map.get("b"));
    //         assertEquals(Integer.valueOf(9), map.get("i"));
    //     }

    //

    @Test
    public void testToLinkedHashMap() {
        Map<String, Integer> map = N.toLinkedHashMap("a", 1);
        assertTrue(map instanceof LinkedHashMap);
        assertEquals(1, map.size());

        map = N.toLinkedHashMap("a", 1, "b", 2);
        assertEquals(2, map.size());

        map = N.toLinkedHashMap("a", 1, "b", 2, "c", 3);
        assertEquals(3, map.size());

        map = N.toLinkedHashMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
        assertEquals(5, map.size());

        Iterator<String> iter = map.keySet().iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals("d", iter.next());
        assertEquals("e", iter.next());

        assertThrows(IllegalArgumentException.class, () -> N.toLinkedHashMap("a", 1, "b"));
    }

    @Test
    public void testAsMap() {
        Map<String, Integer> map = N.asMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9);
        assertTrue(map instanceof ImmutableMap);
        assertEquals(9, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertEquals(Integer.valueOf(3), map.get("c"));
        assertEquals(Integer.valueOf(9), map.get("i"));
        assertThrows(UnsupportedOperationException.class, () -> map.put("j", 10));
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
    public void testHaveSameElements() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(3, 2, 1);
        assertTrue(N.containsSameElements(list1, list2));

        List<Integer> list3 = Arrays.asList(1, 2, 4);
        assertFalse(N.containsSameElements(list1, list3));
    }

    @Test
    public void checkArgNotEmpty_collection_valid() {
        List<String> list = Arrays.asList("a", "b");
        assertSame(list, N.checkArgNotEmpty(list, "coll"));
    }

    @Test
    public void checkArgNotEmpty_iterable_valid() {
        Iterable<String> iterable = Arrays.asList("a", "b");
        assertSame(iterable, N.checkArgNotEmpty(iterable, "iterable"));
    }

    @Test
    public void checkArgNotEmpty_iterator_valid() {
        Iterator<String> iterator = Arrays.asList("a", "b").iterator();
        assertSame(iterator, N.checkArgNotEmpty(iterator, "iterator"));
    }

    @Test
    public void checkArgNotEmpty_dataset_valid() {
        List<String> columnNames = Arrays.asList("col1");
        List<List<?>> rows = new ArrayList<>();
        rows.add(Arrays.asList("val1"));
        Dataset dataset = N.newDataset(columnNames, rows);
        assertSame(dataset, N.checkArgNotEmpty(dataset, "dataset"));
    }

    @Test
    public void testHaveSameElementsCollection() {
        List<String> a = Arrays.asList("a", "b", "c", "b");
        List<String> b = Arrays.asList("b", "a", "c", "b");
        List<String> c = Arrays.asList("a", "b", "b", "c");
        List<String> d = Arrays.asList("a", "b", "c");

        Assertions.assertTrue(N.containsSameElements(a, b));
        Assertions.assertTrue(N.containsSameElements(a, c));
        Assertions.assertFalse(N.containsSameElements(a, d));
        Assertions.assertTrue(N.containsSameElements((Collection<?>) null, (Collection<?>) null));
        Assertions.assertTrue(N.containsSameElements(new ArrayList<>(), new ArrayList<>()));

        Assertions.assertTrue(N.containsSameElements(a, a));
    }

    @Test
    public void testAsList() {
        List<String> list = N.asList("a", "b", "c");
        assertTrue(list instanceof ImmutableList);
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));

        List<String> listWithNull = N.asList("a", null, "c");
        assertEquals(3, listWithNull.size());
        assertNull(listWithNull.get(1));

        assertThrows(UnsupportedOperationException.class, () -> list.add("d"));
    }

    @Test
    public void testAsList_VarArgs() {
        String[] array = { "a", "b", "c" };
        List<String> list = N.asList(array);
        assertTrue(list instanceof ImmutableList);
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));

        List<String> listWithNull = N.asList("a", null, "c");
        assertEquals(3, listWithNull.size());
        assertNull(listWithNull.get(1));

        List<String> emptyList = N.asList();
        assertTrue(emptyList.isEmpty());

        List<String> nullList = N.asList((String[]) null);
        assertTrue(nullList.isEmpty());

        final List<String> immutableList = list;
        assertThrows(UnsupportedOperationException.class, () -> immutableList.add("d"));
    }

    @Test
    public void testAsSet() {
        Set<String> set = N.asSet("a", "b", "c", "a");
        assertTrue(set instanceof ImmutableSet);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));

        Set<String> setWithNull = N.asSet("a", null, "a");
        assertEquals(2, setWithNull.size());
        assertTrue(setWithNull.contains(null));

        assertThrows(UnsupportedOperationException.class, () -> set.add("d"));
    }

    @Test
    public void testAsSet_VarArgs() {
        String[] array = { "a", "b", "c", "a" };
        Set<String> set = N.asSet(array);
        assertTrue(set instanceof ImmutableSet);
        assertEquals(3, set.size());

        Set<String> setWithNull = N.asSet("a", null, "a");
        assertEquals(2, setWithNull.size());
        assertTrue(setWithNull.contains(null));

        Set<String> emptySet = N.asSet();
        assertTrue(emptySet.isEmpty());

        final Set<String> immutableSet = set;
        assertThrows(UnsupportedOperationException.class, () -> immutableSet.add("d"));
    }

    @Test
    public void testAsSingletonList() {
        List<String> list = N.asSingletonList("test");
        assertEquals(1, list.size());
        assertEquals("test", list.get(0));
    }

    @Test
    public void testAsSingletonListWithNull() {
        List<String> list = N.asSingletonList(null);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertNull(list.get(0));
    }

    @Test
    public void testAsSingletonListWithDifferentTypes() {
        List<Integer> intList = N.asSingletonList(42);
        assertEquals(42, intList.get(0));

        List<Double> doubleList = N.asSingletonList(3.14);
        assertEquals(3.14, doubleList.get(0));

        List<Object> objList = N.asSingletonList(new Object());
        assertNotNull(objList.get(0));
    }

    @Test
    public void testAsSingletonSet() {
        Set<String> set = N.asSingletonSet("test");
        assertEquals(1, set.size());
        assertTrue(set.contains("test"));
    }

    @Test
    public void testAsSingletonSetWithNull() {
        Set<String> set = N.asSingletonSet(null);

        assertNotNull(set);
        assertEquals(1, set.size());
        assertTrue(set.contains(null));
    }

    @Test
    public void testAsSingletonSetWithDifferentTypes() {
        Set<Integer> intSet = N.asSingletonSet(100);
        assertTrue(intSet.contains(100));

        Set<Boolean> boolSet = N.asSingletonSet(true);
        assertTrue(boolSet.contains(true));
    }

    @Test
    public void testAsSingletonMap() {
        Map<String, Integer> map = N.asSingletonMap("key", 100);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(100), map.get("key"));
    }

    @Test
    public void testAsSingletonMapWithNullKey() {
        Map<String, String> map = N.asSingletonMap(null, "value");

        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals("value", map.get(null));
        assertTrue(map.containsKey(null));
    }

    @Test
    public void testAsSingletonMapWithNullValue() {
        Map<String, String> map = N.asSingletonMap("key", null);

        assertNotNull(map);
        assertEquals(1, map.size());
        assertNull(map.get("key"));
        assertTrue(map.containsKey("key"));
    }

    @Test
    public void testAsSingletonMapWithNullKeyAndValue() {
        Map<String, String> map = N.asSingletonMap(null, null);

        assertNotNull(map);
        assertEquals(1, map.size());
        assertNull(map.get(null));
        assertTrue(map.containsKey(null));
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
    public void testAsArrayDeque() {
        java.util.ArrayDeque<String> deque = N.toArrayDeque("a", "b", "c");
        assertEquals(3, deque.size());
        assertEquals("a", deque.getFirst());
        assertEquals("c", deque.getLast());
    }

    @Test
    public void testAsArrayBlockingQueue() {
        java.util.concurrent.ArrayBlockingQueue<String> queue = N.toArrayBlockingQueue("a", "b");
        assertEquals(2, queue.size());
        assertEquals("a", queue.peek());
    }

    @Test
    public void testEmptyListReturnsSameInstance() {
        List<String> list1 = N.emptyList();
        List<Integer> list2 = N.emptyList();

        assertSame(list1, list2);
    }

    @Test
    public void testEmptyList() {
        List<String> list = N.emptyList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> list.add("test"));
    }

    @Test
    public void testEmptySetReturnsSameInstance() {
        Set<String> set1 = N.emptySet();
        Set<Integer> set2 = N.emptySet();

        assertSame(set1, set2);
    }

    @Test
    public void testEmptySet() {
        Set<String> set = N.emptySet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> set.add("test"));
    }

    @Test
    public void testEmptySortedSetReturnsSameInstance() {
        SortedSet<String> set1 = N.emptySortedSet();
        SortedSet<Integer> set2 = N.emptySortedSet();

        assertSame(set1, set2);
    }

    @Test
    public void testEmptySortedSet() {
        java.util.SortedSet<String> set = N.emptySortedSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> set.add("test"));
    }

    @Test
    public void testEmptyNavigableSetReturnsSameInstance() {
        NavigableSet<String> set1 = N.emptyNavigableSet();
        NavigableSet<Integer> set2 = N.emptyNavigableSet();

        assertSame(set1, set2);
    }

    @Test
    public void testEmptyNavigableSet() {
        java.util.NavigableSet<String> set = N.emptyNavigableSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> set.add("test"));
    }

    @Test
    public void testEmptyMapReturnsSameInstance() {
        Map<String, String> map1 = N.emptyMap();
        Map<Integer, Integer> map2 = N.emptyMap();

        assertSame(map1, map2);
    }

    @Test
    public void testEmptyMap() {
        Map<String, String> map = N.emptyMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> map.put("key", "value"));
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
    public void testEmptyInputStream() {
        java.io.InputStream is = N.emptyInputStream();
        assertNotNull(is);
        assertDoesNotThrow(() -> assertEquals(-1, is.read()));
    }

    @Test
    public void testEmptyDataset() {
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
    public void testCompare_Collection_WithRange() {
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("x", "b", "c", "y");

        assertEquals(0, N.compare(a, 1, b, 1, 2));
    }

    @Test
    public void testCompareCharArraysWithRange001() {

        {
            char[] arr1 = { 1, 2, 3, 4 };
            char[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, N.compare(arr1, 0, arr2, 1, 3));
        }

        {
            byte[] arr1 = { 1, 2, 3, 4 };
            byte[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, N.compare(arr1, 0, arr2, 1, 3));
        }

        {
            short[] arr1 = { 1, 2, 3, 4 };
            short[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, N.compare(arr1, 0, arr2, 1, 3));
        }

        {
            int[] arr1 = { 1, 2, 3, 4 };
            int[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, N.compare(arr1, 0, arr2, 1, 3));
        }

        {
            long[] arr1 = { 1, 2, 3, 4 };
            long[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, N.compare(arr1, 0, arr2, 1, 3));
        }

        {
            float[] arr1 = { 1, 2, 3, 4 };
            float[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, N.compare(arr1, 0, arr2, 1, 3));
        }

        {
            double[] arr1 = { 1, 2, 3, 4 };
            double[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, N.compare(arr1, 0, arr2, 1, 3));
        }

    }

    @Test
    public void testCompareByteArrays() {
        Assertions.assertEquals(0, N.compare(new byte[] {}, new byte[] {}));
        Assertions.assertEquals(-1, N.compare(new byte[] {}, new byte[] { 1 }));
        Assertions.assertEquals(1, N.compare(new byte[] { 1 }, new byte[] {}));

        byte[] arr1 = { 1, 2, 3 };
        byte[] arr2 = { 1, 2, 3 };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        Assertions.assertEquals(-1, N.compare(new byte[] { 1 }, new byte[] { 2 }));
        Assertions.assertEquals(1, N.compare(new byte[] { 2 }, new byte[] { 1 }));

        Assertions.assertEquals(-1, N.compare(new byte[] { -128 }, new byte[] { 127 }));
        Assertions.assertEquals(1, N.compare(new byte[] { 127 }, new byte[] { -128 }));
    }

    @Test
    public void testCompareByteArraysWithRange() {
        byte[] arr1 = { 1, 2, 3, 4 };
        byte[] arr2 = { 5, 2, 3, 6 };

        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
        Assertions.assertEquals(-1, N.compare(arr1, 3, arr2, 0, 1));
    }

    @Test
    public void testCompareShortArrays() {
        Assertions.assertEquals(0, N.compare(new short[] {}, new short[] {}));
        Assertions.assertEquals(-1, N.compare(new short[] {}, new short[] { 1 }));
        Assertions.assertEquals(1, N.compare(new short[] { 1 }, new short[] {}));

        short[] arr1 = { 1, 2, 3 };
        short[] arr2 = { 1, 2, 3 };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        Assertions.assertEquals(-1, N.compare(new short[] { 1 }, new short[] { 2 }));
        Assertions.assertEquals(1, N.compare(new short[] { 2 }, new short[] { 1 }));
    }

    @Test
    public void testCompareShortArraysWithRange() {
        short[] arr1 = { 1, 2, 3, 4 };
        short[] arr2 = { 5, 2, 3, 6 };

        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
    }

    @Test
    public void testCompareIntArrays() {
        Assertions.assertEquals(0, N.compare(new int[] {}, new int[] {}));
        Assertions.assertEquals(-1, N.compare(new int[] {}, new int[] { 1 }));
        Assertions.assertEquals(1, N.compare(new int[] { 1 }, new int[] {}));

        int[] arr1 = { 1, 2, 3 };
        int[] arr2 = { 1, 2, 3 };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        Assertions.assertEquals(-1, N.compare(new int[] { 1 }, new int[] { 2 }));
        Assertions.assertEquals(1, N.compare(new int[] { 2 }, new int[] { 1 }));
    }

    @Test
    public void testCompareIntArraysWithRange() {
        int[] arr1 = { 1, 2, 3, 4 };
        int[] arr2 = { 5, 2, 3, 6 };

        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
    }

    @Test
    public void testCompareLongArrays() {
        Assertions.assertEquals(0, N.compare(new long[] {}, new long[] {}));
        Assertions.assertEquals(-1, N.compare(new long[] {}, new long[] { 1 }));
        Assertions.assertEquals(1, N.compare(new long[] { 1 }, new long[] {}));

        long[] arr1 = { 1L, 2L, 3L };
        long[] arr2 = { 1L, 2L, 3L };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        Assertions.assertEquals(-1, N.compare(new long[] { 1L }, new long[] { 2L }));
        Assertions.assertEquals(1, N.compare(new long[] { 2L }, new long[] { 1L }));
    }

    @Test
    public void testCompareLongArraysWithRange() {
        long[] arr1 = { 1L, 2L, 3L, 4L };
        long[] arr2 = { 5L, 2L, 3L, 6L };

        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
    }

    @Test
    public void testCompareFloatArrays() {
        Assertions.assertEquals(0, N.compare(new float[] {}, new float[] {}));
        Assertions.assertEquals(-1, N.compare(new float[] {}, new float[] { 1.0f }));
        Assertions.assertEquals(1, N.compare(new float[] { 1.0f }, new float[] {}));

        float[] arr1 = { 1.0f, 2.0f, 3.0f };
        float[] arr2 = { 1.0f, 2.0f, 3.0f };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        Assertions.assertEquals(-1, N.compare(new float[] { 1.0f }, new float[] { 2.0f }));
        Assertions.assertEquals(1, N.compare(new float[] { 2.0f }, new float[] { 1.0f }));

        Assertions.assertTrue(N.compare(new float[] { Float.NaN }, new float[] { 1.0f }) > 0);
        Assertions.assertEquals(0, N.compare(new float[] { Float.NaN }, new float[] { Float.NaN }));
    }

    @Test
    public void testCompareFloatArraysWithRange() {
        float[] arr1 = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] arr2 = { 5.0f, 2.0f, 3.0f, 6.0f };

        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
    }

    @Test
    public void testCompareDoubleArrays() {
        Assertions.assertEquals(0, N.compare(new double[] {}, new double[] {}));
        Assertions.assertEquals(-1, N.compare(new double[] {}, new double[] { 1.0 }));
        Assertions.assertEquals(1, N.compare(new double[] { 1.0 }, new double[] {}));

        double[] arr1 = { 1.0, 2.0, 3.0 };
        double[] arr2 = { 1.0, 2.0, 3.0 };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        Assertions.assertEquals(-1, N.compare(new double[] { 1.0 }, new double[] { 2.0 }));
        Assertions.assertEquals(1, N.compare(new double[] { 2.0 }, new double[] { 1.0 }));

        Assertions.assertTrue(N.compare(new double[] { Double.NaN }, new double[] { 1.0 }) > 0);
        Assertions.assertEquals(0, N.compare(new double[] { Double.NaN }, new double[] { Double.NaN }));
    }

    @Test
    public void testCompareDoubleArraysWithRange() {
        double[] arr1 = { 1.0, 2.0, 3.0, 4.0 };
        double[] arr2 = { 5.0, 2.0, 3.0, 6.0 };

        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
    }

    @Test
    public void testCompareObjectArraysWithRangeAndComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        String[] arr1 = { "a", "b", "c", "d" };
        String[] arr2 = { "x", "b", "c", "y" };

        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2, reverseComparator));

        Assertions.assertTrue(N.compare(arr1, 0, arr2, 0, 1, reverseComparator) > 0);
    }

    @Test
    public void testCompareCollectionsWithRangeAndComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        List<String> list1 = Arrays.asList("a", "b", "c", "d");
        List<String> list2 = Arrays.asList("x", "b", "c", "y");

        Assertions.assertEquals(0, N.compare(list1, 1, list2, 1, 2, reverseComparator));

        Assertions.assertTrue(N.compare(list1, 0, list2, 0, 1, reverseComparator) > 0);

        Set<String> set1 = new LinkedHashSet<>(list1);
        Set<String> set2 = new LinkedHashSet<>(list2);
        Assertions.assertEquals(0, N.compare(set1, 1, set2, 1, 2, reverseComparator));
    }

    @Test
    public void testCompareBoolean() {
        assertEquals(0, N.compare(true, true));
        assertEquals(0, N.compare(false, false));
        assertEquals(1, N.compare(true, false));
        assertEquals(-1, N.compare(false, true));
    }

    @Test
    public void testCompareChar() {
        assertEquals(0, N.compare('a', 'a'));
        assertEquals(-1, N.compare('a', 'b'));
        assertEquals(1, N.compare('b', 'a'));
    }

    @Test
    public void testCompareByte() {
        assertEquals(0, N.compare((byte) 10, (byte) 10));
        assertTrue(N.compare((byte) 5, (byte) 10) < 0);
        assertTrue(N.compare((byte) 10, (byte) 5) > 0);
    }

    @Test
    public void testCompareShort() {
        assertEquals(0, N.compare((short) 100, (short) 100));
        assertTrue(N.compare((short) 50, (short) 100) < 0);
        assertTrue(N.compare((short) 100, (short) 50) > 0);
    }

    @Test
    public void testCompareInt() {
        assertEquals(0, N.compare(100, 100));
        assertTrue(N.compare(50, 100) < 0);
        assertTrue(N.compare(100, 50) > 0);
    }

    @Test
    public void testCompareLong() {
        assertEquals(0, N.compare(100L, 100L));
        assertTrue(N.compare(50L, 100L) < 0);
        assertTrue(N.compare(100L, 50L) > 0);
    }

    @Test
    public void testCompareFloat() {
        assertEquals(0, N.compare(1.5f, 1.5f));
        assertEquals(-1, N.compare(1.0f, 1.5f));
        assertEquals(1, N.compare(1.5f, 1.0f));
        assertEquals(1, N.compare(Float.NaN, 1.0f));
    }

    @Test
    public void testCompareDouble() {
        assertEquals(0, N.compare(1.5, 1.5));
        assertEquals(-1, N.compare(1.0, 1.5));
        assertEquals(1, N.compare(1.5, 1.0));
        assertEquals(1, N.compare(Double.NaN, 1.0));
    }

    @Test
    public void testCompareArraysWithRange_01() {
        {
            byte[] a1 = { 1, 2, 3, 4 };
            byte[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, N.compare(a1, 1, a2, 1, 2));
        }
        {
            short[] a1 = { 1, 2, 3, 4 };
            short[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, N.compare(a1, 1, a2, 1, 2));
        }
        {

            int[] a1 = { 1, 2, 3, 4 };
            int[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, N.compare(a1, 1, a2, 1, 2));
        }
        {

            long[] a1 = { 1, 2, 3, 4 };
            long[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, N.compare(a1, 1, a2, 1, 2));
        }
        {

            float[] a1 = { 1, 2, 3, 4 };
            float[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, N.compare(a1, 1, a2, 1, 2));
        }
        {

            double[] a1 = { 1, 2, 3, 4 };
            double[] a2 = { 5, 2, 3, 6 };

            assertEquals(0, N.compare(a1, 1, a2, 1, 2));
        }
    }

    @Test
    public void testCompareCollections() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        List<String> list3 = Arrays.asList("a", "c", "b");

        assertEquals(0, N.compare(list1, 0, list2, 0, 3));
        assertEquals(-1, N.compare(list1, 1, list3, 1, 2));
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
    public void testCompare_Collection_WithRange_AndComparator() {
        List<String> a = Arrays.asList("x", "b", "c", "y");
        List<String> b = Arrays.asList("z", "B", "C", "w");
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(0, N.compare(a, 1, b, 1, 2, cmp));
    }

    @Test
    public void testCompareBooleanArrays() {
        Assertions.assertEquals(0, N.compare(new boolean[] {}, new boolean[] {}));
        Assertions.assertEquals(-1, N.compare(new boolean[] {}, new boolean[] { true }));
        Assertions.assertEquals(1, N.compare(new boolean[] { true }, new boolean[] {}));

        Assertions.assertEquals(0, N.compare((boolean[]) null, (boolean[]) null));
        Assertions.assertEquals(-1, N.compare((boolean[]) null, new boolean[] { true }));
        Assertions.assertEquals(1, N.compare(new boolean[] { true }, (boolean[]) null));

        boolean[] arr1 = { true, false, true };
        boolean[] arr2 = { true, false, true };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        Assertions.assertEquals(-1, N.compare(new boolean[] { false }, new boolean[] { true }));
        Assertions.assertEquals(1, N.compare(new boolean[] { true }, new boolean[] { false }));

        Assertions.assertEquals(-1, N.compare(new boolean[] { true }, new boolean[] { true, false }));
        Assertions.assertEquals(1, N.compare(new boolean[] { true, false }, new boolean[] { true }));

        boolean[] largeArr1 = new boolean[2000];
        boolean[] largeArr2 = new boolean[2000];
        Arrays.fill(largeArr1, true);
        Arrays.fill(largeArr2, true);
        largeArr2[1999] = false;
        Assertions.assertEquals(1, N.compare(largeArr1, largeArr2));
    }

    @Test
    public void testCompareCharArrays() {
        Assertions.assertEquals(0, N.compare(new char[] {}, new char[] {}));
        Assertions.assertEquals(-1, N.compare(new char[] {}, new char[] { 'a' }));
        Assertions.assertEquals(1, N.compare(new char[] { 'a' }, new char[] {}));

        Assertions.assertEquals(0, N.compare((char[]) null, (char[]) null));
        Assertions.assertEquals(-1, N.compare((char[]) null, new char[] { 'a' }));
        Assertions.assertEquals(1, N.compare(new char[] { 'a' }, (char[]) null));

        char[] arr1 = { 'a', 'b', 'c' };
        char[] arr2 = { 'a', 'b', 'c' };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        Assertions.assertEquals(-1, N.compare(new char[] { 'a' }, new char[] { 'b' }));
        Assertions.assertEquals(1, N.compare(new char[] { 'b' }, new char[] { 'a' }));

        Assertions.assertEquals(-1, N.compare(new char[] { 'a' }, new char[] { 'a', 'b' }));
        Assertions.assertEquals(1, N.compare(new char[] { 'a', 'b' }, new char[] { 'a' }));

        char[] largeArr1 = new char[2000];
        char[] largeArr2 = new char[2000];
        Arrays.fill(largeArr1, 'x');
        Arrays.fill(largeArr2, 'x');
        largeArr2[1999] = 'y';
        Assertions.assertEquals(-1, N.compare(largeArr1, largeArr2));
    }

    @Test
    public void testCompareObjectArrays() {
        Assertions.assertEquals(0, N.compare(new String[] {}, new String[] {}));
        Assertions.assertEquals(-1, N.compare(new String[] {}, new String[] { "a" }));
        Assertions.assertEquals(1, N.compare(new String[] { "a" }, new String[] {}));

        String[] arr1 = { "a", "b", "c" };
        String[] arr2 = { "a", "b", "c" };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        Assertions.assertEquals(-1, N.compare(new String[] { "a" }, new String[] { "b" }));
        Assertions.assertEquals(1, N.compare(new String[] { "b" }, new String[] { "a" }));

        Assertions.assertEquals(-1, N.compare(new String[] { null }, new String[] { "a" }));
        Assertions.assertEquals(1, N.compare(new String[] { "a" }, new String[] { null }));
        Assertions.assertEquals(0, N.compare(new String[] { null }, new String[] { null }));
    }

    @Test
    public void testCompareObjectArraysWithComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);

        String[] arr1 = { "a", "b" };
        String[] arr2 = { "a", "c" };
        Assertions.assertTrue(N.compare(arr1, arr2, reverseComparator) > 0);

        Assertions.assertEquals(0, N.compare(new String[] {}, new String[] {}, reverseComparator));

        Assertions.assertTrue(N.compare(arr1, arr2, (Comparator<String>) null) < 0);
    }

    @Test
    public void testCompareIterables() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        Set<String> set1 = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));

        Assertions.assertEquals(0, N.compare(list1, list2));
        Assertions.assertEquals(0, N.compare(list1, set1));

        List<String> list3 = Arrays.asList("a", "b", "d");
        Assertions.assertTrue(N.compare(list1, list3) < 0);

        Assertions.assertEquals(0, N.compare(Collections.<String> emptyList(), Collections.<String> emptyList()));
        Assertions.assertEquals(-1, N.compare(Collections.emptyList(), list1));
        Assertions.assertEquals(1, N.compare(list1, Collections.emptyList()));
    }

    @Test
    public void testCompareIterators() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");

        Assertions.assertEquals(0, N.compare(list1.iterator(), list2.iterator()));

        List<String> list3 = Arrays.asList("a", "b", "d");
        Assertions.assertTrue(N.compare(list1.iterator(), list3.iterator()) < 0);

        List<String> list4 = Arrays.asList("a", "b");
        Assertions.assertTrue(N.compare(list1.iterator(), list4.iterator()) > 0);

        Assertions.assertEquals(0, N.compare((Iterator<String>) null, (Iterator<String>) null));
        Assertions.assertEquals(-1, N.compare((Iterator<String>) null, list1.iterator()));
        Assertions.assertEquals(1, N.compare(list1.iterator(), (Iterator<String>) null));
    }

    @Test
    public void testCompareIterablesWithComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "d");

        Assertions.assertTrue(N.compare(list1, list2, (Comparator<String>) null) < 0);

        Assertions.assertTrue(N.compare(list1, list2, reverseComparator) > 0);

        Assertions.assertEquals(0, N.compare(Collections.emptyList(), Collections.emptyList(), reverseComparator));
    }

    @Test
    public void testCompareIteratorsWithComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "d");

        Assertions.assertTrue(N.compare(list1.iterator(), list2.iterator(), (Comparator<String>) null) < 0);

        Assertions.assertTrue(N.compare(list1.iterator(), list2.iterator(), reverseComparator) > 0);
    }

    @Test
    public void testCompareComparable() {
        assertEquals(0, N.compare("abc", "abc"));
        assertTrue(N.compare("abc", "def") < 0);
        assertTrue(N.compare("def", "abc") > 0);
        assertTrue(N.compare(null, "abc") < 0);
        assertTrue(N.compare("abc", null) > 0);
        assertEquals(0, N.compare((String) null, null));
    }

    @Test
    public void testCompareWithComparator() {
        Comparator<String> comp = String.CASE_INSENSITIVE_ORDER;
        assertEquals(0, N.compare("ABC", "abc", comp));
        assertTrue(N.compare("abc", "def", comp) < 0);
        assertTrue(N.compare("def", "abc", comp) > 0);

        assertEquals(0, N.compare("abc", "abc", null));
    }

    @Test
    public void testCompareMultiplePairs2() {
        assertEquals(0, N.compare("a", "a", 1, 1));
        assertEquals(-1, N.compare("a", "b", 1, 1));
        assertEquals(1, N.compare("b", "a", 1, 1));
        assertEquals(-1, N.compare("a", "a", 1, 2));
        assertEquals(1, N.compare("a", "a", 2, 1));
    }

    @Test
    public void testCompareMultiplePairs3() {
        assertEquals(0, N.compare("a", "a", 1, 1, 5L, 5L));
        assertEquals(-1, N.compare("a", "b", 1, 1, 5L, 5L));
        assertEquals(1, N.compare("a", "a", 2, 1, 5L, 5L));
        assertEquals(-1, N.compare("a", "a", 1, 1, 5L, 6L));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCompareMultiplePairs4() {
        assertEquals(0, N.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0));
        assertEquals(-1, N.compare("a", "b", 1, 1, 5L, 5L, 1.0, 1.0));
        assertEquals(-1, N.compare("a", "a", 1, 1, 5L, 5L, 1.0, 2.0));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCompareMultiplePairs5() {
        assertEquals(0, N.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x"));
        assertEquals(1, N.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "y", "x"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCompareMultiplePairs6() {
        assertEquals(0, N.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x", true, true));
        assertEquals(-1, N.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x", false, true));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCompareMultiplePairs7() {
        assertEquals(0, N.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x", true, true, 'z', 'z'));
        assertEquals(1, N.compare("a", "a", 1, 1, 5L, 5L, 1.0, 1.0, "x", "x", true, true, 'z', 'y'));
    }

    @Test
    public void testCompareBooleanArraysWithRange() {
        boolean[] arr1 = { true, false, true, false };
        boolean[] arr2 = { false, true, false, true };

        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 2, 2));

        Assertions.assertEquals(1, N.compare(arr1, 0, arr2, 0, 1));
        Assertions.assertEquals(-1, N.compare(arr1, 1, arr2, 1, 1));

        Assertions.assertEquals(0, N.compare(arr1, 0, arr2, 0, 0));

        Assertions.assertEquals(0, N.compare(arr1, 1, arr1, 1, 2));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.compare(arr1, 0, arr2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.compare(arr1, 0, arr2, 0, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.compare(arr1, 3, arr2, 0, 2));

        boolean[] largeArr1 = new boolean[2000];
        boolean[] largeArr2 = new boolean[2000];
        Arrays.fill(largeArr1, true);
        Arrays.fill(largeArr2, true);
        largeArr2[1500] = false;
        Assertions.assertEquals(1, N.compare(largeArr1, 0, largeArr2, 0, 1600));
    }

    @Test
    public void testCompareCharArraysWithRange() {
        char[] arr1 = { 'a', 'b', 'c', 'd' };
        char[] arr2 = { 'x', 'b', 'c', 'y' };

        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
        Assertions.assertEquals(-1, N.compare(arr1, 3, arr2, 3, 1));

        Assertions.assertEquals(0, N.compare(arr1, 0, arr2, 0, 0));

        Assertions.assertEquals(0, N.compare(arr1, 1, arr1, 1, 2));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.compare(arr1, 0, arr2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.compare(arr1, 0, arr2, 0, 10));

        char[] bugArr1 = { 'a', 'b' };
        char[] bugArr2 = { 'x', 'a' };
        Assertions.assertTrue(N.compare(bugArr1, 1, bugArr2, 1, 1) > 0);
    }

    @Test
    public void testCompareObjectArraysWithRange() {
        String[] arr1 = { "a", "b", "c", "d" };
        String[] arr2 = { "x", "b", "c", "y" };

        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertTrue(N.compare(arr1, 0, arr2, 0, 1) < 0);

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.compare(arr1, 0, arr2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.compare(arr1, 0, arr2, 0, 10));
    }

    @Test
    public void testCompareCollectionsWithRange() {
        List<String> list1 = Arrays.asList("a", "b", "c", "d");
        List<String> list2 = Arrays.asList("x", "b", "c", "y");

        Assertions.assertEquals(0, N.compare(list1, 1, list2, 1, 2));

        Assertions.assertTrue(N.compare(list1, 0, list2, 0, 1) < 0);

        Assertions.assertEquals(0, N.compare(list1, 1, list1, 1, 2));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.compare(list1, 0, list2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.compare(list1, 0, list2, 0, 10));
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
    public void testCompareUnsignedByteArrays() {
        byte[] arr1 = { (byte) 255 };
        byte[] arr2 = { 1 };
        Assertions.assertTrue(N.compareUnsigned(arr1, arr2) > 0);

        Assertions.assertEquals(0, N.compareUnsigned(new byte[] {}, new byte[] {}));
        Assertions.assertEquals(-1, N.compareUnsigned(new byte[] {}, new byte[] { 1 }));
        Assertions.assertEquals(1, N.compareUnsigned(new byte[] { 1 }, new byte[] {}));
    }

    @Test
    public void testCompareUnsignedShortArrays() {
        short[] arr1 = { (short) 65535 };
        short[] arr2 = { 1 };
        Assertions.assertTrue(N.compareUnsigned(arr1, arr2) > 0);

        Assertions.assertEquals(0, N.compareUnsigned(new short[] {}, new short[] {}));
    }

    @Test
    public void testCompareUnsignedShortArraysWithRange() {
        short[] arr1 = { 1, (short) 65535, 3 };
        short[] arr2 = { 5, 1, 3 };

        Assertions.assertTrue(N.compareUnsigned(arr1, 1, arr2, 1, 1) > 0);
    }

    @Test
    public void testCompareUnsignedIntArrays() {
        int[] arr1 = { -1 };
        int[] arr2 = { 1 };
        Assertions.assertTrue(N.compareUnsigned(arr1, arr2) > 0);

        Assertions.assertEquals(0, N.compareUnsigned(new int[] {}, new int[] {}));
    }

    @Test
    public void testCompareUnsignedIntArraysWithRange() {
        int[] arr1 = { 1, -1, 3 };
        int[] arr2 = { 5, 1, 3 };

        Assertions.assertTrue(N.compareUnsigned(arr1, 1, arr2, 1, 1) > 0);
    }

    @Test
    public void testCompareUnsignedLongArrays() {
        long[] arr1 = { -1L };
        long[] arr2 = { 1L };
        Assertions.assertTrue(N.compareUnsigned(arr1, arr2) > 0);

        Assertions.assertEquals(0, N.compareUnsigned(new long[] {}, new long[] {}));
    }

    @Test
    public void testCompareUnsignedLongArraysWithRange() {
        long[] arr1 = { 1L, -1L, 3L };
        long[] arr2 = { 5L, 1L, 3L };

        Assertions.assertTrue(N.compareUnsigned(arr1, 1, arr2, 1, 1) > 0);
    }

    @Test
    public void testCompareUnsignedByte() {
        assertEquals(0, N.compareUnsigned((byte) 10, (byte) 10));
        assertTrue(N.compareUnsigned((byte) 5, (byte) 10) < 0);
        assertTrue(N.compareUnsigned((byte) -1, (byte) 127) > 0);
    }

    @Test
    public void testCompareUnsignedShort() {
        assertEquals(0, N.compareUnsigned((short) 100, (short) 100));
        assertTrue(N.compareUnsigned((short) 50, (short) 100) < 0);
        assertTrue(N.compareUnsigned((short) -1, (short) 32767) > 0);
    }

    @Test
    public void testCompareUnsignedInt() {
        assertEquals(0, N.compareUnsigned(100, 100));
        assertTrue(N.compareUnsigned(50, 100) < 0);
        assertTrue(N.compareUnsigned(-1, Integer.MAX_VALUE) > 0);
    }

    @Test
    public void testCompareUnsignedLong() {
        assertEquals(0, N.compareUnsigned(100L, 100L));
        assertEquals(-1, N.compareUnsigned(50L, 100L));
        assertEquals(1, N.compareUnsigned(-1L, Long.MAX_VALUE));
    }

    @Test
    public void testCompareUnsignedByteArraysWithRange() {
        byte[] arr1 = { 1, (byte) 255, 3 };
        byte[] arr2 = { 5, 1, 3 };

        Assertions.assertTrue(N.compareUnsigned(arr1, 1, arr2, 1, 1) > 0);

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.compareUnsigned(arr1, 0, arr2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.compareUnsigned(arr1, 0, arr2, 0, 10));
    }

    @Test
    public void testCompareIgnoreCase() {
        assertEquals(0, N.compareIgnoreCase("test", "TEST"));
        assertTrue(N.compareIgnoreCase("abc", "XYZ") < 0);
        assertTrue(N.compareIgnoreCase("xyz", "ABC") > 0);
        assertEquals(0, N.compareIgnoreCase((String) null, (String) null));
    }

    @Test
    public void testCompareIgnoreCase_StringArray() {
        String[] a = { "a", "b", "c" };
        String[] b = { "A", "B", "C" };

        assertEquals(0, N.compareIgnoreCase(a, b));
    }

    @Test
    public void testCompareIgnoreCaseArrays() {
        String[] arr1 = { "Hello", "World" };
        String[] arr2 = { "HELLO", "WORLD" };
        Assertions.assertEquals(0, N.compareIgnoreCase(arr1, arr2));

        String[] arr3 = { "Hello", "World" };
        String[] arr4 = { "HELLO", "EARTH" };
        Assertions.assertTrue(N.compareIgnoreCase(arr3, arr4) > 0);

        Assertions.assertEquals(0, N.compareIgnoreCase(new String[] {}, new String[] {}));
        Assertions.assertEquals(-1, N.compareIgnoreCase(new String[] {}, arr1));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testCompareByProps() {
        TestBean left = new TestBean();
        left.setName("alpha");
        left.setValue("1");
        TestBean right = new TestBean();
        right.setName("alpha");
        right.setValue("2");

        assertTrue(N.compareByProps(left, right, Arrays.asList("name", "value")) < 0);
        assertEquals(0, N.compareByProps(left, left, Arrays.asList("name", "value")));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testCompareByProps_ErrorPath() {
        TestBean bean = new TestBean();
        bean.setName("alpha");

        assertThrows(IllegalArgumentException.class, () -> N.compareByProps(null, bean, Arrays.asList("name")));
        assertThrows(IllegalArgumentException.class, () -> N.compareByProps(bean, null, Arrays.asList("name")));
    }

    @Test
    public void testLessThan() {
        assertTrue(N.lessThan(3, 5));
        assertFalse(N.lessThan(5, 3));
        assertFalse(N.lessThan(3, 3));
    }

    @Test
    public void testLessThanWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(N.lessThan(1, 2, null));

        Assertions.assertFalse(N.lessThan(1, 2, reverseComparator));
        Assertions.assertTrue(N.lessThan(2, 1, reverseComparator));
    }

    @Test
    public void testLessThanOrEqual() {
        assertTrue(N.lessThanOrEqual(3, 5));
        assertTrue(N.lessThanOrEqual(3, 3));
        assertFalse(N.lessThanOrEqual(5, 3));
    }

    @Test
    public void testLessThanOrEqualWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(N.lessThanOrEqual(1, 2, null));
        Assertions.assertTrue(N.lessThanOrEqual(1, 1, null));

        Assertions.assertFalse(N.lessThanOrEqual(1, 2, reverseComparator));
        Assertions.assertTrue(N.lessThanOrEqual(2, 1, reverseComparator));
    }

    @Test
    public void testLe() {
        assertTrue(N.le(3, 5));
        assertTrue(N.le(3, 3));
        assertFalse(N.le(5, 3));
    }

    @Test
    public void testLeWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        assertTrue(N.le(1, 2, null));
        assertTrue(N.le(1, 1, null));
        assertFalse(N.le(1, 2, reverseComparator));
        assertTrue(N.le(2, 1, reverseComparator));
    }

    @Test
    public void testGreaterThan() {
        assertTrue(N.greaterThan(5, 3));
        assertFalse(N.greaterThan(3, 5));
        assertFalse(N.greaterThan(3, 3));
    }

    @Test
    public void testGreaterThanWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertFalse(N.greaterThan(1, 2, null));

        Assertions.assertTrue(N.greaterThan(1, 2, reverseComparator));
        Assertions.assertFalse(N.greaterThan(2, 1, reverseComparator));
    }

    @Test
    public void testGreaterThanOrEqual() {
        assertTrue(N.greaterThanOrEqual(5, 3));
        assertTrue(N.greaterThanOrEqual(3, 3));
        assertFalse(N.greaterThanOrEqual(2, 3));
    }

    @Test
    public void testGreaterThanOrEqualWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertFalse(N.greaterThanOrEqual(1, 2, null));
        Assertions.assertTrue(N.greaterThanOrEqual(1, 1, null));

        Assertions.assertTrue(N.greaterThanOrEqual(1, 2, reverseComparator));
        Assertions.assertFalse(N.greaterThanOrEqual(2, 1, reverseComparator));
    }

    @Test
    public void testGe() {
        assertTrue(N.ge(5, 3));
        assertTrue(N.ge(3, 3));
        assertFalse(N.ge(2, 3));
    }

    @Test
    public void testGeWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        assertFalse(N.ge(1, 2, null));
        assertTrue(N.ge(1, 1, null));
        assertTrue(N.ge(1, 2, reverseComparator));
        assertFalse(N.ge(2, 1, reverseComparator));
    }

    @Test
    public void testGtAndLt() {
        assertTrue(N.gtAndLt(5, 3, 7));
        assertFalse(N.gtAndLt(2, 3, 7));
        assertFalse(N.gtAndLt(8, 3, 7));
    }

    @Test
    public void testGtAndLtWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(N.gtAndLt(5, 1, 10, null));

        Assertions.assertTrue(N.gtAndLt(5, 10, 1, reverseComparator));
        Assertions.assertFalse(N.gtAndLt(5, 1, 10, reverseComparator));
    }

    @Test
    public void testGeAndLt() {
        assertTrue(N.geAndLt(5, 3, 7));
        assertTrue(N.geAndLt(3, 3, 7));
        assertFalse(N.geAndLt(7, 3, 7));
    }

    @Test
    public void testGeAndLtWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(N.geAndLt(5, 1, 10, null));
        Assertions.assertTrue(N.geAndLt(1, 1, 10, null));

        Assertions.assertTrue(N.geAndLt(5, 10, 1, reverseComparator));
        Assertions.assertTrue(N.geAndLt(10, 10, 1, reverseComparator));
    }

    @Test
    public void testGeAndLe() {
        assertTrue(N.geAndLe(5, 3, 7));
        assertTrue(N.geAndLe(3, 3, 7));
        assertTrue(N.geAndLe(7, 3, 7));
        assertFalse(N.geAndLe(2, 3, 7));
    }

    @Test
    public void testGeAndLeWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(N.geAndLe(5, 1, 10, null));
        Assertions.assertTrue(N.geAndLe(1, 1, 10, null));
        Assertions.assertTrue(N.geAndLe(10, 1, 10, null));

        Assertions.assertTrue(N.geAndLe(5, 10, 1, reverseComparator));
        Assertions.assertTrue(N.geAndLe(10, 10, 1, reverseComparator));
        Assertions.assertTrue(N.geAndLe(1, 10, 1, reverseComparator));
    }

    @Test
    public void testGtAndLe() {
        assertTrue(N.gtAndLe(5, 3, 7));
        assertTrue(N.gtAndLe(7, 3, 7));
        assertFalse(N.gtAndLe(2, 3, 7));
    }

    @Test
    public void testGtAndLeWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(N.gtAndLe(5, 1, 10, null));
        Assertions.assertFalse(N.gtAndLe(1, 1, 10, null));
        Assertions.assertTrue(N.gtAndLe(10, 1, 10, null));

        Assertions.assertTrue(N.gtAndLe(5, 10, 1, reverseComparator));
        Assertions.assertFalse(N.gtAndLe(10, 10, 1, reverseComparator));
        Assertions.assertTrue(N.gtAndLe(1, 10, 1, reverseComparator));
    }

    @Test
    public void testIsBetween() {
        assertTrue(N.isBetween(5, 3, 7));
        assertTrue(N.isBetween(3, 3, 7));
        assertTrue(N.isBetween(7, 3, 7));
        assertFalse(N.isBetween(2, 3, 7));
    }

    @Test
    public void testIsBetweenWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(N.isBetween(5, 1, 10, null));
        Assertions.assertTrue(N.isBetween(5, 10, 1, reverseComparator));
    }

    @Test
    public void testGetElement() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals("b", N.getElement(list, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.getElement(list, 5));
    }

    @Test
    public void testGetElementFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c", "d");

        Assertions.assertEquals("a", N.getElement(list, 0));
        Assertions.assertEquals("b", N.getElement(list, 1));
        Assertions.assertEquals("d", N.getElement(list, 3));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.getElement(list, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.getElement(list, 4));

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals("a", N.getElement(set, 0));
        Assertions.assertEquals("b", N.getElement(set, 1));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.getElement((Iterable<String>) null, 0));
    }

    @Test
    public void testGetElementFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d");

        Assertions.assertEquals("a", N.getElement(list.iterator(), 0));
        Assertions.assertEquals("b", N.getElement(list.iterator(), 1));
        Assertions.assertEquals("d", N.getElement(list.iterator(), 3));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.getElement(list.iterator(), -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.getElement(list.iterator(), 4));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.getElement((Iterator<String>) null, 0));
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
    public void testGetOnlyElementFromIterable() {
        List<String> singleList = Arrays.asList("only");
        Assertions.assertEquals("only", N.getOnlyElement(singleList).get());

        Assertions.assertFalse(N.getOnlyElement(Collections.emptyList()).isPresent());

        List<String> multiList = Arrays.asList("a", "b");
        Assertions.assertThrows(TooManyElementsException.class, () -> N.getOnlyElement(multiList));

        Assertions.assertFalse(N.getOnlyElement((Iterable<String>) null).isPresent());

        Set<String> singleSet = Collections.singleton("only");
        Assertions.assertEquals("only", N.getOnlyElement(singleSet).get());
    }

    @Test
    public void testGetOnlyElementFromIterator() {
        List<String> singleList = Arrays.asList("only");
        Assertions.assertEquals("only", N.getOnlyElement(singleList.iterator()).get());

        Assertions.assertFalse(N.getOnlyElement(Collections.emptyIterator()).isPresent());

        List<String> multiList = Arrays.asList("a", "b");
        Assertions.assertThrows(TooManyElementsException.class, () -> N.getOnlyElement(multiList.iterator()));

        Assertions.assertFalse(N.getOnlyElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testFirstElement() {
        List<String> list = Arrays.asList("a", "b", "c");
        com.landawn.abacus.util.u.Nullable<String> result = N.firstElement(list);
        assertTrue(result.isPresent());
        assertEquals("a", result.get());
    }

    @Test
    public void testFirstElementFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", N.firstElement(list).get());

        Assertions.assertFalse(N.firstElement(Collections.emptyList()).isPresent());

        Assertions.assertFalse(N.firstElement((Iterable<String>) null).isPresent());

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("a", N.firstElement(arrayList).get());

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals("a", N.firstElement(set).get());
    }

    @Test
    public void testFirstElementFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", N.firstElement(list.iterator()).get());

        Assertions.assertFalse(N.firstElement(Collections.emptyIterator()).isPresent());

        Assertions.assertFalse(N.firstElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testLastElement() {
        List<String> list = Arrays.asList("a", "b", "c");
        com.landawn.abacus.util.u.Nullable<String> result = N.lastElement(list);
        assertTrue(result.isPresent());
        assertEquals("c", result.get());
    }

    @Test
    public void testLastElementFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", N.lastElement(list).get());

        Assertions.assertFalse(N.lastElement(Collections.emptyList()).isPresent());

        Assertions.assertFalse(N.lastElement((Iterable<String>) null).isPresent());

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("c", N.lastElement(arrayList).get());

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals("c", N.lastElement(set).get());

        Deque<String> deque = new ArrayDeque<>(list);
        Assertions.assertEquals("c", N.lastElement(deque).get());
    }

    @Test
    public void testLastElementFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", N.lastElement(list.iterator()).get());

        Assertions.assertFalse(N.lastElement(Collections.emptyIterator()).isPresent());

        Assertions.assertFalse(N.lastElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testFirstElements() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        List<String> result = N.firstElements(list, 2);
        assertEquals(2, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
    }

    // Tests for firstElements with Iterable (non-Collection)
    @Test
    public void testFirstElements_Iterable_NonCollection() {
        // Use a non-Collection Iterable to hit the iterator path
        Iterable<String> iterable = () -> Arrays.asList("x", "y", "z", "w").iterator();
        List<String> result = N.firstElements(iterable, 2);
        assertEquals(2, result.size());
        assertEquals("x", result.get(0));
        assertEquals("y", result.get(1));
    }

    @Test
    public void testFirstElements_Iterable_MoreThanAvailable() {
        Iterable<Integer> iterable = () -> Arrays.asList(1, 2).iterator();
        List<Integer> result = N.firstElements(iterable, 10);
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
    }

    @Test
    public void testFirstElements_Array() {
        String[] arr = { "a", "b", "c", "d", "e" };
        List<String> first3 = N.firstElements(arr, 3);
        assertEquals(Arrays.asList("a", "b", "c"), first3);
        List<String> firstAll = N.firstElements(arr, 10);
        assertEquals(5, firstAll.size());
        List<String> firstNone = N.firstElements(arr, 0);
        assertTrue(firstNone.isEmpty());
        List<String> fromNull = N.firstElements((String[]) null, 2);
        assertTrue(fromNull.isEmpty());
    }

    @Test
    public void testFirstElements_Iterable_ZeroN() {
        List<String> result = N.firstElements(Arrays.asList("a", "b"), 0);
        assertEquals(0, result.size());
    }

    @Test
    public void testFirstElementsFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), N.firstElements(list, 3));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.firstElements(list, 5));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.firstElements(list, 10));
        Assertions.assertEquals(Collections.emptyList(), N.firstElements(list, 0));

        Assertions.assertEquals(Collections.emptyList(), N.firstElements(Collections.emptyList(), 5));

        Assertions.assertEquals(Collections.emptyList(), N.firstElements((Iterable<String>) null, 5));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.firstElements(list, -1));

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), N.firstElements(set, 3));
    }

    @Test
    public void testFirstElementsFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), N.firstElements(list.iterator(), 3));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.firstElements(list.iterator(), 5));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.firstElements(list.iterator(), 10));
        Assertions.assertEquals(Collections.emptyList(), N.firstElements(list.iterator(), 0));

        Assertions.assertEquals(Collections.emptyList(), N.firstElements(Collections.emptyIterator(), 5));

        Assertions.assertEquals(Collections.emptyList(), N.firstElements((Iterator<String>) null, 5));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.firstElements(list.iterator(), -1));
    }

    @Test
    public void testFirstElements_Iterable_NegativeN() {
        assertThrows(IllegalArgumentException.class, () -> N.firstElements(Arrays.asList("a"), -1));
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
    public void testLastElements_Array() {
        String[] arr = { "a", "b", "c", "d", "e" };
        List<String> last3 = N.lastElements(arr, 3);
        assertEquals(Arrays.asList("c", "d", "e"), last3);
        List<String> lastAll = N.lastElements(arr, 10);
        assertEquals(5, lastAll.size());
        List<String> lastNone = N.lastElements(arr, 0);
        assertTrue(lastNone.isEmpty());
        List<String> fromNull = N.lastElements((String[]) null, 2);
        assertTrue(fromNull.isEmpty());
    }

    @Test
    public void testLastElementsFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        Assertions.assertEquals(Arrays.asList("c", "d", "e"), N.lastElements(list, 3));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.lastElements(list, 5));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.lastElements(list, 10));
        Assertions.assertEquals(Collections.emptyList(), N.lastElements(list, 0));

        Assertions.assertEquals(Collections.emptyList(), N.lastElements(Collections.emptyList(), 5));

        Assertions.assertEquals(Collections.emptyList(), N.lastElements((Iterable<String>) null, 5));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.lastElements(list, -1));

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals(Arrays.asList("c", "d", "e"), N.lastElements(set, 3));
    }

    @Test
    public void testLastElementsFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        Assertions.assertEquals(Arrays.asList("c", "d", "e"), N.lastElements(list.iterator(), 3));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.lastElements(list.iterator(), 5));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.lastElements(list.iterator(), 10));
        Assertions.assertEquals(Collections.emptyList(), N.lastElements(list.iterator(), 0));

        Assertions.assertEquals(Collections.emptyList(), N.lastElements(Collections.emptyIterator(), 5));

        Assertions.assertEquals(Collections.emptyList(), N.lastElements((Iterator<String>) null, 5));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.lastElements(list.iterator(), -1));
    }

    @Test
    public void testFirstNonNull() {
        com.landawn.abacus.util.u.Optional<String> result = N.firstNonNull(null, "first", null, "second");
        assertTrue(result.isPresent());
        assertEquals("first", result.get());
    }

    @Test
    public void testFirstNonNullTwo() {
        Assertions.assertEquals("a", N.firstNonNull("a", "b").get());

        Assertions.assertEquals("b", N.firstNonNull(null, "b").get());

        Assertions.assertEquals("a", N.firstNonNull("a", null).get());

        Assertions.assertFalse(N.firstNonNull(null, null).isPresent());
    }

    @Test
    public void testFirstNonNullThree() {
        Assertions.assertEquals("a", N.firstNonNull("a", "b", "c").get());

        Assertions.assertEquals("b", N.firstNonNull(null, "b", "c").get());

        Assertions.assertEquals("c", N.firstNonNull(null, null, "c").get());

        Assertions.assertFalse(N.firstNonNull(null, null, null).isPresent());
    }

    @Test
    public void testFirstNonNullVarargs() {
        Assertions.assertEquals("c", N.firstNonNull(null, null, "c", "d").get());

        Assertions.assertFalse(N.firstNonNull(new String[] {}).isPresent());

        Assertions.assertFalse(N.firstNonNull((String[]) null).isPresent());

        Assertions.assertFalse(N.firstNonNull(new String[] { null, null, null }).isPresent());
    }

    @Test
    public void testFirstNonNullIterable() {
        List<String> list = Arrays.asList(null, null, "c", "d");
        Assertions.assertEquals("c", N.firstNonNull(list).get());

        Assertions.assertFalse(N.firstNonNull(Collections.emptyList()).isPresent());

        Assertions.assertFalse(N.firstNonNull((Iterable<String>) null).isPresent());

        List<String> allNulls = Arrays.asList(null, null, null);
        Assertions.assertFalse(N.firstNonNull(allNulls).isPresent());
    }

    @Test
    public void testFirstNonNullIterator() {
        List<String> list = Arrays.asList(null, null, "c", "d");
        Assertions.assertEquals("c", N.firstNonNull(list.iterator()).get());

        Assertions.assertFalse(N.firstNonNull(Collections.emptyIterator()).isPresent());

        Assertions.assertFalse(N.firstNonNull((Iterator<String>) null).isPresent());

        List<String> allNulls = Arrays.asList(null, null, null);
        Assertions.assertFalse(N.firstNonNull(allNulls.iterator()).isPresent());
    }

    @Test
    public void testFirstNonNull2() {
        assertEquals("a", N.firstNonNull("a", "b").get());
        assertEquals("b", N.firstNonNull(null, "b").get());
        assertFalse(N.firstNonNull(null, null).isPresent());
    }

    @Test
    public void testFirstNonNullOrDefault_Array() {
        String[] arr = { null, null, "found", "other" };
        assertEquals("found", N.firstNonNullOrDefault(arr, "default"));
        String[] allNull = { null, null };
        assertEquals("default", N.firstNonNullOrDefault(allNull, "default"));
        assertEquals("default", N.firstNonNullOrDefault((String[]) null, "default"));
        assertEquals("default", N.firstNonNullOrDefault(new String[0], "default"));
    }

    @Test
    public void testLastNonNull() {
        com.landawn.abacus.util.u.Optional<String> result = N.lastNonNull(null, "first", null, "second", null);
        assertTrue(result.isPresent());
        assertEquals("second", result.get());
    }

    @Test
    public void testLastNonNullTwo() {
        Assertions.assertEquals("b", N.lastNonNull("a", "b").get());

        Assertions.assertEquals("b", N.lastNonNull(null, "b").get());

        Assertions.assertEquals("a", N.lastNonNull("a", null).get());

        Assertions.assertFalse(N.lastNonNull(null, null).isPresent());
    }

    @Test
    public void testLastNonNullThree() {
        Assertions.assertEquals("c", N.lastNonNull("a", "b", "c").get());

        Assertions.assertEquals("b", N.lastNonNull("a", "b", null).get());

        Assertions.assertEquals("a", N.lastNonNull("a", null, null).get());

        Assertions.assertFalse(N.lastNonNull(null, null, null).isPresent());
    }

    @Test
    public void testLastNonNullVarargs() {
        Assertions.assertEquals("d", N.lastNonNull("a", "b", null, "d", null).get());

        Assertions.assertFalse(N.lastNonNull(new String[] {}).isPresent());

        Assertions.assertFalse(N.lastNonNull((String[]) null).isPresent());

        Assertions.assertFalse(N.lastNonNull(new String[] { null, null, null }).isPresent());
    }

    @Test
    public void testLastNonNullIterable() {
        List<String> list = Arrays.asList("a", "b", null, "d", null);
        Assertions.assertEquals("d", N.lastNonNull(list).get());

        Assertions.assertFalse(N.lastNonNull(Collections.emptyList()).isPresent());

        Assertions.assertFalse(N.lastNonNull((Iterable<String>) null).isPresent());

        List<String> allNulls = Arrays.asList(null, null, null);
        Assertions.assertFalse(N.lastNonNull(allNulls).isPresent());

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("d", N.lastNonNull(arrayList).get());

    }

    @Test
    public void testLastNonNullIterator() {
        List<String> list = Arrays.asList("a", "b", null, "d", null);
        Assertions.assertEquals("d", N.lastNonNull(list.iterator()).get());

        Assertions.assertFalse(N.lastNonNull(Collections.emptyIterator()).isPresent());

        Assertions.assertFalse(N.lastNonNull((Iterator<String>) null).isPresent());

        List<String> allNulls = Arrays.asList(null, null, null);
        Assertions.assertFalse(N.lastNonNull(allNulls.iterator()).isPresent());
    }

    @Test
    public void testLastNonNull2() {
        assertEquals("b", N.lastNonNull("a", "b").get());
        assertEquals("a", N.lastNonNull("a", null).get());
        assertFalse(N.lastNonNull(null, null).isPresent());
    }

    // ========== lastNonNull(Iterable) - Deque and non-list Iterable paths ==========

    @Test
    public void testLastNonNull_Iterable_Deque() {
        // LinkedList implements Deque and supports null; descendingIterator is used
        java.util.LinkedList<String> deque = new java.util.LinkedList<>(Arrays.asList(null, "first", null, "last"));
        com.landawn.abacus.util.u.Optional<String> result = N.lastNonNull(deque);
        assertTrue(result.isPresent());
        assertEquals("last", result.get());
    }

    @Test
    public void testLastNonNull_Iterable_Deque_AllNull() {
        java.util.LinkedList<String> deque = new java.util.LinkedList<>(Arrays.asList(null, null));
        com.landawn.abacus.util.u.Optional<String> result = N.lastNonNull(deque);
        assertFalse(result.isPresent());
    }

    @Test
    public void testLastNonNullOrDefault_Array() {
        String[] arr = { "first", "second", null, null };
        assertEquals("second", N.lastNonNullOrDefault(arr, "default"));
        String[] allNull = { null, null };
        assertEquals("default", N.lastNonNullOrDefault(allNull, "default"));
        assertEquals("default", N.lastNonNullOrDefault((String[]) null, "default"));
    }

    @Test
    public void testFirstNonEmpty() {
        com.landawn.abacus.util.u.Optional<String> result = N.firstNonEmpty("", null, "first", "second");
        assertTrue(result.isPresent());
        assertEquals("first", result.get());
    }

    @Test
    public void testFirstNonEmptyArraysTwo() {
        String[] arr1 = { "a", "b" };
        String[] arr2 = { "c", "d" };
        String[] empty = {};

        Assertions.assertArrayEquals(arr1, N.firstNonEmpty(arr1, arr2).get());

        Assertions.assertArrayEquals(arr2, N.firstNonEmpty(empty, arr2).get());

        Assertions.assertArrayEquals(arr1, N.firstNonEmpty(arr1, empty).get());

        Assertions.assertFalse(N.firstNonEmpty(empty, empty).isPresent());

        Assertions.assertArrayEquals(arr1, N.firstNonEmpty(null, arr1).get());
        Assertions.assertArrayEquals(arr1, N.firstNonEmpty(arr1, null).get());
        Assertions.assertFalse(N.firstNonEmpty((String[]) null, (String[]) null).isPresent());
    }

    @Test
    public void testFirstNonEmptyArraysThree() {
        String[] arr1 = { "a", "b" };
        String[] arr2 = { "c", "d" };
        String[] arr3 = { "e", "f" };
        String[] empty = {};

        Assertions.assertArrayEquals(arr1, N.firstNonEmpty(arr1, arr2, arr3).get());

        Assertions.assertArrayEquals(arr2, N.firstNonEmpty(empty, arr2, arr3).get());

        Assertions.assertArrayEquals(arr3, N.firstNonEmpty(empty, empty, arr3).get());

        Assertions.assertFalse(N.firstNonEmpty(empty, empty, empty).isPresent());
    }

    @Test
    public void testFirstNonEmptyCollectionsTwo() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("c", "d");
        List<String> empty = Collections.emptyList();

        Assertions.assertEquals(list1, N.firstNonEmpty(list1, list2).get());

        Assertions.assertEquals(list2, N.firstNonEmpty(empty, list2).get());

        Assertions.assertEquals(list1, N.firstNonEmpty(list1, empty).get());

        Assertions.assertFalse(N.firstNonEmpty(empty, empty).isPresent());

        Assertions.assertEquals(list1, N.firstNonEmpty(null, list1).get());
        Assertions.assertEquals(list1, N.firstNonEmpty(list1, null).get());
        Assertions.assertFalse(N.firstNonEmpty((List<String>) null, (List<String>) null).isPresent());
    }

    @Test
    public void testFirstNonEmptyCollectionsThree() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("c", "d");
        List<String> list3 = Arrays.asList("e", "f");
        List<String> empty = Collections.emptyList();

        Assertions.assertEquals(list1, N.firstNonEmpty(list1, list2, list3).get());

        Assertions.assertEquals(list2, N.firstNonEmpty(empty, list2, list3).get());

        Assertions.assertEquals(list3, N.firstNonEmpty(empty, empty, list3).get());

        Assertions.assertFalse(N.firstNonEmpty(empty, empty, empty).isPresent());
    }

    @Test
    public void testFirstNonEmptyMapsTwo() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        Map<String, String> empty = Collections.emptyMap();

        Assertions.assertEquals(map1, N.firstNonEmpty(map1, map2).get());

        Assertions.assertEquals(map2, N.firstNonEmpty(empty, map2).get());

        Assertions.assertEquals(map1, N.firstNonEmpty(map1, empty).get());

        Assertions.assertFalse(N.firstNonEmpty(empty, empty).isPresent());

        Assertions.assertEquals(map1, N.firstNonEmpty(null, map1).get());
        Assertions.assertEquals(map1, N.firstNonEmpty(map1, null).get());
        Assertions.assertFalse(N.firstNonEmpty((Map<String, String>) null, (Map<String, String>) null).isPresent());
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

        Assertions.assertEquals(map1, N.firstNonEmpty(map1, map2, map3).get());

        Assertions.assertEquals(map2, N.firstNonEmpty(empty, map2, map3).get());

        Assertions.assertEquals(map3, N.firstNonEmpty(empty, empty, map3).get());

        Assertions.assertFalse(N.firstNonEmpty(empty, empty, empty).isPresent());
    }

    @Test
    public void testFirstNonEmptyCharSequencesTwo() {
        Assertions.assertEquals("hello", N.firstNonEmpty("hello", "world").get());

        Assertions.assertEquals("world", N.firstNonEmpty("", "world").get());

        Assertions.assertEquals("hello", N.firstNonEmpty("hello", "").get());

        Assertions.assertFalse(N.firstNonEmpty("", "").isPresent());

        Assertions.assertEquals("hello", N.firstNonEmpty(null, "hello").get());
        Assertions.assertEquals("hello", N.firstNonEmpty("hello", null).get());
        Assertions.assertFalse(N.firstNonEmpty((String) null, (String) null).isPresent());

        StringBuilder sb = new StringBuilder("builder");
        StringBuffer buf = new StringBuffer("buffer");
        Assertions.assertEquals(sb, N.firstNonEmpty(sb, buf).get());
    }

    @Test
    public void testFirstNonEmptyCharSequencesThree() {
        Assertions.assertEquals("a", N.firstNonEmpty("a", "b", "c").get());

        Assertions.assertEquals("b", N.firstNonEmpty("", "b", "c").get());

        Assertions.assertEquals("c", N.firstNonEmpty("", "", "c").get());

        Assertions.assertFalse(N.firstNonEmpty("", "", "").isPresent());

        Assertions.assertEquals("c", N.firstNonEmpty(null, "", "c").get());
    }

    @Test
    public void testFirstNonEmptyCharSequencesVarargs() {
        Assertions.assertEquals("c", N.firstNonEmpty("", null, "c", "d").get());

        Assertions.assertFalse(N.firstNonEmpty(new String[] {}).isPresent());

        Assertions.assertFalse(N.firstNonEmpty((String[]) null).isPresent());

        Assertions.assertFalse(N.firstNonEmpty("", "", "").isPresent());
    }

    @Test
    public void testFirstNonEmptyCharSequencesIterable() {
        List<String> list = Arrays.asList("", null, "c", "d");
        Assertions.assertEquals("c", N.firstNonEmpty(list).get());

        Assertions.assertFalse(N.firstNonEmpty(Collections.<String> emptyList()).isPresent());

        Assertions.assertFalse(N.firstNonEmpty((Iterable<String>) null).isPresent());

        List<String> allEmpty = Arrays.asList("", "", null);
        Assertions.assertFalse(N.firstNonEmpty(allEmpty).isPresent());
    }

    @Test
    public void testFirstNonEmptyArrays() {
        String[] a1 = { "a", "b" };
        String[] a2 = { "c", "d" };
        String[] empty = {};

        assertEquals(a1, N.firstNonEmpty(a1, a2).get());
        assertEquals(a2, N.firstNonEmpty(null, a2).get());
        assertEquals(a2, N.firstNonEmpty(empty, a2).get());
        assertFalse(N.firstNonEmpty((String[]) null, (String[]) null).isPresent());
        assertFalse(N.firstNonEmpty(empty, empty).isPresent());

        assertEquals(a1, N.firstNonEmpty(a1, a2, empty).get());
        assertEquals(a2, N.firstNonEmpty(empty, a2, a1).get());
        assertEquals(a1, N.firstNonEmpty(empty, empty, a1).get());
    }

    @Test
    public void testFirstNonEmptyCollections() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("c", "d");
        List<String> empty = Collections.emptyList();

        assertEquals(list1, N.firstNonEmpty(list1, list2).get());
        assertEquals(list2, N.firstNonEmpty(null, list2).get());
        assertEquals(list2, N.firstNonEmpty(empty, list2).get());
        assertFalse(N.firstNonEmpty((List<String>) null, (List<String>) null).isPresent());
        assertFalse(N.firstNonEmpty(empty, empty).isPresent());

        assertEquals(list1, N.firstNonEmpty(list1, list2, empty).get());
    }

    @Test
    public void testFirstNonEmptyMaps() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        Map<String, String> empty = Collections.emptyMap();

        assertEquals(map1, N.firstNonEmpty(map1, map2).get());
        assertEquals(map2, N.firstNonEmpty(null, map2).get());
        assertEquals(map2, N.firstNonEmpty(empty, map2).get());
        assertFalse(N.firstNonEmpty((Map<String, String>) null, (Map<String, String>) null).isPresent());

        assertEquals(map1, N.firstNonEmpty(map1, map2, empty).get());
    }

    @Test
    public void testFirstNonEmptyCharSequences() {
        assertEquals("abc", N.firstNonEmpty("abc", "def").get());
        assertEquals("def", N.firstNonEmpty("", "def").get());
        assertEquals("def", N.firstNonEmpty(null, "def").get());
        assertFalse(N.firstNonEmpty("", "").isPresent());
        assertFalse(N.firstNonEmpty((String) null, (String) null).isPresent());

        assertEquals("abc", N.firstNonEmpty("abc", "def", "ghi").get());
        assertEquals("def", N.firstNonEmpty("", "def", "ghi").get());
        assertEquals("ghi", N.firstNonEmpty("", "", "ghi").get());
    }

    @Test
    public void testFirstNonEmptyOrDefault_Array() {
        String[] arr = { "", null, "found", "other" };
        assertEquals("found", N.firstNonEmptyOrDefault(arr, "default"));
        String[] allEmpty = { "", null };
        assertEquals("default", N.firstNonEmptyOrDefault(allEmpty, "default"));
        assertEquals("default", N.firstNonEmptyOrDefault((String[]) null, "default"));
    }

    @Test
    public void testFirstNonEmptyOrDefault_Iterable() {
        List<String> list = Arrays.asList("", null, "found", "other");
        assertEquals("found", N.firstNonEmptyOrDefault(list, "default"));
        List<String> allEmpty = Arrays.asList("", null);
        assertEquals("default", N.firstNonEmptyOrDefault(allEmpty, "default"));
        assertEquals("default", N.firstNonEmptyOrDefault((Iterable<String>) null, "default"));
    }

    @Test
    public void testFirstNonBlank() {
        com.landawn.abacus.util.u.Optional<String> result = N.firstNonBlank("", "  ", "first", "second");
        assertTrue(result.isPresent());
        assertEquals("first", result.get());
    }

    @Test
    public void testFirstNonBlankTwo() {
        Assertions.assertEquals("hello", N.firstNonBlank("hello", "world").get());

        Assertions.assertEquals("world", N.firstNonBlank("  ", "world").get());

        Assertions.assertEquals("hello", N.firstNonBlank("hello", "  ").get());

        Assertions.assertFalse(N.firstNonBlank("  ", "  ").isPresent());

        Assertions.assertEquals("hello", N.firstNonBlank(null, "hello").get());
        Assertions.assertEquals("hello", N.firstNonBlank("hello", null).get());
        Assertions.assertFalse(N.firstNonBlank((String) null, (String) null).isPresent());
    }

    @Test
    public void testFirstNonBlankThree() {
        Assertions.assertEquals("a", N.firstNonBlank("a", "b", "c").get());

        Assertions.assertEquals("b", N.firstNonBlank("  ", "b", "c").get());

        Assertions.assertEquals("c", N.firstNonBlank("  ", "  ", "c").get());

        Assertions.assertFalse(N.firstNonBlank("  ", "  ", "  ").isPresent());

        Assertions.assertEquals("c", N.firstNonBlank(null, "", "c").get());
        Assertions.assertEquals("c", N.firstNonBlank("  ", "\t", "c").get());
    }

    @Test
    public void testFirstNonBlankVarargs() {
        Assertions.assertEquals("c", N.firstNonBlank("  ", null, "c", "d").get());

        Assertions.assertFalse(N.firstNonBlank(new String[] {}).isPresent());

        Assertions.assertFalse(N.firstNonBlank((String[]) null).isPresent());

        Assertions.assertFalse(N.firstNonBlank("  ", "\t", "\n").isPresent());
    }

    @Test
    public void testFirstNonBlankIterable() {
        List<String> list = Arrays.asList("  ", null, "c", "d");
        Assertions.assertEquals("c", N.firstNonBlank(list).get());

        Assertions.assertFalse(N.firstNonBlank(Collections.<String> emptyList()).isPresent());

        Assertions.assertFalse(N.firstNonBlank((Iterable<String>) null).isPresent());

        List<String> allBlank = Arrays.asList("  ", "\t", null);
        Assertions.assertFalse(N.firstNonBlank(allBlank).isPresent());
    }

    @Test
    public void testFirstNonBlank2() {
        assertEquals("abc", N.firstNonBlank("abc", "def").get());
        assertEquals("def", N.firstNonBlank("  ", "def").get());
        assertEquals("def", N.firstNonBlank(null, "def").get());
        assertFalse(N.firstNonBlank("  ", "  ").isPresent());
        assertFalse(N.firstNonBlank((String) null, (String) null).isPresent());
    }

    @Test
    public void testFirstNonBlank3() {
        assertEquals("abc", N.firstNonBlank("abc", "def", "ghi").get());
        assertEquals("def", N.firstNonBlank("  ", "def", "ghi").get());
        assertEquals("ghi", N.firstNonBlank("  ", "  ", "ghi").get());
        assertFalse(N.firstNonBlank("  ", null, "  ").isPresent());
    }

    @Test
    public void testFirstNonBlankOrDefault_Array() {
        String[] arr = { "  ", "", null, "found" };
        assertEquals("found", N.firstNonBlankOrDefault(arr, "default"));
        String[] allBlank = { "  ", "" };
        assertEquals("default", N.firstNonBlankOrDefault(allBlank, "default"));
        assertEquals("default", N.firstNonBlankOrDefault((String[]) null, "default"));
    }

    @Test
    public void testFirstNonBlankOrDefault_Iterable() {
        List<String> list = Arrays.asList("  ", "", null, "found");
        assertEquals("found", N.firstNonBlankOrDefault(list, "default"));
        List<String> allBlank = Arrays.asList("  ", "");
        assertEquals("default", N.firstNonBlankOrDefault(allBlank, "default"));
        assertEquals("default", N.firstNonBlankOrDefault((Iterable<String>) null, "default"));
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
    public void testFirstOrNullIfEmpty() {
        List<String> list = Arrays.asList("a", "b");
        assertEquals("a", N.firstOrNullIfEmpty(list));
        assertNull(N.firstOrNullIfEmpty(new ArrayList<>()));
    }

    @Test
    public void testFirstOrNullIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals("a", N.firstOrNullIfEmpty(arr));

        Assertions.assertNull(N.firstOrNullIfEmpty(new String[] {}));

        Assertions.assertNull(N.firstOrNullIfEmpty((String[]) null));
    }

    @Test
    public void testFirstOrNullIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", N.firstOrNullIfEmpty(list));

        Assertions.assertNull(N.firstOrNullIfEmpty(Collections.emptyList()));

        Assertions.assertNull(N.firstOrNullIfEmpty((Iterable<String>) null));
    }

    @Test
    public void testFirstOrNullIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", N.firstOrNullIfEmpty(list.iterator()));

        Assertions.assertNull(N.firstOrNullIfEmpty(Collections.emptyIterator()));

        Assertions.assertNull(N.firstOrNullIfEmpty((Iterator<String>) null));
    }

    @Test
    public void testFirstOrDefaultIfEmpty() {
        List<String> list = Arrays.asList("a", "b");
        assertEquals("a", N.firstOrDefaultIfEmpty(list, "default"));
        assertEquals("default", N.firstOrDefaultIfEmpty(new ArrayList<>(), "default"));
    }

    @Test
    public void testFirstOrDefaultIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals("a", N.firstOrDefaultIfEmpty(arr, "default"));

        Assertions.assertEquals("default", N.firstOrDefaultIfEmpty(new String[] {}, "default"));

        Assertions.assertEquals("default", N.firstOrDefaultIfEmpty((String[]) null, "default"));
    }

    @Test
    public void testFirstOrDefaultIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", N.firstOrDefaultIfEmpty(list, "default"));

        Assertions.assertEquals("default", N.firstOrDefaultIfEmpty(Collections.emptyList(), "default"));

        Assertions.assertEquals("default", N.firstOrDefaultIfEmpty((Iterable<String>) null, "default"));

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("a", N.firstOrDefaultIfEmpty(arrayList, "default"));
    }

    @Test
    public void testFirstOrDefaultIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", N.firstOrDefaultIfEmpty(list.iterator(), "default"));

        Assertions.assertEquals("default", N.firstOrDefaultIfEmpty(Collections.emptyIterator(), "default"));

        Assertions.assertEquals("default", N.firstOrDefaultIfEmpty((Iterator<String>) null, "default"));
    }

    @Test
    public void testLastOrNullIfEmpty() {
        List<String> list = Arrays.asList("a", "b");
        assertEquals("b", N.lastOrNullIfEmpty(list));
        assertNull(N.lastOrNullIfEmpty(new ArrayList<>()));
    }

    @Test
    public void testLastOrNullIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals("c", N.lastOrNullIfEmpty(arr));

        Assertions.assertNull(N.lastOrNullIfEmpty(new String[] {}));

        Assertions.assertNull(N.lastOrNullIfEmpty((String[]) null));
    }

    @Test
    public void testLastOrNullIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", N.lastOrNullIfEmpty(list));

        Assertions.assertNull(N.lastOrNullIfEmpty(Collections.emptyList()));

        Assertions.assertNull(N.lastOrNullIfEmpty((Iterable<String>) null));
    }

    @Test
    public void testLastOrNullIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", N.lastOrNullIfEmpty(list.iterator()));

        Assertions.assertNull(N.lastOrNullIfEmpty(Collections.emptyIterator()));

        Assertions.assertNull(N.lastOrNullIfEmpty((Iterator<String>) null));
    }

    @Test
    public void testLastOrDefaultIfEmpty() {
        List<String> list = Arrays.asList("a", "b");
        assertEquals("b", N.lastOrDefaultIfEmpty(list, "default"));
        assertEquals("default", N.lastOrDefaultIfEmpty(new ArrayList<>(), "default"));
    }

    @Test
    public void testLastOrDefaultIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals("c", N.lastOrDefaultIfEmpty(arr, "default"));

        Assertions.assertEquals("default", N.lastOrDefaultIfEmpty(new String[] {}, "default"));

        Assertions.assertEquals("default", N.lastOrDefaultIfEmpty((String[]) null, "default"));
    }

    @Test
    public void testLastOrDefaultIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", N.lastOrDefaultIfEmpty(list, "default"));

        Assertions.assertEquals("default", N.lastOrDefaultIfEmpty(Collections.emptyList(), "default"));

        Assertions.assertEquals("default", N.lastOrDefaultIfEmpty((Iterable<String>) null, "default"));

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("c", N.lastOrDefaultIfEmpty(arrayList, "default"));

        Deque<String> deque = new ArrayDeque<>(list);
        Assertions.assertEquals("c", N.lastOrDefaultIfEmpty(deque, "default"));
    }

    @Test
    public void testLastOrDefaultIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", N.lastOrDefaultIfEmpty(list.iterator(), "default"));

        Assertions.assertEquals("default", N.lastOrDefaultIfEmpty(Collections.emptyIterator(), "default"));

        Assertions.assertEquals("default", N.lastOrDefaultIfEmpty((Iterator<String>) null, "default"));
    }

    @Test
    public void testFindFirst() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        com.landawn.abacus.util.u.Nullable<Integer> result = N.findFirst(list, x -> x > 3);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(4), result.get());
    }

    @Test
    public void testFindFirstArray() {
        String[] arr = { "apple", "banana", "cherry", "date" };
        Predicate<String> startsWithC = s -> s.startsWith("c");
        Predicate<String> startsWithX = s -> s.startsWith("x");

        Assertions.assertEquals("cherry", N.findFirst(arr, startsWithC).get());

        Assertions.assertFalse(N.findFirst(arr, startsWithX).isPresent());

        Assertions.assertFalse(N.findFirst(new String[] {}, startsWithC).isPresent());

        Assertions.assertFalse(N.findFirst((String[]) null, startsWithC).isPresent());
    }

    @Test
    public void testFindFirstIterable() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date");
        Predicate<String> startsWithC = s -> s.startsWith("c");
        Predicate<String> startsWithX = s -> s.startsWith("x");

        Assertions.assertEquals("cherry", N.findFirst(list, startsWithC).get());

        Assertions.assertFalse(N.findFirst(list, startsWithX).isPresent());

        Assertions.assertFalse(N.findFirst(Collections.emptyList(), startsWithC).isPresent());

        Assertions.assertFalse(N.findFirst((Iterable<String>) null, startsWithC).isPresent());
    }

    @Test
    public void testFindFirstIterator() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date");
        Predicate<String> startsWithC = s -> s.startsWith("c");
        Predicate<String> startsWithX = s -> s.startsWith("x");

        Assertions.assertEquals("cherry", N.findFirst(list.iterator(), startsWithC).get());

        Assertions.assertFalse(N.findFirst(list.iterator(), startsWithX).isPresent());

        Assertions.assertFalse(N.findFirst(Collections.emptyIterator(), startsWithC).isPresent());

        Assertions.assertFalse(N.findFirst((Iterator<String>) null, startsWithC).isPresent());
    }

    @Test
    public void testFindLast() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        com.landawn.abacus.util.u.Nullable<Integer> result = N.findLast(list, x -> x < 4);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(3), result.get());
    }

    @Test
    public void testFindLastArray() {
        String[] arr = { "apple", "banana", "cherry", "date", "cucumber" };
        Predicate<String> startsWithC = s -> s.startsWith("c");
        Predicate<String> startsWithX = s -> s.startsWith("x");

        Assertions.assertEquals("cucumber", N.findLast(arr, startsWithC).get());

        Assertions.assertFalse(N.findLast(arr, startsWithX).isPresent());

        Assertions.assertFalse(N.findLast(new String[] {}, startsWithC).isPresent());

        Assertions.assertFalse(N.findLast((String[]) null, startsWithC).isPresent());
    }

    @Test
    public void testFindLastIterable() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date", "cucumber");
        Predicate<String> startsWithC = s -> s.startsWith("c");
        Predicate<String> startsWithX = s -> s.startsWith("x");

        Assertions.assertEquals("cucumber", N.findLast(list, startsWithC).get());

        Assertions.assertFalse(N.findLast(list, startsWithX).isPresent());

        Assertions.assertFalse(N.findLast(Collections.emptyList(), startsWithC).isPresent());

        Assertions.assertFalse(N.findLast((Iterable<String>) null, startsWithC).isPresent());

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("cucumber", N.findLast(arrayList, startsWithC).get());

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals("cucumber", N.findLast(set, startsWithC).get());

        Deque<String> deque = new ArrayDeque<>(list);
        Assertions.assertEquals("cucumber", N.findLast(deque, startsWithC).get());
    }

    // ========== findLast(Iterable, Predicate) - Deque (descendingIterator) path ==========

    @Test
    public void testFindLast_Iterable_Deque() {
        ArrayDeque<Integer> deque = new ArrayDeque<>(Arrays.asList(1, 2, 3, 2, 1));
        com.landawn.abacus.util.u.Nullable<Integer> result = N.findLast(deque, n -> n == 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.get());
    }

    @Test
    public void testFindLast_Iterable_EmptyCollection() {
        com.landawn.abacus.util.u.Nullable<String> result = N.findLast(new ArrayList<String>(), s -> true);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindFirstNonNull() {
    }

    @Test
    public void testFindFirstNonNullArray() {
        String[] arr = { null, "a", null, "b", "c" };
        assertEquals("a", N.findFirstNonNull(arr, s -> true).get());
        assertEquals("b", N.findFirstNonNull(arr, s -> s.equals("b")).get());
        assertFalse(N.findFirstNonNull(arr, s -> s.equals("z")).isPresent());
        assertFalse(N.findFirstNonNull(new String[] { null, null }, s -> true).isPresent());
        assertFalse(N.findFirstNonNull((String[]) null, s -> true).isPresent());
    }

    @Test
    public void testFindFirstNonNullIterable() {
        List<String> list = Arrays.asList(null, "a", null, "b", "c");
        assertEquals("a", N.findFirstNonNull(list, s -> true).get());
        assertEquals("b", N.findFirstNonNull(list, s -> s.equals("b")).get());
        assertFalse(N.findFirstNonNull(list, s -> s.equals("z")).isPresent());
        assertFalse(N.findFirstNonNull(Arrays.asList(null, null), s -> true).isPresent());
        assertFalse(N.findFirstNonNull((Iterable<String>) null, s -> true).isPresent());
    }

    @Test
    public void testFindFirstNonNullIterator() {
        List<String> list = Arrays.asList(null, "a", null, "b", "c");
        assertEquals("a", N.findFirstNonNull(list.iterator(), s -> true).get());
        assertEquals("b", N.findFirstNonNull(list.iterator(), s -> s.equals("b")).get());
        assertFalse(N.findFirstNonNull(list.iterator(), s -> s.equals("z")).isPresent());
        assertFalse(N.findFirstNonNull(Arrays.asList(null, null).iterator(), s -> true).isPresent());
        assertFalse(N.findFirstNonNull((Iterator<String>) null, s -> true).isPresent());
    }

    @Test
    public void testFindLastNonNull() {
    }

    @Test
    public void testFindLastNonNullArray() {
        String[] array = { null, "a", null, "b", "c", null };

        Assertions.assertEquals("b", N.findLastNonNull(array, s -> s.startsWith("b")).orElse(null));
        Assertions.assertEquals("c", N.findLastNonNull(array, s -> s.startsWith("c")).orElse(null));
        Assertions.assertFalse(N.findLastNonNull(array, s -> s.equals("x")).isPresent());
        Assertions.assertFalse(N.findLastNonNull((String[]) null, s -> true).isPresent());

        String[] allNull = { null, null, null };
        Assertions.assertFalse(N.findLastNonNull(allNull, s -> true).isPresent());
    }

    @Test
    public void testFindLastNonNullIterable() {
        List<String> list = Arrays.asList(null, "a", null, "b", "c", null);

        Assertions.assertEquals("b", N.findLastNonNull(list, s -> s.startsWith("b")).orElse(null));
        Assertions.assertEquals("c", N.findLastNonNull(list, s -> s.startsWith("c")).orElse(null));
        Assertions.assertFalse(N.findLastNonNull(list, s -> s.equals("x")).isPresent());
        Assertions.assertFalse(N.findLastNonNull((Iterable<String>) null, s -> true).isPresent());

        List<String> allNull = Arrays.asList(null, null, null);
        Assertions.assertFalse(N.findLastNonNull(allNull, s -> true).isPresent());
    }

    @Test
    public void testHaveSameElementsBoolean() {
        boolean[] a = { true, false, true, false };
        boolean[] b = { false, true, false, true };
        boolean[] c = { true, true, false, false };
        boolean[] d = { true, false, true };

        Assertions.assertTrue(N.containsSameElements(a, b));
        Assertions.assertTrue(N.containsSameElements(a, c));
        Assertions.assertFalse(N.containsSameElements(a, d));
        Assertions.assertTrue(N.containsSameElements((boolean[]) null, (boolean[]) null));
        Assertions.assertTrue(N.containsSameElements(new boolean[0], new boolean[0]));
        Assertions.assertFalse(N.containsSameElements(a, null));
        Assertions.assertFalse(N.containsSameElements(null, a));

        Assertions.assertTrue(N.containsSameElements(a, a));
    }

    @Test
    public void testHaveSameElementsChar() {
        char[] a = { 'a', 'b', 'c', 'b' };
        char[] b = { 'b', 'a', 'c', 'b' };
        char[] c = { 'a', 'b', 'b', 'c' };
        char[] d = { 'a', 'b', 'c' };

        Assertions.assertTrue(N.containsSameElements(a, b));
        Assertions.assertTrue(N.containsSameElements(a, c));
        Assertions.assertFalse(N.containsSameElements(a, d));
        Assertions.assertTrue(N.containsSameElements((char[]) null, (char[]) null));
        Assertions.assertTrue(N.containsSameElements(new char[0], new char[0]));
    }

    @Test
    public void testHaveSameElementsByte() {
        byte[] a = { 1, 2, 3, 2 };
        byte[] b = { 2, 1, 3, 2 };
        byte[] c = { 1, 2, 2, 3 };
        byte[] d = { 1, 2, 3 };

        Assertions.assertTrue(N.containsSameElements(a, b));
        Assertions.assertTrue(N.containsSameElements(a, c));
        Assertions.assertFalse(N.containsSameElements(a, d));
        Assertions.assertTrue(N.containsSameElements((byte[]) null, (byte[]) null));
        Assertions.assertTrue(N.containsSameElements(new byte[0], new byte[0]));
    }

    @Test
    public void testHaveSameElementsShort() {
        short[] a = { 1, 2, 3, 2 };
        short[] b = { 2, 1, 3, 2 };
        short[] c = { 1, 2, 2, 3 };
        short[] d = { 1, 2, 3 };

        Assertions.assertTrue(N.containsSameElements(a, b));
        Assertions.assertTrue(N.containsSameElements(a, c));
        Assertions.assertFalse(N.containsSameElements(a, d));
        Assertions.assertTrue(N.containsSameElements((short[]) null, (short[]) null));
        Assertions.assertTrue(N.containsSameElements(new short[0], new short[0]));
    }

    @Test
    public void testHaveSameElementsInt() {
        int[] a = { 1, 2, 3, 2 };
        int[] b = { 2, 1, 3, 2 };
        int[] c = { 1, 2, 2, 3 };
        int[] d = { 1, 2, 3 };
        int[] e = { 1, 2, 2, 2 };

        Assertions.assertTrue(N.containsSameElements(a, b));
        Assertions.assertTrue(N.containsSameElements(a, c));
        Assertions.assertFalse(N.containsSameElements(a, d));
        Assertions.assertFalse(N.containsSameElements(a, e));
        Assertions.assertTrue(N.containsSameElements((int[]) null, (int[]) null));
        Assertions.assertTrue(N.containsSameElements(new int[0], new int[0]));
    }

    @Test
    public void testHaveSameElementsLong() {
        long[] a = { 1L, 2L, 3L, 2L };
        long[] b = { 2L, 1L, 3L, 2L };
        long[] c = { 1L, 2L, 2L, 3L };
        long[] d = { 1L, 2L, 3L };

        Assertions.assertTrue(N.containsSameElements(a, b));
        Assertions.assertTrue(N.containsSameElements(a, c));
        Assertions.assertFalse(N.containsSameElements(a, d));
        Assertions.assertTrue(N.containsSameElements((long[]) null, (long[]) null));
        Assertions.assertTrue(N.containsSameElements(new long[0], new long[0]));
    }

    @Test
    public void testHaveSameElementsFloat() {
        float[] a = { 1.0f, 2.0f, 3.0f, 2.0f };
        float[] b = { 2.0f, 1.0f, 3.0f, 2.0f };
        float[] c = { 1.0f, 2.0f, 2.0f, 3.0f };
        float[] d = { 1.0f, 2.0f, 3.0f };

        Assertions.assertTrue(N.containsSameElements(a, b));
        Assertions.assertTrue(N.containsSameElements(a, c));
        Assertions.assertFalse(N.containsSameElements(a, d));
        Assertions.assertTrue(N.containsSameElements((float[]) null, (float[]) null));
        Assertions.assertTrue(N.containsSameElements(new float[0], new float[0]));
    }

    @Test
    public void testHaveSameElementsDouble() {
        double[] a = { 1.0, 2.0, 3.0, 2.0 };
        double[] b = { 2.0, 1.0, 3.0, 2.0 };
        double[] c = { 1.0, 2.0, 2.0, 3.0 };
        double[] d = { 1.0, 2.0, 3.0 };

        Assertions.assertTrue(N.containsSameElements(a, b));
        Assertions.assertTrue(N.containsSameElements(a, c));
        Assertions.assertFalse(N.containsSameElements(a, d));
        Assertions.assertTrue(N.containsSameElements((double[]) null, (double[]) null));
        Assertions.assertTrue(N.containsSameElements(new double[0], new double[0]));
    }

    @Test
    public void testHaveSameElementsObject() {
        String[] a = { "a", "b", "c", "b" };
        String[] b = { "b", "a", "c", "b" };
        String[] c = { "a", "b", "b", "c" };
        String[] d = { "a", "b", "c" };

        Assertions.assertTrue(N.containsSameElements(a, b));
        Assertions.assertTrue(N.containsSameElements(a, c));
        Assertions.assertFalse(N.containsSameElements(a, d));
        Assertions.assertTrue(N.containsSameElements((Object[]) null, (Object[]) null));
        Assertions.assertTrue(N.containsSameElements(new Object[0], new Object[0]));

        String[] withNulls1 = { "a", null, "b", null };
        String[] withNulls2 = { null, "a", null, "b" };
        Assertions.assertTrue(N.containsSameElements(withNulls1, withNulls2));
    }

    // Tests for containsSameElements for char, byte, short, long, float, double arrays
    @Test
    public void testContainsSameElements_CharArray() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'c', 'b', 'a' };
        char[] c = { 'a', 'b', 'd' };
        assertTrue(N.containsSameElements(a, b));
        assertFalse(N.containsSameElements(a, c));
        assertTrue(N.containsSameElements((char[]) null, (char[]) null));
        assertFalse(N.containsSameElements(a, new char[] { 'a', 'b' }));
    }

    @Test
    public void testContainsSameElements_ByteArray() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 3, 2, 1 };
        byte[] c = { 1, 2, 4 };
        assertTrue(N.containsSameElements(a, b));
        assertFalse(N.containsSameElements(a, c));
    }

    @Test
    public void testContainsSameElements_ShortArray() {
        short[] a = { 1, 2, 3 };
        short[] b = { 3, 2, 1 };
        short[] c = { 1, 2, 4 };
        assertTrue(N.containsSameElements(a, b));
        assertFalse(N.containsSameElements(a, c));
    }

    @Test
    public void testContainsSameElements_LongArray() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 3L, 2L, 1L };
        long[] c = { 1L, 2L, 4L };
        assertTrue(N.containsSameElements(a, b));
        assertFalse(N.containsSameElements(a, c));
    }

    @Test
    public void testContainsSameElements_FloatArray() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        float[] b = { 3.0f, 2.0f, 1.0f };
        float[] c = { 1.0f, 2.0f, 4.0f };
        assertTrue(N.containsSameElements(a, b));
        assertFalse(N.containsSameElements(a, c));
    }

    @Test
    public void testContainsSameElements_DoubleArray() {
        double[] a = { 1.0, 2.0, 3.0 };
        double[] b = { 3.0, 2.0, 1.0 };
        double[] c = { 1.0, 2.0, 4.0 };
        assertTrue(N.containsSameElements(a, b));
        assertFalse(N.containsSameElements(a, c));
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
    public void testMismatch_Collection_WithRange() {
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("x", "b", "c", "y");

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
    public void testMismatch_Collection_WithRange_AndComparator() {
        List<String> a = Arrays.asList("x", "b", "c", "y");
        List<String> b = Arrays.asList("z", "B", "D", "w");
        Comparator<String> cmp = String.CASE_INSENSITIVE_ORDER;

        assertEquals(1, N.mismatch(a, 1, b, 1, 3, cmp));
    }

    @Test
    public void testMismatchBoolean() {
        boolean[] a = { true, false, true, false };
        boolean[] b = { true, false, true, false };
        boolean[] c = { true, true, false, false };
        boolean[] d = { true, false, true };

        Assertions.assertEquals(-1, N.mismatch(a, b));
        Assertions.assertEquals(1, N.mismatch(a, c));
        Assertions.assertEquals(3, N.mismatch(a, d));
        Assertions.assertEquals(-1, N.mismatch((boolean[]) null, (boolean[]) null));
        Assertions.assertEquals(-1, N.mismatch(new boolean[0], new boolean[0]));
        Assertions.assertEquals(0, N.mismatch(a, null));
        Assertions.assertEquals(0, N.mismatch(null, a));

        Assertions.assertEquals(-1, N.mismatch(a, a));
    }

    @Test
    public void testMismatchChar() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] b = { 'a', 'b', 'c', 'd' };
        char[] c = { 'a', 'x', 'c', 'd' };
        char[] d = { 'a', 'b', 'c' };

        Assertions.assertEquals(-1, N.mismatch(a, b));
        Assertions.assertEquals(1, N.mismatch(a, c));
        Assertions.assertEquals(3, N.mismatch(a, d));
        Assertions.assertEquals(-1, N.mismatch((char[]) null, (char[]) null));
        Assertions.assertEquals(-1, N.mismatch(new char[0], new char[0]));
        Assertions.assertEquals(0, N.mismatch(a, null));
        Assertions.assertEquals(0, N.mismatch(null, a));
    }

    @Test
    public void testMismatchByte() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] b = { 1, 2, 3, 4 };
        byte[] c = { 1, 5, 3, 4 };
        byte[] d = { 1, 2, 3 };

        Assertions.assertEquals(-1, N.mismatch(a, b));
        Assertions.assertEquals(1, N.mismatch(a, c));
        Assertions.assertEquals(3, N.mismatch(a, d));
        Assertions.assertEquals(-1, N.mismatch((byte[]) null, (byte[]) null));
        Assertions.assertEquals(-1, N.mismatch(new byte[0], new byte[0]));
    }

    @Test
    public void testMismatchShort() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 1, 2, 3, 4 };
        short[] c = { 1, 5, 3, 4 };
        short[] d = { 1, 2, 3 };

        Assertions.assertEquals(-1, N.mismatch(a, b));
        Assertions.assertEquals(1, N.mismatch(a, c));
        Assertions.assertEquals(3, N.mismatch(a, d));
        Assertions.assertEquals(-1, N.mismatch((short[]) null, (short[]) null));
        Assertions.assertEquals(-1, N.mismatch(new short[0], new short[0]));
    }

    @Test
    public void testMismatchInt() {
        int[] a = { 1, 2, 3, 4 };
        int[] b = { 1, 2, 3, 4 };
        int[] c = { 1, 5, 3, 4 };
        int[] d = { 1, 2, 3 };

        Assertions.assertEquals(-1, N.mismatch(a, b));
        Assertions.assertEquals(1, N.mismatch(a, c));
        Assertions.assertEquals(3, N.mismatch(a, d));
        Assertions.assertEquals(-1, N.mismatch((int[]) null, (int[]) null));
        Assertions.assertEquals(-1, N.mismatch(new int[0], new int[0]));
    }

    @Test
    public void testMismatchLong() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 1L, 2L, 3L, 4L };
        long[] c = { 1L, 5L, 3L, 4L };
        long[] d = { 1L, 2L, 3L };

        Assertions.assertEquals(-1, N.mismatch(a, b));
        Assertions.assertEquals(1, N.mismatch(a, c));
        Assertions.assertEquals(3, N.mismatch(a, d));
        Assertions.assertEquals(-1, N.mismatch((long[]) null, (long[]) null));
        Assertions.assertEquals(-1, N.mismatch(new long[0], new long[0]));
    }

    @Test
    public void testMismatchFloat() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] b = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] c = { 1.0f, 5.0f, 3.0f, 4.0f };
        float[] d = { 1.0f, 2.0f, 3.0f };

        Assertions.assertEquals(-1, N.mismatch(a, b));
        Assertions.assertEquals(1, N.mismatch(a, c));
        Assertions.assertEquals(3, N.mismatch(a, d));
        Assertions.assertEquals(-1, N.mismatch((float[]) null, (float[]) null));
        Assertions.assertEquals(-1, N.mismatch(new float[0], new float[0]));
    }

    @Test
    public void testMismatchDouble() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] b = { 1.0, 2.0, 3.0, 4.0 };
        double[] c = { 1.0, 5.0, 3.0, 4.0 };
        double[] d = { 1.0, 2.0, 3.0 };

        Assertions.assertEquals(-1, N.mismatch(a, b));
        Assertions.assertEquals(1, N.mismatch(a, c));
        Assertions.assertEquals(3, N.mismatch(a, d));
        Assertions.assertEquals(-1, N.mismatch((double[]) null, (double[]) null));
        Assertions.assertEquals(-1, N.mismatch(new double[0], new double[0]));
    }

    @Test
    public void testMismatchObjectArrays() {
        String[] a = { "a", "b", "c", "d" };
        String[] b = { "a", "b", "c", "d" };
        String[] c = { "a", "x", "c", "d" };
        String[] d = { "a", "b", "c" };

        Assertions.assertEquals(-1, N.mismatch(a, b));
        Assertions.assertEquals(1, N.mismatch(a, c));
        Assertions.assertEquals(3, N.mismatch(a, d));
        Assertions.assertEquals(-1, N.mismatch((String[]) null, (String[]) null));
        Assertions.assertEquals(-1, N.mismatch(new String[0], new String[0]));
        Assertions.assertEquals(0, N.mismatch(a, null));
        Assertions.assertEquals(0, N.mismatch(null, a));
    }

    @Test
    public void testMismatchObjectArraysWithComparator() {
        String[] a = { "A", "B", "C", "D" };
        String[] b = { "a", "b", "c", "d" };
        String[] c = { "a", "x", "c", "d" };

        Assertions.assertEquals(-1, N.mismatch(a, b, String.CASE_INSENSITIVE_ORDER));
        Assertions.assertEquals(1, N.mismatch(a, c, String.CASE_INSENSITIVE_ORDER));
        Assertions.assertEquals(-1, N.mismatch(a, a));
    }

    @Test
    public void testMismatchCollectionsWithComparator() {
        List<String> a = Arrays.asList("A", "B", "C", "D");
        List<String> b = Arrays.asList("x", "b", "c", "y");

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2, String.CASE_INSENSITIVE_ORDER));
        Assertions.assertEquals(-1, N.mismatch(a, 1, a, 1, 2, String.CASE_INSENSITIVE_ORDER));
    }

    @Test
    public void testMismatchIterables() {
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("a", "b", "c", "d");
        List<String> c = Arrays.asList("a", "x", "c", "d");
        List<String> d = Arrays.asList("a", "b", "c");

        Assertions.assertEquals(-1, N.mismatch(a, b));
        Assertions.assertEquals(1, N.mismatch(a, c));
        Assertions.assertEquals(3, N.mismatch(a, d));
        Assertions.assertEquals(-1, N.mismatch((Iterable<String>) null, (Iterable<String>) null));
        Assertions.assertEquals(-1, N.mismatch(new ArrayList<String>(), new ArrayList<String>()));
        Assertions.assertEquals(0, N.mismatch(a, (Iterable<String>) null));
        Assertions.assertEquals(0, N.mismatch((Iterable<String>) null, a));

        Assertions.assertEquals(-1, N.mismatch(a, a));
    }

    @Test
    public void testMismatchIterablesWithComparator() {
        List<String> a = Arrays.asList("A", "B", "C", "D");
        List<String> b = Arrays.asList("a", "b", "c", "d");
        List<String> c = Arrays.asList("a", "x", "c", "d");

        Assertions.assertEquals(-1, N.mismatch(a, b, String.CASE_INSENSITIVE_ORDER));
        Assertions.assertEquals(1, N.mismatch(a, c, String.CASE_INSENSITIVE_ORDER));
    }

    @Test
    public void testMismatchIterators() {
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("a", "b", "c", "d");
        List<String> c = Arrays.asList("a", "x", "c", "d");
        List<String> d = Arrays.asList("a", "b", "c");
        List<String> e = Arrays.asList("a", "b", "c", "d", "e");

        Assertions.assertEquals(-1, N.mismatch(a.iterator(), b.iterator()));
        Assertions.assertEquals(1, N.mismatch(a.iterator(), c.iterator()));
        Assertions.assertEquals(3, N.mismatch(a.iterator(), d.iterator()));
        Assertions.assertEquals(4, N.mismatch(a.iterator(), e.iterator()));
        Assertions.assertEquals(-1, N.mismatch((Iterator<String>) null, (Iterator<String>) null));

        Iterator<String> iter = a.iterator();
        Assertions.assertEquals(-1, N.mismatch(iter, iter));
    }

    @Test
    public void testMismatchIteratorsWithComparator() {
        List<String> a = Arrays.asList("A", "B", "C", "D");
        List<String> b = Arrays.asList("a", "b", "c", "d");
        List<String> c = Arrays.asList("a", "x", "c", "d");

        Assertions.assertEquals(-1, N.mismatch(a.iterator(), b.iterator(), String.CASE_INSENSITIVE_ORDER));
        Assertions.assertEquals(1, N.mismatch(a.iterator(), c.iterator(), String.CASE_INSENSITIVE_ORDER));
    }

    // Tests for mismatch(Collection, fromIndexA, Collection, fromIndexB, len, Comparator)
    @Test
    public void testMismatch_Collection_WithFromIndex_AndComparator() {
        List<String> a = Arrays.asList("x", "a", "b", "c", "y");
        List<String> b = Arrays.asList("z", "a", "b", "d", "w");
        // Compare [a,b,c] vs [a,b,d] - mismatch at index 2 (relative)
        assertEquals(2, N.mismatch(a, 1, b, 1, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testMismatch_Collection_WithFromIndex_NoMismatch() {
        List<String> a = Arrays.asList("x", "a", "b", "c", "y");
        List<String> b = Arrays.asList("z", "a", "b", "c", "w");
        assertEquals(-1, N.mismatch(a, 1, b, 1, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testMismatch_Collection_SameRef_ReturnsMinus1() {
        List<String> a = Arrays.asList("x", "a", "b");
        assertEquals(-1, N.mismatch(a, 0, a, 0, 2, Comparator.naturalOrder()));
    }

    @Test
    public void testMismatchBooleanRange() {
        boolean[] a = { true, false, true, false };
        boolean[] b = { false, false, true, true };

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, N.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.mismatch(a, 0, b, 0, -1));

        Assertions.assertEquals(-1, N.mismatch(a, 1, a, 1, 2));

        Assertions.assertEquals(-1, N.mismatch(a, 0, b, 0, 0));
    }

    @Test
    public void testMismatchCharRange() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] b = { 'x', 'b', 'c', 'y' };

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, N.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchByteRange() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] b = { 5, 2, 3, 6 };

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, N.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchShortRange() {
        short[] a = { 1, 2, 3, 4 };
        short[] b = { 5, 2, 3, 6 };

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, N.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchIntRange() {
        int[] a = { 1, 2, 3, 4 };
        int[] b = { 5, 2, 3, 6 };

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, N.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchLongRange() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] b = { 5L, 2L, 3L, 6L };

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, N.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchFloatRange() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] b = { 5.0f, 2.0f, 3.0f, 6.0f };

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, N.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchDoubleRange() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] b = { 5.0, 2.0, 3.0, 6.0 };

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, N.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchObjectArraysRange() {
        String[] a = { "a", "b", "c", "d" };
        String[] b = { "x", "b", "c", "y" };

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, N.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10));
    }

    @Test
    public void testMismatchObjectArraysRangeWithComparator() {
        String[] a = { "A", "B", "C", "D" };
        String[] b = { "x", "b", "c", "y" };

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2, String.CASE_INSENSITIVE_ORDER));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10, String.CASE_INSENSITIVE_ORDER));
    }

    @Test
    public void testMismatchCollections() {
        List<String> a = Arrays.asList("a", "b", "c", "d");
        List<String> b = Arrays.asList("x", "b", "c", "y");

        Assertions.assertEquals(-1, N.mismatch(a, 1, b, 1, 2));
        Assertions.assertEquals(0, N.mismatch(a, 0, b, 0, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.mismatch(a, 0, b, 0, 10));
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
    public void testReverseByteRange() {
        byte[] a = { 1, 2, 3, 4, 5 };
        byte[] expected = { 1, 4, 3, 2, 5 };
        N.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testReverseShortRange() {
        short[] a = { 1, 2, 3, 4, 5 };
        short[] expected = { 1, 4, 3, 2, 5 };
        N.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testReverseIntRange() {
        int[] a = { 1, 2, 3, 4, 5 };
        int[] expected = { 1, 4, 3, 2, 5 };
        N.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testReverseLongRange() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        long[] expected = { 1L, 4L, 3L, 2L, 5L };
        N.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testReverseFloatRange() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] expected = { 1.0f, 4.0f, 3.0f, 2.0f, 5.0f };
        N.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a, 0.0f);
    }

    @Test
    public void testReverseDoubleRange() {
        double[] a = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] expected = { 1.0, 4.0, 3.0, 2.0, 5.0 };
        N.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a, 0.0);
    }

    @Test
    public void testReverseObjectRange() {
        String[] a = { "a", "b", "c", "d", "e" };
        String[] expected = { "a", "d", "c", "b", "e" };
        N.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testReverseBoolean() {
        boolean[] a = { true, false, true, false };
        boolean[] expected = { false, true, false, true };
        N.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        boolean[] empty = {};
        N.reverse(empty);
        Assertions.assertArrayEquals(new boolean[0], empty);

        N.reverse((boolean[]) null);

        boolean[] single = { true };
        N.reverse(single);
        Assertions.assertArrayEquals(new boolean[] { true }, single);
    }

    @Test
    public void testReverseChar() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] expected = { 'd', 'c', 'b', 'a' };
        N.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        char[] empty = {};
        N.reverse(empty);
        Assertions.assertArrayEquals(new char[0], empty);

        N.reverse((char[]) null);
    }

    @Test
    public void testReverseByte() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] expected = { 4, 3, 2, 1 };
        N.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        byte[] empty = {};
        N.reverse(empty);
        Assertions.assertArrayEquals(new byte[0], empty);

        N.reverse((byte[]) null);
    }

    @Test
    public void testReverseShort() {
        short[] a = { 1, 2, 3, 4 };
        short[] expected = { 4, 3, 2, 1 };
        N.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        short[] empty = {};
        N.reverse(empty);
        Assertions.assertArrayEquals(new short[0], empty);

        N.reverse((short[]) null);
    }

    @Test
    public void testReverseInt() {
        int[] a = { 1, 2, 3, 4 };
        int[] expected = { 4, 3, 2, 1 };
        N.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        int[] empty = {};
        N.reverse(empty);
        Assertions.assertArrayEquals(new int[0], empty);

        N.reverse((int[]) null);
    }

    @Test
    public void testReverseLong() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] expected = { 4L, 3L, 2L, 1L };
        N.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        long[] empty = {};
        N.reverse(empty);
        Assertions.assertArrayEquals(new long[0], empty);

        N.reverse((long[]) null);
    }

    @Test
    public void testReverseFloat() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] expected = { 4.0f, 3.0f, 2.0f, 1.0f };
        N.reverse(a);
        Assertions.assertArrayEquals(expected, a, 0.0f);

        float[] empty = {};
        N.reverse(empty);
        Assertions.assertArrayEquals(new float[0], empty, 0.0f);

        N.reverse((float[]) null);
    }

    @Test
    public void testReverseDouble() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] expected = { 4.0, 3.0, 2.0, 1.0 };
        N.reverse(a);
        Assertions.assertArrayEquals(expected, a, 0.0);

        double[] empty = {};
        N.reverse(empty);
        Assertions.assertArrayEquals(new double[0], empty, 0.0);

        N.reverse((double[]) null);
    }

    @Test
    public void testReverseObject() {
        String[] a = { "a", "b", "c", "d" };
        String[] expected = { "d", "c", "b", "a" };
        N.reverse(a);
        Assertions.assertArrayEquals(expected, a);

        String[] empty = {};
        N.reverse(empty);
        Assertions.assertArrayEquals(new String[0], empty);

        N.reverse((String[]) null);
    }

    @Test
    public void testReverseList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        N.reverse(list);
        Assertions.assertEquals(Arrays.asList("d", "c", "b", "a"), list);

        List<String> empty = new ArrayList<>();
        N.reverse(empty);
        Assertions.assertTrue(empty.isEmpty());

        N.reverse((List<?>) null);

        List<String> single = new ArrayList<>(Arrays.asList("a"));
        N.reverse(single);
        Assertions.assertEquals(Arrays.asList("a"), single);
    }

    @Test
    public void testReverseCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        N.reverse((Collection<?>) list);
        Assertions.assertEquals(Arrays.asList("d", "c", "b", "a"), list);

        Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        N.reverse(set);
        Assertions.assertEquals(4, set.size());

        Collection<String> empty = new ArrayList<>();
        N.reverse(empty);
        Assertions.assertTrue(empty.isEmpty());

        N.reverse((Collection<?>) null);

        Collection<String> single = new ArrayList<>(Arrays.asList("a"));
        N.reverse(single);
        Assertions.assertEquals(1, single.size());
    }

    @Test
    public void testReverseBooleanRange() {
        boolean[] a = { true, false, true, false, true };
        boolean[] expected = { true, false, true, false, true };
        N.reverse(a, 1, 4);
        expected[1] = false;
        expected[2] = true;
        expected[3] = false;
        Assertions.assertArrayEquals(expected, a);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.reverse(a, -1, 3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.reverse(a, 0, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.reverse(a, 3, 2));

        boolean[] b = { true, false, true };
        boolean[] original = b.clone();
        N.reverse(b, 1, 1);
        Assertions.assertArrayEquals(original, b);
    }

    @Test
    public void testReverseCharRange() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        char[] expected = { 'a', 'd', 'c', 'b', 'e' };
        N.reverse(a, 1, 4);
        Assertions.assertArrayEquals(expected, a);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.reverse(a, -1, 3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.reverse(a, 0, 10));
    }

    @Test
    public void testReverseListRange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        N.reverse(list, 1, 4);
        Assertions.assertEquals(Arrays.asList("a", "d", "c", "b", "e"), list);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.reverse(list, -1, 3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.reverse(list, 0, 10));

        List<String> list2 = new ArrayList<>(Arrays.asList("a", "b", "c"));
        N.reverse(list2, 1, 1);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), list2);
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
    public void testRotateChar() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] expected = { 'd', 'a', 'b', 'c' };
        N.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);

        char[] b = { 'a', 'b', 'c', 'd' };
        char[] expected2 = { 'b', 'c', 'd', 'a' };
        N.rotate(b, -1);
        Assertions.assertArrayEquals(expected2, b);

        char[] c = { 'a', 'b', 'c', 'd' };
        char[] expected3 = { 'b', 'c', 'd', 'a' };
        N.rotate(c, 7);
        Assertions.assertArrayEquals(expected3, c);
    }

    @Test
    public void testRotateCharRange() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        char[] expected = { 'a', 'd', 'b', 'c', 'e' };
        N.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateByte() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] expected = { 4, 1, 2, 3 };
        N.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateByteRange() {
        byte[] a = { 1, 2, 3, 4, 5 };
        byte[] expected = { 1, 4, 2, 3, 5 };
        N.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateShort() {
        short[] a = { 1, 2, 3, 4 };
        short[] expected = { 4, 1, 2, 3 };
        N.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateShortRange() {
        short[] a = { 1, 2, 3, 4, 5 };
        short[] expected = { 1, 4, 2, 3, 5 };
        N.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateInt() {
        int[] a = { 1, 2, 3, 4, 5 };
        int[] expected = { 4, 5, 1, 2, 3 };
        N.rotate(a, 2);
        Assertions.assertArrayEquals(expected, a);

        int[] b = { 1, 2, 3, 4, 5 };
        int[] expected2 = { 2, 3, 4, 5, 1 };
        N.rotate(b, -1);
        Assertions.assertArrayEquals(expected2, b);

        int[] c = { 1, 2, 3, 4, 5 };
        int[] expected3 = { 4, 5, 1, 2, 3 };
        N.rotate(c, 7);
        Assertions.assertArrayEquals(expected3, c);
    }

    @Test
    public void testRotateIntRange() {

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 2, 3, 4, 5 };
            N.rotate(a, 0);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 5, 1, 2, 3, 4 };
            N.rotate(a, 1);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 4, 5, 1, 2, 3 };
            N.rotate(a, 2);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 3, 4, 5, 1, 2 };
            N.rotate(a, 3);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 2, 3, 4, 5, 1 };
            N.rotate(a, 4);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 2, 3, 4, 5 };
            N.rotate(a, 5);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 5, 1, 2, 3, 4 };
            N.rotate(a, 6);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 2, 3, 4, 5, 1 };
            N.rotate(a, -1);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 3, 4, 5, 1, 2 };
            N.rotate(a, -2);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 4, 2, 3, 5 };
            N.rotate(a, 1, 4, 1);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 3, 4, 2, 5 };
            N.rotate(a, 1, 4, 2);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 2, 3, 4, 5 };
            N.rotate(a, 1, 4, 3);
            Assertions.assertArrayEquals(expected, a);
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            int[] expected = { 1, 3, 4, 2, 5 };
            N.rotate(a, 1, 4, -1);
            Assertions.assertArrayEquals(expected, a);
        }
    }

    @Test
    public void testRotateLong() {
        long[] a = { 1L, 2L, 3L, 4L };
        long[] expected = { 4L, 1L, 2L, 3L };
        N.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateLongRange() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        long[] expected = { 1L, 4L, 2L, 3L, 5L };
        N.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateFloat() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] expected = { 4.0f, 1.0f, 2.0f, 3.0f };
        N.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a, 0.0f);
    }

    @Test
    public void testRotateFloatRange() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] expected = { 1.0f, 4.0f, 2.0f, 3.0f, 5.0f };
        N.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a, 0.0f);
    }

    @Test
    public void testRotateDouble() {
        double[] a = { 1.0, 2.0, 3.0, 4.0 };
        double[] expected = { 4.0, 1.0, 2.0, 3.0 };
        N.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a, 0.0);
    }

    @Test
    public void testRotateDoubleRange() {
        double[] a = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] expected = { 1.0, 4.0, 2.0, 3.0, 5.0 };
        N.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a, 0.0);
    }

    @Test
    public void testRotateObject() {
        String[] a = { "a", "b", "c", "d" };
        String[] expected = { "d", "a", "b", "c" };
        N.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    @Test
    public void testRotateObjectRange() {
        String[] a = { "a", "b", "c", "d", "e" };
        String[] expected = { "a", "d", "b", "c", "e" };
        N.rotate(a, 1, 4, 1);
        Assertions.assertArrayEquals(expected, a);
    }

    // Tests for rotate with range - char, short, long, float, double arrays
    @Test
    public void testRotate_CharArray_WithRange() {
        char[] arr = { 'a', 'b', 'c', 'd', 'e' };
        N.rotate(arr, 1, 4, 1);
        assertArrayEquals(new char[] { 'a', 'd', 'b', 'c', 'e' }, arr);
    }

    @Test
    public void testRotate_ShortArray_WithRange() {
        short[] arr = { 1, 2, 3, 4, 5 };
        N.rotate(arr, 1, 4, 1);
        assertArrayEquals(new short[] { 1, 4, 2, 3, 5 }, arr);
    }

    @Test
    public void testRotate_LongArray_WithRange() {
        long[] arr = { 1L, 2L, 3L, 4L, 5L };
        N.rotate(arr, 1, 4, 1);
        assertArrayEquals(new long[] { 1L, 4L, 2L, 3L, 5L }, arr);
    }

    @Test
    public void testRotate_FloatArray_WithRange() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        N.rotate(arr, 1, 4, 1);
        assertArrayEquals(new float[] { 1.0f, 4.0f, 2.0f, 3.0f, 5.0f }, arr, 0.0f);
    }

    @Test
    public void testRotate_DoubleArray_WithRange() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        N.rotate(arr, 1, 4, 1);
        assertArrayEquals(new double[] { 1.0, 4.0, 2.0, 3.0, 5.0 }, arr, 0.0);
    }

    @Test
    public void testRotate_IntArray_NegativeDistance() {
        int[] arr = { 1, 2, 3, 4 };
        N.rotate(arr, -1);
        int[] expected = { 2, 3, 4, 1 };
        assertTrue(N.equals(arr, expected));
    }

    @Test
    public void testRotateBoolean() {
        boolean[] a = { true, false, true, false };
        boolean[] expected = { false, true, false, true };
        N.rotate(a, 1);
        Assertions.assertArrayEquals(expected, a);

        boolean[] b = { true, false, true, false };
        boolean[] expected2 = { false, true, false, true };
        N.rotate(b, -1);
        Assertions.assertArrayEquals(expected2, b);

        boolean[] c = { true, false, true, false };
        boolean[] original = c.clone();
        N.rotate(c, 4);
        Assertions.assertArrayEquals(original, c);

        boolean[] empty = {};
        N.rotate(empty, 1);
        Assertions.assertArrayEquals(new boolean[0], empty);

        N.rotate((boolean[]) null, 1);
    }

    @Test
    public void testRotateList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        N.rotate(list, 1);
        Assertions.assertEquals(Arrays.asList("d", "a", "b", "c"), list);

        N.rotate((List<?>) null, 1);

        List<String> single = new ArrayList<>(Arrays.asList("a"));
        N.rotate(single, 1);
        Assertions.assertEquals(Arrays.asList("a"), single);

        List<String> list2 = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        N.rotate(list2, 4);
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d"), list2);
    }

    @Test
    public void testRotateCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        N.rotate((Collection<?>) list, 1);
        Assertions.assertEquals(Arrays.asList("d", "a", "b", "c"), list);

        Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        N.rotate(set, 1);
        Assertions.assertEquals(4, set.size());

        Collection<String> empty = new ArrayList<>();
        N.rotate(empty, 1);
        Assertions.assertTrue(empty.isEmpty());

        N.rotate((Collection<?>) null, 1);

        Collection<String> single = new ArrayList<>(Arrays.asList("a"));
        N.rotate(single, 1);
        Assertions.assertEquals(1, single.size());
    }

    @Test
    public void testRotate_CharArray_WithRange_NegativeDistance() {
        char[] arr = { 'a', 'b', 'c', 'd', 'e' };
        N.rotate(arr, 1, 4, -1);
        assertArrayEquals(new char[] { 'a', 'c', 'd', 'b', 'e' }, arr);
    }

    @Test
    public void testRotate_ShortArray_WithRange_NegativeDistance() {
        short[] arr = { 1, 2, 3, 4, 5 };
        N.rotate(arr, 1, 4, -1);
        assertArrayEquals(new short[] { 1, 3, 4, 2, 5 }, arr);
    }

    @Test
    public void testRotate_LongArray_WithRange_NegativeDistance() {
        long[] arr = { 1L, 2L, 3L, 4L, 5L };
        N.rotate(arr, 1, 4, -1);
        assertArrayEquals(new long[] { 1L, 3L, 4L, 2L, 5L }, arr);
    }

    @Test
    public void testRotate_FloatArray_WithRange_NegativeDistance() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        N.rotate(arr, 1, 4, -1);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 4.0f, 2.0f, 5.0f }, arr, 0.0f);
    }

    @Test
    public void testRotate_DoubleArray_WithRange_NegativeDistance() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        N.rotate(arr, 1, 4, -1);
        assertArrayEquals(new double[] { 1.0, 3.0, 4.0, 2.0, 5.0 }, arr, 0.0);
    }

    @Test
    public void testRotateBooleanRange() {
        boolean[] a = { true, false, true, false, true };
        boolean[] expected = { true, false, true, false, true };
        N.rotate(a, 1, 4, 1);
        expected[1] = false;
        expected[2] = false;
        expected[3] = true;
        Assertions.assertArrayEquals(expected, a);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.rotate(a, -1, 3, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.rotate(a, 0, 10, 1));

        boolean[] b = { true, false, true };
        boolean[] original = b.clone();
        N.rotate(b, 1, 1, 1);
        Assertions.assertArrayEquals(original, b);
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
    public void testShuffleFloatArrayWithRange() {

        for (int i = 0; i < 10000; i++) {
            float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
            float[] original = arr.clone();
            N.shuffle(arr, 1, 4);

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
            N.shuffle(arr, rnd);

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
            N.shuffle(arr, 1, 4, rnd);

            Assertions.assertEquals(original[0], arr[0]);
            Assertions.assertEquals(original[4], arr[4]);

            Arrays.sort(arr);
            Arrays.sort(original);
            Assertions.assertArrayEquals(original, arr);
        }
    }

    @Test
    public void testShuffleDoubleArray() {

        for (int i = 0; i < 10000; i++) {
            double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
            double[] original = arr.clone();
            N.shuffle(arr);

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
            N.shuffle(arr, 1, 4);

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
        N.shuffle(arr, rnd);

        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    @Test
    public void testShuffleDoubleArrayWithRangeAndRandom() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] original = arr.clone();
        Random rnd = new Random(42);
        N.shuffle(arr, 1, 4, rnd);

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
        N.shuffle(arr);

        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    @Test
    public void testShuffleObjectArrayWithRange() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String[] original = arr.clone();
        N.shuffle(arr, 1, 4);

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
        N.shuffle(arr, rnd);

        Arrays.sort(arr);
        Arrays.sort(original);
        Assertions.assertArrayEquals(original, arr);
    }

    @Test
    public void testShuffleObjectArrayWithRangeAndRandom() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String[] original = arr.clone();
        Random rnd = new Random(42);
        N.shuffle(arr, 1, 4, rnd);

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
        N.shuffle(list);

        Collections.sort(list);
        Collections.sort(original);
        Assertions.assertEquals(original, list);
    }

    @Test
    public void testShuffleListWithRandom() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        List<String> original = new ArrayList<>(list);
        Random rnd = new Random(42);
        N.shuffle(list, rnd);

        Collections.sort(list);
        Collections.sort(original);
        Assertions.assertEquals(original, list);
    }

    @Test
    public void testShuffleCollection() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        List<String> original = new ArrayList<>(coll);
        N.shuffle(coll);

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
        N.shuffle(coll, rnd);

        List<String> sorted = new ArrayList<>(coll);
        Collections.sort(sorted);
        Collections.sort(original);
        Assertions.assertEquals(original, sorted);
    }

    @Test
    public void testShuffleCollectionLessThanTwoElements() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a"));
        N.shuffle(coll);
        Assertions.assertEquals(1, coll.size());
        Assertions.assertTrue(coll.contains("a"));
    }

    @Test
    public void testShuffleBooleanWithRandom() {
        boolean[] a = { true, false, true, false, true };
        Random rnd = new Random(12345);
        N.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleBooleanRangeWithRandom() {
        boolean[] a = { true, false, true, false, true };
        boolean first = a[0];
        boolean last = a[4];
        Random rnd = new Random(12345);
        N.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleChar() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        char[] original = a.clone();
        N.shuffle(a);
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
        N.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleCharWithRandom() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        Random rnd = new Random(12345);
        N.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleCharRangeWithRandom() {
        char[] a = { 'a', 'b', 'c', 'd', 'e' };
        char first = a[0];
        char last = a[4];
        Random rnd = new Random(12345);
        N.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleByte() {
        byte[] a = { 1, 2, 3, 4, 5 };
        byte[] original = a.clone();
        N.shuffle(a);
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
        N.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleByteWithRandom() {
        byte[] a = { 1, 2, 3, 4, 5 };
        Random rnd = new Random(12345);
        N.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleByteRangeWithRandom() {
        byte[] a = { 1, 2, 3, 4, 5 };
        byte first = a[0];
        byte last = a[4];
        Random rnd = new Random(12345);
        N.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleShort() {
        short[] a = { 1, 2, 3, 4, 5 };
        short[] original = a.clone();
        N.shuffle(a);
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
        N.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleShortWithRandom() {
        short[] a = { 1, 2, 3, 4, 5 };
        Random rnd = new Random(12345);
        N.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleShortRangeWithRandom() {
        short[] a = { 1, 2, 3, 4, 5 };
        short first = a[0];
        short last = a[4];
        Random rnd = new Random(12345);
        N.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleInt() {
        int[] a = { 1, 2, 3, 4, 5 };
        int[] original = a.clone();
        N.shuffle(a);
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
        N.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleIntWithRandom() {
        int[] a = { 1, 2, 3, 4, 5 };
        Random rnd = new Random(12345);
        N.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleIntRangeWithRandom() {
        int[] a = { 1, 2, 3, 4, 5 };
        int first = a[0];
        int last = a[4];
        Random rnd = new Random(12345);
        N.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleLong() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        long[] original = a.clone();
        N.shuffle(a);
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
        N.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleLongWithRandom() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        Random rnd = new Random(12345);
        N.shuffle(a, rnd);
        Assertions.assertEquals(5, a.length);
    }

    @Test
    public void testShuffleLongRangeWithRandom() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        long first = a[0];
        long last = a[4];
        Random rnd = new Random(12345);
        N.shuffle(a, 1, 4, rnd);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);
    }

    @Test
    public void testShuffleFloat() {
        float[] a = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] original = a.clone();
        N.shuffle(a);
        Assertions.assertEquals(original.length, a.length);
        Arrays.sort(original);
        Arrays.sort(a);
        Assertions.assertArrayEquals(original, a, 0.0f);
    }

    @Test
    public void testShuffleFloatArrayEmpty() {
        float[] arr = {};
        N.shuffle(arr, 0, 0, new Random());
        Assertions.assertEquals(0, arr.length);
    }

    @Test
    public void testShuffleFloatArrayNull() {
        float[] arr = null;
        N.shuffle(arr, new Random());
        Assertions.assertNull(arr);
    }

    @Test
    public void testShuffleListEmpty() {
        List<String> list = new ArrayList<>();
        N.shuffle(list);
        Assertions.assertTrue(list.isEmpty());
    }

    @Test
    public void testShuffleListSingleElement() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        N.shuffle(list);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("a", list.get(0));
    }

    @Test
    public void testShuffleCollectionEmpty() {
        Collection<String> coll = new ArrayList<>();
        N.shuffle(coll);
        Assertions.assertTrue(coll.isEmpty());
    }

    @Test
    public void testShuffleBoolean() {
        boolean[] a = { true, false, true, false, true };
        boolean[] original = a.clone();
        N.shuffle(a);
        Assertions.assertEquals(original.length, a.length);

        boolean[] empty = {};
        N.shuffle(empty);
        Assertions.assertArrayEquals(new boolean[0], empty);

        N.shuffle((boolean[]) null);

        boolean[] single = { true };
        N.shuffle(single);
        Assertions.assertArrayEquals(new boolean[] { true }, single);
    }

    @Test
    public void testShuffleBooleanRange() {
        boolean[] a = { true, false, true, false, true };
        boolean first = a[0];
        boolean last = a[4];
        N.shuffle(a, 1, 4);
        Assertions.assertEquals(first, a[0]);
        Assertions.assertEquals(last, a[4]);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.shuffle(a, -1, 3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.shuffle(a, 0, 10));
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
    public void testSwapBooleanArray() {
        boolean[] arr = { true, false, true, false };
        N.swap(arr, 0, 3);
        Assertions.assertFalse(arr[0]);
        Assertions.assertTrue(arr[3]);
    }

    @Test
    public void testSwapCharArray() {
        char[] arr = { 'a', 'b', 'c', 'd' };
        N.swap(arr, 1, 2);
        Assertions.assertEquals('c', arr[1]);
        Assertions.assertEquals('b', arr[2]);
    }

    @Test
    public void testSwapByteArray() {
        byte[] arr = { 1, 2, 3, 4 };
        N.swap(arr, 0, 3);
        Assertions.assertEquals(4, arr[0]);
        Assertions.assertEquals(1, arr[3]);
    }

    @Test
    public void testSwapShortArray() {
        short[] arr = { 10, 20, 30, 40 };
        N.swap(arr, 1, 3);
        Assertions.assertEquals(40, arr[1]);
        Assertions.assertEquals(20, arr[3]);
    }

    @Test
    public void testSwapIntArray() {
        int[] arr = { 100, 200, 300, 400 };
        N.swap(arr, 0, 2);
        Assertions.assertEquals(300, arr[0]);
        Assertions.assertEquals(100, arr[2]);
    }

    @Test
    public void testSwapLongArray() {
        long[] arr = { 1000L, 2000L, 3000L, 4000L };
        N.swap(arr, 1, 2);
        Assertions.assertEquals(3000L, arr[1]);
        Assertions.assertEquals(2000L, arr[2]);
    }

    @Test
    public void testSwapFloatArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f };
        N.swap(arr, 0, 3);
        Assertions.assertEquals(4.0f, arr[0]);
        Assertions.assertEquals(1.0f, arr[3]);
    }

    @Test
    public void testSwapDoubleArray() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0 };
        N.swap(arr, 1, 3);
        Assertions.assertEquals(4.0, arr[1]);
        Assertions.assertEquals(2.0, arr[3]);
    }

    @Test
    public void testSwapObjectArray() {
        String[] arr = { "a", "b", "c", "d" };
        N.swap(arr, 0, 2);
        Assertions.assertEquals("c", arr[0]);
        Assertions.assertEquals("a", arr[2]);
    }

    @Test
    public void testSwapList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        N.swap(list, 1, 3);
        Assertions.assertEquals("d", list.get(1));
        Assertions.assertEquals("b", list.get(3));
    }

    @Test
    public void testSwapPair() {
        Pair<String, String> pair = Pair.of("left", "right");
        N.swap(pair);
        Assertions.assertEquals("right", pair.left());
        Assertions.assertEquals("left", pair.right());
    }

    @Test
    public void testSwapTriple() {
        Triple<String, Integer, String> triple = Triple.of("left", 42, "right");
        N.swap(triple);
        Assertions.assertEquals("right", triple.left());
        Assertions.assertEquals("left", triple.right());
        Assertions.assertEquals(42, triple.middle());
    }

    @Test
    public void testSwapIf() {
        com.landawn.abacus.util.Pair<Integer, Integer> pair = com.landawn.abacus.util.Pair.of(5, 2);
        N.swapIf(pair, p -> p.getLeft() > p.getRight());
        assertEquals(Integer.valueOf(2), pair.getLeft());
        assertEquals(Integer.valueOf(5), pair.getRight());
    }

    @Test
    public void testSwapIfPair() {
        Pair<Integer, Integer> pair = Pair.of(1, 2);
        boolean swapped = N.swapIf(pair, p -> p.left() < p.right());
        Assertions.assertTrue(swapped);
        Assertions.assertEquals(2, pair.left());
        Assertions.assertEquals(1, pair.right());

        swapped = N.swapIf(pair, p -> p.left() < p.right());
        Assertions.assertFalse(swapped);
    }

    @Test
    public void testSwapIfTriple() {
        Triple<Integer, String, Integer> triple = Triple.of(1, "middle", 2);
        boolean swapped = N.swapIf(triple, t -> t.left() < t.right());
        Assertions.assertTrue(swapped);
        Assertions.assertEquals(2, triple.left());
        Assertions.assertEquals(1, triple.right());

        swapped = N.swapIf(triple, t -> t.left() < t.right());
        Assertions.assertFalse(swapped);
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
    public void testFillBooleanArray() {
        boolean[] arr = new boolean[5];
        N.fill(arr, true);
        for (boolean b : arr) {
            Assertions.assertTrue(b);
        }
    }

    @Test
    public void testFillBooleanArrayWithRange() {
        boolean[] arr = new boolean[5];
        N.fill(arr, 1, 4, true);
        Assertions.assertFalse(arr[0]);
        Assertions.assertTrue(arr[1]);
        Assertions.assertTrue(arr[2]);
        Assertions.assertTrue(arr[3]);
        Assertions.assertFalse(arr[4]);
    }

    @Test
    public void testFillCharArray() {
        char[] arr = new char[5];
        N.fill(arr, 'x');
        for (char c : arr) {
            Assertions.assertEquals('x', c);
        }
    }

    @Test
    public void testFillCharArrayWithRange() {
        char[] arr = new char[5];
        N.fill(arr, 1, 4, 'x');
        Assertions.assertEquals('\0', arr[0]);
        Assertions.assertEquals('x', arr[1]);
        Assertions.assertEquals('x', arr[2]);
        Assertions.assertEquals('x', arr[3]);
        Assertions.assertEquals('\0', arr[4]);
    }

    @Test
    public void testFillByteArray() {
        byte[] arr = new byte[5];
        N.fill(arr, (byte) 42);
        for (byte b : arr) {
            Assertions.assertEquals(42, b);
        }
    }

    @Test
    public void testFillByteArrayWithRange() {
        byte[] arr = new byte[5];
        N.fill(arr, 2, 5, (byte) 42);
        Assertions.assertEquals(0, arr[0]);
        Assertions.assertEquals(0, arr[1]);
        Assertions.assertEquals(42, arr[2]);
        Assertions.assertEquals(42, arr[3]);
        Assertions.assertEquals(42, arr[4]);
    }

    @Test
    public void testFillShortArray() {
        short[] arr = new short[5];
        N.fill(arr, (short) 100);
        for (short s : arr) {
            Assertions.assertEquals(100, s);
        }
    }

    @Test
    public void testFillShortArrayWithRange() {
        short[] arr = new short[5];
        N.fill(arr, 0, 3, (short) 100);
        Assertions.assertEquals(100, arr[0]);
        Assertions.assertEquals(100, arr[1]);
        Assertions.assertEquals(100, arr[2]);
        Assertions.assertEquals(0, arr[3]);
        Assertions.assertEquals(0, arr[4]);
    }

    @Test
    public void testFillIntArray() {
        int[] arr = new int[5];
        N.fill(arr, 999);
        for (int i : arr) {
            Assertions.assertEquals(999, i);
        }
    }

    @Test
    public void testFillIntArrayWithRange() {
        int[] arr = new int[5];
        N.fill(arr, 1, 3, 999);
        Assertions.assertEquals(0, arr[0]);
        Assertions.assertEquals(999, arr[1]);
        Assertions.assertEquals(999, arr[2]);
        Assertions.assertEquals(0, arr[3]);
        Assertions.assertEquals(0, arr[4]);
    }

    @Test
    public void testFillLongArray() {
        long[] arr = new long[5];
        N.fill(arr, 1000L);
        for (long l : arr) {
            Assertions.assertEquals(1000L, l);
        }
    }

    @Test
    public void testFillLongArrayWithRange() {
        long[] arr = new long[5];
        N.fill(arr, 2, 4, 1000L);
        Assertions.assertEquals(0L, arr[0]);
        Assertions.assertEquals(0L, arr[1]);
        Assertions.assertEquals(1000L, arr[2]);
        Assertions.assertEquals(1000L, arr[3]);
        Assertions.assertEquals(0L, arr[4]);
    }

    @Test
    public void testFillFloatArray() {
        float[] arr = new float[5];
        N.fill(arr, 3.14f);
        for (float f : arr) {
            Assertions.assertEquals(3.14f, f);
        }
    }

    @Test
    public void testFillFloatArrayWithRange() {
        float[] arr = new float[5];
        N.fill(arr, 0, 2, 3.14f);
        Assertions.assertEquals(3.14f, arr[0]);
        Assertions.assertEquals(3.14f, arr[1]);
        Assertions.assertEquals(0.0f, arr[2]);
        Assertions.assertEquals(0.0f, arr[3]);
        Assertions.assertEquals(0.0f, arr[4]);
    }

    @Test
    public void testFillDoubleArray() {
        double[] arr = new double[5];
        N.fill(arr, 2.718);
        for (double d : arr) {
            Assertions.assertEquals(2.718, d);
        }
    }

    @Test
    public void testFillDoubleArrayWithRange() {
        double[] arr = new double[5];
        N.fill(arr, 3, 5, 2.718);
        Assertions.assertEquals(0.0, arr[0]);
        Assertions.assertEquals(0.0, arr[1]);
        Assertions.assertEquals(0.0, arr[2]);
        Assertions.assertEquals(2.718, arr[3]);
        Assertions.assertEquals(2.718, arr[4]);
    }

    @Test
    public void testFillList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        N.fill(list, "x");
        for (String s : list) {
            Assertions.assertEquals("x", s);
        }
    }

    @Test
    public void testFillListWithRange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        N.fill(list, 1, 4, "x");
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertEquals("x", list.get(1));
        Assertions.assertEquals("x", list.get(2));
        Assertions.assertEquals("x", list.get(3));
        Assertions.assertEquals("e", list.get(4));
    }

    @Test
    public void testFillListExtending() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        N.fill(list, 0, 5, "x");
        Assertions.assertEquals(5, list.size());
        for (String s : list) {
            Assertions.assertEquals("x", s);
        }
    }

    @Test
    public void testFillBooleanArrayEmpty() {
        boolean[] arr = new boolean[0];
        N.fill(arr, true);
        Assertions.assertEquals(0, arr.length);
    }

    @Test
    public void testFillObjectArrayWithRange() {
        String[] arr = new String[5];
        N.fill(arr, 1, 4, "test");
        Assertions.assertNull(arr[0]);
        Assertions.assertEquals("test", arr[1]);
        Assertions.assertEquals("test", arr[2]);
        Assertions.assertEquals("test", arr[3]);
        Assertions.assertNull(arr[4]);
    }

    @Test
    public void testFillObjectArrayEmpty() {
        String[] arr = new String[0];
        N.fill(arr, "test");
        Assertions.assertEquals(0, arr.length);
    }

    @Test
    public void testFillListWithGaps() {
        List<String> list = new ArrayList<>();
        N.fill(list, 2, 5, "x");
        Assertions.assertEquals(5, list.size());
        Assertions.assertNull(list.get(0));
        Assertions.assertNull(list.get(1));
        Assertions.assertEquals("x", list.get(2));
        Assertions.assertEquals("x", list.get(3));
        Assertions.assertEquals("x", list.get(4));
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
    public void testPadLeftNoChange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        boolean result = N.padLeft(list, 3, "x");
        Assertions.assertFalse(result);
        Assertions.assertEquals(3, list.size());
    }

    @Test
    public void testPadLeftWithNull() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        boolean result = N.padLeft(list, 3, null);
        Assertions.assertTrue(result);
        Assertions.assertEquals(3, list.size());
        Assertions.assertNull(list.get(0));
        Assertions.assertNull(list.get(1));
        Assertions.assertEquals("a", list.get(2));
    }

    @Test
    public void testPadRight() {
        Collection<String> col = new ArrayList<>(Arrays.asList("a", "b", "c"));
        N.padRight(col, 5, "x");
        assertEquals(5, col.size());
    }

    @Test
    public void testPadRightNoChange() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a", "b", "c"));
        boolean result = N.padRight(coll, 2, "x");
        Assertions.assertFalse(result);
        Assertions.assertEquals(3, coll.size());
    }

    @Test
    public void testPadRightWithNull() {
        Collection<String> coll = new ArrayList<>(Arrays.asList("a"));
        boolean result = N.padRight(coll, 3, null);
        Assertions.assertTrue(result);
        Assertions.assertEquals(3, coll.size());
        List<String> list = new ArrayList<>(coll);
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertNull(list.get(1));
        Assertions.assertNull(list.get(2));
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
    public void testRepeatCollection() {
        List<String> list = Arrays.asList("a", "b");
        List<String> repeated = N.cycle(list, 3);
        assertEquals(6, repeated.size());
        assertEquals("a", repeated.get(0));
        assertEquals("b", repeated.get(1));
        assertEquals("a", repeated.get(2));
    }

    @Test
    public void testRepeatCollectionToSize() {
        List<String> list = Arrays.asList("a", "b");
        List<String> repeated = N.cycleToSize(list, 5);
        assertEquals(5, repeated.size());
        assertEquals("a", repeated.get(0));
        assertEquals("b", repeated.get(1));
        assertEquals("a", repeated.get(2));
    }

    @Test
    public void testRepeat() {
        List<String> result = N.repeat("x", 5);
        Assertions.assertEquals(5, result.size());
        for (String s : result) {
            Assertions.assertEquals("x", s);
        }
    }

    @Test
    public void testRepeatCollectionToSizeExact() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = N.cycleToSize(input, 6);
        Assertions.assertEquals(6, result.size());
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "a", "b", "c"), result);
    }

    @Test
    public void testRepeatZero() {
        List<String> result = N.repeat("x", 0);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testRepeatNull() {
        List<String> result = N.repeat(null, 3);
        Assertions.assertEquals(3, result.size());
        for (String s : result) {
            Assertions.assertNull(s);
        }
    }

    @Test
    public void testRepeatCollectionZero() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = N.cycle(input, 0);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testRepeatCollectionEmpty() {
        List<String> input = new ArrayList<>();
        List<String> result = N.cycle(input, 3);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testRepeatCollectionToSizeZero() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = N.cycleToSize(input, 0);
        Assertions.assertTrue(result.isEmpty());
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
    public void testRepeatElementsZero() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = N.repeatElements(input, 0);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testRepeatElementsEmpty() {
        List<String> input = new ArrayList<>();
        List<String> result = N.repeatElements(input, 3);
        Assertions.assertTrue(result.isEmpty());
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
    public void testRepeatElementsToSizeExact() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = N.repeatElementsToSize(input, 6);
        Assertions.assertEquals(6, result.size());
        Assertions.assertEquals(Arrays.asList("a", "a", "b", "b", "c", "c"), result);
    }

    // Tests for repeatElementsToSize with more complex collections
    @Test
    public void testRepeatElementsToSize_ThreeElements() {
        // [1, 1, 2, 2, 3] - 3 elements, repeat to size 5
        List<Integer> result = N.repeatElementsToSize(Arrays.asList(1, 2, 3), 5);
        assertEquals(5, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
        assertEquals(Integer.valueOf(1), result.get(1));
        assertEquals(Integer.valueOf(2), result.get(2));
        assertEquals(Integer.valueOf(2), result.get(3));
        assertEquals(Integer.valueOf(3), result.get(4));
    }

    @Test
    public void testRepeatElementsToSizeZero() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> result = N.repeatElementsToSize(input, 0);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testRepeatElementsToSize_ExactMultiple() {
        List<Integer> result = N.repeatElementsToSize(Arrays.asList(1, 2), 6);
        assertEquals(6, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
        assertEquals(Integer.valueOf(1), result.get(1));
        assertEquals(Integer.valueOf(1), result.get(2));
        assertEquals(Integer.valueOf(2), result.get(3));
    }

    @Test
    public void testRepeatElementsToSize_SizeZero() {
        List<Integer> result = N.repeatElementsToSize(Arrays.asList(1, 2, 3), 0);
        assertEquals(0, result.size());
    }

    // Tests for cycleToSize with truncation
    @Test
    public void testCycleToSize_WithTruncation() {
        // [1, 2, 3, 1, 2] - truncated cycle
        List<Integer> result = N.cycleToSize(Arrays.asList(1, 2, 3), 5);
        assertEquals(5, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
        assertEquals(Integer.valueOf(2), result.get(1));
        assertEquals(Integer.valueOf(3), result.get(2));
        assertEquals(Integer.valueOf(1), result.get(3));
        assertEquals(Integer.valueOf(2), result.get(4));
    }

    @Test
    public void testCycleToSize_ExactMultiple() {
        List<Integer> result = N.cycleToSize(Arrays.asList(1, 2), 6);
        assertEquals(6, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
        assertEquals(Integer.valueOf(2), result.get(1));
        assertEquals(Integer.valueOf(1), result.get(2));
    }

    @Test
    public void testCycleToSize_SizeZero() {
        List<Integer> result = N.cycleToSize(Arrays.asList(1, 2), 0);
        assertEquals(0, result.size());
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
    public void testCopyBooleanArray() {
        boolean[] src = { true, false, true, false, true };
        boolean[] dest = new boolean[5];
        N.copy(src, 1, dest, 2, 2);
        Assertions.assertFalse(dest[0]);
        Assertions.assertFalse(dest[1]);
        Assertions.assertFalse(dest[2]);
        Assertions.assertTrue(dest[3]);
        Assertions.assertFalse(dest[4]);
    }

    @Test
    public void testCopyCharArray() {
        char[] src = { 'a', 'b', 'c', 'd', 'e' };
        char[] dest = new char[5];
        N.copy(src, 1, dest, 0, 3);
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
        N.copy(src, 2, dest, 1, 3);
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
        N.copy(src, 0, dest, 2, 2);
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
        N.copy(src, 1, dest, 1, 3);
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
        N.copy(src, 2, dest, 0, 3);
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
        N.copy(src, 0, dest, 0, 5);
        Assertions.assertArrayEquals(src, dest);
    }

    @Test
    public void testCopyDoubleArray() {
        double[] src = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] dest = new double[5];
        N.copy(src, 3, dest, 3, 2);
        Assertions.assertEquals(0.0, dest[0]);
        Assertions.assertEquals(0.0, dest[1]);
        Assertions.assertEquals(0.0, dest[2]);
        Assertions.assertEquals(4.0, dest[3]);
        Assertions.assertEquals(5.0, dest[4]);
    }

    @Test
    public void testCopyGenericObject() {
        int[] src = { 1, 2, 3, 4, 5 };
        int[] dest = new int[5];
        N.copy((Object) src, 0, (Object) dest, 1, 4);
        Assertions.assertEquals(0, dest[0]);
        Assertions.assertEquals(1, dest[1]);
        Assertions.assertEquals(2, dest[2]);
        Assertions.assertEquals(3, dest[3]);
        Assertions.assertEquals(4, dest[4]);
    }

    // Tests for copy with range (boolean, byte, char, short, int, long, float, double, Object)
    @Test
    public void testCopy_BooleanArray_WithRange() {
        boolean[] src = { true, false, true, false, true };
        boolean[] dest = new boolean[5];
        N.copy(src, 1, dest, 1, 3);
        assertArrayEquals(new boolean[] { false, false, true, false, false }, dest);
    }

    @Test
    public void testCopy_ByteArray_WithRange() {
        byte[] src = { 1, 2, 3, 4, 5 };
        byte[] dest = new byte[5];
        N.copy(src, 1, dest, 1, 3);
        assertArrayEquals(new byte[] { 0, 2, 3, 4, 0 }, dest);
    }

    @Test
    public void testCopy_CharArray_WithRange() {
        char[] src = { 'a', 'b', 'c', 'd', 'e' };
        char[] dest = new char[5];
        N.copy(src, 1, dest, 1, 3);
        assertArrayEquals(new char[] { '\0', 'b', 'c', 'd', '\0' }, dest);
    }

    @Test
    public void testCopy_ShortArray_WithRange() {
        short[] src = { 1, 2, 3, 4, 5 };
        short[] dest = new short[5];
        N.copy(src, 1, dest, 1, 3);
        assertArrayEquals(new short[] { 0, 2, 3, 4, 0 }, dest);
    }

    @Test
    public void testCopy_IntArray_WithRange() {
        int[] src = { 1, 2, 3, 4, 5 };
        int[] dest = new int[5];
        N.copy(src, 1, dest, 1, 3);
        assertArrayEquals(new int[] { 0, 2, 3, 4, 0 }, dest);
    }

    @Test
    public void testCopy_LongArray_WithRange() {
        long[] src = { 1L, 2L, 3L, 4L, 5L };
        long[] dest = new long[5];
        N.copy(src, 1, dest, 1, 3);
        assertArrayEquals(new long[] { 0L, 2L, 3L, 4L, 0L }, dest);
    }

    @Test
    public void testCopy_FloatArray_WithRange() {
        float[] src = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] dest = new float[5];
        N.copy(src, 1, dest, 1, 3);
        assertArrayEquals(new float[] { 0.0f, 2.0f, 3.0f, 4.0f, 0.0f }, dest, 0.0f);
    }

    @Test
    public void testCopy_DoubleArray_WithRange() {
        double[] src = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] dest = new double[5];
        N.copy(src, 1, dest, 1, 3);
        assertArrayEquals(new double[] { 0.0, 2.0, 3.0, 4.0, 0.0 }, dest, 0.0);
    }

    @Test
    public void testCopyBooleanArraySameArray() {
        boolean[] arr = { true, false, true, false, true };
        N.copy(arr, 0, arr, 2, 3);
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
        N.copy(src, 0, dest, 0, 0);
        assertNotNull(dest);
    }

    @Test
    public void testCopyObjectArray() {
        String[] src = { "a", "b", "c", "d", "e" };
        String[] dest = new String[5];
        N.copy(src, 1, dest, 2, 2);
        Assertions.assertNull(dest[0]);
        Assertions.assertNull(dest[1]);
        Assertions.assertEquals("b", dest[2]);
        Assertions.assertEquals("c", dest[3]);
        Assertions.assertNull(dest[4]);
    }

    @Test
    public void testCopy_ObjectArray_WithRange() {
        String[] src = { "a", "b", "c", "d", "e" };
        String[] dest = new String[5];
        N.copy(src, 1, dest, 1, 3);
        assertArrayEquals(new String[] { null, "b", "c", "d", null }, dest);
    }

    @Test
    public void testCopyOfBooleanArray() {
        boolean[] original = { true, false, true };
        boolean[] copy = N.copyOf(original, 5);
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
        boolean[] copy = N.copyOf(original, 3);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertTrue(copy[0]);
        Assertions.assertFalse(copy[1]);
        Assertions.assertTrue(copy[2]);
    }

    @Test
    public void testCopyOfCharArray() {
        char[] original = { 'a', 'b', 'c' };
        char[] copy = N.copyOf(original, 5);
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
        byte[] copy = N.copyOf(original, 2);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(1, copy[0]);
        Assertions.assertEquals(2, copy[1]);
    }

    @Test
    public void testCopyOfShortArray() {
        short[] original = { 10, 20, 30 };
        short[] copy = N.copyOf(original, 4);
        Assertions.assertEquals(4, copy.length);
        Assertions.assertEquals(10, copy[0]);
        Assertions.assertEquals(20, copy[1]);
        Assertions.assertEquals(30, copy[2]);
        Assertions.assertEquals(0, copy[3]);
    }

    @Test
    public void testCopyOfLongArray() {
        long[] original = { 1000L, 2000L, 3000L };
        long[] copy = N.copyOf(original, 5);
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
        float[] copy = N.copyOf(original, 2);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(1.0f, copy[0]);
        Assertions.assertEquals(2.0f, copy[1]);
    }

    @Test
    public void testCopyOfDoubleArray() {
        double[] original = { 1.0, 2.0, 3.0 };
        double[] copy = N.copyOf(original, 4);
        Assertions.assertEquals(4, copy.length);
        Assertions.assertEquals(1.0, copy[0]);
        Assertions.assertEquals(2.0, copy[1]);
        Assertions.assertEquals(3.0, copy[2]);
        Assertions.assertEquals(0.0, copy[3]);
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
    public void testCopyOfBooleanArraySameLength() {
        boolean[] original = { true, false, true };
        boolean[] copy = N.copyOf(original, 3);
        Assertions.assertArrayEquals(original, copy);
        Assertions.assertNotSame(original, copy);
    }

    @Test
    public void testCopyOfIntArray() {
        int[] original = { 100, 200, 300 };
        int[] copy = N.copyOf(original, 3);
        Assertions.assertArrayEquals(original, copy);
        Assertions.assertNotSame(original, copy);
    }

    @Test
    public void testCopyOfObjectArray() {
        String[] original = { "a", "b", "c" };
        String[] copy = N.copyOf(original, 5);
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
        Integer[] copy = N.copyOf(original, 4, Integer[].class);
        Assertions.assertEquals(4, copy.length);
        Assertions.assertEquals(1, copy[0]);
        Assertions.assertEquals(2, copy[1]);
        Assertions.assertEquals(3, copy[2]);
        Assertions.assertNull(copy[3]);
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
    public void testCopyOfRangeBooleanArray() {
        boolean[] original = { true, false, true, false, true };
        boolean[] copy = N.copyOfRange(original, 1, 4);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertFalse(copy[0]);
        Assertions.assertTrue(copy[1]);
        Assertions.assertFalse(copy[2]);
    }

    @Test
    public void testCopyOfRangeBooleanArrayWithStep() {
        boolean[] original = { true, false, true, false, true, false };
        boolean[] copy = N.copyOfRange(original, 0, 6, 2);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertTrue(copy[0]);
        Assertions.assertTrue(copy[1]);
        Assertions.assertTrue(copy[2]);
    }

    @Test
    public void testCopyOfRangeCharArray() {
        char[] original = { 'a', 'b', 'c', 'd', 'e' };
        char[] copy = N.copyOfRange(original, 1, 4);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals('b', copy[0]);
        Assertions.assertEquals('c', copy[1]);
        Assertions.assertEquals('d', copy[2]);
    }

    @Test
    public void testCopyOfRangeCharArrayWithStep() {
        char[] original = { 'a', 'b', 'c', 'd', 'e', 'f' };
        char[] copy = N.copyOfRange(original, 1, 6, 2);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals('b', copy[0]);
        Assertions.assertEquals('d', copy[1]);
        Assertions.assertEquals('f', copy[2]);
    }

    @Test
    public void testCopyOfRangeByteArray() {
        byte[] original = { 1, 2, 3, 4, 5 };
        byte[] copy = N.copyOfRange(original, 2, 5);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals(3, copy[0]);
        Assertions.assertEquals(4, copy[1]);
        Assertions.assertEquals(5, copy[2]);
    }

    @Test
    public void testCopyOfRangeByteArrayWithStep() {
        byte[] original = { 1, 2, 3, 4, 5, 6 };
        byte[] copy = N.copyOfRange(original, 0, 5, 2);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals(1, copy[0]);
        Assertions.assertEquals(3, copy[1]);
        Assertions.assertEquals(5, copy[2]);
    }

    @Test
    public void testCopyOfRangeShortArray() {
        short[] original = { 10, 20, 30, 40, 50 };
        short[] copy = N.copyOfRange(original, 0, 3);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals(10, copy[0]);
        Assertions.assertEquals(20, copy[1]);
        Assertions.assertEquals(30, copy[2]);
    }

    @Test
    public void testCopyOfRangeShortArrayWithStep() {
        short[] original = { 10, 20, 30, 40, 50, 60 };
        short[] copy = N.copyOfRange(original, 1, 5, 2);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(20, copy[0]);
        Assertions.assertEquals(40, copy[1]);
    }

    @Test
    public void testCopyOfRangeIntArray() {
        int[] original = { 100, 200, 300, 400, 500 };
        int[] copy = N.copyOfRange(original, 1, 3);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(200, copy[0]);
        Assertions.assertEquals(300, copy[1]);
    }

    @Test
    public void testCopyOfRangeIntArrayWithStep() {
        int[] original = { 100, 200, 300, 400, 500, 600 };
        int[] copy = N.copyOfRange(original, 0, 6, 3);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(100, copy[0]);
        Assertions.assertEquals(400, copy[1]);
    }

    @Test
    public void testCopyOfRangeLongArray() {
        long[] original = { 1000L, 2000L, 3000L, 4000L, 5000L };
        long[] copy = N.copyOfRange(original, 2, 4);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(3000L, copy[0]);
        Assertions.assertEquals(4000L, copy[1]);
    }

    @Test
    public void testCopyOfRangeLongArrayWithStep() {
        long[] original = { 1000L, 2000L, 3000L, 4000L, 5000L };
        long[] copy = N.copyOfRange(original, 4, 0, -2);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(5000L, copy[0]);
        Assertions.assertEquals(3000L, copy[1]);
    }

    @Test
    public void testCopyOfRangeFloatArray() {
        float[] original = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] copy = N.copyOfRange(original, 3, 5);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(4.0f, copy[0]);
        Assertions.assertEquals(5.0f, copy[1]);
    }

    @Test
    public void testCopyOfRangeFloatArrayWithStep() {
        float[] original = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f };
        float[] copy = N.copyOfRange(original, 0, 6, 2);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals(1.0f, copy[0]);
        Assertions.assertEquals(3.0f, copy[1]);
        Assertions.assertEquals(5.0f, copy[2]);
    }

    @Test
    public void testCopyOfRangeDoubleArray() {
        double[] original = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] copy = N.copyOfRange(original, 0, 2);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(1.0, copy[0]);
        Assertions.assertEquals(2.0, copy[1]);
    }

    @Test
    public void testCopyOfRangeDoubleArrayWithStep() {
        double[] original = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] copy = N.copyOfRange(original, 1, 5, 2);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(2.0, copy[0]);
        Assertions.assertEquals(4.0, copy[1]);
    }

    @Test
    public void testCopyOfRangeObjectArray() {
        String[] original = { "a", "b", "c", "d", "e" };
        String[] copy = N.copyOfRange(original, 1, 4);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals("b", copy[0]);
        Assertions.assertEquals("c", copy[1]);
        Assertions.assertEquals("d", copy[2]);
    }

    @Test
    public void testCopyOfRangeObjectArrayWithStep() {
        String[] original = { "a", "b", "c", "d", "e", "f" };
        String[] copy = N.copyOfRange(original, 0, 6, 2);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals("a", copy[0]);
        Assertions.assertEquals("c", copy[1]);
        Assertions.assertEquals("e", copy[2]);
    }

    @Test
    public void testCopyOfRangeObjectArrayWithType() {
        Number[] original = { 1, 2, 3, 4, 5 };
        Integer[] copy = N.copyOfRange(original, 1, 4, Integer[].class);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertEquals(2, copy[0]);
        Assertions.assertEquals(3, copy[1]);
        Assertions.assertEquals(4, copy[2]);
    }

    @Test
    public void testCopyOfRangeObjectArrayWithTypeAndStep() {
        Number[] original = { 1, 2, 3, 4, 5, 6 };
        Integer[] copy = N.copyOfRange(original, 0, 6, 3, Integer[].class);
        Assertions.assertEquals(2, copy.length);
        Assertions.assertEquals(1, copy[0]);
        Assertions.assertEquals(4, copy[1]);
    }

    @Test
    public void testCopyOfRangeList() {
        List<String> original = Arrays.asList("a", "b", "c", "d", "e");
        List<String> copy = N.copyOfRange(original, 1, 4);
        Assertions.assertEquals(3, copy.size());
        Assertions.assertEquals("b", copy.get(0));
        Assertions.assertEquals("c", copy.get(1));
        Assertions.assertEquals("d", copy.get(2));
    }

    @Test
    public void testCopyOfRangeListWithStep() {
        List<String> original = Arrays.asList("a", "b", "c", "d", "e", "f");
        List<String> copy = N.copyOfRange(original, 0, 5, 2);
        Assertions.assertEquals(3, copy.size());
        Assertions.assertEquals("a", copy.get(0));
        Assertions.assertEquals("c", copy.get(1));
        Assertions.assertEquals("e", copy.get(2));
    }

    @Test
    public void testCopyOfRangeString() {
        String original = "abcde";
        String copy = N.copyOfRange(original, 1, 4);
        Assertions.assertEquals("bcd", copy);
    }

    @Test
    public void testCopyOfRangeStringFullRange() {
        String original = "abcde";
        String copy = N.copyOfRange(original, 0, 5);
        Assertions.assertEquals(original, copy);
    }

    @Test
    public void testCopyOfRangeStringWithStep() {
        String original = "abcdef";
        String copy = N.copyOfRange(original, 0, 6, 2);
        Assertions.assertEquals("ace", copy);
    }

    @Test
    @DisplayName("copyOfRange with step=1 on LinkedList for forward copy")
    public void test_copyOfRange_step1_linkedList() {
        LinkedList<Integer> list = new LinkedList<>(Arrays.asList(10, 20, 30, 40, 50));

        List<Integer> result = N.copyOfRange(list, 1, 4, 1);

        assertEquals(Arrays.asList(20, 30, 40), result);
    }

    // Tests for copyOfRange(List, fromIndex, toIndex, step) edge cases
    @Test
    public void testCopyOfRange_List_WithStep_StepTwo() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);
        List<Integer> result = N.copyOfRange(list, 0, 6, 2);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
        assertEquals(Integer.valueOf(3), result.get(1));
        assertEquals(Integer.valueOf(5), result.get(2));
    }

    @Test
    public void testCopyOfRange_List_WithStep_StepThree() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        List<Integer> result = N.copyOfRange(list, 0, 7, 3);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
        assertEquals(Integer.valueOf(4), result.get(1));
        assertEquals(Integer.valueOf(7), result.get(2));
    }

    @Test
    public void testCopyOfRange_List_WithStep_FromEqualsTo() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        List<Integer> result = N.copyOfRange(list, 2, 2, 1);
        assertEquals(0, result.size());
    }

    @Test
    public void testCopyOfRange_List_WithNonRandomAccess() {
        // LinkedList is not RandomAccess
        java.util.LinkedList<Integer> list = new java.util.LinkedList<>(Arrays.asList(1, 2, 3, 4, 5, 6));
        List<Integer> result = N.copyOfRange(list, 0, 6, 2);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
        assertEquals(Integer.valueOf(3), result.get(1));
        assertEquals(Integer.valueOf(5), result.get(2));
    }

    @Test
    public void testCopyOfRange_FloatArray_WithStep_Forward() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] result = N.copyOfRange(arr, 0, 5, 2);
        assertArrayEquals(new float[] { 1.0f, 3.0f, 5.0f }, result);
    }

    @Test
    public void testCopyOfRange_ObjectArray_WithType_Step() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String[] result = N.copyOfRange(arr, 0, 5, 2, String[].class);
        assertArrayEquals(new String[] { "a", "c", "e" }, result);
    }

    @Test
    public void testCopyOfRange_String_WithStep() {
        String result = N.copyOfRange("abcde", 0, 5, 2);
        assertEquals("ace", result);
    }

    @Test
    public void testCopyOfRangeBooleanArrayFullRange() {
        boolean[] original = { true, false, true };
        boolean[] copy = N.copyOfRange(original, 0, 3);
        Assertions.assertArrayEquals(original, copy);
        Assertions.assertNotSame(original, copy);
    }

    @Test
    public void testCopyOfRangeBooleanArrayWithNegativeStep() {
        boolean[] original = { true, false, true, false, true };
        boolean[] copy = N.copyOfRange(original, 4, 1, -1);
        Assertions.assertEquals(3, copy.length);
        Assertions.assertTrue(copy[0]);
        Assertions.assertFalse(copy[1]);
        Assertions.assertTrue(copy[2]);
    }

    @Test
    public void testCopyOfRangeBooleanArrayWithNegativeStepToBeginning() {
        boolean[] original = { true, false, true, false, true };
        boolean[] copy = N.copyOfRange(original, 4, -1, -1);
        Assertions.assertEquals(5, copy.length);
        Assertions.assertTrue(copy[0]);
        Assertions.assertFalse(copy[1]);
        Assertions.assertTrue(copy[2]);
        Assertions.assertFalse(copy[3]);
        Assertions.assertTrue(copy[4]);
    }

    @Test
    @DisplayName("copyOfRange with step=-2 on LinkedList")
    public void test_copyOfRange_negativeStep2_linkedList() {
        LinkedList<Integer> list = new LinkedList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

        List<Integer> result = N.copyOfRange(list, 9, -1, -2);

        assertEquals(Arrays.asList(9, 7, 5, 3, 1), result);
    }

    @Test
    @DisplayName("copyOfRange with step=-3 on LinkedList")
    public void test_copyOfRange_negativeStep3_linkedList() {
        LinkedList<Integer> list = new LinkedList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

        List<Integer> result = N.copyOfRange(list, 9, -1, -3);

        assertEquals(Arrays.asList(9, 6, 3, 0), result);
    }

    @Test
    @DisplayName("copyOfRange with positive step on LinkedList still works")
    public void test_copyOfRange_positiveStep_linkedList() {
        LinkedList<Integer> list = new LinkedList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

        List<Integer> result = N.copyOfRange(list, 0, 10, 2);

        assertEquals(Arrays.asList(0, 2, 4, 6, 8), result);
    }

    @Test
    @DisplayName("copyOfRange with negative step on ArrayList still works")
    public void test_copyOfRange_negativeStep_arrayList() {
        ArrayList<Integer> list = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

        List<Integer> result = N.copyOfRange(list, 9, -1, -1);

        assertEquals(Arrays.asList(9, 8, 7, 6, 5, 4, 3, 2, 1, 0), result);
    }

    @Test
    @DisplayName("copyOfRange results should be consistent between ArrayList and LinkedList")
    public void test_copyOfRange_negativeStep_consistentResults() {
        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        LinkedList<String> linkedList = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));

        List<String> resultArray = N.copyOfRange(arrayList, 4, -1, -1);
        List<String> resultLinked = N.copyOfRange(linkedList, 4, -1, -1);

        assertEquals(resultArray, resultLinked, "ArrayList and LinkedList should produce the same result");
        assertEquals(Arrays.asList("e", "d", "c", "b", "a"), resultArray);
    }

    @Test
    @DisplayName("copyOfRange step=-2 should be consistent between ArrayList and LinkedList")
    public void test_copyOfRange_negativeStep2_consistentResults() {
        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        LinkedList<String> linkedList = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));

        List<String> resultArray = N.copyOfRange(arrayList, 4, -1, -2);
        List<String> resultLinked = N.copyOfRange(linkedList, 4, -1, -2);

        assertEquals(resultArray, resultLinked, "ArrayList and LinkedList should produce the same result");
        assertEquals(Arrays.asList("e", "c", "a"), resultArray);
    }

    @Test
    @DisplayName("copyOfRange returns empty list when fromIndex equals toIndex")
    public void test_copyOfRange_emptyRange() {
        LinkedList<Integer> list = new LinkedList<>(Arrays.asList(1, 2, 3));

        List<Integer> result = N.copyOfRange(list, 2, 2, -1);

        assertTrue(result.isEmpty());
    }

    // ========== copyOfRange with step (byte[], float[], Object[], String) ==========

    @Test
    public void testCopyOfRange_ByteArray_WithStep_NegativeStep() {
        byte[] arr = { 1, 2, 3, 4, 5 };
        byte[] result = N.copyOfRange(arr, 4, 1, -1);
        assertArrayEquals(new byte[] { 5, 4, 3 }, result);
    }

    @Test
    public void testCopyOfRange_String_NegativeStep() {
        String result = N.copyOfRange("abcde", 4, 1, -1);
        assertEquals("edc", result);
    }

    @Test
    @DisplayName("copyOfRange with negative step on LinkedList should not throw")
    public void test_copyOfRange_negativeStep_linkedList() {
        // LinkedList is NOT RandomAccess
        LinkedList<Integer> list = new LinkedList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

        // Negative step: iterate from index 9 down to index 0 (exclusive), stepping by -1
        List<Integer> result = N.copyOfRange(list, 9, -1, -1);

        assertEquals(Arrays.asList(9, 8, 7, 6, 5, 4, 3, 2, 1, 0), result);
    }

    @Test
    public void testCopyOfRange_List_WithStep_ZeroStep_Throws() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> N.copyOfRange(list, 0, 3, 0));
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
    public void testCloneDouble3DArray() {
        double[][][] nullArray = null;
        assertNull(N.clone(nullArray));

        double[][][] emptyArray = new double[0][][];
        double[][][] clonedEmpty = N.clone(emptyArray);
        assertNotNull(clonedEmpty);
        assertEquals(0, clonedEmpty.length);

        double[][][] original = new double[][][] { { { 1.0, 2.0 }, { 3.0, 4.0 } }, { { 5.0, 6.0 }, { 7.0, 8.0 } } };

        double[][][] cloned = N.clone(original);
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
        assertNull(N.clone(nullArray));

        String[][][] emptyArray = new String[0][][];
        String[][][] clonedEmpty = N.clone(emptyArray);
        assertNotNull(clonedEmpty);
        assertEquals(0, clonedEmpty.length);

        String[][][] original = new String[][][] { { { "a", "b" }, { "c", "d" } }, { { "e", "f" }, { "g", "h" } } };

        String[][][] cloned = N.clone(original);
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
    public void testCloneBooleanArray() {
        boolean[] original = { true, false, true };
        boolean[] cloned = N.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    @Test
    public void testCloneBooleanArrayNull() {
        boolean[] original = null;
        boolean[] cloned = N.clone(original);
        Assertions.assertNull(cloned);
    }

    @Test
    public void testCloneCharArray() {
        char[] original = { 'a', 'b', 'c' };
        char[] cloned = N.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    @Test
    public void testCloneByteArray() {
        byte[] original = { 1, 2, 3 };
        byte[] cloned = N.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    @Test
    public void testCloneShortArray() {
        short[] original = { 10, 20, 30 };
        short[] cloned = N.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    @Test
    public void testCloneIntArray() {
        int[] original = { 100, 200, 300 };
        int[] cloned = N.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    @Test
    public void testCloneLongArray() {
        long[] original = { 1000L, 2000L, 3000L };
        long[] cloned = N.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    @Test
    public void testCloneFloatArray() {
        float[] original = { 1.0f, 2.0f, 3.0f };
        float[] cloned = N.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    @Test
    public void testCloneDoubleArray() {
        double[] original = { 1.0, 2.0, 3.0 };
        double[] cloned = N.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    @Test
    public void testCloneObjectArray() {
        String[] original = { "a", "b", "c" };
        String[] cloned = N.clone(original);
        Assertions.assertArrayEquals(original, cloned);
        Assertions.assertNotSame(original, cloned);
    }

    @Test
    public void testClone2DBoolean() {
        assertNull(N.clone((boolean[][]) null));

        final boolean[][] a = new boolean[][] { { true, false }, { false, true } };
        final boolean[][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = false;
        assertTrue(a[0][0]);

        final boolean[][] b = new boolean[][] { { true, false }, null };
        final boolean[][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final boolean[][] c = new boolean[0][0];
        final boolean[][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DChar() {
        assertNull(N.clone((char[][]) null));

        final char[][] a = new char[][] { { 'a', 'b' }, { 'c', 'd' } };
        final char[][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 'z';
        assertEquals('a', a[0][0]);

        final char[][] b = new char[][] { { 'a', 'b' }, null };
        final char[][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final char[][] c = new char[0][0];
        final char[][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DByte() {
        assertNull(N.clone((byte[][]) null));

        final byte[][] a = new byte[][] { { 1, 2 }, { 3, 4 } };
        final byte[][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 9;
        assertEquals(1, a[0][0]);

        final byte[][] b = new byte[][] { { 1, 2 }, null };
        final byte[][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final byte[][] c = new byte[0][0];
        final byte[][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DShort() {
        assertNull(N.clone((short[][]) null));

        final short[][] a = new short[][] { { 1, 2 }, { 3, 4 } };
        final short[][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 9;
        assertEquals(1, a[0][0]);

        final short[][] b = new short[][] { { 1, 2 }, null };
        final short[][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final short[][] c = new short[0][0];
        final short[][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DInt() {
        assertNull(N.clone((int[][]) null));

        final int[][] a = new int[][] { { 1, 2 }, { 3, 4 } };
        final int[][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 9;
        assertEquals(1, a[0][0]);

        final int[][] b = new int[][] { { 1, 2 }, null };
        final int[][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final int[][] c = new int[0][0];
        final int[][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DLong() {
        assertNull(N.clone((long[][]) null));

        final long[][] a = new long[][] { { 1L, 2L }, { 3L, 4L } };
        final long[][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 9L;
        assertEquals(1L, a[0][0]);

        final long[][] b = new long[][] { { 1L, 2L }, null };
        final long[][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final long[][] c = new long[0][0];
        final long[][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DFloat() {
        assertNull(N.clone((float[][]) null));

        final float[][] a = new float[][] { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
        final float[][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 9.0f;
        assertEquals(1.0f, a[0][0], 0.000001);

        final float[][] b = new float[][] { { 1.0f, 2.0f }, null };
        final float[][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final float[][] c = new float[0][0];
        final float[][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DDouble() {
        assertNull(N.clone((double[][]) null));

        final double[][] a = new double[][] { { 1.0, 2.0 }, { 3.0, 4.0 } };
        final double[][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = 9.0;
        assertEquals(1.0, a[0][0], 0.000001);

        final double[][] b = new double[][] { { 1.0, 2.0 }, null };
        final double[][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final double[][] c = new double[0][0];
        final double[][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone2DGeneric() {
        assertNull(N.clone((String[][]) null));

        final String[][] a = new String[][] { { "a", "b" }, { "c", "d" } };
        final String[][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertArrayEquals(a, cloneA);
        assertNotSame(a[0], cloneA[0]);

        cloneA[0][0] = "z";
        assertEquals("a", a[0][0]);

        final String[][] b = new String[][] { { "a", "b" }, null };
        final String[][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertArrayEquals(b, cloneB);

        final String[][] c = new String[0][0];
        final String[][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertArrayEquals(c, cloneC);
    }

    @Test
    public void testClone3DBoolean() {
        assertNull(N.clone((boolean[][][]) null));

        final boolean[][][] a = new boolean[][][] { { { true, false } }, { { false, true } } };
        final boolean[][][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = false;
        assertTrue(a[0][0][0]);

        final boolean[][][] b = new boolean[][][] { { { true, false } }, null };
        final boolean[][][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final boolean[][][] c = new boolean[0][0][0];
        final boolean[][][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DChar() {
        assertNull(N.clone((char[][][]) null));

        final char[][][] a = new char[][][] { { { 'a', 'b' } }, { { 'c', 'd' } } };
        final char[][][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 'z';
        assertEquals('a', a[0][0][0]);

        final char[][][] b = new char[][][] { { { 'a', 'b' } }, null };
        final char[][][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final char[][][] c = new char[0][0][0];
        final char[][][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DByte() {
        assertNull(N.clone((byte[][][]) null));

        final byte[][][] a = new byte[][][] { { { 1, 2 } }, { { 3, 4 } } };
        final byte[][][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 9;
        assertEquals(1, a[0][0][0]);

        final byte[][][] b = new byte[][][] { { { 1, 2 } }, null };
        final byte[][][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final byte[][][] c = new byte[0][0][0];
        final byte[][][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DShort() {
        assertNull(N.clone((short[][][]) null));

        final short[][][] a = new short[][][] { { { 1, 2 } }, { { 3, 4 } } };
        final short[][][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 9;
        assertEquals(1, a[0][0][0]);

        final short[][][] b = new short[][][] { { { 1, 2 } }, null };
        final short[][][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final short[][][] c = new short[0][0][0];
        final short[][][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DInt() {
        assertNull(N.clone((int[][][]) null));

        final int[][][] a = new int[][][] { { { 1, 2 } }, { { 3, 4 } } };
        final int[][][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 9;
        assertEquals(1, a[0][0][0]);

        final int[][][] b = new int[][][] { { { 1, 2 } }, null };
        final int[][][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final int[][][] c = new int[0][0][0];
        final int[][][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DLong() {
        assertNull(N.clone((long[][][]) null));

        final long[][][] a = new long[][][] { { { 1L, 2L } }, { { 3L, 4L } } };
        final long[][][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 9L;
        assertEquals(1L, a[0][0][0]);

        final long[][][] b = new long[][][] { { { 1L, 2L } }, null };
        final long[][][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final long[][][] c = new long[0][0][0];
        final long[][][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DFloat() {
        assertNull(N.clone((float[][][]) null));

        final float[][][] a = new float[][][] { { { 1.0f, 2.0f } }, { { 3.0f, 4.0f } } };
        final float[][][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 9.0f;
        assertEquals(1.0f, a[0][0][0], 0.000001);

        final float[][][] b = new float[][][] { { { 1.0f, 2.0f } }, null };
        final float[][][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final float[][][] c = new float[0][0][0];
        final float[][][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DDouble() {
        assertNull(N.clone((double[][][]) null));

        final double[][][] a = new double[][][] { { { 1.0, 2.0 } }, { { 3.0, 4.0 } } };
        final double[][][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = 9.0;
        assertEquals(1.0, a[0][0][0], 0.000001);

        final double[][][] b = new double[][][] { { { 1.0, 2.0 } }, null };
        final double[][][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final double[][][] c = new double[0][0][0];
        final double[][][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
    }

    @Test
    public void testClone3DGeneric() {
        assertNull(N.clone((String[][][]) null));

        final String[][][] a = new String[][][] { { { "a", "b" } }, { { "c", "d" } } };
        final String[][][] cloneA = N.clone(a);
        assertNotSame(a, cloneA);
        assertNotSame(a[0], cloneA[0]);
        assertNotSame(a[0][0], cloneA[0][0]);

        for (int i = 0; i < a.length; i++) {
            assertArrayEquals(a[i], cloneA[i]);
        }
        cloneA[0][0][0] = "z";
        assertEquals("a", a[0][0][0]);

        final String[][][] b = new String[][][] { { { "a", "b" } }, null };
        final String[][][] cloneB = N.clone(b);
        assertNotSame(b, cloneB);
        assertEquals(b.length, cloneB.length);
        assertArrayEquals(b[0], cloneB[0]);
        assertNull(cloneB[1]);

        final String[][][] c = new String[0][0][0];
        final String[][][] cloneC = N.clone(c);
        assertNotSame(c, cloneC);
        assertEquals(0, cloneC.length);
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
    public void testIsSortedByteArrayRange() {
        byte[] array = { 5, 1, 2, 3, 0 };
        assertTrue(N.isSorted(array, 1, 4));
        assertFalse(N.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedShortArrayRange() {
        short[] array = { 5, 1, 2, 3, 0 };
        assertTrue(N.isSorted(array, 1, 4));
        assertFalse(N.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedIntArrayRange() {
        int[] array = { 5, 1, 2, 3, 0 };
        assertTrue(N.isSorted(array, 1, 4));
        assertFalse(N.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedLongArrayRange() {
        long[] array = { 5L, 1L, 2L, 3L, 0L };
        assertTrue(N.isSorted(array, 1, 4));
        assertFalse(N.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedFloatArrayRange() {
        float[] array = { 5.0f, 1.0f, 2.0f, 3.0f, 0.0f };
        assertTrue(N.isSorted(array, 1, 4));
        assertFalse(N.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedDoubleArrayRange() {
        double[] array = { 5.0, 1.0, 2.0, 3.0, 0.0 };
        assertTrue(N.isSorted(array, 1, 4));
        assertFalse(N.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedObjectArrayRange() {
        String[] array = { "d", "a", "b", "c", "e" };
        assertTrue(N.isSorted(array, 1, 4));
        assertFalse(N.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedObjectArrayRangeWithComparator() {
        String[] array = { "d", "aaa", "bb", "c", "e" };

        assertTrue(N.isSorted(array, 1, 4, Comparator.comparing(String::length).reversed()));
        assertFalse(N.isSorted(array, 0, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testIsSortedCollectionWithComparator() {
        List<String> list = Arrays.asList("aaa", "bb", "c");

        assertTrue(N.isSorted(list, Comparator.naturalOrder()));
        assertTrue(N.isSorted(list, Comparator.comparing(String::length).reversed()));
    }

    @Test
    public void testIsSortedCollectionRangeWithComparator() {
        List<String> list = Arrays.asList("d", "aaa", "bb", "c", "e");

        assertTrue(N.isSorted(list, 1, 4, Comparator.comparing(String::length).reversed()));
        assertFalse(N.isSorted(list, 0, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testIsSortedBooleanArray() {
        assertTrue(N.isSorted((boolean[]) null));
        assertTrue(N.isSorted(new boolean[0]));
        assertTrue(N.isSorted(new boolean[] { true }));

        assertTrue(N.isSorted(new boolean[] { false, false, true, true }));
        assertTrue(N.isSorted(new boolean[] { false, true }));

        assertFalse(N.isSorted(new boolean[] { true, false }));
        assertFalse(N.isSorted(new boolean[] { false, true, false }));
    }

    @Test
    public void testIsSortedCharArray() {
        assertTrue(N.isSorted((char[]) null));
        assertTrue(N.isSorted(new char[0]));
        assertTrue(N.isSorted(new char[] { 'a' }));

        assertTrue(N.isSorted(new char[] { 'a', 'b', 'c', 'd' }));
        assertTrue(N.isSorted(new char[] { 'a', 'a', 'b', 'b' }));

        assertFalse(N.isSorted(new char[] { 'b', 'a' }));
        assertFalse(N.isSorted(new char[] { 'a', 'c', 'b' }));
    }

    @Test
    public void testIsSortedByteArray() {
        assertTrue(N.isSorted((byte[]) null));
        assertTrue(N.isSorted(new byte[0]));
        assertTrue(N.isSorted(new byte[] { 1, 2, 3 }));
        assertFalse(N.isSorted(new byte[] { 3, 2, 1 }));
    }

    @Test
    public void testIsSortedShortArray() {
        assertTrue(N.isSorted((short[]) null));
        assertTrue(N.isSorted(new short[0]));
        assertTrue(N.isSorted(new short[] { 1, 2, 3 }));
        assertFalse(N.isSorted(new short[] { 3, 2, 1 }));
    }

    @Test
    public void testIsSortedIntArray() {
        assertTrue(N.isSorted((int[]) null));
        assertTrue(N.isSorted(new int[0]));
        assertTrue(N.isSorted(new int[] { 1, 2, 3 }));
        assertFalse(N.isSorted(new int[] { 3, 2, 1 }));
    }

    @Test
    public void testIsSortedLongArray() {
        assertTrue(N.isSorted((long[]) null));
        assertTrue(N.isSorted(new long[0]));
        assertTrue(N.isSorted(new long[] { 1L, 2L, 3L }));
        assertFalse(N.isSorted(new long[] { 3L, 2L, 1L }));
    }

    @Test
    public void testIsSortedFloatArray() {
        assertTrue(N.isSorted((float[]) null));
        assertTrue(N.isSorted(new float[0]));
        assertTrue(N.isSorted(new float[] { 1.0f, 2.0f, 3.0f }));
        assertFalse(N.isSorted(new float[] { 3.0f, 2.0f, 1.0f }));

        assertTrue(N.isSorted(new float[] { 1.0f, 2.0f, Float.NaN }));
    }

    @Test
    public void testIsSortedDoubleArray() {
        assertTrue(N.isSorted((double[]) null));
        assertTrue(N.isSorted(new double[0]));
        assertTrue(N.isSorted(new double[] { 1.0, 2.0, 3.0 }));
        assertFalse(N.isSorted(new double[] { 3.0, 2.0, 1.0 }));

        assertTrue(N.isSorted(new double[] { 1.0, 2.0, Double.NaN }));
    }

    @Test
    public void testIsSortedObjectArray() {
        assertTrue(N.isSorted((String[]) null));
        assertTrue(N.isSorted(new String[0]));
        assertTrue(N.isSorted(new String[] { "a", "b", "c" }));
        assertFalse(N.isSorted(new String[] { "c", "b", "a" }));

        assertTrue(N.isSorted(new String[] { null, null, "a", "b" }));
        assertFalse(N.isSorted(new String[] { "a", null, "b" }));
    }

    @Test
    public void testIsSortedObjectArrayWithComparator() {
        String[] array = { "aaa", "bb", "c" };

        assertTrue(N.isSorted(array, Comparator.naturalOrder()));

        assertTrue(N.isSorted(array, Comparator.comparing(String::length).reversed()));

        assertTrue(N.isSorted(array, null));
    }

    @Test
    public void testIsSortedCollection() {
        assertTrue(N.isSorted((Collection<String>) null));
        assertTrue(N.isSorted(new ArrayList<String>()));
        assertTrue(N.isSorted(Arrays.asList("a", "b", "c")));
        assertFalse(N.isSorted(Arrays.asList("c", "b", "a")));

        assertTrue(N.isSorted(Arrays.asList(null, null, "a", "b")));
    }

    @Test
    public void testIsSortedBooleanArrayRange() {
        boolean[] array = { true, false, false, true, true };

        assertTrue(N.isSorted(array, 1, 3));
        assertTrue(N.isSorted(array, 3, 5));
        assertFalse(N.isSorted(array, 0, 3));

        assertTrue(N.isSorted(array, 2, 2));
        assertTrue(N.isSorted(array, 2, 3));

        assertThrows(IndexOutOfBoundsException.class, () -> N.isSorted(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.isSorted(array, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> N.isSorted(array, 3, 2));
    }

    @Test
    public void testIsSortedCharArrayRange() {
        char[] array = { 'd', 'a', 'b', 'c', 'e' };

        assertTrue(N.isSorted(array, 1, 4));
        assertFalse(N.isSorted(array, 0, 3));

        assertThrows(IndexOutOfBoundsException.class, () -> N.isSorted(array, -1, 2));
    }

    @Test
    public void testIsSortedCollectionRange() {
        List<String> list = Arrays.asList("d", "a", "b", "c", "e");
        assertTrue(N.isSorted(list, 1, 4));
        assertFalse(N.isSorted(list, 0, 3));

        assertThrows(IndexOutOfBoundsException.class, () -> N.isSorted(list, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.isSorted(list, 0, 6));
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
    public void testSortCharArrayRange() {
        char[] array = { 'd', 'c', 'b', 'a', 'e' };
        N.sort(array, 1, 4);
        assertArrayEquals(new char[] { 'd', 'a', 'b', 'c', 'e' }, array);
    }

    @Test
    public void testSortByteArrayRange() {
        byte[] array = { 5, 3, 2, 1, 4 };
        N.sort(array, 1, 4);
        assertArrayEquals(new byte[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortShortArrayRange() {
        short[] array = { 5, 3, 2, 1, 4 };
        N.sort(array, 1, 4);
        assertArrayEquals(new short[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortIntArrayRange() {
        int[] array = { 5, 3, 2, 1, 4 };
        N.sort(array, 1, 4);
        assertArrayEquals(new int[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortLongArrayRange() {
        long[] array = { 5L, 3L, 2L, 1L, 4L };
        N.sort(array, 1, 4);
        assertArrayEquals(new long[] { 5L, 1L, 2L, 3L, 4L }, array);
    }

    @Test
    public void testSortFloatArrayRange() {
        float[] array = { 5.0f, 3.0f, 2.0f, 1.0f, 4.0f };
        N.sort(array, 1, 4);
        assertArrayEquals(new float[] { 5.0f, 1.0f, 2.0f, 3.0f, 4.0f }, array);
    }

    @Test
    public void testSortDoubleArrayRange() {
        double[] array = { 5.0, 3.0, 2.0, 1.0, 4.0 };
        N.sort(array, 1, 4);
        assertArrayEquals(new double[] { 5.0, 1.0, 2.0, 3.0, 4.0 }, array);
    }

    @Test
    public void testSortObjectArrayRange() {
        String[] array = { "e", "d", "c", "b", "a" };
        N.sort(array, 1, 4);
        assertArrayEquals(new String[] { "e", "b", "c", "d", "a" }, array);
    }

    @Test
    public void testSortObjectArrayWithComparator() {
        String[] array = { "aaa", "b", "cc" };
        N.sort(array, Comparator.comparing(String::length));
        assertArrayEquals(new String[] { "b", "cc", "aaa" }, array);
    }

    @Test
    public void testSortObjectArrayRangeWithComparator() {
        String[] array = { "e", "aaa", "b", "cc", "d" };
        N.sort(array, 1, 4, Comparator.comparing(String::length));
        assertArrayEquals(new String[] { "e", "b", "cc", "aaa", "d" }, array);
    }

    @Test
    public void testSortListRange() {
        List<String> list = new ArrayList<>(Arrays.asList("e", "d", "c", "b", "a"));
        N.sort(list, 1, 4);
        assertEquals(Arrays.asList("e", "b", "c", "d", "a"), list);
    }

    @Test
    public void testSortListWithComparator() {
        List<String> list = new ArrayList<>(Arrays.asList("aaa", "b", "cc"));
        N.sort(list, Comparator.comparing(String::length));
        assertEquals(Arrays.asList("b", "cc", "aaa"), list);
    }

    // Tests for sort(List, fromIndex, toIndex, Comparator)
    @Test
    public void testSort_List_WithRange_AndComparator() {
        List<String> list = new ArrayList<>(Arrays.asList("d", "b", "a", "c", "e"));
        N.sort(list, 1, 4, Comparator.naturalOrder());
        assertEquals(Arrays.asList("d", "a", "b", "c", "e"), list);
    }

    @Test
    public void testSort_List_FullRange_WithComparator() {
        List<Integer> list = new ArrayList<>(Arrays.asList(3, 1, 4, 1, 5));
        N.sort(list, 0, list.size(), Comparator.naturalOrder());
        assertEquals(Arrays.asList(1, 1, 3, 4, 5), list);
    }

    @Test
    public void testSortBooleanArray() {
        N.sort((boolean[]) null);
        N.sort(new boolean[0]);

        boolean[] array = { true, false, true, false, false };
        N.sort(array);
        assertArrayEquals(new boolean[] { false, false, false, true, true }, array);

        boolean[] sorted = { false, false, true, true };
        N.sort(sorted);
        assertArrayEquals(new boolean[] { false, false, true, true }, sorted);
    }

    @Test
    public void testSortCharArray() {
        N.sort((char[]) null);
        N.sort(new char[0]);

        char[] array = { 'd', 'b', 'a', 'c' };
        N.sort(array);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, array);
    }

    @Test
    public void testSortByteArray() {
        N.sort((byte[]) null);
        N.sort(new byte[0]);

        byte[] array = { 4, 2, 1, 3 };
        N.sort(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortShortArray() {
        N.sort((short[]) null);
        N.sort(new short[0]);

        short[] array = { 4, 2, 1, 3 };
        N.sort(array);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortIntArray() {
        N.sort((int[]) null);
        N.sort(new int[0]);

        int[] array = { 4, 2, 1, 3 };
        N.sort(array);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortLongArray() {
        N.sort((long[]) null);
        N.sort(new long[0]);

        long[] array = { 4L, 2L, 1L, 3L };
        N.sort(array);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, array);
    }

    @Test
    public void testSortFloatArray() {
        N.sort((float[]) null);
        N.sort(new float[0]);

        float[] array = { 4.0f, 2.0f, 1.0f, 3.0f };
        N.sort(array);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, array);
    }

    @Test
    public void testSortDoubleArray() {
        N.sort((double[]) null);
        N.sort(new double[0]);

        double[] array = { 4.0, 2.0, 1.0, 3.0 };
        N.sort(array);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, array);
    }

    @Test
    public void testSortObjectArray() {
        N.sort((Object[]) null);
        N.sort(new Object[0]);

        String[] array = { "d", "b", "a", "c" };
        N.sort(array);
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, array);

        String[] arrayWithNulls = { "b", null, "a", null };
        N.sort(arrayWithNulls);
        assertArrayEquals(new String[] { null, null, "a", "b" }, arrayWithNulls);
    }

    @Test
    public void testSortList() {
        N.sort((List<String>) null);
        N.sort(new ArrayList<String>());

        List<String> list = new ArrayList<>(Arrays.asList("d", "b", "a", "c"));
        N.sort(list);
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
    }

    @Test
    public void testSortListRangeWithComparator() {
        List<String> list = new ArrayList<>(Arrays.asList("e", "aaa", "b", "cc", "d"));
        N.sort(list, 1, 4, Comparator.comparing(String::length));
        assertEquals(Arrays.asList("e", "b", "cc", "aaa", "d"), list);

        List<String> emptyList = new ArrayList<>();
        N.sort(emptyList, 0, 0, Comparator.naturalOrder());
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testSort_List_WithRange_NullComparator() {
        List<String> list = new ArrayList<>(Arrays.asList("d", "b", "a", "c", "e"));
        N.sort(list, 1, 4, (Comparator<String>) null);
        assertEquals(Arrays.asList("d", "a", "b", "c", "e"), list);
    }

    @Test
    public void testSortBooleanArrayRange() {
        boolean[] array = { true, true, false, true, false };
        N.sort(array, 1, 4);
        assertArrayEquals(new boolean[] { true, false, true, true, false }, array);

        boolean[] array2 = { true, false };
        N.sort(array2, 1, 1);
        assertArrayEquals(new boolean[] { true, false }, array2);

        assertThrows(IndexOutOfBoundsException.class, () -> N.sort(array, -1, 2));
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
    public void testSortByFloat() {
        List<String> list = Arrays.asList("apple", "pie", "a");
        N.sortByFloat(list, s -> (float) s.length());
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
    public void testParallelSortCharArrayRange() {
        char[] array = { 'e', 'd', 'c', 'b', 'a' };
        N.parallelSort(array, 1, 4);
        assertArrayEquals(new char[] { 'e', 'b', 'c', 'd', 'a' }, array);
    }

    @Test
    public void testParallelSortByteArrayRange() {
        byte[] array = { 5, 3, 2, 1, 4 };
        N.parallelSort(array, 1, 4);
        assertArrayEquals(new byte[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortShortArrayRange() {
        short[] array = { 5, 3, 2, 1, 4 };
        N.parallelSort(array, 1, 4);
        assertArrayEquals(new short[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortIntArrayRange() {
        int[] array = { 5, 3, 2, 1, 4 };
        N.parallelSort(array, 1, 4);
        assertArrayEquals(new int[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortLongArrayRange() {
        long[] array = { 5L, 3L, 2L, 1L, 4L };
        N.parallelSort(array, 1, 4);
        assertArrayEquals(new long[] { 5L, 1L, 2L, 3L, 4L }, array);
    }

    @Test
    public void testParallelSortFloatArrayRange() {
        float[] array = { 5.0f, 3.0f, 2.0f, 1.0f, 4.0f };
        N.parallelSort(array, 1, 4);
        assertArrayEquals(new float[] { 5.0f, 1.0f, 2.0f, 3.0f, 4.0f }, array);
    }

    @Test
    public void testParallelSortDoubleArrayRange() {
        double[] array = { 5.0, 3.0, 2.0, 1.0, 4.0 };
        N.parallelSort(array, 1, 4);
        assertArrayEquals(new double[] { 5.0, 1.0, 2.0, 3.0, 4.0 }, array);
    }

    @Test
    public void testParallelSortObjectArrayRange() {
        String[] array = { "e", "d", "c", "b", "a" };
        N.parallelSort(array, 1, 4);
        assertArrayEquals(new String[] { "e", "b", "c", "d", "a" }, array);
    }

    @Test
    public void testParallelSortObjectArrayWithComparator() {
        String[] array = { "aaa", "b", "cc" };
        N.parallelSort(array, Comparator.comparing(String::length));
        assertArrayEquals(new String[] { "b", "cc", "aaa" }, array);
    }

    @Test
    public void testParallelSortObjectArrayRangeWithComparator() {
        String[] array = { "e", "aaa", "b", "cc", "d" };
        N.parallelSort(array, 1, 4, Comparator.comparing(String::length));
        assertArrayEquals(new String[] { "e", "b", "cc", "aaa", "d" }, array);
    }

    @Test
    public void testParallelSortListRange() {
        List<String> list = new ArrayList<>(Arrays.asList("e", "d", "c", "b", "a"));
        N.parallelSort(list, 1, 4);
        assertEquals(Arrays.asList("e", "b", "c", "d", "a"), list);
    }

    @Test
    public void testParallelSortListWithComparator() {
        List<String> list = new ArrayList<>(Arrays.asList("aaa", "b", "cc"));
        N.parallelSort(list, Comparator.comparing(String::length));
        assertEquals(Arrays.asList("b", "cc", "aaa"), list);
    }

    @Test
    public void testParallelSortListRangeWithComparator() {
        List<String> list = new ArrayList<>(Arrays.asList("e", "aaa", "b", "cc", "d"));
        N.parallelSort(list, 1, 4, Comparator.comparing(String::length));
        assertEquals(Arrays.asList("e", "b", "cc", "aaa", "d"), list);
    }

    @Test
    public void testParallelSortCharArray() {
        N.parallelSort((char[]) null);
        N.parallelSort(new char[0]);

        char[] array = { 'd', 'b', 'a', 'c' };
        N.parallelSort(array);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, array);
    }

    @Test
    public void testParallelSortByteArray() {
        N.parallelSort((byte[]) null);
        N.parallelSort(new byte[0]);

        byte[] array = { 4, 2, 1, 3 };
        N.parallelSort(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortShortArray() {
        N.parallelSort((short[]) null);
        N.parallelSort(new short[0]);

        short[] array = { 4, 2, 1, 3 };
        N.parallelSort(array);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortIntArray() {
        N.parallelSort((int[]) null);
        N.parallelSort(new int[0]);

        int[] array = { 4, 2, 1, 3 };
        N.parallelSort(array);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortLongArray() {
        N.parallelSort((long[]) null);
        N.parallelSort(new long[0]);

        long[] array = { 4L, 2L, 1L, 3L };
        N.parallelSort(array);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, array);
    }

    @Test
    public void testParallelSortFloatArray() {
        N.parallelSort((float[]) null);
        N.parallelSort(new float[0]);

        float[] array = { 4.0f, 2.0f, 1.0f, 3.0f };
        N.parallelSort(array);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, array);
    }

    @Test
    public void testParallelSortDoubleArray() {
        N.parallelSort((double[]) null);
        N.parallelSort(new double[0]);

        double[] array = { 4.0, 2.0, 1.0, 3.0 };
        N.parallelSort(array);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, array);
    }

    @Test
    public void testParallelSortObjectArray() {
        N.parallelSort((String[]) null);
        N.parallelSort(new String[0]);

        String[] array = { "d", "b", "a", "c" };
        N.parallelSort(array);
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, array);
    }

    @Test
    public void testParallelSortList() {
        N.parallelSort((List<String>) null);
        N.parallelSort(new ArrayList<String>());

        List<String> list = new ArrayList<>(Arrays.asList("d", "b", "a", "c"));
        N.parallelSort(list);
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
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
    public void testParallelSortByFloat() {
        List<String> list = Arrays.asList("apple", "pie", "a");
        N.parallelSortByFloat(list, s -> (float) s.length());
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
    public void testReverseSortBooleanArrayRange() {
        boolean[] array = { false, false, true, false, true };
        N.reverseSort(array, 1, 4);
        assertArrayEquals(new boolean[] { false, true, false, false, true }, array);
    }

    @Test
    public void testReverseSortCharArray() {
        char[] array = { 'a', 'c', 'b', 'd' };
        N.reverseSort(array);
        assertArrayEquals(new char[] { 'd', 'c', 'b', 'a' }, array);
    }

    @Test
    public void testReverseSortCharArrayRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        N.reverseSort(array, 1, 4);
        assertArrayEquals(new char[] { 'a', 'd', 'c', 'b', 'e' }, array);
    }

    @Test
    public void testReverseSortByteArray() {
        byte[] array = { 1, 3, 2, 4 };
        N.reverseSort(array);
        assertArrayEquals(new byte[] { 4, 3, 2, 1 }, array);
    }

    @Test
    public void testReverseSortByteArrayRange() {
        byte[] array = { 1, 2, 3, 4, 5 };
        N.reverseSort(array, 1, 4);
        assertArrayEquals(new byte[] { 1, 4, 3, 2, 5 }, array);
    }

    @Test
    public void testReverseSortShortArray() {
        short[] array = { 1, 3, 2, 4 };
        N.reverseSort(array);
        assertArrayEquals(new short[] { 4, 3, 2, 1 }, array);
    }

    @Test
    public void testReverseSortShortArrayRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        N.reverseSort(array, 1, 4);
        assertArrayEquals(new short[] { 1, 4, 3, 2, 5 }, array);
    }

    @Test
    public void testReverseSortIntArray() {
        int[] array = { 1, 3, 2, 4 };
        N.reverseSort(array);
        assertArrayEquals(new int[] { 4, 3, 2, 1 }, array);
    }

    @Test
    public void testReverseSortIntArrayRange() {
        int[] array = { 1, 2, 3, 4, 5 };
        N.reverseSort(array, 1, 4);
        assertArrayEquals(new int[] { 1, 4, 3, 2, 5 }, array);
    }

    @Test
    public void testReverseSortLongArray() {
        long[] array = { 1L, 3L, 2L, 4L };
        N.reverseSort(array);
        assertArrayEquals(new long[] { 4L, 3L, 2L, 1L }, array);
    }

    @Test
    public void testReverseSortLongArrayRange() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        N.reverseSort(array, 1, 4);
        assertArrayEquals(new long[] { 1L, 4L, 3L, 2L, 5L }, array);
    }

    @Test
    public void testReverseSortFloatArray() {
        float[] array = { 1.0f, 3.0f, 2.0f, 4.0f };
        N.reverseSort(array);
        assertArrayEquals(new float[] { 4.0f, 3.0f, 2.0f, 1.0f }, array);
    }

    @Test
    public void testReverseSortFloatArrayRange() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        N.reverseSort(array, 1, 4);
        assertArrayEquals(new float[] { 1.0f, 4.0f, 3.0f, 2.0f, 5.0f }, array);
    }

    @Test
    public void testReverseSortDoubleArray() {
        double[] array = { 1.0, 3.0, 2.0, 4.0 };
        N.reverseSort(array);
        assertArrayEquals(new double[] { 4.0, 3.0, 2.0, 1.0 }, array);
    }

    @Test
    public void testReverseSortDoubleArrayRange() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        N.reverseSort(array, 1, 4);
        assertArrayEquals(new double[] { 1.0, 4.0, 3.0, 2.0, 5.0 }, array);
    }

    @Test
    public void testReverseSortObjectArray() {
        String[] array = { "a", "c", "b", "d" };
        N.reverseSort(array);
        assertArrayEquals(new String[] { "d", "c", "b", "a" }, array);
    }

    @Test
    public void testReverseSortObjectArrayRange() {
        String[] array = { "a", "b", "c", "d", "e" };
        N.reverseSort(array, 1, 4);
        assertArrayEquals(new String[] { "a", "d", "c", "b", "e" }, array);
    }

    @Test
    public void testReverseSortList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "c", "b", "d"));
        N.reverseSort(list);
        assertEquals(Arrays.asList("d", "c", "b", "a"), list);
    }

    @Test
    public void testReverseSortListRange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        N.reverseSort(list, 1, 4);
        assertEquals(Arrays.asList("a", "d", "c", "b", "e"), list);
    }

    @Test
    public void testReverseSortBooleanArray() {
        N.reverseSort((boolean[]) null);

        boolean[] array = { false, true, false, true, true };
        N.reverseSort(array);
        assertArrayEquals(new boolean[] { true, true, true, false, false }, array);
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
    public void testReverseSortByFloat() {
        List<String> list = Arrays.asList("a", "cat", "apple");
        N.reverseSortByFloat(list, s -> (float) s.length());
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
    public void testBinarySearchCharArrayRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        assertEquals(2, N.binarySearch(array, 1, 4, 'c'));
        assertTrue(N.binarySearch(array, 1, 4, 'a') < 0);
        assertTrue(N.binarySearch(array, 1, 4, 'e') < 0);
    }

    @Test
    public void testBinarySearchByteArrayRange() {
        byte[] array = { 1, 2, 3, 4, 5 };
        assertEquals(2, N.binarySearch(array, 1, 4, (byte) 3));
        assertTrue(N.binarySearch(array, 1, 4, (byte) 1) < 0);
        assertTrue(N.binarySearch(array, 1, 4, (byte) 5) < 0);
    }

    // ========== binarySearch(List, fromIndex, toIndex, value, comparator) ==========

    @Test
    public void testBinarySearch_List_WithRange_AndComparator() {
        List<Integer> list = Arrays.asList(1, 3, 5, 7, 9, 11);
        // Search within sub-range [1,4)  = [3, 5, 7]
        int idx = N.binarySearch(list, 1, 4, 5, Integer::compareTo);
        assertEquals(2, idx);
    }

    @Test
    public void testBinarySearch_List_WithRange_NotFound() {
        List<Integer> list = Arrays.asList(1, 3, 5, 7, 9);
        int idx = N.binarySearch(list, 0, 5, 4, Integer::compareTo);
        assertTrue(idx < 0);
    }

    @Test
    public void testBinarySearch_shortArray() {
        short[] arr = { 1, 3, 5, 7, 9 };
        Assertions.assertEquals(2, N.binarySearch(arr, (short) 5));
        Assertions.assertEquals(-1, N.binarySearch(arr, (short) 0));
        Assertions.assertEquals(-6, N.binarySearch(arr, (short) 10));

        short[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, (short) 5));

        Assertions.assertEquals(-1, N.binarySearch((short[]) null, (short) 5));
    }

    @Test
    public void testBinarySearch_intArray() {
        int[] arr = { 1, 3, 5, 7, 9 };
        Assertions.assertEquals(2, N.binarySearch(arr, 5));
        Assertions.assertEquals(-1, N.binarySearch(arr, 0));
        Assertions.assertEquals(-6, N.binarySearch(arr, 10));

        int[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 5));

        Assertions.assertEquals(-1, N.binarySearch((int[]) null, 5));
    }

    @Test
    public void testBinarySearch_intArray_withRange() {
        int[] arr = { 1, 3, 5, 7, 9, 11, 13 };
        Assertions.assertEquals(3, N.binarySearch(arr, 1, 5, 7));
        Assertions.assertEquals(-2, N.binarySearch(arr, 1, 5, 2));

        int[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 0, 0, 5));
    }

    @Test
    public void testBinarySearch_longArray() {
        long[] arr = { 1L, 3L, 5L, 7L, 9L };
        Assertions.assertEquals(2, N.binarySearch(arr, 5L));
        Assertions.assertEquals(-1, N.binarySearch(arr, 0L));
        Assertions.assertEquals(-6, N.binarySearch(arr, 10L));

        long[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 5L));

        Assertions.assertEquals(-1, N.binarySearch((long[]) null, 5L));
    }

    @Test
    public void testBinarySearch_longArray_withRange() {
        long[] arr = { 1L, 3L, 5L, 7L, 9L, 11L, 13L };
        Assertions.assertEquals(3, N.binarySearch(arr, 1, 5, 7L));
        Assertions.assertEquals(-2, N.binarySearch(arr, 1, 5, 2L));

        long[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 0, 0, 5L));
    }

    @Test
    public void testBinarySearch_floatArray() {
        float[] arr = { 1.0f, 3.0f, 5.0f, 7.0f, 9.0f };
        Assertions.assertEquals(2, N.binarySearch(arr, 5.0f));
        Assertions.assertEquals(-1, N.binarySearch(arr, 0.0f));
        Assertions.assertEquals(-6, N.binarySearch(arr, 10.0f));

        float[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 5.0f));

        Assertions.assertEquals(-1, N.binarySearch((float[]) null, 5.0f));
    }

    @Test
    public void testBinarySearch_floatArray_withRange() {
        float[] arr = { 1.0f, 3.0f, 5.0f, 7.0f, 9.0f, 11.0f, 13.0f };
        Assertions.assertEquals(3, N.binarySearch(arr, 1, 5, 7.0f));
        Assertions.assertEquals(-2, N.binarySearch(arr, 1, 5, 2.0f));

        float[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 0, 0, 5.0f));
    }

    @Test
    public void testBinarySearch_doubleArray() {
        double[] arr = { 1.0, 3.0, 5.0, 7.0, 9.0 };
        Assertions.assertEquals(2, N.binarySearch(arr, 5.0));
        Assertions.assertEquals(-1, N.binarySearch(arr, 0.0));
        Assertions.assertEquals(-6, N.binarySearch(arr, 10.0));

        double[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 5.0));

        Assertions.assertEquals(-1, N.binarySearch((double[]) null, 5.0));
    }

    @Test
    public void testBinarySearch_doubleArray_withRange() {
        double[] arr = { 1.0, 3.0, 5.0, 7.0, 9.0, 11.0, 13.0 };
        Assertions.assertEquals(3, N.binarySearch(arr, 1, 5, 7.0));
        Assertions.assertEquals(-2, N.binarySearch(arr, 1, 5, 2.0));

        double[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 0, 0, 5.0));
    }

    @Test
    public void testBinarySearch_objectArray() {
        String[] arr = { "a", "c", "e", "g", "i" };
        Assertions.assertEquals(2, N.binarySearch(arr, "e"));
        Assertions.assertEquals(-1, N.binarySearch(arr, ""));
        Assertions.assertEquals(-6, N.binarySearch(arr, "z"));

        String[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, "e"));

        Assertions.assertEquals(-1, N.binarySearch((String[]) null, "e"));
    }

    @Test
    public void testBinarySearch_objectArray_withRange() {
        String[] arr = { "a", "c", "e", "g", "i", "k", "m" };
        Assertions.assertEquals(3, N.binarySearch(arr, 1, 5, "g"));
        Assertions.assertEquals(-2, N.binarySearch(arr, 1, 5, "b"));

        String[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 0, 0, "e"));
    }

    @Test
    public void testBinarySearch_genericArray_withComparator() {
        String[] arr = { "a", "c", "e", "g", "i" };
        Comparator<String> cmp = String::compareTo;
        Assertions.assertEquals(2, N.binarySearch(arr, "e", cmp));
        Assertions.assertEquals(-1, N.binarySearch(arr, "", cmp));

        String[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, "e", cmp));

        Assertions.assertEquals(-1, N.binarySearch((String[]) null, "e", cmp));
    }

    @Test
    public void testBinarySearch_genericArray_withRangeAndComparator() {
        String[] arr = { "a", "c", "e", "g", "i", "k", "m" };
        Comparator<String> cmp = String::compareTo;
        Assertions.assertEquals(3, N.binarySearch(arr, 1, 5, "g", cmp));
        Assertions.assertEquals(-2, N.binarySearch(arr, 1, 5, "b", cmp));

        String[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 0, 0, "e", cmp));
    }

    @Test
    public void testBinarySearch_list() {
        List<Integer> list = Arrays.asList(1, 3, 5, 7, 9);
        Assertions.assertEquals(2, N.binarySearch(list, 5));
        Assertions.assertEquals(-1, N.binarySearch(list, 0));
        Assertions.assertEquals(-6, N.binarySearch(list, 10));

        List<Integer> empty = new ArrayList<>();
        Assertions.assertEquals(-1, N.binarySearch(empty, 5));

        Assertions.assertEquals(-1, N.binarySearch((List<Integer>) null, 5));
    }

    @Test
    public void testBinarySearch_list_withRange() {
        List<Integer> list = Arrays.asList(1, 3, 5, 7, 9, 11, 13);
        Assertions.assertEquals(3, N.binarySearch(list, 1, 5, 7));
        Assertions.assertEquals(-2, N.binarySearch(list, 1, 5, 2));

        List<Integer> empty = new ArrayList<>();
        Assertions.assertEquals(-1, N.binarySearch(empty, 0, 0, 5));
    }

    @Test
    public void testBinarySearch_list_withComparator() {
        List<String> list = Arrays.asList("a", "c", "e", "g", "i");
        Comparator<String> cmp = String::compareTo;
        Assertions.assertEquals(2, N.binarySearch(list, "e", cmp));
        Assertions.assertEquals(-1, N.binarySearch(list, "", cmp));

        List<String> empty = new ArrayList<>();
        Assertions.assertEquals(-1, N.binarySearch(empty, "e", cmp));

        Assertions.assertEquals(-1, N.binarySearch((List<String>) null, "e", cmp));
    }

    @Test
    public void testBinarySearch_list_withRangeAndComparator() {
        List<String> list = Arrays.asList("a", "c", "e", "g", "i", "k", "m");
        Comparator<String> cmp = String::compareTo;
        Assertions.assertEquals(3, N.binarySearch(list, 1, 5, "g", cmp));
        Assertions.assertEquals(-2, N.binarySearch(list, 1, 5, "b", cmp));

        List<String> empty = new ArrayList<>();
        Assertions.assertEquals(-1, N.binarySearch(empty, 0, 0, "e", cmp));
    }

    @Test
    public void testBinarySearchCharArray() {
        assertEquals(-1, N.binarySearch((char[]) null, 'a'));
        assertEquals(-1, N.binarySearch(new char[0], 'a'));

        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        assertEquals(0, N.binarySearch(array, 'a'));
        assertEquals(2, N.binarySearch(array, 'c'));
        assertEquals(4, N.binarySearch(array, 'e'));
        assertTrue(N.binarySearch(array, 'f') < 0);
    }

    @Test
    public void testBinarySearchByteArray() {
        assertEquals(-1, N.binarySearch((byte[]) null, (byte) 1));
        assertEquals(-1, N.binarySearch(new byte[0], (byte) 1));

        byte[] array = { 1, 2, 3, 4, 5 };
        assertEquals(0, N.binarySearch(array, (byte) 1));
        assertEquals(2, N.binarySearch(array, (byte) 3));
        assertEquals(4, N.binarySearch(array, (byte) 5));
        assertTrue(N.binarySearch(array, (byte) 6) < 0);
    }

    @Test
    public void testBinarySearchShortArray() {
        assertEquals(-1, N.binarySearch((short[]) null, (short) 1));
        assertEquals(-1, N.binarySearch(new short[0], (short) 1));

        short[] array = { 1, 2, 3, 4, 5 };
        assertEquals(0, N.binarySearch(array, (short) 1));
        assertEquals(2, N.binarySearch(array, (short) 3));
        assertEquals(4, N.binarySearch(array, (short) 5));
        assertTrue(N.binarySearch(array, (short) 6) < 0);
    }

    @Test
    public void testBinarySearch_shortArray_withRange() {
        short[] arr = { 1, 3, 5, 7, 9, 11, 13 };
        Assertions.assertEquals(3, N.binarySearch(arr, 1, 5, (short) 7));
        Assertions.assertEquals(-2, N.binarySearch(arr, 1, 5, (short) 2));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.binarySearch(arr, 5, 2, (short) 5));

        short[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 0, 0, (short) 5));
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
    public void testIndexOf_BooleanArray() {
        boolean[] arr = { true, false, true, false };
        assertEquals(0, N.indexOf(arr, true));
        assertEquals(1, N.indexOf(arr, false));
        assertEquals(-1, N.indexOf((boolean[]) null, true));
        assertEquals(2, N.indexOf(arr, true, 1));
    }

    @Test
    public void testIndexOf_booleanArray() {
        boolean[] arr = { true, false, true, false, true };
        Assertions.assertEquals(0, N.indexOf(arr, true));
        Assertions.assertEquals(1, N.indexOf(arr, false));

        boolean[] empty = {};
        Assertions.assertEquals(-1, N.indexOf(empty, true));

        Assertions.assertEquals(-1, N.indexOf((boolean[]) null, true));
    }

    @Test
    public void testIndexOf_booleanArray_withFromIndex() {
        boolean[] arr = { true, false, true, false, true };
        Assertions.assertEquals(2, N.indexOf(arr, true, 1));
        Assertions.assertEquals(3, N.indexOf(arr, false, 2));
        Assertions.assertEquals(-1, N.indexOf(arr, true, 10));

        Assertions.assertEquals(0, N.indexOf(arr, true, -1));
    }

    @Test
    public void testIndexOf_charArray() {
        char[] arr = { 'a', 'b', 'c', 'd', 'e' };
        Assertions.assertEquals(2, N.indexOf(arr, 'c'));
        Assertions.assertEquals(-1, N.indexOf(arr, 'z'));

        char[] empty = {};
        Assertions.assertEquals(-1, N.indexOf(empty, 'c'));

        Assertions.assertEquals(-1, N.indexOf((char[]) null, 'c'));
    }

    @Test
    public void testIndexOf_charArray_withFromIndex() {
        char[] arr = { 'a', 'b', 'c', 'd', 'c' };
        Assertions.assertEquals(4, N.indexOf(arr, 'c', 3));
        Assertions.assertEquals(-1, N.indexOf(arr, 'c', 10));
    }

    @Test
    public void testIndexOf_byteArray() {
        byte[] arr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals(2, N.indexOf(arr, (byte) 3));
        Assertions.assertEquals(-1, N.indexOf(arr, (byte) 10));

        byte[] empty = {};
        Assertions.assertEquals(-1, N.indexOf(empty, (byte) 3));

        Assertions.assertEquals(-1, N.indexOf((byte[]) null, (byte) 3));
    }

    @Test
    public void testIndexOf_byteArray_withFromIndex() {
        byte[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(4, N.indexOf(arr, (byte) 3, 3));
        Assertions.assertEquals(-1, N.indexOf(arr, (byte) 3, 10));
    }

    @Test
    public void testIndexOf_shortArray() {
        short[] arr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals(2, N.indexOf(arr, (short) 3));
        Assertions.assertEquals(-1, N.indexOf(arr, (short) 10));

        short[] empty = {};
        Assertions.assertEquals(-1, N.indexOf(empty, (short) 3));

        Assertions.assertEquals(-1, N.indexOf((short[]) null, (short) 3));
    }

    @Test
    public void testIndexOf_shortArray_withFromIndex() {
        short[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(4, N.indexOf(arr, (short) 3, 3));
        Assertions.assertEquals(-1, N.indexOf(arr, (short) 3, 10));
    }

    @Test
    public void testIndexOf_intArray() {
        int[] arr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals(2, N.indexOf(arr, 3));
        Assertions.assertEquals(-1, N.indexOf(arr, 10));

        int[] empty = {};
        Assertions.assertEquals(-1, N.indexOf(empty, 3));

        Assertions.assertEquals(-1, N.indexOf((int[]) null, 3));
    }

    @Test
    public void testIndexOf_intArray_withFromIndex() {
        int[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(4, N.indexOf(arr, 3, 3));
        Assertions.assertEquals(-1, N.indexOf(arr, 3, 10));
    }

    @Test
    public void testIndexOf_longArray() {
        long[] arr = { 1L, 2L, 3L, 4L, 5L };
        Assertions.assertEquals(2, N.indexOf(arr, 3L));
        Assertions.assertEquals(-1, N.indexOf(arr, 10L));

        long[] empty = {};
        Assertions.assertEquals(-1, N.indexOf(empty, 3L));

        Assertions.assertEquals(-1, N.indexOf((long[]) null, 3L));
    }

    @Test
    public void testIndexOf_longArray_withFromIndex() {
        long[] arr = { 1L, 2L, 3L, 4L, 3L };
        Assertions.assertEquals(4, N.indexOf(arr, 3L, 3));
        Assertions.assertEquals(-1, N.indexOf(arr, 3L, 10));
    }

    @Test
    public void testIndexOf_floatArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        Assertions.assertEquals(2, N.indexOf(arr, 3.0f));
        Assertions.assertEquals(-1, N.indexOf(arr, 10.0f));

        float[] empty = {};
        Assertions.assertEquals(-1, N.indexOf(empty, 3.0f));

        Assertions.assertEquals(-1, N.indexOf((float[]) null, 3.0f));
    }

    @Test
    public void testIndexOf_floatArray_withFromIndex() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 3.0f };
        Assertions.assertEquals(4, N.indexOf(arr, 3.0f, 3));
        Assertions.assertEquals(-1, N.indexOf(arr, 3.0f, 10));
    }

    @Test
    public void testIndexOf_doubleArray() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        Assertions.assertEquals(2, N.indexOf(arr, 3.0));
        Assertions.assertEquals(-1, N.indexOf(arr, 10.0));

        double[] empty = {};
        Assertions.assertEquals(-1, N.indexOf(empty, 3.0));

        Assertions.assertEquals(-1, N.indexOf((double[]) null, 3.0));
    }

    @Test
    public void testIndexOf_doubleArray_withFromIndex() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 3.0 };
        Assertions.assertEquals(4, N.indexOf(arr, 3.0, 3));
        Assertions.assertEquals(-1, N.indexOf(arr, 3.0, 10));
    }

    @Test
    public void testIndexOf_doubleArray_withTolerance() {
        double[] arr = { 1.0, 2.0, 3.001, 4.0, 5.0 };
        Assertions.assertEquals(2, N.indexOf(arr, 3.0, 0.01));
        Assertions.assertEquals(-1, N.indexOf(arr, 3.0, 0.0001));

        double[] empty = {};
        Assertions.assertEquals(-1, N.indexOf(empty, 3.0, 0.01));

        Assertions.assertEquals(-1, N.indexOf((double[]) null, 3.0, 0.01));
    }

    @Test
    public void testIndexOf_doubleArray_withToleranceAndFromIndex() {
        double[] arr = { 1.0, 2.0, 3.001, 4.0, 3.002 };
        Assertions.assertEquals(4, N.indexOf(arr, 3.0, 0.01, 3));
        Assertions.assertEquals(-1, N.indexOf(arr, 3.0, 0.0001, 3));
    }

    @Test
    public void testIndexOf_objectArray() {
        String[] arr = { "a", "b", "c", "d", "e" };
        Assertions.assertEquals(2, N.indexOf(arr, "c"));
        Assertions.assertEquals(-1, N.indexOf(arr, "z"));

        String[] arrWithNull = { "a", null, "c" };
        Assertions.assertEquals(1, N.indexOf(arrWithNull, null));

        String[] empty = {};
        Assertions.assertEquals(-1, N.indexOf(empty, "c"));

        Assertions.assertEquals(-1, N.indexOf((String[]) null, "c"));
    }

    @Test
    public void testIndexOf_objectArray_withFromIndex() {
        String[] arr = { "a", "b", "c", "d", "c" };
        Assertions.assertEquals(4, N.indexOf(arr, "c", 3));
        Assertions.assertEquals(-1, N.indexOf(arr, "c", 10));
    }

    @Test
    public void testIndexOf_collection() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Assertions.assertEquals(2, N.indexOf(list, "c"));
        Assertions.assertEquals(-1, N.indexOf(list, "z"));

        List<String> empty = new ArrayList<>();
        Assertions.assertEquals(-1, N.indexOf(empty, "c"));

        Assertions.assertEquals(-1, N.indexOf((Collection<?>) null, "c"));
    }

    @Test
    public void testIndexOf_collection_withFromIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "c");
        Assertions.assertEquals(4, N.indexOf(list, "c", 3));
        Assertions.assertEquals(-1, N.indexOf(list, "c", 10));

        LinkedList<String> linkedList = new LinkedList<>(list);
        Assertions.assertEquals(4, N.indexOf(linkedList, "c", 3));
    }

    @Test
    public void testIndexOf_iterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Assertions.assertEquals(2, N.indexOf(list.iterator(), "c"));
        Assertions.assertEquals(-1, N.indexOf(list.iterator(), "z"));

        Assertions.assertEquals(-1, N.indexOf((Iterator<?>) null, "c"));
    }

    @Test
    public void testIndexOf_iterator_withFromIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "c");
        Assertions.assertEquals(4, N.indexOf(list.iterator(), "c", 3));
        Assertions.assertEquals(-1, N.indexOf(list.iterator(), "c", 10));
    }

    @Test
    public void testWithSpecialFloatingPointValues() {
        double[] arrWithNaN = { 1.0, Double.NaN, 3.0, Double.NaN };
        Assertions.assertEquals(1, N.indexOf(arrWithNaN, Double.NaN));
        Assertions.assertEquals(3, N.lastIndexOf(arrWithNaN, Double.NaN));

        double[] arrWithInf = { 1.0, Double.POSITIVE_INFINITY, 3.0, Double.NEGATIVE_INFINITY };
        Assertions.assertEquals(1, N.indexOf(arrWithInf, Double.POSITIVE_INFINITY));
        Assertions.assertEquals(3, N.indexOf(arrWithInf, Double.NEGATIVE_INFINITY));
    }

    // Tests for indexOf(Collection, Object, fromIndex)
    @Test
    public void testIndexOf_Collection_WithFromIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "b", "d");
        assertEquals(3, N.indexOf(list, "b", 2));
        assertEquals(1, N.indexOf(list, "b", 1));
        assertEquals(-1, N.indexOf(list, "b", 4));
        assertEquals(-1, N.indexOf(list, "z", 0));
    }

    @Test
    public void testIndexOf_Collection_WithFromIndex_LinkedList() {
        // LinkedList is not RandomAccess - hits the iterator path
        java.util.LinkedList<String> list = new java.util.LinkedList<>(Arrays.asList("a", "b", "c", "b", "d"));
        assertEquals(3, N.indexOf(list, "b", 2));
        assertEquals(1, N.indexOf(list, "b", 1));
        assertEquals(-1, N.indexOf(list, "b", 5));
    }

    @Test
    public void testIndexOfSubList_withFromIndex() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d");
        List<String> sub = Arrays.asList("c", "d");
        Assertions.assertEquals(4, N.indexOfSubList(source, sub, 3));
        Assertions.assertEquals(-1, N.indexOfSubList(source, sub, 10));
    }

    @Test
    public void testIndexOfSubList() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> subList = Arrays.asList(3, 4);
        assertEquals(2, N.indexOfSubList(list, subList));
        assertEquals(0, Strings.indexOf("", ""));
        assertEquals(0, "".indexOf(""));
        assertEquals(0, Collections.indexOfSubList(N.emptyList(), N.emptyList()));
        assertEquals(0, N.indexOfSubList(N.emptyList(), N.emptyList()));
        assertEquals(0, Index.ofSubList(N.emptyList(), N.emptyList()).orElseThrow());
        assertEquals(0, Index.ofSubList(N.emptyList(), 0, N.emptyList()).orElseThrow());
        assertEquals(Collections.indexOfSubList(N.emptyList(), N.emptyList()), N.indexOfSubList(N.emptyList(), N.emptyList()));
    }

    @Test
    public void testIndexOfIgnoreCase() {
        String[] arr = { "Apple", "Banana", "Cherry" };
        assertEquals(1, N.indexOfIgnoreCase(arr, "banana"));
        assertEquals(-1, N.indexOfIgnoreCase(arr, "grape"));
    }

    @Test
    public void testIndexOfIgnoreCase_withFromIndex() {
        String[] arr = { "A", "B", "C", "D", "c" };
        Assertions.assertEquals(4, N.indexOfIgnoreCase(arr, "C", 3));
        Assertions.assertEquals(-1, N.indexOfIgnoreCase(arr, "C", 10));
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
    public void testLastIndexOf_booleanArray_withStartIndex() {
        boolean[] arr = { true, false, true, false, true };
        Assertions.assertEquals(2, N.lastIndexOf(arr, true, 3));
        Assertions.assertEquals(1, N.lastIndexOf(arr, false, 2));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, true, -1));
    }

    @Test
    public void testLastIndexOf_charArray_withStartIndex() {
        char[] arr = { 'a', 'b', 'c', 'd', 'c' };
        Assertions.assertEquals(2, N.lastIndexOf(arr, 'c', 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 'c', -1));
    }

    @Test
    public void testLastIndexOf_byteArray_withStartIndex() {
        byte[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(2, N.lastIndexOf(arr, (byte) 3, 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, (byte) 3, -1));
    }

    @Test
    public void testLastIndexOf_shortArray_withStartIndex() {
        short[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(2, N.lastIndexOf(arr, (short) 3, 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, (short) 3, -1));
    }

    @Test
    public void testLastIndexOf_intArray_withStartIndex() {
        int[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(2, N.lastIndexOf(arr, 3, 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 3, -1));
    }

    @Test
    public void testLastIndexOf_longArray_withStartIndex() {
        long[] arr = { 1L, 2L, 3L, 4L, 3L };
        Assertions.assertEquals(2, N.lastIndexOf(arr, 3L, 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 3L, -1));
    }

    @Test
    public void testLastIndexOf_floatArray_withStartIndex() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 3.0f };
        Assertions.assertEquals(2, N.lastIndexOf(arr, 3.0f, 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 3.0f, -1));
    }

    @Test
    public void testLastIndexOf_doubleArray_withStartIndex() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 3.0 };
        Assertions.assertEquals(2, N.lastIndexOf(arr, 3.0, 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 3.0, -1));
    }

    @Test
    public void testLastIndexOf_doubleArray_withToleranceAndStartIndex() {
        double[] arr = { 1.0, 2.0, 3.001, 4.0, 3.002 };
        Assertions.assertEquals(2, N.lastIndexOf(arr, 3.0, 0.01, 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 3.0, 0.0001, 3));
    }

    @Test
    public void testLastIndexOf_objectArray_withStartIndex() {
        String[] arr = { "a", "b", "c", "d", "c" };
        Assertions.assertEquals(2, N.lastIndexOf(arr, "c", 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, "c", -1));
    }

    @Test
    public void testLastIndexOf_collection_withStartIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "c");
        Assertions.assertEquals(2, N.lastIndexOf(list, "c", 3));
        Assertions.assertEquals(-1, N.lastIndexOf(list, "c", -1));

        LinkedList<String> linkedList = new LinkedList<>(list);
        Assertions.assertEquals(2, N.lastIndexOf(linkedList, "c", 3));
    }

    // Tests for lastIndexOf with startIndexFromBack parameter
    @Test
    public void testLastIndexOf_Collection_WithStartIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "b", "d");
        assertEquals(3, N.lastIndexOf(list, "b", 4));
        assertEquals(1, N.lastIndexOf(list, "b", 2));
        assertEquals(-1, N.lastIndexOf(list, "b", 0));
        assertEquals(-1, N.lastIndexOf(list, "z", 4));
        assertEquals(-1, N.lastIndexOf(list, "a", -1));
    }

    @Test
    public void testLastIndexOf_BooleanArray() {
        boolean[] arr = { true, false, true, false };
        assertEquals(2, N.lastIndexOf(arr, true));
        assertEquals(3, N.lastIndexOf(arr, false));
        assertEquals(-1, N.lastIndexOf((boolean[]) null, true));
    }

    @Test
    public void testLastIndexOf_booleanArray() {
        boolean[] arr = { true, false, true, false, true };
        Assertions.assertEquals(4, N.lastIndexOf(arr, true));
        Assertions.assertEquals(3, N.lastIndexOf(arr, false));

        boolean[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOf(empty, true));

        Assertions.assertEquals(-1, N.lastIndexOf((boolean[]) null, true));
    }

    @Test
    public void testLastIndexOf_charArray() {
        char[] arr = { 'a', 'b', 'c', 'd', 'c' };
        Assertions.assertEquals(4, N.lastIndexOf(arr, 'c'));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 'z'));

        char[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOf(empty, 'c'));

        Assertions.assertEquals(-1, N.lastIndexOf((char[]) null, 'c'));
    }

    @Test
    public void testLastIndexOf_byteArray() {
        byte[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(4, N.lastIndexOf(arr, (byte) 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, (byte) 10));

        byte[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOf(empty, (byte) 3));

        Assertions.assertEquals(-1, N.lastIndexOf((byte[]) null, (byte) 3));
    }

    @Test
    public void testLastIndexOf_shortArray() {
        short[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(4, N.lastIndexOf(arr, (short) 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, (short) 10));

        short[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOf(empty, (short) 3));

        Assertions.assertEquals(-1, N.lastIndexOf((short[]) null, (short) 3));
    }

    @Test
    public void testLastIndexOf_intArray() {
        int[] arr = { 1, 2, 3, 4, 3 };
        Assertions.assertEquals(4, N.lastIndexOf(arr, 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 10));

        int[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOf(empty, 3));

        Assertions.assertEquals(-1, N.lastIndexOf((int[]) null, 3));
    }

    @Test
    public void testLastIndexOf_longArray() {
        long[] arr = { 1L, 2L, 3L, 4L, 3L };
        Assertions.assertEquals(4, N.lastIndexOf(arr, 3L));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 10L));

        long[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOf(empty, 3L));

        Assertions.assertEquals(-1, N.lastIndexOf((long[]) null, 3L));
    }

    @Test
    public void testLastIndexOf_floatArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 3.0f };
        Assertions.assertEquals(4, N.lastIndexOf(arr, 3.0f));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 10.0f));

        float[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOf(empty, 3.0f));

        Assertions.assertEquals(-1, N.lastIndexOf((float[]) null, 3.0f));
    }

    @Test
    public void testLastIndexOf_doubleArray() {
        double[] arr = { 1.0, 2.0, 3.0, 4.0, 3.0 };
        Assertions.assertEquals(4, N.lastIndexOf(arr, 3.0));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 10.0));

        double[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOf(empty, 3.0));

        Assertions.assertEquals(-1, N.lastIndexOf((double[]) null, 3.0));
    }

    @Test
    public void testLastIndexOf_doubleArray_withTolerance() {
        double[] arr = { 1.0, 2.0, 3.001, 4.0, 3.002 };
        Assertions.assertEquals(4, N.lastIndexOf(arr, 3.0, 0.01));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 3.0, 0.0001));

        double[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOf(empty, 3.0, 0.01));

        Assertions.assertEquals(-1, N.lastIndexOf((double[]) null, 3.0, 0.01));
    }

    @Test
    public void testLastIndexOf_objectArray() {
        String[] arr = { "a", "b", "c", "d", "c" };
        Assertions.assertEquals(4, N.lastIndexOf(arr, "c"));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, "z"));

        String[] arrWithNull = { "a", null, "c", null };
        Assertions.assertEquals(3, N.lastIndexOf(arrWithNull, null));

        String[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOf(empty, "c"));

        Assertions.assertEquals(-1, N.lastIndexOf((String[]) null, "c"));
    }

    @Test
    public void testLastIndexOf_collection() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "c");
        Assertions.assertEquals(4, N.lastIndexOf(list, "c"));
        Assertions.assertEquals(-1, N.lastIndexOf(list, "z"));

        List<String> empty = new ArrayList<>();
        Assertions.assertEquals(-1, N.lastIndexOf(empty, "c"));

        Assertions.assertEquals(-1, N.lastIndexOf((Collection<?>) null, "c"));
    }

    @Test
    public void testLastIndexOf_Collection_WithStartIndex_EmptyCollection() {
        assertEquals(-1, N.lastIndexOf(new ArrayList<>(), "a", 0));
    }

    @Test
    public void testLastIndexOfSubList_withStartIndex() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d");
        List<String> sub = Arrays.asList("c", "d");
        Assertions.assertEquals(2, N.lastIndexOfSubList(source, sub, 3));
        Assertions.assertEquals(-1, N.lastIndexOfSubList(source, sub, 1));
    }

    @Test
    public void testLastIndexOfSubList() {
        List<Integer> list = Arrays.asList(1, 2, 3, 2, 3, 4);
        List<Integer> subList = Arrays.asList(2, 3);
        assertEquals(3, N.lastIndexOfSubList(list, subList));

        assertEquals(0, Strings.lastIndexOf("", ""));
        assertEquals(0, "".lastIndexOf(""));
        assertEquals(0, Collections.lastIndexOfSubList(N.emptyList(), N.emptyList()));
        assertEquals(0, N.lastIndexOfSubList(N.emptyList(), N.emptyList()));
        assertEquals(0, Index.lastOfSubList(N.emptyList(), N.emptyList()).orElseThrow());
        assertEquals(0, Index.lastOfSubList(N.emptyList(), 0, N.emptyList()).orElseThrow());
        assertEquals(Collections.lastIndexOfSubList(N.emptyList(), N.emptyList()), N.lastIndexOfSubList(N.emptyList(), N.emptyList()));
    }

    @Test
    public void testLastIndexOfIgnoreCase() {
        String[] arr = { "Apple", "Banana", "apple" };
        assertEquals(2, N.lastIndexOfIgnoreCase(arr, "APPLE"));
        assertEquals(-1, N.lastIndexOfIgnoreCase(arr, "grape"));
    }

    @Test
    public void testLastIndexOfIgnoreCase_withStartIndex() {
        String[] arr = { "A", "B", "C", "D", "c" };
        Assertions.assertEquals(2, N.lastIndexOfIgnoreCase(arr, "C", 3));
        Assertions.assertEquals(-1, N.lastIndexOfIgnoreCase(arr, "C", -1));
    }

    @Test
    public void testFindFirstIndex() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        com.landawn.abacus.util.u.OptionalInt index = N.findFirstIndex(list, x -> x > 3);
        assertTrue(index.isPresent());
        assertEquals(3, index.getAsInt());
    }

    @Test
    public void testFindFirstIndex_array() {
        String[] arr = { "a", "b", "c", "d", "e" };
        OptionalInt result = N.findFirstIndex(arr, s -> s.equals("c"));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.getAsInt());

        OptionalInt notFound = N.findFirstIndex(arr, s -> s.equals("z"));
        Assertions.assertFalse(notFound.isPresent());

        String[] empty = {};
        OptionalInt emptyResult = N.findFirstIndex(empty, s -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testFindFirstIndex_array_withBiPredicate() {
        String[] arr = { "a", "b", "c", "d", "e" };
        String prefix = "c";
        OptionalInt result = N.findFirstIndex(arr, prefix, (s, p) -> s.startsWith(p));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.getAsInt());

        String[] empty = {};
        OptionalInt emptyResult = N.findFirstIndex(empty, prefix, (s, p) -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testFindFirstIndex_collection() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        OptionalInt result = N.findFirstIndex(list, s -> s.equals("c"));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.getAsInt());

        OptionalInt notFound = N.findFirstIndex(list, s -> s.equals("z"));
        Assertions.assertFalse(notFound.isPresent());

        List<String> empty = new ArrayList<>();
        OptionalInt emptyResult = N.findFirstIndex(empty, s -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testFindFirstIndex_collection_withBiPredicate() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        String prefix = "c";
        OptionalInt result = N.findFirstIndex(list, prefix, (s, p) -> s.startsWith(p));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.getAsInt());

        List<String> empty = new ArrayList<>();
        OptionalInt emptyResult = N.findFirstIndex(empty, prefix, (s, p) -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testFindLastIndex() {
        List<Integer> list = Arrays.asList(1, 2, 3, 2, 1);
        com.landawn.abacus.util.u.OptionalInt index = N.findLastIndex(list, x -> x == 2);
        assertTrue(index.isPresent());
        assertEquals(3, index.getAsInt());
    }

    // ========== findLastIndex(Collection, Predicate) - non-RandomAccess and Deque paths ==========

    @Test
    public void testFindLastIndex_Collection_LinkedList() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("a", "b", "c", "b", "a"));
        OptionalInt result = N.findLastIndex(list, s -> s.equals("b"));
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void testFindLastIndex_Collection_LinkedList_NoMatch() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("a", "b", "c"));
        OptionalInt result = N.findLastIndex(list, s -> s.equals("z"));
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindLastIndex_array() {
        String[] arr = { "a", "b", "c", "d", "c" };
        OptionalInt result = N.findLastIndex(arr, s -> s.equals("c"));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.getAsInt());

        OptionalInt notFound = N.findLastIndex(arr, s -> s.equals("z"));
        Assertions.assertFalse(notFound.isPresent());

        String[] empty = {};
        OptionalInt emptyResult = N.findLastIndex(empty, s -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testFindLastIndex_array_withBiPredicate() {
        String[] arr = { "a", "b", "c", "d", "ca" };
        String prefix = "c";
        OptionalInt result = N.findLastIndex(arr, prefix, (s, p) -> s.startsWith(p));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.getAsInt());

        String[] empty = {};
        OptionalInt emptyResult = N.findLastIndex(empty, prefix, (s, p) -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testFindLastIndex_collection() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "c");
        OptionalInt result = N.findLastIndex(list, s -> s.equals("c"));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.getAsInt());

        OptionalInt notFound = N.findLastIndex(list, s -> s.equals("z"));
        Assertions.assertFalse(notFound.isPresent());

        List<String> empty = new ArrayList<>();
        OptionalInt emptyResult = N.findLastIndex(empty, s -> true);
        Assertions.assertFalse(emptyResult.isPresent());

        LinkedList<String> linkedList = new LinkedList<>(list);
        OptionalInt linkedResult = N.findLastIndex(linkedList, s -> s.equals("c"));
        Assertions.assertTrue(linkedResult.isPresent());
        Assertions.assertEquals(4, linkedResult.getAsInt());
    }

    @Test
    public void testFindLastIndex_collection_withBiPredicate() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "ca");
        String prefix = "c";
        OptionalInt result = N.findLastIndex(list, prefix, (s, p) -> s.startsWith(p));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.getAsInt());

        List<String> empty = new ArrayList<>();
        OptionalInt emptyResult = N.findLastIndex(empty, prefix, (s, p) -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testEdgeCases() {
        Integer[] allSame = { 5, 5, 5, 5, 5 };
        int[] minIndices = N.indicesOfMin(allSame);
        Assertions.assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, minIndices);
        int[] maxIndices = N.indicesOfMax(allSame);
        Assertions.assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, maxIndices);

        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals(-1, N.indexOf(arr, "a", Integer.MAX_VALUE));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, "a", Integer.MIN_VALUE));
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
    public void testIndicesOfAll_collection_withStartIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "b", "d", "b");
        int[] indices = N.indicesOfAll(list, "b", 2);
        Assertions.assertArrayEquals(new int[] { 3, 5 }, indices);

        int[] beyondIndices = N.indicesOfAll(list, "b", 10);
        Assertions.assertArrayEquals(new int[] {}, beyondIndices);
    }

    @Test
    public void testIndicesOfAll_array_withPredicateAndStartIndex() {
        String[] arr = { "apple", "banana", "apricot", "cherry", "avocado" };
        int[] indices = N.indicesOfAll(arr, s -> s.startsWith("a"), 1);
        Assertions.assertArrayEquals(new int[] { 2, 4 }, indices);

        int[] beyondIndices = N.indicesOfAll(arr, s -> s.startsWith("a"), 10);
        Assertions.assertArrayEquals(new int[] {}, beyondIndices);
    }

    @Test
    public void testIndicesOfAllMax() {
        List<Integer> list = Arrays.asList(1, 5, 3, 5, 2);
        int[] indices = N.indicesOfMax(list);
        assertEquals(2, indices.length);
        assertEquals(1, indices[0]);
        assertEquals(3, indices[1]);
    }

    @Test
    public void testIndicesOfAllMin() {
        List<Integer> list = Arrays.asList(5, 1, 3, 1, 2);
        int[] indices = N.indicesOfMin(list);
        assertEquals(2, indices.length);
        assertEquals(1, indices[0]);
        assertEquals(3, indices[1]);
    }

    @Test
    public void testIndicesOfAllMin_array() {
        Integer[] arr = { 3, 1, 4, 1, 5, 1 };
        int[] indices = N.indicesOfMin(arr);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        Integer[] single = { 5 };
        int[] singleIndices = N.indicesOfMin(single);
        Assertions.assertArrayEquals(new int[] { 0 }, singleIndices);

        Integer[] empty = {};
        int[] emptyIndices = N.indicesOfMin(empty);
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);
    }

    @Test
    public void testIndicesOfAllMin_array_withComparator() {
        String[] arr = { "cat", "a", "dog", "a", "bird", "a" };
        Comparator<String> cmp = Comparator.comparing(String::length);
        int[] indices = N.indicesOfMin(arr, cmp);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        String[] arrWithNull = { null, "a", "b", null };
        int[] nullIndices = N.indicesOfMin(arrWithNull, null);
        Assertions.assertArrayEquals(new int[] { 1 }, nullIndices);
    }

    @Test
    public void testIndicesOfAllMin_collection() {
        List<Integer> list = Arrays.asList(3, 1, 4, 1, 5, 1);
        int[] indices = N.indicesOfMin(list);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        List<Integer> empty = new ArrayList<>();
        int[] emptyIndices = N.indicesOfMin(empty);
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);
    }

    @Test
    public void testIndicesOfAllMin_collection_withComparator() {
        List<String> list = Arrays.asList("cat", "a", "dog", "a", "bird", "a");
        Comparator<String> cmp = Comparator.comparing(String::length);
        int[] indices = N.indicesOfMin(list, cmp);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);
    }

    @Test
    public void testIndicesOfAllMax_array() {
        Integer[] arr = { 3, 5, 4, 5, 1, 5 };
        int[] indices = N.indicesOfMax(arr);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        Integer[] single = { 5 };
        int[] singleIndices = N.indicesOfMax(single);
        Assertions.assertArrayEquals(new int[] { 0 }, singleIndices);

        Integer[] empty = {};
        int[] emptyIndices = N.indicesOfMax(empty);
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);
    }

    @Test
    public void testIndicesOfAllMax_array_withComparator() {
        String[] arr = { "a", "cat", "b", "dog", "c", "dog" };
        Comparator<String> cmp = Comparator.comparing(String::length);
        int[] indices = N.indicesOfMax(arr, cmp);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        String[] arrWithNull = { "a", null, "b", null };
        int[] nullIndices = N.indicesOfMax(arrWithNull, null);
        Assertions.assertArrayEquals(new int[] { 2 }, nullIndices);
    }

    @Test
    public void testIndicesOfAllMax_collection() {
        List<Integer> list = Arrays.asList(3, 5, 4, 5, 1, 5);
        int[] indices = N.indicesOfMax(list);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        List<Integer> empty = new ArrayList<>();
        int[] emptyIndices = N.indicesOfMax(empty);
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);
    }

    @Test
    public void testIndicesOfAllMax_collection_withComparator() {
        List<String> list = Arrays.asList("a", "cat", "b", "dog", "c", "dog");
        Comparator<String> cmp = Comparator.comparing(String::length);
        int[] indices = N.indicesOfMax(list, cmp);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);
    }

    @Test
    public void testIndicesOfAll_objectArray() {
        String[] arr = { "a", "b", "c", "b", "d", "b" };
        int[] indices = N.indicesOfAll(arr, "b");
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        int[] notFound = N.indicesOfAll(arr, "z");
        Assertions.assertArrayEquals(new int[] {}, notFound);

        String[] arrWithNull = { "a", null, "b", null };
        int[] nullIndices = N.indicesOfAll(arrWithNull, (String) null);
        Assertions.assertArrayEquals(new int[] { 1, 3 }, nullIndices);

        String[] empty = {};
        int[] emptyIndices = N.indicesOfAll(empty, "a");
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);
    }

    @Test
    public void testIndicesOfAll_objectArray_withStartIndex() {
        String[] arr = { "a", "b", "c", "b", "d", "b" };
        int[] indices = N.indicesOfAll(arr, "b", 2);
        Assertions.assertArrayEquals(new int[] { 3, 5 }, indices);

        int[] beyondIndices = N.indicesOfAll(arr, "b", 10);
        Assertions.assertArrayEquals(new int[] {}, beyondIndices);

        int[] negativeIndices = N.indicesOfAll(arr, "b", -1);
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, negativeIndices);
    }

    @Test
    public void testIndicesOfAll_collection() {
        List<String> list = Arrays.asList("a", "b", "c", "b", "d", "b");
        int[] indices = N.indicesOfAll(list, "b");
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, indices);

        List<String> empty = new ArrayList<>();
        int[] emptyIndices = N.indicesOfAll(empty, "a");
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);

        LinkedList<String> linkedList = new LinkedList<>(list);
        int[] linkedIndices = N.indicesOfAll(linkedList, "b");
        Assertions.assertArrayEquals(new int[] { 1, 3, 5 }, linkedIndices);
    }

    @Test
    public void testIndicesOfAll_array_withPredicate() {
        String[] arr = { "apple", "banana", "apricot", "cherry", "avocado" };
        int[] indices = N.indicesOfAll(arr, s -> s.startsWith("a"));
        Assertions.assertArrayEquals(new int[] { 0, 2, 4 }, indices);

        String[] empty = {};
        int[] emptyIndices = N.indicesOfAll(empty, s -> true);
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);
    }

    @Test
    public void testIndicesOfAll_collection_withPredicate() {
        List<String> list = Arrays.asList("apple", "banana", "apricot", "cherry", "avocado");
        int[] indices = N.indicesOfAll(list, s -> s.startsWith("a"));
        Assertions.assertArrayEquals(new int[] { 0, 2, 4 }, indices);

        List<String> empty = new ArrayList<>();
        int[] emptyIndices = N.indicesOfAll(empty, s -> true);
        Assertions.assertArrayEquals(new int[] {}, emptyIndices);

        LinkedList<String> linkedList = new LinkedList<>(list);
        int[] linkedIndices = N.indicesOfAll(linkedList, s -> s.startsWith("a"));
        Assertions.assertArrayEquals(new int[] { 0, 2, 4 }, linkedIndices);
    }

    @Test
    public void testIndicesOfAll_collection_withPredicateAndFromIndex() {
        List<String> list = Arrays.asList("apple", "banana", "apricot", "cherry", "avocado");
        int[] indices = N.indicesOfAll(list, s -> s.startsWith("a"), 1);
        Assertions.assertArrayEquals(new int[] { 2, 4 }, indices);

        int[] beyondIndices = N.indicesOfAll(list, s -> s.startsWith("a"), 10);
        Assertions.assertArrayEquals(new int[] {}, beyondIndices);
    }

    // Tests for indicesOfAll(Collection, Object, fromIndex)
    @Test
    public void testIndicesOfAll_Collection_WithFromIndex() {
        List<Integer> list = Arrays.asList(1, 2, 3, 2, 5, 2);
        int[] indices = N.indicesOfAll(list, 2, 2);
        assertEquals(2, indices.length);
        assertEquals(3, indices[0]);
        assertEquals(5, indices[1]);
    }

    @Test
    public void testIndicesOfAll_Collection_WithFromIndex_NoMatch() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4);
        int[] indices = N.indicesOfAll(list, 9, 0);
        assertEquals(0, indices.length);
    }

    @Test
    public void testIndicesOfAll_Collection_WithFromIndex_LinkedList() {
        java.util.LinkedList<String> list = new java.util.LinkedList<>(Arrays.asList("a", "b", "a", "c", "a"));
        int[] indices = N.indicesOfAll(list, "a", 1);
        assertEquals(2, indices.length);
        assertEquals(2, indices[0]);
        assertEquals(4, indices[1]);
    }

    // Tests for indicesOfAll(Collection, Predicate, fromIndex)
    @Test
    public void testIndicesOfAll_Collection_Predicate_WithFromIndex() {
        List<Integer> list = Arrays.asList(1, 4, 3, 6, 5, 8);
        int[] indices = N.indicesOfAll(list, n -> n % 2 == 0, 2);
        assertEquals(2, indices.length);
        assertEquals(3, indices[0]);
        assertEquals(5, indices[1]);
    }

    @Test
    public void testIndicesOfAll_Collection_Predicate_WithFromIndex_LinkedList() {
        java.util.LinkedList<String> list = new java.util.LinkedList<>(Arrays.asList("ab", "cd", "ef", "gh"));
        int[] indices = N.indicesOfAll(list, s -> s.startsWith("e") || s.startsWith("g"), 1);
        assertEquals(2, indices.length);
        assertEquals(2, indices[0]);
        assertEquals(3, indices[1]);
    }

    @Test
    public void testIndicesOfAll_Collection_Predicate_WithFromIndex_NoMatch() {
        List<Integer> list = Arrays.asList(1, 3, 5, 7);
        int[] indices = N.indicesOfAll(list, n -> n % 2 == 0, 0);
        assertEquals(0, indices.length);
    }

    @Test
    public void testGetDescendingIteratorIfPossible() {
        Deque<String> deque = new ArrayDeque<>(Arrays.asList("a", "b", "c"));
        Iterator<String> descIter = N.getDescendingIteratorIfPossible(deque);
        Assertions.assertNotNull(descIter);

        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Iterator<String> listIter = N.getDescendingIteratorIfPossible(list);
        Assertions.assertNull(listIter);
    }

    @Test
    public void testCreateMask() {
        Runnable mask = N.createMask(Runnable.class);
        Assertions.assertNotNull(mask);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> mask.run());

        Comparator<?> compMask = N.createMask(Comparator.class);
        Assertions.assertNotNull(compMask);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> compMask.compare(null, null));
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
    public void testRegisterConverter_CustomClass() {
        Runnable source = (Runnable) java.lang.reflect.Proxy.newProxyInstance(Runnable.class.getClassLoader(), new Class<?>[] { Runnable.class },
                (proxy, method, args) -> null);

        boolean registered = N.registerConverter(source.getClass(), (obj, targetClass) -> {
            if (targetClass == Integer.class) {
                return Integer.valueOf(obj.getClass().getName().contains("Proxy") ? 7 : -1);
            }

            if (targetClass == String.class) {
                return "converted:" + (obj.getClass().getName().contains("Proxy") ? "7" : "x");
            }

            return null;
        });

        assertTrue(registered);
        assertTrue(source.getClass().getName().contains("Proxy"));
        assertEquals(7, N.convert(source, Integer.class).intValue());
        assertEquals("converted:7", N.convert(source, String.class));
        assertFalse(N.registerConverter(source.getClass(), (obj, targetClass) -> obj));
    }

    @Test
    @DisplayName("compareUnsigned ranged: zero-length range returns 0 even when one side is empty (regression)")
    public void testCompareUnsignedRanged_ZeroLength() {
        // byte[] - zero-length range should return 0 regardless of source array contents (consistent with Arrays.compareUnsigned)
        assertEquals(0, N.compareUnsigned(new byte[0], 0, new byte[] { 1, 2, 3 }, 0, 0));
        assertEquals(0, N.compareUnsigned(new byte[] { 1, 2, 3 }, 0, new byte[0], 0, 0));
        assertEquals(0, N.compareUnsigned(new byte[] { 1, 2, 3 }, 1, new byte[] { 4, 5, 6 }, 1, 0));

        // short[]
        assertEquals(0, N.compareUnsigned(new short[0], 0, new short[] { 1, 2, 3 }, 0, 0));
        assertEquals(0, N.compareUnsigned(new short[] { 1, 2, 3 }, 0, new short[0], 0, 0));

        // int[]
        assertEquals(0, N.compareUnsigned(new int[0], 0, new int[] { 1, 2, 3 }, 0, 0));
        assertEquals(0, N.compareUnsigned(new int[] { 1, 2, 3 }, 0, new int[0], 0, 0));

        // long[]
        assertEquals(0, N.compareUnsigned(new long[0], 0, new long[] { 1L, 2L, 3L }, 0, 0));
        assertEquals(0, N.compareUnsigned(new long[] { 1L, 2L, 3L }, 0, new long[0], 0, 0));
    }

    @Test
    @DisplayName("compareUnsigned ranged: actual unsigned comparison")
    public void testCompareUnsignedRanged_NonZero() {
        // -1 as unsigned is the largest byte, so should be > 1 unsigned
        assertTrue(N.compareUnsigned(new byte[] { (byte) -1 }, 0, new byte[] { (byte) 1 }, 0, 1) > 0);
        // Equal
        assertEquals(0, N.compareUnsigned(new byte[] { 1, 2, 3 }, 0, new byte[] { 1, 2, 3 }, 0, 3));
        // Same array, same offset, len > 0
        final byte[] arr = { 1, 2, 3 };
        assertEquals(0, N.compareUnsigned(arr, 0, arr, 0, 3));

        // int[] unsigned: -1 (0xFFFFFFFF) > 1 unsigned
        assertTrue(N.compareUnsigned(new int[] { -1 }, 0, new int[] { 1 }, 0, 1) > 0);

        // long[] unsigned: -1L > 1L unsigned
        assertTrue(N.compareUnsigned(new long[] { -1L }, 0, new long[] { 1L }, 0, 1) > 0);
    }

    // ==================== Additional coverage tests (appended) ====================

    // Package-private toString(StringBuilder, <primitive>[]) — null and empty branches for short/int/long/float/double.
    @Test
    public void testToString_StringBuilder_ShortArray_Null_uncovered() {
        StringBuilder sb = new StringBuilder();
        CommonUtil.toString(sb, (short[]) null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testToString_StringBuilder_ShortArray_Empty_uncovered() {
        StringBuilder sb = new StringBuilder();
        CommonUtil.toString(sb, new short[0]);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testToString_StringBuilder_ShortArray_NonEmpty_uncovered() {
        StringBuilder sb = new StringBuilder();
        CommonUtil.toString(sb, new short[] { 1, 2, 3 });
        assertEquals("[1, 2, 3]", sb.toString());
    }

    @Test
    public void testToString_StringBuilder_IntArray_Null_uncovered() {
        StringBuilder sb = new StringBuilder();
        CommonUtil.toString(sb, (int[]) null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testToString_StringBuilder_IntArray_Empty_uncovered() {
        StringBuilder sb = new StringBuilder();
        CommonUtil.toString(sb, new int[0]);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testToString_StringBuilder_IntArray_NonEmpty_uncovered() {
        StringBuilder sb = new StringBuilder();
        CommonUtil.toString(sb, new int[] { 10, 20 });
        assertEquals("[10, 20]", sb.toString());
    }

    @Test
    public void testToString_StringBuilder_LongArray_Null_uncovered() {
        StringBuilder sb = new StringBuilder();
        CommonUtil.toString(sb, (long[]) null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testToString_StringBuilder_LongArray_Empty_uncovered() {
        StringBuilder sb = new StringBuilder();
        CommonUtil.toString(sb, new long[0]);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testToString_StringBuilder_LongArray_NonEmpty_uncovered() {
        StringBuilder sb = new StringBuilder();
        CommonUtil.toString(sb, new long[] { 100L, 200L });
        assertEquals("[100, 200]", sb.toString());
    }

    @Test
    public void testToString_StringBuilder_FloatArray_Null_uncovered() {
        StringBuilder sb = new StringBuilder();
        CommonUtil.toString(sb, (float[]) null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testToString_StringBuilder_FloatArray_Empty_uncovered() {
        StringBuilder sb = new StringBuilder();
        CommonUtil.toString(sb, new float[0]);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testToString_StringBuilder_FloatArray_NonEmpty_uncovered() {
        StringBuilder sb = new StringBuilder();
        CommonUtil.toString(sb, new float[] { 1.0f, 2.0f });
        assertEquals("[1.0, 2.0]", sb.toString());
    }

    @Test
    public void testToString_StringBuilder_DoubleArray_Null_uncovered() {
        StringBuilder sb = new StringBuilder();
        CommonUtil.toString(sb, (double[]) null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testToString_StringBuilder_DoubleArray_Empty_uncovered() {
        StringBuilder sb = new StringBuilder();
        CommonUtil.toString(sb, new double[0]);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testToString_StringBuilder_DoubleArray_NonEmpty_uncovered() {
        StringBuilder sb = new StringBuilder();
        CommonUtil.toString(sb, new double[] { 1.5d, 2.5d });
        assertEquals("[1.5, 2.5]", sb.toString());
    }

    // unmodifiable* with an already-unmodifiable (Immutable) input returns the SAME instance (skips re-wrapping).
    @Test
    public void testUnmodifiableSortedSet_AlreadyUnmodifiable_ReturnsSameInstance() {
        SortedSet<String> base = new TreeSet<>(Arrays.asList("a", "b", "c"));
        SortedSet<String> already = Collections.unmodifiableSortedSet(base);
        assertSame(already, CommonUtil.unmodifiableSortedSet(already));
    }

    @Test
    public void testUnmodifiableNavigableSet_AlreadyUnmodifiable_ReturnsSameInstance() {
        NavigableSet<String> base = new TreeSet<>(Arrays.asList("a", "b", "c"));
        NavigableSet<String> already = Collections.unmodifiableNavigableSet(base);
        assertSame(already, CommonUtil.unmodifiableNavigableSet(already));
    }

    @Test
    public void testUnmodifiableMap_AlreadyUnmodifiable_ReturnsSameInstance() {
        Map<String, Integer> base = new HashMap<>();
        base.put("a", 1);
        Map<String, Integer> already = Collections.unmodifiableMap(base);
        assertSame(already, CommonUtil.unmodifiableMap(already));
    }

    @Test
    public void testUnmodifiableSortedMap_AlreadyUnmodifiable_ReturnsSameInstance() {
        SortedMap<String, Integer> base = new TreeMap<>();
        base.put("a", 1);
        SortedMap<String, Integer> already = Collections.unmodifiableSortedMap(base);
        assertSame(already, CommonUtil.unmodifiableSortedMap(already));
    }

    @Test
    public void testUnmodifiableNavigableMap_AlreadyUnmodifiable_ReturnsSameInstance() {
        NavigableMap<String, Integer> base = new TreeMap<>();
        base.put("a", 1);
        NavigableMap<String, Integer> already = Collections.unmodifiableNavigableMap(base);
        assertSame(already, CommonUtil.unmodifiableNavigableMap(already));
    }

    // toArray(Collection) and toArray(Collection, a) empty-collection branches.
    @Test
    public void testToArray_Collection_Empty_uncovered() {
        Object[] result = CommonUtil.toArray(new ArrayList<String>());
        assertEquals(0, result.length);
        assertSame(result, CommonUtil.toArray((Collection<?>) null));
    }

    @Test
    public void testToArray_Collection_Array_EmptyCollection_NullsFirstSlot() {
        // Empty collection: existing array's first slot is set to null, same array returned.
        String[] a = { "x", "y" };
        String[] result = CommonUtil.toArray(new ArrayList<String>(), a);
        assertSame(a, result);
        assertNull(result[0]);
    }

    @Test
    public void testToArray_Collection_IntFunction_Empty_uncovered() {
        String[] result = CommonUtil.toArray(new ArrayList<String>(), String[]::new);
        assertEquals(0, result.length);
    }

    // toArray(Collection, from, to, a) — non-List path that sets the trailing slot to null when a is oversized.
    @Test
    public void testToArray_Collection_Range_Array_NonList_NullTail() {
        LinkedHashSet<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d", "e"));
        String[] a = new String[5]; // larger than the requested range (3)
        String[] result = CommonUtil.toArray(set, 1, 4, a);
        // res holds the slice
        assertEquals("b", result[0]);
        assertEquals("c", result[1]);
        assertEquals("d", result[2]);
        // trailing slot in original array nulled out
        assertNull(a[3]);
    }

    @Test
    public void testToArray_Collection_Range_Array_EmptyCollection() {
        String[] a = { "x", "y" };
        String[] result = CommonUtil.toArray(new ArrayList<String>(), 0, 0, a);
        assertSame(a, result);
        assertNull(result[0]);
    }

    // toCharArray(Collection, from, to, defaultForNull) — non-RandomAccess path with a null element.
    @Test
    public void testToCharArray_Collection_Range_NonRandomAccess_WithNull() {
        LinkedList<Character> list = new LinkedList<>(Arrays.asList('a', 'b', null, 'd', 'e'));
        char[] result = CommonUtil.toCharArray(list, 1, 4, 'X');
        assertArrayEquals(new char[] { 'b', 'X', 'd' }, result);
    }

    @Test
    public void testToCharArray_Collection_Range_NonRandomAccess_Empty() {
        LinkedList<Character> list = new LinkedList<>(Arrays.asList('a', 'b', 'c'));
        char[] result = CommonUtil.toCharArray(list, 2, 2, 'X');
        assertEquals(0, result.length);
    }

    // newDataset(columnNames, rows): bean rows where a column name is missing -> null cell.
    @Test
    public void testNewDataset_ColumnNames_BeanRows_MissingProp_NullCell() {
        List<DatasetRowBean> rows = Arrays.asList(new DatasetRowBean("Tom", 10), new DatasetRowBean("Jerry", 12));
        Dataset ds = CommonUtil.newDataset(Arrays.asList("name", "age", "missing"), rows);
        assertEquals(3, ds.columnCount());
        assertEquals(2, ds.size());
        assertNull(ds.get(0, ds.getColumnIndex("missing")));
        assertEquals("Tom", ds.get(0, ds.getColumnIndex("name")));
    }

    // newDataset(columnNames, rows): array row whose length mismatches column count -> IllegalArgumentException.
    @Test
    public void testNewDataset_ColumnNames_ArrayRow_LengthMismatch_Throws() {
        List<String> cols = Arrays.asList("a", "b");
        List<Object> rows = new ArrayList<>();
        rows.add(new Object[] { 1, 2, 3 }); // single row that is an Object[] of wrong length
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(cols, rows));
    }

    // newDataset(columnNames, rows): collection row whose size mismatches column count -> IllegalArgumentException.
    @Test
    public void testNewDataset_ColumnNames_CollectionRow_SizeMismatch_Throws() {
        List<String> cols = Arrays.asList("a", "b");
        List<List<Object>> rows = Arrays.asList(Arrays.<Object> asList(1, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(cols, rows));
    }

    // newDataset(columnNames, rows): single column, scalar (non-array/collection/map/bean) row -> single-column case.
    @Test
    public void testNewDataset_ColumnNames_SingleColumn_ScalarRows() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("val"), Arrays.asList("x", "y", "z"));
        assertEquals(1, ds.columnCount());
        assertEquals(3, ds.size());
        assertEquals("y", ds.get(1, 0));
    }

    // newDataset(columnNames, rows): multi-column with an unsupported scalar row -> IllegalArgumentException.
    @Test
    public void testNewDataset_ColumnNames_UnsupportedRowType_Throws() {
        List<String> cols = Arrays.asList("a", "b");
        List<Object> rows = Arrays.asList((Object) "unsupportedScalar");
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(cols, rows));
    }

    // newDataset(columnNames, rows): null row produces a full row of nulls.
    @Test
    public void testNewDataset_ColumnNames_NullRow_AllNullCells() {
        List<String> cols = Arrays.asList("a", "b");
        List<Object> rows = new ArrayList<>();
        rows.add(null);
        rows.add(new Object[] { 1, 2 });
        Dataset ds = CommonUtil.newDataset(cols, rows);
        assertEquals(2, ds.size());
        assertNull(ds.get(0, 0));
        assertNull(ds.get(0, 1));
        assertEquals(1, (int) ds.get(1, 0));
    }

    // newDataset(rows): auto-derive columns from bean rows.
    @Test
    public void testNewDataset_Rows_BeanAutoColumns() {
        List<DatasetRowBean> rows = Arrays.asList(new DatasetRowBean("Tom", 10), new DatasetRowBean("Jerry", 12));
        Dataset ds = CommonUtil.newDataset(rows);
        assertEquals(2, ds.size());
        assertTrue(ds.columnNames().contains("name"));
        assertTrue(ds.columnNames().contains("age"));
    }

    // newDataset(rows): first element null -> IllegalArgumentException.
    @Test
    public void testNewDataset_Rows_FirstElementNull_Throws() {
        List<Object> rows = new ArrayList<>();
        rows.add(null);
        rows.add("something");
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(rows));
    }

    // newDataset(rows): unsupported row type (scalar) -> IllegalArgumentException.
    @Test
    public void testNewDataset_Rows_UnsupportedType_Throws() {
        List<Object> rows = Arrays.asList((Object) "scalar");
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(rows));
    }

    // newDataset(keyColumnName, valueColumnName, map): non-empty map populates two columns.
    @Test
    public void testNewDataset_KeyValueColumns_NonEmptyMap() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("Alice", 95);
        m.put("Bob", 87);
        Dataset ds = CommonUtil.newDataset("Name", "Score", m);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
        assertEquals("Alice", ds.get(0, ds.getColumnIndex("Name")));
        assertEquals(95, (int) ds.get(0, ds.getColumnIndex("Score")));
    }

    // newDataset(Map<String, Collection>): ragged columns are right-padded with null.
    @Test
    public void testNewDataset_MapOfCollections_RaggedPadding() {
        Map<String, List<Object>> data = new LinkedHashMap<>();
        data.put("Name", Arrays.<Object> asList("Alice", "Bob", "Charlie"));
        data.put("City", Arrays.<Object> asList("NYC", "LA")); // shorter
        Dataset ds = CommonUtil.newDataset(data);
        assertEquals(3, ds.size());
        assertEquals(2, ds.columnCount());
        assertNull(ds.get(2, ds.getColumnIndex("City")));
        assertEquals("Charlie", ds.get(2, ds.getColumnIndex("Name")));
    }

    // newSetMultimap(Map): seeds the multimap from a map's entries.
    @Test
    public void testNewSetMultimap_FromMap() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("a", 1);
        m.put("b", 2);
        SetMultimap<String, Integer> mm = CommonUtil.newSetMultimap(m);
        assertEquals(2, mm.size());
        assertTrue(mm.get("a").contains(1));
        assertTrue(mm.get("b").contains(2));
    }

    // newSetMultimap(collection, keyExtractor): groups elements by extracted key.
    @Test
    public void testNewSetMultimap_FromCollection_KeyExtractor() {
        List<String> words = Arrays.asList("apple", "avocado", "banana");
        SetMultimap<Character, String> mm = CommonUtil.newSetMultimap(words, w -> w.charAt(0));
        assertEquals(2, mm.get('a').size());
        assertTrue(mm.get('b').contains("banana"));
    }

    // newSetMultimap(collection, keyExtractor, valueExtractor): groups extracted values by extracted key.
    @Test
    public void testNewSetMultimap_FromCollection_KeyAndValueExtractor() {
        List<String> words = Arrays.asList("apple", "avocado", "banana");
        SetMultimap<Character, Integer> mm = CommonUtil.newSetMultimap(words, w -> w.charAt(0), String::length);
        assertTrue(mm.get('a').contains(5)); // "apple"
        assertTrue(mm.get('a').contains(7)); // "avocado"
        assertTrue(mm.get('b').contains(6)); // "banana"
    }

    // hashCodeEverything(bean): recurses through bean properties.
    @Test
    public void testHashCodeEverything_Bean_uncovered() {
        DatasetRowBean b1 = new DatasetRowBean("Tom", 10);
        DatasetRowBean b2 = new DatasetRowBean("Tom", 10);
        DatasetRowBean b3 = new DatasetRowBean("Jerry", 12);
        assertEquals(N.hashCodeEverything(b1), N.hashCodeEverything(b2));
        assertNotEquals(N.hashCodeEverything(b1), N.hashCodeEverything(b3));
    }

    // findLastIndex(Collection, Predicate): non-RandomAccess, non-Deque collection -> toArray fall-through path.
    @Test
    public void testFindLastIndex_Collection_LinkedHashSet_ToArrayPath() {
        LinkedHashSet<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        OptionalInt result = N.findLastIndex(set, s -> s.equals("c"));
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    public void testFindLastIndex_Collection_LinkedHashSet_NoMatch() {
        LinkedHashSet<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        OptionalInt result = N.findLastIndex(set, s -> s.equals("z"));
        assertFalse(result.isPresent());
    }

    // lastNonNull(Iterable): non-Deque, non-list iterable -> delegates to iterator path.
    @Test
    public void testLastNonNull_Iterable_LinkedHashSet_uncovered() {
        LinkedHashSet<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        com.landawn.abacus.util.u.Optional<String> result = N.lastNonNull(set);
        assertTrue(result.isPresent());
        assertEquals("c", result.get());
    }

    // convert: Clob -> String and Clob -> char[] (closes the clob via free()).
    @Test
    public void testConvert_ClobToString_uncovered() {
        StubClob clob = new StubClob("hello world");
        String result = N.convert(clob, String.class);
        assertEquals("hello world", result);
        assertTrue(clob.freed, "clob should be freed after conversion");
    }

    @Test
    public void testConvert_ClobToCharArray_uncovered() {
        StubClob clob = new StubClob("abc");
        char[] result = N.convert(clob, char[].class);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
        assertTrue(clob.freed, "clob should be freed after conversion");
    }

    // convert: Map -> Map copy path (same instance type recreated, contents preserved).
    @Test
    public void testConvert_MapToMap_CopyPath_uncovered() {
        Map<String, Integer> src = new LinkedHashMap<>();
        src.put("a", 1);
        src.put("b", 2);
        @SuppressWarnings("unchecked")
        Map<String, Integer> result = N.convert(src, (Class<Map<String, Integer>>) (Class<?>) TreeMap.class);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1, (int) result.get("a"));
        assertTrue(result instanceof TreeMap);
    }

    // convert: Collection -> Collection copy path.
    @Test
    public void testConvert_CollectionToCollection_CopyPath_uncovered() {
        List<Integer> src = Arrays.asList(1, 2, 3, 2);
        @SuppressWarnings("unchecked")
        Set<Integer> result = N.convert(src, (Class<Set<Integer>>) (Class<?>) LinkedHashSet.class);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains(1));
        assertTrue(result instanceof LinkedHashSet);
    }

    // Minimal Clob stub backed by a String; only the methods exercised by convert(...) are implemented.
    private static final class StubClob implements java.sql.Clob {
        private final String data;
        boolean freed = false;

        StubClob(String data) {
            this.data = data;
        }

        @Override
        public long length() {
            return data.length();
        }

        @Override
        public String getSubString(long pos, int length) {
            return data.substring((int) (pos - 1), (int) (pos - 1) + length);
        }

        @Override
        public Reader getCharacterStream() {
            return new StringReader(data);
        }

        @Override
        public InputStream getAsciiStream() {
            return new ByteArrayInputStream(data.getBytes());
        }

        @Override
        public long position(String searchstr, long start) {
            return data.indexOf(searchstr, (int) (start - 1)) + 1;
        }

        @Override
        public long position(java.sql.Clob searchstr, long start) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int setString(long pos, String str) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int setString(long pos, String str, int offset, int len) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OutputStream setAsciiStream(long pos) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.io.Writer setCharacterStream(long pos) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void truncate(long len) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void free() {
            freed = true;
        }

        @Override
        public Reader getCharacterStream(long pos, long length) {
            return new StringReader(data.substring((int) (pos - 1), (int) (pos - 1 + length)));
        }
    }

}
