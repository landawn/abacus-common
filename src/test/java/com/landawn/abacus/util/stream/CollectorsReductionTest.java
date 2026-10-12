package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Collector;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.BigDecimalSummaryStatistics;
import com.landawn.abacus.util.BigIntegerSummaryStatistics;
import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.Comparators;
import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.stream.Collectors.MoreCollectors;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u;

/** Collectors reduction contracts. */
public class CollectorsReductionTest extends CollectorsTestSupport {

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
    public void test_averaging() {
        assertEquals(OptionalDouble.of(2.0), Stream.of(1, 2, 3).collect(Collectors.averagingIntOrEmpty(i -> i)));
        assertEquals(OptionalDouble.of(2.0), Stream.of(1L, 2L, 3L).collect(Collectors.averagingLongOrEmpty(i -> i)));
        assertEquals(OptionalDouble.of(2.0), Stream.of(1.0, 2.0, 3.0).collect(Collectors.averagingDoubleOrEmpty(i -> i)));
        assertEquals(Optional.of(new BigDecimal("2")), Stream.of("1", "2", "3").collect(Collectors.averagingBigIntegerOrEmpty(BigInteger::new)));
        assertEquals(Optional.of(new BigDecimal("2.2")), Stream.of("1.1", "2.2", "3.3").collect(Collectors.averagingBigDecimalOrEmpty(BigDecimal::new)));

        assertThrows(NoSuchElementException.class, () -> Stream.<Integer> empty().collect(Collectors.averagingIntOrElseThrow(i -> i)));
    }

    @Test
    public void testAveragingBigDecimal() {
        BigDecimal result = personList.stream().collect(Collectors.averagingBigDecimal(Person::getBigDecValue));
        assertTrue(result.compareTo(BigDecimal.valueOf(300.5)) == 0);
    }

    @Test
    public void testAveragingBigDecimalOrElseThrow() {
        BigDecimal result = personList.stream().collect(Collectors.averagingBigDecimalOrElseThrow(Person::getBigDecValue));
        assertTrue(result.compareTo(BigDecimal.valueOf(300.5)) == 0);
    }

    @Test
    public void testAveragingBigDecimalOrElseThrow_Empty() {
        assertThrows(NoSuchElementException.class, () -> Stream.<BigDecimal> empty().collect(Collectors.averagingBigDecimalOrElseThrow(i -> i)));
    }

    @Test
    public void testAveragingBigDecimalOrEmpty() {
        Optional<BigDecimal> result = personList.stream().collect(Collectors.averagingBigDecimalOrEmpty(Person::getBigDecValue));
        assertTrue(result.isPresent());
        assertTrue(result.get().compareTo(BigDecimal.valueOf(300.5)) == 0);
    }

    @Test
    public void testAveragingBigDecimalOrEmpty_Empty() {
        u.Optional<BigDecimal> result = Stream.<Person> empty().collect(Collectors.averagingBigDecimalOrEmpty(Person::getBigDecValue));
        assertFalse(result.isPresent());
    }

    @Test
    public void testAveragingBigInteger() {
        BigDecimal result = personList.stream().collect(Collectors.averagingBigInteger(Person::getBigIntValue));
        assertEquals(BigDecimal.valueOf(300), result);
    }

    @Test
    public void testAveragingBigIntegerOrElseThrow() {
        BigDecimal result = personList.stream().collect(Collectors.averagingBigIntegerOrElseThrow(Person::getBigIntValue));
        assertEquals(BigDecimal.valueOf(300), result);
    }

    @Test
    public void testAveragingBigIntegerOrElseThrow_Empty() {
        assertThrows(NoSuchElementException.class, () -> Stream.<BigInteger> empty().collect(Collectors.averagingBigIntegerOrElseThrow(i -> i)));
    }

    @Test
    public void testAveragingBigIntegerOrEmpty() {
        Optional<BigDecimal> result = personList.stream().collect(Collectors.averagingBigIntegerOrEmpty(Person::getBigIntValue));
        assertTrue(result.isPresent());
        assertEquals(BigDecimal.valueOf(300), result.get());
    }

    @Test
    public void testAveragingBigIntegerOrEmpty_Empty() {
        u.Optional<BigDecimal> result = Stream.<Person> empty().collect(Collectors.averagingBigIntegerOrEmpty(Person::getBigIntValue));
        assertFalse(result.isPresent());
    }

    @Test
    public void testAveragingDouble() {
        Double result = doubleList.stream().collect(Collectors.averagingDouble(Double::doubleValue));
        assertEquals(3.3, result, 0.01);
    }

    @Test
    public void testAveragingDoubleOrElseThrow() {
        Double result = doubleList.stream().collect(Collectors.averagingDoubleOrElseThrow(Double::doubleValue));
        assertEquals(3.3, result, 0.01);
    }

    @Test
    public void testAveragingDoubleOrElseThrow_Empty() {
        assertThrows(NoSuchElementException.class, () -> Stream.<Double> empty().collect(Collectors.averagingDoubleOrElseThrow(d -> d)));
    }

    @Test
    public void testAveragingDoubleOrEmpty() {
        OptionalDouble result = doubleList.stream().collect(Collectors.averagingDoubleOrEmpty(Double::doubleValue));
        assertTrue(result.isPresent());
        assertEquals(3.3, result.getAsDouble(), 0.01);
    }

    @Test
    public void testAveragingDoubleOrEmpty_Empty() {
        OptionalDouble result = Stream.<Double> empty().collect(Collectors.averagingDoubleOrEmpty(Double::doubleValue));
        assertFalse(result.isPresent());
    }

    // averagingInt

    @Test
    public void testAveragingInt() {
        Double result = personList.stream().collect(Collectors.averagingInt(Person::getAge));
        assertEquals(30.0, result, 0.01);
    }

    @Test
    public void testAveragingInt_2Mappers_Parallel_CombinerUsed() {
        // Exercises AveragingInt_Combiner_2 in parallel
        Tuple2<Double, Double> result = personList.parallelStream().collect(MoreCollectors.averagingInt(Person::getAge, p -> p.getAge() * 2));
        assertNotNull(result);
        assertEquals(result._1 * 2, result._2, 0.001);
    }

    @Test
    public void test_averagingInt_multiple() {
        Tuple2<Double, Double> result2 = Stream.of(1, 2, 3, 4).collect(MoreCollectors.averagingInt(i -> i, i -> i * 2));
        assertEquals(2.5, result2._1);
        assertEquals(5.0, result2._2);
    }

    @Test
    public void testAveragingIntOrElseThrow() {
        Double result = personList.stream().collect(Collectors.averagingIntOrElseThrow(Person::getAge));
        assertEquals(30.0, result, 0.01);
    }

    @Test
    public void testAveragingIntOrElseThrow_Empty() {
        assertThrows(NoSuchElementException.class, () -> Stream.<Integer> empty().collect(Collectors.averagingIntOrElseThrow(i -> i)));
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
    public void testAveragingLong() {
        Double result = longList.stream().collect(Collectors.averagingLong(Long::longValue));
        assertEquals(300.0, result, 0.01);
    }

    @Test
    public void testAveragingLongOrElseThrow() {
        Double result = longList.stream().collect(Collectors.averagingLongOrElseThrow(Long::longValue));
        assertEquals(300.0, result, 0.01);
    }

    @Test
    public void testAveragingLongOrElseThrow_Empty() {
        assertThrows(NoSuchElementException.class, () -> Stream.<Long> empty().collect(Collectors.averagingLongOrElseThrow(l -> l)));
    }

    @Test
    public void testAveragingLongOrEmpty() {
        OptionalDouble result = longList.stream().collect(Collectors.averagingLongOrEmpty(Long::longValue));
        assertTrue(result.isPresent());
        assertEquals(300.0, result.getAsDouble(), 0.01);
    }

    @Test
    public void testAveragingLongOrEmpty_Empty() {
        OptionalDouble result = Stream.<Person> empty().collect(Collectors.averagingLongOrEmpty(p -> (long) p.getAge()));
        assertFalse(result.isPresent());
    }

    @Test
    public void testCounting() {
        Long result = stringList.stream().collect(Collectors.counting());
        assertEquals(5L, result);
    }

    // countingBy

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
    public void testCountingByWithMapFactory() {
        TreeMap<Integer, Long> counts = Stream.of("a", "bb", "ccc", "dd", "e").collect(Collectors.countingBy(String::length, TreeMap::new));

        Assertions.assertTrue(counts instanceof TreeMap);
        Assertions.assertEquals(2L, counts.get(1));
    }

    @Test
    public void testCountingToInt() {
        Integer result = stringList.stream().collect(Collectors.countingToInt());
        assertEquals(5, result);
    }

    // countingToIntBy

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
    public void testCountingToIntByWithMapFactory() {
        TreeMap<Integer, Integer> counts = Stream.of("a", "bb", "ccc", "dd", "e").collect(Collectors.countingToIntBy(String::length, TreeMap::new));

        Assertions.assertTrue(counts instanceof TreeMap);
        Assertions.assertEquals(2, counts.get(1));
    }

    // first

    @Test
    public void testFirst() {
        Optional<String> result = stringList.stream().collect(Collectors.first());
        assertTrue(result.isPresent());
        assertEquals("apple", result.get());
    }

    @Test
    public void test_first() {
        assertEquals(Optional.of(1), Stream.of(1, 2, 3).collect(Collectors.first()));
        assertEquals(Optional.empty(), Stream.empty().collect(Collectors.first()));
    }

    @Test
    public void testFirst_Empty() {
        Optional<String> result = Stream.<String> empty().collect(Collectors.first());
        assertFalse(result.isPresent());
    }

    @Test
    public void testFirst_N() {
        List<String> result = stringList.stream().collect(Collectors.first(3));
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("apple", "banana", "cherry"), result);
    }

    @Test
    public void test_first_n() {
        assertEquals(Arrays.asList(1, 2), Stream.of(1, 2, 3).collect(Collectors.first(2)));
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(1, 2, 3).collect(Collectors.first(5)));
        assertTrue(Stream.of(1, 2, 3).collect(Collectors.first(0)).isEmpty());
    }

    @Test
    public void testFirst_N_Negative() {
        assertThrows(IllegalArgumentException.class, () -> Collectors.first(-1));
    }

    @Test
    public void testFirstLast_NullElement_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> Stream.of((Integer) null, 1, 2).collect(Collectors.first()));
        assertThrows(NullPointerException.class, () -> Stream.of(1, 2, (Integer) null).collect(Collectors.last()));
    }

    @Test
    public void testFirstN() {
        List<String> result = Stream.of("a", "b", "c", "d", "e").collect(Collectors.first(3));

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);

        List<String> all = Stream.of("a", "b").collect(Collectors.first(5));
        Assertions.assertEquals(Arrays.asList("a", "b"), all);
    }

    // last

    @Test
    public void testLast() {
        Optional<String> result = stringList.stream().collect(Collectors.last());
        assertTrue(result.isPresent());
        assertEquals("elderberry", result.get());
    }

    @Test
    public void test_last() {
        assertEquals(Optional.of(3), Stream.of(1, 2, 3).collect(Collectors.last()));
        assertEquals(Optional.empty(), Stream.empty().collect(Collectors.last()));
    }

    @Test
    public void testLast_Empty() {
        Optional<String> result = Stream.<String> empty().collect(Collectors.last());
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast_N() {
        List<String> result = stringList.stream().collect(Collectors.last(3));
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("cherry", "date", "elderberry"), result);
    }

    @Test
    public void test_last_n() {
        assertEquals(Arrays.asList(2, 3), Stream.of(1, 2, 3).collect(Collectors.last(2)));
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(1, 2, 3).collect(Collectors.last(5)));
        assertTrue(Stream.of(1, 2, 3).collect(Collectors.last(0)).isEmpty());
    }

    @Test
    public void testLast_N_Negative() {
        assertThrows(IllegalArgumentException.class, () -> Collectors.last(-1));
    }

    @Test
    public void testLastN() {
        List<String> result = Stream.of("a", "b", "c", "d", "e").collect(Collectors.last(3));

        Assertions.assertEquals(Arrays.asList("c", "d", "e"), result);

        List<String> all = Stream.of("a", "b").collect(Collectors.last(5));
        Assertions.assertEquals(Arrays.asList("a", "b"), all);
    }

    @Test
    public void testLastNAcceptsNullElements() {
        // regression: last(n) with n <= 1024 used ArrayDeque, which rejects null elements
        assertEquals(N.asList(null, "b"), Stream.of("a", null, "b").collect(Collectors.last(2)));
        assertEquals(N.asList("a", null, "b"), Stream.of("a", null, "b").collect(Collectors.last(5)));
    }

    // max

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
    public void testMaxWithComparator() {
        Optional<String> max = Stream.of("apple", "pie", "banana").collect(Collectors.max(Comparator.comparing(String::length)));

        Assertions.assertTrue(max.isPresent());
        Assertions.assertEquals("banana", max.get());
    }

    // maxAll

    @Test
    public void testMaxAll() {
        List<Integer> result = integerList.stream().collect(Collectors.maxAll());
        assertEquals(Arrays.asList(5), result);
    }

    @Test
    public void testMaxAll_WithAtMostSize_Parallel_CombinerUsed() {
        // Triggers lambda$219 combiner for maxAll(Comparator, int) in parallel
        List<Integer> result = Stream.of(1, 5, 3, 5, 2, 5, 4).parallel().collect(Collectors.maxAll(Comparator.naturalOrder(), 3));
        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(i -> i == 5));
    }

    @Test
    public void testMaxAll_WithComparator() {
        List<String> result = stringList.stream().collect(Collectors.maxAll(Comparator.comparingInt(String::length)));
        assertEquals(Arrays.asList("elderberry"), result);
    }

    @Test
    public void testMaxAll_WithComparatorAndDownstream() {
        Long result = stringList.stream().collect(Collectors.maxAll(Comparator.comparingInt(String::length), Collectors.counting()));
        assertEquals(1L, result);
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
    public void testMaxAll_WithDownstream_Parallel_CombinerUsed() {
        // Triggers lambda$224 (maxAll with downstream combiner) in parallel
        Long count = Stream.of(1, 5, 3, 5, 2).parallel().collect(Collectors.maxAll(Comparator.naturalOrder(), Collectors.counting()));
        assertEquals(2L, count.longValue());
    }

    @Test
    public void testMaxAllRejectsNegativeLimit() {
        assertThrows(IllegalArgumentException.class, () -> Collectors.maxAll(Comparator.naturalOrder(), -1));
    }

    @Test
    public void testMaxAllResetsOpaqueDownstreamStateWithFreshContainer() {
        class StatefulList extends ArrayList<Integer> {
            private static final long serialVersionUID = 1L;
            int sum;
        }

        final Collector<Integer, StatefulList, Integer> downstream = Collector.of(StatefulList::new, (state, value) -> {
            state.add(value);
            state.sum += value;
        }, (left, right) -> {
            left.addAll(right);
            left.sum += right.sum;
            return left;
        }, state -> state.sum);

        // A downstream accumulation container is opaque: Collection.clear() does not reset
        // StatefulList.sum. A new winner must therefore start with a fresh container.
        assertEquals(10, Stream.of(1, 5, 5).collect(Collectors.maxAll(Comparator.<Integer> naturalOrder(), downstream)));
        assertEquals(2, Stream.of(5, 1, 1).collect(Collectors.minAll(Comparator.<Integer> naturalOrder(), downstream)));

        final Optional<Pair<Integer, Integer>> withRepresentative = Stream.of(1, 5, 5)
                .collect(Collectors.maxAllWith(Comparators.<Integer> naturalOrder(), downstream));
        assertTrue(withRepresentative.isPresent());
        assertEquals(Integer.valueOf(5), withRepresentative.get().left());
        assertEquals(Integer.valueOf(10), withRepresentative.get().right());
    }

    @Test
    public void testMaxAllWithComparator() {
        List<String> maxs = Stream.of("a", "bb", "ccc", "dd", "eee").collect(Collectors.maxAll(Comparator.comparing(String::length)));

        Assertions.assertEquals(Arrays.asList("ccc", "eee"), maxs);
    }

    @Test
    public void testMaxAllWithComparatorAndDownstream() {
        Integer count = Stream.of("a", "bb", "ccc", "dd", "eee").collect(Collectors.maxAll(Comparator.comparing(String::length), Collectors.countingToInt()));

        Assertions.assertEquals(2, count);
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
    public void testMinMaxAllCollectorsPropagateTieOrdering() {
        Comparator<String> byLength = Comparator.comparingInt(String::length);

        assertFalse(Collectors.maxAll(byLength, 3).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.minAll(byLength, 3).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.maxAll(byLength, Collectors.<String> toList()).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.minAll(byLength, Collectors.<String> toList()).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.maxAllWith(byLength, Collectors.<String> toList()).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.minAllWith(byLength, Collectors.<String> toList()).characteristics().contains(Characteristics.UNORDERED));

        assertTrue(Collectors.maxAll(byLength, Collectors.<String> toSet()).characteristics().contains(Characteristics.UNORDERED));
        assertTrue(Collectors.minAll(byLength, Collectors.<String> toSet()).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.maxAllWith(byLength, Collectors.<String> toSet()).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.minAllWith(byLength, Collectors.<String> toSet()).characteristics().contains(Characteristics.UNORDERED));

        List<String> firstThreeTies = java.util.stream.Stream.of("a", "b", "c", "d", "e").parallel().collect(Collectors.maxAll(byLength, 3));
        assertEquals(Arrays.asList("a", "b", "c"), firstThreeTies);
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
    public void testMaxAllWith_Parallel_CombinerUsed() {
        // Exercises the maxAllWith combiner in parallel
        Optional<Pair<Integer, Long>> result = integerList.parallelStream().collect(Collectors.maxAllWith(Comparator.naturalOrder(), Collectors.counting()));
        assertTrue(result.isPresent());
        assertEquals(5, (int) result.get().left());
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

    // maxByOrElseThrow

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
    public void testMaxByOrElseThrowWithSupplier() {
        String max = Stream.of("apple", "pie", "banana").collect(Collectors.maxByOrElseThrow(String::length, () -> new IllegalStateException("No max")));

        Assertions.assertEquals("banana", max);
    }

    // maxOrElse

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
    public void testMaxOrElseWithComparator() {
        String max = Stream.of("apple", "pie", "banana").collect(Collectors.maxOrElse(Comparator.comparing(String::length), "default"));

        Assertions.assertEquals("banana", max);
    }

    // maxOrElseGet

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
    public void testMaxOrElseGetWithComparator() {
        String max = Stream.of("apple", "pie", "banana").collect(Collectors.maxOrElseGet(Comparator.comparing(String::length), () -> "default"));

        Assertions.assertEquals("banana", max);
    }

    // maxOrElseThrow

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

    // min

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
    public void testMinMaxWithNullWinnerThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> Stream.of((String) null).collect(Collectors.min()));
        assertThrows(NullPointerException.class, () -> Stream.of((String) null).collect(Collectors.max()));

        // unchanged for normal values
        assertEquals(Optional.of(1), Stream.of(3, 1, 2).collect(Collectors.min()));
    }

    @Test
    public void testMinWithComparator() {
        Optional<String> min = Stream.of("apple", "pie", "banana").collect(Collectors.min(Comparator.comparing(String::length)));

        Assertions.assertTrue(min.isPresent());
        Assertions.assertEquals("pie", min.get());
    }

    // minAll

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
    public void testMinAll_WithComparatorAndDownstream() {
        Long result = stringList.stream().collect(Collectors.minAll(Comparator.comparingInt(String::length), Collectors.counting()));
        assertEquals(1L, result);
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
    public void testMinAll_WithDownstream_Parallel_CombinerUsed() {
        // Triggers lambda$228 (minAll with downstream combiner) in parallel
        List<Integer> minElements = Stream.of(1, 5, 1, 3, 2).parallel().collect(Collectors.minAll(Comparator.naturalOrder(), Collectors.toList()));
        assertEquals(2, minElements.size());
        assertTrue(minElements.stream().allMatch(i -> i == 1));
    }

    @Test
    public void testMinAllRejectsNegativeLimit() {
        assertThrows(IllegalArgumentException.class, () -> Collectors.minAll(Comparator.naturalOrder(), -1));
    }

    @Test
    public void testMinAllWithComparator() {
        List<String> mins = Stream.of("a", "bb", "ccc", "d", "e").collect(Collectors.minAll(Comparator.comparing(String::length)));

        Assertions.assertEquals(Arrays.asList("a", "d", "e"), mins);
    }

    @Test
    public void testMinAllWithComparatorAndDownstream() {
        Integer count = Stream.of("a", "bb", "ccc", "d", "e").collect(Collectors.minAll(Comparator.comparing(String::length), Collectors.countingToInt()));

        Assertions.assertEquals(3, count);
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
    public void testMinAllWith_Parallel_CombinerUsed() {
        // Exercises the minAllWith combiner in parallel
        Optional<Pair<Integer, Long>> result = integerList.parallelStream().collect(Collectors.minAllWith(Comparator.naturalOrder(), Collectors.counting()));
        assertTrue(result.isPresent());
        assertEquals(1, (int) result.get().left());
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

    // minByOrElseThrow

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
    public void testMinByOrElseThrowWithSupplier() {
        String min = Stream.of("apple", "pie", "banana").collect(Collectors.minByOrElseThrow(String::length, () -> new IllegalStateException("No min")));

        Assertions.assertEquals("pie", min);
    }

    // minMax

    @Test
    public void testMinMax() {
        Optional<Pair<Integer, Integer>> result = integerList.stream().collect(Collectors.minMax());
        assertTrue(result.isPresent());
        assertEquals(1, result.get().left());
        assertEquals(5, result.get().right());
    }

    @Test
    public void test_minMax() {
        Optional<Pair<Integer, Integer>> result = Stream.of(3, 1, 5, 2, 4).collect(Collectors.minMax());
        assertTrue(result.isPresent());
        assertEquals(Pair.of(1, 5), result.get());

        Optional<Pair<Integer, Integer>> withNulls = Stream.<Integer> of(null, 3, 1, null, 2).collect(Collectors.minMax());
        assertTrue(withNulls.isPresent());
        assertEquals(Pair.of(1, 3), withNulls.get());

        Optional<Pair<Integer, Integer>> allNull = Stream.<Integer> of(null, null).collect(Collectors.minMax());
        assertTrue(allNull.isPresent());
        assertEquals(Pair.of(null, null), allNull.get());

        Optional<Pair<String, String>> strResult = Stream.of("c", "a", "b").collect(Collectors.minMax(Comparator.naturalOrder()));
        assertTrue(strResult.isPresent());
        assertEquals(Pair.of("a", "c"), strResult.get());

        Optional<Pair<Integer, Integer>> emptyResult = Stream.<Integer> empty().collect(Collectors.minMax());
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testMinMax_WithComparator() {
        Optional<Pair<String, String>> result = stringList.stream().collect(Collectors.minMax(Comparator.comparingInt(String::length)));
        assertTrue(result.isPresent());
        assertEquals("date", result.get().left());
        assertEquals("elderberry", result.get().right());
    }

    @Test
    public void testMinMaxCollectorsAreNotUnordered() {
        // regression: min/max/minMax document first-encountered tie-breaking, but were advertised
        // UNORDERED, which permits a parallel pipeline to discard encounter order and break ties
        // arbitrarily (JDK Collectors.minBy/maxBy/reducing(BinaryOperator) are not UNORDERED either).
        assertFalse(Collectors.min(Comparator.<Integer> naturalOrder()).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.max(Comparator.<Integer> naturalOrder()).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.reducing((a, b) -> a).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.<Integer> minMax().characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.minMax(Comparator.<Integer> naturalOrder()).characteristics().contains(Characteristics.UNORDERED));
        // truly commutative reductions stay UNORDERED
        assertTrue(Collectors.summingInt(i -> (Integer) i).characteristics().contains(Characteristics.UNORDERED));
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

    // minMaxBy

    @Test
    public void testMinMaxBy() {
        Optional<Pair<Person, Person>> result = personList.stream().collect(Collectors.minMaxBy(Person::getAge));
        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().left().getName());
        assertEquals("Charlie", result.get().right().getName());
    }

    @Test
    public void test_minMaxBy() {
        List<String> data = Arrays.asList("a", "bb", "ccc");
        Optional<Pair<String, String>> result = data.stream().collect(Collectors.minMaxBy(String::length));
        assertTrue(result.isPresent());
        assertEquals(Pair.of("a", "ccc"), result.get());
    }

    @Test
    public void testMinMaxByOrdersNullKeyBeforeNonNullKeys() {
        final Optional<Pair<String, String>> result = Stream.of("bb", "a", "ccc").collect(Collectors.minMaxBy(s -> "bb".equals(s) ? null : s.length()));

        assertTrue(result.isPresent());
        assertEquals("bb", result.get().left());
        assertEquals("ccc", result.get().right());

        // minBy, by contrast, orders null keys last.
        assertEquals("a", Stream.of("bb", "a", "ccc").collect(Collectors.minBy(s -> "bb".equals(s) ? null : s.length())).get());

        final Optional<String> withFinisher = Stream.of("bb", "a", "ccc")
                .collect(Collectors.minMaxBy(s -> "bb".equals(s) ? null : s.length(), (min, max) -> min + "-" + max));
        assertEquals("bb-ccc", withFinisher.get());
    }

    @Test
    public void testMinMaxByWithFinisher() {
        Optional<Integer> result = Stream.of("apple", "pie", "banana").collect(Collectors.minMaxBy(String::length, (min, max) -> max.length() - min.length()));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void test_minMaxOrElse() {
        Pair<Integer, Integer> result = Stream.<Integer> empty().collect(Collectors.minMaxOrElseGet(() -> Pair.of(-1, 1)));
        assertEquals(Pair.of(-1, 1), result);

        assertThrows(NoSuchElementException.class, () -> Stream.<Integer> empty().collect(Collectors.minMaxOrElseThrow()));
    }

    // minMaxOrElseGet

    @Test
    public void testMinMaxOrElseGet() {
        Pair<Integer, Integer> result = integerList.stream().collect(Collectors.minMaxOrElseGet(() -> Pair.of(-1, -1)));
        assertEquals(1, result.left());
        assertEquals(5, result.right());

        result = Stream.<Integer> of(null, 3, 1, null, 2).collect(Collectors.minMaxOrElseGet(() -> Pair.of(-1, -1)));
        assertEquals(Pair.of(1, 3), result);

        result = Stream.<Integer> of(null, null).collect(Collectors.minMaxOrElseGet(() -> Pair.of(-1, -1)));
        assertEquals(Pair.of(null, null), result);
    }

    @Test
    public void testMinMaxOrElseGet_WithComparator() {
        Pair<String, String> result = stringList.stream()
                .collect(Collectors.minMaxOrElseGet(Comparator.comparingInt(String::length), () -> Pair.of("default", "default")));
        assertEquals("date", result.left());
        assertEquals("elderberry", result.right());
    }

    @Test
    public void testMinMaxOrElseGetWithComparator() {
        Pair<String, String> result = Stream.of("apple", "pie", "banana")
                .collect(Collectors.minMaxOrElseGet(Comparator.comparing(String::length), () -> Pair.of("", "")));

        Assertions.assertEquals("pie", result.left());
        Assertions.assertEquals("banana", result.right());
    }

    // minMaxOrElseThrow

    @Test
    public void testMinMaxOrElseThrow() {
        Pair<Integer, Integer> result = integerList.stream().collect(Collectors.minMaxOrElseThrow());
        assertEquals(1, result.left());
        assertEquals(5, result.right());

        result = Stream.<Integer> of(null, 3, 1, null, 2).collect(Collectors.minMaxOrElseThrow());
        assertEquals(Pair.of(1, 3), result);

        result = Stream.<Integer> of(null, null).collect(Collectors.minMaxOrElseThrow());
        assertEquals(Pair.of(null, null), result);
    }

    @Test
    public void testMinMaxOrElseThrow_Empty() {
        assertThrows(NoSuchElementException.class, () -> Stream.<Integer> empty().collect(Collectors.minMaxOrElseThrow()));
    }

    @Test
    public void testMinMaxOrElseThrow_WithComparator() {
        Pair<String, String> result = stringList.stream().collect(Collectors.minMaxOrElseThrow(Comparator.comparingInt(String::length)));
        assertEquals("date", result.left());
        assertEquals("elderberry", result.right());
    }

    @Test
    public void testMinMaxOrElseThrowWithComparator() {
        Pair<String, String> result = Stream.of("apple", "pie", "banana").collect(Collectors.minMaxOrElseThrow(Comparator.comparing(String::length)));

        Assertions.assertEquals("pie", result.left());
        Assertions.assertEquals("banana", result.right());
    }

    // minOrElse

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
    public void testMinOrElseWithComparator() {
        String min = Stream.of("apple", "pie", "banana").collect(Collectors.minOrElse(Comparator.comparing(String::length), "default"));

        Assertions.assertEquals("pie", min);
    }

    // minOrElseGet

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
    public void testMinOrElseGetWithComparator() {
        String min = Stream.of("apple", "pie", "banana").collect(Collectors.minOrElseGet(Comparator.comparing(String::length), () -> "default"));

        Assertions.assertEquals("pie", min);
    }

    // minOrElseThrow

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

    // onlyOne

    @Test
    public void testOnlyOne() {
        Optional<String> result = Arrays.asList("single").stream().collect(Collectors.onlyOne());
        assertTrue(result.isPresent());
        assertEquals("single", result.get());
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
    public void testOnlyOne_Empty() {
        Optional<String> result = Stream.<String> empty().collect(Collectors.onlyOne());
        assertFalse(result.isPresent());
    }



    /**
     * Bug fix regression: previously {@code onlyOne()} used Optional.empty() as a sentinel for
     * "not yet seen", which collapses with the result of seeing a single null element. As a
     * result, a stream containing two elements where the first is null silently returned the
     * second element instead of throwing TooManyElementsException.
     */
    @Test
    public void testOnlyOne_FirstElementNull_DetectsDuplicate() {
        // First element is null, second is non-null -> must throw, not silently return 5.
        assertThrows(TooManyElementsException.class, () -> Stream.of((Integer) null, 5).collect(Collectors.onlyOne()));

        // Both null also counts as duplicate.
        assertThrows(TooManyElementsException.class, () -> Stream.of((Integer) null, (Integer) null).collect(Collectors.onlyOne()));

        assertThrows(NullPointerException.class, () -> Stream.<Integer> of((Integer) null).collect(Collectors.onlyOne()));
    }

    @Test
    public void testOnlyOne_MultipleElements_ThrowsException() {
        final TooManyElementsException e = assertThrows(TooManyElementsException.class, () -> stringList.stream().collect(Collectors.onlyOne()));
        assertEquals("More than one element", e.getMessage());
    }

    @Test
    public void testOnlyOne_WithPredicate() {
        Optional<Person> result = personList.stream().collect(Collectors.onlyOne(p -> p.getAge() == 25));
        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().getName());
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
    public void testOnlyOneWithPredicate() {
        Optional<Integer> result = Stream.of(1, 2, 3, 4).collect(Collectors.onlyOne(n -> n > 3));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.get());
    }

    // reducing

    @Test
    public void testReducing_WithIdentity() {
        Integer result = integerList.stream().collect(Collectors.reducing(0, Integer::sum));
        assertEquals(15, result);
    }

    @Test
    public void testReducing_WithMapper() {
        Integer result = stringList.stream().collect(Collectors.reducing(0, String::length, Integer::sum));
        assertEquals(31, result);
    }

    @Test
    public void testReducing_WithoutIdentity() {
        Optional<Integer> result = integerList.stream().collect(Collectors.reducing(Integer::sum));
        assertTrue(result.isPresent());
        assertEquals(15, result.get());
    }

    @Test
    public void testReducingCollectorsRetainEncounterOrderRequirements() {
        assertFalse(Collectors.reducing(0, Integer::sum).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.<Integer> reducing(Integer::sum).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.reducingOrElseGet(Integer::sum, () -> 0).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.reducingOrElseThrow(Integer::sum).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.reducingOrElseThrow(Integer::sum, IllegalStateException::new).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.reducing(0, String::length, Integer::sum).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.<String, Integer> reducing(String::length, Integer::sum).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.reducingOrElseGet(String::length, Integer::sum, () -> 0).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.reducingOrElseThrow(String::length, Integer::sum).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(
                Collectors.reducingOrElseThrow(String::length, Integer::sum, IllegalStateException::new).characteristics().contains(Characteristics.UNORDERED));

        String expected = java.util.stream.IntStream.range(0, 500).mapToObj(String::valueOf).collect(java.util.stream.Collectors.joining());
        String actual = java.util.stream.IntStream.range(0, 500).mapToObj(String::valueOf).parallel().collect(Collectors.reducing("", String::concat));
        assertEquals(expected, actual);
    }

    @Test
    public void testReducingWithIdentity() {
        Integer result = Stream.of(1, 2, 3, 4).collect(Collectors.reducing(0, Integer::sum));

        Assertions.assertEquals(10, result);
    }

    @Test
    public void testReducingWithIdentityMapperAndOperator() {
        int result = Stream.of("a", "bb", "ccc").collect(Collectors.reducing(0, String::length, Integer::sum));
        assertEquals(6, result);
    }

    @Test
    public void testReducingWithMapperAndIdentity() {
        Integer sumOfLengths = Stream.of("a", "bb", "ccc").collect(Collectors.reducing(0, String::length, Integer::sum));

        Assertions.assertEquals(6, sumOfLengths);
    }

    @Test
    public void testReducingWithMapperOptional() {
        Integer result = stringList.stream().map(String::length).collect(Collectors.reducing(0, Integer::sum));
        assertEquals(31, result);
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
    public void testReducingWithoutIdentity() {
        Optional<Integer> result = Stream.of(1, 2, 3, 4).collect(Collectors.reducing(Integer::sum));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(10, result.get());

        Optional<Integer> empty = Stream.<Integer> empty().collect(Collectors.reducing(Integer::sum));
        Assertions.assertFalse(empty.isPresent());
    }

    // reducingOrElseGet

    @Test
    public void testReducingOrElseGet() {
        Integer result = integerList.stream().collect(Collectors.reducingOrElseGet(Integer::sum, () -> 0));
        assertEquals(15, result);
    }

    @Test
    public void testReducingOrElseGet_WithMapper() {
        Integer result = stringList.stream().collect(Collectors.reducingOrElseGet(String::length, Integer::sum, () -> 0));
        assertEquals(31, result);
    }

    @Test
    public void testReducingOrElseGetWithMapper() {
        Integer result = Stream.of("a", "bb", "ccc").collect(Collectors.reducingOrElseGet(String::length, Integer::sum, () -> 0));

        Assertions.assertEquals(6, result);
    }

    // reducingOrElseThrow

    @Test
    public void testReducingOrElseThrow() {
        Integer result = Stream.of(1, 2, 3, 4).collect(Collectors.reducingOrElseThrow(Integer::sum));

        Assertions.assertEquals(10, result);

        Assertions.assertThrows(NoSuchElementException.class, () -> Stream.<Integer> empty().collect(Collectors.reducingOrElseThrow(Integer::sum)));
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
    public void testReducingOrElseThrowWithMapper() {
        Integer result = Stream.of("a", "bb", "ccc").collect(Collectors.reducingOrElseThrow(String::length, Integer::sum));

        Assertions.assertEquals(6, result);
    }

    @Test
    public void testReducingOrElseThrowWithMapperAndExceptionSupplier() {
        assertThrows(IllegalStateException.class,
                () -> Stream.<String> empty().collect(Collectors.reducingOrElseThrow(String::length, Integer::sum, () -> new IllegalStateException("empty"))));
    }

    @Test
    public void testReducingOrElseThrowWithMapperAndSupplier() {
        Integer result = Stream.of("a", "bb", "ccc")
                .collect(Collectors.reducingOrElseThrow(String::length, Integer::sum, () -> new IllegalStateException("Empty")));

        Assertions.assertEquals(6, result);
    }

    @Test
    public void testReducingOrElseThrowWithSupplier() {
        Integer result = Stream.of(1, 2, 3, 4).collect(Collectors.reducingOrElseThrow(Integer::sum, () -> new IllegalStateException("Empty")));

        Assertions.assertEquals(10, result);

        Assertions.assertThrows(IllegalStateException.class,
                () -> Stream.<Integer> empty().collect(Collectors.reducingOrElseThrow(Integer::sum, () -> new IllegalStateException("Empty"))));
    }

    @Test
    public void testReducingWithMapper() {
        Optional<Integer> result = Stream.of("a", "bb", "ccc").collect(Collectors.<String, Integer> reducing(e -> e.length(), Integer::sum));

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(6, result.get());
    }

    // stream

    @Test
    public void testMaxAlll_WithComparatorAndDownstream() {
        Optional<Pair<String, Long>> result = stringList.stream()
                .collect(Collectors.maxAllWith(Comparator.comparingInt(String::length), Collectors.counting()));
        assertTrue(result.isPresent());
        assertEquals("elderberry", result.get().left());
        assertEquals(1L, result.get().right());
    }

    @Test
    public void testMaxAlll_WithDownstream() {
        Optional<Pair<Integer, Long>> result = integerList.stream().collect(Collectors.maxAllWith(Collectors.counting()));
        assertTrue(result.isPresent());
        assertEquals(5, result.get().left());
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
    public void testMinAlll_WithDownstream() {
        Optional<Pair<Integer, Long>> result = integerList.stream().collect(Collectors.minAllWith(Collectors.counting()));
        assertTrue(result.isPresent());
        assertEquals(1, result.get().left());
        assertEquals(1L, result.get().right());
    }

    @Test
    public void testSummarizingBigDecimal() {
        BigDecimalSummaryStatistics result = personList.stream().collect(Collectors.summarizingBigDecimal(Person::getBigDecValue));
        assertEquals(5, result.getCount());
        assertEquals(BigDecimal.valueOf(100.5), result.getMin());
        assertEquals(BigDecimal.valueOf(500.5), result.getMax());
    }

    @Test
    public void testSummarizingBigInteger() {
        BigIntegerSummaryStatistics result = personList.stream().collect(Collectors.summarizingBigInteger(Person::getBigIntValue));
        assertEquals(5, result.getCount());
        assertEquals(BigInteger.valueOf(100), result.getMin());
        assertEquals(BigInteger.valueOf(500), result.getMax());
    }

    @Test
    public void testSummarizingByte() {
        ByteSummaryStatistics result = byteList.stream().collect(Collectors.summarizingByte(b -> b));
        assertEquals(5, result.getCount());
        assertEquals((byte) 1, result.getMin());
        assertEquals((byte) 5, result.getMax());
    }

    @Test
    public void testSummarizingChar() {
        CharSummaryStatistics result = charList.stream().collect(Collectors.summarizingChar(c -> c));
        assertEquals(5, result.getCount());
        assertEquals('a', result.getMin());
        assertEquals('e', result.getMax());
    }

    @Test
    public void testSummarizingDouble() {
        DoubleSummaryStatistics result = doubleList.stream().collect(Collectors.summarizingDouble(d -> d));
        assertEquals(5, result.getCount());
        assertEquals(1.1, result.getMin(), 0.01);
        assertEquals(5.5, result.getMax(), 0.01);
    }

    @Test
    public void testSummarizingFloat() {
        FloatSummaryStatistics result = floatList.stream().collect(Collectors.summarizingFloat(f -> f));
        assertEquals(5, result.getCount());
        assertEquals(1.1f, result.getMin(), 0.01);
        assertEquals(5.5f, result.getMax(), 0.01);
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
    public void testSummarizingShort() {
        ShortSummaryStatistics result = shortList.stream().collect(Collectors.summarizingShort(s -> s));
        assertEquals(5, result.getCount());
        assertEquals((short) 10, result.getMin());
        assertEquals((short) 50, result.getMax());
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
    public void testSummingBigDecimal() {
        BigDecimal result = personList.stream().collect(Collectors.summingBigDecimal(Person::getBigDecValue));
        assertEquals(BigDecimal.valueOf(1502.5), result);
    }

    @Test
    public void testSummingBigDecimal_Parallel_CombinerUsed() {
        // Exercises the SummingBigDecimal_Combiner in parallel
        BigDecimal result = personList.parallelStream().collect(Collectors.summingBigDecimal(Person::getBigDecValue));
        assertNotNull(result);
        assertEquals(new BigDecimal("1502.5"), result);
    }

    // summingBigInteger

    @Test
    public void testSummingBigInteger() {
        BigInteger result = personList.stream().collect(Collectors.summingBigInteger(Person::getBigIntValue));
        assertEquals(BigInteger.valueOf(1500), result);
    }

    @Test
    public void testSummingBigInteger_3Mappers_Parallel_CombinerUsed() {
        // Exercises the SummingBigInteger_Combiner_3 lambda in parallel
        Tuple3<BigInteger, BigInteger, BigInteger> result = personList.parallelStream()
                .collect(MoreCollectors.summingBigInteger(Person::getBigIntValue, p -> p.getBigIntValue().multiply(BigInteger.TWO),
                        p -> p.getBigIntValue().multiply(BigInteger.valueOf(3))));
        assertNotNull(result);
        assertEquals(new BigInteger("1500"), result._1);
        assertEquals(new BigInteger("3000"), result._2);
    }

    @Test
    public void testSummingBigInteger_Parallel_CombinerUsed() {
        // Exercises the SummingBigInteger_Combiner in parallel
        BigInteger result = personList.parallelStream().collect(Collectors.summingBigInteger(Person::getBigIntValue));
        assertNotNull(result);
        assertEquals(new BigInteger("1500"), result);
    }

    @Test
    public void testSummingDouble() {
        Double result = doubleList.stream().collect(Collectors.summingDouble(Double::doubleValue));
        assertEquals(16.5, result, 0.01);
    }

    // summingInt

    @Test
    public void testSummingInt() {
        Integer result = personList.stream().collect(Collectors.summingInt(Person::getAge));
        assertEquals(150, result);
    }

    @Test
    public void testSummingInt_3Mappers_Parallel_CombinerUsed() {
        // Exercises the SummingInt_Combiner_3 lambda in parallel
        Tuple3<Integer, Integer, Integer> result = personList.parallelStream()
                .collect(MoreCollectors.summingInt(Person::getAge, p -> p.getAge() * 2, p -> p.getAge() * 3));
        assertNotNull(result);
        // sum of ages: 25+30+35+28+32=150
        assertEquals(150, (int) result._1);
        assertEquals(300, (int) result._2);
        assertEquals(450, (int) result._3);
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
    public void testSummingInt_Parallel_CombinerUsed() {
        // Exercises the SummingInt_Combiner lambda in parallel
        Integer result = integerList.parallelStream().collect(Collectors.summingInt(i -> i));
        assertEquals(15, (int) result);
    }

    @Test
    public void testSummingIntToLong() {
        Long result = personList.stream().collect(Collectors.summingIntToLong(Person::getAge));
        assertEquals(150L, result);
    }

    @Test
    public void testSummingIntToLong_3Mappers_Parallel_CombinerUsed() {
        // Exercises the SummingIntToLong_Combiner_3 lambda in parallel (ages: 25+30+35+28+32=150)
        Tuple3<Long, Long, Long> result = personList.parallelStream()
                .collect(MoreCollectors.summingIntToLong(Person::getAge, p -> p.getAge() * 2, p -> p.getAge() * 3));
        assertNotNull(result);
        assertEquals(150L, (long) result._1);
        assertEquals(300L, (long) result._2);
        assertEquals(450L, (long) result._3);
    }

    // summingLong

    @Test
    public void testSummingLong() {
        Long result = longList.stream().collect(Collectors.summingLong(Long::longValue));
        assertEquals(1500L, result);
    }

    @Test
    public void testSummingLong_3Mappers_Parallel_CombinerUsed() {
        // Exercises the SummingLong_Combiner_3 lambda in parallel
        Tuple3<Long, Long, Long> result = longList.parallelStream().collect(MoreCollectors.summingLong(Long::longValue, l -> l * 2, l -> l * 3));
        assertNotNull(result);
        assertEquals(1500L, (long) result._1);
        assertEquals(3000L, (long) result._2);
        assertEquals(4500L, (long) result._3);
    }

    @Test
    public void testSummingLong_Parallel_CombinerUsed() {
        // Exercises the SummingLong combiner in parallel
        Long result = longList.parallelStream().collect(Collectors.summingLong(Long::longValue));
        assertEquals(1500L, (long) result);
    }

}
