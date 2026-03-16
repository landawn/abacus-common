package com.landawn.abacus.util.stream;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.BiIterator;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.IntFunctions;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.Supplier;

@Tag("2025")
public class EntryStreamTest extends TestBase {

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
    public void testKeysRejectClosedMapBackedStream() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        stream.close();

        Assertions.assertThrows(IllegalStateException.class, stream::keys);
    }

    @Test
    public void testValuesRejectClosedMapBackedStream() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        stream.close();

        Assertions.assertThrows(IllegalStateException.class, stream::values);
    }

    @Test
    public void testEntriesRejectClosedStreamImmediately() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        stream.close();

        Assertions.assertThrows(IllegalStateException.class, stream::entries);
    }

    @Test
    public void testKeysPropagateCloseHandlers() {
        AtomicInteger closeCount = new AtomicInteger();

        EntryStream.of(testMap).onClose(closeCount::incrementAndGet).keys().count();

        assertEquals(1, closeCount.get());
    }

    @Test
    public void testValuesPropagateCloseHandlers() {
        AtomicInteger closeCount = new AtomicInteger();

        EntryStream.of(testMap).onClose(closeCount::incrementAndGet).values().count();

        assertEquals(1, closeCount.get());
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
        Map<Integer, String> inversed = EntryStream.of(testMap).invert().toMap();
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
    public void testOnEach_Consumer() {
        AtomicInteger count = new AtomicInteger(0);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).peek(e -> count.incrementAndGet()).toList();
        assertEquals(5, result.size());
        assertEquals(5, count.get());
    }

    @Test
    public void testOnEach_BiConsumer() {
        AtomicInteger sum = new AtomicInteger(0);
        List<Entry<String, Integer>> result = EntryStream.of(testMap).peek((k, v) -> sum.addAndGet(v)).toList();
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
        assertTrue(EntryStream.of(testMap).hasMatchCountBetween(2, 3, e -> e.getValue() > 3));
        assertFalse(EntryStream.of(testMap).hasMatchCountBetween(5, 10, e -> e.getValue() > 3));
    }

    @Test
    public void testNMatch_BiPredicate() {
        assertTrue(EntryStream.of(testMap).hasMatchCountBetween(2, 3, (k, v) -> v > 3));
        assertFalse(EntryStream.of(testMap).hasMatchCountBetween(5, 10, (k, v) -> v > 3));
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

        List<Entry<String, Integer>> result = EntryStream.of(testMap).intersection(set).toList();
        assertEquals(1, result.size());
    }

    @Test
    public void testDifference_FromBase() {
        Set<Entry<String, Integer>> set = new HashSet<>();
        set.add(new AbstractMap.SimpleEntry<>("two", 2));

        List<Entry<String, Integer>> result = EntryStream.of(testMap).difference(set).toList();
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
        assertDoesNotThrow(() -> {
            EntryStream.of("a", 1, "b", 2).println();
        });
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

    // ==================== debounce tests ====================

    @Test
    public void testDebounce_BasicFunctionality() {
        // Allow 3 elements per 1 second window
        Map<String, Integer> largeMap = new LinkedHashMap<>();
        largeMap.put("a", 1);
        largeMap.put("b", 2);
        largeMap.put("c", 3);
        largeMap.put("d", 4);
        largeMap.put("e", 5);

        List<Entry<String, Integer>> result = EntryStream.of(largeMap).debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();

        // Only first 3 entries should pass through within the window
        assertEquals(3, result.size());
        assertEquals("a", result.get(0).getKey());
        assertEquals("b", result.get(1).getKey());
        assertEquals("c", result.get(2).getKey());
    }

    @Test
    public void testDebounce_AllElementsPassWhenWithinLimit() {
        // Allow 10 elements per window, but only 5 elements in stream
        List<Entry<String, Integer>> result = EntryStream.of(testMap).debounce(10, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();

        // All elements should pass
        assertEquals(5, result.size());
    }

    @Test
    public void testDebounce_EmptyStream() {
        List<Entry<String, Integer>> result = EntryStream.of(emptyMap).debounce(5, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testDebounce_SingleElement() {
        Map<String, Integer> singleMap = new LinkedHashMap<>();
        singleMap.put("only", 1);

        List<Entry<String, Integer>> result = EntryStream.of(singleMap).debounce(1, com.landawn.abacus.util.Duration.ofMillis(100)).toList();

        assertEquals(1, result.size());
        assertEquals("only", result.get(0).getKey());
    }

    @Test
    public void testDebounce_MaxWindowSizeOne() {
        // Only 1 element allowed per window
        List<Entry<String, Integer>> result = EntryStream.of(testMap).debounce(1, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();

        assertEquals(1, result.size());
        assertEquals("one", result.get(0).getKey());
    }

    @Test
    public void testDebounce_ThrowsExceptionForNonPositiveMaxWindowSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            EntryStream.of(testMap).debounce(0, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            EntryStream.of(testMap).debounce(-1, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();
        });
    }

    @Test
    public void testDebounce_ThrowsExceptionForNonPositiveDuration() {
        assertThrows(IllegalArgumentException.class, () -> {
            EntryStream.of(testMap).debounce(5, com.landawn.abacus.util.Duration.ofMillis(0)).toList();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            EntryStream.of(testMap).debounce(5, com.landawn.abacus.util.Duration.ofMillis(-100)).toList();
        });
    }

    @Test
    public void testDebounce_WithLargeMaxWindowSize() {
        Map<String, Integer> largeMap = new LinkedHashMap<>();
        for (int i = 0; i < 100; i++) {
            largeMap.put("key" + i, i);
        }

        List<Entry<String, Integer>> result = EntryStream.of(largeMap).debounce(50, com.landawn.abacus.util.Duration.ofSeconds(10)).toList();

        assertEquals(50, result.size());
    }

    @Test
    public void testDebounce_PreservesOrder() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();

        assertEquals("one", result.get(0).getKey());
        assertEquals("two", result.get(1).getKey());
        assertEquals("three", result.get(2).getKey());
    }

    @Test
    public void testDebounce_ChainedWithOtherOperations() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);
        map.put("f", 6);
        map.put("g", 7);
        map.put("h", 8);
        map.put("i", 9);
        map.put("j", 10);

        List<Entry<String, Integer>> result = EntryStream.of(map)
                .filter((k, v) -> v % 2 == 0) // b=2, d=4, f=6, h=8, j=10
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)) // b=2, d=4, f=6
                .mapValue(v -> v * 10) // b=20, d=40, f=60
                .toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(20), result.get(0).getValue());
        assertEquals(Integer.valueOf(40), result.get(1).getValue());
        assertEquals(Integer.valueOf(60), result.get(2).getValue());
    }

    @Test
    public void testDebounce_WithNullValues() {
        Map<String, Integer> mapWithNull = new LinkedHashMap<>();
        mapWithNull.put("a", 1);
        mapWithNull.put("b", null);
        mapWithNull.put("c", 3);
        mapWithNull.put("d", null);
        mapWithNull.put("e", 5);

        List<Entry<String, Integer>> result = EntryStream.of(mapWithNull).debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).getKey());
        assertEquals("b", result.get(1).getKey());
        assertEquals("c", result.get(2).getKey());
    }

    @Test
    public void testDebounce_ToMap() {
        Map<String, Integer> largeMap = new LinkedHashMap<>();
        largeMap.put("a", 1);
        largeMap.put("b", 2);
        largeMap.put("c", 3);
        largeMap.put("d", 4);
        largeMap.put("e", 5);

        Map<String, Integer> result = EntryStream.of(largeMap).debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)).toMap();

        assertEquals(3, result.size());
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsKey("b"));
        assertTrue(result.containsKey("c"));
    }

    @Test
    public void testFilter() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(1, EntryStream.of(map).filter(entry -> entry.getValue() > 2).count());
    }

    @Test
    public void testMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Map<String, Integer> result = EntryStream.of(map).map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey() + "!", entry.getValue() * 2)).toMap();
        assertEquals(4, (int) result.get("b!"));
        assertEquals(2, (int) result.get("a!"));

    }

    @Test
    public void testFlatMap() {
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("a", Arrays.asList(1, 2));
        map.put("b", Arrays.asList(3, 4));

        List<Integer> result = EntryStream.of(map)
                .flatMap(entry -> EntryStream
                        .of(entry.getValue().stream().map(v -> new AbstractMap.SimpleEntry<>(entry.getKey(), v)).collect(Collectors.toList())))
                .values()
                .toList();

        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testDistinct() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 1);
        map.put("c", 2);

        long count = EntryStream.of(map).distinctByValue().count();
        assertEquals(2, count);

        assertEquals(3, EntryStream.of(map).distinct().count());
    }

    @Test
    public void testPeek() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        AtomicInteger count = new AtomicInteger(0);
        EntryStream.of(map).peek(entry -> count.incrementAndGet()).count();
        assertEquals(2, count.get());
    }

    @Test
    public void testLimit() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(2, EntryStream.of(map).limit(2).count());
    }

    @Test
    public void testReduce() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        int sum = EntryStream.of(map).values().reduce(0, (a, b) -> a + b);

        assertEquals(6, sum);
    }

    @Test
    public void testCollect() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        List<String> keyList = EntryStream.of(map).keys().collect(Collectors.toList());

        assertEquals(Arrays.asList("a", "b"), keyList);
    }

    @Test
    public void testAnyMatch() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertTrue(EntryStream.of(map).anyMatch(entry -> entry.getValue() > 1));
        assertFalse(EntryStream.of(map).anyMatch(entry -> entry.getValue() > 2));
    }

    @Test
    public void testAllMatch() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertTrue(EntryStream.of(map).allMatch(entry -> entry.getValue() > 0));
        assertFalse(EntryStream.of(map).allMatch(entry -> entry.getValue() > 1));
    }

    @Test
    public void testNoneMatch() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertTrue(EntryStream.of(map).noneMatch(entry -> entry.getValue() > 2));
        assertFalse(EntryStream.of(map).noneMatch(entry -> entry.getValue() > 1));
    }

    @Test
    public void testFindFirst() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(1, (int) EntryStream.of(map).first().get().getValue());
    }

    @Test
    public void testFindAny() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertNotNull(EntryStream.of(map).first().get());
    }

    @Test
    public void testMapKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Map<String, Integer> mappedKeyMap = EntryStream.of(map).mapKey(k -> k + "!").toMap();

        assertTrue(mappedKeyMap.containsKey("a!"));
        assertTrue(mappedKeyMap.containsKey("b!"));
    }

    @Test
    public void testMapValue() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Map<String, Integer> mappedValueMap = EntryStream.of(map).mapValue(v -> v * 2).toMap();

        assertEquals(2, (int) mappedValueMap.get("a"));
        assertEquals(4, (int) mappedValueMap.get("b"));
    }

    @Test
    public void testAppend() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);

        EntryStream<String, Integer> s1 = EntryStream.of(map1);
        EntryStream<String, Integer> s2 = EntryStream.of(map2);

        assertEquals(2, s1.append(s2).count());
    }

    @Test
    public void testPrepend() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);

        EntryStream<String, Integer> s1 = EntryStream.of(map1);
        EntryStream<String, Integer> s2 = EntryStream.of(map2);

        List<String> keys = s1.prepend(s2).keys().toList();

        assertEquals(Arrays.asList("b", "a"), keys);
    }

    @Test
    public void testTakeWhile() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        List<Integer> result = EntryStream.of(map).takeWhile(e -> e.getValue() < 3).values().toList();

        assertEquals(Arrays.asList(1, 2), result);
    }

    @Test
    public void testDropWhile() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        List<Integer> result = EntryStream.of(map).dropWhile(e -> e.getValue() < 3).values().toList();
        assertEquals(Arrays.asList(3, 4), result);
    }

    @Test
    public void testJoin() {
        final Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");

        final String result = EntryStream.of(map).join(", ", "=", "[", "]");
        assertEquals("[1=a, 2=b]", result);
    }

    @Test
    public void testZip() {
        final List<String> keys = Arrays.asList("a", "b");
        final List<Integer> values = Arrays.asList(1, 2);

        final Map<String, Integer> map = EntryStream.zip(keys, values).toMap();
        assertEquals(2, map.size());
        assertEquals(1, (int) map.get("a"));
        assertEquals(2, (int) map.get("b"));
    }

    @Test
    public void testMerge() {
        Map<String, Integer> mapA = new HashMap<>();
        mapA.put("a", 1);
        mapA.put("b", 2);
        Map<String, Integer> mapB = new HashMap<>();
        mapB.put("c", 3);
        mapB.put("d", 4);

        final Map<String, Integer> mergedMap = EntryStream.merge(mapA, mapB,
                (e1, e2) -> Comparator.<String> naturalOrder().compare(e1.getKey(), e2.getKey()) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toMap();

        assertEquals(4, mergedMap.size());
    }

    @Test
    public void testFlatmapValue() {
        final Map<String, List<Integer>> map = new HashMap<>();
        map.put("a", Arrays.asList(1, 2));
        map.put("b", Arrays.asList(3, 4));

        final List<Map.Entry<String, Integer>> res = EntryStream.of(map).flatmapValue(v -> v).toList();

        final List<Integer> values = Stream.of(res).map(Map.Entry::getValue).toList();
        final List<String> keys = Stream.of(res).map(Map.Entry::getKey).toList();
        assertEquals(4, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));
        assertTrue(values.contains(4));
        assertEquals(4, keys.size());
    }

    @Test
    public void testMapKeyPartial() {
        final Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        final Map<String, Integer> result = EntryStream.of(map)
                .mapKeyPartial(k -> k.equals("a") ? com.landawn.abacus.util.u.Optional.of("aa") : com.landawn.abacus.util.u.Optional.empty())
                .toMap();
        assertEquals(1, result.size());
        assertEquals(1, result.get("aa").intValue());
    }

    @Test
    public void testMapValuePartial() {
        final Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        final Map<String, Integer> result = EntryStream.of(map)
                .mapValuePartial(v -> v > 1 ? com.landawn.abacus.util.u.Optional.of(v * 2) : com.landawn.abacus.util.u.Optional.empty())
                .toMap();
        assertEquals(2, result.size());
        assertEquals(4, result.get("b").intValue());
        assertEquals(6, result.get("c").intValue());

    }

    @Test
    public void testFlatmapKey() {
        Map<List<String>, Integer> map = new HashMap<>();
        map.put(Arrays.asList("a", "b"), 1);
        map.put(Arrays.asList("c", "d"), 2);

        final Map<String, Integer> result = EntryStream.of(map).flatmapKey(k -> k).toMap(Fn.throwingMerger());

        assertEquals(4, result.size());
        assertEquals(1, result.get("a").intValue());
        assertEquals(1, result.get("b").intValue());
        assertEquals(2, result.get("c").intValue());
        assertEquals(2, result.get("d").intValue());

    }

    @Test
    public void testTakeWhileAndDropWhileWithBiPredicate() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        assertEquals(2, EntryStream.of(map).takeWhile((k, v) -> v < 3).count());
        assertEquals(2, EntryStream.of(map).dropWhile((k, v) -> v < 3).count());
    }

    @Test
    public void testSkipUntilWithBiPredicate() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        assertEquals(2, EntryStream.of(map).skipUntil((k, v) -> v > 2).count());
    }

    @Test
    public void testMapMulti() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        final long count = EntryStream.of(map).<String, Integer> mapMulti((e, c) -> {
            c.accept(e);
            c.accept(new AbstractMap.SimpleEntry<>(e.getKey() + "!", e.getValue() * 10));
        }).count();

        assertEquals(4, count);
    }

    @Test
    public void testMapPartialWithBiFunction() {
        final Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        final Map<String, Integer> result = EntryStream.of(map).mapPartial((k, v) -> {
            if (v > 1) {
                return com.landawn.abacus.util.u.Optional.of(new AbstractMap.SimpleEntry<>(k, v * 2));
            }
            return com.landawn.abacus.util.u.Optional.empty();
        }).toMap();

        assertEquals(2, result.size());
        assertEquals(4, result.get("b").intValue());
        assertEquals(6, result.get("c").intValue());
    }

    @Test
    public void testAppendIfEmpty() {
        final Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        final Map<String, Integer> emptyMap = Collections.emptyMap();

        assertEquals(1, EntryStream.of(map).appendIfEmpty(emptyMap).count());
        assertEquals(1, EntryStream.of(emptyMap).appendIfEmpty(map).count());

    }

    @Test
    public void testsliding() {
        final Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");
        map.put(4, "d");

        final List<List<Map.Entry<Integer, String>>> windows = EntryStream.of(map).sliding(2).toList();

        assertEquals(3, windows.size());
        assertEquals(Arrays.asList(N.newEntry(1, "a"), N.newEntry(2, "b")), windows.get(0));
        assertEquals(Arrays.asList(N.newEntry(2, "b"), N.newEntry(3, "c")), windows.get(1));
        assertEquals(Arrays.asList(N.newEntry(3, "c"), N.newEntry(4, "d")), windows.get(2));
    }

    @Test
    public void testFlatmap() {
        final Map<Integer, List<String>> map = new HashMap<>();
        map.put(1, Arrays.asList("a", "b"));
        map.put(2, Arrays.asList("c", "d"));

        final Map<Integer, String> result = EntryStream.of(map).flatmap(e -> {
            final Map<Integer, String> newMap = new HashMap<>();
            for (final String s : e.getValue()) {
                newMap.put(e.getKey(), s);
            }
            return newMap;
        }).toMap((v1, v2) -> v2);

        assertEquals(2, result.size());
        assertEquals("b", result.get(1));
        assertEquals("d", result.get(2));
    }

    @Test
    public void testMapPartial() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        final long count = EntryStream.of(map)
                .mapPartial(e -> e.getValue() > 1 ? com.landawn.abacus.util.u.Optional.of(e) : com.landawn.abacus.util.u.Optional.empty())
                .count();
        assertEquals(2, count);
    }

    @Test
    public void testFlatMapKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a,b", 1);
        map.put("c,d", 2);

        final Map<String, Integer> result = EntryStream.of(map).flatMapKey(k -> Stream.of(k.split(","))).toMap();
        assertEquals(4, result.size());
        assertEquals(1, (int) result.get("a"));
        assertEquals(2, (int) result.get("d"));
    }

    @Test
    public void testFlatMapValue() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "1,2");
        map.put("b", "3,4");

        final Map<String, Integer> result = EntryStream.of(map).flatMapValue(v -> Stream.of(v.split(",")).map(Integer::parseInt)).toMap((v1, v2) -> v2);

        assertEquals(2, result.size());
        assertEquals(2, (int) result.get("a"));
        assertEquals(4, (int) result.get("b"));
    }

    @Test
    public void testFromStream() {
        java.util.stream.Stream<Map.Entry<String, Integer>> jdkStream = java.util.stream.Stream.of(new AbstractMap.SimpleEntry<>("a", 1));

        assertEquals(1, Stream.from(jdkStream).mapToEntry(Fn.identity()).count());
    }

    @Test
    public void testDropWhileWithBiPredicateAndConsumer() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        final AtomicInteger droppedSum = new AtomicInteger(0);

        final List<Integer> result = EntryStream.of(map).dropWhile(e -> e.getValue() < 3, e -> droppedSum.addAndGet(e.getValue())).values().toList();

        assertEquals(Arrays.asList(3, 4), result);
        assertEquals(3, droppedSum.get());
    }

    @Test
    public void testDistinctWithMergeFunction() {
        final Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 1);
        map.put("a", 3);

        final EntryStream<String, Integer> stream = EntryStream.of(map);

        final Map<String, Integer> result = stream.distinct((e1, e2) -> e1.getValue() > e2.getValue() ? e1 : e2).toMap();

        assertEquals(2, result.get("b").intValue());
        assertEquals(3, result.get("a").intValue());
    }

    @Test
    public void testConcat() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);

        assertEquals(2, EntryStream.concat(map1, map2).count());
    }

    @Test
    public void testOfEntries() {
        Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("a", 1);
        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("b", 2);
        assertEquals(2, EntryStream.of(N.toList(entry1, entry2)).count());
    }

    @Test
    public void testFromIterator() {
        List<Map.Entry<String, Integer>> list = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2));
        assertEquals(2, EntryStream.of(list.iterator()).count());
    }

    @Test
    public void testFromIterable() {
        List<Map.Entry<String, Integer>> list = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2));
        assertEquals(2, EntryStream.of(list).count());
    }

    @Test
    public void testReduceWithIdentity() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Map.Entry<String, Integer> result = EntryStream.of(map)
                .reduce(new AbstractMap.SimpleEntry<>("sum", 0), (acc, e) -> new AbstractMap.SimpleEntry<>(acc.getKey(), acc.getValue() + e.getValue()));

        assertEquals("sum", result.getKey());
        assertEquals(6, (int) result.getValue());
    }

    @Test
    public void testToMultimapWithFactory() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        ListMultimap<String, Integer> multimap = EntryStream.of(map).toMultimap(Suppliers.ofListMultimap());
        assertEquals(Arrays.asList(1), multimap.get("a"));
    }

    @Test
    public void testOfKeysAndValues() {
        List<String> keys = Arrays.asList("a", "b");
        List<Integer> values = Arrays.asList(1, 2);

        Map<String, Integer> map = EntryStream.of(keys, k -> k, v -> values.get(keys.indexOf(v))).toMap();
        assertEquals(1, map.get("a").intValue());
        assertEquals(2, map.get("b").intValue());

        Map<Integer, String> valKeyMap = EntryStream.of(values, v -> v, k -> keys.get(values.indexOf(k))).toMap();
        assertEquals("a", valKeyMap.get(1));
        assertEquals("b", valKeyMap.get(2));
    }

    @Test
    public void testFlatmapToEntry() {
        final List<String> list = N.toList("a", "b");
        final Map<String, Integer> map = Stream.of(list).flatmapToEntry(s -> N.asMap(s, s.hashCode())).toMap();

        assertNotEquals(0, map.size());
        assertEquals("a".hashCode(), map.get("a").intValue());
        assertEquals("b".hashCode(), map.get("b").intValue());

    }

    @Test
    public void testPrependIfEmpty() {
        final Map<String, Integer> map1 = N.asMap("a", 1);
        final Map<String, Integer> emptyMap = Collections.emptyMap();

        assertEquals(1, EntryStream.of(map1).appendIfEmpty(emptyMap).count());
        assertEquals(1, EntryStream.of(emptyMap).appendIfEmpty(map1).count());
        assertEquals(0, EntryStream.of(emptyMap).appendIfEmpty(emptyMap).count());
    }

    @Test
    public void testSplit() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);

        final List<List<Map.Entry<String, Integer>>> result = EntryStream.of(map).split(2).toList();

        assertEquals(3, result.size());
        assertEquals(2, result.get(0).size());
        assertEquals(2, result.get(1).size());
        assertEquals(1, result.get(2).size());
    }

    @Test
    public void testSplitByPredicate() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        final List<List<Map.Entry<String, Integer>>> result = EntryStream.of(map).entries().split(e -> e.getValue() % 2 == 0).toList();

        assertEquals(4, result.size());
        assertEquals(Arrays.asList(N.newEntry("a", 1)), result.get(0));
        assertEquals(Arrays.asList(N.newEntry("b", 2)), result.get(1));
        assertEquals(Arrays.asList(N.newEntry("c", 3)), result.get(2));
        assertEquals(Arrays.asList(N.newEntry("d", 4)), result.get(3));
    }

    @Test
    public void testRotated() {
        final Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");

        final List<Map.Entry<Integer, String>> result = EntryStream.of(map).rotated(1).toList();
        assertEquals(Arrays.asList(N.newEntry(3, "c"), N.newEntry(1, "a"), N.newEntry(2, "b")), result);
    }

    @Test
    public void testShuffled() {
        final Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");
        map.put(4, "d");
        map.put(5, "e");

        final List<Map.Entry<Integer, String>> original = EntryStream.of(map).toList();
        final List<Map.Entry<Integer, String>> shuffled = EntryStream.of(map).shuffled().toList();

        assertEquals(original.size(), shuffled.size());
        assertFalse(original.equals(shuffled));
        assertTrue(original.containsAll(shuffled));
    }

    @Test
    public void testToImmutableMapWithDuplicateKeys() {
        final Map<String, Integer> map = N.asMap("a", 1, "b", 2);
        final Stream<Map.Entry<String, Integer>> streamWithDuplicates = Stream.of(N.newEntry("a", 3)).append(map.entrySet());
        assertThrows(IllegalStateException.class, () -> streamWithDuplicates.mapToEntry(Fn.identity()).toImmutableMap());
    }

    @Test
    public void testGroupToWithFactory() {
        final Map<String, Integer> map = N.asMap("a", 1, "b", 2, "c", 1);
        final Map<Integer, List<Map.Entry<String, Integer>>> result = EntryStream.of(map)
                .entries()
                .groupTo(Map.Entry::getValue, Fn.identity(), LinkedHashMap::new);
        assertTrue(result instanceof LinkedHashMap);
        assertEquals(2, result.get(1).size());
    }

    @Test
    public void testReduceWithIdentityAndBinaryOperator() {
        final Map<String, Integer> map = N.asMap("a", 1, "b", 2, "c", 3);
        final Map.Entry<String, Integer> identity = new AbstractMap.SimpleEntry<>("total", 0);
        final Map.Entry<String, Integer> result = EntryStream.of(map)
                .reduce(identity, (acc, e) -> new AbstractMap.SimpleEntry<>(acc.getKey(), acc.getValue() + e.getValue()));

        assertEquals("total", result.getKey());
        assertEquals(6, result.getValue().intValue());
    }

    @Test
    public void testFirstAndLast() {
        final Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");

        assertEquals(N.newEntry(1, "a"), EntryStream.of(map).first().get());
        assertEquals(N.newEntry(3, "c"), EntryStream.of(map).last().get());
    }

    @Test
    public void testIfEmpty() {
        final AtomicInteger counter = new AtomicInteger(0);
        EntryStream.empty().ifEmpty(counter::incrementAndGet).count();
        assertEquals(1, counter.get());

        EntryStream.of(N.asMap("a", 1)).ifEmpty(counter::incrementAndGet);
        assertEquals(1, counter.get());
    }

    @Test
    public void testPrependIfEmptyWithSupplier() {
        Map<String, Integer> map = N.asMap("a", 1);
        assertEquals(1, EntryStream.<String, Integer> empty().appendIfEmpty(Fn.s(() -> EntryStream.of(map))).count());
        assertEquals(1, EntryStream.of(map).appendIfEmpty(Fn.s(() -> EntryStream.of(N.asMap("b", 2)))).count());
    }

    @Test
    public void testNMatch() {
        Map<String, Integer> map = N.asMap("a", 1, "b", 2, "c", 3, "d", 4);
        assertTrue(EntryStream.of(map).hasMatchCountBetween(2, 2, e -> e.getValue() % 2 == 0));
        assertFalse(EntryStream.of(map).hasMatchCountBetween(3, 3, e -> e.getValue() % 2 == 0));
        assertTrue(EntryStream.of(map).hasMatchCountBetween(1, 3, e -> e.getValue() % 2 == 0));
    }

    @Test
    public void testFindLast() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 2);
        map.put("b", 4);
        map.put("c", 6);
        map.put("d", 8);

        assertEquals(8, EntryStream.of(map).findLast(e -> e.getValue() > 5).get().getValue().intValue());
        assertFalse(EntryStream.of(map).findLast(e -> e.getValue() > 10).isPresent());
    }

    @Test
    public void testSlidingWithIncrement() {
        final Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");
        map.put(4, "d");
        map.put(5, "e");

        final List<List<Map.Entry<Integer, String>>> windows = EntryStream.of(map).sliding(2, 3).toList();
        assertEquals(2, windows.size());
        assertEquals(Arrays.asList(N.newEntry(1, "a"), N.newEntry(2, "b")), windows.get(0));
        assertEquals(Arrays.asList(N.newEntry(4, "d"), N.newEntry(5, "e")), windows.get(1));
    }

    @Test
    public void testTransformB() {
        final Map<String, Integer> map = N.asMap("a", 1, "b", 2);
        final List<String> result = EntryStream.of(map).entries().transformB(s -> s.map(Map.Entry::getKey)).toList();

        assertTrue(result.containsAll(Arrays.asList("a", "b")));
    }

    @Test
    public void testApplyIfNotEmpty() {
        final Map<String, Integer> map = N.asMap("a", 1);
        assertTrue(EntryStream.of(map).applyIfNotEmpty(s -> true).get());
        assertFalse(EntryStream.<String, Integer> empty().applyIfNotEmpty(s -> true).isPresent());
    }

    @Test
    public void testGroupByWithCollector() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 1);
        map.put("d", 2);

        Map<Integer, Long> result = EntryStream.of(map).groupBy(Map.Entry::getValue, Collectors.counting()).toMap();

        assertEquals(2, result.get(1).longValue());
        assertEquals(2, result.get(2).longValue());
    }

    @Test
    public void testStaticZipOf() {
        List<String> keys = Arrays.asList("a", "b");
        List<Integer> values = Arrays.asList(1, 2, 3);
        Map<String, Integer> zipped = EntryStream.zip(keys, values).toMap();
        assertEquals(2, zipped.size());
        assertEquals(1, zipped.get("a").intValue());
        assertEquals(2, zipped.get("b").intValue());

        Map<String, Integer> zippedWithDefault = EntryStream.zip(keys, values, "z", 0).toMap();
        assertEquals(3, zippedWithDefault.size());
        assertEquals(1, zippedWithDefault.get("a").intValue());
        assertEquals(2, zippedWithDefault.get("b").intValue());
        assertEquals(3, zippedWithDefault.get("z").intValue());
    }

    @Test
    public void testFilterWithPredicate() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> filtered = stream.filter(e -> e.getValue() > 3).toMap();
        assertEquals(2, filtered.size());
        assertFalse(filtered.containsKey("one"));
        assertTrue(filtered.containsKey("four"));
        assertTrue(filtered.containsKey("five"));
    }

    @Test
    public void testFilterWithBiPredicate() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> filtered = stream.filter((k, v) -> k.startsWith("t") && v > 1).toMap();
        assertEquals(2, filtered.size());
        assertTrue(filtered.containsKey("two"));
        assertTrue(filtered.containsKey("three"));
    }

    @Test
    public void testFilterWithAction() {
        List<Map.Entry<String, Integer>> dropped = new ArrayList<>();
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> filtered = stream.filter(e -> e.getValue() > 3, dropped::add).toMap();
        assertEquals(2, filtered.size());
        assertEquals(3, dropped.size());
        assertEquals("one", dropped.get(0).getKey());
    }

    @Test
    public void testTakeWhileWithPredicate() {
        Map<String, Integer> orderedMap = new LinkedHashMap<>();
        orderedMap.put("a", 1);
        orderedMap.put("b", 2);
        orderedMap.put("c", 3);
        orderedMap.put("d", 1);

        EntryStream<String, Integer> stream = EntryStream.of(orderedMap);
        List<Map.Entry<String, Integer>> result = stream.takeWhile(e -> e.getValue() <= 2).toList();
        assertEquals(2, result.size());
        assertEquals("a", result.get(0).getKey());
        assertEquals("b", result.get(1).getKey());
    }

    @Test
    public void testTakeWhileWithBiPredicate() {
        Map<String, Integer> orderedMap = new LinkedHashMap<>();
        orderedMap.put("a", 1);
        orderedMap.put("b", 2);
        orderedMap.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(orderedMap);
        List<Map.Entry<String, Integer>> result = stream.takeWhile((k, v) -> v < 3).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testDropWhileWithPredicate() {
        Map<String, Integer> orderedMap = new LinkedHashMap<>();
        orderedMap.put("a", 1);
        orderedMap.put("b", 2);
        orderedMap.put("c", 3);
        orderedMap.put("d", 1);

        EntryStream<String, Integer> stream = EntryStream.of(orderedMap);
        List<Map.Entry<String, Integer>> result = stream.dropWhile(e -> e.getValue() < 3).toList();
        assertEquals(2, result.size());
        assertEquals("c", result.get(0).getKey());
        assertEquals("d", result.get(1).getKey());
    }

    @Test
    public void testDropWhileWithBiPredicate() {
        Map<String, Integer> orderedMap = new LinkedHashMap<>();
        orderedMap.put("a", 1);
        orderedMap.put("b", 2);
        orderedMap.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(orderedMap);
        List<Map.Entry<String, Integer>> result = stream.dropWhile((k, v) -> v < 3).toList();
        assertEquals(1, result.size());
        assertEquals("c", result.get(0).getKey());
    }

    @Test
    public void testDropWhileWithAction() {
        Map<String, Integer> orderedMap = new LinkedHashMap<>();
        orderedMap.put("a", 1);
        orderedMap.put("b", 2);
        orderedMap.put("c", 3);

        List<Map.Entry<String, Integer>> dropped = new ArrayList<>();
        EntryStream<String, Integer> stream = EntryStream.of(orderedMap);
        List<Map.Entry<String, Integer>> result = stream.dropWhile(e -> e.getValue() < 3, dropped::add).toList();
        assertEquals(1, result.size());
        assertEquals(2, dropped.size());
    }

    @Test
    public void testSkipUntilWithPredicate() {
        Map<String, Integer> orderedMap = new LinkedHashMap<>();
        orderedMap.put("a", 1);
        orderedMap.put("b", 2);
        orderedMap.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(orderedMap);
        List<Map.Entry<String, Integer>> result = stream.skipUntil(e -> e.getValue() >= 3).toList();
        assertEquals(1, result.size());
        assertEquals("c", result.get(0).getKey());
    }

    @Test
    public void testMapWithFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, String> mapped = stream.map(e -> new AbstractMap.SimpleImmutableEntry<>(e.getKey().toUpperCase(), String.valueOf(e.getValue()))).toMap();
        assertEquals("1", mapped.get("ONE"));
        assertEquals("2", mapped.get("TWO"));
        assertEquals("3", mapped.get("THREE"));
    }

    @Test
    public void testMapWithKeyValueMappers() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, String> mapped = stream.map(e -> e.getKey().toUpperCase(), e -> "Value: " + e.getValue()).toMap();
        assertEquals("Value: 1", mapped.get("ONE"));
        assertEquals("Value: 2", mapped.get("TWO"));
        assertEquals("Value: 3", mapped.get("THREE"));
    }

    @Test
    public void testMapWithBiFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, String> mapped = stream.map((k, v) -> new AbstractMap.SimpleImmutableEntry<>(k.toUpperCase(), k + ":" + v)).toMap();
        assertEquals("one:1", mapped.get("ONE"));
        assertEquals("two:2", mapped.get("TWO"));
        assertEquals("three:3", mapped.get("THREE"));
    }

    @Test
    public void testMapWithBiFunctions() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<Integer, String> mapped = stream.map((k, v) -> v, (k, v) -> k + " has value " + v).toMap();
        assertEquals("one has value 1", mapped.get(1));
        assertEquals("two has value 2", mapped.get(2));
        assertEquals("three has value 3", mapped.get(3));
    }

    @Test
    public void testMapPartialWithFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> result = stream.mapPartial(
                e -> e.getValue() > 3 ? Optional.of(new AbstractMap.SimpleImmutableEntry<>(e.getKey().toUpperCase(), e.getValue() * 10)) : Optional.empty())
                .toMap();
        assertEquals(2, result.size());
        assertEquals(40, result.get("FOUR"));
        assertEquals(50, result.get("FIVE"));
    }

    @Test
    public void testMapKeyWithBiFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> mapped = stream.mapKey((k, v) -> k + v).toMap();
        assertEquals(1, mapped.get("one1"));
        assertEquals(2, mapped.get("two2"));
        assertEquals(3, mapped.get("three3"));
    }

    @Test
    public void testMapValueWithBiFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, String> mapped = stream.mapValue((k, v) -> k + " has value " + v).toMap();
        assertEquals("one has value 1", mapped.get("one"));
        assertEquals("two has value 2", mapped.get("two"));
        assertEquals("three has value 3", mapped.get("three"));
    }

    @Test
    public void testMapKeyPartialWithBiFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> result = stream.mapKeyPartial((k, v) -> v > 1 ? Optional.of(k.toUpperCase()) : Optional.empty()).toMap();
        assertEquals(4, result.size());
        assertEquals(2, result.get("TWO"));
        assertEquals(3, result.get("THREE"));
    }

    @Test
    public void testMapValuePartialWithBiFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, String> result = stream.mapValuePartial((k, v) -> v > 3 ? Optional.of(k + ":" + v) : Optional.empty()).toMap();
        assertEquals(2, result.size());
        assertEquals("four:4", result.get("four"));
        assertEquals("five:5", result.get("five"));
    }

    @Test
    public void testFlatMapWithFunction() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 2);
        map.put("b", 1);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Map<String, Integer> result = stream.flatMap(e -> {
            Map<String, Integer> expanded = new HashMap<>();
            for (int i = 0; i < e.getValue(); i++) {
                expanded.put(e.getKey() + i, i);
            }
            return EntryStream.of(expanded);
        }).toMap();
        assertEquals(3, result.size());
        assertEquals(0, result.get("a0"));
        assertEquals(1, result.get("a1"));
        assertEquals(0, result.get("b0"));
    }

    @Test
    public void testFlatMapWithBiFunction() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 2);
        map.put("b", 1);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Map<String, Integer> result = stream.flatMap((k, v) -> EntryStream.of(k + "1", v, k + "2", v * 2)).toMap();
        assertEquals(4, result.size());
        assertEquals(2, result.get("a1"));
        assertEquals(4, result.get("a2"));
        assertEquals(1, result.get("b1"));
        assertEquals(2, result.get("b2"));
    }

    @Test
    public void testFlatmapWithFunction() {
        Map<String, Integer> map = new HashMap<>();
        map.put("group1", 2);
        map.put("group2", 1);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Map<String, Integer> result = stream.flatmap(e -> {
            Map<String, Integer> expanded = new HashMap<>();
            for (int i = 1; i <= e.getValue(); i++) {
                expanded.put(e.getKey() + "_item" + i, i);
            }
            return expanded;
        }).toMap();
        assertEquals(3, result.size());
        assertEquals(1, result.get("group1_item1"));
        assertEquals(2, result.get("group1_item2"));
        assertEquals(1, result.get("group2_item1"));
    }

    @Test
    public void testFlatmapWithBiFunction() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 2);
        map.put("b", 1);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Map<String, Integer> result = stream.flatmap((k, v) -> {
            Map<String, Integer> m = new HashMap<>();
            m.put(k + "_x", v);
            m.put(k + "_y", v * 2);
            return m;
        }).toMap();
        assertEquals(4, result.size());
        assertEquals(2, result.get("a_x"));
        assertEquals(4, result.get("a_y"));
        assertEquals(1, result.get("b_x"));
        assertEquals(2, result.get("b_y"));
    }

    @Test
    public void testFlattMapWithFunction() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Map<String, Integer> result = stream.flattMap(e -> Stream.of(new AbstractMap.SimpleImmutableEntry<>(e.getKey() + "1", e.getValue()),
                new AbstractMap.SimpleImmutableEntry<>(e.getKey() + "2", e.getValue() * 2))).toMap();
        assertEquals(2, result.size());
        assertEquals(2, result.get("a1"));
        assertEquals(4, result.get("a2"));
    }

    @Test
    public void testFlattMapWithBiFunction() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Map<String, Integer> result = stream
                .flattMap((k, v) -> Stream.of(new AbstractMap.SimpleImmutableEntry<>(k + "1", v), new AbstractMap.SimpleImmutableEntry<>(k + "2", v * 2)))
                .toMap();
        assertEquals(2, result.size());
        assertEquals(2, result.get("a1"));
        assertEquals(4, result.get("a2"));
    }

    @Test
    public void testFlatMapKeyWithBiFunction() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2);
        Map<String, Integer> result = stream.flatMapKey((k, v) -> Stream.of(k + v, k + "-" + v)).toMap();
        assertEquals(4, result.size());
        assertEquals(1, result.get("a1"));
        assertEquals(1, result.get("a-1"));
        assertEquals(2, result.get("b2"));
        assertEquals(2, result.get("b-2"));
    }

    @Test
    public void testFlatmapKeyWithBiFunction() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2);
        Map<String, Integer> result = stream.flatmapKey((k, v) -> Arrays.asList(k + v, k + "-" + v)).toMap();
        assertEquals(4, result.size());
        assertEquals(1, result.get("a1"));
        assertEquals(1, result.get("a-1"));
        assertEquals(2, result.get("b2"));
        assertEquals(2, result.get("b-2"));
    }

    @Test
    public void testFlatMapValueWithBiFunction() {
        Map<String, String> map = new HashMap<>();
        map.put("prefix", "a,b");
        map.put("suffix", "c,d");

        EntryStream<String, String> stream = EntryStream.of(map);
        ListMultimap<String, String> result = stream.flatMapValue((k, v) -> Stream.of(v.split(",")).map(s -> k + "-" + s)).toMultimap();
        assertEquals(2, result.get("prefix").size());
        assertTrue(result.get("prefix").contains("prefix-a"));
        assertTrue(result.get("prefix").contains("prefix-b"));
        assertEquals(2, result.get("suffix").size());
        assertTrue(result.get("suffix").contains("suffix-c"));
        assertTrue(result.get("suffix").contains("suffix-d"));
    }

    @Test
    public void testFlatmapValueWithBiFunction() {
        Map<String, String> map = new HashMap<>();
        map.put("prefix", "a,b");
        map.put("suffix", "c,d");

        EntryStream<String, String> stream = EntryStream.of(map);
        ListMultimap<String, String> result = stream
                .flatmapValue((k, v) -> Arrays.stream(v.split(",")).map(s -> k + "-" + s).collect(java.util.stream.Collectors.toList()))
                .toMultimap();
        assertEquals(2, result.get("prefix").size());
        assertTrue(result.get("prefix").contains("prefix-a"));
        assertTrue(result.get("prefix").contains("prefix-b"));
        assertEquals(2, result.get("suffix").size());
        assertTrue(result.get("suffix").contains("suffix-c"));
        assertTrue(result.get("suffix").contains("suffix-d"));
    }

    @Test
    public void testGroupBy() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("a2", 3);
        map.put("b2", 4);

        EntryStream<String, Integer> stream = EntryStream.of(map.entrySet());
        Map<String, List<Integer>> grouped = stream.groupBy(e -> e.getKey().substring(0, 1), e -> e.getValue()).toMap();
        assertEquals(2, grouped.size());
        assertEquals(2, grouped.get("a").size());
        assertEquals(2, grouped.get("b").size());
    }

    @Test
    public void testGroupByWithMapFactory() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("a2", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map.entrySet());
        Map<String, List<Integer>> grouped = stream.groupBy(e -> e.getKey().substring(0, 1), e -> e.getValue()).toMap(TreeMap::new);
        assertTrue(grouped instanceof TreeMap);
        assertEquals(2, grouped.size());
    }

    @Test
    public void testGroupByWithKeyValueMappers() {
        Map<String, Integer> map = new HashMap<>();
        map.put("apple", 5);
        map.put("banana", 6);
        map.put("apricot", 7);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Map<String, List<Integer>> grouped = stream.groupBy(e -> e.getKey().substring(0, 1), e -> e.getValue()).toMap();
        assertEquals(2, grouped.size());
        assertEquals(2, grouped.get("a").size());
        assertEquals(1, grouped.get("b").size());
    }

    @Test
    public void testGroupByWithKeyValueMappersAndMapFactory() {
        Map<String, Integer> map = new HashMap<>();
        map.put("apple", 5);
        map.put("banana", 6);
        map.put("apricot", 7);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Map<String, List<Integer>> grouped = stream.groupBy(e -> e.getKey().substring(0, 1), e -> e.getValue()).toMap(TreeMap::new);
        assertTrue(grouped instanceof TreeMap);
        assertEquals(2, grouped.size());
    }

    @Test
    public void testGroupByWithCollectorAndMapFactory() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map.entrySet());
        Map<String, Integer> grouped = stream.groupBy(Collectors.summingInt(e -> e.getValue())).toMap(TreeMap::new);
        assertTrue(grouped instanceof TreeMap);
        assertEquals(1, grouped.get("a"));
        assertEquals(2, grouped.get("b"));
    }

    @Test
    public void testGroupByWithKeyMapperAndCollector() {
        Map<String, Integer> map = new HashMap<>();
        map.put("apple", 5);
        map.put("banana", 6);
        map.put("apricot", 7);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Map<String, Integer> grouped = stream.groupBy(e -> e.getKey().substring(0, 1), Collectors.summingInt(e -> e.getValue())).toMap();
        assertEquals(12, grouped.get("a"));
        assertEquals(6, grouped.get("b"));
    }

    @Test
    public void testGroupByWithKeyMapperCollectorAndMapFactory() {
        Map<String, Integer> map = new HashMap<>();
        map.put("apple", 5);
        map.put("banana", 6);
        map.put("apricot", 7);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Map<String, Integer> grouped = stream.groupBy(e -> e.getKey().substring(0, 1), Collectors.summingInt(e -> e.getValue())).toMap(TreeMap::new);
        assertTrue(grouped instanceof TreeMap);
        assertEquals(12, grouped.get("a"));
        assertEquals(6, grouped.get("b"));
    }

    @Test
    public void testGroupByWithMergeFunction() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("a2", 3);
        map.put("b2", 4);

        EntryStream<String, Integer> stream = EntryStream.of(map.entrySet());
        Map<String, Integer> grouped = stream.groupBy(e -> e.getKey().substring(0, 1), e -> e.getValue(), Integer::sum).toMap();
        assertEquals(4, grouped.get("a"));
        assertEquals(6, grouped.get("b"));
    }

    @Test
    public void testGroupByWithMergeFunctionAndMapFactory() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map.entrySet());
        Map<String, Integer> grouped = stream.groupBy(Integer::sum).toMap(TreeMap::new);
        assertTrue(grouped instanceof TreeMap);
        assertEquals(1, grouped.get("a"));
        assertEquals(2, grouped.get("b"));
    }

    @Test
    public void testGroupByWithKeyValueMappersAndMergeFunction() {
        Map<String, Integer> map = new HashMap<>();
        map.put("apple", 5);
        map.put("banana", 6);
        map.put("apricot", 7);
        map.put("apple2", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map.entrySet());
        Map<String, Integer> grouped = stream.groupBy(e -> e.getKey().substring(0, 1), e -> e.getValue(), Integer::sum).toMap();
        assertEquals(15, grouped.get("a"));
        assertEquals(6, grouped.get("b"));
    }

    @Test
    public void testGroupByWithKeyValueMappersMergeFunctionAndMapFactory() {
        Map<String, Integer> map = new HashMap<>();
        map.put("apple", 5);
        map.put("banana", 6);
        map.put("apricot", 7);
        map.put("appleB", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map.entrySet());
        Map<String, Integer> grouped = stream.groupBy(e -> e.getKey().substring(0, 1), e -> e.getValue(), Integer::sum).toMap(TreeMap::new);
        assertTrue(grouped instanceof TreeMap);
        assertEquals(15, grouped.get("a"));
        assertEquals(6, grouped.get("b"));
    }

    @Test
    public void testCollapseByKeyWithMapperAndCollector() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a1", 1);
        map.put("a2", 2);
        map.put("b1", 3);
        map.put("b2", 4);
        map.put("c1", 5);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Integer> result = stream.collapseByKey((k1, k2) -> k1.charAt(0) == k2.charAt(0), Map.Entry::getValue, Collectors.summingInt(Integer::intValue))
                .toList();
        assertEquals(3, result.size());
        assertEquals(3, result.get(0));
        assertEquals(7, result.get(1));
        assertEquals(5, result.get(2));
    }

    @Test
    public void testCollapseByValueWithMapperAndCollector() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 1);
        map.put("c", 2);
        map.put("d", 2);
        map.put("e", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<String> result = stream.collapseByValue((v1, v2) -> v1.equals(v2), Map.Entry::getKey, Collectors.joining("-")).toList();
        assertEquals(3, result.size());
        assertEquals("a-b", result.get(0));
        assertEquals("c-d", result.get(1));
        assertEquals("e", result.get(2));
    }

    @Test
    public void testSplitWithCollectionSupplier() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<LinkedList<Map.Entry<String, Integer>>> chunks = stream.split(2, IntFunctions.ofLinkedList()).toList();
        assertEquals(3, chunks.size());
        assertTrue(chunks.get(0) instanceof LinkedList);
    }

    @Test
    public void testSlidingWithCollectionSupplier() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<LinkedList<Map.Entry<String, Integer>>> windows = stream.sliding(3, IntFunctions.ofLinkedList()).toList();
        assertEquals(2, windows.size());
        assertTrue(windows.get(0) instanceof LinkedList);
    }

    @Test
    public void testSlidingWithIncrementAndCollectionSupplier() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<LinkedList<Map.Entry<String, Integer>>> windows = stream.sliding(3, 2, IntFunctions.ofLinkedList()).toList();
        assertEquals(2, windows.size());
        assertTrue(windows.get(0) instanceof LinkedList);
    }

    @Test
    public void testSortedWithComparator() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 3);
        map.put("b", 1);
        map.put("c", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> sorted = stream.sorted(Map.Entry.<String, Integer> comparingByValue()).toList();
        assertEquals(3, sorted.size());
        assertEquals(1, sorted.get(0).getValue());
        assertEquals(2, sorted.get(1).getValue());
        assertEquals(3, sorted.get(2).getValue());
    }

    @Test
    public void testReverseSortedWithComparator() {
        Map<String, Integer> map = new HashMap<>();
        map.put("aaa", 1);
        map.put("b", 2);
        map.put("cc", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> sorted = stream.reverseSorted(Comparator.comparing(e -> e.getKey().length())).toList();
        assertEquals(3, sorted.size());
        assertEquals("aaa", sorted.get(0).getKey());
        assertEquals("cc", sorted.get(1).getKey());
        assertEquals("b", sorted.get(2).getKey());
    }

    @Test
    public void testDistinctBy() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("aaa", 1);
        map.put("b", 2);
        map.put("cc", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> distinct = stream.distinctBy(e -> e.getKey().length()).toList();
        assertEquals(3, distinct.size());
    }

    @Test
    public void testDistinctByWithMergeFunction() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 3),
                new AbstractMap.SimpleEntry<>("c", 2));

        EntryStream<String, Integer> stream = EntryStream.of(entries);
        List<Map.Entry<String, Integer>> distinct = stream.distinctBy(Map.Entry::getKey, (e1, e2) -> e1.getValue() > e2.getValue() ? e1 : e2).toList();
        assertEquals(3, distinct.size());
    }

    @Test
    public void testShuffledWithRandom() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Random rnd = new Random(42);
        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> shuffled = stream.shuffled(rnd).toList();
        assertEquals(3, shuffled.size());
        assertTrue(shuffled.stream().anyMatch(e -> e.getKey().equals("a")));
        assertTrue(shuffled.stream().anyMatch(e -> e.getKey().equals("b")));
        assertTrue(shuffled.stream().anyMatch(e -> e.getKey().equals("c")));
    }

    @Test
    public void testCycledWithRounds() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> cycled = stream.cycled(3).toList();
        assertEquals(6, cycled.size());
        assertEquals("a", cycled.get(0).getKey());
        assertEquals("b", cycled.get(1).getKey());
        assertEquals("a", cycled.get(2).getKey());
        assertEquals("b", cycled.get(3).getKey());
        assertEquals("a", cycled.get(4).getKey());
        assertEquals("b", cycled.get(5).getKey());
    }

    @Test
    public void testPrependStream() {
        EntryStream<String, Integer> main = EntryStream.of("a", 1, "b", 2);
        EntryStream<String, Integer> prefix = EntryStream.of("x", 10, "y", 20);

        List<Map.Entry<String, Integer>> result = main.prepend(prefix).toList();
        assertEquals(4, result.size());
        assertEquals("x", result.get(0).getKey());
        assertEquals("y", result.get(1).getKey());
        assertEquals("a", result.get(2).getKey());
        assertEquals("b", result.get(3).getKey());
    }

    @Test
    public void testPrependOptional() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2);
        Optional<Map.Entry<String, Integer>> optional = Optional.of(new AbstractMap.SimpleEntry<>("x", 10));

        List<Map.Entry<String, Integer>> result = stream.prepend(optional).toList();
        assertEquals(3, result.size());
        assertEquals("x", result.get(0).getKey());
        assertEquals("a", result.get(1).getKey());
        assertEquals("b", result.get(2).getKey());
    }

    @Test
    public void testAppendStream() {
        EntryStream<String, Integer> main = EntryStream.of("a", 1, "b", 2);
        EntryStream<String, Integer> suffix = EntryStream.of("x", 10, "y", 20);

        List<Map.Entry<String, Integer>> result = main.append(suffix).toList();
        assertEquals(4, result.size());
        assertEquals("a", result.get(0).getKey());
        assertEquals("b", result.get(1).getKey());
        assertEquals("x", result.get(2).getKey());
        assertEquals("y", result.get(3).getKey());
    }

    @Test
    public void testAppendOptional() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2);
        Optional<Map.Entry<String, Integer>> optional = Optional.of(new AbstractMap.SimpleEntry<>("x", 10));

        List<Map.Entry<String, Integer>> result = stream.append(optional).toList();
        assertEquals(3, result.size());
        assertEquals("a", result.get(0).getKey());
        assertEquals("b", result.get(1).getKey());
        assertEquals("x", result.get(2).getKey());
    }

    @Test
    public void testAppendIfEmptySupplier() {
        Supplier<EntryStream<String, Integer>> defaultSupplier = () -> EntryStream.of("default", 0);

        EntryStream<String, Integer> emptyStream = EntryStream.empty();
        List<Map.Entry<String, Integer>> result1 = emptyStream.appendIfEmpty(defaultSupplier).toList();
        assertEquals(1, result1.size());
        assertEquals("default", result1.get(0).getKey());

        EntryStream<String, Integer> nonEmptyStream = EntryStream.of("a", 1);
        List<Map.Entry<String, Integer>> result2 = nonEmptyStream.appendIfEmpty(defaultSupplier).toList();
        assertEquals(1, result2.size());
        assertEquals("a", result2.get(0).getKey());
    }

    @Test
    public void testSkipWithAction() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        List<Map.Entry<String, Integer>> skipped = new ArrayList<>();
        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> result = stream.skip(2, skipped::add).toList();
        assertEquals(2, result.size());
        assertEquals(2, skipped.size());
        assertEquals("a", skipped.get(0).getKey());
        assertEquals("b", skipped.get(1).getKey());
    }

    @Test
    public void testStep() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> result = stream.step(2).toList();
        assertEquals(3, result.size());
        assertEquals("a", result.get(0).getKey());
        assertEquals("c", result.get(1).getKey());
        assertEquals("e", result.get(2).getKey());
    }

    @Test
    public void testOnEach() {
        List<String> processed = new ArrayList<>();
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> result = stream.peek(e -> processed.add(e.getKey())).toMap();

        assertEquals(testMap.size(), result.size());
        assertEquals(testMap.size(), processed.size());
    }

    @Test
    public void testOnEachBiConsumer() {
        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> result = stream.peek((k, v) -> {
            keys.add(k);
            values.add(v);
        }).toMap();

        assertEquals(testMap.size(), result.size());
        assertEquals(testMap.size(), keys.size());
        assertEquals(testMap.size(), values.size());
    }

    @Test
    public void testForEach() {
        List<String> keys = new ArrayList<>();
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        stream.forEach(e -> keys.add(e.getKey()));

        assertEquals(testMap.size(), keys.size());
        assertTrue(keys.contains("one"));
        assertTrue(keys.contains("two"));
        assertTrue(keys.contains("three"));
    }

    @Test
    public void testForEachBiConsumer() {
        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        stream.forEach((k, v) -> {
            keys.add(k);
            values.add(v);
        });

        assertEquals(testMap.size(), keys.size());
        assertEquals(testMap.size(), values.size());
    }

    @Test
    public void testAnyMatchBiPredicate() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertTrue(stream.anyMatch((k, v) -> k.equals("two") && v == 2));

        EntryStream<String, Integer> stream2 = EntryStream.of(testMap);
        assertFalse(stream2.anyMatch((k, v) -> k.equals("x")));
    }

    @Test
    public void testAllMatchBiPredicate() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertTrue(stream.allMatch((k, v) -> k.length() >= 3));

        EntryStream<String, Integer> stream2 = EntryStream.of(testMap);
        assertFalse(stream2.allMatch((k, v) -> v > 2));
    }

    @Test
    public void testNoneMatchBiPredicate() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertTrue(stream.noneMatch((k, v) -> k.equals("missing")));

        EntryStream<String, Integer> stream2 = EntryStream.of(testMap);
        assertFalse(stream2.noneMatch((k, v) -> k.equals("one")));
    }

    @Test
    public void testNMatchBiPredicate() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        assertTrue(stream.hasMatchCountBetween(1, 2, (k, v) -> v > 2));

        EntryStream<String, Integer> stream2 = EntryStream.of(map);
        assertFalse(stream2.hasMatchCountBetween(3, 3, (k, v) -> v > 2));
    }

    @Test
    public void testFindFirstBiPredicate() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Optional<Map.Entry<String, Integer>> found = stream.findFirst((k, v) -> v >= 2);

        assertTrue(found.isPresent());
        assertEquals("b", found.get().getKey());
    }

    @Test
    public void testFindAnyBiPredicate() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Optional<Map.Entry<String, Integer>> found = stream.findAny((k, v) -> v == 2);

        assertTrue(found.isPresent());
        assertEquals(2, found.get().getValue());
    }

    @Test
    public void testFindLastBiPredicate() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 1);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Optional<Map.Entry<String, Integer>> found = stream.findLast((k, v) -> v == 1);

        assertTrue(found.isPresent());
        assertEquals("c", found.get().getKey());
    }

    @Test
    public void testOnlyOneThrowsException() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertThrows(TooManyElementsException.class, () -> stream.onlyOne());
    }

    @Test
    public void testToList() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        List<Map.Entry<String, Integer>> list = stream.toList();

        assertEquals(testMap.size(), list.size());
        for (Map.Entry<String, Integer> entry : list) {
            assertTrue(testMap.containsKey(entry.getKey()));
            assertEquals(testMap.get(entry.getKey()), entry.getValue());
        }
    }

    @Test
    public void testToSet() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Set<Map.Entry<String, Integer>> set = stream.toSet();

        assertEquals(testMap.size(), set.size());
    }

    @Test
    public void testToCollection() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        LinkedHashSet<Map.Entry<String, Integer>> collection = stream.toCollection(LinkedHashSet::new);

        assertEquals(testMap.size(), collection.size());
        assertTrue(collection instanceof LinkedHashSet);
    }

    @Test
    public void testToMultisetWithSupplier() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Multiset<Map.Entry<String, Integer>> multiset = stream.toMultiset(Multiset::new);

        assertEquals(testMap.size(), multiset.size());
    }

    @Test
    public void testToMapWithMergeFunction() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("a", 3));

        EntryStream<String, Integer> stream = EntryStream.of(entries);
        Map<String, Integer> map = stream.toMap(Integer::sum);

        assertEquals(2, map.size());
        assertEquals(4, map.get("a"));
        assertEquals(2, map.get("b"));
    }

    @Test
    public void testToMapWithMapFactory() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        TreeMap<String, Integer> map = stream.toMap(TreeMap::new);

        assertTrue(map instanceof TreeMap);
        assertEquals(testMap.size(), map.size());
    }

    @Test
    public void testToMapWithMergeFunctionAndMapFactory() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("a", 3));

        EntryStream<String, Integer> stream = EntryStream.of(entries);
        TreeMap<String, Integer> map = stream.toMap(Integer::sum, TreeMap::new);

        assertTrue(map instanceof TreeMap);
        assertEquals(2, map.size());
        assertEquals(4, map.get("a"));
    }

    @Test
    public void testToImmutableMapWithMergeFunction() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("a", 3));

        EntryStream<String, Integer> stream = EntryStream.of(entries);
        ImmutableMap<String, Integer> map = stream.toImmutableMap(Integer::sum);

        assertEquals(2, map.size());
        assertEquals(4, map.get("a"));
    }

    @Test
    public void testToMultimapWithSupplier() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("a", 3));

        EntryStream<String, Integer> stream = EntryStream.of(entries);
        ListMultimap<String, Integer> multimap = stream.toMultimap(Suppliers.ofListMultimap());

        assertEquals(2, multimap.get("a").size());
    }

    @Test
    public void testGroupToWithMapFactory() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("a", 3));

        EntryStream<String, Integer> stream = EntryStream.of(entries);
        TreeMap<String, List<Integer>> grouped = stream.groupTo(TreeMap::new);

        assertTrue(grouped instanceof TreeMap);
        assertEquals(2, grouped.size());
    }

    @Test
    public void testReduceWithoutIdentity() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Optional<Map.Entry<String, Integer>> result = stream.reduce((e1, e2) -> e1.getValue() > e2.getValue() ? e1 : e2);

        assertTrue(result.isPresent());
        assertEquals(5, result.get().getValue());
    }

    @Test
    public void testCollectWithSupplierAccumulatorCombiner() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        List<Map.Entry<String, Integer>> result = stream.collect(ArrayList::new, List::add, List::addAll);

        assertEquals(testMap.size(), result.size());
    }

    @Test
    public void testCollectWithSupplierAccumulator() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        List<Map.Entry<String, Integer>> result = stream.collect(ArrayList::new, List::add);

        assertEquals(testMap.size(), result.size());
    }

    @Test
    public void testCollectWithCollector() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> result = stream.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertEquals(testMap.size(), result.size());
        assertEquals(testMap, result);
    }

    @Test
    public void testJoinWithPrefixSuffix() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        String result = stream.join(", ", "{", "}");

        assertEquals("{a=1, b=2}", result);
    }

    @Test
    public void testJoinWithKeyValueDelimiter() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        String result = stream.join(", ", ":");

        assertEquals("a:1, b:2", result);
    }

    @Test
    public void testJoinWithAllDelimiters() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        String result = stream.join(", ", ":", "{", "}");

        assertEquals("{a:1, b:2}", result);
    }

    @Test
    public void testTransformBDeferred() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        EntryStream<String, String> transformed = stream
                .<String, String> transformB(s -> s.map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), String.valueOf(e.getValue()))), true);

        Map<String, String> result = transformed.toMap();
        assertEquals(testMap.size(), result.size());
    }

    @Test
    public void testinvertToDisposableEntry() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Map<Integer, String> inversed = stream.invertToDisposableEntry().toMap();

        assertEquals(2, inversed.size());
        assertEquals("a", inversed.get(1));
        assertEquals("b", inversed.get(2));
    }

    @Test
    public void testOfSingleEntry() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1);
        Map<String, Integer> map = stream.toMap();

        assertEquals(1, map.size());
        assertEquals(1, map.get("a"));
    }

    @Test
    public void testOfTwoEntries() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2);
        Map<String, Integer> map = stream.toMap();

        assertEquals(2, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
    }

    @Test
    public void testOfThreeEntries() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2, "c", 3);
        Map<String, Integer> map = stream.toMap();

        assertEquals(3, map.size());
    }

    @Test
    public void testOfFourEntries() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4);
        Map<String, Integer> map = stream.toMap();

        assertEquals(4, map.size());
    }

    @Test
    public void testOfFiveEntries() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
        Map<String, Integer> map = stream.toMap();

        assertEquals(5, map.size());
    }

    @Test
    public void testOfSixEntries() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);
        Map<String, Integer> map = stream.toMap();

        assertEquals(6, map.size());
    }

    @Test
    public void testOfSevenEntries() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
        Map<String, Integer> map = stream.toMap();

        assertEquals(7, map.size());
    }

    @Test
    public void testOfMap() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> result = stream.toMap();

        assertEquals(testMap, result);
    }

    @Test
    public void testOfIterator() {
        Iterator<Map.Entry<String, Integer>> iterator = testMap.entrySet().iterator();
        EntryStream<String, Integer> stream = EntryStream.of(iterator);
        Map<String, Integer> result = stream.toMap();

        assertEquals(testMap.size(), result.size());
    }

    @Test
    public void testOfIterable() {
        Set<Map.Entry<String, Integer>> entrySet = testMap.entrySet();
        EntryStream<String, Integer> stream = EntryStream.of(entrySet);
        Map<String, Integer> result = stream.toMap();

        assertEquals(testMap, result);
    }

    @Test
    public void testOfMultimap() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        multimap.put("a", 1);
        multimap.put("a", 2);
        multimap.put("b", 3);

        EntryStream<String, Collection<Integer>> stream = EntryStream.of(multimap);
        Map<String, Collection<Integer>> result = stream.toMap();

        assertEquals(2, result.size());
        assertEquals(2, result.get("a").size());
        assertEquals(1, result.get("b").size());
    }

    @Test
    public void testOfArrayWithKeyMapper() {
        String[] names = { "Alice", "Bob", "Charlie" };
        EntryStream<Integer, String> stream = EntryStream.of(names, String::length);
        Map<Integer, String> result = stream.toMap();

        assertEquals(3, result.size());
    }

    @Test
    public void testOfArrayWithKeyValueMappers() {
        String[] words = { "hello", "world", "java" };
        EntryStream<String, Integer> stream = EntryStream.of(words, String::toUpperCase, String::length);
        Map<String, Integer> result = stream.toMap();

        assertEquals(3, result.size());
        assertEquals(5, result.get("HELLO"));
        assertEquals(5, result.get("WORLD"));
        assertEquals(4, result.get("JAVA"));
    }

    @Test
    public void testOfCollectionWithKeyMapper() {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
        EntryStream<Integer, String> stream = EntryStream.of(names, String::length);
        Map<Integer, String> result = stream.toMap();

        assertEquals(3, result.size());
    }

    @Test
    public void testOfCollectionWithKeyValueMappers() {
        List<String> words = Arrays.asList("hello", "world", "java");
        EntryStream<String, Integer> stream = EntryStream.of(words, String::toUpperCase, String::length);
        Map<String, Integer> result = stream.toMap();

        assertEquals(3, result.size());
    }

    @Test
    public void testOfIteratorWithKeyMapper() {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
        EntryStream<Integer, String> stream = EntryStream.of(names.iterator(), String::length);
        Map<Integer, String> result = stream.toMap();

        assertEquals(3, result.size());
    }

    @Test
    public void testOfIteratorWithKeyValueMappers() {
        List<String> words = Arrays.asList("hello", "world");
        EntryStream<String, Integer> stream = EntryStream.of(words.iterator(), String::toUpperCase, String::length);
        Map<String, Integer> result = stream.toMap();

        assertEquals(2, result.size());
    }

    @Test
    public void testConcatCollection() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);

        List<Map<String, Integer>> maps = Arrays.asList(map1, map2);
        EntryStream<String, Integer> concatenated = EntryStream.concat(maps);
        Map<String, Integer> result = concatenated.toMap();

        assertEquals(2, result.size());
    }

    @Test
    public void testMergeTwoMaps() {
        Map<Integer, String> map1 = new TreeMap<>();
        map1.put(1, "a");
        map1.put(3, "c");

        Map<Integer, String> map2 = new TreeMap<>();
        map2.put(2, "b");
        map2.put(4, "d");

        EntryStream<Integer, String> merged = EntryStream.merge(map1, map2,
                (e1, e2) -> e1.getKey() < e2.getKey() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);

        List<Map.Entry<Integer, String>> result = merged.toList();
        assertEquals(4, result.size());
        assertEquals(1, result.get(0).getKey());
        assertEquals(2, result.get(1).getKey());
        assertEquals(3, result.get(2).getKey());
        assertEquals(4, result.get(3).getKey());
    }

    @Test
    public void testMergeThreeMaps() {
        Map<Integer, String> map1 = new TreeMap<>();
        map1.put(1, "a");

        Map<Integer, String> map2 = new TreeMap<>();
        map2.put(2, "b");

        Map<Integer, String> map3 = new TreeMap<>();
        map3.put(3, "c");

        EntryStream<Integer, String> merged = EntryStream.merge(map1, map2, map3,
                (e1, e2) -> e1.getKey() < e2.getKey() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);

        List<Map.Entry<Integer, String>> result = merged.toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testMergeCollectionOfMaps() {
        Map<Integer, String> map1 = new TreeMap<>();
        map1.put(1, "a");

        Map<Integer, String> map2 = new TreeMap<>();
        map2.put(2, "b");

        List<Map<Integer, String>> maps = Arrays.asList(map1, map2);

        EntryStream<Integer, String> merged = EntryStream.merge(maps, (e1, e2) -> e1.getKey() < e2.getKey() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);

        List<Map.Entry<Integer, String>> result = merged.toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testZipArrays() {
        String[] names = { "Alice", "Bob", "Charlie" };
        Integer[] ages = { 25, 30, 35, 40 };

        EntryStream<String, Integer> zipped = EntryStream.zip(names, ages);
        Map<String, Integer> result = zipped.toMap();

        assertEquals(3, result.size());
        assertEquals(25, result.get("Alice"));
        assertEquals(30, result.get("Bob"));
        assertEquals(35, result.get("Charlie"));
    }

    @Test
    public void testZipArraysWithDefaults() {
        String[] names = { "Alice", "Bob" };
        Integer[] ages = { 25, 30, 35 };

        EntryStream<String, Integer> zipped = EntryStream.zip(names, ages, "Unknown", -1);
        List<Map.Entry<String, Integer>> result = zipped.toList();

        assertEquals(3, result.size());
        assertEquals("Unknown", result.get(2).getKey());
        assertEquals(35, result.get(2).getValue());
    }

    @Test
    public void testZipIterables() {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
        List<Integer> ages = Arrays.asList(25, 30);

        EntryStream<String, Integer> zipped = EntryStream.zip(names, ages);
        Map<String, Integer> result = zipped.toMap();

        assertEquals(2, result.size());
        assertEquals(25, result.get("Alice"));
        assertEquals(30, result.get("Bob"));
    }

    @Test
    public void testZipIterablesWithDefaults() {
        List<String> names = Arrays.asList("Alice", "Bob");
        List<Integer> ages = Arrays.asList(25, 30, 35);

        EntryStream<String, Integer> zipped = EntryStream.zip(names, ages, "Unknown", -1);
        List<Map.Entry<String, Integer>> result = zipped.toList();

        assertEquals(3, result.size());
        assertEquals("Unknown", result.get(2).getKey());
        assertEquals(35, result.get(2).getValue());
    }

    @Test
    public void testZipIterators() {
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
        List<Integer> ages = Arrays.asList(25, 30);

        EntryStream<String, Integer> zipped = EntryStream.zip(names.iterator(), ages.iterator());
        Map<String, Integer> result = zipped.toMap();

        assertEquals(2, result.size());
    }

    @Test
    public void testZipIteratorsWithDefaults() {
        List<String> names = Arrays.asList("Alice");
        List<Integer> ages = Arrays.asList(25, 30);

        EntryStream<String, Integer> zipped = EntryStream.zip(names.iterator(), ages.iterator(), "Unknown", -1);
        List<Map.Entry<String, Integer>> result = zipped.toList();

        assertEquals(2, result.size());
        assertEquals("Unknown", result.get(1).getKey());
        assertEquals(30, result.get(1).getValue());
    }

    @Test
    public void testEmptyStreamOperations() {

        assertEquals(0, EntryStream.empty().count());
        assertFalse(EntryStream.empty().first().isPresent());
        assertFalse(EntryStream.empty().last().isPresent());
        assertFalse(EntryStream.<String, Integer> empty().min(Map.Entry.comparingByKey()).isPresent());
        assertFalse(EntryStream.<String, Integer> empty().max(Map.Entry.comparingByKey()).isPresent());
        assertTrue(EntryStream.empty().toList().isEmpty());
        assertTrue(EntryStream.empty().toMap().isEmpty());
    }

    @Test
    public void testParallelOperations() {
        Map<String, Integer> largeMap = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            largeMap.put("key" + i, i);
        }

        EntryStream<String, Integer> parallel = EntryStream.of(largeMap).parallel();

        long count = parallel.filter((k, v) -> v % 2 == 0).count();
        assertEquals(500, count);

        EntryStream<String, Integer> parallel2 = EntryStream.of(largeMap).parallel();
        int sum = parallel2.entries().mapToInt(Map.Entry::getValue).sum();
        assertEquals(499500, sum);
    }

    @Test
    public void testStreamReuse() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        stream.toList();

        assertThrows(Exception.class, () -> stream.toMap());
    }

    @Test
    public void testComplexChaining() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("apple", 5);
        map.put("banana", 3);
        map.put("cherry", 7);
        map.put("date", 2);
        map.put("elderberry", 9);

        Map<String, String> result = EntryStream.of(map)
                .filter((k, v) -> v > 3)
                .sortedByValue(Comparator.reverseOrder())
                .mapKey(Fn.toUpperCase())
                .mapValue(v -> "Count: " + v)
                .limit(3)
                .toMap();

        assertEquals(3, result.size());
        assertEquals("Count: 9", result.get("ELDERBERRY"));
        assertEquals("Count: 7", result.get("CHERRY"));
        assertEquals("Count: 5", result.get("APPLE"));
    }

    @Test
    public void testDuplicateKeyHandling() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("a", 2),
                new AbstractMap.SimpleEntry<>("a", 3));

        EntryStream<String, Integer> stream = EntryStream.of(entries);
        assertThrows(IllegalStateException.class, () -> stream.toMap());

        EntryStream<String, Integer> stream2 = EntryStream.of(entries);
        Map<String, Integer> merged = stream2.toMap(Integer::sum);
        assertEquals(6, merged.get("a"));
    }

    @Test
    public void testLargeDatasetPerformance() {
        Map<Integer, String> largeMap = new HashMap<>();
        for (int i = 0; i < 10000; i++) {
            largeMap.put(i, "value" + i);
        }

        long start = System.currentTimeMillis();

        Map<String, List<Integer>> grouped = EntryStream.of(largeMap)
                .filter((k, v) -> k % 10 == 0)
                .groupBy(e -> e.getValue().substring(0, 6), e -> e.getKey())
                .toMap();

        long duration = System.currentTimeMillis() - start;

        assertEquals(10, grouped.size());
        assertTrue(duration < 1000);
    }

    @Test
    public void testCustomCollectors() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);

        String result = stream
                .collect(Collector.of(StringBuilder::new, (sb, entry) -> sb.append(entry.getKey()).append(":").append(entry.getValue()).append(";"),
                        (sb1, sb2) -> sb1.append(sb2), StringBuilder::toString));

        assertTrue(result.contains("one:1"));
        assertTrue(result.contains("two:2"));
        assertTrue(result.contains("three:3"));
    }

    @Test
    public void testMapperFunctions() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);

        Map<String, Integer> identity = stream.map(e -> e).toMap();
        assertEquals(testMap, identity);

        EntryStream<String, Integer> stream2 = EntryStream.of("a", 1);
        Map<String, Integer> nullMapped = stream2.mapKey(k -> (String) null).toMap();
        assertEquals(1, nullMapped.get(null));
    }

    @Test
    public void testPredicateEdgeCases() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);

        assertEquals(testMap.size(), stream.filter(e -> true).count());

        EntryStream<String, Integer> stream2 = EntryStream.of(testMap);
        assertEquals(0, stream2.filter(e -> false).count());

        Map<String, Integer> mapWithNull = new HashMap<>();
        mapWithNull.put("a", null);
        EntryStream<String, Integer> stream3 = EntryStream.of(mapWithNull);
        assertEquals(1, stream3.filter(e -> e.getValue() == null).count());
    }

    @Test
    public void testComparatorEdgeCases() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 1);
        map.put("c", 1);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> sorted = stream.sortedByValue(Comparator.naturalOrder()).toList();

        assertEquals(3, sorted.size());
    }

    @Test
    public void testBoundaryConditions() {
        EntryStream<String, Integer> single = EntryStream.of("a", 1);
        assertEquals(1, single.count());

        EntryStream<String, Long> large = EntryStream.of("max", Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, large.first().get().getValue());

        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertThrows(IllegalArgumentException.class, () -> stream.skip(-1));

        EntryStream<String, Integer> stream2 = EntryStream.of(testMap);
        assertEquals(0, stream2.limit(0).count());
    }

    @Test
    public void testThreadSafety() throws InterruptedException {
        Map<String, Integer> concurrentMap = new ConcurrentHashMap<>();
        for (int i = 0; i < 100; i++) {
            concurrentMap.put("key" + i, i);
        }

        List<Thread> threads = new ArrayList<>();
        List<Map<String, Integer>> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(() -> {
                Map<String, Integer> result = EntryStream.of(concurrentMap).filter((k, v) -> v % 2 == 0).toMap();
                results.add(result);
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(10, results.size());
        for (Map<String, Integer> result : results) {
            assertEquals(50, result.size());
        }
    }

    @Test
    public void testMemoryEfficiency() {
        AtomicInteger counter = new AtomicInteger(0);

        Map<Integer, Integer> largeMap = new HashMap<>();
        for (int i = 0; i < 1000000; i++) {
            largeMap.put(i, i);
        }

        List<Map.Entry<Integer, Integer>> firstTen = EntryStream.of(largeMap).peek(e -> counter.incrementAndGet()).limit(10).toList();

        assertEquals(10, firstTen.size());
        assertTrue(counter.get() <= 1000);
    }

    @Test
    public void testErrorPropagation() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        assertThrows(RuntimeException.class, () -> stream.map(e -> {
            if (e.getKey().equals("b")) {
                throw new RuntimeException("Test exception");
            }
            return e;
        }).toList());
    }

    @Test
    public void testResourceManagement() {
        AtomicBoolean resourceClosed = new AtomicBoolean(false);

        try {
            EntryStream.of(testMap).onClose(() -> resourceClosed.set(true)).map(e -> {
                throw new RuntimeException("Force exception");
            }).toList();
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
        }

        assertTrue(resourceClosed.get());
    }

    @Test
    public void testParallelWithExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            long count = EntryStream.of(testMap).parallel(executor).filter(e -> e.getValue() > 2).count();

            assertEquals(3, count);

            List<Entry<String, Integer>> result = EntryStream.of(testMap)
                    .parallel(3, executor)
                    .map(e -> new AbstractMap.SimpleEntry<>(e.getKey().toUpperCase(), e.getValue() * 2))
                    .toList();

            assertEquals(5, result.size());
            assertTrue(result.stream().anyMatch(e -> "ONE".equals(e.getKey()) && e.getValue() == 2));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testOfWithSingleEntry() {
        Map<String, Integer> map7 = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7).toMap();

        assertEquals(7, map7.size());
        assertEquals(1, map7.get("a"));
        assertEquals(7, map7.get("g"));

        Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 100);
        Map<String, Integer> singleMap = EntryStream.of(Collections.singletonList(entry)).toMap();
        assertEquals(1, singleMap.size());
        assertEquals(100, singleMap.get("key"));
    }

    @Test
    public void testZipWithIterators() {
        Iterator<String> nullKeys = null;
        Iterator<Integer> nullValues = null;

        assertEquals(0, EntryStream.zip(nullKeys, Arrays.asList(1, 2, 3).iterator()).count());
        assertEquals(0, EntryStream.zip(Arrays.asList("a", "b").iterator(), nullValues).count());
        assertEquals(0, EntryStream.zip(nullKeys, nullValues).count());

        Iterator<String> keys = Arrays.asList("x", "y").iterator();
        Iterator<Integer> values = Arrays.asList(10, 20, 30, 40).iterator();

        List<Entry<String, Integer>> zipped = EntryStream.zip(keys, values, "default", 99).toList();
        assertEquals(4, zipped.size());
        assertEquals("x", zipped.get(0).getKey());
        assertEquals(10, zipped.get(0).getValue());
        assertEquals("y", zipped.get(1).getKey());
        assertEquals(20, zipped.get(1).getValue());
        assertEquals("default", zipped.get(2).getKey());
        assertEquals(30, zipped.get(2).getValue());
        assertEquals("default", zipped.get(3).getKey());
        assertEquals(40, zipped.get(3).getValue());
    }

    @Test
    public void testZipWithNullIterables() {
        Iterable<String> nullKeys = null;
        Iterable<Integer> nullValues = null;

        assertEquals(0, EntryStream.zip(nullKeys, Arrays.asList(1, 2, 3)).count());
        assertEquals(0, EntryStream.zip(Arrays.asList("a", "b"), nullValues).count());

        assertEquals(0, EntryStream.zip(nullKeys, nullValues, "default", 0).count());
    }

    @Test
    public void testConcatWithEmptyMaps() {
        Map<String, Integer> empty1 = new HashMap<>();
        Map<String, Integer> empty2 = new HashMap<>();
        Map<String, Integer> nonEmpty = new HashMap<>();
        nonEmpty.put("a", 1);

        Map<String, Integer> result1 = EntryStream.concat(empty1, empty2, nonEmpty).toMap();
        assertEquals(1, result1.size());
        assertEquals(1, result1.get("a"));

        Collection<Map<String, Integer>> emptyCollection = Collections.emptyList();
        assertEquals(0, EntryStream.concat(emptyCollection).count());

        Collection<Map<String, Integer>> collectionWithNull = Arrays.asList(nonEmpty, null, empty1);
        assertEquals(1, EntryStream.concat(collectionWithNull).count());
    }

    @Test
    public void testMergeWithEmptyMaps() {
        Map<String, Integer> empty = new HashMap<>();
        Map<String, Integer> nonEmpty = new HashMap<>();
        nonEmpty.put("a", 1);
        nonEmpty.put("b", 2);

        BiFunction<Entry<String, Integer>, Entry<String, Integer>, MergeResult> selector = (e1, e2) -> MergeResult.TAKE_FIRST;

        List<Entry<String, Integer>> merged1 = EntryStream.merge(empty, nonEmpty, selector).toList();
        assertEquals(2, merged1.size());

        List<Entry<String, Integer>> merged2 = EntryStream.merge(nonEmpty, empty, selector).toList();
        assertEquals(2, merged2.size());

        List<Entry<String, Integer>> merged3 = EntryStream.merge(empty, empty, selector).toList();
        assertEquals(0, merged3.size());

        Map<String, Integer> another = new HashMap<>();
        another.put("c", 3);

        List<Entry<String, Integer>> merged4 = EntryStream.merge(empty, nonEmpty, another, selector).toList();
        assertEquals(3, merged4.size());
    }

    @Test
    public void testGroupByWithKeyMapperValueMapper() {
        List<Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("apple", 5), new AbstractMap.SimpleEntry<>("apricot", 7),
                new AbstractMap.SimpleEntry<>("banana", 6), new AbstractMap.SimpleEntry<>("blueberry", 9), new AbstractMap.SimpleEntry<>("blackberry", 10));

        Map<Character, List<Integer>> grouped = EntryStream.of(entries).groupBy(e -> e.getKey().charAt(0), Entry::getValue).toMap();

        assertEquals(2, grouped.size());
        assertEquals(Arrays.asList(5, 7), grouped.get('a'));
        assertEquals(Arrays.asList(6, 9, 10), grouped.get('b'));
    }

    @Test
    public void testGroupByWithKeyMapperValueMapperMergeFunction() {
        Map<Boolean, String> grouped = EntryStream.of(testMap).groupBy(e -> e.getValue() % 2 == 0, Entry::getKey, (s1, s2) -> s1 + "," + s2).toMap();

        assertEquals(2, grouped.size());
        assertTrue(grouped.get(true).contains("two"));
        assertTrue(grouped.get(true).contains("four"));
        assertTrue(grouped.get(false).contains("one"));
        assertTrue(grouped.get(false).contains("three"));
        assertTrue(grouped.get(false).contains("five"));
    }

    @Test
    public void testOfWithIterableOfEntries() {
        Set<Entry<String, Integer>> entrySet = new LinkedHashSet<>();
        entrySet.add(new AbstractMap.SimpleEntry<>("x", 10));
        entrySet.add(new AbstractMap.SimpleEntry<>("y", 20));
        entrySet.add(new AbstractMap.SimpleEntry<>("z", 30));

        Map<String, Integer> result = EntryStream.of(entrySet).toMap();
        assertEquals(3, result.size());
        assertEquals(10, result.get("x"));
        assertEquals(20, result.get("y"));
        assertEquals(30, result.get("z"));
    }

    @Test
    public void testOfWithIteratorOfEntries() {
        List<Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("c", 3));

        Map<String, Integer> result = EntryStream.of(entries.iterator()).toMap();
        assertEquals(3, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
        assertEquals(3, result.get("c"));
    }

    @Test
    public void testComplexParallelOperations() {
        Map<String, Integer> largeMap = new LinkedHashMap<>();
        for (int i = 0; i < 1000; i++) {
            largeMap.put("key" + i, i);
        }

        Map<String, Integer> result = EntryStream.of(largeMap)
                .parallel(4)
                .filter(e -> e.getValue() % 2 == 0)
                .mapValue(v -> v * 2)
                .filterByKey(k -> k.contains("5"))
                .toMap();

        assertTrue(result.size() > 0);
        result.forEach((k, v) -> {
            assertTrue(k.contains("5"));
            assertTrue(v % 4 == 0);
        });
    }

    @Test
    public void testEdgeCasesForSkipLimit() {
        assertThrows(IllegalArgumentException.class, () -> {
            EntryStream.of(testMap).skip(-1).count();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            EntryStream.of(testMap).limit(-1).count();
        });

        assertEquals(0, EntryStream.of(testMap).skip(10).count());

        assertEquals(0, EntryStream.of(testMap).limit(0).count());
    }

    @Test
    public void testStreamClosing() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        stream.close();

        assertThrows(IllegalStateException.class, () -> {
            stream.count();
        });

        AtomicInteger closeCount = new AtomicInteger(0);
        try (EntryStream<String, Integer> s = EntryStream.of(testMap).onClose(() -> closeCount.incrementAndGet()).onClose(() -> closeCount.incrementAndGet())) {
            s.limit(2).toList();
        }
        assertEquals(2, closeCount.get());

        EntryStream<String, Integer> stream2 = EntryStream.of(testMap).onClose(() -> closeCount.incrementAndGet());
        stream2.close();
        assertEquals(3, closeCount.get());
    }

    @Test
    public void testComplexGroupByOperations() {
        Map<String, List<String>> data = new HashMap<>();
        data.put("fruit", Arrays.asList("apple", "apricot", "avocado"));
        data.put("vegetable", Arrays.asList("asparagus", "artichoke"));
        data.put("grain", Arrays.asList("amaranth"));

        Map<String, Map<Character, List<String>>> nested = EntryStream.of(data)
                .flatmapValue(Function.identity())
                .groupBy(Entry::getKey,
                        Collectors.groupingBy(e -> e.getValue().length() > 1 ? e.getValue().charAt(1) : ' ',
                                Collectors.mapping(Entry::getValue, Collectors.toList())))
                .toMap();

        assertEquals(3, nested.size());
        assertTrue(nested.get("fruit").containsKey('p'));
        assertTrue(nested.get("fruit").containsKey('v'));
    }

    @Test
    public void testCombiningMultipleStreams() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 3);
        map2.put("c", 4);

        Map<String, Integer> map3 = new HashMap<>();
        map3.put("c", 5);
        map3.put("d", 6);

        Map<String, Integer> combined = EntryStream.concat(map1, map2, map3).groupBy(Integer::sum).toMap();

        assertEquals(4, combined.size());
        assertEquals(1, combined.get("a"));
        assertEquals(5, combined.get("b"));
        assertEquals(9, combined.get("c"));
        assertEquals(6, combined.get("d"));
    }

    @Test
    public void testPerformanceOptimizations() {

        Map<String, Integer> largeMap = new LinkedHashMap<>();
        for (int i = 0; i < 10000; i++) {
            largeMap.put("key" + i, i);
        }

        long keyCount = EntryStream.of(largeMap).keys().count();
        assertEquals(10000, keyCount);

        long valueSum = EntryStream.of(largeMap).values().mapToLong(Integer::longValue).sum();
        assertEquals(49995000L, valueSum);
    }

    @Test
    public void testChainedTransformations() {
        Map<Integer, List<String>> result = EntryStream.of(testMap)
                .filterByValue(v -> v <= 4)
                .mapKey(Fn.toUpperCase())
                .flatMapValue(v -> Stream.range(1, v + 1))
                .invert()
                .groupTo();

        assertEquals(4, result.size());
        assertTrue(result.get(1).containsAll(Arrays.asList("ONE", "TWO", "THREE", "FOUR")));
        assertTrue(result.get(2).containsAll(Arrays.asList("TWO", "THREE", "FOUR")));
        assertTrue(result.get(3).containsAll(Arrays.asList("THREE", "FOUR")));
        assertTrue(result.get(4).contains("FOUR"));
    }

    @Test
    public void testErrorHandlingInParallel() {
        assertThrows(RuntimeException.class, () -> {
            EntryStream.of(testMap).parallel(2).map(e -> {
                if (e.getValue() == 3) {
                    throw new RuntimeException("Test exception");
                }
                return e;
            }).toList();
        });
    }

    @Test
    public void testLargeDatasetOperations() {
        Map<Integer, Integer> largeMap = new LinkedHashMap<>();
        for (int i = 0; i < 10000; i++) {
            largeMap.put(i, i * 2);
        }

        Map<Integer, List<Integer>> grouped = EntryStream.of(largeMap)
                .filter(e -> e.getKey() % 100 == 0)
                .mapValue(v -> v / 2)
                .flatMapKey(k -> Stream.of(k, k + 1000))
                .groupBy()
                .toMap();

        assertEquals(110, grouped.size());
        assertTrue(grouped.containsKey(0));
        assertTrue(grouped.containsKey(1000));
        assertEquals(Arrays.asList(0), grouped.get(0));
        assertEquals(Arrays.asList(0, 1000), grouped.get(1000));
    }

    @Test
    public void testMemoryEfficientOperations() {

        AtomicInteger processCount = new AtomicInteger(0);

        List<Entry<String, Integer>> limited = EntryStream.of(testMap).peek(e -> processCount.incrementAndGet()).limit(2).toList();

        assertEquals(2, limited.size());
        assertEquals(2, processCount.get());
    }

    @Test
    public void testSpecializedComparators() {
        Map<String, Double> doubleMap = new LinkedHashMap<>();
        doubleMap.put("a", 1.5);
        doubleMap.put("b", Double.NaN);
        doubleMap.put("c", 2.5);
        doubleMap.put("d", Double.POSITIVE_INFINITY);
        doubleMap.put("e", Double.NEGATIVE_INFINITY);

        List<Entry<String, Double>> sorted = EntryStream.of(doubleMap).sortedByValue(Comparator.naturalOrder()).toList();

        assertEquals(Double.NEGATIVE_INFINITY, sorted.get(0).getValue());
        assertEquals(1.5, sorted.get(1).getValue());
        assertEquals(2.5, sorted.get(2).getValue());
        assertEquals(Double.POSITIVE_INFINITY, sorted.get(3).getValue());
        assertTrue(Double.isNaN(sorted.get(4).getValue()));
    }

    @Test
    public void testIntermediateOperationState() {
        EntryStream<String, Integer> base = EntryStream.of(testMap);

        EntryStream<String, Integer> filtered1 = base.filter(e -> e.getValue() > 2);

        assertEquals(3, filtered1.count());
    }

    @Test
    public void testComplexMapMulti() {
        List<Entry<String, String>> result = EntryStream.of("a", 2, "b", 3).<String, String> mapMulti((entry, consumer) -> {
            String key = entry.getKey();
            int value = entry.getValue();

            for (int i = 1; i <= value; i++) {
                consumer.accept(new AbstractMap.SimpleEntry<>(key + i, key.toUpperCase().repeat(i)));
            }
        }).toList();

        assertEquals(5, result.size());
        assertEquals("a1", result.get(0).getKey());
        assertEquals("A", result.get(0).getValue());
        assertEquals("a2", result.get(1).getKey());
        assertEquals("AA", result.get(1).getValue());
        assertEquals("b3", result.get(4).getKey());
        assertEquals("BBB", result.get(4).getValue());
    }

    @Test
    public void testSortedByIntLongDouble() {
        Map<String, Integer> unsortedMap = new LinkedHashMap<>();
        unsortedMap.put("c", 30);
        unsortedMap.put("a", 10);
        unsortedMap.put("b", 20);

        List<Entry<String, Integer>> sortedByInt = EntryStream.of(unsortedMap).sortedByInt(Entry::getValue).toList();
        assertEquals(10, sortedByInt.get(0).getValue());
        assertEquals(20, sortedByInt.get(1).getValue());
        assertEquals(30, sortedByInt.get(2).getValue());

        Map<String, Long> longMap = new LinkedHashMap<>();
        longMap.put("c", 30L);
        longMap.put("a", 10L);
        longMap.put("b", 20L);

        List<Entry<String, Long>> sortedByLong = EntryStream.of(longMap).sortedByLong(Entry::getValue).toList();
        assertEquals(10L, sortedByLong.get(0).getValue());
        assertEquals(20L, sortedByLong.get(1).getValue());
        assertEquals(30L, sortedByLong.get(2).getValue());

        Map<String, Double> doubleMap = new LinkedHashMap<>();
        doubleMap.put("c", 3.5);
        doubleMap.put("a", 1.5);
        doubleMap.put("b", 2.5);

        List<Entry<String, Double>> sortedByDouble = EntryStream.of(doubleMap).sortedByDouble(Entry::getValue).toList();
        assertEquals(1.5, sortedByDouble.get(0).getValue());
        assertEquals(2.5, sortedByDouble.get(1).getValue());
        assertEquals(3.5, sortedByDouble.get(2).getValue());
    }

    @Test
    public void testIsParallelAndSequential() {
        EntryStream<String, Integer> sequential = EntryStream.of(testMap);
        assertFalse(sequential.isParallel());

        EntryStream<String, Integer> parallel = EntryStream.of(testMap).parallel();
        assertTrue(parallel.isParallel());

        EntryStream<String, Integer> backToSeq = parallel.sequential();
        assertFalse(backToSeq.isParallel());
    }

    @Test
    public void testOfWithArraysAndMappers() {
        String[] names = { "Alice", "Bob", "Charlie" };
        Map<Integer, String> lengthToName = EntryStream.of(names, String::length).toMap((v1, v2) -> v1 + "," + v2);

        assertEquals("Alice", lengthToName.get(5));
        assertEquals("Bob", lengthToName.get(3));
        assertEquals("Charlie", lengthToName.get(7));

        Integer[] numbers = { 1, 2, 3, 4, 5 };
        Map<String, Integer> evenOdd = EntryStream.of(numbers, n -> n % 2 == 0 ? "even" : "odd", n -> n * 10).toMap(Integer::sum);

        assertEquals(60, evenOdd.get("even"));
        assertEquals(90, evenOdd.get("odd"));
    }

    @Test
    public void testOfWithIterableAndMappers() {
        List<String> cities = Arrays.asList("New York", "London", "Paris", "Tokyo");
        Map<Character, String> firstLetterToCity = EntryStream.of(cities, s -> s.charAt(0)).toMap((v1, v2) -> v1 + " & " + v2);

        assertEquals("New York", firstLetterToCity.get('N'));
        assertEquals("London", firstLetterToCity.get('L'));

        List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);
        Map<Boolean, Integer> evenOddSum = EntryStream.of(nums, n -> n % 2 == 0, n -> n).toMap(Integer::sum);

        assertEquals(6, evenOddSum.get(true));
        assertEquals(9, evenOddSum.get(false));
    }

    @Test
    public void testOfWithIteratorAndMappers() {
        List<String> words = Arrays.asList("apple", "banana", "cherry");
        Map<Integer, String> lengthToWord = EntryStream.of(words.iterator(), String::length).toMap((v1, v2) -> v1 + "," + v2);

        assertEquals("apple", lengthToWord.get(5));
        assertEquals("banana,cherry", lengthToWord.get(6));

        Iterator<Integer> iter = Arrays.asList(10, 20, 30, 40).iterator();
        Map<String, String> rangeToValue = EntryStream.of(iter, n -> n <= 20 ? "low" : "high", n -> "val" + n).toMap((v1, v2) -> v1 + "," + v2);

        assertEquals("val10,val20", rangeToValue.get("low"));
        assertEquals("val30,val40", rangeToValue.get("high"));
    }

    @Test
    public void testZipWithDefaultValues() {
        String[] keys = { "a", "b" };
        Integer[] values = { 1, 2, 3, 4 };

        Map<String, Integer> zipped = EntryStream.zip(keys, values, "default", 99).toMap(Fn.replacingMerger());
        assertEquals(3, zipped.size());
        assertEquals(1, zipped.get("a"));
        assertEquals(2, zipped.get("b"));
        assertEquals(4, zipped.get("default"));

        List<String> keyList = Arrays.asList("x", "y", "z");
        List<Integer> valueList = Arrays.asList(10);

        List<Entry<String, Integer>> zippedList = EntryStream.zip(keyList, valueList, "none", 0).toList();
        assertEquals(3, zippedList.size());
        assertEquals(10, zippedList.get(0).getValue());
        assertEquals(0, zippedList.get(1).getValue());
        assertEquals(0, zippedList.get(2).getValue());
    }

    @Test
    public void testConcatWithCollection() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("c", 3);
        map2.put("d", 4);

        Map<String, Integer> map3 = new HashMap<>();
        map3.put("e", 5);

        List<Map<String, Integer>> maps = Arrays.asList(map1, map2, map3);
        Map<String, Integer> concatenated = EntryStream.concat(maps).toMap();

        assertEquals(5, concatenated.size());
        assertEquals(1, concatenated.get("a"));
        assertEquals(5, concatenated.get("e"));
    }

    @Test
    public void testMergeMultipleMaps() {
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("a", 1);
        map1.put("d", 4);

        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("b", 2);
        map2.put("e", 5);

        Map<String, Integer> map3 = new LinkedHashMap<>();
        map3.put("c", 3);
        map3.put("f", 6);

        List<Entry<String, Integer>> merged = EntryStream
                .merge(map1, map2, map3, (e1, e2) -> e1.getKey().compareTo(e2.getKey()) < 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toList();

        assertEquals(6, merged.size());
        assertEquals("a", merged.get(0).getKey());
        assertEquals("b", merged.get(1).getKey());
        assertEquals("c", merged.get(2).getKey());

        List<Map<String, Integer>> mapList = Arrays.asList(map1, map2, map3);
        List<Entry<String, Integer>> mergedFromCollection = EntryStream
                .merge(mapList, (e1, e2) -> e1.getValue() < e2.getValue() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toList();

        assertEquals(6, mergedFromCollection.size());
    }

    @Test
    public void testMapMultiWithBiConsumer() {
        List<Entry<String, Integer>> result = EntryStream.of("a", 2, "b", 3).<String, Integer> mapMulti((entry, consumer) -> {
            String key = entry.getKey();
            int value = entry.getValue();
            for (int i = 1; i <= value; i++) {
                consumer.accept(new AbstractMap.SimpleEntry<>(key + i, i));
            }
        }).toList();

        assertEquals(5, result.size());
        assertEquals("a1", result.get(0).getKey());
        assertEquals(1, result.get(0).getValue());
        assertEquals("b3", result.get(4).getKey());
        assertEquals(3, result.get(4).getValue());
    }

    @Test
    public void testGroupByWithDownstream() {
        List<Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("a", 3), new AbstractMap.SimpleEntry<>("b", 4), new AbstractMap.SimpleEntry<>("a", 5));

        Map<String, Integer> summed = EntryStream.of(entries).groupBy(Collectors.summingInt(Entry::getValue)).toMap();

        assertEquals(9, summed.get("a"));
        assertEquals(6, summed.get("b"));

        Map<String, String> concatenated = EntryStream.of(entries)
                .groupBy(Collectors.mapping(Entry::getValue, Collectors.mapping(Object::toString, Collectors.joining(","))))
                .toMap();

        assertEquals("1,3,5", concatenated.get("a"));
        assertEquals("2,4", concatenated.get("b"));
    }

    @Test
    public void testGroupByWithKeyMapperAndDownstream() {
        Map<String, Integer> data = new LinkedHashMap<>();
        data.put("apple", 5);
        data.put("apricot", 7);
        data.put("banana", 6);
        data.put("berry", 5);

        Map<Character, Long> counts = EntryStream.of(data).groupBy(e -> e.getKey().charAt(0), Collectors.counting()).toMap();

        assertEquals(2L, counts.get('a'));
        assertEquals(2L, counts.get('b'));
    }

    @Test
    public void testSlidingAndSplitWithCollectionSupplier() {
        List<LinkedList<Entry<String, Integer>>> windows = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4).sliding(2, IntFunctions.ofLinkedList()).toList();

        assertEquals(3, windows.size());
        assertTrue(windows.get(0) instanceof LinkedList);
        assertEquals(2, windows.get(0).size());

        List<ArrayList<Entry<String, Integer>>> chunks = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5).split(2, ArrayList::new).toList();

        assertEquals(3, chunks.size());
        assertTrue(chunks.get(0) instanceof ArrayList);
        assertEquals(2, chunks.get(0).size());
        assertEquals(1, chunks.get(2).size());
    }

    @Test
    public void testCollapseByKeyAndValueWithMapperAndCollector() {
        List<String> collapsed = EntryStream.of("a", 1, "a", 2, "b", 3, "b", 4, "c", 5)
                .collapseByKey((k1, k2) -> k1.equals(k2), Entry::getValue, Collectors.mapping(Object::toString, Collectors.joining(",")))
                .toList();

        assertEquals(3, collapsed.size());
        assertEquals("1,2", collapsed.get(0));
        assertEquals("3,4", collapsed.get(1));
        assertEquals("5", collapsed.get(2));

        List<String> collapsedByValue = EntryStream.of("a", 1, "b", 1, "c", 2, "d", 2)
                .collapseByValue((v1, v2) -> v1.equals(v2), Entry::getKey, Collectors.joining("-"))
                .toList();

        assertEquals(2, collapsedByValue.size());
        assertEquals("a-b", collapsedByValue.get(0));
        assertEquals("c-d", collapsedByValue.get(1));
    }

    @Test
    public void testDelayAndRateLimited() {
        long startTime = System.currentTimeMillis();
        List<Entry<String, Integer>> delayed = EntryStream.of("a", 1, "b", 2).delay(Duration.ofMillis(10)).toList();
        long duration = System.currentTimeMillis() - startTime;

        assertEquals(2, delayed.size());
        assertTrue(duration >= 10);

        RateLimiter rateLimiter = RateLimiter.create(100);
        List<Entry<String, Integer>> limited = EntryStream.of(testMap).rateLimited(rateLimiter).toList();

        assertEquals(5, limited.size());
    }

    @Test
    public void testSortedAndReverseSorted() {
        assertThrows(UnsupportedOperationException.class, () -> EntryStream.of("b", 2, "a", 1, "c", 3).sorted().toList());
    }

    @Test
    public void testToMultimapWithMapFactory() {
        Multimap<String, Integer, Set<Integer>> setMultimap = EntryStream.of("a", 1, "b", 2, "a", 1, "a", 3, "b", 2).toMultimap(Suppliers.ofSetMultimap());

        assertEquals(2, setMultimap.get("a").size());
        assertEquals(1, setMultimap.get("b").size());
    }

    @Test
    public void testOf() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertEquals(5, stream.count());

        EntryStream<String, Integer> stream2 = EntryStream.of("a", 1, "b", 2, "c", 3);
        assertEquals(3, stream2.count());

        EntryStream<String, Integer> emptyStream = EntryStream.empty();
        assertEquals(0, emptyStream.count());

        Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 100);
        assertEquals(1, EntryStream.ofNullable(entry).count());
        assertEquals(0, EntryStream.ofNullable(null).count());
    }

    @Test
    public void testMinMax() {
        Optional<Entry<String, Integer>> minByKey = EntryStream.of(testMap).minByKey(Comparator.naturalOrder());
        assertTrue(minByKey.isPresent());
        assertEquals("five", minByKey.get().getKey());

        Optional<Entry<String, Integer>> maxByKey = EntryStream.of(testMap).maxByKey(Comparator.naturalOrder());
        assertTrue(maxByKey.isPresent());
        assertEquals("two", maxByKey.get().getKey());

        Optional<Entry<String, Integer>> minByValue = EntryStream.of(testMap).minByValue(Comparator.naturalOrder());
        assertTrue(minByValue.isPresent());
        assertEquals(1, minByValue.get().getValue());

        Optional<Entry<String, Integer>> maxByValue = EntryStream.of(testMap).maxByValue(Comparator.naturalOrder());
        assertTrue(maxByValue.isPresent());
        assertEquals(5, maxByValue.get().getValue());
    }

    @Test
    public void testAnyAllNoneMatch() {
        assertTrue(EntryStream.of(testMap).anyMatch(e -> e.getValue() > 4));
        assertTrue(EntryStream.of(testMap).anyMatch((k, v) -> k.equals("three") && v == 3));
        assertFalse(EntryStream.of(testMap).anyMatch(e -> e.getValue() > 10));

        assertTrue(EntryStream.of(testMap).allMatch(e -> e.getValue() > 0));
        assertTrue(EntryStream.of(testMap).allMatch((k, v) -> k.length() >= 3));
        assertFalse(EntryStream.of(testMap).allMatch(e -> e.getValue() > 3));

        assertTrue(EntryStream.of(testMap).noneMatch(e -> e.getValue() > 10));
        assertTrue(EntryStream.of(testMap).noneMatch((k, v) -> k.isEmpty()));
        assertFalse(EntryStream.of(testMap).noneMatch(e -> e.getValue() == 3));
    }

    @Test
    public void testFindFirstAnyLast() {
        Optional<Entry<String, Integer>> first = EntryStream.of(testMap).findFirst(e -> e.getValue() > 3);
        assertTrue(first.isPresent());
        assertEquals("four", first.get().getKey());

        Optional<Entry<String, Integer>> any = EntryStream.of(testMap).findAny((k, v) -> v % 2 == 0);
        assertTrue(any.isPresent());
        assertTrue(any.get().getValue() % 2 == 0);

        Optional<Entry<String, Integer>> last = EntryStream.of(testMap).findLast(e -> e.getValue() < 4);
        assertTrue(last.isPresent());
        assertEquals("three", last.get().getKey());
    }

    @Test
    public void testAppendPrepend() {
        List<Entry<String, Integer>> appended = EntryStream.of("a", 1).append(EntryStream.of("b", 2, "c", 3)).toList();
        assertEquals(3, appended.size());
        assertEquals("c", appended.get(2).getKey());

        List<Entry<String, Integer>> prepended = EntryStream.of("c", 3).prepend(EntryStream.of("a", 1, "b", 2)).toList();
        assertEquals(3, prepended.size());
        assertEquals("a", prepended.get(0).getKey());
    }

    @Test
    public void testSelectByKeyValue() {
        EntryStream<Integer, String> selected = EntryStream.<Object, String> of(1, "one", "two", "2", 3, "three").selectByKey(Integer.class);

        Map<Integer, String> result = selected.toMap();
        assertEquals(2, result.size());
        assertEquals("one", result.get(1));
        assertEquals("three", result.get(3));

        EntryStream<String, Integer> selectedByValue = EntryStream.<String, Object> of("a", 1, "b", "two", "c", 3).selectByValue(Integer.class);

        assertEquals(2, selectedByValue.count());
    }

    @Test
    public void testTakeWhileDropWhile() {
        List<Entry<String, Integer>> taken = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 1, "e", 2).takeWhile(e -> e.getValue() <= 2).toList();

        assertEquals(2, taken.size());
        assertEquals("a", taken.get(0).getKey());
        assertEquals("b", taken.get(1).getKey());

        List<Entry<String, Integer>> dropped = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4).dropWhile((k, v) -> v < 3).toList();

        assertEquals(2, dropped.size());
        assertEquals("c", dropped.get(0).getKey());
    }

    @Test
    public void testMapKeyValuePartial() {
        Map<String, Integer> mappedKeys = EntryStream.of(testMap).mapKeyPartial(k -> k.length() > 3 ? Optional.of(k.toUpperCase()) : Optional.empty()).toMap();

        assertEquals(3, mappedKeys.size());
        assertTrue(mappedKeys.containsKey("THREE"));
        assertTrue(mappedKeys.containsKey("FOUR"));
        assertTrue(mappedKeys.containsKey("FIVE"));

        Map<String, Integer> mappedValues = EntryStream.of(testMap).mapValuePartial(v -> v % 2 == 0 ? Optional.of(v * 100) : Optional.empty()).toMap();

        assertEquals(2, mappedValues.size());
        assertEquals(200, mappedValues.get("two"));
        assertEquals(400, mappedValues.get("four"));
    }

    @Test
    public void testIntersectionDifferenceSymmetricDifference() {
        List<Entry<String, Integer>> list1 = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("c", 3));

        List<Entry<String, Integer>> list2 = Arrays.asList(new AbstractMap.SimpleEntry<>("b", 2), new AbstractMap.SimpleEntry<>("c", 3),
                new AbstractMap.SimpleEntry<>("d", 4));

        List<Entry<String, Integer>> intersection = EntryStream.of(list1).intersection(list2).toList();
        assertEquals(2, intersection.size());

        List<Entry<String, Integer>> difference = EntryStream.of(list1).difference(list2).toList();
        assertEquals(1, difference.size());
        assertEquals("a", difference.get(0).getKey());

        List<Entry<String, Integer>> symDiff = EntryStream.of(list1).symmetricDifference(list2).toList();
        assertEquals(2, symDiff.size());
    }

    @Test
    public void testToMapThenApplyAccept() {
        int sum = EntryStream.of("a", 1, "b", 2, "c", 3).toMapThenApply(map -> map.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(6, sum);

        AtomicInteger result = new AtomicInteger(0);
        EntryStream.of("a", 1, "b", 2, "c", 3).toMapThenAccept(map -> result.set(map.size()));
        assertEquals(3, result.get());
    }

    @Test
    public void testGroupToThenApplyAccept() {
        int totalGroups = EntryStream.of("a", 1, "b", 2, "a", 3).groupToThenApply(Map::size);
        assertEquals(2, totalGroups);

        AtomicInteger maxGroupSize = new AtomicInteger(0);
        EntryStream.of("a", 1, "b", 2, "a", 3, "a", 4).groupToThenAccept(map -> {
            map.values().forEach(list -> maxGroupSize.set(Math.max(maxGroupSize.get(), list.size())));
        });
        assertEquals(3, maxGroupSize.get());
    }

    @Test
    public void testCollectThenApplyAccept() {
        String result = EntryStream.of("a", 1, "b", 2)
                .collectThenApply(Collectors.toMap(Entry::getKey, Entry::getValue), map -> map.keySet().stream().collect(Collectors.joining(",")));
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));

        AtomicReference<Map<String, Integer>> mapRef = new AtomicReference<>();
        EntryStream.of("a", 1, "b", 2).collectThenAccept(Collectors.toMap(Entry::getKey, Entry::getValue), mapRef::set);
        assertEquals(2, mapRef.get().size());
    }

}
