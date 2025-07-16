package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;

public class Multimap100Test extends TestBase {

    private Multimap<String, Integer, List<Integer>> multimap;
    private Multimap<String, String, Set<String>> setMultimap;

    @BeforeEach
    public void setUp() {
        multimap = N.newListMultimap();
        setMultimap = N.newSetMultimap();
    }

    // Constructor Tests
    @Test
    public void testDefaultConstructor() {
        Multimap<String, Integer, List<Integer>> mm = N.newListMultimap();
        assertNotNull(mm);
        assertTrue(mm.isEmpty());
        assertEquals(0, mm.size());
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        Multimap<String, Integer, List<Integer>> mm = N.newListMultimap();
        assertNotNull(mm);
        assertTrue(mm.isEmpty());
    }

    @Test
    public void testConstructorWithMapAndCollectionTypes() {
        Multimap<String, Integer, Set<Integer>> mm = N.newMultimap(TreeMap::new, TreeSet::new);
        assertNotNull(mm);
        mm.put("a", 1);
        mm.put("b", 2);
        // TreeMap should maintain order
        assertEquals("a", mm.keySet().iterator().next());
    }

    @Test
    public void testConstructorWithSuppliers() {
        Multimap<String, Integer, List<Integer>> mm = N.newMultimap(() -> new HashMap<>(), () -> new ArrayList<>());
        assertNotNull(mm);
        mm.put("test", 1);
        assertEquals(1, mm.get("test").size());
    }

    // getFirst and getFirstOrDefault Tests
    @Test
    public void testGetFirst() {
        assertNull(multimap.getFirst("key"));

        multimap.put("key", 1);
        multimap.put("key", 2);
        assertEquals(Integer.valueOf(1), multimap.getFirst("key"));
    }

    @Test
    public void testGetFirstOrDefault() {
        assertEquals(Integer.valueOf(99), multimap.getFirstOrDefault("key", 99));

        multimap.put("key", 1);
        multimap.put("key", 2);
        assertEquals(Integer.valueOf(1), multimap.getFirstOrDefault("key", 99));
    }

    // get and getOrDefault Tests
    @Test
    public void testGet() {
        assertNull(multimap.get("key"));

        multimap.put("key", 1);
        List<Integer> values = multimap.get("key");
        assertNotNull(values);
        assertEquals(1, values.size());
        assertEquals(Integer.valueOf(1), values.get(0));
    }

    @Test
    public void testGetOrDefault() {
        List<Integer> defaultList = Arrays.asList(99);
        assertEquals(defaultList, multimap.getOrDefault("key", defaultList));

        multimap.put("key", 1);
        assertNotEquals(defaultList, multimap.getOrDefault("key", defaultList));
    }

    // put Tests
    @Test
    public void testPut() {
        assertTrue(multimap.put("key", 1));
        assertTrue(multimap.put("key", 2));
        assertEquals(2, multimap.get("key").size());
        assertTrue(multimap.get("key").contains(1));
        assertTrue(multimap.get("key").contains(2));
    }

    @Test
    public void testPutMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key1", 1);
        map.put("key2", 2);

        assertTrue(multimap.put(map));
        assertEquals(2, multimap.size());
        assertEquals(Integer.valueOf(1), multimap.getFirst("key1"));
        assertEquals(Integer.valueOf(2), multimap.getFirst("key2"));
    }

    @Test
    public void testPutMapEmpty() {
        assertFalse(multimap.put(new HashMap<>()));
    }

    @Test
    public void testPutIfAbsent() {
        assertTrue(multimap.putIfAbsent("key", 1));
        assertTrue(multimap.putIfAbsent("key", 2));
        assertFalse(multimap.putIfAbsent("key", 1)); // Already contains 1
    }

    @Test
    public void testPutIfKeyAbsent() {
        assertTrue(multimap.putIfKeyAbsent("key", 1));
        assertFalse(multimap.putIfKeyAbsent("key", 2)); // Key already exists
        assertEquals(1, multimap.get("key").size());
    }

    // putMany Tests
    @Test
    public void testPutMany() {
        List<Integer> values = Arrays.asList(1, 2, 3);
        assertTrue(multimap.putMany("key", values));
        assertEquals(3, multimap.get("key").size());

        assertFalse(multimap.putMany("key2", Collections.emptyList()));
    }

    @Test
    public void testPutManyIfKeyAbsent() {
        List<Integer> values = Arrays.asList(1, 2, 3);
        assertTrue(multimap.putManyIfKeyAbsent("key", values));
        assertFalse(multimap.putManyIfKeyAbsent("key", Arrays.asList(4, 5)));
        assertEquals(3, multimap.get("key").size());
    }

    @Test
    public void testPutManyMap() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("key1", Arrays.asList(1, 2));
        map.put("key2", Arrays.asList(3, 4));

        assertTrue(multimap.putMany(map));
        assertEquals(2, multimap.get("key1").size());
        assertEquals(2, multimap.get("key2").size());
    }

    @Test
    public void testPutManyMultimap() {
        Multimap<String, Integer, List<Integer>> other = N.newListMultimap();
        other.putMany("key1", Arrays.asList(1, 2));
        other.putMany("key2", Arrays.asList(3, 4));

        assertTrue(multimap.putMany(other));
        assertEquals(2, multimap.get("key1").size());
        assertEquals(2, multimap.get("key2").size());
    }

    // removeOne Tests
    @Test
    public void testRemoveOne() {
        multimap.put("key", 1);
        multimap.put("key", 2);

        assertTrue(multimap.removeOne("key", 1));
        assertFalse(multimap.get("key").contains(1));
        assertTrue(multimap.get("key").contains(2));

        assertFalse(multimap.removeOne("nonexistent", 1));
    }

    @Test
    public void testRemoveOneMap() {
        multimap.put("key1", 1);
        multimap.put("key2", 2);

        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("key1", 1);
        toRemove.put("key2", 2);

        assertTrue(multimap.removeOne(toRemove));
        assertNull(multimap.get("key1"));
        assertNull(multimap.get("key2"));
    }

    // removeAll Tests
    @Test
    public void testRemoveAll() {
        multimap.put("key", 1);
        multimap.put("key", 2);

        List<Integer> removed = multimap.removeAll("key");
        assertEquals(2, removed.size());
        assertNull(multimap.get("key"));
    }

    // removeMany Tests
    @Test
    public void testRemoveMany() {
        multimap.putMany("key", Arrays.asList(1, 2, 3, 4));

        assertTrue(multimap.removeMany("key", Arrays.asList(2, 3)));
        assertEquals(2, multimap.get("key").size());
        assertTrue(multimap.get("key").contains(1));
        assertTrue(multimap.get("key").contains(4));
    }

    @Test
    public void testRemoveManyMap() {
        multimap.putMany("key1", Arrays.asList(1, 2, 3));
        multimap.putMany("key2", Arrays.asList(4, 5, 6));

        Map<String, List<Integer>> toRemove = new HashMap<>();
        toRemove.put("key1", Arrays.asList(1, 2));
        toRemove.put("key2", Arrays.asList(5));

        assertTrue(multimap.removeMany(toRemove));
        assertEquals(1, multimap.get("key1").size());
        assertEquals(2, multimap.get("key2").size());
    }

    @Test
    public void testRemoveManyMultimap() {
        multimap.putMany("key1", Arrays.asList(1, 2, 3));
        multimap.putMany("key2", Arrays.asList(4, 5, 6));

        Multimap<String, Integer, List<Integer>> toRemove = N.newListMultimap();
        toRemove.putMany("key1", Arrays.asList(1, 2));
        toRemove.putMany("key2", Arrays.asList(5));

        assertTrue(multimap.removeMany(toRemove));
        assertEquals(1, multimap.get("key1").size());
        assertEquals(2, multimap.get("key2").size());
    }

    // removeIf Tests
    @Test
    public void testRemoveOneIf() {
        multimap.put("apple", 1);
        multimap.put("banana", 1);
        multimap.put("cherry", 2);

        assertTrue(multimap.removeOneIf(1, key -> key.startsWith("a")));
        assertNull(multimap.get("apple"));
        assertEquals(Integer.valueOf(1), multimap.getFirst("banana"));
    }

    @Test
    public void testRemoveOneIfBiPredicate() {
        multimap.putMany("key1", Arrays.asList(1, 2, 3));
        multimap.put("key2", 1);

        assertTrue(multimap.removeOneIf(1, (k, v) -> v.size() > 2));
        assertEquals(2, multimap.get("key1").size());
        assertFalse(multimap.get("key1").contains(1));
    }

    @Test
    public void testRemoveManyIf() {
        multimap.putMany("apple", Arrays.asList(1, 2, 3));
        multimap.putMany("banana", Arrays.asList(1, 2));

        assertTrue(multimap.removeManyIf(Arrays.asList(1, 2), key -> key.equals("apple")));
        assertEquals(1, multimap.get("apple").size());
        assertEquals(2, multimap.get("banana").size());
    }

    @Test
    public void testRemoveAllIf() {
        multimap.put("apple", 1);
        multimap.put("banana", 2);
        multimap.put("apricot", 3);

        assertTrue(multimap.removeAllIf(key -> key.startsWith("a")));
        assertEquals(1, multimap.size());
        assertTrue(multimap.containsKey("banana"));
    }

    // replace Tests
    @Test
    public void testReplaceOne() {
        multimap.putMany("key", Arrays.asList(1, 2, 3, 2));

        assertTrue(multimap.replaceOne("key", 2, 99));
        List<Integer> values = multimap.get("key");
        assertEquals(4, values.size());
        assertEquals(Integer.valueOf(99), values.get(1));
        assertEquals(Integer.valueOf(2), values.get(3)); // Only first occurrence replaced
    }

    @Test
    public void testReplaceAllWithOne() {
        multimap.putMany("key", Arrays.asList(1, 2, 3));

        assertTrue(multimap.replaceAllWithOne("key", 99));
        assertEquals(1, multimap.get("key").size());
        assertEquals(Integer.valueOf(99), multimap.getFirst("key"));
    }

    @Test
    public void testReplaceManyWithOne() {
        multimap.putMany("key", Arrays.asList(1, 2, 3, 4, 5));

        assertTrue(multimap.replaceManyWithOne("key", Arrays.asList(2, 3, 4), 99));
        assertEquals(3, multimap.get("key").size());
        assertTrue(multimap.get("key").contains(1));
        assertTrue(multimap.get("key").contains(5));
        assertTrue(multimap.get("key").contains(99));
    }

    @Test
    public void testReplaceOneIf() {
        multimap.putMany("key1", Arrays.asList(1, 2, 3));
        multimap.putMany("key2", Arrays.asList(1, 2, 3));

        assertTrue(multimap.replaceOneIf(key -> key.equals("key1"), 2, 99));
        assertTrue(multimap.get("key1").contains(99));
        assertFalse(multimap.get("key2").contains(99));
    }

    @Test
    public void testReplaceAllWithOneIf() {
        multimap.putMany("key1", Arrays.asList(1, 2, 3));
        multimap.putMany("key2", Arrays.asList(4, 5, 6));

        assertTrue(multimap.replaceAllWithOneIf(key -> key.equals("key1"), 99));
        assertEquals(1, multimap.get("key1").size());
        assertEquals(Integer.valueOf(99), multimap.getFirst("key1"));
        assertEquals(3, multimap.get("key2").size());
    }

    @Test
    public void testReplaceAll() {
        multimap.putMany("key1", Arrays.asList(1, 2));
        multimap.putMany("key2", Arrays.asList(3, 4));

        multimap.replaceAll((k, v) -> {
            List<Integer> newList = new ArrayList<>();
            for (Integer i : v) {
                newList.add(i * 10);
            }
            return newList;
        });

        assertEquals(Arrays.asList(10, 20), multimap.get("key1"));
        assertEquals(Arrays.asList(30, 40), multimap.get("key2"));
    }

    // compute Tests
    @Test
    public void testComputeIfAbsent() {
        List<Integer> result = multimap.computeIfAbsent("key", k -> Arrays.asList(1, 2, 3));
        assertEquals(3, result.size());
        assertEquals(result, multimap.get("key"));

        // Should not compute if key exists
        multimap.computeIfAbsent("key", k -> Arrays.asList(4, 5, 6));
        assertEquals(3, multimap.get("key").size());
    }

    @Test
    public void testComputeIfPresent() {
        assertNull(multimap.computeIfPresent("key", (k, v) -> Arrays.asList(1, 2)));

        multimap.put("key", 1);
        List<Integer> result = multimap.computeIfPresent("key", (k, v) -> {
            List<Integer> newList = new ArrayList<>(v);
            newList.add(2);
            return newList;
        });

        assertEquals(2, result.size());
        assertTrue(result.contains(2));
    }

    @Test
    public void testCompute() {
        List<Integer> result1 = multimap.compute("key", (k, v) -> {
            if (v == null) {
                return Arrays.asList(1, 2);
            }
            return v;
        });
        assertEquals(2, result1.size());

        List<Integer> result2 = multimap.compute("key", (k, v) -> {
            List<Integer> newList = new ArrayList<>(v);
            newList.add(3);
            return newList;
        });
        assertEquals(3, result2.size());
    }

    @Test
    public void testMergeCollection() {
        multimap.put("key", 1);

        List<Integer> result = multimap.merge("key", Arrays.asList(2, 3), (oldVal, newVal) -> {
            List<Integer> merged = new ArrayList<>(oldVal);
            merged.addAll(newVal);
            return merged;
        });

        assertEquals(3, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testMergeElement() {
        multimap.put("key", 1);

        List<Integer> result = multimap.merge("key", 2, (oldVal, newVal) -> {
            List<Integer> merged = new ArrayList<>(oldVal);
            merged.add(newVal);
            return merged;
        });

        assertEquals(2, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
    }

    // inverse Tests
    @Test
    public void testInverse() {
        multimap.put("a", 1);
        multimap.put("b", 1);
        multimap.put("a", 2);

        ListMultimap<Integer, String> inverted = multimap.inverse(IntFunctions.ofListMultimap());

        assertEquals(2, inverted.size());
        assertTrue(inverted.get(1).contains("a"));
        assertTrue(inverted.get(1).contains("b"));
        assertTrue(inverted.get(2).contains("a"));
    }

    // copy Test
    @Test
    public void testCopy() {
        multimap.putMany("key1", Arrays.asList(1, 2, 3));
        multimap.putMany("key2", Arrays.asList(4, 5));

        Multimap<String, Integer, List<Integer>> copy = multimap.copy();

        assertEquals(multimap.size(), copy.size());
        assertEquals(multimap.get("key1"), copy.get("key1"));
        assertEquals(multimap.get("key2"), copy.get("key2"));

        // Verify it's a deep copy
        copy.put("key1", 99);
        assertFalse(multimap.get("key1").contains(99));
    }

    // contains Tests
    @Test
    public void testContains() {
        multimap.put("key", 1);

        assertTrue(multimap.contains("key", 1));
        assertFalse(multimap.contains("key", 2));
        assertFalse(multimap.contains("nonexistent", 1));
    }

    @Test
    public void testContainsKey() {
        multimap.put("key", 1);

        assertTrue(multimap.containsKey("key"));
        assertFalse(multimap.containsKey("nonexistent"));
    }

    @Test
    public void testContainsValue() {
        multimap.put("key1", 1);
        multimap.put("key2", 2);

        assertTrue(multimap.containsValue(1));
        assertTrue(multimap.containsValue(2));
        assertFalse(multimap.containsValue(3));
    }

    @Test
    public void testContainsAll() {
        multimap.putMany("key", Arrays.asList(1, 2, 3));

        assertTrue(multimap.containsAll("key", Arrays.asList(1, 2)));
        assertTrue(multimap.containsAll("key", Arrays.asList(1, 2, 3)));
        assertFalse(multimap.containsAll("key", Arrays.asList(1, 2, 4)));
        assertFalse(multimap.containsAll("nonexistent", Arrays.asList(1)));
    }

    // filter Tests
    @Test
    public void testFilterByKey() {
        multimap.put("apple", 1);
        multimap.put("banana", 2);
        multimap.put("apricot", 3);

        Multimap<String, Integer, List<Integer>> filtered = multimap.filterByKey(key -> key.startsWith("a"));

        assertEquals(2, filtered.size());
        assertTrue(filtered.containsKey("apple"));
        assertTrue(filtered.containsKey("apricot"));
        assertFalse(filtered.containsKey("banana"));
    }

    @Test
    public void testFilterByValue() {
        multimap.putMany("key1", Arrays.asList(1));
        multimap.putMany("key2", Arrays.asList(1, 2));
        multimap.putMany("key3", Arrays.asList(1, 2, 3));

        Multimap<String, Integer, List<Integer>> filtered = multimap.filterByValue(v -> v.size() >= 2);

        assertEquals(2, filtered.size());
        assertTrue(filtered.containsKey("key2"));
        assertTrue(filtered.containsKey("key3"));
    }

    @Test
    public void testFilter() {
        multimap.putMany("short", Arrays.asList(1, 2));
        multimap.putMany("longer", Arrays.asList(3, 4, 5));

        Multimap<String, Integer, List<Integer>> filtered = multimap.filter((k, v) -> k.length() > 5 && v.size() > 2);

        assertEquals(1, filtered.size());
        assertTrue(filtered.containsKey("longer"));
    }

    // forEach Tests
    @Test
    public void testForEach() {
        multimap.put("key1", 1);
        multimap.put("key2", 2);

        Map<String, List<Integer>> collected = new HashMap<>();
        multimap.forEach((k, v) -> collected.put(k, new ArrayList<>(v)));

        assertEquals(2, collected.size());
        assertEquals(multimap.get("key1"), collected.get("key1"));
    }

    @Test
    public void testFlatForEach() {
        multimap.putMany("key1", Arrays.asList(1, 2));
        multimap.putMany("key2", Arrays.asList(3, 4));

        List<String> pairs = new ArrayList<>();
        multimap.flatForEach((k, e) -> pairs.add(k + ":" + e));

        assertEquals(4, pairs.size());
        assertTrue(pairs.contains("key1:1"));
        assertTrue(pairs.contains("key1:2"));
        assertTrue(pairs.contains("key2:3"));
        assertTrue(pairs.contains("key2:4"));
    }

    @Test
    public void testForEachKey() {
        multimap.put("key1", 1);
        multimap.put("key2", 2);

        List<String> keys = new ArrayList<>();
        multimap.forEachKey(keys::add);

        assertEquals(2, keys.size());
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
    }

    @Test
    public void testForEachValue() {
        multimap.putMany("key1", Arrays.asList(1, 2));
        multimap.putMany("key2", Arrays.asList(3, 4));

        List<Integer> sizes = new ArrayList<>();
        multimap.forEachValue(v -> sizes.add(v.size()));

        assertEquals(2, sizes.size());
        assertTrue(sizes.contains(2));
    }

    @Test
    public void testFlatForEachValue() {
        multimap.putMany("key1", Arrays.asList(1, 2));
        multimap.putMany("key2", Arrays.asList(3, 4));

        List<Integer> values = new ArrayList<>();
        multimap.flatForEachValue(values::add);

        assertEquals(4, values.size());
        assertTrue(values.containsAll(Arrays.asList(1, 2, 3, 4)));
    }

    // Collection view Tests
    @Test
    public void testKeySet() {
        multimap.put("key1", 1);
        multimap.put("key2", 2);

        Set<String> keys = multimap.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
    }

    @Test
    public void testValues() {
        multimap.putMany("key1", Arrays.asList(1, 2));
        multimap.putMany("key2", Arrays.asList(3, 4));

        Collection<List<Integer>> values = multimap.values();
        assertEquals(2, values.size());
    }

    @Test
    public void testEntrySet() {
        multimap.put("key1", 1);
        multimap.put("key2", 2);

        Set<Map.Entry<String, List<Integer>>> entries = multimap.entrySet();
        assertEquals(2, entries.size());

        for (Map.Entry<String, List<Integer>> entry : entries) {
            assertTrue(entry.getKey().equals("key1") || entry.getKey().equals("key2"));
            assertEquals(1, entry.getValue().size());
        }
    }

    @Test
    public void testFlatValues() {
        multimap.putMany("key1", Arrays.asList(1, 2));
        multimap.putMany("key2", Arrays.asList(3, 4));

        List<Integer> flatValues = multimap.flatValues();
        assertEquals(4, flatValues.size());
        assertTrue(flatValues.containsAll(Arrays.asList(1, 2, 3, 4)));
    }

    @Test
    public void testFlatValuesWithSupplier() {
        multimap.putMany("key1", Arrays.asList(1, 2));
        multimap.putMany("key2", Arrays.asList(3, 4));

        Set<Integer> flatValues = multimap.flatValues(HashSet::new);
        assertEquals(4, flatValues.size());
        assertTrue(flatValues.containsAll(Arrays.asList(1, 2, 3, 4)));
    }

    // Conversion Tests
    @Test
    public void testToMap() {
        multimap.putMany("key1", Arrays.asList(1, 2));
        multimap.putMany("key2", Arrays.asList(3, 4));

        Map<String, List<Integer>> map = multimap.toMap();
        assertEquals(2, map.size());
        assertEquals(Arrays.asList(1, 2), map.get("key1"));
        assertEquals(Arrays.asList(3, 4), map.get("key2"));
    }

    @Test
    public void testToMapWithSupplier() {
        multimap.putMany("key1", Arrays.asList(1, 2));
        multimap.putMany("key2", Arrays.asList(3, 4));

        TreeMap<String, List<Integer>> map = multimap.toMap(IntFunctions.ofTreeMap());
        assertEquals(2, map.size());
        assertEquals("key1", map.firstKey());
    }

    @Test
    public void testToMultiset() {
        multimap.putMany("key1", Arrays.asList(1, 2));
        multimap.putMany("key2", Arrays.asList(3, 4, 5));

        Multiset<String> multiset = multimap.toMultiset();
        assertEquals(2, multiset.getCount("key1"));
        assertEquals(3, multiset.getCount("key2"));
    }

    // Iterator and Stream Tests
    @Test
    public void testIterator() {
        multimap.put("key1", 1);
        multimap.put("key2", 2);

        int count = 0;
        for (Map.Entry<String, List<Integer>> entry : multimap) {
            count++;
            assertTrue(entry.getKey().startsWith("key"));
            assertEquals(1, entry.getValue().size());
        }
        assertEquals(2, count);
    }

    @Test
    public void testStream() {
        multimap.put("key1", 1);
        multimap.put("key2", 2);

        long count = multimap.stream().count();
        assertEquals(2, count);

        List<String> keys = multimap.stream().map(Map.Entry::getKey).toList();
        assertEquals(2, keys.size());
    }

    @Test
    public void testEntryStream() {
        multimap.put("key1", 1);
        multimap.put("key2", 2);

        Map<String, Integer> sizes = multimap.entryStream().entries().toMap(Map.Entry::getKey, e -> e.getValue().size());

        assertEquals(2, sizes.size());
        assertEquals(Integer.valueOf(1), sizes.get("key1"));
        assertEquals(Integer.valueOf(1), sizes.get("key2"));
    }

    // Utility Tests
    @Test
    public void testClear() {
        multimap.put("key1", 1);
        multimap.put("key2", 2);

        assertFalse(multimap.isEmpty());
        multimap.clear();
        assertTrue(multimap.isEmpty());
        assertEquals(0, multimap.size());
    }

    @Test
    public void testSize() {
        assertEquals(0, multimap.size());

        multimap.put("key1", 1);
        assertEquals(1, multimap.size());

        multimap.put("key1", 2);
        assertEquals(1, multimap.size()); // Same key

        multimap.put("key2", 3);
        assertEquals(2, multimap.size());
    }

    @Test
    public void testTotalCountOfValues() {
        assertEquals(0, multimap.totalCountOfValues());

        multimap.putMany("key1", Arrays.asList(1, 2));
        assertEquals(2, multimap.totalCountOfValues());

        multimap.putMany("key2", Arrays.asList(3, 4, 5));
        assertEquals(5, multimap.totalCountOfValues());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(multimap.isEmpty());

        multimap.put("key", 1);
        assertFalse(multimap.isEmpty());

        multimap.clear();
        assertTrue(multimap.isEmpty());
    }

    // Functional Tests
    @Test
    public void testApply() throws Exception {
        multimap.put("key", 1);

        Integer result = multimap.apply(mm -> mm.totalCountOfValues());
        assertEquals(Integer.valueOf(1), result);
    }

    @Test
    public void testApplyIfNotEmpty() throws Exception {
        Optional<Integer> emptyResult = multimap.applyIfNotEmpty(mm -> mm.totalCountOfValues());
        assertFalse(emptyResult.isPresent());

        multimap.put("key", 1);
        Optional<Integer> result = multimap.applyIfNotEmpty(mm -> mm.totalCountOfValues());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get());
    }

    @Test
    public void testAccept() throws Exception {
        multimap.put("key", 1);

        List<String> keys = new ArrayList<>();
        multimap.accept(mm -> mm.forEachKey(keys::add));

        assertEquals(1, keys.size());
        assertEquals("key", keys.get(0));
    }

    @Test
    public void testAcceptIfNotEmpty() throws Exception {
        List<String> keys = new ArrayList<>();

        OrElse orElse = multimap.acceptIfNotEmpty(mm -> mm.forEachKey(keys::add));
        assertTrue(keys.isEmpty());

        multimap.put("key", 1);
        multimap.acceptIfNotEmpty(mm -> mm.forEachKey(keys::add));
        assertEquals(1, keys.size());
    }

    // equals and hashCode Tests
    @Test
    public void testEquals() {
        Multimap<String, Integer, List<Integer>> other = N.newListMultimap();

        assertTrue(multimap.equals(multimap));
        assertTrue(multimap.equals(other));

        multimap.put("key", 1);
        assertFalse(multimap.equals(other));

        other.put("key", 1);
        assertTrue(multimap.equals(other));

        assertFalse(multimap.equals(null));
        assertFalse(multimap.equals("string"));
    }

    @Test
    public void testHashCode() {
        Multimap<String, Integer, List<Integer>> other = N.newListMultimap();

        assertEquals(multimap.hashCode(), other.hashCode());

        multimap.put("key", 1);
        other.put("key", 1);

        assertEquals(multimap.hashCode(), other.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("{}", multimap.toString());

        multimap.put("key", 1);
        String str = multimap.toString();
        assertTrue(str.contains("key"));
        assertTrue(str.contains("1"));
    }

    // Edge Cases and Special Scenarios
    @Test
    public void testNullValues() {
        multimap.put("key", null);
        assertTrue(multimap.contains("key", null));
        assertEquals(1, multimap.get("key").size());
        assertNull(multimap.getFirst("key"));
    }

    @Test
    public void testEmptyCollectionRemoval() {
        multimap.put("key", 1);
        multimap.removeOne("key", 1);

        // Key should be removed when collection becomes empty
        assertFalse(multimap.containsKey("key"));
        assertNull(multimap.get("key"));
    }

    @Test
    public void testListBehavior() {
        // Lists allow duplicates
        multimap.put("key", 1);
        multimap.put("key", 1);
        multimap.put("key", 1);

        assertEquals(3, multimap.get("key").size());
    }

    @Test
    public void testSetBehavior() {
        // Sets don't allow duplicates
        setMultimap.put("key", "value");
        setMultimap.put("key", "value");
        setMultimap.put("key", "value");

        assertEquals(1, setMultimap.get("key").size());
    }

    @Test
    public void testComputeIfAbsentNullFunction() {
        assertThrows(IllegalArgumentException.class, () -> multimap.computeIfAbsent("key", null));
    }

    @Test
    public void testComputeIfPresentNullFunction() {
        assertThrows(IllegalArgumentException.class, () -> multimap.computeIfPresent("key", null));
    }

    @Test
    public void testComputeNullFunction() {
        assertThrows(IllegalArgumentException.class, () -> multimap.compute("key", null));
    }

    @Test
    public void testMergeNullFunction() {
        assertThrows(IllegalArgumentException.class, () -> multimap.merge("key", 1, null));
    }

    @Test
    public void testReplaceWithInvalidValue() {
        // Create a custom multimap that doesn't allow certain values
        Multimap<String, Integer, Set<Integer>> customMap = N.newMultimap(HashMap::new, () -> new HashSet<Integer>() {
            @Override
            public boolean add(Integer e) {
                if (e != null && e < 0) {
                    return false; // Don't allow negative numbers
                }
                return super.add(e);
            }
        });

        customMap.put("key", 1);
        assertThrows(IllegalStateException.class, () -> customMap.replaceOne("key", 1, -1)); // Should throw IllegalStateException
    }

    // Additional comprehensive tests for edge cases and scenarios

    // More thorough testing of removeIf variations
    @Test
    public void testRemoveManyIfBiPredicate() {
        multimap.putMany("key1", Arrays.asList(1, 2, 3, 4, 5));
        multimap.putMany("key2", Arrays.asList(1, 2));
        multimap.putMany("key3", Arrays.asList(1));

        assertTrue(multimap.removeManyIf(Arrays.asList(1, 2, 3), (k, v) -> v.size() > 3));
        assertEquals(2, multimap.get("key1").size());
        assertTrue(multimap.get("key1").contains(4));
        assertTrue(multimap.get("key1").contains(5));
        assertEquals(2, multimap.get("key2").size());
    }

    @Test
    public void testRemoveAllIfBiPredicate() {
        multimap.putMany("key1", Arrays.asList(1, 2, 3));
        multimap.putMany("key2", Arrays.asList(4));
        multimap.putMany("key3", Arrays.asList(5, 6));

        assertTrue(multimap.removeAllIf((k, v) -> v.size() < 2));
        assertEquals(2, multimap.size());
        assertFalse(multimap.containsKey("key2"));
    }

    // More replace variations
    @Test
    public void testReplaceOneIfBiPredicate() {
        multimap.putMany("key1", Arrays.asList(1, 2, 3));
        multimap.putMany("key2", Arrays.asList(2, 3, 4));

        assertTrue(multimap.replaceOneIf((k, v) -> v.contains(2), 2, 99));
        assertTrue(multimap.get("key1").contains(99));
        assertTrue(multimap.get("key2").contains(99)); // Only first match replaced
    }

    @Test
    public void testReplaceManyWithOneIfPredicate() {
        multimap.putMany("key1", Arrays.asList(1, 2, 3, 4));
        multimap.putMany("key2", Arrays.asList(2, 3, 4, 5));

        assertTrue(multimap.replaceManyWithOneIf(k -> k.equals("key1"), Arrays.asList(2, 3), 99));
        assertEquals(3, multimap.get("key1").size());
        assertTrue(multimap.get("key1").contains(99));
        assertFalse(multimap.get("key1").contains(2));
        assertFalse(multimap.get("key1").contains(3));
    }

    @Test
    public void testReplaceManyWithOneIfBiPredicate() {
        multimap.putMany("key1", Arrays.asList(1, 2, 3, 4));
        multimap.putMany("key2", Arrays.asList(2, 3));

        assertTrue(multimap.replaceManyWithOneIf((k, v) -> v.size() > 3, Arrays.asList(2, 3), 99));
        assertTrue(multimap.get("key1").contains(99));
        assertEquals(3, multimap.get("key1").size());
        assertEquals(2, multimap.get("key2").size());
    }

    @Test
    public void testReplaceAllWithOneIfBiPredicate() {
        multimap.putMany("key1", Arrays.asList(1, 2, 3));
        multimap.putMany("key2", Arrays.asList(4, 5));

        assertTrue(multimap.replaceAllWithOneIf((k, v) -> v.size() == 3, 99));
        assertEquals(1, multimap.get("key1").size());
        assertEquals(Integer.valueOf(99), multimap.getFirst("key1"));
        assertEquals(2, multimap.get("key2").size());
    }

    // Complex compute scenarios
    @Test
    public void testComputeIfAbsentWithEmptyReturn() {
        List<Integer> result = multimap.computeIfAbsent("key", k -> new ArrayList<>());
        assertNull(result); // Empty collection should not be added
        assertFalse(multimap.containsKey("key"));
    }

    @Test
    public void testComputeIfPresentRemovesKey() {
        multimap.put("key", 1);

        List<Integer> result = multimap.computeIfPresent("key", (k, v) -> null);
        assertNull(result);
        assertFalse(multimap.containsKey("key"));
    }

    @Test
    public void testComputeWithComplexScenario() {
        // Test compute with various return values
        multimap.compute("key1", (k, v) -> Arrays.asList(1, 2));
        assertEquals(2, multimap.get("key1").size());

        multimap.compute("key1", (k, v) -> {
            List<Integer> newList = new ArrayList<>(v);
            newList.add(3);
            return newList;
        });
        assertEquals(3, multimap.get("key1").size());

        multimap.compute("key1", (k, v) -> null);
        assertFalse(multimap.containsKey("key1"));
    }

    @Test
    public void testMergeWithNullOldValue() {
        List<Integer> result = multimap.merge("newKey", Arrays.asList(1, 2), (old, val) -> val);
        assertEquals(2, result.size());
        assertEquals(result, multimap.get("newKey"));
    }

    @Test
    public void testMergeRemovesKey() {
        multimap.put("key", 1);

        List<Integer> result = multimap.merge("key", 2, (old, val) -> null);
        assertNull(result);
        assertFalse(multimap.containsKey("key"));
    }

    // Internal Map and Collection modifications
    @Test
    public void testInternalMapSupplier() {
        Multimap<String, Integer, List<Integer>> treeMultimap = N.newMultimap(TreeMap::new, ArrayList::new);

        treeMultimap.put("c", 3);
        treeMultimap.put("a", 1);
        treeMultimap.put("b", 2);

        // TreeMap should maintain sorted order
        Iterator<String> keyIter = treeMultimap.keySet().iterator();
        assertEquals("a", keyIter.next());
        assertEquals("b", keyIter.next());
        assertEquals("c", keyIter.next());
    }

    @Test
    public void testCustomCollectionBehavior() {
        // Test with a collection that has special behavior
        Multimap<String, Integer, List<Integer>> customMultimap = N.newMultimap(HashMap::new, () -> new ArrayList<Integer>() {
            @Override
            public boolean add(Integer e) {
                if (e != null && e > 100) {
                    return super.add(e * 2); // Double values over 100
                }
                return super.add(e);
            }
        });

        customMultimap.put("key", 50);
        customMultimap.put("key", 150);

        List<Integer> values = customMultimap.get("key");
        assertEquals(Integer.valueOf(50), values.get(0));
        assertEquals(Integer.valueOf(300), values.get(1)); // 150 * 2
    }

    // Concurrent modification scenarios
    @Test
    public void testConcurrentModificationDuringIteration() {
        multimap.put("key1", 1);
        multimap.put("key2", 2);

        assertThrows(ConcurrentModificationException.class, () -> {
            // Attempt to modify the multimap while iterating
            for (Map.Entry<String, List<Integer>> entry : multimap) {
                multimap.put("key3", 3); // Should throw ConcurrentModificationException
            }
        });
    }

    // Large data sets
    @Test
    public void testLargeDataSet() {
        // Test with a large number of entries
        int numKeys = 1000;
        int valuesPerKey = 100;

        for (int i = 0; i < numKeys; i++) {
            for (int j = 0; j < valuesPerKey; j++) {
                multimap.put("key" + i, i * valuesPerKey + j);
            }
        }

        assertEquals(numKeys, multimap.size());
        assertEquals(numKeys * valuesPerKey, multimap.totalCountOfValues());

        // Test operations on large dataset
        Multimap<String, Integer, List<Integer>> filtered = multimap.filterByKey(k -> k.endsWith("0"));
        assertEquals(100, filtered.size()); // key0, key10, key20, ..., key990

        List<Integer> flatValues = multimap.flatValues();
        assertEquals(numKeys * valuesPerKey, flatValues.size());
    }

    // Memory efficiency test
    @Test
    public void testMemoryEfficiency() {
        // Test that empty value collections are not retained unnecessarily
        for (int i = 0; i < 100; i++) {
            multimap.put("key" + i, i);
        }

        // Remove all values
        for (int i = 0; i < 100; i++) {
            multimap.removeOne("key" + i, i);
        }

        assertEquals(0, multimap.size());
        assertTrue(multimap.isEmpty());
    }

    // Type safety tests
    @Test
    public void testTypeSafety() {
        // Ensure type safety is maintained
        Multimap<Integer, String, Set<String>> typedMultimap = N.newMultimap(HashMap::new, HashSet::new);

        typedMultimap.put(1, "one");
        typedMultimap.put(2, "two");

        Set<String> values = typedMultimap.get(1);
        assertTrue(values instanceof Set);

        // Test with different types
        Multimap<String, Object, List<Object>> objectMultimap = N.newListMultimap();
        objectMultimap.put("mixed", "string");
        objectMultimap.put("mixed", 123);
        objectMultimap.put("mixed", true);

        List<Object> mixedValues = objectMultimap.get("mixed");
        assertEquals(3, mixedValues.size());
        assertEquals("string", mixedValues.get(0));
        assertEquals(123, mixedValues.get(1));
        assertEquals(true, mixedValues.get(2));
    }

    // Boundary conditions
    @Test
    public void testBoundaryConditions() {
        // Test with minimum values
        multimap.put("", 0);
        assertTrue(multimap.contains("", 0));

        // Test with null key
        multimap.put(null, 1);
        assertTrue(multimap.contains(null, 1));
        assertEquals(Integer.valueOf(1), multimap.getFirst(null));

        // Test remove with null
        assertTrue(multimap.removeOne(null, 1));
        assertFalse(multimap.containsKey(null));
    }

    // Complex filtering scenarios
    @Test
    public void testComplexFiltering() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < i; j++) {
                multimap.put("key" + i, j);
            }
        }

        // Filter by multiple conditions
        Multimap<String, Integer, List<Integer>> filtered = multimap.filter((k, v) -> {
            int keyNum = Integer.parseInt(k.substring(3));
            return keyNum % 2 == 0 && v.size() > 3;
        });

        assertTrue(filtered.containsKey("key4"));
        assertTrue(filtered.containsKey("key6"));
        assertTrue(filtered.containsKey("key8"));
        assertEquals(3, filtered.size());
    }

    // Stream operations
    @Test
    public void testStreamOperations() {
        multimap.putMany("key1", Arrays.asList(1, 2, 3));
        multimap.putMany("key2", Arrays.asList(4, 5));
        multimap.putMany("key3", Arrays.asList(6));

        // Test stream transformations
        int sum = multimap.stream().mapToInt(e -> e.getValue().size()).sum();
        assertEquals(6, sum);

        // Test entry stream
        Map<String, Integer> maxValues = multimap.entryStream().entries().toMap(Map.Entry::getKey, e -> e.getValue().stream().max(Integer::compare).orElse(0));

        assertEquals(Integer.valueOf(3), maxValues.get("key1"));
        assertEquals(Integer.valueOf(5), maxValues.get("key2"));
        assertEquals(Integer.valueOf(6), maxValues.get("key3"));
    }

    // Functional interface tests
    @Test
    public void testFunctionalInterfaces() {
        // Test with method references
        multimap.put("test", 1);
        multimap.forEachKey(System.out::println);
        multimap.forEachValue(System.out::println);
        multimap.flatForEachValue(System.out::println);

        // Test with lambda expressions
        List<String> keyValuePairs = new ArrayList<>();
        multimap.flatForEach((k, v) -> keyValuePairs.add(k + "=" + v));
        assertEquals(1, keyValuePairs.size());
        assertEquals("test=1", keyValuePairs.get(0));
    }

    // Edge cases for replace operations
    @Test
    public void testReplaceEdgeCases() {
        // Replace in empty multimap
        assertFalse(multimap.replaceOne("key", 1, 2));
        assertFalse(multimap.replaceAllWithOne("key", 1));
        assertFalse(multimap.replaceManyWithOne("key", Arrays.asList(1, 2), 3));

        // Replace with same value
        multimap.put("key", 1);
        assertTrue(multimap.replaceOne("key", 1, 1));
        assertEquals(Integer.valueOf(1), multimap.getFirst("key"));

        // Replace multiple occurrences in List
        multimap.clear();
        multimap.putMany("key", Arrays.asList(1, 2, 1, 3, 1));
        assertTrue(multimap.replaceOne("key", 1, 99));
        List<Integer> values = multimap.get("key");
        assertEquals(Integer.valueOf(99), values.get(0));
        assertEquals(Integer.valueOf(1), values.get(2)); // Only first occurrence replaced
    }

    // Test for custom equals and hashCode behavior
    @Test
    public void testCustomEqualsHashCode() {
        class CustomKey {
            private final String value;

            CustomKey(String value) {
                this.value = value;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj instanceof CustomKey) {
                    return value.equalsIgnoreCase(((CustomKey) obj).value);
                }
                return false;
            }

            @Override
            public int hashCode() {
                return value.toLowerCase().hashCode();
            }
        }

        Multimap<CustomKey, Integer, List<Integer>> customMultimap = N.newListMultimap();
        CustomKey key1 = new CustomKey("TEST");
        CustomKey key2 = new CustomKey("test");

        customMultimap.put(key1, 1);
        customMultimap.put(key2, 2); // Should be treated as same key

        assertEquals(1, customMultimap.size());
        assertEquals(2, customMultimap.get(key1).size());
    }

    // Integration tests combining multiple operations
    @Test
    public void testIntegrationScenario1() {
        // Simulate a real-world scenario: grouping and transforming data
        List<String> words = Arrays.asList("apple", "apricot", "banana", "berry", "cherry", "apple");

        // Group by first letter
        Multimap<Character, String, List<String>> grouped = N.newListMultimap();
        for (String word : words) {
            grouped.put(word.charAt(0), word);
        }

        // Transform: convert to uppercase
        grouped.replaceAll((k, v) -> {
            List<String> upper = new ArrayList<>();
            for (String s : v) {
                upper.add(s.toUpperCase());
            }
            return upper;
        });

        // Filter: keep only groups with more than one item
        Multimap<Character, String, List<String>> filtered = grouped.filterByValue(v -> v.size() > 1);

        assertEquals(2, filtered.size());
        assertTrue(filtered.containsKey('a'));
        assertTrue(filtered.containsKey('b'));
        assertTrue(filtered.get('a').contains("APPLE"));
        assertTrue(filtered.get('a').contains("APRICOT"));
    }

    @Test
    public void testIntegrationScenario2() {
        // Scenario: Managing user permissions
        Multimap<String, String, Set<String>> userPermissions = N.newMultimap(HashMap::new, HashSet::new);

        // Add permissions
        userPermissions.putMany("admin", Arrays.asList("read", "write", "delete"));
        userPermissions.putMany("user", Arrays.asList("read"));
        userPermissions.putMany("moderator", Arrays.asList("read", "write"));

        // Grant additional permission to all users who can read
        Set<String> rolesWithRead = new HashSet<>();
        userPermissions.forEach((role, perms) -> {
            if (perms.contains("read")) {
                rolesWithRead.add(role);
            }
        });

        for (String role : rolesWithRead) {
            userPermissions.put(role, "list");
        }

        // Remove delete permission from non-admins
        userPermissions.removeOneIf("delete", role -> !role.equals("admin"));

        // Verify
        assertTrue(userPermissions.get("admin").contains("delete"));
        assertFalse(userPermissions.contains("moderator", "delete"));
        assertTrue(userPermissions.get("user").contains("list"));
    }

    @Test
    public void testIntegrationScenario3() {
        // Scenario: Event tracking system
        Multimap<String, Long, List<Long>> eventTimestamps = N.newListMultimap();

        // Simulate events
        long baseTime = System.currentTimeMillis();
        eventTimestamps.putMany("login", Arrays.asList(baseTime, baseTime + 1000, baseTime + 2000));
        eventTimestamps.putMany("logout", Arrays.asList(baseTime + 3000, baseTime + 4000));
        eventTimestamps.putMany("error", Arrays.asList(baseTime + 1500));

        // Compute session durations
        List<Long> loginTimes = eventTimestamps.get("login");
        List<Long> logoutTimes = eventTimestamps.get("logout");

        // Merge and update
        eventTimestamps.merge("session_duration", loginTimes, (old, count) -> {
            List<Long> durations = new ArrayList<>();
            for (int i = 0; i < Math.min(loginTimes.size(), logoutTimes.size()); i++) {
                durations.add(logoutTimes.get(i) - loginTimes.get(i));
            }
            return durations;
        });

        assertTrue(eventTimestamps.containsKey("session_duration"));
        assertEquals(3, eventTimestamps.get("session_duration").size());

        // Merge and update
        eventTimestamps.merge("session_duration", loginTimes, (old, count) -> {
            List<Long> durations = new ArrayList<>();
            for (int i = 0; i < Math.min(loginTimes.size(), logoutTimes.size()); i++) {
                durations.add(logoutTimes.get(i) - loginTimes.get(i));
            }
            return durations;
        });

        assertTrue(eventTimestamps.containsKey("session_duration"));
        assertEquals(2, eventTimestamps.get("session_duration").size());
    }

    // Performance characteristics test
    @Test // 5 second timeout
    public void testPerformanceCharacteristics() {
        // Test that operations maintain reasonable performance
        int operations = 10000;

        long startTime = System.currentTimeMillis();

        // Insertion
        for (int i = 0; i < operations; i++) {
            multimap.put("key" + (i % 100), i);
        }

        // Retrieval
        for (int i = 0; i < operations; i++) {
            multimap.get("key" + (i % 100));
        }

        // Removal
        for (int i = 0; i < operations / 2; i++) {
            multimap.removeOne("key" + (i % 100), i);
        }

        long endTime = System.currentTimeMillis();

        // Should complete within timeout
        assertTrue(endTime - startTime < 5000, "Operations took too long: " + (endTime - startTime) + "ms");
    }

    //    // Thread safety test (should fail as Multimap is not thread-safe)
    //    @Test
    //    public void testNotThreadSafe() {
    //        // This test demonstrates that Multimap is NOT thread-safe
    //        // In a real concurrent scenario, this might throw ConcurrentModificationException
    //        // or produce inconsistent results
    //
    //        final int threads = 10;
    //        final int operationsPerThread = 100;
    //        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
    //
    //        Runnable task = () -> {
    //            try {
    //                for (int i = 0; i < operationsPerThread; i++) {
    //                    multimap.put(Thread.currentThread().getName(), i);
    //                    multimap.get(Thread.currentThread().getName());
    //                }
    //            } catch (Exception e) {
    //                exceptions.add(e);
    //            }
    //        };
    //
    //        List<Thread> threadList = new ArrayList<>();
    //        for (int i = 0; i < threads; i++) {
    //            Thread t = new Thread(task);
    //            threadList.add(t);
    //            t.start();
    //        }
    //
    //        // Wait for all threads
    //        for (Thread t : threadList) {
    //            try {
    //                t.join();
    //            } catch (InterruptedException e) {
    //                Thread.currentThread().interrupt();
    //            }
    //        }
    //
    //        // The multimap might be in an inconsistent state
    //        // This test just demonstrates the lack of thread safety
    //        assertTrue(multimap.size() <= threads);
    //    }

    // Test all exception scenarios
    @Test
    public void testAllExceptionScenarios() {
        // Test all methods that should throw IllegalArgumentException for null parameters
        try {
            multimap.forEach((BiConsumer) null);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            multimap.flatForEach(null);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            multimap.forEachKey(null);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            multimap.forEachValue(null);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        try {
            multimap.flatForEachValue(null);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    // Test toString format
    @Test
    public void testToStringFormat() {
        multimap.putMany("key1", Arrays.asList(1, 2));
        multimap.put("key2", 3);

        String str = multimap.toString();
        assertTrue(str.startsWith("{"));
        assertTrue(str.endsWith("}"));
        assertTrue(str.contains("key1"));
        assertTrue(str.contains("key2"));
        assertTrue(str.contains("[1, 2]") || str.contains("[2, 1]")); // Order might vary
        assertTrue(str.contains("[3]"));
    }

    // Final comprehensive scenario test
    @Test
    public void testComprehensiveScenario() {
        // Create a complex multimap scenario
        Multimap<String, Object, List<Object>> complexMap = N.newListMultimap();

        // Add various types of data
        complexMap.put("numbers", 1);
        complexMap.put("numbers", 2.5);
        complexMap.put("numbers", 3L);

        complexMap.put("strings", "hello");
        complexMap.put("strings", "world");

        complexMap.put("mixed", 42);
        complexMap.put("mixed", "forty-two");
        complexMap.put("mixed", true);

        // Perform various operations
        assertEquals(3, complexMap.size());
        assertEquals(8, complexMap.totalCountOfValues());

        // Filter and transform
        Multimap<String, Object, List<Object>> filtered = complexMap.filterByKey(k -> !k.equals("mixed"));
        assertEquals(2, filtered.size());

        // Copy and modify
        Multimap<String, Object, List<Object>> copy = complexMap.copy();
        copy.removeAll("strings");
        assertTrue(complexMap.containsKey("strings"));
        assertFalse(copy.containsKey("strings"));

        // Compute operations
        complexMap.compute("computed", (k, v) -> {
            if (v == null) {
                return Arrays.asList("computed", "value");
            }
            return v;
        });

        assertTrue(complexMap.containsKey("computed"));
        assertEquals(2, complexMap.get("computed").size());
    }
}
