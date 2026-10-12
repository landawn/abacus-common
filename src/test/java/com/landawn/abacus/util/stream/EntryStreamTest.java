package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.BiIterator;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Pair;

/** EntryStream general contracts. */
public class EntryStreamTest extends EntryStreamTestSupport {

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
    public void testBiIteratorOperations() {
        BiIterator<String, Integer> iter = EntryStream.of(testMap).biIterator();

        List<Pair<String, Integer>> pairs = new ArrayList<>();
        iter.forEachRemaining((k, v) -> pairs.add(Pair.of(k, v)));

        assertEquals(5, pairs.size());
        assertEquals("one", pairs.get(0).left());
        assertEquals(1, pairs.get(0).right());
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
    public void testChainedTransformations() {
        Map<Integer, List<String>> result = EntryStream.of(testMap)
                .filterByValue(v -> v <= 4)
                .mapKey(Fn.toUpperCase())
                .flatMapValue(v -> Stream.range(1, v + 1))
                .inverted()
                .groupTo();

        assertEquals(4, result.size());
        assertTrue(result.get(1).containsAll(Arrays.asList("ONE", "TWO", "THREE", "FOUR")));
        assertTrue(result.get(2).containsAll(Arrays.asList("TWO", "THREE", "FOUR")));
        assertTrue(result.get(3).containsAll(Arrays.asList("THREE", "FOUR")));
        assertTrue(result.get(4).contains("FOUR"));
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
    public void testEntries() {
        List<Entry<String, Integer>> entries = EntryStream.of(testMap).entries().toList();
        assertEquals(5, entries.size());
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
    public void testIfEmpty() {
        final AtomicInteger counter = new AtomicInteger(0);
        EntryStream.empty().ifEmpty(counter::incrementAndGet).count();
        assertEquals(1, counter.get());

        EntryStream.of(N.asMap("a", 1)).ifEmpty(counter::incrementAndGet);
        assertEquals(1, counter.get());
    }

    @Test
    public void testIfEmpty_FromBase() {
        AtomicBoolean called = new AtomicBoolean(false);
        EntryStream.<String, Integer> empty().ifEmpty(() -> called.set(true)).count();
        assertTrue(called.get());
    }

    @Test
    public void testIntermediateOperationState() {
        EntryStream<String, Integer> base = EntryStream.of(testMap);

        EntryStream<String, Integer> filtered1 = base.filter(e -> e.getValue() > 2);

        assertEquals(3, filtered1.count());
    }

    @Test
    public void testInversed() {
        Map<Integer, String> inversed = EntryStream.of(testMap).inverted().toMap();
        assertEquals("one", inversed.get(1));
        assertEquals("two", inversed.get(2));
    }

    @Test
    public void testInvert_Basic() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Map<Integer, String> result = EntryStream.of(map).inverted().toMap();
        assertEquals("a", result.get(1));
        assertEquals("b", result.get(2));
    }

    @Test
    public void testinvertToDisposableEntry() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        EntryStream<String, Integer> stream = EntryStream.of(map);
        Map<Integer, String> inversed = stream.invertedToDisposableEntry().toMap();

        assertEquals(2, inversed.size());
        assertEquals("a", inversed.get(1));
        assertEquals("b", inversed.get(2));
    }

    @Test
    public void testIsParallel() {
        assertFalse(EntryStream.of(testMap).isParallel());
        assertTrue(EntryStream.of(testMap).parallel().isParallel());
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
    public void testIterator_FromBase() {
        ObjIterator<Entry<String, Integer>> iter = EntryStream.of(testMap).iterator();
        assertNotNull(iter);
    }

    // keys

    @Test
    public void testKeys() {
        List<String> keys = EntryStream.of(testMap).keys().toList();
        assertEquals(Arrays.asList("one", "two", "three", "four", "five"), keys);
    }

    @Test
    public void testKeys_FromNonMapBacking() {
        // keys() when _map is null — goes through the stream path
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("x", 10);
        map.put("y", 20);
        List<String> keys = EntryStream.of(map.entrySet()).keys().toList();
        assertEquals(Arrays.asList("x", "y"), keys);
    }

    @Test
    public void testKeys_parallel() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);

        // Use Iterator-based source so the internal _map field is null and parallel dispatch is exercised.
        List<String> keys = EntryStream.<String, Integer> of(map.entrySet().iterator()).parallel().keys().toList();
        Set<String> keySet = new HashSet<>(keys);
        assertEquals(new HashSet<>(Arrays.asList("x", "y", "z")), keySet);
    }



    // SUSPECT #3: keys() / values() must work in both sequential and parallel modes
    // (annotation changed from @SequentialOnly to @ParallelSupported).
    @Test
    public void testKeys_sequential() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);

        List<String> keys = EntryStream.of(map).keys().toList();
        assertEquals(Arrays.asList("x", "y", "z"), keys);
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
    public void testLargeStream() {
        Map<Integer, String> largeMap = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            largeMap.put(i, "value" + i);
        }

        long count = EntryStream.of(largeMap).filter(e -> e.getKey() % 2 == 0).count();

        assertEquals(500, count);
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
    public void testMemoryEfficientOperations() {

        AtomicInteger processCount = new AtomicInteger(0);

        List<Entry<String, Integer>> limited = EntryStream.of(testMap).peek(e -> processCount.incrementAndGet()).limit(2).toList();

        assertEquals(2, limited.size());
        assertEquals(2, processCount.get());
    }

    @Test
    public void testNMatch() {
        Map<String, Integer> map = N.asMap("a", 1, "b", 2, "c", 3, "d", 4);
        assertTrue(EntryStream.of(map).hasMatchCountBetween(2, 2, e -> e.getValue() % 2 == 0));
        assertFalse(EntryStream.of(map).hasMatchCountBetween(3, 3, e -> e.getValue() % 2 == 0));
        assertTrue(EntryStream.of(map).hasMatchCountBetween(1, 3, e -> e.getValue() % 2 == 0));
    }

    @Test
    public void testNMatch_BiPredicate() {
        assertTrue(EntryStream.of(testMap).hasMatchCountBetween(2, 3, (k, v) -> v > 3));
        assertFalse(EntryStream.of(testMap).hasMatchCountBetween(5, 10, (k, v) -> v > 3));
    }

    @Test
    public void testNMatch_Predicate() {
        assertTrue(EntryStream.of(testMap).hasMatchCountBetween(2, 3, e -> e.getValue() > 3));
        assertFalse(EntryStream.of(testMap).hasMatchCountBetween(5, 10, e -> e.getValue() > 3));
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
    public void testNullHandling() {
        Map<String, Integer> mapWithNull = new LinkedHashMap<>();
        mapWithNull.put("a", 1);
        mapWithNull.put("b", null);
        mapWithNull.put("c", 3);

        List<Entry<String, Integer>> result = EntryStream.of(mapWithNull).toList();
        assertEquals(3, result.size());
    }

    // parallel

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
    public void testInvert_Parallel() {
        // invert() parallel branch
        List<Map.Entry<Integer, String>> result = EntryStream.of(testMap).parallel().inverted().toList();
        assertEquals(5, result.size());
        // All entries should have Integer keys (original values) and String values (original keys)
        for (Map.Entry<Integer, String> e : result) {
            assertTrue(e.getKey() instanceof Integer);
            assertTrue(e.getValue() instanceof String);
        }
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
    public void testPrintln() {
        assertDoesNotThrow(() -> {
            EntryStream.of("a", 1, "b", 2).println();
        });
    }

    @Test
    public void testPsp() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).parallel().psp(s -> s.filter(e -> e.getValue() > 2)).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testReusableEntryHashCodeDoesNotMarkEntryAsConsumed() {
        EntryStream.ReusableEntry<String, String> entry = new EntryStream.ReusableEntry<>();
        entry.set("key1", "value1");

        assertEquals("key1".hashCode() ^ "value1".hashCode(), entry.hashCode());
        assertThrows(IllegalStateException.class, () -> entry.set("key2", "value2"));
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
    public void testStreamReuse() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        stream.toList();

        assertThrows(Exception.class, () -> stream.toMap());
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
    public void testThrowIfEmpty() {
        assertThrows(Exception.class, () -> EntryStream.<String, Integer> empty().throwIfEmpty().count());
    }

    @Test
    public void testThrowIfEmpty_WithSupplier() {
        assertThrows(IllegalStateException.class, () -> EntryStream.<String, Integer> empty().throwIfEmpty(() -> new IllegalStateException("Empty")).count());
    }

    // values

    @Test
    public void testValues() {
        List<Integer> values = EntryStream.of(testMap).values().toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), values);
    }

    @Test
    public void testValues_FromNonMapBacking() {
        // values() when _map is null — goes through the stream path
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        List<Integer> vals = EntryStream.of(map.entrySet()).values().toList();
        assertEquals(Arrays.asList(1, 2), vals);
    }

    @Test
    public void testValues_parallel() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);

        List<Integer> values = EntryStream.<String, Integer> of(map.entrySet().iterator()).parallel().values().toList();
        Set<Integer> valueSet = new HashSet<>(values);
        assertEquals(new HashSet<>(Arrays.asList(10, 20, 30)), valueSet);
    }

    @Test
    public void testValues_sequential() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);

        List<Integer> values = EntryStream.of(map).values().toList();
        assertEquals(Arrays.asList(10, 20, 30), values);
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
