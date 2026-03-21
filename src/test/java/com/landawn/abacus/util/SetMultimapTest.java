package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class SetMultimapTest extends TestBase {

    @Test
    public void test_of_1() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1);
        assertEquals(1, map.totalValueCount());
        assertTrue(map.containsKey("a"));
        assertTrue(map.get("a").contains(1));
        assertEquals(1, map.get("a").size());
    }

    @Test
    public void test_of_2() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2);
        assertEquals(2, map.totalValueCount());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertTrue(map.get("a").contains(1));
        assertTrue(map.get("b").contains(2));
    }

    @Test
    public void test_of_3() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2, "c", 3);
        assertEquals(3, map.totalValueCount());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertTrue(map.containsKey("c"));
        assertTrue(map.get("a").contains(1));
        assertTrue(map.get("b").contains(2));
        assertTrue(map.get("c").contains(3));
    }

    @Test
    public void test_of_4() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2, "c", 3, "d", 4);
        assertEquals(4, map.totalValueCount());
        assertTrue(map.containsKey("d"));
        assertTrue(map.get("d").contains(4));
    }

    @Test
    public void test_of_5() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
        assertEquals(5, map.totalValueCount());
        assertTrue(map.containsKey("e"));
        assertTrue(map.get("e").contains(5));
    }

    @Test
    public void test_of_6() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);
        assertEquals(6, map.totalValueCount());
        assertTrue(map.containsKey("f"));
        assertTrue(map.get("f").contains(6));
    }

    @Test
    public void test_of_7() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
        assertEquals(7, map.totalValueCount());
        assertTrue(map.containsKey("g"));
        assertTrue(map.get("g").contains(7));
    }

    @Test
    public void test_of_withDuplicateKeys() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2);
        assertEquals(2, map.totalValueCount());
        assertTrue(map.containsKey("a"));
        assertEquals(2, map.get("a").size());
        assertTrue(map.get("a").contains(1));
        assertTrue(map.get("a").contains(2));
    }

    @Test
    public void test_of_withDuplicateValues() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 1);
        assertEquals(1, map.totalValueCount());
        assertTrue(map.containsKey("a"));
        assertEquals(1, map.get("a").size());
        assertTrue(map.get("a").contains(1));
    }

    @Test
    public void test_removeAll() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);

        map.removeAll("a");
        assertEquals(1, map.totalValueCount());
        assertFalse(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
    }

    @Test
    public void test_keySet() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2, "c", 3);
        Set<String> keys = map.keySet();

        assertEquals(3, keys.size());
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains("b"));
        assertTrue(keys.contains("c"));
    }

    @Test
    public void test_flattenValues() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        Collection<Integer> flattenValues = map.allValues();

        assertEquals(3, flattenValues.size());
        assertTrue(flattenValues.contains(1));
        assertTrue(flattenValues.contains(2));
        assertTrue(flattenValues.contains(3));
    }

    @Test
    public void test_totalCountOfValues() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        assertEquals(3, map.totalValueCount());
    }

    @Test
    public void test_putMany() {
        SetMultimap<String, Integer> map1 = SetMultimap.of("a", 1);
        SetMultimap<String, Integer> map2 = SetMultimap.of("b", 2, "c", 3);

        map1.putValues(map2);
        assertEquals(3, map1.totalValueCount());
        assertTrue(map1.containsKey("a"));
        assertTrue(map1.containsKey("b"));
        assertTrue(map1.containsKey("c"));
    }

    @Test
    public void test_hashCode() {
        SetMultimap<String, Integer> map1 = SetMultimap.of("a", 1, "b", 2);
        SetMultimap<String, Integer> map2 = SetMultimap.of("a", 1, "b", 2);

        assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    public void test_removeOne() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);

        assertTrue(map.removeEntry("a", 1));
        assertEquals(1, map.get("a").size());
        assertFalse(map.get("a").contains(1));

        assertFalse(map.removeEntry("a", 99));
        assertEquals(1, map.get("a").size());
    }

    @Test
    public void test_removeMany() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "a", 3);

        assertTrue(map.removeValues("a", CommonUtil.toList(1, 2)));
        assertEquals(1, map.get("a").size());
        assertTrue(map.get("a").contains(3));
    }

    @Test
    public void test_contains() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2);

        assertTrue(map.containsEntry("a", 1));
        assertTrue(map.containsEntry("a", 2));
        assertFalse(map.containsEntry("a", 3));
        assertFalse(map.containsEntry("b", 1));
    }

    //
    @Test
    public void test_getOrDefault() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1);

        Set<Integer> result = map.getOrDefault("a", new HashSet<>());
        assertEquals(1, result.size());

        Set<Integer> defaultSet = new HashSet<>();
        defaultSet.add(99);
        result = map.getOrDefault("b", defaultSet);
        assertEquals(defaultSet, result);
    }

    @Test
    public void test_toMap() {
        SetMultimap<String, Integer> multimap = SetMultimap.of("a", 1, "a", 2, "b", 3);
        Map<String, Set<Integer>> map = multimap.toMap();

        assertEquals(2, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertEquals(2, map.get("a").size());
        assertEquals(1, map.get("b").size());
    }

    @Test
    public void test_stream() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2);

        long count = map.stream().count();
        assertEquals(2, count);
    }

    @Test
    public void test_flatForEach() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);

        final int[] count = { 0 };
        map.forEach((k, v) -> {
            count[0]++;
        });
        assertEquals(3, count[0]);
    }

    @Test
    public void test_replaceOne() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2);

        assertTrue(map.replaceEntry("a", 1, 10));
        assertTrue(map.get("a").contains(10));
        assertFalse(map.get("a").contains(1));
        assertTrue(map.get("a").contains(2));
    }

    // ==================== removeEntries ====================

    @Test
    public void testRemoveEntries() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("a", 1);
        toRemove.put("b", 3);
        assertTrue(map.removeEntries(toRemove));
        assertEquals(1, map.get("a").size());
        assertTrue(map.get("a").contains(2));
        assertFalse(map.containsKey("b"));
    }

    // ==================== removeKeysIf ====================

    @Test
    public void testRemoveKeysIf_predicate() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "ab", 2, "abc", 3);
        assertTrue(map.removeKeysIf(k -> k.length() > 1));
        assertEquals(1, map.keyCount());
        assertTrue(map.containsKey("a"));
    }

    @Test
    public void testRemoveKeysIf_biPredicate() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2, "c", 3);
        assertTrue(map.removeKeysIf((k, v) -> v.contains(2)));
        assertFalse(map.containsKey("b"));
        assertTrue(map.containsKey("a"));
    }

    // ==================== removeEntriesIf ====================

    @Test
    public void testRemoveEntriesIf_keyPredicate() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 1);
        assertTrue(map.removeEntriesIf(k -> k.equals("a"), 1));
        assertFalse(map.get("a").contains(1));
        assertTrue(map.get("a").contains(2));
        assertTrue(map.get("b").contains(1));
    }

    @Test
    public void testRemoveEntriesIf_biPredicate() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        assertTrue(map.removeEntriesIf((k, v) -> v.size() > 1, 1));
        assertFalse(map.get("a").contains(1));
        assertTrue(map.get("a").contains(2));
    }

    // ==================== removeValuesIf ====================

    @Test
    public void testRemoveValuesIf_keyPredicate() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        assertTrue(map.removeValuesIf(k -> k.equals("a"), Arrays.asList(1)));
        assertFalse(map.get("a").contains(1));
        assertTrue(map.get("a").contains(2));
    }

    @Test
    public void testRemoveValuesIf_biPredicate() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        assertTrue(map.removeValuesIf((k, v) -> v.size() > 1, Arrays.asList(1)));
        assertFalse(map.get("a").contains(1));
    }

    // ==================== removeValues with map/multimap ====================

    @Test
    public void testRemoveValues_map() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        Map<String, List<Integer>> m = new HashMap<>();
        m.put("a", Arrays.asList(1));
        m.put("b", Arrays.asList(3));
        assertTrue(map.removeValues(m));
        assertTrue(map.get("a").contains(2));
        assertFalse(map.containsKey("b"));
    }

    @Test
    public void testRemoveValues_multimap() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        SetMultimap<String, Integer> toRemove = SetMultimap.of("a", 1, "b", 3);
        assertTrue(map.removeValues(toRemove));
        assertTrue(map.get("a").contains(2));
        assertFalse(map.containsKey("b"));
    }

    // ==================== replaceEntriesIf ====================

    @Test
    public void testReplaceEntriesIf_keyPredicate() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 1);
        assertTrue(map.replaceEntriesIf(k -> k.equals("a"), 1, 100));
        assertTrue(map.get("a").contains(100));
        assertFalse(map.get("a").contains(1));
        assertTrue(map.get("b").contains(1));
    }

    @Test
    public void testReplaceEntriesIf_biPredicate() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 1);
        assertTrue(map.replaceEntriesIf((k, v) -> v.size() > 1, 1, 100));
        assertTrue(map.get("a").contains(100));
    }

    // ==================== replaceValues ====================

    @Test
    public void testReplaceValues() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        assertTrue(map.replaceValues("a", Arrays.asList(10, 20, 30)));
        assertEquals(3, map.get("a").size());
        assertTrue(map.get("a").contains(10));
    }

    // ==================== replaceValuesIf ====================

    @Test
    public void testReplaceValuesIf_keyPredicate() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        assertTrue(map.replaceValuesIf(k -> k.equals("a"), Arrays.asList(10, 20)));
        assertEquals(2, map.get("a").size());
        assertTrue(map.get("a").contains(10));
    }

    @Test
    public void testReplaceValuesIf_biPredicate() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        assertTrue(map.replaceValuesIf((k, v) -> v.size() > 1, Arrays.asList(10, 20)));
        assertTrue(map.get("a").contains(10));
        assertEquals(1, map.get("b").size());
    }

    // ==================== replaceAll ====================

    @Test
    public void testReplaceAll() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2);
        map.replaceAll((k, v) -> {
            Set<Integer> newSet = new HashSet<>();
            for (Integer i : v) {
                newSet.add(i * 10);
            }
            return newSet;
        });
        assertTrue(map.get("a").contains(10));
        assertTrue(map.get("b").contains(20));
    }

    @Test
    public void testComputeIfAbsent_present() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1);
        Set<Integer> result = map.computeIfAbsent("a", k -> new HashSet<>(Arrays.asList(99)));
        assertEquals(1, result.size());
        assertTrue(result.contains(1));
    }

    // ==================== computeIfPresent ====================

    @Test
    public void testComputeIfPresent_present() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1);
        Set<Integer> result = map.computeIfPresent("a", (k, v) -> new HashSet<>(Arrays.asList(10)));
        assertEquals(1, result.size());
        assertTrue(result.contains(10));
    }

    // ==================== valueCollections ====================

    @Test
    public void testValueCollections() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        Collection<Set<Integer>> values = map.valueCollections();
        assertEquals(2, values.size());
    }

    // ==================== flatValues ====================

    @Test
    public void testFlatValues() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        List<Integer> flat = map.flatValues(ArrayList::new);
        assertEquals(3, flat.size());
    }

    // ==================== entryStream ====================

    @Test
    public void testEntryStream() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        long count = map.entryStream().count();
        assertEquals(3, count);
    }

    // ==================== iterator ====================

    @Test
    public void testIterator() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2);
        Iterator<Map.Entry<String, Set<Integer>>> iter = map.iterator();
        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        assertEquals(2, count);
    }

    // ==================== toMultiset ====================

    @Test
    public void testToMultiset() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        Multiset<String> multiset = map.toMultiset();
        assertEquals(2, multiset.get("a"));
        assertEquals(1, multiset.get("b"));
    }

    // ==================== size / keyCount ====================

    @Test
    public void testSize() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        assertEquals(2, map.size());
    }

    @Test
    public void testKeyCount() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        assertEquals(2, map.keyCount());
    }

    // ==================== apply / accept ====================

    @Test
    public void testApply() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2);
        int result = map.apply(m -> m.totalValueCount());
        assertEquals(2, result);
    }

    @Test
    public void testAccept() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1);
        AtomicInteger counter = new AtomicInteger(0);
        map.accept(m -> counter.set(m.totalValueCount()));
        assertEquals(1, counter.get());
    }

    @Test
    public void test_clearAndEmpty() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2);
        assertFalse(map.isEmpty());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.totalValueCount());
    }

    @Test
    public void test_toString() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1);
        String str = map.toString();
        assertNotNull(str);
        assertTrue(str.contains("a"));
    }

    @Test
    public void test_equals() {
        SetMultimap<String, Integer> map1 = SetMultimap.of("a", 1, "b", 2);
        SetMultimap<String, Integer> map2 = SetMultimap.of("a", 1, "b", 2);
        SetMultimap<String, Integer> map3 = SetMultimap.of("a", 1);

        assertEquals(map1, map2);
        assertFalse(map1.equals(map3));
        assertFalse(map1.equals(null));
    }

    @Test
    public void testReplaceValues_nonExistentKey() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1);
        assertFalse(map.replaceValues("z", Arrays.asList(10)));
    }

    // ==================== toMap with supplier ====================

    @Test
    public void testToMap_withSupplier() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2);
        LinkedHashMap<String, Set<Integer>> result = map.toMap(LinkedHashMap::new);
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testApplyIfNotEmpty() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1);
        assertTrue(map.applyIfNotEmpty(m -> m.totalValueCount()).isPresent());

        SetMultimap<String, Integer> empty = CommonUtil.newSetMultimap();
        assertFalse(empty.applyIfNotEmpty(m -> m.totalValueCount()).isPresent());
    }

    @Test
    public void testAcceptIfNotEmpty() {
        AtomicInteger counter = new AtomicInteger(0);

        SetMultimap<String, Integer> map = SetMultimap.of("a", 1);
        map.acceptIfNotEmpty(m -> counter.set(m.totalValueCount()));
        assertEquals(1, counter.get());

        counter.set(0);
        SetMultimap<String, Integer> empty = CommonUtil.newSetMultimap();
        empty.acceptIfNotEmpty(m -> counter.set(99));
        assertEquals(0, counter.get());
    }

    @Test
    public void test_create_fromMap() {
        Map<String, Integer> sourceMap = new HashMap<>();
        sourceMap.put("a", 1);
        sourceMap.put("b", 2);
        sourceMap.put("c", 3);

        SetMultimap<String, Integer> map = SetMultimap.fromMap(sourceMap);
        assertEquals(3, map.totalValueCount());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertTrue(map.containsKey("c"));
        assertTrue(map.get("a").contains(1));
    }

    @Test
    public void test_create_fromEmptyMap() {
        Map<String, Integer> sourceMap = new HashMap<>();
        SetMultimap<String, Integer> map = SetMultimap.fromMap(sourceMap);
        assertEquals(0, map.totalValueCount());
        assertTrue(map.isEmpty());
    }

    @Test
    public void test_create_fromNullMap() {
        SetMultimap<String, Integer> map = SetMultimap.fromMap((Map<String, Integer>) null);
        assertEquals(0, map.totalValueCount());
        assertTrue(map.isEmpty());
    }

    @Test
    public void test_create_withKeyExtractor() {
        List<String> words = CommonUtil.toList("apple", "ant", "banana", "bear");
        SetMultimap<Character, String> map = SetMultimap.fromCollection(words, s -> s.charAt(0));

        assertEquals(4, map.totalValueCount());
        assertTrue(map.containsKey('a'));
        assertTrue(map.containsKey('b'));
        assertEquals(2, map.get('a').size());
        assertTrue(map.get('a').contains("apple"));
        assertTrue(map.get('a').contains("ant"));
        assertEquals(2, map.get('b').size());
        assertTrue(map.get('b').contains("banana"));
        assertTrue(map.get('b').contains("bear"));
    }

    @Test
    public void test_create_withKeyAndValueExtractor() {
        List<String> words = CommonUtil.toList("apple", "ant", "banana");
        SetMultimap<Character, Integer> map = SetMultimap.fromCollection(words, s -> s.charAt(0), String::length);

        assertEquals(3, map.totalValueCount());
        assertTrue(map.containsKey('a'));
        assertTrue(map.containsKey('b'));
        assertTrue(map.get('a').contains(5));
        assertTrue(map.get('a').contains(3));
        assertTrue(map.get('b').contains(6));
    }

    @Test
    public void test_create_withKeyExtractor_emptyCollection() {
        List<String> words = new ArrayList<>();
        SetMultimap<Character, String> map = SetMultimap.fromCollection(words, s -> s.charAt(0));
        assertEquals(0, map.totalValueCount());
    }

    @Test
    public void test_create_withKeyAndValueExtractor_emptyCollection() {
        List<String> words = new ArrayList<>();
        SetMultimap<Character, Integer> map = SetMultimap.fromCollection(words, s -> s.charAt(0), String::length);
        assertEquals(0, map.totalValueCount());
    }

    @Test
    public void test_create_withKeyExtractor_nullExtractor() {
        List<String> words = CommonUtil.toList("apple");
        assertThrows(IllegalArgumentException.class, () -> {
            SetMultimap.fromCollection(words, null);
        });
    }

    @Test
    public void test_concat_twoMaps() {
        Map<String, Integer> map1 = CommonUtil.asMap("a", 1, "b", 2);
        Map<String, Integer> map2 = CommonUtil.asMap("c", 3, "d", 4);

        SetMultimap<String, Integer> result = SetMultimap.merge(map1, map2);
        assertEquals(4, result.totalValueCount());
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsKey("b"));
        assertTrue(result.containsKey("c"));
        assertTrue(result.containsKey("d"));
    }

    @Test
    public void test_concat_twoMaps_overlappingKeys() {
        Map<String, Integer> map1 = CommonUtil.asMap("a", 1);
        Map<String, Integer> map2 = CommonUtil.asMap("a", 2);

        SetMultimap<String, Integer> result = SetMultimap.merge(map1, map2);
        assertEquals(2, result.totalValueCount());
        assertEquals(2, result.get("a").size());
        assertTrue(result.get("a").contains(1));
        assertTrue(result.get("a").contains(2));
    }

    @Test
    public void test_concat_threeMaps() {
        Map<String, Integer> map1 = CommonUtil.asMap("a", 1);
        Map<String, Integer> map2 = CommonUtil.asMap("b", 2);
        Map<String, Integer> map3 = CommonUtil.asMap("c", 3);

        SetMultimap<String, Integer> result = SetMultimap.merge(map1, map2, map3);
        assertEquals(3, result.totalValueCount());
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsKey("b"));
        assertTrue(result.containsKey("c"));
    }

    @Test
    public void test_concat_collection() {
        List<Map<String, Integer>> maps = new ArrayList<>();
        maps.add(CommonUtil.asMap("a", 1));
        maps.add(CommonUtil.asMap("b", 2));
        maps.add(CommonUtil.asMap("c", 3));

        SetMultimap<String, Integer> result = SetMultimap.merge(maps);
        assertEquals(3, result.totalValueCount());
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsKey("b"));
        assertTrue(result.containsKey("c"));
    }

    @Test
    public void test_merge() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();
        map.put("a", 1);

        map.merge("a", 2, (v, e) -> {
            v.add(e);
            return v;
        });

        assertEquals(2, map.get("a").size());
        assertTrue(map.get("a").contains(1));
        assertTrue(map.get("a").contains(2));
    }

    // ==================== merge with Collection ====================

    @Test
    public void testMerge_withCollection_absent() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();
        Set<Integer> elements = new HashSet<>(Arrays.asList(1, 2));
        map.merge("a", elements, (oldV, newV) -> {
            Set<Integer> merged = new HashSet<>(oldV);
            merged.addAll(newV);
            return merged;
        });
        assertEquals(2, map.get("a").size());
    }

    @Test
    public void testMerge_withCollection_present() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1);
        Set<Integer> elements = new HashSet<>(Arrays.asList(2, 3));
        map.merge("a", elements, (oldV, newV) -> {
            Set<Integer> merged = new HashSet<>(oldV);
            merged.addAll(newV);
            return merged;
        });
        assertEquals(3, map.get("a").size());
    }

    @Test
    public void test_concat_twoMaps_withNulls() {
        Map<String, Integer> map1 = CommonUtil.asMap("a", 1);
        SetMultimap<String, Integer> result = SetMultimap.merge(null, map1);
        assertEquals(1, result.totalValueCount());

        result = SetMultimap.merge(map1, null);
        assertEquals(1, result.totalValueCount());

        result = SetMultimap.merge(null, null);
        assertEquals(0, result.totalValueCount());
    }

    @Test
    public void test_concat_threeMaps_withNulls() {
        Map<String, Integer> map1 = CommonUtil.asMap("a", 1);
        SetMultimap<String, Integer> result = SetMultimap.merge(null, null, map1);
        assertEquals(1, result.totalValueCount());

        result = SetMultimap.merge(map1, (Map<String, Integer>) null, (Map<String, Integer>) null);
        assertEquals(1, result.totalValueCount());
    }

    @Test
    public void test_concat_emptyCollection() {
        List<Map<String, Integer>> maps = new ArrayList<>();
        SetMultimap<String, Integer> result = SetMultimap.merge(maps);
        assertEquals(0, result.totalValueCount());
    }

    @Test
    public void test_concat_nullCollection() {
        SetMultimap<String, Integer> result = SetMultimap.merge((Collection<Map<String, Integer>>) null);
        assertEquals(0, result.totalValueCount());
    }

    @Test
    public void test_wrap_map() {
        Map<String, Set<Integer>> map = new HashMap<>();
        map.put("a", new HashSet<>(CommonUtil.toList(1, 2)));
        map.put("b", new HashSet<>(CommonUtil.toList(3, 4)));

        SetMultimap<String, Integer> wrapped = SetMultimap.wrap(map);
        assertEquals(4, wrapped.totalValueCount());
        assertTrue(wrapped.containsKey("a"));
        assertTrue(wrapped.get("a").contains(1));
        assertTrue(wrapped.get("a").contains(2));

        wrapped.put("a", 5);
        assertTrue(map.get("a").contains(5));
    }

    @Test
    public void test_wrap_withSupplier() {
        Map<String, TreeSet<Integer>> map = new HashMap<>();
        map.put("a", new TreeSet<>(CommonUtil.toList(1, 2)));

        SetMultimap<String, Integer> wrapped = SetMultimap.wrap(map, TreeSet::new);
        assertEquals(2, wrapped.totalValueCount());
        assertTrue(wrapped.containsKey("a"));

        wrapped.put("b", 3);
        assertTrue(map.containsKey("b"));
        assertTrue(map.get("b") instanceof TreeSet);
    }

    @Test
    public void test_wrap_map_emptyMap() {
        Map<String, Set<Integer>> map = new HashMap<>();
        SetMultimap<String, Integer> wrapped = SetMultimap.wrap(map);
        assertEquals(0, wrapped.totalValueCount());
    }

    @Test
    public void test_wrap_map_nullMap() {
        assertThrows(IllegalArgumentException.class, () -> {
            SetMultimap.wrap((Map<String, Set<Integer>>) null);
        });
    }

    @Test
    public void test_wrap_map_withNullValue() {
        Map<String, Set<Integer>> map = new HashMap<>();
        map.put("a", new HashSet<>(CommonUtil.toList(1, 2)));
        map.put("b", null);

        assertThrows(IllegalArgumentException.class, () -> {
            SetMultimap.wrap(map);
        });
    }

    @Test
    public void test_wrap_withSupplier_nullMap() {
        assertThrows(IllegalArgumentException.class, () -> {
            SetMultimap.wrap(null, TreeSet::new);
        });
    }

    @Test
    public void test_wrap_withSupplier_nullSupplier() {
        Map<String, Set<Integer>> map = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> {
            SetMultimap.wrap(map, null);
        });
    }

    @Test
    public void test_invert() {
        SetMultimap<String, Integer> original = SetMultimap.of("a", 1, "a", 2, "b", 1, "c", 3);
        SetMultimap<Integer, String> inverted = original.invert();

        assertEquals(4, inverted.totalValueCount());
        assertTrue(inverted.containsKey(1));
        assertTrue(inverted.containsKey(2));
        assertTrue(inverted.containsKey(3));

        assertEquals(2, inverted.get(1).size());
        assertTrue(inverted.get(1).contains("a"));
        assertTrue(inverted.get(1).contains("b"));

        assertEquals(1, inverted.get(2).size());
        assertTrue(inverted.get(2).contains("a"));

        assertEquals(1, inverted.get(3).size());
        assertTrue(inverted.get(3).contains("c"));
    }

    // ==================== invert with supplier ====================

    @Test
    public void testInvert_withSupplier() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 1);
        SetMultimap<Integer, String> inverted = map.invert(SetMultimap::new);
        assertEquals(2, inverted.get(1).size());
        assertTrue(inverted.get(1).contains("a"));
        assertTrue(inverted.get(1).contains("b"));
    }

    @Test
    public void test_inverse_emptyMap() {
        SetMultimap<String, Integer> original = CommonUtil.newSetMultimap();
        SetMultimap<Integer, String> inverted = original.invert();
        assertEquals(0, inverted.totalValueCount());
    }

    @Test
    public void test_inverse_multipleValuesPerKey() {
        SetMultimap<String, Integer> original = CommonUtil.newSetMultimap();
        original.put("a", 1);
        original.put("a", 2);
        original.put("a", 3);
        original.put("b", 2);
        original.put("b", 3);

        SetMultimap<Integer, String> inverted = original.invert();

        assertEquals(5, inverted.totalValueCount());
        assertEquals(1, inverted.get(1).size());
        assertEquals(2, inverted.get(2).size());
        assertEquals(2, inverted.get(3).size());

        assertTrue(inverted.get(1).contains("a"));
        assertTrue(inverted.get(2).contains("a"));
        assertTrue(inverted.get(2).contains("b"));
    }

    @Test
    public void test_copy() {
        SetMultimap<String, Integer> original = SetMultimap.of("a", 1, "b", 2);
        SetMultimap<String, Integer> copy = original.copy();

        assertEquals(original.totalValueCount(), copy.totalValueCount());
        assertTrue(copy.containsKey("a"));
        assertTrue(copy.containsKey("b"));
        assertTrue(copy.get("a").contains(1));
        assertTrue(copy.get("b").contains(2));

        copy.put("c", 3);
        assertFalse(original.containsKey("c"));

        copy.put("a", 10);
        assertFalse(original.get("a").contains(10));
    }

    @Test
    public void test_copy_independence() {
        SetMultimap<String, Integer> original = CommonUtil.newSetMultimap();
        original.put("a", 1);
        original.put("a", 2);

        SetMultimap<String, Integer> copy = original.copy();

        copy.put("b", 3);
        copy.put("a", 10);

        assertEquals(2, original.totalValueCount());
        assertFalse(original.containsKey("b"));
        assertEquals(2, original.get("a").size());
        assertFalse(original.get("a").contains(10));
    }

    @Test
    public void test_copy_emptyMap() {
        SetMultimap<String, Integer> original = CommonUtil.newSetMultimap();
        SetMultimap<String, Integer> copy = original.copy();
        assertEquals(0, copy.totalValueCount());
    }

    @Test
    public void test_edgeCases_emptyMultimap() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();

        assertTrue(map.isEmpty());
        assertEquals(0, map.totalValueCount());
        assertEquals(0, map.totalValueCount());
        assertFalse(map.containsKey("a"));
        assertFalse(map.containsValue(1));

        SetMultimap<String, Integer> copy = map.copy();
        assertTrue(copy.isEmpty());

        SetMultimap<Integer, String> inverted = map.invert();
        assertTrue(inverted.isEmpty());
    }

    @Test
    public void test_toImmutableMap_withSupplier() {
        SetMultimap<String, Integer> map = SetMultimap.of("b", 2, "a", 1);
        ImmutableMap<String, ImmutableSet<Integer>> immutable = map.toImmutableMap(size -> new TreeMap<>());

        assertEquals(2, immutable.size());

        List<String> keys = new ArrayList<>(immutable.keySet());
        assertEquals("a", keys.get(0));
        assertEquals("b", keys.get(1));
    }

    @Test
    public void test_toImmutableMap_empty() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();
        ImmutableMap<String, ImmutableSet<Integer>> immutable = map.toImmutableMap();
        assertEquals(0, immutable.size());
    }

    @Test
    public void test_toImmutableMap_withSupplier_empty() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();
        ImmutableMap<String, ImmutableSet<Integer>> immutable = map.toImmutableMap(size -> new LinkedHashMap<>());
        assertEquals(0, immutable.size());
    }

    //
    //
    //
    //
    //
    //
    //    @Test
    //    public void test_filterByValue_noMatches() {
    //        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2);
    //        SetMultimap<String, Integer> filtered = map.filterByValues(Set::isEmpty);
    //        assertEquals(0, filtered.size());
    //    }
    //
    @Test
    public void test_toImmutableMap() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        ImmutableMap<String, ImmutableSet<Integer>> immutable = map.toImmutableMap();

        assertEquals(2, immutable.size());
        assertTrue(immutable.containsKey("a"));
        assertTrue(immutable.containsKey("b"));

        ImmutableSet<Integer> aValues = immutable.get("a");
        assertEquals(2, aValues.size());
        assertTrue(aValues.contains(1));
        assertTrue(aValues.contains(2));

        assertThrows(UnsupportedOperationException.class, () -> {
            aValues.add(10);
        });
    }

    @Test
    public void test_multipleOperations() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();

        map.put("a", 1);
        map.put("a", 2);
        map.put("b", 3);

        assertEquals(3, map.totalValueCount());
        assertEquals(2, map.get("a").size());

        assertTrue(map.containsKey("a"));
        assertTrue(map.containsValue(3));

        map.removeEntry("a", 1);
        assertEquals(1, map.get("a").size());
        assertFalse(map.get("a").contains(1));

        map.putValues("c", CommonUtil.toList(4, 5, 6));
        assertEquals(3, map.get("c").size());
    }

    @Test
    public void test_setSemantics() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();

        map.put("a", 1);
        map.put("a", 1);

        assertEquals(1, map.get("a").size());
        assertTrue(map.get("a").contains(1));
    }

    @Test
    public void test_nullHandling() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();

        map.put(null, 1);
        assertTrue(map.containsKey(null));
        assertTrue(map.get(null).contains(1));

        map.put("a", null);
        assertTrue(map.get("a").contains(null));
    }

    @Test
    public void test_putIfAbsent() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();

        assertTrue(map.putIfValueAbsent("a", 1));
        assertEquals(1, map.get("a").size());

        assertFalse(map.putIfValueAbsent("a", 1));
        assertEquals(1, map.get("a").size());

        assertTrue(map.putIfValueAbsent("a", 2));
        assertEquals(2, map.get("a").size());
    }

    @Test
    public void test_putIfKeyAbsent() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();

        assertTrue(map.putIfKeyAbsent("a", 1));
        assertEquals(1, map.get("a").size());

        assertFalse(map.putIfKeyAbsent("a", 2));
        assertEquals(1, map.get("a").size());
        assertTrue(map.get("a").contains(1));
        assertFalse(map.get("a").contains(2));
    }

    @Test
    public void test_putManyIfKeyAbsent() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();

        assertTrue(map.putValuesIfKeyAbsent("a", CommonUtil.toList(1, 2, 3)));
        assertEquals(3, map.get("a").size());

        assertFalse(map.putValuesIfKeyAbsent("a", CommonUtil.toList(4, 5)));
        assertEquals(3, map.get("a").size());
    }

    @Test
    public void test_compute() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();

        Set<Integer> result = map.compute("a", (k, v) -> {
            Set<Integer> newSet = new HashSet<>();
            newSet.add(1);
            newSet.add(2);
            return newSet;
        });

        assertEquals(2, result.size());
        assertTrue(map.containsKey("a"));
    }

    @Test
    public void test_filter_complexPredicate() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();
        map.putValues("a", CommonUtil.toList(1, 2, 3));
        map.putValues("b", CommonUtil.toList(4));
        map.putValues("c", CommonUtil.toList(5, 6));

        //    SetMultimap<String, Integer> filtered = map.filter((k, v) -> k.compareTo("b") < 0 && v.size() > 2);
        //
        //    assertEquals(1, filtered.size());
        //    assertTrue(filtered.containsKey("a"));
        //    assertFalse(filtered.containsKey("b"));
        //    assertFalse(filtered.containsKey("c"));
    }

    @Test
    public void test_largeNumberOfValues() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();

        for (int i = 0; i < 1000; i++) {
            map.put("key", i);
        }

        assertEquals(1000, map.totalValueCount());
        assertEquals(1000, map.get("key").size());
        assertEquals(1000, map.totalValueCount());
    }

    // ==================== computeIfAbsent ====================

    @Test
    public void testComputeIfAbsent_absent() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();
        Set<Integer> result = map.computeIfAbsent("a", k -> new HashSet<>(Arrays.asList(1, 2)));
        assertEquals(2, result.size());
        assertTrue(map.containsKey("a"));
    }

    @Test
    public void testComputeIfPresent_absent() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();
        Set<Integer> result = map.computeIfPresent("a", (k, v) -> new HashSet<>(Arrays.asList(10)));
        assertFalse(map.containsKey("a"));
    }

    // ==================== putAll with Map ====================

    @Test
    public void testPutAll_map() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();
        Map<String, Integer> m = new HashMap<>();
        m.put("a", 1);
        m.put("b", 2);
        assertTrue(map.putAll(m));
        assertTrue(map.get("a").contains(1));
        assertTrue(map.get("b").contains(2));
    }

    // ==================== constructors ====================

    @Test
    public void testConstructors() {
        SetMultimap<String, Integer> m1 = new SetMultimap<>();
        assertNotNull(m1);
        assertTrue(m1.isEmpty());

        SetMultimap<String, Integer> m2 = new SetMultimap<>(10);
        assertNotNull(m2);
        assertTrue(m2.isEmpty());

        SetMultimap<String, Integer> m3 = new SetMultimap<>(LinkedHashMap.class, TreeSet.class);
        assertNotNull(m3);
        assertTrue(m3.isEmpty());

        SetMultimap<String, Integer> m4 = new SetMultimap<>(TreeMap::new, HashSet::new);
        assertNotNull(m4);
        assertTrue(m4.isEmpty());
    }
}
