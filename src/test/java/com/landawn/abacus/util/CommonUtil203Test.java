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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class CommonUtil203Test extends TestBase {

    private static final String EMPTY_STR = "";

    @Nested
    @DisplayName("nullToEmpty Methods")
    public class NullToEmptyTests {

        @Test
        public void testNullToEmpty_String() {
            assertEquals(EMPTY_STR, CommonUtil.nullToEmpty((String) null));
            assertEquals("test", CommonUtil.nullToEmpty("test"));
            assertEquals(EMPTY_STR, CommonUtil.nullToEmpty(EMPTY_STR));
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
        public void testNullToEmptyForEach_StringArray() {
            assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, CommonUtil.nullElementsToEmpty(null));
            String[] arr = { "a", "b" };
            String[] result = CommonUtil.nullElementsToEmpty(arr);
            assertSame(arr, result);
            assertArrayEquals(new String[] { "a", "b" }, result);

            String[] arrWithNulls = { "a", null, "c", null };
            String[] expected = { "a", EMPTY_STR, "c", EMPTY_STR };
            result = CommonUtil.nullElementsToEmpty(arrWithNulls);
            assertSame(arrWithNulls, result);
            assertArrayEquals(expected, result);

            String[] allNulls = { null, null };
            String[] expectedAllEmpty = { EMPTY_STR, EMPTY_STR };
            result = CommonUtil.nullElementsToEmpty(allNulls);
            assertSame(allNulls, result);
            assertArrayEquals(expectedAllEmpty, result);

            String[] emptyArr = CommonUtil.EMPTY_STRING_ARRAY;
            result = CommonUtil.nullElementsToEmpty(emptyArr);
            assertSame(emptyArr, result);
            assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, result);
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
        @SuppressWarnings("unchecked")
        public void testNullToEmpty_GenericArray() {
            Integer[] intArr = { 1, 2 };
            assertSame(intArr, CommonUtil.nullToEmpty(intArr, Integer[].class));
            Integer[] nullIntArr = null;
            Integer[] emptyIntArrResult = CommonUtil.nullToEmpty(nullIntArr, Integer[].class);
            assertNotNull(emptyIntArrResult);
            assertEquals(0, emptyIntArrResult.length);
            assertEquals(Integer.class, emptyIntArrResult.getClass().getComponentType());

            String[] strArr = { "a", "b" };
            assertSame(strArr, CommonUtil.nullToEmpty(strArr, String[].class));
            String[] nullStrArr = null;
            String[] emptyStrArrResult = CommonUtil.nullToEmpty(nullStrArr, String[].class);
            assertNotNull(emptyStrArrResult);
            assertEquals(0, emptyStrArrResult.length);
            assertArrayEquals(new String[0], emptyStrArrResult);
            assertEquals(String.class, emptyStrArrResult.getClass().getComponentType());

            MyClass[] myObjArr = { new MyClass(), new MyClass() };
            assertSame(myObjArr, CommonUtil.nullToEmpty(myObjArr, MyClass[].class));
            MyClass[] nullMyObjArr = null;
            MyClass[] emptyMyObjArrResult = CommonUtil.nullToEmpty(nullMyObjArr, MyClass[].class);
            assertNotNull(emptyMyObjArrResult);
            assertEquals(0, emptyMyObjArrResult.length);
            assertEquals(MyClass.class, emptyMyObjArrResult.getClass().getComponentType());

        }

        static class MyClass {
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
    }

    @Nested
    @DisplayName("defaultIfNull Methods")
    public class DefaultIfNullTests {

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
    }

    @Nested
    @DisplayName("defaultIfEmpty/Blank Methods")
    public class DefaultIfEmptyBlankTests {

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
        public void testDefaultIfEmpty_Collection() {
            List<String> defaultList = Arrays.asList("default");

            List<String> nullList = null;
            List<String> emptyList = new ArrayList<>();
            List<String> actualList = Arrays.asList("actual");

            assertEquals(defaultList, CommonUtil.defaultIfEmpty(nullList, defaultList));
            assertEquals(defaultList, CommonUtil.defaultIfEmpty(emptyList, defaultList));
            assertEquals(actualList, CommonUtil.defaultIfEmpty(actualList, defaultList));

            assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty(actualList, Collections.emptyList()));
            assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty(actualList, (List<String>) null));
        }

        @Test
        public void testDefaultIfEmpty_Map() {
            Map<String, String> defaultMap = new HashMap<>();
            defaultMap.put("key", "default");

            Map<String, String> nullMap = null;
            Map<String, String> emptyMap = new HashMap<>();
            Map<String, String> actualMap = new HashMap<>();
            actualMap.put("key", "actual");

            assertEquals(defaultMap, CommonUtil.defaultIfEmpty(nullMap, defaultMap));
            assertEquals(defaultMap, CommonUtil.defaultIfEmpty(emptyMap, defaultMap));
            assertEquals(actualMap, CommonUtil.defaultIfEmpty(actualMap, defaultMap));

            assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty(actualMap, Collections.emptyMap()));
            assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty(actualMap, (Map<String, String>) null));
        }
    }

    @Nested
    @DisplayName("defaultValueOf Method")
    public class DefaultValueOfTests {
        @Test
        public void testDefaultValueOf() {
            assertEquals(false, (boolean) CommonUtil.defaultValueOf(boolean.class));
            assertEquals(0, (char) CommonUtil.defaultValueOf(char.class));
            assertEquals(0, (byte) CommonUtil.defaultValueOf(byte.class));
            assertEquals(0, (short) CommonUtil.defaultValueOf(short.class));
            assertEquals(0, (int) CommonUtil.defaultValueOf(int.class));
            assertEquals(0L, (long) CommonUtil.defaultValueOf(long.class));
            assertEquals(0.0f, CommonUtil.defaultValueOf(float.class), 0.0f);
            assertEquals(0.0, CommonUtil.defaultValueOf(double.class), 0.0);

            assertEquals(null, CommonUtil.defaultValueOf(Boolean.class));
            assertEquals(null, CommonUtil.defaultValueOf(Character.class));
            assertEquals(null, CommonUtil.defaultValueOf(Byte.class));
            assertEquals(null, CommonUtil.defaultValueOf(Short.class));
            assertEquals(null, CommonUtil.defaultValueOf(Integer.class));
            assertEquals(null, CommonUtil.defaultValueOf(Long.class));
            assertEquals(null, CommonUtil.defaultValueOf(Float.class));
            assertEquals(null, CommonUtil.defaultValueOf(Double.class));

            assertNull(CommonUtil.defaultValueOf(String.class));
            assertNull(CommonUtil.defaultValueOf(Object.class));
            assertNull(CommonUtil.defaultValueOf(List.class));

            assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultValueOf(null));
        }
    }
}
