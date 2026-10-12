package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;

/** DoubleStream reduction contracts. */
public class DoubleStreamReductionTest extends DoubleStreamTestSupport {

    @Test
    public void testAcceptIfNotEmpty_EmptyStream() {
        AtomicInteger called = new AtomicInteger(0);
        DoubleStream.empty().acceptIfNotEmpty(s -> called.set(1));
        assertEquals(0, called.get());
    }

    @Test
    public void testAcceptIfNotEmpty_HappyPath() {
        AtomicInteger called = new AtomicInteger(0);
        DoubleStream.of(1.0, 2.0, 3.0).acceptIfNotEmpty(s -> called.set((int) s.sum()));
        assertEquals(6, called.get());
    }

    @Test
    public void testAllMatch() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        assertTrue(stream.allMatch(x -> x > 0.0));

        DoubleStream stream2 = DoubleStream.of(1.0, 2.0, 3.0);
        assertFalse(stream2.allMatch(x -> x > 2.0));
    }

    @Test
    public void testAnyMatch() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        assertTrue(stream.anyMatch(x -> x > 2.0));

        DoubleStream stream2 = DoubleStream.of(1.0, 2.0);
        assertFalse(stream2.anyMatch(x -> x > 5.0));
    }

    @Test
    public void testApplyIfNotEmpty_EmptyStream() {
        Optional<Double> result = DoubleStream.empty().applyIfNotEmpty(s -> s.sum());
        assertFalse(result.isPresent());
    }

    @Test
    public void testApplyIfNotEmpty_HappyPath() {
        Optional<Double> result = DoubleStream.of(1.0, 2.0, 3.0).applyIfNotEmpty(s -> s.sum());
        assertTrue(result.isPresent());
        assertEquals(6.0, result.get(), 0.001);
    }

    @Test
    public void testAverage() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        OptionalDouble result = stream.average();
        assertTrue(result.isPresent());
        assertEquals(2.5, result.get(), 0.0001);

        DoubleStream emptyStream = DoubleStream.empty();
        assertFalse(emptyStream.average().isPresent());
    }

    @Test
    public void testAverageOnEmptyStream() {
        OptionalDouble avg = DoubleStream.empty().average();
        assertFalse(avg.isPresent());
    }

    // count

    @Test
    public void testCount() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        assertEquals(3, stream.count());
    }

    @Test
    public void testCount_Empty() {
        long result = DoubleStream.empty().count();
        assertEquals(0, result);
    }

    @Test
    public void testCount_HappyPath2() {
        long result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).count();
        assertEquals(5, result);
    }

    @Test
    public void testElementAt_BeyondSize() {
        OptionalDouble result = DoubleStream.of(10.0, 20.0).elementAt(5);
        assertFalse(result.isPresent());
    }

    @Test
    public void testElementAt_HappyPath() {
        OptionalDouble result = DoubleStream.of(10.0, 20.0, 30.0, 40.0).elementAt(2);
        assertTrue(result.isPresent());
        assertEquals(30.0, result.get(), 0.001);
    }

    @Test
    public void testFindFirstOnEmptyStream() {
        OptionalDouble first = DoubleStream.empty().first();
        assertFalse(first.isPresent());
    }

    // findAny

    @Test
    public void testFindAny() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        OptionalDouble result = stream.findAny(x -> x > 1.5);
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_NoArg() {
        OptionalDouble result = createDoubleStream(10.0, 20.0, 30.0).findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_NoArg_Empty() {
        OptionalDouble result = DoubleStream.empty().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAnyWithPredicate() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 4.0);
        OptionalDouble result = stream.findAny(x -> x > 2);
        assertTrue(result.isPresent());
        assertTrue(result.getAsDouble() > 2);
    }

    // findFirst

    @Test
    public void testFindFirst() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        OptionalDouble result = stream.findFirst(x -> x > 1.5);
        assertTrue(result.isPresent());
        assertEquals(2.0, result.get(), 0.0001);
    }

    @Test
    public void testFindFirst_NoArg() {
        OptionalDouble result = createDoubleStream(10.0, 20.0, 30.0).findFirst();
        assertTrue(result.isPresent());
        assertEquals(10.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testFindFirst_NoArg_Empty() {
        OptionalDouble result = DoubleStream.empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindFirstWithPredicate() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 4.0);
        OptionalDouble result = stream.findFirst(x -> x > 2);
        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.0001);
    }

    // findLast

    @Test
    public void testFindLast() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        OptionalDouble result = stream.findLast(x -> x < 3.5);
        assertTrue(result.isPresent());
        assertEquals(3.0, result.get(), 0.0001);
    }

    @Test
    public void testFindLast_Empty() {
        OptionalDouble result = DoubleStream.empty().findLast(x -> true);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindLast_HappyPath() {
        OptionalDouble result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).findLast(x -> x < 4.0);
        assertTrue(result.isPresent());
        assertEquals(3.0, result.get(), 0.001);
    }

    @Test
    public void testFindLastWithPredicate() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 4.0);
        OptionalDouble result = stream.findLast(x -> x < 3);
        assertTrue(result.isPresent());
        assertEquals(2.0, result.getAsDouble(), 0.0001);
    }

    // first

    @Test
    public void testFirst() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        OptionalDouble first = stream.first();
        assertTrue(first.isPresent());
        assertEquals(1.0, first.get(), 0.0001);
    }

    @Test
    public void testFirst_Empty() {
        OptionalDouble result = DoubleStream.empty().first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFirst_HappyPath2() {
        OptionalDouble result = DoubleStream.of(5.0, 3.0, 1.0).first();
        assertTrue(result.isPresent());
        assertEquals(5.0, result.get(), 0.001);
    }

    @Test
    public void testForEach() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        List<Double> list = new ArrayList<>();
        stream.forEach(list::add);
        assertEquals(3, list.size());
    }

    @Test
    public void testForeach() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        List<Double> list = new ArrayList<>();
        stream.foreach(list::add);
        assertEquals(3, list.size());
    }

    @Test
    public void testForeach_HappyPath2() {
        List<Double> collected = new ArrayList<>();
        DoubleStream.of(1.0, 2.0, 3.0).foreach(collected::add);
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), collected);
    }

    @Test
    public void testForEachIndexed() {
        DoubleStream stream = DoubleStream.of(10.0, 20.0, 30.0);
        List<String> list = new ArrayList<>();
        stream.forEachIndexed((idx, val) -> list.add(idx + ":" + val));
        assertEquals(3, list.size());
        assertEquals("0:10.0", list.get(0));
    }

    // kthLargest

    @Test
    public void testKthLargest() {
        DoubleStream stream = DoubleStream.of(5.0, 1.0, 3.0, 4.0, 2.0);
        OptionalDouble result = stream.kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(4.0, result.get(), 0.0001);
    }

    @Test
    public void testKthLargest_HappyPath2() {
        OptionalDouble result = DoubleStream.of(3.0, 1.0, 4.0, 1.0, 5.0).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(4.0, result.get(), 0.001);
    }

    @Test
    public void testKthLargest_SortedBranch() {
        OptionalDouble result = createDoubleStream(1.0, 2.0, 3.0, 4.0).sorted().kthLargest(2);

        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.001);
    }

    // last

    @Test
    public void testLast() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        OptionalDouble last = stream.last();
        assertTrue(last.isPresent());
        assertEquals(3.0, last.get(), 0.0001);
    }

    @Test
    public void testLast_Empty() {
        OptionalDouble result = DoubleStream.empty().last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast_HappyPath() {
        OptionalDouble result = DoubleStream.of(5.0, 3.0, 1.0).last();
        assertTrue(result.isPresent());
        assertEquals(1.0, result.get(), 0.001);
    }

    @Test
    public void testMax() {
        DoubleStream stream = DoubleStream.of(5.0, 1.0, 3.0, 2.0);
        OptionalDouble result = stream.max();
        assertTrue(result.isPresent());
        assertEquals(5.0, result.get(), 0.0001);

        DoubleStream emptyStream = DoubleStream.empty();
        assertFalse(emptyStream.max().isPresent());
    }

    @Test
    public void testMin() {
        DoubleStream stream = DoubleStream.of(5.0, 1.0, 3.0, 2.0);
        OptionalDouble result = stream.min();
        assertTrue(result.isPresent());
        assertEquals(1.0, result.get(), 0.0001);

        DoubleStream emptyStream = DoubleStream.empty();
        assertFalse(emptyStream.min().isPresent());
    }

    @Test
    public void testNoneMatch() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0);
        assertTrue(stream.noneMatch(x -> x > 10.0));

        DoubleStream stream2 = DoubleStream.of(1.0, 2.0, 3.0);
        assertFalse(stream2.noneMatch(x -> x > 2.0));
    }

    @Test
    public void testPercentiles_HappyPath() {
        Pair<DoubleSummaryStatistics, Optional<Map<Percentage, Double>>> result = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).summaryStatisticsAndPercentiles();
        assertNotNull(result);
        assertEquals(5, result.left().getCount());
        assertEquals(1.0, result.left().getMin(), 0.001);
        assertEquals(5.0, result.left().getMax(), 0.001);
    }

    // reduce

    @Test
    public void testReduce() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        double result = stream.reduce(0.0, (a, b) -> a + b);
        assertEquals(10.0, result, 0.0001);
    }

    @Test
    public void testReduceOnEmptyStream() {
        OptionalDouble result = DoubleStream.empty().reduce((a, b) -> a + b);
        assertFalse(result.isPresent());
    }

    @Test
    public void testReduceOptional() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        OptionalDouble result = stream.reduce((a, b) -> a + b);
        assertTrue(result.isPresent());
        assertEquals(10.0, result.get(), 0.0001);

        DoubleStream emptyStream = DoubleStream.empty();
        assertFalse(emptyStream.reduce((a, b) -> a + b).isPresent());
    }

    @Test
    public void testReduceWithIdentity() {
        double result = DoubleStream.of(1.0, 2.0, 3.0).reduce(10.0, (a, b) -> a + b);
        assertEquals(16.0, result);
    }

    @Test
    public void testReduceWithoutIdentity() {
        DoubleStream stream = createDoubleStream(1.0, 2.0, 3.0, 4.0);
        OptionalDouble result = stream.reduce(Double::max);
        assertTrue(result.isPresent());
        assertEquals(4.0, result.getAsDouble(), 0.0001);

        DoubleStream emptyStream = DoubleStream.empty();
        OptionalDouble emptyResult = emptyStream.reduce(Double::max);
        assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testSum() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0);
        double result = stream.sum();
        assertEquals(10.0, result, 0.0001);

        DoubleStream emptyStream = DoubleStream.empty();
        assertEquals(0.0, emptyStream.sum(), 0.0001);
    }

    @Test
    public void testsummaryStatistics() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);
        DoubleSummaryStatistics stats = stream.summaryStatistics();
        assertEquals(5, stats.getCount());
        assertEquals(15.0, stats.getSum(), 0.0001);
        assertEquals(3.0, stats.getAverage(), 0.0001);
        assertEquals(1.0, stats.getMin(), 0.0001);
        assertEquals(5.0, stats.getMax(), 0.0001);
    }

    @Test
    public void testsummaryStatisticsAndPercentiles() {
        DoubleStream stream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0);
        Pair<DoubleSummaryStatistics, Optional<Map<Percentage, Double>>> result = stream.summaryStatisticsAndPercentiles();
        assertNotNull(result);
        assertNotNull(result.left());
        assertEquals(5, result.left().getCount());
    }

}
