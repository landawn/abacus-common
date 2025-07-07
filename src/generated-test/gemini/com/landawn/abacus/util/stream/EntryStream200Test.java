package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;


/**
 * Unit tests for EntryStream.
 */
public class EntryStream200Test extends TestBase {

    @Test
    public void testOfMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, EntryStream.of(map).count());
    }

    @Test
    public void testKeys() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(Arrays.asList("a", "b"), EntryStream.of(map).keys().toList());
    }

    @Test
    public void testValues() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(Arrays.asList(1, 2), EntryStream.of(map).values().toList());
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
        map.put("b", 1); // Duplicate value
        map.put("c", 2);

        // distinct by value
        long count = EntryStream.of(map).distinctByValue().count();
        assertEquals(2, count);

        // distinct by key is default
        assertEquals(3, EntryStream.of(map).distinct().count());
    }

    @Test
    public void testSorted() {
        Map<String, Integer> map = new HashMap<>();
        map.put("c", 3);
        map.put("a", 1);
        map.put("b", 2);

        List<String> sortedKeys = EntryStream.of(map).sortedByKey(Comparator.naturalOrder()).keys().toList();
        assertEquals(Arrays.asList("a", "b", "c"), sortedKeys);

        List<Integer> sortedValues = EntryStream.of(map).sortedByValue(Comparator.naturalOrder()).values().toList();

        assertEquals(Arrays.asList(1, 2, 3), sortedValues);
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
    public void testSkip() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(1, EntryStream.of(map).skip(2).count());
    }

    @Test
    public void testToMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Map<String, Integer> resultMap = EntryStream.of(map).toMap();
        assertEquals(map, resultMap);

        // test with merge function
        Map<String, Integer> mapWithDuplicates = new HashMap<>();
        mapWithDuplicates.put("a", 1);
        mapWithDuplicates.put("b", 2);
        mapWithDuplicates.put("a", 3); // duplicate key

        Map<String, Integer> mergedMap = EntryStream.of(mapWithDuplicates).toMap((v1, v2) -> v1 + v2);
        assertEquals(3, (int) mergedMap.get("a"));

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
    public void testMin() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        assertEquals(1, (int) EntryStream.of(map).minByValue(Comparator.naturalOrder()).get().getValue());
    }

    @Test
    public void testMax() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        assertEquals(2, (int) EntryStream.of(map).maxByValue(Comparator.naturalOrder()).get().getValue());
    }

    @Test
    public void testCount() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, EntryStream.of(map).count());
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
    public void testInversed() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Map<Integer, String> inversedMap = EntryStream.of(map).inversed().toMap();
        assertEquals("a", inversedMap.get(1));
        assertEquals("b", inversedMap.get(2));
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
    public void testEmpty() {
        assertTrue(EntryStream.empty().count() == 0);
    }

    @Test
    public void testOfNullable() {
        assertEquals(0, EntryStream.ofNullable(null).count());

        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("a", 1);
        assertEquals(1, EntryStream.ofNullable(entry).count());

    }

    @Test
    public void testReversed() {
        final Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");

        final List<Map.Entry<Integer, String>> reversedEntries = EntryStream.of(map).reversed().toList();

        assertEquals(3, reversedEntries.size());
        assertEquals(N.newEntry(3, "c"), reversedEntries.get(0));
        assertEquals(N.newEntry(2, "b"), reversedEntries.get(1));
        assertEquals(N.newEntry(1, "a"), reversedEntries.get(2));
    }

    @Test
    public void testGroupTo() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 1);
        map.put("d", 2);

        final Map<Integer, List<Map.Entry<String, Integer>>> grouped = EntryStream.of(map).entries().groupTo(Map.Entry::getValue);

        assertEquals(2, grouped.size());
        assertEquals(2, grouped.get(1).size());
        assertEquals("a", grouped.get(1).get(0).getKey());
        assertEquals("c", grouped.get(1).get(1).getKey());

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
    public void testToImmutableMap() {
        final Map<Integer, String> map = new HashMap<>();
        map.put(1, "a");
        map.put(2, "b");

        final Map<Integer, String> immutableMap = EntryStream.of(map).toImmutableMap();

        assertEquals(2, immutableMap.size());
        assertEquals("a", immutableMap.get(1));
        assertEquals("b", immutableMap.get(2));

        // Test immutability
        try {
            immutableMap.put(3, "c");
            assert false;
        } catch (UnsupportedOperationException e) {
            // expected
        }
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
    public void testDistinctByValue() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 1);

        assertEquals(2, EntryStream.of(map).distinctByValue().count());
    }

    @Test
    public void testDistinctByKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("a", 3);

        // distinctByKey is default for maps.
        assertEquals(2, EntryStream.of(map).distinctByKey().count());
    }

    @Test
    public void testCollectThenApply() {
        final Map<Integer, String> map = new HashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");

        final int size = EntryStream.of(map).collectThenApply(Collectors.toList(), List::size);
        assertEquals(3, size);
    }

    @Test
    public void testCollectThenAccept() {
        final Map<Integer, String> map = new HashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");

        final AtomicInteger count = new AtomicInteger(0);
        EntryStream.of(map).collectThenAccept(Collectors.toList(), list -> count.set(list.size()));
        assertEquals(3, count.get());
    }

    @Test
    public void testSelectByKey() {
        final Map<Object, String> map = new HashMap<>();
        map.put(1, "a");
        map.put("2", "b");
        map.put(3L, "c");

        final Map<Integer, String> result = EntryStream.of(map).selectByKey(Integer.class).toMap();

        assertEquals(1, result.size());
        assertTrue(result.containsKey(1));
    }

    @Test
    public void testSelectByValue() {
        final Map<String, Object> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", "2");
        map.put("c", 3L);

        final Map<String, Integer> result = EntryStream.of(map).selectByValue(Integer.class).toMap();
        assertEquals(1, result.size());
        assertTrue(result.containsKey("a"));
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
    public void testSymmetricDifference() {
        final Map<Integer, String> mapA = new HashMap<>();
        mapA.put(1, "a");
        mapA.put(2, "b");
        final Map<Integer, String> mapB = new HashMap<>();
        mapB.put(2, "b");
        mapB.put(3, "c");

        final Map<Integer, String> result = EntryStream.of(mapA).symmetricDifference(mapB.entrySet()).toMap();

        assertEquals(2, result.size());
        assertTrue(result.containsKey(1));
        assertTrue(result.containsKey(3));
    }

    @Test
    public void testToMapThenApply() {
        final Map<Integer, String> map = new HashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");

        final int size = EntryStream.of(map).toMapThenApply(Map::size);
        assertEquals(3, size);
    }

    @Test
    public void testToMapThenAccept() {
        final Map<Integer, String> map = new HashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");

        final AtomicInteger count = new AtomicInteger(0);
        EntryStream.of(map).toMapThenAccept(m -> count.set(m.size()));
        assertEquals(3, count.get());
    }

    @Test
    public void testGroupToThenApply() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 1);

        final int size = EntryStream.of(map).groupToThenApply(Map::size);

        assertEquals(3, size);
    }

    @Test
    public void testGroupToThenAccept() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 1);

        final AtomicInteger count = new AtomicInteger(0);
        EntryStream.of(map).groupToThenAccept(m -> count.set(m.size()));
        assertEquals(3, count.get());
    }

    @Test
    public void testSliding() {
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
    public void testFilterByKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(1, EntryStream.of(map).filterByKey(k -> k.equals("a")).count());
    }

    @Test
    public void testFilterByValue() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(1, EntryStream.of(map).filterByValue(v -> v > 1).count());
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
    public void testMinByKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("c", 3);
        map.put("a", 1);
        map.put("b", 2);
        assertEquals("a", EntryStream.of(map).minByKey(Comparator.naturalOrder()).get().getKey());
    }

    @Test
    public void testMaxByKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("c", 3);
        map.put("a", 1);
        map.put("b", 2);
        assertEquals("c", EntryStream.of(map).maxByKey(Comparator.naturalOrder()).get().getKey());
    }

    @Test
    public void testBiIterator() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        final com.landawn.abacus.util.BiIterator<String, Integer> biIterator = EntryStream.of(map).biIterator();
        assertTrue(biIterator.hasNext());
        assertEquals(Pair.of("a", 1), biIterator.next());
        assertTrue(biIterator.hasNext());
        assertEquals(Pair.of("b", 2), biIterator.next());
        assertFalse(biIterator.hasNext());
    }

    @Test
    public void testFromStream() {
        java.util.stream.Stream<Map.Entry<String, Integer>> jdkStream = java.util.stream.Stream.of(new AbstractMap.SimpleEntry<>("a", 1));

        assertEquals(1, Stream.from(jdkStream).mapToEntry(Fn.identity()).count());
    }

    @Test
    public void testJoinTo() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        final com.landawn.abacus.util.Joiner joiner = com.landawn.abacus.util.Joiner.with(", ");
        EntryStream.of(map).joinTo(joiner);
        assertEquals("a=1, b=2", joiner.toString());
    }

    @Test
    public void testParallel() {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            map.put("key" + i, i);
        }
        long count = EntryStream.of(map).parallel().filter(e -> e.getValue() % 2 == 0).count();
        assertEquals(500, count);

        // Ensure original stream is not modified.
        assertFalse(EntryStream.of(map).isParallel());
    }

    @Test
    public void testSequential() {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            map.put("key" + i, i);
        }
        EntryStream<String, Integer> parallelStream = EntryStream.of(map).parallel();
        assertTrue(parallelStream.isParallel());

        EntryStream<String, Integer> sequentialStream = parallelStream.sequential();
        assertFalse(sequentialStream.isParallel());
    }

    @Test
    public void testClose() {
        AtomicInteger closeCounter = new AtomicInteger(0);
        EntryStream<String, Integer> stream = EntryStream.of(new HashMap<String, Integer>()).onClose(closeCounter::incrementAndGet);
        stream.close();
        assertEquals(1, closeCounter.get());
        stream.close(); // should not increment again
        assertEquals(1, closeCounter.get());
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
        map.put("c", 1); // value is not distinct
        map.put("a", 3); // key is not distinct

        final EntryStream<String, Integer> stream = EntryStream.of(map);

        final Map<String, Integer> result = stream.distinct((e1, e2) -> e1.getValue() > e2.getValue() ? e1 : e2).toMap();

        assertEquals(2, result.get("b").intValue());
        assertEquals(3, result.get("a").intValue());
    }

    @Test
    public void testCycled() {
        final Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");

        final List<Integer> result = EntryStream.of(map).cycled().limit(4).keys().toList();
        assertEquals(Arrays.asList(1, 2, 1, 2), result);
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
        assertEquals(2, EntryStream.of(N.asList(entry1, entry2)).count());
    }

    @Test
    public void testFromIterator() {
        List<Map.Entry<String, Integer>> list = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2));
        assertEquals(2, EntryStream.of(list.iterator()).count());
    }

    @Test
    public void testFromIterable() {
        List<Map.Entry<String, Integer>> list = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2));
        assertEquals(2, EntryStream.of((Iterable<Map.Entry<String, Integer>>) list).count());
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
        final List<String> list = N.asList("a", "b");
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
    public void testEntries() {
        final Map<String, Integer> map = N.asMap("a", 1, "b", 2);
        final List<Map.Entry<String, Integer>> entries = EntryStream.of(map).entries().toList();
        assertTrue(entries.contains(N.newEntry("a", 1)));
        assertTrue(entries.contains(N.newEntry("b", 2)));
    }

    @Test
    public void testCollapseByKey() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("aa", 2);
        map.put("b", 3);
        map.put("bb", 4);

        final List<List<Integer>> result = EntryStream.of(map).collapseByKey((k1, k2) -> k1.substring(0, 1).equals(k2.substring(0, 1))).toList();

        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)), result);
    }

    @Test
    public void testCollapseByValue() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 1);
        map.put("c", 2);
        map.put("d", 2);

        final List<List<String>> result = EntryStream.of(map).collapseByValue((v1, v2) -> v1.equals(v2)).toList();

        assertEquals(Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d")), result);
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
    public void testIntersection() {
        final Map<String, Integer> mapA = N.asMap("a", 1, "b", 2);
        final Map<String, Integer> mapB = N.asMap("b", 2, "c", 3);

        final Map<String, Integer> result = EntryStream.of(mapA).intersection(mapB.entrySet()).toMap();

        assertEquals(1, result.size());
        assertTrue(result.containsKey("b"));
    }

    @Test
    public void testDifference() {
        final Map<String, Integer> mapA = N.asMap("a", 1, "b", 2);
        final Map<String, Integer> mapB = N.asMap("b", 2, "c", 3);

        final Map<String, Integer> result = EntryStream.of(mapA).difference(mapB.entrySet()).toMap();

        assertEquals(1, result.size());
        assertTrue(result.containsKey("a"));
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
        assertFalse(original.equals(shuffled)); // Highly likely to be different
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
    public void testElementAt() {
        final Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");

        assertEquals(N.newEntry(2, "b"), EntryStream.of(map).elementAt(1).get());
        assertFalse(EntryStream.of(map).elementAt(3).isPresent());
    }

    @Test
    public void testIfEmpty() {
        final AtomicInteger counter = new AtomicInteger(0);
        EntryStream.empty().ifEmpty(counter::incrementAndGet).count();
        assertEquals(1, counter.get());

        EntryStream.of(N.asMap("a", 1)).ifEmpty(counter::incrementAndGet);
        assertEquals(1, counter.get());
    }

    // Additional tests for remaining methods

    @Test
    public void testPrependIfEmptyWithSupplier() {
        Map<String, Integer> map = N.asMap("a", 1);
        assertEquals(1, EntryStream.<String, Integer> empty().appendIfEmpty(() -> EntryStream.of(map)).count());
        assertEquals(1, EntryStream.of(map).appendIfEmpty(() -> EntryStream.of(N.asMap("b", 2))).count());
    }

    @Test
    public void testSortedByInt() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("c", 3);
        map.put("a", 1);
        map.put("b", 2);
        List<Integer> values = EntryStream.of(map).sortedByInt(Map.Entry::getValue).values().toList();
        assertEquals(Arrays.asList(1, 2, 3), values);
    }

    @Test
    public void testReverseSorted() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        List<Integer> values = EntryStream.of(map).reverseSortedBy(Map.Entry::getValue).values().toList();
        assertEquals(Arrays.asList(3, 2, 1), values);
    }

    @Test
    public void testNMatch() {
        Map<String, Integer> map = N.asMap("a", 1, "b", 2, "c", 3, "d", 4);
        assertTrue(EntryStream.of(map).nMatch(2, 2, e -> e.getValue() % 2 == 0));
        assertFalse(EntryStream.of(map).nMatch(3, 3, e -> e.getValue() % 2 == 0));
        assertTrue(EntryStream.of(map).nMatch(1, 3, e -> e.getValue() % 2 == 0));
    }

    @Test
    public void testOnlyOne() {
        Map<String, Integer> singleMap = N.asMap("a", 1);
        assertEquals(1, EntryStream.of(singleMap).onlyOne().get().getValue().intValue());
        assertFalse(EntryStream.<String, Integer> empty().onlyOne().isPresent());

        Map<String, Integer> multiMap = N.asMap("a", 1, "b", 2);
        assertThrows(TooManyElementsException.class, () -> EntryStream.of(multiMap).onlyOne());
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
    public void testAcceptIfNotEmpty() {
        final AtomicInteger counter = new AtomicInteger(0);
        final AtomicInteger elseCounter = new AtomicInteger(0);

        EntryStream.of(N.asMap("a", 1)).acceptIfNotEmpty(s -> counter.incrementAndGet()).orElse(elseCounter::incrementAndGet);
        assertEquals(1, counter.get());
        assertEquals(0, elseCounter.get());

        EntryStream.<String, Integer> empty().acceptIfNotEmpty(s -> counter.incrementAndGet()).orElse(elseCounter::incrementAndGet);
        assertEquals(1, counter.get());
        assertEquals(1, elseCounter.get());
    }

    @Test
    public void testForEachIndexed() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        final AtomicInteger indexSum = new AtomicInteger(0);
        EntryStream.of(map).forEachIndexed((i, e) -> indexSum.addAndGet(i));
        assertEquals(1, indexSum.get()); // 0 + 1
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
}
