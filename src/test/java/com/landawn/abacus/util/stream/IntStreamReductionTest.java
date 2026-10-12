package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;

/** IntStream reduction contracts. */
public class IntStreamReductionTest extends IntStreamTestSupport {

    // acceptIfNotEmpty

    @Test
    public void testAcceptIfNotEmpty() throws Exception {
        AtomicInteger sum = new AtomicInteger(0);

        createIntStream(1, 2, 3).acceptIfNotEmpty(s -> sum.set(s.sum()));
        assertEquals(6, sum.get());

        sum.set(0);
        IntStream.empty().acceptIfNotEmpty(s -> sum.set(s.sum())).orElse(() -> sum.set(-1));
        assertEquals(-1, sum.get());
    }

    @Test
    public void testAcceptIfNotEmpty_EmptyStream() {
        AtomicBoolean called = new AtomicBoolean(false);
        IntStream.empty().acceptIfNotEmpty(s -> called.set(true));
        assertFalse(called.get());
    }

    @Test
    public void testAcceptIfNotEmpty_HappyPath() {
        AtomicInteger sum = new AtomicInteger(0);
        IntStream.of(1, 2, 3).acceptIfNotEmpty(s -> sum.set(s.sum()));
        assertEquals(6, sum.get());
    }

    // allMatch

    @Test
    public void testAllMatch() {
        assertTrue(IntStream.of(2, 4, 6, 8).allMatch(n -> n % 2 == 0));
        assertFalse(IntStream.of(2, 4, 5, 8).allMatch(n -> n % 2 == 0));
    }

    @Test
    public void testAllMatch_AllMatching() {
        boolean result = IntStream.of(2, 4, 6, 8).allMatch(x -> x % 2 == 0);
        assertTrue(result);
    }

    @Test
    public void testAllMatch_EmptyStream() {
        boolean result = IntStream.empty().allMatch(x -> false);
        assertTrue(result);
    }

    @Test
    public void testAllMatch_NotAllMatching() {
        boolean result = IntStream.of(1, 2, 3, 4).allMatch(x -> x % 2 == 0);
        assertFalse(result);
    }

    @Test
    public void allMatch_shouldReturnFalseIfAnyElementDoesNotMatch() {
        assertFalse(IntStream.of(1, 2, 3).allMatch(i -> i % 2 == 0));
    }

    @Test
    public void allMatch_shouldReturnTrueIfAllElementsMatch() {
        assertTrue(IntStream.of(2, 4, 6).allMatch(i -> i % 2 == 0));
    }

    // anyMatch

    @Test
    public void testAnyMatch() {
        assertTrue(IntStream.of(1, 2, 3, 4, 5).anyMatch(n -> n > 4));
        assertFalse(IntStream.of(1, 2, 3).anyMatch(n -> n > 5));
    }

    @Test
    public void testAnyMatch_EmptyStream() {
        boolean result = IntStream.empty().anyMatch(x -> true);
        assertFalse(result);
    }

    @Test
    public void testAnyMatch_MatchFound() {
        boolean result = IntStream.of(1, 2, 3, 4, 5).anyMatch(x -> x > 3);
        assertTrue(result);
    }

    @Test
    public void testAnyMatch_NoMatch() {
        boolean result = IntStream.of(1, 2, 3).anyMatch(x -> x > 10);
        assertFalse(result);
    }

    @Test
    public void anyMatch_shouldReturnTrueIfAnyElementMatches() {
        assertTrue(IntStream.of(1, 2, 3).anyMatch(i -> i % 2 == 0));
    }

    // applyIfNotEmpty

    @Test
    public void testApplyIfNotEmpty() throws Exception {
        Optional<Integer> result = createIntStream(1, 2, 3, 4, 5).applyIfNotEmpty(s -> s.sum());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(15), result.get());

        result = IntStream.empty().applyIfNotEmpty(s -> s.sum());
        assertFalse(result.isPresent());
    }

    @Test
    public void testApplyIfNotEmpty_EmptyStream() {
        Optional<Integer> result = IntStream.empty().applyIfNotEmpty(s -> s.sum());
        assertFalse(result.isPresent());
    }

    @Test
    public void testApplyIfNotEmpty_HappyPath() {
        Optional<Integer> result = IntStream.of(1, 2, 3).applyIfNotEmpty(s -> s.sum());
        assertTrue(result.isPresent());
        assertEquals(6, result.get().intValue());
    }

    // average

    @Test
    public void testAverage() {
        OptionalDouble avg = createIntStream(1, 2, 3, 4, 5).average();
        assertTrue(avg.isPresent());
        assertEquals(3.0, avg.getAsDouble(), 0.001);

        assertFalse(IntStream.empty().average().isPresent());
    }

    @Test
    public void testAverage_BasicAverage() {
        OptionalDouble result = IntStream.of(2, 4, 6, 8).average();
        assertTrue(result.isPresent());
        assertEquals(5.0, result.get(), 0.001);
    }

    @Test
    public void testAverage_EmptyStream() {
        OptionalDouble result = IntStream.empty().average();
        assertFalse(result.isPresent());
    }

    @Test
    public void average_onEmptyStream_shouldReturnEmpty() {
        assertTrue(IntStream.empty().average().isEmpty());
    }

    @Test
    public void average_shouldReturnAverageOfElements() {
        assertEquals(2.0, IntStream.of(1, 2, 3).average().orElseThrow(), 0.001);
    }

    @Test
    public void testAverage_SingleElement() {
        OptionalDouble result = IntStream.of(42).average();
        assertTrue(result.isPresent());
        assertEquals(42.0, result.get(), 0.001);
    }

    // count

    @Test
    public void testCount() {
        assertEquals(5, createIntStream(1, 2, 3, 4, 5).count());
        assertEquals(0, IntStream.empty().count());
    }

    @Test
    public void testCount_BasicCount() {
        long count = IntStream.of(1, 2, 3, 4, 5).count();
        assertEquals(5, count);
    }

    @Test
    public void testCount_Empty() {
        long result = IntStream.empty().count();
        assertEquals(0, result);
    }

    @Test
    public void testCount_EmptyStream() {
        long count = IntStream.empty().count();
        assertEquals(0, count);
    }

    @Test
    public void testCount_HappyPath() {
        long result = IntStream.of(1, 2, 3, 4, 5).count();
        assertEquals(5, result);
    }

    // elementAt

    @Test
    public void testElementAt() {
        OptionalInt element = createIntStream(10, 20, 30, 40, 50).elementAt(2);
        assertTrue(element.isPresent());
        assertEquals(30, element.getAsInt());

        assertFalse(IntStream.of(1, 2, 3).elementAt(10).isPresent());
    }

    @Test
    public void testElementAt_BeyondSize() {
        OptionalInt result = IntStream.of(10, 20).elementAt(5);
        assertFalse(result.isPresent());
    }

    @Test
    public void testElementAt_HappyPath() {
        OptionalInt result = IntStream.of(10, 20, 30, 40).elementAt(2);
        assertTrue(result.isPresent());
        assertEquals(30, result.get());
    }

    // empty

    @Test
    public void findFirst_onEmptyStream_shouldReturnEmpty() {
        assertTrue(IntStream.empty().first().isEmpty());
    }

    @Test
    public void testFindLast_EmptyStream() {
        OptionalInt result = IntStream.empty().last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testSummarize_EmptyStream() {
        IntSummaryStatistics stats = IntStream.empty().summaryStatistics();
        assertNotNull(stats);
        assertEquals(0, stats.getCount());
    }

    @Test
    public void testFindAny() {
        OptionalInt any = createIntStream(1, 2, 3).first();
        assertTrue(any.isPresent());
        assertTrue(Arrays.asList(1, 2, 3).contains(any.getAsInt()));
    }

    // findAny

    @Test
    public void testFindAny_ElementFound() {
        OptionalInt result = IntStream.of(1, 2, 3, 4, 5).findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_EmptyStream() {
        OptionalInt result = IntStream.empty().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAny_NoArg() {
        OptionalInt result = createIntStream(10, 20, 30).findAny();
        assertTrue(result.isPresent());
    }

    // findFirst

    @Test
    public void testFindFirst_ElementFound() {
        OptionalInt result = IntStream.of(1, 2, 3, 4, 5).findFirst();
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testFindFirst_EmptyStream() {
        OptionalInt result = IntStream.empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindFirst_NoArg() {
        OptionalInt result = createIntStream(10, 20, 30).findFirst();
        assertTrue(result.isPresent());
        assertEquals(10, result.getAsInt());
    }

    @Test
    public void testFindFirst_WithPredicate() {
        OptionalInt result = IntStream.of(1, 2, 3, 4, 5).findFirst(x -> x > 3);
        assertTrue(result.isPresent());
        assertEquals(4, result.get());
    }

    @Test
    public void testFindFirstWithPredicate() throws Exception {
        OptionalInt found = createIntStream(1, 2, 3, 4, 5).findFirst(n -> n > 3);
        assertTrue(found.isPresent());
        assertEquals(4, found.getAsInt());
    }

    @Test
    public void testFindLastWithPredicate() throws Exception {
        OptionalInt found = createIntStream(1, 2, 3, 4, 5).findLast(n -> n < 4);
        assertTrue(found.isPresent());
        assertEquals(3, found.getAsInt());
    }

    // first

    @Test
    public void testFindFirst() {
        OptionalInt first = createIntStream(1, 2, 3).first();
        assertTrue(first.isPresent());
        assertEquals(1, first.getAsInt());

        assertFalse(IntStream.empty().first().isPresent());
    }

    @Test
    public void findFirst_onNonEmptyStream_shouldReturnFirstElement() {
        assertEquals(1, IntStream.of(1, 2, 3).first().orElseThrow());
    }

    @Test
    public void testFirst_Empty() {
        OptionalInt result = IntStream.empty().first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFirst_HappyPath() {
        OptionalInt result = IntStream.of(5, 3, 1).first();
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    // forEach

    @Test
    public void testForEach() {
        List<Integer> list = new ArrayList<>();
        createIntStream(1, 2, 3).forEach(list::add);
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testForEach_EmptyStream() {
        AtomicInteger counter = new AtomicInteger(0);
        IntStream.empty().forEach(x -> counter.incrementAndGet());
        assertEquals(0, counter.get());
    }

    @Test
    public void testForEach_ExecutesAction() {
        AtomicInteger sum = new AtomicInteger(0);
        IntStream.of(1, 2, 3, 4, 5).forEach(sum::addAndGet);
        assertEquals(15, sum.get());
    }

    // foreach

    @Test
    public void testForeach() {
        List<Integer> collected = new ArrayList<>();
        createIntStream(1, 2, 3).foreach(collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForeach_Empty() {
        List<Integer> collected = new ArrayList<>();
        IntStream.empty().foreach(collected::add);
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testForeach_HappyPath() {
        List<Integer> collected = new ArrayList<>();
        IntStream.of(1, 2, 3).foreach(collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForEachIndexed() throws Exception {
        Map<Integer, Integer> indexToValue = new HashMap<>();
        createIntStream(10, 20, 30).forEachIndexed((index, value) -> {
            indexToValue.put(index, value);
        });

        assertEquals(3, indexToValue.size());
        assertEquals(Integer.valueOf(10), indexToValue.get(0));
        assertEquals(Integer.valueOf(20), indexToValue.get(1));
        assertEquals(Integer.valueOf(30), indexToValue.get(2));
    }

    @Test
    public void testForEachIndexed_ExecutesAction() {
        List<String> results = new ArrayList<>();
        IntStream.of(10, 20, 30).forEachIndexed((idx, val) -> results.add(idx + ":" + val));
        assertEquals(Arrays.asList("0:10", "1:20", "2:30"), results);
    }

    // kthLargest

    @Test
    public void testKthLargest() {
        OptionalInt kth = createIntStream(3, 1, 4, 1, 5, 9, 2, 6).kthLargest(3);
        assertTrue(kth.isPresent());
        assertEquals(5, kth.getAsInt());
    }

    @Test
    public void testKthLargest_BasicKth() {
        OptionalInt result = IntStream.of(5, 3, 8, 1, 9, 2).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(8, result.get());
    }

    @Test
    public void testKthLargest_EmptyStream() {
        OptionalInt result = IntStream.empty().kthLargest(1);
        assertFalse(result.isPresent());
    }

    @Test
    public void testKthLargest_SortedBranch() {
        OptionalInt result = createIntStream(1, 2, 3, 4).sorted().kthLargest(2);

        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    // last

    @Test
    public void testFindLast() {
        OptionalInt last = createIntStream(1, 2, 3).last();
        assertTrue(last.isPresent());
        assertEquals(3, last.getAsInt());
    }

    @Test
    public void testFindLast_ElementFound() {
        OptionalInt result = IntStream.of(1, 2, 3, 4, 5).last();
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    public void testLast_HappyPath() {
        OptionalInt result = IntStream.of(5, 3, 1).last();
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    // max

    @Test
    public void testMax() {
        OptionalInt max = createIntStream(3, 1, 4, 1, 5).max();
        assertTrue(max.isPresent());
        assertEquals(5, max.getAsInt());

        assertFalse(IntStream.empty().max().isPresent());
    }

    @Test
    public void testMax_BasicMax() {
        OptionalInt result = IntStream.of(5, 3, 8, 1, 9, 2).max();
        assertTrue(result.isPresent());
        assertEquals(9, result.get());
    }

    @Test
    public void testMax_EmptyStream() {
        OptionalInt result = IntStream.empty().max();
        assertFalse(result.isPresent());
    }

    @Test
    public void testMax_SingleElement() {
        OptionalInt result = IntStream.of(42).max();
        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    // min

    @Test
    public void testMin() {
        OptionalInt min = createIntStream(3, 1, 4, 1, 5).min();
        assertTrue(min.isPresent());
        assertEquals(1, min.getAsInt());

        assertFalse(IntStream.empty().min().isPresent());
    }

    @Test
    public void testMin_BasicMin() {
        OptionalInt result = IntStream.of(5, 3, 8, 1, 9, 2).min();
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testMin_EmptyStream() {
        OptionalInt result = IntStream.empty().min();
        assertFalse(result.isPresent());
    }

    @Test
    public void testMin_SingleElement() {
        OptionalInt result = IntStream.of(42).min();
        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    // noneMatch

    @Test
    public void testNoneMatch() {
        assertTrue(IntStream.of(1, 3, 5, 7).noneMatch(n -> n % 2 == 0));
        assertFalse(IntStream.of(1, 3, 4, 7).noneMatch(n -> n % 2 == 0));
    }

    @Test
    public void testNoneMatch_EmptyStream() {
        boolean result = IntStream.empty().noneMatch(x -> true);
        assertTrue(result);
    }

    @Test
    public void testNoneMatch_NoneMatching() {
        boolean result = IntStream.of(1, 3, 5, 7).noneMatch(x -> x % 2 == 0);
        assertTrue(result);
    }

    @Test
    public void noneMatch_shouldReturnTrueIfNoElementsMatch() {
        assertTrue(IntStream.of(1, 3, 5).noneMatch(i -> i % 2 == 0));
    }

    @Test
    public void testNoneMatch_SomeMatching() {
        boolean result = IntStream.of(1, 2, 3, 4).noneMatch(x -> x % 2 == 0);
        assertFalse(result);
    }

    @Test
    public void testPercentiles() {
        Optional<Map<Percentage, Integer>> percentiles = createIntStream(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).percentiles();
        assertTrue(percentiles.isPresent());

        Map<Percentage, Integer> map = percentiles.get();
        assertNotNull(map);
        assertTrue(map.containsKey(Percentage._50));
    }

    @Test
    public void testPercentiles_HappyPath() {
        Pair<IntSummaryStatistics, Optional<Map<Percentage, Integer>>> result = IntStream.of(1, 2, 3, 4, 5).summaryStatisticsAndPercentiles();
        assertNotNull(result);
        assertEquals(5, result.left().getCount());
        assertEquals(1, result.left().getMin());
        assertEquals(5, result.left().getMax());
    }

    // reduce

    @Test
    public void testReduce() {
        int sum = createIntStream(1, 2, 3, 4, 5).reduce(0, Integer::sum);
        assertEquals(15, sum);

        OptionalInt product = createIntStream(2, 3, 4).reduce((a, b) -> a * b);
        assertTrue(product.isPresent());
        assertEquals(24, product.getAsInt());
    }

    @Test
    public void reduce_withAccumulator_shouldPerformReduction() {
        int result = IntStream.of(1, 2, 3).reduce((a, b) -> a + b).orElseThrow();
        assertEquals(6, result);
    }

    @Test
    public void testReduce_WithIdentity() {
        int result = IntStream.of(1, 2, 3, 4, 5).reduce(0, (a, b) -> a + b);
        assertEquals(15, result);
    }

    @Test
    public void testReduce_WithIdentity_EmptyStream() {
        int result = IntStream.empty().reduce(10, (a, b) -> a + b);
        assertEquals(10, result);
    }

    @Test
    public void reduce_withIdentityAndAccumulator_shouldPerformReduction() {
        int result = IntStream.of(1, 2, 3).reduce(0, (a, b) -> a + b);
        assertEquals(6, result);
    }

    @Test
    public void testReduce_WithoutIdentity() {
        OptionalInt result = IntStream.of(1, 2, 3, 4, 5).reduce((a, b) -> a + b);
        assertTrue(result.isPresent());
        assertEquals(15, result.get());
    }

    @Test
    public void testReduce_WithoutIdentity_EmptyStream() {
        OptionalInt result = IntStream.empty().reduce((a, b) -> a + b);
        assertFalse(result.isPresent());
    }

    // sum

    @Test
    public void testSum() {
        assertEquals(15, createIntStream(1, 2, 3, 4, 5).sum());
        assertEquals(0, IntStream.empty().sum());
    }

    @Test
    public void testSum_BasicSum() {
        int result = IntStream.of(1, 2, 3, 4, 5).sum();
        assertEquals(15, result);
    }

    @Test
    public void testSum_EmptyStream() {
        int result = IntStream.empty().sum();
        assertEquals(0, result);
    }

    @Test
    public void testSum_NegativeNumbers() {
        int result = IntStream.of(-5, -3, -2).sum();
        assertEquals(-10, result);
    }

    @Test
    public void sum_onEmptyStream_shouldReturnZero() {
        assertEquals(0, IntStream.empty().sum());
    }

    @Test
    public void sum_shouldReturnSumOfElements() {
        assertEquals(6, IntStream.of(1, 2, 3).sum());
    }

    @Test
    public void testSummarize_BasicSummary() {
        IntSummaryStatistics stats = IntStream.of(1, 2, 3, 4, 5).summaryStatistics();
        assertNotNull(stats);
        assertEquals(5, stats.getCount());
        assertEquals(15, stats.getSum());
        assertEquals(1, stats.getMin());
        assertEquals(5, stats.getMax());
        assertEquals(3.0, stats.getAverage(), 0.001);
    }

    @Test
    public void testsummaryStatistics() {
        IntSummaryStatistics stats = createIntStream(1, 2, 3, 4, 5).summaryStatistics();
        assertEquals(5, stats.getCount());
        assertEquals(15, stats.getSum());
        assertEquals(1, stats.getMin());
        assertEquals(5, stats.getMax());
        assertEquals(3.0, stats.getAverage(), 0.001);
    }

    @Test
    public void testsummaryStatisticsAndPercentiles() {
        Pair<IntSummaryStatistics, Optional<Map<Percentage, Integer>>> result = IntStream.range(1, 101).summaryStatisticsAndPercentiles();

        IntSummaryStatistics stats = result.left();
        assertEquals(100, stats.getCount());
        assertEquals(5050, stats.getSum());
        assertEquals(1, stats.getMin());
        assertEquals(100, stats.getMax());

        assertTrue(result.right().isPresent());
        Map<Percentage, Integer> percentiles = result.right().get();
        assertNotNull(percentiles);
    }

    @Test
    public void testSummaryStatisticsAndPercentilesOneToTenQuartiles() {
        final Pair<IntSummaryStatistics, Optional<Map<Percentage, Integer>>> result = IntStream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .summaryStatisticsAndPercentiles();

        assertEquals(10, result.left().getCount());
        assertEquals(55, result.left().getSum());
        assertTrue(result.right().isPresent());

        final Map<Percentage, Integer> percentiles = result.right().get();
        assertEquals(Percentage.values().length, percentiles.size());
        assertEquals(3, percentiles.get(Percentage._25));
        assertEquals(6, percentiles.get(Percentage._50));
        assertEquals(8, percentiles.get(Percentage._75));
    }

}
