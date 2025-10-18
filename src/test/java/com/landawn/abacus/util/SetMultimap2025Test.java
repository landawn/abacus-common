package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class SetMultimap2025Test extends TestBase {

    @Test
    public void test_of_1() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.get("a").contains(1));
        assertEquals(1, map.get("a").size());
    }

    @Test
    public void test_of_2() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertTrue(map.get("a").contains(1));
        assertTrue(map.get("b").contains(2));
    }

    @Test
    public void test_of_3() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2, "c", 3);
        assertEquals(3, map.size());
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
        assertEquals(4, map.size());
        assertTrue(map.containsKey("d"));
        assertTrue(map.get("d").contains(4));
    }

    @Test
    public void test_of_5() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
        assertEquals(5, map.size());
        assertTrue(map.containsKey("e"));
        assertTrue(map.get("e").contains(5));
    }

    @Test
    public void test_of_6() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);
        assertEquals(6, map.size());
        assertTrue(map.containsKey("f"));
        assertTrue(map.get("f").contains(6));
    }

    @Test
    public void test_of_7() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
        assertEquals(7, map.size());
        assertTrue(map.containsKey("g"));
        assertTrue(map.get("g").contains(7));
    }

    @Test
    public void test_of_withDuplicateKeys() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("a"));
        assertEquals(2, map.get("a").size());
        assertTrue(map.get("a").contains(1));
        assertTrue(map.get("a").contains(2));
    }

    @Test
    public void test_of_withDuplicateValues() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 1);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("a"));
        assertEquals(1, map.get("a").size());
        assertTrue(map.get("a").contains(1));
    }

    @Test
    public void test_create_fromMap() {
        Map<String, Integer> sourceMap = new HashMap<>();
        sourceMap.put("a", 1);
        sourceMap.put("b", 2);
        sourceMap.put("c", 3);

        SetMultimap<String, Integer> map = SetMultimap.create(sourceMap);
        assertEquals(3, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertTrue(map.containsKey("c"));
        assertTrue(map.get("a").contains(1));
    }

    @Test
    public void test_create_fromEmptyMap() {
        Map<String, Integer> sourceMap = new HashMap<>();
        SetMultimap<String, Integer> map = SetMultimap.create(sourceMap);
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void test_create_fromNullMap() {
        SetMultimap<String, Integer> map = SetMultimap.create((Map<String, Integer>) null);
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void test_create_withKeyExtractor() {
        List<String> words = CommonUtil.asList("apple", "ant", "banana", "bear");
        SetMultimap<Character, String> map = SetMultimap.create(words, s -> s.charAt(0));

        assertEquals(2, map.size());
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
    public void test_create_withKeyExtractor_nullExtractor() {
        List<String> words = CommonUtil.asList("apple");
        assertThrows(IllegalArgumentException.class, () -> {
            SetMultimap.create(words, null);
        });
    }

    @Test
    public void test_create_withKeyExtractor_emptyCollection() {
        List<String> words = new ArrayList<>();
        SetMultimap<Character, String> map = SetMultimap.create(words, s -> s.charAt(0));
        assertEquals(0, map.size());
    }

    @Test
    public void test_create_withKeyAndValueExtractor() {
        List<String> words = CommonUtil.asList("apple", "ant", "banana");
        SetMultimap<Character, Integer> map = SetMultimap.create(words, s -> s.charAt(0), String::length);

        assertEquals(2, map.size());
        assertTrue(map.containsKey('a'));
        assertTrue(map.containsKey('b'));
        assertTrue(map.get('a').contains(5));
        assertTrue(map.get('a').contains(3));
        assertTrue(map.get('b').contains(6));
    }

    @Test
    public void test_create_withKeyAndValueExtractor_emptyCollection() {
        List<String> words = new ArrayList<>();
        SetMultimap<Character, Integer> map = SetMultimap.create(words, s -> s.charAt(0), String::length);
        assertEquals(0, map.size());
    }

    @Test
    public void test_concat_twoMaps() {
        Map<String, Integer> map1 = CommonUtil.asMap("a", 1, "b", 2);
        Map<String, Integer> map2 = CommonUtil.asMap("c", 3, "d", 4);

        SetMultimap<String, Integer> result = SetMultimap.concat(map1, map2);
        assertEquals(4, result.size());
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsKey("b"));
        assertTrue(result.containsKey("c"));
        assertTrue(result.containsKey("d"));
    }

    @Test
    public void test_concat_twoMaps_withNulls() {
        Map<String, Integer> map1 = CommonUtil.asMap("a", 1);
        SetMultimap<String, Integer> result = SetMultimap.concat(null, map1);
        assertEquals(1, result.size());

        result = SetMultimap.concat(map1, null);
        assertEquals(1, result.size());

        result = SetMultimap.concat(null, null);
        assertEquals(0, result.size());
    }

    @Test
    public void test_concat_twoMaps_overlappingKeys() {
        Map<String, Integer> map1 = CommonUtil.asMap("a", 1);
        Map<String, Integer> map2 = CommonUtil.asMap("a", 2);

        SetMultimap<String, Integer> result = SetMultimap.concat(map1, map2);
        assertEquals(1, result.size());
        assertEquals(2, result.get("a").size());
        assertTrue(result.get("a").contains(1));
        assertTrue(result.get("a").contains(2));
    }

    @Test
    public void test_concat_threeMaps() {
        Map<String, Integer> map1 = CommonUtil.asMap("a", 1);
        Map<String, Integer> map2 = CommonUtil.asMap("b", 2);
        Map<String, Integer> map3 = CommonUtil.asMap("c", 3);

        SetMultimap<String, Integer> result = SetMultimap.concat(map1, map2, map3);
        assertEquals(3, result.size());
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsKey("b"));
        assertTrue(result.containsKey("c"));
    }

    @Test
    public void test_concat_threeMaps_withNulls() {
        Map<String, Integer> map1 = CommonUtil.asMap("a", 1);
        SetMultimap<String, Integer> result = SetMultimap.concat(null, null, map1);
        assertEquals(1, result.size());

        result = SetMultimap.concat(map1, null, null);
        assertEquals(1, result.size());
    }

    @Test
    public void test_concat_collection() {
        List<Map<String, Integer>> maps = new ArrayList<>();
        maps.add(CommonUtil.asMap("a", 1));
        maps.add(CommonUtil.asMap("b", 2));
        maps.add(CommonUtil.asMap("c", 3));

        SetMultimap<String, Integer> result = SetMultimap.concat(maps);
        assertEquals(3, result.size());
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsKey("b"));
        assertTrue(result.containsKey("c"));
    }

    @Test
    public void test_concat_emptyCollection() {
        List<Map<String, Integer>> maps = new ArrayList<>();
        SetMultimap<String, Integer> result = SetMultimap.concat(maps);
        assertEquals(0, result.size());
    }

    @Test
    public void test_concat_nullCollection() {
        SetMultimap<String, Integer> result = SetMultimap.concat((Collection<Map<String, Integer>>) null);
        assertEquals(0, result.size());
    }

    @Test
    public void test_wrap_map() {
        Map<String, Set<Integer>> map = new HashMap<>();
        map.put("a", new HashSet<>(CommonUtil.asList(1, 2)));
        map.put("b", new HashSet<>(CommonUtil.asList(3, 4)));

        SetMultimap<String, Integer> wrapped = SetMultimap.wrap(map);
        assertEquals(2, wrapped.size());
        assertTrue(wrapped.containsKey("a"));
        assertTrue(wrapped.get("a").contains(1));
        assertTrue(wrapped.get("a").contains(2));

        wrapped.put("a", 5);
        assertTrue(map.get("a").contains(5));
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
        map.put("a", new HashSet<>(CommonUtil.asList(1, 2)));
        map.put("b", null);

        assertThrows(IllegalArgumentException.class, () -> {
            SetMultimap.wrap(map);
        });
    }

    @Test
    public void test_wrap_map_emptyMap() {
        Map<String, Set<Integer>> map = new HashMap<>();
        SetMultimap<String, Integer> wrapped = SetMultimap.wrap(map);
        assertEquals(0, wrapped.size());
    }

    @Test
    public void test_wrap_withSupplier() {
        Map<String, TreeSet<Integer>> map = new HashMap<>();
        map.put("a", new TreeSet<>(CommonUtil.asList(1, 2)));

        SetMultimap<String, Integer> wrapped = SetMultimap.wrap(map, TreeSet::new);
        assertEquals(1, wrapped.size());
        assertTrue(wrapped.containsKey("a"));

        wrapped.put("b", 3);
        assertTrue(map.containsKey("b"));
        assertTrue(map.get("b") instanceof TreeSet);
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
    public void test_inverse() {
        SetMultimap<String, Integer> original = SetMultimap.of("a", 1, "a", 2, "b", 1, "c", 3);
        SetMultimap<Integer, String> inverted = original.inverse();

        assertEquals(3, inverted.size());
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

    @Test
    public void test_inverse_emptyMap() {
        SetMultimap<String, Integer> original = CommonUtil.newSetMultimap();
        SetMultimap<Integer, String> inverted = original.inverse();
        assertEquals(0, inverted.size());
    }

    @Test
    public void test_copy() {
        SetMultimap<String, Integer> original = SetMultimap.of("a", 1, "b", 2);
        SetMultimap<String, Integer> copy = original.copy();

        assertEquals(original.size(), copy.size());
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
    public void test_copy_emptyMap() {
        SetMultimap<String, Integer> original = CommonUtil.newSetMultimap();
        SetMultimap<String, Integer> copy = original.copy();
        assertEquals(0, copy.size());
    }

    @Test
    public void test_filter() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3, "c", 4, "c", 5);
        SetMultimap<String, Integer> filtered = map.filter((k, v) -> v.size() > 1);

        assertEquals(2, filtered.size());
        assertTrue(filtered.containsKey("a"));
        assertTrue(filtered.containsKey("c"));
        assertFalse(filtered.containsKey("b"));

        assertEquals(2, filtered.get("a").size());
        assertEquals(2, filtered.get("c").size());
    }

    @Test
    public void test_filter_noMatches() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2);
        SetMultimap<String, Integer> filtered = map.filter((k, v) -> v.size() > 10);
        assertEquals(0, filtered.size());
    }

    @Test
    public void test_filter_allMatch() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2);
        SetMultimap<String, Integer> filtered = map.filter((k, v) -> v.size() >= 1);
        assertEquals(2, filtered.size());
    }

    @Test
    public void test_filterByKey() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2, "c", 3);
        SetMultimap<String, Integer> filtered = map.filterByKey(k -> k.equals("a") || k.equals("b"));

        assertEquals(2, filtered.size());
        assertTrue(filtered.containsKey("a"));
        assertTrue(filtered.containsKey("b"));
        assertFalse(filtered.containsKey("c"));
    }

    @Test
    public void test_filterByKey_noMatches() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2);
        SetMultimap<String, Integer> filtered = map.filterByKey(k -> k.equals("z"));
        assertEquals(0, filtered.size());
    }

    @Test
    public void test_filterByKey_allMatch() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2);
        SetMultimap<String, Integer> filtered = map.filterByKey(k -> k != null);
        assertEquals(2, filtered.size());
    }

    @Test
    public void test_filterByValue() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        SetMultimap<String, Integer> filtered = map.filterByValue(v -> v.size() > 1);

        assertEquals(1, filtered.size());
        assertTrue(filtered.containsKey("a"));
        assertFalse(filtered.containsKey("b"));
    }

    @Test
    public void test_filterByValue_noMatches() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2);
        SetMultimap<String, Integer> filtered = map.filterByValue(Set::isEmpty);
        assertEquals(0, filtered.size());
    }

    @Test
    public void test_filterByValue_allMatch() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2);
        SetMultimap<String, Integer> filtered = map.filterByValue(v -> !v.isEmpty());
        assertEquals(2, filtered.size());
    }

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
    public void test_toImmutableMap_empty() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();
        ImmutableMap<String, ImmutableSet<Integer>> immutable = map.toImmutableMap();
        assertEquals(0, immutable.size());
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
    public void test_toImmutableMap_withSupplier_empty() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();
        ImmutableMap<String, ImmutableSet<Integer>> immutable = map.toImmutableMap(size -> new LinkedHashMap<>());
        assertEquals(0, immutable.size());
    }

    @Test
    public void test_multipleOperations() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();

        map.put("a", 1);
        map.put("a", 2);
        map.put("b", 3);

        assertEquals(2, map.size());
        assertEquals(2, map.get("a").size());

        assertTrue(map.containsKey("a"));
        assertTrue(map.containsValue(3));

        map.removeOne("a", 1);
        assertEquals(1, map.get("a").size());
        assertFalse(map.get("a").contains(1));

        map.putMany("c", CommonUtil.asList(4, 5, 6));
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
    public void test_clearAndEmpty() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2);
        assertFalse(map.isEmpty());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void test_removeAll() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);

        map.removeAll("a");
        assertEquals(1, map.size());
        assertFalse(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
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
    public void test_entrySet() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2);
        Set<Map.Entry<String, Set<Integer>>> entries = map.entrySet();

        assertEquals(2, entries.size());

        boolean foundA = false;
        boolean foundB = false;
        for (Map.Entry<String, Set<Integer>> entry : entries) {
            if (entry.getKey().equals("a")) {
                foundA = true;
                assertTrue(entry.getValue().contains(1));
            } else if (entry.getKey().equals("b")) {
                foundB = true;
                assertTrue(entry.getValue().contains(2));
            }
        }
        assertTrue(foundA);
        assertTrue(foundB);
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
    public void test_values() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        Collection<Set<Integer>> values = map.values();

        assertEquals(2, values.size());

        int totalElements = 0;
        for (Set<Integer> set : values) {
            totalElements += set.size();
        }
        assertEquals(3, totalElements);
    }

    @Test
    public void test_flatValues() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        List<Integer> flatValues = map.flatValues();

        assertEquals(3, flatValues.size());
        assertTrue(flatValues.contains(1));
        assertTrue(flatValues.contains(2));
        assertTrue(flatValues.contains(3));
    }

    @Test
    public void test_totalCountOfValues() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);
        assertEquals(3, map.totalCountOfValues());
    }

    @Test
    public void test_putMany() {
        SetMultimap<String, Integer> map1 = SetMultimap.of("a", 1);
        SetMultimap<String, Integer> map2 = SetMultimap.of("b", 2, "c", 3);

        map1.putMany(map2);
        assertEquals(3, map1.size());
        assertTrue(map1.containsKey("a"));
        assertTrue(map1.containsKey("b"));
        assertTrue(map1.containsKey("c"));
    }

    @Test
    public void test_toString() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1);
        String str = map.toString();
        assertNotNull(str);
        assertTrue(str.contains("a"));
    }

    @Test
    public void test_hashCode() {
        SetMultimap<String, Integer> map1 = SetMultimap.of("a", 1, "b", 2);
        SetMultimap<String, Integer> map2 = SetMultimap.of("a", 1, "b", 2);

        assertEquals(map1.hashCode(), map2.hashCode());
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
    public void test_putIfAbsent() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();

        assertTrue(map.putIfAbsent("a", 1));
        assertEquals(1, map.get("a").size());

        assertFalse(map.putIfAbsent("a", 1));
        assertEquals(1, map.get("a").size());

        assertTrue(map.putIfAbsent("a", 2));
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

        assertTrue(map.putManyIfKeyAbsent("a", CommonUtil.asList(1, 2, 3)));
        assertEquals(3, map.get("a").size());

        assertFalse(map.putManyIfKeyAbsent("a", CommonUtil.asList(4, 5)));
        assertEquals(3, map.get("a").size());
    }

    @Test
    public void test_removeOne() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);

        assertTrue(map.removeOne("a", 1));
        assertEquals(1, map.get("a").size());
        assertFalse(map.get("a").contains(1));

        assertFalse(map.removeOne("a", 99));
        assertEquals(1, map.get("a").size());
    }

    @Test
    public void test_removeMany() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "a", 3);

        assertTrue(map.removeMany("a", CommonUtil.asList(1, 2)));
        assertEquals(1, map.get("a").size());
        assertTrue(map.get("a").contains(3));
    }

    @Test
    public void test_contains() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2);

        assertTrue(map.contains("a", 1));
        assertTrue(map.contains("a", 2));
        assertFalse(map.contains("a", 3));
        assertFalse(map.contains("b", 1));
    }

    @Test
    public void test_containsAll() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "a", 3);

        assertTrue(map.containsAll("a", CommonUtil.asList(1, 2)));
        assertTrue(map.containsAll("a", CommonUtil.asList(1, 2, 3)));
        assertFalse(map.containsAll("a", CommonUtil.asList(1, 2, 3, 4)));
        assertFalse(map.containsAll("b", CommonUtil.asList(1)));
    }

    @Test
    public void test_getFirst() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();
        map.put("a", 1);
        map.put("a", 2);

        Integer first = map.getFirst("a");
        assertNotNull(first);
        assertTrue(first == 1 || first == 2);
    }

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
    public void test_forEach() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "b", 2);

        final int[] count = { 0 };
        map.forEach((k, v) -> {
            count[0]++;
        });
        assertEquals(2, count[0]);
    }

    @Test
    public void test_flatForEach() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2, "b", 3);

        final int[] count = { 0 };
        map.flatForEach((k, v) -> {
            count[0]++;
        });
        assertEquals(3, count[0]);
    }

    @Test
    public void test_replaceOne() {
        SetMultimap<String, Integer> map = SetMultimap.of("a", 1, "a", 2);

        assertTrue(map.replaceOne("a", 1, 10));
        assertTrue(map.get("a").contains(10));
        assertFalse(map.get("a").contains(1));
        assertTrue(map.get("a").contains(2));
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

    @Test
    public void test_inverse_multipleValuesPerKey() {
        SetMultimap<String, Integer> original = CommonUtil.newSetMultimap();
        original.put("a", 1);
        original.put("a", 2);
        original.put("a", 3);
        original.put("b", 2);
        original.put("b", 3);

        SetMultimap<Integer, String> inverted = original.inverse();

        assertEquals(3, inverted.size());
        assertEquals(1, inverted.get(1).size());
        assertEquals(2, inverted.get(2).size());
        assertEquals(2, inverted.get(3).size());

        assertTrue(inverted.get(1).contains("a"));
        assertTrue(inverted.get(2).contains("a"));
        assertTrue(inverted.get(2).contains("b"));
    }

    @Test
    public void test_copy_independence() {
        SetMultimap<String, Integer> original = CommonUtil.newSetMultimap();
        original.put("a", 1);
        original.put("a", 2);

        SetMultimap<String, Integer> copy = original.copy();

        copy.put("b", 3);
        copy.put("a", 10);

        assertEquals(1, original.size());
        assertFalse(original.containsKey("b"));
        assertEquals(2, original.get("a").size());
        assertFalse(original.get("a").contains(10));
    }

    @Test
    public void test_filter_complexPredicate() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();
        map.putMany("a", CommonUtil.asList(1, 2, 3));
        map.putMany("b", CommonUtil.asList(4));
        map.putMany("c", CommonUtil.asList(5, 6));

        SetMultimap<String, Integer> filtered = map.filter((k, v) -> k.compareTo("b") < 0 && v.size() > 2);

        assertEquals(1, filtered.size());
        assertTrue(filtered.containsKey("a"));
        assertFalse(filtered.containsKey("b"));
        assertFalse(filtered.containsKey("c"));
    }

    @Test
    public void test_edgeCases_emptyMultimap() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(0, map.totalCountOfValues());
        assertFalse(map.containsKey("a"));
        assertFalse(map.containsValue(1));

        SetMultimap<String, Integer> copy = map.copy();
        assertTrue(copy.isEmpty());

        SetMultimap<Integer, String> inverted = map.inverse();
        assertTrue(inverted.isEmpty());
    }

    @Test
    public void test_largeNumberOfValues() {
        SetMultimap<String, Integer> map = CommonUtil.newSetMultimap();

        for (int i = 0; i < 1000; i++) {
            map.put("key", i);
        }

        assertEquals(1, map.size());
        assertEquals(1000, map.get("key").size());
        assertEquals(1000, map.totalCountOfValues());
    }
}
