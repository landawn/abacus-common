package com.landawn.abacus.util.stream;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.BiMap;
import com.landawn.abacus.util.BigDecimalSummaryStatistics;
import com.landawn.abacus.util.BigIntegerSummaryStatistics;
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.Collectors.MoreCollectors;

public class Collectors101Test extends TestBase {

    @Test
    public void testCreate() {
        // Test create with supplier, accumulator, combiner
        Supplier<List<String>> supplier = ArrayList::new;
        BiConsumer<List<String>, String> accumulator = List::add;
        BinaryOperator<List<String>> combiner = (list1, list2) -> {
            list1.addAll(list2);
            return list1;
        };

        Collector<String, List<String>, List<String>> collector = Collectors.create(supplier, accumulator, combiner);

        Assertions.assertNotNull(collector);
        Assertions.assertNotNull(collector.supplier());
        Assertions.assertNotNull(collector.accumulator());
        Assertions.assertNotNull(collector.combiner());
    }

    @Test
    public void testCreateWithCharacteristics() {
        // Test create with characteristics array
        Supplier<Set<String>> supplier = HashSet::new;
        BiConsumer<Set<String>, String> accumulator = Set::add;
        BinaryOperator<Set<String>> combiner = (set1, set2) -> {
            set1.addAll(set2);
            return set1;
        };

        Collector<String, Set<String>, Set<String>> collector = Collectors.create(supplier, accumulator, combiner, Collector.Characteristics.UNORDERED);

        Assertions.assertNotNull(collector);
        Assertions.assertTrue(collector.characteristics().contains(Collector.Characteristics.UNORDERED));
    }

    @Test
    public void testCreateWithCharacteristicsCollection() {
        // Test create with characteristics collection
        Supplier<Set<String>> supplier = HashSet::new;
        BiConsumer<Set<String>, String> accumulator = Set::add;
        BinaryOperator<Set<String>> combiner = (set1, set2) -> {
            set1.addAll(set2);
            return set1;
        };
        Collection<Collector.Characteristics> characteristics = Arrays.asList(Collector.Characteristics.UNORDERED);

        Collector<String, Set<String>, Set<String>> collector = Collectors.create(supplier, accumulator, combiner, characteristics);

        Assertions.assertNotNull(collector);
        Assertions.assertTrue(collector.characteristics().contains(Collector.Characteristics.UNORDERED));
    }

    @Test
    public void testCreateWithFinisher() {
        // Test create with finisher
        Supplier<List<String>> supplier = ArrayList::new;
        BiConsumer<List<String>, String> accumulator = List::add;
        BinaryOperator<List<String>> combiner = (list1, list2) -> {
            list1.addAll(list2);
            return list1;
        };
        Function<List<String>, String> finisher = list -> String.join(",", list);

        Collector<String, List<String>, String> collector = Collectors.create(supplier, accumulator, combiner, finisher);

        Assertions.assertNotNull(collector);
        Assertions.assertNotNull(collector.finisher());
    }

    @Test
    public void testCreateWithFinisherAndCharacteristics() {
        // Test create with finisher and characteristics
        Supplier<List<String>> supplier = ArrayList::new;
        BiConsumer<List<String>, String> accumulator = List::add;
        BinaryOperator<List<String>> combiner = (list1, list2) -> {
            list1.addAll(list2);
            return list1;
        };
        Function<List<String>, String> finisher = list -> String.join(",", list);
        Collection<Collector.Characteristics> characteristics = Arrays.asList(Collector.Characteristics.CONCURRENT);

        Collector<String, List<String>, String> collector = Collectors.create(supplier, accumulator, combiner, finisher, characteristics);

        Assertions.assertNotNull(collector);
        Assertions.assertTrue(collector.characteristics().contains(Collector.Characteristics.CONCURRENT));
    }

    @Test
    public void testToCollection() {
        List<String> result = Stream.of("a", "b", "c").collect(Collectors.toCollection(ArrayList::new));

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);
        Assertions.assertTrue(result instanceof ArrayList);
    }

    @Test
    public void testToCollectionWithLimit() {
        List<String> result = Stream.of("a", "b", "c", "d", "e").collect(Collectors.toCollection(3, ArrayList::new));

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);
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
    public void testToList() {
        List<String> result = Stream.of("a", "b", "c").collect(Collectors.toList());

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testToLinkedList() {
        LinkedList<String> result = Stream.of("a", "b", "c").collect(Collectors.toLinkedList());

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);
        Assertions.assertTrue(result instanceof LinkedList);
    }

    @Test
    public void testToImmutableList() {
        ImmutableList<String> result = Stream.of("a", "b", "c").collect(Collectors.toImmutableList());

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> result.add("d"));
    }

    @Test
    public void testToUnmodifiableList() {
        List<String> result = Stream.of("a", "b", "c").collect(Collectors.toUnmodifiableList());

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> result.add("d"));
    }

    @Test
    public void testToSet() {
        Set<String> result = Stream.of("a", "b", "c", "a").collect(Collectors.toSet());

        Assertions.assertEquals(3, result.size());
        Assertions.assertTrue(result.contains("a"));
        Assertions.assertTrue(result.contains("b"));
        Assertions.assertTrue(result.contains("c"));
    }

    @Test
    public void testToLinkedHashSet() {
        Set<String> result = Stream.of("c", "a", "b", "a").collect(Collectors.toLinkedHashSet());

        Assertions.assertEquals(3, result.size());
        Iterator<String> iter = result.iterator();
        Assertions.assertEquals("c", iter.next());
        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("b", iter.next());
    }

    @Test
    public void testToImmutableSet() {
        ImmutableSet<String> result = Stream.of("a", "b", "c", "a").collect(Collectors.toImmutableSet());

        Assertions.assertEquals(3, result.size());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> result.add("d"));
    }

    @Test
    public void testToUnmodifiableSet() {
        Set<String> result = Stream.of("a", "b", "c", "a").collect(Collectors.toUnmodifiableSet());

        Assertions.assertEquals(3, result.size());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> result.add("d"));
    }

    @Test
    public void testToQueue() {
        Queue<String> result = Stream.of("a", "b", "c").collect(Collectors.toQueue());

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("a", result.poll());
        Assertions.assertEquals("b", result.poll());
        Assertions.assertEquals("c", result.poll());
    }

    @Test
    public void testToDeque() {
        Deque<String> result = Stream.of("a", "b", "c").collect(Collectors.toDeque());

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals("a", result.pollFirst());
        Assertions.assertEquals("c", result.pollLast());
    }

    @Test
    public void testToListWithLimit() {
        List<String> result = Stream.of("a", "b", "c", "d", "e").collect(Collectors.toList(3));

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testToSetWithLimit() {
        Set<Integer> result = Stream.of(1, 2, 3, 4, 5).collect(Collectors.toSet(3));

        Assertions.assertTrue(result.size() <= 3);
    }

    @Test
    public void testToMultiset() {
        Multiset<String> result = Stream.of("a", "b", "a", "c", "a").collect(Collectors.toMultiset());

        Assertions.assertEquals(3, result.getCount("a"));
        Assertions.assertEquals(1, result.getCount("b"));
        Assertions.assertEquals(1, result.getCount("c"));
    }

    @Test
    public void testToMultisetWithSupplier() {
        Multiset<String> result = Stream.of("a", "b", "a", "c", "a").collect(Collectors.toMultiset(Multiset::new));

        Assertions.assertEquals(3, result.getCount("a"));
    }

    @Test
    public void testToArray() {
        Object[] result = Stream.of("a", "b", "c").collect(Collectors.toArray());

        Assertions.assertArrayEquals(new Object[] { "a", "b", "c" }, result);
    }

    @Test
    public void testToArrayWithSupplier() {
        String[] result = Stream.of("a", "b", "c").collect(Collectors.toArray(() -> new String[0]));

        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, result);
    }

    @Test
    public void testToArrayWithIntFunction() {
        String[] result = Stream.of("a", "b", "c").collect(Collectors.toArray(String[]::new));

        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, result);
    }

    @Test
    public void testToBooleanList() {
        BooleanList result = Stream.of(true, false, true).collect(Collectors.toBooleanList());

        Assertions.assertEquals(3, result.size());
        Assertions.assertTrue(result.get(0));
        Assertions.assertFalse(result.get(1));
        Assertions.assertTrue(result.get(2));
    }

    @Test
    public void testToBooleanArray() {
        boolean[] result = Stream.of(true, false, true).collect(Collectors.toBooleanArray());

        Assertions.assertArrayEquals(new boolean[] { true, false, true }, result);
    }

    @Test
    public void testToCharList() {
        CharList result = Stream.of('a', 'b', 'c').collect(Collectors.toCharList());

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals('a', result.get(0));
        Assertions.assertEquals('b', result.get(1));
        Assertions.assertEquals('c', result.get(2));
    }

    @Test
    public void testToCharArray() {
        char[] result = Stream.of('a', 'b', 'c').collect(Collectors.toCharArray());

        Assertions.assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testToByteList() {
        ByteList result = Stream.of((byte) 1, (byte) 2, (byte) 3).collect(Collectors.toByteList());

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(1, result.get(0));
        Assertions.assertEquals(2, result.get(1));
        Assertions.assertEquals(3, result.get(2));
    }

    @Test
    public void testToByteArray() {
        byte[] result = Stream.of((byte) 1, (byte) 2, (byte) 3).collect(Collectors.toByteArray());

        Assertions.assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testToShortList() {
        ShortList result = Stream.of((short) 1, (short) 2, (short) 3).collect(Collectors.toShortList());

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(1, result.get(0));
        Assertions.assertEquals(2, result.get(1));
        Assertions.assertEquals(3, result.get(2));
    }

    @Test
    public void testToShortArray() {
        short[] result = Stream.of((short) 1, (short) 2, (short) 3).collect(Collectors.toShortArray());

        Assertions.assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testToIntList() {
        IntList result = Stream.of(1, 2, 3).collect(Collectors.toIntList());

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(1, result.get(0));
        Assertions.assertEquals(2, result.get(1));
        Assertions.assertEquals(3, result.get(2));
    }

    @Test
    public void testToIntArray() {
        int[] result = Stream.of(1, 2, 3).collect(Collectors.toIntArray());

        Assertions.assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testToLongList() {
        LongList result = Stream.of(1L, 2L, 3L).collect(Collectors.toLongList());

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(1L, result.get(0));
        Assertions.assertEquals(2L, result.get(1));
        Assertions.assertEquals(3L, result.get(2));
    }

    @Test
    public void testToLongArray() {
        long[] result = Stream.of(1L, 2L, 3L).collect(Collectors.toLongArray());

        Assertions.assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testToFloatList() {
        FloatList result = Stream.of(1.0f, 2.0f, 3.0f).collect(Collectors.toFloatList());

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(1.0f, result.get(0));
        Assertions.assertEquals(2.0f, result.get(1));
        Assertions.assertEquals(3.0f, result.get(2));
    }

    @Test
    public void testToFloatArray() {
        float[] result = Stream.of(1.0f, 2.0f, 3.0f).collect(Collectors.toFloatArray());

        Assertions.assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
    }

    @Test
    public void testToDoubleList() {
        DoubleList result = Stream.of(1.0, 2.0, 3.0).collect(Collectors.toDoubleList());

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(1.0, result.get(0));
        Assertions.assertEquals(2.0, result.get(1));
        Assertions.assertEquals(3.0, result.get(2));
    }

    @Test
    public void testToDoubleArray() {
        double[] result = Stream.of(1.0, 2.0, 3.0).collect(Collectors.toDoubleArray());

        Assertions.assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, result);
    }

    @Test
    public void testOnlyOne() {
        Optional<String> result = Stream.of("unique").collect(Collectors.onlyOne());

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("unique", result.get());

        // Test empty stream
        Optional<String> empty = Stream.<String> empty().collect(Collectors.onlyOne());
        Assertions.assertFalse(empty.isPresent());

        // Test multiple elements throws exception
        Assertions.assertThrows(TooManyElementsException.class, () -> Stream.of("a", "b").collect(Collectors.onlyOne()));
    }

    @Test
    public void testOnlyOneWithPredicate() {
        Optional<Integer> result = Stream.of(1, 2, 3, 4).collect(Collectors.onlyOne(n -> n > 3));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.get());
    }

    @Test
    public void testFirst() {
        Optional<String> result = Stream.of("a", "b", "c").collect(Collectors.first());

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("a", result.get());

        Optional<String> empty = Stream.<String> empty().collect(Collectors.first());
        Assertions.assertFalse(empty.isPresent());
    }

    @Test
    public void testLast() {
        Optional<String> result = Stream.of("a", "b", "c").collect(Collectors.last());

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("c", result.get());

        Optional<String> empty = Stream.<String> empty().collect(Collectors.last());
        Assertions.assertFalse(empty.isPresent());
    }

    @Test
    public void testFirstN() {
        List<String> result = Stream.of("a", "b", "c", "d", "e").collect(Collectors.first(3));

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);

        // Test with more elements requested than available
        List<String> all = Stream.of("a", "b").collect(Collectors.first(5));
        Assertions.assertEquals(Arrays.asList("a", "b"), all);
    }

    @Test
    public void testLastN() {
        List<String> result = Stream.of("a", "b", "c", "d", "e").collect(Collectors.last(3));

        Assertions.assertEquals(Arrays.asList("c", "d", "e"), result);

        // Test with more elements requested than available
        List<String> all = Stream.of("a", "b").collect(Collectors.last(5));
        Assertions.assertEquals(Arrays.asList("a", "b"), all);
    }

    @Test
    public void testJoining() {
        String result = Stream.of("a", "b", "c").collect(Collectors.joining());

        Assertions.assertEquals("abc", result);
    }

    @Test
    public void testJoiningWithDelimiter() {
        String result = Stream.of("a", "b", "c").collect(Collectors.joining(", "));

        Assertions.assertEquals("a, b, c", result);
    }

    @Test
    public void testJoiningWithDelimiterPrefixSuffix() {
        String result = Stream.of("a", "b", "c").collect(Collectors.joining(", ", "[", "]"));

        Assertions.assertEquals("[a, b, c]", result);
    }

    @Test
    public void testFiltering() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).collect(Collectors.filtering(n -> n % 2 == 0, Collectors.toList()));

        Assertions.assertEquals(Arrays.asList(2, 4), result);
    }

    @Test
    public void testFilteringToList() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).collect(Collectors.filteringToList(n -> n > 3));

        Assertions.assertEquals(Arrays.asList(4, 5), result);
    }

    @Test
    public void testMapping() {
        List<Integer> result = Stream.of("a", "bb", "ccc").collect(Collectors.mapping(String::length, Collectors.toList()));

        Assertions.assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testMappingToList() {
        List<Integer> result = Stream.of("a", "bb", "ccc").collect(Collectors.mappingToList(String::length));

        Assertions.assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testFlatMapping() {
        List<Integer> result = Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4))
                .collect(Collectors.flatMapping(list -> Stream.of(list), Collectors.toList()));

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlatMappingToList() {
        List<Integer> result = Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4)).collect(Collectors.flatMappingToList(list -> Stream.of(list)));

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlatmappingWithCollection() {
        List<Integer> result = Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4)).collect(Collectors.flatmapping(e -> e, Collectors.toList()));

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlatmappingToListWithCollection() {
        List<Integer> result = Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4)).collect(Collectors.flatmappingToList(e -> e));

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlatMappingWithBiFunction() {
        List<String> result = Stream.of("a", "b").collect(Collectors.flatMapping(s -> Stream.of(1, 2), (s, n) -> s + n, Collectors.toList()));

        Assertions.assertEquals(Arrays.asList("a1", "a2", "b1", "b2"), result);
    }

    @Test
    public void testFlatMappingToListWithBiFunction() {
        List<String> result = Stream.of("a", "b").collect(Collectors.flatMappingToList(s -> Stream.of(1, 2), (s, n) -> s + n));

        Assertions.assertEquals(Arrays.asList("a1", "a2", "b1", "b2"), result);
    }

    @Test
    public void testFlatmappingWithBiFunctionAndCollection() {
        List<String> result = Stream.of("a", "b").collect(Collectors.flatmapping(s -> Arrays.asList(1, 2), (s, n) -> s + n, Collectors.toList()));

        Assertions.assertEquals(Arrays.asList("a1", "a2", "b1", "b2"), result);
    }

    @Test
    public void testFlatmappingToListWithBiFunctionAndCollection() {
        List<String> result = Stream.of("a", "b").collect(Collectors.flatmappingToList(s -> Arrays.asList(1, 2), (s, n) -> s + n));

        Assertions.assertEquals(Arrays.asList("a1", "a2", "b1", "b2"), result);
    }

    @Test
    public void testCollectingAndThen() {
        Integer size = Stream.of("a", "b", "c").collect(Collectors.collectingAndThen(Collectors.toList(), List::size));

        Assertions.assertEquals(3, size);
    }

    @Test
    public void testCollectingOrEmpty() {
        Optional<List<String>> result = Stream.of("a", "b", "c").collect(Collectors.collectingOrEmpty(Collectors.toList()));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result.get());

        Optional<List<String>> empty = Stream.<String> empty().collect(Collectors.collectingOrEmpty(Collectors.toList()));

        Assertions.assertFalse(empty.isPresent());
    }

    @Test
    public void testCollectingOrElseIfEmpty() {
        List<String> result = Stream.of("a", "b", "c").collect(Collectors.collectingOrElseIfEmpty(Collectors.toList(), Arrays.asList("default")));

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);

        List<String> defaultResult = Stream.<String> empty().collect(Collectors.collectingOrElseIfEmpty(Collectors.toList(), Arrays.asList("default")));

        Assertions.assertEquals(Arrays.asList("default"), defaultResult);
    }

    @Test
    public void testCollectingOrElseGetIfEmpty() {
        List<String> result = Stream.of("a", "b", "c").collect(Collectors.collectingOrElseGetIfEmpty(Collectors.toList(), () -> Arrays.asList("default")));

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);

        List<String> defaultResult = Stream.<String> empty()
                .collect(Collectors.collectingOrElseGetIfEmpty(Collectors.toList(), () -> Arrays.asList("default")));

        Assertions.assertEquals(Arrays.asList("default"), defaultResult);
    }

    @Test
    public void testCollectingOrElseThrowIfEmpty() {
        List<String> result = Stream.of("a", "b", "c").collect(Collectors.collectingOrElseThrowIfEmpty(Collectors.toList()));

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);

        Assertions.assertThrows(NoSuchElementException.class,
                () -> Stream.<String> empty().collect(Collectors.collectingOrElseThrowIfEmpty(Collectors.toList())));
    }

    @Test
    public void testCollectingOrElseThrowIfEmptyWithSupplier() {
        List<String> result = Stream.of("a", "b", "c")
                .collect(Collectors.collectingOrElseThrowIfEmpty(Collectors.toList(), () -> new IllegalStateException("Empty stream")));

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);

        Assertions.assertThrows(IllegalStateException.class, () -> Stream.<String> empty()
                .collect(Collectors.collectingOrElseThrowIfEmpty(Collectors.toList(), () -> new IllegalStateException("Empty stream"))));
    }

    @Test
    public void testDistinctByToList() {
        List<String> result = Stream.of("a", "bb", "ccc", "dd", "e").collect(Collectors.distinctByToList(String::length));

        Assertions.assertEquals(Arrays.asList("a", "bb", "ccc"), result);
    }

    @Test
    public void testDistinctByToCollection() {
        Set<String> result = Stream.of("a", "bb", "ccc", "dd", "e").collect(Collectors.distinctByToCollection(String::length, HashSet::new));

        Assertions.assertEquals(3, result.size());
        Assertions.assertTrue(result.contains("a"));
        Assertions.assertTrue(result.contains("bb") || result.contains("dd"));
        Assertions.assertTrue(result.contains("ccc"));
    }

    @Test
    public void testDistinctByToCounting() {
        Integer count = Stream.of("a", "bb", "ccc", "dd", "e").collect(Collectors.distinctByToCounting(String::length));

        Assertions.assertEquals(3, count);
    }

    @Test
    public void testCounting() {
        Long count = Stream.of("a", "b", "c").collect(Collectors.counting());

        Assertions.assertEquals(3L, count);
    }

    @Test
    public void testCountingToInt() {
        Integer count = Stream.of("a", "b", "c").collect(Collectors.countingToInt());

        Assertions.assertEquals(3, count);
    }

    @Test
    public void testMin() {
        Optional<Integer> min = Stream.of(3, 1, 4, 1, 5).collect(Collectors.min());

        Assertions.assertTrue(min.isPresent());
        Assertions.assertEquals(1, min.get());

        Optional<Integer> empty = Stream.<Integer> empty().collect(Collectors.min());
        Assertions.assertFalse(empty.isPresent());
    }

    @Test
    public void testMinWithComparator() {
        Optional<String> min = Stream.of("apple", "pie", "banana").collect(Collectors.min(Comparator.comparing(String::length)));

        Assertions.assertTrue(min.isPresent());
        Assertions.assertEquals("pie", min.get());
    }

    @Test
    public void testMinOrElse() {
        Integer min = Stream.of(3, 1, 4).collect(Collectors.minOrElse(0));

        Assertions.assertEquals(1, min);

        Integer defaultMin = Stream.<Integer> empty().collect(Collectors.minOrElse(0));
        Assertions.assertEquals(0, defaultMin);
    }

    @Test
    public void testMinOrElseWithComparator() {
        String min = Stream.of("apple", "pie", "banana").collect(Collectors.minOrElse(Comparator.comparing(String::length), "default"));

        Assertions.assertEquals("pie", min);
    }

    @Test
    public void testMinOrElseGet() {
        Integer min = Stream.of(3, 1, 4).collect(Collectors.minOrElseGet(() -> 0));

        Assertions.assertEquals(1, min);

        Integer defaultMin = Stream.<Integer> empty().collect(Collectors.minOrElseGet(() -> 0));
        Assertions.assertEquals(0, defaultMin);
    }

    @Test
    public void testMinOrElseGetWithComparator() {
        String min = Stream.of("apple", "pie", "banana").collect(Collectors.minOrElseGet(Comparator.comparing(String::length), () -> "default"));

        Assertions.assertEquals("pie", min);
    }

    @Test
    public void testMinOrElseThrow() {
        Integer min = Stream.of(3, 1, 4).collect(Collectors.minOrElseThrow());

        Assertions.assertEquals(1, min);

        Assertions.assertThrows(NoSuchElementException.class, () -> Stream.<Integer> empty().collect(Collectors.minOrElseThrow()));
    }

    @Test
    public void testMinOrElseThrowWithComparator() {
        String min = Stream.of("apple", "pie", "banana").collect(Collectors.minOrElseThrow(Comparator.comparing(String::length)));

        Assertions.assertEquals("pie", min);
    }

    @Test
    public void testMinOrElseThrowWithComparatorAndSupplier() {
        String min = Stream.of("apple", "pie", "banana")
                .collect(Collectors.minOrElseThrow(Comparator.comparing(String::length), () -> new IllegalStateException("No min")));

        Assertions.assertEquals("pie", min);

        Assertions.assertThrows(IllegalStateException.class, () -> Stream.<String> empty()
                .collect(Collectors.minOrElseThrow(Comparator.comparing(String::length), () -> new IllegalStateException("No min"))));
    }

    @Test
    public void testMinBy() {
        Optional<String> min = Stream.of("apple", "pie", "banana").collect(Collectors.minBy(String::length));

        Assertions.assertTrue(min.isPresent());
        Assertions.assertEquals("pie", min.get());
    }

    @Test
    public void testMinByOrElseGet() {
        String min = Stream.of("apple", "pie", "banana").collect(Collectors.minByOrElseGet(String::length, () -> "default"));

        Assertions.assertEquals("pie", min);
    }

    @Test
    public void testMinByOrElseThrow() {
        String min = Stream.of("apple", "pie", "banana").collect(Collectors.minByOrElseThrow(String::length));

        Assertions.assertEquals("pie", min);
    }

    @Test
    public void testMinByOrElseThrowWithSupplier() {
        String min = Stream.of("apple", "pie", "banana").collect(Collectors.minByOrElseThrow(String::length, () -> new IllegalStateException("No min")));

        Assertions.assertEquals("pie", min);
    }

    @Test
    public void testMax() {
        Optional<Integer> max = Stream.of(3, 1, 4, 1, 5).collect(Collectors.max());

        Assertions.assertTrue(max.isPresent());
        Assertions.assertEquals(5, max.get());

        Optional<Integer> empty = Stream.<Integer> empty().collect(Collectors.max());
        Assertions.assertFalse(empty.isPresent());
    }

    @Test
    public void testMaxWithComparator() {
        Optional<String> max = Stream.of("apple", "pie", "banana").collect(Collectors.max(Comparator.comparing(String::length)));

        Assertions.assertTrue(max.isPresent());
        Assertions.assertEquals("banana", max.get());
    }

    @Test
    public void testMaxOrElse() {
        Integer max = Stream.of(3, 1, 4).collect(Collectors.maxOrElse(0));

        Assertions.assertEquals(4, max);

        Integer defaultMax = Stream.<Integer> empty().collect(Collectors.maxOrElse(0));
        Assertions.assertEquals(0, defaultMax);
    }

    @Test
    public void testMaxOrElseWithComparator() {
        String max = Stream.of("apple", "pie", "banana").collect(Collectors.maxOrElse(Comparator.comparing(String::length), "default"));

        Assertions.assertEquals("banana", max);
    }

    @Test
    public void testMaxOrElseGet() {
        Integer max = Stream.of(3, 1, 4).collect(Collectors.maxOrElseGet(() -> 0));

        Assertions.assertEquals(4, max);

        Integer defaultMax = Stream.<Integer> empty().collect(Collectors.maxOrElseGet(() -> 0));
        Assertions.assertEquals(0, defaultMax);
    }

    @Test
    public void testMaxOrElseGetWithComparator() {
        String max = Stream.of("apple", "pie", "banana").collect(Collectors.maxOrElseGet(Comparator.comparing(String::length), () -> "default"));

        Assertions.assertEquals("banana", max);
    }

    @Test
    public void testMaxOrElseThrow() {
        Integer max = Stream.of(3, 1, 4).collect(Collectors.maxOrElseThrow());

        Assertions.assertEquals(4, max);

        Assertions.assertThrows(NoSuchElementException.class, () -> Stream.<Integer> empty().collect(Collectors.maxOrElseThrow()));
    }

    @Test
    public void testMaxOrElseThrowWithComparator() {
        String max = Stream.of("apple", "pie", "banana").collect(Collectors.maxOrElseThrow(Comparator.comparing(String::length)));

        Assertions.assertEquals("banana", max);
    }

    @Test
    public void testMaxOrElseThrowWithComparatorAndSupplier() {
        String max = Stream.of("apple", "pie", "banana")
                .collect(Collectors.maxOrElseThrow(Comparator.comparing(String::length), () -> new IllegalStateException("No max")));

        Assertions.assertEquals("banana", max);

        Assertions.assertThrows(IllegalStateException.class, () -> Stream.<String> empty()
                .collect(Collectors.maxOrElseThrow(Comparator.comparing(String::length), () -> new IllegalStateException("No max"))));
    }

    @Test
    public void testMaxBy() {
        Optional<String> max = Stream.of("apple", "pie", "banana").collect(Collectors.maxBy(String::length));

        Assertions.assertTrue(max.isPresent());
        Assertions.assertEquals("banana", max.get());
    }

    @Test
    public void testMaxByOrElseGet() {
        String max = Stream.of("apple", "pie", "banana").collect(Collectors.maxByOrElseGet(String::length, () -> "default"));

        Assertions.assertEquals("banana", max);
    }

    @Test
    public void testMaxByOrElseThrow() {
        String max = Stream.of("apple", "pie", "banana").collect(Collectors.maxByOrElseThrow(String::length));

        Assertions.assertEquals("banana", max);
    }

    @Test
    public void testMaxByOrElseThrowWithSupplier() {
        String max = Stream.of("apple", "pie", "banana").collect(Collectors.maxByOrElseThrow(String::length, () -> new IllegalStateException("No max")));

        Assertions.assertEquals("banana", max);
    }

    @Test
    public void testMinAll() {
        List<Integer> mins = Stream.of(1, 3, 1, 5, 1).collect(Collectors.minAll());

        Assertions.assertEquals(Arrays.asList(1, 1, 1), mins);
    }

    @Test
    public void testMinAllWithComparator() {
        List<String> mins = Stream.of("a", "bb", "ccc", "d", "e").collect(Collectors.minAll(Comparator.comparing(String::length)));

        Assertions.assertEquals(Arrays.asList("a", "d", "e"), mins);
    }

    @Test
    public void testMinAllWithComparatorAndLimit() {
        List<String> mins = Stream.of("a", "bb", "ccc", "d", "e", "f").collect(Collectors.minAll(Comparator.comparing(String::length), 2));

        Assertions.assertEquals(Arrays.asList("a", "d"), mins);
    }

    @Test
    public void testMinAllWithDownstream() {
        String joined = Stream.of(1, 3, 1, 5, 1).collect(Collectors.minAll(Collectors.mapping(Object::toString, Collectors.joining(","))));

        Assertions.assertEquals("1,1,1", joined);
    }

    @Test
    public void testMinAllWithComparatorAndDownstream() {
        Integer count = Stream.of("a", "bb", "ccc", "d", "e").collect(Collectors.minAll(Comparator.comparing(String::length), Collectors.countingToInt()));

        Assertions.assertEquals(3, count); // "a", "d", "e"
    }

    @Test
    public void testMinAlll() {
        Optional<Pair<Integer, Long>> result = Stream.of(1, 3, 1, 5, 1).collect(Collectors.minAlll(Collectors.counting()));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get().left());
        Assertions.assertEquals(3L, result.get().right());
    }

    @Test
    public void testMinAlllWithComparator() {
        Optional<Pair<String, Integer>> result = Stream.of("a", "bb", "ccc", "d", "e")
                .collect(Collectors.minAlll(Comparator.comparing(String::length), Collectors.countingToInt()));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("a", result.get().left());
        Assertions.assertEquals(3, result.get().right());
    }

    @Test
    public void testMinAlllWithComparatorAndFinisher() {
        String result = Stream.of("a", "bb", "ccc", "d", "e")
                .collect(Collectors.minAlll(Comparator.comparing(String::length), Collectors.counting(),
                        opt -> opt.map(p -> p.left() + ":" + p.right()).orElse("none")));

        Assertions.assertEquals("a:3", result);
    }

    @Test
    public void testMaxAll() {
        List<Integer> maxs = Stream.of(5, 3, 5, 1, 5).collect(Collectors.maxAll());

        Assertions.assertEquals(Arrays.asList(5, 5, 5), maxs);
    }

    @Test
    public void testMaxAllWithComparator() {
        List<String> maxs = Stream.of("a", "bb", "ccc", "dd", "eee").collect(Collectors.maxAll(Comparator.comparing(String::length)));

        Assertions.assertEquals(Arrays.asList("ccc", "eee"), maxs);
    }

    @Test
    public void testMaxAllWithComparatorAndLimit() {
        List<String> maxs = Stream.of("a", "bb", "ccc", "dd", "eee", "fff").collect(Collectors.maxAll(Comparator.comparing(String::length), 2));

        Assertions.assertEquals(Arrays.asList("ccc", "eee"), maxs);
    }

    @Test
    public void testMaxAllWithDownstream() {
        String joined = Stream.of(5, 3, 5, 1, 5).collect(Collectors.maxAll(Collectors.mapping(Object::toString, Collectors.joining(","))));

        Assertions.assertEquals("5,5,5", joined);
    }

    @Test
    public void testMaxAllWithComparatorAndDownstream() {
        Integer count = Stream.of("a", "bb", "ccc", "dd", "eee").collect(Collectors.maxAll(Comparator.comparing(String::length), Collectors.countingToInt()));

        Assertions.assertEquals(2, count); // "ccc", "eee"
    }

    @Test
    public void testMaxAlll() {
        Optional<Pair<Integer, Long>> result = Stream.of(5, 3, 5, 1, 5).collect(Collectors.maxAlll(Collectors.counting()));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(5, result.get().left());
        Assertions.assertEquals(3L, result.get().right());
    }

    @Test
    public void testMaxAlllWithComparator() {
        Optional<Pair<String, Integer>> result = Stream.of("a", "bb", "ccc", "dd", "eee")
                .collect(Collectors.maxAlll(Comparator.comparing(String::length), Collectors.countingToInt()));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("ccc", result.get().left());
        Assertions.assertEquals(2, result.get().right());
    }

    @Test
    public void testMaxAlllWithComparatorAndFinisher() {
        String result = Stream.of("a", "bb", "ccc", "dd", "eee")
                .collect(Collectors.maxAlll(Comparator.comparing(String::length), Collectors.counting(),
                        opt -> opt.map(p -> p.left() + ":" + p.right()).orElse("none")));

        Assertions.assertEquals("ccc:2", result);
    }

    @Test
    public void testMinMax() {
        Optional<Pair<Integer, Integer>> result = Stream.of(3, 1, 4, 1, 5).collect(Collectors.minMax());

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get().left());
        Assertions.assertEquals(5, result.get().right());

        Optional<Pair<Integer, Integer>> empty = Stream.<Integer> empty().collect(Collectors.minMax());
        Assertions.assertFalse(empty.isPresent());
    }

    @Test
    public void testMinMaxWithComparator() {
        Optional<Pair<String, String>> result = Stream.of("apple", "pie", "banana").collect(Collectors.minMax(Comparator.comparing(String::length)));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("pie", result.get().left());
        Assertions.assertEquals("banana", result.get().right());
    }

    @Test
    public void testMinMaxWithComparatorAndFinisher() {
        Optional<Integer> result = Stream.of(3, 1, 4, 1, 5).collect(Collectors.minMax(Integer::compare, (min, max) -> max - min));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.get()); // 5 - 1
    }

    @Test
    public void testMinMaxBy() {
        Optional<Pair<String, String>> result = Stream.of("apple", "pie", "banana").collect(Collectors.minMaxBy(String::length));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("pie", result.get().left());
        Assertions.assertEquals("banana", result.get().right());
    }

    @Test
    public void testMinMaxByWithFinisher() {
        Optional<Integer> result = Stream.of("apple", "pie", "banana").collect(Collectors.minMaxBy(String::length, (min, max) -> max.length() - min.length()));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get()); // 6 - 3
    }

    @Test
    public void testMinMaxOrElseGet() {
        Pair<Integer, Integer> result = Stream.of(3, 1, 4, 1, 5).collect(Collectors.minMaxOrElseGet(() -> Pair.of(0, 0)));

        Assertions.assertEquals(1, result.left());
        Assertions.assertEquals(5, result.right());

        Pair<Integer, Integer> empty = Stream.<Integer> empty().collect(Collectors.minMaxOrElseGet(() -> Pair.of(0, 0)));

        Assertions.assertEquals(0, empty.left());
        Assertions.assertEquals(0, empty.right());
    }

    @Test
    public void testMinMaxOrElseGetWithComparator() {
        Pair<String, String> result = Stream.of("apple", "pie", "banana")
                .collect(Collectors.minMaxOrElseGet(Comparator.comparing(String::length), () -> Pair.of("", "")));

        Assertions.assertEquals("pie", result.left());
        Assertions.assertEquals("banana", result.right());
    }

    @Test
    public void testMinMaxOrElseThrow() {
        Pair<Integer, Integer> result = Stream.of(3, 1, 4, 1, 5).collect(Collectors.minMaxOrElseThrow());

        Assertions.assertEquals(1, result.left());
        Assertions.assertEquals(5, result.right());

        Assertions.assertThrows(NoSuchElementException.class, () -> Stream.<Integer> empty().collect(Collectors.minMaxOrElseThrow()));
    }

    @Test
    public void testMinMaxOrElseThrowWithComparator() {
        Pair<String, String> result = Stream.of("apple", "pie", "banana").collect(Collectors.minMaxOrElseThrow(Comparator.comparing(String::length)));

        Assertions.assertEquals("pie", result.left());
        Assertions.assertEquals("banana", result.right());
    }

    @Test
    public void testSummingInt() {
        Integer sum = Stream.of("a", "bb", "ccc").collect(Collectors.summingInt(String::length));

        Assertions.assertEquals(6, sum); // 1 + 2 + 3
    }

    @Test
    public void testSummingIntToLong() {
        Long sum = Stream.of("a", "bb", "ccc").collect(Collectors.summingIntToLong(String::length));

        Assertions.assertEquals(6L, sum); // 1 + 2 + 3
    }

    @Test
    public void testSumming_3() {
        {
            Tuple3<Long, Long, Long> result = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.summingIntToLong(e -> e.length(), e -> e.length() * 2, e -> e.length() * 3));

            Assertions.assertEquals(6L, result._1); // 1 + 2 + 3
            Assertions.assertEquals(12L, result._2);
            Assertions.assertEquals(18L, result._3);
        }
        {
            Tuple3<Long, Long, Long> result = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.summingLong(e -> e.length(), e -> e.length() * 2, e -> e.length() * 3));

            Assertions.assertEquals(6L, result._1); // 1 + 2 + 3
            Assertions.assertEquals(12L, result._2);
            Assertions.assertEquals(18L, result._3);
        }
        {
            Tuple3<Double, Double, Double> result = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.summingDouble(e -> e.length(), e -> e.length() * 2, e -> e.length() * 3));

            Assertions.assertEquals(6L, result._1); // 1 + 2 + 3
            Assertions.assertEquals(12L, result._2);
            Assertions.assertEquals(18L, result._3);
        }
        {
            Tuple3<BigInteger, BigInteger, BigInteger> result = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.summingBigInteger(e -> BigInteger.valueOf(e.length()), e -> BigInteger.valueOf(e.length() * 2),
                            e -> BigInteger.valueOf(e.length() * 3)));

            Assertions.assertEquals(6L, result._1.longValue()); // 1 + 2 + 3
            Assertions.assertEquals(12L, result._2.longValue());
            Assertions.assertEquals(18L, result._3.longValue());
        }
        {
            Tuple3<BigDecimal, BigDecimal, BigDecimal> result = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.summingBigDecimal(e -> BigDecimal.valueOf(e.length()), e -> BigDecimal.valueOf(e.length() * 2),
                            e -> BigDecimal.valueOf(e.length() * 3)));

            Assertions.assertEquals(6L, result._1.longValue()); // 1 + 2 + 3
            Assertions.assertEquals(12L, result._2.longValue());
            Assertions.assertEquals(18L, result._3.longValue());
        }
    }

    @Test
    public void testAverage_3() {
        {
            Tuple3<Double, Double, Double> result = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.averagingInt(e -> e.length(), e -> e.length() * 2, e -> e.length() * 3));

            Assertions.assertEquals(2L, result._1); // (1 + 2 + 3) / 3
            Assertions.assertEquals(4L, result._2);
            Assertions.assertEquals(6L, result._3);
        }
        {
            Tuple3<Double, Double, Double> result = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.averagingLong(e -> e.length(), e -> e.length() * 2, e -> e.length() * 3));

            Assertions.assertEquals(2L, result._1); // (1 + 2 + 3) / 3
            Assertions.assertEquals(4L, result._2);
            Assertions.assertEquals(6L, result._3);
        }
        {
            Tuple3<Double, Double, Double> result = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.averagingDouble(e -> e.length(), e -> e.length() * 2, e -> e.length() * 3));

            Assertions.assertEquals(2L, result._1); // (1 + 2 + 3) / 3
            Assertions.assertEquals(4L, result._2);
            Assertions.assertEquals(6L, result._3);
        }
        {
            Tuple3<BigDecimal, BigDecimal, BigDecimal> result = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.averagingBigInteger(e -> BigInteger.valueOf(e.length()), e -> BigInteger.valueOf(e.length() * 2),
                            e -> BigInteger.valueOf(e.length() * 3)));

            Assertions.assertEquals(2L, result._1.longValue()); // (1 + 2 + 3) / 3
            Assertions.assertEquals(4L, result._2.longValue());
            Assertions.assertEquals(6L, result._3.longValue());
        }
        {
            Tuple3<BigDecimal, BigDecimal, BigDecimal> result = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.averagingBigDecimal(e -> BigDecimal.valueOf(e.length()), e -> BigDecimal.valueOf(e.length() * 2),
                            e -> BigDecimal.valueOf(e.length() * 3)));

            Assertions.assertEquals(2L, result._1.longValue()); // (1 + 2 + 3) / 3
            Assertions.assertEquals(4L, result._2.longValue());
            Assertions.assertEquals(6L, result._3.longValue());
        }
    }

    @Test
    public void testSummingLong() {
        Long sum = Stream.of(1L, 2L, 3L).collect(Collectors.summingLong(e -> e));

        Assertions.assertEquals(6L, sum);
    }

    @Test
    public void testSummingDouble() {
        Double sum = Stream.of(1.0, 2.0, 3.0).collect(Collectors.summingDouble(e -> e));

        Assertions.assertEquals(6.0, sum, 0.001);
    }

    @Test
    public void testSummingBigInteger() {
        BigInteger sum = Stream.of("1", "2", "3").collect(Collectors.summingBigInteger(BigInteger::new));

        Assertions.assertEquals(new BigInteger("6"), sum);
    }

    @Test
    public void testSummingBigDecimal() {
        BigDecimal sum = Stream.of("1.1", "2.2", "3.3").collect(Collectors.summingBigDecimal(BigDecimal::new));

        Assertions.assertEquals(new BigDecimal("6.6"), sum);
    }

    @Test
    public void testAveragingInt() {
        OptionalDouble avg = Stream.of("a", "bb", "ccc").collect(Collectors.averagingInt(String::length));

        Assertions.assertTrue(avg.isPresent());
        Assertions.assertEquals(2.0, avg.getAsDouble(), 0.001); // (1 + 2 + 3) / 3
    }

    @Test
    public void testAveragingIntOrElseThrow() {
        Double avg = Stream.of("a", "bb", "ccc").collect(Collectors.averagingIntOrElseThrow(String::length));

        Assertions.assertEquals(2.0, avg, 0.001);

        Assertions.assertThrows(NoSuchElementException.class, () -> Stream.<String> empty().collect(Collectors.averagingIntOrElseThrow(String::length)));
    }

    @Test
    public void testAveragingLong() {
        OptionalDouble avg = Stream.of(1L, 2L, 3L).collect(Collectors.averagingLong(e -> e));

        Assertions.assertTrue(avg.isPresent());
        Assertions.assertEquals(2.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void testAveragingLongOrElseThrow() {
        Double avg = Stream.of(1L, 2L, 3L).collect(Collectors.averagingLongOrElseThrow(e -> e));

        Assertions.assertEquals(2.0, avg, 0.001);

        Assertions.assertThrows(NoSuchElementException.class, () -> Stream.<Long> empty().collect(Collectors.averagingLongOrElseThrow(e -> e)));
    }

    @Test
    public void testAveragingDouble() {
        OptionalDouble avg = Stream.of(1.0, 2.0, 3.0).collect(Collectors.averagingDouble(e -> e));

        Assertions.assertTrue(avg.isPresent());
        Assertions.assertEquals(2.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void testAveragingDoubleOrElseThrow() {
        Double avg = Stream.of(1.0, 2.0, 3.0).collect(Collectors.averagingDoubleOrElseThrow(e -> e));

        Assertions.assertEquals(2.0, avg, 0.001);

        Assertions.assertThrows(NoSuchElementException.class, () -> Stream.<Double> empty().collect(Collectors.averagingDoubleOrElseThrow(e -> e)));
    }

    @Test
    public void testAveragingBigInteger() {
        Optional<BigDecimal> avg = Stream.of("1", "2", "3").collect(Collectors.averagingBigInteger(BigInteger::new));

        Assertions.assertTrue(avg.isPresent());
        Assertions.assertEquals(new BigDecimal("2"), avg.get());
    }

    @Test
    public void testAveragingBigIntegerOrElseThrow() {
        BigDecimal avg = Stream.of("1", "2", "3").collect(Collectors.averagingBigIntegerOrElseThrow(BigInteger::new));

        Assertions.assertEquals(new BigDecimal("2"), avg);

        Assertions.assertThrows(NoSuchElementException.class,
                () -> Stream.<String> empty().collect(Collectors.averagingBigIntegerOrElseThrow(BigInteger::new)));
    }

    @Test
    public void testAveragingBigDecimal() {
        Optional<BigDecimal> avg = Stream.of("1", "2", "3").collect(Collectors.averagingBigDecimal(BigDecimal::new));

        Assertions.assertTrue(avg.isPresent());
        Assertions.assertEquals(new BigDecimal("2"), avg.get());
    }

    @Test
    public void testAveragingBigDecimalOrElseThrow() {
        BigDecimal avg = Stream.of("1", "2", "3").collect(Collectors.averagingBigDecimalOrElseThrow(BigDecimal::new));

        Assertions.assertEquals(new BigDecimal("2"), avg);

        Assertions.assertThrows(NoSuchElementException.class,
                () -> Stream.<String> empty().collect(Collectors.averagingBigDecimalOrElseThrow(BigDecimal::new)));
    }

    @Test
    public void testSummarizingChar() {
        CharSummaryStatistics stats = Stream.of('a', 'b', 'c').collect(Collectors.summarizingChar(e -> e));

        Assertions.assertEquals(3, stats.getCount());
        Assertions.assertEquals('a', stats.getMin());
        Assertions.assertEquals('c', stats.getMax());
    }

    @Test
    public void testSummarizingByte() {
        ByteSummaryStatistics stats = Stream.of((byte) 1, (byte) 2, (byte) 3).collect(Collectors.summarizingByte(e -> e));

        Assertions.assertEquals(3, stats.getCount());
        Assertions.assertEquals((byte) 1, stats.getMin());
        Assertions.assertEquals((byte) 3, stats.getMax());
        Assertions.assertEquals(6, stats.getSum());
    }

    @Test
    public void testSummarizingShort() {
        ShortSummaryStatistics stats = Stream.of((short) 1, (short) 2, (short) 3).collect(Collectors.summarizingShort(e -> e));

        Assertions.assertEquals(3, stats.getCount());
        Assertions.assertEquals((short) 1, stats.getMin());
        Assertions.assertEquals((short) 3, stats.getMax());
        Assertions.assertEquals(6, stats.getSum());
    }

    @Test
    public void testSummarizingInt() {
        IntSummaryStatistics stats = Stream.of("a", "bb", "ccc").collect(Collectors.summarizingInt(String::length));

        Assertions.assertEquals(3, stats.getCount());
        Assertions.assertEquals(1, stats.getMin());
        Assertions.assertEquals(3, stats.getMax());
        Assertions.assertEquals(6, stats.getSum());
        Assertions.assertEquals(2.0, stats.getAverage(), 0.001);
    }

    @Test
    public void testSummarizingLong() {
        LongSummaryStatistics stats = Stream.of(1L, 2L, 3L).collect(Collectors.summarizingLong(e -> e));

        Assertions.assertEquals(3, stats.getCount());
        Assertions.assertEquals(1L, stats.getMin());
        Assertions.assertEquals(3L, stats.getMax());
        Assertions.assertEquals(6L, stats.getSum());
        Assertions.assertEquals(2.0, stats.getAverage(), 0.001);
    }

    @Test
    public void testSummarizingFloat() {
        FloatSummaryStatistics stats = Stream.of(1.0f, 2.0f, 3.0f).collect(Collectors.summarizingFloat(e -> e));

        Assertions.assertEquals(3, stats.getCount());
        Assertions.assertEquals(1.0f, stats.getMin());
        Assertions.assertEquals(3.0f, stats.getMax());
        Assertions.assertEquals(6.0f, stats.getSum(), 0.001f);
    }

    @Test
    public void testSummarizingDouble() {
        DoubleSummaryStatistics stats = Stream.of(1.0, 2.0, 3.0).collect(Collectors.summarizingDouble(e -> e));

        Assertions.assertEquals(3, stats.getCount());
        Assertions.assertEquals(1.0, stats.getMin());
        Assertions.assertEquals(3.0, stats.getMax());
        Assertions.assertEquals(6.0, stats.getSum(), 0.001);
        Assertions.assertEquals(2.0, stats.getAverage(), 0.001);
    }

    @Test
    public void testSummarizingBigInteger() {
        BigIntegerSummaryStatistics stats = Stream.of("1", "2", "3").collect(Collectors.summarizingBigInteger(BigInteger::new));

        Assertions.assertEquals(3, stats.getCount());
        Assertions.assertEquals(new BigInteger("1"), stats.getMin());
        Assertions.assertEquals(new BigInteger("3"), stats.getMax());
        Assertions.assertEquals(new BigInteger("6"), stats.getSum());
    }

    @Test
    public void testSummarizingBigDecimal() {
        BigDecimalSummaryStatistics stats = Stream.of("1.0", "2.0", "3.0").collect(Collectors.summarizingBigDecimal(BigDecimal::new));

        Assertions.assertEquals(3, stats.getCount());
        Assertions.assertEquals(new BigDecimal("1.0"), stats.getMin());
        Assertions.assertEquals(new BigDecimal("3.0"), stats.getMax());
        Assertions.assertEquals(new BigDecimal("6.0"), stats.getSum());
    }

    @Test
    public void testReducingWithIdentity() {
        Integer result = Stream.of(1, 2, 3, 4).collect(Collectors.reducing(0, Integer::sum));

        Assertions.assertEquals(10, result);
    }

    @Test
    public void testReducingWithoutIdentity() {
        Optional<Integer> result = Stream.of(1, 2, 3, 4).collect(Collectors.reducing(Integer::sum));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(10, result.get());

        Optional<Integer> empty = Stream.<Integer> empty().collect(Collectors.reducing(Integer::sum));
        Assertions.assertFalse(empty.isPresent());
    }

    @Test
    public void testReducingOrElseGet() {
        Integer result = Stream.of(1, 2, 3, 4).collect(Collectors.reducingOrElseGet(Integer::sum, () -> 0));

        Assertions.assertEquals(10, result);

        Integer defaultResult = Stream.<Integer> empty().collect(Collectors.reducingOrElseGet(Integer::sum, () -> 0));

        Assertions.assertEquals(0, defaultResult);
    }

    @Test
    public void testReducingOrElseThrow() {
        Integer result = Stream.of(1, 2, 3, 4).collect(Collectors.reducingOrElseThrow(Integer::sum));

        Assertions.assertEquals(10, result);

        Assertions.assertThrows(NoSuchElementException.class, () -> Stream.<Integer> empty().collect(Collectors.reducingOrElseThrow(Integer::sum)));
    }

    @Test
    public void testReducingOrElseThrowWithSupplier() {
        Integer result = Stream.of(1, 2, 3, 4).collect(Collectors.reducingOrElseThrow(Integer::sum, () -> new IllegalStateException("Empty")));

        Assertions.assertEquals(10, result);

        Assertions.assertThrows(IllegalStateException.class,
                () -> Stream.<Integer> empty().collect(Collectors.reducingOrElseThrow(Integer::sum, () -> new IllegalStateException("Empty"))));
    }

    @Test
    public void testReducingWithMapperAndIdentity() {
        Integer sumOfLengths = Stream.of("a", "bb", "ccc").collect(Collectors.reducing(0, String::length, Integer::sum));

        Assertions.assertEquals(6, sumOfLengths);
    }

    @Test
    public void testReducingWithMapper() {
        Optional<Integer> result = Stream.of("a", "bb", "ccc").collect(Collectors.<String, Integer> reducing(e -> e.length(), Integer::sum));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(6, result.get());
    }

    @Test
    public void testReducingOrElseGetWithMapper() {
        Integer result = Stream.of("a", "bb", "ccc").collect(Collectors.reducingOrElseGet(String::length, Integer::sum, () -> 0));

        Assertions.assertEquals(6, result);
    }

    @Test
    public void testReducingOrElseThrowWithMapper() {
        Integer result = Stream.of("a", "bb", "ccc").collect(Collectors.reducingOrElseThrow(String::length, Integer::sum));

        Assertions.assertEquals(6, result);
    }

    @Test
    public void testReducingOrElseThrowWithMapperAndSupplier() {
        Integer result = Stream.of("a", "bb", "ccc")
                .collect(Collectors.reducingOrElseThrow(String::length, Integer::sum, () -> new IllegalStateException("Empty")));

        Assertions.assertEquals(6, result);
    }

    @Test
    public void testCommonPrefix() {
        String prefix = Stream.of("prefix_test", "prefix_example", "prefix_demo").collect(Collectors.commonPrefix());

        Assertions.assertEquals("prefix_", prefix);

        String empty = Stream.<String> empty().collect(Collectors.commonPrefix());
        Assertions.assertEquals("", empty);

        String noCommon = Stream.of("abc", "def", "ghi").collect(Collectors.commonPrefix());
        Assertions.assertEquals("", noCommon);
    }

    @Test
    public void testCommonSuffix() {
        String suffix = Stream.of("test_suffix", "example_suffix", "demo_suffix").collect(Collectors.commonSuffix());

        Assertions.assertEquals("_suffix", suffix);

        String empty = Stream.<String> empty().collect(Collectors.commonSuffix());
        Assertions.assertEquals("", empty);

        String noCommon = Stream.of("abc", "def", "ghi").collect(Collectors.commonSuffix());
        Assertions.assertEquals("", noCommon);
    }

    @Test
    public void testGroupingBy() {
        Map<Integer, List<String>> grouped = Stream.of("a", "bb", "ccc", "dd", "e").collect(Collectors.groupingBy(String::length));

        Assertions.assertEquals(3, grouped.size());
        Assertions.assertEquals(Arrays.asList("a", "e"), grouped.get(1));
        Assertions.assertEquals(Arrays.asList("bb", "dd"), grouped.get(2));
        Assertions.assertEquals(Arrays.asList("ccc"), grouped.get(3));
    }

    @Test
    public void testGroupingByWithMapFactory() {
        TreeMap<Integer, List<String>> grouped = Stream.of("a", "bb", "ccc", "dd", "e").collect(Collectors.groupingBy(String::length, TreeMap::new));

        Assertions.assertTrue(grouped instanceof TreeMap);
        Assertions.assertEquals(3, grouped.size());
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
    public void testGroupingByConcurrent() {
        ConcurrentMap<Integer, List<String>> grouped = Stream.of("a", "bb", "ccc", "dd", "e")
                .parallel()
                .collect(Collectors.groupingByConcurrent(String::length));

        Assertions.assertEquals(3, grouped.size());
        Assertions.assertTrue(grouped.get(1).contains("a"));
        Assertions.assertTrue(grouped.get(1).contains("e"));
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
    public void testPartitioningBy() {
        Map<Boolean, List<Integer>> partitioned = Stream.of(1, 2, 3, 4, 5).collect(Collectors.partitioningBy(n -> n % 2 == 0));

        Assertions.assertEquals(2, partitioned.size());
        Assertions.assertEquals(Arrays.asList(1, 3, 5), partitioned.get(false));
        Assertions.assertEquals(Arrays.asList(2, 4), partitioned.get(true));
    }

    @Test
    public void testPartitioningByWithDownstream() {
        Map<Boolean, Long> partitioned = Stream.of(1, 2, 3, 4, 5).collect(Collectors.partitioningBy(n -> n % 2 == 0, Collectors.counting()));

        Assertions.assertEquals(3L, partitioned.get(false));
        Assertions.assertEquals(2L, partitioned.get(true));
    }

    @Test
    public void testCountingBy() {
        Map<Integer, Long> counts = Stream.of("a", "bb", "ccc", "dd", "e").collect(Collectors.countingBy(String::length));

        Assertions.assertEquals(2L, counts.get(1));
        Assertions.assertEquals(2L, counts.get(2));
        Assertions.assertEquals(1L, counts.get(3));
    }

    @Test
    public void testCountingByWithMapFactory() {
        TreeMap<Integer, Long> counts = Stream.of("a", "bb", "ccc", "dd", "e").collect(Collectors.countingBy(String::length, TreeMap::new));

        Assertions.assertTrue(counts instanceof TreeMap);
        Assertions.assertEquals(2L, counts.get(1));
    }

    @Test
    public void testCountingToIntBy() {
        Map<Integer, Integer> counts = Stream.of("a", "bb", "ccc", "dd", "e").collect(Collectors.countingToIntBy(String::length));

        Assertions.assertEquals(2, counts.get(1));
        Assertions.assertEquals(2, counts.get(2));
        Assertions.assertEquals(1, counts.get(3));
    }

    @Test
    public void testCountingToIntByWithMapFactory() {
        TreeMap<Integer, Integer> counts = Stream.of("a", "bb", "ccc", "dd", "e").collect(Collectors.countingToIntBy(String::length, TreeMap::new));

        Assertions.assertTrue(counts instanceof TreeMap);
        Assertions.assertEquals(2, counts.get(1));
    }

    @Test
    public void testToMapFromEntries() {
        Map<String, Integer> map = Stream.of(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2)).collect(Collectors.toMap());

        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals(1, map.get("a"));
        Assertions.assertEquals(2, map.get("b"));
    }

    @Test
    public void testToMapFromEntriesWithMergeFunction() {
        Map<String, Integer> map = Stream
                .of(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("a", 2), new AbstractMap.SimpleEntry<>("b", 3))
                .collect(Collectors.toMap(Integer::sum));

        Assertions.assertEquals(3, map.get("a")); // 1 + 2
        Assertions.assertEquals(3, map.get("b"));
    }

    @Test
    public void testToMapFromEntriesWithMapFactory() {
        TreeMap<String, Integer> map = Stream.of(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2))
                .collect(Collectors.toMap(TreeMap::new));

        Assertions.assertTrue(map instanceof TreeMap);
        Assertions.assertEquals(1, map.get("a"));
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
    public void testToMapWithKeyValueMappers() {
        Map<String, Integer> map = Stream.of("a", "bb", "ccc").collect(Collectors.toMap(e -> e, String::length));

        Assertions.assertEquals(1, map.get("a"));
        Assertions.assertEquals(2, map.get("bb"));
        Assertions.assertEquals(3, map.get("ccc"));
    }

    @Test
    public void testToMapWithKeyValueMappersAndMergeFunction() {
        Map<Integer, String> map = Stream.of("a", "bb", "ccc", "dd").collect(Collectors.toMap(String::length, e -> e, (s1, s2) -> s1 + "," + s2));

        Assertions.assertEquals("bb,dd", map.get(2));
    }

    @Test
    public void testToMapWithKeyValueMappersAndMapFactory() {
        Map<String, Integer> map = Stream.of("a", "bb", "ccc").collect(Collectors.toMap(e -> e, String::length, Suppliers.ofLinkedHashMap()));

        Assertions.assertTrue(map instanceof LinkedHashMap);
        Assertions.assertEquals(1, map.get("a"));
    }

    @Test
    public void testToMapWithAllParameters() {
        LinkedHashMap<Integer, String> map = Stream.of("a", "bb", "ccc", "dd")
                .collect(Collectors.toMap(String::length, e -> e, (s1, s2) -> s1 + "," + s2, LinkedHashMap::new));

        Assertions.assertTrue(map instanceof LinkedHashMap);
        Assertions.assertEquals("bb,dd", map.get(2));
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
    public void testToUnmodifiableMap() {
        Map<String, Integer> map = Stream.of("a", "bb", "ccc").collect(Collectors.toUnmodifiableMap(e -> e, String::length));

        Assertions.assertEquals(1, map.get("a"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.put("d", 4));
    }

    @Test
    public void testToUnmodifiableMapWithMergeFunction() {
        Map<Integer, String> map = Stream.of("a", "bb", "ccc", "dd").collect(Collectors.toUnmodifiableMap(String::length, e -> e, (s1, s2) -> s1 + "," + s2));

        Assertions.assertEquals("bb,dd", map.get(2));
    }

    @Test
    public void testToLinkedHashMap() {
        Map<String, Integer> map = Stream.of("a", "bb", "ccc").collect(Collectors.toLinkedHashMap(e -> e, String::length));

        Assertions.assertTrue(map instanceof LinkedHashMap);
        Assertions.assertEquals(1, map.get("a"));

        // Test order preservation
        Iterator<String> keys = map.keySet().iterator();
        Assertions.assertEquals("a", keys.next());
        Assertions.assertEquals("bb", keys.next());
        Assertions.assertEquals("ccc", keys.next());
    }

    @Test
    public void testToLinkedHashMapWithMergeFunction() {
        Map<Integer, String> map = Stream.of("a", "bb", "ccc", "dd").collect(Collectors.toLinkedHashMap(String::length, e -> e, (s1, s2) -> s1 + "," + s2));

        Assertions.assertTrue(map instanceof LinkedHashMap);
        Assertions.assertEquals("bb,dd", map.get(2));
    }

    @Test
    public void testToConcurrentMap() {
        ConcurrentMap<String, Integer> map = Stream.of("a", "bb", "ccc").parallel().collect(Collectors.toConcurrentMap(e -> e, String::length));

        Assertions.assertEquals(1, map.get("a"));
        Assertions.assertEquals(2, map.get("bb"));
        Assertions.assertEquals(3, map.get("ccc"));
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
    public void testToBiMap() {
        BiMap<Integer, String> biMap = Stream.of("a", "bb", "ccc").collect(Collectors.toBiMap(String::length, e -> e));

        Assertions.assertEquals("a", biMap.get(1));
        Assertions.assertEquals(1, biMap.inversed().get("a"));
    }

    @Test
    public void testToBiMapWithMapFactory() {
        BiMap<Integer, String> biMap = Stream.of("a", "bb", "ccc").collect(Collectors.toBiMap(String::length, e -> e, Suppliers.ofBiMap()));

        Assertions.assertEquals("a", biMap.get(1));
    }

    @Test
    public void testToBiMapWithMergeFunction() {
        BiMap<Integer, String> biMap = Stream.of("a", "bb", "ccc", "dd").collect(Collectors.toBiMap(String::length, e -> e, (s1, s2) -> s1 // Keep first
        ));

        Assertions.assertEquals("bb", biMap.get(2));
    }

    @Test
    public void testToBiMapWithAllParameters() {
        BiMap<Integer, String> biMap = Stream.of("a", "bb", "ccc", "dd")
                .collect(Collectors.toBiMap(String::length, e -> e, (s1, s2) -> s1, // Keep first
                        BiMap::new));

        Assertions.assertEquals("bb", biMap.get(2));
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
    public void testFlatMappingValueToMultimap() {
        ListMultimap<String, Integer> multimap = Stream.of("a", "bb")
                .collect(Collectors.flatMappingValueToMultimap(e -> e, s -> Stream.of(1, 2, 3).limit(s.length())));

        Assertions.assertEquals(Arrays.asList(1), multimap.get("a"));
        Assertions.assertEquals(Arrays.asList(1, 2), multimap.get("bb"));
    }

    @Test
    public void testFlatMappingValueToMultimapWithMapFactory() {
        ListMultimap<String, Integer> multimap = Stream.of("a", "bb")
                .collect(Collectors.flatMappingValueToMultimap(e -> e, s -> Stream.of(1, 2, 3).limit(s.length()), Suppliers.ofListMultimap()));

        Assertions.assertEquals(Arrays.asList(1), multimap.get("a"));
        Assertions.assertEquals(Arrays.asList(1, 2), multimap.get("bb"));
    }

    @Test
    public void testFlatmappingValueToMultimap() {
        ListMultimap<String, Integer> multimap = Stream.of("a", "bb")
                .collect(Collectors.flatmappingValueToMultimap(e -> e, s -> Arrays.asList(1, 2, 3).subList(0, s.length())));

        Assertions.assertEquals(Arrays.asList(1), multimap.get("a"));
        Assertions.assertEquals(Arrays.asList(1, 2), multimap.get("bb"));
    }

    @Test
    public void testFlatmappingValueToMultimapWithMapFactory() {
        ListMultimap<String, Integer> multimap = Stream.of("a", "bb")
                .collect(Collectors.flatmappingValueToMultimap(e -> e, s -> Arrays.asList(1, 2, 3).subList(0, s.length()), Suppliers.ofListMultimap()));

        Assertions.assertEquals(Arrays.asList(1), multimap.get("a"));
        Assertions.assertEquals(Arrays.asList(1, 2), multimap.get("bb"));
    }

    @Test
    public void testFlatMappingKeyToMultimap() {
        ListMultimap<Integer, String> multimap = Stream.of("a", "bb")
                .collect(Collectors.flatMappingKeyToMultimap(s -> Stream.of(1, 2, 3).limit(s.length()), e -> e));

        Assertions.assertEquals(Arrays.asList("a", "bb"), multimap.get(1));
        Assertions.assertEquals(Arrays.asList("bb"), multimap.get(2));
    }

    @Test
    public void testFlatMappingKeyToMultimapWithMapFactory() {
        ListMultimap<Integer, String> multimap = Stream.of("a", "bb")
                .collect(Collectors.flatMappingKeyToMultimap(s -> Stream.of(1, 2, 3).limit(s.length()), e -> e, Suppliers.ofListMultimap()));

        Assertions.assertEquals(Arrays.asList("a", "bb"), multimap.get(1));
        Assertions.assertEquals(Arrays.asList("bb"), multimap.get(2));
    }

    @Test
    public void testFlatmappingKeyToMultimap() {
        ListMultimap<Integer, String> multimap = Stream.of("a", "bb")
                .collect(Collectors.flatmappingKeyToMultimap(s -> Arrays.asList(1, 2, 3).subList(0, s.length()), e -> e));

        Assertions.assertEquals(Arrays.asList("a", "bb"), multimap.get(1));
        Assertions.assertEquals(Arrays.asList("bb"), multimap.get(2));
    }

    @Test
    public void testFlatmappingKeyToMultimapWithMapFactory() {
        ListMultimap<Integer, String> multimap = Stream.of("a", "bb")
                .collect(Collectors.flatmappingKeyToMultimap(s -> Arrays.asList(1, 2, 3).subList(0, s.length()), e -> e, Suppliers.ofListMultimap()));

        Assertions.assertEquals(Arrays.asList("a", "bb"), multimap.get(1));
        Assertions.assertEquals(Arrays.asList("bb"), multimap.get(2));
    }

    @Test
    public void testTeeing() {
        Pair<Long, Integer> result = Stream.of(1, 2, 3, 4, 5)
                .collect(Collectors.teeing(Collectors.counting(), Collectors.summingInt(i -> i), (count, sum) -> Pair.of(count, sum)));

        Assertions.assertEquals(5L, result.left());
        Assertions.assertEquals(15, result.right());
    }
}
