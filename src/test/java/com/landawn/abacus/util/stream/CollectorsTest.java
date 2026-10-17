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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.Tuple.Tuple6;
import com.landawn.abacus.util.Tuple.Tuple7;
import com.landawn.abacus.util.stream.Collectors.MoreCollectors;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;

/** Collectors general contracts. */
public class CollectorsTest extends CollectorsTestSupport {

    // averagingBigDecimal

    @Test
    public void testMoreCollectors_AveragingBigDecimal_2Mappers() {
        Tuple2<BigDecimal, BigDecimal> result = personList.stream()
                .collect(MoreCollectors.averagingBigDecimal(Person::getBigDecValue, p -> p.getBigDecValue().multiply(BigDecimal.valueOf(2))));
        assertTrue(result._1.compareTo(BigDecimal.valueOf(300.5)) == 0);
        assertTrue(result._2.compareTo(BigDecimal.valueOf(601.0)) == 0);
    }

    @Test
    public void testMoreCollectors_AveragingBigDecimal_2Mappers_Parallel() {
        Tuple2<BigDecimal, BigDecimal> result = personList.parallelStream()
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
    public void testMoreCollectors_AveragingBigDecimal_3Mappers_Parallel() {
        Tuple3<BigDecimal, BigDecimal, BigDecimal> result = personList.parallelStream()
                .collect(MoreCollectors.averagingBigDecimal(Person::getBigDecValue, p -> p.getBigDecValue().multiply(BigDecimal.valueOf(2)),
                        p -> p.getBigDecValue().multiply(BigDecimal.valueOf(3))));
        assertTrue(result._1.compareTo(BigDecimal.valueOf(300.5)) == 0);
        assertTrue(result._2.compareTo(BigDecimal.valueOf(601.0)) == 0);
        assertTrue(result._3.compareTo(BigDecimal.valueOf(901.5)) == 0);
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
    public void testMoreCollectorsAveragingBigDecimal3_Parallel() {
        Tuple3<BigDecimal, BigDecimal, BigDecimal> result = Arrays
                .asList(new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0"), new BigDecimal("4.0"))
                .parallelStream()
                .collect(MoreCollectors.averagingBigDecimal(bd -> bd, bd -> bd.multiply(new BigDecimal("2")), bd -> bd.multiply(new BigDecimal("4"))));

        assertEquals(0, new BigDecimal("2.5").compareTo(result._1));
        assertEquals(0, new BigDecimal("5").compareTo(result._2));
        assertEquals(0, new BigDecimal("10").compareTo(result._3));
    }

    // averagingBigInteger

    @Test
    public void testMoreCollectors_AveragingBigInteger_2Mappers() {
        Tuple2<BigDecimal, BigDecimal> result = personList.stream()
                .collect(MoreCollectors.averagingBigInteger(Person::getBigIntValue, p -> p.getBigIntValue().multiply(BigInteger.TWO)));
        assertEquals(BigDecimal.valueOf(300), result._1);
        assertEquals(BigDecimal.valueOf(600), result._2);
    }

    @Test
    public void testMoreCollectors_AveragingBigInteger_2Mappers_Parallel() {
        Tuple2<BigDecimal, BigDecimal> result = personList.parallelStream()
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
    public void testMoreCollectors_AveragingBigInteger_3Mappers_Parallel() {
        Tuple3<BigDecimal, BigDecimal, BigDecimal> result = personList.parallelStream()
                .collect(MoreCollectors.averagingBigInteger(Person::getBigIntValue, p -> p.getBigIntValue().multiply(BigInteger.TWO),
                        p -> p.getBigIntValue().multiply(BigInteger.valueOf(3))));
        assertEquals(BigDecimal.valueOf(300), result._1);
        assertEquals(BigDecimal.valueOf(600), result._2);
        assertEquals(BigDecimal.valueOf(900), result._3);
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
    public void testMoreCollectorsAveragingBigInteger3_Parallel() {
        Tuple3<BigDecimal, BigDecimal, BigDecimal> result = Arrays
                .asList(BigInteger.valueOf(2), BigInteger.valueOf(4), BigInteger.valueOf(6), BigInteger.valueOf(8))
                .parallelStream()
                .collect(MoreCollectors.averagingBigInteger(bi -> bi, bi -> bi.multiply(BigInteger.TEN), bi -> bi.multiply(BigInteger.valueOf(100))));

        assertEquals(0, new BigDecimal("5").compareTo(result._1));
        assertEquals(0, new BigDecimal("50").compareTo(result._2));
        assertEquals(0, new BigDecimal("500").compareTo(result._3));
    }

    // averagingDouble

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
    public void testMoreCollectorsAveragingDouble2() {
        Tuple2<Double, Double> result = Arrays.asList(1.0, 2.0, 3.0).stream().collect(MoreCollectors.averagingDouble(d -> d, d -> d * 2));

        assertEquals(2.0, result._1, 0.01);
        assertEquals(4.0, result._2, 0.01);
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

    // averagingInt

    @Test
    public void testMoreCollectors_AveragingInt_2Mappers() {
        Tuple2<Double, Double> result = personList.stream().collect(MoreCollectors.averagingInt(Person::getAge, p -> p.getAge() * 2));
        assertEquals(30.0, result._1, 0.01);
        assertEquals(60.0, result._2, 0.01);
    }

    @Test
    public void testMoreCollectors_AveragingInt_2Mappers_Parallel() {
        Tuple2<Double, Double> result = personList.parallelStream().collect(MoreCollectors.averagingInt(Person::getAge, p -> p.getAge() * 2));
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
    public void testMoreCollectors_AveragingInt_3Mappers_Parallel() {
        Tuple3<Double, Double, Double> result = personList.parallelStream()
                .collect(MoreCollectors.averagingInt(Person::getAge, p -> p.getAge() * 2, p -> p.getAge() * 3));
        assertEquals(30.0, result._1, 0.01);
        assertEquals(60.0, result._2, 0.01);
        assertEquals(90.0, result._3, 0.01);
    }

    @Test
    public void testMoreCollectorsAveragingInt2() {
        Tuple2<Double, Double> result = integerList.stream().collect(MoreCollectors.averagingInt(i -> i, i -> i * 2));

        assertEquals(3.0, result._1, 0.01);
        assertEquals(6.0, result._2, 0.01);
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

    // averagingLong

    @Test
    public void testMoreCollectors_AveragingLong_2Mappers() {
        Tuple2<Double, Double> result = longList.stream().collect(MoreCollectors.averagingLong(Long::longValue, l -> l * 2));
        assertEquals(300.0, result._1, 0.01);
        assertEquals(600.0, result._2, 0.01);
    }

    @Test
    public void testMoreCollectors_AveragingLong_2Mappers_Parallel() {
        Tuple2<Double, Double> result = longList.parallelStream().collect(MoreCollectors.averagingLong(Long::longValue, l -> l * 2));
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
    public void testMoreCollectors_AveragingLong_3Mappers_Parallel() {
        Tuple3<Double, Double, Double> result = longList.parallelStream().collect(MoreCollectors.averagingLong(Long::longValue, l -> l * 2, l -> l * 3));
        assertEquals(300.0, result._1, 0.01);
        assertEquals(600.0, result._2, 0.01);
        assertEquals(900.0, result._3, 0.01);
    }

    @Test
    public void testMoreCollectorsAveragingLong2() {
        Tuple2<Double, Double> result = Arrays.asList(1L, 2L, 3L).stream().collect(MoreCollectors.averagingLong(l -> l, l -> l * 10));

        assertEquals(2.0, result._1, 0.01);
        assertEquals(20.0, result._2, 0.01);
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
    public void testMoreCollectorsCollectionCombineIntersectsEveryCharacteristicSet() {
        final List<Collector<? super Integer, ?, ?>> components = new ArrayList<>();
        components.add(Collectors.toList());
        components.add(Collectors.toSet());

        final Collector<Integer, ?, Object[]> combined = MoreCollectors.combine(components, values -> values);
        assertFalse(combined.characteristics().contains(Characteristics.UNORDERED));
        assertFalse(combined.characteristics().contains(Characteristics.IDENTITY_FINISH));

        final Object[] result = java.util.stream.IntStream.range(0, 1000).boxed().parallel().collect(combined);
        assertEquals(java.util.stream.IntStream.range(0, 1000).boxed().toList(), result[0]);
        assertEquals(1000, ((Set<?>) result[1]).size());
    }

    // collect

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
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append(s.toUpperCase());
        }, (sb1, sb2) -> {
            if (sb1.length() > 0 && sb2.length() > 0) {
                sb1.append("|");
            }
            sb1.append(sb2);
            return sb1;
        }, StringBuilder::toString);

        String result = stringList.stream().collect(customJoiner);
        assertEquals("APPLE|BANANA|CHERRY|DATE|ELDERBERRY", result);
    }

    @Test
    public void testMoreCollectors_Combine_2Collectors() {
        Tuple2<Long, Integer> result = stringList.stream().collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(String::length)));
        assertEquals(5L, result._1);
        assertEquals(31, result._2);
    }

    @Test
    public void testMoreCollectors_Combine_2CollectorsWithFinisher() {
        String result = stringList.stream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(String::length), (count, sum) -> count + ":" + sum));
        assertEquals("5:31", result);
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
    public void testMoreCollectors_Combine_3CollectorsWithFinisher() {
        String result = stringList.stream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(String::length), Collectors.first(),
                        (count, sum, first) -> count + ":" + sum + ":" + first.orElse("none")));
        assertEquals("5:31:apple", result);
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
    public void testMoreCollectors_Combine_4CollectorsWithFinisher() {
        String result = stringList.stream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(String::length), Collectors.first(), Collectors.last(),
                        (count, sum, first, last) -> count + ":" + sum + ":" + first.orElse("none") + ":" + last.orElse("none")));
        assertEquals("5:31:apple:elderberry", result);
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
    public void testMoreCollectors_Combine_5Collectors_Parallel() {
        Tuple5<Long, Integer, Long, Integer, Long> result = stringList.parallelStream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(String::length), Collectors.counting(), Collectors.countingToInt(),
                        Collectors.counting()));
        assertEquals(5L, result._1);
        assertEquals(31, result._2);
        assertEquals(5L, result._3);
        assertEquals(5, result._4);
        assertEquals(5L, result._5);
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
    public void testMoreCollectors_Combine_6Collectors_Parallel() {
        Tuple6<Long, Integer, Long, Integer, Long, Integer> result = stringList.parallelStream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(String::length), Collectors.counting(), Collectors.countingToInt(),
                        Collectors.counting(), Collectors.countingToInt()));
        assertEquals(5L, result._1);
        assertEquals(31, result._2);
        assertEquals(5L, result._3);
        assertEquals(5, result._4);
        assertEquals(5L, result._5);
        assertEquals(5, result._6);
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
    public void testMoreCollectors_Combine_7Collectors_Parallel() {
        Tuple7<Long, Integer, Long, Integer, Long, Integer, Long> result = stringList.parallelStream()
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(String::length), Collectors.counting(), Collectors.countingToInt(),
                        Collectors.counting(), Collectors.countingToInt(), Collectors.counting()));
        assertEquals(5L, result._1);
        assertEquals(31, result._2);
        assertEquals(5L, result._3);
        assertEquals(5, result._4);
        assertEquals(5L, result._5);
        assertEquals(5, result._6);
        assertEquals(5L, result._7);
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
    public void testMoreCollectorsCombineWithCollection() {
        List<Collector<Integer, ?, ?>> collectors = Arrays.asList(Collectors.counting(), Collectors.summingInt(i -> i), Collectors.averagingIntOrEmpty(i -> i));

        String result = integerList.stream()
                .collect(MoreCollectors.combine(collectors,
                        results -> String.format("Count: %d, Sum: %d, Avg: %.2f", results[0], results[1], ((OptionalDouble) results[2]).get())));

        assertEquals("Count: 5, Sum: 15, Avg: 3.00", result);
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
    public void testNullFunctionalArgumentsAreEvaluatedDuringCollection() {
        assertThrows(IllegalArgumentException.class,
                () -> MoreCollectors.summingInt((java.util.function.ToIntFunction<Integer>) null, (Integer value) -> value));
        assertThrows(IllegalArgumentException.class,
                () -> MoreCollectors.averagingDouble((java.util.function.ToDoubleFunction<Integer>) null, (Integer value) -> value.doubleValue()));

        final List<Collector<? super Integer, ?, ?>> downstreams = new ArrayList<>();
        downstreams.add(Collectors.counting());
        downstreams.add(null);
        assertThrows(IllegalArgumentException.class, () -> MoreCollectors.combine(downstreams, values -> values));

        assertThrows(IllegalArgumentException.class,
                () -> java.util.stream.Stream.of(1).collect(Collectors.filtering((java.util.function.Predicate<Integer>) null, Collectors.toList())));
        assertThrows(IllegalArgumentException.class,
                () -> java.util.stream.Stream.of(1).collect(Collectors.toArray((java.util.function.IntFunction<Integer[]>) null)));
        assertThrows(IllegalArgumentException.class,
                () -> java.util.stream.Stream.of(1).collect(Collectors.toMap((Function<Integer, Integer>) null, Function.identity())));
        assertThrows(IllegalArgumentException.class,
                () -> java.util.stream.Stream.of(1).collect(Collectors.partitioningBy((java.util.function.Predicate<Integer>) null)));
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
    public void testMoreCollectorsToDatasetWithColumnNames() {
        List<String> columnNames = Arrays.asList("name", "age", "department");
        Dataset result = personList.stream().collect(MoreCollectors.toDataset(columnNames));

        assertNotNull(result);
        assertEquals(personList.size(), result.size());
        assertEquals(columnNames, result.columnNames());
    }

    // combine

    @Test
    public void testMoreCollectors_Combine_CollectionWithFinisher() {
        List<Collector<? super String, ?, ?>> collectors = Arrays.asList(Collectors.counting(), Collectors.summingInt(String::length), Collectors.first());
        Collector<String, ?, String> combine = MoreCollectors.combine(collectors, a -> a[0] + ":" + a[1] + ":" + a[2]);
        String result = stringList.stream().collect(combine);
        assertEquals("5:31:Optional[apple]", result);
    }

    // Parallel tests to cover combiner lambdas in MoreCollectors.combine and averaging methods
    @Test
    public void testMoreCollectors_Combine_CollectionWithFinisher_Parallel() {
        List<Collector<? super String, ?, ?>> collectors = Arrays.asList(Collectors.counting(), Collectors.summingInt(String::length));
        Collector<String, ?, String> combine = MoreCollectors.combine(collectors, a -> a[0] + ":" + a[1]);
        String result = stringList.parallelStream().collect(combine);
        assertEquals("5:31", result);
    }

    @Test
    public void testMoreCollectorsCollectionCombineSnapshotsRotatingTraversalOrder() {
        final List<Collector<? super Integer, ?, ?>> components = new ArrayList<>();
        components.add(Collectors.counting());
        components.add(Collectors.summingInt(Integer::intValue));
        components.add(Collectors.mapping(Object::toString, Collectors.joining(",")));

        final class RotatingDownstreams extends java.util.AbstractCollection<Collector<? super Integer, ?, ?>> {
            private int traversalCount;

            @Override
            public java.util.Iterator<Collector<? super Integer, ?, ?>> iterator() {
                final int offset = traversalCount++ % components.size();
                final List<Collector<? super Integer, ?, ?>> rotated = new ArrayList<>(components.size());

                for (int i = 0; i < components.size(); i++) {
                    rotated.add(components.get((i + offset) % components.size()));
                }

                return rotated.iterator();
            }

            @Override
            public int size() {
                return components.size();
            }
        }

        final RotatingDownstreams downstreams = new RotatingDownstreams();
        final Collector<Integer, ?, Object[]> combined = MoreCollectors.combine(downstreams, values -> values);
        final Object[] result = java.util.stream.Stream.of(1, 2, 3, 4, 5, 6).parallel().collect(combined);

        assertEquals(1, downstreams.traversalCount);
        assertArrayEquals(new Object[] { 6L, 21, "1,2,3,4,5,6" }, result);
    }

    // create

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
    public void testCreate_SupplierAccumulatorCombiner() {
        Collector<String, List<String>, List<String>> collector = Collectors.create(ArrayList::new, List::add, (a, b) -> {
            a.addAll(b);
            return a;
        });

        List<String> result = stringList.stream().collect(collector);
        assertEquals(stringList, result);
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
    public void testCreateCollectionCharacteristicsRetainsNullAsEmpty() {
        final Supplier<StringBuilder> supplier = StringBuilder::new;
        final BiConsumer<StringBuilder, String> accumulator = StringBuilder::append;
        final BinaryOperator<StringBuilder> combiner = StringBuilder::append;

        assertEquals("ab",
                java.util.stream.Stream.of("a", "b")
                        .collect(Collectors.create(supplier, accumulator, combiner, (Collection<Characteristics>) null))
                        .toString());
        assertEquals("ab", java.util.stream.Stream.of("a", "b")
                .collect(Collectors.create(supplier, accumulator, combiner, StringBuilder::toString, (Collection<Characteristics>) null)));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> Collectors.create(supplier, accumulator, combiner, Arrays.asList(Characteristics.UNORDERED, null))).getMessage()
                        .contains("characteristics[1]"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> Collectors.create(supplier, accumulator, combiner, StringBuilder::toString, Arrays.asList(Characteristics.UNORDERED, null))).getMessage()
                        .contains("characteristics[1]"));
    }

    @Test
    public void testCreateRejectsNullVarargsCharacteristics() {
        final Supplier<StringBuilder> supplier = StringBuilder::new;
        final BiConsumer<StringBuilder, String> accumulator = StringBuilder::append;
        final BinaryOperator<StringBuilder> combiner = StringBuilder::append;

        assertTrue(assertThrows(IllegalArgumentException.class, () -> Collectors.create(supplier, accumulator, combiner, (Characteristics[]) null)).getMessage()
                .contains("characteristics"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Collectors.create(supplier, accumulator, combiner, Characteristics.UNORDERED, null))
                .getMessage()
                .contains("characteristics[1]"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> Collectors.create(supplier, accumulator, combiner, StringBuilder::toString, (Characteristics[]) null)).getMessage()
                        .contains("characteristics"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> Collectors.create(supplier, accumulator, combiner, StringBuilder::toString, Characteristics.UNORDERED, null)).getMessage()
                        .contains("characteristics[1]"));
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
    public void testCreateRejectsNullFunctionalArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> Collectors.<String, StringBuilder> create(null, StringBuilder::append, (left, right) -> left.append(right)));
        assertThrows(IllegalArgumentException.class,
                () -> Collectors.<String, StringBuilder> create(StringBuilder::new, null, (left, right) -> left.append(right)));
        assertThrows(IllegalArgumentException.class, () -> Collectors.<String, StringBuilder> create(StringBuilder::new, StringBuilder::append, null));
        assertThrows(IllegalArgumentException.class, () -> Collectors.toCollection(null));
    }

    @Test
    public void testEmptyAwareCollectorsStripConcurrentCharacteristic() {
        final Collector<String, ?, Optional<ConcurrentMap<String, String>>> optionalCollector = Collectors
                .collectingOrEmpty(Collectors.toConcurrentMap(Function.identity(), Function.identity(), (a, b) -> a, ConcurrentHashMap::new));
        assertFalse(optionalCollector.characteristics().contains(Characteristics.CONCURRENT));
        assertFalse(optionalCollector.characteristics().contains(Characteristics.IDENTITY_FINISH));
        assertTrue(optionalCollector.characteristics().contains(Characteristics.UNORDERED));

        final Collector<String, ?, ConcurrentMap<String, String>> defaultCollector = Collectors.collectingOrElseGetIfEmpty(
                Collectors.toConcurrentMap(Function.identity(), Function.identity(), (a, b) -> a, ConcurrentHashMap::new), ConcurrentHashMap::new);
        assertFalse(defaultCollector.characteristics().contains(Characteristics.CONCURRENT));
        assertFalse(defaultCollector.characteristics().contains(Characteristics.IDENTITY_FINISH));
        assertTrue(defaultCollector.characteristics().contains(Characteristics.UNORDERED));
    }

    @Test
    public void testErrorConditions() {

        assertThrows(IllegalStateException.class, () -> Arrays.asList("a", "b", "a").stream().collect(Collectors.toMap(Function.identity(), String::length)));

        // A null key is rejected with NullPointerException, matching Stream.groupTo and the rest of this
        // library (and java.util.stream.Collectors.groupingBy).
        assertThrows(NullPointerException.class, () -> Arrays.asList("a", null, "b").stream().collect(Collectors.groupingBy(Function.identity())));
        assertThrows(NullPointerException.class, () -> Arrays.asList("a", null, "b").stream().collect(Collectors.countingBy(Function.identity())));
        assertThrows(NullPointerException.class, () -> Arrays.asList("a", null, "b").stream().collect(Collectors.groupingByConcurrent(Function.identity())));

        assertThrows(UnsupportedOperationException.class, () -> stringList.parallelStream().collect(Collectors.first()));

        assertThrows(UnsupportedOperationException.class, () -> stringList.parallelStream().collect(Collectors.last()));
    }

    @Test
    public void testFloatingPointCollectorsRetainEncounterOrderRequirements() {
        assertFalse(Collectors.summingDouble((Integer value) -> value.doubleValue()).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.averagingDoubleOrEmpty((Integer value) -> value.doubleValue()).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.averagingDoubleOrElseThrow((Integer value) -> value.doubleValue()).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.summarizingFloat((Integer value) -> value.floatValue()).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(Collectors.summarizingDouble((Integer value) -> value.doubleValue()).characteristics().contains(Characteristics.UNORDERED));
        assertFalse(MoreCollectors.summingDouble((Integer value) -> value.doubleValue(), (Integer value) -> -value.doubleValue())
                .characteristics()
                .contains(Characteristics.UNORDERED));
        assertFalse(MoreCollectors.averagingDouble((Integer value) -> value.doubleValue(), (Integer value) -> -value.doubleValue())
                .characteristics()
                .contains(Characteristics.UNORDERED));

        // Exact integer statistics remain genuinely order-insensitive.
        assertTrue(Collectors.summarizingInt((Integer value) -> value).characteristics().contains(Characteristics.UNORDERED));
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
    public void testComparatorMinMaxDistinguishesNullWinnerFromEmptyInput() {
        final Comparator<Integer> nullsFirst = Comparator.nullsFirst(Comparator.naturalOrder());
        final Comparator<Integer> nullsLast = Comparator.nullsLast(Comparator.naturalOrder());

        final Pair<Integer, Integer> firstResult = Stream.of((Integer) null, 2, 1).collect(Collectors.minMax(nullsFirst)).get();
        assertNull(firstResult.left());
        assertEquals(Integer.valueOf(2), firstResult.right());

        final Pair<Integer, Integer> lastResult = Stream.of((Integer) null, 2, 1).collect(Collectors.minMax(nullsLast)).get();
        assertEquals(Integer.valueOf(1), lastResult.left());
        assertNull(lastResult.right());

        final Pair<Integer, Integer> allNull = Stream.of((Integer) null, (Integer) null).collect(Collectors.minMax(nullsFirst)).get();
        assertNull(allNull.left());
        assertNull(allNull.right());

        // A present null extreme must not invoke the empty-stream fallback.
        final Pair<Integer, Integer> withFallback = Stream.of((Integer) null, 2).collect(Collectors.minMaxOrElseGet(nullsFirst, () -> Pair.of(-1, -1)));
        assertNull(withFallback.left());
        assertEquals(Integer.valueOf(2), withFallback.right());
        assertFalse(Stream.<Integer> empty().collect(Collectors.minMax(nullsFirst)).isPresent());
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
    public void testOrderSensitiveMapMergeCollectorsAreNotUnordered() {
        assertFalse(Collectors
                .<String, String, String, LinkedHashMap<String, String>> toMap(Function.identity(), Function.identity(), (a, b) -> a + b, LinkedHashMap::new)
                .characteristics()
                .contains(Characteristics.UNORDERED));
        assertFalse(Collectors.<String, String, String> toBiMap(Function.identity(), Function.identity(), (a, b) -> b, BiMap::new)
                .characteristics()
                .contains(Characteristics.UNORDERED));
    }

    @Test
    public void testBasicCollectionCombiners_ParallelCollectors() {
        final List<Integer> list = java.util.stream.IntStream.range(0, 2000).boxed().parallel().collect(Collectors.toList());
        final Set<Integer> set = java.util.stream.IntStream.range(0, 2000).boxed().parallel().collect(Collectors.toSet());
        final Multiset<Integer> multiset = java.util.stream.Stream.of(1, 1, 2, 2, 2).parallel().collect(Collectors.toMultiset());

        assertEquals(2000, list.size());
        assertEquals(2000, set.size());
        assertEquals(2, multiset.count(1));
        assertEquals(3, multiset.count(2));
    }

    @Test
    public void testOrderSensitiveCollectorsPreserveEncounterOrderWithJdkParallelStreams() {
        // regression: the bigger-wins combiner appended the earlier segment AFTER the later one
        // when the right partial result was bigger, reordering JDK parallel stream results
        final List<Integer> expected = new java.util.ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            expected.add(i);
        }

        final List<Integer> list = java.util.stream.IntStream.rangeClosed(1, 1000).boxed().parallel().collect(Collectors.toList());
        assertEquals(expected, list);

        final List<Integer> linked = new java.util.ArrayList<>(
                java.util.stream.IntStream.rangeClosed(1, 1000).boxed().parallel().collect(Collectors.toLinkedHashSet()));
        assertEquals(expected, linked);

        final Integer[] arr = java.util.stream.IntStream.rangeClosed(1, 1000).boxed().parallel().collect(Collectors.toArray(Integer[]::new));
        assertArrayEquals(expected.toArray(new Integer[0]), arr);
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

    // summingBigDecimal

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
    public void testMoreCollectorsSummingBigDecimal2() {
        Tuple2<BigDecimal, BigDecimal> result = Arrays.asList(new BigDecimal("1.1"), new BigDecimal("2.2"), new BigDecimal("3.3"))
                .stream()
                .collect(MoreCollectors.summingBigDecimal(bd -> bd, bd -> bd.multiply(new BigDecimal("2"))));

        assertEquals(new BigDecimal("6.6"), result._1);
        assertEquals(new BigDecimal("13.2"), result._2);
    }

    // summingBigInteger

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
    public void testMoreCollectorsSummingBigInteger2() {
        Tuple2<BigInteger, BigInteger> result = Arrays.asList(BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3))
                .stream()
                .collect(MoreCollectors.summingBigInteger(bi -> bi, bi -> bi.multiply(BigInteger.TEN)));

        assertEquals(BigInteger.valueOf(6), result._1);
        assertEquals(BigInteger.valueOf(60), result._2);
    }

    // summingDouble

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
    public void testMoreCollectorsSummingDouble2() {
        Tuple2<Double, Double> result = doubleList.stream().collect(MoreCollectors.summingDouble(d -> d, d -> d * 2));

        assertEquals(16.5, result._1, 0.01);
        assertEquals(33.0, result._2, 0.01);
    }

    // summingInt

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

    // summingIntToLong

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
    public void testMoreCollectorsSummingIntToLong2() {
        Tuple2<Long, Long> result = integerList.stream().collect(MoreCollectors.summingIntToLong(i -> i, i -> i * 1000));

        assertEquals(15L, result._1);
        assertEquals(15000L, result._2);
    }

    // summingLong

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
    public void testMoreCollectorsSummingLong2() {
        Tuple2<Long, Long> result = Arrays.asList(1L, 2L, 3L).stream().collect(MoreCollectors.summingLong(l -> l, l -> l * 10));

        assertEquals(6L, result._1);
        assertEquals(60L, result._2);
    }

    @Test
    public void testBugFix_toCollection_negativeAtMostSizeThrows() {
        // Javadoc contract: toCollection(int atMostSize, Supplier) throws IllegalArgumentException
        // if atMostSize is negative (matching the behavior of first(int)/last(int)).
        assertThrows(IllegalArgumentException.class, () -> Collectors.toCollection(-1, ArrayList::new));
        // toList(int) / toSet(int) delegate to toCollection(int, Supplier) and share the contract.
        assertThrows(IllegalArgumentException.class, () -> Collectors.toList(-1));
        assertThrows(IllegalArgumentException.class, () -> Collectors.toSet(-5));

        // Sanity: zero is still allowed and yields an empty collection.
        List<Integer> empty = Stream.of(1, 2, 3).collect(Collectors.toCollection(0, ArrayList::new));
        assertTrue(empty.isEmpty());
    }

    // toDataset

    @Test
    public void testMoreCollectors_ToDataset() {
        Dataset result = personList.stream().collect(MoreCollectors.toDataset());
        assertNotNull(result);
        assertEquals(5, result.size());
    }

    @Test
    public void testMoreCollectors_ToDataset_NullOrEmptyColumnNames_ThrowsIAE() {
        // column names are required (consistent with Stream.toDataset / N.newDataset); validated eagerly
        assertThrows(IllegalArgumentException.class, () -> MoreCollectors.toDataset((List<String>) null));
        assertThrows(IllegalArgumentException.class, () -> MoreCollectors.toDataset(Arrays.<String> asList()));
    }

    @Test
    public void testMoreCollectorsToDataset() {
        Dataset result = personList.stream().collect(MoreCollectors.toDataset());

        assertNotNull(result);
        assertEquals(personList.size(), result.size());
    }

}
