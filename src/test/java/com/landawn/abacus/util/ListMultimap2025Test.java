package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ListMultimap2025Test extends TestBase {

    @Test
    public void testOf_1Param() {
        ListMultimap<String, Integer> map = ListMultimap.of("a", 1);
        Assertions.assertEquals(1, map.totalValueCount());
        Assertions.assertTrue(map.get("a").contains(1));
        Assertions.assertEquals(1, map.get("a").size());
    }

    @Test
    public void testOf_2Params() {
        ListMultimap<String, Integer> map = ListMultimap.of("a", 1, "b", 2);
        Assertions.assertEquals(2, map.totalValueCount());
        Assertions.assertTrue(map.get("a").contains(1));
        Assertions.assertTrue(map.get("b").contains(2));
    }

    @Test
    public void testOf_2Params_SameKey() {
        ListMultimap<String, Integer> map = ListMultimap.of("a", 1, "a", 2);
        Assertions.assertEquals(2, map.totalValueCount());
        Assertions.assertEquals(2, map.get("a").size());
        Assertions.assertTrue(map.get("a").contains(1));
        Assertions.assertTrue(map.get("a").contains(2));
    }

    @Test
    public void testOf_3Params() {
        ListMultimap<String, Integer> map = ListMultimap.of("a", 1, "b", 2, "c", 3);
        Assertions.assertEquals(3, map.totalValueCount());
        Assertions.assertTrue(map.get("a").contains(1));
        Assertions.assertTrue(map.get("b").contains(2));
        Assertions.assertTrue(map.get("c").contains(3));
    }

    @Test
    public void testOf_3Params_DuplicateKeys() {
        ListMultimap<String, Integer> map = ListMultimap.of("a", 1, "a", 2, "b", 3);
        Assertions.assertEquals(3, map.totalValueCount());
        Assertions.assertEquals(2, map.get("a").size());
        Assertions.assertEquals(Arrays.asList(1, 2), map.get("a"));
    }

    @Test
    public void testOf_4Params() {
        ListMultimap<String, Integer> map = ListMultimap.of("a", 1, "b", 2, "c", 3, "d", 4);
        Assertions.assertEquals(4, map.totalValueCount());
        Assertions.assertTrue(map.get("a").contains(1));
        Assertions.assertTrue(map.get("d").contains(4));
    }

    @Test
    public void testOf_5Params() {
        ListMultimap<String, Integer> map = ListMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
        Assertions.assertEquals(5, map.totalValueCount());
        Assertions.assertTrue(map.get("e").contains(5));
    }

    @Test
    public void testOf_6Params() {
        ListMultimap<String, Integer> map = ListMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);
        Assertions.assertEquals(6, map.totalValueCount());
        Assertions.assertTrue(map.get("f").contains(6));
    }

    @Test
    public void testOf_7Params() {
        ListMultimap<String, Integer> map = ListMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
        Assertions.assertEquals(7, map.totalValueCount());
        Assertions.assertTrue(map.get("g").contains(7));
    }

    @Test
    public void testCreate_FromMap() {
        Map<String, Integer> sourceMap = new HashMap<>();
        sourceMap.put("a", 1);
        sourceMap.put("b", 2);

        ListMultimap<String, Integer> multimap = ListMultimap.fromMap(sourceMap);
        Assertions.assertEquals(2, multimap.totalValueCount());
        Assertions.assertEquals(Arrays.asList(1), multimap.get("a"));
        Assertions.assertEquals(Arrays.asList(2), multimap.get("b"));
    }

    @Test
    public void testCreate_FromEmptyMap() {
        Map<String, Integer> emptyMap = new HashMap<>();
        ListMultimap<String, Integer> multimap = ListMultimap.fromMap(emptyMap);
        Assertions.assertTrue(multimap.isEmpty());
    }

    @Test
    public void testCreate_FromLinkedHashMap() {
        Map<String, Integer> linkedMap = new LinkedHashMap<>();
        linkedMap.put("a", 1);
        linkedMap.put("b", 2);
        linkedMap.put("c", 3);

        ListMultimap<String, Integer> multimap = ListMultimap.fromMap(linkedMap);
        Assertions.assertEquals(3, multimap.totalValueCount());
    }

    @Test
    public void testCreate_WithKeyExtractor() {
        List<String> strings = Arrays.asList("apple", "banana", "apricot", "blueberry");
        ListMultimap<Character, String> multimap = ListMultimap.fromCollection(strings, s -> s.charAt(0));

        Assertions.assertEquals(4, multimap.totalValueCount());
        Assertions.assertEquals(2, multimap.get('a').size());
        Assertions.assertTrue(multimap.get('a').contains("apple"));
        Assertions.assertTrue(multimap.get('a').contains("apricot"));
        Assertions.assertEquals(2, multimap.get('b').size());
    }

    @Test
    public void testCreate_WithKeyExtractor_EmptyCollection() {
        List<String> strings = new ArrayList<>();
        ListMultimap<Character, String> multimap = ListMultimap.fromCollection(strings, s -> s.charAt(0));
        Assertions.assertTrue(multimap.isEmpty());
    }

    @Test
    public void testCreate_WithKeyExtractor_NullKeyExtractor() {
        List<String> strings = Arrays.asList("a", "b");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ListMultimap.fromCollection(strings, null);
        });
    }

    @Test
    public void testCreate_WithKeyAndValueExtractors() {
        List<String> strings = Arrays.asList("apple", "banana", "apricot");
        ListMultimap<Integer, String> multimap = ListMultimap.fromCollection(strings, String::length, String::toUpperCase);

        Assertions.assertEquals(Arrays.asList("APPLE"), multimap.get(5));
        Assertions.assertEquals(Arrays.asList("BANANA"), multimap.get(6));
        Assertions.assertEquals(Arrays.asList("APRICOT"), multimap.get(7));
    }

    @Test
    public void testCreate_WithKeyAndValueExtractors_NullKeyExtractor() {
        List<String> strings = Arrays.asList("a");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ListMultimap.fromCollection(strings, null, String::toUpperCase);
        });
    }

    @Test
    public void testCreate_WithKeyAndValueExtractors_NullValueExtractor() {
        List<String> strings = Arrays.asList("a");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ListMultimap.fromCollection(strings, String::length, null);
        });
    }

    @Test
    public void testConcat_TwoMaps() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 3);
        map2.put("c", 4);

        ListMultimap<String, Integer> result = ListMultimap.merge(map1, map2);
        Assertions.assertEquals(4, result.totalValueCount());
        Assertions.assertEquals(2, result.get("b").size());
        Assertions.assertTrue(result.get("b").contains(2));
        Assertions.assertTrue(result.get("b").contains(3));
    }

    @Test
    public void testConcat_TwoMaps_FirstNull() {
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 1);

        ListMultimap<String, Integer> result = ListMultimap.merge(null, map2);
        Assertions.assertEquals(1, result.totalValueCount());
        Assertions.assertTrue(result.get("a").contains(1));
    }

    @Test
    public void testConcat_TwoMaps_SecondNull() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);

        ListMultimap<String, Integer> result = ListMultimap.merge(map1, null);
        Assertions.assertEquals(1, result.totalValueCount());
        Assertions.assertTrue(result.get("a").contains(1));
    }

    @Test
    public void testConcat_TwoMaps_BothNull() {
        ListMultimap<String, Integer> result = ListMultimap.merge(null, null);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testConcat_ThreeMaps() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);

        Map<String, Integer> map3 = new HashMap<>();
        map3.put("c", 3);

        ListMultimap<String, Integer> result = ListMultimap.merge(map1, map2, map3);
        Assertions.assertEquals(3, result.totalValueCount());
        Assertions.assertTrue(result.get("a").contains(1));
        Assertions.assertTrue(result.get("b").contains(2));
        Assertions.assertTrue(result.get("c").contains(3));
    }

    @Test
    public void testConcat_ThreeMaps_FirstNull() {
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);

        Map<String, Integer> map3 = new HashMap<>();
        map3.put("c", 3);

        ListMultimap<String, Integer> result = ListMultimap.merge(null, map2, map3);
        Assertions.assertEquals(2, result.totalValueCount());
    }

    @Test
    public void testConcat_ThreeMaps_AllNull() {
        ListMultimap<String, Integer> result = ListMultimap.merge((Map<String, Integer>) null, (Map<String, Integer>) null, (Map<String, Integer>) null);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testConcat_Collection() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);

        Collection<Map<String, Integer>> maps = Arrays.asList(map1, map2);
        ListMultimap<String, Integer> result = ListMultimap.merge(maps);

        Assertions.assertEquals(2, result.totalValueCount());
        Assertions.assertTrue(result.get("a").contains(1));
        Assertions.assertTrue(result.get("b").contains(2));
    }

    @Test
    public void testConcat_Collection_Empty() {
        Collection<Map<String, Integer>> maps = new ArrayList<>();
        ListMultimap<String, Integer> result = ListMultimap.merge(maps);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testConcat_Collection_Null() {
        ListMultimap<String, Integer> result = ListMultimap.merge((Collection<Map<String, Integer>>) null);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testWrap_Map() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("a", new ArrayList<>(Arrays.asList(1, 2)));
        map.put("b", new ArrayList<>(Arrays.asList(3, 4)));

        ListMultimap<String, Integer> multimap = ListMultimap.wrap(map);
        Assertions.assertEquals(4, multimap.totalValueCount());
        Assertions.assertEquals(2, multimap.get("a").size());

        multimap.put("a", 5);
        Assertions.assertTrue(map.get("a").contains(5));
    }

    @Test
    public void testWrap_Map_NullMap() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ListMultimap.wrap((Map<String, List<Integer>>) null);
        });
    }

    @Test
    public void testWrap_Map_WithEmptyList() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("a", new ArrayList<>());

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ListMultimap.wrap(map);
        });
    }

    @Test
    public void testWrap_Map_WithNullValue() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("a", null);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ListMultimap.wrap(map);
        });
    }

    @Test
    public void testWrap_WithSupplier() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("a", new ArrayList<>(Arrays.asList(1, 2)));

        ListMultimap<String, Integer> multimap = ListMultimap.wrap(map, ArrayList::new);
        Assertions.assertEquals(2, multimap.totalValueCount());
        Assertions.assertEquals(2, multimap.get("a").size());
    }

    @Test
    public void testWrap_WithSupplier_NullMap() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ListMultimap.wrap(null, ArrayList::new);
        });
    }

    @Test
    public void testWrap_WithSupplier_NullSupplier() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("a", new ArrayList<>(Arrays.asList(1)));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ListMultimap.wrap(map, null);
        });
    }

    //    @Test
    //    public void testGetFirst() {
    //        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
    //
    //        Assertions.assertEquals(1, multimap.getFirst("a"));
    //        Assertions.assertEquals(3, multimap.getFirst("b"));
    //        Assertions.assertNull(multimap.getFirst("c"));
    //    }
    //
    //    @Test
    //    public void testGetFirst_EmptyList() {
    //        ListMultimap<String, Integer> multimap = new ListMultimap<>();
    //        Assertions.assertNull(multimap.getFirst("a"));
    //    }
    //
    //    @Test
    //    public void testGetFirstOrDefault() {
    //        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
    //
    //        Assertions.assertEquals(1, multimap.getFirstOrDefault("a", 99));
    //        Assertions.assertEquals(3, multimap.getFirstOrDefault("b", 99));
    //        Assertions.assertEquals(99, multimap.getFirstOrDefault("c", 99));
    //    }
    //
    //    @Test
    //    public void testGetFirstOrDefault_EmptyMultimap() {
    //        ListMultimap<String, Integer> multimap = new ListMultimap<>();
    //        Assertions.assertEquals(99, multimap.getFirstOrDefault("a", 99));
    //    }

    @Test
    public void testInverse() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 1);
        ListMultimap<Integer, String> inverted = multimap.inverse();

        Assertions.assertEquals(3, inverted.totalValueCount());
        Assertions.assertEquals(2, inverted.get(1).size());
        Assertions.assertTrue(inverted.get(1).contains("a"));
        Assertions.assertTrue(inverted.get(1).contains("b"));
        Assertions.assertEquals(1, inverted.get(2).size());
        Assertions.assertTrue(inverted.get(2).contains("a"));
    }

    @Test
    public void testInverse_EmptyMultimap() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        ListMultimap<Integer, String> inverted = multimap.inverse();
        Assertions.assertTrue(inverted.isEmpty());
    }

    @Test
    public void testCopy() {
        ListMultimap<String, Integer> original = ListMultimap.of("a", 1, "b", 2);
        ListMultimap<String, Integer> copy = original.copy();

        Assertions.assertEquals(original.totalValueCount(), copy.totalValueCount());
        Assertions.assertEquals(original.get("a"), copy.get("a"));

        copy.put("c", 3);
        Assertions.assertFalse(original.containsKey("c"));
        Assertions.assertEquals(2, original.totalValueCount());
    }

    @Test
    public void testCopy_EmptyMultimap() {
        ListMultimap<String, Integer> original = new ListMultimap<>();
        ListMultimap<String, Integer> copy = original.copy();
        Assertions.assertTrue(copy.isEmpty());
    }

    //    @Test
    //    public void testFilter() {
    //        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
    //
    //        BiPredicate<String, List<Integer>> filter = (k, v) -> v.size() > 1;
    //        ListMultimap<String, Integer> filtered = multimap.filter(filter);
    //
    //        Assertions.assertEquals(1, filtered.size());
    //        Assertions.assertTrue(filtered.containsKey("a"));
    //        Assertions.assertFalse(filtered.containsKey("b"));
    //        Assertions.assertEquals(2, filtered.get("a").size());
    //    }
    //
    //    @Test
    //    public void testFilter_NoMatch() {
    //        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "b", 2);
    //
    //        BiPredicate<String, List<Integer>> filter = (k, v) -> v.size() > 5;
    //        ListMultimap<String, Integer> filtered = multimap.filter(filter);
    //
    //        Assertions.assertTrue(filtered.isEmpty());
    //    }
    //
    //    @Test
    //    public void testFilter_AllMatch() {
    //        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "b", 2);
    //
    //        BiPredicate<String, List<Integer>> filter = (k, v) -> v.size() >= 1;
    //        ListMultimap<String, Integer> filtered = multimap.filter(filter);
    //
    //        Assertions.assertEquals(2, filtered.size());
    //    }

    //    @Test
    //    public void testFilterByKey() {
    //        ListMultimap<String, Integer> multimap = ListMultimap.of("apple", 1, "apricot", 2, "banana", 3);
    //
    //        Predicate<String> filter = k -> k.startsWith("a");
    //        ListMultimap<String, Integer> filtered = multimap.filterByKey(filter);
    //
    //        Assertions.assertEquals(2, filtered.size());
    //        Assertions.assertTrue(filtered.containsKey("apple"));
    //        Assertions.assertTrue(filtered.containsKey("apricot"));
    //        Assertions.assertFalse(filtered.containsKey("banana"));
    //    }
    //
    //    @Test
    //    public void testFilterByKey_NoMatch() {
    //        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "b", 2);
    //
    //        Predicate<String> filter = k -> k.startsWith("z");
    //        ListMultimap<String, Integer> filtered = multimap.filterByKey(filter);
    //
    //        Assertions.assertTrue(filtered.isEmpty());
    //    }
    //
    //    @Test
    //    public void testFilterByKey_AllMatch() {
    //        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "b", 2);
    //
    //        Predicate<String> filter = k -> k.length() > 0;
    //        ListMultimap<String, Integer> filtered = multimap.filterByKey(filter);
    //
    //        Assertions.assertEquals(2, filtered.size());
    //    }
    //
    //    @Test
    //    public void testFilterByValue() {
    //        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
    //
    //        Predicate<List<Integer>> filter = list -> list.size() >= 2;
    //        ListMultimap<String, Integer> filtered = multimap.filterByValues(filter);
    //
    //        Assertions.assertEquals(1, filtered.size());
    //        Assertions.assertTrue(filtered.containsKey("a"));
    //        Assertions.assertEquals(2, filtered.get("a").size());
    //    }
    //
    //    @Test
    //    public void testFilterByValue_NoMatch() {
    //        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "b", 2);
    //
    //        Predicate<List<Integer>> filter = list -> list.size() > 5;
    //        ListMultimap<String, Integer> filtered = multimap.filterByValues(filter);
    //
    //        Assertions.assertTrue(filtered.isEmpty());
    //    }
    //
    //    @Test
    //    public void testFilterByValue_AllMatch() {
    //        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "b", 2);
    //
    //        Predicate<List<Integer>> filter = list -> !list.isEmpty();
    //        ListMultimap<String, Integer> filtered = multimap.filterByValues(filter);
    //
    //        Assertions.assertEquals(2, filtered.size());
    //    }

    @Test
    public void testToImmutableMap() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
        ImmutableMap<String, ImmutableList<Integer>> immutable = multimap.toImmutableMap();

        Assertions.assertEquals(2, immutable.size());
        Assertions.assertEquals(2, immutable.get("a").size());
        Assertions.assertTrue(immutable.get("a").contains(1));
        Assertions.assertTrue(immutable.get("a").contains(2));

        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            immutable.put("c", ImmutableList.of(4));
        });
    }

    @Test
    public void testToImmutableMap_EmptyMultimap() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        ImmutableMap<String, ImmutableList<Integer>> immutable = multimap.toImmutableMap();
        Assertions.assertTrue(immutable.isEmpty());
    }

    @Test
    public void testToImmutableMap_WithSupplier() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "b", 2);
        ImmutableMap<String, ImmutableList<Integer>> immutable = multimap.toImmutableMap(LinkedHashMap::new);

        Assertions.assertEquals(2, immutable.size());
        Assertions.assertTrue(immutable.get("a").contains(1));
        Assertions.assertTrue(immutable.get("b").contains(2));
    }

    @Test
    public void testToImmutableMap_WithSupplier_EmptyMultimap() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        ImmutableMap<String, ImmutableList<Integer>> immutable = multimap.toImmutableMap(HashMap::new);
        Assertions.assertTrue(immutable.isEmpty());
    }

    @Test
    public void testOf_1Param_NullKey() {
        ListMultimap<String, Integer> map = ListMultimap.of(null, 1);
        Assertions.assertEquals(1, map.totalValueCount());
        Assertions.assertTrue(map.get(null).contains(1));
    }

    @Test
    public void testOf_1Param_NullValue() {
        ListMultimap<String, Integer> map = ListMultimap.of("a", null);
        Assertions.assertEquals(1, map.totalValueCount());
        Assertions.assertTrue(map.get("a").contains(null));
    }

    @Test
    public void testCopy_MaintainsStructure() {
        ListMultimap<String, Integer> original = ListMultimap.of("a", 1, "a", 2, "b", 3);
        ListMultimap<String, Integer> copy = original.copy();

        copy.put("a", 4);

        Assertions.assertEquals(2, original.get("a").size());
        Assertions.assertEquals(3, copy.get("a").size());
    }

    //    @Test
    //    public void testInverse_SingleValue() {
    //        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "b", 2, "c", 3);
    //        ListMultimap<Integer, String> inverted = multimap.inverse();
    //
    //        Assertions.assertEquals(3, inverted.size());
    //        Assertions.assertEquals(1, inverted.get(1).size());
    //        Assertions.assertEquals("a", inverted.getFirst(1));
    //    }

    @Test
    public void testConcat_Collection_WithDuplicates() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 2);

        Collection<Map<String, Integer>> maps = Arrays.asList(map1, map2);
        ListMultimap<String, Integer> result = ListMultimap.merge(maps);

        Assertions.assertEquals(2, result.totalValueCount());
        Assertions.assertEquals(2, result.get("a").size());
        Assertions.assertTrue(result.get("a").contains(1));
        Assertions.assertTrue(result.get("a").contains(2));
    }

    @Test
    public void testCreate_FromMap_NullMap() {
        ListMultimap<String, Integer> multimap = ListMultimap.fromMap((Map<String, Integer>) null);
        Assertions.assertTrue(multimap.isEmpty());
    }

    @Test
    public void testWrap_EmptyMap() {
        Map<String, List<Integer>> map = new HashMap<>();
        ListMultimap<String, Integer> multimap = ListMultimap.wrap(map);
        Assertions.assertTrue(multimap.isEmpty());
    }
}
