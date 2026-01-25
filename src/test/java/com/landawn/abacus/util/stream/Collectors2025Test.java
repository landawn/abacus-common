package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.util.HashSet;
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
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Stream;

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
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.Tuple.Tuple6;
import com.landawn.abacus.util.Tuple.Tuple7;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.Collectors.MoreCollectors;
import com.landawn.abacus.util.stream.Stream.StreamEx;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Tag("2025")
public class Collectors2025Test extends TestBase {

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
        Optional<Pair<Integer, Long>> result = integerList.stream().collect(Collectors.minAlll(Collectors.counting()));
        assertTrue(result.isPresent());
        assertEquals(1, result.get().left());
        assertEquals(1L, result.get().right());
    }

    @Test
    public void testMinAlll_WithComparatorAndDownstream() {
        Optional<Pair<String, Long>> result = stringList.stream().collect(Collectors.minAlll(Comparator.comparingInt(String::length), Collectors.counting()));
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
        Optional<Pair<Integer, Long>> result = integerList.stream().collect(Collectors.maxAlll(Collectors.counting()));
        assertTrue(result.isPresent());
        assertEquals(5, result.get().left());
        assertEquals(1L, result.get().right());
    }

    @Test
    public void testMaxAlll_WithComparatorAndDownstream() {
        Optional<Pair<String, Long>> result = stringList.stream().collect(Collectors.maxAlll(Comparator.comparingInt(String::length), Collectors.counting()));
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
        BiMap<String, Integer> result = StreamEx.of(stringList)
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
}
