package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

// Assuming N.java and its dependencies (com.landawn.abacus.*, etc.) are in the classpath.
// Specifically, constants like Strings.NULL, Strings.ELEMENT_SEPARATOR, WD.BRACKET_L, etc.,
// and helper methods like N.typeOf(), N.CLASS_TYPE_ENUM, N.checkArgNotEmpty(), etc.,
// are assumed to be available and function as expected.
public class CommonUtil203Test extends TestBase {

    // Assuming Strings.EMPTY is defined elsewhere, typically as ""
    private static final String EMPTY_STR = "";

    @Nested
    @DisplayName("nullToEmpty Methods")
    public class NullToEmptyTests {

        @Test
        public void testNullToEmpty_String() {
            assertEquals(EMPTY_STR, N.nullToEmpty((String) null));
            assertEquals("test", N.nullToEmpty("test"));
            assertEquals(EMPTY_STR, N.nullToEmpty(EMPTY_STR));
        }

        @Test
        public void testNullToEmpty_List() {
            List<String> nonNullList = new ArrayList<>();
            nonNullList.add("a");
            assertSame(nonNullList, N.nullToEmpty(nonNullList));

            List<?> emptyList = N.nullToEmpty((List<?>) null);
            assertTrue(emptyList.isEmpty());
            // Check for immutability if N.emptyList() guarantees it
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
            // Note: This method does NOT change nulls inside the array to empty strings.
            String[] arrWithNull = { "a", null };
            String[] resultArrWithNull = N.nullToEmpty(arrWithNull);
            assertSame(arrWithNull, resultArrWithNull);
            assertNull(resultArrWithNull[1]);
        }

        @Test
        public void testNullToEmptyForEach_StringArray() {
            assertArrayEquals(N.EMPTY_STRING_ARRAY, N.nullToEmptyForEach(null));
            String[] arr = { "a", "b" };
            String[] result = N.nullToEmptyForEach(arr);
            assertSame(arr, result); // Modifies in place
            assertArrayEquals(new String[] { "a", "b" }, result);

            String[] arrWithNulls = { "a", null, "c", null };
            String[] expected = { "a", EMPTY_STR, "c", EMPTY_STR };
            result = N.nullToEmptyForEach(arrWithNulls);
            assertSame(arrWithNulls, result); // Modifies in place
            assertArrayEquals(expected, result);

            String[] allNulls = { null, null };
            String[] expectedAllEmpty = { EMPTY_STR, EMPTY_STR };
            result = N.nullToEmptyForEach(allNulls);
            assertSame(allNulls, result);
            assertArrayEquals(expectedAllEmpty, result);

            String[] emptyArr = N.EMPTY_STRING_ARRAY;
            result = N.nullToEmptyForEach(emptyArr);
            assertSame(emptyArr, result); // Should return the same empty array instance
            assertArrayEquals(N.EMPTY_STRING_ARRAY, result);
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
        @SuppressWarnings("unchecked")
        public void testNullToEmpty_GenericArray() {
            // Integer[]
            Integer[] intArr = { 1, 2 };
            assertSame(intArr, N.nullToEmpty(intArr, Integer[].class));
            Integer[] nullIntArr = null;
            Integer[] emptyIntArrResult = N.nullToEmpty(nullIntArr, Integer[].class);
            assertNotNull(emptyIntArrResult);
            assertEquals(0, emptyIntArrResult.length);
            assertEquals(Integer.class, emptyIntArrResult.getClass().getComponentType());

            // String[]
            String[] strArr = { "a", "b" };
            assertSame(strArr, N.nullToEmpty(strArr, String[].class));
            String[] nullStrArr = null;
            String[] emptyStrArrResult = N.nullToEmpty(nullStrArr, String[].class);
            assertNotNull(emptyStrArrResult);
            assertEquals(0, emptyStrArrResult.length);
            assertArrayEquals(new String[0], emptyStrArrResult);
            assertEquals(String.class, emptyStrArrResult.getClass().getComponentType());

            // Custom Object[]
            MyClass[] myObjArr = { new MyClass(), new MyClass() };
            assertSame(myObjArr, N.nullToEmpty(myObjArr, MyClass[].class));
            MyClass[] nullMyObjArr = null;
            MyClass[] emptyMyObjArrResult = N.nullToEmpty(nullMyObjArr, MyClass[].class);
            assertNotNull(emptyMyObjArrResult);
            assertEquals(0, emptyMyObjArrResult.length);
            assertEquals(MyClass.class, emptyMyObjArrResult.getClass().getComponentType());

        }

        static class MyClass {
        }

        // Tests for ImmutableXxx types. These assume that com.landawn.abacus.util.ImmutableX classes
        // have static empty() methods that N calls, or N provides similar empty immutable instances.
        // For simplicity, these tests will check against standard empty collections for emptiness.

        @Test
        public void testNullToEmpty_ImmutableCollection() {
            // Assuming com.landawn.abacus.util.ImmutableList.empty() is what N.nullToEmpty would return for null
            // For the purpose of this test, we'll check the general contract.
            com.landawn.abacus.util.ImmutableList<String> nonNullList = com.landawn.abacus.util.ImmutableList.of("a");
            assertSame(nonNullList, N.nullToEmpty(nonNullList));

            com.landawn.abacus.util.ImmutableCollection<?> emptyCol = N.nullToEmpty((com.landawn.abacus.util.ImmutableCollection<?>) null);
            assertTrue(emptyCol.isEmpty());
            // Further checks depend on the behavior of ImmutableList.empty()
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
    }

    @Nested
    @DisplayName("defaultIfNull Methods")
    public class DefaultIfNullTests {

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
    }

    @Nested
    @DisplayName("defaultIfEmpty/Blank Methods")
    public class DefaultIfEmptyBlankTests {
        //These rely on N.isEmpty, N.isBlank, N.checkArgNotEmpty, N.checkArgNotBlank

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
        public void testDefaultIfEmpty_Collection() {
            List<String> defaultList = Arrays.asList("default");

            List<String> nullList = null;
            List<String> emptyList = new ArrayList<>();
            List<String> actualList = Arrays.asList("actual");

            assertEquals(defaultList, N.defaultIfEmpty(nullList, defaultList));
            assertEquals(defaultList, N.defaultIfEmpty(emptyList, defaultList));
            assertEquals(actualList, N.defaultIfEmpty(actualList, defaultList));

            assertThrows(IllegalArgumentException.class, () -> N.defaultIfEmpty(actualList, Collections.emptyList()));
            assertThrows(IllegalArgumentException.class, () -> N.defaultIfEmpty(actualList, (List<String>) null));
        }

        @Test
        public void testDefaultIfEmpty_Map() {
            Map<String, String> defaultMap = new HashMap<>();
            defaultMap.put("key", "default");

            Map<String, String> nullMap = null;
            Map<String, String> emptyMap = new HashMap<>();
            Map<String, String> actualMap = new HashMap<>();
            actualMap.put("key", "actual");

            assertEquals(defaultMap, N.defaultIfEmpty(nullMap, defaultMap));
            assertEquals(defaultMap, N.defaultIfEmpty(emptyMap, defaultMap));
            assertEquals(actualMap, N.defaultIfEmpty(actualMap, defaultMap));

            assertThrows(IllegalArgumentException.class, () -> N.defaultIfEmpty(actualMap, Collections.emptyMap()));
            assertThrows(IllegalArgumentException.class, () -> N.defaultIfEmpty(actualMap, (Map<String, String>) null));
        }
    }

    @Nested
    @DisplayName("defaultValueOf Method")
    public class DefaultValueOfTests {
        // This test's correctness heavily depends on the implementation of
        // com.landawn.abacus.util.Type.defaultValue() which is not provided.
        // The following are common expectations for default values.
        @Test
        public void testDefaultValueOf() {
            // Assuming Type.defaultValue() behaves as follows:
            assertEquals(false, (boolean) N.defaultValueOf(boolean.class)); // or Boolean.class for primitive default
            assertEquals(0, (char) N.defaultValueOf(char.class)); // or Character.class
            assertEquals(0, (byte) N.defaultValueOf(byte.class)); // or Byte.class
            assertEquals(0, (short) N.defaultValueOf(short.class)); // or Short.class
            assertEquals(0, (int) N.defaultValueOf(int.class)); // or Integer.class
            assertEquals(0L, (long) N.defaultValueOf(long.class)); // or Long.class
            assertEquals(0.0f, (float) N.defaultValueOf(float.class), 0.0f); // or Float.class
            assertEquals(0.0, (double) N.defaultValueOf(double.class), 0.0); // or Double.class

            assertEquals(null, N.defaultValueOf(Boolean.class));
            assertEquals(null, N.defaultValueOf(Character.class)); // Or specific char default
            assertEquals(null, N.defaultValueOf(Byte.class));
            assertEquals(null, N.defaultValueOf(Short.class));
            assertEquals(null, N.defaultValueOf(Integer.class));
            assertEquals(null, N.defaultValueOf(Long.class));
            assertEquals(null, N.defaultValueOf(Float.class));
            assertEquals(null, N.defaultValueOf(Double.class));

            assertNull(N.defaultValueOf(String.class));
            assertNull(N.defaultValueOf(Object.class));
            assertNull(N.defaultValueOf(List.class));

            assertThrows(IllegalArgumentException.class, () -> N.defaultValueOf(null)); // Assuming typeOf(null) throws NPE
        }
    }
}
