package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.stream.Collectors.MoreCollectors;
import com.landawn.abacus.util.u.Optional;

/** The combiner must preserve every aggregate column, even when either partition is empty. */
class CollectorPartitionContractTest extends TestBase {
    static java.util.stream.Stream<Arguments> aggregations() {
        return java.util.stream.Stream.of(
                Arguments.of("summingInt/2", MoreCollectors.summingInt((Integer v) -> v, (Integer v) -> 2 * v), Tuple.of(10, 20), Tuple.of(0, 0)),
                Arguments.of("summingInt/3", MoreCollectors.summingInt((Integer v) -> v, (Integer v) -> 2 * v, (Integer v) -> -1 * v), Tuple.of(10, 20, -10),
                        Tuple.of(0, 0, 0)),
                Arguments.of("summingIntToLong/2", MoreCollectors.summingIntToLong((Integer v) -> v, (Integer v) -> 2 * v), Tuple.of(10L, 20L),
                        Tuple.of(0L, 0L)),
                Arguments.of("summingIntToLong/3", MoreCollectors.summingIntToLong((Integer v) -> v, (Integer v) -> 2 * v, (Integer v) -> -1 * v),
                        Tuple.of(10L, 20L, -10L), Tuple.of(0L, 0L, 0L)),
                Arguments.of("summingLong/2", MoreCollectors.summingLong((Integer v) -> v, (Integer v) -> 2 * v), Tuple.of(10L, 20L), Tuple.of(0L, 0L)),
                Arguments.of("summingLong/3", MoreCollectors.summingLong((Integer v) -> v, (Integer v) -> 2 * v, (Integer v) -> -1 * v),
                        Tuple.of(10L, 20L, -10L), Tuple.of(0L, 0L, 0L)),
                Arguments.of("summingDouble/2", MoreCollectors.summingDouble((Integer v) -> v, (Integer v) -> 2 * v), Tuple.of(10.0, 20.0), Tuple.of(0.0, 0.0)),
                Arguments.of("summingDouble/3", MoreCollectors.summingDouble((Integer v) -> v, (Integer v) -> 2 * v, (Integer v) -> -1 * v),
                        Tuple.of(10.0, 20.0, -10.0), Tuple.of(0.0, 0.0, 0.0)),
                Arguments.of("summingBigInteger/2",
                        MoreCollectors.summingBigInteger((Integer v) -> BigInteger.valueOf(v), (Integer v) -> BigInteger.valueOf(2 * v)),
                        Tuple.of(BigInteger.valueOf(10), BigInteger.valueOf(20)), Tuple.of(BigInteger.ZERO, BigInteger.ZERO)),
                Arguments.of("summingBigInteger/3",
                        MoreCollectors.summingBigInteger((Integer v) -> BigInteger.valueOf(v), (Integer v) -> BigInteger.valueOf(2 * v),
                                (Integer v) -> BigInteger.valueOf(-1 * v)),
                        Tuple.of(BigInteger.valueOf(10), BigInteger.valueOf(20), BigInteger.valueOf(-10)),
                        Tuple.of(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO)),
                Arguments.of("summingBigDecimal/2",
                        MoreCollectors.summingBigDecimal((Integer v) -> BigDecimal.valueOf(v), (Integer v) -> BigDecimal.valueOf(2 * v)),
                        Tuple.of(BigDecimal.valueOf(10), BigDecimal.valueOf(20)), Tuple.of(BigDecimal.ZERO, BigDecimal.ZERO)),
                Arguments.of("summingBigDecimal/3",
                        MoreCollectors.summingBigDecimal((Integer v) -> BigDecimal.valueOf(v), (Integer v) -> BigDecimal.valueOf(2 * v),
                                (Integer v) -> BigDecimal.valueOf(-1 * v)),
                        Tuple.of(BigDecimal.valueOf(10), BigDecimal.valueOf(20), BigDecimal.valueOf(-10)),
                        Tuple.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)),
                Arguments.of("averagingInt/2", MoreCollectors.averagingInt((Integer v) -> v, (Integer v) -> 2 * v), Tuple.of(2.5, 5.0), Tuple.of(0.0, 0.0)),
                Arguments.of("averagingInt/3", MoreCollectors.averagingInt((Integer v) -> v, (Integer v) -> 2 * v, (Integer v) -> -1 * v),
                        Tuple.of(2.5, 5.0, -2.5), Tuple.of(0.0, 0.0, 0.0)),
                Arguments.of("averagingLong/2", MoreCollectors.averagingLong((Integer v) -> v, (Integer v) -> 2 * v), Tuple.of(2.5, 5.0), Tuple.of(0.0, 0.0)),
                Arguments.of("averagingLong/3", MoreCollectors.averagingLong((Integer v) -> v, (Integer v) -> 2 * v, (Integer v) -> -1 * v),
                        Tuple.of(2.5, 5.0, -2.5), Tuple.of(0.0, 0.0, 0.0)),
                Arguments.of("averagingDouble/2", MoreCollectors.averagingDouble((Integer v) -> v, (Integer v) -> 2 * v), Tuple.of(2.5, 5.0),
                        Tuple.of(0.0, 0.0)),
                Arguments.of("averagingDouble/3", MoreCollectors.averagingDouble((Integer v) -> v, (Integer v) -> 2 * v, (Integer v) -> -1 * v),
                        Tuple.of(2.5, 5.0, -2.5), Tuple.of(0.0, 0.0, 0.0)),
                Arguments.of("averagingBigInteger/2",
                        MoreCollectors.averagingBigInteger((Integer v) -> BigInteger.valueOf(v), (Integer v) -> BigInteger.valueOf(2 * v)),
                        Tuple.of(new BigDecimal("2.5"), new BigDecimal("5")), Tuple.of(BigDecimal.ZERO, BigDecimal.ZERO)),
                Arguments.of("averagingBigInteger/3",
                        MoreCollectors.averagingBigInteger((Integer v) -> BigInteger.valueOf(v), (Integer v) -> BigInteger.valueOf(2 * v),
                                (Integer v) -> BigInteger.valueOf(-1 * v)),
                        Tuple.of(new BigDecimal("2.5"), new BigDecimal("5"), new BigDecimal("-2.5")),
                        Tuple.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)),
                Arguments.of("averagingBigDecimal/2",
                        MoreCollectors.averagingBigDecimal((Integer v) -> BigDecimal.valueOf(v), (Integer v) -> BigDecimal.valueOf(2 * v)),
                        Tuple.of(new BigDecimal("2.5"), new BigDecimal("5")), Tuple.of(BigDecimal.ZERO, BigDecimal.ZERO)),
                Arguments.of("averagingBigDecimal/3",
                        MoreCollectors.averagingBigDecimal((Integer v) -> BigDecimal.valueOf(v), (Integer v) -> BigDecimal.valueOf(2 * v),
                                (Integer v) -> BigDecimal.valueOf(-1 * v)),
                        Tuple.of(new BigDecimal("2.5"), new BigDecimal("5"), new BigDecimal("-2.5")),
                        Tuple.of(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("aggregations")
    <A, R> void partitionedAggregationMatchesExpectedTotals(String name, Collector<Integer, A, R> collector, R expected, R empty) {
        List<Integer> values = List.of(1, 2, 3, 4);
        for (int split = 0; split <= values.size(); split++) {
            assertEquals(expected, combine(collector, values.subList(0, split), values.subList(split, values.size())), name + ": split=" + split);
        }
        assertEquals(empty, combine(collector, List.of(), List.of()));
        assertEquals(expected, values.parallelStream().collect(collector));
    }

    @Test
    void minMaxCombinesEmptyAndNonemptyPartitionsInEitherOrder() {
        for (List<Integer> values : List.of(List.of(4, 1, 3, 2), List.of(1, 4, 2, 3), List.of(2, 2, 2, 2))) {
            Optional<Pair<Integer, Integer>> expected = Optional
                    .of(Pair.of(values.stream().min(Integer::compare).orElseThrow(), values.stream().max(Integer::compare).orElseThrow()));
            for (int split = 0; split <= values.size(); split++) {
                assertEquals(expected, combine(Collectors.<Integer> minMax(), values.subList(0, split), values.subList(split, values.size())));
            }
        }
        assertEquals(Optional.empty(), combine(Collectors.<Integer> minMax(), List.of(), List.of()));
    }

    @Test
    void fallbackDependsOnBothPartitionsBeingEmpty() {
        Collector<Integer, ?, Integer> collector = Collectors.collectingOrElseGetIfEmpty(Collectors.countingToInt(), () -> -1);
        assertEquals(-1, combine(collector, List.of(), List.of()));
        assertEquals(2, combine(collector, List.of(1, 2), List.of()));
        assertEquals(2, combine(collector, List.of(), List.of(1, 2)));
        assertEquals(3, combine(collector, List.of(1), List.of(2, 3)));
    }

    private static <A, R> R combine(Collector<Integer, A, R> collector, List<Integer> first, List<Integer> second) {
        A left = collector.supplier().get();
        A right = collector.supplier().get();
        first.forEach(value -> collector.accumulator().accept(left, value));
        second.forEach(value -> collector.accumulator().accept(right, value));
        return collector.finisher().apply(collector.combiner().apply(left, right));
    }
}
