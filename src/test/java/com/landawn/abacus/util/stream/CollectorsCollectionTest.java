package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Collector;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.BiMap;
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.stream.Collectors.MoreCollectors;

/** Collectors collection contracts. */
public class CollectorsCollectionTest extends CollectorsTestSupport {

    // groupingBy

    @Test
    public void testGroupingBy() {
        Map<String, List<Person>> result = personList.stream().collect(Collectors.groupingBy(Person::getDepartment));
        assertEquals(3, result.size());
        assertEquals(2, result.get("Engineering").size());
        assertEquals(2, result.get("Sales").size());
        assertEquals(1, result.get("HR").size());
    }

    @Test
    public void test_groupingBy() {
        Map<Integer, List<String>> map = Stream.of("a", "bb", "c", "dd").collect(Collectors.groupingBy(String::length));
        assertEquals(Arrays.asList("a", "c"), map.get(1));
        assertEquals(Arrays.asList("bb", "dd"), map.get(2));
    }

    @Test
    public void testGroupingBy_WithDownstream() {
        Map<String, Long> result = personList.stream().collect(Collectors.groupingBy(Person::getDepartment, Collectors.counting()));
        assertEquals(3, result.size());
        assertEquals(2L, result.get("Engineering"));
        assertEquals(2L, result.get("Sales"));
        assertEquals(1L, result.get("HR"));
    }

    @Test
    public void test_groupingBy_withDownstream() {
        Map<Integer, Long> map = Stream.of("a", "bb", "c", "dd").collect(Collectors.groupingBy(String::length, Collectors.counting()));
        assertEquals(2L, map.get(1));
        assertEquals(2L, map.get(2));
    }

    @Test
    public void testGroupingBy_WithMapFactory() {
        LinkedHashMap<String, List<Person>> result = personList.stream().collect(Collectors.groupingBy(Person::getDepartment, LinkedHashMap::new));
        assertEquals(3, result.size());
        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testGroupingByWithDownstream() {
        Map<Integer, Long> grouped = Stream.of("a", "bb", "ccc", "dd", "e").collect(Collectors.groupingBy(String::length, Collectors.counting()));

        Assertions.assertEquals(2L, grouped.get(1));
        Assertions.assertEquals(2L, grouped.get(2));
        Assertions.assertEquals(1L, grouped.get(3));
    }

    @Test
    public void testGroupingByWithDownstreamAndMapFactory() {
        TreeMap<Integer, Long> grouped = Stream.of("a", "bb", "ccc", "dd", "e")
                .collect(Collectors.groupingBy(String::length, Collectors.counting(), TreeMap::new));

        Assertions.assertTrue(grouped instanceof TreeMap);
        Assertions.assertEquals(2L, grouped.get(1));
    }

    @Test
    public void testGroupingByWithMapFactory() {
        TreeMap<Integer, List<String>> grouped = Stream.of("a", "bb", "ccc", "dd", "e").collect(Collectors.groupingBy(String::length, TreeMap::new));

        Assertions.assertTrue(grouped instanceof TreeMap);
        Assertions.assertEquals(3, grouped.size());
    }

    // groupingByConcurrent

    @Test
    public void testGroupingByConcurrent() {
        ConcurrentMap<String, List<Person>> result = personList.stream().collect(Collectors.groupingByConcurrent(Person::getDepartment));
        assertEquals(3, result.size());
        assertTrue(result instanceof ConcurrentMap);
    }

    @Test
    public void test_groupingByConcurrent() {
        ConcurrentMap<Integer, List<String>> map = Stream.of("a", "bb", "c", "dd").parallel().collect(Collectors.groupingByConcurrent(String::length));
        assertTrue(map.get(1).containsAll(Arrays.asList("a", "c")));
        assertTrue(map.get(2).containsAll(Arrays.asList("bb", "dd")));
    }

    @Test
    public void testGroupingByConcurrent_WithDownstream() {
        ConcurrentMap<String, Long> result = personList.stream().collect(Collectors.groupingByConcurrent(Person::getDepartment, Collectors.counting()));
        assertEquals(3, result.size());
    }

    @Test
    public void testGroupingByConcurrent_WithMapFactory() {
        ConcurrentHashMap<String, List<Person>> result = personList.stream()
                .collect(Collectors.groupingByConcurrent(Person::getDepartment, ConcurrentHashMap::new));
        assertEquals(3, result.size());
        assertTrue(result instanceof ConcurrentHashMap);
    }

    @Test
    public void testGroupingByConcurrentWithDownstream() {
        ConcurrentMap<Integer, Long> grouped = Stream.of("a", "bb", "ccc", "dd", "e")
                .parallel()
                .collect(Collectors.groupingByConcurrent(String::length, Collectors.counting()));

        Assertions.assertEquals(2L, grouped.get(1));
        Assertions.assertEquals(2L, grouped.get(2));
        Assertions.assertEquals(1L, grouped.get(3));
    }

    @Test
    public void testGroupingByConcurrentWithDownstreamAndMapFactory() {
        ConcurrentHashMap<Integer, Long> grouped = Stream.of("a", "bb", "ccc", "dd", "e")
                .parallel()
                .collect(Collectors.groupingByConcurrent(String::length, Collectors.counting(), ConcurrentHashMap::new));

        Assertions.assertTrue(grouped instanceof ConcurrentHashMap);
        Assertions.assertEquals(2L, grouped.get(1));
    }

    @Test
    public void testGroupingByConcurrentWithMapFactory() {
        ConcurrentHashMap<Integer, List<String>> grouped = Stream.of("a", "bb", "ccc", "dd", "e")
                .parallel()
                .collect(Collectors.groupingByConcurrent(String::length, ConcurrentHashMap::new));

        Assertions.assertTrue(grouped instanceof ConcurrentHashMap);
        Assertions.assertEquals(3, grouped.size());
    }

    @Test
    public void testGroupingFinishesValuesInConcurrentSkipListMap() {
        final Map<Integer, Integer> values = new java.util.concurrent.ConcurrentSkipListMap<>(N.asMap(1, 2, 3, 4));
        Collectors.replaceAll(values, Integer::sum);
        assertEquals(N.asMap(1, 3, 3, 7), values);
        Collectors.replaceAll(java.util.Collections.emptyMap(), (key, value) -> {
            throw new AssertionError("empty map must not invoke the function");
        });

        for (final boolean iteratorSource : new boolean[] { false, true }) {
            for (final boolean parallel : new boolean[] { false, true }) {
                final Stream<Integer> objectSource = iteratorSource ? Stream.of(Arrays.asList(1, 2, 3).iterator()) : Stream.of(1, 2, 3);
                final Stream<Integer> objects = parallel ? objectSource.parallel(2) : objectSource;
                assertEquals(N.asMap(0, 1L, 1, 2L),
                        objects.groupTo(value -> value % 2, Collectors.counting(), java.util.concurrent.ConcurrentSkipListMap::new));

                final IntStream intSource = iteratorSource ? IntStream.of(com.landawn.abacus.util.IntIterator.of(1, 2, 3)) : IntStream.of(1, 2, 3);
                final IntStream ints = parallel ? intSource.parallel(2) : intSource;
                assertEquals(N.asMap(0, 1L, 1, 2L), ints.groupTo(value -> value % 2, Collectors.counting(), java.util.concurrent.ConcurrentSkipListMap::new));
            }
        }
    }

    @Test
    public void testGroupingPreservesDownstreamFinisherFailure() {
        for (final boolean iteratorSource : new boolean[] { false, true }) {
            for (final boolean parallel : new boolean[] { false, true }) {
                final IllegalStateException failure = new IllegalStateException("downstream result is invalid");
                final Collector<Integer, ?, Integer> downstream = Collectors.collectingAndThen(Collectors.<Integer> toList(), values -> {
                    throw failure;
                });

                final Stream<Integer> objectSource = iteratorSource ? Stream.of(Arrays.asList(1, 2, 3).iterator()) : Stream.of(1, 2, 3);
                final Stream<Integer> objects = parallel ? objectSource.parallel(2) : objectSource;
                Assertions.assertSame(failure, assertThrows(IllegalStateException.class, () -> objects.groupTo(value -> value % 2, downstream)));

                final IntStream intSource = iteratorSource ? IntStream.of(com.landawn.abacus.util.IntIterator.of(1, 2, 3)) : IntStream.of(1, 2, 3);
                final IntStream ints = parallel ? intSource.parallel(2) : intSource;
                Assertions.assertSame(failure, assertThrows(IllegalStateException.class, () -> ints.groupTo(value -> value % 2, downstream)));
            }
        }
    }

    // partitioningBy

    @Test
    public void testPartitioningBy() {
        Map<Boolean, List<Integer>> result = integerList.stream().collect(Collectors.partitioningBy(i -> i % 2 == 0));
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(2, 4), result.get(true));
        assertEquals(Arrays.asList(1, 3, 5), result.get(false));
    }

    @Test
    public void test_partitioningBy() {
        Map<Boolean, List<Integer>> map = Stream.of(1, 2, 3, 4).collect(Collectors.partitioningBy(i -> i % 2 == 0));
        assertEquals(Arrays.asList(1, 3), map.get(false));
        assertEquals(Arrays.asList(2, 4), map.get(true));
    }

    @Test
    public void testPartitioningBy_WithDownstream() {
        Map<Boolean, Long> result = integerList.stream().collect(Collectors.partitioningBy(i -> i % 2 == 0, Collectors.counting()));
        assertEquals(2L, result.get(true));
        assertEquals(3L, result.get(false));
    }

    @Test
    public void test_partitioningBy_withDownstream() {
        Map<Boolean, Long> map = Stream.of(1, 2, 3, 4).collect(Collectors.partitioningBy(i -> i % 2 == 0, Collectors.counting()));
        assertEquals(2L, map.get(false));
        assertEquals(2L, map.get(true));
    }

    @Test
    public void testPartitioningBy_WithDownstream_Parallel_CombinerUsed() {
        // Exercises partitioningBy combiner in parallel
        Map<Boolean, Long> result = integerList.parallelStream().collect(Collectors.partitioningBy(i -> i % 2 == 0, Collectors.counting()));
        assertNotNull(result);
        assertEquals(2L, (long) result.get(Boolean.TRUE));
        assertEquals(3L, (long) result.get(Boolean.FALSE));
    }

    @Test
    public void testPartitioningByPropagatesDownstreamOrdering() {
        assertFalse(Collectors.partitioningBy((Integer i) -> i % 2 == 0).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.partitioningBy((Integer i) -> i % 2 == 0, Collectors.toList()).characteristics().contains(Characteristics.UNORDERED));
        assertTrue(Collectors.partitioningBy((Integer i) -> i % 2 == 0, Collectors.toSet()).characteristics().contains(Characteristics.UNORDERED));

        Map<Boolean, List<Integer>> result = java.util.stream.IntStream.range(0, 2000).boxed().parallel().collect(Collectors.partitioningBy(i -> i % 2 == 0));
        List<Integer> expectedEven = new ArrayList<>();
        List<Integer> expectedOdd = new ArrayList<>();

        for (int i = 0; i < 2000; i++) {
            (i % 2 == 0 ? expectedEven : expectedOdd).add(i);
        }

        assertEquals(expectedEven, result.get(Boolean.TRUE));
        assertEquals(expectedOdd, result.get(Boolean.FALSE));
    }

    @Test
    public void testPartitioningByWithDownstream() {
        Map<Boolean, Long> partitioned = Stream.of(1, 2, 3, 4, 5).collect(Collectors.partitioningBy(n -> n % 2 == 0, Collectors.counting()));

        Assertions.assertEquals(3L, partitioned.get(false));
        Assertions.assertEquals(2L, partitioned.get(true));
    }

    @Test
    public void testPartitioningCapturesDownstreamFunctionsOnce() {
        final int[] accessorCalls = new int[4];
        final Collector<Integer, List<Integer>, Integer> downstream = new Collector<>() {
            @Override
            public Supplier<List<Integer>> supplier() {
                accessorCalls[0]++;
                return ArrayList::new;
            }

            @Override
            public BiConsumer<List<Integer>, Integer> accumulator() {
                accessorCalls[1]++;
                return List::add;
            }

            @Override
            public BinaryOperator<List<Integer>> combiner() {
                accessorCalls[2]++;
                return (left, right) -> {
                    left.addAll(right);
                    return left;
                };
            }

            @Override
            public Function<List<Integer>, Integer> finisher() {
                accessorCalls[3]++;
                return List::size;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return java.util.Collections.emptySet();
            }
        };

        final Collector<Integer, ?, Map<Boolean, Integer>> collector = Collectors.partitioningBy(value -> value % 2 == 0, downstream);
        assertArrayEquals(new int[] { 1, 1, 1, 1 }, accessorCalls);
        assertEquals(N.asMap(false, 2, true, 2), Stream.of(1, 2, 3, 4).collect(collector));
        assertEquals(N.asMap(false, 0, true, 0), Stream.<Integer> empty().collect(collector));
        assertArrayEquals(new int[] { 1, 1, 1, 1 }, accessorCalls);
    }

    // toArray

    @Test
    public void test_toArray() {
        Object[] array = Stream.of(1, "b", 3.0).collect(Collectors.toArray());
        assertArrayEquals(new Object[] { 1, "b", 3.0 }, array);
    }

    @Test
    public void testToArray() {
        Object[] result = Stream.of("a", "b", "c").collect(Collectors.toArray());

        Assertions.assertArrayEquals(new Object[] { "a", "b", "c" }, result);
    }

    @Test
    public void testToArray_IntFunction() {
        String[] result = stringList.stream().collect(Collectors.toArray(String[]::new));
        assertArrayEquals(stringList.toArray(new String[0]), result);
    }

    @Test
    public void testToArray_Object() {
        Object[] result = stringList.stream().collect(Collectors.toArray());
        assertArrayEquals(stringList.toArray(), result);
    }

    @Test
    public void testToArray_SupplierArray() {
        String[] result = stringList.stream().collect(Collectors.toArray(() -> new String[0]));
        assertArrayEquals(stringList.toArray(new String[0]), result);
    }

    @Test
    public void test_toArray_withGenerator() {
        Integer[] array = Stream.of(1, 2, 3).collect(Collectors.toArray(Integer[]::new));
        assertArrayEquals(new Integer[] { 1, 2, 3 }, array);
    }

    @Test
    public void testToArrayWithIntFunction() {
        String[] result = Stream.of("a", "b", "c").collect(Collectors.toArray(String[]::new));

        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, result);
    }

    @Test
    public void testToArrayWithSupplier() {
        String[] result = Stream.of("a", "b", "c").collect(Collectors.toArray(() -> new String[0]));

        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, result);
    }

    // toBiMap

    @Test
    public void testToBiMap() {
        BiMap<String, Integer> result = Stream.of(stringList)
                .distinct()
                .distinctBy(Fn.length())
                .collect(Collectors.toBiMap(Function.identity(), String::length));
        assertEquals(4, result.size());
        assertEquals("apple", result.getByValue(5));
    }

    @Test
    public void testToBiMap_WithAllParameters() {
        BiMap<Integer, String> result = stringList.stream().collect(Collectors.toBiMap(String::length, Function.identity(), (a, b) -> a, BiMap::new));
        assertNotNull(result);
    }

    @Test
    public void testToBiMap_WithMergeFunction() {
        BiMap<Integer, String> result = stringList.stream().collect(Collectors.toBiMap(String::length, Function.identity(), (a, b) -> a));
        assertNotNull(result.get(6));
    }

    @Test
    public void testToBiMap_WithMergeFunction_Parallel_CombinerUsed() {
        // Triggers lambda$289 (biMapMerger combiner) in parallel
        BiMap<Integer, String> result = Stream.of("a", "bb", "ccc", "dd", "eee", "ff")
                .parallel()
                .collect(Collectors.toBiMap(String::length, s -> s, (s1, s2) -> s1));
        assertNotNull(result);
    }

    @Test
    public void testToBiMapWithAllParameters() {
        BiMap<Integer, String> biMap = Stream.of("a", "bb", "ccc", "dd").collect(Collectors.toBiMap(String::length, e -> e, (s1, s2) -> s1, BiMap::new));

        Assertions.assertEquals("bb", biMap.get(2));
    }

    @Test
    public void testToBiMapWithMapFactory() {
        BiMap<Integer, String> biMap = Stream.of("a", "bb", "ccc").collect(Collectors.toBiMap(String::length, e -> e, Suppliers.ofBiMap()));

        Assertions.assertEquals("a", biMap.get(1));
    }

    @Test
    public void testToBiMapWithMergeFunction() {
        BiMap<Integer, String> biMap = Stream.of("a", "bb", "ccc", "dd").collect(Collectors.toBiMap(String::length, e -> e, (s1, s2) -> s1));

        Assertions.assertEquals("bb", biMap.get(2));
    }

    @Test
    public void testToBooleanArray() {
        boolean[] result = booleanList.stream().collect(Collectors.toBooleanArray());
        assertEquals(booleanList.size(), result.length);
        assertTrue(result[0]);
        assertFalse(result[1]);
    }

    @Test
    public void testToBooleanList() {
        BooleanList result = booleanList.stream().collect(Collectors.toBooleanList());
        assertEquals(booleanList.size(), result.size());
        assertTrue(result.get(0));
        assertFalse(result.get(1));
    }

    @Test
    public void testToByteArray() {
        byte[] result = byteList.stream().collect(Collectors.toByteArray());
        assertEquals(byteList.size(), result.length);
        assertEquals((byte) 1, result[0]);
    }

    @Test
    public void testToByteList() {
        ByteList result = byteList.stream().collect(Collectors.toByteList());
        assertEquals(byteList.size(), result.size());
        assertEquals((byte) 1, result.get(0));
    }

    @Test
    public void testToCharArray() {
        char[] result = charList.stream().collect(Collectors.toCharArray());
        assertEquals(charList.size(), result.length);
        assertEquals('a', result[0]);
    }

    @Test
    public void testToCharList() {
        CharList result = charList.stream().collect(Collectors.toCharList());
        assertEquals(charList.size(), result.size());
        assertEquals('a', result.get(0));
    }

    // toCollection

    @Test
    public void test_toCollection() {
        List<Integer> list = Stream.of(1, 2, 3, 2).collect(Collectors.toCollection(LinkedList::new));
        assertEquals(LinkedList.class, list.getClass());
        assertEquals(Arrays.asList(1, 2, 3, 2), list);

        Set<Integer> set = Stream.of(1, 2, 3, 2).collect(Collectors.toCollection(HashSet::new));
        assertEquals(HashSet.class, set.getClass());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
    }

    @Test
    public void testToCollection() {
        List<String> result = Stream.of("a", "b", "c").collect(Collectors.toCollection(ArrayList::new));

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);
        Assertions.assertTrue(result instanceof ArrayList);
    }

    @Test
    public void testToCollection_SupplierAccumulator() {
        Collector<String, ?, List<String>> collector = Collectors.toCollection(ArrayList::new, List::add);
        List<String> result = stringList.stream().collect(collector);
        assertEquals(stringList, result);
    }

    @Test
    public void testToCollection_SupplierAccumulatorCombiner() {
        Collector<String, ?, List<String>> collector = Collectors.toCollection(ArrayList::new, List::add, (a, b) -> {
            a.addAll(b);
            return a;
        });
        List<String> result = stringList.stream().collect(collector);
        assertEquals(stringList, result);
    }

    @Test
    public void testToCollection_SupplierOnly() {
        Collector<String, ?, LinkedList<String>> collector = Collectors.toCollection(LinkedList::new);
        LinkedList<String> result = stringList.stream().collect(collector);
        assertEquals(stringList, result);
        assertTrue(result instanceof LinkedList);
    }

    @Test
    public void test_toCollection_with_atMost() {
        List<Integer> result = Stream.range(0, 30).parallel(3).collect(Collectors.toCollection(10, () -> new ArrayList<>()));

        assertNotNull(result);
        assertEquals(10, result.size());
    }

    @Test
    public void testToCollection_WithAtMostSize_Parallel_CombinerUsed() {
        // Triggers lambda$176 combiner in parallel collection with atMostSize
        List<Integer> result = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).parallel().collect(Collectors.toCollection(5, ArrayList::new));
        assertEquals(5, result.size());
    }

    @Test
    public void testToCollection_WithAtMostSize_Parallel_ExceedSize() {
        // Triggers combiner where b.size() > n
        List<String> result = Stream.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j").parallel().collect(Collectors.toCollection(3, ArrayList::new));
        assertEquals(3, result.size());
    }

    @Test
    public void testToCollection_WithLimit() {
        Collector<String, ?, List<String>> collector = Collectors.toCollection(3, ArrayList::new);
        List<String> result = stringList.stream().collect(collector);
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("apple", "banana", "cherry"), result);
    }

    @Test
    public void test_toCollection_withSizeLimit() {
        List<Integer> list = Stream.of(1, 2, 3, 4, 5).collect(Collectors.toCollection(3, ArrayList::new));
        assertEquals(3, list.size());
        assertEquals(Arrays.asList(1, 2, 3), list);

        List<Integer> parallelList = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).parallel().collect(Collectors.toCollection(5, ArrayList::new));
        assertEquals(5, parallelList.size());
    }

    @Test
    public void testToCollectionWithAccumulator() {
        BiConsumer<List<String>, String> accumulator = (list, s) -> {
            if (s.length() > 1) {
                list.add(s);
            }
        };

        List<String> result = Stream.of("a", "bb", "c", "dd").collect(Collectors.toCollection(ArrayList::new, accumulator));

        Assertions.assertEquals(Arrays.asList("bb", "dd"), result);
    }

    @Test
    public void testToCollectionWithAccumulatorAndCombiner() {
        BiConsumer<List<String>, String> accumulator = List::add;
        BinaryOperator<List<String>> combiner = (list1, list2) -> {
            list1.addAll(list2);
            return list1;
        };

        List<String> result = Stream.of("a", "b", "c").collect(Collectors.toCollection(ArrayList::new, accumulator, combiner));

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testToCollectionWithLimit() {
        List<String> result = Stream.of("a", "b", "c", "d", "e").collect(Collectors.toCollection(3, ArrayList::new));

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    // toConcurrentMap

    @Test
    public void testToConcurrentMap() {
        ConcurrentMap<String, Integer> result = stringList.stream().collect(Collectors.toConcurrentMap(Function.identity(), String::length));
        assertEquals(5, result.size());
        assertTrue(result instanceof ConcurrentMap);
    }

    @Test
    public void test_toConcurrentMap() {
        ConcurrentMap<String, Integer> map = Stream.of("a", "bb", "ccc").parallel().collect(Collectors.toConcurrentMap(s -> s, String::length));
        assertEquals(3, map.size());
        assertEquals(1, map.get("a"));

        assertThrows(IllegalStateException.class, () -> Stream.of("a", "b", "a").parallel().collect(Collectors.toConcurrentMap(s -> s, String::length)));
    }

    @Test
    public void testToConcurrentMap_WithAllParameters() {
        ConcurrentHashMap<Integer, String> result = stringList.stream()
                .collect(Collectors.toConcurrentMap(String::length, Function.identity(), (a, b) -> a, ConcurrentHashMap::new));
        assertTrue(result instanceof ConcurrentHashMap);
    }

    @Test
    public void testToConcurrentMap_WithMergeFunction() {
        ConcurrentMap<Integer, String> result = stringList.stream().collect(Collectors.toConcurrentMap(String::length, Function.identity(), (a, b) -> a));
        assertTrue(result instanceof ConcurrentMap);
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testToConcurrentMapCombinerUsesMapMergeSemantics() {
        final Collector<String, ?, ConcurrentMap<String, String>> collector = Collectors.toConcurrentMap(Function.identity(), Function.identity(),
                (left, right) -> null);
        final ConcurrentMap<String, String> left = new ConcurrentHashMap<>();
        final ConcurrentMap<String, String> right = new ConcurrentHashMap<>();
        left.put("duplicate", "left");
        left.put("leftOnly", "left");
        right.put("duplicate", "right");
        right.put("rightOnly", "right");

        final ConcurrentMap<String, String> result = (ConcurrentMap<String, String>) ((BinaryOperator) collector.combiner()).apply(left, right);

        assertFalse(result.containsKey("duplicate"));
        assertEquals("left", result.get("leftOnly"));
        assertEquals("right", result.get("rightOnly"));
    }

    @Test
    public void testToConcurrentMapWithAllParameters() {
        for (int i = 0; i < 100; i++) {
            Map<Integer, String> map = Stream.of("a", "bb", "ccc", "dd")
                    .parallel()
                    .collect(Collectors.toConcurrentMap(String::length, e -> e, (s1, s2) -> s1 + "," + s2, Suppliers.ofConcurrentHashMap()));

            N.println(map);

            Assertions.assertTrue(map instanceof ConcurrentHashMap);
            Assertions.assertTrue(map.get(2).contains("bb"), () -> "Expected map to contain 'bb' but got: " + map.get(2));
            Assertions.assertTrue(map.get(2).contains("dd"), () -> "Expected map to contain 'dd' but got: " + map.get(2));
        }
    }

    @Test
    public void testToConcurrentMapWithMapFactory() {
        ConcurrentHashMap<String, Integer> map = Stream.of("a", "bb", "ccc")
                .parallel()
                .collect(Collectors.toConcurrentMap(e -> e, String::length, Suppliers.ofConcurrentHashMap()));

        Assertions.assertTrue(map instanceof ConcurrentHashMap);
        Assertions.assertEquals(1, map.get("a"));
    }

    @Test
    public void testToConcurrentMapWithMergeFunction() {
        ConcurrentMap<Integer, String> map = Stream.of("a", "bb", "ccc", "dd")
                .parallel()
                .collect(Collectors.toConcurrentMap(String::length, e -> e, (s1, s2) -> s1 + "," + s2));

        Assertions.assertTrue(map.get(2).contains("bb"));
        Assertions.assertTrue(map.get(2).contains("dd"));
    }

    @Test
    public void testToConcurrentMapWithAllParameters_2() {
        for (int i = 0; i < 100; i++) {
            Map<Integer, String> map = Stream.of("a", "bb", "ccc", "dd")
                    .parallel()
                    .collect(Collectors.toMap(String::length, e -> e, (s1, s2) -> s1 + "," + s2, Suppliers.ofMap()));

            N.println(map);

            Assertions.assertTrue(map instanceof HashMap);
            Assertions.assertTrue(map.get(2).contains("bb"), () -> "Expected map to contain 'bb' but got: " + map.get(2));
            Assertions.assertTrue(map.get(2).contains("dd"), () -> "Expected map to contain 'dd' but got: " + map.get(2));
        }
    }

    @Test
    public void test_toDataset() {
        List<Map<String, Object>> data = new ArrayList<>();
        data.add(N.asMap("id", 1, "name", "John"));
        data.add(N.asMap("id", 2, "name", "Jane"));

        Dataset dataset = data.stream().collect(MoreCollectors.toDataset());
        assertEquals(2, dataset.size());
        assertEquals(2, dataset.columnNames().size());
        assertTrue(dataset.columnNames().contains("id"));
        assertTrue(dataset.columnNames().contains("name"));

        Dataset datasetWithNames = data.stream().collect(MoreCollectors.toDataset(Arrays.asList("ID", "NAME")));
        assertEquals(2, datasetWithNames.size());
        assertEquals(Arrays.asList("ID", "NAME"), datasetWithNames.columnNames());
    }

    @Test
    public void testToDeque() {
        Deque<String> result = stringList.stream().collect(Collectors.toDeque());
        assertEquals(stringList.size(), result.size());
        assertEquals("apple", result.pollFirst());
        assertEquals("elderberry", result.pollLast());
    }

    @Test
    public void testToDoubleArray() {
        double[] result = doubleList.stream().collect(Collectors.toDoubleArray());
        assertEquals(doubleList.size(), result.length);
        assertEquals(1.1, result[0], 0.01);
    }

    @Test
    public void testToDoubleList() {
        DoubleList result = doubleList.stream().collect(Collectors.toDoubleList());
        assertEquals(doubleList.size(), result.size());
        assertEquals(1.1, result.get(0), 0.01);
    }

    @Test
    public void testToFloatArray() {
        float[] result = floatList.stream().collect(Collectors.toFloatArray());
        assertEquals(floatList.size(), result.length);
        assertEquals(1.1f, result[0], 0.01);
    }

    @Test
    public void testToFloatList() {
        FloatList result = floatList.stream().collect(Collectors.toFloatList());
        assertEquals(floatList.size(), result.size());
        assertEquals(1.1f, result.get(0), 0.01);
    }

    @Test
    public void testToImmutableList() {
        ImmutableList<String> result = stringList.stream().collect(Collectors.toImmutableList());
        assertEquals(stringList, result);
        assertThrows(UnsupportedOperationException.class, () -> result.add("test"));
    }

    @Test
    public void test_toImmutableList() {
        List<Integer> list = Stream.of(1, 2, 3).collect(Collectors.toImmutableList());
        assertEquals(Arrays.asList(1, 2, 3), list);
        assertThrows(UnsupportedOperationException.class, () -> list.add(4));
    }

    // toImmutableMap

    @Test
    public void testToImmutableMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        ImmutableMap<String, Integer> result = source.entrySet().stream().collect(Collectors.toImmutableMap());
        assertEquals(source, result);
        assertThrows(UnsupportedOperationException.class, () -> result.put("c", 3));
    }

    @Test
    public void testToImmutableMap_FromEntry() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2));
        ImmutableMap<String, Integer> result = entries.stream().collect(Collectors.toImmutableMap());
        assertEquals(2, result.size());
        assertThrows(UnsupportedOperationException.class, () -> result.put("c", 3));
    }

    @Test
    public void testToImmutableMap_FromEntryWithMergeFunction() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("a", 2));
        ImmutableMap<String, Integer> result = entries.stream().collect(Collectors.toImmutableMap(Integer::sum));
        assertEquals(3, result.get("a"));
    }

    @Test
    public void testToImmutableMap_WithKeyValueMappers() {
        ImmutableMap<String, Integer> result = stringList.stream().collect(Collectors.toImmutableMap(Function.identity(), String::length));
        assertEquals(5, result.size());
        assertThrows(UnsupportedOperationException.class, () -> result.put("test", 4));
    }

    @Test
    public void testToImmutableMap_WithKeyValueMappersAndMergeFunction() {
        ImmutableMap<Integer, String> result = stringList.stream().collect(Collectors.toImmutableMap(String::length, Function.identity(), (a, b) -> a));
        assertNotNull(result.get(6));
    }

    @Test
    public void testToImmutableMapFromEntries() {
        ImmutableMap<String, Integer> map = Stream.of(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2))
                .collect(Collectors.toImmutableMap());

        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals(1, map.get("a"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.put("c", 3));
    }

    @Test
    public void testToImmutableMapFromEntriesWithMergeFunction() {
        ImmutableMap<String, Integer> map = Stream
                .of(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("a", 2), new AbstractMap.SimpleEntry<>("b", 3))
                .collect(Collectors.toImmutableMap(Integer::sum));

        Assertions.assertEquals(3, map.get("a"));
    }

    @Test
    public void testToImmutableMapWithKeyValueMappers() {
        ImmutableMap<String, Integer> map = Stream.of("a", "bb", "ccc").collect(Collectors.toImmutableMap(e -> e, String::length));

        Assertions.assertEquals(1, map.get("a"));
        Assertions.assertEquals(2, map.get("bb"));
        Assertions.assertEquals(3, map.get("ccc"));
    }

    @Test
    public void testToImmutableMapWithKeyValueMappersAndMergeFunction() {
        ImmutableMap<Integer, String> map = Stream.of("a", "bb", "ccc", "dd")
                .collect(Collectors.toImmutableMap(String::length, e -> e, (s1, s2) -> s1 + "," + s2));

        Assertions.assertEquals("bb,dd", map.get(2));
    }

    @Test
    public void testToImmutableSet() {
        ImmutableSet<String> result = stringList.stream().collect(Collectors.toImmutableSet());
        assertEquals(new HashSet<>(stringList), result);
        assertThrows(UnsupportedOperationException.class, () -> result.add("test"));
    }

    @Test
    public void test_toImmutableSet() {
        Set<Integer> set = Stream.of(1, 2, 3, 2).collect(Collectors.toImmutableSet());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
        assertThrows(UnsupportedOperationException.class, () -> set.add(4));
    }

    @Test
    public void testToIntArray() {
        int[] result = integerList.stream().collect(Collectors.toIntArray());
        assertEquals(integerList.size(), result.length);
        assertEquals(1, result[0]);
    }

    @Test
    public void testToIntList() {
        IntList result = integerList.stream().collect(Collectors.toIntList());
        assertEquals(integerList.size(), result.size());
        assertEquals(1, result.get(0));
    }

    // toLinkedHashMap

    @Test
    public void testToLinkedHashMap() {
        Map<String, Integer> result = stringList.stream().collect(Collectors.toLinkedHashMap(Function.identity(), String::length));
        assertEquals(5, result.size());
        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testToLinkedHashMap_WithMergeFunction() {
        Map<Integer, String> result = stringList.stream().collect(Collectors.toLinkedHashMap(String::length, Function.identity(), (a, b) -> a));
        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testToLinkedHashMapWithMergeFunction() {
        Map<Integer, String> map = Stream.of("a", "bb", "ccc", "dd").collect(Collectors.toLinkedHashMap(String::length, e -> e, (s1, s2) -> s1 + "," + s2));

        Assertions.assertTrue(map instanceof LinkedHashMap);
        Assertions.assertEquals("bb,dd", map.get(2));
    }

    @Test
    public void testToLinkedHashSet() {
        Set<String> result = stringList.stream().collect(Collectors.toLinkedHashSet());
        assertEquals(new LinkedHashSet<>(stringList), result);
        assertTrue(result instanceof LinkedHashSet);
    }

    @Test
    public void test_toLinkedHashSet() {
        Set<Integer> set = Stream.of(3, 1, 2, 1).collect(Collectors.toLinkedHashSet());
        assertEquals(LinkedHashSet.class, set.getClass());
        assertEquals(Arrays.asList(3, 1, 2), new ArrayList<>(set));
    }

    @Test
    public void testToLinkedList() {
        LinkedList<String> result = stringList.stream().collect(Collectors.toLinkedList());
        assertEquals(stringList, result);
        assertTrue(result instanceof LinkedList);
    }

    @Test
    public void test_toLinkedList() {
        LinkedList<Integer> list = Stream.of(1, 2, 3).collect(Collectors.toLinkedList());
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    // toList

    @Test
    public void testToList() {
        List<String> result = stringList.stream().collect(Collectors.toList());
        assertEquals(stringList, result);
    }

    @Test
    public void test_toList() {
        List<Integer> list = Stream.of(1, 2, 3).collect(Collectors.toList());
        assertEquals(Arrays.asList(1, 2, 3), list);
        assertTrue(list instanceof ArrayList);
    }

    @Test
    public void testToList_Empty() {
        List<String> result = Stream.<String> empty().collect(Collectors.toList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToList_WithLimit() {
        List<String> result = stringList.stream().collect(Collectors.toList(3));
        assertEquals(3, result.size());
    }

    @Test
    public void testToListWithLimit() {
        List<String> result = Stream.of("a", "b", "c", "d", "e").collect(Collectors.toList(3));

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testToLongArray() {
        long[] result = longList.stream().collect(Collectors.toLongArray());
        assertEquals(longList.size(), result.length);
        assertEquals(100L, result[0]);
    }

    @Test
    public void testToLongList() {
        LongList result = longList.stream().collect(Collectors.toLongList());
        assertEquals(longList.size(), result.size());
        assertEquals(100L, result.get(0));
    }

    // toMap

    @Test
    public void test_toMap() {
        Map<String, Integer> map = Stream.of("a", "bb", "ccc").collect(Collectors.toMap(s -> s, String::length));
        assertEquals(3, map.size());
        assertEquals(1, map.get("a"));

        assertThrows(IllegalStateException.class, () -> Stream.of("a", "b", "a").collect(Collectors.toMap(s -> s, String::length)));

        Map<String, Integer> mergedMap = Stream.of("a", "b", "a").collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
        assertEquals(2, mergedMap.get("a"));
        assertEquals(1, mergedMap.get("b"));
    }

    @Test
    public void testToMap_FromEntry() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2));
        Map<String, Integer> result = entries.stream().collect(Collectors.toMap());
        assertEquals(2, result.size());
        assertEquals(1, result.get("a"));
    }

    @Test
    public void testToMap_FromEntryWithMergeFunction() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("a", 2));
        Map<String, Integer> result = entries.stream().collect(Collectors.toMap(Integer::sum));
        assertEquals(3, result.get("a"));
    }

    @Test
    public void testToMap_FromEntryWithMergeFunctionAndMapFactory() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("a", 2));
        TreeMap<String, Integer> result = entries.stream().collect(Collectors.toMap(Integer::sum, TreeMap::new));
        assertEquals(3, result.get("a"));
        assertTrue(result instanceof TreeMap);
    }

    @Test
    public void testToMap_WithAllParameters() {
        TreeMap<Integer, String> result = stringList.stream()
                .collect(Collectors.toMap(String::length, Function.identity(), (a, b) -> a + "," + b, TreeMap::new));
        assertTrue(result instanceof TreeMap);
    }

    @Test
    public void testToMap_WithKeyValueMappers() {
        Map<String, Integer> result = stringList.stream().collect(Collectors.toMap(Function.identity(), String::length));
        assertEquals(5, result.size());
        assertEquals(5, result.get("apple"));
    }

    @Test
    public void testToMap_WithKeyValueMappersAndMergeFunction() {
        Map<Integer, String> result = stringList.stream().collect(Collectors.toMap(String::length, Function.identity(), (a, b) -> a + "," + b));
        assertTrue(result.containsKey(6));
    }

    @Test
    public void testToMap_WithMergeFunction_Parallel_CombinerUsed() {
        // Triggers lambda$299 (mapMerger combiner) in parallel
        Map<Integer, String> result = Stream.of("a", "bb", "ccc", "dd", "eee", "ff")
                .parallel()
                .collect(Collectors.toMap(String::length, s -> s, (s1, s2) -> s1));
        assertNotNull(result);
        assertTrue(result.containsKey(1));
    }

    @Test
    public void testToMapFromEntries() {
        Map<String, Integer> map = Stream.of(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2)).collect(Collectors.toMap());

        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals(1, map.get("a"));
        Assertions.assertEquals(2, map.get("b"));
    }

    @Test
    public void testToMapFromEntries_WithMapFactory() {
        java.util.TreeMap<String, Integer> result = Stream.of(new AbstractMap.SimpleEntry<>("b", 2), new AbstractMap.SimpleEntry<>("a", 1))
                .collect(Collectors.toMap(java.util.TreeMap::new));
        assertEquals("a", result.firstKey());
    }

    @Test
    public void testToMapFromEntries_WithMerge() {
        Map<String, Integer> result = Stream.of(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("a", 2))
                .collect(Collectors.toMap(Integer::sum));
        assertEquals(1, result.size());
        assertEquals(3, result.get("a").intValue());
    }

    @Test
    public void testToMapFromEntriesWithMapFactory() {
        TreeMap<String, Integer> map = Stream.of(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2))
                .collect(Collectors.toMap(TreeMap::new));

        Assertions.assertTrue(map instanceof TreeMap);
        Assertions.assertEquals(1, map.get("a"));
    }

    @Test
    public void testToMapFromEntriesWithMergeFunction() {
        Map<String, Integer> map = Stream
                .of(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("a", 2), new AbstractMap.SimpleEntry<>("b", 3))
                .collect(Collectors.toMap(Integer::sum));

        Assertions.assertEquals(3, map.get("a"));
        Assertions.assertEquals(3, map.get("b"));
    }

    @Test
    public void testToMapFromEntriesWithMergeFunctionAndMapFactory() {
        TreeMap<String, Integer> map = Stream
                .of(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("a", 2), new AbstractMap.SimpleEntry<>("b", 3))
                .collect(Collectors.toMap(Integer::sum, TreeMap::new));

        Assertions.assertTrue(map instanceof TreeMap);
        Assertions.assertEquals(3, map.get("a"));
    }

    @Test
    public void testToMapWithAllParameters() {
        LinkedHashMap<Integer, String> map = Stream.of("a", "bb", "ccc", "dd")
                .collect(Collectors.toMap(String::length, e -> e, (s1, s2) -> s1 + "," + s2, LinkedHashMap::new));

        Assertions.assertTrue(map instanceof LinkedHashMap);
        Assertions.assertEquals("bb,dd", map.get(2));
    }

    @Test
    public void testToMapWithKeyValueMappers() {
        Map<String, Integer> map = Stream.of("a", "bb", "ccc").collect(Collectors.toMap(e -> e, String::length));

        Assertions.assertEquals(1, map.get("a"));
        Assertions.assertEquals(2, map.get("bb"));
        Assertions.assertEquals(3, map.get("ccc"));
    }

    @Test
    public void testToMapWithKeyValueMappersAndMapFactory() {
        Map<String, Integer> map = Stream.of("a", "bb", "ccc").collect(Collectors.toMap(e -> e, String::length, Suppliers.ofLinkedHashMap()));

        Assertions.assertTrue(map instanceof LinkedHashMap);
        Assertions.assertEquals(1, map.get("a"));
    }

    @Test
    public void testToMapWithKeyValueMappersAndMergeFunction() {
        Map<Integer, String> map = Stream.of("a", "bb", "ccc", "dd").collect(Collectors.toMap(String::length, e -> e, (s1, s2) -> s1 + "," + s2));

        Assertions.assertEquals("bb,dd", map.get(2));
    }

    @Test
    public void testToMapWithMapFactory() {
        TreeMap<String, Integer> result = stringList.stream().collect(Collectors.toMap(Function.identity(), String::length, TreeMap::new));
        assertTrue(result instanceof TreeMap);
        assertEquals(5, result.size());
    }

    // toMultimap

    @Test
    public void testGroupingBy_WithDownstream_Parallel_toMultimapCombiner() {
        // additional parallel test for toMultimap combiner path
        ListMultimap<String, Integer> result = Stream.of("a", "b", "c", "a", "b", "d", "e", "f")
                .parallel()
                .collect(Collectors.toMultimap(s -> s, s -> s.length()));
        assertNotNull(result);
        assertEquals(2, result.get("a").size());
    }

    @Test
    public void testToMultimap() {
        Map<String, Integer> source = new IdentityHashMap<>();
        source.put(new String("a"), 1);
        source.put(new String("a"), 2);
        source.put(new String("b"), 3);

        ListMultimap<String, Integer> result = source.entrySet().stream().collect(Collectors.toMultimap());
        assertEquals(2, result.get("a").size());
        assertEquals(1, result.get("b").size());
    }

    @Test
    public void testToMultimap_FromEntry() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("a", 2),
                new AbstractMap.SimpleEntry<>("b", 3));
        ListMultimap<String, Integer> result = entries.stream().collect(Collectors.toMultimap());
        assertEquals(2, result.get("a").size());
        assertEquals(1, result.get("b").size());
    }

    @Test
    public void testToMultimap_Parallel_CombinerUsed() {
        // Triggers lambda$300 (multimapMerger combiner) in parallel
        ListMultimap<Integer, String> result = Stream.of("a", "bb", "ccc", "dd", "eee", "ff").parallel().collect(Collectors.toMultimap(String::length));
        assertNotNull(result);
        assertEquals(Arrays.asList("a"), result.get(1));
    }

    @Test
    public void testToMultimap_WithKeyMapper() {
        ListMultimap<String, Person> result = personList.stream().collect(Collectors.toMultimap(Person::getDepartment));
        assertEquals(2, result.get("Engineering").size());
        assertEquals(2, result.get("Sales").size());
    }

    @Test
    public void testToMultimap_WithKeyValueMappers() {
        ListMultimap<String, Integer> result = personList.stream().collect(Collectors.toMultimap(Person::getDepartment, Person::getAge));
        assertEquals(2, result.get("Engineering").size());
        assertTrue(result.get("Engineering").contains(25));
        assertTrue(result.get("Engineering").contains(30));
    }

    @Test
    public void testToMultimap_WithKeyValueMappers_Parallel_CombinerUsed() {
        // Triggers lambda$294/298 (flatMappingToMultimap combiner) in parallel
        ListMultimap<Integer, String> result = Stream.of("a", "bb", "ccc", "dd").parallel().collect(Collectors.toMultimap(String::length, s -> s));
        assertNotNull(result);
        assertEquals(2, result.get(2).size()); // "bb" and "dd"
    }

    @Test
    public void testToMultimapFromEntries() {
        ListMultimap<String, Integer> multimap = Stream
                .of(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("a", 2), new AbstractMap.SimpleEntry<>("b", 3))
                .collect(Collectors.toMultimap());

        Assertions.assertEquals(Arrays.asList(1, 2), multimap.get("a"));
        Assertions.assertEquals(Arrays.asList(3), multimap.get("b"));
    }

    @Test
    public void testToMultimapFromEntriesWithMapFactory() {
        ListMultimap<String, Integer> multimap = Stream
                .of(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("a", 2), new AbstractMap.SimpleEntry<>("b", 3))
                .collect(Collectors.toMultimap(Suppliers.ofListMultimap()));

        Assertions.assertEquals(Arrays.asList(1, 2), multimap.get("a"));
    }

    @Test
    public void testToMultimapWithKeyMapper() {
        ListMultimap<Integer, String> multimap = Stream.of("a", "bb", "ccc", "dd").collect(Collectors.toMultimap(String::length));

        Assertions.assertEquals(Arrays.asList("bb", "dd"), multimap.get(2));
    }

    @Test
    public void testToMultimapWithKeyMapperAndMapFactory() {
        ListMultimap<Integer, String> multimap = Stream.of("a", "bb", "ccc", "dd").collect(Collectors.toMultimap(String::length, Suppliers.ofListMultimap()));

        Assertions.assertEquals(Arrays.asList("bb", "dd"), multimap.get(2));
    }

    @Test
    public void testToMultimapWithKeyValueMappers() {
        ListMultimap<Integer, String> multimap = Stream.of("apple", "apricot", "banana").collect(Collectors.toMultimap(s -> s.charAt(0) - 'a', e -> e));

        Assertions.assertEquals(Arrays.asList("apple", "apricot"), multimap.get(0));
        Assertions.assertEquals(Arrays.asList("banana"), multimap.get(1));
    }

    @Test
    public void testToMultimapWithKeyValueMappersAndMapFactory() {
        ListMultimap<Integer, String> multimap = Stream.of("apple", "apricot", "banana")
                .collect(Collectors.toMultimap(s -> s.charAt(0) - 'a', e -> e, Suppliers.ofListMultimap()));

        Assertions.assertEquals(Arrays.asList("apple", "apricot"), multimap.get(0));
    }

    @Test
    public void testToMultimapPreservesPerKeyEncounterOrder() {
        // regression: the toMultimap/flatMapping*ToMultimap family documents per-key values in
        // encounter order (ListMultimap by default), but advertised UNORDERED, which permits
        // order-discarding optimizations in JDK parallel pipelines
        assertFalse(Collectors.<Object, Object, Object> toMultimap(Fn.identity(), Fn.identity()).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.<Object, Object, Object> flatMappingValueToMultimap(Fn.identity(), t -> Stream.of(t))
                .characteristics()
                .contains(Characteristics.UNORDERED));
        assertFalse(Collectors.<Object, Object, Object> flatmappingValueToMultimap(Fn.identity(), t -> N.asList(t))
                .characteristics()
                .contains(Characteristics.UNORDERED));
        assertFalse(Collectors.<Object, Object, Object> flatMappingKeyToMultimap(t -> Stream.of(t), Fn.identity())
                .characteristics()
                .contains(Characteristics.UNORDERED));
        assertFalse(Collectors.<Object, Object, Object> flatmappingKeyToMultimap(t -> N.asList(t), Fn.identity())
                .characteristics()
                .contains(Characteristics.UNORDERED));

        // IDENTITY_FINISH is retained
        assertTrue(Collectors.<Object, Object, Object> toMultimap(Fn.identity(), Fn.identity()).characteristics().contains(Characteristics.IDENTITY_FINISH));

        // per-key value lists preserve encounter order in a JDK parallel pipeline (also exercises
        // the multimapMerger combiner path)
        final ListMultimap<Integer, Integer> result = java.util.stream.IntStream.range(0, 2000)
                .boxed()
                .parallel()
                .collect(Collectors.<Integer, Integer, Integer> toMultimap(i -> i % 2, i -> i));

        final List<Integer> expectedEvens = new ArrayList<>();
        for (int i = 0; i < 2000; i += 2) {
            expectedEvens.add(i);
        }

        assertEquals(expectedEvens, result.get(0));
        assertEquals(1000, result.get(1).size());
    }

    // toMultiset

    @Test
    public void testToMultiset() {
        List<String> duplicates = Arrays.asList("a", "b", "a", "c", "b", "a");
        Multiset<String> result = duplicates.stream().collect(Collectors.toMultiset());
        assertEquals(3, result.getCount("a"));
        assertEquals(2, result.getCount("b"));
        assertEquals(1, result.getCount("c"));
    }

    @Test
    public void testToMultiset_WithSupplier() {
        List<String> duplicates = Arrays.asList("a", "b", "a", "c", "b", "a");
        Multiset<String> result = duplicates.stream().collect(Collectors.toMultiset(Multiset::new));
        assertEquals(3, result.getCount("a"));
        assertEquals(2, result.getCount("b"));
    }

    @Test
    public void testToMultisetWithSupplier() {
        Multiset<String> result = Stream.of("a", "b", "a", "c", "a").collect(Collectors.toMultiset(Multiset::new));

        Assertions.assertEquals(3, result.getCount("a"));
    }

    @Test
    public void test_toPrimitiveArrays() {
        assertArrayEquals(new boolean[] { true, false }, Stream.of(true, false).collect(Collectors.toBooleanArray()));
        assertArrayEquals(new char[] { 'a', 'b' }, Stream.of('a', 'b').collect(Collectors.toCharArray()));
        assertArrayEquals(new byte[] { 1, 2 }, Stream.of((byte) 1, (byte) 2).collect(Collectors.toByteArray()));
        assertArrayEquals(new short[] { 1, 2 }, Stream.of((short) 1, (short) 2).collect(Collectors.toShortArray()));
        assertArrayEquals(new int[] { 1, 2 }, Stream.of(1, 2).collect(Collectors.toIntArray()));
        assertArrayEquals(new long[] { 1L, 2L }, Stream.of(1L, 2L).collect(Collectors.toLongArray()));
        assertArrayEquals(new float[] { 1.0f, 2.0f }, Stream.of(1.0f, 2.0f).collect(Collectors.toFloatArray()));
        assertArrayEquals(new double[] { 1.0, 2.0 }, Stream.of(1.0, 2.0).collect(Collectors.toDoubleArray()));
    }

    @Test
    public void testToQueue() {
        Queue<String> result = stringList.stream().collect(Collectors.toQueue());
        assertEquals(stringList.size(), result.size());
        assertEquals("apple", result.poll());
    }

    // toSet

    @Test
    public void testToSet() {
        Set<String> result = stringList.stream().collect(Collectors.toSet());
        assertEquals(new HashSet<>(stringList), result);
    }

    @Test
    public void test_toSet() {
        Set<Integer> set = Stream.of(1, 2, 3, 2, 1).collect(Collectors.toSet());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
    }

    @Test
    public void testToSet_Empty() {
        Set<String> result = Stream.<String> empty().collect(Collectors.toSet());
        assertTrue(result.isEmpty());
    }



    // Stream review 2026-09-09 (pass B) - toSet(int) allocation

    /**
     * {@code toSet(int atMostSize)} used to hand {@code atMostSize} straight to {@code N.newHashSet}, sizing the
     * hash table for the whole bound however few elements actually arrived, so a 3-element stream collected with
     * {@code toSet(Integer.MAX_VALUE)} exhausted the heap. Its siblings all cap: {@code toList(int)} and
     * {@code first(int)} at {@code N.min(256, n)}, {@code maxAll(cmp, int)} at {@code Math.min(16, n)}.
     *
     * <p>Measured on a 3-element stream before the fix: {@code toSet(20_000_000)} retained ~144 MB against
     * ~26 MB for {@code toList(20_000_000)}; after the fix, ~4 MB. The threshold below is deliberately loose -
     * it only has to separate "sized for the bound" from "sized for what arrived".
     */
    @Test
    public void testToSet_withHugeAtMostSizeDoesNotPreallocateForTheWholeBound() {
        final int hugeBound = 20_000_000;

        // correctness first: the bound itself still behaves
        assertEquals(N.asSet(1, 2, 3), Stream.of(1, 2, 3).collect(Collectors.toSet(hugeBound)));
        assertEquals(2, Stream.of(1, 2, 2, 3).collect(Collectors.toSet(2)).size());
        assertThrows(IllegalArgumentException.class, () -> Collectors.toSet(-1));

        Stream.of(1, 2, 3).collect(Collectors.toSet(hugeBound)); // warm up the classes before measuring
        System.gc();

        final Runtime rt = Runtime.getRuntime();
        final long before = rt.totalMemory() - rt.freeMemory();
        final Set<Integer> collected = Stream.of(1, 2, 3).collect(Collectors.toSet(hugeBound));
        final long delta = rt.totalMemory() - rt.freeMemory() - before;

        assertEquals(3, collected.size());
        assertTrue(delta < 32L * 1024 * 1024, "toSet(" + hugeBound + ") on a 3-element stream retained " + delta + " bytes");
    }

    @Test
    public void testToSet_WithLimit() {
        Set<String> result = stringList.stream().collect(Collectors.toSet(3));
        assertEquals(3, result.size());
    }

    @Test
    public void testToSetWithLimit() {
        Set<Integer> result = Stream.of(1, 2, 3, 4, 5).collect(Collectors.toSet(3));

        Assertions.assertTrue(result.size() <= 3);
    }

    @Test
    public void testToShortArray() {
        short[] result = shortList.stream().collect(Collectors.toShortArray());
        assertEquals(shortList.size(), result.length);
        assertEquals((short) 10, result[0]);
    }

    @Test
    public void testToShortList() {
        ShortList result = shortList.stream().collect(Collectors.toShortList());
        assertEquals(shortList.size(), result.size());
        assertEquals((short) 10, result.get(0));
    }

    @Test
    public void testToUnmodifiableList() {
        List<String> result = stringList.stream().collect(Collectors.toUnmodifiableList());
        assertEquals(stringList, result);
        assertThrows(UnsupportedOperationException.class, () -> result.add("test"));
    }

    @Test
    public void test_toUnmodifiableList() {
        List<Integer> list = Stream.of(1, 2, 3).collect(Collectors.toUnmodifiableList());
        assertEquals(Arrays.asList(1, 2, 3), list);
        assertThrows(UnsupportedOperationException.class, () -> list.add(4));
    }

    // toUnmodifiableMap

    @Test
    public void testToUnmodifiableMap() {
        Map<String, Integer> result = stringList.stream().collect(Collectors.toUnmodifiableMap(Function.identity(), String::length));
        assertEquals(5, result.size());
        assertThrows(UnsupportedOperationException.class, () -> result.put("test", 4));
    }

    @Test
    public void testToUnmodifiableMap_WithMergeFunction() {
        Map<Integer, String> result = stringList.stream().collect(Collectors.toUnmodifiableMap(String::length, Function.identity(), (a, b) -> a));
        assertNotNull(result.get(6));
        assertThrows(UnsupportedOperationException.class, () -> result.put(10, "test"));
    }

    @Test
    public void testToUnmodifiableMapWithMergeFunction() {
        Map<Integer, String> map = Stream.of("a", "bb", "ccc", "dd").collect(Collectors.toUnmodifiableMap(String::length, e -> e, (s1, s2) -> s1 + "," + s2));

        Assertions.assertEquals("bb,dd", map.get(2));
    }

    @Test
    public void testToUnmodifiableSet() {
        Set<String> result = stringList.stream().collect(Collectors.toUnmodifiableSet());
        assertEquals(new HashSet<>(stringList), result);
        assertThrows(UnsupportedOperationException.class, () -> result.add("test"));
    }

    @Test
    public void test_toUnmodifiableSet() {
        Set<Integer> set = Stream.of(1, 2, 3, 2).collect(Collectors.toUnmodifiableSet());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
        assertThrows(UnsupportedOperationException.class, () -> set.add(4));
    }

}
