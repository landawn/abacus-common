package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.BiIterator;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.IntFunctions;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;

public class EntryStream103Test extends TestBase {

    private Map<String, Integer> testMap;
    private Map<String, Integer> emptyMap;

    @BeforeEach
    public void setUp() {
        testMap = new HashMap<>();
        testMap.put("a", 1);
        testMap.put("b", 2);
        testMap.put("c", 3);
        emptyMap = new HashMap<>();
    }

    @Test
    public void testKeys() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        List<String> keys = stream.keys().toList();
        assertEquals(3, keys.size());
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains("b"));
        assertTrue(keys.contains("c"));
    }

    @Test
    public void testValues() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        List<Integer> values = stream.values().toList();
        assertEquals(3, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));
    }

    @Test
    public void testEntries() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        List<Map.Entry<String, Integer>> entries = stream.entries().toList();
        assertEquals(3, entries.size());
    }

    @Test
    public void testInversed() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<Integer, String> inversed = stream.inversed().toMap();
        assertEquals("a", inversed.get(1));
        assertEquals("b", inversed.get(2));
        assertEquals("c", inversed.get(3));
    }

    @Test
    public void testSelectByKey() {
        Map<Object, String> mixedMap = new HashMap<>();
        mixedMap.put("string", "value1");
        mixedMap.put(123, "value2");
        mixedMap.put(456, "value3");

        EntryStream<Object, String> stream = EntryStream.of(mixedMap);
        Map<Integer, String> integerKeys = stream.selectByKey(Integer.class).toMap();
        assertEquals(2, integerKeys.size());
        assertTrue(integerKeys.containsKey(123));
        assertTrue(integerKeys.containsKey(456));
    }

    @Test
    public void testSelectByValue() {
        Map<String, Object> mixedMap = new HashMap<>();
        mixedMap.put("a", "string");
        mixedMap.put("b", 123);
        mixedMap.put("c", 456);

        EntryStream<String, Object> stream = EntryStream.of(mixedMap);
        Map<String, Integer> integerValues = stream.selectByValue(Integer.class).toMap();
        assertEquals(2, integerValues.size());
        assertEquals(123, integerValues.get("b"));
        assertEquals(456, integerValues.get("c"));
    }

    @Test
    public void testFilterWithPredicate() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> filtered = stream.filter(e -> e.getValue() > 1).toMap();
        assertEquals(2, filtered.size());
        assertFalse(filtered.containsKey("a"));
        assertTrue(filtered.containsKey("b"));
        assertTrue(filtered.containsKey("c"));
    }

    @Test
    public void testFilterWithBiPredicate() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> filtered = stream.filter((k, v) -> v > 1 && k.compareTo("b") >= 0).toMap();
        assertEquals(2, filtered.size());
        assertTrue(filtered.containsKey("b"));
        assertTrue(filtered.containsKey("c"));
    }

    @Test
    public void testFilterWithAction() {
        List<Map.Entry<String, Integer>> dropped = new ArrayList<>();
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> filtered = stream.filter(e -> e.getValue() > 1, dropped::add).toMap();
        assertEquals(2, filtered.size());
        assertEquals(1, dropped.size());
        assertEquals("a", dropped.get(0).getKey());
    }

    @Test
    public void testFilterByKey() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> filtered = stream.filterByKey(k -> k.compareTo("b") >= 0).toMap();
        assertEquals(2, filtered.size());
        assertTrue(filtered.containsKey("b"));
        assertTrue(filtered.containsKey("c"));
    }

    @Test
    public void testFilterByValue() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> filtered = stream.filterByValue(v -> v >= 2).toMap();
        assertEquals(2, filtered.size());
        assertTrue(filtered.containsKey("b"));
        assertTrue(filtered.containsKey("c"));
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
    public void testSkipUntilWithBiPredicate() {
        Map<String, Integer> orderedMap = new LinkedHashMap<>();
        orderedMap.put("a", 1);
        orderedMap.put("b", 2);
        orderedMap.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(orderedMap);
        List<Map.Entry<String, Integer>> result = stream.skipUntil((k, v) -> k.equals("c")).toList();
        assertEquals(1, result.size());
        assertEquals("c", result.get(0).getKey());
    }

    @Test
    public void testMapWithFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, String> mapped = stream.map(e -> new AbstractMap.SimpleImmutableEntry<>(e.getKey().toUpperCase(), String.valueOf(e.getValue()))).toMap();
        assertEquals("1", mapped.get("A"));
        assertEquals("2", mapped.get("B"));
        assertEquals("3", mapped.get("C"));
    }

    @Test
    public void testMapWithKeyValueMappers() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, String> mapped = stream.map(e -> e.getKey().toUpperCase(), e -> "Value: " + e.getValue()).toMap();
        assertEquals("Value: 1", mapped.get("A"));
        assertEquals("Value: 2", mapped.get("B"));
        assertEquals("Value: 3", mapped.get("C"));
    }

    @Test
    public void testMapWithBiFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, String> mapped = stream.map((k, v) -> new AbstractMap.SimpleImmutableEntry<>(k.toUpperCase(), k + ":" + v)).toMap();
        assertEquals("a:1", mapped.get("A"));
        assertEquals("b:2", mapped.get("B"));
        assertEquals("c:3", mapped.get("C"));
    }

    @Test
    public void testMapWithBiFunctions() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<Integer, String> mapped = stream.map((k, v) -> k.length() + v, (k, v) -> k + " has value " + v).toMap();
        assertEquals("a has value 1", mapped.get(2));
        assertEquals("b has value 2", mapped.get(3));
        assertEquals("c has value 3", mapped.get(4));
    }

    @Test
    public void testMapMulti() {
        EntryStream<String, Integer> stream = EntryStream.of("a", 2, "b", 3);
        Map<String, Integer> result = stream.<String, Integer> mapMulti((entry, consumer) -> {
            for (int i = 0; i < entry.getValue(); i++) {
                consumer.accept(N.newEntry(entry.getKey() + i, i));
            }
        }).toMap();
        assertEquals(5, result.size());
        assertEquals(0, result.get("a0"));
        assertEquals(1, result.get("a1"));
        assertEquals(0, result.get("b0"));
        assertEquals(1, result.get("b1"));
        assertEquals(2, result.get("b2"));
    }

    @Test
    public void testMapPartialWithFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> result = stream.mapPartial(
                e -> e.getValue() > 1 ? Optional.of(new AbstractMap.SimpleImmutableEntry<>(e.getKey().toUpperCase(), e.getValue() * 10)) : Optional.empty())
                .toMap();
        assertEquals(2, result.size());
        assertEquals(20, result.get("B"));
        assertEquals(30, result.get("C"));
    }

    @Test
    public void testMapPartialWithBiFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> result = stream
                .mapPartial((k, v) -> v > 1 ? Optional.of(new AbstractMap.SimpleImmutableEntry<>(k.toUpperCase(), v * 10)) : Optional.empty())
                .toMap();
        assertEquals(2, result.size());
        assertEquals(20, result.get("B"));
        assertEquals(30, result.get("C"));
    }

    @Test
    public void testMapKey() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> mapped = stream.mapKey(Fn.toUpperCase()).toMap();
        assertEquals(1, mapped.get("A"));
        assertEquals(2, mapped.get("B"));
        assertEquals(3, mapped.get("C"));
    }

    @Test
    public void testMapKeyWithBiFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> mapped = stream.mapKey((k, v) -> k + v).toMap();
        assertEquals(1, mapped.get("a1"));
        assertEquals(2, mapped.get("b2"));
        assertEquals(3, mapped.get("c3"));
    }

    @Test
    public void testMapValue() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> mapped = stream.mapValue(v -> v * v).toMap();
        assertEquals(1, mapped.get("a"));
        assertEquals(4, mapped.get("b"));
        assertEquals(9, mapped.get("c"));
    }

    @Test
    public void testMapValueWithBiFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, String> mapped = stream.mapValue((k, v) -> k + " has value " + v).toMap();
        assertEquals("a has value 1", mapped.get("a"));
        assertEquals("b has value 2", mapped.get("b"));
        assertEquals("c has value 3", mapped.get("c"));
    }

    @Test
    public void testMapKeyPartial() {
        Map<String, Integer> map = new HashMap<>();
        map.put("1", 10);
        map.put("two", 20);
        map.put("3", 30);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Map<Integer, Integer> result = stream.mapKeyPartial(k -> {
            try {
                return Optional.of(Integer.parseInt(k));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).toMap();
        assertEquals(2, result.size());
        assertEquals(10, result.get(1));
        assertEquals(30, result.get(3));
    }

    @Test
    public void testMapKeyPartialWithBiFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> result = stream.mapKeyPartial((k, v) -> v > 1 ? Optional.of(k.toUpperCase()) : Optional.empty()).toMap();
        assertEquals(2, result.size());
        assertEquals(2, result.get("B"));
        assertEquals(3, result.get("C"));
    }

    @Test
    public void testMapValuePartial() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "1");
        map.put("b", "two");
        map.put("c", "3");

        EntryStream<String, String> stream = EntryStream.of(map);
        Map<String, Integer> result = stream.mapValuePartial(v -> {
            try {
                return Optional.of(Integer.parseInt(v));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).toMap();
        assertEquals(2, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(3, result.get("c"));
    }

    @Test
    public void testMapValuePartialWithBiFunction() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, String> result = stream.mapValuePartial((k, v) -> k.equals("b") ? Optional.empty() : Optional.of(k + ":" + v)).toMap();
        assertEquals(2, result.size());
        assertEquals("a:1", result.get("a"));
        assertEquals("c:3", result.get("c"));
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
    public void testFlatMapKey() {
        EntryStream<String, Integer> stream = EntryStream.of("word", 1, "hello", 2);
        Map<String, Integer> result = stream.flatMapKey(k -> Stream.of(k.toUpperCase(), k.toLowerCase())).toMap();
        assertEquals(4, result.size());
        assertEquals(1, result.get("WORD"));
        assertEquals(1, result.get("word"));
        assertEquals(2, result.get("HELLO"));
        assertEquals(2, result.get("hello"));
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
    public void testFlatmapKey() {
        EntryStream<String, Integer> stream = EntryStream.of("word", 1, "hello", 2);
        Map<String, Integer> result = stream.flatmapKey(k -> Arrays.asList(k.toUpperCase(), k.toLowerCase())).toMap();
        assertEquals(4, result.size());
        assertEquals(1, result.get("WORD"));
        assertEquals(1, result.get("word"));
        assertEquals(2, result.get("HELLO"));
        assertEquals(2, result.get("hello"));
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
    public void testFlatMapValue() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "1,2");
        map.put("b", "3,4");

        EntryStream<String, String> stream = EntryStream.of(map);
        ListMultimap<String, String> result = stream.flatMapValue(v -> Stream.of(v.split(","))).toMultimap();
        assertEquals(2, result.get("a").size());
        assertTrue(result.get("a").contains("1"));
        assertTrue(result.get("a").contains("2"));
        assertEquals(2, result.get("b").size());
        assertTrue(result.get("b").contains("3"));
        assertTrue(result.get("b").contains("4"));
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
    public void testFlatmapValue() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "1,2");
        map.put("b", "3,4");

        EntryStream<String, String> stream = EntryStream.of(map);
        ListMultimap<String, String> result = stream.flatmapValue(v -> Arrays.asList(v.split(","))).toMultimap();
        assertEquals(2, result.get("a").size());
        assertTrue(result.get("a").contains("1"));
        assertTrue(result.get("a").contains("2"));
        assertEquals(2, result.get("b").size());
        assertTrue(result.get("b").contains("3"));
        assertTrue(result.get("b").contains("4"));
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
    public void testGroupByWithCollector() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("a2", 3);
        map.put("b2", 4);

        EntryStream<String, Integer> stream = EntryStream.of(map.entrySet());
        Map<String, Integer> grouped = stream.groupBy(it -> it.getKey().substring(0, 1), Collectors.summingInt(e -> e.getValue())).toMap();
        assertEquals(4, grouped.get("a"));
        assertEquals(6, grouped.get("b"));
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
    public void testCollapseByKey() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a1", 1);
        map.put("a2", 2);
        map.put("b1", 3);
        map.put("b2", 4);
        map.put("c1", 5);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<List<Integer>> result = stream.collapseByKey((k1, k2) -> k1.charAt(0) == k2.charAt(0)).toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4), result.get(1));
        assertEquals(Arrays.asList(5), result.get(2));
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
    public void testCollapseByValue() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 1);
        map.put("c", 2);
        map.put("d", 2);
        map.put("e", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<List<String>> result = stream.collapseByValue((v1, v2) -> v1.equals(v2)).toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("a", "b"), result.get(0));
        assertEquals(Arrays.asList("c", "d"), result.get(1));
        assertEquals(Arrays.asList("e"), result.get(2));
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
    public void testSplit() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<List<Map.Entry<String, Integer>>> chunks = stream.split(2).toList();
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        assertEquals(1, chunks.get(2).size());
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
    public void testSliding() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<List<Map.Entry<String, Integer>>> windows = stream.sliding(3).toList();
        assertEquals(2, windows.size());
        assertEquals(3, windows.get(0).size());
        assertEquals(3, windows.get(1).size());
        assertEquals("a", windows.get(0).get(0).getKey());
        assertEquals("b", windows.get(1).get(0).getKey());
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
    public void testSlidingWithIncrement() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<List<Map.Entry<String, Integer>>> windows = stream.sliding(3, 2).toList();
        assertEquals(2, windows.size());
        assertEquals(3, windows.get(0).size());
        assertEquals(3, windows.get(1).size());
        assertEquals("a", windows.get(0).get(0).getKey());
        assertEquals("c", windows.get(1).get(0).getKey());
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
    public void testIntersection() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Collection<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("b", 2), new AbstractMap.SimpleEntry<>("c", 3),
                new AbstractMap.SimpleEntry<>("d", 4));

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Set<Map.Entry<String, Integer>> result = stream.intersection(entries).toSet();
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(e -> e.getKey().equals("b") && e.getValue() == 2));
        assertTrue(result.stream().anyMatch(e -> e.getKey().equals("c") && e.getValue() == 3));
    }

    @Test
    public void testDifference() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Collection<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("b", 2), new AbstractMap.SimpleEntry<>("d", 4));

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Set<Map.Entry<String, Integer>> result = stream.difference(entries).toSet();
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(e -> e.getKey().equals("a") && e.getValue() == 1));
        assertTrue(result.stream().anyMatch(e -> e.getKey().equals("c") && e.getValue() == 3));
    }

    @Test
    public void testSymmetricDifference() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Collection<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("b", 2), new AbstractMap.SimpleEntry<>("c", 3),
                new AbstractMap.SimpleEntry<>("d", 4));

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Set<Map.Entry<String, Integer>> result = stream.symmetricDifference(entries).toSet();
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(e -> e.getKey().equals("a") && e.getValue() == 1));
        assertTrue(result.stream().anyMatch(e -> e.getKey().equals("d") && e.getValue() == 4));
    }

    @Test
    public void testSorted() {
        Map<String, Integer> map = new HashMap<>();
        map.put("c", 3);
        map.put("a", 1);
        map.put("b", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> sorted = stream.sorted().toList();
        assertEquals(3, sorted.size());
        assertEquals("a", sorted.get(0).getKey());
        assertEquals("b", sorted.get(1).getKey());
        assertEquals("c", sorted.get(2).getKey());
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
    public void testSortedByKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("c", 1);
        map.put("a", 2);
        map.put("b", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> sorted = stream.sortedByKey(String.CASE_INSENSITIVE_ORDER).toList();
        assertEquals(3, sorted.size());
        assertEquals("a", sorted.get(0).getKey());
        assertEquals("b", sorted.get(1).getKey());
        assertEquals("c", sorted.get(2).getKey());
    }

    @Test
    public void testSortedByValue() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 3);
        map.put("b", 1);
        map.put("c", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> sorted = stream.sortedByValue(Comparator.naturalOrder()).toList();
        assertEquals(3, sorted.size());
        assertEquals(1, sorted.get(0).getValue());
        assertEquals(2, sorted.get(1).getValue());
        assertEquals(3, sorted.get(2).getValue());
    }

    @Test
    public void testSortedBy() {
        Map<String, Integer> map = new HashMap<>();
        map.put("aaa", 1);
        map.put("b", 2);
        map.put("cc", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> sorted = stream.sortedBy(e -> e.getKey().length()).toList();
        assertEquals(3, sorted.size());
        assertEquals("b", sorted.get(0).getKey());
        assertEquals("cc", sorted.get(1).getKey());
        assertEquals("aaa", sorted.get(2).getKey());
    }

    @Test
    public void testSortedByInt() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> sorted = stream.sortedByInt(e -> e.getKey().charAt(0) + e.getValue()).toList();
        assertEquals(3, sorted.size());
        assertEquals("a", sorted.get(0).getKey());
        assertEquals("b", sorted.get(1).getKey());
        assertEquals("c", sorted.get(2).getKey());
    }

    @Test
    public void testSortedByLong() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> sorted = stream.sortedByLong(e -> (long) e.getValue() * 1000).toList();
        assertEquals(3, sorted.size());
        assertEquals(1, sorted.get(0).getValue());
        assertEquals(2, sorted.get(1).getValue());
        assertEquals(3, sorted.get(2).getValue());
    }

    @Test
    public void testSortedByDouble() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 3);
        map.put("b", 1);
        map.put("c", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> sorted = stream.sortedByDouble(e -> e.getValue() * 0.5).toList();
        assertEquals(3, sorted.size());
        assertEquals(1, sorted.get(0).getValue());
        assertEquals(2, sorted.get(1).getValue());
        assertEquals(3, sorted.get(2).getValue());
    }

    @Test
    public void testReverseSorted() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> sorted = stream.reverseSorted().toList();
        assertEquals(3, sorted.size());
        assertEquals("c", sorted.get(0).getKey());
        assertEquals("b", sorted.get(1).getKey());
        assertEquals("a", sorted.get(2).getKey());
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
    public void testReverseSortedBy() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> sorted = stream.reverseSortedBy(Map.Entry::getValue).toList();
        assertEquals(3, sorted.size());
        assertEquals(3, sorted.get(0).getValue());
        assertEquals(2, sorted.get(1).getValue());
        assertEquals(1, sorted.get(2).getValue());
    }

    @Test
    public void testIndexed() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Indexed<Map.Entry<String, Integer>>> indexed = stream.indexed().toList();
        assertEquals(3, indexed.size());
        assertEquals(0, indexed.get(0).index());
        assertEquals("a", indexed.get(0).value().getKey());
        assertEquals(1, indexed.get(1).index());
        assertEquals("b", indexed.get(1).value().getKey());
        assertEquals(2, indexed.get(2).index());
        assertEquals("c", indexed.get(2).value().getKey());
    }

    @Test
    public void testDistinct() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("a", 1));

        EntryStream<String, Integer> stream = EntryStream.of(entries);
        List<Map.Entry<String, Integer>> distinct = stream.distinct().toList();
        assertEquals(2, distinct.size());
        assertEquals("a", distinct.get(0).getKey());
        assertEquals("b", distinct.get(1).getKey());
    }

    @Test
    public void testDistinctWithMergeFunction() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("a", 3),
                new AbstractMap.SimpleEntry<>("b", 2));

        EntryStream<String, Integer> stream = EntryStream.of(entries);
        List<Map.Entry<String, Integer>> distinct = stream.distinct((e1, e2) -> e1.getValue() > e2.getValue() ? e1 : e2).toList();
        assertEquals(3, distinct.size());
    }

    @Test
    public void testDistinctByKey() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("a", 3));

        EntryStream<String, Integer> stream = EntryStream.of(entries);
        List<Map.Entry<String, Integer>> distinct = stream.distinctByKey().toList();
        assertEquals(2, distinct.size());
        assertEquals("a", distinct.get(0).getKey());
        assertEquals(1, distinct.get(0).getValue());
        assertEquals("b", distinct.get(1).getKey());
    }

    @Test
    public void testDistinctByValue() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("c", 1));

        EntryStream<String, Integer> stream = EntryStream.of(entries);
        List<Map.Entry<String, Integer>> distinct = stream.distinctByValue().toList();
        assertEquals(2, distinct.size());
        assertEquals("a", distinct.get(0).getKey());
        assertEquals("b", distinct.get(1).getKey());
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
    public void testRotated() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> rotated = stream.rotated(2).toList();
        assertEquals(4, rotated.size());
        assertEquals("c", rotated.get(0).getKey());
        assertEquals("d", rotated.get(1).getKey());
        assertEquals("a", rotated.get(2).getKey());
        assertEquals("b", rotated.get(3).getKey());
    }

    @Test
    public void testShuffled() {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            map.put(String.valueOf(i), i);
        }

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> shuffled = stream.shuffled().toList();
        assertEquals(10, shuffled.size());
        // Should contain all entries
        for (int i = 0; i < 10; i++) {
            final int val = i;
            assertTrue(shuffled.stream().anyMatch(e -> e.getValue() == val));
        }
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
    public void testReversed() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> reversed = stream.reversed().toList();
        assertEquals(3, reversed.size());
        assertEquals("c", reversed.get(0).getKey());
        assertEquals("b", reversed.get(1).getKey());
        assertEquals("a", reversed.get(2).getKey());
    }

    @Test
    public void testCycled() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> cycled = stream.cycled().limit(6).toList();
        assertEquals(6, cycled.size());
        assertEquals("a", cycled.get(0).getKey());
        assertEquals("b", cycled.get(1).getKey());
        assertEquals("a", cycled.get(2).getKey());
        assertEquals("b", cycled.get(3).getKey());
        assertEquals("a", cycled.get(4).getKey());
        assertEquals("b", cycled.get(5).getKey());
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
    public void testPrepend() {
        Map<String, Integer> main = new LinkedHashMap<>();
        main.put("a", 1);
        main.put("b", 2);

        Map<String, Integer> prefix = new LinkedHashMap<>();
        prefix.put("x", 10);
        prefix.put("y", 20);

        EntryStream<String, Integer> stream = EntryStream.of(main);
        List<Map.Entry<String, Integer>> result = stream.prepend(prefix).toList();
        assertEquals(4, result.size());
        assertEquals("x", result.get(0).getKey());
        assertEquals("y", result.get(1).getKey());
        assertEquals("a", result.get(2).getKey());
        assertEquals("b", result.get(3).getKey());
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
    public void testAppend() {
        Map<String, Integer> main = new LinkedHashMap<>();
        main.put("a", 1);
        main.put("b", 2);

        Map<String, Integer> suffix = new LinkedHashMap<>();
        suffix.put("x", 10);
        suffix.put("y", 20);

        EntryStream<String, Integer> stream = EntryStream.of(main);
        List<Map.Entry<String, Integer>> result = stream.append(suffix).toList();
        assertEquals(4, result.size());
        assertEquals("a", result.get(0).getKey());
        assertEquals("b", result.get(1).getKey());
        assertEquals("x", result.get(2).getKey());
        assertEquals("y", result.get(3).getKey());
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
    public void testAppendIfEmpty() {
        Map<String, Integer> defaults = new LinkedHashMap<>();
        defaults.put("default", 0);

        // Empty stream
        EntryStream<String, Integer> emptyStream = EntryStream.empty();
        List<Map.Entry<String, Integer>> result1 = emptyStream.appendIfEmpty(defaults).toList();
        assertEquals(1, result1.size());
        assertEquals("default", result1.get(0).getKey());

        // Non-empty stream
        EntryStream<String, Integer> nonEmptyStream = EntryStream.of("a", 1);
        List<Map.Entry<String, Integer>> result2 = nonEmptyStream.appendIfEmpty(defaults).toList();
        assertEquals(1, result2.size());
        assertEquals("a", result2.get(0).getKey());
    }

    @Test
    public void testAppendIfEmptySupplier() {
        Supplier<EntryStream<String, Integer>> defaultSupplier = () -> EntryStream.of("default", 0);

        // Empty stream
        EntryStream<String, Integer> emptyStream = EntryStream.empty();
        List<Map.Entry<String, Integer>> result1 = emptyStream.appendIfEmpty(defaultSupplier).toList();
        assertEquals(1, result1.size());
        assertEquals("default", result1.get(0).getKey());

        // Non-empty stream
        EntryStream<String, Integer> nonEmptyStream = EntryStream.of("a", 1);
        List<Map.Entry<String, Integer>> result2 = nonEmptyStream.appendIfEmpty(defaultSupplier).toList();
        assertEquals(1, result2.size());
        assertEquals("a", result2.get(0).getKey());
    }

    @Test
    public void testIfEmpty() {
        AtomicInteger counter = new AtomicInteger(0);

        // Empty stream
        EntryStream<String, Integer> emptyStream = EntryStream.empty();
        emptyStream.ifEmpty(() -> counter.incrementAndGet()).toList();
        assertEquals(1, counter.get());

        // Non-empty stream
        EntryStream<String, Integer> nonEmptyStream = EntryStream.of(testMap);
        nonEmptyStream.ifEmpty(() -> counter.incrementAndGet()).toList();
        assertEquals(1, counter.get()); // Should not increment
    }

    @Test
    public void testSkip() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> result = stream.skip(2).toList();
        assertEquals(2, result.size());
        assertEquals("c", result.get(0).getKey());
        assertEquals("d", result.get(1).getKey());
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
    public void testLimit() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> result = stream.limit(2).toList();
        assertEquals(2, result.size());
        assertEquals("a", result.get(0).getKey());
        assertEquals("b", result.get(1).getKey());
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
    public void testRateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(10.0); // 10 permits per second
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> result = stream.rateLimited(rateLimiter).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testDelay() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        long start = System.currentTimeMillis();
        List<Map.Entry<String, Integer>> result = stream.delay(Duration.ofMillis(10)).toList();
        long duration = System.currentTimeMillis() - start;

        assertEquals(2, result.size());
        assertTrue(duration >= 10); // At least 10ms delay
    }

    @Test
    public void testPeek() {
        List<String> peeked = new ArrayList<>();
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> result = stream.peek(e -> peeked.add(e.getKey())).toMap();

        assertEquals(3, result.size());
        assertEquals(3, peeked.size());
        assertTrue(peeked.contains("a"));
        assertTrue(peeked.contains("b"));
        assertTrue(peeked.contains("c"));
    }

    @Test
    public void testPeekBiConsumer() {
        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> result = stream.peek((k, v) -> {
            keys.add(k);
            values.add(v);
        }).toMap();

        assertEquals(3, result.size());
        assertEquals(3, keys.size());
        assertEquals(3, values.size());
    }

    @Test
    public void testOnEach() {
        List<String> processed = new ArrayList<>();
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> result = stream.onEach(e -> processed.add(e.getKey())).toMap();

        assertEquals(3, result.size());
        assertEquals(3, processed.size());
    }

    @Test
    public void testOnEachBiConsumer() {
        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> result = stream.onEach((k, v) -> {
            keys.add(k);
            values.add(v);
        }).toMap();

        assertEquals(3, result.size());
        assertEquals(3, keys.size());
        assertEquals(3, values.size());
    }

    @Test
    public void testForEach() {
        List<String> keys = new ArrayList<>();
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        stream.forEach(e -> keys.add(e.getKey()));

        assertEquals(3, keys.size());
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains("b"));
        assertTrue(keys.contains("c"));
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

        assertEquals(3, keys.size());
        assertEquals(3, values.size());
    }

    @Test
    public void testForEachIndexed() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        List<Integer> indices = new ArrayList<>();
        List<String> keys = new ArrayList<>();

        EntryStream<String, Integer> stream = EntryStream.of(map);
        stream.forEachIndexed((index, entry) -> {
            indices.add(index);
            keys.add(entry.getKey());
        });

        assertEquals(Arrays.asList(0, 1, 2), indices);
        assertEquals(Arrays.asList("a", "b", "c"), keys);
    }

    @Test
    public void testMin() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Optional<Map.Entry<String, Integer>> min = stream.min((e1, e2) -> Integer.compare(e1.getKey().length(), e2.getKey().length()));

        assertTrue(min.isPresent());
        assertEquals(1, min.get().getKey().length());
    }

    @Test
    public void testMinByKey() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Optional<Map.Entry<String, Integer>> min = stream.minByKey(Comparator.naturalOrder());

        assertTrue(min.isPresent());
        assertEquals("a", min.get().getKey());
    }

    @Test
    public void testMinByValue() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 3);
        map.put("b", 1);
        map.put("c", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Optional<Map.Entry<String, Integer>> min = stream.minByValue(Comparator.naturalOrder());

        assertTrue(min.isPresent());
        assertEquals(1, min.get().getValue());
    }

    @Test
    public void testMinBy() {
        Map<String, Integer> map = new HashMap<>();
        map.put("aaa", 1);
        map.put("b", 2);
        map.put("cc", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Optional<Map.Entry<String, Integer>> min = stream.minBy(e -> e.getKey().length() + e.getValue());

        assertTrue(min.isPresent());
        assertEquals("b", min.get().getKey());
    }

    @Test
    public void testMax() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Optional<Map.Entry<String, Integer>> max = stream.max((e1, e2) -> Integer.compare(e1.getValue(), e2.getValue()));

        assertTrue(max.isPresent());
        assertEquals(3, max.get().getValue());
    }

    @Test
    public void testMaxByKey() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Optional<Map.Entry<String, Integer>> max = stream.maxByKey(Comparator.naturalOrder());

        assertTrue(max.isPresent());
        assertEquals("c", max.get().getKey());
    }

    @Test
    public void testMaxByValue() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Optional<Map.Entry<String, Integer>> max = stream.maxByValue(Comparator.naturalOrder());

        assertTrue(max.isPresent());
        assertEquals(3, max.get().getValue());
    }

    @Test
    public void testMaxBy() {
        Map<String, Integer> map = new HashMap<>();
        map.put("aaa", 1);
        map.put("b", 2);
        map.put("cc", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Optional<Map.Entry<String, Integer>> max = stream.maxBy(e -> e.getKey().length() + e.getValue());

        assertTrue(max.isPresent());
        assertEquals("cc", max.get().getKey());
    }

    @Test
    public void testAnyMatch() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertTrue(stream.anyMatch(e -> e.getValue() > 2));

        EntryStream<String, Integer> stream2 = EntryStream.of(testMap);
        assertFalse(stream2.anyMatch(e -> e.getValue() > 10));
    }

    @Test
    public void testAnyMatchBiPredicate() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertTrue(stream.anyMatch((k, v) -> k.equals("b") && v == 2));

        EntryStream<String, Integer> stream2 = EntryStream.of(testMap);
        assertFalse(stream2.anyMatch((k, v) -> k.equals("x")));
    }

    @Test
    public void testAllMatch() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertTrue(stream.allMatch(e -> e.getValue() > 0));

        EntryStream<String, Integer> stream2 = EntryStream.of(testMap);
        assertFalse(stream2.allMatch(e -> e.getValue() > 2));
    }

    @Test
    public void testAllMatchBiPredicate() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertTrue(stream.allMatch((k, v) -> k.length() == 1));

        EntryStream<String, Integer> stream2 = EntryStream.of(testMap);
        assertFalse(stream2.allMatch((k, v) -> v > 2));
    }

    @Test
    public void testNoneMatch() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertTrue(stream.noneMatch(e -> e.getValue() < 0));

        EntryStream<String, Integer> stream2 = EntryStream.of(testMap);
        assertFalse(stream2.noneMatch(e -> e.getValue() > 2));
    }

    @Test
    public void testNoneMatchBiPredicate() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertTrue(stream.noneMatch((k, v) -> k.equals("x")));

        EntryStream<String, Integer> stream2 = EntryStream.of(testMap);
        assertFalse(stream2.noneMatch((k, v) -> k.equals("a")));
    }

    @Test
    public void testNMatch() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        assertTrue(stream.nMatch(2, 3, e -> e.getValue() > 3));

        EntryStream<String, Integer> stream2 = EntryStream.of(map);
        assertFalse(stream2.nMatch(4, 5, e -> e.getValue() > 3));
    }

    @Test
    public void testNMatchBiPredicate() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        assertTrue(stream.nMatch(1, 2, (k, v) -> v > 2));

        EntryStream<String, Integer> stream2 = EntryStream.of(map);
        assertFalse(stream2.nMatch(3, 3, (k, v) -> v > 2));
    }

    @Test
    public void testFindFirst() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Optional<Map.Entry<String, Integer>> found = stream.findFirst(e -> e.getValue() > 2);

        assertTrue(found.isPresent());
        assertEquals(3, found.get().getValue());
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
    public void testFindAny() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Optional<Map.Entry<String, Integer>> found = stream.findAny(e -> e.getValue() > 2);

        assertTrue(found.isPresent());
        assertTrue(found.get().getValue() > 2);
    }

    @Test
    public void testFindAnyBiPredicate() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Optional<Map.Entry<String, Integer>> found = stream.findAny((k, v) -> v == 2);

        assertTrue(found.isPresent());
        assertEquals(2, found.get().getValue());
    }

    @Test
    public void testFindLast() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 1);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Optional<Map.Entry<String, Integer>> found = stream.findLast(e -> e.getValue() == 1);

        assertTrue(found.isPresent());
        assertEquals("d", found.get().getKey());
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
    public void testFirst() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Optional<Map.Entry<String, Integer>> first = stream.first();

        assertTrue(first.isPresent());
        assertNotNull(first.get().getKey());
        assertNotNull(first.get().getValue());
    }

    @Test
    public void testLast() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Optional<Map.Entry<String, Integer>> last = stream.last();

        assertTrue(last.isPresent());
        assertEquals("c", last.get().getKey());
    }

    @Test
    public void testElementAt() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Optional<Map.Entry<String, Integer>> element = stream.elementAt(1);

        assertTrue(element.isPresent());
        assertEquals("b", element.get().getKey());
    }

    @Test
    public void testOnlyOne() {
        Map<String, Integer> singleMap = new HashMap<>();
        singleMap.put("a", 1);

        EntryStream<String, Integer> stream = EntryStream.of(singleMap);
        Optional<Map.Entry<String, Integer>> only = stream.onlyOne();

        assertTrue(only.isPresent());
        assertEquals("a", only.get().getKey());
    }

    @Test
    public void testOnlyOneThrowsException() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertThrows(TooManyElementsException.class, () -> stream.onlyOne());
    }

    @Test
    public void testPercentiles() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Optional<Map<Percentage, Map.Entry<String, Integer>>> percentiles = stream.percentiles(Map.Entry.comparingByValue());

        assertTrue(percentiles.isPresent());
        assertFalse(percentiles.get().isEmpty());
    }

    @Test
    public void testCount() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertEquals(3, stream.count());

        EntryStream<String, Integer> emptyStream = EntryStream.of(emptyMap);
        assertEquals(0, emptyStream.count());
    }

    @Test
    public void testIterator() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        ObjIterator<Map.Entry<String, Integer>> iter = stream.iterator();

        int count = 0;
        while (iter.hasNext()) {
            Map.Entry<String, Integer> entry = iter.next();
            assertNotNull(entry.getKey());
            assertNotNull(entry.getValue());
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void testBiIterator() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        BiIterator<String, Integer> iter = stream.biIterator();

        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        while (iter.hasNext()) {
            Pair<String, Integer> next = iter.next();
            keys.add(next.left());
            values.add(next.right());
        }

        assertEquals(Arrays.asList("a", "b", "c"), keys);
        assertEquals(Arrays.asList(1, 2, 3), values);
    }

    @Test
    public void testToList() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        List<Map.Entry<String, Integer>> list = stream.toList();

        assertEquals(3, list.size());
        for (Map.Entry<String, Integer> entry : list) {
            assertTrue(testMap.containsKey(entry.getKey()));
            assertEquals(testMap.get(entry.getKey()), entry.getValue());
        }
    }

    @Test
    public void testToSet() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Set<Map.Entry<String, Integer>> set = stream.toSet();

        assertEquals(3, set.size());
    }

    @Test
    public void testToCollection() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        LinkedHashSet<Map.Entry<String, Integer>> collection = stream.toCollection(LinkedHashSet::new);

        assertEquals(3, collection.size());
        assertTrue(collection instanceof LinkedHashSet);
    }

    @Test
    public void testToMultiset() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Multiset<Map.Entry<String, Integer>> multiset = stream.toMultiset();

        assertEquals(3, multiset.size());
    }

    @Test
    public void testToMultisetWithSupplier() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Multiset<Map.Entry<String, Integer>> multiset = stream.toMultiset(Multiset::new);

        assertEquals(3, multiset.size());
    }

    @Test
    public void testToMap() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> map = stream.toMap();

        assertEquals(3, map.size());
        assertEquals(testMap, map);
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
        assertEquals(3, map.size());
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
    public void testToMapThenApply() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        int size = stream.toMapThenApply(Map::size);

        assertEquals(3, size);
    }

    @Test
    public void testToMapThenAccept() {
        AtomicInteger size = new AtomicInteger();
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        stream.toMapThenAccept(map -> size.set(map.size()));

        assertEquals(3, size.get());
    }

    @Test
    public void testToImmutableMap() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        ImmutableMap<String, Integer> map = stream.toImmutableMap();

        assertEquals(3, map.size());
        assertThrows(UnsupportedOperationException.class, () -> map.put("d", 4));
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
    public void testToMultimap() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("a", 3));

        EntryStream<String, Integer> stream = EntryStream.of(entries);
        ListMultimap<String, Integer> multimap = stream.toMultimap();

        assertEquals(2, multimap.get("a").size());
        assertEquals(1, multimap.get("b").size());
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
    public void testGroupTo() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("a", 3), new AbstractMap.SimpleEntry<>("b", 4));

        EntryStream<String, Integer> stream = EntryStream.of(entries);
        Map<String, List<Integer>> grouped = stream.groupTo();

        assertEquals(2, grouped.size());
        assertEquals(Arrays.asList(1, 3), grouped.get("a"));
        assertEquals(Arrays.asList(2, 4), grouped.get("b"));
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
    public void testGroupToThenApply() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("a", 3));

        EntryStream<String, Integer> stream = EntryStream.of(entries);
        int uniqueKeys = stream.groupToThenApply(Map::size);

        assertEquals(2, uniqueKeys);
    }

    @Test
    public void testGroupToThenAccept() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("a", 3));

        AtomicInteger groupCount = new AtomicInteger();
        EntryStream<String, Integer> stream = EntryStream.of(entries);
        stream.groupToThenAccept(map -> groupCount.set(map.size()));

        assertEquals(2, groupCount.get());
    }

    @Test
    public void testReduceWithIdentity() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map.Entry<String, Integer> result = stream.reduce(new AbstractMap.SimpleEntry<>("", 0),
                (e1, e2) -> new AbstractMap.SimpleEntry<>(e1.getKey() + e2.getKey(), e1.getValue() + e2.getValue()));

        assertEquals(3, result.getKey().length());
        assertEquals(6, result.getValue());
    }

    @Test
    public void testReduceWithoutIdentity() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Optional<Map.Entry<String, Integer>> result = stream.reduce((e1, e2) -> e1.getValue() > e2.getValue() ? e1 : e2);

        assertTrue(result.isPresent());
        assertEquals(3, result.get().getValue());
    }

    @Test
    public void testCollectWithSupplierAccumulatorCombiner() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        List<Map.Entry<String, Integer>> result = stream.collect(ArrayList::new, List::add, List::addAll);

        assertEquals(3, result.size());
    }

    @Test
    public void testCollectWithSupplierAccumulator() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        List<Map.Entry<String, Integer>> result = stream.collect(ArrayList::new, List::add);

        assertEquals(3, result.size());
    }

    @Test
    public void testCollectWithCollector() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> result = stream.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertEquals(3, result.size());
        assertEquals(testMap, result);
    }

    @Test
    public void testCollectThenApply() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        int size = stream.collectThenApply(Collectors.toList(), List::size);

        assertEquals(3, size);
    }

    @Test
    public void testCollectThenAccept() {
        AtomicInteger size = new AtomicInteger();
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        stream.collectThenAccept(Collectors.toList(), list -> size.set(list.size()));

        assertEquals(3, size.get());
    }

    @Test
    public void testJoin() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        String result = stream.join(", ");

        assertEquals("a=1, b=2, c=3", result);
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
    public void testJoinTo() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Joiner joiner = Joiner.with(", ", "=", "{", "}");
        EntryStream<String, Integer> stream = EntryStream.of(map);
        stream.joinTo(joiner);

        assertEquals("{a=1, b=2}", joiner.toString());
    }

    @Test
    public void testTransformB() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        EntryStream<String, String> transformed = stream
                .<String, String> transformB(s -> s.map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), String.valueOf(e.getValue()))));

        Map<String, String> result = transformed.toMap();
        assertEquals(3, result.size());
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));
        assertEquals("3", result.get("c"));
    }

    @Test
    public void testTransformBDeferred() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        EntryStream<String, String> transformed = stream
                .<String, String> transformB(s -> s.map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), String.valueOf(e.getValue()))), true);

        Map<String, String> result = transformed.toMap();
        assertEquals(3, result.size());
    }

    @Test
    public void testIsParallel() {
        EntryStream<String, Integer> sequential = EntryStream.of(testMap);
        assertFalse(sequential.isParallel());

        EntryStream<String, Integer> parallel = sequential.parallel();
        assertTrue(parallel.isParallel());
    }

    @Test
    public void testSequential() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap).parallel();
        assertTrue(stream.isParallel());

        EntryStream<String, Integer> sequential = stream.sequential();
        assertFalse(sequential.isParallel());
    }

    @Test
    public void testInversedToDisposableEntry() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Map<Integer, String> inversed = stream.inversedToDisposableEntry().toMap();

        assertEquals(2, inversed.size());
        assertEquals("a", inversed.get(1));
        assertEquals("b", inversed.get(2));
    }

    @Test
    public void testApplyIfNotEmpty() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Optional<Map<String, Integer>> result = stream.applyIfNotEmpty(es -> es.toMap());

        assertTrue(result.isPresent());
        assertEquals(3, result.get().size());

        EntryStream<String, Integer> emptyStream = EntryStream.empty();
        Optional<Map<String, Integer>> emptyResult = emptyStream.applyIfNotEmpty(es -> es.toMap());

        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testAcceptIfNotEmpty() {
        AtomicBoolean executed = new AtomicBoolean(false);

        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        OrElse orElse = stream.acceptIfNotEmpty(es -> {
            executed.set(true);
            es.toMap();
        });

        assertTrue(executed.get());

        executed.set(false);
        EntryStream<String, Integer> emptyStream = EntryStream.empty();
        emptyStream.acceptIfNotEmpty(es -> executed.set(true)).orElse(() -> executed.set(true));

        assertTrue(executed.get());
    }

    @Test
    public void testOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);

        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        stream.onClose(() -> closed.set(true)).toList();

        assertTrue(closed.get());
    }

    @Test
    public void testClose() {
        AtomicBoolean closed = new AtomicBoolean(false);

        EntryStream.of(testMap).onClose(() -> closed.set(true)).close();

        assertTrue(closed.get());
    }

    // Static factory methods tests

    @Test
    public void testEmpty() {
        EntryStream<String, Integer> empty = EntryStream.empty();
        assertEquals(0, empty.count());
    }

    @Test
    public void testDefer() {
        AtomicBoolean created = new AtomicBoolean(false);

        EntryStream<String, Integer> deferred = EntryStream.defer(() -> {
            created.set(true);
            return EntryStream.of("a", 1);
        });

        assertFalse(created.get());

        Map<String, Integer> result = deferred.toMap();
        assertTrue(created.get());
        assertEquals(1, result.size());
    }

    @Test
    public void testOfNullable() {
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("a", 1);
        EntryStream<String, Integer> stream = EntryStream.ofNullable(entry);
        assertEquals(1, stream.count());

        EntryStream<String, Integer> nullStream = EntryStream.ofNullable(null);
        assertEquals(0, nullStream.count());
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

        assertEquals(3, result.size());
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
    public void testConcat() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("c", 3);
        map2.put("d", 4);

        EntryStream<String, Integer> concatenated = EntryStream.concat(map1, map2);
        Map<String, Integer> result = concatenated.toMap();

        assertEquals(4, result.size());
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

    // Edge cases and error conditions

    @Test
    public void testEmptyStreamOperations() {

        // Test various operations on empty stream
        assertEquals(0, EntryStream.empty().count());
        assertFalse(EntryStream.empty().first().isPresent());
        assertFalse(EntryStream.empty().last().isPresent());
        assertFalse(EntryStream.<String, Integer> empty().min(Map.Entry.comparingByKey()).isPresent());
        assertFalse(EntryStream.<String, Integer> empty().max(Map.Entry.comparingByKey()).isPresent());
        assertTrue(EntryStream.empty().toList().isEmpty());
        assertTrue(EntryStream.empty().toMap().isEmpty());
    }

    @Test
    public void testNullHandling() {
        Map<String, Integer> mapWithNull = new HashMap<>();
        mapWithNull.put("a", 1);
        mapWithNull.put("b", null);
        mapWithNull.put(null, 3);

        EntryStream<String, Integer> stream = EntryStream.of(mapWithNull);
        Map<String, Integer> result = stream.toMap();

        assertEquals(3, result.size());
        assertNull(result.get("b"));
        assertEquals(3, result.get(null));
    }

    @Test
    public void testParallelOperations() {
        Map<String, Integer> largeMap = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            largeMap.put("key" + i, i);
        }

        EntryStream<String, Integer> parallel = EntryStream.of(largeMap).parallel();

        // Test that parallel operations work correctly
        long count = parallel.filter((k, v) -> v % 2 == 0).count();
        assertEquals(500, count);

        EntryStream<String, Integer> parallel2 = EntryStream.of(largeMap).parallel();
        int sum = parallel2.entries().mapToInt(Map.Entry::getValue).sum();
        assertEquals(499500, sum);
    }

    @Test
    public void testStreamReuse() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        stream.toList(); // Consume the stream

        // Attempting to use the stream again should throw exception
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

        // Test that duplicate keys throw exception without merge function
        EntryStream<String, Integer> stream = EntryStream.of(entries);
        assertThrows(IllegalStateException.class, () -> stream.toMap());

        // Test merge function handling
        EntryStream<String, Integer> stream2 = EntryStream.of(entries);
        Map<String, Integer> merged = stream2.toMap(Integer::sum);
        assertEquals(6, merged.get("a"));
    }

    @Test
    public void testLargeDataSetPerformance() {
        // Create a large dataset
        Map<Integer, String> largeMap = new HashMap<>();
        for (int i = 0; i < 10000; i++) {
            largeMap.put(i, "value" + i);
        }

        // Test performance of various operations
        long start = System.currentTimeMillis();

        Map<String, List<Integer>> grouped = EntryStream.of(largeMap)
                .filter((k, v) -> k % 10 == 0)
                .groupBy(e -> e.getValue().substring(0, 6), e -> e.getKey())
                .toMap();

        long duration = System.currentTimeMillis() - start;

        // Verify results
        assertEquals(10, grouped.size());
        assertTrue(duration < 1000); // Should complete in reasonable time
    }

    @Test
    public void testCustomCollectors() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);

        // Test with custom collector
        String result = stream
                .collect(Collector.of(StringBuilder::new, (sb, entry) -> sb.append(entry.getKey()).append(":").append(entry.getValue()).append(";"),
                        (sb1, sb2) -> sb1.append(sb2), StringBuilder::toString));

        assertTrue(result.contains("a:1"));
        assertTrue(result.contains("b:2"));
        assertTrue(result.contains("c:3"));
    }

    @Test
    public void testMapperFunctions() {
        // Test various mapper edge cases
        EntryStream<String, Integer> stream = EntryStream.of(testMap);

        // Test identity mapping
        Map<String, Integer> identity = stream.map(e -> e).toMap();
        assertEquals(testMap, identity);

        // Test null-producing mappers
        EntryStream<String, Integer> stream2 = EntryStream.of("a", 1);
        Map<String, Integer> nullMapped = stream2.mapKey(k -> (String) null).toMap();
        assertEquals(1, nullMapped.get(null));
    }

    @Test
    public void testPredicateEdgeCases() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);

        // Always true predicate
        assertEquals(3, stream.filter(e -> true).count());

        // Always false predicate
        EntryStream<String, Integer> stream2 = EntryStream.of(testMap);
        assertEquals(0, stream2.filter(e -> false).count());

        // Null-safe predicates
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

        // Test stable sort with equal values
        EntryStream<String, Integer> stream = EntryStream.of(map);
        List<Map.Entry<String, Integer>> sorted = stream.sortedByValue(Comparator.naturalOrder()).toList();

        assertEquals(3, sorted.size());
        // All values are equal, so order should be preserved
    }

    @Test
    public void testBoundaryConditions() {
        // Test with single element
        EntryStream<String, Integer> single = EntryStream.of("a", 1);
        assertEquals(1, single.count());

        // Test with very large values
        EntryStream<String, Long> large = EntryStream.of("max", Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, large.first().get().getValue());

        // Test with negative indices
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        assertThrows(IllegalArgumentException.class, () -> stream.skip(-1));

        // Test with zero limits
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

        // Create multiple threads that process the same stream source
        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(() -> {
                Map<String, Integer> result = EntryStream.of(concurrentMap).filter((k, v) -> v % 2 == 0).toMap();
                results.add(result);
            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all threads got the same result
        assertEquals(10, results.size());
        for (Map<String, Integer> result : results) {
            assertEquals(50, result.size());
        }
    }

    @Test
    public void testMemoryEfficiency() {
        // Test that streams don't unnecessarily store all elements
        AtomicInteger counter = new AtomicInteger(0);

        // Create a large stream but only take first few elements
        Map<Integer, Integer> largeMap = new HashMap<>();
        for (int i = 0; i < 1000000; i++) {
            largeMap.put(i, i);
        }

        List<Map.Entry<Integer, Integer>> firstTen = EntryStream.of(largeMap).peek(e -> counter.incrementAndGet()).limit(10).toList();

        assertEquals(10, firstTen.size());
        // Due to lazy evaluation, only necessary elements should be processed
        assertTrue(counter.get() <= 1000); // Much less than 1,000,000
    }

    @Test
    public void testErrorPropagation() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        // Test that exceptions in mappers are properly propagated
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

        // Test that close handlers are called even when exception occurs
        try {
            EntryStream.of(testMap).onClose(() -> resourceClosed.set(true)).map(e -> {
                throw new RuntimeException("Force exception");
            }).toList();
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            // Expected
        }

        assertTrue(resourceClosed.get());
    }
}