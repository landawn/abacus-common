package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.u.Nullable;

public class CommonUtil102Test extends TestBase {

    @Test
    public void testAnyEmpty_TwoMaps() {
        Map<String, String> map1 = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();
        
        assertTrue(N.anyEmpty(null, map2));
        assertTrue(N.anyEmpty(map1, null));
        assertTrue(N.anyEmpty(map1, map2));
        
        map1.put("key", "value");
        assertTrue(N.anyEmpty(map1, map2));
        
        map2.put("key2", "value2");
        assertFalse(N.anyEmpty(map1, map2));
    }

    @Test
    public void testAnyEmpty_ThreeMaps() {
        Map<String, String> map1 = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();
        Map<String, String> map3 = new HashMap<>();
        
        assertTrue(N.anyEmpty(null, map2, map3));
        assertTrue(N.anyEmpty(map1, null, map3));
        assertTrue(N.anyEmpty(map1, map2, null));
        assertTrue(N.anyEmpty(map1, map2, map3));
        
        map1.put("key1", "value1");
        map2.put("key2", "value2");
        assertTrue(N.anyEmpty(map1, map2, map3));
        
        map3.put("key3", "value3");
        assertFalse(N.anyEmpty(map1, map2, map3));
    }

    @Test
    public void testAnyBlank_TwoCharSequences() {
        assertTrue(N.anyBlank(null, "test"));
        assertTrue(N.anyBlank("test", null));
        assertTrue(N.anyBlank("", "test"));
        assertTrue(N.anyBlank("test", ""));
        assertTrue(N.anyBlank(" ", "test"));
        assertTrue(N.anyBlank("test", " "));
        assertFalse(N.anyBlank("test1", "test2"));
    }

    @Test
    public void testAnyBlank_ThreeCharSequences() {
        assertTrue(N.anyBlank(null, "test", "test2"));
        assertTrue(N.anyBlank("test", null, "test2"));
        assertTrue(N.anyBlank("test", "test2", null));
        assertTrue(N.anyBlank("", "test", "test2"));
        assertTrue(N.anyBlank(" ", "test", "test2"));
        assertFalse(N.anyBlank("test1", "test2", "test3"));
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
        assertFalse(N.anyBlank(new ArrayList<CharSequence>()));
        
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
    public void testAllNull_TwoObjects() {
        assertTrue(N.allNull(null, null));
        assertFalse(N.allNull("test", null));
        assertFalse(N.allNull(null, "test"));
        assertFalse(N.allNull("test1", "test2"));
    }

    @Test
    public void testAllNull_ThreeObjects() {
        assertTrue(N.allNull(null, null, null));
        assertFalse(N.allNull("test", null, null));
        assertFalse(N.allNull(null, "test", null));
        assertFalse(N.allNull(null, null, "test"));
        assertFalse(N.allNull("test1", "test2", "test3"));
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
    public void testAllNull_Iterable() {
        assertTrue(N.allNull((Iterable<?>) null));
        assertTrue(N.allNull(new ArrayList<>()));
        
        List<Object> list = new ArrayList<>();
        list.add(null);
        list.add(null);
        assertTrue(N.allNull(list));
        
        list.add("test");
        assertFalse(N.allNull(list));
    }

    @Test
    public void testAllEmpty_TwoCharSequences() {
        assertTrue(N.allEmpty((String) null, (String) null));
        assertTrue(N.allEmpty("", ""));
        assertTrue(N.allEmpty(null, ""));
        assertTrue(N.allEmpty("", null));
        assertFalse(N.allEmpty("test", ""));
        assertFalse(N.allEmpty("", "test"));
        assertFalse(N.allEmpty("test1", "test2"));
    }

    @Test
    public void testAllEmpty_ThreeCharSequences() {
        assertTrue(N.allEmpty((String) null, (String) null, (String) null));
        assertTrue(N.allEmpty("", "", ""));
        assertTrue(N.allEmpty(null, "", null));
        assertFalse(N.allEmpty("test", "", ""));
        assertFalse(N.allEmpty("", "test", ""));
        assertFalse(N.allEmpty("", "", "test"));
        assertFalse(N.allEmpty("test1", "test2", "test3"));
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

    @Test
    public void testAllEmpty_CharSequenceIterable() {
        assertTrue(N.allEmpty((Iterable<? extends CharSequence>) null));
        assertTrue(N.allEmpty(new ArrayList<CharSequence>()));
        
        List<CharSequence> list = new ArrayList<>();
        list.add(null);
        list.add("");
        assertTrue(N.allEmpty(list));
        
        list.add("test");
        assertFalse(N.allEmpty(list));
    }

    @Test
    public void testAllEmpty_TwoObjectArrays() {
        assertTrue(N.allEmpty((Object[]) null, (Object[]) null));
        assertTrue(N.allEmpty(new Object[0], new Object[0]));
        assertTrue(N.allEmpty((Object[]) null, new Object[0]));
        assertFalse(N.allEmpty(new Object[] { "test" }, new Object[0]));
        assertFalse(N.allEmpty(new Object[0], new Object[] { "test" }));
        assertFalse(N.allEmpty(new Object[] { "test1" }, new Object[] { "test2" }));
    }

    @Test
    public void testAllEmpty_ThreeObjectArrays() {
        assertTrue(N.allEmpty((Object[]) null, (Object[]) null, (Object[]) null));
        assertTrue(N.allEmpty(new Object[0], new Object[0], new Object[0]));
        assertFalse(N.allEmpty(new Object[] { "test" }, new Object[0], new Object[0]));
        assertFalse(N.allEmpty(new Object[0], new Object[] { "test" }, new Object[0]));
        assertFalse(N.allEmpty(new Object[0], new Object[0], new Object[] { "test" }));
    }

    @Test
    public void testAllEmpty_TwoCollections() {
        assertTrue(N.allEmpty((Collection<?>) null, (Collection<?>) null));
        assertTrue(N.allEmpty(new ArrayList<>(), new ArrayList<>()));
        assertTrue(N.allEmpty((Collection<?>) null, new ArrayList<>()));
        assertFalse(N.allEmpty(Arrays.asList("test"), new ArrayList<>()));
        assertFalse(N.allEmpty(new ArrayList<>(), Arrays.asList("test")));
    }

    @Test
    public void testAllEmpty_ThreeCollections() {
        assertTrue(N.allEmpty((Collection<?>) null, (Collection<?>) null, (Collection<?>) null));
        assertTrue(N.allEmpty(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        assertFalse(N.allEmpty(Arrays.asList("test"), new ArrayList<>(), new ArrayList<>()));
        assertFalse(N.allEmpty(new ArrayList<>(), Arrays.asList("test"), new ArrayList<>()));
        assertFalse(N.allEmpty(new ArrayList<>(), new ArrayList<>(), Arrays.asList("test")));
    }

    @Test
    public void testAllEmpty_TwoMaps() {
        assertTrue(N.allEmpty((Map<?, ?>) null, (Map<?, ?>) null));
        assertTrue(N.allEmpty(new HashMap<>(), new HashMap<>()));
        assertTrue(N.allEmpty((Map<?, ?>) null, new HashMap<>()));
        
        Map<String, String> map1 = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();
        assertTrue(N.allEmpty(map1, map2));
        
        map1.put("key", "value");
        assertFalse(N.allEmpty(map1, map2));
        assertFalse(N.allEmpty(map2, map1));
    }

    @Test
    public void testAllEmpty_ThreeMaps() {
        assertTrue(N.allEmpty((Map<?, ?>) null, (Map<?, ?>) null, (Map<?, ?>) null));
        assertTrue(N.allEmpty(new HashMap<>(), new HashMap<>(), new HashMap<>()));
        
        Map<String, String> map1 = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();
        Map<String, String> map3 = new HashMap<>();
        assertTrue(N.allEmpty(map1, map2, map3));
        
        map1.put("key", "value");
        assertFalse(N.allEmpty(map1, map2, map3));
    }

    @Test
    public void testAllBlank_TwoCharSequences() {
        assertTrue(N.allBlank(null, null));
        assertTrue(N.allBlank("", ""));
        assertTrue(N.allBlank(" ", " "));
        assertTrue(N.allBlank(null, ""));
        assertTrue(N.allBlank("", " "));
        assertFalse(N.allBlank("test", ""));
        assertFalse(N.allBlank("", "test"));
        assertFalse(N.allBlank("test1", "test2"));
    }

    @Test
    public void testAllBlank_ThreeCharSequences() {
        assertTrue(N.allBlank(null, null, null));
        assertTrue(N.allBlank("", "", ""));
        assertTrue(N.allBlank(" ", " ", " "));
        assertTrue(N.allBlank(null, "", " "));
        assertFalse(N.allBlank("test", "", ""));
        assertFalse(N.allBlank("", "test", ""));
        assertFalse(N.allBlank("", "", "test"));
        assertFalse(N.allBlank("test1", "test2", "test3"));
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
    public void testAllBlank_CharSequenceIterable() {
        assertTrue(N.allBlank((Iterable<? extends CharSequence>) null));
        assertTrue(N.allBlank(new ArrayList<CharSequence>()));
        
        List<CharSequence> list = new ArrayList<>();
        list.add(null);
        list.add("");
        list.add(" ");
        assertTrue(N.allBlank(list));
        
        list.add("test");
        assertFalse(N.allBlank(list));
    }

    @Test
    public void testNullToEmpty_String() {
        assertEquals("", N.nullToEmpty((String) null));
        assertEquals("test", N.nullToEmpty("test"));
        assertEquals("", N.nullToEmpty(""));
    }

    @Test
    public void testNullToEmpty_List() {
        List<String> nullList = null;
        List<String> emptyList = N.nullToEmpty(nullList);
        assertNotNull(emptyList);
        assertTrue(emptyList.isEmpty());
        
        List<String> list = new ArrayList<>();
        list.add("test");
        assertSame(list, N.nullToEmpty(list));
    }

    @Test
    public void testNullToEmpty_Set() {
        Set<String> nullSet = null;
        Set<String> emptySet = N.nullToEmpty(nullSet);
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());
        
        Set<String> set = new HashSet<>();
        set.add("test");
        assertSame(set, N.nullToEmpty(set));
    }

    @Test
    public void testNullToEmpty_SortedSet() {
        SortedSet<String> nullSet = null;
        SortedSet<String> emptySet = N.nullToEmpty(nullSet);
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());
        
        SortedSet<String> set = new TreeSet<>();
        set.add("test");
        assertSame(set, N.nullToEmpty(set));
    }

    @Test
    public void testNullToEmpty_NavigableSet() {
        NavigableSet<String> nullSet = null;
        NavigableSet<String> emptySet = N.nullToEmpty(nullSet);
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());
        
        NavigableSet<String> set = new TreeSet<>();
        set.add("test");
        assertSame(set, N.nullToEmpty(set));
    }

    @Test
    public void testNullToEmpty_Collection() {
        Collection<String> nullCollection = null;
        Collection<String> emptyCollection = N.nullToEmpty(nullCollection);
        assertNotNull(emptyCollection);
        assertTrue(emptyCollection.isEmpty());
        
        Collection<String> collection = new ArrayList<>();
        collection.add("test");
        assertSame(collection, N.nullToEmpty(collection));
    }

    @Test
    public void testNullToEmpty_Map() {
        Map<String, String> nullMap = null;
        Map<String, String> emptyMap = N.nullToEmpty(nullMap);
        assertNotNull(emptyMap);
        assertTrue(emptyMap.isEmpty());
        
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertSame(map, N.nullToEmpty(map));
    }

    @Test
    public void testNullToEmpty_SortedMap() {
        SortedMap<String, String> nullMap = null;
        SortedMap<String, String> emptyMap = N.nullToEmpty(nullMap);
        assertNotNull(emptyMap);
        assertTrue(emptyMap.isEmpty());
        
        SortedMap<String, String> map = new TreeMap<>();
        map.put("key", "value");
        assertSame(map, N.nullToEmpty(map));
    }

    @Test
    public void testNullToEmpty_NavigableMap() {
        NavigableMap<String, String> nullMap = null;
        NavigableMap<String, String> emptyMap = N.nullToEmpty(nullMap);
        assertNotNull(emptyMap);
        assertTrue(emptyMap.isEmpty());
        
        NavigableMap<String, String> map = new TreeMap<>();
        map.put("key", "value");
        assertSame(map, N.nullToEmpty(map));
    }

    @Test
    public void testNullToEmpty_Iterator() {
        Iterator<String> nullIterator = null;
        Iterator<String> emptyIterator = N.nullToEmpty(nullIterator);
        assertNotNull(emptyIterator);
        assertFalse(emptyIterator.hasNext());
        
        Iterator<String> iterator = Arrays.asList("test").iterator();
        assertSame(iterator, N.nullToEmpty(iterator));
    }

    @Test
    public void testNullToEmpty_ListIterator() {
        ListIterator<String> nullIterator = null;
        ListIterator<String> emptyIterator = N.nullToEmpty(nullIterator);
        assertNotNull(emptyIterator);
        assertFalse(emptyIterator.hasNext());
        
        ListIterator<String> iterator = Arrays.asList("test").listIterator();
        assertSame(iterator, N.nullToEmpty(iterator));
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
        
        boolean[] boolArray = {true, false};
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
        
        String[] strArray = {"test1", "test2"};
        assertSame(strArray, N.nullToEmpty(strArray));
    }

    @Test
    public void testNullToEmpty_GenericArray() {
        Integer[] nullArray = null;
        Integer[] emptyArray = N.nullToEmpty(nullArray, Integer[].class);
        assertNotNull(emptyArray);
        assertEquals(0, emptyArray.length);
        
        Integer[] array = {1, 2, 3};
        assertSame(array, N.nullToEmpty(array, Integer[].class));
    }

    @Test
    public void testNullToEmptyForEach() {
        String[] nullArray = null;
        String[] result = N.nullToEmptyForEach(nullArray);
        assertNotNull(result);
        assertEquals(0, result.length);
        
        String[] arrayWithNulls = {"test", null, "test2", null};
        String[] converted = N.nullToEmptyForEach(arrayWithNulls);
        assertEquals("test", converted[0]);
        assertEquals("", converted[1]);
        assertEquals("test2", converted[2]);
        assertEquals("", converted[3]);
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
        String nullStr = null;
        assertEquals("default", N.defaultIfNull(nullStr, "default"));
        assertEquals("test", N.defaultIfNull("test", "default"));
        
        assertThrows(NullPointerException.class, () -> N.defaultIfNull(nullStr, null));
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
        assertEquals("default", N.defaultIfEmpty("", "default"));
        assertEquals("default", N.defaultIfEmpty(null, "default"));
        assertEquals("test", N.defaultIfEmpty("test", "default"));
        
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfEmpty("", ""));
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfEmpty("", (String) null));
    }

    @Test
    public void testDefaultIfEmpty_CharSequenceSupplier() {
        assertEquals("default", N.defaultIfEmpty("", Fn.s(Fn.s(() -> "default"))));
        assertEquals("default", N.defaultIfEmpty((String) null, Fn.s(() -> "default")));
        assertEquals("test", N.defaultIfEmpty("test", Fn.s(() -> "default")));
        
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfEmpty("", () -> ""));
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfEmpty("", () -> null));
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
        
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfBlank(" ", () -> " "));
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfBlank(" ", () -> null));
    }

    @Test
    public void testDefaultIfEmpty_Collection() {
        List<String> emptyList = new ArrayList<>();
        List<String> defaultList = Arrays.asList("default");
        
        assertEquals(defaultList, N.defaultIfEmpty(emptyList, defaultList));
        assertEquals(defaultList, N.defaultIfEmpty(null, defaultList));
        
        List<String> list = Arrays.asList("test");
        assertEquals(list, N.defaultIfEmpty(list, defaultList));
        
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfEmpty(emptyList, new ArrayList<>()));
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfEmpty(emptyList, null));
    }

    @Test
    public void testDefaultIfEmpty_Map() {
        Map<String, String> emptyMap = new HashMap<>();
        Map<String, String> defaultMap = new HashMap<>();
        defaultMap.put("key", "value");
        
        assertEquals(defaultMap, N.defaultIfEmpty(emptyMap, defaultMap));
        assertEquals(defaultMap, N.defaultIfEmpty(null, defaultMap));
        
        Map<String, String> map = new HashMap<>();
        map.put("test", "value");
        assertEquals(map, N.defaultIfEmpty(map, defaultMap));
        
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfEmpty(emptyMap, new HashMap<>()));
        assertThrows(IllegalArgumentException.class, () -> N.defaultIfEmpty(emptyMap, null));
    }

    @Test
    public void testDefaultValueOf() {
        assertEquals(false, N.defaultValueOf(boolean.class));
        assertEquals(null, N.defaultValueOf(Boolean.class));
        assertEquals('\0', N.defaultValueOf(char.class));
        assertEquals(null, N.defaultValueOf(Character.class));
        assertEquals((byte) 0, N.defaultValueOf(byte.class));
        assertEquals(null, N.defaultValueOf(Byte.class));
        assertEquals((short) 0, N.defaultValueOf(short.class));
        assertEquals(null, N.defaultValueOf(Short.class));
        assertEquals(0, N.defaultValueOf(int.class));
        assertEquals(null, N.defaultValueOf(Integer.class));
        assertEquals(0L, N.defaultValueOf(long.class));
        assertEquals(null, N.defaultValueOf(Long.class));
        assertEquals(0f, N.defaultValueOf(float.class), 0.0f);
        assertEquals(null, N.defaultValueOf(Float.class));
        assertEquals(0d, N.defaultValueOf(double.class), 0.0);
        assertEquals(null, N.defaultValueOf(Double.class));
        assertEquals(null, N.defaultValueOf(String.class));
        assertEquals(null, N.defaultValueOf(Object.class));
        
        assertThrows(IllegalArgumentException.class, () -> N.defaultValueOf(null));
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
    public void testTypeOf_String() {
        Type<String> stringType = N.typeOf("java.lang.String");
        assertNotNull(stringType);
        assertEquals(String.class, stringType.clazz());
        
        Type<Integer> intType = N.typeOf("int");
        assertNotNull(intType);
        assertEquals(int.class, intType.clazz());
        
        assertThrows(IllegalArgumentException.class, () -> N.typeOf((String) null));
    }

    @Test
    public void testTypeOf_Class() {
        Type<String> stringType = N.typeOf(String.class);
        assertNotNull(stringType);
        assertEquals(String.class, stringType.clazz());
        
        Type<Integer> intType = N.typeOf(int.class);
        assertNotNull(intType);
        assertEquals(int.class, intType.clazz());
        
        assertThrows(IllegalArgumentException.class, () -> N.typeOf((Class<?>) null));
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
    public void testStringOf_Object() {
        assertEquals("test", N.stringOf("test"));
        assertEquals("123", N.stringOf(Integer.valueOf(123)));
        assertEquals("true", N.stringOf(Boolean.TRUE));
        assertNull(N.stringOf(null));
        
        java.util.Date date = new java.util.Date(0);
        String dateStr = N.stringOf(date);
        assertNotNull(dateStr);
    }

    @Test
    public void testValueOf() {
        assertEquals(Integer.valueOf(123), N.valueOf("123", Integer.class));
        assertEquals(Long.valueOf(123L), N.valueOf("123", Long.class));
        assertEquals(Double.valueOf(3.14), N.valueOf("3.14", Double.class));
        assertEquals(Boolean.TRUE, N.valueOf("true", Boolean.class));
        assertEquals("test", N.valueOf("test", String.class));
        
        assertEquals(0, N.valueOf(null, int.class));
        assertEquals(null, N.valueOf(null, Integer.class));
        
        assertThrows(IllegalArgumentException.class, () -> N.valueOf("test", null));
    }

    //    @Test
    //    public void testRegisterConverter() {
    //        // Create a custom class for testing
    //        class CustomClass {
    //            String value;
    //            CustomClass(String value) {
    //                this.value = value;
    //            }
    //        }
    //        
    //        // Register a converter
    //        assertTrue(N.registerConverter(CustomClass.class,
    //            (obj, targetClass) -> {
    //                if (targetClass == String.class) {
    //                    return ((CustomClass) obj).value;
    //                }
    //                return obj;
    //            }));
    //        
    //        // Should return false when registering again
    //        assertFalse(N.registerConverter(CustomClass.class,
    //            (obj, targetClass) -> obj));
    //        
    //        // Test the converter
    //        CustomClass custom = new CustomClass("test");
    //        assertEquals("test", N.convert(custom, String.class));
    //        
    //        // Test registering converter for built-in class should throw exception
    //        assertThrows(IllegalArgumentException.class, 
    //                () -> N.registerConverter(String.class, (obj, targetClass) -> obj));
    //            
    //        assertThrows(IllegalArgumentException.class, 
    //                () -> N.registerConverter(null, (obj, targetClass) -> obj));
    //            
    //        assertThrows(IllegalArgumentException.class, 
    //                () -> N.registerConverter(CustomClass.class, null));
    //    }

    @Test
    public void testConvert_Basic() {
        // Null conversions
        assertEquals(0, N.convert(null, int.class));
        assertEquals(null, N.convert(null, Integer.class));
        assertEquals(null, N.convert(null, String.class));
        
        // String conversions
        assertEquals(Integer.valueOf(123), N.convert("123", Integer.class));
        assertEquals(Long.valueOf(123L), N.convert("123", Long.class));
        assertEquals(Double.valueOf(3.14), N.convert("3.14", Double.class));
        assertEquals(Boolean.TRUE, N.convert("true", Boolean.class));
        
        // Number conversions
        assertEquals(Integer.valueOf(123), N.convert(123L, Integer.class));
        assertEquals(Long.valueOf(123L), N.convert(123, Long.class));
        assertEquals(Float.valueOf(3.14f), N.convert(3.14d, Float.class));
        assertEquals(Double.valueOf(3.14d), N.convert(3.14f, Double.class));
        
        // Boolean conversions
        assertEquals(true, N.convert(1, boolean.class));
        assertEquals(false, N.convert(0, boolean.class));
        assertEquals(true, N.convert(5L, Boolean.class));
        
        // Character conversions
        assertEquals(Character.valueOf('A'), N.convert(65, Character.class));
        assertEquals(Integer.valueOf(65), N.convert('A', Integer.class));
    }

    @Test
    public void testConvert_Collections() {
        // List to List
        List<String> strList = Arrays.asList("1", "2", "3");
        List<String> convertedList = N.convert(strList, List.class);
        assertEquals(strList, convertedList);
        
        // Set to Set
        Set<String> strSet = new HashSet<>(Arrays.asList("1", "2", "3"));
        Set<String> convertedSet = N.convert(strSet, Set.class);
        assertEquals(strSet, convertedSet);
        
        // Array to Collection
        String[] strArray = {"1", "2", "3"};
        Collection<String> collection = N.convert(strArray, Collection.class);
        assertEquals(3, collection.size());
        assertTrue(collection.contains("1"));
        assertTrue(collection.contains("2"));
        assertTrue(collection.contains("3"));
        
        // Collection to Array
        List<String> list = Arrays.asList("1", "2", "3");
        String[] array = N.convert(list, String[].class);
        assertArrayEquals(new String[]{"1", "2", "3"}, array);
    }

    @Test
    public void testConvert_Maps() {
        // Map to Map
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        Map<String, Integer> convertedMap = N.convert(map, Map.class);
        assertEquals(map, convertedMap);
    }

    @Test
    public void testConvert_Dates() {
        // Long to Date conversions
        long timestamp = 1000000L;
        assertEquals(new java.util.Date(timestamp), N.convert(timestamp, java.util.Date.class));
        assertEquals(new java.sql.Timestamp(timestamp), N.convert(timestamp, java.sql.Timestamp.class));
        assertEquals(new java.sql.Date(timestamp), N.convert(timestamp, java.sql.Date.class));
        assertEquals(new java.sql.Time(timestamp), N.convert(timestamp, java.sql.Time.class));
        
        // Date to Long conversion
        java.util.Date date = new java.util.Date(timestamp);
        assertEquals(Long.valueOf(timestamp), N.convert(date, Long.class));
    }

    @Test
    public void testConvert_WithType() {
        Type<Integer> intType = N.typeOf(int.class);
        assertEquals(Integer.valueOf(123), N.convert("123", intType));
        assertEquals(0, N.convert(null, intType));
        
        Type<List> listType = N.typeOf(List.class);
        String[] array = {"1", "2", "3"};
        List<String> list = N.convert(array, listType);
        assertEquals(3, list.size());
    }

    @Test
    public void testCastIfAssignable() {
        // Successful casts
        String str = "test";
        Nullable<String> strResult = N.castIfAssignable(str, String.class);
        assertTrue(strResult.isPresent());
        assertEquals("test", strResult.get());
        
        Integer num = 123;
        Nullable<Number> numResult = N.castIfAssignable(num, Number.class);
        assertTrue(numResult.isPresent());
        assertEquals(123, numResult.get());
        
        // Failed casts
        Nullable<Integer> failedCast = N.castIfAssignable("test", Integer.class);
        assertFalse(failedCast.isPresent());
        
        // Null handling
        Nullable<String> nullResult = N.castIfAssignable(null, String.class);
        assertTrue(nullResult.isPresent());
        assertNull(nullResult.get());
        
        // Primitive type handling
        Nullable<Integer> primitiveResult = N.castIfAssignable(123, int.class);
        assertTrue(primitiveResult.isPresent());
        assertEquals(123, primitiveResult.get());
        
        Nullable<Integer> nullPrimitiveResult = N.castIfAssignable(null, int.class);
        assertFalse(nullPrimitiveResult.isPresent());
    }

    @Test
    public void testCastIfAssignable_WithType() {
        Type<String> strType = N.typeOf(String.class);
        Nullable<String> strResult = N.castIfAssignable("test", strType);
        assertTrue(strResult.isPresent());
        assertEquals("test", strResult.get());
        
        Type<Integer> intType = N.typeOf(Integer.class);
        Nullable<Integer> failedCast = N.castIfAssignable("test", intType);
        assertFalse(failedCast.isPresent());
    }

    @Test
    public void testClone_Object() {
        // Simple object clone
        List<String> list = new ArrayList<>();
        list.add("test1");
        list.add("test2");
        
        List<String> cloned = N.clone(list);
        assertNotSame(list, cloned);
        assertEquals(list, cloned);
        
        // Null handling
        assertNull(N.clone((Object) null));
    }

    @Test
    public void testClone_WithTargetType() {
        // Clone to same type
        List<String> list = new ArrayList<>();
        list.add("test1");
        list.add("test2");
        
        ArrayList<String> cloned = N.clone(list, ArrayList.class);
        assertNotSame(list, cloned);
        assertEquals(list, cloned);
        assertTrue(cloned instanceof ArrayList);
        
        // Clone null to bean type
        TestBean bean = N.clone(null, TestBean.class);
        assertNotNull(bean);
        
        assertThrows(IllegalArgumentException.class, () -> N.clone("test", null));
    }

    @Test
    public void testCopy_SameType() {
        TestBean source = new TestBean();
        source.setName("test");
        source.setValue(123);
        
        TestBean copy = N.copy(source);
        assertNotNull(copy);
        assertNotSame(source, copy);
        assertEquals("test", copy.getName());
        assertEquals(123, copy.getValue());
        
        assertNull(N.copy((TestBean) null));
    }

    @Test
    public void testCopy_WithSelectPropNames() {
        TestBean source = new TestBean();
        source.setName("test");
        source.setValue(123);
        
        TestBean copy = N.copy(source, Arrays.asList("name"));
        assertNotNull(copy);
        assertEquals("test", copy.getName());
        assertEquals(0, copy.getValue()); // Not copied
        
        assertNull(N.copy(null, Arrays.asList("name")));
    }

    @Test
    public void testCopy_WithPropFilter() {
        TestBean source = new TestBean();
        source.setName("test");
        source.setValue(123);
        
        BiPredicate<String, Object> filter = (name, value) -> "name".equals(name);
        TestBean copy = N.copy(source, filter);
        assertNotNull(copy);
        assertEquals("test", copy.getName());
        assertEquals(0, copy.getValue()); // Filtered out
        
        assertNull(N.copy(null, filter));
    }

    @Test
    public void testCopy_ToTargetType() {
        TestBean source = new TestBean();
        source.setName("test");
        source.setValue(123);
        
        TestBean2 copy = N.copy(source, TestBean2.class);
        assertNotNull(copy);
        assertEquals("test", copy.getName());
        
        TestBean2 nullCopy = N.copy(null, TestBean2.class);
        assertNotNull(nullCopy);
        
        assertThrows(IllegalArgumentException.class, () -> N.copy(source, (Class<TestBean>) null));
    }

    @Test
    public void testCopy_WithIgnoreUnmatchedProperty() {
        TestBean source = new TestBean();
        source.setName("test");
        source.setValue(123);
        
        TestBean2 copy = N.copy(source, true, null, TestBean2.class);
        assertNotNull(copy);
        assertEquals("test", copy.getName());
        
        Set<String> ignoredProps = new HashSet<>();
        ignoredProps.add("name");
        TestBean2 copy2 = N.copy(source, true, ignoredProps, TestBean2.class);
        assertNotNull(copy2);
        assertNull(copy2.getName());
        
        assertThrows(IllegalArgumentException.class, () -> N.copy(source, true, null, null));
    }

    @Test
    public void testMerge_Basic() {
        TestBean source = new TestBean();
        source.setName("source");
        source.setValue(100);
        
        TestBean target = new TestBean();
        target.setName("target");
        target.setValue(200);
        
        N.merge(source, target);
        assertEquals("source", target.getName());
        assertEquals(100, target.getValue());
        
        assertThrows(IllegalArgumentException.class, () -> N.merge(source, null));
    }

    @Test
    public void testMerge_WithMergeFunction() {
        TestBean source = new TestBean();
        source.setName("source");
        source.setValue(100);
        
        TestBean target = new TestBean();
        target.setName("target");
        target.setValue(200);
        
        BinaryOperator<Object> mergeFunc = (src, tgt) -> src != null ? src : tgt;
        N.merge(source, target, mergeFunc);
        assertEquals("source", target.getName());
        assertEquals(100, target.getValue());
        
        assertThrows(IllegalArgumentException.class, () -> N.merge(source, null, mergeFunc));
    }

    @Test
    public void testMerge_WithSelectPropNames() {
        TestBean source = new TestBean();
        source.setName("source");
        source.setValue(100);
        
        TestBean target = new TestBean();
        target.setName("target");
        target.setValue(200);
        
        N.merge(source, target, Arrays.asList("name"));
        assertEquals("source", target.getName());
        assertEquals(200, target.getValue()); // Not merged
        
        assertThrows(IllegalArgumentException.class, () -> N.merge(source, null, Arrays.asList("name")));
    }

    @Test
    public void testMerge_WithPropFilter() {
        TestBean source = new TestBean();
        source.setName("source");
        source.setValue(100);
        
        TestBean target = new TestBean();
        target.setName("target");
        target.setValue(200);
        
        BiPredicate<String, Object> filter = (name, value) -> "name".equals(name);
        N.merge(source, target, filter);
        assertEquals("source", target.getName());
        assertEquals(200, target.getValue()); // Filtered out
        
        assertThrows(IllegalArgumentException.class, () -> N.merge(source, null, filter));
    }

    @Test
    public void testMerge_WithIgnoreUnmatchedProperty() {
        TestBean source = new TestBean();
        source.setName("source");
        source.setValue(100);
        
        TestBean target = new TestBean();
        target.setName("target");
        target.setValue(200);
        
        N.merge(source, target, true, null);
        assertEquals("source", target.getName());
        assertEquals(100, target.getValue());
        
        Set<String> ignoredProps = new HashSet<>();
        ignoredProps.add("name");
        TestBean target2 = new TestBean();
        target2.setName("target");
        target2.setValue(200);
        N.merge(source, target2, true, ignoredProps);
        assertEquals("target", target2.getName()); // Ignored
        assertEquals(100, target2.getValue());
        
        assertThrows(IllegalArgumentException.class, () -> N.merge(source, null, true, null));
    }

    @Test
    public void testErase_Array() {
        TestBean bean = new TestBean();
        bean.setName("test");
        bean.setValue(123);
        
        N.erase(bean, "name");
        assertNull(bean.getName());
        assertEquals(123, bean.getValue());
        
        bean.setName("test");
        N.erase(bean, "name", "value");
        assertNull(bean.getName());
        assertEquals(0, bean.getValue());
        
        N.erase(null, "name"); // Should not throw
        N.erase(bean); // Empty array, should not throw
    }

    @Test
    public void testErase_Collection() {
        TestBean bean = new TestBean();
        bean.setName("test");
        bean.setValue(123);
        
        N.erase(bean, Arrays.asList("name"));
        assertNull(bean.getName());
        assertEquals(123, bean.getValue());
        
        bean.setName("test");
        N.erase(bean, Arrays.asList("name", "value"));
        assertNull(bean.getName());
        assertEquals(0, bean.getValue());
        
        N.erase(null, Arrays.asList("name")); // Should not throw
        N.erase(bean, new ArrayList<>()); // Empty collection, should not throw
    }

    @Test
    public void testEraseAll() {
        TestBean bean = new TestBean();
        bean.setName("test");
        bean.setValue(123);
        
        N.eraseAll(bean);
        assertNull(bean.getName());
        assertEquals(0, bean.getValue());
        
        N.eraseAll(null); // Should not throw
    }

    @Test
    public void testGetPropNames_Class() {
        ImmutableList<String> propNames = N.getPropNames(TestBean.class);
        assertNotNull(propNames);
        assertTrue(propNames.contains("name"));
        assertTrue(propNames.contains("value"));
        
        assertThrows(IllegalArgumentException.class, () -> N.getPropNames((Class<?>) null));
    }

    @Test
    public void testGetPropNames_ClassWithExclude() {
        Set<String> exclude = new HashSet<>();
        exclude.add("name");
        
        List<String> propNames = N.getPropNames(TestBean.class, exclude);
        assertNotNull(propNames);
        assertFalse(propNames.contains("name"));
        assertTrue(propNames.contains("value"));
        
        assertThrows(IllegalArgumentException.class, () -> N.getPropNames(null, exclude));
    }

    @Test
    public void testGetPropNames_Object() {
        TestBean bean = new TestBean();
        List<String> propNames = N.getPropNames(bean);
        assertNotNull(propNames);
        assertTrue(propNames.contains("name"));
        assertTrue(propNames.contains("value"));
        
        assertThrows(IllegalArgumentException.class, () -> N.getPropNames((Object) null));
    }

    @Test
    public void testGetPropNames_ObjectWithIgnoreNull() {
        TestBean bean = new TestBean();
        bean.setName("test");
        bean.setValue(0);
        
        List<String> propNames = N.getPropNames(bean, false);
        assertNotNull(propNames);
        assertTrue(propNames.contains("name"));
        assertTrue(propNames.contains("value"));
        
        List<String> nonNullPropNames = N.getPropNames(bean, true);
        assertNotNull(nonNullPropNames);
        assertTrue(nonNullPropNames.contains("name"));
        assertTrue(nonNullPropNames.contains("value"));
        
        bean.setName(null);
        List<String> nonNullPropNames2 = N.getPropNames(bean, true);
        assertFalse(nonNullPropNames2.contains("name"));
        assertTrue(nonNullPropNames2.contains("value"));
    }

    @Test
    public void testGetPropValue() {
        TestBean bean = new TestBean();
        bean.setName("test");
        bean.setValue(123);
        
        assertEquals("test", N.getPropValue(bean, "name"));
        assertEquals(123, (Integer) N.getPropValue(bean, "value"));
        
        assertThrows(NullPointerException.class, () -> N.getPropValue(null, "name"));
    }

    @Test
    public void testGetPropValue_WithIgnoreUnmatched() {
        TestBean bean = new TestBean();
        bean.setName("test");
        
        assertEquals("test", N.getPropValue(bean, "name", false));
        assertEquals("test", N.getPropValue(bean, "name", true));
        
        assertThrows(RuntimeException.class, () -> N.getPropValue(bean, "nonExistent", false));
        assertNull(N.getPropValue(bean, "nonExistent", true));
        
        assertThrows(NullPointerException.class, () -> N.getPropValue(null, "name", true));
    }

    @Test
    public void testSetPropValue() {
        TestBean bean = new TestBean();
        
        N.setPropValue(bean, "name", "newName");
        assertEquals("newName", bean.getName());
        
        N.setPropValue(bean, "value", 999);
        assertEquals(999, bean.getValue());
        
        assertThrows(NullPointerException.class, () -> N.setPropValue(null, "name", "value"));
    }

    @Test
    public void testNegate_Boolean() {
        assertEquals(Boolean.FALSE, N.negate(Boolean.TRUE));
        assertEquals(Boolean.TRUE, N.negate(Boolean.FALSE));
        assertNull(N.negate((Boolean) null));
    }

    @Test
    public void testNegate_BooleanArray() {
        boolean[] array = {true, false, true, false};
        N.negate(array);
        assertArrayEquals(new boolean[]{false, true, false, true}, array);
        
        boolean[] emptyArray = {};
        N.negate(emptyArray); // Should not throw
        
        N.negate((boolean[]) null); // Should not throw
    }

    @Test
    public void testNegate_BooleanArrayRange() {
        boolean[] array = {true, false, true, false, true};
        N.negate(array, 1, 4);
        assertArrayEquals(new boolean[]{true, true, false, true, true}, array);
        
        N.negate(array, 0, 0); // Empty range, should not throw
        
        assertThrows(IndexOutOfBoundsException.class, () -> N.negate(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.negate(array, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> N.negate(array, 3, 2));
    }

    @Test
    public void testEnumListOf() {
        ImmutableList<TestEnum> list = N.enumListOf(TestEnum.class);
        assertNotNull(list);
        assertEquals(3, list.size());
        assertTrue(list.contains(TestEnum.ONE));
        assertTrue(list.contains(TestEnum.TWO));
        assertTrue(list.contains(TestEnum.THREE));
        
        // Test caching
        assertSame(list, N.enumListOf(TestEnum.class));
        
        assertThrows(IllegalArgumentException.class, () -> N.enumListOf(null));
    }

    @Test
    public void testEnumSetOf() {
        ImmutableSet<TestEnum> set = N.enumSetOf(TestEnum.class);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertTrue(set.contains(TestEnum.ONE));
        assertTrue(set.contains(TestEnum.TWO));
        assertTrue(set.contains(TestEnum.THREE));
        
        // Test caching
        assertSame(set, N.enumSetOf(TestEnum.class));
        
        assertThrows(IllegalArgumentException.class, () -> N.enumSetOf(null));
    }

    @Test
    public void testEnumMapOf() {
        ImmutableBiMap<TestEnum, String> map = N.enumMapOf(TestEnum.class);
        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals("ONE", map.get(TestEnum.ONE));
        assertEquals("TWO", map.get(TestEnum.TWO));
        assertEquals("THREE", map.get(TestEnum.THREE));
        assertEquals(TestEnum.ONE, map.getByValue("ONE"));
        assertEquals(TestEnum.TWO, map.getByValue("TWO"));
        assertEquals(TestEnum.THREE, map.getByValue("THREE"));
        
        // Test caching
        assertSame(map, N.enumMapOf(TestEnum.class));
        
        assertThrows(IllegalArgumentException.class, () -> N.enumMapOf(null));
    }

    @Test
    public void testUnmodifiableCollection() {
        Collection<String> nullCollection = null;
        Collection<String> unmodifiable = N.unmodifiableCollection(nullCollection);
        assertNotNull(unmodifiable);
        assertTrue(unmodifiable.isEmpty());
        
        List<String> list = new ArrayList<>();
        list.add("test");
        Collection<String> unmodifiableList = N.unmodifiableCollection(list);
        assertNotNull(unmodifiableList);
        assertEquals(1, unmodifiableList.size());
        assertTrue(unmodifiableList.contains("test"));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableList.add("new"));
    }

    // Test helper classes
    public static class TestBean {
        private String name;
        private int value;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public int getValue() {
            return value;
        }
        
        public void setValue(int value) {
            this.value = value;
        }
    }
    
    public static class TestBean2 {
        private String name;
        private String description;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
    
    enum TestEnum {
        ONE, TWO, THREE
    }
}
