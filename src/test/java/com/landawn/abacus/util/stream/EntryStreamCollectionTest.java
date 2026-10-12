package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.function.Supplier;

/** EntryStream collection contracts. */
public class EntryStreamCollectionTest extends EntryStreamTestSupport {

    // collect

    @Test
    public void testCollect() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        List<String> keyList = EntryStream.of(map).keys().collect(Collectors.toList());

        assertEquals(Arrays.asList("a", "b"), keyList);
    }

    @Test
    public void testCollect_Collector() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).collect(java.util.stream.Collectors.toList());
        assertEquals(5, result.size());
    }



    /**
     * {@code EntryStream}'s class javadoc (239-243) promises {@code IllegalArgumentException} naming the
     * parameter, and a closed stream, for a null argument - {@code collect} used to throw a raw NPE.
     * It delegates to {@code Stream.collect}, so this is pinned here as well as on the Stream side.
     */
    @Test
    public void testCollect_nullCollectorThrowsIae() {
        assertThrows(IllegalArgumentException.class, () -> EntryStream.of(N.asMap("a", 1)).collect((Collector<Map.Entry<String, Integer>, ?, ?>) null));
    }

    @Test
    public void testCollect_SupplierAccumulator() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).collect(ArrayList::new, List::add);
        assertEquals(5, result.size());
    }

    @Test
    public void testCollect_SupplierAccumulatorCombiner() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).collect(ArrayList::new, List::add, List::addAll);
        assertEquals(5, result.size());
    }

    @Test
    public void testCollectorsIntegration() {
        Map<String, List<Entry<String, Integer>>> grouped = EntryStream.of(testMap)
                .collect(java.util.stream.Collectors.groupingBy(e -> e.getValue() % 2 == 0 ? "even" : "odd"));

        assertTrue(grouped.containsKey("even"));
        assertTrue(grouped.containsKey("odd"));
    }

    @Test
    public void testCollectWithCollector() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Map<String, Integer> result = stream.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertEquals(testMap.size(), result.size());
        assertEquals(testMap, result);
    }

    @Test
    public void testCollectWithSupplierAccumulator() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        List<Map.Entry<String, Integer>> result = stream.collect(ArrayList::new, List::add);

        assertEquals(testMap.size(), result.size());
    }

    @Test
    public void testCollectWithSupplierAccumulatorCombiner() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        List<Map.Entry<String, Integer>> result = stream.collect(ArrayList::new, List::add, List::addAll);

        assertEquals(testMap.size(), result.size());
    }

    @Test
    public void testCollectThenAccept() {
        AtomicInteger size = new AtomicInteger(0);
        EntryStream.of(testMap).collectThenAccept(java.util.stream.Collectors.toList(), list -> size.set(list.size()));
        assertEquals(5, size.get());
    }

    @Test
    public void testCollectThenAccept_DownstreamAndConsumer() {
        AtomicInteger count = new AtomicInteger(0);
        EntryStream.of(testMap).collectThenAccept(java.util.stream.Collectors.toList(), list -> count.set(list.size()));
        assertEquals(5, count.get());
    }

    // collectThenApply

    @Test
    public void testCollectThenApply() {
        int size = EntryStream.of(testMap).collectThenApply(java.util.stream.Collectors.toList(), List::size);
        assertEquals(5, size);
    }

    @Test
    public void testCollectThenApply_DownstreamAndMapper() {
        List<Integer> values = EntryStream.of(testMap)
                .collectThenApply(java.util.stream.Collectors.toList(),
                        list -> list.stream().map(Entry::getValue).collect(java.util.stream.Collectors.toList()));
        assertEquals(5, values.size());
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

    // groupBy

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
    public void testGroupBy_BinaryOperator_Parallel() {
        // groupBy(BinaryOperator) parallel branch
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 3).parallel().groupBy(Integer::sum).toMap();
        assertEquals(4, (int) result.get("a"));
        assertEquals(2, (int) result.get("b"));
    }

    @Test
    public void testGroupBy_BinaryOperator_Sequential() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 3, "b", 4).groupBy(Integer::sum).toMap();
        assertEquals(4, (int) result.get("a"));
        assertEquals(6, (int) result.get("b"));
    }

    @Test
    public void testGroupBy_BinaryOperatorAndSupplier_Parallel() {
        // groupBy(BinaryOperator, Supplier) parallel branch
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 3).parallel().groupBy(Integer::sum, TreeMap::new).toMap();
        assertEquals(2, result.size());
        assertEquals(4, (int) result.get("a"));
    }

    @Test
    public void testGroupBy_BinaryOperatorAndSupplier_Sequential() {
        TreeMap<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 3)
                .groupBy(Integer::sum, (java.util.function.Supplier<Map<String, Integer>>) TreeMap::new)
                .toMap(TreeMap::new);
        assertEquals(4, (int) result.get("a"));
        assertEquals(2, (int) result.get("b"));
    }

    @Test
    public void testGroupBy_Function_Collector_Supplier() {
        // groupBy(keyMapper, downstream, mapFactory) - L2023 branch
        Map<String, Long> result = EntryStream.of(testMap)
                .groupBy(e -> e.getKey().length() > 3 ? "long" : "short", java.util.stream.Collectors.counting(), TreeMap::new)
                .toMap();
        assertNotNull(result);
        assertTrue(result.containsKey("long") || result.containsKey("short"));
    }

    @Test
    public void testGroupBy_MergeFunctionWithMapSupplier_ParallelBranch() {
        Map<String, Integer> result = EntryStream.of("a", 1, "a", 2, "b", 3).parallel().groupBy(Integer::sum, java.util.LinkedHashMap::new).toMap();

        assertEquals(Arrays.asList("a", "b"), new ArrayList<>(result.keySet()));
        assertEquals(3, result.get("a").intValue());
        assertEquals(3, result.get("b").intValue());
    }

    @Test
    public void testGroupBy_MergeFuncWithMapSupplier() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 3).groupBy(Integer::sum, (Supplier<Map<String, Integer>>) LinkedHashMap::new).toMap();
        assertEquals(4, (int) result.get("a"));
        assertEquals(2, (int) result.get("b"));
    }

    @Test
    public void testGroupBy_NoArg_Parallel() {
        // groupBy() parallel branch
        Map<String, List<Integer>> result = EntryStream.of("a", 1, "b", 2, "a", 3).parallel().groupBy().toMap();
        assertEquals(2, result.size());
        assertTrue(result.get("a").contains(1));
        assertTrue(result.get("a").contains(3));
    }

    @Test
    public void testGroupBy_NoArg_ParallelBranch() {
        Map<String, List<Integer>> result = EntryStream.of("a", 1, "a", 2, "b", 3).parallel().groupBy().toMap();

        assertEquals(Arrays.asList(1, 2), result.get("a"));
        assertEquals(Arrays.asList(3), result.get("b"));
    }



    // Tests for uncovered paths
    @Test
    public void testGroupBy_NoArg_Sequential() {
        Map<String, List<Integer>> result = EntryStream.of("a", 1, "b", 2, "a", 3, "b", 4).groupBy().toMap();
        assertEquals(2, result.size());
        assertEquals(2, result.get("a").size());
        assertEquals(2, result.get("b").size());
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
    public void testGroupBy_WithKeyValueMappersAndMergeAndMapFactory() {
        // groupBy(keyMapper, valueMapper, mergeFunction, mapFactory) - L2172 branch
        Map<String, Integer> result = EntryStream.of(testMap).groupBy(e -> e.getKey().substring(0, 1), e -> e.getValue(), Integer::sum, TreeMap::new).toMap();
        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    @Test
    public void testGroupBy_WithMapSupplier() {
        Map<String, List<Integer>> result = EntryStream.of("a", 1, "b", 2, "a", 3).groupBy((Supplier<Map<String, List<Integer>>>) LinkedHashMap::new).toMap();
        assertEquals(2, result.size());
        assertTrue(result.get("a").contains(1));
        assertTrue(result.get("a").contains(3));
    }

    @Test
    public void testGroupBy_WithMapSupplier_Parallel() {
        // groupBy(Supplier) parallel branch
        Map<String, List<Integer>> result = EntryStream.of("a", 1, "b", 2, "a", 3).parallel().groupBy(TreeMap::new).toMap();
        assertEquals(2, result.size());
        assertTrue(result.get("a").contains(1));
    }

    @Test
    public void testGroupBy_WithMapSupplier_Sequential() {
        TreeMap<String, List<Integer>> result = EntryStream.of("a", 1, "b", 2, "a", 3)
                .groupBy((java.util.function.Supplier<Map<String, List<Integer>>>) TreeMap::new)
                .toMap(TreeMap::new);
        assertEquals(2, result.size());
        assertTrue(result.get("a").contains(1));
        assertTrue(result.get("a").contains(3));
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
    public void testGroupByEdgeCases() {
        Map<String, List<Integer>> result = EntryStream.of("a", 1).groupBy().toMap();

        assertEquals(1, result.size());
        assertEquals(Arrays.asList(1), result.get("a"));
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
    public void testGroupBy_CollectorWithMapSupplier() {
        Collector<Map.Entry<String, Integer>, ?, Long> countingCollector = Collectors.counting();
        Map<String, Long> result = EntryStream.of("a", 1, "b", 2, "a", 3).<Long> groupBy(countingCollector, LinkedHashMap::new).toMap();
        assertEquals(2L, (long) result.get("a"));
        assertEquals(1L, (long) result.get("b"));
    }

    @Test
    public void testGroupBy_KeyValueFuncWithMapSupplier() {
        Map<String, List<Integer>> result = EntryStream.of("a", 1, "b", 2, "a", 3)
                .<String, Integer> groupBy(e -> e.getKey(), e -> e.getValue(), (Supplier<Map<String, List<Integer>>>) LinkedHashMap::new)
                .toMap();
        assertEquals(2, result.size());
    }

    // groupTo

    @Test
    public void testGroupTo() {
        Map<String, List<Integer>> result = EntryStream.of("a", 1, "a", 2, "b", 3).groupTo();
        assertEquals(2, result.get("a").size());
        assertEquals(Arrays.asList(1, 2), result.get("a"));
    }

    @Test
    public void testGroupTo_Parallel() {
        // groupTo() parallel branch
        Map<String, List<Integer>> result = EntryStream.of("a", 1, "b", 2, "a", 3).parallel().groupTo();
        assertEquals(Arrays.asList(1, 3), result.get("a"));
        assertEquals(Arrays.asList(2), result.get("b"));
    }

    @Test
    public void testGroupTo_Supplier_Parallel() {
        // groupTo(Supplier) parallel branch
        TreeMap<String, List<Integer>> result = EntryStream.of("a", 1, "b", 2, "a", 3).parallel().groupTo(TreeMap::new);
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 3), result.get("a"));
    }

    @Test
    public void testGroupTo_WithSupplier() {
        Map<String, List<Integer>> result = EntryStream.of("a", 1, "a", 2, "b", 3).groupTo(LinkedHashMap::new);
        assertEquals(2, result.size());
        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testGroupTo_WithSupplier_Basic() {
        TreeMap<String, List<Integer>> result = EntryStream.of("a", 1, "b", 2, "a", 3)
                .groupTo((java.util.function.Supplier<TreeMap<String, List<Integer>>>) TreeMap::new);
        assertEquals(2, result.size());
        assertTrue(result.get("a").contains(1));
        assertTrue(result.get("a").contains(3));
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
    public void testGroupToWithMapFactory() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("a", 3));

        EntryStream<String, Integer> stream = EntryStream.of(entries);
        TreeMap<String, List<Integer>> grouped = stream.groupTo(TreeMap::new);

        assertTrue(grouped instanceof TreeMap);
        assertEquals(2, grouped.size());
    }

    @Test
    public void testGroupToThenAccept() {
        AtomicInteger totalSize = new AtomicInteger(0);
        EntryStream.of("a", 1, "a", 2, "b", 3).groupToThenAccept(map -> map.values().forEach(list -> totalSize.addAndGet(list.size())));
        assertEquals(3, totalSize.get());
    }

    @Test
    public void testGroupToThenApply() {
        int totalSize = EntryStream.of("a", 1, "a", 2, "b", 3, "b", 4).groupToThenApply(map -> map.values().stream().mapToInt(List::size).sum());
        assertEquals(4, totalSize);
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
    public void testToArray() {
        Object[] result = EntryStream.of(testMap).toArray();
        assertEquals(5, result.length);
    }

    @Test
    public void testToArray_FromEntryStream() {
        Object[] result = EntryStream.of(testMap).toArray();
        assertEquals(5, result.length);
        assertTrue(result[0] instanceof Entry);
    }

    @Test
    public void testToCollection() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        LinkedHashSet<Map.Entry<String, Integer>> collection = stream.toCollection(LinkedHashSet::new);

        assertEquals(testMap.size(), collection.size());
        assertTrue(collection instanceof LinkedHashSet);
    }

    @Test
    public void testToCollection_FromBase() {
        Collection<Entry<String, Integer>> result = EntryStream.of(testMap).toCollection(ArrayList::new);
        assertEquals(5, result.size());
    }

    @Test
    public void testToImmutableList() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).toImmutableList();
        assertEquals(5, result.size());
        assertThrows(UnsupportedOperationException.class, () -> result.add(new AbstractMap.SimpleEntry<>("six", 6)));
    }

    // toImmutableMap

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
    public void testToImmutableMapWithDuplicateKeys() {
        final Map<String, Integer> map = N.asMap("a", 1, "b", 2);
        final Stream<Map.Entry<String, Integer>> streamWithDuplicates = Stream.of(N.newEntry("a", 3)).append(map.entrySet());
        assertThrows(IllegalStateException.class, () -> streamWithDuplicates.mapToEntry(Fn.identity()).toImmutableMap());
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
    public void testToImmutableSet() {
        Set<Entry<String, Integer>> result = EntryStream.of(testMap).toImmutableSet();
        assertEquals(5, result.size());
        assertThrows(UnsupportedOperationException.class, () -> result.add(new AbstractMap.SimpleEntry<>("six", 6)));
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
    public void testToList_FromBase() {
        List<Entry<String, Integer>> result = EntryStream.of(testMap).toList();
        assertEquals(5, result.size());
    }

    // toMap

    @Test
    public void testToMap() {
        Map<String, Integer> result = EntryStream.of(testMap).toMap();
        assertEquals(5, result.size());
        assertEquals(1, result.get("one"));
    }

    @Test
    public void testToMap_BinaryOperator_Basic() {
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 3).toMap(Integer::sum);
        assertEquals(4, (int) result.get("a"));
        assertEquals(2, (int) result.get("b"));
    }

    @Test
    public void testToMap_BinaryOperator_Parallel() {
        // toMap(BinaryOperator) parallel branch
        Map<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 3).parallel().toMap(Integer::sum);
        assertEquals(4, (int) result.get("a"));
        assertEquals(2, (int) result.get("b"));
    }

    @Test
    public void testToMap_BinaryOperatorAndSupplier_Basic() {
        TreeMap<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 3)
                .toMap(Integer::sum, (java.util.function.Supplier<TreeMap<String, Integer>>) TreeMap::new);
        assertEquals(4, (int) result.get("a"));
        assertEquals("a", result.firstKey());
    }

    @Test
    public void testToMap_BinaryOperatorAndSupplier_Parallel() {
        // toMap(BinaryOperator, Supplier) parallel branch
        TreeMap<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 3).parallel().toMap(Integer::sum, TreeMap::new);
        assertEquals(2, result.size());
        assertEquals(4, (int) result.get("a"));
    }

    @Test
    public void testToMap_Supplier_Basic() {
        TreeMap<String, Integer> result = EntryStream.of("b", 2, "a", 1, "c", 3).toMap((java.util.function.Supplier<TreeMap<String, Integer>>) TreeMap::new);
        assertEquals(3, result.size());
        // TreeMap is sorted
        assertEquals("a", result.firstKey());
    }

    @Test
    public void testToMap_Supplier_Parallel() {
        // toMap(Supplier) parallel branch
        TreeMap<String, Integer> result = EntryStream.of(testMap).parallel().toMap(TreeMap::new);
        assertEquals(5, result.size());
        assertEquals(1, (int) result.get("one"));
        // TreeMap should be sorted
        assertEquals("five", result.firstKey());
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
    public void testToMapWithMapFactory() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        TreeMap<String, Integer> map = stream.toMap(TreeMap::new);

        assertTrue(map instanceof TreeMap);
        assertEquals(testMap.size(), map.size());
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
    public void testToMapThenAccept() {
        AtomicInteger sum = new AtomicInteger(0);
        EntryStream.of(testMap).toMapThenAccept(map -> map.values().forEach(sum::addAndGet));
        assertEquals(15, sum.get());
    }

    @Test
    public void testToMapThenApply() {
        int sum = EntryStream.of(testMap).toMapThenApply(map -> map.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(15, sum);
    }

    @Test
    public void testToMapThenApplyAccept() {
        int sum = EntryStream.of("a", 1, "b", 2, "c", 3).toMapThenApply(map -> map.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(6, sum);

        AtomicInteger result = new AtomicInteger(0);
        EntryStream.of("a", 1, "b", 2, "c", 3).toMapThenAccept(map -> result.set(map.size()));
        assertEquals(3, result.get());
    }

    // toMultimap

    @Test
    public void testToMultimap() {
        ListMultimap<String, Integer> result = EntryStream.of("a", 1, "a", 2, "b", 3).toMultimap();
        assertEquals(2, result.get("a").size());
    }

    @Test
    public void testToMultimap_Parallel() {
        // toMultimap() parallel branch
        ListMultimap<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 3).parallel().toMultimap();
        assertEquals(Arrays.asList(1, 3), result.get("a"));
        assertEquals(Arrays.asList(2), result.get("b"));
    }

    @Test
    public void testToMultimap_Supplier_Parallel() {
        // toMultimap(Supplier) parallel branch
        Multimap<String, Integer, List<Integer>> result = EntryStream.of("a", 1, "b", 2, "a", 3).parallel().toMultimap(N::newListMultimap);
        assertTrue(result.get("a").contains(1));
        assertTrue(result.get("a").contains(3));
    }

    @Test
    public void testToMultimap_WithSupplier() {
        ListMultimap<String, Integer> result = EntryStream.of("a", 1, "a", 2).toMultimap(N::newListMultimap);
        assertEquals(2, result.get("a").size());
    }

    @Test
    public void testToMultimap_WithSupplier_Basic() {
        // toMultimap(Supplier<Multimap>) - use N::newListMultimap as the factory
        com.landawn.abacus.util.ListMultimap<String, Integer> result = EntryStream.of("a", 1, "b", 2, "a", 3).toMultimap(N::newListMultimap);
        assertEquals(2, result.size());
        assertTrue(result.get("a").contains(1));
        assertTrue(result.get("a").contains(3));
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
    public void testToMultimapWithMapFactory() {
        Multimap<String, Integer, Set<Integer>> setMultimap = EntryStream.of("a", 1, "b", 2, "a", 1, "a", 3, "b", 2).toMultimap(Suppliers.ofSetMultimap());

        assertEquals(2, setMultimap.get("a").size());
        assertEquals(1, setMultimap.get("b").size());
    }

    @Test
    public void testToMultimapWithSupplier() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("a", 3));

        EntryStream<String, Integer> stream = EntryStream.of(entries);
        ListMultimap<String, Integer> multimap = stream.toMultimap(Suppliers.ofListMultimap());

        assertEquals(2, multimap.get("a").size());
    }

    // toMultiset

    @Test
    public void testToMultiset() {
        Multiset<Entry<String, Integer>> result = EntryStream.of("a", 1, "a", 1, "b", 2).toMultiset();
        assertEquals(2, result.getCount(new AbstractMap.SimpleEntry<>("a", 1)));
    }

    @Test
    public void testToMultiset_FromBase() {
        Multiset<Entry<String, Integer>> result = EntryStream.of("a", 1, "a", 1).toMultiset();
        assertNotNull(result);
    }

    @Test
    public void testToMultiset_WithSupplier() {
        Multiset<Entry<String, Integer>> result = EntryStream.of("a", 1, "a", 1).toMultiset(Multiset::new);
        assertEquals(2, result.getCount(new AbstractMap.SimpleEntry<>("a", 1)));
    }

    @Test
    public void testToMultisetWithSupplier() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Multiset<Map.Entry<String, Integer>> multiset = stream.toMultiset(Multiset::new);

        assertEquals(testMap.size(), multiset.size());
    }

    @Test
    public void testToSet() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        Set<Map.Entry<String, Integer>> set = stream.toSet();

        assertEquals(testMap.size(), set.size());
    }

    @Test
    public void testToSet_FromBase() {
        Set<Entry<String, Integer>> result = EntryStream.of(testMap).toSet();
        assertEquals(5, result.size());
    }

}
