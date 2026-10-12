package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalShort;

/** ShortStream reduction contracts. */
public class ShortStreamReductionTest extends ShortStreamTestSupport {

    // acceptIfNotEmpty

    @Test
    @DisplayName("Test acceptIfNotEmpty() method")
    public void testAcceptIfNotEmpty() {
        final long[] count = { 0 };

        stream.acceptIfNotEmpty(s -> count[0] = s.count()).orElse(() -> count[0] = -1);
        assertEquals(5L, count[0]);

        count[0] = 0;
        ShortStream.empty().acceptIfNotEmpty(s -> count[0] = s.count()).orElse(() -> count[0] = -1);
        assertEquals(-1L, count[0]);
    }

    @Test
    public void testAcceptIfNotEmpty_EmptyStream() {
        List<Short> collected = new ArrayList<>();
        ShortStream.empty().acceptIfNotEmpty(s -> s.forEach(collected::add));
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testAcceptIfNotEmptyMethod() {
        List<Short> collected = new ArrayList<>();
        createShortStream((short) 1, (short) 2, (short) 3).acceptIfNotEmpty(s -> s.forEach(collected::add));
        assertEquals(3, collected.size());
    }

    // allMatch

    @Test
    public void testAllMatch() {
        boolean result = ShortStream.of((short) 1, (short) 2, (short) 3).allMatch(n -> n > 0);
        assertTrue(result);
    }

    @Test
    public void testAllMatch_Empty() {
        assertTrue(ShortStream.empty().allMatch(s -> false));
    }

    @Test
    public void testAllMatchEmpty() {
        boolean result = ShortStream.empty().allMatch(n -> false);
        assertTrue(result);
    }

    @Test
    public void testAllMatchFalse() {
        boolean result = ShortStream.of((short) 1, (short) 2, (short) 3).allMatch(n -> n > 2);
        assertFalse(result);
    }

    // anyMatch

    @Test
    public void testAnyMatch() {
        boolean result = ShortStream.of((short) 1, (short) 2, (short) 3).anyMatch(n -> n > 2);
        assertTrue(result);
    }

    @Test
    public void testAnyMatch_Empty() {
        assertFalse(ShortStream.empty().anyMatch(s -> true));
    }

    @Test
    public void testAnyMatchEmpty() {
        boolean result = ShortStream.empty().anyMatch(n -> true);
        assertFalse(result);
    }

    @Test
    public void testAnyMatchNone() {
        boolean result = ShortStream.of((short) 1, (short) 2, (short) 3).anyMatch(n -> n > 5);
        assertFalse(result);
    }

    // applyIfNotEmpty

    @Test
    @DisplayName("Test applyIfNotEmpty() method")
    public void testApplyIfNotEmpty() {
        Optional<Long> count = stream.applyIfNotEmpty(s -> s.count());
        assertTrue(count.isPresent());
        assertEquals(5L, count.get());

        Optional<Long> emptyCount = ShortStream.empty().applyIfNotEmpty(s -> s.count());
        assertFalse(emptyCount.isPresent());
    }

    @Test
    public void testApplyIfNotEmpty_EmptyStream() {
        Optional<short[]> result = ShortStream.empty().applyIfNotEmpty(s -> s.toArray());
        assertFalse(result.isPresent());
    }

    @Test
    public void testApplyIfNotEmptyMethod() {
        Optional<short[]> result = createShortStream((short) 1, (short) 2, (short) 3).applyIfNotEmpty(s -> s.toArray());
        assertTrue(result.isPresent());
        assertArrayEquals(new short[] { 1, 2, 3 }, result.get());
    }

    // average

    @Test
    public void testAverage() {
        OptionalDouble avg = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).average();
        assertTrue(avg.isPresent());
        assertEquals(2.5, avg.get(), 0.0001);
    }

    @Test
    public void testAverage_largeStream_noOverflow() {
        // Regression: average() must not throw when the short sum exceeds Integer.MAX_VALUE (#81).
        final int n = 70_000;
        final short[] arr = new short[n];
        java.util.Arrays.fill(arr, (short) 32767);
        org.junit.jupiter.api.Assertions.assertEquals(32767.0, com.landawn.abacus.util.stream.ShortStream.of(arr).average().getAsDouble(), 0.0001);
    }

    @Test
    public void testAverage_SingleElement() {
        OptionalDouble result = createShortStream((short) 42).average();
        assertTrue(result.isPresent());
        assertEquals(42.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testAverageEmpty() {
        OptionalDouble avg = ShortStream.empty().average();
        assertFalse(avg.isPresent());
    }

    // count

    @Test
    public void testCount() {
        long count = ShortStream.of((short) 1, (short) 2, (short) 3).count();
        assertEquals(3, count);
    }

    @Test
    public void testCount_Empty() {
        assertEquals(0, ShortStream.empty().count());
    }

    @Test
    public void testCountEmpty() {
        long count = ShortStream.empty().count();
        assertEquals(0, count);
    }

    // elementAt

    @Test
    public void testElementAt() {
        assertTrue(multiElementStream.elementAt(2).isPresent());
        assertEquals((short) 3, multiElementStream2.elementAt(2).get());
        assertFalse(multiElementStream3.elementAt(10).isPresent());
        assertFalse(emptyStream.elementAt(0).isPresent());
    }

    @Test
    public void testElementAt_OutOfRange() {
        OptionalShort result = createShortStream((short) 10, (short) 20).elementAt(5);
        assertFalse(result.isPresent());
    }

    @Test
    public void testElementAt_Valid() {
        OptionalShort result = createShortStream((short) 10, (short) 20, (short) 30).elementAt(1);
        assertTrue(result.isPresent());
        assertEquals(20, result.get());
    }

    // findAny

    @Test
    public void testFindAny() {
        OptionalShort result = ShortStream.of((short) 1, (short) 2, (short) 3).findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_EmptyStream() {
        OptionalShort result = ShortStream.empty().findAny(s -> s > 0);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAnyNoPredicate() {
        OptionalShort result = createShortStream((short) 7, (short) 8).findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAnyNoPredicate_EmptyStream() {
        OptionalShort result = ShortStream.empty().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAnyWithPredicate() {
        OptionalShort result = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).findAny(n -> n > 2);
        assertTrue(result.isPresent());
        assertTrue(result.get() > 2);
    }

    // findFirst

    @Test
    public void testFindFirst() {
        OptionalShort result = ShortStream.of((short) 1, (short) 2, (short) 3).findFirst();
        assertTrue(result.isPresent());
        assertEquals(1, result.get());
    }

    @Test
    public void testFindFirst_Empty() {
        OptionalShort result = ShortStream.empty().findFirst(s -> s > 0);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindFirstNoPredicate() {
        OptionalShort result = createShortStream((short) 7, (short) 8, (short) 9).findFirst();
        assertTrue(result.isPresent());
        assertEquals((short) 7, result.getAsShort());
    }

    @Test
    public void testFindFirstNoPredicate_EmptyStream() {
        OptionalShort result = ShortStream.empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindFirstWithPredicate() {
        OptionalShort result = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).findFirst(n -> n > 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.get());
    }

    @Test
    public void testFindFirstWithPredicateNone() {
        OptionalShort result = ShortStream.of((short) 1, (short) 2, (short) 3).findFirst(n -> n > 5);
        assertFalse(result.isPresent());
    }

    // findLast

    @Test
    public void testFindLast_EmptyStream() {
        OptionalShort result = ShortStream.empty().findLast(s -> s > 0);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindLast_NonEmpty() {
        OptionalShort result = createShortStream((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).findLast(s -> s < 4);
        assertTrue(result.isPresent());
        assertEquals(3, result.get());
    }

    @Test
    public void testFindLastWithPredicate() {
        OptionalShort result = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 2).findLast(n -> n == 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.get());
    }

    // first

    @Test
    public void testFirst() {
        assertTrue(multiElementStream.first().isPresent());
        assertEquals((short) 1, multiElementStream2.first().get());
        assertFalse(emptyStream.first().isPresent());
    }

    @Test
    public void testFirst_Empty() {
        OptionalShort result = ShortStream.empty().first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFirst_NonEmpty() {
        OptionalShort result = createShortStream((short) 10, (short) 20, (short) 30).first();
        assertTrue(result.isPresent());
        assertEquals(10, result.get());
    }

    @Test
    public void testForEach() {
        List<Short> collected = new ArrayList<>();
        ShortStream.of((short) 1, (short) 2, (short) 3).forEach(collected::add);
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), collected);
    }

    @Test
    public void testForEach_Empty() {
        List<Short> collected = new ArrayList<>();
        ShortStream.empty().forEach(collected::add);
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testForEachIndexed() {
        Map<Integer, Short> collected = new HashMap<>();
        ShortStream.of((short) 10, (short) 20, (short) 30).forEachIndexed(collected::put);
        assertEquals((short) 10, collected.get(0).shortValue());
        assertEquals((short) 20, collected.get(1).shortValue());
        assertEquals((short) 30, collected.get(2).shortValue());
    }

    @Test
    public void testForEachIndexed_Empty() {
        List<Short> collected = new ArrayList<>();
        ShortStream.empty().forEachIndexed((idx, val) -> collected.add(val));
        assertTrue(collected.isEmpty());
    }

    // kthLargest

    @Test
    public void testKthLargest() {
        OptionalShort result = ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2, (short) 5).kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals(4, result.get());
    }

    @Test
    public void testKthLargest_SingleElement() {
        OptionalShort result = createShortStream((short) 5).kthLargest(1);
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    public void testKthLargestEmpty() {
        OptionalShort result = ShortStream.empty().kthLargest(1);
        assertFalse(result.isPresent());
    }

    @Test
    public void testKthLargestOutOfRange() {
        OptionalShort result = ShortStream.of((short) 1, (short) 2).kthLargest(5);
        assertFalse(result.isPresent());
    }

    // last

    @Test
    public void testFindLast() {
        OptionalShort result = ShortStream.of((short) 1, (short) 2, (short) 3).last();
        assertTrue(result.isPresent());
        assertEquals(3, result.get());
    }

    @Test
    public void testLast() {
        assertTrue(multiElementStream.last().isPresent());
        assertEquals((short) 5, multiElementStream2.last().get());
        assertFalse(emptyStream.last().isPresent());
    }

    @Test
    public void testLast_Empty() {
        OptionalShort result = ShortStream.empty().last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast_NonEmpty() {
        OptionalShort result = createShortStream((short) 10, (short) 20, (short) 30).last();
        assertTrue(result.isPresent());
        assertEquals(30, result.get());
    }

    // max

    @Test
    public void testMax() {
        OptionalShort max = ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2).max();
        assertTrue(max.isPresent());
        assertEquals(4, max.get());
    }

    @Test
    public void testMax_SingleElement() {
        OptionalShort result = createShortStream((short) 5).max();
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    public void testMaxEmpty() {
        OptionalShort max = ShortStream.empty().max();
        assertFalse(max.isPresent());
    }

    // min

    @Test
    public void testMin() {
        OptionalShort min = ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2).min();
        assertTrue(min.isPresent());
        assertEquals(1, min.get());
    }

    @Test
    public void testMin_SingleElement() {
        OptionalShort result = createShortStream((short) 5).min();
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    public void testMinEmpty() {
        OptionalShort min = ShortStream.empty().min();
        assertFalse(min.isPresent());
    }

    // noneMatch

    @Test
    public void testNoneMatch() {
        boolean result = ShortStream.of((short) 1, (short) 2, (short) 3).noneMatch(n -> n > 5);
        assertTrue(result);
    }

    @Test
    public void testNoneMatch_False() {
        assertFalse(createShortStream((short) 1, (short) 2, (short) 3).noneMatch(s -> s > 2));
    }

    @Test
    public void testNoneMatch_True() {
        assertTrue(createShortStream((short) 1, (short) 2, (short) 3).noneMatch(s -> s > 10));
    }

    @Test
    public void testNoneMatchEmpty() {
        boolean result = ShortStream.empty().noneMatch(n -> true);
        assertTrue(result);
    }

    @Test
    public void testNoneMatchFalse() {
        boolean result = ShortStream.of((short) 1, (short) 2, (short) 3).noneMatch(n -> n > 2);
        assertFalse(result);
    }

    @Test
    @DisplayName("Test percentiles() method")
    public void testPercentiles() {
        Optional<Map<Percentage, Short>> percentiles = stream.percentiles();
        assertTrue(percentiles.isPresent());

        Map<Percentage, Short> map = percentiles.get();
        assertEquals((short) 3, map.get(Percentage._50));
        assertEquals((short) 5, map.get(Percentage._99_9999));
    }

    @Test
    public void testPercentiles_Empty() {
        Optional<Map<Percentage, Short>> result = ShortStream.empty().percentiles();
        assertFalse(result.isPresent());
    }

    // reduce

    @Test
    public void testReduce() {
        short result = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).reduce((short) 0, (a, b) -> (short) (a + b));
        assertEquals(10, result);
    }

    @Test
    public void testReduce_SingleElement() {
        OptionalShort result = createShortStream((short) 5).reduce((a, b) -> (short) (a + b));
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    public void testReduceOptional() {
        OptionalShort result = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).reduce((a, b) -> (short) (a + b));
        assertTrue(result.isPresent());
        assertEquals(10, result.get());
    }

    @Test
    public void testReduceOptionalEmpty() {
        OptionalShort result = ShortStream.empty().reduce((a, b) -> (short) (a + b));
        assertFalse(result.isPresent());
    }

    @Test
    public void testReduceWithIdentity() {
        short sum = multiElementStream.reduce((short) 0, (a, b) -> (short) (a + b));
        assertEquals(15, sum);
        short product = multiElementStream2.reduce((short) 1, (a, b) -> (short) (a * b));
        assertEquals(120, product);
    }

    @Test
    public void testReduceWithIdentityOnEmpty() {
        short result = ShortStream.empty().reduce((short) 10, (a, b) -> (short) (a + b));
        assertEquals((short) 10, result);
    }

    // sum

    @Test
    public void testSum() {
        int sum = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).sum();
        assertEquals(10, sum);
    }

    @Test
    public void testSum_SingleElement() {
        assertEquals(42, createShortStream((short) 42).sum());
    }

    @Test
    public void testSumEmpty() {
        int sum = ShortStream.empty().sum();
        assertEquals(0, sum);
    }

    @Test
    public void testsummaryStatistics() {
        ShortSummaryStatistics stats = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4).summaryStatistics();
        assertNotNull(stats);
        assertEquals(4, stats.getCount());
        assertEquals(10, stats.getSum());
        assertEquals(1, stats.getMin());
        assertEquals(4, stats.getMax());
        assertEquals(2.5, stats.getAverage(), 0.0001);
    }

    @Test
    public void testSummaryStatistics_SingleElement() {
        ShortSummaryStatistics stats = createShortStream((short) 42).summaryStatistics();
        assertEquals(1, stats.getCount());
        assertEquals(42, stats.getMin());
        assertEquals(42, stats.getMax());
        assertEquals(42, stats.getSum());
    }

    @Test
    public void testsummaryStatisticsAndPercentiles() {
        Pair<ShortSummaryStatistics, Optional<Map<Percentage, Short>>> result = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5)
                .summaryStatisticsAndPercentiles();

        assertNotNull(result.left());
        assertEquals(5, result.left().getCount());
        assertTrue(result.right().isPresent());
    }

    @Test
    public void testSummaryStatisticsAndPercentiles_Empty() {
        Pair<ShortSummaryStatistics, Optional<Map<Percentage, Short>>> result = ShortStream.empty().summaryStatisticsAndPercentiles();
        assertEquals(0, result.getLeft().getCount());
        assertFalse(result.getRight().isPresent());
    }

}
