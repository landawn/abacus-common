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

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.u;


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
        // Test parallel with custom executor
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            long count = EntryStream.of(testMap).parallel(executor).filter(e -> e.getValue() > 2).count();

            assertEquals(3, count);

            // Test parallel with maxThreadNum and executor
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
        // Test of with multiple key-value pairs (up to 7 pairs)
        Map<String, Integer> map7 = EntryStream.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7).toMap();

        assertEquals(7, map7.size());
        assertEquals(1, map7.get("a"));
        assertEquals(7, map7.get("g"));

        // Test of with single entry
        Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 100);
        Map<String, Integer> singleMap = EntryStream.of(Collections.singletonList(entry)).toMap();
        assertEquals(1, singleMap.size());
        assertEquals(100, singleMap.get("key"));
    }

    @Test
    public void testZipWithIterators() {
        // Test zip with null handling
        Iterator<String> nullKeys = null;
        Iterator<Integer> nullValues = null;

        assertEquals(0, EntryStream.zip(nullKeys, Arrays.asList(1, 2, 3).iterator()).count());
        assertEquals(0, EntryStream.zip(Arrays.asList("a", "b").iterator(), nullValues).count());
        assertEquals(0, EntryStream.zip(nullKeys, nullValues).count());

        // Test zip with iterators and default values
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
        // Test zip with null iterables
        Iterable<String> nullKeys = null;
        Iterable<Integer> nullValues = null;

        assertEquals(0, EntryStream.zip(nullKeys, Arrays.asList(1, 2, 3)).count());
        assertEquals(0, EntryStream.zip(Arrays.asList("a", "b"), nullValues).count());

        // Test zip with null iterables and default values
        assertEquals(0, EntryStream.zip(nullKeys, nullValues, "default", 0).count());
    }

    @Test
    public void testConcatWithEmptyMaps() {
        // Test concat with empty maps
        Map<String, Integer> empty1 = new HashMap<>();
        Map<String, Integer> empty2 = new HashMap<>();
        Map<String, Integer> nonEmpty = new HashMap<>();
        nonEmpty.put("a", 1);

        // Test varargs concat with empty maps
        Map<String, Integer> result1 = EntryStream.concat(empty1, empty2, nonEmpty).toMap();
        assertEquals(1, result1.size());
        assertEquals(1, result1.get("a"));

        // Test concat with empty collection
        Collection<Map<String, Integer>> emptyCollection = Collections.emptyList();
        assertEquals(0, EntryStream.concat(emptyCollection).count());

        // Test concat with null in collection (should handle gracefully)
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

        // Test merge with one empty map
        List<Entry<String, Integer>> merged1 = EntryStream.merge(empty, nonEmpty, selector).toList();
        assertEquals(2, merged1.size());

        List<Entry<String, Integer>> merged2 = EntryStream.merge(nonEmpty, empty, selector).toList();
        assertEquals(2, merged2.size());

        // Test merge with both empty
        List<Entry<String, Integer>> merged3 = EntryStream.merge(empty, empty, selector).toList();
        assertEquals(0, merged3.size());

        // Test merge three maps with some empty
        Map<String, Integer> another = new HashMap<>();
        another.put("c", 3);

        List<Entry<String, Integer>> merged4 = EntryStream.merge(empty, nonEmpty, another, selector).toList();
        assertEquals(3, merged4.size());
    }

    @Test
    public void testGroupByWithKeyMapperValueMapper() {
        // Create data for testing
        List<Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("apple", 5), new AbstractMap.SimpleEntry<>("apricot", 7),
                new AbstractMap.SimpleEntry<>("banana", 6), new AbstractMap.SimpleEntry<>("blueberry", 9), new AbstractMap.SimpleEntry<>("blackberry", 10));

        // Test groupBy with key and value mappers
        Map<Character, List<Integer>> grouped = EntryStream.of(entries).groupBy(e -> e.getKey().charAt(0), Entry::getValue).toMap();

        assertEquals(2, grouped.size());
        assertEquals(Arrays.asList(5, 7), grouped.get('a'));
        assertEquals(Arrays.asList(6, 9, 10), grouped.get('b'));
    }

    @Test
    public void testGroupByWithKeyMapperValueMapperMergeFunction() {
        // Test groupBy with merge function
        Map<Boolean, String> grouped = EntryStream.of(testMap)
                .groupBy(e -> e.getValue() % 2 == 0, // even/odd
                        Entry::getKey, // value mapper
                        (s1, s2) -> s1 + "," + s2 // merge function
                )
                .toMap();

        assertEquals(2, grouped.size());
        // Note: order might vary
        assertTrue(grouped.get(true).contains("two"));
        assertTrue(grouped.get(true).contains("four"));
        assertTrue(grouped.get(false).contains("one"));
        assertTrue(grouped.get(false).contains("three"));
        assertTrue(grouped.get(false).contains("five"));
    }

    @Test
    public void testOfWithIterableOfEntries() {
        // Test of with various entry collections
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
        // Test of with iterator of entries
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
        // Test complex parallel operations with multiple transformations
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

        // Verify results
        assertTrue(result.size() > 0);
        result.forEach((k, v) -> {
            assertTrue(k.contains("5"));
            assertTrue(v % 4 == 0); // Original was even, then doubled
        });
    }

    @Test
    public void testEdgeCasesForSkipLimit() {
        // Test skip with negative number (should throw)
        assertThrows(IllegalArgumentException.class, () -> {
            EntryStream.of(testMap).skip(-1).count();
        });

        // Test limit with negative number (should throw)
        assertThrows(IllegalArgumentException.class, () -> {
            EntryStream.of(testMap).limit(-1).count();
        });

        // Test skip more than size
        assertEquals(0, EntryStream.of(testMap).skip(10).count());

        // Test limit 0
        assertEquals(0, EntryStream.of(testMap).limit(0).count());
    }

    @Test
    public void testStreamClosing() {
        // Test that operations after close throw exception
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        stream.close();

        assertThrows(IllegalStateException.class, () -> {
            stream.count();
        });

        // Test close handlers are called
        AtomicInteger closeCount = new AtomicInteger(0);
        try (EntryStream<String, Integer> s = EntryStream.of(testMap).onClose(() -> closeCount.incrementAndGet()).onClose(() -> closeCount.incrementAndGet())) {
            s.limit(2).toList();
        }
        assertEquals(2, closeCount.get());

        // Test multiple close calls
        EntryStream<String, Integer> stream2 = EntryStream.of(testMap).onClose(() -> closeCount.incrementAndGet()); // Should not increment again
        stream2.close();
        assertEquals(3, closeCount.get()); // Only one more increment
    }

    @Test
    public void testNullHandling() {
        // Test with null keys and values
        Map<String, Integer> mapWithNulls = new LinkedHashMap<>();
        mapWithNulls.put("a", 1);
        mapWithNulls.put(null, 2);
        mapWithNulls.put("c", null);
        mapWithNulls.put("d", 4);

        // Filter out null keys
        Map<String, Integer> noNullKeys = EntryStream.of(mapWithNulls).filterByKey(Objects::nonNull).toMap();
        assertEquals(3, noNullKeys.size());
        assertFalse(noNullKeys.containsKey(null));

        // Filter out null values
        Map<String, Integer> noNullValues = EntryStream.of(mapWithNulls).filterByValue(Objects::nonNull).toMap();
        assertEquals(3, noNullValues.size());
        assertFalse(noNullValues.containsValue(null));
    }

    @Test
    public void testComplexGroupByOperations() {
        // Test nested grouping
        Map<String, List<String>> data = new HashMap<>();
        data.put("fruit", Arrays.asList("apple", "apricot", "avocado"));
        data.put("vegetable", Arrays.asList("asparagus", "artichoke"));
        data.put("grain", Arrays.asList("amaranth"));

        // Group by category, then by second letter
        Map<String, Map<Character, List<String>>> nested = EntryStream.of(data)
                .flatmapValue(Function.identity())
                .groupBy(Entry::getKey,
                        Collectors.groupingBy(e -> e.getValue().length() > 1 ? e.getValue().charAt(1) : ' ',
                                Collectors.mapping(Entry::getValue, Collectors.toList())))
                .toMap();

        assertEquals(3, nested.size());
        assertTrue(nested.get("fruit").containsKey('p')); // apple, apricot
        assertTrue(nested.get("fruit").containsKey('v')); // avocado
    }

    @Test
    public void testCombiningMultipleStreams() {
        // Test combining results from multiple entry streams
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 3);
        map2.put("c", 4);

        Map<String, Integer> map3 = new HashMap<>();
        map3.put("c", 5);
        map3.put("d", 6);

        // Concat and group by key, summing values
        Map<String, Integer> combined = EntryStream.concat(map1, map2, map3).groupBy(Integer::sum).toMap();

        assertEquals(4, combined.size());
        assertEquals(1, combined.get("a"));
        assertEquals(5, combined.get("b")); // 2 + 3
        assertEquals(9, combined.get("c")); // 4 + 5
        assertEquals(6, combined.get("d"));
    }

    @Test
    public void testPerformanceOptimizations() {
        // Test that certain operations are optimized
        // For example, keys() on a map-backed EntryStream should be efficient

        Map<String, Integer> largeMap = new LinkedHashMap<>();
        for (int i = 0; i < 10000; i++) {
            largeMap.put("key" + i, i);
        }

        // This should use the keySet() view directly when possible
        long keyCount = EntryStream.of(largeMap).keys().count();
        assertEquals(10000, keyCount);

        // Similarly for values()
        long valueSum = EntryStream.of(largeMap).values().mapToLong(Integer::longValue).sum();
        assertEquals(49995000L, valueSum); // Sum of 0 to 9999
    }

    @Test
    public void testChainedTransformations() {
        // Test complex chained transformations
        Map<Integer, List<String>> result = EntryStream.of(testMap)
                .filterByValue(v -> v <= 4)
                .mapKey(Fn.toUpperCase())
                .flatMapValue(v -> Stream.range(1, v + 1))
                .inversed()
                .groupTo();

        // Each number should map to the keys that generated it
        assertEquals(4, result.size());
        assertTrue(result.get(1).containsAll(Arrays.asList("ONE", "TWO", "THREE", "FOUR")));
        assertTrue(result.get(2).containsAll(Arrays.asList("TWO", "THREE", "FOUR")));
        assertTrue(result.get(3).containsAll(Arrays.asList("THREE", "FOUR")));
        assertTrue(result.get(4).contains("FOUR"));
    }

    @Test
    public void testStreamReuse() {
        // Verify that streams cannot be reused after terminal operation
        EntryStream<String, Integer> stream = EntryStream.of(testMap);

        // First terminal operation
        long count = stream.count();
        assertEquals(5, count);

        // Second terminal operation should fail
        assertThrows(IllegalStateException.class, () -> {
            stream.toList();
        });
    }

    @Test
    public void testEmptyStreamOperations() {

        // Test various operations on empty stream
        assertEquals(0, EntryStream.<String, Integer> empty().count());
        assertFalse(EntryStream.<String, Integer> empty().first().isPresent());
        assertFalse(EntryStream.<String, Integer> empty().first().isPresent());
        assertFalse(EntryStream.<String, Integer> empty().min(Map.Entry.comparingByKey()).isPresent());
        assertFalse(EntryStream.<String, Integer> empty().max(Map.Entry.comparingByValue()).isPresent());
        assertTrue(EntryStream.<String, Integer> empty().allMatch(e -> false));
        assertFalse(EntryStream.<String, Integer> empty().anyMatch(e -> true));
        assertTrue(EntryStream.<String, Integer> empty().noneMatch(e -> true));

        // Test reduce on empty stream
        u.Optional<Entry<String, Integer>> reduced = EntryStream.<String, Integer> empty().reduce((e1, e2) -> e1);
        assertFalse(reduced.isPresent());
    }

    @Test
    public void testCustomCollectors() {
        // Test with custom collectors
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
        // Test that exceptions in parallel streams are properly propagated
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
        // Test with larger data set to ensure correctness
        Map<Integer, Integer> largeMap = new LinkedHashMap<>();
        for (int i = 0; i < 10000; i++) {
            largeMap.put(i, i * 2);
        }

        // Complex operation on large data
        Map<Integer, List<Integer>> grouped = EntryStream.of(largeMap)
                .filter(e -> e.getKey() % 100 == 0)
                .mapValue(v -> v / 2)
                .flatMapKey(k -> Stream.of(k, k + 1000))
                .groupBy()
                .toMap();

        // Verify some results
        assertEquals(110, grouped.size()); // 100 original keys, each mapped to 2 keys
        assertTrue(grouped.containsKey(0));
        assertTrue(grouped.containsKey(1000));
        assertEquals(Arrays.asList(0), grouped.get(0));
        assertEquals(Arrays.asList(0, 1000), grouped.get(1000));
    }

    @Test
    public void testMemoryEfficientOperations() {
        // Test operations that should be memory efficient
        // Using limit() should not process all elements

        AtomicInteger processCount = new AtomicInteger(0);

        List<Entry<String, Integer>> limited = EntryStream.of(testMap).peek(e -> processCount.incrementAndGet()).limit(2).toList();

        assertEquals(2, limited.size());
        assertEquals(2, processCount.get()); // Should only process 2 elements
    }

    @Test
    public void testSpecializedComparators() {
        // Test with specialized comparators
        Map<String, Double> doubleMap = new LinkedHashMap<>();
        doubleMap.put("a", 1.5);
        doubleMap.put("b", Double.NaN);
        doubleMap.put("c", 2.5);
        doubleMap.put("d", Double.POSITIVE_INFINITY);
        doubleMap.put("e", Double.NEGATIVE_INFINITY);

        // Sort by value handling special double values
        List<Entry<String, Double>> sorted = EntryStream.of(doubleMap).sortedByValue(Comparator.naturalOrder()).toList();

        // Verify order handles special values correctly
        assertEquals(Double.NEGATIVE_INFINITY, sorted.get(0).getValue());
        assertEquals(1.5, sorted.get(1).getValue());
        assertEquals(2.5, sorted.get(2).getValue());
        assertEquals(Double.POSITIVE_INFINITY, sorted.get(3).getValue());
        assertTrue(Double.isNaN(sorted.get(4).getValue()));
    }

    @Test
    public void testIntermediateOperationState() {
        // Test that intermediate operations maintain proper state
        EntryStream<String, Integer> base = EntryStream.of(testMap);

        // Create multiple derived streams
        EntryStream<String, Integer> filtered1 = base.filter(e -> e.getValue() > 2);

        // Both should work independently
        assertEquals(3, filtered1.count());
    }

    @Test
    public void testComplexMapMulti() {
        // Test mapMulti with complex logic
        List<Entry<String, String>> result = EntryStream.of("a", 2, "b", 3).<String, String> mapMulti((entry, consumer) -> {
            String key = entry.getKey();
            int value = entry.getValue();

            // Generate multiple entries based on value
            for (int i = 1; i <= value; i++) {
                consumer.accept(new AbstractMap.SimpleEntry<>(key + i, key.toUpperCase().repeat(i)));
            }
        }).toList();

        assertEquals(5, result.size()); // 2 + 3
        assertEquals("a1", result.get(0).getKey());
        assertEquals("A", result.get(0).getValue());
        assertEquals("a2", result.get(1).getKey());
        assertEquals("AA", result.get(1).getValue());
        assertEquals("b3", result.get(4).getKey());
        assertEquals("BBB", result.get(4).getValue());
    }
}
