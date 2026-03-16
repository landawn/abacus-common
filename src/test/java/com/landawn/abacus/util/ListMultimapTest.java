package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ListMultimapTest extends TestBase {

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

    //
    //
    //
    //    @Test
    //    public void testGetFirstOrDefault_EmptyMultimap() {
    //        ListMultimap<String, Integer> multimap = new ListMultimap<>();
    //        Assertions.assertEquals(99, multimap.getFirstOrDefault("a", 99));
    //    }

    //
    //
    //
    //

    @Test
    public void testInverse() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 1);
        ListMultimap<Integer, String> inverted = multimap.invert();

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
        ListMultimap<Integer, String> inverted = multimap.invert();
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

    //
    //
    //
    //
    //
    //
    //
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

    // ==================== getOrDefault ====================

    @Test
    public void testGetOrDefault_existing() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2);
        List<Integer> result = multimap.getOrDefault("a", Arrays.asList(99));
        Assertions.assertEquals(Arrays.asList(1, 2), result);
    }

    @Test
    public void testGetOrDefault_missing() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        List<Integer> defaultVal = Arrays.asList(99);
        List<Integer> result = multimap.getOrDefault("missing", defaultVal);
        Assertions.assertSame(defaultVal, result);
    }

    // ==================== putIfValueAbsent ====================

    @Test
    public void testPutIfValueAbsent() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        Assertions.assertTrue(multimap.putIfValueAbsent("a", 1));
        Assertions.assertTrue(multimap.putIfValueAbsent("a", 2));
        Assertions.assertFalse(multimap.putIfValueAbsent("a", 1));
        Assertions.assertEquals(2, multimap.get("a").size());
    }

    // ==================== putIfKeyAbsent ====================

    @Test
    public void testPutIfKeyAbsent() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        Assertions.assertTrue(multimap.putIfKeyAbsent("a", 1));
        Assertions.assertFalse(multimap.putIfKeyAbsent("a", 2));
        Assertions.assertEquals(Arrays.asList(1), multimap.get("a"));
    }

    // ==================== putValuesIfKeyAbsent ====================

    @Test
    public void testPutValuesIfKeyAbsent() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        Assertions.assertTrue(multimap.putValuesIfKeyAbsent("a", Arrays.asList(1, 2)));
        Assertions.assertFalse(multimap.putValuesIfKeyAbsent("a", Arrays.asList(3)));
        Assertions.assertEquals(Arrays.asList(1, 2), multimap.get("a"));
    }

    // ==================== putValues with Map ====================

    @Test
    public void testPutValues_map() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        Map<String, Collection<Integer>> m = new HashMap<>();
        m.put("a", Arrays.asList(1, 2));
        m.put("b", Arrays.asList(3));
        multimap.putValues(m);
        Assertions.assertEquals(Arrays.asList(1, 2), multimap.get("a"));
        Assertions.assertEquals(Arrays.asList(3), multimap.get("b"));
    }

    // ==================== putValues with Multimap ====================

    @Test
    public void testPutValues_multimap() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        ListMultimap<String, Integer> other = ListMultimap.of("a", 1, "b", 2);
        multimap.putValues(other);
        Assertions.assertEquals(Arrays.asList(1), multimap.get("a"));
        Assertions.assertEquals(Arrays.asList(2), multimap.get("b"));
    }

    // ==================== putAll with Map ====================

    @Test
    public void testPutAll_map() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        Map<String, Integer> m = new HashMap<>();
        m.put("a", 1);
        m.put("b", 2);
        Assertions.assertTrue(multimap.putAll(m));
        Assertions.assertEquals(Arrays.asList(1), multimap.get("a"));
        Assertions.assertEquals(Arrays.asList(2), multimap.get("b"));
    }

    // ==================== removeEntries ====================

    @Test
    public void testRemoveEntries() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("a", 1);
        toRemove.put("b", 3);
        Assertions.assertTrue(multimap.removeEntries(toRemove));
        Assertions.assertEquals(Arrays.asList(2), multimap.get("a"));
        Assertions.assertNull(multimap.get("b"));
    }

    // ==================== removeValues ====================

    @Test
    public void testRemoveValues() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "a", 3);
        Assertions.assertTrue(multimap.removeValues("a", Arrays.asList(1, 3)));
        Assertions.assertEquals(Arrays.asList(2), multimap.get("a"));
    }

    @Test
    public void testRemoveValues_map() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
        Map<String, List<Integer>> m = new HashMap<>();
        m.put("a", Arrays.asList(1));
        m.put("b", Arrays.asList(3));
        Assertions.assertTrue(multimap.removeValues(m));
        Assertions.assertEquals(Arrays.asList(2), multimap.get("a"));
        Assertions.assertNull(multimap.get("b"));
    }

    @Test
    public void testRemoveValues_multimap() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
        ListMultimap<String, Integer> toRemove = ListMultimap.of("a", 1, "b", 3);
        Assertions.assertTrue(multimap.removeValues(toRemove));
        Assertions.assertEquals(Arrays.asList(2), multimap.get("a"));
        Assertions.assertNull(multimap.get("b"));
    }

    // ==================== removeKeysIf ====================

    @Test
    public void testRemoveKeysIf_predicate() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "ab", 2, "abc", 3);
        Assertions.assertTrue(multimap.removeKeysIf(k -> k.length() > 1));
        Assertions.assertEquals(1, multimap.keyCount());
        Assertions.assertTrue(multimap.containsKey("a"));
    }

    @Test
    public void testRemoveKeysIf_biPredicate() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "b", 2, "c", 3);
        Assertions.assertTrue(multimap.removeKeysIf((k, v) -> v.contains(2)));
        Assertions.assertFalse(multimap.containsKey("b"));
        Assertions.assertTrue(multimap.containsKey("a"));
    }

    // ==================== removeEntriesIf ====================

    @Test
    public void testRemoveEntriesIf_keyPredicate() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 1);
        Assertions.assertTrue(multimap.removeEntriesIf(k -> k.equals("a"), 1));
        Assertions.assertEquals(Arrays.asList(2), multimap.get("a"));
        Assertions.assertTrue(multimap.get("b").contains(1));
    }

    @Test
    public void testRemoveEntriesIf_biPredicate() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
        Assertions.assertTrue(multimap.removeEntriesIf((k, v) -> v.size() > 1, 1));
        Assertions.assertEquals(Arrays.asList(2), multimap.get("a"));
    }

    // ==================== removeValuesIf ====================

    @Test
    public void testRemoveValuesIf_keyPredicate() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
        Assertions.assertTrue(multimap.removeValuesIf(k -> k.equals("a"), Arrays.asList(1)));
        Assertions.assertEquals(Arrays.asList(2), multimap.get("a"));
    }

    @Test
    public void testRemoveValuesIf_biPredicate() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
        Assertions.assertTrue(multimap.removeValuesIf((k, v) -> v.size() > 1, Arrays.asList(1)));
        Assertions.assertEquals(Arrays.asList(2), multimap.get("a"));
    }

    // ==================== replaceEntry ====================

    @Test
    public void testReplaceEntry() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2);
        Assertions.assertTrue(multimap.replaceEntry("a", 1, 10));
        Assertions.assertTrue(multimap.get("a").contains(10));
        Assertions.assertFalse(multimap.get("a").contains(1));
    }

    @Test
    public void testReplaceEntry_notFound() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1);
        Assertions.assertFalse(multimap.replaceEntry("a", 99, 10));
    }

    // ==================== replaceValues ====================

    @Test
    public void testReplaceValues() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
        Assertions.assertTrue(multimap.replaceValues("a", Arrays.asList(10, 20, 30)));
        Assertions.assertEquals(Arrays.asList(10, 20, 30), multimap.get("a"));
    }

    @Test
    public void testReplaceValues_nonExistentKey() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1);
        Assertions.assertFalse(multimap.replaceValues("z", Arrays.asList(10)));
    }

    // ==================== replaceEntriesIf ====================

    @Test
    public void testReplaceEntriesIf_keyPredicate() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 1);
        Assertions.assertTrue(multimap.replaceEntriesIf(k -> k.equals("a"), 1, 100));
        Assertions.assertTrue(multimap.get("a").contains(100));
        Assertions.assertFalse(multimap.get("a").contains(1));
        Assertions.assertTrue(multimap.get("b").contains(1)); // b not affected
    }

    @Test
    public void testReplaceEntriesIf_biPredicate() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 1);
        Assertions.assertTrue(multimap.replaceEntriesIf((k, v) -> v.size() > 1, 1, 100));
        Assertions.assertTrue(multimap.get("a").contains(100));
    }

    // ==================== replaceValuesIf ====================

    @Test
    public void testReplaceValuesIf_keyPredicate() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
        Assertions.assertTrue(multimap.replaceValuesIf(k -> k.equals("a"), Arrays.asList(10, 20)));
        Assertions.assertEquals(Arrays.asList(10, 20), multimap.get("a"));
    }

    @Test
    public void testReplaceValuesIf_biPredicate() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
        Assertions.assertTrue(multimap.replaceValuesIf((k, v) -> v.size() > 1, Arrays.asList(10, 20)));
        Assertions.assertEquals(Arrays.asList(10, 20), multimap.get("a"));
        Assertions.assertEquals(Arrays.asList(3), multimap.get("b"));
    }

    // ==================== replaceAll ====================

    @Test
    public void testReplaceAll() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "b", 2);
        multimap.replaceAll((k, v) -> {
            List<Integer> newList = new ArrayList<>();
            for (Integer i : v) {
                newList.add(i * 10);
            }
            return newList;
        });
        Assertions.assertEquals(Arrays.asList(10), multimap.get("a"));
        Assertions.assertEquals(Arrays.asList(20), multimap.get("b"));
    }

    // ==================== computeIfAbsent ====================

    @Test
    public void testComputeIfAbsent_absent() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        List<Integer> result = multimap.computeIfAbsent("a", k -> new ArrayList<>(Arrays.asList(1, 2)));
        Assertions.assertEquals(Arrays.asList(1, 2), result);
        Assertions.assertEquals(Arrays.asList(1, 2), multimap.get("a"));
    }

    @Test
    public void testComputeIfAbsent_present() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1);
        List<Integer> result = multimap.computeIfAbsent("a", k -> new ArrayList<>(Arrays.asList(99)));
        Assertions.assertEquals(Arrays.asList(1), result);
    }

    // ==================== computeIfPresent ====================

    @Test
    public void testComputeIfPresent_present() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1);
        List<Integer> result = multimap.computeIfPresent("a", (k, v) -> new ArrayList<>(Arrays.asList(10)));
        Assertions.assertEquals(Arrays.asList(10), result);
    }

    @Test
    public void testComputeIfPresent_absent() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        List<Integer> result = multimap.computeIfPresent("a", (k, v) -> new ArrayList<>(Arrays.asList(10)));
        Assertions.assertNull(result);
    }

    // ==================== compute ====================

    @Test
    public void testCompute() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1);
        List<Integer> result = multimap.compute("a", (k, v) -> {
            List<Integer> newList = new ArrayList<>(v);
            newList.add(2);
            return newList;
        });
        Assertions.assertEquals(Arrays.asList(1, 2), result);
    }

    // ==================== merge ====================

    @Test
    public void testMerge_absent() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        List<Integer> elements = Arrays.asList(1, 2);
        multimap.merge("a", elements, (oldV, newV) -> {
            List<Integer> merged = new ArrayList<>(oldV);
            merged.addAll(newV);
            return merged;
        });
        Assertions.assertEquals(Arrays.asList(1, 2), multimap.get("a"));
    }

    @Test
    public void testMerge_present() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1);
        List<Integer> elements = Arrays.asList(2, 3);
        multimap.merge("a", elements, (oldV, newV) -> {
            List<Integer> merged = new ArrayList<>(oldV);
            merged.addAll(newV);
            return merged;
        });
        Assertions.assertEquals(Arrays.asList(1, 2, 3), multimap.get("a"));
    }

    @Test
    public void testMerge_singleElement() {
        ListMultimap<String, Integer> multimap = new ListMultimap<>();
        multimap.merge("a", 10, (oldV, newV) -> {
            List<Integer> merged = new ArrayList<>(oldV);
            merged.add(newV);
            return merged;
        });
        Assertions.assertTrue(multimap.get("a").contains(10));
    }

    // ==================== valueCollections ====================

    @Test
    public void testValueCollections() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
        Collection<List<Integer>> values = multimap.valueCollections();
        Assertions.assertEquals(2, values.size());
    }

    // ==================== allValues ====================

    @Test
    public void testAllValues() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
        Collection<Integer> all = multimap.allValues();
        Assertions.assertEquals(3, all.size());
        Assertions.assertTrue(all.contains(1));
        Assertions.assertTrue(all.contains(2));
        Assertions.assertTrue(all.contains(3));
    }

    // ==================== flatValues ====================

    @Test
    public void testFlatValues() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
        List<Integer> flat = multimap.flatValues(ArrayList::new);
        Assertions.assertEquals(3, flat.size());
        Assertions.assertTrue(flat.contains(1));
        Assertions.assertTrue(flat.contains(2));
        Assertions.assertTrue(flat.contains(3));
    }

    // ==================== entryStream ====================

    @Test
    public void testEntryStream() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
        long count = multimap.entryStream().count();
        Assertions.assertEquals(3, count);
    }

    // ==================== stream ====================

    @Test
    public void testStream() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "b", 2);
        long count = multimap.stream().count();
        Assertions.assertEquals(2, count);
    }

    // ==================== iterator ====================

    @Test
    public void testIterator() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "b", 2);
        Iterator<Map.Entry<String, List<Integer>>> iter = multimap.iterator();
        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        Assertions.assertEquals(2, count);
    }

    // ==================== toMultiset ====================

    @Test
    public void testToMultiset() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
        Multiset<String> multiset = multimap.toMultiset();
        Assertions.assertEquals(2, multiset.get("a"));
        Assertions.assertEquals(1, multiset.get("b"));
    }

    // ==================== toMap ====================

    @Test
    public void testToMap() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
        Map<String, List<Integer>> map = multimap.toMap();
        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals(Arrays.asList(1, 2), map.get("a"));
        Assertions.assertEquals(Arrays.asList(3), map.get("b"));
    }

    @Test
    public void testToMap_withSupplier() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "b", 2);
        LinkedHashMap<String, List<Integer>> map = multimap.toMap(LinkedHashMap::new);
        Assertions.assertNotNull(map);
        Assertions.assertEquals(2, map.size());
    }

    // ==================== size / keyCount ====================

    @Test
    public void testKeyCount() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
        Assertions.assertEquals(2, multimap.keyCount());
    }

    // ==================== forEach with BiConsumer ====================

    @Test
    public void testForEach_biConsumer() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 3);
        AtomicInteger counter = new AtomicInteger(0);
        multimap.forEach((k, v) -> counter.incrementAndGet());
        Assertions.assertEquals(3, counter.get());
    }

    // ==================== apply / accept ====================

    @Test
    public void testApply() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "b", 2);
        int result = multimap.apply(m -> m.totalValueCount());
        Assertions.assertEquals(2, result);
    }

    @Test
    public void testApplyIfNotEmpty() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1);
        Assertions.assertTrue(multimap.applyIfNotEmpty(m -> m.totalValueCount()).isPresent());

        ListMultimap<String, Integer> empty = new ListMultimap<>();
        Assertions.assertFalse(empty.applyIfNotEmpty(m -> m.totalValueCount()).isPresent());
    }

    @Test
    public void testAccept() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1);
        AtomicInteger counter = new AtomicInteger(0);
        multimap.accept(m -> counter.set(m.totalValueCount()));
        Assertions.assertEquals(1, counter.get());
    }

    @Test
    public void testAcceptIfNotEmpty() {
        AtomicInteger counter = new AtomicInteger(0);

        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1);
        multimap.acceptIfNotEmpty(m -> counter.set(m.totalValueCount()));
        Assertions.assertEquals(1, counter.get());

        counter.set(0);
        ListMultimap<String, Integer> empty = new ListMultimap<>();
        empty.acceptIfNotEmpty(m -> counter.set(99));
        Assertions.assertEquals(0, counter.get());
    }

    // ==================== invert with supplier ====================

    @Test
    public void testInvert_withSupplier() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("a", 1, "a", 2, "b", 1);
        ListMultimap<Integer, String> inverted = multimap.invert(ListMultimap::new);
        Assertions.assertEquals(2, inverted.get(1).size());
        Assertions.assertTrue(inverted.get(1).contains("a"));
        Assertions.assertTrue(inverted.get(1).contains("b"));
    }

}
