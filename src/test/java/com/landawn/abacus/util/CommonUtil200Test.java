package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class CommonUtil200Test extends TestBase {

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
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(6, 5, 10)); // fromIndex + size > length
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(0, 1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(0, -1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(0, 1, -1)); // Negative length
    }

    @Test
    public void checkIndex_valid() {
        assertEquals(0, N.checkIndex(0, 1));
        assertEquals(5, N.checkIndex(5, 10));
        assertEquals(9, N.checkIndex(9, 10));
    }

    @Test
    public void checkIndex_invalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkIndex(-1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkIndex(10, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkIndex(0, 0));
        assertThrows(IllegalArgumentException.class, () -> N.checkIndex(0, -1));
    }

    @Test
    public void checkElementIndex_valid() {
        assertEquals(0, N.checkElementIndex(0, 1));
        assertEquals(5, N.checkElementIndex(5, 10));
        assertEquals(9, N.checkElementIndex(9, 10));
    }

    @Test
    public void checkElementIndex_invalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(-1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(10, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkElementIndex(0, 0));
        assertThrows(IllegalArgumentException.class, () -> N.checkElementIndex(0, -1));
    }

    @Test
    public void checkElementIndexWithDesc_valid() {
        assertEquals(0, N.checkElementIndex(0, 1, "testIndex"));
        assertEquals(5, N.checkElementIndex(5, 10, "testIndex"));
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
    public void checkPositionIndex_valid() {
        assertEquals(0, N.checkPositionIndex(0, 0));
        assertEquals(0, N.checkPositionIndex(0, 10));
        assertEquals(5, N.checkPositionIndex(5, 10));
        assertEquals(10, N.checkPositionIndex(10, 10));
    }

    @Test
    public void checkPositionIndex_invalid() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkPositionIndex(-1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkPositionIndex(11, 10));
        assertThrows(IllegalArgumentException.class, () -> N.checkPositionIndex(0, -1));
    }

    @Test
    public void checkPositionIndexWithDesc_valid() {
        assertEquals(0, N.checkPositionIndex(0, 0, "testPosIndex"));
        assertEquals(5, N.checkPositionIndex(5, 10, "testPosIndex"));
        assertEquals(10, N.checkPositionIndex(10, 10, "testPosIndex"));
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

    @Nested
    public class NArgumentCheckTest {

        @Test
        public void checkArgNotNull_object_valid() {
            String obj = "test";
            assertSame(obj, N.checkArgNotNull(obj));
            Integer num = 1;
            assertSame(num, N.checkArgNotNull(num));
        }

        @Test
        public void checkArgNotNull_object_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null));
        }

        @Test
        public void checkArgNotNull_objectWithMessage_valid() {
            String obj = "test";
            assertSame(obj, N.checkArgNotNull(obj, "testObject"));
        }

        @Test
        public void checkArgNotNull_objectWithMessage_invalid() {
            IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null, "testObject"));
            assertEquals("'testObject' cannot be null", ex1.getMessage());

            IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNull(null, "Custom error message for null object"));
            assertEquals("Custom error message for null object", ex2.getMessage());
        }

        @Test
        public void checkArgNotEmpty_charSequence_valid() {
            String s = "test";
            assertSame(s, N.checkArgNotEmpty(s, "charSeq"));
            StringBuilder sb = new StringBuilder("abc");
            assertSame(sb, N.checkArgNotEmpty(sb, "charSeqBuilder"));
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
        public void checkArgNotEmpty_booleanArray_valid() {
            boolean[] arr = { true, false };
            assertSame(arr, N.checkArgNotEmpty(arr, "boolArray"));
        }

        @Test
        public void checkArgNotEmpty_booleanArray_invalid() {
            IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((boolean[]) null, "boolArray"));
            assertEquals("'boolArray' cannot be null or empty", ex1.getMessage());

            IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new boolean[0], "boolArray"));
            assertEquals("'boolArray' cannot be null or empty", ex2.getMessage());
        }

        @Test
        public void checkArgNotEmpty_charArray_valid() {
            char[] arr = { 'a', 'b' };
            assertSame(arr, N.checkArgNotEmpty(arr, "charArr"));
        }

        @Test
        public void checkArgNotEmpty_charArray_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((char[]) null, "charArr"));
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new char[0], "charArr"));
        }

        @Test
        public void checkArgNotEmpty_byteArray_valid() {
            byte[] arr = { 1, 2 };
            assertSame(arr, N.checkArgNotEmpty(arr, "byteArr"));
        }

        @Test
        public void checkArgNotEmpty_byteArray_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((byte[]) null, "byteArr"));
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new byte[0], "byteArr"));
        }

        @Test
        public void checkArgNotEmpty_shortArray_valid() {
            short[] arr = { 1, 2 };
            assertSame(arr, N.checkArgNotEmpty(arr, "shortArr"));
        }

        @Test
        public void checkArgNotEmpty_shortArray_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((short[]) null, "shortArr"));
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new short[0], "shortArr"));
        }

        @Test
        public void checkArgNotEmpty_intArray_valid() {
            int[] arr = { 1, 2 };
            assertSame(arr, N.checkArgNotEmpty(arr, "intArr"));
        }

        @Test
        public void checkArgNotEmpty_intArray_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((int[]) null, "intArr"));
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new int[0], "intArr"));
        }

        @Test
        public void checkArgNotEmpty_longArray_valid() {
            long[] arr = { 1L, 2L };
            assertSame(arr, N.checkArgNotEmpty(arr, "longArr"));
        }

        @Test
        public void checkArgNotEmpty_longArray_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((long[]) null, "longArr"));
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new long[0], "longArr"));
        }

        @Test
        public void checkArgNotEmpty_floatArray_valid() {
            float[] arr = { 1.0f, 2.0f };
            assertSame(arr, N.checkArgNotEmpty(arr, "floatArr"));
        }

        @Test
        public void checkArgNotEmpty_floatArray_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((float[]) null, "floatArr"));
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new float[0], "floatArr"));
        }

        @Test
        public void checkArgNotEmpty_doubleArray_valid() {
            double[] arr = { 1.0, 2.0 };
            assertSame(arr, N.checkArgNotEmpty(arr, "doubleArr"));
        }

        @Test
        public void checkArgNotEmpty_doubleArray_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((double[]) null, "doubleArr"));
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new double[0], "doubleArr"));
        }

        @Test
        public void checkArgNotEmpty_objectArray_valid() {
            String[] arr = { "a", "b" };
            assertSame(arr, N.checkArgNotEmpty(arr, "objArr"));
        }

        @Test
        public void checkArgNotEmpty_objectArray_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Object[]) null, "objArr"));
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new Object[0], "objArr"));
        }

        @Test
        public void checkArgNotEmpty_collection_valid() {
            List<String> list = Arrays.asList("a", "b");
            assertSame(list, N.checkArgNotEmpty(list, "coll"));
        }

        @Test
        public void checkArgNotEmpty_collection_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Collection<?>) null, "coll"));
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(Collections.emptyList(), "coll"));
        }

        @Test
        public void checkArgNotEmpty_iterable_valid() {
            Iterable<String> iterable = Arrays.asList("a", "b");
            assertSame(iterable, N.checkArgNotEmpty(iterable, "iterable"));
        }

        @Test
        public void checkArgNotEmpty_iterable_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Iterable<?>) null, "iterable"));
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(Collections.emptyList(), "iterable")); // An empty list is an empty iterable
        }

        @Test
        public void checkArgNotEmpty_iterator_valid() {
            Iterator<String> iterator = Arrays.asList("a", "b").iterator();
            assertSame(iterator, N.checkArgNotEmpty(iterator, "iterator"));
        }

        @Test
        public void checkArgNotEmpty_iterator_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Iterator<?>) null, "iterator"));
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(Collections.emptyIterator(), "iterator"));

            // Test with an iterator that becomes empty after one next()
            List<String> listWithOne = new ArrayList<>(Collections.singletonList("a"));
            Iterator<String> iter = listWithOne.iterator();
            iter.next(); // Make it empty
            // This test is tricky because checkArgNotEmpty for iterator checks iter.hasNext().
            // If the iterator was initially non-empty but is now consumed, it will still pass if it was not null.
            // The check is: if (arg == null || !arg.hasNext()) for iterators.
            // So an iterator that *was* non-empty but is now consumed, will be seen as "empty" by this check.
            Iterator<String> consumedIterator = Arrays.asList("a").iterator();
            consumedIterator.next(); // now it's empty
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(consumedIterator, "consumedIterator"));

            Iterator<String> trulyEmptyIterator = Collections.emptyIterator();
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(trulyEmptyIterator, "trulyEmptyIterator"));

        }

        @Test
        public void checkArgNotEmpty_map_valid() {
            Map<String, Integer> map = new HashMap<>();
            map.put("a", 1);
            assertSame(map, N.checkArgNotEmpty(map, "map"));
        }

        @Test
        public void checkArgNotEmpty_map_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Map<?, ?>) null, "map"));
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(Collections.emptyMap(), "map"));
        }

        @Test
        public void checkArgNotEmpty_primitiveList_valid() {
            BooleanList pList = BooleanList.of(true, false); // Assuming a concrete implementation
            assertSame(pList, N.checkArgNotEmpty(pList, "pList"));
        }

        @Test
        public void checkArgNotEmpty_primitiveList_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((BooleanList) null, "pList"));
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(new BooleanList(), "pList"));
        }

        @Test
        public void checkArgNotEmpty_multiset_valid() {
            Multiset<String> multiset = Multiset.of("a", "b"); // Assuming a concrete implementation
            assertSame(multiset, N.checkArgNotEmpty(multiset, "multiset"));
        }

        @Test
        public void checkArgNotEmpty_multiset_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Multiset<String>) null, "multiset"));
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(N.newMultiset(), "multiset"));
        }

        @Test
        public void checkArgNotEmpty_multimap_valid() {
            Multimap<String, Integer, List<Integer>> multimap = N.newListMultimap(Map.of("a", 1)); // Assuming 
            assertSame(multimap, N.checkArgNotEmpty(multimap, "multimap"));
        }

        @Test
        public void checkArgNotEmpty_multimap_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Multimap<?, ?, ?>) null, "multimap"));
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(N.newListMultimap(), "multimap"));
        }

        @Test
        public void checkArgNotEmpty_dataset_valid() {
            List<String> columnNames = Arrays.asList("col1");
            List<List<?>> rows = new ArrayList<>();
            rows.add(Arrays.asList("val1"));
            Dataset dataset = N.newDataset(columnNames, rows); // Assuming a concrete implementation
            assertSame(dataset, N.checkArgNotEmpty(dataset, "dataset"));
        }

        @Test
        public void checkArgNotEmpty_dataset_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Dataset) null, "dataset"));
            Dataset emptyDs = N.emptyDataset(); // Empty rows
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(emptyDs, "dataset"));
        }

        @Test
        public void checkArgNotBlank_valid() {
            String s = "test";
            assertSame(s, N.checkArgNotBlank(s, "notBlankStr"));
            String s2 = "  test  ";
            assertSame(s2, N.checkArgNotBlank(s2, "notBlankStrWithSpaces"));
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
        public void checkArgNotNegative_byte_valid() {
            assertEquals((byte) 0, N.checkArgNotNegative((byte) 0, "byteArg"));
            assertEquals((byte) 10, N.checkArgNotNegative((byte) 10, "byteArg"));
        }

        @Test
        public void checkArgNotNegative_byte_invalid() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative((byte) -1, "byteArg"));
            assertEquals("'byteArg' cannot be negative: -1", ex.getMessage());
            IllegalArgumentException exCustom = assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative((byte) -1, "Custom msg"));
            assertEquals("Custom msg", exCustom.getMessage());
        }

        @Test
        public void checkArgNotNegative_short_valid() {
            assertEquals((short) 0, N.checkArgNotNegative((short) 0, "shortArg"));
            assertEquals((short) 100, N.checkArgNotNegative((short) 100, "shortArg"));
        }

        @Test
        public void checkArgNotNegative_short_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative((short) -1, "shortArg"));
        }

        @Test
        public void checkArgNotNegative_int_valid() {
            assertEquals(0, N.checkArgNotNegative(0, "intArg"));
            assertEquals(1000, N.checkArgNotNegative(1000, "intArg"));
        }

        @Test
        public void checkArgNotNegative_int_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-1, "intArg"));
        }

        @Test
        public void checkArgNotNegative_long_valid() {
            assertEquals(0L, N.checkArgNotNegative(0L, "longArg"));
            assertEquals(10000L, N.checkArgNotNegative(10000L, "longArg"));
        }

        @Test
        public void checkArgNotNegative_long_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-1L, "longArg"));
        }

        @Test
        public void checkArgNotNegative_float_valid() {
            assertEquals(0.0f, N.checkArgNotNegative(0.0f, "floatArg"), 0.0f);
            assertEquals(10.5f, N.checkArgNotNegative(10.5f, "floatArg"), 0.0f);
        }

        @Test
        public void checkArgNotNegative_float_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-0.1f, "floatArg"));
        }

        @Test
        public void checkArgNotNegative_double_valid() {
            assertEquals(0.0, N.checkArgNotNegative(0.0, "doubleArg"), 0.0);
            assertEquals(10.5, N.checkArgNotNegative(10.5, "doubleArg"), 0.0);
        }

        @Test
        public void checkArgNotNegative_double_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgNotNegative(-0.1, "doubleArg"));
        }

        @Test
        public void checkArgPositive_byte_valid() {
            assertEquals((byte) 1, N.checkArgPositive((byte) 1, "byteArg"));
            assertEquals((byte) 127, N.checkArgPositive((byte) 127, "byteArg"));
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
        public void checkArgPositive_short_valid() {
            assertEquals((short) 1, N.checkArgPositive((short) 1, "shortArg"));
        }

        @Test
        public void checkArgPositive_short_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((short) 0, "shortArg"));
            assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive((short) -1, "shortArg"));
        }

        @Test
        public void checkArgPositive_int_valid() {
            assertEquals(1, N.checkArgPositive(1, "intArg"));
        }

        @Test
        public void checkArgPositive_int_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0, "intArg"));
            assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-1, "intArg"));
        }

        @Test
        public void checkArgPositive_long_valid() {
            assertEquals(1L, N.checkArgPositive(1L, "longArg"));
        }

        @Test
        public void checkArgPositive_long_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0L, "longArg"));
            assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-1L, "longArg"));
        }

        @Test
        public void checkArgPositive_float_valid() {
            assertEquals(0.1f, N.checkArgPositive(0.1f, "floatArg"), 0.0f);
        }

        @Test
        public void checkArgPositive_float_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0.0f, "floatArg"));
            assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-0.1f, "floatArg"));
        }

        @Test
        public void checkArgPositive_double_valid() {
            assertEquals(0.1, N.checkArgPositive(0.1, "doubleArg"), 0.0);
        }

        @Test
        public void checkArgPositive_double_invalid() {
            assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(0.0, "doubleArg"));
            assertThrows(IllegalArgumentException.class, () -> N.checkArgPositive(-0.1, "doubleArg"));
        }

        @Test
        public void checkElementNotNull_array_valid() {
            assertDoesNotThrow(() -> N.checkElementNotNull(new String[] { "a", "b" }));
            assertDoesNotThrow(() -> N.checkElementNotNull(new String[0])); // Empty array is fine
            assertDoesNotThrow(() -> N.checkElementNotNull((Object[]) null)); // Null array is fine
        }

        @Test
        public void checkElementNotNull_array_invalid() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> N.checkElementNotNull(new String[] { "a", null, "b" }));
            assertEquals("null element is found in collection", ex.getMessage()); // Default message
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
            assertDoesNotThrow(() -> N.checkElementNotNull(Collections.emptyList())); // Empty collection is fine
            assertDoesNotThrow(() -> N.checkElementNotNull((Collection<?>) null)); // Null collection is fine
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
        public void checkKeyNotNull_map_valid() {
            Map<String, Integer> map = new HashMap<>();
            map.put("a", 1);
            map.put("b", null); // Null value is fine
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
        public void checkValueNotNull_map_valid() {
            Map<String, Integer> map = new HashMap<>();
            map.put("a", 1);
            map.put("b", 2);
            assertDoesNotThrow(() -> N.checkValueNotNull(map));
            Map<Object, String> mapWithNullKey = new HashMap<>();
            mapWithNullKey.put(null, "value"); // Null key is fine here
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

            IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                    () -> N.checkValueNotNull(mapWithNullValue, "Custom error for null value"));
            assertEquals("Custom error for null value", ex2.getMessage());
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
            assertEquals("arg1 and %s: [arg2]", ex2.getMessage()); // {} takes precedence

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

        // Test a few of the two-parameter template methods
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
    }

    @Nested
    public class NStateCheckTest {

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

        // Test a few of the two-parameter template methods
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
    }

    @Nested
    public class NRequireNonNullTest {

        @Test
        public void requireNonNull_object_valid() {
            String obj = "test";
            assertSame(obj, N.requireNonNull(obj));
            Integer num = 1;
            assertSame(num, N.requireNonNull(num));
        }

        @Test
        public void requireNonNull_object_invalid() {
            assertThrows(NullPointerException.class, () -> N.requireNonNull(null));
        }

        @Test
        public void requireNonNull_objectWithMessage_valid() {
            String obj = "test";
            assertSame(obj, N.requireNonNull(obj, "testObject"));
        }

        @Test
        public void requireNonNull_objectWithMessage_invalid() {
            NullPointerException ex1 = assertThrows(NullPointerException.class, () -> N.requireNonNull(null, "testObject"));
            assertEquals("'testObject' cannot be null", ex1.getMessage());

            NullPointerException ex2 = assertThrows(NullPointerException.class, () -> N.requireNonNull(null, "Custom error message for null object"));
            assertEquals("Custom error message for null object", ex2.getMessage());
        }

        @Test
        public void requireNonNull_objectWithSupplier_valid() {
            String obj = "test";
            assertSame(obj, N.requireNonNull(obj, () -> "This should not be called"));
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

            supplierCalled[0] = false; // reset
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
    }
}
