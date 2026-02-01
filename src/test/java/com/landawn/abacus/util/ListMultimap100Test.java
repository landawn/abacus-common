package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ListMultimap100Test extends TestBase {

    @Test
    public void testConstructors() {
        ListMultimap<String, Integer> multimap1 = new ListMultimap<>();
        Assertions.assertNotNull(multimap1);
        Assertions.assertTrue(multimap1.isEmpty());

        ListMultimap<String, Integer> multimap2 = new ListMultimap<>(10);
        Assertions.assertNotNull(multimap2);
        Assertions.assertTrue(multimap2.isEmpty());

        ListMultimap<String, Integer> multimap3 = new ListMultimap<>(LinkedHashMap.class, LinkedList.class);
        Assertions.assertNotNull(multimap3);
        Assertions.assertTrue(multimap3.isEmpty());

        ListMultimap<String, Integer> multimap4 = new ListMultimap<>(TreeMap::new, ArrayList::new);
        Assertions.assertNotNull(multimap4);
        Assertions.assertTrue(multimap4.isEmpty());
    }

    @Test
    public void testOf() {
        ListMultimap<String, Integer> map1 = ListMultimap.of("a", 1);
        Assertions.assertEquals(1, map1.totalValueCount());
        Assertions.assertTrue(map1.get("a").contains(1));

        ListMultimap<String, Integer> map2 = ListMultimap.of("a", 1, "b", 2);
        Assertions.assertEquals(2, map2.totalValueCount());
        Assertions.assertTrue(map2.get("a").contains(1));
        Assertions.assertTrue(map2.get("b").contains(2));

        ListMultimap<String, Integer> map3 = ListMultimap.of("a", 1, "b", 2, "c", 3);
        Assertions.assertEquals(3, map3.totalValueCount());

        ListMultimap<String, Integer> map4 = ListMultimap.of("a", 1, "b", 2, "c", 3, "d", 4);
        Assertions.assertEquals(4, map4.totalValueCount());

        ListMultimap<String, Integer> map5 = ListMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
        Assertions.assertEquals(5, map5.totalValueCount());

        ListMultimap<String, Integer> map6 = ListMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);
        Assertions.assertEquals(6, map6.totalValueCount());

        ListMultimap<String, Integer> map7 = ListMultimap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
        Assertions.assertEquals(7, map7.totalValueCount());
    }

    @Test
    public void testCreate() {
        Map<String, Integer> sourceMap = new HashMap<>();
        sourceMap.put("a", 1);
        sourceMap.put("b", 2);

        ListMultimap<String, Integer> multimap = ListMultimap.fromMap(sourceMap);
        Assertions.assertEquals(2, multimap.totalValueCount());
        Assertions.assertEquals(Arrays.asList(1), multimap.get("a"));
        Assertions.assertEquals(Arrays.asList(2), multimap.get("b"));

        List<String> strings = Arrays.asList("a", "bb", "ccc", "dd");
        ListMultimap<Integer, String> lengthMap = ListMultimap.fromCollection(strings, String::length);
        Assertions.assertEquals(Arrays.asList("a"), lengthMap.get(1));
        Assertions.assertEquals(Arrays.asList("bb", "dd"), lengthMap.get(2));
        Assertions.assertEquals(Arrays.asList("ccc"), lengthMap.get(3));

        ListMultimap<Integer, String> upperMap = ListMultimap.fromCollection(strings, String::length, String::toUpperCase);
        Assertions.assertEquals(Arrays.asList("A"), upperMap.get(1));
        Assertions.assertEquals(Arrays.asList("BB", "DD"), upperMap.get(2));
        Assertions.assertEquals(Arrays.asList("CCC"), upperMap.get(3));
    }

    @Test
    public void testConcat() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 3);
        map2.put("c", 4);

        ListMultimap<String, Integer> result = ListMultimap.merge(map1, map2);
        Assertions.assertEquals(Arrays.asList(1), result.get("a"));
        Assertions.assertEquals(Arrays.asList(2, 3), result.get("b"));
        Assertions.assertEquals(Arrays.asList(4), result.get("c"));

        ListMultimap<String, Integer> nullResult1 = ListMultimap.merge(null, map1);
        Assertions.assertEquals(1, nullResult1.get("a").size());

        ListMultimap<String, Integer> nullResult2 = ListMultimap.merge(map1, null);
        Assertions.assertEquals(1, nullResult2.get("a").size());

        ListMultimap<String, Integer> nullResult3 = ListMultimap.merge(null, null);
        Assertions.assertTrue(nullResult3.isEmpty());

        Map<String, Integer> map3 = new HashMap<>();
        map3.put("c", 5);
        map3.put("d", 6);

        ListMultimap<String, Integer> result3 = ListMultimap.merge(map1, map2, map3);
        Assertions.assertEquals(Arrays.asList(1), result3.get("a"));
        Assertions.assertEquals(Arrays.asList(2, 3), result3.get("b"));
        Assertions.assertEquals(Arrays.asList(4, 5), result3.get("c"));
        Assertions.assertEquals(Arrays.asList(6), result3.get("d"));

        List<Map<String, Integer>> maps = Arrays.asList(map1, map2, map3);
        ListMultimap<String, Integer> resultCollection = ListMultimap.merge(maps);
        Assertions.assertEquals(Arrays.asList(1), resultCollection.get("a"));
        Assertions.assertEquals(Arrays.asList(2, 3), resultCollection.get("b"));
        Assertions.assertEquals(Arrays.asList(4, 5), resultCollection.get("c"));
        Assertions.assertEquals(Arrays.asList(6), resultCollection.get("d"));
    }

    @Test
    public void testWrap() {
        Map<String, List<Integer>> existingMap = new HashMap<>();
        existingMap.put("a", new ArrayList<>(Arrays.asList(1, 2, 3)));
        existingMap.put("b", new ArrayList<>(Arrays.asList(4, 5)));

        ListMultimap<String, Integer> wrapped = ListMultimap.wrap(existingMap);
        Assertions.assertEquals(5, wrapped.totalValueCount());
        Assertions.assertEquals(Arrays.asList(1, 2, 3), wrapped.get("a"));
        Assertions.assertEquals(Arrays.asList(4, 5), wrapped.get("b"));

        wrapped.put("a", 6);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 6), existingMap.get("a"));

        ListMultimap<String, Integer> wrappedWithSupplier = ListMultimap.wrap(existingMap, LinkedList::new);
        Assertions.assertEquals(6, wrappedWithSupplier.totalValueCount());
    }

    //    @Test
    //    public void testGetFirst() {
    //        ListMultimap<String, Integer> multimap = new ListMultimap<>();
    //        multimap.put("a", 1);
    //        multimap.put("a", 2);
    //        multimap.put("a", 3);
    //
    //        Assertions.assertEquals(Integer.valueOf(1), multimap.getFirst("a"));
    //        Assertions.assertNull(multimap.getFirst("b"));
    //    }
    //
    //    @Test
    //    public void testGetFirstOrDefault() {
    //        ListMultimap<String, Integer> multimap = new ListMultimap<>();
    //        multimap.put("a", 1);
    //        multimap.put("a", 2);
    //
    //        Assertions.assertEquals(Integer.valueOf(1), multimap.getFirstOrDefault("a", 99));
    //        Assertions.assertEquals(Integer.valueOf(99), multimap.getFirstOrDefault("b", 99));
    //    }

    @Test
    public void testInverse() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        multimap.put("a", 1);
        multimap.put("a", 2);
        multimap.put("b", 2);
        multimap.put("b", 3);

        ListMultimap<Integer, String> inverse = multimap.invert();
        Assertions.assertEquals(Arrays.asList("a"), inverse.get(1));
        Assertions.assertEquals(Arrays.asList("a", "b"), inverse.get(2));
        Assertions.assertEquals(Arrays.asList("b"), inverse.get(3));
    }

    @Test
    public void testCopy() {
        ListMultimap<String, Integer> original = new ListMultimap<>();
        original.put("a", 1);
        original.put("a", 2);
        original.put("b", 3);

        ListMultimap<String, Integer> copy = original.copy();
        Assertions.assertEquals(original.totalValueCount(), copy.totalValueCount());
        Assertions.assertEquals(original.get("a"), copy.get("a"));
        Assertions.assertEquals(original.get("b"), copy.get("b"));

        copy.put("a", 4);
        Assertions.assertEquals(2, original.get("a").size());
        Assertions.assertEquals(3, copy.get("a").size());
    }

    //    @Test
    //    public void testFilterByKey() {
    //        ListMultimap<String, Integer> multimap = new ListMultimap<>();
    //        multimap.put("apple", 1);
    //        multimap.put("banana", 2);
    //        multimap.put("apricot", 3);
    //        multimap.put("berry", 4);
    //
    //        Predicate<String> startsWithA = k -> k.startsWith("a");
    //        ListMultimap<String, Integer> filtered = multimap.filterByKey(startsWithA);
    //
    //        Assertions.assertEquals(2, filtered.size());
    //        Assertions.assertTrue(filtered.containsKey("apple"));
    //        Assertions.assertTrue(filtered.containsKey("apricot"));
    //        Assertions.assertFalse(filtered.containsKey("banana"));
    //        Assertions.assertFalse(filtered.containsKey("berry"));
    //    }
    //
    //    @Test
    //    public void testFilterByValue() {
    //        ListMultimap<String, Integer> multimap = new ListMultimap<>();
    //        multimap.put("a", 1);
    //        multimap.put("a", 2);
    //        multimap.put("b", 3);
    //        multimap.put("c", 4);
    //        multimap.put("c", 5);
    //
    //        Predicate<List<Integer>> hasMultipleValues = list -> list.size() > 1;
    //        ListMultimap<String, Integer> filtered = multimap.filterByValues(hasMultipleValues);
    //
    //        Assertions.assertEquals(2, filtered.size());
    //        Assertions.assertTrue(filtered.containsKey("a"));
    //        Assertions.assertTrue(filtered.containsKey("c"));
    //        Assertions.assertFalse(filtered.containsKey("b"));
    //    }

    //    @Test
    //    public void testFilter() {
    //        ListMultimap<String, Integer> multimap = new ListMultimap<>();
    //        multimap.put("a", 1);
    //        multimap.put("a", 2);
    //        multimap.put("b", 10);
    //        multimap.put("c", 20);
    //
    //        BiPredicate<String, List<Integer>> filter = (k, v) -> k.equals("a") || v.stream().anyMatch(i -> i >= 10);
    //        ListMultimap<String, Integer> filtered = multimap.filter(filter);
    //
    //        Assertions.assertEquals(3, filtered.size());
    //        Assertions.assertTrue(filtered.containsKey("a"));
    //        Assertions.assertTrue(filtered.containsKey("b"));
    //        Assertions.assertTrue(filtered.containsKey("c"));
    //    }

    @Test
    public void testToImmutableMap() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        multimap.put("a", 1);
        multimap.put("a", 2);
        multimap.put("b", 3);

        ImmutableMap<String, ImmutableList<Integer>> immutableMap = multimap.toImmutableMap();
        Assertions.assertEquals(2, immutableMap.size());
        Assertions.assertEquals(ImmutableList.of(1, 2), immutableMap.get("a"));
        Assertions.assertEquals(ImmutableList.of(3), immutableMap.get("b"));
    }

    @Test
    public void testToImmutableMapWithSupplier() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        multimap.put("a", 1);
        multimap.put("a", 2);
        multimap.put("b", 3);

        ImmutableMap<String, ImmutableList<Integer>> immutableMap = multimap.toImmutableMap(LinkedHashMap::new);
        Assertions.assertEquals(2, immutableMap.size());
        Assertions.assertEquals(ImmutableList.of(1, 2), immutableMap.get("a"));
        Assertions.assertEquals(ImmutableList.of(3), immutableMap.get("b"));
    }

    @Test
    public void testPutAndGet() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();

        multimap.put("a", 1);
        Assertions.assertEquals(Arrays.asList(1), multimap.get("a"));

        multimap.put("a", 2);
        multimap.put("a", 3);
        Assertions.assertEquals(Arrays.asList(1, 2, 3), multimap.get("a"));

        List<Integer> empty = multimap.get("non-existent");
        Assertions.assertNull(empty);
    }

    @Test
    public void testPutAll() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();

        multimap.putValues("a", Arrays.asList(1, 2, 3));
        Assertions.assertEquals(Arrays.asList(1, 2, 3), multimap.get("a"));

        ListMultimap<String, Integer> other = new ListMultimap<>();
        other.put("b", 4);
        other.put("b", 5);
        other.put("c", 6);

        multimap.putValues(other);
        Assertions.assertEquals(Arrays.asList(4, 5), multimap.get("b"));
        Assertions.assertEquals(Arrays.asList(6), multimap.get("c"));
    }

    @Test
    public void testRemove() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        multimap.put("a", 1);
        multimap.put("a", 2);
        multimap.put("a", 1);
        multimap.put("b", 3);

        boolean removed = multimap.removeEntry("a", 1);
        Assertions.assertTrue(removed);
        Assertions.assertEquals(Arrays.asList(2, 1), multimap.get("a"));

        List<Integer> removedList = multimap.removeAll("a");
        Assertions.assertEquals(Arrays.asList(2, 1), removedList);
        Assertions.assertNull(multimap.get("a"));

        Assertions.assertFalse(multimap.removeEntry("non-existent", 99));
    }

    @Test
    public void testContains() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        multimap.put("a", 1);
        multimap.put("a", 2);
        multimap.put("b", 3);

        Assertions.assertTrue(multimap.containsKey("a"));
        Assertions.assertTrue(multimap.containsKey("b"));
        Assertions.assertFalse(multimap.containsKey("c"));

        Assertions.assertTrue(multimap.containsValue(1));
        Assertions.assertTrue(multimap.containsValue(2));
        Assertions.assertTrue(multimap.containsValue(3));
        Assertions.assertFalse(multimap.containsValue(4));

        Assertions.assertTrue(multimap.containsEntry("a", 1));
        Assertions.assertTrue(multimap.containsEntry("a", 2));
        Assertions.assertFalse(multimap.containsEntry("a", 3));
        Assertions.assertFalse(multimap.containsEntry("b", 1));
    }

    @Test
    public void testSize() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        Assertions.assertEquals(0, multimap.totalValueCount());
        Assertions.assertTrue(multimap.isEmpty());

        multimap.put("a", 1);
        Assertions.assertEquals(1, multimap.totalValueCount());
        Assertions.assertFalse(multimap.isEmpty());

        multimap.put("a", 2);
        Assertions.assertEquals(2, multimap.totalValueCount());

        multimap.put("b", 3);
        Assertions.assertEquals(3, multimap.totalValueCount());
    }

    @Test
    public void testClear() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        multimap.put("a", 1);
        multimap.put("b", 2);

        multimap.clear();
        Assertions.assertTrue(multimap.isEmpty());
        Assertions.assertEquals(0, multimap.totalValueCount());
    }

    @Test
    public void testKeySet() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        multimap.put("a", 1);
        multimap.put("b", 2);
        multimap.put("a", 3);

        Set<String> keys = multimap.keySet();
        Assertions.assertEquals(2, keys.size());
        Assertions.assertTrue(keys.contains("a"));
        Assertions.assertTrue(keys.contains("b"));
    }

    //    @Test
    //    public void test_valueCollections() {
    //        ListMultimap<String, Integer> multimap = new ListMultimap<>();
    //        multimap.put("a", 1);
    //        multimap.put("a", 2);
    //        multimap.put("b", 3);
    //
    //        Collection<List<Integer>> values = multimap.valueCollections();
    //        Assertions.assertEquals(2, values.size());
    //
    //        boolean hasA = false;
    //        boolean hasB = false;
    //        for (List<Integer> list : values) {
    //            if (list.equals(Arrays.asList(1, 2)))
    //                hasA = true;
    //            if (list.equals(Arrays.asList(3)))
    //                hasB = true;
    //        }
    //        Assertions.assertTrue(hasA);
    //        Assertions.assertTrue(hasB);
    //    }

    //    @Test
    //    public void testEntrySet() {
    //        ListMultimap<String, Integer> multimap = new ListMultimap<>();
    //        multimap.put("a", 1);
    //        multimap.put("a", 2);
    //        multimap.put("b", 3);
    //
    //        Set<Map.Entry<String, List<Integer>>> entries = multimap.entrySet();
    //        Assertions.assertEquals(2, entries.size());
    //
    //        for (Map.Entry<String, List<Integer>> entry : entries) {
    //            if (entry.getKey().equals("a")) {
    //                Assertions.assertEquals(Arrays.asList(1, 2), entry.getValue());
    //            } else if (entry.getKey().equals("b")) {
    //                Assertions.assertEquals(Arrays.asList(3), entry.getValue());
    //            }
    //        }
    //    }

    @Test
    public void testDuplicateValues() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();

        multimap.put("a", 1);
        multimap.put("a", 1);
        multimap.put("a", 1);

        List<Integer> values = multimap.get("a");
        Assertions.assertEquals(3, values.size());
        Assertions.assertEquals(Arrays.asList(1, 1, 1), values);
    }

    @Test
    public void testOrderPreservation() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();

        for (int i = 0; i < 10; i++) {
            multimap.put("a", i);
        }

        List<Integer> values = multimap.get("a");
        for (int i = 0; i < 10; i++) {
            Assertions.assertEquals(i, values.get(i));
        }
    }

    @Test
    public void testWithDifferentBackingTypes() {
        ListMultimap<String, Integer> treeMultimap = new ListMultimap<>(TreeMap.class, ArrayList.class);
        treeMultimap.put("c", 1);
        treeMultimap.put("a", 2);
        treeMultimap.put("b", 3);

        List<String> keys = new ArrayList<>(treeMultimap.keySet());
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), keys);

        ListMultimap<String, Integer> linkedMultimap = new ListMultimap<>(HashMap.class, LinkedList.class);
        linkedMultimap.put("a", 1);
        linkedMultimap.put("a", 2);

        List<Integer> values = linkedMultimap.get("a");
        Assertions.assertTrue(values instanceof LinkedList);
        Assertions.assertEquals(Arrays.asList(1, 2), values);
    }

    @Test
    public void testEquals() {
        ListMultimap<String, Integer> map1 = new ListMultimap<>();
        map1.put("a", 1);
        map1.put("a", 2);
        map1.put("b", 3);

        ListMultimap<String, Integer> map2 = new ListMultimap<>();
        map2.put("a", 1);
        map2.put("a", 2);
        map2.put("b", 3);

        ListMultimap<String, Integer> map3 = new ListMultimap<>();
        map3.put("a", 1);
        map3.put("b", 3);

        Assertions.assertEquals(map1, map2);
        Assertions.assertNotEquals(map1, map3);
        Assertions.assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    public void testToString() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        multimap.put("a", 1);
        multimap.put("a", 2);
        multimap.put("b", 3);

        String str = multimap.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("a"));
        Assertions.assertTrue(str.contains("b"));
        Assertions.assertTrue(str.contains("1"));
        Assertions.assertTrue(str.contains("2"));
        Assertions.assertTrue(str.contains("3"));
    }
}
