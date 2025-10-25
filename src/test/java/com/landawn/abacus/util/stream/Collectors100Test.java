package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.DoubleSummaryStatistics;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

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
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.Tuple.Tuple6;
import com.landawn.abacus.util.Tuple.Tuple7;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.Collectors.MoreCollectors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Tag("new-test")
public class Collectors100Test extends TestBase {

    private List<String> stringList;
    private List<Integer> integerList;
    private List<Double> doubleList;
    private List<Person> personList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Person {
        private String name;
        private int age;
        private String department;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Employee {
        private String name;
        private List<String> skills;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Department {
        private String name;
        private List<String> skills;
    }

    @BeforeEach
    public void setUp() {
        stringList = Arrays.asList("apple", "banana", "cherry", "date", "elderberry");
        integerList = Arrays.asList(1, 2, 3, 4, 5);
        doubleList = Arrays.asList(1.1, 2.2, 3.3, 4.4, 5.5);
        personList = Arrays.asList(new Person("Alice", 25, "Engineering"), new Person("Bob", 30, "Engineering"), new Person("Charlie", 35, "Sales"),
                new Person("David", 28, "Sales"), new Person("Eve", 32, "HR"));
    }

    @Test
    public void testCreateWithSupplierAccumulatorCombiner() {
        Collector<String, List<String>, List<String>> collector = Collectors.create(ArrayList::new, List::add, (a, b) -> {
            a.addAll(b);
            return a;
        });

        List<String> result = stringList.stream().collect(collector);
        assertEquals(stringList, result);
    }

    @Test
    public void testCreateWithSupplierAccumulatorCombinerCharacteristics() {
        Collector<String, List<String>, List<String>> collector = Collectors.create(ArrayList::new, List::add, (a, b) -> {
            a.addAll(b);
            return a;
        }, Characteristics.IDENTITY_FINISH);

        List<String> result = stringList.stream().collect(collector);
        assertEquals(stringList, result);
        assertTrue(collector.characteristics().contains(Characteristics.IDENTITY_FINISH));
    }

    @Test
    public void testCreateWithSupplierAccumulatorCombinerCharacteristicsCollection() {
        Set<Characteristics> characteristics = EnumSet.of(Characteristics.IDENTITY_FINISH, Characteristics.UNORDERED);
        Collector<String, List<String>, List<String>> collector = Collectors.create(ArrayList::new, List::add, (a, b) -> {
            a.addAll(b);
            return a;
        }, characteristics);

        List<String> result = stringList.stream().collect(collector);
        assertEquals(stringList, result);
        assertTrue(collector.characteristics().contains(Characteristics.IDENTITY_FINISH));
        assertTrue(collector.characteristics().contains(Characteristics.UNORDERED));
    }

    @Test
    public void testCreateWithFinisher() {
        Collector<String, List<String>, Integer> collector = Collectors.create(ArrayList::new, List::add, (a, b) -> {
            a.addAll(b);
            return a;
        }, List::size);

        Integer result = stringList.stream().collect(collector);
        assertEquals(5, result);
    }

    @Test
    public void testCreateWithFinisherAndCharacteristics() {
        Collector<String, List<String>, Integer> collector = Collectors.create(ArrayList::new, List::add, (a, b) -> {
            a.addAll(b);
            return a;
        }, List::size, Characteristics.UNORDERED);

        Integer result = stringList.stream().collect(collector);
        assertEquals(5, result);
        assertTrue(collector.characteristics().contains(Characteristics.UNORDERED));
    }

    @Test
    public void testToCollection() {
        Collector<String, ?, LinkedList<String>> collector = Collectors.toCollection(LinkedList::new);
        LinkedList<String> result = stringList.stream().collect(collector);
        assertEquals(stringList, result);
        assertTrue(result instanceof LinkedList);
    }

    @Test
    public void testToCollectionWithLimit() {
        Collector<String, ?, List<String>> collector = Collectors.toCollection(3, ArrayList::new);
        List<String> result = stringList.stream().collect(collector);
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("apple", "banana", "cherry"), result);
    }

    @Test
    public void testToCollectionWithAccumulator() {
        Collector<String, ?, List<String>> collector = Collectors.toCollection(ArrayList::new, (list, str) -> {
            if (str.length() > 5)
                list.add(str);
        });
        List<String> result = stringList.stream().collect(collector);
        assertEquals(Arrays.asList("banana", "cherry", "elderberry"), result);
    }

    @Test
    public void testToCollectionWithAccumulatorAndCombiner() {
        Collector<String, ?, List<String>> collector = Collectors.toCollection(ArrayList::new, (list, str) -> list.add(str.toUpperCase()), (a, b) -> {
            a.addAll(b);
            return a;
        });
        List<String> result = stringList.stream().collect(collector);
        assertEquals(Arrays.asList("APPLE", "BANANA", "CHERRY", "DATE", "ELDERBERRY"), result);
    }

    @Test
    public void testToList() {
        List<String> result = stringList.stream().collect(Collectors.toList());
        assertEquals(stringList, result);
    }

    @Test
    public void testToLinkedList() {
        LinkedList<String> result = stringList.stream().collect(Collectors.toLinkedList());
        assertEquals(stringList, result);
        assertTrue(result instanceof LinkedList);
    }

    @Test
    public void testToImmutableList() {
        ImmutableList<String> result = stringList.stream().collect(Collectors.toImmutableList());
        assertEquals(stringList, result);
        assertThrows(UnsupportedOperationException.class, () -> result.add("test"));
    }

    @Test
    public void testToUnmodifiableList() {
        List<String> result = stringList.stream().collect(Collectors.toUnmodifiableList());
        assertEquals(stringList, result);
        assertThrows(UnsupportedOperationException.class, () -> result.add("test"));
    }

    @Test
    public void testToListWithLimit() {
        List<String> result = stringList.stream().collect(Collectors.toList(3));
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("apple", "banana", "cherry"), result);
    }

    @Test
    public void testToSet() {
        Set<String> result = stringList.stream().collect(Collectors.toSet());
        assertEquals(new HashSet<>(stringList), result);
    }

    @Test
    public void testToLinkedHashSet() {
        Set<String> result = Arrays.asList("b", "a", "c", "a").stream().collect(Collectors.toLinkedHashSet());
        assertTrue(result instanceof LinkedHashSet);
        assertEquals(Arrays.asList("b", "a", "c"), new ArrayList<>(result));
    }

    @Test
    public void testToImmutableSet() {
        ImmutableSet<String> result = stringList.stream().collect(Collectors.toImmutableSet());
        assertEquals(new HashSet<>(stringList), result);
        assertThrows(UnsupportedOperationException.class, () -> result.add("test"));
    }

    @Test
    public void testToUnmodifiableSet() {
        Set<String> result = stringList.stream().collect(Collectors.toUnmodifiableSet());
        assertEquals(new HashSet<>(stringList), result);
        assertThrows(UnsupportedOperationException.class, () -> result.add("test"));
    }

    @Test
    public void testToSetWithLimit() {
        Set<Integer> result = Arrays.asList(1, 2, 3, 4, 5, 1, 2).stream().collect(Collectors.toSet(3));
        assertTrue(result.size() <= 3);
    }

    @Test
    public void testToQueue() {
        Queue<String> result = stringList.stream().collect(Collectors.toQueue());
        assertEquals(stringList.size(), result.size());
        assertEquals("apple", result.poll());
    }

    @Test
    public void testToDeque() {
        Deque<String> result = stringList.stream().collect(Collectors.toDeque());
        assertEquals(stringList.size(), result.size());
        assertEquals("apple", result.pollFirst());
        assertEquals("elderberry", result.pollLast());
    }

    @Test
    public void testToMultiset() {
        Multiset<String> result = Arrays.asList("a", "b", "a", "c", "b", "a").stream().collect(Collectors.toMultiset());
        assertEquals(3, result.count("a"));
        assertEquals(2, result.count("b"));
        assertEquals(1, result.count("c"));
    }

    @Test
    public void testToMultisetWithSupplier() {
        Multiset<String> result = Arrays.asList("a", "b", "a", "c", "b", "a").stream().collect(Collectors.toMultiset(Suppliers.ofMultiset()));
        assertEquals(3, result.count("a"));
        assertEquals(2, result.count("b"));
        assertEquals(1, result.count("c"));
    }

    @Test
    public void testToArray() {
        Object[] result = stringList.stream().collect(Collectors.toArray());
        assertArrayEquals(stringList.toArray(), result);
    }

    @Test
    public void testToArrayWithSupplier() {
        String[] result = stringList.stream().collect(Collectors.toArray(() -> new String[0]));
        assertArrayEquals(stringList.toArray(new String[0]), result);
    }

    @Test
    public void testToArrayWithIntFunction() {
        String[] result = stringList.stream().collect(Collectors.toArray(String[]::new));
        assertArrayEquals(stringList.toArray(new String[0]), result);
    }

    @Test
    public void testToBooleanList() {
        BooleanList result = Arrays.asList(true, false, true).stream().collect(Collectors.toBooleanList());
        assertEquals(3, result.size());
        assertTrue(result.get(0));
        assertFalse(result.get(1));
    }

    @Test
    public void testToBooleanArray() {
        boolean[] result = Arrays.asList(true, false, true).stream().collect(Collectors.toBooleanArray());
        assertEquals(3, result.length);
        assertTrue(result[0]);
        assertFalse(result[1]);
    }

    @Test
    public void testToCharList() {
        CharList result = Arrays.asList('a', 'b', 'c').stream().collect(Collectors.toCharList());
        assertEquals(3, result.size());
        assertEquals('a', result.get(0));
    }

    @Test
    public void testToCharArray() {
        char[] result = Arrays.asList('a', 'b', 'c').stream().collect(Collectors.toCharArray());
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testToByteList() {
        ByteList result = Arrays.asList((byte) 1, (byte) 2, (byte) 3).stream().collect(Collectors.toByteList());
        assertEquals(3, result.size());
        assertEquals(1, result.get(0));
    }

    @Test
    public void testToByteArray() {
        byte[] result = Arrays.asList((byte) 1, (byte) 2, (byte) 3).stream().collect(Collectors.toByteArray());
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testToShortList() {
        ShortList result = Arrays.asList((short) 1, (short) 2, (short) 3).stream().collect(Collectors.toShortList());
        assertEquals(3, result.size());
        assertEquals(1, result.get(0));
    }

    @Test
    public void testToShortArray() {
        short[] result = Arrays.asList((short) 1, (short) 2, (short) 3).stream().collect(Collectors.toShortArray());
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testToIntList() {
        IntList result = integerList.stream().collect(Collectors.toIntList());
        assertEquals(5, result.size());
        assertEquals(1, result.get(0));
    }

    @Test
    public void testToIntArray() {
        int[] result = integerList.stream().collect(Collectors.toIntArray());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testToLongList() {
        LongList result = Arrays.asList(1L, 2L, 3L).stream().collect(Collectors.toLongList());
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0));
    }

    @Test
    public void testToLongArray() {
        long[] result = Arrays.asList(1L, 2L, 3L).stream().collect(Collectors.toLongArray());
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testToFloatList() {
        FloatList result = Arrays.asList(1.1f, 2.2f, 3.3f).stream().collect(Collectors.toFloatList());
        assertEquals(3, result.size());
        assertEquals(1.1f, result.get(0), 0.01);
    }

    @Test
    public void testToFloatArray() {
        float[] result = Arrays.asList(1.1f, 2.2f, 3.3f).stream().collect(Collectors.toFloatArray());
        assertArrayEquals(new float[] { 1.1f, 2.2f, 3.3f }, result, 0.01f);
    }

    @Test
    public void testToDoubleList() {
        DoubleList result = doubleList.stream().collect(Collectors.toDoubleList());
        assertEquals(5, result.size());
        assertEquals(1.1, result.get(0), 0.01);
    }

    @Test
    public void testToDoubleArray() {
        double[] result = doubleList.stream().collect(Collectors.toDoubleArray());
        assertArrayEquals(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 }, result, 0.01);
    }

    @Test
    public void testOnlyOne() {
        Optional<String> result = Arrays.asList("apple").stream().collect(Collectors.onlyOne());
        assertTrue(result.isPresent());
        assertEquals("apple", result.get());

        Optional<String> empty = Arrays.<String> asList().stream().collect(Collectors.onlyOne());
        assertFalse(empty.isPresent());

        assertThrows(TooManyElementsException.class, () -> stringList.stream().collect(Collectors.onlyOne()));
    }

    @Test
    public void testOnlyOneWithPredicate() {
        Optional<String> result = stringList.stream().collect(Collectors.onlyOne(s -> s.length() == 4));
        assertTrue(result.isPresent());
        assertEquals("date", result.get());

        assertThrows(TooManyElementsException.class, () -> stringList.stream().collect(Collectors.onlyOne(s -> s.length() > 4)));
    }

    @Test
    public void testFirst() {
        Optional<String> result = stringList.stream().collect(Collectors.first());
        assertTrue(result.isPresent());
        assertEquals("apple", result.get());

        Optional<String> empty = Arrays.<String> asList().stream().collect(Collectors.first());
        assertFalse(empty.isPresent());
    }

    @Test
    public void testLast() {
        Optional<String> result = stringList.stream().collect(Collectors.last());
        assertTrue(result.isPresent());
        assertEquals("elderberry", result.get());

        Optional<String> empty = Arrays.<String> asList().stream().collect(Collectors.last());
        assertFalse(empty.isPresent());
    }

    @Test
    public void testFirstN() {
        List<String> result = stringList.stream().collect(Collectors.first(3));
        assertEquals(Arrays.asList("apple", "banana", "cherry"), result);

        List<String> moreThanSize = stringList.stream().collect(Collectors.first(10));
        assertEquals(stringList, moreThanSize);
    }

    @Test
    public void testLastN() {
        List<String> result = stringList.stream().collect(Collectors.last(3));
        assertEquals(Arrays.asList("cherry", "date", "elderberry"), result);

        List<String> moreThanSize = stringList.stream().collect(Collectors.last(10));
        assertEquals(stringList, moreThanSize);
    }

    @Test
    public void testJoining() {
        String result = stringList.stream().collect(Collectors.joining());
        assertEquals("applebananacherrydateelderberry", result);
    }

    @Test
    public void testJoiningWithDelimiter() {
        String result = stringList.stream().collect(Collectors.joining(", "));
        assertEquals("apple, banana, cherry, date, elderberry", result);
    }

    @Test
    public void testJoiningWithDelimiterPrefixSuffix() {
        String result = stringList.stream().collect(Collectors.joining(", ", "[", "]"));
        assertEquals("[apple, banana, cherry, date, elderberry]", result);
    }

    @Test
    public void testFiltering() {
        List<String> result = stringList.stream().collect(Collectors.filtering(s -> s.length() > 5, Collectors.toList()));
        assertEquals(Arrays.asList("banana", "cherry", "elderberry"), result);
    }

    @Test
    public void testFilteringToList() {
        List<String> result = stringList.stream().collect(Collectors.filteringToList(s -> s.length() > 5));
        assertEquals(Arrays.asList("banana", "cherry", "elderberry"), result);
    }

    @Test
    public void testMapping() {
        List<Integer> result = stringList.stream().collect(Collectors.mapping(String::length, Collectors.toList()));
        assertEquals(Arrays.asList(5, 6, 6, 4, 10), result);
    }

    @Test
    public void testMappingToList() {
        List<Integer> result = stringList.stream().collect(Collectors.mappingToList(String::length));
        assertEquals(Arrays.asList(5, 6, 6, 4, 10), result);
    }

    @Test
    public void testFlatMapping() {
        List<String> words = Arrays.asList("hello world", "java streams");
        List<String> result = words.stream().collect(Collectors.flatmapping(s -> Arrays.asList(s.split(" ")), Collectors.toList()));
        assertEquals(Arrays.asList("hello", "world", "java", "streams"), result);
    }

    @Test
    public void testFlatMappingToList() {
        List<String> words = Arrays.asList("hello world", "java streams");
        List<String> result = words.stream().collect(Collectors.flatmappingToList(s -> Arrays.asList(s.split(" "))));
        assertEquals(Arrays.asList("hello", "world", "java", "streams"), result);
    }

    @Test
    public void testFlatMappingCollection() {
        List<List<String>> nested = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"));
        List<String> result = nested.stream().collect(Collectors.flatmapping(Function.identity(), Collectors.toList()));
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    }

    @Test
    public void testFlatMappingToListCollection() {
        List<List<String>> nested = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"));
        List<String> result = nested.stream().collect(Collectors.flatmappingToList(Function.identity()));
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    }

    @Test
    public void testCollectingAndThen() {
        Integer result = stringList.stream().collect(Collectors.collectingAndThen(Collectors.toList(), List::size));
        assertEquals(5, result);
    }

    @Test
    public void testCollectingOrEmpty() {
        Optional<List<String>> result = stringList.stream().filter(s -> s.length() > 5).collect(Collectors.collectingOrEmpty(Collectors.toList()));
        assertTrue(result.isPresent());
        assertEquals(3, result.get().size());

        Optional<List<String>> empty = stringList.stream().filter(s -> s.length() > 20).collect(Collectors.collectingOrEmpty(Collectors.toList()));
        assertFalse(empty.isPresent());
    }

    @Test
    public void testCollectingOrElseIfEmpty() {
        List<String> defaultList = Arrays.asList("default");

        List<String> result = stringList.stream().filter(s -> s.length() > 20).collect(Collectors.collectingOrElseIfEmpty(Collectors.toList(), defaultList));
        assertEquals(defaultList, result);
    }

    @Test
    public void testCollectingOrElseGetIfEmpty() {
        List<String> result = stringList.stream()
                .filter(s -> s.length() > 20)
                .collect(Collectors.collectingOrElseGetIfEmpty(Collectors.toList(), () -> Arrays.asList("default")));
        assertEquals(Arrays.asList("default"), result);
    }

    @Test
    public void testCollectingOrElseThrowIfEmpty() {
        assertThrows(NoSuchElementException.class,
                () -> stringList.stream().filter(s -> s.length() > 20).collect(Collectors.collectingOrElseThrowIfEmpty(Collectors.toList())));
    }

    @Test
    public void testDistinctByToList() {
        List<String> result = stringList.stream().collect(Collectors.distinctByToList(String::length));
        assertEquals(4, result.size());
    }

    @Test
    public void testDistinctByToCollection() {
        Set<String> result = stringList.stream().collect(Collectors.distinctByToCollection(String::length, HashSet::new));
        assertEquals(4, result.size());
    }

    @Test
    public void testDistinctByToCounting() {
        Integer result = stringList.stream().collect(Collectors.distinctByToCounting(String::length));
        assertEquals(4, result);
    }

    @Test
    public void testCounting() {
        Long result = stringList.stream().collect(Collectors.counting());
        assertEquals(5L, result);
    }

    @Test
    public void testCountingToInt() {
        Integer result = stringList.stream().collect(Collectors.countingToInt());
        assertEquals(5, result);
    }

    @Test
    public void testMin() {
        Optional<Integer> result = integerList.stream().collect(Collectors.min());
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testMinWithComparator() {
        Optional<String> result = stringList.stream().collect(Collectors.min(Comparator.comparing(String::length)));
        assertTrue(result.isPresent());
        assertEquals("date", result.get());
    }

    @Test
    public void testMinOrElse() {
        Integer result = Arrays.<Integer> asList().stream().collect(Collectors.minOrElse(0));
        assertEquals(0, result);
    }

    @Test
    public void testMinOrElseGet() {
        Integer result = Arrays.<Integer> asList().stream().collect(Collectors.minOrElseGet(() -> -1));
        assertEquals(-1, result);
    }

    @Test
    public void testMinOrElseThrow() {
        assertThrows(NoSuchElementException.class, () -> Arrays.<Integer> asList().stream().collect(Collectors.minOrElseThrow()));
    }

    @Test
    public void testMinBy() {
        Optional<Person> result = personList.stream().collect(Collectors.minBy(Person::getAge));
        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().name);
    }

    @Test
    public void testMax() {
        Optional<Integer> result = integerList.stream().collect(Collectors.max());
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    public void testMaxWithComparator() {
        Optional<String> result = stringList.stream().collect(Collectors.max(Comparator.comparing(String::length)));
        assertTrue(result.isPresent());
        assertEquals("elderberry", result.get());
    }

    @Test
    public void testMaxBy() {
        Optional<Person> result = personList.stream().collect(Collectors.maxBy(Person::getAge));
        assertTrue(result.isPresent());
        assertEquals("Charlie", result.get().name);
    }

    @Test
    public void testMinAll() {
        List<Integer> result = Arrays.asList(1, 2, 1, 3, 1).stream().collect(Collectors.minAll());
        assertEquals(Arrays.asList(1, 1, 1), result);
    }

    @Test
    public void testMaxAll() {
        List<Integer> result = Arrays.asList(1, 3, 2, 3, 1).stream().collect(Collectors.maxAll());
        assertEquals(Arrays.asList(3, 3), result);
    }

    @Test
    public void testMinAllWithDownstream() {
        Long count = Arrays.asList(1, 2, 1, 3, 1).stream().collect(Collectors.minAll(Collectors.counting()));
        assertEquals(3L, count);
    }

    @Test
    public void testMaxAllWithDownstream() {
        Long count = Arrays.asList(1, 3, 2, 3, 1).stream().collect(Collectors.maxAll(Collectors.counting()));
        assertEquals(2L, count);
    }

    @Test
    public void testMinMax() {
        Optional<Pair<Integer, Integer>> result = integerList.stream().collect(Collectors.minMax());
        assertTrue(result.isPresent());
        assertEquals(1, result.get().left());
        assertEquals(5, result.get().right());
    }

    @Test
    public void testMinMaxWithComparator() {
        Optional<Pair<String, String>> result = stringList.stream().collect(Collectors.minMax(Comparator.comparing(String::length)));
        assertTrue(result.isPresent());
        assertEquals("date", result.get().left());
        assertEquals("elderberry", result.get().right());
    }

    @Test
    public void testMinMaxBy() {
        Optional<Pair<Person, Person>> result = personList.stream().collect(Collectors.minMaxBy(Person::getAge));
        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().left().name);
        assertEquals("Charlie", result.get().right().name);
    }

    @Test
    public void testMinMaxOrElseGet() {
        Pair<Integer, Integer> result = Arrays.<Integer> asList().stream().collect(Collectors.minMaxOrElseGet(() -> Pair.of(0, 100)));
        assertEquals(0, result.left());
        assertEquals(100, result.right());
    }

    @Test
    public void testMinMaxOrElseThrow() {
        assertThrows(NoSuchElementException.class, () -> Arrays.<Integer> asList().stream().collect(Collectors.minMaxOrElseThrow()));
    }

    @Test
    public void testSummingInt() {
        Integer result = integerList.stream().collect(Collectors.summingInt(i -> i));
        assertEquals(15, result);
    }

    @Test
    public void testSummingIntToLong() {
        Long result = integerList.stream().collect(Collectors.summingIntToLong(i -> i));
        assertEquals(15L, result);
    }

    @Test
    public void testSummingLong() {
        Long result = Arrays.asList(1L, 2L, 3L, 4L, 5L).stream().collect(Collectors.summingLong(l -> l));
        assertEquals(15L, result);
    }

    @Test
    public void testSummingDouble() {
        Double result = doubleList.stream().collect(Collectors.summingDouble(d -> d));
        assertEquals(16.5, result, 0.01);
    }

    @Test
    public void testSummingBigInteger() {
        BigInteger result = Arrays.asList(BigInteger.valueOf(1000000000000L), BigInteger.valueOf(2000000000000L), BigInteger.valueOf(3000000000000L))
                .stream()
                .collect(Collectors.summingBigInteger(Function.identity()));
        assertEquals(BigInteger.valueOf(6000000000000L), result);
    }

    @Test
    public void testSummingBigDecimal() {
        BigDecimal result = Arrays.asList(new BigDecimal("1.11111111111"), new BigDecimal("2.22222222222"), new BigDecimal("3.33333333333"))
                .stream()
                .collect(Collectors.summingBigDecimal(Function.identity()));
        assertEquals(new BigDecimal("6.66666666666"), result);
    }

    @Test
    public void testAveragingInt() {
        OptionalDouble result = integerList.stream().collect(Collectors.averagingIntOrEmpty(i -> i));
        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.01);
    }

    @Test
    public void testAveragingIntOrElseThrow() {
        Double result = integerList.stream().collect(Collectors.averagingIntOrElseThrow(i -> i));
        assertEquals(3.0, result, 0.01);

        assertThrows(NoSuchElementException.class, () -> Arrays.<Integer> asList().stream().collect(Collectors.averagingIntOrElseThrow(i -> i)));
    }

    @Test
    public void testAveragingLong() {
        OptionalDouble result = Arrays.asList(1L, 2L, 3L, 4L, 5L).stream().collect(Collectors.averagingLongOrEmpty(l -> l));
        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.01);
    }

    @Test
    public void testAveragingDouble() {
        OptionalDouble result = doubleList.stream().collect(Collectors.averagingDoubleOrEmpty(d -> d));
        assertTrue(result.isPresent());
        assertEquals(3.3, result.getAsDouble(), 0.01);
    }

    @Test
    public void testAveragingBigInteger() {
        Optional<BigDecimal> result = Arrays.asList(BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3))
                .stream()
                .collect(Collectors.averagingBigIntegerOrEmpty(Function.identity()));
        assertTrue(result.isPresent());
        assertEquals(0, BigDecimal.valueOf(2).compareTo(result.get()));
    }

    @Test
    public void testAveragingBigDecimal() {
        Optional<BigDecimal> result = Arrays.asList(new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0"))
                .stream()
                .collect(Collectors.averagingBigDecimalOrEmpty(Function.identity()));
        assertTrue(result.isPresent());
        assertEquals(0, new BigDecimal("2").compareTo(result.get()));
    }

    @Test
    public void testSummarizingChar() {
        CharSummaryStatistics result = Arrays.asList('a', 'b', 'c').stream().collect(Collectors.summarizingChar(c -> c));
        assertEquals(3, result.getCount());
        assertEquals('a', result.getMin());
        assertEquals('c', result.getMax());
    }

    @Test
    public void testSummarizingByte() {
        ByteSummaryStatistics result = Arrays.asList((byte) 1, (byte) 2, (byte) 3).stream().collect(Collectors.summarizingByte(b -> b));
        assertEquals(3, result.getCount());
        assertEquals(1, result.getMin());
        assertEquals(3, result.getMax());
        assertEquals(6, result.getSum());
    }

    @Test
    public void testSummarizingShort() {
        ShortSummaryStatistics result = Arrays.asList((short) 1, (short) 2, (short) 3).stream().collect(Collectors.summarizingShort(s -> s));
        assertEquals(3, result.getCount());
        assertEquals(1, result.getMin());
        assertEquals(3, result.getMax());
        assertEquals(6, result.getSum());
    }

    @Test
    public void testSummarizingInt() {
        IntSummaryStatistics result = integerList.stream().collect(Collectors.summarizingInt(i -> i));
        assertEquals(5, result.getCount());
        assertEquals(1, result.getMin());
        assertEquals(5, result.getMax());
        assertEquals(15, result.getSum());
        assertEquals(3.0, result.getAverage(), 0.01);
    }

    @Test
    public void testSummarizingLong() {
        LongSummaryStatistics result = Arrays.asList(1L, 2L, 3L, 4L, 5L).stream().collect(Collectors.summarizingLong(l -> l));
        assertEquals(5, result.getCount());
        assertEquals(1L, result.getMin());
        assertEquals(5L, result.getMax());
        assertEquals(15L, result.getSum());
        assertEquals(3.0, result.getAverage(), 0.01);
    }

    @Test
    public void testSummarizingFloat() {
        FloatSummaryStatistics result = Arrays.asList(1.0f, 2.0f, 3.0f).stream().collect(Collectors.summarizingFloat(f -> f));
        assertEquals(3, result.getCount());
        assertEquals(1.0f, result.getMin(), 0.01f);
        assertEquals(3.0f, result.getMax(), 0.01f);
        assertEquals(6.0f, result.getSum(), 0.01f);
    }

    @Test
    public void testSummarizingDouble() {
        DoubleSummaryStatistics result = doubleList.stream().collect(Collectors.summarizingDouble(d -> d));
        assertEquals(5, result.getCount());
        assertEquals(1.1, result.getMin(), 0.01);
        assertEquals(5.5, result.getMax(), 0.01);
        assertEquals(16.5, result.getSum(), 0.01);
        assertEquals(3.3, result.getAverage(), 0.01);
    }

    @Test
    public void testSummarizingBigInteger() {
        BigIntegerSummaryStatistics result = Arrays.asList(BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3))
                .stream()
                .collect(Collectors.summarizingBigInteger(Function.identity()));
        assertEquals(3, result.getCount());
        assertEquals(BigInteger.valueOf(1), result.getMin());
        assertEquals(BigInteger.valueOf(3), result.getMax());
        assertEquals(BigInteger.valueOf(6), result.getSum());
    }

    @Test
    public void testSummarizingBigDecimal() {
        BigDecimalSummaryStatistics result = Arrays.asList(new BigDecimal("1.1"), new BigDecimal("2.2"), new BigDecimal("3.3"))
                .stream()
                .collect(Collectors.summarizingBigDecimal(Function.identity()));
        assertEquals(3, result.getCount());
        assertEquals(new BigDecimal("1.1"), result.getMin());
        assertEquals(new BigDecimal("3.3"), result.getMax());
        assertEquals(new BigDecimal("6.6"), result.getSum());
    }

    @Test
    public void testReducingWithIdentity() {
        Integer result = integerList.stream().collect(Collectors.reducing(0, Integer::sum));
        assertEquals(15, result);
    }

    @Test
    public void testReducing() {
        Optional<Integer> result = integerList.stream().collect(Collectors.reducing(Integer::sum));
        assertTrue(result.isPresent());
        assertEquals(15, result.get());
    }

    @Test
    public void testReducingOrElseGet() {
        Integer result = Arrays.<Integer> asList().stream().collect(Collectors.reducingOrElseGet(Integer::sum, () -> 0));
        assertEquals(0, result);
    }

    @Test
    public void testReducingOrElseThrow() {
        assertThrows(NoSuchElementException.class, () -> Arrays.<Integer> asList().stream().collect(Collectors.reducingOrElseThrow(Integer::sum)));
    }

    @Test
    public void testReducingWithMapper() {
        Integer result = stringList.stream().collect(Collectors.reducing(0, String::length, Integer::sum));
        assertEquals(31, result);
    }

    @Test
    public void testReducingWithMapperOptional() {
        Integer result = stringList.stream().map(String::length).collect(Collectors.reducing(0, Integer::sum));
        assertEquals(31, result);
    }

    @Test
    public void testCommonPrefix() {
        String result = Arrays.asList("prefix_test", "prefix_demo", "prefix_example").stream().collect(Collectors.commonPrefix());
        assertEquals("prefix_", result);

        String empty = Arrays.asList("abc", "xyz").stream().collect(Collectors.commonPrefix());
        assertEquals("", empty);
    }

    @Test
    public void testCommonSuffix() {
        String result = Arrays.asList("test_suffix", "demo_suffix", "example_suffix").stream().collect(Collectors.commonSuffix());
        assertEquals("_suffix", result);

        String empty = Arrays.asList("abc", "xyz").stream().collect(Collectors.commonSuffix());
        assertEquals("", empty);
    }

    @Test
    public void testGroupingBy() {
        Map<Integer, List<String>> result = stringList.stream().collect(Collectors.groupingBy(String::length));
        assertEquals(4, result.size());
        assertEquals(Arrays.asList("apple"), result.get(5));
        assertEquals(Arrays.asList("banana", "cherry"), result.get(6));
    }

    @Test
    public void testGroupingByWithMapFactory() {
        TreeMap<Integer, List<String>> result = stringList.stream().collect(Collectors.groupingBy(String::length, TreeMap::new));
        assertTrue(result instanceof TreeMap);
        assertEquals(4, result.size());
    }

    @Test
    public void testGroupingByWithDownstream() {
        Map<String, Long> result = personList.stream().collect(Collectors.groupingBy(Person::getDepartment, Collectors.counting()));
        assertEquals(3, result.size());
        assertEquals(2L, result.get("Engineering"));
        assertEquals(2L, result.get("Sales"));
        assertEquals(1L, result.get("HR"));
    }

    @Test
    public void testGroupingByWithDownstreamAndMapFactory() {
        TreeMap<String, Long> result = personList.stream().collect(Collectors.groupingBy(Person::getDepartment, Collectors.counting(), TreeMap::new));
        assertTrue(result instanceof TreeMap);
        assertEquals(3, result.size());
    }

    @Test
    public void testGroupingByConcurrent() {
        ConcurrentMap<Integer, List<String>> result = stringList.stream().collect(Collectors.groupingByConcurrent(String::length));
        assertEquals(4, result.size());
        assertTrue(result instanceof ConcurrentMap);
    }

    @Test
    public void testGroupingByConcurrentWithMapFactory() {
        ConcurrentSkipListMap<Integer, List<String>> result = stringList.stream()
                .collect(Collectors.groupingByConcurrent(String::length, ConcurrentSkipListMap::new));
        assertTrue(result instanceof ConcurrentSkipListMap);
        assertEquals(4, result.size());
    }

    @Test
    public void testGroupingByConcurrentWithDownstream() {
        ConcurrentMap<String, Long> result = personList.stream().collect(Collectors.groupingByConcurrent(Person::getDepartment, Collectors.counting()));
        assertEquals(3, result.size());
        assertTrue(result instanceof ConcurrentMap);
    }

    @Test
    public void testPartitioningBy() {
        Map<Boolean, List<String>> result = stringList.stream().collect(Collectors.partitioningBy(s -> s.length() > 5));
        assertEquals(2, result.size());
        assertEquals(Arrays.asList("banana", "cherry", "elderberry"), result.get(true));
        assertEquals(Arrays.asList("apple", "date"), result.get(false));
    }

    @Test
    public void testPartitioningByWithDownstream() {
        Map<Boolean, Long> result = stringList.stream().collect(Collectors.partitioningBy(s -> s.length() > 5, Collectors.counting()));
        assertEquals(2, result.size());
        assertEquals(3L, result.get(true));
        assertEquals(2L, result.get(false));
    }

    @Test
    public void testCountingBy() {
        Map<Integer, Long> result = stringList.stream().collect(Collectors.countingBy(String::length));
        assertEquals(4, result.size());
        assertEquals(1L, result.get(5));
        assertEquals(2L, result.get(6));
    }

    @Test
    public void testCountingByWithMapFactory() {
        TreeMap<Integer, Long> result = stringList.stream().collect(Collectors.countingBy(String::length, TreeMap::new));
        assertTrue(result instanceof TreeMap);
        assertEquals(4, result.size());
    }

    @Test
    public void testCountingToIntBy() {
        Map<Integer, Integer> result = stringList.stream().collect(Collectors.countingToIntBy(String::length));
        assertEquals(4, result.size());
        assertEquals(1, result.get(5));
        assertEquals(2, result.get(6));
    }

    @Test
    public void testToMapFromEntries() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        Map<String, Integer> result = source.entrySet().stream().collect(Collectors.toMap());
        assertEquals(source, result);
    }

    @Test
    public void testToMapFromEntriesWithMergeFunction() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2),
                new AbstractMap.SimpleEntry<>("a", 3));

        Map<String, Integer> result = entries.stream().collect(Collectors.toMap(Integer::sum));
        assertEquals(4, result.get("a"));
        assertEquals(2, result.get("b"));
    }

    @Test
    public void testToMapWithKeyValueMappers() {
        Map<String, Integer> result = stringList.stream().collect(Collectors.toMap(Function.identity(), String::length));
        assertEquals(5, result.size());
        assertEquals(5, result.get("apple"));
        assertEquals(6, result.get("banana"));
    }

    @Test
    public void testToMapWithKeyValueMappersAndMergeFunction() {
        Map<Integer, String> result = stringList.stream().collect(Collectors.toMap(String::length, Function.identity(), (v1, v2) -> v1 + "," + v2));
        assertEquals("banana,cherry", result.get(6));
    }

    @Test
    public void testToMapWithMapFactory() {
        TreeMap<String, Integer> result = stringList.stream().collect(Collectors.toMap(Function.identity(), String::length, TreeMap::new));
        assertTrue(result instanceof TreeMap);
        assertEquals(5, result.size());
    }

    @Test
    public void testToMapWithAllParameters() {
        TreeMap<Integer, String> result = stringList.stream()
                .collect(Collectors.toMap(String::length, Function.identity(), (v1, v2) -> v1 + "," + v2, TreeMap::new));
        assertTrue(result instanceof TreeMap);
        assertEquals("banana,cherry", result.get(6));
    }

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
    public void testToImmutableMapWithKeyValueMappers() {
        ImmutableMap<String, Integer> result = stringList.stream().collect(Collectors.toImmutableMap(Function.identity(), String::length));
        assertEquals(5, result.size());
        assertThrows(UnsupportedOperationException.class, () -> result.put("test", 4));
    }

    @Test
    public void testToUnmodifiableMap() {
        Map<String, Integer> result = stringList.stream().collect(Collectors.toUnmodifiableMap(Function.identity(), String::length));
        assertEquals(5, result.size());
        assertThrows(UnsupportedOperationException.class, () -> result.put("test", 4));
    }

    @Test
    public void testToUnmodifiableMapWithMergeFunction() {
        Map<Integer, String> result = stringList.stream().collect(Collectors.toUnmodifiableMap(String::length, Function.identity(), (v1, v2) -> v1 + "," + v2));
        assertEquals("banana,cherry", result.get(6));
        assertThrows(UnsupportedOperationException.class, () -> result.put(7, "test"));
    }

    @Test
    public void testToLinkedHashMap() {
        Map<String, Integer> result = stringList.stream().collect(Collectors.toLinkedHashMap(Function.identity(), String::length));
        assertTrue(result instanceof LinkedHashMap);
        assertEquals(5, result.size());
        assertEquals(Arrays.asList("apple", "banana", "cherry", "date", "elderberry"), new ArrayList<>(result.keySet()));
    }

    @Test
    public void testToConcurrentMap() {
        ConcurrentMap<String, Integer> result = stringList.stream().collect(Collectors.toConcurrentMap(Function.identity(), String::length));
        assertTrue(result instanceof ConcurrentMap);
        assertEquals(5, result.size());
    }

    @Test
    public void testToConcurrentMapWithMapFactory() {
        ConcurrentSkipListMap<String, Integer> result = stringList.stream()
                .collect(Collectors.toConcurrentMap(Function.identity(), String::length, ConcurrentSkipListMap::new));
        assertTrue(result instanceof ConcurrentSkipListMap);
        assertEquals(5, result.size());
    }

    @Test
    public void testToConcurrentMapWithMergeFunction() {
        ConcurrentMap<Integer, String> result = stringList.stream()
                .collect(Collectors.toConcurrentMap(String::length, Function.identity(), (v1, v2) -> v1 + "," + v2));
        assertEquals("banana,cherry", result.get(6));
    }

    @Test
    public void testToBiMap() {
        BiMap<Integer, String> result = Arrays.asList("a", "bb", "ccc").stream().collect(Collectors.toBiMap(String::length, Function.identity()));
        assertEquals(3, result.size());
        assertEquals("a", result.get(1));
        assertEquals(1, result.inversed().get("a"));
    }

    @Test
    public void testToBiMapWithMapFactory() {
        BiMap<Integer, String> result = Arrays.asList("a", "bb", "ccc")
                .stream()
                .collect(Collectors.toBiMap(String::length, Function.identity(), Suppliers.ofBiMap()));
        assertEquals(3, result.size());
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
    public void testToMultimapWithKeyMapper() {
        ListMultimap<Integer, String> result = stringList.stream().collect(Collectors.toMultimap(String::length));
        assertEquals(1, result.get(5).size());
        assertEquals(2, result.get(6).size());
    }

    @Test
    public void testToMultimapWithKeyValueMappers() {
        ListMultimap<String, String> result = personList.stream().collect(Collectors.toMultimap(Person::getDepartment, Person::getName));
        assertEquals(2, result.get("Engineering").size());
        assertEquals(2, result.get("Sales").size());
        assertEquals(1, result.get("HR").size());
    }

    @Test
    public void testFlatMappingValueToMultimap() {
        List<Department> departments = Arrays.asList(new Department("Engineering", Arrays.asList("Java", "Python")),
                new Department("Sales", Arrays.asList("Communication", "Negotiation")));

        ListMultimap<String, String> result = departments.stream()
                .collect(Collectors.flatmappingValueToMultimap(Department::getName, dept -> dept.getSkills()));

        assertEquals(2, result.get("Engineering").size());
        assertEquals(2, result.get("Sales").size());
    }

    @Test
    public void testFlatmappingValueToMultimapCollection() {
        List<Department> departments = Arrays.asList(new Department("Engineering", Arrays.asList("Java", "Python")),
                new Department("Sales", Arrays.asList("Communication", "Negotiation")));

        ListMultimap<String, String> result = departments.stream().collect(Collectors.flatmappingValueToMultimap(Department::getName, Department::getSkills));

        assertEquals(2, result.get("Engineering").size());
        assertEquals(2, result.get("Sales").size());
    }

    @Test
    public void testFlatMappingKeyToMultimap() {
        List<Employee> employees = Arrays.asList(new Employee("John", Arrays.asList("Java", "Python")),
                new Employee("Jane", Arrays.asList("Java", "JavaScript")));

        ListMultimap<String, String> result = employees.stream().collect(Collectors.flatmappingKeyToMultimap(emp -> emp.getSkills(), Employee::getName));

        assertEquals(2, result.get("Java").size());
        assertTrue(result.get("Java").contains("John"));
        assertTrue(result.get("Java").contains("Jane"));
    }

    @Test
    public void testFlatmappingKeyToMultimapCollection() {
        List<Employee> employees = Arrays.asList(new Employee("John", Arrays.asList("Java", "Python")),
                new Employee("Jane", Arrays.asList("Java", "JavaScript")));

        ListMultimap<String, String> result = employees.stream().collect(Collectors.flatmappingKeyToMultimap(Employee::getSkills, Employee::getName));

        assertEquals(2, result.get("Java").size());
    }

    @Test
    public void testTeeing() {
        Pair<Long, Integer> result = integerList.stream()
                .collect(Collectors.teeing(Collectors.counting(), Collectors.summingInt(i -> i), (count, sum) -> Pair.of(count, sum)));

        assertEquals(5L, result.left());
        assertEquals(15, result.right());
    }

    @Test
    public void testMoreCollectorsSummingInt2() {
        Tuple2<Integer, Integer> result = personList.stream().collect(MoreCollectors.summingInt(p -> p.getAge(), p -> p.getDepartment().length()));

        assertEquals(150, result._1);
        assertTrue(result._2 > 0);
    }

    @Test
    public void testMoreCollectorsSummingInt3() {
        Tuple3<Integer, Integer, Integer> result = Arrays.asList(1, 2, 3).stream().collect(MoreCollectors.summingInt(i -> i, i -> i * 2, i -> i * 3));

        assertEquals(6, result._1);
        assertEquals(12, result._2);
        assertEquals(18, result._3);
    }

    @Test
    public void testMoreCollectorsSummingIntToLong2() {
        Tuple2<Long, Long> result = integerList.stream().collect(MoreCollectors.summingIntToLong(i -> i, i -> i * 1000));

        assertEquals(15L, result._1);
        assertEquals(15000L, result._2);
    }

    @Test
    public void testMoreCollectorsSummingLong2() {
        Tuple2<Long, Long> result = Arrays.asList(1L, 2L, 3L).stream().collect(MoreCollectors.summingLong(l -> l, l -> l * 10));

        assertEquals(6L, result._1);
        assertEquals(60L, result._2);
    }

    @Test
    public void testMoreCollectorsSummingDouble2() {
        Tuple2<Double, Double> result = doubleList.stream().collect(MoreCollectors.summingDouble(d -> d, d -> d * 2));

        assertEquals(16.5, result._1, 0.01);
        assertEquals(33.0, result._2, 0.01);
    }

    @Test
    public void testMoreCollectorsSummingBigInteger2() {
        Tuple2<BigInteger, BigInteger> result = Arrays.asList(BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3))
                .stream()
                .collect(MoreCollectors.summingBigInteger(bi -> bi, bi -> bi.multiply(BigInteger.TEN)));

        assertEquals(BigInteger.valueOf(6), result._1);
        assertEquals(BigInteger.valueOf(60), result._2);
    }

    @Test
    public void testMoreCollectorsSummingBigDecimal2() {
        Tuple2<BigDecimal, BigDecimal> result = Arrays.asList(new BigDecimal("1.1"), new BigDecimal("2.2"), new BigDecimal("3.3"))
                .stream()
                .collect(MoreCollectors.summingBigDecimal(bd -> bd, bd -> bd.multiply(new BigDecimal("2"))));

        assertEquals(new BigDecimal("6.6"), result._1);
        assertEquals(new BigDecimal("13.2"), result._2);
    }

    @Test
    public void testMoreCollectorsAveragingInt2() {
        Tuple2<Double, Double> result = integerList.stream().collect(MoreCollectors.averagingInt(i -> i, i -> i * 2));

        assertEquals(3.0, result._1, 0.01);
        assertEquals(6.0, result._2, 0.01);
    }

    @Test
    public void testMoreCollectorsAveragingLong2() {
        Tuple2<Double, Double> result = Arrays.asList(1L, 2L, 3L).stream().collect(MoreCollectors.averagingLong(l -> l, l -> l * 10));

        assertEquals(2.0, result._1, 0.01);
        assertEquals(20.0, result._2, 0.01);
    }

    @Test
    public void testMoreCollectorsAveragingDouble2() {
        Tuple2<Double, Double> result = Arrays.asList(1.0, 2.0, 3.0).stream().collect(MoreCollectors.averagingDouble(d -> d, d -> d * 2));

        assertEquals(2.0, result._1, 0.01);
        assertEquals(4.0, result._2, 0.01);
    }

    @Test
    public void testMoreCollectorsAveragingBigInteger2() {
        Tuple2<BigDecimal, BigDecimal> result = Arrays.asList(BigInteger.valueOf(2), BigInteger.valueOf(4), BigInteger.valueOf(6))
                .stream()
                .collect(MoreCollectors.averagingBigInteger(bi -> bi, bi -> bi.multiply(BigInteger.TEN)));

        assertEquals(0, new BigDecimal("4").compareTo(result._1));
        assertEquals(0, new BigDecimal("40").compareTo(result._2));
    }

    @Test
    public void testMoreCollectorsAveragingBigDecimal2() {
        Tuple2<BigDecimal, BigDecimal> result = Arrays.asList(new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0"))
                .stream()
                .collect(MoreCollectors.averagingBigDecimal(bd -> bd, bd -> bd.multiply(new BigDecimal("2"))));

        assertEquals(0, new BigDecimal("2").compareTo(result._1));
        assertEquals(0, new BigDecimal("4").compareTo(result._2));
    }

    @Test
    public void testMoreCollectorsCombine2() {
        Tuple2<Long, Integer> result = integerList.stream().collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(i -> i)));

        assertEquals(5L, result._1);
        assertEquals(15, result._2);
    }

    @Test
    public void testMoreCollectorsCombine3() {
        Tuple3<Long, Integer, OptionalDouble> result = integerList.stream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(i -> i), Collectors.averagingIntOrEmpty(i -> i)));

        assertEquals(5L, result._1);
        assertEquals(15, result._2);
        assertEquals(3.0, result._3.orElseThrow(), 0.01);
    }

    @Test
    public void testMoreCollectorsCombine4() {
        Tuple4<Long, Integer, Optional<Integer>, Optional<Integer>> result = integerList.stream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(i -> i), Collectors.minBy(Fn.identity()),
                        Collectors.maxBy(Fn.identity())));

        assertEquals(5L, result._1);
        assertEquals(15, result._2);
        assertEquals(1, result._3.get());
        assertEquals(5, result._4.get());
    }

    @Test
    public void testMoreCollectorsCombine5() {
        Tuple5<Long, Integer, Optional<Integer>, Optional<Integer>, OptionalDouble> result = integerList.stream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(i -> i), Collectors.minBy(Fn.identity()),
                        Collectors.maxBy(Fn.identity()), Collectors.averagingIntOrEmpty(i -> i)));

        assertEquals(5L, result._1);
        assertEquals(15, result._2);
        assertEquals(1, result._3.get());
        assertEquals(5, result._4.get());
        assertEquals(3.0, result._5.orElseThrow(), 0.01);
    }

    @Test
    public void testMoreCollectorsCombine6() {
        Tuple6<Long, Integer, Optional<Integer>, Optional<Integer>, OptionalDouble, List<Integer>> result = integerList.stream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(i -> i), Collectors.minBy(Fn.identity()),
                        Collectors.maxBy(Fn.identity()), Collectors.averagingIntOrEmpty(i -> i), Collectors.toList()));

        assertEquals(5L, result._1);
        assertEquals(15, result._2);
        assertEquals(1, result._3.get());
        assertEquals(5, result._4.get());
        assertEquals(3.0, result._5.get(), 0.01);
        assertEquals(integerList, result._6);
    }

    @Test
    public void testMoreCollectorsCombine7() {
        Tuple7<Long, Integer, Optional<Integer>, Optional<Integer>, OptionalDouble, List<Integer>, Set<Integer>> result = integerList.stream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(i -> i), Collectors.minBy(Fn.identity()),
                        Collectors.maxBy(Fn.identity()), Collectors.averagingIntOrEmpty(i -> i), Collectors.toList(), Collectors.toSet()));

        assertEquals(5L, result._1);
        assertEquals(15, result._2);
        assertEquals(1, result._3.get());
        assertEquals(5, result._4.get());
        assertEquals(3.0, result._5.get(), 0.01);
        assertEquals(integerList, result._6);
        assertEquals(new HashSet<>(integerList), result._7);
    }

    @Test
    public void testMoreCollectorsCombineWithMerger2() {
        String result = integerList.stream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(i -> i), (count, sum) -> "Count: " + count + ", Sum: " + sum));

        assertEquals("Count: 5, Sum: 15", result);
    }

    @Test
    public void testMoreCollectorsCombineWithMerger3() {
        String result = integerList.stream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(i -> i), Collectors.averagingIntOrEmpty(i -> i),
                        (count, sum, avg) -> String.format("Count: %d, Sum: %d, Avg: %.2f", count, sum, ((OptionalDouble) avg).get())));

        assertEquals("Count: 5, Sum: 15, Avg: 3.00", result);
    }

    @Test
    public void testMoreCollectorsCombineWithMerger4() {
        Map<String, Object> result = integerList.stream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(i -> i), Collectors.minBy(Fn.identity()),
                        Collectors.maxBy(Fn.identity()), (count, sum, min, max) -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("count", count);
                            map.put("sum", sum);
                            map.put("min", min.orElse(null));
                            map.put("max", max.orElse(null));
                            return map;
                        }));

        assertEquals(5L, result.get("count"));
        assertEquals(15, result.get("sum"));
        assertEquals(1, result.get("min"));
        assertEquals(5, result.get("max"));
    }

    @Test
    public void testMoreCollectorsCombineWithCollection() {
        List<Collector<Integer, ?, ?>> collectors = Arrays.asList(Collectors.counting(), Collectors.summingInt(i -> i), Collectors.averagingIntOrEmpty(i -> i));

        String result = integerList.stream()
                .collect(MoreCollectors.combine(collectors,
                        results -> String.format("Count: %d, Sum: %d, Avg: %.2f", results[0], results[1], ((OptionalDouble) results[2]).get())));

        assertEquals("Count: 5, Sum: 15, Avg: 3.00", result);
    }

    @Test
    public void testMoreCollectorsToDataset() {
        Dataset result = personList.stream().collect(MoreCollectors.toDataset());

        assertNotNull(result);
        assertEquals(personList.size(), result.size());
    }

    @Test
    public void testMoreCollectorsToDatasetWithColumnNames() {
        List<String> columnNames = Arrays.asList("name", "age", "department");
        Dataset result = personList.stream().collect(MoreCollectors.toDataset(columnNames));

        assertNotNull(result);
        assertEquals(personList.size(), result.size());
        assertEquals(columnNames, result.columnNameList());
    }

    @Test
    public void testEmptyStreamHandling() {
        List<String> emptyList = Arrays.asList();

        assertTrue(emptyList.stream().collect(Collectors.toList()).isEmpty());
        assertTrue(emptyList.stream().collect(Collectors.toSet()).isEmpty());
        assertFalse(emptyList.stream().collect(Collectors.first()).isPresent());
        assertFalse(emptyList.stream().collect(Collectors.last()).isPresent());
        assertEquals("", emptyList.stream().collect(Collectors.joining()));
        assertEquals(0L, emptyList.stream().collect(Collectors.counting()));
        assertEquals(0, emptyList.stream().collect(Collectors.countingToInt()));
    }

    @Test
    public void testNullHandling() {
        List<String> listWithNulls = Arrays.asList("a", null, "b", null, "c");

        List<String> resultList = listWithNulls.stream().collect(Collectors.toList());
        assertEquals(5, resultList.size());
        assertNull(resultList.get(1));
        assertNull(resultList.get(3));
    }

    @Test
    public void testLargeDataset() {
        List<Integer> largeList = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            largeList.add(i);
        }

        Long count = largeList.stream().collect(Collectors.counting());
        assertEquals(10000L, count);

        Integer sum = largeList.stream().collect(Collectors.summingInt(i -> i));
        assertEquals(49995000, sum);

        OptionalDouble avg = largeList.stream().collect(Collectors.averagingIntOrEmpty(i -> i));
        assertTrue(avg.isPresent());
        assertEquals(4999.5, avg.getAsDouble(), 0.01);
    }

    @Test
    public void testParallelStreamCompatibility() {
        Long countSequential = integerList.stream().collect(Collectors.counting());
        Long countParallel = integerList.parallelStream().collect(Collectors.counting());
        assertEquals(countSequential, countParallel);

        Integer sumSequential = integerList.stream().collect(Collectors.summingInt(i -> i));
        Integer sumParallel = integerList.parallelStream().collect(Collectors.summingInt(i -> i));
        assertEquals(sumSequential, sumParallel);

        Map<Integer, List<String>> groupSequential = stringList.stream().collect(Collectors.groupingBy(String::length));
        Map<Integer, List<String>> groupParallel = stringList.parallelStream().collect(Collectors.groupingBy(String::length));
        assertEquals(groupSequential.keySet(), groupParallel.keySet());
    }

    @Test
    public void testCharacteristicsPreservation() {
        Collector<String, ?, List<String>> listCollector = Collectors.toList();
        assertFalse(listCollector.characteristics().contains(Characteristics.UNORDERED));

        Collector<String, ?, Set<String>> setCollector = Collectors.toSet();
        assertTrue(setCollector.characteristics().contains(Characteristics.UNORDERED));

        Collector<String, ?, ConcurrentMap<String, List<String>>> concurrentCollector = Collectors.groupingByConcurrent(String::toLowerCase);
        assertTrue(concurrentCollector.characteristics().contains(Characteristics.CONCURRENT));
        assertTrue(concurrentCollector.characteristics().contains(Characteristics.UNORDERED));
    }

    @Test
    public void testComplexNestedCollectors() {
        Map<String, Map<Integer, Long>> nestedResult = personList.stream()
                .collect(Collectors.groupingBy(Person::getDepartment, Collectors.groupingBy(p -> p.getAge() / 10 * 10, Collectors.counting())));

        assertNotNull(nestedResult);
        assertEquals(3, nestedResult.size());
        assertTrue(nestedResult.containsKey("Engineering"));
        assertTrue(nestedResult.containsKey("Sales"));
        assertTrue(nestedResult.containsKey("HR"));
    }

    @Test
    public void testCustomCollectorIntegration() {
        Collector<String, StringBuilder, String> customJoiner = Collectors.create(StringBuilder::new, (sb, s) -> {
            if (sb.length() > 0)
                sb.append("|");
            sb.append(s.toUpperCase());
        }, (sb1, sb2) -> {
            if (sb1.length() > 0 && sb2.length() > 0)
                sb1.append("|");
            sb1.append(sb2);
            return sb1;
        }, StringBuilder::toString);

        String result = stringList.stream().collect(customJoiner);
        assertEquals("APPLE|BANANA|CHERRY|DATE|ELDERBERRY", result);
    }

    @Test
    public void testErrorConditions() {

        assertThrows(IllegalStateException.class, () -> Arrays.asList("a", "b", "a").stream().collect(Collectors.toMap(Function.identity(), String::length)));

        assertThrows(NullPointerException.class, () -> Arrays.asList("a", null, "b").stream().collect(Collectors.groupingBy(Function.identity())));

        assertThrows(UnsupportedOperationException.class, () -> stringList.parallelStream().collect(Collectors.first()));

        assertThrows(UnsupportedOperationException.class, () -> stringList.parallelStream().collect(Collectors.last()));
    }
}
