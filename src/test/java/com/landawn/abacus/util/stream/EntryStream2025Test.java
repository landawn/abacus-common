package com.landawn.abacus.util.stream;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.BiIterator;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.u.Optional;

@Tag("2025")
public class EntryStream2025Test extends TestBase {

    private Map<String, Integer> testMap;
    private Map<String, List<Integer>> multiValueMap;
    private Map<Integer, String> numberMap;
    private Map<String, Integer> emptyMap;

    @BeforeEach
    public void setUp() {
        testMap = new LinkedHashMap<>();
        testMap.put("one", 1);
        testMap.put("two", 2);
        testMap.put("three", 3);
        testMap.put("four", 4);
        testMap.put("five", 5);

        multiValueMap = new LinkedHashMap<>();
        multiValueMap.put("a", Arrays.asList(1, 2, 3));
        multiValueMap.put("b", Arrays.asList(4, 5));
        multiValueMap.put("c", Arrays.asList(6, 7, 8, 9));

        numberMap = new LinkedHashMap<>();
        numberMap.put(1, "one");
        numberMap.put(2, "two");
        numberMap.put(3, "three");

        emptyMap = new LinkedHashMap<>();
    }

    @Test
    public void testEmpty() {
        EntryStream<String, Integer> stream = EntryStream.empty();
        assertEquals(0, stream.count());
    }

    @Test
    public void testDefer() {
        AtomicBoolean called = new AtomicBoolean(false);
        EntryStream<String, Integer> stream = EntryStream.defer(() -> {
            called.set(true);
            return EntryStream.of(testMap);
        });
        assertFalse(called.get());
        assertEquals(5, stream.count());
        assertTrue(called.get());
    }

    @Test
    public void testOfNullable() {
        Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 100);
        assertEquals(1, EntryStream.ofNullable(entry).count());
        assertEquals(0, EntryStream.ofNullable(null).count());
    }

    @Test
    public void testOf_OneEntry() {
        Map<String, Integer> result = EntryStream.of("a", 1).toMap();
        assertEquals(1, result.size());
        assertEquals(1, result.get("a"));
    }

    @Test
    public void testOf_TwoEntries() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2).toMap();
        assertEquals(2, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
    }

    @Test
    public void testOf_ThreeEntries() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "c", 3).toMap();
        assertEquals(3, result.size());
        assertEquals(3, result.get("c"));
    }

    @Test
    public void testOf_FourEntries() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4).toMap();
        assertEquals(4, result.size());
        assertEquals(4, result.get("d"));
    }

    @Test
    public void testOf_FiveEntries() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5).toMap();
        assertEquals(5, result.size());
        assertEquals(5, result.get("e"));
    }

    @Test
    public void testOf_SixEntries() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6).toMap();
        assertEquals(6, result.size());
        assertEquals(6, result.get("f"));
    }

    @Test
    public void testOf_SevenEntries() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7).toMap();
        assertEquals(7, result.size());
        assertEquals(7, result.get("g"));
    }

    @Test
    public void testOf_Map() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertEquals(5, stream.count());
    }

    @Test
    public void testOf_Iterator() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap.entrySet().iterator());
        assertEquals(5, stream.count());
    }

    @Test
    public void testOf_Iterable() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap.entrySet());
        assertEquals(5, stream.count());
    }

    @Test
    public void testOf_Multimap() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        multimap.put("a", 1);
        multimap.put("a", 2);
        multimap.put("b", 3);
        EntryStream<String, List<Integer>> stream = EntryStream.of(multimap);
        assertEquals(2, stream.count());
    }

    @Test
    public void testOf_ArrayWithKeyMapper() {
        String[] array = { "apple", "banana", "cherry" };
        Map<Integer, String> result = EntryStream.of(array, String::length).toMap((v1, v2) -> v1);
        assertTrue(result.containsKey(5));
        assertTrue(result.containsKey(6));
    }

    @Test
    public void testOf_ArrayWithKeyValueMapper() {
        String[] array = { "apple", "banana", "cherry" };
        Map<Integer, String> result = EntryStream.of(array, String::length, String::toUpperCase).toMap((v1, v2) -> v1);
        assertEquals("APPLE", result.get(5));
    }

    @Test
    public void testOf_IterableWithKeyMapper() {
        List<String> list = Arrays.asList("apple", "banana", "cherry");
        Map<Integer, String> result = EntryStream.of(list, String::length).toMap((v1, v2) -> v1);
        assertTrue(result.containsKey(5));
    }

    @Test
    public void testOf_IterableWithKeyValueMapper() {
        List<String> list = Arrays.asList("apple", "banana", "cherry");
        Map<Integer, String> result = EntryStream.of(list, String::length, String::toUpperCase).toMap((v1, v2) -> v1);
        assertEquals("APPLE", result.get(5));
    }

    @Test
    public void testOf_IteratorWithKeyMapper() {
        List<String> list = Arrays.asList("apple", "banana");
        Map<Integer, String> result = EntryStream.of(list.iterator(), String::length).toMap((v1, v2) -> v1);
        assertTrue(result.containsKey(5));
    }

    @Test
    public void testOf_IteratorWithKeyValueMapper() {
        List<String> list = Arrays.asList("apple", "banana");
        Map<Integer, String> result = EntryStream.of(list.iterator(), String::length, String::toUpperCase).toMap((v1, v2) -> v1);
        assertEquals("APPLE", result.get(5));
    }

    @Test
    public void testConcat_Maps() {
        Map<String, Integer> map1 = N.asMap("a", 1);
        Map<String, Integer> map2 = N.asMap("b", 2);
        Map<String, Integer> result = EntryStream.concat(map1, map2).toMap();
        assertEquals(2, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
    }

    @Test
    public void testConcat_MapCollection() {
        List<Map<String, Integer>> maps = Arrays.asList(N.asMap("a", 1), N.asMap("b", 2));
        Map<String, Integer> result = EntryStream.concat(maps).toMap();
        assertEquals(2, result.size());
    }

    @Test
    public void testZip_Arrays() {
        String[] keys = { "a", "b", "c" };
        Integer[] values = { 1, 2, 3 };
        Map<String, Integer> result = EntryStream.zip(keys, values).toMap();
        assertEquals(3, result.size());
        assertEquals(1, result.get("a"));
    }

    @Test
    public void testZip_ArraysWithDefaults() {
        String[] keys = { "a", "b", "c" };
        Integer[] values = { 1, 2 };
        Map<String, Integer> result = EntryStream.zip(keys, values, "default", 0).toMap();
        assertEquals(3, result.size());
        assertEquals(0, result.get("c"));
    }

    @Test
    public void testZip_Iterables() {
        List<String> keys = Arrays.asList("a", "b", "c");
        List<Integer> values = Arrays.asList(1, 2, 3);
        Map<String, Integer> result = EntryStream.zip(keys, values).toMap();
        assertEquals(3, result.size());
    }

    @Test
    public void testZip_IterablesWithDefaults() {
        List<String> keys = Arrays.asList("a", "b", "c");
        List<Integer> values = Arrays.asList(1, 2);
        Map<String, Integer> result = EntryStream.zip(keys, values, "default", 0).toMap();
        assertEquals(3, result.size());
        assertEquals(0, result.get("c"));
    }

    @Test
    public void testZip_Iterators() {
        List<String> keys = Arrays.asList("a", "b");
        List<Integer> values = Arrays.asList(1, 2);
        Map<String, Integer> result = EntryStream.zip(keys.iterator(), values.iterator()).toMap();
        assertEquals(2, result.size());
    }

    @Test
    public void testZip_IteratorsWithDefaults() {
        List<String> keys = Arrays.asList("a", "b", "c");
        List<Integer> values = Arrays.asList(1, 2);
        Map<String, Integer> result = EntryStream.zip(keys.iterator(), values.iterator(), "default", 0).toMap();
        assertEquals(3, result.size());
    }

    @Test
    public void testKeys() {
        List<String> keys = EntryStream.of(testMap).keys().toList();
        assertEquals(Arrays.asList("one", "two", "three", "four", "five"), keys);
    }

    @Test
    public void testValues() {
        List<Integer> values = EntryStream.of(testMap).values().toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), values);
    }

    @Test
    public void testEntries() {
        List<Entry<String, Integer>> entries = EntryStream.of(testMap).entries().toList();
        assertEquals(5, entries.size());
    }

    @Test
    public void testInversed() {
        Map<Integer, String> inversed = EntryStream.of(testMap).inversed().toMap();
        assertEquals("one", inversed.get(1));
        assertEquals("two", inversed.get(2));
    }

    @Test
    public void testSelectByKey() {
        Map<String, Object> mixed = new LinkedHashMap<>();
        mixed.put("a", 1);
        mixed.put("b", "text");
        mixed.put("c", 3);

        EntryStream<String, Integer> result = EntryStream.of(mixed)
                .filter(e -> e.getValue() instanceof Integer)
                .mapValue(v -> (Integer) v)
                .selectByKey(String.class);
        assertEquals(2, result.count());
    }

    @Test
    public void testSelectByValue() {
        Map<Object, Integer> mixed = new LinkedHashMap<>();
        mixed.put("a", 1);
        mixed.put(2, 2);
        mixed.put("c", 3);

        EntryStream<Object, Integer> result = EntryStream.of(mixed).selectByValue(Integer.class);
        assertEquals(3, result.count());
    }

    @Test
    public void testFilter_Predicate() {
        List<Entry<String, Integer>> filtered = EntryStream.of(testMap).filter(e -> e.getValue() > 2).toList();
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilter_BiPredicate() {
        List<Entry<String, Integer>> filtered = EntryStream.of(testMap).filter((k, v) -> k.length() > 3 && v > 2).toList();
        assertEquals(3, filtered.size());
    }

    @Test
    public void testFilter_WithAction() {
        AtomicInteger dropped = new AtomicInteger(0);
        List<Entry<String, Integer>> filtered = EntryStream.of(testMap).filter(e -> e.getValue() > 3, e -> dropped.incrementAndGet()).toList();
        assertEquals(2, filtered.size());
        assertEquals(3, dropped.get());
    }

    @Test
    public void testFilterByKey() {
        List<Entry<String, Integer>> filtered = EntryStream.of(testMap).filterByKey(k -> k.startsWith("t")).toList();
        assertEquals(2, filtered.size());
    }

    @Test
    public void testFilterByValue() {
        List<Entry<String, Integer>> filtered = EntryStream.of(testMap).filterByValue(v -> v % 2 == 0).toList();
        assertEquals(2, filtered.size());
    }

    @Test
    public void testTakeWhile_Predicate() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).takeWhile(e -> e.getValue() < 4).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testTakeWhile_BiPredicate() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).takeWhile((k, v) -> v < 3).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testDropWhile_Predicate() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).dropWhile(e -> e.getValue() < 3).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testDropWhile_BiPredicate() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).dropWhile((k, v) -> v < 3).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testDropWhile_WithAction() {
        AtomicInteger dropped = new AtomicInteger(0);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).dropWhile(e -> e.getValue() < 3, e -> dropped.incrementAndGet()).toList();
        assertEquals(3, result.size());
        assertEquals(2, dropped.get());
    }

    @Test
    public void testSkipUntil_Predicate() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).skipUntil(e -> e.getValue() >= 3).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testSkipUntil_BiPredicate() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).skipUntil((k, v) -> v >= 3).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testMap_Function() {
        Map<String, String> result = EntryStream.of(testMap).map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().toString())).toMap();
        assertEquals("1", result.get("one"));
    }

    @Test
    public void testMap_BiFunction() {
        Map<String, Integer> result = EntryStream.of(testMap).map((k, v) -> new AbstractMap.SimpleEntry<>(k.toUpperCase(), v * 2)).toMap();
        assertEquals(2, result.get("ONE"));
    }

    @Test
    public void testMapKey_BiFunction() {
        Map<String, Integer> result = EntryStream.of(testMap).mapKey((k, v) -> k.toUpperCase() + v).toMap();
        assertEquals(1, result.get("ONE1"));
    }

    @Test
    public void testMapValue_Function() {
        Map<String, Integer> result = EntryStream.of(testMap).mapValue(v -> v * 10).toMap();
        assertEquals(10, result.get("one"));
    }

    @Test
    public void testMapValue_BiFunction() {
        Map<String, Integer> result = EntryStream.of(testMap).mapValue((k, v) -> v * 10).toMap();
        assertEquals(10, result.get("one"));
    }

    @Test
    public void testMapKeyPartial_Function() {
        Map<String, Integer> result = EntryStream.of(testMap)
                .mapKeyPartial(k -> k.startsWith("t") ? Optional.of(k.toUpperCase()) : Optional.<String> empty())
                .toMap();
        assertEquals(2, result.size());
        assertTrue(result.containsKey("TWO"));
    }

    @Test
    public void testMapKeyPartial_BiFunction() {
        Map<String, Integer> result = EntryStream.of(testMap).mapKeyPartial((k, v) -> v > 3 ? Optional.of(k.toUpperCase()) : Optional.<String> empty()).toMap();
        assertEquals(2, result.size());
    }

    @Test
    public void testMapValuePartial_Function() {
        Map<String, Integer> result = EntryStream.of(testMap).mapValuePartial(v -> v > 3 ? Optional.of(v * 10) : Optional.<Integer> empty()).toMap();
        assertEquals(2, result.size());
        assertEquals(40, result.get("four"));
    }

    @Test
    public void testMapValuePartial_BiFunction() {
        Map<String, Integer> result = EntryStream.of(testMap).mapValuePartial((k, v) -> v > 3 ? Optional.of(v * 10) : Optional.<Integer> empty()).toMap();
        assertEquals(2, result.size());
    }

    @Test
    public void testFlatMap_Function() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 2, "b", 1)
                .flatMap(e -> EntryStream.of(Stream.range(0, e.getValue()).map(i -> new AbstractMap.SimpleEntry<>(e.getKey() + i, i))))
                .toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testFlatMap_BiFunction() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 2)
                .flatMap((k, v) -> EntryStream.of(Stream.range(0, v).map(i -> new AbstractMap.SimpleEntry<>(k + i, i))))
                .toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testFlatmap_Function() {
        List<Entry<String, Integer>> result = EntryStream.of(multiValueMap).flatmap(e -> {
            Map<String, Integer> map = new HashMap<>();
            for (Integer i : e.getValue()) {
                map.put(e.getKey() + i, i);
            }
            return map;
        }).toList();
        assertEquals(9, result.size());
    }

    @Test
    public void testFlatmap_BiFunction() {
        List<Entry<String, Integer>> result = EntryStream.of(multiValueMap).flatmap((k, v) -> {
            Map<String, Integer> map = new HashMap<>();
            for (Integer i : v) {
                map.put(k + i, i);
            }
            return map;
        }).toList();
        assertEquals(9, result.size());
    }

    @Test
    public void testFlatMapKey_Function() {
        Map<String, Integer> result = EntryStream.of(testMap).flatMapKey(k -> Stream.of(k, k.toUpperCase())).toMap((v1, v2) -> v1);
        assertEquals(10, result.size());
    }

    @Test
    public void testFlatMapKey_BiFunction() {
        Map<String, Integer> result = EntryStream.of(testMap).flatMapKey((k, v) -> Stream.of(k, k.toUpperCase())).toMap((v1, v2) -> v1);
        assertEquals(10, result.size());
    }

    @Test
    public void testFlatmapKey_Function() {
        Map<String, Integer> result = EntryStream.of(testMap).flatmapKey(k -> Arrays.asList(k, k.toUpperCase())).toMap((v1, v2) -> v1);
        assertEquals(10, result.size());
    }

    @Test
    public void testFlatmapKey_BiFunction() {
        Map<String, Integer> result = EntryStream.of(testMap).flatmapKey((k, v) -> Arrays.asList(k, k.toUpperCase())).toMap((v1, v2) -> v1);
        assertEquals(10, result.size());
    }

    @Test
    public void testFlatMapValue_Function() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 3).flatMapValue(v -> Stream.range(1, v + 1)).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testFlatMapValue_BiFunction() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 3).flatMapValue((k, v) -> Stream.range(1, v + 1)).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testFlatmapValue_Function() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 3).flatmapValue(v -> Arrays.asList(v, v * 2, v * 3)).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testFlatmapValue_BiFunction() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 3).flatmapValue((k, v) -> Arrays.asList(v, v * 2)).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testGroupBy_Simple() {
        Map<String, List<Integer>> result = EntryStream.of("a", 1, "b", 2, "a", 3, "b", 4).groupBy().toMap();
        assertEquals(Arrays.asList(1, 3), result.get("a"));
        assertEquals(Arrays.asList(2, 4), result.get("b"));
    }

    @Test
    public void testGroupBy_WithDownstream() {
        Map<String, Long> result = EntryStream.of("a", 1, "b", 2, "a", 3).groupBy(java.util.stream.Collectors.counting()).toMap();
        assertEquals(2L, result.get("a"));
        assertEquals(1L, result.get("b"));
    }

    @Test
    public void testGroupBy_WithDownstreamAndFactory() {
        Map<String, Long> result = EntryStream.of("a", 1, "b", 2, "a", 3).groupBy(java.util.stream.Collectors.counting(), LinkedHashMap::new).toMap();
        assertEquals(2L, result.get("a"));
    }

    @Test
    public void testGroupBy_WithMergeFunction() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 3, "b", 4).groupBy(Integer::sum).toMap();
        assertEquals(4, result.get("a"));
        assertEquals(6, result.get("b"));
    }

    @Test
    public void testGroupBy_WithMergeFunctionAndFactory() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 3).groupBy(Integer::sum, LinkedHashMap::new).toMap();
        assertEquals(4, result.get("a"));
    }

    @Test
    public void testCollapseByKey() {
        List<List<Integer>> result = EntryStream.of("a", 1, "a", 2, "b", 3, "b", 4, "c", 5).collapseByKey((k1, k2) -> k1.equals(k2)).toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
    }

    @Test
    public void testCollapseByValue() {
        List<List<String>> result = EntryStream.of("a", 1, "b", 1, "c", 2, "d", 2).collapseByValue((v1, v2) -> v1.equals(v2)).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testSplit_Size() {
        List<List<Entry<String, Integer>>> result = EntryStream.of(testMap).split(2).toList();
        assertEquals(3, result.size());
        assertEquals(2, result.get(0).size());
    }

    @Test
    public void testSliding_WindowSize() {
        List<List<Entry<String, Integer>>> result = EntryStream.of(testMap).sliding(2).toList();
        assertEquals(4, result.size());
    }

    @Test
    public void testSliding_WithIncrement() {
        List<List<Entry<String, Integer>>> result = EntryStream.of(testMap).sliding(2, 2).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testIntersection() {
        Set<Entry<String, Integer>> set = new HashSet<>();
        set.add(new AbstractMap.SimpleEntry<>("two", 2));
        set.add(new AbstractMap.SimpleEntry<>("three", 3));

        List<Entry<String, Integer>> result = EntryStream.of(testMap).intersection(set).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testDifference() {
        Set<Entry<String, Integer>> set = new HashSet<>();
        set.add(new AbstractMap.SimpleEntry<>("two", 2));

        List<Entry<String, Integer>> result = EntryStream.of(testMap).difference(set).toList();
        assertEquals(4, result.size());
    }

    @Test
    public void testSymmetricDifference() {
        Set<Entry<String, Integer>> set = new HashSet<>();
        set.add(new AbstractMap.SimpleEntry<>("two", 2));
        set.add(new AbstractMap.SimpleEntry<>("six", 6));

        List<Entry<String, Integer>> result = EntryStream.of(testMap).symmetricDifference(set).toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testSorted() {
        assertThrows(UnsupportedOperationException.class, () -> EntryStream.of(3, "c", 1, "a", 2, "b").sorted().toList());
    }

    @Test
    public void testSorted_WithComparator() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())).toList();
        assertEquals(5, result.get(0).getValue());
    }

    @Test
    public void testSortedByKey() {
        List<Entry<String, Integer>> result = EntryStream.of("c", 1, "a", 2, "b", 3).sortedByKey(Comparator.naturalOrder()).toList();
        assertEquals("a", result.get(0).getKey());
    }

    @Test
    public void testSortedByValue() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).sortedByValue(Comparator.reverseOrder()).toList();
        assertEquals(5, result.get(0).getValue());
    }

    @Test
    public void testSortedBy() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).sortedBy(Entry::getValue).toList();
        assertEquals(1, result.get(0).getValue());
    }

    @Test
    public void testSortedByInt() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).sortedByInt(Entry::getValue).toList();
        assertEquals(1, result.get(0).getValue());
    }

    @Test
    public void testSortedByLong() {
        List<Entry<String, Long>> result = EntryStream.of("a", 3L, "b", 1L, "c", 2L).sortedByLong(Entry::getValue).toList();
        assertEquals(1L, result.get(0).getValue());
    }

    @Test
    public void testSortedByDouble() {
        List<Entry<String, Double>> result = EntryStream.of("a", 3.0, "b", 1.0, "c", 2.0).sortedByDouble(Entry::getValue).toList();
        assertEquals(1.0, result.get(0).getValue());
    }

    @Test
    public void testReverseSorted() {
        assertThrows(UnsupportedOperationException.class, () -> EntryStream.of(1, "a", 2, "b", 3, "c").reverseSorted().toList());
    }

    @Test
    public void testReverseSorted_WithComparator() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).reverseSorted((e1, e2) -> e1.getValue().compareTo(e2.getValue())).toList();
        assertEquals(5, result.get(0).getValue());
    }

    @Test
    public void testReverseSortedBy() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).reverseSortedBy(Entry::getValue).toList();
        assertEquals(5, result.get(0).getValue());
    }

    @Test
    public void testIndexed() {
        List<Indexed<Entry<String, Integer>>> result = EntryStream.of(testMap).indexed().toList();
        assertEquals(5, result.size());
        assertEquals(0, result.get(0).index());
        assertEquals("one", result.get(0).value().getKey());
    }

    @Test
    public void testDistinct() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "a", 1, "b", 2).distinct().toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testDistinct_WithMergeFunction() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "a", 2, "b", 3)
                .distinctBy(e -> e.getKey(), (e1, e2) -> new AbstractMap.SimpleEntry<>(e1.getKey(), e1.getValue() + e2.getValue()))
                .toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testDistinctByKey() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "a", 2, "b", 3).distinctByKey().toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testDistinctByValue() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 1, "c", 2).distinctByValue().toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testDistinctBy_Function() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).distinctBy(e -> e.getValue() % 2).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testDistinctBy_WithMergeFunction() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).distinctBy(e -> e.getValue() % 2, (e1, e2) -> e1).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testRotated() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2, "c", 3).rotated(1).toList();
        assertEquals("c", result.get(0).getKey());
    }

    @Test
    public void testShuffled() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).shuffled().toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testShuffled_WithRandom() {
        Random rnd = new Random(42);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).shuffled(rnd).toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testReversed() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).reversed().toList();
        assertEquals("five", result.get(0).getKey());
    }

    @Test
    public void testCycled() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2).cycled().limit(5).toList();
        assertEquals(5, result.size());
        assertEquals("a", result.get(2).getKey());
    }

    @Test
    public void testCycled_WithRounds() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2).cycled(2).toList();
        assertEquals(4, result.size());
    }

    @Test
    public void testPrepend_Map() {
        Map<String, Integer> prepend = N.asMap("zero", 0);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).prepend(prepend).toList();
        assertEquals(6, result.size());
        assertEquals("zero", result.get(0).getKey());
    }

    @Test
    public void testPrepend_Stream() {
        EntryStream<String, Integer> prepend = EntryStream.of("zero", 0);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).prepend(prepend).toList();
        assertEquals(6, result.size());
        assertEquals("zero", result.get(0).getKey());
    }

    @Test
    public void testPrepend_Optional() {
        Optional<Entry<String, Integer>> op = Optional.of(new AbstractMap.SimpleEntry<>("zero", 0));
        List<Entry<String, Integer>> result = EntryStream.of(testMap).prepend(op).toList();
        assertEquals(6, result.size());
        assertEquals("zero", result.get(0).getKey());
    }

    @Test
    public void testPrepend_EmptyStream() {
        EntryStream<String, Integer> prepend = EntryStream.empty();
        List<Entry<String, Integer>> result = EntryStream.of(testMap).prepend(prepend).toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testAppend_Map() {
        Map<String, Integer> append = N.asMap("six", 6);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).append(append).toList();
        assertEquals(6, result.size());
        assertEquals("six", result.get(5).getKey());
    }

    @Test
    public void testAppend_Stream() {
        EntryStream<String, Integer> append = EntryStream.of("six", 6);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).append(append).toList();
        assertEquals(6, result.size());
        assertEquals("six", result.get(5).getKey());
    }

    @Test
    public void testAppend_Optional() {
        Optional<Entry<String, Integer>> op = Optional.of(new AbstractMap.SimpleEntry<>("six", 6));
        List<Entry<String, Integer>> result = EntryStream.of(testMap).append(op).toList();
        assertEquals(6, result.size());
        assertEquals("six", result.get(5).getKey());
    }

    @Test
    public void testAppendIfEmpty_Map() {
        Map<String, Integer> append = N.asMap("default", 0);
        List<Entry<String, Integer>> result = EntryStream.<String, Integer> empty().appendIfEmpty(append).toList();
        assertEquals(1, result.size());
        assertEquals("default", result.get(0).getKey());
    }

    @Test
    public void testAppendIfEmpty_Supplier() {
        List<Entry<String, Integer>> result = EntryStream.<String, Integer> empty()
                .appendIfEmpty((Supplier<EntryStream<String, Integer>>) () -> EntryStream.of("default", 0))
                .toList();
        assertEquals(1, result.size());
    }

    @Test
    public void testIfEmpty() {
        AtomicBoolean called = new AtomicBoolean(false);
        EntryStream.<String, Integer> empty().ifEmpty(() -> called.set(true)).count();
        assertTrue(called.get());
    }

    @Test
    public void testSkip() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).skip(2).toList();
        assertEquals(3, result.size());
        assertEquals("three", result.get(0).getKey());
    }

    @Test
    public void testSkip_WithAction() {
        AtomicInteger skipped = new AtomicInteger(0);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).skip(2, e -> skipped.incrementAndGet()).toList();
        assertEquals(3, result.size());
        assertEquals(2, skipped.get());
    }

    @Test
    public void testLimit() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).limit(3).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testStep() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).step(2).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testRateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(100);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).rateLimited(rateLimiter).toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testDelay() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2).delay(com.landawn.abacus.util.Duration.ofMillis(1)).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testDelay_JavaDuration() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2).delay(Duration.ofMillis(1)).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testPeek_Consumer() {
        AtomicInteger count = new AtomicInteger(0);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).peek(e -> count.incrementAndGet()).toList();
        assertEquals(5, result.size());
        assertEquals(5, count.get());
    }

    @Test
    public void testPeek_BiConsumer() {
        AtomicInteger sum = new AtomicInteger(0);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).peek((k, v) -> sum.addAndGet(v)).toList();
        assertEquals(5, result.size());
        assertEquals(15, sum.get());
    }

    @Test
    public void testOnEach_Consumer() {
        AtomicInteger count = new AtomicInteger(0);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).onEach(e -> count.incrementAndGet()).toList();
        assertEquals(5, result.size());
        assertEquals(5, count.get());
    }

    @Test
    public void testOnEach_BiConsumer() {
        AtomicInteger sum = new AtomicInteger(0);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).onEach((k, v) -> sum.addAndGet(v)).toList();
        assertEquals(5, result.size());
        assertEquals(15, sum.get());
    }

    @Test
    public void testForEach_Consumer() {
        AtomicInteger count = new AtomicInteger(0);
        EntryStream.of(testMap).forEach(e -> count.incrementAndGet());
        assertEquals(5, count.get());
    }

    @Test
    public void testForEach_BiConsumer() {
        AtomicInteger sum = new AtomicInteger(0);
        EntryStream.of(testMap).forEach((k, v) -> sum.addAndGet(v));
        assertEquals(15, sum.get());
    }

    @Test
    public void testForEachIndexed() {
        List<Integer> indices = new ArrayList<>();
        EntryStream.of(testMap).forEachIndexed((i, e) -> indices.add(i));
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), indices);
    }

    @Test
    public void testMin() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).min(Comparator.comparing(Entry::getValue));
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getValue());
    }

    @Test
    public void testMinByKey() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).minByKey(Comparator.naturalOrder());
        assertTrue(result.isPresent());
        assertEquals("five", result.get().getKey());
    }

    @Test
    public void testMinByValue() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).minByValue(Comparator.naturalOrder());
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getValue());
    }

    @Test
    public void testMinBy() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).minBy(Entry::getValue);
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getValue());
    }

    @Test
    public void testMax() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).max(Comparator.comparing(Entry::getValue));
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getValue());
    }

    @Test
    public void testMaxByKey() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).maxByKey(Comparator.naturalOrder());
        assertTrue(result.isPresent());
        assertEquals("two", result.get().getKey());
    }

    @Test
    public void testMaxByValue() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).maxByValue(Comparator.naturalOrder());
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getValue());
    }

    @Test
    public void testMaxBy() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).maxBy(Entry::getValue);
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getValue());
    }

    @Test
    public void testAnyMatch_Predicate() {
        assertTrue(EntryStream.of(testMap).anyMatch(e -> e.getValue() > 3));
        assertFalse(EntryStream.of(testMap).anyMatch(e -> e.getValue() > 10));
    }

    @Test
    public void testAnyMatch_BiPredicate() {
        assertTrue(EntryStream.of(testMap).anyMatch((k, v) -> v > 3));
        assertFalse(EntryStream.of(testMap).anyMatch((k, v) -> v > 10));
    }

    @Test
    public void testAllMatch_Predicate() {
        assertTrue(EntryStream.of(testMap).allMatch(e -> e.getValue() > 0));
        assertFalse(EntryStream.of(testMap).allMatch(e -> e.getValue() > 3));
    }

    @Test
    public void testAllMatch_BiPredicate() {
        assertTrue(EntryStream.of(testMap).allMatch((k, v) -> v > 0));
        assertFalse(EntryStream.of(testMap).allMatch((k, v) -> v > 3));
    }

    @Test
    public void testNoneMatch_Predicate() {
        assertTrue(EntryStream.of(testMap).noneMatch(e -> e.getValue() > 10));
        assertFalse(EntryStream.of(testMap).noneMatch(e -> e.getValue() > 3));
    }

    @Test
    public void testNoneMatch_BiPredicate() {
        assertTrue(EntryStream.of(testMap).noneMatch((k, v) -> v > 10));
        assertFalse(EntryStream.of(testMap).noneMatch((k, v) -> v > 3));
    }

    @Test
    public void testNMatch_Predicate() {
        assertTrue(EntryStream.of(testMap).nMatch(2, 3, e -> e.getValue() > 3));
        assertFalse(EntryStream.of(testMap).nMatch(5, 10, e -> e.getValue() > 3));
    }

    @Test
    public void testNMatch_BiPredicate() {
        assertTrue(EntryStream.of(testMap).nMatch(2, 3, (k, v) -> v > 3));
        assertFalse(EntryStream.of(testMap).nMatch(5, 10, (k, v) -> v > 3));
    }

    @Test
    public void testFindFirst_Predicate() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).findFirst(e -> e.getValue() > 3);
        assertTrue(result.isPresent());
        assertEquals(4, result.get().getValue());
    }

    @Test
    public void testFindFirst_BiPredicate() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).findFirst((k, v) -> v > 3);
        assertTrue(result.isPresent());
        assertEquals(4, result.get().getValue());
    }

    @Test
    public void testFindAny_Predicate() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).findAny(e -> e.getValue() > 3);
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_BiPredicate() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).findAny((k, v) -> v > 3);
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindLast_Predicate() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).findLast(e -> e.getValue() < 5);
        assertTrue(result.isPresent());
        assertEquals(4, result.get().getValue());
    }

    @Test
    public void testFindLast_BiPredicate() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).findLast((k, v) -> v < 5);
        assertTrue(result.isPresent());
        assertEquals(4, result.get().getValue());
    }

    @Test
    public void testFirst() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).first();
        assertTrue(result.isPresent());
        assertEquals("one", result.get().getKey());
    }

    @Test
    public void testLast() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).last();
        assertTrue(result.isPresent());
        assertEquals("five", result.get().getKey());
    }

    @Test
    public void testElementAt() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).elementAt(2);
        assertTrue(result.isPresent());
        assertEquals("three", result.get().getKey());
    }

    @Test
    public void testOnlyOne() {
        Optional<Entry<String, Integer>> result = EntryStream.of("a", 1).onlyOne();
        assertTrue(result.isPresent());
        assertEquals("a", result.get().getKey());

        assertThrows(Exception.class, () -> EntryStream.of(testMap).onlyOne());
    }

    @Test
    public void testPercentiles() {
        assertThrows(UnsupportedOperationException.class, () -> EntryStream.of(testMap).percentiles());
    }

    @Test
    public void testPercentiles_WithComparator() {
        Optional<Map<Percentage, Entry<String, Integer>>> result = EntryStream.of(testMap).percentiles(Comparator.comparing(Entry::getValue));
        assertTrue(result.isPresent());
    }

    @Test
    public void testCount() {
        assertEquals(5, EntryStream.of(testMap).count());
        assertEquals(0, EntryStream.empty().count());
    }

    @Test
    public void testIterator() {
        ObjIterator<Entry<String, Integer>> iter = EntryStream.of(testMap).iterator();
        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        assertEquals(5, count);
    }

    @Test
    public void testBiIterator() {
        BiIterator<String, Integer> iter = EntryStream.of(testMap).biIterator();
        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        iter.forEachRemaining((k, v) -> {
            keys.add(k);
            values.add(v);
        });

        assertEquals(5, keys.size());
        assertEquals(5, values.size());
    }

    @Test
    public void testToList() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testToSet() {
        Set<Entry<String, Integer>> result = EntryStream.of(testMap).toSet();
        assertEquals(5, result.size());
    }

    @Test
    public void testToCollection() {
        Collection<Entry<String, Integer>> result = EntryStream.of(testMap).toCollection(ArrayList::new);
        assertEquals(5, result.size());
    }

    @Test
    public void testToMultiset() {
        Multiset<Entry<String, Integer>> result = EntryStream.of("a", 1, "a", 1, "b", 2).toMultiset();
        assertEquals(2, result.get(new AbstractMap.SimpleEntry<>("a", 1)));
    }

    @Test
    public void testToMultiset_WithSupplier() {
        Multiset<Entry<String, Integer>> result = EntryStream.of("a", 1, "a", 1).toMultiset(Multiset::new);
        assertEquals(2, result.get(new AbstractMap.SimpleEntry<>("a", 1)));
    }

    @Test
    public void testToMap() {
        Map<String, Integer> result = EntryStream.of(testMap).toMap();
        assertEquals(5, result.size());
        assertEquals(1, result.get("one"));
    }

    @Test
    public void testToMap_WithMergeFunction() {
        Map<String, Integer> result = EntryStream.of("a", 1, "a", 2, "b", 3).toMap(Integer::sum);
        assertEquals(2, result.size());
        assertEquals(3, result.get("a"));
    }

    @Test
    public void testToMap_WithMergeFunctionAndSupplier() {
        Map<String, Integer> result = EntryStream.of("a", 1, "a", 2).toMap(Integer::sum, LinkedHashMap::new);
        assertEquals(1, result.size());
        assertEquals(3, result.get("a"));
    }

    @Test
    public void testToMapThenApply() {
        int sum = EntryStream.of(testMap).toMapThenApply(map -> map.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(15, sum);
    }

    @Test
    public void testToMapThenAccept() {
        AtomicInteger sum = new AtomicInteger(0);
        EntryStream.of(testMap).toMapThenAccept(map -> map.values().forEach(sum::addAndGet));
        assertEquals(15, sum.get());
    }

    @Test
    public void testToImmutableMap() {
        ImmutableMap<String, Integer> result = EntryStream.of(testMap).toImmutableMap();
        assertEquals(5, result.size());
        assertThrows(UnsupportedOperationException.class, () -> result.put("six", 6));
    }

    @Test
    public void testToImmutableMap_WithMergeFunction() {
        ImmutableMap<String, Integer> result = EntryStream.of("a", 1, "a", 2).toImmutableMap(Integer::sum);
        assertEquals(1, result.size());
        assertEquals(3, result.get("a"));
    }

    @Test
    public void testToMultimap() {
        ListMultimap<String, Integer> result = EntryStream.of("a", 1, "a", 2, "b", 3).toMultimap();
        assertEquals(2, result.get("a").size());
    }

    @Test
    public void testToMultimap_WithSupplier() {
        ListMultimap<String, Integer> result = EntryStream.of("a", 1, "a", 2).toMultimap(N::newListMultimap);
        assertEquals(2, result.get("a").size());
    }

    @Test
    public void testGroupTo() {
        Map<String, List<Integer>> result = EntryStream.of("a", 1, "a", 2, "b", 3).groupTo();
        assertEquals(2, result.get("a").size());
        assertEquals(Arrays.asList(1, 2), result.get("a"));
    }

    @Test
    public void testGroupTo_WithSupplier() {
        Map<String, List<Integer>> result = EntryStream.of("a", 1, "a", 2, "b", 3).groupTo(LinkedHashMap::new);
        assertEquals(2, result.size());
        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testGroupToThenApply() {
        int totalSize = EntryStream.of("a", 1, "a", 2, "b", 3, "b", 4).groupToThenApply(map -> map.values().stream().mapToInt(List::size).sum());
        assertEquals(4, totalSize);
    }

    @Test
    public void testGroupToThenAccept() {
        AtomicInteger totalSize = new AtomicInteger(0);
        EntryStream.of("a", 1, "a", 2, "b", 3).groupToThenAccept(map -> map.values().forEach(list -> totalSize.addAndGet(list.size())));
        assertEquals(3, totalSize.get());
    }

    @Test
    public void testReduce_WithIdentity() {
        Entry<String, Integer> result = EntryStream.of("a", 1, "b", 2)
                .reduce(new AbstractMap.SimpleEntry<>("sum", 0), (e1, e2) -> new AbstractMap.SimpleEntry<>("sum", e1.getValue() + e2.getValue()));
        assertEquals(3, result.getValue());
    }

    @Test
    public void testReduce_WithoutIdentity() {
        Optional<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2)
                .reduce((e1, e2) -> new AbstractMap.SimpleEntry<>("sum", e1.getValue() + e2.getValue()));
        assertTrue(result.isPresent());
        assertEquals(3, result.get().getValue());
    }

    @Test
    public void testCollect_SupplierAccumulatorCombiner() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).collect(ArrayList::new, List::add, List::addAll);
        assertEquals(5, result.size());
    }

    @Test
    public void testCollect_SupplierAccumulator() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).collect(ArrayList::new, List::add);
        assertEquals(5, result.size());
    }

    @Test
    public void testCollect_Collector() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).collect(java.util.stream.Collectors.toList());
        assertEquals(5, result.size());
    }

    @Test
    public void testCollectThenApply() {
        int size = EntryStream.of(testMap).collectThenApply(java.util.stream.Collectors.toList(), List::size);
        assertEquals(5, size);
    }

    @Test
    public void testCollectThenAccept() {
        AtomicInteger size = new AtomicInteger(0);
        EntryStream.of(testMap).collectThenAccept(java.util.stream.Collectors.toList(), list -> size.set(list.size()));
        assertEquals(5, size.get());
    }

    @Test
    public void testJoin_Delimiter() {
        String result = EntryStream.of("a", 1, "b", 2).join(", ");
        assertTrue(result.contains("a"));
        assertTrue(result.contains("1"));
    }

    @Test
    public void testJoin_DelimiterPrefixSuffix() {
        String result = EntryStream.of("a", 1, "b", 2).join(", ", "[", "]");
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
    }

    @Test
    public void testJoin_WithKeyValueDelimiter() {
        String result = EntryStream.of("a", 1, "b", 2).join(", ", "=");
        assertTrue(result.contains("a=1"));
    }

    @Test
    public void testJoin_WithKeyValueDelimiterAndPrefixSuffix() {
        String result = EntryStream.of("a", 1, "b", 2).join(", ", "=", "{", "}");
        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}"));
        assertTrue(result.contains("a=1"));
    }

    @Test
    public void testJoinTo() {
        Joiner joiner = Joiner.with(", ");
        Joiner result = EntryStream.of("a", 1, "b", 2).joinTo(joiner);
        assertNotNull(result);
    }

    @Test
    public void testIsParallel() {
        assertFalse(EntryStream.of(testMap).isParallel());
        assertTrue(EntryStream.of(testMap).parallel().isParallel());
    }

    @Test
    public void testSequential() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap).parallel();
        assertTrue(stream.isParallel());
        stream = stream.sequential();
        assertFalse(stream.isParallel());
    }

    @Test
    public void testParallel() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap).parallel();
        assertTrue(stream.isParallel());
        assertEquals(5, stream.count());
    }

    @Test
    public void testParallel_WithMaxThreadNum() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap).parallel(2);
        assertTrue(stream.isParallel());
        assertEquals(5, stream.count());
    }

    @Test
    public void testSps() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).sps(s -> s.filter(e -> e.getValue() > 2)).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testPsp() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).parallel().psp(s -> s.filter(e -> e.getValue() > 2)).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);
        EntryStream.of(testMap).onClose(() -> closed.set(true)).count();
        assertTrue(closed.get());
    }

    @Test
    public void testClose() {
        AtomicBoolean closed = new AtomicBoolean(false);
        EntryStream<String, Integer> stream = EntryStream.of(testMap).onClose(() -> closed.set(true));
        stream.close();
        assertTrue(closed.get());
    }

    @Test
    public void testFilter_PredicateFromBase() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap)
                .filter((java.util.function.Predicate<Entry<String, Integer>>) e -> e.getValue() > 2)
                .toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testTakeWhile_PredicateFromBase() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap)
                .takeWhile((java.util.function.Predicate<Entry<String, Integer>>) e -> e.getValue() < 4)
                .toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testDropWhile_PredicateFromBase() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap)
                .dropWhile((java.util.function.Predicate<Entry<String, Integer>>) e -> e.getValue() < 3)
                .toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testSkipUntil_PredicateFromBase() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap)
                .skipUntil((java.util.function.Predicate<Entry<String, Integer>>) e -> e.getValue() >= 3)
                .toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testDistinct_FromBase() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "a", 1, "b", 2).distinct().toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testIntersection_FromBase() {
        Set<Entry<String, Integer>> set = new HashSet<>();
        set.add(new AbstractMap.SimpleEntry<>("two", 2));

        List<Entry<String, Integer>> result = EntryStream.of(testMap).intersection((Collection<?>) set).toList();
        assertEquals(1, result.size());
    }

    @Test
    public void testDifference_FromBase() {
        Set<Entry<String, Integer>> set = new HashSet<>();
        set.add(new AbstractMap.SimpleEntry<>("two", 2));

        List<Entry<String, Integer>> result = EntryStream.of(testMap).difference((Collection<?>) set).toList();
        assertEquals(4, result.size());
    }

    @Test
    public void testReversed_FromBase() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).reversed().toList();
        assertEquals(5, result.size());
        assertEquals("five", result.get(0).getKey());
    }

    @Test
    public void testRotated_FromBase() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2, "c", 3).rotated(1).toList();
        assertEquals("c", result.get(0).getKey());
    }

    @Test
    public void testShuffled_FromBase() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).shuffled().toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testShuffled_WithRandomFromBase() {
        Random rnd = new Random(42);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).shuffled(rnd).toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testCycled_FromBase() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2).cycled().limit(5).toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testCycled_WithRoundsFromBase() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2).cycled(2).toList();
        assertEquals(4, result.size());
    }

    @Test
    public void testIndexed_FromBase() {
        List<Indexed<Entry<String, Integer>>> result = EntryStream.of(testMap).indexed().toList();
        assertEquals(5, result.size());
        assertEquals(0, result.get(0).index());
    }

    @Test
    public void testSkip_FromBase() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).skip(2).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testLimit_FromBase() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).limit(3).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testStep_FromBase() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).step(2).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testOnEach_ConsumerFromBase() {
        AtomicInteger count = new AtomicInteger(0);
        List<Entry<String, Integer>> result = EntryStream.of(testMap)
                .onEach((java.util.function.Consumer<Entry<String, Integer>>) e -> count.incrementAndGet())
                .toList();
        assertEquals(5, result.size());
        assertEquals(5, count.get());
    }

    @Test
    public void testPeek_ConsumerFromBase() {
        AtomicInteger count = new AtomicInteger(0);
        List<Entry<String, Integer>> result = EntryStream.of(testMap)
                .peek((java.util.function.Consumer<Entry<String, Integer>>) e -> count.incrementAndGet())
                .toList();
        assertEquals(5, result.size());
        assertEquals(5, count.get());
    }

    @Test
    public void testThrowIfEmpty() {
        assertThrows(Exception.class, () -> EntryStream.<String, Integer> empty().throwIfEmpty().count());
    }

    @Test
    public void testThrowIfEmpty_WithSupplier() {
        assertThrows(IllegalStateException.class, () -> EntryStream.<String, Integer> empty().throwIfEmpty(() -> new IllegalStateException("Empty")).count());
    }

    @Test
    public void testIfEmpty_FromBase() {
        AtomicBoolean called = new AtomicBoolean(false);
        EntryStream.<String, Integer> empty().ifEmpty(() -> called.set(true)).count();
        assertTrue(called.get());
    }

    @Test
    public void testJoin_DelimiterFromBase() {
        String result = EntryStream.of("a", 1, "b", 2).join(", ");
        assertNotNull(result);
    }

    @Test
    public void testCount_FromBase() {
        assertEquals(5, EntryStream.of(testMap).count());
    }

    @Test
    public void testFirst_FromBase() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).first();
        assertTrue(result.isPresent());
    }

    @Test
    public void testLast_FromBase() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).last();
        assertTrue(result.isPresent());
    }

    @Test
    public void testElementAt_FromBase() {
        Optional<Entry<String, Integer>> result = EntryStream.of(testMap).elementAt(2);
        assertTrue(result.isPresent());
    }

    @Test
    public void testOnlyOne_FromBase() {
        Optional<Entry<String, Integer>> result = EntryStream.of("a", 1).onlyOne();
        assertTrue(result.isPresent());
    }

    @Test
    public void testToArray() {
        Object[] result = EntryStream.of(testMap).toArray();
        assertEquals(5, result.length);
    }

    @Test
    public void testToList_FromBase() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testToSet_FromBase() {
        Set<Entry<String, Integer>> result = EntryStream.of(testMap).toSet();
        assertEquals(5, result.size());
    }

    @Test
    public void testToImmutableList() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).toImmutableList();
        assertEquals(5, result.size());
        assertThrows(UnsupportedOperationException.class, () -> result.add(new AbstractMap.SimpleEntry<>("six", 6)));
    }

    @Test
    public void testToImmutableSet() {
        Set<Entry<String, Integer>> result = EntryStream.of(testMap).toImmutableSet();
        assertEquals(5, result.size());
        assertThrows(UnsupportedOperationException.class, () -> result.add(new AbstractMap.SimpleEntry<>("six", 6)));
    }

    @Test
    public void testToCollection_FromBase() {
        Collection<Entry<String, Integer>> result = EntryStream.of(testMap).toCollection(ArrayList::new);
        assertEquals(5, result.size());
    }

    @Test
    public void testToMultiset_FromBase() {
        Multiset<Entry<String, Integer>> result = EntryStream.of("a", 1, "a", 1).toMultiset();
        assertNotNull(result);
    }

    @Test
    public void testPrintln() {
        EntryStream.of("a", 1, "b", 2).println();
    }

    @Test
    public void testIterator_FromBase() {
        ObjIterator<Entry<String, Integer>> iter = EntryStream.of(testMap).iterator();
        assertNotNull(iter);
    }

    @Test
    public void testAcceptIfNotEmpty() {
        AtomicInteger sum = new AtomicInteger(0);
        EntryStream.of(testMap).acceptIfNotEmpty(s -> s.forEach(e -> sum.addAndGet(e.getValue())));
        assertEquals(15, sum.get());
    }

    @Test
    public void testTransformB_Deferred() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).<String, Integer> transformB(s -> s.filter(e -> e.getValue() > 2), true).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testCollectThenApply_DownstreamAndMapper() {
        List<Integer> values = EntryStream.of(testMap)
                .collectThenApply(java.util.stream.Collectors.toList(),
                        list -> list.stream().map(Entry::getValue).collect(java.util.stream.Collectors.toList()));
        assertEquals(5, values.size());
    }

    @Test
    public void testCollectThenAccept_DownstreamAndConsumer() {
        AtomicInteger count = new AtomicInteger(0);
        EntryStream.of(testMap).collectThenAccept(java.util.stream.Collectors.toList(), list -> count.set(list.size()));
        assertEquals(5, count.get());
    }

    @Test
    public void testRateLimited_Double() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2).rateLimited(100.0).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testMap_KeyAndValueFunction() {
        Map<String, String> result = EntryStream.of(testMap).map(Entry::getKey, e -> e.getValue().toString()).toMap();
        assertEquals("1", result.get("one"));
    }

    @Test
    public void testToArray_FromEntryStream() {
        Object[] result = EntryStream.of(testMap).toArray();
        assertEquals(5, result.length);
        assertTrue(result[0] instanceof Entry);
    }

    @Test
    public void testPrepend_MultipleEntries() {
        Map<String, Integer> prepend1 = N.asMap("zero", 0);
        Map<String, Integer> prepend2 = N.asMap("negative", -1);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).prepend(prepend1).prepend(prepend2).toList();
        assertEquals(7, result.size());
        assertEquals("negative", result.get(0).getKey());
    }

    @Test
    public void testAppend_MultipleEntries() {
        Map<String, Integer> append1 = N.asMap("six", 6);
        Map<String, Integer> append2 = N.asMap("seven", 7);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).append(append1).append(append2).toList();
        assertEquals(7, result.size());
        assertEquals("seven", result.get(6).getKey());
    }

    @Test
    public void testParallelStreamOperations() {
        Map<String, Integer> result = EntryStream.of(testMap).parallel().filter(e -> e.getValue() > 2).mapValue(v -> v * 2).sequential().toMap();

        assertEquals(3, result.size());
        assertEquals(6, result.get("three"));
    }

    @Test
    public void testNullHandling() {
        Map<String, Integer> mapWithNull = new LinkedHashMap<>();
        mapWithNull.put("a", 1);
        mapWithNull.put("b", null);
        mapWithNull.put("c", 3);

        List<Entry<String, Integer>> result = EntryStream.of(mapWithNull).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testLargeStream() {
        Map<Integer, String> largeMap = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            largeMap.put(i, "value" + i);
        }

        long count = EntryStream.of(largeMap).filter(e -> e.getKey() % 2 == 0).count();

        assertEquals(500, count);
    }

    @Test
    public void testGroupByEdgeCases() {
        Map<String, List<Integer>> result = EntryStream.of("a", 1).groupBy().toMap();

        assertEquals(1, result.size());
        assertEquals(Arrays.asList(1), result.get("a"));
    }

    @Test
    public void testFlatMapEdgeCases() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 0).flatMapValue(v -> Stream.range(0, v)).toList();

        assertEquals(0, result.size());
    }

    @Test
    public void testDistinctVariants() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 1, "a", 2, "c", 1).distinctByKey().toList();

        assertEquals(3, result.size());
    }

    @Test
    public void testCollectorsIntegration() {
        Map<String, List<Entry<String, Integer>>> grouped = EntryStream.of(testMap)
                .collect(java.util.stream.Collectors.groupingBy(e -> e.getValue() % 2 == 0 ? "even" : "odd"));

        assertTrue(grouped.containsKey("even"));
        assertTrue(grouped.containsKey("odd"));
    }

    @Test
    public void testBiIteratorOperations() {
        BiIterator<String, Integer> iter = EntryStream.of(testMap).biIterator();

        List<Pair<String, Integer>> pairs = new ArrayList<>();
        iter.forEachRemaining((k, v) -> pairs.add(Pair.of(k, v)));

        assertEquals(5, pairs.size());
        assertEquals("one", pairs.get(0).left());
        assertEquals(1, pairs.get(0).right());
    }

    @Test
    public void testZipOperationsEdgeCases() {
        String[] keys = { "a", "b", "c", "d" };
        Integer[] values = { 1, 2 };

        Map<String, Integer> result = EntryStream.zip(keys, values, "default", 0).toMap();

        assertEquals(4, result.size());
        assertEquals(0, result.get("c"));
        assertEquals(0, result.get("d"));
    }

    @Test
    public void testCycledOperations() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 1).cycled(3).toList();

        assertEquals(3, result.size());
        assertEquals("a", result.get(2).getKey());
    }

    @Test
    public void testPercentileOperations() {
        Optional<Map<Percentage, Entry<String, Integer>>> result = EntryStream.of(testMap).percentiles(Comparator.comparing(Entry::getValue));

        assertTrue(result.isPresent());
        Map<Percentage, Entry<String, Integer>> percentiles = result.get();
        assertNotNull(percentiles);
    }
}
