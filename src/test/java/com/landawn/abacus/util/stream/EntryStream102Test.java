package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.u;

@Tag("new-test")
public class EntryStream102Test extends TestBase {

    private Map<String, Integer> testMap;

    @BeforeEach
    public void setUp() {
        testMap = new LinkedHashMap<>();
        testMap.put("one", 1);
        testMap.put("two", 2);
        testMap.put("three", 3);
        testMap.put("four", 4);
        testMap.put("five", 5);
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
    public void testNullHandling() {
        Map<String, Integer> mapWithNulls = new LinkedHashMap<>();
        mapWithNulls.put("a", 1);
        mapWithNulls.put(null, 2);
        mapWithNulls.put("c", null);
        mapWithNulls.put("d", 4);

        Map<String, Integer> noNullKeys = EntryStream.of(mapWithNulls).filterByKey(Objects::nonNull).toMap();
        assertEquals(3, noNullKeys.size());
        assertFalse(noNullKeys.containsKey(null));

        Map<String, Integer> noNullValues = EntryStream.of(mapWithNulls).filterByValue(Objects::nonNull).toMap();
        assertEquals(3, noNullValues.size());
        assertFalse(noNullValues.containsValue(null));
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
    public void testStreamReuse() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);

        long count = stream.count();
        assertEquals(5, count);

        assertThrows(IllegalStateException.class, () -> {
            stream.toList();
        });
    }

    @Test
    public void testEmptyStreamOperations() {

        assertEquals(0, EntryStream.<String, Integer> empty().count());
        assertFalse(EntryStream.<String, Integer> empty().first().isPresent());
        assertFalse(EntryStream.<String, Integer> empty().first().isPresent());
        assertFalse(EntryStream.<String, Integer> empty().min(Map.Entry.comparingByKey()).isPresent());
        assertFalse(EntryStream.<String, Integer> empty().max(Map.Entry.comparingByValue()).isPresent());
        assertTrue(EntryStream.<String, Integer> empty().allMatch(e -> false));
        assertFalse(EntryStream.<String, Integer> empty().anyMatch(e -> true));
        assertTrue(EntryStream.<String, Integer> empty().noneMatch(e -> true));

        u.Optional<Entry<String, Integer>> reduced = EntryStream.<String, Integer> empty().reduce((e1, e2) -> e1);
        assertFalse(reduced.isPresent());
    }

    @Test
    public void testCustomCollectors() {
        Map<Boolean, String> partitioned = EntryStream.of(testMap)
                .collect(Collectors.partitioningBy(e -> e.getValue() > 3, Collectors.mapping(Entry::getKey, Collectors.joining(", "))));

        assertEquals(2, partitioned.size());
        assertTrue(partitioned.get(true).contains("four"));
        assertTrue(partitioned.get(true).contains("five"));
        assertTrue(partitioned.get(false).contains("one"));
        assertTrue(partitioned.get(false).contains("two"));
        assertTrue(partitioned.get(false).contains("three"));
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
}
