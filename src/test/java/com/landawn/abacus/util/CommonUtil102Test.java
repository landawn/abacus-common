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
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.u.Nullable;

@Tag("new-test")
public class CommonUtil102Test extends TestBase {

    @Test
    public void testAnyEmpty_TwoMaps() {
        Map<String, String> map1 = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();

        assertTrue(CommonUtil.anyEmpty(null, map2));
        assertTrue(CommonUtil.anyEmpty(map1, null));
        assertTrue(CommonUtil.anyEmpty(map1, map2));

        map1.put("key", "value");
        assertTrue(CommonUtil.anyEmpty(map1, map2));

        map2.put("key2", "value2");
        assertFalse(CommonUtil.anyEmpty(map1, map2));
    }

    @Test
    public void testAnyEmpty_ThreeMaps() {
        Map<String, String> map1 = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();
        Map<String, String> map3 = new HashMap<>();

        assertTrue(CommonUtil.anyEmpty(null, map2, map3));
        assertTrue(CommonUtil.anyEmpty(map1, null, map3));
        assertTrue(CommonUtil.anyEmpty(map1, map2, null));
        assertTrue(CommonUtil.anyEmpty(map1, map2, map3));

        map1.put("key1", "value1");
        map2.put("key2", "value2");
        assertTrue(CommonUtil.anyEmpty(map1, map2, map3));

        map3.put("key3", "value3");
        assertFalse(CommonUtil.anyEmpty(map1, map2, map3));
    }

    @Test
    public void testAnyBlank_TwoCharSequences() {
        assertTrue(CommonUtil.anyBlank(null, "test"));
        assertTrue(CommonUtil.anyBlank("test", null));
        assertTrue(CommonUtil.anyBlank("", "test"));
        assertTrue(CommonUtil.anyBlank("test", ""));
        assertTrue(CommonUtil.anyBlank(" ", "test"));
        assertTrue(CommonUtil.anyBlank("test", " "));
        assertFalse(CommonUtil.anyBlank("test1", "test2"));
    }

    @Test
    public void testAnyBlank_ThreeCharSequences() {
        assertTrue(CommonUtil.anyBlank(null, "test", "test2"));
        assertTrue(CommonUtil.anyBlank("test", null, "test2"));
        assertTrue(CommonUtil.anyBlank("test", "test2", null));
        assertTrue(CommonUtil.anyBlank("", "test", "test2"));
        assertTrue(CommonUtil.anyBlank(" ", "test", "test2"));
        assertFalse(CommonUtil.anyBlank("test1", "test2", "test3"));
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
    public void testAllNull_TwoObjects() {
        assertTrue(CommonUtil.allNull(null, null));
        assertFalse(CommonUtil.allNull("test", null));
        assertFalse(CommonUtil.allNull(null, "test"));
        assertFalse(CommonUtil.allNull("test1", "test2"));
    }

    @Test
    public void testAllNull_ThreeObjects() {
        assertTrue(CommonUtil.allNull(null, null, null));
        assertFalse(CommonUtil.allNull("test", null, null));
        assertFalse(CommonUtil.allNull(null, "test", null));
        assertFalse(CommonUtil.allNull(null, null, "test"));
        assertFalse(CommonUtil.allNull("test1", "test2", "test3"));
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
    public void testAllNull_Iterable() {
        assertTrue(CommonUtil.allNull((Iterable<?>) null));
        assertTrue(CommonUtil.allNull(new ArrayList<>()));

        List<Object> list = new ArrayList<>();
        list.add(null);
        list.add(null);
        assertTrue(CommonUtil.allNull(list));

        list.add("test");
        assertFalse(CommonUtil.allNull(list));
    }

    @Test
    public void testAllEmpty_TwoCharSequences() {
        assertTrue(CommonUtil.allEmpty((String) null, (String) null));
        assertTrue(CommonUtil.allEmpty("", ""));
        assertTrue(CommonUtil.allEmpty(null, ""));
        assertTrue(CommonUtil.allEmpty("", null));
        assertFalse(CommonUtil.allEmpty("test", ""));
        assertFalse(CommonUtil.allEmpty("", "test"));
        assertFalse(CommonUtil.allEmpty("test1", "test2"));
    }

    @Test
    public void testAllEmpty_ThreeCharSequences() {
        assertTrue(CommonUtil.allEmpty((String) null, (String) null, (String) null));
        assertTrue(CommonUtil.allEmpty("", "", ""));
        assertTrue(CommonUtil.allEmpty(null, "", null));
        assertFalse(CommonUtil.allEmpty("test", "", ""));
        assertFalse(CommonUtil.allEmpty("", "test", ""));
        assertFalse(CommonUtil.allEmpty("", "", "test"));
        assertFalse(CommonUtil.allEmpty("test1", "test2", "test3"));
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
    public void testAllEmpty_CharSequenceIterable() {
        assertTrue(CommonUtil.allEmpty((Iterable<? extends CharSequence>) null));
        assertTrue(CommonUtil.allEmpty(new ArrayList<>()));

        List<CharSequence> list = new ArrayList<>();
        list.add(null);
        list.add("");
        assertTrue(CommonUtil.allEmpty(list));

        list.add("test");
        assertFalse(CommonUtil.allEmpty(list));
    }

    @Test
    public void testAllEmpty_TwoObjectArrays() {
        assertTrue(CommonUtil.allEmpty((Object[]) null, (Object[]) null));
        assertTrue(CommonUtil.allEmpty(new Object[0], new Object[0]));
        assertTrue(CommonUtil.allEmpty((Object[]) null, new Object[0]));
        assertFalse(CommonUtil.allEmpty(new Object[] { "test" }, new Object[0]));
        assertFalse(CommonUtil.allEmpty(new Object[0], new Object[] { "test" }));
        assertFalse(CommonUtil.allEmpty(new Object[] { "test1" }, new Object[] { "test2" }));
    }

    @Test
    public void testAllEmpty_ThreeObjectArrays() {
        assertTrue(CommonUtil.allEmpty((Object[]) null, (Object[]) null, (Object[]) null));
        assertTrue(CommonUtil.allEmpty(new Object[0], new Object[0], new Object[0]));
        assertFalse(CommonUtil.allEmpty(new Object[] { "test" }, new Object[0], new Object[0]));
        assertFalse(CommonUtil.allEmpty(new Object[0], new Object[] { "test" }, new Object[0]));
        assertFalse(CommonUtil.allEmpty(new Object[0], new Object[0], new Object[] { "test" }));
    }

    @Test
    public void testAllEmpty_TwoCollections() {
        assertTrue(CommonUtil.allEmpty((Collection<?>) null, (Collection<?>) null));
        assertTrue(CommonUtil.allEmpty(new ArrayList<>(), new ArrayList<>()));
        assertTrue(CommonUtil.allEmpty((Collection<?>) null, new ArrayList<>()));
        assertFalse(CommonUtil.allEmpty(Arrays.asList("test"), new ArrayList<>()));
        assertFalse(CommonUtil.allEmpty(new ArrayList<>(), Arrays.asList("test")));
    }

    @Test
    public void testAllEmpty_ThreeCollections() {
        assertTrue(CommonUtil.allEmpty((Collection<?>) null, (Collection<?>) null, (Collection<?>) null));
        assertTrue(CommonUtil.allEmpty(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
        assertFalse(CommonUtil.allEmpty(Arrays.asList("test"), new ArrayList<>(), new ArrayList<>()));
        assertFalse(CommonUtil.allEmpty(new ArrayList<>(), Arrays.asList("test"), new ArrayList<>()));
        assertFalse(CommonUtil.allEmpty(new ArrayList<>(), new ArrayList<>(), Arrays.asList("test")));
    }

    @Test
    public void testAllEmpty_TwoMaps() {
        assertTrue(CommonUtil.allEmpty((Map<?, ?>) null, (Map<?, ?>) null));
        assertTrue(CommonUtil.allEmpty(new HashMap<>(), new HashMap<>()));
        assertTrue(CommonUtil.allEmpty((Map<?, ?>) null, new HashMap<>()));

        Map<String, String> map1 = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();
        assertTrue(CommonUtil.allEmpty(map1, map2));

        map1.put("key", "value");
        assertFalse(CommonUtil.allEmpty(map1, map2));
        assertFalse(CommonUtil.allEmpty(map2, map1));
    }

    @Test
    public void testAllEmpty_ThreeMaps() {
        assertTrue(CommonUtil.allEmpty((Map<?, ?>) null, (Map<?, ?>) null, (Map<?, ?>) null));
        assertTrue(CommonUtil.allEmpty(new HashMap<>(), new HashMap<>(), new HashMap<>()));

        Map<String, String> map1 = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();
        Map<String, String> map3 = new HashMap<>();
        assertTrue(CommonUtil.allEmpty(map1, map2, map3));

        map1.put("key", "value");
        assertFalse(CommonUtil.allEmpty(map1, map2, map3));
    }

    @Test
    public void testAllBlank_TwoCharSequences() {
        assertTrue(CommonUtil.allBlank(null, null));
        assertTrue(CommonUtil.allBlank("", ""));
        assertTrue(CommonUtil.allBlank(" ", " "));
        assertTrue(CommonUtil.allBlank(null, ""));
        assertTrue(CommonUtil.allBlank("", " "));
        assertFalse(CommonUtil.allBlank("test", ""));
        assertFalse(CommonUtil.allBlank("", "test"));
        assertFalse(CommonUtil.allBlank("test1", "test2"));
    }

    @Test
    public void testAllBlank_ThreeCharSequences() {
        assertTrue(CommonUtil.allBlank(null, null, null));
        assertTrue(CommonUtil.allBlank("", "", ""));
        assertTrue(CommonUtil.allBlank(" ", " ", " "));
        assertTrue(CommonUtil.allBlank(null, "", " "));
        assertFalse(CommonUtil.allBlank("test", "", ""));
        assertFalse(CommonUtil.allBlank("", "test", ""));
        assertFalse(CommonUtil.allBlank("", "", "test"));
        assertFalse(CommonUtil.allBlank("test1", "test2", "test3"));
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
    public void testAllBlank_CharSequenceIterable() {
        assertTrue(CommonUtil.allBlank((Iterable<? extends CharSequence>) null));
        assertTrue(CommonUtil.allBlank(new ArrayList<>()));

        List<CharSequence> list = new ArrayList<>();
        list.add(null);
        list.add("");
        list.add(" ");
        assertTrue(CommonUtil.allBlank(list));

        list.add("test");
        assertFalse(CommonUtil.allBlank(list));
    }

    @Test
    public void testNullToEmpty_String() {
        assertEquals("", CommonUtil.nullToEmpty((String) null));
        assertEquals("test", CommonUtil.nullToEmpty("test"));
        assertEquals("", CommonUtil.nullToEmpty(""));
    }

    @Test
    public void testNullToEmpty_List() {
        List<String> nullList = null;
        List<String> emptyList = CommonUtil.nullToEmpty(nullList);
        assertNotNull(emptyList);
        assertTrue(emptyList.isEmpty());

        List<String> list = new ArrayList<>();
        list.add("test");
        assertSame(list, CommonUtil.nullToEmpty(list));
    }

    @Test
    public void testNullToEmpty_Set() {
        Set<String> nullSet = null;
        Set<String> emptySet = CommonUtil.nullToEmpty(nullSet);
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());

        Set<String> set = new HashSet<>();
        set.add("test");
        assertSame(set, CommonUtil.nullToEmpty(set));
    }

    @Test
    public void testNullToEmpty_SortedSet() {
        SortedSet<String> nullSet = null;
        SortedSet<String> emptySet = CommonUtil.nullToEmpty(nullSet);
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());

        SortedSet<String> set = new TreeSet<>();
        set.add("test");
        assertSame(set, CommonUtil.nullToEmpty(set));
    }

    @Test
    public void testNullToEmpty_NavigableSet() {
        NavigableSet<String> nullSet = null;
        NavigableSet<String> emptySet = CommonUtil.nullToEmpty(nullSet);
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());

        NavigableSet<String> set = new TreeSet<>();
        set.add("test");
        assertSame(set, CommonUtil.nullToEmpty(set));
    }

    @Test
    public void testNullToEmpty_Collection() {
        Collection<String> nullCollection = null;
        Collection<String> emptyCollection = CommonUtil.nullToEmpty(nullCollection);
        assertNotNull(emptyCollection);
        assertTrue(emptyCollection.isEmpty());

        Collection<String> collection = new ArrayList<>();
        collection.add("test");
        assertSame(collection, CommonUtil.nullToEmpty(collection));
    }

    @Test
    public void testNullToEmpty_Map() {
        Map<String, String> nullMap = null;
        Map<String, String> emptyMap = CommonUtil.nullToEmpty(nullMap);
        assertNotNull(emptyMap);
        assertTrue(emptyMap.isEmpty());

        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertSame(map, CommonUtil.nullToEmpty(map));
    }

    @Test
    public void testNullToEmpty_SortedMap() {
        SortedMap<String, String> nullMap = null;
        SortedMap<String, String> emptyMap = CommonUtil.nullToEmpty(nullMap);
        assertNotNull(emptyMap);
        assertTrue(emptyMap.isEmpty());

        SortedMap<String, String> map = new TreeMap<>();
        map.put("key", "value");
        assertSame(map, CommonUtil.nullToEmpty(map));
    }

    @Test
    public void testNullToEmpty_NavigableMap() {
        NavigableMap<String, String> nullMap = null;
        NavigableMap<String, String> emptyMap = CommonUtil.nullToEmpty(nullMap);
        assertNotNull(emptyMap);
        assertTrue(emptyMap.isEmpty());

        NavigableMap<String, String> map = new TreeMap<>();
        map.put("key", "value");
        assertSame(map, CommonUtil.nullToEmpty(map));
    }

    @Test
    public void testNullToEmpty_Iterator() {
        Iterator<String> nullIterator = null;
        Iterator<String> emptyIterator = CommonUtil.nullToEmpty(nullIterator);
        assertNotNull(emptyIterator);
        assertFalse(emptyIterator.hasNext());

        Iterator<String> iterator = Arrays.asList("test").iterator();
        assertSame(iterator, CommonUtil.nullToEmpty(iterator));
    }

    @Test
    public void testNullToEmpty_ListIterator() {
        ListIterator<String> nullIterator = null;
        ListIterator<String> emptyIterator = CommonUtil.nullToEmpty(nullIterator);
        assertNotNull(emptyIterator);
        assertFalse(emptyIterator.hasNext());

        ListIterator<String> iterator = Arrays.asList("test").listIterator();
        assertSame(iterator, CommonUtil.nullToEmpty(iterator));
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
    public void testNullToEmpty_GenericArray() {
        Integer[] nullArray = null;
        Integer[] emptyArray = CommonUtil.nullToEmpty(nullArray, Integer[].class);
        assertNotNull(emptyArray);
        assertEquals(0, emptyArray.length);

        Integer[] array = { 1, 2, 3 };
        assertSame(array, CommonUtil.nullToEmpty(array, Integer[].class));
    }

    @Test
    public void testNullToEmptyForEach() {
        String[] nullArray = null;
        String[] result = CommonUtil.nullToEmptyForEach(nullArray);
        assertNotNull(result);
        assertEquals(0, result.length);

        String[] arrayWithNulls = { "test", null, "test2", null };
        String[] converted = CommonUtil.nullToEmptyForEach(arrayWithNulls);
        assertEquals("test", converted[0]);
        assertEquals("", converted[1]);
        assertEquals("test2", converted[2]);
        assertEquals("", converted[3]);
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
    public void testDefaultIfNull_Object() {
        String nullStr = null;
        assertEquals("default", CommonUtil.defaultIfNull(nullStr, "default"));
        assertEquals("test", CommonUtil.defaultIfNull("test", "default"));

        assertThrows(NullPointerException.class, () -> CommonUtil.defaultIfNull(nullStr, null));
    }

    @Test
    public void testDefaultIfNull_Supplier() {
        String nullStr = null;
        assertEquals("default", CommonUtil.defaultIfNull(nullStr, Fn.s(() -> "default")));
        assertEquals("test", CommonUtil.defaultIfNull("test", Fn.s(() -> "default")));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfNull(nullStr, () -> null));
    }

    @Test
    public void testDefaultIfEmpty_CharSequence() {
        assertEquals("default", CommonUtil.defaultIfEmpty("", "default"));
        assertEquals("default", CommonUtil.defaultIfEmpty(null, "default"));
        assertEquals("test", CommonUtil.defaultIfEmpty("test", "default"));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty("", ""));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty("", (String) null));
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
    public void testDefaultIfEmpty_Collection() {
        List<String> emptyList = new ArrayList<>();
        List<String> defaultList = Arrays.asList("default");

        assertEquals(defaultList, CommonUtil.defaultIfEmpty(emptyList, defaultList));
        assertEquals(defaultList, CommonUtil.defaultIfEmpty(null, defaultList));

        List<String> list = Arrays.asList("test");
        assertEquals(list, CommonUtil.defaultIfEmpty(list, defaultList));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty(emptyList, new ArrayList<>()));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty(emptyList, null));
    }

    @Test
    public void testDefaultIfEmpty_Map() {
        Map<String, String> emptyMap = new HashMap<>();
        Map<String, String> defaultMap = new HashMap<>();
        defaultMap.put("key", "value");

        assertEquals(defaultMap, CommonUtil.defaultIfEmpty(emptyMap, defaultMap));
        assertEquals(defaultMap, CommonUtil.defaultIfEmpty(null, defaultMap));

        Map<String, String> map = new HashMap<>();
        map.put("test", "value");
        assertEquals(map, CommonUtil.defaultIfEmpty(map, defaultMap));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty(emptyMap, new HashMap<>()));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty(emptyMap, null));
    }

    @Test
    public void testDefaultValueOf() {
        assertEquals(false, CommonUtil.defaultValueOf(boolean.class));
        assertEquals(null, CommonUtil.defaultValueOf(Boolean.class));
        assertEquals('\0', CommonUtil.defaultValueOf(char.class));
        assertEquals(null, CommonUtil.defaultValueOf(Character.class));
        assertEquals((byte) 0, CommonUtil.defaultValueOf(byte.class));
        assertEquals(null, CommonUtil.defaultValueOf(Byte.class));
        assertEquals((short) 0, CommonUtil.defaultValueOf(short.class));
        assertEquals(null, CommonUtil.defaultValueOf(Short.class));
        assertEquals(0, CommonUtil.defaultValueOf(int.class));
        assertEquals(null, CommonUtil.defaultValueOf(Integer.class));
        assertEquals(0L, CommonUtil.defaultValueOf(long.class));
        assertEquals(null, CommonUtil.defaultValueOf(Long.class));
        assertEquals(0f, CommonUtil.defaultValueOf(float.class), 0.0f);
        assertEquals(null, CommonUtil.defaultValueOf(Float.class));
        assertEquals(0d, CommonUtil.defaultValueOf(double.class), 0.0);
        assertEquals(null, CommonUtil.defaultValueOf(Double.class));
        assertEquals(null, CommonUtil.defaultValueOf(String.class));
        assertEquals(null, CommonUtil.defaultValueOf(Object.class));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultValueOf(null));
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
        assertEquals(String.class, stringType.clazz());

        Type<Integer> intType = CommonUtil.typeOf("int");
        assertNotNull(intType);
        assertEquals(int.class, intType.clazz());

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.typeOf((String) null));
    }

    @Test
    public void testTypeOf_Class() {
        Type<String> stringType = CommonUtil.typeOf(String.class);
        assertNotNull(stringType);
        assertEquals(String.class, stringType.clazz());

        Type<Integer> intType = CommonUtil.typeOf(int.class);
        assertNotNull(intType);
        assertEquals(int.class, intType.clazz());

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
    public void testStringOf_Object() {
        assertEquals("test", CommonUtil.stringOf("test"));
        assertEquals("123", CommonUtil.stringOf(Integer.valueOf(123)));
        assertEquals("true", CommonUtil.stringOf(Boolean.TRUE));
        assertNull(CommonUtil.stringOf(null));

        java.util.Date date = new java.util.Date(0);
        String dateStr = CommonUtil.stringOf(date);
        assertNotNull(dateStr);
    }

    @Test
    public void testValueOf() {
        assertEquals(Integer.valueOf(123), CommonUtil.valueOf("123", Integer.class));
        assertEquals(Long.valueOf(123L), CommonUtil.valueOf("123", Long.class));
        assertEquals(Double.valueOf(3.14), CommonUtil.valueOf("3.14", Double.class));
        assertEquals(Boolean.TRUE, CommonUtil.valueOf("true", Boolean.class));
        assertEquals("test", CommonUtil.valueOf("test", String.class));

        assertEquals(0, CommonUtil.valueOf(null, int.class));
        assertEquals(null, CommonUtil.valueOf(null, Integer.class));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.valueOf("test", null));
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
    public void testCastIfAssignable() {
        String str = "test";
        Nullable<String> strResult = CommonUtil.castIfAssignable(str, String.class);
        assertTrue(strResult.isPresent());
        assertEquals("test", strResult.get());

        Integer num = 123;
        Nullable<Number> numResult = CommonUtil.castIfAssignable(num, Number.class);
        assertTrue(numResult.isPresent());
        assertEquals(123, numResult.get());

        Nullable<Integer> failedCast = CommonUtil.castIfAssignable("test", Integer.class);
        assertFalse(failedCast.isPresent());

        Nullable<String> nullResult = CommonUtil.castIfAssignable(null, String.class);
        assertTrue(nullResult.isPresent());
        assertNull(nullResult.get());

        Nullable<Integer> primitiveResult = CommonUtil.castIfAssignable(123, int.class);
        assertTrue(primitiveResult.isPresent());
        assertEquals(123, primitiveResult.get());

        Nullable<Integer> nullPrimitiveResult = CommonUtil.castIfAssignable(null, int.class);
        assertFalse(nullPrimitiveResult.isPresent());
    }

    @Test
    public void testCastIfAssignable_WithType() {
        Type<String> strType = CommonUtil.typeOf(String.class);
        Nullable<String> strResult = CommonUtil.castIfAssignable("test", strType);
        assertTrue(strResult.isPresent());
        assertEquals("test", strResult.get());

        Type<Integer> intType = CommonUtil.typeOf(Integer.class);
        Nullable<Integer> failedCast = CommonUtil.castIfAssignable("test", intType);
        assertFalse(failedCast.isPresent());
    }

    @Test
    public void testClone_Object() {
        List<String> list = new ArrayList<>();
        list.add("test1");
        list.add("test2");

        List<String> cloned = Beans.clone(list);
        assertNotSame(list, cloned);
        assertEquals(list, cloned);

        assertNull(Beans.clone((Object) null));
    }

    @Test
    public void testClone_WithTargetType() {
        List<String> list = new ArrayList<>();
        list.add("test1");
        list.add("test2");

        ArrayList<String> cloned = Beans.clone(list, ArrayList.class);
        assertNotSame(list, cloned);
        assertEquals(list, cloned);
        assertTrue(cloned instanceof ArrayList);

        TestBean bean = Beans.clone(null, TestBean.class);
        assertNotNull(bean);

        assertThrows(IllegalArgumentException.class, () -> Beans.clone("test", null));
    }

    @Test
    public void testCopy_SameType() {
        TestBean source = new TestBean();
        source.setName("test");
        source.setValue(123);

        TestBean copy = Beans.copy(source);
        assertNotNull(copy);
        assertNotSame(source, copy);
        assertEquals("test", copy.getName());
        assertEquals(123, copy.getValue());

        assertNull(Beans.copy((TestBean) null));
    }

    @Test
    public void testCopy_WithSelectPropNames() {
        TestBean source = new TestBean();
        source.setName("test");
        source.setValue(123);

        TestBean copy = Beans.copy(source, Arrays.asList("name"));
        assertNotNull(copy);
        assertEquals("test", copy.getName());
        assertEquals(0, copy.getValue());

        assertNull(Beans.copy(null, Arrays.asList("name")));
    }

    @Test
    public void testCopy_WithPropFilter() {
        TestBean source = new TestBean();
        source.setName("test");
        source.setValue(123);

        BiPredicate<String, Object> filter = (name, value) -> "name".equals(name);
        TestBean copy = Beans.copy(source, filter);
        assertNotNull(copy);
        assertEquals("test", copy.getName());
        assertEquals(0, copy.getValue());

        assertNull(Beans.copy(null, filter));
    }

    @Test
    public void testCopy_ToTargetType() {
        TestBean source = new TestBean();
        source.setName("test");
        source.setValue(123);

        TestBean2 copy = Beans.copy(source, TestBean2.class);
        assertNotNull(copy);
        assertEquals("test", copy.getName());

        TestBean2 nullCopy = Beans.copy(null, TestBean2.class);
        assertNotNull(nullCopy);

        assertThrows(IllegalArgumentException.class, () -> Beans.copy(source, (Class<TestBean>) null));
    }

    @Test
    public void testCopy_WithIgnoreUnmatchedProperty() {
        TestBean source = new TestBean();
        source.setName("test");
        source.setValue(123);

        TestBean2 copy = Beans.copy(source, true, null, TestBean2.class);
        assertNotNull(copy);
        assertEquals("test", copy.getName());

        Set<String> ignoredProps = new HashSet<>();
        ignoredProps.add("name");
        TestBean2 copy2 = Beans.copy(source, true, ignoredProps, TestBean2.class);
        assertNotNull(copy2);
        assertNull(copy2.getName());

        assertThrows(IllegalArgumentException.class, () -> Beans.copy(source, true, null, null));
    }

    @Test
    public void testMerge_Basic() {
        TestBean source = new TestBean();
        source.setName("source");
        source.setValue(100);

        TestBean target = new TestBean();
        target.setName("target");
        target.setValue(200);

        Beans.merge(source, target);
        assertEquals("source", target.getName());
        assertEquals(100, target.getValue());

        assertThrows(IllegalArgumentException.class, () -> Beans.merge(source, null));
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
        Beans.merge(source, target, mergeFunc);
        assertEquals("source", target.getName());
        assertEquals(100, target.getValue());

        assertThrows(IllegalArgumentException.class, () -> Beans.merge(source, null, mergeFunc));
    }

    @Test
    public void testMerge_WithSelectPropNames() {
        TestBean source = new TestBean();
        source.setName("source");
        source.setValue(100);

        TestBean target = new TestBean();
        target.setName("target");
        target.setValue(200);

        Beans.merge(source, target, Arrays.asList("name"));
        assertEquals("source", target.getName());
        assertEquals(200, target.getValue());

        assertThrows(IllegalArgumentException.class, () -> Beans.merge(source, null, Arrays.asList("name")));
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
        Beans.merge(source, target, filter);
        assertEquals("source", target.getName());
        assertEquals(200, target.getValue());

        assertThrows(IllegalArgumentException.class, () -> Beans.merge(source, null, filter));
    }

    @Test
    public void testMerge_WithIgnoreUnmatchedProperty() {
        TestBean source = new TestBean();
        source.setName("source");
        source.setValue(100);

        TestBean target = new TestBean();
        target.setName("target");
        target.setValue(200);

        Beans.merge(source, target, true, null);
        assertEquals("source", target.getName());
        assertEquals(100, target.getValue());

        Set<String> ignoredProps = new HashSet<>();
        ignoredProps.add("name");
        TestBean target2 = new TestBean();
        target2.setName("target");
        target2.setValue(200);
        Beans.merge(source, target2, true, ignoredProps);
        assertEquals("target", target2.getName());
        assertEquals(100, target2.getValue());

        assertThrows(IllegalArgumentException.class, () -> Beans.merge(source, null, true, null));
    }

    @Test
    public void testErase_Array() {
        TestBean bean = new TestBean();
        bean.setName("test");
        bean.setValue(123);

        Beans.erase(bean, "name");
        assertNull(bean.getName());
        assertEquals(123, bean.getValue());

        bean.setName("test");
        Beans.erase(bean, "name", "value");
        assertNull(bean.getName());
        assertEquals(0, bean.getValue());

        Beans.erase(null, "name");
        Beans.erase(bean);
    }

    @Test
    public void testErase_Collection() {
        TestBean bean = new TestBean();
        bean.setName("test");
        bean.setValue(123);

        Beans.erase(bean, Arrays.asList("name"));
        assertNull(bean.getName());
        assertEquals(123, bean.getValue());

        bean.setName("test");
        Beans.erase(bean, Arrays.asList("name", "value"));
        assertNull(bean.getName());
        assertEquals(0, bean.getValue());

        Beans.erase(null, Arrays.asList("name"));
        Beans.erase(bean, new ArrayList<>());
    }

    @Test
    public void testEraseAll() {
        TestBean bean = new TestBean();
        bean.setName("test");
        bean.setValue(123);

        Beans.eraseAll(bean);
        assertNull(bean.getName());
        assertEquals(0, bean.getValue());

        Beans.eraseAll(null);
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
    public void testGetPropNames_ObjectWithIgnoreNull() {
        TestBean bean = new TestBean();
        bean.setName("test");
        bean.setValue(0);

        List<String> propNames = Beans.getPropNames(bean, false);
        assertNotNull(propNames);
        assertTrue(propNames.contains("name"));
        assertTrue(propNames.contains("value"));

        List<String> nonNullPropNames = Beans.getPropNames(bean, true);
        assertNotNull(nonNullPropNames);
        assertTrue(nonNullPropNames.contains("name"));
        assertTrue(nonNullPropNames.contains("value"));

        bean.setName(null);
        List<String> nonNullPropNames2 = Beans.getPropNames(bean, true);
        assertFalse(nonNullPropNames2.contains("name"));
        assertTrue(nonNullPropNames2.contains("value"));
    }

    @Test
    public void testGetPropValue() {
        TestBean bean = new TestBean();
        bean.setName("test");
        bean.setValue(123);

        assertEquals("test", Beans.getPropValue(bean, "name"));
        assertEquals(123, (Integer) Beans.getPropValue(bean, "value"));

        assertThrows(NullPointerException.class, () -> Beans.getPropValue(null, "name"));
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
    public void testSetPropValue() {
        TestBean bean = new TestBean();

        Beans.setPropValue(bean, "name", "newName");
        assertEquals("newName", bean.getName());

        Beans.setPropValue(bean, "value", 999);
        assertEquals(999, bean.getValue());

        assertThrows(NullPointerException.class, () -> Beans.setPropValue(null, "name", "value"));
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
    public void testEnumListOf() {
        ImmutableList<TestEnum> list = CommonUtil.enumListOf(TestEnum.class);
        assertNotNull(list);
        assertEquals(3, list.size());
        assertTrue(list.contains(TestEnum.ONE));
        assertTrue(list.contains(TestEnum.TWO));
        assertTrue(list.contains(TestEnum.THREE));

        assertSame(list, CommonUtil.enumListOf(TestEnum.class));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.enumListOf(null));
    }

    @Test
    public void testEnumSetOf() {
        ImmutableSet<TestEnum> set = CommonUtil.enumSetOf(TestEnum.class);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertTrue(set.contains(TestEnum.ONE));
        assertTrue(set.contains(TestEnum.TWO));
        assertTrue(set.contains(TestEnum.THREE));

        assertSame(set, CommonUtil.enumSetOf(TestEnum.class));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.enumSetOf(null));
    }

    @Test
    public void testEnumMapOf() {
        ImmutableBiMap<TestEnum, String> map = CommonUtil.enumMapOf(TestEnum.class);
        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals("ONE", map.get(TestEnum.ONE));
        assertEquals("TWO", map.get(TestEnum.TWO));
        assertEquals("THREE", map.get(TestEnum.THREE));
        assertEquals(TestEnum.ONE, map.getByValue("ONE"));
        assertEquals(TestEnum.TWO, map.getByValue("TWO"));
        assertEquals(TestEnum.THREE, map.getByValue("THREE"));

        assertSame(map, CommonUtil.enumMapOf(TestEnum.class));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.enumMapOf(null));
    }

    @Test
    public void testUnmodifiableCollection() {
        Collection<String> nullCollection = null;
        Collection<String> unmodifiable = CommonUtil.unmodifiableCollection(nullCollection);
        assertNotNull(unmodifiable);
        assertTrue(unmodifiable.isEmpty());

        List<String> list = new ArrayList<>();
        list.add("test");
        Collection<String> unmodifiableList = CommonUtil.unmodifiableCollection(list);
        assertNotNull(unmodifiableList);
        assertEquals(1, unmodifiableList.size());
        assertTrue(unmodifiableList.contains("test"));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableList.add("new"));
    }

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
