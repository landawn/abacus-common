package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Suppliers;

/** Stream collection contracts. */
public class StreamCollectionTest extends StreamTestSupport {

    // collect

    @Test
    public void testCollect() {
        List<Integer> list = Stream.of(1, 2, 3).collect(Collectors.toList());
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testCollectCollector() {
        List<Integer> result = Stream.of(1, 2, 3).collect(java.util.stream.Collectors.toList());
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testCollectSupplierAccumulator() {
        List<Integer> result = Stream.of(1, 2, 3).collect(ArrayList::new, ArrayList::add);
        assertEquals(Arrays.asList(1, 2, 3), result);

        Set<Integer> parallelResult = Stream.of(1, 2, 3, 1, 2).parallel().collect(HashSet::new, HashSet::add);
        assertEquals(Set.of(1, 2, 3), parallelResult);
    }

    //
    //
    @Test
    public void testCollectSupplierAccumulatorCombiner() {
        ArrayList<Integer> result = Stream.of(1, 2, 3).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testCollectThenAccept() {
        List<Integer> holder = new ArrayList<>();
        Stream.of(1, 2, 3).collectThenAccept(java.util.stream.Collectors.toList(), holder::addAll);
        assertEquals(Arrays.asList(1, 2, 3), holder);
    }

    @Test
    public void testCollectThenApply() {
        int result = Stream.of(1, 2, 3).collectThenApply(java.util.stream.Collectors.toList(), List::size);
        assertEquals(3, result);
    }

    // groupBy

    @Test
    public void testGroupBy() {
        Map<Integer, List<String>> map = Stream.of("a", "bb", "c", "dd", "eee").groupBy(String::length).toMap(e -> e.getKey(), e -> e.getValue());
        assertEquals(Arrays.asList("a", "c"), map.get(1));
        assertEquals(Arrays.asList("bb", "dd"), map.get(2));
        assertEquals(Arrays.asList("eee"), map.get(3));
    }

    @Test
    public void testGroupBy_Empty() {
        List<Map.Entry<Integer, List<String>>> result = Stream.<String> empty().groupBy(String::length).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGroupBy_WithDownstream() {
        List<Map.Entry<Integer, Long>> result = Stream.of("a", "bb", "c", "dd", "eee").groupBy(String::length, java.util.stream.Collectors.counting()).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testGroupBy_WithKeyValueAndDownstream() {
        List<Map.Entry<Integer, Long>> result = Stream.of("a", "bb", "c", "dd", "eee")
                .groupBy(String::length, String::toUpperCase, java.util.stream.Collectors.counting())
                .sorted(Map.Entry.comparingByKey())
                .toList();
        assertEquals(3, result.size());
        assertEquals(2L, (long) result.get(0).getValue()); // length=1: "A","C"
    }

    @Test
    public void testGroupByEmpty() {
        Map<Integer, List<String>> map = Stream.<String> empty().groupBy(String::length).toMap(e -> e.getKey(), e -> e.getValue());
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGroupByRejectsNullKeyOnTraversal() {
        final Stream<Map.Entry<Integer, List<String>>> grouped = Stream.of("a", "bb").groupBy(s -> s.length() == 1 ? null : s.length());

        final NullPointerException e = assertThrows(NullPointerException.class, grouped::toList);
        assertEquals("element cannot be mapped to a null key", e.getMessage());
    }

    @Test
    public void testGroupBySimple() {
        Stream<Map.Entry<Integer, List<String>>> resultStream = Stream.of("apple", "banana", "apricot", "blueberry", "cherry").groupBy(String::length);

        Map<Integer, List<String>> resultMap = resultStream.toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(Set.of(5, 6, 7, 9), resultMap.keySet());
        assertTrue(resultMap.get(5).containsAll(Arrays.asList("apple")));
        assertTrue(resultMap.get(6).containsAll(Arrays.asList("banana", "cherry")));
        assertTrue(resultMap.get(7).containsAll(Arrays.asList("apricot")));
        assertTrue(resultMap.get(9).containsAll(Arrays.asList("blueberry")));
    }

    @Test
    public void testGroupByWithCollector() {
        Map<Integer, Long> map = Stream.of("a", "bb", "c", "dd").groupBy(String::length, Collectors.counting()).toMap(e -> e.getKey(), e -> e.getValue());
        assertEquals(Long.valueOf(2), map.get(1));
        assertEquals(Long.valueOf(2), map.get(2));
    }

    @Test
    public void testGroupByWithKeyMapperValueMapperAndMergeFunction() {
        Map<Character, String> result = Stream.of("apple", "apricot", "banana", "blueberry", "avocado")
                .groupBy(s -> s.charAt(0), s -> s.substring(0, Math.min(s.length(), 3)), (v1, v2) -> v1 + ";" + v2)
                .toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals("app;apr;avo", result.get('a'));
        assertEquals("ban;blu", result.get('b'));
    }

    @Test
    public void testGroupByWithValueMapper() {
        Map<Integer, List<Integer>> map = Stream.of("a", "bb", "ccc").groupBy(String::length, String::length).toMap(e -> e.getKey(), e -> e.getValue());
        assertEquals(Arrays.asList(1), map.get(1));
        assertEquals(Arrays.asList(2), map.get(2));
        assertEquals(Arrays.asList(3), map.get(3));
    }

    // groupByToEntry

    @Test
    public void testGroupByToEntry() {
        List<Map.Entry<Integer, List<String>>> list = Stream.of("a", "bb", "c", "dd").groupByToEntry(String::length).toList();
        assertEquals(2, list.size());
    }

    @Test
    public void testGroupByToEntry_WithKeyValueAndMerge() {
        List<Map.Entry<Integer, String>> result = Stream.of("a", "bb", "c", "dd", "eee")
                .groupByToEntry(String::length, s -> s, (s1, s2) -> s1 + "," + s2)
                .sorted(Map.Entry.comparingByKey())
                .toList();
        assertEquals(3, result.size());
        assertTrue(result.get(0).getValue().contains("a"));
    }

    @Test
    public void testGroupByToEntry_WithKeyValueMergeAndMapFactory() {
        List<Map.Entry<Integer, String>> result = Stream.of("a", "bb", "c", "dd")
                .groupByToEntry(String::length, s -> s, (s1, s2) -> s1 + "," + s2, LinkedHashMap::new)
                .toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testGroupByToEntrySimple() {
        EntryStream<Integer, List<String>> resultStream = Stream.of("apple", "banana", "apricot", "blueberry", "cherry").groupByToEntry(String::length);

        Map<Integer, List<String>> resultMap = resultStream.toMap();
        assertEquals(Set.of(5, 6, 7, 9), resultMap.keySet());
        assertTrue(resultMap.get(5).containsAll(Arrays.asList("apple")));
    }

    @Test
    public void testGroupByToEntryWithKeyMapperValueMapperCollectorAndMapFactory() {
        Map<Character, String> result = Stream.of("apple", "apricot", "banana", "blueberry", "avocado")
                .groupByToEntry(s -> s.charAt(0), s -> String.valueOf(s.length()), Collectors.joining(",", "[", "]"), TreeMap::new)
                .toMap();

        assertEquals("[5,7,7]", result.get('a'));
        assertEquals("[6,9]", result.get('b'));
    }

    // groupJoin

    @Test
    public void testGroupJoin() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 1, 2, 3, 3);

        List<Pair<Integer, List<Integer>>> result = Stream.of(list1).groupJoin(list2, n -> n, n -> n).toList();
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0).getLeft());
        assertEquals(2, result.get(0).getRight().size());
    }

    @Test
    public void testGroupJoin_WithCollector() {
        List<Integer> left = Arrays.asList(1, 2);
        List<Integer> right = Arrays.asList(1, 1, 2, 3);
        List<com.landawn.abacus.util.Pair<Integer, Long>> result = Stream.of(left)
                .groupJoin(right, n -> n, java.util.stream.Collectors.counting())
                .sorted((a, b) -> a.left().compareTo(b.left()))
                .toList();
        assertEquals(2, result.size());
        assertEquals(2L, (long) result.get(0).right()); // key=1 appears twice in right
        assertEquals(1L, (long) result.get(1).right()); // key=2 appears once in right
    }

    @Test
    public void testGroupJoin_WithCollectorAndResultMapper() {
        List<Integer> left = Arrays.asList(1, 2);
        List<Integer> right = Arrays.asList(1, 1, 2);
        List<String> result = Stream.of(left).groupJoin(right, n -> n, java.util.stream.Collectors.counting(), (k, v) -> k + "=" + v).sorted().toList();
        assertEquals(2, result.size());
        assertTrue(result.contains("1=2"));
        assertTrue(result.contains("2=1"));
    }

    @Test
    public void testGroupJoinEmpty() {
        List<Integer> list2 = Arrays.asList(1, 2, 3);

        List<Pair<Integer, List<Integer>>> result = Stream.<Integer> empty().groupJoin(list2, n -> n, n -> n).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGroupJoinParallelNoSharedStateRace() {
        // regression: the groupJoin mappers stored the looked-up group in a shared instance field,
        // racing across parallel worker threads and returning another element's (or no) group
        final List<Integer> left = new ArrayList<>();
        for (int i = 0; i < 20000; i++) {
            left.add(i);
        }
        final List<Integer> right = new ArrayList<>();
        for (int i = 0; i < 20000; i += 2) {
            right.add(i);
        }

        final List<Pair<Integer, List<Integer>>> joined = Stream.of(left)
                .parallel(8)
                .groupJoin(right, Fn.identity(), Fn.identity(), (final Integer t, final List<Integer> l) -> Pair.of(t, l))
                .toList();

        assertEquals(20000, joined.size());

        for (final Pair<Integer, List<Integer>> p : joined) {
            if (p.left() % 2 == 0) {
                assertEquals(N.asList(p.left()), p.right());
            } else {
                assertTrue(p.right().isEmpty());
            }
        }
    }

    @Test
    public void testGroupJoinPassesStoredNullRightValueThrough() {
        // An absent key and a key stored with a null value are the same thing to the merge-function
        // overload: both hand 'func' null. A containsKey() probe here would only cost a second lookup.
        final List<String> right = new ArrayList<>();
        right.add(null);

        final List<String> result = Stream.of("k", "missing")
                .groupJoin(right, s -> s, s -> "k", (x, y) -> x, (l, r) -> l + "/" + (r == null ? "<null>" : r))
                .toList();

        assertEquals(Arrays.asList("k/<null>", "missing/<null>"), result);
    }

    @Test
    public void testGroupJoinWithMapper() {
        List<String> list1 = Arrays.asList("a", "bb", "ccc");
        List<Integer> list2 = Arrays.asList(1, 1, 2, 3);

        List<Pair<String, List<Integer>>> result = Stream.of(list1).groupJoin(list2, String::length, n -> n).toList();
        assertEquals(3, result.size());
    }

    // groupTo

    //
    //
    //
    @Test
    public void testGroupTo() {
        Map<Integer, List<String>> map = Stream.of("a", "bb", "c", "dd", "eee").groupTo(String::length);
        assertEquals(2, map.get(1).size());
        assertEquals(2, map.get(2).size());
        assertEquals(1, map.get(3).size());
    }

    @Test
    public void testGroupToEmpty() {
        Map<Integer, List<String>> map = Stream.<String> empty().groupTo(String::length);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGroupToKeyAndValueMapper() {
        Map<Integer, List<String>> map = Stream.of("a", "bb", "c").groupTo(String::length, String::toUpperCase);
        assertEquals(Arrays.asList("A", "C"), map.get(1));
        assertEquals(Arrays.asList("BB"), map.get(2));
    }

    @Test
    public void testGroupToKeyMapper() {
        Map<Integer, List<String>> map = Stream.of("a", "bb", "c", "ddd").groupTo(String::length);
        assertEquals(Arrays.asList("a", "c"), map.get(1));
        assertEquals(Arrays.asList("bb"), map.get(2));
        assertEquals(Arrays.asList("ddd"), map.get(3));
    }

    @Test
    public void testGroupToWithDownstreamCollectorAndMapFactory() {
        TreeMap<Integer, Long> map = Stream.of("a", "bb", "c", "ddd", "ee").groupTo(String::length, Collectors.counting(), TreeMap::new);
        assertTrue(map instanceof TreeMap);
        assertEquals(2L, map.get(1));
        assertEquals(2L, map.get(2));
        assertEquals(1L, map.get(3));
    }

    // partitionBy

    @Test
    public void testPartitionBy() {
        Map<Boolean, List<Integer>> map = Stream.of(1, 2, 3, 4, 5).partitionBy(n -> n % 2 == 0).toMap(e -> e.getKey(), e -> e.getValue());
        assertEquals(Arrays.asList(2, 4), map.get(true));
        assertEquals(Arrays.asList(1, 3, 5), map.get(false));
    }

    @Test
    public void testPartitionBy_WithDownstream() {
        List<Map.Entry<Boolean, Long>> result = Stream.of(1, 2, 3, 4, 5).partitionBy(n -> n % 2 == 0, java.util.stream.Collectors.counting()).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void testPartitionByEmpty() {
        Map<Boolean, List<Integer>> map = Stream.<Integer> empty().partitionBy(n -> n % 2 == 0).toMap(e -> e.getKey(), e -> e.getValue());
        assertTrue(map.get(true).isEmpty());
        assertTrue(map.get(false).isEmpty());
    }

    @Test
    public void testPartitionBySimple() {
        Stream<Map.Entry<Boolean, List<Integer>>> resultStream = Stream.of(1, 2, 3, 4, 5).partitionBy(x -> x % 2 == 0);
        Map<Boolean, List<Integer>> resultMap = resultStream.toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(Arrays.asList(2, 4), resultMap.get(true));
        assertEquals(Arrays.asList(1, 3, 5), resultMap.get(false));
    }

    @Test
    public void testPartitionByWithCollector() {
        Map<Boolean, Long> map = Stream.of(1, 2, 3, 4, 5).partitionBy(n -> n % 2 == 0, Collectors.counting()).toMap(e -> e.getKey(), e -> e.getValue());
        assertEquals(Long.valueOf(2), map.get(true));
        assertEquals(Long.valueOf(3), map.get(false));
    }

    @Test
    public void testPartitionByToEntry() {
        List<Map.Entry<Boolean, List<Integer>>> list = Stream.of(1, 2, 3, 4, 5).partitionByToEntry(n -> n % 2 == 0).toList();
        assertEquals(2, list.size());
    }

    @Test
    public void testPartitionByToEntrySimple() {
        EntryStream<Boolean, List<Integer>> resultStream = Stream.of(1, 2, 3, 4, 5).partitionByToEntry(x -> x % 2 == 0);
        Map<Boolean, List<Integer>> resultMap = resultStream.toMap();

        assertEquals(Arrays.asList(2, 4), resultMap.get(true));
        assertEquals(Arrays.asList(1, 3, 5), resultMap.get(false));
    }

    @Test
    public void testPartitionTo() {
        Map<Boolean, List<Integer>> map = Stream.of(1, 2, 3, 4, 5).partitionTo(n -> n % 2 == 0);
        assertEquals(Arrays.asList(2, 4), map.get(true));
        assertEquals(Arrays.asList(1, 3, 5), map.get(false));
    }

    @Test
    public void testPartitionToEmpty() {
        Map<Boolean, List<Integer>> map = Stream.<Integer> empty().partitionTo(n -> n % 2 == 0);
        assertTrue(map.get(true).isEmpty());
        assertTrue(map.get(false).isEmpty());
    }

    // rollup

    @Test
    public void testRollup() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4).rollup().toList();
        assertEquals(5, result.size());
        assertEquals(Arrays.asList(Arrays.asList(), Arrays.asList(1), Arrays.asList(1, 2), Arrays.asList(1, 2, 3), Arrays.asList(1, 2, 3, 4)), result);
    }

    @Test
    public void testRollup_Empty() {
        List<List<Integer>> result = Stream.<Integer> empty().rollup().toList();
        assertEquals(1, result.size());
        assertTrue(result.get(0).isEmpty());
    }

    @Test
    public void testRollupEmpty() {
        List<List<Integer>> result = Stream.<Integer> empty().rollup().toList();
        assertEquals(1, result.size());
        assertEquals(Arrays.asList(Arrays.asList()), result);
    }

    @Test
    public void testRollupSingleElement() {
        List<List<Integer>> result = Stream.of(1).rollup().toList();
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(Arrays.asList(), Arrays.asList(1)), result);
    }

    @Test
    public void testToArray() {
        Integer[] array = Stream.of(1, 2, 3).toArray(Integer[]::new);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, array);
    }

    @Test
    public void testToArrayGenerator() {
        String[] result = Stream.of("a", "b").toArray(String[]::new);
        assertArrayEquals(new String[] { "a", "b" }, result);
    }

    @Test
    public void testToCollection() {
        LinkedHashSet<Integer> set = Stream.of(1, 2, 3).toCollection(LinkedHashSet::new);
        assertEquals(new LinkedHashSet<>(Arrays.asList(1, 2, 3)), set);
    }

    @Test
    public void testToCollectionThenAccept() {
        List<Integer> holder = new ArrayList<>();
        Stream.of(1, 2, 3).toCollectionThenAccept(ArrayList::new, holder::addAll);
        assertEquals(Arrays.asList(1, 2, 3), holder);
    }

    @Test
    public void testToCollectionThenApply() {
        int result = Stream.of(1, 2, 3).toCollectionThenApply(ArrayList::new, List::size);
        assertEquals(3, result);
    }

    // toDataset

    @Test
    public void testToDataset() {
        Dataset dataset = Stream.of(Arrays.asList("a", 1), Arrays.asList("b", 2), Arrays.asList("c", 3)).toDataset(N.toList("Column1", "Column2"));
        assertEquals(3, dataset.size());
    }

    @Test
    public void testToDatasetEmpty() {
        Dataset dataset = Stream.<List<Object>> empty().toDataset();
        assertEquals(0, dataset.size());
    }

    @Test
    public void testToDatasetSimple() {
        Map<String, Object> row1 = N.asMap("id", 1, "name", "Alice");
        Map<String, Object> row2 = N.asMap("id", 2, "name", "Bob");
        Dataset dataset = Stream.of(row1, row2).toDataset();

        assertEquals(2, dataset.size());
        assertTrue(dataset.columnNames().containsAll(Arrays.asList("id", "name")));
    }

    @Test
    public void testToDatasetWithColumnNames() {
        Dataset dataset = Stream.of(Arrays.asList("a", 1), Arrays.asList("b", 2)).toDataset(Arrays.asList("col1", "col2"));
        assertEquals(2, dataset.size());
        assertEquals(Arrays.asList("col1", "col2"), dataset.columnNames());
    }

    @Test
    public void testToImmutableList() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).toImmutableList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

        try {
            result.add(6);
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testToImmutableListEmpty() {
        List<Integer> result = Stream.<Integer> empty().toImmutableList();
        assertTrue(result.isEmpty());
    }

    // toImmutableMap

    @Test
    public void testToImmutableMap() {
        com.landawn.abacus.util.ImmutableMap<Integer, String> map = Stream.of("a", "bb", "ccc").toImmutableMap(String::length, s -> s);
        assertEquals(3, map.size());
        assertEquals("a", map.get(1));
        assertEquals("bb", map.get(2));
        assertEquals("ccc", map.get(3));
    }

    @Test
    public void testToImmutableMapEmpty() {
        com.landawn.abacus.util.ImmutableMap<Integer, String> map = Stream.<String> empty().toImmutableMap(String::length, s -> s);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testToImmutableMapWithMerge() {
        ImmutableMap<Integer, String> map = Stream.of("a", "b", "cc").toImmutableMap(String::length, Fn.identity(), (s1, s2) -> s1 + ";" + s2);
        assertEquals("a;b", map.get(1));
        assertEquals("cc", map.get(2));
    }

    @Test
    public void testToImmutableSet() {
        Set<Integer> result = Stream.of(1, 2, 3, 2, 1).toImmutableSet();
        assertEquals(3, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));

        try {
            result.add(4);
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testToImmutableSetEmpty() {
        Set<Integer> result = Stream.<Integer> empty().toImmutableSet();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToJdkStream() {
        java.util.stream.Stream<Integer> jdkStream = Stream.of(1, 2, 3).toJdkStream();
        assertEquals(Arrays.asList(1, 2, 3), jdkStream.collect(Collectors.toList()));
    }

    @Test
    public void testToList() {
        List<Integer> list = Stream.of(1, 2, 3).toList();
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testToListThenAccept() {
        List<Integer> holder = new ArrayList<>();
        Stream.of(1, 2, 3).toListThenAccept(holder::addAll);
        assertEquals(Arrays.asList(1, 2, 3), holder);
    }

    @Test
    public void testToListThenApply() {
        int result = Stream.of(1, 2, 3).toListThenApply(List::size);
        assertEquals(3, result);
    }

    // toMap

    @Test
    public void testToMap() {
        Map<Integer, String> map = Stream.of("a", "bb", "ccc").toMap(String::length, s -> s);
        assertEquals("a", map.get(1));
        assertEquals("bb", map.get(2));
        assertEquals("ccc", map.get(3));
    }

    @Test
    public void testToMapWithFactory() {
        Map<Integer, String> map = Stream.of("a", "bb", "ccc").toMap(String::length, Fn.identity(), Suppliers.ofLinkedHashMap());
        assertEquals("a", map.get(1));
        assertEquals("bb", map.get(2));
        Iterator<Map.Entry<Integer, String>> iter = map.entrySet().iterator();
        assertEquals(1, iter.next().getKey());
        assertEquals(2, iter.next().getKey());
    }

    @Test
    public void testToMapWithMerge() {
        Map<Integer, String> map = Stream.of("a", "b", "cc", "dd").toMap(String::length, s -> s, (s1, s2) -> s1 + s2);
        assertEquals("ab", map.get(1));
        assertEquals("ccdd", map.get(2));
    }

    // toMultimap

    @Test
    public void testToMultimap() {
        com.landawn.abacus.util.ListMultimap<Integer, String> multimap = Stream.of("a", "bb", "c", "dd", "eee").toMultimap(String::length);
        assertEquals(2, multimap.get(1).size());
        assertEquals(2, multimap.get(2).size());
        assertEquals(1, multimap.get(3).size());
    }

    @Test
    public void testToMultimapEmpty() {
        com.landawn.abacus.util.ListMultimap<Integer, String> multimap = Stream.<String> empty().toMultimap(String::length);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testToMultimapKeyAndValueMapperAndFactory() {
        ListMultimap<Character, Integer> multimap = Stream.of("apple", "apricot").toMultimap(s -> s.charAt(0), String::length, Suppliers.ofListMultimap());

        assertEquals(Arrays.asList(5, 7), multimap.get('a'));
    }

    @Test
    public void testToMultimapKeyMapper() {
        ListMultimap<Integer, String> multimap = Stream.of("a", "bb", "c", "dd").toMultimap(String::length);
        assertEquals(Arrays.asList("a", "c"), multimap.get(1));
        assertEquals(Arrays.asList("bb", "dd"), multimap.get(2));
    }

    @Test
    public void testToMultiset() {
        Multiset<Integer> multiset = Stream.of(1, 2, 2, 3, 3, 3).toMultiset();
        assertEquals(1, multiset.count(1));
        assertEquals(2, multiset.count(2));
        assertEquals(3, multiset.count(3));
    }

    @Test
    public void testToSet() {
        Set<Integer> set = Stream.of(1, 2, 2, 3).toSet();
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
    }

    @Test
    public void testToSetThenAccept() {
        List<Integer> holder = new ArrayList<>();
        Stream.of(1, 2, 2, 3).toSetThenAccept(holder::addAll);
        assertEquals(3, holder.size());
    }

    @Test
    public void testToSetThenApply() {
        int result = Stream.of(1, 2, 2, 3).toSetThenApply(java.util.Set::size);
        assertEquals(3, result);
    }

}
