package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
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
import java.lang.reflect.InvocationHandler;

import org.junit.jupiter.api.Test;

public class CommonUtilNullTest extends CommonUtilTestSupport {

    @Test
    public void testNullHandlingInCheckArgument() {
        CommonUtil.checkArgument(true, "value is %s", (Object) null);
        Exception ex = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "value %s", (Object) null));
        assertTrue(ex.getMessage().contains("null"));
        ex = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s and %s", null, null));
        assertEquals("null and null", ex.getMessage());
    }

    @Test
    public void testNullToEmpty_arrays() {
        assertArrayEquals(CommonUtil.EMPTY_BOOLEAN_ARRAY, CommonUtil.nullToEmpty((boolean[]) null));
        assertArrayEquals(CommonUtil.EMPTY_CHAR_ARRAY, CommonUtil.nullToEmpty((char[]) null));
        assertArrayEquals(CommonUtil.EMPTY_BYTE_ARRAY, CommonUtil.nullToEmpty((byte[]) null));
        assertArrayEquals(CommonUtil.EMPTY_SHORT_ARRAY, CommonUtil.nullToEmpty((short[]) null));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, CommonUtil.nullToEmpty((int[]) null));
        assertArrayEquals(CommonUtil.EMPTY_LONG_ARRAY, CommonUtil.nullToEmpty((long[]) null));
        assertArrayEquals(CommonUtil.EMPTY_FLOAT_ARRAY, CommonUtil.nullToEmpty((float[]) null), 0.0f);
        assertArrayEquals(CommonUtil.EMPTY_DOUBLE_ARRAY, CommonUtil.nullToEmpty((double[]) null), 0.0);
        assertArrayEquals(CommonUtil.EMPTY_BIG_INTEGER_ARRAY, CommonUtil.nullToEmpty((BigInteger[]) null));
        assertArrayEquals(CommonUtil.EMPTY_BIG_DECIMAL_ARRAY, CommonUtil.nullToEmpty((BigDecimal[]) null));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, CommonUtil.nullToEmpty((String[]) null));
        assertArrayEquals(CommonUtil.EMPTY_JU_DATE_ARRAY, CommonUtil.nullToEmpty((java.util.Date[]) null));
        assertArrayEquals(CommonUtil.EMPTY_DATE_ARRAY, CommonUtil.nullToEmpty((java.sql.Date[]) null));
        assertArrayEquals(CommonUtil.EMPTY_TIME_ARRAY, CommonUtil.nullToEmpty((java.sql.Time[]) null));
        assertArrayEquals(CommonUtil.EMPTY_TIMESTAMP_ARRAY, CommonUtil.nullToEmpty((java.sql.Timestamp[]) null));
        assertArrayEquals(CommonUtil.EMPTY_CALENDAR_ARRAY, CommonUtil.nullToEmpty((Calendar[]) null));
        assertArrayEquals(CommonUtil.EMPTY_OBJECT_ARRAY, CommonUtil.nullToEmpty((Object[]) null));

        boolean[] bools = { true, false };
        assertSame(bools, CommonUtil.nullToEmpty(bools));
        char[] chars = { 'a', 'b' };
        assertSame(chars, CommonUtil.nullToEmpty(chars));
        byte[] bytes = { 1, 2 };
        assertSame(bytes, CommonUtil.nullToEmpty(bytes));
        short[] shorts = { 1, 2 };
        assertSame(shorts, CommonUtil.nullToEmpty(shorts));
        int[] ints = { 1, 2 };
        assertSame(ints, CommonUtil.nullToEmpty(ints));
        long[] longs = { 1L, 2L };
        assertSame(longs, CommonUtil.nullToEmpty(longs));
        float[] floats = { 1.0f, 2.0f };
        assertSame(floats, CommonUtil.nullToEmpty(floats));
        double[] doubles = { 1.0, 2.0 };
        assertSame(doubles, CommonUtil.nullToEmpty(doubles));
        String[] strings = { "a", null };
        assertSame(strings, CommonUtil.nullToEmpty(strings));
        assertNull(CommonUtil.nullToEmpty(strings)[1]);
        BigInteger[] bigInts = { BigInteger.ONE };
        assertSame(bigInts, CommonUtil.nullToEmpty(bigInts));
        BigDecimal[] bigDecs = { BigDecimal.ONE };
        assertSame(bigDecs, CommonUtil.nullToEmpty(bigDecs));
        java.util.Date[] dates = { new java.util.Date() };
        assertSame(dates, CommonUtil.nullToEmpty(dates));
        Calendar[] cals = { Calendar.getInstance() };
        assertSame(cals, CommonUtil.nullToEmpty(cals));
        Object[] objects = { new Object(), "string" };
        assertSame(objects, CommonUtil.nullToEmpty(objects));
    }

    @Test
    public void testNullToEmpty_typedArraySharesPreSeededConstantsOnly() {
        Integer[] e1 = CommonUtil.nullToEmpty((Integer[]) null, Integer[].class);
        Integer[] e2 = CommonUtil.nullToEmpty((Integer[]) null, Integer[].class);
        assertSame(e1, e2);
        Integer[] nonNull = { 1, 2 };
        assertSame(nonNull, CommonUtil.nullToEmpty(nonNull, Integer[].class));

        String[] s1 = CommonUtil.nullToEmpty((String[]) null, String[].class);
        String[] s2 = CommonUtil.nullToEmpty((String[]) null, String[].class);
        assertSame(s1, s2);
        assertEquals(String[].class, s1.getClass());
        assertNotSame(e1, s1);
        assertSame(CommonUtil.EMPTY_STRING_ARRAY, s1);
        assertSame(CommonUtil.EMPTY_INT_OBJ_ARRAY, e1);
        assertSame(CommonUtil.EMPTY_OBJECT_ARRAY, CommonUtil.nullToEmpty((Object[]) null, Object[].class));

        java.util.concurrent.atomic.AtomicInteger[] a1 = CommonUtil.nullToEmpty((java.util.concurrent.atomic.AtomicInteger[]) null,
                java.util.concurrent.atomic.AtomicInteger[].class);
        java.util.concurrent.atomic.AtomicInteger[] a2 = CommonUtil.nullToEmpty((java.util.concurrent.atomic.AtomicInteger[]) null,
                java.util.concurrent.atomic.AtomicInteger[].class);
        assertNotSame(a1, a2);
        assertEquals(0, a1.length);
        assertEquals(java.util.concurrent.atomic.AtomicInteger[].class, a1.getClass());
        assertFalse(CommonUtil.CLASS_EMPTY_ARRAY.containsKey(java.util.concurrent.atomic.AtomicInteger.class));
    }

    @Test
    public void testNullToEmpty_immutableCollections() {
        ImmutableList<String> list = ImmutableList.of("a");
        assertSame(list, CommonUtil.nullToEmpty(list));
        assertTrue(CommonUtil.nullToEmpty((ImmutableCollection<?>) null).isEmpty());
        assertTrue(CommonUtil.nullToEmpty((ImmutableList<?>) null).isEmpty());
        assertTrue(CommonUtil.nullToEmpty((ImmutableSet<?>) null).isEmpty());
        assertTrue(CommonUtil.nullToEmpty((ImmutableSortedSet<?>) null).isEmpty());
        assertTrue(CommonUtil.nullToEmpty((ImmutableNavigableSet<?>) null).isEmpty());
        assertTrue(CommonUtil.nullToEmpty((ImmutableMap<?, ?>) null).isEmpty());
        assertTrue(CommonUtil.nullToEmpty((ImmutableSortedMap<?, ?>) null).isEmpty());
        assertTrue(CommonUtil.nullToEmpty((ImmutableNavigableMap<?, ?>) null).isEmpty());
        assertTrue(CommonUtil.nullToEmpty((ImmutableBiMap<?, ?>) null).isEmpty());

        ImmutableSet<String> set = ImmutableSet.of("a");
        assertSame(set, CommonUtil.nullToEmpty(set));
        ImmutableSortedSet<String> sortedSet = ImmutableSortedSet.of("a");
        assertSame(sortedSet, CommonUtil.nullToEmpty(sortedSet));
        ImmutableNavigableSet<String> navSet = ImmutableNavigableSet.of("a");
        assertSame(navSet, CommonUtil.nullToEmpty(navSet));
        ImmutableMap<String, String> map = ImmutableMap.of("a", "b");
        assertSame(map, CommonUtil.nullToEmpty(map));
        ImmutableSortedMap<String, String> sortedMap = ImmutableSortedMap.of("a", "b");
        assertSame(sortedMap, CommonUtil.nullToEmpty(sortedMap));
        ImmutableNavigableMap<String, String> navMap = ImmutableNavigableMap.of("a", "b");
        assertSame(navMap, CommonUtil.nullToEmpty(navMap));
        ImmutableBiMap<String, String> biMap = ImmutableBiMap.of("a", "b");
        assertSame(biMap, CommonUtil.nullToEmpty(biMap));
    }

    @Test
    public void testNullToEmpty_collectionsMapsIterators() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        assertSame(list, CommonUtil.nullToEmpty(list));
        List<?> emptyList = CommonUtil.nullToEmpty((List<?>) null);
        assertTrue(emptyList.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyList.add(null));

        Set<String> set = new HashSet<>(Arrays.asList("a"));
        assertSame(set, CommonUtil.nullToEmpty(set));
        Set<?> emptySet = CommonUtil.nullToEmpty((Set<?>) null);
        assertTrue(emptySet.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptySet.add(null));

        SortedSet<String> sortedSet = new TreeSet<>(Arrays.asList("a"));
        assertSame(sortedSet, CommonUtil.nullToEmpty(sortedSet));
        SortedSet<?> emptySortedSet = CommonUtil.nullToEmpty((SortedSet<?>) null);
        assertTrue(emptySortedSet.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptySortedSet.add(null));

        NavigableSet<String> navSet = new TreeSet<>(Arrays.asList("a"));
        assertSame(navSet, CommonUtil.nullToEmpty(navSet));
        NavigableSet<?> emptyNavSet = CommonUtil.nullToEmpty((NavigableSet<?>) null);
        assertTrue(emptyNavSet.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyNavSet.add(null));

        Collection<String> coll = new ArrayList<>(Arrays.asList("a"));
        assertSame(coll, CommonUtil.nullToEmpty(coll));
        Collection<?> emptyColl = CommonUtil.nullToEmpty((Collection<?>) null);
        assertTrue(emptyColl.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyColl.add(null));

        Map<String, String> map = new HashMap<>();
        map.put("a", "b");
        assertSame(map, CommonUtil.nullToEmpty(map));
        Map<?, ?> emptyMap = CommonUtil.nullToEmpty((Map<?, ?>) null);
        assertTrue(emptyMap.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyMap.put(null, null));

        SortedMap<String, String> sortedMap = new TreeMap<>();
        sortedMap.put("a", "b");
        assertSame(sortedMap, CommonUtil.nullToEmpty(sortedMap));
        SortedMap<?, ?> emptySortedMap = CommonUtil.nullToEmpty((SortedMap<?, ?>) null);
        assertTrue(emptySortedMap.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptySortedMap.put(null, null));

        NavigableMap<String, String> navMap = new TreeMap<>();
        navMap.put("a", "b");
        assertSame(navMap, CommonUtil.nullToEmpty(navMap));
        NavigableMap<?, ?> emptyNavMap = CommonUtil.nullToEmpty((NavigableMap<?, ?>) null);
        assertTrue(emptyNavMap.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyNavMap.put(null, null));

        Iterator<String> it = Arrays.asList("a").iterator();
        assertSame(it, CommonUtil.nullToEmpty(it));
        Iterator<?> emptyIt = CommonUtil.nullToEmpty((Iterator<?>) null);
        assertFalse(emptyIt.hasNext());
        assertThrows(NoSuchElementException.class, emptyIt::next);

        ListIterator<String> lit = Arrays.asList("a").listIterator();
        assertSame(lit, CommonUtil.nullToEmpty(lit));
        ListIterator<?> emptyLit = CommonUtil.nullToEmpty((ListIterator<?>) null);
        assertFalse(emptyLit.hasNext());
        assertFalse(emptyLit.hasPrevious());
        assertThrows(NoSuchElementException.class, emptyLit::next);
        assertThrows(NoSuchElementException.class, emptyLit::previous);
    }

    @Test
    public void testNullFunctionalInterfaceArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newProxyInstance(Runnable.class, null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newProxyInstance(new Class<?>[] { Runnable.class }, null));
        assertThrows(IllegalArgumentException.class, () -> N.splitByChunkCount(1, 1, (com.landawn.abacus.util.function.IntBiFunction<Object>) null));
    }
    @Test
    public void testNewProxyInstance_nullInterfaceThrowsNullPointerException() {
        final InvocationHandler h = (p, m, args) -> null;

        assertThrows(NullPointerException.class, () -> CommonUtil.newProxyInstance((Class<Runnable>) null, h));
        assertThrows(NullPointerException.class, () -> CommonUtil.newProxyInstance((Class<?>[]) null, h));
        assertThrows(NullPointerException.class, () -> CommonUtil.newProxyInstance(new Class<?>[] { null }, h));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newProxyInstance(Runnable.class, null));

        assertNotNull(CommonUtil.newProxyInstance(new Class<?>[0], h));
    }

}
