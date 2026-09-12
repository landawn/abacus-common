package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

public class MultimapToTest extends MultimapTestSupport {
    @Test
    public void testToImmutableMap_WithSupplier_ListMultimap() {
        listMultimap.putValues("a", Arrays.asList(1, 2));
        IntFunction<Map<String, ImmutableList<Integer>>> mapSupplier = LinkedHashMap::new;

        ImmutableMap<String, ImmutableList<Integer>> immutable = listMultimap.toImmutableMap(mapSupplier);
        assertEquals(1, immutable.size());
        assertTrue(immutable.get("a") instanceof ImmutableList);
        assertEquals(ImmutableList.of(1, 2), immutable.get("a"));
    }

    @Test
    public void testToImmutableMap_WithSupplier_SetMultimap() {
        setMultimap.putValues("a", Arrays.asList(1, 2));
        IntFunction<Map<String, ImmutableSet<Integer>>> mapSupplier = LinkedHashMap::new;

        ImmutableMap<String, ImmutableSet<Integer>> immutable = setMultimap.toImmutableMap(mapSupplier);
        assertEquals(1, immutable.size());
        assertTrue(immutable.get("a") instanceof ImmutableSet);
        assertEquals(ImmutableSet.of(1, 2), immutable.get("a"));
    }

    @Test
    public void testToImmutableMap_ListMultimap() {
        listMultimap.putValues("a", Arrays.asList(1, 2));
        listMultimap.put("b", 3);

        ImmutableMap<String, ImmutableList<Integer>> immutable = listMultimap.toImmutableMap();
        assertEquals(2, immutable.size());
        assertTrue(immutable.get("a") instanceof ImmutableList);
        assertEquals(ImmutableList.of(1, 2), immutable.get("a"));
        assertEquals(ImmutableList.of(3), immutable.get("b"));

        assertThrows(UnsupportedOperationException.class, () -> immutable.get("a").add(4));
        assertThrows(UnsupportedOperationException.class, () -> immutable.put("c", ImmutableList.of(5)));
    }

    @Test
    public void testToImmutableMap_SetMultimap() {
        setMultimap.putValues("a", Arrays.asList(1, 2, 1));
        setMultimap.put("b", 3);

        ImmutableMap<String, ImmutableSet<Integer>> immutable = setMultimap.toImmutableMap();
        assertEquals(2, immutable.size());
        assertTrue(immutable.get("a") instanceof ImmutableSet);
        assertEquals(ImmutableSet.of(1, 2), immutable.get("a"));
        assertEquals(ImmutableSet.of(3), immutable.get("b"));

        assertThrows(UnsupportedOperationException.class, () -> immutable.get("a").add(4));
        assertThrows(UnsupportedOperationException.class, () -> immutable.put("c", ImmutableSet.of(5)));
    }

    @Test
    public void testToMultiset_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key1", 30);
        listMultimap.put("key2", 40);

        Multiset<String> multiset = listMultimap.toMultiset();
        assertEquals(3, multiset.count("key1"));
        assertEquals(1, multiset.count("key2"));
    }

    @Test
    public void testToMultiset() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2, 1));
        mm.put("b", 3);
        Multiset<String> ms = mm.toMultiset();
        assertEquals(3, ms.getCount("a"));
        assertEquals(1, ms.getCount("b"));
        assertEquals(2, ms.countOfDistinctElements());
    }

    @Test
    public void testToMultiset_AfterRemoval() {
        listMultimap.put("key1", 10);
        listMultimap.put("key1", 20);
        listMultimap.put("key2", 30);
        listMultimap.removeAll("key1");

        Multiset<String> multiset = listMultimap.toMultiset();
        assertEquals(0, multiset.count("key1"));
        assertEquals(1, multiset.count("key2"));
    }

    @Test
    public void testToMultiset_EmptyMultimap() {
        Multiset<String> multiset = listMultimap.toMultiset();
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testToMap_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        Map<String, List<Integer>> map = listMultimap.toMap();
        assertEquals(2, map.size());
        assertTrue(map.containsKey("key1"));
        assertTrue(map.containsKey("key2"));
    }

    @Test
    public void testToMapWithSupplier_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        HashMap<String, List<Integer>> map = listMultimap.toMap(HashMap::new);
        assertEquals(2, map.size());
    }

    @Test
    public void testToMap_supplier() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        Map<String, List<Integer>> linkedMap = mm.toMap(LinkedHashMap::new);
        assertTrue(linkedMap instanceof LinkedHashMap);
        assertEquals(Arrays.asList(1, 2), linkedMap.get("a"));
    }

    @Test
    public void testToMapWithSupplier() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        TreeMap<String, List<Integer>> map = listMultimap.toMap(size -> new TreeMap<>());
        assertEquals(2, map.size());
        assertTrue(map instanceof TreeMap);
        assertTrue(map.get("key1").contains(10));
        assertTrue(map.get("key2").contains(20));
    }

    @Test
    public void testToMap_EmptyMultimap() {
        Map<String, List<Integer>> map = listMultimap.toMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testToMapWithSupplier_EmptyMultimap() {
        HashMap<String, List<Integer>> map = listMultimap.toMap(HashMap::new);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testToMap_Mutability() {
        listMultimap.put("key1", 10);
        Map<String, List<Integer>> map = listMultimap.toMap();
        map.put("key2", Arrays.asList(20));
        assertNull(listMultimap.get("key2"));
        assertNotNull(map.get("key2"));
    }

    @Test
    public void testToMap() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        Map<String, List<Integer>> map = mm.toMap();
        assertEquals(2, map.size());
        assertEquals(Arrays.asList(1, 2), map.get("a"));
        assertNotSame(mm.get("a"), map.get("a"));
    }

    @Test
    public void testToString() {
        Multimap<String, Integer, List<Integer>> mm = getTestMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        mm.put("b", 3);
        String str = mm.toString();
        assertTrue(str.startsWith("{") && str.endsWith("}"));
        assertTrue(str.contains("a=[1, 2]") || str.contains("a=[2, 1]"));
        assertTrue(str.contains("b=[3]"));
        assertEquals(("{" + "a=[1, 2]" + ", " + "b=[3]" + "}").length(), str.length());

        Multimap<String, Integer, List<Integer>> linkedMm = CommonUtil.newListMultimap(LinkedHashMap.class, ArrayList.class);
        linkedMm.put("z", 10);
        linkedMm.put("y", 20);
        assertEquals("{z=[10], y=[20]}", linkedMm.toString());
    }

    @Test
    public void testToStringFormat() {
        multimap.putValues("key1", Arrays.asList(1, 2));
        multimap.put("key2", 3);

        String str = multimap.toString();
        assertTrue(str.startsWith("{"));
        assertTrue(str.endsWith("}"));
        assertTrue(str.contains("key1"));
        assertTrue(str.contains("key2"));
        assertTrue(str.contains("[1, 2]") || str.contains("[2, 1]"));
        assertTrue(str.contains("[3]"));
    }

    @Test
    public void testToString_EmptyMultimap() {
        String str = listMultimap.toString();
        assertNotNull(str);
        assertEquals("{}", str);
    }

    @Test
    public void testToString_WithValues() {
        listMultimap.put("key1", 10);
        listMultimap.put("key2", 20);

        String str = listMultimap.toString();
        assertNotNull(str);
        assertTrue(str.contains("key1"));
        assertTrue(str.contains("key2"));
    }

    @Test
    public void testToMultisetPreservesMapSupplier() {
        // regression: toMultiset rebuilt the backing map from its Class, losing custom comparators
        // (ClassCastException for non-Comparable keys in comparator-backed TreeMaps)
        final ListMultimap<Object, Integer> mm = CommonUtil
                .newListMultimap(() -> new java.util.TreeMap<>(java.util.Comparator.comparingInt(System::identityHashCode)), java.util.ArrayList::new);
        final Object k = new Object();
        mm.put(k, 1);
        mm.put(k, 2);

        final Multiset<Object> ms = mm.toMultiset();

        assertEquals(2, ms.getCount(k));
    }
}
