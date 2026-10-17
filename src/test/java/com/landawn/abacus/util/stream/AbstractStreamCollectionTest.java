package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Suppliers;

/** AbstractStream collection contracts. */
public class AbstractStreamCollectionTest extends AbstractStreamTestSupport {

    // collect

    @Test
    public void test_collect() {
        List<Integer> result = Stream.of(1, 2, 3).collect(Collectors.toList());
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    public void test_collect_withSupplierAccumulatorAndCombiner() {
        Set<Integer> result = Stream.of(1, 2, 3).parallel().collect(HashSet::new, Set::add, Set::addAll);
        assertEquals(Set.of(1, 2, 3), result);
    }

    @Test
    public void test_collect_withSupplierAndAccumulator() {
        List<Integer> result = Stream.of(1, 2, 3).collect(ArrayList::new, ArrayList::add);
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    public void test_collectThenAccept() {
        AtomicInteger count = new AtomicInteger();
        Stream.of(1, 2, 3).collectThenAccept(Collectors.toList(), list -> count.set(list.size()));
        assertEquals(3, count.get());
    }

    @Test
    public void testCollectThenAccept() {
        stream = createStream(1, 2, 3);
        List<Integer> result = new ArrayList<>();
        stream.collectThenAccept(Collectors.toList(), list -> result.addAll(list));
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void test_collectThenApply() {
        int result = Stream.of(1, 2, 3).collectThenApply(Collectors.toList(), List::size);
        assertEquals(3, result);
    }

    @Test
    public void testCollectThenApply() {
        stream = createStream(1, 2, 3);
        String result = stream.collectThenApply(Collectors.toList(), list -> "Size: " + list.size());
        assertEquals("Size: 3", result);
    }

    // groupBy

    //
    //
    //
    @Test
    public void testGroupBy() {
        stream = createStream(1, 2, 3, 4, 5, 6);
        Stream<Map.Entry<Integer, List<Integer>>> result = stream.groupBy(i -> i % 2);
        Map<Integer, List<Integer>> map = result.toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(Arrays.asList(2, 4, 6), map.get(0));
        assertEquals(Arrays.asList(1, 3, 5), map.get(1));
    }

    @Test
    public void testGroupByWithCollector() {
        stream = createStream(1, 2, 3, 4, 5, 6);
        Stream<Map.Entry<Integer, Long>> result = stream.groupBy(i -> i % 2, Collectors.counting());
        Map<Integer, Long> map = result.toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(3L, map.get(0));
        assertEquals(3L, map.get(1));
    }

    @Test
    public void testGroupByWithMapFactory() {
        stream = createStream(1, 2, 3, 4);
        Stream<Map.Entry<Integer, List<Integer>>> result = stream.groupBy(i -> i % 2, Suppliers.ofTreeMap());
        assertTrue(result.toList().size() > 0);
    }

    @Test
    public void testGroupByWithMergeFunction() {
        stream = createStream(1, 2, 3, 4, 5, 6);
        Stream<Map.Entry<Integer, Integer>> result = stream.groupBy(i -> i % 2, Fn.identity(), Integer::sum);
        Map<Integer, Integer> map = result.toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(12, map.get(0));
        assertEquals(9, map.get(1));
    }

    @Test
    public void testGroupByWithValueMapper() {
        stream = createStream(1, 2, 3, 4);
        Stream<Map.Entry<Integer, List<String>>> result = stream.groupBy(i -> i % 2, i -> "val" + i);
        Map<Integer, List<String>> map = result.toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(Arrays.asList("val2", "val4"), map.get(0));
    }

    @Test
    public void test_groupBy_keyAndValueMappers() {
        Map<Integer, List<String>> result = Stream.of(1, 2, 3, 4, 5).groupByToEntry(i -> i % 2, String::valueOf).toMap();
        assertEquals(Map.of(0, Arrays.asList("2", "4"), 1, Arrays.asList("1", "3", "5")), result);
    }

    //
    @Test
    public void test_groupBy_keyMapper() {
        Map<Integer, List<Integer>> result = Stream.of(1, 2, 3, 4, 5).groupByToEntry(i -> i % 2).toMap();
        assertEquals(Map.of(0, Arrays.asList(2, 4), 1, Arrays.asList(1, 3, 5)), result);
    }

    @Test
    public void test_groupBy_keyMapperAndCollector() {
        Map<Integer, Long> result = Stream.of(1, 2, 3, 4, 5).groupByToEntry(i -> i % 2, Collectors.counting()).toMap();
        assertEquals(Map.of(0, 2L, 1, 3L), result);
    }

    @Test
    public void testGroupByToEntry() {
        {
            stream = createStream(1, 2, 3, 4);
            EntryStream<Integer, List<Integer>> result = stream.groupByToEntry(i -> i % 2);
            Map<Integer, List<Integer>> map = result.toMap();
            assertEquals(Arrays.asList(2, 4), map.get(0));
            assertEquals(Arrays.asList(1, 3), map.get(1));
        }
        {
            stream = createStream(1, 2, 3, 4);
            EntryStream<Integer, List<Integer>> result = stream.groupByToEntry(i -> i % 2, Fn.identity(), Collectors.toList());
            Map<Integer, List<Integer>> map = result.toMap();
            assertEquals(Arrays.asList(2, 4), map.get(0));
            assertEquals(Arrays.asList(1, 3), map.get(1));
        }
    }

    // groupJoin

    @Test
    public void testGroupJoin() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, List<String>>> result = stream.groupJoin(Arrays.asList("1a", "1b", "2c"), Fn.identity(), s -> Integer.parseInt(s.substring(0, 1)));
        Map<Integer, List<String>> map = result.toMap(Pair::getLeft, Pair::getRight);
        assertEquals(2, map.get(1).size());
        assertEquals(1, map.get(2).size());
        assertEquals(0, map.get(3).size());
    }

    @Test
    public void test_groupJoin() {
        List<Pair<Integer, Long>> result = Stream.of(Pair.of(1, "Group1"), Pair.of(2, "Group2"))
                .groupJoin(List.of(Pair.of(1, 10), Pair.of(1, 20), Pair.of(2, 30)), p -> p.left(), p -> p.left(),
                        Collectors.summingInt(p -> (Integer) p.right()), (p1, sum) -> Pair.of(p1.left(), sum.longValue()))
                .toList();
        assertEquals(List.of(Pair.of(1, 30L), Pair.of(2, 30L)), result);
    }

    @Test
    public void testGroupJoin_2() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "Order1"), new TestOrder(1, "Order2"), new TestOrder(2, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, List<TestOrder>>> result = stream.groupJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, Pair::of).toList();

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).right().size());
        assertEquals(1, result.get(1).right().size());
    }

    @Test
    public void testGroupJoin_3() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "Order1"), new TestOrder(1, "Order2"), new TestOrder(2, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, TestOrder>> result = stream.groupJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, (a, b) -> a, Pair::of)
                .toList();

        assertEquals(2, result.size());
        assertEquals("John", result.get(0).left().getName());
        assertEquals("Order1", result.get(0).right().getOrderName());
        assertEquals("Jane", result.get(1).left().getName());
        assertEquals("Order3", result.get(1).right().getOrderName());

    }

    @Test
    public void testGroupJoin_4() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "Order1"), new TestOrder(1, "Order2"), new TestOrder(2, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, List<TestOrder>>> result = stream
                .groupJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, Collectors.toList(), Pair::of)
                .toList();

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).right().size());
        assertEquals(1, result.get(1).right().size());
    }

    @Test
    public void testGroupJoinPreservesNullDownstreamResult() {
        final Collector<Integer, List<Integer>, String> downstream = Collector.of(ArrayList::new, List::add, (left, right) -> {
            left.addAll(right);
            return left;
        }, values -> values.isEmpty() ? "empty" : null);

        List<Pair<Integer, String>> result = Stream.of(1, 2).groupJoin(List.of(1), Fn.identity(), Fn.identity(), downstream, Pair::of).toList();

        assertEquals(Pair.of(1, null), result.get(0));
        assertEquals(Pair.of(2, "empty"), result.get(1));

        result = Stream.of(1, 2).groupJoin(Stream.of(1), Fn.identity(), Fn.identity(), downstream, Pair::of).toList();

        assertEquals(Pair.of(1, null), result.get(0));
        assertEquals(Pair.of(2, "empty"), result.get(1));
    }

    @Test
    public void testGroupJoinSameType() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, List<Integer>>> result = stream.groupJoin(Arrays.asList(1, 1, 2, 2, 2), Fn.identity());
        Map<Integer, List<Integer>> map = result.toMap(Pair::getLeft, Pair::getRight);
        assertEquals(2, map.get(1).size());
        assertEquals(3, map.get(2).size());
    }

    @Test
    public void testGroupJoinWithCollector() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, Long>> result = stream.groupJoin(Arrays.asList(1, 1, 2, 2, 2), Fn.identity(), Fn.identity(), Collectors.counting());
        Map<Integer, Long> map = result.toMap(Pair::getLeft, Pair::getRight);
        assertEquals(2L, (long) map.get(1));
        assertEquals(3L, (long) map.get(2));
    }

    @Test
    public void testGroupJoinWithDownstream() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "100"), new TestOrder(1, "200"), new TestOrder(2, "300"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, Long>> result = stream.groupJoin(orders, TestPerson::getId, TestOrder::getPersonId, Collectors.counting()).toList();

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).right().longValue());
        assertEquals(1L, result.get(1).right().longValue());
    }

    @Test
    public void testGroupJoinWithMergeFunction() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, Integer>> result = stream.groupJoin(Arrays.asList(1, 1, 2, 2, 2), Fn.identity(), Fn.identity(), Integer::sum);
        Map<Integer, Integer> map = result.toMap(Pair::getLeft, Pair::getRight);
        assertEquals(2, (int) map.get(1));
        assertEquals(6, (int) map.get(2));
    }

    // groupTo

    @Test
    public void testGroupTo() {
        stream = createStream(1, 2, 3, 4);
        Map<Integer, List<Integer>> result = stream.groupTo(i -> i % 2);
        assertEquals(Arrays.asList(2, 4), result.get(0));
        assertEquals(Arrays.asList(1, 3), result.get(1));
    }

    @Test
    public void testGroupToWithCollector() {
        stream = createStream(1, 2, 3, 4, 5, 6);
        Map<Integer, Long> result = stream.groupTo(i -> i % 2, Collectors.counting());
        assertEquals(3L, result.get(0));
        assertEquals(3L, result.get(1));
    }

    @Test
    public void testGroupToWithMapFactory() {
        stream = createStream(1, 2, 3, 4);
        TreeMap<Integer, List<Integer>> result = stream.groupTo(i -> i % 2, Suppliers.ofTreeMap());
        assertEquals(Arrays.asList(2, 4), result.get(0));
    }

    @Test
    public void testGroupToWithValueMapper() {
        stream = createStream(1, 2, 3, 4);
        Map<Integer, List<String>> result = stream.groupTo(i -> i % 2, i -> "val" + i);
        assertEquals(Arrays.asList("val2", "val4"), result.get(0));
    }

    // partitionBy

    @Test
    public void testPartitionBy() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Map.Entry<Boolean, List<Integer>>> result = stream.partitionBy(i -> i % 2 == 0);
        Map<Boolean, List<Integer>> map = result.toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(Arrays.asList(2, 4), map.get(true));
        assertEquals(Arrays.asList(1, 3, 5), map.get(false));
    }

    @Test
    public void testPartitionByRejectsNullDownstreamBeforeTraversal() {
        final AtomicInteger predicateCalls = new AtomicInteger();
        try (final Stream<Integer> stream = Stream.of(1, 2)) {
            assertThrows(IllegalArgumentException.class, () -> stream.partitionBy(value -> {
                predicateCalls.incrementAndGet();
                return value == 2;
            }, null));
            assertEquals(0, predicateCalls.get());
        }
    }

    @Test
    public void testPartitionByWithCollector() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Map.Entry<Boolean, Long>> result = stream.partitionBy(i -> i % 2 == 0, Collectors.counting());
        Map<Boolean, Long> map = result.toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(2L, map.get(true));
        assertEquals(3L, map.get(false));
    }

    @Test
    public void test_partitionBy_predicate() {
        Map<Boolean, List<Integer>> result = Stream.of(1, 2, 3, 4, 5).partitionByToEntry(i -> i % 2 == 0).toMap();
        assertEquals(Map.of(true, Arrays.asList(2, 4), false, Arrays.asList(1, 3, 5)), result);
    }

    @Test
    public void test_partitionBy_predicateAndCollector() {
        Map<Boolean, Long> result = Stream.of(1, 2, 3, 4, 5).partitionByToEntry(i -> i % 2 == 0, Collectors.counting()).toMap();
        assertEquals(Map.of(true, 2L, false, 3L), result);
    }

    @Test
    public void testPartitionByToEntry() {
        stream = createStream(1, 2, 3, 4, 5);
        EntryStream<Boolean, List<Integer>> result = stream.partitionByToEntry(i -> i % 2 == 0);
        Map<Boolean, List<Integer>> map = result.toMap();
        assertEquals(Arrays.asList(2, 4), map.get(true));
        assertEquals(Arrays.asList(1, 3, 5), map.get(false));
    }

    @Test
    public void testPartitionTo() {
        stream = createStream(1, 2, 3, 4, 5);
        Map<Boolean, List<Integer>> result = stream.partitionTo(i -> i % 2 == 0);
        assertEquals(Arrays.asList(2, 4), result.get(true));
        assertEquals(Arrays.asList(1, 3, 5), result.get(false));
    }

    @Test
    public void testPartitionToWithCollector() {
        stream = createStream(1, 2, 3, 4, 5);
        Map<Boolean, Long> result = stream.partitionTo(i -> i % 2 == 0, Collectors.counting());
        assertEquals(2L, result.get(true));
        assertEquals(3L, result.get(false));
    }

    // rollup

    @Test
    public void test_rollup() {
        List<List<Integer>> result = Stream.of(1, 2, 3).rollup().toList();
        assertEquals(List.of(List.of(), List.of(1), List.of(1, 2), List.of(1, 2, 3)), result);
    }

    @Test
    public void testRollup() {
        stream = createStream(1, 2, 3);
        Stream<List<Integer>> result = stream.rollup();
        List<List<Integer>> lists = result.toList();
        assertEquals(4, lists.size());
        assertEquals(Arrays.asList(), lists.get(0));
        assertEquals(Arrays.asList(1), lists.get(1));
        assertEquals(Arrays.asList(1, 2), lists.get(2));
        assertEquals(Arrays.asList(1, 2, 3), lists.get(3));
    }

    // Stream review 2026-09-09 (pass B) - rollup / forEachUntil / splitAt

    /**
     * {@code rollup()} used to emit {@code Arrays.asList(...).subList(...)} views over the array returned by
     * {@code arrayForIntermediateOp()}, which for an array-backed stream is the CALLER's own array (and which
     * {@code StreamBase} documents as read-only). So {@code set(...)} on an emitted list wrote through into the
     * caller's array, and every emitted list was a mutable view of one shared buffer.
     */
    @Test
    public void testRollup_emittedListsAreIsolatedFromTheCallersArray() {
        final String[] arr = { "a", "b", "c", "d" };
        final List<List<String>> out = Stream.of(arr).rollup().toList();

        assertEquals(5, out.size());
        assertEquals(N.asList("a", "b", "c", "d"), out.get(4));

        // used to rewrite arr[0] to "PWNED", and every other emitted list with it
        assertThrows(UnsupportedOperationException.class, () -> out.get(3).set(0, "PWNED"));
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, arr);

        // and mutating the caller's array used to retroactively change already-collected lists
        arr[1] = "MUTATED";
        assertEquals(N.asList("a", "b"), out.get(2));
        assertEquals(N.asList("a", "b", "c"), out.get(3));
    }

    /**
     * The iterator-backed path never touched the caller's array, but every emitted list was still a view over
     * one shared buffer, so writing into one corrupted the others.
     */
    @Test
    public void testRollup_emittedListsDoNotAliasEachOther() {
        final List<List<String>> out = Stream.of(Arrays.asList("a", "b", "c", "d").iterator()).rollup().toList();

        assertThrows(UnsupportedOperationException.class, () -> out.get(3).set(0, "PWNED"));
        assertEquals(N.asList("a"), out.get(1));
        assertEquals(N.asList("a", "b", "c", "d"), out.get(4));
    }

    @Test
    public void testToArray() {
        stream = createStream(1, 2, 3);
        Integer[] result = stream.toArray(Integer[]::new);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, result);
    }

    @Test
    public void test_toArray_generator() {
        Integer[] result = Stream.of(1, 2, 3).toArray(Integer[]::new);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, result);
    }

    @Test
    public void test_toCollection() {
        ArrayList<Integer> result = Stream.of(1, 2, 3).toCollection(ArrayList::new);
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    public void test_toCollectionThenAccept() {
        final List<Integer> holder = new ArrayList<>();
        Stream.of(1, 2, 3).toCollectionThenAccept(ArrayList::new, holder::addAll);
        assertEquals(List.of(1, 2, 3), holder);
    }

    @Test
    public void testToCollectionThenAccept() {
        stream = createStream(1, 2, 3);
        List<Integer> result = new ArrayList<>();
        stream.toCollectionThenAccept(LinkedList::new, result::addAll);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void test_toCollectionThenApply() {
        Integer result = Stream.of(1, 2, 3).toCollectionThenApply(ArrayList::new, Collection::size);
        assertEquals(3, result);
    }

    @Test
    public void testToCollectionThenApply() {
        stream = createStream(1, 2, 3);
        Integer result = stream.toCollectionThenApply(ArrayList::new, Collection::size);
        assertEquals(3, result);
    }

    // toDataset

    @Test
    public void test_toDataset() {
        List<Map<String, Object>> data = List.of(Map.of("id", 1, "name", "A"), Map.of("id", 2, "name", "B"));
        com.landawn.abacus.util.Dataset dataset = Stream.of(data).toDataset();
        assertEquals(2, dataset.size());
        assertTrue(dataset.columnNames().containsAll(List.of("id", "name")));
    }

    @Test
    public void testToDataset() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("id", 1);
        map1.put("value", 100);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("id", 2);
        map2.put("value", 200);

        Stream<Map<String, Integer>> mapStream = createStream(map1, map2);
        Dataset result = mapStream.toDataset();
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void test_toDataset_withColumnNames() {
        List<List<Object>> data = List.of(List.of(1, "A"), List.of(2, "B"));
        com.landawn.abacus.util.Dataset dataset = Stream.of(data).toDataset(List.of("id", "name"));
        assertEquals(2, dataset.size());
        assertEquals(List.of("id", "name"), dataset.columnNames());
    }

    @Test
    public void testToDatasetWithColumnNames() {
        stream = createStream(1, 2, 3);
        Dataset result = stream.toDataset(Arrays.asList("value"));
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void test_toList() {
        List<Integer> result = Stream.of(1, 2, 3).toList();
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    public void test_toListThenAccept() {
        final List<Integer> holder = new ArrayList<>();
        Stream.of(1, 2, 3).toListThenAccept(holder::addAll);
        assertEquals(List.of(1, 2, 3), holder);
    }

    @Test
    public void testToListThenAccept() {
        stream = createStream(1, 2, 3);
        List<Integer> result = new ArrayList<>();
        stream.toListThenAccept(result::addAll);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void test_toListThenApply() {
        Integer result = Stream.of(1, 2, 3).toListThenApply(List::size);
        assertEquals(3, result);
    }

    @Test
    public void testToListThenApply() {
        stream = createStream(1, 2, 3);
        Integer result = stream.toListThenApply(list -> list.size());
        assertEquals(3, result);
    }

    // toMap

    @Test
    public void testToMap() {
        stream = createStream(1, 2, 3);
        Map<Integer, String> result = stream.toMap(Fn.identity(), i -> "val" + i);
        assertEquals("val1", result.get(1));
        assertEquals("val2", result.get(2));
        assertEquals("val3", result.get(3));
    }

    @Test
    public void test_toMap_keyAndValueMappers() {
        Map<Integer, String> result = Stream.of(1, 2, 3).toMap(i -> i, String::valueOf);
        assertEquals(Map.of(1, "1", 2, "2", 3, "3"), result);
    }

    @Test
    public void testToMapWithMapFactory() {
        stream = createStream(1, 2, 3);
        TreeMap<Integer, String> result = stream.toMap(Fn.identity(), i -> "val" + i, TreeMap::new);
        assertEquals("val1", result.get(1));
    }

    @Test
    public void testToMapWithMergeFunction() {
        stream = createStream(1, 1, 2, 2, 3);
        Map<Integer, Integer> result = stream.toMap(Fn.identity(), Fn.identity(), Integer::sum);
        assertEquals(2, result.get(1));
        assertEquals(4, result.get(2));
        assertEquals(3, result.get(3));
    }

    // toMultimap

    @Test
    public void test_toMultimap() {
        ListMultimap<Integer, Integer> result = Stream.of(1, 2, 3, 4).toMultimap(i -> i % 2);
        assertEquals(2, result.get(0).size());
        assertEquals(2, result.get(1).size());
    }

    @Test
    public void testToMultimap() {
        stream = createStream(1, 2, 3, 4);
        ListMultimap<Integer, Integer> result = stream.toMultimap(i -> i % 2);
        assertEquals(Arrays.asList(2, 4), result.get(0));
        assertEquals(Arrays.asList(1, 3), result.get(1));
    }

    @Test
    public void testToMultimapWithMapFactory() {
        stream = createStream(1, 2, 3, 4);
        ListMultimap<Integer, Integer> result = stream.toMultimap(i -> i % 2, Suppliers.ofListMultimap());
        assertEquals(Arrays.asList(2, 4), result.get(0));
    }

    @Test
    public void testToMultimapWithValueMapper() {
        stream = createStream(1, 2, 3, 4);
        ListMultimap<Integer, String> result = stream.toMultimap(i -> i % 2, i -> "val" + i);
        assertEquals(Arrays.asList("val2", "val4"), result.get(0));
    }

    @Test
    public void test_toMultiset() {
        Multiset<Integer> result = Stream.of(1, 2, 1, 3, 2, 1).toMultiset();
        assertEquals(3, result.count(1));
        assertEquals(2, result.count(2));
        assertEquals(1, result.count(3));
    }

    @Test
    public void test_toSet() {
        Set<Integer> result = Stream.of(1, 2, 1).toSet();
        assertEquals(Set.of(1, 2), result);
    }

    @Test
    public void test_toSetThenAccept() {
        final Set<Integer> holder = new HashSet<>();
        Stream.of(1, 2, 1).toSetThenAccept(holder::addAll);
        assertEquals(Set.of(1, 2), holder);
    }

    @Test
    public void testToSetThenAccept() {
        stream = createStream(1, 2, 2, 3);
        Set<Integer> result = new HashSet<>();
        stream.toSetThenAccept(result::addAll);
        assertEquals(3, result.size());
    }

    @Test
    public void test_toSetThenApply() {
        Integer result = Stream.of(1, 2, 1).toSetThenApply(Set::size);
        assertEquals(2, result);
    }

    @Test
    public void testToSetThenApply() {
        stream = createStream(1, 2, 2, 3);
        Integer result = stream.toSetThenApply(Set::size);
        assertEquals(3, result);
    }

}
