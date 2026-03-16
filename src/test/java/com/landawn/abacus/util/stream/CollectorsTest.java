package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.DoubleSummaryStatistics;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
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
import com.landawn.abacus.util.N;
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
import com.landawn.abacus.util.u;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.Collectors.MoreCollectors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Tag("2025")
public class CollectorsTest extends TestBase {

    private List<String> stringList;
    private List<Integer> integerList;
    private List<Double> doubleList;
    private List<Person> personList;
    private List<Boolean> booleanList;
    private List<Character> charList;
    private List<Byte> byteList;
    private List<Short> shortList;
    private List<Long> longList;
    private List<Float> floatList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Person {
        private String name;
        private int age;
        private String department;
        private BigInteger bigIntValue;
        private BigDecimal bigDecValue;
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
        personList = Arrays.asList(new Person("Alice", 25, "Engineering", BigInteger.valueOf(100), BigDecimal.valueOf(100.5)),
                new Person("Bob", 30, "Engineering", BigInteger.valueOf(200), BigDecimal.valueOf(200.5)),
                new Person("Charlie", 35, "Sales", BigInteger.valueOf(300), BigDecimal.valueOf(300.5)),
                new Person("David", 28, "Sales", BigInteger.valueOf(400), BigDecimal.valueOf(400.5)),
                new Person("Eve", 32, "HR", BigInteger.valueOf(500), BigDecimal.valueOf(500.5)));
        booleanList = Arrays.asList(true, false, true, false, true);
        charList = Arrays.asList('a', 'b', 'c', 'd', 'e');
        byteList = Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        shortList = Arrays.asList((short) 10, (short) 20, (short) 30, (short) 40, (short) 50);
        longList = Arrays.asList(100L, 200L, 300L, 400L, 500L);
        floatList = Arrays.asList(1.1f, 2.2f, 3.3f, 4.4f, 5.5f);
    }

    @Test
    public void testCreate_SupplierAccumulatorCombiner() {
        Collector<String, List<String>, List<String>> collector = Collectors.create(ArrayList::new, List::add, (a, b) -> {
            a.addAll(b);
            return a;
        });

        List<String> result = stringList.stream().collect(collector);
        assertEquals(stringList, result);
    }

    @Test
    public void testCreate_SupplierAccumulatorCombinerCharacteristics_VarArgs() {
        Collector<String, List<String>, List<String>> collector = Collectors.create(ArrayList::new, List::add, (a, b) -> {
            a.addAll(b);
            return a;
        }, Characteristics.IDENTITY_FINISH);

        List<String> result = stringList.stream().collect(collector);
        assertEquals(stringList, result);
        assertTrue(collector.characteristics().contains(Characteristics.IDENTITY_FINISH));
    }

    @Test
    public void testCreate_SupplierAccumulatorCombinerFinisher() {
        Collector<String, List<String>, Integer> collector = Collectors.create(ArrayList::new, List::add, (a, b) -> {
            a.addAll(b);
            return a;
        }, List::size);

        Integer result = stringList.stream().collect(collector);
        assertEquals(5, result);
    }

    @Test
    public void testCreate_SupplierAccumulatorCombinerFinisherCharacteristics_VarArgs() {
        Collector<String, List<String>, Integer> collector = Collectors.create(ArrayList::new, List::add, (a, b) -> {
            a.addAll(b);
            return a;
        }, List::size, Characteristics.UNORDERED);

        Integer result = stringList.stream().collect(collector);
        assertEquals(5, result);
        assertTrue(collector.characteristics().contains(Characteristics.UNORDERED));
    }

    @Test
    public void testCreate_SupplierAccumulatorCombinerCharacteristics_Collection() {
        Set<Characteristics> characteristics = EnumSet.of(Characteristics.IDENTITY_FINISH, Characteristics.UNORDERED);
        Collector<String, List<String>, List<String>> collector = Collectors.create(ArrayList::new, List::add, (a, b) -> {
            a.addAll(b);
            return a;
        }, characteristics);

        List<String> result = stringList.stream().collect(collector);
        assertEquals(stringList, result);
        assertTrue(collector.characteristics().containsAll(characteristics));
    }

    @Test
    public void testToCollection_SupplierOnly() {
        Collector<String, ?, LinkedList<String>> collector = Collectors.toCollection(LinkedList::new);
        LinkedList<String> result = stringList.stream().collect(collector);
        assertEquals(stringList, result);
        assertTrue(result instanceof LinkedList);
    }

    @Test
    public void testToCollection_WithLimit() {
        Collector<String, ?, List<String>> collector = Collectors.toCollection(3, ArrayList::new);
        List<String> result = stringList.stream().collect(collector);
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("apple", "banana", "cherry"), result);
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
    public void testToList() {
        List<String> result = stringList.stream().collect(Collectors.toList());
        assertEquals(stringList, result);
    }

    @Test
    public void testToList_Empty() {
        List<String> result = Stream.<String> empty().collect(Collectors.toList());
        assertTrue(result.isEmpty());
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
    public void testToSet() {
        Set<String> result = stringList.stream().collect(Collectors.toSet());
        assertEquals(new HashSet<>(stringList), result);
    }

    @Test
    public void testToSet_Empty() {
        Set<String> result = Stream.<String> empty().collect(Collectors.toSet());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToLinkedHashSet() {
        Set<String> result = stringList.stream().collect(Collectors.toLinkedHashSet());
        assertEquals(new LinkedHashSet<>(stringList), result);
        assertTrue(result instanceof LinkedHashSet);
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
    public void testToList_WithLimit() {
        List<String> result = stringList.stream().collect(Collectors.toList(3));
        assertEquals(3, result.size());
    }

    @Test
    public void testToSet_WithLimit() {
        Set<String> result = stringList.stream().collect(Collectors.toSet(3));
        assertEquals(3, result.size());
    }

    @Test
    public void testToMultiset() {
        List<String> duplicates = Arrays.asList("a", "b", "a", "c", "b", "a");
        Multiset<String> result = duplicates.stream().collect(Collectors.toMultiset());
        assertEquals(3, result.get("a"));
        assertEquals(2, result.get("b"));
        assertEquals(1, result.get("c"));
    }

    @Test
    public void testToMultiset_WithSupplier() {
        List<String> duplicates = Arrays.asList("a", "b", "a", "c", "b", "a");
        Multiset<String> result = duplicates.stream().collect(Collectors.toMultiset(Multiset::new));
        assertEquals(3, result.get("a"));
        assertEquals(2, result.get("b"));
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
    public void testToArray_IntFunction() {
        String[] result = stringList.stream().collect(Collectors.toArray(String[]::new));
        assertArrayEquals(stringList.toArray(new String[0]), result);
    }

    @Test
    public void testToBooleanList() {
        BooleanList result = booleanList.stream().collect(Collectors.toBooleanList());
        assertEquals(booleanList.size(), result.size());
        assertTrue(result.get(0));
        assertFalse(result.get(1));
    }

    @Test
    public void testToBooleanArray() {
        boolean[] result = booleanList.stream().collect(Collectors.toBooleanArray());
        assertEquals(booleanList.size(), result.length);
        assertTrue(result[0]);
        assertFalse(result[1]);
    }

    @Test
    public void testToCharList() {
        CharList result = charList.stream().collect(Collectors.toCharList());
        assertEquals(charList.size(), result.size());
        assertEquals('a', result.get(0));
    }

    @Test
    public void testToCharArray() {
        char[] result = charList.stream().collect(Collectors.toCharArray());
        assertEquals(charList.size(), result.length);
        assertEquals('a', result[0]);
    }

    @Test
    public void testToByteList() {
        ByteList result = byteList.stream().collect(Collectors.toByteList());
        assertEquals(byteList.size(), result.size());
        assertEquals((byte) 1, result.get(0));
    }

    @Test
    public void testToByteArray() {
        byte[] result = byteList.stream().collect(Collectors.toByteArray());
        assertEquals(byteList.size(), result.length);
        assertEquals((byte) 1, result[0]);
    }

    @Test
    public void testToShortList() {
        ShortList result = shortList.stream().collect(Collectors.toShortList());
        assertEquals(shortList.size(), result.size());
        assertEquals((short) 10, result.get(0));
    }

    @Test
    public void testToShortArray() {
        short[] result = shortList.stream().collect(Collectors.toShortArray());
        assertEquals(shortList.size(), result.length);
        assertEquals((short) 10, result[0]);
    }

    @Test
    public void testToIntList() {
        IntList result = integerList.stream().collect(Collectors.toIntList());
        assertEquals(integerList.size(), result.size());
        assertEquals(1, result.get(0));
    }

    @Test
    public void testToIntArray() {
        int[] result = integerList.stream().collect(Collectors.toIntArray());
        assertEquals(integerList.size(), result.length);
        assertEquals(1, result[0]);
    }

    @Test
    public void testToLongList() {
        LongList result = longList.stream().collect(Collectors.toLongList());
        assertEquals(longList.size(), result.size());
        assertEquals(100L, result.get(0));
    }

    @Test
    public void testToLongArray() {
        long[] result = longList.stream().collect(Collectors.toLongArray());
        assertEquals(longList.size(), result.length);
        assertEquals(100L, result[0]);
    }

    @Test
    public void testToFloatList() {
        FloatList result = floatList.stream().collect(Collectors.toFloatList());
        assertEquals(floatList.size(), result.size());
        assertEquals(1.1f, result.get(0), 0.01);
    }

    @Test
    public void testToFloatArray() {
        float[] result = floatList.stream().collect(Collectors.toFloatArray());
        assertEquals(floatList.size(), result.length);
        assertEquals(1.1f, result[0], 0.01);
    }

    @Test
    public void testToDoubleList() {
        DoubleList result = doubleList.stream().collect(Collectors.toDoubleList());
        assertEquals(doubleList.size(), result.size());
        assertEquals(1.1, result.get(0), 0.01);
    }

    @Test
    public void testToDoubleArray() {
        double[] result = doubleList.stream().collect(Collectors.toDoubleArray());
        assertEquals(doubleList.size(), result.length);
        assertEquals(1.1, result[0], 0.01);
    }

    @Test
    public void testOnlyOne() {
        Optional<String> result = Arrays.asList("single").stream().collect(Collectors.onlyOne());
        assertTrue(result.isPresent());
        assertEquals("single", result.get());
    }

    @Test
    public void testOnlyOne_MultipleElements_ThrowsException() {
        assertThrows(TooManyElementsException.class, () -> stringList.stream().collect(Collectors.onlyOne()));
    }

    @Test
    public void testOnlyOne_Empty() {
        Optional<String> result = Stream.<String> empty().collect(Collectors.onlyOne());
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne_WithPredicate() {
        Optional<Person> result = personList.stream().collect(Collectors.onlyOne(p -> p.getAge() == 25));
        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().getName());
    }

    @Test
    public void testFirst() {
        Optional<String> result = stringList.stream().collect(Collectors.first());
        assertTrue(result.isPresent());
        assertEquals("apple", result.get());
    }

    @Test
    public void testFirst_Empty() {
        Optional<String> result = Stream.<String> empty().collect(Collectors.first());
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast() {
        Optional<String> result = stringList.stream().collect(Collectors.last());
        assertTrue(result.isPresent());
        assertEquals("elderberry", result.get());
    }

    @Test
    public void testLast_Empty() {
        Optional<String> result = Stream.<String> empty().collect(Collectors.last());
        assertFalse(result.isPresent());
    }

    @Test
    public void testFirst_N() {
        List<String> result = stringList.stream().collect(Collectors.first(3));
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("apple", "banana", "cherry"), result);
    }

    @Test
    public void testLast_N() {
        List<String> result = stringList.stream().collect(Collectors.last(3));
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("cherry", "date", "elderberry"), result);
    }

    @Test
    public void testJoining() {
        String result = stringList.stream().collect(Collectors.joining());
        assertEquals("applebananacherrydateelderberry", result);
    }

    @Test
    public void testJoining_WithDelimiter() {
        String result = stringList.stream().collect(Collectors.joining(", "));
        assertEquals("apple, banana, cherry, date, elderberry", result);
    }

    @Test
    public void testJoining_WithDelimiterPrefixSuffix() {
        String result = stringList.stream().collect(Collectors.joining(", ", "[", "]"));
        assertEquals("[apple, banana, cherry, date, elderberry]", result);
    }

    @Test
    public void testFiltering() {
        List<String> result = stringList.stream().collect(Collectors.filtering(s -> s.startsWith("a"), Collectors.toList()));
        assertEquals(Arrays.asList("apple"), result);
    }

    @Test
    public void testFilteringToList() {
        List<String> result = stringList.stream().collect(Collectors.filteringToList(s -> s.startsWith("b")));
        assertEquals(Arrays.asList("banana"), result);
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
    public void testFlatmapping() {
        List<String> words = Arrays.asList("hello world", "foo bar");
        List<String> result = words.stream().collect(Collectors.flatmapping(s -> Arrays.asList(s.split(" ")), Collectors.toList()));
        assertEquals(Arrays.asList("hello", "world", "foo", "bar"), result);
    }

    @Test
    public void testFlatmappingToList() {
        List<String> words = Arrays.asList("hello world", "foo bar");
        List<String> result = words.stream().collect(Collectors.flatmappingToList(s -> Arrays.asList(s.split(" "))));
        assertEquals(Arrays.asList("hello", "world", "foo", "bar"), result);
    }

    @Test
    public void testCollectingAndThen() {
        Integer result = stringList.stream().collect(Collectors.collectingAndThen(Collectors.toList(), List::size));
        assertEquals(5, result);
    }

    @Test
    public void testCollectingOrEmpty() {
        Optional<List<String>> result = stringList.stream().collect(Collectors.collectingOrEmpty(Collectors.toList()));
        assertTrue(result.isPresent());
        assertEquals(stringList, result.get());
    }

    @Test
    public void testCollectingOrEmpty_Empty() {
        Optional<List<String>> result = Stream.<String> empty().collect(Collectors.collectingOrEmpty(Collectors.toList()));
        assertFalse(result.isPresent());
    }

    @Test
    public void testCollectingOrDefaultIfEmpty() {
        List<String> defaultList = Arrays.asList("default");
        List<String> result = Stream.<String> empty().collect(Collectors.collectingOrDefaultIfEmpty(Collectors.toList(), defaultList));
        assertEquals(defaultList, result);
    }

    @Test
    public void testCollectingOrElseGetIfEmpty() {
        List<String> result = Stream.<String> empty().collect(Collectors.collectingOrElseGetIfEmpty(Collectors.toList(), () -> Arrays.asList("default")));
        assertEquals(Arrays.asList("default"), result);
    }

    @Test
    public void testCollectingOrElseThrowIfEmpty() {
        assertThrows(NoSuchElementException.class, () -> Stream.<String> empty().collect(Collectors.collectingOrElseThrowIfEmpty(Collectors.toList())));
    }

    @Test
    public void testCollectingOrElseThrowIfEmpty_WithExceptionSupplier() {
        assertThrows(IllegalStateException.class,
                () -> Stream.<String> empty().collect(Collectors.collectingOrElseThrowIfEmpty(Collectors.toList(), IllegalStateException::new)));
    }

    @Test
    public void testDistinctByToList() {
        List<Person> result = personList.stream().collect(Collectors.distinctByToList(Person::getDepartment));
        assertEquals(3, result.size());
    }

    @Test
    public void testDistinctByToCollection() {
        Set<Person> result = personList.stream().collect(Collectors.distinctByToCollection(Person::getDepartment, HashSet::new));
        assertEquals(3, result.size());
    }

    @Test
    public void testDistinctByToCounting() {
        Integer result = personList.stream().collect(Collectors.distinctByToCounting(Person::getDepartment));
        assertEquals(3, result);
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
    public void testMin_WithComparator() {
        Optional<String> result = stringList.stream().collect(Collectors.min(Comparator.comparingInt(String::length)));
        assertTrue(result.isPresent());
        assertEquals("date", result.get());
    }

    @Test
    public void testMinOrElse() {
        Integer result = integerList.stream().collect(Collectors.minOrElse(0));
        assertEquals(1, result);
    }

    @Test
    public void testMinOrElse_WithComparator() {
        String result = stringList.stream().collect(Collectors.minOrElse(Comparator.comparingInt(String::length), "default"));
        assertEquals("date", result);
    }

    @Test
    public void testMinOrElseGet() {
        Integer result = integerList.stream().collect(Collectors.minOrElseGet(() -> 0));
        assertEquals(1, result);
    }

    @Test
    public void testMinOrElseGet_WithComparator() {
        String result = stringList.stream().collect(Collectors.minOrElseGet(Comparator.comparingInt(String::length), () -> "default"));
        assertEquals("date", result);
    }

    @Test
    public void testMinOrElseThrow() {
        Integer result = integerList.stream().collect(Collectors.minOrElseThrow());
        assertEquals(1, result);
    }

    @Test
    public void testMinOrElseThrow_WithComparator() {
        String result = stringList.stream().collect(Collectors.minOrElseThrow(Comparator.comparingInt(String::length)));
        assertEquals("date", result);
    }

    @Test
    public void testMinOrElseThrow_WithComparatorAndExceptionSupplier() {
        assertThrows(IllegalStateException.class,
                () -> Stream.<String> empty().collect(Collectors.minOrElseThrow(Comparator.naturalOrder(), IllegalStateException::new)));
    }

    @Test
    public void testMinBy() {
        Optional<Person> result = personList.stream().collect(Collectors.minBy(Person::getAge));
        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().getName());
    }

    @Test
    public void testMinByOrElseGet() {
        Person result = personList.stream().collect(Collectors.minByOrElseGet(Person::getAge, () -> new Person("Default", 0, "None", null, null)));
        assertEquals("Alice", result.getName());
    }

    @Test
    public void testMinByOrElseThrow() {
        Person result = personList.stream().collect(Collectors.minByOrElseThrow(Person::getAge));
        assertEquals("Alice", result.getName());
    }

    @Test
    public void testMinByOrElseThrow_WithExceptionSupplier() {
        assertThrows(IllegalStateException.class,
                () -> Stream.<Person> empty().collect(Collectors.minByOrElseThrow(Person::getAge, IllegalStateException::new)));
    }

    @Test
    public void testMax() {
        Optional<Integer> result = integerList.stream().collect(Collectors.max());
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    public void testMax_WithComparator() {
        Optional<String> result = stringList.stream().collect(Collectors.max(Comparator.comparingInt(String::length)));
        assertTrue(result.isPresent());
        assertEquals("elderberry", result.get());
    }

    @Test
    public void testMaxOrElse() {
        Integer result = integerList.stream().collect(Collectors.maxOrElse(0));
        assertEquals(5, result);
    }

    @Test
    public void testMaxOrElse_WithComparator() {
        String result = stringList.stream().collect(Collectors.maxOrElse(Comparator.comparingInt(String::length), "default"));
        assertEquals("elderberry", result);
    }

    @Test
    public void testMaxOrElseGet() {
        Integer result = integerList.stream().collect(Collectors.maxOrElseGet(() -> 0));
        assertEquals(5, result);
    }

    @Test
    public void testMaxOrElseGet_WithComparator() {
        String result = stringList.stream().collect(Collectors.maxOrElseGet(Comparator.comparingInt(String::length), () -> "default"));
        assertEquals("elderberry", result);
    }

    @Test
    public void testMaxOrElseThrow() {
        Integer result = integerList.stream().collect(Collectors.maxOrElseThrow());
        assertEquals(5, result);
    }

    @Test
    public void testMaxOrElseThrow_WithComparator() {
        String result = stringList.stream().collect(Collectors.maxOrElseThrow(Comparator.comparingInt(String::length)));
        assertEquals("elderberry", result);
    }

    @Test
    public void testMaxOrElseThrow_WithComparatorAndExceptionSupplier() {
        assertThrows(IllegalStateException.class,
                () -> Stream.<String> empty().collect(Collectors.maxOrElseThrow(Comparator.naturalOrder(), IllegalStateException::new)));
    }

    @Test
    public void testMaxBy() {
        Optional<Person> result = personList.stream().collect(Collectors.maxBy(Person::getAge));
        assertTrue(result.isPresent());
        assertEquals("Charlie", result.get().getName());
    }

    @Test
    public void testMaxByOrElseGet() {
        Person result = personList.stream().collect(Collectors.maxByOrElseGet(Person::getAge, () -> new Person("Default", 0, "None", null, null)));
        assertEquals("Charlie", result.getName());
    }

    @Test
    public void testMaxByOrElseThrow() {
        Person result = personList.stream().collect(Collectors.maxByOrElseThrow(Person::getAge));
        assertEquals("Charlie", result.getName());
    }

    @Test
    public void testMaxByOrElseThrow_WithExceptionSupplier() {
        assertThrows(IllegalStateException.class,
                () -> Stream.<Person> empty().collect(Collectors.maxByOrElseThrow(Person::getAge, IllegalStateException::new)));
    }

    @Test
    public void testMinAll() {
        List<Integer> result = integerList.stream().collect(Collectors.minAll());
        assertEquals(Arrays.asList(1), result);
    }

    @Test
    public void testMinAll_WithComparator() {
        List<String> result = stringList.stream().collect(Collectors.minAll(Comparator.comparingInt(String::length)));
        assertEquals(Arrays.asList("date"), result);
    }

    @Test
    public void testMinAll_WithComparatorAndLimit() {
        List<String> result = stringList.stream().collect(Collectors.minAll(Comparator.comparingInt(String::length), 2));
        assertTrue(result.size() <= 2);
    }

    @Test
    public void testMinAll_WithDownstream() {
        Long result = integerList.stream().collect(Collectors.minAll(Collectors.counting()));
        assertEquals(1L, result);
    }

    @Test
    public void testMinAll_WithComparatorAndDownstream() {
        Long result = stringList.stream().collect(Collectors.minAll(Comparator.comparingInt(String::length), Collectors.counting()));
        assertEquals(1L, result);
    }

    @Test
    public void testMinAlll_WithDownstream() {
        Optional<Pair<Integer, Long>> result = integerList.stream().collect(Collectors.minAllWith(Collectors.counting()));
        assertTrue(result.isPresent());
        assertEquals(1, result.get().left());
        assertEquals(1L, result.get().right());
    }

    @Test
    public void testMinAlll_WithComparatorAndDownstream() {
        Optional<Pair<String, Long>> result = stringList.stream()
                .collect(Collectors.minAllWith(Comparator.comparingInt(String::length), Collectors.counting()));
        assertTrue(result.isPresent());
        assertEquals("date", result.get().left());
        assertEquals(1L, result.get().right());
    }

    @Test
    public void testMaxAll() {
        List<Integer> result = integerList.stream().collect(Collectors.maxAll());
        assertEquals(Arrays.asList(5), result);
    }

    @Test
    public void testMaxAll_WithComparator() {
        List<String> result = stringList.stream().collect(Collectors.maxAll(Comparator.comparingInt(String::length)));
        assertEquals(Arrays.asList("elderberry"), result);
    }

    @Test
    public void testMaxAll_WithComparatorAndLimit() {
        List<String> result = stringList.stream().collect(Collectors.maxAll(Comparator.comparingInt(String::length), 2));
        assertTrue(result.size() <= 2);
    }

    @Test
    public void testMaxAll_WithDownstream() {
        Long result = integerList.stream().collect(Collectors.maxAll(Collectors.counting()));
        assertEquals(1L, result);
    }

    @Test
    public void testMaxAll_WithComparatorAndDownstream() {
        Long result = stringList.stream().collect(Collectors.maxAll(Comparator.comparingInt(String::length), Collectors.counting()));
        assertEquals(1L, result);
    }

    @Test
    public void testMaxAlll_WithDownstream() {
        Optional<Pair<Integer, Long>> result = integerList.stream().collect(Collectors.maxAllWith(Collectors.counting()));
        assertTrue(result.isPresent());
        assertEquals(5, result.get().left());
        assertEquals(1L, result.get().right());
    }

    @Test
    public void testMaxAlll_WithComparatorAndDownstream() {
        Optional<Pair<String, Long>> result = stringList.stream()
                .collect(Collectors.maxAllWith(Comparator.comparingInt(String::length), Collectors.counting()));
        assertTrue(result.isPresent());
        assertEquals("elderberry", result.get().left());
        assertEquals(1L, result.get().right());
    }

    @Test
    public void testMinMax() {
        Optional<Pair<Integer, Integer>> result = integerList.stream().collect(Collectors.minMax());
        assertTrue(result.isPresent());
        assertEquals(1, result.get().left());
        assertEquals(5, result.get().right());
    }

    @Test
    public void testMinMax_WithComparator() {
        Optional<Pair<String, String>> result = stringList.stream().collect(Collectors.minMax(Comparator.comparingInt(String::length)));
        assertTrue(result.isPresent());
        assertEquals("date", result.get().left());
        assertEquals("elderberry", result.get().right());
    }

    @Test
    public void testMinMaxBy() {
        Optional<Pair<Person, Person>> result = personList.stream().collect(Collectors.minMaxBy(Person::getAge));
        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().left().getName());
        assertEquals("Charlie", result.get().right().getName());
    }

    @Test
    public void testMinMaxOrElseGet() {
        Pair<Integer, Integer> result = integerList.stream().collect(Collectors.minMaxOrElseGet(() -> Pair.of(-1, -1)));
        assertEquals(1, result.left());
        assertEquals(5, result.right());
    }

    @Test
    public void testMinMaxOrElseGet_WithComparator() {
        Pair<String, String> result = stringList.stream()
                .collect(Collectors.minMaxOrElseGet(Comparator.comparingInt(String::length), () -> Pair.of("default", "default")));
        assertEquals("date", result.left());
        assertEquals("elderberry", result.right());
    }

    @Test
    public void testMinMaxOrElseThrow() {
        Pair<Integer, Integer> result = integerList.stream().collect(Collectors.minMaxOrElseThrow());
        assertEquals(1, result.left());
        assertEquals(5, result.right());
    }

    @Test
    public void testMinMaxOrElseThrow_WithComparator() {
        Pair<String, String> result = stringList.stream().collect(Collectors.minMaxOrElseThrow(Comparator.comparingInt(String::length)));
        assertEquals("date", result.left());
        assertEquals("elderberry", result.right());
    }

    @Test
    public void testMinMaxOrElseThrow_Empty() {
        assertThrows(NoSuchElementException.class, () -> Stream.<Integer> empty().collect(Collectors.minMaxOrElseThrow()));
    }

    @Test
    public void testSummingInt() {
        Integer result = personList.stream().collect(Collectors.summingInt(Person::getAge));
        assertEquals(150, result);
    }

    @Test
    public void testSummingIntToLong() {
        Long result = personList.stream().collect(Collectors.summingIntToLong(Person::getAge));
        assertEquals(150L, result);
    }

    @Test
    public void testSummingLong() {
        Long result = longList.stream().collect(Collectors.summingLong(Long::longValue));
        assertEquals(1500L, result);
    }

    @Test
    public void testSummingDouble() {
        Double result = doubleList.stream().collect(Collectors.summingDouble(Double::doubleValue));
        assertEquals(16.5, result, 0.01);
    }

    @Test
    public void testSummingBigInteger() {
        BigInteger result = personList.stream().collect(Collectors.summingBigInteger(Person::getBigIntValue));
        assertEquals(BigInteger.valueOf(1500), result);
    }

    @Test
    public void testSummingBigDecimal() {
        BigDecimal result = personList.stream().collect(Collectors.summingBigDecimal(Person::getBigDecValue));
        assertEquals(BigDecimal.valueOf(1502.5), result);
    }

    @Test
    public void testAveragingInt() {
        Double result = personList.stream().collect(Collectors.averagingInt(Person::getAge));
        assertEquals(30.0, result, 0.01);
    }

    @Test
    public void testAveragingIntOrEmpty() {
        OptionalDouble result = personList.stream().collect(Collectors.averagingIntOrEmpty(Person::getAge));
        assertTrue(result.isPresent());
        assertEquals(30.0, result.getAsDouble(), 0.01);
    }

    @Test
    public void testAveragingIntOrEmpty_Empty() {
        OptionalDouble result = Stream.<Person> empty().collect(Collectors.averagingIntOrEmpty(Person::getAge));
        assertFalse(result.isPresent());
    }

    @Test
    public void testAveragingIntOrElseThrow() {
        Double result = personList.stream().collect(Collectors.averagingIntOrElseThrow(Person::getAge));
        assertEquals(30.0, result, 0.01);
    }

    @Test
    public void testAveragingLong() {
        Double result = longList.stream().collect(Collectors.averagingLong(Long::longValue));
        assertEquals(300.0, result, 0.01);
    }

    @Test
    public void testAveragingLongOrEmpty() {
        OptionalDouble result = longList.stream().collect(Collectors.averagingLongOrEmpty(Long::longValue));
        assertTrue(result.isPresent());
        assertEquals(300.0, result.getAsDouble(), 0.01);
    }

    @Test
    public void testAveragingLongOrElseThrow() {
        Double result = longList.stream().collect(Collectors.averagingLongOrElseThrow(Long::longValue));
        assertEquals(300.0, result, 0.01);
    }

    @Test
    public void testAveragingDouble() {
        Double result = doubleList.stream().collect(Collectors.averagingDouble(Double::doubleValue));
        assertEquals(3.3, result, 0.01);
    }

    @Test
    public void testAveragingDoubleOrEmpty() {
        OptionalDouble result = doubleList.stream().collect(Collectors.averagingDoubleOrEmpty(Double::doubleValue));
        assertTrue(result.isPresent());
        assertEquals(3.3, result.getAsDouble(), 0.01);
    }

    @Test
    public void testAveragingDoubleOrElseThrow() {
        Double result = doubleList.stream().collect(Collectors.averagingDoubleOrElseThrow(Double::doubleValue));
        assertEquals(3.3, result, 0.01);
    }

    @Test
    public void testAveragingBigInteger() {
        BigDecimal result = personList.stream().collect(Collectors.averagingBigInteger(Person::getBigIntValue));
        assertEquals(BigDecimal.valueOf(300), result);
    }

    @Test
    public void testAveragingBigIntegerOrEmpty() {
        Optional<BigDecimal> result = personList.stream().collect(Collectors.averagingBigIntegerOrEmpty(Person::getBigIntValue));
        assertTrue(result.isPresent());
        assertEquals(BigDecimal.valueOf(300), result.get());
    }

    @Test
    public void testAveragingBigIntegerOrElseThrow() {
        BigDecimal result = personList.stream().collect(Collectors.averagingBigIntegerOrElseThrow(Person::getBigIntValue));
        assertEquals(BigDecimal.valueOf(300), result);
    }

    @Test
    public void testAveragingBigDecimal() {
        BigDecimal result = personList.stream().collect(Collectors.averagingBigDecimal(Person::getBigDecValue));
        assertTrue(result.compareTo(BigDecimal.valueOf(300.5)) == 0);
    }

    @Test
    public void testAveragingBigDecimalOrEmpty() {
        Optional<BigDecimal> result = personList.stream().collect(Collectors.averagingBigDecimalOrEmpty(Person::getBigDecValue));
        assertTrue(result.isPresent());
        assertTrue(result.get().compareTo(BigDecimal.valueOf(300.5)) == 0);
    }

    @Test
    public void testAveragingBigDecimalOrElseThrow() {
        BigDecimal result = personList.stream().collect(Collectors.averagingBigDecimalOrElseThrow(Person::getBigDecValue));
        assertTrue(result.compareTo(BigDecimal.valueOf(300.5)) == 0);
    }

    @Test
    public void testSummarizingChar() {
        CharSummaryStatistics result = charList.stream().collect(Collectors.summarizingChar(c -> c));
        assertEquals(5, result.getCount());
        assertEquals('a', result.getMin());
        assertEquals('e', result.getMax());
    }

    @Test
    public void testSummarizingByte() {
        ByteSummaryStatistics result = byteList.stream().collect(Collectors.summarizingByte(b -> b));
        assertEquals(5, result.getCount());
        assertEquals((byte) 1, result.getMin());
        assertEquals((byte) 5, result.getMax());
    }

    @Test
    public void testSummarizingShort() {
        ShortSummaryStatistics result = shortList.stream().collect(Collectors.summarizingShort(s -> s));
        assertEquals(5, result.getCount());
        assertEquals((short) 10, result.getMin());
        assertEquals((short) 50, result.getMax());
    }

    @Test
    public void testSummarizingInt() {
        java.util.IntSummaryStatistics result = integerList.stream().collect(Collectors.summarizingInt(i -> i));
        assertEquals(5, result.getCount());
        assertEquals(1, result.getMin());
        assertEquals(5, result.getMax());
        assertEquals(15, result.getSum());
    }

    @Test
    public void testSummarizingLong() {
        LongSummaryStatistics result = longList.stream().collect(Collectors.summarizingLong(l -> l));
        assertEquals(5, result.getCount());
        assertEquals(100L, result.getMin());
        assertEquals(500L, result.getMax());
    }

    @Test
    public void testSummarizingFloat() {
        FloatSummaryStatistics result = floatList.stream().collect(Collectors.summarizingFloat(f -> f));
        assertEquals(5, result.getCount());
        assertEquals(1.1f, result.getMin(), 0.01);
        assertEquals(5.5f, result.getMax(), 0.01);
    }

    @Test
    public void testSummarizingDouble() {
        DoubleSummaryStatistics result = doubleList.stream().collect(Collectors.summarizingDouble(d -> d));
        assertEquals(5, result.getCount());
        assertEquals(1.1, result.getMin(), 0.01);
        assertEquals(5.5, result.getMax(), 0.01);
    }

    @Test
    public void testSummarizingBigInteger() {
        BigIntegerSummaryStatistics result = personList.stream().collect(Collectors.summarizingBigInteger(Person::getBigIntValue));
        assertEquals(5, result.getCount());
        assertEquals(BigInteger.valueOf(100), result.getMin());
        assertEquals(BigInteger.valueOf(500), result.getMax());
    }

    @Test
    public void testSummarizingBigDecimal() {
        BigDecimalSummaryStatistics result = personList.stream().collect(Collectors.summarizingBigDecimal(Person::getBigDecValue));
        assertEquals(5, result.getCount());
        assertEquals(BigDecimal.valueOf(100.5), result.getMin());
        assertEquals(BigDecimal.valueOf(500.5), result.getMax());
    }

    @Test
    public void testReducing_WithIdentity() {
        Integer result = integerList.stream().collect(Collectors.reducing(0, Integer::sum));
        assertEquals(15, result);
    }

    @Test
    public void testReducing_WithoutIdentity() {
        Optional<Integer> result = integerList.stream().collect(Collectors.reducing(Integer::sum));
        assertTrue(result.isPresent());
        assertEquals(15, result.get());
    }

    @Test
    public void testReducingOrElseGet() {
        Integer result = integerList.stream().collect(Collectors.reducingOrElseGet(Integer::sum, () -> 0));
        assertEquals(15, result);
    }

    @Test
    public void testReducingOrElseThrow_NoArgs() {
        Integer result = integerList.stream().collect(Collectors.reducingOrElseThrow(Integer::sum));
        assertEquals(15, result);
    }

    @Test
    public void testReducingOrElseThrow_WithExceptionSupplier() {
        assertThrows(IllegalStateException.class,
                () -> Stream.<Integer> empty().collect(Collectors.reducingOrElseThrow(Integer::sum, IllegalStateException::new)));
    }

    @Test
    public void testReducing_WithMapper() {
        Integer result = stringList.stream().collect(Collectors.reducing(0, String::length, Integer::sum));
        assertEquals(31, result);
    }

    @Test
    public void testReducingOrElseGet_WithMapper() {
        Integer result = stringList.stream().collect(Collectors.reducingOrElseGet(String::length, Integer::sum, () -> 0));
        assertEquals(31, result);
    }

    @Test
    public void testReducingOrElseThrow_WithMapper() {
        Integer result = stringList.stream().collect(Collectors.reducingOrElseThrow(String::length, Integer::sum, IllegalStateException::new));
        assertEquals(31, result);
    }

    @Test
    public void testReducingOrElseThrow_WithMapperNoExceptionSupplier() {
        Integer result = stringList.stream().collect(Collectors.reducingOrElseThrow(String::length, Integer::sum));
        assertEquals(31, result);
    }

    @Test
    public void testCommonPrefix() {
        List<String> words = Arrays.asList("apple", "application", "apply");
        String result = words.stream().collect(Collectors.commonPrefix());
        assertEquals("appl", result);
    }

    @Test
    public void testCommonPrefix_NoCommon() {
        String result = stringList.stream().collect(Collectors.commonPrefix());
        assertEquals("", result);
    }

    @Test
    public void testCommonSuffix() {
        List<String> words = Arrays.asList("running", "jumping", "walking");
        String result = words.stream().collect(Collectors.commonSuffix());
        assertEquals("ing", result);
    }

    @Test
    public void testCommonSuffix_NoCommon() {
        String result = stringList.stream().collect(Collectors.commonSuffix());
        assertEquals("", result);
    }

    @Test
    public void testGroupingBy() {
        Map<String, List<Person>> result = personList.stream().collect(Collectors.groupingBy(Person::getDepartment));
        assertEquals(3, result.size());
        assertEquals(2, result.get("Engineering").size());
        assertEquals(2, result.get("Sales").size());
        assertEquals(1, result.get("HR").size());
    }

    @Test
    public void testGroupingBy_WithMapFactory() {
        LinkedHashMap<String, List<Person>> result = personList.stream().collect(Collectors.groupingBy(Person::getDepartment, LinkedHashMap::new));
        assertEquals(3, result.size());
        assertTrue(result instanceof LinkedHashMap);
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
    public void testGroupingByConcurrent() {
        ConcurrentMap<String, List<Person>> result = personList.stream().collect(Collectors.groupingByConcurrent(Person::getDepartment));
        assertEquals(3, result.size());
        assertTrue(result instanceof ConcurrentMap);
    }

    @Test
    public void testGroupingByConcurrent_WithMapFactory() {
        ConcurrentHashMap<String, List<Person>> result = personList.stream()
                .collect(Collectors.groupingByConcurrent(Person::getDepartment, ConcurrentHashMap::new));
        assertEquals(3, result.size());
        assertTrue(result instanceof ConcurrentHashMap);
    }

    @Test
    public void testGroupingByConcurrent_WithDownstream() {
        ConcurrentMap<String, Long> result = personList.stream().collect(Collectors.groupingByConcurrent(Person::getDepartment, Collectors.counting()));
        assertEquals(3, result.size());
    }

    @Test
    public void testPartitioningBy() {
        Map<Boolean, List<Integer>> result = integerList.stream().collect(Collectors.partitioningBy(i -> i % 2 == 0));
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(2, 4), result.get(true));
        assertEquals(Arrays.asList(1, 3, 5), result.get(false));
    }

    @Test
    public void testPartitioningBy_WithDownstream() {
        Map<Boolean, Long> result = integerList.stream().collect(Collectors.partitioningBy(i -> i % 2 == 0, Collectors.counting()));
        assertEquals(2L, result.get(true));
        assertEquals(3L, result.get(false));
    }

    @Test
    public void testCountingBy() {
        Map<String, Long> result = personList.stream().collect(Collectors.countingBy(Person::getDepartment));
        assertEquals(3, result.size());
        assertEquals(2L, result.get("Engineering"));
        assertEquals(2L, result.get("Sales"));
        assertEquals(1L, result.get("HR"));
    }

    @Test
    public void testCountingBy_WithMapFactory() {
        TreeMap<String, Long> result = personList.stream().collect(Collectors.countingBy(Person::getDepartment, TreeMap::new));
        assertEquals(3, result.size());
        assertTrue(result instanceof TreeMap);
    }

    @Test
    public void testCountingToIntBy() {
        Map<String, Integer> result = personList.stream().collect(Collectors.countingToIntBy(Person::getDepartment));
        assertEquals(3, result.size());
        assertEquals(2, result.get("Engineering"));
        assertEquals(2, result.get("Sales"));
        assertEquals(1, result.get("HR"));
    }

    @Test
    public void testCountingToIntBy_WithMapFactory() {
        LinkedHashMap<String, Integer> result = personList.stream().collect(Collectors.countingToIntBy(Person::getDepartment, LinkedHashMap::new));
        assertEquals(3, result.size());
        assertTrue(result instanceof LinkedHashMap);
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
    public void testToMap_WithAllParameters() {
        TreeMap<Integer, String> result = stringList.stream()
                .collect(Collectors.toMap(String::length, Function.identity(), (a, b) -> a + "," + b, TreeMap::new));
        assertTrue(result instanceof TreeMap);
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
    public void testToConcurrentMap() {
        ConcurrentMap<String, Integer> result = stringList.stream().collect(Collectors.toConcurrentMap(Function.identity(), String::length));
        assertEquals(5, result.size());
        assertTrue(result instanceof ConcurrentMap);
    }

    @Test
    public void testToConcurrentMap_WithMergeFunction() {
        ConcurrentMap<Integer, String> result = stringList.stream().collect(Collectors.toConcurrentMap(String::length, Function.identity(), (a, b) -> a));
        assertTrue(result instanceof ConcurrentMap);
    }

    @Test
    public void testToConcurrentMap_WithAllParameters() {
        ConcurrentHashMap<Integer, String> result = stringList.stream()
                .collect(Collectors.toConcurrentMap(String::length, Function.identity(), (a, b) -> a, ConcurrentHashMap::new));
        assertTrue(result instanceof ConcurrentHashMap);
    }

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
    public void testToBiMap_WithMergeFunction() {
        BiMap<Integer, String> result = stringList.stream().collect(Collectors.toBiMap(String::length, Function.identity(), (a, b) -> a));
        assertNotNull(result.get(6));
    }

    @Test
    public void testToBiMap_WithAllParameters() {
        BiMap<Integer, String> result = stringList.stream().collect(Collectors.toBiMap(String::length, Function.identity(), (a, b) -> a, BiMap::new));
        assertNotNull(result);
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
    public void testFlatmappingValueToMultimap() {
        List<Pair<String, List<Integer>>> data = Arrays.asList(Pair.of("a", Arrays.asList(1, 2, 3)));
        ListMultimap<String, Integer> result = data.stream().collect(Collectors.flatmappingValueToMultimap(Pair::left, Pair::right));
        assertEquals(3, result.get("a").size());
    }

    @Test
    public void testFlatmappingKeyToMultimap() {
        List<Pair<List<String>, Integer>> data = Arrays.asList(Pair.of(Arrays.asList("a", "b"), 1));
        ListMultimap<String, Integer> result = data.stream().collect(Collectors.flatmappingKeyToMultimap(Pair::left, Pair::right));
        assertEquals(1, result.get("a").get(0));
    }

    @Test
    public void testTeeing() {
        Pair<Long, Integer> result = stringList.stream().collect(Collectors.teeing(Collectors.counting(), Collectors.summingInt(String::length), Pair::of));
        assertEquals(5L, result.left());
        assertEquals(31, result.right());
    }

    @Test
    public void testMoreCollectors_SummingInt_2Mappers() {
        Tuple2<Integer, Integer> result = personList.stream().collect(MoreCollectors.summingInt(Person::getAge, p -> p.getAge() * 2));
        assertEquals(150, result._1);
        assertEquals(300, result._2);
    }

    @Test
    public void testMoreCollectors_SummingInt_3Mappers() {
        Tuple3<Integer, Integer, Integer> result = personList.stream()
                .collect(MoreCollectors.summingInt(Person::getAge, p -> p.getAge() * 2, p -> p.getAge() * 3));
        assertEquals(150, result._1);
        assertEquals(300, result._2);
        assertEquals(450, result._3);
    }

    @Test
    public void testMoreCollectors_SummingIntToLong_2Mappers() {
        Tuple2<Long, Long> result = personList.stream().collect(MoreCollectors.summingIntToLong(Person::getAge, p -> p.getAge() * 2));
        assertEquals(150L, result._1);
        assertEquals(300L, result._2);
    }

    @Test
    public void testMoreCollectors_SummingIntToLong_3Mappers() {
        Tuple3<Long, Long, Long> result = personList.stream()
                .collect(MoreCollectors.summingIntToLong(Person::getAge, p -> p.getAge() * 2, p -> p.getAge() * 3));
        assertEquals(150L, result._1);
        assertEquals(300L, result._2);
        assertEquals(450L, result._3);
    }

    @Test
    public void testMoreCollectors_SummingLong_2Mappers() {
        Tuple2<Long, Long> result = longList.stream().collect(MoreCollectors.summingLong(Long::longValue, l -> l * 2));
        assertEquals(1500L, result._1);
        assertEquals(3000L, result._2);
    }

    @Test
    public void testMoreCollectors_SummingLong_3Mappers() {
        Tuple3<Long, Long, Long> result = longList.stream().collect(MoreCollectors.summingLong(Long::longValue, l -> l * 2, l -> l * 3));
        assertEquals(1500L, result._1);
        assertEquals(3000L, result._2);
        assertEquals(4500L, result._3);
    }

    @Test
    public void testMoreCollectors_SummingDouble_2Mappers() {
        Tuple2<Double, Double> result = doubleList.stream().collect(MoreCollectors.summingDouble(Double::doubleValue, d -> d * 2));
        assertEquals(16.5, result._1, 0.01);
        assertEquals(33.0, result._2, 0.01);
    }

    @Test
    public void testMoreCollectors_SummingDouble_3Mappers() {
        Tuple3<Double, Double, Double> result = doubleList.stream().collect(MoreCollectors.summingDouble(Double::doubleValue, d -> d * 2, d -> d * 3));
        assertEquals(16.5, result._1, 0.01);
        assertEquals(33.0, result._2, 0.01);
        assertEquals(49.5, result._3, 0.01);
    }

    @Test
    public void testMoreCollectors_SummingBigInteger_2Mappers() {
        Tuple2<BigInteger, BigInteger> result = personList.stream()
                .collect(MoreCollectors.summingBigInteger(Person::getBigIntValue, p -> p.getBigIntValue().multiply(BigInteger.TWO)));
        assertEquals(BigInteger.valueOf(1500), result._1);
        assertEquals(BigInteger.valueOf(3000), result._2);
    }

    @Test
    public void testMoreCollectors_SummingBigInteger_3Mappers() {
        Tuple3<BigInteger, BigInteger, BigInteger> result = personList.stream()
                .collect(MoreCollectors.summingBigInteger(Person::getBigIntValue, p -> p.getBigIntValue().multiply(BigInteger.TWO),
                        p -> p.getBigIntValue().multiply(BigInteger.valueOf(3))));
        assertEquals(BigInteger.valueOf(1500), result._1);
        assertEquals(BigInteger.valueOf(3000), result._2);
        assertEquals(BigInteger.valueOf(4500), result._3);
    }

    @Test
    public void testMoreCollectors_SummingBigDecimal_2Mappers() {
        Tuple2<BigDecimal, BigDecimal> result = personList.stream()
                .collect(MoreCollectors.summingBigDecimal(Person::getBigDecValue, p -> p.getBigDecValue().multiply(BigDecimal.valueOf(2))));
        assertEquals(BigDecimal.valueOf(1502.5), result._1);
        assertEquals(BigDecimal.valueOf(3005.0), result._2);
    }

    @Test
    public void testMoreCollectors_SummingBigDecimal_3Mappers() {
        Tuple3<BigDecimal, BigDecimal, BigDecimal> result = personList.stream()
                .collect(MoreCollectors.summingBigDecimal(Person::getBigDecValue, p -> p.getBigDecValue().multiply(BigDecimal.valueOf(2)),
                        p -> p.getBigDecValue().multiply(BigDecimal.valueOf(3))));
        assertEquals(BigDecimal.valueOf(1502.5), result._1);
        assertEquals(BigDecimal.valueOf(3005.0), result._2);
        assertTrue(result._3.compareTo(BigDecimal.valueOf(4507.5)) == 0);
    }

    @Test
    public void testMoreCollectors_AveragingInt_2Mappers() {
        Tuple2<Double, Double> result = personList.stream().collect(MoreCollectors.averagingInt(Person::getAge, p -> p.getAge() * 2));
        assertEquals(30.0, result._1, 0.01);
        assertEquals(60.0, result._2, 0.01);
    }

    @Test
    public void testMoreCollectors_AveragingInt_3Mappers() {
        Tuple3<Double, Double, Double> result = personList.stream()
                .collect(MoreCollectors.averagingInt(Person::getAge, p -> p.getAge() * 2, p -> p.getAge() * 3));
        assertEquals(30.0, result._1, 0.01);
        assertEquals(60.0, result._2, 0.01);
        assertEquals(90.0, result._3, 0.01);
    }

    @Test
    public void testMoreCollectors_AveragingLong_2Mappers() {
        Tuple2<Double, Double> result = longList.stream().collect(MoreCollectors.averagingLong(Long::longValue, l -> l * 2));
        assertEquals(300.0, result._1, 0.01);
        assertEquals(600.0, result._2, 0.01);
    }

    @Test
    public void testMoreCollectors_AveragingLong_3Mappers() {
        Tuple3<Double, Double, Double> result = longList.stream().collect(MoreCollectors.averagingLong(Long::longValue, l -> l * 2, l -> l * 3));
        assertEquals(300.0, result._1, 0.01);
        assertEquals(600.0, result._2, 0.01);
        assertEquals(900.0, result._3, 0.01);
    }

    @Test
    public void testMoreCollectors_AveragingDouble_2Mappers() {
        Tuple2<Double, Double> result = doubleList.stream().collect(MoreCollectors.averagingDouble(Double::doubleValue, d -> d * 2));
        assertEquals(3.3, result._1, 0.01);
        assertEquals(6.6, result._2, 0.01);
    }

    @Test
    public void testMoreCollectors_AveragingDouble_3Mappers() {
        Tuple3<Double, Double, Double> result = doubleList.stream().collect(MoreCollectors.averagingDouble(Double::doubleValue, d -> d * 2, d -> d * 3));
        assertEquals(3.3, result._1, 0.01);
        assertEquals(6.6, result._2, 0.01);
        assertEquals(9.9, result._3, 0.01);
    }

    @Test
    public void testMoreCollectors_AveragingBigInteger_2Mappers() {
        Tuple2<BigDecimal, BigDecimal> result = personList.stream()
                .collect(MoreCollectors.averagingBigInteger(Person::getBigIntValue, p -> p.getBigIntValue().multiply(BigInteger.TWO)));
        assertEquals(BigDecimal.valueOf(300), result._1);
        assertEquals(BigDecimal.valueOf(600), result._2);
    }

    @Test
    public void testMoreCollectors_AveragingBigInteger_3Mappers() {
        Tuple3<BigDecimal, BigDecimal, BigDecimal> result = personList.stream()
                .collect(MoreCollectors.averagingBigInteger(Person::getBigIntValue, p -> p.getBigIntValue().multiply(BigInteger.TWO),
                        p -> p.getBigIntValue().multiply(BigInteger.valueOf(3))));
        assertEquals(BigDecimal.valueOf(300), result._1);
        assertEquals(BigDecimal.valueOf(600), result._2);
        assertEquals(BigDecimal.valueOf(900), result._3);
    }

    @Test
    public void testMoreCollectors_AveragingBigDecimal_2Mappers() {
        Tuple2<BigDecimal, BigDecimal> result = personList.stream()
                .collect(MoreCollectors.averagingBigDecimal(Person::getBigDecValue, p -> p.getBigDecValue().multiply(BigDecimal.valueOf(2))));
        assertTrue(result._1.compareTo(BigDecimal.valueOf(300.5)) == 0);
        assertTrue(result._2.compareTo(BigDecimal.valueOf(601.0)) == 0);
    }

    @Test
    public void testMoreCollectors_AveragingBigDecimal_3Mappers() {
        Tuple3<BigDecimal, BigDecimal, BigDecimal> result = personList.stream()
                .collect(MoreCollectors.averagingBigDecimal(Person::getBigDecValue, p -> p.getBigDecValue().multiply(BigDecimal.valueOf(2)),
                        p -> p.getBigDecValue().multiply(BigDecimal.valueOf(3))));
        assertTrue(result._1.compareTo(BigDecimal.valueOf(300.5)) == 0);
        assertTrue(result._2.compareTo(BigDecimal.valueOf(601.0)) == 0);
        assertTrue(result._3.compareTo(BigDecimal.valueOf(901.5)) == 0);
    }

    @Test
    public void testMoreCollectors_Combine_2Collectors() {
        Tuple2<Long, Integer> result = stringList.stream().collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(String::length)));
        assertEquals(5L, result._1);
        assertEquals(31, result._2);
    }

    @Test
    public void testMoreCollectors_Combine_3Collectors() {
        Tuple3<Long, Integer, Optional<String>> result = stringList.stream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(String::length), Collectors.first()));
        assertEquals(5L, result._1);
        assertEquals(31, result._2);
        assertEquals("apple", result._3.get());
    }

    @Test
    public void testMoreCollectors_Combine_4Collectors() {
        Tuple4<Long, Integer, Optional<String>, Optional<String>> result = stringList.stream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(String::length), Collectors.first(), Collectors.last()));
        assertEquals(5L, result._1);
        assertEquals(31, result._2);
        assertEquals("apple", result._3.get());
        assertEquals("elderberry", result._4.get());
    }

    @Test
    public void testMoreCollectors_Combine_5Collectors() {
        Tuple5<Long, Integer, Optional<String>, Optional<String>, Integer> result = stringList.stream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(String::length), Collectors.first(), Collectors.last(),
                        Collectors.summingInt(s -> 1)));
        assertEquals(5L, result._1);
        assertEquals(31, result._2);
        assertEquals("apple", result._3.get());
        assertEquals("elderberry", result._4.get());
        assertEquals(5, result._5);
    }

    @Test
    public void testMoreCollectors_Combine_6Collectors() {
        Tuple6<Long, Integer, Optional<String>, Optional<String>, Integer, Long> result = stringList.stream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(String::length), Collectors.first(), Collectors.last(),
                        Collectors.summingInt(s -> 1), Collectors.counting()));
        assertEquals(5L, result._1);
        assertEquals(31, result._2);
        assertEquals("apple", result._3.get());
        assertEquals("elderberry", result._4.get());
        assertEquals(5, result._5);
        assertEquals(5L, result._6);
    }

    @Test
    public void testMoreCollectors_Combine_7Collectors() {
        Tuple7<Long, Integer, Optional<String>, Optional<String>, Integer, Long, Integer> result = stringList.stream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(String::length), Collectors.first(), Collectors.last(),
                        Collectors.summingInt(s -> 1), Collectors.counting(), Collectors.countingToInt()));
        assertEquals(5L, result._1);
        assertEquals(31, result._2);
        assertEquals("apple", result._3.get());
        assertEquals("elderberry", result._4.get());
        assertEquals(5, result._5);
        assertEquals(5L, result._6);
        assertEquals(5, result._7);
    }

    @Test
    public void testMoreCollectors_Combine_2CollectorsWithFinisher() {
        String result = stringList.stream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(String::length), (count, sum) -> count + ":" + sum));
        assertEquals("5:31", result);
    }

    @Test
    public void testMoreCollectors_Combine_3CollectorsWithFinisher() {
        String result = stringList.stream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(String::length), Collectors.first(),
                        (count, sum, first) -> count + ":" + sum + ":" + first.orElse("none")));
        assertEquals("5:31:apple", result);
    }

    @Test
    public void testMoreCollectors_Combine_4CollectorsWithFinisher() {
        String result = stringList.stream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(String::length), Collectors.first(), Collectors.last(),
                        (count, sum, first, last) -> count + ":" + sum + ":" + first.orElse("none") + ":" + last.orElse("none")));
        assertEquals("5:31:apple:elderberry", result);
    }

    @Test
    public void testMoreCollectors_Combine_CollectionWithFinisher() {
        List<Collector<? super String, ?, ?>> collectors = Arrays.asList(Collectors.counting(), Collectors.summingInt(String::length), Collectors.first());
        Collector<String, ?, String> combine = MoreCollectors.combine(collectors, a -> a[0] + ":" + a[1] + ":" + a[2]);
        String result = stringList.stream().collect(combine);
        assertEquals("5:31:Optional[apple]", result);
    }

    @Test
    public void testMoreCollectors_ToDataset() {
        Dataset result = personList.stream().collect(MoreCollectors.toDataset());
        assertNotNull(result);
        assertEquals(5, result.size());
    }

    @Test
    public void testMoreCollectors_ToDataset_WithColumnNames() {
        List<String> columnNames = Arrays.asList("name", "age", "department", "bigIntValue", "bigDecValue");
        Dataset result = personList.stream().collect(MoreCollectors.toDataset(columnNames));
        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals(columnNames, result.columnNames());
    }

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
    public void test_toCollection_withSizeLimit() {
        List<Integer> list = Stream.of(1, 2, 3, 4, 5).collect(Collectors.toCollection(3, ArrayList::new));
        assertEquals(3, list.size());
        assertEquals(Arrays.asList(1, 2, 3), list);

        List<Integer> parallelList = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).parallel().collect(Collectors.toCollection(5, ArrayList::new));
        assertEquals(5, parallelList.size());
    }

    @Test
    public void test_toList() {
        List<Integer> list = Stream.of(1, 2, 3).collect(Collectors.toList());
        assertEquals(Arrays.asList(1, 2, 3), list);
        assertTrue(list instanceof ArrayList);
    }

    @Test
    public void test_toLinkedList() {
        LinkedList<Integer> list = Stream.of(1, 2, 3).collect(Collectors.toLinkedList());
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void test_toImmutableList() {
        List<Integer> list = Stream.of(1, 2, 3).collect(Collectors.toImmutableList());
        assertEquals(Arrays.asList(1, 2, 3), list);
        assertThrows(UnsupportedOperationException.class, () -> list.add(4));
    }

    @Test
    public void test_toUnmodifiableList() {
        List<Integer> list = Stream.of(1, 2, 3).collect(Collectors.toUnmodifiableList());
        assertEquals(Arrays.asList(1, 2, 3), list);
        assertThrows(UnsupportedOperationException.class, () -> list.add(4));
    }

    @Test
    public void test_toSet() {
        Set<Integer> set = Stream.of(1, 2, 3, 2, 1).collect(Collectors.toSet());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
    }

    @Test
    public void test_toLinkedHashSet() {
        Set<Integer> set = Stream.of(3, 1, 2, 1).collect(Collectors.toLinkedHashSet());
        assertEquals(LinkedHashSet.class, set.getClass());
        assertEquals(Arrays.asList(3, 1, 2), new ArrayList<>(set));
    }

    @Test
    public void test_toImmutableSet() {
        Set<Integer> set = Stream.of(1, 2, 3, 2).collect(Collectors.toImmutableSet());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
        assertThrows(UnsupportedOperationException.class, () -> set.add(4));
    }

    @Test
    public void test_toUnmodifiableSet() {
        Set<Integer> set = Stream.of(1, 2, 3, 2).collect(Collectors.toUnmodifiableSet());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
        assertThrows(UnsupportedOperationException.class, () -> set.add(4));
    }

    @Test
    public void test_toArray() {
        Object[] array = Stream.of(1, "b", 3.0).collect(Collectors.toArray());
        assertArrayEquals(new Object[] { 1, "b", 3.0 }, array);
    }

    @Test
    public void test_toArray_withGenerator() {
        Integer[] array = Stream.of(1, 2, 3).collect(Collectors.toArray(Integer[]::new));
        assertArrayEquals(new Integer[] { 1, 2, 3 }, array);
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
    public void test_onlyOne() {
        Optional<Integer> result = Stream.of(1).collect(Collectors.onlyOne());
        assertTrue(result.isPresent());
        assertEquals(1, result.get());

        Optional<Integer> emptyResult = Stream.<Integer> empty().collect(Collectors.onlyOne());
        assertFalse(emptyResult.isPresent());

        assertThrows(TooManyElementsException.class, () -> Stream.of(1, 2).collect(Collectors.onlyOne()));
    }

    @Test
    public void test_onlyOne_withPredicate() {
        Optional<Integer> result = Stream.of(1, 2, 3, 4).collect(Collectors.onlyOne(i -> i == 3));
        assertTrue(result.isPresent());
        assertEquals(3, result.get());

        Optional<Integer> emptyResult = Stream.of(1, 2, 4).collect(Collectors.onlyOne(i -> i == 3));
        assertFalse(emptyResult.isPresent());

        assertThrows(TooManyElementsException.class, () -> Stream.of(1, 2, 3, 2, 4).collect(Collectors.onlyOne(i -> i == 2)));
    }

    @Test
    public void test_first() {
        assertEquals(Optional.of(1), Stream.of(1, 2, 3).collect(Collectors.first()));
        assertEquals(Optional.empty(), Stream.empty().collect(Collectors.first()));
    }

    @Test
    public void test_last() {
        assertEquals(Optional.of(3), Stream.of(1, 2, 3).collect(Collectors.last()));
        assertEquals(Optional.empty(), Stream.empty().collect(Collectors.last()));
    }

    @Test
    public void test_first_n() {
        assertEquals(Arrays.asList(1, 2), Stream.of(1, 2, 3).collect(Collectors.first(2)));
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(1, 2, 3).collect(Collectors.first(5)));
        assertTrue(Stream.of(1, 2, 3).collect(Collectors.first(0)).isEmpty());
    }

    @Test
    public void test_last_n() {
        assertEquals(Arrays.asList(2, 3), Stream.of(1, 2, 3).collect(Collectors.last(2)));
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(1, 2, 3).collect(Collectors.last(5)));
        assertTrue(Stream.of(1, 2, 3).collect(Collectors.last(0)).isEmpty());
    }

    @Test
    public void test_joining() {
        assertEquals("abc", Stream.of("a", "b", "c").collect(Collectors.joining()));
        assertEquals("a,b,c", Stream.of("a", "b", "c").collect(Collectors.joining(",")));
        assertEquals("[a,b,c]", Stream.of("a", "b", "c").collect(Collectors.joining(",", "[", "]")));
    }

    @Test
    public void test_summing() {
        assertEquals(6, Stream.of(1, 2, 3).collect(Collectors.summingInt(i -> i)));
        assertEquals(6L, Stream.of(1, 2, 3).collect(Collectors.summingLong(i -> (long) i)));
        assertEquals(6.0, Stream.of(1.0, 2.0, 3.0).collect(Collectors.summingDouble(d -> d)));
        assertEquals(new BigInteger("6"), Stream.of("1", "2", "3").collect(Collectors.summingBigInteger(BigInteger::new)));
        assertEquals(new BigDecimal("6.6"), Stream.of("1.1", "2.2", "3.3").collect(Collectors.summingBigDecimal(BigDecimal::new)));
    }

    @Test
    public void test_averaging() {
        assertEquals(OptionalDouble.of(2.0), Stream.of(1, 2, 3).collect(Collectors.averagingIntOrEmpty(i -> i)));
        assertEquals(OptionalDouble.of(2.0), Stream.of(1L, 2L, 3L).collect(Collectors.averagingLongOrEmpty(i -> i)));
        assertEquals(OptionalDouble.of(2.0), Stream.of(1.0, 2.0, 3.0).collect(Collectors.averagingDoubleOrEmpty(i -> i)));
        assertEquals(Optional.of(new BigDecimal("2")), Stream.of("1", "2", "3").collect(Collectors.averagingBigIntegerOrEmpty(BigInteger::new)));
        assertEquals(Optional.of(new BigDecimal("2.2")), Stream.of("1.1", "2.2", "3.3").collect(Collectors.averagingBigDecimalOrEmpty(BigDecimal::new)));

        assertThrows(NoSuchElementException.class, () -> Stream.<Integer> empty().collect(Collectors.averagingIntOrElseThrow(i -> i)));
    }

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
    public void test_toConcurrentMap() {
        ConcurrentMap<String, Integer> map = Stream.of("a", "bb", "ccc").parallel().collect(Collectors.toConcurrentMap(s -> s, String::length));
        assertEquals(3, map.size());
        assertEquals(1, map.get("a"));

        assertThrows(IllegalStateException.class, () -> Stream.of("a", "b", "a").parallel().collect(Collectors.toConcurrentMap(s -> s, String::length)));
    }

    @Test
    public void test_groupingBy() {
        Map<Integer, List<String>> map = Stream.of("a", "bb", "c", "dd").collect(Collectors.groupingBy(String::length));
        assertEquals(Arrays.asList("a", "c"), map.get(1));
        assertEquals(Arrays.asList("bb", "dd"), map.get(2));
    }

    @Test
    public void test_groupingBy_withDownstream() {
        Map<Integer, Long> map = Stream.of("a", "bb", "c", "dd").collect(Collectors.groupingBy(String::length, Collectors.counting()));
        assertEquals(2L, map.get(1));
        assertEquals(2L, map.get(2));
    }

    @Test
    public void test_groupingByConcurrent() {
        ConcurrentMap<Integer, List<String>> map = Stream.of("a", "bb", "c", "dd").parallel().collect(Collectors.groupingByConcurrent(String::length));
        assertTrue(map.get(1).containsAll(Arrays.asList("a", "c")));
        assertTrue(map.get(2).containsAll(Arrays.asList("bb", "dd")));
    }

    @Test
    public void test_partitioningBy() {
        Map<Boolean, List<Integer>> map = Stream.of(1, 2, 3, 4).collect(Collectors.partitioningBy(i -> i % 2 == 0));
        assertEquals(Arrays.asList(1, 3), map.get(false));
        assertEquals(Arrays.asList(2, 4), map.get(true));
    }

    @Test
    public void test_partitioningBy_withDownstream() {
        Map<Boolean, Long> map = Stream.of(1, 2, 3, 4).collect(Collectors.partitioningBy(i -> i % 2 == 0, Collectors.counting()));
        assertEquals(2L, map.get(false));
        assertEquals(2L, map.get(true));
    }

    @Test
    public void test_minMax() {
        Optional<Pair<Integer, Integer>> result = Stream.of(3, 1, 5, 2, 4).collect(Collectors.minMax());
        assertTrue(result.isPresent());
        assertEquals(Pair.of(1, 5), result.get());

        Optional<Pair<String, String>> strResult = Stream.of("c", "a", "b").collect(Collectors.minMax(Comparator.naturalOrder()));
        assertTrue(strResult.isPresent());
        assertEquals(Pair.of("a", "c"), strResult.get());

        Optional<Pair<Integer, Integer>> emptyResult = Stream.<Integer> empty().collect(Collectors.minMax());
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void test_minMaxBy() {
        List<String> data = Arrays.asList("a", "bb", "ccc");
        Optional<Pair<String, String>> result = data.stream().collect(Collectors.minMaxBy(String::length));
        assertTrue(result.isPresent());
        assertEquals(Pair.of("a", "ccc"), result.get());
    }

    @Test
    public void test_minMaxOrElse() {
        Pair<Integer, Integer> result = Stream.<Integer> empty().collect(Collectors.minMaxOrElseGet(() -> Pair.of(-1, 1)));
        assertEquals(Pair.of(-1, 1), result);

        assertThrows(NoSuchElementException.class, () -> Stream.<Integer> empty().collect(Collectors.minMaxOrElseThrow()));
    }

    @Test
    public void test_summingInt_multiple() {
        Tuple2<Integer, Integer> result2 = Stream.of("a", "bb", "ccc").collect(MoreCollectors.summingInt(String::length, s -> s.hashCode() % 100));
        assertEquals(1 + 2 + 3, result2._1);

        Tuple3<Integer, Integer, Integer> result3 = Stream.of("a", "bb", "ccc")
                .collect(MoreCollectors.summingInt(String::length, s -> s.hashCode() % 100, s -> 1));
        assertEquals(6, result3._1);
        assertEquals(3, result3._3);
    }

    @Test
    public void test_averagingInt_multiple() {
        Tuple2<Double, Double> result2 = Stream.of(1, 2, 3, 4).collect(MoreCollectors.averagingInt(i -> i, i -> i * 2));
        assertEquals(2.5, result2._1);
        assertEquals(5.0, result2._2);
    }

    @Test
    public void test_combine_two() {
        Tuple2<Long, Integer> result = Stream.of(1, 2, 3, 4, 5).collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(i -> i)));
        assertEquals(5L, result._1);
        assertEquals(15, result._2);
    }

    @Test
    public void test_combine_three() {
        Tuple3<Long, Integer, Optional<Integer>> result = Stream.of(1, 2, 3, 4, 5)
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(i -> i), Collectors.maxBy(Fn.identity())));
        assertEquals(5L, result._1);
        assertEquals(15, result._2);
        assertEquals(Optional.of(5), result._3);
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
    public void test_toCollection_with_atMost() {
        List<Integer> result = Stream.range(0, 30).parallel(3).collect(Collectors.toCollection(10, () -> new ArrayList<>()));

        assertNotNull(result);
        assertEquals(10, result.size());
    }

    @Test
    public void testCreate() {
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
    public void testOnlyOneWithPredicate() {
        Optional<Integer> result = Stream.of(1, 2, 3, 4).collect(Collectors.onlyOne(n -> n > 3));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.get());
    }

    @Test
    public void testFirstN() {
        List<String> result = Stream.of("a", "b", "c", "d", "e").collect(Collectors.first(3));

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);

        List<String> all = Stream.of("a", "b").collect(Collectors.first(5));
        Assertions.assertEquals(Arrays.asList("a", "b"), all);
    }

    @Test
    public void testLastN() {
        List<String> result = Stream.of("a", "b", "c", "d", "e").collect(Collectors.last(3));

        Assertions.assertEquals(Arrays.asList("c", "d", "e"), result);

        List<String> all = Stream.of("a", "b").collect(Collectors.last(5));
        Assertions.assertEquals(Arrays.asList("a", "b"), all);
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
    public void testCollectingOrElseThrowIfEmptyWithSupplier() {
        List<String> result = Stream.of("a", "b", "c")
                .collect(Collectors.collectingOrElseThrowIfEmpty(Collectors.toList(), () -> new IllegalStateException("Empty stream")));

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);

        Assertions.assertThrows(IllegalStateException.class, () -> Stream.<String> empty()
                .collect(Collectors.collectingOrElseThrowIfEmpty(Collectors.toList(), () -> new IllegalStateException("Empty stream"))));
    }

    @Test
    public void testMinWithComparator() {
        Optional<String> min = Stream.of("apple", "pie", "banana").collect(Collectors.min(Comparator.comparing(String::length)));

        Assertions.assertTrue(min.isPresent());
        Assertions.assertEquals("pie", min.get());
    }

    @Test
    public void testMinOrElseWithComparator() {
        String min = Stream.of("apple", "pie", "banana").collect(Collectors.minOrElse(Comparator.comparing(String::length), "default"));

        Assertions.assertEquals("pie", min);
    }

    @Test
    public void testMinOrElseGetWithComparator() {
        String min = Stream.of("apple", "pie", "banana").collect(Collectors.minOrElseGet(Comparator.comparing(String::length), () -> "default"));

        Assertions.assertEquals("pie", min);
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
    public void testMinByOrElseThrowWithSupplier() {
        String min = Stream.of("apple", "pie", "banana").collect(Collectors.minByOrElseThrow(String::length, () -> new IllegalStateException("No min")));

        Assertions.assertEquals("pie", min);
    }

    @Test
    public void testMaxWithComparator() {
        Optional<String> max = Stream.of("apple", "pie", "banana").collect(Collectors.max(Comparator.comparing(String::length)));

        Assertions.assertTrue(max.isPresent());
        Assertions.assertEquals("banana", max.get());
    }

    @Test
    public void testMaxOrElseWithComparator() {
        String max = Stream.of("apple", "pie", "banana").collect(Collectors.maxOrElse(Comparator.comparing(String::length), "default"));

        Assertions.assertEquals("banana", max);
    }

    @Test
    public void testMaxOrElseGetWithComparator() {
        String max = Stream.of("apple", "pie", "banana").collect(Collectors.maxOrElseGet(Comparator.comparing(String::length), () -> "default"));

        Assertions.assertEquals("banana", max);
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
    public void testMaxByOrElseThrowWithSupplier() {
        String max = Stream.of("apple", "pie", "banana").collect(Collectors.maxByOrElseThrow(String::length, () -> new IllegalStateException("No max")));

        Assertions.assertEquals("banana", max);
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

        Assertions.assertEquals(3, count);
    }

    @Test
    public void testMinAlll() {
        Optional<Pair<Integer, Long>> result = Stream.of(1, 3, 1, 5, 1).collect(Collectors.minAllWith(Collectors.counting()));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get().left());
        Assertions.assertEquals(3L, result.get().right());
    }

    @Test
    public void testMinAlllWithComparator() {
        Optional<Pair<String, Integer>> result = Stream.of("a", "bb", "ccc", "d", "e")
                .collect(Collectors.minAllWith(Comparator.comparing(String::length), Collectors.countingToInt()));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("a", result.get().left());
        Assertions.assertEquals(3, result.get().right());
    }

    @Test
    public void testMinAlllWithComparatorAndFinisher() {
        String result = Stream.of("a", "bb", "ccc", "d", "e")
                .collect(Collectors.minAllWith(Comparator.comparing(String::length), Collectors.counting(),
                        opt -> opt.map(p -> p.left() + ":" + p.right()).orElse("none")));

        Assertions.assertEquals("a:3", result);
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

        Assertions.assertEquals(2, count);
    }

    @Test
    public void testMaxAlll() {
        Optional<Pair<Integer, Long>> result = Stream.of(5, 3, 5, 1, 5).collect(Collectors.maxAllWith(Collectors.counting()));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(5, result.get().left());
        Assertions.assertEquals(3L, result.get().right());
    }

    @Test
    public void testMaxAlllWithComparator() {
        Optional<Pair<String, Integer>> result = Stream.of("a", "bb", "ccc", "dd", "eee")
                .collect(Collectors.maxAllWith(Comparator.comparing(String::length), Collectors.countingToInt()));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("ccc", result.get().left());
        Assertions.assertEquals(2, result.get().right());
    }

    @Test
    public void testMaxAlllWithComparatorAndFinisher() {
        String result = Stream.of("a", "bb", "ccc", "dd", "eee")
                .collect(Collectors.maxAllWith(Comparator.comparing(String::length), Collectors.counting(),
                        opt -> opt.map(p -> p.left() + ":" + p.right()).orElse("none")));

        Assertions.assertEquals("ccc:2", result);
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
        Assertions.assertEquals(4, result.get());
    }

    @Test
    public void testMinMaxByWithFinisher() {
        Optional<Integer> result = Stream.of("apple", "pie", "banana").collect(Collectors.minMaxBy(String::length, (min, max) -> max.length() - min.length()));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testMinMaxOrElseGetWithComparator() {
        Pair<String, String> result = Stream.of("apple", "pie", "banana")
                .collect(Collectors.minMaxOrElseGet(Comparator.comparing(String::length), () -> Pair.of("", "")));

        Assertions.assertEquals("pie", result.left());
        Assertions.assertEquals("banana", result.right());
    }

    @Test
    public void testMinMaxOrElseThrowWithComparator() {
        Pair<String, String> result = Stream.of("apple", "pie", "banana").collect(Collectors.minMaxOrElseThrow(Comparator.comparing(String::length)));

        Assertions.assertEquals("pie", result.left());
        Assertions.assertEquals("banana", result.right());
    }

    @Test
    public void testSumming_3() {
        {
            Tuple3<Long, Long, Long> result = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.summingIntToLong(e -> e.length(), e -> e.length() * 2, e -> e.length() * 3));

            Assertions.assertEquals(6L, result._1);
            Assertions.assertEquals(12L, result._2);
            Assertions.assertEquals(18L, result._3);
        }
        {
            Tuple3<Long, Long, Long> result = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.summingLong(e -> e.length(), e -> e.length() * 2, e -> e.length() * 3));

            Assertions.assertEquals(6L, result._1);
            Assertions.assertEquals(12L, result._2);
            Assertions.assertEquals(18L, result._3);
        }
        {
            Tuple3<Double, Double, Double> result = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.summingDouble(e -> e.length(), e -> e.length() * 2, e -> e.length() * 3));

            Assertions.assertEquals(6L, result._1);
            Assertions.assertEquals(12L, result._2);
            Assertions.assertEquals(18L, result._3);
        }
        {
            Tuple3<BigInteger, BigInteger, BigInteger> result = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.summingBigInteger(e -> BigInteger.valueOf(e.length()), e -> BigInteger.valueOf(e.length() * 2),
                            e -> BigInteger.valueOf(e.length() * 3)));

            Assertions.assertEquals(6L, result._1.longValue());
            Assertions.assertEquals(12L, result._2.longValue());
            Assertions.assertEquals(18L, result._3.longValue());
        }
        {
            Tuple3<BigDecimal, BigDecimal, BigDecimal> result = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.summingBigDecimal(e -> BigDecimal.valueOf(e.length()), e -> BigDecimal.valueOf(e.length() * 2),
                            e -> BigDecimal.valueOf(e.length() * 3)));

            Assertions.assertEquals(6L, result._1.longValue());
            Assertions.assertEquals(12L, result._2.longValue());
            Assertions.assertEquals(18L, result._3.longValue());
        }
    }

    @Test
    public void testAverage_3() {
        {
            Tuple3<Double, Double, Double> result = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.averagingInt(e -> e.length(), e -> e.length() * 2, e -> e.length() * 3));

            Assertions.assertEquals(2L, result._1);
            Assertions.assertEquals(4L, result._2);
            Assertions.assertEquals(6L, result._3);
        }
        {
            Tuple3<Double, Double, Double> result = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.averagingLong(e -> e.length(), e -> e.length() * 2, e -> e.length() * 3));

            Assertions.assertEquals(2L, result._1);
            Assertions.assertEquals(4L, result._2);
            Assertions.assertEquals(6L, result._3);
        }
        {
            Tuple3<Double, Double, Double> result = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.averagingDouble(e -> e.length(), e -> e.length() * 2, e -> e.length() * 3));

            Assertions.assertEquals(2L, result._1);
            Assertions.assertEquals(4L, result._2);
            Assertions.assertEquals(6L, result._3);
        }
        {
            Tuple3<BigDecimal, BigDecimal, BigDecimal> result = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.averagingBigInteger(e -> BigInteger.valueOf(e.length()), e -> BigInteger.valueOf(e.length() * 2),
                            e -> BigInteger.valueOf(e.length() * 3)));

            Assertions.assertEquals(2L, result._1.longValue());
            Assertions.assertEquals(4L, result._2.longValue());
            Assertions.assertEquals(6L, result._3.longValue());
        }
        {
            Tuple3<BigDecimal, BigDecimal, BigDecimal> result = Stream.of("a", "bb", "ccc")
                    .collect(MoreCollectors.averagingBigDecimal(e -> BigDecimal.valueOf(e.length()), e -> BigDecimal.valueOf(e.length() * 2),
                            e -> BigDecimal.valueOf(e.length() * 3)));

            Assertions.assertEquals(2L, result._1.longValue());
            Assertions.assertEquals(4L, result._2.longValue());
            Assertions.assertEquals(6L, result._3.longValue());
        }
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
    public void testPartitioningByWithDownstream() {
        Map<Boolean, Long> partitioned = Stream.of(1, 2, 3, 4, 5).collect(Collectors.partitioningBy(n -> n % 2 == 0, Collectors.counting()));

        Assertions.assertEquals(3L, partitioned.get(false));
        Assertions.assertEquals(2L, partitioned.get(true));
    }

    @Test
    public void testCountingByWithMapFactory() {
        TreeMap<Integer, Long> counts = Stream.of("a", "bb", "ccc", "dd", "e").collect(Collectors.countingBy(String::length, TreeMap::new));

        Assertions.assertTrue(counts instanceof TreeMap);
        Assertions.assertEquals(2L, counts.get(1));
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

        Assertions.assertEquals(3, map.get("a"));
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
    public void testToUnmodifiableMapWithMergeFunction() {
        Map<Integer, String> map = Stream.of("a", "bb", "ccc", "dd").collect(Collectors.toUnmodifiableMap(String::length, e -> e, (s1, s2) -> s1 + "," + s2));

        Assertions.assertEquals("bb,dd", map.get(2));
    }

    @Test
    public void testToLinkedHashMapWithMergeFunction() {
        Map<Integer, String> map = Stream.of("a", "bb", "ccc", "dd").collect(Collectors.toLinkedHashMap(String::length, e -> e, (s1, s2) -> s1 + "," + s2));

        Assertions.assertTrue(map instanceof LinkedHashMap);
        Assertions.assertEquals("bb,dd", map.get(2));
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
    public void testToBiMapWithAllParameters() {
        BiMap<Integer, String> biMap = Stream.of("a", "bb", "ccc", "dd").collect(Collectors.toBiMap(String::length, e -> e, (s1, s2) -> s1, BiMap::new));

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
    public void testFlatmappingKeyToMultimapWithMapFactory() {
        ListMultimap<Integer, String> multimap = Stream.of("a", "bb")
                .collect(Collectors.flatmappingKeyToMultimap(s -> Arrays.asList(1, 2, 3).subList(0, s.length()), e -> e, Suppliers.ofListMultimap()));

        Assertions.assertEquals(Arrays.asList("a", "bb"), multimap.get(1));
        Assertions.assertEquals(Arrays.asList("bb"), multimap.get(2));
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
    public void testReducingWithMapperOptional() {
        Integer result = stringList.stream().map(String::length).collect(Collectors.reducing(0, Integer::sum));
        assertEquals(31, result);
    }

    @Test
    public void testToMapWithMapFactory() {
        TreeMap<String, Integer> result = stringList.stream().collect(Collectors.toMap(Function.identity(), String::length, TreeMap::new));
        assertTrue(result instanceof TreeMap);
        assertEquals(5, result.size());
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
    public void testFlatmappingValueToMultimapCollection() {
        List<Department> departments = Arrays.asList(new Department("Engineering", Arrays.asList("Java", "Python")),
                new Department("Sales", Arrays.asList("Communication", "Negotiation")));

        ListMultimap<String, String> result = departments.stream().collect(Collectors.flatmappingValueToMultimap(Department::getName, Department::getSkills));

        assertEquals(2, result.get("Engineering").size());
        assertEquals(2, result.get("Sales").size());
    }

    @Test
    public void testFlatmappingKeyToMultimapCollection() {
        List<Employee> employees = Arrays.asList(new Employee("John", Arrays.asList("Java", "Python")),
                new Employee("Jane", Arrays.asList("Java", "JavaScript")));

        ListMultimap<String, String> result = employees.stream().collect(Collectors.flatmappingKeyToMultimap(Employee::getSkills, Employee::getName));

        assertEquals(2, result.get("Java").size());
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
    public void testMoreCollectorsAveragingInt3_Parallel() {
        Tuple3<Double, Double, Double> result = Arrays.asList(1, 2, 3, 4, 5, 6)
                .parallelStream()
                .collect(MoreCollectors.averagingInt(i -> i, i -> i * 2, i -> i * 3));

        assertEquals(3.5, result._1, 0.01);
        assertEquals(7.0, result._2, 0.01);
        assertEquals(10.5, result._3, 0.01);
    }

    @Test
    public void testMoreCollectorsAveragingLong3_Parallel() {
        Tuple3<Double, Double, Double> result = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L)
                .parallelStream()
                .collect(MoreCollectors.averagingLong(l -> l, l -> l * 10, l -> l * 100));

        assertEquals(3.5, result._1, 0.01);
        assertEquals(35.0, result._2, 0.01);
        assertEquals(350.0, result._3, 0.01);
    }

    @Test
    public void testMoreCollectorsAveragingDouble3_Parallel() {
        Tuple3<Double, Double, Double> result = Arrays.asList(1.0, 2.0, 3.0, 4.0)
                .parallelStream()
                .collect(MoreCollectors.averagingDouble(d -> d, d -> d * 2, d -> d * 4));

        assertEquals(2.5, result._1, 0.01);
        assertEquals(5.0, result._2, 0.01);
        assertEquals(10.0, result._3, 0.01);
    }

    @Test
    public void testMoreCollectorsAveragingBigInteger3_Parallel() {
        Tuple3<BigDecimal, BigDecimal, BigDecimal> result = Arrays
                .asList(BigInteger.valueOf(2), BigInteger.valueOf(4), BigInteger.valueOf(6), BigInteger.valueOf(8))
                .parallelStream()
                .collect(MoreCollectors.averagingBigInteger(bi -> bi, bi -> bi.multiply(BigInteger.TEN), bi -> bi.multiply(BigInteger.valueOf(100))));

        assertEquals(0, new BigDecimal("5").compareTo(result._1));
        assertEquals(0, new BigDecimal("50").compareTo(result._2));
        assertEquals(0, new BigDecimal("500").compareTo(result._3));
    }

    @Test
    public void testMoreCollectorsAveragingBigDecimal3_Parallel() {
        Tuple3<BigDecimal, BigDecimal, BigDecimal> result = Arrays
                .asList(new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0"), new BigDecimal("4.0"))
                .parallelStream()
                .collect(MoreCollectors.averagingBigDecimal(bd -> bd, bd -> bd.multiply(new BigDecimal("2")), bd -> bd.multiply(new BigDecimal("4"))));

        assertEquals(0, new BigDecimal("2.5").compareTo(result._1));
        assertEquals(0, new BigDecimal("5").compareTo(result._2));
        assertEquals(0, new BigDecimal("10").compareTo(result._3));
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
                        (count, sum, avg) -> String.format("Count: %d, Sum: %d, Avg: %.2f", count, sum, avg.get())));

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
        assertEquals(columnNames, result.columnNames());
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

    @Test
    public void testCreate_SupplierAccumulatorCombinerFinisherCharacteristics_Collection() {
        Set<Characteristics> characteristics = EnumSet.of(Characteristics.UNORDERED);
        Collector<String, List<String>, Integer> collector = Collectors.create(ArrayList::new, List::add, (a, b) -> {
            a.addAll(b);
            return a;
        }, List::size, characteristics);

        Integer result = stringList.stream().collect(collector);
        assertEquals(5, result);
        assertTrue(collector.characteristics().contains(Characteristics.UNORDERED));
    }

    @Test
    public void testFirst_N_Negative() {
        assertThrows(IllegalArgumentException.class, () -> Collectors.first(-1));
    }

    @Test
    public void testLast_N_Negative() {
        assertThrows(IllegalArgumentException.class, () -> Collectors.last(-1));
    }

    @Test
    public void testAveragingLongOrEmpty_Empty() {
        OptionalDouble result = Stream.<Person> empty().collect(Collectors.averagingLongOrEmpty(p -> (long) p.getAge()));
        assertFalse(result.isPresent());
    }

    @Test
    public void testAveragingDoubleOrEmpty_Empty() {
        OptionalDouble result = Stream.<Double> empty().collect(Collectors.averagingDoubleOrEmpty(Double::doubleValue));
        assertFalse(result.isPresent());
    }

    @Test
    public void testAveragingBigIntegerOrEmpty_Empty() {
        u.Optional<BigDecimal> result = Stream.<Person> empty().collect(Collectors.averagingBigIntegerOrEmpty(Person::getBigIntValue));
        assertFalse(result.isPresent());
    }

    @Test
    public void testAveragingBigDecimalOrEmpty_Empty() {
        u.Optional<BigDecimal> result = Stream.<Person> empty().collect(Collectors.averagingBigDecimalOrEmpty(Person::getBigDecValue));
        assertFalse(result.isPresent());
    }

    @Test
    public void testCommonPrefix_Empty() {
        String result = Stream.<String> empty().collect(Collectors.commonPrefix());
        assertEquals("", result);
    }

    @Test
    public void testCommonPrefix_WithValues() {
        String result = Stream.of("abcdef", "abcxyz", "abcpqr").collect(Collectors.commonPrefix());
        assertEquals("abc", result);
    }

    @Test
    public void testCommonPrefix_NoCommonPrefix() {
        String result = Stream.of("abc", "xyz").collect(Collectors.commonPrefix());
        assertEquals("", result);
    }

    @Test
    public void testCommonSuffix_Empty() {
        String result = Stream.<String> empty().collect(Collectors.commonSuffix());
        assertEquals("", result);
    }

    @Test
    public void testReducingWithOperator() {
        Optional<Integer> result = Stream.of(1, 2, 3).collect(Collectors.reducing(Integer::sum));
        assertTrue(result.isPresent());
        assertEquals(6, result.get().intValue());
    }

    @Test
    public void testReducingWithOperator_Empty() {
        Optional<Integer> result = Stream.<Integer> empty().collect(Collectors.reducing(Integer::sum));
        assertFalse(result.isPresent());
    }

    @Test
    public void testReducingWithIdentityMapperAndOperator() {
        int result = Stream.of("a", "bb", "ccc").collect(Collectors.reducing(0, String::length, Integer::sum));
        assertEquals(6, result);
    }

    @Test
    public void testToMapFromEntries_WithMerge() {
        Map<String, Integer> result = Stream.of(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("a", 2))
                .collect(Collectors.toMap(Integer::sum));
        assertEquals(1, result.size());
        assertEquals(3, result.get("a").intValue());
    }

    @Test
    public void testToMapFromEntries_WithMapFactory() {
        java.util.TreeMap<String, Integer> result = Stream.of(new AbstractMap.SimpleEntry<>("b", 2), new AbstractMap.SimpleEntry<>("a", 1))
                .collect(Collectors.toMap(java.util.TreeMap::new));
        assertEquals("a", result.firstKey());
    }

    @Test
    public void testAveragingIntOrElseThrow_Empty() {
        assertThrows(NoSuchElementException.class, () -> Stream.<Integer> empty().collect(Collectors.averagingIntOrElseThrow(i -> i)));
    }

    @Test
    public void testAveragingLongOrElseThrow_Empty() {
        assertThrows(NoSuchElementException.class, () -> Stream.<Long> empty().collect(Collectors.averagingLongOrElseThrow(l -> l)));
    }

    @Test
    public void testAveragingDoubleOrElseThrow_Empty() {
        assertThrows(NoSuchElementException.class, () -> Stream.<Double> empty().collect(Collectors.averagingDoubleOrElseThrow(d -> d)));
    }

    @Test
    public void testAveragingBigIntegerOrElseThrow_Empty() {
        assertThrows(NoSuchElementException.class, () -> Stream.<BigInteger> empty().collect(Collectors.averagingBigIntegerOrElseThrow(i -> i)));
    }

    @Test
    public void testAveragingBigDecimalOrElseThrow_Empty() {
        assertThrows(NoSuchElementException.class, () -> Stream.<BigDecimal> empty().collect(Collectors.averagingBigDecimalOrElseThrow(i -> i)));
    }

    @Test
    public void testReducingOrElseThrowWithMapperAndExceptionSupplier() {
        assertThrows(IllegalStateException.class,
                () -> Stream.<String> empty().collect(Collectors.reducingOrElseThrow(String::length, Integer::sum, () -> new IllegalStateException("empty"))));
    }
}
